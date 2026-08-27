import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the PLAT-06 **release** contract: an ad/Play-Games ID that is missing, blank
 * or still a committed placeholder fails a release build with a message that names the
 * exact property (D-09), while a debug build resolves Google's test units with nothing
 * configured at all (D-05/D-08 — "a fresh clone builds with test IDs and never
 * accidentally serves real ads").
 *
 * **Why this test lives in `build-logic` and not in `:app`.** AGP 9.1.0 creates a
 * unit-test component only for the tested build type, so `testReleaseUnitTest` does not
 * exist and a release-side assertion in `:app` would silently never run. The debug half
 * of PLAT-06 is proven by `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt`.
 */
class AdIdResolutionTest {
    @Test
    fun environmentValueWinsOverLocalProperties() {
        val result =
            AdIdResolution.resolve(
                env = releaseIds(suffix = ENV_SUFFIX),
                local = releaseIds(suffix = LOCAL_SUFFIX),
                isRelease = true,
            )

        assertThat(result).isInstanceOf(AdIdResolutionResult.Resolved::class.java)
        assertThat((result as AdIdResolutionResult.Resolved).rewarded)
            .isEqualTo("rewarded$ENV_SUFFIX")
    }

    @Test
    fun debugResolvesGoogleTestIdsWhenNothingIsConfigured() {
        val result =
            AdIdResolution.resolve(env = emptyMap(), local = emptyMap(), isRelease = false)

        assertThat(result).isInstanceOf(AdIdResolutionResult.Resolved::class.java)
        val resolved = result as AdIdResolutionResult.Resolved
        assertThat(resolved.appId).isEqualTo(AdIdDefaults.TEST_APP_ID)
        assertThat(resolved.rewarded).isEqualTo(AdIdDefaults.TEST_REWARDED)
        assertThat(resolved.interstitial).isEqualTo(AdIdDefaults.TEST_INTERSTITIAL)
        assertThat(resolved.gamesProjectId).isEqualTo(AdIdDefaults.TEST_GAMES_PROJECT_ID)
    }

    /**
     * DEF-06-01 (resolved 2026-08-27): debug IGNORES configured IDs entirely.
     *
     * This test previously asserted the opposite — that a `local.properties` value won on
     * debug too. That override meant configuring real IDs for one release build silently
     * armed every later debug build on the machine to serve real ads, which is the
     * invalid-traffic pattern that gets an AdMob account suspended non-appealably. It also
     * directly contradicted `BuildConfigIdsTest`, which asserts the debug `BuildConfig`
     * always carries Google's test publisher prefix. Both now encode the same rule.
     */
    @Test
    fun debugIgnoresConfiguredIdsAndAlwaysUsesGoogleTestUnits() {
        val result =
            AdIdResolution.resolve(
                env = mapOf(AdIdKeys.APP_ID to "ca-app-pub-9999999999999999~1111111111"),
                local = releaseIds(suffix = LOCAL_SUFFIX),
                isRelease = false,
            )

        val resolved = result as AdIdResolutionResult.Resolved
        assertThat(resolved.appId).isEqualTo(AdIdDefaults.TEST_APP_ID)
        assertThat(resolved.rewarded).isEqualTo(AdIdDefaults.TEST_REWARDED)
        assertThat(resolved.interstitial).isEqualTo(AdIdDefaults.TEST_INTERSTITIAL)
        assertThat(resolved.gamesProjectId).isEqualTo(AdIdDefaults.TEST_GAMES_PROJECT_ID)
    }

    @Test
    fun releaseFailureNamesTheMissingKey() {
        val withoutRewarded = releaseIds(suffix = LOCAL_SUFFIX) - AdIdKeys.REWARDED

        val result =
            AdIdResolution.resolve(env = emptyMap(), local = withoutRewarded, isRelease = true)

        assertThat(result).isInstanceOf(AdIdResolutionResult.Failure::class.java)
        assertThat((result as AdIdResolutionResult.Failure).message)
            .contains("ADMOB_REWARDED_AD_UNIT_ID")
    }

    @Test
    fun releaseTreatsBlankAndPlaceholderValuesAsMissing() {
        val blanked =
            releaseIds(suffix = LOCAL_SUFFIX) +
                mapOf(
                    AdIdKeys.APP_ID to "   ",
                    AdIdKeys.INTERSTITIAL to "REPLACE_ME_INTERSTITIAL_AD_UNIT_ID",
                )

        val result = AdIdResolution.resolve(env = emptyMap(), local = blanked, isRelease = true)

        val message = (result as AdIdResolutionResult.Failure).message
        assertThat(message).contains(AdIdKeys.APP_ID)
        assertThat(message).contains(AdIdKeys.INTERSTITIAL)
        assertThat(message).doesNotContain(AdIdKeys.PGS_PROJECT_ID)
    }

    @Test
    fun releaseNeverFallsBackToATestId() {
        val result =
            AdIdResolution.resolve(env = emptyMap(), local = emptyMap(), isRelease = true)

        assertThat(result).isInstanceOf(AdIdResolutionResult.Failure::class.java)
        assertThat((result as AdIdResolutionResult.Failure).message)
            .doesNotContain(AdIdDefaults.TEST_REWARDED)
    }

    @Test
    fun releaseResolvesAllFourConfiguredValues() {
        val result =
            AdIdResolution.resolve(
                env = emptyMap(),
                local = releaseIds(suffix = LOCAL_SUFFIX),
                isRelease = true,
            )

        val resolved = result as AdIdResolutionResult.Resolved
        assertThat(resolved.appId).isEqualTo("appId$LOCAL_SUFFIX")
        assertThat(resolved.rewarded).isEqualTo("rewarded$LOCAL_SUFFIX")
        assertThat(resolved.interstitial).isEqualTo("interstitial$LOCAL_SUFFIX")
        assertThat(resolved.gamesProjectId).isEqualTo("gamesProjectId$LOCAL_SUFFIX")
    }

    private fun releaseIds(suffix: String): Map<String, String> =
        mapOf(
            AdIdKeys.APP_ID to "appId$suffix",
            AdIdKeys.REWARDED to "rewarded$suffix",
            AdIdKeys.INTERSTITIAL to "interstitial$suffix",
            AdIdKeys.PGS_PROJECT_ID to "gamesProjectId$suffix",
        )
}

// Distinguishable per-source markers, so "env wins over local" cannot pass by accident.
private const val ENV_SUFFIX = "-from-env"
private const val LOCAL_SUFFIX = "-from-local"
