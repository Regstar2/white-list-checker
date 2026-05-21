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
- `local.properties`;
- release keystore;
- приватные ключи;
- реальные логи с секретами.

## Сообщение об уязвимости

Если вы нашли проблему безопасности, создайте GitHub Issue без публикации секретов и приватных данных.
