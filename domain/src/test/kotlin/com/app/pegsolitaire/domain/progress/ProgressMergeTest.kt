package com.app.pegsolitaire.domain.progress

import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/**
 * The algebraic-law spec for [ProgressMerge] — the whole of what makes CLOUD-02's "two
 * devices merge without data loss" true.
 *
 * CLOUD-02 is an **algebraic** property, not an integration one. If the merge is a
 * join-semilattice — commutative, associative, idempotent and identity-respecting — then
 * no ordering of syncs, no duplicate delivery and no replayed conflict can lose a field:
 * every device converges on the same union regardless of who merged what first. That is
 * exactly why this file lands (and is green) **before any `SnapshotsClient` code exists**,
 * per the ROADMAP sequencing mandate. Everything downstream — the codec, the import
 * transaction, the SDK conflict loop — is a faithful transport of what these laws decide.
 *
 * No property-testing library is used, deliberately: `:domain`'s entire dependency set is
 * `junit` + `truth`, and adding a generator framework for five laws is a poor trade.
 * Seeded [Random] loops give the same coverage at zero dependency cost — and the fixed
 * [LAW_SEED] makes a failure reproducible rather than a flake nobody can reproduce.
 *
 * Expectations about POLICY values are written as their named [ProgressMergeLimits]
 * constants, never as bare literals, per `AchievementRulesTest`'s rule. Bare literals used
 * as arbitrary *fixture* values (a ladder of `137`, a level id of `"42"`) are fine — they
 * are inputs chosen away from any boundary, not restatements of a tunable.
 */
class ProgressMergeTest {
    // ---- The algebraic laws -------------------------------------------------

    @Test
    fun mergeIsCommutative() {
        forEachPair { a, b ->
            assertThat(ProgressMerge.merge(a, b)).isEqualTo(ProgressMerge.merge(b, a))
        }
    }

    @Test
    fun mergeIsAssociative() {
        val rnd = Random(LAW_SEED)
        repeat(LAW_ITERATIONS) {
            val a = randomProgress(rnd)
            val b = randomProgress(rnd)
            val c = randomProgress(rnd)

            assertThat(ProgressMerge.merge(ProgressMerge.merge(a, b), c))
                .isEqualTo(ProgressMerge.merge(a, ProgressMerge.merge(b, c)))
        }
    }

    @Test
    fun mergeIsIdempotent() {
        forEachPair { a, b ->
            // Re-merging a value with itself changes nothing...
            assertThat(ProgressMerge.merge(a, a)).isEqualTo(a)
            // ...and a redelivered remote payload cannot move an already-merged result.
            val merged = ProgressMerge.merge(a, b)
            assertThat(ProgressMerge.merge(merged, b)).isEqualTo(merged)
        }
    }

    @Test
    fun mergeWithEmptyIsIdentity() {
        forEachPair { a, _ ->
            assertThat(ProgressMerge.merge(a, CloudProgress.EMPTY)).isEqualTo(a)
            assertThat(ProgressMerge.merge(CloudProgress.EMPTY, a)).isEqualTo(a)
        }
    }

    @Test
    fun mergeNeverRegressesAnyField() {
        forEachPair { a, b ->
            val merged = ProgressMerge.merge(a, b)

            assertThat(merged.ladder).isAtLeast(maxOf(a.ladder, b.ladder))
            assertThat(merged.playedTimeMs).isAtLeast(maxOf(a.playedTimeMs, b.playedTimeMs))
            assertThat(merged.bests.keys).isEqualTo(a.bests.keys + b.bests.keys)

            merged.bests.forEach { (key, best) ->
                val sides = listOfNotNull(a.bests[key], b.bests[key])
                assertThat(best.stars).isAtLeast(sides.maxOf { it.stars })
                assertThat(best.bestScore).isAtLeast(sides.maxOf { it.bestScore })
                assertThat(best.bestTimeMs).isAtMost(sides.minOf { it.bestTimeMs })
            }
        }
    }

    /**
     * Pitfall 10, stated as an assertion: the identity of the `min`-merged `bestTimeMs`
     * is [ProgressMergeLimits.NO_TIME_RECORDED], never `0L`.
     *
     * `0L` is that merge's **absorbing** element — a single defaulted or hostile row at
     * zero would pin the level's best time at zero on every device, forever, and
     * [mergeWithEmptyIsIdentity] would fail for every real time. Asserting the constant's
     * value directly is what stops a future "tidy-up" from redefining it to `0L`.
     */
    @Test
    fun emptyTimeIdentityIsNoTimeRecorded() {
        assertThat(ProgressMergeLimits.NO_TIME_RECORDED).isEqualTo(Long.MAX_VALUE)

        val real = LevelBest(stars = 2, bestTimeMs = 42_000L, bestScore = 900)
        val absent =
            LevelBest(
                stars = ProgressMergeLimits.NO_STARS,
                bestTimeMs = ProgressMergeLimits.NO_TIME_RECORDED,
                bestScore = ProgressMergeLimits.NO_SCORE,
            )

        assertThat(ProgressMerge.merge(real, absent)).isEqualTo(real)
        assertThat(ProgressMerge.merge(absent, real)).isEqualTo(real)
    }

