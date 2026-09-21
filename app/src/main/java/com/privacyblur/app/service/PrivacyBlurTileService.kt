package com.privacyblur.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.privacyblur.app.PrivacyBlurApplication
import com.privacyblur.app.data.PrivacyPreferencesRepository
import com.privacyblur.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrivacyBlurTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: PrivacyPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        repository = (application as PrivacyBlurApplication).preferencesRepository
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        tileScope.launch {
            val isEnabled = repository.isProtectionEnabledFlow.first()
            val tile = qsTile ?: return@launch
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Privacy Blur"
            tile.subtitle = if (isEnabled) "Active" else "Off"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        tileScope.launch {
            val current = repository.isProtectionEnabledFlow.first()
            val nextState = !current

            if (nextState) {
                if (!PermissionUtils.areAllRequiredPermissionsGranted(this@PrivacyBlurTileService)) {
                    // Open main activity if permissions are missing
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivityAndCollapse(intent)
                    return@launch
                }
                repository.setProtectionEnabled(true)
                PrivacyOverlayService.start(this@PrivacyBlurTileService)
            } else {
                repository.setProtectionEnabled(false)
                PrivacyOverlayService.stop(this@PrivacyBlurTileService)
            }
            updateTileState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tileScope.cancel()
    }
}
