package com.app.pegsolitaire.domain.progress

/**
 * The one monotonic progress merge — CLOUD-02's "two devices merge without data loss",
 * named (D-05: *where the merge lives*).
 *
 * This is not a new algorithm. `DefaultProgressRepository.recordResultInTransaction` has
 * performed `max` stars / `min` time / `max` score per level since v1.0, and
 * `ChallengeProgress.advancePast` has performed `max` ladder for just as long. This object
 * gives those two halves a single name so that local recording, the cloud import and the
 * SDK's conflict resolution all provably run the *same* merge rather than three
 * hand-written copies that can drift.
 *
 * ## Why the laws matter more than the code
 *
 * [merge] is a **join-semilattice** over [CloudProgress]: commutative, associative,
 * idempotent, with [CloudProgress.EMPTY] as its identity. Those four properties are the
 * whole argument that CLOUD-02 holds. Because the result does not depend on the order of
 * merges, on how many times a payload is delivered, or on which device merged first, every
 * device converges on the same union — so no sync ordering, no duplicate delivery and no
 * replayed conflict can lose a field. `ProgressMergeTest` machine-checks all four over
 * seeded random inputs, and the ROADMAP mandates that it be green **before** any
 * `SnapshotsClient` code is written, because an integration test cannot establish a
 * property that is quantified over every possible pair of inputs.
 *
 * Every field also merges **independently** (D-07): a fast sloppy run keeps its time and a
 * slow perfect run keeps its score, so a level's row can only ever improve. That is why
 * the merge is monotone, and monotone is why it can never walk a value backwards.
 *
 * Pure in every sense [com.app.pegsolitaire.domain.achievement.AchievementRules] is: a
 * top-level `object`, total and deterministic, no clock, no I/O, no SDK, no suspension,
 * no annotations, no injection, no hidden state.
 */
object ProgressMerge {
    /**
     * The better of [a] and [b] in every field, taken field by field (D-07).
     *
     * Note the asymmetry that makes this correct: `stars` and `bestScore` take the
     * **larger** value, `bestTimeMs` takes the **smaller** one. All three are still
     * monotone improvements — "better" simply points the other way for a stopwatch.
     */
    fun merge(
        a: LevelBest,
        b: LevelBest,
    ): LevelBest =
        LevelBest(
            stars = maxOf(a.stars, b.stars),
            bestTimeMs = minOf(a.bestTimeMs, b.bestTimeMs),
            bestScore = maxOf(a.bestScore, b.bestScore),
        )

    /**
     * The union of [a] and [b]: the higher ladder, the higher played time, and every level
     * either side knows about, merged where both know it.
     *
     * A key present on only one side is carried through **verbatim** rather than merged
     * against a fabricated identity row — the two are equivalent by the identity law, but
     * taking the row as-is makes it obvious that arriving progress is never diluted.
     */
    fun merge(
        a: CloudProgress,
        b: CloudProgress,
    ): CloudProgress =
        CloudProgress(
            ladder = maxOf(a.ladder, b.ladder),
            playedTimeMs = maxOf(a.playedTimeMs, b.playedTimeMs),
            bests = (a.bests.keys + b.bests.keys).associateWith { key -> bestAt(a, b, key) },
        )

    /**
     * [progress] with its [count] lowest Challenge levels removed — D-05's size-cap safety
     * valve, expected never to fire in practice.
     *
     * Three guarantees, each asserted by its own test, because this is the only function in
     * the phase that can *lose* a player's data:
     *  - **A CLASSIC row is never a candidate.** There are five of them, they are the
     *    hand-authored boards the player chose to play, and they are a rounding error
     *    against a 10,000-row Challenge ladder.
     *  - **[CloudProgress.ladder] and [CloudProgress.playedTimeMs] are never touched.**
     *    They are two scalars; trimming them would save nothing and would drop the player
     *    back down the Challenge ladder on their next sign-in.
     *  - **Challenge ids are ordered NUMERICALLY, not as text.** As text `"9"` outranks
     *    `"17"`, and the trim would throw away the hardest-won recent levels while keeping
     *    the early ones. An id that will not parse sorts LAST (as [Int.MAX_VALUE]), so a
     *    malformed row is dropped only after every well-formed one.
     *
     * A non-positive [count] is a no-op returning [progress] unchanged, and a [count] at or
     * beyond the Challenge row count leaves exactly the Classic rows plus the two scalars —
     * so the caller's shrink loop (plan 13-03's `encodeWithinCap`) is bounded and can never
     * spin.
     */
    fun dropLowestChallenge(
        progress: CloudProgress,
        count: Int,
    ): CloudProgress {
        val dropped =
            progress.bests.keys
                .filter { it.mode == ProgressMode.CHALLENGE }
                .sortedBy { it.levelId.toIntOrNull() ?: Int.MAX_VALUE }
                .take(count.coerceAtLeast(0))
                .toSet()
        if (dropped.isEmpty()) {
            return progress
        }
        return progress.copy(bests = progress.bests.filterKeys { it !in dropped })
    }

    /**
     * The bests for [key] across [a] and [b]. At least one side holds it, because [key] is
     * drawn from the union of the two key sets — the `error` is unreachable and exists so
     * a future caller cannot quietly pass a key from somewhere else and get a null back.
     */
    private fun bestAt(
        a: CloudProgress,
        b: CloudProgress,
        key: LevelKey,
    ): LevelBest {
        val left = a.bests[key]
        val right = b.bests[key]
        return when {
            left == null && right == null -> error("Key $key belongs to neither side of the merge")
            left == null -> requireNotNull(right)
            right == null -> left
            else -> merge(left, right)
        }
    }
}
