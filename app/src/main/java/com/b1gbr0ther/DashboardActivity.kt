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
import com.b1gbr0ther.data.database.DatabaseManager
import java.time.LocalDateTime
import java.util.Objects
import kotlin.math.floor
import kotlin.math.sqrt
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDate
import java.time.LocalTime

class DashboardActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var currentTaskText: TextView
    private lateinit var timeTracker: TimeTrackerInterface
    private lateinit var statusTextView: TextView
    private lateinit var simulateWakeWordButton: Button
    private lateinit var databaseManager: DatabaseManager
    private var currentTaskName: String? = null
    private var lastSneezeTime: Long = 0

    private lateinit var voiceRecognizerManager: VoiceRecognizerManager
    private lateinit var commandHandler: VoiceCommandHandler

    private var sensorManager: SensorManager? = null
    private var isDialogShown = false
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var allTasks: List<Task> = emptyList()
    private var allTasksId: List<Int> = emptyList()
    private var activeTaskId: Int = -1

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

    private fun updateCurrentTaskDisplay() {
        if (timeTracker.isTracking()) {
            if (timeTracker.isOnBreak()) {
                updateCurrentTask("On break")
            } else if (currentTaskName != null) {
                updateCurrentTask("Currently tracking: $currentTaskName")
            } else {
                databaseManager.getAllTasks { tasks ->
                    if (tasks.isNotEmpty()) {
                        val lastTask = tasks.last()
                        currentTaskName = lastTask.taskName
                        updateCurrentTask("Currently tracking: ${lastTask.taskName}")
                    }
                }
            }
        } else {
            currentTaskName = null
            updateCurrentTask("Not tracking any task")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        databaseManager = DatabaseManager(applicationContext)
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

        findViewById<Button>(R.id.btnDatabaseTest).setOnClickListener {
            val intent = Intent(this, DatabaseTestActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.b1gBr0therButton).setOnClickListener {
            //Used to be gesture activity. Change it to something else or nothing
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

        voiceRecognizerManager.setOnSneezeDetected {
            runOnUiThread {
                handleSneeze()
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
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val lastTask = tasks.last()
                    timeTracker.startTracking()
                    Toast.makeText(this, "Tracking started for: ${lastTask.taskName}", Toast.LENGTH_SHORT).show()
                    updateCurrentTask("Currently tracking: ${lastTask.taskName}")
                } else {
                    Toast.makeText(this, "No tasks available to track", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Already tracking a task", Toast.LENGTH_SHORT).show()
        }
    }

    fun startTrackingWithTask(taskName: String) {
        if (!timeTracker.isTracking()) {
            databaseManager.getAllTasks { tasks ->
                val matchingTask = tasks.find { it.taskName.equals(taskName, ignoreCase = true) }
                if (matchingTask != null && !matchingTask.isCompleted) {
                    timeTracker.startTracking()
                    currentTaskName = matchingTask.taskName
                    Toast.makeText(this, "Tracking started for: ${matchingTask.taskName}", Toast.LENGTH_SHORT).show()
                    updateCurrentTask("Currently tracking: ${matchingTask.taskName}")
                } else if (matchingTask != null && matchingTask.isCompleted) {
                    Toast.makeText(this, "Task '${matchingTask.taskName}' is already completed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Task '$taskName' not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Already tracking a task", Toast.LENGTH_SHORT).show()
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

        databaseManager.getAllTasks { tasks ->
            if (tasks.isNotEmpty()) {
                val lastTask = tasks.last()
                lastTask.isCompleted = true
                lastTask.endTime = LocalDateTime.now()

                databaseManager.updateTask(lastTask) {
                    Toast.makeText(
                        this,
                        "Task '${lastTask.taskName}' completed. Time tracked: ${hoursElapsed}h ${minutesElapsed}m",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        currentTaskName = null
        updateCurrentTask("Not tracking any task")
    }

    fun startBreak() {
        if (!timeTracker.isTracking()) {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
            return
        }

        if (timeTracker.isOnBreak()) {
            Toast.makeText(this, "Already on a break", Toast.LENGTH_SHORT).show()
        } else if (timeTracker.startBreak()) {
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val currentTask = tasks.last()
                    currentTask.isBreak = true
                    
                    databaseManager.updateTask(currentTask) {
                        Toast.makeText(this, "Break started", Toast.LENGTH_SHORT).show()
                        updateCurrentTask("On break")
                    }
                }
            }
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
            databaseManager.getAllTasks { tasks ->
                if (tasks.isNotEmpty()) {
                    val currentTask = tasks.last()
                    currentTask.isBreak = false
                    
                    databaseManager.updateTask(currentTask) {
                        Toast.makeText(this, "Break ended", Toast.LENGTH_SHORT).show()
                        updateCurrentTask("Currently busy with a task")
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

    fun getDatabaseManager(): DatabaseManager {
        return databaseManager
    }

    override fun onResume() {
        super.onResume()

        updateAllTasks()

        if (timeTracker.isTracking()) {
            val currentDuration = timeTracker.getCurrentDuration()
            timerText.text = formatTimeFromMillis(currentDuration)
            updateCurrentTaskDisplay()
        } else {
            mockStartTime = System.currentTimeMillis()
            updateCurrentTaskDisplay()
        }
        
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

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {

            // Fetching x,y,z values
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            // Getting current accelerations
            // with the help of fetched x,y,z values
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta


            if (acceleration > 17 && !isDialogShown && !isActiveTask()) {
                isDialogShown = true
                createInputTaskDialog()
            }
            else if (acceleration>17 &&!isDialogShown && isActiveTask()){
                isDialogShown = true
                createExistingTaskDialog()
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
            markTaskAsCompleted(activeTaskId.toLong())
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        // Delete the task
        deleteButton?.setOnClickListener {
            deleteTask(activeTaskId.toLong())
            isDialogShown = false
            dialog.dismiss()
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
            val taskDate = LocalDate.of(datePicker.year, datePicker.month, datePicker.dayOfMonth)

            dialog.dismiss()
            setStartTimeDialog(name, taskDate)
            updateAllTasks()
        }
        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
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
            setEndTimeDialog(name, dateTime)
            updateAllTasks()
        }

        cancelButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
            updateAllTasks()
        }

        dialog.show()
    }

    private fun setEndTimeDialog(name: String, dateTime: LocalDateTime){//Last dialog in chain
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_new_task_step_4)
        dialog.setCancelable(true)

        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)
        val submitTask = dialog.findViewById<Button>(R.id.EndTaskCreation)

        val hours = dialog.findViewById<EditText>(R.id.HoursInput)
        val minutes = dialog.findViewById<EditText>(R.id.MinutesInput)

        submitTask.setOnClickListener{
            val hoursSubmitted = (hours.text.toString()).toLong()
            val minutesSubmitted = (minutes.text.toString()).toLong()
            var estimatedCompletion = dateTime
            val startTime = dateTime

            if (hoursSubmitted >= 0){
                estimatedCompletion = estimatedCompletion.plusHours(hoursSubmitted)
                if (minutesSubmitted >= 0){
                    estimatedCompletion = estimatedCompletion.plusMinutes(minutesSubmitted)
                }
            }
            else{
                estimatedCompletion.plusHours(3)//default for task time
            }

            val newTask = Task(name, startTime, estimatedCompletion)//this creates the task

            // Save the task to the database
            databaseManager.createAppTask(newTask) { taskId ->
                Toast.makeText(this, "Task saved to database with ID: $taskId", Toast.LENGTH_SHORT).show()
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

    /**
     * Mark a task as completed in the database
     */
    private fun markTaskAsCompleted(taskId: Long) {
        databaseManager.getTask(taskId) { task ->
            if (task != null) {
                // Update the task
                task.isCompleted = true

                // Save the updated task
                databaseManager.updateTask(task) {
                    Toast.makeText(this, "Task marked as completed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Delete a task from the database
     */
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
}
