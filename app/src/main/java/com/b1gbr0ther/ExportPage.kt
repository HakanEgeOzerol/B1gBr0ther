package com.b1gbr0ther

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.models.export.ExportTemplateManager
import com.b1gbr0ther.models.export.templates.ExportTemplate
import com.b1gbr0ther.CreationMethod
import com.b1gbr0ther.TimingStatus
import android.view.LayoutInflater
import android.widget.TextView
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.ui.TaskAdapter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import android.os.Handler
import android.os.Looper

class ExportPage : AppCompatActivity() {
    private lateinit var menuBar: MenuBar
    private lateinit var exportButton: Button
    private lateinit var selectAllButton: Button
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var completedCheckbox: CheckBox
    private lateinit var breaksCheckbox: CheckBox
    private lateinit var preplannedCheckbox: CheckBox
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var databaseManager: DatabaseManager
    private lateinit var templateManager: ExportTemplateManager
    private lateinit var taskAdapter: TaskAdapter
    
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private var allTasks: List<Task> = emptyList()
    private var appliedTheme: Int = -1

    private lateinit var handler: Handler

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

        menuBar = findViewById(R.id.menuBar)
        exportButton = findViewById(R.id.exportButton)
        selectAllButton = findViewById(R.id.selectAllButton)
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        completedCheckbox = findViewById(R.id.completedCheckbox)
        breaksCheckbox = findViewById(R.id.breaksCheckbox)
        preplannedCheckbox = findViewById(R.id.preplannedCheckbox)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)

        menuBar.setActivePage(0)
        menuBar.bringToFront()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        databaseManager = DatabaseManager(this)
        
        handler = Handler(Looper.getMainLooper())

        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(emptyList())
        taskAdapter.setOnSelectionChangedListener {
            updateExportButtonText()
            updateSelectAllButtonText()
        }
        recordingsRecyclerView.adapter = taskAdapter

        exportButton.setOnClickListener {
            handleExport()
        }
        
        selectAllButton.setOnClickListener {
            handleSelectAll()
        }
        
        // Initialize button texts
        updateExportButtonText()
        updateSelectAllButtonText()
        
        setupFilterButtons()

        templateManager = ExportTemplateManager()
        fetchAndDisplayTasks()
        
        // Check if we should auto-export based on voice command
        val exportFormat = intent.getStringExtra("export_format")
        if (exportFormat != null) {
            // Wait a bit for tasks to load, then trigger the export
            handler.postDelayed({
                selectAllAndExport(exportFormat)
            }, 500)
        }
    }
    
    private fun setupFilterButtons() {
        startDateButton.setOnClickListener { showDatePicker(true) }
        endDateButton.setOnClickListener { showDatePicker(false) }
        
        val filterListener = { _: Any ->
            applyFilters()
        }
        
        completedCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
        breaksCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
        preplannedCheckbox.setOnCheckedChangeListener { _, _ -> applyFilters() }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStartDate) {
                    startDate = selectedDate
                    startDateButton.text = "Start: ${selectedDate}"
                } else {
                    endDate = selectedDate
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
    
    private fun applyFilters() {
        var filteredTasks = allTasks
        
        if (startDate != null) {
            filteredTasks = filteredTasks.filter { 
                it.startTime.toLocalDate().isAfter(startDate!!.minusDays(1))
            }
        }
        if (endDate != null) {
            filteredTasks = filteredTasks.filter { 
                it.endTime.toLocalDate().isBefore(endDate!!.plusDays(1))
            }
        }
        
        if (completedCheckbox.isChecked) {
            filteredTasks = filteredTasks.filter { it.isCompleted }
        }
        if (breaksCheckbox.isChecked) {
            filteredTasks = filteredTasks.filter { it.isBreak }
        }
        if (preplannedCheckbox.isChecked) {
            filteredTasks = filteredTasks.filter { it.isPreplanned }
        }
        
        taskAdapter.updateTasks(filteredTasks)
        updateExportButtonText()
    }
    
    private fun updateExportButtonText() {
        val selectedCount = taskAdapter.getSelectedCount()
        val totalCount = taskAdapter.itemCount
        exportButton.text = getString(R.string.export_selected_format, selectedCount, totalCount)
    }
    
    private fun updateSelectAllButtonText() {
        val selectedCount = taskAdapter.getSelectedCount()
        val totalCount = taskAdapter.itemCount
        selectAllButton.text = if (selectedCount == totalCount && totalCount > 0) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
    }
    
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
        
        // Debug logging
        android.util.Log.d("ExportPage", "selectAllAndExport called with format: '$format'")
        
        // Find the template for the requested format
        val templates = templateManager.getAllTemplates()
        android.util.Log.d("ExportPage", "Available templates: ${templates.map { "${it.javaClass.simpleName}(${it.getFileExtension()})" }}")
        
        val template = templates.find { template ->
            val directMatch = template.getFileExtension().equals(format, ignoreCase = true)
            val markdownMatch = format.equals("markdown", ignoreCase = true) && template.getFileExtension().equals("md", ignoreCase = true)
            val textMatch = format.equals("text", ignoreCase = true) && template.getFileExtension().equals("txt", ignoreCase = true)
            
            android.util.Log.d("ExportPage", "Checking template ${template.javaClass.simpleName}(${template.getFileExtension()}) against format '$format': directMatch=$directMatch, markdownMatch=$markdownMatch, textMatch=$textMatch")
            
            directMatch || markdownMatch || textMatch
        }
        
        if (template != null) {
            android.util.Log.d("ExportPage", "Selected template: ${template.javaClass.simpleName}(${template.getFileExtension()})")
            exportTasks(selectedTasks, template)
        } else {
            android.util.Log.e("ExportPage", "No template found for format: '$format'")
            Toast.makeText(this, getString(R.string.export_format_not_supported, format), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleExport() {
        val selectedTasks = taskAdapter.getSelectedTasks()
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_tasks_to_export), Toast.LENGTH_SHORT).show()
            return
        }
        
        showTemplateSelectionDialog(selectedTasks)
    }
    
    private fun showTemplateSelectionDialog(tasksToExport: List<Task>) {
        val templates = templateManager.getAllTemplates()
        val templateNames = templates.map { getString(R.string.format_suffix, it.getFileExtension().uppercase()) }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_export_format))
            .setItems(templateNames) { _, which ->
                val selectedTemplate = templates[which]
                exportTasks(tasksToExport, selectedTemplate)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun exportTasks(tasks: List<Task>, template: ExportTemplate) {
        try {
            val exportedData = template.format(tasks)
            saveExportedData(exportedData, tasks, template.getFileExtension(), template.getMimeType())
            Toast.makeText(this, getString(R.string.exported_tasks, tasks.size, template.getFileExtension().uppercase()), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun createSampleTasks(onComplete: () -> Unit) {
        Toast.makeText(this, getString(R.string.creating_sample_tasks), Toast.LENGTH_SHORT).show()
        
        val sampleTasks = listOf(
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Morning Planning",
                startTime = java.time.LocalDateTime.now().minusHours(2),
                endTime = java.time.LocalDateTime.now().minusHours(1).minusMinutes(30),
                creationMethod = CreationMethod.Voice,
                isPreplanned = true,
                isCompleted = true,
                isBreak = false
            ),
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Coffee Break",
                startTime = java.time.LocalDateTime.now().minusHours(1).minusMinutes(30),
                endTime = java.time.LocalDateTime.now().minusHours(1),
                creationMethod = CreationMethod.Voice,
                isPreplanned = false,
                isCompleted = true,
                isBreak = true,
                 timingStatus = com.b1gbr0ther.TimingStatus.ON_TIME
            ),
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Development Work",
                startTime = java.time.LocalDateTime.now().minusHours(1),
                endTime = java.time.LocalDateTime.now(),
                creationMethod = CreationMethod.Gesture,
                isPreplanned = true,
                isCompleted = false,
                isBreak = false
            )
        )
        
        var tasksCreated = 0
        sampleTasks.forEach { task ->
            databaseManager.createTask(task) {
                tasksCreated++
                if (tasksCreated == sampleTasks.size) {
                    Toast.makeText(this, getString(R.string.created_sample_tasks, sampleTasks.size), Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        }
    }
    
    private fun saveExportedData(data: String, tasks: List<Task>, fileExtension: String, mimeType: String) {
        try {
            val taskCount = tasks.size
            val dateFormatter = java.text.SimpleDateFormat("MMM_dd_yyyy", java.util.Locale.getDefault())
            val currentDate = dateFormatter.format(java.util.Date())
            val filename = "${taskCount}_tasks_$currentDate.$fileExtension"
            
            val exportDir = getExternalFilesDir(null)
            val exportFile = java.io.File(exportDir, filename)
            
            exportFile.writeText(data)
            
            shareExportedFile(exportFile, mimeType)
            
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_saving_export, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun shareExportedFile(file: java.io.File, mimeType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.task_export_subject))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_export_file)))
            
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.file_saved_to, file.absolutePath), Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchAndDisplayTasks() {
        databaseManager.getAllTasks { tasks ->
            runOnUiThread {
                allTasks = tasks
                taskAdapter.updateTasks(tasks)
                updateExportButtonText()
            }
        }
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
}
