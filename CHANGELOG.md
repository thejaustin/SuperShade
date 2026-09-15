# Changelog

All notable changes to SuperShade are documented here.
Releases follow [Semantic Versioning](https://semver.org/).

---

## [1.9.0] — 2026-09-15

### Added & Enhanced (One UI 8 Settings Hub, Precision Gestures & Notification Polish)
- **Modern Redesigned Settings UI**:
  - Implemented sleek One UI 8 Hero Header with live status badge ("Active • Swipe down from status bar to open" vs "Setup required"), app emblem, and embedded Master Toggle switch.
  - Added Quick Action Hub with primary "Open Shade" and tonal "Customize Tiles" buttons.
  - Unified Permissions & System Integration Hub: Clean 5-service island card grouping Notification Access, Overlay, Accessibility, System Settings, and Shizuku with direct action pills and status badges.
  - Visual Gestures & Controls Card: Diagram explaining the 72%/28% status bar pull split, tactile haptic feedback indicator, and native system panel suppression toggle.
  - Appearance & Theming Hub: Single-choice segmented button rows for Shade Style (One UI, Pixel, Pure Material) and Theme Mode (System, Dark, Light, AMOLED), plus interactive accent swatches with active selection rings.
- **Precision Status Bar Gestures & Haptics**:
  - Refined status bar touch capture zone in both `GestureOverlay` and `SuperShadeAccessibilityService` to strictly encompass the status bar region (`h + 6dp`) to eliminate touch occlusion on top app navigation bars.
  - Added immediate mechanical clock-tick haptic feedback (`CLOCK_TICK`) when the user crosses the pull threshold.
  - Automatic gesture overlay bypass when the shade overlay is already open to avoid consuming touches.
  - Screen rotation and display resizing listener dynamically re-attaches touch bounds in `SuperShadeAccessibilityService`.
- **Quick Settings & Notification Polish**:
  - Dynamic content sizing on `QuickSettingsGrid` using `Modifier.animateContentSize()` with low-bouncy spring spec to eliminate font/display scale clipping.
  - Added trailing chevron to connectivity wide cards for clearer affordance.
  - Proportional trash icon scaling and alpha feedback on notification dismiss swipe backgrounds.
  - Redesigned "Clear all" button as a Samsung One UI pill chip with `ClearAll` icon.
  - Injected `ShadeWindowManager` into `MainActivity` and `TilePreferencesActivity` for instant guaranteed shade opening.

## [1.8.9] — 2026-09-15

### Fixed & Enhanced (Gestures, Quick Settings Toggles, Categorization & Direct Installer)
- **Responsive Notification Swipe-to-Dismiss**:
  - Configured relaxed `positionalThreshold` (35% of total card distance) on `SwipeToDismissBoxState` for both single and grouped notification cards.
  - Enabled bidirectional swipe dismissal (`enableDismissFromStartToEnd` & `enableDismissFromEndToStart`) with matching directional delete indicator backgrounds.
- **Enhanced Notification Categorization Engine**:
  - Added new `Productivity` (Tasks, Calendar, Notes, Reminders) and `Media` categories to `ShadeCategory`.
  - Overhauled `CategoryEngine` with comprehensive heuristics across popular messaging, social, email, streaming, and productivity apps.
  - Prevented user-facing Samsung apps (Samsung Pay, Notes, Health, Wearable) from being misclassified into the "System" category.
  - Cleaned up `CategoryBar`: empty categories with 0 notifications are dynamically hidden to prevent clutter, and the bar is suppressed when no notifications exist.
- **Direct Shade Presentation & Overlay Lifecycle**:
  - Registered `ShadeWindowManager` and `HeadsUpOverlay` as shared singletons in `AppModule`.
  - Injected `ShadeWindowManager` into `SuperShadeAccessibilityService` and `SuperShadeTileService` so the shade overlay presents immediately without failing due to Android 14+ background Foreground Service restrictions.
  - Extended status bar touch capture zone by +36dp below the bezel in both `GestureOverlay` and `SuperShadeAccessibilityService` with relaxed swipe thresholds (18f move threshold, 0.75 ratio).
- **Reliable Quick Settings Toggles**:
  - Restored Bluetooth toggling on Samsung One UI by switching from `cmd bluetooth` to `svc bluetooth`.
  - Added `canRunPrivileged` check in `StatusBarGovernor` to resolve async Shizuku service binding race conditions.
  - Added optimistic UI state updates with automatic 250ms driver verification query.
  - Fixed QuickSettingsGrid layout using chunked rows to prevent constraint overflows.
- **In-App Direct Update Installer**:
  - Integrated system `DownloadManager` in `UpdateDialog` with status-bar download progress and tap-to-install completion.
  - Added `REQUEST_INSTALL_PACKAGES` permission in `AndroidManifest.xml` for direct APK installation.

## [1.8.8] — 2026-09-15

### Added & Enhanced (Dynamic Color Palette, One UI 8 Connectivity Cards & Rich Tile Subtitles)
- **Personalization & Accent Color Palette**:
  - Added modern Color Palette engine supporting Galaxy Blue (`#2575FC`), Emerald (`#10B981`), Violet (`#8B5CF6`), Amber (`#F59E0B`), Coral (`#F43F5E`), and Dynamic Monet.
  - Interactive circular color swatches in Settings with active ring indicator and accent preview.
  - Seamless colorScheme propagation across One UI, Pixel, Pure Material, and in-app settings surfaces.
- **One UI 8 Dual Connectivity Cards**:
  - When expanded in One UI mode, two prominent wide connectivity cards (Wi-Fi and Bluetooth) are rendered at the top of the Quick Settings island matching Samsung One UI 8 layout.
  - Integrated circular icon badges, primary accent activation state, and responsive spring touch animations.
  - Long-pressing either card directly opens the corresponding system settings screen.
- **Real-Time Rich Tile Subtitles**:
  - Added connected Bluetooth device name queries via `BluetoothAdapter.bondedDevices`.
  - Added sound mode status subtitles ("Sound", "Vibrate", "Mute") via `AudioManager.ringerMode`.
  - Added Auto-rotate ("Auto rotate" / "Portrait") and Wi-Fi hotspot active indicator.

## [1.8.7] — 2026-09-15

### Added & Enhanced (One UI 8 & Android 16 Island Architecture & AMOLED Theming)
- **One UI 8 Island Card Architecture**:
  - Enclosed Quick Settings grid in a unified 26dp rounded island card with frosted translucent background and delicate outline border (`surfaceContainer`).
  - Enclosed dual brightness & volume pill sliders in a matching 24dp rounded island container with consistent padding and alignment.
  - Notification cards and notification groups upgraded to elevated `surfaceContainer` floating islands with subtle borders, eliminating flat background blending and enhancing readability.
- **Deep AMOLED & Material You Palette Refinement**:
  - Redesigned AMOLED mode with true black (`#000000`) canvas paired with rich dark graphite (`#111317`) island containers and crisp edges, saving battery on Galaxy Dynamic AMOLED 2X displays.
  - Updated standard One UI dark palette to a modern frosted glass aesthetic (`Color(0xEB121418)`).
- **Haptic & Visual Interaction Polish**:
  - Added responsive tactile haptic feedback to Quick Settings tiles and category chips on tap.
  - Category filter chips upgraded with rounded pill backgrounds and subtle inactive border strokes.
  - Replaced legacy volume and send icons with modern `AutoMirrored` vector drawables.

## [1.8.6] — 2026-09-15

### Fixed & Enhanced (GitHub Releases, In-App Auto-Update & MediaCard Polish)
- **GitHub Release CI & In-App Auto-Update Overhaul**:
  - Removed hardcoded local Termux path from repository `gradle.properties` that was causing GitHub Actions `assembleRelease` jobs to fail on Ubuntu runners with `Specified AAPT2 executable does not exist`. Release builds and published APKs now succeed cleanly.
  - Hardened semantic version comparison in `UpdateInfo` to sanitize pre-release suffixes, build tags, and non-numeric characters so updates are accurately detected.
  - Added `UpdateCheckResult` (`UpdateAvailable`, `UpToDate`, `Error`) and reactive `isChecking` flow to `UpdateRepository`.
  - Added visual loading indicator to "Check for updates" button in Settings and contextual Toast notifications confirming up-to-date status or connectivity issues on manual check.
- **Media Player & MediaCard Visual Polish**:
  - Full-bleed background color adapted dynamically from track artwork via `Palette` with smooth spring transition.
  - Layered translucent album art backdrop with dual gradient scrim for improved contrast and readability.
  - Animated favorite "Like" button with bouncy spring scale effect and haptic confirmation.
  - Redesigned 56dp elevated primary play/pause transport button with accent color theming.
  - Added album title display from `MediaMetadata.METADATA_KEY_ALBUM`.
- **Status Bar Clock Two-Part Hierarchy**:
  - Refined clock display in `StatusBarRow` with large primary digits and lighter AM/PM suffix matching Samsung One UI 8 typography.
  - Synchronized status bar refresh ticker to clean 60-second intervals.

## [1.8.5] — 2026-09-14

### Added & Enhanced (Tactile Pill Sliders, Interactive Header, Quick Power Menu & 12-Tile Expanded Grid)
- **Modern One UI Tactile Pill Sliders (`BrightnessSlider` & `VolumeSlider`)**:
  - Replaced legacy thin 4dp lines with 44dp rounded pill tracks (`RoundedCornerShape(22.dp)`) featuring active progress fill, smooth spring physics, and full touch/drag responsiveness.
  - **Brightness Pill**:
    - Embedded dynamic sun icon (low/medium/high) with contrast color adaptation.
    - Real-time brightness percentage indicator (e.g. `82%` or `Auto 82%`).
    - Integrated Auto-brightness toggle with vibrant active state styling and haptic feedback.
    - Drag anywhere across the pill or tap to set exact brightness level instantly.
  - **Volume Pill**:
    - Embedded dynamic speaker icon (off/down/up) with tap-to-mute/unmute and long-press to open system Volume Panel (`Settings.Panel.ACTION_VOLUME`).
    - Real-time volume percentage indicator or "Mute" status.
    - Integrated Volume Mixer button for multi-stream volume adjustments.
- **Interactive System Status Bar Header (`StatusBarRow`)**:
  - **Clickable Clock**: Tapping launches Alarm / Clock app (`AlarmClock.ACTION_SHOW_ALARMS`, Samsung Clock package fallback).
  - **Clickable Date**: Tapping launches Calendar (`ACTION_MAIN` with `CATEGORY_APP_CALENDAR`, Samsung Calendar fallback).
  - **Clickable Battery**: Tapping opens Battery Settings (`Settings.ACTION_POWER_USAGE_SUMMARY`).
  - **Top-Right Quick Actions**:
    - Power button: Opens the Quick Power Menu dialog.
    - Settings button: Single-tap opens SuperShade Settings; long-press opens Android System Settings.
- **Quick Power Menu (`PowerMenuDialog`)**:
  - One UI style modal dialog with direct actions:
    - **Power Off**: Executes shutdown via Shizuku shell (`svc power shutdown`).
    - **Restart**: Executes system reboot via Shizuku shell (`svc power reboot`).
    - **Lock Screen**: Triggers immediate lock via `AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN`.
    - **System Dialog**: Invokes Android's native system power dialog via `AccessibilityService.GLOBAL_ACTION_POWER_DIALOG`.
- **Expanded 12-Tile Quick Settings Grid (`QuickSettingsGrid`)**:
  - Expanded mode now scales to 12 tiles (3 rows of 4) with animated height interpolation.
  - Added NFC and Mobile Hotspot to default quick tiles.

## [1.8.4] — 2026-09-14

### Added & Enhanced (System Quick Settings Tile, Tap Customization & Long-Press Controls Menu)
- **SuperShade Quick Settings Tile (`SuperShadeTileService`)**:
  - Integrated native Android Quick Settings tile service registered with `BIND_QUICK_SETTINGS_TILE` and active tile state reporting (`STATE_ACTIVE` / `STATE_INACTIVE`).
  - Tile displays live subtitle feedback: "Active", "Disabled", "Tap to Open", or "Tap for Menu".
  - One-tap "Add Tile to System Quick Settings" button in Settings on Android 13+ (API 33+) via `StatusBarManager.requestAddTileService`.
- **Customizable Single-Tap Action**:
  - Configurable tile single-tap action via `ShadeSettings.qsTileTapAction`:
    - **Toggle On/Off**: Instant toggle of SuperShade replacement and native status bar expansion.
    - **Open Shade**: Instantly collapses the native system Quick Settings panel and pulls down SuperShade.
    - **Show Menu**: Displays the interactive quick controls dialog.
- **Interactive Long-Press Quick Controls Dialog (`TilePreferencesActivity`)**:
  - Registered with `android.service.quicksettings.action.QS_TILE_PREFERENCES` for native long-press support from Android SystemUI / Samsung Quick Panel.
  - Presents a modal bottom sheet dialog with:
    - Primary "Open SuperShade Now" launch action.
    - Master "SuperShade Active" switch with instant status reflection.
    - Segmented selector for single-tap behavior ("Toggle", "Open Shade", "Show Menu").
    - "Block System Status Bar" switch to toggle Samsung One UI suppression.
    - Direct shortcut to open full application settings.

## [1.8.3] — 2026-09-14

### Added & Fixed (Full-Height Pull-Down & Native Status Bar Restoration)
- **Full-Height Edge-to-Edge Shade Pull-Down**:
  - Replaced the restrictive `0.93f` height limit with `.fillMaxSize()` on the shade root container and added `FLAG_LAYOUT_NO_LIMITS` to `ShadeWindowManager`.
  - The notification panel now pulls all the way down to 100% full screen height, unlocking maximum vertical space for notifications on large screens and Galaxy S-series displays.
  - Integrated `navigationBarsPadding()` across the shade layout so bottom notifications, the "Clear all" action, and the dismiss drag handle adaptively clear 3-button navigation bars and gesture navigation pills.
  - Added bidirectional swipe gestures on the Quick Settings chevron handle: swipe down to expand Quick Settings, swipe up to collapse Quick Settings, or swipe up when collapsed to smoothly dismiss the panel.
- **Clean Native Status Bar Restoration When SuperShade is Disabled**:
  - Added `enableExpansionBlocking()` and reflection-based fallback in `StatusBarGovernor` ensuring `cmd statusbar send-disable-flag none` executes synchronously without binder-lifecycle race conditions.
  - `SuperShadeAccessibilityService` now reacts dynamically to `settings.isActive`: when SuperShade is toggled OFF, it instantly detaches the `TYPE_ACCESSIBILITY_OVERLAY` catchment strip, disables SystemUI window state interception, and restores native status bar expansion. When re-enabled, it smoothly re-attaches.
  - `ShadeService` monitors `settings.isActive` to cleanly stop itself and guarantee native status bar restoration upon service destruction.
  - `MainActivity` actively drives `StatusBarGovernor` on "Enable SuperShade" and "Block System Shade" toggle changes for immediate, responsive state transitions.

## [1.8.2] — 2026-09-14

### Fixed & Enhanced (Samsung One UI 8 & Android 16 Status Bar Override)
- **Zero-Flicker Android 16 Window Context Architecture**:
  - Implemented `createWindowContext(display, TYPE_ACCESSIBILITY_OVERLAY, null)` in `SuperShadeAccessibilityService`, resolving Android 12+/16 window token restrictions and ensuring the `TYPE_ACCESSIBILITY_OVERLAY` (Layer ~33) attaches reliably above the system status bar.
  - Expanded top bezel gesture catchment strip to 48dp+ in both `GestureOverlay` and `SuperShadeAccessibilityService` to match Samsung Galaxy S-series display metrics and prevent touches leaking to SystemUI.
- **Pure Java Shizuku `ShadeCommanderService` & StatusBar Governor**:
  - Rewrote `ShadeCommanderService` into clean Java with `@Keep` constructors (`public ShadeCommanderService()` and `public ShadeCommanderService(Context)`) and lifecycle `destroy()`.
  - Fixed `InstantiationException` in `ShizukuServiceStarter` caused by Kotlin runtime reflection linkage in secondary dex files.
  - Successfully applies `cmd statusbar send-disable-flag statusbar-expansion` (`mDisabled1=0x10000`) so the native One UI panel is completely blocked from pulling down while SuperShade is enabled.
- **Samsung One UI Separate Quick Settings Swipe Gesture**:
  - Swiping down from the **top-right edge** (> 72% screen width) opens SuperShade with the Quick Settings grid pre-expanded, mirroring Samsung One UI 6/7/8's native separate panel gesture.
  - Swiping down from the **center or left** opens the primary notifications-first view with compact 1-row Quick Settings.
- **Samsung SystemUI Panel Event Interception**:
  - Added deep interception for Samsung One UI class events (`SecPanelTouchDispatcher`, `SecQuickStatusBarHeader`, `NotificationShadeWindowView`, `CentralSurfaces`).

## [1.8.1] — 2026-09-14

### Added & Overhauled (Heads-Up Popup 2D Gestures, OS Notification Settings & Notification Backend)
- **Heads-Up Popup 2D Gestures & Interactions**:
  - Implemented 2D multi-touch gesture detection engine in `HeadsUpOverlay`:
    - **Swipe Up (< -36dp)**: Dismisses and hides popup toast while safely preserving the notification in the notification shade.
    - **Swipe Left or Right (> 110dp)**: Cancels and dismisses notification from the Android system via `NotificationRepository.cancelAndRemove()`.
    - **Hold Down / Long-Press**: Pauses auto-dismiss countdown timer, vibrates with haptic confirmation, and unfolds native OS notification controls.
- **Native OS Notification Controls on Long-Press**:
  - Integrated full OS settings controls on heads-up popups, standard notification cards, and grouped notification cards:
    - "Notification settings": Launches system app notification preferences (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
    - "Turn off notifications": Directly opens channel-level settings (`Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS`) for the specific notification channel.
    - Snooze options: Quick-snooze directly from the menu for 15 minutes, 1 hour, or 4 hours.
- **Interactive Popup Actions & Inline Replies**:
  - Added direct inline reply text field with send button for messaging notifications via Android `RemoteInput`.
  - Added actionable buttons directly to the heads-up banner for quick actions without needing to open the shade.
- **Notification Spam Prevention & Repository Filtering**:
  - Filtered ongoing system services (`FLAG_ONGOING_EVENT`), non-clearable foreground tasks, group summaries, and SuperShade's own status notifications from generating heads-up popups.
  - Theme-aware styling matching active One UI, Pixel, or Pure Material design systems.

## [1.8.0] — 2026-09-14

### Added & Overhauled (Samsung One UI 8 & Android 16 Alignment, Pure Material Theme & Layout Overhaul)
- **Zero-Flicker Status Bar Suppression (Android 16 & Samsung One UI 8)**:
  - Added `TYPE_ACCESSIBILITY_OVERLAY` gesture capture in `SuperShadeAccessibilityService` positioned at window layer ~33 (above `TYPE_STATUS_BAR` layer ~28), preventing SystemUI from ever receiving top-edge `ACTION_DOWN` gestures.
  - Resolved logic inversion where Accessibility Service skipped interception when Shizuku was connected; now unconditionally collapses native panels (`GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` + `cmd statusbar collapse`).
  - Added `TYPE_WINDOWS_CHANGED` listener to catch Samsung Quick Panel expansion events.
  - Immediate system panel collapse on `ShadeWindowManager.show()` to eliminate peeking.
  - Adjusted `GestureOverlay` to the full status bar height (`status_bar_height` dimen) for reliable swipe detection when accessibility service is initializing.
- **Notification Visibility & Layout Overhaul**:
  - Expanded shade panel height from `fillMaxHeight(0.72f)` to `fillMaxHeight(0.93f)` (~828dp on Galaxy S24/S25/S26 Ultra), providing 550dp+ of clean vertical space for notifications.
  - Created expandable Quick Settings: 1-row compact mode (4 tiles, 84dp) with smooth spring animation to 2-row full mode (8 tiles, 168dp), toggled via interactive chevron.
  - Consolidated Brightness and Volume sliders into a compact side-by-side row (only ~40dp high), saving over 60dp of vertical height.
- **Pure Material System Theme**:
  - Introduced `ShadeTheme.PureMaterial` powered by Android dynamic coloring (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) matching the system wallpaper on Android 12+.
  - Added AMOLED pure black mode (`#000000`) for OLED power savings while maintaining vibrant Monet accent hues.
  - Material 3 Expressive shapes with rounded cards and pill sliders.
  - Upgraded `OneUiTheme` to match Samsung One UI 8 (squircle 22dp, One UI typography, surface containers).
  - Upgraded `PixelTheme` with authentic Google Pixel Material You typography, dark surfaces, and shapes.

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
