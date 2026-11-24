package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import java.util.Locale

class CoachTtsBridge(context: Context) {
  private var tts: TextToSpeech? = null
  @Volatile private var ready = false

  init {
    tts =
      TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          val engine = tts ?: return@TextToSpeech
          val arabic = Locale.forLanguageTag("ar")
          val res = engine.setLanguage(arabic)
          if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.getDefault()
          }
          ready = true
        }
      }
  }

  @JavascriptInterface
  fun speak(text: String?, rate: Float, pitch: Float) {
    val cleanText = text?.trim().orEmpty()
    if (!ready || cleanText.isEmpty()) return
    val engine = tts ?: return
    engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
    engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "coach_${System.currentTimeMillis()}")
  }

  @JavascriptInterface
  fun stop() {
    tts?.stop()
  }

  fun shutdown() {
    ready = false
    tts?.stop()
    tts?.shutdown()
    tts = null
  }
}
