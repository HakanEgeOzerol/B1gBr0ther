package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class JSONTemplate : ExportTemplate {
    override fun format(tasks: List<Task>): String {
        return tasks.joinToString(
            prefix = "[", postfix = "]", separator = ",\n"
        ) { task ->
            """
            {
                "id": ${task.id},
                "taskName": "${task.taskName.replace("\"", "\\\"")}",
                "startTime": "${task.startTime}",
                "endTime": "${task.endTime}",
                "isPreplanned": ${task.isPreplanned},
                "isCompleted": ${task.isCompleted},
                "isBreak": ${task.isBreak}
            }
            """.trimIndent()
        }
    }

    override fun getFileExtension(): String = "json"

    override fun getMimeType(): String = "application/json"
}
