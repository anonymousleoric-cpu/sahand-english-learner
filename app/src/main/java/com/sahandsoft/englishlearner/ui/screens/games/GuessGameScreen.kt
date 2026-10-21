package com.sahandsoft.englishlearner.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
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

data class GuessRound(val word: WordEntity, val letters: List<Char>)

@HiltViewModel
class GuessGameViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val totalRounds = 5

    private val _rounds = MutableStateFlow<List<GuessRound>>(emptyList())
    val rounds: StateFlow<List<GuessRound>> = _rounds.asStateFlow()

    private val _roundIndex = MutableStateFlow(0)
    val roundIndex: StateFlow<Int> = _roundIndex.asStateFlow()

    private val _slots = MutableStateFlow<List<Char?>>(emptyList())
    val slots: StateFlow<List<Char?>> = _slots.asStateFlow()

    private val _used = MutableStateFlow<Set<Int>>(emptySet())
    val used: StateFlow<Set<Int>> = _used.asStateFlow()

    private val _mistakes = MutableStateFlow(0)
    val mistakes: StateFlow<Int> = _mistakes.asStateFlow()

    private val _solved = MutableStateFlow(0)
    val solved: StateFlow<Int> = _solved.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            val words = db.wordDao().all()
                .filter { it.en.length in 3..8 && it.en.all { ch -> ch.isLetter() } }
                .shuffled()
                .take(totalRounds)
            _rounds.value = words.map { w ->
                val base = w.en.uppercase().toList()
                val pool = ('A'..'Z').filter { !base.contains(it) }.shuffled().take(3)
                GuessRound(w, (base + pool).shuffled())
            }
            _roundIndex.value = 0
            _mistakes.value = 0
            _solved.value = 0
            _finished.value = false
            _started.value = true
            resetSlots()
        }
    }

    private fun resetSlots() {
        val r = _rounds.value.getOrNull(_roundIndex.value)
        _slots.value = r?.let { List(it.word.en.length) { null } } ?: emptyList()
        _used.value = emptySet()
    }

    fun tapLetter(letterIndex: Int) {
        val r = _rounds.value.getOrNull(_roundIndex.value) ?: return
        if (_used.value.contains(letterIndex) || _finished.value) return
        val slots = _slots.value.toMutableList()
        val emptyIdx = slots.indexOf(null)
        if (emptyIdx == -1) return
        slots[emptyIdx] = r.letters[letterIndex]
        _slots.value = slots
        _used.value = _used.value + letterIndex

        if (slots.none { it == null }) {
            val built = slots.filterNotNull().joinToString("")
            if (built == r.word.en.uppercase()) {
                _solved.value += 1
                viewModelScope.launch {
                    delay(600)
                    nextRound(true)
                }
            } else {
                _mistakes.value += 1
                viewModelScope.launch {
                    delay(700)
                    resetSlots()
                }
            }
        }
    }

    fun undo() {
        val slots = _slots.value.toMutableList()
        val lastFilled = slots.indexOfLast { it != null }
        if (lastFilled == -1) return
        val ch = slots[lastFilled]
        val r = _rounds.value.getOrNull(_roundIndex.value) ?: return
        val letterIdx = _used.value.firstOrNull { r.letters[it] == ch } ?: return
        slots[lastFilled] = null
        _slots.value = slots
        _used.value = _used.value - letterIdx
    }

    fun skip() {
        nextRound(false)
    }

    private fun nextRound(solvedNow: Boolean) {
        val next = _roundIndex.value + 1
        if (next >= _rounds.value.size) {
            _finished.value = true
            viewModelScope.launch { progressRepository.addSeconds(45, 5) }
        } else {
            _roundIndex.value = next
            resetSlots()
        }
    }
}

@Composable
fun GuessGameScreen(nav: NavHostController, vm: GuessGameViewModel = hiltViewModel()) {
    val rounds by vm.rounds.collectAsState()
    val roundIndex by vm.roundIndex.collectAsState()
    val slots by vm.slots.collectAsState()
    val used by vm.used.collectAsState()
    val mistakes by vm.mistakes.collectAsState()
    val solved by vm.solved.collectAsState()
    val finished by vm.finished.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "🔤 حدس واژه", onBack = { nav.popBackStack() })

        if (rounds.isEmpty()) {
            EmptyView("🔤", "در حال آماده‌سازی…")
            return
        }

        val round = rounds[roundIndex.coerceIn(0, rounds.size - 1)]

        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "واژهٔ ${DayUtils.fa(roundIndex + 1)} از ${DayUtils.fa(rounds.size)} • درست: ${DayUtils.fa(solved)} • خطا: ${DayUtils.fa(mistakes)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(round.word.fa, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    round.word.faPron?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // خانه‌های واژه
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEach { ch ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.size(44.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EnText(
                                ch?.toString() ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // کاشی‌های حروف (با اندیس واقعی)
            val letterTiles = remember(round.word.en) { round.letters.withIndex().toList() }
            letterTiles.chunked(4).forEach { rowTiles ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    rowTiles.forEach { (tileIndex, tileChar) ->
                        OutlinedButton(
                            onClick = { vm.tapLetter(tileIndex) },
                            enabled = !used.contains(tileIndex) && !finished,
                            modifier = Modifier.size(56.dp)
                        ) {
                            EnText(tileChar.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = vm::undo, modifier = Modifier.weight(1f)) { Text("↩ حذف آخرین") }
                OutlinedButton(onClick = vm::skip, modifier = Modifier.weight(1f)) { Text("رد کردن ⏭") }
            }

            if (finished) {
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉 تمام شد!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "${DayUtils.fa(solved)} از ${DayUtils.fa(rounds.size)} واژه درست ساخته شد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = vm::start, modifier = Modifier.weight(1f)) { Text("دوباره") }
                            OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("بازگشت") }
                        }
                    }
                }
            }
        }
    }
}
