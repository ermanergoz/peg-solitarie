package com.app.pegsolitaire.ui.results

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The rays' centre on a scrolled column (ux-spec §4.7, "the centre follows the row"; code-auditor
 * finding 1 on results-confetti-rays): the star row reports its centre through
 * [reportCentreInRoot] inside the Results column's `verticalScroll`, which clips. The reported
 * centre must be the row's own, unclipped, so it moves one for one with the scroll offset — while
 * the row is part-clipped at the viewport's top edge and after it has scrolled out entirely —
 * rather than shrinking to the visible slice or collapsing to the root's corner.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CENTRE_SCROLL_ROBOLECTRIC_SDK])
class ResultsRaysCentreScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var reported = Offset.Unspecified
    private lateinit var scroll: ScrollState

    @Test
    fun theReportedCentreIsTheRowsOwnWhileTheColumnHasNotScrolled() {
        setColumn()
        assertCentreAfterScrolling(0.dp)
    }

    @Test
    fun theReportedCentreFollowsTheScrollWhileTheRowIsPartlyClippedAtTheTop() {
        setColumn()
        assertCentreAfterScrolling(PART_CLIPPED_SCROLL)
    }

    @Test
    fun theReportedCentreFollowsTheScrollAfterTheRowHasScrolledOutOfView() {
        setColumn()
        assertCentreAfterScrolling(SCROLLED_OUT_SCROLL)
    }

    private fun setColumn() {
        composeRule.setContent {
            scroll = rememberScrollState()
            Box(modifier = Modifier.size(VIEWPORT)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Spacer(modifier = Modifier.height(ROW_TOP))
                    Box(modifier = Modifier.size(ROW_SIZE).reportCentreInRoot { reported = it })
                    Spacer(modifier = Modifier.height(TAIL))
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertCentreAfterScrolling(by: Dp) {
        val density = composeRule.density
        val byPx = with(density) { by.toPx() }
        composeRule.runOnIdle { scroll.dispatchRawDelta(byPx) }
        composeRule.waitForIdle()
        val half = with(density) { (ROW_SIZE / 2).toPx() }
        val expected = Offset(half, with(density) { ROW_TOP.toPx() } + half - byPx)
        composeRule.runOnIdle {
            assertWithMessage("scrolled by $by: the scroll moved as asked")
                .that(scroll.value.toFloat())
                .isWithin(TOLERANCE_PX)
                .of(byPx)
            assertWithMessage("scrolled by $by: centre x").that(reported.x).isWithin(TOLERANCE_PX).of(expected.x)
            assertWithMessage("scrolled by $by: centre y moves one for one with the scroll")
                .that(reported.y)
                .isWithin(TOLERANCE_PX)
                .of(expected.y)
        }
    }

    private companion object {
        /** The clipping viewport, the Results column's visible area in miniature. */
        val VIEWPORT = 100.dp

        /** Space above the row inside the scrolling content. */
        val ROW_TOP = 50.dp

        /** The row's side. */
        val ROW_SIZE = 40.dp

        /** Space below the row, so the column can scroll the row fully out. */
        val TAIL = 300.dp

        /** The row spans −10 … 30 dp of the viewport: its top tenth is clipped. */
        val PART_CLIPPED_SCROLL = 60.dp

        /** The row spans −100 … −60 dp: entirely above the viewport. */
        val SCROLLED_OUT_SCROLL = 150.dp

        /** Sub-pixel rounding of layout positions. */
        const val TOLERANCE_PX = 1f
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val CENTRE_SCROLL_ROBOLECTRIC_SDK = 34
