package com.b1gbr0ther

import kotlin.math.abs

class BlowDetector {
    companion object {
        private const val BLOW_RMS_THRESHOLD = 5.5f
        private const val MIN_DURATION = 200L
        private const val MAX_DURATION = 2000L
        private const val COOLDOWN = 4000L
        private const val REQUIRED_HIGH_RMS = 3
    }

    private var blowStartTime: Long = 0
    private var lastBlowTime: Long = 0
    private var isBlowing: Boolean = false
    private var highRMSCount: Int = 0
    private var lastRMSValues = ArrayDeque<Float>(5)

    fun processAudioSample(rms: Float, currentTimeMs: Long): Boolean {
        if (lastRMSValues.size >= 5) {
            lastRMSValues.removeFirst()
        }
        lastRMSValues.addLast(rms)

        if (currentTimeMs - lastBlowTime < COOLDOWN) {
            return false
        }

        if (isSpeechLikePattern()) {
            resetState()
            return false
        }

        if (rms > BLOW_RMS_THRESHOLD) {
            highRMSCount++
            if (!isBlowing && highRMSCount >= REQUIRED_HIGH_RMS) {
                isBlowing = true
                blowStartTime = currentTimeMs
            }
        } else {
            highRMSCount = 0
            if (isBlowing) {
                val duration = currentTimeMs - blowStartTime
                if (duration >= MIN_DURATION && duration <= MAX_DURATION) {
                    lastBlowTime = currentTimeMs
                    resetState()
                    return true
                }
                resetState()
            }
        }

        if (isBlowing && currentTimeMs - blowStartTime > MAX_DURATION) {
            resetState()
        }

        return false
    }

    private fun isSpeechLikePattern(): Boolean {
        if (lastRMSValues.size < 5) return false

        val mean = lastRMSValues.average()
        val variance = lastRMSValues.map { (it - mean) * (it - mean) }.average()

        return variance > 3.0f
    }

    private fun resetState() {
        isBlowing = false
        highRMSCount = 0
    }

    fun reset() {
        resetState()
        lastBlowTime = 0
        blowStartTime = 0
        lastRMSValues.clear()
    }
}