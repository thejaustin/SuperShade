package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.supershade.haptics.LocalSuperHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.supershade.domain.notification.model.ShadeNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NotificationGroup(
    val packageName: String,
    val groupKey: String?,
    val notifications: List<ShadeNotification>,
) {
    val isStacked: Boolean get() = notifications.size > 1
    val preview: ShadeNotification get() = notifications.first()
}

fun List<ShadeNotification>.toGroups(): List<NotificationGroup> {
    val map = linkedMapOf<String, MutableList<ShadeNotification>>()
    forEach { n ->
        val key = "${n.packageName}::${n.groupKey.orEmpty()}"
        map.getOrPut(key) { mutableListOf() }.add(n)
    }
    return map.values.map { items ->
        NotificationGroup(
            packageName = items.first().packageName,
            groupKey = items.first().groupKey,
            notifications = items,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedNotificationCard(
    group: NotificationGroup,
    onDismissGroup: () -> Unit,
    onDismiss: (String) -> Unit,
    onNotificationClick: (ShadeNotification) -> Unit,
    onSnooze: (String, Long) -> Unit = { _, _ -> },
    onHideChannel: (pkg: String, channelId: String) -> Unit = { _, _ -> },
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalShadeShapeScheme.current
    var expanded by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }

    val appName = remember(group.packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(group.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            group.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    val appIconBitmap by produceState<ImageBitmap?>(null, group.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(group.packageName)
                    .toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                haptics.sheetDetent()
                onDismissGroup()
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
    )

    val dragProgress = kotlin.math.abs(dismissState.progress).coerceIn(0f, 1f)
    val isPastDismissThreshold = dragProgress >= 0.35f
    var hasTickedThreshold by remember { mutableStateOf(false) }
    LaunchedEffect(isPastDismissThreshold) {
        if (isPastDismissThreshold && !hasTickedThreshold) {
            haptics.sheetDetent()
            hasTickedThreshold = true
        } else if (!isPastDismissThreshold && dragProgress < 0.20f) {
            hasTickedThreshold = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart else Alignment.CenterEnd
            val progress = dragProgress

            val badgeScale by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 1.08f else (0.80f + progress * 0.40f).coerceIn(0.80f, 1.0f),
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeBadgeScale",
            )
            val iconRotation by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 0f else 12f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeIconRotation",
            )

            val isSwiping = progress > 0.01f
            val trackBgColor = if (!isSwiping) {
                Color.Transparent
            } else {
                Color(0xFFE53935).copy(alpha = (progress * 0.45f).coerceIn(0f, 0.40f))
            }
            val badgeColor = if (isPastDismissThreshold) Color(0xFFE53935) else Color(0xFFE53935).copy(alpha = 0.85f)

            val iconSlideOffset by animateDpAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    ((progress.coerceIn(0f, 0.35f) / 0.35f - 1f) * 16f).dp
                } else {
                    ((1f - progress.coerceIn(0f, 0.35f) / 0.35f) * 16f).dp
                },
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeIconSlide",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.card)
                    .background(trackBgColor),
                contentAlignment = alignment,
            ) {
                if (isSwiping) {
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
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear group",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = iconRotation },
                        )
                        if (progress > 0.25f) {
                            Text(
                                text = "Clear group",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = modifier.fillMaxWidth(),
    ) {
        val cardScale = (1.0f - dragProgress * 0.04f).coerceIn(0.95f, 1.0f)
        val cardAlpha = if (dragProgress > 0.75f) (1f - (dragProgress - 0.75f) * 2.5f).coerceIn(0.4f, 1.0f) else 1.0f
        val cardElevation = (dragProgress * 8f).dp
        val dynamicCardShape = RoundedCornerShape((20f + dragProgress * 8f).coerceIn(20f, 28f).dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
        ) {

            // Farthest ghost — narrowest, offsets most below the main card
            if (!expanded && group.notifications.size >= 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .height(16.dp)
                        .clip(dynamicCardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)),
                )
            }
            // Closer ghost — slightly less narrow, offset less
            if (!expanded && group.notifications.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .height(12.dp)
                        .clip(dynamicCardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)),
                )
            }

            // Main card — on top, determines Box height
            Card(
                shape = dynamicCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                border = getCardBorder(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { expanded = !expanded },
                        onLongClick = { showSettingsMenu = true },
                    ),
            ) {
                val hPadding = if (compact) 10.dp else 14.dp
                val vPadding = if (compact) 6.dp else 12.dp
                val iconBoxSize = if (compact) 30.dp else 38.dp
                val iconRadius = if (compact) 8.dp else 12.dp
                val iconImgSize = if (compact) 26.dp else 34.dp
                val columnSpacing = if (compact) 9.dp else 12.dp

                Column(modifier = Modifier.padding(horizontal = hPadding, vertical = vPadding)) {
                    // One UI 8 Group header & collapsed preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // App icon badge
                        Box(
                            modifier = Modifier
                                .size(iconBoxSize)
                                .clip(RoundedCornerShape(iconRadius)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (appIconBitmap != null) {
                                Image(
                                    bitmap = appIconBitmap!!,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(iconImgSize)
                                        .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp)),
                                )
                            } else {
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

                        // App Name + Collapsed preview column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = appName,
                                    style = if (compact)
                                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                                    else
                                        MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
                                ) {
                                    Text(
                                        text = "${group.notifications.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (compact) 10.sp else 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = if (compact) 6.dp else 7.dp, vertical = 1.5.dp),
                                    )
                                    if (expanded) {
                                        IconButton(
                                            onClick = {
                                                onDismissGroup()
                                            },
                                            modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ClearAll,
                                                contentDescription = "Clear group",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                                            )
                                        }
                                    }
                                    val groupChevronRotation by animateFloatAsState(
                                        targetValue = if (expanded) 180f else 0f,
                                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                        label = "groupChevronRotation",
                                    )
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            expanded = !expanded
                                        },
                                        modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expanded) "Collapse group" else "Expand group",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(if (compact) 18.dp else 22.dp)
                                                .graphicsLayer { rotationZ = groupChevronRotation },
                                        )
                                    }
                                }
                            }

                            // Collapsed preview: first notification title + text + "+N more"
                            if (!expanded) {
                                val preview = group.preview
                                val displayTitle = preview.title.ifBlank { appName }
                                if (compact) {
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
                                        if (preview.text.isNotBlank()) {
                                            Text(
                                                text = "— ${preview.text}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (preview.text.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = preview.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (group.notifications.size > 1) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "+${group.notifications.size - 1} more",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Settings & Snooze dropdown for the whole group — shown on long-press (OS-style)
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
                                        putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        )
                        val channelId = group.preview.channelId
                        if (channelId != null) {
                            DropdownMenuItem(
                                text = { Text("Turn off notifications", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onHideChannel(group.packageName, channelId)
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
                                            putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Turn off notifications from app", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onHideChannel(group.packageName, "default")
                                },
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        listOf(
                            "Snooze 15 minutes" to 15 * 60 * 1_000L,
                            "Snooze 1 hour"     to 60 * 60 * 1_000L,
                            "Snooze 4 hours"    to 4 * 60 * 60 * 1_000L,
                        ).forEach { (label, delayMs) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    group.notifications.forEach { onSnooze(it.key, delayMs) }
                                    showSettingsMenu = false
                                },
                            )
                        }
                    }
                }

                // Expanded: individual cards grouped by notification channel
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                    exit = shrinkVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                ) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        // Group notifications by channelId so we can show channel sub-headers
                        val byChannel = remember(group.notifications) {
                            group.notifications
                                .groupBy { it.channelId ?: "" }
                                .entries.toList()
                        }
                        val multiChannel = byChannel.size > 1

                        byChannel.forEachIndexed { channelIdx, (channelId, channelNotifs) ->
                            // ── Channel sub-header (only shown when > 1 channel present) ──────
                            if (multiChannel) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = channelId.ifBlank { "Default" }.let { id ->
                                            // Format snake_case or dot.separated channel IDs into Title Case
                                            id.replace(Regex("[_.]"), " ")
                                              .split(" ")
                                              .joinToString(" ") { word ->
                                                  word.replaceFirstChar { it.uppercase() }
                                              }
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                    )
                                    // "Manage" chip taps to channel settings
                                    Surface(
                                        onClick = {
                                            if (channelId.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                                        putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.60f),
                                        modifier = Modifier,
                                    ) {
                                        Text(
                                            text = "Manage",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }

                            channelNotifs.forEachIndexed { index, notification ->
                                NotificationCard(
                                    notification = notification,
                                    onDismiss = { onDismiss(notification.key) },
                                    onClick = { onNotificationClick(notification) },
                                    onSnooze = { delayMs -> onSnooze(notification.key, delayMs) },
                                    compact = compact,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = if (compact) 2.dp else 4.dp),
                                )
                                val isLast = channelIdx == byChannel.lastIndex && index == channelNotifs.lastIndex
                                if (!isLast) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
