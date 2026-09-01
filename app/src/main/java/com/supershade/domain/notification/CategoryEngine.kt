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
            isCallsApp(sbn.packageName) -> ShadeCategory.Calls
            isMessagingApp(sbn.packageName) -> ShadeCategory.Messages
            isSocialApp(sbn.packageName) -> ShadeCategory.Social
            isEmailApp(sbn.packageName) -> ShadeCategory.Email
            isSystemApp(sbn.packageName) -> ShadeCategory.System
            else -> ShadeCategory.Apps
        }
    }

    private fun isCallsApp(pkg: String) = pkg in setOf(
        "com.google.android.dialer", "com.samsung.android.dialer",
        "com.android.dialer", "com.truecaller"
    )

    private fun isMessagingApp(pkg: String) = pkg in setOf(
        // WhatsApp
        "com.whatsapp", "com.whatsapp.w4b",
        // Telegram
        "org.telegram.messenger", "org.thunderdog.challegram", "org.telegram.messenger.web",
        // Signal / Wire / Threema / Element
        "org.thoughtcrime.securesms", "com.wire", "ch.threema.app", "im.vector.app",
        // Viber / Discord / Snapchat
        "com.viber.voip", "com.discord", "com.snapchat.android",
        // Slack / Teams / Skype
        "com.Slack", "com.microsoft.teams", "com.skype.raider",
        // Line / WeChat / KakaoTalk
        "jp.naver.line.android", "com.tencent.mm", "com.kakao.talk",
        // Google Chat / Messenger
        "com.google.android.apps.dynamite", "com.facebook.orca",
        // BeReal / Kik / TextNow
        "com.bereal.ft", "kik.android", "com.textnow.android",
        // Android Messages (Samsung & Google)
        "com.google.android.apps.messaging", "com.samsung.android.messaging"
    )

    private fun isSocialApp(pkg: String) = pkg in setOf(
        // Instagram / Facebook / Threads
        "com.instagram.android", "com.facebook.katana", "com.instagram.barcelona",
        // X (Twitter)
        "com.twitter.android", "com.x.android",
        // Fediverse
        "org.joinmastodon.android", "xyz.blueskyweb.app",
        // Reddit / LinkedIn
        "com.reddit.frontpage", "com.linkedin.android",
        // Pinterest / Tumblr
        "com.pinterest", "com.tumblr",
        // YouTube / TikTok
        "com.google.android.youtube", "com.zhiliaoapp.musically",
        // Clubhouse / Twitch
        "io.clubhouse", "tv.twitch.android.app"
    )

    private fun isEmailApp(pkg: String) = pkg in setOf(
        // Google / Outlook / Samsung / Yahoo
        "com.google.android.gm", "com.microsoft.office.outlook",
        "com.samsung.android.email.provider", "com.yahoo.mobile.client.android.mail",
        // Privacy-focused
        "ch.protonmail.android", "de.tutao.tutanota",
        // Third-party clients
        "com.readdle.spark", "me.bluemail.mail", "com.ninefolders.hd",
        "org.kman.AquaMail", "com.fastmail.app",
        // HEY / Edison
        "com.basecamp.hey", "com.easilydo.mail"
    )

    private fun isSystemApp(pkg: String) =
        pkg.startsWith("com.android.") ||
        pkg.startsWith("com.samsung.android.") ||
        pkg.startsWith("com.google.android.") ||
        pkg.startsWith("com.oneplus.") ||
        pkg.startsWith("com.miui.") ||
        pkg.startsWith("com.huawei.") ||
        pkg.startsWith("com.oppo.") ||
        pkg.startsWith("com.realme.") ||
        pkg == "android"
}
