plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
    id("standards")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