    @Test
    fun keyPresentOnOneSideSurvives() {
        val onlyMine = LevelKey(ProgressMode.CHALLENGE, "137")
        val best = LevelBest(stars = 3, bestTimeMs = 12_345L, bestScore = 4_200)
        val mine = CloudProgress(ladder = 138, bests = mapOf(onlyMine to best))
        val theirs = CloudProgress(ladder = 7, bests = mapOf(LevelKey(ProgressMode.CLASSIC, "english33") to best))

        val merged = ProgressMerge.merge(mine, theirs)

        assertThat(merged.bests[onlyMine]).isEqualTo(best)
        assertThat(merged.bests).hasSize(2)
    }

    /**
     * [ProgressMergeLimits.DEFAULT_LADDER] is the same "next level to play" floor the app
     * module's `ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL` uses. `:domain` cannot
     * reference that `app`-module constant, so the equality is asserted here against
     * [ChallengeLevelBounds.MIN] (which the app constant is itself pinned to), exactly the
     * belt-and-braces `AchievementThresholds.PERFECT_STARS` / `ScoreParams.THREE_STARS`
     * precedent. Plan 13-03's codec test pins the app-side half.
     */
    @Test
    fun defaultLadderMatchesTheChallengeFloor() {
        assertThat(ProgressMergeLimits.DEFAULT_LADDER).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(CloudProgress.EMPTY.ladder).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(CloudProgress.EMPTY.playedTimeMs).isEqualTo(ProgressMergeLimits.NO_PLAYED_TIME)
        assertThat(CloudProgress.EMPTY.bests).isEmpty()
    }

    // ---- D-05's trim policy -------------------------------------------------

    @Test
    fun dropLowestChallengeKeepsEveryClassicRow() {
        val progress = trimFixture()
        val classicKeys = progress.bests.keys.filter { it.mode == ProgressMode.CLASSIC }

        val trimmed = ProgressMerge.dropLowestChallenge(progress, count = 3)

        assertThat(trimmed.bests.keys).containsAtLeastElementsIn(classicKeys)
        classicKeys.forEach { key ->
            assertThat(trimmed.bests[key]).isEqualTo(progress.bests[key])
        }
    }

    @Test
    fun dropLowestChallengeKeepsLadderAndPlayedTime() {
        val progress = trimFixture()

        val trimmed = ProgressMerge.dropLowestChallenge(progress, count = 3)

        assertThat(trimmed.ladder).isEqualTo(progress.ladder)
        assertThat(trimmed.playedTimeMs).isEqualTo(progress.playedTimeMs)
    }

    /**
     * Challenge level ids are compared as **numbers**, not as text: string ordering would
     * make `"9"` outrank `"17"` and the trim would throw away the player's most recent —
     * and hardest-won — levels while keeping the early ones.
     *
     * The malformed id (`"not-a-level"`) is deliberately in the fixture: it must sort
     * LAST, so a row nothing can parse is dropped only after every well-formed one.
     */
    @Test
    fun dropLowestChallengeRemovesNumericallyLowestFirst() {
        val progress = trimFixture()

        val trimmed = ProgressMerge.dropLowestChallenge(progress, count = 3)

        assertThat(trimmed.bests).hasSize(progress.bests.size - 3)
        assertThat(trimmed.bests.keys).containsNoneOf(
            LevelKey(ProgressMode.CHALLENGE, "2"),
            LevelKey(ProgressMode.CHALLENGE, "9"),
            LevelKey(ProgressMode.CHALLENGE, "17"),
        )
        assertThat(trimmed.bests.keys).containsAtLeast(
            LevelKey(ProgressMode.CHALLENGE, "108"),
            LevelKey(ProgressMode.CHALLENGE, "not-a-level"),
        )
    }

    @Test
    fun dropLowestChallengeBeyondRowCountKeepsClassicOnly() {
        val progress = trimFixture()

        val trimmed = ProgressMerge.dropLowestChallenge(progress, count = progress.bests.size * 2)

        val survivingModes = trimmed.bests.keys.mapTo(mutableSetOf()) { it.mode }

        assertThat(survivingModes).containsExactly(ProgressMode.CLASSIC)
        assertThat(trimmed.ladder).isEqualTo(progress.ladder)
        assertThat(trimmed.playedTimeMs).isEqualTo(progress.playedTimeMs)
    }

