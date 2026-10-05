package com.supershade.ui.shade

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.supershade.ui.theme.getCardBorder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.ChamferedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.getCardBorder
import com.supershade.viewmodel.TileDetailState
import com.supershade.viewmodel.TileDetailType

@Composable
fun QuickTileDetailSheet(
    detailState: TileDetailState,
    onDismiss: () -> Unit,
    onSetTorchStrength: (Int) -> Unit = {},
    onToggleTorch: () -> Unit = {},
    onSetRingerMode: (Int) -> Unit = {},
    onSetStreamVolume: (stream: Int, volume: Int) -> Unit = { _, _ -> },
    onToggleDnd: () -> Unit = {},
    onSetDndDuration: (Int) -> Unit = {},
    onToggleHotspot: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptics.sheetDetent()
                    onDismiss()
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val shadeTheme = LocalShadeTheme.current
        val sheetShape = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(12.dp)
            is ShadeTheme.Nothing -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            is ShadeTheme.Pixel -> RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            else -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        }
        val sheetBorder = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.70f))
            is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            else -> getCardBorder(alpha = 0.35f)
        }
        val sheetBg = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> Color(0xFF090D18)
            is ShadeTheme.Nothing -> Color(0xFF0A0C0E)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // consume clicks so sheet doesn't dismiss
                ),
            shape = sheetShape,
            color = sheetBg,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = sheetBorder,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)),
                )
            // Header Row: Title & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    val headerIcon = when (detailState.type) {
                        TileDetailType.FLASHLIGHT -> if (detailState.isActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff
                        TileDetailType.WIFI -> Icons.Default.Wifi
                        TileDetailType.BLUETOOTH -> Icons.Default.Bluetooth
                        TileDetailType.SOUND_MODE -> when (detailState.ringerMode) {
                            android.media.AudioManager.RINGER_MODE_VIBRATE -> Icons.Default.Vibration
                            android.media.AudioManager.RINGER_MODE_SILENT -> Icons.AutoMirrored.Filled.VolumeOff
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        }
                        TileDetailType.DND -> Icons.Default.DoNotDisturbOn
                        TileDetailType.HOTSPOT -> Icons.Default.WifiTethering
                    }
                    val iconTint by animateColorAsState(
                        targetValue = if (detailState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "iconTint",
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (detailState.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detailState.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        detailState.subtitle?.let { sub ->
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onDismiss()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Body content according to type
            when (detailState.type) {
                TileDetailType.FLASHLIGHT -> {
                    FlashlightDetailContent(
                        isActive = detailState.isActive,
                        torchLevel = detailState.torchLevel,
                        maxTorchLevel = detailState.maxTorchLevel,
                        onToggle = {
                            if (!detailState.isActive) haptics.tileToggleOn() else haptics.tileToggleOff()
                            onToggleTorch()
                        },
                        onLevelChange = { level ->
                            haptics.sliderTick()
                            onSetTorchStrength(level)
                        },
                    )
                }
                TileDetailType.WIFI -> {
                    WifiDetailContent(
                        ssid = detailState.wifiSsid ?: "Not Connected",
                        band = detailState.wifiBand ?: "Wi-Fi",
                        ip = detailState.wifiIp ?: "Unknown",
                        speed = detailState.wifiLinkSpeed ?: "Unknown",
                        rssi = detailState.wifiRssi,
                        settingsAction = detailState.settingsAction,
                    )
                }
                TileDetailType.BLUETOOTH -> {
                    BluetoothDetailContent(
                        deviceName = detailState.btDeviceName ?: "Bluetooth",
                        isAudioConnected = detailState.btAudioConnected,
                        settingsAction = detailState.settingsAction,
                    )
                }
                TileDetailType.SOUND_MODE -> {
                    SoundModeDetailContent(
                        ringerMode = detailState.ringerMode,
                        mediaVol = detailState.mediaVol,
                        mediaMaxVol = detailState.mediaMaxVol,
                        ringVol = detailState.ringVol,
                        ringMaxVol = detailState.ringMaxVol,
                        notifVol = detailState.notifVol,
                        notifMaxVol = detailState.notifMaxVol,
                        sysVol = detailState.sysVol,
                        sysMaxVol = detailState.sysMaxVol,
                        settingsAction = detailState.settingsAction,
                        onSetRingerMode = onSetRingerMode,
                        onSetStreamVolume = onSetStreamVolume,
                    )
                }
                TileDetailType.DND -> {
                    DndDetailContent(
                        isActive = detailState.isActive,
                        durationMinutes = detailState.dndDurationMinutes,
                        settingsAction = detailState.settingsAction,
                        onToggle = onToggleDnd,
                        onSetDuration = onSetDndDuration,
                    )
                }
                TileDetailType.HOTSPOT -> {
                    HotspotDetailContent(
                        isActive = detailState.isActive,
                        ssid = detailState.hotspotSsid ?: "AndroidAP",
                        band = detailState.hotspotBand ?: "5 GHz",
                        settingsAction = detailState.settingsAction,
                        onToggle = onToggleHotspot,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
    }
}

@Composable
private fun FlashlightDetailContent(
    isActive: Boolean,
    torchLevel: Int,
    maxTorchLevel: Int,
    onToggle: () -> Unit,
    onLevelChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = getCardBorder(alpha = 0.30f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Torch Status",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = if (isActive) "Flashlight is illuminating" else "Flashlight is powered off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
            )
        }
    }

    if (maxTorchLevel > 1) {
        val levelLabel = when (torchLevel) {
            1 -> "Level 1 • Soft Glow"
            2 -> "Level 2 • Moderate"
            3 -> "Level 3 • Standard"
            4 -> "Level 4 • High Brightness"
            maxTorchLevel -> "Level $maxTorchLevel • Maximum Power"
            else -> "Level $torchLevel"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = getCardBorder(alpha = 0.30f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Hardware Brightness Level",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = levelLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Slider(
                    value = torchLevel.toFloat(),
                    onValueChange = {
                        val newLevel = it.toInt()
                        if (newLevel != torchLevel) {
                            haptics.sliderTick()
                            onLevelChange(newLevel)
                        }
                    },
                    valueRange = 1f..maxTorchLevel.toFloat(),
                    steps = (maxTorchLevel - 2).coerceAtLeast(0),
                    enabled = isActive,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Quick Level Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    (1..maxTorchLevel).forEach { level ->
                        val isSelected = level == torchLevel
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                            label = "torchChipScale",
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = if (isSelected) null else getCardBorder(alpha = 0.35f),
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clickable(enabled = isActive) {
                                    haptics.sliderTick()
                                    onLevelChange(level)
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$level",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiDetailContent(
    ssid: String,
    band: String,
    ip: String,
    speed: String,
    rssi: Int,
    settingsAction: String?,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = getCardBorder(alpha = 0.30f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = "Connected Network",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ssid,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = band,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailMetricTile(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = speed,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                DetailMetricTile(
                    icon = Icons.Default.Router,
                    label = "IP Address",
                    value = ip,
                    modifier = Modifier.weight(1f),
                )
                if (rssi != 0 && rssi != -127) {
                    Spacer(Modifier.width(8.dp))
                    DetailMetricTile(
                        icon = Icons.Default.NetworkCheck,
                        label = "Signal",
                        value = if (rssi >= -60) "Strong" else if (rssi >= -75) "Good" else "Weak",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    FilledTonalButton(
        onClick = {
            haptics.lightTap()
            try {
                val action = settingsAction ?: Settings.ACTION_WIFI_SETTINGS
                context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            } catch (_: Exception) {}
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Wi-Fi Settings")
    }
}

@Composable
private fun BluetoothDetailContent(
    deviceName: String,
    isAudioConnected: Boolean,
    settingsAction: String?,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val bondedDevices = remember {
        try {
            val bm = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
            bm?.adapter?.bondedDevices?.toList()?.sortedBy {
                try { it.name.orEmpty() } catch (_: SecurityException) { "" }
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = getCardBorder(alpha = 0.30f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = "Current Connection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAudioConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = if (isAudioConnected) "Audio Endpoint" else "Paired",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isAudioConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            if (bondedDevices.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paired Devices",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    bondedDevices.take(4).forEach { device ->
                        val dName = try { device.name ?: "Bluetooth Device" } catch (_: SecurityException) { "Bluetooth Device" }
                        val isCurrent = dName.equals(deviceName, ignoreCase = true)
                        Surface(
                            onClick = {
                                haptics.lightTap()
                                try {
                                    val action = settingsAction ?: Settings.ACTION_BLUETOOTH_SETTINGS
                                    context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isCurrent) getCardBorder(alpha = 0.40f) else null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = if (isAudioConnected && isCurrent) Icons.Default.Headphones else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = dName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "Connected",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = {
                haptics.lightTap()
                try {
                    val action = settingsAction ?: Settings.ACTION_BLUETOOTH_SETTINGS
                    context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                } catch (_: Exception) {}
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Bluetooth Settings")
        }

        if (isAudioConnected) {
            OutlinedButton(
                onClick = {
                    haptics.sheetDetent()
                    try {
                        val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Media Output")
            }
        }
    }
}

@Composable
private fun DetailMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.60f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SoundModeDetailContent(
    ringerMode: Int,
    mediaVol: Int,
    mediaMaxVol: Int,
    ringVol: Int,
    ringMaxVol: Int,
    notifVol: Int,
    notifMaxVol: Int,
    sysVol: Int,
    sysMaxVol: Int,
    settingsAction: String?,
    onSetRingerMode: (Int) -> Unit,
    onSetStreamVolume: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val shadeTheme = LocalShadeTheme.current

    val selectorContainerShape = when (shadeTheme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(10.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(16.dp)
        is ShadeTheme.Pixel -> RoundedCornerShape(24.dp)
        else -> RoundedCornerShape(20.dp)
    }
    val selectorContainerBorder = when (shadeTheme) {
        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.50f))
        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        else -> getCardBorder(alpha = 0.30f)
    }
    val selectorContainerBg = when (shadeTheme) {
        is ShadeTheme.Cyberpunk -> Color(0xFF0A0F1D)
        is ShadeTheme.Nothing -> Color(0xFF101216)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Multi-Theme Tri-State Mode Selector
        Surface(
            shape = selectorContainerShape,
            color = selectorContainerBg,
            border = selectorContainerBorder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Triple(android.media.AudioManager.RINGER_MODE_NORMAL, "Sound", Icons.AutoMirrored.Filled.VolumeUp),
                    Triple(android.media.AudioManager.RINGER_MODE_VIBRATE, "Vibrate", Icons.Default.Vibration),
                    Triple(android.media.AudioManager.RINGER_MODE_SILENT, "Mute", Icons.AutoMirrored.Filled.VolumeOff),
                ).forEach { (mode, label, icon) ->
                    val isSelected = ringerMode == mode
                    val modeScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "modeScale",
                    )

                    val itemShape = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                        is ShadeTheme.Nothing -> RoundedCornerShape(12.dp)
                        is ShadeTheme.Pixel -> RoundedCornerShape(20.dp)
                        else -> RoundedCornerShape(14.dp)
                    }
                    val itemColor = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.20f) else Color(0xFF060A14)
                        is ShadeTheme.Nothing -> if (isSelected) Color.White else Color(0xFF16191E)
                        is ShadeTheme.Pixel -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f)
                        else -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f)
                    }
                    val itemBorder = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> if (isSelected) BorderStroke(1.2.dp, Color(0xFF00F0FF)) else BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.15f))
                        is ShadeTheme.Nothing -> if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                        else -> if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    }
                    val itemContentColor = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> if (isSelected) Color(0xFF00F0FF) else Color(0xFF80A8C0)
                        is ShadeTheme.Nothing -> if (isSelected) Color.Black else Color(0xFFB0B0B0)
                        else -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val displayLabel = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> when (mode) {
                            android.media.AudioManager.RINGER_MODE_NORMAL -> "[SOUND]"
                            android.media.AudioManager.RINGER_MODE_VIBRATE -> "[VIB]"
                            else -> "[MUTE]"
                        }
                        is ShadeTheme.Nothing -> label.uppercase()
                        else -> label
                    }

                    Surface(
                        onClick = {
                            haptics.sliderTick()
                            onSetRingerMode(mode)
                        },
                        shape = itemShape,
                        color = itemColor,
                        border = itemBorder,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = modeScale
                                scaleY = modeScale
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = itemContentColor,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 11.5.sp,
                                ),
                                color = itemContentColor,
                            )
                        }
                    }
                }
            }
        }

        // Live Audio Stream Sliders
        Surface(
            shape = selectorContainerShape,
            color = selectorContainerBg,
            border = selectorContainerBorder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (shadeTheme is ShadeTheme.Cyberpunk) "[VOLUME // CHANNELS]" else "Volume Levels",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                    ),
                    color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                )

                // Media volume
                VolumeStreamRow(
                    label = "Media",
                    icon = Icons.Default.MusicNote,
                    current = mediaVol,
                    max = mediaMaxVol,
                    onVolumeChange = { onSetStreamVolume(android.media.AudioManager.STREAM_MUSIC, it) },
                    haptics = haptics,
                )

                // Ringtone volume (muted if silent/vibrate)
                VolumeStreamRow(
                    label = "Ringtone",
                    icon = Icons.Default.Phone,
                    current = if (ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) ringVol else 0,
                    max = ringMaxVol,
                    enabled = ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL,
                    onVolumeChange = { onSetStreamVolume(android.media.AudioManager.STREAM_RING, it) },
                    haptics = haptics,
                )

                // Notification volume
                VolumeStreamRow(
                    label = "Notifications",
                    icon = Icons.Default.Notifications,
                    current = if (ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) notifVol else 0,
                    max = notifMaxVol,
                    enabled = ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL,
                    onVolumeChange = { onSetStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, it) },
                    haptics = haptics,
                )

                // System volume
                VolumeStreamRow(
                    label = "System",
                    icon = Icons.Default.Tune,
                    current = sysVol,
                    max = sysMaxVol,
                    onVolumeChange = { onSetStreamVolume(android.media.AudioManager.STREAM_SYSTEM, it) },
                    haptics = haptics,
                )
            }
        }

        // Bottom Settings Button
        settingsAction?.let { action ->
            OutlinedButton(
                onClick = {
                    haptics.lightTap()
                    try {
                        context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    } catch (_: Exception) {}
                },
                shape = when (shadeTheme) {
                    is ShadeTheme.Cyberpunk -> ChamferedCornerShape(8.dp)
                    is ShadeTheme.Nothing -> RoundedCornerShape(10.dp)
                    is ShadeTheme.Pixel -> RoundedCornerShape(20.dp)
                    else -> RoundedCornerShape(14.dp)
                },
                border = when (shadeTheme) {
                    is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f))
                    is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (shadeTheme is ShadeTheme.Cyberpunk) "[AUDIO_SETTINGS // CFG]" else "Sound & Vibration Settings",
                    fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                )
            }
        }
    }
}

