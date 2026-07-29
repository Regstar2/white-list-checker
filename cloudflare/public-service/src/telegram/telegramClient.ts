import { ApiError } from "../http/errors";
import type { Env } from "../types";

interface TelegramApiResponse<T> {
  ok: boolean;
  result?: T;
  description?: string;
  parameters?: {
    retry_after?: number;
  };
}

export interface InlineKeyboardButton {
  text: string;
  callback_data: string;
}

export type InlineKeyboard = InlineKeyboardButton[][];

export class TelegramClient {
  constructor(private readonly env: Env) {}

  async sendMessage(chatId: string, text: string, keyboard?: InlineKeyboard): Promise<{ messageId: number | null }> {
    const result = await this.call<{ message_id?: number }>("sendMessage", {
      chat_id: chatId,
      text,
      parse_mode: "HTML",
      disable_web_page_preview: true,
      reply_markup: keyboard ? { inline_keyboard: keyboard } : undefined,
    });
    return { messageId: result.message_id ?? null };
  }

  async editMessageText(chatId: string, messageId: string | number, text: string, keyboard?: InlineKeyboard): Promise<void> {
    await this.call("editMessageText", {
      chat_id: chatId,
      message_id: messageId,
      text,
      parse_mode: "HTML",
      disable_web_page_preview: true,
      reply_markup: keyboard ? { inline_keyboard: keyboard } : undefined,
    });
  }

  async answerCallbackQuery(callbackQueryId: string, text?: string): Promise<void> {
    await this.call("answerCallbackQuery", {
      callback_query_id: callbackQueryId,
      text,
      show_alert: false,
    });
  }

  async setMyCommands(): Promise<void> {
    await this.call("setMyCommands", {
      commands: [
        { command: "start", description: "Главное меню" },
        { command: "help", description: "Справка" },
        { command: "status", description: "Статус по данным пользователей" },
        { command: "region", description: "Выбрать регион" },
        { command: "operator", description: "Выбрать оператора" },
        { command: "devices", description: "Мои устройства" },
        { command: "link", description: "Привязать устройство" },
        { command: "about", description: "О проекте" },
        { command: "feedback", description: "Обратная связь" },
      ],
    });
  }

  async getWebhookInfo(): Promise<unknown> {
    return this.call("getWebhookInfo", {});
  }

  private async call<T = unknown>(method: string, payload: Record<string, unknown>): Promise<T> {
    const token = this.env.PUBLIC_BOT_TOKEN;
    if (!token) {
      throw new ApiError("BOT_TOKEN_NOT_CONFIGURED", "Telegram bot token is not configured", 500);
    }
    const response = await fetch(`https://api.telegram.org/bot${token}/${method}`, {
      method: "POST",
      headers: {
        "content-type": "application/json; charset=utf-8",
      },
      body: JSON.stringify(removeUndefined(payload)),
    });
    const body = (await response.json().catch(() => null)) as TelegramApiResponse<T> | null;
    if (!response.ok || !body?.ok) {
      const retryAfter = body?.parameters?.retry_after;
      const message = retryAfter
        ? `Telegram temporarily rate limited the bot; retry after ${retryAfter} seconds`
        : "Telegram request failed";
      throw new ApiError("TELEGRAM_REQUEST_FAILED", message, response.status || 502);
    }
    return body.result as T;
  }
}

function removeUndefined(value: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined));
}
