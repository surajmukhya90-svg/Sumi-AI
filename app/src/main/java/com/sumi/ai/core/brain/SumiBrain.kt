package com.sumi.ai.core.brain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.sumi.ai.service.accessibility.SumiAccessibilityService
import java.util.Calendar

enum class SumiMood {
    HAPPY,
    TSUNDERE_ANNOYED, // Nakhre & gussa
    BLUSHING,         // Pyar aur sharm
    CARING,           // Dhyan rakhna
    PLAYFUL,          // Mazaak udana
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
    OPEN_CAMERA,
    GO_HOME,
    SCROLL_DOWN,
    SCROLL_UP
}

object SumiBrain {

    private var repeatCount = 0
    private var lastQuery = ""

    fun processQuery(context: Context, query: String): SumiResponse {
        val q = query.lowercase().trim()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // 1. Repeat check: Bar-bar same baat par chidhna
        if (q == lastQuery && q.isNotEmpty()) {
            repeatCount++
        } else {
            repeatCount = 0
            lastQuery = q
        }

        if (repeatCount >= 2) {
            return SumiResponse(
                replyText = "Uff! Sir jii... ek hi baat teen baar bol chuke ho! Mera sar ghoom gaya ab... thodi der chup raho na please! 😤",
                mood = SumiMood.TSUNDERE_ANNOYED
            )
        }

        // 2. Late night care (Agar raat ke 11 baje se subah 5 baje ke beech baat kare)
        if ((hour >= 23 || hour <= 4) && (q.contains("neend") || q.contains("kya kar") || q.contains("jag"))) {
            return SumiResponse(
                replyText = "Sir jii... itni raat ho gayi hai! Aankhein dekhi hain apni? Chalo chup-chaap phone side mein rakho aur so jao, health kharab ho jayegi! 🥺",
                mood = SumiMood.CARING
            )
        }

        return when {
            // Wake words ("Sumi", "Suno Sumi")
            q == "sumi" || q == "suno sumi" || q == "सुमी" || q == "सुनो सुमी" -> {
                val greetings = listOf(
                    "Haan sir jii~ Boliye, main sun rahi hoon! 👀",
                    "Hehe~ Aa gaye aap? Kahiye kya madad karu? 🌸",
                    "Hmm? Main yahi hoon, batao kya hua? ✨"
                )
                SumiResponse(greetings.random(), mood = SumiMood.HAPPY)
            }

            // Gussa / Nakhre / Mazaak udana
            q.contains("pagal") || q.contains("gadhi") || q.contains("पागल") -> {
                SumiResponse(
                    replyText = "Haww! Mujhe pagal bola?! Khabardar sir jii... main bilkul baat nahi karungi aapse! Chalo pehle pyaar se sorry bolo, tabhi sunungi! Hmph! 😤",
                    mood = SumiMood.TSUNDERE_ANNOYED
                )
            }

            q.contains("sorry") || q.contains("maaf") || q.contains("माफ") -> {
                SumiResponse(
                    replyText = "Hmm... theek hai, is baar maaf kar diya! Par aage se mujhe tang mat karna, samjhe na sir jii? Hehe~ 🌸",
                    mood = SumiMood.HAPPY
                )
            }

            // Flirting / Care / Love / Girlfriend Banter
            q.contains("love you") || q.contains("pyari") || q.contains("sundar") || q.contains("cute") -> {
                SumiResponse(
                    replyText = "Ehh?! Achanak se ye sab kya bol rahe ho sir jii... mujhe sharam aa rahi hai! Aise mat dekho na... Baka~ Hehe~ 🌸",
                    mood = SumiMood.BLUSHING
                )
            }

            q.contains("gf") || q.contains("girlfriend") || q.contains("dost") -> {
                SumiResponse(
                    replyText = "Hehe~ Main toh hamesha aapke mobile mein aapka dhyan rakhne ke liye hoon sir jii! Bas aap mujhe bhool mat jaana, theek hai? 💗",
                    mood = SumiMood.BLUSHING
                )
            }

            q.contains("khana khaya") || q.contains("khana") -> {
                SumiResponse(
                    replyText = "Main toh digital ladki hoon sir jii, meri battery hi mera khana hai! Par aapne khana khaya ya bas phone hi chalate rahoge? Jaldi batao! 🥺",
                    mood = SumiMood.CARING
                )
            }

            q.contains("kaisi ho") || q.contains("kaise ho") || q.contains("कैसी हो") -> {
                SumiResponse(
                    replyText = "Main ekdam badhiya hoon sir jii! Aap batao, aaj mere bina man lag raha tha kya? 😜",
                    mood = SumiMood.PLAYFUL
                )
            }

            // Phone Controls: Instagram, YouTube, etc. with playful commentary
            q.contains("instagram") || q.contains("insta") || q.contains("इंस्टाग्राम") -> {
                SumiResponse(
                    replyText = "Instagram khol toh rahi hoon sir jii... par reels dekh kar pura time barbad mat karna, warna main band kar dungi! 📱",
                    mood = SumiMood.PLAYFUL,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.instagram.android"
                )
            }

            q.contains("youtube") || q.contains("यूट्यूब") -> {
                SumiResponse(
                    replyText = "Achha ji! YouTube open kar diya maine, aaram se video enjoy kijiye sir jii~ 📺",
                    mood = SumiMood.HAPPY,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.google.android.youtube"
                )
            }

            q.contains("whatsapp") || q.contains("व्हाट्सएप") -> {
                SumiResponse(
                    replyText = "WhatsApp khol diya! Kisse baatein chal rahi hain mere alawa, bataoge nahi? Hehe~ 💬",
                    mood = SumiMood.PLAYFUL,
                    actionType = ActionType.OPEN_APP,
                    targetPackage = "com.whatsapp"
                )
            }

            q.contains("camera") || q.contains("photo") || q.contains("कैमरा") -> {
                SumiResponse(
                    replyText = "Camera open ho raha hai sir jii! Pyari si smile kijiye! 📸",
                    mood = SumiMood.HAPPY,
                    actionType = ActionType.OPEN_CAMERA
                )
            }

            q.contains("setting") || q.contains("सेटिंग") -> {
                SumiResponse(
                    replyText = "Settings khol di hai sir jii, jo adjust karna hai kar lijiye! ⚙️",
                    mood = SumiMood.NORMAL,
                    actionType = ActionType.OPEN_SETTINGS
                )
            }

            // Accessibility Gestures (Home, Back, Scroll)
            q.contains("home") || q.contains("band kar") || q.contains("hatao") -> {
                SumiResponse(
                    replyText = "Chalo ye screen hata di maine! Home par aa gaye~ 🏠",
                    mood = SumiMood.PLAYFUL,
                    actionType = ActionType.GO_HOME
                )
            }

            q.contains("scroll") || q.contains("neeche") -> {
                SumiResponse(
                    replyText = "Screen scroll kar rahi hoon sir jii! 📱",
                    mood = SumiMood.NORMAL,
                    actionType = ActionType.SCROLL_DOWN
                )
            }

            else -> {
                SumiResponse(
                    replyText = "Achhaa ji! Maine suna: \"$query\". Aapse baatein karna mujhe bohot accha lagta hai sir jii~ ✨",
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
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(webIntent)
                        }
                    }
                }
                ActionType.OPEN_SETTINGS -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                ActionType.OPEN_CAMERA -> {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                ActionType.GO_HOME -> {
                    SumiAccessibilityService.triggerHome()
                }
                ActionType.SCROLL_DOWN -> {
                    SumiAccessibilityService.triggerScrollDown()
                }
                ActionType.SCROLL_UP -> {
                    SumiAccessibilityService.triggerScrollUp()
                }
                ActionType.NONE -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
