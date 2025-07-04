package com.b1gbr0ther.model.export

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.b1gbr0ther.R
import com.b1gbr0ther.model.TaskCategory
import com.b1gbr0ther.data.database.entities.Task
import com.b1gbr0ther.models.export.ExportTemplateManager
import com.b1gbr0ther.models.export.templates.ExportTemplate

/**
 * Manages the export process for tasks
 * Coordinates between TaskFilterManager, ExportTemplateManager, and ExportFileHandler
 */
class ExportManager(private val context: Context) {
    private val templateManager = ExportTemplateManager()
    private val fileHandler = ExportFileHandler(context)
    private val filterManager = TaskFilterManager()
    
    /**
     * Exports tasks using the specified format
     * 
     * @param tasks The tasks to export
     * @param format The format to export in (e.g., "csv", "json")
     * @return True if export was successful, false otherwise
     */
    fun exportTasks(tasks: List<Task>, format: String): Boolean {
        try {
            // Find the template for the requested format
            val templates = templateManager.getAllTemplates()
            Log.d("ExportManager", "Available templates: ${templates.map { "${it.javaClass.simpleName}(${it.getFileExtension()})" }}")
            
            val template = findTemplateForFormat(format)
            
            if (template != null) {
                Log.d("ExportManager", "Selected template: ${template.javaClass.simpleName}(${template.getFileExtension()})")
                val exportedData = template.format(tasks)
                val success = fileHandler.saveExportedData(
                    exportedData, 
                    tasks, 
                    template.getFileExtension(), 
                    template.getMimeType()
                )
                
                if (success) {
                    Toast.makeText(
                        context, 
                        context.getString(R.string.exported_tasks, tasks.size, template.getFileExtension().uppercase()), 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                return success
            } else {
                Log.e("ExportManager", "No template found for format: '$format'")
                Toast.makeText(
                    context, 
                    context.getString(R.string.export_format_not_supported, format), 
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(
                context, 
                context.getString(R.string.export_failed, e.message), 
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }
    
    /**
     * Finds a template that matches the requested format
     * 
     * @param format The format to find a template for
     * @return The matching template or null if none found
     */
    private fun findTemplateForFormat(format: String): ExportTemplate? {
        val templates = templateManager.getAllTemplates()
        return templates.find { template ->
            val directMatch = template.getFileExtension().equals(format, ignoreCase = true)
            val markdownMatch = format.equals("markdown", ignoreCase = true) && 
                                template.getFileExtension().equals("md", ignoreCase = true)
            val textMatch = format.equals("text", ignoreCase = true) && 
                            template.getFileExtension().equals("txt", ignoreCase = true)
            
            Log.d("ExportManager", "Checking template ${template.javaClass.simpleName}(${template.getFileExtension()}) " +
                    "against format '$format': directMatch=$directMatch, markdownMatch=$markdownMatch, textMatch=$textMatch")
            
            directMatch || markdownMatch || textMatch
        }
    }
    
    /**
     * Exports tasks from a specific category
     * 
     * @param tasks All available tasks
     * @param category The category to filter by
     * @param format The format to export in
     * @return True if export was successful, false otherwise
     */
    fun exportTasksByCategory(tasks: List<Task>, category: TaskCategory, format: String): Boolean {
        val tasksInCategory = tasks.filter { it.category == category }
        if (tasksInCategory.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.no_tasks_to_export), Toast.LENGTH_SHORT).show()
            return false
        }
        
        return exportTasks(tasksInCategory, format)
    }
    
    /**
     * Gets the filter manager instance
     * 
     * @return The TaskFilterManager instance
     */
    fun getFilterManager(): TaskFilterManager = filterManager
    
    /**
     * Gets all available export formats
     * 
     * @return Array of format names
     */
    fun getAvailableFormats(): Array<String> {
        return arrayOf("CSV", "JSON", "HTML", "Markdown", "XML", "Text")
    }
}
