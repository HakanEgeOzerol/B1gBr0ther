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
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period

class HandGesturesActivity : ComponentActivity() {
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private var sensorManager: SensorManager? = null

    private var isDialogShown = false

    private lateinit var lastTask: Task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gesture)

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

            this.lastTask = Task(name, startTime, estimatedCompletion)//This creates the task
//            Do something with the input text
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

        dynamicTime.text =message

        val okButton = dialog.findViewById<Button>(R.id.existing_ok)
        okButton.setOnClickListener{
            isDialogShown = false
            dialog.dismiss()
        }

        dialog.show()
    }

    fun getLastTask(): Task {
        return this.lastTask
    }
}