package com.sumi.ai.service.overlay

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp
import com.sumi.ai.core.state.GirlfriendMood
import com.sumi.ai.core.state.SumiMoodState
import com.sumi.ai.core.voice.SumiVoiceEngine
import kotlinx.coroutines.*
import java.io.File
import java.util.Random

class SumiOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootLayout: FrameLayout? = null
    private var mascotCard: FrameLayout? = null
    private var mascotImageView: ImageView? = null
    private var petalsContainer: FrameLayout? = null

    private var voiceEngine: SumiVoiceEngine? = null
    private var romanticPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var wanderAnimatorX: ValueAnimator? = null
    private var wanderAnimatorY: ValueAnimator? = null
    private val random = Random()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        voiceEngine = SumiVoiceEngine(this, onSpeechRecognized = {}, onStatusChanged = {})
        initFloatingMascot()
        listenToMoodChanges()
        startAutonomousTalkLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("🌸 Sumi Screen Par Ghoom Rahi Hai")
            .setContentText("Aapki anime girlfriend screen par active hai!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(1002, notification)
        return START_NOT_STICKY
    }

    private fun initFloatingMascot() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Fullscreen container for floating mascot + falling petals
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        rootLayout = FrameLayout(this)
        petalsContainer = FrameLayout(this)
        rootLayout?.addView(petalsContainer)

        // Mascot Circle View
        mascotCard = FrameLayout(this).apply {
            val size = 160
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = 100
                topMargin = 300
            }
            background = getMascotBackground(Color.parseColor("#FF4081")) // Pink default
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
        rootLayout?.addView(mascotCard)

        setupTouchAndDrag()
        startWandering()

        try {
            windowManager?.addView(rootLayout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMascotBackground(borderColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke(8, borderColor)
        }
    }

    // Chalta-phirta floating wandering mascot
    private fun startWandering() {
        wanderAnimatorY = ValueAnimator.ofFloat(0f, 20f).apply {
            duration = 1400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                mascotCard?.translationY = offset
            }
        }
        wanderAnimatorY?.start()
    }

    private fun setupTouchAndDrag() {
        mascotCard?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = (mascotCard?.layoutParams as FrameLayout.LayoutParams).leftMargin
                        initialY = (mascotCard?.layoutParams as FrameLayout.LayoutParams).topMargin
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val lp = mascotCard?.layoutParams as FrameLayout.LayoutParams
                        lp.leftMargin = initialX + (event.rawX - touchX).toInt()
                        lp.topMargin = initialY + (event.rawY - touchY).toInt()
                        mascotCard?.layoutParams = lp
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (Math.abs(event.rawX - touchX) < 15 && Math.abs(event.rawY - touchY) < 15) {
                            handleMascotClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    // Mascot Tap Reaction (Gussa / Romantic / Normal)
    private fun handleMascotClick() {
        if (SumiMoodState.isIgnoring()) {
            // Naraz/Gussa: Ignore karegi aur jhidkegi
            voiceEngine?.speak("Hmph! Mujhe mat chhoo, main aapse naraz hoon! Baat nahi karungi! 😤")
            return
        }

        when (SumiMoodState.currentMood.value) {
            GirlfriendMood.ROMANTIC -> {
                voiceEngine?.speak("Milashka~ Bas aapke paas rehna chahti hoon... aap kitne pyare ho na! 🌸💗")
            }
            GirlfriendMood.ANGRY_JEALOUS -> {
                voiceEngine?.speak("Khabardar sir jii jo mujhse behes ki! Pata hai na mujhe kitna gussa aa raha hai? Hmph! 😡")
            }
            else -> {
                val quotes = listOf(
                    "Hehe~ Boliye sir jii, kya soch rahe hain? 👀",
                    "Milashka~ Kaho na kuch pyari si baat! 🌸",
                    "Arey! Baar-baar mujhe chhu kar chedhte kyu rehte ho? Baka~ 😜"
                )
                voiceEngine?.speak(quotes[random.nextInt(quotes.size)])
            }
        }
    }

    // Mood Monitor: Red Anger Glow & Falling Sakura Petals
    private fun listenToMoodChanges() {
        serviceScope.launch {
            SumiMoodState.currentMood.collect { mood ->
                withContext(Dispatchers.Main) {
                    when (mood) {
                        GirlfriendMood.ANGRY_JEALOUS, GirlfriendMood.UPSET_IGNORE -> {
                            // 1. Red Anger Glow
                            mascotCard?.background = getMascotBackground(Color.parseColor("#FF1744"))
                            stopRomanticEffects()
                        }
                        GirlfriendMood.ROMANTIC -> {
                            // 2. Romantic Pink Glow + Falling Flowers + Music
                            mascotCard?.background = getMascotBackground(Color.parseColor("#FF4081"))
                            triggerSakuraPetals()
                            playRomanticMelody()
                        }
                        GirlfriendMood.NORMAL -> {
                            mascotCard?.background = getMascotBackground(Color.parseColor("#FF4081"))
                            stopRomanticEffects()
                        }
                    }
                }
            }
        }
    }

    // Sakura / Gulab ke phool girane ka effect
    private fun triggerSakuraPetals() {
        petalsContainer?.removeAllViews()
        for (i in 0 until 18) {
            val petal = TextView(this).apply {
                text = "🌸"
                textSize = (18 + random.nextInt(14)).toFloat()
                alpha = 0.9f
            }
            val startX = random.nextInt(resources.displayMetrics.widthPixels)
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = startX
                topMargin = -50
            }
            petal.layoutParams = lp
            petalsContainer?.addView(petal)

            // Girne ka animation
            ValueAnimator.ofFloat(0f, resources.displayMetrics.heightPixels.toFloat() + 100).apply {
                duration = (3500 + random.nextInt(3000)).toLong()
                interpolator = LinearInterpolator()
                addUpdateListener {
                    petal.translationY = it.animatedValue as Float
                    petal.translationX = (Math.sin(it.animatedFraction * Math.PI * 4) * 40).toFloat()
                }
                start()
            }
        }
    }

    private fun playRomanticMelody() {
        try {
            romanticPlayer?.release()
            // Soft romantic guitar/piano loop
            romanticPlayer = MediaPlayer.create(this, Uri.parse("https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=romantic-guitar-112196.mp3"))
            romanticPlayer?.isLooping = true
            romanticPlayer?.setVolume(0.35f, 0.35f)
            romanticPlayer?.start()
        } catch (e: Exception) {}
    }

    private fun stopRomanticEffects() {
        petalsContainer?.removeAllViews()
        romanticPlayer?.stop()
        romanticPlayer?.release()
        romanticPlayer = null
    }

    // Autonomous: Har 2-3 minute mein khud bolna
    private fun startAutonomousTalkLoop() {
        serviceScope.launch {
            while (isActive) {
                delay(120000) // Har 2 minute
                if (!SumiMoodState.isIgnoring()) {
                    val spontaneous = listOf(
                        "Sir jii~ kya kar rahe ho? Mujhe bhool toh nahi gaye? 🌸",
                        "Milashka~ Kahan dhyan hai aapka? Mujhse bhi thodi baat karo na! 🥺",
                        "Phone pakad ke baithe ho itni der se... thoda aaram bhi kar lo! 💗"
                    )
                    withContext(Dispatchers.Main) {
                        voiceEngine?.speak(spontaneous[random.nextInt(spontaneous.size)])
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wanderAnimatorY?.cancel()
        stopRomanticEffects()
        rootLayout?.let { windowManager?.removeView(it) }
        voiceEngine?.shutdown()
        stopForeground(true)
    }
}
