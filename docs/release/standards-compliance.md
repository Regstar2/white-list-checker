# Соответствие WhiteListChecker стандартам Regstar2/sheduler

Аудит выполнен для подготовки `1.0.0` по актуальным правилам `Regstar2/sheduler` на 2026-08-24. Этот файл фиксирует только публичные продуктовые решения и release gates. Приватные `.project-rules/`, `AGENTS.md` и локальное AI/tool state в репозиторий не копируются.

## Статусы

| Стандарт | Статус | Решение для WhiteListChecker |
|---|---|---|
| `DEVELOPMENT_WORKFLOW.md` | Применяется | Работа ведётся через ограниченные Issues, отдельные ветки, PR, автоматические проверки и физические Android-сценарии для сетевых изменений. Исторические документы версий сохраняются как история. |
| `ENGINEERING_PRINCIPLES.md` | Применяется | Сохраняются границы `ui` / `domain` / `data`, manual composition root `AppContainer`, явный cellular `Network`, локальное persistence и минимальные зависимости. |
| `README_STANDARD.md` | Применяется | `README.md` и `README_EN.md` синхронизированы, используют одинаковые разделы и описывают только фактический local-first продукт. |
| `RELEASE_STANDARD.md` | Применяется | Для опубликованного `1.0.0` обязательны синхронные RU/EN release notes, точные installation/update steps, verification, known issues, assets и checksums. Финальные notes создаются после завершения release blockers. |
| `LOCALIZATION_STANDARD.md` | Применяется | Пользовательский Android UI поддерживает `ru` + `en`; новые UI-строки идут через Android resources. Update Delivery имеет синхронные RU/EN строки. |
| `NETWORK_PROXY_STANDARD.md` | `N/A` для checker path | См. отдельное обоснование ниже. Proxy/VPN transport нельзя добавлять в измеряемый cellular path. |
| `AUTO_UPDATE_STANDARD.md` | Применяется | Issue #9 реализует async/manual version check через public GitHub Releases, SemVer channel policy, dismissible update prompt и переход только на официальный release page. Silent/self APK install не используется. |
| `FEEDBACK_STANDARD.md` | Обязателен, отдельно | Реализуется в Issue #10. Здесь не дублируется. |
| `GITHUB_AUTOMATION_STANDARD.md` | Обязателен, отдельно | Реализуется в Issue #11. Здесь не дублируются trusted CI, Project Sync и release workflow. |
| `AI_TEXT_GUARDRAILS.md` | Применяется к публичным текстам | README, release notes и пользовательская документация должны отделять проверенные факты от предположений и не обещать неподтверждённые возможности. |
| `CODEX_SESSION_RECOVERY_STANDARD.md` | Локальный/private | `.codex/` является локальным state и игнорируется Git. Содержимое не публикуется. |
| `PROJECT_NAMING.md` | Выполнено для существующего проекта | Переименование перед `1.0.0` не требуется; каноническое имя продукта — `WhiteListChecker`, репозиторий — `white-list-checker`. |

## Project-specific исключение: Network / Proxy

Для основного сетевого checker действует решение:

> Proxy support: N/A by project-specific design — WhiteListChecker измеряет прямой маршрут cellular-сети; прокси/VPN изменяет измеряемый транспорт и может сделать результат диагностики недостоверным.

Причина проверяема архитектурно: приложение получает `TRANSPORT_CELLULAR` через `ConnectivityManager.requestNetwork(...)` и выполняет DNS/HTTPS операции через конкретный Android `Network`. Добавление HTTP/SOCKS/VPN transport между checker и target изменит объект измерения.

Следствия:

- `System / Direct / Custom proxy` не добавляются в основной checker path;
- silent fallback через proxy/VPN запрещён;
- VPN/tunnel документируется как ограничение достоверности результата;
- personal Telegram relay является отдельной опциональной интеграцией и не определяет transport checker;
- updater использует обычную default network policy Android, не получает cellular `Network` checker'а и не влияет на измеряемый route.

## Update Delivery

Issue #9 использует следующий fallback вместо silent self-update:

```text
BuildConfig.VERSION_NAME
    -> public GitHub Releases API
    -> SemVer/channel selection
    -> update prompt / manual check
    -> official GitHub Release page
    -> user-controlled APK installation
```

В APK нет GitHub PAT/OAuth secret. Stable installed build не получает GitHub prerelease или SemVer prerelease tag как обычное stable-обновление. Ошибки update check не блокируют запуск и сетевую диагностику.

Подробно: [../update-delivery.md](../update-delivery.md).

## Публичный vs приватный repository state

Из публичного дерева удалены:

- `AGENTS.md`;
- `docs/personal_app_development_path.md`;
- `docs/universal_development_principles.md`.

`.gitignore` защищает от повторной публикации:

- `/AGENTS.md`;
- `/.project-rules/`;
- `/.cursor/`, `/.codex/`, `/.claude/`, `/.ai/`;
- private README/release templates;
- `docs/ai-prompts/` и `docs/private/`;
- local environment/secrets.

Исторические `docs/versions/`, `docs/releases/` и `docs/archive/` сохраняются, даже если описывают удалённые функции. Это фактическая история проекта, а не текущая governance-конфигурация.

## Документация 1.0

Канонические актуальные документы:

- `README.md` / `README_EN.md` — пользовательский вход;
- `docs/product/mvp-scope.md` — границы продукта 1.0;
- `docs/architecture/current-architecture.md` — runtime architecture;
- `docs/architecture/tech-stack.md` — стек и platform versions;
- `docs/network-routing-notes.md` — детали routing/DNS;
- `docs/update-delivery.md` — update source, channel policy и self-update boundary;
- `docs/testing/manual-test-plan.md` — текущий физический test plan;
- `docs/release/release-checklist.md` — release gate;
- `SECURITY.md` — secrets, trust boundaries и vulnerability reporting;
- `CONTRIBUTING.md` — правила внешних изменений.

## Localization audit

Для текущего UI принято следующее правило:

- видимый текст, accessibility descriptions, dialogs, notifications и user-facing errors — Android resources `values/` + `values-en/`;
- technical identifiers, URLs, protocols, file names, machine-readable codes и developer logs могут оставаться нелокализованными;
- символьные UI glyphs (`<`, `>`) допустимы, если accessibility description локализован;
- неиспользуемые hardcoded RU labels в `AppScreen`, `TelegramUi.kt` и `LocalNotificationUi.kt` удалены, а не перенесены как мёртвые resources.

Android lint и unit tests остаются обязательным release gate. Критические сценарии должны быть вручную проверены на RU и EN перед stable release.

## Release blockers, вынесенные в отдельные Issues

После реализации #9 до stable `1.0.0` остаются отдельные обязательные задачи:

- #10 — Feedback / GitHub Issues;
- #11 — GitHub Automation / release orchestration.

Update Delivery также должен пройти физический RU/EN smoke test и финальную release-проверку перед tag.
