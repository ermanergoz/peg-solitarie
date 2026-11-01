package com.app.pegsolitaire.ui.game

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The dock's state language as **data** (ux-spec §4.4.1, D-04e): five looks, one recipe each.
 *
 * The founder could not tell on from off because one channel — fill against outline — was
 * carrying two meanings at once, importance and state. The fix is a vocabulary in which every two
 * looks differ in something other than hue, and this class holds that vocabulary to its contract
 * over the recipe itself rather than over a screenshot: the composables draw from [DockLook.recipe]
 * and nothing else, so a recipe that passes here is a dock that reads without colour.
 *
 * A pure JVM test — no composition, no Robolectric — because the property is about the mapping,
 * not about any one frame of it.
 */
class DockLookRecipeTest {
    /**
     * Criterion 21b: each of the ten pairs of looks differs in at least two of fill, ring, shadow,
     * glyph shape and on mark. Hue is not a channel and glyph strength is not counted — the
     * spec's pair table names both as extras, never as one of the two.
     *
     * The mutations this catches are the ones that would bring the founder's complaint back: a
     * Working disc that keeps its shadow differs from Available by its spinner alone; an Off
     * control that loses its ring differs from Unavailable by its slash alone; an On disc drawn
     * pale differs from Available by its mark alone. Each of those is one channel, and one
     * channel is exactly what failed on the device.
     */
    @Test
    fun everyPairOfLooksDiffersInAtLeastTwoChannelsThatAreNotHue() {
        val pairs = DockLook.entries.pairs()

        assertThat(pairs).hasSize(TEN_PAIRS)
        pairs.forEach { (first, second) ->
            assertWithMessage("$first against $second")
                .that(channelsThatDiffer(first.recipe, second.recipe))
                .isAtLeast(MIN_DIFFERING_CHANNELS)
        }
    }

    /**
     * The one pair the founder named, stated on its own because it is the pair the whole
     * amendment exists for: On and Off must differ in every channel the dock has.
     */
    @Test
    fun onAndOffDifferInEveryChannelTheDockHas() {
        assertThat(channelsThatDiffer(DockLook.On.recipe, DockLook.Off.recipe)).isEqualTo(ALL_CHANNELS)
    }

    private fun channelsThatDiffer(
        first: DockLookRecipe,
        second: DockLookRecipe,
    ): Int =
        listOf(
            first.fill != second.fill,
            first.ring != second.ring,
            first.shadow != second.shadow,
            first.glyph != second.glyph,
            first.onMark != second.onMark,
        ).count { it }

    private fun List<DockLook>.pairs(): List<Pair<DockLook, DockLook>> =
        flatMapIndexed { index, first -> drop(index + 1).map { second -> first to second } }
}

/** Five looks taken two at a time. */
private const val TEN_PAIRS = 10

/** Criterion 21b's floor: two channels that are not hue. */
private const val MIN_DIFFERING_CHANNELS = 2

/** Fill, ring, shadow, glyph shape and on mark — every channel criterion 21b counts. */
private const val ALL_CHANNELS = 5
