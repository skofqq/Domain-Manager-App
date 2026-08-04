# Helm

[Русская версия](README.ru.md)

Android app for managing domain-based routing on an OpenWrt router — add and remove
domains in [mihomo](https://github.com/MetaCubeX/mihomo) and
[MagiTrickle](https://github.com/MagiTrickle/MagiTrickle) lists, drive the DPI-bypass
engines, and control router services, right from your phone. Replaces a
browser-extension workflow with a native Share-Target flow: share a link from any
browser and the domain lands on the router.

## Features

### Domains

- **Routing** — full list of managed domains with two independent per-system
  indicators (mihomo / MagiTrickle), since a domain may be present in only one of them
  - One field does both jobs: it filters the list as you type and adds what you typed.
    Paste from the clipboard (URLs are collapsed to the registrable domain
    automatically) or share a link from any browser
  - Remove with an explicit target choice when the two systems disagree
  - Long-press to rename a domain (keeps its system membership)
- **Strategies** — per-domain DPI-bypass strategy on the running zapret engine
  - The active engine is detected on every visit; a top-bar dropdown lets you peek at
    the other one without persisting the choice
  - Same merged search-and-add field as Routing. A new domain lands on the engine's
    minimum strategy and is retuned by tapping its row
  - Sticky per-strategy groups, alphabetical inside each

Both tabs pull-to-refresh and re-fetch automatically on return to the screen, so
changes made from the Share flow or another device show up without a manual refresh.

### Status

- **Services** — mihomo, MagiTrickle, zapret, zapret2 and Tor with live *running* and
  *autostart* flags, contextual Start / Stop / Restart, and per-service log viewing.
  Color-coded state: green — running, red — should be running but isn't, grey — stopped.
  Handles the zapret ⇄ zapret2 mutual exclusivity (starting one disables the other)
- **Router health** — model, firmware, uptime, load, CPU temperature, memory and
  `/overlay` usage. WAN IPv4/IPv6 are hidden behind placeholders and revealed by
  flipping the phone face-down
- **mihomo** — proxy groups with node switching, live transfer speed, the connection
  list grouped by source device, and per-node latency / protocol / IPv6 badges
  - **Subscriptions** — add, re-point and remove proxy providers. All subscriptions
    merge into one shared node pool, so anything you add becomes selectable in every
    proxy group. The subscription baked into the router's own config is listed
    read-only
  - **Rule-sets** — bind an external rule-provider to a proxy group through a
    step-by-step wizard, with a live source catalog read from
    [MetaCubeX/meta-rules-dat](https://github.com/MetaCubeX/meta-rules-dat)
  - mihomo version check and in-place update, with confirmation and a changelog link
- **Tor** — bootstrap progress, uptime and SOCKS/PAC endpoints; "new identity" for
  fresh circuits without dropping the connection; a real connectivity check through
  Tor reporting the exit IP and country; and bridge management (bridges are stored as
  whole opaque lines and every change restarts the daemon so it takes effect)
- **zapret / zapret2 profiles** — switch the per-category traffic strategy on the
  running engine (9 categories for zapret2, 4 for v1), plus a reachability check and a
  voice-traffic mode switch for Discord / WhatsApp / Telegram. zapret2 edits are staged
  until "Apply" and can be rolled back; v1 applies immediately, which is an engine
  limitation
- **Devices** — connected LAN clients with icons, favourites, Wake-on-LAN and a
  router-side "who is online now" ping sweep
- **Diagnostics** — ping, traceroute, WAN speed test and a proxy-group latency test

### Settings

- Appearance: swipeable live theme preview, System/Light/Dark, Material You dynamic
  color (Android 12+) or a full Material 3 palette generated from the brand color
  `#D2DA40`
- Language: system / English / Русский, switchable at runtime
- Routers: several router profiles with instant switching, QR pairing
  (`routerdomains://setup`), host, port and access token (stored in
  `EncryptedSharedPreferences`)
- Security: biometric/PIN lock for the whole app or just the token reveal
- Backup: export routing + strategies as a versioned JSON snapshot and import it back
- Monitoring: background notifications on WAN IP change, low disk space and proxy
  latency degradation
- Diagnostics: adjustable HTTP timeout and a rolling log of recent API requests with
  the token always masked

### Elsewhere

Animated splash (the two logo halves "dock" and flash on connection), predictive back
throughout, Material 3 Expressive, a home-screen widget with service status, dynamic
app shortcuts and a Quick Settings tile.

## Requirements

- Android 10+ (minSdk 29)
- An OpenWrt router with the `domain-api` CGI script at
  `http://<router>:<port>/cgi-bin/domain-api` (token-protected, LAN-only, plain HTTP)

### Router API the app talks to

Every call is a GET with query-string parameters and a JSON response.

**Domains and services**

| Request | Response |
|---|---|
| `?token=…&action=add\|remove\|status&domain=example.com&target=both\|mihomo\|magitrickle` | `{"domain":"…","mihomo":bool,"magitrickle":bool}` |
| `?token=…&action=list` | `{"domains":[{"domain":"…","mihomo":bool,"magitrickle":bool},…]}` |
| `?token=…&action=svc_list` | `{"services":[{"service":"…","running":bool,"enabled":bool},…]}` |
| `?token=…&action=svc_status\|svc_start\|svc_stop\|svc_restart&service=mihomo\|magitrickle\|zapret\|zapret2\|tor` | `{"service":"…","running":bool,"enabled":bool}` |

**Tor**

| Request | Response |
|---|---|
| `?token=…&action=tor_status` | `{"installed":bool,"running":bool,"enabled":bool,"controlport":bool,"bootstrap":0-100,"bridges":int,"socks_port":int,"uptime_seconds":int,"lan_ip":"…","pac_url":"…"}` — degrades to `{"installed":false}` alone when Tor is absent |
| `?token=…&action=tor_test` | `{"ok":bool,"ip":"…","country":"XX"}` — takes several seconds; `ok:false` means "not connected", not a failed request |
| `?token=…&action=tor_newnym` | `{"ok":true}`, or `502 {"ok":false,"error":"controlport_not_ready"}` |
| `?token=…&action=tor_bridges_list` | `{"bridges":["obfs4 … cert=… iat-mode=0",…]}` — empty array = direct connection |
| `?token=…&action=tor_bridges_set&bridges=<newline-separated>` | Same shape as `tor_status`. Full replace, no incremental add/remove. Rewrites `torrc` only — a restart is what applies it |

**mihomo subscriptions**

| Request | Response |
|---|---|
| `?token=…&action=subscription_list` | `{"subscriptions":[{"name":"…","url":"…","interval":int,"removable":bool},…]}` |
| `?token=…&action=subscription_add&sub_name=…&url=https://…` | One `subscriptions[]` entry. `sub_name` must match `[a-z][a-z0-9-]*` |
| `?token=…&action=subscription_update&sub_name=…&url=https://…` | Same; changes the URL only (the name is immutable) and forces an immediate re-fetch |
| `?token=…&action=subscription_remove&sub_name=…` | `{"name":"…","removed":bool}`. `removable:false` entries are rejected |

**Everything else**, by area — see `RouterApi.kt` for the exact shapes:

- Strategies: `strat_list`, `strat_add`, `strat_set`, `strat_remove`
- zapret profiles: `z2profile_list`, `z2profile_apply`, `z2profile_rollback`,
  `z1profile_list`, `z1profile_apply`, `voice_status`, `voice_set`, `checkconn`
- mihomo: `mihomo_proxies`, `mihomo_select`, `mihomo_connections`,
  `mihomo_connection_close`, `mihomo_group_delay`, `mihomo_node_ipv6`,
  `mihomo_check_update`, `mihomo_update`, `pg_list`, `magitrickle_groups`
- Rule-sets: `provider_list`, `provider_add`, `provider_remove`
- Router: `sys_info`, `disk_info`, `devices`, `wake`, `versions`, `service_log`,
  `reboot`
- Diagnostics: `ping`, `traceroute`, `speedtest`

Errors: `400 {"error":"bad_domain"|"bad_action"|"bad_target"|"bad_service"|"empty_domain"}`,
`403 {"error":"bad_token"}`, `500 {"error":"token_not_configured"}`. Feature-specific
codes (`provider_exists`, `subscription_exists`, `subscription_not_found`,
`cannot_remove_base_subscription`, `controlport_not_ready`, `mihomo_config_test_failed`,
…) each get their own message in the app.

> **Android 16+ note:** if "Test connection" times out on a freshly installed build,
> allow the local-network permission for the app (shown as "Nearby devices" in App info) —
> Local Network Protection silently drops LAN traffic otherwise.

## Building

```
./gradlew assembleDebug
```

Release builds are signed with keys supplied via `local.properties`:

```
RELEASE_STORE_FILE=…
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

## Tech stack

Kotlin · Jetpack Compose (Material 3 Expressive) · OkHttp · EncryptedSharedPreferences ·
WorkManager · androidx.biometric · Coil ·
[MaterialKolor](https://github.com/jordond/MaterialKolor) · core-splashscreen · ZXing +
Play Services code scanner

No Retrofit, no JSON-mapping library and no DI framework: raw OkHttp with `org.json`
parsing and manual constructor injection. Every `RouterApi` call returns a sealed
result (`Success` / `ApiError` / `NetworkError`) and never throws.
