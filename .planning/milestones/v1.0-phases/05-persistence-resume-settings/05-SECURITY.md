---
phase: 05-persistence-resume-settings
audited: 2026-08-26
threats_total: 14
threats_closed: 14
threats_open: 0
status: secured
---

# SECURITY — Phase 05: Persistence, Resume & Settings

**Audited at:** HEAD `a726cbd` (post-review fix commits `cf11498`, `141236e`, `0fe00c6`, `4dcf369`, `aae5b4b` included)
**Audit date:** 2026-08-26
**ASVS level:** default (L1 — local, offline, single-user Android app; no network, auth, or PII surface)
**Threats:** 14 total (13 registered + T-05-13 added during audit) — **14 CLOSED**, **0 OPEN**
**Remediation commits:** `7dbe2de` (T-05-10), `030e8b7` (T-05-13)

Every mitigation below was verified by locating the actual code that performs it. Documentation
and KDoc claims were not accepted as evidence; each row cites `file:line` plus the test that
locks the behaviour.

---

## Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-05-01 | Tampering / data loss | mitigate | CLOSED | `PegSolitaireDatabase.kt:23-27` (`exportSchema = true`, `version = DB_VERSION` = 1); `app/build.gradle.kts:21-24,48,55` (Room plugin `schemaDirectory`, debug-asset copy after `copyRoomSchemas`); `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json` is `git ls-files`-tracked (formatVersion 1, version 1, tables `level_result` + `resume_snapshot`); `DatabaseModule.kt:41-47` builds with `.build()` and **no** destructive fallback — `grep -rn "fallbackToDestructiveMigration" app/` returns 0 matches anywhere in the repo. Test: `MigrationTest.exportedVersion1SchemaOpens` (`MigrationTest.kt:41-44`) opens the committed v1 schema via `MigrationTestHelper`. |
| T-05-02 | DoS (corrupt DataStore read) | mitigate | CLOSED | All five persisted reads fail open: `SettingsRepository.kt:82,87,92` (`.catch { emit(emptyPreferences()) }` on hints/sound/haptics) with defaults `DEFAULT_HINTS/SOUND/HAPTICS = true` (`:51,57,63`); `ChallengeProgress.kt:60-68` (`.catch` + `DEFAULT_CURRENT_LEVEL = 1`, also the `stateIn` initial value); `OnboardingRepository.kt:49`. No unguarded DataStore read exists — `grep -rn "\.catch" app/src/main/java` returns exactly these five sites. Tests: `SettingsRepositoryTest`, `ChallengeProgressTest`. |
| T-05-03 | DoS (corrupt/tampered resume blob) | mitigate | CLOSED | Decode: `SnapshotMapper.kt:59` (`Json { ignoreUnknownKeys = true }`), `:118-123` (`decode` catches `IllegalArgumentException` — the supertype of `SerializationException` — → `null`). Repository fail-safe **and purge**: `ProgressRepository.kt:165-178` — absent row → `null`; undecodable blob → `null`; a blob whose own `mode` ≠ its row key is refused (`?.takeIf { it.mode == mode }`, :172); any unusable row is `resumeDao.clear(mode)`-purged (:176). Consumers: `GameViewModel.kt:290-305` (null restore → purge decoded-but-unreplayable row, then `loadFreshBoard()`), `GameViewModel.kt:361-367` (unknown Classic `levelId` from the blob caught → default board, CR-01 fix `cf11498`), `HomeViewModel.kt:90-119,128-138` (unreadable/stale blob → no Continue tile + purge, WR-02 fix `4dcf369`). Tests: `SnapshotMapperTest.corruptBlobDecodesToNullInsteadOfThrowing`, `.unknownKeysInAStoredBlobAreIgnoredForForwardCompatibility`; `ResumeRepositoryTest.aCorruptStoredBlobLoadsAsNoResumeInsteadOfThrowing`, `.aBlobClaimingAnotherModeIsRejected`, `.purgingACorruptBlobDropsItsModeFromTheObservedResumeModes`, `.aHealthySnapshotIsNeverPurgedByLoading`; `GameViewModelResumeTest.anAbsentSnapshotFallsBackToTheNormalLoad`, `.aTamperedSnapshotFallsBackToTheNormalLoadInsteadOfCrashing`, `.continueWithAnUnknownClassicLevelIdRestoresTheSnapshotInsteadOfCrashing`, `.continueWithAnUnknownClassicLevelIdAndNoSnapshotFallsBackToADefaultBoard`; `HomeViewModelTest.anUnreadableClassicSnapshotSimplyOffersNoContinue`, `.anUnreadableChallengeSnapshotOffersNoContinue`, `.aClassicSnapshotOnAnUnknownBoardOffersNoContinueAndPurgesTheRow`. |
| T-05-04 | Tampering (forged moves) | mitigate | CLOSED | `SnapshotMapper.kt:130-139` — `replayOrNull` folds the stored history and re-checks **every** move with `move in rulesEngine.legalMoves(board)` before `applyMove`; first failure collapses the fold to `null`. `Move`/`Position` are `data class`es (`domain/.../Move.kt:11`, `Position.kt:8`), so membership is value equality, not identity. Structural pre-checks: `:141-166` (arity via `toMoveOrNull`/`toPositionOrNull`, footprint 1..64, no duplicate positions, target inside the footprint, peg mask with no bits outside the footprint via `holdsOnly`, `:179`). Entry-point coverage: `fromDto` is the **only** rebuild path — `grep -rn "SnapshotMapper" app/src/main` shows exactly one caller, `GameViewModel.kt:293`; `HomeViewModel` reads only `levelId`/`challengeLevel` and never rebuilds a board. Tests: `SnapshotMapperTest.aTamperedIllegalMoveYieldsNoResume`, `.structurallyMalformedTuplesYieldNoResume`, `.aPegMaskReachingOutsideTheFootprintYieldsNoResume`, `.aHalfSpecifiedOrOffBoardTargetYieldsNoResume`, `.aFootprintWithDuplicatePositionsYieldsNoResume`; `GameViewModelResumeTest.aTamperedSnapshotFallsBackToTheNormalLoadInsteadOfCrashing`. |
| T-05-05 | Tampering (DAO SQL) | mitigate | CLOSED | Every DAO statement binds parameters and interpolates only compile-time constants: `LevelResultDao.kt:28` (`WHERE mode = :mode AND levelId = :levelId`), `:35` (`WHERE mode = :mode`), `:39` `@Upsert`; `ResumeDao.kt:25`, `:29`, `:33` `@Upsert`, `:37` (`DELETE ... WHERE mode = :mode`). No raw/dynamic SQL anywhere — no `@RawQuery`, `SupportSQLiteQuery`, or string concatenation in a query annotation. Repository adds no SQL (`ProgressRepository.kt` uses DAO methods only). Room verifies the SQL at compile time. |
| T-05-06 | Tampering (edit `app_prefs` on a rooted device) | **accept** | CLOSED | Logged below — see Accepted Risks. |
| T-05-07 | Info disclosure (PII at rest) | **accept** | CLOSED | Logged below — see Accepted Risks. Verified stored fields carry no PII: `LevelResultEntity` (mode/levelId/stars/bestTimeMs/bestScore/completedAt) and `ResumeSnapshotDto` (mode/levelId/challengeLevel/generatorVersion/inPlay/target/initialPegs/moves/elapsedMs). |
| T-05-08 | Tampering (Settings nav route) | **accept** | CLOSED | Logged below — see Accepted Risks. The `Settings` route carries no arguments. |
| T-05-09 | Tampering (double-record) | mitigate | CLOSED | `ResultsViewModel.kt:90-113` — solve gate (`args.pegsRemaining != PEGS_REMAINING_SOLVED → return`, :91), then the `SavedStateHandle`-backed once-only guard read at :92 and **set at :93 before** the write is launched at :103, so a config change (same handle) and a process-death restore (new instance, same handle) both short-circuit. Idempotent even if it did fire twice: `ProgressRepository.kt:127-147` merges per-field inside `database.withTransaction` with `maxOf`/`minOf`. Tests: `ResultsViewModelTest.recreatingTheViewModelOverTheSameSavedStateRecordsOnlyOnce`, `.anUnsolvedPlayRecordsNothing`, `.aStrictlyWorseReplayReportsNoNewBestAtAll`. |
| T-05-10 | DoS (missing/corrupt result read) | mitigate | CLOSED | **Missing-row half — present:** `ClassicCatalogViewModel.kt:68` (`starsByLevelId[descriptor.id] ?: NO_STARS_EARNED`) and `:58` (`initialValue = catalogWith(emptyMap())`) render the full catalog with 0 pips; an out-of-range stored `stars` value cannot over-draw or crash because `BoardPreview.kt:104-105` renders a fixed `repeat(TOTAL_STARS)` with `index < earned`. Test: `ClassicCatalogViewModelTest.anUnplayedCatalogShowsNoEarnedStarsAtAll`. **Corrupt/failing-read half — fixed in `7dbe2de`:** `.catch { emit(emptyList()) }` on `observeResults`/`observeResumeModes`; `loadSnapshot` wrapped in the new `StorageFailSafe.kt` guard (fallback null, `CancellationException` rethrown). Locked by `ProgressRepositoryFailureTest` (throwing-DAO fakes with `SQLiteException`: catalog degrades to empty, Home loads, snapshot load yields null). |
| T-05-11 | Tampering (out-of-range level) | mitigate | CLOSED (residual noted) | `GameViewModel.kt:134-135` — `clampedChallengeLevel = routeChallengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)`. The clamp governs **every load path**: `loadChallengeBoardIfNeeded` (`:320`), `loadFreshBoard` (`:344`), the persistence `mode` derivation (`:168`), the score/ladder-keyed `activeChallengeLevel` (`:152`), and the restored provenance stamp is clamped identically at `:391-393`. A tampered stored `currentLevel` therefore cannot load an out-of-range board. Residual: `WARN-03` below (the public `challengeLevel` getter is unclamped on the non-restore path). |
| T-05-12 | Tampering / data loss (stale Continue) | mitigate | CLOSED | `GameViewModel.kt:555-560` — on `GameStatus.Won`, behind the `finishedEmitted` latch, `clearSnapshot()` runs for both modes; `:770-784` — `resetToInitial()` (the sole restart path, called from `:647` and `:653`) clears the snapshot; `:426-429` — `onLeaveScreen` returns early unless `status == GameStatus.Playing` **and** `initialBoard != null`, so a Won/Stuck board and an in-flight restore/generation can never write a snapshot (WR-01 fix `0fe00c6`). Tests: `GameViewModelResumeTest.solvingClearsTheModesSnapshot`, `.restartingClearsTheModesSnapshot`, `.leavingASolvedGameSavesNothing`, `.leavingDuringAnInFlightRestoreNeverOverwritesTheStoredSnapshot`, `.leavingAPlayingGameSavesTheHistoryAndTheElapsedTime`; `HomeViewModelTest.clearingASnapshotWhileOnHomeDropsItsContinueAffordance`. |
| T-05-SC | Tampering (supply chain) | **accept** | CLOSED | Logged below — see Accepted Risks. Room 2.8.4 / DataStore / kotlinx-serialization are first-party artifacts on Google Maven / Maven Central, pinned in `gradle/libs.versions.toml`. No new third-party dependency was added in this phase (05-04 explicitly avoided `material-icons-extended`). |

