---
phase: 6
slug: platform-foundation-build-hardening
status: active
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-27
validated: 2026-08-27
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Derived from `06-RESEARCH.md` § Validation Architecture.

**This phase's validation problem is unusual: its success criterion is the *absence* of change.** "The tests still pass" is nearly worthless as evidence on its own — they would also pass if half the phase were never implemented. Validation must therefore prove three distinct things, and the plan should treat them as three distinct kinds of task:

1. **Nothing broke** — the existing 299 tests stay green and the release APK still renders downloadable fonts.
2. **The new seams behave as specified** — including the `Unavailable` branch, which has **no prior art anywhere in the codebase** (the stub has always returned `true`, so every non-grant path is unexecuted code).
3. **The new gates actually bite** — a gate that has never been observed to fail is indistinguishable from a gate that cannot fail.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2; Robolectric 4.16 for Compose/Android-runtime tests; ArchUnit 1.3.0 (`archunit-junit4`) for boundary gates |
| **Config file** | `app/build.gradle.kts` (test deps), `build-logic/src/main/kotlin/standards.gradle.kts` (`check` wiring), `config/detekt/detekt.yml` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*" --tests "*NoOpAdGateway*" --tests "*SnapshotMapper*"` |
| **Full suite command** | `./gradlew check` (detekt + ktlintCheck + dpSpLiteralCheck + `:app:testDebugUnitTest` + `:domain:test`) |
| **Estimated runtime** | ~20–40 s quick (warm) · ~90–120 s full (warm) |
| **Verified baseline** | **299 tests green** — confirmed by a real `./gradlew check` run on 2026-08-27, not read from stale build output |

---

## Sampling Rate

- **After every task commit:** `./gradlew :app:testDebugUnitTest --tests "<touched area>"` (~20–40 s warm)
- **After every plan wave:** `./gradlew check` — must include detekt, ktlint, dpSpLiteralCheck, both ArchUnit classes, and all 299+ tests
- **After any `app/build.gradle.kts` or manifest change:** additionally `./gradlew clean :app:assembleRelease`, then re-check APK size and the `aapt2` font-cert grep
- **Before `/gsd:verify-work`:** full suite green **and** `./gradlew check --offline` green
- **Max feedback latency:** ~120 seconds

**Offline contract (D-18):** `./gradlew check --offline` is the enforced invariant. `assembleRelease` is **not** offline-safe until `org.jetbrains.kotlin:compose-group-mapping:2.3.20` is cached — AGP 9.1.0's `produceReleaseComposeMapping` requires it and it is absent from the local cache. Each machine and CI runner needs one warm online run.

---

## Per-Task Verification Map

