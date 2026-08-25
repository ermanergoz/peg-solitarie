# Walking Skeleton — Peg Solitaire (Android)

**Phase:** 1
**Generated:** 2026-06-28

## Capability Proven End-to-End

> One sentence: the smallest user-visible capability that exercises the full stack.

A player can tap a peg on a real English-33 board, see its legal orthogonal destinations highlighted, tap a destination, and watch the peg slide into the hole while the jumped peg is removed — driven by a pure-Kotlin `RulesEngine` mutation surfaced through a real Compose Canvas tap, with CI standards gates green from the first commit.

> Note: this project has **no persistence layer in Phase 1** (domain is pure in-memory Kotlin). The standard skeleton's "one real DB read AND write" requirement is mapped to its meaningful equivalent here: **one real immutable domain state mutation** (`RulesEngine.applyMove` produces a new `Board`) wired to a real UI tap interaction. There is no database in this phase by design.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Language / build | Kotlin 2.4.0, AGP 9.2.0 (use 9.2.1 patch), Gradle 9.x wrapper, JDK 17 toolchain | Pinned in CLAUDE.md; KSP2 default; pure-Kotlin domain is the project core. detekt-alpha tested against AGP 9.2.1/Gradle 9.5.1 (RESEARCH Open Q1/Q2). |
| Module topology | Two modules: `:domain` (pure Kotlin/JVM, no Android plugin) + `:app` (Android) | `:domain` cannot import `android.*` structurally (strongest purity guard, RESEARCH Q1). `:app` depends on `:domain`. Exactly one split — no further modularization in Phase 1. |
| Domain purity gate | `:domain` JVM module (structural) **+** named ArchUnit bytecode boundary test (`DomainPurityTest`) | ArchUnit reads bytecode → immune to Kotlin-2.4 source-parse risk that affects Konsist's embedded 2.0.20 compiler (RESEARCH Q1). Satisfies success criterion #5 wording. |
| Board representation | `Board(spec: BoardSpec, pegs: Long)` — `Long` peg-bitmask behind `Board` accessors; `Position(row, col)` public coordinate | Free correct equals/hashCode, deterministic serialization for golden-master, forward-compatible with Phase-3 Solver/generator. All four classic footprints (≤41 holes) fit one 64-bit Long (RESEARCH Q2). |
| UI architecture | Compose + Material 3 (BOM 2026.06.00), MVVM/UDF: `GameViewModel` owns one immutable `@Immutable GameUiState` over `StateFlow`; board on one `Canvas`; Hilt for DI | CLAUDE.md mandate; RESEARCH Q5. Game-over is replayable `status` state, not a one-shot effect. No game logic in the Canvas. |
| Standards toolchain | `dev.detekt` 2.0.0-alpha.5 + ktlint-gradle 14.2.0, wired into `check`; custom dp/sp gate; convention plugins in `build-logic/` | Only detekt line that reads Kotlin 2.4 metadata (stable 1.x breaks the build — RESEARCH Pitfall T1). `MagicNumber` enforces no-magic-numbers; custom gate enforces no raw dp/sp (QUAL-03). |
| Persistence / platform SDKs | **None this phase.** No Room/DataStore; no AdMob/Play Games. Pure in-memory domain. | Persistence is Phase 5; SDKs are Slice 2 behind interfaces. Skeleton stays thin. |
| Directory layout | `com.app.pegsolitaire.domain.{model,rules}` in `:domain`; `com.app.pegsolitaire.{ui.game,ui.components,ui.theme,di}` in `:app` | Per design spec §8.5 package structure (RESEARCH Recommended Project Structure). |

## Stack Touched in Phase 1

- [x] Project scaffold (Gradle wrapper, version catalog, `build-logic` convention plugins, detekt + ktlint + ArchUnit, CI workflow) — Plan 01
- [x] Routing — single `GameRoute` hosting the game screen (full 5-screen navigation is Phase 2) — Plan 05
- [x] "Real read AND write" mapped to domain mutation — `RulesEngine.legalMoves` (read) + `applyMove` returning a new immutable `Board` (write) — Plan 02
- [x] UI — `BoardCanvas` tap hit-test wired to `GameViewModel`, animated jump + removal — Plans 04 & 05
- [x] Deployment — `./gradlew assembleDebug` builds a debug APK runnable on the present Pixel AVDs; emulator playtest checkpoint — Plan 05

## Out of Scope (Deferred to Later Slices)

> Anything that is *not* in the skeleton. Be explicit — this list prevents future phases from re-litigating Phase 1's minimalism.

- Results/scoring screen, stars, elapsed-time clock and numeric score (Phase 2)
- Home / mode-selection / Classic catalog navigation and the other three classic boards (English-33 only here) (Phase 2)
- The `LevelRepository` seam (Phase 2)
- Challenge generation, determinism `generatorVersion`, Solver (Phase 3)
- Hint and undo (Phase 4)
- Persistence (Room/DataStore), resume snapshot, settings (Phase 5)
- AdMob / Play Games SDKs (Slice 2, behind `AdGateway`/`GamesService`)
- Drag-to-jump input, arc/hop motion, pop removal, visual theming/polish pass (Slice 2 — D-02/D-07/D-08 deliver the baseline floor only)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: Home → pick 1 of 4 classic boards → play → results with stars/score; introduces the `LevelRepository` seam (replaces the D-05 game-over dialog with the Results screen).
- Phase 3: Infinite, deterministic, guaranteed-solvable generated Challenge levels behind `LevelRepository`; golden-master harness (built here) snapshots generator output.
- Phase 4: State-relative hint (canonical line + Solver fallback + "no winning move" state) and undo.
- Phase 5: Per-level results, continue-from-highest, in-progress resume, sound/haptics on migration-safe Room/DataStore.
