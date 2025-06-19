package com.b1gbr0ther

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.adapters.TaskAdapter
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDateTime

/**
 * Activity for testing database operations on Task entities.
 * Provides UI for creating, updating, and viewing tasks.
 */
class DatabaseTesterActivity : AppCompatActivity() {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var taskAdapter: TaskAdapter
    
    // Create Task UI elements
    private lateinit var etCreateTaskName: EditText
    private lateinit var rgCreationMethod: RadioGroup
    private lateinit var rbGesture: RadioButton
    private lateinit var rbVoice: RadioButton
    private lateinit var cbPreplanned: CheckBox
    private lateinit var cbCompleted: CheckBox
    private lateinit var cbBreak: CheckBox
    private lateinit var btnCreateTask: Button
    
    // Update Task UI elements
    private lateinit var etTaskId: EditText
    private lateinit var btnLoadTask: Button
    private lateinit var etUpdateTaskName: EditText
    private lateinit var cbUpdatePreplanned: CheckBox
    private lateinit var cbUpdateCompleted: CheckBox
    private lateinit var cbUpdateBreak: CheckBox
    private lateinit var btnUpdateTask: Button
    
    // Task List UI elements
    private lateinit var btnRefreshTasks: Button
    private lateinit var rvTasks: RecyclerView
    
    // Currently loaded task for update
    private var currentTask: Task? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        
        setContentView(R.layout.activity_database_tester)
        
        // Initialize database manager
        databaseManager = DatabaseManager(this)
        
        // Initialize UI elements
        initializeViews()
        setupRecyclerView()
        setupListeners()
        
        // Load initial task list
        loadTasks()
    }
    
    private fun initializeViews() {
        // Create Task UI elements
        etCreateTaskName = findViewById(R.id.etCreateTaskName)
        rgCreationMethod = findViewById(R.id.rgCreationMethod)
        rbGesture = findViewById(R.id.rbGesture)
        rbVoice = findViewById(R.id.rbVoice)
        cbPreplanned = findViewById(R.id.cbPreplanned)
        cbCompleted = findViewById(R.id.cbCompleted)
        cbBreak = findViewById(R.id.cbBreak)
        btnCreateTask = findViewById(R.id.btnCreateTask)
        
        // Update Task UI elements
        etTaskId = findViewById(R.id.etTaskId)
        btnLoadTask = findViewById(R.id.btnLoadTask)
        etUpdateTaskName = findViewById(R.id.etUpdateTaskName)
        cbUpdatePreplanned = findViewById(R.id.cbUpdatePreplanned)
        cbUpdateCompleted = findViewById(R.id.cbUpdateCompleted)
        cbUpdateBreak = findViewById(R.id.cbUpdateBreak)
        btnUpdateTask = findViewById(R.id.btnUpdateTask)
        
        // Task List UI elements
        btnRefreshTasks = findViewById(R.id.btnRefreshTasks)
        rvTasks = findViewById(R.id.rvTasks)
    }
    
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList()) { task ->
            // Handle delete task click
            deleteTask(task)
        }
        
        rvTasks.apply {
            layoutManager = LinearLayoutManager(this@DatabaseTesterActivity)
            adapter = taskAdapter
        }
    }
    
    private fun setupListeners() {
        // Create Task button
        btnCreateTask.setOnClickListener {
            createTask()
        }
        
        // Load Task button
        btnLoadTask.setOnClickListener {
            loadTaskForUpdate()
        }
        
        // Update Task button
        btnUpdateTask.setOnClickListener {
            updateTask()
        }
        
        // Refresh Tasks button
        btnRefreshTasks.setOnClickListener {
            loadTasks()
        }
    }
    
    private fun createTask() {
        val taskName = etCreateTaskName.text.toString().trim()
        
        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get creation method
        val creationMethod = if (rbGesture.isChecked) CreationMethod.Gesture else CreationMethod.Voice
        
        // Create task entity
        val task = Task(
            id = 0, // Will be auto-generated
            taskName = taskName,
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(1),
            creationMethod = creationMethod,
            isPreplanned = cbPreplanned.isChecked,
            isCompleted = cbCompleted.isChecked,
            isBreak = cbBreak.isChecked
        )
        
        // Save task to database
        databaseManager.createTask(task) { taskId ->
            runOnUiThread {
                Toast.makeText(this, "Task created with ID: $taskId", Toast.LENGTH_SHORT).show()
                
                // Clear input fields
                etCreateTaskName.text?.clear()
                cbPreplanned.isChecked = false
                cbCompleted.isChecked = false
                cbBreak.isChecked = false
                
                // Refresh task list
                loadTasks()
            }
        }
    }
    
    private fun loadTaskForUpdate() {
        val taskIdText = etTaskId.text.toString().trim()
        
        if (taskIdText.isEmpty()) {
            Toast.makeText(this, "Please enter a task ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val taskId = taskIdText.toLong()
            
            databaseManager.getTask(taskId) { task ->
                if (task != null) {
                    // Store current task for update
                    currentTask = task
                    
                    // Populate update fields
                    etUpdateTaskName.setText(task.taskName)
                    cbUpdatePreplanned.isChecked = task.isPreplanned
                    cbUpdateCompleted.isChecked = task.isCompleted
                    cbUpdateBreak.isChecked = task.isBreak
                    
                    Toast.makeText(this, "Task loaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Task not found with ID: $taskId", Toast.LENGTH_SHORT).show()
                    currentTask = null
                    
                    // Clear update fields
                    etUpdateTaskName.text?.clear()
                    cbUpdatePreplanned.isChecked = false
                    cbUpdateCompleted.isChecked = false
                    cbUpdateBreak.isChecked = false
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTask() {
        val currentTask = this.currentTask
        
        if (currentTask == null) {
            Toast.makeText(this, "Please load a task first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val newTaskName = etUpdateTaskName.text.toString().trim()
        
        if (newTaskName.isEmpty()) {
            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update task properties
        currentTask.taskName = newTaskName
        currentTask.isPreplanned = cbUpdatePreplanned.isChecked
        currentTask.isCompleted = cbUpdateCompleted.isChecked
        currentTask.isBreak = cbUpdateBreak.isChecked
        
        // Save updated task to database
        databaseManager.updateTask(currentTask) {
            runOnUiThread {
                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show()
                
                // Clear update fields
                etTaskId.text?.clear()
                etUpdateTaskName.text?.clear()
                cbUpdatePreplanned.isChecked = false
                cbUpdateCompleted.isChecked = false
                cbUpdateBreak.isChecked = false
                
                // Clear current task
                this.currentTask = null
                
                // Refresh task list
                loadTasks()
            }
        }
    }
    
    private fun deleteTask(task: Task) {
        databaseManager.deleteTask(task) {
            runOnUiThread {
                Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show()
                
                // Refresh task list
                loadTasks()
            }
        }
    }
    
    private fun loadTasks() {
        databaseManager.getAllTasks { tasks ->
            runOnUiThread {
                taskAdapter.updateTasks(tasks)
                
                if (tasks.isEmpty()) {
                    Toast.makeText(this, "No tasks found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
