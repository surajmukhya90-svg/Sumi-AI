package com.sumi.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class SumiVoiceEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isListening = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Alya Anime Character Pitch & Rate (High pitched, super cute & energetic)
            tts?.setPitch(1.68f)      // High pitch like cute anime girl
            tts?.setSpeechRate(1.08f)  // Slightly playful fast anime cadence

            // Hindi Female voice select karne ki koshish
            val hindiLocale = Locale("hi", "IN")
            val result = tts?.setLanguage(hindiLocale)

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                // Best female voice filter
                val voices = tts?.voices
                val femaleHindiVoice = voices?.firstOrNull { voice ->
                    voice.locale == hindiLocale && (voice.name.contains("female") || voice.name.contains("f0") || !voice.isNetworkConnectionRequired)
                }
                femaleHindiVoice?.let { tts?.voice = it }
            } else {
                tts?.setLanguage(Locale.getDefault())
            }
            isTtsReady = true
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady) return
        onStatusChanged("Speaking...")
        
        // Anime expression injection
        val animeStyledText = text
            .replace("Haan ji", "Hehe~ Haan ji!")
            .replace("Achha ji", "Achhaa ji~")
            .replace("Theek hai", "Hai! Theek hai babu ji~")

        tts?.speak(animeStyledText, TextToSpeech.QUEUE_FLUSH, null, "sumi_voice_id")
    }

    fun startListening() {
        if (isListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatusChanged("Speech mic unavailable")
            return
        }

        stopListening()
        isListening = true
        onStatusChanged("Listening...")

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStatusChanged("Listening...")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    onStatusChanged("Thinking...")
                    isListening = false
                }
                override fun onError(error: Int) {
                    isListening = false
                    onStatusChanged("Ready")
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        onSpeechRecognized(spokenText)
                    } else {
                        onStatusChanged("Ready")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onStatusChanged("Ready")
        }
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun shutdown() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
    }
}
