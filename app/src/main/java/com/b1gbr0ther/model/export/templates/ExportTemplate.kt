package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

interface ExportTemplate {
    fun format(tasks: List<Task>): String
    fun getFileExtension(): String
    fun getMimeType(): String
}
