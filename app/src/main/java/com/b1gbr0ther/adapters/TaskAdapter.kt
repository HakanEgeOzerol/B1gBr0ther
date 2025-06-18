package com.b1gbr0ther.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task

/**
 * Adapter for displaying Task entities in a RecyclerView.
 */
class TaskAdapter(
    private var tasks: List<Task> = emptyList(),
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    /**
     * Update the list of tasks in the adapter.
     * @param newTasks The new list of tasks to display
     */
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
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
        private val tvPreplanned: TextView = itemView.findViewById(R.id.tvPreplanned)
        private val tvCompleted: TextView = itemView.findViewById(R.id.tvCompleted)
        private val tvBreak: TextView = itemView.findViewById(R.id.tvBreak)
        private val btnDeleteTask: Button = itemView.findViewById(R.id.btnDeleteTask)

        fun bind(task: Task) {
            tvTaskId.text = task.id.toString()
            tvTaskName.text = task.taskName
            tvCreationMethod.text = task.creationMethod.toString()
            
            // Show/hide status indicators
            tvPreplanned.visibility = if (task.isPreplanned) View.VISIBLE else View.GONE
            tvCompleted.visibility = if (task.isCompleted) View.VISIBLE else View.GONE
            tvBreak.visibility = if (task.isBreak) View.VISIBLE else View.GONE
            
            // Set delete button click listener
            btnDeleteTask.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }
}
