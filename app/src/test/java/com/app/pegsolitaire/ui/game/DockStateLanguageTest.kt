package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The dock's state language on the real screen (ux-spec §4.4.1, D-04e): every control is drawn
 * in the look its state requires, and only in that look.
 *
 * [DockLookRecipeTest] proves the five looks cannot be confused without colour; this class proves
 * the screen *reaches* the right one — that `GameScreen` hands each control the state that decides
 * its look, and that the control reports the look it draws. The look is read from [DockLookKey],
 * the custom-semantics idiom `GameScreen`'s `PegCountKey` uses, so every row below is decided on
 * each run rather than by a screenshot.
 *
 * Each control is found by its name **and** the look key inside the dock, so a control that stops
 * reporting a look fails here rather than being skipped, and a same-named node elsewhere (the rescue
 * banner's text Undo) can never stand in for it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DOCK_ROBOLECTRIC_SDK])
class DockStateLanguageTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Criterion 21a — table-driven over §4.4.1's "which control is in which look" rows that a
     * `GameUiState` can express. The two spent-allowance rows are not here because the state
     * cannot express them — by design, the counters never reach the UI — and criterion 21c proves
     * them against the real ViewModel instead ([DockSpentAllowanceTest]).
     *
     * One composition, one row at a time: the state is swapped between rows rather than recomposed
     * from scratch, which is also how a real game moves between them.
     */
    @Test
    fun everyControlIsInTheLookItsStateRequires() {
        val state = mutableStateOf(GameUiState.initial())
        composeRule.setContent { AppTheme { GameScreenFor(state.value) } }

        LOOK_ROWS.forEach { row ->
            composeRule.runOnUiThread { state.value = row.state }
            composeRule.waitForIdle()

            assertWithMessage(row.situation).that(lookOf(row.control)).isEqualTo(row.look)
        }
    }

    // ---- Helpers -------------------------------------------------------------

    /** The look the dock control named [control] reports — asserting there is exactly one. */
    private fun lookOf(
        @StringRes control: Int,
    ): DockLook? =
        composeRule
            .onNode(
                hasContentDescription(string(control)) and
                    SemanticsMatcher.keyIsDefined(DockLookKey) and
                    hasAnyAncestor(hasTestTag(GAME_ACTION_DOCK_TEST_TAG)),
            ).fetchSemanticsNode()
            .config
            .getOrNull(DockLookKey)

    private fun string(
        @StringRes id: Int,
    ): String = RuntimeEnvironment.getApplication().getString(id)
}

/** The real screen over [state], every callback inert: these tests read looks, not taps. */
@Composable
private fun GameScreenFor(state: GameUiState) {
    GameScreen(
        state = state,
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
    )
}

/**
 * One row of §4.4.1's table: a situation, the state that expresses it, the control's name and
 * the look the row requires.
 */
private data class LookRow(
    val situation: String,
    val state: GameUiState,
    @param:StringRes val control: Int,
    val look: DockLook,
)

private val FRESH_BOARD = GameUiState.initial()

/** Any in-play cell of the English-33 start will do as a shown hint's source and target. */
private val HINT_SOURCE = Position(row = 3, col = 1)
private val HINT_TARGET = Position(row = 3, col = 3)

private val LOOK_ROWS =
    listOf(
        LookRow("Undo after one jump", FRESH_BOARD.copy(canUndo = true), R.string.undo_action, DockLook.Available),
        LookRow("Undo on a fresh board", FRESH_BOARD, R.string.undo_action, DockLook.Unavailable),
        LookRow(
            "Undo while a Challenge board loads",
            GameUiState.loading(),
            R.string.undo_action,
            DockLook.Unavailable,
        ),
        LookRow("Hint at rest", FRESH_BOARD, R.string.hint_action, DockLook.Available),
        LookRow(
            "Hint while a hint is showing on the board",
            FRESH_BOARD.copy(hintSource = HINT_SOURCE, hintTarget = HINT_TARGET),
            R.string.hint_action,
            DockLook.Available,
        ),
        LookRow(
            "Hint after the last search found no winning move",
            FRESH_BOARD.copy(noWinningMove = true),
            R.string.hint_action,
            DockLook.Available,
        ),
        LookRow("Hint while a Challenge board loads", GameUiState.loading(), R.string.hint_action, DockLook.Available),
        LookRow(
            "Hint while the Solver searches",
            FRESH_BOARD.copy(solverThinking = true),
            R.string.hint_thinking,
            DockLook.Working,
        ),
        LookRow("Show moves with the dots on", FRESH_BOARD.copy(showHints = true), R.string.show_moves_on, DockLook.On),
        LookRow(
            "Show moves with the dots off",
            FRESH_BOARD.copy(showHints = false),
            R.string.show_moves_on,
            DockLook.Off,
        ),
        LookRow("Restart on a fresh board", FRESH_BOARD, R.string.restart_board, DockLook.Available),
        LookRow("Restart mid-game", FRESH_BOARD.copy(canUndo = true), R.string.restart_board, DockLook.Available),
    )

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val DOCK_ROBOLECTRIC_SDK = 34
