Сейчас зафиксируем стек так, чтобы Cursor не превратил проект в “а давай ещё Clean Architecture, KMP, Room, Hilt и маленький дата-центр”. Цель: Android-native MVP, минимум магии, максимум контролируемости.

Думал на протяжении 40s

Да. Фиксируем стек так:

```
Android nativeKotlinJetpack ComposeMaterial 3MVVM + UseCase слойHiltCoroutines + FlowDataStoreRoomWorkManagerOkHttp
```

Без Flutter, без KMP, без сервера, без Firebase. Иначе Cursor начнёт строить “архитектуру будущего”, а нам нужно приложение, которое не развалится от двух HTTP-запросов.

## Итоговый стек для MVP

|Зона|Выбор|Почему|
|---|---|---|
|Язык|**Kotlin**|Нативный Android, нормальная работа с coroutines, Compose, WorkManager|
|UI|**Jetpack Compose**|Быстрее собрать MVP, меньше XML-мусора; Compose официально поддерживает Material 3.|
|Дизайн|**Material 3**|Стандартный современный Android UI, без самодельного визуального цирка|
|Архитектура|**MVVM + UseCases**|Достаточно чисто, но не переусложнено|
|DI|**Hilt**|Android официально рекомендует Hilt как стандартный DI-подход; удобно для репозиториев, клиентов, Worker’ов.|
|Асинхронность|**Coroutines + Flow**|Нормально ложится на ViewModel, DataStore, WorkManager и сетевые проверки.|
|Настройки|**DataStore Preferences**|Для bot token, chat id, proxy host/port, последнего состояния; DataStore позиционируется как замена SharedPreferences.|
|История/очередь|**Room**|Для истории проверок и очереди неотправленных Telegram-сообщений; Room даёт слой над SQLite.|
|Фоновые проверки|**WorkManager**|Для проверки раз в 15/30/60 минут; у PeriodicWorkRequest минимум 15 минут.|
|Telegram API|**OkHttp**|Удобно делать `sendMessage/getMe`, плюс можно жёстко задать HTTP/SOCKS proxy|
|Telegram proxy|**java.net.Proxy + OkHttpClient.proxy(...)**|Telegram всегда идёт только через локальный proxy, без direct fallback|
|Проверка мобильной сети|**ConnectivityManager + Network.openConnection()**|Нужно явно запрашивать `TRANSPORT_CELLULAR`, а не верить `activeNetwork`, потому что Wi-Fi всё испортит с невинным лицом. Android даёт API для запроса сети по transport/capability.|

## SDK

```
minSdk = 26targetSdk = 36compileSdk = 36
```

Почему так:

```
minSdk 26 — Android 8.0, достаточно широко и без лишней боли.targetSdk/compileSdk 36 — Android 16 / API 36.
```

Android 16 соответствует API level 36. Для Google Play с 31 августа 2025 новые приложения и обновления должны target’ить Android 15 / API 35 или выше, так что target 36 нормален для нового проекта.

## Что НЕ берём

```
Не берём Flutter.Не берём Kotlin Multiplatform.Не берём Firebase.Не берём Retrofit.Не берём Ktor.Не берём RxJava.Не берём Compose Multiplatform.Не делаем multi-module на MVP.Не делаем свой VPN/прокси-клиент.
```

Retrofit тут не нужен: у нас 2 метода Telegram Bot API и простые HTTP-проверки. Тащить Retrofit ради `sendMessage` — это как покупать экскаватор, чтобы посадить кактус.

## Финальная архитектура

```
app├── core│   ├── network│   ├── time│   └── util│├── data│   ├── settings│   ├── history│   ├── telegram│   └── checker│├── domain│   ├── model│   ├── checker│   ├── classifier│   ├── monitor│   └── telegram│├── worker│   └── WhitelistCheckWorker│└── ui    ├── main    ├── settings    └── history
```

## Основные зависимости

```
// UIandroidx.activity:activity-composeandroidx.compose.ui:uiandroidx.compose.material3:material3androidx.lifecycle:lifecycle-viewmodel-composeandroidx.navigation:navigation-compose// DIcom.google.dagger:hilt-androidcom.google.dagger:hilt-compilerandroidx.hilt:hilt-navigation-composeandroidx.hilt:hilt-work// Asyncorg.jetbrains.kotlinx:kotlinx-coroutines-android// Storageandroidx.datastore:datastore-preferencesandroidx.room:room-runtimeandroidx.room:room-ktxandroidx.room:room-compiler// Backgroundandroidx.work:work-runtime-ktx// Networkcom.squareup.okhttp3:okhttp// Testsjunit:junitandroidx.test.ext:junitandroidx.test.espresso:espresso-coreandroidx.compose.ui:ui-test-junit4org.jetbrains.kotlinx:kotlinx-coroutines-test
```

## Как распределить задачи

### Проверка БС

```
ConnectivityManager↓request TRANSPORT_CELLULAR↓Network.openConnection()↓проверка foreign/local целей↓WhitelistStateClassifier
```

### Telegram

```
OkHttpClient с Proxy↓getMe / sendMessage↓api.telegram.org↓если ошибка — Room queue
```

### Хранение

```
DataStore:- bot token- chat id- proxy type- proxy host- proxy port- last confirmed whitelist state- debounce state- background check enabledRoom:- check history- pending telegram reports
```

## Вердикт

Для Cursor делай **один Android-модуль `app`**, Kotlin + Compose + Hilt + DataStore + Room + WorkManager + OkHttp.

Это достаточно серьёзно, чтобы проект не был кашей, но ещё не настолько “энтерпрайз”, чтобы приложение для двух сайтов выглядело как банковская система с тремя архитекторами и ни одной рабочей кнопкой.