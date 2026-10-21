package com.sahandsoft.englishlearner.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
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
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
    progressRepository: ProgressRepository
) : ViewModel() {
    val dueCount: StateFlow<Int> = progressRepository.observeDueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun ToolsScreen(nav: NavHostController, vm: ToolsViewModel = hiltViewModel()) {
    val dueCount by vm.dueCount.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "ابزارها")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ToolCard(
                    emoji = "🧠",
                    title = "مرور هوشمند",
                    desc = "واژه‌هایی که زمان مرورشان رسیده است",
                    badge = if (dueCount > 0) DayUtils.fa(dueCount) else null,
                    onClick = { nav.navigate(Routes.REVIEW) }
                )
            }
            item {
                ToolCard(
                    emoji = "📝",
                    title = "آزمون",
                    desc = "آزمون چهارگزینه‌ای با سطوح مختلف",
                    badge = null,
                    onClick = { nav.navigate(Routes.QUIZ) }
                )
            }
            item {
                ToolCard(
                    emoji = "🔍",
                    title = "فرهنگ لغت",
                    desc = "جست‌وجوی دوطرفه در ۵۰۰ واژه",
                    badge = null,
                    onClick = { nav.navigate(Routes.DICTIONARY) }
                )
            }
            item {
                ToolCard(
                    emoji = "🏆",
                    title = "پروفایل و آمار",
                    desc = "پیشرفت، زنجیره روزها و نشان‌ها",
                    badge = null,
                    onClick = { nav.navigate(Routes.PROFILE) }
                )
            }
            item {
                ToolCard(
                    emoji = "⚙️",
                    title = "تنظیمات",
                    desc = "تم، حالت کودک، هدف روزانه و پشتیبان‌گیری",
                    badge = null,
                    onClick = { nav.navigate(Routes.SETTINGS) }
                )
            }
            item {
                ToolCard(
                    emoji = "ℹ️",
                    title = "دربارهٔ برنامه",
                    desc = "دربارهٔ سازنده و حریم خصوصی",
                    badge = null,
                    onClick = { nav.navigate(Routes.ABOUT) }
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    emoji: String,
    title: String,
    desc: String,
    badge: String?,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                Badge { Text(badge) }
            }
        }
    }
}
