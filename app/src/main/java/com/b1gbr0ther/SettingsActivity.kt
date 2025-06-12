package com.b1gbr0ther

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var voiceRecognitionSwitch: Switch
    private lateinit var notificationsSwitch: Switch
    private lateinit var accelerometerSwitch: Switch
    private lateinit var blowDetectionSwitch: Switch
    private lateinit var sneezeDetectionSwitch: Switch
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeValueText: TextView
    private lateinit var sensitivitySeekBar: SeekBar
    private lateinit var sensitivityValueText: TextView
    private lateinit var darkThemeSwitch: Switch
    private lateinit var themeLabel: TextView
    private lateinit var languageSwitch: Switch
    private lateinit var languageLabel: TextView
    private lateinit var backButton: Button
    private lateinit var resetButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
        
        initializeViews()
        loadSettings()
        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        voiceRecognitionSwitch = findViewById(R.id.voiceRecognitionSwitch)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        accelerometerSwitch = findViewById(R.id.accelerometerSwitch)
        blowDetectionSwitch = findViewById(R.id.blowDetectionSwitch)
        sneezeDetectionSwitch = findViewById(R.id.sneezeDetectionSwitch)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        volumeValueText = findViewById(R.id.volumeValueText)
        sensitivitySeekBar = findViewById(R.id.sensitivitySeekBar)
        sensitivityValueText = findViewById(R.id.sensitivityValueText)
        darkThemeSwitch = findViewById(R.id.darkThemeSwitch)
        themeLabel = findViewById(R.id.themeLabel)
        languageSwitch = findViewById(R.id.languageSwitch)
        languageLabel = findViewById(R.id.languageLabel)
        backButton = findViewById(R.id.backButton)
        resetButton = findViewById(R.id.resetButton)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSettings() {
        // Load saved settings or use defaults
        voiceRecognitionSwitch.isChecked = sharedPreferences.getBoolean("voice_recognition", true)
        notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications", true)
        accelerometerSwitch.isChecked = sharedPreferences.getBoolean("accelerometer", true)
        blowDetectionSwitch.isChecked = sharedPreferences.getBoolean("blow_detection", true)
        sneezeDetectionSwitch.isChecked = sharedPreferences.getBoolean("sneeze_detection", true)
        
        val volume = sharedPreferences.getInt("volume", 50)
        volumeSeekBar.progress = volume
        volumeValueText.text = "$volume%"
        
        val sensitivity = sharedPreferences.getInt("sensitivity", 75)
        sensitivitySeekBar.progress = sensitivity
        sensitivityValueText.text = "$sensitivity%"
        
        darkThemeSwitch.isChecked = sharedPreferences.getBoolean("dark_theme", false)
        updateThemeLabel()
        languageSwitch.isChecked = sharedPreferences.getBoolean("dutch_language", false)
        updateLanguageLabel()
    }

    private fun updateThemeLabel() {
        themeLabel.text = if (darkThemeSwitch.isChecked) {
            "Theme: Dark Mode"
        } else {
            "Theme: Light Mode"
        }
    }

    private fun updateLanguageLabel() {
        languageLabel.text = if (languageSwitch.isChecked) {
            "Language: Dutch"
        } else {
            "Language: English"
        }
    }

    private fun setupListeners() {
        // Volume SeekBar listener
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Sensitivity SeekBar listener
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivityValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Dark theme switch listener
        darkThemeSwitch.setOnCheckedChangeListener { _, _ ->
            updateThemeLabel()
        }

        // Language switch listener
        languageSwitch.setOnCheckedChangeListener { _, _ ->
            updateLanguageLabel()
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Reset button
        resetButton.setOnClickListener {
            resetToDefaults()
        }

        // Save button
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun resetToDefaults() {
        voiceRecognitionSwitch.isChecked = true
        notificationsSwitch.isChecked = true
        accelerometerSwitch.isChecked = true
        blowDetectionSwitch.isChecked = true
        sneezeDetectionSwitch.isChecked = true
        volumeSeekBar.progress = 50
        volumeValueText.text = "50%"
        sensitivitySeekBar.progress = 75
        sensitivityValueText.text = "75%"
        darkThemeSwitch.isChecked = false
        updateThemeLabel()
        languageSwitch.isChecked = false
        updateLanguageLabel()
        
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("voice_recognition", voiceRecognitionSwitch.isChecked)
        editor.putBoolean("notifications", notificationsSwitch.isChecked)
        editor.putBoolean("accelerometer", accelerometerSwitch.isChecked)
        editor.putBoolean("blow_detection", blowDetectionSwitch.isChecked)
        editor.putBoolean("sneeze_detection", sneezeDetectionSwitch.isChecked)
        editor.putInt("volume", volumeSeekBar.progress)
        editor.putInt("sensitivity", sensitivitySeekBar.progress)
        editor.putBoolean("dark_theme", darkThemeSwitch.isChecked)
        editor.putBoolean("dutch_language", languageSwitch.isChecked)
        editor.apply()
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    companion object {
        // Static methods to access settings from other activities
        fun isVoiceRecognitionEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("voice_recognition", true)
        }

        fun isNotificationsEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("notifications", true)
        }

        fun isAccelerometerEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("accelerometer", true)
        }

        fun isBlowDetectionEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("blow_detection", true)
        }

        fun isSneezeDetectionEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("sneeze_detection", true)
        }

        fun getVolume(sharedPreferences: SharedPreferences): Int {
            return sharedPreferences.getInt("volume", 50)
        }

        fun getSensitivity(sharedPreferences: SharedPreferences): Int {
            return sharedPreferences.getInt("sensitivity", 75)
        }

        fun isDarkThemeEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("dark_theme", false)
        }

        fun isDutchLanguageEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("dutch_language", false)
        }
    }
} 