---
phase: 12-leaderboards-achievements
plan: 04
subsystem: infra
tags: [gradle, buildconfig, play-games, plat-06, build-logic, ksp-free, id-resolution]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening
    provides: "AdIdResolution / AdIdKeys, the release fail-fast pattern (D-08/D-09), verifyReleaseAdIds, the adIdLiteralCheck source gate and local.properties.template"
  - phase: 11-play-games-session
    provides: "platform/games package, GamesConstants, the PGS project ID resValue and the debug-keystore SHA-1 registration that makes debug submission possible"
  - phase: 12-leaderboards-achievements
    provides: "12-03's GamesService seam and platform/games conventions (wave 1)"
provides:
  - "GamesIdKeys — the 27 Play Games key names (7 PGS_LB_* + 20 PGS_ACH_*) in exactly one place"
  - "GamesIdResolution — Gradle-free resolver: debug never fails (UNCONFIGURED-<KEY>), release fails naming every missing key"
  - "27 looped buildConfigField String fields in BOTH build types"
  - "verifyReleaseGamesIds — execution-time packaging backstop hooked to assembleRelease/bundleRelease"
  - "gamesIdLiteralCheck — src/main source gate closing the games half of PLAT-06"
  - "GamesIds.Leaderboards (7 accessors + forClassic) and GamesIds.Achievements (20 accessors)"
  - "local.properties.template documenting all 27 keys with REPLACE_ME placeholders"
