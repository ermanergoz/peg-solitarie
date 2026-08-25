# Phase 3: Challenge Generation & Difficulty Curve - Context

**Gathered:** 2026-07-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver an **infinite, sequential stream of procedurally-generated Challenge levels** behind the existing `LevelRepository` seam:
- **Deterministic** — level N is always the same board (seed = stableHash(N)); carries a `generatorVersion`.
- **Guaranteed solvable by construction** — reverse-generation from the solved 1-peg state, which also yields a recorded winning line.
- **Difficulty scales with N** — via solution length (primary), then footprint size/shape, peg density, and starting empty-pattern (data-driven `DifficultyTier`).
- Routed through `LevelRepository` (replacing the Phase-2 `StubChallengeLevelSource`), with determinism locked by a **golden-master CI test** before any difficulty tuning.

**Requirements:** CHAL-01, CHAL-02, CHAL-03, CHAL-04. (CHAL-05 "continue from highest level" + PERS-02 persistence are Phase 5 — see Deferred.)

**In scope:** the generator (reverse-gen + quality gates), deterministic seeding + `generatorVersion`, the data-driven difficulty tier table, the golden-master determinism test, the `ChallengeLevelSource` wiring behind `LevelRepository`, and the Challenge play/results/progression UI (Challenge entry from Home, gameplay reusing the Phase-2 board, a Challenge-aware Results screen).

**Out of scope:** hints/undo (Phase 4), persistence of highest level / resume (Phase 5), leaderboards & daily challenges (Slice 2), diagonal jumps (never — orthogonal-only both modes).
</domain>

<decisions>
## Implementation Decisions

