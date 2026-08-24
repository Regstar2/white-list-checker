<div align="center">

# WhiteListChecker

Android application for checking cellular-network availability and detecting observable signs of allowlist-only access. Checks use the cellular connection even when Wi‑Fi remains the phone's active network.

[Русский](README.md) · **English**

[![Android CI](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml/badge.svg)](https://github.com/Regstar2/WhiteListChecker/actions/workflows/android.yml)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

</div>

## About

WhiteListChecker checks local and external sites through an explicitly acquired cellular `Network`, uses DNS as an additional diagnostic signal, and stores results locally. It supports manual diagnostics, background checks, active monitoring, and state-change notifications.

Starting with `1.0.0`, WhiteListChecker **does not use a central public service**. The Android application has no project-owned shared Worker, public bot, aggregate user-report upload, device linking to a shared service, or remote commands through that service. Core functionality does not require infrastructure maintained by the project owner.

Personal Telegram notifications remain available through a Cloudflare Worker deployed and configured by the user.

WhiteListChecker observes network behavior only. It cannot access an operator's internal policy and therefore cannot prove allowlist mode with complete certainty.

## Features

- explicit cellular routing through `ConnectivityManager.requestNetwork(...)`;
- cellular checks while Wi‑Fi remains enabled;
- editable `FOREIGN` and `LOCAL` site groups;
- editable DNS resolver list;
- DNS over UDP/53 with TCP/53 fallback;
- hostname fallback through the acquired cellular `Network` without process-wide binding;
- HTTPS checks through OkHttp with normal TLS and hostname verification;
- site results as the primary classification signal and DNS as secondary diagnostics;
- local history and statistics stored in Room;
- local Android notifications;
- background checks through WorkManager;
- active monitoring through a foreground service;
- personal Telegram notifications and commands through a user-owned Worker;
- Russian and English UI.

## Quick start

Requires JDK 17, Android SDK 35, and Android 8.0+ (`minSdk 26`).

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

After launch, select **Check mobile network**. Wi‑Fi may remain enabled; cellular data must be available.

## DNS and Android Private DNS

WhiteListChecker first uses enabled user-configured DNS servers through the acquired cellular `Network`. Failure of an individual Cloudflare, Google, or Yandex resolver is not by itself treated as a broken cellular DNS path and must not override a clear site result.

When custom resolvers are unavailable, hostname resolution may fall back to `Network.getAllByName(...)` on the acquired cellular `Network`. Process-wide `bindProcessToNetwork` is not used.

Raw DNS traffic is currently unencrypted. Configure only trusted resolvers.

## Personal Telegram notifications

Telegram integration is user-owned and does not depend on WhiteListChecker infrastructure:

```text
Android app
   -> user-owned Cloudflare Worker relay
   -> user's Telegram bot
```

The app stores the URL of the user's Worker, Relay Secret, and recipients. There is no project-owned central Telegram bot in the runtime.

Worker documentation: [docs/cloudflare-worker/README.md](docs/cloudflare-worker/README.md).

## Architecture

```text
Android UI
   |
   v
ViewModel / use cases
   |
   +-- Cellular Network
   |      +-- DNS probes / resolver
   |      +-- HTTPS target checks
   +-- Room / DataStore
   +-- WorkManager / Foreground service
   +-- optional user-owned Telegram relay
```

The central Cloudflare Worker and `pending_public_reports` queue have been removed. Upgrading from `0.10.4` runs Room migration schema `7 -> 8`, dropping only the obsolete public-report table while preserving history, statistics, and other local data.

See [docs/architecture/current-architecture.md](docs/architecture/current-architecture.md) and [docs/network-routing-notes.md](docs/network-routing-notes.md).

## Privacy

Checks, history, and statistics remain on the device. WhiteListChecker no longer has a runtime path for submitting results to a central service.

When personal Telegram notifications are enabled, information explicitly sent to the user's bot passes through that user's Worker and Telegram. Bot tokens and Relay Secrets must not be committed to Git.

## Security

- TLS certificate and hostname verification remain enabled for target checks;
- the app contains no production URL for a central shared service;
- the app no longer requests approximate-location permission for shared-service region detection;
- personal Telegram Worker secrets are configured by the user;
- never publish `local.properties`, release keystores, tokens, passwords, or logs containing secrets.

See [SECURITY.md](SECURITY.md).

## Build and test

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

CI runs the same main checks for pull requests.

## Documentation

- [Current architecture](docs/architecture/current-architecture.md)
- [Network and DNS routing](docs/network-routing-notes.md)
- [Personal Telegram Cloudflare Worker](docs/cloudflare-worker/README.md)
- [SECURITY.md](SECURITY.md)
- [Change history](CHANGELOG.md)
- `docs/versions/` and release notes intentionally preserve historical descriptions of earlier versions, including the former public service.

## Limitations

- classification is based on observed results and can be wrong;
- VPN remains a separate Android networking limitation;
- blocking both UDP/53 and TCP/53 may make a particular custom DNS resolver unavailable;
- WorkManager does not guarantee exact execution times;
- Android may restrict foreground services;
- personal Telegram functionality requires the user's own Worker and Telegram bot.

## License

Distributed under the [MIT](LICENSE) license.
