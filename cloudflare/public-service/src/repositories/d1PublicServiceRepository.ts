import { loadConfig, type ServiceConfig } from "../config";
import { ApiError } from "../http/errors";
import { isKnownCity, isKnownOperator, isKnownRegion } from "../domain/catalog";
import { generateDeviceToken, generateLinkCode, hmacSha256Hex, normalizeLinkCode, randomId } from "../security/crypto";
import type {
  ClaimedCommand,
  CommandRecord,
  CommandResultRequest,
  Env,
  InstallationRecord,
  InstallationSettingsRequest,
  LinkedDeviceRecord,
  OperatorCode,
  PublicReportRequest,
  ReportSample,
  ServiceSyncRequest,
  TelegramUpdate,
} from "../types";

export class D1PublicServiceRepository {
  readonly config: ServiceConfig;

  constructor(
    private readonly db: D1Database,
    private readonly env: Env,
  ) {
    this.config = loadConfig(env);
  }

  async registerInstallation(appVersion: string, now: number): Promise<{
    installationId: string;
    deviceToken: string;
    createdAt: number;
  }> {
    const pepper = this.requirePepper();
    const installationId = randomId("ins");
    const deviceToken = generateDeviceToken();
    const hash = await hmacSha256Hex(pepper, deviceToken);
    await this.db
      .prepare(
        `
        INSERT INTO installations(
          installation_id, device_token_hash, created_at, updated_at, app_version, schema_version
        ) VALUES(?1, ?2, ?3, ?3, ?4, 1)
        `,
      )
      .bind(installationId, hash, now, appVersion)
      .run();
    return { installationId, deviceToken, createdAt: now };
  }

  async authenticateDevice(authorization: string | null): Promise<InstallationRecord> {
    const token = parseBearer(authorization);
    if (!token) throw new ApiError("UNAUTHORIZED", "Device token is required", 401);
    const hash = await hmacSha256Hex(this.requirePepper(), token);
    const row = await this.db
      .prepare(
        `
        SELECT installation_id, share_reports, allow_remote_checks, revoked_at, region_code,
               city_code, custom_city_name, area_source, operator_code, operator_source,
               app_version, device_alias
        FROM installations
        WHERE device_token_hash = ?1
        `,
      )
      .bind(hash)
      .first<InstallationRow>();
    if (!row) throw new ApiError("UNAUTHORIZED", "Device token is invalid", 401);
    if (row.revoked_at != null) throw new ApiError("INSTALLATION_REVOKED", "Installation is revoked", 403);
    return toInstallation(row);
  }

  async saveInstallationSettings(
    installationId: string,
    request: InstallationSettingsRequest,
    now: number,
  ): Promise<void> {
    validateRegionOperator(request.regionCode, request.operatorCode);
    validateAreaOperatorMetadata(request);
    const alias = sanitizeAlias(request.deviceAlias);
    await this.db
      .prepare(
        `
        UPDATE installations
        SET share_reports = ?2,
            allow_remote_checks = ?3,
            region_code = ?4,
            city_code = ?5,
            custom_city_name = ?6,
            area_source = ?7,
            operator_code = ?8,
            operator_source = ?9,
            device_alias = ?10,
            updated_at = ?11
        WHERE installation_id = ?1 AND revoked_at IS NULL
        `,
      )
      .bind(
        installationId,
        request.shareReports ? 1 : 0,
        request.allowRemoteChecks ? 1 : 0,
        request.regionCode,
        request.cityCode ?? null,
        sanitizeCustomCityName(request.customCityName),
        request.areaSource ?? null,
        request.operatorCode,
        request.operatorSource ?? null,
        alias,
        now,
      )
      .run();
  }

