---
phase: 05-persistence-resume-settings
plan: 03
subsystem: data
tags: [repository, room, kotlinx-serialization, resume, defensive-deserialization, mockk, tdd]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    plan: 01
    provides: "PegSolitaireDatabase v1, LevelResultDao/ResumeDao + entities, ProgressPersistenceKeys, DatabaseModule"
  - phase: 01-domain-foundation-playable-board
    provides: "pure-domain Board/BoardSpec/Move/Position + RulesEngine.applyMove/legalMoves (the replay substrate)"
  - phase: 02-navigation-onboarding
    provides: "SettingsRepository interface + Impl + @Binds shape mirrored here"
provides:
  - "`ProgressRepository` — single source of truth for per-level bests (PERS-01) and per-mode resume snapshots (PERS-03), Hilt-bound"
  - "`NewBests` / `LevelResult` app-layer read models (no Room type reaches the UI)"
  - "`ResumeSnapshotDto` — the only @Serializable type in the resume path"
  - "`ResumeSnapshotFormat` + tuple codec — scoped on-disk index constants"
  - "`SnapshotMapper` — DTO ⇄ domain, rebuild-by-replay with per-move legality re-validation"
  - "`RestoredGame` / `ChallengeStamp` — the restore result consumed by 05-07"
  - "MockK wired into `:app` unit tests (first use)"
affects: [05-05, 05-06, 05-07, results-new-best-cue, classic-catalog-star-pips, home-continue-tiles]

# Tech tracking
tech-stack:
  added: ["io.mockk:mockk 1.14.2 (testImplementation, catalog-pinned — first use in :app)"]
  patterns:
    - "Repository owns the per-field best merge inside `db.withTransaction`; the DAO stays a dumb store"
    - "Serialized state is never trusted: decode → structural validation → per-move `RulesEngine.legalMoves` re-check → null on any failure"
    - "`@Serializable` confined to the app-layer DTO; domain types are mapped explicitly (BoardSpec is not a data class)"
    - "On-disk tuple indices live in a scoped constants object next to the DTO, never as inline literals"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/ResumeRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
    - app/build.gradle.kts

key-decisions:
  - "Restore validates **every** stored move against `RulesEngine.legalMoves` before applying it — a forged history is rejected wholesale, not partially applied"
  - "`completedAt` advances only when a best actually improved, honouring the entity's documented 'last improved' contract (RESEARCH Pattern 1 wrote it unconditionally)"
  - "`loadSnapshot` also rejects a payload whose own `mode` differs from the row key it was filed under"
  - "`ChallengeStamp(level, generatorVersion)` replaces two loose nullable params — the pair is meaningless apart, and it keeps `toDto` under detekt's LongParameterList threshold"
  - "Tuple codec helpers live in `ResumeSnapshotDto.kt` next to `ResumeSnapshotFormat` (format concern), keeping `SnapshotMapper` under detekt's TooManyFunctions ceiling"
  - "`saveSnapshot(dto, nowMs = System.currentTimeMillis())` — defaulted clock param keeps the plan's call shape while staying deterministic in tests"

patterns-established:
  - "Untrusted-blob pipeline: `Json { ignoreUnknownKeys = true }` + try/catch → null, then structural validation, then domain-authority re-validation"
  - "Board-state assertions compare (footprint, target, peg mask) rather than `Board ==` — `BoardSpec` has identity equality, so a rebuilt board can never be `==`"

requirements-completed: [PERS-01, PERS-03]

# Metrics
duration: 20min
completed: 2026-08-25
---

# Phase 5 Plan 03: Progress + Resume Data Layer Summary

**`ProgressRepository` merges each per-level best independently inside a Room transaction and reports which fields improved, while the resume codec stores the initial footprint plus the full move history so a restore rebuilds the exact board by replay — undo intact, generator never called, and every corrupt or forged blob failing safe to "no resume".**

## Performance

- **Duration:** ~20 min
- **Tasks:** 2 (both TDD: RED → GREEN)
- **Files modified:** 8 (6 created, 2 modified)
- **Tests added:** 25 (11 mapper + 7 results + 7 resume), all green

