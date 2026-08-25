---
phase: 05-persistence-resume-settings
plan: 02
subsystem: database
tags: [datastore, preferences, kotlin, coroutines, stateflow, hilt, settings, challenge-progress]

# Dependency graph
requires:
  - phase: 02-core-gameplay-loop
    provides: SettingsRepository + shared app_prefs DataStore (hintsEnabled fail-open pattern)
  - phase: 03-challenge-mode
    provides: the in-memory ChallengeProgress seam (currentLevel/advanceToNext/reset)
provides:
  - Persisted soundOn flag (SET-01) with fail-open ON default
  - Persisted hapticsOn flag (SET-02) with fail-open ON default
  - DataStore-backed ChallengeProgress.currentLevel that survives process death (PERS-02, CHAL-05 data half)
  - "@ApplicationScope qualifier + app-lifetime CoroutineScope provider in DataModule"
affects: [05-04-settings-ui, 05-06-challenge-continue-wiring, haptics-on-jump]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Fail-open DataStore read: .catch { emit(emptyPreferences()) }.map { it[KEY] ?: DEFAULT }"
    - "stateIn(scope, SharingStarted.Eagerly, DEFAULT) to keep a synchronous StateFlow.value over an async store"
    - "@ApplicationScope-qualified CoroutineScope for fire-and-forget writes from @Singleton holders"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/di/ApplicationScope.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt
    - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
    - app/src/test/java/com/app/pegsolitaire/data/SettingsRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "soundOn/hapticsOn default true (D-17 fail-open) and are read with the exact hintsEnabled catch/map pattern — no new abstraction"
  - "ChallengeProgress.currentLevel exposed via stateIn(Eagerly) so the pre-existing synchronous .value contract survives the async DataStore backing (Pitfall 3)"
  - "advanceToNext()/reset() stay non-suspend and fire-and-forget on an injected @ApplicationScope CoroutineScope — preserves the public surface for the 05-06 wiring"
  - "advanceToNext() increments inside the dataStore.edit transaction (not from the cached StateFlow value) so two rapid solves cannot collapse into one level"
  - "Added an @ApplicationScope qualifier rather than binding a bare CoroutineScope, which would be captured by any future unqualified scope injection"

patterns-established:
  - "Per-feature scoped Preferences constants objects (SettingsPreferences, ChallengeProgressPreferences) — QUAL-03, no inline key strings"
  - "Persistence unit tests use a TemporaryFolder-backed PreferenceDataStoreFactory plus a deliberately-throwing DataStore for the fail-open case"
  - "Process-death is simulated in tests by constructing a second holder over the same DataStore instance"

requirements-completed: [SET-01, SET-02, PERS-02, CHAL-05]

# Metrics
duration: 18min
completed: 2026-08-25
---

# Phase 05 Plan 02: Scalar Preferences Persistence Summary

**soundOn/hapticsOn persisted in the shared `app_prefs` DataStore with fail-open ON defaults, and `ChallengeProgress.currentLevel` re-backed by that same store via `stateIn` so the Challenge level survives process death behind an unchanged public surface.**

## Performance

- **Duration:** ~18 min
- **Tasks:** 2 (both TDD: RED → GREEN)
- **Files modified:** 8 (1 created, 7 modified)

## Accomplishments

- `SettingsRepository` now exposes `soundOn` (SET-01) and `hapticsOn` (SET-02) as fail-open `Flow<Boolean>`s plus `setSoundOn`/`setHapticsOn`, keyed by scoped `sound_on` / `haptics_on` constants — unblocking the Settings UI slice (05-04) and the haptic-on-jump wiring.
- `ChallengeProgress` is DataStore-backed (PERS-02 / CHAL-05 data half): `currentLevel` is a `stateIn`-shared `StateFlow<Int>` over `challenge_current_level`, so a fresh holder over the same store resumes at the persisted level while `.value` stays synchronously readable.
- Added an `@ApplicationScope` qualifier and app-lifetime `CoroutineScope` provider (`SupervisorJob() + Dispatchers.Default`) — the reusable seam for fire-and-forget persistence writes from `@Singleton` holders.
- Every new read uses the established `.catch { emit(emptyPreferences()) }` fail-open path, mitigating threat **T-05-02** (a corrupt store degrades to helpers-ON / level 1, never a crash), and each degradation is asserted by a throwing-store test.
- The single shared `app_prefs` delegate is untouched — no second `preferencesDataStore(...)` was introduced.

