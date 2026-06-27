# Peg Solitaire (Android) — Design Spec

- **Date:** 2026-06-28
- **Status:** Approved (pending final user review)
- **Scope of this spec:** Overall product design + detailed **Slice 1 (Playable Core)**. Slice 2 (Play Games + AdMob) is outlined for context and will get its own spec.

---

## 1. Vision

A polished native Android peg solitaire game with two modes:

- **Classic** — the well-known, hand-defined boards, always available.
- **Challenge** — an effectively infinite stream of sequential, procedurally generated levels that are **unique** and **guaranteed solvable**, scaling in difficulty as the player climbs.

Players earn **1–3 stars** per level. Later: Google Play Games (sign-in, leaderboards, achievements, cloud save) and AdMob monetization (rewarded + interstitial).

## 2. Goals & Non-Goals

### Goals
- Faithful peg solitaire rules with satisfying jump/removal feel.
- Infinite, never-repeating, always-solvable Challenge levels.
- Deterministic levels: level _N_ is always the same board (retry, resume, fair comparison).
- Star scoring with a separate numeric score used later for ranking.
- Clean Architecture (UI/MVVM · Domain · Data) with the Repository pattern, fully unit-testable game logic.
- Platform concerns (ads, Play Games) isolated behind interfaces so the core never depends on an SDK.

### Non-Goals (now — YAGNI)
- Online/real-time multiplayer.
- Daily challenges, custom board editor, level sharing.
- Multiple visual themes (ship one clean theme first).
- Compose Multiplatform / iOS (Android-only for now).

## 3. Game Rules

- A **board** is a set of holes laid out on a grid. Each hole is `Blocked` (not part of the board), `Empty`, or `Peg`.
- A **move**: a peg jumps **orthogonally** (up/down/left/right — no diagonals) over an **adjacent peg** into an **empty hole two cells away**, in a straight line. The jumped-over peg is removed.
- The game **ends** when no legal move remains.
- **Objective:** minimize pegs remaining; the ideal outcome is exactly **one** peg.

## 4. Modes

### 4.1 Classic
A fixed, hand-defined catalog stored as static data. Initial set:
- English cross (33 holes, classic single-center empty)
- European / Continental (37 holes)
- Triangle-15
- Diamond-41

Each classic board declares its layout, starting configuration, and (optionally) a designated target hole. Solvability of classic boards is known; a `Solver` validates them in tests.

### 4.2 Challenge
- Levels are **sequential** integers starting at 1.
- Level _N_ maps to a deterministic seed (e.g. `seed = hash(N)`), so the generated board is reproducible and identical across devices/sessions.
- Each level is generated to be **unique** and **guaranteed solvable** (see §5).

## 5. Level Generation (the core mechanism)

Generate **backwards** to guarantee solvability:

1. Start from a **solved state** for the target board footprint (a single peg in a chosen hole).
2. Repeatedly apply **reverse moves**: a reverse move takes a peg and "un-jumps" it — placing the jumping peg back two cells away and re-adding the jumped peg, with the appropriate emptiness constraints. Each reverse move adds one peg.
3. Drive the reverse walk with a **seeded PRNG** (seed derived from the level number). The resulting board is a valid starting position whose **forward solution is the reverse of the generation sequence** — so a winning line is known by construction.

### Difficulty scaling (knobs chosen during design)
Difficulty increases with level number via:
- **Board size & shape** — start small (e.g. cross/diamond footprints); grow footprint and introduce varied outlines (crosses, diamonds, rectangles, triangles, irregular) at higher levels.
- **Peg density / solution length** — more reverse moves → more pegs and longer required solution chains.
- **Starting pattern** — vary which holes begin empty (not just a single center gap).

Jumps remain **orthogonal-only** across all Challenge levels (classic rule set).

### Generator output (per level)
- Board footprint + starting configuration.
- The recorded winning solution (ordered move list) — used for **hints** and for validation.

### Validation
- Generator output is checked in tests: the recorded solution must legally reduce the board to one peg, and the board must be uniquely reproducible from its seed.

## 6. Hints & Undo

- **Hint:** reveal the next move from the recorded winning solution **relative to the current board state**. If the player has deviated from the canonical solution, fall back to the `Solver` to find a winning move from the current state (Challenge levels remain solvable from any state only along valid lines; if the player has reached an unsolvable state, the hint surfaces "no winning move — undo or restart").
- **Undo:** revert the last move (move history stack).
- In Slice 1 these are **free**. In Slice 2 they are gated behind rewarded ads (the `AdGateway` decides; the gameplay code just requests "grant a hint").

## 7. Scoring & Stars

- **Stars** are driven by **pegs remaining** at game end:
  - 1 peg → ★★★
  - 2 pegs → ★★
  - 3 pegs → ★
  - 4+ pegs → no stars (offer retry)
- **Time** feeds a separate numeric **score** shown on the results screen and reserved for leaderboards (Slice 2). Proposed form: `score = base(pegsRemaining) * 1000 - timePenalty`, floored at a minimum. Time therefore rewards speed via ranking **without** withholding stars from a slow-but-perfect solve.
- _Open decision flagged for review:_ alternative is to gate the 3rd star behind a per-level "par" time. Current spec uses the ranking-score model.

