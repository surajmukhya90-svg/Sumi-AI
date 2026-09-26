package com.sumi.ai.core.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ElevenLabsVoiceEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null

    // Default cute female anime voice ID (e.g. Freya / Rachel / Custom Anime Model)
    companion object {
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Warm, expressive female voice
    }

    suspend fun speakAnimeVoice(apiKey: String, voiceId: String, text: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false

        try {
            val vId = if (voiceId.isNotBlank()) voiceId else DEFAULT_VOICE_ID
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$vId"

            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2") // Supports natural Hindi & Japanese cadence
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.45)           // Expressive & emotional
                    put("similarity_boost", 0.85)
                    put("style", 0.40)
                    put("use_speaker_boost", true)
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val audioBytes = response.body?.bytes()
                if (audioBytes != null) {
                    val tempFile = File(context.cacheDir, "sumi_voice_temp.mp3")
                    FileOutputStream(tempFile).use { it.write(audioBytes) }

                    withContext(Dispatchers.Main) {
                        playAudioFile(tempFile)
                    }
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private fun playAudioFile(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}
    }
}
