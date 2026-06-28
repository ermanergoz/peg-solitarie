package com.app.pegsolitaire.ui.game

import app.cash.turbine.test
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Turbine coverage of every documented `GameViewModel` behavior (D-02..D-06,
 * GAME-02/04/05) as `uiState` emissions, plus the T-03-01 security control that
 * the VM applies ONLY moves the `RulesEngine` deems legal.
 *
 * Custom tiny [BoardSpec]s are used for the terminal-state cases so a Won/Stuck
 * line is reached in a single jump without scripting a full English-33 solution.
 */
class GameViewModelTest {
    // A peg above the empty center; its only legal jump lands in the center hole.
    private val pegAboveCenter = Position(row = 1, col = 3)
    private val centerHole = Position(row = 3, col = 3)
    private val pegLeftOfCenter = Position(row = 3, col = 1)

    private fun englishViewModel(): GameViewModel =
        GameViewModel(rulesEngine = RulesEngine, initialBoard = Boards.english33Initial)

    @Test
    fun selectingPegHighlightsItsLegalDestinations() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                val initial = awaitItem()
                assertThat(initial.selected).isNull()
                assertThat(initial.legalTargets).isEmpty()

                viewModel.onCellTap(pegAboveCenter)

                val selected = awaitItem()
                assertThat(selected.selected).isEqualTo(pegAboveCenter)
                assertThat(selected.legalTargets).containsExactly(centerHole)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reTappingTheSelectedPegDeselectsIt() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()

                viewModel.onCellTap(pegAboveCenter)

                val deselected = awaitItem()
                assertThat(deselected.selected).isNull()
                assertThat(deselected.legalTargets).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tappingADifferentPegWhileSelectedIsANoOp() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                val selected = awaitItem()

