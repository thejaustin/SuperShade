package com.supershade.domain.notification

import android.service.notification.StatusBarNotification
import com.supershade.domain.notification.model.ShadeCategory

class CategoryEngine {

    fun categorize(sbn: StatusBarNotification): ShadeCategory {
        val androidCategory = sbn.notification.category
        if (androidCategory != null) {
            ShadeCategory.entries.forEach { cat ->
                if (cat.androidCategory == androidCategory) return cat
            }
        }
        // Package-based heuristics for uncategorized notifications
        return when {
            isMessagingApp(sbn.packageName) -> ShadeCategory.Messages
            isSocialApp(sbn.packageName) -> ShadeCategory.Social
            isEmailApp(sbn.packageName) -> ShadeCategory.Email
            isSystemApp(sbn.packageName) -> ShadeCategory.System
            else -> ShadeCategory.Apps
        }
    }

    private fun isMessagingApp(pkg: String) = pkg in setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
        "com.viber.voip", "com.discord", "com.snapchat.android",
        "com.google.android.apps.messaging", "com.samsung.android.messaging"
    )

    private fun isSocialApp(pkg: String) = pkg in setOf(
        "com.instagram.android", "com.twitter.android", "com.facebook.katana",
        "com.reddit.frontpage", "com.zhiliaoapp.musically", "com.linkedin.android"
    )

    private fun isEmailApp(pkg: String) = pkg in setOf(
        "com.google.android.gm", "com.microsoft.office.outlook",
        "com.samsung.android.email.provider", "com.yahoo.mobile.client.android.mail"
    )

    private fun isSystemApp(pkg: String) = pkg.startsWith("com.android.") ||
        pkg.startsWith("com.samsung.android.") ||
        pkg == "android"
}
