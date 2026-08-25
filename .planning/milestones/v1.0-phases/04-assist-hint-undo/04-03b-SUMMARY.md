---
phase: 04-assist-hint-undo
plan: 03b
subsystem: ui
tags: [hint, compose, canvas, boardcanvas, material3, dimens, state-hoisting, tertiary]

# Dependency graph
requires:
  - phase: 04-assist-hint-undo
    provides: "04-03 GameUiState.hintSource/hintTarget/solverThinking + GameViewModel.onHintRequested (the hint state this plan renders)"
  - phase: 01-foundation
    provides: "Stateless BoardCanvas Canvas leaf + BoardGeometry radii + BoardMetrics ratio style + QUAL-03 dimens/strings gates"
provides:
  - "BoardCanvas winning-move highlight: tertiary source glow ring + target ring + faint A→B arrow, render-only (D-01), suppressed in read-only previews"
  - "HintControl — stateless top-bar action (Icons.Rounded.Search) with a disabled CircularProgressIndicator thinking state (D-10)"
  - "GameScreen/GameRoute wiring: HintControl in the actions row, onHint = GameViewModel::onHintRequested"
  - "D-02 rename: the legal-dots toggle now reads 'Show possible moves', unmistakably distinct from the Hint action"
affects: [04-04, 04-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Render-only board overlay: drawBoard reads hint fields and draws glow/ring/arrow, gated by `interactive && both positions set`, never mutating state or performing the jump (mirrors selection/dots suppression)"
    - "Second accent role (tertiary) reserved exclusively for the hint highlight so a hint never reads as a selection (primary)"
    - "Disabled-button thinking state: the content description sits on the disabled IconButton (not the decorative spinner) so the control announces as one disabled unit and blocks double-taps"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/HintControl.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/HintControlTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardGeometry.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/BoardMetrics.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/HintToggleControl.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "The hint arrow is drawn with drawLine (a straight A→B connector) rather than a full arrowhead Path — a faint line plus the two distinctly-sized tertiary rings communicates direction (D-01 'faint'); richer arrow animation stays deferred to Slice 2 (POLI-01)."
  - "The `hint_thinking` content description sits on the disabled IconButton, not on the CircularProgressIndicator, because the spinner's ProgressBarRangeInfo forms a separate merging semantics node — putting the description on the button makes the disabled unit locatable/testable and announces the whole control as one disabled affordance."
  - "BoardCanvasTest stays pure-JVM (no Robolectric): the highlight contract is verified through two new BoardGeometry radius accessors (hintSourceRingRadius > selectedRingRadius, hintTargetRingRadius > destinationDotRadius, both proportional to cellSize), matching the existing geometry-math approach rather than adding a screenshot golden."

patterns-established:
  - "Winning-move highlight overlay on BoardCanvas: tertiary rings + faint arrow, interactive-gated and render-only."
  - "Top-bar action with an inline disabled thinking spinner (HintControl), distinct from the persisted render-gate toggle (HintToggleControl)."

requirements-completed: [ASST-01, ASST-03]

# Metrics
duration: 25min
completed: 2026-07-08
---

# Phase 4 Plan 03b: Hint Rendering + Wiring Summary

**Turned the hint state 04-03 already writes into pixels: a `tertiary` source-glow + target-ring + faint A→B arrow on `BoardCanvas` (render-only, never auto-plays the jump), a `HintControl` top-bar action with a disabled `CircularProgressIndicator` thinking state, the `GameScreen`/`GameRoute` wiring to `onHintRequested`, and the D-02 "Show possible moves" rename that makes the dots toggle unmistakably distinct from the Hint action.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-07-08
- **Tasks:** 1
- **Files:** 11 (2 created, 9 modified)

## Accomplishments

- **Winning-move highlight (ASST-01, D-01):** `drawBoard` now reads `state.hintSource`/`state.hintTarget` and, only when `interactive && both are set`, draws (in order) the target ring (B), the larger source glow ring (A), and a faint `drawLine` A→B arrow — all in `MaterialTheme.colorScheme.tertiary` captured into `BoardPalette`. It is render-only: it never calls the jump handler (T-04-3B-RP) and is suppressed in the read-only catalog preview exactly like selection/dots.
- **Proportional geometry:** five documented `HINT_*` ratios added to `BoardMetrics` (source/target ring radii, arrow width, two alphas) and two radius accessors (`hintSourceRingRadius`/`hintTargetRingRadius`) added to `BoardGeometry`, mirroring `selectedRingRadius`/`destinationDotRadius`. No hardcoded dp/sp — visuals are unitless × cellSize plus the reused `peg_selected_ring_width` stroke.
- **HintControl (ASST-03, D-10):** a stateless `Icons.Rounded.Search` `IconButton` tinted `onSurface`; while `solverThinking` it swaps to a `top_bar_icon_size` `CircularProgressIndicator` and disables the button, blocking a double-tap from launching an overlapping Solver search (T-04-3B-DT).
- **Wiring:** `GameScreen` gained an `onHint` param, threads `state.solverThinking` + `onHint` through `GameTopBar`, and places `HintControl` in the actions row (timer · Undo · Hint · Show-possible-moves · Restart); `GameRoute` wires `onHint = viewModel::onHintRequested`.
- **D-02 rename:** `hints_on`/`hints_off` replaced by `show_moves_on`="Show possible moves" / `show_moves_off`="Hide possible moves"; `HintToggleControl` repointed and its KDoc clarified it is the dots render-gate, distinct from the new Hint action. New strings `hint_action`="Hint" and `hint_thinking`="Finding a move" added.
- **Tests:** `HintControlTest` (Robolectric + `createComposeRule`) proves the resting control is enabled and a tap invokes the handler, and that the thinking state is disabled with the "Finding a move" description while the resting Hint icon is gone. `BoardCanvasTest` extended with two pure-JVM assertions locking the hint-ring radii distinct from selection/dot and proportional to cellSize.

## Task Commits

1. **Task 1: Hint highlight rendering + HintControl + D-02 rename + wiring** — `fd08809` (feat)

**Plan metadata:** _(final docs commit)_

## Files Created/Modified

- `ui/game/HintControl.kt` (created) — stateless Hint action with the disabled thinking spinner.
- `ui/game/HintControlTest.kt` (created) — Robolectric Compose coverage of resting-clickable / thinking-disabled.
- `ui/components/BoardCanvas.kt` — `hint` (tertiary) palette color + `drawHintHighlight` overlay, interactive-gated, render-only.
- `ui/components/BoardGeometry.kt` — `hintSourceRingRadius` / `hintTargetRingRadius` accessors.
- `ui/game/BoardMetrics.kt` — five `HINT_*` proportional ratios.
- `ui/game/HintToggleControl.kt` — repointed to `show_moves_on`/`show_moves_off`; KDoc clarified (D-02).
- `ui/game/GameScreen.kt` — `onHint` param + `HintControl` in the actions row + `solverThinking` threading.
- `ui/game/GameRoute.kt` — `onHint = viewModel::onHintRequested`.
- `res/values/strings.xml` — `hint_action`, `hint_thinking` added; toggle strings renamed.
- `ui/components/BoardCanvasTest.kt` — hint-ring radius assertions.
- `ui/game/GameFlowTest.kt` — passes `onHint` to the now-updated `GameScreen` signature.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `GameFlowTest` needed the new `onHint` param**
- **Found during:** Task 1 (verification build)
- **Issue:** adding the required `onHint` param to `GameScreen` broke `GameFlowTest`'s call site (not in the plan's `files_modified`).
- **Fix:** passed `onHint = viewModel::onHintRequested` at the test call site (compile-only; no behavior change).
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt`
- **Commit:** `fd08809`

**2. [Rule 3 - Blocking] detekt `LongMethod` on `BoardCanvas` and `GameScreen`**
- **Found during:** Task 1 (detekt gate)
- **Issue:** the added `tertiary` palette line (BoardCanvas) and the `onHint` param + `HintControl` line (GameScreen) pushed each Compose function to 61 lines (limit 60).
- **Fix:** added `@Suppress("LongMethod")` with a one-line rationale to each — matching the 04-03 precedent for detekt method-size gates on cohesive UI hub functions; no threshold change to the shared config.
- **Files modified:** `BoardCanvas.kt`, `GameScreen.kt`
- **Commit:** `fd08809`

**3. [Rule 3 - Blocking] thinking-state semantics placement for testability + a11y**
- **Found during:** Task 1 (HintControlTest run)
- **Issue:** with `hint_thinking` on the `CircularProgressIndicator` (as the plan sketched), the spinner's `ProgressBarRangeInfo` forms a separate merging semantics node, so the disabled `IconButton` state and the content description landed on different nodes — `assertIsNotEnabled` could not locate a disabled node by description.
- **Fix:** moved the `hint_thinking` content description onto the disabled `IconButton` modifier (spinner left decorative). This is also better accessibility — the whole control announces as one disabled unit — and preserves the plan intent (spinner shown, disabled, announced).
- **Files modified:** `HintControl.kt`
- **Commit:** `fd08809`

**Total deviations:** 3 (all Rule 3 - blocking compile/lint/test-gate fixes; no change to plan intent).

## Threat Model Compliance

- **T-04-3B-RP (render-only):** `drawHintHighlight` reads `hintSource`/`hintTarget` and draws only; it never invokes a jump handler (D-01). The overlay is gated on `interactive`, so read-only previews cannot leak a hint or a tappable affordance — verified by the existing interactive-suppression path and the new geometry test.
- **T-04-3B-DT (double-tap):** while `solverThinking`, `HintControl` renders a disabled `IconButton`; `HintControlTest.thinkingShowsTheSpinnerAndIsDisabled` asserts the control is not enabled, so no overlapping Solver search can be launched from the UI (the VM-side cancel guard from 04-03 backs this up).

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*HintControlTest*' --tests '*BoardCanvasTest*' --tests '*GameFlowTest*' :app:ktlintCheck :app:detekt` — **BUILD SUCCESSFUL** (13 tests passed).
- No hardcoded dp/sp reached a composable: hint visuals are `BoardMetrics` ratios × cellSize plus the reused `top_bar_icon_size` dimen and `peg_selected_ring_width` stroke; all copy is in `strings.xml`.

## Notes on the 04-04 handoff

`noWinningMove` is written by the 04-03 VM but still not surfaced — the inline "no winning move — undo or restart" rescue banner (`NoWinBanner.kt`, D-06/D-08/D-09) ships in **04-04**. This plan intentionally renders only the positive hint (source/target/thinking); the no-win banner and stuck surface are the next slice.

## Self-Check: PASSED

- `ui/game/HintControl.kt` and `ui/game/HintControlTest.kt` exist on disk — verified.
- Commit `fd08809` present in `git log` — verified.
- `grep` acceptance: `hint_action`/`hint_thinking`/`show_moves_on`/`show_moves_off` present and `hints_on`/`hints_off` gone in `strings.xml`; `HintToggleControl` references `show_moves_on`; `HINT_SOURCE_RING_RATIO` in `BoardMetrics.kt`; `hintSource` + `hint` (tertiary) in `BoardCanvas.kt`; `onHint = viewModel::onHintRequested` in `GameRoute.kt`.
- Targeted tests + `:app` ktlint + detekt green.

---
*Phase: 04-assist-hint-undo*
*Completed: 2026-07-08*
