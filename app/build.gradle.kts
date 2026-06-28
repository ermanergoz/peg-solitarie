plugins {
    id("android-app")
    id("standards")
    // DI: applied here (not in the convention plugin) so AGP's android extension
    // is realised before Hilt applies. KSP runs Hilt's processor — no KAPT.
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.app.pegsolitaire"

    defaultConfig {
        applicationId = "com.app.pegsolitaire"
        versionCode = 1
        versionName = "0.1.0"
    }
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

    // DI: Hilt runtime + annotation processor under KSP (no KAPT).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // hiltViewModel() for scoping the GameViewModel to the route.
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // StateFlow emission assertions + virtual-time coroutines for the ViewModel tests.
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    // QUAL-01 named domain-purity boundary gate (bytecode-based).
    testImplementation(libs.archunit.junit4)
    // End-to-end Compose UI test (GameFlowTest), run JVM-side via Robolectric.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
