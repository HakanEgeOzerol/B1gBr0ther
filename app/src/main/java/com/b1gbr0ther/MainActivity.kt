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
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnDatabaseTest = findViewById<Button>(R.id.btnDatabaseTest)

        goToButton.setOnClickListener {
//            Used to send to handGesture activity. Not anymore. The entire class needs to go
        }

        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        btnDatabaseTest.setOnClickListener {
            val intent = Intent(this, DatabaseTestActivity::class.java)
            startActivity(intent)
        }
    }
}
