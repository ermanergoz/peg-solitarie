// Explicit import: inside a Gradle Kotlin DSL script the `java` accessor shadows the
// `java` package, so a fully-qualified `java.util.Properties` does not resolve here.
import java.io.File
import java.util.Properties

plugins {
    id("android-app")
    id("standards")
    // Generates the Google AARs' open-source notices into every variant's assets
    // (tech design §13.1 LN-2; build-logic/src/main/kotlin/google-notices.gradle.kts).
    id("google-notices")
    // DI: applied here (not in the convention plugin) so AGP's android extension
    // is realised before Hilt applies. KSP runs Hilt's processor — no KAPT.
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Generates serializers for `@Serializable` type-safe nav routes (NAV-02).
    // Pitfall 6: without applying this plugin, navigating a @Serializable route
    // crashes at runtime — the runtime json artifact alone is not enough.
    alias(libs.plugins.kotlin.serialization)
    // Room Gradle plugin — declares the exported-schema directory (below) as a
    // variant-aware build input/output (Room 2.6+ recommended way; avoids the KSP
    // `room.schemaLocation` apoption-format pitfall).
    alias(libs.plugins.room)
}

// Export the Room schema JSON to a VCS-committed directory (Success Criterion 5).
// Combined with `exportSchema = true` on @Database, KSP writes `schemas/<db>/1.json`
// here — the diffable migration contract validated by the MigrationTestHelper spike.
val roomSchemaDir = "$projectDir/schemas"

room {
    schemaDirectory(roomSchemaDir)
}

// ---- PLAT-06 / D-05 / D-08 / D-09 / D-17: build-time ad + Play Games IDs ------
// The resolution itself lives in `build-logic/src/main/kotlin/AdIdResolution.kt` and its
// sibling `GamesIdResolution.kt` as Gradle-free pure functions so they can be unit-tested
// (AGP 9.1.0 gives :app no `testReleaseUnitTest`, so the release-failure contract is
// unprovable from here). This file only supplies their input maps and acts on the results.
//
// `local.properties` is NOT automatically exposed as Gradle properties — AGP reads only
// `sdk.dir` out of it — so it is parsed by hand. `providers.fileContents` /
// `providers.environmentVariable` are the configuration-cache-safe accessors; a plain
// FileInputStream would work today (CC is off) but would be a future migration blocker.
//
// Read and parsed EXACTLY ONCE, then sliced per key list below: with 4 ad keys plus 27
// Play Games keys (D-17) a second parse would be pure duplicated I/O for the same bytes.
val localProperties: Properties? =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull
        ?.let { text -> Properties().apply { load(text.reader()) } }

val adIdEnv: Map<String, String> =
    AdIdKeys.ALL
        .mapNotNull { key -> providers.environmentVariable(key).orNull?.let { key to it } }
        .toMap()

val adIdLocal: Map<String, String> =
    localProperties
        ?.let { props ->
            AdIdKeys.ALL.mapNotNull { key -> props.getProperty(key)?.let { key to it } }.toMap()
        }
        ?: emptyMap()

// D-17: the same two sources, over the 27 leaderboard/achievement keys. Separate maps
// rather than one merged map so each resolver's failure message names only its own domain.
val gamesIdEnv: Map<String, String> =
    GamesIdKeys.ALL
        .mapNotNull { key -> providers.environmentVariable(key).orNull?.let { key to it } }
        .toMap()

val gamesIdLocal: Map<String, String> =
    localProperties
        ?.let { props ->
            GamesIdKeys.ALL.mapNotNull { key -> props.getProperty(key)?.let { key to it } }.toMap()
        }
        ?: emptyMap()

// Pitfall 3 / D-08. AGP configures EVERY variant on EVERY invocation, so a naive check
// inside `release { }` would fire during `assembleDebug` and during IDE sync — breaking
// "a fresh clone builds successfully". `gradle.startParameter.taskNames` is the one
// signal that distinguishes what the user actually asked for, and it is EMPTY during
// IDE sync, which is exactly the behaviour we want.
//
// The tradeoff, stated plainly: `./gradlew build`, `assemble` or `bundle` on a fresh
// clone with no IDs configured fails with an actionable message, while `assembleDebug`,
// `check`, `test`, `lintRelease`, `detektRelease` and IDE sync never do. The predicate
// itself — which names package a release and which merely analyse one — lives in
// `build-logic/src/main/kotlin/ReleaseInvocation.kt` so it can be unit-tested (`:app` has
// no `testReleaseUnitTest` under AGP 9.1.0); the rationale for the verb-prefix test that
// replaced `contains("elease")` is on that object, and `ReleaseInvocationTest` pins it.
val isReleaseInvocation = ReleaseInvocation.isReleaseInvocation(gradle.startParameter.taskNames)

