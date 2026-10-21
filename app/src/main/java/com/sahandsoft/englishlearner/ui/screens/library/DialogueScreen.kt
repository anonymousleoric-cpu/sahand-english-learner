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
import com.sahandsoft.englishlearner.data.db.DialogueEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.data.seed.ContentJson
import com.sahandsoft.englishlearner.data.seed.DialogueLineDto
import com.sahandsoft.englishlearner.data.seed.DialoguePracticeDto
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

@HiltViewModel
class DialogueViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dialogueId: String = savedStateHandle.get<String>("dialogueId") ?: ""

    data class Loaded(
        val dialogue: DialogueEntity,
        val lines: List<DialogueLineDto>,
        val practice: List<DialoguePracticeDto>
    )

    private val _data = MutableStateFlow<Loaded?>(null)
    val data: StateFlow<Loaded?> = _data.asStateFlow()

    private val _revealed = MutableStateFlow<Set<Int>>(emptySet())
    val revealed: StateFlow<Set<Int>> = _revealed.asStateFlow()

    init {
        viewModelScope.launch {
            val d = db.dialogueDao().get(dialogueId) ?: return@launch
            val lines = ContentJson.decodeFromString<List<DialogueLineDto>>(d.linesJson)
            val practice = ContentJson.decodeFromString<List<DialoguePracticeDto>>(d.practiceJson)
            progressRepository.addSeconds(20)
            _data.value = Loaded(d, lines, practice)
        }
    }

    fun reveal(i: Int) {
        _revealed.value = _revealed.value + i
    }

    fun speak(text: String) = tts.speak(text)

    fun playAll(lines: List<DialogueLineDto>) {
        tts.stop()
        lines.forEach { tts.speakQueued(it.en) }
    }
}

@Composable
fun DialogueScreen(nav: NavHostController, vm: DialogueViewModel = hiltViewModel()) {
    val data by vm.data.collectAsState()
    val revealed by vm.revealed.collectAsState()
    val d = data

    if (d == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(
            title = d.dialogue.titleFa,
            onBack = { nav.popBackStack() },
            actions = {
                IconButton(onClick = { vm.playAll(d.lines) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "پخش کل گفت‌وگو", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.dialogue.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    EnText(d.dialogue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    LevelBadge(d.dialogue.level)
                }
            }
            item { SectionHeader("گفت‌وگو") }
            itemsIndexed(d.lines, key = { i, _ -> "l$i" }) { i, line ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            line.role,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EnText(line.en, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { vm.speak(line.en) }) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(line.fa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionHeader("تمرین گفت‌وگو") }
            itemsIndexed(d.practice, key = { i, _ -> "p$i" }) { i, p ->
                val isRevealed = revealed.contains(i)
                Card(onClick = { vm.reveal(i) }) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(p.fa, style = MaterialTheme.typography.bodyLarge)
                        if (isRevealed) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EnText(p.en, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { vm.speak(p.en) }) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Spacer(Modifier.height(6.dp))
                            Text("برای دیدن جملهٔ انگلیسی ضربه بزن", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.playAll(d.lines) }, modifier = Modifier.fillMaxWidth()) {
                    Text("▶ پخش کامل گفت‌وگو")
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
