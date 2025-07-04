package com.b1gbr0ther.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.b1gbr0ther.data.database.converters.LocalDateTimeConverter
import com.b1gbr0ther.data.database.converters.TaskCategoryConverter
import com.b1gbr0ther.data.database.dao.TaskDao
import com.b1gbr0ther.data.database.entities.Task

/**
 * Main database class for the B1gBr0ther app.
 * This is the primary access point for the Room database.
 */
@Database(entities = [Task::class], version = 5, exportSchema = false)
@TypeConverters(LocalDateTimeConverter::class, TaskCategoryConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Get the TaskDao for accessing task data.
     */
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Set a test instance of the database for unit testing
         * @param testDatabase The test database instance
         */
        @JvmStatic
        fun setTestInstance(testDatabase: AppDatabase) {
            INSTANCE = testDatabase
        }
        
        /**
         * Migration from version 4 to version 5: Add category field to tasks table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new category column with a default value of 'WORK'
                database.execSQL("ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'WORK'")
            }
        }

        /**
         * Get the database instance, creating it if it doesn't exist.
         * @param context The application context
         * @return The database instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "b1gbr0ther_database"
                )
                    // Add migration from version 4 to 5 (adding category field)
                    .addMigrations(MIGRATION_4_5)
                    // Add fallback migration strategy as a safety net
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
