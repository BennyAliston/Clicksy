package com.clicksy.keyboard.service

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Intent
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.clicksy.keyboard.data.ClipboardDatabase
import com.clicksy.keyboard.data.ClipboardRepository
import com.clicksy.keyboard.data.ClipboardManagerService
import com.clicksy.keyboard.ui.keyboard.ClipboardViewModel
import com.clicksy.keyboard.settings.SettingsActivity
import com.clicksy.keyboard.data.NeuTheme
import com.clicksy.keyboard.data.KeyboardSize
import com.clicksy.keyboard.data.ShiftState
import com.clicksy.keyboard.data.KeyboardMode
import com.clicksy.keyboard.data.DetectedInputType
import com.clicksy.keyboard.data.AutoCapsType
import com.clicksy.keyboard.data.DictionaryProvider
import com.clicksy.keyboard.ui.keyboard.KeyboardScreen
import com.clicksy.keyboard.ui.theme.ClicksyTheme
import com.clicksy.keyboard.ui.theme.ClicksyDimensions
import com.clicksy.keyboard.ui.theme.toColorScheme
import com.clicksy.keyboard.util.HapticFeedbackManager
import com.clicksy.keyboard.util.SpeechRecognizerHelper
import com.clicksy.keyboard.util.SoundFeedbackManager
import com.clicksy.keyboard.util.SoundType
import com.clicksy.keyboard.util.InputConfiguration
import com.clicksy.keyboard.util.InputTypeDetector

private val AUTO_CAPS_REGEX = Regex("(?:[\\.!?]|\\n)\\s+$")
private val WORD_SPLIT_REGEX = Regex("[\\s.,;:!?()\\[\\]{}\"<>/\\\\=+\\-*&^%$#@~`|]+")

/**
 * The core InputMethodService for Clicksy.
 *
 * Hosts the Compose-based keyboard UI within the IME framework.
 * Manually implements LifecycleOwner, ViewModelStoreOwner, and
 * SavedStateRegistryOwner to support Jetpack Compose in a Service context.
 */
class ClicksyService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {


    // --- Lifecycle plumbing for Compose ---
    private val _lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = _lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val _savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = _savedStateRegistryController.savedStateRegistry

    // --- Services ---
    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var speechHelper: SpeechRecognizerHelper
    private lateinit var soundManager: SoundFeedbackManager
    private lateinit var clipboardManagerService: ClipboardManagerService
    private val clipboardViewModel: ClipboardViewModel by lazy {
        val database = ClipboardDatabase.getDatabase(this)
        val repository = ClipboardRepository(database.clipboardDao())
        ViewModelProvider(this, ClipboardViewModel.Factory(repository))[ClipboardViewModel::class.java]
    }
    private lateinit var prefs: SharedPreferences

    // --- Reactive state ---
    private var currentTheme by mutableStateOf(NeuTheme.SUNSHINE)
    private var keyboardSize by mutableStateOf(KeyboardSize.MEDIUM)
    private var hapticEnabled by mutableStateOf(true)
    private var autocompleteEnabled by mutableStateOf(true)
    private var autoCapitalizationEnabled by mutableStateOf(true)
    private var showNumberRow by mutableStateOf(false)
    private var roundedKeysEnabled by mutableStateOf(false)
    private var soundType by mutableStateOf(SoundType.MUTE)
    private var activePackageName by mutableStateOf("com.clicksy.keyboard")
    private var shiftState by mutableStateOf(ShiftState.OFF)
    private var keyboardMode by mutableStateOf(KeyboardMode.QWERTY)
    private var detectedInputType by mutableStateOf(DetectedInputType.TEXT)
    private var suggestionsAllowed by mutableStateOf(true)
    private var currentInputConfig by mutableStateOf(InputConfiguration())
    private val recentEmojis = mutableStateListOf<String>()
    private var enterLabel by mutableStateOf("↵")
    private var lastAutoCorrectedOriginal: String? = null
    private var lastAutoCorrectedReplacement: String? = null
    private var lastSpaceTapTime = 0L
    private var isDeviceDarkTheme by mutableStateOf(false)

