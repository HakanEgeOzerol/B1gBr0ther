package com.b1gbr0ther

import android.app.Application
import com.b1gbr0ther.data.database.AppDatabase
import com.b1gbr0ther.data.database.DatabaseManager

/**
 * Application class for the B1gBr0ther app.
 * Initializes app-wide components like the database and theme.
 */
class B1gBr0therApplication : Application() {
    // Database components
    lateinit var database: AppDatabase
    lateinit var databaseManager: DatabaseManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the database
        database = AppDatabase.getDatabase(this)
        databaseManager = DatabaseManager(this)
        
        // Apply saved theme globally
        ThemeManager.applyTheme(this)
    }
}
