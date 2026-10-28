package nz.dronepilot.focuslock

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams as LLP
import android.widget.TextView

class MainActivity : Activity() {
    private val presets = listOf(15, 25, 45, 60, 90, 120)
    private var minutes = 25
    private lateinit var amount: EditText
    private lateinit var start: TextView
    private lateinit var setup: View
    private val chips = mutableListOf<Pair<Int, TextView>>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        val c = this
        fun dp(v: Int) = Ui.dp(c, v)

        amount = EditText(c).apply {
            textSize = 44f; setTextColor(Ui.TEXT); gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(3))
            background = null
            setSelectAllOnFocus(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun afterTextChanged(s: Editable?) { minutes = s.toString().toIntOrNull() ?: 0; update() }
            })
        }
        val typed = LinearLayout(c).apply {
            gravity = Gravity.CENTER
            addView(amount, LLP(-2, -2))
            addView(Ui.text(c, "min", 22f, Ui.MUTED))
        }
        val stepper = LinearLayout(c).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.pill(c, Ui.CARD)
            setPadding(dp(12), dp(20), dp(12), dp(20))
            addView(round("−") { step(-5) }, LLP(dp(56), dp(56)))
            addView(typed, LLP(0, -2, 1f))
            addView(round("+") { step(5) }, LLP(dp(56), dp(56)))
        }

        val grid = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        presets.chunked(3).forEach { row ->
            grid.addView(LinearLayout(c).apply {
                row.forEach { m ->
                    val chip = Ui.text(c, Focus.label(m), 16f).apply {
                        setPadding(0, dp(14), 0, dp(14))
                        setOnClickListener { setMinutes(m) }
                    }
                    chips += m to chip
                    addView(chip, LLP(0, -2, 1f).apply { setMargins(dp(5), dp(5), dp(5), dp(5)) })
                }
            })
        }

        setup = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.pill(c, Ui.CARD, 0xFF5A4A2A.toInt())
            setPadding(dp(20), dp(16), dp(20), dp(16))
            addView(Ui.text(c, "One-time setup: turn on Focus Lock in Accessibility so it can lock the screen.", 14f, 0xFFF0C674.toInt()))
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        start = Ui.text(c, "", 18f, Ui.BG, bold = true).apply {
            background = Ui.pill(c, Ui.ACCENT)
            setPadding(0, dp(18), 0, dp(18))
            setOnClickListener { confirm() }
        }

        setContentView(LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(dp(24), dp(56), dp(24), dp(32))
            addView(Ui.text(c, "Focus Lock", 34f, bold = true).apply { gravity = Gravity.START })
            addView(Ui.text(c, "Put the phone away. Get it done.", 16f, Ui.MUTED).apply { gravity = Gravity.START })
            addView(View(c), LLP(-1, 0, 1f))
            addView(stepper, LLP(-1, -2))
            addView(grid, LLP(-1, -2).apply { topMargin = dp(16) })
            addView(View(c), LLP(-1, 0, 1f))
            addView(setup, LLP(-1, -2).apply { bottomMargin = dp(16) })
            addView(start, LLP(-1, -2))
            addView(Ui.text(c, "Can't be cancelled, even by restarting. Calls still work.", 13f, Ui.MUTED),
                LLP(-1, -2).apply { topMargin = dp(12) })
        })
        setMinutes(minutes)
    }

    override fun onResume() {
        super.onResume()
        setup.visibility = if (serviceEnabled()) View.GONE else View.VISIBLE
    }

    private fun round(label: String, onClick: () -> Unit) = Ui.text(this, label, 26f, Ui.ACCENT).apply {
        background = Ui.pill(this@MainActivity, Ui.TRACK)
        setOnClickListener { onClick() }
    }

    private fun step(by: Int) = setMinutes((minutes + by).coerceIn(1, Focus.MAX_MINUTES))

    private fun setMinutes(m: Int) {
        amount.setText(m.toString())  // triggers update() via the watcher
        amount.clearFocus()
    }

    private fun update() {
        val ok = minutes in 1..Focus.MAX_MINUTES
        start.text = if (ok) "Start focus · ${Focus.label(minutes)}" else "Enter 1–${Focus.MAX_MINUTES} minutes"
        start.alpha = if (ok) 1f else 0.4f
        chips.forEach { (m, chip) ->
            val on = m == minutes
            chip.background = Ui.pill(this, if (on) Ui.ACCENT else Ui.CARD)
            chip.setTextColor(if (on) Ui.BG else Ui.TEXT)
        }
    }

    private fun confirm() {
        if (minutes !in 1..Focus.MAX_MINUTES) return
        if (!serviceEnabled()) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Lock for ${Focus.label(minutes)}?")
            .setMessage("You won't be able to use any app or cancel until the timer ends, even after a restart. Calls and emergency calls still work.")
            .setPositiveButton("Lock") { _, _ -> Focus.start(this, minutes) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun serviceEnabled(): Boolean {
        val me = ComponentName(this, FocusService::class.java).flattenToString()
        val list = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return list.split(':').any { ComponentName.unflattenFromString(it)?.flattenToString() == me }
    }
}
