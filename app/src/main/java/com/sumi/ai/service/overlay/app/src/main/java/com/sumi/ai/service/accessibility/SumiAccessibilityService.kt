package com.sumi.ai.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class SumiAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: SumiAccessibilityService? = null

        fun triggerHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun triggerBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun triggerNotifications() {
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
