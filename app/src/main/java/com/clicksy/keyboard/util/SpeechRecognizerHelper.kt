package com.clicksy.keyboard.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Helper for voice input using Android's SpeechRecognizer API.
 * Wraps the SpeechRecognizer lifecycle for use from InputMethodService.
 */
class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    interface VoiceInputCallback {
        fun onResult(text: String, isFinal: Boolean)
        fun onError(errorMessage: String)
        fun onListeningStarted()
        fun onListeningStopped()
        fun onRmsChanged(rmsdB: Float) {}
    }

    /**
     * Check if RECORD_AUDIO permission has been granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if voice recognition is currently listening.
     */
    fun isListening(): Boolean = isListening

    /**
     * Check if speech recognition is supported on this device.
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start voice recognition.
     * Results are delivered through [callback].
     */
    fun startListening(
        callback: VoiceInputCallback,
        localeTag: String = Locale.getDefault().toLanguageTag()
    ) {
        if (isListening) {
            stopListening()
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    callback.onListeningStarted()
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    callback.onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    callback.onListeningStopped()
                }

                override fun onError(error: Int) {
                    isListening = false
                    callback.onListeningStopped()
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    callback.onError(message)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    callback.onListeningStopped()
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        callback.onResult(text, true)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        callback.onResult(text, false)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            callback.onError("Failed to start voice recognition: ${e.localizedMessage}")
        }
    }

    /**
     * Stop active voice recognition gracefully (waiting for final results).
     */
    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
    }

    /**
     * Cancel active voice recognition immediately without waiting for results.
     */
    fun cancel() {
        isListening = false
        speechRecognizer?.cancel()
    }

    /**
     * Release resources.
     */
    fun destroy() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
