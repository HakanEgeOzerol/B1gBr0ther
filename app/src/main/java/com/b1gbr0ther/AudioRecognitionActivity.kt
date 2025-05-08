package com.b1gbr0ther

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale

class AudioRecognitionActivity : ComponentActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var transcriptTextView: TextView

    private var recognizer: SpeechRecognizer? = null

    // Dynamic calibration and smoothing
    private var minDb = Float.MAX_VALUE
    private var maxDb = -Float.MAX_VALUE
    private var smoothedLevel = 0f

    private companion object {
        private const val SMOOTHING_FACTOR = 0.8f
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startAudioRecognition()
            else showError("Microphone permission is required")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_audio_recognition)

        statusTextView = findViewById(R.id.statusTextView)
        transcriptTextView = findViewById(R.id.transcriptTextView)
        findViewById<Button>(R.id.listenButton).setOnClickListener {
            checkPermissionAndStartRecognition()
        }
    }

    private fun checkPermissionAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            updateStatus("Please grant microphone permission")
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startAudioRecognition()
        }
    }

    private fun startAudioRecognition() {
        updateStatus("Initializing...")

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showError("Speech recognition not available on this device")
            return
        }

        // Reset calibration
        minDb = Float.MAX_VALUE
        maxDb = -Float.MAX_VALUE
        smoothedLevel = 0f

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateStatus("Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    // reset per phrase
                    minDb = Float.MAX_VALUE
                    maxDb = -Float.MAX_VALUE
                    smoothedLevel = 0f
                    updateStatus("Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // update calibration
                    minDb = minOf(minDb, rmsdB)
                    maxDb = maxOf(maxDb, rmsdB)
                    // normalize
                    val norm = ((rmsdB - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                    smoothedLevel = SMOOTHING_FACTOR * smoothedLevel + (1 - SMOOTHING_FACTOR) * norm
                    val percent = (smoothedLevel * 100).toInt()

                    // display multiple stats
                    val stats = String.format(
                        Locale.getDefault(),
                        "Level: %d%%\nRaw: %.1f dB\nMin: %.1f dB\nMax: %.1f dB",
                        percent,
                        rmsdB,
                        minDb,
                        maxDb
                    )
                    updateStatus(stats)
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // no processing needed here
                }

                override fun onEndOfSpeech() {
                    updateStatus("Speech ended")
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()

                    val display = if (spoken.isNotEmpty()) spoken else "No speech detected"

                    runOnUiThread {
                        transcriptTextView.text = display
                        updateStatus("Recognition complete")
                    }

                    Toast.makeText(
                        this@AudioRecognitionActivity, "Recognition complete", Toast.LENGTH_SHORT
                    ).show()

                    destroyRecognizer()
                }


                override fun onPartialResults(partial: Bundle?) {
                    val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotEmpty()) updateStatus("Partial: $text")
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
                    showError(msg)
                    destroyRecognizer()
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            // configure and start
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            startListening(intent)
        }
    }

    private fun updateStatus(text: String) {
        runOnUiThread { statusTextView.text = text }
    }

    private fun showError(text: String) {
        runOnUiThread {
            statusTextView.text = "Error: $text"
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun destroyRecognizer() {
        recognizer?.apply {
            stopListening()
            cancel()
            destroy()
        }
        recognizer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyRecognizer()
    }
}
