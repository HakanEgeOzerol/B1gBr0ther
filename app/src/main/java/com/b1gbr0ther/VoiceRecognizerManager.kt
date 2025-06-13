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
import com.b1gbr0ther.audio.UrinationSoundDetector
import kotlinx.coroutines.*
import android.util.Log

class VoiceRecognizerManager(
    private val context: Context,
    private val onStatusUpdate: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit) {
    companion object {
        private const val TAG = "VoiceRecognizerManager"
        private const val NO_COMMAND_TIMEOUT = 5000L
        private const val MAX_CONSECUTIVE_ERRORS = 3
        private const val ERROR_RECOVERY_DELAY = 1000L
    }
    
    private var recognizer: SpeechRecognizer? = null
    private val blowDetector = BlowDetector()
    private val urinationDetector = UrinationSoundDetector(context)
    private var onBlowDetected: (() -> Unit)? = null
    private var onSneezeDetected: (() -> Unit)? = null
    private var onUrinationDetected: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastCommandAttemptTime = 0L
    private var noCommandRunnable: Runnable? = null
    
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
        val isVoiceRecognitionEnabled = SettingsActivity.isVoiceRecognitionEnabled(sharedPreferences)

        if (!isVoiceRecognitionEnabled) {
            onStatusUpdate("Voice recognition disabled")
            onError("Voice recognition is disabled in settings")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onStatusUpdate("Voice recognition permission required")
            onError("Microphone permission required")
            return
        }

        startRecognition()
    }

    fun startRecognition() {
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val isVoiceRecognitionEnabled = SettingsActivity.isVoiceRecognitionEnabled(sharedPreferences)
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (!isVoiceRecognitionEnabled || !hasPermission) {
            Log.w(TAG, "Cannot start recognition - enabled: $isVoiceRecognitionEnabled, permission: $hasPermission")
            if (!isVoiceRecognitionEnabled) {
                onStatusUpdate("Voice recognition disabled")
                onError("Voice recognition is disabled in settings")
            } else {
                onStatusUpdate("Voice recognition permission required")
                onError("Voice recognition is disabled or permission not granted")
            }
            return
        }

        Log.d(TAG, "Starting recognition - using single microphone source...")
        onStatusUpdate("Voice recognition ready")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Starting background urination detector initialization...")
                urinationDetector.initialize()
                Log.i(TAG, "Urination detector initialized successfully")
                
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Voice recognition ready")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize urination detector (voice commands still work)", e)
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Voice recognition ready")
                }
            }
        }

        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        isVoiceRecognitionActive = true
        isProcessingCommand = false
        lastCommandAttemptTime = System.currentTimeMillis()
        consecutiveErrorCount = 0
        
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
            startListening(createRecognitionIntent())
        }
        scheduleNoCommandTimeout()
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

    private fun stopRecognition() {
        isVoiceRecognitionActive = false
        noCommandRunnable?.let { handler.removeCallbacks(it) }
        recognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        recognizer = null
        onStatusUpdate("Voice recognition stopped")
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
        noCommandRunnable?.let { handler.removeCallbacks(it) }
        noCommandRunnable = Runnable {
            if (System.currentTimeMillis() - lastCommandAttemptTime >= NO_COMMAND_TIMEOUT) {
                stopRecognition()
            }
        }
        handler.postDelayed(noCommandRunnable!!, NO_COMMAND_TIMEOUT)
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
            else -> "Unknown error: $error"
        }

        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
            error == SpeechRecognizer.ERROR_NETWORK ||
            error == SpeechRecognizer.ERROR_SERVER) {
            consecutiveErrorCount++
            if (consecutiveErrorCount >= MAX_CONSECUTIVE_ERRORS) {
                onError("Voice recognition stopped: $msg")
                stopRecognition()
                return
            }
            onError(msg)
        } else {
            onStatusUpdate(msg)
            if (isVoiceRecognitionActive && !isProcessingCommand) {
                lastCommandAttemptTime = System.currentTimeMillis()
                scheduleNoCommandTimeout()
                handler.postDelayed({ 
                    if (isVoiceRecognitionActive) {
                        restartRecognition() 
                    }
                }, ERROR_RECOVERY_DELAY)
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isProcessingCommand) {
                    onStatusUpdate("Ready for speech")
                    blowDetector.reset()
                }
            }

            override fun onBeginningOfSpeech() {
                if (!isProcessingCommand) {
                    onStatusUpdate("Speech started")
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (blowDetector.processAudioSample(rmsdB, System.currentTimeMillis())) {
                    onBlowDetected?.invoke()
                }
                
                scope.launch {
                    try {
                        val simulatedSamples = ShortArray(1024) { (rmsdB * 100).toInt().toShort() }
                        Log.v(TAG, "Processing RMS data: rmsdB=$rmsdB")
                        val detectionResult = urinationDetector.processAudioSample(simulatedSamples)
                        Log.v(TAG, "Urination detection result from RMS: $detectionResult")
                        if (detectionResult) {
                            withContext(Dispatchers.Main) {
                                Log.i(TAG, "URINATION DETECTED from RMS data - invoking callback!")
                                onUrinationDetected?.invoke()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing RMS for urination detection", e)
                    }
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                buffer?.let { audioData ->
                    scope.launch {
                        try {
                            val audioSamples = ShortArray(audioData.size / 2)
                            for (i in audioSamples.indices) {
                                val byteIndex = i * 2
                                if (byteIndex + 1 < audioData.size) {
                                    audioSamples[i] = ((audioData[byteIndex + 1].toInt() shl 8) or 
                                                      (audioData[byteIndex].toInt() and 0xFF)).toShort()
                                }
                            }
                            
                            Log.v(TAG, "Processing ${audioSamples.size} audio samples from buffer")
                            val detectionResult = urinationDetector.processAudioSample(audioSamples)
                            Log.d(TAG, "Urination detection result from buffer: $detectionResult")
                            if (detectionResult) {
                                withContext(Dispatchers.Main) {
                                    Log.i(TAG, "URINATION DETECTED from audio buffer - invoking callback!")
                                    onUrinationDetected?.invoke()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing audio buffer for urination detection", e)
                        }
                    }
                }
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

                if (spoken.isNotEmpty()) {
                    isProcessingCommand = true
                    onResult(spoken)
                    stopRecognition()
                } else {
                    lastCommandAttemptTime = System.currentTimeMillis()
                    if (isVoiceRecognitionActive && !isProcessingCommand) {
                        scheduleNoCommandTimeout()
                        restartRecognition()
                    }
                }
            }

            override fun onPartialResults(partial: Bundle?) {
                if (!isProcessingCommand) {
                    val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotEmpty()) {
                        lastCommandAttemptTime = System.currentTimeMillis()
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
        recognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        recognizer = null
        
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val isEnabled = SettingsActivity.isVoiceRecognitionEnabled(sharedPreferences)
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        when {
            !isEnabled -> onStatusUpdate("Voice recognition disabled")
            !hasPermission -> onStatusUpdate("Voice recognition permission required")
            else -> onStatusUpdate("Voice recognition ready")
        }
    }
}
