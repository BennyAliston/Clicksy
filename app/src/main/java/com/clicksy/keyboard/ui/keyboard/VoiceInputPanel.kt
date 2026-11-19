package com.clicksy.keyboard.ui.keyboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyTypography
import com.clicksy.keyboard.ui.theme.textOnAccent

/**
 * Dedicated Neubrutalist Voice-to-Text Input Panel.
 *
 * Features:
 * - Live real-time audio waveform equalizer reacting to voice RMS dB
 * - Neubrutalist microphone button with pulse ring animation
 * - Live streaming transcription preview card with animated cursor
 * - Quick bottom action keys (Delete, Space, Done) reusing NeuKey
 * - Top header with status indicator and instant return to keyboard
 * - Permission request prompt button if microphone permission is not yet granted
 */
@Composable
fun VoiceInputPanel(
    isListening: Boolean,
    rmsDb: Float,
    partialText: String,
    statusMessage: String,
    hasPermission: Boolean,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onDone: () -> Unit,
    onRequestHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClicksyTheme.colors
    val dims = ClicksyTheme.dimensions
    val cornerRadius = dims.keyCornerRadius
    val borderWidth = dims.borderWidth
    val shadowOffset = dims.shadowOffsetX

    val infiniteTransition = rememberInfiniteTransition(label = "VoicePanelAnimations")

    // Cursor blink for transcription card
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorBlink"
    )

    // Pulse animation for active microphone button
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = dims.keyboardPadding, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. TOP HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Neubrutalist Badge
            Box(
                modifier = Modifier
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    .background(colors.accentKeyBackground, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🎙️ VOICE TYPING",
                    style = ClicksyTypography.keyLabelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textOnAccent
                    )
                )
            }

            // Center: Live Listening status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isListening) colors.actionKeyBackground else colors.textSecondary.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
                Text(
                    text = if (isListening) "LISTENING..." else if (!hasPermission) "PERMISSION NEEDED" else "TAP MIC TO SPEAK",
                    style = ClicksyTypography.keyLabelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) colors.actionKeyBackground else colors.textSecondary
                    )
                )
            }

            // Right: Return to Keyboard button
            Box(
                modifier = Modifier
                    .drawBehind {
                        drawRoundRect(
                            color = colors.shadow,
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                    .background(colors.keyBackground, RoundedCornerShape(6.dp))
                    .border(2.dp, colors.border, RoundedCornerShape(6.dp))
                    .clickable {
                        onRequestHaptic()
                        onBackToKeyboard()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⌨️ KEYBOARD",
                    style = ClicksyTypography.keyLabelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. LIVE TRANSCRIPTION CARD ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .drawBehind {
                    drawRoundRect(
                        color = colors.shadow,
                        topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                }
                .background(colors.keyBackground, RoundedCornerShape(cornerRadius))
                .border(borderWidth, colors.border, RoundedCornerShape(cornerRadius))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (partialText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = partialText,
                        style = ClicksyTypography.keyLabel.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " |",
                        style = ClicksyTypography.keyLabel.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.actionKeyBackground.copy(alpha = cursorAlpha)
                        )
                    )
                }
            } else {
                Text(
                    text = when {
                        !hasPermission -> "Microphone permission required. Tap below to enable voice typing."
                        isListening -> "Listening... Speak clearly into your phone's microphone."
                        statusMessage.isNotBlank() -> statusMessage
                        else -> "Say something... Your words will appear right here."
                    },
                    style = ClicksyTypography.keyLabelSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. AUDIO WAVEFORM EQUALIZER & BIG PULSING MIC ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Equalizer Bars (3 bars)
            WaveformEqualizerSide(
                isListening = isListening,
                rmsDb = rmsDb,
                barFactors = listOf(0.45f, 0.85f, 0.6f),
                borderColor = colors.border,
                barColor = colors.accentKeyBackground
            )

            // Center: Big Neubrutalist Microphone Button or Permission Grant Button
            if (!hasPermission) {
                // Permission Request Action Button
                Box(
                    modifier = Modifier
                        .drawBehind {
                            drawRoundRect(
                                color = colors.shadow,
                                topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        }
                        .background(colors.actionKeyBackground, RoundedCornerShape(12.dp))
                        .border(borderWidth, colors.border, RoundedCornerShape(12.dp))
                        .clickable {
                            onRequestHaptic()
                            onRequestPermission()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎙️ GRANT PERMISSION",
                        style = ClicksyTypography.keyLabelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textOnAction
                        )
                    )
                }
            } else {
                // Big Round Microphone Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    // Outer pulse ring when active
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .background(
                                    colors.actionKeyBackground.copy(alpha = pulseAlpha),
                                    CircleShape
                                )
                                .border(2.dp, colors.border.copy(alpha = pulseAlpha), CircleShape)
                        )
                    }

                    // Main Neubrutalist Mic Button
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .drawBehind {
                                drawCircle(
                                    color = colors.shadow,
                                    center = Offset(size.width / 2 + 3.dp.toPx(), size.height / 2 + 3.dp.toPx()),
                                    radius = size.minDimension / 2
                                )
                            }
                            .background(
                                if (isListening) colors.actionKeyBackground else colors.accentKeyBackground,
                                CircleShape
                            )
                            .border(borderWidth, colors.border, CircleShape)
                            .clickable {
                                onRequestHaptic()
                                onToggleListening()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isListening) "⏹️" else "🎙️",
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Right Equalizer Bars (3 bars)
            WaveformEqualizerSide(
                isListening = isListening,
                rmsDb = rmsDb,
                barFactors = listOf(0.6f, 0.9f, 0.5f),
                borderColor = colors.border,
                barColor = colors.actionKeyBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 4. BOTTOM ACTION ROW (Delete, Space, Done) ---
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.keySpacing)
        ) {
            NeuKey(
                label = "⌫ Delete",
                modifier = Modifier.weight(1.2f),
                keyType = KeyType.ACCENT,
                onTap = {
                    onDelete()
                    onRequestHaptic()
                }
            )

            NeuKey(
                label = "␣ Space",
                modifier = Modifier.weight(1.6f),
                keyType = KeyType.CHARACTER,
                onTap = {
                    onSpace()
                    onRequestHaptic()
                }
            )

            NeuKey(
                label = "↵ Done",
                modifier = Modifier.weight(1.2f),
                keyType = KeyType.ACTION,
                onTap = {
                    onDone()
                    onRequestHaptic()
                }
            )
        }
    }
}

/**
 * Animated Neubrutalist Equalizer side-bars that react to microphone RMS volume.
 */
@Composable
private fun WaveformEqualizerSide(
    isListening: Boolean,
    rmsDb: Float,
    barFactors: List<Float>,
    borderColor: Color,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    // Normalize rmsDb: Android SpeechRecognizer typically reports -2.0f to 10.0f
    val normalizedVolume = if (isListening) {
        ((rmsDb + 2f) / 12f).coerceIn(0.12f, 1f)
    } else {
        0.1f
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barFactors.forEach { factor ->
            val targetHeight = if (isListening) {
                (14.dp + (46.dp * normalizedVolume * factor)).coerceIn(12.dp, 56.dp)
            } else {
                10.dp
            }

            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                label = "EqualizerBarHeight"
            )

            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(3.dp))
            )
        }
    }
}
