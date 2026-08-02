# Cloudflare Public Service

Центральный публичный сервис находится в:

```text
cloudflare/public-service/
```

Фактический production Worker:

```text
whitelist-monitor-tg-relay
```

Production URL:

```text
https://whitelist-monitor-tg-relay.regstar2.workers.dev
```

Имя историческое: раньше этот экземпляр был личным Telegram relay владельца проекта, теперь он используется как центральный сервис WhiteListChecker. Новый Worker `whitelistchecker-public-service` не создаётся.

## Назначение

Сервис предоставляет:

- центральный Android API;
- общий Telegram-бот;
- D1-хранилище обезличенных отчётов;
- привязку Telegram-чата к устройству;
- удалённые команды `CHECK_NOW`;
- публичную агрегацию статусов;
- scheduled cleanup.

Он не заменяет пользовательские личные relay Worker. Шаблон личного relay остаётся в:

```text
docs/cloudflare-worker/telegram-relay-worker.js
```

## Два независимых сценария

Общий сервис:

```text
Android
-> BuildConfig.PUBLIC_SERVICE_BASE_URL
-> whitelist-monitor-tg-relay
-> D1
-> общий Telegram-бот
```

Личный Telegram relay пользователя:

```text
Android
-> пользовательский Worker URL
-> пользовательский Relay Secret
-> пользовательский Telegram-бот
```

URL общего сервиса встроен в приложение. URL личного relay вводится пользователем в настройках Telegram-уведомлений. Токен личного бота не передаётся центральному сервису.

После перехода старый экземпляр владельца проекта по адресу `whitelist-monitor-tg-relay` больше не обязан обслуживать `/tg/*`. Для личных уведомлений владелец может создать новый отдельный Worker по шаблону.

## Wrangler

Основная конфигурация:

```toml
name = "whitelist-monitor-tg-relay"
main = "src/index.ts"
```

`npm run build` остаётся dry-run:

```powershell
npm run build
```

Скрипт deploy существует только для явной ручной production-задачи:

```powershell
npm run deploy
```

В этой задаче production deployment не выполняется.

## Secrets

Центральный сервис ожидает runtime secrets:

```text
PUBLIC_BOT_TOKEN
TELEGRAM_WEBHOOK_SECRET
DEVICE_TOKEN_PEPPER
ADMIN_API_SECRET
```

Настройка выполняется вручную через Cloudflare Dashboard или отдельную явную `wrangler secret put` задачу.

Правила перехода:

- `PUBLIC_BOT_TOKEN` может получить прежнее значение `BOT_TOKEN`, если прежний личный бот становится общим ботом.
- `TELEGRAM_WEBHOOK_SECRET` должен быть новой случайной строкой.
- `DEVICE_TOKEN_PEPPER` должен быть отдельной случайной строкой.
- `ADMIN_API_SECRET` должен быть третьей отдельной случайной строкой.
- Старые `BOT_TOKEN` и `RELAY_SECRET` можно удалить только после успешного перехода.

Не помещать secrets в репозиторий, `wrangler.toml`, `.env`, `.dev.vars`, документацию или Android `BuildConfig`.

## D1

Binding:

```text
DB
```

Требуемая конфигурация:

```toml
[[d1_databases]]
binding = "DB"
database_name = "whitelist-public-service"
database_id = "..."
```

Нельзя придумывать `database_id`. Если в `cloudflare/public-service/wrangler.toml` стоит:

```text
00000000-0000-0000-0000-000000000000
```

production deployment не готов. Нужно создать или выбрать D1 database в Cloudflare Dashboard и вставить настоящий Database ID в `cloudflare/public-service/wrangler.toml`.

Миграции применяются вручную в Cloudflare D1 Console в порядке:

1. `cloudflare/public-service/migrations/0001_initial.sql`
2. `cloudflare/public-service/migrations/0002_area_city_operator_sources.sql`

Для изменений кнопок, имени Worker и URL новая миграция не нужна.

## Локальные проверки

```powershell
cd cloudflare/public-service
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

`npm run build` использует `wrangler deploy --dry-run` и не публикует Worker.

## Telegram webhook

Endpoint:

```text
POST /telegram/webhook
```

Полный production URL:

```text
https://whitelist-monitor-tg-relay.regstar2.workers.dev/telegram/webhook
```

Worker проверяет header:

```text
X-Telegram-Bot-Api-Secret-Token
```

Webhook должен принимать Telegram updates:

- `message`
- `callback_query`

Ручная настройка:

1. Сначала задеплоить Worker.
2. Добавить runtime secrets.
3. Установить webhook на `/telegram/webhook`.
4. Передать `secret_token`, совпадающий с `TELEGRAM_WEBHOOK_SECRET`.
5. Указать `allowed_updates`: `message`, `callback_query`.
6. Проверить через `getWebhookInfo`.
7. После deployment отправить боту `/start`.

Установка webhook отключает `getUpdates` для этого же Telegram-бота. Это ожидаемо, если прежний личный бот становится общим.

## Public Bot Buttons

Бот использует только Telegram Inline Keyboard. Reply Keyboard не добавляется.

Главное меню:

```text
[ Статус сети ]

[ Регион ] [ Оператор ]

[ Мои устройства ]

[ Помощь ] [ О проекте ]

[ Обратная связь ]
```

Callback:

```text
v1:status
v1:regions
v1:operators
v1:devices
v1:help
v1:about
v1:feedback
v1:status-refresh
v1:region:{regionCode}
v1:operator:{operatorCode}
v1:device:{linkId}
v1:check:{linkId}
v1:unlink-request:{linkId}
v1:unlink-confirm:{linkId}
v1:unlink-cancel:{linkId}
v1:menu
```

Старые сообщения Telegram не получают новую клавиатуру автоматически. После deployment пользователь должен отправить `/start`.

## API

Основные endpoints:

- `POST /api/v1/installations/register`
- `PUT /api/v1/installations/me/settings`
- `POST /api/v1/reports`
- `POST /api/v1/installations/me/link-codes`
- `GET /api/v1/installations/me/links`
- `DELETE /api/v1/installations/me/links/{linkId}`
- `POST /api/v1/installations/me/service-sync`
- `POST /api/v1/installations/me/commands/{commandId}/result`
- `DELETE /api/v1/installations/me`
- `GET /api/v1/catalog/regions`
- `GET /api/v1/catalog/regions/{regionCode}/cities`
- `GET /api/v1/catalog/operators`
- `GET /api/v1/public/status`

Публичный API в v0.8.16 не меняется.

## Cloudflare Workers Builds

Repository:

```text
Regstar2/WhiteListChecker
```

Root directory:

```text
/cloudflare/public-service
```

Production branch:

```text
main
```

Recommended Build command:

```text
npm run typecheck && npm run lint && npm test && npm run build
```

Deploy command:

```text
npx wrangler deploy
```

Version command:

```text
npx wrangler versions upload
```

Push в `main` запускает production deployment через Cloudflare Workers Builds, если он включён в Dashboard.

## Ограничения

- Production deployment выполняется отдельно.
- Remote D1 migrations не применяются автоматически.
- Secrets не читаются и не выводятся локально.
- Public data зависит от числа пользователей и выбранного региона/оператора.
- Remote checks доступны только пока Android foreground service работает и присылает heartbeat.
