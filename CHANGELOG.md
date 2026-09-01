# Changelog

All notable changes to SuperShade are documented here.
Releases follow [Semantic Versioning](https://semver.org/).

---

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
