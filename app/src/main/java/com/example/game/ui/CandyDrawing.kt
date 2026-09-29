package com.example.game.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.game.model.CandyTile
import com.example.game.model.CandyType
import com.example.game.model.SpecialType

@Composable
fun CandyView(
    tile: CandyTile,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "candy_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val hintBounce by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_bounce"
    )

    val stripeShimmer by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stripe_shimmer"
    )

    val colorBombRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bomb_rot"
    )

    Box(modifier = modifier.fillMaxSize().padding(2.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f + (if (tile.isHinted) hintBounce else 0f))

            if (tile.special == SpecialType.COLOR_BOMB) {
                drawColorBomb(center, canvasWidth * 0.42f, colorBombRotation)
                return@Canvas
            }

            val scale = when {
                tile.isSelected -> pulseScale
                tile.isHinted -> 1.05f
                else -> 1f
            }
            val baseRadius = (canvasWidth.coerceAtMost(canvasHeight) / 2.25f) * scale

            // Ambient drop shadow for high depth
            drawCircle(
                color = Color(0x33000000),
                radius = baseRadius * 0.9f,
                center = center + Offset(0f, baseRadius * 0.18f)
            )

            // Calculate precise candy shape boundary for flawless clipping
            val candyPath = getCandyPath(tile.type, center, baseRadius)

            // Draw base candy shape with 3D richness
            when (tile.type) {
                CandyType.RED -> drawJuicyHeart(center, baseRadius, tile.type)
                CandyType.ORANGE -> drawJuicyLozenge(center, baseRadius, tile.type)
                CandyType.YELLOW -> drawJuicyStar(center, baseRadius, tile.type)
                CandyType.GREEN -> drawJuicySquare(center, baseRadius, tile.type)
                CandyType.BLUE -> drawJuicySphere(center, baseRadius, tile.type)
                CandyType.PURPLE -> drawJuicyTriangle(center, baseRadius, tile.type)
            }

            // Draw special candy confectionery overlays - 100% clipped inside candy shape
            when (tile.special) {
                SpecialType.HORIZONTAL_STRIPED -> {
                    clipPath(candyPath) {
                        drawHorizontalCandyStripes(center, baseRadius, stripeShimmer)
                    }
                    // Re-draw outer rim gloss to blend seamlessly
                    drawPath(
                        path = candyPath,
                        color = Color.White.copy(alpha = 0.4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                SpecialType.VERTICAL_STRIPED -> {
                    clipPath(candyPath) {
                        drawVerticalCandyStripes(center, baseRadius, stripeShimmer)
                    }
                    // Re-draw outer rim gloss to blend seamlessly
                    drawPath(
                        path = candyPath,
                        color = Color.White.copy(alpha = 0.4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                SpecialType.WRAPPED -> {
                    drawWrappedCandyFoil(center, baseRadius, candyPath)
                }
                else -> {}
            }

            // Selection / Hint Halo
            if (tile.isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = baseRadius * 1.15f,
                    style = Stroke(width = 3.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0x55FFFFFF),
                    radius = baseRadius * 1.25f,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else if (tile.isHinted) {
                drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = baseRadius * 1.18f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

fun getCandyPath(type: CandyType, center: Offset, radius: Float): Path {
    return when (type) {
        CandyType.RED -> {
            val r = radius * 0.92f
            Path().apply {
                moveTo(center.x, center.y + r * 0.85f)
                cubicTo(
                    center.x - r * 1.45f, center.y,
                    center.x - r * 1.25f, center.y - r * 1.15f,
                    center.x, center.y - r * 0.38f
                )
                cubicTo(
                    center.x + r * 1.25f, center.y - r * 1.15f,
                    center.x + r * 1.45f, center.y,
                    center.x, center.y + r * 0.85f
                )
                close()
            }
        }
        CandyType.BLUE -> {
            Path().apply {
                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }
        }
        CandyType.GREEN -> {
            val sizePx = radius * 1.75f
            val topLeft = Offset(center.x - sizePx / 2f, center.y - sizePx / 2f)
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(topLeft, Size(sizePx, sizePx)),
                        cornerRadius = CornerRadius(radius * 0.4f, radius * 0.4f)
                    )
                )
            }
        }
        CandyType.ORANGE -> {
            val width = radius * 2.2f
            val height = radius * 1.35f
            val topLeft = Offset(center.x - width / 2f, center.y - height / 2f)
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(topLeft, Size(width, height)),
                        cornerRadius = CornerRadius(height / 2f, height / 2f)
                    )
                )
            }
        }
        CandyType.YELLOW -> {
            val path = Path()
            val points = 5
            val outerR = radius * 1.15f
            val innerR = radius * 0.56f
            for (i in 0 until points * 2) {
                val angle = (i * Math.PI / points) - (Math.PI / 2)
                val currentRadius = if (i % 2 == 0) outerR else innerR
                val x = (center.x + Math.cos(angle) * currentRadius).toFloat()
                val y = (center.y + Math.sin(angle) * currentRadius).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            path
        }
        CandyType.PURPLE -> {
            val r = radius * 1.15f
            Path().apply {
                moveTo(center.x, center.y - r * 0.95f)
                lineTo(center.x + r * 0.92f, center.y + r * 0.78f)
                lineTo(center.x - r * 0.92f, center.y + r * 0.78f)
                close()
            }
        }
    }
}

private fun DrawScope.drawJuicyHeart(center: Offset, radius: Float, type: CandyType) {
    val r = radius * 0.92f
    val path = Path().apply {
        moveTo(center.x, center.y + r * 0.85f)
        cubicTo(
            center.x - r * 1.45f, center.y,
            center.x - r * 1.25f, center.y - r * 1.15f,
            center.x, center.y - r * 0.38f
        )
        cubicTo(
            center.x + r * 1.25f, center.y - r * 1.15f,
            center.x + r * 1.45f, center.y,
            center.x, center.y + r * 0.85f
        )
        close()
    }

    // Rich 3D gradient fill
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF8DA1), type.mainColor, type.darkColor, Color(0xFF7F1D1D)),
            center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
            radius = r * 1.4f
        )
    )

    // Inner rim glow
    drawPath(
        path = path,
        color = Color(0x33FFFFFF),
        style = Stroke(width = 2.dp.toPx())
    )

    // Sweet primary gloss arc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(center.x - r * 0.38f, center.y - r * 0.42f),
            radius = r * 0.35f
        ),
        radius = r * 0.32f,
        center = Offset(center.x - r * 0.38f, center.y - r * 0.42f)
    )

    // Sparkling specular point
    drawCircle(
        color = Color.White,
        radius = r * 0.12f,
        center = Offset(center.x - r * 0.42f, center.y - r * 0.45f)
    )
}

