package com.sahandsoft.englishlearner.ui.screens.tools

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.LevelBadge
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.screens.words.posFa
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val db: SahandDatabase,
    val tts: TtsManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<WordEntity>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            flow {
                val t = q.trim()
                emit(if (t.length < 2) emptyList() else db.wordDao().search(t))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wordCount: StateFlow<Int> = db.wordDao().observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val topics: StateFlow<List<TopicEntity>> = db.topicDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun speak(text: String) = tts.speak(text)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(nav: NavHostController, vm: DictionaryViewModel = hiltViewModel()) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val wordCount by vm.wordCount.collectAsState()
    val topics by vm.topics.collectAsState()
    var selected by remember { mutableStateOf<WordEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "🔍 فرهنگ لغت", onBack = { nav.popBackStack() })
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("جست‌وجوی انگلیسی یا فارسی…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true
        )
        Text(
            if (query.trim().length < 2) "${DayUtils.fa(wordCount)} واژه در بانک واژگان"
            else "${DayUtils.fa(results.size)} نتیجه",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (query.trim().length < 2) {
            EmptyView("🔍", "حداقل دو حرف بنویس…\nمی‌توانی انگلیسی یا فارسی جست‌وجو کنی.")
        } else if (results.isEmpty()) {
            EmptyView("😕", "چیزی پیدا نشد. کلمهٔ دیگری را امتحان کن.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { word ->
                    Card(onClick = { selected = word }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(word.emoji ?: "🔤", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                EnText(word.en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                word.ipa?.let {
                                    EnText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(word.fa, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    selected?.let { word ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
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
                    IconButton(onClick = { vm.speak(word.en) }) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(word.fa, style = MaterialTheme.typography.titleLarge)
                word.faPron?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val p = posFa(word.pos)
                if (p.isNotEmpty()) {
                    Text("نوع: $p", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val topicLabel = topics.firstOrNull { it.key == word.topic }?.titleFa ?: word.topic
                Text("موضوع: $topicLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}
