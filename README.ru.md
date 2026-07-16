# Domain Manager

[English version](README.md)

Android-приложение для управления доменной маршрутизацией на роутере OpenWrt —
добавление и удаление доменов в списках [mihomo](https://github.com/MetaCubeX/mihomo)
и [MagiTrickle](https://github.com/MagiTrickle/MagiTrickle), а также управление
службами роутера прямо с телефона. Заменяет браузерное расширение нативным
Share-Target-флоу: поделитесь ссылкой из любого браузера — и домен окажется на роутере.

## Возможности

- **Домены** — главный экран:
  - Полный список управляемых доменов с двумя независимыми индикаторами по системам
    (mihomo / MagiTrickle) — домен может состоять только в одной из них
  - Добавление вручную, вставкой из буфера (URL автоматически сворачивается до
    registrable-домена) или через «Поделиться» из любого браузера
  - Удаление с явным выбором цели, если состояние систем расходится
  - Долгий тап — переименование домена (членство в системах сохраняется)
  - Pull-to-refresh и автообновление при возврате на экран
- **Статус** — управление службами роутера:
  - mihomo, MagiTrickle, zapret, zapret2 с живыми флагами *работает* и *автозапуск*
  - Контекстные кнопки Запустить / Остановить / Перезапустить
  - Цветовая индикация: зелёный — работает, красный — должна работать, но не запущена,
    серый — остановлена
  - Учтена взаимоисключаемость zapret ⇄ zapret2 (запуск одного выключает второй)
- **Настройки** — иерархические, в два уровня:
  - Внешний вид: живое превью, Системная/Светлая/Тёмная тема, динамические цвета
    Material You (Android 12+) или полноценная палитра Material 3 из фирменного
    цвета `#D2DA40`
  - Язык: системный / English / Русский, переключается на лету
  - Авторизация: хост, порт и токен роутера (хранятся в `EncryptedSharedPreferences`)
- Анимированный сплэш (две половинки логотипа «стыкуются» со вспышкой соединения),
  предиктивный жест «назад», Material 3 Expressive, плитка быстрых настроек.

## Требования

- Android 10+ (minSdk 29)
- Роутер OpenWrt с CGI-скриптом `domain-api` по адресу
  `http://<роутер>:<порт>/cgi-bin/domain-api` (доступ по токену, только LAN, обычный HTTP)

### API роутера, с которым работает приложение

| Запрос | Ответ |
|---|---|
| `?token=…&action=add\|remove\|status&domain=example.com&target=both\|mihomo\|magitrickle` | `{"domain":"…","mihomo":bool,"magitrickle":bool}` |
| `?token=…&action=list` | `{"domains":[{"domain":"…","mihomo":bool,"magitrickle":bool},…]}` |
| `?token=…&action=svc_list` | `{"services":[{"service":"…","running":bool,"enabled":bool},…]}` |
| `?token=…&action=svc_status\|svc_start\|svc_stop\|svc_restart&service=mihomo\|magitrickle\|zapret\|zapret2` | `{"service":"…","running":bool,"enabled":bool}` |

Ошибки: `400 {"error":"bad_domain"|"bad_action"|"bad_target"|"bad_service"|"empty_domain"}`,
`403 {"error":"bad_token"}`, `500 {"error":"token_not_configured"}`.

> **Замечание для Android 16+:** если «Проверить подключение» на свежей установке
> отваливается по таймауту — включите приложению разрешение локальной сети
> (в сведениях о приложении отображается как «Устройства поблизости»), иначе
> Local Network Protection молча отбрасывает LAN-трафик.

## Сборка

```
./gradlew assembleDebug
```

Релизные сборки подписываются ключами из `local.properties`:

```
RELEASE_STORE_FILE=…
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

## Технологии

Kotlin · Jetpack Compose (Material 3 Expressive) · OkHttp · EncryptedSharedPreferences ·
Coil · [MaterialKolor](https://github.com/jordond/MaterialKolor) · core-splashscreen
