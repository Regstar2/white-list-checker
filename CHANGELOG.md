# Changelog

## 0.8.16 - migrate existing relay Worker to public service

### Added

- Migrated the central public service configuration to the existing historical Worker `whitelist-monitor-tg-relay`.
- Updated Android `BuildConfig.PUBLIC_SERVICE_BASE_URL` for debug and release to `https://whitelist-monitor-tg-relay.regstar2.workers.dev`.
- Added the production-ready inline main menu for the central public Telegram bot.
- Added status-screen buttons for refresh, region/operator selection and return to the main menu.
- Added device-list and device-detail button flows with explicit `linkId` callbacks.
- Added two-step unlink confirmation before calling `revokeLinkFromTelegram`.
- Added Telegram keyboard and callback-routing tests for menu, status, region/operator returns, devices, unlink confirmation, callback length and alias escaping.
- Added `npm run deploy` and `npm run migrations:remote:list` scripts for manual production operations while keeping `npm run build` as dry-run.
- Added `docs/versions/v0.8.16.md`.

### Changed

- Removed the duplicate main-menu device-check button; users now go through "Мои устройства".
- Removed the non-working "Последний результат" device button.
- Public bot callback routing now handles `v1:menu`, `v1:help`, `v1:status-refresh`, device details, check, unlink request/confirm/cancel, about and feedback.
- New public bot user-facing texts are kept in `publicBotFormatter.ts`, with escaped external values.
- Documented that `whitelist-monitor-tg-relay` is now the central service, while user-owned personal Telegram relay Workers remain supported.
- Documented that new buttons appear only after production deployment and old Telegram messages need `/start` for a fresh keyboard.

### Security

- D1 schema, public API, user-owned Telegram relay support, secrets and webhook secret validation were not changed.
- The checked-in D1 `database_id` remains a placeholder and must be replaced manually before production deployment.
- Production secrets remain out of the repository.
- Production deployment was not performed.

## 0.8.15 — Fixed public service URL and area/operator selection

### Added

- Добавлен единый фиксированный `BuildConfig.PUBLIC_SERVICE_BASE_URL` для центрального общего сервиса.
- Добавлено автоопределение региона и города через платформенный `LocationManager` + `Geocoder` после явного действия пользователя.
- Добавлен ручной searchable fallback выбора региона и города; город остаётся необязательным.
- Добавлено автоопределение оператора по active/default data subscription через `TelephonyManager`.
- Добавлен ручной searchable fallback выбора оператора и режим ручного override.
- Public report DTO расширен полями `cityCode`, `customCityName`, `areaSource`, `operatorSource`.
- Worker получил catalog endpoints и отдельный privacy threshold для city-level aggregation.

### Changed

- С экрана «Общий сервис и Telegram-бот» удалено редактируемое поле URL общего сервиса.
- Большие наборы chips региона и оператора заменены компактными строками настройки и searchable dialogs.
- Старые `regionCode` и `operatorCode` мигрируются как ручной подтверждённый выбор.
- Старое поле `public_service_base_url` больше не используется и очищается при сохранении настроек центрального сервиса.
- Версия приложения поднята до `0.8.15` / `versionCode 23`.

### Security

- Координаты, точный адрес, номер телефона, IMSI, IMEI и SIM serial не сохраняются и не отправляются.
- `READ_PHONE_STATE` не добавлялся; номер телефона и идентификаторы SIM не читаются.
- Личный Telegram relay и его пользовательский Worker URL сохранены отдельно от центрального сервиса.

## 0.8.14 — Central public Telegram service MVP

### Added

- Добавлен отдельный Cloudflare Worker `cloudflare/public-service/` для центрального публичного Telegram-сервиса.
- Добавлены D1-миграции для installations, reports, link codes, Telegram users, linked devices, remote commands и command results.
- Добавлен public aggregate flow: бот показывает «Статус по данным пользователей» без требования установить Android-приложение.
- Добавлен `PublicStatusAggregator`: primary/fallback window, minimum sample, consensus threshold и правило «один последний report на installation».
- Добавлен Android `PublicServiceClient`, lazy registration, защищённое хранение device token через Android Keystore и отдельная очередь `pending_public_reports`.
- Добавлен экран «Общий сервис» с независимыми согласиями `shareReports` и `allowRemoteChecks`.
- Добавлена привязка Telegram-чата к устройству через одноразовый `/link` code.
- Добавлен polling central Worker из `ActiveMonitoringService` и remote command `CHECK_NOW`.
- Добавлен trigger истории `REMOTE_TELEGRAM`.

