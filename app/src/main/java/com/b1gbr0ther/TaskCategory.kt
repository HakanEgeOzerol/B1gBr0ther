package com.b1gbr0ther

/**
 * Enum class representing categories for tasks.
 * Each category has a display name and an associated color resource ID.
 * 
 * Categories provide a way to organize and filter tasks in the app.
 * Each task must have exactly one category. If no category is specified,
 * the default category (OTHER) is used.
 */
enum class TaskCategory(val displayName: String, val colorResourceId: Int) {
    /**
     * Work-related tasks and professional activities
     */
    WORK("Work", R.color.category_work),
    
    /**
     * Educational tasks and study sessions
     */
    STUDY("Study", R.color.category_study),
    
    /**
     * Personal tasks and errands
     */
    PERSONAL("Personal", R.color.category_personal),
    
    /**
     * Health-related activities and wellness tasks
     */
    HEALTH("Health", R.color.category_health),
    
    /**
     * Family-related responsibilities and activities
     */
    FAMILY("Family", R.color.category_family),
    
    /**
     * Leisure and hobby activities
     */
    HOBBY("Hobby", R.color.category_hobby),
    
    /**
     * Tasks that don't fit into other categories
     * This is the default category for tasks with unspecified categories
     */
    OTHER("Other", R.color.category_other);

    override fun toString(): String {
        return displayName
    }
    
    companion object {
        /**
         * Get the default category to use when no category is specified.
         * @return The default TaskCategory
         */
        fun getDefault(): TaskCategory = WORK
        
        /**
         * Get a TaskCategory by its display name.
         * @param name The display name of the category to find
         * @return The matching TaskCategory or OTHER if not found
         */
        fun fromDisplayName(name: String): TaskCategory {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}
