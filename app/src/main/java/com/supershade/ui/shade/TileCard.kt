package com.supershade.ui.shade

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.supershade.ui.theme.ChamferedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChargingStation
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import com.supershade.domain.tile.humanizeTileLabel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.tile.TileDefinition
import com.supershade.settings.TileShape
import com.supershade.settings.TileSize
import com.supershade.ui.theme.ShadeTheme

@Composable
fun TileCard(
    tile: TileDefinition,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    tileShape: TileShape = TileShape.SQUIRCLE,
    tileSize: TileSize = TileSize.STANDARD,
    columns: Int = 4,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    /** Set to true when this card is being dragged (renders with lifted shadow/scale) */
    isDragging: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val shapeScheme = com.supershade.ui.theme.LocalShadeShapeScheme.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseCorner = when (tileShape) {
        TileShape.SQUIRCLE -> 22.dp
        TileShape.ROUNDED -> 16.dp
        TileShape.PILL -> 28.dp
        TileShape.SOFT -> 14.dp
        TileShape.SHARP -> 6.dp
        else -> null
    }

    val dynamicCornerRadius by animateDpAsState(
        targetValue = if (baseCorner != null) {
            when {
                isPressed -> (baseCorner - 5.dp).coerceAtLeast(4.dp)
                tile.isActive -> baseCorner + 2.dp
                else -> baseCorner
            }
        } else 0.dp,
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.spatialDefault(),
        label = "tileCornerRadius",
    )

    val cardShape = when {
        theme is ShadeTheme.Cyberpunk -> ChamferedCornerShape(if (isPressed) 5.dp else 8.dp)
        theme is ShadeTheme.Nothing -> RoundedCornerShape(if (isPressed) 12.dp else 16.dp)
        baseCorner != null -> RoundedCornerShape(dynamicCornerRadius)
        else -> shapeScheme.tile
    }

    // Scale: pressed shrink, dragging lift, otherwise 1f
    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isPressed -> 0.92f
            else -> 1f
        },
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.spatialFast(),
        label = "tileScale",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (tile.isActive) 1.06f else 1.0f,
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.spatialFast(),
        label = "tileIconScale",
    )

    val iconRotation by animateFloatAsState(
        targetValue = when {
            tile.id.contains("rotate") -> if (tile.isActive) 90f else 0f
            tile.id.contains("sync") -> if (tile.isActive) 180f else 0f
            tile.id.contains("airplane") -> if (tile.isActive) 45f else 0f
            tile.id.contains("flashlight") || tile.id.contains("torch") -> if (tile.isActive) -12f else 0f
            isPressed -> -4f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 800f),
        label = "tileIconRotation",
    )

    val activeDotScale by animateFloatAsState(
        targetValue = if (tile.isActive && !isEditing) 1.0f else 0.0f,
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.spatialBouncy(),
        label = "tileActiveDotScale",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            tile.isActive -> when (theme) {
                is ShadeTheme.Nothing -> Color.White
                is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF).copy(alpha = 0.22f)
                else -> MaterialTheme.colorScheme.primary
            }
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.effectsDefault(),
        label = "tileContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            tile.isActive -> when (theme) {
                is ShadeTheme.Nothing -> Color.Black
                is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                else -> MaterialTheme.colorScheme.onPrimary
            }
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = com.supershade.ui.theme.M3ExpressiveMotion.effectsDefault(),
        label = "tileContent",
    )

    val indication = LocalIndication.current
    val borderStroke = when {
        theme is ShadeTheme.Cyberpunk -> BorderStroke(
            1.2.dp,
            if (tile.isActive) Color(0xFF00F0FF) else Color(0xFF00F0FF).copy(alpha = 0.35f),
        )
        theme is ShadeTheme.Nothing && !tile.isActive -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        )
        theme is ShadeTheme.Pixel -> null
        tile.isActive -> null
        else -> getCardBorder(alpha = 0.40f)
    }

    val stateDesc = if (tile.isActive) {
        tile.subtitle ?: "Active"
    } else {
        "Off"
    }

    val cardHeight = tileSize.heightDp.dp
    val rawIconSize = if (columns >= 5) (tileSize.iconSizeDp - 2).coerceAtLeast(18) else tileSize.iconSizeDp
    val iconSize = rawIconSize.dp
    val horizPadding = if (columns >= 5) (shapeScheme.tilePaddingHorizontal - 2.dp).coerceAtLeast(4.dp) else shapeScheme.tilePaddingHorizontal
    val vertPadding = when (tileSize) {
        TileSize.COMPACT    -> (shapeScheme.tilePaddingVertical - 3.dp).coerceAtLeast(4.dp)
        TileSize.COMFORTABLE -> shapeScheme.tilePaddingVertical + 2.dp
        TileSize.STANDARD   -> shapeScheme.tilePaddingVertical
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Subtle shadow lift when dragging
                shadowElevation = if (isDragging) 24f else 0f
            }
            .semantics {
                role = Role.Switch
                stateDescription = stateDesc
                tile.settingsAction?.let { action ->
                    customActions = listOf(
                        CustomAccessibilityAction("Open settings") {
                            try {
                                context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                true
                            } catch (_: Exception) { false }
                        }
                    )
                }
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (tile.isActive && LocalBackdropTheme.current == BackdropTheme.LIQUID_GLASS) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                                start = Offset.Zero,
                                end = Offset(200f, 200f),
                            )
                        )
                )
            }

            // Click / long-press — disabled while editing (parent handles drag)
            val clickModifier = if (!isEditing) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = {
                        if (!tile.isActive) haptics.tileToggleOn() else haptics.tileToggleOff()
                        onClick()
                    },
                    onLongClick = if (onLongClick != null || tile.settingsAction != null) {
                        {
                            haptics.sheetDetent()
                            if (onLongClick != null) {
                                onLongClick()
                            } else {
                                tile.settingsAction?.let { action ->
                                    try {
                                        context.startActivity(
                                            Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        )
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    } else null,
                    role = Role.Switch,
                )
            } else Modifier

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(clickModifier)
                    .padding(horizontal = horizPadding, vertical = vertPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val resolvedIcon = tileIcon(tile.id, tile.isActive, tile.subtitle)
                    if (resolvedIcon != Icons.Filled.Widgets) {
                    Icon(
                        imageVector = resolvedIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                rotationZ = iconRotation
                            },
                    )
                } else if (tile.customIcon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = tile.customIcon,
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor),
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                rotationZ = iconRotation
                            },
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                rotationZ = iconRotation
                            },
                    )
                }
                    if (activeDotScale > 0.05f) {
                        val dotColor = when (theme) {
                            is ShadeTheme.Nothing -> Color(0xFFD71920) // Nothing signature glyph red
                            is ShadeTheme.Cyberpunk -> MaterialTheme.colorScheme.secondary // Hot neon pink
                            else -> contentColor.copy(alpha = 0.85f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (columns >= 5) 5.dp else 6.dp)
                                .graphicsLayer {
                                    scaleX = activeDotScale
                                    scaleY = activeDotScale
                                }
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    }
                }
                val baseSize = when (tileSize) {
                    TileSize.COMPACT    -> if (columns >= 5) 8.5.sp else 9.5.sp
                    TileSize.COMFORTABLE -> if (columns >= 5) 9.5.sp else 12.sp
                    TileSize.STANDARD   -> if (columns >= 5) 8.5.sp else 10.5.sp
                }
                val rawLabel = tile.label.ifBlank { humanizeTileLabel(tile.id) }
                val displayLabel = when {
                    tile.id.lowercase().contains("rotation") -> if (tile.isActive) "Auto rotate" else "Portrait"
                    tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> tile.subtitle ?: rawLabel
                    else -> rawLabel
                }
                val displaySubtitle = when {
                    tile.id.lowercase().contains("rotation") -> null
                    tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> null
                    else -> tile.subtitle
                }
                val formattedLabel = when (theme) {
                    is ShadeTheme.Nothing -> displayLabel.uppercase()
                    else -> displayLabel
                }
                val formattedSubtitle = when {
                    displaySubtitle != null -> when (theme) {
                        is ShadeTheme.Cyberpunk -> "// $displaySubtitle"
                        is ShadeTheme.Nothing -> displaySubtitle.uppercase()
                        else -> displaySubtitle
                    }
                    theme is ShadeTheme.Cyberpunk && tileSize != TileSize.COMPACT && !isEditing -> {
                        if (tile.isActive) "// ON" else "// OFF"
                    }
                    else -> null
                }
                val titleFontSize = when {
                    columns >= 5 && formattedLabel.length > 8 -> (baseSize.value - 1f).sp
                    formattedLabel.length > 13 -> (baseSize.value - 0.75f).sp
                    else -> baseSize
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formattedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (theme is ShadeTheme.Nothing) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = titleFontSize,
                            lineHeight = (titleFontSize.value + 2).sp,
                            fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                            letterSpacing = if (theme is ShadeTheme.Nothing) 0.8.sp else 0.sp,
                        ),
                        color = contentColor,
                        maxLines = if (tileSize == TileSize.COMPACT || formattedSubtitle != null) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                    )
                    if (formattedSubtitle != null && tileSize != TileSize.COMPACT && !isEditing) {
                        Text(
                            text = formattedSubtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (columns >= 5) 8.sp else 9.sp,
                                lineHeight = if (columns >= 5) 9.sp else 10.sp,
                                fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                                letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                            ),
                            color = contentColor.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Remove (×) badge shown when editing — top-right corner
            if (isEditing && onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable {
                            haptics.lightTap()
                            onRemove()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${tile.label}",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

fun tileIcon(id: String, isActive: Boolean, subtitle: String?): ImageVector {
    val key = id.lowercase()
    return when {
        key.contains("wifi") || key.contains("internet") -> Icons.Default.Wifi
        key.contains("bt") || key.contains("bluetooth")  -> Icons.Default.Bluetooth
        key.contains("airplane")                        -> Icons.Default.AirplanemodeActive
        key.contains("hotspot") || key.contains("tether") -> Icons.Default.WifiTethering
        key.contains("dnd") || key.contains("disturb")   -> Icons.Default.DoNotDisturb
        key.contains("rotation") || key.contains("rotate") -> Icons.Default.ScreenRotation
        key.contains("dark") || key.contains("night") || key.contains("uimode") -> Icons.Default.DarkMode
        key.contains("flash") || key.contains("torch")   -> Icons.Default.FlashOn
        key.contains("location") || key.contains("gps")  -> Icons.Default.LocationOn
        key.contains("nfc")                             -> Icons.Default.Nfc
        key.contains("sync")                            -> Icons.Default.Sync
        key.contains("record")                          -> Icons.Default.Videocam
        key.contains("cast") || key.contains("smart") || key.contains("mirror") -> Icons.Default.Cast
        key.contains("vpn")                             -> Icons.Default.VpnKey
        key.contains("cell") || key.contains("data") || key.contains("mobile") -> Icons.Default.SignalCellularAlt
        key.contains("powershare")                      -> Icons.Default.ChargingStation
        key.contains("battery") || key.contains("saver") -> Icons.Default.Battery5Bar
        key.contains("chargi")                          -> Icons.Default.BatteryChargingFull
        key.contains("camera")                          -> Icons.Default.CameraAlt
        key.contains("mic")                             -> Icons.Default.Mic
        key.contains("qr")                              -> Icons.Default.QrCodeScanner
        key.contains("wallet") || key.contains("cashiro") || key.contains("transaction") || key.contains("subscription") -> Icons.Default.AccountBalanceWallet
        key.contains("share") || key.contains("nearby") || key.contains("quickshare") -> Icons.Default.Share
        key.contains("dolby") || key.contains("atmos") || key.contains("jamesdsp") || key.contains("soundalive") || key.contains("auracast") || key.contains("audiobroadcast") || key.contains("equalizer") -> Icons.Default.GraphicEq
        key.contains("dex") || key.contains("desktop")  -> Icons.Default.DesktopWindows
        key.contains("note") || key.contains("todo") || key.contains("ramble") || key.contains("task") -> Icons.AutoMirrored.Filled.StickyNote2
        key.contains("hearing")                         -> Icons.Default.Hearing
        key.contains("dim") || key.contains("reduce") || key.contains("extradim") -> Icons.Default.Brightness4
        key.contains("work") || key.contains("focus")    -> Icons.Default.Work
        key.contains("alarm")                           -> Icons.Default.Alarm
        key.contains("pocket")                          -> Icons.Default.ScreenLockPortrait
        key.contains("lock") || key.contains("secure") || key.contains("knox") || key.contains("folder") -> Icons.Default.ScreenLockPortrait
        key.contains("radio") || key.contains("nrs")    -> Icons.Default.RadioButtonChecked
        key.contains("sensor") || key.contains("sidegesturepad") || key.contains("sgp") || key.contains("onehand") || key.contains("gesture") -> Icons.Default.PanTool
        key.contains("vibrate") || key.contains("vibration") -> Icons.Default.Vibration
        key.contains("mute") || key.contains("sound") || key.contains("volume") -> {
            if (isActive || subtitle?.contains("Vibrate", ignoreCase = true) == true)
                Icons.AutoMirrored.Filled.VolumeOff
            else
                Icons.AutoMirrored.Filled.VolumeUp
        }
        key.contains("bedtime") || key.contains("sleep") -> Icons.Default.NightsStay
        key.contains("usage") || key.contains("stats")  -> Icons.Default.DataUsage
        key.contains("privacy") || key.contains("screenprivacy") || key.contains("curtain") -> Icons.Filled.VisibilityOff
        key.contains("power") -> Icons.Filled.PowerSettingsNew
        key.contains("aod") || key.contains("alwayson") || key.contains("always_on") || key.contains("keepscreen") || key.contains("caffeine") || key.contains("caffeinate") || key.contains("awake") -> Icons.Filled.WatchLater
        key.contains("mode") || key.contains("routine") -> Icons.Filled.Tune
        key.contains("kid") -> Icons.Filled.ChildCare
        key.contains("color") || key.contains("invert") -> Icons.Filled.InvertColors
        key.contains("refresh") || key.contains("speed") || key.contains("perf") -> Icons.Filled.Speed
        key.contains("terminal") || key.contains("shell") || key.contains("wirelessdebugging") || key.contains("debug") -> Icons.Filled.Terminal
        key.contains("windows") || key.contains("link") -> Icons.Filled.Phonelink
        key.contains("smartthings") || key.contains("home") -> Icons.Filled.Home
        key.contains("bluelight") || key.contains("blue_light") || key.contains("eye") || key.contains("comfort") || key.contains("shield") -> Icons.Default.Visibility
        key.contains("screen") || key.contains("capture") || key.contains("shot") -> Icons.Default.CameraAlt
        key.contains("font") || key.contains("text") -> Icons.Default.TextFields
        key.contains("keyboard") || key.contains("ime") -> Icons.Default.Keyboard
        key.contains("music") || key.contains("audio") -> Icons.Default.MusicNote
        key.contains("protect") -> Icons.Filled.Security
        else -> Icons.Filled.Widgets
    }
}
