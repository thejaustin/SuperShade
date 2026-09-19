package com.supershade.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.supershade.settings.CardBorderWidth

/**
 * Ambient CompositionLocal providing the current [CardBorderWidth] configured by the user.
 */
val LocalCardBorderWidth = staticCompositionLocalOf { CardBorderWidth.THIN }

/**
 * Returns a standardized [BorderStroke] matching the user's preferred [CardBorderWidth].
 * If [borderWidth] is [CardBorderWidth.NONE], returns null.
 */
@Composable
fun getCardBorder(
    borderWidth: CardBorderWidth = LocalCardBorderWidth.current,
    borderColor: Color? = null,
    alpha: Float = 0.35f,
): BorderStroke? {
    if (borderWidth == CardBorderWidth.NONE || borderWidth.widthDp <= 0f) return null
    val strokeColor = borderColor ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
    return BorderStroke(borderWidth.widthDp.dp, strokeColor)
}
