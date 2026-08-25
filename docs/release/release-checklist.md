# Release checklist — WhiteListChecker 1.0.0

Этот checklist отражает текущее подтверждённое состояние. Отмечать пункт как выполненный можно только после фактической проверки.

## Scope и repository

- [x] `versionName = 1.0.0`, `versionCode = 26` в Android configuration.
- [x] Центральный/public service удалён из current Android UI/runtime.
- [x] `PUBLIC_SERVICE_BASE_URL` удалён из current build configuration.
- [x] Центральный Cloudflare public-service runtime/workflow удалён.
- [x] Private governance-файлы не входят в публичный repository.
- [x] `.gitignore` содержит правила для `AGENTS.md`, `.project-rules/`, `.codex/` и другого локального AI/tool state.
- [x] Personal development documents удалены из public docs.
- [x] Proxy support для checker path зафиксирован как project-specific `N/A`.

## Данные и миграция

- [x] Room schema повышена `7 -> 8` без destructive migration.
- [x] Migration удаляет только устаревшую `pending_public_reports`.
- [x] На физическом устройстве выполнено обновление debug `0.10.4 -> 1.0.0` через `adb install -r` без очистки данных.
- [x] После обновления история сохранилась.
- [x] После обновления UI общего сервиса отсутствует.
- [ ] Проверить официальный `WhiteListChecker-v0.10.4-release.apk -> 1.0.0 release` с совпадающим release certificate и без очистки данных.

## Localization

- [x] Обязательные Android locale: default/RU и `values-en`.
- [x] Language switch существует в приложении.
- [x] Неиспользуемые hardcoded RU screen/notification helper labels удалены из UI/navigation source.
- [x] Update Delivery UI strings существуют в RU/EN resources.
- [x] Feedback UI strings существуют в RU/EN resources.
- [ ] Финальный smoke test основных экранов на RU.
- [ ] Финальный smoke test основных экранов на EN.
- [ ] Проверить отсутствие критического clipping/overflow на RU/EN с увеличенным системным шрифтом.

## Build и автоматические проверки

