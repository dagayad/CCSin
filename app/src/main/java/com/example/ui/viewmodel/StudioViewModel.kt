package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CaptionAiService
import com.example.ai.ViralHookResult
import com.example.data.local.ProjectEntity
import com.example.data.model.CaptionConverters
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionStyle
import com.example.data.model.CaptionWord
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val aiService = CaptionAiService(application)

    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    private val _captions = MutableStateFlow<List<CaptionItem>>(emptyList())
    val captions: StateFlow<List<CaptionItem>> = _captions.asStateFlow()

    private val _captionStyle = MutableStateFlow(CaptionStyle())
    val captionStyle: StateFlow<CaptionStyle> = _captionStyle.asStateFlow()

    private val _currentPlayheadMs = MutableStateFlow(0L)
    val currentPlayheadMs: StateFlow<Long> = _currentPlayheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(15000L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    // Video Upload State
    private val _selectedVideoUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedVideoUri: StateFlow<android.net.Uri?> = _selectedVideoUri.asStateFlow()

    private val _selectedVideoName = MutableStateFlow<String?>(null)
    val selectedVideoName: StateFlow<String?> = _selectedVideoName.asStateFlow()

    private val _selectedVideoResolution = MutableStateFlow<String?>(null)
    val selectedVideoResolution: StateFlow<String?> = _selectedVideoResolution.asStateFlow()

    // AI state
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _audioProcessingStatus = MutableStateFlow<String?>(null)
    val audioProcessingStatus: StateFlow<String?> = _audioProcessingStatus.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<String?>(null)
    val detectedLanguage: StateFlow<String?> = _detectedLanguage.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private val _singlishResult = MutableStateFlow<String?>(null)
    val singlishResult: StateFlow<String?> = _singlishResult.asStateFlow()

    private val _viralHookResult = MutableStateFlow<ViralHookResult?>(null)
    val viralHookResult: StateFlow<ViralHookResult?> = _viralHookResult.asStateFlow()

    private val _extractedStyleResult = MutableStateFlow<com.example.ai.ExtractedStyleResult?>(null)
    val extractedStyleResult: StateFlow<com.example.ai.ExtractedStyleResult?> = _extractedStyleResult.asStateFlow()

    // Dialog & navigation states
    var isAiDialogOpen = MutableStateFlow(false)
    var isExportDialogOpen = MutableStateFlow(false)
    var activeStudioTab = MutableStateFlow(0) // 0: Editor, 1: Styles, 2: Projects

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initSampleProjectsIfEmpty()
            repository.projectsFlow.collect { list ->
                if (_currentProject.value == null && list.isNotEmpty()) {
                    selectProject(list.first())
                }
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        pausePlayback()
        _currentProject.value = project
        _totalDurationMs.value = project.durationMs
        _currentPlayheadMs.value = 0L

        val loadedCaptions = CaptionConverters.jsonToCaptionList(project.captionsJson)
        _captions.value = loadedCaptions
        _captionStyle.value = CaptionConverters.jsonToStyle(project.styleJson)
        _selectedVideoUri.value = null
        _selectedVideoName.value = null
        _selectedVideoResolution.value = null
    }

    fun onVideoSelected(uri: android.net.Uri, context: android.content.Context) {
        pausePlayback()
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            retriever.release()

            val durationMs = durationStr?.toLongOrNull() ?: 15000L
            val width = widthStr?.toIntOrNull() ?: 1080
            val height = heightStr?.toIntOrNull() ?: 1920

            val ratio = when {
                height > width * 1.25f -> "9:16"
                width > height * 1.25f -> "16:9"
                else -> "1:1"
            }

            _selectedVideoUri.value = uri
            _totalDurationMs.value = maxOf(3000L, durationMs)
            _currentPlayheadMs.value = 0L

            var fileName: String? = null
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) fileName = cursor.getString(idx)
                    }
                }
            }
            _selectedVideoName.value = fileName ?: uri.lastPathSegment ?: "Uploaded Video"
            _selectedVideoResolution.value = "${width}x${height} ($ratio)"

            // Update current project aspect ratio and duration
            _currentProject.value?.let { current ->
                val updated = current.copy(
                    aspectRatio = ratio,
                    durationMs = _totalDurationMs.value
                )
                _currentProject.value = updated
                viewModelScope.launch {
                    repository.saveProject(updated)
                }
            }

            // Automatically extract audio and generate captions in detected language
            autoGenerateCaptionsFromVideoAudio(context, uri)

        } catch (e: Exception) {
            android.util.Log.e("StudioViewModel", "Error processing selected video", e)
            _selectedVideoUri.value = uri
            _selectedVideoName.value = uri.lastPathSegment ?: "Uploaded Video"
        }
    }

    /**
     * Automatically extracts the audio track from the uploaded video, runs multilingual
     * speech recognition via Firebase AI Gemini model, and synchronizes captions.
     */
    fun autoGenerateCaptionsFromVideoAudio(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _audioProcessingStatus.value = "Extracting video audio track..."
            _aiErrorMessage.value = null

            try {
                val extracted = com.example.data.util.AudioExtractorUtil.extractAudioFromVideo(
                    context = context,
                    videoUri = uri,
                    maxDurationMs = _totalDurationMs.value
                )

                if (!extracted.hasAudio || extracted.audioBytes.isEmpty()) {
                    _audioProcessingStatus.value = "No audio track detected in video"
                    _isAiLoading.value = false
                    delay(3000L)
                    _audioProcessingStatus.value = null
                    return@launch
                }

                _audioProcessingStatus.value = "AI listening & detecting language (Sinhala / English)..."

                val result = aiService.generateCaptionsFromAudio(
                    audioBytes = extracted.audioBytes,
                    mimeType = extracted.mimeType
                )

                result.fold(
                    onSuccess = { generatedCaptions ->
                        if (generatedCaptions.isNotEmpty()) {
                            _captions.value = generatedCaptions
                            recalculateDuration()
                            autoPersistProject()

                            // Detect primary language of generated captions
                            val fullText = generatedCaptions.joinToString(" ") { it.text }
                            val isSinhala = com.example.data.font.SinhalaUnicodeConverter.containsSinhala(fullText)
                            val isEnglish = fullText.any { it in 'a'..'z' || it in 'A'..'Z' }
                            val langSummary = when {
                                isSinhala && isEnglish -> "Bilingual (Sinhala + English)"
                                isSinhala -> "Sinhala"
                                else -> "English"
                            }
                            _detectedLanguage.value = langSummary
                            _audioProcessingStatus.value = "✨ Captions auto-generated! Detected: $langSummary"
                        } else {
                            _audioProcessingStatus.value = "No speech detected in audio"
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("StudioViewModel", "Auto caption generation failed", error)
                        _aiErrorMessage.value = "Auto caption: ${error.message}"
                        _audioProcessingStatus.value = "Speech detection failed. Use AI Tools to enter script."
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("StudioViewModel", "Audio extraction/detection error", e)
                _aiErrorMessage.value = e.message
                _audioProcessingStatus.value = "Error processing video audio"
            } finally {
                _isAiLoading.value = false
                delay(4000L)
                _audioProcessingStatus.value = null
            }
        }
    }

    fun retranscribeCurrentVideo(context: android.content.Context) {
        val uri = _selectedVideoUri.value ?: return
        autoGenerateCaptionsFromVideoAudio(context, uri)
    }

    fun clearVideo() {
        pausePlayback()
        _selectedVideoUri.value = null
        _selectedVideoName.value = null
        _selectedVideoResolution.value = null
    }

    fun createNewProject(
        title: String,
        aspectRatio: String = "9:16",
        initialVideoUri: android.net.Uri? = null,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            val proj = repository.createNewProject(title, aspectRatio)
            repository.saveProject(proj)
            selectProject(proj)
            if (initialVideoUri != null && context != null) {
                onVideoSelected(initialVideoUri, context)
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_currentProject.value?.id == id) {
                val remaining = repository.getAllProjects()
                if (remaining.isNotEmpty()) {
                    selectProject(remaining.first())
                } else {
                    val defaultProj = repository.createNewProject("New Reel", "9:16")
                    repository.saveProject(defaultProj)
                    selectProject(defaultProj)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val stepMs = 33L
            var lastTime = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                delay(stepMs)
                val now = System.currentTimeMillis()
                val delta = (now - lastTime).coerceIn(10L, 80L)
                lastTime = now
                val next = _currentPlayheadMs.value + delta
                if (next >= _totalDurationMs.value) {
                    _currentPlayheadMs.value = 0L // loop
                    lastTime = System.currentTimeMillis()
                } else {
                    _currentPlayheadMs.value = next
                }
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(ms: Long) {
        _currentPlayheadMs.value = ms.coerceIn(0L, _totalDurationMs.value)
    }

    fun restartPlayback() {
        seekTo(0L)
        startPlayback()
    }

    fun updateStyle(style: CaptionStyle) {
        _captionStyle.value = style
        autoPersistProject()
    }

    fun toggleSafeZone() {
        _captionStyle.value = _captionStyle.value.copy(showSafeZone = !_captionStyle.value.showSafeZone)
    }

    fun updateCaption(updated: CaptionItem) {
        _captions.value = _captions.value.map { if (it.id == updated.id) updated else it }
        recalculateDuration()
        autoPersistProject()
    }

    fun deleteCaption(id: String) {
        _captions.value = _captions.value.filterNot { it.id == id }
        autoPersistProject()
    }

    fun addCaptionAtPlayhead() {
        val start = _currentPlayheadMs.value
        val end = (start + 2500L).coerceAtMost(_totalDurationMs.value + 3000L)
        val newDuration = maxOf(_totalDurationMs.value, end)
        _totalDurationMs.value = newDuration

        val text = "New Caption"
        val words = text.split(" ").mapIndexed { idx, w ->
            CaptionWord(w, start + (idx * 400L), start + ((idx + 1) * 400L))
        }
        val newItem = CaptionItem(
            id = UUID.randomUUID().toString(),
            startMs = start,
            endMs = end,
            text = text,
            words = words
        )
        _captions.value = (_captions.value + newItem).sortedBy { it.startMs }
        autoPersistProject()
    }

    fun splitCaption(item: CaptionItem, splitMs: Long) {
        if (splitMs <= item.startMs || splitMs >= item.endMs) return

        val words = item.words
        val firstWords = words.filter { it.endMs <= splitMs }
        val secondWords = words.filter { it.startMs >= splitMs }

        val firstText = firstWords.joinToString(" ") { it.word }.ifBlank { item.text }
        val secondText = secondWords.joinToString(" ") { it.word }.ifBlank { "..." }

        val part1 = item.copy(endMs = splitMs, text = firstText, words = firstWords)
        val part2 = CaptionItem(
            id = UUID.randomUUID().toString(),
            startMs = splitMs,
            endMs = item.endMs,
            text = secondText,
            words = secondWords
        )

        _captions.value = _captions.value.map {
            if (it.id == item.id) part1 else it
        } + part2
        _captions.value = _captions.value.sortedBy { it.startMs }
        autoPersistProject()
    }

    // AI Features Execution
    fun runAiAutoCaption(script: String, languagePreference: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiService.generateCaptionsFromScript(script, languagePreference)
                result.fold(
                    onSuccess = { generatedCaptions ->
                        _captions.value = generatedCaptions
                        if (generatedCaptions.isNotEmpty()) {
                            _totalDurationMs.value = generatedCaptions.maxOf { it.endMs } + 800L
                            _currentPlayheadMs.value = 0L
                        }
                        isAiDialogOpen.value = false
                        autoPersistProject()
                    },
                    onFailure = { err ->
                        _aiErrorMessage.value = err.localizedMessage ?: "Failed to generate captions."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "Unknown AI error."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runSinglishConversion(singlish: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiService.convertSinglishToSinhala(singlish)
                result.fold(
                    onSuccess = { converted ->
                        _singlishResult.value = converted
                    },
                    onFailure = { err ->
                        _aiErrorMessage.value = err.localizedMessage ?: "Conversion failed."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "Error during transliteration."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runTranslation(targetLang: String) {
        viewModelScope.launch {
            if (_captions.value.isEmpty()) {
                _aiErrorMessage.value = "No captions in timeline to translate."
                return@launch
            }
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiService.translateCaptions(_captions.value, targetLang)
                result.fold(
                    onSuccess = { translated ->
                        _captions.value = translated
                        isAiDialogOpen.value = false
                        autoPersistProject()
                    },
                    onFailure = { err ->
                        _aiErrorMessage.value = err.localizedMessage ?: "Translation failed."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "Translation error."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runViralHooks(topic: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiService.generateViralHooks(topic)
                result.fold(
                    onSuccess = { hooks ->
                        _viralHookResult.value = hooks
                    },
                    onFailure = { err ->
                        _aiErrorMessage.value = err.localizedMessage ?: "Hook generation failed."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "Failed to generate hooks."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun runScreenshotStyleExtraction(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            try {
                val result = aiService.analyzeStyleFromScreenshot(bitmap)
                result.fold(
                    onSuccess = { extracted ->
                        _extractedStyleResult.value = extracted
                    },
                    onFailure = { err ->
                        _aiErrorMessage.value = err.localizedMessage ?: "Failed to analyze screenshot style."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = e.localizedMessage ?: "Screenshot analysis failed."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun applyExtractedStyle(style: CaptionStyle) {
        _captionStyle.value = style
        isAiDialogOpen.value = false
        autoPersistProject()
    }

    private fun recalculateDuration() {
        val maxEnd = _captions.value.maxOfOrNull { it.endMs } ?: 10000L
        if (maxEnd + 500L > _totalDurationMs.value) {
            _totalDurationMs.value = maxEnd + 800L
        }
    }

    private var persistJob: kotlinx.coroutines.Job? = null
    private fun autoPersistProject(debounceMs: Long = 300L) {
        val current = _currentProject.value ?: return
        persistJob?.cancel()
        persistJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (debounceMs > 0) delay(debounceMs)
            val updated = current.copy(
                captionsJson = CaptionConverters.captionListToJson(_captions.value),
                styleJson = CaptionConverters.styleToJson(_captionStyle.value),
                durationMs = _totalDurationMs.value,
                updatedAt = System.currentTimeMillis()
            )
            _currentProject.value = updated
            repository.saveProject(updated)
        }
    }
}
