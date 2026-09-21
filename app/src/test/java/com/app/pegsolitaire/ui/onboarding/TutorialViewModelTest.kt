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
            viewModel.onCellTap(Position(row = 1, col = 1))
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(Position(row = 1, col = 1))
            assertThat(viewModel.uiState.value.board.legalTargets).isEmpty()

            // Tapping an illegal destination while a movable peg is selected KEEPS the
            // selection — nothing is lost and nothing is announced as wrong.
            viewModel.onCellTap(Position(row = 0, col = 1))
            runCurrent()
            viewModel.onCellTap(Position(row = 2, col = 2))
            runCurrent()
            assertThat(viewModel.uiState.value.board.selected).isEqualTo(Position(row = 0, col = 1))
            assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)

            // And there is no error field to set in the first place (D-01c), which is the
            // half a behavioural assertion cannot reach.
            val fields = TutorialUiState::class.java.declaredFields.map { it.name.lowercase() }
            assertThat(fields.none { FORBIDDEN_STATE_WORDS.any(it::contains) }).isTrue()
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

    private fun TestScope.viewModelFor(
        repository: OnboardingRepository = GatedOnboardingRepository(),
    ): TutorialViewModel = TutorialViewModel(repository, backgroundScope)

    private companion object {
        const val FIRST_STEP = 0
        const val SECOND_STEP = 1
        const val THIRD_STEP = 2
        const val T1_START_PEGS = 2
        const val ONE_PEG = 1

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
