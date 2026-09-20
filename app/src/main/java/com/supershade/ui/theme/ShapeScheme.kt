package com.supershade.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.supershade.settings.TileShape

/**
 * Cohesive global shape tokens derived from the selected [TileShape].
 * Automatically shapes tiles, cards, container islands, sliders, chips, and pills across SuperShade.
 */
data class ShadeShapeScheme(
    val tile: Shape,
    val card: Shape,
    val container: Shape,
    val slider: Shape,
    val chip: Shape,
    val pill: Shape,
    val tilePaddingHorizontal: Dp = 8.dp,
    val tilePaddingVertical: Dp = 8.dp,
    val tileIconSizeModifier: Dp = 0.dp,
) {
    companion object {
        fun fromTileShape(tileShape: TileShape): ShadeShapeScheme = when (tileShape) {
            TileShape.SQUIRCLE -> ShadeShapeScheme(
                tile = RoundedCornerShape(22.dp),
                card = RoundedCornerShape(24.dp),
                container = RoundedCornerShape(26.dp),
                slider = RoundedCornerShape(22.dp),
                chip = RoundedCornerShape(14.dp),
                pill = RoundedCornerShape(50),
                tilePaddingHorizontal = 8.dp,
                tilePaddingVertical = 8.dp,
            )
            TileShape.ROUNDED -> ShadeShapeScheme(
                tile = RoundedCornerShape(16.dp),
                card = RoundedCornerShape(18.dp),
                container = RoundedCornerShape(20.dp),
                slider = RoundedCornerShape(16.dp),
                chip = RoundedCornerShape(12.dp),
                pill = RoundedCornerShape(50),
                tilePaddingHorizontal = 8.dp,
                tilePaddingVertical = 8.dp,
            )
            TileShape.CIRCLE -> ShadeShapeScheme(
                tile = CircleShape,
                card = RoundedCornerShape(30.dp),
                container = RoundedCornerShape(32.dp),
                slider = CircleShape,
                chip = CircleShape,
                pill = CircleShape,
                tilePaddingHorizontal = 10.dp,
                tilePaddingVertical = 6.dp, // Protect text from circular arc clipping
            )
            TileShape.PILL -> ShadeShapeScheme(
                tile = RoundedCornerShape(28.dp),
                card = RoundedCornerShape(26.dp),
                container = RoundedCornerShape(28.dp),
                slider = RoundedCornerShape(28.dp),
                chip = RoundedCornerShape(50),
                pill = RoundedCornerShape(50),
                tilePaddingHorizontal = 9.dp,
                tilePaddingVertical = 8.dp,
            )
            TileShape.SOFT -> ShadeShapeScheme(
                tile = RoundedCornerShape(12.dp),
                card = RoundedCornerShape(14.dp),
                container = RoundedCornerShape(16.dp),
                slider = RoundedCornerShape(12.dp),
                chip = RoundedCornerShape(10.dp),
                pill = RoundedCornerShape(18.dp),
                tilePaddingHorizontal = 8.dp,
                tilePaddingVertical = 8.dp,
            )
            TileShape.LEAF -> ShadeShapeScheme(
                tile = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp),
                card = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 10.dp, bottomStart = 10.dp),
                container = RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 12.dp, bottomStart = 12.dp),
                slider = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 8.dp, bottomStart = 8.dp),
                chip = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 6.dp, bottomStart = 6.dp),
                pill = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 8.dp, bottomStart = 8.dp),
                tilePaddingHorizontal = 8.dp,
                tilePaddingVertical = 7.dp,
            )
            TileShape.SHARP -> ShadeShapeScheme(
                tile = RoundedCornerShape(6.dp),
                card = RoundedCornerShape(8.dp),
                container = RoundedCornerShape(10.dp),
                slider = RoundedCornerShape(6.dp),
                chip = RoundedCornerShape(4.dp),
                pill = RoundedCornerShape(8.dp),
                tilePaddingHorizontal = 9.dp,
                tilePaddingVertical = 8.dp,
            )
        }
    }
}

val LocalShadeShapeScheme = staticCompositionLocalOf {
    ShadeShapeScheme.fromTileShape(TileShape.SQUIRCLE)
}
