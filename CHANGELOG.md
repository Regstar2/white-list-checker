# Changelog

## 0.7.0 MVP

### Добавлено

- Разделение UI по экранам.
- Ручная проверка мобильной сети.
- FOREIGN / LOCAL группы сайтов.
- Подтверждение смены состояния.
- Локальные уведомления.
- Telegram через пользовательский Cloudflare Worker relay.
- Несколько Telegram-получателей.
- Очередь Telegram через Room.
- Автопроверка через WorkManager.
- Редактируемый список сайтов.
- Диагностика.
- Состояние `MOBILE_DNS_FAILURE`.

### Безопасность

- `BOT_TOKEN` не хранится в Android.
- Direct fallback к `api.telegram.org` запрещён.
- Telegram работает только через пользовательский Worker relay.

### Ограничения

- MVP/beta.
- Возможны ложные срабатывания.
- WorkManager может запускаться не строго по расписанию.

Подробнее: [docs/releases/v0.7.0-mvp.md](docs/releases/v0.7.0-mvp.md).

---

## 0.7.0

### Добавлено

- Разделение UI по экранам:
  - Главная;
  - Уведомления;
  - Настройки проверки;
  - Автопроверка;
  - Диагностика.
- Telegram через user-owned Cloudflare Worker relay.
- Несколько Telegram-получателей.
- Очередь неотправленных Telegram-сообщений через Room.
- Автопроверка через WorkManager.
- Редактируемый список сайтов.
- Диагностика последней проверки.
- Состояние `MOBILE_DNS_FAILURE`.

### Изменено

- Telegram Bot Token больше не хранится в Android-приложении.
- Telegram-сообщения отправляются через Worker relay.
- Уведомления отправляются только при подтверждённой смене состояния.
- README и документация обновлены под текущую архитектуру.

### Безопасность

- Запрещён direct fallback к `api.telegram.org` из Android.
- `BOT_TOKEN` должен храниться только в Cloudflare Worker secrets.
