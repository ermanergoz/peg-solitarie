---
phase: 02-classic-mode-scoring-navigation
plan: 03
subsystem: infra
tags: [navigation-compose, kotlinx-serialization, datastore, gradle, dimens, strings]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: "version catalog + :app build with the compileSdk-36 resolution guard, dimens.xml/strings.xml resource ownership pattern"
provides:
  - "navigation-compose 2.9.8 on :app's compileSdk-36 classpath (NAV-02 type-safe NavHost)"
  - "kotlin-serialization plugin applied + kotlinx-serialization-json 1.11.0 runtime for @Serializable routes"
  - "datastore-preferences 1.2.1 for the onboardingSeen flag (ONB-01)"
  - "Six Phase-2 dimens tokens (content_max_width, mode_tile_min_height, catalog_grid_min_cell_width, catalog_preview_size, results_star_size, onboarding_illustration_size)"
  - "All Phase-2 strings copy (board names, Home tiles, catalog, gameplay, results, onboarding) + holes_count plural"
affects: [02-05 catalog, 02-06 home-onboarding, 02-07 results, nav-graph]

# Tech tracking
tech-stack:
  added: [androidx.navigation:navigation-compose:2.9.8, org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0, androidx.datastore:datastore-preferences:1.2.1, org.jetbrains.kotlin.plugin.serialization]
  patterns: ["Single-owner resource files: dimens/strings declared up front in 02-03 so downstream parallel UI plans avoid write contention", "compileSdk-36 pin discipline: stay on pre-37 stable lines (navigation 2.9.x not 2.10.x)"]

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/res/values/dimens.xml
    - app/src/main/res/values/strings.xml

key-decisions:
  - "Pinned navigation-compose to 2.9.8 (latest STABLE) rather than 2.10.x — 2.10.x is alpha and requires compileSdk 37 (uninstallable here)"
  - "Applied the kotlin-serialization Gradle plugin (not just the runtime jar) — @Serializable nav routes crash at navigate time without the plugin (RESEARCH Pitfall 6)"
  - "No new resolutionStrategy guard needed: navigation 2.9.8 pulls lifecycle 2.9.x, already force-pinned to 2.9.4 by the existing atomic-group guard; resolved graph carries no compileSdk-37 line"

patterns-established:
  - "Resource-file single ownership: shared dimens/strings centralized in the deps plan so later UI plans only read them"

requirements-completed: [NAV-02, ONB-01]

# Metrics
duration: 9min
completed: 2026-06-29
---

# Phase 2 Plan 03: Phase-2 Build Dependencies + UI Resource Tokens Summary

**Added navigation-compose 2.9.8, kotlinx-serialization (plugin + json 1.11.0), and datastore-preferences 1.2.1 to the compileSdk-36 graph, and declared all Phase-2 dimens tokens and strings copy in one place for the downstream Classic-flow UI plans.**

## Performance

- **Duration:** ~9 min
- **Completed:** 2026-06-29
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Three first-party Phase-2 deps (navigation-compose, kotlinx-serialization-json, datastore-preferences) resolve on the compileSdk-36 classpath with no compileSdk-37 lifecycle/core line dragged in
- kotlin-serialization Gradle plugin applied to :app so future @Serializable routes generate serializers (NAV-02)
- Six Phase-2 dimens tokens added (all multiples of 4) and all Phase-2 strings copy declared verbatim from the UI-SPEC Copywriting Contract, including the plural-aware holes_count

## Task Commits

Each task was committed atomically:

1. **Task 1: Catalog + :app dependencies + resolution guard** - `4289f7e` (feat)
2. **Task 2: Phase-2 dimens tokens + strings copy** - `e3dc26e` (feat)

## Files Created/Modified
- `gradle/libs.versions.toml` - Added navigationCompose/kotlinxSerialization/datastore versions, three library aliases, kotlin-serialization plugin alias
- `app/build.gradle.kts` - Applied serialization plugin; added navigation-compose, serialization-json, datastore-preferences implementation deps
- `app/src/main/res/values/dimens.xml` - Six Phase-2 layout tokens
- `app/src/main/res/values/strings.xml` - All Phase-2 copy + holes_count plural

## Decisions Made
- Pinned navigation-compose 2.9.8 (stable) over 2.10.x alpha to stay compileSdk-36-safe
- Applied the serialization plugin (Pitfall 6), not just the json runtime
- Reused the Phase-1 restart_board string rather than duplicating it for the gameplay restart content description

## Deviations from Plan

None - plan executed exactly as written. The optional `resolutionStrategy.eachDependency` extension in Task 1's action was not required: the resolved `debugRuntimeClasspath` showed navigation-compose 2.9.8 with no compileSdk-37 lifecycle (2.10/2.11) or core (1.19) line, since the existing atomic-group lifecycle guard already force-pins 2.9.4.

## Issues Encountered
None. Pre-existing build warnings (Kotlin plugin loaded in multiple subprojects; deprecated hiltViewModel()) are out of scope and carried over from Phase 1 — not introduced by this plan.

## User Setup Required
None - all three deps are first-party AndroidX/JetBrains Maven artifacts, no external service configuration.

## Next Phase Readiness
- NAV-02 build prerequisites ready: type-safe NavHost can be authored in the nav-graph plan (navigation-compose + serialization plugin both on classpath)
- ONB-01 ready: datastore-preferences available for the onboardingSeen flag
- All shared dimens/strings declared — Catalog (02-05), Home/onboarding (02-06), Results (02-07) can consume via dimensionResource/stringResource with no magic numbers/strings

## Self-Check: PASSED

All 4 modified files present; both task commits (4289f7e, e3dc26e) found in git history.

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-29*
