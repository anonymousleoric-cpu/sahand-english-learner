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
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewState(
    val queue: List<WordEntity> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val known: Int = 0,
    val again: Int = 0,
    val loaded: Boolean = false
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    val tts: TtsManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val words = progressRepository.dueWords()
            _state.value = ReviewState(queue = words.shuffled(), loaded = true)
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun speak() {
        _state.value.queue.getOrNull(_state.value.index)?.let { tts.speak(it.en) }
    }

    fun answer(correct: Boolean) {
        viewModelScope.launch {
            val s = _state.value
            val w = s.queue.getOrNull(s.index) ?: return@launch
            progressRepository.recordAnswer(w.id, correct)
            _state.value = s.copy(
                index = s.index + 1,
                revealed = false,
                known = s.known + if (correct) 1 else 0,
                again = s.again + if (correct) 0 else 1
            )
        }
    }
}

@Composable
fun ReviewScreen(nav: NavHostController, vm: ReviewViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "مرور هوشمند", onBack = { nav.popBackStack() })

        when {
            !state.loaded -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏳", style = MaterialTheme.typography.displayLarge)
                }
            }
            state.queue.isEmpty() -> {
                EmptyView("✅", "الان واژه‌ای برای مرور نداری!\nهر وقت با کارت‌واژه یا آزمون واژه‌ها را تمرین کنی، مرور بعدی اینجا ظاهر می‌شود.")
            }
            state.index >= state.queue.size -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", style = MaterialTheme.typography.displayLarge)
                    Text("مرور تمام شد!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "بلد بودی: ${DayUtils.fa(state.known)} • تکرار: ${DayUtils.fa(state.again)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { nav.popBackStack() }) { Text("بازگشت") }
                }
            }
            else -> {
                val word = state.queue[state.index]
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${DayUtils.fa(state.index + 1)} از ${DayUtils.fa(state.queue.size)}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "✅ ${DayUtils.fa(state.known)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (state.index + 1).toFloat() / state.queue.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(word.emoji ?: "🔤", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            EnText(word.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = vm::speak, modifier = Modifier.padding(top = 8.dp)) {
                                Text("🔊 تلفظ")
                            }
                            if (state.revealed) {
                                Spacer(Modifier.height(16.dp))
                                Text(word.fa, style = MaterialTheme.typography.headlineSmall)
                                word.faPron?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!word.example.isNullOrBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    EnText(word.example, style = MaterialTheme.typography.bodyLarge)
                                    word.exampleFa?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = vm::reveal) { Text("نمایش معنی") }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    if (state.revealed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { vm.answer(false) },
                                modifier = Modifier.weight(1f)
                            ) { Text("دوباره مرور کن 🔄") }
                            Button(
                                onClick = { vm.answer(true) },
                                modifier = Modifier.weight(1f)
                            ) { Text("بلدم ✅") }
                        }
                    }
                }
            }
        }
    }
}
