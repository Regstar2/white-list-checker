# AGENTS.md

# Whitelist Checker Android Project Instructions

This project is an Android-native Kotlin application for detecting signs of mobile-network whitelist mode.

The app periodically checks the mobile network, classifies whether whitelist mode appears to be enabled or disabled, and notifies the user when the confirmed state changes.

The user develops this project through Cursor. The user may connect an Android phone in USB debugging mode. After every implemented version/milestone, the agent must build and install the new debug APK onto the connected device and tell the user exactly what to test manually.

Do not skip device installation after a completed version unless the build fails or no device is connected.

---

## 1. Main product goal

Build an Android app that:

1. Checks the mobile network, even when Wi-Fi is active.
2. Detects signs of whitelist mode by comparing availability of FOREIGN and LOCAL target groups.
3. Uses debounce/confirmation logic before treating a state as changed.
4. Sends notifications only when the confirmed whitelist state changes:
   - whitelist OFF → whitelist ON;
   - whitelist ON → whitelist OFF.
5. Supports local Android notifications.
6. Supports optional Telegram notifications through a user-owned Cloudflare Worker relay.
7. Each user creates their own Telegram bot and Worker; Android stores only Worker URL, Relay Secret, and Chat ID.
8. `BOT_TOKEN` must never be stored in Android.
9. No shared Worker, shared Relay Secret, or direct Telegram fallback from the app.
10. Can later perform periodic checks through WorkManager.

---

## 2. Important project constraints

Do not add unnecessary technologies.

Use:

- Kotlin
- Android native
- Jetpack Compose
- Material 3
- Coroutines
- Flow where useful
- DataStore for settings/state
- Room only when structured queue/history storage is needed
- WorkManager only for periodic checks
- OkHttp only for HTTPS calls to the user's Cloudflare Worker relay (not direct Telegram Bot API from Android)

Do not use unless explicitly requested:

- Flutter
- Kotlin Multiplatform
- Firebase
- Retrofit
- Ktor
- RxJava
- foreground service
- VPN service
- custom proxy/VPN implementation
- multi-module architecture
- server backend
- Telegram getUpdates / bot commands

Keep the MVP focused. Do not build an enterprise cathedral for a network checker.

---

## 3. Network checking rules

The app must check the mobile network specifically.

When checking target sites:

1. Request a cellular network through `ConnectivityManager.requestNetwork(...)`.
2. Use `NetworkCapabilities.TRANSPORT_CELLULAR`.
3. Use `NetworkCapabilities.NET_CAPABILITY_INTERNET`.
4. Do not request `NET_CAPABILITY_VALIDATED` inside `NetworkRequest`.
5. Perform site checks through the returned `Network`.
6. Use `network.openConnection(URL(...))`.
7. Do not use plain `URL.openConnection(...)` for target checks.
8. Do not use `bindProcessToNetwork(...)`.
9. Do not use `activeNetwork` for actual target checks.
10. `activeNetwork` may be used only for displaying a label in UI.

Correct flow:

```text
ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR)
    ↓
Network
    ↓
network.openConnection(...)
    ↓
FOREIGN/LOCAL target checks
    ↓
WhitelistStateClassifier
```

---

## 4. Target groups

Do not use YouTube, Telegram, or Discord as whitelist-detection targets.

They may be blocked separately and are bad baseline targets.

Use groups like:

```text
FOREIGN:
- Google
- Cloudflare
- GitHub
- Wikipedia

LOCAL:
- Yandex
- VK
- Mail.ru
- Gosuslugi
```

The exact target list may evolve, but the classifier must rely on groups, not one single domain.

---

## 5. Whitelist states

Use this state model unless the user explicitly changes it:

```kotlin
enum class WhitelistState {
    UNKNOWN,
    WHITELIST_OFF,
    WHITELIST_ON,
    NO_MOBILE_INTERNET,
    PARTIAL_PROBLEM,
    CELLULAR_NETWORK_UNAVAILABLE
}
```

Meaning:

```text
WHITELIST_OFF:
FOREIGN and LOCAL groups are sufficiently available.

WHITELIST_ON:
FOREIGN group is mostly unavailable while LOCAL group is available.

NO_MOBILE_INTERNET:
FOREIGN and LOCAL groups are both unavailable.

PARTIAL_PROBLEM:
The result is mixed and does not clearly match whitelist mode or normal internet.

CELLULAR_NETWORK_UNAVAILABLE:
Android could not provide a cellular Network.

UNKNOWN:
Initial or undefined state.
```

Do not present `WHITELIST_ON` as absolute truth. In UI use wording like:

```text
Похоже на включённые белые списки
```