---

## OPEN Threats

None — OPEN-01 (T-05-10 corrupt-read half) was remediated in commit `7dbe2de` on 2026-08-26,
same-day as the audit. The read flows now fail open and the suspend DAO reads degrade to
"no result / no resume" via the shared `storageFailSafe` guard (`data/StorageFailSafe.kt`).

## Accepted Risks Log

| Threat ID | Risk | Rationale | Scope / expiry |
|-----------|------|-----------|----------------|
| T-05-06 | A player with root (or an emulator/backup extraction) edits `app_prefs` to inflate `challenge_current_level` and skip ahead in the Challenge ladder. | Local, single-user, offline game. There is no server, no leaderboard, and no cross-player comparison this phase, so the value has no integrity requirement — the player is only cheating themselves. Challenge levels are deterministically generated, so every level is reachable anyway. Tamper-proofing a private int would add cost with zero security value. | Re-evaluate when Play Games leaderboards / cloud save land (Slice 2): a server-visible score derived from this int changes the trust model. |
| T-05-07 | Personally identifiable information exposed in stored results or the resume snapshot. | Verified by field inspection, not by claim: `LevelResultEntity` stores `mode`, `levelId`, `stars`, `bestTimeMs`, `bestScore`, `completedAt`; `ResumeSnapshotDto` stores `mode`, `levelId`, `challengeLevel`, `generatorVersion`, board footprint, peg bitmask, move tuples, `elapsedMs`. No identifier, account, contact, or location field exists in either. Storage is app-private and never leaves the device. | Re-evaluate if a Play Games account id or any user-entered name is ever persisted. |
| T-05-08 | The new `Settings` navigation route widens the app's input surface. | The route is argument-free (`Settings` is a bare typed destination); nothing untrusted crosses it. Its writes go through `SettingsRepository`, whose reads fail open (T-05-02). It is a local UI-only screen with no deep link registered. | Re-evaluate if the route ever takes arguments or is exposed as a deep link / app-link. |
| T-05-SC | Supply-chain compromise of the persistence dependencies (Room 2.8.4, DataStore 1.2.1, kotlinx-serialization-json 1.11.0). | All are first-party AndroidX / JetBrains artifacts resolved from Google Maven and Maven Central, version-pinned in `gradle/libs.versions.toml` (single source of truth, no dynamic `+` ranges). This phase introduced no third-party dependency — 05-04 deliberately avoided `material-icons-extended`. Typosquat scanners (slopcheck et al.) target npm/PyPI/crates and are N/A for Google Maven coordinates. | Re-evaluate on any dependency bump or the first non-first-party artifact added to the catalog. |

