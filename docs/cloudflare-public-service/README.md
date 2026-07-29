# Cloudflare Public Service

Центральный публичный сервис находится в:

```text
cloudflare/public-service/
```

Он не заменяет личный `docs/cloudflare-worker/telegram-relay-worker.js`.

## Возможности MVP

- D1 schema для installations, reports, links, commands и Telegram users.
- Public aggregate endpoint.
- Android registration/settings/report/link/service-sync/command-result endpoints.
- Telegram webhook для публичного бота.
- Read-only catalog endpoints для регионов, городов и операторов.
- Одноразовые link codes.
- Remote command `CHECK_NOW` для связанных устройств.
- Scheduled cleanup.

## Secrets

Создавать только через Wrangler secrets:

```powershell
wrangler secret put PUBLIC_BOT_TOKEN
wrangler secret put TELEGRAM_WEBHOOK_SECRET
wrangler secret put DEVICE_TOKEN_PEPPER
wrangler secret put ADMIN_API_SECRET
```

Не помещать реальные secrets в `wrangler.toml`, README, `.env`, `.dev.vars` или Android `BuildConfig`.

## D1

Binding:

```text
DB
```

Миграции:

```powershell
cd cloudflare/public-service
npm ci
npm run migrations:local
```

Для production нужно создать D1 database в Cloudflare и подставить реальный `database_id` в рабочий `wrangler.toml`, который не содержит secrets.

## Локальные проверки

```powershell
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

`npm run build` использует `wrangler deploy --dry-run` и не выполняет production deployment.

## Telegram webhook

Webhook endpoint:

```text
POST /telegram/webhook
```

Worker проверяет:

```text
X-Telegram-Bot-Api-Secret-Token
```

Центральный бот использует webhook. `getUpdates` для центрального публичного бота не используется.

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

Все JSON contracts имеют `schemaVersion` и `requestId`, кроме простого public status query.

Catalog endpoints публичные, read-only, кэшируемые и не возвращают secrets. Android может использовать их как серверный каталог, но текущая сборка также содержит локальный fallback-каталог.

Для городской статистики используется отдельная настройка:

```text
MIN_UNIQUE_INSTALLATIONS_FOR_CITY
```

Если выборка по городу ниже порога, public status должен оставаться на уровне региона или показывать недостаточность данных.

## Retention

Начальные сроки задаются конфигурацией Worker:

- link codes: по TTL;
- commands: по `COMMAND_RETENTION_HOURS`;
- processed Telegram updates: 7 дней;
- reports: `REPORT_RETENTION_DAYS`.

## Ограничения

- Production deploy и настройка реального webhook выполняются вручную.
- Public data зависит от числа пользователей и выбранного региона/оператора.
- Remote checks доступны только пока Android foreground service работает и присылает heartbeat.
