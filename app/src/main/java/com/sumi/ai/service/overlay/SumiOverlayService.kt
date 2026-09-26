package com.sumi.ai.service.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sumi.ai.SumiApp

class SumiOverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, SumiApp.OVERLAY_CHANNEL_ID)
            .setContentTitle("Sumi Bubble Active")
            .setContentText("Sumi is floating on your screen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1002, notification)
        return START_NOT_STICKY
    }
}
