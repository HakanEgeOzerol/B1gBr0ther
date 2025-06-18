package com.b1gbr0ther

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManualPage : AppCompatActivity() {
    private lateinit var menuBar: MenuBar
    private var appliedTheme: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual)

        menuBar = findViewById(R.id.menuBar)
        menuBar.setActivePage(1)
        menuBar.bringToFront()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if theme has changed and recreate if needed
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && appliedTheme != currentTheme) {
            android.util.Log.d("ManualPage", "Theme changed from $appliedTheme to $currentTheme - recreating activity")
            recreate()
            return
        }
        appliedTheme = currentTheme
        
        menuBar.setActivePage(1)
    }
}
