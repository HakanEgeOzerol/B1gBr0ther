package com.b1gbr0ther.detection

class BlowDetector {
    companion object {
        private const val BLOW_RMS_THRESHOLD = 8.0f
        private const val MIN_DURATION = 200L
        private const val MAX_DURATION = 2000L
        private const val COOLDOWN = 4000L
        private const val REQUIRED_HIGH_RMS = 3
        private const val COMPLETION_THRESHOLD = 6.0f
    }

    private var blowStartTime: Long = 0
    private var lastBlowTime: Long = 0
    private var isBlowing: Boolean = false
    private var highRMSCount: Int = 0
    private var maxRMSInBlow: Float = 0f

    fun processAudioSample(rms: Float, currentTimeMs: Long): Boolean {
        if (currentTimeMs - lastBlowTime < COOLDOWN) {
            return false
        }

        if (rms > BLOW_RMS_THRESHOLD) {
            highRMSCount++
            if (!isBlowing && highRMSCount >= REQUIRED_HIGH_RMS) {
                isBlowing = true
                blowStartTime = currentTimeMs
                maxRMSInBlow = rms
                android.util.Log.d("BlowDetector", "Blow detection started! RMS: $rms")
            } else if (isBlowing) {
                maxRMSInBlow = maxOf(maxRMSInBlow, rms)
            }
        } else {
            highRMSCount = 0
            if (isBlowing) {
                val duration = currentTimeMs - blowStartTime
                if ((rms < COMPLETION_THRESHOLD || duration >= MIN_DURATION * 2) &&
                    duration >= MIN_DURATION && duration <= MAX_DURATION
                ) {
                    android.util.Log.i("BlowDetector", "BLOW DETECTED! Duration: $duration ms, Peak: $maxRMSInBlow")
                    lastBlowTime = currentTimeMs
                    resetState()
                    return true
                }
                if (duration < MIN_DURATION && rms < COMPLETION_THRESHOLD) {
                    android.util.Log.d("BlowDetector", "Blow too short, resetting")
                    resetState()
                }
            }
        }

        if (isBlowing) {
            val duration = currentTimeMs - blowStartTime
            if (duration >= MIN_DURATION && maxRMSInBlow > BLOW_RMS_THRESHOLD + 1.0f) {
                if (duration >= MIN_DURATION * 3) {
                    android.util.Log.i("BlowDetector", "BLOW DETECTED! (Auto-completed) Duration: $duration ms, Peak: $maxRMSInBlow")
                    lastBlowTime = currentTimeMs
                    resetState()
                    return true
                }
            }
            
            if (duration > MAX_DURATION) {
                android.util.Log.d("BlowDetector", "Blow detection timeout")
                resetState()
            }
        }

        return false
    }

    private fun resetState() {
        isBlowing = false
        highRMSCount = 0
        maxRMSInBlow = 0f
    }

    fun reset() {
        resetState()
        lastBlowTime = 0
        blowStartTime = 0
    }
}