    private lateinit var shiftStateManager: ShiftStateManager

    private var clipChangedListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        loadPreferences()
    }

    companion object {
        const val PREFS_NAME = "clicksy_prefs"
        const val KEY_THEME = "theme"
        const val KEY_KEYBOARD_SIZE = "keyboard_size"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_AUTOCOMPLETE = "autocomplete_enabled"
        const val KEY_AUTO_CAPITALIZATION = "auto_capitalization_enabled"
        const val KEY_SHOW_NUMBER_ROW = "show_number_row"
        const val KEY_SOUND_TYPE = "sound_type"
        const val KEY_RECENT_EMOJIS = "recent_emojis"
        const val KEY_ROUNDED_KEYS = "rounded_keys_enabled"
    }

    override fun onCreate() {
        super.onCreate()
        _savedStateRegistryController.performRestore(null)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        hapticManager = HapticFeedbackManager(this)
        speechHelper = SpeechRecognizerHelper(this)
        soundManager = SoundFeedbackManager(this)
        
        val database = ClipboardDatabase.getDatabase(this)
        val repository = ClipboardRepository(database.clipboardDao())
        clipboardManagerService = ClipboardManagerService(this, repository)
        clipboardManagerService.startListening()
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        loadPreferences()
        isDeviceDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        shiftStateManager = ShiftStateManager { newState ->
            shiftState = newState
        }
        loadRecentEmojis()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            DictionaryProvider.initialize(this@ClicksyService)
        }
    }

    override fun onCreateInputView(): View {
        // Ensure owners are set on the decor view BEFORE ComposeView is attached to prevent IllegalStateException
        // and allow WindowInsets to propagate to the view tree by disabling decor fits system windows
        window?.window?.let { win ->
            win.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
            val decorView = win.decorView
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            // Set layout parameters to ensure it matches parent width and wraps content height
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Keep the composition alive while the IME Service lives so reopening the keyboard has 0ms delay
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@ClicksyService))

            // Wire up lifecycle owners for Compose
            // CRITICAL: Must be set before setContent
            setViewTreeLifecycleOwner(this@ClicksyService)
            setViewTreeViewModelStoreOwner(this@ClicksyService)
            setViewTreeSavedStateRegistryOwner(this@ClicksyService)

            setContent {
                val dimensions = remember(keyboardSize, roundedKeysEnabled) {
                    val defaultDims = ClicksyDimensions(
                        keyCornerRadius = if (roundedKeysEnabled) 14.dp else 8.dp
                    )
                    when (keyboardSize) {
                        KeyboardSize.SMALL -> defaultDims.copy(keyHeight = 42.dp)
                        KeyboardSize.MEDIUM -> defaultDims.copy(keyHeight = 50.dp)
                        KeyboardSize.LARGE -> defaultDims.copy(keyHeight = 58.dp)
                        KeyboardSize.EXTRA_LARGE -> defaultDims.copy(keyHeight = 65.dp)
                    }
                }
                ClicksyTheme(
                    colorScheme = currentTheme.toColorScheme(activePackageName = activePackageName, isDark = isDeviceDarkTheme),
                    dimensions = dimensions
                ) {
                    KeyboardScreen(
                        recentEmojis = recentEmojis,
                        onEmojiUsed = { emoji ->
                            recentEmojis.remove(emoji)
                            recentEmojis.add(0, emoji)
                            if (recentEmojis.size > 30) {
                                recentEmojis.removeRange(30, recentEmojis.size)
                            }
                            saveRecentEmojis()
                        },
                        mode = keyboardMode,
                        onModeChange = { keyboardMode = it },
                        shiftState = shiftState,
                        onShiftToggle = {
                            shiftStateManager.handleShiftTap()
                        },
                        onShiftLock = {
                            shiftStateManager.handleShiftLongPress()
                        },
                        enterLabel = enterLabel,
                        clipboardViewModel = clipboardViewModel,
                        onLaunchSettings = {
                            val intent = Intent(this@ClicksyService, SettingsActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },
                        autocompleteEnabled = autocompleteEnabled,
                        keyboardSize = keyboardSize,
                        showNumberRow = showNumberRow,
                        detectedInputType = detectedInputType,
                        suggestionsAllowed = suggestionsAllowed,
                        onTextInput = { text ->
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            val ic = currentInputConnection

                            // Smart Punctuation: if user types a punctuation mark immediately after a space, snap it: "hello " + "." -> "hello. "
                            // Only apply in standard text mode
                            if (detectedInputType == DetectedInputType.TEXT) {
                                if (text.length == 1 && (text == "." || text == "," || text == "!" || text == "?" || text == ";" || text == ":")) {
                                    val textBefore = ic?.getTextBeforeCursor(1, 0)?.toString() ?: ""
                                    if (textBefore == " ") {
                                        ic?.deleteSurroundingText(1, 0)
                                        ic?.commitText("$text ", 1)
                                        if (text == "." || text == "!" || text == "?") {
                                            shiftStateManager.forceState(ShiftState.ONCE)
                                        }
                                        return@KeyboardScreen
                                    }
                                }
                            }

                            // If it's a suggestion selection, it ends with a space (only learn for text mode)
                            if (text.endsWith(" ") && suggestionsAllowed && detectedInputType == DetectedInputType.TEXT) {
                                val word = text.trim()
                                if (word.isNotEmpty()) {
                                    DictionaryProvider.learnWord(this@ClicksyService, word)
                                }
                            }
                            ic?.commitText(text, 1)
                            shiftStateManager.handleCharacterInput(text)
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onDeleteBackward = {
                            val ic = currentInputConnection
                            if (ic != null) {
                                // Check if we should undo the previous auto-correction
                                val orig = lastAutoCorrectedOriginal
                                val repl = lastAutoCorrectedReplacement
                                if (orig != null && repl != null) {
                                    val textBefore = ic.getTextBeforeCursor(repl.length + 1, 0)?.toString() ?: ""
                                    if (textBefore == "$repl ") {
                                        ic.deleteSurroundingText(repl.length + 1, 0)
                                        ic.commitText(orig, 1)
                                        lastAutoCorrectedOriginal = null
                                        lastAutoCorrectedReplacement = null
                                        shiftStateManager.resetDoubleTapTimer()
                                        return@KeyboardScreen
                                    }
                                }
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null

                                val selectedText = ic.getSelectedText(0)
                                if (!selectedText.isNullOrEmpty()) {
                                    ic.commitText("", 1)
                                } else {
                                    val beforeText = ic.getTextBeforeCursor(2, 0)
                                    if (beforeText != null && beforeText.length >= 2 &&
                                        Character.isHighSurrogate(beforeText[beforeText.length - 2]) &&
                                        Character.isLowSurrogate(beforeText[beforeText.length - 1])
                                    ) {
                                        ic.deleteSurroundingText(2, 0)
                                    } else {
                                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                                    }
                                }
                            }
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onEnterAction = {
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            if (suggestionsAllowed && detectedInputType == DetectedInputType.TEXT) {
                                learnLastTypedWord()
                            }
                            handleEnterAction()
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onSpace = {
                            val currentTime = System.currentTimeMillis()
                            val delta = currentTime - lastSpaceTapTime
                            val ic = currentInputConnection
                            val textBefore = ic?.getTextBeforeCursor(2, 0)?.toString() ?: ""

                            // Double-tap Space shortcut: converts "  " into ". " and triggers Auto-Caps (only for standard text)
                            if (detectedInputType == DetectedInputType.TEXT && delta in 60..500 && textBefore.endsWith(" ") && !textBefore.endsWith(". ")) {
                                ic?.deleteSurroundingText(1, 0)
                                ic?.commitText(". ", 1)
                                lastSpaceTapTime = 0L
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null
                                shiftStateManager.forceState(ShiftState.ONCE)
                                return@KeyboardScreen
                            }
                            lastSpaceTapTime = currentTime

                            val currentWord = getCurrentWordBeforeCursor()
                            val prevWord = getPreviousWordBeforeCursor()

                            if (autocompleteEnabled && suggestionsAllowed && detectedInputType == DetectedInputType.TEXT && currentWord.isNotBlank()) {
                                val correction = DictionaryProvider.getAutoCorrection(currentWord, prevWord)
                                if (correction != null && !correction.equals(currentWord, ignoreCase = false)) {
                                    if (ic != null) {
                                        ic.deleteSurroundingText(currentWord.length, 0)
                                        ic.commitText("$correction ", 1)
                                        lastAutoCorrectedOriginal = currentWord
                                        lastAutoCorrectedReplacement = correction
                                        DictionaryProvider.learnWord(this@ClicksyService, correction)
                                        if (prevWord.isNotBlank()) {
                                            DictionaryProvider.learnWordSequence(this@ClicksyService, prevWord, correction)
                                        }
                                    }
                                } else {
                                    learnLastTypedWord()
                                    ic?.commitText(" ", 1)
                                    lastAutoCorrectedOriginal = null
                                    lastAutoCorrectedReplacement = null
                                }
                            } else {
                                if (suggestionsAllowed && detectedInputType == DetectedInputType.TEXT) {
                                    learnLastTypedWord()
                                }
                                ic?.commitText(" ", 1)
                                lastAutoCorrectedOriginal = null
                                lastAutoCorrectedReplacement = null
                            }
                            shiftStateManager.resetDoubleTapTimer()
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        onRequestHaptic = {
                            if (hapticEnabled) {
                                hapticManager.performKeyTick()
                            }
                            soundManager.playSound(soundType)
                        },
                        onVoiceInput = {
                            startVoiceInput()
                        },
                        onSuggestionSelected = { suggestion ->
                            val ic = currentInputConnection
                            if (ic != null) {
                                val word = getCurrentWordBeforeCursor()
                                val prevWord = getPreviousWordBeforeCursor()
                                val cleaned = suggestion.removeSurrounding("\"")

                                if (detectedInputType == DetectedInputType.EMAIL) {
                                    if (cleaned.startsWith("@")) {
                                        if (word.contains("@")) {
                                            val username = word.substringBefore("@")
                                            ic.deleteSurroundingText(word.length, 0)
                                            ic.commitText("$username$cleaned", 1)
                                        } else {
                                            ic.commitText(cleaned, 1)
                                        }
                                    } else {
                                        if (word.isNotEmpty()) {
                                            ic.deleteSurroundingText(word.length, 0)
                                        }
                                        ic.commitText(cleaned, 1)
                                    }
                                } else if (detectedInputType == DetectedInputType.URI) {
                                    if (word.isNotEmpty() && cleaned.startsWith(".")) {
                                        ic.commitText(cleaned, 1)
                                    } else {
                                        if (word.isNotEmpty()) {
                                            ic.deleteSurroundingText(word.length, 0)
                                        }
                                        ic.commitText(cleaned, 1)
                                    }
                                } else {
                                    if (word.isNotEmpty()) {
                                        ic.deleteSurroundingText(word.length, 0)
                                    }
                                    ic.commitText("$cleaned ", 1)
                                    if (suggestionsAllowed && detectedInputType == DetectedInputType.TEXT) {
                                        DictionaryProvider.learnWord(this@ClicksyService, cleaned)
                                        if (prevWord.isNotBlank()) {
                                            DictionaryProvider.learnWordSequence(this@ClicksyService, prevWord, cleaned)
                                        }
                                    }
                                }
                            }
                            lastAutoCorrectedOriginal = null
                            lastAutoCorrectedReplacement = null
                            shiftStateManager.handleCharacterInput(" ")
                            if (shiftState == ShiftState.OFF) {
                                checkAutoCaps()
                            }
                        },
                        getCurrentWord = {
                            getCurrentWordBeforeCursor()
                        },
                        getPreviousWord = {
                            getPreviousWordBeforeCursor()
                        },
                        getCursorContext = ::getCursorContext
                    )
                }
            }
        }

        return composeView
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Ensure owners are set on the decor view so they are found during traversal
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.updateActiveEditorInfo(attribute)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadPreferences()
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isDeviceDarkTheme != nightMode) {
            isDeviceDarkTheme = nightMode
        }
        activePackageName = info?.packageName ?: "com.clicksy.keyboard"

        currentInputConfig = InputTypeDetector.detect(info)

        detectedInputType = currentInputConfig.detectedType
        suggestionsAllowed = currentInputConfig.suggestionsAllowed
        enterLabel = currentInputConfig.enterLabel

        if (!restarting || keyboardMode == KeyboardMode.QWERTY || keyboardMode == KeyboardMode.NUMPAD) {
            keyboardMode = currentInputConfig.initialMode
        }

        shiftState = ShiftState.OFF
        shiftStateManager.forceState(ShiftState.OFF)
        checkAutoCaps()

        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.updateActiveEditorInfo(info)
            clipboardManagerService.processCurrentClipboard()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val nightMode = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isDeviceDarkTheme != nightMode) {
            isDeviceDarkTheme = nightMode
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clipboardManagerService.isInitialized) {
            clipboardManagerService.stopListening()
        }
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
        if (_lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        speechHelper.destroy()
        _viewModelStore.clear()
    }

    // --- Private helpers ---

    private fun isPasswordField(inputType: Int): Boolean {
        return InputTypeDetector.isPasswordField(inputType)
    }

    private fun shouldAutoCapitalize(textBeforeCursor: String): Boolean {
        if (textBeforeCursor.isEmpty()) return true
        if (textBeforeCursor.endsWith("\n") || textBeforeCursor.endsWith("\r")) return true
        return AUTO_CAPS_REGEX.containsMatchIn(textBeforeCursor)
    }

    private fun checkAutoCaps() {
        val ic = currentInputConnection ?: return
        if (shiftState == ShiftState.CAPS_LOCK) return

        val info = currentInputEditorInfo
        val inputType = info?.inputType ?: 0

        if (inputType == 0 || isPasswordField(inputType)) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        if (!autoCapitalizationEnabled) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        val config = currentInputConfig
        if (config.autoCapsType == AutoCapsType.NONE) {
            shiftStateManager.forceState(ShiftState.OFF)
            return
        }

        if (config.autoCapsType == AutoCapsType.CHARACTERS) {
            shiftStateManager.forceState(ShiftState.CAPS_LOCK)
            return
        }

        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""

        if (config.autoCapsType == AutoCapsType.WORDS) {
            if (textBefore.isEmpty() || textBefore.last().isWhitespace() || textBefore.endsWith("\n")) {
                shiftStateManager.forceState(ShiftState.ONCE)
            } else {
                shiftStateManager.forceState(ShiftState.OFF)
            }
            return
        }

        // SENTENCES
        if (shouldAutoCapitalize(textBefore)) {
            shiftStateManager.forceState(ShiftState.ONCE)
        } else {
            shiftStateManager.forceState(ShiftState.OFF)
        }
    }

    private fun loadRecentEmojis() {
        val serialized = prefs.getString(KEY_RECENT_EMOJIS, "") ?: ""
        recentEmojis.clear()
        if (serialized.isNotEmpty()) {
            recentEmojis.addAll(serialized.split(","))
        }
    }

    private fun saveRecentEmojis() {
        val serialized = recentEmojis.joinToString(",")
        prefs.edit().putString(KEY_RECENT_EMOJIS, serialized).apply()
    }

    private fun loadPreferences() {
        val themeName = prefs.getString(KEY_THEME, NeuTheme.SUNSHINE.name)
        currentTheme = try {
            NeuTheme.valueOf(themeName ?: NeuTheme.SUNSHINE.name)
        } catch (e: Exception) {
            NeuTheme.SUNSHINE
        }
        val sizeName = prefs.getString(KEY_KEYBOARD_SIZE, KeyboardSize.MEDIUM.name)
        keyboardSize = try {
            KeyboardSize.valueOf(sizeName ?: KeyboardSize.MEDIUM.name)
        } catch (e: Exception) {
            KeyboardSize.MEDIUM
        }
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true)
        autocompleteEnabled = prefs.getBoolean(KEY_AUTOCOMPLETE, true)
        autoCapitalizationEnabled = prefs.getBoolean(KEY_AUTO_CAPITALIZATION, true)
        showNumberRow = prefs.getBoolean(KEY_SHOW_NUMBER_ROW, false)
        roundedKeysEnabled = prefs.getBoolean(KEY_ROUNDED_KEYS, false)
        
        val soundName = prefs.getString(KEY_SOUND_TYPE, SoundType.MUTE.name) ?: SoundType.MUTE.name
        soundType = try {
            SoundType.valueOf(soundName)
        } catch (e: Exception) {
            SoundType.MUTE
        }
    }

    private fun updateEnterLabel(info: EditorInfo?) {
        enterLabel = InputTypeDetector.getEnterLabel(info)
    }

    private fun handleEnterAction() {
        val info = currentInputEditorInfo
        val actionId = currentInputConfig.actionId

        if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED &&
            actionId != EditorInfo.IME_ACTION_NONE
        ) {
            currentInputConnection?.performEditorAction(actionId)
        } else {
            val isMultiline = ((info?.inputType ?: 0) and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            if (isMultiline) {
                currentInputConnection?.commitText("\n", 1)
            } else {
                val performed = currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE) == true
                if (!performed) {
                    currentInputConnection?.commitText("\n", 1)
                }
            }
        }
    }

    private fun getCursorContext(): Pair<String, String> {
        val textBefore = currentInputConnection
            ?.getTextBeforeCursor(80, 0)
            ?.toString() ?: return Pair("", "")

        val isEndsWithSpace = textBefore.endsWith(" ") || textBefore.endsWith("\n") || textBefore.endsWith("\t")
        val words = textBefore.trim().split(WORD_SPLIT_REGEX).filter { it.isNotBlank() }

        val currentWord = if (isEndsWithSpace) "" else (words.lastOrNull() ?: "")
        val previousWord = if (isEndsWithSpace) {
            words.lastOrNull() ?: ""
        } else {
            if (words.size >= 2) words[words.size - 2] else ""
        }

        return Pair(currentWord, previousWord)
    }

    private fun getPreviousWordBeforeCursor(): String {
        val textBefore = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString() ?: return ""

        val words = textBefore.trim().split(WORD_SPLIT_REGEX)
            .filter { it.isNotBlank() }

        return if (words.size >= 2) words[words.size - 2] else if (words.size == 1 && textBefore.endsWith(" ")) words[0] else ""
    }

    private fun getCurrentWordBeforeCursor(): String {
        val textBefore = currentInputConnection
            ?.getTextBeforeCursor(50, 0)
            ?.toString() ?: return ""

        if (textBefore.endsWith(" ") || textBefore.endsWith("\n")) return ""

        // Find the last word (split by spaces and common delimiters)
        return textBefore.split(WORD_SPLIT_REGEX)
            .lastOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun learnLastTypedWord() {
        if (!suggestionsAllowed || detectedInputType != DetectedInputType.TEXT) return
        val textBefore = currentInputConnection
            ?.getTextBeforeCursor(100, 0)
            ?.toString() ?: return

        val words = textBefore.trim().split(WORD_SPLIT_REGEX)
            .filter { it.isNotBlank() }

        if (words.isNotEmpty()) {
            val lastWord = words.last()
            DictionaryProvider.learnWord(this, lastWord)

            if (words.size >= 2) {
                val prevWord = words[words.size - 2]
                DictionaryProvider.learnWordSequence(this, prevWord, lastWord)
            }
        }
    }

    private fun startVoiceInput() {
        speechHelper.startListening(object : SpeechRecognizerHelper.VoiceInputCallback {
            override fun onResult(text: String, isFinal: Boolean) {
                if (isFinal) {
                    currentInputConnection?.commitText(text, 1)
                } else {
                    currentInputConnection?.setComposingText(text, 1)
                }
            }

            override fun onError(errorMessage: String) {
                // Silently handle — could show a toast in future
            }

            override fun onListeningStarted() {
                // Could update UI to show listening indicator
            }

            override fun onListeningStopped() {
                // Could update UI to hide listening indicator
            }
        })
    }
}
