package com.sahandsoft.englishlearner.ui.screens.games

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.ui.components.SectionHeader
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.ui.nav.Routes

@Composable
fun GamesHubScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        SahandTopBar(title = "بازی‌ها")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader("با بازی واژه‌هایت را قوی‌تر کن!") }
            item {
                GameCard(
                    emoji = "🧩",
                    title = "جورچین واژه‌ها",
                    desc = "واژه‌های انگلیسی را با معنی فارسی آن‌ها جور کن.",
                    onClick = { nav.navigate(Routes.GAME_MATCH) }
                )
            }
            item {
                GameCard(
                    emoji = "🔤",
                    title = "حدس واژه",
                    desc = "حروف بهم‌ریخته را به ترتیب بگذار و واژه را بساز.",
                    onClick = { nav.navigate(Routes.GAME_GUESS) }
                )
            }
            item {
                GameCard(
                    emoji = "⚡",
                    title = "آزمون برقی",
                    desc = "۶۰ ثانیه وقت داری! هرچه می‌توانی سؤال درست جواب بده.",
                    onClick = { nav.navigate(Routes.GAME_SPEED) }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun GameCard(emoji: String, title: String, desc: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
