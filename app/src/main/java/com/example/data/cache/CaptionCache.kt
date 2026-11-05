package com.example.data.cache

import android.content.Context
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionWord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Saves auto-generated captions per video, so picking the same video again loads instantly
 * instead of sending the audio to the AI again. Stored in app files; failures are ignored
 * because the cache is only an optimisation.
 */
object CaptionCache {

    private const val DIR = "caption_cache"

    /** Key from the video URI and duration. Changing either means a different video. */
    fun keyFor(videoUri: String, durationMs: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$videoUri|$durationMs".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun load(context: Context, key: String): List<CaptionItem>? = try {
        val file = fileFor(context, key)
        if (!file.exists()) null else parse(file.readText(Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }

    fun save(context: Context, key: String, captions: List<CaptionItem>) {
        try {
            fileFor(context, key).writeText(serialize(captions).toString(), Charsets.UTF_8)
        } catch (_: Exception) {
            // Cache is optional
        }
    }

    private fun fileFor(context: Context, key: String): File =
        File(File(context.filesDir, DIR).apply { mkdirs() }, "$key.json")

    private fun serialize(captions: List<CaptionItem>): JSONArray {
        val array = JSONArray()
        captions.forEach { c ->
            val words = JSONArray()
            c.words.forEach { w ->
                words.put(JSONObject().put("word", w.word).put("startMs", w.startMs).put("endMs", w.endMs))
            }
            array.put(
                JSONObject()
                    .put("startMs", c.startMs)
                    .put("endMs", c.endMs)
                    .put("text", c.text)
                    .put("words", words)
            )
        }
        return array
    }

    private fun parse(text: String): List<CaptionItem> {
        val array = JSONArray(text)
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val wordsArr = obj.getJSONArray("words")
            CaptionItem(
                id = UUID.randomUUID().toString(),
                startMs = obj.getLong("startMs"),
                endMs = obj.getLong("endMs"),
                text = obj.getString("text"),
                words = List(wordsArr.length()) { j ->
                    val w = wordsArr.getJSONObject(j)
                    CaptionWord(w.getString("word"), w.getLong("startMs"), w.getLong("endMs"))
                }
            )
        }
    }
}
