package com.sumi.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sumi.ai.core.brain.SumiMood

@Composable
fun AnimeCharacterGraphic(mood: SumiMood, isSpeaking: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Pink Hair Base
        drawCircle(color = Color(0xFFFF8DA1), radius = w * 0.48f, center = Offset(w * 0.5f, h * 0.5f))

        // 2. Face Skin Tone
        drawCircle(color = Color(0xFFFFF0E8), radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.52f))

        // 3. Hair Bangs
        drawCircle(color = Color(0xFFFF6B8B), radius = w * 0.22f, center = Offset(w * 0.35f, h * 0.32f))
        drawCircle(color = Color(0xFFFF6B8B), radius = w * 0.22f, center = Offset(w * 0.65f, h * 0.32f))

        // 4. Large Anime Eyes (Deep Purple)
        val leftEye = Offset(w * 0.36f, h * 0.52f)
        val rightEye = Offset(w * 0.64f, h * 0.52f)
        val eyeR = w * 0.08f

        if (mood == SumiMood.TSUNDERE_ANNOYED) {
            // Pout / Gussa Eyes (> <)
            drawLine(Color(0xFF6A1B9A), Offset(leftEye.x - 12f, leftEye.y - 8f), Offset(leftEye.x + 12f, leftEye.y + 8f), strokeWidth = 8f)
            drawLine(Color(0xFF6A1B9A), Offset(leftEye.x - 12f, leftEye.y + 8f), Offset(leftEye.x + 12f, leftEye.y - 8f), strokeWidth = 8f)
            drawLine(Color(0xFF6A1B9A), Offset(rightEye.x - 12f, rightEye.y - 8f), Offset(rightEye.x + 12f, rightEye.y + 8f), strokeWidth = 8f)
            drawLine(Color(0xFF6A1B9A), Offset(rightEye.x - 12f, rightEye.y + 8f), Offset(rightEye.x + 12f, rightEye.y - 8f), strokeWidth = 8f)
        } else {
            // Sparkly Anime Eyes
            drawCircle(color = Color(0xFF6A1B9A), radius = eyeR, center = leftEye)
            drawCircle(color = Color(0xFF6A1B9A), radius = eyeR, center = rightEye)
            drawCircle(color = Color.White, radius = eyeR * 0.45f, center = Offset(leftEye.x - 4f, leftEye.y - 4f))
            drawCircle(color = Color.White, radius = eyeR * 0.45f, center = Offset(rightEye.x - 4f, rightEye.y - 4f))
        }

        // 5. Blush Cheeks
        val blushColor = if (mood == SumiMood.BLUSHING || mood == SumiMood.TSUNDERE_ANNOYED) Color(0xFFFF5252).copy(alpha = 0.65f) else Color(0xFFFF8DA1).copy(alpha = 0.4f)
        drawCircle(color = blushColor, radius = w * 0.07f, center = Offset(w * 0.26f, h * 0.62f))
        drawCircle(color = blushColor, radius = w * 0.07f, center = Offset(w * 0.74f, h * 0.62f))

        // 6. Mouth (Talking / Pout / Smile)
        if (isSpeaking) {
            drawCircle(color = Color(0xFFE91E63), radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.68f))
        } else if (mood == SumiMood.TSUNDERE_ANNOYED) {
            drawLine(Color(0xFFE91E63), Offset(w * 0.45f, h * 0.68f), Offset(w * 0.55f, h * 0.68f), strokeWidth = 6f)
        } else {
            drawCircle(color = Color(0xFFE91E63), radius = w * 0.025f, center = Offset(w * 0.5f, h * 0.67f))
        }

        // 7. Hair Flower Accessories
        drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.22f, h * 0.28f))
        drawCircle(color = Color(0xFFFF4081), radius = w * 0.02f, center = Offset(w * 0.22f, h * 0.28f))
        drawCircle(color = Color.White, radius = w * 0.05f, center = Offset(w * 0.78f, h * 0.28f))
        drawCircle(color = Color(0xFFFF4081), radius = w * 0.02f, center = Offset(w * 0.78f, h * 0.28f))
    }
}
