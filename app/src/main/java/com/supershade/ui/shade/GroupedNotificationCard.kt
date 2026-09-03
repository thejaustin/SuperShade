package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

@Composable
fun GroupedNotificationCard(
    group: NotificationGroup,
    onDismissGroup: () -> Unit,
    onDismiss: (String) -> Unit,
    onNotificationClick: (ShadeNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
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

    Column(modifier = modifier.fillMaxWidth()) {
        // Stacked card peek effect — show up to 2 ghost cards behind
        if (!expanded && group.notifications.size >= 3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(12.dp)
                    .offset(y = 10.dp)
                    .zIndex(0f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            )
        }
        if (!expanded && group.notifications.size >= 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(12.dp)
                    .offset(y = 6.dp)
                    .zIndex(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
            )
        }

        // Main card
        Card(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(2f)
                .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // Group header
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

                if (!expanded) {
                    // Collapsed: show preview of first notification
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
            }

            // Expanded: individual cards with dividers
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
                            onDismiss = {
                                onDismiss(notification.key)
                                if (group.notifications.size == 1) expanded = false
                            },
                            onClick = { onNotificationClick(notification) },
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
