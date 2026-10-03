package com.supershade.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * High-tech chamfered corner shape with 45-degree cuts at specified corners.
 * Asymmetric chamfers (e.g. cut Top-Right and Bottom-Left) create iconic military/cyberpunk HUD panels.
 */
class ChamferedCornerShape(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
) : Shape {

    constructor(all: Dp = 0.dp) : this(all, all, all, all)

    companion object {
        fun diagonal(cut: Dp) = ChamferedCornerShape(
            topStart = 0.dp,
            topEnd = cut,
            bottomEnd = 0.dp,
            bottomStart = cut,
        )
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val ts = with(density) { topStart.toPx() }.coerceAtMost(size.minDimension / 2f)
        val te = with(density) { topEnd.toPx() }.coerceAtMost(size.minDimension / 2f)
        val be = with(density) { bottomEnd.toPx() }.coerceAtMost(size.minDimension / 2f)
        val bs = with(density) { bottomStart.toPx() }.coerceAtMost(size.minDimension / 2f)

        val path = Path().apply {
            moveTo(ts, 0f)
            lineTo(size.width - te, 0f)
            lineTo(size.width, te)
            lineTo(size.width, size.height - be)
            lineTo(size.width - be, size.height)
            lineTo(bs, size.height)
            lineTo(0f, size.height - bs)
            lineTo(0f, ts)
            close()
        }
        return Outline.Generic(path)
    }
}
