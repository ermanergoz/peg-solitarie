package com.app.pegsolitaire.ui.theme

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * BD-03's and BD-18's aid to the auditor, in the `DockLookRecipeTest` idiom: the finish each
 * container takes is exactly backdrop spec §3.5.1's table, written out here row by row.
 *
 * The Home stage art's row left the table with the stage (ux-spec D-03p; the backdrop spec's §8.1
 * pointer, item 2; the tech-lead's ruling 4). The board Home draws in a slab now is the Classic Continue
 * chip's thumbnail, and its slab is **not** a finished container: a plain `surfaceContainerHigh`
 * square with no rim, lip, gloss or glow, which BD-17's sample of its blocked corner proves plain
 * (`BoardInteriorIdentityTest`).
 */
class FinishRecipeTest {
    @Test
    fun theGlossLiesOnTheChipsAndPillsAndNeverOnTheSlabOrTheDock() {
        val expected =
            mapOf(
                FinishedContainer.BoardSlab to FinishRecipe(rim = true, lip = true, gloss = false, glow = true),
                FinishedContainer.DockSlab to FinishRecipe(rim = true, lip = true, gloss = false, glow = true),
                FinishedContainer.BackChip to FinishRecipe(rim = true, lip = true, gloss = true, glow = true),
                FinishedContainer.TimerPill to FinishRecipe(rim = true, lip = true, gloss = true, glow = true),
                FinishedContainer.GearChip to FinishRecipe(rim = true, lip = true, gloss = true, glow = true),
                FinishedContainer.RibbonPill to FinishRecipe(rim = true, lip = true, gloss = true, glow = true),
                FinishedContainer.ContinueChip to FinishRecipe(rim = true, lip = true, gloss = true, glow = true),
                FinishedContainer.RescueBanner to FinishRecipe(rim = true, lip = true, gloss = false, glow = false),
            )
        assertThat(FinishedContainer.entries.toSet()).isEqualTo(expected.keys)
        expected.forEach { (container, recipe) ->
            assertWithMessage("$container").that(container.recipe).isEqualTo(recipe)
        }
    }

    @Test
    fun everyFinishedContainerCarriesARimAndALip() {
        FinishedContainer.entries.forEach { container ->
            assertWithMessage("$container rim").that(container.recipe.rim).isTrue()
            assertWithMessage("$container lip").that(container.recipe.lip).isTrue()
        }
    }
}
