# Update Delivery — WhiteListChecker 1.0

## Источник версии

Установленная версия берётся из Android `BuildConfig.VERSION_NAME`. Это тот же version source, который показывается на экране «О приложении».

## Доверенный источник релизов

Доверенный источник — только GitHub Releases репозитория `Regstar2/white-list-checker`.

Приложение:

- не использует GitHub PAT/OAuth token;
- не отправляет пользовательские данные при update check;
- не принимает произвольный download URL из ответа GitHub;
- самостоятельно строит переход только на официальный `github.com/Regstar2/white-list-checker/releases/tag/<tag>`.

## Rate-limit-safe стратегия

Stable и prerelease каналы используют разные read paths.

### Stable installed build

Для обычной stable-сборки (`1.0.0`) приложение открывает публичный GitHub URL:

```text
https://github.com/Regstar2/white-list-checker/releases/latest
```

GitHub перенаправляет его на последний опубликованный stable release. Приложение принимает результат только если финальный URL относится к `github.com/Regstar2/white-list-checker/releases/tag/<tag>`.

Такой stable-check не использует GitHub REST Releases API и не расходует его unauthenticated REST quota.

### Prerelease installed build

Для установленной prerelease-сборки нужен список beta/RC и stable releases, поэтому используется публичный GitHub REST API:

```text
https://api.github.com/repos/Regstar2/white-list-checker/releases?per_page=20
```

PAT/OAuth token в APK не добавляется. REST rate-limit остаётся контролируемым ограничением только prerelease-канала.

## Проверка при запуске

`AppUpdateViewModel` выполняет update check асинхронно и не блокирует запуск приложения.

Автоматическая проверка имеет persisted throttle:

```text
не чаще одного автоматического запроса за 24 часа
```

Время последней автоматической попытки хранится локально. Повторные перезапуски приложения внутри окна не создают новые update-запросы.

Одновременно может выполняться только один update check. Если автоматическая проверка уже идёт, ручное нажатие не создаёт второй параллельный HTTP-запрос.

Если фоновая проверка завершается ошибкой, приложение продолжает работу и не показывает interrupting error dialog. Пользователь может повторить проверку вручную на экране «О приложении».

## Stable и prerelease

Версии сравниваются по SemVer.

Политика каналов:

- stable installed build рассматривает только последний stable GitHub Release;
- prerelease GitHub Release не предлагается stable-пользователю;
- tag с SemVer prerelease suffix (`-alpha`, `-beta`, `-rc` и т.п.) также не предлагается stable-пользователю;
- prerelease installed build может получать более новый prerelease или stable release;
- draft releases игнорируются в REST prerelease path.

## Пользовательский сценарий

При наличии обновления приложение показывает номер новой и установленной версии.

Пользователь может:

- открыть официальный GitHub Release;
- выбрать «Позже» и продолжить работу;
- повторно запустить «Проверить обновления» на экране «О приложении»;
- прочитать краткие release notes, когда они доступны через выбранный source path, либо открыть полную официальную страницу релиза.

Update не является обязательным и не блокирует основной checker.

## Почему APK не устанавливается автоматически

WhiteListChecker не использует Play Store/in-app update framework и не запрашивает привилегии package installer для silent self-update.

Для `1.0.0` используется безопасный fallback:

```text
version check -> update available -> official GitHub Release -> user-controlled APK download/install
```

Приложение не скачивает APK в фоне и не пытается обходить системное подтверждение Android.

## Сетевая политика updater

Updater является отдельной интеграцией и не использует явно полученный `TRANSPORT_CELLULAR` checker'а.

GitHub вызывается отдельным OkHttp-клиентом через default network policy Android. Поэтому update-check не меняет и не загрязняет измеряемый cellular transport.

Project-specific правило остаётся неизменным: proxy/VPN transport не добавляется в основной checker path.

## Ошибки

Ручная проверка различает:

- GitHub/network недоступен;
- HTTP error;
- GitHub REST rate limit для prerelease path;
- некорректный response;
- некорректный installed version.

Эти ошибки не повреждают настройки/данные и не влияют на проверки мобильной сети.
