# SuperShade

[![GitHub Release](https://img.shields.io/github/v/release/thejaustin/SuperShade?style=flat-square&color=36BCF7&labelColor=1a1b27&label=Latest)](https://github.com/thejaustin/SuperShade/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/thejaustin/SuperShade/total?style=flat-square&color=3DDC84&labelColor=1a1b27&logo=android&label=Downloads)](https://github.com/thejaustin/SuperShade/releases)
[![Stars](https://img.shields.io/github/stars/thejaustin/SuperShade?style=flat-square&color=FFD700&labelColor=1a1b27&logo=github&label=Stars)](https://github.com/thejaustin/SuperShade/stargazers)

A fully custom Android notification shade built with Jetpack Compose — One UI, Pixel, and Pure Material styles, with Shizuku-powered status bar control and no root required.

SuperShade replaces Android's stock notification shade with a custom overlay that matches the aesthetic of modern launchers. Pull down anywhere on the status bar to open it: quick settings, notifications, media controls, brightness and volume sliders — all in one gesture, styled the way you want.

---

## ✨ Features

### 🎨 Three Shade Styles
Switch between **One UI**, **Pixel**, and **Pure Material** layouts in settings. Each style has its own tile grid shape, card geometry, and motion feel. All three support **System, Dark, Light, and AMOLED** theme modes plus six accent color options (Galaxy Blue, Emerald, Violet, Amber, Coral, Dynamic Monet).

### ⚡ Quick Settings
- 12-tile expanded grid with tap, long-press, and subtitle support
- One UI 8 dual connectivity cards (Wi-Fi + Bluetooth) with live signal/device info
- Real-time tile subtitles: connected device name, sound mode, hotspot state, auto-rotate
- Direct Bluetooth device connect from an expandable paired-devices sheet

### 🔊 Brightness & Volume Sliders
- 44dp pill-shaped sliders with spring-physics drag
- Brightness pill: auto-brightness toggle, live percentage, adaptive sun icon
- Volume pill: tap-to-mute, long-press for system Volume Panel, Volume Mixer button

### 🔔 Notifications
- Categorized filter bar (All, Messaging, Social, Media, Productivity, System…)
- Bidirectional swipe-to-dismiss with position threshold control
- Group collapse/expand with smart header summarization
- Notification pins, snooze, undo pill
- Inline direct reply without leaving the shade

### 🎵 Media Controls
- Album-art backdrop with dynamic `Palette` color blending and gradient scrim
- Spring-animated transport buttons (play/pause, skip) with haptic feedback
- Favorite/Like button with bouncy scale animation

### 🖐️ Status Bar Gestures
- 72%/28% touch zone split: left side opens SuperShade, right side opens system shade
- Mechanical haptic tick when crossing the pull threshold
- Auto-bypass when the shade is already open

### 🔧 Settings
- One UI 8–style settings hub with live status badge and master toggle
- Permissions hub with direct-action pills (Notification Access, Overlay, Accessibility, Shizuku)
- In-app auto-update checker pulling from GitHub Releases

---

## 📋 Requirements

- **Android 10+** (Android 12+ recommended for Material You colors)
- **[Shizuku](https://github.com/RikkaApps/Shizuku)** running (ADB or root) — required for status bar suppression and Quick Settings tile control
- Notification Access and Overlay permissions granted

---

## 📦 Installation

Download the latest APK from the [Releases](https://github.com/thejaustin/SuperShade/releases) page and install it. Then:

1. Open SuperShade and grant the required permissions shown in the Permissions Hub.
2. Start the Shizuku service if not already running (`adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`).
3. Pull down from the top-left of your status bar to open the shade.

---

<details>
<summary>🛠️ Building from Source</summary>

Requires Android SDK (API 34+) and JDK 17+.

```bash
git clone https://github.com/thejaustin/SuperShade.git
cd SuperShade

# Debug build + install over ADB
bash scripts/dev/build-install-debug.sh

# Or just compile-check
bash scripts/dev/check-build.sh
```

ADB loopback works without Wi-Fi: `adb connect 127.0.0.1:5555`

After install, verify the service is running:
```bash
adb shell dumpsys activity services | grep ShadeService
```

</details>

<details>
<summary>📃 License & Credits</summary>

[Apache 2.0](LICENSE)

Built with [Jetpack Compose](https://developer.android.com/jetpack/compose), [Koin](https://insert-koin.io/), [Shizuku](https://github.com/RikkaApps/Shizuku), and [Coil](https://coil-kt.github.io/coil/).

</details>
