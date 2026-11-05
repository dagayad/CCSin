package com.example.data.font

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.AbhayaLibreFontFamily
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.NotoSansSinhalaFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

private const val TAG = "FontManager"

data class CustomFontModel(
    val id: String,
    val name: String,
    val filePath: String,
    val isBuiltIn: Boolean = false,
    val category: String = "Sinhala Unicode",
    val isSinhalaUnicode: Boolean = true,
    val isEnglishSupported: Boolean = true,
    val isLegacyFmFont: Boolean = false,
    val glyphDetails: String = "Sinhala Unicode (Native ✓)"
)

object FontManager {

    private val customFontCache = mutableMapOf<String, FontFamily>()
    private val customTypefaceCache = mutableMapOf<String, Typeface>()
    private val legacyFmOverrides = mutableMapOf<String, Boolean>()

    fun getFontDir(context: Context): File {
        val dir = File(context.filesDir, "custom_fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Inspects the font's TrueType/OpenType 'cmap' table to reliably determine
     * if the font actually maps the Sinhala Unicode codepoints (U+0D85 for 'අ' and U+0D9A for 'ක')
     * to valid, non-zero glyph indices.
     */
    fun hasSinhalaUnicodeInCmap(file: File): Boolean {
        if (!file.exists() || file.length() < 12) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val sfntVersion = raf.readInt()
                val numTables = raf.readUnsignedShort()
                raf.skipBytes(6) // searchRange, entrySelector, rangeShift

                var cmapOffset = -1L
                for (i in 0 until numTables) {
                    val tag = raf.readInt()
                    val checkSum = raf.readInt()
                    val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                    val length = raf.readInt().toLong() and 0xFFFFFFFFL
                    // 'cmap' tag in ASCII is 0x636D6170
                    if (tag == 0x636D6170) {
                        cmapOffset = offset
                        break
                    }
                }
                if (cmapOffset == -1L) return false

                raf.seek(cmapOffset)
                val version = raf.readUnsignedShort()
                val numSubtables = raf.readUnsignedShort()

                for (i in 0 until numSubtables) {
                    val platformId = raf.readUnsignedShort()
                    val encodingId = raf.readUnsignedShort()
                    val subtableOffset = raf.readInt().toLong() and 0xFFFFFFFFL

                    val currentPos = raf.filePointer
                    raf.seek(cmapOffset + subtableOffset)
                    val format = raf.readUnsignedShort()

                    if (format == 4) {
                        val length = raf.readUnsignedShort()
                        val language = raf.readUnsignedShort()
                        val segCountX2 = raf.readUnsignedShort()
                        val segCount = segCountX2 / 2
                        raf.skipBytes(6) // searchRange, entrySelector, rangeShift

                        val endCodes = IntArray(segCount)
                        for (s in 0 until segCount) {
                            endCodes[s] = raf.readUnsignedShort()
                        }
                        raf.readUnsignedShort() // reservedPad
                        val startCodes = IntArray(segCount)
                        for (s in 0 until segCount) {
                            startCodes[s] = raf.readUnsignedShort()
                        }
                        val idDeltas = IntArray(segCount)
                        for (s in 0 until segCount) {
                            idDeltas[s] = raf.readShort().toInt()
                        }
                        val idRangeOffsetPositions = LongArray(segCount)
                        val idRangeOffsets = IntArray(segCount)
                        for (s in 0 until segCount) {
                            idRangeOffsetPositions[s] = raf.filePointer
                            idRangeOffsets[s] = raf.readUnsignedShort()
                        }

                        // Test Sinhala Unicode codepoint U+0D85 ('අ')
                        val testChar = 0x0D85
                        var foundSegment = -1
                        for (s in 0 until segCount) {
                            if (testChar >= startCodes[s] && testChar <= endCodes[s]) {
                                foundSegment = s
                                break
                            }
                        }

                        if (foundSegment != -1) {
                            val rangeOffset = idRangeOffsets[foundSegment]
                            val delta = idDeltas[foundSegment]
                            var glyphId = 0

                            if (rangeOffset == 0) {
                                glyphId = (testChar + delta) and 0xFFFF
                            } else {
                                val offsetAddr = idRangeOffsetPositions[foundSegment] + rangeOffset + (testChar - startCodes[foundSegment]) * 2
                                if (offsetAddr + 2 <= cmapOffset + subtableOffset + length) {
                                    raf.seek(offsetAddr)
                                    val valAtAddr = raf.readUnsignedShort()
                                    if (valAtAddr != 0) {
                                        glyphId = (valAtAddr + delta) and 0xFFFF
                                    }
                                }
                            }

                            if (glyphId > 0) {
                                return true
                            }
                        }
                    } else if (format == 12) {
                        raf.skipBytes(2) // reserved
                        val length = raf.readInt()
                        val language = raf.readInt()
                        val nGroups = raf.readInt()
                        for (g in 0 until nGroups) {
                            val startCharCode = raf.readInt()
                            val endCharCode = raf.readInt()
                            val startGlyphId = raf.readInt()
                            // U+0D85 is 'අ'
                            if (startCharCode <= 0x0D85 && endCharCode >= 0x0D85) {
                                if (startGlyphId > 0) return true
                            }
                        }
                    }
                    raf.seek(currentPos)
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking font cmap for Sinhala Unicode", e)
            false
        }
    }

    /**
     * Inspects a Typeface and TTF file for Sinhala Unicode and Legacy FM patterns.
     */
    fun analyzeFont(file: File, typeface: Typeface): CustomFontModel {
        val fileName = file.name
        val cleanName = fileName.substringBeforeLast(".").replace("_", " ").capitalizeWords()

        // Notice: We strictly inspect the font's own TrueType/OpenType 'cmap' table!
        // We do NOT use Paint.hasGlyph() because on Android, Minikin falls back to the system's
        // Noto Sans Sinhala font, causing ALL fonts (including FM fonts) to report hasGlyph=true!
        val hasUnicodeSinhala = hasSinhalaUnicodeInCmap(file)

        // If font does NOT have native Sinhala Unicode glyphs in its own cmap, but was uploaded by user
        // in a Sinhala subtitle creator, it is an FM/DL legacy font (like FM-Abhaya, Tharu Digital, DL-Amantha)
        val isLegacyFm = legacyFmOverrides[fileName] ?: (!hasUnicodeSinhala)

        val (category, glyphDetails) = when {
            hasUnicodeSinhala -> "Sinhala Unicode" to "Sinhala Unicode (Native ✓)"
            isLegacyFm -> "Sinhala FM (Auto-Convert)" to "Sinhala FM / DL (Auto-Convert ⚡)"
            else -> "English / Latin" to "English & Latin Glyphs"
        }

        return CustomFontModel(
            id = fileName,
            name = cleanName,
            filePath = file.absolutePath,
            isBuiltIn = false,
            category = category,
            isSinhalaUnicode = hasUnicodeSinhala,
            isEnglishSupported = true,
            isLegacyFmFont = isLegacyFm,
            glyphDetails = glyphDetails
        )
    }

    fun isLegacyFmFont(context: Context, fontId: String): Boolean {
        legacyFmOverrides[fontId]?.let { return it }
        val fontDir = getFontDir(context)
        val file = File(fontDir, fontId)
        if (!file.exists()) return false

        // Strict Cmap inspection: only return false if the font file itself
        // maps U+0D85 ('අ') to a real glyph in its own font tables.
        val hasUnicode = hasSinhalaUnicodeInCmap(file)
        if (hasUnicode) return false

        // If it lacks native Unicode Sinhala glyphs in its cmap, it must use FM conversion
        // for Sinhala text so Sinhala letters render via the legacy font rather than falling back.
        return true
    }

    fun toggleLegacyFmMode(context: Context, fontId: String): Boolean {
        val current = isLegacyFmFont(context, fontId)
        val newMode = !current
        legacyFmOverrides[fontId] = newMode
        return newMode
    }

    fun setLegacyFmOverride(fontId: String, isFm: Boolean) {
        legacyFmOverrides[fontId] = isFm
    }

    /**
     * Resolves Compose FontFamily for a given font identifier or file path.
     */
    fun resolveFontFamily(context: Context, fontId: String): FontFamily {
        return when (fontId.uppercase()) {
            "NOTO_SINHALA", "NOTO SANS SINHALA" -> NotoSansSinhalaFontFamily
            "ABHAYA_LIBRE", "ABHAYA LIBRE SERIF" -> AbhayaLibreFontFamily
            "BEBAS_NEUE", "BEBAS NEUE DISPLAY" -> BebasNeueFontFamily
            "MONTSERRAT", "MONTSERRAT HEAVY" -> MontserratFontFamily
            "OUTFIT", "OUTFIT STUDIO" -> OutfitFontFamily
            "JAKARTA", "PLUS JAKARTA" -> PlusJakartaSansFontFamily
            "SYSTEM_BOLD" -> FontFamily.Default
            else -> {
                // Check if user custom font
                customFontCache[fontId]?.let { return it }
                val fontDir = getFontDir(context)
                val targetFile = File(fontDir, fontId)
                if (targetFile.exists()) {
                    try {
                        val typeface = Typeface.createFromFile(targetFile)
                        customTypefaceCache[fontId] = typeface
                        val family = FontFamily(typeface)
                        customFontCache[fontId] = family
                        return family
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load custom typeface from ${targetFile.absolutePath}", e)
                    }
                }
                // Fallback to Noto Sans Sinhala for best glyph support
                NotoSansSinhalaFontFamily
            }
        }
    }

    /**
     * Imports a user-uploaded .ttf or .otf file into internal storage
     */
    fun importFontFromUri(context: Context, uri: Uri): Result<CustomFontModel> {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(context, uri) ?: "custom_font_${System.currentTimeMillis()}.ttf"
            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(getFontDir(context), sanitizedName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open input stream for URI")

            val typeface = Typeface.createFromFile(targetFile)
            customTypefaceCache[sanitizedName] = typeface
            val fontModel = analyzeFont(targetFile, typeface)

            // Cache Compose FontFamily
            customFontCache[sanitizedName] = FontFamily(typeface)

            Result.success(fontModel)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing font from URI", e)
            Result.failure(e)
        }
    }

    fun listCustomFonts(context: Context): List<CustomFontModel> {
        val dir = getFontDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".ttf", true) || name.endsWith(".otf", true) }
        return files?.map { f ->
            try {
                val tf = customTypefaceCache[f.name] ?: Typeface.createFromFile(f).also { customTypefaceCache[f.name] = it }
                analyzeFont(f, tf)
            } catch (_: Exception) {
                CustomFontModel(
                    id = f.name,
                    name = f.name.substringBeforeLast(".").replace("_", " ").capitalizeWords(),
                    filePath = f.absolutePath,
                    isBuiltIn = false,
                    category = "Custom Font",
                    isSinhalaUnicode = false,
                    isEnglishSupported = true,
                    isLegacyFmFont = true,
                    glyphDetails = "Sinhala FM / DL (Auto-Convert ⚡)"
                )
            }
        } ?: emptyList()
    }

    fun deleteCustomFont(context: Context, fontId: String): Boolean {
        customFontCache.remove(fontId)
        customTypefaceCache.remove(fontId)
        legacyFmOverrides.remove(fontId)
        val file = File(getFontDir(context), fontId)
        return if (file.exists()) file.delete() else false
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
