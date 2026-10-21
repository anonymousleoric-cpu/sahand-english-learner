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
class MatchGameViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _pairs = MutableStateFlow<List<WordEntity>>(emptyList())
    val pairs: StateFlow<List<WordEntity>> = _pairs.asStateFlow()

    private val _faOrder = MutableStateFlow<List<Int>>(emptyList())
    val faOrder: StateFlow<List<Int>> = _faOrder.asStateFlow()

    private val _selectedEn = MutableStateFlow<Int?>(null)
    val selectedEn: StateFlow<Int?> = _selectedEn.asStateFlow()

    private val _selectedFa = MutableStateFlow<Int?>(null)
    val selectedFa: StateFlow<Int?> = _selectedFa.asStateFlow()

    private val _matched = MutableStateFlow<Set<Int>>(emptySet())
    val matched: StateFlow<Set<Int>> = _matched.asStateFlow()

    private val _mistakes = MutableStateFlow(0)
    val mistakes: StateFlow<Int> = _mistakes.asStateFlow()

    private val _startedAt = MutableStateFlow(0L)
    private val _elapsed = MutableStateFlow(0L)
    val elapsed: StateFlow<Long> = _elapsed.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            val ws = db.wordDao().all().shuffled().take(6)
            _pairs.value = ws
            _faOrder.value = ws.indices.shuffled()
            _selectedEn.value = null
            _selectedFa.value = null
            _matched.value = emptySet()
            _mistakes.value = 0
            _finished.value = false
            _elapsed.value = 0
            _startedAt.value = System.currentTimeMillis()
            _started.value = true
        }
    }

    fun tick() {
        if (!_finished.value && _started.value) {
            _elapsed.value = (System.currentTimeMillis() - _startedAt.value) / 1000
        }
    }

    fun pickEn(pairId: Int) {
        if (_matched.value.contains(pairId) || _finished.value) return
        _selectedEn.value = pairId
        tryMatch()
    }

    fun pickFa(pairId: Int) {
        if (_matched.value.contains(pairId) || _finished.value) return
        _selectedFa.value = pairId
        tryMatch()
    }

    private fun tryMatch() {
        val en = _selectedEn.value ?: return
        val fa = _selectedFa.value ?: return
        if (en == fa) {
            _matched.value = _matched.value + en
            _selectedEn.value = null
            _selectedFa.value = null
            if (_matched.value.size == _pairs.value.size) {
                _finished.value = true
                _elapsed.value = (System.currentTimeMillis() - _startedAt.value) / 1000
                viewModelScope.launch { progressRepository.addSeconds(60, 6) }
            }
        } else {
            _mistakes.value += 1
            viewModelScope.launch {
                delay(450)
                _selectedEn.value = null
                _selectedFa.value = null
            }
        }
    }
}

@Composable
fun MatchGameScreen(nav: NavHostController, vm: MatchGameViewModel = hiltViewModel()) {
    val pairs by vm.pairs.collectAsState()
    val faOrder by vm.faOrder.collectAsState()
    val selectedEn by vm.selectedEn.collectAsState()
    val selectedFa by vm.selectedFa.collectAsState()
    val matched by vm.matched.collectAsState()
    val mistakes by vm.mistakes.collectAsState()
    val elapsed by vm.elapsed.collectAsState()
    val finished by vm.finished.collectAsState()
    val started by vm.started.collectAsState()

    androidx.compose.runtime.LaunchedEffect(started) {
        while (started && !finished) {
            delay(1000)
            vm.tick()
        }
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "🧩 جورچین واژه‌ها", onBack = { nav.popBackStack() })

        if (pairs.isEmpty()) {
            EmptyView("🧩", "در حال آماده‌سازی…")
            return
        }

        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row {
                Text(
                    "⏱ ${DayUtils.fa(elapsed.toInt())} ثانیه",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text("خطاها: ${DayUtils.fa(mistakes)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("انگلیسی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    pairs.forEachIndexed { wIdx, w ->
                        val isMatched = matched.contains(wIdx)
                        val isSelected = selectedEn == wIdx
                        MatchTile(
                            text = w.en,
                            isEn = true,
                            state = when {
                                isMatched -> TileState.MATCHED
                                isSelected -> TileState.SELECTED
                                else -> TileState.IDLE
                            },
                            onClick = { vm.pickEn(wIdx) }
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("فارسی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    faOrder.forEach { idx ->
                        val w = pairs[idx]
                        val isMatched = matched.contains(idx)
                        val isSelected = selectedFa == idx
                        MatchTile(
                            text = w.fa,
                            isEn = false,
                            state = when {
                                isMatched -> TileState.MATCHED
                                isSelected -> TileState.SELECTED
                                else -> TileState.IDLE
                            },
                            onClick = { vm.pickFa(idx) }
                        )
                    }
                }
            }

            if (finished) {
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉 آفرین!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "زمان: ${DayUtils.fa(elapsed.toInt())} ثانیه • خطاها: ${DayUtils.fa(mistakes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = vm::start, modifier = Modifier.weight(1f)) { Text("دوباره بازی کن") }
                            OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.weight(1f)) { Text("بازگشت") }
                        }
                    }
                }
            }
        }
    }
}

enum class TileState { IDLE, SELECTED, MATCHED }

@Composable
private fun MatchTile(text: String, isEn: Boolean, state: TileState, onClick: () -> Unit) {
    val container = when (state) {
        TileState.MATCHED -> MaterialTheme.colorScheme.primaryContainer
        TileState.SELECTED -> MaterialTheme.colorScheme.secondaryContainer
        TileState.IDLE -> MaterialTheme.colorScheme.surface
    }
    Card(
        onClick = onClick,
        enabled = state != TileState.MATCHED,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isEn) {
            EnText(text, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        } else {
            Text(text, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
