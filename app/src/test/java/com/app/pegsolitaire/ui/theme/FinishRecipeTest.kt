package com.app.pegsolitaire.ui.theme

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * BD-03's and BD-18's aid to the auditor, in the `DockLookRecipeTest` idiom: the finish each
 * container takes is exactly backdrop spec §3.5.1's table, written out here row by row.
 */
class FinishRecipeTest {
    @Test
    fun theGlossLiesOnTheChipsAndPillsAndNeverOnTheSlabTheStageArtOrTheDock() {
        val expected =
            mapOf(
                FinishedContainer.BoardSlab to FinishRecipe(rim = true, lip = true, gloss = false, glow = true),
                FinishedContainer.StageArt to FinishRecipe(rim = true, lip = true, gloss = false, glow = true),
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
