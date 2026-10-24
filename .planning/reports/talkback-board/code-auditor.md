# code-auditor report — talkback-board
Date: 2026-10-24
Branch: feature/talkback-board (1b7cda2..92b0c31; reviewed `git diff 6aa7624...92b0c31`; B0 `c690e55`)
Skills loaded: ergoz-soft:standards (plugin 1.0.0), code-reviewer, security-reviewer, compose-skill, compose-performance-audit, android-accessibility
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh — staffing deviation: pinned fable; Fable limit; Q-019; author also Opus.
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: returned as text; condensed by the producer with every finding, probe and check kept.

## Summary
Architecture sound (one stop per cell over a pointer-less overlay, pure speech model, one polite channel on the Canvas node; read-only path is today's Canvas; no suppression added). Check green, forced re-run green. Two Major defects shown by JVM probes; three Minor. **VERDICT: FAIL.**

## Findings
1. **Major — `ui/results/ResultsScreen.kt:397-410`: the Results summary is spoken again after a configuration change.** D-TBa item 7 / TB-8 say not on a rotation. `LaunchedEffect(state.playCelebration)` fires on first composition whenever the latch is true; nothing lowers it (only write `ResultsViewModel.kt:530`; no `configChanges`), and a rotation keeps the `hiltViewModel()`. `ResultsUiState.kt:38-43`'s KDoc is inaccurate (F-TB-6 inherited it). Probe P3 (compose, dispose, re-compose over the same state): the line is spoken again from frame 1. Fix must survive recreation (saveable "spoken" flag, or the ViewModel consuming the latch); test re-composes over a retained `true`.
2. **Major — `TutorialAnnouncements.kt:80`, `GameScreen.kt:295`/`:299`, `BoardCellOverlay.kt:77`: a "Moved" focus row is a no-op when the target already has input focus** (no `Focused` change → no `TYPE_VIEW_FOCUSED` → TalkBack does not move). Probe P2: with step 2 played by double-taps, the board stop stays `Focused=true` into step 3, so step 3's move does nothing (fails TB-7/TB-9 on the common path). Probe P1: jump by action, undo, same action again — no move. Same for a second banner rescue by double-taps. Would read on the device as "TalkBack does not follow input focus" (device question 1) and wrongly trigger the F-TB-1 fallback. Fix must guarantee a change (e.g. clear focus before each request); assert a false→true transition for step 3 after a double-tap step 2, a second banner Undo, a retried jump action.
3. **Minor — `GameScreen.kt:293-300`:** the banner's Undo/Restart move focus even when the banner does not leave (Undo can open the ad opt-in, `GameViewModel.kt:1225`; Restart on a no-winning-move board opens the confirm). Move focus only when the action happened, or the device pass rules.
4. **Minor — `BoardCanvas.kt:103`:** the existing `@Suppress("LongMethod")` now covers the new ~35-line interactive branch, its reason unchanged; amend the comment or move the branch into `BoardCellOverlay.kt`.
5. **Minor — `ResultsScreen.kt:413-415`:** a blank line detaches `headlineResFor`'s KDoc.
- Info: `GameScreen` recomposes at 1 Hz (`elapsedSeconds`, `:221`), building `boardStopLabel`, a `SelectionView` and a `Pair` per tick — negligible.
APPROVED SUPPRESSION: none (the `UNCHECKED_CAST` added in `be7fbf3` was removed in `92b0c31`).

## Deviations 1–10
All accepted (6: the five `requestFocus()` guards read and present; M3abc survives as expected because four targets' `focusable` is already gated; the fifth, "Got it", is caught by `TutorialScreenTest.theCompletionButtonClearsTheNavigationBar`). The three edited tests approved as D-TBa items 2–3 retargets; `TutorialSourceGuardTest` keeps all 11 tokens.

## Verified
Speech-model purity (no android imports; TB-12's five inputs; ArchUnit guard); overlay semantics (row-major `traversalIndex`, no role/selected/state description, jump actions only as ruled, sorted up/down/left/right, board stop's unlabelled consumed `onClick` and traversal group, no pointer input); one polite region on the Canvas node with generation-guarded clear and repeat mark; gameplay traversal group with slab at `-1f`; tripwire 1 empty (no 15e).
Mutations (throwaway worktree, removed): M1 swap action labels killed (4); M2 cap jump count killed (3); M3abc remove focus guards survived (expected); M3d ungate `focusable` killed (2); M4 live region on the Box killed (14); M5 coach before rejected line killed.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain` at `4431b46` — pass, 11m 7s.
- Same with `:app:testDebugUnitTest --rerun :domain:test --rerun` — pass, 11m 26s: `:app` 1,291 (125 classes), `:domain` 294, 0 failures.

## Open issues / risks
- Results' live region sits on the content Column (`ResultsScreen.kt:133`), whose subtree holds the ~600 ms count-up — QA listens for a repeat or a whole-column read; if heard, move to a leaf.
- All four focus moves request focus before the line is set (brief's mechanism; device question 2).
- Pre-existing, outside the diff: `StarPopSounds` (`ResultsCelebration.kt:107`) replays its sounds on a configuration change (F-1's reason) — to route.

## Handoff
android-game-dev (fix mode): F-1, F-2 must; F-3–F-5 same loop; then a fresh re-review. QA's device pass waits for F-2's fix.

VERDICT: FAIL
