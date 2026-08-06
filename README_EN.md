<div align="center">

# WhiteListChecker

An Android application for checking mobile-network availability and detecting signs of allowlist-only network access. Checks are routed through the cellular connection even when Wi-Fi remains the phone's active network.

[Русский](README.md) · **English**

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Quick start](#quick-start) ·
[Documentation](#documentation) ·
[Releases](../../releases)

</div>

---

## About

WhiteListChecker checks local and external websites through the cellular network, classifies the result, and stores observation history. It is intended for manual diagnostics, periodic checks, and notifications when the observed network state changes.

The project detects observable network behavior only. It has no access to an operator's internal rules and cannot prove allowlist mode with complete certainty.

## Project status

The project is in **MVP / beta**. The current development line is `0.8.x`; the Android `versionName` stored in the repository is `0.8.15`.

| Area | Status |
|---|---|
| Manual cellular-network check | Beta |
| Background checks with WorkManager | Beta |
| Active monitoring with a foreground service | Beta |
| Local and personal Telegram notifications | Beta |
| Central public service and bot | Beta |
| Remote check of a linked device | Experimental |

## Features

- explicit cellular routing through `ConnectivityManager` and `Network.openConnection()`;
- `FOREIGN` and `LOCAL` target groups with an editable target list;
- classification of availability, DNS failures, partial outages, and allowlist signs;
- state-change confirmation using consecutive checks;
- history, statistics, and diagnostic reports;
- local Android notifications;
- periodic checks through WorkManager;
- active monitoring through a foreground service;
- personal Telegram notifications through a user-owned Cloudflare Worker relay;
- a central Cloudflare Worker with a public Telegram bot;
- optional submission of anonymized results for aggregate statistics;
- one-time-code linking between a Telegram chat and a device;
- remote check commands while active monitoring is running on the device.

## Quick start

1. Install Android Studio with JDK 17 and Android SDK 35.
2. Copy `local.properties.example` to `local.properties` and configure `sdk.dir`.
3. Build the debug APK:

```powershell
.\gradlew.bat assembleDebug
```

4. Install it on a connected Android device:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

5. Open the application and select **Check mobile network**.

Published builds are available under [GitHub Releases](../../releases). They may lag behind the current development branch.

## Requirements

- Android 8.0 or later (`minSdk 26`);
- JDK 17 or later for building;
- Android SDK Platform 35 and Build-Tools 35.x;
- an active cellular connection for the primary use case;
- Git and ADB for local development and installation;
- Node.js and npm for central Cloudflare Worker development.

## Installation

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it over an existing debug build:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Usage

### Manual check

1. Keep mobile data enabled.
2. Wi-Fi may remain enabled because the application requests the cellular network separately.
3. Start a check from the home screen.
4. Open the detailed report when some targets fail or the result is ambiguous.

### Background and active checks

- WorkManager performs approximate periodic checks with Android's 15-minute minimum interval.
- Active monitoring uses a foreground service and a persistent notification.
- Android may restrict or stop the foreground service, especially under aggressive battery management.

### Public service and Telegram bot

Consent for anonymized report submission and consent for remote checks are independent and disabled by default.

To use remote checks:

1. enable remote checks on the public-service screen;
2. save the settings;
3. create a link code and send `/link <code>` to the public bot;
4. start active monitoring;
5. request a check from the linked-device screen in the bot.

## Architecture

```text
Android UI
   │
   ▼
ViewModel / Use cases
   │
   ├── Cellular network checker
   ├── DataStore / Room
   ├── WorkManager / Foreground service
   ├── User-owned Telegram relay Worker
   └── Central public-service Worker
            │
            ├── D1
            └── Public Telegram bot
```

The Android application uses Kotlin, Jetpack Compose, Material 3, Coroutines/Flow, DataStore, Room, WorkManager, and OkHttp. The central service is located under `cloudflare/public-service/` and is implemented as a Cloudflare Worker backed by D1.

## Security

- Telegram bot tokens and Worker secrets must not be stored in Android code or Git.
- The central-service device token is stored on the device using Android Keystore-backed encryption.
- The central Worker stores a device-token hash rather than the original token.
- Do not publish `local.properties`, release keystores, tokens, passwords, or diagnostic logs containing secrets.

See [SECURITY.md](SECURITY.md).

## Privacy

Public-service data submission is disabled by default.

When enabled, the service receives the selected region, optional city, mobile operator, application version, check time, resulting state, and aggregate target results.

It does not receive coordinates, an exact address, phone number, IMEI, IMSI, SIM serial, Wi-Fi SSID/BSSID, device contents, personal Telegram messages, bot tokens, or Relay Secrets.

See [docs/privacy/public-data-sharing.md](docs/privacy/public-data-sharing.md).

## Troubleshooting

### Active monitoring reports `Worker HTTP 404`

The active-monitoring route should return `405`, not `404`, for a diagnostic GET request. Run the production verification command from the Worker directory:

```powershell
cd cloudflare\public-service
npm run verify:production
```

When the command reports a legacy revision or a missing `service-sync` capability, deploy the current Worker version:

```powershell
npm run deploy
```

After deployment, `/health` must return JSON containing a `revision` field and the `service-sync` capability.

### ADB is not found

Add the Android SDK `platform-tools` directory to `PATH`, or invoke `adb.exe` with its full path.

Additional scenarios are covered by [docs/testing/public-service-manual-test-plan.md](docs/testing/public-service-manual-test-plan.md).

## Development

Read [AGENTS.md](AGENTS.md) before making changes. Architecture and product documentation are stored under `docs/`.

Main directories:

```text
app/                         Android application
cloudflare/public-service/   central Worker and public bot
docs/                        architecture, test plans, and version notes
```

## Build

Android:

```powershell
.\gradlew.bat assembleDebug
```

Central Worker dry-run without publishing:

```powershell
cd cloudflare\public-service
npm ci
npm run build
```

## Testing

Android project commands:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

Worker commands:

```powershell
cd cloudflare\public-service
npm ci
npm run typecheck
npm run lint
npm test
npm run build
```

Production Worker verification:

```powershell
npm run verify:production
```

## Documentation

| Task | Document |
|---|---|
| Current MVP | [docs/WhiteListChecker - current MVP.md](docs/WhiteListChecker%20-%20current%20MVP.md) |
| Technology stack | [docs/stack.md](docs/stack.md) |
| Central service | [docs/architecture/central-public-service.md](docs/architecture/central-public-service.md) |
| Remote commands | [docs/architecture/remote-command-flow.md](docs/architecture/remote-command-flow.md) |
| Personal Telegram relay | [docs/cloudflare-worker/README.md](docs/cloudflare-worker/README.md) |
| Public-service deployment | [docs/cloudflare-public-service/README.md](docs/cloudflare-public-service/README.md) |
| Manual test plan | [docs/testing/public-service-manual-test-plan.md](docs/testing/public-service-manual-test-plan.md) |
| Change history | [CHANGELOG.md](CHANGELOG.md) |
| Development rules | [AGENTS.md](AGENTS.md) |

## Limitations

- Classification is based on observed results and may produce false positives.
- Results depend on the operator, region, Android firmware, VPN, Private DNS, and the current state of target websites.
- WorkManager does not guarantee exact execution times.
- Android may restrict or stop a foreground service.
- Remote checks require a current production Worker, stored consent, and active monitoring running on the device.
- Aggregate statistics depend on fresh reports and are not official operator information.

## License

This project is distributed under the [MIT](LICENSE) license.
