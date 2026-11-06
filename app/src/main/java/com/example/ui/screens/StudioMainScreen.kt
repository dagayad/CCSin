package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AiAssistantDialog
import com.example.ui.components.CaptionTimelineList
import com.example.ui.components.ExportDialog
import com.example.ui.components.StyleCustomizerSheet
import com.example.ui.components.VideoPreviewPlayer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun StudioMainScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val captions by viewModel.captions.collectAsState()
    val style by viewModel.captionStyle.collectAsState()
    val currentPlayheadMs by viewModel.currentPlayheadMs.collectAsState()
    val totalDurationMs by viewModel.totalDurationMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val activeTab by viewModel.activeStudioTab.collectAsState()

    val selectedVideoUri by viewModel.selectedVideoUri.collectAsState()
    val selectedVideoName by viewModel.selectedVideoName.collectAsState()
    val selectedVideoResolution by viewModel.selectedVideoResolution.collectAsState()
    val audioProcessingStatus by viewModel.audioProcessingStatus.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()

    val context = LocalContext.current
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.onVideoSelected(uri, context)
        }
    }

    val isAiDialogOpen by viewModel.isAiDialogOpen.collectAsState()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsState()
    val singlishResult by viewModel.singlishResult.collectAsState()
    val viralHookResult by viewModel.viralHookResult.collectAsState()
    val extractedStyleResult by viewModel.extractedStyleResult.collectAsState()

    val projects by viewModel.projects.collectAsState()

    Scaffold(
        topBar = {
            StudioTopBar(
                projectTitle = currentProject?.title ?: "SinCC Studio",
                aspectRatio = currentProject?.aspectRatio ?: "9:16",
                onOpenAi = { viewModel.isAiDialogOpen.value = true },
                onOpenExport = { viewModel.isExportDialogOpen.value = true },
                onOpenProjects = { viewModel.activeStudioTab.value = 2 }
            )
        },
        bottomBar = {
            StudioBottomNavigation(
                activeTab = activeTab,
                onSelectTab = { viewModel.activeStudioTab.value = it }
            )
        },
        containerColor = StudioBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isTabletOrLandscape = screenWidth >= 600.dp || (screenWidth > screenHeight && screenHeight > 420.dp)

            when (activeTab) {
                0 -> {
                    // Studio Editor Tab: Automatically adapts between Split Studio (Tablets/Landscape)
                    // and Proportional Vertical Studio (Compact Phones)
                    if (isTabletOrLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            // Left Pane: Video Player and Controls
                            Column(
                                modifier = Modifier
                                    .weight(0.48f)
                                    .fillMaxHeight()
                                    .padding(end = 6.dp)
                            ) {
                                VideoHeaderBar(
                                    selectedVideoUri = selectedVideoUri,
                                    selectedVideoName = selectedVideoName,
                                    selectedVideoResolution = selectedVideoResolution,
                                    totalDurationMs = totalDurationMs,
                                    detectedLanguage = detectedLanguage,
                                    isAiLoading = isAiLoading,
                                    onPickVideo = {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    onClearVideo = { viewModel.clearVideo() },
                                    onRetranscribe = { viewModel.retranscribeCurrentVideo(context) }
                                )

                                AudioStatusBanner(
                                    audioProcessingStatus = audioProcessingStatus,
                                    isAiLoading = isAiLoading
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                VideoPreviewPlayer(
                                    captions = captions,
                                    style = style,
                                    vmPlayheadMs = currentPlayheadMs,
                                    totalDurationMs = totalDurationMs,
                                    isPlaying = isPlaying,
                                    aspectRatioStr = currentProject?.aspectRatio ?: "9:16",
                                    videoUri = selectedVideoUri,
                                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                                    onSeek = { viewModel.seekTo(it) },
                                    onRestart = { viewModel.restartPlayback() },
                                    onToggleSafeZone = { viewModel.toggleSafeZone() },
                                    onStyleChange = { viewModel.updateStyle(it) }
                                )
                            }

                            // Right Pane: Subtitles Timeline List
                            Column(
                                modifier = Modifier
                                    .weight(0.52f)
                                    .fillMaxHeight()
                                    .padding(start = 6.dp)
                            ) {
                                CaptionTimelineList(
                                    captions = captions,
                                    currentPlayheadMs = currentPlayheadMs,
                                    onSelectCaption = { viewModel.seekTo(it.startMs) },
                                    onUpdateCaption = { viewModel.updateCaption(it) },
                                    onDeleteCaption = { viewModel.deleteCaption(it) },
                                    onAddCaptionAtPlayhead = { viewModel.addCaptionAtPlayhead() },
                                    onSplitCaption = { item, ms -> viewModel.splitCaption(item, ms) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        // Standard Vertical Handheld Phone Layout
                        Column(modifier = Modifier.fillMaxSize()) {
                            VideoHeaderBar(
                                selectedVideoUri = selectedVideoUri,
                                selectedVideoName = selectedVideoName,
                                selectedVideoResolution = selectedVideoResolution,
                                totalDurationMs = totalDurationMs,
                                detectedLanguage = detectedLanguage,
                                isAiLoading = isAiLoading,
                                onPickVideo = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onClearVideo = { viewModel.clearVideo() },
                                onRetranscribe = { viewModel.retranscribeCurrentVideo(context) }
                            )

                            AudioStatusBanner(
                                audioProcessingStatus = audioProcessingStatus,
                                isAiLoading = isAiLoading
                            )

                            VideoPreviewPlayer(
                                captions = captions,
                                style = style,
                                vmPlayheadMs = currentPlayheadMs,
                                totalDurationMs = totalDurationMs,
                                isPlaying = isPlaying,
                                aspectRatioStr = currentProject?.aspectRatio ?: "9:16",
                                videoUri = selectedVideoUri,
                                onPlayPauseToggle = { viewModel.togglePlayPause() },
                                onSeek = { viewModel.seekTo(it) },
                                onRestart = { viewModel.restartPlayback() },
                                onToggleSafeZone = { viewModel.toggleSafeZone() },
                                onStyleChange = { viewModel.updateStyle(it) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            CaptionTimelineList(
                                captions = captions,
                                currentPlayheadMs = currentPlayheadMs,
                                onSelectCaption = { viewModel.seekTo(it.startMs) },
                                onUpdateCaption = { viewModel.updateCaption(it) },
                                onDeleteCaption = { viewModel.deleteCaption(it) },
                                onAddCaptionAtPlayhead = { viewModel.addCaptionAtPlayhead() },
                                onSplitCaption = { item, ms -> viewModel.splitCaption(item, ms) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                1 -> {
                    // Style & Fonts Tab: On wide screens, shows video live preview on left and customizer on right
                    if (isTabletOrLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.44f)
                                    .fillMaxHeight()
                                    .padding(end = 6.dp)
                            ) {
                                VideoPreviewPlayer(
                                    captions = captions,
                                    style = style,
                                    vmPlayheadMs = currentPlayheadMs,
                                    totalDurationMs = totalDurationMs,
                                    isPlaying = isPlaying,
                                    aspectRatioStr = currentProject?.aspectRatio ?: "9:16",
                                    videoUri = selectedVideoUri,
                                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                                    onSeek = { viewModel.seekTo(it) },
                                    onRestart = { viewModel.restartPlayback() },
                                    onToggleSafeZone = { viewModel.toggleSafeZone() },
                                    onStyleChange = { viewModel.updateStyle(it) }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.56f)
                                    .fillMaxHeight()
                                    .padding(start = 6.dp)
                            ) {
                                StyleCustomizerSheet(
                                    style = style,
                                    onStyleChange = { viewModel.updateStyle(it) }
                                )
                            }
                        }
                    } else {
                        StyleCustomizerSheet(
                            style = style,
                            onStyleChange = { viewModel.updateStyle(it) }
                        )
                    }
                }
                2 -> {
                    // Projects Tab: Centered and constrained width for large displays
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProjectsListScreen(
                            projects = projects,
                            currentProjectId = currentProject?.id,
                            onSelectProject = {
                                viewModel.selectProject(it)
                                viewModel.activeStudioTab.value = 0
                            },
                            onCreateProject = { title, ratio, videoUri ->
                                viewModel.createNewProject(title, ratio, videoUri, context)
                                viewModel.activeStudioTab.value = 0
                            },
                            onDeleteProject = { viewModel.deleteProject(it) },
                            modifier = Modifier.widthIn(max = 750.dp)
                        )
                    }
                }
            }
        }
    }

    // AI Assistant Dialog
    AiAssistantDialog(
        isOpen = isAiDialogOpen,
        isLoading = isAiLoading,
        errorMessage = aiErrorMessage,
        onDismiss = { viewModel.isAiDialogOpen.value = false },
        onGenerateCaptions = { script, lang -> viewModel.runAiAutoCaption(script, lang) },
        onConvertSinglish = { viewModel.runSinglishConversion(it) },
        onTranslateCaptions = { viewModel.runTranslation(it) },
        onGenerateViralHooks = { viewModel.runViralHooks(it) },
        onExtractStyleFromScreenshot = { viewModel.runScreenshotStyleExtraction(it) },
        onApplyExtractedStyle = { viewModel.applyExtractedStyle(it) },
        singlishConvertedResult = singlishResult,
        viralHookResult = viralHookResult,
        extractedStyleResult = extractedStyleResult
    )

    // Export Dialog
    ExportDialog(
        isOpen = isExportDialogOpen,
        projectTitle = currentProject?.title ?: "SinCC Reel",
        aspectRatioStr = currentProject?.aspectRatio ?: "9:16",
        captions = captions,
        onDismiss = { viewModel.isExportDialogOpen.value = false },
        sourceVideoUri = selectedVideoUri,
        style = style
    )
}

@Composable
private fun StudioTopBar(
    projectTitle: String,
    aspectRatio: String,
    onOpenAi: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenProjects: () -> Unit
) {
    Surface(
        color = StudioSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, StudioSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Left: Logo and Project Title (Clickable to switch/manage projects)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceVariant)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sincc_logo),
                        contentDescription = "SinCC Logo",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenProjects() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("top_bar_project_switcher")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SinCC",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioSurfaceElevated
                        ) {
                            Text(
                                text = aspectRatio,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Projects",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = projectTitle,
                        fontSize = 11.sp,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Right: AI Tools and Export Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Tools button
                Button(
                    onClick = onOpenAi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .border(1.dp, StudioSurfaceVariant, RoundedCornerShape(10.dp))
                        .testTag("open_ai_tools_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SoftBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Tools", fontSize = 11.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }

                // Export Button
                Button(
                    onClick = onOpenExport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("export_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VideoHeaderBar(
    selectedVideoUri: android.net.Uri?,
    selectedVideoName: String?,
    selectedVideoResolution: String?,
    totalDurationMs: Long,
    detectedLanguage: String?,
    isAiLoading: Boolean,
    onPickVideo: () -> Unit,
    onClearVideo: () -> Unit,
    onRetranscribe: () -> Unit
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (selectedVideoUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = com.example.ui.theme.EmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = selectedVideoName ?: "Custom Video",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                append(selectedVideoResolution ?: "")
                                append(" • ")
                                append("${totalDurationMs / 1000}s")
                                if (detectedLanguage != null) {
                                    append(" • ")
                                    append(detectedLanguage)
                                }
                            },
                            fontSize = 9.sp,
                            color = com.example.ui.theme.EmeraldGreen,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRetranscribe,
                        enabled = !isAiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioSurfaceVariant,
                            contentColor = NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Auto Subtitle", fontSize = 9.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Change", fontSize = 10.sp, fontFamily = OutfitFontFamily)
                    }
                    IconButton(
                        onClick = onClearVideo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Video",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Background: Motion Canvas",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = OutfitFontFamily
                    )
                }
                Button(
                    onClick = onPickVideo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioSurfaceVariant,
                        contentColor = NeonCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("upload_video_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Video", fontSize = 10.sp, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AudioStatusBanner(
    audioProcessingStatus: String?,
    isAiLoading: Boolean
) {
    if (audioProcessingStatus == null) return
    Surface(
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isAiLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = NeonCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = audioProcessingStatus,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StudioBottomNavigation(
    activeTab: Int,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(
        containerColor = StudioSurface,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("studio_bottom_navigation")
    ) {
        NavigationBarItem(
            selected = activeTab == 0,
            onClick = { onSelectTab(0) },
            icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = "Studio Editor") },
            label = { Text("Studio", fontFamily = OutfitFontFamily, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = TextPrimary,
                indicatorColor = SoftBlue.copy(alpha = 0.25f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = activeTab == 1,
            onClick = { onSelectTab(1) },
            icon = { Icon(Icons.Default.Palette, contentDescription = "Styles & Fonts") },
            label = { Text("Style & Fonts", fontFamily = OutfitFontFamily, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = TextPrimary,
                indicatorColor = SoftBlue.copy(alpha = 0.25f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = activeTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "My Projects") },
            label = { Text("Projects", fontFamily = OutfitFontFamily, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = TextPrimary,
                indicatorColor = SoftBlue.copy(alpha = 0.25f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}
