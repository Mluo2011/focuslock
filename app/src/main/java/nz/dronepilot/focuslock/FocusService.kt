package nz.dronepilot.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Date

class FocusService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { refresh() }
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> showing = true; refresh() }
    private var overlay: View? = null
    private var timer: TextView? = null
    private var until: TextView? = null
    private var ring: RingView? = null
    private var showing = true

    override fun onServiceConnected() {
        Focus.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        refresh()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        val next = Focus.overlayFor(pkg, showing)
        if (next != showing) { showing = next; refresh() }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        Focus.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        handler.removeCallbacks(tick)
        hide()
        super.onDestroy()
    }

    private fun refresh() {
        handler.removeCallbacks(tick)
        val end = Focus.end(this)
        val left = Focus.remaining(end, System.currentTimeMillis())
        if (left == 0L) { hide(); return }
        if (showing) show() else hide()
        timer?.text = Focus.format(left)
        until?.text = "Unlocks at " + DateFormat.getTimeFormat(this).format(Date(end))
        ring?.progress = left.toFloat() / Focus.total(this).coerceAtLeast(left)
        handler.postDelayed(tick, 1000)
    }

    private fun show() {
        if (overlay != null) return
        val c = this
        val ringSize = Ui.dp(c, 300)
        val ringView = RingView(c)
        val time = Ui.text(c, "", 68f, bold = true)
        val center = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(Ui.text(c, "FOCUS MODE", 15f, Ui.ACCENT, bold = true).apply { letterSpacing = 0.25f })
            addView(FrameLayout(c).apply {
                addView(ringView, FrameLayout.LayoutParams(ringSize, ringSize))
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(time)
                    addView(Ui.text(c, "remaining", 16f, Ui.MUTED))
                }, FrameLayout.LayoutParams(ringSize, ringSize))
            }, LinearLayout.LayoutParams(ringSize, ringSize).apply { topMargin = Ui.dp(c, 32); bottomMargin = Ui.dp(c, 32) })
        }
        val untilView = Ui.text(c, "", 17f, Ui.MUTED)
        center.addView(untilView)
        val emergency = Ui.text(c, "Emergency call", 16f, Ui.TEXT).apply {
            background = Ui.pill(c, Ui.CARD, Ui.TRACK)
            setPadding(Ui.dp(c, 32), Ui.dp(c, 14), Ui.dp(c, 32), Ui.dp(c, 14))
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:111")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val root = FrameLayout(c).apply {
            setBackgroundColor(Ui.BG)
            addView(center, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
            addView(emergency, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = Ui.dp(c, 72)
            })
        }
        val lp = LP(
            LP.MATCH_PARENT, LP.MATCH_PARENT, LP.TYPE_ACCESSIBILITY_OVERLAY,
            LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_LAYOUT_NO_LIMITS, PixelFormat.OPAQUE,
        ).apply {
            layoutInDisplayCutoutMode = LP.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
        getSystemService(WindowManager::class.java).addView(root, lp)
        overlay = root; timer = time; until = untilView; ring = ringView
    }

    private fun hide() {
        overlay?.let { getSystemService(WindowManager::class.java).removeView(it) }
        overlay = null; timer = null; until = null; ring = null
    }
}
