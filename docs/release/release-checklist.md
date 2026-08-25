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
- [x] Issue #8 / PR #14 после retarget на `main`: GitHub Actions run #67 — debug build, unit tests, Android lint и release build прошли.
- [ ] Выполнить финальный CI через стандартизированный flow Issue #11.

## Update Delivery

Реализация Issue #9:

- [x] Installed version берётся из `BuildConfig.VERSION_NAME`.
- [x] Trusted source — public GitHub Releases API `Regstar2/white-list-checker`.
- [x] GitHub PAT/OAuth secret не требуется и не хранится в APK.
- [x] Startup check выполняется асинхронно и его failure не блокирует основной UI/checker.
- [x] Экран «О приложении» содержит ручное действие проверки обновлений.
- [x] Stable installed build отфильтровывает GitHub prerelease и SemVer prerelease tags.
- [x] Draft releases игнорируются.
- [x] При update available показываются installed/new version и краткие release notes.
- [x] Пользователь может выбрать «Позже».
- [x] Release URL строится только на официальном repository prefix.
- [x] Silent/background APK installation отсутствует; установка остаётся под контролем Android/пользователя.
- [x] Unit tests добавлены для SemVer, channel selection, errors и GitHub response parsing.
- [ ] На физическом устройстве проверить, что startup update check не задерживает запуск.
- [ ] На физическом устройстве выполнить ручную проверку в «О приложении» при доступном GitHub.
- [ ] Проверить manual error state при недоступном `api.github.com`.
- [ ] Проверить update-available prompt, «Позже» и переход на официальный release page на controlled test version/fixture.
- [ ] Повторить update UI smoke test на RU и EN.

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
- [x] Release keystore/credentials исключены из Git.
- [x] TLS certificate/hostname verification не отключается для target checks.
- [ ] Проверить final diff/history на случайно добавленные secrets перед tag.

## Обязательные отдельные задачи до stable 1.0.0

- [x] #9 — Update Delivery implementation.
- [ ] #9 — physical update-flow smoke test по разделу выше.
- [ ] #10 — Feedback / GitHub Issues path.
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
