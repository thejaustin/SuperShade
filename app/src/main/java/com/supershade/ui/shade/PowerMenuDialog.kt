package com.supershade.ui.shade

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.getCardBorder
import kotlinx.coroutines.delay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh

enum class PowerConfirmAction {
    NONE, POWER_OFF, RESTART, RESTART_SYSTEMUI, REBOOT_RECOVERY, REBOOT_BOOTLOADER
}

/**
 * Modern One UI 8/9 style Quick Power Menu dialog with two-step safety confirmation,
 * spring-driven press feedback, Shizuku advanced reboot actions, and flagship mechanical haptics.
 */
@Composable
fun PowerMenuDialog(
    onDismiss: () -> Unit,
    onLockScreen: () -> Unit,
    onRestart: () -> Unit,
    onPowerOff: () -> Unit,
    onSystemPowerDialog: () -> Unit,
    onRestartSystemUI: () -> Unit = {},
    onRebootRecovery: () -> Unit = {},
    onRebootBootloader: () -> Unit = {},
    isShizukuActive: Boolean = false,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    var confirmAction by remember { mutableStateOf(PowerConfirmAction.NONE) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Auto-revert confirmation after 4 seconds of inactivity
    LaunchedEffect(confirmAction) {
        if (confirmAction != PowerConfirmAction.NONE) {
            delay(4000L)
            confirmAction = PowerConfirmAction.NONE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = getCardBorder(alpha = 0.35f),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (confirmAction != PowerConfirmAction.NONE) {
                            confirmAction = PowerConfirmAction.NONE
                        }
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = confirmAction,
                    transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(120)) },
                    label = "powerTitle",
                ) { state ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (state) {
                                PowerConfirmAction.POWER_OFF -> "Power off"
                                PowerConfirmAction.RESTART -> "Restart"
                                PowerConfirmAction.RESTART_SYSTEMUI -> "Restart SystemUI"
                                PowerConfirmAction.REBOOT_RECOVERY -> "Reboot Recovery"
                                PowerConfirmAction.REBOOT_BOOTLOADER -> "Reboot Bootloader"
                                PowerConfirmAction.NONE -> "Power options"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (state) {
                                PowerConfirmAction.POWER_OFF -> "Tap again to turn off your phone"
                                PowerConfirmAction.RESTART -> "Tap again to restart your phone"
                                PowerConfirmAction.RESTART_SYSTEMUI -> "Tap again to reload Samsung SystemUI & status bar"
                                PowerConfirmAction.REBOOT_RECOVERY -> "Tap again to reboot into Android Recovery"
                                PowerConfirmAction.REBOOT_BOOTLOADER -> "Tap again to reboot into Download / Bootloader mode"
                                PowerConfirmAction.NONE -> "Select a power or security action"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state != PowerConfirmAction.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val isPowerOffConfirmed = confirmAction == PowerConfirmAction.POWER_OFF
                    PowerActionButton(
                        icon = Icons.Default.PowerSettingsNew,
                        label = if (isPowerOffConfirmed) "Tap to confirm" else "Power off",
                        iconColor = if (isPowerOffConfirmed) Color.White else Color(0xFFE53935),
                        backgroundColor = if (isPowerOffConfirmed) Color(0xFFE53935) else Color(0xFFE53935).copy(alpha = 0.16f),
                        isConfirmed = isPowerOffConfirmed,
                        haptics = haptics,
                        onClick = {
                            if (confirmAction == PowerConfirmAction.POWER_OFF) {
                                haptics.heavyClick()
                                onPowerOff()
                            } else {
                                haptics.sheetDetent()
                                confirmAction = PowerConfirmAction.POWER_OFF
                            }
                        },
                    )

                    val isRestartConfirmed = confirmAction == PowerConfirmAction.RESTART
                    PowerActionButton(
                        icon = Icons.Default.RestartAlt,
                        label = if (isRestartConfirmed) "Tap to confirm" else "Restart",
                        iconColor = if (isRestartConfirmed) Color.White else Color(0xFF43A047),
                        backgroundColor = if (isRestartConfirmed) Color(0xFF43A047) else Color(0xFF43A047).copy(alpha = 0.16f),
                        isConfirmed = isRestartConfirmed,
                        haptics = haptics,
                        onClick = {
                            if (confirmAction == PowerConfirmAction.RESTART) {
                                haptics.heavyClick()
                                onRestart()
                            } else {
                                haptics.sheetDetent()
                                confirmAction = PowerConfirmAction.RESTART
                            }
                        },
                    )

                    PowerActionButton(
                        icon = Icons.Default.Lock,
                        label = "Lock",
                        iconColor = Color(0xFF1E88E5),
                        backgroundColor = Color(0xFF1E88E5).copy(alpha = 0.16f),
                        isConfirmed = false,
                        haptics = haptics,
                        onClick = {
                            haptics.heavyClick()
                            confirmAction = PowerConfirmAction.NONE
                            onLockScreen()
                        },
                    )

                    PowerActionButton(
                        icon = Icons.Default.MoreHoriz,
                        label = "System",
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        isConfirmed = false,
                        haptics = haptics,
                        onClick = {
                            haptics.sheetDetent()
                            confirmAction = PowerConfirmAction.NONE
                            onSystemPowerDialog()
                        },
                    )
                }

                // Advanced Controls Section (Shizuku)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.lightTap()
                            showAdvanced = !showAdvanced
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isShizukuActive) "Advanced actions (Shizuku active)" else "Advanced actions",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showAdvanced) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = fadeIn(tween(140)) + expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            val isSystemUIConfirmed = confirmAction == PowerConfirmAction.RESTART_SYSTEMUI
                            PowerActionButton(
                                icon = Icons.Default.Refresh,
                                label = if (isSystemUIConfirmed) "Tap to confirm" else "SystemUI",
                                iconColor = if (isSystemUIConfirmed) Color.White else Color(0xFF00ACC1),
                                backgroundColor = if (isSystemUIConfirmed) Color(0xFF00ACC1) else Color(0xFF00ACC1).copy(alpha = 0.16f),
                                isConfirmed = isSystemUIConfirmed,
                                haptics = haptics,
                                onClick = {
                                    if (confirmAction == PowerConfirmAction.RESTART_SYSTEMUI) {
                                        haptics.heavyClick()
                                        onRestartSystemUI()
                                    } else {
                                        haptics.sheetDetent()
                                        confirmAction = PowerConfirmAction.RESTART_SYSTEMUI
                                    }
                                },
                            )

                            val isRecoveryConfirmed = confirmAction == PowerConfirmAction.REBOOT_RECOVERY
                            PowerActionButton(
                                icon = Icons.Default.Build,
                                label = if (isRecoveryConfirmed) "Tap to confirm" else "Recovery",
                                iconColor = if (isRecoveryConfirmed) Color.White else Color(0xFFFFA000),
                                backgroundColor = if (isRecoveryConfirmed) Color(0xFFFFA000) else Color(0xFFFFA000).copy(alpha = 0.16f),
                                isConfirmed = isRecoveryConfirmed,
                                haptics = haptics,
                                onClick = {
                                    if (confirmAction == PowerConfirmAction.REBOOT_RECOVERY) {
                                        haptics.heavyClick()
                                        onRebootRecovery()
                                    } else {
                                        haptics.sheetDetent()
                                        confirmAction = PowerConfirmAction.REBOOT_RECOVERY
                                    }
                                },
                            )

                            val isBootloaderConfirmed = confirmAction == PowerConfirmAction.REBOOT_BOOTLOADER
                            PowerActionButton(
                                icon = Icons.Default.Android,
                                label = if (isBootloaderConfirmed) "Tap to confirm" else "Bootloader",
                                iconColor = if (isBootloaderConfirmed) Color.White else Color(0xFF8E24AA),
                                backgroundColor = if (isBootloaderConfirmed) Color(0xFF8E24AA) else Color(0xFF8E24AA).copy(alpha = 0.16f),
                                isConfirmed = isBootloaderConfirmed,
                                haptics = haptics,
                                onClick = {
                                    if (confirmAction == PowerConfirmAction.REBOOT_BOOTLOADER) {
                                        haptics.heavyClick()
                                        onRebootBootloader()
                                    } else {
                                        haptics.sheetDetent()
                                        confirmAction = PowerConfirmAction.REBOOT_BOOTLOADER
                                    }
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    backgroundColor: Color,
    isConfirmed: Boolean,
    haptics: SuperHaptics,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            isConfirmed -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "powerButtonScale",
    )

    val animatedBg by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "powerButtonBg",
    )

    val animatedIconColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "powerButtonIconTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(animatedBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedIconColor,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isConfirmed) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (isConfirmed) 10.5.sp else 11.5.sp,
            ),
            color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
