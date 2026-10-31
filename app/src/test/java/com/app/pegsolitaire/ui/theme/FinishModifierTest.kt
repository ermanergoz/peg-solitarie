package com.app.pegsolitaire.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.renderedWindow
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The gloss finish as drawn (candy-backdrop.md §3.5.1, the tech-lead's A3): every finished container
 * draws a rim on its edge and a lip inside its top; a glossed chip is lighter at its top than its
 * fill; a slab's fill away from its edge is exactly its fill, with no gloss over the board.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FINISH_ROBOLECTRIC_SDK], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinishModifierTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aGlossedChipIsLighterAtItsTopAndEveryFinishedContainerDrawsItsRimOnItsEdge() {
        var container by mutableStateOf(FinishedContainer.RibbonPill)
        var dark by mutableStateOf(false)
        rule.setContent {
            AppTheme(darkTheme = dark) {
                Box(Modifier.size(SIZE.dp).glossFinish(container, RoundedCornerShape(CORNER.dp), fill = FILL))
            }
        }
        listOf(false, true).forEach { theme ->
            FinishedContainer.entries.forEach { c ->
                dark = theme
                container = c
                rule.waitForIdle()
                val window = rule.renderedWindow()
                val where = "$c, dark = $theme"
                val edge = window.getPixel(0, SIZE / 2)
                assertWithMessage("$where: the rim is drawn on the edge").that(edge).isNotEqualTo(FILL.toArgb())
                val upper = window.getPixel(SIZE / 2, UPPER_PX)
                val lower = window.getPixel(SIZE / 2, SIZE - LOWER_PX)
                assertWithMessage("$where: the fill beneath the gloss").that(lower).isEqualTo(FILL.toArgb())
                if (c.recipe.gloss) {
                    assertWithMessage("$where: the gloss lightens the top").that(upper).isNotEqualTo(FILL.toArgb())
                } else {
                    assertWithMessage("$where: no gloss on a slab").that(upper).isEqualTo(FILL.toArgb())
                }
            }
        }
    }

    private companion object {
        const val SIZE = 100
        const val CORNER = 16
        const val UPPER_PX = 8
        const val LOWER_PX = 10
        val FILL = Color(0xFF808080)
    }
}

private const val FINISH_ROBOLECTRIC_SDK = 34
