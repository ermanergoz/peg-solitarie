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
     * How many rows of the Google notices list join the entrance stagger: the first screenful
     * (spec §4A); later rows enter plainly. 8 × 40 ms after the 240 ms entrance puts the first
     * screenful in within about 560 ms. The count is the ux-designer's to confirm (brief F6).
     */
    const val STAGGERED_ROWS = 8
}
