package com.b1gbr0ther.data.database

import android.content.Context
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.data.database.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.Duration
import com.b1gbr0ther.WorkBlock
import com.b1gbr0ther.TaskCategory

/**
 * Database manager class that handles CRUD operations for Tasks in the B1gBr0ther app.
 * This class provides methods to create, read, update, and delete tasks from the database.
 */
class DatabaseManager(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val taskRepository = TaskRepository(database.taskDao())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Retrieve all tasks for the specified week and convert them into a list of WorkBlock objects
     * that the dashboard calendar can render.
     *
     * @param weekStart The Monday of the week to load (use LocalDate.now().with(DayOfWeek.MONDAY)).
     * @param callback  Callback delivering the resulting list on the main thread.
     */
    fun getWorkBlocksForWeek(weekStart: LocalDate, callback: (List<WorkBlock>) -> Unit) {
        val weekStartDateTime = weekStart.atStartOfDay()
        val weekEndDateTime = weekStart.plusDays(7).atStartOfDay()
        coroutineScope.launch {
            val tasks = taskRepository.getTasksByTimeRange(weekStartDateTime, weekEndDateTime)
            val blocks = tasks.map { task ->
                val start = task.startTime
                val end = task.endTime
                val dayIndex = start.dayOfWeek.value - 1 // DayOfWeek.MONDAY = 1 -> 0

                val startHour = start.hour + start.minute / 60f + start.second / 3600f
                val endHour = end.hour + end.minute / 60f + end.second / 3600f

                WorkBlock(dayIndex, startHour, endHour, task.isBreak)
            }
            withContext(Dispatchers.Main) {
                callback(blocks)
            }
        }
    }

    /**
     * Return all tasks whose startTime falls on the given calendar date.
     */
    fun getTasksForDate(date: LocalDate, callback: (List<Task>) -> Unit) {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        coroutineScope.launch {
            val tasks = taskRepository.getTasksByTimeRange(start, end)
            withContext(Dispatchers.Main) { callback(tasks) }
        }
    }

    /**
     * For a whole month, compute worked hours (excluding breaks) for every day.
     * Returns map { LocalDate -> hoursWorkedFloat }
     */
    fun getDailyWorkHoursForMonth(yearMonth: YearMonth, callback: (Map<LocalDate, Float>) -> Unit) {
        val monthStart = yearMonth.atDay(1).atStartOfDay()
        val monthEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay()
        coroutineScope.launch {
            val tasks = taskRepository.getTasksByTimeRange(monthStart, monthEnd)
            val dailyMap = mutableMapOf<LocalDate, Float>()
            for (task in tasks) {
                if (task.isBreak) continue // ignore breaks in hours summary
                val date = task.startTime.toLocalDate()
                val durationMin = Duration.between(task.startTime, task.endTime).toMinutes()
                val hours = durationMin / 60f
                dailyMap[date] = (dailyMap[date] ?: 0f) + hours
            }
            withContext(Dispatchers.Main) { callback(dailyMap) }
        }
    }
    
    // ============================== Statistics Methods ==============================
    
    /**
     * Get count of all completed tasks.
     * @param callback Callback with the count of completed tasks
     */
    fun getCompletedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getCompletedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of tasks completed late.
     * @param callback Callback with the count of late completed tasks
     */
    fun getLateCompletedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getLateCompletedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of tasks completed on time.
     * @param callback Callback with the count of on-time completed tasks
     */
    fun getOnTimeCompletedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getOnTimeCompletedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of tasks completed early.
     * @param callback Callback with the count of early completed tasks
     */
    fun getEarlyCompletedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getEarlyCompletedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of uncompleted tasks.
     * @param callback Callback with the count of uncompleted tasks
     */
    fun getUncompletedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getUncompletedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of tasks created via voice command.
     * @param callback Callback with the count of voice-created tasks
     */
    fun getVoiceCreatedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getVoiceCreatedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of tasks created via gesture.
     * @param callback Callback with the count of gesture-created tasks
     */
    fun getGestureCreatedTasksCount(callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getGestureCreatedTasksCount()
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    // ============================== Task CRUD Operations ==============================
    
    /**
     * Create a new task in the database.
     * @param task The task to create
     * @param callback Optional callback with the ID of the created task
     */
    fun createTask(task: Task, callback: ((Long) -> Unit)? = null) {
        coroutineScope.launch {
            val id = taskRepository.insertTask(task)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it(id)
                }
            }
        }
    }
    
    /**
     * Create a new task in the database from the app's Task model.
     * @param appTask The app's Task model to create
     * @param callback Optional callback with the ID of the created task
     */
    fun createAppTask(appTask: com.b1gbr0ther.Task, callback: ((Long) -> Unit)? = null) {
        coroutineScope.launch {
            val id = taskRepository.insertAppTask(appTask)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it(id)
                }
            }
        }
    }
    
    /**
     * Get a task by its ID asynchronously.
     * @param id The ID of the task to retrieve
     * @param callback Callback with the task, or null if not found
     */
    fun getTask(id: Long, callback: (Task?) -> Unit) {
        coroutineScope.launch {
            val task = taskRepository.getTaskById(id)
            withContext(Dispatchers.Main) {
                callback(task)
            }
        }
    }
    
    /**
     * Get all tasks asynchronously.
     * @param callback Callback with the list of all tasks
     */
    fun getAllTasks(callback: (List<Task>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getAllTasks()
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }

    fun getAllTasksId(callback: (List<Int>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getAllTasks()
            withContext(Dispatchers.Main) {
                var tasksId = mutableListOf<Int>()

                for (task in tasks){
                    tasksId.add(task.id.toInt())
                }
                callback(tasksId)
            }
        }
    }
    
    /**
     * Get tasks by completion status asynchronously.
     * @param isCompleted Whether to retrieve completed or incomplete tasks
     * @param callback Callback with the list of tasks with the specified completion status
     */
    fun getTasksByCompletionStatus(isCompleted: Boolean, callback: (List<Task>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getTasksByCompletionStatus(isCompleted)
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }
    
    /**
     * Get tasks by time range asynchronously.
     * @param startTime The start time of the range
     * @param endTime The end time of the range
     * @param callback Callback with the list of tasks with start times within the specified range
     */
    fun getTasksByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime, callback: (List<Task>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getTasksByTimeRange(startTime, endTime)
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }
    
    /**
     * Get tasks that are breaks asynchronously.
     * @param callback Callback with the list of tasks that are marked as breaks
     */
    fun getBreaks(callback: (List<Task>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getBreaks()
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }
    
    /**
     * Get tasks that are preplanned asynchronously.
     * @param callback Callback with the list of tasks that are marked as preplanned
     */
    fun getPreplannedTasks(callback: (List<Task>) -> Unit) {
        coroutineScope.launch {
            val tasks = taskRepository.getPreplannedTasks()
            withContext(Dispatchers.Main) {
                callback(tasks)
            }
        }
    }
    
    /**
     * Update a task in the database.
     * @param task The task to update
     * @param callback Optional callback when the update is complete
     */
    fun updateTask(task: Task, callback: (() -> Unit)? = null) {
        coroutineScope.launch {
            taskRepository.updateTask(task)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it()
                }
            }
        }
    }
    
    /**
     * Update a task from the app's Task model.
     * @param appTask The app's Task model to update
     * @param id The ID of the task to update
     * @param callback Optional callback when the update is complete
     */
    fun updateAppTask(appTask: com.b1gbr0ther.Task, id: Long, callback: (() -> Unit)? = null) {
        coroutineScope.launch {
            taskRepository.updateAppTask(appTask, id)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it()
                }
            }
        }
    }
    
    /**
     * Delete a task from the database.
     * @param task The task to delete
     * @param callback Optional callback when the deletion is complete
     */
    fun deleteTask(task: Task, callback: (() -> Unit)? = null) {
        coroutineScope.launch {
            taskRepository.deleteTask(task)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it()
                }
            }
        }
    }
    
    /**
     * Delete a task by its ID.
     * @param id The ID of the task to delete
     * @param callback Optional callback when the deletion is complete
     */
    fun deleteTask(id: Long, callback: (() -> Unit)? = null) {
        coroutineScope.launch {
            taskRepository.deleteTaskById(id)
            callback?.let { 
                withContext(Dispatchers.Main) {
                    it()
                }
            }
        }
    }
    
    /**
     * Get count of tasks for a specific category.
     * @param category The category to count tasks for
     * @param callback Callback with the count of tasks in the category
     */
    fun getTaskCountByCategory(category: TaskCategory, callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getTaskCountByCategory(category)
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get count of completed tasks for a specific category.
     * @param category The category to count completed tasks for
     * @param callback Callback with the count of completed tasks in the category
     */
    fun getCompletedTaskCountByCategory(category: TaskCategory, callback: (Int) -> Unit) {
        coroutineScope.launch {
            val count = taskRepository.getCompletedTaskCountByCategory(category)
            withContext(Dispatchers.Main) {
                callback(count)
            }
        }
    }
    
    /**
     * Get all distinct categories used in tasks.
     * @param callback Callback with list of all categories
     */
    fun getAllCategories(callback: (List<String>) -> Unit) {
        coroutineScope.launch {
            val categories = taskRepository.getAllCategories()
            withContext(Dispatchers.Main) {
                callback(categories)
            }
        }
    }
}
