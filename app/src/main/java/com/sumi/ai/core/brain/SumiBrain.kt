package com.sumi.ai.core.brain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

data class SumiResponse(
    val replyText: String,
    val actionType: ActionType = ActionType.NONE,
    val targetPackage: String? = null
)

enum class ActionType {
    NONE,
    OPEN_APP,
    OPEN_SETTINGS,
    OPEN_CAMERA
}

object SumiBrain {

    fun processQuery(context: Context, query: String): SumiResponse {
        val q = query.lowercase().trim()

        return when {
            // Greetings / Halchal
            q.contains("kaise ho") || q.contains("kaisi ho") -> {
                SumiResponse("Main bilkul theek aur fresh hoon ji! Aap batao, aaj main aapki kya madad karu? 😄")
            }
            q.contains("naam kya hai") || q.contains("tum kaun ho") || q.contains("who are you") -> {
                SumiResponse("Main Sumi hoon! Aapki personal Hindi AI dost aur phone controller 🌸")
            }
            q.contains("kya kar sakti ho") -> {
                SumiResponse("Main aapse Hindi mein baatein kar sakti hoon, YouTube, Instagram, Camera aur phone ki settings khol sakti hoon!")
            }

            // Teasing / Playful responses
            q.contains("pagal") || q.contains("gadhi") -> {
                SumiResponse("Aise bologe toh main baat nahi karungi aapse... naraz ho jaungi 😤")
            }

            // Open YouTube
            q.contains("youtube") -> {
                SumiResponse("Achha ji, abhi YouTube kholti hoon! 📺", ActionType.OPEN_APP, "com.google.android.youtube")
            }

            // Open Instagram
            q.contains("instagram") || q.contains("insta") || q.contains("ig") -> {
                SumiResponse("Haan ji babu ji, Instagram khol rahi hoon, reels dekho aaram se 😄", ActionType.OPEN_APP, "com.instagram.android")
            }

            // Open WhatsApp
            q.contains("whatsapp") -> {
                SumiResponse("Ji, WhatsApp open kar rahi hoon! 💬", ActionType.OPEN_APP, "com.whatsapp")
            }

            // Open Camera
            q.contains("camera") || q.contains("photo") -> {
                SumiResponse("Camera khol rahi hoon ji, smile kijiye! 📸", ActionType.OPEN_CAMERA)
            }

            // Open Settings
            q.contains("setting") -> {
                SumiResponse("Theek hai, phone ki settings khol di maine! ⚙️", ActionType.OPEN_SETTINGS)
            }

            // Default friendly reply
            else -> {
                SumiResponse("Achhaaa ji! Aapne kaha: \"$query\". Main abhi seekh rahi hoon, par jaldi hi isme bhi expert ho jaungi! ✨")
            }
        }
    }

    fun executeAction(context: Context, response: SumiResponse) {
        try {
            when (response.actionType) {
                ActionType.OPEN_APP -> {
                    response.targetPackage?.let { pkg ->
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            // Agar direct app nahi hai toh Play Store ya browser intent
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(marketIntent)
                        }
                    }
                }
                ActionType.OPEN_SETTINGS -> {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                ActionType.OPEN_CAMERA -> {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                ActionType.NONE -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
