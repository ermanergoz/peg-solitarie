package com.app.pegsolitaire.domain.progress

/**
 * Scoped tunable constants for the cross-device progress merge (CLOUD-02 / D-05 / D-08).
 *
 * Split by scope per the project's no-magic-number rule, in the same shape as
 * [com.app.pegsolitaire.domain.achievement.AchievementThresholds], `ScoreParams` and
 * `AssistAllowanceLimits`: every value is a KDoc'd `const val` naming the decision that
 * fixes it, and every merge site reads its floor from here rather than restating it.
 * detekt's `MagicNumber` exempts constant declarations, which is precisely why this object
 * is the only legal home for these numbers — a second statement of any of them, in the
 * merge, in the repository, or in a codec, is a bug waiting for the copies to drift apart.
 *
 * ## Remit: the identities of the merge, and only those
 *
 * These are **identity elements**, not validation bounds. The star ceiling, the score
 * ceiling and the decode-time rejection of an out-of-range value belong to plan 13-03's
 * `data/cloud/` decoder, where untrusted bytes arrive; nothing here clamps anything.
 */
object ProgressMergeLimits {
    /**
     * The identity of the `max`-merged `stars` field — "no stars recorded".
     *
     * Zero is the correct identity for a `max` merge (`max(x, 0) == x` for any legal star
     * count, which is never negative), and it is the same floor
     * `DefaultProgressRepository.recordResultInTransaction` has used for the "no previous
     * row" case since v1.0.
     */
    const val NO_STARS = 0

    /** The identity of the `max`-merged `bestScore` field — "no score recorded". */
    const val NO_SCORE = 0

    /**
     * The identity of the `min`-merged `bestTimeMs` field — "no time recorded".
     *
     * **This is [Long.MAX_VALUE] and must never become `0L` (RESEARCH Pitfall 10).** The
     * best-time field merges by `min`, and `0L` is that merge's *absorbing* element, not
     * its identity: `min(x, 0) == 0` for every real time. One defaulted, corrupt or
     * hostile row carrying a zero would therefore pin that level's best time at zero on
     * the local device, then propagate the zero to the cloud, then to every other device
     * the player signs into — permanently, because the merge is monotone and can never
     * walk a value back up. [Long.MAX_VALUE] is the only value for which
     * `min(x, identity) == x` holds over the whole `Long` range, which is exactly what
     * `ProgressMergeTest.mergeWithEmptyIsIdentity` asserts.
     *
     * Plan 13-03's decoder additionally *rejects* an inbound `bestTimeMs <= 0` at the
     * trust boundary (drop that row, keep the rest — fail open per CLOUD-05), so a
     * tampered payload cannot smuggle the absorbing value past this identity.
     */
    const val NO_TIME_RECORDED = Long.MAX_VALUE

    /**
     * The identity of the `max`-merged ladder field — the first Challenge level (D-08).
     *
     * The ladder is `ChallengeProgress.currentLevel`, the **next** level to play, so its
     * lowest legal value is level 1 and a brand-new player is exactly that. `max` over two
     * ladders therefore has `1` as its identity.
     *
     * Deliberately equal to the `app` module's
     * `ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL` and to
     * [com.app.pegsolitaire.domain.generator.ChallengeLevelBounds.MIN]. `:domain` cannot
     * reference an `app`-module constant (that is the whole point of the pure-Kotlin
     * module), so the equality cannot be expressed as a shared symbol across that
     * boundary: `ProgressMergeTest` asserts it against `ChallengeLevelBounds.MIN` here,
     * and plan 13-03's codec test asserts the `app`-side half. Same belt-and-braces
     * treatment [com.app.pegsolitaire.domain.achievement.AchievementThresholds.PERFECT_STARS]
     * already gets against `ScoreParams.THREE_STARS`.
     */
    const val DEFAULT_LADDER = 1

    /**
     * The identity of the `max`-merged `playedTimeMs` field — "nothing played yet" (D-10).
     *
     * Played time is `SUM(elapsedMs)` over solved plays and merges by `max` rather than by
     * addition, so two devices reporting overlapping histories cannot double-count. `max`
     * over a non-negative quantity has `0L` as its identity — and unlike `bestTimeMs`,
     * zero here is genuinely the neutral value, not the absorbing one.
     */
    const val NO_PLAYED_TIME = 0L
}
