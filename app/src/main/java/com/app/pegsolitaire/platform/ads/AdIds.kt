package com.app.pegsolitaire.platform.ads

import com.app.pegsolitaire.BuildConfig

/**
 * The app's single window onto the generated ad-unit identifiers (PLAT-06 / D-06 / D-08).
 *
 * Three things a reader needs to know here:
 *
 * 1. **These are the only `BuildConfig` ad-field readers in the app.** Everything that
 *    needs an ad unit — the Phase 9 AdMob gateway included — goes through this object,
 *    so there is exactly one place to look when an ID is wrong and exactly one place to
 *    change when a field is renamed.
 *
 * 2. **The values differ per build type, so they cannot live in source.** Debug carries
 *    Google's test units; release carries real IDs resolved at build time from an
 *    untracked `local.properties` or a same-named CI environment variable. See
 *    `app/build.gradle.kts` and `build-logic/src/main/kotlin/AdIdResolution.kt`.
 *
 * 3. **Ad IDs are a deliberate, documented exception to the no-magic-strings rule
 *    (QUAL-03).** Where the project's convention would otherwise call for an
 *    `AdConstants` scoped-constants object — the sibling of `ResumeSnapshotFormat` or
 *    `ProgressPersistenceKeys` — writing one here would be actively wrong: it would
 *    hard-code one build type's IDs into every build, and a real unit reaching a
 *    development device is what gets an AdMob account suspended for invalid traffic,
 *    non-appealably. The build gate `adIdLiteralCheck` fails the build on any ad-ID
 *    literal under `src/main` precisely to stop that object from ever being written.
 *
 * The Play Games Services project ID is deliberately **absent** from this object: it is
 * consumed by the manifest, not by code, so it exists only as the generated
 * `game_services_project_id` string resource.
 */
object AdIds {
    /** AdMob application ID (the `~`-separated form). */
    val admobAppId: String get() = BuildConfig.ADMOB_APP_ID

    /** Rewarded ad unit — hint / undo / retry (ADS-03). */
    val rewardedAdUnitId: String get() = BuildConfig.REWARDED_AD_UNIT_ID

    /** Interstitial ad unit — shown between levels. */
    val interstitialAdUnitId: String get() = BuildConfig.INTERSTITIAL_AD_UNIT_ID
}
