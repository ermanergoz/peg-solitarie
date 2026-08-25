---
phase: 05
slug: persistence-resume-settings
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-13
---

# Phase 05 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Robolectric (JVM-side); Turbine + Truth + MockK; Room `MigrationTestHelper` (androidx.room:room-testing) |
| **Config file** | `app/build.gradle.kts` (test deps + Room schema export/asset dirs) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*.data.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest ktlintCheck detekt` |
| **Estimated runtime** | ~90 seconds (Robolectric warm) |

---

## Sampling Rate

- **After every task commit:** Run the quick command scoped to the touched package
- **After every plan wave:** Run the full suite command
- **Before `/gsd:verify-work`:** Full suite must be green (tests + ktlint + detekt)
- **Max feedback latency:** ~90 seconds

---

## Per-Task Verification Map

> Populated by the planner as PLAN.md tasks are authored. Each persistence task must map to a Room DAO / DataStore / repository / migration test with a concrete `./gradlew` command. Verified against RESEARCH.md §Validation Architecture.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01 | 01 | 1 | PERS-01 | — | Room DB + schema export builds; schema JSON emitted to committed VCS dir | unit | `./gradlew :app:testDebugUnitTest --tests "*PegSolitaireDatabaseTest"` | ❌ W0 | ⬜ pending |
| 05-01 | 01 | 1 | PERS-01/03 | — | v1 schema opens under `MigrationTestHelper` (no destructive fallback) | unit | `./gradlew :app:testDebugUnitTest --tests "*MigrationTest"` | ❌ W0 | ⬜ pending |
| 05-01 | 01 | 1 | — | T-05-01 | `NoOpGamesService.signIn()` returns false; bound in Hilt | unit | `./gradlew :app:testDebugUnitTest --tests "*NoOpGamesServiceTest"` | ❌ W0 | ⬜ pending |
| 05-02 | 02 | 1 | SET-01,SET-02 | — | soundOn/hapticsOn persist + fail-open default true | unit | `./gradlew :app:testDebugUnitTest --tests "*SettingsRepositoryTest"` | ✅ | ⬜ pending |
| 05-02 | 02 | 1 | PERS-02,CHAL-05 | — | currentLevel persists in shared app_prefs; survives process death | unit | `./gradlew :app:testDebugUnitTest --tests "*ChallengeProgressTest"` | ❌ W0 | ⬜ pending |
| 05-03 | 03 | 2 | PERS-01 | — | Upsert keeps best stars(max)/time(min)/score(max) per (mode,levelId) | unit | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest"` | ❌ W0 | ⬜ pending |
| 05-03 | 03 | 2 | PERS-03 | T-05-03 | Resume snapshot round-trips board+full moveHistory+elapsed, NO regeneration; corrupt blob → null (fail-safe) | unit | `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapperTest" --tests "*DomainPurityTest"` | ❌ W0 | ⬜ pending |
| 05-04 | 04 | 2 | SET-01,SET-02 | — | Settings toggles persist; haptic fires on jump only when enabled | unit | `./gradlew :app:testDebugUnitTest --tests "*SettingsViewModelTest"` | ❌ W0 | ⬜ pending |
| 05-05 | 05 | 3 | PERS-01 | — | Solve records best; "New best!" cue; catalog star pips from LevelResult | unit | `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest"` | ❌ W0 | ⬜ pending |
| 05-06 | 06 | 3 | PERS-02,CHAL-05 | — | Challenge solve advances persisted currentLevel; Home enters at stored level (hardcoded 1 removed) | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelChallengeAdvanceTest"` | ❌ W0 | ⬜ pending |
| 05-07 | 07 | 4 | PERS-03 | — | onStop saves (Playing only); Continue restores exact board+undo history+elapsed by replay; cleared on solve/restart | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelResumeTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Room 2.8.4 deps (`room-runtime`, `room-ktx`, `ksp(room-compiler)`, `room-testing`) + `room` gradle plugin wired in `libs.versions.toml` / `app/build.gradle.kts`
- [ ] Room schema export dir configured (`room { schemaDirectory(...) }`) and committed to VCS
- [ ] Robolectric access to exported schema assets resolved (RESEARCH Open-Q1 — spike the v1 `MigrationTestHelper.createDatabase` test first)
- [ ] Test stubs authored for: `ProgressRepository`, resume round-trip, `ChallengeProgress` wiring, Room `Migration`

*Existing DataStore/`SettingsRepository` infrastructure covers the settings slice.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real haptic feedback felt on peg jump (D-15) | SET-02 | Physical haptic actuation cannot be asserted JVM-side | On device: enable Haptics in Settings, perform a jump → feel light haptic; disable → no haptic |
| Full app-restart resume of an in-progress game | PERS-03 | Process-death / cold-start lifecycle is integration-level | Start a game, make moves, background app (onStop), kill process, relaunch → Home Continue restores exact board + undo history + elapsed |

*Remaining phase behaviors have automated JVM verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-07-13 (plan-check passed; every executable task carries a scoped `<automated>` verify)
