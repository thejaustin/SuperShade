package com.supershade.ui.shade.pixel

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.getCardBorder
import com.supershade.viewmodel.StatusBarState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pixel (Material 3 Expressive / Android 15/16) Header.
 * Features an expressive Google Sans display clock, date pill chip,
 * live battery capsule with charging accents, and a tonal action dock.
 */
@Composable
fun PixelHeader(
    statusBar: StatusBarState,
    isEditing: Boolean = false,
    onOpenPowerMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDeviceSettings: () -> Unit = {},
    onOpenEdit: () -> Unit = {},
    onLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    var timeText by remember { mutableStateOf(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())) }
    var ampmText by remember { mutableStateOf(SimpleDateFormat("a", Locale.getDefault()).format(Date())) }
    var dateText by remember { mutableStateOf(SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            timeText = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
            ampmText = SimpleDateFormat("a", Locale.getDefault()).format(now)
            dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            delay(1000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Left: Expressive Clock & Date Pill Chip ──────────────────────────
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Display Clock
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.lightTap()
                        try {
                            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 38.sp,
                        letterSpacing = (-0.75).sp,
                        lineHeight = 40.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ampmText.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Material 3 Expressive Chips Row: Date & Weather
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Date Chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                    border = getCardBorder(alpha = 0.20f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptics.lightTap()
                            try {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val calIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("content://com.android.calendar/time")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(calIntent)
                                } catch (_: Exception) {}
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Weather Pill Chip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                    border = getCardBorder(alpha = 0.20f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptics.lightTap()
                            try {
                                val weatherIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://www.google.com/search?q=weather")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(weatherIntent)
                            } catch (_: Exception) {}
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "72°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Center space: double-tap to lock screen without interfering with buttons
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onDoubleClick = {
                        haptics.heavyClick()
                        onLockScreen()
                    },
                )
        )

        // ── Right: Action Buttons & Battery Capsule ──────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Edit Tiles Button (Expressive squircle)
            ExpressiveHeaderButton(
                icon = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = if (isEditing) "Done" else "Edit tiles",
                isActive = isEditing,
                onClick = {
                    haptics.sheetDetent()
                    onOpenEdit()
                },
            )

            // Settings Button (Tap for Android settings, long-press for SuperShade)
            val settingsInteraction = remember { MutableInteractionSource() }
            val settingsPressed by settingsInteraction.collectIsPressedAsState()
            val settingsScale by animateFloatAsState(
                targetValue = if (settingsPressed) 0.92f else 1.0f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "settingsScale",
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = settingsScale
                        scaleY = settingsScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        interactionSource = settingsInteraction,
                        indication = null,
                        onClick = {
                            haptics.sheetDetent()
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                                onOpenDeviceSettings()
                            } catch (_: Exception) {}
                        },
                        onLongClick = {
                            haptics.sheetDetent()
                            onOpenSettings()
                        },
                        role = Role.Button,
                    )
                    .semantics {
                        contentDescription = "Settings. Tap for Android Settings, long press for SuperShade Settings"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Power Menu Button
            ExpressiveHeaderButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "Power options",
                isActive = false,
                onClick = {
                    haptics.sheetDetent()
                    onOpenPowerMenu()
                },
            )

            // Battery Capsule Pill
            val batteryPct = statusBar.batteryPct
            val isCharging = statusBar.isCharging
            val batteryIcon = when {
                isCharging -> Icons.Default.BatteryChargingFull
                batteryPct >= 90 -> Icons.Default.BatteryFull
                batteryPct >= 75 -> Icons.Default.Battery6Bar
                batteryPct >= 55 -> Icons.Default.Battery5Bar
                batteryPct >= 35 -> Icons.Default.Battery4Bar
                batteryPct >= 20 -> Icons.Default.Battery3Bar
                else -> Icons.Default.Battery2Bar
            }
            val batteryTint = when {
                isCharging -> Color(0xFF4CAF50)
                batteryPct < 20 -> Color(0xFFEF5350)
                else -> MaterialTheme.colorScheme.primary
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = getCardBorder(alpha = 0.25f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        haptics.lightTap()
                        try {
                            context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (_: Exception) {}
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "$batteryPct%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = null,
                        tint = batteryTint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive tactile header action button with spring scale feedback.
 */
@Composable
private fun ExpressiveHeaderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "headerBtnScale",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "headerBtnBg",
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "headerBtnIcon",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
