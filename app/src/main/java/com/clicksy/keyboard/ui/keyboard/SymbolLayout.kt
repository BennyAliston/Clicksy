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

/**
 * Number + Symbol keyboard layouts (two pages).
 *
 * Page 1 (SYMBOLS_1):
 * Row 1: 1 2 3 4 5 6 7 8 9 0
 * Row 2: @ # $ _ & - + ( ) /
 * Row 3: =\< * " ' : ; ! ? ⌫
 * Row 4: ABC 😊 , [space] . ↵
 *
 * Page 2 (SYMBOLS_2):
 * Row 1: ~ ` | • √ π ÷ × { }
 * Row 2: £ ¥ € ¢ ^ ° = [ ] \
 * Row 3: ?123 % © ® ™ ✓ < > ⌫
 * Row 4: ABC 😊 , [space] . ↵
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
    onToggleSymbolPage: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    val rows = if (!isPage2) {
        listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?")
        )
    } else {
        listOf(
            listOf("~", "`", "|", "•", "√", "π", "÷", "×", "{", "}"),
            listOf("£", "¥", "€", "¢", "^", "°", "=", "[", "]", "\\"),
            listOf("%", "©", "®", "™", "✓", "<", ">")
        )
    }

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
            rows[0].forEach { char ->
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
            rows[1].forEach { char ->
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

            rows[2].forEach { char ->
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

        // Row 4: ABC + emoji + comma + space + period + enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            NeuKey(
                label = "ABC",
                modifier = Modifier.weight(1.5f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSwitchToQwerty
            )

            NeuKey(
                label = "",
                modifier = Modifier.weight(1.0f),
                icon = { CustomEmojiIcon(size = 22.dp) },
                onTap = onSwitchToEmoji
            )

            NeuKey(
                label = ",",
                modifier = Modifier.weight(1.0f),
                onTap = { onCharacterInput(",") }
            )

            NeuKey(
                label = "",
                modifier = Modifier.weight(4.0f),
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSpace
            )

            NeuKey(
                label = ".",
                modifier = Modifier.weight(1.0f),
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
