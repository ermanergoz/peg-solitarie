package com.app.pegsolitaire.ui.theme

import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.testing.SystemMotionScale
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A composition whose context carries no `MotionDurationScale` (a preview, or a test that provides
 * none) runs its animations at their own speed, so the gate is on there too: the gate agrees with
 * the clock, and the app no longer reads `Settings.Global` itself (tech-lead-signoff.md §3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [GATE_ROBOLECTRIC_SDK])
class AnimationsEnabledGateWithoutScaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withNoScaleInItsContextTheGateIsOnLikeTheClockWhateverTheSettingSays() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            SystemMotionScale.OFF,
        )
        val seen = mutableListOf<Boolean>()
        composeRule.setContent { seen += rememberAnimationsEnabled() }
        composeRule.waitForIdle()

        assertThat(seen).containsExactly(true)
    }
}
