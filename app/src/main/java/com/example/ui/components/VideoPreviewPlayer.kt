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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.TextStyle
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
    currentPlayheadMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    aspectRatioStr: String = "9:16",
    videoUri: android.net.Uri? = null,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onRestart: () -> Unit,
    onToggleSafeZone: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Optimized active caption lookup
    val activeCaption by remember(captions, currentPlayheadMs) {
        derivedStateOf {
            captions.find { currentPlayheadMs in it.startMs..it.endMs }
        }
    }

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
                    .height(responsiveHeight)
                    .aspectRatio(ratio)
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
                if (videoUri != null) {
                    // Real user-uploaded video playback via hardware-accelerated TextureView
                    TextureVideoPlayer(
                        videoUri = videoUri,
                        isPlaying = isPlaying,
                        currentPlayheadMs = currentPlayheadMs,
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
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    cap.words.forEach { wordItem ->
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

                                        Text(
                                            text = "$displayedWord ",
                                            fontFamily = fontToUse,
                                            fontWeight = curWordWeight,
                                            fontStyle = fontStyle,
                                            textDecoration = textDecoration,
                                            fontSize = style.fontSizeSp.sp,
                                            lineHeight = (style.fontSizeSp * 1.08f).sp,
                                            color = curWordColor,
                                            style = TextStyle(shadow = textShadow),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .scale(wordScale)
                                                .padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                val words = remember(cap.text) {
                                    com.example.data.font.SinhalaUnicodeConverter.splitWords(cap.text)
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.widthIn(max = 280.dp)
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

                                        Text(
                                            text = "$displayedWord ",
                                            fontFamily = fontToUse,
                                            fontWeight = wordWeight,
                                            fontStyle = fontStyle,
                                            textDecoration = textDecoration,
                                            fontSize = style.fontSizeSp.sp,
                                            lineHeight = (style.fontSizeSp * 1.08f).sp,
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
                onClick = onRestart,
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

@Composable
private fun TextureVideoPlayer(
    videoUri: Uri,
    isPlaying: Boolean,
    currentPlayheadMs: Long,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }

    DisposableEffect(videoUri) {
        isPrepared = false
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, videoUri)
            mediaPlayer.isLooping = true
            mediaPlayer.setOnPreparedListener {
                isPrepared = true
                if (isPlaying) {
                    it.start()
                }
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.e("TextureVideoPlayer", "Error preparing video", e)
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        try {
            if (isPlaying) {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            } else {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(currentPlayheadMs) {
        if (!isPrepared || isPlaying) return@LaunchedEffect
        try {
            val currentPos = mediaPlayer.currentPosition
            if (kotlin.math.abs(currentPos - currentPlayheadMs) > 400) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(currentPlayheadMs, MediaPlayer.SEEK_CLOSEST)
                } else {
                    mediaPlayer.seekTo(currentPlayheadMs.toInt())
                }
            }
        } catch (_: Exception) {}
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val surface = Surface(st)
                            mediaPlayer.setSurface(surface)
                        } catch (_: Exception) {}
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer.setSurface(null)
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}


