# Project Research Summary

**Project:** Peg Solitaire (Android)
**Domain:** Native Android single-player puzzle game — procedurally generated + classic boards
**Researched:** 2026-06-28
**Confidence:** HIGH

## Executive Summary

Peg Solitaire is a single-player board puzzle where pegs jump orthogonally over adjacent pegs to remove them, ending ideally with one peg remaining. The distinguishing engineering challenge is a **procedural Challenge mode** that must produce infinite, unique, guaranteed-solvable levels with a well-tuned difficulty curve. The correct generation technique — and the one the spec mandates — is **reverse-generation**: start from the solved state (one peg at the target hole) and iteratively apply legal inverse jumps driven by a level-number-seeded PRNG. This produces a solvable starting board and records the winning solution as a by-product, which powers reliable free hints. The domain core (RulesEngine, LevelGenerator, Solver, ScoreCalculator) must be pure Kotlin with zero Android imports, making it the dependency root of the entire architecture and the highest-value test target.

The recommended approach is a six-stage vertical-slice build rooted in the domain layer. Each slice delivers an end-to-end playable outcome, with platform SDKs (AdMob, Play Games) hidden behind no-op interfaces from Slice 1 so Slice 2 only swaps Hilt bindings. Persistence is deliberately last among Slice-1 work because the game is fully playable without it. The UI uses Jetpack Compose (Material 3) with strict UDF/MVVM: screens collect immutable `StateFlow<UiState>` from ViewModels; one-off effects travel through a `Channel<Effect>`; and the `BoardCanvas` is a pure renderer with no game logic inside composables. Room handles queryable, multi-row results and the in-progress resume snapshot; DataStore handles scalar user settings and scalar challenge progress.

The project's top risk is the **generator difficulty curve**: reverse-generation can produce trivially short or spatially degenerate boards if quality gates are absent, and any post-launch change to difficulty knobs or generator logic silently rewrites every Challenge level for every existing player. Mitigation requires stamping every generated board with a `generatorVersion` from day one, keeping difficulty parameters data-driven and finalized before the first public release, and locking a golden-master CI test (board + solution snapshots for levels 1, 2, 10, 100, 1000) before any tuning work begins. The difficulty-curve sub-phase inside VS3 must be treated as a playtest-iteration loop, not a one-shot implementation.

---

## Key Findings

### Recommended Stack

The stack is well-settled and driven by official Android guidance. The Kotlin 2.4.0 / AGP 9.2.0 / Gradle 9.1+ / JDK 17 quartet is non-negotiable: Hilt 2.57.x requires AGP 9, KSP2 is the default annotation processor from Kotlin 2.x (KAPT is gone), and the Compose compiler ships with Kotlin as a Gradle plugin rather than a standalone artifact. All Compose artifacts are versioned via the BOM (2026.06.00) — individual Compose versions must never be hand-pinned. Room 2.8.4 stable is the persistence choice (Room 3.0 is alpha/KMP-focused and out of scope). Slice 2 SDK versions (Play Games v2, AdMob 23.6.0+) are pinned at integration time; use only PGS v2 — v1 APIs are being removed starting May 2026.

**Core technologies:**
- **Kotlin 2.4.0** — language; KSP2 built-in; pure-Kotlin domain layer is the architecture foundation
- **AGP 9.2.0 / Gradle 9.1+ / JDK 17** — build toolchain required by Hilt 2.57.x
- **Jetpack Compose BOM 2026.06.00** — UI toolkit; single version source for all Compose artifacts; never hand-pin member versions
- **Material 3 (via BOM)** — design system; M3 Expressive components available; M2 is deprecated
- **Hilt 2.57.1 + KSP** — DI; wires interface to impl at the app boundary; no KAPT anywhere
- **Room 2.8.4** — structured local DB for per-level results and the in-progress resume snapshot
- **DataStore Preferences 1.2.1** — settings and scalar challenge progress
- **kotlinx-coroutines 1.11.0 + kotlinx-serialization-json 1.11.0** — async substrate and board-blob serialization
- **Navigation Compose 2.9.8** — type-safe routing across 5 screens; not Navigation 3
- **Compose Canvas + Animatable** — board rendering and jump/removal animations; no external game engine
- **Roborazzi 1.x** — primary screenshot test tool; Hilt-compatible; supports post-interaction snapshots
- **Turbine 1.2.1 + MockK 1.14.x + Truth 1.4.x** — Flow assertions, Kotlin-native mocking, readable assertions