## Task Commits

Each task was committed atomically (TDD: test → feat):

1. **Task 1: Add persisted soundOn / hapticsOn to SettingsRepository**
   - `14dfcb5` (test) — RED: default-ON, round-trip, fail-open and flag-independence cases
   - `0f9f2c7` (feat) — GREEN: keys, defaults, reads, setters + test-fake updates
2. **Task 2: Back ChallengeProgress with the shared DataStore**
   - `2b198c4` (test) — RED: default 1, persist-across-restart, reset, fail-open
   - `070c226` (feat) — GREEN: `stateIn`-backed `currentLevel`, transactional writes, `@ApplicationScope`

No REFACTOR commit was needed — both implementations landed at their final shape (a one-for-one copy of an existing in-repo pattern).

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/di/ApplicationScope.kt` — **created**; `@Qualifier` for the app-lifetime `CoroutineScope`.
- `app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt` — added `soundOn`/`hapticsOn` + setters to the interface and impl; added `SOUND_KEY`/`DEFAULT_SOUND`, `HAPTICS_KEY`/`DEFAULT_HAPTICS` to `SettingsPreferences`.
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — swapped the `MutableStateFlow` backing for the shared DataStore; added the `ChallengeProgressPreferences` scoped constants object.
- `app/src/main/java/com/app/pegsolitaire/di/DataModule.kt` — added the `@ApplicationScope` `CoroutineScope` `@Provides` next to `provideAppDataStore`.
- `app/src/test/java/com/app/pegsolitaire/data/SettingsRepositoryTest.kt` — +7 cases covering both new flags.
- `app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt` — rewritten against a temp-file DataStore; 5 cases incl. persist-across-"restart" and fail-open.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt` — `FakeSettingsRepository` extended with sound/haptics state.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt` — `HintsOnSettingsRepository` stub extended with the new members.

## Decisions Made

See `key-decisions` in the frontmatter. The load-bearing one is Pitfall 3's resolution: `currentLevel` keeps `StateFlow` (not a cold `Flow`) via `stateIn(scope, SharingStarted.Eagerly, DEFAULT_CURRENT_LEVEL)`, so the persisted value lands as soon as the first disk read completes while `.value` never becomes unavailable. That is what let `advanceToNext()`/`reset()` stay non-`suspend` and the public surface stay byte-for-byte compatible with the Phase-3 seam.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created the worktree's missing `local.properties`**
- **Found during:** Task 1 (first verification run)
- **Issue:** `./gradlew :app:testDebugUnitTest` failed with "SDK location not found" — `local.properties` is gitignored and therefore absent in a fresh git worktree.
- **Fix:** Wrote `local.properties` with `sdk.dir=/Users/ermanergoz/Library/Android/sdk`.
- **Files modified:** `local.properties` (gitignored — **not committed**, by design).
- **Verification:** Gradle resolves the SDK; all subsequent builds/tests run.
- **Committed in:** n/a (intentionally untracked).

**2. [Rule 3 - Blocking] Extended the two in-repo `SettingsRepository` test doubles**
- **Found during:** Task 1 (GREEN)
- **Issue:** Adding members to the `SettingsRepository` interface broke compilation of `FakeSettingsRepository` (GameViewModelTest) and `HintsOnSettingsRepository` (GameFlowTest), which implement it.
- **Fix:** Added `soundOn`/`hapticsOn` (both defaulting ON) and the two setters to both doubles; the fake got real `MutableStateFlow` backing so a future write re-emits.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt`
- **Verification:** `:app:testDebugUnitTest` green (full suite, not just the two target classes).
- **Committed in:** `0f9f2c7` (Task 1 GREEN commit)

