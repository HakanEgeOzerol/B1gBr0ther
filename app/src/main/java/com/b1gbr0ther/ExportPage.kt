package com.b1gbr0ther

import android.app.DatePickerDialog
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
import android.view.LayoutInflater
import android.widget.TextView
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.ui.TaskAdapter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar

class ExportPage : AppCompatActivity() {
    private lateinit var menuBar: MenuBar
    private lateinit var exportButton: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme before setting content view
        ThemeManager.applyTheme(this)
        appliedTheme = ThemeManager.getCurrentTheme(this)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_page)

        menuBar = findViewById(R.id.menuBar)
        exportButton = findViewById(R.id.exportButton)
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

        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(emptyList())
        taskAdapter.setOnSelectionChangedListener {
            updateExportButtonText()
        }
        recordingsRecyclerView.adapter = taskAdapter

        exportButton.setOnClickListener {
            handleExport()
        }
        
        setupFilterButtons()

        templateManager = ExportTemplateManager()
        fetchAndDisplayTasks()
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
        exportButton.text = "Export Selected ($selectedCount/$totalCount)"
    }

    private fun handleExport() {
        val selectedTasks = taskAdapter.getSelectedTasks()
        
        if (selectedTasks.isEmpty()) {
            Toast.makeText(this, "Please select tasks to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        showTemplateSelectionDialog(selectedTasks)
    }
    
    private fun showTemplateSelectionDialog(tasksToExport: List<Task>) {
        val templates = templateManager.getAllTemplates()
        val templateNames = templates.map { "${it.getFileExtension().uppercase()} Format" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Choose Export Format")
            .setItems(templateNames) { _, which ->
                val selectedTemplate = templates[which]
                exportTasks(tasksToExport, selectedTemplate)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportTasks(tasks: List<Task>, template: com.b1gbr0ther.models.export.templates.ExportTemplate) {
        try {
            val exportedData = template.format(tasks)
            saveExportedData(exportedData, template.getFileExtension(), template.getMimeType())
            Toast.makeText(this, "Exported ${tasks.size} tasks as ${template.getFileExtension().uppercase()}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createSampleTasks(onComplete: () -> Unit) {
        Toast.makeText(this, "Creating sample tasks for testing...", Toast.LENGTH_SHORT).show()
        
        val sampleTasks = listOf(
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Morning Planning",
                startTime = java.time.LocalDateTime.now().minusHours(2),
                endTime = java.time.LocalDateTime.now().minusHours(1).minusMinutes(30),
                isPreplanned = true,
                isCompleted = true,
                isBreak = false
            ),
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Coffee Break",
                startTime = java.time.LocalDateTime.now().minusHours(1).minusMinutes(30),
                endTime = java.time.LocalDateTime.now().minusHours(1),
                isPreplanned = false,
                isCompleted = true,
                isBreak = true
            ),
            com.b1gbr0ther.data.database.entities.Task(
                taskName = "Development Work",
                startTime = java.time.LocalDateTime.now().minusHours(1),
                endTime = java.time.LocalDateTime.now(),
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
                    Toast.makeText(this, "Created ${sampleTasks.size} sample tasks", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        }
    }
    
    private fun saveExportedData(data: String, fileExtension: String, mimeType: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val filename = "tasks_export_$timestamp.$fileExtension"
            
            val exportDir = getExternalFilesDir(null)
            val exportFile = java.io.File(exportDir, filename)
            
            exportFile.writeText(data)
            
            shareExportedFile(exportFile, mimeType)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving export: ${e.message}", Toast.LENGTH_LONG).show()
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
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Task Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(android.content.Intent.createChooser(shareIntent, "Share export file"))
            
        } catch (e: Exception) {
            Toast.makeText(this, "File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
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
