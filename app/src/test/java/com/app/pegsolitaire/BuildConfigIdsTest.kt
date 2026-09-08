package com.app.pegsolitaire

import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.platform.ads.AdIds
import com.app.pegsolitaire.platform.games.GamesIds
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the PLAT-06 **debug** contract: every ad ID this build can request is one of
 * Google's published TEST units (D-05). A real ad unit in a build a developer or CI can
 * run is what gets an AdMob account suspended for invalid traffic, and that suspension
 * is non-appealable — so this is the guard that keeps the whole development loop safe.
 *
 * The assertions go through [AdIds] rather than `BuildConfig` directly, so they also
 * pin the accessor to the right generated fields — misnaming one there would otherwise
 * fail only at Phase 9's first real ad request.
 *
 * **The Play Games half asserts *wiring*, not *values* (D-17).** Unlike an ad ID, a
 * configured debug Play Games ID is entirely legitimate — Google publishes no PGS test
 * IDs and the project registers the debug-keystore SHA-1 precisely so debug builds can
 * sign in and submit — so no assertion here may depend on whether this machine's
 * `local.properties` happens to carry real values. What is asserted instead holds on
 * every machine: the 27 accessors are non-blank, mutually distinct, never resolve a
 * committed `REPLACE_ME` placeholder or the release `UNRESOLVED-…` marker, and — on any
 * unconfigured machine and on CI — each one carries the `UNCONFIGURED-` fallback naming
 * its **own** key, which is the exact accessor-to-generated-field proof.
 *
 * **What this test cannot prove.** It runs against the **debug** variant only. AGP 9.1.0
 * leaves `android.onlyEnableUnitTestForTheTestedBuildType` at `true`, so a unit-test
 * component exists only for the tested build type and `testReleaseUnitTest` does not
 * exist at all — a release-side assertion written here would silently never run. The
 * other half of PLAT-06 — "a missing or placeholder ID fails the build" — is proven by
 * `build-logic/src/test/kotlin/AdIdResolutionTest.kt` and its Play Games sibling
 * `GamesIdResolutionTest.kt`, where the resolution logic lives as a Gradle-free pure
 * function for exactly that reason.
 */
class BuildConfigIdsTest {
    @Test
    fun rewardedAdUnitIsAGoogleTestUnit() {
        assertThat(AdIds.rewardedAdUnitId).startsWith("$TEST_PUBLISHER_PREFIX/")
    }

    @Test
    fun interstitialAdUnitIsAGoogleTestUnit() {
        assertThat(AdIds.interstitialAdUnitId).startsWith("$TEST_PUBLISHER_PREFIX/")
    }

    @Test
    fun admobAppIdIsGooglesSampleAppId() {
        // An AdMob *app* ID uses `~` as its separator; an ad *unit* ID uses `/`.
        assertThat(AdIds.admobAppId).startsWith("$TEST_PUBLISHER_PREFIX~")
    }

    @Test
    fun everyGamesIdAccessorReturnsANonBlankValue() {
        assertThat(gamesIdsByKey).hasSize(EXPECTED_GAMES_ID_COUNT)
        gamesIdsByKey.forEach { (key, value) ->
            assertThat(value.isNotBlank()).isTrue()
            assertThat(key).isNotEmpty()
        }
    }

    @Test
    fun theGamesIdAccessorsAreMutuallyDistinct() {
        // The failure this catches: two accessors pointing at the same BuildConfig field
        // after a copy-paste. It holds whether or not this machine has real IDs.
        assertThat(gamesIdsByKey.values.distinct()).hasSize(EXPECTED_GAMES_ID_COUNT)
    }

    @Test
    fun noGamesIdAccessorResolvesACommittedPlaceholder() {
        gamesIdsByKey.forEach { (_, value) ->
            assertThat(value).doesNotContain(COMMITTED_PLACEHOLDER_PREFIX)
        }
    }

    @Test
    fun noGamesIdAccessorInheritsTheReleaseUnresolvedMarker() {
        // Debug must never pick up the value the RELEASE variant carries when
        // configuration ran outside a release invocation with nothing configured.
        gamesIdsByKey.forEach { (_, value) ->
            assertThat(value).isNotEqualTo(UNRESOLVED_RELEASE_MARKER)
        }
    }

    @Test
    fun anUnconfiguredGamesIdNamesItsOwnBuildConfigKey() {
        // The accessor-to-field wiring proof. Vacuously true on a machine with real IDs
        // configured, which is why the distinctness case above carries that machine.
        gamesIdsByKey.forEach { (key, value) ->
            if (value.startsWith(UNCONFIGURED_PREFIX)) {
                assertThat(value.removePrefix(UNCONFIGURED_PREFIX)).isEqualTo(key)
            }
        }
    }

