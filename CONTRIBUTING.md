# Contributing

Спасибо за интерес к WhiteListChecker.

## Область проекта

WhiteListChecker — Android-приложение для диагностики прямого cellular-маршрута. Изменения должны сохранять этот смысл: основной checker измеряет сеть оператора, а не доступность через сторонний tunnel/proxy.

## Основные правила

1. Не добавляйте `BOT_TOKEN`, `RELAY_SECRET`, `local.properties`, release keystore, пароли и другие секреты в репозиторий.
2. Не добавляйте прямой вызов `api.telegram.org` из Android-приложения. Личный Telegram работает только через user-owned Cloudflare Worker relay.
3. Не добавляйте proxy/VPN transport в основной checker path. Proxy support для измеряемого cellular-маршрута имеет статус `N/A` по назначению продукта: proxy/VPN меняет измеряемый транспорт и может сделать диагностику недостоверной.
4. Не используйте process-wide `bindProcessToNetwork`; сетевые операции checker должны работать через явно полученный cellular `Network`.
5. Не отключайте TLS certificate/hostname verification для HTTPS target checks.
6. Не отправляйте уведомления при каждой проверке, если это не соответствует выбранной пользователем notification policy.
7. Не используйте YouTube, Telegram и Discord как базовые контрольные сайты для классификации белых списков без отдельного обоснования.
8. Не добавляйте тяжёлые зависимости без необходимости.
9. Новые пользовательские строки должны находиться в Android resources и иметь RU/EN варианты. Developer logs, machine-readable error codes, команды и технические идентификаторы локализовать не требуется.
10. Приватные governance/tool-state материалы (`AGENTS.md`, `.project-rules/`, `.codex/`, `.cursor/`, `.claude/`, `.ai/`) не коммитятся.

## Сборка и проверки

Минимальная локальная проверка:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Если изменение затрагивает сетевой checker, дополнительно требуется физическое Android-устройство и ручная проверка cellular-маршрута.

## Перед Pull Request

Проверьте:

1. `testDebugUnitTest`, `lintDebug`, `assembleDebug` и `assembleRelease` проходят.
2. В diff нет секретов, локальных governance-файлов и AI/tool state.
3. Новые пользовательские строки существуют в `values/` и `values-en/`.
4. Изменение не добавляет скрытый proxy/VPN fallback в checker path.
5. Для изменения сетевой логики описан и выполнен релевантный ручной сценарий.
6. README/документация обновлены, если изменилось пользовательское поведение или архитектурное ограничение.

## Документация

Актуальные документы:

- [README.md](README.md) / [README_EN.md](README_EN.md)
- [текущая архитектура](docs/architecture/current-architecture.md)
- [технологический стек](docs/architecture/tech-stack.md)
- [scope v1.0](docs/product/mvp-scope.md)
- [ручной test plan](docs/testing/manual-test-plan.md)
- [соответствие стандартам](docs/release/standards-compliance.md)
- [release checklist](docs/release/release-checklist.md)
- [SECURITY.md](SECURITY.md)

Исторические документы в `docs/versions/`, `docs/releases/` и `docs/archive/` могут описывать старую архитектуру и не являются текущими требованиями к коду.
