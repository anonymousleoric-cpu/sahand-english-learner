package com.sahandsoft.englishlearner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words ORDER BY en ASC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY en ASC")
    suspend fun all(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<WordEntity>

    @Query(
        "SELECT * FROM words WHERE lower(en) LIKE '%' || :q || '%' " +
            "OR fa LIKE '%' || :q || '%' OR faPron LIKE '%' || :q || '%' " +
            "ORDER BY en ASC LIMIT 60"
    )
    suspend fun search(q: String): List<WordEntity>
}

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<TopicEntity>)

    @Query("SELECT * FROM topics")
    fun observeAll(): Flow<List<TopicEntity>>
}

@Dao
interface StoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stories: List<StoryEntity>)

    @Query("SELECT * FROM stories ORDER BY id ASC")
    fun observeAll(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun get(id: String): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(p: StoryProgressEntity)

    @Query("SELECT * FROM story_progress WHERE storyId = :id")
    suspend fun progress(id: String): StoryProgressEntity?

    @Query("SELECT * FROM story_progress")
    fun observeAllProgress(): Flow<List<StoryProgressEntity>>

    @Query("SELECT * FROM story_progress")
    suspend fun allProgress(): List<StoryProgressEntity>

    @Query("DELETE FROM story_progress")
    suspend fun clearProgress()
}

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<BookPageEntity>)

    @Query("SELECT * FROM books ORDER BY id ASC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: String): BookEntity?

    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY number ASC")
    suspend fun pages(bookId: String): List<BookPageEntity>

    @Query("SELECT * FROM book_progress WHERE bookId = :bookId")
    suspend fun progress(bookId: String): BookProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(p: BookProgressEntity)

    @Query("SELECT * FROM book_progress")
    fun observeAllProgress(): Flow<List<BookProgressEntity>>

    @Query("SELECT * FROM book_progress")
    suspend fun allProgress(): List<BookProgressEntity>

    @Query("DELETE FROM book_progress")
    suspend fun clearProgress()
}

@Dao
interface GrammarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<GrammarEntity>)

    @Query("SELECT * FROM grammar ORDER BY lessonOrder ASC")
    fun observeAll(): Flow<List<GrammarEntity>>

    @Query("SELECT * FROM grammar WHERE id = :id")
    suspend fun get(id: String): GrammarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(p: GrammarProgressEntity)

    @Query("SELECT * FROM grammar_progress WHERE lessonId = :id")
    suspend fun progress(id: String): GrammarProgressEntity?

    @Query("SELECT * FROM grammar_progress")
    fun observeAllProgress(): Flow<List<GrammarProgressEntity>>

    @Query("SELECT * FROM grammar_progress")
    suspend fun allProgress(): List<GrammarProgressEntity>

    @Query("DELETE FROM grammar_progress")
    suspend fun clearProgress()
}

@Dao
interface DialogueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dialogues: List<DialogueEntity>)

    @Query("SELECT * FROM dialogues ORDER BY id ASC")
    fun observeAll(): Flow<List<DialogueEntity>>

    @Query("SELECT * FROM dialogues WHERE id = :id")
    suspend fun get(id: String): DialogueEntity?
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM word_progress WHERE wordId = :wordId")
    suspend fun get(wordId: String): WordProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: WordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<WordProgressEntity>)

    @Query("SELECT * FROM word_progress")
    fun observeAll(): Flow<List<WordProgressEntity>>

    @Query("SELECT wordId FROM word_progress WHERE learned = 1")
    fun observeLearnedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM word_progress WHERE dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM word_progress WHERE dueAt <= :now ORDER BY dueAt ASC LIMIT 60")
    suspend fun due(now: Long): List<WordProgressEntity>

    @Query("SELECT COUNT(*) FROM word_progress WHERE learned = 1")
    fun learnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE reps >= 5")
    fun masteredCount(): Flow<Int>

    @Query("SELECT * FROM word_progress")
    suspend fun all(): List<WordProgressEntity>

    @Query("DELETE FROM word_progress")
    suspend fun clear()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE day = :day")
    fun observeDay(day: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM activity ORDER BY day ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity ORDER BY day ASC")
    suspend fun all(): List<ActivityEntity>

    @Query("SELECT * FROM activity WHERE day = :day")
    suspend fun get(day: Long): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<ActivityEntity>)

    @Query("DELETE FROM activity")
    suspend fun clear()
}

data class QuizAgg(val cnt: Int, val sumScore: Int, val sumTotal: Int)

@Dao
interface QuizDao {
    @Insert
    suspend fun insert(r: QuizResultEntity)

    @Query("SELECT COUNT(*) as cnt, COALESCE(SUM(score), 0) as sumScore, COALESCE(SUM(total), 0) as sumTotal FROM quiz_results")
    suspend fun agg(): QuizAgg

    @Query("SELECT * FROM quiz_results ORDER BY at DESC LIMIT 30")
    fun observeRecent(): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results ORDER BY at DESC")
    suspend fun all(): List<QuizResultEntity>

    @Query("DELETE FROM quiz_results")
    suspend fun clear()
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(e: MetaEntity)
}
