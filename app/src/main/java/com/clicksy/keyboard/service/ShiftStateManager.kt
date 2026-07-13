package com.clicksy.keyboard.service

import com.clicksy.keyboard.data.ShiftState

/**
 * Handles Shift state transitions, timing, and rules to match Gboard.
 * Keeps the state and logic cleanly separated from the UI.
 */
class ShiftStateManager(
    private val onStateChanged: (ShiftState) -> Unit
) {
    var state: ShiftState = ShiftState.OFF
        private set(value) {
            if (field != value) {
                field = value
                onStateChanged(value)
            }
        }

    private var lastShiftTapTime = 0L
    private var lastShiftStateBeforeTap = ShiftState.OFF

    // Gboard double-tap detection window: approximately 350-500 milliseconds.
    private val doubleTapTimeout = 500L
    private val debounceTimeout = 100L

    /**
     * Handles a single tap on the Shift key.
     * Transition rules:
     * - If CAPS_LOCK is enabled: Transition to OFF.
     * - If OFF: Transition to ONCE.
     * - If ONCE:
     *     - If this tap is within the double tap window (100–500ms) of the first tap
     *       which triggered OFF -> ONCE, transition to CAPS_LOCK.
     *     - Otherwise, transition to OFF.
     */
    fun handleShiftTap() {
        val currentTime = System.currentTimeMillis()
        val delta = currentTime - lastShiftTapTime

        // Debounce touch jitter/bounces (ignore taps within 100ms)
        if (lastShiftTapTime > 0L && delta < debounceTimeout) {
            return
        }

        val previousState = state

        when (state) {
            ShiftState.CAPS_LOCK -> {
                // Tap Shift while CAPS_LOCK is enabled: Return to OFF.
                state = ShiftState.OFF
                lastShiftTapTime = 0L
                lastShiftStateBeforeTap = ShiftState.OFF
                return
            }
            ShiftState.ONCE -> {
                // Detect double tap: tap spacing must be within 100ms to 500ms,
                // and the previous state change must have been OFF -> ONCE.
                if (delta in debounceTimeout..doubleTapTimeout && lastShiftStateBeforeTap == ShiftState.OFF) {
                    state = ShiftState.CAPS_LOCK
                } else {
                    state = ShiftState.OFF
                }
            }
            ShiftState.OFF -> {
                state = ShiftState.ONCE
            }
        }

        lastShiftStateBeforeTap = previousState
        lastShiftTapTime = currentTime
    }

    /**
     * Handles long press on the Shift key.
     * Toggles CAPS_LOCK state: if already CAPS_LOCK, returns to OFF. Otherwise, enables CAPS_LOCK.
     */
    fun handleShiftLongPress() {
        state = if (state == ShiftState.CAPS_LOCK) {
            ShiftState.OFF
        } else {
            ShiftState.CAPS_LOCK
        }
        resetDoubleTapTimer()
    }

    /**
     * Handles typed characters.
     * If Shift is in ONCE state and an alphabet character is typed:
     * - Automatically transitions to OFF.
     * - Non-alphabet keys (numbers, symbols, spaces) do not consume the state.
     * In all cases, typing a character resets the double tap timer.
     */
    fun handleCharacterInput(text: String) {
        if (state == ShiftState.ONCE && text.isNotEmpty() && text.first().isLetter()) {
            state = ShiftState.OFF
        }
        resetDoubleTapTimer()
    }

    /**
     * Resets the double tap tracking timer.
     */
    fun resetDoubleTapTimer() {
        lastShiftTapTime = 0L
        lastShiftStateBeforeTap = ShiftState.OFF
    }

    /**
     * Forces the ShiftState directly (used for auto-capitalization resets).
     */
    fun forceState(newState: ShiftState) {
        state = newState
    }
}
