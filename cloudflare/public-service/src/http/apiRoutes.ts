import { PublicStatusAggregator } from "../domain/publicStatusAggregator";
import { CITIES, OPERATORS, REGIONS } from "../domain/catalog";
import { ApiError, jsonResponse, methodNotAllowed, notFound } from "./errors";
import { readJsonBody, requireSchemaVersion } from "./json";
import { D1RateLimiter } from "../security/rateLimit";
import type {
  CommandResultRequest,
  Env,
  InstallationSettingsRequest,
  PublicReportRequest,
  RegisterInstallationRequest,
  ServiceSyncRequest,
} from "../types";
import { D1PublicServiceRepository } from "../repositories/d1PublicServiceRepository";
import { TelegramClient } from "../telegram/telegramClient";
import { remoteResultText } from "../telegram/publicBotFormatter";

export async function handleApiRequest(request: Request, env: Env, now = Date.now()): Promise<Response> {
  const url = new URL(request.url);
  const repo = new D1PublicServiceRepository(env.DB, env);
  const limiter = new D1RateLimiter(env.DB);

  if (url.pathname === "/api/v1/catalog/regions") {
    if (request.method !== "GET") return methodNotAllowed();
    await limiter.check(`catalog:${clientIp(request)}`, { maxRequests: 120, windowSeconds: 3600 }, now);
    return catalogResponse(REGIONS.filter((region) => region.code !== "UNKNOWN"));
  }

  const citiesMatch = url.pathname.match(/^\/api\/v1\/catalog\/regions\/([^/]+)\/cities$/);
  if (citiesMatch) {
    if (request.method !== "GET") return methodNotAllowed();
    await limiter.check(`catalog:${clientIp(request)}`, { maxRequests: 120, windowSeconds: 3600 }, now);
    return catalogResponse(CITIES.filter((city) => city.regionCode === citiesMatch[1]));
  }

  if (url.pathname === "/api/v1/catalog/operators") {
    if (request.method !== "GET") return methodNotAllowed();
    await limiter.check(`catalog:${clientIp(request)}`, { maxRequests: 120, windowSeconds: 3600 }, now);
    return catalogResponse(OPERATORS.filter((operator) => operator.code !== "UNKNOWN"));
  }

  if (url.pathname === "/api/v1/installations/register") {
    if (request.method !== "POST") return methodNotAllowed();
    await limiter.check(`register:${clientIp(request)}`, { maxRequests: 10, windowSeconds: 3600 }, now);
    const body = await readJsonBody<RegisterInstallationRequest>(request);
    requireSchemaVersion(body);
    if (body.platform !== "ANDROID") {
      throw new ApiError("UNSUPPORTED_PLATFORM", "Only Android is supported", 400);
    }
    if (!body.appVersion || body.appVersion.length > 40) {
      throw new ApiError("INVALID_APP_VERSION", "appVersion is invalid", 400);
    }
    const result = await repo.registerInstallation(body.appVersion, now);
    return jsonResponse({
      schemaVersion: 1,
      requestId: body.requestId,
      installationId: result.installationId,
      deviceToken: result.deviceToken,
      createdAt: result.createdAt,
    });
  }

  if (url.pathname === "/api/v1/installations/me/settings") {
    if (request.method !== "PUT") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    const body = await readJsonBody<InstallationSettingsRequest>(request);
    requireSchemaVersion(body);
    await repo.saveInstallationSettings(installation.installationId, body, now);
    return jsonResponse({ schemaVersion: 1, requestId: body.requestId, saved: true });
  }

  if (url.pathname === "/api/v1/reports") {
    if (request.method !== "POST") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    await limiter.check(`reports:${installation.installationId}`, { maxRequests: 120, windowSeconds: 3600 }, now);
    const body = await readJsonBody<PublicReportRequest>(request);
    requireSchemaVersion(body);
    const result = await repo.insertReport(installation, body, now);
    return jsonResponse({ schemaVersion: 1, requestId: body.requestId, ...result });
  }

  if (url.pathname === "/api/v1/installations/me/link-codes") {
    if (request.method !== "POST") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    const body = await readJsonBody<{ schemaVersion: number; requestId: string }>(request);
    requireSchemaVersion(body);
    const result = await repo.createLinkCode(installation.installationId, now);
    return jsonResponse({ schemaVersion: 1, requestId: body.requestId, ...result });
  }

  if (url.pathname === "/api/v1/installations/me/links") {
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    if (request.method === "GET") {
      const links = await repo.listLinksForInstallation(installation.installationId);
      return jsonResponse({ schemaVersion: 1, links });
    }
    return methodNotAllowed();
  }

  const linkMatch = url.pathname.match(/^\/api\/v1\/installations\/me\/links\/([^/]+)$/);
  if (linkMatch) {
    if (request.method !== "DELETE") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    await repo.revokeLinkFromDevice(installation.installationId, linkMatch[1], now);
    return jsonResponse({ schemaVersion: 1, revoked: true });
  }

  if (url.pathname === "/api/v1/installations/me/service-sync") {
    if (request.method !== "POST") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    const body = await readJsonBody<ServiceSyncRequest>(request);
    requireSchemaVersion(body);
    const command = await repo.serviceSync(installation, body, now);
    return jsonResponse({
      schemaVersion: 1,
      requestId: body.requestId,
      serverTime: now,
      nextPollAfterSeconds: repo.config.devicePollIntervalSeconds,
      command: command
        ? { commandId: command.commandId, type: command.type, expiresAt: command.expiresAt }
        : null,
    });
  }

  const commandResultMatch = url.pathname.match(/^\/api\/v1\/installations\/me\/commands\/([^/]+)\/result$/);
  if (commandResultMatch) {
    if (request.method !== "POST") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    const body = await readJsonBody<CommandResultRequest>(request);
    requireSchemaVersion(body);
    const { command, duplicate } = await repo.saveCommandResult(installation, commandResultMatch[1], body, now);
    if (!duplicate && await repo.markCommandTelegramResultSent(command.commandId, now)) {
      const telegram = new TelegramClient(env);
      const text = remoteResultText(installation.deviceAlias ?? "Мой телефон", body);
      if (command.telegramMessageId) {
        await telegram.editMessageText(command.telegramChatId, command.telegramMessageId, text);
      } else {
        await telegram.sendMessage(command.telegramChatId, text);
      }
    }
    return jsonResponse({ schemaVersion: 1, requestId: body.requestId, accepted: true, duplicate });
  }

  if (url.pathname === "/api/v1/installations/me") {
    if (request.method !== "DELETE") return methodNotAllowed();
    const installation = await repo.authenticateDevice(request.headers.get("authorization"));
    await repo.revokeInstallation(installation.installationId, now);
    return jsonResponse({ schemaVersion: 1, revoked: true });
  }

  if (url.pathname === "/api/v1/public/status") {
    if (request.method !== "GET") return methodNotAllowed();
    const regionCode = url.searchParams.get("regionCode");
    const operatorCode = url.searchParams.get("operatorCode");
    const cityCode = url.searchParams.get("cityCode");
    if (!regionCode || !operatorCode) {
      throw new ApiError("MISSING_STATUS_FILTERS", "regionCode and operatorCode are required", 400);
    }
    const primarySince = now - repo.config.reportPrimaryWindowMinutes * 60_000;
    const fallbackSince = now - repo.config.reportFallbackWindowMinutes * 60_000;
    const [primarySamples, fallbackSamples] = await Promise.all([
      repo.getLatestSamples(regionCode, operatorCode, primarySince, cityCode),
      repo.getLatestSamples(regionCode, operatorCode, fallbackSince, cityCode),
    ]);
    const minUniqueInstallations = cityCode
      ? repo.config.minUniqueInstallationsForCity
      : repo.config.minUniqueInstallations;
    const result = new PublicStatusAggregator().aggregate({
      regionCode,
      operatorCode,
      currentTime: now,
      primarySamples,
      fallbackSamples,
      policy: {
        primaryWindowMinutes: repo.config.reportPrimaryWindowMinutes,
        fallbackWindowMinutes: repo.config.reportFallbackWindowMinutes,
        minUniqueInstallations,
        consensusNumerator: repo.config.consensusNumerator,
        consensusDenominator: repo.config.consensusDenominator,
      },
    });
    return jsonResponse({ schemaVersion: 1, result });
  }

  return notFound();
}

function catalogResponse(items: unknown[]): Response {
  return jsonResponse(
    {
      schemaVersion: 1,
      catalogVersion: 1,
      items,
    },
    {
      headers: {
        "Cache-Control": "public, max-age=3600",
        ETag: "\"catalog-v1\"",
      },
    },
  );
}

function clientIp(request: Request): string {
  return request.headers.get("cf-connecting-ip") ?? "unknown";
}
