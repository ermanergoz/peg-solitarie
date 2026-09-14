---
phase: quick-260915-3jd
plan: 01
subsystem: build-logic
tags: [build, play-games, plat-06, def-06-01, d-17]
requires: [PLAT-06, D-17, DEF-06-01]
provides:
  - "Debug builds emit the configured PGS_PROJECT_ID as string/game_services_project_id"
affects:
  - build-logic/src/main/kotlin/AdIdResolution.kt
  - build-logic/src/main/kotlin/AdIdDefaults.kt
  - local.properties.template
tech-stack:
  added: []
  patterns: ["Gradle-free Map-in/result-out resolver, JVM-tested in the included build"]
key-files:
  created: []
  modified:
    - build-logic/src/main/kotlin/AdIdResolution.kt
    - build-logic/src/main/kotlin/AdIdDefaults.kt
    - build-logic/src/test/kotlin/AdIdResolutionTest.kt
    - local.properties.template
decisions:
  - "DEF-06-01 is narrowed, not reversed: the three AD IDs stay hard-wired on debug; PGS_PROJECT_ID is carved out because a project ID cannot serve an ad."
metrics:
  duration: ~20 min
  completed: 2026-09-15
requirements: [PLAT-06, D-17, DEF-06-01]
---

# Quick Task 260915-3jd: Fix debug builds emitting Play Games project ID "0" — Summary

One-liner: `AdIdResolution`'s debug branch now honours a configured `PGS_PROJECT_ID`
(env over `local.properties`) while keeping the three AdMob IDs hard-wired to Google's
test units, so a debug APK carries the real `game_services_project_id` instead of `"0"`.

## What Changed

**`AdIdResolution.resolve` (debug branch, one field).** `gamesProjectId` became
`configured[AdIdKeys.PGS_PROJECT_ID] ?: AdIdDefaults.TEST_GAMES_PROJECT_ID`. `appId`,
`rewarded` and `interstitial` keep their `AdIdDefaults.TEST_*` constants verbatim. The
`configured` map, `present`, `failureMessage` and the entire release branch are untouched —
env-over-local and the blank/`REPLACE_ME`-means-absent rule already came for free from the
existing `configured` line, which is why the fix is one expression and three new tests.

**The DEF-06-01 comment** above that return was rewritten to state the carve-out and its
justification: the ad IDs stay hard-wired because a real ad unit on a development device is
invalid traffic and an AdMob suspension is non-appealable; `PGS_PROJECT_ID` is exempt because
a project ID cannot serve an ad — it only lets a debug build sign in to the real Play Games
project, which is precisely why the debug-keystore SHA-1 is registered (Phase 11 D-04). It
names the defect it fixes (backend NOT_FOUND → `isAuthenticated=false` with no error, which
the Settings row renders as a silent cancel) and cross-references D-17 / `GamesIdResolution`,
whose debug rule this now matches. The four-key result is no longer split-brain about Play
Games.

**`AdIdDefaults.TEST_GAMES_PROJECT_ID` KDoc** was stale on both of its claims — it promised
"Phase 11 supplies a real debug value" and asserted "nothing consumes this resource". It now
says it is the fallback used only when `PGS_PROJECT_ID` is unset, that a configured value
wins on debug, and that the resource IS consumed by the manifest's
`com.google.android.gms.games.APP_ID`, so `"0"` means sign-in silently fails.

**`local.properties.template`** — the header's hard-wire promise is scoped to the three AD
units with the `PGS_PROJECT_ID` exception named (the fresh-clone promise is kept intact), and
the key's own comment states both directions: set it and debug signs in to the REAL project;
leave it unset and debug falls back to the inert `"0"` where sign-in never succeeds.

## Tests

`AdIdResolutionTest`: 10 tests, all green.

- `debugIgnoresConfiguredIdsAndAlwaysUsesGoogleTestUnits` → renamed
  `debugIgnoresConfiguredAdIdsAndAlwaysUsesGoogleTestUnits`; its `gamesProjectId` assertion
  removed (it now proves the ads half only) and its KDoc points at the carve-out test.
