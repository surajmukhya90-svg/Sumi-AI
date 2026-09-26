package com.sumi.ai.service.overlay

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
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp
import com.sumi.ai.core.voice.SumiVoiceEngine
import java.io.File

class SumiOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingAvatarView: ImageView? = null
    private var voiceEngine: SumiVoiceEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
        showFloatingAvatar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("🌸 Sumi Screen Par Tair Rahi Hai")
            .setContentText("Avatar tap karke baat karein!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1002, notification)
        return START_NOT_STICKY
    }

    private fun showFloatingAvatar() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingAvatarView = ImageView(this).apply {
            // Load saved photo if exists, else fallback
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
            160, 160,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Draggable Touch Listener
        floatingAvatarView?.setOnTouchListener(object : View.OnTouchListener {
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
                        windowManager?.updateViewLayout(floatingAvatarView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            // Tap karne par Alya bol padegi
                            voiceEngine?.speak("Hehe~ Kya hua sir jii? Mujhe kyu chhua? Boliye na! 🌸")
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(floatingAvatarView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingAvatarView?.let { windowManager?.removeView(it) }
        voiceEngine?.shutdown()
        stopForeground(true)
    }
}