    @Test
    fun forClassicMapsEveryClassicLevelIdToItsBoardLeaderboard() {
        val leaderboards = GamesIds.Leaderboards
        assertThat(leaderboards.forClassic(ClassicLevelIds.ENGLISH33))
            .isEqualTo(leaderboards.classicEnglish33)
        assertThat(leaderboards.forClassic(ClassicLevelIds.EUROPEAN37))
            .isEqualTo(leaderboards.classicEuropean37)
        assertThat(leaderboards.forClassic(ClassicLevelIds.ASYMMETRIC39))
            .isEqualTo(leaderboards.classicAsymmetric39)
        assertThat(leaderboards.forClassic(ClassicLevelIds.DIAMOND41))
            .isEqualTo(leaderboards.classicDiamond41)
        assertThat(leaderboards.forClassic(ClassicLevelIds.WIEGLEB45))
            .isEqualTo(leaderboards.classicWiegleb45)
    }

    @Test
    fun forClassicReturnsNullForAnythingThatIsNotAClassicBoard() {
        // `null` is the correct, expected answer for every Challenge level — a caller
        // must read it as "skip", not as an error.
        assertThat(GamesIds.Leaderboards.forClassic("challenge-7")).isNull()
        assertThat(GamesIds.Leaderboards.forClassic("")).isNull()
    }
}

// Google's test-publisher account. Named once here rather than inlined in three
// assertions — and it is the reason `adIdLiteralCheck` scans `src/main` only: a test
// asserting on the test-publisher prefix must be able to spell it.
private const val TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

// The debug fallback prefix emitted by `GamesIdResolution` for any unset key.
private const val UNCONFIGURED_PREFIX = "UNCONFIGURED-"

// The marker `app/build.gradle.kts` gives the RELEASE variant outside a release
// invocation. Debug must never carry it.
private const val UNRESOLVED_RELEASE_MARKER = "UNRESOLVED-see-local.properties.template"

// The committed `local.properties.template` placeholder prefix — never a resolved value.
private const val COMMITTED_PLACEHOLDER_PREFIX = "REPLACE_ME"

private const val EXPECTED_GAMES_ID_COUNT = 27

/**
 * Each of the 27 generated `BuildConfig` field names mapped to the value its [GamesIds]
 * accessor returns — the one structure every games assertion above is driven off.
 *
 * Spelling the key names in a **test** is sanctioned by the same reasoning this file
 * already records for [TEST_PUBLISHER_PREFIX]: `gamesIdLiteralCheck` and `adIdLiteralCheck`
 * scan `src/main` only, precisely so a test can name what it asserts on. These are field
 * names, not IDs — no real leaderboard or achievement ID appears here.
 */
private val gamesIdsByKey: Map<String, String> =
    mapOf(
        "PGS_LB_CHALLENGE_LADDER" to GamesIds.Leaderboards.challengeLadder,
        "PGS_LB_CHALLENGE_SCORE" to GamesIds.Leaderboards.challengeScore,
        "PGS_LB_CLASSIC_ENGLISH33" to GamesIds.Leaderboards.classicEnglish33,
        "PGS_LB_CLASSIC_EUROPEAN37" to GamesIds.Leaderboards.classicEuropean37,
        "PGS_LB_CLASSIC_ASYMMETRIC39" to GamesIds.Leaderboards.classicAsymmetric39,
        "PGS_LB_CLASSIC_DIAMOND41" to GamesIds.Leaderboards.classicDiamond41,
        "PGS_LB_CLASSIC_WIEGLEB45" to GamesIds.Leaderboards.classicWiegleb45,
        "PGS_ACH_FIRST_SOLVE" to GamesIds.Achievements.firstSolve,
        "PGS_ACH_FIRST_THREE_STAR" to GamesIds.Achievements.firstThreeStar,
        "PGS_ACH_CHALLENGE_LEVEL_5" to GamesIds.Achievements.challengeLevel5,
        "PGS_ACH_SPEED_SOLVE" to GamesIds.Achievements.speedSolve,
        "PGS_ACH_HINT_FREE" to GamesIds.Achievements.hintFree,
        "PGS_ACH_UNDO_FREE" to GamesIds.Achievements.undoFree,
        "PGS_ACH_CENTER_FINISH" to GamesIds.Achievements.centerFinish,
        "PGS_ACH_CLEARED_ENGLISH33" to GamesIds.Achievements.clearedEnglish33,
        "PGS_ACH_CLEARED_EUROPEAN37" to GamesIds.Achievements.clearedEuropean37,
        "PGS_ACH_CLEARED_ASYMMETRIC39" to GamesIds.Achievements.clearedAsymmetric39,
        "PGS_ACH_CLEARED_DIAMOND41" to GamesIds.Achievements.clearedDiamond41,
        "PGS_ACH_CLEARED_WIEGLEB45" to GamesIds.Achievements.clearedWiegleb45,
        "PGS_ACH_LADDER_10" to GamesIds.Achievements.ladder10,
        "PGS_ACH_LADDER_50" to GamesIds.Achievements.ladder50,
        "PGS_ACH_LADDER_100" to GamesIds.Achievements.ladder100,
        "PGS_ACH_STARS_25" to GamesIds.Achievements.stars25,
        "PGS_ACH_STARS_100" to GamesIds.Achievements.stars100,
        "PGS_ACH_STARS_250" to GamesIds.Achievements.stars250,
        "PGS_ACH_GRAND_TOUR" to GamesIds.Achievements.grandTour,
        "PGS_ACH_PERFECTIONIST" to GamesIds.Achievements.perfectionist,
    )
