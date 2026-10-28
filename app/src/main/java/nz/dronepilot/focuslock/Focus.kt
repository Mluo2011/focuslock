package nz.dronepilot.focuslock

import android.content.Context

object Focus {
    // ponytail: hard cap so a typo can't lock the phone for days
    const val MAX_MINUTES = 12 * 60

    // Phone apps stay usable so calls (incl. emergency 111) always get through.
    val CALL_APPS = setOf(
        "com.android.incallui", "com.android.phone", "com.android.server.telecom",
        "com.android.dialer", "com.google.android.dialer", "com.android.contacts",
    )

    // Windows that pop over whatever is open (shade, power menu, us) don't change the decision.
    private val PASSTHROUGH = setOf("com.android.systemui", "android", "nz.dronepilot.focuslock")

    fun overlayFor(pkg: String, showing: Boolean): Boolean = when (pkg) {
        in CALL_APPS -> false
        in PASSTHROUGH -> showing
        else -> true
    }

    fun remaining(end: Long, now: Long): Long = (end - now).coerceAtLeast(0)

    /** Countdown text: "25:00", or "1:30:00" once it's an hour or more. */
    fun format(ms: Long): String {
        val s = (ms + 999) / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
        else "%d:%02d".format(s / 60, s % 60)
    }

    /** Duration label: "25 min", "2 h", "1 h 30 min". */
    fun label(min: Int): String = when {
        min < 60 -> "$min min"
        min % 60 == 0 -> "${min / 60} h"
        else -> "${min / 60} h ${min % 60} min"
    }

    fun prefs(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)
    fun end(c: Context) = prefs(c).getLong("end", 0)
    fun total(c: Context) = prefs(c).getLong("total", 0)
    fun start(c: Context, minutes: Int) {
        val total = minutes * 60_000L
        prefs(c).edit().putLong("total", total).putLong("end", System.currentTimeMillis() + total).commit()
    }
}
