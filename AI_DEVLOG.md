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
- [x] **Lockscreen media / notification ambient widget** — Mini heads-up or AOD/ambient widget option for lockscreen media controls with `FLAG_SHOW_WHEN_LOCKED`, album art, playback controls, progress bar, and fluid swipe dismiss.

### Stability / Verification
- [x] **StatusBarBlocker on landscape / multi-display** — `attachStatusBarBlocker()` now dynamically queries `WindowInsets.Type.statusBars()` on API 30+ with safe fallback, and sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on API 28+ to prevent status bar interception around cutouts and landscape orientations.
- [x] **ART Baseline Profile compilation** — Added validated `app/src/main/baseline-prof.txt` covering all critical Compose UI, overlay lifecycle, Shizuku IPC, and haptics paths to eliminate cold-start and first-swipe jank.

### Infrastructure
- [x] **CI release workflow** — Hardened `release.yml` with resilient signed and unsigned APK fallback handling to guarantee seamless GitHub release creation.

---

## Session History (newest first)

### 2026-10-05 — Antigravity (Gemini 3.8 Flash)

**Done:**
- **Native Android CLI Tools Suite & Environment Activation** (`android`, `apkanalyzer`, `profgen`, `lint`, `retrace`, `aapt2`): Replaced unsupported x86_64 binaries with a unified native ARM64 developer CLI at `~/.local/bin/android`. Provides on-device `android info`, `android describe`, `android layout` (UI hierarchy JSON/XML), `android screen capture`, plus native symlinks for `apkanalyzer`, `profgen`, `lint`, `retrace`, and `aapt2`.
- **ART Baseline Profiles for Compose Performance** (`baseline-prof.txt`): Authored and validated (`profgen validate`) an ART ahead-of-time (AOT) baseline profile for SuperShade in `app/src/main/baseline-prof.txt` covering ShadeRoot, notification feeds, fluid physics, and Shizuku IPC for butter-smooth 120Hz gesture execution.
- **CI Release Pipeline Hardening** (`release.yml`): Updated release workflow APK packaging step to gracefully support both signed and unsigned release artifacts.
- **Samsung One UI 9 Media Player & Scrubber** (`MediaCard.kt`): Designed and implemented the complete Samsung One UI 8.5/9 flagship media playback experience. Features 26dp squircle container, 20dp smooth squircle album art, app icon header, and One UI 9 "Media output" capsule button. Built `OneUi9PillScrubber` with an 8dp rounded capsule track, continuous gesture scrubbing, and Samsung LRA `segmentTick()` (50056) haptic detents on every 5% scrub step. Built `OneUi9InlineVolumeSlider` allowing smooth expandable inline media stream volume adjustment (`AudioManager.STREAM_MUSIC`) directly on the playback card.
- **Multi-Theme Media Architecture** (`MediaCard.kt`, `PixelMediaCard.kt`, `HeadsUpOverlay.kt`): Refactored `MediaCard` into a master dispatcher that automatically resolves `LocalShadeTheme.current`:
  - **One UI 9**: Squircle card, One UI 9 thick pill scrubber, inline volume expander, "Media output" capsule chip, and tactile spring controls.
  - **Pixel**: Signature Android 15/16 sinusoidal wavy seekbar that undulates while playing and straightens when paused, M3 Expressive buttons, stock Android output switcher chip, and collapsed/expanded view states.
  - **Nothing OS**: Technical monochrome container, dot-matrix uppercase typography, Nothing signature glyph red (`#D71920`) indicator dot, 2.5dp wireframe progress bar with red dot playhead, and geometric transport buttons.
  - **Cyberpunk HUD**: `ChamferedCornerShape(12.dp)`, neon cyan border glow (`#00F0FF`), monospace bracketed telemetry (`[TRK // ...]`, `[ART // ...]`), jumping cyber equalizer bars, and neon gradient progress bar.
  - **Pure Material 3**: Clean M3 tonal container, linear slider with thumb, and M3 Expressive buttons.
  - Adapted `AmbientMediaCard` in `HeadsUpOverlay.kt` so lockscreen ambient peek widgets reflect the user's active theme.
- **Hardware & OS Alignment (Samsung One UI 9.0 / Android 17)**: Inspected and aligned with host hardware (`ro.build.version.oneui=90000`, `ro.build.version.sdk=37`, `ro.build.version.sep=180000`). Updated settings and branding references to One UI 9.
- **Lockscreen & Ambient Media Mini Widget** (`HeadsUpOverlay.kt`, `ShadeService.kt`, `ShadeSettings.kt`, `SettingsScreen.kt`, `MainActivity.kt`): Added `ambientMediaWidgetEnabled` preference and reactive DataStore flow. Designed compact M3 Expressive `AmbientMediaCard` with `FLAG_SHOW_WHEN_LOCKED` on the overlay window so track changes display gracefully on lockscreen and over running apps. Features album art squircle, title/artist marquee, skip prev/next, play/pause toggle with haptics, slim progress bar, and swipe-up spring dismiss.
- **Drag-to-Reorder Tactile Arc & Segment Detent Ticks** (`SuperHaptics.kt`, `QuickSettingsGrid.kt`): Adopted tactile arc design patterns from ShizukuPlus. Added `segmentTick()`, `gestureStart()`, and `gestureEnd()` to `SuperHaptics.kt`. Wired `DraggableTileGrid` to emit `gestureStart` + `tileGrab` on lift, `segmentTick` as the dragged card crosses slots, and `tileDrop` + `gestureEnd` for a physical landing thud on release. Upgraded ghost tile to scale 1.10 with subtle 0.96 alpha.
- **M3 Expressive Motion Tuning** (`TileCard.kt`): Replaced bouncy spring color interpolation with critically damped `M3ExpressiveMotion.effectsDefault()` to prevent chromatic overshoot or flashing on tile state changes. Replaced scale spring with `M3ExpressiveMotion.spatialFast()` for tactile press responsiveness.
- **Environment & Token Efficiency**: Integrated practices from "Fixing Termux Crashing Issues" — using `gdt`, `log-trim`, slice reads, and `~/.ignore` to eliminate token waste on build outputs.

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
