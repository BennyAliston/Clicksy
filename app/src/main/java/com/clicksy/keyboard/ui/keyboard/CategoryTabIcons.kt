package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium Neubrutalist Vector Icons for Emoji Category Section Tabs.
 */
@Composable
fun CustomCategoryTabIcon(
    categoryName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val outlineColor = Color.Black
    val yellow = Color(0xFFFFDE4D)
    val red = Color(0xFFFF4D4D)
    val cyan = Color(0xFF00E5FF)
    val orange = Color(0xFFFF9800)
    val gold = Color(0xFFFFD700)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokePx = w * 0.09f

        when (categoryName) {
            "Recent" -> { // Clock icon with ticks & pivot
                drawCircle(yellow, radius = w * 0.42f, center = Offset(w / 2, h / 2))
                drawCircle(outlineColor, radius = w * 0.42f, center = Offset(w / 2, h / 2), style = Stroke(strokePx))
                // Clock ticks at 12, 3, 6, 9
                drawLine(outlineColor, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.22f), strokePx * 0.9f)
                drawLine(outlineColor, Offset(w * 0.85f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), strokePx * 0.9f)
                drawLine(outlineColor, Offset(w * 0.5f, h * 0.85f), Offset(w * 0.5f, h * 0.78f), strokePx * 0.9f)
                drawLine(outlineColor, Offset(w * 0.15f, h * 0.5f), Offset(w * 0.22f, h * 0.5f), strokePx * 0.9f)
                // Hands
                val hands = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    lineTo(w * 0.50f, h * 0.50f)
                    lineTo(w * 0.70f, h * 0.50f)
                }
                drawPath(hands, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(red, radius = strokePx * 0.8f, center = Offset(w / 2, h / 2))
            }

            "Smileys" -> { // Winking smiley face with tongue
                drawCircle(yellow, radius = w * 0.42f, center = Offset(w / 2, h / 2))
                drawCircle(outlineColor, radius = w * 0.42f, center = Offset(w / 2, h / 2), style = Stroke(strokePx))
                // Left eye
                drawCircle(outlineColor, radius = w * 0.065f, center = Offset(w * 0.35f, h * 0.38f))
                // Right wink eye arc
                val wink = Path().apply {
                    moveTo(w * 0.58f, h * 0.38f)
                    quadraticTo(w * 0.65f, h * 0.30f, w * 0.72f, h * 0.38f)
                }
                drawPath(wink, outlineColor, style = Stroke(strokePx * 0.9f, cap = StrokeCap.Round))
                // Open smile
                val smile = Path().apply {
                    moveTo(w * 0.28f, h * 0.58f)
                    quadraticTo(w * 0.50f, h * 0.78f, w * 0.72f, h * 0.58f)
                }
                drawPath(smile, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round))
            }

            "Gestures" -> { // Peace sign gesture
                val hand = Path().apply {
                    moveTo(w * 0.35f, h * 0.85f)
                    lineTo(w * 0.35f, h * 0.45f)
                    lineTo(w * 0.35f, h * 0.20f)
                    cubicTo(w * 0.35f, h * 0.12f, w * 0.45f, h * 0.12f, w * 0.45f, h * 0.20f)
                    lineTo(w * 0.45f, h * 0.42f)
                    lineTo(w * 0.58f, h * 0.20f)
                    cubicTo(w * 0.58f, h * 0.12f, w * 0.68f, h * 0.12f, w * 0.68f, h * 0.20f)
                    lineTo(w * 0.68f, h * 0.50f)
                    cubicTo(w * 0.78f, h * 0.55f, w * 0.78f, h * 0.75f, w * 0.65f, h * 0.85f)
                    close()
                }
                drawPath(hand, yellow)
                drawPath(hand, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            "Animals" -> { // Cute Cat Face
                val catHead = Path().apply {
                    moveTo(w * 0.20f, h * 0.22f)
                    lineTo(w * 0.36f, h * 0.40f)
                    lineTo(w * 0.64f, h * 0.40f)
                    lineTo(w * 0.80f, h * 0.22f)
                    lineTo(w * 0.85f, h * 0.65f)
                    cubicTo(w * 0.70f, h * 0.86f, w * 0.30f, h * 0.86f, w * 0.15f, h * 0.65f)
                    close()
                }
                drawPath(catHead, yellow)
                drawPath(catHead, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Inner ears (pink)
                val leftEar = Path().apply {
                    moveTo(w * 0.24f, h * 0.28f)
                    lineTo(w * 0.33f, h * 0.40f)
                    lineTo(w * 0.23f, h * 0.42f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(w * 0.76f, h * 0.28f)
                    lineTo(w * 0.67f, h * 0.40f)
                    lineTo(w * 0.77f, h * 0.42f)
                    close()
                }
                drawPath(leftEar, red)
                drawPath(rightEar, red)
                // Eyes & nose
                drawCircle(outlineColor, radius = w * 0.055f, center = Offset(w * 0.36f, h * 0.54f))
                drawCircle(outlineColor, radius = w * 0.055f, center = Offset(w * 0.64f, h * 0.54f))
                drawCircle(red, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.62f))
            }

            "Food" -> { // Pizza Slice with Pepperoni
                val slice = Path().apply {
                    moveTo(w * 0.18f, h * 0.20f)
                    lineTo(w * 0.82f, h * 0.20f)
                    lineTo(w * 0.50f, h * 0.88f)
                    close()
                }
                drawPath(slice, orange)
                drawPath(slice, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Crust
                drawLine(outlineColor, Offset(w * 0.18f, h * 0.20f), Offset(w * 0.82f, h * 0.20f), strokePx * 1.5f, cap = StrokeCap.Round)
                // Pepperoni spots
                drawCircle(red, radius = w * 0.08f, center = Offset(w * 0.45f, h * 0.42f))
                drawCircle(outlineColor, radius = w * 0.08f, center = Offset(w * 0.45f, h * 0.42f), style = Stroke(strokePx * 0.6f))
                drawCircle(red, radius = w * 0.07f, center = Offset(w * 0.58f, h * 0.58f))
                drawCircle(outlineColor, radius = w * 0.07f, center = Offset(w * 0.58f, h * 0.58f), style = Stroke(strokePx * 0.6f))
            }

            "Travel" -> { // Earth Globe
                drawCircle(cyan, radius = w * 0.42f, center = Offset(w / 2, h / 2))
                drawCircle(outlineColor, radius = w * 0.42f, center = Offset(w / 2, h / 2), style = Stroke(strokePx))
                // Equator & Tropics
                drawLine(outlineColor, Offset(w * 0.08f, h * 0.50f), Offset(w * 0.92f, h * 0.50f), strokePx * 0.9f)
                val tropicTop = Path().apply {
                    moveTo(w * 0.18f, h * 0.30f)
                    quadraticTo(w * 0.50f, h * 0.38f, w * 0.82f, h * 0.30f)
                }
                drawPath(tropicTop, outlineColor, style = Stroke(strokePx * 0.8f))
                val tropicBottom = Path().apply {
                    moveTo(w * 0.18f, h * 0.70f)
                    quadraticTo(w * 0.50f, h * 0.62f, w * 0.82f, h * 0.70f)
                }
                drawPath(tropicBottom, outlineColor, style = Stroke(strokePx * 0.8f))
            }

            "Objects" -> { // Glowing Lightbulb
                val bulb = Path().apply {
                    moveTo(w * 0.35f, h * 0.65f)
                    cubicTo(w * 0.15f, h * 0.45f, w * 0.25f, h * 0.15f, w * 0.50f, h * 0.15f)
                    cubicTo(w * 0.75f, h * 0.15f, w * 0.85f, h * 0.45f, w * 0.65f, h * 0.65f)
                    lineTo(w * 0.60f, h * 0.82f)
                    lineTo(w * 0.40f, h * 0.82f)
                    close()
                }
                drawPath(bulb, gold)
                drawPath(bulb, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Screw base lines
                drawLine(outlineColor, Offset(w * 0.38f, h * 0.72f), Offset(w * 0.62f, h * 0.72f), strokePx * 0.9f)
                // Glow rays
                drawLine(orange, Offset(w * 0.50f, h * 0.04f), Offset(w * 0.50f, h * 0.10f), strokePx * 0.9f, cap = StrokeCap.Round)
                drawLine(orange, Offset(w * 0.12f, h * 0.25f), Offset(w * 0.20f, h * 0.30f), strokePx * 0.9f, cap = StrokeCap.Round)
                drawLine(orange, Offset(w * 0.88f, h * 0.25f), Offset(w * 0.80f, h * 0.30f), strokePx * 0.9f, cap = StrokeCap.Round)
            }

            "Symbols" -> { // Glossy Red Heart
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.85f)
                    cubicTo(w * 0.15f, h * 0.55f, w * 0.05f, h * 0.28f, w * 0.30f, h * 0.16f)
                    cubicTo(w * 0.42f, h * 0.12f, w * 0.50f, h * 0.24f, w * 0.50f, h * 0.24f)
                    cubicTo(w * 0.50f, h * 0.24f, w * 0.58f, h * 0.12f, w * 0.70f, h * 0.16f)
                    cubicTo(w * 0.95f, h * 0.28f, w * 0.85f, h * 0.55f, w * 0.50f, h * 0.85f)
                    close()
                }
                drawPath(heart, red)
                drawPath(heart, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // White sheen arc
                val sheen = Path().apply {
                    moveTo(w * 0.22f, h * 0.32f)
                    quadraticTo(w * 0.30f, h * 0.22f, w * 0.38f, h * 0.25f)
                }
                drawPath(sheen, Color.White, style = Stroke(strokePx * 0.8f, cap = StrokeCap.Round))
            }

            "Flags" -> { // Waving Flag
                val pole = Path().apply {
                    moveTo(w * 0.22f, h * 0.12f)
                    lineTo(w * 0.22f, h * 0.88f)
                }
                drawLine(outlineColor, Offset(w * 0.22f, h * 0.12f), Offset(w * 0.22f, h * 0.88f), strokePx, cap = StrokeCap.Round)
                drawCircle(gold, radius = strokePx * 0.9f, center = Offset(w * 0.22f, h * 0.12f))
                val flag = Path().apply {
                    moveTo(w * 0.22f, h * 0.18f)
                    cubicTo(w * 0.45f, h * 0.26f, w * 0.60f, h * 0.12f, w * 0.82f, h * 0.22f)
                    lineTo(w * 0.82f, h * 0.54f)
                    cubicTo(w * 0.60f, h * 0.44f, w * 0.45f, h * 0.58f, w * 0.22f, h * 0.50f)
                    close()
                }
                drawPath(flag, red)
                drawPath(flag, outlineColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            else -> {
                drawCircle(yellow, radius = w * 0.42f, center = Offset(w / 2, h / 2))
                drawCircle(outlineColor, radius = w * 0.42f, center = Offset(w / 2, h / 2), style = Stroke(strokePx))
            }
        }
    }
}
