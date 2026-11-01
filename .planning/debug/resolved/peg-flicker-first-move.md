---
status: resolved
trigger: "When making the first move in the game, the peg flickers in the top-left part of the screen (likely an animation starting from an unset/zero offset before the real board coordinates are applied)"
created: 2026-08-27
updated: 2026-08-27
resolved: 2026-08-27
---

## Symptoms

DATA_START
- expected: On a jump move, the moving peg animates smoothly from its source hole to the destination hole on the board.
- actual: On the FIRST move of a game, the jumping peg briefly flickers/renders at the top-left part of the screen before/while the move animation plays. Subsequent moves animate correctly.
- errors: None reported (visual glitch only).
- timeline: Has always done this since the jump animation was added (not a recent regression).
- reproduction: Launch a game (user tested one mode only; other mode untested), tap a peg and make the first jump move. Flicker appears at top-left.
- user_note: User suspects an animation starting from an unset/zero Offset (0,0 = top-left) before the real board coordinates are applied.
DATA_END

## Current Focus

hypothesis: CONFIRMED — the draw phase renders the in-flight jumper the moment `state.animating != null`, but the jumper's position comes from an `Animatable` that is only snapped to the source-hole center inside a `LaunchedEffect`. The effect body is dispatched at least one frame AFTER the composition+draw that first observed `animating`, so the first drawn frame uses the Animatable's stale value — `Offset.Zero` (canvas top-left) on the very first move of a game.
test: Trace the write of `animating` in GameViewModel vs. the read in BoardCanvas.drawBoard, and the initialization/snapTo ordering of `jumpOffset`.
expecting: An `Animatable(Offset.Zero)` drawn before any snapTo positions it.
next_action: none — fix applied, human-verified on device, session resolved.

reasoning_checkpoint:
  hypothesis: "BoardCanvas draws the in-flight jumper at `jumpOffset.value` whenever `state.animating != null`, but `jumpOffset` is only positioned by a LaunchedEffect that runs after that frame's draw; on the first move the Animatable still holds its initial Offset.Zero, so a peg is drawn at canvas (0,0) — the top-left — for one frame."
  confirming_evidence:
    - "BoardCanvas.kt:115 — `val jumpOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }`; Offset.Zero is the canvas top-left and is never a valid cell center (BoardGeometry.center always returns originX/Y + 0.5*cellSize > 0)."
    - "BoardCanvas.kt:312-326 — drawBoard draws the jumper unconditionally whenever `transition != null`, reading `animations.jumpOffset()`; the draw is gated ONLY on state, never on whether the animation has been positioned."
    - "BoardCanvas.kt:120-133 + 208 — the only thing that positions the Animatable is `jumpOffset.snapTo(geometry.center(transition.from))` inside `runJumpAnimation`, called from a LaunchedEffect. LaunchedEffect bodies are dispatched on AndroidUiDispatcher and resume after the current Choreographer doFrame (which includes the TRAVERSAL/draw phase) completes — so the snapTo lands a frame after the draw that first saw the transition."
    - "GameViewModel.kt:531-548 — `board = nextBoard` and `animating = transition` are written in a SINGLE atomic `_uiState.update`, so the very first recomposition that shows the post-move board also already satisfies the `transition != null` draw condition."
    - "Explains first-move-only: from move 2 onward the Animatable retains the previous move's destination center, which is a real board cell where a peg legitimately sits, so the identical one-frame glitch is invisible."
  falsification_test: "If the flicker were NOT caused by the unpositioned Animatable, drawing the jumper at `geometry.center(transition.from)` until the effect snaps it would leave the top-left flicker unchanged. Also: if the effect ran before the draw, jumpOffset would already equal center(from) on the first drawn frame and no flicker could occur."
  fix_rationale: "The root cause is that the jumper's DRAW condition (`animating != null`) and its POSITION SOURCE (an Animatable positioned asynchronously) are not synchronized. The fix makes the position total: until the effect confirms it has snapped the Animatable for this exact move id, the draw falls back to the source-hole center computed synchronously from geometry. That removes the unsynchronized window entirely rather than masking Offset.Zero (e.g. by seeding a different initial value, which would only relocate the ghost peg)."
  blind_spots: "Frame-ordering of LaunchedEffect dispatch is established from Compose's dispatcher design plus the first-move-only symptom, not from an on-device trace. The move id is `moveHistory.size`, which RECYCLES after undo — so the prepared-id marker must be invalidated whenever the transition key changes, or an undo-then-replay would reintroduce a stale-position ghost frame. User tested one mode only; the code path is mode-independent (single shared BoardCanvas)."

## Evidence

- checked: BoardCanvas.kt:115 — jumper Animatable initialization
  found: `val jumpOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }` — initial value is the canvas origin (top-left).
  implication: Any draw occurring before the first snapTo renders the peg at the top-left. Matches the reported symptom location exactly.

- checked: BoardGeometry.kt:62-66 — center() math
  found: `center = origin + (index + 0.5) * cellSize` — strictly positive for every in-play position; Offset.Zero can never be a legitimate cell center.
  implication: A peg drawn at Offset.Zero is unambiguously an uninitialized-animation artifact, not a real board cell.

- checked: BoardCanvas.kt:312-326 — drawBoard jumper overlay
  found: The overlay peg is drawn whenever `transition != null`, positioned solely by `animations.jumpOffset()`. There is no guard tying the draw to the animation actually having been positioned.
  implication: Draw condition and position source are decoupled — this is the actual defect.

