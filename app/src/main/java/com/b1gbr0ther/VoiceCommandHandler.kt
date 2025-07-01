package com.b1gbr0ther

class VoiceCommandHandler(private val activity: DashboardActivity) {

    private val commandAliases = VoiceCommandAliases.commandAliases

    private val commandMap = mapOf(
        "stop tracking" to { activity.stopTracking() },
        "start break" to { activity.startBreak() },
        "stop break" to { activity.endBreak() },
        "show export" to { activity.showExportPage() },
        "show manual" to { activity.showManualPage() },
        "show timesheet" to { activity.showTimesheetPage() },
        "show statistics" to { activity.showStatisticsPage() },
        "create task" to { activity.createTaskByVoice() },
        "export csv" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export csv command")
            activity.exportCSV() 
        },
        "export json" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export json command")
            activity.exportJSON() 
        },
        "export html" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export html command")
            activity.exportHTML() 
        },
        "export markdown" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export markdown command")
            activity.exportMarkdown() 
        },
        "export xml" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export xml command")
            activity.exportXML() 
        },
        "export text" to { 
            android.util.Log.d("VoiceCommandHandler", "Executing export text command")
            activity.exportText() 
        }
    )

    private var lastSpokenCommand: String? = null

    fun handleCommand(spoken: String): Boolean {
        val normalizedInput = spoken.trim().lowercase()
        android.util.Log.d("VoiceCommandHandler", "handleCommand called with spoken: '$spoken', normalized: '$normalizedInput'")
        
        if (commandAliases["cancel command"]?.any { alias -> 
            normalizedInput == alias || 
            normalizedInput.contains(alias) 
        } == true) {
            lastSpokenCommand = null
            return true
        }

        lastSpokenCommand = normalizedInput

        if (normalizedInput.startsWith("import ") || 
            normalizedInput.startsWith("load ") || 
            normalizedInput.startsWith("open ")) {
            val fileName = when {
                normalizedInput.startsWith("import ") -> normalizedInput.substringAfter("import ")
                normalizedInput.startsWith("load ") -> normalizedInput.substringAfter("load ")
                normalizedInput.startsWith("open ") -> normalizedInput.substringAfter("open ")
                else -> ""
            }.trim()
            
            if (fileName.isNotEmpty()) {
                activity.importFile(fileName)
                return true
            }
        }

        if (normalizedInput.startsWith("create task ") || 
            normalizedInput.startsWith("add task ") || 
            normalizedInput.startsWith("new task ")) {
            val taskName = when {
                normalizedInput.startsWith("create task ") -> normalizedInput.substringAfter("create task ")
                normalizedInput.startsWith("add task ") -> normalizedInput.substringAfter("add task ")
                normalizedInput.startsWith("new task ") -> normalizedInput.substringAfter("new task ")
                else -> ""
            }.trim()
            
            if (taskName.isNotEmpty()) {
                activity.createTaskByVoice(taskName)
                return true
            }
        }

        // Special handling for export commands - check for format keywords first
        if (normalizedInput.contains("export") && (normalizedInput.contains("task") || normalizedInput.contains("data"))) {
            val formatKeywords = mapOf(
                "csv" to "export csv",
                "json" to "export json", 
                "jason" to "export json",  // Common mispronunciation
                "html" to "export html",
                "markdown" to "export markdown",
                "md" to "export markdown",  // Short form
                "xml" to "export xml",
                "text" to "export text",
                "txt" to "export text",    // File extension form
                "plain text" to "export text"
            )
            
            for ((keyword, command) in formatKeywords) {
                if (normalizedInput.contains(keyword)) {
                    android.util.Log.d("VoiceCommandHandler", "Format keyword match: '$normalizedInput' contains '$keyword' -> '$command'")
                    commandMap[command]?.invoke()
                    return true
                }
            }
        }

        if (normalizedInput.startsWith("delete task ") || normalizedInput.startsWith("remove task ")) {
            val taskName = normalizedInput.substringAfter(" task ").trim()
            if (taskName.isNotEmpty()) {
                activity.deleteTaskByName(taskName)
                return true
            }
            return false
        }

        if (normalizedInput.startsWith("start tracking ") || normalizedInput.startsWith("begin tracking ")) {
            val taskName = normalizedInput.substringAfter(" tracking ").trim()
            if (taskName.isNotEmpty()) {
                activity.startTrackingWithTask(taskName)
                return true
            }
            return false
        }

        // Enhanced flexible command parsing for various tracking patterns
        if (normalizedInput.contains("track") && (normalizedInput.contains("task") || normalizedInput.contains("project"))) {
            var taskName = ""
            if (normalizedInput.contains(" task ")) {
                taskName = normalizedInput.substringAfter(" task ").trim()
            } else if (normalizedInput.contains(" project ")) {
                taskName = normalizedInput.substringAfter(" project ").trim()
            } else if (normalizedInput.contains("track ")) {
                taskName = normalizedInput.substringAfter("track ").trim()
                taskName = taskName.replace("the ", "").replace("my ", "").replace("a ", "").trim()
            }
            
            if (taskName.isNotEmpty()) {
                activity.startTrackingWithTask(taskName)
                return true
            }
        }

        commandMap[normalizedInput]?.let {
            android.util.Log.d("VoiceCommandHandler", "Direct command match found: '$normalizedInput'")
            it.invoke()
            return true
        }

        for ((mainCommand, aliases) in commandAliases) {
            if (aliases.any { alias -> normalizedInput == alias }) {
                android.util.Log.d("VoiceCommandHandler", "Exact alias match found: '$normalizedInput' -> '$mainCommand'")
                commandMap[mainCommand]?.invoke()
                return true
            }
        }

        for ((mainCommand, aliases) in commandAliases) {
            for (alias in aliases) {
                val isSim = isSimilar(normalizedInput, alias)
                val isSimWords = isSimilarWords(normalizedInput, alias)
                if (isSim || isSimWords) {
                    android.util.Log.d("VoiceCommandHandler", "Similar match: '$normalizedInput' matched alias '$alias' for command '$mainCommand' (similar=$isSim, similarWords=$isSimWords)")
                    commandMap[mainCommand]?.invoke()
                    return true
                }
            }
        }
        android.util.Log.d("VoiceCommandHandler", "No command match found for: '$normalizedInput'")
        return false
    }

    private fun isSimilar(str1: String, str2: String): Boolean {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) dp[i][0] = i
        for (j in 0..str2.length) dp[0][j] = j
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + if (str1[i-1].equals(str2[j-1], ignoreCase = true)) 0 else 1
                )

                if (i > 1 && j > 1) {
                    if (str1[i-1] == str2[j-2] && str1[i-2] == str2[j-1]) {
                        dp[i][j] = minOf(dp[i][j], dp[i-2][j-2] + 1)
                    }
                }
            }
        }
        
        val maxLength = maxOf(str1.length, str2.length)
        val threshold = when {
            maxLength <= 5 -> 2
            maxLength <= 10 -> 3
            else -> maxOf(3, maxLength / 3)
        }
        return dp[str1.length][str2.length] <= threshold
    }

    private fun isSimilarWords(str1: String, str2: String): Boolean {
        val words1 = str1.split(" ")
        val words2 = str2.split(" ")

        var matchCount = 0
        for (word1 in words1) {
            if (words2.any { word2 -> isSimilar(word1, word2) }) {
                matchCount++
            }
        }
        return matchCount >= (words1.size * 0.6)
    }
}