private fun DrawScope.drawJuicySphere(center: Offset, radius: Float, type: CandyType) {
    // 3D sphere gradient with deep sapphire shadow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFBAE6FD), type.mainColor, type.darkColor, Color(0xFF0C4A6E)),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    // Swirling candy marble detail
    drawCircle(
        color = Color(0x22FFFFFF),
        radius = radius * 0.72f,
        center = Offset(center.x + radius * 0.1f, center.y + radius * 0.1f),
        style = Stroke(width = 3.dp.toPx())
    )

    // Crescent gloss highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.35f),
            radius = radius * 0.45f
        ),
        radius = radius * 0.38f,
        center = Offset(center.x - radius * 0.32f, center.y - radius * 0.35f)
    )

    // Glint dot
    drawCircle(
        color = Color.White,
        radius = radius * 0.11f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.38f)
    )
}

private fun DrawScope.drawJuicySquare(center: Offset, radius: Float, type: CandyType) {
    val sizePx = radius * 1.75f
    val topLeft = Offset(center.x - sizePx / 2f, center.y - sizePx / 2f)

    // Rounded gummy cube with 3D gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF86EFAC), type.mainColor, type.darkColor, Color(0xFF064E3B)),
            start = topLeft,
            end = Offset(topLeft.x + sizePx, topLeft.y + sizePx)
        ),
        topLeft = topLeft,
        size = Size(sizePx, sizePx),
        cornerRadius = CornerRadius(radius * 0.4f, radius * 0.4f)
    )

    // Frosted inner bevel
    val inset = 3.5.dp.toPx()
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
            start = topLeft,
            end = center
        ),
        topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
        size = Size(sizePx - inset * 2, sizePx - inset * 2),
        cornerRadius = CornerRadius(radius * 0.3f, radius * 0.3f)
    )

    // Corner specular gleam
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = radius * 0.16f,
        center = Offset(topLeft.x + radius * 0.45f, topLeft.y + radius * 0.45f)
    )
}

