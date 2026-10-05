package com.supershade.ui.shade

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.font.FontFamily
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.ChamferedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import com.supershade.ui.theme.getCardBorder
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.domain.notification.model.NotificationAction
import com.supershade.domain.notification.model.ShadeNotification

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import com.supershade.ui.theme.LocalShadeShapeScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: ShadeNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onSnooze: ((Long) -> Unit)? = null,
    onHideChannel: (pkg: String, channelId: String) -> Unit = { _, _ -> },
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val shapes = LocalShadeShapeScheme.current
    var expanded by remember { mutableStateOf(false) }
    var replyingAction by remember { mutableStateOf<NotificationAction?>(null) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Resolve readable app name from the package
    val appName = remember(notification.packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(notification.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            notification.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    val appIconBitmap by produceState<ImageBitmap?>(null, notification.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(notification.packageName)
                    .toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    val largeIconBitmap by produceState<ImageBitmap?>(null, notification.largeIcon) {
        value = withContext(Dispatchers.IO) {
            try {
                notification.largeIcon?.loadDrawable(context)
                    ?.toBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                    ?.asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    val postTimeLabel by produceState(
        initialValue = relativeTime(notification.postTime),
        key1 = notification.postTime,
    ) {
        while (true) {
            delay(60_000L)
            value = relativeTime(notification.postTime)
        }
    }

    val displayTitle = when {
        notification.title.isNotBlank() -> notification.title
        notification.text.isNotBlank() -> appName
        else -> appName
    }
    val displayText = when {
        notification.title.isNotBlank() -> notification.text
        else -> ""
    }

    val swipeState = rememberFluidSwipeState(key = notification.key)

    FluidSwipeToDismiss(
        state = swipeState,
        onDismissed = { onDismiss() },
        enableStartToEnd = notification.isClearable,
        enableEndToStart = notification.isClearable,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val isSwiping = !swipeState.isAtRest
            if (!isSwiping) return@FluidSwipeToDismiss
            val currentOffset = swipeState.offsetPx
            val alignment = if (currentOffset > 0f) Alignment.CenterStart else Alignment.CenterEnd
            val progress = swipeState.progress
            val isPastDismissThreshold = kotlin.math.abs(currentOffset) > swipeState.thresholdPx

            val badgeScale by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 1.08f else (0.80f + progress * 0.40f).coerceIn(0.80f, 1.0f),
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "swipeBadgeScale",
            )
            val iconRotation by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 0f else if (currentOffset > 0f) -12f else 12f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "swipeIconRotation",
            )
            val trackBgColor = Color(0xFFE53935).copy(alpha = (progress * 0.45f).coerceIn(0f, 0.40f))
            val badgeColor = if (isPastDismissThreshold) Color(0xFFE53935) else Color(0xFFE53935).copy(alpha = 0.85f)
            val icon = Icons.Default.Delete
            val label = "Dismiss"

            val iconSlideOffset by animateDpAsState(
                targetValue = if (currentOffset > 0f) {
                    ((progress.coerceIn(0f, 0.35f) / 0.35f - 1f) * 16f).dp
                } else {
                    ((1f - progress.coerceIn(0f, 0.35f) / 0.35f) * 16f).dp
                },
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "swipeIconSlide",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.card)
                    .background(trackBgColor),
                contentAlignment = alignment,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor,
                    shadowElevation = if (isPastDismissThreshold) 6.dp else 1.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(x = iconSlideOffset)
                        .graphicsLayer {
                            scaleX = badgeScale
                            scaleY = badgeScale
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (currentOffset > 0f) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = iconRotation },
                            )
                            if (progress > 0.25f) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }
                        } else {
                            if (progress > 0.25f) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = iconRotation },
                            )
                        }
                    }
                }
            }
        },
    ) {
        val dragProgress = swipeState.progress
        val isSwiping = !swipeState.isAtRest
        val cardScale = (1.0f - dragProgress * 0.04f).coerceIn(0.95f, 1.0f)
        val cardAlpha = if (dragProgress > 0.75f) (1f - (dragProgress - 0.75f) * 2.5f).coerceIn(0.4f, 1.0f) else 1.0f
        val cardElevation = (dragProgress * 8f).dp
        val shadeTheme = LocalShadeTheme.current
        val baseThemeShape = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(10.dp)
            is ShadeTheme.Nothing -> RoundedCornerShape(16.dp)
            is ShadeTheme.Pixel -> RoundedCornerShape(26.dp)
            else -> shapes.card
        }

        val dynamicCardShape = if (isSwiping && dragProgress > 0.05f && baseThemeShape is RoundedCornerShape) {
            RoundedCornerShape(24.dp + (dragProgress * 6f).dp)
        } else {
            baseThemeShape
        }

        val cardBorder = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.45f))
            is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            else -> getCardBorder(alpha = 0.25f)
        }

        val cardBg = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> Color(0xFF0A0E1A)
            is ShadeTheme.Nothing -> Color(0xFF0D0F12)
            else -> MaterialTheme.colorScheme.surfaceContainer
        }

        Card(
            shape = dynamicCardShape,
            colors = CardDefaults.cardColors(
                containerColor = cardBg,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            border = cardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        haptics.lightTap()
                        onClick()
                    },
                    onLongClick = {
                        haptics.sheetDetent()
                        showSettingsMenu = true
                    },
                ),
        ) {
            val hPadding = if (compact) 10.dp else 14.dp
            val vPadding = if (compact) 6.dp else 12.dp
            val iconBoxSize = if (compact) 30.dp else 38.dp
            val iconRadius = if (compact) 8.dp else 12.dp
            val iconImgSize = if (compact) 26.dp else 34.dp
            val columnSpacing = if (compact) 9.dp else 12.dp

            val iconShape = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(if (compact) 4.dp else 6.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(if (compact) 6.dp else 8.dp)
                is ShadeTheme.Pixel -> CircleShape
                else -> RoundedCornerShape(iconRadius)
            }

            Column(modifier = Modifier.padding(horizontal = hPadding, vertical = vPadding)) {
                // One UI / Multi-Theme notification row: Left icon badge + right content column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    // App icon / avatar container
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(iconShape)
                            .then(
                                if (shadeTheme is ShadeTheme.Cyberpunk) {
                                    Modifier
                                        .background(Color(0xFF00F0FF).copy(alpha = 0.10f))
                                        .border(BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f)), iconShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            largeIconBitmap != null -> {
                                Image(
                                    bitmap = largeIconBitmap!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(iconBoxSize),
                                )
                                if (appIconBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(if (compact) 14.dp else 18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            bitmap = appIconBitmap!!,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                        )
                                    }
                                }
                            }
                            appIconBitmap != null -> {
                                Image(
                                    bitmap = appIconBitmap!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(iconImgSize)
                                        .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp)),
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(iconBoxSize)
                                        .clip(RoundedCornerShape(iconRadius))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = appName.take(1).uppercase(),
                                        style = if (compact)
                                            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        else
                                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    // Content column: App header (name · time, expand chevron), Title, Text body
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Text(
                                    text = if (notification.isConversation && notification.conversationTitle != null)
                                        "$appName · ${notification.conversationTitle}"
                                    else if (shadeTheme is ShadeTheme.Cyberpunk) "[${appName.uppercase()}]"
                                    else if (shadeTheme is ShadeTheme.Nothing) appName.uppercase()
                                    else appName,
                                    style = if (compact)
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 11.sp,
                                        )
                                    else
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        ),
                                    color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (shadeTheme is ShadeTheme.Nothing) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD71920)),
                                    )
                                } else {
                                    Text(
                                        text = if (shadeTheme is ShadeTheme.Cyberpunk) "//" else "·",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = if (compact) 10.sp else 11.sp,
                                        ),
                                        color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFFFF007F) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                Text(
                                    text = if (shadeTheme is ShadeTheme.Cyberpunk) "[TIME // $postTimeLabel]" else postTimeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        fontSize = if (compact) 10.sp else 11.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                                if (notification.isOngoing) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Ongoing",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                                        modifier = Modifier.size(if (compact) 9.dp else 11.dp),
                                    )
                                }
                            }

                            val canExpand = notification.actions.isNotEmpty() || notification.picture != null || displayText.length > 50
                            val chevronRotation by animateFloatAsState(
                                targetValue = if (expanded) 180f else 0f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                label = "chevronRotation",
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 2.dp),
                            ) {
                                if (onSnooze != null) {
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            showSettingsMenu = true
                                        },
                                        modifier = Modifier.size(if (compact) 28.dp else 38.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Snooze,
                                            contentDescription = "Snooze notification",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                                            modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                                        )
                                    }
                                }
                                if (canExpand) {
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            expanded = !expanded
                                        },
                                        modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expanded) "Collapse notification details" else "Expand notification details",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(if (compact) 18.dp else 22.dp)
                                                .graphicsLayer { rotationZ = chevronRotation },
                                        )
                                    }
                                }
                            }
                        }

                        if (!compact) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (displayText.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            // Compact mode: ultra-sleek layout fitting more context in minimal height
                            if (!expanded && displayText.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Text(
                                        text = "— $displayText",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 1.dp),
                                )
                                if (expanded && displayText.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = displayText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = Int.MAX_VALUE,
                                    )
                                }
                            }
                        }
                    }
                }

                // Notification progress bar (downloads, installs, etc.)
                val hasProgress = notification.isProgressIndeterminate ||
                    (notification.progressMax > 0 && notification.progress >= 0)
                if (hasProgress) {
                    Spacer(Modifier.height(8.dp))
                    if (notification.isProgressIndeterminate) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { notification.progress.toFloat() / notification.progressMax },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }

                // Settings & Snooze dropdown — shown on long-press (OS-style)
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Notification settings", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showSettingsMenu = false
                            try {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, notification.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                    )
                    if (notification.isOngoing) {
                        DropdownMenuItem(
                            text = { Text("Hide sticky notification", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showSettingsMenu = false
                                onHideChannel(notification.packageName, notification.channelId ?: "sticky")
                            },
                        )
                    }
                    if (notification.channelId != null) {
                        DropdownMenuItem(
                            text = { Text("Turn off notifications", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showSettingsMenu = false
                                onHideChannel(notification.packageName, notification.channelId)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open notification settings →", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showSettingsMenu = false
                                try {
                                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, notification.packageName)
                                        putExtra(Settings.EXTRA_CHANNEL_ID, notification.channelId)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        )
                    } else if (!notification.isOngoing) {
                        DropdownMenuItem(
                            text = { Text("Turn off notifications from app", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showSettingsMenu = false
                                onHideChannel(notification.packageName, "default")
                            },
                        )
                    }
                    if (onSnooze != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        listOf(
                            "Snooze 15 minutes" to 15 * 60 * 1_000L,
                            "Snooze 30 minutes" to 30 * 60 * 1_000L,
                            "Snooze 1 hour"     to 60 * 60 * 1_000L,
                            "Snooze 2 hours"    to 2 * 60 * 60 * 1_000L,
                            "Snooze 4 hours"    to 4 * 60 * 60 * 1_000L,
                            "Snooze 8 hours"    to 8 * 60 * 60 * 1_000L,
                        ).forEach { (label, delayMs) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    haptics.sheetDetent()
                                    onSnooze.invoke(delayMs)
                                    showSettingsMenu = false
                                },
                            )
                        }
                    }
                }

                // BigPicture preview — shown in expanded state
                if (expanded && notification.picture != null) {
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = notification.picture.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }

                if (expanded && notification.actions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        notification.actions.take(3).forEach { action ->
                            val actionInteraction = remember { MutableInteractionSource() }
                            val isActionPressed by actionInteraction.collectIsPressedAsState()
                            val actionScale by animateFloatAsState(
                                targetValue = if (isActionPressed) 0.94f else 1.0f,
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                label = "actionScale",
                            )

                            val actionShape = when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                                is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                                is ShadeTheme.Pixel -> RoundedCornerShape(16.dp)
                                else -> shapes.chip
                            }
                            val actionBorder = when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f))
                                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                                else -> getCardBorder(alpha = 0.35f)
                            }
                            val actionBg = when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> Color(0xFF050B14)
                                is ShadeTheme.Nothing -> Color(0xFF14171C)
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                            val actionText = when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> "[${action.label.uppercase()}]"
                                is ShadeTheme.Nothing -> action.label.uppercase()
                                else -> action.label
                            }

                            Surface(
                                onClick = {
                                    haptics.lightTap()
                                    if (action.replyInput != null) {
                                        replyingAction = action
                                    } else {
                                        try { action.pendingIntent?.send() } catch (_: Exception) {}
                                    }
                                },
                                interactionSource = actionInteraction,
                                shape = actionShape,
                                color = actionBg,
                                border = actionBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (compact) 30.dp else 38.dp)
                                    .graphicsLayer {
                                        scaleX = actionScale
                                        scaleY = actionScale
                                    },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                ) {
                                    val actionFontSize = if (action.label.length > 12) 10.sp else 11.sp
                                    Text(
                                        text = actionText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = actionFontSize,
                                        ),
                                        color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = replyingAction != null) {
                        replyingAction?.let { action ->
                            var replyText by remember(action) { mutableStateOf("") }
                            fun sendReply() {
                                val ri = action.replyInput ?: return
                                if (replyText.isBlank()) return
                                haptics.tileToggleOn()
                                val intent = android.content.Intent().addFlags(android.content.Intent.FLAG_RECEIVER_FOREGROUND)
                                android.app.RemoteInput.addResultsToIntent(
                                    arrayOf(ri), intent,
                                    android.os.Bundle().apply { putCharSequence(ri.resultKey, replyText) }
                                )
                                try { action.pendingIntent?.send(context, 0, intent) } catch (_: Exception) {}
                                replyingAction = null
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Reply…", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = shapes.chip,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { sendReply() }),
                                )
                                Surface(
                                    onClick = { sendReply() },
                                    shape = CircleShape,
                                    color = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send reply",
                                            tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun relativeTime(postTime: Long): String {
    val delta = System.currentTimeMillis() - postTime
    return when {
        delta < 60_000L    -> "now"
        delta < 3_600_000L -> "${delta / 60_000}m ago"
        delta < 86_400_000L -> java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(postTime))
        else -> java.text.SimpleDateFormat("EEE h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(postTime))
    }
}
