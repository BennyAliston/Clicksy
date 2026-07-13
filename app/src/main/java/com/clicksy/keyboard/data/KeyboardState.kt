package com.clicksy.keyboard.data

/**
 * Represents the current keyboard input mode.
 */
enum class KeyboardMode {
    QWERTY,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD
}

/**
 * Represents the shift/caps state.
 */
enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

/**
 * Represents the available neubrutalism themes.
 */
enum class NeuTheme {
    SUNSHINE,
    BUBBLEGUM,
    MINTY,
    ADAPTIVE
}

/**
 * Represents the keyboard size settings.
 */
enum class KeyboardSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

/**
 * Immutable state snapshot of the keyboard.
 */
data class KeyboardState(
    val mode: KeyboardMode = KeyboardMode.QWERTY,
    val shiftState: ShiftState = ShiftState.OFF,
    val isVoiceInputActive: Boolean = false,
    val currentTheme: NeuTheme = NeuTheme.SUNSHINE,
    val keyboardSize: KeyboardSize = KeyboardSize.MEDIUM,
    val hapticEnabled: Boolean = true,
    val autocompleteEnabled: Boolean = true
)
