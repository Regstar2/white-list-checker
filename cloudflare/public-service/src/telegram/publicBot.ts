import { loadConfig } from "../config";
import { PublicStatusAggregator } from "../domain/publicStatusAggregator";
import { ApiError } from "../http/errors";
import { D1PublicServiceRepository } from "../repositories/d1PublicServiceRepository";
import { D1RateLimiter } from "../security/rateLimit";
import type { Env, LinkedDeviceRecord, TelegramCallbackQuery, TelegramMessage, TelegramUpdate } from "../types";
import {
  deviceKeyboard,
  devicesKeyboard,
  mainKeyboard,
  operatorKeyboard,
  regionKeyboard,
  statusKeyboard,
  unlinkConfirmKeyboard,
} from "./keyboards";
import {
  aboutText,
  chooseOperatorText,
  chooseRegionText,
  deviceNotFoundText,
  deviceText,
  devicesText,
  feedbackPromptText,
  feedbackSavedText,
  helpText,
  linkedDeviceText,
  missingStatusSelectionText,
  operatorSavedText,
  remoteRequestQueuedText,
  regionSavedText,
  safeErrorText,
  startText,
  statusText,
  unlinkConfirmationText,
  unlinkSuccessText,
} from "./publicBotFormatter";
import { TelegramClient } from "./telegramClient";

export class PublicTelegramBot {
  private readonly repo: D1PublicServiceRepository;
  private readonly telegram: TelegramClient;
  private readonly config;
  private readonly limiter: D1RateLimiter;

  constructor(private readonly env: Env) {
    this.repo = new D1PublicServiceRepository(env.DB, env);
    this.telegram = new TelegramClient(env);
    this.config = loadConfig(env);
    this.limiter = new D1RateLimiter(env.DB);
  }

  async handleUpdate(update: TelegramUpdate, now = Date.now()): Promise<void> {
    const shouldProcess = await this.repo.markTelegramUpdateProcessing(update.update_id, now);
    if (!shouldProcess) return;

    if (update.message) {
      await this.handleMessage(update.message, now);
      return;
    }
    if (update.callback_query) {
      await this.handleCallback(update.callback_query, now);
    }
  }

