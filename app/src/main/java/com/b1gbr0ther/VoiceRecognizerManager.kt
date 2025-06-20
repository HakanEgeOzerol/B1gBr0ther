package com.b1gbr0ther

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.util.Locale
import kotlinx.coroutines.*
import android.util.Log
import com.b1gbr0ther.easteregg.DoodleJumpActivity

class VoiceRecognizerManager(
    private val context: Context,
    private val onStatusUpdate: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit) {
    companion object {
        private const val TAG = "VoiceRecognizerManager"
        private const val NO_COMMAND_TIMEOUT = 15000L
        private const val MAX_CONSECUTIVE_ERRORS = 3
        private const val ERROR_RECOVERY_DELAY = 1000L
    }
    
    private var recognizer: SpeechRecognizer? = null
    private val blowDetector = BlowDetector()
    private val sneezeDetector = SneezeDetector()
    private var onBlowDetected: (() -> Unit)? = null
    private var onSneezeDetected: (() -> Unit)? = null
    private var onUrinationDetected: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var noCommandRunnable: Runnable? = null
    private var sessionStartTime = 0L
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isVoiceRecognitionActive = false
    private var isProcessingCommand = false
    private var consecutiveErrorCount = 0

    fun setOnBlowDetected(callback: () -> Unit) {
        onBlowDetected = callback
    }

    fun setOnSneezeDetected(callback: () -> Unit) {
        onSneezeDetected = callback
    }

    fun setOnUrinationDetected(callback: () -> Unit) {
        onUrinationDetected = callback
        Log.d(TAG, "Urination detection callback has been set")
    }

    fun checkPermissionAndStart() {
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)

        if (audioMode == SettingsActivity.AUDIO_MODE_OFF) {
            onStatusUpdate(context.getString(R.string.audio_features_disabled))
            onError(context.getString(R.string.voice_recognition_disabled_settings))
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onStatusUpdate(context.getString(R.string.audio_permission_required))
            onError(context.getString(R.string.microphone_permission_required))
            return
        }

        startRecognition()
    }

    fun startRecognition() {
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (audioMode == SettingsActivity.AUDIO_MODE_OFF || !hasPermission) {
            Log.w(TAG, "Cannot start recognition - audio mode: $audioMode, permission: $hasPermission")
            if (audioMode == SettingsActivity.AUDIO_MODE_OFF) {
                onStatusUpdate(context.getString(R.string.audio_features_disabled))
                onError(context.getString(R.string.voice_recognition_disabled_settings))
            } else {
                val modeText = when (audioMode) {
                    SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> context.getString(R.string.voice_commands)
                    SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> context.getString(R.string.sound_detection)
                    else -> context.getString(R.string.audio)
                }
                onStatusUpdate(context.getString(R.string.permission_required, modeText))
                onError(context.getString(R.string.voice_recognition_disabled_permission))
            }
            return
        }

        Log.d(TAG, "Starting recognition - mode: $audioMode")
        val statusMessage = when (audioMode) {
            SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> context.getString(R.string.voice_recognition_ready)
            SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> context.getString(R.string.sound_detection_ready)
            else -> context.getString(R.string.audio_ready)
        }
        onStatusUpdate(statusMessage)

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError(context.getString(R.string.speech_recognition_not_available))
            return
        }

        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        isVoiceRecognitionActive = true
        isProcessingCommand = false
        consecutiveErrorCount = 0
        sessionStartTime = System.currentTimeMillis()
        
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
            startListening(createRecognitionIntent())
        }
        scheduleNoCommandTimeout()
        
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
        val listeningMessage = when (audioMode) {
            SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> context.getString(R.string.listening_say_command)
            SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> context.getString(R.string.listening_for_sounds)
            else -> context.getString(R.string.listening)
        }
        onStatusUpdate(listeningMessage)
    }

    private fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
        }
    }

    private fun restartRecognition() {
        if (!isProcessingCommand && isVoiceRecognitionActive) {
            recognizer?.apply {
                stopListening()
                cancel()
                destroy()
            }
            startVoiceRecognition()
        }
    }

    private fun scheduleNoCommandTimeout() {
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
        
        noCommandRunnable?.let { handler.removeCallbacks(it) }
        
        if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
            // Sound Detection Mode - NO TIMEOUT, run continuously
            Log.d(TAG, "Sound Detection Mode - no timeout scheduled")
        } else {
            // Voice Commands Mode - use timeout
            noCommandRunnable = Runnable {
                onStatusUpdate(context.getString(R.string.voice_recognition_timeout_stopping))
                stopRecognition()
            }
            handler.postDelayed(noCommandRunnable!!, NO_COMMAND_TIMEOUT)
            Log.d(TAG, "Voice Commands Mode - timeout scheduled for ${NO_COMMAND_TIMEOUT}ms")
        }
    }

    private fun handleRecognitionError(error: Int) {
        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.audio_recording_error)
            SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.listening)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.insufficient_permissions)
            SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.network_error)
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.network_timeout)
            SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.listening)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.recognizer_busy)
            SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.server_error)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.listening)
            11 -> context.getString(R.string.language_not_supported) // ERROR_LANGUAGE_NOT_SUPPORTED
            else -> context.getString(R.string.unknown_error, error)
        }

        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)

        if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION && 
            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            Log.d(TAG, "Sound Detection Mode - expected error: $error ($msg)")
        } else {
            Log.w(TAG, "Recognition error: $error ($msg)")
        }

        // Critical errors that should stop recognition
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
            error == SpeechRecognizer.ERROR_NETWORK ||
            error == SpeechRecognizer.ERROR_SERVER ||
            error == 11) { // Language not supported
            consecutiveErrorCount++
            if (consecutiveErrorCount >= MAX_CONSECUTIVE_ERRORS) {
                onError(context.getString(R.string.voice_recognition_stopped_error, msg))
                stopRecognition()
                return
            }
            onError(msg)
        } else {
            // Non-critical errors - just continue listening
            onStatusUpdate(msg)
            if (isVoiceRecognitionActive && !isProcessingCommand) {
                
                if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                    if (error == SpeechRecognizer.ERROR_AUDIO || 
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Log.d(TAG, "Sound Detection Mode - restarting after audio error")
                        scheduleNoCommandTimeout()
                        handler.postDelayed({ restartRecognition() }, 3000L)
                    } else {
                        Log.d(TAG, "Sound Detection Mode - ignoring non-critical error: $error")
                    }
                } else {
                    // Voice Commands Mode - check timeout
                    if (System.currentTimeMillis() - sessionStartTime < NO_COMMAND_TIMEOUT - 2000L) {
                        scheduleNoCommandTimeout()
                        restartRecognition()
                    } else {
                        onStatusUpdate(context.getString(R.string.voice_recognition_timeout_stopping))
                        stopRecognition()
                    }
                }
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isProcessingCommand) {
                    val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
                    val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                    
                    Log.d(TAG, "onReadyForSpeech called - mode: $audioMode")
                    onStatusUpdate(context.getString(R.string.ready_for_speech))
                    
                    if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                        Log.d(TAG, "Sound Detection Mode - initializing detectors")
                        blowDetector.reset()
                        sneezeDetector.reset()
                    } else {
                        Log.d(TAG, "Voice Commands Mode - skipping detector initialization")
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                if (!isProcessingCommand) {
                    Log.d(TAG, "onBeginningOfSpeech called")
                    onStatusUpdate(context.getString(R.string.speech_started))
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Check audio mode setting
                val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
                val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                
                if (rmsdB > 8.0f) {
                    Log.d(TAG, "High RMS Level: $rmsdB dB (Mode: $audioMode)")
                }
                if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                    val blowResult = blowDetector.processAudioSample(rmsdB, System.currentTimeMillis())
                    if (blowResult) {
                        Log.i(TAG, "BLOW DETECTED!")
                        onBlowDetected?.invoke()
                        handler.postDelayed({ stopRecognition() }, 1000L)
                        return
                    }
                    
                    val sneezeResult = sneezeDetector.processAudioSample(rmsdB, System.currentTimeMillis())
                    if (rmsdB > 10.0f) {
                        Log.d(TAG, "SneezeDetector processing: rmsdB=$rmsdB, result=$sneezeResult")
                    }
                    if (sneezeResult) {
                        Log.i(TAG, "SNEEZE DETECTED!")
                        onSneezeDetected?.invoke()
                        handler.postDelayed({ stopRecognition() }, 1000L)
                        return
                    }
                } else {
                    if (rmsdB > 10.0f) {
                        Log.d(TAG, "Voice Commands Mode - ignoring audio detection (rmsdB=$rmsdB)")
                    }
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received - no action needed for our implementation
            }

            override fun onEndOfSpeech() {
                onStatusUpdate(context.getString(R.string.processing_speech))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                var bestMatch: String? = null
                var bestConfidence = 0f
                
                if (matches != null && confidenceScores != null) {
                    for (i in matches.indices) {
                        if (i < confidenceScores.size && confidenceScores[i] > bestConfidence) {
                            bestConfidence = confidenceScores[i]
                            bestMatch = matches[i]
                        }
                    }
                }

                val spoken = bestMatch ?: matches?.firstOrNull().orEmpty()

                // Check audio mode setting
                val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
                val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                
                if (spoken.isNotEmpty()) {
                    if (audioMode == SettingsActivity.AUDIO_MODE_VOICE_COMMANDS) {
                        // Voice Commands Mode - process command and stop
                        isProcessingCommand = true
                        onResult(spoken)
                        stopRecognition()
                    } else {
                        Log.d(TAG, "Sound Detection Mode - ignoring voice command: $spoken")
                    }
                } else {
                    // No speech detected
                    if (isVoiceRecognitionActive && !isProcessingCommand) {
                        if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                            Log.d(TAG, "Sound Detection Mode - no speech, continuing current session")
                        } else {
                            // Voice Commands Mode - check timeout
                            if (System.currentTimeMillis() - sessionStartTime < NO_COMMAND_TIMEOUT - 2000L) {
                                scheduleNoCommandTimeout()
                                restartRecognition()
                            } else {
                                onStatusUpdate(context.getString(R.string.voice_recognition_timeout_stopping))
                                stopRecognition()
                            }
                        }
                    }
                }
            }

            override fun onPartialResults(partial: Bundle?) {
                if (!isProcessingCommand) {
                    val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotEmpty()) {
                        onStatusUpdate(context.getString(R.string.partial_result, text))
                    }
                }
            }

            override fun onError(error: Int) {
                handleRecognitionError(error)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun showSmokeBreakDialog(activity: Activity, onConfirm: () -> Unit) {
        // Skip notifications if Doodle Jump game is running
        if (DoodleJumpActivity.isGameRunning()) {
            return
        }
        Toast.makeText(activity, activity.getString(R.string.enjoy_smoke_break), Toast.LENGTH_SHORT).show()
        onConfirm()
    }

    fun sayBlessYou(activity: Activity) {
        // Skip notifications if Doodle Jump game is running
        if (DoodleJumpActivity.isGameRunning()) {
            return
        }
        Toast.makeText(activity, activity.getString(R.string.bless_you), Toast.LENGTH_SHORT).show()
    }

    fun sayUrination(activity: Activity) {
        // Skip notifications if Doodle Jump game is running
        if (DoodleJumpActivity.isGameRunning()) {
            return
        }
        Toast.makeText(activity, activity.getString(R.string.urination_detected), Toast.LENGTH_SHORT).show()
    }

    fun stopRecognition() {
        isVoiceRecognitionActive = false
        noCommandRunnable?.let { handler.removeCallbacks(it) }
        recognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        recognizer = null
        
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)

        when {
            audioMode == SettingsActivity.AUDIO_MODE_OFF -> onStatusUpdate(context.getString(R.string.audio_features_disabled))
            audioMode == SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> onStatusUpdate(context.getString(R.string.press_button_start_voice_commands))
            audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> onStatusUpdate(context.getString(R.string.press_button_start_sound_detection))
            else -> onStatusUpdate(context.getString(R.string.start_voice_recognition))
        }
    }
}