**3. [Rule 2 - Missing Critical] `advanceToNext()` increments inside the write transaction**
- **Found during:** Task 2 (GREEN)
- **Issue:** The plan's sketch implies writing `currentLevel.value + 1`. Reading the cached `StateFlow` value outside the `edit` block is a lost-update race — two solves landing before the store round-trips would both write the same N+1.
- **Fix:** `dataStore.edit { it[KEY] = (it[KEY] ?: DEFAULT) + 1 }`, i.e. read-modify-write inside DataStore's own transaction.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt`
- **Verification:** `advanceToNextTwiceReachesLevelThree` asserts sequential advances reach 3.
- **Committed in:** `070c226` (Task 2 GREEN commit)

**4. [Rule 3 - Blocking] Added an `@ApplicationScope` qualifier file (not in `files_modified`)**
- **Found during:** Task 2 (GREEN)
- **Issue:** The plan called for constructor-injecting a `CoroutineScope`, but Hilt cannot safely bind a bare `CoroutineScope` — any future unqualified scope injection would silently receive the app-lifetime one.
- **Fix:** New `di/ApplicationScope.kt` `@Qualifier`, applied to both the provider and the `ChallengeProgress` constructor parameter.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/di/ApplicationScope.kt` (created)
- **Verification:** `:app:assembleDebug` green — the Hilt graph resolves.
- **Committed in:** `070c226` (Task 2 GREEN commit)

---

**Total deviations:** 4 auto-fixed (3 blocking, 1 missing-critical)
**Impact on plan:** All four were required for the plan's own acceptance criteria to be verifiable (build, compile, correctness). No scope creep — no UI, Room or wiring work was pulled in.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*SettingsRepositoryTest"` | PASS |
| `./gradlew :app:testDebugUnitTest --tests "*ChallengeProgressTest"` | PASS (5 tests, 0 failures) |
| `./gradlew :app:testDebugUnitTest` (full suite) | PASS |
| `./gradlew :domain:test` | PASS |
| `./gradlew :app:ktlintCheck :app:detekt` | PASS |
| `./gradlew :app:assembleDebug` (Hilt graph) | PASS |
| Exactly one `preferencesDataStore(` delegate (AppPreferences only) | PASS |

## Issues Encountered

- Fresh git worktrees lack the gitignored `local.properties`, so the first Gradle invocation fails with "SDK location not found". Resolved by writing it locally (deviation 1). Future worktree-isolated executors in this repo should expect the same first-run failure.

## Known Stubs

None introduced by this plan. Note the **intentional, plan-scoped** gaps that remain:
- `soundOn` is a persisted flag only — there is no audio engine or assets yet (D-16, explicitly out of scope).
- `hapticsOn` is persisted but nothing fires a haptic yet; the `LocalHapticFeedback` jump path is a later slice (SET-02 UI half).
- `ChallengeProgress.advanceToNext()` still has zero production callers and `AppNavHost` still hardcodes its entry level. Persisting was this plan's scope; **wiring advance-on-solve and the Home tile is 05-06** (Research Pitfall 7 — "invisible feature" trap). Until 05-06 lands, PERS-02 is data-complete but not yet user-visible.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **05-04 (Settings UI)** can collect `soundOn`/`hapticsOn` from `SettingsRepository` exactly as `hintsEnabled` is collected today.
- **05-06 (Challenge continue)** can read `ChallengeProgress.currentLevel.value` for the Home tile and call `advanceToNext()` on a Challenge solve — no further data-layer work needed.
- Any other `@Singleton` needing a long-lived writer scope can now inject `@ApplicationScope CoroutineScope`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
