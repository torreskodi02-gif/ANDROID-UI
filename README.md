# Merlin Launcher v1.0.0

A native Kotlin Android launcher built for Kodi's **Motorola One Action**
(6.3" 1080×2520 21:9, 4 GB RAM, Exynos 9609, Android 11 — its last update).
Programmatic Views only, **zero dependencies** (stdlib + Android framework),
target API 30 / min API 28 / compile API 34. Light and fast is the priority.

Ideas borrowed from Nova, Lawnchair, Action, Niagara, Smart Launcher and
Microsoft Launcher — the *ideas* only. No code, assets, or designs copied.

## Download the APK

**[Download merlin-launcher.apk (latest release)](https://github.com/torreskodi02-gif/ANDROID-UI/releases/download/latest/merlin-launcher.apk)**

This repo is public — anyone can download it from this page. The link
above starts working as soon as the first GitHub Actions build publishes
the `latest` release (see "How the APK gets here" below). Until then, the
v1.0.0 APK is also attached in the chat where it was built.

Install: download the APK on your phone, tap it, allow install from
unknown sources if asked, then press Home, pick Merlin Launcher, tap
Always. Then set it as default: Settings > Apps > Default apps >
Home app > Merlin Launcher.

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
- **Settings, searchable** — 10 grouped sections, every control reads and
  writes a real preference (no dead toggles):
  Look & Feel (6 theme palettes, Auto/Light/Dark, 8 accents, dynamic
  colour sampled from the wallpaper, wallpaper picker) · Home (style,
  presets Minimal/Classic/Dense/One-handed, grid rows/cols, icon size,
  labels, lock layout, animation speed) · Drawer (sort, columns, hidden
  apps manager) · Dock · Icons & Folders (icon-pack picker for ADW/Nova
  packs, 4 icon shapes, themed icons) · Gestures (swipe-down choice,
  double-tap lock) · Notifications (badges off/dot/count + grant buttons)
  · Feed · Backup (export/import JSON via the system file picker, with a
  share-intent fallback) · About.
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

## Not in v1 (planned for v1.1)

Widget host · Shutters (widget preview on icon swipe) · Cover folders ·
Auto-backup schedule · Real weather.

## How the APK gets here

The repo is **public**: [torreskodi02-gif/ANDROID-UI](https://github.com/torreskodi02-gif/ANDROID-UI).
The v1.0.0 code is already pushed to `main` (commit `fd61ddb6`).

One step is still left, and only Kodi can do it — the GitHub App cannot
push workflow files (403):

1. **Paste `build-apk.yml`** to `.github/workflows/build-apk.yml` via the
   GitHub web editor: open `build-apk.yml` in the repo, Raw, copy it,
   then Add file > Create new file, name it
   `.github/workflows/build-apk.yml`, paste, commit.
2. That starts the Actions build. When it finishes, the APK is published
   to the `latest` release and the Download link at the top of this page
   works: `releases/download/latest/merlin-launcher.apk`.
3. Install it and set it as the default launcher: Settings > Apps >
   Default apps > Home app > Merlin Launcher. Then grant, in Settings >
   Notifications and Settings > Gestures, the three optional permissions
   above if you want badges, shade pull-down, and double-tap lock.

## Files

| File | What it is |
| --- | --- |
| `app/src/main/java/app/merlinlauncher/Prefs.kt` | All settings + presets + JSON backup/restore |
| `…/Themes.kt` | 6 palettes, 8 accents, wallpaper-average dynamic colour |
| `…/Apps.kt` | App loading, categories, usage counts, launch, long-press menu |
| `…/IconPack.kt` | ADW/Nova icon packs + shape masking / themed icons |
| `…/MainActivity.kt` | Home: Grid + List, Feed, dock, gestures, double-tap lock |
| `…/Drawer.kt` | DrawerActivity: search, sorts, category sections |
| `…/Settings.kt` | SettingsActivity: 10 sections, searchable, no dead toggles |
| `…/BadgeService.kt` | NotificationListenerService, counts only |
| `…/ShadeService.kt` | AccessibilityService stub + LockAdmin device admin |
| `app/src/main/AndroidManifest.xml` | HOME launcher, services, device admin |
| `app/build.gradle`, `settings.gradle`, `gradle.properties` | Zero-dependency build, target 30 / min 28 |
| `build-apk.yml` | GitHub Actions build (paste to `.github/workflows/`) |
