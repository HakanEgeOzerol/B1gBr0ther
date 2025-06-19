package com.b1gbr0ther

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple launcher activity for the database tester.
 * Provides a button to navigate to the DatabaseTesterActivity.
 */
class DatabaseLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        
        setContentView(R.layout.activity_database_launcher)
        
        // Set up the button to launch the database tester
        findViewById<Button>(R.id.btnLaunchDatabaseTester).setOnClickListener {
            startActivity(Intent(this, DatabaseTesterActivity::class.java))
        }
    }
}
