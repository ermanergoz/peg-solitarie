# Phase 2: Classic Mode, Scoring & Navigation - Context

**Gathered:** 2026-06-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver the complete **five-screen navigation flow** (Home → Classic catalog → gameplay → results) and make **all four classic boards** (English-33, European-37, Triangle-15, Diamond-41) browsable, selectable, and playable to completion with the existing orthogonal jump rules. Add **scoring** — 1–3 stars from pegs remaining plus a separate time-based numeric score — surfaced on a dedicated **results screen** that replaces the Phase-1 `GameOverDialog` seam. Stand up the **`LevelRepository` seam** as the single source of truth for playable levels (Classic source real; Challenge source stubbed so Phase 3's generator slots in behind it). Add **first-time onboarding** (a how-to-play overlay for the jump rule).

**In scope:** Home (mode select) + Classic catalog screens; nav graph wiring all five screens; the 3 new board footprints + their initial states; selected-board → gameplay flow (board now comes from selection, not a hardcoded inject); `ScoreCalculator` (stars + score) in pure domain; results screen (stars/pegs/time/score + actions); `LevelRepository` interface with a real Classic source and a stubbed Challenge source; first-time onboarding overlay + a single `onboardingSeen` DataStore flag + a permanent "How to play" affordance.

**Out of scope (later phases):** Challenge generation/Solver and difficulty curve (Phase 3 — only the repo stub here); hint/undo (Phase 4); saved per-level results, "best stars/score" badges, highest-level continue, in-progress resume, sound/haptics settings, and the broader DataStore/Room persistence layer (Phase 5 — only the one onboarding flag is pulled in early); drag input and the visual-polish animation pass (Slice 2).

</domain>

<decisions>
## Implementation Decisions

### Scoring — Stars
- **D-01:** **Stars come from absolute pegs remaining, identical on every board** — 1 peg left = 3★, 2–3 pegs left = 2★, finished-but-more-than-3 = 1★. Chosen over size-relative or per-board tuned thresholds because it matches the canonical "solve to one peg" goal, is board-agnostic, and is trivial to explain. The exact 2★ band cutoff (here 2–3 pegs) is a named constant the planner may sanity-check, but the *approach* (absolute pegs) is locked.
- **D-02:** **The target hole is ignored for stars.** Stars depend solely on peg count; a board's target/center hole (e.g. English-33's center) is flavor, not a star condition. Keeps scoring uniform across boards that may not define a meaningful target and avoids harsher 3★ gating.

### Scoring — Score & Timer
- **D-03:** **Score = peg base + decaying time bonus, independent of stars.** Shape: `score = pegsCleared × PEG_VALUE + max(0, TIME_BONUS_CAP − elapsedSeconds × DECAY)` (exact constants to be set in planning). Stars and score are separate axes — a slow perfect solve still earns 3★ but a smaller score. Rejected pure-speed (ignores how well the board was cleared) and multiplicative (small time deltas swing the score too much).
- **D-04:** **The timer is visible during play** (in the game top bar), so the speed stakes are legible in real time. Elapsed time is the input to D-03's score.

### Results Screen
- **D-05:** **Results is a full, dedicated navigation destination** (the 5th screen), not a modal. It is the screen the phase goal's "results with stars/score" calls for and the explicit replacement for the Phase-1 `GameOverDialog` (D-05 in Phase 1 was built as this seam).
- **D-06:** **Results actions: Replay (same board) / Next board / Catalog / Home.** "Next board" navigates to the next board in the catalog's ascending-size order (see D-12) and **wraps** at the end.
- **D-07:** **A "stuck" loss (no legal moves, >1 peg) uses the same results screen** — it simply yields a low star count (typically 1★) plus the score. One consistent end-flow; the star count already encodes the outcome. No separate loss/retry screen.
- **D-08:** **The results headline is tiered to the star outcome** (e.g. 3★ "Perfect!", 2★ "Solved!", 1★ "Cleared" / "Stuck"), reusing the Phase-1 D-05 win/perfect copy-tier idea. Exact copy is the UI's to finalize.

