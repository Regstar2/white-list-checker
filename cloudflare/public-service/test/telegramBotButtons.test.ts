import { PublicTelegramBot } from "../src/telegram/publicBot";
import worker from "../src/index";
import {
  TELEGRAM_CALLBACK_DATA_LIMIT_BYTES,
  deviceKeyboard,
  devicesKeyboard,
  mainKeyboard,
  operatorKeyboard,
  regionKeyboard,
  statusKeyboard,
  unlinkConfirmKeyboard,
} from "../src/telegram/keyboards";
import { deviceText, unlinkConfirmationText } from "../src/telegram/publicBotFormatter";
import type { Env, LinkedDeviceRecord, TelegramUpdate } from "../src/types";
import type { InlineKeyboard } from "../src/telegram/telegramClient";
import { readFileSync } from "node:fs";

const now = 1_000_000;
const linkId = "123e4567-e89b-12d3-a456-426614174000";

const device: LinkedDeviceRecord = {
  linkId,
  installationId: "installation-1",
  deviceAlias: "Pixel <owner> & test",
  lastSeenAt: now - 10_000,
  allowRemoteChecks: true,
  lastServiceState: "RUNNING",
};

describe("Telegram inline keyboards", () => {
  it("builds the required main menu without the duplicate device-check button", () => {
    expect(flatButtons(mainKeyboard(true)).map((button) => button.callback_data)).toEqual([
      "v1:status",
      "v1:regions",
      "v1:operators",
      "v1:devices",
      "v1:help",
      "v1:about",
      "v1:feedback",
    ]);
    expect(flatButtons(mainKeyboard(true)).some((button) => button.text === "Проверить на моём устройстве")).toBe(false);
  });

  it("builds the status keyboard", () => {
    expect(statusKeyboard()).toEqual([
      [{ text: "Обновить", callback_data: "v1:status-refresh" }],
      [
        { text: "Изменить регион", callback_data: "v1:regions" },
        { text: "Изменить оператора", callback_data: "v1:operators" },
      ],
      [{ text: "Главное меню", callback_data: "v1:menu" }],
    ]);
  });

  it("adds a main menu return button to region and operator lists", () => {
    expect(lastButton(regionKeyboard()).callback_data).toBe("v1:menu");
    expect(lastButton(operatorKeyboard()).callback_data).toBe("v1:menu");
  });

  it("builds a linked devices list with device linkIds and main menu return", () => {
    const keyboard = devicesKeyboard([device]);
    expect(keyboard[0][0]).toEqual({ text: device.deviceAlias, callback_data: `v1:device:${linkId}` });
    expect(lastButton(keyboard).callback_data).toBe("v1:menu");
  });

  it("builds a device screen keyboard without the non-working last-result button", () => {
    const keyboard = deviceKeyboard(linkId);
    expect(flatButtons(keyboard).map((button) => button.callback_data)).toEqual([
      `v1:check:${linkId}`,
      `v1:unlink-request:${linkId}`,
      "v1:devices",
    ]);
    expect(flatButtons(keyboard).some((button) => button.text === "Последний результат")).toBe(false);
  });

  it("builds unlink confirmation with the exact linkId", () => {
    expect(unlinkConfirmKeyboard(linkId)).toEqual([
      [
        { text: "Да, отвязать", callback_data: `v1:unlink-confirm:${linkId}` },
        { text: "Отмена", callback_data: `v1:unlink-cancel:${linkId}` },
      ],
    ]);
  });

  it("keeps callback_data within Telegram limits for production callback shapes", () => {
    const keyboards = [
      mainKeyboard(true),
      statusKeyboard(),
      regionKeyboard(),
      operatorKeyboard(),
      devicesKeyboard([device]),
      deviceKeyboard(linkId),
      unlinkConfirmKeyboard(linkId),
    ];
    for (const button of keyboards.flatMap(flatButtons)) {
      expect(new TextEncoder().encode(button.callback_data).length).toBeLessThanOrEqual(TELEGRAM_CALLBACK_DATA_LIMIT_BYTES);
    }
  });

  it("escapes device aliases in user-facing HTML", () => {
    expect(deviceText(device, now, 45)).toContain("Pixel &lt;owner&gt; &amp; test");
    expect(unlinkConfirmationText(device.deviceAlias)).toContain("Pixel &lt;owner&gt; &amp; test");
  });
});

