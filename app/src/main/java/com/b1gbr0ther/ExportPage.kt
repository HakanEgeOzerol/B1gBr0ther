package com.b1gbr0ther

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.adapters.TaskAdapter
import com.b1gbr0ther.data.database.AppDatabase
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.model.export.ExportManager
import com.b1gbr0ther.model.export.TaskFilterManager
import com.b1gbr0ther.model.import.ImportFileParser
import com.b1gbr0ther.model.import.UnsupportedFileTypeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Activity for exporting tasks
 * UI layer that delegates business logic to manager classes
 */
class ExportPage : AppCompatActivity() {
    // UI components
    private lateinit var menuBar: MenuBar
    private lateinit var exportButton: Button
    private lateinit var selectAllButton: Button
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var completedCheckbox: CheckBox
    private lateinit var breaksCheckbox: CheckBox
    private lateinit var preplannedCheckbox: CheckBox
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var importButton: Button
    
    // Data and managers
    private lateinit var databaseManager: DatabaseManager
    private lateinit var exportManager: ExportManager
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var handler: Handler
    
    private var allTasks: List<Task> = emptyList()
    private var appliedTheme: Int = -1
    private var selectedCategory: TaskCategory? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_page)

        // Initialize UI components
        initializeUIComponents()
        
        // Initialize managers
        databaseManager = DatabaseManager(this)
        exportManager = ExportManager(this)
        handler = Handler(Looper.getMainLooper())
        
        // Setup UI interactions
        setupUIInteractions()
        
        // Load data
        fetchAndDisplayTasks()
        
        // Check if we should auto-export based on voice command
        handleVoiceCommandExport()
    }
    
    /**
     * Initialize all UI components
     */
    private fun initializeUIComponents() {
        menuBar = findViewById(R.id.menuBar)
        exportButton = findViewById(R.id.exportButton)
        selectAllButton = findViewById(R.id.selectAllButton)
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        completedCheckbox = findViewById(R.id.completedCheckbox)
        breaksCheckbox = findViewById(R.id.breaksCheckbox)
        preplannedCheckbox = findViewById(R.id.preplannedCheckbox)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)
        importButton = findViewById(R.id.importButton)

        menuBar.setActivePage(0)
        menuBar.bringToFront()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Setup RecyclerView
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(
            emptyList(),
            onDeleteClick = { task -> handleTaskDelete(task) }
        )
        taskAdapter.setOnSelectionChangedListener {
            updateExportButtonText()
            updateSelectAllButtonText()
        }
        recordingsRecyclerView.adapter = taskAdapter
    }
    
    /**
     * Setup all UI interaction listeners
     */
    private fun setupUIInteractions() {
        // Setup category filter spinner
        setupCategoryFilter()
        
        // Setup buttons
        exportButton.setOnClickListener { handleExport() }
        selectAllButton.setOnClickListener { handleSelectAll() }
        importButton.setOnClickListener { openFilePicker() }
        
        // Initialize button texts
        updateExportButtonText()
        updateSelectAllButtonText()
        
        // Setup filter buttons
        setupFilterButtons()
    }
    
    /**
     * Setup filter buttons and their listeners
     */
    private fun setupFilterButtons() {
        startDateButton.setOnClickListener { showDatePicker(true) }
        endDateButton.setOnClickListener { showDatePicker(false) }
        
        completedCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
        breaksCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
        preplannedCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
    }
    
    /**
     * Show date picker dialog for start or end date
     */
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStartDate) {
                    exportManager.getFilterManager().setDateRange(selectedDate, exportManager.getFilterManager().getEndDate())
                    startDateButton.text = "Start: ${selectedDate}"
                } else {
                    exportManager.getFilterManager().setDateRange(exportManager.getFilterManager().getStartDate(), selectedDate)
                    endDateButton.text = "End: ${selectedDate}"
                }
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    /**
     * Setup category filter spinner
     */
    private fun setupCategoryFilter() {
        val categories = TaskCategory.values().toList()
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { it.displayName }.toMutableList().apply { add(0, "All Categories") }
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = categoryAdapter

        categoryFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) null else categories[position - 1]
                exportManager.getFilterManager().setCategory(selectedCategory)
                applyFilters()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedCategory = null
                exportManager.getFilterManager().setCategory(null)
                applyFilters()
            }
        }
        
        // Add long click listener to category spinner for quick category export
        categoryFilterSpinner.setOnLongClickListener {
            if (selectedCategory != null) {
                showCategoryExportOptionsDialog()
            } else {
                Toast.makeText(this, getString(R.string.task_category) + " not selected", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
    
    /**
     * Apply filters to the task list
     */
    private fun applyFilters() {
        // Update filter manager with current UI state
        exportManager.getFilterManager().setTaskTypeFilters(
            completedCheckbox.isChecked,
            breaksCheckbox.isChecked,
            preplannedCheckbox.isChecked
        )
        
        // Apply filters
        val filteredTasks = exportManager.getFilterManager().applyFilters(allTasks)
        taskAdapter.updateTasks(filteredTasks)
        updateExportButtonText()
    }
    
    /**
     * Update export button text based on selection
     */
    private fun updateExportButtonText() {
        val selectedCount = taskAdapter.getSelectedCount()
        val totalCount = taskAdapter.itemCount
        exportButton.text = getString(R.string.export_selected_format, selectedCount, totalCount)
    }
    
    /**
     * Update select all button text based on selection state
     */
    private fun updateSelectAllButtonText() {
        val selectedCount = taskAdapter.getSelectedCount()
        val totalCount = taskAdapter.itemCount
        selectAllButton.text = if (selectedCount == totalCount && totalCount > 0) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
    }
    
    /**
     * Handle select all button click
     */
    private fun handleSelectAll() {
        val selectedCount = taskAdapter.getSelectedCount()
        val totalCount = taskAdapter.itemCount
        
        if (selectedCount == totalCount && totalCount > 0) {
            // Deselect all
            taskAdapter.deselectAll()
        } else {
            // Select all
            taskAdapter.selectAll()
        }
        updateExportButtonText()
        updateSelectAllButtonText()
    }
    
    /**
     * Handle task deletion
     */
    private fun handleTaskDelete(task: Task) {
        // Show confirmation dialog before deleting
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete_task_message, task.taskName))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // Delete the task from database
                databaseManager.deleteTask(task.id) {
                    // Refresh the task list after deletion
                    fetchAndDisplayTasks()
                    Toast.makeText(this, getString(R.string.task_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Voice command export methods
    fun exportCSV() {
        selectAllAndExport("csv")
    }
    
    fun exportJSON() {
        selectAllAndExport("json")
    }
    
    fun exportHTML() {
        selectAllAndExport("html")
    }
    
    fun exportMarkdown() {
        selectAllAndExport("markdown")
    }
    
    fun exportXML() {
        selectAllAndExport("xml")
    }
    
    fun exportText() {
        selectAllAndExport("text")
    }
    
    /**
     * Handle voice command export
     */
    private fun handleVoiceCommandExport() {
        val exportFormat = intent.getStringExtra("export_format")
        if (exportFormat != null) {
            // Wait a bit for tasks to load, then trigger the export
            handler.postDelayed({
                selectAllAndExport(exportFormat)
            }, 500)
        }
    }
    
    /**
     * Select all tasks and export in specified format
     */
    private fun selectAllAndExport(format: String) {
        // Select all tasks first
        taskAdapter.selectAll()
        updateExportButtonText()
        updateSelectAllButtonText()
        
        val selectedTasks = taskAdapter.getSelectedTasks()
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_tasks_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Export tasks using the export manager
        exportManager.exportTasks(selectedTasks, format)
    }

    /**
     * Handle export button click
     */
    private fun handleExport() {
        val selectedTasks = taskAdapter.getSelectedTasks()
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_tasks_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        
        showExportFormatDialog(selectedTasks)
    }
    
    /**
     * Show dialog with category export options
     */
    private fun showCategoryExportOptionsDialog() {
        if (selectedCategory == null) {
            // If no category is selected, show category selection dialog first
            showCategorySelectionDialog { selectedCat ->
                selectedCategory = selectedCat
                categoryFilterSpinner.setSelection(TaskCategory.values().indexOf(selectedCat) + 1)
                exportManager.getFilterManager().setCategory(selectedCategory)
                applyFilters() // Apply filters to update the task list
                showCategoryExportTypeDialog()
            }
        } else {
            showCategoryExportTypeDialog()
        }
    }
    
    /**
     * Show dialog to select a category for export
     */
    private fun showCategorySelectionDialog(onCategorySelected: (TaskCategory) -> Unit) {
        val categories = TaskCategory.values()
        val categoryNames = categories.map { it.displayName }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.task_category))
            .setItems(categoryNames) { _, which ->
                onCategorySelected(categories[which])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Show dialog to choose between exporting all tasks or selected tasks from the current category
     */
    private fun showCategoryExportTypeDialog() {
        val options = arrayOf(
            getString(R.string.export_category_all),
            getString(R.string.export_category_selected)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_category_options))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportAllTasksFromCategory()
                    1 -> exportSelectedTasksFromCategory()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Export all tasks from the currently selected category
     */
    private fun exportAllTasksFromCategory() {
        if (selectedCategory == null) return
        
        val tasksInCategory = allTasks.filter { it.category == selectedCategory }
        if (tasksInCategory.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_tasks_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show export format dialog with only tasks from the selected category
        showExportFormatDialog(tasksInCategory)
    }
    
    /**
     * Export only selected tasks from the currently selected category
     */
    private fun exportSelectedTasksFromCategory() {
        if (selectedCategory == null) return
        
        val selectedTasksInCategory = taskAdapter.getSelectedTasks().filter { it.category == selectedCategory }
        if (selectedTasksInCategory.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_tasks_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show export format dialog
        showExportFormatDialog(selectedTasksInCategory)
    }
    
    /**
     * Show dialog to select export format
     */
    private fun showExportFormatDialog(tasks: List<Task>) {
        val formats = exportManager.getAvailableFormats()
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_export_format))
        builder.setItems(formats) { _, which ->
            val format = formats[which]
            exportManager.exportTasks(tasks, format)
        }
        builder.show()
    }

    /**
     * Fetch tasks from database and display them
     */
    private fun fetchAndDisplayTasks() {
        databaseManager.getAllTasks { tasks ->
            runOnUiThread {
                allTasks = tasks
                // Reset filters
                exportManager.getFilterManager().resetFilters()
                // Make sure all checkboxes are unchecked by default
                completedCheckbox.isChecked = false
                breaksCheckbox.isChecked = false
                preplannedCheckbox.isChecked = false
                applyFilters() // Apply filters to show all tasks
                updateExportButtonText()
            }
        }
    }

    /**
     * Open file picker for importing tasks
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"  // allow every file type for now, change this to the allowed file types later
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()
        
        // Check if theme has changed and recreate if needed
        val currentTheme = ThemeManager.getCurrentTheme(this)
        if (appliedTheme != -1 && appliedTheme != currentTheme) {
            android.util.Log.d("ExportPage", "Theme changed from $appliedTheme to $currentTheme - recreating activity")
            recreate()
            return
        }
        appliedTheme = currentTheme
        
        menuBar.setActivePage(0)
        updateExportButtonText()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Parse the file and show the confirmation dialog with parsed tasks
                parseFileAndShowConfirmation(uri)
            }
        }
    }
    
    private fun parseFileAndShowConfirmation(uri: Uri) {
        // Show a loading indicator
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Parsing file...")
            setCancelable(false)
            show()
        }
        
        // Use a background thread for parsing to avoid blocking the UI
        Thread {
            try {
                // Parse the file
                val parser = ImportFileParser(this)
                val parsedTasks = parser.parseFile(uri)
                
                // Update UI on the main thread
                runOnUiThread {
                    progressDialog.dismiss()
                    showImportConfirmationDialog(uri, parsedTasks)
                }
            } catch (e: UnsupportedFileTypeException) {
                // Handle unsupported file type exception specifically
                Log.e(TAG, "Unsupported file type: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    // Show a more specific message for unsupported file types
                    Toast.makeText(this, "${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing file: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error parsing file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showImportConfirmationDialog(uri: Uri, tasks: List<Task>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_confirmation, null)
        
        // Get references to views
        val fileNameTextView = dialogView.findViewById<TextView>(R.id.fileNameTextView)
        val tasksCountTextView = dialogView.findViewById<TextView>(R.id.tasksCountTextView)
        val taskNameTextView = dialogView.findViewById<TextView>(R.id.taskNameTextView)
        val taskTimeTextView = dialogView.findViewById<TextView>(R.id.taskTimeTextView)
        val taskDetailsTextView = dialogView.findViewById<TextView>(R.id.taskDetailsTextView)
        val taskCounterTextView = dialogView.findViewById<TextView>(R.id.taskCounterTextView)
        val previousButton = dialogView.findViewById<Button>(R.id.previousButton)
        val nextButton = dialogView.findViewById<Button>(R.id.nextButton)
        val taskCardView = dialogView.findViewById<CardView>(R.id.taskCardView)
        
        // Set the file name
        fileNameTextView.text = uri.lastPathSegment
        
        // Set tasks count
        tasksCountTextView.text = tasks.size.toString()
        
        // Create dialog builder
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(R.string.import_confirmation)
            .setView(dialogView)
        
        // If no tasks found, show a message and only a cancel button
        if (tasks.isEmpty()) {
            taskCardView.visibility = View.GONE
            previousButton.visibility = View.GONE
            nextButton.visibility = View.GONE
            taskCounterTextView.visibility = View.GONE
            
            dialogBuilder.setMessage(R.string.no_tasks_found)
                .setNegativeButton(android.R.string.cancel, null)
        } else {
            // For tasks display, we need to track the current task index
            var currentTaskIndex = 0
            
            // Function to update the task display
            fun updateTaskDisplay() {
                val currentTask = tasks[currentTaskIndex]
                
                // Update task name
                taskNameTextView.text = currentTask.taskName
                
                // Format and update time information
                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val startTimeStr = currentTask.startTime.format(dateTimeFormatter)
                val endTimeStr = currentTask.endTime.format(dateTimeFormatter)
                taskTimeTextView.text = "Start: $startTimeStr - End: $endTimeStr"
                
                // Format and update other details
                val statusText = if (currentTask.isCompleted) "Completed" else "Not Completed"
                val methodText = currentTask.creationMethod.toString()
                val breakText = if (currentTask.isBreak) "Break" else "Task"
                taskDetailsTextView.text = "Status: $statusText | Method: $methodText | Type: $breakText"
                
                // Update counter
                taskCounterTextView.text = getString(R.string.task_counter, currentTaskIndex + 1, tasks.size)
                
                // Update button states
                previousButton.isEnabled = currentTaskIndex > 0
                nextButton.isEnabled = currentTaskIndex < tasks.size - 1
            }
            
            // Set up button click listeners
            previousButton.setOnClickListener {
                if (currentTaskIndex > 0) {
                    currentTaskIndex--
                    updateTaskDisplay()
                }
            }
            
            nextButton.setOnClickListener {
                if (currentTaskIndex < tasks.size - 1) {
                    currentTaskIndex++
                    updateTaskDisplay()
                }
            }
            
            // Initialize the display with the first task
            updateTaskDisplay()
            
            // Add positive and negative buttons to the dialog
            dialogBuilder.setPositiveButton(R.string.import_all) { _, _ ->
                importTasks(tasks)
            }
            .setNegativeButton(android.R.string.cancel, null)
        }

        // Create and show the dialog
        val dialog = dialogBuilder.create()
        dialog.show()
    }
    
    private fun importTasks(tasks: List<Task>) {
        // Show a loading indicator
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Importing tasks...")
            setCancelable(false)
            show()
        }
        
        // Use a background thread for database operations
        Thread {
            try {
                // Use the DatabaseManager to insert tasks
                val insertedIds = mutableListOf<Long>()
                
                // Process each task
                for (task in tasks) {
                    // Reset the ID to 0 to let Room auto-generate a new one
                    task.id = 0
                    
                    // Use the DatabaseManager to create the task
                    // We need to use a callback since createTask is asynchronous
                    val latch = java.util.concurrent.CountDownLatch(1)
                    var newId: Long = 0
                    
                    databaseManager.createTask(task) { id ->
                        newId = id
                        insertedIds.add(id)
                        latch.countDown()
                    }
                    
                    // Wait for the task to be created before continuing
                    latch.await()
                }
                
                // Update UI on the main thread
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Successfully imported ${insertedIds.size} tasks",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing tasks: ${e.message}", e)
                
                // Show error on the main thread
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Error importing tasks: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    companion object {
        private const val PICK_FILE_REQUEST_CODE = 123
        private const val TAG = "ExportPage"
    }
}