private fun DrawScope.drawJuicyLozenge(center: Offset, radius: Float, type: CandyType) {
    val width = radius * 2.2f
    val height = radius * 1.35f
    val topLeft = Offset(center.x - width / 2f, center.y - height / 2f)

    // Pill shaped rounded lozenge
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFED7AA), type.mainColor, type.darkColor, Color(0xFF7C2D12)),
            center = Offset(center.x, center.y - height * 0.25f),
            radius = width * 0.75f
        ),
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f)
    )

    // Horizontal glossy reflection bar
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.1f)),
            start = Offset(topLeft.x + width * 0.15f, topLeft.y + height * 0.2f),
            end = Offset(topLeft.x + width * 0.85f, topLeft.y + height * 0.2f)
        ),
        topLeft = Offset(topLeft.x + width * 0.18f, topLeft.y + height * 0.2f),
        size = Size(width * 0.64f, height * 0.24f),
        cornerRadius = CornerRadius(height * 0.12f, height * 0.12f)
    )
}

private fun DrawScope.drawJuicyStar(center: Offset, radius: Float, type: CandyType) {
    val path = Path()
    val points = 5
    val outerR = radius * 1.15f
    val innerR = radius * 0.56f

    for (i in 0 until points * 2) {
        val angle = (i * Math.PI / points) - (Math.PI / 2)
        val currentRadius = if (i % 2 == 0) outerR else innerR
        val x = (center.x + Math.cos(angle) * currentRadius).toFloat()
        val y = (center.y + Math.sin(angle) * currentRadius).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    // Vibrant golden gradient
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF08A), type.mainColor, type.darkColor, Color(0xFF713F12)),
            center = center - Offset(radius * 0.2f, radius * 0.2f),
            radius = outerR * 1.1f
        )
    )

    // Crystal center spark
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = innerR * 0.45f,
        center = center - Offset(radius * 0.15f, radius * 0.15f)
    )
    drawCircle(
        color = Color.White,
        radius = innerR * 0.18f,
        center = center - Offset(radius * 0.18f, radius * 0.18f)
    )
}

private fun DrawScope.drawJuicyTriangle(center: Offset, radius: Float, type: CandyType) {
    val r = radius * 1.15f
    val path = Path().apply {
        moveTo(center.x, center.y - r * 0.95f)
        lineTo(center.x + r * 0.92f, center.y + r * 0.78f)
        lineTo(center.x - r * 0.92f, center.y + r * 0.78f)
        close()
    }

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE9D5FF), type.mainColor, type.darkColor, Color(0xFF581C87)),
            center = Offset(center.x, center.y - r * 0.2f),
            radius = r * 1.25f
        )
    )

    // Crystal facet edge
    drawLine(
        color = Color.White.copy(alpha = 0.75f),
        start = Offset(center.x, center.y - r * 0.75f),
        end = Offset(center.x - r * 0.65f, center.y + r * 0.55f),
        strokeWidth = 3.dp.toPx()
    )

    drawCircle(
        color = Color.White,
        radius = r * 0.14f,
        center = Offset(center.x - r * 0.18f, center.y - r * 0.25f)
    )
}

/**
 * Clean, perfectly clipped confectionery horizontal stripes - zero overflow, zero loose floating lines
 */
