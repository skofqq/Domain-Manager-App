# Helm

[English version](README.md)

Android-приложение для управления доменной маршрутизацией на роутере OpenWrt —
добавление и удаление доменов в списках [mihomo](https://github.com/MetaCubeX/mihomo)
и [MagiTrickle](https://github.com/MagiTrickle/MagiTrickle), управление движками обхода
DPI и службами роутера прямо с телефона. Заменяет браузерное расширение нативным
Share-Target-флоу: поделитесь ссылкой из любого браузера — и домен окажется на роутере.

## Возможности

### Домены

- **Маршрутизация** — полный список управляемых доменов с двумя независимыми
  индикаторами по системам (mihomo / MagiTrickle) — домен может состоять только в одной
  из них
  - Одно поле делает две работы: фильтрует список по мере ввода и добавляет введённое.
    Вставка из буфера (URL автоматически сворачивается до registrable-домена) или
    «Поделиться» из любого браузера
  - Удаление с явным выбором цели, если состояние систем расходится
  - Долгий тап — переименование домена (членство в системах сохраняется)
- **Стратегии** — персональная стратегия обхода DPI для каждого домена на запущенном
  движке zapret
  - Активный движок определяется при каждом входе; выпадающий список в шапке позволяет
    заглянуть во второй, не запоминая выбор
  - То же объединённое поле поиска и добавления, что и в «Маршрутизации». Новый домен
    попадает на минимальную стратегию движка, а меняется она тапом по строке
  - Липкие группы по стратегиям, внутри каждой — по алфавиту

Обе вкладки поддерживают pull-to-refresh и сами обновляются при возврате на экран,
поэтому изменения из «Поделиться» или с другого устройства видны без ручного обновления.

### Статус

- **Службы** — mihomo, MagiTrickle, zapret, zapret2 и Tor с живыми флагами *работает* и
  *автозапуск*, контекстные кнопки Запустить / Остановить / Перезапустить и просмотр
  лога по каждой службе. Цветовая индикация: зелёный — работает, красный — должна
  работать, но не запущена, серый — остановлена. Учтена взаимоисключаемость
  zapret ⇄ zapret2 (запуск одного выключает второй)
- **Здоровье роутера** — модель, прошивка, аптайм, нагрузка, температура CPU, память и
  занятость `/overlay`. WAN IPv4/IPv6 по умолчанию скрыты и раскрываются переворотом
  телефона экраном вниз
- **mihomo** — proxy-группы с переключением нод, живая скорость передачи, список
  соединений с группировкой по устройству-источнику, бейджи задержки, протокола и IPv6
  по каждой ноде
  - **Подписки** — добавление, смена URL и удаление proxy-провайдеров. Все подписки
    сливаются в один общий пул узлов, поэтому добавленная становится доступна во всех
    proxy-группах. Подписка, прописанная в самом конфиге роутера, показывается только
    для чтения
  - **Наборы правил** — привязка внешнего rule-provider к proxy-группе через пошаговый
    мастер, с живым каталогом источников из
    [MetaCubeX/meta-rules-dat](https://github.com/MetaCubeX/meta-rules-dat)
  - Проверка обновлений mihomo и обновление на месте, с подтверждением и ссылкой на
    changelog
- **Tor** — прогресс запуска, время работы, адреса SOCKS и PAC; «новая личность» —
  новые цепочки без обрыва текущего соединения; настоящая проверка подключения через
  Tor с выводом выходного IP и страны; управление мостами (мост хранится как целая
  непрозрачная строка, каждое изменение перезапускает демон, иначе оно не вступит в
  силу)
- **Профили zapret / zapret2** — переключение стратегии по категориям трафика на
  запущенном движке (9 категорий у zapret2, 4 у v1), проверка доступности и режим
  голосового трафика Discord / WhatsApp / Telegram. Правки zapret2 копятся до
  «Применить» и откатываются; v1 применяется сразу — это ограничение самого движка
- **Устройства** — клиенты локальной сети с иконками, избранным, Wake-on-LAN и
  проверкой «кто сейчас онлайн» пингом со стороны роутера
- **Диагностика** — ping, traceroute, замер скорости WAN и тест задержки proxy-группы

### Настройки

- Внешний вид: живое превью со свайпом, Системная/Светлая/Тёмная тема, динамические
  цвета Material You (Android 12+) или полноценная палитра Material 3 из фирменного
  цвета `#D2DA40`
- Язык: системный / English / Русский, переключается на лету
- Роутеры: несколько профилей с мгновенным переключением, сопряжение по QR
  (`routerdomains://setup`), хост, порт и токен (хранятся в
  `EncryptedSharedPreferences`)
- Безопасность: блокировка по биометрии/PIN — на всё приложение или только на показ
  токена
- Бэкап: экспорт маршрутизации и стратегий версионированным JSON-снимком и импорт обратно
- Мониторинг: фоновые уведомления о смене WAN IP, нехватке места и деградации задержки
  proxy-группы
- Диагностика: настраиваемый HTTP-таймаут и журнал последних запросов к API с всегда
  замаскированным токеном

### Прочее

Анимированный сплэш (две половинки логотипа «стыкуются» со вспышкой соединения),
предиктивный жест «назад» во всех разделах, Material 3 Expressive, виджет на главном
экране со статусом служб, динамические ярлыки приложения и плитка быстрых настроек.

## Требования

- Android 10+ (minSdk 29)
- Роутер OpenWrt с CGI-скриптом `domain-api` по адресу
  `http://<роутер>:<порт>/cgi-bin/domain-api` (доступ по токену, только LAN, обычный HTTP)

### API роутера, с которым работает приложение

Каждый вызов — GET с параметрами в query-строке и JSON в ответе.

**Домены и службы**

| Запрос | Ответ |
|---|---|
| `?token=…&action=add\|remove\|status&domain=example.com&target=both\|mihomo\|magitrickle` | `{"domain":"…","mihomo":bool,"magitrickle":bool}` |
| `?token=…&action=list` | `{"domains":[{"domain":"…","mihomo":bool,"magitrickle":bool},…]}` |
| `?token=…&action=svc_list` | `{"services":[{"service":"…","running":bool,"enabled":bool},…]}` |
| `?token=…&action=svc_status\|svc_start\|svc_stop\|svc_restart&service=mihomo\|magitrickle\|zapret\|zapret2\|tor` | `{"service":"…","running":bool,"enabled":bool}` |

**Tor**

| Запрос | Ответ |
|---|---|
| `?token=…&action=tor_status` | `{"installed":bool,"running":bool,"enabled":bool,"controlport":bool,"bootstrap":0-100,"bridges":int,"socks_port":int,"uptime_seconds":int,"lan_ip":"…","pac_url":"…"}` — если Tor не установлен, ответ вырождается в один `{"installed":false}` |
| `?token=…&action=tor_test` | `{"ok":bool,"ip":"…","country":"XX"}` — занимает несколько секунд; `ok:false` означает «нет подключения», а не сбой запроса |
| `?token=…&action=tor_newnym` | `{"ok":true}` либо `502 {"ok":false,"error":"controlport_not_ready"}` |
| `?token=…&action=tor_bridges_list` | `{"bridges":["obfs4 … cert=… iat-mode=0",…]}` — пустой массив = прямое подключение |
| `?token=…&action=tor_bridges_set&bridges=<через перевод строки>` | То же, что у `tor_status`. Полная замена списка, инкрементального add/remove нет. Переписывает только `torrc` — применяет изменения перезапуск |

**Подписки mihomo**

| Запрос | Ответ |
|---|---|
| `?token=…&action=subscription_list` | `{"subscriptions":[{"name":"…","url":"…","interval":int,"removable":bool},…]}` |
| `?token=…&action=subscription_add&sub_name=…&url=https://…` | Одна запись вида `subscriptions[]`. `sub_name` обязан подходить под `[a-z][a-z0-9-]*` |
| `?token=…&action=subscription_update&sub_name=…&url=https://…` | То же; меняет только URL (имя неизменяемо) и заставляет сразу перечитать список узлов |
| `?token=…&action=subscription_remove&sub_name=…` | `{"name":"…","removed":bool}`. Записи с `removable:false` отклоняются |

**Остальное** по областям — точные форматы см. в `RouterApi.kt`:

- Стратегии: `strat_list`, `strat_add`, `strat_set`, `strat_remove`
- Профили zapret: `z2profile_list`, `z2profile_apply`, `z2profile_rollback`,
  `z1profile_list`, `z1profile_apply`, `voice_status`, `voice_set`, `checkconn`
- mihomo: `mihomo_proxies`, `mihomo_select`, `mihomo_connections`,
  `mihomo_connection_close`, `mihomo_group_delay`, `mihomo_node_ipv6`,
  `mihomo_check_update`, `mihomo_update`, `pg_list`, `magitrickle_groups`
- Наборы правил: `provider_list`, `provider_add`, `provider_remove`
- Роутер: `sys_info`, `disk_info`, `devices`, `wake`, `versions`, `service_log`,
  `reboot`
- Диагностика: `ping`, `traceroute`, `speedtest`

Ошибки: `400 {"error":"bad_domain"|"bad_action"|"bad_target"|"bad_service"|"empty_domain"}`,
`403 {"error":"bad_token"}`, `500 {"error":"token_not_configured"}`. Для специфичных кодов
(`provider_exists`, `subscription_exists`, `subscription_not_found`,
`cannot_remove_base_subscription`, `controlport_not_ready`, `mihomo_config_test_failed`
и др.) в приложении есть отдельные сообщения.

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
WorkManager · androidx.biometric · Coil ·
[MaterialKolor](https://github.com/jordond/MaterialKolor) · core-splashscreen · ZXing +
Play Services code scanner

Без Retrofit, без библиотек маппинга JSON и без DI-фреймворка: чистый OkHttp, разбор
через `org.json` и ручное внедрение зависимостей через конструктор. Каждый вызов
`RouterApi` возвращает sealed-результат (`Success` / `ApiError` / `NetworkError`) и
никогда не бросает исключение.
