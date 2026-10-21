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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.prefs.SettingsRepository
import com.sahandsoft.englishlearner.data.repo.HomeStats
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.MiniBar
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.components.StatCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileExtra(
    val storiesDone: Int = 0,
    val storiesTotal: Int = 30,
    val lessonsDone: Int = 0,
    val lessonsTotal: Int = 16,
    val pagesRead: Int = 0,
    val quizScore: Int = 0,
    val quizTotal: Int = 0
)

data class Achievement(val emoji: String, val title: String, val desc: String, val earned: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val db: com.sahandsoft.englishlearner.data.db.SahandDatabase,
    private val progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val stats: StateFlow<HomeStats> = settingsRepository.settings
        .flatMapLatest { progressRepository.observeHomeStats(it.dailyGoalMinutes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())

    private val _extra = MutableStateFlow(ProfileExtra())
    val extra: StateFlow<ProfileExtra> = _extra.asStateFlow()

    init {
        viewModelScope.launch {
            val storiesTotal = db.storyDao().count()
            val lessonsTotal = db.grammarDao().observeAll().first().size
            val (qs, qt) = progressRepository.quizAgg()
            _extra.value = ProfileExtra(
                storiesDone = progressRepository.storiesDone(),
                storiesTotal = storiesTotal,
                lessonsDone = progressRepository.lessonsDone(),
                lessonsTotal = lessonsTotal,
                pagesRead = progressRepository.booksPagesRead(),
                quizScore = qs,
                quizTotal = qt
            )
        }
    }

    fun achievements(stats: HomeStats, extra: ProfileExtra): List<Achievement> = listOf(
        Achievement("🌱", "قدم اول", "اولین فعالیت امروزت را انجام بده", stats.streak >= 1),
        Achievement("🔥", "یک هفتهٔ پیوسته", "۷ روز پشت سر هم فعال باش", stats.streak >= 7),
        Achievement("🗂️", "ده واژه", "۱۰ واژه را یاد بگیر", stats.learned >= 10),
        Achievement("💯", "صدها واژه", "۱۰۰ واژه را یاد بگیر", stats.learned >= 100),
        Achievement("📖", "قصه‌خوان", "اولین داستان را کامل کن", extra.storiesDone >= 1),
        Achievement("📚", "کتاب‌خوان", "۵۰ صفحه کتاب بخوان", extra.pagesRead >= 50),
        Achievement("📝", "آزمون‌باز", "اولین آزمونت را بده", extra.quizTotal > 0),
        Achievement("🎓", "استاد واژگان", "۲۵ واژه را کامل مسلط شو", stats.mastered >= 25)
    )
}

@Composable
fun ProfileScreen(nav: NavHostController, vm: ProfileViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()
    val extra by vm.extra.collectAsState()
    val achievements = vm.achievements(stats, extra)

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "🏆 پروفایل و آمار")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🔥", DayUtils.fa(stats.streak), "روز زنجیره", Modifier.weight(1f))
                StatCard("🗂️", DayUtils.fa(stats.learned), "واژه یادگرفته", Modifier.weight(1f))
                StatCard("🎓", DayUtils.fa(stats.mastered), "مسلط", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("📖", "${DayUtils.fa(extra.storiesDone)}/${DayUtils.fa(extra.storiesTotal)}", "داستان", Modifier.weight(1f))
                StatCard("📘", "${DayUtils.fa(extra.lessonsDone)}/${DayUtils.fa(extra.lessonsTotal)}", "درس گرامر", Modifier.weight(1f))
                StatCard("📚", DayUtils.fa(extra.pagesRead), "صفحه کتاب", Modifier.weight(1f))
            }
        }

        if (extra.quizTotal > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("دقت آزمون‌ها", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        MiniBar(extra.quizScore.toFloat() / extra.quizTotal.coerceAtLeast(1))
                        Text(
                            "${DayUtils.fa(extra.quizScore)} پاسخ درست از ${DayUtils.fa(extra.quizTotal)} پاسخ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("فعالیت هفت روز گذشته", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    stats.weekActivity.forEach { (day, sec) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                DayUtils.dateLabel(day),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(48.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MiniBar(
                                (sec.toFloat() / (stats.weekActivity.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1)),
                                Modifier.weight(1f).padding(vertical = 3.dp),
                                color = if (sec > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "${DayUtils.fa((sec / 60).toInt())}د",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("نشان‌ها", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    achievements.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { a ->
                                Row(
                                    Modifier.weight(1f).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        a.emoji,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (a.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            a.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (a.earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            a.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
