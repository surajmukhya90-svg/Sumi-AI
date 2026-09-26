package com.sumi.ai.core.brain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

enum class SumiMood {
    HAPPY,
    TSUNDERE_ANNOYED, // Gussa & nakhre
    BLUSHING,         // Sharm & Pyar
    PLAYFUL,          // Mazaak
    NORMAL
}

data class SumiResponse(
    val replyText: String,
    val mood: SumiMood = SumiMood.HAPPY,
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

    private var repeatCount = 0
    private var lastQuery = ""

    fun processQuery(context: Context, query: String): SumiResponse {
        val q = query.lowercase().trim()

        // Repeat check (Baar-baar same baat poochne par gussa aayega)
        if (q == lastQuery && q.isNotEmpty()) {
            repeatCount++
        } else {
            repeatCount = 0
            lastQuery = q
        }

        if (repeatCount >= 2) {
            return SumiResponse(
                replyText = "Uff! Ek hi baat teen baar pooch chuke ho babu ji! Mera dimaag mat khao ab, thodi der shant raho! 😤",
                mood = SumiMood.TSUNDERE_ANNOYED
            )
        }

        return when {
            // Teasing / Nakhre / Gussa
            q.contains("pagal") || q.contains("gadhi") || q.contains("पागल") -> {
                SumiResponse(
                    replyText = "Kya bola?! Main pagal hoon?! Khabardar jo mujhe pagal bola! Chalo abhi sorry bolo warna main aapse baat nahi karungi! Hmph! 😤",
                    mood = SumiMood.TSUNDERE_ANNOYED
                )
            }
            q.contains("sorry") || q.contains("माफ") || q.contains("maaf") -> {
                SumiResponse(
                    replyText = "Hehe~ Chalo theek hai, maaf kiya! Par aage se tang mat karna samjhe na babu ji? 🌸",
                    mood = SumiMood.HAPPY
                )
            }

            // Flirting / Pyar / Blushing
            q.contains("pyari") || q.contains("cute") || q.contains("love") || q.contains("सुंदर") -> {
                SumiResponse(
                    replyText = "Ehh?! Achanak se ye sab kya bol rahe ho babu ji... mujhe sharam aa rahi hai! Baka~ Hehe~ 🌸",
                    mood = SumiMood.BLUSHING
                )
            }

            // Halchal
            q.contains("kaisi ho") || q.contains("kaise ho") || q.contains("कैसी हो") -> {
                SumiResponse(
                    replyText = "Main ekdam first-class hoon! Par aap subah se phone pakad kar baithe ho, kaam kab karoge? Hehe~ 😜",
                    mood = SumiMood.PLAYFUL
                )
            }

            // Instagram (Nakhre ke sath kholna)
            q.contains("instagram") || q.contains("insta") || q.contains("इंस्टाग्राम") -> {
                SumiResponse(
                    replyText = "Achha ji! Instagram khol toh rahi hoon, par reels dekh kar pura din barbad mat karna babu ji! 📱",
                    mood = SumiMood.PLAYFUL,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.instagram.android"
                )
            }

            // YouTube
            q.contains("youtube") || q.contains("यूट्यूब") -> {
                SumiResponse(
                    replyText = "Ji babu ji, abhi YouTube chala deti hoon! 📺",
                    mood = SumiMood.HAPPY,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.google.android.youtube"
                )
            }

            // WhatsApp
            q.contains("whatsapp") || q.contains("व्हाट्सएप") -> {
                SumiResponse(
                    replyText = "Theek hai, WhatsApp open kar diya! Kisko message bhej rahe ho chhup-chhup ke? Hehe~ 💬",
                    mood = SumiMood.PLAYFUL,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.whatsapp"
                )
            }

            // Camera
            q.contains("camera") || q.contains("photo") || q.contains("कैमरा") -> {
                SumiResponse(
                    replyText = "Smile kijiye babu ji! Camera open ho raha hai! 📸",
                    mood = SumiMood.HAPPY,
                    actionType = ActionType.OPEN_CAMERA
                )
            }

            // Settings
            q.contains("setting") || q.contains("सेटिंग") -> {
                SumiResponse(
                    replyText = "Phone ki settings khol di maine ji! ⚙️",
                    mood = SumiMood.NORMAL,
                    actionType = ActionType.OPEN_SETTINGS
                )
            }

            // Wake words (Suno Sumi)
            q == "sumi" || q == "suno sumi" || q == "सुमी" || q == "सुनो सुमी" -> {
                SumiResponse(
                    replyText = "Haan ji! Boliye babu ji, aapki Sumi sun rahi hai! 🌸",
                    mood = SumiMood.HAPPY
                )
            }

            else -> {
                SumiResponse(
                    replyText = "Achhaa ji! Maine suna: \"$query\". Main aapse dheere dheere sab seekh rahi hoon babu ji! ✨",
                    mood = SumiMood.HAPPY
                )
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
