package com.clicksy.keyboard.util

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.clicksy.keyboard.data.AutoCapsType
import com.clicksy.keyboard.data.DetectedInputType
import com.clicksy.keyboard.data.KeyboardMode

/**
 * Immutable configuration derived from EditorInfo for keyboard layout and behavior.
 */
data class InputConfiguration(
    val detectedType: DetectedInputType = DetectedInputType.TEXT,
    val initialMode: KeyboardMode = KeyboardMode.QWERTY,
    val autoCapsType: AutoCapsType = AutoCapsType.SENTENCES,
    val suggestionsAllowed: Boolean = true,
    val enterLabel: String = "↵",
    val actionId: Int = EditorInfo.IME_ACTION_UNSPECIFIED
)

/**
 * Analyzes EditorInfo to automatically detect the desired input type,
 * capitalization rules, suggestion allowances, and initial keyboard layout.
 */
object InputTypeDetector {

    /**
     * Inspects EditorInfo and returns an InputConfiguration specifying the
     * layout mode, auto-caps mode, suggestion permissions, and action button.
     */
    fun detect(info: EditorInfo?): InputConfiguration {
        if (info == null || info.inputType == InputType.TYPE_NULL) {
            val hintFallback = checkHintFallback(info)
            if (hintFallback != null) {
                return hintFallback
            }
            return InputConfiguration(
                detectedType = DetectedInputType.TEXT,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                enterLabel = getEnterLabel(info),
                actionId = getActionId(info)
            )
        }

        val inputType = info.inputType
        val maskClass = inputType and InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and InputType.TYPE_MASK_VARIATION
        val maskFlags = inputType and InputType.TYPE_MASK_FLAGS

        val detectedType: DetectedInputType
        val initialMode: KeyboardMode
        val autoCapsType: AutoCapsType
        val suggestionsAllowed: Boolean

        when (maskClass) {
            InputType.TYPE_CLASS_NUMBER -> {
                if (maskVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                    detectedType = DetectedInputType.NUMBER_PASSWORD
                    suggestionsAllowed = false
                } else {
                    detectedType = DetectedInputType.NUMBER
                    suggestionsAllowed = false
                }
                initialMode = KeyboardMode.NUMPAD
                autoCapsType = AutoCapsType.NONE
            }

            InputType.TYPE_CLASS_PHONE -> {
                detectedType = DetectedInputType.PHONE
                initialMode = KeyboardMode.NUMPAD
                autoCapsType = AutoCapsType.NONE
                suggestionsAllowed = false
            }

            InputType.TYPE_CLASS_DATETIME -> {
                detectedType = DetectedInputType.DATETIME
                initialMode = KeyboardMode.NUMPAD
                autoCapsType = AutoCapsType.NONE
                suggestionsAllowed = false
            }

            InputType.TYPE_CLASS_TEXT -> {
                when (maskVariation) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> {
                        detectedType = DetectedInputType.EMAIL
                        initialMode = KeyboardMode.QWERTY
                        autoCapsType = AutoCapsType.NONE
                        suggestionsAllowed = (maskFlags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0
                    }

                    InputType.TYPE_TEXT_VARIATION_URI -> {
                        detectedType = DetectedInputType.URI
                        initialMode = KeyboardMode.QWERTY
                        autoCapsType = AutoCapsType.NONE
                        suggestionsAllowed = (maskFlags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0
                    }

                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                        detectedType = DetectedInputType.PASSWORD
                        initialMode = KeyboardMode.QWERTY
                        autoCapsType = AutoCapsType.NONE
                        suggestionsAllowed = false
                    }

                    else -> {
                        // Check hint/field name heuristics if variation is normal text
                        val hintConfig = checkHintFallback(info)
                        if (hintConfig != null) {
                            return hintConfig
                        }

                        detectedType = DetectedInputType.TEXT
                        initialMode = KeyboardMode.QWERTY
                        suggestionsAllowed = (maskFlags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0

                        autoCapsType = when {
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0 -> AutoCapsType.CHARACTERS
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0 -> AutoCapsType.WORDS
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 -> AutoCapsType.SENTENCES
                            maskVariation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> AutoCapsType.WORDS
                            else -> AutoCapsType.SENTENCES
                        }
                    }
                }
            }

            else -> {
                val hintConfig = checkHintFallback(info)
                if (hintConfig != null) {
                    return hintConfig
                }

                detectedType = DetectedInputType.TEXT
                initialMode = KeyboardMode.QWERTY
                autoCapsType = AutoCapsType.SENTENCES
                suggestionsAllowed = true
            }
        }

        return InputConfiguration(
            detectedType = detectedType,
            initialMode = initialMode,
            autoCapsType = autoCapsType,
            suggestionsAllowed = suggestionsAllowed,
            enterLabel = getEnterLabel(info),
            actionId = getActionId(info)
        )
    }

    /**
     * Checks field name, hint text, and label as a fallback heuristic
     * for apps or webviews that do not set explicit InputType variations.
     */
    private fun checkHintFallback(info: EditorInfo?): InputConfiguration? {
        if (info == null) return null
        val hint = (info.hintText?.toString() ?: "").lowercase()
        val field = (info.fieldName ?: "").lowercase()
        val label = (info.label?.toString() ?: "").lowercase()
        val text = "$hint $field $label"

        if (text.isBlank()) return null

        val enterLabel = getEnterLabel(info)
        val actionId = getActionId(info)

        if (text.contains("email") || text.contains("e-mail")) {
            return InputConfiguration(
                detectedType = DetectedInputType.EMAIL,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = true,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (text.contains("phone") || text.contains("mobile") || text.contains("telephone")) {
            return InputConfiguration(
                detectedType = DetectedInputType.PHONE,
                initialMode = KeyboardMode.NUMPAD,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (text.contains("url") || text.contains("website") || text.contains("http")) {
            return InputConfiguration(
                detectedType = DetectedInputType.URI,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = true,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (text.contains("pin") || text.contains("passcode") || text.contains("otp")) {
            return InputConfiguration(
                detectedType = DetectedInputType.NUMBER_PASSWORD,
                initialMode = KeyboardMode.NUMPAD,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (text.contains("password")) {
            return InputConfiguration(
                detectedType = DetectedInputType.PASSWORD,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        return null
    }

    /**
     * Determines the appropriate label for the action/Enter key.
     */
    fun getEnterLabel(info: EditorInfo?): String {
        if (info == null) return "↵"
        if (!info.actionLabel.isNullOrEmpty()) {
            return info.actionLabel.toString()
        }
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_SEARCH -> "🔍"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_NEXT -> "→"
            EditorInfo.IME_ACTION_PREVIOUS -> "←"
            else -> "↵"
        }
    }

    /**
     * Resolves the target editor action ID.
     */
    fun getActionId(info: EditorInfo?): Int {
        if (info == null) return EditorInfo.IME_ACTION_UNSPECIFIED
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return EditorInfo.IME_ACTION_UNSPECIFIED
        }
        if (info.actionId != 0) return info.actionId
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            action
        } else {
            EditorInfo.IME_ACTION_UNSPECIFIED
        }
    }

    /**
     * Checks if the given input type represents any password or sensitive field.
     */
    fun isPasswordField(inputType: Int): Boolean {
        val maskClass = inputType and InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and InputType.TYPE_MASK_VARIATION
        
        val isTextPassword = maskClass == InputType.TYPE_CLASS_TEXT && (
            maskVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            maskVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            maskVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )
        val isNumberPassword = maskClass == InputType.TYPE_CLASS_NUMBER &&
            maskVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            
        return isTextPassword || isNumberPassword
    }
}