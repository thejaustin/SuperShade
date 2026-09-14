package com.supershade.ui.tile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supershade.MainActivity
import com.supershade.R
import com.supershade.service.ShadeService
import com.supershade.service.SuperShadeTileService
import com.supershade.settings.QsTileTapAction
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.ui.theme.SuperShadeAppTheme
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Interactive options modal shown when the user long-presses the SuperShade Quick Settings
 * tile, or taps the tile when configured to "Show Menu".
 */
@OptIn(ExperimentalMaterial3Api::class)
class TilePreferencesActivity : ComponentActivity() {

    private val settings: ShadeSettings by inject()
    private val governor: StatusBarGovernor by inject()
    private val shadeViewModel: ShadeViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val darkThemeMode by settings.darkThemeMode.collectAsState(initial = com.supershade.ui.theme.DarkThemeMode.SYSTEM)
            val isActive by settings.isActive.collectAsState(initial = false)
            val blockSystemShade by settings.blockSystemShade.collectAsState(initial = true)
            val tapAction by settings.qsTileTapAction.collectAsState(initial = QsTileTapAction.TOGGLE_ACTIVE)
            val scope = rememberCoroutineScope()

            SuperShadeAppTheme(mode = darkThemeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finish() },
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Drag handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            )

                            // Title & Dismiss
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_notification_shade),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        text = "SuperShade Quick Controls",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            // Primary Action: Open SuperShade
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (!isActive) {
                                            settings.setActive(true)
                                            toggleShadeService(true)
                                        }
                                        governor.collapse()
                                        val intent = Intent(this@TilePreferencesActivity, ShadeService::class.java).apply {
                                            action = ShadeService.ACTION_OPEN_SHADE
                                        }
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                startForegroundService(intent)
                                            } else {
                                                startService(intent)
                                            }
                                        } catch (_: Exception) {}
                                        shadeViewModel.open()
                                        finish()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open SuperShade Now")
                            }

                            HorizontalDivider()

                            // Master Switch: Enable / Disable SuperShade
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SuperShade Active",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = if (isActive) "Replacement shade is running" else "Disabled (native status bar active)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = isActive,
                                        onCheckedChange = { active ->
                                            toggleShadeService(active)
                                            scope.launch {
                                                settings.setActive(active)
                                                if (!active) {
                                                    governor.enableExpansion()
                                                } else if (blockSystemShade) {
                                                    governor.disableExpansion()
                                                }
                                                SuperShadeTileService.requestUpdate(this@TilePreferencesActivity)
                                            }
                                        },
                                    )
                                }
                            }

                            // Single Tap Customization
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.TouchApp,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "Tile Single-Tap Action",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Text(
                                        text = "Choose what happens when tapping the tile in Quick Settings:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                                    )

                                    val actions = listOf(
                                        QsTileTapAction.TOGGLE_ACTIVE to "Toggle",
                                        QsTileTapAction.OPEN_SHADE to "Open Shade",
                                        QsTileTapAction.SHOW_MENU to "Show Menu",
                                    )
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        actions.forEachIndexed { index, (action, label) ->
                                            SegmentedButton(
                                                selected = tapAction == action,
                                                onClick = {
                                                    scope.launch {
                                                        settings.setQsTileTapAction(action)
                                                        SuperShadeTileService.requestUpdate(this@TilePreferencesActivity)
                                                    }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(index, actions.size),
                                                icon = {},
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            // Block Native Status Bar Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Block System Status Bar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Suppresses Samsung One UI panel expansion when active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = blockSystemShade,
                                    onCheckedChange = { block ->
                                        scope.launch {
                                            settings.setBlockSystemShade(block)
                                            if (isActive) {
                                                if (block) governor.disableExpansion() else governor.enableExpansion()
                                            }
                                        }
                                    },
                                )
                            }

                            // Full Settings Link
                            OutlinedButton(
                                onClick = {
                                    startActivity(Intent(this@TilePreferencesActivity, MainActivity::class.java))
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open Full App Settings")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toggleShadeService(enable: Boolean) {
        val intent = Intent(this, ShadeService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }
}
