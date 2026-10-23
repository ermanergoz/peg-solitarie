import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
    id("standards")
    alias(libs.plugins.kover)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

// rules/testing.md: core logic carries >= 90 % line coverage, measured with kover
// (docs/tech-design.md §8.4). The scope is :domain only; :app's coverage is not gated.
// Measured 2026-10-23 before the bound was set: 896 of 905 lines (99.0 %), so the bound is the
// rule's 90 — the design's procedure sets a lower floor only when the measurement is below it.
val minimumLineCoveragePercent = 90

kover {
    currentProject {
        instrumentation {
            // The sweep and the fuzz harness are Test tasks of their own (below). Instrumenting
            // them would put both in koverVerify's graph, and check's coverage step would re-run
            // the 11-minute sweep; coverage is measured on :domain:test alone.
            disabledForTestTasks.addAll("depthSweep", "fuzz")
        }
    }
    reports {
        verify {
            rule {
                minBound(minimumLineCoveragePercent)
            }
        }
    }
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

// rules/puzzle-design.md invariant 8: the fuzz harness (`FuzzHarnessTest`, docs/tech-design.md
// §7.3) gets its own task in `depthSweep`'s shape, for the same reason: `:domain:test` stays the
// seconds-latency per-commit gate and `check` gets the harness. The gate is belt-and-braces like
// the sweep's: this task sets RUN_FUZZ and the test body checks it. Unlike the sweep, the harness
// is a pure function of the code and FUZZ_SEEDS, so it keeps Gradle's normal up-to-date
// semantics with FUZZ_SEEDS declared as an input: a tier change re-runs it, an unchanged build
// does not.
//
// Default FUZZ_SEEDS for a local `check`; chosen by measurement — see the C5 report. CI pins 10,000;
// the release runs FUZZ_SEEDS=100000 (= the whole 10,000-level space, design §7.3).
// Measured 2026-10-23 on a quiet host: the harness at 10,000 took 5.955 s (warm run), inside the
// brief's 60 s budget, so a local `check` is exhaustive and equal to CI (rule 1 of brief §7).
val fuzzSeedsDefault = "10000"
val fuzzSeeds = providers.environmentVariable("FUZZ_SEEDS").orElse(fuzzSeedsDefault)
val fuzz =
    tasks.register<Test>("fuzz") {
        group = "verification"
        description =
            "Generate → verify-certificate → replay → win over FUZZ_SEEDS Challenge levels " +
            "(rules/puzzle-design.md 8)."
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        systemProperty("RUN_FUZZ", "true")
        // Forwarded verbatim; the test validates it, so a non-integer FUZZ_SEEDS fails the run.
        systemProperty("FUZZ_SEEDS", fuzzSeeds.get())
        inputs.property("fuzzSeeds", fuzzSeeds)
        filter { includeTestsMatching("*FuzzHarnessTest*") }
        // The harness's one summary line is the point of running it, as the sweep's report is.
        // FULL puts a failure's own message on the console — the invalid FUZZ_SEEDS value, or
        // the path of build/fuzz-failures.txt — where the default shows only the class.
        testLogging {
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

tasks.named("check") {
    dependsOn(fuzz)
}
