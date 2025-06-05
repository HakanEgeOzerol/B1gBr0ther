package com.b1gbr0ther

class VoiceCommandHandler(private val activity: DashboardActivity) {

    private val commandAliases = mapOf(
        "stop tracking" to listOf(
            "stop tracking",
            "end tracking",
            "finish tracking",
            "stop work",
            "end work",
            "finish work",
            "stop working",
            "end working"
        ),
        "start break" to listOf(
            "log break",
            "begin break",
            "take break",
            "take a break",
            "start a break",
            "begin a break",
            "going on break",
        ),
        "stop break" to listOf(
            "stop break",
            "end break",
            "finish break",
            "stop the break",
            "end the break",
            "finish the break",
            "break ended",
            "break finished",
            "break is over",
            "breaks over",
            "break over",
            "done with break",
            "finished break",
            "handbreak",
        ),
        "show export" to listOf(
            "show export",
            "export data",
            "open export",
            "show exports",
            "export page",
            "open exports"
        ),
        "show manual" to listOf(
            "show manual",
            "open manual",
            "show help",
            "open help",
            "help page",
            "manual page"
        ),
        "show timesheet" to listOf(
            "show timesheet",
            "open timesheet",
            "view timesheet",
            "show time sheet",
            "open time sheet",
            "view time sheet",
            "show my hours",
            "view my hours",
            "check timesheet",
            "check my hours"
        )
    )

    private val commandMap = mapOf(
        "stop tracking" to { activity.stopTracking() },
        "start break" to { activity.startBreak() },
        "stop break" to { activity.endBreak() },
        "show export" to { activity.showExportPage() },
        "show manual" to { activity.showManualPage() },
//        "show timesheet" to { activity.showTimesheetPage() }
    )

    fun handleCommand(spoken: String): Boolean {
        val normalizedInput = spoken.trim().lowercase()

        if (normalizedInput.startsWith("start tracking ") || normalizedInput.startsWith("begin tracking ")) {
            val taskName = normalizedInput.substringAfter(" tracking ").trim()
            if (taskName.isNotEmpty()) {
                activity.startTrackingWithTask(taskName)
                return true
            }
            return false
        }

        commandMap[normalizedInput]?.let {
            it.invoke()
            return true
        }

        for ((mainCommand, aliases) in commandAliases) {
            if (aliases.any { alias -> normalizedInput == alias }) {
                commandMap[mainCommand]?.invoke()
                return true
            }
        }

        for ((mainCommand, aliases) in commandAliases) {
            if (aliases.any { alias -> 
                    isSimilar(normalizedInput, alias) || 
                    normalizedInput.contains(alias) ||
                    alias.contains(normalizedInput) ||
                    isSimilarWords(normalizedInput, alias)
                }) {
                commandMap[mainCommand]?.invoke()
                return true
            }
        }
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
