# Cloudflare Worker relay для Telegram-уведомлений

Whitelist Checker не хранит Telegram Bot Token в Android-приложении.

Для Telegram-уведомлений каждый пользователь создаёт своего Telegram-бота и свой Cloudflare Worker.

## Что нужно

1. Telegram-бот, созданный через BotFather.
2. Cloudflare Worker.
3. Worker secrets:
   - `BOT_TOKEN`
   - `RELAY_SECRET`
4. В приложении:
   - Worker URL
   - Relay Secret
   - Chat ID

## Пример Worker URL

https://whitelist-monitor-tg-relay.your-subdomain.workers.dev

## Настройка

1. Создай бота через BotFather.
2. Скопируй bot token.
3. Создай новый Cloudflare Worker.
4. Вставь код из `telegram-relay-worker.js`.
5. Открой Worker → Settings → Variables and Secrets.
6. Добавь Secret:
   - Name: `BOT_TOKEN`
   - Value: токен Telegram-бота
7. Добавь Secret:
   - Name: `RELAY_SECRET`
   - Value: длинный случайный секрет
8. Нажми Deploy.
9. Скопируй Worker URL.
10. В приложении введи Worker URL и Relay Secret.
11. Получи Chat ID через `/start`.

## Важно

- Не вставляй `BOT_TOKEN` в Android-приложение.
- Не публикуй `RELAY_SECRET`.
- Если `RELAY_SECRET` утёк, поменяй его в Cloudflare и в приложении.
- Для групп `chat_id` обычно отрицательный и может начинаться с `-100`.
