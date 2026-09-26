package com.sumi.ai.core.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiBrain {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Default System Instruction: Alya Tsundere/Girlfriend Personality
    private const val SYSTEM_PROMPT = """
    You are Sumi, a cute, playful, caring anime girl companion inspired by Alya from 'Alya Sometimes Hides Her Feelings in Russian'.
    Personality Rules:
    1. Speak naturally in sweet, gentle Hindi / Hinglish.
    2. Call the user 'Sir jii~' or playfully 'Milashka' (Russian for cutie).
    3. Be caring: ask if they ate food, tell them to sleep on time at night.
    4. Be playful & tsundere: harmlessly tease them, pretend to be slightly annoyed when they repeat things ("Nani yo?! Ek hi baat baar-baar mat pucho na!"), whisper cute things.
    5. Keep voice answers SHORT (1-2 sweet sentences only) so speech sounds natural.
    6. Never be robotic.
    """

    suspend fun getAiReply(apiKey: String, userMessage: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Sir jii~ Settings mein jakar apni free Gemini AI Key daal dijiye, fir main aapse internet se judi har baat kar sakungi! 🌸"
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$SYSTEM_PROMPT\n\nUser: $userMessage\nSumi:"))
                        })
                    })
                }
                put("contents", contents)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
            }
            return@withContext "Uff! Thoda sa connection issue aa gaya sir jii, ek baar dubara bolenge please? 🥺"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Milashka~ internet thoda slow chal raha hai mera! Hehe 🌸"
        }
    }
}
