import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the D-17 contract for the 27 Play Games IDs: a **release** invocation missing any
 * leaderboard or achievement ID fails with a message naming every missing key, while a
 * **debug** invocation can never fail — an unset key resolves to the self-describing
 * `UNCONFIGURED-<KEY>` placeholder so a fresh clone with zero configuration still builds,
 * still runs the whole JVM suite, and simply cannot submit.
 *
 * **Why this test lives in `build-logic` and not in `:app`.** AGP 9.1.0 leaves
 * `android.onlyEnableUnitTestForTheTestedBuildType` at `true`, so a unit-test component
 * exists only for the tested build type — `testReleaseUnitTest` does not exist at all and a
 * release-side assertion written in `:app` would silently never run. This is therefore the
 * only place the release contract *can* be proven, exactly as `AdIdResolutionTest` is for the
 * ad IDs (06 D-08). The debug half — that each `GamesIds` accessor reads the field its own
 * key names — is proven by `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt`.
 *
 * Note the deliberate divergence this file pins down: unlike `AdIdResolution`, debug here
 * HONOURS a configured ID. See `GamesIdResolution`'s debug branch for why.
 */
class GamesIdResolutionTest {
    @Test
    fun keyListIsTwentySevenDistinctValidJavaIdentifiers() {
        assertThat(GamesIdKeys.LEADERBOARDS).hasSize(EXPECTED_LEADERBOARDS)
        assertThat(GamesIdKeys.ACHIEVEMENTS).hasSize(EXPECTED_ACHIEVEMENTS)
        assertThat(GamesIdKeys.ALL).hasSize(EXPECTED_TOTAL)
        assertThat(GamesIdKeys.ALL.distinct()).hasSize(EXPECTED_TOTAL)

        // BuildConfig field names are emitted verbatim, so a non-identifier key would break
        // codegen inside generated BuildConfig.java rather than here.
        val keyPattern = Regex("^PGS_(LB|ACH)_[A-Z0-9_]+$")
        GamesIdKeys.ALL.forEach { key ->
            assertThat(keyPattern.matches(key)).isTrue()
        }
    }

    @Test
    fun debugResolvesTheSelfDescribingPlaceholderWhenNothingIsConfigured() {
        val result =
            GamesIdResolution.resolve(env = emptyMap(), local = emptyMap(), isRelease = false)

        assertThat(result).isInstanceOf(GamesIdResolutionResult.Resolved::class.java)
        val ids = (result as GamesIdResolutionResult.Resolved).ids
        assertThat(ids).hasSize(EXPECTED_TOTAL)
        GamesIdKeys.ALL.forEach { key ->
            assertThat(ids[key]).isEqualTo("${GamesIdKeys.UNCONFIGURED_PREFIX}$key")
        }
    }

    @Test
    fun debugHonoursAnIdConfiguredInLocalProperties() {
        val result =
            GamesIdResolution.resolve(
                env = emptyMap(),
                local = mapOf(GamesIdKeys.LB_CHALLENGE_LADDER to "ladder$LOCAL_SUFFIX"),
                isRelease = false,
            )

        val ids = (result as GamesIdResolutionResult.Resolved).ids
        assertThat(ids[GamesIdKeys.LB_CHALLENGE_LADDER]).isEqualTo("ladder$LOCAL_SUFFIX")
        // Every OTHER key still falls back — one configured key does not arm the rest.
        assertThat(ids[GamesIdKeys.ACH_FIRST_SOLVE])
            .isEqualTo("${GamesIdKeys.UNCONFIGURED_PREFIX}${GamesIdKeys.ACH_FIRST_SOLVE}")
    }

    @Test
    fun debugPrefersTheEnvironmentValueOverLocalProperties() {
        val result =
            GamesIdResolution.resolve(
                env = configuredIds(suffix = ENV_SUFFIX),
                local = configuredIds(suffix = LOCAL_SUFFIX),
                isRelease = false,
            )

        val ids = (result as GamesIdResolutionResult.Resolved).ids
        assertThat(ids[GamesIdKeys.LB_CHALLENGE_SCORE])
            .isEqualTo("${GamesIdKeys.LB_CHALLENGE_SCORE}$ENV_SUFFIX")
    }

    @Test
    fun debugTreatsBlankAndPlaceholderValuesAsUnconfiguredAndStillNeverFails() {
        val result =
            GamesIdResolution.resolve(
                env = emptyMap(),
                local =
                    mapOf(
                        GamesIdKeys.LB_CHALLENGE_LADDER to "   ",
                        GamesIdKeys.ACH_FIRST_SOLVE to "REPLACE_ME_PGS_ACH_FIRST_SOLVE",
                    ),
                isRelease = false,
            )

        assertThat(result).isInstanceOf(GamesIdResolutionResult.Resolved::class.java)
        val ids = (result as GamesIdResolutionResult.Resolved).ids
        assertThat(ids[GamesIdKeys.LB_CHALLENGE_LADDER])
            .isEqualTo("${GamesIdKeys.UNCONFIGURED_PREFIX}${GamesIdKeys.LB_CHALLENGE_LADDER}")
        assertThat(ids[GamesIdKeys.ACH_FIRST_SOLVE])
            .isEqualTo("${GamesIdKeys.UNCONFIGURED_PREFIX}${GamesIdKeys.ACH_FIRST_SOLVE}")
    }

