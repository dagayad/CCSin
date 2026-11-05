package com.example

import com.example.data.font.SinhalaUnicodeConverter
import com.example.data.font.SinhalaUnicodeConverter.ScriptRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SinhalaScriptRunsTest {

    @Test
    fun pureEnglishWordIsOneNonSinhalaRun() {
        assertEquals(listOf(ScriptRun("reels", false)), SinhalaUnicodeConverter.splitScriptRuns("reels"))
    }

    @Test
    fun pureSinhalaWordIsOneSinhalaRun() {
        assertEquals(listOf(ScriptRun("කරගන්න", true)), SinhalaUnicodeConverter.splitScriptRuns("කරගන්න"))
    }

    @Test
    fun mixedWordSplitsIntoEnglishThenSinhala() {
        // "AIය" must not put the English letters in the Sinhala font
        assertEquals(
            listOf(ScriptRun("AI", false), ScriptRun("ය", true)),
            SinhalaUnicodeConverter.splitScriptRuns("AIය")
        )
    }

    @Test
    fun sinhalaThenEnglishSplitsBack() {
        assertEquals(
            listOf(ScriptRun("ය", true), ScriptRun("reels", false)),
            SinhalaUnicodeConverter.splitScriptRuns("යreels")
        )
    }

    @Test
    fun digitsAndPunctuationStayWithPreviousRun() {
        assertEquals(
            listOf(ScriptRun("ය123!", true)),
            SinhalaUnicodeConverter.splitScriptRuns("ය123!")
        )
    }

    @Test
    fun runsRejoinToOriginalWord() {
        val word = "AI tools එක්ක reels"
        val joined = SinhalaUnicodeConverter.splitScriptRuns(word).joinToString("") { it.text }
        assertEquals(word, joined)
    }

    @Test
    fun emptyWordGivesNoRuns() {
        assertTrue(SinhalaUnicodeConverter.splitScriptRuns("").isEmpty())
    }

    @Test
    fun uppercaseAppliesOnlyToEnglishRuns() {
        val runs = SinhalaUnicodeConverter.formatRuns("reelsය", isLegacyFm = false, isUppercase = true)
        assertEquals(listOf(ScriptRun("REELS", false), ScriptRun("ය", true)), runs)
    }

    @Test
    fun sinhalaWordWithoutLegacyModeIsUnchanged() {
        val runs = SinhalaUnicodeConverter.formatRuns("කරගන්න", isLegacyFm = false, isUppercase = false)
        assertEquals(listOf(ScriptRun("කරගන්න", true)), runs)
        assertFalse(runs.first().text.isEmpty())
    }
}
