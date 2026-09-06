package com.clicksy.keyboard.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clicksy.keyboard.data.DetectedInputType
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography

/**
 * Dedicated Numeric Keypad (Numpad) layout for number-heavy input.
 *
 * Automatically adapts operator and utility keys based on whether input is:
 * - Standard numeric (calculator, arithmetic operators)
 * - Telephone keypad (+, -, *, #, pause, wait)
 * - Date/time (/, -, :, AM, PM)
 * - PIN / Number password (clean numeric grid)
 */
@Composable
fun NumpadLayout(
    enterLabel: String,
    onCharacterInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToQwerty: () -> Unit,
    onSwitchToSymbols: () -> Unit,
    onSwitchToCalculator: () -> Unit = {},
    modifier: Modifier = Modifier,
    detectedInputType: DetectedInputType = DetectedInputType.NUMBER
) {
    val dims = ClicksyTheme.dimensions
    val spacing = dims.keySpacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.keyboardPadding, vertical = dims.keyboardPadding),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            when (detectedInputType) {
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = "/",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("/") }
                    )
                }
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    NeuKey(
                        label = "+",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("+") }
                    )
                }
            }

            NeuKey(
                label = "1",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("1") }
            )
            NeuKey(
                label = "2",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("2") }
            )
            NeuKey(
                label = "3",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("3") }
            )

            if (detectedInputType == DetectedInputType.NUMBER_PASSWORD) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                NeuKey(
                    label = "-",
                    modifier = Modifier.weight(1f),
                    keyType = KeyType.ACCENT,
                    onTap = { onCharacterInput("-") }
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            when (detectedInputType) {
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = ":",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput(":") }
                    )
                }
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    NeuKey(
                        label = "*",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("*") }
                    )
                }
            }

            NeuKey(
                label = "4",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("4") }
            )
            NeuKey(
                label = "5",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("5") }
            )
            NeuKey(
                label = "6",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("6") }
            )

            when (detectedInputType) {
                DetectedInputType.PHONE -> {
                    NeuKey(
                        label = ",",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput(",") }
                    )
                }
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = ".",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput(".") }
                    )
                }
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    NeuKey(
                        label = "/",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("/") }
                    )
                }
            }
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            when (detectedInputType) {
                DetectedInputType.PHONE -> {
                    NeuKey(
                        label = "#",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("#") }
                    )
                }
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = "AM",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        textStyle = ClicksyTypography.keyLabelSmall,
                        onTap = { onCharacterInput(" AM") }
                    )
                }
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    NeuKey(
                        label = "🧮",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = onSwitchToCalculator
                    )
                }
            }

            NeuKey(
                label = "7",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("7") }
            )
            NeuKey(
                label = "8",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("8") }
            )
            NeuKey(
                label = "9",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("9") }
            )

            NeuKey(
                label = "⌫",
                modifier = Modifier.weight(1f),
                keyType = KeyType.CHARACTER,
                onTap = onBackspace,
                onRepeat = onBackspace
            )
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            NeuKey(
                label = "ABC",
                modifier = Modifier.weight(1f),
                keyType = KeyType.ACCENT,
                textStyle = ClicksyTypography.keyLabelSmall,
                onTap = onSwitchToQwerty
            )

            when (detectedInputType) {
                DetectedInputType.PHONE -> {
                    NeuKey(
                        label = ";",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput(";") }
                    )
                }
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = "PM",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        textStyle = ClicksyTypography.keyLabelSmall,
                        onTap = { onCharacterInput(" PM") }
                    )
                }
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                else -> {
                    NeuKey(
                        label = "?123",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        textStyle = ClicksyTypography.keyLabelSmall,
                        onTap = onSwitchToSymbols
                    )
                }
            }

            NeuKey(
                label = "0",
                modifier = Modifier.weight(1f),
                onTap = { onCharacterInput("0") }
            )

            when (detectedInputType) {
                DetectedInputType.NUMBER_PASSWORD -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
                DetectedInputType.DATETIME -> {
                    NeuKey(
                        label = "/",
                        modifier = Modifier.weight(1f),
                        keyType = KeyType.ACCENT,
                        onTap = { onCharacterInput("/") }
                    )
                }
                else -> {
                    NeuKey(
                        label = ".",
                        modifier = Modifier.weight(1f),
                        onTap = { onCharacterInput(".") }
                    )
                }
            }

            NeuKey(
                label = enterLabel,
                modifier = Modifier.weight(1f),
                keyType = KeyType.ACTION,
                textStyle = if (enterLabel.length > 2) ClicksyTypography.keyLabelSmall else ClicksyTypography.keyLabel,
                onTap = onEnter
            )
        }
    }
}
