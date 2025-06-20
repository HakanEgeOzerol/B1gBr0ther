package com.b1gbr0ther

import android.app.Application
import android.content.Context
import com.b1gbr0ther.data.database.AppDatabase
import com.b1gbr0ther.data.database.DatabaseManager

/**
 * Application class for the B1gBr0ther app.
 * Initializes app-wide components like the database and theme.
 */
class B1gBr0therApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var databaseManager: DatabaseManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getDatabase(this)
        databaseManager = DatabaseManager(this)
        
        //Theme is now applied per-activity, not globally
    }
}