### Changed

- Быстрые действия на главном экране получили пункт «Общий сервис».
- Версия приложения поднята до `0.8.14` / `versionCode 22`.

### Security

- Личный Telegram relay сохранён отдельно и не смешивается с центральным сервисом.
- `BOT_TOKEN` пользовательского relay по-прежнему не хранится в Android.
- Device token центрального сервиса не хранится в D1 в сыром виде.
- Public sharing и remote checks выключены по умолчанию и включаются разными согласиями.

### Notes

- Production deploy Worker и настройка реального Telegram webhook не выполнялись автоматически.
- Remote checks работают только пока Android foreground service реально активен.
- Public aggregate зависит от числа пользователей и не является официальным статусом оператора.

## 0.8.13 — Active monitoring and notification policies

### Added

- Добавлены политики уведомлений для автопроверки: `NONE`, `EVERY_ATTEMPT`, `STATE_CHANGE_ONLY`.
- Добавлено отдельное хранение последней попытки и последнего валидного статуса `WHITELIST_ON/OFF`.
- Добавлен `CheckExecutionCoordinator`, запрещающий параллельные проверки из UI, WorkManager, foreground service и Telegram-команд.
- Добавлен опциональный foreground service `ActiveMonitoringService` с типом `dataSync`, постоянным тихим уведомлением и действиями «Проверить сейчас» / «Остановить».
- Добавлена секция «Активный мониторинг» на существующий экран автопроверки.
- Добавлены команды Telegram-бота `/status`, `/check`, `/help`, работающие только пока активный мониторинг запущен и команды разрешены.
- Добавлено уведомление о восстановлении доступности проверки для активного мониторинга.

### Changed

- WorkManager сохраняет результаты и историю как раньше, но уведомления теперь выбираются политикой.
- При активном мониторинге периодическая WorkManager-задача временно отменяется и восстанавливается после остановки сервиса, если была включена до запуска.
- Telegram polling идёт через пользовательский Cloudflare Worker relay; `BOT_TOKEN` по-прежнему не хранится в Android.
- Шаблон Cloudflare Worker теперь принимает `timeout` для `getUpdates`, чтобы поддерживать server-side long polling.

### Notes

- WorkManager остаётся приблизительным механизмом: Android может сдвигать запуск.
- Активный мониторинг не гарантирует круглосуточную работу; Android может остановить foreground service, особенно на Android 15+ для `dataSync`.
- Telegram-команды используют сохранённый список enabled recipients и точное сравнение `chatId`.

## 0.8.12 — Home screen UX polish

### Added

- Добавлены иконки состояния последнего результата и быстрых действий.
- В карточку последнего результата добавлена кнопка «Подробнее» с переходом в диагностику.
- Добавлена UI-модель для отображения последнего результата на главном экране.

### Changed

- Главный экран стал компактнее: короткое описание, кнопка проверки стандартной высоты, меньше визуального шума.
- Карточка последнего результата теперь показывает главный вывод, локальные и внешние проверки, относительное время и понятный маршрут проверки.
- Сырой `Mobile` больше не выводится на главном экране; маршрут отображается как «Через мобильную сеть при включённом Wi-Fi».
- Быстрые действия перестроены в компактную сетку: статистика/проверки, уведомления/автопроверка, диагностика на всю ширину.
- Пользовательские строки главного экрана вынесены в ресурсы.

---

## 0.8.11 — Selectable statistics periods

### Added

- В графике БС добавлен выбор конкретного дня, недели, месяца и года.
- Добавлена навигация по соседним периодам стрелками назад/вперёд.
- Добавлена кнопка «Выбрать» для выбора даты, по которой определяется нужный день, неделя, месяц или год.
- Добавлена кнопка «Текущий» для возврата к текущему периоду.

### Changed

- «Неделя» теперь строится как календарная неделя с понедельника по воскресенье.
- «Месяц» теперь строится как полный выбранный календарный месяц.
- «Год» строится как полный выбранный календарный год по месяцам.

---

## 0.8.10 — Natural statistics periods

### Changed

