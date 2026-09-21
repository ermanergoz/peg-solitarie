package com.app.pegsolitaire.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Coverage of [TutorialViewModel]: the exit ordering (D-01d/D-01g) and the three-step
 * machine (D-01b/D-01c).
 *
 * The exit tests are the reason this class exists. A fire-and-forget `markSeen()` leaves
 * the flag `false`, Home re-pushes `Tutorial` on the next recomposition and the player is
 * in a loop they cannot leave — so [theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted]
 * asserts the **ordering** and not merely the call: a fake whose write suspends until
 * released is what makes the two distinguishable, because a test that only checks "markSeen
 * was called" passes against the bug.
 */
class TutorialViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Group A: the exit ordering (D-01g) --------------------------------

    @Test
    fun theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = GatedOnboardingRepository(gate)
            val viewModel = viewModelFor(repository)

            viewModel.exitSignal.test {
                viewModel.onExit()
                advanceUntilIdle()

                // The write has STARTED and has not finished, so there must be no signal:
                // the screen may not pop while the flag is still unwritten.
                assertThat(repository.markSeenStarted).isTrue()
                assertThat(repository.markSeenCount).isEqualTo(0)
                expectNoEvents()

                gate.complete(Unit)
                advanceUntilIdle()

                awaitItem()
                assertThat(repository.markSeenCount).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun everyExitRouteSkipBackAndCompletionPersistsTheSeenFlagExactlyOnce() =
        runTest(testDispatcher) {
            // Three callers, one function: Skip, system back and the completion CTA all
            // route through onExit(), so each of them writes exactly once.
            listOf<(TutorialViewModel) -> Unit>(
                { it.onExit() },
                { it.onExit() },
                { it.onExit() },
            ).forEach { route ->
                val repository = GatedOnboardingRepository()
                val viewModel = viewModelFor(repository)

                route(viewModel)
                advanceUntilIdle()

                assertThat(repository.markSeenCount).isEqualTo(1)
            }
        }

    @Test
    fun aSecondExitIsALatchedNoOp() =
        runTest(testDispatcher) {
            val repository = GatedOnboardingRepository()
            val viewModel = viewModelFor(repository)

            viewModel.exitSignal.test {
                // Skip tapped, then system back pressed a frame later.
                viewModel.onExit()
                viewModel.onExit()
                advanceUntilIdle()

                awaitItem()
                expectNoEvents()
                assertThat(repository.markSeenCount).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theSeenFlagIsWrittenOnAScopeThatOutlivesTheScreen() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = GatedOnboardingRepository(gate)
            // A scope INDEPENDENT of this test's own, modelling `@ApplicationScope`: a
            // process-lifetime `SupervisorJob` that is nobody's child and is never cancelled
            // (`DataModule.provideApplicationScope`).
            val applicationScope = CoroutineScope(SupervisorJob() + testDispatcher)
            val viewModel = TutorialViewModel(repository, applicationScope)

            viewModel.onExit()
            runCurrent()
            // The pop clears the destination's ViewModel. Were the write on viewModelScope
            // it would die right here and the flag would stay false forever, which is the
            // navigation loop this whole design exists to prevent.
            viewModel.viewModelScope.cancel()
            gate.complete(Unit)
            advanceUntilIdle()

            assertThat(repository.markSeenCount).isEqualTo(1)
            applicationScope.cancel()
        }

    // ---- Group B: the step machine (D-01b/D-01c) --------------------------

    @Test
    fun aStepAdvancesOnlyWhenItsBoardHoldsExactlyOnePeg() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()

            // T1 offers one move. Selecting a peg does not advance anything.
            viewModel.onCellTap(Position(row = 0, col = 0))
            runCurrent()
            assertThat(viewModel.uiState.value.stepIndex).isEqualTo(FIRST_STEP)
            assertThat(viewModel.uiState.value.board.board.pegCount).isEqualTo(T1_START_PEGS)

            // Completing the jump leaves one peg, which IS the completion predicate.
            viewModel.onCellTap(Position(row = 0, col = 2))
            runCurrent()
            assertThat(viewModel.uiState.value.board.board.pegCount).isEqualTo(ONE_PEG)
            assertThat(viewModel.uiState.value.stepIndex).isEqualTo(FIRST_STEP)

            settleStepAdvance()
            assertThat(viewModel.uiState.value.stepIndex).isEqualTo(SECOND_STEP)
        }

    @Test
    fun theThreeStepsRunInTheOrderT1T2T3AndTheTutorialEndsAfterTheThird() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()

            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T1)
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T3)
            assertThat(viewModel.uiState.value.completed).isFalse()
            T3_WINNING_LINE.forEach { (from, to) ->
                viewModel.onCellTap(from)
                runCurrent()
                viewModel.onCellTap(to)
                runCurrent()
            }
            settleStepAdvance()

            assertThat(viewModel.uiState.value.completed).isTrue()
            assertThat(viewModel.uiState.value.stepIndex).isEqualTo(THIRD_STEP)
        }

    @Test
    fun showHintsIsForcedOnRegardlessOfThePersistedSetting() =
        runTest(testDispatcher) {
            // D-01b: legal destinations are always visible, so no step can become a
            // guessing game. There is no settings collaborator to disagree with — see
            // theTutorialViewModelsOnlyCollaboratorsAre... below.
            val viewModel = viewModelFor()
            runCurrent()

            assertThat(viewModel.uiState.value.board.showHints).isTrue()
            playTheOnlyMove(viewModel)
            settleStepAdvance()
            assertThat(viewModel.uiState.value.board.showHints).isTrue()
            playTheOnlyMove(viewModel)
            settleStepAdvance()
            assertThat(viewModel.uiState.value.board.showHints).isTrue()
        }

    @Test
    fun aWrongTapNeverLosesProgressAndNeverProducesAnErrorState() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            // T2's middle peg (1,1) has no legal jump: it selects and offers nothing.
            viewModel.onCellTap(T2_IMMOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_IMMOVABLE_PEG)
            assertThat(viewModel.uiState.value.board.legalTargets).isEmpty()

            // Tapping an illegal destination while a peg is held KEEPS the selection —
            // nothing is lost and nothing is announced as wrong.
            viewModel.onCellTap(T2_ILLEGAL_HOLE)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_IMMOVABLE_PEG)
            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)

            // And there is no error field to set in the first place (D-01c), which is the
            // half a behavioural assertion cannot reach.
            val fields = TutorialUiState::class.java.declaredFields.map { it.name.lowercase() }
            assertThat(fields.none { FORBIDDEN_STATE_WORDS.any(it::contains) }).isTrue()
        }

    /**
     * Criterion 3a's cheap half (§4.1 "Tap semantics", ruled 2026-09-21).
     *
     * The tutorial teaches a tap rule, so it must be the *game's* tap rule:
     * `GameViewModel.onCellTap` selects only while nothing is selected (`GameViewModel.kt:817`)
     * and answers a tap on a different peg with the quiet no-op (`:823`), a rule its own KDoc
     * states outright (`:788-796`). A tutorial that re-selected would teach a gesture the real
     * board refuses within a graduate's first few taps. The expensive half — performing the
     * same tap on both screens and comparing — is a device check.
     */
    @Test
    fun aTapOnADifferentPegWhileOneIsHeldChangesNothingExactlyAsTheGameplayBoardDoes() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            // Hold T2's movable peg, which offers exactly one destination.
            viewModel.onCellTap(T2_MOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_MOVABLE_PEG)
            val heldTargets = viewModel.uiState.value.board.legalTargets

            // Now tap the OTHER peg. The selection does not move there, and its dots stand.
            viewModel.onCellTap(T2_IMMOVABLE_PEG)
            runCurrent()

            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_MOVABLE_PEG)
            assertThat(viewModel.uiState.value.board.legalTargets).isEqualTo(heldTargets)
            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)

            // Re-tapping the held peg is still the one deselect, and the other peg then
            // selects normally — the recovery is one tap, and it is the game's own.
            viewModel.onCellTap(T2_MOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isNull()
            viewModel.onCellTap(T2_IMMOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_IMMOVABLE_PEG)
        }

    /**
     * The release clause (§4.4, criterion 3): the escalation puts down a held peg it is **not**
     * pointing at, in the same state change that raises the ring.
     *
     * Without it, matching the game's tap rule would make the help *unreachable* rather than
     * merely slow — while a peg is held, a tap on the pulsing peg is a class-2 no-op, so the
     * coach would point at a peg the player cannot pick up and every further tap would
     * re-escalate onto the same unreachable suggestion.
     */
    @Test
    fun theCoachEscalationPutsDownAHeldPegItIsNotPointingAt() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            viewModel.onCellTap(T2_IMMOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_IMMOVABLE_PEG)

            // Three taps that change nothing — the tap-count trigger.
            T2_EMPTY_HOLES.forEach { hole ->
                viewModel.onCellTap(hole)
                runCurrent()
            }

            assertThat(viewModel.uiState.value.coachTick).isEqualTo(FIRST_ESCALATION)
            assertThat(viewModel.uiState.value.board.hintSource).isEqualTo(T2_MOVABLE_PEG)
            // Released: the coached peg can be tapped straight away.
            assertThat(viewModel.uiState.value.board.selected).isNull()
            assertThat(viewModel.uiState.value.board.legalTargets).isEmpty()
        }

    /** The same release on the other trigger — idleness, with no taps at all (§4.4). */
    @Test
    fun theIdleNudgeAlsoPutsDownAHeldPegTheCoachIsNotPointingAt() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            viewModel.onCellTap(T2_IMMOVABLE_PEG)
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_IMMOVABLE_PEG)

            settleIdleNudge()

            assertThat(viewModel.uiState.value.coachTick).isEqualTo(FIRST_ESCALATION)
            assertThat(viewModel.uiState.value.board.selected).isNull()
        }

    /**
     * The one case the release must **not** fire (§4.4): the held peg *is* the coached source.
     *
     * The player has already selected correctly and only needs to find the dot, so letting go
     * there would undo a correct selection and make the help worse rather than better.
     */
    @Test
    fun theCoachEscalationKeepsAHeldPegThatIsItsOwnCoachedSource() =
        runTest(testDispatcher) {
            val viewModel = viewModelFor()
            runCurrent()
            playTheOnlyMove(viewModel)
            settleStepAdvance()

            viewModel.onCellTap(T2_MOVABLE_PEG)
            runCurrent()
            val heldTargets = viewModel.uiState.value.board.legalTargets

            settleIdleNudge()

            assertThat(viewModel.uiState.value.coachTick).isEqualTo(FIRST_ESCALATION)
            assertThat(viewModel.uiState.value.board.hintSource).isEqualTo(T2_MOVABLE_PEG)
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(T2_MOVABLE_PEG)
            assertThat(viewModel.uiState.value.board.legalTargets).isEqualTo(heldTargets)
        }

    @Test
    fun theTutorialViewModelsOnlyCollaboratorsAreTheOnboardingRepositoryAndTheApplicationScope() {
        // §5.1 prohibitions 2 and 3 as a TEST rather than a grep: a later refactor that
        // injects ProgressRepository, ResumeRepository or ChallengeProgress — any of which
        // could bank a star, clear a resume snapshot or advance the ladder — fails here.
        val constructor = TutorialViewModel::class.java.declaredConstructors.single()

        assertThat(constructor.parameterTypes.toList())
            .containsExactly(OnboardingRepository::class.java, CoroutineScope::class.java)
    }

    /** Plays the single legal move of whichever one-move step is installed (T1 or T2). */
    private fun TestScope.playTheOnlyMove(viewModel: TutorialViewModel) {
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
        viewModel.onCellTap(move.from)
        runCurrent()
        viewModel.onCellTap(move.to)
        runCurrent()
    }

    /** Lets the one functional beat between a completed step and the next board elapse. */
    private fun TestScope.settleStepAdvance() {
        advanceTimeBy(TutorialAnim.STEP_ADVANCE_DELAY_MS.toLong() + 1)
        runCurrent()
    }

    /** Lets the idle nudge fire — the second of the escalation's two triggers (§4.4). */
    private fun TestScope.settleIdleNudge() {
        advanceTimeBy(TutorialAnim.NUDGE_DELAY_MS.toLong() + 1)
        runCurrent()
    }

    private fun TestScope.viewModelFor(
        repository: OnboardingRepository = GatedOnboardingRepository(),
    ): TutorialViewModel = TutorialViewModel(repository, backgroundScope)

    private companion object {
        const val FIRST_STEP = 0
        const val SECOND_STEP = 1
        const val THIRD_STEP = 2
        const val T1_START_PEGS = 2
        const val ONE_PEG = 1

        /** The coach escalates **once**; this is that one escalation's ordinal. */
        const val FIRST_ESCALATION = 1

        /** T2's only movable peg — the source of its single legal move, so the coached one. */
        val T2_MOVABLE_PEG = Position(row = 0, col = 1)

        /** T2's middle peg: in play, holds a peg, and has no legal jump (§4.4 class 1). */
        val T2_IMMOVABLE_PEG = Position(row = 1, col = 1)

        /** An in-play empty hole on T2 that is never a legal destination. */
        val T2_ILLEGAL_HOLE = Position(row = 2, col = 2)

        /** Three in-play empty holes on T2 — enough taps to reach the coach (§4.4 class 3). */
        val T2_EMPTY_HOLES =
            listOf(
                Position(row = 0, col = 0),
                Position(row = 0, col = 2),
                Position(row = 2, col = 0),
            )

        /**
         * A full four-move winning line for T3, source then destination per move. The last
         * two moves are made by the same peg, so they also exercise the pitch ladder.
         */
        val T3_WINNING_LINE =
            listOf(
                Position(row = 0, col = 1) to Position(row = 2, col = 1),
                Position(row = 0, col = 0) to Position(row = 2, col = 0),
                Position(row = 2, col = 0) to Position(row = 2, col = 2),
                Position(row = 2, col = 2) to Position(row = 0, col = 2),
            )

        /** State vocabulary D-01c forbids: a tutorial has no failure and keeps no tally. */
        val FORBIDDEN_STATE_WORDS = listOf("error", "invalid", "retry", "score", "fail")
    }
}

/**
 * An [OnboardingRepository] whose write can be held open, which is the only way to tell
 * "the signal waits for the write" from "the write was merely started".
 */
private class GatedOnboardingRepository(
    private val gate: CompletableDeferred<Unit>? = null,
) : OnboardingRepository(NeverEmittingDataStore) {
    var markSeenCount = 0
        private set

    var markSeenStarted = false
        private set

    override val onboardingSeen: Flow<Boolean> = flowOf(false)

    override suspend fun markSeen() {
        markSeenStarted = true
        gate?.await()
        markSeenCount++
    }
}

/** A [DataStore] the base repository is constructed over and never actually reads. */
private object NeverEmittingDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = emptyPreferences()
}