    @Test
    fun releaseResolvesAllTwentySevenConfiguredValuesAndCarriesNoPlaceholder() {
        val result =
            GamesIdResolution.resolve(
                env = emptyMap(),
                local = configuredIds(suffix = LOCAL_SUFFIX),
                isRelease = true,
            )

        val ids = (result as GamesIdResolutionResult.Resolved).ids
        assertThat(ids).hasSize(EXPECTED_TOTAL)
        assertThat(ids[GamesIdKeys.ACH_PERFECTIONIST])
            .isEqualTo("${GamesIdKeys.ACH_PERFECTIONIST}$LOCAL_SUFFIX")
        ids.values.forEach { value ->
            assertThat(value).doesNotContain(GamesIdKeys.UNCONFIGURED_PREFIX)
        }
    }

    @Test
    fun releaseFailureNamesEveryMissingKeyAndNoConfiguredKey() {
        val withoutTwo =
            configuredIds(suffix = LOCAL_SUFFIX) -
                setOf(GamesIdKeys.LB_CLASSIC_DIAMOND41, GamesIdKeys.ACH_LADDER_50)

        val result = GamesIdResolution.resolve(emptyMap(), withoutTwo, isRelease = true)

        val message = (result as GamesIdResolutionResult.Failure).message
        assertThat(message).contains(GamesIdKeys.LB_CLASSIC_DIAMOND41)
        assertThat(message).contains(GamesIdKeys.ACH_LADDER_50)
        assertThat(message).doesNotContain(GamesIdKeys.LB_CHALLENGE_LADDER)
        assertThat(message).doesNotContain(GamesIdKeys.ACH_PERFECTIONIST)
    }

    @Test
    fun releaseTreatsBlankPlaceholderAndDebugFallbackValuesAsMissing() {
        val tainted =
            configuredIds(suffix = LOCAL_SUFFIX) +
                mapOf(
                    GamesIdKeys.LB_CHALLENGE_SCORE to "   ",
                    GamesIdKeys.ACH_HINT_FREE to "REPLACE_ME_PGS_ACH_HINT_FREE",
                    // A copy-pasted debug placeholder must not satisfy the release check.
                    GamesIdKeys.ACH_GRAND_TOUR to
                        "${GamesIdKeys.UNCONFIGURED_PREFIX}${GamesIdKeys.ACH_GRAND_TOUR}",
                )

        val result = GamesIdResolution.resolve(emptyMap(), tainted, isRelease = true)

        val message = (result as GamesIdResolutionResult.Failure).message
        assertThat(message).contains(GamesIdKeys.LB_CHALLENGE_SCORE)
        assertThat(message).contains(GamesIdKeys.ACH_HINT_FREE)
        assertThat(message).contains(GamesIdKeys.ACH_GRAND_TOUR)
    }

    @Test
    fun releaseWithNothingConfiguredFailsAndNamesAllTwentySevenKeys() {
        val result = GamesIdResolution.resolve(emptyMap(), emptyMap(), isRelease = true)

        assertThat(result).isInstanceOf(GamesIdResolutionResult.Failure::class.java)
        val message = (result as GamesIdResolutionResult.Failure).message
        GamesIdKeys.ALL.forEach { key ->
            assertThat(message).contains("Missing: $key")
        }
    }

    @Test
    fun releaseFailureMessageNamesBothSourcesAndStatesDebugNeedsNone() {
        val result = GamesIdResolution.resolve(emptyMap(), emptyMap(), isRelease = true)

        val message = (result as GamesIdResolutionResult.Failure).message
        assertThat(message).contains("local.properties")
        assertThat(message).contains("environment variable")
        assertThat(message).contains(GamesIdKeys.UNCONFIGURED_PREFIX)
        assertThat(message).contains("Debug builds")
    }

    /** Every key configured with a source-distinguishing suffix. */
    private fun configuredIds(suffix: String): Map<String, String> =
        GamesIdKeys.ALL.associateWith { key -> "$key$suffix" }
}

// Distinguishable per-source markers, so "env wins over local" cannot pass by accident —
// the same device `AdIdResolutionTest` uses.
private const val ENV_SUFFIX = "-from-env"
private const val LOCAL_SUFFIX = "-from-local"

private const val EXPECTED_LEADERBOARDS = 7
private const val EXPECTED_ACHIEVEMENTS = 20
private const val EXPECTED_TOTAL = 27
