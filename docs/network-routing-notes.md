# Mobile routing, VPN и Private DNS

## Что приложение уже делает

WhiteListChecker не проверяет сайты через обычный `URL.openConnection()` и не берёт `activeNetwork` как маршрут проверки.

Фактический путь:

```text
ConnectivityManager.requestNetwork(TRANSPORT_CELLULAR + NET_CAPABILITY_INTERNET)
    -> Network
    -> network.openConnection(URL(...))
    -> FOREIGN/LOCAL targets
```

По документации Android, `Network.openConnection(URL)` открывает URL на конкретном `Network`, и весь трафик такого соединения идёт через этот `Network`. `Network.getAllByName(...)` также резолвит имя именно на этом `Network`.

Источники:

- Android `Network`: https://developer.android.com/reference/android/net/Network
- Android `ConnectivityManager.requestNetwork`: https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,%20android.net.ConnectivityManager.NetworkCallback,%20int)

## Что это означает для Wi-Fi

Если активная сеть телефона — Wi-Fi, приложение всё равно запрашивает отдельную cellular-сеть и выполняет проверки через полученный `Network`.

В UI:

- активная сеть может быть `Wi-Fi`;
- проверяемая сеть должна быть `Mobile`.

## VPN: честное ограничение Android

Полностью гарантировать обход любого VPN обычное приложение не может.

Причина: Android `VpnService.Builder.allowBypass()` описывает, что по умолчанию трафик приложений направляется через VPN-интерфейс, и приложения не могут обойти VPN. Обход возможен только если сам VPN разрешил bypass через `allowBypass()`.

Источник:

- Android `VpnService.Builder.allowBypass`: https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()

Практический вывод:

- если VPN разрешает bypass, проверка через cellular `Network` может идти мимо VPN;
- если VPN не разрешает bypass, приложение не должно обещать обход VPN;
- приложение не должно использовать VPN service, foreground service или собственный proxy ради обхода.

Коротко для UI/релизных заметок:

```text
Wi-Fi приложение обходит для проверки: да, через cellular Network.
VPN приложение гарантированно не обходит: зависит от VPN и allowBypass().
Private DNS приложение гарантированно не обходит: DNS остаётся политикой Android/сети.
```

## Private DNS и DNS-серверы

Приложение может направить DNS-resolution на выбранный Android `Network`, но не должно обещать обход всех системных DNS-политик.

Практический вывод:

- `Network.openConnection(...)` и network-bound DNS уменьшают риск проверки через Wi-Fi/DNS активной сети;
- Android Private DNS, прошивка, операторский APN, корпоративный профиль или VPN могут влиять на резолвинг;
- если большинство доменов не резолвится через cellular `Network`, приложение классифицирует это как `MOBILE_DNS_FAILURE`, а не как включённые белые списки.

## Что не добавлять

Не добавлять ради обхода VPN/DNS:

- `WRITE_SETTINGS`;
- собственный VPN/proxy;
- foreground service;
- прямой `bindProcessToNetwork(...)` для всего приложения;
- прямой Telegram API fallback.
