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

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val category = categoryEngine.categorize(sbn)
        val shade = sbn.toShadeNotification(category)
        if (shade.isGroupSummary) {
            _notifications.update { current -> current.filter { it.key != shade.key } }
            return
        }
        _notifications.update { current ->
            val without = current.filter { it.key != shade.key }
            (listOf(shade) + without).sortedByDescending { it.postTime }
        }
    }

    fun onNotificationRemoved(key: String) {
        _notifications.update { current -> current.filter { it.key != key } }
    }

    fun cancelAndRemove(key: String) {
        canceller?.invoke(key)
        onNotificationRemoved(key)
    }

    fun clearAll() {
        _notifications.update { emptyList() }
    }
}
