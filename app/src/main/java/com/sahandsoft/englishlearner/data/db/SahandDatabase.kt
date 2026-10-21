package com.sahandsoft.englishlearner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        TopicEntity::class,
        StoryEntity::class,
        BookEntity::class,
        BookPageEntity::class,
        GrammarEntity::class,
        DialogueEntity::class,
        WordProgressEntity::class,
        StoryProgressEntity::class,
        BookProgressEntity::class,
        GrammarProgressEntity::class,
        QuizResultEntity::class,
        ActivityEntity::class,
        MetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SahandDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun topicDao(): TopicDao
    abstract fun storyDao(): StoryDao
    abstract fun bookDao(): BookDao
    abstract fun grammarDao(): GrammarDao
    abstract fun dialogueDao(): DialogueDao
    abstract fun progressDao(): ProgressDao
    abstract fun activityDao(): ActivityDao
    abstract fun quizDao(): QuizDao
    abstract fun metaDao(): MetaDao
}
