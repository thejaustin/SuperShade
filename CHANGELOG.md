# Changelog

All notable changes to SuperShade are documented here.
Releases follow [Semantic Versioning](https://semver.org/).

---

## [1.7.0] — 2026-09-11

### Fixed & Enhanced (Sliders, Zero-ADB Architecture & Media)
- **Brightness & Volume Sliders Fully Functional**:
  - Live brightness adjustment during drag with auto-brightness permission handling (`WRITE_SETTINGS` / `Settings.ACTION_MANAGE_WRITE_SETTINGS`).
  - Added direct fallback via Shizuku shell when `WRITE_SETTINGS` is not granted.
  - Eliminated volume slider drag fighting and snap-back by adding an `isDragging` guard and real-time `AudioManager` synchronization.
  - Tapping the volume icon opens Google's native floating Volume Panel (`Settings.Panel.ACTION_VOLUME`) with one-tap mute fallback.
- **Media Controls & Scrubbing Fixes**:
  - Resolved seek slider thumb fighting by decoupling `seekPreview` from the 1-second position ticker during user dragging.
  - Eliminated fatal bitmap recycling crashes by removing manual `oldArt.recycle()` calls and enabling GC native bitmap lifecycle.
  - Added support for loading album art from `METADATA_KEY_ART_URI` / `METADATA_KEY_ALBUM_ART_URI`.
- **Zero-ADB & Zero-Shizuku Native Shade Replacement**:
  - Introduced `SuperShadeAccessibilityService` using `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` (API 31+) to instantly collapse the system shade and open SuperShade without ADB or root.
  - Refactored `GestureOverlay` from an intrusive 56dp strip into an ultra-thin 4dp top-edge trigger with `FLAG_LAYOUT_IN_SCREEN` so toolbar buttons, back arrows, and tabs in underlying apps are never blocked.
  - Animated exit transition preserved in `ShadeWindowManager` with smooth 260ms delay before view detachment.
  - Added camera cutout/notch support with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
- **Quick Settings & System Toggles Without Shizuku**:
  - Cleaned default tiles to ensure Flashlight, Auto-Rotate, DND, Airplane, Battery Saver, Location, and Dark Mode are all accessible.
  - Removed artificial 50% opacity dimming on `FULL_TOGGLE` tiles when Shizuku is disconnected.
  - Direct non-root tile execution: Auto-Rotate via `WRITE_SETTINGS`, DND via `NotificationCollector`/`NotificationManager`, Sound mode via `AudioManager`, and Wi-Fi / Mobile Data / NFC via official native floating slice panels (`Settings.Panel.*`).
  - Real-time reactive state tracking via system `BroadcastReceiver`s and `ContentObserver`s (0ms latency instead of 5-second polling).
- **Notification Improvements**:
  - Fixed fatal `NoSuchMethodError` on inline replies by updating `PendingIntent.send(context, 0, intent)` with foreground receiver flags.
  - Native framework `snoozeNotification` and `cancelAllNotifications` hooked through `NotificationCollector`.
  - Fixed 1-on-1 direct message conversation formatting in `ShadeNotification`.
  - Refined `CategoryEngine` to prioritize calls and prevent misclassifying user consumer apps into "System".
  - Connected AMOLED black theme styling directly to the shade root surface.

## [1.2.5] — 2026-09-02

### Added & Improved
- Full App Dark Mode support in Settings with System / Dark / Light / AMOLED Black options
- Material 3 dynamic color scheme and pitch-black AMOLED styling
- Live Notification Center reload upon opening the shade
- Real application icons rendered on every notification card
- Multi-field fallback parser for notification titles, body text, and action buttons
- Intelligent notification group filtering to prevent missing standalone group alerts

## [1.2.4] — 2026-09-02

### Fixed & Enhanced
- Gesture overlay now spans the full status bar area with responsive raw coordinate tracking
- Automatic system statusbar disable flag execution on Shizuku connection
- Real privileged toggles for Wi-Fi, Bluetooth, Dark Mode, Auto-Rotate, Airplane Mode, Location, NFC, Mobile Data, DND, and Battery Saver
- Live system state querying so QS tiles accurately reflect real device states
- Tap notification cards to open apps directly and dismiss the shade
- "Open Shade Preview" button in settings for direct testing
- Automatic foreground service restart on app resume if active

## [1.2.3] — 2026-09-01

### Added
- Panel height capped at 72% screen height with scrim tap to dismiss
- Swipe-to-dismiss red delete background
- "Clear all" button with notification counter header
- Notification card animation and media track timer
- Dual brightness icons (dim and high)

## [1.0.0] — 2026-09-01

### Added
- Custom notification shade overlay replacing the system shade via ShizukuPlus
- Category bar with spring animations (All, Messages, Social, Email, Calls, Alarms, System, Apps)
- One UI and Pixel theme support with live switching
- Quick Settings grid with full toggle support via `cmd statusbar click-tile`
- Brightness slider with auto-brightness disable
- Media player card with real MediaController transport controls and album art
- Swipe-to-dismiss notifications with expandable action buttons
- Heads-Up notification overlay for incoming alerts
- In-app update checker against GitHub Releases
- What's New sheet shown automatically after version upgrades
- Shizuku permission auto-grant on connection
- Foreground service with persistent shade gesture detection

### Technical
- Built on ShizukuPlus backend for privileged shell operations
- Compose BOM 2026.02.00, Material3, Koin DI, DataStore preferences
- GitHub Actions CI/CD: debug builds on push, signed release APK on tag
