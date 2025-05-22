package com.b1gbr0ther

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*
import kotlin.math.sqrt
import android.app.Dialog
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDateTime
import com.b1gbr0ther.data.database.DatabaseManager

class HandGesturesActivity : ComponentActivity() {
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private var sensorManager: SensorManager? = null
    private lateinit var databaseManager: DatabaseManager

    private var isDialogShown = false

    private lateinit var lastTask: Task
    private var lastTaskId: Long = -1 // Store the database ID of the last task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gesture)

        // Initialize the database manager
        databaseManager = (application as B1gBr0therApplication).databaseManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val goBackButton = findViewById<Button>(R.id.BackButton)
        goBackButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)

        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        // Load the last task from the database if it exists
        loadLastTask()
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


            if (acceleration > 17 && !isDialogShown && ::lastTask.isInitialized) {
                isDialogShown = true
                createExistingTaskDialog()
            }
            else if (acceleration > 17 && !isDialogShown && !::lastTask.isInitialized){
                isDialogShown = true
                createInputTaskDialog()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }

    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    /**
     * Load the last created task from the database
     */
    private fun loadLastTask() {
        databaseManager.getAllTasks { tasks ->
            if (tasks.isNotEmpty()) {
                // Get the most recently created task (assuming it's the last one in the list)
                val lastDbTask = tasks.last()
                lastTaskId = lastDbTask.id
                
                // Convert to app Task model
                lastTask = lastDbTask.toAppTask()
                
                Toast.makeText(this, "Loaded last task: ${lastTask.getName()}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createInputTaskDialog(){
        // Create Dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_text_input)
        dialog.setCancelable(true)

        val textInput = dialog.findViewById<EditText>(R.id.TaskInput)
        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)
        val startTracking = dialog.findViewById<Button>(R.id.SubmitTask)
        val hours = dialog.findViewById<EditText>(R.id.HourInput)
        val minutes = dialog.findViewById<EditText>(R.id.MinuteInput)

        startTracking.setOnClickListener{
            var name = textInput.text.toString()
            var hoursSubmitted = (hours.text.toString()).toLong()
            var minutesSubmitted = (minutes.text.toString()).toLong()

            var estimatedCompletion = LocalDateTime.now()

            var startTime = LocalDateTime.now()

            if (hoursSubmitted >= 0){
                estimatedCompletion = estimatedCompletion.plusHours(hoursSubmitted)
                if (minutesSubmitted >= 0){
                    estimatedCompletion = estimatedCompletion.plusMinutes(minutesSubmitted)
                }
            }
            else{
                estimatedCompletion.plusHours(3)//default for task time
            }

            // Create the task in memory
            this.lastTask = Task(name, startTime, estimatedCompletion)//this creates the task
            
            // Save the task to the database
            databaseManager.createAppTask(this.lastTask) { taskId ->
                lastTaskId = taskId
                Toast.makeText(this, "Task saved to database with ID: $taskId", Toast.LENGTH_SHORT).show()
            }
            
            isDialogShown = false
            dialog.dismiss()
        }

        cancelButton.setOnClickListener{
//            do something is dismissed

            isDialogShown = false
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createExistingTaskDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.gesture_dialog_existing_task)
        dialog.setCancelable(true)

        val dynamicTime = dialog.findViewById<TextView>(R.id.existing_remaining_time)
        val hourDif = getLastTask().getEndTime().hour - LocalDateTime.now().hour
        val minDif = getLastTask().getEndTime().minute - LocalDateTime.now().minute

        val message = "$hourDif hours and $minDif minutes"

        dynamicTime.text = message

        // Add buttons for task operations
        val okButton = dialog.findViewById<Button>(R.id.existing_ok)
        val completeButton = dialog.findViewById<Button>(R.id.existing_complete)
        val deleteButton = dialog.findViewById<Button>(R.id.existing_delete)

        // Just dismiss the dialog
        okButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
        }

        // Mark task as completed
        completeButton?.setOnClickListener {
            if (lastTaskId > 0) {
                markTaskAsCompleted(lastTaskId)
            }
            isDialogShown = false
            dialog.dismiss()
        }

        // Delete the task
        deleteButton?.setOnClickListener {
            if (lastTaskId > 0) {
                deleteTask(lastTaskId)
            }
            isDialogShown = false
            dialog.dismiss()
        }

        dialog.show()
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
                    // Update the in-memory task
                    lastTask = task.toAppTask()
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
            // Clear the in-memory task reference
            if (::lastTask.isInitialized) {
                // We need to find a new last task
                loadLastTask()
            }
        }
    }

    fun getLastTask(): Task {
        return this.lastTask
    }
}