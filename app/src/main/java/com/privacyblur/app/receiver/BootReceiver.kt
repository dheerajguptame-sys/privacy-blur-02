package com.privacyblur.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privacyblur.app.PrivacyBlurApplication
import com.privacyblur.app.service.PrivacyOverlayService
import com.privacyblur.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as? PrivacyBlurApplication ?: return
        val repository = app.preferencesRepository

        CoroutineScope(Dispatchers.Default).launch {
            val isEnabled = repository.isProtectionEnabledFlow.first()
            if (isEnabled && PermissionUtils.areAllRequiredPermissionsGranted(context)) {
                PrivacyOverlayService.start(context)
            }
        }
    }
}
