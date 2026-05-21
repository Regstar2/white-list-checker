# WhiteListChecker

Android-приложение для определения признаков режима белых списков мобильной сети.

## Документация

- [Описание MVP и версий](docs/WhiteListChecker%20-%20description.md)
- [Стек технологий](docs/stack.md)
- [Инструкции для агента / разработки](AGENTS.md)
- Версии: [v0.1](docs/versions/v0.1.md) … [v0.6](docs/versions/v0.6.md)

## Стек (MVP)

- Kotlin, Jetpack Compose, Material 3
- MVVM + UseCase (по мере роста проекта)
- Coroutines, DataStore, Room (когда понадобится), WorkManager, OkHttp (Telegram через proxy)
- `minSdk` 26, `compileSdk` / `targetSdk` 35 (SDK 36 — по желанию, через sdkmanager)

## Требования

- JDK 17+
- Android SDK (`ANDROID_HOME` или `local.properties` → `sdk.dir`)
- Platform `android-35`, Build-Tools 35.x
- Git

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Установка на устройство

```powershell
adb devices
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

## Дорожная карта

| Версия | Содержание |
|--------|------------|
| v0.1 | Ручная проверка мобильной сети (Google + Yandex) |
| v0.2 | Группы FOREIGN / LOCAL |
| v0.3 | Debounce, подтверждённое состояние |
| v0.3.5 | Локальные уведомления |
| v0.4 | Telegram через локальный proxy |
| v0.5 | Очередь неотправленных сообщений |
| v0.6 | WorkManager, автопроверка |
| v0.7 | История, UI, диагностика |

Текущий scaffold: `0.0.1-scaffold` — пустой Compose-экран, готов к реализации v0.1.
