package com.b1gbr0ther

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.b1gbr0ther.data.database.AppDatabase
import com.b1gbr0ther.data.database.entities.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Activity to test Room database operations without needing hand gestures.
 * This activity provides a simple UI to test CRUD operations on the Task entity.
 */
class DatabaseTestActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView
    private var currentTaskId: Long = 0
    private var appliedTheme: Int = -1
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            // Apply saved app theme before setting content view
            ThemeManager.applyTheme(this)
            appliedTheme = ThemeManager.getCurrentTheme(this)
            
            setContentView(R.layout.activity_database_test)
            
            // Initialize UI elements
            resultTextView = findViewById(R.id.tv_results)
            
            // Log startup message
            appendResult("Database Test Activity Started")
            
            // Set up button click listeners
            findViewById<Button>(R.id.btn_insert_task)?.setOnClickListener {
                insertTask()
            }
            
            findViewById<Button>(R.id.btn_update_task)?.setOnClickListener {
                updateTask()
            }
            
            findViewById<Button>(R.id.btn_delete_task)?.setOnClickListener {
                deleteTask()
            }
            
            findViewById<Button>(R.id.btn_get_all_tasks)?.setOnClickListener {
                getAllTasks()
            }
        } catch (e: Exception) {
            // Handle any exceptions during initialization
            e.printStackTrace()
            // Show error in a Toast if possible
            android.widget.Toast.makeText(this, "Error initializing: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Insert a new task into the database
     */
    private fun insertTask() {
        try {
            val task = Task(
                taskName = "Test Task ${System.currentTimeMillis()}",
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now().plusHours(1),
                isPreplanned = false,
                isCompleted = false,
                isBreak = false
            )
            
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val taskId = withContext(Dispatchers.IO) {
                        db.taskDao().insertTask(task)
                    }
                    currentTaskId = taskId
                    appendResult("✅ Task inserted successfully with ID: $taskId")
                } catch (e: Exception) {
                    appendResult("❌ Error inserting task: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            appendResult("❌ Error creating task: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Update the last inserted task
     */
    private fun updateTask() {
        if (currentTaskId <= 0) {
            appendResult("❌ No task to update. Insert a task first.")
            return
        }
        
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val task = withContext(Dispatchers.IO) {
                    try {
                        db.taskDao().getTaskById(currentTaskId)
                    } catch (e: Exception) {
                        appendResult("❌ Error retrieving task: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                
                if (task != null) {
                    try {
                        task.taskName = "Updated Task ${System.currentTimeMillis()}"
                        withContext(Dispatchers.IO) {
                            db.taskDao().updateTask(task)
                        }
                        appendResult("✅ Task updated successfully: ${task.taskName}")
                    } catch (e: Exception) {
                        appendResult("❌ Error saving updated task: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    appendResult("❌ Task not found with ID: $currentTaskId")
                }
            } catch (e: Exception) {
                appendResult("❌ Error updating task: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete the last inserted task
     */
    private fun deleteTask() {
        if (currentTaskId <= 0) {
            appendResult("❌ No task to delete. Insert a task first.")
            return
        }
        
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@DatabaseTestActivity)
                val task = withContext(Dispatchers.IO) {
                    db.taskDao().getTaskById(currentTaskId)
                }
                
                if (task != null) {
                    withContext(Dispatchers.IO) {
                        db.taskDao().deleteTask(task)
                    }
                    appendResult("✅ Task deleted successfully with ID: $currentTaskId")
                    currentTaskId = 0
                } else {
                    appendResult("❌ Task not found with ID: $currentTaskId")
                }
            } catch (e: Exception) {
                appendResult("❌ Error deleting task: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get all tasks from the database
     */
    private fun getAllTasks() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@DatabaseTestActivity)
                val tasks = withContext(Dispatchers.IO) {
                    db.taskDao().getAllTasks()
                }
                
                if (tasks.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val result = StringBuilder("📋 All Tasks (${tasks.size}):\n\n")
                    
                    tasks.forEach { task ->
                        result.append("ID: ${task.id}\n")
                        result.append("Name: ${task.taskName}\n")
                        result.append("Start: ${task.startTime.format(formatter)}\n")
                        result.append("End: ${task.endTime.format(formatter)}\n")
                        result.append("Completed: ${task.isCompleted}\n")
                        result.append("Is Break: ${task.isBreak}\n")
                        result.append("------------------------------\n")
                    }
                    
                    appendResult(result.toString())
                } else {
                    appendResult("ℹ️ No tasks found in the database")
                }
            } catch (e: Exception) {
                appendResult("❌ Error getting tasks: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Append text to the result TextView
     */
    private fun appendResult(text: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val formattedText = "[$timestamp] $text\n\n"
        
        runOnUiThread {
            val currentText = resultTextView.text.toString()
            if (currentText.length > 5000) {
                // Prevent the TextView from getting too large
                resultTextView.text = formattedText
            } else {
                resultTextView.text = formattedText + currentText
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if theme has changed and recreate if needed
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && appliedTheme != currentTheme) {
            android.util.Log.d("DatabaseTestActivity", "Theme changed from $appliedTheme to $currentTheme - recreating activity")
            recreate()
            return
        }
        appliedTheme = currentTheme
    }
}
