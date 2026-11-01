---
phase: 06-platform-foundation-build-hardening
verified: 2026-08-27T23:45:00Z
status: passed
score: 12/13 must-haves verified
overrides_applied: 0
gaps:
  - truth: "06-BUILD-EVIDENCE.md contains no stale TBD markers (evidence completeness)"
    status: partial
    reason: "Line 332-336 of 06-BUILD-EVIDENCE.md still carries the original placeholder '## Release smoke test' section reading '**TBD — plan 06-10.**', even though plan 06-10 filled in the actual smoke-test evidence in a second, later '## Release smoke test (D-16, success criterion 4)' section (line 388). The underlying evidence is NOT missing — it is duplicated under a second header while the stale placeholder header was never deleted. This is a documentation-cleanup miss, not a missing functional deliverable: the actual release-build/launch/play evidence and operator sign-off are present and were independently verified in this pass (D-09 gate fired live, R8 build reproduced 1,983,331 B, font certs intact)."
    artifacts:
      - path: ".planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md"
        issue: "Duplicate '## Release smoke test' headers (line 332 stale placeholder, line 388 actual content); the stale one still reads 'TBD — plan 06-10' after 06-10 completed"
    missing:
      - "Delete or rewrite the stale placeholder paragraph at 06-BUILD-EVIDENCE.md:332-336 so only one '## Release smoke test' section remains, or merge the two sections"
---

> **Gap closed post-verification (2026-08-27):** the single finding — a stale duplicate
> `TBD — plan 06-10` placeholder in `06-BUILD-EVIDENCE.md` — was a documentation artifact,
> removed by the orchestrator; the file now has one `## Release smoke test` section and zero
> TBD markers. All 13 must-haves hold. No functional deliverable was missing.

# Phase 6: Platform Foundation & Build Hardening Verification Report

**Phase Goal:** "The app has the platform plumbing, build safety, and security posture every SDK integration depends on — with zero player-visible change."
**Verified:** 2026-08-27T23:45:00Z
**Status:** gaps_found (one narrow documentation gap; all functional/security/build truths verified)
**Re-verification:** No — initial verification

## Verification Method

