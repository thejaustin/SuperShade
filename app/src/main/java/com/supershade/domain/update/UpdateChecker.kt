package com.supershade.domain.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val RELEASES_API =
            "https://api.github.com/repos/thejaustin/SuperShade/releases/latest"
    }

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "SuperShade/$currentVersion")
            connectTimeout = 10_000
            readTimeout    = 10_000
        }
        try {
            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.use { it.bufferedReader().readText() }
            val root = json.parseToJsonElement(body).jsonObject

            val tagName = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
            val releaseNotes = root["body"]?.jsonPrimitive?.content ?: ""
            val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: ""

            val apkUrl = root["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?.get("browser_download_url")?.jsonPrimitive?.content

            UpdateInfo(
                latestVersion = tagName,
                currentVersion = currentVersion,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                releasePageUrl = htmlUrl,
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