  async insertReport(installation: InstallationRecord, report: PublicReportRequest, now: number): Promise<{
    accepted: boolean;
    duplicate: boolean;
  }> {
    if (!installation.shareReports) {
      throw new ApiError("REPORT_SHARING_DISABLED", "Report sharing is disabled for this installation", 403);
    }
    validateReport(report, now);
    const existing = await this.db
      .prepare("SELECT report_id FROM reports WHERE report_id = ?1")
      .bind(report.reportId)
      .first<{ report_id: string }>();
    if (existing) return { accepted: true, duplicate: true };

    await this.db.batch([
      this.db
        .prepare(
          `
          INSERT INTO reports(
            report_id, installation_id, schema_version, app_version, checked_at, received_at,
            trigger_type, region_code, city_code, custom_city_name, operator_code,
            area_source, operator_source, whitelist_state, foreign_available,
            foreign_total, local_available, local_total, result_quality, is_conclusive,
            source_command_id
          ) VALUES(?1, ?2, 1, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20)
          `,
        )
        .bind(
          report.reportId,
          installation.installationId,
          report.appVersion,
          report.checkedAt,
          now,
          report.triggerType,
          report.regionCode,
          report.cityCode ?? null,
          sanitizeCustomCityName(report.customCityName),
          report.operatorCode,
          report.areaSource ?? null,
          report.operatorSource ?? null,
          report.whitelistState,
          report.foreign.available,
          report.foreign.total,
          report.local.available,
          report.local.total,
          report.resultQuality,
          isVoteState(report.whitelistState) ? 1 : 0,
          report.sourceCommandId ?? null,
        ),
      ...safeTargets(report).map((target) =>
        this.db
          .prepare(
            `
            INSERT INTO report_targets(report_id, target_code, target_group, target_status, latency_bucket)
            VALUES(?1, ?2, ?3, ?4, ?5)
            `,
          )
          .bind(report.reportId, target.targetCode, target.targetGroup, target.targetStatus, target.latencyBucket ?? null),
      ),
    ]);
    return { accepted: true, duplicate: false };
  }

  async getLatestSamples(
    regionCode: string,
    operatorCode: string,
    sinceMillis: number,
    cityCode?: string | null,
  ): Promise<ReportSample[]> {
    const cityFilter = cityCode ? "AND city_code = ?4" : "";
    const rows = await this.db
      .prepare(
        `
        SELECT installation_id, checked_at, city_code, whitelist_state, is_conclusive,
               foreign_available, foreign_total, local_available, local_total
        FROM reports
        WHERE region_code = ?1
          AND operator_code = ?2
          AND checked_at >= ?3
          ${cityFilter}
        ORDER BY checked_at DESC
        `,
      )
      .bind(...(cityCode ? [regionCode, operatorCode, sinceMillis, cityCode] : [regionCode, operatorCode, sinceMillis]))
      .all<ReportRow>();
    return rows.results.map((row) => ({
      installationId: row.installation_id,
      checkedAt: row.checked_at,
      cityCode: row.city_code ?? null,
      whitelistState: row.whitelist_state,
      isConclusive: Boolean(row.is_conclusive),
      foreignAvailable: row.foreign_available,
      foreignTotal: row.foreign_total,
      localAvailable: row.local_available,
      localTotal: row.local_total,
    }));
  }

  async createLinkCode(installationId: string, now: number): Promise<{ code: string; expiresAt: number }> {
    const code = generateLinkCode();
    const expiresAt = now + this.config.linkCodeTtlMinutes * 60_000;
    const codeHash = await hmacSha256Hex(this.requirePepper(), code);
    const codeId = randomId("link_code");
    await this.db.batch([
      this.db
        .prepare(
          `
          UPDATE link_codes
          SET used_at = ?2
          WHERE installation_id = ?1 AND used_at IS NULL AND expires_at > ?2
          `,
        )
        .bind(installationId, now),
      this.db
        .prepare(
          `
          INSERT INTO link_codes(link_code_id, link_code_hash, installation_id, created_at, expires_at)
          VALUES(?1, ?2, ?3, ?4, ?5)
          `,
        )
        .bind(codeId, codeHash, installationId, now, expiresAt),
    ]);
    return { code, expiresAt };
  }

  async linkTelegramChat(rawCode: string, chatId: string, now: number): Promise<LinkedDeviceRecord> {
    const code = normalizeLinkCode(rawCode);
    if (!code) throw new ApiError("INVALID_LINK_CODE", "Код привязки должен выглядеть как ABCD-EFGH", 400);
    const codeHash = await hmacSha256Hex(this.requirePepper(), code);
    const codeRow = await this.db
      .prepare(
        `
        SELECT lc.link_code_id, lc.installation_id, lc.expires_at, lc.used_at,
               i.revoked_at, i.device_alias, i.allow_remote_checks, i.last_seen_at, i.last_service_state
        FROM link_codes lc
        JOIN installations i ON i.installation_id = lc.installation_id
        WHERE lc.link_code_hash = ?1
        `,
      )
      .bind(codeHash)
      .first<LinkCodeJoinRow>();
    if (!codeRow) throw new ApiError("LINK_CODE_NOT_FOUND", "Код привязки не найден", 404);
    if (codeRow.revoked_at != null) throw new ApiError("INSTALLATION_REVOKED", "Устройство больше не активно", 403);
    if (codeRow.used_at != null) throw new ApiError("LINK_CODE_USED", "Код привязки уже использован", 409);
    if (codeRow.expires_at <= now) throw new ApiError("LINK_CODE_EXPIRED", "Код привязки истёк", 410);

    const linkId = randomId("link");
    await this.db.batch([
      this.db
        .prepare(
          `
          INSERT INTO installation_links(
            link_id, installation_id, chat_id, device_alias, created_at, created_by_link_code_id
          ) VALUES(?1, ?2, ?3, ?4, ?5, ?6)
          ON CONFLICT(installation_id, chat_id) WHERE revoked_at IS NULL DO UPDATE SET
            device_alias = excluded.device_alias
          `,
        )
        .bind(linkId, codeRow.installation_id, chatId, codeRow.device_alias ?? "Мой телефон", now, codeRow.link_code_id),
      this.db
        .prepare("UPDATE link_codes SET used_at = ?2, used_by_chat_id = ?3 WHERE link_code_id = ?1")
        .bind(codeRow.link_code_id, now, chatId),
    ]);
    return {
      linkId,
      installationId: codeRow.installation_id,
      deviceAlias: codeRow.device_alias ?? "Мой телефон",
      lastSeenAt: codeRow.last_seen_at,
      allowRemoteChecks: Boolean(codeRow.allow_remote_checks),
      lastServiceState: codeRow.last_service_state,
    };
  }

