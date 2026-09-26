package com.sumi.ai.service.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp
import com.sumi.ai.core.state.GirlfriendMood
import com.sumi.ai.core.state.SumiMoodState
import com.sumi.ai.core.voice.SumiVoiceEngine
import kotlinx.coroutines.*
import java.io.File

class SumiOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var mascotCard: FrameLayout? = null
    private var mascotImageView: ImageView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var voiceEngine: SumiVoiceEngine? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var breathingAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
        createSafeFloatingMascot()
        listenToMoodChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("🌸 Sumi Screen Par Ghoom Rahi Hai")
            .setContentText("Aapki anime companion active hai!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1002, notification)
        return START_NOT_STICKY
    }

    private fun createSafeFloatingMascot() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // CRITICAL FIX: Only 170x170 pixels size so full screen touches pass through to phone apps!
        val mascotSize = 175
        windowParams = WindowManager.LayoutParams(
            mascotSize,
            mascotSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 350
        }

        mascotCard = FrameLayout(this).apply {
            background = getMascotBorder(Color.parseColor("#FF4081")) // Pink border
        }

        mascotImageView = ImageView(this).apply {
            val file = File(filesDir, "custom_avatar.png")
            if (file.exists()) {
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                setImageResource(android.R.drawable.btn_star_big_on)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        mascotCard?.addView(mascotImageView)

        setupDragAndTouch()
        startLivingBreathingAnimation()

        try {
            windowManager?.addView(mascotCard, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMascotBorder(colorInt: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke(8, colorInt)
        }
    }

    // Live Breathing & Floating Motion
    private fun startLivingBreathingAnimation() {
        breathingAnimator = ValueAnimator.ofFloat(0.95f, 1.05f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                mascotCard?.scaleX = scale
                mascotCard?.scaleY = scale
            }
        }
        breathingAnimator?.start()
    }

    // Drag Anywhere on Screen + Tap to Talk
    private fun setupDragAndTouch() {
        mascotCard?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val params = windowParams ?: return false
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        try {
                            windowManager?.updateViewLayout(mascotCard, params)
                        } catch (e: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - touchX)
                        val diffY = Math.abs(event.rawY - touchY)
                        if (diffX < 15 && diffY < 15) {
                            handleMascotTap()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    // Tap karne par Alya ke expressions aur baat karna
    private fun handleMascotTap() {
        if (SumiMoodState.isIgnoring()) {
            voiceEngine?.speak("Hmph! Mujhe mat chhoo sir jii, main abhi aapse naraz hoon! 😤")
            return
        }

        when (SumiMoodState.currentMood.value) {
            GirlfriendMood.ROMANTIC -> {
                voiceEngine?.speak("Milashka~ Kahan dhyan hai aapka? Dil kar raha hai bas aapse baatein karti rahoon! 🌸💗")
            }
            GirlfriendMood.ANGRY_JEALOUS -> {
                voiceEngine?.speak("Nani yo?! Chhu kyu rahe ho mujhe? Pata hai na kitna gussa aa raha hai mujhe? 😡")
            }
            else -> {
                val quotes = listOf(
                    "Hehe~ Boliye sir jii, kya hua? Kahan kho gaye the? 👀",
                    "Milashka~ Aise achanak se kyu chhua mujhe? Baka~ 🌸",
                    "Main hamesha aapki screen par aapke sath hoon sir jii! 💗"
                )
                voiceEngine?.speak(quotes.random())
            }
        }
    }

    // Gussa hone par Border RED ho jana
    private fun listenToMoodChanges() {
        serviceScope.launch {
            SumiMoodState.currentMood.collect { mood ->
                withContext(Dispatchers.Main) {
                    when (mood) {
                        GirlfriendMood.ANGRY_JEALOUS, GirlfriendMood.UPSET_IGNORE -> {
                            mascotCard?.background = getMascotBorder(Color.parseColor("#FF1744")) // RED
                        }
                        GirlfriendMood.ROMANTIC -> {
                            mascotCard?.background = getMascotBorder(Color.parseColor("#FF4081")) // Pink
                        }
                        GirlfriendMood.NORMAL -> {
                            mascotCard?.background = getMascotBorder(Color.parseColor("#FF4081"))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        breathingAnimator?.cancel()
        mascotCard?.let { windowManager?.removeView(it) }
        voiceEngine?.shutdown()
        stopForeground(true)
    }
}
