package com.sahandsoft.englishlearner.di

import android.content.Context
import androidx.room.Room
import com.sahandsoft.englishlearner.data.db.ActivityDao
import com.sahandsoft.englishlearner.data.db.BookDao
import com.sahandsoft.englishlearner.data.db.DialogueDao
import com.sahandsoft.englishlearner.data.db.GrammarDao
import com.sahandsoft.englishlearner.data.db.MetaDao
import com.sahandsoft.englishlearner.data.db.ProgressDao
import com.sahandsoft.englishlearner.data.db.QuizDao
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.StoryDao
import com.sahandsoft.englishlearner.data.db.TopicDao
import com.sahandsoft.englishlearner.data.db.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SahandDatabase =
        Room.databaseBuilder(ctx, SahandDatabase::class.java, "sahand.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWordDao(db: SahandDatabase): WordDao = db.wordDao()

    @Provides
    fun provideTopicDao(db: SahandDatabase): TopicDao = db.topicDao()

    @Provides
    fun provideStoryDao(db: SahandDatabase): StoryDao = db.storyDao()

    @Provides
    fun provideBookDao(db: SahandDatabase): BookDao = db.bookDao()

    @Provides
    fun provideGrammarDao(db: SahandDatabase): GrammarDao = db.grammarDao()

    @Provides
    fun provideDialogueDao(db: SahandDatabase): DialogueDao = db.dialogueDao()

    @Provides
    fun provideProgressDao(db: SahandDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideActivityDao(db: SahandDatabase): ActivityDao = db.activityDao()

    @Provides
    fun provideQuizDao(db: SahandDatabase): QuizDao = db.quizDao()

    @Provides
    fun provideMetaDao(db: SahandDatabase): MetaDao = db.metaDao()
}
