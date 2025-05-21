package com.b1gbr0ther

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DashboardActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private lateinit var currentTaskText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        timerText = findViewById(R.id.timer)
        currentTaskText = findViewById(R.id.currentTaskText)

        TimeTracker.getInstance(applicationContext)

        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(2) // 2 is for Dashboard

        // Makes sure nothing gets drawn behind the top notification/wifi bar nor the android nav bar at the bottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup the WeekTimeGridView with real data
        val chart = findViewById<WeekTimeGridView>(R.id.weekGrid)
        chart.setWorkData(getDummyWorkData())

        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }

        // Start the timer update
        handler.post(timerRunnable!!)
    }

    private fun updateTimerDisplay() {
        if (TimeTracker.isTracking()) {
            val elapsedTime = TimeTracker.getCurrentEffectiveTime()
            timerText.text = formatTime(elapsedTime)
            updateStatusText()
        } else {
            timerText.text = "00:00"
            currentTaskText.text = getString(R.string.status_not_tracking)
        }
    }

    private fun updateStatusText() {
        val statusText = if (TimeTracker.isOnBreak()) {
            getString(R.string.status_on_break)
        } else {
            getString(R.string.status_working)
        }
        currentTaskText.text = statusText
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

    private fun getDummyWorkData(): List<WorkBlock> {
        // This is placeholder data - later you can generate this from TimeTracker history
        return listOf(
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
    }

    override fun onPause() {
        super.onPause()
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        timerRunnable?.let { handler.post(it) }

        // If tracking is active, update the UI immediately
        if (TimeTracker.isTracking()) {
            updateTimerDisplay()
        }
    }
}
