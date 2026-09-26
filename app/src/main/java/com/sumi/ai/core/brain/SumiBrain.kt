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
            // Wake words (Suno Sumi / Sumi)
            q == "sumi" || q == "suno sumi" || q == "सुमी" || q == "सुनो सुमी" -> {
                SumiResponse("Haan ji babu ji, boliye! Main sun rahi hoon 😄")
            }

            // Halchal / Identity
            q.contains("kaise ho") || q.contains("kaisi ho") || q.contains("कैसे हो") || q.contains("कैसी हो") -> {
                SumiResponse("Main ekdam badhiya hoon ji! Aap batao kaisa chal raha hai? 🌸")
            }
            q.contains("naam") || q.contains("naam kya hai") || q.contains("नाम") -> {
                SumiResponse("Mera naam Sumi hai, aapki Hindi AI companion aur phone controller!")
            }

            // Instagram (Devanagari Hindi + English)
            q.contains("instagram") || q.contains("insta") || q.contains("ig") ||
            q.contains("इंस्टाग्राम") || q.contains("इन्स्टा") -> {
                SumiResponse("Achha ji, abhi Instagram kholti hoon! Reels dekhiye aaram se 😄", ActionType.OPEN_APP, "com.instagram.android")
            }

            // YouTube (Hindi + English)
            q.contains("youtube") || q.contains("यूट्यूब") || q.contains("yt") -> {
                SumiResponse("Ji babu ji, abhi YouTube open kar rahi hoon! 📺", ActionType.OPEN_APP, "com.google.android.youtube")
            }

            // WhatsApp
            q.contains("whatsapp") || q.contains("व्हाट्सएप") -> {
                SumiResponse("WhatsApp khol diya hai ji! 💬", ActionType.OPEN_APP, "com.whatsapp")
            }

            // Camera
            q.contains("camera") || q.contains("photo") || q.contains("कैमरा") || q.contains("फोटो") -> {
                SumiResponse("Smile kijiye ji, camera open ho raha hai! 📸", ActionType.OPEN_CAMERA)
            }

            // Settings
            q.contains("setting") || q.contains("सेटिंग") -> {
                SumiResponse("Theek hai ji, phone ki settings khol di maine! ⚙️", ActionType.OPEN_SETTINGS)
            }

            // Teasing
            q.contains("baar baar") || q.contains("बार बार") -> {
                SumiResponse("Kyunki aap bhi toh baar baar wahi pooch rahe ho 😑😂")
            }

            else -> {
                SumiResponse("Theek hai babu ji! Maine suna: \"$query\". Abhi main ise samajhne ki koshish kar rahi hoon ✨")
            }
        }
    }

    fun executeAction(context: Context, response: SumiResponse) {
        try {
            when (response.actionType) {
                ActionType.OPEN_APP -> {
                    response.targetPackage?.let { pkg ->
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            val webIntent = when (pkg) {
                                "com.instagram.android" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com"))
                                "com.google.android.youtube" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
                                else -> Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                            }
                            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(webIntent)
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
