plugins {
    `kotlin-dsl`
}

// The precompiled convention plugins (kotlin-jvm, android-app) apply these
// third-party plugins by id, so their implementations must be on the classpath.
// Versions come from the shared catalog — nothing is hand-pinned.
dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.composeCompiler.gradlePlugin)
    // Standards gate (consumed by the `standards` convention plugin).
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    // NOTE: KSP + Hilt Gradle plugins are NOT on the convention classpath — they
    // are applied directly in :app via catalog aliases (Hilt needs AGP's android
    // extension realised before it applies; see android-app.gradle.kts).

    // WHY build-logic has a test source set at all (PLAT-06 / D-09):
    // the "a release build fails when a real ad ID is missing" behaviour CANNOT be
    // proven from `:app`. AGP 9.1.0 leaves `android.onlyEnableUnitTestForTheTestedBuildType`
    // at `true`, so a unit-test component exists only for the tested build type
    // (`debug`) — there is no `testReleaseUnitTest`, and a JUnit test in `:app`
    // asserting release-ID behaviour would never run. The resolution logic therefore
    // lives here as a Gradle-free pure function (`AdIdResolution`) and is tested here.
    // Versions come from the shared catalog (build-logic/settings.gradle.kts:15-21).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

// JUnit4 (the project-wide runner, catalog `junit`), stated explicitly rather than
// relying on Gradle's default test framework.
tasks.withType<Test>().configureEach {
    useJUnit()
}
