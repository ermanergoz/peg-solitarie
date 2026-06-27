plugins {
    id("android-app")
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
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
