plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
    id("standards")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

// The shipped recorded-solution tests only REPLAY committed fixtures and need no
// extra memory. ONLY when regenerating those fixtures (`UPDATE_GOLDEN=true`) does
// the one-off authoring search run; give it a larger heap then so normal CI runs
// stay at the default (D-19 — the search never ships).
tasks.withType<Test>().configureEach {
    val regeneratingFixtures =
        System.getenv("UPDATE_GOLDEN")?.equals("true", ignoreCase = true) == true
    if (regeneratingFixtures) {
        maxHeapSize = "4g"
    }
}
