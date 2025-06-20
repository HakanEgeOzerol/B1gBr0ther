package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class PlainTextTemplate : ExportTemplate {
    override fun format(tasks: List<Task>): String {
        return tasks.joinToString("\n\n") { task ->
            """
            ID: ${task.id}
            Task: ${task.taskName}
            Start: ${task.startTime}
            End: ${task.endTime}
            Creation Method: ${task.creationMethod}
            Timing Status: ${task.timingStatus}
            Preplanned: ${task.isPreplanned}
            Completed: ${task.isCompleted}
            Break: ${task.isBreak}
            """.trimIndent()
        }
    }

    override fun getFileExtension() = "txt"
    override fun getMimeType() = "text/plain"
}
