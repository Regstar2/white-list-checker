# Contributing

Спасибо за интерес к WhiteListChecker.

## Основные правила

1. Не добавляйте `BOT_TOKEN`, `RELAY_SECRET`, `local.properties` и другие секреты в репозиторий.
2. Не добавляйте прямой вызов `api.telegram.org` из Android-приложения.
3. Telegram должен работать только через user-owned Cloudflare Worker relay.
4. Не возвращайте локальный HTTP/SOCKS proxy как основной Telegram-путь.
5. Не отправляйте уведомления при каждой проверке.
6. Уведомления должны отправляться только при подтверждённой смене `WHITELIST_OFF ↔ WHITELIST_ON`.
7. Не используйте YouTube, Telegram и Discord как базовые сайты для определения белых списков.
8. Не добавляйте тяжёлые зависимости без необходимости.

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

## Перед Pull Request

Проверьте:

1. Проект компилируется.
2. APK запускается.
3. Ручная проверка мобильной сети работает.
4. Telegram Worker test работает, если настроен.
5. Уведомления не спамят без смены статуса.
6. В diff нет секретов.

## Архитектура

Текущая архитектура описана в:

- [README.md](README.md)
- [docs/stack.md](docs/stack.md)
- [docs/WhiteListChecker - current MVP.md](docs/WhiteListChecker%20-%20current%20MVP.md)
- [AGENTS.md](AGENTS.md)
