package com.example.data.export

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StyleSpan
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.R
import com.example.data.font.FontManager
import com.example.data.font.SinhalaUnicodeConverter
import com.example.data.model.CaptionFontFamily
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionStyle
import com.example.data.model.FontEncodingMode
import com.google.common.collect.ImmutableList
import java.io.File

/**
 * Burns the captions into an MP4 file with Media3 Transformer.
 *
 * Output keeps the source video's resolution and audio. Captions are drawn as a text overlay
 * with the chosen Sinhala / English fonts. Karaoke word highlighting is not exported yet:
 * each caption shows as one line while it is on screen.
 *
 * Transformer must be created and started on the main thread.
 */
@UnstableApi
object VideoExporter {

    /** Starts an export. Returns the running [Transformer] so the caller can poll progress. */
    fun start(
        context: Context,
        sourceUri: Uri,
        captions: List<CaptionItem>,
        style: CaptionStyle,
        outputFile: File,
        onResult: (Result<File>) -> Unit
    ): Transformer {
        val appContext = context.applicationContext
        val sinhalaTypeface = sinhalaTypefaceFor(appContext, style)
        val englishTypeface = builtinTypeface(appContext, style.englishFontFamily)
        val isLegacyFm = isLegacyFm(appContext, style)

        val overlay = CaptionOverlay(
            captions = captions,
            sinhalaTypeface = sinhalaTypeface,
            englishTypeface = englishTypeface,
            isLegacyFm = isLegacyFm,
            isUppercase = style.isUppercase,
            isBold = style.isBold,
            textColor = parseColor(style.textColorHex, 0xFFFFFFFF.toInt())
        )

        val effects = Effects(
            emptyList<AudioProcessor>(),
            listOf<Effect>(OverlayEffect(ImmutableList.of<TextureOverlay>(overlay)))
        )
        val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
            .setEffects(effects)
            .build()

        val transformer = Transformer.Builder(appContext)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onResult(Result.success(outputFile))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    outputFile.delete()
                    onResult(Result.failure(exportException))
                }
            })
            .build()

        outputFile.parentFile?.mkdirs()
        transformer.start(editedItem, outputFile.absolutePath)
        return transformer
    }

    /** Export progress 0..1. Call from the main thread while the export runs. */
    fun progressOf(transformer: Transformer): Float {
        val holder = ProgressHolder()
        transformer.getProgress(holder)
        return holder.progress / 100f
    }

    private fun isLegacyFm(context: Context, style: CaptionStyle): Boolean = when (style.fontEncodingMode) {
        FontEncodingMode.FM_LEGACY -> true
        FontEncodingMode.UNICODE -> false
        FontEncodingMode.AUTO -> style.customFontId?.let { FontManager.isLegacyFmFont(context, it) } ?: false
    }

    private fun sinhalaTypefaceFor(context: Context, style: CaptionStyle): Typeface {
        val custom = style.customFontId?.let { FontManager.uploadedTypeface(context, it) }
        return custom ?: builtinTypeface(context, style.fontFamily)
    }

    private fun builtinTypeface(context: Context, font: CaptionFontFamily): Typeface {
        val resId = when (font) {
            CaptionFontFamily.ABHAYA_LIBRE -> R.font.abhaya_libre
            CaptionFontFamily.BEBAS_NEUE -> R.font.bebas_neue
            CaptionFontFamily.MONTSERRAT -> R.font.montserrat
            CaptionFontFamily.OUTFIT -> R.font.outfit
            CaptionFontFamily.JAKARTA -> R.font.plus_jakarta_sans
            CaptionFontFamily.NOTO_SINHALA -> R.font.noto_sans_sinhala
            CaptionFontFamily.SYSTEM_BOLD -> null
        }
        return resId?.let { ResourcesCompat.getFont(context, it) } ?: Typeface.DEFAULT_BOLD
    }

    private fun parseColor(hex: String, fallback: Int): Int =
        runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(fallback)

    /** Applies a specific typeface to a span of text. Needed for uploaded fonts. */
    private class TypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) {
            tp.typeface = typeface
        }

        override fun updateMeasureState(tp: TextPaint) {
            tp.typeface = typeface
        }
    }

    /**
     * Draws the caption that is on screen at each moment. Each run of Sinhala or English text
     * uses its own font, the same way the preview does.
     */
    private class CaptionOverlay(
        private val captions: List<CaptionItem>,
        private val sinhalaTypeface: Typeface,
        private val englishTypeface: Typeface,
        private val isLegacyFm: Boolean,
        private val isUppercase: Boolean,
        private val isBold: Boolean,
        private val textColor: Int
    ) : TextOverlay() {

        override fun getText(presentationTimeUs: Long): android.text.SpannableString? {
            val ms = presentationTimeUs / 1000L
            val caption = captions.firstOrNull { ms in it.startMs..it.endMs } ?: return null
            val builder = SpannableStringBuilder()
            val words = SinhalaUnicodeConverter.splitWords(caption.text)
            words.forEachIndexed { index, word ->
                SinhalaUnicodeConverter.formatRuns(word, isLegacyFm, isUppercase).forEach { run ->
                    val start = builder.length
                    builder.append(run.text)
                    builder.setSpan(
                        TypefaceSpan(if (run.isSinhala) sinhalaTypeface else englishTypeface),
                        start,
                        builder.length,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (index != words.lastIndex) builder.append(' ')
            }
            if (isBold) {
                builder.setSpan(StyleSpan(Typeface.BOLD), 0, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            builder.setSpan(ForegroundColorSpan(textColor), 0, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            return android.text.SpannableString(builder)
        }

        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings =
            OverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, -0.8f) // lower part of the frame
                .build()
    }
}
