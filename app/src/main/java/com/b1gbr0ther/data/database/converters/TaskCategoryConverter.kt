package com.b1gbr0ther.data.database.converters

import androidx.room.TypeConverter
import com.b1gbr0ther.TaskCategory

/**
 * Type converter for Room database to convert TaskCategory enum to/from String
 */
class TaskCategoryConverter {
    /**
     * Convert TaskCategory enum to String for storage in database
     */
    @TypeConverter
    fun fromTaskCategory(category: TaskCategory): String {
        return category.name
    }

    /**
     * Convert String from database back to TaskCategory enum
     */
    @TypeConverter
    fun toTaskCategory(categoryName: String): TaskCategory {
        return try {
            TaskCategory.valueOf(categoryName)
        } catch (e: IllegalArgumentException) {
            // Fallback to default category if the stored name doesn't match any enum value
            // (This could happen if enum values change)
            TaskCategory.getDefault()
        }
    }
}
