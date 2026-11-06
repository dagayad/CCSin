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
object GeminiClient {

    /** Change this one line if the model name is not available for your key. */
    const val MODEL = "gemini-2.5-flash"

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
    fun generate(
        parts: List<JSONObject>,
        responseSchema: JSONObject? = null,
        timeoutMs: Int = 60_000
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

        val conn = (URL(BASE_URL + MODEL + ":generateContent").openConnection() as HttpURLConnection)
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
                throw IllegalStateException("Gemini error $code: $message")
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
