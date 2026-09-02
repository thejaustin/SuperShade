package com.supershade.domain.notification.model

import android.app.Notification
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification

data class ShadeNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val category: ShadeCategory,
    val smallIcon: Icon?,
    val isGroupSummary: Boolean,
    val groupKey: String?,
    val postTime: Long,
    val actions: List<NotificationAction>,
    val isClearable: Boolean,
    val contentIntent: android.app.PendingIntent? = null
)

data class NotificationAction(
    val label: String,
    val pendingIntent: android.app.PendingIntent?
)

fun StatusBarNotification.toShadeNotification(category: ShadeCategory): ShadeNotification {
    val extras = notification.extras
    val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        ?: notification.tickerText?.toString()
        ?: ""

    val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
        ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        ?: ""

    val rawSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()

    return ShadeNotification(
        key = key,
        packageName = packageName,
        title = rawTitle.trim(),
        text = rawText.trim(),
        subText = rawSubText?.trim(),
        category = category,
        smallIcon = notification.smallIcon,
        isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        groupKey = notification.group,
        postTime = postTime,
        actions = notification.actions?.mapNotNull { action ->
            val title = action.title?.toString()?.trim()
            if (!title.isNullOrBlank()) NotificationAction(title, action.actionIntent) else null
        } ?: emptyList(),
        isClearable = isClearable,
        contentIntent = notification.contentIntent
    )
}
