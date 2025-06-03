package com.b1gbr0ther.models.export

import com.b1gbr0ther.models.export.templates.*

class ExportTemplateManager {
    val templates: List<ExportTemplate> = listOf(
        CSVTemplate(),
        JSONTemplate(),
        HTMLTemplate(),
        MarkdownTemplate(),
        XMLTemplate(),
        PlainTextTemplate(),
    )

    fun getTemplateByExtension(ext: String): ExportTemplate? {
        return templates.find { it.getFileExtension() == ext }
    }

    fun getAllTemplates(): List<ExportTemplate> = templates
}
