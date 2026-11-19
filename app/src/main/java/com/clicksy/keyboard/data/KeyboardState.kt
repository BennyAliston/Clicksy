package com.clicksy.keyboard.data

/**
 * Represents the current keyboard input mode.
 */
enum class KeyboardMode {
    QWERTY,
    SYMBOLS_1,
    SYMBOLS_2,
    NUMPAD,
    CALCULATOR,
    EMOJI,
    EMOJI_SEARCH,
    CLIPBOARD,
    VOICE_INPUT
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
 * Represents the detected input type requested by the application.
 */
enum class DetectedInputType {
    TEXT,
    EMAIL,
    URI,
    PASSWORD,
    NUMBER,
    NUMBER_PASSWORD,
    PHONE,
    DATETIME
}

/**
 * Rules for auto-capitalization based on input type and flags.
 */
enum class AutoCapsType {
    NONE,
    CHARACTERS,
    WORDS,
    SENTENCES
}

/**
 * Represents the available neubrutalism key shape styles.
 */
enum class KeyShapeStyle {
    ROUNDED,
    SHARP,
    SOFT,
    PILL,
    CUT_CORNER
}

/**
 * Represents the available neubrutalism themes.
 */
enum class NeuTheme {
    SUNSHINE,
    BUBBLEGUM,
    MINTY,
    MIDNIGHT,
    CYBERPUNK,
    LAVENDER,
    FOREST,
    RETRO,
    SUNSET,
    MONOCHROME,
    ADAPTIVE,
    CUSTOM
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