Минимальный набор:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
```

- [x] Последний CI ветки удаления public service: debug build прошёл.
- [x] Последний CI ветки удаления public service: unit tests прошли.
- [x] Последний CI ветки удаления public service: Android lint прошёл.
- [x] Последний CI ветки удаления public service: release build прошёл.
- [x] Issue #8 / PR #14: GitHub Actions debug build, unit tests, Android lint и release build прошли.
- [x] Issue #9 / PR #15: финальный GitHub Actions run прошёл debug build, unit tests, Android lint и release build.
- [x] Issue #10 / PR #16: GitHub Actions run #89 — debug build, unit tests, Android lint и release build прошли.
- [ ] Выполнить финальный CI через стандартизированный flow Issue #11.

## Update Delivery

Реализация Issue #9:

- [x] Installed version берётся из `BuildConfig.VERSION_NAME`.
- [x] GitHub PAT/OAuth secret не требуется и не хранится в APK.
- [x] Startup check выполняется асинхронно и его failure не блокирует основной UI/checker.
- [x] Экран «О приложении» содержит ручное действие проверки обновлений.
- [x] Stable installed build использует официальный `github.com/.../releases/latest` path и не расходует REST Releases quota.
- [x] Prerelease path использует GitHub Releases metadata только когда нужна beta/RC discovery.
- [x] Автоматические проверки throttled и не запускаются параллельно с manual check.
- [x] Stable installed build отфильтровывает prerelease releases/tags.
- [x] Пользователь может выбрать «Позже».
- [x] Release URL ограничен официальным repository prefix.
- [x] Silent/background APK installation отсутствует; установка остаётся под контролем Android/пользователя.
- [x] Unit tests добавлены для SemVer, channel selection, errors и GitHub response parsing.
- [x] Физически подтверждено, что manual stable check работает после rate-limit fix.
- [ ] Проверить update-available prompt, «Позже» и переход на официальный release page на controlled test version/fixture.
- [ ] Повторить update UI smoke test на RU и EN.

## Feedback через GitHub Issues

Реализация Issue #10:

- [x] В «О приложении» добавлены действия bug report и feature request.
- [x] Android открывает только официальный `github.com/Regstar2/white-list-checker/issues/new` path.
- [x] В заголовок автоматически добавляется только безопасный `BuildConfig.VERSION_NAME`.
- [x] GitHub PAT/OAuth write credential не требуется и не хранится в APK.
- [x] Приложение не прикладывает автоматически логи, `BOT_TOKEN`, Relay Secret, `chat_id`, personal Worker URL, passwords или signing data.
- [x] Legacy Markdown templates заменены structured `bug_report.yml` и `feature_request.yml`.
- [x] Issue Forms требуют useful reproduction/proposal data и sensitive-data confirmation.
- [x] Blank issues отключены для основного public feedback path.
- [x] Ошибка открытия браузера обрабатывается локализованным сообщением без crash.
- [x] JVM tests проверяют host/path locking и encoding feedback URL.
- [ ] До merge: физически проверить обе кнопки, официальный URL и RU/EN UI.
- [ ] После merge: проверить, что GitHub реально показывает обе `.yml` Issue Forms из default branch.
- [ ] Controlled browser-unavailable smoke test, если среда позволяет воспроизвести сценарий.

## Manual network checks

- [ ] Проверка через cellular при включённом Wi‑Fi.
- [ ] Проверка через cellular при выключенном Wi‑Fi.
- [ ] Недоступность отдельного public DNS не создаёт ложный `MOBILE_DNS_FAILURE`, если site checks дают ясный результат.
- [ ] UDP/53 failure корректно использует TCP/53 fallback там, где применимо.
- [ ] Private DNS телефона не ломает предусмотренный custom-DNS/cellular resolver path.
- [ ] VPN/tunnel явно рассматривается как ограничение достоверности, а не как поддерживаемый checker transport.

## Notifications и background

- [ ] Local test notification.
- [ ] WorkManager background check.
- [ ] Active monitoring: start / check now / stop.
- [ ] Personal Telegram Worker connection test.
- [ ] Personal Telegram test message/report.
- [ ] Telegram commands при включённом active monitoring, если функция используется в release scope.

## Security и privacy

- [x] Центральный project-owned service не требуется для core functionality.
- [x] Checker history/statistics остаются локальными.
- [x] `BOT_TOKEN` не хранится в Android app.
- [x] GitHub update check не содержит PAT/OAuth write credential.
- [x] GitHub feedback не содержит PAT/OAuth write credential и не отправляет Issue через Android API.
- [x] Release keystore/credentials исключены из Git.
- [x] TLS certificate/hostname verification не отключается для target checks.
- [ ] Проверить final diff/history на случайно добавленные secrets перед tag.

## Обязательные отдельные задачи до stable 1.0.0

- [x] #9 — Update Delivery implementation.
- [ ] #9 — оставшийся controlled update-available smoke test.
- [x] #10 — Feedback / GitHub Issues implementation.
- [ ] #10 — physical + post-merge Issue Form smoke test по разделу выше.
- [ ] #11 — trusted CI, Project Sync и release orchestration.

## Release documentation

Перед tag `v1.0.0`:

- [ ] Создать `docs/releases/v1.0.0.md`.
- [ ] Создать синхронный `docs/releases/v1.0.0_EN.md`.
- [ ] Использовать фиксированный порядок `О релизе / Главное / Изменения / Установка и обновление / Проверено / Известные проблемы / Артефакты / Ссылки`.
- [ ] Указать только реально выполненные verification steps.
- [ ] Посчитать SHA-256 финального release APK.
- [ ] Проверить APK через `apksigner verify --print-certs`.
- [ ] Убедиться, что release certificate совпадает с официальным предыдущим release, если заявляется in-place update.
- [ ] GitHub Release должен ссылаться на release notes через tag `v1.0.0`, а не через mutable `main`.

## Финальный gate

Stable `v1.0.0` публикуется только когда все применимые незакрытые пункты выше либо выполнены, либо явно перенесены из release scope с обоснованием в Issue/release notes. Обязательные стандарты #9–#11 нельзя молча пропустить.
