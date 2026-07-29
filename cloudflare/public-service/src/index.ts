import { handleApiRequest } from "./http/apiRoutes";
import { ApiError, errorResponse, notFound } from "./http/errors";
import { handleTelegramWebhook } from "./http/telegramWebhook";
import { D1PublicServiceRepository } from "./repositories/d1PublicServiceRepository";
import type { Env } from "./types";

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
        return new Response("ok", { status: 200 });
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
