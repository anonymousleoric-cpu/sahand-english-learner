package com.sahandsoft.englishlearner.data.repo

import com.sahandsoft.englishlearner.data.db.ActivityEntity
import com.sahandsoft.englishlearner.data.db.QuizResultEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.data.db.WordProgressEntity
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.domain.Srs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class HomeStats(
    val todaySeconds: Long = 0,
    val goalSeconds: Long = 600,
    val streak: Int = 0,
    val dueCount: Int = 0,
    val learned: Int = 0,
    val mastered: Int = 0,
    val totalWords: Int = 0,
    val recentQuizzes: Int = 0,
    val recentQuizScore: Int = 0,
    val recentQuizTotal: Int = 0,
    val weekActivity: List<Pair<Long, Long>> = emptyList()
)

@Singleton
class ProgressRepository @Inject constructor(
    private val db: SahandDatabase
) {

    /* ---------- ثبت فعالیت روزانه ---------- */

    suspend fun addSeconds(seconds: Long, actions: Int = 0) {
        val day = DayUtils.localEpochDay()
        val cur = db.activityDao().get(day)
        db.activityDao().upsert(
            ActivityEntity(
                day = day,
                seconds = (cur?.seconds ?: 0L) + seconds,
                actions = (cur?.actions ?: 0) + actions
            )
        )
    }

    /* ---------- مرور با فاصله ---------- */

    suspend fun recordAnswer(wordId: String, correct: Boolean) {
        val prev = db.progressDao().get(wordId)
        db.progressDao().upsert(Srs.next(wordId, prev, correct))
        addSeconds(if (correct) 15L else 20L, 1)
    }

    suspend fun markSeen(wordId: String) {
        val prev = db.progressDao().get(wordId)
        val now = System.currentTimeMillis()
        db.progressDao().upsert(prev?.copy(lastSeenAt = now) ?: WordProgressEntity(wordId = wordId, lastSeenAt = now))
    }

    suspend fun dueWords(): List<WordEntity> {
        val ps = db.progressDao().due(System.currentTimeMillis())
        if (ps.isEmpty()) return emptyList()
        val ws = db.wordDao().byIds(ps.map { it.wordId })
        val map = ws.associateBy { it.id }
        return ps.mapNotNull { map[it.wordId] }
    }

    fun observeDueCount(): Flow<Int> =
        db.progressDao().observeAll().map { list ->
            val now = System.currentTimeMillis()
            list.count { it.dueAt <= now }
        }

    /* ---------- آزمون ---------- */

    suspend fun recordQuiz(kind: String, score: Int, total: Int) {
        db.quizDao().insert(QuizResultEntity(at = System.currentTimeMillis(), kind = kind, score = score, total = total))
        addSeconds(total * 20L, total)
    }

    /* ---------- آمار خانه ---------- */

    fun observeHomeStats(goalMinutes: Int): Flow<HomeStats> = combine(
        db.activityDao().observeAll(),
        db.progressDao().observeAll(),
        db.wordDao().observeCount(),
        db.quizDao().observeRecent()
    ) { activities, progress, wordCount, quizzes ->
        val today = DayUtils.localEpochDay()
        val now = System.currentTimeMillis()
        val todaySec = activities.firstOrNull { it.day == today }?.seconds ?: 0L
        val active = activities.filter { it.seconds > 0 }.map { it.day }.toSet()
        val week = ((today - 6)..today).map { d ->
            d to (activities.firstOrNull { it.day == d }?.seconds ?: 0L)
        }
        HomeStats(
            todaySeconds = todaySec,
            goalSeconds = goalMinutes * 60L,
            streak = DayUtils.streak(active, today),
            dueCount = progress.count { it.dueAt <= now },
            learned = progress.count { it.learned },
            mastered = progress.count { it.reps >= 5 },
            totalWords = wordCount,
            recentQuizzes = quizzes.size,
            recentQuizScore = quizzes.sumOf { it.score },
            recentQuizTotal = quizzes.sumOf { it.total },
            weekActivity = week
        )
    }

    /* ---------- آمار پروفایل ---------- */

    suspend fun storiesDone(): Int = db.storyDao().allProgress().count { it.done }

    suspend fun booksPagesRead(): Int = db.bookDao().allProgress().sumOf { maxOf(0, it.lastPage - 1) }

    suspend fun lessonsDone(): Int = db.grammarDao().allProgress().count { it.done }

    suspend fun quizAgg(): Pair<Int, Int> {
        val agg = db.quizDao().agg()
        return agg.sumScore to agg.sumTotal
    }

    /* ---------- بازنشانی ---------- */

    suspend fun resetAllProgress() {
        db.withTransactionCompat {
            db.progressDao().clear()
            db.storyDao().clearProgress()
            db.bookDao().clearProgress()
            db.grammarDao().clearProgress()
            db.quizDao().clear()
            db.activityDao().clear()
        }
    }

    private suspend fun SahandDatabase.withTransactionCompat(block: suspend () -> Unit) {
        androidx.room.withTransaction(this) { block() }
    }
}
