package com.b1gbr0ther

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionTracker(context: Context) : TimeTrackerInterface {
    private var startTime: Long = 0L
    private var endTime: Long = 0L
    private var currentBreakStart: Long? = null
    private val breaks = mutableListOf<Pair<Long, Long>>()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("time_tracker", Context.MODE_PRIVATE)

    init {
        if (prefs.getBoolean("is_tracking", false)) {
            startTime = prefs.getLong("start_time", 0L)

            if (prefs.getBoolean("is_on_break", false)) {
                currentBreakStart = prefs.getLong("current_break", 0L)
            }

            val breaksStr = prefs.getString("breaks", "")
            if (!breaksStr.isNullOrEmpty()) {
                breaksStr.split(";").forEach {
                    val parts = it.split(",")
                    if (parts.size == 2) {
                        breaks.add(Pair(parts[0].toLong(), parts[1].toLong()))
                    }
                }
            }
        }
    }

    override fun startTracking(): Boolean {
        startTime = System.currentTimeMillis()
        endTime = 0L
        breaks.clear()
        currentBreakStart = null
        saveState()
        return true
    }

    override fun startBreak(): Boolean {
        if (!isTracking() || currentBreakStart != null) return false
        currentBreakStart = System.currentTimeMillis()
        saveState()
        return true
    }

    override fun endBreak(): Boolean {
        if (!isTracking() || currentBreakStart == null) return false
        val breakEnd = System.currentTimeMillis()
        breaks.add(Pair(currentBreakStart!!, breakEnd))
        currentBreakStart = null
        saveState()
        return true
    }

    override fun stopTracking(): TimeTracker.TrackingSummary? {
        if (!isTracking()) return null
        endTime = System.currentTimeMillis()

        currentBreakStart?.let {
            breaks.add(Pair(it, endTime))
            currentBreakStart = null
        }

        val totalTime = endTime - startTime
        val totalBreakTime = breaks.sumOf { it.second - it.first }
        val effectiveTime = totalTime - totalBreakTime

        clearState()

        return TimeTracker.TrackingSummary(
            totalTimeMillis = totalTime,
            breakTimeMillis = totalBreakTime,
            effectiveTimeMillis = effectiveTime,
            breakCount = breaks.size
        )
    }

    private fun saveState() {
        prefs.edit().apply {
            putBoolean("is_tracking", isTracking())
            putLong("start_time", startTime)
            putBoolean("is_on_break", isOnBreak())

            if (isOnBreak()) {
                putLong("current_break", currentBreakStart!!)
            } else {
                remove("current_break")
            }

            if (breaks.isNotEmpty()) {
                val breaksStr = breaks.joinToString(";") { "${it.first},${it.second}" }
                putString("breaks", breaksStr)
            } else {
                remove("breaks")
            }

            apply()
        }
    }

    private fun clearState() {
        prefs.edit().clear().apply()
    }

    override fun isOnBreak(): Boolean = currentBreakStart != null

    fun getCurrentBreakTime(): Long {
        val breakStart = currentBreakStart ?: return 0L
        return System.currentTimeMillis() - breakStart
    }

    override fun getTotalBreakTime(): Long {
        val completedBreaksTime = breaks.sumOf { it.second - it.first }
        val currentBreakTime = if (currentBreakStart != null) {
            System.currentTimeMillis() - currentBreakStart!!
        } else 0L
        return completedBreaksTime + currentBreakTime
    }

    override fun getCurrentEffectiveTime(): Long {
        if (!isTracking()) return 0L
        return getCurrentDuration() - getTotalBreakTime()
    }

    override fun isTracking(): Boolean = startTime > 0L && endTime == 0L

    override fun getCurrentDuration(): Long =
        if (isTracking()) System.currentTimeMillis() - startTime else 0L

    override fun getBreakCount(): Int = breaks.size

    override fun reset() {
        startTime = 0L
        endTime = 0L
        breaks.clear()
        currentBreakStart = null
        clearState()
    }
}