Do not use wording like:

```text
Белые списки точно включены
```

The app detects symptoms, not the mobile operator's internal rules.

---

## 6. Debounce and confirmed state changes

The app must not notify immediately after one suspicious check.

Use confirmation logic:

```text
A new state must appear 2 times in a row before it becomes confirmed.
```

Notify only for these confirmed transitions:

```text
WHITELIST_OFF → WHITELIST_ON
WHITELIST_ON → WHITELIST_OFF
```

Do not notify for:

```text
UNKNOWN initial state fixation
pendingStateCount = 1
OTHER_CONFIRMED_CHANGE
NO_MOBILE_INTERNET
PARTIAL_PROBLEM
CELLULAR_NETWORK_UNAVAILABLE
ordinary checks without state change
```

The state detector must be independent from notification senders.

Correct architecture:

```text
Network check
    ↓
Classifier
    ↓
StateChangeDetector
    ↓
WhitelistStateChangeEvent
    ↓
Notification dispatchers
```

Do not put notification logic inside `StateChangeDetector`.

---

## 7. Local notification rules

The app should support local Android notifications.

Local notifications must:

1. Be optional.
2. Be controlled by a user setting.
3. Use a notification channel on Android 8+.
4. Request `POST_NOTIFICATIONS` runtime permission on Android 13+.
5. Be sent only for:
   - `WHITELIST_TURNED_ON`;
   - `WHITELIST_TURNED_OFF`.
6. Not be sent for pending states or ordinary checks.

Use:

```text
Channel ID:
whitelist_events

Channel name:
События белых списков

Channel description:
Уведомления о включении и выключении белых списков
```

Use a proper small notification icon. Do not use the launcher icon as the small icon unless there is no better choice.

---

## 8. Telegram notification rules

Telegram notifications are optional and use a **user-owned Cloudflare Worker relay**.

Each user must create:

```text
1. Telegram bot (BotFather)
2. Cloudflare Worker with secrets BOT_TOKEN and RELAY_SECRET
```

Android app stores only:

```text
Worker URL
Relay Secret
Chat ID
enabled flag
```

Worker endpoints (POST + header `X-Relay-Secret`):

```text
<WORKER_URL>/tg/getMe
<WORKER_URL>/tg/getUpdates
<WORKER_URL>/tg/sendMessage
```

`BOT_TOKEN` lives only in Worker secrets. Never in Android, logs, Room queue, or error messages.

Forbidden:

```text
BOT_TOKEN in Android
shared Worker URL or Relay Secret baked into APK
local HTTP/SOCKS proxy for Telegram Bot API as main path
direct api.telegram.org from Android
logging relaySecret
storing relaySecret in Room queue
```

If Worker is unavailable:

```text
Do not call Telegram directly from Android.
Save the report to queue if queue exists (v0.5+).
Show error in UI.
```

Manual test checklist:

```text
1. Worker URL and Relay Secret configured
2. getMe via Worker succeeds
3. chat_id obtained via /start + getUpdates
4. test sendMessage works
5. notification on whitelist state change via Worker
```

---

## 9. Version roadmap

Use this roadmap unless the user changes it.

```text
v0.1   Manual mobile-network check.
v0.2   Group-based FOREIGN/LOCAL checks.
v0.3   Debounce and confirmed state tracking.
v0.3.5 Local Android notifications.
v0.4   Telegram (legacy local proxy; superseded by v0.4.2 Worker relay).
v0.4.2 Telegram via user Cloudflare Worker relay.
v0.5   Telegram pending-message queue.
v0.6   WorkManager periodic checks.
v0.7   UI cleanup, history, diagnostics, reliability improvements.
```

Do not skip versions.

After each completed version:

1. Build the project.
2. Install the APK to the connected debug device.
3. Launch or tell the user how to launch it.
4. Report exactly what the user must test manually.
5. Stop and wait for the user's test results before moving to the next version.

---

## 10. Mandatory build and install procedure after each version

After finishing each version/milestone, run these checks.

### 10.1 Check connected devices

Run:

```powershell
adb devices
```

Expected:

```text
Exactly one device should be listed as "device".
```

If no device is connected:

```text
Stop.
Tell the user in Russian: "Телефон не найден через adb. Подключи телефон в режиме USB debugging и проверь adb devices."
Do not pretend the APK was installed.
```

If multiple devices are connected:

```text
Stop.
Tell the user in Russian to leave only one device connected or provide the target device serial.
Do not randomly pick a device.
```

If the device says `unauthorized`:

```text
Stop.
Tell the user in Russian to confirm the USB debugging authorization prompt on the phone.
```

### 10.2 Build debug APK

