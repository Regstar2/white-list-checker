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
v0.8   — история, статистика, активный мониторинг, центральный public service MVP
```

## v0.8.13 update — активный мониторинг и команды Telegram

WorkManager:

- интервалы 15 / 30 / 60 минут сохранены;
- Android может сдвигать запуск, точное время по секундам не обещается;
- добавлены политики уведомлений: `NONE`, `EVERY_ATTEMPT`, `STATE_CHANGE_ONLY`;
- недоступность проверки и техническая ошибка не перезаписывают последний валидный статус `WHITELIST_ON/OFF`.

Активный мониторинг:

- запускается только явной кнопкой пользователя;
- работает через foreground service с типом `dataSync`;
- показывает постоянное уведомление;
- первая проверка выполняется сразу;
- интервал 1–60 минут;
- параллельные проверки запрещает общий `CheckExecutionCoordinator`;
- на время работы отменяет WorkManager и восстанавливает его после остановки, если он был включён;
- Android 15+ может остановить `dataSync` foreground service по системному лимиту.

Telegram-команды:

- `/status` возвращает сохранённый результат;
- `/check` запускает новую проверку через общий runner;
- `/help` показывает справку;
- команды работают только пока активный мониторинг запущен и включён соответствующий переключатель;
- авторизация выполняется точным сравнением `chatId` с включёнными получателями;
- polling идёт через пользовательский Cloudflare Worker relay, Android не хранит `BOT_TOKEN` и не вызывает `api.telegram.org` напрямую.

## v0.8.14–v0.8.15 update — центральный public service

Центральный сервис отделён от личного Telegram relay:

```text
Android-приложения пользователей → central Cloudflare Worker → D1 → public Telegram bot
```

Личный relay остаётся прежним:

```text
Android → user-owned Cloudflare Worker → личный Telegram bot
```

Общий сервис:

- принимает добровольные обезличенные reports;
- показывает публичный агрегированный статус без требования установить приложение;
- позволяет связать Telegram-чат с устройством для приватной remote check;
- remote check работает только пока активный foreground service реально запущен.

В Android:

- `shareReports=false` и `allowRemoteChecks=false` по умолчанию;
- URL центрального сервиса встроен в `BuildConfig.PUBLIC_SERVICE_BASE_URL` и не редактируется пользователем;
- личный `Worker URL` Telegram relay остаётся отдельной пользовательской настройкой;
- регион обязателен для отправки public reports, город необязателен;
- автоопределение региона/города запускается только по кнопке, использует approximate location и требует подтверждения;
- координаты и raw address не сохраняются и не отправляются;
- оператор определяется по active/default data subscription или выбирается вручную.
