<div align="center">

# WhiteListChecker

Android-приложение для проверки доступности мобильной сети и обнаружения наблюдаемых признаков режима белых списков. Проверки выполняются через мобильное подключение, даже если Wi‑Fi остаётся основной сетью телефона.

**Русский** · [English](README_EN.md)

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

</div>

## О проекте

WhiteListChecker проверяет локальные и внешние сайты через явно полученный cellular `Network`, анализирует DNS как дополнительный диагностический сигнал и сохраняет результаты локально. Приложение предназначено для ручной диагностики, фоновых проверок, активного мониторинга и уведомлений об изменении наблюдаемого состояния сети.

Начиная с `1.0.0`, WhiteListChecker **не использует центральный публичный сервис**: в приложении нет общего Worker, публичного бота, отправки агрегированных пользовательских отчётов, привязки устройства к общему сервису и удалённых команд через него. Основная функциональность не требует инфраструктуры владельца проекта.

Личные Telegram-уведомления сохранены. Они работают через Cloudflare Worker, который пользователь разворачивает и настраивает самостоятельно.

WhiteListChecker определяет только наблюдаемые сетевые признаки и не имеет доступа к внутренним правилам оператора, поэтому результат не является доказательством режима белых списков.

## Возможности

- отдельный запрос мобильной сети через `ConnectivityManager.requestNetwork(...)`;
- работа через cellular `Network`, даже когда Wi‑Fi включён;
- редактируемые группы сайтов `FOREIGN` и `LOCAL`;
- редактируемый список DNS-серверов;
- DNS через UDP/53 с TCP/53 fallback;
- fallback разрешения доменов через полученный cellular `Network`, без process-wide binding;
- HTTPS-проверки через OkHttp с сохранением штатной TLS/hostname verification;
- сайты как основной сигнал классификации, DNS как вторичная диагностика;
- история и локальная статистика в Room;
- локальные Android-уведомления;
- фоновые проверки через WorkManager;
- активный мониторинг через foreground service;
- личные Telegram-уведомления и Telegram-команды через пользовательский Worker;
- RU/EN интерфейс.

## Быстрый старт

Требуются JDK 17, Android SDK 35 и Android 8.0+ (`minSdk 26`).

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

После запуска нажмите **Проверить мобильную сеть**. Wi‑Fi можно оставить включённым; мобильные данные должны быть доступны.

## DNS и Android Private DNS

WhiteListChecker сначала использует включённые пользовательские DNS-серверы, привязанные к полученному cellular `Network`. Недоступность отдельного Cloudflare/Google/Yandex resolver сама по себе не означает поломку DNS мобильной сети и не должна переопределять ясный результат проверки сайтов.

При недоступности custom resolver приложение может разрешить hostname через `Network.getAllByName(...)` на полученном мобильном `Network`. Process-wide `bindProcessToNetwork` не используется.

Текущий raw DNS transport не шифруется. Используйте только доверенные resolver.

## Личные Telegram-уведомления

Telegram-интеграция является пользовательской и не зависит от инфраструктуры WhiteListChecker:

```text
Android app
   -> user-owned Cloudflare Worker relay
   -> user's Telegram bot
```

Пользователь хранит в приложении URL своего Worker, Relay Secret и список получателей. Центрального Telegram-бота проекта в runtime нет.

Документация Worker: [docs/cloudflare-worker/README.md](docs/cloudflare-worker/README.md).

## Архитектура

```text
Android UI
   |
   v
ViewModel / use cases
   |
   +-- Cellular Network
   |      +-- DNS probes / resolver
   |      +-- HTTPS target checks
   +-- Room / DataStore
   +-- WorkManager / Foreground service
   +-- optional user-owned Telegram relay
```

Центральный Cloudflare Worker и очередь `pending_public_reports` удалены. При обновлении с `0.10.4` Room выполняет миграцию schema `7 -> 8`, удаляя только устаревшую таблицу публичных отчётов и сохраняя историю, статистику и остальные локальные данные.

Подробнее: [docs/architecture/current-architecture.md](docs/architecture/current-architecture.md) и [docs/network-routing-notes.md](docs/network-routing-notes.md).

## Приватность

Проверки, история и статистика остаются на устройстве. WhiteListChecker больше не имеет runtime-пути отправки результатов в центральный сервис.

При использовании личных Telegram-уведомлений данные, которые пользователь явно отправляет своему боту, проходят через его собственный Worker и Telegram. Bot token и Relay Secret не должны коммититься в Git.

## Безопасность

- TLS certificate/hostname verification target checks не отключается;
- приложение не хранит production URL общего сервиса;
- приложение не запрашивает разрешение приблизительного местоположения для определения региона общего сервиса;
- секреты личного Telegram Worker задаются пользователем;
- не публикуйте `local.properties`, release keystore, токены, пароли и логи с секретами.

Подробнее: [SECURITY.md](SECURITY.md).

## Сборка и тестирование

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

CI выполняет те же основные проверки для pull request.

## Документация

- [Текущая архитектура](docs/architecture/current-architecture.md)
- [Маршрутизация сети и DNS](docs/network-routing-notes.md)
- [Личный Cloudflare Worker для Telegram](docs/cloudflare-worker/README.md)
- [SECURITY.md](SECURITY.md)
- [История изменений](CHANGELOG.md)
- `docs/versions/` и release notes сохраняют историческое описание старых версий, включая ранее существовавший публичный сервис.

## Ограничения

- классификация основана на наблюдаемых результатах и может ошибаться;
- VPN остаётся отдельным ограничением Android-сетевого стека;
- блокировка и UDP/53, и TCP/53 может сделать конкретный custom DNS недоступным;
- WorkManager не гарантирует точное время выполнения;
- Android может ограничивать foreground service;
- личные Telegram-функции требуют собственного Worker и Telegram-бота пользователя.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
