package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.model.CaptionConverters
import com.example.data.model.CaptionItem
import com.example.data.model.CaptionPreset
import com.example.data.model.CaptionStyle
import com.example.data.model.CaptionWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "sincc_studio.db"
    ).build()

    private val dao = db.projectDao()

    val projectsFlow: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getAllProjects(): List<ProjectEntity> = withContext(Dispatchers.IO) {
        dao.getProjectsList()
    }

    suspend fun getProject(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.updateProject(project)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        dao.deleteProject(id)
    }

    /** Removes the old built-in sample projects (from earlier versions). New users never get them. */
    suspend fun removeSampleProjects() = withContext(Dispatchers.IO) {
        listOf("sample_project_1", "sample_project_2", "sample_project_3").forEach { dao.deleteProject(it) }
    }
    fun createNewProject(
        title: String,
        aspectRatio: String = "9:16",
        initialCaptions: List<CaptionItem> = emptyList()
    ): ProjectEntity {
        val id = UUID.randomUUID().toString()
        val duration = if (initialCaptions.isNotEmpty()) {
            initialCaptions.maxOf { it.endMs } + 1000L
        } else {
            10000L
        }
        return ProjectEntity(
            id = id,
            title = title,
            aspectRatio = aspectRatio,
            durationMs = duration,
            captionsJson = CaptionConverters.captionListToJson(initialCaptions),
            styleJson = CaptionConverters.styleToJson(CaptionStyle()),
            videoBackgroundPreset = "cyber_gradient",
            language = "Sinhala (සිංහල)",
            updatedAt = System.currentTimeMillis()
        )
    }
}
