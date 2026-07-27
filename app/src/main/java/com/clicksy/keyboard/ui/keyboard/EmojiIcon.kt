package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Neubrutalist Emoji Icon component for Clicksy Keyboard.
 *
 * Renders a stylized yellow smiley face with crisp black outlines,
 * matching Clicksy's neubrutalism aesthetic.
 */
@Composable
fun CustomEmojiIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    faceColor: Color = Color(0xFFFFDE4D),
    outlineColor: Color = Color(0xFF000000)
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        val strokeWidthPx = width * 0.08f

        // 1. Draw yellow face background
        drawCircle(
            color = faceColor,
            radius = (width / 2f) - (strokeWidthPx / 2f),
            center = Offset(width / 2f, height / 2f)
        )

        // 2. Draw black border outline
        drawCircle(
            color = outlineColor,
            radius = (width / 2f) - (strokeWidthPx / 2f),
            center = Offset(width / 2f, height / 2f),
            style = Stroke(width = strokeWidthPx)
        )

        // 3. Draw Left Eye
        drawCircle(
            color = outlineColor,
            radius = width * 0.07f,
            center = Offset(width * 0.35f, height * 0.40f)
        )

        // 4. Draw Right Eye
        drawCircle(
            color = outlineColor,
            radius = width * 0.07f,
            center = Offset(width * 0.65f, height * 0.40f)
        )

        // 5. Draw Neubrutalist Smile Arc
        val smilePath = Path().apply {
            moveTo(width * 0.30f, height * 0.58f)
            quadraticTo(
                width * 0.50f, height * 0.78f,
                width * 0.70f, height * 0.58f
            )
        }
        drawPath(
            path = smilePath,
            color = outlineColor,
            style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
