package com.sahandsoft.englishlearner.ui.screens.games

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
import androidx.compose.runtime.LaunchedEffect
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
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.domain.QuizEngine
import com.sahandsoft.englishlearner.domain.QuizQuestion
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedGameViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val durationSeconds = 60

    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions: StateFlow<List<QuizQuestion>> = _questions.asStateFlow()

    private val _qIndex = MutableStateFlow(0)
    val qIndex: StateFlow<Int> = _qIndex.asStateFlow()

    private val _chosen = MutableStateFlow<Int?>(null)
    val chosen: StateFlow<Int?> = _chosen.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _correct = MutableStateFlow(0)
    private val _answered = MutableStateFlow(0)

    private val _remaining = MutableStateFlow(durationSeconds)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        viewModelScope.launch {
            val words = db.wordDao().all()
            _questions.value = QuizEngine.generate(words, null, 200)
            if (_questions.value.isNotEmpty()) {
                _running.value = true
                tickLoop()
            }
        }
    }

    private fun tickLoop() {
        viewModelScope.launch {
            while (_remaining.value > 0) {
                delay(1000)
                _remaining.value -= 1
            }
            endGame()
        }
    }

    fun answer(optionIndex: Int) {
        if (!_running.value || _chosen.value != null) return
        val q = _questions.value.getOrNull(_qIndex.value) ?: return
        _chosen.value = optionIndex
        _answered.value += 1
        val correct = optionIndex == q.answer
        _score.value += if (correct) 10 else -5
        if (correct) _correct.value += 1
        viewModelScope.launch {
            delay(500)
            _chosen.value = null
            val next = _qIndex.value + 1
            if (next >= _questions.value.size) {
                endGame()
            } else {
                _qIndex.value = next
            }
        }
    }

    private fun endGame() {
        if (_finished.value) return
        _running.value = false
        _finished.value = true
        _remaining.value = 0
        viewModelScope.launch {
            progressRepository.recordQuiz("speed", _correct.value, _answered.value)
        }
    }
}

@Composable
fun SpeedGameScreen(nav: NavHostController, vm: SpeedGameViewModel = hiltViewModel()) {
    val questions by vm.questions.collectAsState()
    val qIndex by vm.qIndex.collectAsState()
    val chosen by vm.chosen.collectAsState()
    val score by vm.score.collectAsState()
    val remaining by vm.remaining.collectAsState()
    val finished by vm.finished.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "⚡ آزمون برقی", onBack = { nav.popBackStack() })

        if (questions.isEmpty()) {
            EmptyView("⚡", "در حال آماده‌سازی سؤال‌ها…")
            return
        }

        if (finished) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🏁", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(12.dp))
                Text("زمان تمام شد!", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "امتیاز: ${DayUtils.fa(score)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("بازگشت") }
                }
            }
            return
        }

        val q = questions[qIndex.coerceIn(0, questions.size - 1)]

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⏱ ${DayUtils.fa(remaining)} ثانیه",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (remaining <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "امتیاز: ${DayUtils.fa(score)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { remaining / 60f },
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
                    EnText(q.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            q.options.forEachIndexed { oi, opt ->
                val isCorrect = oi == q.answer
                val container = when {
                    chosen == null -> MaterialTheme.colorScheme.surface
                    isCorrect -> MaterialTheme.colorScheme.primaryContainer
                    chosen == oi -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                Card(
                    onClick = { vm.answer(oi) },
                    colors = CardDefaults.cardColors(containerColor = container),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    if (q.type == "fa2en") {
                        EnText(opt, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text(opt, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