**Room vs DataStore split (resolved):**

| Data | Store | Reason |
|------|-------|--------|
| `LevelResult` (per-level stars, bestTimeMs, bestScore) | Room | Many rows, keyed queries, upsert-on-better-score |
| `InProgressGame` (board snapshot, moveHistory, elapsedMs) | Room | Single transactional store with results; board serialized as JSON blob via TypeConverter |
| `ChallengeProgress` (highestLevelReached, currentLevel) | DataStore | Two scalar ints, read on app start, no queries |
| `Settings` (soundOn, hapticsOn) | DataStore | Canonical scalar prefs use case; replaces SharedPreferences |

---

### Expected Features

**Must have — table stakes (Slice 1):**
- Orthogonal jump mechanic + win/lose detection — this is the game
- Classic catalog: English-33, European-37, Triangle-15, Diamond-41
- Undo last move + restart current board
- Hint (next winning move — canonical line first, Solver fallback on deviation)
- Star rating per level (1-3 from pegs remaining); ranking score (time-based, separate from stars)
- Visible pegs-remaining count + elapsed timer in the HUD
- Jump and removal animation (satisfying juice is a stated project goal)
- Progress persistence: per-level best results, highest Challenge level, in-progress resume
- Level select (Classic picker + Challenge continue screen)
- Settings: sound + haptics toggles
- Lightweight first-level tutorial (skippable interactive overlay)
- `AdGateway` + `GamesService` as no-op stubs so the core stays SDK-free

**Should have — differentiators (Slice 1):**
- Infinite, sequential, deterministic, guaranteed-solvable Challenge levels — the headline feature; no competitor offers this
- Recorded winning line from reverse-generation — free, reliable hints by construction
- Difficulty scaling via board size/shape, peg density, starting pattern — data-driven knobs so tuning never regenerates the seed-to-board contract
- Deterministic level seeding via `stableHash(N)` — enables retry/resume/leaderboard fairness

**Add after validation (Slice 2):**
- Google Play Games sign-in (optional/non-blocking), leaderboards, achievements, cloud save
- AdMob rewarded ads (hint/undo/retry) + frequency-capped interstitials between levels
- Visual theming / polish pass

**Defer (v2+):**
- Additional exotic classic boards (Hexagon, Star, Wiegleb)
- Daily challenge (low marginal cost atop the deterministic ladder)
- Redo / color-blind accessibility extras

**Explicit anti-features — never build:**
- Diagonal jumps — breaks the rule set and doubles generator/solver complexity
- Lives/energy/timers-to-play — antithetical to casual brain-training audience
- Forced account sign-in at launch — Play Games must be optional and non-blocking
- Par-time gating of the 3rd star — spec deliberately rejected this; time feeds the ranking score instead

---

### Architecture Approach

The architecture follows strict Clean Architecture with unidirectional dependencies: UI to Domain to Data. The pure-Kotlin domain layer (`domain/`) is the dependency root — it declares repository interfaces and platform interfaces (`AdGateway`, `GamesService`) that the data and platform layers implement; domain never imports Room, DataStore, AdMob, or Play Games. ViewModels own `StateFlow<UiState>` (immutable, read-only to composables) and emit one-off effects via `Channel<Effect>`; composables render state and emit events only. The `BoardCanvas` is a pure function of `UiState` — it drives `Animatable`s for jump/removal animations from a declarative `MoveTransition` descriptor; no game logic crosses into the Canvas. One `LevelRepository` hides the classic/challenge branch behind a `LevelRef` sealed type so UI code never knows which source is active.

