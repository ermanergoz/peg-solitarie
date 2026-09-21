package com.app.pegsolitaire.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.OnboardingRepository
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.app.pegsolitaire.ui.game.MoveTransition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the interactive tutorial (ONB-01, D-01/D-01a-i) over an immutable
 * [StateFlow]<[TutorialUiState]>.
 *
 * ## What it deliberately is not
 *
 * It is **not** the gameplay state holder and must never become it (ux-spec §5.1,
 * prohibitions 2 and 3, which name it). That holder's `Won` path clears the mode's resume
 * snapshot and advances the Challenge ladder; every tutorial step ends on exactly one peg, so
 * routing the tutorial through it would fire that path three times per run and destroy two of
 * the three cloud-save device oracles — a failure that looks correct on screen and is wrong in
 * the database. This class
 * therefore has exactly **two** collaborators, [OnboardingRepository] and the application
 * scope, and `TutorialViewModelTest` pins that by reflection over this constructor rather
 * than by a grep, so a later refactor that injects a progress dependency fails a test.
 *
 * For the same reason a step's completion is `board.pegCount == 1` evaluated here and not a
 * `GameStatus`: reaching for the game's status type is the first step down that same path
 * (§4.1).
 *
 * There is **no saved-state cursor** (§5 names the type): a persisted one would survive
 * process death and
 * resume at T2, contradicting D-01e's ruling that a process death mid-tutorial re-offers the
 * tutorial from T1. Within a session the `NavBackStackEntry` retains this ViewModel, so
 * rotation keeps the step for free (D-01g).
 *
 * ## The exit, which is the one place this can go wrong
 *
 * [onExit] is the **single** exit path with three callers — `Skip`, system back and the
 * completion CTA (D-01d). It **awaits** `markSeen()` and only then emits [exitSignal]; the
 * screen navigates on that signal and never on the tap. The write runs on the injected
 * `@ApplicationScope` scope, so the pop cancelling this ViewModel's own scope cannot drop it.
 *
 * Both halves are load-bearing. `OnboardingRepository.markSeen` swallows its own failure by
 * design, so a fire-and-forget write that never completes leaves the flag `false`, Home's
 * `LaunchedEffect` re-pushes `Tutorial`, and `launchSingleTop` does not help because
 * `Tutorial` is no longer on top — a loop the player cannot leave. A double exit is a latched
 * no-op for the same reason.
 *
 * @param onboardingRepository the DataStore-backed seen flag — the single source of truth for
 *   whether the tutorial is still owed, and the only thing this screen persists.
 * @param applicationScope the process-lifetime scope the seen flag is written on, so the
 *   write outlives the screen that requested it (the same reasoning
 *   `ProgressSubmitter.onProfileSwapResolved` already records for the wipe).
 */
