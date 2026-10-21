package com.sahandsoft.englishlearner.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.sahandsoft.englishlearner.data.db.ActivityEntity
import com.sahandsoft.englishlearner.data.db.BookProgressEntity
import com.sahandsoft.englishlearner.data.db.GrammarProgressEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.StoryProgressEntity
import com.sahandsoft.englishlearner.data.db.WordProgressEntity
import com.sahandsoft.englishlearner.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupWordProgress(
    val wordId: String,
    val ease: Double,
    val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    val correct: Int,
    val wrong: Int,
    val dueAt: Long,
    val learned: Boolean,
    val lastSeenAt: Long
)

@Serializable
data class BackupStoryProgress(
    val storyId: String,
    val bestScore: Int,
    val questionCount: Int,
    val done: Boolean,
    val updatedAt: Long
)

@Serializable
data class BackupBookProgress(
    val bookId: String,
    val lastPage: Int,
    val updatedAt: Long
)

@Serializable
data class BackupGrammarProgress(
    val lessonId: String,
    val bestQuiz: Int,
    val quizTotal: Int,
    val done: Boolean,
    val updatedAt: Long
)

@Serializable
data class BackupActivity(val day: Long, val seconds: Long, val actions: Int)

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val themeMode: String = "system",
    val childMode: Boolean = false,
    val dailyGoalMinutes: Int = 10,
    val ttsRate: Float = 1.0f,
    val wordProgress: List<BackupWordProgress> = emptyList(),
    val storyProgress: List<BackupStoryProgress> = emptyList(),
    val bookProgress: List<BackupBookProgress> = emptyList(),
    val grammarProgress: List<BackupGrammarProgress> = emptyList(),
    val activity: List<BackupActivity> = emptyList()
)

/**
 * پشتیبان‌گیری و بازیابی پیشرفت کاربر به‌صورت فایل JSON
 * (کاملاً محلی — هیچ داده‌ای از دستگاه خارج نمی‌شود).
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SahandDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = settingsRepository.snapshot()
            val data = BackupData(
                exportedAt = System.currentTimeMillis(),
                themeMode = s.themeMode,
                childMode = s.childMode,
                dailyGoalMinutes = s.dailyGoalMinutes,
                ttsRate = s.ttsRate,
                wordProgress = db.progressDao().all().map {
                    BackupWordProgress(
                        wordId = it.wordId, ease = it.ease, intervalDays = it.intervalDays,
                        reps = it.reps, lapses = it.lapses, correct = it.correct, wrong = it.wrong,
                        dueAt = it.dueAt, learned = it.learned, lastSeenAt = it.lastSeenAt
                    )
                },
                storyProgress = db.storyDao().allProgress().map {
                    BackupStoryProgress(it.storyId, it.bestScore, it.questionCount, it.done, it.updatedAt)
                },
                bookProgress = db.bookDao().allProgress().map {
                    BackupBookProgress(it.bookId, it.lastPage, it.updatedAt)
                },
                grammarProgress = db.grammarDao().allProgress().map {
                    BackupGrammarProgress(it.lessonId, it.bestQuiz, it.quizTotal, it.done, it.updatedAt)
                },
                activity = db.activityDao().all().map {
                    BackupActivity(it.day, it.seconds, it.actions)
                }
            )
            val out = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            out.bufferedWriter().use { writer ->
                writer.write(json.encodeToString(BackupData.serializer(), data))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val text = input.bufferedReader().use { it.readText() }
            val data = json.decodeFromString(BackupData.serializer(), text)
            db.withTransaction {
                db.progressDao().clear()
                db.progressDao().upsertAll(
                    data.wordProgress.map {
                        WordProgressEntity(
                            wordId = it.wordId, ease = it.ease, intervalDays = it.intervalDays,
                            reps = it.reps, lapses = it.lapses, correct = it.correct, wrong = it.wrong,
                            dueAt = it.dueAt, learned = it.learned, lastSeenAt = it.lastSeenAt
                        )
                    }
                )
                db.storyDao().clearProgress()
                data.storyProgress.forEach {
                    db.storyDao().upsertProgress(
                        StoryProgressEntity(it.storyId, it.bestScore, it.questionCount, it.done, it.updatedAt)
                    )
                }
                db.bookDao().clearProgress()
                data.bookProgress.forEach {
                    db.bookDao().upsertProgress(BookProgressEntity(it.bookId, it.lastPage, it.updatedAt))
                }
                db.grammarDao().clearProgress()
                data.grammarProgress.forEach {
                    db.grammarDao().upsertProgress(
                        GrammarProgressEntity(it.lessonId, it.bestQuiz, it.quizTotal, it.done, it.updatedAt)
                    )
                }
                db.activityDao().clear()
                db.activityDao().upsertAll(
                    data.activity.map { ActivityEntity(it.day, it.seconds, it.actions) }
                )
            }
            settingsRepository.setTheme(data.themeMode)
            settingsRepository.setChildMode(data.childMode)
            settingsRepository.setDailyGoal(data.dailyGoalMinutes)
            settingsRepository.setTtsRate(data.ttsRate)
            true
        } catch (e: Exception) {
            false
        }
    }
}
