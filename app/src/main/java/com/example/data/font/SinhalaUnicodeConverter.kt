package com.example.data.font

/**
 * Bidirectional and rendering converter between Sinhala Unicode (U+0D80..U+0DFF)
 * and Sri Lankan Legacy FM / DL font ASCII encodings (e.g. FM-Abhaya, FM-Derana, DL-Araliya).
 *
 * In legacy FM fonts, glyphs for Sinhala characters are placed in standard ASCII character slots.
 * This converter transforms modern Sinhala Unicode into the exact sequence of FM characters,
 * properly handling Kombuwa pre-positioning, complex pillam, and conjuncts (bandi akuru),
 * while preserving English words, numbers, and emojis intact.
 */
object SinhalaUnicodeConverter {

    // Common consonant mapping: Unicode -> FM ASCII
    // Wijesekara / FM standard key positions
    private val CONSONANTS = mapOf(
        'ක' to "l",
        'ඛ' to "L",
        'ග' to ".",
        'ඝ' to ">",
        'ඞ' to "X",
        'ඟ' to ".",
        'ච' to "p",
        'ඡ' to "P",
        'ජ' to "c",
        'ඣ' to "C",
        'ඤ' to "[",
        'ඥ' to "{",
        'ඦ' to "[",
        'ට' to "g",
        'ඨ' to "G",
        'ඩ' to "v",
        'ඪ' to "V",
        'ණ' to "K",
        'ඬ' to "v",
        'ත' to ";",
        'ථ' to ":",
        'ද' to "o",
        'ධ' to "O",
        'න' to "k",
        'ඳ' to "o",
        'ප' to "m",
        'ඵ' to "M",
        'බ' to "n",
        'භ' to "N",
        'ම' to "u",
        'ඹ' to "U",
        'ය' to "h",
        'ර' to "r",
        'ල' to ",",
        'ව' to "j",
        'ශ' to "Y",
        'ෂ' to "I",
        'ස' to "i",
        'හ' to "y",
        'ළ' to "<",
        'ෆ' to "f"
    )

    // Independent vowels mapping: Unicode -> FM ASCII
    private val INDEPENDENT_VOWELS = mapOf(
        "අ" to "w",
        "ආ" to "wd",
        "ඇ" to "we",
        "ඈ" to "wE",
        "ඉ" to "b",
        "ඊ" to "B",
        "උ" to "W",
        "ඌ" to "W!",
        "එ" to "t",
        "ඒ" to "ta",
        "ඓ" to "ft",
        "ඔ" to "T",
        "ඕ" to "Ta",
        "ඖ" to "T!",
        "ඍ" to "RD",
        "ඎ" to "RDD"
    )

    // Special conjuncts / bandi akuru (with zero-width joiner \u200D)
    private val CONJUNCTS = mapOf(
        "ක්‍ර" to "l%",
        "ප්‍ර" to "m%",
        "ත්‍ර" to ";%",
        "ශ්‍ර" to "Y%",
        "බ්‍ර" to "n%",
        "ග්‍ර" to ".%",
        "ද්‍ර" to "o%",
        "ව්‍ර" to "j%",
        "ක්‍ය" to "lH",
        "ත්‍ය" to ";H",
        "ද්‍ය" to "oH",
        "භ්‍ය" to "NH",
        "ම්‍ය" to "uH",
        "ප්‍ය" to "mH",
        "ව්‍ය" to "jH",
        "න්‍ය" to "kH",
        "ඤ්ඤ" to "{"
    )

    private val fmCache = java.util.concurrent.ConcurrentHashMap<String, String>(512)
    private val containsSinhalaCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>(512)
    private val wordRenderCache = java.util.concurrent.ConcurrentHashMap<String, WordRender>(512)

    /**
     * Checks if a character is within the Sinhala Unicode block (U+0D80 to U+0DFF).
     */
    fun isSinhalaChar(c: Char): Boolean {
        return c in '\u0D80'..'\u0DFF' || c == '\u200D' || c == '\u200C'
    }

    /**
     * Checks if a string contains any Sinhala Unicode characters.
     */
    fun containsSinhala(text: String): Boolean {
        containsSinhalaCache[text]?.let { return it }
        val res = text.any { isSinhalaChar(it) }
        if (text.length < 100) containsSinhalaCache[text] = res
        return res
    }

