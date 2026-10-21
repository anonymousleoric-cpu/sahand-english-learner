package com.sahandsoft.englishlearner.domain

import com.sahandsoft.englishlearner.data.db.WordProgressEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * الگوریتم مرور با فاصله (نسخهٔ سبک SM-2).
 * پاسخ درست → افزایش فاصلهٔ مرور؛ پاسخ غلط → بازگشت به فاصلهٔ کوتاه.
 */
object Srs {

    const val DAY_MS: Long = 86_400_000L
    private const val TEN_MIN_MS: Long = 10 * 60_000L

    fun next(wordId: String, prev: WordProgressEntity?, correct: Boolean, now: Long = System.currentTimeMillis()): WordProgressEntity {
        if (prev == null) {
            return if (correct) {
                WordProgressEntity(
                    wordId = wordId,
                    ease = 2.5,
                    intervalDays = 1,
                    reps = 1,
                    correct = 1,
                    dueAt = now + DAY_MS,
                    learned = false,
                    lastSeenAt = now
                )
            } else {
                WordProgressEntity(
                    wordId = wordId,
                    ease = 1.3,
                    wrong = 1,
                    dueAt = now + TEN_MIN_MS,
                    lastSeenAt = now
                )
            }
        }
        return if (correct) {
            val reps = prev.reps + 1
            val interval = when {
                reps == 1 -> 1
                reps == 2 -> 3
                else -> max(3, (prev.intervalDays * prev.ease).roundToInt())
            }
            val ease = min(2.8, prev.ease + 0.05)
            prev.copy(
                reps = reps,
                correct = prev.correct + 1,
                intervalDays = interval,
                ease = ease,
                dueAt = now + interval * DAY_MS,
                learned = prev.learned || reps >= 3,
                lastSeenAt = now
            )
        } else {
            prev.copy(
                reps = 0,
                lapses = prev.lapses + 1,
                wrong = prev.wrong + 1,
                intervalDays = 0,
                ease = max(1.3, prev.ease - 0.2),
                dueAt = now + TEN_MIN_MS,
                learned = false,
                lastSeenAt = now
            )
        }
    }
}