  async listLinksForInstallation(installationId: string): Promise<Array<{ linkId: string; chatId: string; deviceAlias: string }>> {
    const rows = await this.db
      .prepare(
        `
        SELECT link_id, chat_id, device_alias
        FROM installation_links
        WHERE installation_id = ?1 AND revoked_at IS NULL
        ORDER BY created_at DESC
        `,
      )
      .bind(installationId)
      .all<{ link_id: string; chat_id: string; device_alias: string | null }>();
    return rows.results.map((row) => ({
      linkId: row.link_id,
      chatId: row.chat_id,
      deviceAlias: row.device_alias ?? "Мой телефон",
    }));
  }

  async revokeLinkFromDevice(installationId: string, linkId: string, now: number): Promise<void> {
    await this.db
      .prepare(
        `
        UPDATE installation_links
        SET revoked_at = ?3
        WHERE installation_id = ?1 AND link_id = ?2 AND revoked_at IS NULL
        `,
      )
      .bind(installationId, linkId, now)
      .run();
    await this.cancelPendingCommandsForLink(linkId, now);
  }

  async revokeLinkFromTelegram(chatId: string, linkId: string, now: number): Promise<void> {
    await this.db
      .prepare(
        `
        UPDATE installation_links
        SET revoked_at = ?3
        WHERE chat_id = ?1 AND link_id = ?2 AND revoked_at IS NULL
        `,
      )
      .bind(chatId, linkId, now)
      .run();
    await this.cancelPendingCommandsForLink(linkId, now);
  }

  async listDevicesForChat(chatId: string): Promise<LinkedDeviceRecord[]> {
    const rows = await this.db
      .prepare(
        `
        SELECT l.link_id, l.installation_id, COALESCE(l.device_alias, i.device_alias, 'Мой телефон') AS device_alias,
               i.last_seen_at, i.allow_remote_checks, i.last_service_state
        FROM installation_links l
        JOIN installations i ON i.installation_id = l.installation_id
        WHERE l.chat_id = ?1
          AND l.revoked_at IS NULL
          AND i.revoked_at IS NULL
        ORDER BY l.created_at DESC
        `,
      )
      .bind(chatId)
      .all<LinkedDeviceRow>();
    return rows.results.map((row) => ({
      linkId: row.link_id,
      installationId: row.installation_id,
      deviceAlias: row.device_alias,
      lastSeenAt: row.last_seen_at,
      allowRemoteChecks: Boolean(row.allow_remote_checks),
      lastServiceState: row.last_service_state,
    }));
  }

