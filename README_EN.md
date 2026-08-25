<div align="center">

# WhiteListChecker

Android application for checking cellular-network availability and detecting observable signs of allowlist-only access. Checks run through an explicitly acquired cellular `Network`, even when Wi‑Fi remains the phone's active network.

[Русский](README.md) · **English**

[![Trusted CI](https://github.com/Regstar2/white-list-checker/actions/workflows/trusted-ci.yml/badge.svg)](https://github.com/Regstar2/white-list-checker/actions/workflows/trusted-ci.yml)
[![Android](https://img.shields.io/badge/Android-minSdk%2026-green)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

[Quick start](#quick-start) · [Documentation](#documentation) · [Releases](https://github.com/Regstar2/white-list-checker/releases)

</div>

## About

WhiteListChecker checks local and external sites through a separate cellular `Network`, uses DNS as an additional diagnostic signal, and stores results locally. Its main use case is measuring how the mobile network actually reaches control targets without rebinding the whole app process to cellular and without replacing the measured route with proxy/VPN transport.

The application observes network behavior only. It cannot access an operator's internal policy, so its result is not proof that an allowlist mode is enabled.

Starting with `1.0.0`, the central/public service is removed. Core functionality requires no Worker, bot, or other infrastructure maintained by the project owner. Personal Telegram remains optional and uses a Cloudflare Worker owned by the user.

## Project status

`1.0.0` is in release preparation. The local-first refactor, Room migration `7 -> 8`, and Update Delivery through the official GitHub Releases source are implemented. A physical debug upgrade from `0.10.4` to `1.0.0` without clearing app data preserved history and removed the shared-service UI.

Separate release tasks still remain:

- [#10 — in-app Feedback](https://github.com/Regstar2/white-list-checker/issues/10);
- [#11 — GitHub/release automation](https://github.com/Regstar2/white-list-checker/issues/11).

Update Delivery from #9 still requires the final physical RU/EN smoke test before tagging. The current `1.0.0` code is not an already published stable release until the release checklist is completed.

## Features

- explicit mobile `Network` acquisition through `ConnectivityManager.requestNetwork(...)`;
- cellular checks while Wi‑Fi remains enabled;
- editable `FOREIGN` and `LOCAL` site groups;
- editable DNS resolver list;
- DNS over UDP/53 with TCP/53 fallback;
- hostname fallback through `Network.getAllByName(...)` on the acquired cellular `Network`;
- HTTPS target checks through a network-bound socket factory with normal TLS/hostname verification;
- site results as the primary classification signal and DNS as secondary diagnostics;
- local history, statistics, and timeline stored in Room;
- statistics export to CSV/JSON/TXT;
- local Android notifications;
- background checks through WorkManager;
- active monitoring through a foreground service;
- personal Telegram notifications and commands through a user-owned Worker;
- asynchronous and manual update checks through the official GitHub Releases source;
- stable/prerelease filtering for update checks;
- Russian and English UI.

## Quick start

For the current source branch:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

After launch, select **Check mobile network**. Cellular data must be available; Wi‑Fi may remain enabled.

The public stable `1.0.0` APK will be distributed through GitHub Releases after the release checklist is complete.

## Requirements

- Android 8.0+ (`minSdk 26`);
- cellular data and an available cellular transport for the main checker;
- for source builds: JDK 17 and Android SDK 35;
- for personal Telegram: your own Telegram bot and user-owned Cloudflare Worker.

## Installation

For a development/debug build:

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

For the future stable release, use only an APK from the project's official GitHub Release. Do not uninstall the existing app before a compatible upgrade if you need to preserve local history and settings.

## Usage

1. Open the home screen and run a mobile-network check.
2. Adjust site and DNS lists under check settings when needed.
3. Use Statistics for state history and data export.
4. Use Diagnostics for route, DNS, and site details.
5. Configure local notifications, WorkManager, or active monitoring as needed.
6. Enable personal Telegram only after configuring your own Worker and bot.
7. Check for a new app version manually from the About screen when needed.

## Configuration

Users can configure:

- enabled LOCAL/FOREIGN sites;
- enabled DNS resolvers and custom DNS entries;
- theme and language;
- local notification policy;
- background check interval/policy;
- active monitoring options;
- personal Telegram Worker URL, Relay Secret, and recipients.

`BOT_TOKEN` belongs only in the user's Worker secrets and must not be stored in the Android application.

## Network and proxy

The main checker intentionally does not support proxy/VPN transport:

> Proxy support: N/A by project-specific design — WhiteListChecker measures the direct cellular-network route; a proxy/VPN changes the measured transport and can make diagnostics unreliable.

Therefore the checker path has no `System / Direct / Custom proxy` modes, HTTP proxy, or SOCKS5. This is a project-specific exception to the universal `NETWORK_PROXY_STANDARD.md`, not an unfinished checker feature.

A VPN/tunnel is treated as a measurement limitation: when it changes the traffic path, results may no longer describe the mobile operator's direct access.

Personal Telegram relay and the updater are separate integrations. The updater accesses GitHub through Android's normal default network policy and never receives the cellular `Network` used by the checker, so update checking does not change the measured route.

## Architecture

```text
Compose UI
   |
   +-- MainViewModel / checker use cases
   |      +-- Cellular Network
   |      |      +-- DNS probes / cellular resolver
   |      |      +-- HTTPS target checks
   |      |      +-- WhitelistStateClassifier
   |      +-- Room / DataStore
   |      +-- WorkManager / Foreground service
   |      +-- optional user-owned Telegram relay
   |
   +-- AppUpdateViewModel
          +-- CheckForAppUpdateUseCase
                 +-- public GitHub Releases API
```

Room schema `8` removes the obsolete `pending_public_reports` table through migration `7 -> 8` without destructive migration. History, statistics, and other local data are preserved.

See [current architecture](docs/architecture/current-architecture.md) and [technology stack](docs/architecture/tech-stack.md).

## Security

- TLS certificate and hostname verification remain enabled for target checks;
- there is no production URL for a central shared service;
- `BOT_TOKEN` is not stored in the APK;
- update checks contain no GitHub PAT/OAuth secret;
- release pages are constructed only from the official repository prefix;
- release keystores, `local.properties`, passwords, and secrets are excluded from Git;
- private governance and AI tool state are not published;
- personal Telegram secrets are user-provided and must not appear in issues or logs.

See [SECURITY.md](SECURITY.md).

## Privacy

Checks, history, and statistics remain on the device. There is no runtime path that uploads results to a central project service.

Update checks make a public request to GitHub Releases without a GitHub account/PAT and do not send check history, app settings, or other user data.

When personal Telegram is enabled, data explicitly sent to the user's bot passes through that user's Worker and Telegram. The exact content depends on the chosen action: test message, check report, or commands.

## Troubleshooting

If a result looks incorrect:

1. open Diagnostics;
2. compare the checked network with the active network;
3. inspect LOCAL/FOREIGN site and DNS results separately;
4. verify that one unavailable resolver is not interpreted as a global DNS failure;
5. repeat the check with Wi‑Fi enabled and disabled;
6. disable VPN/tunnel when measuring the direct cellular route.

The full physical test flow is in [docs/testing/manual-test-plan.md](docs/testing/manual-test-plan.md).

## Updating

WhiteListChecker uses `BuildConfig.VERSION_NAME` as the installed version and checks public releases only in the official `Regstar2/white-list-checker` repository.

Checks run:

- asynchronously at startup without blocking the main UI;
- manually through **About → Check for updates**.

For a stable build, prereleases (`alpha`, `beta`, `rc`, and GitHub `prerelease=true`) are not offered as normal stable updates. When a newer version is found, the app displays the version number and short release notes. The user can open the official GitHub Release or select **Later**.

The app does not download or install APKs automatically. Installation remains under Android and user control. A compatibly signed APK can be installed over the existing version through the Android package installer or with:

```powershell
adb install -r path\to\WhiteListChecker.apk
```

The new APK certificate must match the installed version. `adb uninstall` removes local app data and must not be used to validate the migration path.

See [docs/update-delivery.md](docs/update-delivery.md) for the complete policy.

## Build

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Release signing is supplied only through local Gradle properties/environment/local properties using `WL_RELEASE_*`; the keystore and credentials are not stored in Git.

## Testing

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Update Delivery has unit tests for SemVer, stable/prerelease selection, error mapping, and GitHub response parsing. Cellular routing, DNS, migration, and the final update UI still require a physical Android device. See [docs/testing/manual-test-plan.md](docs/testing/manual-test-plan.md).

## Documentation

- [Version 1.0 scope](docs/product/mvp-scope.md)
- [Current architecture](docs/architecture/current-architecture.md)
- [Technology stack](docs/architecture/tech-stack.md)
- [Network and DNS routing](docs/network-routing-notes.md)
- [Update Delivery](docs/update-delivery.md)
- [Personal Telegram Cloudflare Worker](docs/cloudflare-worker/README.md)
- [Manual test plan](docs/testing/manual-test-plan.md)
- [sheduler standards compliance](docs/release/standards-compliance.md)
- [1.0.0 release checklist](docs/release/release-checklist.md)
- [Security policy](SECURITY.md)
- [Change history](CHANGELOG.md)

`docs/versions/`, `docs/releases/`, and `docs/archive/` preserve the factual history of earlier versions and may describe features that no longer exist.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR. Private governance files and local AI/tool state must not be committed to the public repository.

## Feedback

Until the in-app feedback flow is available, bugs and feature proposals can be submitted through [GitHub Issues](https://github.com/Regstar2/white-list-checker/issues). Do not attach tokens, Relay Secrets, `chat_id`, private logs, or other sensitive data.

The in-app path is tracked separately in [Issue #10](https://github.com/Regstar2/white-list-checker/issues/10).

## Limitations

- classification is based on observed results and may be wrong;
- proxy/VPN transport is intentionally unsupported in the checker path;
- an enabled VPN/tunnel can make results unrepresentative of the direct cellular network;
- raw DNS/53 is unencrypted;
- blocking both UDP/53 and TCP/53 may make a particular custom resolver unavailable;
- the public GitHub API may temporarily rate-limit update checks;
- the app does not perform silent/background APK installation;
- WorkManager does not guarantee exact execution times;
- Android may restrict foreground services;
- personal Telegram requires the user's own Worker and Telegram bot;
- stable `1.0.0` has not been published yet.

## License

Distributed under the [MIT](LICENSE) license.