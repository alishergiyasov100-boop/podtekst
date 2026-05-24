package com.podtekst.decoder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class PodtekstApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_OVERLAY,
                "Подтекст · оверлей",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Фоновое окно киберпанк-расшифровки"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY = "podtekst_overlay"
    }
}
