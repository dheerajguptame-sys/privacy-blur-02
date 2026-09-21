package com.privacyblur.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyblur.app.PrivacyBlurApplication
import com.privacyblur.app.data.InstalledAppInfo
import com.privacyblur.app.data.PrivacyStrength
import com.privacyblur.app.data.RevealAreaSize
import com.privacyblur.app.data.RevealDuration
import com.privacyblur.app.data.RevealMode
import com.privacyblur.app.service.PrivacyOverlayService
import com.privacyblur.app.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PrivacyBlurApplication).preferencesRepository

    enum class Screen {
        DASHBOARD,
        PROTECTED_APPS
    }

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val isProtectionEnabled = repository.isProtectionEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val protectedPackages = repository.protectedPackagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("com.whatsapp")
    )
    val revealMode = repository.revealModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), RevealMode.TAP
    )
    val revealDuration = repository.revealDurationFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), RevealDuration.THREE_SECONDS
    )
    val revealAreaSize = repository.revealAreaSizeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), RevealAreaSize.MEDIUM
    )
    val privacyStrength = repository.privacyStrengthFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), PrivacyStrength.HIGH
    )

    // Permission states
    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission = _hasOverlayPermission.asStateFlow()

    private val _hasAccessibilityPermission = MutableStateFlow(false)
    val hasAccessibilityPermission = _hasAccessibilityPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission = _hasNotificationPermission.asStateFlow()

    // Installed apps
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        refreshPermissionStatus()
        loadInstalledApps()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun refreshPermissionStatus() {
        val context = getApplication<Application>()
        _hasOverlayPermission.value = PermissionUtils.hasOverlayPermission(context)
        _hasAccessibilityPermission.value = PermissionUtils.hasAccessibilityPermission(context)
        _hasNotificationPermission.value = PermissionUtils.hasNotificationPermission(context)
    }

    fun toggleProtection(enable: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            if (enable) {
                if (!PermissionUtils.areAllRequiredPermissionsGranted(context)) {
                    refreshPermissionStatus()
                    return@launch
                }
                repository.setProtectionEnabled(true)
                PrivacyOverlayService.start(context)
            } else {
                repository.setProtectionEnabled(false)
                PrivacyOverlayService.stop(context)
            }
        }
    }

    fun toggleAppProtection(packageName: String, shouldProtect: Boolean) {
        viewModelScope.launch {
            repository.togglePackageProtection(packageName, shouldProtect)
            loadInstalledApps()
        }
    }

    fun setRevealMode(mode: RevealMode) {
        viewModelScope.launch { repository.setRevealMode(mode) }
    }

    fun setRevealDuration(duration: RevealDuration) {
        viewModelScope.launch { repository.setRevealDuration(duration) }
    }

    fun setRevealAreaSize(size: RevealAreaSize) {
        viewModelScope.launch { repository.setRevealAreaSize(size) }
    }

    fun setPrivacyStrength(strength: PrivacyStrength) {
        viewModelScope.launch { repository.setPrivacyStrength(strength) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val currentProtected = protectedPackages.value

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == getApplication<Application>().packageName) return@mapNotNull null

                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val isWhatsapp = pkg.contains("whatsapp", ignoreCase = true)

                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    isProtected = currentProtected.contains(pkg),
                    isPrimaryTarget = isWhatsapp,
                    icon = icon
                )
            }.distinctBy { it.packageName }
             .sortedWith(compareByDescending<InstalledAppInfo> { it.isPrimaryTarget }
                 .thenByDescending { it.isProtected }
                 .thenBy { it.appName })

            _installedApps.value = apps
        }
    }

    fun requestOverlayPermission() {
        PermissionUtils.openOverlaySettings(getApplication())
    }

    fun requestAccessibilityPermission() {
        PermissionUtils.openAccessibilitySettings(getApplication())
    }
}
