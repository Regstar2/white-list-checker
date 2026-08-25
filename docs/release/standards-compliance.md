# Соответствие WhiteListChecker стандартам Regstar2/sheduler

Аудит выполнен для подготовки `1.0.0` по правилам владельца (репозиторий стандартов сейчас доступен как `Regstar2/template`, ранее `Regstar2/sheduler`). Этот файл фиксирует только публичные продуктовые решения и release gates. Приватные `.project-rules/`, `AGENTS.md` и локальное AI/tool state в репозиторий не копируются.

## Статусы

| Стандарт | Статус | Решение для WhiteListChecker |
|---|---|---|
| `DEVELOPMENT_WORKFLOW.md` | Применяется | Работа ведётся через ограниченные Issues, отдельные ветки, PR, автоматические проверки и физические Android-сценарии для сетевых изменений. Исторические документы версий сохраняются как история. |
| `ENGINEERING_PRINCIPLES.md` | Применяется | Сохраняются границы `ui` / `domain` / `data`, manual composition root `AppContainer`, явный cellular `Network`, локальное persistence и минимальные зависимости. |
| `README_STANDARD.md` | Применяется | `README.md` и `README_EN.md` синхронизированы, используют одинаковые разделы и описывают только фактический local-first продукт. |
| `RELEASE_STANDARD.md` | Применяется | Для опубликованного `1.0.0` обязательны синхронные RU/EN release notes, точные installation/update steps, verification, known issues, assets и checksums. `scripts/release.ps1` формирует signed APK + `SHA256SUMS.txt`; финальные notes создаются после завершения release gates. |
| `LOCALIZATION_STANDARD.md` | Применяется | Пользовательский Android UI поддерживает `ru` + `en`; новые UI-строки идут через Android resources. Update Delivery и Feedback имеют синхронные RU/EN строки. |
| `NETWORK_PROXY_STANDARD.md` | `N/A` для checker path | Proxy/VPN transport нельзя добавлять в измеряемый cellular path; обоснование ниже. |
| `AUTO_UPDATE_STANDARD.md` | Применяется | Issue #9 реализовал async/manual version check, SemVer channel policy, rate-limit-safe stable lookup, dismissible prompt и переход только на официальный release page. Silent/self APK install не используется. |
| `FEEDBACK_STANDARD.md` | Реализован | Issue #10 добавил in-app bug/feature entry points, structured GitHub Issue Forms, RU/EN UI и запрет автоматической передачи секретов. Post-merge rendering форм остаётся release smoke test. |
| `GITHUB_AUTOMATION_STANDARD.md` | Реализован, integration gate остаётся | Issue #11 добавляет `trusted-ci.yml`, `project-sync.yml`, `release.yml`, `scripts/ci.ps1` и `scripts/release.ps1`; legacy unrestricted `android.yml` удаляется. После merge требуется интеграционный smoke self-hosted CI/Project Sync. |
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

Issue #9 использует безопасный fallback вместо silent self-update:

```text
BuildConfig.VERSION_NAME
    -> official GitHub release lookup
    -> SemVer/channel selection
    -> update prompt / manual check
    -> official GitHub Release page
    -> user-controlled APK installation
```

В APK нет GitHub PAT/OAuth secret. Stable installed build не получает prerelease как обычное stable-обновление. Ошибки update check не блокируют запуск и сетевую диагностику.

Подробно: [../update-delivery.md](../update-delivery.md).

## Feedback

Issue #10 использует browser-based GitHub feedback, а не write API из APK:

```text
About -> Report a bug / Suggest an improvement
      -> official github.com/Regstar2/white-list-checker/issues/new
      -> structured Issue Form
```

Приложение предзаполняет только безопасную версию в заголовке. Логи, `BOT_TOKEN`, Relay Secret, `chat_id`, personal Worker URL, пароли и signing data автоматически не передаются.

Подробно: [../feedback.md](../feedback.md).

## GitHub automation

Issue #11 реализует стандартную модель:

```text
GitHub workflow = orchestration/trust gate
scripts/ci.ps1 = единый Android CI entry point
scripts/release.ps1 = единый Android release entry point
```

- `trusted-ci.yml` использует `pull_request_target`, но self-hosted runner выделяется только после owner + same-repository gate;
- external/fork PR code не должен исполняться на persistent self-hosted machine;
- `project-sync.yml` добавляет owner Issues в `https://github.com/users/Regstar2/projects/2` через `ADD_TO_PROJECT_PAT`;
- `release.yml` работает с exact tag, повторно запускает CI, требует непустой `dist/` и публикует через минимальный `GITHUB_TOKEN contents: write`;
- release signing credentials не передаются в PR CI и хранятся только вне repository;
- `scripts/release.ps1` требует signed non-debug APK и создаёт SHA-256 checksum.

Подробно: [../github-automation.md](../github-automation.md).

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
- `docs/feedback.md` — feedback trust/privacy boundary;
- `docs/github-automation.md` — trusted CI, Project Sync и release orchestration;
- `docs/testing/manual-test-plan.md` — текущий физический test plan;
- `docs/release/release-checklist.md` — release gate;
- `SECURITY.md` — secrets, trust boundaries и vulnerability reporting;
- `CONTRIBUTING.md` — правила внешних изменений.

## Localization audit

Для текущего UI принято следующее правило:

- видимый текст, accessibility descriptions, dialogs, notifications и user-facing errors — Android resources `values/` + `values-en/`;
- technical identifiers, URLs, protocols, file names, machine-readable codes и developer logs могут оставаться нелокализованными;
- символьные UI glyphs (`<`, `>`) допустимы, если accessibility description локализован;
- неиспользуемые hardcoded RU labels удаляются, а не переносятся как мёртвые resources.

Android lint и unit tests остаются обязательным release gate. Критические сценарии должны быть вручную проверены на RU и EN перед stable release.

## Оставшиеся release integration gates

Реализация обязательных стандартов #9–#11 находится в release scope. До stable `1.0.0` остаются фактические проверки:

- controlled update-available flow для #9;
- post-merge rendering Issue Forms для #10;
- post-merge self-hosted Trusted CI и Project Sync для #11;
- release signing/package dry run через `scripts/release.ps1` на trusted environment;
- финальные RU/EN и cellular smoke tests из release checklist.
