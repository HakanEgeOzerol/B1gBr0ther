package com.b1gbr0ther

import android.util.Log

class SneezeDetector {
    companion object {
        private const val TAG = "SneezeDetector"
        
        // Sneeze characteristics (tuned based on actual audio levels of 8-10 dB)
        private const val SNEEZE_THRESHOLD = 9.0f  // Lowered from 12.0f to match actual audio levels
        private const val MIN_SNEEZE_DURATION = 300L  // 0.3 seconds minimum
        private const val MAX_SNEEZE_DURATION = 1500L // 1.5 seconds maximum  
        private const val RAPID_ONSET_THRESHOLD = 3.0f // Lowered from 6.0f - Quick volume increase
        private const val COOLDOWN_PERIOD = 3000L // 3 seconds between detections
        private const val SPEECH_VARIANCE_THRESHOLD = 2.5f // Detect speech-like patterns
    }
    
    private var isDetecting = false
    private var detectionStartTime = 0L
    private var maxVolumeInBurst = 0f
    private var lastDetectionTime = 0L
    private var previousRms = 0f
    private var lastRMSValues = ArrayDeque<Float>(10) // Track recent RMS values for speech detection
    
    fun processAudioSample(rmsdB: Float, timestamp: Long): Boolean {
        // Debug logging for high audio levels
        if (rmsdB > 8.5f) {
            Log.d(TAG, "Processing audio: rmsdB=$rmsdB, isDetecting=$isDetecting, cooldown=${timestamp - lastDetectionTime}ms")
        }
        
        // Update RMS history for speech detection
        if (lastRMSValues.size >= 10) {
            lastRMSValues.removeFirst()
        }
        lastRMSValues.addLast(rmsdB)
        
        // Cooldown period to prevent multiple detections
        if (timestamp - lastDetectionTime < COOLDOWN_PERIOD) {
            if (rmsdB > 8.5f) {
                Log.d(TAG, "In cooldown period - ignoring")
            }
            return false
        }
        
        // Check for speech-like patterns and reject them
        if (isSpeechLikePattern()) {
            if (isDetecting) {
                Log.d(TAG, "Speech pattern detected - cancelling sneeze detection")
                reset()
            }
            return false
        }
        
        // Calculate volume change rate (rapid onset detection)
        val volumeChangeRate = rmsdB - previousRms
        previousRms = rmsdB
        
        // Start detection if we hit the sneeze threshold with rapid onset
        if (!isDetecting && rmsdB > SNEEZE_THRESHOLD && volumeChangeRate > RAPID_ONSET_THRESHOLD) {
            isDetecting = true
            detectionStartTime = timestamp
            maxVolumeInBurst = rmsdB
            Log.i(TAG, "Sneeze detection started - RMS: $rmsdB, onset rate: $volumeChangeRate")
            return false
        }
        
        // Continue tracking if we're in detection mode
        if (isDetecting) {
            val duration = timestamp - detectionStartTime
            maxVolumeInBurst = maxOf(maxVolumeInBurst, rmsdB)
            
            Log.v(TAG, "Tracking sneeze: duration=${duration}ms, current=$rmsdB, peak=$maxVolumeInBurst")
            
            // Check if volume drops significantly (end of sneeze)
            val isVolumeDropped = rmsdB < (maxVolumeInBurst * 0.4f) // Slightly more lenient
            
            // Valid sneeze: right duration, high peak, and volume dropped
            if (isVolumeDropped && duration in MIN_SNEEZE_DURATION..MAX_SNEEZE_DURATION) {
                Log.i(TAG, "SNEEZE DETECTED! Duration: ${duration}ms, Peak: $maxVolumeInBurst")
                reset()
                lastDetectionTime = timestamp
                return true
            }
            
            // Reset if too long (probably not a sneeze)
            if (duration > MAX_SNEEZE_DURATION) {
                Log.d(TAG, "Detection timeout - duration too long: ${duration}ms")
                reset()
                return false
            }
        }
        
        return false
    }
    
    private fun isSpeechLikePattern(): Boolean {
        if (lastRMSValues.size < 5) return false
        
        val mean = lastRMSValues.average()
        val variance = lastRMSValues.map { (it - mean) * (it - mean) }.average()
        
        // Speech has variable patterns; sneezes are more sudden bursts
        return variance > SPEECH_VARIANCE_THRESHOLD
    }
    
    fun reset() {
        isDetecting = false
        detectionStartTime = 0L
        maxVolumeInBurst = 0f
        previousRms = 0f
        lastRMSValues.clear()
        Log.d(TAG, "SneezeDetector reset")
    }
    
    fun getLastDetectionInfo(): String {
        return "Last sneeze: ${if (lastDetectionTime > 0) 
            "${(System.currentTimeMillis() - lastDetectionTime) / 1000}s ago" 
            else "never"}"
    }
} 