package com.app.pegsolitaire.ui.game

import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry

/**
 * The payload of `GameViewModel.finished`: the play's [ScoreResult] and the
 * [SolveTelemetry] that belongs to it, travelling as one event (D-08).
 *
 * This is D-08's "rides alongside" made structural. [ScoreResult] is a *scoring* type —
 * stars, score, pegs, elapsed seconds — and gains nothing here; the achievement-facing
 * facts (hints taken, undos taken, whether the last peg landed on the board's target)
 * live in their own carrier. Pairing them in one event is what makes it impossible for a
 * consumer to receive a score without the telemetry of the very play that produced it,
 * which a second `Flow` or a nullable side channel would not.
 *
 * Deliberately a **UI-layer transport type**:
 *  - not in `:domain`, because there is nothing pure to compute — it is a two-field
 *    envelope for a one-shot event that only exists between `GameViewModel` and
 *    `GameRoute`;
 *  - not a [ScoreResult] field, because a scoring type that also carried assist counts
 *    would be re-derived, persisted and compared in places that have no business seeing
 *    them (`ProgressRepository`'s per-level best merge, for one).
 *
 * `GameRoute` unpacks it into the `Results` route's flat scalar args; `ResultsViewModel`
 * resolves those back into one clamped [SolveTelemetry].
 *
 * @property result the pure-domain scoring outcome, byte-identical to what the finish
 *   event carried before telemetry existed.
 * @property telemetry the per-solve facts scoring does not care about but achievements do.
 * @property minimumPegs the FINISHING BOARD's own `BoardSpec.minimumPegs` — the peg count
 *   at which a Classic board counts as solved. Carried because "solved" is a per-board fact in
 *   Classic and nothing downstream can re-derive it: `GameViewModel` declares a Classic win with
 *   `pegCount <= spec.minimumPegs` (Diamond-41's proven floor is TWO), while Results is a
 *   display-only destination with no board in hand. Sending it alongside the score is what
 *   lets `ResultsViewModel` ask the same question `GameViewModel` already answered, instead
 *   of a hardcoded one-peg test that drops every flawless Diamond-41 win on the floor. A
 *   Challenge finish is cleared by `ChallengeClearRule` instead, which reads no floor; every
 *   generated footprint carries one here anyway (docs/gdd.md I5).
 */
data class GameFinish(
    val result: ScoreResult,
    val telemetry: SolveTelemetry,
    val minimumPegs: Int,
)
