# Security Policy

## Секреты

WhiteListChecker не должен хранить Telegram `BOT_TOKEN` в Android-приложении.

`BOT_TOKEN` должен находиться только в Cloudflare Worker secrets.

В Android-приложении пользователь вводит только:

- Worker URL;
- Relay Secret;
- Telegram chat_id / получателей.

## Если секрет утёк

### Если утёк `RELAY_SECRET`

1. Открой Cloudflare Worker.
2. Замени `RELAY_SECRET` в Worker secrets.
3. Обнови Relay Secret в приложении.
4. Проверь Worker через приложение.

### Если утёк `BOT_TOKEN`

1. Открой BotFather.
2. Выпусти новый token для бота.
3. Обнови `BOT_TOKEN` в Cloudflare Worker secrets.
4. Проверь Worker через приложение.

## Что не нужно публиковать

Не публикуйте:

- `BOT_TOKEN`;
- `RELAY_SECRET`;
- `PUBLIC_BOT_TOKEN`;
- `TELEGRAM_WEBHOOK_SECRET`;
- `DEVICE_TOKEN_PEPPER`;
- `ADMIN_API_SECRET`;
- device token;
- Authorization headers;
- `local.properties`;
- release keystore;
- приватные ключи;
- реальные логи с секретами.

## Центральный публичный сервис

Центральный Cloudflare Worker использует отдельные secrets:

- `PUBLIC_BOT_TOKEN`;
- `TELEGRAM_WEBHOOK_SECRET`;
- `DEVICE_TOKEN_PEPPER`;
- `ADMIN_API_SECRET`.

Android-приложение не хранит `PUBLIC_BOT_TOKEN` и не вызывает Telegram Bot API напрямую для центрального публичного бота.

`BuildConfig.PUBLIC_SERVICE_BASE_URL` не является секретом: это только адрес центрального API. Он не должен содержать tokens, query parameters или пользовательский `Relay Secret`.

Device token центрального сервиса:

- возвращается Android только при регистрации установки;
- хранится на устройстве через Android Keystore-backed encryption;
- в D1 хранится только как hash/HMAC с server-side pepper;
- не должен попадать в логи, UI, bug reports или документацию.

Публичная статистика не должна раскрывать `installationId`, `chatId`, token hash или raw report отдельного устройства.

Автоопределение региона и города:

- запускается только по явному действию пользователя;
- использует только приблизительное location permission;
- не сохраняет и не отправляет координаты, raw address, улицу или postal code.

Автоопределение оператора не должно читать номер телефона, IMEI, IMSI или SIM serial.

## Сообщение об уязвимости

Если вы нашли проблему безопасности, создайте GitHub Issue без публикации секретов и приватных данных.
