# Политика конфиденциальности WhiteListChecker

Дата вступления в силу: 1 сентября 2026 года.

WhiteListChecker — Android-приложение для локальной диагностики доступности ресурсов через мобильную сеть. Основные результаты проверок, история и статистика хранятся на устройстве пользователя.

## Реклама

Магазинные сборки WhiteListChecker могут использовать Yandex Mobile Ads для показа рекламы.

Приложение:

- не передаёт рекламному SDK объект Android `Network`, который отдельно получает для диагностической cellular-проверки;
- не привязывает рекламный трафик к измеряемому checker path;
- явно отключает передачу геолокации в Yandex Mobile Ads через `YandexAds.setLocationTracking(false)`;
- передаёт в SDK выбранное пользователем состояние согласия через `YandexAds.setUserConsent(...)`;
- не делает просмотр рекламы условием доступа к проверке сети.

Согласно документации Yandex Mobile Ads, SDK может обрабатывать технические данные устройства и рекламные/другие идентификаторы, если они доступны на устройстве и разрешены системой. Эти данные используются рекламной системой для показа, измерения и защиты рекламы. Передача данных SDK выполняется по защищённому сетевому соединению.

При выборе ограниченного режима WhiteListChecker передаёт `userConsent = false`. Конкретная доступность и состав рекламы при таком состоянии определяются Yandex Mobile Ads и применимыми требованиями региона.

## Основная диагностика

WhiteListChecker получает отдельный cellular `Network` через Android API только для выполнения сетевой диагностики. Рекламный SDK не получает этот объект от приложения и использует обычную сетевую политику Android.

Результаты диагностических проверок, локальная история и статистика не отправляются в Yandex Mobile Ads.

## Дополнительные функции

Проверка обновлений и открытие ссылок используют обычную сетевую политику Android.

Личный Telegram является необязательной функцией. Если пользователь включает её, явно отправляемые данные проходят через настроенный самим пользователем Worker и Telegram согласно настройкам пользователя и политикам этих сервисов.

## Управление согласием

При первом запуске сборки с включённой рекламой приложение предлагает выбрать персонализированную рекламу или ограниченный режим. Выбор хранится локально на устройстве и может быть изменён в разделе «Настройки» → «Реклама и конфиденциальность».

## Контакты

Вопросы по приложению и конфиденциальности можно направлять через Issues публичного репозитория `Regstar2/white-list-checker` на GitHub.

---

# WhiteListChecker Privacy Policy

Effective date: September 1, 2026.

WhiteListChecker is an Android application for local diagnostics of resource availability over a mobile network. Core check results, history, and statistics are stored on the user's device.

## Advertising

Store builds of WhiteListChecker may use Yandex Mobile Ads.

The application:

- does not pass the Android `Network` object obtained for cellular diagnostics to the advertising SDK;
- does not bind advertising traffic to the checker path being measured;
- explicitly disables location tracking in Yandex Mobile Ads with `YandexAds.setLocationTracking(false)`;
- passes the user's consent choice to the SDK with `YandexAds.setUserConsent(...)`;
- never requires viewing an advertisement to use the network checker.

According to Yandex Mobile Ads documentation, the SDK may process technical device data and advertising/other identifiers when they are available and permitted by the operating system. The advertising system uses this information for ad delivery, measurement, and protection. SDK data is transferred over encrypted network connections.

When limited mode is selected, WhiteListChecker passes `userConsent = false`. The exact availability and composition of advertising in this state is controlled by Yandex Mobile Ads and applicable regional requirements.

## Core diagnostics

WhiteListChecker obtains a separate cellular `Network` through Android APIs only for network diagnostics. The advertising SDK is not given this object by the application and uses Android's normal network policy.

Diagnostic results, local history, and statistics are not sent to Yandex Mobile Ads.

## Optional features

Update checks and external links use Android's normal network policy.

Personal Telegram integration is optional. If enabled, data explicitly submitted by the user passes through the user's own configured Worker and Telegram according to the user's settings and those services' policies.

## Consent controls

On first launch of a build with advertising enabled, the application offers a choice between personalized advertising and limited mode. The choice is stored locally and can be changed under Settings → Ads and privacy.

## Contact

Questions about the application or privacy can be submitted through GitHub Issues in the public `Regstar2/white-list-checker` repository.
