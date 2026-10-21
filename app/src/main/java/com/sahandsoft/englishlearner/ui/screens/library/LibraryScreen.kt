package com.sahandsoft.englishlearner.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.db.BookEntity
import com.sahandsoft.englishlearner.data.db.DialogueEntity
import com.sahandsoft.englishlearner.data.db.GrammarEntity
import com.sahandsoft.englishlearner.data.db.SahandDatabase
import com.sahandsoft.englishlearner.data.db.StoryEntity
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.EmptyView
import com.sahandsoft.englishlearner.ui.components.EnText
import com.sahandsoft.englishlearner.ui.components.LevelBadge
import com.sahandsoft.englishlearner.ui.components.MiniBar
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.components.bookGradientColors
import com.sahandsoft.englishlearner.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    db: SahandDatabase
) : ViewModel() {
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()
    fun setTab(i: Int) { _tab.value = i }

    val stories = db.storyDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val storyProgress = db.storyDao().observeAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val books = db.bookDao().observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bookProgress = db.bookDao().observeAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val grammar = db.grammarDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val grammarProgress = db.grammarDao().observeAllProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dialogues = db.dialogueDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private val libraryTabs = listOf("داستان‌ها", "کتاب‌ها", "دیالوگ‌ها", "گرامر")

@Composable
fun LibraryScreen(nav: NavHostController, vm: LibraryViewModel = hiltViewModel()) {
    val tab by vm.tab.collectAsState()
    val stories by vm.stories.collectAsState()
    val storyProgress by vm.storyProgress.collectAsState()
    val books by vm.books.collectAsState()
    val bookProgress by vm.bookProgress.collectAsState()
    val grammar by vm.grammar.collectAsState()
    val grammarProgress by vm.grammarProgress.collectAsState()
    val dialogues by vm.dialogues.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "کتابخانه")
        TabRow(selectedTabIndex = tab) {
            libraryTabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { vm.setTab(i) }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> StoriesTab(stories, storyProgress.associate { it.storyId to it }) { nav.navigate(Routes.story(it)) }
            1 -> BooksTab(books, bookProgress.associate { it.bookId to it }) { nav.navigate(Routes.book(it)) }
            2 -> DialoguesTab(dialogues) { nav.navigate(Routes.dialogue(it)) }
            else -> GrammarTab(grammar, grammarProgress.associate { it.lessonId to it }) { nav.navigate(Routes.lesson(it)) }
        }
    }
}

@Composable
private fun StoriesTab(
    stories: List<StoryEntity>,
    progress: Map<String, com.sahandsoft.englishlearner.data.db.StoryProgressEntity>,
    onOpen: (String) -> Unit
) {
    if (stories.isEmpty()) { EmptyView("📖", "داستانی یافت نشد."); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            val p = progress[story.id]
            Card(onClick = { onOpen(story.id) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(story.emoji, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(story.titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        EnText(story.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LevelBadge(story.level)
                            Spacer(Modifier.width(8.dp))
                            Text("⏱ ${DayUtils.fa(story.minutes)} دقیقه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (p?.done == true) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "انجام شد", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun BooksTab(
    books: List<BookEntity>,
    progress: Map<String, com.sahandsoft.englishlearner.data.db.BookProgressEntity>,
    onOpen: (String) -> Unit
) {
    if (books.isEmpty()) { EmptyView("📚", "کتابی یافت نشد."); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(books, key = { it.id }) { book ->
            val p = progress[book.id]
            val readPages = ((p?.lastPage ?: 1) - 1).coerceAtLeast(0)
            val (c1, c2) = bookGradientColors(book.color)
            Card(onClick = { onOpen(book.id) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Brush.horizontalGradient(listOf(c1, c2)), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(book.emoji, style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.titleFa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            EnText(book.title, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
                        }
                        LevelBadge(book.level)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(book.descFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    MiniBar(readPages / 50f)
                    Text(
                        "خوانده‌شده: ${DayUtils.fa(readPages)} از ${DayUtils.fa(50)} صفحه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DialoguesTab(
    dialogues: List<DialogueEntity>,
    onOpen: (String) -> Unit
) {
    if (dialogues.isEmpty()) { EmptyView("💬", "دیالوگی یافت نشد."); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dialogues, key = { it.id }) { d ->
            Card(onClick = { onOpen(d.id) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(d.emoji, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        EnText(d.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LevelBadge(d.level)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun GrammarTab(
    lessons: List<GrammarEntity>,
    progress: Map<String, com.sahandsoft.englishlearner.data.db.GrammarProgressEntity>,
    onOpen: (String) -> Unit
) {
    if (lessons.isEmpty()) { EmptyView("📘", "درسی یافت نشد."); return }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lessons, key = { it.id }) { lesson ->
            val p = progress[lesson.id]
            Card(onClick = { onOpen(lesson.id) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(DayUtils.fa(lesson.lessonOrder), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lesson.titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        EnText(lesson.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (p?.done == true) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "انجام شد", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
