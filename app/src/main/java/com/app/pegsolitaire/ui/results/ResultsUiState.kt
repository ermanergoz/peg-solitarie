package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.NewBests

/**
 * The single immutable UI state for the end-of-game Results destination (SCOR-03).
 *
 * Owned by `ResultsViewModel` and exposed over a read-only `StateFlow`; the screen
 * is a stateless render of this object (state hoisting). Every value is a `val` and
 * the type is `@Immutable`, so Compose can skip recomposition on equal instances.
 *
 * The four display values ([stars], [pegsRemaining], [elapsedSeconds], [score]) are
 * the typed `Results` route args verbatim — they were computed in-game by the
 * pure-domain `ScoreCalculator` (02-01) and are NEVER re-derived here. [headlineTier]
 * is the star-outcome bucket the screen resolves to its copy (D-08); [replayId] is the
 * catalog id the Replay action navigates to.
 *
 * @property stars 1-3 earned, from pegs remaining (D-01); drives the filled-star row.
 * @property pegsRemaining pegs left on the terminal board (PEGS LEFT metric).
 * @property elapsedSeconds whole seconds elapsed, formatted `mm:ss` (TIME metric).
 * @property score the ranking score (SCORE metric, D-03).
 * @property headlineTier the star-tiered headline bucket (D-08).
 * @property replayId the board just played — Replay re-plays this same level.
 * @property challengeLevel the Challenge level just played, or `null` for a Classic game.
 *   When non-null the screen renders the mode-aware Challenge actions (Next level / Retry,
 *   D-01/D-10) instead of the Classic Replay / Catalog / Home trio.
 * @property newBests which of the level's stored bests this play improved (D-09), as
 *   reported by `ProgressRepository.recordResult`. All three are `false` until the record
 *   completes — and stay `false` for a play that beat nothing (or was never recorded at
 *   all, D-08) — so the "New best!" cue only ever appears on a genuine improvement.
 * @property playCelebration whether THIS entry into Results should sound the star-pop
 *   celebration (D-16). Like [newBests] it is a one-shot cue a recomposition cannot
 *   re-fire, but its scope is wider than one composition: it is `true` only on the first
 *   construction for a given `SavedStateHandle`, so a configuration change (same instance)
 *   and a process-death restore or back navigation (new instance, same handle) alike yield
 *   `false` and re-entering the same Results shows the stars silently. Unlike the recorded
 *   best it does not depend on a solve — a stuck one-star finish still earns its one pop.
 * @property adScrimVisible the INT-04 quiescence signal: `true` from the instant an
 *   ad-eligible continue tap is accepted until the interstitial presentation *resolves*
 *   — dismissed, failed to show, or cancelled. The screen renders it as a plain dim that
 *   swallows all input (D-08: no text, no spinner; the dim IS the "something is coming"
 *   signal).
 *
 *   It is emphatically **not** a fixed-duration animation, and must never be reimplemented
 *   as one. A scrim that lifts on a timer while the ad is still opening reopens the exact
 *   accidental-tap window INT-04 exists to close: the player's stray second tap would land
 *   on a fullscreen ad that appeared underneath their finger, which is invalid traffic. The
 *   `AdConstants.INTERSTITIAL_QUIESCENCE_MS` pause happens *inside* the span this flag
 *   covers; it does not bound it.
 * @property pendingNavigation the forward-moving destination this Results entry has been
 *   asked to leave for, or `null` when it is not leaving. The route consumes it exactly
 *   once and calls `ResultsViewModel.onNavigationHandled()` to clear it.
 *
 *   It is deliberately **state on this object rather than an event stream**, and the reason
 *   is a real failure mode rather than a style preference (RESEARCH Pitfall 6). The host
 *   Activity is STOPPED for the whole ad presentation, so the route's
 *   `collectAsStateWithLifecycle` is not collecting at the moment the dismiss callback
 *   lands. A `StateFlow` field survives a stopped collector — the value is simply read when
 *   collection resumes — whereas a `Channel` or `SharedFlow` emission may be dropped
 *   outright or re-delivered on resume. A re-delivery here is a Challenge ladder that
 *   advances two levels for one tap, which is why this may not be "modernised" into a
 *   one-shot effect stream.
 */
@Immutable
data class ResultsUiState(
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
    val headlineTier: ResultsHeadlineTier,
    val replayId: String,
    val challengeLevel: Int? = null,
    val newBests: NewBests = NO_NEW_BESTS,
    val playCelebration: Boolean = false,
    val adScrimVisible: Boolean = false,
    val pendingNavigation: ResultsNavTarget? = null,
) {
    /**
     * Whether the "Next level" action is offered (D-01): only a solved Challenge level
     * advances. Solved ⇔ exactly one peg by construction, so a stuck Challenge board
     * ([pegsRemaining] >= 2) yields `false` and gets Retry instead (D-10). Always `false`
     * for Classic ([challengeLevel] == null).
     */
    val showNextLevel: Boolean
        get() = challengeLevel != null && pegsRemaining == PEGS_REMAINING_SOLVED
}

/**
 * The destinations a Results **continue** action can navigate to — and, by construction,
 * the complete set of taps that can ever be gated behind an interstitial (D-06).
 *
 * D-06 makes exactly two actions ad-eligible, and they are both *forward-moving win*
 * actions: Challenge "Next level" and Classic "Catalog" after a win. The three actions that
 * are NOT in this type are the whole point of it being a closed set:
 *
 *  - **Classic "Replay"** is a retry, and retry stays free — `AdReason.RETRY` was dropped
 *    at requirements time precisely so that repeating a level you just lost never costs an
 *    ad.
 *  - **"Home"** is an exit, and AdMob policy singles out interstitials on app exit as a
 *    disallowed implementation.
 *  - **Challenge-stuck "Retry"** is a loss, which INT-03 excludes outright.
 *
 * Those three keep calling their navigation lambdas directly from the composable and never
 * enter this type or `ResultsViewModel.onNextLevelTapped` / `onCatalogTapped`. That is what
 * makes them *structurally* unable to gain an ad later: adding one would mean adding a
 * member here and routing the tap through the ViewModel, which is a visible, reviewable
 * change rather than a silently widened condition.
 */
sealed interface ResultsNavTarget {
    /** Challenge: advance to [level], the level after the one just solved (D-01). */
    data class NextLevel(
        val level: Int,
    ) : ResultsNavTarget

    /** Classic: back to the board catalog after a win. */
    data object Catalog : ResultsNavTarget
}

/** A solved board leaves exactly one peg — the sole state that unlocks the next level (D-01). */
private const val PEGS_REMAINING_SOLVED = 1

/** Nothing improved (yet): the pre-record state and the state of a play that beat no best. */
private val NO_NEW_BESTS = NewBests(stars = false, time = false, score = false)

/**
 * The three star-outcome headline buckets (D-08). The tier → string-resource mapping
 * is resolved in `ResultsScreen` (keeping the copy in `strings.xml`), not here.
 */
enum class ResultsHeadlineTier {
    /** A textbook solve (3 stars) → "Perfect!". */
    PERFECT,

    /** A solid finish (2 stars) → "Solved!". */
    SOLVED,

    /** Out of moves with pegs left (1 star) → "Out of moves". */
    STUCK,
}
