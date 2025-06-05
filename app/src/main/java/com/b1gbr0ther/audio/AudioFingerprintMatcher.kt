package com.b1gbr0ther.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.content.res.AssetFileDescriptor
import android.util.Log
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.*

data class AudioFingerprint(
    val filename: String,
    val spectralCentroid: Double,
    val spectralRolloff: Double,
    val zeroCrossingRate: Double,
    val energyDistribution: DoubleArray,
    val mfcc: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioFingerprint
        return filename == other.filename
    }

    override fun hashCode(): Int = filename.hashCode()
}

data class MatchResult(
    val isMatch: Boolean,
    val confidence: Double,
    val matchedFile: String?
)

class AudioFingerprintMatcher(private val context: Context) {
    private val fingerprints = mutableListOf<AudioFingerprint>()
    private val fft = DoubleFFT_1D(2048)
    
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE = 2048
        private const val OVERLAP = 1024
        private const val MATCH_THRESHOLD = 0.7
        private const val TAG = "AudioFingerprintMatcher"
    }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing audio fingerprint matcher...")
                loadFingerprintsFromAssets()
                Log.d(TAG, "Loaded ${fingerprints.size} fingerprints")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize fingerprint matcher", e)
                throw e
            }
        }
    }

    private suspend fun loadFingerprintsFromAssets() {

        loadFingerprintsFromFolder("urination/men")
        loadFingerprintsFromFolder("urination/woman")
    }

    private suspend fun loadFingerprintsFromFolder(folderPath: String) {
        try {
            val assetFiles = context.assets.list(folderPath) ?: return
            Log.d(TAG, "Loading fingerprints from $folderPath: ${assetFiles.size} files")
            
            for (filename in assetFiles) {
                if (filename.endsWith(".mp3", ignoreCase = true)) {
                    try {
                        val fullPath = "$folderPath/$filename"
                        val fingerprint = createFingerprintFromAsset(fullPath)
                        fingerprints.add(fingerprint)
                        Log.d(TAG, "Created fingerprint for: $filename")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create fingerprint for $filename", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load fingerprints from folder: $folderPath", e)
        }
    }

    private suspend fun createFingerprintFromAsset(assetPath: String): AudioFingerprint {
        return withContext(Dispatchers.IO) {
            val audioData = extractAudioFromAsset(context, assetPath)
            createFingerprint(assetPath, audioData)
        }
    }

    private fun extractAudioFromAsset(context: Context, assetPath: String): ShortArray {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var afd: AssetFileDescriptor? = null
        
        return try {
            afd = context.assets.openFd(assetPath)
            extractor = MediaExtractor()
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            
            if (audioTrackIndex == -1) {
                Log.e(TAG, "No audio track found in $assetPath")
                return ShortArray(0)
            }
            
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val audioSamples = mutableListOf<Short>()
            val info = MediaCodec.BufferInfo()
            var isEOS = false
            var timeoutUs = 10000L // 10ms timeout
            
            while (!isEOS) {
                val inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            val presentationTime = extractor.sampleTime
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTime, 0)
                            extractor.advance()
                        }
                    }
                }
                
                val outputBufferIndex = decoder.dequeueOutputBuffer(info, timeoutUs)
                when {
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        
                        if (outputBuffer != null && info.size > 0) {
                            // Convert ByteBuffer to ShortArray (assuming 16-bit PCM)
                            val pcmData = ByteArray(info.size)
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            outputBuffer.get(pcmData)
                            
                            for (i in pcmData.indices step 2) {
                                if (i + 1 < pcmData.size) {
                                    val sample = ((pcmData[i + 1].toInt() shl 8) or (pcmData[i].toInt() and 0xFF)).toShort()
                                    audioSamples.add(sample)
                                }
                            }
                        }
                        
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isEOS = true
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed for $assetPath")
                    }
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available yet, continue
                    }
                }
            }
            
            Log.d(TAG, "Extracted ${audioSamples.size} audio samples from $assetPath")
            audioSamples.toShortArray()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract audio from asset: $assetPath", e)
            ShortArray(0)
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/releasing decoder", e)
            }
            try {
                extractor?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing extractor", e)
            }
            try {
                afd?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing asset file descriptor", e)
            }
        }
    }

    private fun createFingerprint(filename: String, audioData: ShortArray): AudioFingerprint {
        if (audioData.isEmpty()) {
            return AudioFingerprint(
                filename = filename,
                spectralCentroid = 0.0,
                spectralRolloff = 0.0,
                zeroCrossingRate = 0.0,
                energyDistribution = DoubleArray(10) { 0.0 },
                mfcc = DoubleArray(13) { 0.0 }
            )
        }
        
        val spectralCentroid = calculateSpectralCentroid(audioData)
        val spectralRolloff = calculateSpectralRolloff(audioData)
        val zeroCrossingRate = calculateZeroCrossingRate(audioData)
        val energyDistribution = calculateEnergyDistribution(audioData)
        val mfcc = calculateMFCC(audioData)
        
        return AudioFingerprint(
            filename = filename,
            spectralCentroid = spectralCentroid,
            spectralRolloff = spectralRolloff,
            zeroCrossingRate = zeroCrossingRate,
            energyDistribution = energyDistribution,
            mfcc = mfcc
        )
    }

    private fun calculateSpectralCentroid(audioData: ShortArray): Double {
        val fftSize = minOf(2048, audioData.size)
        val fftInput = DoubleArray(fftSize * 2)
        
        for (i in 0 until fftSize) {
            fftInput[i * 2] = audioData[i].toDouble()
            fftInput[i * 2 + 1] = 0.0
        }
        
        fft.complexForward(fftInput)
        
        var weightedSum = 0.0
        var magnitudeSum = 0.0
        
        for (i in 0 until fftSize / 2) {
            val real = fftInput[i * 2]
            val imag = fftInput[i * 2 + 1]
            val magnitude = sqrt(real * real + imag * imag)
            val frequency = i * SAMPLE_RATE.toDouble() / fftSize
            
            weightedSum += frequency * magnitude
            magnitudeSum += magnitude
        }
        
        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0.0
    }

    private fun calculateSpectralRolloff(audioData: ShortArray): Double {
        val fftSize = minOf(2048, audioData.size)
        val fftInput = DoubleArray(fftSize * 2)
        
        for (i in 0 until fftSize) {
            fftInput[i * 2] = audioData[i].toDouble()
            fftInput[i * 2 + 1] = 0.0
        }
        
        fft.complexForward(fftInput)
        
        val magnitudes = DoubleArray(fftSize / 2)
        var totalEnergy = 0.0
        
        for (i in 0 until fftSize / 2) {
            val real = fftInput[i * 2]
            val imag = fftInput[i * 2 + 1]
            magnitudes[i] = sqrt(real * real + imag * imag)
            totalEnergy += magnitudes[i]
        }
        
        val threshold = totalEnergy * 0.85 // 85% rolloff
        var cumulativeEnergy = 0.0
        
        for (i in magnitudes.indices) {
            cumulativeEnergy += magnitudes[i]
            if (cumulativeEnergy >= threshold) {
                return i * SAMPLE_RATE.toDouble() / fftSize
            }
        }
        
        return SAMPLE_RATE.toDouble() / 2
    }

    private fun calculateZeroCrossingRate(audioData: ShortArray): Double {
        if (audioData.size < 2) return 0.0
        
        var crossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i] >= 0) != (audioData[i - 1] >= 0)) {
                crossings++
            }
        }
        
        return crossings.toDouble() / audioData.size
    }

    private fun calculateEnergyDistribution(audioData: ShortArray): DoubleArray {
        val bands = 10
        val energyDistribution = DoubleArray(bands) { 0.0 }
        val fftSize = minOf(2048, audioData.size)
        val fftInput = DoubleArray(fftSize * 2)
        
        for (i in 0 until fftSize) {
            fftInput[i * 2] = audioData[i].toDouble()
            fftInput[i * 2 + 1] = 0.0
        }
        
        fft.complexForward(fftInput)
        
        for (i in 0 until fftSize / 2) {
            val real = fftInput[i * 2]
            val imag = fftInput[i * 2 + 1]
            val magnitude = sqrt(real * real + imag * imag)
            val bandIndex = (i * bands) / (fftSize / 2)
            energyDistribution[minOf(bandIndex, bands - 1)] += magnitude
        }
        
        return energyDistribution
    }

    private fun calculateMFCC(audioData: ShortArray): DoubleArray {
        // Simplified MFCC calculation - in production you'd want a more sophisticated implementation
        val mfcc = DoubleArray(13) { 0.0 }
        val fftSize = minOf(2048, audioData.size)
        val fftInput = DoubleArray(fftSize * 2)
        
        for (i in 0 until fftSize) {
            fftInput[i * 2] = audioData[i].toDouble()
            fftInput[i * 2 + 1] = 0.0
        }
        
        fft.complexForward(fftInput)
        
        for (i in mfcc.indices) {
            var sum = 0.0
            val start = (i * fftSize / 2) / mfcc.size
            val end = ((i + 1) * fftSize / 2) / mfcc.size
            
            for (j in start until end) {
                val real = fftInput[j * 2]
                val imag = fftInput[j * 2 + 1]
                sum += sqrt(real * real + imag * imag)
            }
            
            mfcc[i] = ln(maxOf(sum, 1.0))
        }
        
        return mfcc
    }

    fun matchAudioSample(audioSample: ShortArray): MatchResult {
        if (fingerprints.isEmpty()) {
            return MatchResult(false, 0.0, null)
        }
        
        val sampleFingerprint = createFingerprint("live_sample", audioSample)
        var bestMatch: AudioFingerprint? = null
        var bestSimilarity = 0.0
        
        for (fingerprint in fingerprints) {
            val similarity = calculateSimilarity(sampleFingerprint, fingerprint)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = fingerprint
            }
        }
        
        val isMatch = bestSimilarity >= MATCH_THRESHOLD
        Log.d(TAG, "Best match: ${bestMatch?.filename}, similarity: $bestSimilarity, isMatch: $isMatch")
        
        return MatchResult(isMatch, bestSimilarity, bestMatch?.filename)
    }

    private fun calculateSimilarity(sample: AudioFingerprint, reference: AudioFingerprint): Double {
        val centroidSim = 1.0 - abs(sample.spectralCentroid - reference.spectralCentroid) / 
                         maxOf(sample.spectralCentroid, reference.spectralCentroid, 1.0)
        
        val rolloffSim = 1.0 - abs(sample.spectralRolloff - reference.spectralRolloff) / 
                        maxOf(sample.spectralRolloff, reference.spectralRolloff, 1.0)
        
        val zcrSim = 1.0 - abs(sample.zeroCrossingRate - reference.zeroCrossingRate)
        
        // Energy distribution similarity (cosine similarity)
        val energySim = cosineSimilarity(sample.energyDistribution, reference.energyDistribution)
        
        // MFCC similarity (cosine similarity)
        val mfccSim = cosineSimilarity(sample.mfcc, reference.mfcc)
        
        // Weighted average
        return (centroidSim * 0.2 + rolloffSim * 0.2 + zcrSim * 0.1 + energySim * 0.3 + mfccSim * 0.2)
    }

    private fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }
} 