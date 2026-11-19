package com.clicksy.keyboard.util

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.clicksy.keyboard.data.AutoCapsType
import com.clicksy.keyboard.data.DetectedInputType
import com.clicksy.keyboard.data.KeyboardMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTypeDetectorTest {

    @Test
    fun testNullOrEmptyInputType() {
        val configNull = InputTypeDetector.detect(null)
        assertEquals(DetectedInputType.TEXT, configNull.detectedType)
        assertEquals(KeyboardMode.QWERTY, configNull.initialMode)
        assertEquals(AutoCapsType.SENTENCES, configNull.autoCapsType)
        assertTrue(configNull.suggestionsAllowed)

        val infoTypeNull = EditorInfo().apply {
            inputType = InputType.TYPE_NULL
        }
        val configTypeNull = InputTypeDetector.detect(infoTypeNull)
        assertEquals(DetectedInputType.TEXT, configTypeNull.detectedType)
        assertEquals(KeyboardMode.QWERTY, configTypeNull.initialMode)
        assertEquals(AutoCapsType.SENTENCES, configTypeNull.autoCapsType)
        assertTrue(configTypeNull.suggestionsAllowed)
    }

    @Test
    fun testNumberInputTypes() {
        // Standard integer number
        val infoNumber = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val configNumber = InputTypeDetector.detect(infoNumber)
        assertEquals(DetectedInputType.NUMBER, configNumber.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configNumber.initialMode)
        assertEquals(AutoCapsType.NONE, configNumber.autoCapsType)
        assertFalse(configNumber.suggestionsAllowed)

        // Decimal number
        val infoDecimal = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val configDecimal = InputTypeDetector.detect(infoDecimal)
        assertEquals(DetectedInputType.NUMBER, configDecimal.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configDecimal.initialMode)
        assertEquals(AutoCapsType.NONE, configDecimal.autoCapsType)

        // Signed number
        val infoSigned = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val configSigned = InputTypeDetector.detect(infoSigned)
        assertEquals(DetectedInputType.NUMBER, configSigned.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configSigned.initialMode)

        // Number Password (PIN)
        val infoPin = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val configPin = InputTypeDetector.detect(infoPin)
        assertEquals(DetectedInputType.NUMBER_PASSWORD, configPin.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configPin.initialMode)
        assertEquals(AutoCapsType.NONE, configPin.autoCapsType)
        assertFalse(configPin.suggestionsAllowed)
        assertTrue(InputTypeDetector.isPasswordField(infoPin.inputType))
    }

    @Test
    fun testPhoneInputType() {
        val infoPhone = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        val configPhone = InputTypeDetector.detect(infoPhone)
        assertEquals(DetectedInputType.PHONE, configPhone.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configPhone.initialMode)
        assertEquals(AutoCapsType.NONE, configPhone.autoCapsType)
        assertFalse(configPhone.suggestionsAllowed)
    }

    @Test
    fun testDateTimeInputTypes() {
        val infoDateTime = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_DATETIME
        }
        val configDateTime = InputTypeDetector.detect(infoDateTime)
        assertEquals(DetectedInputType.DATETIME, configDateTime.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configDateTime.initialMode)
        assertEquals(AutoCapsType.NONE, configDateTime.autoCapsType)

        val infoDate = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
        }
        assertEquals(DetectedInputType.DATETIME, InputTypeDetector.detect(infoDate).detectedType)

        val infoTime = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
        }
        assertEquals(DetectedInputType.DATETIME, InputTypeDetector.detect(infoTime).detectedType)
    }

    @Test
    fun testEmailInputTypes() {
        val infoEmail = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val configEmail = InputTypeDetector.detect(infoEmail)
        assertEquals(DetectedInputType.EMAIL, configEmail.detectedType)
        assertEquals(KeyboardMode.QWERTY, configEmail.initialMode)
        assertEquals(AutoCapsType.NONE, configEmail.autoCapsType)
        assertTrue(configEmail.suggestionsAllowed)

        val infoWebEmail = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        }
        val configWebEmail = InputTypeDetector.detect(infoWebEmail)
        assertEquals(DetectedInputType.EMAIL, configWebEmail.detectedType)
        assertEquals(KeyboardMode.QWERTY, configWebEmail.initialMode)
        assertEquals(AutoCapsType.NONE, configWebEmail.autoCapsType)
    }

    @Test
    fun testUriInputType() {
        val infoUri = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        val configUri = InputTypeDetector.detect(infoUri)
        assertEquals(DetectedInputType.URI, configUri.detectedType)
        assertEquals(KeyboardMode.QWERTY, configUri.initialMode)
        assertEquals(AutoCapsType.NONE, configUri.autoCapsType)
        assertTrue(configUri.suggestionsAllowed)
    }

    @Test
    fun testPasswordInputTypes() {
        val infoTextPassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val configPassword = InputTypeDetector.detect(infoTextPassword)
        assertEquals(DetectedInputType.PASSWORD, configPassword.detectedType)
        assertEquals(KeyboardMode.QWERTY, configPassword.initialMode)
        assertEquals(AutoCapsType.NONE, configPassword.autoCapsType)
        assertFalse(configPassword.suggestionsAllowed)
        assertTrue(InputTypeDetector.isPasswordField(infoTextPassword.inputType))

        val infoVisiblePassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        val configVisiblePassword = InputTypeDetector.detect(infoVisiblePassword)
        assertEquals(DetectedInputType.PASSWORD, configVisiblePassword.detectedType)
        assertFalse(configVisiblePassword.suggestionsAllowed)
        assertTrue(InputTypeDetector.isPasswordField(infoVisiblePassword.inputType))

        val infoWebPassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        val configWebPassword = InputTypeDetector.detect(infoWebPassword)
        assertEquals(DetectedInputType.PASSWORD, configWebPassword.detectedType)
        assertFalse(configWebPassword.suggestionsAllowed)
        assertTrue(InputTypeDetector.isPasswordField(infoWebPassword.inputType))
    }

    @Test
    fun testAutoCapitalizationFlags() {
        // Standard sentence caps
        val infoSentences = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val configSentences = InputTypeDetector.detect(infoSentences)
        assertEquals(AutoCapsType.SENTENCES, configSentences.autoCapsType)

        // All characters (CAPS_LOCK)
        val infoCaps = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        val configCaps = InputTypeDetector.detect(infoCaps)
        assertEquals(AutoCapsType.CHARACTERS, configCaps.autoCapsType)

        // Words capitalization
        val infoWords = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val configWords = InputTypeDetector.detect(infoWords)
        assertEquals(AutoCapsType.WORDS, configWords.autoCapsType)

        // Person Name default to WORDS
        val infoName = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }
        val configName = InputTypeDetector.detect(infoName)
        assertEquals(AutoCapsType.WORDS, configName.autoCapsType)
    }

    @Test
    fun testNoSuggestionsFlag() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        val config = InputTypeDetector.detect(info)
        assertTrue(config.suggestionsAllowed)
    }

    @Test
    fun testHintFallbackDoesNotFalsePositiveOnSubstrings() {
        // "shopping", "opinion", "shipping" should not trigger PIN
        val infoShopping = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Shopping cart search"
        }
        val configShopping = InputTypeDetector.detect(infoShopping)
        assertEquals(DetectedInputType.TEXT, configShopping.detectedType)
        assertTrue(configShopping.suggestionsAllowed)

        val infoOpinion = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Give your opinion"
        }
        val configOpinion = InputTypeDetector.detect(infoOpinion)
        assertEquals(DetectedInputType.TEXT, configOpinion.detectedType)
        assertTrue(configOpinion.suggestionsAllowed)
    }

    @Test
    fun testEnterActionLabelsAndIds() {
        val infoSearch = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertEquals("🔍", InputTypeDetector.getEnterLabel(infoSearch))
        assertEquals(EditorInfo.IME_ACTION_SEARCH, InputTypeDetector.getActionId(infoSearch))

        val infoSend = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        assertEquals("Send", InputTypeDetector.getEnterLabel(infoSend))
        assertEquals(EditorInfo.IME_ACTION_SEND, InputTypeDetector.getActionId(infoSend))

        val infoGo = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_GO
        }
        assertEquals("Go", InputTypeDetector.getEnterLabel(infoGo))
        assertEquals(EditorInfo.IME_ACTION_GO, InputTypeDetector.getActionId(infoGo))

        val infoDone = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        assertEquals("Done", InputTypeDetector.getEnterLabel(infoDone))
        assertEquals(EditorInfo.IME_ACTION_DONE, InputTypeDetector.getActionId(infoDone))

        val infoNext = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        assertEquals("→", InputTypeDetector.getEnterLabel(infoNext))
        assertEquals(EditorInfo.IME_ACTION_NEXT, InputTypeDetector.getActionId(infoNext))

        // Custom action label
        val infoCustom = EditorInfo().apply {
            actionLabel = "Submit"
            actionId = 42
        }
        assertEquals("Submit", InputTypeDetector.getEnterLabel(infoCustom))
        assertEquals(42, InputTypeDetector.getActionId(infoCustom))

        // IME_FLAG_NO_ENTER_ACTION suppresses action
        val infoNoEnter = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }
        assertEquals(EditorInfo.IME_ACTION_UNSPECIFIED, InputTypeDetector.getActionId(infoNoEnter))
    }

    @Test
    fun testHintFallbackHeuristics() {
        // App using standard text inputType but with "email" in hint
        val infoEmailHint = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Enter your email address"
        }
        val configEmail = InputTypeDetector.detect(infoEmailHint)
        assertEquals(DetectedInputType.EMAIL, configEmail.detectedType)
        assertEquals(AutoCapsType.NONE, configEmail.autoCapsType)

        // App using standard text inputType but with "phone" in hint
        val infoPhoneHint = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Mobile phone number"
        }
        val configPhone = InputTypeDetector.detect(infoPhoneHint)
        assertEquals(DetectedInputType.PHONE, configPhone.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configPhone.initialMode)

        // App with "PIN" in hint
        val infoPinHint = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Enter 6-digit PIN"
        }
        val configPin = InputTypeDetector.detect(infoPinHint)
        assertEquals(DetectedInputType.NUMBER_PASSWORD, configPin.detectedType)
        assertEquals(KeyboardMode.NUMPAD, configPin.initialMode)

        // App with "website" in fieldName
        val infoUrlField = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            fieldName = "website_url"
        }
        val configUrl = InputTypeDetector.detect(infoUrlField)
        assertEquals(DetectedInputType.URI, configUrl.detectedType)
        assertEquals(AutoCapsType.NONE, configUrl.autoCapsType)

        // Search bar with phone/email in hint should NOT be hijacked to NUMPAD or EMAIL
        val infoSearchHint = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Search by name, phone or email"
        }
        val configSearch = InputTypeDetector.detect(infoSearchHint)
        assertEquals(DetectedInputType.TEXT, configSearch.detectedType)
        assertEquals(KeyboardMode.QWERTY, configSearch.initialMode)
        assertTrue(configSearch.suggestionsAllowed)
    }

    @Test
    fun testInstagramDirectChatDetection() {
        // Standard Instagram Direct message composer
        val infoInstagramChat = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hintText = "Message..."
            fieldName = "row_thread_composer_edittext"
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val configInstagram = InputTypeDetector.detect(infoInstagramChat)
        assertEquals(DetectedInputType.TEXT, configInstagram.detectedType)
        assertEquals(KeyboardMode.QWERTY, configInstagram.initialMode)
        assertTrue(configInstagram.suggestionsAllowed)
        assertFalse(InputTypeDetector.isPasswordField(infoInstagramChat))
        assertEquals("Send", configInstagram.enterLabel)

        // Instagram Direct Chat with TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or NO_SUGGESTIONS
        val infoInstagramVisiblePassword = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            hintText = "Send a message..."
            fieldName = "direct_thread_edit_text"
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val configVisiblePassword = InputTypeDetector.detect(infoInstagramVisiblePassword)
        assertEquals(DetectedInputType.TEXT, configVisiblePassword.detectedType)
        assertEquals(KeyboardMode.QWERTY, configVisiblePassword.initialMode)
        assertTrue(configVisiblePassword.suggestionsAllowed)
        assertFalse(InputTypeDetector.isPasswordField(infoInstagramVisiblePassword))

        // Instagram Login Password Screen should still be recognized as password
        val infoInstagramLogin = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hintText = "Password"
            fieldName = "password"
        }
        val configInstagramLogin = InputTypeDetector.detect(infoInstagramLogin)
        assertEquals(DetectedInputType.PASSWORD, configInstagramLogin.detectedType)
        assertFalse(configInstagramLogin.suggestionsAllowed)
        assertTrue(InputTypeDetector.isPasswordField(infoInstagramLogin))

        // WhatsApp and Messenger chat inputs
        val infoWhatsApp = EditorInfo().apply {
            packageName = "com.whatsapp"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hintText = "Type a message"
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val configWhatsApp = InputTypeDetector.detect(infoWhatsApp)
        assertEquals(DetectedInputType.TEXT, configWhatsApp.detectedType)
        assertTrue(configWhatsApp.suggestionsAllowed)
        assertTrue(configWhatsApp.autocorrectAllowed)
        assertFalse(InputTypeDetector.isPasswordField(infoWhatsApp))
    }

    @Test
    fun testInstagramPostAndReelComments() {
        val infoComment = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hintText = "Add a comment..."
            fieldName = "layout_comment_thread_edittext"
        }
        val config = InputTypeDetector.detect(infoComment)
        assertEquals(DetectedInputType.TEXT, config.detectedType)
        assertTrue(config.suggestionsAllowed)
        assertTrue(config.autocorrectAllowed)
        assertEquals(AutoCapsType.SENTENCES, config.autoCapsType)
    }

    @Test
    fun testInstagramStoryRepliesAndCaptions() {
        // Caption
        val infoCaption = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hintText = "Write a caption..."
            fieldName = "caption_text_view"
        }
        val configCaption = InputTypeDetector.detect(infoCaption)
        assertTrue(configCaption.suggestionsAllowed)
        assertTrue(configCaption.autocorrectAllowed)
        assertEquals(AutoCapsType.SENTENCES, configCaption.autoCapsType)

        // Story reply
        val infoStoryReply = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            hintText = "Send message"
            fieldName = "reel_viewer_composer"
            imeOptions = EditorInfo.IME_ACTION_SEND
        }
        val configStoryReply = InputTypeDetector.detect(infoStoryReply)
        assertTrue(configStoryReply.suggestionsAllowed)
        assertTrue(configStoryReply.autocorrectAllowed)
        assertEquals("Send", configStoryReply.enterLabel)
    }

    @Test
    fun testInstagramSearchField() {
        val infoSearch = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "Search"
            fieldName = "action_bar_search_edit_text"
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        val config = InputTypeDetector.detect(infoSearch)
        assertTrue(config.suggestionsAllowed)
        assertFalse(config.autocorrectAllowed)
        assertEquals("🔍", config.enterLabel)
        assertEquals(EditorInfo.IME_ACTION_SEARCH, config.actionId)
    }

    @Test
    fun testInstagramProfileFields() {
        // Profile Name (e.g. John Doe)
        val infoName = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            hintText = "Name"
            fieldName = "full_name"
        }
        val configName = InputTypeDetector.detect(infoName)
        assertTrue(configName.suggestionsAllowed)
        assertFalse(configName.autocorrectAllowed)
        assertEquals(AutoCapsType.WORDS, configName.autoCapsType)

        // Profile Username Handle (e.g. @john_doe_99)
        // Genuine restriction: technical handle must not be auto-corrected or suggested
        val infoUsername = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            hintText = "Username"
            fieldName = "username_field"
        }
        val configUsername = InputTypeDetector.detect(infoUsername)
        assertFalse(configUsername.suggestionsAllowed)
        assertFalse(configUsername.autocorrectAllowed)
        assertEquals(AutoCapsType.NONE, configUsername.autoCapsType)

        // Profile Bio (natural language multiline)
        val infoBio = EditorInfo().apply {
            packageName = "com.instagram.android"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hintText = "Bio"
            fieldName = "biography"
        }
        val configBio = InputTypeDetector.detect(infoBio)
        assertTrue(configBio.suggestionsAllowed)
        assertTrue(configBio.autocorrectAllowed)
        assertEquals(AutoCapsType.SENTENCES, configBio.autoCapsType)
    }

    @Test
    fun testNonInstagramOrdinaryFieldsPreserved() {
        // Standard note app body
        val infoNote = EditorInfo().apply {
            packageName = "com.google.android.keep"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hintText = "Note"
        }
        val configNote = InputTypeDetector.detect(infoNote)
        assertEquals(DetectedInputType.TEXT, configNote.detectedType)
        assertTrue(configNote.suggestionsAllowed)
        assertTrue(configNote.autocorrectAllowed)
        assertEquals(AutoCapsType.SENTENCES, configNote.autoCapsType)

        // Web search in Chrome
        val infoChrome = EditorInfo().apply {
            packageName = "com.android.chrome"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        val configChrome = InputTypeDetector.detect(infoChrome)
        assertEquals(DetectedInputType.URI, configChrome.detectedType)
        assertTrue(configChrome.suggestionsAllowed)
        assertFalse(configChrome.autocorrectAllowed)
    }
}