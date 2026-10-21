package com.sahandsoft.englishlearner.ui.screens.words

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.MiniBar
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val category: String = savedStateHandle.get<String>("category") ?: "all"

    private val _deck = MutableStateFlow<List<WordEntity>>(emptyList())
    val deck: StateFlow<List<WordEntity>> = _deck.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _flipped = MutableStateFlow(false)
    val flipped: StateFlow<Boolean> = _flipped.asStateFlow()

    private val _known = MutableStateFlow(0)
    val known: StateFlow<Int> = _known.asStateFlow()

    private val _unknown = MutableStateFlow(0)
    val unknown: StateFlow<Int> = _unknown.asStateFlow()

    init {
        viewModelScope.launch {
            val all = db.wordDao().all()
            val filtered = if (category == "all") all else all.filter { it.topic == category }
            _deck.value = filtered.shuffled().take(20)
        }
    }

    fun flip() {
        _flipped.value = !_flipped.value
    }

    fun speakCurrent() {
        _deck.value.getOrNull(_index.value)?.let { tts.speak(it.en) }
    }

    fun answer(correct: Boolean) {
        viewModelScope.launch {
            val word = _deck.value.getOrNull(_index.value) ?: return@launch
            progressRepository.recordAnswer(word.id, correct)
            if (correct) _known.value += 1 else _unknown.value += 1
            _index.value += 1
            _flipped.value = false
        }
    }
}

@Composable
fun FlashcardsScreen(nav: NavHostController, vm: FlashcardsViewModel = hiltViewModel()) {
    val deck by vm.deck.collectAsState()
    val index by vm.index.collectAsState()
    val flipped by vm.flipped.collectAsState()
    val known by vm.known.collectAsState()
    val unknown by vm.unknown.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "کارت واژه", onBack = { nav.popBackStack() })

        when {
            deck.isEmpty() -> EmptyView("🎴", "کارت‌ها در حال آماده‌سازی…")
            index >= deck.size -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("جلسه تمام شد!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("بلدم", style = MaterialTheme.typography.titleSmall)
                                Text(DayUtils.fa(known), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("نمی‌دانستم", style = MaterialTheme.typography.titleSmall)
                                Text(DayUtils.fa(unknown), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { nav.popBackStack() }) { Text("بازگشت") }
                }
            }
            else -> {
                val word = deck[index]
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${DayUtils.fa(index + 1)} از ${DayUtils.fa(deck.size)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        MiniBar(index.toFloat() / deck.size, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))

                    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")
                    Card(
                        onClick = vm::flip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (rotation <= 90f) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Text(word.emoji ?: "🔤", style = MaterialTheme.typography.displayLarge)
                                    Spacer(Modifier.height(12.dp))
                                    EnText(word.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    word.ipa?.let {
                                        Spacer(Modifier.height(4.dp))
                                        EnText(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("برای دیدن معنی ضربه بزن", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp).graphicsLayer { rotationY = 180f }) {
                                    Text(word.fa, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    word.faPron?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!word.example.isNullOrBlank()) {
                                        Spacer(Modifier.height(16.dp))
                                        EnText(word.example, style = MaterialTheme.typography.bodyLarge)
                                        word.exampleFa?.let {
                                            Spacer(Modifier.height(4.dp))
                                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = vm::speakCurrent, modifier = Modifier.fillMaxWidth()) {
                        Text("🔊 تلفظ")
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { vm.answer(false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("نمی‌دانم ❌") }
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