| Requirement | Behavior | Test Type | Automated Command | File Exists |
|-------------|----------|-----------|-------------------|-------------|
| PLAT-01 | Resumed Activity is published; pausing that Activity clears it; pausing a *different* one does not | unit | `--tests "*ForegroundActivityProviderTest"` | ❌ W0 |
| PLAT-01 | `awaitActivity()` suspends until an Activity resumes, then returns it | unit (`runTest` + Turbine) | same | ❌ W0 |
| PLAT-01 | No class outside `..platform..` (except `App`) depends on `ForegroundActivityProvider`; `ui`/`domain`/`data` never depend on `android.app.Activity` | arch | `--tests "*PlatformBoundaryTest"` | ❌ W0 |
| PLAT-02 | `NoOpAdGateway.requestRewarded` returns `Granted` for every `AdReason` | unit | `--tests "*NoOpAdGatewayTest"` | ✅ rewrite assertions |
| PLAT-02 | `onUndo` **proceeds** on `Unavailable` (ADS-03) and aborts on `Declined` | unit | `--tests "*GameViewModelTest"` | ✅ extend |
| PLAT-02 | `onHintRequested` proceeds on `Unavailable`; on `Declined` clears `solverThinking` and does not run the solver | unit | `--tests "*GameViewModelTest"` | ✅ extend |
| PLAT-02 | End-to-end hint/undo behavior through the Compose UI is byte-identical to v1.0 | Robolectric UI | `--tests "*HintControlTest" --tests "*UndoControlTest" --tests "*GameFlowTest"` | ✅ **must stay green unmodified** |
| PLAT-03 | `NoOpGamesService`: `signIn()` false, session `SignedOut`, every new method a silent no-op | unit | `--tests "*NoOpGamesServiceTest"` | ✅ extend |
| PLAT-04 | No class outside `..platform..` depends on `com.google.android.gms..` / `.ump..` / `.libraries.ads..` | arch | `--tests "*PlatformBoundaryTest"` | ❌ W0 |
| PLAT-04 | **The rule fails when violated** | arch-negative | `--tests "*PlatformBoundaryGateTest"` | ❌ W0 |
| PLAT-05 | Release build minifies and shrinks successfully | build | `./gradlew clean :app:assembleRelease` | n/a |
| PLAT-05 | Downloadable-font cert arrays survive `shrinkResources` | build + tool | `aapt2 dump resources <apk> \| grep fonts_certs` (expect 3 entries) | n/a |
| PLAT-05 | R8 keep-rule set is minimal | build | assert `missing_rules.txt` empty or absent after a successful `assembleRelease` | n/a |
| PLAT-05 | APK size recorded for D-12 | build | pre-R8 pre-SDK **9,890,332 B**; post-R8 pre-SDK **1,518,323 B** (both measured); post-R8 with-SDKs **new** | n/a |
| PLAT-06 | Debug `BuildConfig` ad unit IDs are Google test IDs (`ca-app-pub-3940256099942544/…`) | unit | `--tests "*BuildConfigIdsTest"` | ❌ W0 |
| PLAT-06 | Release resolution **fails** on missing/placeholder ID; **succeeds** when present | unit (pure fn in `build-logic`) | `./gradlew -p build-logic test` | ❌ W0 |
| PLAT-06 | No ad-unit-ID literal anywhere in `app/src/main` | grep gate | `! grep -rn "ca-app-pub-" app/src/main` | ❌ W0 |
| PLAT-07 | `PlayGamesInitProvider` absent from the merged manifest | Robolectric | `--tests "*ManifestSurfaceTest"` | ❌ W0 |
| PLAT-07 | `INTERNET` is declared | Robolectric | `--tests "*ManifestSurfaceTest"` | ❌ W0 |
| PLAT-07 | Whole suite green with SDK artifacts on the graph | suite | `./gradlew check` | ✅ |
| PLAT-07 | Whole suite green **offline** | suite | `./gradlew check --offline` | ✅ |
| DEBT-04 / WARN-02 | Forged coordinates (`-5`, `2^30`, `64`) rejected at `decodeOrNull`; `63` accepted; a valid English-33 snapshot still decodes | unit | `--tests "*SnapshotMapperTest"` | ✅ extend |
| DEBT-04 / WARN-03 | `challengeLevel` clamped for out-of-range route args; recorded `levelId` and `onNextLevel` cannot exceed the bound | unit | `--tests "*GameViewModelTest" --tests "*ResultsViewModelTest"` | ✅ extend |

---

## Proving the Gates Actually Bite

**This is the part the planner must not skip.** It is why the phase needs a `PlatformBoundaryGateTest` in addition to `PlatformBoundaryTest`.

**Technique:** evaluate the *production* `ArchRule` against a deliberately-violating fixture package (`com.app.pegsolitaire.archfixture`) and assert it throws. ArchUnit rules are ordinary objects, so the same rule can be run against a hand-picked import scope. `PlatformBoundaryTest` cannot see the fixture because it imports with `ImportOption.DoNotIncludeTests`, so the production gate stays green while the negative test proves it has teeth.

Constraints the planner must budget for:

- **Keep the two rule definitions from drifting.** Extract each `ArchRule` into a shared `internal object PlatformBoundaryRules` in `src/test/`, referenced by both the `@ArchTest` fields and the negative test. Two copies of a rule is how a gate quietly stops matching what it claims to check.
- **The fixture must reference a real gms class**, so `play-services-games-v2` must be on the test classpath — meaning the negative test lands in the *same plan* that adds the dependency (D-14 makes this possible).
- **`ImportOption.DoNotIncludeTests` must actually exclude the fixture.** ArchUnit's heuristic is path-based and `app/src/test` matches, but confirm on first run — observing `PlatformBoundaryTest` stay green *after* the fixture is added is itself part of the evidence (assumption A5).

For build-side gates the principle holds but the mechanism differs:

