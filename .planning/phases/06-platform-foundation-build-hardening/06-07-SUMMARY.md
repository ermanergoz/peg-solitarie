---
phase: 06-platform-foundation-build-hardening
plan: 07
subsystem: infra
tags: [gradle, agp, buildconfig, resvalue, admob, play-games, build-logic, junit, truth]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening (plan 06-03)
    provides: "the `buildTypes { debug { } release { … } }` block, `proguard-rules.pro`, and the R8/shrinker release configuration this plan adds ID fields into"
  - phase: 01-foundation
    provides: "`build-logic/` convention plugins (`standards`, `android-app`) and the shared version catalog wired into the included build"
provides:
  - "`AdIdResolution.resolve(env, local, isRelease)` — a Gradle-free, unit-tested ID resolver in `build-logic`"
  - "`AdIdDefaults` — Google's three published test ad units plus an inert Play Games project ID"
  - "Per-build-type `buildConfigField`/`resValue` ad + Play Games IDs (debug = test units, release = untracked real IDs)"
  - "A configuration-time release failure naming each missing property, plus an execution-time `verifyReleaseAdIds` backstop"
  - "`adIdLiteralCheck` — a `check`-wired gate that fails on any ad-ID literal under `src/main`"
  - "`build-logic`'s first test source set, run by root `./gradlew check`"
  - "`local.properties.template` — the discoverable path from a fresh clone to a release build"
  - "`platform/ads/AdIds.kt` — the app's only `BuildConfig` ad-field reader"
affects: [09-ads-and-consent, 11-play-games-services, 14-release-signing-and-store, ci-pipeline]

# Tech tracking
tech-stack:
  added: [junit-4.13.2 (build-logic test source set), truth-1.4.4 (build-logic test source set)]
  patterns:
    - "Non-trivial build logic lives in a plain `.kt` file under `build-logic/src/main/kotlin/` taking plain-Kotlin inputs, so it can be unit-tested — not inlined in a `.gradle.kts` script"
    - "Included-build `:check` is wired into the root `check` lifecycle so build-logic gates cannot be skipped"
    - "Build-time secrets are resolved env → local.properties → (debug-only) default, with blank and `REPLACE_ME` both counting as absent"

key-files:
  created:
    - build-logic/src/main/kotlin/AdIdDefaults.kt
    - build-logic/src/main/kotlin/AdIdResolution.kt
    - build-logic/src/test/kotlin/AdIdResolutionTest.kt
    - local.properties.template
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt
    - app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt
  modified:
    - build-logic/build.gradle.kts
    - build-logic/src/main/kotlin/standards.gradle.kts
    - build.gradle.kts
    - app/build.gradle.kts

key-decisions:
  - "Ad IDs are a documented exception to QUAL-03's no-magic-strings rule: no `AdConstants` object, only `buildConfigField`/`resValue`, enforced by `adIdLiteralCheck`"
  - "The root `build.gradle.kts` is no longer strictly empty — it applies `base` solely to hang `gradle.includedBuild(\"build-logic\").task(\":check\")` on, because a gate that runs only as a separate command gets skipped"
  - "When configuration runs outside a release invocation with nothing configured, the release variant gets a loud `UNRESOLVED-see-local.properties.template` marker rather than the debug test IDs — silently substituting a test unit is exactly how test IDs reach production"
  - "`assemble`, `build` and `bundle` (bare, or `:app:`-qualified) are treated as release invocations because they build both variants; `bundle` was added beyond the plan's list for symmetry"
  - "`BuildConfigIdsTest` asserts through `AdIds` rather than `BuildConfig` directly, so the accessor's field wiring is pinned too and the RED phase is genuine"

patterns-established:
  - "Pattern: a build gate that must be provable is a pure function in build-logic with a test, not script-inlined logic"
  - "Pattern: comment-aware source scanning (skip `//`, `*`, `<!--`, `#`) so the comment explaining a rule cannot trip the rule"
  - "Pattern: configuration-time fail-fast + execution-time backstop when the configuration-time trigger is a heuristic"

requirements-completed: [PLAT-06]

# Metrics
duration: 16min
completed: 2026-08-27
---

# Phase 06 Plan 07: Build-Config Ad & Play Games IDs Summary

