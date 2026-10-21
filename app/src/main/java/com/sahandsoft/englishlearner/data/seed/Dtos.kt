package com.sahandsoft.englishlearner.data.seed

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** نمونهٔ مشترک Json برای همهٔ محتواها */
val ContentJson = Json { ignoreUnknownKeys = true }

/* DTOهای محتوا — دقیقاً متناظر با فایل‌های JSON در assets/seed */

@Serializable
data class WordDto(
    val en: String,
    val fa: String,
    val ipa: String? = null,
    val faPron: String? = null,
    val pos: String? = null,
    val emoji: String? = null,
    val example: String? = null,
    val exampleFa: String? = null,
    val topic: String = "",
    val level: String = "zero"
)

@Serializable
data class TopicDto(val key: String, val titleFa: String, val emoji: String)

@Serializable
data class StorySentenceDto(val en: String, val fa: String)

@Serializable
data class StorySectionDto(
    val title: String? = null,
    val titleFa: String? = null,
    val sentences: List<StorySentenceDto> = emptyList()
)

@Serializable
data class StoryQuestionDto(
    val q: String,
    val qFa: String? = null,
    val options: List<String> = emptyList(),
    val answer: Int = 0,
    val hintFa: String? = null
)

@Serializable
data class VocabDto(val en: String, val fa: String, val note: String? = null)

@Serializable
data class StoryDto(
    val id: String,
    val title: String,
    val titleFa: String,
    val level: String = "zero",
    val topic: String = "",
    val emoji: String = "📖",
    val minutes: Int = 2,
    val sections: List<StorySectionDto> = emptyList(),
    val vocab: List<VocabDto> = emptyList(),
    val questions: List<StoryQuestionDto> = emptyList()
)

@Serializable
data class BookExerciseDto(
    val type: String = "mcq",
    val prompt: String = "",
    val promptFa: String? = null,
    val options: List<String> = emptyList(),
    val answer: Int = 0,
    val answerText: String? = null
)

@Serializable
data class BookPageContentDto(
    val bodyEn: List<String> = emptyList(),
    val bodyFa: List<String> = emptyList(),
    val vocab: List<VocabDto> = emptyList(),
    val exercise: BookExerciseDto? = null
)

@Serializable
data class BookPageDto(
    val number: Int,
    val chapter: Int = 1,
    val title: String = "",
    val titleFa: String = "",
    val kind: String = "reading",
    val content: BookPageContentDto = BookPageContentDto()
)

@Serializable
data class BookDto(
    val id: String,
    val title: String,
    val titleFa: String,
    val level: String = "zero",
    val descFa: String = "",
    val emoji: String = "📚",
    val color: String = "",
    val pages: List<BookPageDto> = emptyList()
)

@Serializable
data class GrammarExerciseDto(
    val prompt: String = "",
    val promptFa: String? = null,
    val options: List<String> = emptyList(),
    val answer: Int = 0
)

@Serializable
data class GrammarWordOrderDto(
    val fa: String,
    val words: List<String> = emptyList(),
    val answer: List<String> = emptyList()
)

@Serializable
data class GrammarQuizItemDto(
    val prompt: String,
    val promptFa: String? = null,
    val options: List<String> = emptyList(),
    val answer: Int = 0
)

@Serializable
data class GrammarContentDto(
    val explanation: String = "",
    val structure: String = "",
    val tipFa: String = "",
    val examples: List<VocabDto> = emptyList(),
    val fillBlank: List<GrammarExerciseDto> = emptyList(),
    val mcq: List<GrammarExerciseDto> = emptyList(),
    val wordOrder: List<GrammarWordOrderDto> = emptyList(),
    val quiz: List<GrammarQuizItemDto> = emptyList()
)

@Serializable
data class GrammarDto(
    val id: String,
    val title: String,
    val titleFa: String,
    val order: Int = 0,
    val emoji: String = "📘",
    val level: String = "zero",
    val content: GrammarContentDto = GrammarContentDto()
)

@Serializable
data class DialogueLineDto(val role: String, val en: String, val fa: String)

@Serializable
data class DialoguePracticeDto(
    val fa: String,
    val en: String,
    val options: List<String> = emptyList()
)

@Serializable
data class DialogueDto(
    val id: String,
    val title: String,
    val titleFa: String,
    val emoji: String = "💬",
    val level: String = "zero",
    val lines: List<DialogueLineDto> = emptyList(),
    val practice: List<DialoguePracticeDto> = emptyList()
)
