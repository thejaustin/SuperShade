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
    var canceller: ((String) -> Unit)? = null
    var snoozer: ((String, Long) -> Unit)? = null
    var clearAller: (() -> Unit)? = null

    // key → timestamp when snooze expires; in-memory only, reset on service restart
    private val snoozedUntil = mutableMapOf<String, Long>()

    fun snooze(key: String, delayMs: Long) {
        snoozedUntil[key] = System.currentTimeMillis() + delayMs
        snoozer?.invoke(key, delayMs)
        onNotificationRemoved(key)
    }

    private fun isSnoozed(key: String): Boolean {
        val until = snoozedUntil[key] ?: return false
        return if (System.currentTimeMillis() < until) true
        else { snoozedUntil.remove(key); false }
    }

    fun refresh() {
        com.supershade.service.NotificationCollector.instance?.refreshNotifications()
    }

    fun onNotificationPosted(sbn: StatusBarNotification) {
        val category = categoryEngine.categorize(sbn)
        val shade = sbn.toShadeNotification(category)

        if (shade.title.isBlank() && shade.text.isBlank()) return
        if (isSnoozed(shade.key)) return

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

        // Only show a heads-up peek card for genuinely new, non-summary, non-ongoing notifications.
        // Updates to existing notifications or ongoing services shouldn't spam toasts.
        if (isNew && !shade.isGroupSummary && !shade.isOngoing && shade.packageName != "com.supershade") {
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
        if (clearAller != null) {
            clearAller?.invoke()
        } else {
            val dismissible = _notifications.value.filter { it.isClearable }
            dismissible.forEach { note -> canceller?.invoke(note.key) }
        }
        _notifications.update { current -> current.filter { !it.isClearable } }
    }

    fun clearAll() {
        _notifications.update { emptyList() }
    }
}
