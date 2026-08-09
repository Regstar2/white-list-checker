<div align="center">

# WhiteListChecker

Android-приложение для проверки доступности мобильной сети и обнаружения признаков режима белых списков. Проверки выполняются через мобильное подключение, даже когда основной сетью телефона остаётся Wi-Fi.

**Русский** · [English](README_EN.md)

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![AI-assisted development](https://img.shields.io/badge/Development-AI--assisted-8A2BE2)](#использование-ai)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Быстрый старт](#быстрый-старт) ·
[Документация](#документация) ·
[Релизы](../../releases)

</div>

---

## О проекте

WhiteListChecker проверяет набор локальных и внешних сайтов через мобильную сеть, классифицирует результат и сохраняет историю наблюдений. Приложение предназначено для ручной диагностики, периодических проверок и уведомлений при изменении состояния сети.

Начиная с `0.9.0`, контрольные домены разрешаются через управляемый пользователем набор DNS-серверов, привязанных к тому же cellular `Network`. Поэтому системный Android Private DNS не используется в основном маршруте проверки сайтов.

Проект определяет только наблюдаемые сетевые признаки. Он не имеет доступа к внутренним правилам оператора и не может подтвердить наличие белых списков со стопроцентной точностью.

## Статус проекта

Проект находится на стадии **MVP / beta**. Текущая линия разработки — `0.10.x`; значение `versionName` Android-приложения в репозитории — `0.10.4`.

| Область | Статус |
|---|---|
| Ручная проверка мобильной сети | Beta |
| Custom DNS через cellular Network | Beta |
| DNS-сигнал FOREIGN / LOCAL | Beta |
| Фоновая проверка через WorkManager | Beta |
| Активный мониторинг через foreground service | Beta |
| Локальные и личные Telegram-уведомления | Beta |
| Центральный публичный сервис и бот | Beta |
| Удалённая проверка связанного устройства | Экспериментально |

## Возможности

- явный запрос мобильной сети через `ConnectivityManager.requestNetwork(...)`;
- проверка групп сайтов `FOREIGN` и `LOCAL` с редактируемым списком целей;
- отдельный редактируемый список DNS с группами `FOREIGN` и `LOCAL`;
- raw DNS через UDP/53 с TCP/53 fallback, привязанный к cellular `Network`;
- custom DNS resolution контрольных сайтов без системного Android DNS;
- HTTPS target checks через OkHttp с `cellular Network.socketFactory` и штатной TLS/hostname verification;
- DNS-сигнал как второй независимый источник, не способный сам создать `WHITELIST_ON`;
- классификация доступности, DNS-сбоев, частичных проблем и признаков белых списков;
- подтверждение смены состояния последовательными проверками;
- история, статистика и подробный диагностический отчёт;
- локальные Android-уведомления;
- периодические проверки через WorkManager;
- активный мониторинг через foreground service;
- личные Telegram-уведомления через пользовательский Cloudflare Worker relay;
- центральный Cloudflare Worker с публичным Telegram-ботом;
- добровольная отправка обезличенных результатов в агрегированную статистику;
- привязка Telegram-чата к устройству одноразовым кодом;
- удалённая команда проверки, пока активный мониторинг работает на устройстве.

## DNS по умолчанию

Встроенный стартовый набор содержит по две точки в каждой группе:

```text
FOREIGN
Cloudflare   1.1.1.1:53
Google       8.8.8.8:53

LOCAL
Yandex DNS             77.88.8.8:53
Yandex DNS Secondary   77.88.8.1:53
```

DNS можно включать и выключать, добавлять, удалять и сбрасывать к стандартному набору. Хотя бы один DNS должен оставаться включённым, иначе независимая от Android Private DNS проверка невозможна.

Группа DNS используется для диагностики доступности инфраструктуры, но не ограничивает резолвинг: любой доступный enabled DNS может разрешать любой контрольный домен.

## Быстрый старт

1. Установите Android Studio с JDK 17 и Android SDK 35.
2. Скопируйте `local.properties.example` в `local.properties` и укажите `sdk.dir`.
3. Соберите debug APK:

```powershell
.\gradlew.bat assembleDebug
```

4. Установите APK на подключённое устройство:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

5. Откройте приложение и нажмите **«Проверить мобильную сеть»**.

Опубликованные сборки находятся в [GitHub Releases](../../releases) и могут отставать от текущей ветки разработки.

## Требования

- Android 8.0 или новее (`minSdk 26`);
- JDK 17 или новее для сборки;
- Android SDK Platform 35 и Build-Tools 35.x;
- подключение к мобильной сети для основного сценария;
- Git и ADB для локальной разработки и установки;
- Node.js и npm для разработки центрального Cloudflare Worker.

## Установка

Debug APK создаётся по пути:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Установка поверх существующей debug-сборки:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Использование

### Ручная проверка

1. Оставьте мобильные данные включёнными.
2. Wi-Fi можно не отключать: приложение отдельно запрашивает мобильную сеть.
3. При необходимости откройте **Настройки проверки → DNS** и настройте resolvers.
4. Запустите проверку на главном экране.
5. Откройте подробности, если часть целей или DNS недоступна либо результат неоднозначен.

### Android Private DNS

WhiteListChecker выполняет собственное разрешение контрольных доменов через настроенные literal IP DNS-серверов. DNS-запросы и HTTPS target checks идут через один и тот же запрошенный cellular `Network`.

Private DNS Android при этом остаётся системной настройкой телефона, но не участвует в основном resolver path WhiteListChecker. Приложение не изменяет эту настройку и не запрашивает `WRITE_SETTINGS`.

Текущий DNS/53 transport не шифруется. Используйте только доверенные resolvers.

### Фоновая и активная проверка

- WorkManager выполняет приблизительные периодические проверки с минимальным интервалом Android в 15 минут.
- Активный мониторинг использует foreground service и постоянное уведомление.
- Android может ограничить или остановить foreground service, особенно при жёстком энергосбережении.

### Общий сервис и публичный Telegram-бот

Согласия на отправку обезличенных результатов и на удалённые проверки независимы и выключены по умолчанию.

Для удалённой проверки:

1. разрешите удалённые проверки на экране общего сервиса;
2. сохраните настройки;
3. создайте код привязки и отправьте его публичному боту командой `/link <код>`;
4. запустите активный мониторинг;
5. запросите проверку из карточки связанного устройства в боте.

## Архитектура

```text
Android UI
   │
   ▼
ViewModel / Use cases
   │
   ├── Cellular Network
   │      ├── DNS probes (UDP/TCP 53)
   │      ├── CellularDnsResolver
   │      └── OkHttp target checks
   ├── DataStore / Room
   ├── WorkManager / Foreground service
   ├── User-owned Telegram relay Worker
   └── Central public-service Worker
            │
            ├── D1
            └── Public Telegram bot
```

Android-приложение использует Kotlin, Jetpack Compose, Material 3, Coroutines/Flow, DataStore, Room, WorkManager и OkHttp. Центральный сервис находится в `cloudflare/public-service/` и реализован как Cloudflare Worker с D1.

Подробнее о маршрутизации: [docs/network-routing-notes.md](docs/network-routing-notes.md).

## Безопасность

- TLS и hostname verification target checks не отключаются.
- Telegram bot tokens и Worker secrets не должны храниться в Android-коде или Git.
- Device token центрального сервиса хранится на устройстве с использованием Android Keystore-backed encryption.
- Центральный Worker хранит хэш device token, а не исходное значение.
- Не публикуйте `local.properties`, release keystore, токены, пароли и диагностические логи с секретами.

Подробнее: [SECURITY.md](SECURITY.md).

## Приватность

Отправка данных в общий сервис выключена по умолчанию.

При включённой отправке используются выбранные регион, необязательный город, оператор, версия приложения, время проверки, итоговое состояние и агрегированные результаты целей. DNS diagnostics v0.9.0 остаются локальными и не добавляются в публичный контракт автоматически.

Не отправляются координаты, точный адрес, номер телефона, IMEI, IMSI, SIM serial, Wi-Fi SSID/BSSID, содержимое устройства, личные Telegram-сообщения, bot token и Relay Secret.

Подробнее: [docs/privacy/public-data-sharing.md](docs/privacy/public-data-sharing.md).

## Диагностика

Подробный отчёт показывает:

- активную и проверяемую сеть;
- Private DNS active/inactive и hostname, если он доступен;
- использовался ли custom DNS;
- FOREIGN/LOCAL DNS summary;
- результат каждого resolver, latency и typed error;
- Site signal;
- DNS signal;
- итоговое состояние.

### Активный мониторинг показывает `Worker HTTP 404`

Маршрут активного мониторинга должен отвечать не `404`, а `405` на диагностический GET-запрос. Из каталога Worker выполните:

```powershell
cd cloudflare\public-service
npm run verify:production
```

Если проверка сообщает об устаревшей ревизии или отсутствии `service-sync`, разверните текущую версию Worker:

```powershell
npm run deploy
```

После развёртывания `/health` с заголовком `Accept: application/json` должен возвращать JSON с полем `revision` и capability `service-sync`.

### ADB не найден

Добавьте каталог `platform-tools` Android SDK в `PATH` или запускайте `adb.exe` по полному пути.

Дополнительные сценарии: [docs/testing/public-service-manual-test-plan.md](docs/testing/public-service-manual-test-plan.md).

## Разработка

Перед изменениями прочитайте [AGENTS.md](AGENTS.md). Архитектурные и продуктовые документы находятся в `docs/`.

Основные каталоги:

```text
app/                         Android-приложение
cloudflare/public-service/   центральный Worker и публичный бот
docs/                        архитектура, тест-планы и история версий
```

## Использование AI

В разработке проекта AI применяется как вспомогательный инструмент для анализа, подготовки вариантов реализации, документации и тестов.

- все изменения проходят проверку сопровождающим проекта;
- ответственность за принятый код, архитектуру, безопасность и релизы несёт сопровождающий;
- AI не является компонентом WhiteListChecker;
- AI не обрабатывает пользовательский трафик, пароли, токены или конфигурацию серверов во время работы приложения.

## Сборка

Android:

```powershell
.\gradlew.bat assembleDebug
```

Центральный Worker, dry-run без публикации:

```powershell
cd cloudflare\public-service
npm ci
npm run build
```

## Тестирование

Команды Android-проекта:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Команды Worker:

```powershell
cd cloudflare\public-service
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

Проверка production-развёртывания Worker:

```powershell
npm run verify:production
```

## Документация

| Задача | Документ |
|---|---|
| Текущий MVP | [docs/WhiteListChecker - current MVP.md](docs/WhiteListChecker%20-%20current%20MVP.md) |
| Сеть, VPN и Private DNS | [docs/network-routing-notes.md](docs/network-routing-notes.md) |
| Версия 0.9.0 | [docs/versions/v0.9.0.md](docs/versions/v0.9.0.md) |
| Стек | [docs/stack.md](docs/stack.md) |
| Центральный сервис | [docs/architecture/central-public-service.md](docs/architecture/central-public-service.md) |
| Удалённые команды | [docs/architecture/remote-command-flow.md](docs/architecture/remote-command-flow.md) |
| Личный Telegram relay | [docs/cloudflare-worker/README.md](docs/cloudflare-worker/README.md) |
| Развёртывание public service | [docs/cloudflare-public-service/README.md](docs/cloudflare-public-service/README.md) |
| Ручной тест-план | [docs/testing/public-service-manual-test-plan.md](docs/testing/public-service-manual-test-plan.md) |
| История изменений | [CHANGELOG.md](CHANGELOG.md) |
| Правила разработки | [AGENTS.md](AGENTS.md) |

## Ограничения

- Классификация основана на наблюдаемых результатах и может давать ложные срабатывания.
- VPN остаётся отдельным ограничением Android: custom DNS не гарантирует обход произвольного VPN.
- Raw DNS v0.9.0 использует literal IPv4 endpoints и не шифрует DNS/53 трафик.
- WorkManager не гарантирует точное время запуска.
- Foreground service может быть ограничен или остановлен Android.
- Удалённая проверка требует актуального production Worker, сохранённого разрешения и запущенного активного мониторинга.
- Агрегированная статистика зависит от количества свежих отчётов и не является официальной информацией оператора.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
