package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class XMLTemplate : ExportTemplate {
    override fun format(tasks: List<Task>): String {
        val entries = tasks.joinToString("\n") { task ->
            """
            <task>
                <id>${task.id}</id>
                <name>${task.taskName}</name>
                <start>${task.startTime}</start>
                <end>${task.endTime}</end>
                <creationMethod>${task.creationMethod}</creationMethod>
                <timingStatus>${task.timingStatus}</timingStatus>
                <preplanned>${task.isPreplanned}</preplanned>
                <completed>${task.isCompleted}</completed>
                <break>${task.isBreak}</break>
            </task>
            """.trimIndent()
        }
        return "<tasks>\n$entries\n</tasks>"
    }

    override fun getFileExtension() = "xml"
    override fun getMimeType() = "application/xml"
}
