# GitHub automation — WhiteListChecker

WhiteListChecker использует project-specific automation entry points по стандарту Regstar2/template (ранее `Regstar2/sheduler`). Workflow-файлы отвечают только за orchestration и trust gates; реальные Android-команды находятся в `scripts/`.

## Состав

```text
.github/workflows/trusted-ci.yml
.github/workflows/project-sync.yml
.github/workflows/release.yml
scripts/ci.ps1
scripts/release.ps1
```

Legacy `.github/workflows/android.yml` удалён. Отдельного public-service/Worker release workflow нет: удалённый центральный сервис не входит в Android release chain.

## Trusted CI

`trusted-ci.yml` использует `pull_request_target`, но код PR checkout'ится и исполняется на persistent self-hosted runner только после строгого gate:

- `github.actor == Regstar2`;
- `github.triggering_actor == Regstar2`;
- PR создан `Regstar2`;
- head repository совпадает с target repository.

External/fork PR поэтому не получает выполнение на trusted Windows machine. Workflow definition берётся из base branch, а после gate checkout выполняется по exact `pull_request.head.sha` с `persist-credentials: false`.

Runner labels:

```text
self-hosted
Windows
X64
```

Основной entry point:

```powershell
./scripts/ci.ps1
```

Script выполняет обязательные Android checks без преобразования ошибок в warning:

- `testDebugUnitTest`;
- `lintDebug`;
- `assembleDebug`;
- `assembleRelease`;
- проверку наличия APK;
- `apksigner verify` для signed release APK, когда release signing настроен в текущем окружении.

Если signing credentials отсутствуют, CI всё равно проверяет unsigned release build. Фактическая публикация через `scripts/release.ps1` unsigned APK не допускает.

## Project Sync

`project-sync.yml` реагирует на owner events `issues: opened, reopened, transferred` и добавляет Issue в:

```text
https://github.com/users/Regstar2/projects/2
```

Требуется repository secret:

```text
ADD_TO_PROJECT_PAT
```

Значение PAT нельзя хранить в repository, documentation, logs или issue body. Workflow передаёт его только в официальный `actions/add-to-project`.

Если secret ещё не настроен, Project Sync считается deployment/configuration blocker: workflow-файл готов, но интеграционный smoke test должен выполняться только после добавления secret в GitHub Actions settings.

## Release orchestration

`release.yml` запускается:

- автоматически на tag `v*`;
- вручную через `workflow_dispatch` для уже существующего tag.

Release job доступен только trusted owner и работает на `[self-hosted, Windows, X64]`.

Последовательность:

```text
exact tag
  -> scripts/ci.ps1
  -> clean dist/
  -> scripts/release.ps1 -Version <tag>
  -> verify non-empty dist/
  -> gh release create
```

Workflow не публикует повторно уже существующий GitHub Release. Для prerelease tag с `-` (`-beta.1`, `-rc.1` и т.п.) используется GitHub prerelease flag.

## Release signing

Signing secrets не передаются в trusted PR CI и не хранятся в Git.

Android build уже поддерживает следующие значения через Gradle property / environment:

```text
WL_RELEASE_STORE_FILE
WL_RELEASE_STORE_PASSWORD
WL_RELEASE_KEY_ALIAS
WL_RELEASE_KEY_PASSWORD
```

На self-hosted release runner их нужно настроить вне repository — например, в environment runner service или в user-level Gradle configuration того account, под которым работает runner. Keystore также хранится вне Git.

`scripts/release.ps1` fail-closed:

1. проверяет формат `vX.Y.Z`/SemVer prerelease;
2. сверяет tag с `app/build.gradle.kts -> versionName`;
3. собирает release APK заново;
4. требует именно signed `app-release.apk`;
5. проверяет APK через `apksigner`;
6. отклоняет Android Debug certificate;
7. копирует APK в `dist/` с versioned name;
8. создаёт `SHA256SUMS.txt`.

Release script не знает и не выводит пароли signing key.

## Локальный запуск

Полный CI тем же entry point, что использует GitHub:

```powershell
./scripts/ci.ps1
```

Подготовка publishable artifact при настроенном release signing:

```powershell
./scripts/release.ps1 -Version v1.0.0
```

Ожидаемый `dist/`:

```text
WhiteListChecker-v1.0.0-release.apk
SHA256SUMS.txt
```

## Интеграционные проверки после merge

Новые workflow начинают полноценно работать из default branch только после merge. После попадания automation в `main` выполнить:

1. создать owner same-repository test PR и убедиться, что `Trusted CI` запускается на self-hosted Windows runner;
2. убедиться, что external/fork PR получает skipped trusted job и не выделяет self-hosted runner;
3. при настроенном `ADD_TO_PROJECT_PAT` создать тестовый Issue владельца и проверить появление в Development Project;
4. выполнить `workflow_dispatch` Trusted CI из `main`;
5. перед stable release проверить `scripts/release.ps1 -Version v1.0.0` локально на trusted runner с release signing;
6. release workflow тестировать только на точном существующем test/prerelease tag либо непосредственно на финальном tag после выполнения release checklist.

Нельзя создавать production tag только ради проверки YAML.
