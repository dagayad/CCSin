package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.font.CustomFontModel
import com.example.data.font.FontManager
import com.example.data.model.CaptionAnimation
import com.example.data.model.CaptionFontFamily
import com.example.data.model.CaptionPosition
import com.example.data.model.CaptionPreset
import com.example.data.model.CaptionStyle
import com.example.data.model.FontEncodingMode
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.SolarGold
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleCustomizerSheet(
    style: CaptionStyle,
    onStyleChange: (CaptionStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Custom fonts state
    val customFonts = remember { mutableStateListOf<CustomFontModel>() }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        customFonts.clear()
        customFonts.addAll(FontManager.listCustomFonts(context))
    }

    // Font upload launcher for .ttf and .otf files
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = FontManager.importFontFromUri(context, uri)
            result.fold(
                onSuccess = { model ->
                    customFonts.removeAll { it.id == model.id }
                    customFonts.add(model)
                    onStyleChange(style.copy(customFontId = model.id))
                    uploadStatusMessage = "Detected: ${model.name} • ${model.glyphDetails}"
                    Toast.makeText(context, "${model.name}: ${model.glyphDetails}", Toast.LENGTH_SHORT).show()
                },
                onFailure = { err ->
                    uploadStatusMessage = "Failed to load font: ${err.localizedMessage}"
                    Toast.makeText(context, "Failed to load font", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. PINNED / LOCKED Top Live Preview Card (Isolated from controls)
        PinnedSubtitlePreviewCard(style = style)

        // 2. SCROLLABLE Controls below it
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Viral Presets Grid
        Text("Viral Caption Presets", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CaptionPreset.values().forEach { preset ->
                val isSelected = style.preset == preset
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF262244) else StudioSurface
                    ),
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) NeonCyan else StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onStyleChange(
                                style.copy(
                                    preset = preset,
                                    highlightColorHex = preset.highlightColor,
                                    textColorHex = preset.textColor,
                                    backgroundColorHex = preset.boxColor,
                                    hasStroke = preset.hasStroke,
                                    fontFamily = preset.defaultFont,
                                    englishFontFamily = preset.defaultEnglishFont,
                                    position = preset.defaultPosition,
                                    verticalOffsetDp = preset.defaultVerticalOffsetDp,
                                    isUppercase = preset.defaultUppercase,
                                    hasBackgroundBox = preset.defaultHasBackgroundBox,
                                    fontSizeSp = preset.defaultFontSizeSp,
                                    animation = preset.defaultAnimation,
                                    customFontId = null
                                )
                            )
                        }
                        .testTag("preset_${preset.name}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = preset.title,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) NeonCyan else TextPrimary
                        )
                        Text(
                            text = preset.previewText,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sinhala Font Encoding Mode Selector Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("font_encoding_mode_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sinhala Font Mode",
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (style.fontEncodingMode == FontEncodingMode.FM_LEGACY) SolarGold.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = when (style.fontEncodingMode) {
                                FontEncodingMode.AUTO -> "Auto ⚡"
                                FontEncodingMode.UNICODE -> "Unicode 🔤"
                                FontEncodingMode.FM_LEGACY -> "FM / DL 🎨"
                            },
                            color = if (style.fontEncodingMode == FontEncodingMode.FM_LEGACY) SolarGold else EmeraldGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "If Sinhala text only renders in phone default font, switch to FM/DL Legacy mode:",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FontEncodingMode.values().forEach { mode ->
                        val isModeSelected = style.fontEncodingMode == mode
                        FilterChip(
                            selected = isModeSelected,
                            onClick = {
                                onStyleChange(style.copy(fontEncodingMode = mode))
                            },
                            label = { Text(mode.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                selectedLabelColor = NeonCyan,
                                containerColor = StudioSurface,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isModeSelected,
                                borderColor = StudioSurfaceVariant,
                                selectedBorderColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sinhala & English Font Packs Section + Custom Font Upload
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Font Packs & Styles", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

            // Upload Custom Font Button (.ttf/.otf)
            OutlinedButton(
                onClick = {
                    fontPickerLauncher.launch(
                        arrayOf(
                            "*/*",
                            "font/*",
                            "application/x-font-ttf",
                            "application/x-font-otf",
                            "application/octet-stream"
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("upload_custom_font_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload Font (.ttf)", fontSize = 11.sp, fontFamily = OutfitFontFamily)
            }
        }

        uploadStatusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldGreen.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(msg, color = EmeraldGreen, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sinhala Font Section
        Text(
            text = "Sinhala Font:",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val sinhalaBuiltIns = listOf(
                CaptionFontFamily.NOTO_SINHALA to "Noto Sans (Sinhala)",
                CaptionFontFamily.ABHAYA_LIBRE to "Abhaya Serif (Sinhala)"
            )
            sinhalaBuiltIns.forEach { (font, label) ->
                val isSelected = style.fontFamily == font && style.customFontId == null
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onStyleChange(style.copy(fontFamily = font, customFontId = null))
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = StudioSurface,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StudioSurfaceVariant,
                        selectedBorderColor = NeonCyan
                    )
                )
            }
        }

        // Uploaded Custom Fonts List
        if (customFonts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Your Uploaded Custom Fonts:", fontSize = 11.sp, color = SolarGold, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                customFonts.forEach { customFont ->
                    val isSelected = style.customFontId == customFont.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else StudioSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else StudioSurfaceVariant
                        ),
                        modifier = Modifier
                            .clickable {
                                onStyleChange(
                                    style.copy(
                                        customFontId = customFont.id,
                                        fontEncodingMode = if (customFont.isLegacyFmFont) FontEncodingMode.FM_LEGACY else FontEncodingMode.UNICODE
                                    )
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.FontDownload,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = customFont.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = when {
                                            customFont.isLegacyFmFont -> SolarGold.copy(alpha = 0.25f)
                                            customFont.isSinhalaUnicode -> EmeraldGreen.copy(alpha = 0.2f)
                                            else -> Color(0x3338BDF8)
                                        }
                                    ) {
                                        Text(
                                            text = when {
                                                customFont.isLegacyFmFont -> "FM / DL ⚡"
                                                customFont.isSinhalaUnicode -> "Unicode ✓"
                                                else -> "Latin / EN"
                                            },
                                            fontSize = 8.sp,
                                            color = when {
                                                customFont.isLegacyFmFont -> SolarGold
                                                customFont.isSinhalaUnicode -> EmeraldGreen
                                                else -> Color(0xFF38BDF8)
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Quick toggle between FM mode and Unicode mode
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = if (customFont.isLegacyFmFont) SolarGold.copy(alpha = 0.3f) else StudioSurfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                val newMode = FontManager.toggleLegacyFmMode(context, customFont.id)
                                                customFonts.clear()
                                                customFonts.addAll(FontManager.listCustomFonts(context))
                                                val targetEncoding = if (newMode) FontEncodingMode.FM_LEGACY else FontEncodingMode.UNICODE
                                                onStyleChange(
                                                    style.copy(
                                                        customFontId = customFont.id,
                                                        fontEncodingMode = targetEncoding
                                                    )
                                                )
                                                Toast.makeText(
                                                    context,
                                                    if (newMode) "⚡ Sinhala FM Auto-Convert: ON" else "Unicode Direct: ON",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (customFont.isLegacyFmFont) "Switch: Uni" else "Switch: FM",
                                            fontSize = 7.5.sp,
                                            color = if (customFont.isLegacyFmFont) SolarGold else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Font",
                                tint = Color(0xFFF87171),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        FontManager.deleteCustomFont(context, customFont.id)
                                        customFonts.remove(customFont)
                                        if (style.customFontId == customFont.id) {
                                             onStyleChange(style.copy(customFontId = null))
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // English Font Pairing Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "English Font Pairing:",
                fontSize = 11.sp,
                color = SoftBlue,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = NeonCyan.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Bilingual Auto-Pair 🛡️",
                    fontSize = 8.5.sp,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        Text(
            text = "English words ('SinCC AI', '2024') automatically use this font so Sinhala legacy fonts don't corrupt them.",
            fontSize = 9.5.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val englishOptions = listOf(
                CaptionFontFamily.MONTSERRAT to "Montserrat (Viral)",
                CaptionFontFamily.BEBAS_NEUE to "Bebas (Impact)",
                CaptionFontFamily.OUTFIT to "Outfit (Modern)",
                CaptionFontFamily.JAKARTA to "Jakarta (Tech)",
                CaptionFontFamily.SYSTEM_BOLD to "System Bold"
            )
            englishOptions.forEach { (font, label) ->
                val isSelected = style.englishFontFamily == font
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onStyleChange(style.copy(englishFontFamily = font))
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarGold.copy(alpha = 0.2f),
                        selectedLabelColor = SolarGold,
                        containerColor = StudioSurface,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StudioSurfaceVariant,
                        selectedBorderColor = SolarGold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text Formatting: Bold, Italic, Underline, Shadow, Stroke
        Text("Text Formatting", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = style.isBold,
                onClick = { onStyleChange(style.copy(isBold = !style.isBold)) },
                leadingIcon = { Icon(Icons.Default.FormatBold, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Bold", fontSize = 11.sp) }
            )
            FilterChip(
                selected = style.isItalic,
                onClick = { onStyleChange(style.copy(isItalic = !style.isItalic)) },
                leadingIcon = { Icon(Icons.Default.FormatItalic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Italic", fontSize = 11.sp) }
            )
            FilterChip(
                selected = style.isUnderline,
                onClick = { onStyleChange(style.copy(isUnderline = !style.isUnderline)) },
                leadingIcon = { Icon(Icons.Default.FormatUnderlined, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Underline", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = style.hasShadow,
                onClick = { onStyleChange(style.copy(hasShadow = !style.hasShadow)) },
                label = { Text("Drop Shadow", fontSize = 11.sp) }
            )
            FilterChip(
                selected = style.hasStroke,
                onClick = { onStyleChange(style.copy(hasStroke = !style.hasStroke)) },
                label = { Text("Outline Stroke", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Font Size Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Font Size", fontSize = 12.sp, color = TextSecondary)
            Text("${style.fontSizeSp.toInt()} sp", fontSize = 12.sp, color = NeonCyan, fontFamily = OutfitFontFamily)
        }
        Slider(
            value = style.fontSizeSp,
            onValueChange = { onStyleChange(style.copy(fontSizeSp = it)) },
            valueRange = 14f..36f,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Base Caption Color Picker
        Text("Base Caption Color", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val textColors = listOf(
                "#FFDE00" to Color(0xFFFFDE00),
                "#FFFFFF" to Color.White,
                "#FACC15" to SolarGold,
                "#A3E635" to Color(0xFFA3E635),
                "#00F0FF" to NeonCyan,
                "#F43F5E" to Color(0xFFF43F5E),
                "#E2E8F0" to Color(0xFFE2E8F0)
            )
            textColors.forEach { (hex, col) ->
                val isSelected = style.textColorHex.equals(hex, ignoreCase = true)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onStyleChange(style.copy(textColorHex = hex)) }
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (hex == "#FFFFFF" || hex == "#FFDE00" || hex == "#FACC15" || hex == "#A3E635") Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Highlight Color Picker
        Text("Active Word Highlight Color", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val colors = listOf(
                "#FFFFFF" to Color.White,
                "#FFDE00" to Color(0xFFFFDE00),
                "#FACC15" to SolarGold,
                "#22C55E" to Color(0xFF22C55E),
                "#00F0FF" to NeonCyan,
                "#F43F5E" to Color(0xFFF43F5E),
                "#A855F7" to NeonPurple
            )
            colors.forEach { (hex, col) ->
                val isSelected = style.highlightColorHex.equals(hex, ignoreCase = true)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onStyleChange(style.copy(highlightColorHex = hex)) }
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (hex == "#FFFFFF" || hex == "#FFDE00" || hex == "#FACC15") Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Position & Vertical Offset
        Text("Screen Position", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CaptionPosition.values().forEach { pos ->
                val isSelected = style.position == pos
                FilterChip(
                    selected = isSelected,
                    onClick = { onStyleChange(style.copy(position = pos)) },
                    label = { Text(pos.label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fine Vertical Offset", fontSize = 12.sp, color = TextSecondary)
            Text("${style.verticalOffsetDp.toInt()} dp", fontSize = 12.sp, color = NeonCyan)
        }
        Slider(
            value = style.verticalOffsetDp,
            onValueChange = { onStyleChange(style.copy(verticalOffsetDp = it)) },
            valueRange = -90f..90f,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Animation Mode
        Text("Animation Mode", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CaptionAnimation.values().forEach { anim ->
                val isSelected = style.animation == anim
                FilterChip(
                    selected = isSelected,
                    onClick = { onStyleChange(style.copy(animation = anim)) },
                    label = { Text(anim.label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggles: Background Box, Uppercase, Safe Zone
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Background Box", fontSize = 13.sp, color = TextPrimary)
                Text("Improves readability over busy videos", fontSize = 10.sp, color = TextSecondary)
            }
            Switch(
                checked = style.hasBackgroundBox,
                onCheckedChange = { onStyleChange(style.copy(hasBackgroundBox = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("UPPERCASE", fontSize = 13.sp, color = TextPrimary)
                Text("Capitalize all English words", fontSize = 10.sp, color = TextSecondary)
            }
            Switch(
                checked = style.isUppercase,
                onCheckedChange = { onStyleChange(style.copy(isUppercase = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("TikTok / Reels Safe Zone", fontSize = 13.sp, color = TextPrimary)
                Text("Overlay platform boundary guide", fontSize = 10.sp, color = TextSecondary)
            }
            Switch(
                checked = style.showSafeZone,
                onCheckedChange = { onStyleChange(style.copy(showSafeZone = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF3B30), checkedTrackColor = Color(0x66FF3B30))
            )
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedSubtitlePreviewCard(
    style: CaptionStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSimulatingKaraoke by remember { mutableStateOf(true) }
    var activeWordIndex by remember { mutableIntStateOf(0) }
    val previewWords = remember(style.preset) {
        if (style.preset == CaptionPreset.STEAL_MY_STYLE) {
            listOf("⚡ සුපිරි", "VIRAL", "සිංහල", "CAPTION", "STYLE", "🔥")
        } else {
            listOf("🔥 අද", "අපි", "කතා", "කරන්නේ", "SinCC", "AI", "ගැන!")
        }
    }

    LaunchedEffect(isSimulatingKaraoke) {
        while (isSimulatingKaraoke) {
            delay(500)
            activeWordIndex = (activeWordIndex + 1) % previewWords.size
        }
    }

    val currentFontFamily = remember(style.fontFamily, style.customFontId) {
        FontManager.resolveFontFamily(context, style.customFontId ?: style.fontFamily.name)
    }
    val currentEnglishFont = remember(style.englishFontFamily) {
        FontManager.resolveFontFamily(context, style.englishFontFamily.name)
    }

    val textColor = remember(style.textColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(style.textColorHex)) }.getOrDefault(Color.White)
    }
    val highlightColor = remember(style.highlightColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(style.highlightColorHex)) }.getOrDefault(Color(0xFFFACC15))
    }
    val boxColor = remember(style.backgroundColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(style.backgroundColorHex)) }.getOrDefault(Color(0xCC000000))
    }
    val textShadow = remember(style.hasShadow, style.hasBackgroundBox) {
        if (style.hasShadow || !style.hasBackgroundBox) {
            Shadow(color = Color.Black.copy(alpha = 0.95f), offset = Offset(2.5f, 2.5f), blurRadius = 5f)
        } else null
    }

    val isCustomFont = style.customFontId != null
    val isLegacyFm = remember(style.customFontId, style.fontEncodingMode) {
        when (style.fontEncodingMode) {
            FontEncodingMode.FM_LEGACY -> true
            FontEncodingMode.UNICODE -> false
            FontEncodingMode.AUTO -> {
                style.customFontId?.let { id ->
                    FontManager.isLegacyFmFont(context, id)
                } ?: false
            }
        }
    }
    val baseWeight = when {
        isCustomFont -> FontWeight.Normal
        style.isBold -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(16.dp))
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .testTag("live_subtitle_preview_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = SoftBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Subtitle Preview (Pinned)",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioSurfaceVariant)
                            .clickable { isSimulatingKaraoke = !isSimulatingKaraoke }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSimulatingKaraoke) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SolarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSimulatingKaraoke) "Simulating" else "Paused",
                            fontFamily = OutfitFontFamily,
                            fontSize = 10.sp,
                            color = SolarGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF090D16)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (style.hasBackgroundBox) boxColor else Color.Transparent,
                        border = if (style.hasStroke) androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.7f)) else null,
                        modifier = Modifier
                            .shadow(if (style.hasBackgroundBox) 6.dp else 0.dp, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            previewWords.forEachIndexed { idx, word ->
                                val isWordActive = isSimulatingKaraoke && (idx == activeWordIndex)
                                val curColor = if (isWordActive) highlightColor else textColor
                                val scale = if (isWordActive) 1.15f else 1.0f
                                val fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal
                                val wordWeight = if (isWordActive) {
                                    if (isCustomFont) baseWeight else FontWeight.Black
                                } else if (style.isBold) {
                                    FontWeight.ExtraBold
                                } else baseWeight
                                val textDecoration = if (style.isUnderline) TextDecoration.Underline else TextDecoration.None

                                val rendered = com.example.data.font.SinhalaUnicodeConverter.formatWord(word, isLegacyFm, style.isUppercase)
                                val wordFont = if (rendered.isSinhala) currentFontFamily else currentEnglishFont

                                Text(
                                    text = "${rendered.displayText} ",
                                    fontFamily = wordFont,
                                    fontSize = style.fontSizeSp.sp,
                                    lineHeight = (style.fontSizeSp * 1.08f).sp,
                                    fontWeight = wordWeight,
                                    fontStyle = fontStyle,
                                    textDecoration = textDecoration,
                                    color = curColor,
                                    style = TextStyle(shadow = textShadow),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .scale(scale)
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



