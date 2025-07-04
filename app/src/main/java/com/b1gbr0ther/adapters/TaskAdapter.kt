package com.b1gbr0ther.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.TaskCategory

/**
 * Adapter for displaying Task entities in a RecyclerView.
 */
class TaskAdapter(
    private var tasks: List<Task> = emptyList(),
    private val onDeleteClick: ((Task) -> Unit)? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Set to keep track of selected task IDs
    private val selectedTaskIds = mutableSetOf<Long>()
    
    // Selection change listener
    private var onSelectionChangedListener: (() -> Unit)? = null
    
    /**
     * Update the list of tasks in the adapter.
     * @param newTasks The new list of tasks to display
     */
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        // Clear selections when tasks are updated
        selectedTaskIds.clear()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    /**
     * Set a listener to be notified when selection changes
     */
    fun setOnSelectionChangedListener(listener: () -> Unit) {
        onSelectionChangedListener = listener
    }
    
    /**
     * Get the number of selected tasks
     */
    fun getSelectedCount(): Int {
        return selectedTaskIds.size
    }
    
    /**
     * Get the list of selected tasks
     */
    fun getSelectedTasks(): List<Task> {
        return tasks.filter { it.id in selectedTaskIds }
    }
    
    /**
     * Select all tasks
     */
    fun selectAll() {
        tasks.forEach { task -> selectedTaskIds.add(task.id) }
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    /**
     * Deselect all tasks
     */
    fun deselectAll() {
        selectedTaskIds.clear()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    /**
     * Select all tasks in a specific category
     */
    fun selectAllInCategory(category: TaskCategory) {
        tasks.filter { it.category == category }.forEach { task ->
            selectedTaskIds.add(task.id)
        }
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    /**
     * Get the position of a task in the adapter by its ID
     */
    fun getPositionForTask(taskId: Long): Int {
        return tasks.indexOfFirst { it.id == taskId }
    }
    
    /**
     * Select a specific task by its ID
     */
    fun selectTask(taskId: Long) {
        selectedTaskIds.add(taskId)
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskId: TextView = itemView.findViewById(R.id.tvTaskId)
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvCreationMethod: TextView = itemView.findViewById(R.id.tvCreationMethod)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvPreplanned: TextView = itemView.findViewById(R.id.tvPreplanned)
        private val tvCompleted: TextView = itemView.findViewById(R.id.tvCompleted)
        private val tvBreak: TextView = itemView.findViewById(R.id.tvBreak)
        private val tvSelected: TextView = itemView.findViewById(R.id.tvSelected)
        private val btnDeleteTask: Button? = itemView.findViewById(R.id.btnDeleteTask)
        // No checkbox in the layout, so we'll use item click instead

        fun bind(task: Task) {
            tvTaskId.text = task.id.toString()
            tvTaskName.text = task.taskName
            tvCreationMethod.text = task.creationMethod.toString()
            
            // Display task category with color coding
            tvCategory.text = task.category.name
            
            // Apply color based on category
            val categoryColorResId = when (task.category) {
                TaskCategory.PROFESSIONAL -> R.color.category_work
                TaskCategory.PERSONAL -> R.color.category_personal
                TaskCategory.FAMILY -> R.color.category_family
                TaskCategory.LEISURE -> R.color.category_hobby
                TaskCategory.OTHER -> R.color.category_other
                else -> R.color.category_other
            }
            tvCategory.setTextColor(itemView.context.getColor(categoryColorResId))
            
            // Show/hide status indicators
            tvPreplanned.visibility = if (task.isPreplanned) View.VISIBLE else View.GONE
            tvCompleted.visibility = if (task.isCompleted) View.VISIBLE else View.GONE
            tvBreak.visibility = if (task.isBreak) View.VISIBLE else View.GONE
            
            // Set delete button click listener if available
            btnDeleteTask?.setOnClickListener {
                onDeleteClick?.invoke(task)
            }
            
            // Make the entire item clickable for selection
            itemView.setOnClickListener {
                if (selectedTaskIds.contains(task.id)) {
                    selectedTaskIds.remove(task.id)
                } else {
                    selectedTaskIds.add(task.id)
                }
                notifyItemChanged(adapterPosition)
                onSelectionChangedListener?.invoke()
            }
            
            // Visual indication of selection state
            val isSelected = selectedTaskIds.contains(task.id)
            itemView.isSelected = isSelected
            
            // Show/hide the selection indicator
            tvSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}
