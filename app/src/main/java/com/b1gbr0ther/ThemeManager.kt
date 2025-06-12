package com.b1gbr0ther

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Utility class to manage app themes
 */
object ThemeManager {
    private const val PREFS_NAME = "B1gBr0therSettings"
    private const val THEME_KEY = "theme_mode"
    
    // Theme modes
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2
    
    fun applyTheme(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_KEY, THEME_SYSTEM)
        
        val nightMode = when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    
    fun setTheme(context: Context, themeMode: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(THEME_KEY, themeMode).apply()
        applyTheme(context)
    }
    
    fun getCurrentTheme(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(THEME_KEY, THEME_SYSTEM)
    }
    
    fun getThemeName(themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            THEME_SYSTEM -> "Follow System"
            else -> "Follow System"
        }
    }
} 