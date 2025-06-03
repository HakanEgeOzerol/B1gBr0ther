package com.b1gbr0ther

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.b1gbr0ther.data.database.DatabaseManager
import com.b1gbr0ther.models.export.ExportTemplateManager

class ExportPage : AppCompatActivity() {
    private lateinit var menuBar: MenuBar
    private lateinit var exportButton: Button
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var databaseManager: DatabaseManager
    private lateinit var templateManager: ExportTemplateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_page)

        // Initialize views
        menuBar = findViewById(R.id.menuBar)
        exportButton = findViewById(R.id.exportButton)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)

        // Setup menu bar
        menuBar.setActivePage(0) // Set export page as active
        menuBar.bringToFront() // Ensure menu bar is on top

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup recycler view
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Add adapter for recordings list

        // Setup export button
        exportButton.setOnClickListener {
            handleExport()
        }

        databaseManager = (application as B1gBr0therApplication).databaseManager
        templateManager = ExportTemplateManager()
    }

    private fun handleExport() {
        // Get all tasks from database and export them
        databaseManager.getAllTasks { tasks ->
            if (tasks.isEmpty()) {
                // Create sample tasks for testing if none exist
                createSampleTasks {
                    // Try export again after creating sample tasks
                    handleExport()
                }
                return@getAllTasks
            }
            
            // For now, export as CSV (you can later add UI to choose format)
            val csvTemplate = templateManager.getTemplateByExtension("csv")
            if (csvTemplate != null) {
                val exportedData = csvTemplate.format(tasks)
                saveExportedData(exportedData, csvTemplate.getFileExtension(), csvTemplate.getMimeType())
                Toast.makeText(this, "Exported ${tasks.size} tasks to CSV", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "CSV export template not found", Toast.LENGTH_SHORT).show()
            }
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
            // Create filename with current timestamp
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val filename = "tasks_export_$timestamp.$fileExtension"
            
            // Save to external files directory
            val exportDir = getExternalFilesDir(null)
            val exportFile = java.io.File(exportDir, filename)
            
            exportFile.writeText(data)
            
            // Share the file
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

    override fun onResume() {
        super.onResume()
        menuBar.setActivePage(0) // Ensure correct menu item is highlighted
    }
}