**Every ad-unit and Play Games ID is now a per-build-type `buildConfigField`/`resValue` — Google's test units in debug with zero configuration, real untracked IDs in release, and a configuration-time `GradleException` naming the exact missing property, backed by a Gradle-free resolver with 7 unit tests in `build-logic` and a `check`-wired gate that forbids ad-ID literals in `src/main`.**

## Performance

- **Duration:** ~16 min
- **Started:** 2026-08-27T18:30:35+03:00
- **Completed:** 2026-08-27T18:46:04+03:00
- **Tasks:** 3
- **Files modified:** 10 (6 created, 4 modified)

## Accomplishments

- **The release-failure contract is actually provable.** `AdIdResolution.resolve(env, local, isRelease)` is a pure `Map`-in / sealed-result-out function in `build-logic/src/main/kotlin/`, with **7 green tests** in `build-logic`'s first-ever test source set. This exists because AGP 9.1.0 leaves `android.onlyEnableUnitTestForTheTestedBuildType` at `true`, so `:app` has no `testReleaseUnitTest` and a release-side assertion there would silently never run.
- **A fresh clone builds debug with zero configuration.** Verified: with no ad keys in `local.properties` and no environment variables set, `./gradlew :app:assembleDebug` succeeds and the generated debug `BuildConfig` carries Google's test units.
- **A release build without real IDs fails at configuration time**, naming all four missing keys, both places each can be set, and the fact that debug needs none of it. Verified by running `:app:assembleRelease` with nothing configured. With the four keys present in `local.properties`, `./gradlew clean :app:assembleRelease` succeeds and the release `BuildConfig` carries the configured values.
- **`adIdLiteralCheck` is a real gate, not a comment.** It scans `src/main/{java,kotlin,res}` for `ca-app-pub-`, skips comment lines, and was demonstrated to fail naming `file:line` on a deliberately planted literal (reverted).
- **`build-logic`'s tests now run under root `./gradlew check`** via `gradle.includedBuild("build-logic").task(":check")` — confirmed in task output (`> Task :build-logic:test`).
- App + domain suite: **309 → 312** (baseline preserved, +3 from `BuildConfigIdsTest`); `build-logic` adds 7 more. `./gradlew check` green.

## Task Commits

1. **Task 1: unit-tested ID resolver, source-literal gate, `check` wiring** (TDD)
   - `8eab3e7` (test) — build-logic test source set + failing `AdIdResolutionTest`
   - `a2dcb8f` (feat) — `AdIdDefaults` + Gradle-free `AdIdResolution`
   - `7b28007` (feat) — `adIdLiteralCheck` + root `check` wiring
2. **Task 2: per-build-type IDs, release gate, fresh-clone template**
   - `e5eb873` (feat)
3. **Task 3: platform-only `BuildConfig` accessor and its debug-side test** (TDD)
   - `3b2636d` (test) — failing `BuildConfigIdsTest`
   - `a9fd17e` (feat) — `AdIds`

## Files Created/Modified

