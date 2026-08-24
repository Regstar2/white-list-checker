# Security Policy

## Модель безопасности v1.0

Начиная с `1.0.0`, WhiteListChecker не использует центральный публичный сервис. В Android runtime отсутствуют общий production Worker URL, public bot, device token, привязка устройства и server-side remote commands.

Проверки, история и статистика хранятся локально. Сетевые проверки выполняются через полученный cellular `Network`; TLS certificate и hostname verification для HTTPS target checks не отключаются.

## Личный Telegram Worker

Telegram-интеграция остаётся опциональной и принадлежит пользователю:

```text
Android -> user-owned Cloudflare Worker -> user's Telegram bot
```

`BOT_TOKEN` должен храниться только в secrets пользовательского Cloudflare Worker и не должен попадать в Android-приложение или Git.

В Android пользователь задаёт:

- Worker URL;
- Relay Secret;
- Telegram `chat_id` / получателей.

### Если утёк `RELAY_SECRET`

1. Замените `RELAY_SECRET` в Worker secrets.
2. Обновите Relay Secret в приложении.
3. Повторно проверьте Worker из приложения.

### Если утёк `BOT_TOKEN`

1. Выпустите новый token через BotFather.
2. Обновите `BOT_TOKEN` в Worker secrets.
3. Повторно проверьте Worker из приложения.

## Не публиковать

- `BOT_TOKEN`;
- `RELAY_SECRET`;
- Authorization headers;
- `local.properties`;
- release keystore и пароли к нему;
- приватные ключи;
- реальные логи, содержащие секреты или персональные данные.

Исторические документы старых версий могут упоминать удалённые secrets центрального сервиса. Они описывают прошлую архитектуру и не являются актуальными требованиями v1.0.

## Android permissions

Приложению нужны сетевые разрешения для запроса cellular `Network`, foreground service и уведомлений. Разрешение приблизительного местоположения, ранее использовавшееся для определения региона общего сервиса, в v1.0 удалено.

## Сообщение об уязвимости

Если вы нашли проблему безопасности, создайте GitHub Issue без публикации секретов, токенов и приватных пользовательских данных.
