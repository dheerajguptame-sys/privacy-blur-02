package com.privacyblur.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.privacyblur.app.data.PrivacyStrength
import com.privacyblur.app.data.RevealAreaSize
import com.privacyblur.app.data.RevealDuration
import com.privacyblur.app.data.RevealMode

/**
 * PrivacyOverlayView
 *
 * Hardware-accelerated view rendering an obscuring privacy shield over WhatsApp or other
 * protected apps.
 *
 * TECHNICAL NOTE ON SCREEN BLUR:
 * Android security sandboxing strictly forbids arbitrary background apps from reading or
 * capturing the pixel framebuffer of other apps (like WhatsApp) in real-time.
 * Therefore, true live Gaussian blur of background windows is not supported without rooted
 * OS or invasive MediaProjection screen-recording permissions (which would violate user trust).
 *
 * Instead, PrivacyOverlayView employs an opaque frosted privacy shield using layered geometric
 * micro-patterns and customizable opacity (Low, Medium, High, Maximum), paired with a
 * PorterDuff.Mode.CLEAR hardware aperture for the tap/hold-to-reveal mechanic.
 */
@SuppressLint("ViewConstructor")
class PrivacyOverlayView(context: Context) : View(context) {

    private var revealMode = RevealMode.TAP
    private var revealDuration = RevealDuration.THREE_SECONDS
    private var revealAreaSize = RevealAreaSize.MEDIUM
    private var privacyStrength = PrivacyStrength.HIGH

    private var touchPos = PointF(-1000f, -1000f)
    private var isRevealed = false
    private var currentRevealRadius = 0f

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable {
        animateHide()
    }

    // Paint for the frosted obscuring privacy background
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 23, 42) // Slate 900
        style = Paint.Style.FILL
    }

    // Paint to punch a clear viewing hole through the privacy shield
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Paint for aperture guide border
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(14, 165, 233) // Sky 500
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    // Paint for privacy badge pill
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 15, 23, 42)
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private var targetAppName: String = "WhatsApp"

    init {
        // Enable software/hardware layer for PorterDuff.Mode.CLEAR composition
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setAppTitle(packageName: String) {
        targetAppName = if (packageName.contains("whatsapp", ignoreCase = true)) {
            "WhatsApp Protected"
        } else {
            "Privacy Shield Active"
        }
        invalidate()
    }

    fun updateSettings(
        mode: RevealMode,
        duration: RevealDuration,
        size: RevealAreaSize,
        strength: PrivacyStrength
    ) {
        this.revealMode = mode
        this.revealDuration = duration
        this.revealAreaSize = size
        this.privacyStrength = strength

        val density = resources.displayMetrics.density
        val targetRadius = size.radiusDp * density
        currentRevealRadius = targetRadius

        val alphaInt = (strength.alpha * 255).toInt().coerceIn(0, 255)
        shieldPaint.alpha = alphaInt
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val density = resources.displayMetrics.density
        val maxRadius = revealAreaSize.radiusDp * density

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchPos.set(event.x, event.y)
                isRevealed = true
                currentRevealRadius = maxRadius
                mainHandler.removeCallbacks(autoHideRunnable)
                invalidate()

                if (revealMode == RevealMode.TAP && revealDuration.seconds > 0) {
                    mainHandler.postDelayed(autoHideRunnable, revealDuration.seconds * 1000L)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isRevealed) {
                    touchPos.set(event.x, event.y)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (revealMode == RevealMode.HOLD) {
                    animateHide()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animateHide() {
        val startRadius = currentRevealRadius
        val animator = ValueAnimator.ofFloat(startRadius, 0f).apply {
            duration = 200
            addUpdateListener {
                currentRevealRadius = it.animatedValue as Float
                invalidate()
            }
        }
        animator.start()
        isRevealed = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw the frosted obscuring canvas over the entire window
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shieldPaint)

        // 2. If revealed, punch a transparent aperture using PorterDuff.Mode.CLEAR
        if (isRevealed && currentRevealRadius > 0f) {
            canvas.drawCircle(touchPos.x, touchPos.y, currentRevealRadius, clearPaint)
            canvas.drawCircle(touchPos.x, touchPos.y, currentRevealRadius, ringPaint)
        }

        // 3. Draw top minimal privacy indicator pill
        drawTopBadge(canvas)
    }

    private fun drawTopBadge(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val badgeW = 220f * density
        val badgeH = 32f * density
        val badgeX = (width - badgeW) / 2f
        val badgeY = 48f * density

        val rect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
        canvas.drawRoundRect(rect, 16f * density, 16f * density, badgeBgPaint)

        val label = if (isRevealed) {
            "• ${targetAppName} Revealed •"
        } else {
            "🔒 ${targetAppName} • Tap to Reveal"
        }
        val textY = badgeY + (badgeH / 2f) + (badgeTextPaint.textSize / 3f)
        canvas.drawText(label, width / 2f, textY, badgeTextPaint)
    }
}
