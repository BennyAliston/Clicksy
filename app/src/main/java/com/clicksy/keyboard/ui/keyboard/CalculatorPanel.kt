package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent
import com.clicksy.keyboard.util.CalculatorEngine

/**
 * Full Neubrutalist Inbuilt Calculator Panel for fast mathematical productivity.
 * Features live calculation preview, equation display, and instant paste-to-editor actions.
 */
@Composable
fun CalculatorPanel(
    onInsertText: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onRequestHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    var expression by remember { mutableStateOf("") }
    val liveResult = remember(expression) {
        if (expression.isNotBlank()) {
            CalculatorEngine.evaluate(expression)
        } else {
            null
        }
    }

    fun appendInput(token: String) {
        expression += token
        onRequestHaptic()
    }

    fun handleBackspace() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
        }
        onRequestHaptic()
    }

    fun handleClear() {
        expression = ""
        onRequestHaptic()
    }

    fun appendParenthesis() {
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val lastChar = expression.lastOrNull()
        if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
            expression += ")"
        } else {
            expression += "("
        }
        onRequestHaptic()
    }

    fun handleEquals() {
        liveResult?.let { res ->
            if (res != "Error") {
                expression = res
            }
        }
        onRequestHaptic()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // --- Calculator Display Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.suggestionBarHeight + 10.dp)
                .background(colors.suggestionBarBackground)
                .drawBehind {
                    val strokeWidthPx = dims.borderWidth.toPx()
                    drawLine(
                        color = colors.border,
                        start = Offset(0f, size.height - strokeWidthPx / 2),
                        end = Offset(size.width, size.height - strokeWidthPx / 2),
                        strokeWidth = strokeWidthPx
                    )
                }
                .padding(horizontal = dims.keyboardPadding + 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live expression and preview calculation
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    style = ClicksyTypography.monoLabel.copy(
                        fontSize = if (expression.length > 18) 13.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (liveResult != null && liveResult != expression) {
                    Text(
                        text = "= $liveResult",
                        style = ClicksyTypography.monoLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }

            // Quick Paste / Insert Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (liveResult != null && liveResult != "Error") {
                    // Paste Answer Button
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .drawBehind {
                                drawRoundRect(
                                    color = colors.shadow,
                                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                    size = Size(size.width, size.height),
                                    cornerRadius = CornerRadius(6.dp.toPx())
                                )
                            }
                            .background(colors.actionKeyBackground, RoundedCornerShape(6.dp))
                            .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                            .clickable {
                                onInsertText(liveResult)
                                onBackToKeyboard()
                                onRequestHaptic()
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↵ Paste $liveResult",
                            style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textOnAction
                        )
                    }
                }

                // ABC Back to keyboard button
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .width(44.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                        }
                        .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                        .border(dims.borderWidth, colors.border, RoundedCornerShape(6.dp))
                        .clickable {
                            onBackToKeyboard()
                            onRequestHaptic()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ABC",
                        style = ClicksyTypography.keyLabelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textOnAccent
                    )
                }
            }
        }

        // --- Symmetrical 5x4 Calculator Keypad Grid ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.keyboardPadding, vertical = dims.keyboardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Row 1: C  7  8  9  ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NeuKey(
                    label = "C",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACTION,
                    onTap = ::handleClear
                )
                NeuKey(
                    label = "7",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("7") }
                )
                NeuKey(
                    label = "8",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("8") }
                )
                NeuKey(
                    label = "9",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("9") }
                )
                NeuKey(
                    label = "÷",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { appendInput("÷") }
                )
            }

            // Row 2: ( )  4  5  6  ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NeuKey(
                    label = "( )",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    textStyle = ClicksyTypography.keyLabelSmall,
                    onTap = ::appendParenthesis,
                    onLongPress = { appendInput(")") }
                )
                NeuKey(
                    label = "4",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("4") }
                )
                NeuKey(
                    label = "5",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("5") }
                )
                NeuKey(
                    label = "6",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("6") }
                )
                NeuKey(
                    label = "×",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { appendInput("×") }
                )
            }

            // Row 3: %  1  2  3  -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NeuKey(
                    label = "%",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { appendInput("%") }
                )
                NeuKey(
                    label = "1",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("1") }
                )
                NeuKey(
                    label = "2",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("2") }
                )
                NeuKey(
                    label = "3",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("3") }
                )
                NeuKey(
                    label = "-",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { appendInput("-") }
                )
            }

            // Row 4: ⌫  0  .  =  +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NeuKey(
                    label = "⌫",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.CHARACTER,
                    onTap = ::handleBackspace,
                    onRepeat = ::handleBackspace
                )
                NeuKey(
                    label = "0",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput("0") }
                )
                NeuKey(
                    label = ".",
                    modifier = Modifier.weight(1f),
                    onTap = { appendInput(".") }
                )
                NeuKey(
                    label = "=",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACTION,
                    onTap = ::handleEquals
                )
                NeuKey(
                    label = "+",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { appendInput("+") }
                )
            }
        }
    }
}
