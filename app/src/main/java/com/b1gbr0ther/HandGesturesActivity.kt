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

class HandGesturesActivity : ComponentActivity() {
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private var sensorManager: SensorManager? = null

    private var isDialogShown = false;

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

            if (acceleration > 17 && !isDialogShown) {
                isDialogShown = true
                showTaskDialog()
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

    private fun showTaskDialog(){
        // Create Dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_text_input)
        dialog.setCancelable(true)

        val textInput = dialog.findViewById<EditText>(R.id.TaskInput)
        val cancelButton = dialog.findViewById<Button>(R.id.Cancel)
        val submitTask = dialog.findViewById<Button>(R.id.SubmitTask)

        submitTask.setOnClickListener{
//            var textInput = textInput.text.toString()
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
}