### Home & Classic Catalog
- **D-09:** **Challenge mode is shown but disabled with a "Coming soon" badge** on Home. Reserves the slot honestly so Home's layout does not change when Phase 3 lands; no placeholder route is built.
- **D-10:** **Home is a mode-select screen (Classic / Challenge) and the Classic catalog is a separate screen.** Matches NAV-01 (Home with entries to both modes) and the full five-screen flow; the stubbed Challenge entry slots cleanly into the same structure later.
- **D-11:** **The catalog presents each board as a card with a mini board-shape preview** (reusing Phase 1's `BoardCanvas`/`BoardGeometry` to render the footprint) plus name and hole count. Visual and recognizable; reuses existing rendering rather than a flat text list.
- **D-12 [informational]:** ~~**Boards are ordered by ascending size: Triangle-15 → English-33 → European-37 → Diamond-41.**~~ **SUPERSEDED by D-17 below** (Triangle-15 removed). Not independently tracked — the active ascending-hole-count ordering is captured by D-18, covered by plans 02-04/02-07. Catalog ordering is still ascending hole count, over the new 5-board set.

### Board Catalog Resolution (post-research, user-approved 2026-06-28) — overrides D-11/D-12 + the Triangle-15 discretion item
- **D-17 (BLOCKING conflict resolved):** Research found **Triangle-15 is irreconcilable with the locked orthogonal-only jump rule** — authentic triangular peg solitaire requires 3-axis (diagonal) jumps, which `RulesEngine` (4 orthogonal directions) cannot express and the project bans. **Resolution (user decision):** **drop Triangle-15 entirely** and adopt a **5-board, all-orthogonal-square-grid catalog**, so the orthogonal-only rule and `RulesEngine` stay unchanged and **no triangular-lattice geometry is built**.
- **D-18 (the locked catalog — footprints VERIFIED by follow-up research):** The Classic catalog is exactly these **five** boards (standard peg-solitaire variants, all orthogonal square-grid, all fitting the ≤64-hole `Long`-bitmask `BoardSpec`):
  1. **English-33** — standard cross, 33 holes, 7×7. Already built in Phase 1. Start: center `(3,3)`.
  2. **European / French-37** — octagon, 37 holes, 7×7 (per-row 3-5-7-7-7-5-3). **Off-center start mandatory** — center-empty is provably unsolvable to one peg; locked start `(3,2)` (an orthogonal neighbour of center).
  3. **Asymmetric 3-3-2-2** (George Bell) — **39 holes, 8×8** (per-row 3-3-3-8-8-8-3-3; in-play iff `row in 3..5 || col in 2..4`; center `(4,3)`). Center start is solvable.
  4. **Diamond-41** — square diamond, 41 holes, 9×9 (per-row 1-3-5-7-9-7-5-3-1). **Off-center start mandatory** — center is ALSO provably unsolvable (corrects earlier assumption); use a solver-validated off-center vacancy (candidate `(4,2)`), locked by D-19.
  5. **J.C. Wiegleb-45** (Germany, 1779) — German cross, 45 holes, 9×9 (per-row 3-3-3-9-9-9-3-3-3; in-play iff `row in 3..5 || col in 3..5`; center `(4,4)`). Center start is solvable.
  - The only `BoardGeometry`/`BoardMetrics` generalization needed is **deriving the grid dimension per board** (7/8/9) from the `BoardSpec`, away from the hardcoded English-33 `GRID_DIMENSION = 7` / `aspect = 1f`. **No `BoardLattice`/triangle strategy.** `RulesEngine` needs ZERO changes (all orthogonal).
  - **Catalog order (replaces D-12):** ascending hole count → English-33 (33) → European-37 (37) → **Asymmetric-39 (39)** → Diamond-41 (41) → Wiegleb-45 (45). This sequence also defines the "Next board" wrap order (D-06).
- **D-19 (solvability validation, no Solver yet — GENERATE, don't transcribe):** No runtime Solver until Phase 4. For each non-trivial board, commit a **recorded-solution golden fixture** (a winning `List<Move>` reducing the start to a single peg). Generate fixtures with a **one-off authoring-time DFS/IDDFS solver kept OUT of `:domain` main** (a `tools/` or test source set), run once against the real `Boards.*` value, serialized to `domain/src/test/resources/golden/solutions/`. The **shipped golden test only replays** the fixture through `RulesEngine` asserting `pegCount == 1` — no search ships in any runtime path. This guarantees 3★ is reachable (D-01 fairness), confirms the locked off-center starts for European/Diamond, and forward-feeds the Phase-3 generator contract. (Hand-transcribing published solutions is rejected — foreign coordinate conventions silently mis-validate.)

### Onboarding
- **D-13:** **First-time how-to-play is a dismissable, illustrated overlay** (a few steps showing the orthogonal jump: a peg hops an adjacent peg into an empty hole; the jumped peg is removed). Skippable; low build cost vs a dedicated screen or interactive demo.
- **D-14:** **It triggers on first app launch, over Home** (once), rather than on first board entry.
- **D-15:** **A permanent "How to play" / help affordance** exists (e.g. on Home or the game top bar) so the rule can be re-read any time after the first-run overlay is dismissed.
- **D-16:** **"Onboarding seen" is tracked with a single DataStore-Preferences boolean (`onboardingSeen`) introduced now.** Forward-compatible: Phase 5 folds this key into the broader settings store. **Scope note:** this deliberately pulls one small slice of the persistence stack into Phase 2 (only this one flag); all other persistence — per-level results, resume, settings — stays in Phase 5. Add `androidx.datastore:datastore-preferences` (1.2.1 per stack) for this flag only.

### Claude's Discretion
Left to research + planning to resolve against the design spec, the existing code, and success criteria:
- **Navigation library wiring** — introducing Navigation Compose 2.9.8 (already in the stack, not yet wired; Phase 1 routes `MainActivity → GameRoute` directly) and the type-safe nav graph for the five destinations.
- **How the selected board reaches `GameViewModel`** — the VM currently takes a hardcoded injected `Board` (`DomainModule.provideInitialBoard`); planning decides the mechanism (nav arg + `SavedStateHandle`, repository lookup by level id, etc.) so the played board comes from the user's catalog selection.
- **`LevelRepository` interface shape** — the abstraction that serves a level (board + spec + metadata) with a real Classic source and a stubbed Challenge source; design it so Phase 3's generator drops in behind the same seam.
- **The four new board footprints + initial states** — European-37, Diamond-41, Wiegleb-45, Asymmetric 3-3-2-2 (per D-18) in-play positions and starting empty hole(s), following the standard definitions; all fit the existing ≤64-hole `Long`-bitmask `BoardSpec` scheme. (Was "three new boards incl. Triangle-15"; Triangle-15 removed per D-17.)
- **Board rendering (square grid only)** — all five boards are square-grid; `BoardCanvas`/`BoardGeometry` only need their hardcoded English-33 grid dimension generalized to a per-board dimension (D-18). No triangular-lattice geometry. `onCellTap` must stay bounds-checked for every footprint.
- **Where `ScoreCalculator` lives and its exact constants** (`PEG_VALUE`, `TIME_BONUS_CAP`, `DECAY`, the 2★ band) — pure-Kotlin domain, JVM-unit-tested, per QUAL-01/QUAL-04.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product & domain design
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` — full approved design spec. Most relevant for this phase: the Classic boards and their definitions, scoring/stars model, the screen/navigation flow, the `LevelRepository`/data-layer seam, and onboarding. Cross-check the board footprints and the stars-from-pegs / time-score model against this spec.

### Project governance & standards
- `CLAUDE.md` (repo root) — mandatory tech stack + code standards: pinned versions (Kotlin 2.3.20, AGP 9.1.0, Compose BOM 2026.06.00, Hilt 2.60, KSP not KAPT, Navigation Compose 2.9.8, DataStore Preferences 1.2.1), no-magic-numbers/strings (scoped constants), all UI dp/sp in `res/values/dimens.xml`, detekt + ktlint as a build gate, strict Gitflow.
- `.planning/PROJECT.md` — Core Value, Key Decisions table (incl. "stars from pegs remaining; time feeds ranking score"), slicing strategy.
- `.planning/ROADMAP.md` — Phase 2 goal, requirements (CLAS-01/02, SCOR-01/02/03, NAV-01/02, ONB-01), and the 5 success criteria this phase is verified against.
- `.planning/REQUIREMENTS.md` — full text of CLAS-01/02, SCOR-01/02/03, NAV-01/02, ONB-01.

### Carried-forward phase context
- `.planning/phases/01-domain-foundation-playable-board/01-CONTEXT.md` — Phase 1 locked decisions still in force: D-01 (English-33 already built), D-02..D-04 (tap-only, always-two-step, deselect-by-retap interaction — unchanged), D-05 (`GameOverDialog` is the seam this phase's results screen replaces), D-07/D-08 (baseline jump/removal animation).

### UI implementation guidance
- `~/.claude/skills/compose-skill` — local Compose / UDF / state-hoisting practices the Compose layer must follow (stateless composables, state owned by ViewModels).
- Android app architecture guide — https://developer.android.com/topic/architecture (Clean Architecture / MVVM / Repository — the `LevelRepository` seam follows this).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`domain/.../model/BoardSpec.kt` + `Board.kt`** — the immutable `Long`-bitmask board with a stable `(row,col)`-sorted bit index, explicitly designed to carry all four Classic footprints unchanged (every board ≤ 64 holes). The 3 new boards are new `BoardSpec`/`Board` values, not new machinery.
- **`domain/.../model/Boards.kt`** — `English33` spec + `english33Initial` already exist as the pattern; European-37, Triangle-15, Diamond-41 follow the same scoped-layout-constants → derived-footprint shape.
- **`domain/.../rules/RulesEngine.kt`** — stateless move authority works for any `BoardSpec`; no change needed for new boards (orthogonal-only rules already general).
- **`app/.../ui/components/BoardCanvas.kt` + `BoardGeometry.kt`** — stateless Canvas board renderer reused both for gameplay on the new boards and for the catalog's mini board-shape previews (D-11). Triangle-15's non-square lattice is the one geometry generalization to design (discretion).
- **`app/.../ui/game/GameOverDialog.kt`** — the Phase-1 end-state dialog; the results screen (D-05) is its deliberate replacement at the same point in the flow.
- **Golden-master harness** (`domain/src/test/.../golden/`) — `BoardCodec` + `GoldenMasterAssert` over committed fixtures; reusable to lock the new boards' encodings and (later) the generator.

### Established Patterns
- **Route/Screen/Leaf + UDF**: `GameRoute` (hiltViewModel + `collectAsStateWithLifecycle`) → stateless `GameScreen` over an immutable `GameUiState` `StateFlow`. New screens (Home, Catalog, Results) follow the same stateless-composable + ViewModel-owned-state shape.
- **Hilt seam in `:app`** (`di/DomainModule.kt`): pure domain stays Android-free (QUAL-01); bindings are provided in `:app`. The `LevelRepository` and `ScoreCalculator` wiring belongs here / in a new data module-or-package following the same approach.
- **Scoped constants + `dimens.xml`** (QUAL-03), detekt/ktlint gate (QUAL-02), domain-purity boundary test (QUAL-01), JVM unit tests incl. golden-master (QUAL-04) — all already enforced; new domain (`ScoreCalculator`, board specs) must land under the same gates.

### Integration Points
- **`MainActivity` → `GameRoute()` is the current single-destination host.** Phase 2 inserts a NavHost here with five destinations; this is the main structural change.
- **`GameViewModel(initialBoard: Board)` + `DomainModule.provideInitialBoard()`** — today a hardcoded English-33 inject. This is the exact seam where the *selected* board (via `LevelRepository`) must flow in instead; the hardcoded provider is replaced/parameterized.
- **`GameUiState` / `GameStatus` (Playing/Won/Stuck)** — already expose pegs-remaining-derivable state and end status; the results screen and `ScoreCalculator` consume these (pegs remaining + elapsed time) at game end.

</code_context>

<specifics>
## Specific Ideas

- The results screen should feel like the natural payoff the Phase-1 `GameOverDialog` was a placeholder for — same point in the flow, now a full screen with the star-tiered headline (D-08) reusing the Phase-1 win/perfect copy tiers.
- Stars and score are intentionally **two independent axes** (D-01 vs D-03): a slow, perfect solve is still 3★ but a modest score; a fast, imperfect clear can out-score it. This is the explicit "avoid punishing slow-but-perfect with fewer stars" intent from PROJECT.md's Key Decisions.
- The catalog's board-shape previews (D-11) double as a free visual smoke test that each new `BoardSpec` footprint renders correctly.

</specifics>

<deferred>
## Deferred Ideas

- **Saved "best stars / best score" badges on catalog cards** — depends on per-level persistence (PERS-01); belongs in Phase 5. Catalog cards show static info only this phase.
- **Challenge mode itself** — only the disabled "Coming soon" entry + stubbed repository source here; real generation, difficulty curve, and the sequential level stream are Phase 3.
- **Full settings (sound/haptics) and the broader DataStore/Room store** — Phase 5. Only the single `onboardingSeen` flag is introduced now (D-16).
- **Interactive onboarding demo** (guided first move on a live mini board) — considered for ONB-01 but deferred in favor of the dismissable overlay (D-13); revisit only if playtests show the static overlay under-teaches.
- **Drag-to-jump input and the arc/pop animation polish pass** — already deferred from Phase 1 to Slice 2; unchanged.

</deferred>

---

*Phase: 2-classic-mode-scoring-navigation*
*Context gathered: 2026-06-28*