@HiltViewModel
class TutorialViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TutorialUiState.initial())

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

        /**
         * A **buffered** one-shot channel rather than a replay-less `SharedFlow`: the exit
         * signal is emitted from the application scope and may land before the screen's
         * collector has attached, and a dropped signal is a tutorial the player cannot leave.
         */
        private val exitChannel = Channel<Unit>(capacity = Channel.CONFLATED)

        /** Emits once, after the seen flag is persisted, when the screen should pop. */
        val exitSignal: Flow<Unit> = exitChannel.receiveAsFlow()

        /** The latch behind [onExit]; only ever read and written from the main thread. */
        private var exitRequested = false

        /** How many consecutive taps have made no progress in the current step (§4.4). */
        private var nonProgressingTaps = 0

        /** The cell the last jump landed on, for the same-peg pitch ladder (D-05/D-06). */
        private var lastLandedOn: Position? = null

        /** The current same-peg run length, i.e. the pitch ladder rung. */
        private var runIndex = 0

        /** Monotonic move ordinal — [MoveTransition.id], so each jump keys a fresh animation. */
        private var moveOrdinal = 0

        private var nudgeJob: Job? = null

        init {
            armNudge()
        }

        /**
         * Handles a tap on [position]. Selection, deselection and the jump — and nothing
         * else: there is no error state, no red, no retry count and no score (D-01c), so a
         * tap that changes nothing simply escalates *assistance*.
         */
        fun onCellTap(position: Position) {
            val state = _uiState.value
            if (state.completed) return
            val board = state.board.board
            val selected = state.board.selected

            when {
                // A re-tap puts the peg back down; a tap on a legal dot jumps.
                selected == position -> deselect()
                selected != null && position in state.board.legalTargets -> jump(from = selected, to = position)
                // Any other peg SELECTS, including while another peg is already held. That
                // matters on T2 and T3, where some pegs have no legal jump at all (§4.4
                // class 1): a player who taps one must be able to pick a movable peg
                // straight afterwards rather than having to put the first one down.
                board.cellAt(position) == Cell.Peg -> select(position)
                else -> registerNonProgressingTap()
            }
        }

        /** Clears the finished jump animation, exactly as the gameplay screen does. */
        fun onAnimationFinished() {
            _uiState.update { it.withBoard(it.board.copy(animating = null)) }
        }

        /**
         * The one exit path (D-01d/D-01g): persist, **then** signal. Latched, so `Skip`
         * followed by a fast back press writes once and pops once.
         */
        fun onExit() {
            if (exitRequested) return
            exitRequested = true
            // On the APPLICATION scope, not viewModelScope: the pop that follows the signal
            // clears this ViewModel, and a write on a cancelled scope silently never lands.
            applicationScope.launch {
                onboardingRepository.markSeen()
                exitChannel.send(Unit)
            }
        }

        private fun select(position: Position) {
            nonProgressingTaps = 0
            _uiState.update {
                it.withBoard(
                    it.board.copy(
                        selected = position,
                        legalTargets =
                            RulesEngine
                                .legalMoves(it.board.board)
                                .filter { move -> move.from == position }
                                .map { move -> move.to },
                    ),
                )
            }
            armNudge()
        }

        private fun deselect() {
            _uiState.update { it.withBoard(it.board.copy(selected = null, legalTargets = emptyList())) }
            armNudge()
        }

        private fun jump(
            from: Position,
            to: Position,
        ) {
            val state = _uiState.value
            val move =
                RulesEngine.legalMoves(state.board.board).first { it.from == from && it.to == to }
            val next = RulesEngine.applyMove(state.board.board, move)

            runIndex = if (lastLandedOn == from) runIndex + 1 else 0
            lastLandedOn = to
            moveOrdinal += 1
            nonProgressingTaps = 0

            _uiState.update {
                it.withBoard(
                    it.board.copy(
                        board = next,
                        selected = null,
                        legalTargets = emptyList(),
                        hintSource = null,
                        hintTarget = null,
                        animating =
                            MoveTransition(
                                from = move.from,
                                to = move.to,
                                removed = move.over,
                                id = moveOrdinal,
                                runIndex = runIndex,
                            ),
                    ),
                )
            }

            if (next.pegCount == TutorialUiState.STEP_COMPLETE_PEGS) {
                advanceAfterABeat()
            } else {
                armNudge()
            }
        }

        /**
         * Installs the next step — or completes the tutorial — one beat after the jump, so
         * the player sees their own jump land before the board changes (§4.1).
         *
         * The beat is **functional, not decorative**: it still applies when OS animations are
         * off, because collapsing it would swap boards under the player's finger (§10).
         */
        private fun advanceAfterABeat() {
            nudgeJob?.cancel()
            viewModelScope.launch {
                delay(TutorialAnim.STEP_ADVANCE_DELAY_MS.toLong())
                val nextIndex = _uiState.value.stepIndex + 1
                if (nextIndex >= TutorialBoards.ALL.size) {
                    _uiState.update { it.completed() }
                } else {
                    resetStepCursors()
                    _uiState.update { TutorialUiState.forStep(nextIndex) }
                    armNudge()
                }
            }
        }

        private fun resetStepCursors() {
            lastLandedOn = null
            runIndex = 0
            nonProgressingTaps = 0
        }

        /**
         * A tap that changed nothing (§4.4 classes 1 and 2). It is never a failure: the
         * selection and the board are untouched, nothing is announced as wrong, and on the
         * third such tap the coach escalates once.
         */
        private fun registerNonProgressingTap() {
            nonProgressingTaps += 1
            if (nonProgressingTaps >= TutorialAnim.NON_PROGRESSING_TAPS_BEFORE_COACH) {
                escalateCoach()
            } else {
                armNudge()
            }
        }

        /**
         * Shows more help, never less: the source ring pulses and the destination dot
         * brightens, using the board's **own** highlight idiom rather than a new one (§10).
         * It escalates once, never blocks, and never offers to take a move back — no tutorial
         * board can box the player in, so there is nothing to be rescued from, and promising a
         * rescue from an impossible failure teaches the player to expect failure.
         */
        private fun escalateCoach() {
            nudgeJob?.cancel()
            nonProgressingTaps = 0
            val hint = RulesEngine.legalMoves(_uiState.value.board.board).firstOrNull() ?: return
            _uiState.update {
                it
                    .withBoard(it.board.copy(hintSource = hint.from, hintTarget = hint.to))
                    .copy(coachTick = it.coachTick + 1)
            }
        }

        /** Re-arms the idle nudge: escalate once after [TutorialAnim.NUDGE_DELAY_MS] of no progress. */
        private fun armNudge() {
            nudgeJob?.cancel()
            nudgeJob =
                viewModelScope.launch {
                    delay(TutorialAnim.NUDGE_DELAY_MS.toLong())
                    escalateCoach()
                }
        }
    }
