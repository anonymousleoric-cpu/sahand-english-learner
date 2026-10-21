package com.sahandsoft.englishlearner.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.domain.QuizEngine
import com.sahandsoft.englishlearner.domain.QuizQuestion
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.PillChip
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuizPhase {
    data object Setup : QuizPhase
    data object Running : QuizPhase
    data object Done : QuizPhase
}

data class QuizState(
    val phase: QuizPhase = QuizPhase.Setup,
    val level: String = "all",
    val count: Int = 10,
    val questions: List<QuizQuestion> = emptyList(),
    val qIndex: Int = 0,
    val selected: Int? = null,
    val correctCount: Int = 0,
    val wrongWordIds: List<String> = emptyList(),
    val wrongWords: List<WordEntity> = emptyList()
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager
) : ViewModel() {

    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    fun setLevel(level: String) = _state.update { it.copy(level = level) }
    fun setCount(count: Int) = _state.update { it.copy(count = count) }

    fun start() {
        viewModelScope.launch {
            val words = db.wordDao().all()
            val s = _state.value
            val questions = QuizEngine.generate(words, s.level, s.count)
            if (questions.isEmpty()) return@launch
            _state.update {
                it.copy(
                    phase = QuizPhase.Running,
                    questions = questions,
                    qIndex = 0,
                    selected = null,
                    correctCount = 0,
                    wrongWordIds = emptyList(),
                    wrongWords = emptyList()
                )
            }
        }
    }

    fun select(optionIndex: Int) {
        _state.update { if (it.selected == null) it.copy(selected = optionIndex) else it }
    }

    fun next() {
        viewModelScope.launch {
            val s = _state.value
            val q = s.questions.getOrNull(s.qIndex) ?: return@launch
            val correct = s.selected == q.answer
            progressRepository.recordAnswer(q.wordId, correct)
            val updated = s.copy(
                correctCount = s.correctCount + if (correct) 1 else 0,
                wrongWordIds = if (correct) s.wrongWordIds else s.wrongWordIds + q.wordId,
                qIndex = s.qIndex + 1,
                selected = null
            )
            if (updated.qIndex >= updated.questions.size) {
                progressRepository.recordQuiz("quiz", updated.correctCount, updated.questions.size)
                val wrong = if (updated.wrongWordIds.isEmpty()) emptyList() else db.wordDao().byIds(updated.wrongWordIds)
                _state.value = updated.copy(phase = QuizPhase.Done, wrongWords = wrong)
            } else {
                _state.value = updated
            }
        }
    }

    fun backToSetup() = _state.update { QuizState() }

    fun speakPrompt() {
        _state.value.questions.getOrNull(_state.value.qIndex)?.speakText?.let { tts.speak(it) }
    }
}

@Composable
fun QuizScreen(nav: NavHostController, vm: QuizViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(
            title = "آزمون",
            onBack = {
                if (state.phase == QuizPhase.Setup) nav.popBackStack() else vm.backToSetup()
            }
        )
        when (state.phase) {
            QuizPhase.Setup -> QuizSetup(vm)
            QuizPhase.Running -> QuizRunning(state, vm)
            QuizPhase.Done -> QuizDone(state, nav, vm)
        }
    }
}

@Composable
private fun QuizSetup(vm: QuizViewModel) {
    val state by vm.state.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("سطح را انتخاب کن", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip("همه", "🌍", state.level == "all") { vm.setLevel("all") }
            PillChip("پایه صفر", "🌱", state.level == "zero") { vm.setLevel("zero") }
            PillChip("A1", "📗", state.level == "a1") { vm.setLevel("a1") }
            PillChip("A2", "📘", state.level == "a2") { vm.setLevel("a2") }
        }
        Text("تعداد سؤال‌ها", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip("۵ سؤال", selected = state.count == 5) { vm.setCount(5) }
            PillChip("۱۰ سؤال", selected = state.count == 10) { vm.setCount(10) }
            PillChip("۲۰ سؤال", selected = state.count == 20) { vm.setCount(20) }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = vm::start, modifier = Modifier.fillMaxWidth()) { Text("شروع آزمون 🚀") }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                "انواع سؤال: معنی واژه، انتخاب واژه و تکمیل جای خالی. پاسخ‌های غلط دوباره برای مرور برنامه‌ریزی می‌شوند.",
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuizRunning(state: QuizState, vm: QuizViewModel) {
    val q = state.questions.getOrNull(state.qIndex) ?: return
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "سؤال ${DayUtils.fa(state.qIndex + 1)} از ${DayUtils.fa(state.questions.size)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                "درست: ${DayUtils.fa(state.correctCount)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.qIndex + 1).toFloat() / state.questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    when (q.type) {
                        "en2fa" -> "معنی این واژه چیست؟"
                        "fa2en" -> "واژهٔ درست کدام است؟"
                        else -> "جای خالی را کامل کن"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                if (q.type == "fa2en") {
                    Text(q.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    EnText(q.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                q.promptFa?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (q.speakText != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = vm::speakPrompt) { Text("🔊 بشنو") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        q.options.forEachIndexed { oi, opt ->
            val isCorrect = oi == q.answer
            val container = when {
                state.selected == null -> MaterialTheme.colorScheme.surface
                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                state.selected == oi -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Card(
                onClick = { vm.select(oi) },
                colors = CardDefaults.cardColors(containerColor = container),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                if (q.type == "en2fa") {
                    Text(opt, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                } else {
                    EnText(opt, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (state.selected != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.qIndex + 1 >= state.questions.size) "دیدن نتیجه" else "سؤال بعدی")
            }
        }
    }
}

@Composable
private fun QuizDone(state: QuizState, nav: NavHostController, vm: QuizViewModel) {
    val total = state.questions.size
    val frac = if (total > 0) state.correctCount.toFloat() / total else 0f
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (frac >= 0.8f) "🏆 عالی بود!"
            else if (frac >= 0.5f) "👍 خوب بود"
            else "💪 ادامه بده!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${DayUtils.fa(state.correctCount)} از ${DayUtils.fa(total)}",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        if (state.wrongWords.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("واژه‌هایی که باید بیشتر تمرین کنی:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(8.dp))
                    state.wrongWords.forEach { w ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            EnText(w.en, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(w.fa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = vm::backToSetup, modifier = Modifier.weight(1f)) { Text("آزمون جدید") }
            OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("بازگشت") }
        }
    }
}
