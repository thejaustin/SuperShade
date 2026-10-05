# SuperShade — AI Session Devlog

Living document. Update at the start of every AI session: mark completed items, add new ones.
Single source of truth for cross-session continuity.

Kotlin + Jetpack Compose + Koin + Shizuku. Project at `~/projects/SuperShade/`.

---

## Open Backlog

### Features
- [x] **Monet accent strength setting** — `MonetScheme.kt` now supports `accentStrength` with live color interpolation; interactive slider in `SettingsScreen.kt` (0–100%) backed by DataStore `monetAccentStrength` and propagated to all themes.
- [x] **Haptic tick at dismiss threshold & commit** — Added `notificationDismissTick()` (Samsung LRA `EFFECT_CLICK_DISMISS` 50067 / Android R+ `PRIMITIVE_LOW_TICK`) and `notificationDismissCommit()` to `SuperHaptics.kt`; wired into `FluidSwipeToDismiss.kt`. Refactored `NotificationCard.kt` to use `FluidSwipeToDismiss` across all individual and stacked notifications.
- [x] **History / notification log screen** — In-shade `NotificationHistorySheet.kt` with live search, category chips, clear log, and Android system history link. `NotificationRepository.kt` now tracks dismissed notification records in a bounded ring-buffer.
- [ ] **Lockscreen media / notification ambient widget** — Mini heads-up or AOD/ambient widget option for lockscreen media controls.

### Stability / Verification
- [x] **StatusBarBlocker on landscape / multi-display** — `attachStatusBarBlocker()` now dynamically queries `WindowInsets.Type.statusBars()` on API 30+ with safe fallback, and sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on API 28+ to prevent status bar interception around cutouts and landscape orientations.
- [ ] **`SettingsSharingProvider` needs verification** — cross-flavor settings ContentProvider needs device test with both flavors installed (see ShizukuPlus)

### Infrastructure
- [ ] **CI release workflow** — `release.yml` triggers on `v*` tags; no release has been tagged yet; test the full release path with a `v1.9.34` tag

---

## Session History (newest first)

### 2026-10-04 — Antigravity (Gemini 3.8 Flash)

**Done:**
- **In-Shade Sound Mode / Volume Mixer Integration** (`VolumeSlider.kt`, `ShadeViewModel.kt`, `ShadeRoot.kt`): Added `onOpenVolumeMixer` callback to `VolumeSlider`. Tapping the mixer equalizer button or long-pressing the speaker icon now opens the in-shade `QuickTileDetailSheet` with multi-stream volume sliders (media, ring, notification, system) and sound/vibrate/mute mode selector directly inside the shade.
- **Hardened StatusBarBlocker for Landscape & Cutouts** (`ShadeWindowManager.kt`): Updated `attachStatusBarBlocker()` to compute exact top status bar insets using `WindowInsets.Type.statusBars()` on API 30+ with safe fallback against missing resource IDs. Added `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on API 28+ to ensure landscape display cutouts are cleanly guarded against native system shade interception.
- **In-Shade Notification History Sheet** (`NotificationHistorySheet.kt`, `NotificationRepository.kt`, `ShadeViewModel.kt`): Built comprehensive notification log sheet styled in One UI 8 / M3 Expressive. Records all dismissed notifications in `NotificationRepository.dismissedHistory` (up to 60 items) with app icons, formatted timestamps, search filter, category chips, clear log, and OS-level system history launcher. Connected to header "History" button and empty notifications view.
- **Universal Fluid Swipe Dismiss & Haptics** (`FluidSwipeToDismiss.kt`, `NotificationCard.kt`, `SuperHaptics.kt`): Replaced M3 `SwipeToDismissBox` in `NotificationCard` with `FluidSwipeToDismiss`. Added Samsung LRA `EFFECT_CLICK_DISMISS` (50067) and Android R+ `PRIMITIVE_LOW_TICK` detent tick on threshold crossing and decisive commit pulse on release. All individual and stacked notifications now share unified spring physics and angle tilt.
- **Monet Dynamic Strength Slider & Color Lerping** (`MonetScheme.kt`, `ShadeSettings.kt`, `SettingsScreen.kt`, `MainActivity.kt`, theme files): Added configurable `monetAccentStrength` (0.0 to 1.0) with real-time color interpolation in `monetScheme`. Added interactive slider with 5% increments in Appearance settings. Propagated to `OneUiTheme`, `PixelTheme`, `NothingTheme`, `CyberpunkTheme`, and `PureMaterialTheme`.

### 2026-10-03 (Session 2) — Claude Code (Sonnet 4.6) [Housekeeping + Consolidation]

**Done:**
- Added `CLAUDE.md`, `.claude/settings.json` PostToolUse hook (`check-build.sh` after every Edit/Write), and `scripts/dev/` to the canonical repo `~/projects/SuperShade/`
- Deleted stale `/sdcard/Documents/SuperShade/` clone (freed 227 MB)
- Global `~/.claude/CLAUDE.md` updated with SuperShade quick-ref entry

---

### 2026-10-03 (Session 1) — Claude Code (Sonnet 4.6) + Antigravity (Gemini 3.8 Flash)

**Done:**
- **`MonetScheme.kt`** (NEW) — wallpaper-derived Monet color blending for API 31+. `monetScheme(base, isDark, surfaceBlend)` overlays `dynamicDarkColorScheme` / `dynamicLightColorScheme` accent roles onto static theme, blended by `surfaceBlend`. Wired into `OneUiTheme` (0.45), `PixelTheme` (0.85), `NothingTheme` (0.35), `CyberpunkTheme` (0.25).
- **`M3ExpressiveShapes.kt` matrix fix** — corrected post-multiply ordering in both `MorphShape` and `PolygonShape`: Compose `Matrix` post-multiplies, so the last call applies first to points. Correct pipeline: `scale()` then `translate()` (not the reverse). Was producing distorted / mis-aligned shapes.
- **`FluidSwipeToDismiss.kt`** (NEW) — physics-based swipe-to-dismiss replacing M3 `SwipeToDismissBox`. `FluidSwipeState` with `Animatable` offset, `thresholdPx = width * 0.28`, velocity-seeded fling at ≥900dp/s, spring-back (`DampingRatio=0.62, Stiffness=450`) below threshold, no-bounce exit spring for commit. Wired into `GroupedNotificationCard`.
- **Status bar touch blocker** (`ShadeWindowManager`) — invisible `TYPE_APPLICATION_OVERLAY` view at `status_bar_height` px consuming `ACTION_DOWN/MOVE/UP/CANCEL` to prevent native status bar from swallowing swipes while our shade is open. Attached on `show()`, removed on `hide()`.
- **UI polish** — clock: `displayMedium` → `displaySmall`; AM/PM: `headlineSmall` → `titleMedium`; status bar row top padding 16→10dp; sliders island spacing/padding tightened.