- Переключатели графика БС заменены на понятные периоды: день, неделя, месяц, год.
- Неделя отображается по дням за последние 7 дней.
- Месяц отображается по дням за последние 30 дней.
- Год отображается по месяцам за последние 12 месяцев.

---

## 0.8.9 — Statistics navigation polish

### Changed

- Домашняя плашка «Статистика БС» убрана с главного экрана.
- Переход в статистику перенесён в быстрые действия одной кнопкой «Статистика».
- В статистике оставлен один бинарный график БС с переключателями периода.
- Системная кнопка/жест «назад» на внутренних экранах возвращает на главный экран.

---

## 0.8.8 — Binary whitelist timeline

### Added

- Добавлена лёгкая Room-таблица бинарных timeline-сэмплов `whitelist_timeline_samples`.
- Экран «Статистика» теперь показывает график `БС были / БС не было` по часам, дням, неделям и месяцам.
- Существующая история проверок мигрируется в новую timeline-таблицу.

### Changed

- Статистика больше не фокусируется на доступности отдельных целей.
- Домашняя статистическая карточка переработана под бинарный статус БС.
- С главного экрана убраны плашки «Маршрут проверки» и «Краткий статус».
- Статус сохранения проверки теперь учитывает обновление timeline-графика.

### Notes

- VPN/DNS: приложение проверяет через cellular `Network`, но не обещает гарантированный обход VPN, если VPN не разрешает bypass на уровне Android.

---

## 0.8.7 — Stabilization, richer defaults and chart polish

### Added

- Расширены встроенные FOREIGN/LOCAL цели до 8 + 8.
- Добавлен статус записи истории и статистических агрегатов после проверки.
- Добавлен линейный дневной график доступности целей.
- Добавлен технический график успешности target-проверок по дням.
- Добавлена документация по ограничениям VPN/Private DNS и будущим кнопкам Telegram-бота.

### Changed

- Обновлена цветовая тема и базовые карточки UI.
- Домашняя статистика теперь показывает доступные цели как `N/M`.
- История проверок пишет `networkType = Mobile`, потому что проверка фактически идёт через cellular `Network`.

### Fixed

- Исправлена статистическая путаница, когда активная сеть `Wi-Fi` могла попасть в агрегаты как сеть проверки.

---

## 0.8.6 — Whitelist-First Statistics UI

### Added

- Экран «Статистика» ориентирован на белые списки и доступность целей.
- Блок текущего состояния целей, последних изменений и стабильных целей.
- Компактная сводка белых списков на главном экране.
- Сворачиваемый блок «Технические данные проверок» и переход в диагностику.

### Changed

- Подробная техническая статистика проверок убрана с основного экрана статистики.
- Загрузка последних событий доступности для списка изменений.

### Fixed

- Устранено визуальное смешение метрик проверок и статистики белых списков на главном экране.

### Known Issues

- Worker, Telegram Bot и multi-device reports пока не реализованы.

---

## 0.8.5 — Statistics UI Polish & Semantics Fix

### Changed

- Разделены формулировки технического статуса проверки, детекции БС и доступности целей.
- Карточка статистики на главном экране и экран «Статистика» упрощены и переведены на единые подписи.
- Графики показывают проценты с символом `%`; при однородных данных — сообщение вместо вводящих в заблуждение столбиков.
- Список целей на экране статистики сворачивается (топ-5 + «Показать все»).
- Подписи endpoint — короткий host без query, без смеси RU/EN «targets/Endpoints».

### Fixed

- «Серия ошибок» учитывает только полные ошибки проверки, не частичные успехи.
- «Статус: успешно» у последней проверки заменён на «проверка завершена» (не путается с детекцией БС).
- Топ нестабильных целей скрывается, если все имеют одинаковый score.

---

## 0.8.4 — Whitelist Availability Statistics

### Added

- Добавлена локальная статистика появления и исчезновения доступности targets (белые списки).
- Добавлены события изменения состояния target и агрегаты summary/daily/target.
- Добавлен rebuild статистики доступности из истории проверок.
- Добавлен UI-блок «Статистика белых списков» и простые графики на экране «Статистика».
- Добавлены unit-тесты детектора переходов состояния.

### Changed

- Разделена техническая статистика проверок и статистика доступности whitelist.
- Rebuild в диагностике пересчитывает обе статистики.

