package com.example.data.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Re-encodes speech audio to a small AAC file (mono-quality speech at 32 kbps).
 * Smaller upload = faster auto-caption. Speech stays clear at this bitrate.
 * Returns null on failure so the caller can fall back to the original audio.
 */
@UnstableApi
object SpeechAudioCompressor {

    private const val SPEECH_BITRATE = 32_000

    suspend fun compress(context: Context, input: File): File? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<File?> { cont ->
            val output = File(context.cacheDir, "speech_${System.currentTimeMillis()}.m4a")

            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedAudioEncoderSettings(
                    AudioEncoderSettings.Builder().setBitrate(SPEECH_BITRATE).build()
                )
                .build()

            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(output)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        output.delete()
                        if (cont.isActive) cont.resume(null)
                    }
                })
                .build()

            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setRemoveVideo(true)
                .build()

            transformer.start(item, output.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
    }
}
