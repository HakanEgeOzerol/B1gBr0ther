package com.b1gbr0ther.data.database.converters

import com.b1gbr0ther.model.TaskCategory
import org.junit.Assert.*
import org.junit.Test

class TaskCategoryConverterTest {

    private val converter = TaskCategoryConverter()

    @Test
    fun testFromTaskCategory() {
        // Test conversion from TaskCategory enum to String
        assertEquals("WORK_STUDY", converter.fromTaskCategory(TaskCategory.WORK_STUDY))
        assertEquals("PERSONAL", converter.fromTaskCategory(TaskCategory.PERSONAL))
        assertEquals("FAMILY", converter.fromTaskCategory(TaskCategory.FAMILY))
        assertEquals("LEISURE", converter.fromTaskCategory(TaskCategory.LEISURE))
        assertEquals("OTHER", converter.fromTaskCategory(TaskCategory.OTHER))
    }

    @Test
    fun testToTaskCategory() {
        // Test conversion from String to TaskCategory enum
        assertEquals(TaskCategory.WORK_STUDY, converter.toTaskCategory("WORK_STUDY"))
        assertEquals(TaskCategory.PERSONAL, converter.toTaskCategory("PERSONAL"))
        assertEquals(TaskCategory.FAMILY, converter.toTaskCategory("FAMILY"))
        assertEquals(TaskCategory.LEISURE, converter.toTaskCategory("LEISURE"))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory("OTHER"))
    }

    @Test
    fun testToTaskCategoryWithInvalidValue() {
        // Test handling of invalid category names
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory("INVALID_CATEGORY"))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory(""))
        assertEquals(TaskCategory.OTHER, converter.toTaskCategory(null))
    }
    
    @Test
    fun testBackwardCompatibility() {
        // Test backward compatibility with old category names
        assertEquals(TaskCategory.WORK_STUDY, converter.toTaskCategory("WORK"))
        assertEquals(TaskCategory.WORK_STUDY, converter.toTaskCategory("STUDY"))
        assertEquals(TaskCategory.PERSONAL, converter.toTaskCategory("HEALTH"))
        assertEquals(TaskCategory.LEISURE, converter.toTaskCategory("HOBBY"))
    }
}