@Composable
private fun VolumeStreamRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    current: Int,
    max: Int,
    enabled: Boolean = true,
    onVolumeChange: (Int) -> Unit,
    haptics: SuperHaptics,
) {
    val shadeTheme = LocalShadeTheme.current
    val pct = if (max > 0) (current * 100 / max).coerceIn(0, 100) else 0
    val progress = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "volumeStreamProgress",
    )

    val trackHeight = when (shadeTheme) {
        is ShadeTheme.Cyberpunk -> 36.dp
        is ShadeTheme.Nothing -> 38.dp
        else -> 42.dp
    }
    val trackShape = when (shadeTheme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(12.dp)
        is ShadeTheme.Pixel -> RoundedCornerShape(21.dp)
        else -> RoundedCornerShape(21.dp)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(trackShape)
                .background(
                    when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> Color(0xFF050B14)
                        is ShadeTheme.Nothing -> Color(0xFF14171C)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                    }
                )
                .then(
                    when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> Modifier.border(BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f)), trackShape)
                        is ShadeTheme.Nothing -> Modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), trackShape)
                        else -> Modifier
                    }
                )
                .pointerInput(max, enabled) {
                    if (!enabled || max <= 0) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var currentVol = current
                        val update = { x: Float ->
                            val ratio = (x / size.width).coerceIn(0f, 1f)
                            val targetVol = (ratio * max).roundToInt()
                            if (targetVol != currentVol) {
                                haptics.segmentTick()
                                currentVol = targetVol
                                onVolumeChange(targetVol)
                            }
                        }
                        update(down.position.x)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            update(change.position.x)
                            change.consume()
                        }
                    }
                },
        ) {
            // Active Progress Fill
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> Brush.horizontalGradient(
                                    listOf(Color(0xFF00F0FF).copy(alpha = 0.75f), Color(0xFFFF007F).copy(alpha = 0.85f))
                                )
                                is ShadeTheme.Nothing -> Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.88f), Color.White)
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                                )
                            }
                        ),
                )
            }

            // Theme-specific decorative elements (Nothing 8 dot notches, Cyberpunk hash marks)
            when (shadeTheme) {
                is ShadeTheme.Nothing -> {
                    // 7 discrete dot notches
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(7) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                            )
                        }
                    }
                    // Red glyph dot at thumb edge if volume > 0
                    if (animatedProgress in 0.05f..0.98f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .wrapContentSize(Alignment.CenterEnd)
                                .offset(x = 3.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD71920)),
                        )
                    }
                }
                is ShadeTheme.Cyberpunk -> {
                    // 9 subtle vertical grid tick marks
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(9) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(10.dp)
                                    .background(Color(0xFF00F0FF).copy(alpha = 0.20f)),
                            )
                        }
                    }
                }
                else -> Unit
            }

            // Internal Label & Icon row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val iconTint = when (shadeTheme) {
                        is ShadeTheme.Nothing -> if (animatedProgress > 0.35f) Color.Black else Color.White
                        is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                        else -> if (animatedProgress > 0.35f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    }
                    val textTint = when (shadeTheme) {
                        is ShadeTheme.Nothing -> if (animatedProgress > 0.35f) Color.Black else Color.White
                        is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                        else -> if (animatedProgress > 0.35f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) iconTint else iconTint.copy(alpha = 0.40f),
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = when (shadeTheme) {
                            is ShadeTheme.Cyberpunk -> "[${label.uppercase()}]"
                            is ShadeTheme.Nothing -> label.uppercase()
                            else -> label
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 12.sp,
                        ),
                        color = if (enabled) textTint else textTint.copy(alpha = 0.40f),
                    )
                }

                val pctColor = when (shadeTheme) {
                    is ShadeTheme.Nothing -> if (animatedProgress > 0.85f) Color.Black else Color.White
                    is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                    else -> if (animatedProgress > 0.85f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = if (enabled) "$pct%" else "MUTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 11.5.sp,
                    ),
                    color = pctColor,
                )
            }
        }
    }
}

