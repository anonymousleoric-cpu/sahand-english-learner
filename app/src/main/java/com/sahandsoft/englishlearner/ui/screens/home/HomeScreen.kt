package com.sahandsoft.englishlearner.ui.screens.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.sahandsoft.englishlearner.ui.components.SectionHeader
import com.sahandsoft.englishlearner.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    val stats: StateFlow<HomeStats> = settingsRepository.settings
        .flatMapLatest { progressRepository.observeHomeStats(it.dailyGoalMinutes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())
}

@Composable
fun HomeScreen(nav: NavHostController, vm: HomeViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سلام! 👋", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "امروز چند دقیقه برای انگلیسی وقت داری؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            DayUtils.fa(stats.streak) + " روز",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item {
            val frac = if (stats.goalSeconds > 0) stats.todaySeconds.toFloat() / stats.goalSeconds else 0f
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "هدف امروز",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${DayUtils.fa((stats.todaySeconds / 60).toInt())} از ${DayUtils.fa((stats.goalSeconds / 60).toInt())} دقیقه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (stats.todaySeconds >= stats.goalSeconds) {
                            Text(
                                "انجام شد! ✅",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    MiniBar(frac, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    emoji = "🧠",
                    title = "مرور هوشمند",
                    subtitle = if (stats.dueCount > 0) "${DayUtils.fa(stats.dueCount)} واژه" else "آماده",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Routes.REVIEW) }
                QuickAction(
                    emoji = "⚡",
                    title = "آزمون سریع",
                    subtitle = "شروع کن",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Routes.QUIZ) }
                QuickAction(
                    emoji = "🎴",
                    title = "کارت واژه",
                    subtitle = "تمرین",
                    modifier = Modifier.weight(1f)
                ) { nav.navigate(Routes.flashcards("all")) }
            }
        }

        item { SectionHeader("بخش‌های آموزشی") }

        val features = listOf(
            Triple("📖", "داستان‌ها", Routes.LIBRARY),
            Triple("📚", "کتاب‌ها", Routes.LIBRARY),
            Triple("💬", "دیالوگ‌ها", Routes.LIBRARY),
            Triple("📘", "گرامر", Routes.LIBRARY),
            Triple("🗂️", "واژگان", Routes.WORDS),
            Triple("🔍", "فرهنگ لغت", Routes.DICTIONARY),
            Triple("🎮", "بازی‌ها", Routes.GAMES),
            Triple("🏆", "پروفایل", Routes.PROFILE)
        )
        items(features.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (emoji, label, route) ->
                    FeatureCard(emoji, label, Modifier.weight(1f)) { nav.navigate(route) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("فعالیت هفت روز گذشته", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxSec = (stats.weekActivity.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(60L)
                        stats.weekActivity.forEach { (day, sec) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((40f * (sec.toFloat() / maxSec)).dp.coerceAtLeast(3.dp))
                                        .background(
                                            if (sec > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    DayUtils.fa(DayUtils.dateLabel(day).substringAfter('/')),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