- **D-09's release-ID failure cannot be a `:app` JUnit test** — AGP 9.1.0 only creates a unit-test component for the *tested* build type, so there is no `testReleaseUnitTest`. Extract the resolution into a pure function in `build-logic` and unit-test it there, including "release + missing → failure naming the key". `build-logic` is an included build with **no test source set at all** today; either wire root `check` to it or state explicitly in VERIFICATION that it runs as a separate command.
- **`shrinkResources` not eating the fonts** is proven by the `aapt2 dump resources` grep; commit the output as evidence.

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProviderTest.kt` — PLAT-01
- [ ] `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryTest.kt` — PLAT-04, PLAT-01
- [ ] `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryGateTest.kt` + `app/src/test/java/com/app/pegsolitaire/archfixture/` — proves the gates bite
- [ ] `app/src/test/java/com/app/pegsolitaire/ManifestSurfaceTest.kt` — PLAT-07
- [ ] `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt` — PLAT-06 (debug side)
- [ ] `build-logic/src/test/kotlin/AdIdResolutionTest.kt` + `testImplementation(libs.junit)` in `build-logic/build.gradle.kts` — PLAT-06 (release-failure side). **`build-logic` currently has no test source set.**
- [ ] `app/proguard-rules.pro` — must exist before `proguardFiles` references it
- [ ] Extend existing: `NoOpAdGatewayTest`, `NoOpGamesServiceTest`, `GameViewModelTest`, `GameViewModelChallengeAdvanceTest`, `GameViewModelResumeTest`, `SnapshotMapperTest`, `ResultsViewModelTest`

No framework install needed — every required library is already in the version catalog.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Release build launches and plays after R8 | PLAT-05 / SC-4 | No signing config exists; `assembleRelease` yields an unsigned, uninstallable APK. Per D-16, debug-sign locally for the smoke test — do **not** commit a `signingConfig` | Build release, sign with the debug key, install, launch, play one level, confirm fonts render |
| Pre-SDK cold-start baseline | D-12 (feeds PLAT-08 in Phase 14) | No benchmark module, no `androidTest` source set, no device tooling | `adb shell am start -W com.app.pegsolitaire/.MainActivity` sampled N times on a fixed device; commit the numbers as a text artifact, honest about precision |
| One warm online run per machine/CI runner | D-18 | `compose-group-mapping` must be cached before `assembleRelease` can run offline | Run `assembleRelease` online once, then verify `check --offline` passes |

---

## Known Assumptions Carried Into Execution

| # | Claim | Risk if wrong | Cheap check |
|---|-------|---------------|-------------|
| A1 | `play-services-games-v2` needs no hand-written keep rules (covered transitively by `play-services-basement`) | A release crash in **Phase 11** only, after PGS is actually called | Re-run a minified release build in Phase 11 with live PGS calls before trusting this |
| A2 | Declaring `buildTypes { debug { … } }` leaves the existing `sourceSets.getByName("debug")` / `mergeDebugAssets → copyRoomSchemas` wiring intact | Room `MigrationTest` fails with "Cannot find the schema file in the assets folder" | Run `--tests "*MigrationTest"` immediately after adding the block |
| A3 | Gating on `gradle.startParameter.taskNames` reliably distinguishes release invocations from IDE sync and debug builds | A fresh clone fails to sync (violating D-08), or a release slips through unvalidated | Note that `assemble` builds *both* variants; test `:app:assembleRelease`, `bundleRelease`, and bare `assemble` |
| A5 | `ImportOption.DoNotIncludeTests` excludes `app/src/test` from the production gate's import | The violating fixture would fail the *production* gate, making the phase unmergeable | Confirm `PlatformBoundaryTest` stays green on the first run after the fixture lands |
| A7 | No cold-start measurement tooling exists | D-12's baseline may be unachievable as an automated task | Accept the manual path above |

*A6 (the 299-test baseline) is **resolved** — verified by a real `./gradlew check` run on 2026-08-27.*

---

## Validation Sign-Off

- [ ] All tasks have an automated verify command or a Wave 0 dependency
- [ ] Sampling continuity: no 3 consecutive tasks without automated verification
- [ ] Both ArchUnit classes present — the production gate **and** the negative gate proving it fails when violated
- [ ] The three APK size numbers recorded (D-12)
- [ ] `check --offline` green after one warm online run
- [ ] Manual-only items above explicitly signed off in `06-VERIFICATION.md`, not left implied
