package com.b1gbr0ther

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
        
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

        // Updated to use ThemeManager instead of old boolean preference
        fun isDarkThemeEnabled(context: android.content.Context): Boolean {
            return ThemeManager.getCurrentTheme(context) == ThemeManager.THEME_DARK
        }

        fun isDutchLanguageEnabled(sharedPreferences: SharedPreferences): Boolean {
            return sharedPreferences.getBoolean("dutch_language", false)
        }

        fun applyTheme(activity: android.app.Activity) {
            ThemeManager.applyTheme(activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme BEFORE setting content view
        ThemeManager.applyTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
        
        initializeViews()
        loadSettings()
        setupListeners()
        
        // Debug: Log current theme colors
        debugThemeColors()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check initial permission state
        updateVoiceRecognitionSwitchState()
    }

    private fun applyTheme() {
        ThemeManager.applyTheme(this)
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
        android.util.Log.d("SettingsActivity", "Loading settings from SharedPreferences...")
        
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
        
        android.util.Log.d("SettingsActivity", "Loaded settings: volume=$volume, sensitivity=$sensitivity")
        
        // Load theme setting using ThemeManager (temporarily remove listener to prevent infinite loop)
        val currentTheme = ThemeManager.getCurrentTheme(this)
        android.util.Log.d("SettingsActivity", "Loading settings - current theme: ${ThemeManager.getThemeName(currentTheme)} ($currentTheme)")
        darkThemeSwitch.setOnCheckedChangeListener(null)
        darkThemeSwitch.isChecked = (currentTheme == ThemeManager.THEME_DARK)
        updateThemeLabel()
        
        // Re-attach the listener after setting the state
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            android.util.Log.d("SettingsActivity", "Theme switch toggled: $isChecked")
            android.util.Log.d("SettingsActivity", "BEFORE theme change:")
            debugThemeColors()
            
            val themeMode = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.setTheme(this, themeMode)
            updateThemeLabel()
            
            recreate()
        }
        
        languageSwitch.isChecked = sharedPreferences.getBoolean("dutch_language", false)
        updateLanguageLabel()
    }

    private fun updateThemeLabel() {
        val currentTheme = ThemeManager.getCurrentTheme(this)
        themeLabel.text = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> "Theme: Light Mode (Purple)"
            ThemeManager.THEME_DARK -> "Theme: Dark Mode (Gray)"
            else -> "Theme: Light Mode (Purple)"
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

        // Dark theme switch listener is now set in loadSettings() to prevent infinite loop

        // Language switch listener
        languageSwitch.setOnCheckedChangeListener { _, _ ->
            updateLanguageLabel()
        }

        // Voice recognition switch listener
        voiceRecognitionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestVoiceRecognitionPermission()
            } else {
                // If turning off, just save the setting
                saveVoiceRecognitionSetting(false)
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            resetToDefaults()
        }

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun requestVoiceRecognitionPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                saveVoiceRecognitionSetting(true)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_RECORD_AUDIO
                )
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Microphone Permission Required")
            .setMessage("Voice recognition requires microphone access to work. Would you like to grant this permission?")
            .setPositiveButton("Yes") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_RECORD_AUDIO
                )
            }
            .setNegativeButton("No") { _, _ ->
                voiceRecognitionSwitch.isChecked = false
                saveVoiceRecognitionSetting(false)
            }
            .create()
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Voice recognition cannot work without microphone access. Would you like to open settings to grant the permission?")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                voiceRecognitionSwitch.isChecked = false
                saveVoiceRecognitionSetting(false)
            }
            .create()
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveVoiceRecognitionSetting(true)
                    Toast.makeText(this, "Voice recognition enabled", Toast.LENGTH_SHORT).show()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        // User clicked "Don't ask again", show settings dialog
                        showPermissionDeniedDialog()
                    } else {
                        // User just denied once
                        voiceRecognitionSwitch.isChecked = false
                        saveVoiceRecognitionSetting(false)
                        Toast.makeText(this, "Voice recognition disabled: Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveVoiceRecognitionSetting(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("voice_recognition", enabled).apply()
    }

    private fun updateVoiceRecognitionSwitchState() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val isEnabled = sharedPreferences.getBoolean("voice_recognition", true)
        
        // Temporarily remove listener to prevent triggering other switches
        voiceRecognitionSwitch.setOnCheckedChangeListener(null)
        voiceRecognitionSwitch.isChecked = isEnabled && hasPermission
        
        // Re-attach the listener
        voiceRecognitionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestVoiceRecognitionPermission()
            } else {
                saveVoiceRecognitionSetting(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update switch state when returning from Settings
        updateVoiceRecognitionSwitchState()
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
        // Temporarily remove listener to prevent triggering recreation
        darkThemeSwitch.setOnCheckedChangeListener(null)
        darkThemeSwitch.isChecked = false
        ThemeManager.setTheme(this, ThemeManager.THEME_LIGHT)
        updateThemeLabel()
        
        // Re-attach the listener
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            android.util.Log.d("SettingsActivity", "Theme switch toggled: $isChecked")
            val themeMode = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            ThemeManager.setTheme(this, themeMode)
            updateThemeLabel()
            recreate()
        }
        languageSwitch.isChecked = false
        updateLanguageLabel()
        
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }

    private fun saveSettings() {
        android.util.Log.d("SettingsActivity", "Save button pressed - saving settings...")
        
        val editor = sharedPreferences.edit()
        editor.putBoolean("voice_recognition", voiceRecognitionSwitch.isChecked)
        editor.putBoolean("notifications", notificationsSwitch.isChecked)
        editor.putBoolean("accelerometer", accelerometerSwitch.isChecked)
        editor.putBoolean("blow_detection", blowDetectionSwitch.isChecked)
        editor.putBoolean("sneeze_detection", sneezeDetectionSwitch.isChecked)
        editor.putInt("volume", volumeSeekBar.progress)
        editor.putInt("sensitivity", sensitivitySeekBar.progress)
        // Theme is handled by ThemeManager, no need to save it here
        editor.putBoolean("dutch_language", languageSwitch.isChecked)
        editor.apply()
        
        android.util.Log.d("SettingsActivity", "Settings saved: volume=${volumeSeekBar.progress}, sensitivity=${sensitivitySeekBar.progress}")
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }
    
    private fun debugThemeColors() {
        try {
            val typedArray = theme.obtainStyledAttributes(intArrayOf(
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary
            ))
            
            val backgroundColor = typedArray.getColor(0, 0)
            val textColor = typedArray.getColor(1, 0)
            
            typedArray.recycle()
            
            android.util.Log.d("SettingsActivity", "=== THEME DEBUG ===")
            android.util.Log.d("SettingsActivity", "Background: #${Integer.toHexString(backgroundColor).uppercase()}")
            android.util.Log.d("SettingsActivity", "Text: #${Integer.toHexString(textColor).uppercase()}")
            android.util.Log.d("SettingsActivity", "================")
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error debugging theme colors: ${e.message}")
        }
    }

} 