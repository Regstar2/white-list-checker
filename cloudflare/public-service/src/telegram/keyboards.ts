import { OPERATORS, REGIONS, type OperatorInfo, type RegionInfo } from "../domain/catalog";
import type { LinkedDeviceRecord } from "../types";
import type { InlineKeyboard } from "./telegramClient";

export const TELEGRAM_CALLBACK_DATA_LIMIT_BYTES = 64;

export function mainKeyboard(_hasDevices = false): InlineKeyboard {
  return [
    [{ text: "Статус сети", callback_data: "v1:status" }],
    [
      { text: "Регион", callback_data: "v1:regions" },
      { text: "Оператор", callback_data: "v1:operators" },
    ],
    [{ text: "Мои устройства", callback_data: "v1:devices" }],
    [
      { text: "Помощь", callback_data: "v1:help" },
      { text: "О проекте", callback_data: "v1:about" },
    ],
    [{ text: "Обратная связь", callback_data: "v1:feedback" }],
  ];
}

export function statusKeyboard(): InlineKeyboard {
  return [
    [{ text: "Обновить", callback_data: "v1:status-refresh" }],
    [
      { text: "Изменить регион", callback_data: "v1:regions" },
      { text: "Изменить оператора", callback_data: "v1:operators" },
    ],
    [{ text: "Главное меню", callback_data: "v1:menu" }],
  ];
}

export function regionKeyboard(regions: RegionInfo[] = REGIONS): InlineKeyboard {
  const rows = chunk(regions.map((region) => ({
    text: region.label,
    callback_data: `v1:region:${region.code}`,
  })), 2);
  rows.push([{ text: "Главное меню", callback_data: "v1:menu" }]);
  return rows;
}

export function operatorKeyboard(operators: OperatorInfo[] = OPERATORS): InlineKeyboard {
  const rows = chunk(operators.map((operator) => ({
    text: operator.label,
    callback_data: `v1:operator:${operator.code}`,
  })), 2);
  rows.push([{ text: "Главное меню", callback_data: "v1:menu" }]);
  return rows;
}

export function devicesKeyboard(devices: LinkedDeviceRecord[]): InlineKeyboard {
  const rows: InlineKeyboard = [];
  for (const device of devices.slice(0, 8)) {
    rows.push([{ text: device.deviceAlias, callback_data: `v1:device:${device.linkId}` }]);
  }
  rows.push([{ text: "Главное меню", callback_data: "v1:menu" }]);
  return rows;
}

export function deviceKeyboard(linkId: string): InlineKeyboard {
  return [
    [{ text: "Проверить сейчас", callback_data: `v1:check:${linkId}` }],
    [{ text: "Отвязать устройство", callback_data: `v1:unlink-request:${linkId}` }],
    [{ text: "Назад", callback_data: "v1:devices" }],
  ];
}

export function unlinkConfirmKeyboard(linkId: string): InlineKeyboard {
  return [
    [
      { text: "Да, отвязать", callback_data: `v1:unlink-confirm:${linkId}` },
      { text: "Отмена", callback_data: `v1:unlink-cancel:${linkId}` },
    ],
  ];
}

function chunk<T>(items: T[], size: number): T[][] {
  const rows: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    rows.push(items.slice(i, i + size));
  }
  return rows;
}
