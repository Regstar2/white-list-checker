# Cloudflare Worker relay для личных Telegram-уведомлений

WhiteListChecker не хранит Telegram `BOT_TOKEN` в Android-приложении. Для личных Telegram-уведомлений пользователь создаёт собственного Telegram-бота и собственный Cloudflare Worker relay.

Начиная с WhiteListChecker `1.0.0`, это единственная поддерживаемая Telegram-интеграция приложения: центрального общего сервиса и общего Telegram-бота в runtime больше нет.

## Что нужно

1. Telegram-бот, созданный через BotFather.
2. Cloudflare Worker пользователя.
3. Worker secrets:
   - `BOT_TOKEN`;
   - `RELAY_SECRET`.
4. В приложении:
   - Worker URL;
   - Relay Secret;
   - Telegram recipients / `chat_id`.

## Пример Worker URL

Используйте только свой Worker и свой Cloudflare subdomain:

```text
https://my-whitelist-relay.your-subdomain.workers.dev
```

WhiteListChecker не содержит встроенного production URL и не предоставляет общий Worker.

## Создание Worker в Cloudflare Dashboard

1. Откройте Cloudflare Dashboard → **Workers & Pages**.
2. Создайте новый Worker, например `my-whitelist-relay`.
3. Замените его код содержимым [`telegram-relay-worker.js`](telegram-relay-worker.js).
4. В **Settings → Variables and Secrets** добавьте:
   - `BOT_TOKEN` — токен вашего Telegram-бота;
   - `RELAY_SECRET` — длинный случайный секрет.
5. Разверните Worker и скопируйте его URL.
6. Введите Worker URL и Relay Secret в WhiteListChecker.

## Проверка в приложении

1. Откройте **Уведомления → Telegram**.
2. Включите Telegram-уведомления.
3. Введите URL своего Worker и Relay Secret.
4. Нажмите **Проверить Worker**.
5. Запустите получение `chat_id`, отправьте своему боту `/start`, затем получите список чатов.
6. Добавьте нужного получателя.
7. Отправьте тестовое сообщение.

## Endpoints

Android вызывает пользовательский Worker через:

```text
POST <WORKER_URL>/tg/getMe
POST <WORKER_URL>/tg/getUpdates
POST <WORKER_URL>/tg/sendMessage
```

Все запросы должны содержать:

```text
X-Relay-Secret: <RELAY_SECRET>
```

`BOT_TOKEN` хранится только в secrets Worker.

## Telegram-команды

При включённых командах активный мониторинг может получать updates через личный Worker. Это не создаёт внешней зависимости от инфраструктуры WhiteListChecker: Worker и бот принадлежат пользователю.

## Частые ошибки

### Worker HTTP 401

Relay Secret не совпадает с `RELAY_SECRET` в Worker.

### BOT_TOKEN is not configured

В Worker secrets отсутствует `BOT_TOKEN`.

### Forbidden: bot can't send messages to the bot

Указан ID самого бота вместо `chat_id` получателя.

### Chat not found

Бот ещё не видел этот чат или пользователь не отправил ему `/start`.

### Timeout

Проверьте доступность собственного Cloudflare Worker и Telegram Bot API со стороны Cloudflare.

## Безопасность

- не вставляйте `BOT_TOKEN` в Android-приложение;
- не публикуйте `RELAY_SECRET`;
- при утечке `RELAY_SECRET` замените его в Cloudflare и приложении;
- не коммитьте реальные secrets или логи с ними.
