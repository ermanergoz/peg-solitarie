package com.app.pegsolitaire.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.pegsolitaire.data.HapticsSetting
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
 * therefore has exactly **three** collaborators, [OnboardingRepository], [HapticsSetting] and the
 * application scope, and `TutorialViewModelTest` pins that by reflection over this constructor
 * rather than by a grep, so a later refactor that injects a progress dependency fails a test.
 * The third is allowed because it is not a progress dependency: it is a read-only seam over one
 * setting, Haptics, which the jump the player feels honours exactly as on the gameplay board
 * (D-01j), and a second test keeps that seam to its single member (tech-lead ruling 2026-10-18).
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
 * @param hapticsSetting the persisted Haptics switch (SET-02), collected into
 *   `board.hapticsOn` exactly as the gameplay board collects it, so the shared jump haptic reads
 *   the same gate on both boards.
 * @param applicationScope the process-lifetime scope the seen flag is written on, so the
 *   write outlives the screen that requested it (the same reasoning
 *   `ProgressSubmitter.onProfileSwapResolved` already records for the wipe).
 */
@HiltViewModel
class TutorialViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        hapticsSetting: HapticsSetting,
        // `@param:`, as in SoundPoolPlayer: Dagger reads the parameter annotation, and the
        // default use-site target is changing (KT-73255), which Kotlin 2.3 warns about.
        @param:ApplicationScope private val applicationScope: CoroutineScope,
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
            // Same contract as the gameplay board (SET-02, D-15): the board's state carries the
            // persisted switch and the screen fires the tick; fail-open ON until the first
            // emission. No `catch` of its own: the repository's read already fails open (D-17).
            viewModelScope.launch {
                hapticsSetting.hapticsOn.collect { on ->
                    _uiState.update { it.withBoard(it.board.copy(hapticsOn = on)) }
                }
            }
            armNudge()
        }

        /**
         * Handles a tap on [position]. Selection, deselection and the jump — and nothing
         * else: there is no error state, no red, no retry count and no score (D-01c), so a
         * tap that changes nothing simply escalates *assistance*.
         *
         * ## The branch order is the gameplay board's, deliberately (§4.1 "Tap semantics")
         *
         * A tap rule is the mechanic this screen exists to teach, so it is the *game's* rule:
         * `GameViewModel.onCellTap` selects only while **nothing** is selected
         * (`GameViewModel.kt:817`) and answers every other tap with the quiet no-op (`:823`),
         * which its own KDoc states outright — re-tapping the selected peg is the only
         * deselect, and tapping a different peg while one is held changes nothing
         * (`GameViewModel.kt:788-796`). A tutorial that re-selected instead would teach a
         * gesture the real board then refuses, and it would refuse within a graduate's first
         * few taps, because "the peg I tapped cannot move" is the ordinary case on a full
         * board rather than a corner case.
         *
         * A player left holding an immovable peg is freed by [escalateCoach]'s release clause,
         * which is tutorial-only assistance and so diverges from nothing — **not** by the
         * board reinterpreting their next tap.
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
                // A peg selects only while NOTHING is held — the game's rule, above.
                selected == null && board.cellAt(position) == Cell.Peg -> select(position)
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
                    // The persisted Haptics value is read INSIDE the update, from the board being
                    // replaced: a fresh step board starts at the fail-open `true`, and the
                    // DataStore flow does not re-emit an unchanged value, so a value captured
                    // outside the lambda could lose a collector emission landing between the two.
                    _uiState.update { TutorialUiState.forStep(nextIndex, hapticsOn = it.board.hapticsOn) }
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
         *
         * ## The release clause (§4.4, ruled 2026-09-21) — do not drop this half
         *
         * When the escalation fires while a peg is held that is **not** the peg it is about to
         * point at, it puts that peg back down, silently, in the same state change that raises
         * the ring — on both triggers, [registerNonProgressingTap]'s count and [armNudge]'s
         * idleness alike. Without it, matching the gameplay board's tap rule (see [onCellTap])
         * would make this help *unreachable* rather than merely slow: while a peg is held, a
         * tap on the pulsing peg is itself a non-progressing no-op, so the coach would point at
         * a peg the player cannot pick up and every further tap would re-escalate onto the same
         * unreachable suggestion.
         *
         * It deliberately does **not** release when the held peg *is* the coached source: the
         * player has already selected correctly and only needs to find the dot, so letting go
         * there would undo a correct selection and make the help worse.
         *
         * The release is silent because this screen hosts no cue that could sound it — the
         * `Deselect` and `Hint` effects both stay private to the gameplay screen (§5, D-01i).
         */
        private fun escalateCoach() {
            nudgeJob?.cancel()
            nonProgressingTaps = 0
            val hint = RulesEngine.legalMoves(_uiState.value.board.board).firstOrNull() ?: return
            _uiState.update {
                val releaseHeldPeg = it.board.selected != null && it.board.selected != hint.from
                it
                    .withBoard(
                        it.board.copy(
                            hintSource = hint.from,
                            hintTarget = hint.to,
                            selected = if (releaseHeldPeg) null else it.board.selected,
                            legalTargets = if (releaseHeldPeg) emptyList() else it.board.legalTargets,
                        ),
                    ).copy(coachTick = it.coachTick + 1)
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
