<div align="center">

# WhiteListChecker

Android-приложение для проверки доступности мобильной сети и обнаружения признаков режима белых списков. Проверки выполняются через мобильное подключение, даже когда основной сетью телефона остаётся Wi-Fi.

**Русский** · [English](README_EN.md)

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![AI-assisted development](https://img.shields.io/badge/Development-AI--assisted-8A2BE2)](#использование-ai)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Быстрый старт](#быстрый-старт) · [Документация](#документация) · [Релизы](../../releases)

</div>

---

## О проекте

WhiteListChecker проверяет локальные и внешние сайты через мобильную сеть, классифицирует результат и сохраняет историю наблюдений. Приложение предназначено для ручной диагностики, периодических проверок и уведомлений при изменении наблюдаемого состояния сети.

Начиная с `0.9.0`, контрольные домены разрешаются через управляемый пользователем список DNS-серверов, привязанных к тому же cellular `Network`. Поэтому основной маршрут проверки сайтов не использует системный Android Private DNS.

Проект определяет только наблюдаемые сетевые признаки. Он не имеет доступа к внутренним правилам оператора и не может доказать наличие белых списков со стопроцентной точностью.

## Статус проекта

Проект находится на стадии **MVP / beta**. Текущая линия разработки - `0.10.x`; значение Android `versionName` в репозитории - `0.10.4`.

| Область | Статус |
|---|---|
| Ручная проверка мобильной сети | Beta |
| Custom DNS через cellular Network | Beta |
| DNS-сигнал FOREIGN / LOCAL | Beta |
| Фоновая проверка через WorkManager | Beta |
| Активный мониторинг через foreground service | Beta |
| Локальные и личные Telegram-уведомления | Beta |
| Центральный публичный сервис и бот | Beta |
| Удалённая проверка связанного устройства | Экспериментально в разработке |

## Возможности

- явный запрос мобильной сети через `ConnectivityManager.requestNetwork(...)`;
- редактируемые группы сайтов `FOREIGN` и `LOCAL`;
- отдельный редактируемый список DNS с группами `FOREIGN` и `LOCAL`;
- raw DNS через UDP/53 с TCP/53 fallback, привязанный к cellular `Network`;
- custom DNS resolution контрольных сайтов без системного Android DNS;
- HTTPS target checks через OkHttp с `cellular Network.socketFactory` и штатной TLS/hostname verification;
- DNS-доступность как вторичный независимый сигнал, который сам по себе не создаёт `WHITELIST_ON`;
- классификация доступности, DNS-сбоев, частичных проблем и признаков белых списков;
- подтверждение смены состояния последовательными проверками;
- история, статистика и подробный диагностический отчёт;
- локальные Android-уведомления;
- периодические проверки через WorkManager;
- активный мониторинг через foreground service;
- личные Telegram-уведомления через пользовательский Cloudflare Worker relay;
- центральный Cloudflare Worker с публичным Telegram-ботом;
- добровольная отправка обезличенной агрегированной статистики;
- привязка Telegram-чата к устройству одноразовым кодом;
- удалённые команды проверки, пока активный мониторинг работает на устройстве.

## Скриншоты

| Главный экран | Статистика | Настройки проверок |
|---|---|---|
| <img src="docs/assets/screenshots/mainscreen.jpg" width="240" alt="Главный экран WhiteListChecker"> | <img src="docs/assets/screenshots/statistics.jpg" width="240" alt="Экран статистики WhiteListChecker"> | <img src="docs/assets/screenshots/checklist.jpg" width="240" alt="Экран настроек проверок WhiteListChecker"> |

## Быстрый старт

1. Установите Android Studio с JDK 17 и Android SDK 35.
2. Скопируйте `local.properties.example` в `local.properties` и укажите `sdk.dir`.
3. Соберите debug APK:

```powershell
.\gradlew.bat assembleDebug
```

4. Установите APK на подключённое Android-устройство:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

5. Откройте приложение и нажмите **Проверить мобильную сеть**.

Опубликованные сборки находятся в [GitHub Releases](../../releases) и могут отставать от текущей ветки разработки.

## Требования

- Android 8.0 или новее (`minSdk 26`);
- JDK 17 или новее для сборки;
- Android SDK Platform 35 и Build-Tools 35.x;
- активное мобильное подключение для основного сценария;
- Git и ADB для локальной разработки и установки;
- Node.js и npm для разработки центрального Cloudflare Worker.

## Использование

### Ручная проверка

1. Оставьте мобильные данные включёнными.
2. Wi-Fi можно не отключать: приложение отдельно запрашивает мобильную сеть.
3. При необходимости откройте **Настройки проверок -> DNS** и настройте resolver.
4. Запустите проверку на главном экране.
5. Откройте подробный отчёт, если результаты сайтов или DNS неоднозначны.

### Android Private DNS

WhiteListChecker разрешает контрольные домены через настроенные DNS-серверы с literal IP. DNS probes, DNS resolution и target HTTPS traffic используют один и тот же запрошенный cellular `Network`.

Android Private DNS остаётся системной настройкой телефона, но не участвует в основном resolver path WhiteListChecker. Приложение не меняет Private DNS и не запрашивает `WRITE_SETTINGS`.

Текущий DNS/53 transport не шифруется. Используйте только доверенные resolver.

### Фоновая и активная проверка

- WorkManager выполняет приблизительные периодические проверки с минимальным интервалом Android в 15 минут.
- Активный мониторинг использует foreground service и постоянное уведомление.
- Android может ограничить или остановить foreground service, особенно при жёстком энергосбережении.

### Общий сервис и публичный Telegram-бот

Согласия на отправку обезличенных отчётов и на удалённые проверки независимы и выключены по умолчанию.

Для удалённой проверки:

1. разрешите удалённые проверки на экране общего сервиса;
2. сохраните настройки;
3. создайте код привязки и отправьте его публичному боту командой `/link <код>`;
4. запустите активный мониторинг;
5. запросите проверку из карточки связанного устройства в боте.

## Конфигурация

### DNS по умолчанию

Встроенный стартовый набор содержит минимум два resolver в каждой группе:

```text
FOREIGN
Cloudflare   1.1.1.1:53
Google       8.8.8.8:53

LOCAL
Yandex DNS             77.88.8.8:53
Yandex DNS Secondary   77.88.8.1:53
```

DNS можно включать, выключать, добавлять, удалять и сбрасывать к стандартному набору. Хотя бы один resolver должен оставаться включённым, иначе независимая от Android Private DNS проверка невозможна.

Группа DNS используется только для диагностической классификации. Любой доступный enabled resolver может разрешать любой контрольный домен.

## Архитектура

```text
Android UI
   |
   v
ViewModel / Use cases
   |
   +-- Cellular Network
   |      +-- DNS probes (UDP/TCP 53)
   |      +-- CellularDnsResolver
   |      +-- OkHttp target checks
   +-- DataStore / Room
   +-- WorkManager / Foreground service
   +-- User-owned Telegram relay Worker
   +-- Central public-service Worker
            |
            +-- D1
            +-- Public Telegram bot
```

Android-приложение использует Kotlin, Jetpack Compose, Material 3, Coroutines/Flow, DataStore, Room, WorkManager и OkHttp. Центральный сервис находится в `cloudflare/public-service/` и реализован как Cloudflare Worker с D1.

Подробнее о маршрутизации: [docs/network-routing-notes.md](docs/network-routing-notes.md).

## Безопасность

- TLS certificate и hostname verification target checks не отключаются.
- Telegram bot tokens и Worker secrets не должны храниться в Android-коде или Git.
- Device token центрального сервиса хранится на устройстве с использованием Android Keystore-backed encryption.
- Центральный Worker хранит hash device token, а не исходное значение.
- Не публикуйте `local.properties`, release keystore, токены, пароли и логи с секретами.

Подробнее: [SECURITY.md](SECURITY.md).

## Приватность

Отправка данных в общий сервис выключена по умолчанию.

При включённой отправке сервис получает выбранный регион, необязательный город, мобильного оператора, версию приложения, время проверки, итоговое состояние и агрегированные результаты целей. DNS diagnostics, добавленные в v0.9.0, остаются локальными и не добавляются в публичный контракт автоматически.

Не отправляются координаты, точный адрес, номер телефона, IMEI, IMSI, SIM serial, Wi-Fi SSID/BSSID, содержимое устройства, личные Telegram-сообщения, bot token и Relay Secret.

Подробнее: [docs/privacy/public-data-sharing.md](docs/privacy/public-data-sharing.md).

## Диагностика

Подробный отчёт включает:

- активную и проверяемую сеть;
- Private DNS active/inactive и hostname, если он доступен;
- использовался ли custom DNS;
- FOREIGN/LOCAL DNS summaries;
- latency и typed errors каждого resolver;
- Site signal;
- DNS signal;
- итоговое состояние.

### Активный мониторинг показывает `Worker HTTP 404`

Маршрут активного мониторинга должен отвечать не `404`, а `405` на диагностический GET-запрос. Выполните:

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

## Разработка

Перед изменениями прочитайте [AGENTS.md](AGENTS.md). Архитектурные и продуктовые документы находятся в `docs/`.

```text
app/                         Android-приложение
cloudflare/public-service/   центральный Worker и публичный бот
docs/                        архитектура, тест-планы и документы версий
```

## Использование AI

В разработке проекта AI применяется как вспомогательный инструмент для анализа, вариантов реализации, документации и тестов.

- каждое изменение проверяет сопровождающий проекта;
- сопровождающий отвечает за принятый код, архитектуру, безопасность и релизы;
- AI не является компонентом продукта WhiteListChecker;
- AI не обрабатывает пользовательский трафик, пароли, токены или серверную конфигурацию во время работы приложения.

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

Dry-run центрального Worker:

```powershell
cd cloudflare\public-service
npm ci
npm run build
```

## Тестирование

Android:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Worker:

```powershell
cd cloudflare\public-service
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

## Документация

| Задача | Документ |
|---|---|
| Текущий MVP | [docs/WhiteListChecker - current MVP.md](docs/WhiteListChecker%20-%20current%20MVP.md) |
| Network, VPN и Private DNS | [docs/network-routing-notes.md](docs/network-routing-notes.md) |
| Версия 0.9.0 | [docs/versions/v0.9.0.md](docs/versions/v0.9.0.md) |
| Технологический стек | [docs/stack.md](docs/stack.md) |
| Центральный сервис | [docs/architecture/central-public-service.md](docs/architecture/central-public-service.md) |
| Удалённые команды | [docs/architecture/remote-command-flow.md](docs/architecture/remote-command-flow.md) |
| История изменений | [CHANGELOG.md](CHANGELOG.md) |
| Правила разработки | [AGENTS.md](AGENTS.md) |

## Ограничения

- Классификация основана на наблюдаемых результатах и может давать ложные срабатывания.
- VPN остаётся отдельным ограничением Android; custom DNS не гарантирует обход произвольного VPN.
- Raw DNS v0.9.0 принимает literal IPv4 resolver endpoints и отправляет нешифрованный DNS/53 traffic.
- WorkManager не гарантирует точное время запуска.
- Android может ограничить или остановить foreground service.
- Удалённые проверки требуют актуальный production Worker, сохранённое согласие и активный мониторинг на устройстве.
- Агрегированная статистика зависит от свежих отчётов и не является официальной информацией операторов.

## Лицензия

Проект распространяется под лицензией [MIT](LICENSE).