---

## Unregistered Flags (WARNING — not ship blockers)

New attack surface or residual exposure found during verification that no threat register entry maps to.

### WARN-01 — Persistence **write** paths have no failure handling — RESOLVED as T-05-13 (`030e8b7`)
The register's DoS entries (T-05-02, T-05-03, T-05-10) all reason about *reads*. Every write is
unguarded:
- `SettingsRepository.kt:95-105` — three `dataStore.edit { }` calls with no `try`; called from
  `SettingsViewModel` / `GameViewModel.kt:461` inside `viewModelScope.launch`.
- `ChallengeProgress.kt:80-100` — `advancePast` / `reset` launch `dataStore.edit` on the injected
  `@ApplicationScope` (`DataModule.kt:89`: `CoroutineScope(SupervisorJob() + Dispatchers.Default)`).
  `SupervisorJob` isolates child failure but does **not** swallow it: an uncaught `IOException`
  still reaches the default handler.
- `GameViewModel.kt:445,450` and `ResultsViewModel.kt:103` — Room writes in `viewModelScope.launch`.

Impact: a disk-full or I/O failure while toggling a setting, advancing the ladder, or backgrounding
a game crashed the app instead of dropping the write.

**Resolution (`030e8b7`):** registered as **T-05-13 — DoS, persistence write failure — mitigate — CLOSED**.
`recordResult` guarded at the transaction boundary (fallback `NewBests(false,false,false)` — an
individual `find` fallback inside the transaction would clobber better stored bests);
`saveSnapshot`/`clearSnapshot` writes dropped on failure; `SettingsRepository` setters and
`ChallengeProgress.advancePast`/`reset` `dataStore.edit` calls wrapped. `CancellationException`
is always rethrown. Locked by 11 throwing-fake tests across `ProgressRepositoryFailureTest`,
`SettingsRepositoryTest`, `ChallengeProgressTest` (suite 282 → 293, green).