## 8. Architecture

Native Kotlin, **Clean Architecture** with three layers per the official Android guidance
(<https://developer.android.com/topic/architecture>), DI via **Hilt**, async via **Coroutines/Flow**.

### 8.1 UI layer — **MVVM**
- **Composables** (Jetpack Compose) render state and emit user events. No business logic in composables.
- **ViewModels** expose a single immutable `UiState` via `StateFlow`; **unidirectional data flow** (state down, events up). One-off effects (navigation, dialogs) via a `SharedFlow`/effect channel.
- Board is drawn on a Compose `Canvas` with jump/removal animations.
- Compose implementation follows the locally installed **`compose-skill`** (`~/.claude/skills/compose-skill`) — its UDF and state-management practices apply; we retain MVVM naming per project preference.
- Screens: **Home**, **Classic picker**, **Challenge** (continue/level select), **Gameplay**, **Results**.

### 8.2 Domain layer (optional, pure Kotlin — no Android deps)
- **Models:** `Board`, `Cell`, `Position`, `Move`, `GameState`, `LevelResult`.
- **Engine:** `RulesEngine` (enumerate legal moves, apply move, detect game-over).
- **Generation:** `LevelGenerator` (seeded reverse-generation), `Solver` (search for validation/fallback hints).
- **Scoring:** `ScoreCalculator` (stars + numeric score).
- **Use cases:** e.g. `GenerateChallengeLevelUseCase`, `LoadClassicLevelUseCase`, `ApplyMoveUseCase`, `GetHintUseCase`, `CalculateResultUseCase`, `SaveProgressUseCase`.

### 8.3 Data layer — **Repository pattern** (single source of truth)
- **Repositories:**
  - `LevelRepository` — supplies classic boards (static data source) and generated challenge boards (generator data source).
  - `ProgressRepository` — per-level results (stars, best time/score), highest level reached, in-progress board snapshot for resume.
  - `SettingsRepository` — sound, haptics, preferences.
- **Local data sources:** **Room** for per-level results/progress; **DataStore** for settings/lightweight state. (Final choice between Room vs DataStore-for-progress decided in the implementation plan.)
- Cloud data source (Play Games Saved Games) added in Slice 2 behind the same repositories.

### 8.4 Platform adapters (interfaces now, implementations in Slice 2)
- `AdGateway` — `showInterstitial()`, `requestRewarded(reason): Boolean`. Slice 1 = no-op/auto-grant stub.
- `GamesService` — sign-in, submit score, unlock achievement, cloud save. Slice 1 = no-op stub.

### 8.5 Suggested package structure
```
com.app.pegsolitaire
├─ domain/        models, rulesengine, generator, solver, scoring, usecase
├─ data/          repository, local (room/datastore), source
├─ ui/            home, classic, challenge, game, results, theme, components
├─ platform/      ads (AdGateway), games (GamesService)
└─ di/            Hilt modules
```

## 9. Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Hilt (DI), Coroutines + Flow/StateFlow
- Room and/or DataStore (persistence)
- Compose `Canvas` for board rendering + animations
- JUnit + Turbine (Flow tests); domain logic targeted for high unit-test coverage
- Slice 2: Google Play Games Services v2, Google Mobile Ads (AdMob)

## 10. Persistence Model (Slice 1)

- **LevelResult**: `{ mode, levelId, stars, bestTimeMs, bestScore, completedAt }`
- **ChallengeProgress**: `{ highestLevelReached, currentLevel }`
- **InProgressGame** (resume): `{ mode, levelId, serializedBoard, moveHistory, elapsedMs }`
- **Settings**: `{ soundOn, hapticsOn, ... }`

## 11. Build Slices

### Slice 1 — Playable Core (this spec)
Board model + `RulesEngine`; Compose board with jump/removal animation; Classic catalog (4 boards); Challenge `LevelGenerator` + seeding + difficulty scaling; `Solver` (validation/fallback); free hint + undo; star + score results screen; local save, resume, per-level results; Home / Classic / Challenge / Gameplay / Results screens; `AdGateway` & `GamesService` as no-op stubs. **Outcome: a fully playable, testable game.**

### Slice 2 — Online & Monetization (future spec)
Google Play Games: sign-in, leaderboards (highest level + best scores), achievements, cloud save. AdMob: rewarded ads for hint/undo/retry, interstitial between levels. Visual theming/polish.

## 12. Risks & Open Questions

- **Generator difficulty curve** needs tuning so early levels are easy and growth feels fair — will need playtesting/iteration; difficulty parameters are data-driven to allow tweaking.
- **Reverse-generation edge cases** (degenerate boards, trivial solutions) — guarded by validation in tests.
- **Star model** — ranking-score vs par-time gate (flagged in §7) — confirm during review.
- **Room vs DataStore** for progress — finalized in the implementation plan.

## 13. References

- Android app architecture guide — <https://developer.android.com/topic/architecture>
- `compose-skill` (local) — `~/.claude/skills/compose-skill`
- Google Play Games Services v2 / AdMob (Slice 2)
