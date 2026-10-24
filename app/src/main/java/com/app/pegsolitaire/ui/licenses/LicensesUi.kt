package com.app.pegsolitaire.ui.licenses

/**
 * The licences screens' own timing and counts (spec §4, §4A; brief §0.3, F6), scoped here rather
 * than in `ClayAnim` because no other screen uses them.
 */
internal object LicensesUi {
    /**
     * How long a loading screen shows nothing before its spinner appears (spec §4: "nothing for the
     * first 300 ms"). Shared by the three screens that load; a text that arrives sooner never
     * flashes a spinner. Read on the composition clock, so the Compose test clock drives it.
     */
    const val SPINNER_DELAY_MS = 300L

    /**
     * How many items of each licences list join the entrance stagger, the intro counted as item 0
     * (spec §3.1 and §4A as amended in loop 2, F-23, answering brief F6): 8 × 40 ms after the 240 ms
     * entrance puts the first screenful in within about 560 ms; later items enter plainly.
     */
    const val STAGGERED_ROWS = 8

    /**
     * The entrance order of the list item at [position] (0 = the intro), or `null` past the first
     * [STAGGERED_ROWS] items: an item composed as it scrolls in must not wait out a long delay.
     */
    fun entranceOrderOf(position: Int): Int? = position.takeIf { it < STAGGERED_ROWS }
}
