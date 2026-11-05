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

    suspend fun initSampleProjectsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getProjectById("sample_project_1")
        if (existing == null) {
            val sample1Captions = listOf(
                CaptionItem(
                    id = "c1_1",
                    startMs = 0L,
                    endMs = 2800L,
                    text = "අද අපි කතා කරන්නේ SinCC AI ගැන!",
                    words = listOf(
                        CaptionWord("අද", 0L, 500L),
                        CaptionWord("අපි", 500L, 1000L),
                        CaptionWord("කතා", 1000L, 1500L),
                        CaptionWord("කරන්නේ", 1500L, 2100L),
                        CaptionWord("SinCC", 2100L, 2500L),
                        CaptionWord("AI", 2500L, 2700L),
                        CaptionWord("ගැන!", 2700L, 2800L)
                    )
                ),
                CaptionItem(
                    id = "c1_2",
                    startMs = 3000L,
                    endMs = 7000L,
                    text = "Reels සහ TikTok වලට Sinhala Captions හදන්න සුපිරිම App එකක්!",
                    words = listOf(
                        CaptionWord("Reels", 3000L, 3600L),
                        CaptionWord("සහ", 3600L, 4000L),
                        CaptionWord("TikTok", 4000L, 4600L),
                        CaptionWord("වලට", 4600L, 5100L),
                        CaptionWord("Sinhala", 5100L, 5700L),
                        CaptionWord("Captions", 5700L, 6200L),
                        CaptionWord("හදන්න", 6200L, 6600L),
                        CaptionWord("සුපිරිම", 6600L, 6850L),
                        CaptionWord("App", 6850L, 6950L),
                        CaptionWord("එකක්!", 6950L, 7000L)
                    )
                ),
                CaptionItem(
                    id = "c1_3",
                    startMs = 7200L,
                    endMs = 11500L,
                    text = "Word-by-word karaoke style එකෙන් auto subtitles හැදෙනවා.",
                    words = listOf(
                        CaptionWord("Word-by-word", 7200L, 8000L),
                        CaptionWord("karaoke", 8000L, 8700L),
                        CaptionWord("style", 8700L, 9300L),
                        CaptionWord("එකෙන්", 9300L, 9800L),
                        CaptionWord("auto", 9800L, 10300L),
                        CaptionWord("subtitles", 10300L, 10900L),
                        CaptionWord("හැදෙනවා.", 10900L, 11500L)
                    )
                ),
                CaptionItem(
                    id = "c1_4",
                    startMs = 11800L,
                    endMs = 15000L,
                    text = "Try කරලා බලන්න, අදම download කරගන්න! 🔥",
                    words = listOf(
                        CaptionWord("Try", 11800L, 12400L),
                        CaptionWord("කරලා", 12400L, 13000L),
                        CaptionWord("බලන්න,", 13000L, 13600L),
                        CaptionWord("අදම", 13600L, 14200L),
                        CaptionWord("download", 14200L, 14700L),
                        CaptionWord("කරගන්න! 🔥", 14700L, 15000L)
                    )
                )
            )

            dao.insertProject(
                ProjectEntity(
                    id = "sample_project_1",
                    title = "SinCC Sinhala AI Review Reel",
                    aspectRatio = "9:16",
                    durationMs = 15500L,
                    captionsJson = CaptionConverters.captionListToJson(sample1Captions),
                    styleJson = CaptionConverters.styleToJson(CaptionStyle(preset = CaptionPreset.HORMOZI_PUNCH)),
                    videoBackgroundPreset = "cyber_gradient",
                    language = "Sinhala (සිංහල)",
                    updatedAt = System.currentTimeMillis()
                )
            )

            val sample2Captions = listOf(
                CaptionItem(
                    id = "c2_1",
                    startMs = 0L,
                    endMs = 3200L,
                    text = "Wassup guys! Exploring Ella Sri Lanka today 🇱🇰",
                    words = listOf(
                        CaptionWord("Wassup", 0L, 600L),
                        CaptionWord("guys!", 600L, 1200L),
                        CaptionWord("Exploring", 1200L, 1900L),
                        CaptionWord("Ella", 1900L, 2400L),
                        CaptionWord("Sri Lanka", 2400L, 2900L),
                        CaptionWord("today 🇱🇰", 2900L, 3200L)
                    )
                ),
                CaptionItem(
                    id = "c2_2",
                    startMs = 3400L,
                    endMs = 7500L,
                    text = "Kandy to Ella train ride eka truly magical machan!",
                    words = listOf(
                        CaptionWord("Kandy", 3400L, 4000L),
                        CaptionWord("to", 4000L, 4300L),
                        CaptionWord("Ella", 4300L, 4800L),
                        CaptionWord("train", 4800L, 5400L),
                        CaptionWord("ride", 5400L, 5900L),
                        CaptionWord("eka", 5900L, 6400L),
                        CaptionWord("truly", 6400L, 6800L),
                        CaptionWord("magical", 6800L, 7200L),
                        CaptionWord("machan!", 7200L, 7500L)
                    )
                ),
                CaptionItem(
                    id = "c2_3",
                    startMs = 7700L,
                    endMs = 12000L,
                    text = "Nine Arch bridge eka gawa views are insane ✨",
                    words = listOf(
                        CaptionWord("Nine", 7700L, 8300L),
                        CaptionWord("Arch", 8300L, 8800L),
                        CaptionWord("bridge", 8800L, 9400L),
                        CaptionWord("eka", 9400L, 9900L),
                        CaptionWord("gawa", 9900L, 10500L),
                        CaptionWord("views", 10500L, 11000L),
                        CaptionWord("are", 11000L, 11400L),
                        CaptionWord("insane ✨", 11400L, 12000L)
                    )
                )
            )

            dao.insertProject(
                ProjectEntity(
                    id = "sample_project_2",
                    title = "Ella Travel Singlish Shorts",
                    aspectRatio = "9:16",
                    durationMs = 12500L,
                    captionsJson = CaptionConverters.captionListToJson(sample2Captions),
                    styleJson = CaptionConverters.styleToJson(CaptionStyle(preset = CaptionPreset.CYBER_PUNK)),
                    videoBackgroundPreset = "travel_vibe",
                    language = "Singlish",
                    updatedAt = System.currentTimeMillis() - 3600000L
                )
            )

            val sample3Captions = listOf(
                CaptionItem(
                    id = "c3_1",
                    startMs = 0L,
                    endMs = 2800L,
                    text = "සුපිරිම SINHALA CAPTION STYLE ⚡",
                    words = listOf(
                        CaptionWord("සුපිරිම", 0L, 700L),
                        CaptionWord("SINHALA", 700L, 1400L),
                        CaptionWord("CAPTION", 1400L, 2100L),
                        CaptionWord("STYLE ⚡", 2100L, 2800L)
                    )
                ),
                CaptionItem(
                    id = "c3_2",
                    startMs = 3000L,
                    endMs = 6500L,
                    text = "කහ පාටින් TOP POSITION එකේ පේනවා",
                    words = listOf(
                        CaptionWord("කහ", 3000L, 3500L),
                        CaptionWord("පාටින්", 3500L, 4100L),
                        CaptionWord("TOP", 4100L, 4800L),
                        CaptionWord("POSITION", 4800L, 5600L),
                        CaptionWord("එකේ", 5600L, 6000L),
                        CaptionWord("පේනවා", 6000L, 6500L)
                    )
                ),
                CaptionItem(
                    id = "c3_3",
                    startMs = 6800L,
                    endMs = 10500L,
                    text = "ඕනෑම සිංහල FONT එකක් USE කරන්න පුළුවන් 🔥",
                    words = listOf(
                        CaptionWord("ඕනෑම", 6800L, 7400L),
                        CaptionWord("සිංහල", 7400L, 8100L),
                        CaptionWord("FONT", 8100L, 8800L),
                        CaptionWord("එකක්", 8800L, 9300L),
                        CaptionWord("USE", 9300L, 9800L),
                        CaptionWord("කරන්න", 9800L, 10200L),
                        CaptionWord("පුළුවන් 🔥", 10200L, 10500L)
                    )
                )
            )

            val stealStyle = CaptionStyle(
                preset = CaptionPreset.STEAL_MY_STYLE,
                highlightColorHex = CaptionPreset.STEAL_MY_STYLE.highlightColor,
                textColorHex = CaptionPreset.STEAL_MY_STYLE.textColor,
                backgroundColorHex = CaptionPreset.STEAL_MY_STYLE.boxColor,
                hasStroke = CaptionPreset.STEAL_MY_STYLE.hasStroke,
                fontFamily = CaptionPreset.STEAL_MY_STYLE.defaultFont,
                englishFontFamily = CaptionPreset.STEAL_MY_STYLE.defaultEnglishFont,
                position = CaptionPreset.STEAL_MY_STYLE.defaultPosition,
                verticalOffsetDp = CaptionPreset.STEAL_MY_STYLE.defaultVerticalOffsetDp,
                isUppercase = CaptionPreset.STEAL_MY_STYLE.defaultUppercase,
                hasBackgroundBox = CaptionPreset.STEAL_MY_STYLE.defaultHasBackgroundBox,
                fontSizeSp = CaptionPreset.STEAL_MY_STYLE.defaultFontSizeSp,
                animation = CaptionPreset.STEAL_MY_STYLE.defaultAnimation
            )

            dao.insertProject(
                ProjectEntity(
                    id = "sample_project_3",
                    title = "Steal My Style Viral Reel",
                    aspectRatio = "9:16",
                    durationMs = 11000L,
                    captionsJson = CaptionConverters.captionListToJson(sample3Captions),
                    styleJson = CaptionConverters.styleToJson(stealStyle),
                    videoBackgroundPreset = "sunny_beach",
                    language = "English / Creator",
                    updatedAt = System.currentTimeMillis() - 1800000L
                )
            )
        }
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
