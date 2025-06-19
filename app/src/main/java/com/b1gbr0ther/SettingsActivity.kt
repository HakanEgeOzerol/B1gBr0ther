package com.b1gbr0ther

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import android.widget.RadioGroup
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.content.Context

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var audioModeRadioGroup: RadioGroup
    private lateinit var radioAudioOff: RadioButton
    private lateinit var radioVoiceCommands: RadioButton
    private lateinit var radioSoundDetection: RadioButton
    private lateinit var notificationsSwitch: Switch
    private lateinit var accelerometerSwitch: Switch
    private lateinit var blowDetectionSwitch: Switch
    private lateinit var sneezeDetectionSwitch: Switch

    private lateinit var darkThemeSwitch: Switch
    private lateinit var themeLabel: TextView
    private lateinit var languageButton: Button
    private lateinit var languageLabel: TextView
    private lateinit var backButton: Button
    private lateinit var resetButton: Button
    private lateinit var saveButton: Button

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
        private const val PERMISSION_REQUEST_NOTIFICATIONS = 1002
        
        // Audio mode constants
        const val AUDIO_MODE_OFF = 0
        const val AUDIO_MODE_VOICE_COMMANDS = 1  
        const val AUDIO_MODE_SOUND_DETECTION = 2
        
        // Static methods to access settings from other activities
        fun getAudioMode(sharedPreferences: SharedPreferences): Int {
            val mode = sharedPreferences.getInt("audio_mode", AUDIO_MODE_OFF)
            android.util.Log.d("SettingsActivity", "getAudioMode() returning: $mode")
            return mode
        }
        
        fun isVoiceRecognitionEnabled(sharedPreferences: SharedPreferences): Boolean {
            return getAudioMode(sharedPreferences) == AUDIO_MODE_VOICE_COMMANDS
        }

        fun isSoundDetectionMode(sharedPreferences: SharedPreferences): Boolean {
            return getAudioMode(sharedPreferences) == AUDIO_MODE_SOUND_DETECTION
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



        // Updated to use ThemeManager instead of old boolean preference
        fun isDarkThemeEnabled(context: android.content.Context): Boolean {
            return ThemeManager.getCurrentTheme(context) == ThemeManager.THEME_DARK
        }

        fun getCurrentLanguage(context: android.content.Context): String {
            return LocaleHelper.getCurrentLanguage(context)
        }
        
        fun isDutchLanguageEnabled(context: android.content.Context): Boolean {
            return LocaleHelper.getCurrentLanguage(context) == "nl"
        }

        fun applyTheme(activity: android.app.Activity) {
            ThemeManager.applyTheme(activity)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme BEFORE setting content view
        ThemeManager.applyTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        sharedPreferences = getSharedPreferences("B1gBr0therSettings", MODE_PRIVATE)
        
        initializeViews()
        setupListeners()
        debugCurrentSettings("BEFORE enforceSettingsConsistency")
        enforceSettingsConsistency()
        debugCurrentSettings("AFTER enforceSettingsConsistency")
        loadSettings()
        debugCurrentSettings("AFTER loadSettings")
        
        // Debug: Log current theme colors
        debugThemeColors()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // REMOVED: updateVoiceRecognitionSwitchState() - this was interfering with our mutual exclusivity setup
        // Permission handling now happens only when user tries to enable a mode
    }

    private fun applyTheme() {
        ThemeManager.applyTheme(this)
    }

    private fun initializeViews() {
        audioModeRadioGroup = findViewById(R.id.audioModeRadioGroup)
        radioAudioOff = findViewById(R.id.radioAudioOff)
        radioVoiceCommands = findViewById(R.id.radioVoiceCommands)
        radioSoundDetection = findViewById(R.id.radioSoundDetection)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        accelerometerSwitch = findViewById(R.id.accelerometerSwitch)
        blowDetectionSwitch = findViewById(R.id.blowDetectionSwitch)
        sneezeDetectionSwitch = findViewById(R.id.sneezeDetectionSwitch)
        darkThemeSwitch = findViewById(R.id.darkThemeSwitch)
        themeLabel = findViewById(R.id.themeLabel)
        languageButton = findViewById(R.id.languageButton)
        languageLabel = findViewById(R.id.languageLabel)
        backButton = findViewById(R.id.backButton)
        resetButton = findViewById(R.id.resetButton)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSettings() {
        android.util.Log.d("SettingsActivity", "Loading settings from SharedPreferences...")
        
        val audioMode = getAudioMode(sharedPreferences)
        android.util.Log.d("SettingsActivity", "Loading audio mode: $audioMode")
        
        // Set the radio button based on saved mode
        audioModeRadioGroup.clearCheck() // Clear all first
        when (audioMode) {
            AUDIO_MODE_OFF -> radioAudioOff.isChecked = true
            AUDIO_MODE_VOICE_COMMANDS -> radioVoiceCommands.isChecked = true
            AUDIO_MODE_SOUND_DETECTION -> radioSoundDetection.isChecked = true
            else -> radioAudioOff.isChecked = true // Default to off
        }
        
        notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications", true)
        accelerometerSwitch.isChecked = sharedPreferences.getBoolean("accelerometer", true)
        blowDetectionSwitch.isChecked = sharedPreferences.getBoolean("blow_detection", true)
        sneezeDetectionSwitch.isChecked = sharedPreferences.getBoolean("sneeze_detection", true)
        
        // Load theme setting using ThemeManager (temporarily remove listener to prevent infinite loop)
        val currentTheme = ThemeManager.getCurrentTheme(this)
        android.util.Log.d("SettingsActivity", "Loading settings - current theme: ${ThemeManager.getThemeName(currentTheme)} ($currentTheme)")
        darkThemeSwitch.setOnCheckedChangeListener(null)
        darkThemeSwitch.isChecked = (currentTheme == ThemeManager.THEME_DARK)
        updateThemeLabel()
        
        // Re-attach theme listener
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) ThemeManager.THEME_DARK else ThemeManager.THEME_LIGHT
            android.util.Log.d("SettingsActivity", "Theme switch changed: isChecked=$isChecked -> theme=$newTheme")
            ThemeManager.setTheme(this, newTheme)
            // Recreate activity to immediately apply the new theme
            recreate()
        }
        
        updateLanguageLabel()
    }

    private fun updateThemeLabel() {
        val currentTheme = ThemeManager.getCurrentTheme(this)
        themeLabel.text = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> getString(R.string.theme_light_mode)
            ThemeManager.THEME_DARK -> getString(R.string.theme_dark_mode)
            else -> getString(R.string.theme_light_mode)
        }
    }

    private fun updateLanguageLabel() {
        val currentLang = LocaleHelper.getCurrentLanguage(this)
        val langName = LocaleHelper.getCurrentLanguageDisplayName(this)
        languageLabel.text = getString(R.string.language_label, langName)
        
        // Update button text to show current language
        languageButton.text = langName
    }

    private fun setupListeners() {
        setupMainListeners()
        
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermission()
            }
        }
        
        accelerometerSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("accelerometer", isChecked).apply()
        }
        
        blowDetectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("blow_detection", isChecked).apply()
        }
        
        sneezeDetectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("sneeze_detection", isChecked).apply()
        }


        // Dark theme switch listener is now set in loadSettings() to prevent infinite loop

        // Language button listener - shows selection dialog
        languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
        
        backButton.setOnClickListener {
            finish()
        }
        
        resetButton.setOnClickListener {
            resetToDefaults()
        }
        
        saveButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
        }
    }

    private fun setupMainListeners() {
        audioModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            android.util.Log.d("SettingsActivity", "Radio group changed: checkedId=$checkedId")
            
            when (checkedId) {
                R.id.radioAudioOff -> {
                    android.util.Log.d("SettingsActivity", "Audio mode: OFF")
                    sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
                }
                R.id.radioVoiceCommands -> {
                    android.util.Log.d("SettingsActivity", "Audio mode: VOICE COMMANDS")
                    requestVoiceRecognitionPermission()
                }
                R.id.radioSoundDetection -> {
                    android.util.Log.d("SettingsActivity", "Audio mode: SOUND DETECTION")
                    requestSoundDetectionPermission()
                }
            }
        }
    }

    private fun requestVoiceRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user
                AlertDialog.Builder(this)
                    .setTitle("Microphone Permission Required")
                    .setMessage("Voice recognition requires microphone access to listen for spoken commands.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        radioAudioOff.isChecked = true
                        sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
                    }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO)
            }
        } else {
            // Permission already granted
            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_VOICE_COMMANDS).apply()
            Toast.makeText(this, "Voice Commands enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestSoundDetectionPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user
                AlertDialog.Builder(this)
                    .setTitle("Microphone Permission Required")
                    .setMessage("Sound detection requires microphone access to listen for sneezes and other sounds.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        radioAudioOff.isChecked = true
                        sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
                    }
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO)
            }
        } else {
            // Permission already granted
            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_SOUND_DETECTION).apply()
            Toast.makeText(this, "Sound Detection enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Voice recognition cannot work without microphone access. Would you like to open settings to grant the permission?")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                radioAudioOff.isChecked = true
                sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    saveNotificationSetting(true)
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionRationaleDialog()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_NOTIFICATIONS
                    )
                }
            }
        } else {
            saveNotificationSetting(true)
        }
    }

    private fun showNotificationPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_required))
            .setMessage(getString(R.string.notification_permission_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_NOTIFICATIONS
                    )
                }
            }
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                notificationsSwitch.isChecked = false
                saveNotificationSetting(false)
            }
            .create()
            .show()
    }

    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_permission_denied_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                notificationsSwitch.isChecked = false
                saveNotificationSetting(false)
            }
            .create()
            .show()
    }

    private fun saveNotificationSetting(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications", enabled).apply()
        setResult(RESULT_OK)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted - determine which mode was being requested
                    when {
                        radioVoiceCommands.isChecked -> {
                            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_VOICE_COMMANDS).apply()
                            Toast.makeText(this, "Voice Commands enabled", Toast.LENGTH_SHORT).show()
                        }
                        radioSoundDetection.isChecked -> {
                            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_SOUND_DETECTION).apply()
                            Toast.makeText(this, "Sound Detection enabled", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Shouldn't happen, but fail safely
                            radioAudioOff.isChecked = true
                            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
                        }
                    }
                } else {
                    // Permission denied
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        Toast.makeText(this, "Microphone permission is required for audio features", Toast.LENGTH_LONG).show()
                    } else {
                        showPermissionDeniedDialog()
                    }
                    // Reset to off mode
                    radioAudioOff.isChecked = true
                    sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
                }
            }
            PERMISSION_REQUEST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveNotificationSetting(true)
                    Toast.makeText(this, "Push notifications enabled", Toast.LENGTH_SHORT).show()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionDeniedDialog()
                    } else {
                        notificationsSwitch.isChecked = false
                        saveNotificationSetting(false)
                        Toast.makeText(this, "Push notifications disabled: Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        debugCurrentSettings("onResume() called")
        // REMOVED: updateVoiceRecognitionSwitchState() - this was interfering with our mutual exclusivity setup
        // Both voice commands and sound detection need the same microphone permission, 
        // so we don't need separate permission-based switch logic
        // REMOVED: updateNotificationSwitchState() - notifications are handled in loadSettings()
    }

    private fun resetToDefaults() {
        radioAudioOff.isChecked = true
        notificationsSwitch.isChecked = true
        accelerometerSwitch.isChecked = true
        blowDetectionSwitch.isChecked = true
        sneezeDetectionSwitch.isChecked = true
        
        // Reset theme and language
        ThemeManager.setTheme(this, ThemeManager.THEME_LIGHT)
        LocaleHelper.setLocale(this, "en")
        
        // Recreate activity to immediately apply the new theme and language
        recreate()
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        
        // Audio mode is automatically saved when radio buttons change
        editor.putBoolean("notifications", notificationsSwitch.isChecked)
        editor.putBoolean("accelerometer", accelerometerSwitch.isChecked)
        editor.putBoolean("blow_detection", blowDetectionSwitch.isChecked)
        editor.putBoolean("sneeze_detection", sneezeDetectionSwitch.isChecked)
        // Language is now managed by LocaleHelper, no need to save boolean
        
        editor.apply()
        
        android.util.Log.d("SettingsActivity", "Settings saved successfully")
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
    }
    
    private fun debugCurrentSettings(stage: String) {
        val audioMode = getAudioMode(sharedPreferences)
        val audioModeText = when(audioMode) {
            AUDIO_MODE_OFF -> "OFF"
            AUDIO_MODE_VOICE_COMMANDS -> "VOICE_COMMANDS"
            AUDIO_MODE_SOUND_DETECTION -> "SOUND_DETECTION"
            else -> "UNKNOWN($audioMode)"
        }
        
        android.util.Log.w("SettingsDebug", "=== $stage ===")
        android.util.Log.w("SettingsDebug", "SharedPrefs: audioMode=$audioModeText")
        if (::radioAudioOff.isInitialized) {
            android.util.Log.w("SettingsDebug", "UI RadioButtons: off=${radioAudioOff.isChecked}, voice=${radioVoiceCommands.isChecked}, sound=${radioSoundDetection.isChecked}")
        } else {
            android.util.Log.w("SettingsDebug", "UI RadioButtons: NOT_INITIALIZED")
        }
        android.util.Log.w("SettingsDebug", "===================")
    }
    
    @Suppress("ResourceType")
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
    
    private fun showLanguageSelectionDialog() {
        val languages = LocaleHelper.getSupportedLanguages()
        val languageNames = languages.map { it.nativeDisplayName }.toTypedArray()
        val currentLanguage = LocaleHelper.getCurrentLanguage(this)
        val currentIndex = languages.indexOfFirst { it.code == currentLanguage }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_language))
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                LocaleHelper.setLocale(this, selectedLanguage.code)
                updateLanguageLabel()
                dialog.dismiss()
                recreate() // Recreate activity to apply new language
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun enforceSettingsConsistency() {
        // With RadioGroup, mutual exclusivity is guaranteed by the UI component
        // Just ensure we have a valid audio mode setting
        val audioMode = getAudioMode(sharedPreferences)
        android.util.Log.d("SettingsActivity", "Checking settings consistency: audioMode=$audioMode")
        
        if (audioMode !in arrayOf(AUDIO_MODE_OFF, AUDIO_MODE_VOICE_COMMANDS, AUDIO_MODE_SOUND_DETECTION)) {
            android.util.Log.w("SettingsActivity", "Invalid audio mode detected, resetting to OFF")
            sharedPreferences.edit().putInt("audio_mode", AUDIO_MODE_OFF).apply()
        }
        
        val isFirstRun = sharedPreferences.getBoolean("first_run", true)
        if (isFirstRun) {
            android.util.Log.i("SettingsActivity", "First run detected - ensuring audio mode is OFF")
            sharedPreferences.edit()
                .putInt("audio_mode", AUDIO_MODE_OFF)
                .putBoolean("first_run", false)
                .apply()
        }
    }

} 