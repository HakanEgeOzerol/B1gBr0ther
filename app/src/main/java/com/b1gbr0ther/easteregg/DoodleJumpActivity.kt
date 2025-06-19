package com.b1gbr0ther.easteregg

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.b1gbr0ther.LocaleHelper
import com.b1gbr0ther.R
import com.b1gbr0ther.ThemeManager

class DoodleJumpActivity : AppCompatActivity() {

    private lateinit var gameView: DoodleJumpGameView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        
        enableEdgeToEdge()
        
        gameView = DoodleJumpGameView(this)
        setContentView(gameView)

        ViewCompat.setOnApplyWindowInsetsListener(gameView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
} 