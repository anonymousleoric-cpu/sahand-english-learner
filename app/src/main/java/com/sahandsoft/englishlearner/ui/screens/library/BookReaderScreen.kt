package com.sahandsoft.englishlearner.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.BookEntity
import com.sahandsoft.englishlearner.data.db.BookPageEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.data.seed.BookPageContentDto
import com.sahandsoft.englishlearner.data.seed.ContentJson
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.KindBadge
import com.sahandsoft.englishlearner.ui.components.MiniBar
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    private val db: SahandDatabase,
    private val progressRepository: ProgressRepository,
    val tts: TtsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = savedStateHandle.get<String>("bookId") ?: ""

    data class Loaded(val book: BookEntity, val pages: List<BookPageEntity>, val lastPage: Int)

    private val _data = MutableStateFlow<Loaded?>(null)
    val data: StateFlow<Loaded?> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            val book = db.bookDao().get(bookId) ?: return@launch
            val pages = db.bookDao().pages(bookId)
            val progress = db.bookDao().progress(bookId)
            val last = (progress?.lastPage ?: 1).coerceIn(1, pages.size.coerceAtLeast(1))
            _data.value = Loaded(book, pages, last)
        }
    }

    fun onPageSettled(pageIndex: Int) {
        viewModelScope.launch {
            db.bookDao().upsertProgress(BookProgressEntity(bookId, pageIndex + 1, System.currentTimeMillis()))
            progressRepository.addSeconds(30)
        }
    }

    fun speak(text: String) = tts.speak(text)
}

@Composable
fun BookReaderScreen(nav: NavHostController, vm: BookReaderViewModel = hiltViewModel()) {
    val data by vm.data.collectAsState()
    val loaded = data

    if (loaded == null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = loaded.lastPage - 1) { loaded.pages.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { vm.onPageSettled(it) }
    }

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = loaded.book.titleFa, onBack = { nav.popBackStack() })

        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "صفحهٔ ${DayUtils.fa(pagerState.currentPage + 1)} از ${DayUtils.fa(loaded.pages.size)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                MiniBar((pagerState.currentPage + 1).toFloat() / loaded.pages.size, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = loaded.pages[pageIndex]
            BookPageView(page = page, vm = vm)
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
                modifier = Modifier.weight(1f),
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                Text("قبلی")
            }
            androidx.compose.material3.Button(
                onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(loaded.pages.size - 1)) } },
                modifier = Modifier.weight(1f),
                enabled = pagerState.currentPage < loaded.pages.size - 1
            ) {
                Text("بعدی")
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
            }
        }
    }
}

@Composable
private fun BookPageView(page: BookPageEntity, vm: BookReaderViewModel) {
    val content = remember(page.id) {
        runCatching { ContentJson.decodeFromString<BookPageContentDto>(page.contentJson) }
            .getOrDefault(BookPageContentDto())
    }
    var chosen by remember(page.id) { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindBadge(page.kind)
            Spacer(Modifier.width(8.dp))
            Text(page.titleFa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        when (page.kind) {
            "vocab" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.bodyEn.forEachIndexed { i, line ->
                        Column {
                            EnText(line, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            content.bodyFa.getOrNull(i)?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                content.vocab.forEach { v ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                EnText(v.en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(v.fa, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { vm.speak(v.en) }) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            "dialogue" -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content.bodyEn.forEachIndexed { i, line ->
                        val faLine = content.bodyFa.getOrNull(i) ?: ""
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EnText(line, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = { vm.speak(line) }) {
                                        Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (faLine.isNotBlank()) {
                                    Text(faLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            "exercise" -> {
                val ex = content.exercise
                if (ex == null) {
                    EnText("", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("تمرین", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(6.dp))
                            EnText(ex.prompt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            ex.promptFa?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(10.dp))
                            ex.options.forEachIndexed { oi, opt ->
                                val isCorrect = oi == ex.answer
                                val container = when {
                                    chosen == null -> MaterialTheme.colorScheme.surfaceVariant
                                    isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                    chosen == oi -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Card(
                                    onClick = { if (chosen == null) chosen = oi },
                                    colors = CardDefaults.cardColors(containerColor = container),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    EnText(opt, modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (chosen != null && ex.type == "fill" && !ex.answerText.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "پاسخ درست: ${ex.answerText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                content.bodyEn.forEachIndexed { i, line ->
                    EnText(line, style = MaterialTheme.typography.bodyLarge)
                    content.bodyFa.getOrNull(i)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                content.vocab.forEach { v ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EnText(v.en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(v.fa, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { vm.speak(v.en) }) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "تلفظ", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
