package com.b1gbr0ther.audio

import android.content.Context
import kotlinx.coroutines.*
import java.util.ArrayDeque
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

class UrinationSoundDetector(private val context: Context) {
    companion object {
        private const val MATCH_CONFIDENCE_THRESHOLD = 0.55
        private const val CONSECUTIVE_MATCHES_REQUIRED = 2
        private const val COOLDOWN_PERIOD = 30000L
        private const val BUFFER_SIZE = 2048
        private const val TAG = "UrinationSoundDetector"
    }

    private val fingerprintMatcher = AudioFingerprintMatcher(context)
    private var lastDetectionTime = 0L
    private var consecutiveMatches = 0
    private var isInitialized = false
    private var isInitializing = false
    private var useSimpleDetection = true
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val audioBuffer = ArrayDeque<Short>(BUFFER_SIZE * 3)
    private val bufferMutex = Mutex()

    suspend fun initialize() {
        if (isInitialized || isInitializing) {
            Log.d(TAG, "Already initialized or initializing")
            return
        }
        
        isInitializing = true
        Log.d(TAG, "Quick initialization - using simple energy detection first...")
        
        try {
            // Start with simple detection immediately (no MP3 loading needed)
            isInitialized = true
            useSimpleDetection = true
            Log.i(TAG, "UrinationSoundDetector ready with simple detection")
            
            // Load fingerprints in background, othervise it takes too long to load
            scope.launch {
                try {
                    Log.d(TAG, "Loading advanced fingerprint matching in background...")
                    fingerprintMatcher.initialize()
                    useSimpleDetection = false
                    Log.i(TAG, "Advanced fingerprint matching now active")
                } catch (e: Exception) {
                    Log.w(TAG, "Fingerprint matching failed, continuing with simple detection", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UrinationSoundDetector", e)
            throw e
        } finally {
            isInitializing = false
        }
    }

    suspend fun processAudioSample(samples: ShortArray): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastDetectionTime < COOLDOWN_PERIOD) {
            return false
        }

        if (!isInitialized) {
            Log.d(TAG, "Not initialized yet, skipping processing")
            return false
        }

        val analysisBuffer = bufferMutex.withLock {
            for (sample in samples) {
                if (audioBuffer.size >= BUFFER_SIZE * 3) audioBuffer.removeFirst()
                audioBuffer.addLast(sample)
            }
            
            val buffer = ShortArray(BUFFER_SIZE) { 0 }
            for (i in 0 until BUFFER_SIZE) {
                buffer[i] = audioBuffer.elementAtOrNull(audioBuffer.size - BUFFER_SIZE + i) ?: 0
            }
            buffer
        }

        if (audioBuffer.size >= BUFFER_SIZE) {
            Log.d(TAG, "Processing audio buffer (${audioBuffer.size} samples) - using ${if (useSimpleDetection) "simple" else "advanced"} detection")
            
            try {
                val isMatch = if (useSimpleDetection) {
                    val energy = analysisBuffer.map { it.toDouble() * it.toDouble() }.average()
                    val threshold = 5000.0
                    Log.v(TAG, "Simple detection - energy: $energy, threshold: $threshold")
                    energy > threshold
                } else {
                    val matchResult = fingerprintMatcher.matchAudioSample(analysisBuffer)
                    Log.d(TAG, "Fingerprint result: isMatch=${matchResult.isMatch}, confidence=${matchResult.confidence}")
                    matchResult.isMatch && matchResult.confidence >= MATCH_CONFIDENCE_THRESHOLD
                }

                if (isMatch) {
                    consecutiveMatches++
                    Log.d(TAG, "Match found! Consecutive: $consecutiveMatches/$CONSECUTIVE_MATCHES_REQUIRED")
                    
                    if (consecutiveMatches >= CONSECUTIVE_MATCHES_REQUIRED) {
                        Log.d(TAG, "TRIGGERING DETECTION - about to return true!")
                        lastDetectionTime = currentTime
                        consecutiveMatches = 0
                        
                        bufferMutex.withLock {
                            audioBuffer.clear()
                        }
                        
                        Log.i(TAG, "URINATION DETECTED using ${if (useSimpleDetection) "simple" else "advanced"} detection!")
                        return true
                    }
                } else {
                    consecutiveMatches = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio processing", e)
            }
        } else {
            Log.v(TAG, "Buffer too small: ${audioBuffer.size}/$BUFFER_SIZE")
        }

        return false
    }

    fun reset() {
        consecutiveMatches = 0
        lastDetectionTime = 0L
        audioBuffer.clear()
        Log.d(TAG, "UrinationSoundDetector reset")
    }

    fun getLastMatchInfo(): String {
        return "Last detection: ${if (lastDetectionTime > 0) 
            "${(System.currentTimeMillis() - lastDetectionTime) / 1000}s ago" 
            else "never"}"
    }
}
