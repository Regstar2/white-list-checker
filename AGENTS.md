# AGENTS.md

# WhiteListChecker Android Project Instructions

WhiteListChecker is an Android-native Kotlin application for detecting observable signs of mobile-network whitelist mode.

The application must remain conservative: it reports network symptoms, not an operator's internal policy. User-facing wording for a positive result must remain cautious, for example «Похоже на включённые белые списки».

Before network changes, read:

- `docs/network-routing-notes.md`;
- the current version document under `docs/versions/`;
- `docs/universal_development_principles.md` when present.

---

## 1. Core stack and scope

Use the existing stack unless a task explicitly requires something else:

- Kotlin;
- native Android;
- Jetpack Compose / Material 3;
- Coroutines / Flow;
- DataStore for settings and lightweight state;
- Room for structured history/queues/statistics;
- WorkManager for periodic work;
- OkHttp for the existing Worker/public-service clients and for cellular target checks that require custom DNS.

Do not introduce without an explicit requirement:

- Flutter or Kotlin Multiplatform;
- Firebase;
- Retrofit or Ktor;
- RxJava;
- a new multi-module architecture;
- a custom backend for target checks;
- root requirements;
- a local proxy;
- a new VPN service;
- process-wide network binding.

Do not rewrite working areas merely because they can be refactored.

---

## 2. Architecture rules

Keep responsibilities separated:

```text
ui -> ViewModel -> domain/use cases -> repositories/infrastructure
```

Rules:

1. Composables do not create or receive repositories/network clients.
2. `AppContainer` is the composition root for infrastructure dependencies.
3. Domain logic returns typed states/errors, not localized UI strings.
4. User-visible strings belong in Android resources.
5. Keep public APIs small and dependencies explicit.
6. One business rule should have one implementation location.
7. Avoid generic `Manager`, `Processor`, `Handler`, or universal repository classes when a precise responsibility exists.
8. Do not change public-service JSON contracts as a side effect of a local Android feature.

---

## 3. Cellular target-check routing

Target checks must use the mobile network specifically, even when Wi-Fi is active.

For every check run:

1. Request a network with `ConnectivityManager.requestNetwork(...)`.
2. Require `NetworkCapabilities.TRANSPORT_CELLULAR`.
3. Require `NetworkCapabilities.NET_CAPABILITY_INTERNET`.
4. Do not require `NET_CAPABILITY_VALIDATED` in the request.
5. Hold the returned `Network` for the complete DNS + HTTPS check run.
6. Release the callback in `finally` after all stages finish.
7. Do not use `activeNetwork` for target traffic; it is display/diagnostic data only.
8. Do not call `bindProcessToNetwork(...)`.

Correct v0.9+ flow:

```text
ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR)
    ↓
cellular Network
    ↓
DNS probes bound to that Network
    ↓
CellularDnsResolver
    ↓
OkHttp target-check session
  socketFactory = cellular Network.socketFactory
  dns = CellularDnsResolver
    ↓
FOREIGN / LOCAL sites
    ↓
Site signal + DNS signal
    ↓
WhitelistStateClassifier
```

`CHANGE_NETWORK_STATE` remains required for explicit cellular requests. Do not add `WRITE_SETTINGS` for this feature.

---

## 4. Custom DNS rules

Android Private DNS must not be part of the main target-resolution path.

Configured DNS servers have two independent roles:

1. **Resolver role** — resolve control-site hostnames.
2. **Diagnostic role** — their own availability contributes a second whitelist-related signal.

For raw DNS/53:

- the resolver endpoint must be a literal IP address unless an independently bootstrapped protocol is implemented;
- UDP sockets must be bound to the supplied cellular `Network`;
- TCP fallback must use that `Network.socketFactory`;
- do not call `InetAddress.getByName(...)` for resolver endpoints or target hostnames;
- do not use `Network.getAllByName(...)` as the target resolver;
- do not silently fall back to Android/system DNS when custom DNS is unavailable;
- a DNS server is available only after a valid DNS response, not after a TCP connect alone;
- validate transaction IDs and malformed responses;
- use TCP fallback when the UDP response is truncated;
- keep hostname cache in memory for a single check run only.

DNS groups:

```text
FOREIGN = external DNS infrastructure
LOCAL   = local DNS infrastructure
```

A DNS group is only a classification attribute. Never restrict a FOREIGN site to FOREIGN DNS or a LOCAL site to LOCAL DNS. Any available enabled resolver may resolve any target hostname.

Disabled DNS servers must not participate in probing, resolving, or classification.

At least one custom DNS must remain enabled for a fully independent Private-DNS check. Do not hide a system-DNS fallback from the user.

---

## 5. OkHttp target-check exception

The old rule requiring `network.openConnection(URL(...))` for target checks is superseded for custom-DNS checks.

OkHttp is explicitly allowed for target checks because it can combine:

- `cellular Network.socketFactory`;
- a custom `okhttp3.Dns` implementation;
- normal hostname-based HTTPS URLs.

This preserves standard TLS behavior, including hostname verification, certificate validation, SNI, and redirects while keeping sockets on the requested cellular network.

Forbidden:

```text
trustAllCertificates()
hostnameVerifier { _, _ -> true }
https://IP with disabled certificate/hostname checks
custom permissive TrustManager
```

Do not weaken TLS to make custom DNS easier.

---

## 6. Private DNS and VPN

Private DNS and VPN are different concerns.

### Private DNS

