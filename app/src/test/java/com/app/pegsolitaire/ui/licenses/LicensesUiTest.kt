package com.app.pegsolitaire.ui.licenses

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The entrance stagger's cap (spec §3.1 and §4A as amended in loop 2, F-23): on both lists only the
 * first eight items join the stagger, the intro counted as item 0; every later item, composed as it
 * scrolls in, enters plainly instead of after a long delay.
 */
class LicensesUiTest {
    @Test
    fun onlyTheFirstEightItemsJoinTheStaggerTheIntroCounted() {
        assertThat((0..7).map { LicensesUi.entranceOrderOf(it) }).containsExactly(0, 1, 2, 3, 4, 5, 6, 7).inOrder()
        assertThat(LicensesUi.entranceOrderOf(8)).isNull()
        assertThat(LicensesUi.entranceOrderOf(40)).isNull()
    }
}
