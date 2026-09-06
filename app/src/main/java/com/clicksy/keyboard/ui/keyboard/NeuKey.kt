package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputChange
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
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
 * High-Performance Neubrutalism Key Button.
 *
 * Optimized for:
 * - 0 memory allocations in draw pass
 * - GPU-accelerated drawing via native Compose drawScope
 * - Instant touch feedback with 0 coroutine overhead on standard characters
 * - Hardware layer translation without recomposition
 */
@Composable
fun NeuKey(
    label: String,
    modifier: Modifier = Modifier,
    keyType: KeyType = KeyType.CHARACTER,
    textStyle: TextStyle = ClicksyTypography.keyLabel,
    subLabel: String? = null,
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

    val subLabelStyle = remember(dims.keyHeight) {
        val heightFactor = dims.keyHeight.value / 52f
        ClicksyTypography.keySubLabel.copy(
            fontSize = (ClicksyTypography.keySubLabel.fontSize.value * heightFactor).sp
        )
    }

    val isRounded = dims.keyCornerRadius > 10.dp
    val subLabelEndPadding = remember(dims.shadowOffsetX, dims.borderWidth, isRounded) {
        if (isRounded) {
            dims.shadowOffsetX + dims.borderWidth + 4.dp
        } else {
            dims.shadowOffsetX + dims.borderWidth + 1.5.dp
        }
    }
    val subLabelTopPadding = remember(dims.borderWidth, isRounded) {
        if (isRounded) {
            dims.borderWidth + 1.5.dp
        } else {
            dims.borderWidth + 0.5.dp
        }
    }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnRepeat by rememberUpdatedState(onRepeat)

    var isPressed by remember { mutableStateOf(false) }

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

    val subLabelColor = when (keyType) {
        KeyType.ACTION -> colors.textOnAction.copy(alpha = 0.65f)
        KeyType.ACCENT -> colors.textOnAccent.copy(alpha = 0.65f)
        else -> colors.textSecondary.copy(alpha = 0.7f)
    }

    val shadowColor = colors.shadow
    val borderColor = colors.border

    val shadowOffsetPx = with(density) { dims.shadowOffsetX.toPx() }
    val shadowOffsetYPx = with(density) { dims.shadowOffsetY.toPx() }
    val borderWidthPx = with(density) { dims.borderWidth.toPx() }
    val cornerRadiusPx = with(density) { dims.keyCornerRadius.toPx() }
    val pressedTranslatePx = with(density) { (dims.shadowOffsetX - 1.dp).coerceAtLeast(0.dp).toPx() }

    Box(
        modifier = modifier
            .height(dims.keyHeight)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val targetPointerId = down.id

                    if (currentOnRepeat != null) {
                        // Repeating keys (e.g. Backspace): Fire first delete immediately on DOWN
                        currentOnTap()
                        val repeatJob = scope.launch {
                            delay(320L)
                            while (true) {
                                currentOnRepeat?.invoke()
                                delay(45L)
                            }
                        }
                        try {
                            isPressed = true
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == targetPointerId }
                                if (change == null || !change.pressed || change.isConsumed) {
                                    break
                                }
                            }
                        } finally {
                            repeatJob.cancel()
                            isPressed = false
                        }
                    } else if (currentOnLongPress != null) {
                        // Long-pressable keys (e.g. Shift, symbols on character keys)
                        var isLongPressHandled = false
                        val longPressJob = scope.launch {
                            delay(300L)
                            isLongPressHandled = true
                            currentOnLongPress?.invoke()
                        }
                        var upEvent: PointerInputChange? = null
                        try {
                            isPressed = true
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == targetPointerId }
                                if (change == null) {
                                    break
                                }
                                if (!change.pressed) {
                                    upEvent = change
                                    break
                                }
                                if (change.isConsumed) {
                                    break
                                }
                                // Cancel if dragged far outside keycap bounds
                                val pos = change.position
                                val slop = 50f
                                if (pos.x < -slop || pos.x > size.width + slop || pos.y < -slop || pos.y > size.height + slop) {
                                    break
                                }
                            }
                        } finally {
                            longPressJob.cancel()
                            isPressed = false
                        }
                        if (upEvent != null && !isLongPressHandled) {
                            currentOnTap()
                        }
                    } else {
                        // Standard character, symbol, space & enter keys:
                        // Fire instantly on touch DOWN for true 0ms commercial-grade typing response!
                        currentOnTap()
                        try {
                            isPressed = true
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == targetPointerId }
                                if (change == null || !change.pressed || change.isConsumed) {
                                    break
                                }
                            }
                        } finally {
                            isPressed = false
                        }
                    }
                }
            }
            .drawBehind {
                val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                val keyCapWidth = size.width - shadowOffsetPx
                val keyCapHeight = size.height - shadowOffsetYPx

                if (keyCapWidth <= 0f || keyCapHeight <= 0f) return@drawBehind

                val transX = if (isPressed) pressedTranslatePx else 0f
                val transY = if (isPressed) pressedTranslatePx else 0f

                // 1. Draw hard Neubrutalism drop shadow
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(shadowOffsetPx, shadowOffsetYPx),
                    size = Size(keyCapWidth, keyCapHeight),
                    cornerRadius = cornerRadius
                )

                // 2. Draw keycap fill
                drawRoundRect(
                    color = backgroundColor,
                    topLeft = Offset(transX, transY),
                    size = Size(keyCapWidth, keyCapHeight),
                    cornerRadius = cornerRadius
                )

                // 3. Draw solid black border
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(transX, transY),
                    size = Size(keyCapWidth, keyCapHeight),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = borderWidthPx)
                )
            }
            .graphicsLayer {
                val trans = if (isPressed) pressedTranslatePx else 0f
                translationX = trans - (shadowOffsetPx / 2f)
                translationY = trans - (shadowOffsetYPx / 2f)
            },
        contentAlignment = Alignment.Center
    ) {
        if (subLabel != null) {
            Text(
                text = subLabel,
                style = subLabelStyle,
                color = subLabelColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = subLabelTopPadding, end = subLabelEndPadding)
            )
        }
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
