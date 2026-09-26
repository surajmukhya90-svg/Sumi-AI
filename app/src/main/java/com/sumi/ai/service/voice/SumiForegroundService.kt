package com.sumi.ai.service.voice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp

class SumiForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.VOICE_CHANNEL_ID)
            .setContentTitle("Sumi Active")
            .setContentText("Listening and ready in Hindi/Hinglish")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
        return START_STICKY
    }
}