## Accomplishments

- **Undo survives a resume.** The snapshot stores `initialPegs` + the complete move list, and `SnapshotMapper.fromDto` folds `RulesEngine.applyMove` over it exactly as `GameViewModel.onUndo` does. `restoredHistoryStillSupportsUndoByReplay` proves the restored history replays back to the pre-last-move board — this is the whole point of PERS-03 and it is now locked by a test.
- **The board is never regenerated (D-06).** A MockK spy proves `applyMove` is called exactly once per stored move, and a deliberately mismatched Challenge stamp (`challengeLevel = 7` over a Classic English-33 footprint) still restores the Classic board — if anything regenerated from the level number, that test fails.
- **Nothing read back from disk is trusted.** Beyond the mandated try/catch decode, the restore rejects: malformed tuples, an empty/oversized/duplicated footprint, a peg mask with bits outside the footprint, a half-specified or off-board target, **and any stored move that is not legal on the board it claims to act upon** (T-05-04). Seven distinct fail-safe cases are asserted; all return `null`, none throw.
- **Bests genuinely merge per field (D-07).** A slow perfect run keeps its score while the earlier fast run keeps its time — asserted in both directions, plus a "worse replay regresses nothing and moves no timestamp" case. `NewBests` reports which fields improved, which is exactly what 05-05's "New best!" cue needs.
- **The UI can never see a Room type.** `observeResults` maps entities to a plain `LevelResult`; `NewBests`, `ResumeSnapshotDto` and `RestoredGame` are all app-layer models.

## Task Commits

1. **Task 1: ResumeSnapshotDto + SnapshotMapper** — `e801876` (test, RED) → `f84cfa9` (feat, GREEN)
2. **Task 2: ProgressRepository over Room, Hilt-bound** — `fa63f57` (test, RED) → `3d0a606` (feat, GREEN)

