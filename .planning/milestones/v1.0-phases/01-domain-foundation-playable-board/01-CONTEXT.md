# Phase 1: Domain Foundation & Playable Board - Context

**Gathered:** 2026-06-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a pure-Kotlin domain core — immutable `Board`/`Cell`/`Position`/`Move`/`GameState` model plus a `RulesEngine` that enumerates legal orthogonal moves, applies a move, and detects game-over — and make **one hardcoded board playable end-to-end** in Compose: tap a peg → its legal destinations highlight → tap a hole → the peg slides over and the jumped peg is removed → game-over is detected → the player can restart. All of it is guarded from the first commit by CI standards gates: domain-purity boundary test (zero `android.*` in domain), detekt + ktlint as a build gate, no magic numbers/strings or hardcoded dp/sp (scoped constants + `dimens.xml`), and a JVM unit-test harness for `RulesEngine` including the golden-master determinism scaffold.

**In scope:** domain model + RulesEngine; one playable English-33 board; tap-to-jump interaction with highlighting and animation; game-over detection + minimal end-state signal; restart; CI gates + test harness.

**Out of scope (later phases):** Results/scoring screen and stars (Phase 2), Home/Classic/Challenge navigation and the other 3 classic boards (Phase 2), the LevelRepository seam (Phase 2), Challenge generation/Solver (Phase 3), hint/undo (Phase 4), persistence/resume/settings (Phase 5), drag input and visual-polish pass (Slice 2).

</domain>

<decisions>
## Implementation Decisions

### First Playable Board
- **D-01:** The single hardcoded Phase-1 board is **English-33** — the iconic cross, 33 holes, single center-empty start. Chosen because it's the most recognizable peg solitaire board (proves the engine on the board players expect), it's a pure orthogonal-grid layout (clean fit for orthogonal-only rules), and it is also one of the four Classic boards needed in Phase 2, so the layout/fixture work carries forward.

### Move Interaction Feel
- **D-02:** **Tap-only input** in Phase 1 — tap a peg to select (legal destinations highlight), tap a highlighted hole to jump. **No drag** in Phase 1 (drag is a candidate Slice-2 polish item and must not require domain changes).
- **D-03:** **Always two-step.** Even when a selected peg has exactly one legal destination, the player must still tap the destination — no auto-jump. Behavior is identical in every case (predictable, no accidental moves).
- **D-04:** **Deselect by re-tapping the selected peg.** Tapping the currently-selected peg again clears selection/highlights. Tapping a different one of the player's pegs re-selects that peg. Tapping an empty/non-legal cell is a no-op (does NOT deselect).

### End-State Signal & Restart
- **D-05:** On game-over (no legal moves remain), show a **simple modal dialog/overlay** displaying pegs remaining + a win/perfect message, with a Restart action in it. This is the minimal "signal the end state" for Phase 1 (there is no Results screen until Phase 2; this dialog is the natural seam the Phase-2 Results screen later replaces).
- **D-06:** **Persistent Restart control** (e.g. top-bar/icon) available at any time. If a game is in progress (moves made, not yet ended), Restart shows a quick confirmation to avoid accidental resets. At end-state, Restart in the dialog needs no confirmation.

### Jump / Removal Animation (baseline feel — full polish deferred to Slice 2)
- **D-07:** The jumping peg moves with a **straight-line slide** from its start cell to the destination hole (single `Animatable` `Offset`). Reads clearly as an orthogonal jump; an arc/hop is a Slice-2 polish candidate.
- **D-08:** The jumped-over peg is removed via **fade + slight shrink** (opacity → 0 while scaling down), timed to finish as the jumper lands.

