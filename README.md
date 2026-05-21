# WhiteListChecker

![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)
![Android](https://img.shields.io/badge/Android-minSdk%2026-green)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

Android-приложение для проверки мобильной сети и обнаружения признаков режима белых списков.

Приложение проверяет **именно мобильную сеть** через `ConnectivityManager` + `Network.openConnection()`, даже если активная сеть телефона — Wi-Fi.

Текущая версия: **0.7.0**

## Статус проекта

Проект находится на стадии **MVP**.

Уже реализовано:

- ручная проверка мобильной сети;
- классификация признаков белых списков;
- локальные уведомления;
- Telegram через пользовательский Cloudflare Worker;
- очередь Telegram;
- автопроверка через WorkManager;
- диагностика;
- редактируемые сайты.

Возможны ложные срабатывания: приложение определяет **симптомы сетевого режима**, а не внутренние правила оператора.

## Безопасность

Не публикуйте:

- `BOT_TOKEN`;
- `RELAY_SECRET`;
- `local.properties`;
- release keystore;
- реальные логи с секретами.

`BOT_TOKEN` должен храниться только в Cloudflare Worker secrets.

Подробнее: [SECURITY.md](SECURITY.md)

## Возможности

- ручная проверка мобильной сети;
- группы **FOREIGN** / **LOCAL** сайтов;
- классификация:
  - белые списки не обнаружены;
  - похоже на включённые белые списки;
  - мобильного интернета нет;
  - проблема DNS в мобильной сети;
  - частичная проблема сети;
  - мобильная сеть недоступна;
- подтверждение смены состояния через **2 проверки подряд**;
- локальные Android-уведомления **только при смене статуса**;
- Telegram-уведомления через **пользовательский Cloudflare Worker relay**;
- несколько Telegram-получателей;
- очередь неотправленных Telegram-сообщений (Room);
- автопроверка через **WorkManager**;
- редактируемый список сайтов;
- диагностика и подробный отчёт.

## Стек

- Kotlin
- Android native
- Jetpack Compose
- Material 3
- MVVM + UseCase
- Coroutines / Flow
- DataStore Preferences
- Room
- WorkManager
- OkHttp (только HTTPS к Cloudflare Worker relay)

`minSdk` 26 · `compileSdk` / `targetSdk` 35

## Документация

- [Текущий MVP](docs/WhiteListChecker%20-%20current%20MVP.md)
- [Стек технологий](docs/stack.md)
- [Cloudflare Worker relay](docs/cloudflare-worker/README.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Инструкции для агента](AGENTS.md)
- Версии: [v0.1](docs/versions/v0.1.md) … [v0.6](docs/versions/v0.6.md)
- Legacy: [архив](docs/archive/)

## Требования

- JDK 17+
- Android SDK (`ANDROID_HOME` или `local.properties` → `sdk.dir`)
- Platform `android-35`, Build-Tools 35.x
- Git

Скопируй `local.properties.example` → `local.properties` и укажи путь к SDK локально.

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Установка на устройство

```powershell
adb devices
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

## Telegram через Cloudflare Worker

WhiteListChecker **не хранит BOT_TOKEN** в Android.

Каждый пользователь сам создаёт:

1. Telegram-бота через BotFather;
2. Cloudflare Worker;
3. Worker secrets:
   - `BOT_TOKEN`
   - `RELAY_SECRET`.

В приложении вводятся:

- Worker URL;
- Relay Secret;
- Telegram-получатели / `chat_id`.

Подробная инструкция: [docs/cloudflare-worker/README.md](docs/cloudflare-worker/README.md)

Схема:

```text
Android → user's Cloudflare Worker → Telegram Bot API
```

## Дорожная карта

| Версия | Содержание |
|--------|------------|
| v0.1 | Ручная проверка мобильной сети |
| v0.2 | Группы FOREIGN / LOCAL |
| v0.3 | Подтверждение смены состояния |
| v0.3.5 | Локальные уведомления |
| v0.4.2 | Telegram через Cloudflare Worker relay |
| v0.5 | Очередь Telegram |
| v0.6 | WorkManager автопроверка |
| v0.7 | UI cleanup, редактируемые сайты, диагностика |

## Лицензия

Проект распространяется под лицензией MIT. См. [LICENSE](LICENSE).
