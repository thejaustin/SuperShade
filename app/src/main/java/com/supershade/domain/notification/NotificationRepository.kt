package com.supershade.domain.notification

import android.service.notification.StatusBarNotification
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.domain.notification.model.toShadeNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DismissedNotificationRecord(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val dismissedTime: Long = System.currentTimeMillis(),
    val category: ShadeCategory = ShadeCategory.All,
    val isClearable: Boolean = true,
    val channelId: String? = null,
)

class NotificationRepository {

    companion object {
        /**
         * Packages for voice recorder and audio recording tools.
         * Their ongoing recording notifications are routed to the Media Card instead of the feed.
         */
        val VOICE_RECORDER_PACKAGES = setOf(
            "com.sec.android.app.voicenote",
            "com.samsung.android.voicenote",
            "com.samsung.android.app.voicerecorder",
            "com.samsung.android.voiceserviceplatform",
            "com.google.android.apps.recorder",
        )

        private val MEDIA_ONLY_PACKAGES = VOICE_RECORDER_PACKAGES

        /**
         * Returns true if [sbn] is an ongoing notification from a known media/recording app.
         */
        private fun isMediaOnlyNotification(sbn: android.service.notification.StatusBarNotification): Boolean {
            if (sbn.packageName !in MEDIA_ONLY_PACKAGES) return false
            val flags = sbn.notification?.flags ?: return false
            return (flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        }
    }

    private val categoryEngine = CategoryEngine()
    private val _notifications = MutableStateFlow<List<ShadeNotification>>(emptyList())
    val notifications: StateFlow<List<ShadeNotification>> = _notifications.asStateFlow()

    private val _dismissedHistory = MutableStateFlow<List<DismissedNotificationRecord>>(emptyList())
    val dismissedHistory: StateFlow<List<DismissedNotificationRecord>> = _dismissedHistory.asStateFlow()

    private val _voiceRecorderNotification = MutableStateFlow<ShadeNotification?>(null)
    val voiceRecorderNotification: StateFlow<ShadeNotification?> = _voiceRecorderNotification.asStateFlow()

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
        // Apps like Samsung Voice Recorder post an ongoing MediaStyle service notification
        // while also registering a MediaSession. Suppress them from the notification feed;
        // they are routed to the media card.
        if (isMediaOnlyNotification(sbn)) {
            val category = categoryEngine.categorize(sbn)
            val shade = sbn.toShadeNotification(category)
            _voiceRecorderNotification.value = shade
            return
        }

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
        if (_voiceRecorderNotification.value?.key == key) {
            _voiceRecorderNotification.value = null
        }
        val removed = _notifications.value.find { it.key == key }
        if (removed != null && removed.packageName != "com.supershade") {
            val record = DismissedNotificationRecord(
                key = removed.key,
                packageName = removed.packageName,
                title = removed.title,
                text = removed.text,
                postTime = removed.postTime,
                dismissedTime = System.currentTimeMillis(),
                category = removed.category,
                isClearable = removed.isClearable,
                channelId = removed.channelId,
            )
            _dismissedHistory.update { current ->
                (listOf(record) + current.filter { it.key != key }).take(60)
            }
        }
        _notifications.update { current -> current.filter { it.key != key } }
    }

    fun cancelAndRemove(key: String) {
        canceller?.invoke(key)
        onNotificationRemoved(key)
    }

    fun cancelAll() {
        val now = System.currentTimeMillis()
        val dismissible = _notifications.value.filter { it.isClearable }
        val newRecords = dismissible.map { note ->
            DismissedNotificationRecord(
                key = note.key,
                packageName = note.packageName,
                title = note.title,
                text = note.text,
                postTime = note.postTime,
                dismissedTime = now,
                category = note.category,
                isClearable = note.isClearable,
                channelId = note.channelId,
            )
        }
        _dismissedHistory.update { current ->
            (newRecords + current).distinctBy { it.key }.take(60)
        }
        if (clearAller != null) {
            clearAller?.invoke()
        } else {
            dismissible.forEach { note -> canceller?.invoke(note.key) }
        }
        _notifications.update { current -> current.filter { !it.isClearable } }
    }

    fun clearAll() {
        _notifications.update { emptyList() }
    }

    fun clearDismissedHistory() {
        _dismissedHistory.value = emptyList()
    }
}
