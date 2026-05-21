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
   - Telegram-получатели / chat_id

## Пример Worker URL

```text
https://whitelist-monitor-tg-relay.your-subdomain.workers.dev
```

## Создание Worker в Cloudflare Dashboard

1. Открой [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. Перейди в **Workers & Pages**.
3. Нажми **Create application**.
4. Выбери **Worker**.
5. Задай имя, например `whitelist-monitor-tg-relay`.
6. Нажми **Deploy**.
7. Открой созданный Worker.
8. Нажми **Edit code**.
9. Замени код на содержимое [`telegram-relay-worker.js`](telegram-relay-worker.js).
10. Нажми **Deploy**.
11. Открой **Settings → Variables and Secrets**.
12. Добавь Secret:
    - Name: `BOT_TOKEN`
    - Value: токен Telegram-бота
13. Добавь Secret:
    - Name: `RELAY_SECRET`
    - Value: длинный случайный секрет
14. Скопируй Worker URL.
15. Введи Worker URL и Relay Secret в приложении.

## Быстрая настройка (если Worker уже создан)

1. Создай бота через BotFather.
2. Скопируй bot token.
3. Вставь код из `telegram-relay-worker.js` в Worker.
4. Добавь secrets `BOT_TOKEN` и `RELAY_SECRET`.
5. Deploy и скопируй Worker URL.

## Проверка в приложении

1. Открой экран **Уведомления**.
2. Включи Telegram-уведомления.
3. Введи Worker URL.
4. Введи Relay Secret.
5. Нажми **Проверить Worker**.
6. Нажми **Начать получение chat_id**.
7. Напиши боту `/start`.
8. Вернись в приложение.
9. Нажми **Получить chat_id**.
10. Добавь найденный чат в получатели.
11. Нажми **Отправить тестовое сообщение**.

## Частые ошибки

### Worker HTTP 401

Неверный Relay Secret или secret не добавлен в Cloudflare Worker.

### BOT_TOKEN is not configured

В Worker secrets не добавлен `BOT_TOKEN`.

### Forbidden: bot can't send messages to the bot

Вы указали ID самого бота, а не `chat_id` получателя. Нужно написать боту `/start` и получить `chat_id` через приложение.

### Chat not found

Бот не видел этот чат, пользователь не нажал `/start`, или бот не добавлен в группу.

### Timeout

Проверьте доступность Cloudflare Worker и Telegram Bot API со стороны Cloudflare.

## Важно

- Не вставляй `BOT_TOKEN` в Android-приложение.
- Не публикуй `RELAY_SECRET`.
- Если `RELAY_SECRET` утёк, поменяй его в Cloudflare и в приложении.
- Для групп `chat_id` обычно отрицательный и может начинаться с `-100`.

См. также: [SECURITY.md](../../SECURITY.md)
