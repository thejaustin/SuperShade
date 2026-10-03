# SuperShade — Claude Code Guidelines

Custom notification shade replacement for Android. Kotlin + Jetpack Compose + Koin + Shizuku.
Path: `~/projects/SuperShade/`

## Build commands

Local Android SDK is set up (`local.properties` → `sdk.dir`). Builds run on-device.

| Task | Command |
|------|---------|
| Compile check (fast) | `bash scripts/dev/check-build.sh` |
| Build + install debug APK | `bash scripts/dev/build-install-debug.sh` |
| Full debug build only | `./gradlew :app:assembleDebug` |
| CI status | `gh run list --limit 5` |

CI (GitHub Actions) runs on push to `main`/`develop` and on `v*` tags.
- `build.yml` — debug APK
- `release.yml` — signed release APK + GitHub Release on `v*` tag

## Architecture

| Layer | Key classes |
|-------|------------|
| Entry | `MainActivity.kt`, `SuperShadeApplication.kt` (Koin init) |
| Overlay | `ShadeService` (foreground service), `ShadeWindowManager` |
| State | `ShadeViewModel` — Koin `single {}` (shared between Service and overlay) |
| Notifications | `NotificationCollector` (NLS) → `NotificationRepository` |
| Shizuku | `StatusBarGovernor` wraps `Shizuku.newProcess()` for `cmd statusbar` commands |
| Settings | `ui/settings/` — DataStore-backed settings |
| Updates | `domain/update/UpdateChecker` — GitHub Releases API, once/24h |

## Critical architecture rules

| Rule | Why |
|------|-----|
| `ShadeViewModel` is `single {}` not `viewModel {}` | Shared between `ShadeService` and overlay — using `viewModel {}` would create a separate instance for each. Never change this. |
| `ShadeWindowManager` creates a fresh `ShadeLifecycleOwner` on each `show()` | Lifecycle can't re-RESUME after DESTROY. Each overlay instance needs its own lifecycle. |
| Shizuku binder required at runtime | `StatusBarGovernor` needs `Shizuku.newProcess()`. Always guard with `Shizuku.pingBinder()` before any Shizuku call. |

## On-device testing

ADB loopback works without Wi-Fi: `adb connect 127.0.0.1:5555`

Fastest path: `bash scripts/dev/build-install-debug.sh`
This builds a debug APK and installs it directly over ADB. The app requires Shizuku running on the test device for overlay + status bar control.

After install, verify with: `adb -s 127.0.0.1:5555 shell dumpsys activity services | grep ShadeService`

## Branding

- App name: `SuperShade` (`app_name` in `app/src/main/res/values/strings.xml`); package `com.supershade`.
- Original app, not a fork — no upstream attribution needed.

### Project family (all by thejaustin)

| Product | Display name | Repo | Package / coordinates | Upstream |
|---------|-------------|------|------------------------|----------|
| Shizuku+ | `Shizuku+` | `thejaustin/ShizukuPlus` | `af.shizuku.plus.api` (Plus flavor), `moe.shizuku.privileged.api` (Drop-In flavor) | thedjchi/Shizuku ← RikkaApps/Shizuku |
| Shizuku+-API | `Shizuku+-API` | `thejaustin/ShizukuPlus-API` | Maven group `af.shizuku.plus`; JitPack `com.github.thejaustin:Shizuku+-API:<ver>-plus` | RikkaApps/Shizuku-API |
| Obtainium+ | `Obtainium+` | `thejaustin/ObtainiumPlus` | `dev.thejaustin.obtainiumplus` | ImranR98/Obtainium |
| SuperShade | `SuperShade` | `thejaustin/SuperShade` | `com.supershade` | original (no upstream) |

Naming rules:
- User-facing text uses the `+` form (`Shizuku+`, `Obtainium+`); repo names, URLs, and code identifiers spell it `Plus` (`ShizukuPlus`, `PlusSettingsProvider`). Never write "Shizuku Plus" or "ObtainiumPlus" in UI strings.
- `SuperShade` is one word, capital S twice — never "Super Shade" / "Supershade".
- Refer to upstreams by their own names (Shizuku, Obtainium) and credit them; don't rebrand upstream attributions or license notices.

## Git / commits

- Stage files by name, never `git add -A`
- Push `v*` tags to trigger release CI: `git tag v1.9.3 && git push origin v1.9.3`

## Sentry / crash reporting
Check `app/src/main/java/com/supershade/` for any Sentry integration before adding crash logging.
