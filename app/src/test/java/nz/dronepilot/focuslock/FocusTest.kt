package nz.dronepilot.focuslock

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTest {
    @Test fun overlayDecision() {
        assertEquals(true, Focus.overlayFor("com.android.chrome", false))
        assertEquals(false, Focus.overlayFor("com.android.incallui", true))
        assertEquals(false, Focus.overlayFor("com.android.systemui", false)) // shade over dialer keeps it hidden
        assertEquals(true, Focus.overlayFor("com.android.systemui", true))   // shade can't reveal apps
    }

    @Test fun timer() {
        assertEquals(0L, Focus.remaining(1000, 5000))
        assertEquals("0:01", Focus.format(1))
        assertEquals("25:00", Focus.format(25 * 60_000L))
        assertEquals("1:30:00", Focus.format(90 * 60_000L))
    }

    @Test fun labels() {
        assertEquals("25 min", Focus.label(25))
        assertEquals("2 h", Focus.label(120))
        assertEquals("1 h 30 min", Focus.label(90))
    }
}
