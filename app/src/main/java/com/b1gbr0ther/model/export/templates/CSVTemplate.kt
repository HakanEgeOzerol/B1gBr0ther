package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class CSVTemplate : ExportTemplate {

    override fun format(tasks: List<Task>): String {
        val header = "Task Name,Start Time,End Time,Preplanned,Completed,Break"

        val rows = tasks.joinToString("\n") { task ->
            listOf(
                task.taskName.replace(",", " "),
                task.startTime.toString(),
                task.endTime.toString(),
                task.isPreplanned.toString(),
                task.isCompleted.toString(),
                task.isBreak.toString()
            ).joinToString(",")
        }

        return "$header\n$rows"
    }

    override fun getFileExtension(): String = "csv"

    override fun getMimeType(): String = "text/csv"
}
