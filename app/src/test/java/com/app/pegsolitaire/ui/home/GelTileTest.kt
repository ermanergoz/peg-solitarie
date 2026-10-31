package com.app.pegsolitaire.ui.home

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.testing.wcagLuminance
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BD-27's layout half and the gel tiles' own contract (backdrop spec §3.5.2, B-18, the
 * tech-lead's A4): each mode tile is deep gel beneath its top band, its white text starts at or below
 * `gel_top_band` at 1.0 and 2.0 font scale, pressing darkens it, and it announces exactly what the
 * clay tile did.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [GEL_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GelTileTest {
    @get:Rule
    val rule = createComposeRule()

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val classic = resources.getString(R.string.classic_tile_title)
    private val challenge = resources.getString(R.string.challenge_tile_title)

    @Test
    fun eachTileIsDeepGelBeneathItsTopBand() {
        rule.renderHome(state = HomeUiState())
        val window = rule.renderedWindow()
        listOf(classic, challenge).forEach { title ->
            val tile = tileBounds(title)
            val inset = resources.getDimension(R.dimen.gel_lip_inset)
            val x = (tile.left + inset / 2f).toInt()
            val y = (tile.bottom - inset).toInt()
            val luminance = Color(window.getPixel(x, y)).wcagLuminance()
            assertWithMessage("$title near its bottom edge").that(luminance).isLessThan(DEEP_GEL_MAX_L)
        }
    }

    @Test
    fun theTilesTextStartsAtOrBelowTheGelsTopBandAtDefaultFontScale() = assertTextBand(DEFAULT_SCALE)

    @Test
    fun theTilesTextStartsAtOrBelowTheGelsTopBandAtDoubleFontScale() = assertTextBand(DOUBLE_SCALE)

    @Test
    fun pressingAGelTileDarkensIt() {
        rule.renderHome(state = HomeUiState())
        val tile = tileBounds(classic)
        val x = (tile.left + tile.width / 2f).toInt()
        val y = (tile.bottom - resources.getDimension(R.dimen.gel_lip_inset)).toInt()
        val resting = Color(rule.renderedWindow().getPixel(x, y)).wcagLuminance()
        rule.onNode(hasText(classic) and hasClickAction()).performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(PRESS_SETTLE_MS)
        val pressed = Color(rule.renderedWindow().getPixel(x, y)).wcagLuminance()
        assertWithMessage("pressed against resting").that(pressed).isLessThan(resting)
    }

    @Test
    fun theGelTilesAnnounceExactlyWhatTheClayTilesDid() {
        rule.renderHome(state = HomeUiState())
        val classicText = tileTexts(classic)
        assertThat(classicText)
            .containsExactly(classic, resources.getString(R.string.classic_tile_supporting))
            .inOrder()
        val level = resources.getString(R.string.challenge_tile_continue, HomeUiState().challengeCurrentLevel)
        assertThat(tileTexts(challenge))
            .containsExactly(challenge, level)
            .inOrder()
    }

    private fun assertTextBand(fontScale: Float) {
        rule.renderHome(state = HomeUiState(), fontScale = fontScale)
        val band = resources.getDimension(R.dimen.gel_top_band)
        listOf(classic, challenge).forEach { title ->
            val tile = tileBounds(title)
            val text = rule.onNode(hasText(title), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertWithMessage("$title at font scale $fontScale").that(text.top - tile.top).isAtLeast(band)
        }
    }

    private fun tileBounds(title: String) =
        rule.onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot

    private fun tileTexts(title: String): List<String> =
        rule
            .onNode(hasText(title) and hasClickAction())
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .map { it.text }

    private companion object {
        /** Deep gel: `GelVioletBottom` is L 0.067 and `GelPinkBottom` L 0.124; the clay tiles are near 0.8. */
        const val DEEP_GEL_MAX_L = 0.25
        const val DEFAULT_SCALE = 1f
        const val DOUBLE_SCALE = 2f
        const val PRESS_SETTLE_MS = 300L
    }
}

private const val GEL_ROBOLECTRIC_SDK = 34
