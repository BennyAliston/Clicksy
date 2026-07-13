package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicksy.keyboard.data.ShiftState
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography

/**
 * Standard QWERTY keyboard layout.
 *
 * Layout:
 * Row 1: Q W E R T Y U I O P
 * Row 2:  A S D F G H J K L
 * Row 3: ⇧ Z X C V B N M ⌫
 * Row 4: ?123 😊 , [  space  ] . ↵
 */
@Composable
fun QwertyLayout(
    shiftState: ShiftState,
    enterLabel: String,
    onCharacterInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onShiftToggle: () -> Unit,
    onShiftLock: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToSymbols: () -> Unit,
    onSwitchToEmoji: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier,
    showNumberRow: Boolean = false
) {
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    val displayChar: (String) -> String = { char ->
        when (shiftState) {
            ShiftState.OFF -> char.lowercase()
            ShiftState.ONCE, ShiftState.CAPS_LOCK -> char.uppercase()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.keyboardPadding, vertical = dims.keyboardPadding),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        if (showNumberRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { numChar ->
                    NeuKey(
                        label = numChar,
                        modifier = Modifier.weight(1f),
                        onTap = { onCharacterInput(numChar) }
                    )
                }
            }
        }

        // Row 1: Q W E R T Y U I O P
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach { char ->
                NeuKey(
                    label = displayChar(char),
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(displayChar(char)) }
                )
            }
        }

        // Row 2: A S D F G H J K L (indented using weight-based spacers to scale horizontally)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { char ->
                NeuKey(
                    label = displayChar(char),
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(displayChar(char)) }
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 3: Shift + Z X C V B N M + Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Shift key
            NeuKey(
                label = when (shiftState) {
                    ShiftState.OFF -> "⇧"
                    ShiftState.ONCE -> "⇧"
                    ShiftState.CAPS_LOCK -> "⇪"
                },
                modifier = Modifier.weight(1.5f),
                keyType = if (shiftState != ShiftState.OFF) KeyType.ACCENT else KeyType.CHARACTER,
                onTap = onShiftToggle,
                onLongPress = onShiftLock
            )

            listOf("z", "x", "c", "v", "b", "n", "m").forEach { char ->
                NeuKey(
                    label = displayChar(char),
                    modifier = Modifier.weight(1f),
                    onTap = { onCharacterInput(displayChar(char)) }
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

        // Row 4: ?123 + 😊 + , + space + . + Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            NeuKey(
                label = "?123",
                modifier = Modifier.weight(1.5f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSwitchToSymbols
            )

            NeuKey(
                label = "😊",
                modifier = Modifier.weight(1.0f),
                onTap = onSwitchToEmoji
            )

            NeuKey(
                label = ",",
                modifier = Modifier.weight(1.0f),
                onTap = { onCharacterInput(",") }
            )

            // Space bar
            NeuKey(
                label = "English",
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