    /**
     * Converts a Sinhala Unicode string into FM / DL ASCII font representation.
     * English words, digits, punctuation, and emojis are preserved intact.
     */
    fun unicodeToFm(text: String): String {
        if (text.isEmpty() || !containsSinhala(text)) return text
        fmCache[text]?.let { return it }

        val result = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            // If non-Sinhala character (e.g. English, digits, spaces, emojis), append directly
            if (!isSinhalaChar(c)) {
                result.append(c)
                i++
                continue
            }

            // Check for multi-character conjuncts first (e.g. ප්‍ර, ශ්‍ර, ත්‍ය)
            var matchedConjunct = false
            for ((conjunct, fmValue) in CONJUNCTS) {
                if (text.startsWith(conjunct, i)) {
                    val nextIdx = i + conjunct.length
                    if (nextIdx < len && isVowelSign(text[nextIdx])) {
                        val pillamChar = text[nextIdx]
                        val rendered = renderConsonantWithPillam(fmValue, pillamChar, text, nextIdx)
                        result.append(rendered.first)
                        i = rendered.second
                    } else {
                        result.append(fmValue)
                        i += conjunct.length
                    }
                    matchedConjunct = true
                    break
                }
            }
            if (matchedConjunct) continue

            // Check for independent vowels (අ, ආ, ඇ, ඉ, etc.)
            var matchedVowel = false
            for ((vowel, fmValue) in INDEPENDENT_VOWELS) {
                if (text.startsWith(vowel, i)) {
                    result.append(fmValue)
                    i += vowel.length
                    matchedVowel = true
                    break
                }
            }
            if (matchedVowel) continue

            // Consonant with potential vowel signs
            val fmConsonant = CONSONANTS[c]
            if (fmConsonant != null) {
                val nextIdx = i + 1
                if (nextIdx < len && isVowelSign(text[nextIdx])) {
                    val pillamChar = text[nextIdx]
                    val rendered = renderConsonantWithPillam(fmConsonant, pillamChar, text, nextIdx)
                    result.append(rendered.first)
                    i = rendered.second
                } else {
                    result.append(fmConsonant)
                    i++
                }
                continue
            }

            // Direct pillam or standalone marks
            when (c) {
                'ං' -> result.append("x")
                'ඃ' -> result.append("H")
                '්' -> result.append("a")
                'ා' -> result.append("d")
                'ැ' -> result.append("e")
                'ෑ' -> result.append("E")
                'ි' -> result.append("s")
                'ී' -> result.append("S")
                'ු' -> result.append("q")
                'ූ' -> result.append("Q")
                'ෘ' -> result.append("D")
                'ෲ' -> result.append("DD")
                'ෙ' -> result.append("f")
                'ේ' -> result.append("fa")
                'ෛ' -> result.append("ff")
                'ො' -> result.append("fd")
                'ෝ' -> result.append("fda")
                'ෞ' -> result.append("f!")
                else -> result.append(c)
            }
            i++
        }

