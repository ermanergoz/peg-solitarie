plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
