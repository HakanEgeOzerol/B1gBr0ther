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
            onStatusUpdate("Audio features disabled")
            onError("Voice recognition is disabled in settings")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onStatusUpdate("Audio permission required")
            onError("Microphone permission required")
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
                onStatusUpdate("Audio features disabled")
                onError("Voice recognition is disabled in settings")
            } else {
                val modeText = when (audioMode) {
                    SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> "Voice commands"
                    SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> "Sound detection"
                    else -> "Audio"
                }
                onStatusUpdate("$modeText permission required")
                onError("Voice recognition is disabled or permission not granted")
            }
            return
        }

        Log.d(TAG, "Starting recognition - mode: $audioMode")
        val statusMessage = when (audioMode) {
            SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> "Voice recognition ready"
            SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> "Sound detection ready"
            else -> "Audio ready"
        }
        onStatusUpdate(statusMessage)

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
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
            SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> "Listening... (say your command)"
            SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> "Listening for sounds..."
            else -> "Listening..."
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
                onStatusUpdate("Voice recognition timeout - stopping")
                stopRecognition()
            }
            handler.postDelayed(noCommandRunnable!!, NO_COMMAND_TIMEOUT)
            Log.d(TAG, "Voice Commands Mode - timeout scheduled for ${NO_COMMAND_TIMEOUT}ms")
        }
    }

    private fun handleRecognitionError(error: Int) {
        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Listening..."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "Listening..."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening..."
            11 -> "Language not supported" // ERROR_LANGUAGE_NOT_SUPPORTED
            else -> "Unknown error: $error"
        }

        // Check audio mode setting for context-specific logging
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)

        // Reduce log spam for expected errors in Sound Detection Mode
        if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION && 
            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            // These are expected in sound detection mode - only log at debug level
            Log.d(TAG, "Sound Detection Mode - expected error: $error ($msg)")
        } else {
            // Log other errors at warning level
            Log.w(TAG, "Recognition error: $error ($msg)")
        }

        // Critical errors that should stop recognition
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
            error == SpeechRecognizer.ERROR_NETWORK ||
            error == SpeechRecognizer.ERROR_SERVER ||
            error == 11) { // Language not supported
            consecutiveErrorCount++
            if (consecutiveErrorCount >= MAX_CONSECUTIVE_ERRORS) {
                onError("Voice recognition stopped: $msg")
                stopRecognition()
                return
            }
            onError(msg)
        } else {
            // Non-critical errors - just continue listening
            onStatusUpdate(msg)
            if (isVoiceRecognitionActive && !isProcessingCommand) {
                
                if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                    // Sound Detection Mode - CRITICAL: Don't restart on every error!
                    // Only restart on actual audio failures, not timeout/no-match
                    if (error == SpeechRecognizer.ERROR_AUDIO || 
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Log.d(TAG, "Sound Detection Mode - restarting after audio error")
                        scheduleNoCommandTimeout()
                        handler.postDelayed({ restartRecognition() }, 3000L)
                    } else {
                        // For timeout/no-match errors in Sound Detection Mode, just continue
                        Log.d(TAG, "Sound Detection Mode - ignoring non-critical error: $error")
                    }
                } else {
                    // Voice Commands Mode - check timeout
                    if (System.currentTimeMillis() - sessionStartTime < NO_COMMAND_TIMEOUT - 2000L) {
                        scheduleNoCommandTimeout()
                        restartRecognition()
                    } else {
                        onStatusUpdate("Voice recognition timeout - stopping")
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
                    onStatusUpdate("Ready for speech")
                    
                    // CRITICAL: Only initialize detectors in Sound Detection Mode
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
                    onStatusUpdate("Speech started")
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Check audio mode setting
                val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
                val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                
                // Only log high audio levels to reduce spam, but include mode info
                if (rmsdB > 8.0f) {
                    Log.d(TAG, "High RMS Level: $rmsdB dB (Mode: $audioMode)")
                }
                
                // CRITICAL: Only process audio detection in Sound Detection Mode
                if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                    // Process blow detection with more logging
                    val blowResult = blowDetector.processAudioSample(rmsdB, System.currentTimeMillis())
                    if (blowResult) {
                        Log.i(TAG, "BLOW DETECTED! Triggering callback and stopping recognition...")
                        onBlowDetected?.invoke()
                        // Stop recognition after blow detection to prevent stuck state
                        handler.postDelayed({ stopRecognition() }, 1000L)
                        return // Exit early to prevent further processing
                    }
                    
                    // Process sneeze detection with detailed logging
                    val sneezeResult = sneezeDetector.processAudioSample(rmsdB, System.currentTimeMillis())
                    if (rmsdB > 10.0f) { // Log sneeze detection attempts for high audio
                        Log.d(TAG, "SneezeDetector processing: rmsdB=$rmsdB, result=$sneezeResult")
                    }
                    if (sneezeResult) {
                        Log.i(TAG, "SNEEZE DETECTED! Triggering callback and stopping recognition...")
                        onSneezeDetected?.invoke()
                        // Stop recognition after sneeze detection to prevent stuck state
                        handler.postDelayed({ stopRecognition() }, 1000L)
                        return // Exit early to prevent further processing
                    }
                } else {
                    // Voice Commands Mode - completely skip audio detection (no detector calls at all)
                    if (rmsdB > 10.0f) { // Only log very high audio in voice mode to confirm separation
                        Log.d(TAG, "Voice Commands Mode - ignoring audio detection (rmsdB=$rmsdB)")
                    }
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received - no action needed for our implementation
            }

            override fun onEndOfSpeech() {
                onStatusUpdate("Processing speech...")
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
                        // Sound Detection Mode - ignore voice commands, just continue listening
                        Log.d(TAG, "Sound Detection Mode - ignoring voice command: $spoken")
                        // CRITICAL: Don't restart on every voice result! This causes the constant restarting
                        // Just continue the current session without restarting
                    }
                } else {
                    // No speech detected
                    if (isVoiceRecognitionActive && !isProcessingCommand) {
                        if (audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION) {
                            // Sound Detection Mode - CRITICAL: Don't restart continuously!
                            // Let the session continue naturally without forced restarts
                            Log.d(TAG, "Sound Detection Mode - no speech, continuing current session")
                        } else {
                            // Voice Commands Mode - check timeout
                            if (System.currentTimeMillis() - sessionStartTime < NO_COMMAND_TIMEOUT - 2000L) {
                                scheduleNoCommandTimeout()
                                restartRecognition()
                            } else {
                                onStatusUpdate("Voice recognition timeout - stopping")
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
                        onStatusUpdate("Partial: $text")
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
        Toast.makeText(activity, "Enjoy your smoke break!", Toast.LENGTH_SHORT).show()
        onConfirm()
    }

    fun sayBlessYou(activity: Activity) {
        Toast.makeText(activity, "Bless you!", Toast.LENGTH_SHORT).show()
    }

    fun sayUrination(activity: Activity) {
        Toast.makeText(activity, "Urination detected!", Toast.LENGTH_SHORT).show()
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
            audioMode == SettingsActivity.AUDIO_MODE_OFF -> onStatusUpdate("Audio features disabled")
            audioMode == SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> onStatusUpdate("Press button to start voice commands")
            audioMode == SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> onStatusUpdate("Press button to start sound detection")
            else -> onStatusUpdate("Press button to start voice recognition")
        }
    }
}
