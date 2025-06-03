package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task

class HTMLTemplate : ExportTemplate {
    override fun format(tasks: List<Task>): String {
        val rows = tasks.joinToString("\n") { task ->
            """
            <tr>
                <td>${task.taskName}</td>
                <td>${task.startTime}</td>
                <td>${task.endTime}</td>
                <td>${task.isPreplanned}</td>
                <td>${task.isCompleted}</td>
                <td>${task.isBreak}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <html>
        <head><title>Task Export</title></head>
        <body>
            <h2>Exported Tasks</h2>
            <table border="1" cellpadding="5" cellspacing="0">
                <tr>
                    <th>Name</th><th>Start</th><th>End</th><th>Preplanned</th><th>Completed</th><th>Break</th>
                </tr>
                $rows
            </table>
        </body>
        </html>
        """.trimIndent()
    }

    override fun getFileExtension() = "html"
    override fun getMimeType() = "text/html"
}
