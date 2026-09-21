package com.supershade.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.BuildConfig
import kotlinx.coroutines.launch

private fun localReleaseNotes(version: String): String {
    val cleanVersion = version.removeSuffix("-debug").removePrefix("v").trim()
    return when (cleanVersion) {
        "1.9.19" -> """
        ✨ Predictive Scale Peek & Directional Notification Gestures
        • Predictive Scale Rubber-Banding: Pulling up or initiating edge-back gestures now applies physical elastic scale compression (anchored at top-center), matching Android 15/16 and One UI 7 predictive back aesthetics
        • Directional Notification Swiping: Swipe right (Start to End) to quickly Snooze notifications for 1 hour with a warm amber badge and haptic detent; swipe left (End to Start) to Dismiss
        • Animated Action Badges: Notification swipe backgrounds dynamically show animated contextual text badges ("Snooze 1h" / "Dismiss") and scaling icons as the drag distance increases
        • Bottom Grab Handle Refinement: 48dp ergonomic touch target with subtle haptic response for effortless one-handed dismissal
        """.trimIndent()
        "1.9.18" -> """
        ✨ AOSP, Pixel & One UI Fluid Gestures Architecture
        • Universal Back & Side Swipes: Swiping inward from either the left or right screen edge (or pressing the back button) now triggers hierarchical back navigation — closes tile detail sheets, collapses full Quick Settings, or dismisses the shade cleanly
        • 1:1 Live Drag Tracking: The shade panel now tracks your finger in real time when dragging up to dismiss with authentic physical resistance, matching stock AOSP and One UI physics
        • Dynamic Scrim Fade: The dark backdrop scrim smoothly fades in real time proportional to your pull-up distance, elegantly revealing the wallpaper or apps underneath before releasing
        • Velocity-Driven Spring Fling: Flicking up fast or pulling past the dismiss threshold dismisses the shade with fluid spring physics; gentle releases bounce back with elastic snap
        • Fluid Horizontal Panel Slide: Swiping left or right between Notifications and Quick Settings now animates with a fluid horizontal slide and crossfade transition inspired by One UI 8 & iOS Control Center
        • Good Lock Quick Panel Shortcut: Added support for Good Lock One-Handed Operation+ "Quick Panel" gesture to open directly to expanded Quick Settings
        """.trimIndent()
        "1.9.17" -> """
        ✨ Smarter Dismiss Gestures & Good Lock One-Handed Integration
        • Swipe Up Anywhere to Close: Swipe up from anywhere on the shade to dismiss — if Quick Settings is expanded it collapses first, then a second swipe closes the shade entirely
        • Edge-Swipe to Dismiss: Short upward swipe along either the left or right screen edge instantly closes the shade, just like AOSP
        • Good Lock One-Handed Operation: SuperShade now automatically opens when your Good Lock "Notification Panel" side gesture fires — no settings changes needed on your end, even with the system shade blocked
        • Samsung Broadcasts Intercepted: All known Good Lock, One-handed mode, and Edge Panel broadcast paths are caught and redirected to SuperShade
        • Public Open API: Any app (Tasker, Bixby Routines, ADB) can open SuperShade via broadcast: com.supershade.action.OPEN_SHADE
        """.trimIndent()
        "1.9.16" -> """
        ✨ Freehand Tile Drag & Drop (AOSP / One UI Style)
        • Home Screen–Style Tile Reorder: Drag tiles freely anywhere in the Quick Settings grid, just like rearranging apps on a launcher home screen — no arrows, no sliders
        • Long Press → App Settings: Holding down a tile now opens the tile's own system settings or panel, as on stock Android/One UI — not edit mode
        • Edit Mode via Pencil Button Only: Quick tile editing is now exclusively entered via the header Edit (✎) button for a clean, intentional experience
        • Lifted Ghost Tile: The dragged tile lifts off the grid with scale and shadow, and a semi-transparent placeholder shows its original slot
        • Live Slot Preview: Tiles shift in real time as you drag over new positions, giving instant visual feedback before you release
        • Heads-Up Notification Rectangle Fix: Eliminated the faint rectangle that appeared in the center of pop-up (heads-up) notifications caused by an animateContentSize ghost frame
        """.trimIndent()
        "1.9.15" -> """
        ✨ In-Place Quick Settings Customizer & Advanced Notification Controls
        • In-Place Quick Settings Editing: Hold down on any quick tile or tap the header Edit button to customize buttons directly inside the status bar shade without leaving the shade
        • Tactile Reorder & Gestures: Move buttons left/right using responsive arrow keys or fluid horizontal drag gestures with tick haptics
        • Dynamic Available Buttons Tray: Remove buttons with a single tap (×) or add new buttons from an expandable flow drawer of unassigned controls
        • One-Tap Quick Settings Reset: Instantly restore default tile layout with the top-bar Reset action
        • Notification History Shortcut: Access Android's Notification History directly from the notification feed header next to Clear all
        • Expanded Notification Actions: Quick access to snooze, channel notification settings, and inline "Clear group" for stacked notifications
    """.trimIndent()
        "1.9.14" -> """
        ✨ Interactive Appearance Studio Canvas & Liquid Glass Active Sheens
        • Interactive Studio Canvas: Tap mini quick tiles to toggle states, drag or tap the mini brightness bar to scrub brightness with tactile haptics, and tap the theme badge or clock to cycle styles live
        • Luminous Liquid Glass Active Tiles: Active quick tiles and prominent connectivity pills catch luminous corner specular refraction sweeps under Liquid Glass
        • Refractive Liquid Media Player: Media player card adopts specular gradient light overlays matching the fluid shade backdrop
        • Full-Tree Theme Composition: Settings screen and subcomponents inherit local backdrop theme, border widths, and shape scheme seamlessly
    """.trimIndent()
        "1.9.13" -> """
        ✨ Live Appearance Studio Canvas & Unified Refractive Sliders
        • Live Appearance Studio Canvas: Real-time interactive preview canvas in Appearance settings showcasing your theme style, glass opacity, specular sheen, accent color, and custom tile shape instantly as you customize
        • Unified Refractive Sliders: Brightness and Media Volume slider pills and secondary controls now adapt to custom card borders and Liquid Glass specular refraction
    """.trimIndent()
        "1.9.12" -> """
        ✨ Liquid Glass Theme, Specular Card Refraction & Adaptive Quick Toggles
        • Liquid Glass Theme: New theme featuring luminous diagonal specular light sweeps, iridescent border refraction, and vivid fluid depth
        • Specular Refractive Card Borders: Cards, tiles, sliders, and notification feeds catch ambient light with dual-tone specular reflections when Liquid Glass is active
        • Adaptive Quick Toggle Labels: Auto-Rotate tile dynamically relabels to "Portrait" when locked with matched portrait lock iconography; Sound tile dynamically cycles Sound / Vibrate / Mute
        • Expanded System Tile Handlers: Added robust handlers and fallback panels for Airplane Mode, Mobile Hotspot / Tethering, Dark Mode, Location Services, and Battery Saver
    """.trimIndent()
        "1.9.11" -> """
        ✨ Expressive Glass Transparency Slider & Live Haptic Calibration
        • Expressive Transparency Slider: Fine-tune glass opacity continuously from 20% crystal-clear to 100% solid opaque with real-time responsive feedback
        • Harmonious Preset Synchronization: Slider seamlessly stays in sync with Frosted Glass, Blurry, Opaque, and Clear presets without overwhelming or cluttered settings
        • Dynamic Window Compositor Scaling: Window compositor blur behind and scrim contrast automatically adjust in real time to match exact opacity
        • Tactile Potentiometer Detents: Mechanical haptic tick feedback as you slide across 5% calibration thresholds
    """.trimIndent()
        "1.9.10" -> """
        ✨ Frosted Glass, Blurry & Opaque Themes, Global Shape Adaptation, One UI Header & Switcher Dock
        • Glass & Backdrop Themes: Choose between Frosted Glass (acrylic blur & luminous highlights), Blurry (heavy Gaussian blur with deep contrast scrim), Opaque (100% solid surface with zero bleed-through for maximum legibility), and Transparent (translucent see-through glass)
        • Dynamic Window Compositor: Android 12+ real-time blur behind scales dynamically (0px for solid opaque, 115px for frosted, 160px for deep blur) with zero background lag
        • Global Shape Adaptation: Selected Tile Shape (Squircle, Rounded, Circle, Pill, Soft, Leaf, Sharp) harmoniously cascades across all cards, containers, sliders, chips, and pills
        • One UI Header Actions: Single-tap settings gear opens Android device settings, long-press opens SuperShade settings; added permanent Edit (pencil) button to quickly access tile customization
        • Natural Gesture Navigation: Swiping down on notifications in combined mode smoothly expands quick settings; separate mode supports effortless horizontal gestures
        • Bottom Switcher Dock: Panel switcher pill is now hidden by default for clean swipe-driven navigation, can be enabled via settings, and sits ergonomically at the bottom of the screen
        • M3 Expressive Settings & Developer Options: Streamlined settings with collapsible permissions accordion and non-overlapping About actions; 7-tap Easter egg unlocks experimental card border width controls
    """.trimIndent()
        "1.9.9" -> """
        ✨ Maximized Notification Space, Dual-Panel Swipes, Sound Mode & Card Borders
        • Maximized Notification Viewport: Swiping up on quick controls or scrolling notifications tucks quick settings into a minimal 36dp bar, granting ~90% vertical display space to notifications
        • Dual-Panel Pill Tabs & Horizontal Gestures: Switch smoothly between Notifications and dedicated full Quick Settings via top pill tabs or fluid horizontal swipe gestures
        • Instant Tile Loading & Sound Mode: Pre-seeded tile cache eliminates loading flicker; renamed Mute to Sound with dynamic Sound/Vibrate/Mute cycling and live icon states
        • Uniform Card Borders & Outlines: Choose between None (0dp), Thin (1dp default), Distinct (1.5dp), and Bold (2dp) border outlines for all cards, tiles, and dialogs
        • Dynamic Rotation State: Live Screen Rotation tile icon and label adapt to portrait lock vs auto-rotate
    """.trimIndent()
        "1.9.8" -> """
        ✨ Fluid Gesture Navigation, Zero-Clip Peek Overlays & Enhanced Touch Points
        • Seamless Viewport Transitions: Scrolling notifications upward automatically collapses expanded Quick Settings into compact mode, dynamically allocating 100% of screen height to notifications
        • Samsung & Pixel Pull-to-Expand: Pulling down at the top of the notification feed or swiping down across the header and quick settings smoothly expands the full quick settings panel
        • Natural Quick Closes: Flinging upward from anywhere, swiping up on the bottom handle, or tapping the bottom bar instantly dismisses the shade with spring damping physics
        • Edge-Safe Heads-Up Overlays: Completely eliminated text clipping on peek cards with calibrated internal margins that clear 26dp rounded corner contours
        • Ergonomic Touch Targets: Upgraded notification snooze, group expansion chevrons, and inline action buttons to 38-40dp touch surfaces with mechanical tactile feedback
    """.trimIndent()
        "1.9.7" -> """
        ✨ Multi-Size Quick Tiles, Leaf/Sharp Shapes, Reorder Studio & Double-Tap Sleep
        • Multiple Tile Sizes: Choose between Compact (58dp, saves vertical space for notifications & media), Standard (72dp One UI 8), or Tall & Spacious (84dp, large touch targets and thumb readability)
        • Expanded Tile Shapes: Added Asymmetric Leaf (24dp/8dp organic curve) and Sharp Modern (6dp technical minimal) alongside Squircle, Rounded, Stadium Circle, Pill, and Soft Minimal
        • Tile Customizer & Reorder Studio: Upgraded bottom sheet with live shade mini-preview, categorized tile drawer (Connectivity, Display, Audio/Power, Utilities), position indexing, and quick move-to-top/bottom
        • Double-Tap Header to Sleep: Double-tap anywhere on the status bar header clock or empty area to instantly sleep/lock the device via accessibility or power governor
        • Persistent Hardware Torch Level: Restores your preferred flashlight brightness level (1–5) whenever toggled ON
        • Anti-Clipping Guarantees: Text in quick tile detail panels, headers, and metric tiles now automatically wraps and adapts to prevent any cutoffs
    """.trimIndent()
        "1.9.6" -> """
        ✨ Split Shade Pull Presets, In-Shade Tile Sub-Panels & Hardware Torch Slider
        • Split Status Bar Pull Presets: Choose between 5 split modes including One UI 8 / iOS 50/50 Half & Half, Standard 70/30, Left-Handed 30/70, Notifications Only, or Quick Settings Only with a live interactive visualizer
        • In-Shade Tile Sub-Panels: Long-press Wi-Fi, Bluetooth, or Flashlight tiles to open sleek, in-shade detail sheets without getting thrown into Android system settings
        • Multi-Level Hardware Torch Slider: Direct in-shade flashlight panel with live on/off toggle, 5-level hardware brightness slider (Android 13+ CameraManager), quick level chips, and discrete haptic detents
        • Live Wi-Fi & Bluetooth Info: In-shade sub-panels display active Wi-Fi SSID, frequency band (2.4/5/6 GHz), link speed, IP address, and Bluetooth accessory details
        • Notification Snooze Enhancements: Added quick header snooze trigger and expanded snooze presets (15m, 30m, 1h, 2h, 4h, 8h) with tactile confirmations
    """.trimIndent()
        "1.9.5" -> """
        ✨ Custom Tile Shapes, Multi-Density Grid & Anti-Clipping Polish
        • Custom Tile Shapes: Choose between Squircle (22dp One UI 8), Rounded (16dp), Circle / Stadium (50%), Stadium Pill (28dp), or Soft Minimal (12dp) with real-time interactive previews
        • Multi-Density Quick Tile Grid: Tailor your layout with 3-column Comfortable (large reach), 4-column Standard, or 5-column Compact grids
        • Prominent Dual Connectivity Toggle: Choose whether to display top Wi-Fi & Bluetooth island pills in expanded Quick Settings
        • Zero Text Cut-offs & Cutout Insets: Comprehensive audit and resolution of text cutoffs across peek cards, tiles, headers, and bottom sheets with dynamic font scaling and camera cutout padding
        • Interactive Tile Manager: Enable, disable, and customize your active quick settings tiles directly from settings
    """.trimIndent()
        "1.9.4" -> """
        ✨ Frosted Peek Notifications, Dynamic Music Visualizer & Zero-Drift Clock
        • Frosted Acrylic Heads-Up Notifications: Peek notification cards now feature real-time 50–90px window background blur, 26dp One UI 8 rounded contours, and translucent frosted glass styling
        • Tactile Peek Haptics: Detent haptic ticks on swipe-up hide, swipe-to-dismiss, long-press settings, and action button interactions
        • Dynamic Audio Equalizer: Animated 4-bar rhythmic music visualizer beside active track titles reflecting live playback status
        • Live Streaming Broadcast Indicator: Dedicated pulsating status badge for endless radio streams and podcasts without static durations
        • Audio Output Switcher Haptics: Tactile physical feedback when switching audio endpoints between phone speakers and Bluetooth accessories
        • Zero-Drift Minute Synchronization: Status bar header clock now perfectly syncs to the exact zero-second boundary of each minute
    """.trimIndent()
    "1.9.3" -> """
        ✨ Frosted Acrylic Backdrop, Tactile Haptics Engine & Audio Output Switcher
        • Frosted Acrylic Glassmorphism: Deep 75–115px real-time backdrop blur paired with a translucent acrylic surface for authentic glassmorphism
        • Enterprise Tactile Haptics: Full physical feedback engine using native Android vibration primitives across tiles, sliders, notifications, and gestures
        • Dynamic Audio Output Switcher: Live media output chip identifying connected Bluetooth earbuds or speakers with one-tap routing panel
        • Multi-Level Flashlight Control: Android 13+ hardware torch strength integration with level scaling
        • Tactile Slider Notches: Physical detent ticks when scrubbing brightness and volume sliders
        • Spring Damping Physics: Fluid iOS/One UI 8 spring physics for shade entrance and Quick Settings expansion
    """.trimIndent()
    "1.9.2" -> """
        ✨ Fluid Status Bar Gestures, One UI 8 Action Pills & Control Center Polish
        • Enhanced Status Bar Pull Zone: Adaptive capture zone with density-scaled drag thresholds ensures 100% reliable swipe-downs without missed pulls
        • Split Quick Settings Pull: Pull down from right 30% for instant expanded Quick Settings; pull left 70% for Notifications & compact QS
        • One UI 8 Action Pills: Notification cards feature filled tonal pill action buttons with smooth click and reply flows
        • Integrated Quick Reply: Full-width rounded reply pill with enter/send IME action and instant dispatch
        • Quick Settings Tile Polish: Active indicator status dots, micro-elevation, and refined tactile feedback on tap and long-press
        • Control Center Pill Handle: Tactile rounded drag handle between quick settings and sliders for effortless collapse and expansion
    """.trimIndent()
    "1.9.1" -> """
        ✨ One UI 8 Notification Cards, Edge-to-Edge Settings & Visual Polish
        • Authentic One UI 8 Notifications: 38dp prominent app icon badge with fallback letter avatar, streamlined app/time header, and bold subject hierarchy
        • Grouped Stacks Modernization: Matching One UI 8 icon styling with count pill and sub-item separation
        • Edge-to-Edge Settings UI: Removed redundant top bar header for a clean, immersive status bar transition into the Hero Header
        • Deep Crisp Backdrop: Increased shade surface opacity to prevent background bleed-through while keeping glass aesthetic
        • Enhanced Container Contrast: Richer slate and graphite surfaces in dark, AMOLED, and light themes
        • Formatted Release Highlights: Structured What's New presentation with feature cards and icons
    """.trimIndent()
    "1.9.0" -> """
        ✨ One UI 8 Settings Hub, Precision Gestures & Notification Polish
        • Modern Redesigned Settings UI: One UI 8 Hero header with live active badge, quick preview actions, and unified permissions island
        • Precision Status Bar Gestures: Refined touch capture zone tightly bounded to status bar with zero overlap onto app toolbars
        • Mechanical Haptics: Tactile clock-tick haptic feedback immediately on crossing swipe threshold
        • Right-Side Quick Settings Expansion: Pull down top-right edge for instant expanded Quick Settings grid
        • Fluid Dynamic Sizing: Quick settings grid adapts smoothly to any screen scale or font size with spring animation
        • Proportional Dismiss Trash Icon: Dynamic scaling and alpha feedback proportional to swipe distance
        • Samsung Pill Clear-All: Redesigned clear button with One UI pill chip styling
    """.trimIndent()
    "1.8.9" -> """
        ✨ Gestures, Tile Fixes, Categorization & Direct Installer
        • Responsive Swipe-to-Dismiss: Lighter 35% swipe threshold and bidirectional dismissal on individual and grouped notifications
        • Enhanced Categorization: Added Productivity (Tasks/Calendar/Notes) and Media categories with smart app heuristics; clean dynamic bar that hides empty categories
        • Direct Shade Presentation: Overlay presentation wired directly to accessibility & tile services for instant, reliable swipe-down response
        • Quick Settings Tile Fixes: Restored Bluetooth toggling on One UI, optimistic visual state, and robust driver verification
        • In-App Update Installer: Integrated system DownloadManager with live progress notifications and direct package installation
    """.trimIndent()
    "1.8.8" -> """
        ✨ Dynamic Color Palette, One UI 8 Connectivity Cards & Live Subtitles
        • Dynamic Color Palette: Choose from Galaxy Blue, Emerald, Violet, Amber, Coral, or Dynamic Monet
        • One UI 8 Dual Connectivity Cards: Prominent top-row Wi-Fi & Bluetooth island cards with live SSID/device labels
        • Real-time rich tile subtitles: Bluetooth connected device name, Sound/Vibrate/Mute mode, Auto-rotate, and Hotspot status
        • Cohesive theming across active tiles, buttons, swatches, and sliders
    """.trimIndent()
    "1.8.7" -> """
        🎨 One UI 8 & Android 16 Island Architecture & AMOLED Theming
        • Unified 26dp rounded island card for Quick Settings toggles with smooth spring height animation
        • Dual Sliders Island: Brightness & Volume enclosed in matching 24dp rounded container
        • Floating notification cards with surfaceContainer elevation and soft borders
        • Deep AMOLED mode: pure black (#000000) backdrop with rich dark graphite cards for Galaxy displays
        • Tactile haptic feedback on tile taps and category chip filters
    """.trimIndent()
    "1.8.6" -> """
        🚀 GitHub Releases, In-App Auto-Update & Media Player Polish
        • Fixed GitHub release CI — release APKs and tags now publish automatically without failure
        • Hardened semver parsing so updates are reliably detected and notified
        • Check for Updates button features active loading spinner and contextual status feedback
        • Media Player full-bleed palette backdrop, like/favorite button, and prominent 56dp transport controls
        • Refined status bar clock with One UI two-part layout and separate AM/PM badge
    """.trimIndent()
    "1.8.5" -> """
        🎛️ Tactile Pill Sliders, Quick Power Menu & 12-Tile Grid
        • 44dp tactile pill sliders for Brightness and Volume with real-time percentage and spring physics
        • Clickable Status Bar header: Clock launches alarms, Date launches calendar, Battery opens usage
        • Quick Power Menu: Power Off, Reboot, Lock Screen, and System Dialog shortcuts
        • 12-Tile expanded Quick Settings grid (3 rows of 4) with smooth spring transitions
    """.trimIndent()
    "1.8.4" -> """
        🔘 Quick Settings Tile & Control Center
        • Native system Quick Settings tile to toggle or open SuperShade
        • Configurable tap action: Toggle, Open Shade, or Quick Menu
        • Long-press Quick Controls bottom sheet
    """.trimIndent()
    "1.8.3" -> """
        📱 Full-Height Pull-Down & Native Status Bar Restoration
        • 100% full-screen pull-down height with adaptive navigation bar padding
        • Clean native status bar restoration when SuperShade is disabled
    """.trimIndent()
    "1.8.2" -> """
        ⚡ One UI 8 & Android 16 Interception
        • Zero-flicker WindowContext architecture for Android 16
        • Pure Java Shizuku commander for privileged system status bar disable
        • Samsung separate Quick Settings gesture (swipe top-right)
    """.trimIndent()
    "1.8.0", "1.8.1" -> """
        🌟 Android 16 Architecture Overhaul
        • 2D multi-touch heads-up popup gestures (swipe up to hide, swipe sideways to dismiss)
        • Pure Material 3 Expressive theme with dynamic tonal palettes
        • Native OS notification settings integration on long-press
    """.trimIndent()
    "1.7.0" -> """
        ✨ Zero-ADB Architecture & Media Controls
        • Zero-ADB native shade replacement via Accessibility Service
        • Responsive volume and brightness sliders with drag protection
        • Non-root quick setting toggles with live system state observers
    """.trimIndent()
    "1.6.0" -> """
        🔦 Smarter toggles & richer notifications
        • Flashlight tile works without Shizuku — toggles torch directly via CameraManager and tracks state live
        • Alarm tile shows your next scheduled alarm time as a subtitle
        • Notification timestamps switch from "2h ago" to an absolute clock time (e.g. 2:47 PM) once they're over an hour old
        • Long-press a notification group to snooze the entire stack — each card inside also supports snooze individually
        • Notifications with an attached image (BigPicture style) show a rounded preview when expanded
    """.trimIndent()
    "1.5.0" -> """
        🎯 Smarter tiles & notification controls
        • Long-press any QS tile to jump straight to its Settings page — no Shizuku required
        • Wi-Fi tile now shows the connected network name; DND tile shows the active mode
        • Notification cards show a live progress bar for downloads, installs, and similar ongoing operations
        • Long-press any notification to snooze it for 15 minutes, 1 hour, or 4 hours — it re-appears automatically
    """.trimIndent()
    "1.4.0" -> """
        🎛️ Controls & live status
        • Volume slider in the shade — drag to change media volume; syncs with hardware keys
        • Auto-brightness toggle pill on the brightness row — tap to enable adaptive screen brightness
        • Media progress bar is now seekable — drag to any position and release to jump there
        • Heads-up peek cards swipe left/right to dismiss with velocity-aware spring physics
        • Live network speed shown below the date: ↓ download and ↑ upload update every second
    """.trimIndent()
    "1.3.1" -> """
        🔧 Polish & correctness
        • Heads-up toasts no longer fire for group-summary or updated notifications — only genuinely new ones
        • Brightness slider no longer hammers Settings.System during drag — one write on finger-up
        • Notification stacks now swipe-to-dismiss the whole group in one gesture
        • Stack ghost cards now correctly peek behind the bottom of the main card, not above it
        • Gmail, Maps, and other Google/OEM apps now land in their correct category (Messages, Social, Email) instead of System
    """.trimIndent()
    "1.3.0" -> """
        📬 Notification grouping
        • Notifications from the same app and group are stacked into a single card
        • Collapsed stack shows the latest message with a "+N more" hint and ghost cards behind it
        • Tap the card or the arrow to expand — each notification inside is individually swipe-dismissible
        • Stacks of 3+ show a two-layer peek effect so you can see depth at a glance
    """.trimIndent()
    "1.2.9" -> """
        🎨 Richer UI — gradients, inline reply, animated brightness
        • Media card background now pulls dominant color from album art with a live gradient
        • Brightness slider has a gradient track (dark → warm yellow) and icons that fade with brightness
        • Tap a "Reply" action on any messaging notification to type and send inline — no app switch needed
        • Heads-up peek card shows sender avatar at full circle size when available
    """.trimIndent()
    "1.2.8" -> """
        🤌 Fluid interactions & live tiles
        • Swipe up on the drag handle to dismiss the shade — spring physics, velocity-gated
        • QS tiles refresh every 5 s while the shade is open so state stays accurate
        • FULL_TOGGLE tiles dim to 50 % when Shizuku is absent — tap still opens Settings
        • Heads-up peek card shows sender avatar (large icon) instead of the tiny app icon
    """.trimIndent()
    "1.2.7" -> """
        ✨ Visual quality & notification richness
        • Frosted-glass blur behind the shade panel on Android 12+ devices
        • Lighter scrim on Android 12+ — blur provides contrast, no dark veil needed
        • Large notification icons (sender avatars, images) displayed in cards
        • Group chat / conversation title shown next to app name in header
        • App icon and large icon loading moved off the composition thread
    """.trimIndent()
    "1.2.6" -> """
        🔓 Shizuku-free mode
        • SuperShade now activates with just Notification Access + Display Over Other Apps
        • Shizuku is optional — it enables direct QS tile toggling
        • Without Shizuku, tile taps open the relevant Settings screen instead
        • "Block system shade" toggle is greyed out when Shizuku is unavailable
        • Shizuku status card uses a neutral style when not connected
    """.trimIndent()
    "1.2.5" -> """
        🌙 App Dark Mode & Notification Center
        • App Dark Mode settings (System, Dark, Light, AMOLED)
        • Material 3 dynamic coloring and deep AMOLED theme
        • Instant notification center refresh on shade open
        • Real app icons displayed on each notification card
        • Comprehensive title, body, and action fallback parsing
        • Intelligent group notification handling
    """.trimIndent()
    "1.2.4" -> """
        🚀 Core functionality & Shade activation
        • Gesture overlay spans status bar with responsive touch detection
        • Automatic system shade override on Shizuku connection
        • Real privileged toggles for Wi-Fi, Bluetooth, Dark Mode, Rotation, Airplane Mode, Location, and Battery Saver
        • Live tile state indicators reflect real system settings
        • Tap any notification to launch the app directly
        • One-tap "Open Shade Preview" in Settings
        • "Block system shade" setting to suppress system panel while SuperShade is active
    """.trimIndent()
    "1.2.3" -> """
        ✨ UI polish
        • Panel capped at 72 % height — scrim always visible below
        • Swipe-to-dismiss shows a red delete background
        • "Clear all" button with notification count header
        • Notification cards animate in and out
        • Media card shows mm:ss / mm:ss time below the seek bar
        • Brightness slider now has a dim icon on the left
    """.trimIndent()
    "1.2.1", "1.2.0" -> """
        🎨 OneUI 8.5 redesign
        Samsung-accurate shade panel — large Light clock, Galaxy Blue accent, 22 dp tile corners, and transparent inactive category chips.

        🐛 16 backend fixes
        • Notification dismiss now also cancels from the system tray
        • Quick Settings tiles work on Samsung (full component names)
        • Media playback position stays in sync after seeking
        • Theme selection takes effect immediately without restart
        • Boot auto-start is crash-safe with a hard timeout
        • Shizuku rebinds automatically after a restart
    """.trimIndent()
    "1.1.0" -> """
        ✨ Initial public release
        Functional custom shade with notifications, Quick Settings, brightness, and media controls.
    """.trimIndent()
    else -> "Thanks for keeping SuperShade up to date!"
    }
}

private data class ReleaseNoteItem(
    val title: String,
    val description: String,
)

private fun parseReleaseNotes(raw: String): Pair<String, List<ReleaseNoteItem>> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    val header = lines.firstOrNull { !it.startsWith("•") } ?: "What's new in SuperShade"
    val items = lines.filter { it.startsWith("•") }.map { line ->
        val content = line.removePrefix("•").trim()
        val parts = content.split(":", limit = 2)
        if (parts.size == 2) {
            ReleaseNoteItem(parts[0].trim(), parts[1].trim())
        } else {
            ReleaseNoteItem(content, "")
        }
    }
    return Pair(header, items)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    releaseNotes: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val notes = releaseNotes.ifBlank { localReleaseNotes(BuildConfig.VERSION_NAME) }
        val (headerSubtitle, items) = remember(notes) { parseReleaseNotes(notes) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero Header Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "What's New",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = headerSubtitle.removePrefix("✨").trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Feature Highlights Cards
            if (items.isNotEmpty()) {
                for (item in items) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (item.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(
                    text = "Explore SuperShade",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
