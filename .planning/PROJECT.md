# Peg Solitaire (Android)

## What This Is

A polished native Android peg solitaire game with two modes: a **Classic** mode of five hand-defined boards (English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45), and a **Challenge** mode that serves an effectively infinite stream of sequential, procedurally-generated levels that are each unique and guaranteed solvable. Players earn 1–3 stars per level, get solver-backed hints and unlimited undo, and their progress — per-level bests, Challenge ladder position, and in-progress games — survives leaving the app. For casual puzzle players on Android.

## Current State

**v1.0 "Slice 1 — Playable Core" shipped 2026-08-26.** All 30 v1 requirements validated; milestone audit passed (5/5 E2E flows, integration 9.5/10, zero leftover stubs). ~17.6k lines of Kotlin across a pure-JVM `:domain` module and a Compose `:app`, 293 JVM tests green, phase 5 additionally code-reviewed (all Critical/Warning findings fixed) and security-audited (14/14 threats closed). No external SDKs — `AdGateway` and `GamesService` are no-op stubs awaiting Slice 2.

## Core Value

Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying — if nothing else works, the procedurally-generated puzzles must be solvable and fun.

**Validated in v1.0:** the reverse-generation Challenge generator ships frozen behind golden-master fixtures — deterministic, gate-checked, always solvable by construction.

## Current Milestone: v1.1 Slice 2 — Play Games, AdMob & Polish

**Goal:** Turn the shipped offline game into a release-ready product by wiring real Play Games and AdMob SDKs behind the existing seams, adding sound, and paying down carried tech debt.

**Target features:**
- Google Play Games Services v2 behind the existing `GamesService` seam — sign-in, leaderboards, achievements, cloud save
- AdMob + UMP consent behind the existing `AdGateway` seam — rewarded ads for hint/undo/retry, interstitials between levels
- Sound playback consuming the already-persisted `soundOn` flag
- Carried tech debt: solver deadline envelope on hard early positions, Diamond-41 solvability fixture, Nyquist gaps on phases 3/4, security advisories WARN-02/03

**Key context:** The Candy Clay visual redesign (merged to `develop` 2026-08-27, outside GSD planning) already delivers the bulk of the former POLI-01 "visual polish pass" — remaining polish in this milestone is audio/juice, not a visual overhaul. SDK versions are pinned at integration time per the stack doc (play-services-games-v2 ~21.x, play-services-ads 23.6.0, UMP latest).

## Requirements

### Validated

- ✓ Correct orthogonal jump rules with clear win/lose detection — v1.0
- ✓ Classic mode with 5 boards (scope grew from 4: English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45; Triangle-15 dropped — square-grid orthogonal rules) — v1.0
- ✓ Infinite, sequential, unique, guaranteed-solvable Challenge levels — v1.0
- ✓ Deterministic levels (level N always the same board; golden-master locked) — v1.0
- ✓ Difficulty scaling via solution length, footprint pool, peg density, freeform boards — v1.0
- ✓ 1–3 stars from pegs remaining; time-decay score as separate ranking — v1.0
- ✓ Hint (recorded-line-first, Solver fallback, honest "no winning move" state) and multi-step undo — v1.0
- ✓ Local persistence: per-level bests, highest Challenge level, full in-progress resume by replay — v1.0

### Active (v1.1)

- [ ] Google Play Games v2: sign-in, leaderboards, achievements, cloud save — behind existing `GamesService` seam
- [ ] AdMob monetization: rewarded ads for hint/undo/retry, interstitials between levels — behind existing `AdGateway` seam
- [ ] Sound playback consuming the already-persisted `soundOn` flag (visual polish largely delivered by the Candy Clay redesign, merged 2026-08-27)
- [ ] Tech debt payoff: solver deadline envelope on hard early positions, Diamond-41 solvability fixture, Nyquist gaps on phases 3/4, security advisories WARN-02/03

### Out of Scope

- HTML5 / Google H5 Games Ads — incompatible with the chosen native Kotlin build; AdMob is the native equivalent
- Online / real-time multiplayer — not core to a single-player puzzle game
- Daily challenges, custom board editor, level sharing — deferred; not needed to validate core value
- Multiple visual themes — ship one clean theme first
- Compose Multiplatform / iOS — Android-only for now
- Diagonal jumps — Challenge keeps classic orthogonal-only rules
- Triangle-15 board — triangular geometry conflicts with the square-grid orthogonal RulesEngine (replaced by Asymmetric-39 + Wiegleb-45)

## Context

