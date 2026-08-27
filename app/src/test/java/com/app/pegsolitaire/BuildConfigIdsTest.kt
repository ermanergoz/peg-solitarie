package com.app.pegsolitaire

import com.app.pegsolitaire.platform.ads.AdIds
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
 * **What this test cannot prove.** It runs against the **debug** variant only. AGP 9.1.0
 * leaves `android.onlyEnableUnitTestForTheTestedBuildType` at `true`, so a unit-test
 * component exists only for the tested build type and `testReleaseUnitTest` does not
 * exist at all — a release-side assertion written here would silently never run. The
 * other half of PLAT-06 — "a missing or placeholder ID fails the build" — is proven by
 * `build-logic/src/test/kotlin/AdIdResolutionTest.kt`, where the resolution logic lives
 * as a Gradle-free pure function for exactly that reason.
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
}

// Google's test-publisher account. Named once here rather than inlined in three
// assertions — and it is the reason `adIdLiteralCheck` scans `src/main` only: a test
// asserting on the test-publisher prefix must be able to spell it.
private const val TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"
