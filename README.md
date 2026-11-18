# Merlin Launcher v1.2.0

A native Kotlin Android launcher built for Kodi's **Motorola One Action**
(6.3" 1080×2520 21:9, 4 GB RAM, Exynos 9609, Android 11 — its last update).
Programmatic Views only, **zero dependencies** (stdlib + Android framework),
target API 30 / min API 28 / compile API 34. Light and fast is the priority.

Ideas borrowed from Nova, Lawnchair, Action, Niagara, Smart Launcher and
Microsoft Launcher — the *ideas* only. No code, assets, or designs copied.

## Download the APK

**[Download merlin-launcher.apk (latest release)](https://github.com/torreskodi02-gif/ANDROID-UI/releases/download/latest/merlin-launcher.apk)**

This repo is public — anyone can download it from this page. The link
above works now: every push to `main` builds the APK in GitHub Actions
and publishes it to the `latest` release.

Install: download the APK on your phone, tap it, allow install from
unknown sources if asked, then press Home, pick Merlin Launcher, tap
Always. The first-run wizard walks you through picking Grid or List,
a preset, and making Merlin your default Home. You can also set it
anytime in Merlin Settings ("Set Merlin as default launcher" at the
top) or in system Settings > Apps > Default apps > Home app.

## What v1.2 adds — Personalities (the final build)

Every user has their own personality, so the launcher does too:

- **Six Personalities, one tap each** — Minimal, Sunset Pop, Neon Night,
  Ocean Glass, Paper Day, Playful. Each one restyles the *whole*
  launcher at once: colours, icon shape, grid density, labels, dock,
  clock, search, tiles, animations. Pick one in Settings > Looks &
  Personalities (preview cards show the colours first) or tap
  "Surprise me" and let it pick for you.
- **A home screen that looks different** — a greeting by time of day
  ("Good morning / afternoon / evening"), three clock styles (Big,
  Compact, Date-first), three search bar styles (Pill, Bar, Minimal),
  three tile styles (Flat, Glassy card, Bold outline), text size,
  home dim, and a slide-up entrance animation.
- **Two new icon shapes** — Teardrop and Hexagon, alongside Circle,
  Squircle, Rounded and Square. The drawer can be a grid or list rows.
- **More Settings** — clock style, search bar style, tile style, drawer
  style, text size, home dim, all live-applying.

## What v1.1 adds (on top of v1.0)

- **Friendly first-run wizard** — Welcome, pick your style (Grid/List +
  preset), then a big "Make Merlin your Home" button. Permissions are
  explained in plain words, all optional.
- **Set as default, the easy way** — a "Set Merlin as default launcher"
  row at the top of Settings (with live status) and a gentle one-time
  pill on Home, both using Android's RoleManager default-home request.
- **Visual rework** — rounder cards and pills, softer colours, press
  ripples everywhere, friendlier drawer (group headers, clear-search
  button, "No apps match" empty state, drag-to-jump alphabet strip),
  and gradient letter tiles for apps with no icon.
- **More Settings** — Home grid columns/rows, icon size + label size,
  show/hide labels, dock styles (floating rounded / full-width bar /
  transparent), search bar and At-a-Glance toggles, date on/off,
  12/24-hour clock, swipe-up-for-drawer toggle, haptics toggle,
  animation speed, and "Reset all settings" with confirm.

## What v1 does

- **Home, both styles, switchable anytime** — Grid mode (rows/cols from
  Settings, favourites + Home apps) and List mode (Niagara-style vertical
  A–Z list, favourites on top, alphabet wave on the right edge for
  one-handed jumping). Toggle on Home or in Settings. Default: Grid,
  preset Minimal.
- **At-a-Glance** — date + time. Weather is an honest placeholder
  ("Add widget") until a widget host lands in v1.1; no fake forecast.
- **App drawer** — live search, sort A–Z / most-used / category sections
  with headers, grid columns from Settings, all-apps count, hidden apps
  excluded.
- **Dock** — show/hide, 3–7 apps, favourites first then most-used.
  Swipe up on the dock opens the drawer.
- **Feed** — swipe left on Home: quick notes (auto-saved), recent apps,
  favourites. Can be turned off.
- **Settings, searchable** — grouped sections, every control reads and
  writes a real preference (no dead toggles):
  Look & Feel (6 theme palettes, Auto/Light/Dark, 8 accents, dynamic
  colour sampled from the wallpaper, wallpaper picker) · Home (style,
  presets Minimal/Classic/Dense/One-handed, grid rows/cols, icon size,
  labels, lock layout, animation speed) · Drawer (sort, columns, hidden
  apps manager) · Dock (style, size) · Icons & Folders (icon-pack picker
  for ADW/Nova packs, 4 icon shapes, themed icons) · Gestures (swipe-down
  choice, swipe-up for drawer, double-tap lock, haptics) · Home screen
  (search bar, At-a-Glance, date, clock format) · Notifications (badges
  off/dot/count + grant buttons) · Feed · Backup (export/import JSON via
  the system file picker, with a share-intent fallback) · Reset · About.
- **Long-press any app** — App info, Uninstall, favourite, add/remove
  from Home, rename label (stored per app), hide app.
- **Badges** — dot or count, from active notifications only. Never reads
  notification content.
- **Backup / restore** — full settings JSON (presets, favourites, hidden
  apps, labels, usage counts, notes).

## Honest limits (also shown in Settings > About)

- Cannot replace the lock screen, status bar, or notification shade —
  Android does not allow a launcher to do that.
- Gesture-nav Recents will be choppy without root. **3-button navigation
  is recommended.**
- Shade pull-down needs the AccessibilityService granted in system
  Settings (Settings > Notifications has the button). Until granted it
  does nothing — that is an Android limit, not a bug.
- Double-tap lock needs device-admin granted in system Settings.
- Badges need Notification access granted in system Settings.
- No widget host yet, so no weather widget and no At-a-Glance weather.

## Not in v1.2 (planned next)

Widget host · Shutters (widget preview on icon swipe) · Cover folders ·
Auto-backup schedule · Real weather.

## How the APK gets here

The repo is **public**: [torreskodi02-gif/ANDROID-UI](https://github.com/torreskodi02-gif/ANDROID-UI).
Every push to `main` runs the "Build APK" workflow in GitHub Actions
(`.github/workflows/build-apk.yml` — pasted by Kodi, since the GitHub App
cannot push workflow files) and publishes the APK to the `latest`
release, so the Download link at the top of this page always has the
newest build. Install it and set it as the default launcher from the
first-run wizard or from Merlin Settings.

## Files

| File | What it is |
| --- | --- |
| `app/src/main/java/app/merlinlauncher/Prefs.kt` | All settings + presets + JSON backup/restore |
| `…/Themes.kt` | 6 palettes, 8 accents, wallpaper-average dynamic colour |
| `…/Apps.kt` | App loading, categories, usage counts, launch, long-press menu |
| `…/IconPack.kt` | ADW/Nova icon packs + shape masking / themed icons |
| `…/MainActivity.kt` | Home: Grid + List, Feed, dock, gestures, double-tap lock, default-home nudge |
| `…/Onboarding.kt` | First-run welcome wizard (style, preset, make-me-default) |
| `…/DefaultHome.kt` | RoleManager default-home request + is-default check |
| `…/Personalities.kt` | 6 one-tap full looks (Minimal, Sunset Pop, Neon Night, Ocean Glass, Paper Day, Playful) |
| `…/Drawer.kt` | DrawerActivity: search, sorts, category sections |
| `…/Settings.kt` | SettingsActivity: 10 sections, searchable, no dead toggles |
| `…/BadgeService.kt` | NotificationListenerService, counts only |
| `…/ShadeService.kt` | AccessibilityService stub + LockAdmin device admin |
| `app/src/main/AndroidManifest.xml` | HOME launcher, services, device admin |
| `app/build.gradle`, `settings.gradle`, `gradle.properties` | Zero-dependency build, target 30 / min 28 |
| `build-apk.yml` | GitHub Actions build (paste to `.github/workflows/`) |
