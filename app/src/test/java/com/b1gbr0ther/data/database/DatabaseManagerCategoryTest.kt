package com.b1gbr0ther.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.TaskCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DatabaseManagerCategoryTest {
    
    private lateinit var database: AppDatabase
    private lateinit var databaseManager: DatabaseManager
    private val testContext = mock(Context::class.java)
    
    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Use mock context for DatabaseManager
        databaseManager = DatabaseManager(context)
    }
    
    @After
    fun closeDatabase() {
        database.close()
    }
    
    @Test
    fun testGetTaskCountByCategory() = runBlocking {
        // Create test tasks with different categories
        val now = LocalDateTime.now()
        val task1 = Task(
            taskName = "Work Task 1",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
            category = TaskCategory.WORK
        )
        
        val task2 = Task(
            taskName = "Work Task 2",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
            category = TaskCategory.WORK
        )
        
        val task3 = Task(
            taskName = "Study Task",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
            category = TaskCategory.LEISURE
        )
        
        // Insert tasks
        val latch1 = CountDownLatch(3)
        databaseManager.createTask(task1) { latch1.countDown() }
        databaseManager.createTask(task2) { latch1.countDown() }
        databaseManager.createTask(task3) { latch1.countDown() }
        latch1.await(5, TimeUnit.SECONDS)
        
        // Test count by category
        val latch2 = CountDownLatch(2)
        var workCount = 0
        var studyCount = 0
        
        databaseManager.getTaskCountByCategory(TaskCategory.PROFESSIONAL) { count ->
            workCount = count
            latch2.countDown()
        }
        
        databaseManager.getTaskCountByCategory(TaskCategory.LEISURE) { count ->
            studyCount = count
            latch2.countDown()
        }
        
        latch2.await(5, TimeUnit.SECONDS)
        
        assertEquals(2, workCount, "Should have 2 work/study tasks")
        assertEquals(1, studyCount, "Should have 1 leisure task")
    }
    
    @Test
    fun testGetCompletedTaskCountByCategory() = runBlocking {
        // Create test tasks with different categories and completion status
        val now = LocalDateTime.now()
        val task1 = Task(
            taskName = "Work Task 1",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
            category = TaskCategory.WORK
        )
        
        val task2 = Task(
            taskName = "Work Task 2",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = "Test",
            category = TaskCategory.WORK
        )
        
        // Insert tasks
        val latch1 = CountDownLatch(2)
        databaseManager.createTask(task1) { latch1.countDown() }
        databaseManager.createTask(task2) { latch1.countDown() }
        latch1.await(5, TimeUnit.SECONDS)
        
        // Test completed count by category
        val latch2 = CountDownLatch(1)
        var completedWorkCount = 0
        
        databaseManager.getCompletedTaskCountByCategory(TaskCategory.PROFESSIONAL) { count ->
            completedWorkCount = count
            latch2.countDown()
        }
        
        latch2.await(5, TimeUnit.SECONDS)
        
        assertEquals(1, completedWorkCount, "Should have 1 completed work/study task")
    }
}
