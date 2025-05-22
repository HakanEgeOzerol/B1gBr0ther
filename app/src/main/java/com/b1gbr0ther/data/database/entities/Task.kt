package com.b1gbr0ther.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.b1gbr0ther.data.database.converters.LocalDateTimeConverter
import java.time.LocalDateTime

/**
 * Task entity for Room database that matches the app's Task class.
 * This entity represents a task with start and end times, and various status flags.
 */
@Entity(tableName = "tasks")
@TypeConverters(LocalDateTimeConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var taskName: String = "",
    var startTime: LocalDateTime = LocalDateTime.now(),
    var endTime: LocalDateTime = LocalDateTime.now(),
    var isPreplanned: Boolean = false,
    var isCompleted: Boolean = false,
    var isBreak: Boolean = false
) {
    // Empty constructor required by Room
    constructor() : this(
        0,
        "",
        LocalDateTime.now(),
        LocalDateTime.now(),
        false,
        false,
        false
    )
    
    /**
     * Convert this Room entity to the app's Task model
     */
    fun toAppTask(): com.b1gbr0ther.Task {
        return com.b1gbr0ther.Task(
            taskName,
            startTime,
            endTime,
            isPreplanned,
            isCompleted,
            isBreak
        )
    }
    
    companion object {
        /**
         * Create a Room Task entity from the app's Task model
         */
        fun fromAppTask(appTask: com.b1gbr0ther.Task): Task {
            return Task(
                taskName = appTask.getName(),
                startTime = appTask.getStartTime(),
                endTime = appTask.getEndTime(),
                isPreplanned = appTask.getIsPreplanned() ?: false,
                isCompleted = appTask.getIsCompleted() ?: false,
                isBreak = appTask.getIsBreak() ?: false
            )
        }
    }
}