affects: [12-07, 12-08, 14-release, play-console-setup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sibling resolver objects in build-logic rather than one growing resolver — each domain's failure message names only its own keys"
    - "Map-returning resolution result where the only consumer is a buildConfigField forEach"
    - "Looped buildConfigField emission for large key sets, replacing hand-written per-key lines"
    - "Heuristic ID-shape source gates (CgkI) as a sibling to the exact-prefix ad gate"

key-files:
  created:
    - build-logic/src/main/kotlin/GamesIdResolution.kt
    - build-logic/src/test/kotlin/GamesIdResolutionTest.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt
  modified:
    - app/build.gradle.kts
    - build-logic/src/main/kotlin/standards.gradle.kts
    - local.properties.template
    - app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt

key-decisions:
  - "GamesIdResolution is a SIBLING of AdIdResolution, never an extension: growing AdIdResolutionResult.Resolved from 4 to 31 fields would bury the AdMob-suspension contract that file exists to protect"
  - "Resolved carries a Map<String, String>, not a 27-field data class — the only consumer is a buildConfigField forEach, and GamesIds pins the field names on the Kotlin side anyway"
  - "Debug HONOURS a configured Play Games ID, deliberately opposite to AdIdResolution's hard-wired debug: Google publishes no PGS test IDs, the project registers the debug-keystore SHA-1 precisely so debug builds can submit, and pre-publication scores are resettable via the Management API"
  - "present() also rejects UNCONFIGURED--prefixed values, so a copy-pasted debug placeholder cannot satisfy the release check"
  - "The non-release-invocation release fallback is the shared loud UNRESOLVED- marker, never the debug UNCONFIGURED- placeholder — release must never silently inherit a debug value"
  - "verifyReleaseGamesIds is a second task beside verifyReleaseAdIds so each failure message names its own domain"
  - "No resValue for any of the 27: nothing in the manifest or any layout reads a leaderboard or achievement ID"
  - "GamesIds.of(AchievementId) is deliberately absent — the :domain enum is 12-02's work and belongs to 12-07"

patterns-established:
  - "Per-domain build-time ID resolution: keys object + Gradle-free resolver + build-logic test + looped buildConfigField + verify task + literal gate + template block"
  - "Wiring-not-values testing: assert an accessor's UNCONFIGURED- fallback names its own key, so the accessor-to-field proof holds on any unconfigured machine and stays vacuously true on a configured one"

requirements-completed: [PGSL-01, PGSL-02, PGSL-03, PGSL-06, PGSA-05]

# Metrics
duration: 34 min
completed: 2026-09-09
---

# Phase 12 Plan 04: Play Games Build-Config ID Plumbing Summary

**27 Play Games leaderboard/achievement IDs resolved from an untracked `local.properties` or same-named CI env vars into per-build-type `BuildConfig` fields, with a debug `UNCONFIGURED-<KEY>` fallback that keeps a zero-configuration clone building and a release fail-fast that names every missing key — surfaced to the app through `GamesIds`.**

## Performance

- **Duration:** 34 min
- **Started:** 2026-09-09T01:38:00Z
- **Completed:** 2026-09-09T02:12:00Z
- **Tasks:** 3 (2 of them TDD, so 5 commits)
- **Files modified:** 8 (3 created, 5 modified)

## Accomplishments

- `GamesIdKeys` holds all 27 key names in exactly one place, each KDoc'd with the CONTEXT decision (D-01…D-04) it serves, so 12-02's enum and 12-08's Console checklist have an unambiguous cross-reference.
- `GamesIdResolution` is a Gradle-free pure function proven by 10 `build-logic` tests — the only place AGP 9.1.0 permits a release-side assertion, since `:app` has no `testReleaseUnitTest`.
- Both build types emit all 27 `String` fields from a single `forEach`; the generated debug `BuildConfig.java` was verified to carry exactly 27 `PGS_` fields.
- Two independent release gates: a configuration-time `GradleException` and the execution-time `verifyReleaseGamesIds` task hooked to `assembleRelease`/`bundleRelease`.
- `gamesIdLiteralCheck` closes the games half of PLAT-06, which `adIdLiteralCheck` could not cover (`ca-app-pub-` matches no Play Games ID). Proven by planting a `CgkI` literal and watching the gate fail with an actionable message.
- `GamesIds` gives the app one typed window onto the fields, with `forClassic` mapping all five Classic boards and returning `null` for everything else.

## Task Commits

1. **Task 1 RED: GamesIdResolution contract test** — `26c7d67` (test)
2. **Task 1 GREEN: GamesIdResolution + GamesIdKeys** — `c42a864` (feat)
3. **Task 2: 27 buildConfigFields, verifyReleaseGamesIds, gamesIdLiteralCheck, template** — `7ab1c4b` (feat)
4. **Task 3 RED: GamesIds wiring test** — `076a912` (test)
5. **Task 3 GREEN: GamesIds + GamesConstants KDoc fix** — `ed4033b` (feat)

_Tasks 1 and 3 were `tdd="true"`, hence the test → feat pairs. No REFACTOR commit was needed: neither implementation had duplication worth extracting once green._

## The 27 key names (for 12-08's CI-secret and Play Console recording)

**Leaderboards (7):**
`PGS_LB_CHALLENGE_LADDER`, `PGS_LB_CHALLENGE_SCORE`, `PGS_LB_CLASSIC_ENGLISH33`,
`PGS_LB_CLASSIC_EUROPEAN37`, `PGS_LB_CLASSIC_ASYMMETRIC39`, `PGS_LB_CLASSIC_DIAMOND41`,
`PGS_LB_CLASSIC_WIEGLEB45`

**Achievements (20):**
`PGS_ACH_FIRST_SOLVE`, `PGS_ACH_FIRST_THREE_STAR`, `PGS_ACH_CHALLENGE_LEVEL_5`,
`PGS_ACH_SPEED_SOLVE`, `PGS_ACH_HINT_FREE`, `PGS_ACH_UNDO_FREE`, `PGS_ACH_CENTER_FINISH`,
`PGS_ACH_CLEARED_ENGLISH33`, `PGS_ACH_CLEARED_EUROPEAN37`, `PGS_ACH_CLEARED_ASYMMETRIC39`,
`PGS_ACH_CLEARED_DIAMOND41`, `PGS_ACH_CLEARED_WIEGLEB45`, `PGS_ACH_LADDER_10`,
`PGS_ACH_LADDER_50`, `PGS_ACH_LADDER_100`, `PGS_ACH_STARS_25`, `PGS_ACH_STARS_100`,
`PGS_ACH_STARS_250`, `PGS_ACH_GRAND_TOUR`, `PGS_ACH_PERFECTIONIST`

Each is settable either as a `local.properties` entry or as an identically-named environment
variable; the environment wins.

## Files Created/Modified

- `build-logic/src/main/kotlin/GamesIdResolution.kt` — `GamesIdKeys` (27 keys + `LEADERBOARDS`/`ACHIEVEMENTS`/`ALL` + `UNCONFIGURED_PREFIX`), `GamesIdResolutionResult`, and the `resolve`/`present`/`failureMessage` trio.
- `build-logic/src/test/kotlin/GamesIdResolutionTest.kt` — 10 cases; the only executable proof of the release contract.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt` — 27 `get()` accessors in two nested objects, plus `Leaderboards.forClassic`.
- `app/build.gradle.kts` — single `local.properties` parse, `gamesIdEnv`/`gamesIdLocal`, `debugGamesIds`/`releaseGamesIds`, the two `forEach` field loops, `verifyReleaseGamesIds`, and the widened `dependsOn`.
- `build-logic/src/main/kotlin/standards.gradle.kts` — `gamesIdMarker` + `gamesIdLiteralCheck`, wired into `check` (header KDoc updated from five checks to six).
- `local.properties.template` — a documented 27-key block with `REPLACE_ME_` placeholders.
- `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt` — 7 new games cases driven off one `gamesIdsByKey` map; the 3 ad cases are byte-identical.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt` — the stale "Phase 12's IDs get their own build-time resolution" forward reference now names `GamesIds`.

## Decisions Made

See `key-decisions` in the frontmatter. The one worth restating in prose:

**Debug honours a configured Play Games ID, and that is the opposite of what `AdIdResolution` does.** `AdIdResolution` hard-wires debug to Google's test units and ignores configuration entirely, because a real ad unit on a development device is invalid traffic and gets an AdMob account suspended non-appealably. Play Games carries no equivalent penalty, publishes no test IDs at all, and registers the debug-keystore SHA-1 *precisely so debug builds can sign in and submit*. Hard-wiring here would make on-device submission untestable forever. The residual risk — a developer who fills real IDs into `local.properties` will write to the real leaderboards from debug — is recorded in the resolver's comment, in `local.properties.template`, and in the plan's threat register as T-12-42 (accept, bounded).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `local.properties` absent from the fresh worktree**
- **Found during:** Task 1 (before the first Gradle invocation)
- **Issue:** `local.properties` is gitignored, so a fresh Claude Code worktree has none and Gradle fails with "SDK location not found". This is the known environment quirk every wave-1 executor hit.
- **Fix:** Copied `local.properties` from the main checkout into the worktree root. Its contents are only `sdk.dir` — it carries **no** ad or Play Games IDs, so it also served as the plan's required "fresh clone with zero configuration" case.
- **Files modified:** None tracked (the file is gitignored and was never staged; `git ls-files local.properties` is empty and `git check-ignore` still exits 0).
- **Verification:** `./gradlew :app:assembleDebug` green with all 27 keys unset.
- **Committed in:** N/A — deliberately not committed.

**2. [Rule 1 - Bug] Task 3 acceptance criterion `grep -c 'TODO(' GamesIds.kt` printed 1**
- **Found during:** Task 3 (acceptance-criteria verification loop)
- **Issue:** The criterion demands `0`. The only match was the `GamesIds` KDoc line *stating that there is no `TODO()` here* — prose describing the absence, not a stub. Substantively compliant, literally failing.
- **Fix:** Reworded the KDoc to "no `TODO` marker here", preserving the meaning while making the literal criterion pass rather than arguing the intent.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt`
- **Verification:** `grep -c 'TODO(' …/GamesIds.kt` prints `0`.
- **Committed in:** `ed4033b` (Task 3 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 criterion-compliance).
**Impact on plan:** None on scope or design. The first is an environment quirk that produced the exact zero-configuration state the plan wanted to test; the second is a one-word doc edit. No scope creep.

## Issues Encountered

**Verification step 7 — which resolver threw first (the plan asked this be recorded explicitly).**

`./gradlew :app:assembleRelease --dry-run` fails at configuration time with the **ad** resolver's message, naming `ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID` and `PGS_PROJECT_ID`. The games check is never reached, because `releaseIds` (ad) is declared above `releaseGamesIds` in `app/build.gradle.kts` and throws first. **This is correct behaviour, not a defect** — this machine has no ad IDs configured either, and both gates are fail-fast by design. The resolvers were **not** reordered and no real IDs were configured to make it pass.

The games gate was instead proven independently, two ways:
1. `GamesIdResolutionTest`'s four `release…` cases (the authoritative proof).
2. A one-shot smoke run supplying only the four ad keys as throwaway environment variables (`ADMOB_APP_ID=probe-app …`), which pushed configuration past the ad resolver and produced the games resolver's own failure naming all 27 keys and closing with `Debug builds substitute UNCONFIGURED-<KEY> and need no configuration.` Nothing was written to disk by that run.

**`.github/workflows/ci.yml` needed no change, deliberately.** CI writes a `local.properties` containing only `sdk.dir` and runs `ktlintCheck detekt :domain:test test assembleDebug` — never a release invocation. So it needs no new secret, and it exercises the `UNCONFIGURED-` path, which is exactly the path `BuildConfigIdsTest`'s wiring assertions are written to prove. **A future signed-release CI job will need all 27 keys as secrets — that is a Phase 14 concern**, alongside the 4 existing ad keys.

## Verification Results

| # | Command | Result |
|---|---------|--------|
| 1 | `./gradlew -p build-logic test` | PASS — 10 `GamesIdResolutionTest` + 7 `AdIdResolutionTest`, the latter unchanged |
| 2 | `./gradlew :app:compileDebugKotlin` | PASS |
| 3 | `./gradlew :app:testDebugUnitTest --tests "*BuildConfigIdsTest*"` | PASS — 10 cases (3 ad + 7 games) |
| 4 | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*" --tests "*DomainPurityTest*"` | PASS — the `platform → data` `ClassicLevelIds` import breaks no gate |
| 5 | `./gradlew detekt ktlintCheck gamesIdLiteralCheck adIdLiteralCheck` | PASS |
| 6 | `./gradlew :app:assembleDebug` | PASS with zero PGS keys configured |
| 7 | `./gradlew :app:assembleRelease --dry-run` | FAILS as designed (ad resolver first — see Issues) |
| — | `./gradlew :app:testDebugUnitTest` (whole suite) | PASS — no regression |
| — | Generated `BuildConfig.java` spot check | 27 `PGS_` fields; `PGS_LB_CHALLENGE_LADDER = "UNCONFIGURED-PGS_LB_CHALLENGE_LADDER"` |
| — | `gamesIdLiteralCheck` negative test | Planted a `CgkI…` literal under `src/main`; gate failed naming file and line, then the probe was deleted |

The phase gate `./gradlew check` was deliberately **not** run here — it pulls in `:domain:depthSweep` at ~11 minutes and is the orchestrator's post-merge step.

## Known Stubs

None. `GamesIds.of(AchievementId)` is **absent by design, not stubbed** — no placeholder function, no stub enum, no `TODO` marker. The `:domain` `AchievementId` enum is plan 12-02's deliverable and the mapping function is plan 12-07's; inventing either here would collide with those plans. `GamesIds.Achievements` is structured so `of()` drops in beside it without anything moving, and the `GamesIds` KDoc records that contract for 12-07's author.

## Threat Flags

None. This plan introduces no network endpoint, no auth path, no file access and no schema change. Every surface it touches is already enumerated in the plan's threat register (T-12-40 through T-12-44, T-12-SC), and the two `mitigate` dispositions were implemented as specified: `local.properties` stays untracked with `gamesIdLiteralCheck` guarding `src/main` (T-12-40), and the release placeholder path is closed by two independent gates plus `present()`'s `UNCONFIGURED-` rejection (T-12-41). No package manager ran and `gradle/libs.versions.toml` is byte-identical (T-12-SC).

## User Setup Required

None for this plan to build or test. **Operationally**, all 27 IDs are unavailable until the Play Console project, its 7 leaderboards and its 20 achievements exist — that is the 12-08 operator checklist and Phase 14. Until then every key resolves to `UNCONFIGURED-<KEY>`, the game builds and runs normally, and only submission is inert.

## Next Phase Readiness

Ready for **12-07**, which is the direct consumer:
- `GamesIds.Leaderboards.challengeLadder` / `.challengeScore` / `.forClassic(levelId)` are available now.
- `GamesIds.Achievements` exposes all 20 accessors, ready for `fun of(id: AchievementId): String` as an exhaustive `when` once 12-02 lands the enum.
- `forClassic` returns `null` for every Challenge level — 12-07 must treat that as *skip*, not as a failure, and must not set the dirty flag on it. This is KDoc'd at the function.

Also ready for **12-08**: the 27 key names above are final and can be recorded into CI secrets and the Play Console checklist verbatim.

No blockers. `.planning/STATE.md` and `.planning/ROADMAP.md` were deliberately **not** modified — this plan ran as a parallel worktree executor and the orchestrator owns those writes.

## Self-Check: PASSED

- `build-logic/src/main/kotlin/GamesIdResolution.kt` — FOUND
- `build-logic/src/test/kotlin/GamesIdResolutionTest.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt` — FOUND
- `local.properties.template` — FOUND (27 keys, 27 `REPLACE_ME_` placeholders)
- Commits `26c7d67`, `c42a864`, `7ab1c4b`, `076a912`, `ed4033b` — all present on `worktree-agent-afec1f8cec6596063`
- `git ls-files local.properties` empty and `git check-ignore -q local.properties` exits 0 — the real file stays untracked
- `gradle/libs.versions.toml`, `.github/workflows/ci.yml` and `build-logic/src/main/kotlin/AdIdResolution.kt` — zero diff against the base commit
- All task `<acceptance_criteria>` re-run and green; all 7 plan `<verification>` steps executed with results recorded above

---
*Phase: 12-leaderboards-achievements*
*Completed: 2026-09-09*
