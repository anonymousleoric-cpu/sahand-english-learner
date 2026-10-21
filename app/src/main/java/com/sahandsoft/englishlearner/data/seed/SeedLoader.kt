package com.sahandsoft.englishlearner.data.seed

import android.content.Context
import androidx.room.withTransaction
import com.sahandsoft.englishlearner.data.db.BookEntity
import com.sahandsoft.englishlearner.data.db.BookPageEntity
import com.sahandsoft.englishlearner.data.db.DialogueEntity
import com.sahandsoft.englishlearner.data.db.GrammarEntity
import com.sahandsoft.englishlearner.data.db.MetaEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.StoryEntity
import com.sahandsoft.englishlearner.data.db.TopicEntity
import com.sahandsoft.englishlearner.data.db.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SahandDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun readAsset(name: String): String =
        context.assets.open("seed/$name").bufferedReader().use { it.readText() }

    suspend fun ensureSeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (db.metaDao().get("seed_version") != null) return@withContext true

            val words = json.decodeFromString<List<WordDto>>(readAsset("words.json"))
            val topics = json.decodeFromString<List<TopicDto>>(readAsset("topics.json"))
            val stories = json.decodeFromString<List<StoryDto>>(readAsset("stories.json"))
            val books = json.decodeFromString<List<BookDto>>(readAsset("books.json"))
            val grammar = json.decodeFromString<List<GrammarDto>>(readAsset("grammar.json"))
            val dialogues = json.decodeFromString<List<DialogueDto>>(readAsset("dialogues.json"))

            val wordEntities = words.map {
                WordEntity(
                    id = it.en.trim().lowercase(),
                    en = it.en.trim(),
                    fa = it.fa,
                    ipa = it.ipa,
                    faPron = it.faPron,
                    pos = it.pos,
                    emoji = it.emoji,
                    example = it.example,
                    exampleFa = it.exampleFa,
                    topic = it.topic,
                    level = it.level
                )
            }
            val topicEntities = topics.map { TopicEntity(it.key, it.titleFa, it.emoji) }
            val storyEntities = stories.map {
                StoryEntity(
                    id = it.id,
                    title = it.title,
                    titleFa = it.titleFa,
                    level = it.level,
                    topic = it.topic,
                    emoji = it.emoji,
                    minutes = it.minutes,
                    sectionsJson = json.encodeToString(it.sections),
                    vocabJson = json.encodeToString(it.vocab),
                    questionsJson = json.encodeToString(it.questions)
                )
            }
            val bookEntities = books.map {
                BookEntity(it.id, it.title, it.titleFa, it.level, it.descFa, it.emoji, it.color)
            }
            val pageEntities = books.flatMap { b ->
                b.pages.map { p ->
                    BookPageEntity(
                        id = "${b.id}-${p.number}",
                        bookId = b.id,
                        number = p.number,
                        chapter = p.chapter,
                        title = p.title,
                        titleFa = p.titleFa,
                        kind = p.kind,
                        contentJson = json.encodeToString(p.content)
                    )
                }
            }
            val grammarEntities = grammar.map {
                GrammarEntity(
                    id = it.id,
                    lessonOrder = it.order,
                    title = it.title,
                    titleFa = it.titleFa,
                    emoji = it.emoji,
                    level = it.level,
                    contentJson = json.encodeToString(it.content)
                )
            }
            val dialogueEntities = dialogues.map {
                DialogueEntity(
                    id = it.id,
                    title = it.title,
                    titleFa = it.titleFa,
                    emoji = it.emoji,
                    level = it.level,
                    linesJson = json.encodeToString(it.lines),
                    practiceJson = json.encodeToString(it.practice)
                )
            }

            db.withTransaction {
                db.wordDao().insertAll(wordEntities)
                db.topicDao().insertAll(topicEntities)
                db.storyDao().insertAll(storyEntities)
                db.bookDao().insertBooks(bookEntities)
                db.bookDao().insertPages(pageEntities)
                db.grammarDao().insertAll(grammarEntities)
                db.dialogueDao().insertAll(dialogueEntities)
                db.metaDao().put(MetaEntity("seed_version", "1"))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
