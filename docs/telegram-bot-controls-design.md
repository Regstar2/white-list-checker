# Telegram bot controls: public status and linked-device checks

## Новая модель

В проекте есть две разные Telegram-схемы.

### Личный Telegram relay

```text
Android
  -> user-owned Cloudflare Worker relay
  -> user's Telegram bot
```

Пользователь сам создаёт bot token и Worker. Android хранит только Worker URL, Relay Secret и получателей. Эта схема нужна для личных уведомлений и очереди неотправленных сообщений.

### Центральный публичный сервис

```text
Android apps
  -> central Cloudflare Worker
  -> D1
  -> public Telegram bot
```

Центральный сервис нужен для:

1. публичного агрегированного статуса;
2. приватной удалённой проверки связанного устройства.

Эти сценарии не смешивают права, данные и согласия.

## Публичный бот без приложения

Любой пользователь Telegram может:

- выполнить `/start`;
- выбрать регион;
- выбрать оператора;
- нажать «Статус по данным пользователей»;
- увидеть sample size, свежесть данных, распределение и disclaimer;
- отправить feedback.

Публичная кнопка не запускает проверку на телефонах.

Правильная формулировка:

```text
Статус по данным пользователей
```

Не использовать:

```text
Проверить сеть прямо сейчас
```

если фактически показываются сохранённые серверные данные.

## Пользователь приложения

В Android есть два независимых согласия:

```text
[ ] Отправлять обезличенные результаты в общий сервис
[ ] Разрешить удалённые проверки из Telegram
```

Оба выключены по умолчанию.

URL центрального сервиса встроен в Android через `BuildConfig.PUBLIC_SERVICE_BASE_URL`. Пользователь не вводит его в UI. Это не относится к личному Telegram relay: там пользовательский `Worker URL` остаётся настройкой пользователя.

Возможные комбинации:

- `shareReports=false`, `remoteChecks=false` — приложение локально относительно центрального сервиса.
- `shareReports=true`, `remoteChecks=false` — отчёты идут в public aggregate, remote command запрещены.
- `shareReports=false`, `remoteChecks=true` — связанный chat может получить приватный remote result, public aggregate не меняется.
- `shareReports=true`, `remoteChecks=true` — доступны оба сценария.

## Привязка устройства

Android создаёт одноразовый код:

```text
ABCD-EFGH
```

Пользователь отправляет публичному боту:

```text
/link ABCD-EFGH
```

Worker хранит hash code, проверяет TTL и одноразовость, создаёт `installation_link` между Telegram chat и installation.

Бот не показывает installation ID, device token или другие технические идентификаторы.

## Мои устройства

Для связанного устройства доступны:

```text
[ Последний результат ]
[ Проверить сейчас ]
[ Переименовать ]
[ Отвязать ]
```

В MVP реализован remote command `CHECK_NOW`; переименование можно оставить следующим небольшим этапом, если UI ещё не подключён.

## Удалённая проверка

Remote check разрешён только если:

- link активен;
- installation не revoked;
- `allowRemoteChecks=true`;
- foreground service недавно прислал heartbeat;
- cooldown не нарушен;
- нет другой active command.

Поток:

```text
Telegram user
  -> Проверить сейчас
  -> Worker создаёт CHECK_NOW с TTL
  -> ActiveMonitoringService забирает команду через service-sync
  -> Android выполняет существующую проверку через общий runner
  -> Android отправляет command result
  -> Worker отдаёт приватный ответ в связанный chat
```

Если active monitoring остановлен, бот должен честно ответить, что устройство offline и нужно запустить активный мониторинг.

## Public sharing после remote check

Remote result всегда приватно возвращается связанному chat.

В public aggregate он попадает только если Android отдельно включает `shareReports=true` и создаёт PendingPublicReport из того же локального результата. Worker command-result endpoint сам не добавляет результат в публичную статистику.

## Регион, город и оператор для public status

Публичные reports используют стабильные коды:

- `regionCode`;
- необязательный `cityCode`;
- `operatorCode`.

Android может определить регион и город автоматически только после нажатия пользователя и подтверждения результата. Координаты не отправляются в Worker. Оператор определяется по active/default data subscription или выбирается вручную.

Городская статистика показывается только при отдельном privacy threshold; при малой выборке бот должен использовать региональный уровень или показывать недостаточность данных.

## Сети

- Whitelist check идёт через конкретный cellular `Network`.
- Central Worker API идёт через default network.
- Личный relay идёт через default network.
- `bindProcessToNetwork(...)` не использовать.

## Что не делать

- Не требовать приложение для public `/status`.
- Не запускать телефонную проверку при public status.
- Не разрешать public user командовать чужим устройством.
- Не объединять public sharing consent и remote consent.
- Не хранить `BOT_TOKEN` в Android.
- Не смешивать личный relay и центральный public service.
- Не превращать remote command в универсальное удалённое управление телефоном.
- Не обещать мгновенную проверку при остановленном foreground service.
