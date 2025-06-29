package com.b1gbr0ther.data.database.entities

import com.b1gbr0ther.TaskCategory
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TaskTest {

    @Test
    fun testTaskCreationWithCategory() {
        val now = LocalDateTime.now()
        val taskName = "Test Task"
        val category = TaskCategory.WORK
        
        val task = Task(
            taskName = taskName,
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
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
            creationMethod = "Test",
            // If no category is provided, it should default to OTHER
            category = null // This should be replaced with default in constructor if null handling is implemented
        )
        
        // Should not be null even if initialized with null
        assertNotNull(task.category)
        
        // The default should be OTHER
        assertEquals(TaskCategory.OTHER, task.category)
    }
}
