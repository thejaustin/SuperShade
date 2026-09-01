package com.supershade.domain.update

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val releasePageUrl: String,
) {
    val isUpdateAvailable: Boolean
        get() = parseVersion(latestVersion) > parseVersion(currentVersion)
}

private fun parseVersion(v: String): Triple<Int, Int, Int> {
    val clean = v.trimStart('v')
    val parts = clean.split(".").map { it.toIntOrNull() ?: 0 }
    return Triple(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
}

private operator fun Triple<Int, Int, Int>.compareTo(other: Triple<Int, Int, Int>): Int {
    if (first != other.first) return first.compareTo(other.first)
    if (second != other.second) return second.compareTo(other.second)
    return third.compareTo(other.third)
}
