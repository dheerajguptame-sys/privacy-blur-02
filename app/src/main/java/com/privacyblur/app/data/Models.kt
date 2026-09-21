package com.privacyblur.app.data

import android.graphics.drawable.Drawable

enum class RevealMode(val title: String, val description: String) {
    TAP("Tap to Reveal", "Tapping shows clear circle for set duration"),
    HOLD("Hold to Reveal", "Reveals only while finger is held on screen")
}

enum class RevealDuration(val seconds: Int, val label: String) {
    ONE_SECOND(1, "1 Second"),
    THREE_SECONDS(3, "3 Seconds"),
    FIVE_SECONDS(5, "5 Seconds"),
    HOLD_ONLY(0, "Hold Only")
}

enum class RevealAreaSize(val radiusDp: Float, val label: String) {
    SMALL(70f, "Small (70dp)"),
    MEDIUM(105f, "Medium (105dp)"),
    LARGE(140f, "Large (140dp)")
}

enum class PrivacyStrength(val alpha: Float, val label: String, val description: String) {
    LOW(0.70f, "Low", "Subtle cover (30% transparency)"),
    MEDIUM(0.85f, "Medium", "Balanced frosted cover"),
    HIGH(0.96f, "High", "Heavy opaque privacy shield"),
    MAXIMUM(1.00f, "Maximum", "Complete blackout shield")
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isProtected: Boolean,
    val isPrimaryTarget: Boolean = false,
    val icon: Drawable? = null
)
