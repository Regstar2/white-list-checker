# Mobile routing, VPN и Private DNS

## Основной маршрут проверки с v0.9.0

WhiteListChecker не использует `activeNetwork` как маршрут проверки и не использует системный Android DNS для разрешения контрольных доменов.

Фактический путь одного check run:

```text
ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR + NET_CAPABILITY_INTERNET)
    -> cellular Network
    -> DNS probes через сокеты, привязанные к этому Network
    -> CellularDnsResolver
    -> доступные пользовательские DNS независимо от группы FOREIGN/LOCAL
    -> OkHttpClient(socketFactory = cellular Network.socketFactory, dns = CellularDnsResolver)
    -> HTTPS FOREIGN/LOCAL targets
    -> Site signal + DNS signal
    -> WhitelistStateClassifier
```

Один и тот же `Network` удерживается на DNS- и HTTPS-стадиях и освобождается после завершения check run.

Источники Android:

- `Network`: https://developer.android.com/reference/android/net/Network
- `ConnectivityManager.requestNetwork`: https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,%20android.net.ConnectivityManager.NetworkCallback,%20int)
- `LinkProperties`: https://developer.android.com/reference/android/net/LinkProperties

## Что это означает для Wi-Fi

Если активная сеть телефона — Wi-Fi, приложение всё равно запрашивает отдельную cellular-сеть.

Через неё идут:

- DNS probes;
- DNS resolution контрольных сайтов;
- HTTPS-проверки сайтов.

В UI и диагностике:

- активная сеть может быть `Wi-Fi`;
- проверяемая сеть должна быть `Mobile`.

## Custom DNS

Настроенные DNS выполняют две независимые роли.

### Resolver

`CellularDnsResolver` отправляет обычные DNS-запросы непосредственно на literal IP настроенного сервера.

Текущая реализация v0.9.0:

- UDP/53;
- TCP/53 fallback для truncated UDP response;
- A и AAAA для разрешения сайтов;
- transaction ID validation;
- typed errors для timeout, connection, malformed response, SERVFAIL, NXDOMAIN и network failure;
- небольшой in-memory cache только на время одного check run.

Системные `InetAddress.getByName(...)`, `Network.getAllByName(...)` и Android Private DNS в основном resolver path не используются.

### Диагностический сигнал

Каждый enabled DNS отдельно проверяется через cellular `Network`.

DNS делятся на:

- `FOREIGN` — внешние DNS;
- `LOCAL` — локальные DNS.

Группа DNS используется только для классификации доступности инфраструктуры. Она не ограничивает домены, которые этот DNS может разрешать.

Все доступные enabled DNS могут использоваться для разрешения любого контрольного сайта.

DNS-сигнал является вторичным. Недоступность внешних публичных DNS сама по себе не означает `WHITELIST_ON`.

## Private DNS

WhiteListChecker выполняет собственное разрешение доменов через настроенные DNS-серверы, привязанные к cellular `Network`, поэтому системный Android Private DNS не используется для основной проверки.

Диагностика сохраняет:

- активен ли Private DNS;
- hostname Private DNS, если Android его предоставляет;
- использовался ли custom DNS.

Эти признаки не меняют маршрут custom DNS.

Приложение:

- не меняет системную настройку Private DNS;
- не запрашивает `WRITE_SETTINGS`;
- не создаёт VPN или proxy.

### Приватность raw DNS

DNS/53 выбран как диагностически предсказуемый механизм, который не требует системного DNS для bootstrap самого resolver.

Следствие: запросы DNS/53 не шифруются. Это осознанный компромисс v0.9.0. Пользователь должен настраивать только доверенные DNS-серверы. Android рекомендует приложениям, реализующим собственный DNS при активном Private DNS, учитывать требования к защищённому DNS; в этой версии независимость диагностического маршрута от системного Private DNS является целевой особенностью WhiteListChecker.

## VPN: отдельное ограничение Android

Полностью гарантировать обход любого VPN обычное приложение не может.

`VpnService.Builder.allowBypass()` определяет, разрешает ли конкретный VPN приложениям обходить его маршрут.

Источник:

- Android `VpnService.Builder.allowBypass`: https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()

Практический вывод:

- если VPN разрешает bypass, проверка через cellular `Network` может идти мимо VPN;
- если VPN не разрешает bypass, приложение не должно обещать обход VPN;
- custom DNS не превращает произвольный VPN в обходящийся;
- Private DNS и VPN диагностируются как разные ограничения.

Коротко:

```text
Wi-Fi: проверки принудительно выполняются через запрошенный cellular Network.
Private DNS: основная проверка использует собственный DNS resolver и не зависит от системного Private DNS.
VPN: гарантированный bypass отсутствует и зависит от политики конкретного VPN.
```

## TLS и HTTPS

Target checks используют OkHttp только потому, что ему можно одновременно передать:

- `socketFactory` конкретного cellular `Network`;
- custom `Dns` implementation;
- исходный hostname URL для штатных SNI и hostname verification.

Запрещено:

- подключаться к `https://IP` с отключённой проверкой hostname;
- `trustAllCertificates()`;
- `hostnameVerifier { _, _ -> true }`;
- отключать certificate validation.

## Что не добавлять

Не добавлять ради обхода VPN/DNS:

- `WRITE_SETTINGS`;
- собственный VPN/proxy;
- `bindProcessToNetwork(...)` для всего приложения;
- root;
- изменение системного Private DNS;
- прямой Telegram API fallback.