    @Test
    fun dropLowestChallengeWithZeroIsIdentity() {
        val progress = trimFixture()

        assertThat(ProgressMerge.dropLowestChallenge(progress, count = 0)).isEqualTo(progress)
        assertThat(ProgressMerge.dropLowestChallenge(progress, count = -5)).isEqualTo(progress)
    }

    // ---- Fixtures -----------------------------------------------------------

    /** Runs [assertion] over [LAW_ITERATIONS] seeded random pairs. */
    private fun forEachPair(assertion: (CloudProgress, CloudProgress) -> Unit) {
        val rnd = Random(LAW_SEED)
        repeat(LAW_ITERATIONS) {
            assertion(randomProgress(rnd), randomProgress(rnd))
        }
    }

    /**
     * A random progress snapshot drawn from a deliberately **narrow** level-id pool, so
     * two independently generated snapshots genuinely collide on keys — a generator with
     * unique ids per side would only ever exercise the union half of the merge and never
     * the per-field max/min half.
     */
    private fun randomProgress(rnd: Random): CloudProgress =
        CloudProgress(
            ladder = rnd.nextInt(ChallengeLevelBounds.MIN, LADDER_CEILING),
            playedTimeMs = rnd.nextLong(ProgressMergeLimits.NO_PLAYED_TIME, PLAYED_TIME_CEILING),
            bests =
                buildMap {
                    repeat(rnd.nextInt(0, MAX_ROWS)) {
                        put(randomKey(rnd), randomBest(rnd))
                    }
                },
        )

    private fun randomKey(rnd: Random): LevelKey =
        LevelKey(
            mode = if (rnd.nextBoolean()) ProgressMode.CLASSIC else ProgressMode.CHALLENGE,
            levelId = rnd.nextInt(ChallengeLevelBounds.MIN, ID_POOL_CEILING).toString(),
        )

    private fun randomBest(rnd: Random): LevelBest =
        LevelBest(
            stars = rnd.nextInt(ProgressMergeLimits.NO_STARS, AchievementThresholds.PERFECT_STARS + 1),
            bestTimeMs = rnd.nextLong(1L, TIME_CEILING_MS),
            bestScore = rnd.nextInt(ProgressMergeLimits.NO_SCORE, SCORE_CEILING),
        )

    /**
     * Five Challenge rows whose ids are ordered differently as text than as numbers
     * (`"2" < "9" < "17" < "108"` numerically, but `"108" < "17" < "2" < "9"` as text),
     * plus one unparseable id and two Classic rows the trim must never touch.
     */
    private fun trimFixture(): CloudProgress =
        CloudProgress(
            ladder = 209,
            playedTimeMs = 7_654_321L,
            bests =
                mapOf(
                    LevelKey(ProgressMode.CHALLENGE, "17") to LevelBest(1, 30_000L, 100),
                    LevelKey(ProgressMode.CHALLENGE, "2") to LevelBest(3, 10_000L, 300),
                    LevelKey(ProgressMode.CHALLENGE, "108") to LevelBest(2, 40_000L, 800),
                    LevelKey(ProgressMode.CHALLENGE, "9") to LevelBest(2, 20_000L, 200),
                    LevelKey(ProgressMode.CHALLENGE, "not-a-level") to LevelBest(1, 50_000L, 50),
                    LevelKey(ProgressMode.CLASSIC, "english33") to LevelBest(3, 60_000L, 900),
                    LevelKey(ProgressMode.CLASSIC, "diamond41") to LevelBest(2, 70_000L, 700),
                ),
        )

    private companion object {
        /** Fixed so a law failure is reproducible on the next run, not a one-off flake. */
        const val LAW_SEED = 13L

        /** Samples per law — enough to cover the key-overlap combinatorics, still sub-second. */
        const val LAW_ITERATIONS = 1_000

        /** Exclusive ladder ceiling for generated fixtures; well inside `ChallengeLevelBounds.MAX`. */
        const val LADDER_CEILING = 10_000

        /** Exclusive played-time ceiling, ~11 days in millis — far from `Long` overflow. */
        const val PLAYED_TIME_CEILING = 1_000_000_000L

        /** Exclusive row-count ceiling per generated snapshot. */
        const val MAX_ROWS = 40

        /** Exclusive level-id ceiling: narrow on purpose, so two sides collide on keys. */
        const val ID_POOL_CEILING = 12

        /** Exclusive best-time ceiling in millis; the floor is `1L`, never `0L` (Pitfall 10). */
        const val TIME_CEILING_MS = 600_000L

        /** Exclusive score ceiling for generated fixtures. */
        const val SCORE_CEILING = 5_000
    }
}
