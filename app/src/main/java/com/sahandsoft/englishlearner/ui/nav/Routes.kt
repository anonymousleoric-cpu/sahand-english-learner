package com.sahandsoft.englishlearner.ui.nav

object Routes {
    const val HOME = "home"
    const val WORDS = "words"
    const val FLASHCARDS = "flashcards/{category}"
    fun flashcards(category: String) = "flashcards/$category"
    const val LIBRARY = "library"
    const val GAMES = "games"
    const val TOOLS = "tools"
    const val STORY = "story/{storyId}"
    fun story(id: String) = "story/$id"
    const val BOOK = "book/{bookId}"
    fun book(id: String) = "book/$id"
    const val LESSON = "lesson/{lessonId}"
    fun lesson(id: String) = "lesson/$id"
    const val DIALOGUE = "dialogue/{dialogueId}"
    fun dialogue(id: String) = "dialogue/$id"
    const val QUIZ = "quiz"
    const val REVIEW = "review"
    const val DICTIONARY = "dictionary"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val GAME_MATCH = "game_match"
    const val GAME_GUESS = "game_guess"
    const val GAME_SPEED = "game_speed"

    val bottomTabs = listOf(HOME, WORDS, LIBRARY, GAMES, TOOLS)
}
