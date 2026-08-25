# Документация WhiteListChecker

Этот каталог разделяет актуальную документацию продукта `1.0` и исторические материалы старых версий. Приватные project-governance правила, AI prompts и local tool state сюда не публикуются.

## Актуальная документация 1.0

### Product

- [Scope версии 1.0](product/mvp-scope.md)

### Architecture

- [Текущая архитектура](architecture/current-architecture.md)
- [Технологический стек](architecture/tech-stack.md)
- [Маршрутизация сети и DNS](network-routing-notes.md)

### Integrations

- [Update Delivery через GitHub Releases](update-delivery.md)
- [Встроенный Feedback через GitHub Issues](feedback.md)
- [GitHub automation: Trusted CI, Project Sync и Release](github-automation.md)
- [Личный Cloudflare Worker для Telegram](cloudflare-worker/README.md)

### Testing

- [Manual test plan 1.0.0](testing/manual-test-plan.md)
- [Reliability checklist](testing/reliability-checklist.md)

### Release preparation

- [Соответствие стандартам Regstar2/sheduler](release/standards-compliance.md)
- [Release checklist 1.0.0](release/release-checklist.md)

Политика безопасности находится в корне: [../SECURITY.md](../SECURITY.md). Правила внешних изменений: [../CONTRIBUTING.md](../CONTRIBUTING.md).

## Исторические материалы

- `versions/` — документы отдельных development versions;
- `releases/` — опубликованные release notes;
- `archive/` — явно архивированные старые архитектурные/продуктовые документы.

Исторические документы не переписываются под текущую архитектуру только ради единообразия. Они могут упоминать удалённый central/public service, старые версии SDK или ранее рассматривавшийся local proxy.

## Что не относится к публичной документации

Следующие материалы являются локальными/private и защищены `.gitignore`:

- `AGENTS.md`;
- `.project-rules/`;
- `.codex/`, `.cursor/`, `.claude/`, `.ai/`;
- `docs/ai-prompts/`, `docs/private/`;
- private README/release templates;
- environment/secrets/signing state.

Если такой файл нужен для разработки, он должен существовать локально, но не в Git history новых изменений.