private fun DrawScope.drawHorizontalCandyStripes(center: Offset, radius: Float, shimmer: Float) {
    val stripeThickness = radius * 0.24f
    val positions = listOf(-0.54f, -0.18f, 0.18f, 0.54f)
    positions.forEach { relY ->
        val y = center.y + relY * radius

        // Soft confectionery under-shadow for 3D depth
        drawLine(
            color = Color(0x28000000),
            start = Offset(center.x - radius * 1.6f, y + stripeThickness * 0.5f),
            end = Offset(center.x + radius * 1.6f, y + stripeThickness * 0.5f),
            strokeWidth = 2.dp.toPx()
        )

        // Luscious white sugar band
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f * shimmer),
                    Color.White.copy(alpha = 0.84f * shimmer),
                    Color.White.copy(alpha = 0.96f * shimmer)
                ),
                startY = y - stripeThickness / 2f,
                endY = y + stripeThickness / 2f
            ),
            topLeft = Offset(center.x - radius * 1.6f, y - stripeThickness / 2f),
            size = Size(radius * 3.2f, stripeThickness)
        )

        // Glossy central reflection thread
        drawLine(
            color = Color.White.copy(alpha = 0.90f * shimmer),
            start = Offset(center.x - radius * 1.6f, y - stripeThickness * 0.15f),
            end = Offset(center.x + radius * 1.6f, y - stripeThickness * 0.15f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

/**
 * Clean, perfectly clipped confectionery vertical stripes - zero overflow, zero loose floating lines
 */
private fun DrawScope.drawVerticalCandyStripes(center: Offset, radius: Float, shimmer: Float) {
    val stripeThickness = radius * 0.24f
    val positions = listOf(-0.54f, -0.18f, 0.18f, 0.54f)
    positions.forEach { relX ->
        val x = center.x + relX * radius

        // Soft confectionery under-shadow for 3D depth
        drawLine(
            color = Color(0x28000000),
            start = Offset(x + stripeThickness * 0.5f, center.y - radius * 1.6f),
            end = Offset(x + stripeThickness * 0.5f, center.y + radius * 1.6f),
            strokeWidth = 2.dp.toPx()
        )

        // Luscious white sugar band
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f * shimmer),
                    Color.White.copy(alpha = 0.84f * shimmer),
                    Color.White.copy(alpha = 0.96f * shimmer)
                ),
                startX = x - stripeThickness / 2f,
                endX = x + stripeThickness / 2f
            ),
            topLeft = Offset(x - stripeThickness / 2f, center.y - radius * 1.6f),
            size = Size(stripeThickness, radius * 3.2f)
        )

        // Glossy central reflection thread
        drawLine(
            color = Color.White.copy(alpha = 0.90f * shimmer),
            start = Offset(x - stripeThickness * 0.15f, center.y - radius * 1.6f),
            end = Offset(x - stripeThickness * 0.15f, center.y + radius * 1.6f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

/**
 * Authentic bonbon wrapped confectionery wrapper with crimped fan ties & crystalline foil sheen
 */
private fun DrawScope.drawWrappedCandyFoil(center: Offset, radius: Float, candyPath: Path) {
    // Two crimped triangular bonbon wrapper fan ties at opposite sides
    val tieSpan = radius * 0.58f
    val leftTie = Path().apply {
        moveTo(center.x - radius * 0.85f, center.y)
        lineTo(center.x - radius * 1.32f, center.y - tieSpan * 0.65f)
        lineTo(center.x - radius * 1.20f, center.y)
        lineTo(center.x - radius * 1.32f, center.y + tieSpan * 0.65f)
        close()
    }
    val rightTie = Path().apply {
        moveTo(center.x + radius * 0.85f, center.y)
        lineTo(center.x + radius * 1.32f, center.y - tieSpan * 0.65f)
        lineTo(center.x + radius * 1.20f, center.y)
        lineTo(center.x + radius * 1.32f, center.y + tieSpan * 0.65f)
        close()
    }

    // Outer wrapper fan ties with frosted cellophane shading
    drawPath(
        path = leftTie,
        brush = Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.35f))
        )
    )
    drawPath(
        path = rightTie,
        brush = Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.85f))
        )
    )
    drawPath(leftTie, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 1.5.dp.toPx()))
    drawPath(rightTie, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 1.5.dp.toPx()))

    // Glossy wrapper outline hugging the exact candy contour
    drawPath(
        path = candyPath,
        color = Color.White.copy(alpha = 0.75f),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Sweet diagonal cellophane sheen band
    clipPath(candyPath) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.42f),
                    Color.Transparent
                ),
                start = Offset(center.x - radius * 1.2f, center.y - radius * 1.2f),
                end = Offset(center.x + radius * 1.2f, center.y + radius * 1.2f)
            ),
            topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 1.5f),
            size = Size(radius * 3f, radius * 3f)
        )
    }

    // Sparkle gleam point
    drawCircle(
        color = Color.White,
        radius = radius * 0.14f,
        center = center + Offset(radius * 0.2f, -radius * 0.2f)
    )
}

private fun DrawScope.drawColorBomb(center: Offset, radius: Float, rotation: Float) {
    // Outer shimmering chocolate aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = center
    )

    // Decadent cocoa sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF1A0A03)),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )

    // Vibrant rainbow sugar sprinkles orbit
    val sprinkleColors = listOf(
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFFEAB308),
        Color(0xFF22C55E),
        Color(0xFF06B6D4),
        Color(0xFFA855F7),
        Color(0xFFEC4899)
    )

    val sprinkleCount = 12
    for (i in 0 until sprinkleCount) {
        val angle = Math.toRadians((i * (360f / sprinkleCount) + rotation).toDouble())
        val dist = radius * (0.45f + (i % 3) * 0.18f)
        val sx = (center.x + Math.cos(angle) * dist).toFloat()
        val sy = (center.y + Math.sin(angle) * dist).toFloat()
        val color = sprinkleColors[i % sprinkleColors.size]

        drawCircle(
            color = color,
            radius = radius * 0.13f,
            center = Offset(sx, sy)
        )
        // Specular glint on sprinkle
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = radius * 0.05f,
            center = Offset(sx - radius * 0.03f, sy - radius * 0.03f)
        )
    }

    // Central glistening sugar glint
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 0.45f
        ),
        radius = radius * 0.45f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
    )
}
