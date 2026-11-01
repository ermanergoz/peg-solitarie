package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The rescue banner's words and its put-away on the real [GameScreen] (ux-spec D-SB-3/D-SB-4,
 * §4.4.3; docs/gdd.md SB-U2, SB-U3, SB-U5; criteria 30h, 30i, 30j's screen half). The GDD names two
 * of these tests under a `GameScreenTest` that does not exist; the ux-spec's §13 puts them here.
 *
 * The screen picks the message by mode first: a Challenge level takes the two plurals, resolved
 * with the board's peg count and the clear rule's threshold; Classic keeps its two strings. Every
 * string is found by its resource id, resolved here the way the screen resolves it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESCUE_BANNER_ROBOLECTRIC_SDK])
class GameScreenRescueBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources get() = RuntimeEnvironment.getApplication().resources

    private val cellTaps = mutableListOf<Position>()

    @Test
    fun aStuckChallengeBoardShowsTheChallengeStuckMessageWithItsPegCount() {
        show(stuckOnFivePegs(), challengeLevel = CHALLENGE_LEVEL)

        val expected = stuckChallengeMessage(pegs = FIVE_PEGS)
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(expected).assertIsDisplayed()
        assertThat(expected).contains(FIVE_PEGS.toString())
        listOf(R.string.undo_action, R.string.rescue_restart, R.string.action_home).forEach { action ->
            composeRule
                .onNode(hasText(resources.getString(action)) and hasAnyAncestor(hasTestTag(NOWIN_BANNER_TEST_TAG)))
                .assertIsDisplayed()
        }
        composeRule.onNodeWithText(resources.getString(R.string.stuck_message)).assertDoesNotExist()
    }

    @Test
    fun aStuckClassicBoardKeepsTheClassicStuckMessage() {
        show(stuckOnFivePegs(), challengeLevel = null)

        composeRule.onNodeWithText(resources.getString(R.string.stuck_message)).assertIsDisplayed()
        composeRule.onNodeWithText(stuckChallengeMessage(pegs = FIVE_PEGS)).assertDoesNotExist()
    }

    @Test
    fun aProvenDeadChallengeBoardSaysOnePegIsOutOfReachNotThatTheLevelIsLost() {
        show(provenDead(), challengeLevel = CHALLENGE_LEVEL)

        val expected = provenDeadChallengeMessage()
        composeRule.onNodeWithText(expected).assertIsDisplayed()
        FORBIDDEN_WORDS.forEach { word ->
            assertWithMessage("\"%s\" in the banner", word).that(expected.contains(word, ignoreCase = true)).isFalse()
        }
        composeRule.onNodeWithText(resources.getString(R.string.nowin_message)).assertDoesNotExist()
    }

    @Test
    fun aProvenDeadClassicBoardKeepsTheClassicMessage() {
        show(provenDead(), challengeLevel = null)

        composeRule.onNodeWithText(resources.getString(R.string.nowin_message)).assertIsDisplayed()
        composeRule.onNodeWithText(provenDeadChallengeMessage()).assertDoesNotExist()
    }

    @Test
    fun aTapOnTheChallengeProvenDeadBannerPutsItAwayAndTheBoardBeneathThenPlays() {
        // SB-U5 / 30j: the screen hands the put-away to the ViewModel's entry point, which clears
        // the proof flag; here the hoisted state does what the ViewModel does.
        val state = mutableStateOf(provenDead())
        showOver(state, challengeLevel = CHALLENGE_LEVEL) { state.value = state.value.copy(noWinningMove = false) }

        composeRule.onNodeWithText(provenDeadChallengeMessage()).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertDoesNotExist()
        assertThat(cellTaps).isEmpty()
        tapCell(BOTTOM_ROW_CELL)
        assertThat(cellTaps).containsExactly(BOTTOM_ROW_CELL)
    }

    @Test
    fun aTapOnAStuckChallengeBannersMessageLeavesItUp() {
        var dismissals = 0
        val state = mutableStateOf(stuckOnFivePegs())
        showOver(state, challengeLevel = CHALLENGE_LEVEL) { dismissals++ }

        composeRule.onNodeWithText(stuckChallengeMessage(pegs = FIVE_PEGS)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()
        assertThat(dismissals).isEqualTo(0)
    }

    private fun show(
        state: GameUiState,
        challengeLevel: Int?,
    ) = showOver(mutableStateOf(state), challengeLevel) {}

    private fun showOver(
        state: MutableState<GameUiState>,
        challengeLevel: Int?,
        onDismissProvenDead: () -> Unit,
    ) {
        composeRule.setContent {
            AppTheme {
                GameScreen(
                    state = state.value,
                    onCellTap = { cellTaps += it },
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
                    onDismissProvenDead = onDismissProvenDead,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun tapCell(cell: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (cell.col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID,
                    y = (cell.row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    private fun stuckChallengeMessage(pegs: Int): String =
        resources.getQuantityString(R.plurals.stuck_message_challenge, pegs, pegs, ChallengeClearRule.CLEAR_MAX_PEGS)

    private fun provenDeadChallengeMessage(): String =
        resources.getQuantityString(
            R.plurals.nowin_message_challenge,
            ChallengeClearRule.CLEAR_MAX_PEGS,
            ChallengeClearRule.CLEAR_MAX_PEGS,
        )

    /** Five isolated pegs on the English-33 footprint, no move: a stuck Challenge board (R6). */
    private fun stuckOnFivePegs(): GameUiState =
        GameUiState
            .initial(
                listOf(Position(2, 0), Position(4, 0), Position(2, 6), Position(4, 6), Position(6, 2))
                    .fold(Board(spec = Boards.English33, pegs = 0L)) { board, position -> board.withPeg(position) },
            ).copy(status = GameStatus.Stuck, stuck = true)

    /** The full English-33 start with a Hint's proof up: still playing, one peg out of reach. */
    private fun provenDead(): GameUiState = GameUiState.initial().copy(noWinningMove = true)

    private companion object {
        const val CHALLENGE_LEVEL = 9
        const val FIVE_PEGS = 5

        /** The English-33 lattice is seven cells a side. */
        const val ENGLISH_GRID = 7f

        /** A cell of the board's bottom row, where the banner sits until it is put away. */
        val BOTTOM_ROW_CELL = Position(row = 6, col = 3)

        /** GDD §16, ux-spec §4.4.3: a proven-dead Challenge board is never a loss. */
        val FORBIDDEN_WORDS = listOf("lost", "failed", "game over", "no winning move", "try again")
    }
}

private const val RESCUE_BANNER_ROBOLECTRIC_SDK = 34
