package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

object CaptionConverters {

    fun captionListToJson(list: List<CaptionItem>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("startMs", item.startMs)
                put("endMs", item.endMs)
                put("text", item.text)

                val wordsArray = JSONArray()
                for (w in item.words) {
                    val wObj = JSONObject().apply {
                        put("word", w.word)
                        put("startMs", w.startMs)
                        put("endMs", w.endMs)
                    }
                    wordsArray.put(wObj)
                }
                put("words", wordsArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToCaptionList(jsonStr: String): List<CaptionItem> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<CaptionItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", System.currentTimeMillis().toString() + "_$i")
                val startMs = obj.optLong("startMs", 0L)
                val endMs = obj.optLong("endMs", 2000L)
                val text = obj.optString("text", "")

                val words = mutableListOf<CaptionWord>()
                val wordsArray = obj.optJSONArray("words")
                if (wordsArray != null) {
                    for (j in 0 until wordsArray.length()) {
                        val wObj = wordsArray.getJSONObject(j)
                        words.add(
                            CaptionWord(
                                word = wObj.optString("word", ""),
                                startMs = wObj.optLong("startMs", startMs),
                                endMs = wObj.optLong("endMs", endMs)
                            )
                        )
                    }
                } else {
                    val split = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (split.isNotEmpty()) {
                        val durationPerWord = (endMs - startMs).coerceAtLeast(100L) / split.size
                        split.forEachIndexed { idx, word ->
                            val wStart = startMs + (idx * durationPerWord)
                            val wEnd = if (idx == split.lastIndex) endMs else (wStart + durationPerWord)
                            words.add(CaptionWord(word, wStart, wEnd))
                        }
                    }
                }

                list.add(CaptionItem(id, startMs, endMs, text, words))
            }
        } catch (_: Exception) {}
        return list
    }

    fun styleToJson(style: CaptionStyle): String {
        return JSONObject().apply {
            put("preset", style.preset.name)
            put("fontFamily", style.fontFamily.name)
            put("customFontId", style.customFontId ?: "")
            put("englishFontFamily", style.englishFontFamily.name)
            put("fontEncodingMode", style.fontEncodingMode.name)
            put("fontSizeSp", style.fontSizeSp.toDouble())
            put("textColorHex", style.textColorHex)
            put("highlightColorHex", style.highlightColorHex)
            put("backgroundColorHex", style.backgroundColorHex)
            put("hasBackgroundBox", style.hasBackgroundBox)
            put("position", style.position.name)
            put("verticalOffsetDp", style.verticalOffsetDp.toDouble())
            put("animation", style.animation.name)
            put("isBold", style.isBold)
            put("isItalic", style.isItalic)
            put("isUnderline", style.isUnderline)
            put("isUppercase", style.isUppercase)
            put("hasShadow", style.hasShadow)
            put("hasStroke", style.hasStroke)
            put("strokeWidthPx", style.strokeWidthPx.toDouble())
            put("showSafeZone", style.showSafeZone)
        }.toString()
    }

    fun jsonToStyle(jsonStr: String): CaptionStyle {
        if (jsonStr.isBlank()) return CaptionStyle()
        return try {
            val obj = JSONObject(jsonStr)
            val rawCustomFont = obj.optString("customFontId", "")
            CaptionStyle(
                preset = runCatching { CaptionPreset.valueOf(obj.optString("preset", CaptionPreset.HORMOZI_PUNCH.name)) }.getOrDefault(CaptionPreset.HORMOZI_PUNCH),
                fontFamily = runCatching { CaptionFontFamily.valueOf(obj.optString("fontFamily", CaptionFontFamily.NOTO_SINHALA.name)) }.getOrDefault(CaptionFontFamily.NOTO_SINHALA),
                customFontId = if (rawCustomFont.isNotBlank()) rawCustomFont else null,
                englishFontFamily = runCatching { CaptionFontFamily.valueOf(obj.optString("englishFontFamily", CaptionFontFamily.MONTSERRAT.name)) }.getOrDefault(CaptionFontFamily.MONTSERRAT),
                fontEncodingMode = runCatching { FontEncodingMode.valueOf(obj.optString("fontEncodingMode", FontEncodingMode.AUTO.name)) }.getOrDefault(FontEncodingMode.AUTO),
                fontSizeSp = obj.optDouble("fontSizeSp", 22.0).toFloat(),
                textColorHex = obj.optString("textColorHex", "#FFFFFF"),
                highlightColorHex = obj.optString("highlightColorHex", "#FACC15"),
                backgroundColorHex = obj.optString("backgroundColorHex", "#CC000000"),
                hasBackgroundBox = obj.optBoolean("hasBackgroundBox", true),
                position = runCatching { CaptionPosition.valueOf(obj.optString("position", CaptionPosition.BOTTOM.name)) }.getOrDefault(CaptionPosition.BOTTOM),
                verticalOffsetDp = obj.optDouble("verticalOffsetDp", -24.0).toFloat(),
                animation = runCatching { CaptionAnimation.valueOf(obj.optString("animation", CaptionAnimation.KARAOKE_POP.name)) }.getOrDefault(CaptionAnimation.KARAOKE_POP),
                isBold = obj.optBoolean("isBold", true),
                isItalic = obj.optBoolean("isItalic", false),
                isUnderline = obj.optBoolean("isUnderline", false),
                isUppercase = obj.optBoolean("isUppercase", false),
                hasShadow = obj.optBoolean("hasShadow", true),
                hasStroke = obj.optBoolean("hasStroke", true),
                strokeWidthPx = obj.optDouble("strokeWidthPx", 3.0).toFloat(),
                showSafeZone = obj.optBoolean("showSafeZone", false)
            )
        } catch (_: Exception) {
            CaptionStyle()
        }
    }

    /**
     * Standard SRT Subtitle Exporter
     */
    fun toSrt(captions: List<CaptionItem>): String {
        val sb = StringBuilder()
        captions.sortedBy { it.startMs }.forEachIndexed { index, item ->
            sb.append("${index + 1}\n")
            sb.append("${formatSrtTime(item.startMs)} --> ${formatSrtTime(item.endMs)}\n")
            sb.append("${item.text}\n\n")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Standard WebVTT Subtitle Exporter
     */
    fun toVtt(captions: List<CaptionItem>): String {
        val sb = StringBuilder("WEBVTT\n\n")
        captions.sortedBy { it.startMs }.forEachIndexed { index, item ->
            sb.append("${index + 1}\n")
            sb.append("${formatVttTime(item.startMs)} --> ${formatVttTime(item.endMs)}\n")
            sb.append("${item.text}\n\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun formatVttTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}
