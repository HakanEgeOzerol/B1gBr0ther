package com.b1gbr0ther.model.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles file operations related to exporting tasks
 * Responsible for saving exported data to files and sharing them
 */
class ExportFileHandler(private val context: Context) {
    
    /**
     * Saves exported data to a file in the Downloads directory
     * 
     * @param data The formatted data to save
     * @param tasks The tasks that were exported
     * @param fileExtension The file extension to use
     * @param mimeType The MIME type of the file
     * @return True if successful, false otherwise
     */
    fun saveExportedData(data: String, tasks: List<Task>, fileExtension: String, mimeType: String): Boolean {
        try {
            val taskCount = tasks.size
            val dateFormatter = SimpleDateFormat("MMM_dd_yyyy", Locale.getDefault())
            val currentDate = dateFormatter.format(Date())
            val filename = "${taskCount}_tasks_$currentDate.$fileExtension"
            
            // Save file to Downloads directory
            val uri = saveFileToDownloads(filename, data, mimeType)
            
            if (uri != null) {
                // Notify user about successful save
                Toast.makeText(
                    context, 
                    context.getString(R.string.file_saved_to_downloads, filename), 
                    Toast.LENGTH_LONG
                ).show()
                
                return true
            } else {
                Toast.makeText(
                    context, 
                    context.getString(R.string.error_saving_to_downloads), 
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(
                context, 
                context.getString(R.string.error_saving_export, e.message), 
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }
    
    // Share functionality removed as requested
    
    /**
     * Saves a file to the Downloads directory
     * 
     * @param filename The name of the file
     * @param data The data to write to the file
     * @param mimeType The MIME type of the file
     * @return The URI of the saved file, or null if saving failed
     */
    private fun saveFileToDownloads(filename: String, data: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (API 29) and above, use MediaStore
            saveFileUsingMediaStore(filename, data, mimeType)
        } else {
            // For older Android versions, save directly to Downloads directory
            saveFileToDownloadsLegacy(filename, data)
        }
    }
    
    /**
     * Saves a file using MediaStore API (Android 10+)
     */
    private fun saveFileUsingMediaStore(filename: String, data: String, mimeType: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(data.toByteArray())
            }
            
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
            
            return uri
        }
        
        return null
    }
    
    /**
     * Saves a file directly to Downloads directory (pre-Android 10)
     */
    private fun saveFileToDownloadsLegacy(filename: String, data: String): Uri? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(data.toByteArray())
            }
            
            return Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}
