package com.sahandsoft.englishlearner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahandsoft.englishlearner.data.prefs.AppSettings
import com.sahandsoft.englishlearner.data.prefs.SettingsRepository
import com.sahandsoft.englishlearner.data.seed.SeedLoader
import com.sahandsoft.englishlearner.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SeedState { LOADING, READY, ERROR }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val seedLoader: SeedLoader,
    settingsRepository: SettingsRepository,
    private val tts: TtsManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _seedState = MutableStateFlow(SeedState.LOADING)
    val seedState: StateFlow<SeedState> = _seedState

    init {
        viewModelScope.launch {
            _seedState.value = if (seedLoader.ensureSeeded()) SeedState.READY else SeedState.ERROR
        }
        viewModelScope.launch {
            settings.collect { tts.setRate(it.ttsRate) }
        }
    }

    fun retrySeed() {
        _seedState.value = SeedState.LOADING
        viewModelScope.launch {
            _seedState.value = if (seedLoader.ensureSeeded()) SeedState.READY else SeedState.ERROR
        }
    }
}
