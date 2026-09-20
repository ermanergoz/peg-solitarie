package com.app.pegsolitaire.domain.progress

/**
 * Which half of the game a stored best belongs to, as `:domain` speaks it.
 *
 * **This enum is NOT the on-disk column value and NOT the wire token** (RESEARCH
 * Pitfall 11). `ProgressPersistenceKeys.MODE_CLASSIC` / `MODE_CHALLENGE` are the strings
 * Room stores; the cloud payload carries its own independently-pinned tokens. Reusing
 * either one here would couple three formats that must be free to change apart: renaming
 * a database constant would otherwise silently orphan every cloud save already written.
 *
 * The two-way translation lives in exactly one place — plan 13-03's `data/cloud/` mapper —
 * in the same shape `ClassicBoardIds` already uses to turn stored level-id text into a
 * [com.app.pegsolitaire.domain.achievement.ClassicBoard]. `:domain` never learns a mode
 * string, here or anywhere.
 */
enum class ProgressMode {
    /** A hand-defined Classic board. Never trimmed at the size cap (D-05). */
    CLASSIC,

    /** A procedurally-generated Challenge level, identified by its level number as text. */
    CHALLENGE,
}

/**
 * The identity of one level's stored bests: its mode plus its level id.
 *
 * A `data class` rather than a composed string key, so the two halves cannot be
 * concatenated into an ambiguous token and so [ProgressMode] stays a compile-time
 * exhaustive choice at every use site.
 *
 * @property levelId The id as stored: a Classic board's id text, or a Challenge level's
 *   number rendered as decimal. It is **not** parsed here — [ProgressMerge] only ever
 *   reads it numerically for the trim ordering, and tolerates an id that will not parse.
 */
data class LevelKey(
    val mode: ProgressMode,
    val levelId: String,
)

/**
 * The three independently-merged bests for one level (D-07).
 *
 * Deliberately narrower than the RESEARCH sketch, which also carried `mode` and `levelId`
 * on this class: both already live in the [LevelKey] that indexes it, and duplicating them
 * would create a second place for them to disagree — a map entry whose key said Challenge
 * 42 and whose value said Classic English-33 would be representable, and nothing would
 * catch it.
 *
 * **`completedAt` is deliberately absent.** It means "last improved *on this device*" and
 * has no cross-device meaning at all; syncing it would also make the merge non-idempotent,
 * because the value that changes would not be the value that decides whether anything
 * changed — re-merging the same payload would keep restamping it. It stays local: on
 * import, an existing row keeps its own `completedAt` and a newly-arriving row is stamped
 * with the import time.
 *
 * @property stars Merged by `max`; identity [ProgressMergeLimits.NO_STARS].
 * @property bestTimeMs Merged by **`min`**; identity
 *   [ProgressMergeLimits.NO_TIME_RECORDED] — see that constant for why it is not `0L`.
 * @property bestScore Merged by `max`; identity [ProgressMergeLimits.NO_SCORE].
 */
data class LevelBest(
    val stars: Int,
    val bestTimeMs: Long,
    val bestScore: Int,
)

/**
 * The whole of what follows the player's Play Games account: every level's bests plus the
 * Challenge ladder and the played-time metadata (CLOUD-01 / CLOUD-02).
 *
 * Every property is DEFAULTED, so a brand-new player is exactly `CloudProgress()` — the
 * convention [com.app.pegsolitaire.domain.achievement.ProgressAggregate] sets, and here it
 * pays twice over: [EMPTY] is both the fresh-player value and the **identity element** of
 * [ProgressMerge.merge], which is what gives the identity law a free fixture.
 *
 * **The in-progress board snapshot is not here and never will be (CLOUD-04).** Nor is
 * `play_session` telemetry. This class is the complete definition of what leaves the
 * device; a field added here is a field synced, so adding one is a product decision, not
 * a refactor.
 *
 * Pure Kotlin with zero annotations: the serialization library is not on `:domain`'s
 * classpath at all — the module declares no `implementation` dependency whatsoever — so
 * the annotated DTO mirror lives in `data/cloud/`, exactly as `ResumeSnapshotDto` mirrors
 * the resume model (RESEARCH Pitfall 9). A purity gate greps this package for serializer
 * and platform imports, so keep that vocabulary out of it, prose included.
 *
 * @property ladder `ChallengeProgress.currentLevel` — the **next** Challenge level to
 *   play, not the highest solved (D-08). Merged by `max`, which is safe precisely because
 *   no UI calls `ChallengeProgress.reset()`, so the ladder is monotonic today. Note this
 *   is *not* `ProgressAggregate.highestChallengeLevel`; the off-by-one between them is
 *   expected and documented there.
 * @property playedTimeMs Total solved-play time in millis (D-10), merged by `max` rather
 *   than summed so two overlapping histories cannot double-count. Feeds the saved game's
 *   `playedTimeMillis` metadata (CLOUD-03).
 * @property bests Every level's bests, keyed by [LevelKey]. A level absent from the map
 *   has never been solved — there is no "present with zeroes" state, so a merge can never
 *   invent a row nor a caller accidentally submit a zero for an unplayed level.
 */
data class CloudProgress(
    val ladder: Int = ProgressMergeLimits.DEFAULT_LADDER,
    val playedTimeMs: Long = ProgressMergeLimits.NO_PLAYED_TIME,
    val bests: Map<LevelKey, LevelBest> = emptyMap(),
) {
    companion object {
        /**
         * The fresh-player value, and the identity element of [ProgressMerge.merge]:
         * `merge(a, EMPTY) == a` and `merge(EMPTY, a) == a` for every `a`.
         */
        val EMPTY = CloudProgress()
    }
}
