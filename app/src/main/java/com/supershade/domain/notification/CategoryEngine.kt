package com.supershade.domain.notification

import android.service.notification.StatusBarNotification
import com.supershade.domain.notification.model.ShadeCategory

class CategoryEngine {

    fun categorize(sbn: StatusBarNotification): ShadeCategory {
        val pkg = sbn.packageName
        val androidCategory = sbn.notification.category

        // Calls always win
        if (androidCategory == android.app.Notification.CATEGORY_CALL ||
            androidCategory == android.app.Notification.CATEGORY_MISSED_CALL) {
            return ShadeCategory.Calls
        }

        // Package heuristics run first — known apps always land in the right bucket
        if (isCallsApp(pkg))        return ShadeCategory.Calls
        if (isMessagingApp(pkg))    return ShadeCategory.Messages
        if (isSocialApp(pkg))       return ShadeCategory.Social
        if (isEmailApp(pkg))        return ShadeCategory.Email
        if (isProductivityApp(pkg)) return ShadeCategory.Productivity
        if (isMediaApp(pkg))        return ShadeCategory.Media

        // androidCategory mapping for everything else
        if (androidCategory != null) {
            ShadeCategory.entries.forEach { cat ->
                if (cat.androidCategory == androidCategory) return cat
            }
        }

        // Productivity by android category
        if (androidCategory == android.app.Notification.CATEGORY_REMINDER ||
            androidCategory == android.app.Notification.CATEGORY_EVENT) {
            return ShadeCategory.Productivity
        }

        // System apps last
        return if (isSystemApp(pkg)) ShadeCategory.System else ShadeCategory.Apps
    }

    private fun isCallsApp(pkg: String) = pkg in setOf(
        "com.google.android.dialer", "com.samsung.android.dialer",
        "com.android.dialer", "com.truecaller",
        "com.samsung.android.incallui"
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
        // Slack / Teams / Skype / Zoom
        "com.Slack", "com.microsoft.teams", "com.skype.raider", "us.zoom.videomeetings",
        // Line / WeChat / KakaoTalk / Zalo
        "jp.naver.line.android", "com.tencent.mm", "com.kakao.talk", "com.zing.zalo",
        // Google Chat / Messenger / Hangouts
        "com.google.android.apps.dynamite", "com.facebook.orca", "com.google.android.talk",
        // BeReal / Kik / TextNow / ICQ
        "com.bereal.ft", "kik.android", "com.textnow.android", "com.icq.mobile.client",
        // Android Messages (Samsung & Google)
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
        // iMessage (Beeper/Nothing)
        "com.beeper.android", "com.nothing.nothing.phone.messages",
        // Keybase / Briar / Session
        "io.keybase.famchat", "org.briarproject.briar.android", "network.loki.messenger"
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
        "io.clubhouse", "tv.twitch.android.app",
        // GitHub / Product Hunt
        "com.github.android", "com.producthunt.android"
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

    private fun isProductivityApp(pkg: String) = pkg in setOf(
        // Google suite
        "com.google.android.calendar", "com.google.android.keep",
        "com.google.android.apps.tasks", "com.google.android.apps.docs",
        "com.google.android.apps.drive", "com.google.android.apps.sheets",
        "com.google.android.apps.docs.editors.docs",
        // Samsung suite
        "com.samsung.android.calendar", "com.samsung.android.app.notes",
        "com.samsung.android.app.reminder", "com.samsung.android.bixby.agent",
        // Microsoft suite
        "com.microsoft.todos", "com.microsoft.planner", "com.microsoft.launcher.enterprise",
        "com.microsoft.office.word", "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint", "com.microsoft.office.onenote",
        // Notion / Todoist / TickTick / Any.do
        "notion.id", "com.todoist.android.Todoist",
        "com.ticktick.task", "com.anydo",
        // Trello / Asana / Monday
        "com.trello", "com.asana.app", "com.monday.monday",
        // Clock / Reminders
        "com.samsung.android.app.clockpackage", "com.google.android.deskclock",
        "com.android.deskclock",
        // Files / Downloads
        "com.android.providers.downloads.ui", "com.android.documentsui",
        "com.samsung.android.app.myfiles"
    )

    private fun isMediaApp(pkg: String) = pkg in setOf(
        // Spotify / Apple Music / Tidal / Deezer
        "com.spotify.music", "com.apple.android.music", "com.tidal.wave", "deezer.android.app",
        // YouTube Music / Google Play Music
        "com.google.android.apps.youtube.music", "com.google.android.music",
        // Samsung Music / Podcast
        "com.sec.android.app.music", "com.samsung.android.podcasts",
        // Podcast apps
        "au.com.shiftyjelly.pocketcasts", "com.google.android.apps.podcasts",
        "fm.castbox.audiobook.radio.podcast",
        // Video streaming
        "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus", "com.hbo.hbonow",
        "com.crunchyroll.crunchyroid", "com.hulu.livingroomlauncher",
        // Local media
        "com.google.android.videos", "org.videolan.vlc", "com.mxtech.videoplayer.ad",
        // Radio
        "com.iheartradio.android", "com.audible.application"
    )

    private fun isSystemApp(pkg: String): Boolean {
        // Definitely system
        if (pkg == "android" ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.providers.") ||
            pkg.startsWith("com.google.android.gms") ||
            pkg.startsWith("com.google.android.gsf") ||
            pkg == "com.android.phone" ||
            pkg == "com.android.server.telecom") {
            return true
        }

        // Samsung consumer-facing apps that should NOT be System
        val samsungUserApps = setOf(
            "com.samsung.android.app.watchmanager",
            "com.samsung.android.galaxywearable",
            "com.samsung.android.app.galaxyfinder",
            "com.samsung.android.themestore",
            "com.samsung.android.app.tips",
            "com.samsung.android.app.updatecenter",
            "com.samsung.android.kidsinstaller",
            "com.samsung.android.health",
            "com.samsung.android.samsungpay",
            "com.samsung.android.arzone",
            "com.samsung.android.app.routines",
            "com.samsung.android.bixby.wakeup",
            "com.samsung.android.goodlock",
            "com.samsung.android.honeyboard",
            "com.samsung.android.game.gamehome",
        )
        if (pkg in samsungUserApps) return false

        // Preserve clearly non-system Google apps in Apps
        val nonSystemSubstrings = listOf(
            "apps.maps", "photos", "keep", "drive", "notes", "chrome",
            "youtube", "calendar", "tasks", "translate", "lens", "pay",
            "fit", "home", "news"
        )
        if (nonSystemSubstrings.any { pkg.contains(it) }) return false

        return pkg.startsWith("com.android.") ||
            pkg.startsWith("com.samsung.android.") ||
            pkg.startsWith("com.google.android.") ||
            pkg.startsWith("com.oneplus.") ||
            pkg.startsWith("com.miui.") ||
            pkg.startsWith("com.huawei.") ||
            pkg.startsWith("com.oppo.") ||
            pkg.startsWith("com.realme.")
    }
}