        val formatted = result.toString()
        if (text.length < 250) fmCache[text] = formatted
        return formatted
    }

    private fun isVowelSign(c: Char): Boolean {
        return c in listOf(
            '්', 'ා', 'ැ', 'ෑ', 'ි', 'ී', 'ු', 'ූ', 'ෘ', 'ෲ',
            'ෙ', 'ේ', 'ෛ', 'ො', 'ෝ', 'ෞ', 'ං', 'ඃ'
        )
    }

    /**
     * Handles vowel sign positioning:
     * e.g. kombuwa ('f') must appear BEFORE the consonant in FM fonts.
     * Returns Pair(renderedString, nextIndex)
     */
    private fun renderConsonantWithPillam(
        fmConsonant: String,
        pillam: Char,
        fullText: String,
        pillamIndex: Int
    ): Pair<String, Int> {
        return when (pillam) {
            // Kombuwa ('ෙ') -> 'f' + consonant
            'ෙ' -> Pair("f$fmConsonant", pillamIndex + 1)

            // Kombuwa + al-lakuna ('ේ') -> 'f' + consonant + 'a'
            'ේ' -> Pair("f${fmConsonant}a", pillamIndex + 1)

            // Kombuwa + aela pilla ('ො') -> 'f' + consonant + 'd'
            'ො' -> Pair("f${fmConsonant}d", pillamIndex + 1)

            // Kombuwa + aela pilla + al-lakuna ('ෝ') -> 'f' + consonant + 'da'
            'ෝ' -> Pair("f${fmConsonant}da", pillamIndex + 1)

            // Kombu deka ('ෛ') -> 'ff' + consonant
            'ෛ' -> Pair("ff$fmConsonant", pillamIndex + 1)

            // Kombuwa + gayanukitta ('ෞ') -> 'f' + consonant + '!'
            'ෞ' -> Pair("f$fmConsonant!", pillamIndex + 1)

            // Al-lakuna ('්') -> consonant + 'a'
            '්' -> Pair("${fmConsonant}a", pillamIndex + 1)

            // Aela-pilla ('ා') -> consonant + 'd'
            'ා' -> Pair("${fmConsonant}d", pillamIndex + 1)

            // Keti aeda ('ැ') -> consonant + 'e'
            'ැ' -> Pair("${fmConsonant}e", pillamIndex + 1)

            // Diga aeda ('ෑ') -> consonant + 'E'
            'ෑ' -> Pair("${fmConsonant}E", pillamIndex + 1)

            // Is-pilla ('ි') -> consonant + 's'
            'ි' -> Pair("${fmConsonant}s", pillamIndex + 1)

            // Diga is-pilla ('ී') -> consonant + 'S'
            'ී' -> Pair("${fmConsonant}S", pillamIndex + 1)

            // Paa-pilla ('ු') -> consonant + 'q'
            'ු' -> Pair("${fmConsonant}q", pillamIndex + 1)

            // Diga paa-pilla ('ූ') -> consonant + 'Q'
            'ූ' -> Pair("${fmConsonant}Q", pillamIndex + 1)

            // Gaeta-pilla ('ෘ') -> consonant + 'D'
            'ෘ' -> Pair("${fmConsonant}D", pillamIndex + 1)

            // Anusvaraya ('ං') -> consonant + 'x'
            'ං' -> Pair("${fmConsonant}x", pillamIndex + 1)

            else -> Pair("$fmConsonant$pillam", pillamIndex + 1)
        }
    }

    data class WordRender(
        val displayText: String,
        val isSinhala: Boolean
    )

    /**
     * Prepares a word for rendering. If the word contains Sinhala characters,
     * it is converted to FM glyphs (if isLegacyFm is true). If it is an English word,
     * acronym, or number, it is returned in standard Latin without FM conversion,
     * optionally uppercased if isUppercase is true.
     */
    fun formatWord(rawWord: String, isLegacyFm: Boolean, isUppercase: Boolean): WordRender {
        val cacheKey = "$rawWord|$isLegacyFm|$isUppercase"
        wordRenderCache[cacheKey]?.let { return it }

        val hasSinhala = containsSinhala(rawWord)
        val text = if (hasSinhala) {
            if (isLegacyFm) unicodeToFm(rawWord) else rawWord
        } else {
            if (isUppercase) rawWord.uppercase() else rawWord
        }
        val result = WordRender(displayText = text, isSinhala = hasSinhala)
        if (cacheKey.length < 120) wordRenderCache[cacheKey] = result
        return result
    }

    /**
     * Splits a multi-word caption text into individual words while preserving
     * script identity so English and Sinhala parts can be styled with distinct font families.
     */
    fun splitWords(text: String): List<String> {
        return text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    /** One run of characters that all use the same font: Sinhala or not. */
    data class ScriptRun(val text: String, val isSinhala: Boolean)

    /**
     * Splits a word into runs of Sinhala and non-Sinhala characters, so a token like
     * "AIය" becomes [AI (English font), ය (Sinhala font)]. Digits and punctuation
     * ("123", "!", "?") stay with the run before them, so they never switch fonts.
     */
    fun splitScriptRuns(word: String): List<ScriptRun> {
        val runs = mutableListOf<ScriptRun>()
        for (c in word) {
            val sinhala = isSinhalaChar(c)
            val neutral = !sinhala && !c.isLetter()
            val last = runs.lastOrNull()
            when {
                last == null -> runs.add(ScriptRun(c.toString(), sinhala))
                neutral || last.isSinhala == sinhala -> runs[runs.lastIndex] = last.copy(text = last.text + c)
                else -> runs.add(ScriptRun(c.toString(), sinhala))
            }
        }
        return runs
    }

    /**
     * Like [splitScriptRuns] but also applies FM conversion (Sinhala runs, legacy mode only)
     * and uppercase (English runs only), so the renderer can draw each run in its own font.
     */
    fun formatRuns(word: String, isLegacyFm: Boolean, isUppercase: Boolean): List<ScriptRun> {
        return splitScriptRuns(word).map { run ->
            if (run.isSinhala) {
                run.copy(text = if (isLegacyFm) unicodeToFm(run.text) else run.text)
            } else {
                run.copy(text = if (isUppercase) run.text.uppercase() else run.text)
            }
        }
    }
}
