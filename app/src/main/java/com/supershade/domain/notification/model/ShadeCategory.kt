package com.supershade.domain.notification.model

import android.app.Notification

enum class ShadeCategory(val label: String, val androidCategory: String?) {
    All("All", null),
    Messages("Messages", Notification.CATEGORY_MESSAGE),
    Social("Social", Notification.CATEGORY_SOCIAL),
    Email("Email", Notification.CATEGORY_EMAIL),
    Calls("Calls", Notification.CATEGORY_CALL),
    Alarms("Alarms", Notification.CATEGORY_ALARM),
    System("System", Notification.CATEGORY_SYSTEM),
    Apps("Apps", null)
}
