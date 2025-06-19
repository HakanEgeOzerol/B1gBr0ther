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
        val first = sampleWindow.firstOrNull() ?: return GestureType.UNIDENTIFIED
        val last = sampleWindow.lastOrNull() ?: return GestureType.UNIDENTIFIED

        val dx = last.x - first.x
        val dy = last.y - first.y

        if (abs(dx) > 5 && abs(dx) > abs(dy)) {//Horizontal
            return if (dx < 0){
                GestureType.LEFT
            } else{
                GestureType.RIGHT
            }
        } else if (abs(dy) > abs(dx) && abs(dy) > 5) {//Vertical
            return if (dy < 0){
                GestureType.DOWN
            } else{
                GestureType.UP
            }
        }

        return GestureType.SHAKE
    }
}