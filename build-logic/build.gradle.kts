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
}
