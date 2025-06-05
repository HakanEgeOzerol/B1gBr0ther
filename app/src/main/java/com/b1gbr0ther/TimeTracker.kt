package com.b1gbr0ther

import android.content.Context

object TimeTracker {
    private var instance: TimeTrackerInterface? = null

    fun getInstance(context: Context): TimeTrackerInterface {
        if (instance == null) {
            instance = SessionTracker(context.applicationContext)
        }
        return instance!!
    }

    fun startTracking() = instance?.startTracking() ?: false
    fun stopTracking() = instance?.stopTracking()
    fun startBreak() = instance?.startBreak() ?: false
    fun endBreak() = instance?.endBreak() ?: false
    fun isTracking() = instance?.isTracking() ?: false
    fun isOnBreak() = instance?.isOnBreak() ?: false
    fun getCurrentDuration() = instance?.getCurrentDuration() ?: 0L
    fun getTotalBreakTime() = instance?.getTotalBreakTime() ?: 0L
    fun getCurrentEffectiveTime() = instance?.getCurrentEffectiveTime() ?: 0L
    fun getBreakCount() = instance?.getBreakCount() ?: 0
    fun reset() = instance?.reset()

    data class TrackingSummary(
        val totalTimeMillis: Long,
        val breakTimeMillis: Long,
        val effectiveTimeMillis: Long,
        val breakCount: Int = 0
    )
}
