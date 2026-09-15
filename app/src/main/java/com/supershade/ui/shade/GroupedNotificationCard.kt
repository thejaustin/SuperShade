package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                onDismissGroup(); true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete group",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        // Box so ghost-peek strips can be drawn behind and below the main card.
        // Ghost strips are first children (drawn behind), main card is last (on top).
        Box(modifier = Modifier.fillMaxWidth()) {

            // Farthest ghost — narrowest, offsets most below the main card
            if (!expanded && group.notifications.size >= 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
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
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)),
                )
            }

            // Main card — on top, determines Box height
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                ),
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
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    // Group header row: app icon + name + count badge + expand toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (appIconBitmap != null) {
                                Image(
                                    bitmap = appIconBitmap!!,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                            }
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${group.notifications.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Collapsed preview: first notification title + text + "+N more"
                    if (!expanded) {
                        Spacer(Modifier.height(6.dp))
                        val preview = group.preview
                        val displayTitle = preview.title.ifBlank { appName }
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (preview.text.isNotBlank()) {
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
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

                // Expanded: individual cards separated by subtle dividers
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                    exit = shrinkVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                ) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                        group.notifications.forEachIndexed { index, notification ->
                            NotificationCard(
                                notification = notification,
                                onDismiss = { onDismiss(notification.key) },
                                onClick = { onNotificationClick(notification) },
                                onSnooze = { delayMs -> onSnooze(notification.key, delayMs) },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                            if (index < group.notifications.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
