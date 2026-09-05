package com.supershade.ui.shade

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.supershade.domain.notification.model.NotificationAction
import com.supershade.domain.notification.model.ShadeNotification

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
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
) {
    var expanded by remember { mutableStateOf(false) }
    var replyingAction by remember { mutableStateOf<NotificationAction?>(null) }
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && notification.isClearable) {
                onDismiss(); true
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
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = if (onSnooze != null) { { showSnoozeMenu = true } } else null,
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // App icon + app name + post time header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
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
                            text = if (notification.isConversation && notification.conversationTitle != null)
                                "$appName · ${notification.conversationTitle}"
                            else appName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = postTimeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Title + body + optional large icon (sender avatar / image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (displayText.isNotBlank()) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (largeIconBitmap != null) {
                        Spacer(Modifier.width(10.dp))
                        Image(
                            bitmap = largeIconBitmap!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    val canExpand = notification.actions.isNotEmpty() || notification.picture != null
                    if (canExpand) {
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

                // Snooze dropdown — shown on long-press
                DropdownMenu(
                    expanded = showSnoozeMenu,
                    onDismissRequest = { showSnoozeMenu = false },
                ) {
                    listOf(
                        "Snooze 15 minutes" to 15 * 60 * 1_000L,
                        "Snooze 1 hour"     to 60 * 60 * 1_000L,
                        "Snooze 4 hours"    to 4 * 60 * 60 * 1_000L,
                    ).forEach { (label, delayMs) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onSnooze?.invoke(delayMs)
                                showSnoozeMenu = false
                            },
                        )
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
                            OutlinedButton(
                                onClick = {
                                    if (action.replyInput != null) {
                                        replyingAction = action
                                    } else {
                                        try { action.pendingIntent?.send() } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = replyingAction != null) {
                        replyingAction?.let { action ->
                            var replyText by remember(action) { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Reply…", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                IconButton(
                                    onClick = {
                                        val ri = action.replyInput ?: return@IconButton
                                        val intent = android.content.Intent()
                                        android.app.RemoteInput.addResultsToIntent(
                                            arrayOf(ri), intent,
                                            android.os.Bundle().apply { putCharSequence(ri.resultKey, replyText) }
                                        )
                                        try { action.pendingIntent?.send(intent) } catch (_: Exception) {}
                                        replyingAction = null
                                    },
                                    enabled = replyText.isNotBlank(),
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send reply")
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
