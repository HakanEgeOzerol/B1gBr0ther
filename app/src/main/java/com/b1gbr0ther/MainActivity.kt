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

        goToButton.setOnClickListener {
            val intent = Intent(this, HandGesturesActivity::class.java)
            startActivity(intent)
        }

        audioRecognitionButton.setOnClickListener {
            val intent = Intent(this, AudioRecognitionActivity::class.java)
            startActivity(intent)
        }
    }
}