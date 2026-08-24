# WhiteListChecker v1.0 — current architecture

## Product boundary

WhiteListChecker is an Android-first, local diagnostic application. Core checks, history, statistics, background monitoring, and local notifications do not depend on infrastructure operated by the project owner.

The central/public service present in older versions was removed in v1.0. Historical release notes may still describe it.

## Runtime components

```text
Compose UI
   |
MainViewModel
   |
   +-- WhitelistCheckUseCase
   |      +-- CellularNetworkProvider
   |      +-- DNS probes / CellularDnsResolver
   |      +-- MobileSiteChecker
   |      +-- WhitelistStateClassifier
   |
   +-- CheckAndNotifyUseCase
   |      +-- local history / statistics / timeline
   |      +-- local Android notifications
   |      +-- optional personal Telegram relay
   |
   +-- WorkManager background checks
   +-- ActiveMonitoringService
```

## Cellular network isolation

The app explicitly acquires a cellular `Network` through `ConnectivityManager.requestNetwork(...)`. DNS sockets and target HTTP(S) connections are created through that network. The app does not use process-wide `bindProcessToNetwork`.

Custom DNS is preferred. UDP/53 failures that represent transport timeout/network failures can fall back to TCP/53. If configured public resolvers cannot resolve a target, hostname resolution may fall back to `Network.getAllByName(...)` on the acquired cellular network.

Site checks are the primary classification signal. DNS availability is secondary diagnostics and the failure of one or more public resolvers does not by itself produce `MOBILE_DNS_FAILURE`.

## Local persistence

Room stores:

- check history and per-target results;
- derived statistics;
- whitelist timeline;
- the queue for personal Telegram notifications.

DataStore/shared preferences store application and monitoring settings.

Schema v8 removes the obsolete `pending_public_reports` table through migration `7 -> 8`. The migration does not delete local history, statistics, targets, DNS configuration, or personal Telegram settings.

## Telegram integration

Only the personal Telegram flow remains:

```text
Android -> user-owned Cloudflare Worker -> user's Telegram bot
```

The user supplies the Worker URL and Relay Secret. The Worker is optional and is not operated by the WhiteListChecker project.

Active monitoring may listen for commands through this personal Worker when the user enables Telegram commands.

## Removed in v1.0

- project-owned central Cloudflare Worker;
- public Telegram bot;
- device registration/linking with a central service;
- aggregate public report upload and queue;
- central remote-command loop;
- hardcoded `PUBLIC_SERVICE_BASE_URL`;
- approximate-location permission used for shared-service region detection.
