package com.b1gbr0ther.data.database.repository

import com.b1gbr0ther.data.database.dao.TaskDao
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDateTime

/**
 * Repository class for Task entity.
 * Provides a clean API for accessing task data from the database.
 */
class TaskRepository(private val taskDao: TaskDao) {
    /**
     * Insert a new task into the database.
     * @param task The task to insert
     * @return The ID of the inserted task
     */
    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }
    
    /**
     * Insert a task from the app's Task model.
     * @param appTask The app's Task model to insert
     * @return The ID of the inserted task
     */
    suspend fun insertAppTask(appTask: com.b1gbr0ther.Task): Long {
        val task = Task.fromAppTask(appTask)
        return taskDao.insertTask(task)
    }
    
    /**
     * Update an existing task in the database.
     * @param task The task to update
     */
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    /**
     * Update a task from the app's Task model.
     * @param appTask The app's Task model to update
     * @param id The ID of the task to update
     */
    suspend fun updateAppTask(appTask: com.b1gbr0ther.Task, id: Long) {
        // Get the existing task first to preserve any properties not being updated
        val existingTask = taskDao.getTaskById(id)
        if (existingTask != null) {
            val updatedTask = Task.fromAppTask(appTask, id)
            taskDao.updateTask(updatedTask)
        }
    }
    
    /**
     * Delete a task from the database.
     * @param task The task to delete
     */
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    /**
     * Delete a task by its ID.
     * @param id The ID of the task to delete
     */
    suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }
    
    /**
     * Get a task by its ID.
     * @param id The ID of the task to retrieve
     * @return The task with the specified ID, or null if not found
     */
    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }
    
    /**
     * Get all tasks from the database.
     * @return A list of all tasks
     */
    suspend fun getAllTasks(): List<Task> {
        return taskDao.getAllTasks()
    }
    
    /**
     * Get tasks by completion status.
     * @param isCompleted Whether to retrieve completed or incomplete tasks
     * @return A list of tasks with the specified completion status
     */
    suspend fun getTasksByCompletionStatus(isCompleted: Boolean): List<Task> {
        return taskDao.getTasksByCompletionStatus(isCompleted)
    }
    
    /**
     * Get tasks by time range.
     * @param startTime The start time of the range
     * @param endTime The end time of the range
     * @return A list of tasks with start times within the specified range
     */
    suspend fun getTasksByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): List<Task> {
        return taskDao.getTasksByTimeRange(startTime, endTime)
    }
    
    /**
     * Get tasks that are breaks.
     * @return A list of tasks that are marked as breaks
     */
    suspend fun getBreaks(): List<Task> {
        return taskDao.getBreaks()
    }
    
    /**
     * Get tasks that are preplanned.
     * @return A list of tasks that are marked as preplanned
     */
    suspend fun getPreplannedTasks(): List<Task> {
        return taskDao.getPreplannedTasks()
    }
}
