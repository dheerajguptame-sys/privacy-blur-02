package com.privacyblur.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.privacyblur.app.data.PrivacyPreferencesRepository

class PrivacyBlurApplication : Application() {

    lateinit var preferencesRepository: PrivacyPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PrivacyPreferencesRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROTECTION,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_PROTECTION = "privacy_blur_protection_channel"
        lateinit var instance: PrivacyBlurApplication
            private set
    }
}
