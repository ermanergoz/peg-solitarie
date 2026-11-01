/**
 * The property / environment-variable key names for the 27 Play Games IDs (D-17 / PLAT-06):
 * 7 leaderboards and 20 achievements, minted in the Play Console and carried into
 * `BuildConfig` by `app/build.gradle.kts`.
 *
 * Each constant is named after its key with the `PGS_` prefix stripped, and each value is
 * emitted **verbatim** as a generated `BuildConfig` field name — which is why every key must
 * be a valid Java identifier (`^PGS_(LB|ACH)_[A-Z0-9_]+$`, asserted by `GamesIdResolutionTest`).
 *
 * One spelling serves both sources deliberately, exactly as [AdIdKeys] does: a
 * `local.properties` key and its CI environment variable are named identically, so a failure
 * message can name a single string and be actionable on either machine.
 */
object GamesIdKeys {
    // ---- Leaderboards (7) — PGSL-01, PGSL-02 / D-07, PGSL-03 --------------------
    // Two Challenge boards (highest ladder level; cumulative SUM(bestScore)) plus one
    // per-board time/score board for each of the five Classic ids.

    /** Highest Challenge level reached — the ladder board (PGSL-01). */
    const val LB_CHALLENGE_LADDER = "PGS_LB_CHALLENGE_LADDER"

    /** Cumulative Challenge score, `SUM(bestScore)` where `mode = challenge` (PGSL-02, D-07). */
    const val LB_CHALLENGE_SCORE = "PGS_LB_CHALLENGE_SCORE"

    /** Per-board Classic leaderboard: English-33 (PGSL-03). */
    const val LB_CLASSIC_ENGLISH33 = "PGS_LB_CLASSIC_ENGLISH33"

    /** Per-board Classic leaderboard: European-37 (PGSL-03). */
    const val LB_CLASSIC_EUROPEAN37 = "PGS_LB_CLASSIC_EUROPEAN37"

    /** Per-board Classic leaderboard: Asymmetric-39 (PGSL-03). */
    const val LB_CLASSIC_ASYMMETRIC39 = "PGS_LB_CLASSIC_ASYMMETRIC39"

    /** Per-board Classic leaderboard: Diamond-41 (PGSL-03). */
    const val LB_CLASSIC_DIAMOND41 = "PGS_LB_CLASSIC_DIAMOND41"

    /** Per-board Classic leaderboard: Wiegleb-45 (PGSL-03). */
    const val LB_CLASSIC_WIEGLEB45 = "PGS_LB_CLASSIC_WIEGLEB45"

    // ---- Achievements (20) ------------------------------------------------------
    // The roster is fixed by CONTEXT D-01..D-04 and totals 20 (D-05). Plan 12-02's
    // `:domain` `AchievementId` enum and plan 12-08's Play Console checklist both key
    // off THIS list, so the grouping comments below are the cross-reference.

    // D-02: the four first-hour early wins (the fourth is the first per-board unlock).

    /** First solved level in either mode (D-02). */
    const val ACH_FIRST_SOLVE = "PGS_ACH_FIRST_SOLVE"

    /** First 3-star result in either mode (D-02). */
    const val ACH_FIRST_THREE_STAR = "PGS_ACH_FIRST_THREE_STAR"

    /** Reached Challenge level 5 — one-shot (D-02). */
    const val ACH_CHALLENGE_LEVEL_5 = "PGS_ACH_CHALLENGE_LEVEL_5"

    // D-01 / D-04: the skill one-shots.

    /** Any level solved in under 60 seconds of elapsed time (D-04). */
    const val ACH_SPEED_SOLVE = "PGS_ACH_SPEED_SOLVE"

    /** A level solved with no hint used, either mode (D-01). */
    const val ACH_HINT_FREE = "PGS_ACH_HINT_FREE"

    /** A level solved with no undo used, either mode (D-01). */
    const val ACH_UNDO_FREE = "PGS_ACH_UNDO_FREE"

    /** English-33 solved with the last peg on the centre hole `(3,3)` (D-01). */
    const val ACH_CENTER_FINISH = "PGS_ACH_CENTER_FINISH"

    // D-04: the five per-board Classic first-solve unlocks.

    /** First stored result on English-33 (D-04). */
    const val ACH_CLEARED_ENGLISH33 = "PGS_ACH_CLEARED_ENGLISH33"

    /** First stored result on European-37 (D-04). */
    const val ACH_CLEARED_EUROPEAN37 = "PGS_ACH_CLEARED_EUROPEAN37"

    /** First stored result on Asymmetric-39 (D-04). */
    const val ACH_CLEARED_ASYMMETRIC39 = "PGS_ACH_CLEARED_ASYMMETRIC39"

