package com.b1gbr0ther

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.b1gbr0ther.data.database.DatabaseManager // Added DatabaseManager import
import com.b1gbr0ther.data.database.entities.Task // Added Task entity import
import java.time.LocalDateTime
import java.util.Objects
import kotlin.math.floor
import kotlin.math.sqrt
import com.b1gbr0ther.gestureRecognition.GestureRecognizer
import com.b1gbr0ther.gestureRecognition.GestureType
import com.b1gbr0ther.notifications.TaskNotificationManager
import java.time.LocalDate
import java.time.LocalTime
import com.b1gbr0ther.TimingStatus
import java.time.DayOfWeek
import com.b1gbr0ther.easteregg.DoodleJumpActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var databaseManager: DatabaseManager
    private var allTasksList: List<Task> = emptyList()
    private lateinit var timerText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var currentTaskText: TextView
    private lateinit var timeTracker: TimeTrackerInterface
    private lateinit var statusTextView: TextView
    private lateinit var simulateWakeWordButton: Button
    private lateinit var notificationManager: TaskNotificationManager
    private var currentTaskName: String? = null
    private var currentTaskId: Long = -1
    private var lastSneezeTime: Long = 0

    private fun loadAllTasks() {
        databaseManager.getAllTasks { tasks ->
            this.allTasksList = tasks
        }
    }

    private lateinit var voiceRecognizerManager: VoiceRecognizerManager
    private lateinit var commandHandler: VoiceCommandHandler

    private var sensorManager: SensorManager? = null
    private val gestureRecognizer = GestureRecognizer()
    private val shakeThreshold = 15f
    private var isDialogShown = false
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var allTasks: List<Task> = emptyList()
    private var allTasksId: List<Int> = emptyList()
    private var activeTaskId: Int = -1
    private val mediaPlayer = MediaPlayer.create(this, R.raw.pipes)

    private var mockStartTime = 0L

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                val sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
                val currentMode = SettingsActivity.getAudioMode(sharedPreferences)
                
                if (currentMode == SettingsActivity.AUDIO_MODE_OFF) {
                    sharedPreferences.edit().putInt("audio_mode", SettingsActivity.AUDIO_MODE_VOICE_COMMANDS).apply()
                    Toast.makeText(this, "Voice Commands enabled by default", Toast.LENGTH_SHORT).show()
                    updateVoiceRecognitionStatus()
                } else {
                    startVoiceRecognition()
                }
            } else {
                Toast.makeText(this, getString(R.string.microphone_permission_required_for_voice), Toast.LENGTH_SHORT).show()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, getString(R.string.notifications_disabled_warning), Toast.LENGTH_LONG).show()
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

    private fun loadWeeklyWorkBlocks(chart: WeekTimeGridView) {
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        databaseManager.getWorkBlocksForWeek(weekStart) { workBlocks ->
            chart.setWorkData(workBlocks)
        }
    }

    private fun updateCurrentTaskDisplay() {
        if (timeTracker.isTracking()) {
            if (timeTracker.isOnBreak()) {
                updateCurrentTask(getString(R.string.on_break))
            } else {
                val taskId = timeTracker.getCurrentTaskId()
                val taskName = timeTracker.getCurrentTaskName()

                if (taskName != null) {
                    currentTaskName = taskName
                    currentTaskId = taskId
                    updateCurrentTask(getString(R.string.currently_tracking, taskName))
                } else if (taskId != -1L) {
                    databaseManager.getTask(taskId) { task ->
                        if (task != null) {
                            currentTaskName = task.taskName
                            currentTaskId = taskId
                            timeTracker.setCurrentTask(taskId, task.taskName)
                            updateCurrentTask(getString(R.string.currently_tracking, task.taskName))
                        }
                    }
                }
            }
        } else {
            currentTaskName = null
            currentTaskId = -1L
            updateCurrentTask(getString(R.string.not_tracking_any_task))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        appliedLanguage = LocaleHelper.getCurrentLanguage(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        databaseManager = DatabaseManager(applicationContext)
        loadAllTasks()

        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(1) // 1 is for Dashboard
        notificationManager = TaskNotificationManager(applicationContext)
        updateAllTasks()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        timeTracker = TimeTracker.getInstance(this)

        mockStartTime = System.currentTimeMillis()

        currentTaskText = findViewById(R.id.currentTaskText)

        statusTextView = findViewById(R.id.statusTextView)
        simulateWakeWordButton = findViewById(R.id.simulateWakeWordButton)

        initializeVoiceRecognition()

        menu.setActivePage(1)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chart = findViewById<WeekTimeGridView>(R.id.weekGrid)

        //0f = 24:00, 11f = 11:00, 23 = 23:00
        // Load work blocks for the current week from the database
        loadWeeklyWorkBlocks(chart)
        /*
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
        // Data now loaded dynamically from database

        */
        // chart.setWorkData(myWorkData) -- retained for reference


        timerText = findViewById(R.id.timer)

        timerRunnable = object : Runnable {
            override fun run() {
                if (timeTracker.isTracking()) {
                    val currentDuration = timeTracker.getCurrentDuration()
                    timerText.text = formatTimeFromMillis(currentDuration)
                    updateCurrentTaskDisplay()
                } else {
                    val now = java.time.LocalTime.now()
                    timerText.text = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    updateCurrentTaskDisplay()
                }
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable)

        findViewById<Button>(R.id.btnManualPage).setOnClickListener {
            val intent = Intent(this, ManualPage::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.b1gBr0therButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        simulateWakeWordButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
            val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
            
            val stopText = when (audioMode) {
                SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.stop_voice_commands)
                SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.stop_sound_detection)
                else -> getString(R.string.stop_audio)
            }
            
            if (simulateWakeWordButton.text == stopText) {
                voiceRecognizerManager.stopRecognition()
            } else {
                checkPermissionAndStartRecognition()
            }
        }

        startTaskChecker()

        checkNotificationPermission()
    }

    private fun initializeVoiceRecognition() {
        voiceRecognizerManager = VoiceRecognizerManager(
            context = this,
            onStatusUpdate = { status ->
                runOnUiThread {
                    statusTextView.text = status
                    // Update button text based on voice recognition status and mode
                    val sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
                    val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                    
                    val startText = when (audioMode) {
                        SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.start_voice_commands)
                        SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.start_sound_detection)
                        else -> getString(R.string.start_audio)
                    }
                    val stopText = when (audioMode) {
                        SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.stop_voice_commands)
                        SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.stop_sound_detection)
                        else -> getString(R.string.stop_audio)
                    }
                    
                    when {
                        status.contains("Listening") || status.contains("Ready for speech") || status.contains("Processing") -> {
                            simulateWakeWordButton.text = stopText
                        }
                        status.contains("disabled") || status.contains("permission") -> {
                            simulateWakeWordButton.text = getString(R.string.start_audio)
                            simulateWakeWordButton.isEnabled = false
                        }
                        else -> {
                            simulateWakeWordButton.text = startText
                            simulateWakeWordButton.isEnabled = true
                        }
                    }
                }
            },
            onResult = { result ->
                handleVoiceResult(result)
            },
            onError = { error ->
                runOnUiThread {
                    val sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
                    val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
                    
                    val startText = when (audioMode) {
                        SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.start_voice_commands)
                        SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.start_sound_detection)
                        else -> getString(R.string.start_audio)
                    }
                    
                    if (error == "Voice recognition is disabled in settings") {
                        // Only show disabled if audio mode is actually off
                        if (audioMode == SettingsActivity.AUDIO_MODE_OFF) {
                            statusTextView.text = getString(R.string.audio_features_disabled)
                            simulateWakeWordButton.text = getString(R.string.start_audio)
                            simulateWakeWordButton.isEnabled = false
                        } else {
                            // If a mode is enabled, show the appropriate start text
                            statusTextView.text = when (audioMode) {
                                SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.press_button_start_voice_commands)
                                SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.press_button_start_sound_detection)
                                else -> getString(R.string.press_button_start_voice_commands)
                            }
                            simulateWakeWordButton.text = startText
                            simulateWakeWordButton.isEnabled = true
                        }
                    } else {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                        simulateWakeWordButton.text = startText
                        simulateWakeWordButton.isEnabled = true
                    }
                }
            }
        )

        voiceRecognizerManager.setOnBlowDetected {
            runOnUiThread {
                // Skip blow detection if Doodle Jump game is running
                if (DoodleJumpActivity.isGameRunning()) {
                    return@runOnUiThread
                }
                
                if (timeTracker.isTracking() && !timeTracker.isOnBreak()) {
                    voiceRecognizerManager.showSmokeBreakDialog(this) {
                        startBreak()
                    }
                } else {
                    // Show blow detection even when not tracking
                    Toast.makeText(this, "Blow detected! 💨", Toast.LENGTH_SHORT).show()
                }
            }
        }

        voiceRecognizerManager.setOnSneezeDetected {
            runOnUiThread {
                // Skip sneeze detection if Doodle Jump game is running
                if (DoodleJumpActivity.isGameRunning()) {
                    return@runOnUiThread
                }
                handleSneeze()
            }
        }

        commandHandler = VoiceCommandHandler(this)
        updateVoiceRecognitionStatus()
    }

    private fun updateVoiceRecognitionStatus() {
        val sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
        val audioMode = SettingsActivity.getAudioMode(sharedPreferences)
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (audioMode == SettingsActivity.AUDIO_MODE_OFF) {
            statusTextView.text = getString(R.string.audio_features_disabled)
            simulateWakeWordButton.text = getString(R.string.start_audio)
            simulateWakeWordButton.isEnabled = false
            voiceRecognizerManager.stopRecognition()
        } else if (!hasPermission) {
            val modeText = when (audioMode) {
                SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> getString(R.string.voice_commands)
                SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> getString(R.string.sound_detection)
                else -> getString(R.string.audio)
            }
            statusTextView.text = getString(R.string.permission_required, modeText)
            simulateWakeWordButton.text = getString(R.string.start_audio)
            simulateWakeWordButton.isEnabled = false
            voiceRecognizerManager.stopRecognition()
        } else {
            when (audioMode) {
                SettingsActivity.AUDIO_MODE_VOICE_COMMANDS -> {
                    statusTextView.text = getString(R.string.press_button_start_voice_commands)
                    simulateWakeWordButton.text = getString(R.string.start_voice_commands)
                }
                SettingsActivity.AUDIO_MODE_SOUND_DETECTION -> {
                    statusTextView.text = getString(R.string.press_button_start_sound_detection)
                    simulateWakeWordButton.text = getString(R.string.start_sound_detection)
                }
                else -> {
                    statusTextView.text = getString(R.string.audio_mode_not_configured)
                    simulateWakeWordButton.text = getString(R.string.start_audio)
                }
            }
            simulateWakeWordButton.isEnabled = true
            // Don't automatically start recognition
        }
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
            // Skip voice command processing if Doodle Jump game is running
            if (DoodleJumpActivity.isGameRunning()) {
                return@runOnUiThread
            }
            
            val recognized = commandHandler.handleCommand(result)
            if (!recognized) {
                Toast.makeText(this, getString(R.string.command_not_recognized, result), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startTracking() {
        if (!timeTracker.isTracking()) {
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val lastTask = tasks.last()
                    timeTracker.startTracking()
                    currentTaskName = lastTask.taskName
                    currentTaskId = lastTask.id
                    timeTracker.setCurrentTask(lastTask.id, lastTask.taskName)
                    Toast.makeText(this, getString(R.string.tracking_started_for, lastTask.taskName), Toast.LENGTH_SHORT).show()
                    updateCurrentTask(getString(R.string.currently_tracking, lastTask.taskName))
                } else {
                    Toast.makeText(this, getString(R.string.no_tasks_available_to_track), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.already_tracking_a_task), Toast.LENGTH_SHORT).show()
        }
    }

    fun startTrackingWithTask(taskName: String) {
        if (!timeTracker.isTracking()) {
            databaseManager.getAllTasks { tasks ->
                // Enhanced task matching: tries exact, partial, reverse partial, and word-by-word matching
                android.util.Log.d("VoiceCommand", "Looking for task: '$taskName'")
                android.util.Log.d("VoiceCommand", "Available tasks: ${tasks.map { "${it.taskName} (completed: ${it.isCompleted})" }}")
                
                var matchingTask = tasks.find { it.taskName.equals(taskName, ignoreCase = true) && !it.isCompleted }
                
                if (matchingTask == null) {
                    matchingTask = tasks.find { 
                        it.taskName.contains(taskName, ignoreCase = true) && !it.isCompleted 
                    }
                }
                
                if (matchingTask == null) {
                    matchingTask = tasks.find { task ->
                        !task.isCompleted && taskName.contains(task.taskName, ignoreCase = true)
                    }
                }
                
                if (matchingTask == null) {
                    val spokenWords = taskName.split(" ").filter { it.length > 2 }
                    matchingTask = tasks.find { task ->
                        !task.isCompleted && spokenWords.any { word -> 
                            task.taskName.contains(word, ignoreCase = true) 
                        }
                    }
                }
                
                if (matchingTask != null && !matchingTask.isCompleted) {
                    matchingTask.startTime = LocalDateTime.now()

                    databaseManager.updateTask(matchingTask) {
                        timeTracker.startTracking()
                        currentTaskName = matchingTask.taskName
                        currentTaskId = matchingTask.id
                        timeTracker.setCurrentTask(matchingTask.id, matchingTask.taskName)
                        Toast.makeText(this, getString(R.string.tracking_started_for, matchingTask.taskName), Toast.LENGTH_SHORT).show()
                        updateCurrentTask(getString(R.string.currently_tracking, matchingTask.taskName))
                    }
                } else if (matchingTask != null && matchingTask.isCompleted) {
                    Toast.makeText(this, getString(R.string.task_already_completed, matchingTask.taskName), Toast.LENGTH_SHORT).show()
                } else {
                    val availableTasks = tasks.filter { !it.isCompleted }.map { it.taskName }.joinToString(", ")
                    Toast.makeText(this, getString(R.string.task_not_found_available, taskName, availableTasks), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.already_tracking_a_task), Toast.LENGTH_SHORT).show()
        }
    }

    fun stopTracking() {
        val summary = timeTracker.stopTracking()
        if (summary == null) {
            Toast.makeText(this, getString(R.string.no_active_tracking_session), Toast.LENGTH_SHORT).show()
            return
        }

        val hoursElapsed = floor(summary.effectiveTimeMillis / (1000.0 * 60 * 60)).toLong()
        val minutesElapsed = ((summary.effectiveTimeMillis / (1000.0 * 60)) % 60).toLong()

        if (currentTaskId == -1L) {
            Toast.makeText(this, getString(R.string.no_task_was_being_tracked), Toast.LENGTH_SHORT).show()
            return
        }

        databaseManager.getTask(currentTaskId) { task ->
            if (task != null && !task.isCompleted) {
                task.isCompleted = true
                task.endTime = LocalDateTime.now()

                databaseManager.updateTask(task) {
                    Toast.makeText(
                        this,
                        getString(R.string.task_completed_time_tracked, task.taskName, hoursElapsed, minutesElapsed),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reset notification count when task is completed
                    notificationManager.resetNotificationCount(task.id)
                }
            } else {
                Toast.makeText(this, getString(R.string.could_not_find_tracked_task), Toast.LENGTH_SHORT).show()
            }
        }

        currentTaskName = null
        currentTaskId = -1L
        updateCurrentTask(getString(R.string.not_tracking_any_task))
    }

    fun startBreak() {
        if (!timeTracker.isTracking()) {
            Toast.makeText(this, getString(R.string.start_tracking_first), Toast.LENGTH_SHORT).show()
            return
        }

        if (timeTracker.isOnBreak()) {
            Toast.makeText(this, getString(R.string.already_on_a_break), Toast.LENGTH_SHORT).show()
        } else if (timeTracker.startBreak()) {
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val currentTask = tasks.last()
                    currentTask.isBreak = true

                    databaseManager.updateTask(currentTask) {
                        Toast.makeText(this, getString(R.string.break_started), Toast.LENGTH_SHORT).show()
                        updateCurrentTask(getString(R.string.on_break))
                    }
                }
            }
        }
    }

    fun endBreak() {
        if (!timeTracker.isTracking()) {
            Toast.makeText(this, getString(R.string.start_tracking_first), Toast.LENGTH_SHORT).show()
            return
        }

        if (!timeTracker.isOnBreak()) {
            Toast.makeText(this, getString(R.string.no_active_break), Toast.LENGTH_SHORT).show()
        } else if (timeTracker.endBreak()) {
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val currentTask = tasks.last()
                    currentTask.isBreak = false

                    databaseManager.updateTask(currentTask) {
                        Toast.makeText(this, getString(R.string.break_ended), Toast.LENGTH_SHORT).show()
                        updateCurrentTask(getString(R.string.currently_busy_with_task))
                    }
                }
            }
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

    fun showStatisticsPage() {
        val intent = Intent(this, StatisticsActivity::class.java)
        startActivity(intent)
    }

    fun getDatabaseManager(): DatabaseManager {
        return databaseManager
    }

    private var appliedTheme: Int = -1
    private var appliedLanguage: String = ""

    override fun onResume() {
        super.onResume()

        // Check if theme has changed and recreate if needed
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && appliedTheme != currentTheme) {
            android.util.Log.d("DashboardActivity", "Theme changed from $appliedTheme to $currentTheme - recreating activity")
            recreate()
            return
        }
        appliedTheme = currentTheme

        // Check if language has changed and recreate if needed
        val currentLanguage = LocaleHelper.getCurrentLanguage(this)
        if (appliedLanguage.isNotEmpty() && appliedLanguage != currentLanguage) {
            android.util.Log.d("DashboardActivity", "Language changed from $appliedLanguage to $currentLanguage - recreating activity")
            recreate()
            return
        }
        appliedLanguage = currentLanguage

        updateAllTasks()

        if (timeTracker.isTracking()) {
            val currentDuration = timeTracker.getCurrentDuration()
            timerText.text = formatTimeFromMillis(currentDuration)
            updateCurrentTaskDisplay()
        } else {
            mockStartTime = System.currentTimeMillis()
            updateCurrentTaskDisplay()
        }

        // Stop any running recognition to ensure clean state when returning from settings
        if (::voiceRecognizerManager.isInitialized) {
            voiceRecognizerManager.stopRecognition()
        }
        updateVoiceRecognitionStatus()
    }

    override fun onPause() {
        super.onPause()
        voiceRecognizerManager.stopRecognition()
        statusTextView.text = getString(R.string.voice_recognition_stopped)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        voiceRecognizerManager.stopRecognition()
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Fetching x,y,z values
            val (x, y, z) = event.values

            lastAcceleration = currentAcceleration

            // Getting current accelerations
            // with the help of fetched x,y,z values
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            val timeStamp = System.currentTimeMillis()
            gestureRecognizer.addSensorData(x, y, z, timeStamp)

            var analyzedGesture = GestureType.UNIDENTIFIED

            // Skip gesture detection if Doodle Jump game is running
            if (DoodleJumpActivity.isGameRunning()) {
                return
            }
            
            if (acceleration > shakeThreshold && !isDialogShown && !isActiveTask()) {
                updateAllTasks()
                isDialogShown = true
                analyzedGesture = gestureRecognizer.analyzeGesture()
                Toast.makeText(applicationContext, analyzedGesture.name, Toast.LENGTH_SHORT).show()
//                gestureAction(analyzedGesture, acceleration, isDialogShown, isActiveTask())
                createInputTaskDialog()
            }
            else if (acceleration > shakeThreshold && !isDialogShown && isActiveTask()){
                updateAllTasks()
                isDialogShown = true
                createExistingTaskDialog()
                analyzedGesture = gestureRecognizer.analyzeGesture()
                Toast.makeText(applicationContext, analyzedGesture.name, Toast.LENGTH_SHORT).show()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun isActiveTask(): Boolean{
        try {
            if(getActiveTask(this.allTasks) != null){
                return true
            }
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return false
    }

    private fun getActiveTask(allTasks: List<Task>): Task?{
        for (task in allTasks){
            if (task.endTime.isAfter(LocalDateTime.now()) && task.startTime.isBefore(LocalDateTime.now()) && !task.isCompleted){
                return task
            }
        }
        return null
    }

    private fun updateAllTasks(){
        databaseManager.getAllTasks { tasks -> this.allTasks = tasks }
        databaseManager.getAllTasksId { tasks -> this.allTasksId = tasks }
        this.activeTaskId = getIdOfActiveTask()
    }

    private fun getIdOfActiveTask(): Int{
        val tmpActiveTask = getActiveTask(this.allTasks)
        if (tmpActiveTask == null){
            return -1
        }
        else{
            val activeIndex = this.allTasks.indexOf(tmpActiveTask)
            val activeId = this.allTasksId.get(activeIndex)
            return activeId
        }
    }

    private fun createExistingTaskDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_existing_task)
        dialog.setCancelable(true)

        val activeTask = getActiveTask(this.allTasks)!!

        val dynamicTime = dialog.findViewById<TextView>(R.id.existing_remaining_time)
        val dayDif = activeTask.endTime.dayOfMonth - LocalDateTime.now().dayOfMonth
        val hourDif = activeTask.endTime.hour - LocalDateTime.now().hour
        val minDif = activeTask.endTime.minute - LocalDateTime.now().minute
        val secDif = activeTask.endTime.second - LocalDateTime.now().second

        val convertedValues = differenceConverter(dayDif, hourDif, minDif, secDif)

        val message = "${convertedValues[0]} days, ${convertedValues[1]} hours, ${convertedValues[2]} minutes and ${convertedValues[3]} seconds"

        dynamicTime.text = message

        // Add buttons for task operations
        val okButton = dialog.findViewById<Button>(R.id.existing_ok)
        val completeButton = dialog.findViewById<Button>(R.id.existing_complete)
        val deleteButton = dialog.findViewById<Button>(R.id.existing_delete)

        // Just dismiss the dialog
        okButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        // Mark task as completed
        completeButton?.setOnClickListener {
            val activeId = getIdOfActiveTask()
            //This dbmanager activity completes the task
            databaseManager.getTask(activeId.toLong()) { task ->
                if (task != null && !task.isCompleted) {
                    task.isCompleted = true
                    task.endTime = LocalDateTime.now()

                    databaseManager.updateTask(task) {
                        // Reset notification count when task is completed
                        notificationManager.resetNotificationCount(task.id)
                    }
                } else {
                    Toast.makeText(this, "Could not find the task", Toast.LENGTH_SHORT).show()
                }
            }

            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        // Delete the task
        deleteButton?.setOnClickListener {
            val activeID = getIdOfActiveTask()
            deleteTask(activeID.toLong())
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.setOnCancelListener {
            isDialogShown = false
            updateAllTasks()
        }

        dialog.show()
    }

    private fun createInputTaskDialog(){//First dialog in chain
        // Create Dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_new_task_step_1)
        dialog.setCancelable(true)

        val textInput = dialog.findViewById<EditText>(R.id.TaskInput)
        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)
        val startTaskCreation = dialog.findViewById<Button>(R.id.StartTaskCreation)
        val checkBox = dialog.findViewById<CheckBox>(R.id.checkBoxNewTask)

        startTaskCreation.setOnClickListener{
            var name = textInput.text.toString()
            if (name.isEmpty()){
                name = "Default task name"
            }
            val isPlannedAhead = checkBox.isChecked

            if (isPlannedAhead){
                dialog.dismiss()
                setDateDialog(name)
            }
            else{
                dialog.dismiss()
                setEndTimeDialog(name, LocalDateTime.now())
            }
            updateAllTasks()
        }

        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.setOnCancelListener {
            isDialogShown = false
            updateAllTasks()
        }

        dialog.show()
    }

    private fun setDateDialog(name: String){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_new_task_step_2)
        dialog.setCancelable(true)

        val datePicker = dialog.findViewById<DatePicker>(R.id.datePicker)
        val continueButton = dialog.findViewById<Button>(R.id.ContinueTaskCreationStep2)
        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)

        continueButton.setOnClickListener{
            var taskDate = LocalDate.of(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)

            dialog.dismiss()
            setStartTimeDialog(name, taskDate)
            updateAllTasks()
        }
        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.setOnCancelListener {
            isDialogShown = false
            updateAllTasks()
        }

        dialog.show()
    }

    private fun setStartTimeDialog(name: String, date: LocalDate){//Penultimate dialog in chain, if task is to be planed ahead
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_new_task_step_3)
        dialog.setCancelable(true)

        val startTime = dialog.findViewById<TimePicker>(R.id.timePicker)
        val continueButton = dialog.findViewById<Button>(R.id.ContinueTaskCreationStep3)
        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)

        continueButton.setOnClickListener{
            val time = LocalTime.of(startTime.hour, startTime.minute, 0)

            val dateTime = LocalDateTime.of(date, time)

            isDialogShown = false
            dialog.dismiss()
            setEndTimeDialog(name, dateTime, true)
            updateAllTasks()
        }

        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.setOnCancelListener {
            isDialogShown = false
            updateAllTasks()
        }

        dialog.show()
    }

    private fun setEndTimeDialog(name: String, dateTime: LocalDateTime, isPreplanned: Boolean = false){//Last dialog in chain
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_new_task_step_4)
        dialog.setCancelable(true)

        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)
        val submitTask = dialog.findViewById<Button>(R.id.EndTaskCreation)

        val hours = dialog.findViewById<EditText>(R.id.HoursInput)
        val minutes = dialog.findViewById<EditText>(R.id.MinutesInput)

        submitTask.setOnClickListener{
            var hoursSubmitted: Long = 3 //Possibly expand it in the settings
            var minutesSubmitted: Long = 0

            if (hours.text.isNotEmpty()){
                hoursSubmitted = (hours.text.toString()).toLong()
            }

            if (minutes.text.isNotEmpty()){
                minutesSubmitted = (minutes.text.toString()).toLong()
            }

            var estimatedCompletion = dateTime
            var startTime = dateTime

            if (hoursSubmitted >= 0){
                estimatedCompletion = estimatedCompletion.plusHours(hoursSubmitted)
                if (minutesSubmitted >= 0){
                    estimatedCompletion = estimatedCompletion.plusMinutes(minutesSubmitted)
                }
            }
            else{
                estimatedCompletion = estimatedCompletion.plusHours(3)
            }

            // Determine task timing status
            val timingStatus = when {
                LocalDateTime.now().isBefore(estimatedCompletion) -> TimingStatus.EARLY
                LocalDateTime.now().isAfter(estimatedCompletion) -> TimingStatus.LATE
                else -> TimingStatus.ON_TIME
            }

            val newTask = Task(
                name,
                startTime,
                estimatedCompletion,
                CreationMethod.Gesture,
                timingStatus, isPreplanned)

            databaseManager.createAppTask(newTask) { taskId ->
                Toast.makeText(this, "Task saved to database with ID: $taskId", Toast.LENGTH_SHORT).show()

                if (!isPreplanned) {
                    currentTaskName = name
                    currentTaskId = taskId
                    timeTracker.startTracking()
                    updateCurrentTaskDisplay()
                }
            }

            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.setOnCancelListener {
            isDialogShown = false
            updateAllTasks()
        }
        dialog.show()
    }

    private fun differenceConverter(day: Int, hour :Int, minute: Int, second: Int): List<Int> {
        var convertedDays = day
        var convertedHours = hour
        var convertedMinutes = minute
        var convertedSeconds = second

        if (convertedSeconds<0){
            convertedSeconds += 60

            convertedMinutes -= 1
        }
        if (convertedMinutes<0){
            convertedMinutes += 60

            convertedHours -= 1
        }
        if (convertedHours<0){
            convertedHours += 24

            convertedDays -= 1
        }
        if (convertedDays<0){
            convertedDays +=30
        }

        return listOf(convertedDays, convertedHours, convertedMinutes, convertedSeconds)
    }

    private fun deleteTask(taskId: Long) {
        databaseManager.deleteTask(taskId) {
            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSneeze() {
        //Implement sneeze logic here
        lastSneezeTime = System.currentTimeMillis()

        voiceRecognizerManager.sayBlessYou(this)
    }

    private fun startTaskChecker() {
        val handler = Handler(Looper.getMainLooper())
        val checkInterval = 60000L // Check every minute

        val taskChecker = object : Runnable {
            override fun run() {
                checkTasks()
                handler.postDelayed(this, checkInterval)
            }
        }

        handler.post(taskChecker)
    }

    private fun checkTasks() {
        databaseManager.getAllTasks { tasks ->
            tasks.forEach { task ->
                notificationManager.checkAndNotify(task)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun showTimesheetPage() {
        val intent = Intent(this, TimesheetActivity::class.java)
        startActivity(intent)
    }

    fun deleteTaskByName(taskName: String) {
        databaseManager.getAllTasks { tasks ->
            // Enhanced task matching: tries exact, partial, reverse partial, and word-by-word matching
            var task = tasks.find { it.taskName.equals(taskName, ignoreCase = true) }
            
            if (task == null) {
                task = tasks.find { it.taskName.contains(taskName, ignoreCase = true) }
            }
            
            if (task == null) {
                task = tasks.find { taskName.contains(it.taskName, ignoreCase = true) }
            }
            
            if (task == null) {
                val spokenWords = taskName.split(" ").filter { it.length > 2 }
                task = tasks.find { t ->
                    spokenWords.any { word -> t.taskName.contains(word, ignoreCase = true) }
                }
            }
            
            if (task != null) {
                databaseManager.deleteTask(task) {
                    Toast.makeText(this, "Task '${task.taskName}' deleted", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Could not find task with name '$taskName'. Available tasks: ${tasks.map { it.taskName }.joinToString(", ")}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun triggerEasterEgg() {
        // Launch the Doodle Jump game
        val intent = Intent(applicationContext, DoodleJumpActivity::class.java)
        applicationContext.startActivity(intent)
    }

    private fun gestureAction(gesture: GestureType, acceleration: Float, isDialogShown: Boolean, isActiveTask: Boolean){
        when (gesture){
            GestureType.UP -> triggerEasterEgg()
            GestureType.SHAKE -> showCorrectDialog(acceleration, isDialogShown, isActiveTask)
            GestureType.DOWN -> playPipeFallingEasterEgg() //Pipe falling sound as an easter egg
            GestureType.LEFT -> TODO() //Start audio rec
            GestureType.RIGHT -> TODO() //Start audio rec
            GestureType.CIRCLE -> return //Identification for squares and circles is not done. Possible extension in the future
            GestureType.SQUARE -> return //Identification for squares and circles is not done. Possible extension in the future
            else -> {
                Toast.makeText(applicationContext, "Something went wrong, try again", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    private fun showCorrectDialog(acceleration: Float, isDialogShown: Boolean, isActiveTask: Boolean){
        var analyzedGesture = GestureType.UNIDENTIFIED

        if (acceleration > shakeThreshold && !isDialogShown && !isActiveTask()) {
            updateAllTasks()
            this.isDialogShown = true
//            analyzedGesture = gestureRecognizer.analyzeGesture()
//            Toast.makeText(applicationContext, analyzedGesture.name, Toast.LENGTH_SHORT).show()
            createInputTaskDialog()
        }
        else if (acceleration > shakeThreshold && !isDialogShown && isActiveTask()){
            updateAllTasks()
            this.isDialogShown = true
            createExistingTaskDialog()
//            analyzedGesture = gestureRecognizer.analyzeGesture()
//            Toast.makeText(applicationContext, analyzedGesture.name, Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPipeFallingEasterEgg(){
        mediaPlayer.start()
    }
}