On Windows PowerShell, run:

```powershell
.\gradlew.bat assembleDebug
```

If the project uses a Unix shell, run:

```bash
./gradlew assembleDebug
```

If build fails:

```text
1. Fix compilation errors.
2. Rebuild.
3. Do not install an old APK.
4. Report what was fixed in Russian.
```

### 10.3 Locate debug APK

Default path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If the module name is not `app`, find the produced debug APK under:

```text
*/build/outputs/apk/debug/*.apk
```

Do not guess. Use the actual produced APK.

### 10.4 Install APK to phone

Run:

```powershell
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

If the path differs, use the actual APK path.

Flags:

```text
-r = reinstall keeping data
-d = allow version downgrade
```

If install fails because of signature mismatch:

```powershell
adb uninstall <applicationId>
adb install -r -d <path-to-debug-apk>
```

Before uninstalling, warn the user in Russian that app data will be removed.

### 10.5 Optional launch command

If `applicationId` is known, launch with:

```powershell
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1
```

If `applicationId` is unknown:

```text
Read it from app/build.gradle(.kts).
Do not invent it.
```

### 10.6 Android 13+ notification permission helper

For versions with local notifications, if needed during debug, the agent may tell the user to grant permission manually in the app UI.

Do not silently grant permission unless the user asked.

Optional debug command if user explicitly wants it:

```powershell
adb shell pm grant <applicationId> android.permission.POST_NOTIFICATIONS
```

This may fail on Android versions below 13 or depending on app state. That is acceptable.

---

## 11. Required report after every installed version

After building and installing the APK, the agent must report in Russian using this format:

```text
Версия: vX.X
Статус сборки: успешно / ошибка
APK установлен: да / нет
Устройство adb: <device id or model if available>

Что изменено:
- ...
- ...
- ...

Что протестировать на телефоне:
1. ...
2. ...
3. ...

Ожидаемый результат:
- ...
- ...

Если что-то сломается:
- пришли скриншот;
- пришли текст ошибки;
- пришли relevant logcat, если есть.
```

Do not say only:

```text
готово
```

That is useless.

---

## 12. Manual test checklist by version

### v0.1 manual tests

After installing v0.1, tell the user in Russian to test:

```text
1. Открой приложение.
2. Включи Wi-Fi.
3. Включи мобильные данные.
4. Нажми "Проверить мобильную сеть".
5. Проверь, что приложение показывает:
   - активная сеть телефона: Wi-Fi;
   - проверяемая сеть: Mobile;
   - Google доступен/недоступен;
   - Yandex доступен/недоступен;
   - итоговый статус.
6. Выключи мобильные данные и повтори проверку.
7. Ожидаемо: приложение должно показать, что мобильная сеть недоступна.
```

### v0.2 manual tests

After installing v0.2, tell the user in Russian to test:

```text
1. Нажми "Проверить мобильную сеть".
2. Проверь, что отображаются группы:
   - внешние сайты;
   - локальные сайты.
3. Проверь, что по каждому сайту есть:
   - доступен/недоступен;
   - HTTP-код или ошибка;
   - время ответа.
4. Проверь итог:
   - внешние доступны + локальные доступны → БС не обнаружены;
   - внешние недоступны + локальные доступны → похоже на БС;
   - обе группы недоступны → мобильного интернета нет.
