package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.backdropSemantics
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BD-15: the backdrop root carries its tag and the three test-only properties, and nothing a
 * screen reader or a pointer can act on — no description, no text, no click, no focus.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SEMANTICS_ROBOLECTRIC_SDK])
class BackdropSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBackdropIsNotFocusableClickableOrAnnounced() {
        rule.setContent { AppNavHostWithStandIns(rememberNavController(), InfiniteBackdropFrames) }
        val keys = rule.backdropSemantics().map { it.key }.toSet()
        assertThat(keys)
            .containsExactly(SemanticsProperties.TestTag, BackdropDensityKey, BackdropLayersKey, BackdropMotionKey)
    }
}

private const val SEMANTICS_ROBOLECTRIC_SDK = 34
