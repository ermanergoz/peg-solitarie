---
phase: 12
slug: leaderboards-achievements
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-09-07
updated: 2026-09-09
source: 12-RESEARCH.md "Validation Architecture" (lines 896-951), mapped onto 12-01..12-08-PLAN.md
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Derived from the RESEARCH.md Validation Architecture; the plan tasks below carry the same commands in their `<verify><automated>` blocks.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for Android-runtime JVM tests (`@Config(sdk = [34])`); ArchUnit 1.3.0 for boundary gates |
| **Config file** | none for JUnit4; detekt at `config/detekt/detekt.yml` |
| **Modules** | `:domain` (pure JVM) and `:app` (`testDebugUnitTest`; no `androidTest` source set); `build-logic` tests via `./gradlew -p build-logic test` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "<narrowest class filter>"` or `./gradlew :domain:test --tests "<class>"` |
| **Full suite command** | `./gradlew :domain:test :app:testDebugUnitTest` |
| **Phase gate** | `./gradlew check` (adds detekt, ktlintCheck, `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck` (new, 12-04), `audioFocusLiteralCheck`, and `:domain:depthSweep` ≈ 11 min) plus one `./gradlew :app:assembleRelease` for the R8 `missing_rules.txt` check |
| **Estimated runtime** | quick filters: seconds; full suite: minutes (no measured figure — build reports are stale); `check`: ~11 min dominated by `depthSweep` |

---

## Sampling Rate

- **After every task commit:** Run the narrowest `--tests` filter for the class touched (listed per task below).
- **After every plan wave:** Run `./gradlew :domain:test :app:testDebugUnitTest`.
- **Before `/gsd:verify-work`:** `./gradlew check` green (accept the ~11-minute `depthSweep`) and `./gradlew :app:assembleRelease` with no `play-services-games` entries in `missing_rules.txt`.
- **Max feedback latency:** ~60 s for a quick filter; never use watch-mode flags.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | PGSA-06 | T-12-01 | `play_session` added without touching `level_result`; `2.json` exported | integration (Robolectric) | `./gradlew :app:kspDebugKotlin :app:copyRoomSchemas && ./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"` | ✅ class exists | ⬜ pending |
| 12-01-02 | 01 | 1 | PGSA-06 | T-12-02 / T-12-03 | `MIGRATION_1_2` DDL copied from `2.json`; registered via `addMigrations` | integration | `./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"` | ✅ | ⬜ pending |
| 12-01-03 | 01 | 1 | PGSA-06 | T-12-01 / T-12-02 | v1 rows survive `runMigrationsAndValidate`; production builder has the migration | integration | `./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"` | ❌ W0 (cases) | ⬜ pending |
| 12-02-01 | 02 | 1 | PGSA-01, PGSA-02, PGSA-03 | — | `SolveTelemetry.coerced` clamps negatives; no `android.*` import in `:domain` | unit + arch | `./gradlew :domain:test --tests "*SolveTelemetryTest*" --tests "*AchievementCatalogTest*" && ./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*"` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 1 | PGSA-02, PGSA-03 | — | `evaluate` never emits a step ≤ 0; tiered ids never in `unlocked` | unit (TDD) | `./gradlew :domain:test --tests "*AchievementRulesTest*"` | ❌ W0 | ⬜ pending |
| 12-03-01 | 03 | 1 | PGSL-04, PGSL-06, PGSA-05 | T-12-3x | Seam returns `Boolean`, never throws; no gms/Activity type in the seam | compile + arch | `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*"` | ✅ (auto) | ⬜ pending |
| 12-03-02 | 03 | 1 | PGSL-04 | T-12-3x | `NoOp` and `PlayGamesService` return `false` when signed out; no logging in `platform/games` | unit | `./gradlew :app:testDebugUnitTest --tests "*NoOpGamesService*" --tests "*PlayGamesServiceTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-03-03 | 03 | 1 | PGSL-06, PGSA-05 | — | Rows present only when `SignedIn`; taps reach the seam; no ID in `ui/` | Robolectric Compose | `./gradlew :app:testDebugUnitTest --tests "*SettingsPlayGames*" --tests "*SettingsViewModelTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-04-01 | 04 | 2 | PGSL-01..03, PGSL-06, PGSA-05 | T-12-41 | Release fails on any missing key; debug never fails; `UNCONFIGURED-` treated as absent | unit (build-logic, TDD) | `./gradlew -p build-logic test --tests "*GamesIdResolutionTest*"` | ❌ W0 | ⬜ pending |
| 12-04-02 | 04 | 2 | PGSL-01..03 | T-12-41 / T-12-43 | 27 `buildConfigField`s; `verifyReleaseGamesIds`; `gamesIdLiteralCheck` gate | build | `./gradlew :app:compileDebugKotlin && ./gradlew gamesIdLiteralCheck` | n/a | ⬜ pending |
| 12-04-03 | 04 | 2 | PGSL-03, PGSA-05 | T-12-43 | `GamesIds` reads only `BuildConfig`; `forClassic` maps the five ids | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*BuildConfigIdsTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-05-01 | 05 | 2 | PGSA-02 | T-12-5x | Old snapshot blobs read counters as 0 (fail-closed) | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapper*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-05-02 | 05 | 2 | PGSA-02 | T-12-5x | Counters increment only on applied assists; declined offer counts nothing; restart resets | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*GameViewModel*" --tests "*GameFlowTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-05-03 | 05 | 2 | PGSA-02 | T-12-06 | Nav args defaulted and clamped via `SolveTelemetry.coerced`; never crash | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-06-01 | 06 | 3 | PGSL-01..03, PGSA-02, PGSA-03 | — | Bound `:mode` params (no SQL literals); Room KSP validates queries | compile | `./gradlew :app:kspDebugKotlin :app:compileDebugKotlin` | n/a | ⬜ pending |
| 12-06-02 | 06 | 3 | PGSL-01..03, PGSA-03 | T-12-6x (overflow) | `SUM(bestScore)` as `Long?`; empty history → nulls; center predicate English-33 only | integration (in-memory Room, TDD) | `./gradlew :app:testDebugUnitTest --tests "*ProgressAggregateTest*"` | ❌ W0 | ⬜ pending |
| 12-06-03 | 06 | 3 | PGSA-02, PGSL-04 | T-12-6x (torn write) | `level_result` + `play_session` commit atomically; rollback both directions | integration (TDD) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*" --tests "*ResultsViewModelTest*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-07-01 | 07 | 4 | PGSL-05, PGSA-02 | T-12-7x | Pending flag key persisted; `GamesIds.of` bijective over 20 ids | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*SubmissionSyncStore*" --tests "*GamesIds*"` | ❌ W0 | ⬜ pending |
| 12-07-02 | 07 | 4 | PGSL-01..05, PGSA-02, PGSA-03 | T-12-7x | Flag set when signed out or any call fails; cleared only on all-success; mutex-serialised; zero-call pass never clears | unit (TDD) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterTest*"` | ❌ W0 | ⬜ pending |
| 12-07-03 | 07 | 4 | PGSL-01..04 | T-12-7x | `onResultRecorded()` exactly once after `recordResult`; not on stuck path; throwing submitter never alters `NewBests`; `start()` in `MainActivity` | unit (TDD) + arch | `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest*" --tests "*PlatformBoundary*"` | ✅ exists, ❌ cases | ⬜ pending |
| 12-08-01 | 08 | 5 | PGSA-01, PGSA-04 | T-12-08-01 | Checklist holds key names only — `grep -c 'CgkI'` prints 0 | doc grep | `grep -c '^| A[0-9][0-9] |' 12-OPERATOR-CHECKLIST.md` = 20; `grep -c '^| L[0-9] |'` = 7; `grep -c 'CgkI'` = 0 | n/a | ⬜ pending |
| 12-08-02 | 08 | 5 | PGSA-04, PGSL-06, PGSA-05 | T-12-08-02 | Six device tests appended; R8 check documented | doc grep | `grep -n "## Phase 12" .planning/phases/11-play-games-session/11-HUMAN-UAT.md` | n/a | ⬜ pending |
| 12-08-03 | 08 | 5 | PGSA-01 | — | ROADMAP icon row = 20; plan list finalised | doc grep | `grep -n "20 unique 512" .planning/ROADMAP.md` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Sampling continuity: no task lacks an automated verify; no three consecutive tasks rely on the same class filter without a new assertion.

