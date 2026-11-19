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
    val autocorrectAllowed: Boolean = true,
    val enterLabel: String = "↵",
    val actionId: Int = EditorInfo.IME_ACTION_UNSPECIFIED
)

/**
 * Analyzes EditorInfo to automatically detect the desired input type,
 * capitalization rules, suggestion allowances, and initial keyboard layout.
 */
object InputTypeDetector {

    private val EMAIL_REGEX = Regex("\\b(email|e-mail)\\b", RegexOption.IGNORE_CASE)
    private val PHONE_REGEX = Regex("\\b(phone|mobile|telephone)\\b", RegexOption.IGNORE_CASE)
    private val URL_REGEX = Regex("\\b(url|website)\\b|https?://", RegexOption.IGNORE_CASE)
    private val PIN_REGEX = Regex("\\b(pin|passcode|otp)\\b", RegexOption.IGNORE_CASE)
    private val PASSWORD_REGEX = Regex("\\b(password|passwd)\\b", RegexOption.IGNORE_CASE)
    private val USERNAME_REGEX = Regex("\\b(username|user_name|handle|user_id|screen_name)\\b", RegexOption.IGNORE_CASE)

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
                autoCapsType = AutoCapsType.SENTENCES,
                suggestionsAllowed = true,
                autocorrectAllowed = true,
                enterLabel = getEnterLabel(info),
                actionId = getActionId(info)
            )
        }

        // Priority 1: Genuine Password & Sensitive Fields
        if (isRealPasswordField(info)) {
            val maskClass = info.inputType and InputType.TYPE_MASK_CLASS
            val isNumber = maskClass == InputType.TYPE_CLASS_NUMBER
            return InputConfiguration(
                detectedType = if (isNumber) DetectedInputType.NUMBER_PASSWORD else DetectedInputType.PASSWORD,
                initialMode = if (isNumber) KeyboardMode.NUMPAD else KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                autocorrectAllowed = false,
                enterLabel = getEnterLabel(info),
                actionId = getActionId(info)
            )
        }

        // Priority 2: Technical Username / Handle fields (e.g. Instagram @username, Twitter handle)
        // Genuine restriction: No suggestions, no autocorrect, no capitalization
        if (isUsernameField(info)) {
            return InputConfiguration(
                detectedType = DetectedInputType.TEXT,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                autocorrectAllowed = false,
                enterLabel = getEnterLabel(info),
                actionId = getActionId(info)
            )
        }

        // Priority 3: Search queries (Instagram Search, In-app search bars, Web queries)
        // Suggestions allowed for convenience, but autocorrect disabled so search terms/handles aren't mutated
        if (isSearchField(info)) {
            val maskVariation = info.inputType and InputType.TYPE_MASK_VARIATION
            val detectedType = when (maskVariation) {
                InputType.TYPE_TEXT_VARIATION_URI -> DetectedInputType.URI
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> DetectedInputType.EMAIL
                else -> DetectedInputType.TEXT
            }
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val actionId = if (action != EditorInfo.IME_ACTION_UNSPECIFIED && action != EditorInfo.IME_ACTION_NONE) {
                action
            } else {
                EditorInfo.IME_ACTION_SEARCH
            }
            return InputConfiguration(
                detectedType = detectedType,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = true,
                autocorrectAllowed = false,
                enterLabel = "🔍",
                actionId = actionId
            )
        }

        // Priority 4: Person Name fields (Instagram Profile Name, Contacts)
        // Capitalize words; suggestions allowed; autocorrect disabled to avoid altering personal names
        if (isPersonNameField(info)) {
            return InputConfiguration(
                detectedType = DetectedInputType.TEXT,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.WORDS,
                suggestionsAllowed = true,
                autocorrectAllowed = false,
                enterLabel = getEnterLabel(info),
                actionId = getActionId(info)
            )
        }

        // Priority 5: Profile Bio, Comments, Captions, Direct Messages & Chat Fields
        // Full natural-language support with suggestions and autocorrect enabled
        if (isMessagingOrChat(info) || isCommentOrCaption(info) || isBioField(info)) {
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val enterLabel = if (action == EditorInfo.IME_ACTION_SEND) "Send" else getEnterLabel(info)
            return InputConfiguration(
                detectedType = DetectedInputType.TEXT,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.SENTENCES,
                suggestionsAllowed = true,
                autocorrectAllowed = true,
                enterLabel = enterLabel,
                actionId = getActionId(info)
            )
        }

        val inputType = info.inputType
        val maskClass = inputType and InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and InputType.TYPE_MASK_VARIATION
        val maskFlags = inputType and InputType.TYPE_MASK_FLAGS

        when (maskClass) {
            InputType.TYPE_CLASS_NUMBER -> {
                val isNumberPassword = maskVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                return InputConfiguration(
                    detectedType = if (isNumberPassword) DetectedInputType.NUMBER_PASSWORD else DetectedInputType.NUMBER,
                    initialMode = KeyboardMode.NUMPAD,
                    autoCapsType = AutoCapsType.NONE,
                    suggestionsAllowed = false,
                    autocorrectAllowed = false,
                    enterLabel = getEnterLabel(info),
                    actionId = getActionId(info)
                )
            }

            InputType.TYPE_CLASS_PHONE -> {
                return InputConfiguration(
                    detectedType = DetectedInputType.PHONE,
                    initialMode = KeyboardMode.NUMPAD,
                    autoCapsType = AutoCapsType.NONE,
                    suggestionsAllowed = false,
                    autocorrectAllowed = false,
                    enterLabel = getEnterLabel(info),
                    actionId = getActionId(info)
                )
            }

            InputType.TYPE_CLASS_DATETIME -> {
                return InputConfiguration(
                    detectedType = DetectedInputType.DATETIME,
                    initialMode = KeyboardMode.NUMPAD,
                    autoCapsType = AutoCapsType.NONE,
                    suggestionsAllowed = false,
                    autocorrectAllowed = false,
                    enterLabel = getEnterLabel(info),
                    actionId = getActionId(info)
                )
            }

            InputType.TYPE_CLASS_TEXT -> {
                when (maskVariation) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> {
                        return InputConfiguration(
                            detectedType = DetectedInputType.EMAIL,
                            initialMode = KeyboardMode.QWERTY,
                            autoCapsType = AutoCapsType.NONE,
                            suggestionsAllowed = true,
                            autocorrectAllowed = false,
                            enterLabel = getEnterLabel(info),
                            actionId = getActionId(info)
                        )
                    }

                    InputType.TYPE_TEXT_VARIATION_URI -> {
                        return InputConfiguration(
                            detectedType = DetectedInputType.URI,
                            initialMode = KeyboardMode.QWERTY,
                            autoCapsType = AutoCapsType.NONE,
                            suggestionsAllowed = true,
                            autocorrectAllowed = false,
                            enterLabel = getEnterLabel(info),
                            actionId = getActionId(info)
                        )
                    }

                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> {
                        return InputConfiguration(
                            detectedType = DetectedInputType.PASSWORD,
                            initialMode = KeyboardMode.QWERTY,
                            autoCapsType = AutoCapsType.NONE,
                            suggestionsAllowed = false,
                            autocorrectAllowed = false,
                            enterLabel = getEnterLabel(info),
                            actionId = getActionId(info)
                        )
                    }

                    else -> {
                        val hintConfig = checkHintFallback(info)
                        if (hintConfig != null) {
                            return hintConfig
                        }

                        val hasNoSuggestions = (maskFlags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
                        val isMultiLine = (maskFlags and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

                        val autoCapsType = when {
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0 -> AutoCapsType.CHARACTERS
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0 -> AutoCapsType.WORDS
                            (maskFlags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 -> AutoCapsType.SENTENCES
                            maskVariation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> AutoCapsType.WORDS
                            else -> AutoCapsType.SENTENCES
                        }

                        // If TYPE_TEXT_FLAG_NO_SUGGESTIONS is present:
                        // In natural language or multiline fields, we keep suggestions and autocorrect enabled.
                        // In single-line generic fields with NO_SUGGESTIONS, allow suggestions in strip, but disable automatic space-correction.
                        val autocorrectAllowed = if (hasNoSuggestions && !isMultiLine && autoCapsType != AutoCapsType.SENTENCES) {
                            false
                        } else {
                            true
                        }

                        return InputConfiguration(
                            detectedType = DetectedInputType.TEXT,
                            initialMode = KeyboardMode.QWERTY,
                            autoCapsType = autoCapsType,
                            suggestionsAllowed = true,
                            autocorrectAllowed = autocorrectAllowed,
                            enterLabel = getEnterLabel(info),
                            actionId = getActionId(info)
                        )
                    }
                }
            }

            else -> {
                val hintConfig = checkHintFallback(info)
                if (hintConfig != null) {
                    return hintConfig
                }

                return InputConfiguration(
                    detectedType = DetectedInputType.TEXT,
                    initialMode = KeyboardMode.QWERTY,
                    autoCapsType = AutoCapsType.SENTENCES,
                    suggestionsAllowed = true,
                    autocorrectAllowed = true,
                    enterLabel = getEnterLabel(info),
                    actionId = getActionId(info)
                )
            }
        }
    }

    /**
     * Checks field name, hint text, and label as a fallback heuristic
     * for apps or webviews that do not set explicit InputType variations.
     */
    private fun checkHintFallback(info: EditorInfo?): InputConfiguration? {
        if (info == null) return null
        val hint = info.hintText?.toString() ?: ""
        val field = info.fieldName ?: ""
        val label = info.label?.toString() ?: ""
        val text = "$hint $field $label"

        if (text.isBlank()) return null
        val normalized = text.lowercase().replace('_', ' ').replace('-', ' ')

        val isSearchOrMessage = normalized.contains("search") ||
            normalized.contains("find") ||
            normalized.contains("query") ||
            normalized.contains("message") ||
            normalized.contains("chat") ||
            normalized.contains("comment") ||
            normalized.contains("note") ||
            normalized.contains("post") ||
            normalized.contains("status")

        if (isSearchOrMessage) {
            return null
        }

        val enterLabel = getEnterLabel(info)
        val actionId = getActionId(info)

        if (EMAIL_REGEX.containsMatchIn(normalized)) {
            return InputConfiguration(
                detectedType = DetectedInputType.EMAIL,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = true,
                autocorrectAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (PHONE_REGEX.containsMatchIn(normalized)) {
            return InputConfiguration(
                detectedType = DetectedInputType.PHONE,
                initialMode = KeyboardMode.NUMPAD,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                autocorrectAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (URL_REGEX.containsMatchIn(normalized)) {
            return InputConfiguration(
                detectedType = DetectedInputType.URI,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = true,
                autocorrectAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (PIN_REGEX.containsMatchIn(normalized)) {
            return InputConfiguration(
                detectedType = DetectedInputType.NUMBER_PASSWORD,
                initialMode = KeyboardMode.NUMPAD,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                autocorrectAllowed = false,
                enterLabel = enterLabel,
                actionId = actionId
            )
        }

        if (PASSWORD_REGEX.containsMatchIn(normalized)) {
            return InputConfiguration(
                detectedType = DetectedInputType.PASSWORD,
                initialMode = KeyboardMode.QWERTY,
                autoCapsType = AutoCapsType.NONE,
                suggestionsAllowed = false,
                autocorrectAllowed = false,
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
     * Detects if the editor represents a search field.
     */
    fun isSearchField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        if (action == EditorInfo.IME_ACTION_SEARCH) return true

        val maskVariation = info.inputType and InputType.TYPE_MASK_VARIATION
        if (maskVariation == InputType.TYPE_TEXT_VARIATION_FILTER) return true

        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"

        return text.contains("search") ||
            text.contains("find") ||
            text.contains("query") ||
            text.contains("explore")
    }

    /**
     * Detects if the editor represents a username or technical handle input.
     * Genuine restriction: usernames/handles must not be autocorrected or dictionary-suggested.
     */
    fun isUsernameField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"

        // Do not false positive on password fields or real full name fields
        if (text.contains("password") || text.contains("passcode")) return false
        if (text.contains("full name") || text.contains("display name") || text.contains("first name")) return false

        return USERNAME_REGEX.containsMatchIn(text) ||
            field.contains("username") ||
            field.contains("handle") ||
            hint.contains("username")
    }

    /**
     * Detects if the editor represents a person's display name or full name.
     */
    fun isPersonNameField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val maskVariation = info.inputType and InputType.TYPE_MASK_VARIATION
        if (maskVariation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME) return true

        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"

        if (isUsernameField(info) || isSearchField(info)) return false

        return text.contains("full name") ||
            text.contains("your name") ||
            text.contains("person name") ||
            text.contains("display name") ||
            text.contains("profile name") ||
            (text.contains("name") && !text.contains("file") && !text.contains("app"))
    }

    /**
     * Detects if the editor represents a bio or about field.
     */
    fun isBioField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"

        return text.contains("bio") ||
            text.contains("biography") ||
            text.contains("about you") ||
            text.contains("about yourself")
    }

    /**
     * Detects if the editor represents a comment or post caption field.
     */
    fun isCommentOrCaption(info: EditorInfo?): Boolean {
        if (info == null) return false
        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"

        return text.contains("comment") ||
            text.contains("caption") ||
            text.contains("reply") ||
            text.contains("add a comment") ||
            text.contains("write a caption") ||
            field.contains("comment") ||
            field.contains("caption")
    }

    /**
     * Detects if the current editor corresponds to a chat, messaging, or comment composer.
     */
    fun isMessagingOrChat(info: EditorInfo?): Boolean {
        if (info == null) return false
        val pkg = info.packageName?.lowercase() ?: ""
        if (pkg.contains("instagram") ||
            pkg.contains("whatsapp") ||
            pkg.contains("facebook.orca") || // Messenger
            pkg.contains("telegram") ||
            pkg.contains("discord") ||
            pkg.contains("snapchat") ||
            pkg.contains("twitter") ||
            pkg.contains("threads") ||
            pkg.contains("messaging") ||
            pkg.contains("mms") ||
            pkg.contains("signal") ||
            pkg.contains("viber") ||
            pkg.contains("wechat") ||
            pkg.contains("line")
        ) {
            return true
        }
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        if (action == EditorInfo.IME_ACTION_SEND) {
            return true
        }
        val hint = info.hintText?.toString()?.lowercase() ?: ""
        val field = info.fieldName?.lowercase() ?: ""
        val label = info.label?.toString()?.lowercase() ?: ""
        val text = "$hint $field $label"
        return text.contains("message") ||
            text.contains("chat") ||
            text.contains("reply") ||
            text.contains("comment") ||
            text.contains("composer") ||
            text.contains("direct") ||
            text.contains("send") ||
            text.contains("status") ||
            text.contains("post") ||
            text.contains("dm") ||
            text.contains("conversation") ||
            text.contains("thread")
    }

    /**
     * Strictly verifies whether an EditorInfo corresponds to a genuine, sensitive password field.
     * Prevents chat composers (like Instagram Direct) from being falsely categorized as passwords.
     */
    fun isRealPasswordField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val inputType = info.inputType
        val maskClass = inputType and InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and InputType.TYPE_MASK_VARIATION

        val isNumberPassword = maskClass == InputType.TYPE_CLASS_NUMBER &&
            maskVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        if (isNumberPassword) return true

        val isHiddenTextPassword = maskClass == InputType.TYPE_CLASS_TEXT && (
            maskVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            maskVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )

        if (isMessagingOrChat(info)) {
            // For chat apps (like Instagram DMs), only treat as password if it strictly has a password keyword in field/hint
            val hint = info.hintText?.toString()?.lowercase() ?: ""
            val field = info.fieldName?.lowercase() ?: ""
            val text = "$hint $field"
            val hasPasswordKeyword = text.contains("password") || text.contains("passcode")
            return isHiddenTextPassword && hasPasswordKeyword
        }

        val isVisiblePassword = maskClass == InputType.TYPE_CLASS_TEXT &&
            maskVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        return isHiddenTextPassword || isVisiblePassword
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

    /**
     * Checks if the EditorInfo represents a password field, taking application context into account.
     */
    fun isPasswordField(info: EditorInfo?): Boolean {
        return isRealPasswordField(info)
    }
}