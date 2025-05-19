package com.b1gbr0ther

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DashboardActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var currentTaskText: TextView


    fun getCurrentWorkTime(): String {
        // This is a temporary function. Replace this later on with the real stuff.
        val mockSeconds = ((System.currentTimeMillis() / 1000) % 3600).toInt()
        val minutes = mockSeconds / 60
        val seconds = mockSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun updateCurrentTask(name: String) {
        currentTaskText.text = name
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTaskText = findViewById(R.id.currentTaskText)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        timerText = findViewById(R.id.timer)

        timerRunnable = object : Runnable {
            override fun run() {
                val currentTime = getCurrentWorkTime()
                timerText.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable)

    }
}
