# Public Report Contract

## Endpoint

```text
POST /api/v1/reports
Authorization: Bearer <device-token>
```

## Request

```json
{
  "schemaVersion": 1,
  "requestId": "uuid",
  "reportId": "uuid",
  "checkedAt": 1710000000000,
  "triggerType": "MANUAL",
  "appVersion": "0.8.15",
  "regionCode": "RU-RYA",
  "cityCode": "RU-RYA-RYAZAN",
  "customCityName": null,
  "areaSource": "AUTOMATIC_LOCATION",
  "operatorCode": "MEGAFON",
  "operatorSource": "NETWORK_OPERATOR",
  "whitelistState": "LIKELY_ENABLED",
  "resultQuality": "CONCLUSIVE",
  "foreign": {
    "available": 0,
    "total": 8
  },
  "local": {
    "available": 7,
    "total": 8
  },
  "targets": []
}
```

## Validation

Worker принимает report только если:

- installation авторизована Bearer token;
- installation не revoked;
- `shareReports=true`;
- `schemaVersion` поддерживается;
- `reportId` ранее не принимался;
- `checkedAt` не слишком старый и не из будущего;
- `regionCode`, `cityCode`, `operatorCode`, `areaSource`, `operatorSource`, `triggerType`, `whitelistState`, `resultQuality` допустимы;
- `available >= 0`, `total >= 0`, `available <= total`;
- payload не превышает лимит размера.

Если `shareReports=false`, возвращается `403 REPORT_SHARING_DISABLED`.

## Допустимые состояния

Для публичного consensus голосуют только:

- `LIKELY_ENABLED`;
- `LIKELY_DISABLED`.

Состояния:

- `PARTIAL_PROBLEM`;
- `MOBILE_DNS_FAILURE`;
- технические ошибки;
- отсутствие мобильной сети;
- недоступность cellular Network

не превращаются в голос за включённые или выключенные белые списки. Они могут отображаться как неоднозначные данные.

## Idempotency

`reportId` уникален. Повторная отправка того же `reportId` считается успешной, но не изменяет ранее принятый report и не создаёт второй голос.

## Privacy

Публичные API и Telegram-ответы не возвращают:

- `installationId`;
- `chatId`;
- device token;
- token hash;
- IP;
- точные координаты;
- полный URL target с query;
- секреты личного relay.

Для target details действует privacy threshold; без достаточной выборки публичные target-агрегаты не раскрываются.

Для городской статистики действует отдельный threshold `MIN_UNIQUE_INSTALLATIONS_FOR_CITY`. Если `cityCode` отсутствует или данных по городу недостаточно, публичный статус должен работать на уровне региона.

Android не отправляет:

- latitude / longitude;
- raw address;
- street;
- postal code;
- cell ID;
- IMSI;
- IMEI;
- phone number;
- SIM serial;
- Wi-Fi SSID / BSSID.
