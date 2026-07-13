package com.clicksy.keyboard.data

import android.content.ClipDescription
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.inputmethod.EditorInfo
import com.clicksy.keyboard.service.ClicksyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service that handles listening for clipboard changes, filtering out sensitive
 * content (passwords, OTPs, etc.), checking user settings, and saving copies to the database.
 */
class ClipboardManagerService(
    private val context: Context,
    private val repository: ClipboardRepository
) {
    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    private val prefs: SharedPreferences = context.getSharedPreferences(ClicksyService.PREFS_NAME, Context.MODE_PRIVATE)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activeEditorInfo: EditorInfo? = null

    companion object {
        const val KEY_CLIPBOARD_HISTORY_ENABLED = "clipboard_history_enabled"
    }

    private val clipChangedListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        processCurrentClipboard()
    }

    fun startListening() {
        try {
            systemClipboard.addPrimaryClipChangedListener(clipChangedListener)
        } catch (e: Exception) {
            // Ignore listener registration errors
        }
    }

    fun stopListening() {
        try {
            systemClipboard.removePrimaryClipChangedListener(clipChangedListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun updateActiveEditorInfo(editorInfo: EditorInfo?) {
        activeEditorInfo = editorInfo
    }

    fun isHistoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_CLIPBOARD_HISTORY_ENABLED, true)
    }

    fun processCurrentClipboard() {
        if (!isHistoryEnabled()) return

        // 1. If currently in a password field, do not record copies
        val activeInputType = activeEditorInfo?.inputType ?: 0
        if (isPasswordField(activeInputType)) {
            return
        }

        try {
            val clipData = systemClipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val text = item.text?.toString() ?: ""
                
                if (text.isBlank()) return

                // 2. Check Android standard sensitive content
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    val description = clipData.description
                    if (description != null) {
                        val isSensitive = description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true ||
                                          description.extras?.getBoolean("org.chromium.chrome.extra.IS_SENSITIVE", false) == true
                        if (isSensitive) return
                    }
                }

                // 3. Custom heuristics for OTPs / verification codes
                if (isOtpOrVerificationCode(text)) {
                    return
                }

                // 4. Save to database using repository
                serviceScope.launch {
                    repository.addCopy(text)
                }
            }
        } catch (e: SecurityException) {
            // Ignore SecurityExceptions when background IME has no clipboard focus
        } catch (e: Exception) {
            // Ignore other clipboard retrieval errors
        }
    }

    private fun isPasswordField(inputType: Int): Boolean {
        val maskClass = inputType and android.text.InputType.TYPE_MASK_CLASS
        val maskVariation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        
        val isTextPassword = maskClass == android.text.InputType.TYPE_CLASS_TEXT && (
            maskVariation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            maskVariation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            maskVariation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )
        val isNumberPassword = maskClass == android.text.InputType.TYPE_CLASS_NUMBER &&
            maskVariation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            
        return isTextPassword || isNumberPassword
    }

    private fun isOtpOrVerificationCode(text: String): Boolean {
        // Match numbers only, 4 to 8 digits (commonly OTPs)
        val numericOtpPattern = Regex("^\\d{4,8}$")
        if (numericOtpPattern.matches(text.trim())) {
            return true
        }

        // Keywords indicating OTP, verification code, etc.
        val lower = text.lowercase()
        val otpKeywords = listOf("otp", "verification code", "security code", "passcode", "one-time", "auth code")
        if (text.length < 50 && otpKeywords.any { lower.contains(it) }) {
            return true
        }

        return false
    }
}
