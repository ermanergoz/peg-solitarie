package com.app.pegsolitaire.ui.window

import android.view.Window

/**
 * The bar seam's recording fake (docs/tech-design.md §3.8 (a)): every [apply] the app makes, in
 * order, with the window it named. The tests read what the app asked for here and never the
 * platform, which under Robolectric shows no bar to hide (§3.8 (g)); the device is the proof that
 * the bars obey (criteria 30g–30n).
 */
internal class RecordingSystemBars : SystemBars {
    private val applied = mutableListOf<Applied>()

    /** Every call so far, oldest first. */
    val records: List<Applied> get() = applied.toList()

    override fun apply(
        window: Window,
        state: SystemBarsState,
    ) {
        applied += Applied(window, state)
    }

    /** Forgets what was recorded, so a test can read only what follows an action. */
    fun clear() {
        applied.clear()
    }

    /** One [apply]: the window it named and the state it asked for. */
    data class Applied(
        val window: Window,
        val state: SystemBarsState,
    )
}