**Major components:**
1. **domain/model** — `Board`, `Cell`, `Position`, `Move`, `GameState`, `LevelResult`; all immutable `data class`; zero Android imports; the dependency root
2. **domain/rules — RulesEngine** — pure stateless functions: `legalMoves`, `applyMove`, `isGameOver`, `pegCount`
3. **domain/generator — LevelGenerator** — seeded reverse-generation; produces starting board + recorded solution; deterministic via `kotlin.random.Random(seed)` over a sorted candidate list
4. **domain/solver — Solver** — DFS with transposition table and pagoda-function pruning; validates classic/generated boards in tests; runtime fallback for off-script hints; runs off-thread with node/time budget
5. **domain/scoring — ScoreCalculator** — `stars(pegsRemaining)` and `score(pegsRemaining, timeMs)` pure functions
6. **domain/repository interfaces** — `LevelRepository`, `ProgressRepository`, `SettingsRepository` (dependency inversion; impls in data/)
7. **domain/platform interfaces** — `AdGateway`, `GamesService` (Slice 1 = no-op stubs; Slice 2 = real impls swapped via Hilt)
8. **data/repository impls** — `LevelRepositoryImpl` (routes to `ClassicLevelSource` / `ChallengeLevelSource`), `ProgressRepositoryImpl` (Room), `SettingsRepositoryImpl` (DataStore)
9. **ui/ per screen** — Home, ClassicPicker, Challenge, Gameplay, Results; each as Route + Screen + ViewModel + Contract; `BoardCanvas` + `PegRenderer` in `ui/components/`
10. **di/ — Hilt modules** — DomainModule, DataModule, PlatformModule; swap platform impls in Slice 2 without touching gameplay code

---

### Critical Pitfalls

1. **Non-deterministic generation despite a seeded PRNG** — candidate moves enumerated from a `HashSet`/unordered collection produce different boards per run. Fix: always sort candidates into a `List` (by row, col, direction) before every PRNG draw; use `kotlin.random.Random(seed)` with a portable hash; generate single-threaded. Lock a **golden-master CI test** (board + solution for levels 1, 2, 10, 100, 1000) before any difficulty tuning. This is the single most valuable test in the project.

2. **Generator changed post-launch silently rewrites every Challenge level** — any change to difficulty knobs, reverse-walk logic, or seed derivation is a breaking change for the entire infinite level set and corrupts existing stars, resumes, and leaderboard entries. Fix: stamp every generated/saved board with `generatorVersion` from day one; finalize difficulty parameters (data-driven) before the version is frozen; never mutate v1 in place post-launch — ship a parallel v2 algorithm with a migration policy.

3. **Reverse-generation produces trivial, degenerate, or duplicate boards** — pure random reverse-walks produce compact, low-branching boards with short solutions. Fix: add quality gates in the generator (minimum solution length, spatial spread metric, branching-factor floor, forced-line rejection) with deterministic re-seeding on rejection; symmetry-normalized dedup for small footprints. Requires playtest iteration.

4. **Hint gives an illegal or losing move after the player deviates from the canonical line** — indexing `solution[moveCount]` blindly returns an illegal or dead-end move once the player goes off-script. Fix: hint logic is state-relative: (1) if the next canonical move is legal in the current board, return it; (2) else run `Solver.findWinningMove` within a budget; (3) if no win is reachable, surface a first-class "no winning move — undo or restart" state, never an exception or silent no-op.

5. **Solver blocks the main thread on large footprints** — peg solitaire is NP-complete; DFS without pruning ANRs the UI on hint tap for high-level large boards. Fix: run any Solver call on `Dispatchers.Default`; apply pagoda-function pruning to cut unsolvable branches early; enforce a node/time budget with graceful degradation; cap generated footprint size so solution length carries late-game difficulty.

