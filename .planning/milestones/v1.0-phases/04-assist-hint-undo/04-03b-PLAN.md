---
phase: 04-assist-hint-undo
plan: 03b
type: execute
wave: 3
depends_on: [04-03]
files_modified:
  - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/BoardMetrics.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/HintControl.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/HintToggleControl.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/ui/game/HintControlTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt
autonomous: true
requirements: [ASST-01, ASST-03]
must_haves:
  truths:
    - "The hint highlights BOTH the source peg (glow) and the target hole (ring) with a faint A->B arrow — it never auto-performs the jump (D-01)"
    - "While the Solver computes, the Hint control shows a thinking/disabled state and cannot be double-tapped (D-10)"
    - "The renamed 'Show possible moves' toggle is unmistakably distinct from the new 'Hint' action (D-02)"
    - "The Hint control sits in the game top bar actions row and requests a hint via the wired VM handler"
  artifacts:
    - path: "app/src/main/java/com/app/pegsolitaire/ui/game/HintControl.kt"
      provides: "Hint action control with thinking spinner"
      contains: "fun HintControl"
    - path: "app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt"
      provides: "source glow + target ring + A->B arrow overlay when interactive"
      contains: "hintSource"
  key_links:
    - from: "app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt"
      to: "hintSource/hintTarget"
      via: "drawBoard renders tertiary source glow + target ring + A->B arrow"
      pattern: "hintSource|hintTarget"
    - from: "app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt"
      to: "GameViewModel.onHintRequested"
      via: "GameScreen onHint = viewModel::onHintRequested"
      pattern: "onHint = viewModel::onHintRequested"
---

<objective>
## Phase Goal (MVP user story)

**As a** peg-solitaire player, **I want to** ask for a hint that reveals a genuine next winning move and undo my last move, **so that** I can get unstuck and keep playing a puzzle that stays fair and always winnable.

## This plan's slice

Render + wire the **Hint** UI on top of the hint logic delivered in 04-03. This plan reads the `GameUiState` fields the 04-03 ViewModel already sets — `hintSource`, `hintTarget`, `solverThinking` — and turns them into pixels: a `BoardCanvas` highlight (source-peg glow + target-hole ring + faint A->B arrow, in the tertiary accent), the `HintControl` button with its D-10 thinking/disabled spinner, and the `GameScreen`/`GameRoute` wiring that calls `onHintRequested`. It also performs the D-02 rename of the existing legal-dots toggle to "Show possible moves" so the two affordances are unmistakably distinct. The highlight is render-only: it never auto-performs the jump (D-01) — the player still makes the move.

This is the second half of the hint feature; 04-03 ships the decision + orchestration, this plan ships the reveal + control the player sees.

Purpose: ASST-01 (the player SEES the next winning move) + ASST-03 (the thinking indicator during Solver fallback) + D-01/D-02/D-10 presentation.
Output: `BoardCanvas` highlight rendering, `HintControl`, the D-02 rename, `GameScreen`/`GameRoute` wiring, and Compose tests.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/04-assist-hint-undo/04-CONTEXT.md
@.planning/phases/04-assist-hint-undo/04-RESEARCH.md
@.planning/phases/04-assist-hint-undo/04-PATTERNS.md
@.planning/phases/04-assist-hint-undo/04-UI-SPEC.md
@.planning/phases/04-assist-hint-undo/04-01-SUMMARY.md
@.planning/phases/04-assist-hint-undo/04-03-SUMMARY.md

<interfaces>
<!-- Contracts from 04-03 (hint logic) and the codebase the executor consumes directly. -->

GameUiState (app/.../ui/game/GameUiState.kt — from 04-03): now carries
  val hintSource: Position? = null, val hintTarget: Position? = null,
  val solverThinking: Boolean = false, val noWinningMove: Boolean = false.
  This plan RENDERS these; it does not set them.

GameViewModel (app/.../ui/game/GameViewModel.kt — from 04-03): exposes `fun onHintRequested()`.
  GameRoute wires it as `onHint = viewModel::onHintRequested`.

