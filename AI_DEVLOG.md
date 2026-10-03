# SuperShade — AI Session Devlog

Living document. Update at the start of every AI session: mark completed items, add new ones.
Single source of truth for cross-session continuity.

Kotlin + Jetpack Compose + Koin + Shizuku. Project at `~/projects/SuperShade/`.

---

## Open Backlog

### Features
- [ ] **Monet accent strength setting** — `MonetScheme.kt` blends wallpaper colors with fixed ratios per theme (Pixel 0.85, OneUI 0.45, Nothing 0.35, Cyberpunk 0.25); a user-facing slider in settings to tune this per-theme would be natural next step
- [ ] **Haptic tick at dismiss threshold** — `FluidSwipeToDismiss` has a `SuperHaptics` integration point but the haptic tick at `thresholdPx` commit is not yet wired up
- [ ] **History / notification log screen** — button exists in the shade header; tapping it should open a filtered list of dismissed notifications

### Stability / Verification
- [ ] **StatusBarBlocker on landscape / multi-display** — `attachStatusBarBlocker()` uses `status_bar_height` dimen which is correct for portrait; needs verification on foldable / landscape orientations
- [ ] **`SettingsSharingProvider` needs verification** — cross-flavor settings ContentProvider needs device test with both flavors installed (see ShizukuPlus)

### Infrastructure
- [ ] **CI release workflow** — `release.yml` triggers on `v*` tags; no release has been tagged yet; test the full release path with a `v1.9.34` tag

---

## Session History (newest first)

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
