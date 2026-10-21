package com.sahandsoft.englishlearner.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.sahandsoft.englishlearner.data.db.GrammarEntity
import com.sahandsoft.englishlearner.data.db.GrammarProgressEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.data.seed.ContentJson
import com.sahandsoft.englishlearner.data.seed.GrammarContentDto
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EnText
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
class LessonViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val lessonId: String = savedStateHandle.get<String>("lessonId") ?: ""

    data class Loaded(
        val lesson: GrammarEntity,
        val content: GrammarContentDto,
        val progress: GrammarProgressEntity?
    )

    private val _data = MutableStateFlow<Loaded?>(null)
    val data: StateFlow<Loaded?> = _data.asStateFlow()

    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()
    fun setTab(i: Int) { _tab.value = i }

    private val _quizAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quizAnswers: StateFlow<Map<Int, Int>> = _quizAnswers.asStateFlow()

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    init {
        viewModelScope.launch {
            val lesson = db.grammarDao().get(lessonId) ?: return@launch
            val content = ContentJson.decodeFromString<GrammarContentDto>(lesson.contentJson)
            val progress = db.grammarDao().progress(lessonId)
            progressRepository.addSeconds(30)
            _data.value = Loaded(lesson, content, progress)
        }
    }

    fun setQuizAnswer(i: Int, opt: Int) {
        if (!_quizSubmitted.value) _quizAnswers.value = _quizAnswers.value + (i to opt)
    }

    fun submitQuiz() {
        viewModelScope.launch {
            val d = _data.value ?: return@launch
            val score = d.content.quiz.withIndex().count { (i, q) -> _quizAnswers.value[i] == q.answer }
            val prev = d.progress
            db.grammarDao().upsertProgress(
                GrammarProgressEntity(
                    lessonId = d.lesson.id,
                    bestQuiz = max(score, prev?.bestQuiz ?: 0),
                    quizTotal = d.content.quiz.size,
                    done = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            progressRepository.recordQuiz("grammar", score, d.content.quiz.size)
            _quizScore.value = score
            _quizSubmitted.value = true
        }
    }

    fun speak(text: String) = tts.speak(text)
}

private val lessonTabs = listOf("آموزش", "تمرین", "آزمون")

@Composable
fun LessonScreen(nav: NavHostController, vm: LessonViewModel = hiltViewModel()) {
    val data by vm.data.collectAsState()
    val tab by vm.tab.collectAsState()
    val d = data

    if (d == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "${DayUtils.fa(d.lesson.lessonOrder)}. ${d.lesson.titleFa}", onBack = { nav.popBackStack() })
        TabRow(selectedTabIndex = tab) {
            lessonTabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { vm.setTab(i) }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> TeachTab(d, vm)
            1 -> PracticeTab(d, vm)
            else -> QuizTab(d, vm)
        }
    }
}

@Composable
private fun TeachTab(d: LessonViewModel.Loaded, vm: LessonViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(d.content.explanation, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("ساختار", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    EnText(d.content.structure, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("💡", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        d.content.tipFa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        item { SectionHeader("مثال‌ها") }
        itemsIndexed(d.content.examples) { _, ex ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        EnText(ex.en, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(ex.fa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.speak(ex.en) }) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PracticeTab(d: LessonViewModel.Loaded, vm: LessonViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("جای خالی") }
        itemsIndexed(d.content.fillBlank, key = { i, _ -> "fill$i" }) { i, ex ->
            ExerciseCard(
                key = "fill-$i",
                prompt = ex.prompt,
                promptFa = ex.promptFa,
                options = ex.options,
                answer = ex.answer,
                onSpeak = { vm.speak(ex.prompt) }
            )
        }
        item { SectionHeader("چهارگزینه‌ای") }
        itemsIndexed(d.content.mcq, key = { i, _ -> "mcq$i" }) { i, ex ->
            ExerciseCard(
                key = "mcq-$i",
                prompt = ex.prompt,
                promptFa = ex.promptFa,
                options = ex.options,
                answer = ex.answer,
                onSpeak = { vm.speak(ex.prompt) }
            )
        }
        item { SectionHeader("مرتب‌سازی جمله") }
        itemsIndexed(d.content.wordOrder, key = { i, _ -> "wo$i" }) { i, wo ->
            WordOrderCard(key = "wo-$i", fa = wo.fa, words = wo.words, answer = wo.answer)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ExerciseCard(
    key: String,
    prompt: String,
    promptFa: String?,
    options: List<String>,
    answer: Int,
    onSpeak: () -> Unit
) {
    var chosen by remember(key) { mutableStateOf<Int?>(null) }
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EnText(prompt, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                IconButton(onClick = onSpeak) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                }
            }
            promptFa?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { oi, opt ->
                val isCorrect = oi == answer
                val container = when {
                    chosen == null -> MaterialTheme.colorScheme.surfaceVariant
                    isCorrect -> MaterialTheme.colorScheme.primaryContainer
                    chosen == oi -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Card(
                    onClick = { if (chosen == null) chosen = oi },
                    colors = CardDefaults.cardColors(containerColor = container),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    EnText(opt, modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun WordOrderCard(key: String, fa: String, words: List<String>, answer: List<String>) {
    var used by remember(key) { mutableStateOf<List<Int>>(emptyList()) }
    var checked by remember(key) { mutableStateOf(false) }
    val built = used.map { words[it] }
    val correct = built == answer

    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(fa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (built.isEmpty()) {
                        EnText("…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(built.joinToString(" "), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // کلمات باهم‌ریخته
            val shuffled = remember(key) { words.indices.shuffled() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                shuffled.chunked(3).forEach { rowIndices ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowIndices.forEach { idx ->
                            val isUsed = used.contains(idx)
                            OutlinedButton(
                                onClick = { if (!checked && !isUsed) used = used + idx },
                                enabled = !isUsed
                            ) {
                                EnText(words[idx], style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { used = emptyList(); checked = false }) { Text("از نو") }
                Button(onClick = { checked = true }, enabled = used.size == answer.size) { Text("بررسی") }
            }
            if (checked) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (correct) "✅ درست بود!" else "❌ پاسخ درست: ${answer.joinToString(" ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuizTab(d: LessonViewModel.Loaded, vm: LessonViewModel) {
    val quizAnswers by vm.quizAnswers.collectAsState()
    val quizSubmitted by vm.quizSubmitted.collectAsState()
    val quizScore by vm.quizScore.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(d.content.quiz, key = { i, _ -> "q$i" }) { qi, q ->
            Column {
                Text(q.promptFa ?: q.prompt, style = MaterialTheme.typography.titleSmall)
                if (!q.promptFa.isNullOrBlank()) {
                    EnText(q.prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                        onClick = { vm.setQuizAnswer(qi, oi) },
                        colors = CardDefaults.cardColors(containerColor = container),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        EnText(opt, modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            if (!quizSubmitted) {
                Button(onClick = vm::submitQuiz, modifier = Modifier.fillMaxWidth()) { Text("ثبت آزمون") }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "نمره: ${DayUtils.fa(quizScore)} از ${DayUtils.fa(d.content.quiz.size)}",
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