6. **`StateFlow` does not recompose because `Board` is mutated in place** — mutating cells in a backing array and re-assigning the same reference emits no change from `StateFlow` (deduplicated by `equals()`). Fix: all domain models are immutable `data class`; moves produce a new `GameState` via `copy()`, never mutation.

7. **Elapsed-time clock causes whole-board recomposition every tick** — `elapsedMs` bundled into the same `UiState` as the board triggers `Canvas` recomposition on every timer tick, burning CPU and causing jank at 90/120 Hz. Fix: expose elapsed time as a separate `StateFlow` or pass it as a lambda into a leaf composable; board and timer must never share the same state holder.

8. **`fallbackToDestructiveMigration()` wipes player progress on schema change** — a schema update without a proper Room `Migration` crashes or silently deletes all stars. Fix: `exportSchema = true`, schema JSON committed to VCS, explicit `Migration` objects, Room migration test harness; remove `fallbackToDestructiveMigration` from release builds entirely.

---

## Implications for Roadmap

Research maps directly to the six vertical slices defined in ARCHITECTURE.md. Each slice is end-to-end playable; later slices slot behind seams established in earlier ones without touching game code.

### Phase 1 (VS1): Domain Foundation — Model, RulesEngine, Board Canvas, Single Hardcoded Board

**Rationale:** The domain model and `RulesEngine` are the dependency root for every other component. Building them first in pure Kotlin lets the highest-value unit tests run on the JVM from day one. The gameplay loop must be proven end-to-end before any persistence, generation, or SDK work begins.

**Delivers:** A single hardcoded board playable end-to-end — tap peg, tap target hole, peg jumps with animation, game-over detected. No navigation, no persistence, no generation yet.

**Addresses features:** Correct orthogonal jump mechanic, win/lose detection, jump/removal animation, immutable `Board`/`GameState` as `StateFlow` source of truth.

**Implements architecture:** `domain/model/`, `domain/rules/RulesEngine`, `GameUiState`, `GameViewModel`, `BoardCanvas` (Compose `Canvas` + `Animatable`), `GameEffect` channel. Hilt wired. One in-code board definition.

**Avoids pitfalls:** Pitfall 6 (mutable `Board` no recompose), Pitfall 9 (Android imports in domain), Pitfall 10 (logic in composables / leaked `MutableStateFlow`). Enforce domain purity boundary with Konsist/ArchUnit from Phase 1 — cheapest to establish first.

**Code-standard gates:** No magic numbers in `RulesEngine` or `Board` geometry — extract to a scoped constants object. `detekt` + `ktlint` configured and green before anything else commits. State hoisting enforced from the first composable (stateless `BoardCanvas`; state owned by `GameViewModel`).

**Research flag:** Standard patterns — no additional research needed; `RulesEngine` is pure game logic fully specified in the design doc (section 4).

---

### Phase 2 (VS2): Classic Mode — Catalog, Scoring, Navigation, Results Screen

**Rationale:** Classic mode validates the complete screen-to-screen flow (Home → pick board → Gameplay → Results) and establishes the `LevelRepository` seam before tackling generation. The seam must exist here so the generator (Phase 3) slots in behind it without touching the UI.

**Delivers:** Home → Classic picker → play one of 4 boards → Results screen with stars and ranking score. `ScoreCalculator` tested. Navigation complete for all 5 screens (Challenge screen stubbed).

**Addresses features:** Classic catalog (English-33, European-37, Triangle-15, Diamond-41), star rating (pegs remaining), ranking score (time-based), level select, `Solver` validating classic board solvability in tests.

**Implements architecture:** `ClassicLevelSource` (static board definitions), `LevelRepository`/`LevelRepositoryImpl` with `LevelRef.Classic` routing, `ScoreCalculator`, `CalculateResultUseCase`, `ClassicPickerViewModel`, `ResultsViewModel`, Navigation Compose routes for all 5 screens. `Solver` introduced here for offline classic-board validation.