    /** First stored result on Diamond-41 (D-04). */
    const val ACH_CLEARED_DIAMOND41 = "PGS_ACH_CLEARED_DIAMOND41"

    /** First stored result on Wiegleb-45 (D-04). */
    const val ACH_CLEARED_WIEGLEB45 = "PGS_ACH_CLEARED_WIEGLEB45"

    // D-03: two tiered axes, three incremental achievements each, absolute steps.

    /** Ladder tier — Challenge level 10, incremental (D-03). */
    const val ACH_LADDER_10 = "PGS_ACH_LADDER_10"

    /** Ladder tier — Challenge level 50, incremental (D-03). */
    const val ACH_LADDER_50 = "PGS_ACH_LADDER_50"

    /** Ladder tier — Challenge level 100, incremental (D-03). */
    const val ACH_LADDER_100 = "PGS_ACH_LADDER_100"

    /** Star tier — 25 total stars across both modes, incremental (D-03). */
    const val ACH_STARS_25 = "PGS_ACH_STARS_25"

    /** Star tier — 100 total stars across both modes, incremental (D-03). */
    const val ACH_STARS_100 = "PGS_ACH_STARS_100"

    /** Star tier — 250 total stars across both modes, incremental (D-03). */
    const val ACH_STARS_250 = "PGS_ACH_STARS_250"

    // D-04: the two completionist one-shots.

    /** A stored result exists on every one of the five Classic boards (D-04). */
    const val ACH_GRAND_TOUR = "PGS_ACH_GRAND_TOUR"

    /** Every Classic board's stored best is at that board's minimum-peg finish (D-04). */
    const val ACH_PERFECTIONIST = "PGS_ACH_PERFECTIONIST"

    /** The 7 leaderboard keys, in the order the failure message reports them. */
    val LEADERBOARDS =
        listOf(
            LB_CHALLENGE_LADDER,
            LB_CHALLENGE_SCORE,
            LB_CLASSIC_ENGLISH33,
            LB_CLASSIC_EUROPEAN37,
            LB_CLASSIC_ASYMMETRIC39,
            LB_CLASSIC_DIAMOND41,
            LB_CLASSIC_WIEGLEB45,
        )

    /** The 20 achievement keys, in the order the failure message reports them. */
    val ACHIEVEMENTS =
        listOf(
            ACH_FIRST_SOLVE,
            ACH_FIRST_THREE_STAR,
            ACH_CHALLENGE_LEVEL_5,
            ACH_SPEED_SOLVE,
            ACH_HINT_FREE,
            ACH_UNDO_FREE,
            ACH_CENTER_FINISH,
            ACH_CLEARED_ENGLISH33,
            ACH_CLEARED_EUROPEAN37,
            ACH_CLEARED_ASYMMETRIC39,
            ACH_CLEARED_DIAMOND41,
            ACH_CLEARED_WIEGLEB45,
            ACH_LADDER_10,
            ACH_LADDER_50,
            ACH_LADDER_100,
            ACH_STARS_25,
            ACH_STARS_100,
            ACH_STARS_250,
            ACH_GRAND_TOUR,
            ACH_PERFECTIONIST,
        )

    /** All 27, leaderboards first. This is the list `buildConfigField` iterates. */
    val ALL = LEADERBOARDS + ACHIEVEMENTS

    /**
     * The debug fallback prefix. `UNCONFIGURED-PGS_LB_CHALLENGE_LADDER` is a real, inert
     * string: the SDK rejects it server-side, so an unconfigured debug build runs the whole
     * game and only submission is a no-op. It is deliberately self-describing — if it ever
     * surfaces in a log, it says what is missing.
     *
     * No placeholder prefix is declared here: `REPLACE_ME` has exactly one spelling for the
     * whole build, [AdIdKeys.PLACEHOLDER_PREFIX].
     */
    const val UNCONFIGURED_PREFIX = "UNCONFIGURED-"
}

/** The 27 IDs a build type needs, or the reason a release build cannot proceed. */
sealed interface GamesIdResolutionResult {
    /**
     * Every key resolved — from the environment, `local.properties`, or (debug only) the
     * [GamesIdKeys.UNCONFIGURED_PREFIX] fallback.
     *
     * A `Map` keyed by [GamesIdKeys.ALL] rather than a 27-field data class: the only consumer
     * is a `forEach` in `app/build.gradle.kts` emitting one `buildConfigField` per entry, and
     * `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt` pins the field names
     * on the Kotlin side anyway. A 27-field class would buy no type safety and cost 27 lines
     * of restatement in three places.
     */
    data class Resolved(val ids: Map<String, String>) : GamesIdResolutionResult

