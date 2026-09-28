package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.home.fixture.BOARDS_PILL
import com.app.pegsolitaire.ui.home.fixture.CLEARED_BOARDS
import com.app.pegsolitaire.ui.home.fixture.EARNED_STARS
import com.app.pegsolitaire.ui.home.fixture.EVERY_CHIP_STATE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.PERSISTED_LEVEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.bothResumesState
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home's stage (ux-spec §4.2.4, D-03l): the slack beneath the hero row holds a clay board showing a
 * game in progress, sized by the slack itself and not drawn at all when there is no room — criteria
 * 15p (no empty middle gap) and 15r (the stage is decoration).
 *
 * The founder's screenshot measured ≈172dp of **nothing** between the ribbon and the Continue chip
 * on a 6.7-inch phone: D-03i had parked three quarters of the slack there as an empty `Spacer`. The
 * stage takes that share instead and grows into it, so the largest blank Home can show is just
 * under the smallest board it will draw plus the two gaps around it.
 *
 * ## How it is measured
 *
 * With the device's system-bar insets supplied (§13 forbids a zero-inset reading of a layout
 * property), under NATIVE graphics — LEGACY measures every character one dp wide, so every text
 * band would come out at its unwrapped floor and every slack figure would be wrong by exactly the
 * wrap it ignored. Distances are taken **along the column**, from each node's unclipped position,
 * so a band below the fold is measured where it is rather than where `getBoundsInRoot()` clips it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeStageTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Criterion 15p, part 1: the founder's phone ----------------------------

    /**
     * On the 6.7-inch baseline (843dp usable), with no Continue chip and with only the Classic one
     * — the founder's own state — the stage draws a board of at least 120dp, and no two
     * consecutive visible elements are more than 24dp apart: every gap on the screen is a band gap,
     * and the hole is gone.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun onTheTallPhoneTheStageHoldsABoardOfAtLeast120dpAndNoGapExceeds24dp() {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state)

        listOf(false, true).forEach { classic ->
            show(state, chipsState(classic = classic, challenge = false))
            val situation = if (classic) "one Classic chip" else "no chip"

            assertWithMessage("$situation: the stage art's edge")
                .that(widthOf(stageArt()))
                .isAtLeast(TALL_PHONE_MIN_ART_DP.dp)
            assertWithMessage("$situation: the widest gap").that(widestGap()).isAtMost(TALL_PHONE_MAX_GAP_DP.dp)
        }
    }

    // ---- Criterion 15p, part 2: no window leaves a hole ------------------------

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun noBlankOver64dpOnA360x640dpPhoneInAnyChipState() = assertNoBlankExceeds(COMPACT_MAX_BLANK_DP)

    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun noBlankOver64dpOnA360x720dpPhoneInAnyChipState() = assertNoBlankExceeds(COMPACT_MAX_BLANK_DP)

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun noBlankOver64dpOnA360x800dpPhoneInAnyChipState() = assertNoBlankExceeds(COMPACT_MAX_BLANK_DP)

    /** 780 − 72 = 708dp usable, under the 720dp threshold: this window takes the compact set. */
    @Test
    @Config(qualifiers = "w412dp-h780dp")
    fun noBlankOver64dpOnA412x780dpPhoneInAnyChipState() = assertNoBlankExceeds(COMPACT_MAX_BLANK_DP)

    @Test
    @Config(qualifiers = "w412dp-h850dp")
    fun noBlankOver80dpOnA412x850dpPhoneInAnyChipState() = assertNoBlankExceeds(FULL_MAX_BLANK_DP)

    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun noBlankOver80dpOnA412x915dpPhoneInAnyChipState() = assertNoBlankExceeds(FULL_MAX_BLANK_DP)

    // ---- Criterion 15r: the stage is decoration --------------------------------

    /**
     * The stage and everything in it are neither clickable nor focusable and announce nothing: on
     * every node of its subtree the semantics are a test tag — which TalkBack does not read — and,
     * on the art, the slab's `Shape`, which `Modifier.background(color, shape)` records as a focus
     * outline hint and which is neither spoken nor a reason to take focus. Nothing else: no action,
     * no role, no label, no state. Asserted on a window where the stage art *is* drawn, so the
     * subtree is the whole of it.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theStageAndItsArtCarryNothingButTestTags() {
        renderWithInsets(mutableStateOf(chipsState(classic = false, challenge = false)))
        stageArt().assertExistsAndGet()

        val stage = composeRule.onNodeWithTag(HOME_STAGE_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode()
        (listOf(stage) + stage.descendants()).forEach { node ->
            assertWithMessage("semantics of ${node.config.getOrNull(SemanticsProperties.TestTag)}")
                .that(node.config.map { it.key } - SemanticsProperties.Shape)
                .containsExactly(SemanticsProperties.TestTag)
        }
    }

    /**
     * Home's TalkBack traversal is unchanged by the stage: title, gear, the three pills, both
     * Continue chips, both tiles — and no stop in between, because the stage offers none.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun homesTalkBackTraversalRunsTitleGearPillsChipsTilesAndNothingElse() {
        renderWithInsets(mutableStateOf(bothResumesState()))
        stageArt().assertExistsAndGet()

        assertThat(spokenInTraversalOrder())
            .containsExactly(
                appName,
                settingsLabel,
                composeRule.pillDescriptions(STARS_PILL).single(),
                composeRule.pillDescriptions(BOARDS_PILL).single(),
                composeRule.pillDescriptions(LEVEL_PILL).single(),
                RESUME_CLASSIC_LABEL,
                RESUME_CHALLENGE_LABEL,
                classicTileTitle,
                challengeTileTitle,
            ).inOrder()
    }

    /**
     * The stage renders identically before and after `HomeUiState` receives its first progress
     * emission: it reads no state, so the ribbon filling in beside it moves nothing.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theStageRendersIdenticallyBeforeAndAfterTheFirstProgressEmission() {
        val state = mutableStateOf(HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))
        renderWithInsets(state)
        val beforeTheRead = boundsOf(stageArt())

        show(state, state.value.copy(totalStars = EARNED_STARS, classicBoardsCleared = CLEARED_BOARDS))

        assertThat(boundsOf(stageArt())).isEqualTo(beforeTheRead)
    }

    // ---- Harness ---------------------------------------------------------------

    private fun renderWithInsets(state: MutableState<HomeUiState>) {
        composeRule.renderHome(state)
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
    }

    private fun show(
        holder: MutableState<HomeUiState>,
        state: HomeUiState,
    ) {
        composeRule.runOnUiThread { holder.value = state }
        composeRule.waitForIdle()
    }

    /**
     * Criterion 15p part 2 on the current window: in every chip state, no distance between two
     * consecutive visible elements above the second mode tile exceeds [maxBlankDp].
     */
    private fun assertNoBlankExceeds(maxBlankDp: Float) {
        val state = mutableStateOf(EVERY_CHIP_STATE.first())
        renderWithInsets(state)
        EVERY_CHIP_STATE.forEach { chips ->
            show(state, chips)
            assertWithMessage("the widest blank with ${chipsNamed(chips)}").that(widestGap()).isAtMost(maxBlankDp.dp)
        }
    }

    private fun chipsNamed(state: HomeUiState): String =
        listOfNotNull(
            "Classic".takeIf { state.classicResumeAvailable },
            "Challenge".takeIf { state.challengeResumeAvailable },
        ).joinToString(" and ").ifEmpty { "no chip" } + " saved"

    /**
     * The widest distance between two consecutive visible elements, top to bottom: the hero row,
     * the stage art if drawn, the ribbon, each Continue chip, the first mode tile and the second.
     * The footer's slack below the second tile is not a gap between two elements and is not read.
     */
    private fun widestGap(): Dp {
        val elements =
            buildList {
                add(boundsOf(composeRule.onNodeWithTag(HOME_HERO_TEST_TAG)))
                if (exists(HOME_STAGE_ART_TEST_TAG)) add(boundsOf(stageArt()))
                add(boundsOf(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)))
                listOf(RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL).forEach { chip ->
                    if (composeRule.onAllNodesWithText(chip).fetchSemanticsNodes().isNotEmpty()) {
                        add(boundsOf(composeRule.onNodeWithText(chip)))
                    }
                }
                add(boundsOf(composeRule.onNodeWithText(classicTileTitle)))
                add(boundsOf(composeRule.onNodeWithText(challengeTileTitle)))
            }.sortedBy { it.top.value }
        return elements.zipWithNext { upper, lower -> lower.top - upper.bottom }.maxOf { it }
    }

    private fun stageArt(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(HOME_STAGE_ART_TEST_TAG, useUnmergedTree = true)

    private fun exists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun SemanticsNodeInteraction.assertExistsAndGet(): SemanticsNode = fetchSemanticsNode()

    /**
     * A node's bounds **along the column**: its unclipped position and size, so a band below the
     * fold reports where it is, not the viewport edge `getBoundsInRoot()` would clip it to.
     */
    private fun boundsOf(node: SemanticsNodeInteraction): DpRect {
        val semantics = node.fetchSemanticsNode()
        val origin = semantics.positionInRoot
        return with(composeRule.density) {
            DpRect(
                left = origin.x.toDp(),
                top = origin.y.toDp(),
                right = (origin.x + semantics.size.width).toDp(),
                bottom = (origin.y + semantics.size.height).toDp(),
            )
        }
    }

    private fun widthOf(node: SemanticsNodeInteraction): Dp {
        val bounds = boundsOf(node)
        return bounds.right - bounds.left
    }

    /**
     * What TalkBack would say, stop by stop: a walk of the merged tree in order, taking each
     * node's first label and not descending past a node that speaks — its children were merged
     * into it.
     */
    private fun spokenInTraversalOrder(): List<String> = composeRule.onRoot().fetchSemanticsNode().spoken()

    private fun SemanticsNode.spoken(): List<String> {
        val label =
            config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        return if (label != null) listOf(label) else children.flatMap { it.spoken() }
    }

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }
}

/** The 6.7-inch reference phone (criteria 15g, 15l, 15p). */
private const val TALL_PHONE_QUALIFIER = "w412dp-h915dp"

/** The design baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24f
private const val NAVIGATION_BAR_DP = 48f

/** Criterion 15p part 1: the stage art's floor on the founder's phone, and the widest gap there. */
private const val TALL_PHONE_MIN_ART_DP = 120
private const val TALL_PHONE_MAX_GAP_DP = 24

/**
 * Criterion 15p part 2: the largest blank the stage can leave — just under `home_stage_art_min_size`
 * (48dp) plus the two band gaps around it, `spacing_md` in the full set and `spacing_sm` in the
 * compact one.
 */
private const val FULL_MAX_BLANK_DP = 80f
private const val COMPACT_MAX_BLANK_DP = 64f
