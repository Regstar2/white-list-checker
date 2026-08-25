# WhiteListChecker 1.0 — текущая архитектура

## Граница продукта

WhiteListChecker — local-first Android-приложение для диагностики мобильной сети. Основные проверки, история, статистика, background monitoring и local notifications не зависят от инфраструктуры владельца проекта.

Центральный/public service старых версий удалён в `1.0.0`. Исторические release notes и version documents могут продолжать описывать его как часть прошлых версий.

Канонический scope: [../product/mvp-scope.md](../product/mvp-scope.md). Технологический стек: [tech-stack.md](tech-stack.md).

## Runtime components

```text
Compose UI
   |
   +-- MainViewModel
   |      +-- WhitelistCheckUseCase
   |      |      +-- CellularNetworkProvider
   |      |      +-- DNS probes / CellularDnsResolver
   |      |      +-- MobileSiteChecker
   |      |      +-- WhitelistStateClassifier
   |      |
   |      +-- CheckAndNotifyUseCase
   |      |      +-- local history / statistics / timeline
   |      |      +-- local Android notifications
   |      |      +-- optional personal Telegram relay
   |      |
   |      +-- WorkManager background checks
   |      +-- ActiveMonitoringService
   |
   +-- AppUpdateViewModel
          +-- CheckForAppUpdateUseCase
                 +-- GitHubReleaseSource
```

`AppContainer` является composition root и явно соединяет platform/data implementations с domain use cases. UI отображает state и передаёт действия; checker/classification/persistence не реализуются внутри Compose components.

## Cellular network isolation

Приложение явно получает cellular `Network` через `ConnectivityManager.requestNetwork(...)`. DNS sockets и target HTTP(S) connections создаются через этот network. Process-wide `bindProcessToNetwork` не используется.

Custom DNS является предпочтительным диагностическим resolver path. UDP/53 failures, представляющие transport timeout/network failure, могут использовать TCP/53 fallback. Если настроенные resolver не разрешают target, hostname resolution может fallback-иться на `Network.getAllByName(...)` на полученном cellular network.

Site checks являются основным классификационным сигналом. DNS availability — вторичная диагностика; недоступность одного или нескольких public resolver сама по себе не создаёт `MOBILE_DNS_FAILURE` при ясном site result.

## Network / Proxy project exception

Для основного checker действует намеренное исключение из универсального proxy-стандарта:

> Proxy support: N/A by project-specific design — WhiteListChecker измеряет прямой маршрут cellular-сети; прокси/VPN изменяет измеряемый транспорт и может сделать результат диагностики недостоверным.

Следовательно:

- checker не предоставляет `System / Direct / Custom proxy` modes;
- HTTP proxy, SOCKS5 и VPN/tunnel не являются checker transports;
- скрытый fallback через proxy/VPN запрещён;
- включённый VPN/tunnel рассматривается как ограничение достоверности прямого cellular measurement;
- optional personal Telegram relay и updater являются отдельными network integrations и не меняют checker path.

Updater использует обычный OkHttp client через default network policy Android и никогда не получает cellular `Network`, выделенный для диагностики. Подробно: [../update-delivery.md](../update-delivery.md).

Полная фиксация применимости стандартов: [../release/standards-compliance.md](../release/standards-compliance.md).

## Update Delivery

Установленная версия берётся из `BuildConfig.VERSION_NAME`. `AppUpdateViewModel` асинхронно запускает `CheckForAppUpdateUseCase`, который читает публичный GitHub Releases API репозитория проекта через `GitHubReleaseSource`.

Основные правила:

- GitHub PAT/OAuth token в APK отсутствует;
- draft releases игнорируются;
- stable installed build не получает prerelease как обычное обновление;
- SemVer prerelease tag дополнительно считается prerelease независимо от GitHub metadata;
- при наличии новой версии приложение показывает номер версии и предлагает открыть официальный release page;
- пользователь может выбрать «Позже»;
- background/startup failure не блокирует запуск и основной checker;
- приложение не скачивает и не устанавливает APK автоматически.

URL release page строится приложением на фиксированном repository prefix, а не доверяется произвольному URL из API response.

## Local persistence

Room хранит:

- check history и per-target results;
- derived statistics;
- whitelist timeline;
- очередь personal Telegram notifications.

DataStore/shared preferences хранят application, checker, notification и monitoring settings.

Schema `8` удаляет устаревшую `pending_public_reports` миграцией `7 -> 8`. Миграция не удаляет local history, statistics, targets, DNS configuration или personal Telegram settings.

Физический debug update `0.10.4 -> 1.0.0` без очистки app data подтвердил сохранение истории и отсутствие UI общего сервиса. Release-to-release certificate/update path остаётся отдельным release gate.

## Telegram integration

Остаётся только personal Telegram flow:

```text
Android -> user-owned Cloudflare Worker -> user's Telegram bot
```

Пользователь задаёт Worker URL и Relay Secret. Worker опционален и не управляется проектом WhiteListChecker.

Active monitoring может слушать команды через personal Worker, когда пользователь явно включает Telegram commands.

`BOT_TOKEN` находится только в Worker secrets и не хранится в Android APK.

## Localization boundary

Пользовательский UI использует Android resources с обязательными RU/EN locale. User-facing text, dialogs, notifications, accessibility descriptions и понятные ошибки должны проходить через resources; technical identifiers, protocols, URLs, machine-readable codes и developer logs могут оставаться стабильными техническими значениями.

Неиспользуемые hardcoded UI helper labels удаляются, а не сохраняются как отдельная параллельная система локализации.

## Removed in 1.0

- project-owned central Cloudflare Worker;
- public Telegram bot;
- device registration/linking with a central service;
- aggregate public report upload and queue;
- central remote-command loop;
- hardcoded `PUBLIC_SERVICE_BASE_URL`;
- approximate-location permission used for shared-service region detection.
