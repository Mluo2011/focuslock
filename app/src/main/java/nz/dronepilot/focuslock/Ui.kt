package nz.dronepilot.focuslock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

object Ui {
    const val BG = 0xFF0E0F14.toInt()
    const val CARD = 0xFF1A1C25.toInt()
    const val TRACK = 0xFF2A2C38.toInt()
    const val ACCENT = 0xFF7C8CFF.toInt()
    const val TEXT = 0xFFE8EAF6.toInt()
    const val MUTED = 0xFF8A8FA8.toInt()

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun pill(c: Context, fill: Int, stroke: Int = 0) = GradientDrawable().apply {
        cornerRadius = dp(c, 28).toFloat()
        setColor(fill)
        if (stroke != 0) setStroke(dp(c, 1), stroke)
    }

    fun text(c: Context, s: String, size: Float, color: Int = TEXT, bold: Boolean = false) = TextView(c).apply {
        text = s; textSize = size; setTextColor(color)
        gravity = android.view.Gravity.CENTER
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
}

/** Ring that empties as focus time runs out. */
class RingView(c: Context) : View(c) {
    var progress = 1f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    private val stroke = Ui.dp(c, 10).toFloat()
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; color = Ui.TRACK }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke; color = Ui.ACCENT; strokeCap = Paint.Cap.ROUND
    }
    private val box = RectF()

    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) / 2f - stroke
        box.set(width / 2f - r, height / 2f - r, width / 2f + r, height / 2f + r)
        canvas.drawOval(box, track)
        canvas.drawArc(box, -90f, 360f * progress, false, arc)
    }
}