  async createRemoteCheckCommand(params: {
    linkId: string;
    chatId: string;
    telegramMessageId: string | null;
    now: number;
  }): Promise<{ commandId: string; device: LinkedDeviceRecord }> {
    const device = await this.getAuthorizedLinkedDevice(params.linkId, params.chatId);
    if (!device.allowRemoteChecks) {
      throw new ApiError("REMOTE_CHECKS_DISABLED", "Удалённые проверки выключены на устройстве", 403);
    }
    if (!device.lastSeenAt || params.now - device.lastSeenAt > this.config.deviceOnlineTimeoutSeconds * 1000) {
      throw new ApiError("DEVICE_OFFLINE", "Устройство сейчас не подключено", 409);
    }
    const activeCommand = await this.db
      .prepare(
        `
        SELECT command_id FROM commands
        WHERE installation_id = ?1
          AND status IN ('PENDING', 'CLAIMED')
          AND expires_at > ?2
        LIMIT 1
        `,
      )
      .bind(device.installationId, params.now)
      .first<{ command_id: string }>();
    if (activeCommand) {
      throw new ApiError("REMOTE_COMMAND_ALREADY_ACTIVE", "Устройство уже ожидает или выполняет команду", 409);
    }
    const recentCommand = await this.db
      .prepare(
        `
        SELECT command_id FROM commands
        WHERE installation_id = ?1
          AND requested_by_chat_id = ?2
          AND created_at >= ?3
        LIMIT 1
        `,
      )
      .bind(device.installationId, params.chatId, params.now - this.config.commandCooldownSeconds * 1000)
      .first<{ command_id: string }>();
    if (recentCommand) {
      throw new ApiError("REMOTE_COMMAND_COOLDOWN", "Проверку можно запрашивать не чаще одного раза в минуту", 429);
    }
    const commandId = randomId("cmd");
    await this.db
      .prepare(
        `
        INSERT INTO commands(
          command_id, installation_id, requested_by_chat_id, command_type, status,
          created_at, expires_at, telegram_chat_id, telegram_message_id
        ) VALUES(?1, ?2, ?3, 'CHECK_NOW', 'PENDING', ?4, ?5, ?3, ?6)
        `,
      )
      .bind(
        commandId,
        device.installationId,
        params.chatId,
        params.now,
        params.now + this.config.commandTtlSeconds * 1000,
        params.telegramMessageId,
      )
      .run();
    return { commandId, device };
  }

  async serviceSync(
    installation: InstallationRecord,
    request: ServiceSyncRequest,
    now: number,
  ): Promise<ClaimedCommand | null> {
    await this.db
      .prepare(
        `
        UPDATE installations
        SET last_seen_at = ?2,
            last_active_session_id = ?3,
            last_service_started_at = ?4,
            last_service_state = ?5,
            app_version = ?6,
            updated_at = ?2
        WHERE installation_id = ?1 AND revoked_at IS NULL
        `,
      )
      .bind(
        installation.installationId,
        now,
        request.serviceSessionId,
        request.serviceStartedAt,
        request.serviceState,
        request.appVersion,
      )
      .run();

    if (!installation.allowRemoteChecks || request.checkInProgress) return null;

    const command = await this.db
      .prepare(
        `
        SELECT command_id, command_type, expires_at
        FROM commands
        WHERE installation_id = ?1
          AND status = 'PENDING'
          AND expires_at > ?2
        ORDER BY created_at ASC
        LIMIT 1
        `,
      )
      .bind(installation.installationId, now)
      .first<{ command_id: string; command_type: "CHECK_NOW"; expires_at: number }>();
    if (!command) return null;

    const update = await this.db
      .prepare(
        `
        UPDATE commands
        SET status = 'CLAIMED', claimed_at = ?3, service_session_id = ?4
        WHERE command_id = ?1 AND status = 'PENDING' AND expires_at > ?2
        `,
      )
      .bind(command.command_id, now, now, request.serviceSessionId)
      .run();
    if (update.meta.changes !== 1) return null;
    return {
      commandId: command.command_id,
      type: command.command_type,
      expiresAt: command.expires_at,
    };
  }

  async saveCommandResult(
    installation: InstallationRecord,
    commandId: string,
    request: CommandResultRequest,
    now: number,
  ): Promise<{ command: CommandRecord; duplicate: boolean }> {
    const command = await this.getCommand(commandId);
    if (!command) throw new ApiError("COMMAND_NOT_FOUND", "Command not found", 404);
    if (command.installationId !== installation.installationId) {
      throw new ApiError("COMMAND_INSTALLATION_MISMATCH", "Command does not belong to this installation", 403);
    }
    if (command.serviceSessionId && command.serviceSessionId !== request.serviceSessionId) {
      throw new ApiError("WRONG_SERVICE_SESSION", "Command was claimed by another service session", 409);
    }

    const existing = await this.db
      .prepare("SELECT command_id FROM command_results WHERE command_id = ?1")
      .bind(commandId)
      .first<{ command_id: string }>();
    if (existing) return { command, duplicate: true };

    validateCommandResult(request, now);
    const finalStatus = request.outcome === "SUCCESS" ? "COMPLETED" : "FAILED";
    await this.db.batch([
      this.db
        .prepare(
          `
          INSERT INTO command_results(
            command_id, received_at, outcome, whitelist_state, foreign_available, foreign_total,
            local_available, local_total, error_code, checked_at
          ) VALUES(?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)
          `,
        )
        .bind(
          commandId,
          now,
          request.outcome,
          request.whitelistState ?? null,
          request.foreign?.available ?? null,
          request.foreign?.total ?? null,
          request.local?.available ?? null,
          request.local?.total ?? null,
          request.errorCode ?? null,
          request.checkedAt,
        ),
      this.db
        .prepare(
          `
          UPDATE commands
          SET status = ?2, completed_at = ?3, result_status = ?4, error_code = ?5
          WHERE command_id = ?1
          `,
        )
        .bind(commandId, finalStatus, now, request.outcome, request.errorCode ?? null),
    ]);
    return { command: { ...command, status: finalStatus }, duplicate: false };
  }

