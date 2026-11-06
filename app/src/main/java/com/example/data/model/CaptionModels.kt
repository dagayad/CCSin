package com.example.data.model

import java.util.UUID

data class CaptionWord(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class CaptionItem(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<CaptionWord> = emptyList()
) {
    fun formatTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val millis = (ms % 1000) / 100
        return String.format("%02d:%02d.%01d", minutes, seconds, millis)
    }

    fun formattedRange(): String = "${formatTimestamp(startMs)} → ${formatTimestamp(endMs)}"
}

enum class CaptionFontFamily(val displayName: String) {
    NOTO_SINHALA("Noto Sans Sinhala"),
    ABHAYA_LIBRE("Abhaya Libre Serif"),
    BEBAS_NEUE("Bebas Neue Display"),
    MONTSERRAT("Montserrat Heavy"),
    OUTFIT("Outfit Studio"),
    JAKARTA("Plus Jakarta"),
    SYSTEM_BOLD("System Bold")
}

enum class CaptionPreset(
    val title: String,
    val previewText: String,
    val highlightColor: String,
    val textColor: String,
    val boxColor: String,
    val hasStroke: Boolean = true,
    val defaultFont: CaptionFontFamily = CaptionFontFamily.NOTO_SINHALA,
    val defaultEnglishFont: CaptionFontFamily = CaptionFontFamily.MONTSERRAT,
    val defaultPosition: CaptionPosition = CaptionPosition.BOTTOM,
    val defaultVerticalOffsetDp: Float = -24f,
    val defaultUppercase: Boolean = false,
    val defaultHasBackgroundBox: Boolean = true,
    val defaultFontSizeSp: Float = 22f,
    val defaultAnimation: CaptionAnimation = CaptionAnimation.KARAOKE_POP
) {
    STEAL_MY_STYLE(
        title = "Steal My Style",
        previewText = "සුපිරි CAPTION STYLE ⚡",
        highlightColor = "#FFFFFF",
        textColor = "#FFDE00",
        boxColor = "#00000000",
        hasStroke = false,
        defaultFont = CaptionFontFamily.NOTO_SINHALA,
        defaultEnglishFont = CaptionFontFamily.BEBAS_NEUE,
        defaultPosition = CaptionPosition.TOP,
        defaultVerticalOffsetDp = 44f,
        defaultUppercase = true,
        defaultHasBackgroundBox = false,
        defaultFontSizeSp = 30f,
        defaultAnimation = CaptionAnimation.KARAOKE_POP
    ),
    CREATOR_TOP_YELLOW(
        title = "Top Yellow Hook",
        previewText = "VIRAL HOOK TOP 👑",
        highlightColor = "#FFE600",
        textColor = "#FFFFFF",
        boxColor = "#00000000",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA,
        defaultEnglishFont = CaptionFontFamily.MONTSERRAT,
        defaultPosition = CaptionPosition.TOP,
        defaultVerticalOffsetDp = 40f,
        defaultUppercase = true,
        defaultHasBackgroundBox = false,
        defaultFontSizeSp = 28f,
        defaultAnimation = CaptionAnimation.KARAOKE_POP
    ),
    HORMOZI_PUNCH(
        title = "Hormozi Punch",
        previewText = "VIRAL HOOK 🔥",
        highlightColor = "#22C55E",
        textColor = "#FACC15",
        boxColor = "#F0000000",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA,
        defaultEnglishFont = CaptionFontFamily.MONTSERRAT,
        defaultPosition = CaptionPosition.BOTTOM,
        defaultVerticalOffsetDp = -24f,
        defaultUppercase = true,
        defaultHasBackgroundBox = true,
        defaultFontSizeSp = 23f
    ),
    MRBEAST_NEON(
        title = "MrBeast Pop",
        previewText = "1,000,000 CASH ⚡",
        highlightColor = "#00F0FF",
        textColor = "#FFFFFF",
        boxColor = "#E6090714",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA,
        defaultEnglishFont = CaptionFontFamily.MONTSERRAT,
        defaultPosition = CaptionPosition.BOTTOM,
        defaultVerticalOffsetDp = -24f,
        defaultUppercase = true,
        defaultHasBackgroundBox = true,
        defaultFontSizeSp = 24f
    ),
    KARAOKE_NEON(
        title = "Karaoke Glow",
        previewText = "සජීවී Karaoke ✨",
        highlightColor = "#FACC15",
        textColor = "#FFFFFF",
        boxColor = "#CC000000",
        hasStroke = false,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    CYBER_PUNK(
        title = "Cyber Pulse",
        previewText = "Futuristic Tech 🚀",
        highlightColor = "#00F0FF",
        textColor = "#E0E7FF",
        boxColor = "#D9110E28",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    TIKTOK_CLASSIC(
        title = "TikTok Bold",
        previewText = "Clean TikTok Sub",
        highlightColor = "#38BDF8",
        textColor = "#FFFFFF",
        boxColor = "#E6000000",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    RETRO_SUNSET(
        title = "Sunset Wave",
        previewText = "Island Vibes 🌴",
        highlightColor = "#FB923C",
        textColor = "#FFF7ED",
        boxColor = "#D9431407",
        hasStroke = false,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    MINIMAL_GLASS(
        title = "Minimal Glass",
        previewText = "Modern & Elegant",
        highlightColor = "#A7F3D0",
        textColor = "#F8FAFC",
        boxColor = "#801E293B",
        hasStroke = false,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    ALERT_POP(
        title = "Red Alert",
        previewText = "STOP SCROLLING ⚠️",
        highlightColor = "#EF4444",
        textColor = "#FEF08A",
        boxColor = "#E618181B",
        hasStroke = true,
        defaultFont = CaptionFontFamily.NOTO_SINHALA
    ),
    GOLDEN_CHIC(
        title = "Golden Lux",
        previewText = "සුවිශේෂී Video 👑",
        highlightColor = "#F59E0B",
        textColor = "#FFFBEB",
        boxColor = "#E6291E03",
        hasStroke = false,
        defaultFont = CaptionFontFamily.ABHAYA_LIBRE
    ),
    CLOUD_FLOAT(
        title = "Floating Cloud",
        previewText = "Soft Aesthetic ☁️",
        highlightColor = "#C084FC",
        textColor = "#FFFFFF",
        boxColor = "#661E1B4B",
        hasStroke = false,
        defaultFont = CaptionFontFamily.ABHAYA_LIBRE
    )
}

enum class CaptionPosition(val label: String) {
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom")
}

enum class CaptionAnimation(val label: String) {
    KARAOKE_POP("Karaoke Word Glow"),
    WORD_BOUNCE("Pop Zoom"),
    FADE_IN("Smooth Fade"),
    NONE("Static")
}

enum class FontEncodingMode(val label: String, val description: String) {
    AUTO("Auto-Detect ⚡", "Auto-detects based on font glyphs"),
    UNICODE("Sinhala Unicode 🔤", "Direct Unicode (Noto, Abhaya, Bhashitha)"),
    FM_LEGACY("FM / DL Legacy 🎨", "Converts to FM glyphs (FMAbhaya, DL-Amantha)")
}

data class CaptionStyle(
    val preset: CaptionPreset = CaptionPreset.HORMOZI_PUNCH,
    val fontFamily: CaptionFontFamily = CaptionFontFamily.NOTO_SINHALA,
    val customFontId: String? = null,
    val englishFontFamily: CaptionFontFamily = CaptionFontFamily.MONTSERRAT,
    val fontEncodingMode: FontEncodingMode = FontEncodingMode.AUTO,
    val fontSizeSp: Float = 22f,
    val textColorHex: String = "#FFFFFF",
    val highlightColorHex: String = "#FACC15",
    val backgroundColorHex: String = "#CC000000",
    val hasBackgroundBox: Boolean = true,
    val position: CaptionPosition = CaptionPosition.BOTTOM,
    val verticalOffsetDp: Float = -24f,
    val animation: CaptionAnimation = CaptionAnimation.KARAOKE_POP,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isUppercase: Boolean = false,
    val hasShadow: Boolean = true,
    val hasStroke: Boolean = true,
    val strokeWidthPx: Float = 3f,
    val showSafeZone: Boolean = false,
    // Fullscreen-only size multiplier (1.0 = same as preview) and words per line (0 = auto)
    val fullscreenCaptionScale: Float = 1.8f,
    val wordsPerLine: Int = 0
)