                // A DIFFERENT peg while one is already selected must not switch selection.
                viewModel.onCellTap(pegLeftOfCenter)

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tappingALegalDestinationPerformsTheJumpAndAnimates() =
        runTest {
            val viewModel = englishViewModel()
            val pegsBefore = Boards.english33Initial.pegCount
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()

                viewModel.onCellTap(centerHole)

                val jumped = awaitItem()
                assertThat(jumped.board.pegCount).isEqualTo(pegsBefore - 1)
                assertThat(jumped.selected).isNull()
                assertThat(jumped.legalTargets).isEmpty()
                assertThat(jumped.status).isEqualTo(GameStatus.Playing)
                val transition = jumped.animating
                assertThat(transition).isNotNull()
                requireNotNull(transition)
                assertThat(transition.from).isEqualTo(pegAboveCenter)
                assertThat(transition.to).isEqualTo(centerHole)
                assertThat(transition.removed).isEqualTo(Position(row = 2, col = 3))
                assertThat(transition.id).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun animationFinishedClearsTheTransition() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                assertThat(awaitItem().animating).isNotNull()

                viewModel.onAnimationFinished()

                assertThat(awaitItem().animating).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun tappingABlockedCellWhileSelectedIsANoOp() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                val selected = awaitItem()

                // A 2x2 corner coordinate is outside the English-33 footprint.
                viewModel.onCellTap(Position(row = 0, col = 0))

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aDestinationNotInLegalMovesIsNeverApplied() =
        runTest {
            // A 1x5 strip: pegs at the two left cells, the rest empty. Selecting
            // the leftmost peg yields exactly one legal target (col 2); tapping a
            // farther empty hole (col 4) is a forged, illegal destination.
            val strip = Position(row = 0, col = 0)
            val spec =
                BoardSpec(
                    inPlay =
                        listOf(
                            Position(row = 0, col = 0),
                            Position(row = 0, col = 1),
                            Position(row = 0, col = 2),
                            Position(row = 0, col = 3),
                            Position(row = 0, col = 4),
                        ),
                    target = null,
                )
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
            val viewModel = GameViewModel(rulesEngine = RulesEngine, initialBoard = board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(strip)
                val selected = awaitItem()
                assertThat(selected.legalTargets).containsExactly(Position(row = 0, col = 2))

                // Forge: an empty hole that is NOT a legal destination.
                viewModel.onCellTap(Position(row = 0, col = 4))

                expectNoEvents()
                assertThat(viewModel.uiState.value).isEqualTo(selected)
                assertThat(viewModel.uiState.value.board.pegCount).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun clearingTheBoardToOnePegReportsWon() =
        runTest {
            // 1x3 strip: pegs at cols 0,1, hole at col 2 → one jump leaves a lone peg.
            val spec =
                BoardSpec(
                    inPlay =
                        listOf(
                            Position(row = 0, col = 0),
                            Position(row = 0, col = 1),
                            Position(row = 0, col = 2),
                        ),
                    target = Position(row = 0, col = 2),
                )
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
            val viewModel = GameViewModel(rulesEngine = RulesEngine, initialBoard = board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))

                val won = awaitItem()
                assertThat(won.status).isEqualTo(GameStatus.Won)
                assertThat(won.board.pegCount).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun reachingADeadEndWithMultiplePegsReportsStuck() =
        runTest {
            // Strip cols 0,1,2 plus an isolated cell (2,0). After the only jump,
            // the lone peg at (2,0) survives with no neighbors → Stuck (2 pegs).
            val spec =
                BoardSpec(
                    inPlay =
                        listOf(
                            Position(row = 0, col = 0),
                            Position(row = 0, col = 1),
                            Position(row = 0, col = 2),
                            Position(row = 2, col = 0),
                        ),
                    target = null,
                )
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
                    .withPeg(Position(row = 2, col = 0))
            val viewModel = GameViewModel(rulesEngine = RulesEngine, initialBoard = board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))

                val stuck = awaitItem()
                assertThat(stuck.status).isEqualTo(GameStatus.Stuck)
                assertThat(stuck.board.pegCount).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun midGameRestartAsksForConfirmationThenResets() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                val afterJump = awaitItem()
                viewModel.onAnimationFinished()
                awaitItem()

                viewModel.onRestart()

                val confirming = awaitItem()
                assertThat(confirming.confirmRestart).isTrue()
                // Board is NOT reset yet — still the post-jump board.
                assertThat(confirming.board).isEqualTo(afterJump.board)

                viewModel.onConfirmRestart()

                val reset = awaitItem()
                assertThat(reset.confirmRestart).isFalse()
                assertThat(reset.board).isEqualTo(Boards.english33Initial)
                assertThat(reset.selected).isNull()
                assertThat(reset.status).isEqualTo(GameStatus.Playing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissingTheRestartConfirmationKeepsTheGame() =
        runTest {
            val viewModel = englishViewModel()
            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(pegAboveCenter)
                awaitItem()
                viewModel.onCellTap(centerHole)
                val afterJump = awaitItem()
                viewModel.onAnimationFinished()
                awaitItem()
                viewModel.onRestart()
                awaitItem()

                viewModel.onDismissRestart()

                val kept = awaitItem()
                assertThat(kept.confirmRestart).isFalse()
                assertThat(kept.board).isEqualTo(afterJump.board)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun restartFromAnEndStateResetsWithoutConfirmation() =
        runTest {
            val spec =
                BoardSpec(
                    inPlay =
                        listOf(
                            Position(row = 0, col = 0),
                            Position(row = 0, col = 1),
                            Position(row = 0, col = 2),
                        ),
                    target = Position(row = 0, col = 2),
                )
            val board =
                Board(spec = spec, pegs = 0L)
                    .withPeg(Position(row = 0, col = 0))
                    .withPeg(Position(row = 0, col = 1))
            val viewModel = GameViewModel(rulesEngine = RulesEngine, initialBoard = board)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 0))
                awaitItem()
                viewModel.onCellTap(Position(row = 0, col = 2))
                assertThat(awaitItem().status).isEqualTo(GameStatus.Won)

                viewModel.onRestart()

                val reset = awaitItem()
                assertThat(reset.confirmRestart).isFalse()
                assertThat(reset.status).isEqualTo(GameStatus.Playing)
                assertThat(reset.board).isEqualTo(board)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
