package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography

private val SYMBOLS_PAGE_1_ROW_1 = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val SYMBOLS_PAGE_1_ROW_2 = arrayOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
private val SYMBOLS_PAGE_1_ROW_3 = arrayOf("*", "\"", "'", ":", ";", "!", "?")

private val SYMBOLS_PAGE_2_ROW_1 = arrayOf("~", "`", "|", "•", "√", "π", "÷", "×", "{", "}")
private val SYMBOLS_PAGE_2_ROW_2 = arrayOf("£", "¥", "€", "¢", "^", "°", "=", "[", "]", "\\")
private val SYMBOLS_PAGE_2_ROW_3 = arrayOf("%", "©", "®", "™", "✓", "<", ">")

/**
 * Number + Symbol keyboard layouts (two pages).
 *
 * Optimized with static matrices and zero-allocation key iteration.
 */
@Composable
fun SymbolLayout(
    isPage2: Boolean,
    enterLabel: String,
    onCharacterInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToQwerty: () -> Unit,
    onSwitchToEmoji: () -> Unit,
    onSwitchToNumpad: () -> Unit,
    onToggleSymbolPage: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    val row1 = if (!isPage2) SYMBOLS_PAGE_1_ROW_1 else SYMBOLS_PAGE_2_ROW_1
    val row2 = if (!isPage2) SYMBOLS_PAGE_1_ROW_2 else SYMBOLS_PAGE_2_ROW_2
    val row3 = if (!isPage2) SYMBOLS_PAGE_1_ROW_3 else SYMBOLS_PAGE_2_ROW_3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.keyboardPadding, vertical = dims.keyboardPadding),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1: numbers or extra symbols
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            row1.forEach { char ->
                NeuKey(
                    label = char,
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(char) }
                )
            }
        }

        // Row 2: common symbols
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            row2.forEach { char ->
                NeuKey(
                    label = char,
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(char) }
                )
            }
        }

        // Row 3: toggle + symbols + backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Page toggle key
            NeuKey(
                label = if (!isPage2) "=\\<" else "?123",
                modifier = Modifier.weight(1.5f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onToggleSymbolPage
            )

            row3.forEach { char ->
                NeuKey(
                    label = char,
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(char) }
                )
            }

            // Backspace
            NeuKey(
                label = "⌫",
                modifier = Modifier.weight(1.5f),
                keyType = KeyType.CHARACTER,
                onTap = onBackspace,
                onRepeat = onBackspace
            )
        }

        // Row 4: ABC + 1234 (Numpad) + emoji + comma + space + period + enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            NeuKey(
                label = "ABC",
                modifier = Modifier.weight(1.4f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSwitchToQwerty
            )

            NeuKey(
                label = "1234",
                modifier = Modifier.weight(1.2f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSwitchToNumpad
            )

            NeuKey(
                label = "",
                modifier = Modifier.weight(1.0f),
                icon = { CustomEmojiIcon(size = 22.dp) },
                onTap = onSwitchToEmoji
            )

            NeuKey(
                label = ",",
                modifier = Modifier.weight(0.8f),
                onTap = { onCharacterInput(",") }
            )

            NeuKey(
                label = "",
                modifier = Modifier.weight(3.3f),
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSpace
            )

            NeuKey(
                label = ".",
                modifier = Modifier.weight(0.8f),
                onTap = { onCharacterInput(".") }
            )

            NeuKey(
                label = enterLabel,
                modifier = Modifier.weight(1.5f),
                keyType = KeyType.ACTION,
                textStyle = if (enterLabel.length > 2) ClicksyTypography.keyLabelSmall else ClicksyTypography.keyLabel,
                onTap = onEnter
            )
        }
    }
}
