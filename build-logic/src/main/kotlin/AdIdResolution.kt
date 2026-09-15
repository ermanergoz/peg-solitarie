/**
 * The property / environment-variable key names for the four build-time IDs (PLAT-06).
 *
 * One spelling serves both sources deliberately: a `local.properties` key and its CI
 * environment variable are named identically, so the failure message can name a single
 * string and be actionable on either machine.
 */
object AdIdKeys {
    /** AdMob application ID (`ca-app-pub-…~…`). */
    const val APP_ID = "ADMOB_APP_ID"

    /** Rewarded ad unit ID — hint / undo / retry. */
    const val REWARDED = "ADMOB_REWARDED_AD_UNIT_ID"

    /** Interstitial ad unit ID — between levels. */
    const val INTERSTITIAL = "ADMOB_INTERSTITIAL_AD_UNIT_ID"

    /** Play Games Services project ID (numeric; consumed as a `@string` resource). */
    const val PGS_PROJECT_ID = "PGS_PROJECT_ID"

    /** All four, in the order the failure message reports them. */
    val ALL = listOf(APP_ID, REWARDED, INTERSTITIAL, PGS_PROJECT_ID)

    /**
     * A committed placeholder (see `local.properties.template`) is *not* a value.
     * Treating it as one is how a `REPLACE_ME_…` string reaches a store upload.
     */
    const val PLACEHOLDER_PREFIX = "REPLACE_ME"
}

/** The four IDs a build type needs, or the reason a release build cannot proceed. */
sealed interface AdIdResolutionResult {
    /** Every ID resolved — from the environment, `local.properties`, or (debug) defaults. */
    data class Resolved(
        val appId: String,
        val rewarded: String,
        val interstitial: String,
        val gamesProjectId: String,
    ) : AdIdResolutionResult

    /**
     * A release build is missing at least one real ID. [message] names each missing key
     * and how to supply it — the caller wraps it in a `GradleException` (D-09).
     */
    data class Failure(val message: String) : AdIdResolutionResult
}

/**
 * Resolves the build-time ad / Play Games IDs (PLAT-06, D-05 / D-08 / D-09).
 *
 * Resolution order per key: **environment variable** (CI secret) → **`local.properties`**
 * (developer machine, untracked) → **`AdIdDefaults`** (debug only). A value counts as
 * absent when the key is missing, when it is blank, or when it still carries the
 * committed [AdIdKeys.PLACEHOLDER_PREFIX]. On a release invocation an absent value is a
 * hard failure naming the key; on debug it can never fail, because "a fresh clone builds
 * with test IDs and never accidentally serves real ads" is the governing rule of D-08.
 *
 * On debug the three AD IDs stop at the hard-wired [AdIdDefaults] test units and never read
 * the configured value at all (DEF-06-01); [AdIdKeys.PGS_PROJECT_ID] is the one key that
 * keeps the full order, because it cannot serve an ad — see the comment on the debug return.
 *
 * **[resolve] is deliberately Gradle-free** — `Map` in, result out, no `Project`, no
 * `Provider`. That is the whole reason it is a plain `.kt` file here rather than logic
 * inlined in a `.gradle.kts` script the way `standards.gradle.kts`'s `dpSpLiteralCheck`
 * is: AGP 9.1.0 gives `:app` no `testReleaseUnitTest`, so `AdIdResolutionTest` in this
 * included build is the only place the release-failure contract can be proven at all.
 */
object AdIdResolution {
    fun resolve(
        env: Map<String, String>,
        local: Map<String, String>,
        isRelease: Boolean,
    ): AdIdResolutionResult {
        val configured = AdIdKeys.ALL.associateWith { key -> present(env[key]) ?: present(local[key]) }

        if (isRelease) {
            val missing = AdIdKeys.ALL.filter { configured[it] == null }
            if (missing.isNotEmpty()) {
                return AdIdResolutionResult.Failure(failureMessage(missing))
            }
            // Every key is non-null here — the `missing` filter above is the proof.
            fun required(key: String): String =
                configured[key] ?: error("unreachable: $key was reported present")
            return AdIdResolutionResult.Resolved(
                appId = required(AdIdKeys.APP_ID),
                rewarded = required(AdIdKeys.REWARDED),
                interstitial = required(AdIdKeys.INTERSTITIAL),
                gamesProjectId = required(AdIdKeys.PGS_PROJECT_ID),
            )
        }

        // DEF-06-01 (resolved 2026-08-27, narrowed 2026-09-15): the three AD IDs are
        // HARD-WIRED to Google's test units on debug and deliberately ignore `configured`.
        // Honouring a configured ad ID here meant that the moment a developer set real IDs
        // in `local.properties` for a release build, every subsequent debug build on that
        // machine served REAL ads — the exact invalid-traffic pattern that gets an AdMob
        // account suspended non-appealably. D-08's governing sentence is "a fresh clone
        // builds with test IDs and never accidentally serves real ads"; an override makes
        // the second half conditional on developer discipline, so there is no override.
        //
        // `PGS_PROJECT_ID` is EXEMPT, and the original hard-wire covering it was a defect:
        // a Play Games project ID cannot serve an ad, so no AdMob penalty reaches it. All it
        // does is let a debug build sign in to the real Play Games project — which is
        // exactly why the project registers the debug-keystore SHA-1 (Phase 11 D-04). The
        // placeholder shipped instead: debug APKs carried
        // `com.google.android.gms.games.APP_ID` = "0", the backend answered NOT_FOUND for
        // the package, and the SDK reported `isAuthenticated=false` with no error — which
        // the Settings sign-in row renders as a silent user cancel.
        //
        // This is D-17's rule, which `GamesIdResolution` already applies to the 27
        // leaderboard/achievement IDs addressing this same project; the four-key result is
        // no longer split-brain about Play Games. Residual risk is D-17's, unchanged: a
        // developer with real IDs in `local.properties` writes to the REAL leaderboards from
        // a debug build, and pre-publication scores/unlocks are resettable per whitelisted
        // tester through the Management API. Env-over-local and the blank/`REPLACE_ME`-means-
        // absent rule come for free from `configured`, so an unset key still yields the inert
        // placeholder and a fresh clone still builds.
        return AdIdResolutionResult.Resolved(
            appId = AdIdDefaults.TEST_APP_ID,
            rewarded = AdIdDefaults.TEST_REWARDED,
            interstitial = AdIdDefaults.TEST_INTERSTITIAL,
            gamesProjectId = configured[AdIdKeys.PGS_PROJECT_ID] ?: AdIdDefaults.TEST_GAMES_PROJECT_ID,
        )
    }

    /** `null` for absent, blank, or still-a-placeholder; the trimmed value otherwise. */
    private fun present(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith(AdIdKeys.PLACEHOLDER_PREFIX) }

    /**
     * The D-09 message: it must name the exact key, both places it can be set, and the
     * fact that debug needs none of this — a reader hitting it on a fresh clone should
     * not have to open a build file to get unblocked.
     */
    private fun failureMessage(missing: List<String>): String =
        buildString {
            appendLine("Release build requires real ad/Play Games IDs.")
            missing.forEach { key ->
                appendLine("Missing: $key")
                appendLine(
                    "Set it in local.properties (untracked) or as the $key environment variable.",
                )
            }
            append("Debug builds use Google test IDs and need no configuration.")
        }
}
