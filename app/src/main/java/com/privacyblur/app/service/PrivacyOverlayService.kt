package com.privacyblur.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.privacyblur.app.PrivacyBlurApplication
import com.privacyblur.app.R
import com.privacyblur.app.data.PrivacyPreferencesRepository
import com.privacyblur.app.overlay.PrivacyOverlayView
import com.privacyblur.app.ui.MainActivity
import com.privacyblur.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PrivacyOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var repository: PrivacyPreferencesRepository

    private var overlayView: PrivacyOverlayView? = null
    private var isOverlayAttached = false

    private var isProtectionEnabled = false
    private var protectedPackages = setOf<String>()
    private var currentForegroundPackage: String? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Instantly detach overlay on screen lock so it never interferes with lock screen
                    hideOverlay()
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Re-evaluate foreground app on unlock
                    evaluateOverlayState()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = (application as PrivacyBlurApplication).preferencesRepository

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)

        observePreferences()
    }

    private fun observePreferences() {
        serviceScope.launch {
            combine(
                repository.isProtectionEnabledFlow,
                repository.protectedPackagesFlow,
                repository.revealModeFlow,
                repository.revealDurationFlow,
                repository.revealAreaSizeFlow,
                repository.privacyStrengthFlow
            ) { enabled, packages, mode, duration, size, strength ->
                isProtectionEnabled = enabled
                protectedPackages = packages
                overlayView?.updateSettings(mode, duration, size, strength)
                evaluateOverlayState()
            }.collect {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                serviceScope.launch {
                    repository.setProtectionEnabled(false)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_FOREGROUND_APP_CHANGED -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                currentForegroundPackage = pkg
                evaluateOverlayState()
            }
        }

        return START_STICKY
    }

    private fun evaluateOverlayState() {
        if (!isProtectionEnabled || !PermissionUtils.hasOverlayPermission(this)) {
            hideOverlay()
            return
        }

        val pkg = currentForegroundPackage
        val shouldProtect = pkg != null && protectedPackages.contains(pkg)

        if (shouldProtect) {
            showOverlay(pkg ?: "")
        } else {
            hideOverlay()
        }
    }

    private fun showOverlay(packageName: String) {
        if (isOverlayAttached && overlayView != null) return

        if (!PermissionUtils.hasOverlayPermission(this)) {
            hideOverlay()
            return
        }

        val view = PrivacyOverlayView(this).apply {
            setAppTitle(packageName)
        }
        overlayView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(view, params)
            isOverlayAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            isOverlayAttached = false
        }
    }

    private fun hideOverlay() {
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayAttached = false
        }
    }

    private fun startForegroundWithNotification() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PrivacyOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, PrivacyBlurApplication.CHANNEL_ID_PROTECTION)
            .setSmallIcon(R.drawable.ic_notification_privacy)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setContentIntent(openAppIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        unregisterReceiver(screenStateReceiver)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_FOREGROUND_APP_CHANGED = "com.privacyblur.app.ACTION_FOREGROUND_APP_CHANGED"
        const val ACTION_STOP_SERVICE = "com.privacyblur.app.ACTION_STOP_SERVICE"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun start(context: Context) {
            val intent = Intent(context, PrivacyOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrivacyOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
