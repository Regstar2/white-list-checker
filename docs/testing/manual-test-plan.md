# Ручной test plan — WhiteListChecker 1.0.0

Исторические сценарии старых версий находятся в `docs/versions/`. Этот файл описывает только актуальный release scope `1.0.0`.

## 1. Установка и migration

1. Зафиксировать текущую версию, историю, статистику, custom sites/DNS и настройки уведомлений на `0.10.4`.
2. Установить `1.0.0` поверх существующего приложения через совместимо подписанный APK без `adb uninstall` и без очистки данных.
3. Убедиться, что приложение запускается без crash.
4. Проверить `versionCode=26`, `versionName=1.0.0`.
5. Убедиться, что история и статистика сохранились.
6. Убедиться, что custom sites/DNS и остальные локальные настройки сохранились.
7. Убедиться, что плитка/экран «Общий сервис» отсутствует.
8. Убедиться, что приложение не запрашивает approximate location для удалённого public-service сценария.

Фактически уже проверено на debug update path: история сохранилась, UI общего сервиса исчез.

## 2. Основной cellular checker

1. Включить мобильные данные и Wi‑Fi.
2. Запустить «Проверить мобильную сеть».
3. Убедиться, что результат относится к cellular route, а Wi‑Fi остаётся системной active network по применимости.
4. Отключить Wi‑Fi и повторить проверку.
5. Сравнить диагностические route/network данные в обоих сценариях.
6. Проверить, что новые результаты сохраняются в историю и статистику.
7. Повторить после перезапуска приложения.

## 3. DNS

1. Проверить стандартный набор enabled DNS resolvers.
2. Сделать один resolver недоступным или отключить его и повторить проверку.
3. Убедиться, что недоступность отдельного public DNS не создаёт `MOBILE_DNS_FAILURE`, если site checks дают ясный результат.
4. Проверить UDP/53 path.
5. Проверить TCP/53 fallback после transport timeout/network failure, если среда позволяет воспроизвести сценарий.
6. Включить Android Private DNS и повторить проверку.
7. Убедиться, что custom-DNS/cellular resolver path работает независимо от системного Private DNS настолько, насколько это предусмотрено текущей архитектурой.
8. Проверить сообщение при реальном DNS-resolution failure всех site checks.

## 4. Proxy/VPN boundary

1. Без VPN выполнить baseline check.
2. При включённом VPN убедиться, что документация/диагностика не обещает достоверное измерение прямого cellular-маршрута.
3. Убедиться, что в UI checker нет `System / Direct / Custom proxy` transport selector.
4. Убедиться, что приложение не выполняет скрытый fallback через proxy/VPN.

`Proxy support: N/A` для checker path является проектным решением, а не дефектом release.

## 5. Sites и DNS settings

1. Добавить custom LOCAL site.
2. Добавить custom FOREIGN site.
3. Отключить/включить target.
4. Удалить custom target.
5. Добавить custom DNS server.
6. Проверить валидацию IPv4/port/duplicates.
7. Перезапустить приложение и убедиться, что настройки сохранились.
8. Проверить reset к default sites/DNS.

## 6. Статистика и диагностика

1. Открыть статистику после нескольких checks.
2. Переключить day/week/month/year periods.
3. Проверить timeline и current state.
4. Экспортировать CSV.
5. Экспортировать JSON.
6. Экспортировать TXT.
7. Открыть диагностику и проверить группы сайтов/DNS.
8. Скопировать detailed report.
9. Выполнить rebuild statistics и убедиться, что история не удаляется.

## 7. Local notifications

1. Разрешить Android notifications.
2. Отправить test notification.
3. Проверить notification policy для background checks.
4. Проверить отсутствие ложного state-change notification при временной недоступности проверки.
5. Проверить recovery notification, если соответствующая настройка включена.

## 8. WorkManager

1. Включить background checks.
2. Выбрать допустимый interval.
3. Сохранить настройки.
4. Выполнить «запустить сейчас».
5. Проверить сохранение результата.
6. Перезапустить приложение и проверить сохранение настроек.

## 9. Active monitoring

1. Запустить foreground active monitoring.
2. Проверить постоянное уведомление.
3. Нажать «Проверить сейчас».
4. Убедиться, что параллельная проверка не создаётся.
5. Нажать «Остановить» и убедиться, что service/notification завершаются.
6. Проверить взаимодействие active monitoring с WorkManager schedule.
7. Перезагрузить телефон и убедиться, что foreground service не стартует сам без заявленного поведения.

## 10. Personal Telegram Worker

1. Настроить user-owned Worker URL и Relay Secret.
2. Проверить Worker connection.
3. Найти/add recipient.
4. Отправить test message.
5. Отправить последний check report.
6. Временно сделать Worker недоступным и проверить local queue/retry behavior.
7. Убедиться, что direct Android fallback к `api.telegram.org` отсутствует.
8. Если Telegram commands включены: проверить `/status` и `/check` во время active monitoring.
9. Команда от неразрешённого `chat_id` не должна выполнять remote check.