### Progression flow
- **D-01: Solve → Results → "Next level".** A solved Challenge level goes to the Phase-2 Results screen (stars/score/time). Results shows a **"Next level"** action that advances to level N+1. This is where the Challenge "next board" action lives (Classic deliberately has no next-board action — see quick task 260701-n9t). Results therefore needs mode/context awareness (Classic vs Challenge) to conditionally show "Next level".
- **D-02: Must solve to advance.** Only a 1-peg solve unlocks level N+1. There is no skip in this phase. Levels are guaranteed solvable, so this is a fair ladder-of-mastery.
- **D-03 (default, Claude's discretion): show "Level N" during play** in the Challenge gameplay top bar.
- **D-04 (default, deferred behavior): Challenge starts at level 1 each session this phase.** Resuming from the player's highest reached level is CHAL-05/PERS-02 (Phase 5). The current level should be tracked in a way that Phase 5 can later back with persistence (e.g. a `ChallengeProgress` seam), but no DataStore persistence is required in Phase 3.

### Difficulty curve feel (intent — exact knob values are playtested, not set now)
- **D-05: Gentle start, smooth steady climb.** Levels ~1-10 are easy/tutorial-ish (small boards, short solutions); difficulty then increases smoothly and continuously forever (no hard tier walls, no early plateau).
- **D-06: Solution length is the primary difficulty lever.** Raise the required winning-line length (depth of planning) first; grow footprint size later and more slowly. Difficulty should read as "trickier," not merely "bigger." This maps directly to `tier.solutionLength` in the reverse-generation loop.
- **D-07: Difficulty knobs are a data-driven table** (`DifficultyTier`), tuned by playtest before `generatorVersion` is frozen. Knob VALUES (solution-length floor per level range, footprint sizes by range, density/spread thresholds, freeform-introduction level) are explicitly NOT set in this context — they are a playtest-iteration loop per the roadmap risk note.

### Board shapes
- **D-08: Canonical footprints early, freeform shapes later.** Early levels generate onto the known-good canonical square footprints (English-33 cross, European-37, Diamond-41, Wiegleb-45, and smaller variants), reusing the existing generalized `BoardGeometry`/rendering. Freeform shapes (arbitrary connected shapes within an NxN bounding grid) are introduced at higher levels as an added variety lever.
- **D-09: Freeform validity guardrails (technical constraint, Claude handles).** Freeform boards MUST pass validity gates: connected (no isolated cells / unreachable regions), ≤ 64 in-play cells (the domain `Board` is a `Long` bitmask), and expressed on the existing square-grid coordinate system so `BoardGeometry` and `BoardCanvas` render them unchanged. Prefer symmetric/aesthetically-intentional shapes where cheap.

### Stuck & scoring
- **D-10: Stuck → Results → Retry (no "Next level").** Reaching a stuck/dead-end terminal state (2+ pegs, no legal moves) routes to the Results screen showing the outcome with **Retry** (reload the same deterministic board) and **Home** — but NOT "Next level" (the level wasn't solved). Consistent with the solve flow; honest about the outcome. (Hints/undo that would rescue a stuck board are Phase 4.)
- **D-11: Level-scaled score.** Reuse the star model — a 1-peg solve is 3★ (so stars are near-constant in Challenge since solving is required to advance; a stuck finish shows fewer stars on the Results screen but does not advance). The **numeric score is scaled by level/difficulty** (a difficulty-aware multiplier over the Phase-2 `ScoreCalculator` base) so climbing the ladder makes the running score meaningful. This is a small, additive scoring change — do NOT re-base stars onto speed/efficiency in this phase.

### Claude's Discretion
- Level-number display (D-03) and level-1 start (D-04) are sensible defaults the user did not object to.
- Exact difficulty knob values (D-07), the freeform-introduction level (D-08), and freeform symmetry preferences (D-09) are left to implementation + playtest.
- Whether Results gains a `mode`/`showNextLevel` flag vs a separate Challenge-results route is an implementation choice for the planner (D-01/D-10 define the behavior).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Generator design & determinism (highest priority)
- `.planning/research/ARCHITECTURE.md` §Generator / "Pattern 3: Reverse-generation level generator" — the concrete reverse-generation design: inverse-move definition (+1 peg), `generate(levelNumber)` loop, `Prng`, `DifficultyTier`, degenerate-board guards, `LevelRepository.getLevel(LevelRef.Challenge)` routing.
- `.planning/research/PITFALLS.md` §Pitfall 1 (non-deterministic generation despite seeded PRNG — sorted candidate list, portable hash, single-threaded, golden-master test), §Pitfall 2 (versioned frozen generator contract + `generatorVersion` from day one; data-driven knobs; never mutate v1 in place), §Pitfall 3 (trivial/degenerate/duplicate boards — quality gates: min solution length, degenerate-forced-line reject, symmetry-normalized dedup, reject-and-reseed).
- `.planning/research/FEATURES.md` — reverse-generation as the headline differentiator; recorded winning line → free hints (Phase 4); deterministic seeding underpins retry/resume/leaderboards.

### Project constraints & requirements
- `.planning/PROJECT.md` — Core Value ("infinite, never-repeating, always-solvable"); Key Decisions (Phase 3 generator = #1 risk; golden-master locked before tuning; `generatorVersion` from day one; knobs data-driven and frozen before release).
- `.planning/REQUIREMENTS.md` — CHAL-01..04 (this phase), CHAL-05/PERS-02 (Phase 5), QUAL-04 (JVM golden-master determinism test).
- `.planning/ROADMAP.md` §"Phase 3" — goal, success criteria, and the Risk note (golden-master before tuning; difficulty as a playtest-iteration loop; quality gates reject-and-reseed).
- `CLAUDE.md` — code standards (pure-Kotlin domain, no magic numbers/strings → constants files, dimens for UI, detekt+ktlint gate, Gitflow).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`LevelRepository` seam (Phase 2)** — `DefaultClassicLevelSource` + `StubChallengeLevelSource` (currently throws). Phase 3 replaces the stub with a real `ChallengeLevelSource` that maps level-number → seed → generator; the UI/use cases never branch on classic-vs-challenge.
- **`RulesEngine` (Phase 1, pure domain)** — provides `legalMoves`/`applyMove`/`isGameOver`; the reverse-generation "un-jump" is its inverse (peg at C, B & A empty, colinear spacing-2 → C empty, B & A pegs). Reuse, don't reimplement.
- **`ScoreCalculator` / `ScoreParams` / `ScoreResult` (Phase 2)** — reuse for D-11; add a level/difficulty multiplier over the numeric score, keep the star bands.
- **Golden-master harness (Phase 1: `BoardCodec` + `GoldenMasterAssert`, committed fixtures)** — reuse the exact encoding/assert contract for the CHAL-03 determinism fixture (levels 1/2/10/100/1000).
- **`BoardGeometry` (Phase 2, generalized to 7/8/9 grids)** and **`BoardCanvas`** — render generated boards unchanged; freeform must stay on the square grid + ≤64 cells.
- **Board = immutable `Long` peg-bitmask (Phase 1)** — ≤64 in-play cells; constrains freeform footprints.
- **Type-safe routes (Phase 2, `Routes.kt`) + `AppNavHost`** — add a Challenge gameplay route (carries `levelNumber`) and a Challenge-aware Results path (solved vs stuck; "Next level" only when solved).

### Established Patterns
- Pure-Kotlin domain (`:domain`, no Android) for `LevelGenerator`/`Solver`/`Prng`/`DifficultyTier` → JVM unit tests, highest coverage (this is the #1-risk code).
- MVVM + `StateFlow` UiState, stateless composables (state hoisting); one-shot nav events (Channel) as used by Phase-2 gameplay finish.
- Data-driven constants split by scope (no magic numbers) — the `DifficultyTier` table lives in data/config, not inline literals.

### Integration Points
- `ChallengeLevelSource` (data) wraps the domain `LevelGenerator` + seed mapping behind `LevelRepository`.
- Challenge gameplay reuses the Phase-2 `GameViewModel`/board flow, parameterized by a generated board + (for Phase 4) the recorded solution.
- Challenge progression (current level N, advance on solve) needs a small holder this phase that Phase 5 can back with persistence (`ChallengeProgress`).

</code_context>

<specifics>
## Specific Ideas

- The "Next board/level" action the user asked to remove from Classic (quick task 260701-n9t) is intentionally reintroduced HERE as Challenge's "Next level" (D-01) — Challenge is its rightful home.
- Difficulty should feel like "gentle on-ramp, then endlessly, smoothly trickier — deeper puzzles before bigger boards" (D-05/D-06).
- Boards start familiar (canonical shapes), then surprise with novel freeform shapes deeper in (D-08).

</specifics>

<deferred>
## Deferred Ideas

- **Continue from highest Challenge level reached / resume** (CHAL-05, PERS-02) — Phase 5 (persistence). Phase 3 tracks current level in-memory only.
- **Hints (recorded-line + solver fallback) and undo** (ASST-01..03) — Phase 4. These are what would rescue a stuck Challenge board; until then, stuck → Retry (D-10).
- **Leaderboards on highest level / best scores** — Slice 2 (Play Games). Enabled BY this phase's determinism, but not built now.
- **Daily challenges** — deferred (Slice 2+); the infinite deterministic ladder already supplies the "next" loop.
- **Re-basing stars on speed/move-efficiency** (considered under scoring) — not this phase; D-11 keeps the star model and only scales the numeric score.

None of the above are scope creep into Phase 3 — all are explicitly later-phase or later-slice.

</deferred>

---

*Phase: 3-challenge-generation-difficulty-curve*
*Context gathered: 2026-07-01*
