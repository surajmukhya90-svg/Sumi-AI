package com.sumi.ai.service.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp
import com.sumi.ai.core.brain.SumiBrain
import com.sumi.ai.core.voice.SumiVoiceEngine
import com.sumi.ai.ui.MainActivity
import java.util.Locale

class SumiForegroundService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceEngine: SumiVoiceEngine? = null
    private var isLoopRunning = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1001, notification)
        startSilentWakeListening()
        return START_STICKY
    }

    private fun startSilentWakeListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isLoopRunning) {
                        restartListening()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spoken = matches[0].lowercase(Locale.ROOT)
                        // "Sumi" ya "Suno Sumi" pakadne par uth jana
                        if (spoken.contains("sumi") || spoken.contains("सुमी") || spoken.contains("suno")) {
                            val response = SumiBrain.processQuery(applicationContext, spoken)
                            voiceEngine?.speak(response.replyText)
                            SumiBrain.executeAction(applicationContext, response)
                        }
                    }
                    if (isLoopRunning) {
                        restartListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        restartListening()
    }

    private fun restartListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, SumiApp.VOICE_CHANNEL_ID)
            .setContentTitle("🌸 Sumi hamesha sun rahi hai")
            .setContentText("Bolein: 'Sumi' ya 'Suno Sumi'...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isLoopRunning = false
        speechRecognizer?.destroy()
        voiceEngine?.shutdown()
        stopForeground(true)
    }
}
