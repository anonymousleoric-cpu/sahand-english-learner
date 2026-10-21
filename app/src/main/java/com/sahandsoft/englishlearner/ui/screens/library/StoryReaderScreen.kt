package com.sahandsoft.englishlearner.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.StoryEntity
import com.sahandsoft.englishlearner.data.db.StoryProgressEntity
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.data.seed.ContentJson
import com.sahandsoft.englishlearner.data.seed.StoryQuestionDto
import com.sahandsoft.englishlearner.data.seed.StorySectionDto
import com.sahandsoft.englishlearner.data.seed.VocabDto
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.LevelBadge
import com.sahandsoft.englishlearner.ui.components.SectionHeader
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class StoryReaderViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val storyId: String = savedStateHandle.get<String>("storyId") ?: ""

    data class Loaded(
        val story: StoryEntity,
        val sections: List<StorySectionDto>,
        val vocab: List<VocabDto>,
        val questions: List<StoryQuestionDto>,
        val progress: StoryProgressEntity?
    )

    private val _data = MutableStateFlow<Loaded?>(null)
    val data: StateFlow<Loaded?> = _data.asStateFlow()

    private val _quizAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quizAnswers: StateFlow<Map<Int, Int>> = _quizAnswers.asStateFlow()

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    init {
        viewModelScope.launch {
            val story = db.storyDao().get(storyId) ?: return@launch
            val sections = ContentJson.decodeFromString<List<StorySectionDto>>(story.sectionsJson)
            val vocab = ContentJson.decodeFromString<List<VocabDto>>(story.vocabJson)
            val questions = ContentJson.decodeFromString<List<StoryQuestionDto>>(story.questionsJson)
            val progress = db.storyDao().progress(storyId)
            progressRepository.addSeconds(45)
            _data.value = Loaded(story, sections, vocab, questions, progress)
        }
    }

    fun setAnswer(q: Int, opt: Int) {
        if (!_quizSubmitted.value) _quizAnswers.value = _quizAnswers.value + (q to opt)
    }

    fun submitQuiz() {
        viewModelScope.launch {
            val d = _data.value ?: return@launch
            val score = d.questions.withIndex().count { (i, q) -> _quizAnswers.value[i] == q.answer }
            val prev = d.progress
            db.storyDao().upsertProgress(
                StoryProgressEntity(
                    storyId = d.story.id,
                    bestScore = max(score, prev?.bestScore ?: 0),
                    questionCount = d.questions.size,
                    done = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            progressRepository.recordQuiz("story", score, d.questions.size)
            _quizScore.value = score
            _quizSubmitted.value = true
        }
    }

    fun speak(text: String) = tts.speak(text)

    fun playWholeStory() {
        val d = _data.value ?: return
        tts.stop()
        d.sections.forEach { s -> s.sentences.forEach { tts.speakQueued(it.en) } }
    }
}

@Composable
fun StoryReaderScreen(nav: NavHostController, vm: StoryReaderViewModel = hiltViewModel()) {
    val data by vm.data.collectAsState()
    val quizAnswers by vm.quizAnswers.collectAsState()
    val quizSubmitted by vm.quizSubmitted.collectAsState()
    val quizScore by vm.quizScore.collectAsState()
    var showFa by remember { mutableStateOf(true) }

    val d = data
    if (d == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(
            title = d.story.titleFa,
            onBack = { nav.popBackStack() },
            actions = {
                IconButton(onClick = vm::playWholeStory) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "خواندن کل داستان", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.story.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    LevelBadge(d.story.level)
                    Spacer(Modifier.width(8.dp))
                    Text("⏱ ${DayUtils.fa(d.story.minutes)} دقیقه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("نمایش ترجمهٔ فارسی", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Switch(checked = showFa, onCheckedChange = { showFa = it })
                }
            }

            d.sections.forEach { section ->
                item(key = "h-${section.titleFa ?: section.title ?: ""}") {
                    if (!section.titleFa.isNullOrBlank() || !section.title.isNullOrBlank()) {
                        SectionHeader(section.titleFa ?: section.title ?: "")
                    }
                }
                itemsIndexed(section.sentences, key = { i, s -> "$i-${s.en}" }) { _, sentence ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                EnText(
                                    sentence.en,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { vm.speak(sentence.en) }) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ جمله", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (showFa) {
                                Text(sentence.fa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (d.vocab.isNotEmpty()) {
                item { SectionHeader("واژه‌های این داستان") }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            d.vocab.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { v ->
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                EnText(v.en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(v.fa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { vm.speak(v.en) }, modifier = Modifier.height(28.dp)) {
                                                Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                                            }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("سؤال‌های درک مطلب") }
            itemsIndexed(d.questions, key = { i, _ -> "q$i" }) { qi, q ->
                Column {
                    Text(q.qFa ?: q.q, style = MaterialTheme.typography.titleSmall)
                    if (q.qFa.isNullOrBlank()) Unit else EnText(q.q, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    q.options.forEachIndexed { oi, opt ->
                        val chosen = quizAnswers[qi]
                        val isSelected = chosen == oi
                        val container = when {
                            quizSubmitted && oi == q.answer -> MaterialTheme.colorScheme.primaryContainer
                            quizSubmitted && isSelected && oi != q.answer -> MaterialTheme.colorScheme.errorContainer
                            isSelected -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            onClick = { vm.setAnswer(qi, oi) },
                            colors = CardDefaults.cardColors(containerColor = container),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            EnText(opt, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (quizSubmitted && !q.hintFa.isNullOrBlank()) {
                        Text("💡 ${q.hintFa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                if (!quizSubmitted) {
                    Button(onClick = vm::submitQuiz, modifier = Modifier.fillMaxWidth()) { Text("ثبت پاسخ‌ها") }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(
                            "نمرهٔ شما: ${DayUtils.fa(quizScore)} از ${DayUtils.fa(d.questions.size)}",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
