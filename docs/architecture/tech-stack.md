# Технологический стек WhiteListChecker 1.0

## Выбранный стек

| Зона | Технология | Назначение |
|---|---|---|
| Язык | Kotlin | Android-код и domain/data/UI слои |
| JDK | 17 | Gradle/Android compilation target |
| UI | Jetpack Compose + Material 3 | Пользовательский интерфейс без XML layouts |
| Архитектура | MVVM + use cases | Разделение UI, domain и data responsibilities |
| Composition root | manual `AppContainer` | Явное создание и передача зависимостей без DI framework |
| Асинхронность | Kotlin Coroutines + Flow | ViewModel, repositories, background flows |
| Локальные настройки | DataStore Preferences | Настройки checker, monitoring, notifications и Telegram relay |
| Локальная БД | Room, schema 8 | История, статистика, timeline и локальные очереди |
| Фоновые проверки | WorkManager | Периодические проверки с ограничениями Android |
| Активный мониторинг | Foreground Service | Явно запущенный пользователем мониторинг |
| HTTP(S) | OkHttp + cellular `Network` socket factory | Target checks и личный Worker relay по применимости |
| Cellular routing | `ConnectivityManager.requestNetwork(...)` | Получение отдельного `TRANSPORT_CELLULAR` без process-wide binding |
| DNS | network-bound UDP/53 + TCP/53 fallback | Дополнительная DNS-диагностика |

## Android SDK

```text
minSdk      = 26
compileSdk  = 35
targetSdk   = 35
versionCode = 26
versionName = 1.0.0
```

## Архитектурные причины выбора

- один Android-модуль `app` достаточен для текущего масштаба;
- manual DI сохраняет composition root явным и не требует Hilt/Dagger;
- Room отделяет долговечную историю и агрегаты от UI;
- DataStore используется для небольших настроек и сериализуемого состояния;
- WorkManager применяется там, где Android сам планирует периодическую работу;
- foreground service используется только для активного мониторинга, который пользователь запускает явно;
- checker работает через конкретный cellular `Network`, чтобы Wi‑Fi не подменял измеряемый маршрут.

## Сетевая политика

Основной checker специально не поддерживает proxy/VPN transport:

> Proxy support: N/A by project-specific design — WhiteListChecker измеряет прямой маршрут cellular-сети; прокси/VPN изменяет измеряемый транспорт и может сделать результат диагностики недостоверным.

Это исключение относится к измеряемому checker path. Не следует добавлять `System / Direct / Custom proxy` переключатель в этот путь только ради формального соответствия универсальному proxy-стандарту.

Опциональный personal Telegram relay является отдельной внешней интеграцией и использует HTTPS к Worker, указанному пользователем. Центрального Worker владельца проекта нет.

## Что сознательно не используется

- Hilt / Dagger;
- Retrofit / Ktor;
- Firebase;
- process-wide `bindProcessToNetwork`;
- отключение TLS certificate/hostname verification;
- встроенный project-owned central service;
- project-owned public Telegram bot;
- proxy/VPN transport для основного checker path;
- `BOT_TOKEN` в Android-приложении.

## Сборка

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
```

## Тестирование

- JUnit;
- mockito-kotlin;
- kotlinx-coroutines-test;
- Android lint;
- debug/release Gradle builds;
- ручные проверки на физическом Android-устройстве для cellular routing и migration scenarios.

## Официальные источники технологий

- Android Developers — Connectivity: https://developer.android.com/develop/connectivity
- Android Developers — WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Android Developers — Room: https://developer.android.com/training/data-storage/room
- Android Developers — DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Jetpack Compose: https://developer.android.com/compose
- OkHttp: https://square.github.io/okhttp/
