# WhiteListChecker — текущий MVP (v0.7)

Актуальное описание продукта. Для исторической версии с локальным Telegram-proxy см. [docs/archive/](archive/).

## 1. Цель приложения

Android-приложение определяет **признаки режима белых списков** мобильного оператора: когда внешние сайты недоступны через cellular, а локальные — доступны.

Приложение не утверждает «БС точно включены» — только «похоже на включённые белые списки».

## 2. Проверка именно мобильной сети

Даже при активном Wi-Fi проверка идёт через **cellular Network**:

1. `ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR)`
2. `network.openConnection(url)` для каждого сайта
3. `activeNetwork` — только для подписи в UI («активная сеть: Wi-Fi»)

## 3. Группы сайтов

| Группа | Примеры |
|--------|---------|
| **FOREIGN** | Google, Cloudflare, GitHub, Wikipedia |
| **LOCAL** | Yandex, VK, Mail.ru, Gosuslugi |

Список редактируется пользователем (включение/выключение, добавление своих URL).

## 4. Состояния (`WhitelistState`)

| Состояние | Смысл |
|-----------|--------|
| `UNKNOWN` | Начальное / не определено |
| `WHITELIST_OFF` | FOREIGN и LOCAL достаточно доступны |
| `WHITELIST_ON` | FOREIGN недоступны, LOCAL доступны |
| `NO_MOBILE_INTERNET` | Обе группы недоступны |
| `MOBILE_DNS_FAILURE` | Cellular Network есть, но домены массово не резолвятся |
| `PARTIAL_PROBLEM` | Смешанный результат, не похоже на классический БС |
| `CELLULAR_NETWORK_UNAVAILABLE` | Android не выдал cellular Network |

## 5. Подтверждение смены состояния

Новое состояние становится **подтверждённым** только после **2 одинаковых проверок подряд** (`StateChangeDetector`).

## 6. Уведомления только при WHITELIST_OFF ↔ WHITELIST_ON

Уведомления (локальные и Telegram) отправляются **только** при подтверждённых переходах:

- `WHITELIST_OFF` → `WHITELIST_ON`
- `WHITELIST_ON` → `WHITELIST_OFF`

Не уведомляем: pending 1/2, `NO_MOBILE_INTERNET`, `MOBILE_DNS_FAILURE`, `PARTIAL_PROBLEM`, `CELLULAR_NETWORK_UNAVAILABLE`, обычные проверки без смены.

## 7. Локальные уведомления

- Опционально, переключатель в UI
- Канал `whitelist_events`
- `POST_NOTIFICATIONS` на Android 13+
- Тестовое уведомление — только по явной кнопке

## 8. Telegram через user-owned Cloudflare Worker

Android **не хранит BOT_TOKEN**.

Пользователь создаёт:

1. Telegram-бота (BotFather)
2. Cloudflare Worker + secrets `BOT_TOKEN`, `RELAY_SECRET`

В приложении:

- Worker URL
- Relay Secret
- Список получателей (`chat_id`, имя, тип чата)

`getUpdates` через Worker — **только** для ручного получения `chat_id` (кнопки в UI). Long polling и bot commands в фоне запрещены.

Инструкция: [cloudflare-worker/README.md](cloudflare-worker/README.md)

## 9. Очередь Telegram (Room)

Если Worker недоступен или отправка не удалась, сообщение сохраняется в Room. Пользователь может повторить отправку или очистить очередь из UI.

Direct fallback на `api.telegram.org` **запрещён**.

## 10. Автопроверка (WorkManager)

- Интервалы: 15 / 30 / 60 минут или своё (≥ 15 мин)
- `WhitelistCheckWorker` — та же цепочка проверки + уведомления
- Статус последнего запуска в DataStore

## 11. Текущий UI

Навигация через enum `AppScreen` (без Navigation Compose):

| Экран | Назначение |
|-------|------------|
| **Главная** | Проверка, последний результат, быстрые действия |
| **Уведомления** | Локальные, Worker, получатели, очередь |
| **Настройки проверки** | Список FOREIGN/LOCAL сайтов |
| **Автопроверка** | WorkManager, интервал, статус |
| **Диагностика** | Подробности последней проверки, копирование отчёта |

## 12. Roadmap (выполнено)

```text
v0.1   — ручная проверка мобильной сети
v0.2   — FOREIGN / LOCAL
v0.3   — debounce / confirmed state
v0.3.5 — локальные уведомления
v0.4.2 — Telegram через Cloudflare Worker relay
v0.5   — очередь Telegram
v0.6   — WorkManager
v0.7   — split UI, multi-recipient, editable sites, diagnostics, UI polish
```
