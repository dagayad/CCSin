package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.CaptionFontFamily
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionPreset
import com.example.data.model.CaptionStyle
import com.example.data.model.CaptionWord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

private const val TAG = "CaptionAiService"

data class ViralHookResult(
    val hooks: List<String>,
    val hashtags: List<String>,
    val description: String
)

data class ExtractedStyleResult(
    val style: CaptionStyle,
    val detectedFontCategory: String,
    val styleSummary: String
)

/**
 * All AI calls go straight to the Gemini API through [GeminiClient] (API key based).
 * No Firebase or App Check is needed for these calls.
 */
class CaptionAiService(@Suppress("UNUSED_PARAMETER") private val context: android.content.Context? = null) {

    // Segment timings only. Word-level arrays made the model output several times longer, which
    // was the main reason auto-captions were slow. parseCaptionsJson spreads words evenly inside
    // each segment, which is accurate enough for 3-6 word chunks.
    private val captionsSchema: JSONObject = JSONObject()
        .put("type", "OBJECT")
        .put(
            "properties",
            JSONObject().put(
                "captions",
                JSONObject()
                    .put("type", "ARRAY")
                    .put("description", "Synchronized caption segments in spoken order")
                    .put(
                        "items",
                        JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties",
                                JSONObject()
                                    .put("startMs", JSONObject().put("type", "INTEGER").put("description", "Segment start in milliseconds"))
                                    .put("endMs", JSONObject().put("type", "INTEGER").put("description", "Segment end in milliseconds"))
                                    .put("text", JSONObject().put("type", "STRING").put("description", "Caption text, 3 to 6 words"))
                            )
                            .put("required", JSONArray(listOf("startMs", "endMs", "text")))
                    )
            )
        )
        .put("required", JSONArray(listOf("captions")))

    /**
     * Pulls the first JSON object out of a model reply. Handles ```json fences and any
     * prose around the object. Throws a clear error instead of a random parse failure.
     */
    private fun extractJsonObject(text: String): JSONObject {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end <= start) {
            throw IllegalStateException("AI reply did not contain JSON. Try again.")
        }
        return JSONObject(text.substring(start, end + 1))
    }

    /**
     * Automatically breaks speech, audio notes, or scripts into word-synchronized
     * Sinhala / Singlish / English captions with realistic pacing.
     */
    suspend fun generateCaptionsFromScript(
        script: String,
        targetLanguage: String = "Sinhala"
    ): Result<List<CaptionItem>> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are SinCC Studio, an expert AI video caption engine specialized in Sinhala, Singlish, and English short-form social videos (TikTok, Reels, Shorts).
                Transform the following spoken dialogue, speech audio notes, or script into timed, engaging caption blocks:

                Input Text / Spoken Dialogue:
                $script

                Language Setting / Preference: $targetLanguage

                Crucial Guidelines:
                1. AUTOMATIC LANGUAGE DETECTION:
                   - Automatically detect the language of the spoken text.
                   - If Sinhala Unicode (සිංහල) or Singlish romanized input (e.g. 'ada api katha karanne', 'video eka balanna'): generate authentic, high-accuracy Sinhala Unicode with proper grammar, vowel signs (පිල්ලම්), and conjunct letters (බැඳි අකුරු – ඤ, ඥ, ළු, ෲ, ්‍ර, ්‍ය, ර්‍).
                   - If English: keep clear, modern English.
                   - If Code-switched (bilingual Sinhala + English spoken together): keep the authentic blend as modern Sri Lankan creators speak (e.g. "AI tools එක්ක reels viral කරගන්න").

                2. EXACT-TIME SUBTITLE GENERATION (ZERO DELAY):
                   - Subtitles must align immediately at the start of speech with NO delay or latency.
                   - First caption segment starts at 0ms (or initial speech onset).
                   - Chunks should be punchy: 3 to 6 words per line max for high viewer retention.
                   - Segment startMs and endMs must match when the phrase is spoken.
                   - Segments follow each other in order with no overlap.
            """.trimIndent()

            val jsonText = withTimeout(120_000L) {
                GeminiClient.generate(listOf(GeminiClient.textPart(prompt)), captionsSchema)
            }
            Result.success(parseCaptionsJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption generation timed out", e)
            Result.failure(Exception("AI captioning timed out after 120s. Please try with shorter text."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Caption generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically listens to uploaded video audio, detects the spoken language
     * (Sinhala, English, Singlish/mixed), and generates synchronized captions.
     */
    suspend fun generateCaptionsFromAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): Result<List<CaptionItem>> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are SinCC Studio, an advanced AI speech recognition and video subtitle engine specializing in Sinhala, Singlish, and English short-form social videos (TikTok, Instagram Reels, YouTube Shorts).

                Listen to this audio track carefully and generate exact-time word-synchronized captions:

                TASK & GUIDELINES:
                1. AUTOMATIC LANGUAGE DETECTION:
                   - Accurately detect the spoken language in the audio track.
                   - If Sinhala speech is detected: transcribe directly into authentic, grammatically correct Sinhala Unicode (සිංහල අකුරු) with proper vowel signs (පිල්ලම් - ැ, ෑ, ි, ී, ු, ූ, ෙ, ෛ, ො, ෝ) and conjuncts (බැඳි අකුරු – ඤ, ඥ, ළු, ෲ, ්‍ර, ්‍ය, ර්‍).
                   - If English speech is detected: transcribe into clean, modern English with proper capitalization.
                   - If Code-switched / Mixed (Bilingual Sinhala + English spoken together): keep the authentic blend as spoken (e.g. "AI tools එක්ක reels viral කරගන්න"). Do not transliterate English brand/tech words into phonetic Sinhala if spoken in English.

                2. PRECISE REAL-TIME WORD TIMING (ZERO DELAY):
                   - Start each caption segment at the exact moment speech begins in the audio.
                   - Give segment timings that match when each phrase starts and ends in the audio.
                   - Chunks must be punchy: 3 to 6 words per line max for high mobile video retention.

                Adhere strictly to the JSON schema output.
            """.trimIndent()

            val jsonText = withTimeout(120_000L) {
                GeminiClient.generate(
                    listOf(
                        GeminiClient.inlinePart(audioBytes, mimeType),
                        GeminiClient.textPart(prompt)
                    ),
                    captionsSchema
                )
            }
            Result.success(parseCaptionsJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio caption generation timed out", e)
            Result.failure(Exception("Audio speech recognition timed out after 120s. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio caption generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Translates existing subtitle captions into target language (Sinhala <-> English)
     * while preserving timestamps.
     */
    suspend fun translateCaptions(
        captions: List<CaptionItem>,
        targetLanguage: String
    ): Result<List<CaptionItem>> = withContext(Dispatchers.IO) {
        try {
            val inputPayload = StringBuilder()
            captions.forEachIndexed { i, c ->
                inputPayload.append("Segment $i [${c.startMs}-${c.endMs}ms]: ${c.text}\n")
            }

            val prompt = """
                Translate the following video caption segments into $targetLanguage.
                Maintain the EXACT startMs and endMs timestamps for each segment.

                Segments to translate:
                $inputPayload
            """.trimIndent()

            val jsonText = withTimeout(120_000L) {
                GeminiClient.generate(listOf(GeminiClient.textPart(prompt)), captionsSchema)
            }
            Result.success(parseCaptionsJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Translation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts Singlish input to authentic Sinhala Unicode with proper spell-check
     */
    suspend fun convertSinglishToSinhala(singlishInput: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a Sinhala linguistic expert for SinCC.
                Convert the following Singlish text (Sinhala written in English letters) into accurate, natural Sinhala Unicode (සිංහල අකුරු).
                Correct any spelling mistakes and render proper conjunct characters (බැඳි අකුරු).
                Return ONLY the converted Sinhala text with no explanation, preamble, or quotes.

                Input:
                $singlishInput
            """.trimIndent()

            val text = withTimeout(45_000L) {
                GeminiClient.generate(listOf(GeminiClient.textPart(prompt)))
            }.trim()

            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Could not convert text."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates viral hooks, hashtags, and social descriptions for Sri Lankan creators
     */
    suspend fun generateViralHooks(topic: String): Result<ViralHookResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a viral social media strategist for Sri Lankan TikTok & Instagram creators.
                For a video about "$topic", generate:
                1. 3 powerful, scroll-stopping opening hook lines (mix of Sinhala and Singlish).
                2. 5 high-reach hashtags (including Sri Lankan viral tags).
                3. A short 2-line reel caption with emojis.

                Format as JSON:
                {
                   "hooks": ["Hook 1", "Hook 2", "Hook 3"],
                   "hashtags": ["#tag1", "#tag2", "#tag3", "#tag4", "#tag5"],
                   "description": "caption description"
                }
            """.trimIndent()

            val text = withTimeout(45_000L) {
                GeminiClient.generate(listOf(GeminiClient.textPart(prompt)))
            }
            val json = extractJsonObject(text)

            val hooksArr = json.optJSONArray("hooks")
            val hooks = mutableListOf<String>()
            if (hooksArr != null) {
                for (i in 0 until hooksArr.length()) hooks.add(hooksArr.getString(i))
            }

            val tagsArr = json.optJSONArray("hashtags")
            val tags = mutableListOf<String>()
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) tags.add(tagsArr.getString(i))
            }

            val desc = json.optString("description", "Check out this reel! 🔥 #SinCC #SriLanka")

            Result.success(ViralHookResult(hooks, tags, desc))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyzes a social media screenshot (from TikTok/Reels/Shorts) and extracts/clones
     * the exact subtitle styling and animation parameters.
     */
    suspend fun analyzeStyleFromScreenshot(bitmap: Bitmap): Result<ExtractedStyleResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are SinCC Studio's AI Visual Style Replicator.
                Analyze the video caption/subtitle style shown in this screenshot from TikTok, Instagram Reels, or YouTube Shorts.
                Extract the exact visual style attributes to replicate this caption style in the app:

                Format your response strictly as valid JSON:
                {
                   "primaryTextColorHex": "#FFFFFF",
                   "highlightColorHex": "#FACC15",
                   "backgroundColorHex": "#CC000000",
                   "hasBackgroundBox": true,
                   "hasStroke": true,
                   "hasShadow": true,
                   "isBold": true,
                   "isItalic": false,
                   "isUppercase": false,
                   "fontSizeEstimateSp": 22,
                   "matchedPreset": "HORMOZI_PUNCH",
                   "suggestedFont": "NOTO_SINHALA",
                   "detectedFontCategory": "Bold Headline Sans with Contrast Outline",
                   "styleSummary": "Hormozi style high-energy caption with neon highlight"
                }

                Valid matchedPreset values: STEAL_MY_STYLE, CREATOR_TOP_YELLOW, HORMOZI_PUNCH, MRBEAST_NEON, KARAOKE_NEON, CYBER_PUNK, TIKTOK_CLASSIC, RETRO_SUNSET, MINIMAL_GLASS, ALERT_POP, GOLDEN_CHIC, CLOUD_FLOAT
                Valid suggestedFont values: NOTO_SINHALA, ABHAYA_LIBRE, BEBAS_NEUE, MONTSERRAT, OUTFIT, JAKARTA
                Output ONLY the raw JSON without markdown code fences.
            """.trimIndent()

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)

            val text = withTimeout(120_000L) {
                GeminiClient.generate(
                    listOf(
                        GeminiClient.inlinePart(out.toByteArray(), "image/jpeg"),
                        GeminiClient.textPart(prompt)
                    )
                )
            }
            val json = extractJsonObject(text)

            val textColor = json.optString("primaryTextColorHex", "#FFFFFF")
            val highlightColor = json.optString("highlightColorHex", "#FACC15")
            val bgColor = json.optString("backgroundColorHex", "#CC000000")
            val hasBox = json.optBoolean("hasBackgroundBox", true)
            val hasStroke = json.optBoolean("hasStroke", true)
            val hasShadow = json.optBoolean("hasShadow", true)
            val isBold = json.optBoolean("isBold", true)
            val isItalic = json.optBoolean("isItalic", false)
            val isUpper = json.optBoolean("isUppercase", false)
            val fontSp = json.optDouble("fontSizeEstimateSp", 22.0).toFloat().coerceIn(16f, 32f)

            val preset = runCatching {
                CaptionPreset.valueOf(json.optString("matchedPreset", CaptionPreset.HORMOZI_PUNCH.name))
            }.getOrDefault(CaptionPreset.HORMOZI_PUNCH)

            val font = runCatching {
                CaptionFontFamily.valueOf(json.optString("suggestedFont", CaptionFontFamily.NOTO_SINHALA.name))
            }.getOrDefault(CaptionFontFamily.NOTO_SINHALA)

            val detectedFontCategory = json.optString("detectedFontCategory", "Social Media Creator Sans")
            val styleSummary = json.optString("styleSummary", "Detected Custom Social Media Caption Style")

            val matchedStyle = CaptionStyle(
                preset = preset,
                fontFamily = font,
                fontSizeSp = fontSp,
                textColorHex = textColor,
                highlightColorHex = highlightColor,
                backgroundColorHex = bgColor,
                hasBackgroundBox = hasBox,
                hasStroke = hasStroke,
                hasShadow = hasShadow,
                isBold = isBold,
                isItalic = isItalic,
                isUppercase = isUpper
            )

            Result.success(ExtractedStyleResult(matchedStyle, detectedFontCategory, styleSummary))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze screenshot style", e)
            Result.failure(e)
        }
    }

    private fun parseCaptionsJson(jsonString: String): List<CaptionItem> {
        val root = extractJsonObject(jsonString)
        val array = root.getJSONArray("captions")
        val result = mutableListOf<CaptionItem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val startMs = obj.getLong("startMs")
            val endMs = obj.getLong("endMs")
            val text = obj.getString("text")

            val wordsList = mutableListOf<CaptionWord>()
            val wordsArray = obj.optJSONArray("words")
            if (wordsArray != null) {
                for (j in 0 until wordsArray.length()) {
                    val wObj = wordsArray.getJSONObject(j)
                    wordsList.add(
                        CaptionWord(
                            word = wObj.getString("word"),
                            startMs = wObj.getLong("startMs"),
                            endMs = wObj.getLong("endMs")
                        )
                    )
                }
            } else {
                val tokens = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.isNotEmpty()) {
                    val span = (endMs - startMs).coerceAtLeast(100L) / tokens.size
                    tokens.forEachIndexed { idx, token ->
                        val s = startMs + (idx * span)
                        val e = if (idx == tokens.lastIndex) endMs else (s + span)
                        wordsList.add(CaptionWord(token, s, e))
                    }
                }
            }

            result.add(
                CaptionItem(
                    id = UUID.randomUUID().toString(),
                    startMs = startMs,
                    endMs = endMs,
                    text = text,
                    words = wordsList
                )
            )
        }
        return result
    }
}