- checked: BoardCanvas.kt:120-133, 191-228 — when the Animatable is positioned
  found: `jumpOffset.snapTo(geometry.center(transition.from))` happens only inside `runJumpAnimation`, invoked from `LaunchedEffect(transition?.id, geometry, interactive)`.
  implication: Positioning is asynchronous relative to the composition that first exposes the transition; the effect coroutine resumes after the current frame's draw.

- checked: GameViewModel.kt:505-548 — performJump state emission
  found: `board = nextBoard` and `animating = transition` are set in one atomic `_uiState.update`.
  implication: There is no intermediate state where the board has moved but `animating` is still null; the first post-move draw already tries to render the jumper.

- checked: Why only the first move flickers
  found: The Animatable is remembered across moves, so after move 1 it retains the previous move's destination center — a real cell that already shows a peg in the post-move board.
  implication: The same one-frame desync happens on EVERY move; it is only visually detectable on move 1, where the stale value is the top-left corner. Confirms first-move-only without needing a separate mechanism.

- checked: GameViewModel.kt:528 — MoveTransition id source
  found: `id = moveHistory.size`, which decreases on undo and therefore recycles ids.
  implication: The fix cannot rely on "prepared id != current id" alone; the marker must be reset whenever the transition key changes, or undo-then-replay reintroduces a stale-position frame.

- checked: On-device verification after the fix (2026-08-27)
  found: User ran the debug build and confirmed the top-left flicker no longer occurs on the first move ("cool all good now"). Reported via the debug orchestrator, not observed directly by the agent.
  implication: Closes the one verification gap the JVM tests could not cover (sub-frame visual artifact, no screenshot infrastructure in the project).

## Eliminated

- hypothesis: The flicker is a board-geometry/measurement problem (animation starting before the canvas is measured, cellSize == 0).
  evidence: Both the LaunchedEffect (`geometry.isReady` guard, BoardCanvas.kt:121) and the draw phase (`if (!geometry.isReady) return`, BoardCanvas.kt:283) short-circuit on an unmeasured canvas, and the board is fully rendered and tappable before any move can be made. Geometry is always ready by the first move.

- hypothesis: The ViewModel briefly emits a state where the board has advanced but the transition coordinates are absent/default.
  evidence: GameViewModel.kt:531-548 sets board and animating in a single atomic `_uiState.update`; MoveTransition is constructed with real `move.from/to/over` positions (line 523-529). No default-valued or partially-populated transition is ever emitted.

## Resolution

root_cause: In BoardCanvas, the in-flight jumper is drawn whenever `state.animating != null`, but its position is read from a `remember`ed `Animatable(Offset.Zero)` that is only moved to the source hole by `jumpOffset.snapTo(geometry.center(transition.from))` inside a `LaunchedEffect`. The effect coroutine resumes a frame after the composition/draw that first observes `animating`, so the first rendered frame of a jump uses the Animatable's stale value. On the first move of a game that stale value is the initial `Offset.Zero` — the canvas top-left — producing the flicker. On later moves the stale value is the previous move's destination (a real cell), which hides the identical glitch.
fix: Synchronized the jumper overlay's draw condition with its position source. Added a `preparedMoveId` marker that the animation effect sets the instant it snaps the Animatables onto this move's board coordinates, and reset at the top of the `LaunchedEffect` (the move id is `moveHistory.size` and recycles after an undo). Until a move is prepared, the draw phase positions the overlay itself via the new pure `jumperCenter()` — the source hole the peg is about to leave, i.e. exactly where it was a frame earlier — instead of the stale Animatable value. Applied the identical guard to the removal channel via `removalAlpha()`: the doomed peg's fade Animatable rests at `REMOVAL_END` (fully transparent) after each move, so from move 2 onward the jumped-over peg was blinking out and back in for one frame before its fade began. Both decisions were extracted as pure internal functions so they are covered by fast JVM tests rather than being buried in the composable.
verification: |
  - Root cause reproduced in test form: reverting `jumperCenter` to the old behavior (`animatedOffset` passthrough) fails `jumperRendersAtItsSourceHoleBeforeTheAnimationIsPrepared` and `aRecycledMoveIdIsNotTreatedAsPrepared`; reverting `removalAlpha` fails `doomedPegIsWholeBeforeTheRemovalFadeIsPrepared`. Restoring the fix turns all three green — the tests genuinely bind the defect, they are not tautological.
  - 6 new regression tests in BoardCanvasTest cover: unprepared first move (must not use Offset.Zero), prepared move follows the animation, recycled move id after undo, no-transition passthrough, removal blink, prepared removal fade.
  - Full `:app:testDebugUnitTest` suite green, including the Robolectric Compose `GameFlowTest` that drives a real tap-to-jump through `BoardCanvas` — no regression in selection, jump, undo, hint or stuck-banner behavior.
  - `:domain:test` green (untouched, confirms no domain coupling).
  - `detekt` + `ktlintCheck` green (CLAUDE.md build gate); `:app:assembleDebug` green.
  - ON-DEVICE (2026-08-27): user installed the debug build, made the first move and confirmed the top-left flicker is gone ("cool all good now"). This confirmation was relayed through the debug orchestrator rather than observed by the agent. It covers the mode the user exercised; the other mode shares the same BoardCanvas code path and was not separately re-checked.
files_changed:
  - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt: added `preparedMoveId` state + `setPrepared` callback; jumper position and removal alpha now routed through the new pure `jumperCenter()` / `removalAlpha()` guards.
  - app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasTest.kt: 6 regression tests for the two guards.
