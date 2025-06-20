package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class CSVTemplate : ExportTemplate {

    override fun format(tasks: List<Task>): String {
        val header = "ID,Task Name,Start Time,End Time,Creation Method,Timing Status,Preplanned,Completed,Break"

        val rows = tasks.joinToString("\n") { task ->
            listOf(
                task.id.toString(),
                task.taskName.replace(",", " "),
                task.startTime.toString(),
                task.endTime.toString(),
                task.creationMethod.toString(),
                task.timingStatus.toString(),
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
