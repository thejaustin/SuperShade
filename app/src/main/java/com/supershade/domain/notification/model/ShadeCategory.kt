package com.supershade.domain.notification.model

import android.app.Notification

enum class ShadeCategory(val label: String, val androidCategory: String?) {
    All("All", null),

    // Domain categories (One UI)
    Messages("Messages", Notification.CATEGORY_MESSAGE),
    Social("Social", Notification.CATEGORY_SOCIAL),
    Email("Email", Notification.CATEGORY_EMAIL),
    Calls("Calls", Notification.CATEGORY_CALL),
    Productivity("Tasks", null),
    Media("Media", null),
    Alarms("Alarms", Notification.CATEGORY_ALARM),
    System("System", Notification.CATEGORY_SYSTEM),
    Apps("Apps", null),

    // Priority categories (Pixel / AOSP)
    Conversations("Conversations", null),
    Alerting("Alerting", null),
    Silent("Silent", null),

    // Essential categories (Nothing OS)
    Essential("Essential", null),
    General("General", null);

    fun displayLabel(modeId: String = "one_ui"): String = when (modeId) {
        "cyberpunk" -> when (this) {
            All -> "[ALL_PACKETS]"
            Messages -> "[COMM_STREAM]"
            Social -> "[NET_MESH]"
            Email -> "[DATA_DISPATCH]"
            Calls -> "[VOX_LINK]"
            Productivity -> "[TASK_CYCLES]"
            Media -> "[AUDIO_FEED]"
            Alarms -> "[ALERT_SIGNAL]"
            System -> "[NET_KERN]"
            Apps -> "[APP_PAYLOADS]"
            Conversations -> "[COMMS_PRIORITY]"
            Alerting -> "[URGENT_TELEMETRY]"
            Silent -> "[PASSIVE_STREAM]"
            Essential -> "[VIP_FEED]"
            General -> "[GENERAL_NET]"
        }
        "essential" -> when (this) {
            All -> "ALL"
            Essential -> "ESSENTIAL"
            General -> "GENERAL"
            Messages -> "MESSAGES"
            Social -> "SOCIAL"
            Calls -> "CALLS"
            else -> label.uppercase()
        }
        else -> label
    }
}
