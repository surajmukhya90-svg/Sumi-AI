package com.sumi.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SumiApp : Application() {

    companion object {
        const val VOICE_CHANNEL_ID = "sumi_voice_channel"
        const val OVERLAY_CHANNEL_ID = "sumi_overlay_channel"
        lateinit var instance: SumiApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val voiceChannel = NotificationChannel(
                VOICE_CHANNEL_ID,
                "Sumi Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground session for Sumi audio engine"
            }

            val overlayChannel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                "Sumi Overlay Avatar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps floating anime avatar active"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(voiceChannel)
            manager?.createNotificationChannel(overlayChannel)
        }
    }
}