  async markCommandTelegramResultSent(commandId: string, now: number): Promise<boolean> {
    const update = await this.db
      .prepare(
        `
        UPDATE commands
        SET telegram_result_sent_at = ?2
        WHERE command_id = ?1 AND telegram_result_sent_at IS NULL
        `,
      )
      .bind(commandId, now)
      .run();
    return update.meta.changes === 1;
  }

  async getCommand(commandId: string): Promise<CommandRecord | null> {
    const row = await this.db
      .prepare(
        `
        SELECT command_id, installation_id, requested_by_chat_id, telegram_chat_id,
               telegram_message_id, status, expires_at, service_session_id, telegram_result_sent_at
        FROM commands
        WHERE command_id = ?1
        `,
      )
      .bind(commandId)
      .first<CommandRow>();
    if (!row) return null;
    return {
      commandId: row.command_id,
      installationId: row.installation_id,
      requestedByChatId: row.requested_by_chat_id,
      telegramChatId: row.telegram_chat_id,
      telegramMessageId: row.telegram_message_id,
      status: row.status,
      expiresAt: row.expires_at,
      serviceSessionId: row.service_session_id,
      telegramResultSentAt: row.telegram_result_sent_at,
    };
  }

  async createPublicReportFromCommandResult(
    installation: InstallationRecord,
    commandId: string,
    result: CommandResultRequest,
    now: number,
  ): Promise<void> {
    if (!installation.shareReports || result.outcome !== "SUCCESS" || !result.whitelistState || !result.foreign || !result.local) {
      return;
    }
    if (!installation.regionCode || !installation.operatorCode) return;
    await this.insertReport(
      installation,
      {
        schemaVersion: 1,
        requestId: `remote-${commandId}`,
        reportId: `remote-${commandId}`,
        checkedAt: result.checkedAt,
        triggerType: "REMOTE_TELEGRAM",
        appVersion: installation.appVersion ?? "unknown",
        regionCode: installation.regionCode,
        operatorCode: installation.operatorCode,
        whitelistState: result.whitelistState,
        resultQuality: isVoteState(result.whitelistState) ? "CONCLUSIVE" : "INCONCLUSIVE",
        foreign: result.foreign,
        local: result.local,
        targets: [],
        sourceCommandId: commandId,
      },
      now,
    );
  }

  async revokeInstallation(installationId: string, now: number): Promise<void> {
    await this.db.batch([
      this.db
        .prepare("UPDATE installations SET revoked_at = ?2, updated_at = ?2 WHERE installation_id = ?1")
        .bind(installationId, now),
      this.db
        .prepare("UPDATE installation_links SET revoked_at = ?2 WHERE installation_id = ?1 AND revoked_at IS NULL")
        .bind(installationId, now),
      this.db
        .prepare("UPDATE commands SET status = 'CANCELLED' WHERE installation_id = ?1 AND status IN ('PENDING', 'CLAIMED')")
        .bind(installationId),
    ]);
  }

  async upsertTelegramUser(chatId: string, telegramUserId: string | null, languageCode: string | null, now: number): Promise<void> {
    await this.db
      .prepare(
        `
        INSERT INTO telegram_users(
          chat_id, telegram_user_id, created_at, updated_at, language_code, last_interaction_at
        ) VALUES(?1, ?2, ?3, ?3, ?4, ?3)
        ON CONFLICT(chat_id) DO UPDATE SET
          telegram_user_id = COALESCE(excluded.telegram_user_id, telegram_users.telegram_user_id),
          language_code = COALESCE(excluded.language_code, telegram_users.language_code),
          updated_at = excluded.updated_at,
          last_interaction_at = excluded.last_interaction_at
        `,
      )
      .bind(chatId, telegramUserId, now, languageCode)
      .run();
  }

  async isTelegramUserBlocked(chatId: string): Promise<boolean> {
    const row = await this.db
      .prepare("SELECT is_blocked FROM telegram_users WHERE chat_id = ?1")
      .bind(chatId)
      .first<{ is_blocked: number }>();
    return Boolean(row?.is_blocked);
  }