**Avoids pitfalls:** Pitfall 2 — `LevelRepository` seam prevents UI from ever branching on classic/challenge; established here, paid off in Phase 3.

**Code-standard gates:** All screen dimensions in `res/values/dimens.xml`; per-screen constants objects for any numeric/string constants; no hardcoded dp/sp in composables.

**Research flag:** Standard patterns — static catalog + DFS Solver for known boards is well-understood.

---

### Phase 3 (VS3): Challenge Generation — LevelGenerator, DifficultyTier, Determinism, Difficulty Curve

**Rationale:** This is the project's headline differentiator and its number-one risk. The generator must be proven deterministic (golden-master test in CI), produce non-trivial boards (quality gates), and deliver a playable difficulty ramp before any persistence or monetization work begins. The difficulty-curve sub-phase is a playtest-iteration loop — budget accordingly. No generator code may ship without the golden-master CI fixture locked.

**Delivers:** Infinite, sequential Challenge levels: deterministic (level N always produces the same board), guaranteed solvable by construction, difficulty scaling across early/mid/late tiers. `ChallengeLevelSource` slots behind the existing `LevelRepository` seam. The Challenge screen is fully playable.

**Addresses features:** Infinite Challenge mode, guaranteed-solvable generation, deterministic seeding, difficulty scaling (data-driven knobs: footprint size/shape, peg density, starting empty-pattern, solution length), `generatorVersion` field on every generated/saved board.

**Implements architecture:** `domain/generator/` (LevelGenerator, ReverseMove, DifficultyTier, Prng), `data/source/challenge/ChallengeLevelSource`, `GenerateChallengeLevelUseCase`, Challenge screen + `ChallengeViewModel`. `Solver` reused for generated-board validation in tests.

**Avoids pitfalls:**
- Pitfall 1 (non-determinism) — sorted candidate lists + portable seed derivation + golden-master CI test locked before any tuning
- Pitfall 2 (generator changes post-launch) — `generatorVersion` stamped on every board; difficulty knobs data-driven and frozen before release
- Pitfall 3 (trivial/degenerate boards) — quality-gate rejection loop: solution-length floor, spatial spread, branching-factor floor, forced-line rejection; symmetry-normalized dedup for small footprints

**Code-standard gates:** `generatorVersion` as a constant in a scoped `GeneratorConstants` object (never a magic literal); difficulty-tier parameters in a data config file, not scattered literals.

**Research flag:** NEEDS PLAYTEST ITERATION — difficulty knob values (minimum solution length per tier, spatial spread threshold, footprint sizes by level range) cannot be determined by static analysis. Budget a dedicated playtest sub-phase with manual walkthroughs across the full level range before freezing the `generatorVersion` contract. This is the phase most likely to require iteration cycles.

---

### Phase 4 (VS4): Assist Features — Hint (Canonical + Solver Fallback) and Undo

**Rationale:** Hint and undo are table-stakes puzzle affordances; their absence reads as unfinished. Hint depends on the recorded solution from Phase 3 and the `Solver` from Phase 2. The "no winning move" first-class state and the `Solver` off-thread budget must be designed here, not bolted on later.

**Delivers:** Working hint (canonical next move on-script; `Solver` fallback off-script; "no winning move — undo/restart" when stuck) and undo (`moveHistory` stack). `AdGateway` no-op stub wired — hint/undo are free in Slice 1 via the auto-grant stub.

**Addresses features:** Hint (valid next winning move), undo last move, "no winning move" UX state, `AdGateway` interface established for Slice 2 gating.

**Implements architecture:** `GetHintUseCase` (state-relative: canonical-line legality check first, `Solver` fallback second, explicit no-win state third), `UndoUseCase`, `AdGateway` interface + `NoOpAdGateway` stub wired in `di/PlatformModule`, move-history stack in `GameState`.

