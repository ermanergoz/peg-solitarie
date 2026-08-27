plugins {
    id("android-app")
    id("standards")
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

android {
    namespace = "com.app.pegsolitaire"

    defaultConfig {
        applicationId = "com.app.pegsolitaire"
        versionCode = 1
        versionName = "0.1.0"
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
            // Intentionally empty today. It exists so the block is complete and so the
            // debug-type-is-configured-not-created constraint above is documented at
            // the point of risk. Plan 06-07 adds the per-type build-config and string
            // resource entries (Google's test ad units, D-05) here — together with the
            // build-feature flag that enables BuildConfig generation, which defaults to
            // FALSE in AGP 9.1.0 and so must land in the same commit as its fields.
        }
        release {
            // R8 (full mode, per the note above) plus the resource shrinker. The
            // measured effect on this dependency set is 9,890,332 B -> 1,518,323 B
            // (-84.6%); the ~5.5 MB ads SDK and its transitives land in plan 06-09, so
            // minification has to exist before they arrive.
            isMinifyEnabled = true
            // Safe here despite `font_certs.xml` being an array OF array references
            // (`@array/..._dev` / `..._prod`) reached only as
            // `R.array.com_google_android_gms_fonts_certs` from ui/theme/Type.kt: the
            // shrinker follows resource->resource references, verified against the
            // shrunk APK with `aapt2 dump resources`. If it had not, downloadable
            // Fredoka/Nunito would silently fall back to the default font — the one
            // player-visible thing R8 could plausibly break here.
            isShrinkResources = true
            // `proguard-android-optimize.txt`, NOT the plain variant: the optimizing
            // baseline is AGP's current recommendation and is the maintained rule set,
            // so nothing in this repo has to re-derive it by hand.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro"),
            )
            // No signing is configured here, deliberately (D-16): real release signing
            // is Phase 14 scope, and a committed debug-keystore shortcut is exactly the
            // kind of thing that survives to a store upload. `assembleRelease` therefore
            // emits `app-release-unsigned.apk`; plan 06-10's launch smoke test signs
            // that locally with the debug key without committing anything.
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
    // Downloadable Fredoka/Nunito (Candy-Clay type scale); async with default-font fallback.
    implementation(libs.androidx.compose.ui.text.google.fonts)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // MVVM: ViewModel base for the StateFlow-driven UiState. Non-Compose artifact
    // — the `-compose` lifecycle variants at 2.11.0 demand compileSdk 37.
    implementation(libs.androidx.lifecycle.viewmodel)
    // Lifecycle-aware StateFlow collection in the route (collectAsStateWithLifecycle).
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DI: Hilt runtime + annotation processor under KSP (no KAPT).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Dagger 2.60 stopped exporting error_prone_annotations transitively, but its
    // generated members-injectors still emit `@CanIgnoreReturnValue`. The moment any
    // class gains an `@Inject` field (App, for the ForegroundActivityProvider) the
    // generated SingletonC stops compiling without it. CLASS-retention annotations,
    // so compileOnly is enough — nothing is added to the APK.
    compileOnly(libs.errorprone.annotations)
    // hiltViewModel() for scoping the GameViewModel to the route.
    implementation(libs.hilt.navigation.compose)

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
