<div align="center">

# WhiteListChecker

Android-приложение для проверки доступности мобильной сети и обнаружения наблюдаемых признаков режима белых списков. Проверки выполняются через явно полученный cellular `Network`, даже если Wi‑Fi остаётся основной сетью телефона.

**Русский** · [English](README_EN.md)

[![Trusted CI](https://github.com/Regstar2/white-list-checker/actions/workflows/trusted-ci.yml/badge.svg)](https://github.com/Regstar2/white-list-checker/actions/workflows/trusted-ci.yml)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Быстрый старт](#быстрый-старт) · [Скриншоты](#скриншоты) · [Документация](#документация) · [Релизы](https://github.com/Regstar2/white-list-checker/releases)

</div>

## О проекте

WhiteListChecker проверяет локальные и внешние сайты через отдельный cellular `Network`, использует DNS как дополнительный диагностический сигнал и сохраняет результаты локально. Главный сценарий — понять, как мобильная сеть фактически достигает контрольных ресурсов, не переключая весь процесс приложения на cellular и не подменяя измеряемый маршрут proxy/VPN transport.

Приложение определяет только наблюдаемые сетевые признаки. Оно не имеет доступа к внутренним правилам оператора, поэтому результат не является доказательством режима белых списков.

Начиная с `1.0.0`, центральный/public service удалён. Core functionality не требует Worker, бота или другой инфраструктуры владельца проекта. Личный Telegram остаётся опциональным и работает через Cloudflare Worker пользователя.

## Статус проекта

`v1.0.0` — первый стабильный релиз WhiteListChecker. Он опубликован **25 августа 2026 года** и доступен в [GitHub Releases](https://github.com/Regstar2/white-list-checker/releases/tag/v1.0.0).

В стабильную версию вошли local-first refactor, миграция Room `7 -> 8` без очистки пользовательских данных, исправление ложного статуса «Проблема DNS в мобильной сети», Update Delivery через официальный GitHub Releases, встроенный Feedback через GitHub Issues и актуальная GitHub/release automation.

Главный экран также приведён к текущему UI: шесть основных быстрых действий образуют симметричную сетку, а переход в «О приложении» находится в компактной иконке в правой части шапки.

Актуальный стабильный APK: `WhiteListChecker-v1.0.0-release.apk`. Контрольная сумма публикуется рядом с APK в `SHA256SUMS.txt`.

## Скриншоты

| Главный экран | Статистика | Настройки сайтов |
| --- | --- | --- |
| <img src="docs/assets/screenshots/home.jpg" width="220" alt="Главный экран WhiteListChecker"> | <img src="docs/assets/screenshots/statistics.jpg" width="220" alt="Экран статистики WhiteListChecker"> | <img src="docs/assets/screenshots/check-sites.jpg" width="220" alt="Настройки проверяемых сайтов"> |
| Настройки DNS | Фоновая проверка | Активный мониторинг |
| <img src="docs/assets/screenshots/check-dns.jpg" width="220" alt="Настройки DNS resolver"> | <img src="docs/assets/screenshots/background-checks.jpg" width="220" alt="Настройки фоновой проверки"> | <img src="docs/assets/screenshots/active-monitoring.jpg" width="220" alt="Настройки активного мониторинга"> |
| Локальные уведомления |  |  |
| <img src="docs/assets/screenshots/local-notifications.jpg" width="220" alt="Настройки локальных уведомлений"> |  |  |

## Возможности

- явное получение mobile `Network` через `ConnectivityManager.requestNetwork(...)`;
- проверки через cellular, даже когда Wi‑Fi включён;
- редактируемые группы сайтов `FOREIGN` и `LOCAL`;
- редактируемый список DNS resolver;
- DNS через UDP/53 с TCP/53 fallback;
- hostname fallback через `Network.getAllByName(...)` на полученном cellular `Network`;
- HTTPS target checks через network-bound socket factory с обычной TLS/hostname verification;
- сайты как основной сигнал классификации, DNS как вторичная диагностика;
- локальная история, статистика и timeline в Room;
- экспорт статистики в CSV/JSON/TXT;
- локальные Android-уведомления;
- фоновые проверки через WorkManager;
- активный мониторинг через foreground service;
- личные Telegram-уведомления и команды через user-owned Worker;
- асинхронная и ручная проверка новых версий через официальный GitHub Releases;
- stable/prerelease filtering для update check;
- встроенный переход к формам bug report и feature request в GitHub Issues без GitHub PAT в APK;
- RU/EN интерфейс.

## Быстрый старт

### Готовая стабильная версия

Скачайте `WhiteListChecker-v1.0.0-release.apk` из [релиза v1.0.0](https://github.com/Regstar2/white-list-checker/releases/tag/v1.0.0) и установите его средствами Android.

После запуска нажмите **«Проверить мобильную сеть»**. Мобильные данные должны быть доступны; Wi‑Fi можно оставить включённым.

### Сборка из исходников

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Требования

- Android 8.0+ (`minSdk 26`);
- мобильные данные и доступный cellular transport для основного checker;
- для сборки из исходников: JDK 17 и Android SDK 35;
- для личного Telegram: собственный Telegram-бот и user-owned Cloudflare Worker.

## Установка

Для обычной установки используйте APK только из [официальных GitHub Releases](https://github.com/Regstar2/white-list-checker/releases). Для `v1.0.0` это файл:

```text
WhiteListChecker-v1.0.0-release.apk
```

Контрольная сумма находится в `SHA256SUMS.txt` того же релиза.

Для development/debug сборки:

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Не удаляйте установленное приложение перед совместимым обновлением, если нужно сохранить локальную историю и настройки. При совместимой подписи новый APK устанавливается поверх предыдущей версии.

## Использование

1. Откройте главный экран и запустите проверку мобильной сети.
2. При необходимости измените списки сайтов и DNS в разделе проверок.
3. Используйте «Статистика» для истории состояния и экспорта данных.
4. Используйте «Диагностика» для route/DNS/site details.
5. Настройте локальные уведомления, WorkManager или active monitoring по необходимости.
6. Личный Telegram включайте только после настройки собственного Worker и бота.
7. Откройте «О приложении» через иконку информации в правом верхнем углу главного экрана, чтобы проверить обновления или перейти к информации о проекте.
8. Для сообщения об ошибке или предложения используйте встроенный Feedback, открывающий формы GitHub Issues.

## Конфигурация

Пользователь может изменять:

- enabled LOCAL/FOREIGN sites;
- enabled DNS resolvers и custom DNS entries;
- тему и язык;
- local notification policy;
- background check interval/policy;
- active monitoring options;
- personal Telegram Worker URL, Relay Secret и recipients.

`BOT_TOKEN` хранится только в secrets пользовательского Worker и не должен находиться в Android-приложении.

## Сеть и прокси

Основной checker намеренно не поддерживает proxy/VPN transport:

> Proxy support: N/A by project-specific design — WhiteListChecker измеряет прямой маршрут cellular-сети; прокси/VPN изменяет измеряемый транспорт и может сделать результат диагностики недостоверным.

Поэтому в checker path нет режимов `System / Direct / Custom proxy`, HTTP proxy или SOCKS5. Это проектное исключение из универсального `NETWORK_PROXY_STANDARD.md`, а не незавершённая функция.

VPN/tunnel рассматривается как ограничение достоверности: если он меняет путь трафика, результат может перестать описывать прямой доступ мобильного оператора.

Личный Telegram relay и updater — отдельные интеграции. Updater обращается к GitHub через обычную default network policy Android и не получает cellular `Network`, используемый checker'ом; поэтому update check не меняет измеряемый маршрут.

## Архитектура

```text
Compose UI
   |
   +-- MainViewModel / checker use cases
   |      +-- Cellular Network
   |      |      +-- DNS probes / cellular resolver
   |      |      +-- HTTPS target checks
   |      |      +-- WhitelistStateClassifier
   |      +-- Room / DataStore
   |      +-- WorkManager / Foreground service
   |      +-- optional user-owned Telegram relay
   |
   +-- AppUpdateViewModel
          +-- CheckForAppUpdateUseCase
                 +-- public GitHub Releases API
```

Room schema `8` удаляет устаревшую таблицу `pending_public_reports` миграцией `7 -> 8` без destructive migration. История, статистика и остальные локальные данные сохраняются.

Подробнее: [текущая архитектура](docs/architecture/current-architecture.md) и [технологический стек](docs/architecture/tech-stack.md).

## Безопасность

- TLS certificate/hostname verification target checks не отключается;
- production URL центрального сервиса отсутствует;
- `BOT_TOKEN` не хранится в APK;
- update check не содержит GitHub PAT/OAuth secret;
- встроенный Feedback не требует GitHub write credential в APK;
- release page строится только на официальном repository prefix;
- release keystore, `local.properties`, passwords и secrets исключены из Git;
- private governance/AI tool state не публикуется;
- personal Telegram secrets задаются пользователем и не должны попадать в issues/logs.

Подробнее: [SECURITY.md](SECURITY.md).

## Приватность

Проверки, история и статистика остаются на устройстве. Runtime-пути отправки результатов в центральный сервис больше нет.

Update check выполняет публичный запрос к GitHub Releases без GitHub-аккаунта/PAT и не отправляет историю проверок, настройки или другие пользовательские данные.

Feedback открывает публичные формы GitHub Issues. Приложение не должно автоматически прикладывать токены, Relay Secret, `chat_id`, приватные логи или другие чувствительные данные.

При включённом personal Telegram данные, которые пользователь явно отправляет своему боту, проходят через его собственный Worker и Telegram. Точный объём зависит от выбранного действия — test message, check report или commands.

## Диагностика

Если результат выглядит неверным:

1. откройте экран «Диагностика»;
2. сравните checked network и active network;
3. проверьте результаты LOCAL/FOREIGN sites и DNS отдельно;
4. убедитесь, что отдельный недоступный resolver не интерпретируется как общий DNS failure;
5. повторите проверку с Wi‑Fi включённым и выключенным;
6. для проверки прямого cellular route отключите VPN/tunnel.

Подробный физический сценарий: [docs/testing/manual-test-plan.md](docs/testing/manual-test-plan.md).

## Обновление

WhiteListChecker использует `BuildConfig.VERSION_NAME` как установленную версию и проверяет публичные релизы только в официальном репозитории `Regstar2/white-list-checker`.

Проверка выполняется:

- асинхронно при запуске без блокировки основного UI;
- вручную через **«О приложении» → «Проверить обновления»**.

Для stable-сборки prerelease (`alpha`, `beta`, `rc` и GitHub `prerelease=true`) не предлагается как обычное stable-обновление. При обнаружении новой версии приложение показывает номер версии и краткие release notes. Пользователь может открыть официальный GitHub Release или выбрать **«Позже»**.

Приложение не скачивает и не устанавливает APK автоматически. Установка остаётся под контролем пользователя и Android. Совместимо подписанный APK можно установить поверх существующей версии через Android package installer или:

```powershell
adb install -r path\to\WhiteListChecker.apk
```

Подпись нового APK должна совпадать с подписью установленной версии. `adb uninstall` удалит локальные данные и не подходит для проверки migration path.

Подробная политика: [docs/update-delivery.md](docs/update-delivery.md).

## Сборка

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Release signing берётся только из локальных Gradle properties/environment/local properties через `WL_RELEASE_*`; keystore и credentials не находятся в Git.

## Тестирование

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Update Delivery покрыт unit tests для SemVer, stable/prerelease selection, error mapping и GitHub response parsing. Для cellular routing, DNS, migration и update UI всё равно требуется физическое Android-устройство. Текущий manual plan: [docs/testing/manual-test-plan.md](docs/testing/manual-test-plan.md).

Перед выпуском `v1.0.0` были выполнены unit tests, Android lint, debug/release build, проверка подписи APK, физический UI smoke test, проверка Update Delivery, GitHub Feedback, миграции локальной базы и автоматизированного release workflow.

## Документация

- [Scope версии 1.0](docs/product/mvp-scope.md)
- [Текущая архитектура](docs/architecture/current-architecture.md)
- [Технологический стек](docs/architecture/tech-stack.md)
- [Маршрутизация сети и DNS](docs/network-routing-notes.md)
- [Update Delivery](docs/update-delivery.md)
- [Личный Cloudflare Worker для Telegram](docs/cloudflare-worker/README.md)
- [Manual test plan](docs/testing/manual-test-plan.md)
- [Соответствие стандартам sheduler](docs/release/standards-compliance.md)
- [Release checklist 1.0.0](docs/release/release-checklist.md)
- [Security policy](SECURITY.md)
- [История изменений](CHANGELOG.md)

`docs/versions/`, `docs/releases/` и `docs/archive/` сохраняют фактическую историю старых версий и могут описывать ранее существовавшие функции.

## Участие в разработке

Перед PR прочитайте [CONTRIBUTING.md](CONTRIBUTING.md). Private governance-файлы и локальное AI/tool state в публичный repository не коммитятся.

## Обратная связь

В приложении доступны действия для сообщения об ошибке и предложения улучшения. Они открывают публичные формы GitHub Issues проекта без хранения GitHub PAT/OAuth write credential в APK.

Также issue можно создать напрямую в [GitHub Issues](https://github.com/Regstar2/white-list-checker/issues).

Не прикладывайте токены, Relay Secret, `chat_id`, private logs или другие чувствительные данные.

## Ограничения

- классификация основана на наблюдаемых результатах и может ошибаться;
- proxy/VPN transport не поддерживается в checker path по назначению продукта;
- включённый VPN/tunnel может сделать результат нерепрезентативным для прямой cellular-сети;
- raw DNS/53 не шифруется;
- блокировка UDP/53 и TCP/53 может сделать конкретный custom resolver недоступным;
- public GitHub API может временно ограничивать частоту update checks;
- приложение не выполняет silent/background APK installation;
- WorkManager не гарантирует точное время выполнения;
- Android может ограничивать foreground service;
- personal Telegram требует собственного Worker и Telegram-бота.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
