package com.sahandsoft.englishlearner.util

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * پوشش ساده روی موتور گفتار دستگاه (Android TTS).
 * توجه صادقانه: کیفیت تلفظ به موتور نصب‌شده روی دستگاه کاربر وابسته است
 * و ممکن است روی بعضی دستگاه‌ها در دسترس نباشد.
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private var tts: TextToSpeech? = null

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private var rate: Float = 1.0f

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                _ready.value = result != null && result >= TextToSpeech.LANG_AVAILABLE
            } else {
                _ready.value = false
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _speaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _speaking.value = false
            }
        })
    }

    fun setRate(value: Float) {
        rate = value
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String) {
        if (!_ready.value) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sahand_${System.currentTimeMillis()}")
    }

    fun speakQueued(text: String) {
        if (!_ready.value) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "sahand_${System.currentTimeMillis()}")
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _speaking.value = false
    }
}