@Composable
private fun DndDetailContent(
    isActive: Boolean,
    durationMinutes: Int,
    settingsAction: String?,
    onToggle: () -> Unit,
    onSetDuration: (Int) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = getCardBorder(alpha = 0.30f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Do Not Disturb",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = if (isActive) "Mute calls, alerts, and media sounds" else "Allow all calls and notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = {
                        if (!isActive) haptics.tileToggleOn() else haptics.tileToggleOff()
                        onToggle()
                    },
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = getCardBorder(alpha = 0.30f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )

                listOf(
                    0 to "Until I turn it off",
                    60 to "For 1 hour",
                    120 to "For 2 hours",
                    -1 to "Until next alarm",
                ).forEach { (mins, label) ->
                    val isSelected = durationMinutes == mins
                    Surface(
                        onClick = {
                            haptics.sliderTick()
                            onSetDuration(mins)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        settingsAction?.let { action ->
            OutlinedButton(
                onClick = {
                    haptics.lightTap()
                    try {
                        context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("DND Exceptions & Schedules")
            }
        }
    }
}

@Composable
private fun HotspotDetailContent(
    isActive: Boolean,
    ssid: String,
    band: String,
    settingsAction: String?,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = getCardBorder(alpha = 0.30f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Mobile Hotspot",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = if (isActive) "Sharing portable Wi-Fi connection" else "Hotspot is turned off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = {
                        if (!isActive) haptics.tileToggleOn() else haptics.tileToggleOff()
                        onToggle()
                    },
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = getCardBorder(alpha = 0.30f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DetailMetricTile(
                        icon = Icons.Default.Wifi,
                        label = "Network Name",
                        value = ssid,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    DetailMetricTile(
                        icon = Icons.Default.Router,
                        label = "Band",
                        value = band,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        settingsAction?.let { action ->
            OutlinedButton(
                onClick = {
                    haptics.lightTap()
                    try {
                        context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Configure Mobile Hotspot")
            }
        }
    }
}
