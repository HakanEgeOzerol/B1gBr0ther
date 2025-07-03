package com.b1gbr0ther.models

import com.b1gbr0ther.TaskCategory
import org.junit.Assert.*
import org.junit.Test

class TaskCategoryTest {
    @Test
    fun testEnumValues() {
        // Test that all expected categories are defined
        val categories = TaskCategory.values()
        assertEquals(5, categories.size)
        
        assertTrue(categories.any { it.name == "PROFESSIONAL" })
        assertTrue(categories.any { it.name == "PERSONAL" })
        assertTrue(categories.any { it.name == "FAMILY" })
        assertTrue(categories.any { it.name == "LEISURE" })
        assertTrue(categories.any { it.name == "OTHER" })
    }
    
    @Test
    fun testDefaultCategory() {
        // Test that OTHER is available as a default category
        assertNotNull(TaskCategory.OTHER)
    }
}