    /**
     * A release build is missing at least one real ID. [message] names every missing key and
     * how to supply it — the caller wraps it in a `GradleException` (D-17, mirroring D-09).
     */
    data class Failure(val message: String) : GamesIdResolutionResult
}

/**
 * Resolves the build-time Play Games leaderboard/achievement IDs (D-17 / PLAT-06).
 *
 * **A sibling of [AdIdResolution], deliberately not an extension of it.**
 * [AdIdResolutionResult.Resolved] is a fixed 4-field data class guarding a domain where a
 * leaked or misused value costs a non-appealable AdMob suspension; growing it to 31 fields
 * would bury that contract under Play Games plumbing that carries no such penalty. Same
 * Gradle-free shape (`Map` in, result out — no `Project`, no `Provider`), same reason: AGP
 * 9.1.0 gives `:app` no `testReleaseUnitTest`, so `GamesIdResolutionTest` in this included
 * build is the only place the release-failure contract can be proven at all.
 *
 * Resolution order per key: **environment variable** (CI secret) → **`local.properties`**
 * (developer machine, untracked) → **debug placeholder**. A value counts as absent when the
 * key is missing, when it is blank, when it still carries the committed
 * [AdIdKeys.PLACEHOLDER_PREFIX], or when it is a copy-pasted debug
 * [GamesIdKeys.UNCONFIGURED_PREFIX] placeholder.
 */
object GamesIdResolution {
    fun resolve(
        env: Map<String, String>,
        local: Map<String, String>,
        isRelease: Boolean,
    ): GamesIdResolutionResult {
        val configured = GamesIdKeys.ALL.associateWith { key -> present(env[key]) ?: present(local[key]) }

        if (isRelease) {
            val missing = GamesIdKeys.ALL.filter { configured[it] == null }
            if (missing.isNotEmpty()) {
                return GamesIdResolutionResult.Failure(failureMessage(missing))
            }
            // Every key is non-null here — the `missing` filter above is the proof.
            return GamesIdResolutionResult.Resolved(
                ids =
                    GamesIdKeys.ALL.associateWith { key ->
                        configured[key] ?: error("unreachable: $key was reported present")
                    },
            )
        }

        // D-17 / RESEARCH A3: debug HONOURS a configured ID, which is the one place copying
        // `AdIdResolution` would be wrong. `AdIdResolution` hard-wires debug and ignores
        // configuration because a real ad unit on a development device is invalid traffic and
        // gets an AdMob account suspended non-appealably. Play Games carries no equivalent
        // penalty: Google publishes NO test leaderboard or achievement IDs, the PGS project
        // registers the debug-keystore SHA-1 precisely so debug builds can sign in (Phase 11
        // D-04), and pre-publication unlocks and scores are resettable per whitelisted tester
        // through the Management API (`achievements.reset`, `scores.reset`). Hard-wiring here
        // would make on-device submission untestable forever, which was judged the worse
        // outcome. Residual risk, stated plainly: a developer who fills real IDs into
        // `local.properties` WILL write to the real leaderboards from a debug build.
        return GamesIdResolutionResult.Resolved(
            ids =
                GamesIdKeys.ALL.associateWith { key ->
                    configured[key] ?: "${GamesIdKeys.UNCONFIGURED_PREFIX}$key"
                },
        )
    }

    /**
     * `null` for absent, blank, still-a-committed-placeholder, or still-a-debug-placeholder;
     * the trimmed value otherwise.
     *
     * The [GamesIdKeys.UNCONFIGURED_PREFIX] case is what stops a `UNCONFIGURED-…` string
     * copied out of a debug log or a `BuildConfig` dump from satisfying the release check.
     */
    private fun present(raw: String?): String? =
        raw?.trim()?.takeIf {
            it.isNotEmpty() &&
                !it.startsWith(AdIdKeys.PLACEHOLDER_PREFIX) &&
                !it.startsWith(GamesIdKeys.UNCONFIGURED_PREFIX)
        }

    /**
     * The D-17 message, in the register of [AdIdResolution]'s: it names the exact key, both
     * places it can be set, and the fact that debug needs none of this — a reader hitting it
     * on a fresh clone should not have to open a build file to get unblocked.
     */
    private fun failureMessage(missing: List<String>): String =
        buildString {
            appendLine("Release build requires real Play Games leaderboard/achievement IDs.")
            missing.forEach { key ->
                appendLine("Missing: $key")
                appendLine(
                    "Set it in local.properties (untracked) or as the $key environment variable.",
                )
            }
            append(
                "Debug builds substitute ${GamesIdKeys.UNCONFIGURED_PREFIX}<KEY> and need no configuration.",
            )
        }
}
