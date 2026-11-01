package com.app.pegsolitaire.ui.game

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.composeHost
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Where the gameplay chrome **is**, after D-04/D-05 move it (ux-spec §4.3, §4.4).
 *
 * The strongest guarantees this refresh owes are elsewhere and deliberately so: that the
 * controls still *work* is asserted by `UndoControlTest`/`HintControlTest`/`GameFlowTest`, and
 * that their content descriptions survived the move is proven by `ButtonSoundTest.kt:226-244`
 * and `GameFlowTest.kt:129` passing **byte-unchanged** — if either had needed an edit, a
 * description had drifted and the fix would belong in the production code.
 *
 * What is left for this file is the thing those cannot see: the chrome's new *shape*. Each
 * assertion is structural — which surface a control is inside — rather than pixel-based, and
 * each is reached through the two test tags rather than through copy (§13).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CHROME_ROBOLECTRIC_SDK])
class GameScreenChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    /**
     * Criterion 16, the half a unit test can reach: every piece of chrome the deleted
     * `TopAppBar` carried is now inside the strip this screen owns, and the four actions are
     * not in it at all — so there is nothing left for a Material bar to hold.
     *
     * The type-level half — that no `TopAppBar`, `BottomAppBar` or `NavigationBar` is
     * instantiated anywhere in `ui/game` — is §10's P8 grep, which is a build-verifiable gate
     * rather than a runtime one; the visual half (no bar background, no bar elevation) is a
     * device check.
     */
    @Test
    fun theGameplayScreenHasNoMaterialTopAppBar() {
        renderGame()

        assertThat(isInside(GAME_STATUS_STRIP_TEST_TAG, description(R.string.navigate_back))).isTrue()
        assertThat(isInside(GAME_STATUS_STRIP_TEST_TAG, TIMER_AT_THE_START)).isTrue()
        DOCK_ACTIONS.forEach { action ->
            assertThat(isInside(GAME_STATUS_STRIP_TEST_TAG, description(action))).isFalse()
        }
    }

    /** Criteria 17 and 18: back, title and timer, all three, and all three in the strip. */
    @Test
    fun theBackControlTitleAndTimerAreAllInTheStatusStrip() {
        renderGame(challengeLevel = CHALLENGE_LEVEL)

        composeRule.onNodeWithTag(GAME_STATUS_STRIP_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(description(R.string.navigate_back)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(TIMER_AT_THE_START).assertIsDisplayed()

        // The context title is the Challenge indicator verbatim, carried across unchanged.
        val title = app.getString(R.string.challenge_level_indicator_desc, CHALLENGE_LEVEL)
        assertThat(isInside(GAME_STATUS_STRIP_TEST_TAG, title)).isTrue()
    }

    /**
     * Criterion 26b's gameplay order, the half Compose decides (tech-lead defect ruling §3, B-3):
     * the timer is a stop of its own in Compose's reading order, after the title and before Undo.
     *
     * Compose writes its traversal hints only between the nodes it counts as screen-reader
     * focusable, and a node that does not merge its descendants counts only as a leaf with
     * something to say. The clock's node kept its five hidden cells as semantics children, so it
     * was no leaf: Compose left it out, the title's hint went straight to Undo, and TalkBack, which
     * still focuses the clock by its description, placed that unhinted node first of all.
     *
     * Read from the view's accessibility node provider, which is what TalkBack is handed, with a
     * screen reader enabled, because Compose computes its order only while one is. NATIVE
     * graphics, because Compose picks the nodes it hands a service by intersecting
     * `android.graphics.Region`s, which LEGACY stubs: there the provider hands out the root alone.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun theTimerIsAStopInComposesReadingOrderBetweenTheTitleAndUndo() {
        enableAScreenReader()
        renderGame()

        val provider = checkNotNull(composeRule.composeHost().accessibilityNodeProvider) { "no node provider" }
        val title =
            composeRule
                .onNode(hasText(description(R.string.app_name)), useUnmergedTree = true)
                .fetchSemanticsNode()
                .id
        val clock = clockNode().id
        val undo = composeRule.onNodeWithContentDescription(description(R.string.undo_action)).fetchSemanticsNode().id
        val stops = mapOf(title to TITLE_STOP, clock to TIMER_STOP, undo to UNDO_STOP)

        val clockInfo = checkNotNull(provider.createAccessibilityNodeInfo(clock)) { "no node for the clock" }
        assertWithMessage("whether the clock's node is screen-reader focusable")
            .that(clockInfo.isScreenReaderFocusable)
            .isTrue()
        assertWithMessage("the stop Compose reads after the title")
            .that(stopAfter(provider, title, stops))
            .isEqualTo(TIMER_STOP)
        assertWithMessage("the stop Compose reads after the timer")
            .that(stopAfter(provider, clock, stops))
            .isEqualTo(UNDO_STOP)
    }

    /** Criterion 19: all four actions still reachable, and all four in the one dock. */
    @Test
    fun allFourActionsLiveInTheActionDock() {
        renderGame()

        composeRule.onNodeWithTag(GAME_ACTION_DOCK_TEST_TAG).assertIsDisplayed()
        DOCK_ACTIONS.forEach { action ->
            assertThat(isInside(GAME_ACTION_DOCK_TEST_TAG, description(action))).isTrue()
        }
    }

    /**
     * Criterion 21d, and criterion 21 as D-04e restates it. Retargeted — never deleted — from
     * `showMovesCarriesItsStateByFillAndNotByTintAlone` (ux-spec §13's founder-feedback table):
     * that test read a filled/outlined flag and a pair of descriptions swapped with the state, and
     * both were the defect. The fill it trusted measured ≈1.04:1 against the slab, and the pair was
     * two imperatives ("Show…"/"Hide possible moves") each naming the *opposite* of what the next
     * tap does.
     *
     * What it proves now, on the one node TalkBack reaches: the toggle is a **switch** whose state
     * is announced by the role, under **one constant name** in both states; and it is drawn On or
     * Off — the strong disc with the on mark, or the empty ring with the slashed glyph — which is
     * [DockLookKey]'s look resolved through the recipe every dock control draws from. The name is
     * the lookup in both states, so a description that drifted back to "Hide" fails to be found.
     */
    @Test
    fun showMovesCarriesItsStateByFillRingMarkAndSwitchStateNeverByTint() {
        renderGame(showHints = true)
        val on = showMovesToggle()
        assertThat(on.config[DockLookKey]).isEqualTo(DockLook.On)
        assertThat(on.config[DockLookKey].recipe.fill).isEqualTo(DockFill.Strong)
        assertThat(on.config[DockLookKey].recipe.onMark).isTrue()
        assertThat(on.config[SemanticsProperties.Role]).isEqualTo(Role.Switch)
        assertThat(on.config[SemanticsProperties.ToggleableState]).isEqualTo(ToggleableState.On)
        assertThat(on.config[SemanticsProperties.ContentDescription])
            .containsExactly(description(R.string.show_moves_on))

        setShowHints(false)
        val off = showMovesToggle()
        assertThat(off.config[DockLookKey]).isEqualTo(DockLook.Off)
        assertThat(off.config[DockLookKey].recipe.ring).isTrue()
        assertThat(off.config[DockLookKey].recipe.glyph).isEqualTo(DockGlyph.Slashed)
        assertThat(off.config[SemanticsProperties.Role]).isEqualTo(Role.Switch)
        assertThat(off.config[SemanticsProperties.ToggleableState]).isEqualTo(ToggleableState.Off)
        assertThat(off.config[SemanticsProperties.ContentDescription])
            .containsExactly(description(R.string.show_moves_on))
    }

    /**
     * Criterion 24: the negative contract inherited verbatim from the ad-allowance ruling the
     * code records at `GameUiState.kt:131-135` — no badge, no "×3" chip, no ad glyph, no
     * allowance-driven disable. This refresh does not relax it.
     *
     * Asserted as "the dock renders no text at all", which is the strongest available form: a
     * count, a badge or an "×3" would have to be text, and the four actions are icon-only with
     * their labels living in content descriptions.
     */
    @Test
    fun noControlShowsAnAllowanceCountBadgeOrAdGlyph() {
        renderGame()

        val dockTexts =
            composeRule
                .onNodeWithTag(GAME_ACTION_DOCK_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNode()
                .textsIn()

        assertThat(dockTexts).isEmpty()
    }

    // ---- Helpers -------------------------------------------------------------

    private fun description(resId: Int): String = app.getString(resId)

    /** Whether the node carrying [contentDescription] sits inside the subtree tagged [tag]. */
    private fun isInside(
        tag: String,
        contentDescription: String,
    ): Boolean =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .descriptionsIn()
            .contains(contentDescription)

    /**
     * The Show-moves toggle's own node: the one dock node carrying both its constant name and a
     * look. Found by the name in **both** states, which is half of what criterion 21d asserts.
     */
    private fun showMovesToggle(): SemanticsNode =
        composeRule
            .onNode(
                hasContentDescription(description(R.string.show_moves_on)) and
                    SemanticsMatcher.keyIsDefined(DockLookKey),
            ).fetchSemanticsNode()

    private fun SemanticsNode.descriptionsIn(): List<String> =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            children.flatMap { it.descriptionsIn() }

    private fun SemanticsNode.textsIn(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            children.flatMap { it.textsIn() }

    /** The clock's own node: the node inside the timer pill whose description starts with its label. */
    private fun clockNode(): SemanticsNode =
        composeRule
            .onNode(
                hasAnyAncestor(hasTestTag(GAME_TIMER_PILL_TEST_TAG)) and
                    hasContentDescription(description(R.string.elapsed_time), substring = true),
                useUnmergedTree = true,
            ).fetchSemanticsNode()

    /**
     * Turns a screen reader on for the next composition, as TalkBack does: accessibility enabled,
     * with touch exploration and one enabled service. Compose computes its reading order only
     * while an enabled service is listed, so a test that reads the order must do this first.
     */
    private fun enableAScreenReader() {
        shadowOf(app.getSystemService(AccessibilityManager::class.java)).apply {
            setEnabled(true)
            setTouchExplorationEnabled(true)
            setEnabledAccessibilityServiceList(listOf(AccessibilityServiceInfo()))
        }
    }

    /**
     * The stop Compose reads after the node [id], named by [stops]: the node its traversal hint
     * says it is read before, as an accessibility service is handed it. Any other node is named by
     * its id, and a node with no hint reads [NO_STOP].
     */
    private fun stopAfter(
        provider: AccessibilityNodeProvider,
        id: Int,
        stops: Map<Int, String>,
    ): String {
        val info = checkNotNull(provider.createAccessibilityNodeInfo(id)) { "no node for $id" }
        provider.addExtraDataToAccessibilityNodeInfo(id, info, TRAVERSAL_BEFORE_EXTRA, null)
        val next = info.extras.getInt(TRAVERSAL_BEFORE_EXTRA, NO_NODE_ID)
        return if (next == NO_NODE_ID) NO_STOP else stops[next] ?: "node $next"
    }

    /**
     * The toggle's ON/OFF state, hoisted **outside** the composition and written only from the
     * test thread.
     *
     * `setContent` may be called once per test, so the one test that needs both states drives
     * them through this slot rather than through a second composition. It is deliberately *not*
     * written from inside [GameChrome]: a state write during composition is a backwards write
     * whose effect depends on which lambda's restart scope happens to own the read, which is
     * exactly the kind of accident a chrome test must not be built on.
     */
    private val showHints = mutableStateOf(true)

    private fun renderGame(
        showHints: Boolean = true,
        challengeLevel: Int? = null,
    ) {
        this.showHints.value = showHints
        composeRule.setContent { GameChrome(challengeLevel = challengeLevel) }
        composeRule.waitForIdle()
    }

    /** Flips the already-composed toggle, so one test can read both of its states. */
    private fun setShowHints(on: Boolean) {
        composeRule.runOnUiThread { showHints.value = on }
        composeRule.waitForIdle()
    }

    @Composable
    private fun GameChrome(challengeLevel: Int?) {
        AppTheme {
            GameScreen(
                state = GameUiState.initial().copy(showHints = showHints.value, canUndo = true),
                onCellTap = {},
                onRestart = {},
                onConfirmRestart = {},
                onDismissRestart = {},
                onAnimationFinished = {},
                onToggleHints = {},
                onUndo = {},
                onHint = {},
                onAcceptAdOffer = {},
                onDismissAdOffer = {},
                onHome = {},
                onNavigateBack = {},
                challengeLevel = challengeLevel,
            )
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val CHROME_ROBOLECTRIC_SDK = 34

/** A Challenge level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
private const val CHALLENGE_LEVEL = 7

/**
 * The extra under which Compose hands a test the id of the node a node is read before: its
 * traversal hint, which a sealed node info does not otherwise expose.
 */
private const val TRAVERSAL_BEFORE_EXTRA = "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL"

/** What the extra reads when Compose wrote no hint for the node. */
private const val NO_NODE_ID = -1

/** The three stops criterion 26b's gameplay line puts in a row, and a node with no hint. */
private const val TITLE_STOP = "the title"
private const val TIMER_STOP = "the timer"
private const val UNDO_STOP = "Undo"
private const val NO_STOP = "no stop"

/**
 * The timer's whole description on a game's first second, word for word under D-05e's whole-minute
 * fallback (tech-lead defect ruling §2, B-2): the `elapsed_time` label, then the time in whole
 * minutes, which is "under a minute" at the start. The label alone is no longer the description.
 */
private const val TIMER_AT_THE_START = "Elapsed time, under a minute"

/**
 * The four dock actions, by the content description each keeps from before the move (§4.4:
 * "every content description is the existing string — no new copy"). Show-moves is listed in
 * its ON form because that is the state the default render is in.
 */
private val DOCK_ACTIONS =
    listOf(
        R.string.undo_action,
        R.string.hint_action,
        R.string.show_moves_on,
        R.string.restart_board,
    )