**Avoids pitfalls:**
- Pitfall 4 (hint illegal/losing after deviation) — state-relative logic, never `solution[moveCount]` indexing
- Pitfall 5 (`Solver` blocks main thread) — `GetHintUseCase` calls `Solver` on `Dispatchers.Default` with node/time budget; degradation returns "no winning move" rather than hanging

**Code-standard gates:** `AdGateway` reason as a domain-scoped sealed class (`HINT`, `UNDO`, `RETRY`); no string literals for ad reasons.

**Research flag:** Standard patterns — logic flows directly from the recorded solution and `Solver` already built in Phases 2 and 3.

---

### Phase 5 (VS5): Persistence and Resume — Room, DataStore, Progress, In-Progress Snapshot

**Rationale:** Persistence is deliberately the last Slice-1 phase because the game is fully playable without it, allowing all earlier slices to be validated before adding storage complexity. Room migration discipline must be established here and maintained for every schema-touching change thereafter.

**Delivers:** Per-level best results persisted (Room), highest Challenge level persisted (DataStore), in-progress game resume with full move history + elapsed time + `generatorVersion` (Room), Settings persisted (DataStore). Player can background, return, and resume with undo history intact.

**Addresses features:** Local persistence, in-progress resume, settings (sound/haptics), `ProgressRepository`, `SettingsRepository`.

**Implements architecture:** `data/local/room/` (AppDatabase, DAOs, `LevelResultEntity`, `InProgressGameEntity`, TypeConverters for board JSON blob), `data/local/datastore/SettingsDataStore`, `ProgressRepositoryImpl`, `SettingsRepositoryImpl`, `SaveProgressUseCase`, `ResumeGameUseCase`, `data/mapper/` (entity to/from domain). `GamesService` no-op stub wired alongside `AdGateway`.

**Avoids pitfalls:**
- Pitfall 11 (inexact resume) — serialize complete `InProgressGame`: board cells + full `moveHistory` + `elapsedMs` + `generatorVersion`; restore board directly, never regenerate from N; elapsed time as accumulated duration not wall-clock delta
- Pitfall 12 (destructive migration) — `exportSchema = true`, schema JSON committed to VCS, explicit `Migration` objects, Room migration test harness, `fallbackToDestructiveMigration` absent from release

**Code-standard gates:** `generatorVersion` persisted from `GeneratorConstants`; all DataStore preference keys in a scoped `PreferenceKeys` object; Room `@Database` version as a named constant.

**Research flag:** Standard patterns for Room + DataStore; migration discipline is process, not novel technology.

---

### Phase 6 (VS6 — Slice 2): Online and Monetization — AdMob + Play Games Services

**Rationale:** Slice 2 only swaps Hilt bindings — `AdMobAdGateway` replaces `NoOpAdGateway`, `PlayGamesService` replaces `NoOpGamesService`. No game or UI code changes. The playable core was validated in Slice 1; Slice 2 adds the revenue and social layer on a proven foundation.

**Delivers:** AdMob rewarded ads gating hint/undo/retry (opt-in dialog, reward only on completion callback); frequency-capped interstitials between levels; optional Play Games sign-in; leaderboards (meaningful because levels are deterministic); achievements; cloud save with explicit conflict merge (max `highestLevelReached` + best-per-level result).

**Implements architecture:** `platform/ads/AdMobAdGateway`, `platform/games/PlayGamesService` + cloud Saved-Games source behind `ProgressRepository`, UMP consent flow, Hilt `PlatformModule` Slice 2 bindings.

**Avoids pitfalls:**
- Pitfall 13 (AdMob policy) — interstitials only between content, frequency-capped, never on launch/exit; rewarded always behind explicit opt-in dialog; test ad unit IDs in all non-release builds
- Pitfall 14 (PGS v2 sign-in / cloud conflict) — PGS v2 auto sign-in + `isAuthenticated()` (no v1 `GoogleSignInClient`); sign-in optional and non-blocking; conflicts resolved with explicit merge via `resolveConflict()` (max level + best-per-level)

