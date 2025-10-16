package com.example.innovexia.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Voice input states
 */
enum class VoiceInputState {
    IDLE,           // Not recording
    LISTENING,      // Recording in progress
    PROCESSING,     // Converting speech to text
    ERROR           // Error occurred
}

/**
 * Voice input error types
 */
sealed class VoiceInputError {
    object NoSpeech : VoiceInputError()
    object NetworkError : VoiceInputError()
    object AudioError : VoiceInputError()
    object RecognizerBusy : VoiceInputError()
    object PermissionDenied : VoiceInputError()
    data class Unknown(val code: Int) : VoiceInputError()
}

/**
 * Manager for voice input using Android's SpeechRecognizer.
 * Handles speech-to-text conversion with proper lifecycle management.
 */
class VoiceInputManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceInputManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isInitialized = false

    // State flows
    private val _state = MutableStateFlow(VoiceInputState.IDLE)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    private val _finalResult = MutableStateFlow<String?>(null)
    val finalResult: StateFlow<String?> = _finalResult.asStateFlow()

    private val _error = MutableStateFlow<VoiceInputError?>(null)
    val error: StateFlow<VoiceInputError?> = _error.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private var startTime = 0L

    /**
     * Initialize the speech recognizer
     */
    private fun initialize() {
        if (isInitialized) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            _error.value = VoiceInputError.AudioError
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }
        isInitialized = true
        Log.d(TAG, "VoiceInputManager initialized")
    }

    /**
     * Start listening for voice input
     */
    fun startListening() {
        try {
            initialize()

            if (speechRecognizer == null) {
                Log.e(TAG, "SpeechRecognizer is null, cannot start listening")
                _state.value = VoiceInputState.ERROR
                _error.value = VoiceInputError.AudioError
                return
            }

            // Reset state
            _state.value = VoiceInputState.LISTENING
            _partialResult.value = ""
            _finalResult.value = null
            _error.value = null
            _elapsedTime.value = 0L
            startTime = System.currentTimeMillis()

            // Create recognition intent with 2-second silence timeout
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                // Configure silence timeouts (in milliseconds)
                // Stop listening after 2 seconds of silence
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                // Consider speech "possibly complete" after 2 seconds of silence
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                // Minimum length of silence after speech before end-of-speech is detected
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening for voice input")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice input: ${e.message}", e)
            _state.value = VoiceInputState.ERROR
            _error.value = VoiceInputError.Unknown(-1)
        }
    }

    /**
     * Stop listening for voice input
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice input: ${e.message}", e)
        }
    }

    /**
     * Cancel voice input (no results will be delivered)
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _state.value = VoiceInputState.IDLE
            _partialResult.value = ""
            _finalResult.value = null
            _elapsedTime.value = 0L
            Log.d(TAG, "Voice input canceled")
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling voice input: ${e.message}", e)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
        if (_state.value == VoiceInputState.ERROR) {
            _state.value = VoiceInputState.IDLE
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isInitialized = false
            _state.value = VoiceInputState.IDLE
            Log.d(TAG, "VoiceInputManager destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying VoiceInputManager: ${e.message}", e)
        }
    }

    /**
     * Recognition listener implementation
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _state.value = VoiceInputState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech detected")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Update elapsed time
            if (startTime > 0) {
                _elapsedTime.value = System.currentTimeMillis() - startTime
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Not used
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            _state.value = VoiceInputState.PROCESSING
        }

        override fun onError(error: Int) {
            Log.e(TAG, "Recognition error: $error")
            _state.value = VoiceInputState.ERROR

            _error.value = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> VoiceInputError.NoSpeech
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputError.NoSpeech
                SpeechRecognizer.ERROR_NETWORK -> VoiceInputError.NetworkError
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceInputError.NetworkError
                SpeechRecognizer.ERROR_AUDIO -> VoiceInputError.AudioError
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceInputError.RecognizerBusy
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputError.PermissionDenied
                else -> VoiceInputError.Unknown(error)
            }

            // Auto-reset to idle after error
            _state.value = VoiceInputState.IDLE
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""

            Log.d(TAG, "Final results: $text")
            _finalResult.value = text
            _state.value = VoiceInputState.IDLE
            _elapsedTime.value = 0L
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""

            Log.d(TAG, "Partial results: $text")
            _partialResult.value = text
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Not used
        }
    }

    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}
