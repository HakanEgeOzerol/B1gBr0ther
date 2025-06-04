package com.b1gbr0ther.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.b1gbr0ther.data.database.converters.LocalDateTimeConverter
import com.b1gbr0ther.data.database.dao.TaskDao
import com.b1gbr0ther.data.database.entities.Task

/**
 * Main database class for the B1gBr0ther app.
 * This is the primary access point for the Room database.
 */
@Database(entities = [Task::class], version = 2, exportSchema = false)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Get the TaskDao for accessing task data.
     */
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
