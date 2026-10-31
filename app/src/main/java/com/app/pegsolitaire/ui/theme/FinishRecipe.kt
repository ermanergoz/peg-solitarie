package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Immutable

/**
 * Every container on Home and the game screen that takes a part of the gloss finish
 * (docs/ux/candy-backdrop.md §3.5.1, B-19). The dock's discs, every dialog and snackbar, and every
 * container on a Sparse screen are not here: they keep today's finish.
 */
enum class FinishedContainer {
    BoardSlab,
    StageArt,
    DockSlab,
    BackChip,
    TimerPill,
    GearChip,
    RibbonPill,
    ContinueChip,
    RescueBanner,
}

/** Which parts of the finish a container takes: its rim, lip, white gloss, and the glow tint on its shadow. */
@Immutable
data class FinishRecipe(
    val rim: Boolean,
    val lip: Boolean,
    val gloss: Boolean,
    val glow: Boolean,
)

/**
 * The finish §3.5.1's table gives this container. No gloss lies over the board slab, the stage art
 * (both hold the board), the dock slab (21e and 21f measure the discs against its flat fill) or the
 * rescue banner, whose shadow also stays the clay one.
 */
val FinishedContainer.recipe: FinishRecipe
    get() =
        when (this) {
            FinishedContainer.BoardSlab, FinishedContainer.StageArt, FinishedContainer.DockSlab -> HERO_SLAB
            FinishedContainer.BackChip,
            FinishedContainer.TimerPill,
            FinishedContainer.GearChip,
            FinishedContainer.RibbonPill,
            FinishedContainer.ContinueChip,
            -> GLOSSED_CHIP
            FinishedContainer.RescueBanner -> RIMMED_BANNER
        }

private val HERO_SLAB = FinishRecipe(rim = false, lip = false, gloss = false, glow = false)
private val GLOSSED_CHIP = FinishRecipe(rim = false, lip = false, gloss = false, glow = false)
private val RIMMED_BANNER = FinishRecipe(rim = false, lip = false, gloss = false, glow = false)
