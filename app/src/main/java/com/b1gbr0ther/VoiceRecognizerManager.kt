package com.b1gbr0ther

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceRecognizerManager(
    private val context: Context,
    private val onStatusUpdate: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    private val BLOW_THRESHOLD = 9.9f
    private val BLOW_COOLDOWN = 5000L
    private var lastBlowDetectedTime = 0L
    private var blowDetectionEnabled = true
    private var onBlowDetected: (() -> Unit)? = null

    fun setOnBlowDetected(callback: () -> Unit) {
        onBlowDetected = callback
    }

    fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission required")
            return
        }

        startRecognition()
    }

    fun startRecognition() {
        onStatusUpdate("Initializing speech recognition...")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            startListening(intent)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStatusUpdate("Ready for speech")
                blowDetectionEnabled = true
            }

            override fun onBeginningOfSpeech() {
                onStatusUpdate("Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (blowDetectionEnabled &&
                    rmsdB > BLOW_THRESHOLD &&
                    System.currentTimeMillis() - lastBlowDetectedTime > BLOW_COOLDOWN) {

                    lastBlowDetectedTime = System.currentTimeMillis()
                    onBlowDetected?.invoke()
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onStatusUpdate("Speech ended")
            }

            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()

                if (spoken.isNotEmpty()) {
                    onResult(spoken)
                }

                destroyRecognizer()
            }

            override fun onPartialResults(partial: Bundle?) {
                val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isNotEmpty()) onStatusUpdate("Partial: $text")
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }
                onError(msg)

                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    startRecognition()
                } else {
                    destroyRecognizer()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun showSmokeBreakDialog(activity: Activity, onConfirm: () -> Unit) {
        if (activity.isFinishing) return

        AlertDialog.Builder(activity)
            .setTitle("Smoke Break Detected")
            .setMessage("Enjoy your smoke break!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setCancelable(true)
            .show()
    }

    fun destroyRecognizer() {
        try {
            recognizer?.apply {
                stopListening()
                cancel()
                destroy()
            }
        } catch (e: Exception) {
            onError("Error destroying recognizer: ${e.message}")
        } finally {
            recognizer = null
        }
    }
}
