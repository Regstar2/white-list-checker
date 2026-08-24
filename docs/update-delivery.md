# Update Delivery — WhiteListChecker 1.0

## Источник версии

Установленная версия берётся из Android `BuildConfig.VERSION_NAME`. Это тот же version source, который показывается на экране «О приложении».

## Доверенный источник релизов

Проверка выполняется только через публичный GitHub Releases API репозитория `Regstar2/white-list-checker`.

Приложение:

- не использует GitHub PAT/OAuth token;
- не отправляет пользовательские данные при update check;
- не принимает произвольный download URL из ответа API;
- самостоятельно строит переход только на официальный `github.com/Regstar2/white-list-checker/releases/tag/<tag>`.

Неавторизованный GitHub API имеет rate limits. Их превышение обрабатывается как контролируемая ошибка и не влияет на checker, историю или запуск приложения.

## Проверка при запуске

`AppUpdateViewModel` запускает update check асинхронно после создания UI lifecycle. HTTP-запрос выполняется на `Dispatchers.IO` и не блокирует запуск приложения.

Если фоновая проверка завершается ошибкой, приложение продолжает работу и не показывает interrupting error dialog. Пользователь может повторить проверку вручную на экране «О приложении».

## Stable и prerelease

Версии сравниваются по SemVer.

Политика каналов:

- stable installed build (`1.0.0`) рассматривает только stable GitHub releases;
- release с `prerelease=true` не предлагается stable-пользователю;
- tag с SemVer prerelease suffix (`-alpha`, `-beta`, `-rc` и т.п.) также не предлагается stable-пользователю, даже если GitHub metadata ошибочно помечает его как stable;
- prerelease installed build может получать более новый prerelease или stable release;
- draft releases игнорируются.

## Пользовательский сценарий

При наличии обновления приложение показывает номер новой и установленной версии.

Пользователь может:

- открыть официальный GitHub Release;
- выбрать «Позже» и продолжить работу;
- повторно запустить «Проверить обновления» на экране «О приложении»;
- прочитать краткие release notes в приложении или открыть полную страницу релиза.

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

GitHub API вызывается обычным OkHttp-клиентом через default network policy Android. Поэтому update-check не меняет и не загрязняет измеряемый cellular transport.

Project-specific правило остаётся неизменным: proxy/VPN transport не добавляется в основной checker path.

## Ошибки

Ручная проверка различает:

- GitHub/network недоступен;
- HTTP error;
- GitHub rate limit;
- некорректный JSON response;
- некорректный installed version.

Эти ошибки не повреждают настройки/данные и не влияют на проверки мобильной сети.
