<div align="center">

# WhiteListChecker

### Проверка мобильной сети и обнаружение признаков режима белых списков

Android-приложение выполняет проверки именно через мобильное подключение, отслеживает изменение состояния и может отправлять уведомления локально или через Telegram.

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](#технологии)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-3DDC84?logo=android&logoColor=white)](#требования)
[![Status](https://img.shields.io/badge/status-MVP%20%2F%20beta-orange)](#статус-проекта)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[Возможности](#возможности) · [Как это работает](#как-это-работает) · [Сборка](#сборка) · [Документация](#документация)

</div>

---

## Статус проекта

| Параметр | Состояние |
|---|---|
| Ручная проверка | Готово |
| Фоновая автопроверка | Готово |
| Активный foreground-мониторинг | Готово |
| Локальные уведомления | Готово |
| Личный Telegram relay | Готово |
| Центральный публичный сервис | MVP |
| Удалённая проверка из Telegram | Beta |
| Точность на разных прошивках | Требует накопления данных |

> [!IMPORTANT]
> WhiteListChecker определяет **признаки сетевого режима**, а не внутренние правила оператора. Возможны ложные срабатывания, особенности прошивок и временные сетевые ошибки.

## Что проверяет приложение

Приложение запрашивает мобильную сеть через `ConnectivityManager` и выполняет соединения через полученный объект `Network`. Поэтому проверка не переключается на Wi-Fi, даже когда Wi-Fi является активным подключением телефона.

```text
Android
   │ requestNetwork(TRANSPORT_CELLULAR)
   ▼
Мобильная сеть
   │
   ├── FOREIGN sites
   ├── LOCAL sites
   └── DNS / TCP / HTTPS observations
            │
            ▼
      Классификация состояния
```

## Возможности

<table>
<tr>
<td width="50%" valign="top">

### Проверки

- ручной запуск;
- WorkManager-автопроверка;
- foreground service для активного мониторинга;
- редактируемый список сайтов;
- подтверждение смены двумя проверками подряд;
- подробный диагностический отчёт.

</td>
<td width="50%" valign="top">

### Уведомления

- локальные Android-уведомления;
- уведомления только при смене состояния;
- Telegram через личный Cloudflare Worker;
- очередь неотправленных сообщений в Room;
- несколько Telegram-получателей.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### Общие данные

- добровольная отправка обезличенных результатов;
- публичный Telegram-бот с агрегированными данными;
- регион, город и оператор с ручным подтверждением;
- отдельное согласие на каждую функцию.

</td>
<td width="50%" valign="top">

### Удалённая проверка

- привязка Telegram-чата к устройству;
- команда проверки во время активного мониторинга;
- доступ только связанному чату;
- функция выключена без явного согласия пользователя.

</td>
</tr>
</table>

## Классификация

| Результат | Что означает |
|---|---|
| Белые списки не обнаружены | FOREIGN и LOCAL ресурсы доступны в ожидаемом объёме |
| Похоже на белые списки | LOCAL доступен заметно лучше FOREIGN |
| Мобильного интернета нет | Проверки не подтверждают рабочий доступ |
| Проблема DNS | Имена не разрешаются через мобильную сеть |
| Частичная проблема сети | Доступность ресурсов нестабильна или неоднородна |
| Мобильная сеть недоступна | Android не предоставил подходящую cellular-сеть |

## Как это работает

### Локальный сценарий

```text
Проверка мобильной сети
        │
        ▼
Классификатор состояния
        │
        ├── UI
        ├── локальное уведомление
        └── история / диагностика
```

### Личный Telegram relay

```text
Android → Cloudflare Worker пользователя → Telegram Bot API
```

`BOT_TOKEN` хранится только в Worker secrets и не попадает в Android-приложение.

### Центральный публичный сервис

```text
Android-устройства пользователей
             │ обезличенные reports
             ▼
     Центральный Cloudflare Worker
             │
             ▼
            D1
             │
             ▼
    Публичный Telegram-бот
```

Личный relay и центральный сервис — независимые сценарии. Центральный сервис не заменяет пользовательского Telegram-бота.

## Приватность и безопасность

По умолчанию отключены:

- отправка обезличенных результатов в общий сервис;
- удалённые проверки из Telegram;
- доступ к приблизительному местоположению.

Определение региона запускается только по действию пользователя. Координаты не должны сохраняться или отправляться как часть public report.

Никогда не публикуйте:

```text
BOT_TOKEN
RELAY_SECRET
local.properties
release keystore
реальные логи с секретами
```

Подробнее: [SECURITY.md](SECURITY.md) и [описание приватности](docs/privacy/public-data-sharing.md).

## Технологии

| Слой | Стек |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Архитектура | MVVM, UseCase |
| Асинхронность | Coroutines, Flow |
| Настройки | DataStore Preferences |
| Очередь сообщений | Room |
| Фоновые задачи | WorkManager, Foreground Service |
| Сеть | Android `Network.openConnection()`, OkHttp |
| Центральный сервис | Cloudflare Workers, D1 |

`minSdk 26` · `compileSdk 35` · `targetSdk 35`

## Требования

- JDK 17 или новее;
- Android SDK;
- Android Platform 35;
- Build-Tools 35.x;
- Git.

Создайте локальный файл настроек:

```powershell
Copy-Item local.properties.example local.properties
```

Укажите в нём путь к Android SDK. `local.properties` не должен попадать в Git.

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Установка на подключённое устройство:

```powershell
adb devices
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

## Разрешения Android

| Разрешение | Назначение |
|---|---|
| `INTERNET` | сетевые проверки |
| `ACCESS_NETWORK_STATE` | чтение состояния подключений |
| `CHANGE_NETWORK_STATE` | явный запрос мобильной сети |
| `ACCESS_COARSE_LOCATION` | приблизительный регион по явному запросу |
| `FOREGROUND_SERVICE_DATA_SYNC` | активный сетевой мониторинг |
| `POST_NOTIFICATIONS` | локальные уведомления |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | переход к системным настройкам энергосбережения |

## Документация

- [Текущий MVP](docs/WhiteListChecker%20-%20current%20MVP.md)
- [Стек технологий](docs/stack.md)
- [Центральный публичный сервис](docs/architecture/central-public-service.md)
- [Контракт public reports](docs/architecture/public-report-contract.md)
- [Удалённые команды](docs/architecture/remote-command-flow.md)
- [Личный Cloudflare Worker relay](docs/cloudflare-worker/README.md)
- [Маршрутизация, VPN и Private DNS](docs/network-routing-notes.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

---

<div align="center">

WhiteListChecker распространяется по лицензии [MIT](LICENSE).

</div>