- Shipped v1.0: ~17.6k LOC Kotlin (pure-JVM `:domain` + Compose `:app`), 293 JVM tests, detekt/ktlint/dp-sp/ArchUnit-purity gates all green in `check`.
- Persistence: Room 2.8.4 (per-level bests + resume snapshots, migration-safe with committed schema) + DataStore Preferences (settings, challenge ladder, onboarding). All reads fail open, all writes best-effort, snapshot restore replay-validates every stored move.
- Core generation technique: **reverse-generation** — start from the solved state and play legal jumps in reverse, seeded by level number. Guarantees solvability and records the winning solution used for hints. Output FROZEN behind golden-master fixtures (levels 1/2/10/100/1000) stamped with `generatorVersion`.
- Design spec: `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md`. Compose work follows the local `compose-skill`.
- Known carried debt (documented in v1.0-MILESTONE-AUDIT.md): solver deadline envelope on hard early positions, Diamond-41 solvability fixture, Nyquist partial coverage on phases 3/4, security advisories WARN-02/03.

## Constraints

- **Tech stack**: Native Kotlin + Jetpack Compose (Material 3) — chosen for performance and tight Play integration.
- **Architecture**: Clean Architecture per the official Android guide (https://developer.android.com/topic/architecture) — UI layer with **MVVM** (StateFlow UiState, unidirectional data flow), optional Domain layer of use cases, Data layer using the **Repository pattern** as single source of truth. Hilt for DI; Coroutines/Flow for async.
- **Game logic isolation**: domain logic is pure Kotlin, fully unit-testable, with no Android dependencies; ads and Play Games sit behind interfaces (`AdGateway`, `GamesService`).
- **Rules**: orthogonal-only jumps across both modes.
- **Code standards (mandatory)**:
  - No magic numbers or strings in logic/UI — extract to constants files, **split by scope** (e.g. per-feature/per-layer constants objects) rather than one global dump.
  - Compose: use **state hoisting** (stateless composables; state owned by ViewModels), per the locally installed `compose-skill`.
  - Use a **dimens** resource file for all UI dimensions (spacing, sizes) — no hardcoded dp/sp scattered in composables.
  - **detekt** and **ktlint** configured and passing — treated as a build/verification gate, not optional.
- **Git workflow**: follow **Gitflow strictly** — long-lived `main` (releases) and `develop` (integration); work on `feature/*` branches off `develop`, `release/*` for release prep, `hotfix/*` off `main`. No direct commits to `main`/`develop` outside merges.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Native Kotlin (not HTML5/engine) | Performance + tightest Play Games integration | ✓ Good — Canvas + Compose handled the whole game; no engine needed |
| AdMob instead of H5 Games Ads | H5 Games Ads only works in HTML5 games; AdMob is the native equivalent | — Pending (Slice 2) |
| Stars from pegs remaining; time feeds ranking score | Avoids frustration of withholding stars on slow-but-perfect solves | ✓ Good — shipped as designed (SCOR-01/02) |
| Reverse-generation for Challenge levels | Guarantees solvability and yields a recorded solution for free hints | ✓ Good — the core value shipped exactly on this technique; solution powers recorded-line hints |
| Deterministic level seeding by level number | Reproducible levels enable retry/resume/fair comparison | ✓ Good — golden-master fixtures lock determinism in CI |
| Build in slices (core first, then online/ads) | Fastest path to a playable, testable game | ✓ Good — v1.0 shipped with zero SDK dependencies; seams ready |
| Clean Architecture + MVVM + Repository pattern | User preference; aligns with official Android guidance | ✓ Good — repository seams (Level/Progress/Settings) absorbed every phase handoff cleanly |
| Kotlin 2.3.20 / KSP 2.3.9 / Hilt 2.60 / AGP 9.1.0 pin cascade (2026-06-28) | KSP availability + tooling max-supported AGP; no KAPT anywhere | ✓ Good — full gate green for the entire milestone |
| Undo/restore as pure replay (fold applyMove over history) | One source of truth (RulesEngine); no serialized board-state trust | ✓ Good — reused for resume; every stored move re-validated on restore |
| Room for results+resume, DataStore for scalars | Queries/upserts need SQL; two ints + flags don't | ✓ Good — migration-safe v1 schema committed; fail-open/best-effort hardening added post-review |
| Solver = memoised symmetry-canonical DFS + pagoda pruning, 1.5s deadline | CI-fast honest guarantee; universal early-position completeness not achievable at usable deadline | ⚠️ Revisit — hard early positions on big boards fold to "no winning move"; candidate Slice-2 solver improvement |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-27 at v1.1 milestone start*
