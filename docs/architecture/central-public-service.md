# Центральный публичный сервис

## Назначение

Центральный сервис отделён от личного Telegram relay пользователя.

Фактический Worker:

```text
whitelist-monitor-tg-relay
```

Production URL:

```text
https://whitelist-monitor-tg-relay.regstar2.workers.dev
```

Имя историческое: раньше этот экземпляр был личным Telegram relay владельца проекта, теперь он используется как общий сервис WhiteListChecker. Новый Worker `whitelistchecker-public-service` не создаётся.

Сервис даёт два независимых сценария:

1. Публичный Telegram-бот показывает агрегированный статус по сохранённым обезличенным отчётам пользователей.
2. Связанный Telegram-чат может запросить приватную проверку своего устройства, пока в Android работает активный мониторинг.

Публичная кнопка статуса не запускает проверку на телефонах.

## Компоненты

```text
Android app
  -> PublicServiceClient
  -> whitelist-monitor-tg-relay
  -> D1
  -> public Telegram bot webhook
```

Личный relay остаётся отдельным:

```text
Android app
  -> user-owned Cloudflare Worker relay
  -> user's Telegram bot
```

Центральный сервис не использует `RELAY_SECRET` личного relay и не получает `BOT_TOKEN` пользователя.

## Cloudflare Worker

Код находится в:

```text
cloudflare/public-service/
```

Основные модули:

- `src/http/apiRoutes.ts` - JSON API Android-клиента.
- `src/http/telegramWebhook.ts` - webhook общего Telegram-бота.
- `src/domain/publicStatusAggregator.ts` - правила публичной агрегации.
- `src/repositories/d1PublicServiceRepository.ts` - D1-запросы и lifecycle команд.
- `src/telegram/publicBot.ts` - команды и inline-кнопки общего бота.
- `src/telegram/keyboards.ts` - Telegram Inline Keyboard.
- `src/telegram/publicBotFormatter.ts` - пользовательские тексты Telegram.
- `src/telegram/telegramClient.ts` - typed Telegram Bot API client.
- `migrations/0001_initial.sql` - базовая D1 schema.
- `migrations/0002_area_city_operator_sources.sql` - город и источники выбора региона/оператора.

## URL общего сервиса

Android получает адрес общего сервиса из:

```text
BuildConfig.PUBLIC_SERVICE_BASE_URL
```

В v0.8.16 debug и release временно указывают на один production Worker:

```text
https://whitelist-monitor-tg-relay.regstar2.workers.dev
```

Этот URL не хранится в DataStore, не вводится пользователем и не смешивается с пользовательским `Worker URL` личного Telegram relay.

## Android-согласия

В Android есть отдельные настройки:

- `shareReports` - отправлять успешные обезличенные отчёты в общий сервис.
- `allowRemoteChecks` - разрешить связанному Telegram-чату запрашивать новую проверку.

Оба флага выключены по умолчанию. Включение одного не включает второй.

Перед включением `shareReports` Android проверяет, что пользователь подтвердил регион и выбрал или автоматически определил оператора. Город необязателен.

## Public Aggregate Flow

```text
Пользователь общего бота
  -> /status или "Статус сети"
  -> выбор regionCode/operatorCode
  -> Worker выбирает свежие reports
  -> один последний report на installation
  -> PublicStatusAggregator
  -> Telegram message с sample size, freshness и disclaimer
```

Агрегатор учитывает только независимые установки. Много отчётов одного телефона дают один голос в выбранном окне.

Для city-level status применяется отдельный threshold `MIN_UNIQUE_INSTALLATIONS_FOR_CITY`. Если выборка по городу мала, бот должен честно показывать недостаточность данных или fallback, не раскрывая одиночный report.

## Remote Linked-Device Flow

```text
Android
  -> создать одноразовый /link код
Telegram
  -> /link ABCD-EFGH
  -> active installation_link
  -> "Мои устройства"
  -> "Проверить сейчас"
Worker
  -> проверка link + allowRemoteChecks + online heartbeat + cooldown
  -> command CHECK_NOW с TTL
Foreground service
  -> service-sync через default network
  -> получает команду
  -> CheckAndNotifyUseCase / CheckExecutionCoordinator
  -> cellular Network.openConnection для самой проверки
  -> command result назад в Worker
Worker
  -> приватный Telegram-result в связанный chat
```

Удалённый результат попадает в публичную статистику только через обычную Android public report queue и только если `shareReports=true`.

## Public Bot Callback Flow

Бот использует Telegram Inline Keyboard.

Поддерживаемые callback:

```text
v1:menu
v1:help
v1:status
v1:status-refresh
v1:regions
v1:operators
v1:region:{code}
v1:operator:{code}
v1:devices
v1:device:{linkId}
v1:check:{linkId}
v1:unlink-request:{linkId}
v1:unlink-confirm:{linkId}
v1:unlink-cancel:{linkId}
v1:about
v1:feedback
```

Первое нажатие отвязки не меняет D1. Подтверждение повторно проверяет, что link принадлежит текущему Telegram chat, и вызывает `revokeLinkFromTelegram`.

## D1

Production D1 binding:

```toml
[[d1_databases]]
binding = "DB"
database_name = "whitelist-public-service"
database_id = "..."
```

`database_id` нельзя придумывать. Если в `wrangler.toml` осталась заглушка `00000000-0000-0000-0000-000000000000`, production deployment не готов.

Миграции применяются вручную:

1. `cloudflare/public-service/migrations/0001_initial.sql`
2. `cloudflare/public-service/migrations/0002_area_city_operator_sources.sql`

## Telegram Webhook

Общий бот использует webhook:

```text
https://whitelist-monitor-tg-relay.regstar2.workers.dev/telegram/webhook
```

Webhook должен принимать:

- `message`
- `callback_query`

`secret_token` должен совпадать с `TELEGRAM_WEBHOOK_SECRET`.

Установка webhook отключает `getUpdates` для этого же Telegram-бота. Это нормально для общего бота. Личные relay Worker пользователей по-прежнему используют `/tg/getUpdates` в своих отдельных экземплярах.

## Сетевое разделение

- Whitelist-проверка использует конкретный cellular `Network` и `network.openConnection(...)`.
- Центральный Worker API использует default network через отдельный `PublicServiceClient`.
- Личный Telegram relay также использует default network.
- `bindProcessToNetwork(...)` не используется.

## Ограничения

Активный мониторинг - foreground service типа `dataSync`. Он не является круглосуточным сервером: Android 15+ и производители могут ограничивать длительность таких сервисов. При остановленном service remote command не создаёт ложного обещания мгновенной проверки.