// The value the release variant carries when configuration ran outside a release
// invocation and nothing was configured. Deliberately not a valid ID and deliberately
// self-describing: if it ever surfaces in a log or an APK, it says what to do about it.
val unresolvedReleaseId = "UNRESOLVED-see-local.properties.template"

// Debug can never fail: with nothing configured it falls back to Google's test units.
val debugIds =
    AdIdResolution.resolve(adIdEnv, adIdLocal, isRelease = false) as AdIdResolutionResult.Resolved

// D-09 is fail-fast, deliberately: a warning in a long Gradle log is what gets missed
// before a store upload, so this throws — the same register as `standards.gradle.kts`'s
// gates and `ClassicLevelSource`'s `requireNotNull`.
val releaseIds =
    when (val resolved = AdIdResolution.resolve(adIdEnv, adIdLocal, isRelease = true)) {
        is AdIdResolutionResult.Resolved -> resolved
        is AdIdResolutionResult.Failure -> {
            if (isReleaseInvocation) {
                throw GradleException(resolved.message)
            }
            // Not a release invocation, so configuration must still succeed. The release
            // variant gets a loud unresolved marker rather than the debug test IDs:
            // silently substituting a test unit is precisely how test IDs reach
            // production. Any invocation the name gate above misses is still caught by
            // `verifyReleaseAdIds` below, before the APK/AAB is packaged.
            AdIdResolutionResult.Resolved(
                appId = unresolvedReleaseId,
                rewarded = unresolvedReleaseId,
                interstitial = unresolvedReleaseId,
                gamesProjectId = unresolvedReleaseId,
            )
        }
    }

// D-17, the Play Games half. Debug can never fail either: with nothing configured every
// key falls back to the inert `UNCONFIGURED-<KEY>` placeholder, so a fresh clone builds.
val debugGamesIds: Map<String, String> =
    (
        GamesIdResolution.resolve(gamesIdEnv, gamesIdLocal, isRelease = false)
            as GamesIdResolutionResult.Resolved
    ).ids

// Same fail-fast shape as `releaseIds` above, and for the same reason: a warning in a long
// Gradle log is what gets missed before a store upload.
val releaseGamesIds: Map<String, String> =
    when (val resolved = GamesIdResolution.resolve(gamesIdEnv, gamesIdLocal, isRelease = true)) {
        is GamesIdResolutionResult.Resolved -> resolved.ids
        is GamesIdResolutionResult.Failure -> {
            if (isReleaseInvocation) {
                throw GradleException(resolved.message)
            }
            // Not a release invocation, so configuration must still succeed. The marker is
            // the loud `UNRESOLVED-…` one shared with the ad IDs, NOT the debug
            // `UNCONFIGURED-…` placeholder: the release variant must never silently inherit
            // a debug value, and `GamesIdResolution.present()` treats an `UNCONFIGURED-`
            // string as absent precisely so a leaked debug placeholder cannot pass the
            // release check. Any invocation the name gate above misses is still caught by
            // `verifyReleaseGamesIds` below, before the APK/AAB is packaged.
            GamesIdKeys.ALL.associateWith { unresolvedReleaseId }
        }
    }

// ---- PLAT-08 / D-16: the release signing credentials -------------------------------
// Until this block existed there was no `signingConfig` in the build at all, deliberately
// (D-16: "a committed debug-keystore shortcut is exactly the kind of thing that survives to
// a store upload"), and `assembleRelease` happily emitted `app-release-unsigned.apk`.
//
// Third source list in this file and the only one with ONE source: the environment. The ad
// and Play Games IDs also accept an untracked `local.properties` entry because they ship
// inside the APK and are not secrets; these four are cryptographic secrets, and the reasoning
// lives on `SigningKeys`. `providers.environmentVariable` for the same configuration-cache
// reason as the maps above.
val signingEnv: Map<String, String> =
    SigningKeys.ALL
        .mapNotNull { key -> providers.environmentVariable(key).orNull?.let { key to it } }
        .toMap()

