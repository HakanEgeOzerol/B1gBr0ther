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
        private const val STARTUP_GRACE_PERIOD = 2000L // 2 seconds to ignore microphone initialization spikes
    }
    
    private var isDetecting = false
    private var detectionStartTime = 0L
    private var maxVolumeInBurst = 0f
    private var lastDetectionTime = 0L
    private var previousRms = 0f
    private var initializationTime = 0L // Track when detector was reset/started
    
    fun processAudioSample(rmsdB: Float, timestamp: Long): Boolean {
        // Debug logging for high audio levels
        if (rmsdB > 8.5f) {
            Log.d(TAG, "Processing audio: rmsdB=$rmsdB, isDetecting=$isDetecting, cooldown=${timestamp - lastDetectionTime}ms, grace=${timestamp - initializationTime}ms")
        }
        
        // Grace period after initialization to ignore microphone startup spikes
        if (timestamp - initializationTime < STARTUP_GRACE_PERIOD) {
            if (rmsdB > 8.5f) {
                Log.d(TAG, "In startup grace period - ignoring potential false positive")
            }
            previousRms = rmsdB // Update baseline but don't detect
            return false
        }
        
        // Cooldown period to prevent multiple detections
        if (timestamp - lastDetectionTime < COOLDOWN_PERIOD) {
            if (rmsdB > 8.5f) {
                Log.d(TAG, "In cooldown period - ignoring")
            }
            return false
        }
        
        // Calculate volume change rate (rapid onset detection)
        val volumeChangeRate = rmsdB - previousRms
        previousRms = rmsdB
        
        // More robust onset detection: require both threshold AND reasonable change rate
        // Prevent massive false spikes from triggering detection
        if (!isDetecting && rmsdB > SNEEZE_THRESHOLD && volumeChangeRate > RAPID_ONSET_THRESHOLD && volumeChangeRate < 15.0f) {
            isDetecting = true
            detectionStartTime = timestamp
            maxVolumeInBurst = rmsdB
            Log.i(TAG, "Sneeze detection started - RMS: $rmsdB, onset rate: $volumeChangeRate")
            return false
        } else if (!isDetecting && rmsdB > SNEEZE_THRESHOLD && volumeChangeRate >= 15.0f) {
            // Log massive spikes that we're ignoring (likely false positives)
            Log.w(TAG, "Ignoring massive onset spike - RMS: $rmsdB, onset rate: $volumeChangeRate (likely false positive)")
            return false
        }
        
        // Continue tracking if we're in detection mode
        if (isDetecting) {
            val duration = timestamp - detectionStartTime
            maxVolumeInBurst = maxOf(maxVolumeInBurst, rmsdB)
            
            Log.v(TAG, "Tracking sneeze: duration=${duration}ms, current=$rmsdB, peak=$maxVolumeInBurst")
            
            // Check if volume drops significantly (end of sneeze)
            val isVolumeDropped = rmsdB < (maxVolumeInBurst * 0.4f)
            
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
    
    fun reset() {
        isDetecting = false
        detectionStartTime = 0L
        maxVolumeInBurst = 0f
        previousRms = 0f
        initializationTime = System.currentTimeMillis() // Track when we reset
        Log.d(TAG, "SneezeDetector reset - starting grace period")
    }
    
    fun getLastDetectionInfo(): String {
        return "Last sneeze: ${if (lastDetectionTime > 0) 
            "${(System.currentTimeMillis() - lastDetectionTime) / 1000}s ago" 
            else "never"}"
    }
} 