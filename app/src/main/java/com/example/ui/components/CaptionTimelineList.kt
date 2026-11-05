package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionWord
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NotoSansSinhalaFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CaptionTimelineList(
    captions: List<CaptionItem>,
    currentPlayheadMs: Long,
    onSelectCaption: (CaptionItem) -> Unit,
    onUpdateCaption: (CaptionItem) -> Unit,
    onDeleteCaption: (String) -> Unit,
    onAddCaptionAtPlayhead: () -> Unit,
    onSplitCaption: (CaptionItem, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCaptionId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Subtitles Timeline (${captions.size})",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            OutlinedButton(
                onClick = onAddCaptionAtPlayhead,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("add_caption_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Caption", fontSize = 12.sp, fontFamily = OutfitFontFamily)
            }
        }

        if (captions.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Text(
                    text = "No captions yet. Use AI Auto-Caption or tap 'Add Caption'!",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                itemsIndexed(captions, key = { _, item -> item.id }) { index, item ->
                    val isActive = currentPlayheadMs in item.startMs..item.endMs
                    val isEditing = editingCaptionId == item.id
                    val canSplit = isActive && (item.endMs - item.startMs > 600L)

                    CaptionCard(
                        index = index + 1,
                        item = item,
                        isActive = isActive,
                        isEditing = isEditing,
                        canSplit = canSplit,
                        onTap = onSelectCaption,
                        onStartEdit = { id -> editingCaptionId = id },
                        onCancelEdit = { editingCaptionId = null },
                        onSaveEdit = { updated ->
                            onUpdateCaption(updated)
                            editingCaptionId = null
                        },
                        onDelete = onDeleteCaption,
                        onSplit = { onSplitCaption(item, currentPlayheadMs) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionCard(
    index: Int,
    item: CaptionItem,
    isActive: Boolean,
    isEditing: Boolean,
    canSplit: Boolean,
    onTap: (CaptionItem) -> Unit,
    onStartEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (CaptionItem) -> Unit,
    onDelete: (String) -> Unit,
    onSplit: () -> Unit
) {
    var editedText by remember(item.text, isEditing) { mutableStateOf(item.text) }
    var editedStartStr by remember(item.startMs, isEditing) { mutableStateOf(item.startMs.toString()) }
    var editedEndStr by remember(item.endMs, isEditing) { mutableStateOf(item.endMs.toString()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) StudioSurfaceElevated else StudioSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) NeonCyan else StudioSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onTap(item) }
            .testTag("caption_card_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: index, timestamp badge, and action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .background(if (isActive) NeonCyan else StudioSurfaceVariant, CircleShape)
                    ) {
                        Text(
                            text = "$index",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E1C2E)
                    ) {
                        Text(
                            text = item.formattedRange(),
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            color = if (isActive) NeonCyan else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canSplit) {
                        IconButton(
                            onClick = onSplit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.CallSplit,
                                contentDescription = "Split caption at current time",
                                tint = NeonPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (isEditing) onCancelEdit() else onStartEdit(item.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDelete(item.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!isEditing) {
                // Read-only text display with Noto Sans Sinhala font
                Text(
                    text = item.text,
                    fontFamily = NotoSansSinhalaFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            } else {
                // Edit mode
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Caption Text (Sinhala / English)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = StudioSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editedStartStr,
                        onValueChange = { editedStartStr = it },
                        label = { Text("Start ms", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editedEndStr,
                        onValueChange = { editedEndStr = it },
                        label = { Text("End ms", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val s = editedStartStr.toLongOrNull() ?: item.startMs
                            val e = editedEndStr.toLongOrNull() ?: item.endMs
                            // rebuild word timing
                            val words = rebuildWords(editedText, s, e)
                            onSaveEdit(item.copy(text = editedText, startMs = s, endMs = e, words = words))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonCyan, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
                    }
                }
            }
        }
    }
}

private fun rebuildWords(text: String, startMs: Long, endMs: Long): List<CaptionWord> {
    val tokens = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()
    val span = (endMs - startMs).coerceAtLeast(100L) / tokens.size
    return tokens.mapIndexed { i, tok ->
        val s = startMs + (i * span)
        val e = if (i == tokens.lastIndex) endMs else (s + span)
        CaptionWord(tok, s, e)
    }
}
