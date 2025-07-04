package com.b1gbr0ther.data.database.converters

import androidx.room.TypeConverter
import com.b1gbr0ther.model.TaskCategory

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
     * Includes backward compatibility for old category names
     */
    @TypeConverter
    fun toTaskCategory(categoryName: String?): TaskCategory {
        if (categoryName == null) return TaskCategory.OTHER
        
        // Handle legacy category names for backward compatibility
        return when (categoryName) {
            "WORK", "STUDY", "WORK_STUDY" -> TaskCategory.PROFESSIONAL
            "HEALTH" -> TaskCategory.PERSONAL
            "HOBBY" -> TaskCategory.LEISURE
            else -> try {
                TaskCategory.valueOf(categoryName)
            } catch (e: IllegalArgumentException) {
                // Fallback to default category if the stored name doesn't match any enum value
                TaskCategory.OTHER
            }
        }
    }
}
