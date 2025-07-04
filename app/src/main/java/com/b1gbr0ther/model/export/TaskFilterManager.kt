package com.b1gbr0ther.model.export

import com.b1gbr0ther.model.TaskCategory
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDate

/**
 * Manages task filtering functionality for exports
 * Handles filtering by date range, task type, and category
 */
class TaskFilterManager {
    // Filter state
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var showCompleted: Boolean = false
    private var showBreaks: Boolean = false
    private var showPreplanned: Boolean = false
    private var selectedCategory: TaskCategory? = null
    
    /**
     * Sets the date range filter
     */
    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        startDate = start
        endDate = end
    }
    
    /**
     * Sets the task type filters
     */
    fun setTaskTypeFilters(completed: Boolean, breaks: Boolean, preplanned: Boolean) {
        showCompleted = completed
        showBreaks = breaks
        showPreplanned = preplanned
    }
    
    /**
     * Sets the category filter
     */
    fun setCategory(category: TaskCategory?) {
        selectedCategory = category
    }
    
    /**
     * Applies all current filters to the provided task list
     */
    fun applyFilters(tasks: List<Task>): List<Task> {
        // Check if any task type filter is active
        val anyTaskTypeFilterActive = showCompleted || showBreaks || showPreplanned
        
        return tasks.filter { task ->
            // Filter by date range if set
            val withinDateRange = if (startDate != null && endDate != null) {
                val taskDate = task.startTime?.toLocalDate()
                taskDate != null && !taskDate.isBefore(startDate) && !taskDate.isAfter(endDate)
            } else {
                true // No date filter
            }
            
            // Apply task type filters only if at least one filter is active
            val passesCompletedFilter = if (anyTaskTypeFilterActive) {
                showCompleted && task.isCompleted || !showCompleted && !task.isCompleted
            } else {
                true // Show all if no filter is active
            }
            
            val passesBreakFilter = if (anyTaskTypeFilterActive) {
                showBreaks && task.isBreak || !showBreaks && !task.isBreak
            } else {
                true // Show all if no filter is active
            }
            
            val passesPreplannedFilter = if (anyTaskTypeFilterActive) {
                showPreplanned && task.isPreplanned || !showPreplanned && !task.isPreplanned
            } else {
                true // Show all if no filter is active
            }
            
            // Apply category filter if a specific category is selected
            val passesCategoryFilter = selectedCategory == null || task.category == selectedCategory
            
            withinDateRange && passesCompletedFilter && passesBreakFilter && 
                passesPreplannedFilter && passesCategoryFilter
        }
    }
    
    /**
     * Reset all filters to default state
     */
    fun resetFilters() {
        startDate = null
        endDate = null
        showCompleted = false
        showBreaks = false
        showPreplanned = false
        selectedCategory = null
    }
    
    /**
     * Get current start date
     */
    fun getStartDate(): LocalDate? = startDate
    
    /**
     * Get current end date
     */
    fun getEndDate(): LocalDate? = endDate
    
    /**
     * Get current category
     */
    fun getSelectedCategory(): TaskCategory? = selectedCategory
    
    /**
     * Get completed tasks filter state
     */
    fun isShowingCompleted(): Boolean = showCompleted
    
    /**
     * Get breaks filter state
     */
    fun isShowingBreaks(): Boolean = showBreaks
    
    /**
     * Get preplanned tasks filter state
     */
    fun isShowingPreplanned(): Boolean = showPreplanned
}
