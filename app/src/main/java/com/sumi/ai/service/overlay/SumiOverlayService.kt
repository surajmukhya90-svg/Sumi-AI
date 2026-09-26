package com.sumi.ai.service.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp
import com.sumi.ai.core.voice.SumiVoiceEngine
import java.io.File

class SumiOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var mascotView: ImageView? = null
    private var voiceEngine: SumiVoiceEngine? = null
    private var floatAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
        showAnimatedMascot()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("🌸 Sumi Screen Par Active Hai")
            .setContentText("Sumi mascot ko touch karke baat karein!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1002, notification)
        return START_NOT_STICKY
    }

    private fun showAnimatedMascot() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        mascotView = ImageView(this).apply {
            val file = File(filesDir, "custom_avatar.png")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                setImageBitmap(bitmap)
            } else {
                setImageResource(android.R.drawable.btn_star_big_on)
            }
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            170, 170,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 300
        }

        // Floating idle bounce animation (Chalta-firta mascot)
        floatAnimator = ValueAnimator.ofFloat(0f, 15f).apply {
            duration = 1200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val offset = anim.animatedValue as Float
                params.y = (300 + offset).toInt()
                try {
                    windowManager?.updateViewLayout(mascotView, params)
                } catch (e: Exception) {}
            }
        }
        floatAnimator?.start()

        // Drag & Touch interaction
        mascotView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(mascotView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 12 && diffY < 12) {
                            // Touch karne par Alya teasing / jealous dialogues
                            val teasingQuotes = listOf(
                                "Milashka~ Kahan dhyan hai aapka? Mujhe dekho na! 🌸",
                                "Kya hua sir jii? Baar-baar mujhe chhu kar tang kar rahe ho! Hmph! 😤",
                                "Betsu ni... Main toh bas dekh rahi thi aap kya kar rahe ho! Hehe~ 👀"
                            )
                            voiceEngine?.speak(teasingQuotes.random())
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(mascotView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatAnimator?.cancel()
        mascotView?.let { windowManager?.removeView(it) }
        voiceEngine?.shutdown()
        stopForeground(true)
    }
}
