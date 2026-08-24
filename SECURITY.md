# Security Policy

## Модель безопасности v1.0

Начиная с `1.0.0`, WhiteListChecker не использует центральный публичный сервис. В Android runtime отсутствуют общий production Worker URL, public bot, device token, привязка устройства и server-side remote commands.

Проверки, история и статистика хранятся локально. Сетевой checker выполняет DNS/HTTPS операции через явно полученный cellular `Network`; TLS certificate и hostname verification для HTTPS target checks не отключаются.

## Сетевая граница checker

Основной checker измеряет прямой cellular-маршрут. Для него действует проектное исключение:

> Proxy support: N/A by project-specific design — WhiteListChecker измеряет прямой маршрут cellular-сети; прокси/VPN изменяет измеряемый транспорт и может сделать результат диагностики недостоверным.

Поэтому HTTP/SOCKS/VPN transport не добавляется в основной checker path и не используется как скрытый fallback. Если на устройстве включён VPN/tunnel и он меняет фактический маршрут, результат нельзя интерпретировать как чистое измерение прямого доступа мобильного оператора.

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
- реальные логи, содержащие секреты или персональные данные;
- `AGENTS.md`, `.project-rules/`, `.codex/` и другое локальное governance/AI-tool state.

`.gitignore` содержит отдельные правила для private governance, local AI/tool state, environment files и signing secrets.

Исторические документы старых версий могут упоминать удалённые secrets центрального сервиса. Они описывают прошлую архитектуру и не являются актуальными требованиями v1.0.

## Android permissions

Приложению нужны сетевые разрешения для запроса cellular `Network`, foreground service и уведомлений. Разрешение приблизительного местоположения, ранее использовавшееся для определения региона общего сервиса, в v1.0 удалено.

## Release signing и обновления

Release keystore не хранится в репозитории. Release signing configuration передаётся локально через `WL_RELEASE_*` properties/environment/local properties.

Android разрешает in-place update только при совместимой подписи APK. Перед публикацией `1.0.0` нужно проверить сертификат финального release APK и совместимость обновления с официальным предыдущим release. `adb uninstall` не является допустимым способом проверки migration path, поскольку удаляет локальные данные.

## Сообщение об уязвимости

Для обычного bug report используйте публичные GitHub Issues проекта, но никогда не публикуйте там токены, Relay Secret, `chat_id`, приватные логи или другие чувствительные данные.

Если проблема безопасности требует передачи секрета или приватных данных, не создавайте публичный Issue с этими данными. Публичного безопасного канала для передачи секретов этот документ не обещает; сначала свяжитесь с владельцем репозитория через доступный профильный контакт без отправки самого секрета.