**Research flag:** NEEDS RE-VERIFICATION AT INTEGRATION — PGS v2 and GMA artifact versions move fast; re-verify against current release notes at the start of Phase 6. Conflict-merge UX strategy needs stakeholder input before implementation.

---

### Phase Ordering Rationale

- **Domain core before everything else** — `model/` and `RulesEngine` have zero dependencies; everything else depends on them. Building and testing them first removes the project's foundational risk and enables JVM-only unit tests without an emulator.
- **Classic mode before generator** — validates the complete screen flow, establishes the `LevelRepository` seam, and proves `Solver` works on known boards before tackling generation.
- **Generator before assist features** — Hint depends on the recorded solution from the generator and the `Solver` from Classic mode; Undo depends on the move-history data structure in `GameState`.
- **Persist last** — persistence adds complexity without unblocking any Slice-1 gameplay validation. Deferring it keeps earlier slices lean and fast to iterate.
- **Slice 2 after Slice 1 is proven** — the trigger for Slice 2 is confirmation that generated levels are fun and the difficulty curve is acceptable. Monetization and social features require a validated core.
- **Gitflow discipline throughout** — each vertical slice maps to a `feature/*` branch off `develop`; no direct commits to `develop` or `main`; `release/*` for Slice 1 and Slice 2 release prep; `hotfix/*` off `main` only for production defects.

---

### Research Flags

**Needs deeper research / playtest iteration:**
- **Phase 3 (Generator + Difficulty Curve):** Difficulty knob values (minimum solution length per tier, spatial spread threshold, footprint sizes by level range) cannot be determined by static analysis — they require manual playtesting of generated boards across the full level range before the `generatorVersion` is frozen. Budget a dedicated playtest iteration loop. The golden-master CI test must be locked before tuning begins — this ordering is mandatory.
- **Phase 6 (Slice 2 — AdMob + PGS):** SDK artifact versions move fast; re-verify `play-services-games-v2` and GMA exact versions against current Maven/release notes at integration time. Conflict-merge UX strategy (how the player is informed of progress merges across devices) needs stakeholder sign-off before implementation.

**Standard patterns — skip additional research:**
- **Phase 1 (Domain + RulesEngine):** Pure game logic, fully specified in the design doc (section 4). No research gap.
- **Phase 2 (Classic Catalog + Scoring):** Static data + well-documented DFS Solver patterns. No research gap.
- **Phase 4 (Hint + Undo):** Logic flows directly from the Phase 3 generator output and Phase 2 Solver. No research gap.
- **Phase 5 (Persistence):** Room + DataStore patterns are well-documented; migration discipline is process. No research gap.

---

## Code-Standard Constraints for All Phases

These apply as build gates across every phase — not optional style preferences.

| Constraint | Requirement | Enforcement |
|------------|-------------|-------------|
| No magic numbers or strings | Every constant extracted to a **scoped constants object** (per feature/per layer — never one global dump) | `detekt` MagicNumber rule; code review |
| UI dimensions | All dp/sp values in `res/values/dimens.xml`; no hardcoded dimension literals in composables | Code review |
| State hoisting | Composables are stateless; state owned by ViewModels; `collectAsStateWithLifecycle()` not `collectAsState()` | compose-skill; code review |
| Immutable domain models | All `domain/` types are `data class` with `val` fields; moves produce new instances via `copy()` | Unit test: mutated board never passes equality |
| Domain purity boundary | Zero `android.*` imports in `domain/`; validated by Konsist or ArchUnit boundary test in CI | Boundary test green from Phase 1 |
| Read-only ViewModel API | `val uiState: StateFlow<UiState>` public; `MutableStateFlow` private; effects via `Channel<Effect>` | Code review |
| `detekt` + `ktlint` | Both configured and passing as a required CI gate | CI pipeline from Phase 1 |

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All library versions verified against official release notes as of 2026-06-28; Slice 2 SDK exact versions MEDIUM — pin at Slice 2 integration |
| Features | HIGH | Spec-driven (design doc sections 3-11) corroborated by competitor survey; competitor gap confirmed (no infinite generated mode exists) |
| Architecture | HIGH | Aligned with official Android guide; vertical-slice build order derived from concrete dependency analysis; no novel patterns |
| Pitfalls | HIGH for generation/architecture/Compose; MEDIUM for difficulty-curve tuning values | Generation pitfalls grounded in peg-solitaire mathematics literature; Compose pitfalls verified against official performance docs |

