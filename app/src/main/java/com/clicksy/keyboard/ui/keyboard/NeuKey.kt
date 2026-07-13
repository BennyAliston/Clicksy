package com.clicksy.keyboard.ui.keyboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Classifies keys for color assignment.
 */
enum class KeyType {
    /** Standard letter/character key — white background */
    CHARACTER,
    /** Accent key (shift, symbols, emoji) — vibrant accent color */
    ACCENT,
    /** Action key (enter, send, search) — bold action color */
    ACTION
}

/**
 * The core neubrutalism key button.
 *
 * Features:
 * - Thick black border
 * - Hard offset shadow (no blur)
 * - Press animation: shadow shrinks + key translates down-right
 * - Long-press support for continuous backspace and popups
 */
@Composable
fun NeuKey(
    label: String,
    modifier: Modifier = Modifier,
    keyType: KeyType = KeyType.CHARACTER,
    textStyle: TextStyle = ClicksyTypography.keyLabel,
    icon: (@Composable () -> Unit)? = null,
    onTap: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val labelStyle = remember(textStyle, dims.keyHeight) {
        val heightFactor = dims.keyHeight.value / 52f
        textStyle.copy(fontSize = (textStyle.fontSize.value * heightFactor).sp)
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnRepeat by rememberUpdatedState(onRepeat)

    var isPressed by remember { mutableStateOf(false) }

    val shadowOffsetState = animateDpAsState(
        targetValue = if (isPressed) 1.dp else dims.shadowOffsetX,
        animationSpec = tween(durationMillis = 40),
        label = "shadowOffset"
    )

    val translateOffsetState = animateDpAsState(
        targetValue = if (isPressed) (dims.shadowOffsetX - 1.dp) else 0.dp,
        animationSpec = tween(durationMillis = 40),
        label = "translateOffset"
    )

    val backgroundColor = when (keyType) {
        KeyType.CHARACTER -> colors.keyBackground
        KeyType.ACCENT -> colors.accentKeyBackground
        KeyType.ACTION -> colors.actionKeyBackground
    }

    val textColor = when (keyType) {
        KeyType.ACTION -> colors.textOnAction
        KeyType.ACCENT -> colors.textOnAccent
        else -> colors.textPrimary
    }

    val keycapDrawable = remember(backgroundColor, colors.border, colors.shadow, dims.keyCornerRadius, dims.borderWidth) {
        val shadowDrawable = android.graphics.drawable.GradientDrawable().apply {
            setShape(android.graphics.drawable.GradientDrawable.RECTANGLE)
            cornerRadius = with(density) { dims.keyCornerRadius.toPx() }
            setColor(colors.shadow.toArgb())
        }
        val fillDrawable = android.graphics.drawable.GradientDrawable().apply {
            setShape(android.graphics.drawable.GradientDrawable.RECTANGLE)
            cornerRadius = with(density) { dims.keyCornerRadius.toPx() }
            setColor(backgroundColor.toArgb())
            setStroke(
                with(density) { dims.borderWidth.roundToPx() },
                colors.border.toArgb()
            )
        }
        val drawable = android.graphics.drawable.LayerDrawable(arrayOf<android.graphics.drawable.Drawable>(shadowDrawable, fillDrawable))
        val offsetPx = with(density) { 4.dp.roundToPx() }
        // Set insets: Shadow (index 0) offset down and right; Fill (index 1) padded down and right
        drawable.setLayerInset(0, offsetPx, offsetPx, 0, 0)
        drawable.setLayerInset(1, 0, 0, offsetPx, offsetPx)
        drawable
    }

    Box(
        modifier = modifier
            .height(dims.keyHeight)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var longPressed = false

                    val repeatJob = if (currentOnRepeat != null) {
                        scope.launch {
                            delay(400L)
                            while (true) {
                                currentOnRepeat?.invoke()
                                delay(50L)
                            }
                        }
                    } else null

                    val longPressJob = if (currentOnLongPress != null) {
                        scope.launch {
                            delay(500L)
                            longPressed = true
                            currentOnLongPress?.invoke()
                        }
                    } else null

                    val up = waitForUpOrCancellation()
                    isPressed = false
                    repeatJob?.cancel()
                    longPressJob?.cancel()

                    if (up != null && !longPressed) {
                        currentOnTap()
                    }
                }
            }
            .offset {
                val translatePx = with(density) { translateOffsetState.value.roundToPx() }
                IntOffset(
                    x = translatePx,
                    y = translatePx
                )
            }
            .drawBehind {
                keycapDrawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                keycapDrawable.draw(drawContext.canvas.nativeCanvas)
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else {
            Text(
                text = label,
                style = labelStyle,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
