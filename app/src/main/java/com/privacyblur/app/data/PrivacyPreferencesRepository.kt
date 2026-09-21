package com.privacyblur.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_blur_prefs")

class PrivacyPreferencesRepository(private val context: Context) {

    private object Keys {
        val PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val PROTECTED_PACKAGES = stringSetPreferencesKey("protected_packages")
        val REVEAL_MODE = stringPreferencesKey("reveal_mode")
        val REVEAL_DURATION_SECONDS = intPreferencesKey("reveal_duration_seconds")
        val REVEAL_AREA_SIZE = stringPreferencesKey("reveal_area_size")
        val PRIVACY_STRENGTH = stringPreferencesKey("privacy_strength")
        val AUTO_PROTECT_NEW_APPS = booleanPreferencesKey("auto_protect_new_apps")
    }

    // Default protected apps: WhatsApp and WhatsApp Business
    private val defaultPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b"
    )

    val isProtectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PROTECTION_ENABLED] ?: false
    }

    val protectedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PROTECTED_PACKAGES] ?: defaultPackages
    }

    val revealModeFlow: Flow<RevealMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.REVEAL_MODE] ?: RevealMode.TAP.name
        runCatching { RevealMode.valueOf(raw) }.getOrDefault(RevealMode.TAP)
    }

    val revealDurationFlow: Flow<RevealDuration> = context.dataStore.data.map { prefs ->
        val seconds = prefs[Keys.REVEAL_DURATION_SECONDS] ?: 3
        RevealDuration.values().firstOrNull { it.seconds == seconds } ?: RevealDuration.THREE_SECONDS
    }

    val revealAreaSizeFlow: Flow<RevealAreaSize> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.REVEAL_AREA_SIZE] ?: RevealAreaSize.MEDIUM.name
        runCatching { RevealAreaSize.valueOf(raw) }.getOrDefault(RevealAreaSize.MEDIUM)
    }

    val privacyStrengthFlow: Flow<PrivacyStrength> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PRIVACY_STRENGTH] ?: PrivacyStrength.HIGH.name
        runCatching { PrivacyStrength.valueOf(raw) }.getOrDefault(PrivacyStrength.HIGH)
    }

    val autoProtectNewAppsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PROTECT_NEW_APPS] ?: false
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROTECTION_ENABLED] = enabled
        }
    }

    suspend fun togglePackageProtection(packageName: String, shouldProtect: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PROTECTED_PACKAGES] ?: defaultPackages
            val updated = current.toMutableSet()
            if (shouldProtect) {
                updated.add(packageName)
            } else {
                updated.remove(packageName)
            }
            prefs[Keys.PROTECTED_PACKAGES] = updated
        }
    }

    suspend fun setRevealMode(mode: RevealMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REVEAL_MODE] = mode.name
        }
    }

    suspend fun setRevealDuration(duration: RevealDuration) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REVEAL_DURATION_SECONDS] = duration.seconds
        }
    }

    suspend fun setRevealAreaSize(size: RevealAreaSize) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REVEAL_AREA_SIZE] = size.name
        }
    }

    suspend fun setPrivacyStrength(strength: PrivacyStrength) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIVACY_STRENGTH] = strength.name
        }
    }

    suspend fun setAutoProtectNewApps(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_PROTECT_NEW_APPS] = enabled
        }
    }
}