No REFACTOR commits: the detekt-driven restructuring in Task 1 (see Deviation #2) happened before the GREEN commit, so both implementations landed at their final shape.

## Exported Signatures (for 05-05 / 05-06 / 05-07)

```kotlin
// data/ProgressRepository.kt
interface ProgressRepository {
    suspend fun recordResult(mode: String, levelId: String, result: ScoreResult, nowMs: Long): NewBests
    fun observeResults(mode: String): Flow<List<LevelResult>>
    suspend fun saveSnapshot(dto: ResumeSnapshotDto, nowMs: Long = System.currentTimeMillis())
    suspend fun loadSnapshot(mode: String): ResumeSnapshotDto?   // null = no resume (also on corrupt blob)
    suspend fun clearSnapshot(mode: String)
    fun observeResumeModes(): Flow<List<String>>
}

data class NewBests(val stars: Boolean, val time: Boolean, val score: Boolean) {
    val any: Boolean get() = stars || time || score          // whether to show the D-09 cue
}

data class LevelResult(
    val mode: String, val levelId: String,
    val stars: Int, val bestTimeMs: Long, val bestScore: Int, val completedAt: Long,
)

// data/resume/SnapshotMapper.kt
object SnapshotMapper {
    fun toDto(
        mode: String, levelId: String,
        initialBoard: Board, moveHistory: List<Move>, elapsedMs: Long,
        challenge: ChallengeStamp? = null,
    ): ResumeSnapshotDto
    fun fromDto(dto: ResumeSnapshotDto, rulesEngine: RulesEngine): RestoredGame?   // null = do not resume
    fun encode(dto: ResumeSnapshotDto): String
    fun decode(blob: String): ResumeSnapshotDto?
}

data class ChallengeStamp(val level: Int, val generatorVersion: Int)   // Challenge-only provenance
data class RestoredGame(
    val initialBoard: Board,     // seed `GameViewModel.initialBoard` with this
    val currentBoard: Board,     // seed the UiState board with this
    val moveHistory: List<Move>, // seed `moveHistory` with this → undo works immediately
    val elapsedMs: Long,
    val challenge: ChallengeStamp?,
)
```

**Usage note for 05-07:** `ResumeSnapshotDto` holds two `IntArray` properties, so its generated `equals`/`hashCode` compare array *identity*. Compare snapshots via `SnapshotMapper.encode(dto)`, never with `==`.

## Files Created/Modified

**Created (main)**
- `data/ProgressRepository.kt` — the interface, `NewBests`/`LevelResult` read models, `DefaultProgressRepository` (`withTransaction` best merge, defensive snapshot load), and a private `ProgressConstants` scoped-constants object
- `data/resume/ResumeSnapshotDto.kt` — the `@Serializable` DTO, `ResumeSnapshotFormat` (on-disk tuple indices + the 64-hole `Long`-mask ceiling), and the four `internal` tuple codec functions
- `data/resume/SnapshotMapper.kt` — `ChallengeStamp`, `RestoredGame`, and the `SnapshotMapper` object (`toDto`/`fromDto`/`encode`/`decode` + private validation and replay helpers)

**Created (tests)**
- `data/resume/SnapshotMapperTest.kt` — 11 pure-JVM cases: round-trip, undo-by-replay, spy-verified one-`applyMove`-per-move, provenance-only Challenge stamp, corrupt blob, unknown-key forward-compat, forged move, malformed tuples, out-of-footprint peg mask, bad target, duplicate footprint
- `data/ProgressRepositoryTest.kt` — 7 Robolectric + in-memory-Room cases over the per-field merge and `NewBests` flags, plus per-mode row isolation and the `observeResults` re-emission (D-10)
- `data/ResumeRepositoryTest.kt` — 7 cases: save→load→rebuild, replace-per-mode, clear-one-leaves-the-other, `observeResumeModes`, corrupt blob → null, mode-mismatched blob → null, empty → null

**Modified**
- `di/DataModule.kt` — `@Binds @Singleton bindProgressRepository`, next to `bindSettingsRepository`
- `app/build.gradle.kts` — `testImplementation(libs.mockk)` (catalog entry already existed; this is its first consumer)

## Decisions Made

- **Move legality is re-checked on restore, not just structure.** RESEARCH §V5 asked for a defensive decode; the plan's threat register (T-05-04) also demanded move validation. The replay short-circuits to `null` the instant a stored move is illegal on the board it claims to act on, so a tampered history is rejected in full rather than partially applied.
- **`completedAt` only moves when a best improves.** RESEARCH Pattern 1 wrote `completedAt = nowMs` unconditionally, but 05-01's `LevelResultEntity` KDoc defines the column as "epoch millis of the play that last improved any best". The entity's documented contract wins — it is the on-disk meaning, and 05-05 can trust the column.
- **`ChallengeStamp` groups level + generator version.** They are meaningless apart (a level number without the generator contract that produced it cannot be trusted to mean the same board later), and pairing them keeps `toDto` inside detekt's parameter budget.
- **Board equality is asserted by content.** `BoardSpec` is not a data class, so a rebuilt `Board` can never be `==` to the original. Tests compare footprint + target + peg mask — sound precisely because `BoardSpec` assigns bit indices by sorting `(row, col)`, making the mask reproducible for an identical footprint. Worth remembering in every later plan that asserts on a restored board.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**
- **Found during:** setup (first Gradle invocation)
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every Gradle command with "SDK location not found".
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk`.
- **Files modified:** `local.properties` (gitignored — intentionally **not** committed)
- **Committed in:** n/a

**2. [Rule 3 - Blocking] Restructured the mapper to satisfy detekt's `TooManyFunctions` / `LongParameterList`**
- **Found during:** Task 1 (verification, before the GREEN commit)
- **Issue:** The plan's literal shapes break the mandatory standards gate: `toDto(mode, levelId, challengeLevel, generatorVersion, initialBoard, moveHistory, elapsedMs)` is 7 params (detekt `functionThreshold: 6`), and the mapper object with all its validation helpers held 13 functions (`thresholdInObjects: 11` — `:app:detekt` failed with exactly this).
- **Fix:** `challengeLevel`/`generatorVersion` were folded into one `ChallengeStamp?` param (6 → within budget), and the four tuple codec functions moved to file level in `ResumeSnapshotDto.kt` beside `ResumeSnapshotFormat`, where they belong as format concerns. `SnapshotMapper` is now 10 functions.
- **Files modified:** `data/resume/SnapshotMapper.kt`, `data/resume/ResumeSnapshotDto.kt`
- **Verification:** `./gradlew :app:detekt :app:ktlintCheck` → BUILD SUCCESSFUL
- **Committed in:** `f84cfa9`

**3. [Rule 2 - Missing Critical] Extra validation beyond the plan's list**
- **Found during:** Task 1 (implementing the restore)
- **Issue:** Structural checks alone still admit states that corrupt the `Board` invariants: a footprint longer than 64 holes silently breaks the `Long` bit mask, duplicate positions make `BoardSpec.size` disagree with the stored footprint, a peg mask with bits past `spec.size` is garbage, and a target outside the footprint is meaningless.
- **Fix:** All four rejected in `toBoardSpecOrNull`/`toInitialBoardOrNull`; each is covered by a test.
- **Files modified:** `data/resume/SnapshotMapper.kt`, `data/resume/SnapshotMapperTest.kt`
- **Committed in:** `e801876` (tests) / `f84cfa9` (impl)

**4. [Rule 2 - Missing Critical] `loadSnapshot` rejects a mode-mismatched payload**
- **Found during:** Task 2
- **Issue:** A tampered row could file a Challenge payload under the Classic key, resuming the player into the wrong mode's game.
- **Fix:** `?.takeIf { it.mode == mode }` after the decode; asserted by `aBlobClaimingAnotherModeIsRejected`.
- **Files modified:** `data/ProgressRepository.kt`, `data/ResumeRepositoryTest.kt`
- **Committed in:** `fa63f57` (test) / `3d0a606` (impl)

**5. [Rule 3 - Blocking] Added `testImplementation(libs.mockk)` to `:app`**
- **Found during:** Task 1 (RED)
- **Issue:** The plan's no-regeneration assertion needs a spy on `RulesEngine`, which is a Kotlin `object` (final, not subclassable). MockK was declared in `libs.versions.toml` (1.14.2) but wired into no module.
- **Fix:** Added the existing catalog entry as an `:app` test dependency. **No new package was installed** — the version was already pinned in the catalog by an earlier phase and is the RESEARCH-listed test-stack member.
- **Files modified:** `app/build.gradle.kts`
- **Verification:** `spyk(RulesEngine)` + `verify(exactly = 4)` runs green on JDK 17.
- **Committed in:** `e801876`

**6. [Trivial] Mapper test lives in the mirrored package directory**
- The plan listed `app/src/test/java/com/app/pegsolitaire/data/SnapshotMapperTest.kt`; it was created at `.../data/resume/SnapshotMapperTest.kt` so the directory matches its `com.app.pegsolitaire.data.resume` package, mirroring 05-01's `data/db/` tests. The `--tests "*SnapshotMapperTest"` verification command is unaffected.

---

**Total deviations:** 5 auto-fixed (3 blocking, 2 missing-critical) + 1 trivial path change
**Impact on plan:** No scope creep — no UI, ViewModel, or lifecycle wiring was pulled forward. Deviations 3 and 4 harden the plan's own threat register; deviation 2 was required to pass the mandatory standards gate.

## Issues Encountered

- **detekt's thresholds bite at `>=`, not `>`, for parameter lists** (`functionThreshold: 6` rejects a 6-param... actually a 7-param function; a 6-param one passes) and at `> 11` for functions per object. Both were caught by running `:app:detekt` before committing — worth doing on every task in this project rather than relying on the plan's `--tests` command alone.
- **No Robolectric surprises this time.** The in-memory Room + Turbine recipe from 05-01 transferred unchanged; nothing in this plan reads assets, so the wave-1 asset-path caveat did not apply.

## TDD Gate Compliance

Both tasks followed RED → GREEN with commits proving the sequence:
- Task 1: `e801876` (`test`, compile-fails on missing types) → `f84cfa9` (`feat`)
- Task 2: `fa63f57` (`test`, compile-fails on missing types) → `3d0a606` (`feat`)

No test passed unexpectedly during RED. REFACTOR was skipped for both (the Task 1 cleanup landed inside GREEN, before the commit).

## Verification

| Gate | Result |
|------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapperTest" --tests "*DomainPurityTest"` | BUILD SUCCESSFUL (11 + 1 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest" --tests "*ResumeRepositoryTest"` | BUILD SUCCESSFUL (7 + 7 tests, 0 failures) |
| `./gradlew check` (both modules: all unit tests + detekt + ktlint + dpSpLiteralCheck + lint) | BUILD SUCCESSFUL |
| `./gradlew :app:assembleDebug` (Hilt graph resolves the new binding) | BUILD SUCCESSFUL; `DefaultProgressRepository_Factory` generated |
| `grep -rn "@Serializable" domain/src` | no matches — `:domain` stays annotation-free |
| `grep -rn "loadChallenge\|LevelGenerator" app/.../data/resume/` | no matches — the restore cannot regenerate |

## Known Stubs

None. Every declared member of `ProgressRepository`, the DTO, and the mapper is fully implemented and exercised by a test.

The repository has **no callers yet** — that is by design, not a stub: 05-05 (Results "New best!" + catalog pips), 05-06 (Home Continue tiles) and 05-07 (save/restore lifecycle) are the consumers, and this plan is the wave-2 data layer they depend on.

## Threat Flags

None — no new security-relevant surface beyond the plan's register, which is fully addressed:

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-05-03 (corrupt blob crashes restore) | mitigate | Done — `ignoreUnknownKeys` decode in try/catch → null; `loadSnapshot`/`fromDto` fail safe; asserted by 3 tests |
| T-05-04 (forged moves applied unvalidated) | mitigate | Done — every replayed move re-checked via `RulesEngine.legalMoves`; asserted by `aTamperedIllegalMoveYieldsNoResume` |
| T-05-05 (DAO SQL) | mitigate | Unchanged — repository adds no SQL; Room binds `:params` |
| T-05-07 (PII) | accept | Unchanged — stars/time/score and board geometry only, local single-user store |

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready to consume:**
- `ProgressRepository` is injectable today (`@Binds @Singleton` in `DataModule`).
- 05-05 calls `recordResult(mode, levelId, scoreResult, nowMs)` at the Won → Results finalize point and renders the returned `NewBests` (use `.any` to gate the cue); the catalog collects `observeResults(MODE_CLASSIC)` for star pips.
- 05-06 collects `observeResumeModes()` to decide which Home Continue tiles appear (no blob is deserialized for that question).
- 05-07 saves with `SnapshotMapper.toDto(...)` + `saveSnapshot(dto, nowMs)` on `ON_STOP`, and restores with `loadSnapshot(mode)` → `SnapshotMapper.fromDto(dto, rulesEngine)`; seed `initialBoard`, the UiState board, and `moveHistory` from the returned `RestoredGame` so undo works immediately.

**Carry forward:**
1. **A `null` from `loadSnapshot`/`fromDto` means "no resume" and must be handled silently** — drop the Continue affordance, never surface an error or crash. That fail-safe is the whole T-05-03 mitigation.
2. **`clearSnapshot(mode)` is owed on solve, restart and new-game (D-05)** — the repository provides it; wiring it is 05-07's job. Failing to call it leaves a stale Continue tile pointing at a finished board.
3. **Board assertions must compare content, not `Board ==`** (`BoardSpec` has identity equality).
4. Still unaddressed by this plan (unchanged from 05-01): `ChallengeProgress` persistence and the `AppNavHost` hardcoded Challenge entry level.

**Blockers:** None.

## Self-Check: PASSED

- All 6 created files verified present and tracked by `git ls-files`.
- All 4 commit hashes verified present in `git log` (`e801876`, `f84cfa9`, `fa63f57`, `3d0a606`).
- Working tree clean apart from the deliberately gitignored `local.properties`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
