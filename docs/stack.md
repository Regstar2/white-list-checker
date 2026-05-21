# Стек WhiteListChecker (MVP)

Актуально для версии **0.7.0**. Один Android-модуль `app`, без лишних фреймворков.

## Итоговый стек

| Зона | Выбор | Зачем |
|------|-------|-------|
| Язык | **Kotlin** | Нативный Android, coroutines, Compose |
| UI | **Jetpack Compose** | Экраны без XML |
| Дизайн | **Material 3** | Стандартный современный UI |
| Архитектура | **MVVM + UseCases** | ViewModel + domain use cases |
| DI | **Manual `AppContainer`** | Простая композиция зависимостей без Hilt |
| Асинхронность | **Coroutines + Flow** | ViewModel, DataStore, WorkManager |
| Настройки | **DataStore Preferences** | Telegram, автопроверка, monitor state, сайты |
| Очередь | **Room** | Pending Telegram reports |
| Фон | **WorkManager** | Периодическая автопроверка (≥ 15 мин) |
| HTTP | **OkHttp** | HTTPS к Cloudflare Worker relay |
| Проверка сети | **ConnectivityManager + Network.openConnection()** | Явный `TRANSPORT_CELLULAR` |

## SDK

```
minSdk     = 26
targetSdk  = 35
compileSdk = 35
versionName = 0.7.1
```

## Android permissions

`CHANGE_NETWORK_STATE` is required because the app calls `ConnectivityManager.requestNetwork()` to obtain a cellular `Network` while Wi-Fi may be active.

Do **not** remove this permission. Do **not** add `WRITE_SETTINGS` for this use case.

## Что не используется

- Hilt / Dagger
- Retrofit / Ktor
- Navigation Compose (навигация через `AppScreen` enum)
- Firebase
- Flutter / KMP
- Локальный HTTP/SOCKS proxy для Telegram
- Прямые вызовы `api.telegram.org` из Android
- Хранение `BOT_TOKEN` в приложении

## Структура проекта

```text
app/src/main/java/com/whitelistchecker/
├── AppContainer.kt              # manual DI
├── MainActivity.kt
├── WhitelistCheckerApplication.kt
├── data/
│   ├── background/              # DataStore: автопроверка
│   ├── db/                      # Room
│   ├── monitor/                 # DataStore: monitor state
│   ├── notifications/           # DataStore: локальные уведомления
│   ├── targets/                 # DataStore: список сайтов
│   └── telegram/                # DataStore + Room queue
├── domain/
│   ├── checker/                 # cellular network + site checks
│   ├── classifier/
│   ├── monitor/                 # StateChangeDetector
│   ├── notifications/
│   └── telegram/                # Worker client, broadcast, queue
├── worker/
│   └── WhitelistCheckWorker.kt
└── ui/
    ├── home/
    ├── notifications/
    ├── checksettings/
    ├── autocheck/
    ├── diagnostics/
    ├── components/
    └── main/
```

## Проверка белых списков

```text
ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR)
    ↓
Network.openConnection()
    ↓
FOREIGN / LOCAL targets (редактируемый список)
    ↓
WhitelistStateClassifier
    ↓
StateChangeDetector (2 подтверждения подряд)
    ↓
Local / Telegram notifications (только WHITELIST_OFF ↔ WHITELIST_ON)
```

## Telegram

```text
Android (Worker URL + Relay Secret + recipients)
    ↓
HTTPS POST + header X-Relay-Secret
    ↓
User's Cloudflare Worker
    ↓
Telegram Bot API (BOT_TOKEN только в Worker secrets)
```

Эндпоинты relay:

```text
<WORKER_URL>/tg/getMe
<WORKER_URL>/tg/getUpdates   # только ручной chat_id discovery
<WORKER_URL>/tg/sendMessage
```

При недоступности Worker сообщение сохраняется в Room-очередь; **direct fallback запрещён**.

## Хранение

**DataStore:**

- monitor state (confirmed / pending)
- local notification settings
- Telegram: enabled, worker URL, relay secret, recipients JSON, discovery offset
- background check settings / status
- editable check targets JSON

**Room:**

- `pending_telegram_reports` — очередь неотправленных сообщений

## Gradle-зависимости (основные)

```text
androidx.core:core-ktx
androidx.activity:activity-compose
androidx.compose.material3:material3
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.lifecycle:lifecycle-runtime-compose
androidx.datastore:datastore-preferences
androidx.room:room-runtime / room-ktx
androidx.work:work-runtime-ktx
com.squareup.okhttp3:okhttp
```

Тесты: JUnit, mockito-kotlin, kotlinx-coroutines-test.

## Сборка

```powershell
.\gradlew.bat assembleDebug
```
