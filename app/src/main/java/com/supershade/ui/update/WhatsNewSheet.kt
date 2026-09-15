package com.supershade.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supershade.BuildConfig
import kotlinx.coroutines.launch

private fun localReleaseNotes(version: String): String = when (version) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "What's new in ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            val notes = releaseNotes.ifBlank { localReleaseNotes(BuildConfig.VERSION_NAME) }
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Got it")
            }
        }
    }
}