### Claude's Discretion
Left to research + planning to resolve against the design spec and success criteria (user explicitly deferred these as technical):
- Gradle module topology / domain-purity enforcement — separate `:domain` JVM module (structural: can't import `android.*`) vs single app module with a Konsist/ArchUnit boundary test. Note: success criterion #5 names "Konsist/ArchUnit boundary test," which leans toward an in-module boundary check, but the planner decides.
- Internal board data representation (grid array vs position map vs other) and the `Position`/coordinate model.
- Exact scope of the golden-master determinism scaffold for Phase 1 (the determinism contract becomes load-bearing for the generator in Phase 3; here it's scaffolding over `RulesEngine`).
- Visual styling of "legal destination" highlighting.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product & domain design
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` — full approved design spec. Most relevant for this phase: §3 Game Rules (orthogonal jump definition, game-over, objective), §8 Architecture (Clean Architecture / MVVM / Repository), §8.2 Domain layer (model + `RulesEngine` responsibilities), §8.5 suggested package structure (`com.app.pegsolitaire` domain/data/ui/platform/di).

### Project governance & standards
- `CLAUDE.md` (repo root) — mandatory tech stack + code standards: pinned versions (Kotlin 2.4.0, AGP 9.2.0, Compose BOM 2026.06.00, Hilt 2.57.1, KSP not KAPT, etc.), no-magic-numbers/strings rule (scoped constants), `dimens.xml` for all UI dp/sp, detekt + ktlint as a build gate, strict Gitflow.
- `.planning/PROJECT.md` — Core Value, Key Decisions table, slicing strategy (Slice 1 = playable core, no SDKs).
- `.planning/ROADMAP.md` — Phase 1 goal, requirements (GAME-01..05, QUAL-01..04), and the 5 success criteria this phase is verified against.
- `.planning/REQUIREMENTS.md` — full text of GAME-01..05 and QUAL-01..04.

### UI implementation guidance
- `~/.claude/skills/compose-skill` — local Compose / UDF / state-hoisting practices the Compose layer must follow (per spec §8.1; project retains MVVM naming).
- Android app architecture guide — https://developer.android.com/topic/architecture (Clean Architecture reference cited by the constraints).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — greenfield project. Repo currently contains only `CLAUDE.md` and `README.md`; no Kotlin/Gradle code, no `.planning/codebase/` maps yet. Phase 1 establishes the project scaffold (Gradle/version catalog, module structure, Hilt, detekt/ktlint, test harness) from scratch.

### Established Patterns
- Patterns are prescribed (not yet present in code) by `CLAUDE.md` and the design spec: Clean Architecture (UI/MVVM · Domain · Data), unidirectional data flow with a single immutable `UiState` over `StateFlow`, board drawn on Compose `Canvas`, platform concerns behind `AdGateway`/`GamesService` interfaces (no-op stubs in Slice 1).

### Integration Points
- The Compose UI (game screen ViewModel) is the first consumer of the domain `RulesEngine` — tap events map to domain `Position`/`Move`, domain returns legal moves + new `GameState`, UI renders + animates. This boundary (UI ↔ pure domain) is what the domain-purity CI gate protects.

</code_context>

<specifics>
## Specific Ideas

- "Satisfying jump/removal feel" is a stated core goal (spec §2) — the straight-slide + fade-shrink baseline (D-07/D-08) is the Phase-1 floor for that feel, with arc/pop reserved for the Slice-2 polish pass.
- The end-state dialog (D-05) should be treated as a deliberate seam: it is the minimal stand-in that the Phase-2 Results screen (stars + score) will later replace at the same point in the flow.

</specifics>

<deferred>
## Deferred Ideas

- **Drag-to-jump input** — raised under interaction; deferred to the Slice-2 polish pass (must not require domain changes).
- **Arc/hop jump motion and pop removal** — richer animation feel; deferred to the Slice-2 visual-polish pass (POLI-01).
- **Auto-jump on single legal move** — considered and explicitly rejected for consistency (see D-03); not revisiting unless playtest feedback warrants.

</deferred>

---

*Phase: 1-domain-foundation-playable-board*
*Context gathered: 2026-06-28*
