package com.supershade.domain.notification

/**
 * Intelligent OTP / 2FA verification code and VIP alert detector.
 * Inspired by Samsung Good Lock NotiStar, jd1378/otphelper, and modern Android 15/16 SystemUI.
 *
 * Extracts one-time verification passcodes from incoming notification contents
 * to render immediate "Copy code: 123456" action chips.
 */
object OtpDetector {

    // Common multilingual trigger keywords for OTPs and verification codes
    private val OTP_TRIGGER_REGEX = Regex(
        "(?i)\\b(otp|code|c[oó]digo|passcode|secret|one-time|verification|v[eé]rification|auth|authenticat(or|ion)|pin|tan|2fa|security code|login code|confirmation code)\\b"
    )

    // Prefixed codes like "G-123456", "FB-12345", "VK-123456"
    private val PREFIXED_CODE_REGEX = Regex(
        "(?i)\\b([A-Z]{1,4}-\\d{4,8})\\b"
    )

    // Hyphenated numeric codes like "123-456", "1234-5678"
    private val HYPHENATED_CODE_REGEX = Regex(
        "(?<!\\d)(\\d{3,4}[-\\s]\\d{3,4})(?!\\d)"
    )

    // Standard 4 to 8 digit standalone numbers
    private val STANDALONE_NUMERIC_REGEX = Regex(
        "(?<!\\d)(\\d{4,8})(?!\\d)"
    )

    // VIP urgency and security keywords
    private val VIP_TRIGGER_REGEX = Regex(
        "(?i)\\b(urgent|critical|security alert|fraud|unauthorized|emergency|breach|immediate action|danger|unrecognized login)\\b"
    )

    /**
     * Extracts an OTP / verification code if present in the notification content.
     */
    fun extractOtp(title: String, text: String, subText: String? = null): String? {
        val fullContent = buildString {
            if (title.isNotBlank()) append(title).append("\n")
            if (text.isNotBlank()) append(text).append("\n")
            if (!subText.isNullOrBlank()) append(subText)
        }

        if (fullContent.isBlank()) return null
        if (!OTP_TRIGGER_REGEX.containsMatchIn(fullContent)) return null

        // 1. Try prefixed codes (e.g. Google G-123456, Facebook FB-12345)
        PREFIXED_CODE_REGEX.find(fullContent)?.let { match ->
            return match.value.trim()
        }

        // 2. Try hyphenated codes (e.g. 123-456, 452 891)
        HYPHENATED_CODE_REGEX.find(fullContent)?.let { match ->
            val candidate = match.value.replace(" ", "").trim()
            if (candidate.contains("-") || candidate.length in 6..8) {
                return candidate
            }
        }

        // 3. Try standard 4-8 digit codes, prioritizing those near trigger keywords
        val matches = STANDALONE_NUMERIC_REGEX.findAll(fullContent).toList()
        for (match in matches) {
            val code = match.value
            // Avoid capturing common years (1950..2050) unless explicitly preceded by an OTP keyword
            if (code.length == 4) {
                val num = code.toIntOrNull() ?: 0
                if (num in 1950..2050 && !isNearKeyword(fullContent, match.range.first)) {
                    continue
                }
            }
            return code
        }

        return null
    }

    /**
     * Returns true if the notification indicates an urgent security or VIP alert.
     */
    fun isVipAlert(title: String, text: String, subText: String? = null): Boolean {
        val fullContent = "$title $text ${subText.orEmpty()}"
        return VIP_TRIGGER_REGEX.containsMatchIn(fullContent)
    }

    private fun isNearKeyword(content: String, index: Int): Boolean {
        val start = maxOf(0, index - 35)
        val end = minOf(content.length, index + 35)
        val window = content.substring(start, end)
        return OTP_TRIGGER_REGEX.containsMatchIn(window)
    }
}