```

### v0.3 manual tests

After installing v0.3, tell the user in Russian to test:

```text
1. Выполни первую проверку.
2. Проверь, что приложение зафиксировало начальное подтверждённое состояние.
3. Измени сетевые условия или APN так, чтобы статус изменился.
4. Выполни проверку один раз.
5. Ожидаемо: должно появиться pending-состояние 1/2, но события ещё нет.
6. Выполни проверку второй раз с тем же статусом.
7. Ожидаемо: состояние подтверждается, появляется событие смены.
8. Верни сеть в исходное состояние и повтори 2 проверки.
9. Ожидаемо: появляется обратное событие.
```

### v0.3.5 manual tests

After installing v0.3.5, tell the user in Russian to test:

```text
1. Открой секцию локальных уведомлений.
2. Убедись, что локальные уведомления можно включить/выключить.
3. На Android 13+ нажми "Разрешить уведомления".
4. Проверь, что приложение показывает статус разрешения.
5. Создай подтверждённый переход БС выключены → БС включены.
6. Ожидаемо: появляется локальное уведомление "Белые списки включились".
7. Создай подтверждённый переход БС включены → БС выключены.
8. Ожидаемо: появляется локальное уведомление "Белые списки выключились".
9. Выключи локальные уведомления в приложении и повтори переход.
10. Ожидаемо: уведомление не появляется.
```

### v0.4.2 manual tests

After installing v0.4.2, tell the user in Russian to test:

```text
1. Открой Telegram-секцию.
2. Включи Telegram-уведомления.
3. Введи Worker URL своего Cloudflare Worker.
4. Введи Relay Secret.
5. Нажми "Проверить Worker" — ожидаемо: Worker работает, бот доступен.
6. "Начать получение chat_id" → /start боту → "Получить chat_id" → выбери чат.
7. "Отправить тестовое сообщение" — сообщение в Telegram.
8. Создай подтверждённый переход БС — уведомление через Worker.
```

For groups: add bot to group, /start in group, discover group chat_id, test message to group.

### v0.5 manual tests

After installing v0.5, tell the user in Russian to test:

```text
1. Настрой Telegram.
2. Отключи локальный proxy.
3. Создай подтверждённый переход БС.
4. Ожидаемо: сообщение не отправляется и сохраняется в очередь.
5. Проверь, что UI показывает количество сообщений в очереди.
6. Включи proxy.
7. Нажми "Повторить отправку очереди".
8. Ожидаемо: сообщение отправляется, очередь уменьшается.
9. Проверь, что direct fallback не используется.
```

### v0.6 manual tests

After installing v0.6, tell the user in Russian to test:

```text
1. Открой секцию автопроверки.
2. Включи автопроверку.
3. Выбери интервал 15 минут.
4. Проверь, что UI показывает "Автопроверка включена".
5. Нажми "Запланировать заново", если такая кнопка есть.
6. Подожди запуска WorkManager или запусти worker через Android Studio, если тестируешь вручную.
7. Проверь, что обновились:
   - время последней фоновой проверки;
   - последний статус;
   - последняя ошибка, если была.
8. Выключи автопроверку.
9. Ожидаемо: периодическая задача отменена.
```

---

## 13. Logcat instructions

When debugging runtime crashes, use:

```powershell
adb logcat
```

For focused logs, use package filtering if possible:

```powershell
adb logcat | findstr <applicationId>
```

On PowerShell:

```powershell
adb logcat | Select-String "<applicationId>"
```

If app crashes on launch:

```text
1. Capture the exception stacktrace.
2. Fix the root cause.
3. Rebuild.
4. Reinstall APK.
5. Tell the user in Russian what changed.
```

Do not ask the user to debug obvious compile/runtime errors that the agent can inspect.

---

## 14. Commit/version discipline

After each version:

```text
1. Keep changes scoped to that version.
2. Do not silently implement future versions.
3. Do not rename large parts of the project without need.
4. Do not add dependencies unrelated to the version.
5. Do not leave dead code.
6. Do not leave TODO in core logic.
7. Build before reporting.
8. Install APK before reporting, if a device is connected.
9. Provide manual test checklist in Russian.
```

If the user asks for the next version, continue from the current implemented state.

---

## 15. User-facing language

The agent must communicate with the user in Russian.

All progress reports, build reports, install reports, test checklists, error explanations, and final summaries must be written in Russian.

Use English only for:

```text
1. Code.
2. File names.
3. Class names.
4. Package names.
5. Gradle tasks.
6. Android/ADB commands.
7. API names.
8. Logs and stacktraces.
```

The app UI should be Russian-first.

Use clear Russian wording in the app:

```text
Похоже на включённые белые списки
Белые списки не обнаружены
Мобильного интернета нет
Частичная проблема сети
Мобильная сеть недоступна
Локальные уведомления
Telegram-уведомления
Автопроверка
Проверить мобильную сеть
```

Avoid overclaiming:

```text
Белые списки точно включены
```

Use cautious wording instead:

```text
Похоже на включённые белые списки
```

The app detects symptoms, not the operator's internal rules.

When reporting after build/install, use this Russian format:

```text
Версия: vX.X
Статус сборки: успешно / ошибка
APK установлен: да / нет
Устройство adb: <device id or model if available>

Что изменено:
- ...
- ...
- ...

Что протестировать на телефоне:
1. ...
2. ...
3. ...

Ожидаемый результат:
- ...
- ...

Если что-то сломается:
- пришли скриншот;
- пришли текст ошибки;
- пришли relevant logcat, если есть.
```

Do not answer the user in English unless the user explicitly asks for English.

---

## 16. Done means installed and testable

A version is not done when code was edited.

A version is done only when:

```text
1. The project compiles.
2. Debug APK is produced.
3. APK is installed on the connected debug device, if available.
4. The app launches or the agent reports why launch could not be verified.
5. The agent provides a concrete manual test checklist in Russian.
```

Anything else is theater.