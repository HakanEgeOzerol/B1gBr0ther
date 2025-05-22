package com.b1gbr0ther

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
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
    private lateinit var trackingStatusTextView: TextView
    private lateinit var lastSessionTextView: TextView
    private lateinit var menuBar: MenuBar

    private var lastSummary: TimeTracker.TrackingSummary? = null
    private var recognizer: SpeechRecognizer? = null
    private lateinit var commandHandler: VoiceCommandHandler
    private lateinit var timeTracker: TimeTrackerInterface

    private val handler = Handler(Looper.getMainLooper())
    private var liveTimerRunnable: Runnable? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startAudioRecognition()
            else showError("Microphone permission is required")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_audio_recognition)

        menuBar = findViewById(R.id.menuBar)
        menuBar.setActivePage(2) // Set timesheet page as active

        statusTextView = findViewById(R.id.statusTextView)
        transcriptTextView = findViewById(R.id.transcriptTextView)
        trackingStatusTextView = findViewById(R.id.trackingStatusTextView)
        lastSessionTextView = findViewById(R.id.lastSessionTextView)

        timeTracker = TimeTracker.getInstance(this)
        commandHandler = VoiceCommandHandler(this)

        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(1) // 1 is for Dashboard

        findViewById<Button>(R.id.listenButton).setOnClickListener {
            checkPermissionAndStartRecognition()
        }

        findViewById<Button>(R.id.statusButton).setOnClickListener {
            updateTrackingStatus()
            Toast.makeText(this, "Checking tracking status...", Toast.LENGTH_SHORT).show()

            if (lastSummary != null) {
                val summary = lastSummary!!
                val text = """
                    Last Session
                    Total: ${formatTime(summary.totalTimeMillis)}
                    Breaks: ${formatTime(summary.breakTimeMillis)}
                    Work Time: ${formatTime(summary.effectiveTimeMillis)}
                    Break #: ${summary.breakCount}
                """.trimIndent()
                lastSessionTextView.text = text
                lastSessionTextView.visibility = View.VISIBLE
            } else {
                lastSessionTextView.text = "No previous sessions found."
                lastSessionTextView.visibility = View.VISIBLE
            }
        }

        findViewById<Button>(R.id.manualOverlayButton).setOnClickListener {
            // TODO: Implement manual overlay functionality
            Toast.makeText(this, "Manual overlay coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        menuBar.setActivePage(2) // Ensure correct menu item is highlighted

        if (timeTracker.isTracking()) {
            updateTrackingStatus()
            startLiveTimer()
            Toast.makeText(
                this,
                "Tracking session restored from ${formatTime(timeTracker.getCurrentDuration())} ago",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLiveTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyRecognizer()
    }

    private fun checkPermissionAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
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

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateStatus("Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    updateStatus("Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    updateStatus("Speech ended")
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    transcriptTextView.text = spoken

                    val recognized = commandHandler.handleCommand(spoken)
                    updateStatus(if (recognized) "Command executed: \"$spoken\"" else "Unrecognized command: \"$spoken\"")
                    Toast.makeText(this@AudioRecognitionActivity, "Recognition complete", Toast.LENGTH_SHORT).show()
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

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        startAudioRecognition()
                    } else {
                        destroyRecognizer()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            startListening(intent)
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

    private fun updateStatus(text: String) {
        runOnUiThread { statusTextView.text = text }
    }

    private fun showError(text: String) {
        runOnUiThread {
            statusTextView.text = "Error: $text"
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun startLiveTimer() {
        liveTimerRunnable = object : Runnable {
            override fun run() {
                updateTrackingStatus()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.post(liveTimerRunnable!!)
    }

    private fun stopLiveTimer() {
        liveTimerRunnable?.let { handler.removeCallbacks(it) }
    }

    fun startTracking() {
        timeTracker.startTracking()
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
        updateTrackingStatus()
        startLiveTimer()
    }

    fun stopTracking() {
        val summary = timeTracker.stopTracking()
        if (summary == null) {
            Toast.makeText(this, "Tracking not active", Toast.LENGTH_SHORT).show()
            return
        }

        val toastText = "Total: ${formatTime(summary.totalTimeMillis)}, " +
                "Work Time: ${formatTime(summary.effectiveTimeMillis)}"
        Toast.makeText(this, "Tracking stopped. $toastText", Toast.LENGTH_LONG).show()

        lastSummary = summary
        trackingStatusTextView.text = "No active tracking session."
        stopLiveTimer()
    }

    fun startBreak() {
        if (!timeTracker.isTracking()) {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
            return
        }

        if (timeTracker.isOnBreak()) {
            Toast.makeText(this, "Already on a break", Toast.LENGTH_SHORT).show()
        } else if (timeTracker.startBreak()) {
            Toast.makeText(this, "Break started", Toast.LENGTH_SHORT).show()
            updateTrackingStatus()
        }
    }

    fun endBreak() {
        if (!timeTracker.isTracking()) {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!timeTracker.isOnBreak()) {
            Toast.makeText(this, "No active break", Toast.LENGTH_SHORT).show()
        } else if (timeTracker.endBreak()) {
            Toast.makeText(this, "Break ended", Toast.LENGTH_SHORT).show()
            updateTrackingStatus()
        }
    }

    fun showDashboard() {
        updateTrackingStatus()
    }

    private fun updateTrackingStatus() {
        if (!timeTracker.isTracking()) {
            trackingStatusTextView.text = "No active tracking session."
            return
        }

        val currentDuration = timeTracker.getCurrentDuration()
        val workTime = timeTracker.getCurrentEffectiveTime()
        val breakTime = timeTracker.getTotalBreakTime()
        val breakCount = timeTracker.getBreakCount()

        val statusText = if (timeTracker.isOnBreak()) {
            """
            TRACKING (On break)
            Duration: ${formatTime(currentDuration)}
            Break time: ${formatTime(breakTime)}
            Work Time: ${formatTime(workTime)}
            Break #: $breakCount
            """
        } else {
            """
            TRACKING (Working)
            Duration: ${formatTime(currentDuration)}
            Break time: ${formatTime(breakTime)}
            Work Time: ${formatTime(workTime)}
            Break #: $breakCount
            """
        }

        trackingStatusTextView.text = statusText.trimIndent()
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }
}
