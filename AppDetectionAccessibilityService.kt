package com.privacyblur.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * AppDetectionAccessibilityService
 *
 * PRIVACY CONTRACT:
 * - This service ONLY listens to TYPE_WINDOW_STATE_CHANGED events to detect when a user
 *   navigates into a protected app (such as WhatsApp).
 * - "canRetrieveWindowContent" is set to FALSE in accessibility_service_config.xml.
 * - This service NEVER reads accessibility node hierarchies, text fields, contacts, or messages.
 * - No analytics or network calls are performed.
 */
class AppDetectionAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Filter out system dialogs, lock screens, and permission controllers
        if (isSystemProtectedPackage(pkgName)) return

        // Dispatch foreground package change to our overlay manager service
        val intent = Intent(this, PrivacyOverlayService::class.java).apply {
            action = PrivacyOverlayService.ACTION_FOREGROUND_APP_CHANGED
            putExtra(PrivacyOverlayService.EXTRA_PACKAGE_NAME, pkgName)
        }
        startService(intent)
    }

    private fun isSystemProtectedPackage(packageName: String): Boolean {
        return packageName.startsWith("com.android.systemui") ||
               packageName.startsWith("com.android.permissioncontroller") ||
               packageName.startsWith("com.google.android.packageinstaller") ||
               packageName == "android" ||
               packageName == this.packageName
    }

    override fun onInterrupt() {
        // Required by AccessibilityService interface
    }
}
