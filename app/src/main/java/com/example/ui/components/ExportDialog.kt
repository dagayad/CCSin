package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CaptionConverters
import com.example.data.model.CaptionItem
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarGold
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
@OptIn(androidx.media3.common.util.UnstableApi::class)
fun ExportDialog(
    isOpen: Boolean,
    projectTitle: String,
    aspectRatioStr: String,
    captions: List<CaptionItem>,
    onDismiss: () -> Unit,
    sourceVideoUri: android.net.Uri? = null,
    style: com.example.data.model.CaptionStyle = com.example.data.model.CaptionStyle()
) {
    if (!isOpen) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedFormat by remember { mutableStateOf("Burned-In Video") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf("60fps") }
    var copiedFeedback by remember { mutableStateOf(false) }

    // Video render simulation state
    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var renderStageText by remember { mutableStateOf("") }
    var isRenderComplete by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(targetValue = renderProgress, label = "renderProgress")

    // Real MP4 export (Media3 Transformer) with captions burned into the frames
    var transformer by remember { mutableStateOf<androidx.media3.transformer.Transformer?>(null) }
    var outputFile by remember { mutableStateOf<java.io.File?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRendering, transformer) {
        val running = transformer ?: return@LaunchedEffect
        while (isRendering) {
            renderProgress = com.example.data.export.VideoExporter.progressOf(running)
            delay(250L)
        }
    }

    val startExport: () -> Unit = {
        val source = sourceVideoUri
        if (source == null) {
            renderError = "Load a video first to export an MP4. SRT/VTT export works without a video."
        } else {
            renderError = null
            isRenderComplete = false
            renderProgress = 0f
            renderStageText = "Exporting MP4 with captions..."
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val file = java.io.File(dir, "SinCC_${System.currentTimeMillis()}.mp4")
            isRendering = true
            transformer = com.example.data.export.VideoExporter.start(context, source, captions, style, file) { result ->
                isRendering = false
                result.fold(
                    onSuccess = {
                        outputFile = it
                        renderProgress = 1f
                        renderStageText = "MP4 ready"
                        isRenderComplete = true
                    },
                    onFailure = { renderError = "Export failed: ${it.message ?: "unknown error"}" }
                )
            }
        }
    }

    // NOTE: Burned-in video export is NOT implemented yet. The old version only faked progress
    // with delays and never wrote a file. The render button stays disabled until a real
    // renderer (e.g. Media3 Transformer) is added. SRT/VTT/text export still works below.

    val formattedContent = remember(selectedFormat, captions) {
        when (selectedFormat) {
            "SRT" -> CaptionConverters.toSrt(captions)
            "VTT" -> CaptionConverters.toVtt(captions)
            else -> CaptionConverters.toSrt(captions)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
            modifier = Modifier
                .widthIn(min = 280.dp, max = 540.dp)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
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
                            Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Studio",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Format selector
                Text("Export Format:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Burned-In Video", "SRT", "VTT").forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedFormat == "Burned-In Video") {
                    // Resolution selector (720p, 1080p, 4K)
                    Text("Video Resolution:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("720p" to "HD", "1080p" to "Full HD", "4K" to "Ultra HD").forEach { (res, label) ->
                            val isSel = selectedResolution == res
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedResolution = res },
                                label = { Text("$res ($label)", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Frame rate selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Frame Rate:", fontSize = 11.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("30fps", "60fps").forEach { fps ->
                                val isSel = selectedFps == fps
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedFps = fps },
                                    label = { Text(fps, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resolution Info Specs Card
                    val pixelDimensions = when (selectedResolution) {
                        "4K" -> if (aspectRatioStr == "9:16") "2160 x 3840" else if (aspectRatioStr == "1:1") "2160 x 2160" else "3840 x 2160"
                        "720p" -> if (aspectRatioStr == "9:16") "720 x 1280" else if (aspectRatioStr == "1:1") "720 x 720" else "1280 x 720"
                        else -> if (aspectRatioStr == "9:16") "1080 x 1920 (Reels/TikTok)" else if (aspectRatioStr == "1:1") "1080 x 1080" else "1920 x 1080"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$selectedResolution Video Specs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = NeonCyan.copy(alpha = 0.2f)) {
                                    Text(selectedFps, fontSize = 9.sp, color = NeonCyan, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Resolution: $pixelDimensions", fontSize = 11.sp, color = TextSecondary)
                            Text("Encoding: H.264 / AAC High-Bitrate with burned-in subtitles", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Video Render Progress area
                    if (isRendering || isRenderComplete) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(renderStageText, fontSize = 11.sp, color = if (isRenderComplete) EmeraldGreen else NeonCyan)
                                Text("${(animatedProgress * 100).toInt()}%", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                color = if (isRenderComplete) EmeraldGreen else NeonCyan,
                                trackColor = StudioSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    renderError?.let { message ->
                        Text(message, fontSize = 11.sp, color = Color(0xFFFF3B30))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Render or Share Action Button
                    if (!isRenderComplete) {
                        Button(
                            onClick = startExport,
                            enabled = !isRendering && sourceVideoUri != null,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("render_video_button")
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isRendering) "Exporting ${(animatedProgress * 100).toInt()}%..." else "Export MP4 with captions",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = startExport,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Re-Render", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val mp4 = outputFile
                                    val sendIntent: Intent = if (mp4 != null && mp4.exists()) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            mp4
                                        )
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    } else {
                                        Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Created with SinCC Studio: $projectTitle")
                                            type = "text/plain"
                                        }
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share SinCC Video"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share / Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // SRT or VTT preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(Color(0xFF09080F), RoundedCornerShape(8.dp))
                            .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = formattedContent.ifBlank { "No captions available to export." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(formattedContent))
                                copiedFeedback = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (copiedFeedback) EmeraldGreen else NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_subtitles_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copiedFeedback) "Copied! ✓" else "Copy Text", fontSize = 12.sp, fontFamily = OutfitFontFamily)
                        }

                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, formattedContent)
                                    putExtra(Intent.EXTRA_TITLE, "$projectTitle ($selectedFormat Subtitles)")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share SinCC Subtitles")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_subtitles_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share File", fontSize = 12.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
