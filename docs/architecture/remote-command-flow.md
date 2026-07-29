# Remote Command Flow

## Назначение

Удалённая команда в MVP умеет только одно действие:

```text
CHECK_NOW
```

Это не универсальное удалённое управление устройством.

## Preconditions

Worker создаёт команду только если:

- `installation_links` содержит active link между Telegram chat и installation;
- installation не revoked;
- `allowRemoteChecks=true`;
- foreground service недавно прислал heartbeat;
- cooldown не нарушен;
- нет другой active command для installation;
- устройство не offline по `DEVICE_ONLINE_TIMEOUT_SECONDS`.

Если service offline, бот отвечает, что нужно запустить «Активный мониторинг» в приложении.

## Lifecycle

```text
PENDING
  -> CLAIMED
  -> COMPLETED | FAILED

PENDING
  -> EXPIRED

CLAIMED
  -> FAILED | EXPIRED
```

Запрещённые переходы отклоняются. `service-sync` атомарно выдаёт максимум одну команду и переводит её в `CLAIMED`.

## Android execution

Foreground service выполняет polling через `POST /api/v1/installations/me/service-sync`.

Если пришла команда:

1. Android проверяет локальный флаг `allowRemoteChecks`.
2. Проверяет TTL команды.
3. Запускает общий `CheckAndNotifyUseCase` с trigger `REMOTE_TELEGRAM`.
4. `CheckExecutionCoordinator` не допускает параллельную проверку.
5. Результат сохраняется в локальной истории.
6. Android отправляет command result в Worker.

Если проверка уже идёт, Android возвращает `BUSY`.

## Telegram result

Worker отправляет или редактирует одно сообщение в связанном Telegram-чате.

Повторная отправка одного command result идемпотентна:

- не создаёт второе Telegram-сообщение;
- не создаёт второй public report;
- возвращает success для уже завершённого результата.

## Public sharing

Remote result приватен. Он попадает в публичную статистику только если Android после этой же локальной проверки создаёт PendingPublicReport при `shareReports=true`.

Worker endpoint command result не включает результат в public aggregate сам по себе.

## Ограничения

- Remote command работает только при реально запущенном foreground service.
- HTTP polling добавляет задержку.
- Android может остановить foreground service.
- Команда имеет короткий TTL и не хранится как бессрочная очередь.
