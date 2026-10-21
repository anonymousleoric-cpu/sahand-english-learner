package com.sahandsoft.englishlearner.domain

import com.sahandsoft.englishlearner.data.db.WordEntity
import kotlin.random.Random

data class QuizQuestion(
    val type: String,
    val wordId: String,
    val prompt: String,
    val promptFa: String?,
    val options: List<String>,
    val answer: Int,
    val speakText: String?
)

/**
 * تولید سؤالات چهارگزینه‌ای از بانک واژگان:
 * en2fa (واژه → معنی)، fa2en (معنی → واژه)، fill (جای خالی در جملهٔ مثال).
 */
object QuizEngine {

    fun generate(
        words: List<WordEntity>,
        level: String?,
        count: Int,
        rnd: Random = Random.Default
    ): List<QuizQuestion> {
        val pool = if (level.isNullOrBlank() || level == "all") words else words.filter { it.level == level }
        if (pool.size < 4) return emptyList()
        val unique = pool.distinctBy { it.en.lowercase() }
        return unique.shuffled(rnd).take(count).mapNotNull { w -> makeQuestion(w, unique, rnd) }
    }

    private fun makeQuestion(w: WordEntity, pool: List<WordEntity>, rnd: Random): QuizQuestion? {
        val types = if (w.example.isNullOrBlank()) listOf("en2fa", "fa2en") else listOf("en2fa", "fa2en", "fill")
        val type = types.random(rnd)
        val others = pool.filter { it.id != w.id }.shuffled(rnd).take(3)
        if (others.size < 3) return null
        return when (type) {
            "en2fa" -> {
                val faOptions = others.map { it.fa }.distinct()
                if (faOptions.size < 3) return null
                val opts = (faOptions + w.fa).shuffled(rnd)
                QuizQuestion("en2fa", w.id, w.en, null, opts, opts.indexOf(w.fa), w.en)
            }
            "fa2en" -> {
                val enOptions = others.map { it.en }.distinct()
                if (enOptions.size < 3) return null
                val opts = (enOptions + w.en).shuffled(rnd)
                QuizQuestion("fa2en", w.id, w.fa, null, opts, opts.indexOf(w.en), w.en)
            }
            else -> {
                val ex = w.example ?: return null
                val blanked = replaceWholeWord(ex, w.en)
                if (blanked == ex) return null
                val enOptions = others.map { it.en }.distinct()
                if (enOptions.size < 3) return null
                val opts = (enOptions + w.en).shuffled(rnd)
                QuizQuestion(
                    "fill", w.id, blanked,
                    w.exampleFa,
                    opts, opts.indexOf(w.en), w.en
                )
            }
        }
    }

    private fun replaceWholeWord(sentence: String, word: String): String {
        val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
        return regex.replace(sentence, "___")
    }
}
