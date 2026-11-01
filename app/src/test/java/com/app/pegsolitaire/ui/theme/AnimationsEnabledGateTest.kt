package com.app.pegsolitaire.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.testing.SystemMotionScale
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reduced-motion gate, [rememberAnimationsEnabled], reads the scale the composition's own
 * animations run on (tech-lead-signoff.md §3): so it changes when "Remove animations" changes with
 * the app resident, in the same composition, both ways.
 *
 * The scale is the composition's `MotionDurationScale`, turned live by [SystemMotionScale].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [GATE_ROBOLECTRIC_SDK])
class AnimationsEnabledGateTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    @Test
    fun theGateTurnsOffAndBackOnWithTheSystemScaleWhileTheScreenStaysComposed() {
        val seen = mutableListOf<Boolean>()
        composeRule.setContent { seen += rememberAnimationsEnabled() }
        composeRule.waitForIdle()
        assertThat(seen.last()).isTrue()

        motion.turnAnimationsOff()
        composeRule.waitForIdle()
        assertThat(seen.last()).isFalse()

        motion.turnAnimationsOn()
        composeRule.waitForIdle()
        assertThat(seen.last()).isTrue()
    }

    @Test
    fun aScreenComposedWithTheSystemScaleAtZeroHasTheGateOffFromItsFirstComposition() {
        motion.turnAnimationsOff()
        val seen = mutableListOf<Boolean>()
        composeRule.setContent { seen += rememberAnimationsEnabled() }
        composeRule.waitForIdle()

        assertThat(seen).containsExactly(false)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
internal const val GATE_ROBOLECTRIC_SDK = 34