### Fixed

- Ошибки проверки (DNS, timeout, connection) не считаются выключением whitelist.

### Known Issues

- Worker и экспорт статистики пока не реализованы.

---

## 0.8.3 — Statistics Reliability & Diagnostics

### Added

- Добавлена диагностика локальной истории проверок и агрегированной статистики.
- Добавлена проверка согласованности истории и статистики.
- Добавлен безопасный пересчёт статистики из истории на экране «Диагностика».
- Добавлен reliability checklist для статистики.
- Добавлены unit-тесты consistency checker, sanitizer и rebuild.

### Changed

- Улучшена обработка ошибок чтения и записи статистики.
- Улучшена устойчивость cleanup истории (сохранение последней записи).
- Улучшена защита Statistics UI от некорректных значений.

### Fixed

- UI статистики не показывает NaN, Infinity и отрицательные проценты/задержки.
- Ошибки storage/statistics не приводят к падению главного экрана и check flow.

### Known Issues

- Worker пока не реализован.
- Экспорт статистики пока не реализован.

---

## 0.8.2 — Statistics UI

### Added

- Добавлен экран локальной статистики проверок.
- Добавлена краткая сводка статистики на главный экран.
- Добавлено отображение общей статистики, endpoints/targets, routeKind, сети/оператора и дневной статистики.
- Добавлены empty, loading, error и stale-состояния для статистики.
- Добавлены unit-тесты форматирования, stale-логики и загрузки дашборда.

### Changed

- Главный экран показывает компактную карточку статистики с переходом на полный экран.

### Fixed

- —

### Known Issues

- Графики пока не реализованы.
- Экспорт статистики пока не реализован.

---

## 0.8.1 — Local Statistics Writer

### Added

- Добавлена локальная запись статистики по завершённым проверкам.
- Добавлены агрегаты общей статистики проверок.
- Добавлена статистика по endpoint/target, routeKind, networkType/operator и дням.
- Добавлена возможность пересобрать статистику из сохранённой истории проверок.
- Добавлены unit-тесты расчёта статистики.

### Changed

- Check flow теперь обновляет локальную статистику после сохранения результата проверки.

### Fixed

- Ошибка обновления статистики не ломает главный экран или результат текущей проверки.

### Known Issues

- Экран статистики пока не реализован.
- Графики пока не реализованы.
- Отправка отчётов наружу пока не реализована.

---

## 0.8.0 — Check Result Persistence Foundation

### Added

- Добавлено локальное сохранение завершённых проверок в Room.
- Добавлены модели `CheckRun` и `CheckTargetResult` для истории проверок.
- Добавлен `CheckHistoryRepository` и retention policy (200 записей / 14 дней).
- Добавлены unit-тесты маппинга и сохранения истории.

### Changed

- Check flow сохраняет структурированный результат завершённой проверки для будущей статистики.
- Фоновые проверки помечаются как `BACKGROUND`, ручные — как `MANUAL`.

### Fixed

- Сохранение истории не ломает отображение последней проверки на главном экране.

### Known Issues

- Экран статистики пока не реализован.
- Агрегированная статистика пока не рассчитывается.
- Отправка отчётов наружу пока не реализована.

---

## 0.7.2 — Last Check Stale State Fix

### Fixed

- Исправлено отображение последней проверки на главном экране, если данные устарели.
- Последняя проверка теперь не скрывается только из-за возраста данных.
- Старые результаты проверки отображаются как устаревшие.
- Старые ошибочные результаты проверки также отображаются как последнее известное состояние.

### Changed

- Уточнена модель состояния последней проверки для главного экрана.
- Последний результат проверки сохраняется локально и восстанавливается после перезапуска.

---

## 0.7.1

### Исправлено

- Возвращено разрешение `CHANGE_NETWORK_STATE`, необходимое для `ConnectivityManager.requestNetwork()`.
- Исправлен crash/ошибка при ручной проверке мобильной сети:
  `was not granted either CHANGE_NETWORK_STATE or WRITE_SETTINGS`.
- Добавлена защита от `SecurityException` при запросе cellular network.

### Важно

`WRITE_SETTINGS` не используется и не требуется.

Подробнее: [docs/releases/v0.7.1-hotfix.md](docs/releases/v0.7.1-hotfix.md).

---

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