BoardCanvas (app/.../ui/components/BoardCanvas.kt): private class BoardPalette; DrawScope.drawBoard/drawCell; drawSelectedPeg ring pattern (lines 259-275); destination-dot pattern (222-228); interactive=false suppresses overlays; geometry.center helpers.
BoardMetrics (app/.../ui/game/BoardMetrics.kt): scoped unitless ratio consts.
RestartControl / HintToggleControl: leaf-control analogs; GameScreen.LoadingIndicator uses CircularProgressIndicator (line 242-247).
HintToggleControl.kt: currently uses R.string.hints_on / R.string.hints_off (lines 38-39) — RENAME target.
strings.xml lines 78-79: hints_on="Hints on", hints_off="Hints off" — repoint to show_moves_on/show_moves_off.
GameScreen (app/.../ui/game/GameScreen.kt): actions row + GameTopBar; CircularProgressIndicator import already present.
GameRoute (app/.../ui/game/GameRoute.kt): VM handler wiring pattern.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Hint highlight rendering + HintControl + D-02 rename + wiring</name>
  <files>app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt, app/src/main/java/com/app/pegsolitaire/ui/game/BoardMetrics.kt, app/src/main/java/com/app/pegsolitaire/ui/game/HintControl.kt, app/src/main/java/com/app/pegsolitaire/ui/game/HintToggleControl.kt, app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt, app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt, app/src/main/res/values/strings.xml, app/src/test/java/com/app/pegsolitaire/ui/game/HintControlTest.kt, app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt</files>
  <read_first>
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt (WHOLE — BoardPalette, drawBoard/drawCell, drawSelectedPeg ring, destination-dot, interactive suppression, geometry.center)
    - app/src/main/java/com/app/pegsolitaire/ui/game/BoardMetrics.kt (WHOLE — ratio const style to extend)
    - app/src/main/java/com/app/pegsolitaire/ui/game/RestartControl.kt + HintToggleControl.kt (leaf pattern + string rename target)
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt (WHOLE — actions row, GameTopBar, CircularProgressIndicator import already present)
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt (WHOLE — VM handler wiring)
    - app/src/main/res/values/strings.xml (lines 70-93 gameplay block; hints_on/off lines 78-79 to rename)
    - .planning/phases/04-assist-hint-undo/04-UI-SPEC.md §"1. Hint control", §"3. Winning-move highlight", §"5. D-02 rename", §Color (tertiary), §Copywriting
    - app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt (Compose test analog)
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt (the hint/thinking fields from 04-03 to render)
  </read_first>
  <action>
    strings.xml: add `hint_action`="Hint", `hint_thinking`="Finding a move". Rename `hints_on`/`hints_off` -> `show_moves_on`="Show possible moves", `show_moves_off`="Hide possible moves" and repoint `HintToggleControl.kt` (lines 38-39) to the new keys. (Icon `Icons.Rounded.Info` unchanged; it stays the legal-dots render gate.)
    BoardMetrics.kt: add documented ratios `HINT_SOURCE_RING_RATIO = 0.42f`, `HINT_TARGET_RING_RATIO = 0.30f`, `HINT_ARROW_WIDTH_RATIO = 0.06f`, `HINT_HIGHLIGHT_ALPHA = 0.9f`, `HINT_ARROW_ALPHA = 0.5f` (unitless × cellSize; not dp).
    BoardCanvas.kt: add `hint = MaterialTheme.colorScheme.tertiary` to `BoardPalette` (constructor + capture). In `drawBoard`, after cells and before/around the animation overlay, when `interactive && state.hintSource != null && state.hintTarget != null` draw, in order: the target ring (B) at `geometry.center(hintTarget)` radius `pegRadius*HINT_TARGET_RING_RATIO`-derived using a `Stroke(ringWidthPx)` in `palette.hint.copy(alpha=HINT_HIGHLIGHT_ALPHA)`; the source glow ring (A) similarly at `hintSource` using `HINT_SOURCE_RING_RATIO`; a faint `Path` line/arrow from `geometry.center(hintSource)` to `geometry.center(hintTarget)` stroked with width `HINT_ARROW_WIDTH_RATIO*cellSize` in `palette.hint.copy(alpha=HINT_ARROW_ALPHA)`. Suppress entirely when `interactive=false` (like selection/dots). Reuse `geometry` radii helpers; add any needed radius accessor to BoardGeometry mirroring `selectedRingRadius`/`destinationDotRadius`. Do NOT auto-perform any jump (render-only, D-01).
    Create `HintControl.kt`: stateless `@Composable fun HintControl(solverThinking: Boolean, onHint: () -> Unit, modifier: Modifier = Modifier)` mirroring RestartControl. Resting: `IconButton(onClick = onHint)` with `Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.hint_action), tint = MaterialTheme.colorScheme.onSurface, size = top_bar_icon_size)`. While `solverThinking`: `IconButton(onClick = onHint, enabled = false)` containing `CircularProgressIndicator(Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)).semantics { contentDescription = <hint_thinking> })`. (Icons.Rounded.Search is core-set.)
    GameScreen.kt: add `onHint: () -> Unit` param; forward `state.solverThinking` + `onHint` into `GameTopBar`; add `HintControl(solverThinking = solverThinking, onHint = onHint)` to the actions row (order: timer, Undo, Hint, Show-possible-moves toggle, Restart). Update KDoc.
    GameRoute.kt: pass `onHint = viewModel::onHintRequested`.
    Tests: create `HintControlTest.kt` (Robolectric+createComposeRule): resting shows the `Hint` content description and clicking invokes the handler; when `solverThinking=true` the button is disabled and the `Finding a move` (thinking) node is present. Extend `BoardCanvasTest.kt` with a semantics/snapshot assertion that a state carrying `hintSource`/`hintTarget` renders the highlight (assert via a testable signal, e.g. a Roborazzi golden of the highlighted board, or a semantics/testTag if BoardCanvas exposes one — follow the existing BoardCanvasTest approach).
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests '*HintControlTest*' --tests '*BoardCanvasTest*' :app:ktlintCheck :app:detekt</automated>
  </verify>
  <acceptance_criteria>
    - strings.xml has `hint_action`, `hint_thinking`, `show_moves_on`, `show_moves_off`; `hints_on`/`hints_off` are gone and `HintToggleControl` references the new keys (`grep -q "show_moves_on" app/.../HintToggleControl.kt`).
    - BoardMetrics has the five `HINT_*` ratios; `grep -q "HINT_SOURCE_RING_RATIO" app/.../BoardMetrics.kt`.
    - BoardCanvas `BoardPalette` has a `hint` (tertiary) color and draws source ring + target ring + arrow only when interactive and both hint positions are set; `grep -q "hintSource" app/.../BoardCanvas.kt`.
    - `HintControl.kt` exists using `Icons.Rounded.Search` + `CircularProgressIndicator` thinking state; `HintControlTest` proves resting-clickable and thinking-disabled.
    - GameScreen renders HintControl in the actions row; GameRoute wires `onHint = viewModel::onHintRequested`.
    - No hardcoded dp/sp for hint visuals (ratios only); dpSpLiteralCheck + detekt + ktlint green.
  </acceptance_criteria>
  <done>Tapping Hint highlights the winning move (source glow + target ring + faint arrow) without auto-playing it, shows a thinking state while the Solver runs, and the dots toggle now reads "Show possible moves" — unmistakably distinct from Hint.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| GameUiState hint fields → BoardCanvas render | The canvas draws hintSource/hintTarget read-only; it must never mutate game state or perform the jump |
