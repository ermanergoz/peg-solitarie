package com.app.pegsolitaire.ui.window

import android.os.Looper
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The production bar seam on a real activity window (docs/tech-design.md §3.8 (a); docs/ux-spec.md
 * D-05g part 2): [SystemBarsState.Hidden] asks for both bars to go and to come back only
 * transiently, on a swipe, and [SystemBarsState.Shown] asks for both back with the window's default
 * behaviour.
 *
 * Two reads, each the way the rest of the process would see it: the behaviour a fresh
 * `WindowInsetsControllerCompat` over the same window reports, and the visibility of each bar type in
 * the window's root insets once the main looper has run. The platform controller has no public
 * read-back of the visibility it was asked for, so the root insets are the answer; Robolectric gives
 * the window no bar heights, so what the bars look like, and that they really slide away, are the
 * device's to prove (criteria 30g–30n). The recording fake stands in for this object everywhere else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WINDOW_BARS_ROBOLECTRIC_SDK])
class WindowInsetsSystemBarsTest {
    private val window: Window =
        Robolectric
            .buildActivity(ComponentActivity::class.java)
            .setup()
            .visible()
            .get()
            .window

    @Test
    fun hiddenLetsTheBarsBackOnlyTransientlyOnASwipe() {
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)

        assertWithMessage("the window's bar behaviour after Hidden")
            .that(behaviour())
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
    }

    @Test
    fun shownAfterHiddenGivesTheWindowItsDefaultBehaviourBack() {
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)
        WindowInsetsSystemBars.apply(window, SystemBarsState.Shown)

        assertWithMessage("the window's bar behaviour after Hidden, then Shown")
            .that(behaviour())
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
    }

    @Test
    fun askingForTheSameStateTwiceChangesNothing() {
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)

        assertWithMessage("the window's bar behaviour after Hidden twice")
            .that(behaviour())
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
    }

    @Test
    fun hiddenAsksForBothBarsToGo() {
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)
        shadowOf(Looper.getMainLooper()).idle()

        assertWithMessage("whether the status bar is visible after Hidden").that(visible(statusBars())).isFalse()
        assertWithMessage("whether the navigation bar is visible after Hidden")
            .that(visible(navigationBars()))
            .isFalse()
    }

    @Test
    fun shownAfterHiddenAsksForBothBarsBack() {
        WindowInsetsSystemBars.apply(window, SystemBarsState.Hidden)
        shadowOf(Looper.getMainLooper()).idle()
        WindowInsetsSystemBars.apply(window, SystemBarsState.Shown)
        shadowOf(Looper.getMainLooper()).idle()

        assertWithMessage("whether the status bar is visible after Hidden, then Shown")
            .that(visible(statusBars()))
            .isTrue()
        assertWithMessage("whether the navigation bar is visible after Hidden, then Shown")
            .that(visible(navigationBars()))
            .isTrue()
    }

    private fun behaviour(): Int = WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior

    /**
     * Whether the window's root insets report bars of [type] as visible: the platform controller's
     * answer to the request, which Robolectric's window passes back to the decor view.
     */
    private fun visible(type: Int): Boolean =
        checkNotNull(ViewCompat.getRootWindowInsets(window.decorView)) { "the decor view has no root insets" }
            .isVisible(type)
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val WINDOW_BARS_ROBOLECTRIC_SDK = 34
