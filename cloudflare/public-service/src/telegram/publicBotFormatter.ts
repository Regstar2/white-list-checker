import { operatorLabel, regionLabel } from "../domain/catalog";
import type { PublicStatusResult } from "../domain/publicStatusAggregator";
import type { CommandResultRequest, LinkedDeviceRecord } from "../types";

const PROJECT_REPOSITORY_URL = "https://github.com/Regstar2/white-list-checker";

export function startText(): string {
  return [
    "<b>Whitelist Checker</b>",
    "",
    "Бот показывает агрегированные данные пользователей приложения. Устанавливать Android-приложение для просмотра общего статуса не нужно.",
    "",
    "Данные не являются официальной информацией оператора. Отдельные устройства публично не показываются.",
    "",
    "Если у вас установлено приложение, вы можете отдельно включить отправку обезличенных результатов и привязать устройство для приватной проверки.",
  ].join("\n");
}

export function helpText(): string {
  return [
    "<b>Команды</b>",
    "",
    "/status — статус по данным пользователей",
    "/region — выбрать регион",
    "/operator — выбрать оператора",
    "/devices — мои связанные устройства",
    "/link ABCD-EFGH — привязать устройство",
    "/about — о проекте",
    "/feedback текст — обратная связь",
  ].join("\n");
}

export function aboutText(): string {
  return [
    "<b>О проекте</b>",
    "",
    "WhiteListChecker ищет признаки режима белых списков в мобильной сети: когда внешние сайты недоступны, а локальные доступны.",
    "",
    "Публичный статус строится только по добровольно отправленным обезличенным отчётам и не раскрывает отдельные устройства.",
    "",
    `<a href="${PROJECT_REPOSITORY_URL}">Репозиторий проекта на GitHub</a>`,
  ].join("\n");
}

export function statusText(result: PublicStatusResult): string {
  const title = `${operatorLabel(result.operatorCode)} · ${regionLabel(result.regionCode)}`;
  const state = (() => {
    if (result.kind === "NO_DATA") return "Свежих данных нет";
    if (result.kind === "INSUFFICIENT_SAMPLE") return "Недостаточно независимых данных";
    if (result.kind === "MIXED") return "Данные пользователей расходятся";
    if (result.consensusState === "LIKELY_ENABLED") return "Белые списки, вероятно, включены";
    if (result.consensusState === "LIKELY_DISABLED") return "Признаки белых списков преимущественно не обнаружены";
    return "Недостаточно независимых данных";
  })();
  const freshness = result.stale
    ? `Период: последние ${result.windowMinutes} минут, данные могут быть устаревшими`
    : `Период: последние ${result.windowMinutes} минут`;
  const latest = result.latestCheckedAt == null
    ? "Последний отчёт: нет"
    : `Последний отчёт: ${ageText(result.latestCheckedAt)}`;
  return [
    `<b>${escapeHtml(title)}</b>`,
    "",
    `<b>${escapeHtml(state)}</b>`,
    "",
    `По данным ${result.sampleSize} независимых устройств:`,
    `• признаки белых списков: ${result.enabledCount}`,
    `• признаки не обнаружены: ${result.disabledCount}`,
    `• неоднозначные результаты: ${result.inconclusiveCount}`,
    "",
    freshness,
    latest,
    "",
    "Данные предоставлены пользователями WhiteListChecker.",
    "Это не официальный статус оператора.",
  ].join("\n");
}

export function chooseRegionText(): string {
  return "Выберите регион. Показаны только регионы со свежими данными пользователей.";
}

export function chooseOperatorText(regionCode?: string | null): string {
  if (regionCode) {
    return `Выберите оператора для региона «${escapeHtml(regionLabel(regionCode))}». Показаны только операторы со свежими данными.`;
  }
  return "Выберите оператора. Показаны только операторы со свежими данными пользователей.";
}

export function noAvailableRegionsText(): string {
  return "Сейчас нет свежих данных ни по одному региону. Попробуйте позже.";
}

export function noAvailableOperatorsText(regionCode?: string | null): string {
  if (regionCode) {
    return `Сейчас нет свежих данных по операторам для региона «${escapeHtml(regionLabel(regionCode))}». Выберите другой регион или попробуйте позже.`;
  }
  return "Сейчас нет свежих данных ни по одному оператору. Попробуйте позже.";
}

export function missingStatusSelectionText(): string {
  return "Для публичного статуса сначала выберите регион и оператора.";
}

export function devicesText(devices: LinkedDeviceRecord[], now: number, onlineTimeoutSeconds: number): string {
  if (devices.length === 0) {
    return [
      "<b>Мои устройства</b>",
      "",
      "Связанных устройств нет.",
      "",
      "Чтобы привязать телефон, откройте приложение WhiteListChecker и создайте код привязки, затем отправьте сюда /link ABCD-EFGH.",
    ].join("\n");
  }
  const lines = ["<b>Мои устройства</b>", ""];
  for (const device of devices) {
    const online = device.lastSeenAt != null && now - device.lastSeenAt <= onlineTimeoutSeconds * 1000;
    lines.push(`${escapeHtml(device.deviceAlias)} — ${online ? "онлайн" : "не подключено"}`);
  }
  return lines.join("\n");
}