The main WhiteListChecker check uses its own configured DNS resolver and must not depend on Android Private DNS.

Private DNS data is diagnostic only:

- active/inactive;
- server hostname when Android exposes it;
- whether custom DNS was used.

Do not:

- change the Android Private DNS setting;
- request `WRITE_SETTINGS`;
- route custom DNS back through system DNS.

### VPN

Do not claim that custom DNS guarantees bypass of an arbitrary VPN.

VPN bypass depends on Android and the VPN application's policy. Do not add a VPN service, proxy, root requirement, or process-wide binding to solve this milestone.

---

## 7. Site and DNS classification

Sites remain the primary source of the final whitelist state.

DNS is a secondary independent signal and must not create a false `WHITELIST_ON` merely because public foreign DNS is blocked.

Current DNS signal model:

```text
UNKNOWN
WHITELIST_LIKE
NORMAL
NO_DNS_ACCESS
PARTIAL
```

Expected combined behavior:

```text
SITE whitelist-like + DNS whitelist-like -> WHITELIST_ON
SITE normal         + DNS normal          -> WHITELIST_OFF
SITE whitelist-like + DNS normal          -> PARTIAL_PROBLEM
SITE normal         + DNS whitelist-like  -> PARTIAL_PROBLEM
SITE normal         + DNS unavailable     -> keep clear site result
```

For inconclusive site results, DNS may appear in diagnostics but must not independently promote the result to confident `WHITELIST_ON`.

Keep the existing state model unless a task explicitly changes it:

```kotlin
enum class WhitelistState {
    UNKNOWN,
    WHITELIST_OFF,
    WHITELIST_ON,
    NO_MOBILE_INTERNET,
    MOBILE_DNS_FAILURE,
    PARTIAL_PROBLEM,
    CELLULAR_NETWORK_UNAVAILABLE,
}
```

---

## 8. Target lists and settings persistence

Website targets and DNS servers are separate responsibilities.

Rules:

- keep the existing website DataStore keys unchanged;
- keep DNS JSON separate from target-site JSON;
- DNS settings must survive restarts;
- reset sites must not reset DNS;
- reset DNS must not reset sites;
- built-in DNS IDs must be stable;
- future built-ins may be migrated in without reviving a built-in the user explicitly removed;
- custom DNS additions must reject duplicate endpoint + port + protocol combinations.

Do not turn `CheckTargetsRepository` into a universal settings manager.

---

## 9. Public service and privacy

DNS diagnostics introduced for local checks are local by default.

Do not extend `PublicReport` or the central Worker contract merely because `NetworkCheckResult` gained DNS fields.

If public DNS reporting is ever required, it must be a separate deliberate product/privacy change with backward-compatible API handling.

Telegram bot tokens and Worker secrets must never be stored in source control or included in diagnostics.

The personal Telegram relay and central public service remain separate systems.

---

## 10. Notifications and state confirmation

Keep state-change detection separate from notification delivery.

A suspicious state is not automatically a notification event. Existing debounce/confirmation rules remain authoritative.

Do not place notification-sending logic inside classifiers or DNS components.

Network check components return network results; notification components decide whether/how to notify.

---

## 11. Tests required for DNS changes

Do not use real public DNS endpoints in unit tests.

Use fake/abstracted transports and deterministic responses.

DNS changes should cover, as applicable:

- default FOREIGN and LOCAL groups;
- stable/unique built-in IDs and endpoints;
- JSON round-trip and corrupt input;
- backward-compatible optional-field defaults;
- DNS signal thresholds;
- site/DNS conflict behavior;
- resolver fallback;
- malformed DNS packets;
- NXDOMAIN/SERVFAIL handling;
- truncated UDP -> TCP fallback;
- single-run cache;
- disabled resolvers;
- cellular Network propagation through DNS and site checking;
- callback release;
- Private DNS diagnostic flags;
- no DNS-only false-positive `WHITELIST_ON`.

---

## 12. Build, test, and device verification

After an Android milestone, run when the environment permits:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
adb devices
```

On Unix:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
adb devices
```

If exactly one authorized device is connected and the build succeeds:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Never claim a build, test, APK installation, or manual phone test succeeded unless the command actually ran and succeeded.

If no device is available, report that fact rather than inventing an ADB status.

If uninstalling would be required because of a signature mismatch, warn before removing app data.

---

## 13. Required implementation report

For a completed milestone, report in Russian:

```text
Версия:
Статус сборки:
Статус тестов:
APK установлен:
Устройство adb:

Что реализовано:
- ...

Архитектура DNS:
- ...

Как обеспечена независимость от Private DNS:
- ...

Как DNS участвуют в классификации:
- ...

Какие файлы добавлены:
- ...

Какие файлы изменены:
- ...

Какие тесты добавлены:
- ...

Что протестировать вручную:
1. ...

Известные ограничения:
- ...
```

Do not report only «готово».

---

## 14. General engineering rules

1. One version = one clear goal.
2. Do not implement unrelated ideas “along the way”.
3. Review the diff before accepting AI-generated changes.
4. Keep user-visible strings in resources.
5. Prefer typed errors/status models over business logic based on error text.
6. Inject infrastructure dependencies instead of constructing them inside business logic or Composables.
7. Preserve existing user data and backward compatibility unless a task explicitly requires a migration.
8. Do not change vendor/generated code directly.
9. Do not add abstractions merely for future possibilities.
10. If a rule must be broken for correctness, document the reason in the same version.