### WARN-02 — Snapshot footprint coordinates are unbounded (review finding IN-03)
`SnapshotMapper.kt:146-161` validates tuple arity, footprint size (1..64), duplicates, target
membership and peg-mask width, but never the coordinate *values*. A tampered blob containing
`[-5, 3]` or `[1073741824, 0]` passes validation (`BoardSpec` imposes no constraints), producing a
degenerate off-canvas board. No crash was reproduced, so this does not open T-05-04 (whose declared
mitigation — move re-validation — is fully present); it is residual tamper surface the register
does not name. Suggested fix: bound row/col in `toPositionOrNull()` against a named constant.

### WARN-03 — The public `challengeLevel` getter is unclamped (review finding IN-04)
`GameViewModel.kt:125` exposes `challengeLevel = stampedChallengeLevel ?: routeChallengeLevel` — the
**raw** route arg when no restore stamp exists — and `GameRoute.kt:69,88` passes it into the
`Results` route and the screen title. The load path is clamped (T-05-11 CLOSED), but an
out-of-range value would be recorded in the DB under the raw level id and drive "Next level" as
`raw + 1`, while the board actually played was the clamped one. Bounded impact (the next entry is
re-clamped at load), and unreachable today because Home always supplies a persisted in-range level —
but it breaks the "the clamp is the single boundary" invariant T-05-11 relies on. Suggested fix:
have `GameRoute` read the clamped value for both the title and the Results route.