**Overall confidence:** HIGH

### Gaps to Address

- **Difficulty-curve knob values:** Exact thresholds for minimum solution length, spatial spread, and footprint sizes per difficulty tier cannot be set from research alone. Treat initial values as hypotheses; plan a playtest-iteration sub-phase in Phase 3 to revise them before freezing the `generatorVersion`.
- **Slice 2 SDK exact versions:** PGS v2 and GMA artifact versions move between research and integration. Re-verify at the start of Phase 6 against current Maven/release notes.
- **Board collision rate for small footprints:** Birthday-paradox duplicates across early-level seeds are a real risk for tiny footprints (Triangle-15 class). The dedup strategy (symmetry-normalized canonical hash + sub-seed perturbation) is correct in principle; measure exact collision rates experimentally during Phase 3.

---

## Sources

### Primary (HIGH confidence)
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` — authoritative spec for mechanics (section 4), generation (section 5), hints (section 6), scoring (section 7), architecture (section 8), slices (section 11), risks (section 12)
- `.planning/PROJECT.md` — constraints, code standards, Gitflow, key decisions
- developer.android.com/topic/architecture — three-layer architecture, SSOT repositories, UDF
- developer.android.com/jetpack/androidx/releases/compose — Compose BOM 2026.06.00
- developer.android.com/build/releases/agp-9-2-0-release-notes — AGP 9.2.0 / Gradle 9.1+ / JDK 17
- kotlinlang.org/docs/releases.html — Kotlin 2.4.0, KSP2 default
- developer.android.com/jetpack/androidx/releases/room — Room 2.8.4 stable; Room 3.0 alpha/KMP focus
- developer.android.com/jetpack/androidx/releases/datastore — DataStore Preferences 1.2.1
- developer.android.com/games/pgs/android/migrate-to-v2 — PGS v2 sign-in, v1 removal timeline
- support.google.com/admob/answer/6128543 and /6201362 and /7313578 — AdMob policy (interstitial, rewarded)
- developer.android.com/games/pgs/savedgames — Saved Games conflict resolution

### Secondary (MEDIUM confidence)
- Google Play competitor survey — Pinkpointer Peg Solitaire, Peg Genius, rc4812 Logic Puzzle, Solo Noble: confirms finite catalog model and gap for infinite generated mode
- pegsolitaire.fun — board taxonomy and auto-solver conventions
- Udonis FTUE guide, Sunday.gg level design for ad-monetized casual games, Apple Onboarding for Games — FTUE and ad-frequency anti-patterns

### Tertiary — mathematics grounding (HIGH for theory)
- George I. Bell, "A Fresh Look at Peg Solitaire," Mathematics Magazine 2007 — pagoda functions, solvability
- Jaap Scherphuis, "Analysis of Peg Solitaire" (jaapsch.net) — board analysis, resource counts
- Wikipedia: Peg solitaire — NP-completeness, pagoda functions
- jagatsastry/peg-solitaire-solver (GitHub) — pagoda + weight-matrix pruning heuristics

---
*Research completed: 2026-06-28*
*Ready for roadmap: yes*
