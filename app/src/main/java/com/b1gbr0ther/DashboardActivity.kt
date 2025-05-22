package com.b1gbr0ther

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
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
        //Can also just make this hours, minutes. since you will prolly work on stuff for a long time
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

        val menu = findViewById<MenuBar>(R.id.menuBar)
        menu.setActivePage(1) // 0 is for export, 1 is for Dashboard, 2 is for calender/timesheet

        //makes sure nothing gets drawn behind the top notification/wifi bar nor the android nav bar at the bottom
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
                timerText.text = getCurrentWorkTime()
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable)

        // Set up click listener for database test button
        findViewById<Button>(R.id.btnDatabaseTest).setOnClickListener {
            val intent = Intent(this, DatabaseTestActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.b1gBr0therButton).setOnClickListener {
            val intent = Intent(this, HandGesturesActivity::class.java)
            startActivity(intent)
        }
    }
}