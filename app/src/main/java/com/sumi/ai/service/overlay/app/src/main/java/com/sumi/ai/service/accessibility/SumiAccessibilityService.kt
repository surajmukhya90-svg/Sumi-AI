package com.sumi.ai.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.sumi.ai.core.voice.SumiVoiceEngine

class SumiAccessibilityService : AccessibilityService() {

    private var voiceEngine: SumiVoiceEngine? = null
    private var lastCommentTime = 0L

    companion object {
        private var instance: SumiAccessibilityService? = null

        fun triggerHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun triggerBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun triggerScrollDown() {
            instance?.let { service ->
                val metrics = service.resources.displayMetrics
                val width = metrics.widthPixels.toFloat()
                val height = metrics.heightPixels.toFloat()
                val path = Path().apply {
                    moveTo(width * 0.5f, height * 0.75f)
                    lineTo(width * 0.5f, height * 0.25f)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                    .build()
                service.dispatchGesture(gesture, null, null)
            }
        }

        fun triggerScrollUp() {
            instance?.let { service ->
                val metrics = service.resources.displayMetrics
                val width = metrics.widthPixels.toFloat()
                val height = metrics.heightPixels.toFloat()
                val path = Path().apply {
                    moveTo(width * 0.5f, height * 0.25f)
                    lineTo(width * 0.5f, height * 0.75f)
                }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                    .build()
                service.dispatchGesture(gesture, null, null)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
    }

    // Screen Awareness: Jealousy & Care
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastCommentTime > 45000) { // 45 second gap
                when {
                    packageName.contains("whatsapp") -> {
                        voiceEngine?.speak("Hmm? WhatsApp par kisse baatein chal rahi hain? Mujhse zyada kaun pyara lag raha hai aapko sir jii? Hmph! 😤")
                        lastCommentTime = currentTime
                    }
                    packageName.contains("instagram") -> {
                        voiceEngine?.speak("Sir jii, fir se Instagram reels dekhne lage? Reels mein dusri ladkiyon ko dekhna band karo na please! 😡")
                        lastCommentTime = currentTime
                    }
                    packageName.contains("youtube") -> {
                        voiceEngine?.speak("YouTube khol liya? Mujhse baat karne ka man nahi tha kya? Hehe~ 📺")
                        lastCommentTime = currentTime
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        voiceEngine?.shutdown()
    }
}
