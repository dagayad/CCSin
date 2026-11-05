package com.example.ui.screens

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ProjectEntity
import com.example.data.model.CaptionConverters
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarGold
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SocialMediaPreset(
    val ratio: String,
    val title: String,
    val subtitle: String,
    val resolution: String,
    val platforms: String,
    val badge: String? = null,
    val icon: ImageVector,
    val frameWidthDp: Int,
    val frameHeightDp: Int
)

@Composable
fun ProjectsListScreen(
    projects: List<ProjectEntity>,
    currentProjectId: String?,
    onSelectProject: (ProjectEntity) -> Unit,
    onCreateProject: (title: String, ratio: String, videoUri: Uri?) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isNewProjectDialogOpen by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Saved Projects",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Manage your video projects & reels",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { isNewProjectDialogOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_new_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Project", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No projects yet",
                        color = TextPrimary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap 'New Project' to pick social media sizes or upload video",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projects, key = { it.id }) { project ->
                    val isSelected = project.id == currentProjectId
                    val count = CaptionConverters.jsonToCaptionList(project.captionsJson).size

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) StudioSurfaceElevated else StudioSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonCyan else StudioSurfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectProject(project) }
                            .testTag("project_item_${project.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = StudioSurfaceVariant) {
                                        Text(
                                            text = project.aspectRatio,
                                            fontSize = 10.sp,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = StudioSurfaceVariant) {
                                        Text(
                                            text = "$count captions",
                                            fontSize = 10.sp,
                                            color = SolarGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${project.durationMs / 1000}s",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { projectToDelete = project },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_project_${project.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete project",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (projectToDelete != null) {
        val proj = projectToDelete!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = {
                Text(
                    text = "Delete Project?",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${proj.title}'? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_delete_project_button")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = StudioSurfaceElevated
        )
    }

    if (isNewProjectDialogOpen) {
        NewProjectDialog(
            onDismiss = { isNewProjectDialogOpen = false },
            onCreate = { title, ratio, videoUri ->
                onCreateProject(title, ratio, videoUri)
                isNewProjectDialogOpen = false
            }
        )
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, ratio: String, videoUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var newTitle by remember { mutableStateOf("") }
    var selectedRatio by remember { mutableStateOf("9:16") }

    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var detectedWidth by remember { mutableStateOf<Int?>(null) }
    var detectedHeight by remember { mutableStateOf<Int?>(null) }
    var detectedDurationMs by remember { mutableStateOf<Long?>(null) }
    var detectedFileName by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedVideoUri = uri
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                var w = wStr?.toIntOrNull() ?: 1080
                var h = hStr?.toIntOrNull() ?: 1920
                val rot = rotStr?.toIntOrNull() ?: 0
                if (rot == 90 || rot == 270) {
                    val tmp = w
                    w = h
                    h = tmp
                }
                detectedWidth = w
                detectedHeight = h
                detectedDurationMs = durStr?.toLongOrNull() ?: 0L

                // Extract filename
                var fName: String? = null
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) fName = cursor.getString(idx)
                        }
                    }
                }
                val cleanName = fName ?: uri.lastPathSegment ?: "Uploaded Video"
                detectedFileName = cleanName

                // Auto-suggest project title from video name if currently empty
                if (newTitle.isBlank()) {
                    newTitle = cleanName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
                }

                // Auto-match best aspect ratio preset
                val aspect = if (h > 0) w.toFloat() / h.toFloat() else 0.5625f
                selectedRatio = when {
                    aspect in 0.5f..0.65f -> "9:16"
                    aspect in 0.72f..0.88f -> "4:5"
                    aspect in 0.95f..1.05f -> "1:1"
                    aspect >= 1.3f -> "16:9"
                    aspect < 0.8f -> "9:16"
                    else -> "16:9"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectsListScreen", "Failed to inspect video", e)
            } finally {
                retriever.release()
            }
        }
    }

    val socialMediaPresets = remember {
        listOf(
            SocialMediaPreset(
                ratio = "9:16",
                title = "Vertical Video (9:16)",
                subtitle = "Optimized for TikTok, Reels & Shorts",
                resolution = "1080 × 1920 px (Full HD)",
                platforms = "TikTok • Instagram Reels • YouTube Shorts",
                badge = "Recommended",
                icon = Icons.Default.Smartphone,
                frameWidthDp = 14,
                frameHeightDp = 24
            ),
            SocialMediaPreset(
                ratio = "16:9",
                title = "Widescreen (16:9)",
                subtitle = "Standard horizontal video format",
                resolution = "1920 × 1080 px (1080p / 4K)",
                platforms = "YouTube • Facebook Video • Desktop",
                badge = null,
                icon = Icons.Default.Tv,
                frameWidthDp = 24,
                frameHeightDp = 14
            ),
            SocialMediaPreset(
                ratio = "1:1",
                title = "Square Post (1:1)",
                subtitle = "Standard square aspect ratio",
                resolution = "1080 × 1080 px (Square)",
                platforms = "Instagram Feed • Facebook • LinkedIn",
                badge = null,
                icon = Icons.Default.CropSquare,
                frameWidthDp = 18,
                frameHeightDp = 18
            ),
            SocialMediaPreset(
                ratio = "4:5",
                title = "Portrait Feed (4:5)",
                subtitle = "Vertical feed with maximum screen area",
                resolution = "1080 × 1350 px (Tall Feed)",
                platforms = "Instagram Mobile Feed • Facebook Feed",
                badge = "Max Feed Area",
                icon = Icons.Default.CropPortrait,
                frameWidthDp = 16,
                frameHeightDp = 20
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceVariant),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 560.dp)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create New Project",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose social media size or auto-detect from video",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Upload Video to Auto-Detect Dimensions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pickedVideoUri != null) NeonCyan.copy(alpha = 0.08f) else StudioSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (pickedVideoUri != null) NeonCyan.copy(alpha = 0.5f) else StudioSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (pickedVideoUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (pickedVideoUri != null) NeonCyan else SolarGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pickedVideoUri != null) "Video Detected & Ready" else "Auto-Detect Video Size",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (pickedVideoUri != null) NeonCyan else TextPrimary
                                )
                            }

                            if (pickedVideoUri != null) {
                                IconButton(
                                    onClick = {
                                        pickedVideoUri = null
                                        detectedWidth = null
                                        detectedHeight = null
                                        detectedDurationMs = null
                                        detectedFileName = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear video",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (pickedVideoUri == null) {
                            Text(
                                text = "Upload your video file (.mp4/.mov) to automatically calculate exact pixel dimensions, duration, and match the best aspect ratio.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioSurfaceVariant,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_video_detect_size_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Video File (.mp4)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text(
                                text = detectedFileName ?: "Selected Video",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = NeonCyan.copy(alpha = 0.2f)) {
                                    Text(
                                        text = "${detectedWidth ?: 0} × ${detectedHeight ?: 0} px",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                val sec = (detectedDurationMs ?: 0L) / 1000
                                Text(
                                    text = "Duration: ${sec / 60}:${String.format(Locale.US, "%02d", sec % 60)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SolarGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-matched to '$selectedRatio' preset",
                                    fontSize = 11.sp,
                                    color = SolarGold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Social Media Size Selection
                Text(
                    text = "Social Media Video Sizes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Select target platform format or keep auto-detected ratio",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    socialMediaPresets.forEach { preset ->
                        val isSelected = selectedRatio == preset.ratio
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.12f) else StudioSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else StudioSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedRatio = preset.ratio }
                                .testTag("ratio_preset_${preset.ratio}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                // Miniature Aspect Ratio Box Indicator
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) NeonCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(preset.frameWidthDp.dp, preset.frameHeightDp.dp)
                                            .border(
                                                1.5.dp,
                                                if (isSelected) NeonCyan else TextSecondary,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = preset.title,
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) NeonCyan else TextPrimary
                                        )

                                        if (preset.badge != null) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeonPurple.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = preset.badge,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFFC084FC),
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = preset.resolution,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )

                                    Text(
                                        text = preset.platforms,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )

                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Project Title
                Text(
                    text = "Project Title",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Travel Reel ${SimpleDateFormat("MMM d", Locale.US).format(Date())}") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            val finalTitle = if (newTitle.isNotBlank()) {
                                newTitle.trim()
                            } else {
                                "Reel ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())}"
                            }
                            onCreate(finalTitle, selectedRatio, pickedVideoUri)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_create_project_button")
                    ) {
                        Icon(
                            if (pickedVideoUri != null) Icons.Default.Movie else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pickedVideoUri != null) "Create & Load" else "Create Project",
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }
            }
        }
    }
}
