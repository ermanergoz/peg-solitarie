package com.app.pegsolitaire.ui.window

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

/**
 * The stable-inset source below API 30 (docs/tech-design.md §3.8 (d), as built): the window's root
 * insets, read whenever the window's insets are dispatched again.
 *
 * Below API 30 androidx emulates the bars' slide for the Compose host, and the emulation hands
 * Compose's own "ignoring visibility" values the slide's interpolated insets, which carry no stable
 * value: on an API 27 device they fell from 63/126 px to 0/0 as the bars hid, and the bottom stayed 0
 * after they came back. The
 * window's root insets are never part of that emulation and kept 63/126 px throughout. So below 30
 * [RootStableSystemInsets] reads the root's stable bar insets and cutout, and reads them again each
 * time the host's dispatched insets change, which is every hide, show and slide.
 *
 * Robolectric runs only API 34 here, so the root read is injected and the platform's own behaviour
 * below 30 is the device's to prove (criterion 30r); what is tested is that this source is the root's
 * stable bars and cutout, and that it follows them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROOT_STABLE_ROBOLECTRIC_SDK])
class RootStableSystemInsetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun itReadsTheRootsStableBarsWhateverTheBarsShowNow() {
        val density = composeRule.density
        val source = RootStableSystemInsets(rootInsets = { rootWithHiddenBars(density) }, dispatched = NoInsets)

        assertWithMessage("the top, in px").that(source.getTop(density)).isEqualTo(px(density, STATUS_DP))
        assertWithMessage("the bottom, in px").that(source.getBottom(density)).isEqualTo(px(density, NAVIGATION_DP))
        assertWithMessage("the left, in px").that(source.getLeft(density, LayoutDirection.Ltr)).isEqualTo(0)
        assertWithMessage("the right, in px").that(source.getRight(density, LayoutDirection.Ltr)).isEqualTo(0)
    }

    @Test
    fun aCutoutDeeperThanTheBarWins() {
        val density = composeRule.density
        val source =
            RootStableSystemInsets(
                rootInsets = { rootWithHiddenBars(density, cutoutTop = CUTOUT_DP, cutoutLeft = CUTOUT_DP) },
                dispatched = NoInsets,
            )

        assertWithMessage("the top under a deeper cutout, in px")
            .that(source.getTop(density))
            .isEqualTo(px(density, CUTOUT_DP))
        assertWithMessage("the left beside a cutout, in px")
            .that(source.getLeft(density, LayoutDirection.Ltr))
            .isEqualTo(px(density, CUTOUT_DP))
    }

    @Test
    fun withNoRootInsetsYetItReadsNone() {
        val density = composeRule.density
        val source = RootStableSystemInsets(rootInsets = { null }, dispatched = NoInsets)

        assertWithMessage("the top with no root insets").that(source.getTop(density)).isEqualTo(0)
        assertWithMessage("the bottom with no root insets").that(source.getBottom(density)).isEqualTo(0)
    }

    /**
     * A padding laid out against the source follows the root when the host's insets are dispatched
     * again: the read happens in layout, and the dispatch is what invalidates it. The root here is a
     * plain holder, not snapshot state, as a window's root insets are, so nothing but the dispatch can
     * make the layout read it again.
     */
    @Test
    fun aLayoutPaddedOffItFollowsTheRootWhenInsetsAreDispatchedAgain() {
        val root = AtomicReference<(Density) -> WindowInsetsCompat?> { rootWithHiddenBars(it) }
        composeRule.setContent {
            val density = LocalDensity.current
            val dispatched = WindowInsets.systemBars
            val source = remember(dispatched) { RootStableSystemInsets({ root.get()(density) }, dispatched) }
            Box(Modifier.fillMaxSize().windowInsetsPadding(source)) {
                Box(Modifier.size(1.dp).testTag(PADDED))
            }
        }
        composeRule.applySystemBarInsets(top = STATUS_DP, bottom = NAVIGATION_DP)
        assertWithMessage("the padded node's top, in dp, against a 24dp root").that(top()).isEqualTo(STATUS_DP)

        root.set { rootWithHiddenBars(it, statusDp = TALL_STATUS_DP) }
        composeRule.applySystemBarInsets(top = STATUS_DP, bottom = NAVIGATION_DP, visible = false)

        assertWithMessage("the padded node's top, in dp, after a dispatch against a 54dp root")
            .that(top())
            .isEqualTo(TALL_STATUS_DP)
    }

    private fun top(): Dp = composeRule.onNodeWithTag(PADDED).getBoundsInRoot().top

    /** Root insets as a device below API 30 reports them with both bars hidden: stable values, nothing visible. */
    private fun rootWithHiddenBars(
        density: Density,
        statusDp: Dp = STATUS_DP,
        cutoutTop: Dp = 0.dp,
        cutoutLeft: Dp = 0.dp,
    ): WindowInsetsCompat =
        WindowInsetsCompat
            .Builder()
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.statusBars(),
                Insets.of(0, px(density, statusDp), 0, 0),
            ).setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, px(density, NAVIGATION_DP)),
            ).setVisible(WindowInsetsCompat.Type.systemBars(), false)
            .setInsets(
                WindowInsetsCompat.Type.displayCutout(),
                Insets.of(px(density, cutoutLeft), px(density, cutoutTop), 0, 0),
            ).build()

    private fun px(
        density: Density,
        dp: Dp,
    ): Int = with(density) { dp.roundToPx() }
}

/** A dispatched-insets stand-in for the reads that need no layout to invalidate. */
private object NoInsets : WindowInsets {
    override fun getLeft(
        density: Density,
        layoutDirection: LayoutDirection,
    ) = 0

    override fun getTop(density: Density) = 0

    override fun getRight(
        density: Density,
        layoutDirection: LayoutDirection,
    ) = 0

    override fun getBottom(density: Density) = 0
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val ROOT_STABLE_ROBOLECTRIC_SDK = 34

private const val PADDED = "padded"
private val STATUS_DP = 24.dp
private val TALL_STATUS_DP = 54.dp
private val NAVIGATION_DP = 48.dp
private val CUTOUT_DP = 34.dp