export function deviceText(device: LinkedDeviceRecord, now: number, onlineTimeoutSeconds: number): string {
  const online = device.lastSeenAt != null && now - device.lastSeenAt <= onlineTimeoutSeconds * 1000;
  const remoteChecks = device.allowRemoteChecks ? "разрешены" : "выключены в приложении";
  const serviceConnected = online && device.lastServiceState != null;
  const lines = [
    `<b>${escapeHtml(device.deviceAlias)}</b>`,
    "",
    `Статус: ${online ? "online" : "offline"}`,
    `Удалённые проверки: ${remoteChecks}`,
  ];
  if (!serviceConnected) {
    lines.push("", "Foreground service не подключён. Запустите «Активный мониторинг» в Android-приложении, чтобы устройство могло принять команду.");
  }
  return lines.join("\n");
}

export function deviceNotFoundText(): string {
  return "Устройство не найдено или уже отвязано.";
}

export function unlinkConfirmationText(deviceAlias: string): string {
  return `Точно отвязать устройство «${escapeHtml(deviceAlias)}»?`;
}

export function unlinkCancelledText(device: LinkedDeviceRecord, now: number, onlineTimeoutSeconds: number): string {
  return deviceText(device, now, onlineTimeoutSeconds);
}

export function unlinkSuccessText(deviceAlias: string): string {
  return `Устройство «${escapeHtml(deviceAlias)}» отвязано.`;
}

export function regionSavedText(): string {
  return "Регион сохранён.";
}

export function operatorSavedText(): string {
  return "Оператор сохранён.";
}

export function feedbackPromptText(): string {
  return "Отправьте сообщение вида:\n/feedback ваш текст";
}

export function feedbackSavedText(): string {
  return "Спасибо. Сообщение сохранено.";
}

export function linkedDeviceText(device: LinkedDeviceRecord): string {
  return `Устройство привязано: <b>${escapeHtml(device.deviceAlias)}</b>`;
}

export function remoteRequestQueuedText(deviceAlias: string): string {
  return [
    `<b>${escapeHtml(deviceAlias)}</b>`,
    "",
    "Запрос отправлен устройству.",
    "Ожидание результата...",
  ].join("\n");
}

export function remoteResultText(deviceAlias: string, result: CommandResultRequest): string {
  if (result.outcome === "SUCCESS") {
    return [
      `<b>${escapeHtml(deviceAlias)}</b>`,
      "",
      "<b>Проверка завершена</b>",
      "",
      escapeHtml(stateLabel(result.whitelistState)),
      `Локальные проверки: ${result.local?.available ?? 0} из ${result.local?.total ?? 0}`,
      `Внешние проверки: ${result.foreign?.available ?? 0} из ${result.foreign?.total ?? 0}`,
      "",
      `Проверено: ${new Date(result.checkedAt).toISOString()}`,
    ].join("\n");
  }
  if (result.outcome === "UNAVAILABLE") {
    return [
      `<b>${escapeHtml(deviceAlias)}</b>`,
      "",
      "<b>Не удалось выполнить проверку</b>",
      "",
      escapeHtml(result.errorCode ?? "Проверка сейчас недоступна."),
    ].join("\n");
  }
  return [
    `<b>${escapeHtml(deviceAlias)}</b>`,
    "",
    "<b>Проверка завершилась ошибкой</b>",
    "",
    `Код: ${escapeHtml(result.errorCode ?? result.outcome)}`,
    "Попробуйте повторить позже.",
  ].join("\n");
}

export function safeErrorText(code: string): string {
  switch (code) {
    case "DEVICE_OFFLINE":
      return "Устройство сейчас не подключено.\n\nЗапустите «Активный мониторинг» в приложении. Удалённые проверки работают только пока Android разрешает foreground service.";
    case "REMOTE_CHECKS_DISABLED":
      return "Удалённые проверки выключены в приложении для этого устройства.";
    case "REMOTE_COMMAND_COOLDOWN":
      return "Проверку можно запрашивать не чаще одного раза в минуту.";
    case "REMOTE_COMMAND_ALREADY_ACTIVE":
      return "Устройство уже выполняет или ожидает проверку. Повторите запрос позже.";
    default:
      return "Не удалось выполнить действие. Попробуйте позже.";
  }
}

export function escapeHtml(value: unknown): string {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function stateLabel(state: string | undefined): string {
  switch (state) {
    case "LIKELY_ENABLED":
      return "Белые списки, вероятно, включены";
    case "LIKELY_DISABLED":
      return "Белые списки не обнаружены";
    case "NO_MOBILE_INTERNET":
      return "Мобильного интернета нет";
    case "MOBILE_DNS_FAILURE":
      return "Проблема DNS в мобильной сети";
    case "PARTIAL_PROBLEM":
      return "Частичная проблема сети";
    case "CELLULAR_NETWORK_UNAVAILABLE":
      return "Мобильная сеть недоступна";
    default:
      return "Неоднозначный результат";
  }
}

function ageText(timestamp: number): string {
  const diffMs = Math.max(0, Date.now() - timestamp);
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return "только что";
  if (minutes < 60) return `${minutes} мин назад`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} ч назад`;
  return `${Math.floor(hours / 24)} дн назад`;
}
