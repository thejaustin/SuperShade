package com.supershade.domain.notification

import android.service.notification.StatusBarNotification
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.domain.notification.model.toShadeNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationRepository {

    private val categoryEngine = CategoryEngine()
    private val _notifications = MutableStateFlow<List<ShadeNotification>>(emptyList())
    val notifications: StateFlow<List<ShadeNotification>> = _notifications.asStateFlow()

    private val _newNotifications = MutableSharedFlow<ShadeNotification>(extraBufferCapacity = 16)
    val newNotifications: SharedFlow<ShadeNotification> = _newNotifications.asSharedFlow()

    // Set by NotificationCollector when the listener service is connected.
    // Called by cancelAndRemove to cancel the notification at the system level.
    var canceller: ((String) -> Unit)? = null

    fun refresh() {
        com.supershade.service.NotificationCollector.instance?.refreshNotifications()
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val category = categoryEngine.categorize(sbn)
        val shade = sbn.toShadeNotification(category)

        if (shade.title.isBlank() && shade.text.isBlank()) return

        // Capture whether this key is genuinely new BEFORE updating the list.
        val isNew = _notifications.value.none { it.key == shade.key }

        _notifications.update { current ->
            val without = current.filter { it.key != shade.key }
            // If this is a group summary and we already have child notifications for
            // this group, omit the summary — children carry all the visible content.
            if (shade.isGroupSummary && without.any {
                    it.packageName == shade.packageName &&
                    it.groupKey == shade.groupKey &&
                    !it.isGroupSummary
                }) {
                without
            } else {
                (listOf(shade) + without).sortedByDescending { it.postTime }
            }
        }

        // Only show a heads-up peek card for genuinely new, non-summary notifications.
        // Updates to existing notifications (badge count changes, progress updates, etc.)
        // should not trigger another toast — they're already visible in the feed.
        if (isNew && !shade.isGroupSummary) {
            _newNotifications.tryEmit(shade)
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
