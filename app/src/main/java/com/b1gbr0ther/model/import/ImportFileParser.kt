package com.b1gbr0ther.model.import

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.b1gbr0ther.CreationMethod
import com.b1gbr0ther.TaskCategory
import com.b1gbr0ther.TimingStatus
import com.b1gbr0ther.data.database.entities.Task
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Exception thrown when an unsupported file type is attempted to be imported
 */
class UnsupportedFileTypeException(message: String) : Exception(message)

/**
 * Handles parsing of imported task files in various formats
 */
class ImportFileParser(private val context: Context) {
    
    companion object {
        private const val TAG = "ImportFileParser"
        
        // File extensions supported for import
        const val CSV_EXTENSION = "csv"
        const val JSON_EXTENSION = "json"
        const val XML_EXTENSION = "xml"
        const val MARKDOWN_EXTENSION = "md"
        const val TEXT_EXTENSION = "txt"
        
        // List of supported file extensions
        val SUPPORTED_EXTENSIONS = listOf(
            CSV_EXTENSION,
            JSON_EXTENSION,
            XML_EXTENSION,
            MARKDOWN_EXTENSION,
            TEXT_EXTENSION
        )
        
        // Date format patterns for parsing
        private val DATE_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "dd.MM.yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd.MM.yyyy",
            "MM/dd/yyyy"
        )
    }
    
    /**
     * Parse a file from the given URI and return a list of tasks
     * 
     * @param uri The URI of the file to parse
     * @return List of parsed tasks or empty list if parsing failed
     */
    fun parseFile(uri: Uri): List<Task> {
        // Log the full URI for debugging
        Log.d(TAG, "Parsing file from URI: $uri")
        
        // Get file extension and MIME type
        val fileExtension = getFileExtension(uri)
        Log.d(TAG, "Detected file extension: '$fileExtension'")
        
        // Check if this is an HTML file or other unsupported type
        if (fileExtension.lowercase() == "html" || fileExtension.lowercase() == "htm") {
            Log.d(TAG, "HTML file detected - not supported")
            throw UnsupportedFileTypeException("File type not supported yet.")
        }
        
        // Check if the file extension is one of our supported types
        val isSupportedExtension = fileExtension.lowercase() == CSV_EXTENSION || 
                                  fileExtension.lowercase() == JSON_EXTENSION || 
                                  fileExtension.lowercase() == XML_EXTENSION || 
                                  fileExtension.lowercase() == MARKDOWN_EXTENSION
        
        if (!isSupportedExtension) {
            Log.d(TAG, "Unsupported file type detected: $fileExtension")
            throw UnsupportedFileTypeException("File type not supported yet.")
        }
        
        return try {
            // Parse the file based on its extension
            val result = when (fileExtension.lowercase()) {
                CSV_EXTENSION -> {
                    Log.d(TAG, "Parsing as CSV file")
                    parseCSV(uri)
                }
                JSON_EXTENSION -> {
                    Log.d(TAG, "Parsing as JSON file")
                    parseJSON(uri)
                }
                XML_EXTENSION -> {
                    Log.d(TAG, "Parsing as XML file")
                    parseXML(uri)
                }
                MARKDOWN_EXTENSION -> {
                    Log.d(TAG, "Parsing as Markdown file")
                    parseMarkdown(uri)
                }
                else -> {
                    // This should never happen due to the check above
                    Log.d(TAG, "Unsupported file type: $fileExtension")
                    throw UnsupportedFileTypeException("File type not supported yet.")
                }
            }
            
            Log.d(TAG, "Parsing completed, found ${result.size} tasks")
            result
        } catch (e: UnsupportedFileTypeException) {
            // Re-throw UnsupportedFileTypeException to be caught by the caller
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing file: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Detect the file format from content and parse accordingly
     */
    private fun detectAndParseFormat(uri: Uri): List<Task> {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                val trimmedContent = content.trim()
                
                // Try to detect JSON format (look for { or [ at the beginning)
                if ((trimmedContent.startsWith("[") && trimmedContent.endsWith("]")) || 
                    (trimmedContent.startsWith("{") && trimmedContent.endsWith("}"))) {
                    Log.d(TAG, "Content appears to be JSON, attempting to parse")
                    return parseJSONContent(content)
                }
                
                // Try to detect XML format (look for < at the beginning)
                if (trimmedContent.startsWith("<") && trimmedContent.contains("</")) {
                    Log.d(TAG, "Content appears to be XML, attempting to parse")
                    return parseXML(uri)
                }
                
                // Try to detect CSV format (look for commas and newlines)
                if (trimmedContent.contains(",") && trimmedContent.contains("\n")) {
                    Log.d(TAG, "Content appears to be CSV, attempting to parse")
                    return parseCSV(uri)
                }
                
                // HTML format is no longer supported
                
                // Try to detect Markdown format
                if (trimmedContent.contains("#") || trimmedContent.contains("|---|")) {
                    Log.d(TAG, "Content appears to be Markdown, attempting to parse")
                    return parseMarkdown(uri)
                }
                
                // Default to plain text
                Log.d(TAG, "Could not detect format, defaulting to plain text")
                return parseText(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting file format: ${e.message}", e)
        }
        
        return emptyList()
    }
    
    /**
     * Parse JSON content directly from a string
     */
    private fun parseJSONContent(content: String): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            // Log the first 200 chars of JSON for debugging
            val previewLength = minOf(content.length, 200)
            Log.d(TAG, "JSON content preview: ${content.substring(0, previewLength)}...")
            
            // First try parsing as a JSON array
            if (content.trim().startsWith("[")) {
                val jsonArray = JSONArray(content)
                Log.d(TAG, "Found JSON array with ${jsonArray.length()} items")
                
                for (i in 0 until jsonArray.length()) {
                    try {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val task = createTaskFromJSON(jsonObject)
                        tasks.add(task)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing JSON array item at index $i: ${e.message}", e)
                    }
                }
            } 
            // Then try parsing as a single JSON object
            else if (content.trim().startsWith("{")) {
                val jsonObject = JSONObject(content)
                Log.d(TAG, "Found single JSON object")
                
                val task = createTaskFromJSON(jsonObject)
                tasks.add(task)
            }
            
            Log.d(TAG, "Successfully parsed ${tasks.size} tasks from JSON content")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON content: ${e.message}", e)
        }
        
        return tasks
    }
    
    /**
     * Parse a JSON file
     */
    private fun parseJSON(uri: Uri): List<Task> {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                return parseJSONContent(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON file: ${e.message}", e)
        }
        
        return emptyList()
    }
    
    /**
     * Create a Task object from a JSON object
     */
    private fun createTaskFromJSON(json: JSONObject): Task {
        // Log the JSON keys for debugging
        val keys = json.keys().asSequence().toList()
        Log.d(TAG, "JSON keys: $keys")
        
        // Handle different possible field names
        val id = when {
            json.has("id") -> json.optLong("id", 0)
            json.has("ID") -> json.optLong("ID", 0)
            else -> 0
        }
        
        val taskName = when {
            json.has("taskName") -> json.optString("taskName", "")
            json.has("name") -> json.optString("name", "")
            json.has("task_name") -> json.optString("task_name", "")
            json.has("n") -> json.optString("n", "")
            else -> ""
        }
        
        // Get start time with fallbacks
        val startTimeStr = when {
            json.has("startTime") -> json.optString("startTime")
            json.has("start_time") -> json.optString("start_time")
            json.has("start") -> json.optString("start")
            else -> ""
        }
        
        // Get end time with fallbacks
        val endTimeStr = when {
            json.has("endTime") -> json.optString("endTime")
            json.has("end_time") -> json.optString("end_time")
            json.has("end") -> json.optString("end")
            else -> ""
        }
        
        // Get creation method with fallbacks
        val creationMethodStr = when {
            json.has("creationMethod") -> json.optString("creationMethod")
            json.has("creation_method") -> json.optString("creation_method")
            json.has("method") -> json.optString("method")
            else -> ""
        }
        
        // Get timing status with fallbacks
        val timingStatusStr = when {
            json.has("timingStatus") -> json.optString("timingStatus")
            json.has("timing_status") -> json.optString("timing_status")
            json.has("status") -> json.optString("status")
            else -> ""
        }
        
        // Get boolean flags with fallbacks
        val isPreplanned = when {
            json.has("isPreplanned") -> json.optBoolean("isPreplanned", false)
            json.has("is_preplanned") -> json.optBoolean("is_preplanned", false)
            json.has("preplanned") -> json.optBoolean("preplanned", false)
            else -> false
        }
        
        val isCompleted = when {
            json.has("isCompleted") -> json.optBoolean("isCompleted", false)
            json.has("is_completed") -> json.optBoolean("is_completed", false)
            json.has("completed") -> json.optBoolean("completed", false)
            else -> false
        }
        
        val isBreak = when {
            json.has("isBreak") -> json.optBoolean("isBreak", false)
            json.has("is_break") -> json.optBoolean("is_break", false)
            json.has("break") -> json.optBoolean("break", false)
            else -> false
        }
        
        // Get category with fallback
        val categoryStr = when {
            json.has("category") -> json.optString("category")
            else -> ""
        }
        val category = if (categoryStr.isNotEmpty()) {
            TaskCategory.fromDisplayName(categoryStr) ?: TaskCategory.getDefault()
        } else {
            TaskCategory.getDefault()
        }
        
        try {
            return Task(
                id = id,
                taskName = taskName,
                startTime = parseDateTime(startTimeStr),
                endTime = parseDateTime(endTimeStr),
                creationMethod = parseCreationMethod(creationMethodStr),
                timingStatus = parseTimingStatus(timingStatusStr),
                isPreplanned = isPreplanned,
                isCompleted = isCompleted,
                isBreak = isBreak,
                category = category
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Task from JSON: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Parse an XML file
     */
    private fun parseXML(uri: Uri): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(inputStream, null)
                
                var eventType = parser.eventType
                var currentTask: Task? = null
                var currentTag = ""
                
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name
                            if (currentTag == "task") {
                                currentTask = Task()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val text = parser.text
                            currentTask?.let { task ->
                                when (currentTag) {
                                    "id" -> task.id = text.toLongOrNull() ?: 0
                                    "taskName", "name", "task_name", "n" -> task.taskName = text
                                    "startTime", "start_time", "start" -> task.startTime = parseDateTime(text)
                                    "endTime", "end_time", "end" -> task.endTime = parseDateTime(text)
                                    "creationMethod", "creation_method", "method" -> task.creationMethod = parseCreationMethod(text)
                                    "timingStatus", "timing_status", "status" -> task.timingStatus = parseTimingStatus(text)
                                    "isPreplanned", "is_preplanned", "preplanned" -> task.isPreplanned = parseBoolean(text)
                                    "isCompleted", "is_completed", "completed" -> task.isCompleted = parseBoolean(text)
                                    "isBreak", "is_break", "break" -> task.isBreak = parseBoolean(text)
                                    "category" -> task.category = TaskCategory.fromDisplayName(text) ?: TaskCategory.getDefault()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "task" && currentTask != null) {
                                tasks.add(currentTask!!)
                                currentTask = null
                            }
                            currentTag = ""
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML: ${e.message}", e)
        }
        
        return tasks
    }
    
    /**
     * Parse a CSV file
     */
    private fun parseCSV(uri: Uri): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                // Read header line
                val headerLine = reader.readLine()
                if (headerLine == null) {
                    Log.e(TAG, "CSV file is empty")
                    return emptyList()
                }
                
                // Parse header to determine column indices
                val headers = headerLine.split(",").map { it.trim().lowercase() }
                val idIndex = headers.indexOfFirst { it.contains("id") }
                val nameIndex = headers.indexOfFirst { it.contains("name") || it == "n" }
                val startIndex = headers.indexOfFirst { it.contains("start") }
                val endIndex = headers.indexOfFirst { it.contains("end") }
                val methodIndex = headers.indexOfFirst { it.contains("method") }
                val statusIndex = headers.indexOfFirst { it.contains("status") }
                val preplannedIndex = headers.indexOfFirst { it.contains("preplanned") }
                val completedIndex = headers.indexOfFirst { it.contains("completed") }
                val breakIndex = headers.indexOfFirst { it.contains("break") }
                val categoryIndex = headers.indexOfFirst { it.contains("category") }
                
                // Process data rows
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    try {
                        val values = line!!.split(",").map { it.trim() }
                        
                        // Skip rows with too few columns
                        if (values.size < 3) continue
                        
                        val task = Task(
                            id = if (idIndex >= 0 && idIndex < values.size) values[idIndex].toLongOrNull() ?: 0 else 0,
                            taskName = if (nameIndex >= 0 && nameIndex < values.size) values[nameIndex] else "",
                            startTime = if (startIndex >= 0 && startIndex < values.size) parseDateTime(values[startIndex]) else LocalDateTime.now(),
                            endTime = if (endIndex >= 0 && endIndex < values.size) parseDateTime(values[endIndex]) else LocalDateTime.now(),
                            creationMethod = if (methodIndex >= 0 && methodIndex < values.size) parseCreationMethod(values[methodIndex]) else CreationMethod.Gesture, // Default to Gesture instead of Import
                            timingStatus = if (statusIndex >= 0 && statusIndex < values.size) parseTimingStatus(values[statusIndex]) else TimingStatus.ON_TIME,
                            isPreplanned = if (preplannedIndex >= 0 && preplannedIndex < values.size) parseBoolean(values[preplannedIndex]) else false,
                            isCompleted = if (completedIndex >= 0 && completedIndex < values.size) parseBoolean(values[completedIndex]) else false,
                            isBreak = if (breakIndex >= 0 && breakIndex < values.size) parseBoolean(values[breakIndex]) else false,
                            category = if (categoryIndex >= 0 && categoryIndex < values.size) 
                                TaskCategory.fromDisplayName(values[categoryIndex]) ?: TaskCategory.getDefault()
                            else 
                                TaskCategory.getDefault()
                        )
                        tasks.add(task)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing CSV line: $line", e)
                        // Continue with next line
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV: ${e.message}", e)
        }
        
        return tasks
    }
    
    /**
     * Parse a Markdown file
     */
    private fun parseMarkdown(uri: Uri): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                // Skip header lines until we find the table header
                var line: String?
                var foundHeader = false
                
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("| ID | Task Name |") == true) {
                        foundHeader = true
                        reader.readLine() // Skip the divider line with dashes
                        break
                    }
                }
                
                if (!foundHeader) {
                    Log.e(TAG, "Invalid Markdown format: missing table header")
                    return emptyList()
                }
                
                // Process data rows
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("|") != true || line?.endsWith("|") != true) {
                        // End of table
                        break
                    }
                    
                    try {
                        // Remove leading/trailing | and split by |
                        val rowContent = line!!.substring(1, line!!.length - 1)
                        val cells = rowContent.split("|").map { it.trim() }
                        
                        if (cells.size >= 9) {
                            val task = Task(
                                id = cells[0].toLongOrNull() ?: 0,
                                taskName = cells[1].replace("...", ""), // Remove ellipsis if present
                                startTime = parseDateTime(cells[2]),
                                endTime = parseDateTime(cells[3]),
                                creationMethod = parseCreationMethod(cells[4]),
                                timingStatus = parseTimingStatus(cells[5]),
                                isPreplanned = cells[6] == "✓",
                                isCompleted = cells[7] == "✓",
                                isBreak = cells[8] == "✓",
                                category = TaskCategory.getDefault()
                            )
                            tasks.add(task)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Markdown line: $line", e)
                        // Continue with next line
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Markdown: ${e.message}", e)
        }
        
        return tasks
    }
    
    /**
     * Parse a plain text file
     */
    private fun parseText(uri: Uri): List<Task> {
        val tasks = mutableListOf<Task>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                
                // Split by double newline which separates task entries
                val taskBlocks = content.split("\n\n")
                
                for (block in taskBlocks) {
                    if (block.isBlank()) continue
                    
                    try {
                        val lines = block.lines()
                        if (lines.size < 5) continue // Need at least ID, name, start, end
                        
                        val task = Task()
                        
                        for (line in lines) {
                            val parts = line.split(":", limit = 2)
                            if (parts.size != 2) continue
                            
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            
                            when (key.lowercase()) {
                                "id" -> task.id = value.toLongOrNull() ?: 0
                                "name", "task name", "task_name" -> task.taskName = value
                                "start", "start time", "start_time" -> task.startTime = parseDateTime(value)
                                "end", "end time", "end_time" -> task.endTime = parseDateTime(value)
                                "method", "creation method", "creation_method" -> task.creationMethod = parseCreationMethod(value)
                                "status", "timing status", "timing_status" -> task.timingStatus = parseTimingStatus(value)
                                "preplanned", "is preplanned", "is_preplanned" -> task.isPreplanned = parseBoolean(value)
                                "completed", "is completed", "is_completed" -> task.isCompleted = parseBoolean(value)
                                "break", "is break", "is_break" -> task.isBreak = parseBoolean(value)
                                "category" -> task.category = TaskCategory.fromDisplayName(value) ?: TaskCategory.getDefault()
                            }
                        }
                        
                        if (task.taskName.isNotEmpty()) {
                            tasks.add(task)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing text block: ${e.message}", e)
                        // Continue with next block
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing text file: ${e.message}", e)
        }
        
        return tasks
    }
    
    /**
     * Get the file extension from a URI
     */
    private fun getFileExtension(uri: Uri): String {
        // Try to get the file extension from the URI
        try {
            // First try to get the display name from the content resolver
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        val displayName = it.getString(displayNameIndex)
                        Log.d(TAG, "Display name from content resolver: $displayName")
                        
                        val dotIndex = displayName.lastIndexOf('.')
                        if (dotIndex > 0 && dotIndex < displayName.length - 1) {
                            val extension = displayName.substring(dotIndex + 1)
                            Log.d(TAG, "Found extension from display name: $extension")
                            return extension
                        }
                    }
                }
            }
            
            // If we couldn't get it from the content resolver, try from the last path segment
            val fileName = uri.lastPathSegment ?: ""
            Log.d(TAG, "Getting file extension from last path segment: $fileName")
            
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex > 0 && dotIndex < fileName.length - 1) {
                val extension = fileName.substring(dotIndex + 1)
                Log.d(TAG, "Found extension from path: $extension")
                return extension
            }
            
            // If we still don't have an extension, try to get it from the path
            val path = uri.path
            if (path != null) {
                val pathDotIndex = path.lastIndexOf('.')
                if (pathDotIndex > 0 && pathDotIndex < path.length - 1) {
                    val extension = path.substring(pathDotIndex + 1)
                    Log.d(TAG, "Found extension from URI path: $extension")
                    return extension
                }
            }
            
            Log.d(TAG, "No extension found in any method")
            return ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file extension: ${e.message}", e)
            return ""
        }
    }
    
    /**
     * Parse a datetime string into a LocalDateTime object
     */
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        if (dateTimeStr.isBlank()) {
            return LocalDateTime.now()
        }
        
        // Clean up the date string
        val cleanDateStr = dateTimeStr.trim()
        
        // Try each date format pattern
        for (pattern in DATE_FORMATS) {
            try {
                val format = DateTimeFormatter.ofPattern(pattern)
                val dateTime = LocalDateTime.parse(cleanDateStr, format)
                Log.d(TAG, "Successfully parsed datetime with format: ${format.toString()}")
                return dateTime
            } catch (e: DateTimeParseException) {
                // Try next pattern
            }
        }
        
        // If all patterns fail, return current time
        Log.w(TAG, "Could not parse datetime: $cleanDateStr")
        return LocalDateTime.now()
    }
    
    /**
     * Parse a string into a CreationMethod enum
     */
    private fun parseCreationMethod(method: String): CreationMethod {
        return when (method.trim().lowercase()) {
            "gesture" -> CreationMethod.Gesture
            "voice" -> CreationMethod.Voice
            else -> CreationMethod.Gesture // Default to Gesture
        }
    }
    
    /**
     * Parse a string into a TimingStatus enum
     */
    private fun parseTimingStatus(status: String): TimingStatus {
        return when (status.trim().lowercase()) {
            "on_time", "on time", "ontime" -> TimingStatus.ON_TIME
            "late" -> TimingStatus.LATE
            "early" -> TimingStatus.EARLY
            else -> TimingStatus.ON_TIME // Default
        }
    }
    
    /**
     * Parse a string into a boolean value
     */
    private fun parseBoolean(value: String): Boolean {
        return when (value.trim().lowercase()) {
            "true", "yes", "1", "y", "✓" -> true
            else -> false
        }
    }
}
