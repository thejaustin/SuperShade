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
    "1.2.4" -> """
        🚀 Core functionality & Shade activation
        • Gesture overlay spans status bar with responsive touch detection
        • Automatic system shade override on Shizuku connection
        • Real privileged toggles for Wi-Fi, Bluetooth, Dark Mode, Rotation, Airplane Mode, Location, and Battery Saver
        • Live tile state indicators reflect real system settings
        • Tap any notification to launch the app directly
        • One-tap "Open Shade Preview" in Settings
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