## 11. Update Delivery

### Обычная проверка текущей 1.0.0

1. Запустить приложение с доступом в интернет.
2. Убедиться, что старт приложения не блокируется ожиданием GitHub.
3. Открыть «О приложении».
4. Нажать «Проверить обновления».
5. Пока публичного stable release новее `1.0.0` нет, увидеть состояние «актуальная версия».
6. Убедиться, что на экране указан официальный GitHub Releases как источник и нет обещания автоматической установки APK.

### Ошибка сети

1. Отключить интернет.
2. Перезапустить приложение — startup update check не должен приводить к crash/dialog, блокирующему основной UI.
3. Выполнить обычную проверку мобильной сети — checker должен продолжать работать независимо от updater failure.
4. Открыть «О приложении» и нажать ручную проверку обновлений.
5. Убедиться, что отображается локализованная контролируемая ошибка.
6. Вернуть интернет и повторить ручную проверку.

### Проверка update available до stable release

Для воспроизводимой проверки без публикации ложного stable релиза использовать test build с временным `versionName`, меньшим уже существующего официального stable release, либо отдельный controlled test fixture/branch. Не публиковать фиктивный production release только ради UI test.

Проверить:

1. При обнаружении новой версии показывается dialog с installed/new version.
2. «Позже» закрывает dialog и не блокирует приложение.
3. На экране «О приложении» обновление остаётся видимым после «Позже».
4. Повторная ручная проверка снова разрешает показать prompt.
5. «Открыть релиз» ведёт только на `github.com/Regstar2/white-list-checker/releases/tag/...`.

### Stable/prerelease policy

Автоматически покрывается unit tests. Перед release дополнительно проверить на controlled fixtures, что stable installed version не предлагает `alpha/beta/rc` как stable update, а prerelease installed build может увидеть более новый prerelease/stable release.

## 12. Feedback via GitHub Issues

### До merge PR с Issue Forms

1. Открыть «О приложении».
2. Убедиться, что виден раздел «Обратная связь» с двумя действиями: «Сообщить об ошибке» и «Предложить улучшение».
3. Нажать каждую кнопку и убедиться, что открывается только `github.com/Regstar2/white-list-checker/issues/new`.
4. Проверить, что в редактируемом заголовке предзаполнена текущая версия приложения.
5. Убедиться, что приложение не показывает/не прикладывает автоматически логи, Worker URL, Relay Secret, `chat_id` или другие пользовательские данные.
6. Вернуться в приложение и убедиться, что checker/UI продолжают работать.

GitHub читает Issue Forms из default branch. Пока новые `.yml` ещё не находятся в `main`, URL может не отобразить финальную structured form. Это не заменяет post-merge check ниже.

### После merge Issue Forms в main

1. Повторно открыть bug report.
2. Убедиться, что GitHub показывает `bug_report.yml`, а не blank issue/старый Markdown template.
3. Проверить обязательные поля: problem, reproduction steps, expected behavior, app/Android version, network context и sensitive-data confirmation.
4. Повторно открыть feature request.
5. Убедиться, что GitHub показывает `feature_request.yml` с problem/proposal/area и sensitive-data confirmation.
6. Не отправлять тестовый Issue, если это не требуется; достаточно проверить заполнение формы и закрыть страницу.

### Browser failure

На устройстве/эмуляторе без Activity для HTTPS либо в controlled test убедиться, что попытка открыть feedback не приводит к crash и показывает локализованное сообщение об ошибке.

## 13. Localization и UI

Повторить основные экраны на RU и EN:

- Home;
- Checks / Sites / DNS;
- Statistics;
- Diagnostics;
- Notifications;
- Auto-check / active monitoring;
- Settings;
- About / Updates / Feedback.

Проверить:

1. Нет hardcoded raw localization keys.
2. Нет очевидного смешивания RU/EN пользовательского текста.
3. Нет clipping/overflow на стандартном и увеличенном системном font scale.
4. Language selection сохраняется после перезапуска.
5. Unsupported system locale использует предусмотренный fallback.
6. Update UI и обе Feedback-кнопки переведены на RU/EN.

## 14. Crash/log sanity

После полного smoke test:

```powershell
adb logcat -b crash -d
adb logcat -d | Select-String "FATAL EXCEPTION|Room|pending_public_reports|PublicService|GitHubFeedback"
```

Упоминание `pending_public_reports` допустимо только как SQL migration/drop context. Crash/error из-за отсутствующей таблицы недопустим.

## 15. Release result

Результаты ручной проверки переносятся в `docs/release/release-checklist.md` и затем только фактически выполненные пункты попадают в `docs/releases/v1.0.0*.md`.
