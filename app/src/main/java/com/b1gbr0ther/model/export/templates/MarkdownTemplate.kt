package com.b1gbr0ther.models.export.templates

import com.b1gbr0ther.data.database.entities.Task
import java.time.format.DateTimeFormatter

class MarkdownTemplate : ExportTemplate {
    override fun format(tasks: List<Task>): String {
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        val header = "# Task Export Report\n\n"
        val tableHeader = "| Task Name | Start Time | End Time | Preplanned | Completed | Break |"
        val divider = "|-----------|------------|----------|------------|-----------|-------|"
        val rows = tasks.joinToString("\n") { task ->
            val taskName = task.taskName.take(20) + if (task.taskName.length > 20) "..." else ""
            val startTime = task.startTime.format(dateFormatter)
            val endTime = task.endTime.format(dateFormatter)
            val preplanned = if (task.isPreplanned) "✓" else "✗"
            val completed = if (task.isCompleted) "✓" else "✗"
            val isBreak = if (task.isBreak) "✓" else "✗"
            "| $taskName | $startTime | $endTime | $preplanned | $completed | $isBreak |"
        }
        val summary = "\n\n## Summary\n" +
                "- **Total Tasks:** ${tasks.size}\n" +
                "- **Completed:** ${tasks.count { it.isCompleted }}\n" +
                "- **Preplanned:** ${tasks.count { it.isPreplanned }}\n" +
                "- **Breaks:** ${tasks.count { it.isBreak }}\n"
        return "$header$tableHeader\n$divider\n$rows$summary"
    }
    override fun getFileExtension() = "md"
    override fun getMimeType() = "text/markdown"
}
