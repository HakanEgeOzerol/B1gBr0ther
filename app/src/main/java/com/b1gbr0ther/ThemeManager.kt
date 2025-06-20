package com.b1gbr0ther

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage app themes (NOT system themes)
 */
object ThemeManager {
    private const val PREFS_NAME = "B1gBr0therSettings"
    private const val THEME_KEY = "app_theme_mode"
    
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    
    fun applyTheme(activity: Activity) {
        val sharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt(THEME_KEY, THEME_LIGHT)
        
        val themeRes = when (themeMode) {
            THEME_DARK -> R.style.Theme_B1gBr0ther_Dark
            THEME_LIGHT -> R.style.Theme_B1gBr0ther_Light
            else -> R.style.Theme_B1gBr0ther_Light
        }
        
        activity.setTheme(themeRes)
    }
    
    fun setTheme(context: Context, themeMode: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(THEME_KEY, themeMode).apply()
    }
    
    fun getCurrentTheme(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(THEME_KEY, THEME_LIGHT)
    }
    
    fun getThemeName(themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            else -> "Light"
        }
    }
    
    fun getThemeResource(themeMode: Int): Int {
        return when (themeMode) {
            THEME_DARK -> R.style.Theme_B1gBr0ther_Dark
            THEME_LIGHT -> R.style.Theme_B1gBr0ther_Light
            else -> R.style.Theme_B1gBr0ther_Light
        }
    }
} 