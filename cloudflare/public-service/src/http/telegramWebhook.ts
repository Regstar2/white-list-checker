import { ApiError } from "./errors";
import { readJsonBody } from "./json";
import { PublicTelegramBot } from "../telegram/publicBot";
import type { Env, TelegramUpdate } from "../types";

export async function handleTelegramWebhook(request: Request, env: Env, now = Date.now()): Promise<Response> {
  if (request.method !== "POST") {
    throw new ApiError("METHOD_NOT_ALLOWED", "Method not allowed", 405);
  }
  const expectedSecret = env.TELEGRAM_WEBHOOK_SECRET;
  if (!expectedSecret) {
    throw new ApiError("WEBHOOK_SECRET_NOT_CONFIGURED", "Webhook secret is not configured", 500);
  }
  const actualSecret = request.headers.get("X-Telegram-Bot-Api-Secret-Token");
  if (actualSecret !== expectedSecret) {
    throw new ApiError("WEBHOOK_UNAUTHORIZED", "Unauthorized", 401);
  }
  const update = await readJsonBody<TelegramUpdate>(request, 128 * 1024);
  if (!Number.isInteger(update.update_id)) {
    throw new ApiError("INVALID_TELEGRAM_UPDATE", "Invalid Telegram update", 400);
  }
  await new PublicTelegramBot(env).handleUpdate(update, now);
  return new Response("ok", { status: 200 });
}