// Same fail-fast shape as `releaseIds` and `releaseGamesIds`, and for a sharper version of
// the same reason: an unsigned artifact is not a loud failure, it is a *successful* build
// whose output is useless — and "a 'successful' unsigned build is precisely what gets
// mistaken for a release candidate" (14-RELEASE-PLAN.md §4a).
val releaseSigning: SigningResolutionResult.Resolved? =
    when (val resolved = SigningResolution.resolve(signingEnv)) {
        is SigningResolutionResult.Resolved -> resolved
        is SigningResolutionResult.Failure -> {
            if (isReleaseInvocation) {
                throw GradleException(resolved.message)
            }
            // Not a release invocation, so configuration must still succeed — and a debug
            // build, `check`, `test` and IDE sync must never need any of these variables.
            // `null` here leaves the release variant with NO signing config, exactly as
            // before this block existed. There is no fallback to the debug keystore, which
            // is the one substitution D-16 exists to forbid; any invocation the name gate
            // misses is still caught by `verifyReleaseSigning` below, before packaging.
            null
        }
    }

// Execution-time backstop for the same rule. The task-name gate above is a heuristic —
// it cannot be proven exhaustive over every invocation form — so the packaging tasks
// carry a hard dependency that re-runs the identical resolution and fails outright.
val verifyReleaseAdIds =
    tasks.register("verifyReleaseAdIds") {
        group = "verification"
        description = "Fails if a release package would be built without real ad/PGS IDs."
        doLast {
            val result = AdIdResolution.resolve(adIdEnv, adIdLocal, isRelease = true)
            if (result is AdIdResolutionResult.Failure) {
                throw GradleException(result.message)
            }
        }
    }

// Deliberately a SECOND task rather than more `doLast` inside `verifyReleaseAdIds`: each
// failure message then names its own domain, so a reader sees "missing leaderboard IDs"
// rather than one merged list mixing AdMob unit IDs with Play Games IDs.
val verifyReleaseGamesIds =
    tasks.register("verifyReleaseGamesIds") {
        group = "verification"
        description =
            "Fails if a release package would be built without real Play Games leaderboard/achievement IDs."
        doLast {
            val result = GamesIdResolution.resolve(gamesIdEnv, gamesIdLocal, isRelease = true)
            if (result is GamesIdResolutionResult.Failure) {
                throw GradleException(result.message)
            }
        }
    }

// The signing sibling of those two, and it carries a second job they do not have: the four
// variables being SET is not the same as them being RIGHT. This one opens the keystore the
// way the signer will and distinguishes a missing file, a wrong store password, an absent
// alias and a wrong key password — the four mistakes an operator actually makes, each of
// which AGP's own signer reports as one opaque line, and only after a release variant has
// already been assembled.
val verifyReleaseSigning =
    tasks.register("verifyReleaseSigning") {
        group = "verification"
        description = "Fails if a release package would be built without a usable upload keystore."
        doLast {
            val credentials =
                when (val resolved = SigningResolution.resolve(signingEnv)) {
                    is SigningResolutionResult.Resolved -> resolved
                    is SigningResolutionResult.Failure -> throw GradleException(resolved.message)
                }
            val probed = SigningKeystoreProbe.probe(credentials)
            if (probed is SigningKeystoreProbeResult.Failure) {
                throw GradleException(probed.message)
            }
        }
    }

// Two hooks, and the ORDER of the two matters more than the fact that there are two.
//
// `assembleRelease` and `bundleRelease` are LIFECYCLE tasks: a dependency added to them is a
// SIBLING of the package task, and Gradle may run the two in either order. A `-m :app:bundleRelease`
// dry run measured exactly that — both ID checks were listed AFTER `packageReleaseBundle`. With the
// real upload key exported (which is precisely the state the release procedure puts the machine in)
// that window leaves a SIGNED `app-release.aab` carrying `UNRESOLVED-see-local.properties.template`
// in `BuildConfig` on disk after a build that then failed, which is the exact artifact that gets
// uploaded by accident. So all three checks hang off the PACKAGE tasks, where signing and packaging
// actually happen, and the lifecycle hook is kept as the second attachment: `matching {}` is silent
// when a name does not exist, so if a future AGP renames the package tasks the checks still run
// (late) rather than vanishing. Belt and braces, in that order.
tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    dependsOn(verifyReleaseAdIds, verifyReleaseGamesIds, verifyReleaseSigning)
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseAdIds, verifyReleaseGamesIds, verifyReleaseSigning)
}