  private async handleMessage(message: TelegramMessage, now: number): Promise<void> {
    const chatId = String(message.chat.id);
    await this.registerInteraction(chatId, message.from?.id, message.from?.language_code ?? null, now);
    if (await this.repo.isTelegramUserBlocked(chatId)) {
      await this.telegram.sendMessage(chatId, "Доступ к боту ограничен.");
      return;
    }
    await this.limiter.check(`bot:${chatId}`, { maxRequests: 30, windowSeconds: 60 }, now);
    const text = message.text?.trim() ?? "";
    const [commandRaw, ...args] = text.split(/\s+/);
    const command = substringBefore(commandRaw, "@").toLowerCase();
    switch (command) {
      case "/start":
        await this.sendMainMenu(chatId, startText());
        return;
      case "/help":
        await this.telegram.sendMessage(chatId, helpText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      case "/status":
        await this.sendStatus(chatId, now);
        return;
      case "/region":
        await this.telegram.sendMessage(chatId, chooseRegionText(), regionKeyboard());
        return;
      case "/operator":
        await this.telegram.sendMessage(chatId, chooseOperatorText(), operatorKeyboard());
        return;
      case "/devices":
        await this.sendDevices(chatId, now);
        return;
      case "/link":
        await this.linkDevice(chatId, args.join(" "), now);
        return;
      case "/about":
        await this.telegram.sendMessage(chatId, aboutText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      case "/feedback":
        await this.saveFeedback(chatId, message.from?.id, args.join(" "), "/feedback", now);
        return;
      default:
        if (text) {
          await this.telegram.sendMessage(chatId, helpText(), mainKeyboard(await this.hasDevices(chatId)));
        }
    }
  }

  private async handleCallback(callback: TelegramCallbackQuery, now: number): Promise<void> {
    const message = callback.message;
    if (!message) {
      await this.telegram.answerCallbackQuery(callback.id);
      return;
    }
    const chatId = String(message.chat.id);
    await this.registerInteraction(chatId, callback.from?.id, callback.from.language_code ?? null, now);
    await this.telegram.answerCallbackQuery(callback.id);
    if (await this.repo.isTelegramUserBlocked(chatId)) {
      await this.telegram.sendMessage(chatId, "Доступ к боту ограничен.");
      return;
    }
    await this.limiter.check(`bot:${chatId}`, { maxRequests: 40, windowSeconds: 60 }, now);

    const parts = (callback.data ?? "").split(":");
    if (parts[0] !== "v1") return;
    const action = parts[1] ?? "";
    const id = parts[2];

    switch (action) {
      case "menu":
        await this.telegram.editMessageText(chatId, message.message_id, startText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      case "help":
        await this.telegram.editMessageText(chatId, message.message_id, helpText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      case "status":
      case "status-refresh":
        await this.sendStatus(chatId, now, message.message_id);
        return;
      case "regions":
        await this.telegram.editMessageText(chatId, message.message_id, chooseRegionText(), regionKeyboard());
        return;
      case "operators":
        await this.telegram.editMessageText(chatId, message.message_id, chooseOperatorText(), operatorKeyboard());
        return;
      case "region":
        if (id) {
          await this.repo.saveTelegramRegion(chatId, id, now);
          await this.telegram.editMessageText(chatId, message.message_id, regionSavedText(), mainKeyboard(await this.hasDevices(chatId)));
        }
        return;
      case "operator":
        if (id) {
          await this.repo.saveTelegramOperator(chatId, id, now);
          await this.telegram.editMessageText(chatId, message.message_id, operatorSavedText(), mainKeyboard(await this.hasDevices(chatId)));
        }
        return;
      case "devices":
        await this.sendDevices(chatId, now, message.message_id);
        return;
      case "device":
        if (id) await this.sendDevice(chatId, id, message.message_id, now);
        return;
      case "check":
        if (id) await this.createRemoteCommand(chatId, id, message.message_id, now);
        return;
      case "unlink-request":
        if (id) await this.confirmUnlink(chatId, id, message.message_id);
        return;
      case "unlink-confirm":
        if (id) await this.unlinkDevice(chatId, id, message.message_id, now);
        return;
      case "unlink-cancel":
        if (id) await this.sendDevice(chatId, id, message.message_id, now);
        return;
      case "about":
        await this.telegram.editMessageText(chatId, message.message_id, aboutText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      case "feedback":
        await this.telegram.editMessageText(chatId, message.message_id, feedbackPromptText(), mainKeyboard(await this.hasDevices(chatId)));
        return;
      default:
        return;
    }
  }

  private async sendMainMenu(chatId: string, text: string): Promise<void> {
    await this.telegram.sendMessage(chatId, text, mainKeyboard(await this.hasDevices(chatId)));
  }

  private async sendStatus(chatId: string, now: number, editMessageId?: number): Promise<void> {
    const prefs = await this.repo.getTelegramPreference(chatId);
    if (!prefs.regionCode || !prefs.operatorCode) {
      const text = missingStatusSelectionText();
      if (editMessageId) await this.telegram.editMessageText(chatId, editMessageId, text, regionKeyboard());
      else await this.telegram.sendMessage(chatId, text, regionKeyboard());
      return;
    }
    const primarySince = now - this.config.reportPrimaryWindowMinutes * 60_000;
    const fallbackSince = now - this.config.reportFallbackWindowMinutes * 60_000;
    const [primarySamples, fallbackSamples] = await Promise.all([
      this.repo.getLatestSamples(prefs.regionCode, prefs.operatorCode, primarySince),
      this.repo.getLatestSamples(prefs.regionCode, prefs.operatorCode, fallbackSince),
    ]);
    const result = new PublicStatusAggregator().aggregate({
      regionCode: prefs.regionCode,
      operatorCode: prefs.operatorCode,
      currentTime: now,
      primarySamples,
      fallbackSamples,
      policy: {
        primaryWindowMinutes: this.config.reportPrimaryWindowMinutes,
        fallbackWindowMinutes: this.config.reportFallbackWindowMinutes,
        minUniqueInstallations: this.config.minUniqueInstallations,
        consensusNumerator: this.config.consensusNumerator,
        consensusDenominator: this.config.consensusDenominator,
      },
    });
    const text = statusText(result);
    if (editMessageId) await this.telegram.editMessageText(chatId, editMessageId, text, statusKeyboard());
    else await this.telegram.sendMessage(chatId, text, statusKeyboard());
  }

  private async sendDevices(chatId: string, now: number, editMessageId?: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const text = devicesText(devices, now, this.config.deviceOnlineTimeoutSeconds);
    const keyboard = devices.length > 0 ? devicesKeyboard(devices) : mainKeyboard(false);
    if (editMessageId) await this.telegram.editMessageText(chatId, editMessageId, text, keyboard);
    else await this.telegram.sendMessage(chatId, text, keyboard);
  }

  private async sendDevice(chatId: string, linkId: string, editMessageId: number, now: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const device = findDevice(devices, linkId);
    if (!device) {
      await this.telegram.editMessageText(chatId, editMessageId, deviceNotFoundText(), devices.length > 0 ? devicesKeyboard(devices) : mainKeyboard(false));
      return;
    }
    await this.telegram.editMessageText(
      chatId,
      editMessageId,
      deviceText(device, now, this.config.deviceOnlineTimeoutSeconds),
      deviceKeyboard(linkId),
    );
  }

  private async confirmUnlink(chatId: string, linkId: string, editMessageId: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const device = findDevice(devices, linkId);
    if (!device) {
      await this.telegram.editMessageText(chatId, editMessageId, deviceNotFoundText(), devices.length > 0 ? devicesKeyboard(devices) : mainKeyboard(false));
      return;
    }
    await this.telegram.editMessageText(chatId, editMessageId, unlinkConfirmationText(device.deviceAlias), unlinkConfirmKeyboard(linkId));
  }

  private async unlinkDevice(chatId: string, linkId: string, editMessageId: number, now: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const device = findDevice(devices, linkId);
    if (!device) {
      await this.telegram.editMessageText(chatId, editMessageId, deviceNotFoundText(), devices.length > 0 ? devicesKeyboard(devices) : mainKeyboard(false));
      return;
    }
    await this.repo.revokeLinkFromTelegram(chatId, linkId, now);
    const updatedDevices = await this.repo.listDevicesForChat(chatId);
    const text = [unlinkSuccessText(device.deviceAlias), "", devicesText(updatedDevices, now, this.config.deviceOnlineTimeoutSeconds)].join("\n");
    await this.telegram.editMessageText(chatId, editMessageId, text, updatedDevices.length > 0 ? devicesKeyboard(updatedDevices) : mainKeyboard(false));
  }

  private async createRemoteCommand(chatId: string, linkId: string, messageId: number, now: number): Promise<void> {
    try {
      const { device } = await this.repo.createRemoteCheckCommand({
        linkId,
        chatId,
        telegramMessageId: String(messageId),
        now,
      });
      await this.telegram.editMessageText(chatId, messageId, remoteRequestQueuedText(device.deviceAlias), deviceKeyboard(linkId));
    } catch (error) {
      const code = error instanceof ApiError ? error.code : "UNKNOWN";
      await this.telegram.editMessageText(chatId, messageId, safeErrorText(code), deviceKeyboard(linkId));
    }
  }

  private async linkDevice(chatId: string, code: string, now: number): Promise<void> {
    try {
      const device = await this.repo.linkTelegramChat(code, chatId, now);
      await this.telegram.sendMessage(chatId, linkedDeviceText(device), deviceKeyboard(device.linkId));
    } catch (error) {
      const text = error instanceof ApiError ? error.message : "Не удалось привязать устройство.";
      await this.telegram.sendMessage(chatId, text);
    }
  }

  private async saveFeedback(
    chatId: string,
    telegramUserId: number | undefined,
    text: string,
    context: string,
    now: number,
  ): Promise<void> {
    try {
      await this.repo.saveFeedback(chatId, telegramUserId == null ? null : String(telegramUserId), text, context, now);
      await this.telegram.sendMessage(chatId, feedbackSavedText());
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "Не удалось сохранить сообщение.";
      await this.telegram.sendMessage(chatId, message);
    }
  }

  private async hasDevices(chatId: string): Promise<boolean> {
    return (await this.repo.listDevicesForChat(chatId)).length > 0;
  }

  private async registerInteraction(
    chatId: string,
    telegramUserId: number | undefined,
    languageCode: string | null,
    now: number,
  ): Promise<void> {
    await this.repo.upsertTelegramUser(chatId, telegramUserId == null ? null : String(telegramUserId), languageCode, now);
  }
}

function findDevice(devices: LinkedDeviceRecord[], linkId: string): LinkedDeviceRecord | undefined {
  return devices.find((item) => item.linkId === linkId);
}

function substringBefore(value: string, separator: string): string {
  const index = value.indexOf(separator);
  if (index < 0) {
    return value;
  }
  return value.slice(0, index);
}
