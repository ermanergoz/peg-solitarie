package com.app.pegsolitaire.ui.game

/**
 * Formats the in-game elapsed timer (D-04) as a zero-padded `mm:ss` clock.
 *
 * The displayed time is a *computed* string, not a string resource — only the
 * numeric tokens vary, so the format is owned here (with named constants, no inline
 * magic) rather than in `strings.xml`. Minutes are not capped at 60: a long game
 * reads `75:09` rather than rolling into hours.
 */
internal object GameTimerFormat {
    /** Whole seconds in one minute — splits the elapsed total into mm and ss. */
    private const val SECONDS_PER_MINUTE = 60

    /** Zero-padded `mm:ss` pattern (e.g. `01:09`). */
    private const val CLOCK_PATTERN = "%02d:%02d"

    /** Renders [totalSeconds] as `mm:ss` (e.g. `69` → `01:09`). */
    fun format(totalSeconds: Int): String {
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return CLOCK_PATTERN.format(minutes, seconds)
    }
}
