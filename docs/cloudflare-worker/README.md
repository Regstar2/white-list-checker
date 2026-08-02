# Cloudflare Worker relay для Telegram-уведомлений

WhiteListChecker не хранит Telegram Bot Token в Android-приложении.

Для личных Telegram-уведомлений каждый пользователь создаёт своего Telegram-бота и свой Cloudflare Worker relay.

Личный relay остаётся отдельным от общего сервиса WhiteListChecker.

## Важно про старый Worker владельца проекта

Исторический Worker:

```text
whitelist-monitor-tg-relay
```

и URL:

```text
https://whitelist-monitor-tg-relay.carkov195.workers.dev
```

раньше могли использоваться владельцем проекта как личный relay. Теперь этот экземпляр преобразуется в центральный общий сервис:

```text
Android
-> BuildConfig.PUBLIC_SERVICE_BASE_URL
-> whitelist-monitor-tg-relay
-> D1
-> общий Telegram-бот
```

После перехода этот конкретный экземпляр больше не обязан поддерживать `/tg/*`.

Личные relay Worker пользователей продолжают поддерживаться:

```text
Android
-> пользовательский Worker URL
-> пользовательский Relay Secret
-> пользовательский Telegram-бот
```

Владелец проекта, если ему нужны личные уведомления отдельно от общего бота, может создать новый отдельный Worker по этому шаблону.

## Что нужно

1. Telegram-бот, созданный через BotFather.
2. Cloudflare Worker пользователя.
3. Worker secrets:
   - `BOT_TOKEN`
   - `RELAY_SECRET`
4. В приложении:
   - Worker URL;
   - Relay Secret;
   - Telegram recipients / `chat_id`.

## Пример Worker URL

Используйте свой Worker и свой Cloudflare subdomain:

```text
https://my-whitelist-relay.your-subdomain.workers.dev
```

Не используйте URL общего сервиса `https://whitelist-monitor-tg-relay.carkov195.workers.dev` как личный relay.

## Создание Worker в Cloudflare Dashboard

1. Открой Cloudflare Dashboard.
2. Перейди в **Workers & Pages**.
3. Нажми **Create application**.
4. Выбери **Worker**.
5. Задай любое имя для личного relay, например `my-whitelist-relay`.
6. Нажми **Deploy**.
7. Открой созданный Worker.
8. Нажми **Edit code**.
9. Замени код на содержимое [`telegram-relay-worker.js`](telegram-relay-worker.js).
10. Нажми **Deploy**.
11. Открой **Settings -> Variables and Secrets**.
12. Добавь Secret:
    - Name: `BOT_TOKEN`
    - Value: токен Telegram-бота
13. Добавь Secret:
    - Name: `RELAY_SECRET`
    - Value: длинный случайный секрет
14. Скопируй Worker URL.
15. Введи Worker URL и Relay Secret в приложении.

## Проверка в приложении

1. Открой экран **Уведомления**.
2. Включи Telegram-уведомления.
3. Введи Worker URL личного relay.
4. Введи Relay Secret.
5. Нажми **Проверить Worker**.
6. Нажми **Начать получение chat_id**.
7. Напиши личному боту `/start`.
8. Вернись в приложение.
9. Нажми **Получить chat_id**.
10. Добавь найденный чат в recipients.
11. Нажми **Отправить тестовое сообщение**.

## Endpoints личного relay

Android вызывает личный Worker через:

```text
POST <WORKER_URL>/tg/getMe
POST <WORKER_URL>/tg/getUpdates
POST <WORKER_URL>/tg/sendMessage
```

Все запросы должны содержать:

```text
X-Relay-Secret: <RELAY_SECRET>
```

`BOT_TOKEN` хранится только в secrets личного Worker.

## Команды бота и long polling

Для личного relay приложение может получать команды через:

```text
<WORKER_URL>/tg/getUpdates
```

Это относится только к пользовательскому личному Worker. Общий бот использует Telegram webhook `/telegram/webhook`, а не `/tg/getUpdates`.

## Частые ошибки

### Worker HTTP 401

Неверный Relay Secret или secret не добавлен в Cloudflare Worker.

### BOT_TOKEN is not configured

В Worker secrets не добавлен `BOT_TOKEN`.

### Forbidden: bot can't send messages to the bot

Указан ID самого бота, а не `chat_id` получателя. Нужно написать боту `/start` и получить `chat_id` через приложение.

### Chat not found

Бот не видел этот чат, пользователь не нажал `/start`, или бот не добавлен в группу.

### Timeout

Проверь доступность Cloudflare Worker и Telegram Bot API со стороны Cloudflare.

## Безопасность

- Не вставляй `BOT_TOKEN` в Android-приложение.
- Не публикуй `RELAY_SECRET`.
- Если `RELAY_SECRET` утёк, поменяй его в Cloudflare и в приложении.
- Для групп `chat_id` обычно отрицательный и может начинаться с `-100`.

См. также: [SECURITY.md](../../SECURITY.md)
