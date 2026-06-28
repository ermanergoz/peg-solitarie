# Peg Solitaire (Android)

## What This Is

A polished native Android peg solitaire game with two modes: a **Classic** mode of well-known hand-defined boards, and a **Challenge** mode that serves an effectively infinite stream of sequential, procedurally-generated levels that are each unique and guaranteed solvable. Players earn 1–3 stars per level. For casual puzzle players on Android.

## Core Value

Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying — if nothing else works, the procedurally-generated puzzles must be solvable and fun.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Player can play peg solitaire with correct orthogonal jump rules and clear win/lose detection
- [ ] Player can play Classic mode boards (English-33, European-37, Triangle-15, Diamond-41)
- [ ] Player can play infinite, sequential Challenge levels that are unique and guaranteed solvable
- [ ] Challenge levels are deterministic — level N is always the same board (retry/resume/compare)
- [ ] Challenge difficulty scales via board size/shape, peg density, and starting pattern
- [ ] Player earns 1–3 stars based on pegs remaining, with elapsed time feeding a separate ranking score
- [ ] Player can use a hint (next winning move) and undo the last move
- [ ] Player progress is saved locally — per-level results, highest level, in-progress resume
- [ ] (Slice 2) Google Play Games: sign-in, leaderboards, achievements, cloud save
- [ ] (Slice 2) AdMob monetization: rewarded ads for hint/undo/retry, interstitials between levels

### Out of Scope

- HTML5 / Google H5 Games Ads — incompatible with the chosen native Kotlin build; AdMob is the native equivalent
- Online / real-time multiplayer — not core to a single-player puzzle game
- Daily challenges, custom board editor, level sharing — deferred; not needed to validate core value
- Multiple visual themes — ship one clean theme first
- Compose Multiplatform / iOS — Android-only for now
- Diagonal jumps — Challenge keeps classic orthogonal-only rules

## Context

- Greenfield project, native Android, Kotlin.
- A full design spec exists and is committed: `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md`.
- Core generation technique: **reverse-generation** — start from the solved state (one peg) and play legal jumps in reverse, driven by a level-number-seeded PRNG. This guarantees solvability and records a winning solution used for free hints.
- Compose UI implementation will follow the locally installed `compose-skill` (`~/.claude/skills/compose-skill`).
- Build is sliced: Slice 1 = playable core (testable, no SDKs); Slice 2 = Play Games + AdMob + polish.

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
| Native Kotlin (not HTML5/engine) | Performance + tightest Play Games integration | — Pending |
| AdMob instead of H5 Games Ads | H5 Games Ads only works in HTML5 games; AdMob is the native equivalent (rewarded/interstitial) | — Pending |
| Stars from pegs remaining; time feeds ranking score | Avoids frustration of withholding stars on slow-but-perfect solves while still rewarding speed | — Pending |
| Reverse-generation for Challenge levels | Guarantees solvability and yields a recorded solution for free hints | — Pending |
| Deterministic level seeding by level number | Reproducible levels enable retry/resume/fair comparison | — Pending |
| Build in slices (core first, then online/ads) | Fastest path to a playable, testable game | — Pending |
| Clean Architecture + MVVM + Repository pattern | User preference; aligns with official Android guidance | — Pending |
| Kotlin pin relaxed 2.4.0 → 2.3.20 to obtain a KSP build for Hilt; KSP + Hilt + no-KAPT retained (2026-06-28) | KSP's latest build (symbol-processing 2.3.9) targets Kotlin 2.3.20; no KSP exists for 2.4.x, which blocked Hilt's annotation processor. 2.3.20 is the latest 2.3.x with an exact KSP match. Also bumped Hilt 2.57.1 → 2.60 (2.57.1's Gradle plugin needs the legacy Android BaseExtension, dropped by AGP 9.2.1). | ✅ Applied — Hilt graph generates under KSP; full gate green |
| AGP pinned to 9.1.0 (tooling max-supported); re-resolved Gradle/Hilt cascade accordingly (2026-06-28) | User Android tooling supports at most AGP 9.1.0; 9.2.1 was incompatible. AGP 9.1.0 also dropped the legacy Android BaseExtension, so Hilt stays 2.60 (2.57.1 fails to apply); Gradle 9.5.1 wrapper kept (no too-new advisory under AGP 9.1.0); Kotlin 2.3.20/KSP 2.3.9 and compileSdk-36 core/lifecycle pins unchanged. | ✅ Applied — full gate (ktlint/detekt/:domain:test/test/assembleDebug + dp-sp + ArchUnit) green, exit 0 |

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
*Last updated: 2026-06-28 after initialization*
