# Domain Manager

[Русская версия](README.ru.md)

Android app for managing domain-based routing on an OpenWrt router — add and remove
domains in [mihomo](https://github.com/MetaCubeX/mihomo) and
[MagiTrickle](https://github.com/MagiTrickle/MagiTrickle) lists, and control router
services, right from your phone. Replaces a browser-extension workflow with a native
Share-Target flow: share a link from any browser and the domain lands on the router.

## Features

- **Domains** — the main screen:
  - Full list of managed domains with two independent per-system indicators
    (mihomo / MagiTrickle), since a domain may be present in only one of them
  - Add a domain by typing, pasting from the clipboard (URLs are collapsed to the
    registrable domain automatically), or sharing a link from any browser
  - Remove with an explicit target choice when the two systems disagree
  - Long-press to rename a domain (keeps its system membership)
  - Pull-to-refresh, automatic refresh on return to the screen
- **Status** — router service control:
  - mihomo, MagiTrickle, zapret, zapret2 with live *running* and *autostart* flags
  - Contextual Start / Stop / Restart buttons
  - Color-coded state: green — running, red — should be running but isn't, grey — stopped
  - Handles the zapret ⇄ zapret2 mutual exclusivity (starting one disables the other)
- **Settings** — hierarchical, two levels:
  - Appearance: live phone preview, System/Light/Dark, Material You dynamic color
    (Android 12+) or a full Material 3 palette generated from the brand color `#D2DA40`
  - Language: system / English / Русский, switchable at runtime
  - Authorization: router host, port, access token (stored in `EncryptedSharedPreferences`)
- Animated splash (the two logo halves "dock" and flash on connection), predictive back,
  Material 3 Expressive, Quick Settings tile.

## Requirements

- Android 10+ (minSdk 29)
- An OpenWrt router with the `domain-api` CGI script at
  `http://<router>:<port>/cgi-bin/domain-api` (token-protected, LAN-only, plain HTTP)

### Router API the app talks to

| Request | Response |
|---|---|
| `?token=…&action=add\|remove\|status&domain=example.com&target=both\|mihomo\|magitrickle` | `{"domain":"…","mihomo":bool,"magitrickle":bool}` |
| `?token=…&action=list` | `{"domains":[{"domain":"…","mihomo":bool,"magitrickle":bool},…]}` |
| `?token=…&action=svc_list` | `{"services":[{"service":"…","running":bool,"enabled":bool},…]}` |
| `?token=…&action=svc_status\|svc_start\|svc_stop\|svc_restart&service=mihomo\|magitrickle\|zapret\|zapret2` | `{"service":"…","running":bool,"enabled":bool}` |

Errors: `400 {"error":"bad_domain"|"bad_action"|"bad_target"|"bad_service"|"empty_domain"}`,
`403 {"error":"bad_token"}`, `500 {"error":"token_not_configured"}`.

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
Coil · [MaterialKolor](https://github.com/jordond/MaterialKolor) · core-splashscreen
