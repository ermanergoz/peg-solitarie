# Roadmap: Peg Solitaire (Android)

## Overview

This milestone delivers **Slice 1 — the playable core**: a polished, fully testable peg solitaire game with Classic and infinite procedurally-generated Challenge modes, hints, undo, scoring, and local persistence — with no external SDKs (AdMob and Play Games exist only as no-op stubs behind interfaces). The build is a dependency-ordered chain of vertical slices, each delivering an end-to-end *playable* capability rather than a horizontal technical layer. It starts at the pure-Kotlin domain root (model + RulesEngine) with the playable hardcoded board and all code-standards gates established up front, then layers Classic mode + scoring + navigation, then the headline differentiator (deterministic, guaranteed-solvable Challenge generation — the project's #1 risk), then assist features (hint + undo) that depend on the recorded solution and Solver, and finally persistence/resume which the game is fully playable without. Slice 2 (Play Games + AdMob + visual-polish pass) is deferred to a future milestone and slots in by swapping Hilt bindings behind the interfaces established here.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Domain Foundation & Playable Board** - Pure-Kotlin core + one hardcoded board playable end-to-end, with CI standards gates established up front (completed 2026-06-28)
- [x] **Phase 2: Classic Mode, Scoring & Navigation** - Home → pick 1 of 4 classic boards → play → results with stars/score; full screen flow + LevelRepository seam (completed 2026-06-29)
- [ ] **Phase 3: Challenge Generation & Difficulty Curve** - Infinite, deterministic, guaranteed-solvable generated levels with scaling difficulty (project #1 risk)
- [ ] **Phase 4: Assist — Hint & Undo** - State-relative hint (canonical line + Solver fallback + "no winning move" state) and undo
- [ ] **Phase 5: Persistence, Resume & Settings** - Per-level results, continue-from-highest, in-progress resume, sound/haptics — migration-safe

## Phase Details

### Phase 1: Domain Foundation & Playable Board
**Goal**: A single hardcoded peg solitaire board is fully playable end-to-end — tap a peg, tap a legal hole, the peg jumps with animation, the jumped peg is removed, and game-over is detected — built on a pure-Kotlin domain core (immutable model + RulesEngine) guarded by CI standards gates from the very first commit.
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: GAME-01, GAME-02, GAME-03, GAME-04, GAME-05, QUAL-01, QUAL-02, QUAL-03, QUAL-04
**Success Criteria** (what must be TRUE):
  1. Player can tap a peg and see its legal orthogonal destinations highlighted on the board.
  2. Player can complete a legal jump — the peg slides into the empty hole with animation and the jumped peg is removed.
  3. The game detects and signals the end state (game-over) when no legal moves remain.
  4. Player can restart the current board to its initial state at any time.
  5. CI gates are green: the domain layer has zero `android.*` imports (Konsist/ArchUnit boundary test), `detekt` + `ktlint` pass as a build gate, no magic numbers/strings or hardcoded dp/sp literals, and a JVM unit-test harness (including the golden-master determinism scaffold) covers `RulesEngine`.
**Plans**: 5 plans (Walking Skeleton — sequential waves 1→5)
- [x] 01-01-PLAN.md — Build scaffold + CI standards gates (two-module split, detekt/ktlint/ArchUnit/dp-sp gate, CI) [QUAL-01,02,03]
- [x] 01-02-PLAN.md — Pure-Kotlin domain core + golden-master harness (Board bitmask, RulesEngine, BoardCodec) [GAME-01,02; QUAL-04]
- [x] 01-03-PLAN.md — GameViewModel + immutable GameUiState (select/two-step-jump/restart/game-over) [GAME-02,04,05]
- [x] 01-04-PLAN.md — BoardCanvas render + UI resources (Canvas draw, hit-test, D-07/D-08 animation) [GAME-03,04]
- [x] 01-05-PLAN.md — Screen assembly + playable end-to-end + emulator playtest [GAME-01,03,05]
**UI hint**: yes

### Phase 2: Classic Mode, Scoring & Navigation
**Goal**: Players can launch the app to a Home screen, choose one of five classic boards, play it to completion with correct rules, and land on a results screen showing stars and a ranking score — establishing the complete five-screen navigation flow and the `LevelRepository` seam (with the Challenge source stubbed) so the generator slots in behind it next phase.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: CLAS-01, CLAS-02, SCOR-01, SCOR-02, SCOR-03, NAV-01, NAV-02, ONB-01
**Success Criteria** (what must be TRUE):
  1. Player can browse a catalog of classic boards (English-33, European-37, Diamond-41, Wiegleb-45, Asymmetric 3-3-2-2) and select one to play.
  2. Player can play any selected classic board to completion with correct orthogonal jump rules.
  3. At level end the player sees a results screen showing stars (1–3 from pegs remaining), pegs remaining, elapsed time, and a numeric score.
  4. Player can navigate between Home, mode selection, gameplay, and results screens.
  5. A first-time player sees a brief, skippable how-to-play explanation of the jump rule.
**Plans**: 8 plans (waves 1→5)
- [x] 02-01-PLAN.md — ScoreCalculator: pure-domain stars + time-decay score [SCOR-01, SCOR-02]
- [x] 02-02-PLAN.md — 4 new Classic board footprints + golden solvability fixtures [CLAS-02]
- [x] 02-03-PLAN.md — Build deps (navigation/serialization/datastore) + all Phase-2 dimens & strings [NAV-02, ONB-01]
- [x] 02-04-PLAN.md — LevelRepository seam (real Classic + stub Challenge) + per-board BoardGeometry [CLAS-01, CLAS-02]
- [x] 02-05-PLAN.md — Type-safe Routes + Gameplay loads board by levelId + visible timer + one-shot Results hop [CLAS-02, SCOR-02, NAV-02]
- [x] 02-06-PLAN.md — Results destination: score/stars/pegs/time, tiered headline, wrapping Next-board [SCOR-01, SCOR-02, SCOR-03]
- [x] 02-07-PLAN.md — Classic catalog screen + read-only board previews over LevelRepository [CLAS-01]
- [x] 02-08-PLAN.md — Home mode-select + onboarding overlay (DataStore flag) + type-safe NavHost wiring [NAV-01, NAV-02, ONB-01]
**UI hint**: yes

### Phase 3: Challenge Generation & Difficulty Curve
**Goal**: Players can play an infinite, sequential stream of procedurally-generated Challenge levels that are deterministic (level N is always the same board), guaranteed solvable by construction (reverse-generation with a recorded solution), and scale in difficulty with level number — all routed behind the existing `LevelRepository` seam, with determinism locked by a golden-master CI test and every board stamped with a `generatorVersion`.
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: CHAL-01, CHAL-02, CHAL-03, CHAL-04
**Success Criteria** (what must be TRUE):
  1. Player can start at Challenge level 1 and advance sequentially through generated levels, each guaranteed solvable.
  2. Re-playing Challenge level N always produces the identical board — verified by a golden-master CI test snapshotting board + solution for levels 1, 2, 10, 100, 1000.
  3. Difficulty visibly scales with level number via board size/shape, peg density, and starting empty-pattern.
  4. Every generated/served board carries a `generatorVersion` stamp, and all difficulty knobs are data-driven (frozen before release, never magic literals).
**Plans**: TBD
**Risk**: This is the project's #1 risk. The golden-master CI fixture MUST be locked before any difficulty tuning begins (no generator code ships without it). Treat the difficulty-curve sub-phase as a playtest-iteration loop — knob values (minimum solution length per tier, spatial-spread threshold, footprint sizes by level range) cannot be set by static analysis and must be playtested across the full level range before the `generatorVersion` contract is frozen. Quality gates (solution-length floor, spatial spread, branching-factor floor, symmetry-normalized dedup) reject-and-reseed degenerate boards.

### Phase 4: Assist — Hint & Undo
**Goal**: Players can request a hint that reveals a genuine next winning move and can undo their last move, with both affordances free in Slice 1 via the no-op `AdGateway` auto-grant stub. Hint logic is state-relative (never blind `solution[moveCount]` indexing): canonical line first, off-thread budgeted Solver fallback on deviation, and an explicit "no winning move" state when the position is genuinely unsolvable.
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: ASST-01, ASST-02, ASST-03
**Success Criteria** (what must be TRUE):
  1. While on the recorded solution line, the player can tap "hint" and see a valid next winning move highlighted.
  2. After the player deviates from the canonical line, the hint falls back to the Solver (on `Dispatchers.Default`, node/time-budgeted) and still returns a legal, win-preserving move.
  3. When the position is genuinely unsolvable, the player sees a clear "no winning move — undo or restart" state rather than a crash, illegal move, or silent no-op.
  4. Player can undo the last move, restoring the exact prior board state.
**Plans**: TBD

### Phase 5: Persistence, Resume & Settings
**Goal**: Players' progress survives leaving the app: per-level best results, the highest Challenge level reached (so Challenge mode continues where they left off), and a full in-progress resume (board cells + complete move history + elapsed time + `generatorVersion`), plus sound and haptics settings — all on a migration-safe Room/DataStore storage layer, with the `GamesService` no-op stub wired alongside `AdGateway` for Slice 2.
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: PERS-01, PERS-02, PERS-03, SET-01, SET-02, CHAL-05
**Success Criteria** (what must be TRUE):
  1. Player's per-level results (stars, best time, best score) persist across app restarts.
  2. Player can continue Challenge mode from their highest reached level after closing and reopening the app.
  3. Player can leave a game in progress and resume it with the board, full move history (undo still intact), and elapsed time exactly restored — the stored board is restored, never regenerated.
  4. Player can toggle sound and haptics on/off, and the choices persist.
  5. Storage is migration-safe: `exportSchema = true` with schema committed to VCS, explicit Room `Migration` objects with a migration test, and no `fallbackToDestructiveMigration` in release builds.
**Plans**: TBD

## Future Milestone (out of scope for this roadmap)

**Slice 2 — Online & Monetization (v2):** Swap Hilt bindings to add AdMob (`AdMobAdGateway` — rewarded ads for hint/undo/retry, frequency-capped interstitials with UMP consent) and Play Games Services v2 (`PlayGamesService` — optional sign-in, leaderboards, achievements, cloud save with explicit conflict-merge). No game or UI code changes — only the platform impls behind the interfaces established in Slice 1. SDK versions re-verified at integration time (PGS v2 + GMA move fast). Plus a visual theming/polish pass (POLI-01). Covers PGS-01..04, ADS-01..02, POLI-01.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Domain Foundation & Playable Board | 5/5 | Complete   | 2026-06-28 |
| 2. Classic Mode, Scoring & Navigation | 8/8 | Complete   | 2026-06-29 |
| 3. Challenge Generation & Difficulty Curve | 0/TBD | Not started | - |
| 4. Assist — Hint & Undo | 0/TBD | Not started | - |
| 5. Persistence, Resume & Settings | 0/TBD | Not started | - |
