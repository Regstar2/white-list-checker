import { loadConfig } from "../config";
import { OPERATORS, REGIONS } from "../domain/catalog";
import { PublicStatusAggregator } from "../domain/publicStatusAggregator";
import { ApiError } from "../http/errors";
import { D1RateLimiter } from "../security/rateLimit";
import type { Env, TelegramCallbackQuery, TelegramMessage, TelegramUpdate } from "../types";
import { D1PublicServiceRepository } from "../repositories/d1PublicServiceRepository";
import { deviceKeyboard, devicesKeyboard, mainKeyboard, operatorKeyboard, regionKeyboard } from "./keyboards";
import {
  aboutText,
  chooseOperatorText,
  chooseRegionText,
  devicesText,
  helpText,
  missingStatusSelectionText,
  remoteRequestQueuedText,
  safeErrorText,
  startText,
  statusText,
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

    const data = callback.data ?? "";
    const parts = data.split(":");
    if (parts[0] !== "v1") return;
    const action = parts[1];
    if (action === "menu") {
      await this.telegram.editMessageText(chatId, message.message_id, startText(), mainKeyboard(await this.hasDevices(chatId)));
      return;
    }
    if (action === "status") {
      await this.sendStatus(chatId, now, message.message_id);
      return;
    }
    if (action === "regions") {
      await this.telegram.editMessageText(chatId, message.message_id, chooseRegionText(), regionKeyboard());
      return;
    }
    if (action === "operators") {
      await this.telegram.editMessageText(chatId, message.message_id, chooseOperatorText(), operatorKeyboard());
      return;
    }
    if (action === "region") {
      await this.repo.saveTelegramRegion(chatId, parts[2], now);
      await this.telegram.editMessageText(chatId, message.message_id, "Регион сохранён.", mainKeyboard(await this.hasDevices(chatId)));
      return;
    }
    if (action === "operator") {
      await this.repo.saveTelegramOperator(chatId, parts[2], now);
      await this.telegram.editMessageText(chatId, message.message_id, "Оператор сохранён.", mainKeyboard(await this.hasDevices(chatId)));
      return;
    }
    if (action === "devices") {
      await this.sendDevices(chatId, now, message.message_id);
      return;
    }
    if (action === "device") {
      await this.sendDevice(chatId, parts[2], message.message_id);
      return;
    }
    if (action === "check") {
      await this.createRemoteCommand(chatId, parts[2], message.message_id, now);
      return;
    }
    if (action === "unlink") {
      await this.repo.revokeLinkFromTelegram(chatId, parts[2], now);
      await this.telegram.editMessageText(chatId, message.message_id, "Устройство отвязано.", mainKeyboard(await this.hasDevices(chatId)));
      return;
    }
    if (action === "about") {
      await this.telegram.editMessageText(chatId, message.message_id, aboutText(), mainKeyboard(await this.hasDevices(chatId)));
      return;
    }
    if (action === "feedback") {
      await this.telegram.sendMessage(chatId, "Отправьте сообщение вида:\n/feedback ваш текст");
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
    if (editMessageId) await this.telegram.editMessageText(chatId, editMessageId, text, mainKeyboard(await this.hasDevices(chatId)));
    else await this.telegram.sendMessage(chatId, text, mainKeyboard(await this.hasDevices(chatId)));
  }

  private async sendDevices(chatId: string, now: number, editMessageId?: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const text = devicesText(devices, now, this.config.deviceOnlineTimeoutSeconds);
    const keyboard = devices.length > 0 ? devicesKeyboard(devices) : mainKeyboard(false);
    if (editMessageId) await this.telegram.editMessageText(chatId, editMessageId, text, keyboard);
    else await this.telegram.sendMessage(chatId, text, keyboard);
  }

  private async sendDevice(chatId: string, linkId: string, editMessageId: number): Promise<void> {
    const devices = await this.repo.listDevicesForChat(chatId);
    const device = devices.find((item) => item.linkId === linkId);
    if (!device) {
      await this.telegram.editMessageText(chatId, editMessageId, "Устройство не найдено или отвязано.", mainKeyboard(devices.length > 0));
      return;
    }
    await this.telegram.editMessageText(chatId, editMessageId, `<b>${device.deviceAlias}</b>`, deviceKeyboard(linkId));
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
      await this.telegram.sendMessage(
        chatId,
        `Устройство привязано: <b>${device.deviceAlias}</b>`,
        deviceKeyboard(device.linkId),
      );
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
      await this.telegram.sendMessage(chatId, "Спасибо. Сообщение сохранено.");
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

function substringBefore(value: string, separator: string): string {
  const index = value.indexOf(separator);
  if (index < 0) {
    return value;
  }
  return value.slice(0, index);
}
