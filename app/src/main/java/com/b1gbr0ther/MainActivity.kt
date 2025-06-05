package com.b1gbr0ther

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val goToButton = findViewById<Button>(R.id.goToButton)
        val audioRecognitionButton = findViewById<Button>(R.id.audioRecognitionButton)
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnDatabaseTest = findViewById<Button>(R.id.btnDatabaseTest)
        val btnStatistics = findViewById<Button>(R.id.btnStatistics)

        goToButton.setOnClickListener {
            val intent = Intent(this, HandGesturesActivity::class.java)
            startActivity(intent)
        }

        audioRecognitionButton.setOnClickListener {
            val intent = Intent(this, AudioRecognitionActivity::class.java)
            startActivity(intent)
        }

        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
        
        btnDatabaseTest.setOnClickListener {
            val intent = Intent(this, DatabaseTesterActivity::class.java)
            startActivity(intent)
        }
        
        btnStatistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }
}