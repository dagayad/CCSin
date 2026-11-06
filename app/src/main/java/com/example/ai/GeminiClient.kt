package com.example.ai

import android.util.Base64
import com.example.BuildConfig
import com.example.data.settings.ApiKeyStore
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal direct client for the Gemini API (generativelanguage.googleapis.com).
 * Uses the API key from BuildConfig, so no Firebase App Check is involved.
 * Plain HttpURLConnection + org.json: no extra dependencies.
 *
 * Call from a background thread (callers already use Dispatchers.IO).
 */
/** An HTTP error from the Gemini API, carrying the status code so callers can decide to retry. */
class GeminiHttpException(val code: Int, message: String) : IllegalStateException(message)

object GeminiClient {

    /**
     * Models tried in order. If one is busy (503/429) or not available for the key (404),
     * the next one is used. Edit this list to match the models your key can use.
     */
    val MODELS = listOf("gemini-3.8-flash", "gemini-3.8-flash-lite", "gemini-2.5-flash")

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    fun textPart(text: String): JSONObject = JSONObject().put("text", text)

    fun inlinePart(bytes: ByteArray, mimeType: String): JSONObject = JSONObject().put(
        "inline_data",
        JSONObject()
            .put("mime_type", mimeType)
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
    )

    /**
     * Sends one user turn made of [parts] and returns the reply text.
     * [responseSchema] (optional) forces the reply to match an OpenAPI-style schema as JSON.
     * Throws IllegalStateException with the API's error message on failure.
     */
    /**
     * Tries each model in [MODELS]. A busy model gets one quick retry, then the next model is used.
     * A model that is not available for this key (404) is skipped.
     */
    fun generate(
        parts: List<JSONObject>,
        responseSchema: JSONObject? = null,
        timeoutMs: Int = 60_000
    ): String {
        var lastError: GeminiHttpException? = null
        for (model in MODELS) {
            var retries = 0
            while (true) {
                try {
                    return callOnce(model, parts, responseSchema, timeoutMs)
                } catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.code == 404) break
                    val busy = e.code == 429 || e.code == 500 || e.code == 503
                    if (!busy) throw e
                    if (retries >= 1) break
                    Thread.sleep(1500L)
                    retries++
                }
            }
        }
        throw IllegalStateException(
            "AI models දැන් busy (high demand). ටික වෙලාවකින් නැවත try කරන්න.", lastError
        )
    }

    private fun callOnce(
        model: String,
        parts: List<JSONObject>,
        responseSchema: JSONObject?,
        timeoutMs: Int
    ): String {
        // The key the user typed in the app wins. The build-time key is only a fallback.
        val apiKey = ApiKeyStore.userKey().ifBlank { BuildConfig.GEMINI_API_KEY }
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is missing. Enter it in the app.")
        }

        val generationConfig = JSONObject()
        if (responseSchema != null) {
            generationConfig.put("responseMimeType", "application/json")
            generationConfig.put("responseSchema", responseSchema)
        }

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray(parts))
                )
            )
            .put("generationConfig", generationConfig)

        val conn = (URL(BASE_URL + model + ":generateContent").openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", apiKey)

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(raw).getJSONObject("error").getString("message")
                }.getOrDefault(raw.take(300))
                throw GeminiHttpException(code, "Gemini error $code: $message")
            }

            val parsed = JSONObject(raw)
            val partsArray = parsed.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")

            val text = if (partsArray == null) {
                ""
            } else {
                buildString {
                    for (i in 0 until partsArray.length()) append(partsArray.getJSONObject(i).optString("text"))
                }
            }

            if (text.isBlank()) throw IllegalStateException("AI returned an empty response")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