  async getTelegramPreference(chatId: string): Promise<{ regionCode: string | null; operatorCode: OperatorCode | null }> {
    const row = await this.db
      .prepare("SELECT selected_region_code, selected_operator_code FROM telegram_preferences WHERE chat_id = ?1")
      .bind(chatId)
      .first<{ selected_region_code: string | null; selected_operator_code: OperatorCode | null }>();
    return {
      regionCode: row?.selected_region_code ?? null,
      operatorCode: row?.selected_operator_code ?? null,
    };
  }

  async saveTelegramRegion(chatId: string, regionCode: string, now: number): Promise<void> {
    if (!isKnownRegion(regionCode)) throw new ApiError("UNKNOWN_REGION", "Неизвестный регион", 400);
    await this.db
      .prepare(
        `
        INSERT INTO telegram_preferences(chat_id, selected_region_code, updated_at)
        VALUES(?1, ?2, ?3)
        ON CONFLICT(chat_id) DO UPDATE SET selected_region_code = excluded.selected_region_code, updated_at = excluded.updated_at
        `,
      )
      .bind(chatId, regionCode, now)
      .run();
  }

  async saveTelegramOperator(chatId: string, operatorCode: string, now: number): Promise<void> {
    if (!isKnownOperator(operatorCode)) throw new ApiError("UNKNOWN_OPERATOR", "Неизвестный оператор", 400);
    await this.db
      .prepare(
        `
        INSERT INTO telegram_preferences(chat_id, selected_operator_code, updated_at)
        VALUES(?1, ?2, ?3)
        ON CONFLICT(chat_id) DO UPDATE SET selected_operator_code = excluded.selected_operator_code, updated_at = excluded.updated_at
        `,
      )
      .bind(chatId, operatorCode, now)
      .run();
  }

  async markTelegramUpdateProcessing(updateId: number, now: number): Promise<boolean> {
    const result = await this.db
      .prepare("INSERT OR IGNORE INTO processed_telegram_updates(update_id, processed_at) VALUES(?1, ?2)")
      .bind(updateId, now)
      .run();
    return result.meta.changes === 1;
  }

  async saveFeedback(chatId: string, telegramUserId: string | null, message: string, commandContext: string | null, now: number): Promise<void> {
    const normalized = message.trim();
    if (normalized.length < 2) throw new ApiError("FEEDBACK_EMPTY", "Напишите текст обратной связи после /feedback", 400);
    if (normalized.length > 1000) throw new ApiError("FEEDBACK_TOO_LONG", "Сообщение слишком длинное", 400);
    await this.db
      .prepare(
        `
        INSERT INTO feedback(feedback_id, chat_id, telegram_user_id, message, created_at, command_context)
        VALUES(?1, ?2, ?3, ?4, ?5, ?6)
        `,
      )
      .bind(randomId("fb"), chatId, telegramUserId, normalized, now, commandContext)
      .run();
  }

  async cleanup(now: number): Promise<void> {
    await this.db.batch([
      this.db
        .prepare("DELETE FROM link_codes WHERE expires_at < ?1")
        .bind(now - 60 * 60 * 1000),
      this.db
        .prepare("UPDATE commands SET status = 'EXPIRED' WHERE status IN ('PENDING', 'CLAIMED') AND expires_at < ?1")
        .bind(now),
      this.db
        .prepare("DELETE FROM commands WHERE created_at < ?1 AND status IN ('COMPLETED', 'FAILED', 'EXPIRED', 'CANCELLED')")
        .bind(now - this.config.commandRetentionHours * 60 * 60 * 1000),
      this.db
        .prepare("DELETE FROM reports WHERE received_at < ?1")
        .bind(now - this.config.reportRetentionDays * 24 * 60 * 60 * 1000),
      this.db
        .prepare("DELETE FROM processed_telegram_updates WHERE processed_at < ?1")
        .bind(now - 7 * 24 * 60 * 60 * 1000),
      this.db
        .prepare("DELETE FROM rate_limits WHERE window_start < ?1")
        .bind(now - 24 * 60 * 60 * 1000),
    ]);
  }

