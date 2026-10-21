package com.sahandsoft.englishlearner.ui.screens.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.sahandsoft.englishlearner.data.prefs.AppSettings
import com.sahandsoft.englishlearner.data.prefs.SettingsRepository
import com.sahandsoft.englishlearner.data.repo.ProgressRepository
import com.sahandsoft.englishlearner.domain.DayUtils
import com.sahandsoft.englishlearner.ui.components.PillChip
import com.sahandsoft.englishlearner.ui.components.SahandTopBar
import com.sahandsoft.englishlearner.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setTheme(mode: String) = viewModelScope.launch { settingsRepository.setTheme(mode) }
    fun setChildMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setChildMode(enabled) }
    fun setDailyGoal(minutes: Int) = viewModelScope.launch { settingsRepository.setDailyGoal(minutes) }
    fun setTtsRate(rate: Float) = viewModelScope.launch { settingsRepository.setTtsRate(rate) }

    fun export(uri: android.net.Uri) = viewModelScope.launch {
        _message.value = if (backupManager.exportTo(uri)) "پشتیبان با موفقیت ذخیره شد ✅" else "ذخیرهٔ پشتیبان ناموفق بود ❌"
    }

    fun import(uri: android.net.Uri) = viewModelScope.launch {
        _message.value = if (backupManager.importFrom(uri)) "پشتیبان بازیابی شد ✅" else "بازیابی ناموفق بود — فایل معتبر نیست ❌"
    }

    fun resetProgress() = viewModelScope.launch {
        progressRepository.resetAllProgress()
        _message.value = "همهٔ پیشرفت‌ها بازنشانی شد"
    }

    fun clearMessage() {
        _message.value = null
    }
}

@Composable
fun SettingsScreen(nav: NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::import) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SahandTopBar(title = "⚙️ تنظیمات", onBack = { nav.popBackStack() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("ظاهر برنامه", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillChip("سیستم", "🌓", settings.themeMode == "system") { vm.setTheme("system") }
                            PillChip("روشن", "☀️", settings.themeMode == "light") { vm.setTheme("light") }
                            PillChip("تاریک", "🌙", settings.themeMode == "dark") { vm.setTheme("dark") }
                        }
                    }
                }

                Card {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("حالت کودک", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "فونت درشت‌تر و رنگ‌های شاداب‌تر برای بچه‌ها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.childMode, onCheckedChange = vm::setChildMode)
                    }
                }

                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("هدف روزانه", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillChip("۵ دقیقه", "🚀", settings.dailyGoalMinutes == 5) { vm.setDailyGoal(5) }
                            PillChip("۱۰ دقیقه", "⏱️", settings.dailyGoalMinutes == 10) { vm.setDailyGoal(10) }
                            PillChip("۱۵ دقیقه", "🏅", settings.dailyGoalMinutes == 15) { vm.setDailyGoal(15) }
                        }
                    }
                }

                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("سرعت گفتار", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "برای بچه‌ها سرعت آهسته‌تر بهتر است",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.ttsRate,
                                onValueChange = vm::setTtsRate,
                                valueRange = 0.5f..1.5f,
                                steps = 4,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("×${settings.ttsRate}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "پیشرفتت را در یک فایل ذخیره کن و روی دستگاه دیگر بازیابی کن. همه‌چیز محلی می‌ماند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { exportLauncher.launch("sahand-backup.json") },
                                modifier = Modifier.weight(1f)
                            ) { Text("خروجی پشتیبان 💾") }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f)
                            ) { Text("بازیابی 📥") }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("بازنشانی پیشرفت", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "همهٔ واژه‌های یادگرفته، آزمون‌ها و زنجیرهٔ روزها پاک می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { showResetDialog = true }) { Text("بازنشانی کن") }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "🔒 این برنامه کاملاً آفلاین است؛ بدون تبلیغ، بدون خرید درون‌برنامه‌ای، بدون حساب کاربری و بدون ارسال هیچ داده‌ای.",
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("بازنشانی پیشرفت") },
            text = { Text("مطمئنی؟ این کار برگشت ندارد.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    vm.resetProgress()
                }) { Text("بله، پاک کن") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("انصراف") }
            }
        )
    }
}
