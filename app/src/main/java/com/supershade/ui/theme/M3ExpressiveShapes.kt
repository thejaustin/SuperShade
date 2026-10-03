package com.supershade.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star

/**
 * Android 15/16 Material 3 Expressive (M3E) Shape Library.
 * Provides Google's iconic geometric polygons and real-time bezier curve morphing.
 */
object M3ExpressiveShapes {

    // Base Polygons (normalized coordinate space)
    val Circle: RoundedPolygon by lazy {
        RoundedPolygon.circle(numVertices = 16)
    }

    val Squircle: RoundedPolygon by lazy {
        RoundedPolygon.rectangle(
            width = 2f,
            height = 2f,
            rounding = CornerRounding(radius = 0.52f, smoothing = 0.65f),
        )
    }

    val Clover4: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 4,
            radius = 1f,
            innerRadius = 0.65f,
            rounding = CornerRounding(radius = 0.38f, smoothing = 0.55f),
            innerRounding = CornerRounding(radius = 0.15f, smoothing = 0.30f),
        )
    }

    val SoftBurst8: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.82f,
            rounding = CornerRounding(radius = 0.22f, smoothing = 0.50f),
            innerRounding = CornerRounding(radius = 0.18f, smoothing = 0.40f),
        )
    }

    val Cookie12: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = 1f,
            innerRadius = 0.88f,
            rounding = CornerRounding(radius = 0.14f, smoothing = 0.45f),
            innerRounding = CornerRounding(radius = 0.12f, smoothing = 0.35f),
        )
    }

    val Sunny: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.70f,
            rounding = CornerRounding(radius = 0.28f, smoothing = 0.60f),
        )
    }

    // Cached Pre-matched Morphs for Fluid Animation
    val MorphCircleToBurst: Morph by lazy {
        Morph(Circle, SoftBurst8)
    }

    val MorphCircleToClover: Morph by lazy {
        Morph(Circle, Clover4)
    }

    val MorphCircleToSquircle: Morph by lazy {
        Morph(Circle, Squircle)
    }

    val MorphCircleToCookie: Morph by lazy {
        Morph(Circle, Cookie12)
    }

    val MorphCircleToSunny: Morph by lazy {
        Morph(Circle, Sunny)
    }
}

/**
 * Converts a [Morph] at [progress] (0f..1f) into a Compose [Path].
 */
fun Morph.toComposePath(progress: Float, targetPath: Path = Path()): Path {
    targetPath.reset()
    var isFirst = true
    forEachCubic(progress) { cubic ->
        if (isFirst) {
            targetPath.moveTo(cubic.anchor0X, cubic.anchor0Y)
            isFirst = false
        }
        targetPath.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    targetPath.close()
    return targetPath
}

/**
 * Converts a [RoundedPolygon] into a Compose [Path].
 */
fun RoundedPolygon.toComposePath(targetPath: Path = Path()): Path {
    targetPath.reset()
    val cubics = cubics
    if (cubics.isNotEmpty()) {
        targetPath.moveTo(cubics[0].anchor0X, cubics[0].anchor0Y)
        for (i in cubics.indices) {
            val c = cubics[i]
            targetPath.cubicTo(
                c.control0X, c.control0Y,
                c.control1X, c.control1Y,
                c.anchor1X, c.anchor1Y,
            )
        }
        targetPath.close()
    }
    return targetPath
}

/**
 * High-performance Compose [Shape] that transforms a [Morph] into an [Outline.Generic]
 * scaled and centered to fit within the component's render bounds.
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotationAngle: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        morph.toComposePath(progress, path)

        val bounds = path.getBounds()
        val matrix = Matrix()

        val scaleX = if (bounds.width > 0f) size.width / bounds.width else 1f
        val scaleY = if (bounds.height > 0f) size.height / bounds.height else 1f

        matrix.translate(-bounds.left, -bounds.top)
        matrix.scale(scaleX, scaleY)

        if (rotationAngle != 0f) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            matrix.translate(cx, cy)
            matrix.rotateZ(rotationAngle)
            matrix.translate(-cx, -cy)
        }

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * Remembers a [MorphShape] updating in real-time as [progress] animates.
 */
@Composable
fun rememberMorphShape(
    morph: Morph,
    progress: Float,
    rotationAngle: Float = 0f,
): Shape {
    return remember(morph, progress, rotationAngle) {
        MorphShape(morph, progress, rotationAngle)
    }
}

/**
 * High-performance static Compose [Shape] that draws a [RoundedPolygon] as an [Outline.Generic].
 */
class PolygonShape(
    private val polygon: RoundedPolygon,
    private val rotationAngle: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        polygon.toComposePath(path)

        val bounds = path.getBounds()
        val matrix = Matrix()

        val scaleX = if (bounds.width > 0f) size.width / bounds.width else 1f
        val scaleY = if (bounds.height > 0f) size.height / bounds.height else 1f

        matrix.translate(-bounds.left, -bounds.top)
        matrix.scale(scaleX, scaleY)

        if (rotationAngle != 0f) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            matrix.translate(cx, cy)
            matrix.rotateZ(rotationAngle)
            matrix.translate(-cx, -cy)
        }

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

fun RoundedPolygon.toComposeShape(rotationAngle: Float = 0f): Shape = PolygonShape(this, rotationAngle)