// Diagnostics, not security, and one line. AGP's own `validateSigningRelease` is a sibling of
// `verifyReleaseSigning`, so on `assembleRelease` it may run first and report a mistyped keystore
// path in its own register — which makes `SigningKeystoreProbe`'s four distinguishable messages
// (missing file, wrong store password, absent alias, wrong key password) effectively unreachable on
// the one command an operator runs. Ordering ours first costs nothing: it produces no artifact and
// changes no signing behaviour.
tasks.matching { it.name == "validateSigningRelease" }.configureEach {
    dependsOn(verifyReleaseSigning)
}

android {
    namespace = "com.app.pegsolitaire"

    defaultConfig {
        // This build UPDATES the already-published listing (decided 2026-08-27):
        // https://play.google.com/store/apps/details?id=com.erman.pegsolitarie
        // The id must match the published app exactly (note the "-arie" spelling) and the
        // versionCode must exceed the published versionCode 2 (versionName "2.0").
        //
        // The code encodes the name: major * 10_000 + minor * 100 + patch, so "3.0.0" is
        // 30_000 and a later "3.1.2" would be 30_102. Chosen by the founder on 2026-09-21
        // over a plain increment, because the code is then readable back as the name and
        // leaves room between releases. It is a one-way door — Play rejects any upload
        // whose code does not exceed the last one — so every future release must follow
        // the same formula rather than reverting to counting.
        // The `namespace` (code packages, BuildConfig) deliberately stays
        // com.app.pegsolitaire — only the store identity changes.
        applicationId = "com.erman.pegsolitarie"
        versionCode = 30_000
        versionName = "3.0.0"
    }

    // Pitfall 1: `BuildFeatures.buildConfig` defaults to FALSE in AGP 8+/9 (verified in
    // gradle-api-9.1.0-sources BuildFeatures.kt:49). Without this flag every build-config
    // field declared below silently generates nothing and `BuildConfig.REWARDED_AD_UNIT_ID`
    // fails to resolve with an "unresolved reference" that survives a clean build — so the
    // flag lands in the same commit as its fields, never after.
    // PLAT-08 / D-16. Created ONLY when all four variables resolved — `releaseSigning` is
    // null on every debug build, every `check`, every IDE sync and every fresh clone, and a
    // config with half its fields filled in is worse than no config at all.
    //
    // Note what is NOT here: no `storeFile` default, no `System.getenv(...) ?: "debug.keystore"`,
    // no committed `.jks`. The release variant is either signed with the founder's upload key
    // or not signed at all; there is no third state for a mistake to hide in.
    signingConfigs {
        releaseSigning?.let { credentials ->
            create("release") {
                storeFile = File(credentials.keystorePath)
                storePassword = credentials.storePassword
                keyAlias = credentials.keyAlias
                keyPassword = credentials.keyPassword
            }
        }
    }

    buildFeatures {
        buildConfig = true
        // Same story, second flag — and one RESEARCH's Pitfall 1 missed. AGP 9.1.0 also
        // gates custom resource values behind a feature flag, but fails LOUDLY rather
        // than silently: "Build Type debug contains custom resource values, but the
        // feature is disabled." Verified by hitting it. Note for anyone counting the
        // custom resource entries below with grep: this flag's name shares their prefix,
        // so a naive count comes back one high.
        resValues = true
    }

    // PLAT-05 / D-11. Placement matters and is load-bearing for the `sourceSets` line
    // below: declaring `debug { }` here CONFIGURES the always-present debug build type
    // rather than creating a new one, so `sourceSets.getByName("debug")` and the
    // `mergeDebugAssets -> copyRoomSchemas` dependency further down keep their meaning.
    // Keeping `buildTypes` immediately above them is what makes that readable to the
    // next author. Verified after adding this block with
    // `./gradlew :app:testDebugUnitTest --tests "*MigrationTest"` — the
    // "Cannot find the schema file in the assets folder" failure mode did not occur.
    //
    // No `gradle.properties` R8 entry is needed: read out of AGP 9.1.0's
    // BooleanOption.kt, `android.enableR8.fullMode` and
    // `android.r8.strictFullModeForKeepRules` BOTH already default to `true`. Writing
    // `android.enableR8.fullMode=false` would be a regression, not documentation.
    //
    // `android.proguard.failOnMissingFiles` also defaults to `true`, so the
    // `file("proguard-rules.pro")` reference below fails the build outright if that
    // file is missing — it is committed alongside this block for exactly that reason.
    buildTypes {
        debug {
            // D-05: Google's published TEST units, always — never a real ad unit in a
            // build a developer or CI can run. Requesting a real unit from a development
            // device is what gets an AdMob account suspended for invalid traffic, and
            // that suspension is non-appealable. These come from `AdIdDefaults` unless
            // the developer explicitly overrides a key, so a fresh clone with no
            // configuration at all still builds and still cannot serve a real ad (D-08).
            //
            // Pitfall 2: the third argument is a Java/Kotlin SOURCE LITERAL, not a value,
            // so a String field needs the embedded quotes. Omitting them produces an
            // unresolved-identifier compile error inside generated `BuildConfig.java`.
            buildConfigField("String", "ADMOB_APP_ID", "\"${debugIds.appId}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${debugIds.rewarded}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${debugIds.interstitial}\"")
            // Why the same values are ALSO emitted as string resources: manifest
            // `meta-data` cannot read `BuildConfig`, the AdMob app ID has to appear in the
            // manifest for UMP, and the Play Games project ID *must* be a `@string`
            // reference because a bare numeric `android:value` is parsed as an integer.
            // BOTH resources are now referenced from AndroidManifest.xml — `admob_app_id`
            // by Phase 9's UMP `meta-data`, `game_services_project_id` by Phase 11's
            // `com.google.android.gms.games.APP_ID` `meta-data` — so the resource shrinker
            // retains them in release. The earlier note that they "may be dropped from
            // release until then" is spent: there is no longer an unreferenced case.
            resValue("string", "admob_app_id", debugIds.appId)
            resValue("string", "game_services_project_id", debugIds.gamesProjectId)

            // D-17: 27 `String` fields whose names are `GamesIdKeys.ALL` verbatim (7
            // leaderboards + 20 achievements), emitted in a loop rather than 27 hand-written
            // lines per build type. Same Pitfall 2 as above — the third argument is a
            // Java/Kotlin SOURCE LITERAL, not a value, so a String field needs the embedded
            // quotes. Unset keys arrive here as `UNCONFIGURED-<KEY>`, which is why a fresh
            // clone with no Play Games configuration still builds and still runs.
            //
            // No `resValue` for any of the 27, deliberately: nothing in the manifest or any
            // layout reads a leaderboard or achievement ID, and an unreferenced string
            // resource is dead weight the shrinker has to reason about. The
            // `game_services_project_id` `resValue` above exists ONLY because manifest
            // `meta-data` cannot read `BuildConfig` — that reasoning does not transfer.
            debugGamesIds.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
        }
        release {
            // R8 (full mode, per the note above) plus the resource shrinker. The
            // measured effect on this dependency set is 9,890,332 B -> 1,518,323 B
            // (-84.6%); the ~5.5 MB ads SDK and its transitives land in plan 06-09, so
            // minification has to exist before they arrive.
            isMinifyEnabled = true
            // `res/font` is referenced from code through `R.font`, so the resource shrinker keeps it.
            isShrinkResources = true
            // `proguard-android-optimize.txt`, NOT the plain variant: the optimizing
            // baseline is AGP's current recommendation and is the maintained rule set,
            // so nothing in this repo has to re-derive it by hand.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro"),
            )
            // PLAT-08, the Phase 14 half of D-16. `findByName` rather than `getByName`:
            // the config above exists only when all four PLAY_UPLOAD_* variables were
            // exported, so this is null for every debug build, every `check` and every
            // fresh clone — and a null `signingConfig` is precisely the pre-Phase-14
            // behaviour (`app-release-unsigned.apk`), which is what keeps a release-named
            // invocation the ONLY thing these variables gate.
            //
            // A release invocation can never reach this line unsigned: `releaseSigning`
            // threw at configuration time, and `verifyReleaseSigning` re-checks — and
            // additionally opens the keystore — before the package task runs.
            signingConfig = signingConfigs.findByName("release")

            // D-08: the real IDs, resolved from an untracked `local.properties` or a
            // same-named CI environment variable — never from a committed file. Reaching
            // this line with anything unresolved on a release invocation is impossible:
            // the resolver threw above.
            buildConfigField("String", "ADMOB_APP_ID", "\"${releaseIds.appId}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${releaseIds.rewarded}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${releaseIds.interstitial}\"")
            resValue("string", "admob_app_id", releaseIds.appId)
            resValue("string", "game_services_project_id", releaseIds.gamesProjectId)

            // D-17: the same 27 `String` fields, named `GamesIdKeys.ALL` verbatim, from the
            // real values. Same Pitfall 2 — the third argument is a source literal needing
            // embedded quotes. Reaching this line with anything unresolved on a release
            // invocation is impossible: `releaseGamesIds` threw above, and
            // `verifyReleaseGamesIds` re-checks at packaging time. No `resValue` here for
            // the same reason as debug: nothing outside Kotlin reads these IDs.
            releaseGamesIds.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
        }
    }

    // Put the exported Room schema on the DEBUG variant's assets so Robolectric's
    // MigrationTestHelper can resolve `<db-class>/1.json` JVM-side (this project has
    // no `androidTest` source set, so the migration test runs in `test/`).
    //
    // RESEARCH Pitfall 1 proposed the `test` source set — that does NOT work: AGP
    // points Robolectric at the MAIN variant's merged assets (the generated
    // `test_config.properties` sets `android_merged_assets=…/mergeDebugAssets`), so a
    // test-only asset srcDir is never on the path and every MigrationTestHelper
    // constructor — including the `File`-based one, whose `file` argument is the
    // *database* file, not the schema dir — fails with "Cannot find the schema file in
    // the assets folder". `debug` is the narrowest source set Robolectric can actually
    // see; release APKs never carry the schema JSON.
    sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)
}

