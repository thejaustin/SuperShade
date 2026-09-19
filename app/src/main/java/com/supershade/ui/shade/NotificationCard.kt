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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.animation.AnimatedVisibility
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
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
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

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && notification.isClearable) {
                haptics.sheetDetent()
                onDismiss(); true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart else Alignment.CenterEnd
            val progress = kotlin.math.abs(dismissState.progress).coerceIn(0f, 1f)
            val iconScale = (0.6f + progress * 0.5f).coerceIn(0.6f, 1.15f)
            val bgAlpha = (progress * 1.4f).coerceIn(0.2f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE53935).copy(alpha = bgAlpha)),
                contentAlignment = alignment,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        },
        enableDismissFromStartToEnd = notification.isClearable,
        enableDismissFromEndToStart = notification.isClearable,
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            border = getCardBorder(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
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
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // One UI 8 notification row: Left 38dp icon badge + right content column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Prominent 38dp app icon / avatar container
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            largeIconBitmap != null -> {
                                Image(
                                    bitmap = largeIconBitmap!!,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(38.dp),
                                )
                                if (appIconBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            bitmap = appIconBitmap!!,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(12.dp)
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
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = appName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Text(
                                    text = if (notification.isConversation && notification.conversationTitle != null)
                                        "$appName · ${notification.conversationTitle}"
                                    else appName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = postTimeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                )
                            }

                            val canExpand = notification.actions.isNotEmpty() || notification.picture != null
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (onSnooze != null) {
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            showSettingsMenu = true
                                        },
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Snooze,
                                            contentDescription = "Snooze notification",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                if (canExpand) {
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            expanded = !expanded
                                        },
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (expanded) "Collapse notification details" else "Expand notification details",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }

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
                    if (notification.channelId != null) {
                        DropdownMenuItem(
                            text = { Text("Turn off notifications", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
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
                            Surface(
                                onClick = {
                                    haptics.lightTap()
                                    if (action.replyInput != null) {
                                        replyingAction = action
                                    } else {
                                        try { action.pendingIntent?.send() } catch (_: Exception) {}
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = getCardBorder(alpha = 0.35f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                ) {
                                    val actionFontSize = if (action.label.length > 12) 10.sp else 11.sp
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = actionFontSize,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                    shape = RoundedCornerShape(24.dp),
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
