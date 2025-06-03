package com.b1gbr0ther

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.b1gbr0ther.data.database.DatabaseManager
import java.time.LocalDateTime
import kotlin.math.floor

class DashboardActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var currentTaskText: TextView
    private lateinit var timeTracker: TimeTrackerInterface
    private lateinit var statusTextView: TextView
    private lateinit var simulateWakeWordButton: Button

    private lateinit var databaseManager: DatabaseManager

    private lateinit var voiceRecognizerManager: VoiceRecognizerManager
    private lateinit var commandHandler: VoiceCommandHandler

    private var mockStartTime = 0L

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
            }
        }

    fun formatTimeFromMillis(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun updateCurrentTask(name: String) {
        currentTaskText.text = name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        databaseManager = (application as B1gBr0therApplication).databaseManager

        timeTracker = TimeTracker.getInstance(this)

        mockStartTime = System.currentTimeMillis()

        currentTaskText = findViewById(R.id.currentTaskText)
        statusTextView = findViewById(R.id.statusTextView)
        simulateWakeWordButton = findViewById(R.id.simulateWakeWordButton)

        initializeVoiceRecognition()

        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(1)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chart = findViewById<WeekTimeGridView>(R.id.weekGrid)

        //0f = 24:00, 11f = 11:00, 23 = 23:00
        val myWorkData = listOf(
            WorkBlock(0, 8f, 12f, false),
            WorkBlock(0, 12f, 13.5f, true),
            WorkBlock(0, 13.5f, 17.5f, false),
            WorkBlock(1, 9f, 11f, false),
            WorkBlock(3, 15f, 19f, false),
            WorkBlock(4, 8f, 15f, false),
            WorkBlock(5, 14f, 16.5f, false),
            WorkBlock(6, 9f, 13.5f, false),
            WorkBlock(6, 23f, 23.99f, false),
        )
        //^^this is dummy data, replace this with data gathered from the database once the connection is there

        chart.setWorkData(myWorkData)

        timerText = findViewById(R.id.timer)

        timerRunnable = object : Runnable {
            override fun run() {
                if (timeTracker.isTracking()) {
                    val currentDuration = timeTracker.getCurrentDuration()
                    timerText.text = formatTimeFromMillis(currentDuration)

                    if (currentTaskText.text == "Not busy with a task") {
                        if (timeTracker.isOnBreak()) {
                            currentTaskText.text = "On break"
                        } else {
                            currentTaskText.text = "Currently busy with a task"
                        }
                    }
                } else {
                    val now = java.time.LocalTime.now()
                    timerText.text = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

                    if (currentTaskText.text != "Not busy with a task") {
                        currentTaskText.text = "Not busy with a task"
                    }
                }

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable)

        findViewById<Button>(R.id.btnDatabaseTest).setOnClickListener {
            val intent = Intent(this, DatabaseTestActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.b1gBr0therButton).setOnClickListener {
            val intent = Intent(this, HandGesturesActivity::class.java)
            startActivity(intent)
        }

        simulateWakeWordButton.setOnClickListener {
            checkPermissionAndStartRecognition()
        }
    }

    private fun initializeVoiceRecognition() {
        voiceRecognizerManager = VoiceRecognizerManager(
            context = this,
            onStatusUpdate = { status ->
                runOnUiThread {
                    statusTextView.text = status
                }
            },
            onResult = { result ->
                handleVoiceResult(result)
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )

        voiceRecognizerManager.setOnBlowDetected {
            runOnUiThread {
                if (timeTracker.isTracking() && !timeTracker.isOnBreak()) {
                    voiceRecognizerManager.showSmokeBreakDialog(this) {
                        startBreak()
                    }
                }
            }
        }

        commandHandler = VoiceCommandHandler(this)
        statusTextView.text = "Voice recognition ready"
    }

    private fun checkPermissionAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startVoiceRecognition()
        }
    }

    private fun startVoiceRecognition() {
        voiceRecognizerManager.checkPermissionAndStart()
    }

    private fun handleVoiceResult(result: String) {
        runOnUiThread {
            val recognized = commandHandler.handleCommand(result)
            if (!recognized) {
                Toast.makeText(this, "Command not recognized: $result", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startTracking() {
        if (!timeTracker.isTracking()) {
            timeTracker.startTracking()
            Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
            updateCurrentTask("Currently busy with a task")
        } else {
            Toast.makeText(this, "Already tracking", Toast.LENGTH_SHORT).show()
        }
    }

    fun startTrackingWithTask(task: Task) {
        if (!timeTracker.isTracking()) {
            timeTracker.startTracking()
            Toast.makeText(this, "Tracking started for: ${task.getName()}", Toast.LENGTH_SHORT).show()
            updateCurrentTask("Currently busy with a task")
        } else {
            Toast.makeText(this, "Already tracking", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopTracking() {
        val summary = timeTracker.stopTracking()
        if (summary == null) {
            Toast.makeText(this, "No active tracking session", Toast.LENGTH_SHORT).show()
            return
        }

        val hoursElapsed = floor(summary.effectiveTimeMillis / (1000.0 * 60 * 60)).toLong()
        val minutesElapsed = ((summary.effectiveTimeMillis / (1000.0 * 60)) % 60).toLong()

        val startTime = LocalDateTime.now()
            .minusHours(hoursElapsed)
            .minusMinutes(minutesElapsed)

        val task = Task(
            "Dashboard voice task",
            startTime,
            LocalDateTime.now(),
            false, // not preplanned
            true,  // completed
            false  // not a break
        )

        databaseManager.createAppTask(task) { taskId ->
            Toast.makeText(
                this,
                "Tracking stopped. Session saved with ID: $taskId",
                Toast.LENGTH_SHORT
            ).show()
        }

        updateCurrentTask("Not busy with a task")
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
            updateCurrentTask("On break")
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
            updateCurrentTask("Currently busy with a task")
        }
    }

    fun showExportPage() {
        val intent = Intent(this, ExportPage::class.java)
        startActivity(intent)
    }

    fun showManualPage() {
        val intent = Intent(this, ManualPage::class.java)
        startActivity(intent)
    }

    fun getDatabaseManager(): DatabaseManager {
        return databaseManager
    }

    override fun onResume() {
        super.onResume()

        if (timeTracker.isTracking()) {
            val currentDuration = timeTracker.getCurrentDuration()
            timerText.text = formatTimeFromMillis(currentDuration)

            if (timeTracker.isOnBreak()) {
                currentTaskText.text = "On break"
            } else {
                currentTaskText.text = "Currently busy with a task"
            }
        } else {
            mockStartTime = System.currentTimeMillis()
            currentTaskText.text = "Not busy with a task"
        }
        
        // Reset voice recognition status
        statusTextView.text = "Voice recognition ready"
    }

    override fun onPause() {
        super.onPause()
        voiceRecognizerManager.destroyRecognizer()
        statusTextView.text = "Voice recognition stopped"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        voiceRecognizerManager.destroyRecognizer()
    }
}
