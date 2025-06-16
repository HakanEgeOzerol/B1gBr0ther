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
    private lateinit var languageButton: Button
    private lateinit var languageLabel: TextView
    private lateinit var backButton: Button
    private lateinit var resetButton: Button
    private lateinit var saveButton: Button

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
        private const val PERMISSION_REQUEST_NOTIFICATIONS = 1002
        
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
        languageButton = findViewById(R.id.languageButton)
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

        // Language button listener - shows selection dialog
        languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        voiceRecognitionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestVoiceRecognitionPermission()
            } else {
                saveVoiceRecognitionSetting(false)
            }
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermission()
            } else {
                saveNotificationSetting(false)
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
            .setTitle(getString(R.string.microphone_permission_required))
            .setMessage(getString(R.string.microphone_permission_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_RECORD_AUDIO
                )
            }
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                voiceRecognitionSwitch.isChecked = false
                saveVoiceRecognitionSetting(false)
            }
            .create()
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.microphone_permission_denied_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
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

    private fun saveVoiceRecognitionSetting(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("voice_recognition", enabled).apply()
        setResult(RESULT_OK)
    }

    private fun saveNotificationSetting(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications", enabled).apply()
        setResult(RESULT_OK)
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

    private fun updateNotificationSwitchState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val isEnabled = sharedPreferences.getBoolean("notifications", true)
            
            notificationsSwitch.setOnCheckedChangeListener(null)
            notificationsSwitch.isChecked = isEnabled && hasPermission
            
            notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    requestNotificationPermission()
                } else {
                    saveNotificationSetting(false)
                }
            }
        } else {
            notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications", true)
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
                    Toast.makeText(this, getString(R.string.voice_recognition_enabled), Toast.LENGTH_SHORT).show()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        showPermissionDeniedDialog()
                    } else {
                        voiceRecognitionSwitch.isChecked = false
                        saveVoiceRecognitionSetting(false)
                        Toast.makeText(this, getString(R.string.voice_recognition_disabled_permission), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            PERMISSION_REQUEST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveNotificationSetting(true)
                    Toast.makeText(this, getString(R.string.push_notifications_enabled), Toast.LENGTH_SHORT).show()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionDeniedDialog()
                    } else {
                        notificationsSwitch.isChecked = false
                        saveNotificationSetting(false)
                        Toast.makeText(this, getString(R.string.push_notifications_disabled_permission), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateVoiceRecognitionSwitchState()
        updateNotificationSwitchState()
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
        // Reset language to English
        LocaleHelper.setLocale(this, "en")
        updateLanguageLabel()
        
        Toast.makeText(this, getString(R.string.settings_reset_to_defaults), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, getString(R.string.settings_saved_successfully), Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
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

} 