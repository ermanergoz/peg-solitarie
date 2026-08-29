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
//
// 8g, not 4g (DEBT-02): the Diamond-41 floor line is produced by
// `BeamSearchAuthoring`, a level-synchronous beam that holds its whole frontier —
// and every candidate expanded from it — resident for the duration of a ply. That
// is memory-bound where the DFS was not. The bump stays INSIDE this conditional, so
// a normal `:domain:test` run neither searches nor reserves the heap.
tasks.withType<Test>().configureEach {
    val regeneratingFixtures =
        System.getenv("UPDATE_GOLDEN")?.equals("true", ignoreCase = true) == true
    if (regeneratingFixtures) {
        maxHeapSize = "8g"
    }
}

// DEBT-01's exhaustive depth sweep (`DepthSweepTest`) gets its OWN task rather than
// living in the default `test` run — Pitfall 5.
//
// The phase 3 and phase 4 validation contracts signed off in plan 08-03 both name
// `./gradlew :domain:test` as the per-commit gate and hold it to seconds latency. A
// sweep of this shape in that source set would violate both: RESEARCH measured the
// PRE-book K=4 sweep at ~6 minutes across four boards (158.6s for European-37 alone),
// dominated by positions that burned the whole 1.5s deadline before answering.
//
// The gate is belt-and-braces. This task sets RUN_SWEEP, and the test body ALSO checks
// it — so the default `test` task still compiles and "runs" the class, but returns
// immediately, while `check` gets the real sweep. Neither half silently disables the
// other: without the property the body is a no-op, and without the task nothing sets it.
val depthSweep =
    tasks.register<Test>("depthSweep") {
        group = "verification"
        description = "Exhaustive depth-K hint sweep over every shipped Classic board (DEBT-01 / D-08)."
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        systemProperty("RUN_SWEEP", "true")
        filter { includeTestsMatching("*DepthSweepTest*") }
        // The sweep's own report is the point of running it; without this the per-board
        // examined counts and timings never reach the console.
        testLogging { showStandardStreams = true }
        // Never skipped as up-to-date: the sweep is a timing measurement against the
        // shipped deadline, so re-running it on unchanged inputs is still meaningful.
        outputs.upToDateWhen { false }
    }

tasks.named("check") {
    dependsOn(depthSweep)
}
