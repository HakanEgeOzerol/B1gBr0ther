package com.b1gbr0ther.data.database.entities

import com.b1gbr0ther.CreationMethod
import com.b1gbr0ther.TaskCategory
import com.b1gbr0ther.CreationMethod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TaskTest {

    @Test
    fun testTaskCreationWithCategory() {
        val now = LocalDateTime.now()
        val taskName = "Test Task"
        val category = TaskCategory.PROFESSIONAL
        
        val task = Task(
            taskName = taskName,
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = category
        )
        
        assertEquals(taskName, task.taskName)
        assertEquals(category, task.category)
    }
    
    @Test
    fun testTaskDefaultValues() {
        val now = LocalDateTime.now()
        val task = Task(
            taskName = "Test Task",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.OTHER
        )
        
        // Category should never be null
        assertNotNull(task.category)
        
        // The default should be PROFESSIONAL (as defined in TaskCategory.getDefault())
        assertEquals(TaskCategory.PROFESSIONAL, task.category)
    }
}
