package com.example

import com.example.data.font.SinhalaUnicodeConverter
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testFirebaseInitialization() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        println("PackageName: " + context.packageName)
        val app = SinCcApplication.ensureFirebaseInitialized(context)
        println("FirebaseApp initialized: $app")
        assertNotNull(app)
        assertEquals("cccc-5d0b3", app.options.projectId)
    }

    @Test
    fun testSinhalaDetection() {
        assertTrue(SinhalaUnicodeConverter.containsSinhala("අද අපි කතා කරන්නේ"))
        assertTrue(SinhalaUnicodeConverter.containsSinhala("SinCC AI 🔥 සිංහල"))
        assertFalse(SinhalaUnicodeConverter.containsSinhala("Hello World 123!"))
    }

    @Test
    fun testUnicodeToFmConversion() {
        // "අපි" -> "wms"
        val convertedApi = SinhalaUnicodeConverter.unicodeToFm("අපි")
        assertEquals("wms", convertedApi)

        // "කොහොමද" -> "fldfyduo"
        // 'ක' + 'ො' -> "fld", 'හ' + 'ො' -> "fyd", 'ම' -> "u", 'ද' -> "o"
        val convertedKohomada = SinhalaUnicodeConverter.unicodeToFm("කොහොමද")
        assertEquals("fldfyduo", convertedKohomada)

        // English text and emojis remain untouched
        val mixed = SinhalaUnicodeConverter.unicodeToFm("🔥 SinCC AI අපි")
        assertTrue(mixed.startsWith("🔥 SinCC AI "))
        assertTrue(mixed.endsWith("wms"))
    }

    @Test
    fun testSrtAndVttFormatting() {
        val captions = listOf(
            com.example.data.model.CaptionItem(id = "1", startMs = 1000L, endMs = 3500L, text = "සිංහල Subtitles"),
            com.example.data.model.CaptionItem(id = "2", startMs = 4000L, endMs = 6500L, text = "SinCC AI Video Studio")
        )
        val srt = com.example.data.model.CaptionConverters.toSrt(captions)
        assertTrue(srt.contains("00:00:01,000 --> 00:00:03,500"))
        assertTrue(srt.contains("සිංහල Subtitles"))

        val vtt = com.example.data.model.CaptionConverters.toVtt(captions)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue(vtt.contains("00:00:01.000 --> 00:00:03.500"))
    }

    @Test
    fun testCustomFontModelUnicodeFlags() {
        val sinhalaFont = com.example.data.font.CustomFontModel(
            id = "sinhala_font.ttf",
            name = "Sinhala Custom Font",
            filePath = "/dummy/path/sinhala_font.ttf",
            isSinhalaUnicode = true,
            category = "Sinhala & English Unicode",
            glyphDetails = "Sinhala Unicode & English"
        )
        assertTrue(sinhalaFont.isSinhalaUnicode)
        assertEquals("Sinhala & English Unicode", sinhalaFont.category)
    }
}