| Hint control tap → ViewModel | A tap only invokes `onHintRequested`; the disabled thinking state prevents double-taps |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-04-3B-RP | Tampering (correctness) | BoardCanvas hint overlay | mitigate | The highlight is render-only — `drawBoard` reads `hintSource`/`hintTarget` and draws a glow/ring/arrow but never calls the jump handler (D-01); the player performs the move. Overlay is suppressed when `interactive=false` (mirrors selection/dots), so non-interactive renders cannot leak a hint or a tappable affordance. |
| T-04-3B-DT | Denial of Service | HintControl double-tap | mitigate | While `solverThinking` the control is `enabled = false` (the UI half of D-10) so the player cannot launch overlapping Solver jobs; the VM-side cancel guard (T-04-SD) in 04-03 backs this up. |

No package installs; no network/auth/crypto/persistence. UI-only slice.
</threat_model>

<verification>
- `./gradlew :app:testDebugUnitTest :app:ktlintCheck :app:detekt` green.
- The hint the player sees (source glow + target ring + arrow) is driven entirely by the `GameUiState` fields 04-03 sets; the `noWinningMove` banner still ships in 04-04.
</verification>

<success_criteria>
- ASST-01 (reveal half): tapping Hint highlights the next winning move (source glow + target ring + faint arrow); the player performs it (no auto-play, D-01).
- ASST-03 (feedback half): a thinking/disabled indicator shows while the Solver runs on Classic / post-deviation hints (D-10).
- D-02 satisfied: the dots toggle is renamed "Show possible moves", distinct from the new "Hint" action.
</success_criteria>

<output>
Create `.planning/phases/04-assist-hint-undo/04-03b-SUMMARY.md` when done.
</output>
</content>
