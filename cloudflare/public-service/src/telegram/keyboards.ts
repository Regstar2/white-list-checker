import { OPERATORS, REGIONS } from "../domain/catalog";
import type { InlineKeyboard } from "./telegramClient";
import type { LinkedDeviceRecord } from "../types";

export function mainKeyboard(hasDevices: boolean): InlineKeyboard {
  const rows: InlineKeyboard = [
    [{ text: "Статус по данным пользователей", callback_data: "v1:status" }],
    [
      { text: "Выбрать регион", callback_data: "v1:regions" },
      { text: "Выбрать оператора", callback_data: "v1:operators" },
    ],
    [{ text: "Мои устройства", callback_data: "v1:devices" }],
    [
      { text: "О проекте", callback_data: "v1:about" },
      { text: "Обратная связь", callback_data: "v1:feedback" },
    ],
  ];
  if (hasDevices) {
    rows.splice(1, 0, [{ text: "Проверить на моём устройстве", callback_data: "v1:devices" }]);
  }
  return rows;
}

export function regionKeyboard(): InlineKeyboard {
  return chunk(REGIONS.map((region) => ({
    text: region.label,
    callback_data: `v1:region:${region.code}`,
  })), 2);
}

export function operatorKeyboard(): InlineKeyboard {
  return chunk(OPERATORS.map((operator) => ({
    text: operator.label,
    callback_data: `v1:operator:${operator.code}`,
  })), 2);
}

export function devicesKeyboard(devices: LinkedDeviceRecord[]): InlineKeyboard {
  const rows: InlineKeyboard = [];
  for (const device of devices.slice(0, 8)) {
    rows.push([{ text: device.deviceAlias, callback_data: `v1:device:${device.linkId}` }]);
  }
  rows.push([{ text: "Назад", callback_data: "v1:menu" }]);
  return rows;
}

export function deviceKeyboard(linkId: string): InlineKeyboard {
  return [
    [{ text: "Последний результат", callback_data: `v1:device:${linkId}` }],
    [{ text: "Проверить сейчас", callback_data: `v1:check:${linkId}` }],
    [{ text: "Отвязать", callback_data: `v1:unlink:${linkId}` }],
    [{ text: "Назад", callback_data: "v1:devices" }],
  ];
}

function chunk<T>(items: T[], size: number): T[][] {
  const rows: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    rows.push(items.slice(i, i + size));
  }
  return rows;
}
