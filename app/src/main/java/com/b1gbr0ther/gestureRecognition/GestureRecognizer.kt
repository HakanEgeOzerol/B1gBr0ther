package com.b1gbr0ther.gestureRecognition

import kotlin.math.abs

class GestureRecognizer(private val onGesture: (GestureType) -> Unit) {
    private val sampleWindow = mutableListOf<GestureSample>()
    private val windowDuration = 500L // milliseconds

    fun addSensorData(x: Float, y: Float, z: Float, timestamp: Long) {
        sampleWindow.add(GestureSample(x, y, z, timestamp))
        trimOldSamples(timestamp)
        analyzeGesture()
    }

    private fun trimOldSamples(currentTime: Long) {
        sampleWindow.removeIf { currentTime - it.timestamp > windowDuration }
    }

    private fun analyzeGesture() {
        val first = sampleWindow.firstOrNull() ?: return
        val last = sampleWindow.lastOrNull() ?: return

        val dx = last.x - first.x
        val dy = last.y - first.y

        if (abs(dx) > 5 && abs(dx) > abs(dy)) {
            onGesture(if (dx > 0) GestureType.RIGHT else GestureType.LEFT)
        } else if (abs(dy) > 5) {
            onGesture(if (dy > 0) GestureType.DOWN else GestureType.UP)
        }
    }
}