- `debugHonoursAConfiguredGamesProjectIdWhileKeepingTestAdUnits` (new) — the regression test.
  Asserts both halves in one test: the configured project ID wins AND all three ad IDs are
  still the `AdIdDefaults.TEST_*` units.
- `debugPrefersAnEnvironmentGamesProjectIdOverLocalProperties` (new).
- `debugTreatsABlankOrPlaceholderGamesProjectIdAsUnset` (new) — both inputs fall back.
- `debugResolvesGoogleTestIdsWhenNothingIsConfigured` unchanged (now the *unconfigured* case).
- The six release tests are unmodified and green.

RED was observed before the fix: exactly the two "configured wins" tests failed, at the
`gamesProjectId` assertion, for the right reason (placeholder returned where a configured
value was expected). The blank/placeholder test passed from the start, as it pins behaviour
the change must preserve.

## Verification

| Gate | Result |
|------|--------|
| `./gradlew -p build-logic test` | BUILD SUCCESSFUL — `AdIdResolutionTest` 10/10, `GamesIdResolutionTest` green |
| `./gradlew detekt ktlintCheck` | BUILD SUCCESSFUL (25 tasks) |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| aapt2 resource dump vs `local.properties` | `string/game_services_project_id` = the configured `PGS_PROJECT_ID`, equal and not `"0"` |
| `:app:testDebugUnitTest --tests *BuildConfigIdsTest --tests *ManifestSurfaceTest` | BUILD SUCCESSFUL — debug ad IDs still carry Google's test publisher prefix (T-3jd-03) |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The plan's aapt2 extraction one-liner selected the wrong resource**

- **Found during:** Task 2 verification
- **Issue:** `grep -A3 'game_services_project_id' | grep -o '"[^"]*"' | tail -1` takes the
  *last* quoted string in a 4-line window. aapt2 lists resources alphabetically, so that
  window ends inside the next entry (`string/hint_action` = `"Hint"`), and the check compared
  `"Hint"` against the project ID and exited 1 — a false negative, not a real failure.
- **Fix:** narrowed the window and took the first match: `grep -A1 'string/game_services_project_id'
  | grep -o '"[^"]*"' | head -1`. Verification then passed on the same, unmodified APK.
- **Files modified:** none (verification command only — no plan artifact changed).

No other deviations. The resolver change is the single line the plan specified.

## Security Notes

- T-3jd-01 (information disclosure) held: `local.properties` was never `cat`-ed; only the
  single `PGS_PROJECT_ID` line was grepped, and the project number is the only value echoed
  anywhere. The file remains gitignored and uncommitted.
- T-3jd-02 (debug writes to real leaderboards) is the accepted D-17 residual risk, now
  stated in both the code comment and the template.
- T-3jd-03 (loosening the ad hard-wire) is pinned by two build-logic tests plus the
  independent `BuildConfigIdsTest`, all green.
- T-3jd-04 (release regression) — no edit below `if (isRelease)`, no edit to
  `app/build.gradle.kts`; the six release tests are byte-identical and pass.

## Commits

| Hash | Message |
|------|---------|
| f734f44 | `fix(quick-260915-3jd): honour a configured PGS_PROJECT_ID on debug builds` |
| 8f2cbe3 | `docs(quick-260915-3jd): state the PGS_PROJECT_ID debug carve-out in the template` |

## Follow-ups

None. The on-device confirmation that Play Games sign-in now succeeds belongs to
`11-HUMAN-UAT.md` tests 4-9, which were already outstanding and are unaffected by this fix
other than being unblocked by it.

## Self-Check: PASSED

All four modified files exist on disk, both commit hashes resolve in `git log`, and the
carve-out expression `configured[AdIdKeys.PGS_PROJECT_ID]` is present in the debug branch.
The two commits touch exactly the four planned files and nothing else.
