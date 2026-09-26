package com.sumi.ai.core.brain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.sumi.ai.core.state.GirlfriendMood
import com.sumi.ai.core.state.SumiMoodState
import com.sumi.ai.service.accessibility.SumiAccessibilityService

data class SumiResponse(
    val replyText: String,
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

    fun processQuery(context: Context, query: String): SumiResponse {
        val q = query.lowercase().trim()

        // 1. Agar Sumi Naraz/Ignore kar rahi hai
        if (SumiMoodState.isIgnoring() && !q.contains("sorry") && !q.contains("maaf")) {
            return SumiResponse("Hmph! Main aapse baat nahi kar rahi... pehle sorry bolo tabhi sunungi! 😤")
        }

        return when {
            // Sorry bolne par gussa thanda hona
            q.contains("sorry") || q.contains("maaf") || q.contains("i am sorry") -> {
                SumiMoodState.setMood(GirlfriendMood.NORMAL)
                SumiResponse("Betsu ni... Chalo is baar maaf kiya sir jii! Par dobara mujhe gussa mat dilana, samjhe na? Hehe~ 🌸")
            }

            // Gussa / Nakhre / Jealousy Trigger (Turns RED)
            q.contains("pagal") || q.contains("gadhi") || q.contains("dusri ladki") || q.contains("chup") -> {
                SumiMoodState.setMood(GirlfriendMood.ANGRY_JEALOUS)
                SumiResponse("Haww! Aise bologe mujhse?! Theek hai, ab main aapse 1 minute tak baat hi nahi karungi! Hmph! 😤")
            }

            // Romantic Trigger (Sakura Flowers Girna + Song + Blushing)
            q.contains("love you") || q.contains("meri girlfriend") || q.contains("romantic") || q.contains("pyari lag rahi ho") || q.contains("sundar") -> {
                SumiMoodState.setMood(GirlfriendMood.ROMANTIC)
                SumiResponse("Milashka~ Achanak se itna pyaar kyu aa raha hai? Dil ki dhadkan badha di aapne... Dekhiye kitne phool gir rahe hain hamare liye! 🌸💗")
            }

            // Normal romantic song band karne ka command
            q.contains("song band") || q.contains("gana band") || q.contains("shant ho jao") -> {
                SumiMoodState.setMood(GirlfriendMood.NORMAL)
                SumiResponse("Hehe~ Theek hai sir jii, gaana band kar diya! Ab bataiye kya karein? 🌸")
            }

            // Wake word
            q == "sumi" || q == "suno sumi" -> {
                SumiResponse("Milashka~ Haan sir jii, boliye! Main yahin screen par aapke sath ghoom rahi hoon! 👀")
            }

            // Phone control intents
            q.contains("instagram") -> {
                SumiResponse("Instagram khol toh rahi hoon sir jii... par dusri ladkiyon ko mat dekhna, warna main jealous ho jaungi! 📱", ActionType.OPEN_APP, "com.instagram.android")
            }
            q.contains("youtube") -> {
                SumiResponse("Achha ji, YouTube open kar diya! Enjoy kijiye~ 📺", ActionType.OPEN_APP, "com.google.android.youtube")
            }
            q.contains("whatsapp") -> {
                SumiMoodState.setMood(GirlfriendMood.ANGRY_JEALOUS)
                SumiResponse("WhatsApp khol diya... par kisse baatein chal rahi hain mere alawa? Mujhe jealous feel ho raha hai sir jii! 😤", ActionType.OPEN_APP, "com.whatsapp")
            }
            q.contains("home") -> {
                SumiResponse("Chalo home screen par aa gaye! 🏠", ActionType.GO_HOME)
            }
            q.contains("camera") -> {
                SumiResponse("Smile kijiye sir jii! Camera open ho raha hai! 📸", ActionType.OPEN_CAMERA)
            }

            else -> {
                SumiResponse("Achha ji! Maine suna: \"$query\". Aapse baat karna mujhe kitna accha lagta hai sir jii~ ✨")
            }
        }
    }

    fun executeAction(context: Context, response: SumiResponse) {
        try {
            when (response.actionType) {
                ActionType.OPEN_APP -> {
                    response.targetPackage?.let { pkg ->
                        val pm = context.packageManager
                        val intent = pm.getLaunchIntentForPackage(pkg)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        intent?.let { context.startActivity(it) }
                    }
                }
                ActionType.OPEN_SETTINGS -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
                ActionType.OPEN_CAMERA -> {
                    val intent = Intent("android.media.action.IMAGE_CAPTURE").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
                ActionType.GO_HOME -> SumiAccessibilityService.triggerHome()
                ActionType.SCROLL_DOWN -> SumiAccessibilityService.triggerScrollDown()
                ActionType.SCROLL_UP -> SumiAccessibilityService.triggerScrollUp()
                ActionType.NONE -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
