package com.b1gbr0ther.data.database

import android.content.Context
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.data.database.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Database manager class that handles CRUD operations for Tasks in the B1gBr0ther app.
 * This class provides methods to create, read, update, and delete tasks from the database.
 */
class DatabaseManager(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val taskRepository = TaskRepository(database.taskDao())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
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
}
