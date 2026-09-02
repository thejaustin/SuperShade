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
    return ShadeNotification(
        key = key,
        packageName = packageName,
        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
        subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        category = category,
        smallIcon = notification.smallIcon,
        isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        groupKey = notification.group,
        postTime = postTime,
        actions = notification.actions?.map {
            NotificationAction(it.title?.toString() ?: "", it.actionIntent)
        } ?: emptyList(),
        isClearable = isClearable,
        contentIntent = notification.contentIntent
    )
}
