package com.sahandsoft.englishlearner.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: String,
    val en: String,
    val fa: String,
    val ipa: String?,
    val faPron: String?,
    val pos: String?,
    val emoji: String?,
    val example: String?,
    val exampleFa: String?,
    val topic: String,
    val level: String
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val key: String,
    val titleFa: String,
    val emoji: String
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleFa: String,
    val level: String,
    val topic: String,
    val emoji: String,
    val minutes: Int,
    val sectionsJson: String,
    val vocabJson: String,
    val questionsJson: String
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleFa: String,
    val level: String,
    val descFa: String,
    val emoji: String,
    val color: String
)

@Entity(tableName = "book_pages", indices = [Index("bookId")])
data class BookPageEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val number: Int,
    val chapter: Int,
    val title: String,
    val titleFa: String,
    val kind: String,
    val contentJson: String
)

@Entity(tableName = "grammar")
data class GrammarEntity(
    @PrimaryKey val id: String,
    val lessonOrder: Int,
    val title: String,
    val titleFa: String,
    val emoji: String,
    val level: String,
    val contentJson: String
)

@Entity(tableName = "dialogues")
data class DialogueEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleFa: String,
    val emoji: String,
    val level: String,
    val linesJson: String,
    val practiceJson: String
)

@Entity(tableName = "word_progress")
data class WordProgressEntity(
    @PrimaryKey val wordId: String,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val dueAt: Long = 0L,
    val learned: Boolean = false,
    val lastSeenAt: Long = 0L
)

@Entity(tableName = "story_progress")
data class StoryProgressEntity(
    @PrimaryKey val storyId: String,
    val bestScore: Int = 0,
    val questionCount: Int = 0,
    val done: Boolean = false,
    val updatedAt: Long = 0L
)

@Entity(tableName = "book_progress")
data class BookProgressEntity(
    @PrimaryKey val bookId: String,
    val lastPage: Int = 1,
    val updatedAt: Long = 0L
)

@Entity(tableName = "grammar_progress")
data class GrammarProgressEntity(
    @PrimaryKey val lessonId: String,
    val bestQuiz: Int = 0,
    val quizTotal: Int = 0,
    val done: Boolean = false,
    val updatedAt: Long = 0L
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val at: Long,
    val kind: String,
    val score: Int,
    val total: Int
)

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val day: Long,
    val seconds: Long = 0L,
    val actions: Int = 0
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String
)
