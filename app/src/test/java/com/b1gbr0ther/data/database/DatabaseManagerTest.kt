package com.b1gbr0ther.data.database

import android.content.Context
import androidx.room.Room
import com.b1gbr0ther.CreationMethod
import com.b1gbr0ther.TaskCategory
import com.b1gbr0ther.data.database.entities.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import android.os.Looper
import org.robolectric.annotation.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.resume
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for DatabaseManager functionality
 * Tests core data operations including:
 * - Task creation and retrieval
 * - Date-based task filtering
 * - Statistics calculations
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class DatabaseManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var databaseManager: DatabaseManager
    
    // Test coroutine dispatcher
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setupDatabase() {
        // Set the main dispatcher to our test dispatcher
        Dispatchers.setMain(testDispatcher)
        
        val context = RuntimeEnvironment.getApplication().applicationContext
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Set the test database as the singleton instance for testing
        AppDatabase.setTestInstance(database)
        
        // Create DatabaseManager with the test context
        databaseManager = DatabaseManager(context)
        
        // Process any pending main thread operations
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
    
    @After
    fun closeDatabase() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
        
        // Close the database
        database.close()
    }
    
    // Helper function to convert callback-based API to suspending function
    private suspend fun createTaskSuspending(task: Task): Long {
        return suspendCancellableCoroutine { continuation ->
            databaseManager.createTask(task) { taskId ->
                continuation.resume(taskId)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
    
    // Helper function to convert callback-based API to suspending function
    private suspend fun getTasksForDateSuspending(date: LocalDate): List<Task> {
        return suspendCancellableCoroutine { continuation ->
            databaseManager.getTasksForDate(date) { tasks ->
                continuation.resume(tasks)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
    
    @Test
    fun testCreateAndRetrieveTask() = runTest {
        // Create a test task
        val now = LocalDateTime.now()
        val testTask = Task(
            taskName = "Test Task",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        // Insert task using suspending function
        val createdTaskId = createTaskSuspending(testTask)
        
        // Verify task was created successfully
        assertTrue("Task should have a valid ID", createdTaskId > 0)
        
        // Retrieve tasks for today and find the one we just created
        val tasks = getTasksForDateSuspending(LocalDate.now())
        val retrievedTask = tasks.find { it.id == createdTaskId }
        
        // Verify task was retrieved correctly
        assertNotNull("Retrieved task should not be null", retrievedTask)
        assertEquals("Task name should match", testTask.taskName, retrievedTask?.taskName)
        assertEquals("Task category should match", testTask.category, retrievedTask?.category)
    }
    
    @Test
    fun testGetTasksForDate() = runTest {
        // Create test tasks for today and tomorrow
        val today = LocalDate.now()
        val todayDateTime = today.atStartOfDay()
        val tomorrowDateTime = today.plusDays(1).atStartOfDay()
        
        val todayTask1 = Task(
            taskName = "Today Task 1",
            startTime = todayDateTime.plusHours(2),
            endTime = todayDateTime.plusHours(3),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val todayTask2 = Task(
            taskName = "Today Task 2",
            startTime = todayDateTime.plusHours(4),
            endTime = todayDateTime.plusHours(5),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.LEISURE
        )
        
        val tomorrowTask = Task(
            taskName = "Tomorrow Task",
            startTime = tomorrowDateTime.plusHours(2),
            endTime = tomorrowDateTime.plusHours(3),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PERSONAL
        )
        
        // Insert tasks using suspending function
        createTaskSuspending(todayTask1)
        createTaskSuspending(todayTask2)
        createTaskSuspending(tomorrowTask)
        
        // Retrieve tasks for today
        val todayTasks = getTasksForDateSuspending(today)
        
        // Verify results
        assertEquals("Should have 2 tasks for today", 2, todayTasks.size)
        assertTrue("Should contain Today Task 1", todayTasks.any { it.taskName == "Today Task 1" })
        assertTrue("Should contain Today Task 2", todayTasks.any { it.taskName == "Today Task 2" })
    }
    
    // Helper function to convert callback-based API to suspending function
    private suspend fun getDailyWorkHoursForMonthSuspending(yearMonth: YearMonth): Map<LocalDate, Float> {
        return suspendCancellableCoroutine { continuation ->
            databaseManager.getDailyWorkHoursForMonth(yearMonth) { hours ->
                continuation.resume(hours)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
    
    @Test
    fun testGetDailyWorkHoursForMonth() = runTest {
        // Create test tasks for different days in the month
        val thisMonth = YearMonth.now()
        val firstDay = thisMonth.atDay(1)
        val secondDay = thisMonth.atDay(2)
        
        val firstDayDateTime = firstDay.atStartOfDay()
        val secondDayDateTime = secondDay.atStartOfDay()
        
        val task1 = Task(
            taskName = "Day 1 Task 1",
            startTime = firstDayDateTime.plusHours(9),
            endTime = firstDayDateTime.plusHours(11),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val task2 = Task(
            taskName = "Day 2 Task 1",
            startTime = secondDayDateTime.plusHours(10),
            endTime = secondDayDateTime.plusHours(12),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val task3 = Task(
            taskName = "Day 2 Task 2",
            startTime = secondDayDateTime.plusHours(14),
            endTime = secondDayDateTime.plusHours(15),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        // Insert tasks using suspending function
        val taskId1 = createTaskSuspending(task1)
        val taskId2 = createTaskSuspending(task2)
        val taskId3 = createTaskSuspending(task3)
        
        // Verify tasks were created successfully
        assertTrue("Task 1 should have a valid ID", taskId1 > 0)
        assertTrue("Task 2 should have a valid ID", taskId2 > 0)
        assertTrue("Task 3 should have a valid ID", taskId3 > 0)
        
        // Get daily work hours
        val dailyHours = getDailyWorkHoursForMonthSuspending(thisMonth)
        
        // Verify results
        assertEquals("Should have entries for 2 days", 2, dailyHours.size)
        assertEquals("Day 1 should have 2 hours", 2f, dailyHours[firstDay] ?: 0f)
        assertEquals("Day 2 should have 3 hours", 3f, dailyHours[secondDay] ?: 0f)
    }
    
    // Helper function to convert callback-based API to suspending function
    private suspend fun getCompletedTasksCountSuspending(): Int {
        return suspendCancellableCoroutine { continuation ->
            databaseManager.getCompletedTasksCount { count ->
                continuation.resume(count)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
    
    @Test
    fun testGetCompletedTasksCount() = runTest {
        // Create test tasks with different completion statuses
        val now = LocalDateTime.now()
        
        val completedTask1 = Task(
            taskName = "Completed Task 1",
            startTime = now.minusHours(3),
            endTime = now.minusHours(2),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val completedTask2 = Task(
            taskName = "Completed Task 2",
            startTime = now.minusHours(5),
            endTime = now.minusHours(4),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.LEISURE
        )
        
        val incompleteTask = Task(
            taskName = "Incomplete Task",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PERSONAL
        )
        
        // Insert tasks using suspending function
        createTaskSuspending(completedTask1)
        createTaskSuspending(completedTask2)
        createTaskSuspending(incompleteTask)
        
        // Get completed tasks count
        val completedCount = getCompletedTasksCountSuspending()
        
        // Verify results
        assertEquals("Should have 2 completed tasks", 2, completedCount)
    }
    
    // Helper function to convert callback-based API to suspending function
    private suspend fun getTaskCountByCategorySuspending(category: TaskCategory): Int {
        return suspendCancellableCoroutine { continuation ->
            databaseManager.getTaskCountByCategory(category) { count ->
                continuation.resume(count)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }
    
    @Test
    fun testGetTaskCountByCategory() = runTest {
        // Create test tasks with different categories
        val now = LocalDateTime.now()
        
        val professionalTask1 = Task(
            taskName = "Professional Task 1",
            startTime = now.minusHours(3),
            endTime = now.minusHours(2),
            isCompleted = true,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val professionalTask2 = Task(
            taskName = "Professional Task 2",
            startTime = now.minusHours(5),
            endTime = now.minusHours(4),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.PROFESSIONAL
        )
        
        val leisureTask = Task(
            taskName = "Leisure Task",
            startTime = now,
            endTime = now.plusHours(1),
            isCompleted = false,
            isBreak = false,
            isPreplanned = false,
            creationMethod = CreationMethod.Gesture,
            category = TaskCategory.LEISURE
        )
        
        // Insert tasks using suspending function
        createTaskSuspending(professionalTask1)
        createTaskSuspending(professionalTask2)
        createTaskSuspending(leisureTask)
        
        // Get task count by category
        val professionalCount = getTaskCountByCategorySuspending(TaskCategory.PROFESSIONAL)
        val leisureCount = getTaskCountByCategorySuspending(TaskCategory.LEISURE)
        
        // Verify results
        assertEquals("Should have 2 professional tasks", 2, professionalCount)
        assertEquals("Should have 1 leisure task", 1, leisureCount)
    }
}
