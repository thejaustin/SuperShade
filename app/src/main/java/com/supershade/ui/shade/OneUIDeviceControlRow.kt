package com.supershade.ui.shade

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.supershade.settings.DeviceControlMode
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder

/**
 * Official Samsung One UI 8.5/9 Dual Pill Row:
 * [ Device control ]  [ Media output ]
 *
 * Fully removable and customizable style:
 * - Mode: SHOW_WHEN_EXPANDED, SHOW_ALWAYS, or DONT_SHOW.
 * - In Edit Mode: Displays a dedicated customizable One UI 8.5/9 header allowing
 *   quick mode switching ("When expanded" vs "Always") and a 1-tap Remove button (X).
 * - Launches official system device controls (SmartThings/Google Home) and system media output selector.
 */
@Composable
fun OneUIDeviceControlRow(
    mode: DeviceControlMode,
    isExpanded: Boolean,
    isEditing: Boolean = false,
    onRemove: () -> Unit = {},
    onChangeMode: ((DeviceControlMode) -> Unit)? = null,
    onDismissShade: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // When not editing, adhere strictly to mode and expanded visibility
    if (!isEditing) {
        if (mode == DeviceControlMode.DONT_SHOW) return
        if (mode == DeviceControlMode.SHOW_WHEN_EXPANDED && !isExpanded) return
    } else {
        // In editing mode, if removed/don't show, it is placed in the "Available buttons" section
        if (mode == DeviceControlMode.DONT_SHOW) return
    }

    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val shapes = LocalShadeShapeScheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Edit Mode Header: Mode switcher chip and 1-tap Remove button (X)
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Device & Media buttons",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Mode cycle toggle pill
                    if (onChangeMode != null) {
                        Surface(
                            onClick = {
                                haptics.lightTap()
                                val nextMode = when (mode) {
                                    DeviceControlMode.SHOW_WHEN_EXPANDED -> DeviceControlMode.SHOW_ALWAYS
                                    else -> DeviceControlMode.SHOW_WHEN_EXPANDED
                                }
                                onChangeMode(nextMode)
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = if (mode == DeviceControlMode.SHOW_ALWAYS) "Always" else "When expanded",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    // 1-tap Remove button (X)
                    Surface(
                        onClick = {
                            haptics.sheetDetent()
                            onRemove()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f)),
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Device control & Media output",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        // Dual Pill Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Pill: Device control
            OneUIPillButton(
                icon = Icons.Outlined.Devices,
                label = "Device control",
                shape = shapes.chip,
                isEditing = isEditing,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isEditing) {
                        haptics.lightTap()
                        launchDeviceControls(context)
                        onDismissShade()
                    }
                },
            )

            // Right Pill: Media output
            OneUIPillButton(
                icon = Icons.Outlined.Speaker,
                label = "Media output",
                shape = shapes.chip,
                isEditing = isEditing,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isEditing) {
                        haptics.lightTap()
                        launchMediaOutput(context)
                        onDismissShade()
                    }
                },
            )
        }
    }
}

@Composable
private fun OneUIPillButton(
    icon: ImageVector,
    label: String,
    shape: androidx.compose.ui.graphics.Shape,
    isEditing: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isEditing) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "oneUiPillScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isEditing) 0.40f else 0.55f),
        border = getCardBorder(alpha = if (isEditing) 0.50f else 0.35f),
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

private fun launchDeviceControls(context: Context) {
    val intents = listOf(
        Intent("android.intent.action.VIEW_CONTROLS"),
        Intent("android.service.controls.ControlsProviderService.SERVICE_CONTROLS"),
        Intent("com.samsung.android.oneconnect.action.DEVICE_CONTROL"),
        context.packageManager.getLaunchIntentForPackage("com.samsung.android.oneconnect"),
        context.packageManager.getLaunchIntentForPackage("com.google.android.apps.chromecast.app"),
        Intent(Settings.ACTION_SETTINGS),
    )

    for (intent in intents) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}

private fun launchMediaOutput(context: Context) {
    val intents = listOf(
        Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
            putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
        },
        Intent("com.samsung.android.app.soundalive.action.MEDIA_OUTPUT"),
        Intent(Settings.ACTION_SOUND_SETTINGS),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
    )

    for (intent in intents) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
}