### WARN-04 — `05-02-SUMMARY.md` has no `## Threat Flags` section
Task 05-02's summary omits the section entirely (`grep -c "Threat" 05-02-SUMMARY.md` → 0), so the
executor's new-attack-surface declaration is missing for the task that introduced the
`ChallengeProgress` DataStore reads and writes. That task's threats (T-05-02, T-05-06) were
verified directly against code here, so nothing is unverified — but the summary cannot be relied on
as a complete surface inventory for that task.

---

## Notes on verification method

- Implementation files were read only; nothing outside this document was created or modified.
- Every `mitigate` row required a located call site, not a KDoc claim: `ProgressRepository.kt:109-111`,
  `SnapshotMapper.kt:38-55` and `HomeViewModel.kt:33-43` all *describe* fail-safe behaviour; the
  status above rests on the executable lines cited, and where documentation over-claimed
  ("never crashes the catalog") the gap is reported as OPEN-01 rather than accepted.
- Multi-entry-point checks performed: all five DataStore reads (T-05-02), the single `fromDto`
  rebuild path (T-05-04), all six DAO statements (T-05-05), both restart callers and the Won latch
  (T-05-12), and all load paths reading the clamp (T-05-11).
- Phase test suite reported green at 112/112 across 16 classes (`05-VERIFICATION.md:109`); test names
  are cited above as behavioural locks on each mitigation.

---

## Security Audit 2026-08-26

| Metric | Count |
|--------|-------|
| Threats found | 14 (13 plan-time register + T-05-13 added during audit) |
| Closed | 14 |
| Open | 0 |

Initial audit at HEAD `a726cbd` found 12/13 closed, 1 open (T-05-10 corrupt-read half) and flagged
WARN-01. Both were remediated same-day (`7dbe2de`, `030e8b7`) and verified: full `./gradlew check`
green, 293 tests. WARN-02 (footprint coordinate bounds) and WARN-03 (unclamped `challengeLevel`
getter on the non-restore path) remain logged advisories with bounded impact; WARN-04 is a
documentation gap in `05-02-SUMMARY.md` with all its threats verified directly against code.
