package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.ExtractedStyleResult
import com.example.ai.ViralHookResult
import com.example.data.model.CaptionStyle
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotoSansSinhalaFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarGold
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiAssistantDialog(
    isOpen: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onGenerateCaptions: (script: String, lang: String) -> Unit,
    onConvertSinglish: (singlish: String) -> Unit,
    onTranslateCaptions: (targetLang: String) -> Unit,
    onGenerateViralHooks: (topic: String) -> Unit,
    onExtractStyleFromScreenshot: (Bitmap) -> Unit,
    onApplyExtractedStyle: (CaptionStyle) -> Unit,
    singlishConvertedResult: String?,
    viralHookResult: ViralHookResult?,
    extractedStyleResult: ExtractedStyleResult?
) {
    if (!isOpen) return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
            modifier = Modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SinCC AI Suite",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable clean tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = StudioSurfaceVariant,
                    contentColor = NeonCyan,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        "Auto-Caption" to Icons.Default.Mic,
                        "Style Matcher" to Icons.Default.ImageSearch,
                        "Singlish to Sinhala" to Icons.Default.Spellcheck,
                        "Translate" to Icons.Default.Language,
                        "Viral Hooks" to Icons.Default.TrendingUp
                    )
                    tabs.forEachIndexed { idx, (title, icon) ->
                        Tab(
                            selected = selectedTabIndex == idx,
                            onClick = { selectedTabIndex = idx },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(title, fontSize = 11.sp, fontFamily = OutfitFontFamily)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33EF4444),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "SinCC AI is processing...",
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        when (selectedTabIndex) {
                            0 -> AutoCaptionTab(onGenerateCaptions)
                            1 -> ScreenshotStyleMatcherTab(
                                onAnalyze = onExtractStyleFromScreenshot,
                                onApply = onApplyExtractedStyle,
                                extractedResult = extractedStyleResult
                            )
                            2 -> SinglishToSinhalaTab(onConvertSinglish, singlishConvertedResult, clipboardManager)
                            3 -> TranslateTab(onTranslateCaptions)
                            4 -> ViralHooksTab(onGenerateViralHooks, viralHookResult, clipboardManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotStyleMatcherTab(
    onAnalyze: (Bitmap) -> Unit,
    onApply: (CaptionStyle) -> Unit,
    extractedResult: ExtractedStyleResult?
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    selectedBitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (_: Exception) {}
        }
    }

    Column {
        Text(
            text = "Upload a screenshot of any TikTok / Reel / Shorts subtitle style you love:",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Upload Area / Thumbnail
        if (selectedBitmap == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceVariant)
                    .border(1.5.dp, androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f).let { NeonCyan.copy(alpha = 0.5f) }, RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .testTag("upload_screenshot_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap to select screenshot from Gallery", fontFamily = OutfitFontFamily, fontSize = 12.sp, color = TextPrimary)
                    Text("JPG, PNG, WebP supported", fontSize = 10.sp, color = TextSecondary)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected Screenshot",
                    modifier = Modifier
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Change Screenshot", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { selectedBitmap?.let { onAnalyze(it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("analyze_screenshot_submit_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clone Style with AI", fontSize = 11.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Extracted Result Card
        if (extractedResult != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Style Extracted Successfully!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        Surface(shape = RoundedCornerShape(4.dp), color = NeonPurple.copy(alpha = 0.2f)) {
                            Text(extractedResult.style.preset.title, fontSize = 10.sp, color = NeonPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(extractedResult.styleSummary, fontSize = 11.sp, color = TextPrimary)
                    Text("Detected Font: ${extractedResult.detectedFontCategory}", fontSize = 10.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color swatches detected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Colors:", fontSize = 10.sp, color = TextSecondary)
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(runCatching { Color(android.graphics.Color.parseColor(extractedResult.style.textColorHex)) }.getOrDefault(Color.White)))
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(runCatching { Color(android.graphics.Color.parseColor(extractedResult.style.highlightColorHex)) }.getOrDefault(SolarGold)))
                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(runCatching { Color(android.graphics.Color.parseColor(extractedResult.style.backgroundColorHex)) }.getOrDefault(Color.DarkGray)))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onApply(extractedResult.style) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("apply_cloned_style_button")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Cloned Style to My Subtitles", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoCaptionTab(
    onGenerate: (script: String, lang: String) -> Unit
) {
    var scriptInput by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("Auto-Detect") }

    Column {
        Text(
            text = "Enter spoken script, speech transcription, or notes:",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = scriptInput,
            onValueChange = { scriptInput = it },
            placeholder = {
                Text(
                    "e.g. Type or paste your video script here in Sinhala, Singlish, or English...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            },
            minLines = 4,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = StudioSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("script_input_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Quick Test Dialogue (Tap to paste):", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val samples = listOf(
                "Sinhala Tech" to "අද මම ගෙනාවේ Sri Lanka වල trending AI tool එකක්. SinCC එක්ක reels viral කරගන්න පුළුවන්!",
                "Singlish Reel" to "Kandy to Ella train ride eka balanna yaluwane. Scenic tea plantations and misty mountains!",
                "Inspirational" to "ජීවිතේ අමාරු දවස් එන්නේ ඔයාව ශක්තිමත් කරන්න. කිසිම දවසක හීන අත්හරින්න එපා."
            )
            samples.forEach { (label, text) ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioSurfaceVariant,
                    modifier = Modifier.clickable { scriptInput = text }
                ) {
                    Text(label, fontSize = 10.sp, color = NeonCyan, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Language Detection Mode:", fontSize = 11.sp, color = TextSecondary)
            Text("⚡ Zero Delay", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Scrolls sideways instead of squeezing the last chip into a vertical column on small phones
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            listOf("Auto-Detect", "Sinhala", "Singlish", "English").forEach { lang ->
                val isSelected = selectedLang == lang
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLang = lang },
                    label = { Text(lang, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (scriptInput.isNotBlank()) {
                    onGenerate(scriptInput, selectedLang)
                }
            },
            enabled = scriptInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_captions_submit_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Auto-Detect & Generate Exact-Time Subtitles", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SinglishToSinhalaTab(
    onConvert: (String) -> Unit,
    result: String?,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    var singlishText by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Type Singlish (Sinhala using English letters):",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = singlishText,
            onValueChange = { singlishText = it },
            placeholder = { Text("e.g. oya kohomada machan, meka godak watinawa", fontSize = 12.sp) },
            minLines = 3,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { if (singlishText.isNotBlank()) onConvert(singlishText) },
            enabled = singlishText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Spellcheck, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Convert to Sinhala Unicode", fontFamily = OutfitFontFamily)
        }

        if (!result.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sinhala Unicode Result:", fontSize = 11.sp, color = SolarGold)
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(result)) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result,
                        fontFamily = NotoSansSinhalaFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslateTab(
    onTranslate: (targetLang: String) -> Unit
) {
    var targetLang by remember { mutableStateOf("English") }

    Column {
        Text(
            text = "Translate current subtitles while preserving exact millisecond timestamps:",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("English", "Sinhala").forEach { lang ->
                val isSelected = targetLang == lang
                FilterChip(
                    selected = isSelected,
                    onClick = { targetLang = lang },
                    label = { Text(lang, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onTranslate(targetLang) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Translate All Captions", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ViralHooksTab(
    onGenerate: (topic: String) -> Unit,
    result: ViralHookResult?,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    var topic by remember { mutableStateOf("") }

    Column {
        Text("Video Topic / Theme:", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            placeholder = { Text("e.g. AI video editing tools, Ella Sri Lanka trip", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { if (topic.isNotBlank()) onGenerate(topic) },
            enabled = topic.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.TrendingUp, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate Viral Opening Hooks", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
        }

        if (result != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text("Top 3 Opening Hooks:", fontSize = 12.sp, color = SolarGold, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            result.hooks.forEach { hook ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { clipboardManager.setText(AnnotatedString(hook)) }
                ) {
                    Text(
                        text = "• $hook",
                        fontFamily = NotoSansSinhalaFontFamily,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Suggested Hashtags:", fontSize = 12.sp, color = NeonCyan)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.hashtags.joinToString(" "),
                color = NeonCyan,
                fontSize = 12.sp
            )
        }
    }
}
