package com.b1gbr0ther.data.database.dao

import androidx.room.*
import com.b1gbr0ther.TaskCategory
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDateTime

/**
 * Data Access Object (DAO) for Task entity.
 * Provides methods to interact with the tasks table in the database.
 */
@Dao
interface TaskDao {
    /**
     * Insert a new task into the database.
     * @param task The task to insert
     * @return The ID of the inserted task
     */
    @Insert
    suspend fun insertTask(task: Task): Long
    
    /**
     * Update an existing task in the database.
     * @param task The task to update
     */
    @Update
    suspend fun updateTask(task: Task)
    
    /**
     * Delete a task from the database.
     * @param task The task to delete
     */
    @Delete
    suspend fun deleteTask(task: Task)
    
    /**
     * Delete a task by its ID.
     * @param id The ID of the task to delete
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
    
    /**
     * Get a task by its ID.
     * @param id The ID of the task to retrieve
     * @return The task with the specified ID, or null if not found
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?
    
    /**
     * Get all tasks from the database.
     * @return A list of all tasks
     */
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>
    
    /**
     * Get tasks by completion status.
     * @param isCompleted Whether to retrieve completed or incomplete tasks
     * @return A list of tasks with the specified completion status
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted")
    suspend fun getTasksByCompletionStatus(isCompleted: Boolean): List<Task>
    
    /**
     * Get tasks that fall within a specific time range.
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return List of tasks with start times within the specified range
     */
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getTasksByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): List<Task>
    
    /**
     * Get count of all completed tasks.
     * @return Number of completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTasksCount(): Int
    
    /**
     * Get count of tasks completed late (endTime > startTime + expected duration).
     * @return Number of late completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE timingStatus = 'LATE' AND isCompleted = 1")
    suspend fun getLateCompletedTasksCount(): Int
    
    /**
     * Get count of tasks completed on time (endTime <= startTime + expected duration).
     * @return Number of on-time completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE timingStatus = 'ON_TIME' AND isCompleted = 1")
    suspend fun getOnTimeCompletedTasksCount(): Int
    
    /**
     * Get count of tasks completed early (endTime < startTime).
     * @return Number of early completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE timingStatus = 'EARLY' AND isCompleted = 1")
    suspend fun getEarlyCompletedTasksCount(): Int
    
    /**
     * Get count of uncompleted tasks.
     * @return Number of uncompleted tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    suspend fun getUncompletedTasksCount(): Int
    
    // Creation method related functions removed
    
    /**
     * Get tasks that are breaks.
     * @return A list of tasks that are marked as breaks
     */
    @Query("SELECT * FROM tasks WHERE isBreak = 1")
    suspend fun getBreaks(): List<Task>
    
    /**
     * Get tasks that are preplanned.
     * @return A list of tasks that are marked as preplanned
     */
    @Query("SELECT * FROM tasks WHERE isPreplanned = 1")
    suspend fun getPreplannedTasks(): List<Task>
    
    /**
     * Get tasks by category.
     * @param category The category to filter tasks by
     * @return A list of tasks with the specified category
     */
    @Query("SELECT * FROM tasks WHERE category = :category")
    suspend fun getTasksByCategory(category: TaskCategory): List<Task>
    
    /**
     * Get tasks that are completed by category.
     * @param category The category to filter tasks by
     * @param isCompleted Whether the tasks are completed or not
     * @return A list of tasks with the specified category and completion status
     */
    @Query("SELECT * FROM tasks WHERE category = :category AND isCompleted = :isCompleted")
    suspend fun getTasksByCategoryAndCompletionStatus(category: TaskCategory, isCompleted: Boolean): List<Task>
    
    /**
     * Get count of tasks by category.
     * @param category The category to count tasks for
     * @return Number of tasks in the specified category
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE category = :category")
    suspend fun getTaskCountByCategory(category: TaskCategory): Int
    
    /**
     * Get count of completed tasks by category.
     * @param category The category to count tasks for
     * @return Number of completed tasks in the specified category
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE category = :category AND isCompleted = 1")
    suspend fun getCompletedTaskCountByCategory(category: TaskCategory): Int
    
    /**
     * Get all categories used in existing tasks.
     * @return A list of distinct categories used in tasks
     */
    @Query("SELECT DISTINCT category FROM tasks")
    suspend fun getAllCategories(): List<String>
}
