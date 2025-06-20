package com.b1gbr0ther.gestureRecognition

import kotlin.math.abs

class GestureRecognizer {
    private val motionWindow = mutableListOf<Motion>()
    private val windowDuration = 200L // milliseconds

    fun addSensorData(x: Float, y: Float, z: Float, timestamp: Long) {//Continuously adding sensor data, up to a window
        motionWindow.add(Motion(x, y, z, timestamp))
        trimOldSamples(timestamp)
    }

    private fun trimOldSamples(currentTime: Long) {//Get rid of old samples
        motionWindow.removeIf { currentTime - it.timestamp > windowDuration }
    }

    fun analyzeGesture(): GestureType {//Analyze gesture
        if (motionWindow.size < 2) return GestureType.UNIDENTIFIED //If less then 2 samples the gesture cant be analysed

        var totalDx = 0f
        var totalDy = 0f

        for (i in 1 until motionWindow.size) {//Average is found
            val prev = motionWindow[i - 1]
            val curr = motionWindow[i]

            totalDx += (curr.x - prev.x)
            totalDy += (curr.y - prev.y)
        }

        val avgDx = totalDx / (motionWindow.size - 1)
        val avgDy = totalDy / (motionWindow.size - 1)

        if (abs(avgDx) > 2 && abs(avgDx) > abs(avgDy)) {//Gesture is identified
            return if (avgDx < 0) GestureType.LEFT else GestureType.RIGHT
        } else if (abs(avgDy) > 2) {
            return if (avgDy < 0) GestureType.DOWN else GestureType.UP
        }

        return GestureType.SHAKE
    }
}