---

## Wave 0 Requirements

New test files are created by the task that introduces the code they cover (no separate Wave 0 plan):

- [ ] `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt` — v1→v2 survival case + registration case (12-01-03; class exists)
- [ ] `domain/src/test/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetryTest.kt` and `.../achievement/AchievementCatalogTest.kt` (12-02-01)
- [ ] `domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRulesTest.kt` (12-02-02)
- [ ] `app/src/test/java/com/app/pegsolitaire/platform/games/FakeGamesService.kt` — extended into a recording fake with settable results (12-03-02)
- [ ] `build-logic/src/test/kotlin/GamesIdResolutionTest.kt` (12-04-01; `AdIdResolutionTest` is the template)
- [ ] `app/src/test/java/com/app/pegsolitaire/data/ProgressAggregateTest.kt` (12-06-02; `ProgressRepositoryTest` is the template)
- [ ] `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt` (12-07-02)
- [ ] No framework install needed — every library is already on the test classpath.

---

## Manual-Only Verifications

All require a physical device or emulator signed into a Play Console project that does not yet exist (Phase 11 D-04). They live in `11-HUMAN-UAT.md` (extended by 12-08-02).

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Stock achievement unlock popup appears on first solve | PGSA-04 | v2 exposes no popup API; rendering is SDK-owned | Solve any level signed in; observe popup over Results |
| "Leaderboards" row opens the all-leaderboards picker; "Achievements" row opens the achievements UI | PGSL-06, PGSA-05 | Stock Google Activity | Settings → Play Games → tap each row |
| A real score lands on ladder, cumulative, and per-board leaderboards | PGSL-01..03 | Requires live Console leaderboards | Solve; open leaderboard picker; confirm entries |
| Offline solves are submitted once after later sign-in | PGSL-05 | Requires live PGS session | Play signed out, sign in, confirm bests appear once |
| Both rows hidden when signed out | D-15 | Visual check on device (also covered by Robolectric test) | Sign out; confirm section shows only the sign-in row |
| Minified release has no PGS keep-rule gaps | Pitfall 12 | Needs `assembleRelease` output inspection | `./gradlew :app:assembleRelease`; inspect `missing_rules.txt` |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (created in-plan by the introducing task)
- [x] No watch-mode flags
- [x] Feedback latency < 60s for quick filters
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-09 (plan-phase orchestrator, derived from RESEARCH Validation Architecture)
