package com.b1gbr0ther.data.database.converters

import com.b1gbr0ther.TaskCategory
import org.junit.Assert.*
import org.junit.Test

class TaskCategoryConverterTest {

    private val converter = TaskCategoryConverter()

    @Test
    fun testFromTaskCategory() {
        // Test conversion from TaskCategory enum to String
        assertEquals("WORK", converter.fromTaskCategory(TaskCategory.WORK))
        assertEquals("STUDY", converter.fromTaskCategory(TaskCategory.STUDY))
        assertEquals("PERSONAL", converter.fromTaskCategory(TaskCategory.PERSONAL))
        assertEquals("HEALTH", converter.fromTaskCategory(TaskCategory.HEALTH))
        assertEquals("FAMILY", converter.fromTaskCategory(TaskCategory.FAMILY))
        assertEquals("HOBBY", converter.fromTaskCategory(TaskCategory.HOBBY))
        assertEquals("OTHER", converter.fromTaskCategory(TaskCategory.OTHER))
    }

    @Test
    fun testToTaskCategory() {
        // Test conversion from String to TaskCategory enum
        assertEquals(TaskCategory.WORK, converter.toTaskCategory("WORK"))
        assertEquals(TaskCategory.STUDY, converter.toTaskCategory("STUDY"))
        assertEquals(TaskCategory.PERSONAL, converter.toTaskCategory("PERSONAL"))
        assertEquals(TaskCategory.HEALTH, converter.toTaskCategory("HEALTH"))
        assertEquals(TaskCategory.FAMILY, converter.toTaskCategory("FAMILY"))
        assertEquals(TaskCategory.HOBBY, converter.toTaskCategory("HOBBY"))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory("OTHER"))
    }

    @Test
    fun testToTaskCategoryWithInvalidValue() {
        // Test that invalid values are handled gracefully
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory("INVALID_CATEGORY"))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory(""))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory(null))
    }
}
