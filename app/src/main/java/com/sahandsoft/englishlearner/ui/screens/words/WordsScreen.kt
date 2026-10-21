package com.sahandsoft.englishlearner.ui.screens.words

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.TopicEntity
import com.sahandsoft.englishlearner.data.db.WordEntity
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.LevelBadge
import com.sahandsoft.englishlearner.ui.components.PillChip
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.nav.Routes
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordsViewModel @Inject constructor(
    db: SahandDatabase,
    val tts: TtsManager
) : ViewModel() {

    private val _topic = MutableStateFlow("all")
    val topic: StateFlow<String> = _topic.asStateFlow()

    val topics: StateFlow<List<TopicEntity>> = db.topicDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val words: StateFlow<List<WordEntity>> =
        combine(db.wordDao().observeAll(), _topic) { ws, t ->
            if (t == "all") ws else ws.filter { it.topic == t }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val learnedIds: StateFlow<Set<String>> = db.progressDao().observeLearnedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setTopic(t: String) {
        _topic.value = t
    }

    fun speak(text: String) = tts.speak(text)
}

fun posFa(pos: String?): String = when (pos) {
    "noun" -> "اسم"
    "adj" -> "صفت"
    "verb" -> "فعل"
    "adv" -> "قید"
    "phrase" -> "عبارت"
    "number" -> "عدد"
    null, "" -> ""
    else -> pos
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordsScreen(nav: NavHostController, vm: WordsViewModel = hiltViewModel()) {
    val topics by vm.topics.collectAsState()
    val words by vm.words.collectAsState()
    val learnedIds by vm.learnedIds.collectAsState()
    val currentTopic by vm.topic.collectAsState()
    var selected by remember { mutableStateOf<WordEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(
            title = "واژگان",
            actions = {
                IconButton(onClick = { nav.navigate(Routes.flashcards(currentTopic)) }) {
                    Icon(Icons.Rounded.Style, contentDescription = "کارت واژه")
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PillChip(label = "همه", emoji = "🗂️", selected = currentTopic == "all", onClick = { vm.setTopic("all") })
            }
            items(topics) { t ->
                PillChip(
                    label = t.titleFa,
                    emoji = t.emoji,
                    selected = currentTopic == t.key,
                    onClick = { vm.setTopic(t.key) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${words.size} واژه",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        if (words.isEmpty()) {
            EmptyView("🗂️", "واژه‌ای در این موضوع نیست.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(words, key = { it.id }) { word ->
                    WordCard(
                        word = word,
                        learned = learnedIds.contains(word.id),
                        onSpeak = { vm.speak(word.en) },
                        onClick = { selected = word }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    selected?.let { word ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            WordDetail(
                word = word,
                topicLabel = topics.firstOrNull { it.key == word.topic }?.titleFa ?: word.topic,
                onSpeak = { vm.speak(word.en) }
            )
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    learned: Boolean,
    onSpeak: () -> Unit,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(word.emoji ?: "🔤", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1.2f)) {
                EnText(word.en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    word.ipa?.let {
                        EnText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val p = posFa(word.pos)
                    if (p.isNotEmpty()) {
                        Text(
                            (if (word.ipa != null) " • " else "") + p,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(word.fa, style = MaterialTheme.typography.titleSmall)
                word.faPron?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (learned) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "یادگرفته‌شده",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WordDetail(word: WordEntity, topicLabel: String, onSpeak: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word.emoji ?: "🔤", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                EnText(word.en, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    word.ipa?.let {
                        EnText(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                    }
                    LevelBadge(word.level)
                }
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(word.fa, style = MaterialTheme.typography.titleLarge)
        val p = posFa(word.pos)
        if (p.isNotEmpty()) {
            Text(
                "نوع: $p",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "موضوع: $topicLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!word.example.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    EnText(word.example, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    word.exampleFa?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
