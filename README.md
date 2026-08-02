# WhiteListChecker

## v0.8.16 - central Worker migration

Фактический общий Cloudflare Worker:

```text
whitelist-monitor-tg-relay
```

Production URL общего сервиса:

```text
https://whitelist-monitor-tg-relay.carkov195.workers.dev
```

Имя Worker историческое: раньше этот экземпляр был личным Telegram relay владельца проекта, теперь он используется как центральный сервис WhiteListChecker. Новый Worker `whitelistchecker-public-service` не создаётся.

Встроенный `BuildConfig.PUBLIC_SERVICE_BASE_URL` Android указывает на общий сервис. Пользовательский Worker URL личного Telegram relay по-прежнему вводится отдельно в настройках Telegram-уведомлений; личные relay Worker и шаблон `docs/cloudflare-worker/telegram-relay-worker.js` продолжают поддерживаться.

![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)
![Android](https://img.shields.io/badge/Android-minSdk%2026-green)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

Android-приложение для проверки мобильной сети и обнаружения признаков режима белых списков.

Приложение проверяет **именно мобильную сеть** через `ConnectivityManager` + `Network.openConnection()`, даже если активная сеть телефона — Wi-Fi.

Текущая dev-версия: **0.8.15**

Последний релиз: [v0.7.1-hotfix](docs/releases/v0.7.1-hotfix.md) · [GitHub Releases](https://github.com/Regstar2/WhiteListChecker/releases)

## Статус проекта

Проект находится на стадии **MVP/beta**.

Приложение уже можно использовать для ручной проверки и тестовой автопроверки, но возможны ложные срабатывания и особенности работы на разных прошивках Android.

WhiteListChecker определяет **признаки сетевого режима**, а не внутренние правила оператора.

Уже реализовано:

- ручная проверка мобильной сети;
- классификация признаков белых списков;
- локальные уведомления;
- Telegram через пользовательский Cloudflare Worker;
- очередь Telegram;
- автопроверка через WorkManager;
- политики уведомлений для автопроверки;
- активный мониторинг через foreground service с постоянным уведомлением;
- команды Telegram-бота `/status`, `/check`, `/help` во время активного мониторинга;
- центральный Cloudflare-сервис с публичным Telegram-ботом для агрегированных данных;
- добровольная отправка обезличенных результатов в общий сервис;
- фиксированный URL центрального сервиса из `BuildConfig`, без пользовательского поля URL;
- автоматическое и ручное определение региона, города и мобильного оператора для public reports;
- привязка Telegram-чата к устройству для удалённой проверки во время активного мониторинга;
- диагностика;
- редактируемые сайты.

## Android permissions

Приложение использует:

- `INTERNET` — сетевые проверки;
- `ACCESS_NETWORK_STATE` — чтение активной сети и состояния подключений;
- `CHANGE_NETWORK_STATE` — требуется для `ConnectivityManager.requestNetwork()`, чтобы явно запросить мобильную сеть при активном Wi-Fi;
- `ACCESS_COARSE_LOCATION` — разовое приблизительное местоположение по явному нажатию пользователя для определения региона и города;
- `FOREGROUND_SERVICE` — запуск активного мониторинга с постоянным уведомлением;
- `FOREGROUND_SERVICE_DATA_SYNC` — тип foreground service для сетевой синхронизации на Android 14+;
- `POST_NOTIFICATIONS` — локальные уведомления на Android 13+;
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — переход в настройки энергосбережения для автопроверки.

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
- публичный Telegram-бот центрального сервиса без требования установить приложение;
- отдельные согласия на отправку обезличенных reports и удалённые проверки;
- автоопределение региона/города через платформенный `LocationManager` + `Geocoder` с ручным fallback;
- автоопределение оператора по active/default data subscription с ручным override;
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
- [Центральный публичный сервис](docs/architecture/central-public-service.md)
- [Cloudflare Public Service](docs/cloudflare-public-service/README.md)
- [Public report contract](docs/architecture/public-report-contract.md)
- [Remote command flow](docs/architecture/remote-command-flow.md)
- [Public data sharing privacy](docs/privacy/public-data-sharing.md)
- [Mobile routing, VPN и Private DNS](docs/network-routing-notes.md)
- [Telegram bot buttons and distributed reports](docs/telegram-bot-controls-design.md)
- [Changelog](CHANGELOG.md)
- [Release v0.7.1 Hotfix](docs/releases/v0.7.1-hotfix.md)
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

## Центральный публичный сервис

Начиная с dev-версии 0.8.14 проект содержит отдельный Cloudflare Worker MVP для центрального публичного сервиса:

```text
Android-приложения пользователей → центральный Cloudflare Worker → D1 → публичный Telegram-бот
```

Это не замена личного Telegram relay.

Начиная с dev-версии 0.8.15 URL центрального сервиса встроен в сборку через `BuildConfig.PUBLIC_SERVICE_BASE_URL`:

- пользователь не вводит и не редактирует URL общего сервиса;
- debug/release URL задаются в Gradle и не являются секретом;
- пользовательский Worker URL личного Telegram relay остаётся отдельной настройкой.

Сценарии разделены:

- **Статус по данным пользователей** — доступен любому пользователю публичного бота, приложение не требуется; бот показывает сохранённые агрегированные данные.
- **Проверить на моём устройстве** — доступно только связанному Telegram-чату и только пока в Android работает активный мониторинг.

В Android согласия независимы и выключены по умолчанию:

- отправлять обезличенные результаты в общий сервис;
- разрешить удалённые проверки из Telegram.

Для public reports пользователь выбирает или подтверждает:

- регион;
- необязательный город;
- мобильного оператора.

Автоопределение местоположения запускается только по нажатию, использует приблизительное location permission, не сохраняет и не отправляет координаты. Оператор определяется по текущей/default SIM мобильных данных; ручной выбор не перезаписывается автоматикой.

Подробно: [docs/architecture/central-public-service.md](docs/architecture/central-public-service.md)

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
| v0.8.14 | Центральный public service MVP |
| v0.8.15 | Fixed public service URL, auto/manual area and operator selection |

## Лицензия

Проект распространяется под лицензией MIT. См. [LICENSE](LICENSE).
