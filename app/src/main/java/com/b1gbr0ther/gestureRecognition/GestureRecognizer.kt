package com.b1gbr0ther.gestureRecognition

import android.widget.Toast
import com.b1gbr0ther.DashboardActivity
import kotlin.math.abs

class GestureRecognizer {
    private val sampleWindow = mutableListOf<GestureSample>()
    private val windowDuration = 500L // milliseconds

    fun addSensorData(x: Float, y: Float, z: Float, timestamp: Long) {
        sampleWindow.add(GestureSample(x, y, z, timestamp))
        trimOldSamples(timestamp)
//        analyzeGesture()
    }

    private fun trimOldSamples(currentTime: Long) {
        sampleWindow.removeIf { currentTime - it.timestamp > windowDuration }
    }

    fun analyzeGesture(): GestureType {
        if (sampleWindow.size < 2) return GestureType.UNIDENTIFIED

        var totalDx = 0f
        var totalDy = 0f
        
        for (i in 1 until sampleWindow.size) {
            val prev = sampleWindow[i - 1]
            val curr = sampleWindow[i]

            totalDx += (curr.x - prev.x)
            totalDy += (curr.y - prev.y)
        }

        val avgDx = totalDx / (sampleWindow.size - 1)
        val avgDy = totalDy / (sampleWindow.size - 1)

        if (abs(avgDx) > 2 && abs(avgDx) > abs(avgDy)) {
            return if (avgDx < 0) GestureType.LEFT else GestureType.RIGHT
        } else if (abs(avgDy) > 2) {
            return if (avgDy < 0) GestureType.DOWN else GestureType.UP
        }

        return GestureType.SHAKE
    }
}