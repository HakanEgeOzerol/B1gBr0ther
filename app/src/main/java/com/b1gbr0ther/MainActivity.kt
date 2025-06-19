package com.b1gbr0ther

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import android.content.Context

class MainActivity : AppCompatActivity() {
    private var appliedTheme: Int = -1
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved app theme before setting content view
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val goToButton = findViewById<Button>(R.id.goToButton)
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)
        val btnDatabaseTest = findViewById<Button>(R.id.btnDatabaseTest)
        val btnStatistics = findViewById<Button>(R.id.btnStatistics)

        goToButton.setOnClickListener {
//            Used to send to handGesture activity. Not anymore. The entire class needs to go
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
    
    override fun onResume() {
        super.onResume()
        
        // Check if theme has changed and recreate if needed
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && appliedTheme != currentTheme) {
            android.util.Log.d("MainActivity", "Theme changed from $appliedTheme to $currentTheme - recreating activity")
            recreate()
            return
        }
        appliedTheme = currentTheme
    }
}