  private async getAuthorizedLinkedDevice(linkId: string, chatId: string): Promise<LinkedDeviceRecord> {
    const row = await this.db
      .prepare(
        `
        SELECT l.link_id, l.installation_id, COALESCE(l.device_alias, i.device_alias, 'Мой телефон') AS device_alias,
               i.last_seen_at, i.allow_remote_checks, i.last_service_state
        FROM installation_links l
        JOIN installations i ON i.installation_id = l.installation_id
        WHERE l.link_id = ?1
          AND l.chat_id = ?2
          AND l.revoked_at IS NULL
          AND i.revoked_at IS NULL
        `,
      )
      .bind(linkId, chatId)
      .first<LinkedDeviceRow>();
    if (!row) throw new ApiError("DEVICE_LINK_NOT_FOUND", "Устройство не найдено или отвязано", 404);
    return {
      linkId: row.link_id,
      installationId: row.installation_id,
      deviceAlias: row.device_alias,
      lastSeenAt: row.last_seen_at,
      allowRemoteChecks: Boolean(row.allow_remote_checks),
      lastServiceState: row.last_service_state,
    };
  }

  private async cancelPendingCommandsForLink(linkId: string, now: number): Promise<void> {
    const device = await this.db
      .prepare("SELECT installation_id, chat_id FROM installation_links WHERE link_id = ?1")
      .bind(linkId)
      .first<{ installation_id: string; chat_id: string }>();
    if (!device) return;
    await this.db
      .prepare(
        `
        UPDATE commands
        SET status = 'CANCELLED', completed_at = ?3
        WHERE installation_id = ?1
          AND requested_by_chat_id = ?2
          AND status IN ('PENDING', 'CLAIMED')
        `,
      )
      .bind(device.installation_id, device.chat_id, now)
      .run();
  }

  private requirePepper(): string {
    if (!this.env.DEVICE_TOKEN_PEPPER) {
      throw new ApiError("SERVER_NOT_CONFIGURED", "Service secret is not configured", 500);
    }
    return this.env.DEVICE_TOKEN_PEPPER;
  }
}

function parseBearer(authorization: string | null): string | null {
  const match = authorization?.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim() || null;
}

function validateRegionOperator(regionCode: string, operatorCode: string): void {
  if (!isKnownRegion(regionCode)) throw new ApiError("UNKNOWN_REGION", "Unknown region", 400);
  if (!isKnownOperator(operatorCode)) throw new ApiError("UNKNOWN_OPERATOR", "Unknown operator", 400);
}

function validateAreaOperatorMetadata(request: {
  regionCode: string;
  cityCode?: string | null;
  areaSource?: string | null;
  operatorSource?: string | null;
}): void {
  if (!isKnownCity(request.regionCode, request.cityCode)) {
    throw new ApiError("UNKNOWN_CITY", "Unknown city for selected region", 400);
  }
  if (request.areaSource && !VALID_AREA_SOURCES.has(request.areaSource)) {
    throw new ApiError("UNKNOWN_AREA_SOURCE", "Unknown area source", 400);
  }
  if (request.operatorSource && !VALID_OPERATOR_SOURCES.has(request.operatorSource)) {
    throw new ApiError("UNKNOWN_OPERATOR_SOURCE", "Unknown operator source", 400);
  }
}

function sanitizeAlias(alias: string | undefined): string | null {
  const normalized = alias?.trim();
  if (!normalized) return null;
  if (normalized.length > 64) throw new ApiError("DEVICE_ALIAS_TOO_LONG", "Device alias is too long", 400);
  return normalized;
}

function sanitizeCustomCityName(value: string | null | undefined): string | null {
  const normalized = value?.replace(/[\u0000-\u001f\u007f]/g, "").trim().replace(/\s+/g, " ");
  if (!normalized) return null;
  if (normalized.length > 64) throw new ApiError("CUSTOM_CITY_TOO_LONG", "Custom city name is too long", 400);
  return normalized;
}

function validateReport(report: PublicReportRequest, now: number): void {
  validateRegionOperator(report.regionCode, report.operatorCode);
  validateAreaOperatorMetadata(report);
  if (!/^[a-zA-Z0-9._:-]{8,128}$/.test(report.reportId)) {
    throw new ApiError("INVALID_REPORT_ID", "reportId is invalid", 400);
  }
  validateCountPair(report.foreign, "foreign");
  validateCountPair(report.local, "local");
  if (!VALID_STATES.has(report.whitelistState)) throw new ApiError("UNKNOWN_WHITELIST_STATE", "Unknown whitelist state", 400);
  if (!VALID_QUALITIES.has(report.resultQuality)) throw new ApiError("UNKNOWN_RESULT_QUALITY", "Unknown result quality", 400);
  const maxAgeMs = 24 * 60 * 60 * 1000;
  const maxFutureMs = 10 * 60 * 1000;
  if (report.checkedAt < now - maxAgeMs || report.checkedAt > now + maxFutureMs) {
    throw new ApiError("INVALID_CHECKED_AT", "checkedAt is outside the accepted window", 400);
  }
  if ((report.targets?.length ?? 0) > 32) throw new ApiError("TOO_MANY_TARGETS", "Too many targets", 400);
}

