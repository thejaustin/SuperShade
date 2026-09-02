# Changelog

All notable changes to SuperShade are documented here.
Releases follow [Semantic Versioning](https://semver.org/).

---

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
