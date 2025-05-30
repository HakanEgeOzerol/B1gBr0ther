package com.b1gbr0ther

class VoiceCommandHandler(private val activity: AudioRecognitionActivity) {

    private val commandMap: Map<String, () -> Unit> = mapOf(
        "start tracking" to { activity.startTracking() },
        "stop tracking" to { activity.stopTracking() },
        "log break" to { activity.startBreak() },
        "end break" to { activity.endBreak() },
        "show dashboard" to { activity.showDashboard() },
        "show export" to { activity.showExportPage() },
        "show manual" to { activity.showManualPage() },
        // "show timesheet" to { activity.showTimesheetPage() } Remove comment once timesheet page is ready
    )

    fun handleCommand(spoken: String): Boolean {
        val command = spoken.trim().lowercase()
        val action = commandMap[command]
        return if (action != null) {
            action.invoke()
            true
        } else {
            false
        }
    }
}
