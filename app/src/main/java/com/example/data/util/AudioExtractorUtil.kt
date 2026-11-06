package com.example.data.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

private const val TAG = "AudioExtractorUtil"

data class ExtractedAudio(
    val audioBytes: ByteArray,
    val mimeType: String,
    val durationMs: Long,
    val hasAudio: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtractedAudio

        if (!audioBytes.contentEquals(other.audioBytes)) return false
        if (mimeType != other.mimeType) return false
        if (durationMs != other.durationMs) return false
        if (hasAudio != other.hasAudio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + hasAudio.hashCode()
        return result
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
object AudioExtractorUtil {

    /**
     * Rapidly demuxes the AAC/audio track from the selected video into an in-memory byte array
     * without CPU-heavy re-encoding, preserving exact original speech audio.
     */
    suspend fun extractAudioFromVideo(
        context: Context,
        videoUri: Uri,
        maxDurationMs: Long = 90_000L
    ): ExtractedAudio = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "temp_extracted_audio_${System.currentTimeMillis()}.m4a")
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, videoUri, null)

            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            var totalAudioDurationUs = 0L

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    totalAudioDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        format.getLong(MediaFormat.KEY_DURATION)
                    } else 0L
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                Log.w(TAG, "No audio track found in selected video.")
                return@withContext ExtractedAudio(
                    audioBytes = ByteArray(0),
                    mimeType = "audio/mp4",
                    durationMs = 0L,
                    hasAudio = false
                )
            }

            extractor.selectTrack(audioTrackIndex)

            // Setup MediaMuxer for MPEG-4 container (.m4a)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(trackFormat)
            muxer.start()

            val maxBufferSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val size = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (size > 0) size else 256 * 1024
            } else 256 * 1024

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val maxDurationUs = maxDurationMs * 1000L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (maxDurationUs > 0 && bufferInfo.presentationTimeUs > maxDurationUs) {
                    break
                }
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()
            extractor = null

            if (outputFile.exists() && outputFile.length() > 0) {
                // Shrink big audio to 32 kbps speech AAC: much smaller upload, faster captions.
                // If compression fails, use the original audio.
                val bytes = if (outputFile.length() > 300_000L) {
                    val smaller = SpeechAudioCompressor.compress(context, outputFile)
                    if (smaller != null) {
                        try { smaller.readBytes() } finally { smaller.delete() }
                    } else {
                        outputFile.readBytes()
                    }
                } else {
                    outputFile.readBytes()
                }
                // Report the length actually extracted, not the full track length
                val trackMs = if (totalAudioDurationUs > 0) totalAudioDurationUs / 1000L else 15000L
                val durationMs = if (maxDurationMs > 0) minOf(trackMs, maxDurationMs) else trackMs
                Log.d(TAG, "Audio successfully extracted: ${bytes.size} bytes, duration: ${durationMs}ms")
                ExtractedAudio(
                    audioBytes = bytes,
                    mimeType = "audio/mp4",
                    durationMs = durationMs,
                    hasAudio = true
                )
            } else {
                ExtractedAudio(
                    audioBytes = ByteArray(0),
                    mimeType = "audio/mp4",
                    durationMs = 0L,
                    hasAudio = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to demux audio track from video", e)
            // Fallback: read the whole video directly. Keep it small: Firebase AI Logic caps the
            // whole request at 20 MB of inline data and base64 adds ~33% on top of the raw bytes.
            try {
                // input.available() is unreliable for content:// URIs, so ask for the real size first
                val size = context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.use { it.length } ?: -1L
                context.contentResolver.openInputStream(videoUri)?.use { input ->
                    if (size in 1..(8L * 1024 * 1024)) {
                        val videoBytes = input.readBytes()
                        return@withContext ExtractedAudio(
                            audioBytes = videoBytes,
                            mimeType = "video/mp4",
                            durationMs = 15000L,
                            hasAudio = true
                        )
                    }
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback video stream read also failed", fallbackEx)
            }

            ExtractedAudio(
                audioBytes = ByteArray(0),
                mimeType = "audio/mp4",
                durationMs = 0L,
                hasAudio = false
            )
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try { outputFile.delete() } catch (_: Exception) {}
        }
    }
}
