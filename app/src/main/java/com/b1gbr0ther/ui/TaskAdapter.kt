package com.b1gbr0ther.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task
import java.time.format.DateTimeFormatter

class TaskAdapter(private var tasks: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    
    private val selectedTasks = mutableSetOf<Long>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    private var onSelectionChangedListener: (() -> Unit)? = null
    
    fun setOnSelectionChangedListener(listener: () -> Unit) {
        onSelectionChangedListener = listener
    }
    
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        val nameText: TextView = itemView.findViewById(R.id.taskName)
        val timeText: TextView = itemView.findViewById(R.id.taskTime)
        val statusText: TextView = itemView.findViewById(R.id.taskStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_export, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        
        holder.nameText.text = task.taskName
        holder.timeText.text = "${task.startTime.format(dateFormatter)} - ${task.endTime.format(dateFormatter)}"
        
        val statusParts = mutableListOf<String>()
        if (task.isCompleted) statusParts.add("✓ Completed")
        if (task.isBreak) statusParts.add("☕ Break")
        if (task.isPreplanned) statusParts.add("📅 Preplanned")
        holder.statusText.text = statusParts.joinToString(" • ")
        
        holder.checkBox.isChecked = selectedTasks.contains(task.id)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedTasks.add(task.id)
            } else {
                selectedTasks.remove(task.id)
            }
            onSelectionChangedListener?.invoke()
        }
        
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }
    
    fun getSelectedTasks(): List<Task> {
        return tasks.filter { selectedTasks.contains(it.id) }
    }
    
    fun selectAll() {
        selectedTasks.clear()
        selectedTasks.addAll(tasks.map { it.id })
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    fun clearSelection() {
        selectedTasks.clear()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke()
    }
    
    fun getSelectedCount(): Int = selectedTasks.size
} 