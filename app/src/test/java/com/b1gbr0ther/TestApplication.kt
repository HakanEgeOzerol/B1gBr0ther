package com.b1gbr0ther

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Test application class for Robolectric tests
 * Provides proper theme configuration to avoid "You need to use a Theme.AppCompat theme" errors
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Set default night mode to avoid theme-related issues in tests
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        // Apply the AppCompat theme
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Ensure resources are properly initialized
        resources
    }
    
    /**
     * Helper method to explicitly set theme for tests
     */
    fun setTestTheme(themeResId: Int) {
        setTheme(themeResId)
    }
}