describe("PublicTelegramBot callback routing", () => {
  it("answers callback queries early and routes status-refresh to an edited status message", async () => {
    const { bot, telegram } = botWithFakes();
    await bot.handleUpdate(callbackUpdate("v1:status-refresh"));
    expect(telegram.answerCallbackQuery).toHaveBeenCalledWith("callback-1");
    expect(telegram.editMessageText).toHaveBeenCalledWith(
      "42",
      7,
      expect.stringContaining("<b>"),
      statusKeyboard(),
    );
  });

  it("routes devices and device callbacks with the expected linkId", async () => {
    const { bot, telegram } = botWithFakes();
    await bot.handleUpdate(callbackUpdate("v1:devices"));
    expect(telegram.editMessageText).toHaveBeenLastCalledWith("42", 7, expect.stringContaining("Pixel"), devicesKeyboard([device]));

    await bot.handleUpdate(callbackUpdate(`v1:device:${linkId}`, 2));
    expect(telegram.editMessageText).toHaveBeenLastCalledWith("42", 7, expect.stringContaining("Pixel"), deviceKeyboard(linkId));
  });

  it("shows unlink confirmation before D1 revoke and revokes only after confirmation", async () => {
    const { bot, repo, telegram } = botWithFakes();
    await bot.handleUpdate(callbackUpdate(`v1:unlink-request:${linkId}`));
    expect(repo.revokeLinkFromTelegram).not.toHaveBeenCalled();
    expect(telegram.editMessageText).toHaveBeenLastCalledWith(
      "42",
      7,
      expect.stringContaining("Точно отвязать"),
      unlinkConfirmKeyboard(linkId),
    );

    repo.listDevicesForChat.mockResolvedValueOnce([device]).mockResolvedValueOnce([]);
    await bot.handleUpdate(callbackUpdate(`v1:unlink-confirm:${linkId}`, 2));
    expect(repo.revokeLinkFromTelegram).toHaveBeenCalledWith("42", linkId, now);
    expect(telegram.editMessageText).toHaveBeenLastCalledWith("42", 7, expect.stringContaining("отвязано"), mainKeyboard(false));
  });

  it("handles malformed callbacks without leaking internal errors", async () => {
    const { bot, telegram } = botWithFakes();
    await expect(bot.handleUpdate(callbackUpdate("broken"))).resolves.toBeUndefined();
    expect(telegram.answerCallbackQuery).toHaveBeenCalledWith("callback-1");
    expect(telegram.editMessageText).not.toHaveBeenCalled();
  });

  it("keeps slash commands working", async () => {
    const { bot, telegram } = botWithFakes();
    await bot.handleUpdate(messageUpdate("/start"));
    expect(telegram.sendMessage).toHaveBeenLastCalledWith("42", expect.stringContaining("Whitelist Checker"), mainKeyboard(true));

    await bot.handleUpdate(messageUpdate("/help", 2));
    expect(telegram.sendMessage).toHaveBeenLastCalledWith("42", expect.stringContaining("/status"), mainKeyboard(true));
  });
});

describe("Worker public service invariants", () => {
  it("serves /health without touching D1 or Telegram", async () => {
    const response = await worker.fetch(new Request("https://example.com/health"), { DB: {} as D1Database } satisfies Env);
    expect(response.status).toBe(200);
    expect(await response.text()).toBe("ok");
  });

  it("keeps public API routes in the central service entrypoint", () => {
    const indexSource = readFileSync(new URL("../src/index.ts", import.meta.url), "utf8");
    expect(indexSource).toContain('/api/v1/');
    expect(indexSource).toContain('/telegram/webhook');
    expect(indexSource).toContain('/health');
  });

  it("does not add D1 migrations for button, Worker name, or URL changes", () => {
    const firstMigration = readFileSync(new URL("../migrations/0001_initial.sql", import.meta.url), "utf8");
    const secondMigration = readFileSync(new URL("../migrations/0002_area_city_operator_sources.sql", import.meta.url), "utf8");
    expect(firstMigration).toContain("CREATE TABLE IF NOT EXISTS installations");
    expect(firstMigration).toContain("CREATE TABLE IF NOT EXISTS reports");
    expect(secondMigration).toContain("ALTER TABLE installations ADD COLUMN city_code TEXT");
    expect(secondMigration).toContain("ALTER TABLE reports ADD COLUMN operator_source TEXT");
  });
});

function botWithFakes(): {
  bot: PublicTelegramBot;
  repo: ReturnType<typeof fakeRepo>;
  telegram: ReturnType<typeof fakeTelegram>;
} {
  const bot = new PublicTelegramBot({
    DB: {} as D1Database,
    PUBLIC_BOT_TOKEN: "test-token",
    DEVICE_ONLINE_TIMEOUT_SECONDS: "45",
  } satisfies Env);
  const repo = fakeRepo();
  const telegram = fakeTelegram();
  const internals = bot as unknown as {
    repo: typeof repo;
    telegram: typeof telegram;
    limiter: { check: ReturnType<typeof vi.fn> };
  };
  internals.repo = repo;
  internals.telegram = telegram;
  internals.limiter = { check: vi.fn(async () => undefined) };
  return { bot, repo, telegram };
}

function fakeRepo() {
  return {
    markTelegramUpdateProcessing: vi.fn(async () => true),
    upsertTelegramUser: vi.fn(async () => undefined),
    isTelegramUserBlocked: vi.fn(async () => false),
    listDevicesForChat: vi.fn(async () => [device]),
    getTelegramPreference: vi.fn(async () => ({ regionCode: "RU-MOW", operatorCode: "MTS" })),
    getLatestSamples: vi.fn(async () => []),
    saveTelegramRegion: vi.fn(async () => undefined),
    saveTelegramOperator: vi.fn(async () => undefined),
    createRemoteCheckCommand: vi.fn(async () => ({ commandId: "command-1", device })),
    revokeLinkFromTelegram: vi.fn(async () => undefined),
    linkTelegramChat: vi.fn(async () => device),
    saveFeedback: vi.fn(async () => undefined),
  };
}

function fakeTelegram() {
  return {
    sendMessage: vi.fn(async () => ({ messageId: 7 })),
    editMessageText: vi.fn(async () => undefined),
    answerCallbackQuery: vi.fn(async () => undefined),
  };
}

function callbackUpdate(data: string, updateId = 1): TelegramUpdate {
  return {
    update_id: updateId,
    callback_query: {
      id: "callback-1",
      from: { id: 100, language_code: "ru" },
      message: {
        message_id: 7,
        chat: { id: "42" },
      },
      data,
    },
  };
}

function messageUpdate(text: string, updateId = 1): TelegramUpdate {
  return {
    update_id: updateId,
    message: {
      message_id: 7,
      chat: { id: "42" },
      from: { id: 100, language_code: "ru" },
      text,
    },
  };
}

function flatButtons(keyboard: InlineKeyboard) {
  return keyboard.flat();
}

function lastButton(keyboard: InlineKeyboard) {
  return keyboard[keyboard.length - 1][0];
}