// The schema JSON is produced by KSP and copied into `schemas/` by the Room plugin's
// `copyRoomSchemas` task, so the asset merge above must run after it (Gradle otherwise
// reports consuming another task's output without a declared dependency).
tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn("copyRoomSchemas")
}

// P5 (docs/tech/audio-silent-mode.md §2.3). `AudioCreditsTest` reads docs/audio/CREDITS.md and
// audio/recipes/ straight from the working tree, outside every source set, so Gradle did not
// know the tests depend on them: an edit to the credits table alone left `testDebugUnitTest`
// UP-TO-DATE (or served it FROM-CACHE), and the provenance gate never saw the edit it exists to
// police. Declared here as inputs of every unit-test task, so such an edit re-runs the tests.
// `res/raw` needs no line of its own: the packaged test resources already carry it.
val audioProvenanceInputs =
    files(
        rootProject.layout.projectDirectory.file("docs/audio/CREDITS.md"),
        rootProject.layout.projectDirectory.dir("audio/recipes"),
    )

tasks.withType<Test>().configureEach {
    inputs
        .files(audioProvenanceInputs)
        .withPropertyName("audioProvenance")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// TutorialSourceGuardTest reads the tutorial's own sources (D-01j(2), WT-16's grep), comments
// included, and a comment-only edit there may change no class file the test task watches. Declared
// as an input of every unit-test task, like the audio files above, so such an edit re-runs it.
val tutorialSourceGuardInputs = files(layout.projectDirectory.dir("src/main/java/com/app/pegsolitaire/ui/onboarding"))

tasks.withType<Test>().configureEach {
    inputs
        .files(tutorialSourceGuardInputs)
        .withPropertyName("tutorialSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Pin androidx.core to the highest line whose minCompileSdk is 36. The default
// resolved version (1.19.0) requires compileSdk 37, which is not installable in
// this environment. Version comes from the catalog — no hand-pinned literal here.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.core" &&
            (requested.name == "core" || requested.name == "core-ktx")
        ) {
            useVersion(libs.versions.androidxCore.get())
            because("core 1.19.0 needs compileSdk 37; pin to 36-compatible line")
        }
        // androidx.lifecycle is an atomic group; hilt-navigation-compose can pull a
        // newer line (2.11.x) whose `-compose` variants require compileSdk 37. Force
        // the whole group to the 36-compatible line the Compose BOM already resolves.
        if (requested.group == "androidx.lifecycle") {
            useVersion(libs.versions.androidxLifecycle.get())
            because("lifecycle 2.11.x -compose variants need compileSdk 37; pin to 36-compatible line")
        }
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose)
    // Curated Material Symbols set — only Icons.Rounded.Refresh is used (top-bar restart).
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // MVVM: ViewModel base for the StateFlow-driven UiState. Non-Compose artifact
    // — the `-compose` lifecycle variants at 2.11.0 demand compileSdk 37.
    implementation(libs.androidx.lifecycle.viewmodel)
    // Lifecycle-aware StateFlow collection in the route (collectAsStateWithLifecycle).
    implementation(libs.androidx.lifecycle.runtime.compose)
    // ProcessLifecycleOwner: whole-app foreground/background for the interstitial session
    // boundary (D-16). Inside the atomic group the resolution strategy above already pins.
    implementation(libs.androidx.lifecycle.process)

    // DI: Hilt runtime + annotation processor under KSP (no KAPT).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Dagger 2.60 stopped exporting error_prone_annotations transitively, but its
    // generated members-injectors still emit `@CanIgnoreReturnValue`. The moment any
    // class gains an `@Inject` field (App, for the ForegroundActivityProvider) the
    // generated SingletonC stops compiling without it. CLASS-retention annotations,
    // so compileOnly is enough — nothing is added to the APK.
    compileOnly(libs.errorprone.annotations)
    // Kept on the graph as it was. Its own hiltViewModel() is a deprecated forwarder to the
    // artifact below, which is where the routes now import hiltViewModel() from (design §9.8).
    implementation(libs.hilt.navigation.compose)
    // hiltViewModel() for scoping each screen's ViewModel to its route. Same version ref as
    // hilt-navigation-compose, and already on the runtime classpath through it.
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Navigation: type-safe NavHost between Home / Catalog / Gameplay / Results
    // (NAV-02). kept on 2.9.8 (stable) — 2.10.x needs compileSdk 37. The
    // serialization runtime backs the `@Serializable` route classes.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    // Persistence: DataStore Preferences for the first-run onboardingSeen flag (ONB-01).
    implementation(libs.androidx.datastore.preferences)

    // Persistence: Room (Phase 5) — results + resume snapshot. Compiler runs under
    // KSP (never KAPT); room-ktx gives suspend/Flow DAOs + withTransaction.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---- Slice-2 SDK artifacts (D-14) ------------------------------------
    // On the dependency graph in THIS phase, referenced by NO code in it. That
    // combination is deliberate and each half earns its keep:
    //  - PLAT-07's provider-removal test is vacuous without play-services-games-v2
    //    on the graph — there is no PlayGamesInitProvider to fail to remove.
    //  - PLAT-04's violating fixture needs a REAL com.google.android.gms class to
    //    name; a locally-declared decoy in a com.google.android.gms.fake package
    //    would prove only that the rule matches strings we wrote ourselves.
    //  - PLAT-05's keep-rule surface is only measured once the SDKs that publish
    //    (or, for PGS, fail to publish) consumer rules are actually shrunk.
    // `implementation`, not `compileOnly`: compileOnly puts nothing on the runtime
    // or unit-test classpath and contributes no library manifest, which would
    // defeat both of the first two points above.
    // Real usage arrives in phases 9 (ads/UMP) and 11+ (Play Games), behind the
    // existing AdGateway / GamesService seams and inside ..platform.. — which
    // PlatformBoundaryTest enforces from the moment these lines land.
    implementation(libs.ads.mobile.sdk)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.services.games.v2)
    // Phase 11: the official Task<T> -> suspend bridge for the Play Games v2 calls made by
    // platform/games/PlayGamesService. Not hand-rolled with suspendCancellableCoroutine
    // (the pattern UmpConsentGateway uses) because every PGS v2 entry point this project
    // touches already returns a Task, and the official adapter is version-locked to the
    // catalog's coroutines pin.
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // StateFlow emission assertions + virtual-time coroutines for the ViewModel tests.
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    // QUAL-01 named domain-purity boundary gate (bytecode-based).
    testImplementation(libs.archunit.junit4)
    // Spies/mocks for collaborator-call assertions (e.g. proving the resume restore
    // replays stored moves rather than regenerating a board). Catalog-pinned.
    testImplementation(libs.mockk)
    // End-to-end Compose UI test (GameFlowTest), run JVM-side via Robolectric.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    // Room MigrationTestHelper for the JVM-side (Robolectric) migration-safety spike.
    testImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
