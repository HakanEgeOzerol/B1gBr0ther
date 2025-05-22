package com.b1gbr0ther

interface TimeTrackerInterface {
    fun startTracking(): Boolean
    fun stopTracking(): TimeTracker.TrackingSummary?
    fun startBreak(): Boolean
    fun endBreak(): Boolean
    fun isTracking(): Boolean
    fun isOnBreak(): Boolean
    fun getCurrentDuration(): Long
    fun getTotalBreakTime(): Long
    fun getCurrentEffectiveTime(): Long
    fun getBreakCount(): Int
    fun reset()
}