- `build-logic/src/main/kotlin/AdIdDefaults.kt` — Google's three verified test units (`ca-app-pub-3940256099942544~/…`) plus `TEST_GAMES_PROJECT_ID = "0"`, with a KDoc noting Google publishes no test Play Games project ID and Phase 11 supplies a real debug value.
- `build-logic/src/main/kotlin/AdIdResolution.kt` — `AdIdKeys` (the four key names, shared by `local.properties` and CI env vars), the `AdIdResolutionResult` sealed type, and the pure `resolve` function. No `Project`, no `Provider`, no Gradle types.
- `build-logic/src/test/kotlin/AdIdResolutionTest.kt` — 7 tests: env-beats-local, debug test-ID fallback, debug override, release failure naming `ADMOB_REWARDED_AD_UNIT_ID`, blank/`REPLACE_ME` treated as absent, release never falls back to a test ID, all four resolved.
- `build-logic/build.gradle.kts` — `testImplementation(libs.junit)` + `libs.truth`, an explicit `useJUnit()`, and a comment recording *why* build-logic has tests at all (Pitfall 9).
- `build-logic/src/main/kotlin/standards.gradle.kts` — new `adIdLiteralCheck` task; `check` now depends on four gates, not three.
- `build.gradle.kts` — applies `base` and wires the included build's `:check` into root `check`; the "intentionally empty" comment now records the exception and its rationale.
- `app/build.gradle.kts` — `buildFeatures { buildConfig; resValues }`, the provider-API input maps, the `startParameter.taskNames` release gate, the `GradleException`, `verifyReleaseAdIds`, and 3 `buildConfigField` + 2 `resValue` entries per build type.
- `local.properties.template` — the four keys with `REPLACE_ME_*` placeholders and a header explaining these are not cryptographic secrets (they ship in the APK) but are untracked so a fork cannot serve ads against this AdMob account.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt` — the app's only `BuildConfig` ad-field reader; KDoc names PLAT-06 / D-06 / D-08 and the QUAL-03 exception.
- `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt` — 3 tests; KDoc states what it cannot prove and points at `AdIdResolutionTest`.

## Decisions Made

- **Failure message reports *every* missing key, not just the first.** RESEARCH's Pattern 6 text is emitted verbatim once per missing key. A developer configuring a release for the first time is missing all four; making them re-run the build four times to discover that would be hostile.
- **`bundle` added to the whole-project task names.** The plan named bare `assemble` and `build`; `bundle` builds the release AAB and is the store-upload path, so omitting it would have left the most dangerous invocation ungated.
- **`AdIds` uses `get()` accessors rather than `const val`.** `BuildConfig` fields are not compile-time constants from Kotlin's perspective in a way that would allow `const`, and property getters keep the indirection honest.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `java.util.Properties` does not resolve inside a Gradle Kotlin DSL script**
- **Found during:** Task 2 (reading `local.properties`)
- **Issue:** RESEARCH's Pattern 6 snippet uses a fully-qualified `java.util.Properties()`. Inside `app/build.gradle.kts` the generated `java` extension accessor shadows the `java` *package*, producing `Unresolved reference 'util'` and 8 cascading inference errors.
- **Fix:** Added an explicit `import java.util.Properties` at the top of `app/build.gradle.kts` with a comment naming the shadowing, and used the bare `Properties()`.
- **Files modified:** `app/build.gradle.kts`
- **Verification:** `./gradlew :app:assembleDebug` configures cleanly.
- **Committed in:** `e5eb873` (Task 2 commit)

**2. [Rule 3 - Blocking] AGP 9.1.0 also gates `resValue` behind a build feature flag**
- **Found during:** Task 2 (adding the string resources)
- **Issue:** RESEARCH's Pitfall 1 covers only `buildFeatures.buildConfig`. AGP 9.1.0 also defaults `buildFeatures.resValues` to `false` and fails configuration with *"Build Type debug contains custom resource values, but the feature is disabled."*
- **Fix:** Added `resValues = true` alongside `buildConfig = true`, with a comment recording that this one fails loudly rather than silently.
- **Files modified:** `app/build.gradle.kts`
- **Verification:** `:app:assembleDebug` and `clean :app:assembleRelease` both succeed; generated `BuildConfig` and string resources present.
- **Committed in:** `e5eb873` (Task 2 commit)

### Intentional Departures from the Written Plan

**3. `BuildConfigIdsTest` asserts through `AdIds`, not `BuildConfig` directly**
- **Why:** The plan's `<behavior>` names `BuildConfig.REWARDED_AD_UNIT_ID`. But by Task 3 the `BuildConfig` fields already existed (Task 2 generated them), so a test asserting on them directly would have been **green on first run** — no RED phase, and the TDD gate says a test that passes before implementation must be investigated, not accepted. Routing through `AdIds` gives a genuine RED (`Unresolved reference 'AdIds'`), and additionally pins the accessor to the correct generated field names — a mismatch there would otherwise only surface at Phase 9's first real ad request. The asserted values are still `BuildConfig`'s.
- **Impact:** None on the acceptance criteria; `grep -rln "BuildConfig\." app/src/main/java | grep -v AdIds.kt` is still empty, and the test-file literal count is still 1.

**4. `grep -c "resValue" app/build.gradle.kts` returns 5, not the plan's expected 4**
- **Why:** the `resValues = true` feature flag (deviation 2) shares the substring. The four actual resource entries — two per build type — are present as specified. The comment on that line warns the next grepper.

---

**Total deviations:** 2 auto-fixed (both Rule 3 - blocking), 2 documented departures
**Impact on plan:** Both auto-fixes were hard configuration failures with no alternative. Neither expands scope. The two departures strengthen the plan's own intent (a real RED phase; a correctly-configured build).

## Issues Encountered

- **The plan's TDD RED for Task 1 was a compile failure, not an assertion failure.** Unavoidable for a brand-new API in a brand-new source set — `AdIdResolution` did not exist, so `compileTestKotlin` failed. The RED commit (`8eab3e7`) is a genuinely failing state; the GREEN commit (`a2dcb8f`) turns 0 tests into 7 green.
- **Release build needed the network once** (RESEARCH Pitfall 5 — `produceReleaseComposeMapping` pulls `compose-group-mapping`). It resolved from cache in this worktree; no action needed.
- No Gradle daemon or lock contention from the sibling parallel agents was observed.

## Threat Flags

None — no new security-relevant surface beyond the plan's `<threat_model>`. Dispositions honoured:

| Threat ID | How it is mitigated here |
|-----------|--------------------------|
| T-06-23 | Debug is hard-wired to `AdIdDefaults`; `BuildConfigIdsTest` proves all three debug IDs carry Google's test-publisher prefix. |
| T-06-24 | IDs live only in untracked `local.properties` / CI env; `local.properties.template` carries `REPLACE_ME` placeholders; `adIdLiteralCheck` fails on any `ca-app-pub-` literal in `src/main` (demonstrated). `git check-ignore local.properties` still reports it ignored. |
| T-06-25 | Configuration-time `GradleException` (verified) **plus** `verifyReleaseAdIds` wired to `assembleRelease`/`bundleRelease`; when the name gate misses, the release variant carries a loud unresolved marker rather than a test ID. |
| T-06-26 | `assembleDebug` verified green with nothing configured; `taskNames` is empty during IDE sync so sync cannot fail. |
| T-06-27 | Accepted as planned — the IDs are not encrypted and no secrets manager was built. |
| T-06-28 | `git check-ignore -v local.properties` → `.gitignore:7`. Verified after adding the template. |

## Known Stubs

- `AdIdDefaults.TEST_GAMES_PROJECT_ID = "0"` — intentional and documented in its KDoc. Google publishes no test Play Games project ID, so there is nothing safe to hard-wire. Nothing consumes the generated `game_services_project_id` resource until Phase 9 wires the manifest `meta-data`; Phase 11 supplies a real debug value from `local.properties` once the Play Console project exists.
- The two generated string resources (`admob_app_id`, `game_services_project_id`) have no consumer until Phase 9, so the resource shrinker may drop them from release builds until then. Expected, noted in the build file, not a defect.

## User Setup Required

None for debug — that is the point of D-08. **For a release build:** copy `local.properties.template`'s four keys into your untracked `local.properties` (or set the same-named environment variables in CI) and replace every `REPLACE_ME_*` value with a real ID from the AdMob console and Play Console. `./gradlew :app:assembleRelease` tells you exactly which key is missing if you skip one.

## Next Phase Readiness

- **Phase 9 (ads/consent)** can read `AdIds.rewardedAdUnitId` / `interstitialAdUnitId` / `admobAppId` directly, and must add the manifest `meta-data` entries pointing at `@string/admob_app_id` (which is why the resource exists).
- **Phase 11 (Play Games)** consumes `@string/game_services_project_id` from the manifest and should replace `AdIdDefaults.TEST_GAMES_PROJECT_ID` with a `local.properties`-sourced debug value.
- **CI** must set the four environment variables for any release job; debug/test jobs need nothing.
- No blockers.

## Self-Check: PASSED

All 6 created files verified present on disk; all 6 task commit hashes verified in `git log`.

## TDD Gate Compliance

Both TDD tasks show the full RED → GREEN sequence in git log:
- Task 1: `test(06-07)` `8eab3e7` → `feat(06-07)` `a2dcb8f`
- Task 3: `test(06-07)` `3b2636d` → `feat(06-07)` `a9fd17e`

No REFACTOR commits were needed — neither implementation had cleanup pending after GREEN.

---
*Phase: 06-platform-foundation-build-hardening*
*Completed: 2026-08-27*
