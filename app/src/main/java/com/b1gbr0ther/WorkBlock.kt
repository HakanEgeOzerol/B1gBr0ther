package com.b1gbr0ther

data class WorkBlock(
    val dayIndex: Int,     // 0 = Mon, 6 = Sun
    val startHour: Float,  // Example: 9.5f = 09:30, and 17.0f = 17:00
    val endHour: Float,
    val isBreak: Boolean
)
