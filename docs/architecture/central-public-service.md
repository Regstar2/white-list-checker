# Центральный публичный сервис

## Назначение

Центральный сервис отделён от личного Telegram relay пользователя.

Он даёт два независимых сценария:

1. Публичный Telegram-бот показывает агрегированный статус по сохранённым обезличенным отчётам пользователей.
2. Связанный Telegram-чат может запросить приватную проверку своего устройства, пока в Android работает активный мониторинг.

Публичная кнопка «Статус по данным пользователей» не запускает проверку на телефонах.

## Компоненты

```text
Android app
  -> PublicServiceClient
  -> central Cloudflare Worker
  -> D1
  -> public Telegram bot webhook
```

Личный relay остаётся отдельным:

```text
Android app
  -> user-owned Cloudflare Worker relay
  -> user's Telegram bot
```

Центральный сервис не использует `RELAY_SECRET` личного relay и не получает `BOT_TOKEN` пользователя.

## Cloudflare Worker

Код MVP находится в `cloudflare/public-service/`.

Основные модули:

- `src/http/apiRoutes.ts` — JSON API Android-клиента.
- `src/http/telegramWebhook.ts` — webhook публичного Telegram-бота.
- `src/domain/publicStatusAggregator.ts` — чистые правила публичной агрегации.
- `src/repositories/d1PublicServiceRepository.ts` — D1-запросы и lifecycle команд.
- `src/telegram/publicBot.ts` — команды и кнопки публичного бота.
- `src/telegram/telegramClient.ts` — typed Telegram Bot API client.
- `migrations/0001_initial.sql` — таблицы и индексы D1.
- `migrations/0002_area_city_operator_sources.sql` — город и источники выбора региона/оператора.

## URL центрального сервиса

URL общего сервиса не является пользовательской настройкой.

Android получает адрес из одного источника:

```text
BuildConfig.PUBLIC_SERVICE_BASE_URL
```

Debug и release сборки могут иметь разные значения в Gradle. URL не хранится в DataStore, не вводится пользователем и не смешивается с личным `Worker URL` Telegram relay.

## Согласия Android

В Android есть отдельные настройки:

- `shareReports` — отправлять успешные обезличенные отчёты в общий сервис.
- `allowRemoteChecks` — разрешить связанному Telegram-чату запрашивать новую проверку.

Оба флага выключены по умолчанию. Включение одного не включает второй.

Перед включением `shareReports` Android проверяет, что пользователь подтвердил регион и выбрал или автоматически определил оператора. Город необязателен.

## Регион, город и оператор

Регион и город хранятся отдельно:

- `regionCode` — обязательный стабильный код для public reports;
- `cityCode` — необязательный стабильный код города;
- `customCityName` — безопасно очищенное ручное название без стабильного city-level aggregation;
- `areaSource` — `AUTOMATIC_LOCATION` или `MANUAL_SELECTION`.

Автоматическое определение местоположения запускается только по нажатию пользователя. Android запрашивает только `ACCESS_COARSE_LOCATION`, получает одноразовое приблизительное местоположение через платформенный `LocationManager`, делает reverse geocoding через `Geocoder`, нормализует результат через каталог и затем требует подтверждения пользователя.

Координаты, точный адрес, улица, почтовый индекс и raw geocoder address не сохраняются и не отправляются.

Оператор определяется отдельно:

- сначала по active data subscription, если API доступен;
- затем по default data subscription;
- затем через обычный `TelephonyManager` как fallback;
- `networkOperator` MCC+MNC имеет приоритет над SIM operator;
- ручной выбор не перезаписывается автоматикой.

`READ_PHONE_STATE` не требуется для текущей реализации; номер телефона, IMEI, IMSI и SIM serial не читаются.

## Регистрация установки

Установка регистрируется лениво, когда пользователь включает общую отправку, удалённые проверки или создаёт код привязки.

Worker возвращает:

- `installationId`;
- одноразово показываемый `deviceToken`.

Android хранит `deviceToken` в `SecureDeviceTokenStore` через Android Keystore. D1 хранит только HMAC/SHA-256 token hash с server-side pepper.

## Public Aggregate Flow

```text
Пользователь бота
  -> /status или кнопка «Статус по данным пользователей»
  -> выбор regionCode/operatorCode
  -> Worker выбирает свежие reports
  -> один последний report на installation
  -> PublicStatusAggregator
  -> Telegram message с sample size, freshness и disclaimer
```

Агрегатор учитывает только независимые установки. Много отчётов одного телефона дают один голос в выбранном окне.

Для city-level status применяется отдельный threshold `MIN_UNIQUE_INSTALLATIONS_FOR_CITY`. Если по городу выборки мало, бот должен честно показывать недостаточность данных и fallback на региональную статистику, а не раскрывать одиночный report.

## Remote Linked-Device Flow

```text
Android
  -> создать одноразовый /link код
Telegram
  -> /link ABCD-EFGH
  -> active installation_link
  -> «Мои устройства»
  -> «Проверить сейчас»
Worker
  -> проверка link + allowRemoteChecks + online heartbeat + cooldown
  -> command CHECK_NOW с TTL
Foreground service
  -> service-sync через default network
  -> получает команду
  -> общий CheckAndNotifyUseCase / CheckExecutionCoordinator
  -> cellular Network.openConnection для самой проверки
  -> command result назад в Worker
Worker
  -> приватный Telegram-result в связанный chat
```

Удалённый результат попадает в публичную статистику только через обычную Android public report queue и только если `shareReports=true`.

## Сетевое разделение

- Whitelist-проверка использует конкретный cellular `Network` и `network.openConnection(...)`.
- Центральный Worker API использует default network через отдельный `PublicServiceClient`.
- Личный Telegram relay также использует default network.
- `bindProcessToNetwork(...)` не используется.

## Ограничения

Активный мониторинг — foreground service типа `dataSync`. Он не является круглосуточным сервером: Android 15+ может ограничивать длительность таких сервисов, производители могут применять дополнительные ограничения энергосбережения. При остановленном service remote command не создаёт ложного обещания мгновенной проверки.
