package com.supershade.ui.shade

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.haptics.LocalSuperHaptics
import androidx.compose.material3.HorizontalDivider
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun NotificationAccessCard(
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val shapes = LocalShadeShapeScheme.current

    Surface(
        shape = shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
        border = getCardBorder(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "Notification Access Required",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Grant notification listener permission so SuperShade can display, organize into categories, and manage incoming alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    haptics.sheetDetent()
                    try {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Grant Access",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsView(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = "No notifications",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
        Text(
            text = "You're all caught up",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
        Surface(
            onClick = {
                haptics.sheetDetent()
                onOpenHistory()
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
            border = getCardBorder(alpha = 0.25f),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Notification history",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

enum class FeedSectionType {
    PINNED,
    CONVERSATIONS,
    ALERTS,
    SILENT,
}

fun FeedSectionType.getTitle(theme: ShadeTheme): String = when (theme) {
    is ShadeTheme.Cyberpunk -> when (this) {
        FeedSectionType.PINNED -> "[LOCKED // SYS_PIN]"
        FeedSectionType.CONVERSATIONS -> "[COMMS // INCOMING]"
        FeedSectionType.ALERTS -> "[TELEMETRY // ALERTS]"
        FeedSectionType.SILENT -> "[PASSIVE // BACKGROUND]"
    }
    is ShadeTheme.Nothing -> when (this) {
        FeedSectionType.PINNED -> "PINNED"
        FeedSectionType.CONVERSATIONS -> "CONVERSATIONS"
        FeedSectionType.ALERTS -> "NOTIFICATIONS"
        FeedSectionType.SILENT -> "SILENT"
    }
    is ShadeTheme.Pixel, is ShadeTheme.PureMaterial -> when (this) {
        FeedSectionType.PINNED -> "Pinned"
        FeedSectionType.CONVERSATIONS -> "Conversations"
        FeedSectionType.ALERTS -> "Alerts"
        FeedSectionType.SILENT -> "Silent notifications"
    }
    else -> when (this) {
        FeedSectionType.PINNED -> "Pinned"
        FeedSectionType.CONVERSATIONS -> "Conversations"
        FeedSectionType.ALERTS -> "Alerts"
        FeedSectionType.SILENT -> "Silent"
    }
}

data class NotificationFeedSection(
    val type: FeedSectionType,
    val title: String,
    val groups: List<NotificationGroup>,
)

fun List<NotificationGroup>.toSections(
    pinnedKeys: Set<String>,
    theme: ShadeTheme,
): List<NotificationFeedSection> {
    if (isEmpty()) return emptyList()

    val pinned = mutableListOf<NotificationGroup>()
    val conversations = mutableListOf<NotificationGroup>()
    val alerts = mutableListOf<NotificationGroup>()
    val silent = mutableListOf<NotificationGroup>()

    forEach { group ->
        val isPinned = group.notifications.any { it.key in pinnedKeys }
        if (isPinned) {
            pinned.add(group)
        } else {
            val isConv = group.preview.isConversation ||
                group.preview.category == ShadeCategory.Conversations ||
                group.preview.category == ShadeCategory.Messages ||
                group.notifications.any { it.isConversation }
            val isSilent = group.preview.category == ShadeCategory.Silent ||
                (!group.preview.isOngoing && group.notifications.all { it.category == ShadeCategory.Silent })

            when {
                isConv -> conversations.add(group)
                isSilent -> silent.add(group)
                else -> alerts.add(group)
            }
        }
    }

    val sections = mutableListOf<NotificationFeedSection>()
    if (pinned.isNotEmpty()) {
        sections.add(NotificationFeedSection(FeedSectionType.PINNED, FeedSectionType.PINNED.getTitle(theme), pinned))
    }
    if (conversations.isNotEmpty()) {
        sections.add(NotificationFeedSection(FeedSectionType.CONVERSATIONS, FeedSectionType.CONVERSATIONS.getTitle(theme), conversations))
    }
    if (alerts.isNotEmpty()) {
        sections.add(NotificationFeedSection(FeedSectionType.ALERTS, FeedSectionType.ALERTS.getTitle(theme), alerts))
    }
    if (silent.isNotEmpty()) {
        sections.add(NotificationFeedSection(FeedSectionType.SILENT, FeedSectionType.SILENT.getTitle(theme), silent))
    }
    return sections
}

@Composable
fun FeedSectionHeader(
    section: NotificationFeedSection,
    theme: ShadeTheme,
    modifier: Modifier = Modifier,
) {
    val totalCount = section.groups.sumOf { it.notifications.size }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        when (theme) {
            is ShadeTheme.Cyberpunk -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp,
                        ),
                        color = Color(0xFF00F0FF),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0xFF00F0FF).copy(alpha = 0.25f),
                    )
                    Text(
                        text = "[CNT:${totalCount.toString().padStart(2, '0')}]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = Color(0xFFFF0055),
                    )
                }
            }
            is ShadeTheme.Nothing -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD71920)),
                    )
                    Text(
                        text = section.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "($totalCount)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
            is ShadeTheme.Pixel, is ShadeTheme.PureMaterial -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "$totalCount",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (section.type) {
                                    FeedSectionType.PINNED -> Color(0xFFFFA000)
                                    FeedSectionType.CONVERSATIONS -> MaterialTheme.colorScheme.primary
                                    FeedSectionType.ALERTS -> MaterialTheme.colorScheme.secondary
                                    FeedSectionType.SILENT -> MaterialTheme.colorScheme.outline
                                }
                            ),
                    )
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    ) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationFeed(
    notifications: List<ShadeNotification>,
    onDismiss: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationClick: (ShadeNotification) -> Unit = {},
    onSnooze: (String, Long) -> Unit = { _, _ -> },
    onHideChannel: (pkg: String, channelId: String) -> Unit = { _, _ -> },
    onOpenHistory: () -> Unit = {},
    pinnedKeys: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    compact: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val isAccessGranted = remember(context) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val shadeTheme = LocalShadeTheme.current

    if (!isAccessGranted) {
        NotificationAccessCard(modifier = modifier)
    } else if (notifications.isEmpty()) {
        EmptyNotificationsView(modifier = modifier, onOpenHistory = onOpenHistory)
    } else {
        val groups = remember(notifications) { notifications.toGroups() }
        val sections = remember(groups, pinnedKeys, shadeTheme) {
            groups.toSections(pinnedKeys, shadeTheme)
        }
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = if (compact) 4.dp else 6.dp, bottom = 68.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
        ) {
            item {
                val hasClearable = notifications.any { it.isClearable }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${notifications.size} notification${if (notifications.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeaderActionButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = {
                                haptics.sheetDetent()
                                onOpenHistory()
                            },
                        )

                        if (hasClearable) {
                            HeaderActionButton(
                                icon = Icons.Default.ClearAll,
                                label = "Clear all",
                                isPrimary = true,
                                onClick = {
                                    haptics.sheetDetent()
                                    onClearAll()
                                },
                            )
                        }
                    }
                }
            }
            sections.forEach { section ->
                if (sections.size > 1 || section.type == FeedSectionType.PINNED || section.type == FeedSectionType.CONVERSATIONS) {
                    item(key = "section_header_${section.type.name}") {
                        FeedSectionHeader(section = section, theme = shadeTheme)
                    }
                }
                items(
                    items = section.groups,
                    key = { "${it.packageName}::${it.groupKey.orEmpty()}::${section.type.name}" },
                ) { group ->
                    if (group.isStacked) {
                        GroupedNotificationCard(
                            group = group,
                            onDismissGroup = { group.notifications.forEach { onDismiss(it.key) } },
                            onDismiss = onDismiss,
                            onNotificationClick = onNotificationClick,
                            onSnooze = onSnooze,
                            onHideChannel = onHideChannel,
                            pinnedKeys = pinnedKeys,
                            onTogglePin = onTogglePin,
                            compact = compact,
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                        val notification = group.preview
                        NotificationCard(
                            notification = notification,
                            onDismiss = { onDismiss(notification.key) },
                            onClick = { onNotificationClick(notification) },
                            onSnooze = { delayMs -> onSnooze(notification.key, delayMs) },
                            onHideChannel = onHideChannel,
                            isPinned = notification.key in pinnedKeys,
                            onTogglePin = { onTogglePin(notification.key) },
                            compact = compact,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Non-lazy version of [NotificationFeed] for use inside the TOGETHER mode vertical scroll.
 * Uses [Column] instead of [LazyColumn] to avoid nested scroll conflicts.
 */
@Composable
fun TogetherNotificationFeed(
    notifications: List<ShadeNotification>,
    onDismiss: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationClick: (ShadeNotification) -> Unit = {},
    onSnooze: (String, Long) -> Unit = { _, _ -> },
    onHideChannel: (pkg: String, channelId: String) -> Unit = { _, _ -> },
    onOpenHistory: () -> Unit = {},
    pinnedKeys: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    compact: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }

    val isAccessGranted = remember(context) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    if (!isAccessGranted) {
        NotificationAccessCard(modifier = modifier)
    } else if (notifications.isEmpty()) {
        EmptyNotificationsView(modifier = modifier, onOpenHistory = onOpenHistory)
    } else {
        val shadeTheme = LocalShadeTheme.current
        val groups = remember(notifications) { notifications.toGroups() }
        val sections = remember(groups, pinnedKeys, shadeTheme) {
            groups.toSections(pinnedKeys, shadeTheme)
        }
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
        ) {
            // Header row
            val hasClearable = notifications.any { it.isClearable }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${notifications.size} notification${if (notifications.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HeaderActionButton(
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = {
                            haptics.sheetDetent()
                            onOpenHistory()
                        },
                    )
                    if (hasClearable) {
                        HeaderActionButton(
                            icon = Icons.Default.ClearAll,
                            label = "Clear all",
                            isPrimary = true,
                            onClick = {
                                haptics.sheetDetent()
                                onClearAll()
                            },
                        )
                    }
                }
            }

            // Notification groups organized by sections (Column, not LazyColumn)
            sections.forEach { section ->
                if (sections.size > 1 || section.type == FeedSectionType.PINNED || section.type == FeedSectionType.CONVERSATIONS) {
                    FeedSectionHeader(section = section, theme = shadeTheme)
                }
                section.groups.forEach { group ->
                    if (group.isStacked) {
                        GroupedNotificationCard(
                            group = group,
                            onDismissGroup = { group.notifications.forEach { onDismiss(it.key) } },
                            onDismiss = onDismiss,
                            onNotificationClick = onNotificationClick,
                            onSnooze = onSnooze,
                            onHideChannel = onHideChannel,
                            pinnedKeys = pinnedKeys,
                            onTogglePin = onTogglePin,
                            compact = compact,
                        )
                    } else {
                        val notification = group.preview
                        NotificationCard(
                            notification = notification,
                            onDismiss = { onDismiss(notification.key) },
                            onClick = { onNotificationClick(notification) },
                            onSnooze = { delayMs -> onSnooze(notification.key, delayMs) },
                            onHideChannel = onHideChannel,
                            isPinned = notification.key in pinnedKeys,
                            onTogglePin = { onTogglePin(notification.key) },
                            compact = compact,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "headerBtnScale",
    )
    val shapes = LocalShadeShapeScheme.current

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shapes.chip,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isPrimary) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
            },
        ),
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