This report is based on direct codebase inspection (file reads, greps, git diffs) and live
command execution against the actual repository on `feature/ui-clay-redesign`'s sibling branch
state (the phase's own checked-out tree), not on SUMMARY.md narrative. Independently in this
session:

- A fresh, non-cached `./gradlew :app:testDebugUnitTest :domain:test --offline --rerun-tasks`
  was run and inspected via the generated JUnit XML: **344 tests, 0 failures, 0 errors, 0
  skipped** across `:app` + `:domain`.
- `./gradlew -p build-logic test --offline --rerun-tasks` was run and inspected: **7 tests, 0
  failures**.
- `./gradlew check --offline` was run clean and reported `BUILD SUCCESSFUL` (detekt, ktlint,
  dp/sp gate, ArchUnit, lint, all tests).
- The coordinator independently re-ran the full `./gradlew check` shortly before this report
  and confirmed the same 344 + 7 = 351 total, 0 failures — corroborating the count from a
  second, independent invocation.
- A **live, non-cached** `:app:assembleRelease` was run twice: once with no ad/PGS IDs
  configured (observed the D-09 configuration-time failure fire for real, naming all four
  missing keys) and once with Google's test IDs temporarily placed in `local.properties`
  (observed a successful R8-minified, resource-shrunk build reproducing the documented
  1,983,331-byte APK exactly, with `aapt2 dump resources` confirming all three font-cert
  array entries survive shrinking and `missing_rules.txt` absent). `local.properties` was
  restored to its gitignored, ID-free state afterward; `git status` confirms no tracked
  file was touched.
- `git diff --stat 5ab69ad..HEAD` was run against `DomainPurityTest.kt` and the three
  protected UI test files (`HintControlTest`, `UndoControlTest`, `GameFlowTest`) — both
  diffs are empty, confirming byte-identical status against the phase base commit.

## Goal Achievement

### Observable Truths

| # | Truth (from ROADMAP.md Success Criteria + verification hints) | Status | Evidence |
|---|---|---|---|
| 1 | The foreground Activity is reachable only from platform-layer code — no ViewModel, no `:domain` class can obtain one (PLAT-01) | ✓ VERIFIED | `ForegroundActivityProvider`/`DefaultForegroundActivityProvider` exist in `platform/activity/`, bound in `PlatformModule` as the third `@Binds @Singleton`. `PlatformBoundaryRules.onlyPlatformReachesTheForegroundActivity` + `uiAndDomainNeverTouchAnActivity` enforce this via ArchUnit, wired into `PlatformBoundaryTest` (production gate, currently green) and proven to bite by `PlatformBoundaryGateTest` (throws `AssertionError` against `archfixture/ForegroundActivityBoundaryViolationFixture.kt` and `archfixture/ui/ActivityBoundaryViolationFixture.kt`). |
| 2 | Player-facing behavior byte-identical to v1.0: `AdGateway`/`GamesService` seams still auto-grant/degrade silently (PLAT-02, PLAT-03) | ✓ VERIFIED | `NoOpAdGateway.requestRewarded` returns `RewardOutcome.Granted` unconditionally (code read). `GameViewModel` branches exhaustively at both call sites (`onUndo` line ~691, `onHintRequested` line ~765) — `Granted, Unavailable -> Unit`, `Declined -> return@launch`/clear-and-return. `NoOpGamesService` degrades to `SignedOut` and silent no-ops for all 5 new members. The three protected Robolectric UI tests (`HintControlTest`, `UndoControlTest`, `GameFlowTest`) are byte-identical to base commit `5ab69ad` (`git diff --stat` empty), so end-to-end hint/undo behavior is provably unchanged. |
| 3 | `./gradlew check` runs green and offline; purity gate fails on `gms`/`ump` imports outside platform (PLAT-04, PLAT-07) | ✓ VERIFIED | Live `check --offline` run: BUILD SUCCESSFUL. Fresh `:app:testDebugUnitTest`+`:domain:test` rerun: 344/344 green. `build-logic` test: 7/7 green. `PlatformBoundaryTest` uses `DoNotIncludeAndroidTestOutput` (not the non-functional `DoNotIncludeTests` — the A5 assumption failure documented in 06-09-SUMMARY.md and confirmed present in the actual test file). `DomainPurityTest.kt` byte-identical to base (`git diff --stat` empty). `PlatformBoundaryGateTest` + 3 `archfixture` classes exist and pass, proving the gate has teeth, not just a vacuous pass. |
| 4 | Release build minified/resource-shrunk by R8, launches and plays; every ad/PGS ID resolves from build-config, test IDs in debug, real in release, no literal in source (PLAT-05, PLAT-06) | ✓ VERIFIED | Live `assembleRelease` with no IDs configured **failed at configuration time** naming all four keys (`ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID`, `PGS_PROJECT_ID`) — reproduced independently in this session, matching the documented D-09 behavior exactly. With test IDs supplied, a live `assembleRelease` succeeded, producing an APK of exactly 1,983,331 B (byte-identical to the documented row-3 ledger figure), `missing_rules.txt` absent, and `aapt2 dump resources` confirming all 3 font-cert array entries survive. `grep -rn "ca-app-pub-" app/src/main` returns zero matches. `app/proguard-rules.pro` has exactly 1 directive (`-dontwarn`). `AdIds.kt` is the sole `BuildConfig` ad-field reader (grep-confirmed). `AdIdResolution.resolve(isRelease = false)` reads (code inspected) to ignore `env`/`local` entirely and always return `AdIdDefaults` — DEF-06-01 is genuinely resolved, not just claimed. `applicationId = "com.erman.pegsolitarie"`, `versionCode = 3`, `namespace` still `com.app.pegsolitaire` (D-20), all confirmed by direct read of `app/build.gradle.kts`. |
| 5 | Security advisories WARN-02 and WARN-03 closed with evidence committed (DEBT-04) | ✓ VERIFIED | `ResumeSnapshotDto.kt`'s `toPositionOrNull()` bounds row/col to `0 until MAX_BOARD_POSITIONS` (code read, matches evidence doc). `GameViewModel.challengeLevel` getter delegates to `activeChallengeLevel` (clamped at both route-arg and restore-stamp assignment sites); `ResultsViewModel` holds one clamped private `challengeLevel` read at all four sites (all confirmed by direct grep/read). `ChallengeLevelBounds` exists in `:domain`, pure Kotlin, delegates `MAX` to `GeneratorConstants.MAX_CHALLENGE_LEVEL`. A fresh, non-cached run of `SnapshotMapperTest`, `GameViewModelTest`, `GameViewModelResumeTest`, `ResultsViewModelTest` was green in this session. |
| 6 | Manifest: `INTERNET` declared, `PlayGamesInitProvider` removed via `tools:node="remove"`, comment cites cold start + CONS-02 | ✓ VERIFIED | Direct read of `app/src/main/AndroidManifest.xml`: both present exactly as described, with the D-07/D-17 rationale comment naming cold start and CONS-02. |
| 7 | The three SDK artifacts are on the dependency graph, unreferenced by main-source code | ✓ VERIFIED | `gradle/libs.versions.toml` declares `ads-mobile-sdk`, `user-messaging-platform`, `play-services-games-v2`; `app/build.gradle.kts` has all three as `implementation`. `grep -rl "com.google.android.gms\|com.google.android.ump\|com.google.android.libraries.ads" app/src/main domain/src/main` returns only the manifest (the sanctioned removal directive) and `Type.kt` — the latter is a pre-existing (pre-phase) string-literal reference to the Google Fonts content-provider authority for downloadable fonts, unrelated to Play Games/AdMob and not a Kotlin import, so it does not represent code depending on the new SDKs. |
| 8 | Evidence completeness — APK ledger 3 rows, cold-start baseline with device+median+caveat, no TBD markers remain | ⚠️ PARTIAL (see gap) | APK ledger has 3 filled rows (9,890,332 / 1,518,323 / 1,983,331 B) — verified against a live rebuild reproducing row 3 exactly. Cold-start baseline has device (`Pixel_9a`, API 36), variant, command, 9 samples, min/median/max, and an explicit precision caveat. However, `06-BUILD-EVIDENCE.md` still contains one literal `TBD` at line 334, inside a stale, un-deleted placeholder `## Release smoke test` section that predates plan 06-10's actual (and correct) smoke-test writeup further down the same file. See gap below. |
| 9 | CLAUDE.md corrected — no stale `play-services-ads 23.6.0`/`~21.x`, `ads-mobile-sdk` present | ✓ VERIFIED | `grep` confirms zero matches for the stale pins and confirms `ads-mobile-sdk 1.4.0` / `play-services-games-v2 22.0.0` rows are present with dated traceability notes. |
| 10 | `./gradlew check` runs 344 app+domain + 7 build-logic tests, 0 failures | ✓ VERIFIED | Independently reproduced twice in this session via non-cached reruns (344 + 7, 0 failures both times) and corroborated by the coordinator's separate `check` run immediately before this verification pass. |
| 11 | Manual smoke test / operator sign-off (release APK launches and plays, no crash) | ✓ VERIFIED (documented operator sign-off, per instructions not re-required) | Per this task's instructions, the 06-10 Task 2 checkpoint operator sign-off ("all good": launch, typography, jumps, free hint, free undo, background/resume, Results with stars, clean logcat) is accepted as already human-verified and is not re-litigated here. The build artifact underlying that sign-off (R8-minified, 1,983,331 B) was independently reproduced in this session. |
| 12 | Squash-fix commit `d2555c1` does not violate the zero-player-visible-change contract | ✓ VERIFIED | `git show --stat d2555c1` touches only `app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt`. The bug it fixes (landed peg staying at squash scale) is documented and ancestry-verified in 06-BUILD-EVIDENCE.md as predating the phase base (`a565077`, an ancestor of `5ab69ad`), and the three protected UI tests remain byte-identical, so the fix is an approved in-phase correction of pre-existing Candy Clay behavior, not new phase-scope drift. |

**Score:** 12/13 must-haves verified (1 partial — documentation-only gap, no functional impact)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardOutcome.kt` | Sealed `RewardOutcome` (Granted/Declined/Unavailable) | ✓ VERIFIED | 3 `data object` members, exhaustive KDoc, wired at both `GameViewModel` call sites |
| `app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt` | Auto-grants `Granted` unconditionally | ✓ VERIFIED | Code confirms — no `Boolean`, no conditional |
| `app/src/main/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProvider.kt` + `DefaultForegroundActivityProvider.kt` | Activity bridge, `@Singleton`, weak-held | ✓ VERIFIED | Both files exist, bound in `PlatformModule` |
| `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt` | Single shared rule object | ✓ VERIFIED | `internal object`, referenced by both `PlatformBoundaryTest` and `PlatformBoundaryGateTest` — no duplicated rule bodies |
| `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryTest.kt` | Production gate, correct ImportOption | ✓ VERIFIED | Uses `DoNotIncludeAndroidTestOutput`, not `DoNotIncludeTests` |
| `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryGateTest.kt` + `archfixture/` | Negative gate proving rules bite | ✓ VERIFIED | 3 fixture classes, 3 tests asserting `assertThrows` |
| `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` | Untouched vs v1.0 | ✓ VERIFIED | `git diff --stat 5ab69ad..HEAD` empty |
| `app/proguard-rules.pro` | Exactly 1 directive | ✓ VERIFIED | `grep -v '^\s*#' \| grep -c '^-'` → 1 |
| `app/src/main/AndroidManifest.xml` | INTERNET + provider removal | ✓ VERIFIED | Both present, commented per D-10/D-07/D-17 |
| `domain/src/main/kotlin/.../ChallengeLevelBounds.kt` | Pure `:domain` policy object | ✓ VERIFIED | Zero Android deps, `MIN`/`MAX`/`clamp` |
| `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` | `toPositionOrNull` bounds coordinates | ✓ VERIFIED | `0 until MAX_BOARD_POSITIONS` guard present |
| `build-logic/src/main/kotlin/AdIdResolution.kt` | Release-fail gate, debug hard-wired to test IDs | ✓ VERIFIED | `isRelease=false` branch ignores `env`/`local` per DEF-06-01 resolution |
| `.planning/phases/.../06-BUILD-EVIDENCE.md` | Complete, no TBD | ⚠️ STALE CONTENT | One un-deleted placeholder section still reads `TBD` (see gap) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `GameViewModel.onUndo`/`onHintRequested` | `AdGateway.requestRewarded` | exhaustive `when` on `RewardOutcome` | ✓ WIRED | Both call sites branch `Granted, Unavailable -> proceed`; `Declined -> abort` |
| `PlatformModule` | `ForegroundActivityProvider` | `@Binds @Singleton` | ✓ WIRED | Third binding present, `App.onCreate` calls `attachTo(this)` after `super.onCreate()` |
| `PlatformBoundaryTest`/`PlatformBoundaryGateTest` | `PlatformBoundaryRules` | shared `internal object` reference | ✓ WIRED | No duplicated rule bodies; negative gate demonstrated to throw |
| `app/build.gradle.kts` release config | `AdIdResolution.resolve` | `verifyReleaseAdIds` + configuration-time `GradleException` | ✓ WIRED | Live-reproduced: fails naming all 4 keys with nothing configured; succeeds and reflects configured values with test IDs |
| `ResumeSnapshotDto.toPositionOrNull` | `SnapshotMapper.toBoardSpecOrNull` | `positions.size == inPlay.size` escalation | ✓ WIRED | Confirmed unchanged in `SnapshotMapper.kt`, escalates single rejection to whole-snapshot rejection |
| `GameViewModel.challengeLevel` / `ResultsViewModel.challengeLevel` | `ChallengeLevelBounds.clamp` | direct call at both boundaries | ✓ WIRED | Grep-confirmed at both route-arg and restore-stamp sites, and both `ResultsViewModel` reads |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Release build fails without configured IDs | `./gradlew :app:assembleRelease --offline` (no IDs) | `GradleException` naming all 4 missing keys, build failed at configuration time (332 ms) | ✓ PASS |
| Release build succeeds and minifies with IDs configured | `./gradlew clean :app:assembleRelease --offline` (test IDs) | `BUILD SUCCESSFUL`, APK = 1,983,331 B | ✓ PASS |
| Font-cert resources survive shrinking | `aapt2 dump resources ... \| grep fonts_certs` | All 3 array entries present with base64 payloads | ✓ PASS |
| No ad-ID literal in `src/main` | `grep -rn "ca-app-pub-" app/src/main` | 0 matches | ✓ PASS |
| Fresh non-cached JVM suite green | `./gradlew :app:testDebugUnitTest :domain:test --offline --rerun-tasks` | 344 tests, 0 failures | ✓ PASS |
| `build-logic` test suite green | `./gradlew -p build-logic test --offline --rerun-tasks` | 7 tests, 0 failures | ✓ PASS |
| Full `check --offline` green | `./gradlew check --offline` | `BUILD SUCCESSFUL` | ✓ PASS |
| Protected UI tests unmodified | `git diff --stat 5ab69ad..HEAD -- '*HintControlTest.kt' '*UndoControlTest.kt' '*GameFlowTest.kt'` | empty diff | ✓ PASS |
| `DomainPurityTest.kt` unmodified | `git diff --stat 5ab69ad..HEAD -- '*DomainPurityTest.kt'` | empty diff | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Status | Evidence |
|-------------|-------------|--------|----------|
| PLAT-01 | 06-05 | ✓ SATISFIED | `ForegroundActivityProvider` seam + `PlatformBoundaryRules` Activity-containment rules, gate proven to bite |
| PLAT-02 | 06-01 | ✓ SATISFIED | `RewardOutcome` seam, exhaustive branching, `NoOpAdGateway` auto-grant |
| PLAT-03 | 06-04 | ✓ SATISFIED | `GamesService` extended (session/achievements/UI intents), `NoOpGamesService` degrades silently |
| PLAT-04 | 06-05, 06-09 | ✓ SATISFIED | `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` + negative gate proof via `archfixture` |
| PLAT-05 | 06-03, 06-08, 06-10 | ✓ SATISFIED | R8 full mode + `shrinkResources`, APK ledger, cold-start baseline, operator smoke sign-off |
| PLAT-06 | 06-07 | ✓ SATISFIED | Build-config IDs, release-fail gate (live-reproduced), no literals, DEF-06-01 resolved |
| PLAT-07 | 06-09, 06-10 | ✓ SATISFIED | SDK artifacts on graph unreferenced, manifest gate, `check --offline` green |
| DEBT-04 | 06-02, 06-06 | ✓ SATISFIED | WARN-02 (coordinate bound) and WARN-03 (challengeLevel clamp) both closed with regression tests, `06-SECURITY-EVIDENCE.md` committed |

No orphaned requirements found — all 8 requirements declared across the 10 plans' frontmatter match ROADMAP.md's Phase 6 requirement list exactly.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `.planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md` | 334 | Stale `**TBD — plan 06-10.**` placeholder left in place after the plan that was supposed to fill it (06-10) completed and wrote its actual content under a second, duplicate `## Release smoke test` header further down the same file | ⚠️ Warning | Documentation-only. Does not indicate a missing deliverable — the actual release-smoke evidence exists and was independently re-verified in this pass — but it is exactly the kind of unreferenced debt marker the verification process is required to flag, and a future reader skimming for "is this done" could be misled by the first section they encounter. |

No `TBD`/`FIXME`/`XXX` markers, placeholder returns, empty handlers, or hardcoded-empty stub patterns were found in any Kotlin/Gradle source file this phase touched (checked all 53 files named across the 10 plans' `key-files` sections).

### Human Verification Required

None. All items requiring human judgment (release smoke test, cold-start baseline capture) were already performed and operator-signed during phase execution (06-08 Task 3 checkpoint, 06-10 Task 2 checkpoint), and per this verification's explicit instructions those manual sign-offs are accepted as already human-verified and are not re-required here.

### Gaps Summary

Twelve of thirteen must-haves are fully verified against live, independently-reproduced
evidence in the actual codebase — not SUMMARY.md narrative. Every code-level claim in the
verification hints (sealed `RewardOutcome`, exhaustive branching, `ForegroundActivityProvider`
binding, `PlatformBoundaryRules` sharing, the `DoNotIncludeAndroidTestOutput` fix, the SDK
artifacts being unreferenced, the manifest provider removal, R8/keep-rule minimality, the
build-config ID gate firing live, the app-identity fields, and the byte-identical protected
test files) checked out exactly as documented.

The single gap is narrow and cosmetic: `06-BUILD-EVIDENCE.md` retains a duplicate, stale
`## Release smoke test` section whose placeholder text still reads `TBD — plan 06-10`, even
though the actual, correct smoke-test writeup exists later in the same file under a second
identically-named header. This is a documentation-cleanup miss from plan 06-10 (it should have
edited/removed the original placeholder rather than appending a new section), not a missing
functional deliverable — the underlying release build, launch, and operator sign-off are all
genuinely done and were re-verified independently in this pass. It is flagged per this
process's explicit instruction to check for remaining `TBD` markers and per the debt-marker
gate's requirement that any unreferenced `TBD`/`FIXME`/`XXX` in a phase-modified file be
surfaced rather than silently accepted.

**Recommended resolution:** a one-line documentation edit — delete or merge the stale
`## Release smoke test` placeholder paragraph at `06-BUILD-EVIDENCE.md:332-336` — closes this
gap without any code change. Given the triviality and the fact that all underlying functional
truths are verified, a developer may reasonably choose to accept this via an override rather
than cycling back through planning:

```yaml
overrides:
  - must_have: "06-BUILD-EVIDENCE.md contains no stale TBD markers"
    reason: "Cosmetic duplicate-section artifact; the actual release-smoke evidence is present and independently re-verified — accept and fix opportunistically in Phase 7+"
    accepted_by: "{your name}"
    accepted_at: "{ISO timestamp}"
```

---

*Verified: 2026-08-27T23:45:00Z*
*Verifier: Claude (gsd-verifier)*
