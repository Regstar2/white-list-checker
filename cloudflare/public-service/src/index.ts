import { handleApiRequest } from "./http/apiRoutes";
import { ApiError, errorResponse, jsonResponse, notFound } from "./http/errors";
import { handleTelegramWebhook } from "./http/telegramWebhook";
import { D1PublicServiceRepository } from "./repositories/d1PublicServiceRepository";
import type { Env } from "./types";

const SERVICE_REVISION = "2026-08-06-service-sync-v1";
const CAPABILITIES = [
  "installation-registration",
  "link-codes",
  "public-reports",
  "service-sync",
  "telegram-webhook",
] as const;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(request.url);
      if (url.pathname.startsWith("/api/v1/")) {
        return await handleApiRequest(request, env);
      }
      if (url.pathname === "/telegram/webhook") {
        return await handleTelegramWebhook(request, env);
      }
      if (url.pathname === "/health") {
        if (request.headers.get("accept")?.includes("application/json")) {
          return jsonResponse({
            status: "ok",
            service: "whitelist-monitor-tg-relay",
            revision: SERVICE_REVISION,
            capabilities: CAPABILITIES,
          });
        }
        return new Response("ok", {
          status: 200,
          headers: {
            "cache-control": "no-store",
            "content-type": "text/plain; charset=utf-8",
            "x-service-revision": SERVICE_REVISION,
            "x-service-capabilities": CAPABILITIES.join(","),
          },
        });
      }
      return notFound();
    } catch (error) {
      if (error instanceof ApiError) return errorResponse(error);
      return errorResponse(new ApiError("INTERNAL_ERROR", "Internal service error", 500));
    }
  },

  async scheduled(_event: ScheduledEvent, env: Env): Promise<void> {
    await new D1PublicServiceRepository(env.DB, env).cleanup(Date.now());
  },
};
