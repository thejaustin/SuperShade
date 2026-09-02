package com.supershade.domain.notification

import android.service.notification.StatusBarNotification
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.domain.notification.model.toShadeNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationRepository {

    private val categoryEngine = CategoryEngine()
    private val _notifications = MutableStateFlow<List<ShadeNotification>>(emptyList())
    val notifications: StateFlow<List<ShadeNotification>> = _notifications.asStateFlow()

    // Set by NotificationCollector when the listener service is connected.
    // Called by cancelAndRemove to cancel the notification at the system level.
    var canceller: ((String) -> Unit)? = null

    fun refresh() {
        com.supershade.service.NotificationCollector.instance?.refreshNotifications()
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val category = categoryEngine.categorize(sbn)
        val shade = sbn.toShadeNotification(category)

        // Ignore phantom notifications with neither title nor text
        if (shade.title.isBlank() && shade.text.isBlank()) {
            return
        }

        _notifications.update { current ->
            val without = current.filter { it.key != shade.key }
            // If this is a group summary and we already have child notifications for this group, omit summary
            if (shade.isGroupSummary && without.any { it.packageName == shade.packageName && it.groupKey == shade.groupKey && !it.isGroupSummary }) {
                without
            } else {
                (listOf(shade) + without).sortedByDescending { it.postTime }
            }
        }
    }

    fun onNotificationRemoved(key: String) {
        _notifications.update { current -> current.filter { it.key != key } }
    }

    fun cancelAndRemove(key: String) {
        canceller?.invoke(key)
        onNotificationRemoved(key)
    }

    fun cancelAll() {
        val dismissible = _notifications.value.filter { it.isClearable }
        dismissible.forEach { note -> canceller?.invoke(note.key) }
        _notifications.update { current -> current.filter { !it.isClearable } }
    }

    fun clearAll() {
        _notifications.update { emptyList() }
    }
}