function validateCommandResult(result: CommandResultRequest, now: number): void {
  if (result.checkedAt < now - 24 * 60 * 60 * 1000 || result.checkedAt > now + 10 * 60 * 1000) {
    throw new ApiError("INVALID_CHECKED_AT", "checkedAt is outside the accepted window", 400);
  }
  if (result.outcome === "SUCCESS") {
    if (!result.whitelistState || !result.foreign || !result.local) {
      throw new ApiError("INVALID_COMMAND_RESULT", "Successful command result must include status and counts", 400);
    }
    validateCountPair(result.foreign, "foreign");
    validateCountPair(result.local, "local");
  } else if (!result.errorCode) {
    throw new ApiError("INVALID_COMMAND_RESULT", "Non-success command result must include errorCode", 400);
  }
}

function validateCountPair(pair: { available: number; total: number }, name: string): void {
  if (!Number.isInteger(pair.available) || !Number.isInteger(pair.total)) {
    throw new ApiError("INVALID_COUNTS", `${name} counts must be integers`, 400);
  }
  if (pair.available < 0 || pair.total < 0 || pair.available > pair.total) {
    throw new ApiError("INVALID_COUNTS", `${name} counts are invalid`, 400);
  }
}

function safeTargets(report: PublicReportRequest) {
  return (report.targets ?? []).map((target) => ({
    targetCode: target.targetCode.trim().slice(0, 80),
    targetGroup: target.targetGroup,
    targetStatus: target.targetStatus,
    latencyBucket: target.latencyBucket?.trim().slice(0, 40),
  }));
}

function isVoteState(state: string): boolean {
  return state === "LIKELY_ENABLED" || state === "LIKELY_DISABLED";
}

function toInstallation(row: InstallationRow): InstallationRecord {
  return {
    installationId: row.installation_id,
    shareReports: Boolean(row.share_reports),
    allowRemoteChecks: Boolean(row.allow_remote_checks),
    revokedAt: row.revoked_at,
    regionCode: row.region_code,
    cityCode: row.city_code,
    customCityName: row.custom_city_name,
    areaSource: row.area_source,
    operatorCode: row.operator_code,
    operatorSource: row.operator_source,
    appVersion: row.app_version,
    deviceAlias: row.device_alias,
  };
}

const VALID_STATES = new Set([
  "LIKELY_ENABLED",
  "LIKELY_DISABLED",
  "INCONCLUSIVE",
  "NO_MOBILE_INTERNET",
  "MOBILE_DNS_FAILURE",
  "PARTIAL_PROBLEM",
  "CELLULAR_NETWORK_UNAVAILABLE",
]);

const VALID_QUALITIES = new Set(["CONCLUSIVE", "PARTIAL", "INCONCLUSIVE"]);
const VALID_AREA_SOURCES = new Set(["AUTOMATIC_LOCATION", "MANUAL_SELECTION"]);
const VALID_OPERATOR_SOURCES = new Set(["NETWORK_OPERATOR", "SIM_OPERATOR", "MANUAL", "UNKNOWN"]);

interface InstallationRow {
  installation_id: string;
  share_reports: number;
  allow_remote_checks: number;
  revoked_at: number | null;
  region_code: string | null;
  city_code: string | null;
  custom_city_name: string | null;
  area_source: InstallationRecord["areaSource"];
  operator_code: OperatorCode | null;
  operator_source: InstallationRecord["operatorSource"];
  app_version: string | null;
  device_alias: string | null;
}

interface ReportRow {
  installation_id: string;
  checked_at: number;
  city_code: string | null;
  whitelist_state: ReportSample["whitelistState"];
  is_conclusive: number;
  foreign_available: number;
  foreign_total: number;
  local_available: number;
  local_total: number;
}

interface LinkCodeJoinRow {
  link_code_id: string;
  installation_id: string;
  expires_at: number;
  used_at: number | null;
  revoked_at: number | null;
  device_alias: string | null;
  allow_remote_checks: number;
  last_seen_at: number | null;
  last_service_state: string | null;
}

interface LinkedDeviceRow {
  link_id: string;
  installation_id: string;
  device_alias: string;
  last_seen_at: number | null;
  allow_remote_checks: number;
  last_service_state: string | null;
}

interface CommandRow {
  command_id: string;
  installation_id: string;
  requested_by_chat_id: string;
  telegram_chat_id: string;
  telegram_message_id: string | null;
  status: CommandRecord["status"];
  expires_at: number;
  service_session_id: string | null;
  telegram_result_sent_at: number | null;
}
