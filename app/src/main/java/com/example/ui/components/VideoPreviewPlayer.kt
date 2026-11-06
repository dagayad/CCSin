package com.example.ui.components

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptionAnimation
import com.example.data.model.CaptionFontFamily
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionPosition
import com.example.data.model.CaptionStyle
import com.example.ui.theme.AbhayaLibreFontFamily
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotoSansSinhalaFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.StudioSurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPreviewPlayer(
    captions: List<CaptionItem>,
    style: CaptionStyle,
    vmPlayheadMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    aspectRatioStr: String = "9:16",
    videoUri: android.net.Uri? = null,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onRestart: () -> Unit,
    onToggleSafeZone: () -> Unit,
    isFullscreenMode: Boolean = false,
    onExitFullscreen: () -> Unit = {},
    onPlaybackPosition: (Long) -> Unit = {},
    onStyleChange: (CaptionStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // The video's own position is the playhead while a video is loaded (see TextureVideoPlayer)
    var videoPosMs by remember(videoUri) { mutableStateOf(-1L) }
    var isFullscreen by remember { mutableStateOf(false) }
    var restartNonce by remember { mutableStateOf(0) }
    val currentPlayheadMs = if (videoUri != null && videoPosMs >= 0L) videoPosMs else vmPlayheadMs

    // Optimized active caption lookup
    val activeCaption by remember(captions, currentPlayheadMs) {
        derivedStateOf {
            captions.find { currentPlayheadMs in it.startMs..it.endMs }
        }
    }

    // Fullscreen shows captions bigger, using the user's fullscreen size setting
    val captionSizeScale = if (isFullscreenMode) style.fullscreenCaptionScale else 1f

    val ratio = remember(aspectRatioStr) {
        when (aspectRatioStr) {
            "1:1" -> 1f
            "16:9" -> 16f / 9f
            else -> 9f / 16f
        }
    }

    // Cached colors to avoid reparsing on every tick
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val chosenFont = remember(style.fontFamily, style.customFontId) {
        com.example.data.font.FontManager.resolveFontFamily(context, style.customFontId ?: style.fontFamily.name)
    }
    val englishFont = remember(style.englishFontFamily) {
        com.example.data.font.FontManager.resolveFontFamily(context, style.englishFontFamily.name)
    }



    // Fullscreen: the same preview again, filling the screen. Only one player is alive at a time,
    // so the normal stage stops its video while this dialog is open (no double audio).
    val exitFullscreen: () -> Unit = {
        isFullscreen = false
        onSeek(videoPosMs.coerceAtLeast(0L))
    }
    if (isFullscreen && !isFullscreenMode) {
        Dialog(
            onDismissRequest = exitFullscreen,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VideoPreviewPlayer(
                    captions = captions,
                    style = style,
                    vmPlayheadMs = currentPlayheadMs,
                    totalDurationMs = totalDurationMs,
                    isPlaying = isPlaying,
                    aspectRatioStr = aspectRatioStr,
                    videoUri = videoUri,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onSeek = onSeek,
                    onRestart = onRestart,
                    onToggleSafeZone = onToggleSafeZone,
                    isFullscreenMode = true,
                    onExitFullscreen = exitFullscreen,
                    onPlaybackPosition = { videoPosMs = it },
                    onStyleChange = onStyleChange,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Adaptive container with responsive height calculation
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(if (isFullscreenMode) Modifier.weight(1f) else Modifier)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF09080F))
                .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(20.dp))
        ) {
            // Responsive height and width calculation: prevents horizontal overflow for 16:9 videos
            // and guarantees vertical breathing room for subtitle editors on compact devices.
            val maxAllowedWidth = maxWidth - 16.dp
            val targetHeightFromWidth = if (ratio > 0f) maxAllowedWidth / ratio else 300.dp
            val baseMaxHeight = remember(maxHeight) {
                if (maxHeight < 550.dp && maxHeight > 50.dp) {
                    (maxHeight * 0.40f).coerceIn(160.dp, 230.dp)
                } else if (maxHeight in 550.dp..750.dp) {
                    275.dp
                } else {
                    320.dp
                }
            }
            val responsiveHeight = minOf(baseMaxHeight, targetHeightFromWidth).coerceAtLeast(140.dp)

            Box(
                modifier = Modifier
                    .then(
                        if (isFullscreenMode) Modifier.fillMaxSize()
                        else Modifier.height(responsiveHeight).aspectRatio(ratio)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF0F172A),
                                Color(0xFF090D16)
                            )
                        )
                    )
                    .testTag("video_player_stage")
            ) {
                if (videoUri != null && !isFullscreen) {
                    // Real user-uploaded video playback via hardware-accelerated TextureView
                    TextureVideoPlayer(
                        videoUri = videoUri,
                        isPlaying = isPlaying,
                        seekTargetMs = vmPlayheadMs,
                        seekKey = restartNonce,
                        isMuted = VideoAudioState.isMuted,
                        onPositionChange = { pos ->
                            videoPosMs = pos
                            onPlaybackPosition(pos)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MotionCanvasBackground(isPlaying = isPlaying)
                }

                // Stage Status Badge (Top Start)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0x99000000),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (videoUri != null) "🎬 Video" else "✨ Canvas",
                        fontSize = 9.sp,
                        color = if (videoUri != null) NeonCyan else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Safe Zone Overlay if active
                if (style.showSafeZone) {
                    SafeZoneOverlay()
                }

                // Fullscreen only: caption size and words-per-line controls
                if (isFullscreenMode) {
                    CaptionFullscreenControls(
                        style = style,
                        onStyleChange = onStyleChange,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }

                // Render Live Subtitle Caption Overlay
                activeCaption?.let { cap ->
                    val align = when (style.position) {
                        CaptionPosition.TOP -> Alignment.TopCenter
                        CaptionPosition.CENTER -> Alignment.Center
                        CaptionPosition.BOTTOM -> Alignment.BottomCenter
                    }

                    val fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal
                    val textDecoration = if (style.isUnderline) TextDecoration.Underline else TextDecoration.None

                    val isCustomFont = style.customFontId != null
                    val isLegacyFm = remember(style.customFontId, style.fontEncodingMode) {
                        when (style.fontEncodingMode) {
                            com.example.data.model.FontEncodingMode.FM_LEGACY -> true
                            com.example.data.model.FontEncodingMode.UNICODE -> false
                            com.example.data.model.FontEncodingMode.AUTO -> {
                                style.customFontId?.let { id ->
                                    com.example.data.font.FontManager.isLegacyFmFont(context, id)
                                } ?: false
                            }
                        }
                    }

                    // For custom fonts, prevent Android Minikin from dropping Sinhala glyphs to system fallback
                    // by using a stable Normal weight, and emphasizing active words with scale and color
                    val baseWeight = when {
                        isCustomFont -> FontWeight.Normal
                        style.isBold -> FontWeight.Bold
                        else -> FontWeight.Normal
                    }

                    Box(
                        modifier = Modifier
                            .align(align)
                            .offset { IntOffset(0, style.verticalOffsetDp.dp.roundToPx()) }
                            .padding(horizontal = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (style.hasBackgroundBox) boxColor else Color.Transparent,
                            border = if (style.hasStroke) androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.7f)) else null,
                            modifier = Modifier
                                .shadow(if (style.hasBackgroundBox) 8.dp else 0.dp, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (style.animation == CaptionAnimation.KARAOKE_POP && cap.words.isNotEmpty()) {
                                // Words per line: 0 = auto (one flowing line), otherwise break every N words
                                val wordLines = if (style.wordsPerLine > 0) cap.words.chunked(style.wordsPerLine) else listOf(cap.words)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                wordLines.forEach { lineWords ->
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.widthIn(max = 280.dp * captionSizeScale)
                                ) {
                                    lineWords.forEach { wordItem ->
                                        val isWordActive = currentPlayheadMs in wordItem.startMs..wordItem.endMs
                                        val curWordColor = if (isWordActive) highlightColor else textColor
                                        val wordScale = if (isWordActive) 1.15f else 1.0f
                                        val curWordWeight = if (isWordActive) {
                                            if (isCustomFont) baseWeight else FontWeight.Black
                                        } else if (style.isBold) {
                                            FontWeight.ExtraBold
                                        } else baseWeight

                                        val rawWord = wordItem.word
                                        val formatted = com.example.data.font.SinhalaUnicodeConverter.formatWord(rawWord, isLegacyFm, style.isUppercase)
                                        val fontToUse = if (formatted.isSinhala) chosenFont else englishFont
                                        val displayedWord = formatted.displayText
                                        // Each script run gets its own font, so "AIය" draws AI in English font
                                        val parts = remember(rawWord, isLegacyFm, style.isUppercase) {
                                            com.example.data.font.SinhalaUnicodeConverter.formatRuns(rawWord, isLegacyFm, style.isUppercase)
                                        }

                                        Text(
                                            text = buildAnnotatedString {
                                                parts.forEach { part ->
                                                    pushStyle(SpanStyle(fontFamily = if (part.isSinhala) chosenFont else englishFont))
                                                    append(part.text)
                                                    pop()
                                                }
                                                append(" ")
                                            },
                                            fontFamily = fontToUse,
                                            fontWeight = curWordWeight,
                                            fontStyle = fontStyle,
                                            textDecoration = textDecoration,
                                            fontSize = (style.fontSizeSp * captionSizeScale).sp,
                                            lineHeight = (style.fontSizeSp * 1.08f * captionSizeScale).sp,
                                            color = curWordColor,
                                            style = TextStyle(shadow = textShadow),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .scale(wordScale)
                                                .padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                                }
                                }
                            } else {
                                val words = remember(cap.text) {
                                    com.example.data.font.SinhalaUnicodeConverter.splitWords(cap.text)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.widthIn(max = 280.dp * captionSizeScale)
                                ) {
                                    words.forEach { rawWord ->
                                        val isSinhala = com.example.data.font.SinhalaUnicodeConverter.containsSinhala(rawWord)
                                        val fontToUse = if (isSinhala) chosenFont else englishFont
                                        val displayedWord = if (isSinhala && isLegacyFm) {
                                            com.example.data.font.SinhalaUnicodeConverter.unicodeToFm(rawWord)
                                        } else {
                                            if (style.isUppercase) rawWord.uppercase() else rawWord
                                        }
                                        val wordWeight = if (style.isBold) FontWeight.ExtraBold else baseWeight
                                        val parts = remember(rawWord, isLegacyFm, style.isUppercase) {
                                            com.example.data.font.SinhalaUnicodeConverter.formatRuns(rawWord, isLegacyFm, style.isUppercase)
                                        }

                                        Text(
                                            text = buildAnnotatedString {
                                                parts.forEach { part ->
                                                    pushStyle(SpanStyle(fontFamily = if (part.isSinhala) chosenFont else englishFont))
                                                    append(part.text)
                                                    pop()
                                                }
                                                append(" ")
                                            },
                                            fontFamily = fontToUse,
                                            fontWeight = wordWeight,
                                            fontStyle = fontStyle,
                                            textDecoration = textDecoration,
                                            fontSize = (style.fontSizeSp * captionSizeScale).sp,
                                            lineHeight = (style.fontSizeSp * 1.08f * captionSizeScale).sp,
                                            color = textColor,
                                            style = TextStyle(shadow = textShadow),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Play / Pause Tap Overlay in center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPlayPauseToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0x99000000), CircleShape)
                                .border(1.5.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Playback Timeline Controls Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(38.dp)
                    .background(StudioSurfaceVariant, CircleShape)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    restartNonce++ // forces the video to seek to 0 even if the playhead was already 0
                    onRestart()
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "${formatMs(currentPlayheadMs)} / ${formatMs(totalDurationMs)}",
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.width(8.dp))

            Slider(
                value = currentPlayheadMs.coerceAtMost(totalDurationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..totalDurationMs.coerceAtLeast(1000L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = StudioSurfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("timeline_slider")
            )

            Spacer(modifier = Modifier.width(4.dp))

            if (videoUri != null) {
                IconButton(
                    onClick = { VideoAudioState.isMuted = !VideoAudioState.isMuted },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("mute_video_button")
                ) {
                    Icon(
                        imageVector = if (VideoAudioState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (VideoAudioState.isMuted) "Unmute" else "Mute",
                        tint = if (VideoAudioState.isMuted) Color(0xFFFF3B30) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { if (isFullscreenMode) onExitFullscreen() else isFullscreen = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("fullscreen_video_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreenMode) "Exit fullscreen" else "Fullscreen",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = onToggleSafeZone,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (style.showSafeZone) Color(0x33FF3B30) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .testTag("toggle_safe_zone_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Toggle Safe Zone",
                    tint = if (style.showSafeZone) Color(0xFFFF3B30) else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%01d", m, s, tenths)
}

@Composable
private fun MotionCanvasBackground(isPlaying: Boolean) {
    if (!isPlaying) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.20f),
                            NeonCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "videoMotion")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.20f + (waveOffset * 0.12f)),
                        NeonCyan.copy(alpha = 0.12f + (waveOffset * 0.08f)),
                        Color.Transparent
                    )
                )
            )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 14.dp)
    ) {
        repeat(7) { index ->
            val h = (10 + ((index * 7 + (waveOffset * 18).toInt()) % 15)).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeonCyan.copy(alpha = 0.85f))
            )
        }
    }
}


/** Mute is app-wide, so the choice holds in the normal preview and in fullscreen. */
object VideoAudioState {
    var isMuted by mutableStateOf(false)
}

private fun seekPlayer(mp: MediaPlayer, ms: Long) {
    val target = ms.coerceAtLeast(0L)
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mp.seekTo(target, MediaPlayer.SEEK_CLOSEST)
        } else {
            mp.seekTo(target.toInt())
        }
    } catch (_: Exception) {}
}

/**
 * Plays the uploaded video. The video is the clock while it plays: it reports its position
 * through [onPositionChange], and captions follow that. [seekTargetMs] only seeks the video
 * when it changes (slider drag, restart), so the two clocks never fight each other.
 */
@Composable
private fun TextureVideoPlayer(
    videoUri: Uri,
    isPlaying: Boolean,
    seekTargetMs: Long,
    seekKey: Int,
    isMuted: Boolean,
    onPositionChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember { mutableStateOf(false) }

    // Latest values for callbacks that run after this composition has moved on
    val startAtMs by rememberUpdatedState(seekTargetMs)
    val playingNow by rememberUpdatedState(isPlaying)
    val mutedNow by rememberUpdatedState(isMuted)
    val surfaceRef = remember { java.util.concurrent.atomic.AtomicReference<android.view.Surface?>(null) }

    // One MediaPlayer per video. It is created and released together with this composable,
    // so reopening fullscreen or changing the video never reuses a released player.
    DisposableEffect(videoUri) {
        isPrepared = false
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(context, videoUri)
            mp.isLooping = true
            mp.setOnPreparedListener { p ->
                val vol = if (mutedNow) 0f else 1f
                p.setVolume(vol, vol)
                seekPlayer(p, startAtMs)
                surfaceRef.get()?.let { p.setSurface(it) }
                isPrepared = true
                if (playingNow) p.start()
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e("TextureVideoPlayer", "Error preparing video", e)
        }

        onDispose {
            player = null
            try { mp.setSurface(null) } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        val mp = player ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        try {
            if (isPlaying && !mp.isPlaying) mp.start()
            if (!isPlaying && mp.isPlaying) mp.pause()
        } catch (_: Exception) {}
    }

    LaunchedEffect(seekTargetMs, seekKey, isPrepared) {
        val mp = player ?: return@LaunchedEffect
        if (isPrepared) seekPlayer(mp, seekTargetMs)
    }

    LaunchedEffect(isMuted, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        player?.let { mp -> runCatching { val vol = if (isMuted) 0f else 1f
        mp.setVolume(vol, vol) } }
    }

    // Report the video's real position about 30 times a second while playing, and less often
    // while paused. Compose skips work when the value is unchanged.
    LaunchedEffect(isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        while (isActive) {
            player?.let { mp -> runCatching { onPositionChange(mp.currentPosition.toLong()) } }
            delay(if (playingNow) 33L else 150L)
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        val s = android.view.Surface(st)
                        surfaceRef.getAndSet(s)?.release()
                        player?.let { mp -> runCatching { mp.setSurface(s) } }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        player?.let { mp -> runCatching { mp.setSurface(null) } }
                        surfaceRef.getAndSet(null)?.release()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}

/** Fullscreen-only controls: caption size and words per line. Saved in the project style. */
@Composable
private fun CaptionFullscreenControls(
    style: CaptionStyle,
    onStyleChange: (CaptionStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC000000),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Size ${(style.fullscreenCaptionScale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.width(130.dp)
                )
                IconButton(onClick = {
                    onStyleChange(style.copy(fullscreenCaptionScale = (style.fullscreenCaptionScale - 0.1f).coerceIn(0.5f, 3f)))
                }) { Text("-", color = Color.White, fontSize = 18.sp) }
                IconButton(onClick = {
                    onStyleChange(style.copy(fullscreenCaptionScale = (style.fullscreenCaptionScale + 0.1f).coerceIn(0.5f, 3f)))
                }) { Text("+", color = Color.White, fontSize = 18.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (style.wordsPerLine == 0) "Words/line: Auto" else "Words/line: ${style.wordsPerLine}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.width(130.dp)
                )
                IconButton(onClick = { onStyleChange(style.copy(wordsPerLine = (style.wordsPerLine - 1).coerceAtLeast(0))) }) {
                    Text("-", color = Color.White, fontSize = 18.sp)
                }
                IconButton(onClick = { onStyleChange(style.copy(wordsPerLine = (style.wordsPerLine + 1).coerceAtMost(8))) }) {
                    Text("+", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}
