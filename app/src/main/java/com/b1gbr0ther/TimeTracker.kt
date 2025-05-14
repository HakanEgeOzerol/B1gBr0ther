package com.b1gbr0ther

class TimeTracker {

    private var startTime: Long = 0L
    private var endTime: Long = 0L

    private var currentBreakStart: Long? = null
    private val breaks = mutableListOf<Pair<Long, Long>>() // Pair of (start, end)

    fun startTracking() {
        startTime = System.currentTimeMillis()
        endTime = 0L
        breaks.clear()
        currentBreakStart = null
    }

    fun startBreak(): Boolean {
        if (!isTracking() || currentBreakStart != null) return false
        currentBreakStart = System.currentTimeMillis()
        return true
    }

    fun endBreak(): Boolean {
        if (!isTracking() || currentBreakStart == null) return false
        val breakEnd = System.currentTimeMillis()
        breaks.add(Pair(currentBreakStart!!, breakEnd))
        currentBreakStart = null
        return true
    }

    fun isOnBreak(): Boolean = currentBreakStart != null

    fun getCurrentBreakTime(): Long {
        val breakStart = currentBreakStart ?: return 0L
        return System.currentTimeMillis() - breakStart
    }

    fun getTotalBreakTime(): Long {
        val completedBreaksTime = breaks.sumOf { it.second - it.first }
        val currentBreakTime = if (currentBreakStart != null) {
            System.currentTimeMillis() - currentBreakStart!!
        } else 0L
        return completedBreaksTime + currentBreakTime
    }

    fun getCurrentEffectiveTime(): Long {
        if (!isTracking()) return 0L
        return getCurrentDuration() - getTotalBreakTime()
    }

    fun stopTracking(): TrackingSummary? {
        if (!isTracking()) return null
        endTime = System.currentTimeMillis()

        currentBreakStart?.let {
            breaks.add(Pair(it, endTime))
            currentBreakStart = null
        }

        val totalTime = endTime - startTime
        val totalBreakTime = breaks.sumOf { it.second - it.first }
        val effectiveTime = totalTime - totalBreakTime

        return TrackingSummary(
            totalTimeMillis = totalTime,
            breakTimeMillis = totalBreakTime,
            effectiveTimeMillis = effectiveTime,
            breakCount = breaks.size
        )
    }

    fun isTracking(): Boolean = startTime > 0L && endTime == 0L

    fun getCurrentDuration(): Long =
        if (isTracking()) System.currentTimeMillis() - startTime else 0L

    fun getBreakCount(): Int = breaks.size

    fun reset() {
        startTime = 0L
        endTime = 0L
        breaks.clear()
        currentBreakStart = null
    }

    data class TrackingSummary(
        val totalTimeMillis: Long,
        val breakTimeMillis: Long,
        val effectiveTimeMillis: Long,
        val breakCount: Int = 0
    )
}
