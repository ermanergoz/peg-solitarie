# android-game-dev report — reduced-motion-live-gate
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (7382351..47e7477, off develop 6aa7624)
Skills loaded: ergoz-soft:standards (plugin 1.0.0 rule files), compose-skill, superpowers:test-driven-development
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: returned as text; condensed by the producer with every check and issue kept.

## Summary
- **Live gate** (`7382351`): `rememberAnimationsEnabled()` reads `rememberCoroutineScope().coroutineContext[MotionDurationScale]`; signature unchanged, so all **14 call sites in 9 files** follow the live scale, incl. `HintUndecidedNotice.kt:170`. Our own `Settings.Global` read is gone.
- **F-1** (`335bc3b`): Select, Deselect, Undo, InvalidTap cues go through `NewOrdinalEffect`.
- **Riders:** `35cfd35` one test-input block in `app/build.gradle.kts` (auditor finding 1); `47e7477` Settings KDoc (F-2).
- **Escalation:** §3(c)'s premise fails under Compose's test rule (open issue 1).

## Per-commit
| Commit | Red | Green |
|---|---|---|
| `7382351` | 6/6 new tests red (board "dashes in the tap's frame expected 12 but was 0" = QA's FAIL; tutorial 1,030 changed px; Results "star pixels at least 300 but was 0"; gate never left "on"); v2 rule → 4/4 red again | 6 new green; six §3(c) files unedited 43/43; `GameScreenHintNoticeTest` unedited 19/20 → setup change 20/20 |
| `335bc3b` | `GameScreenCueRotationTest` 4/4 red ("expected empty but was [InvalidTap]" etc.) | 4/4; `ui.game.*` + `ui.onboarding.*` 36 suites, 306 tests, 0 failures |
| `35cfd35` | build config | UP-TO-DATE unedited; `CREDITS.md` edit re-runs via `audioProvenance`; `ui/onboarding` edit via `tutorialSources` (probes reverted) |
| `47e7477` | KDoc | the Check |
New tests: `ui/theme/AnimationsEnabledGateTest.kt`, `AnimationsEnabledGateWithoutScaleTest.kt`, `ui/components/BoardCanvasWrongTapLiveMotionTest.kt`, `ui/results/ResultsStarsLiveMotionTest.kt`, `ui/game/GameScreenCueRotationTest.kt`; helper `testing/SystemMotionScale.kt`; `WrongTapBoards.kt` scale now mutable. `GameScreenHintNoticeTest.kt`: setup only (`effectContext = motion`, `motion.turnAnimationsOff()`), assertion untouched.

## Checks
- Compose 1.11.3 sources: `MotionDurationScaleImpl` is snapshot-backed and observes `ANIMATOR_DURATION_SCALE` (`WindowRecomposer.android.kt:94-130, 423-460`); a provided element wins (`:333-337`); the test rule composes on its own Recomposer (`ComposeUiTest.android.kt:591-602, 763`), so a JVM test has no scale element unless `effectContext` provides one.
- Probe (temporary `check(gate on)`, never committed): over the six named files 43 tests, 6 failed — all in `BoardCanvasWrongTapReducedMotionTest`, the one file that provides the element; 0 hits in the other 37.
- `./scripts/check.sh --max-workers=4` — pass, 11m 24s: `:app` 1,224 (+10), `:domain` 294, depthSweep 1, fuzz 1, build-logic 55 = 1,575, 0 failures; lint 0 errors, 64 warnings (identical to develop's audio tree); 0 `w:` in main (C7).
- `git grep 'rememberAnimationsEnabled()' -- app/src/main` → 14 in 9 files. Trailers reworded to the single Fable line (`git commit-tree`, trees identical).

## Open issues / risks
1. **ESCALATION (tech-lead):** under the ruled gate a JVM `Settings.Global` write never reaches the composition's scale. (a) The notice test cannot stay green unedited (setup changed, assertion kept). (b) Six named files stay green unedited but 7 tests no longer reach the reduced-motion path, incl. the 7c-race guards: `GameScreenSoundTest` ×3 (`aJumpStillLayersTheRemovalCue…`, `theJumpPitchStillClimbs…`, `aJumpReplayedAfterAnUndo…`), `GameFlowTest.aJumpStillFiresTheHaptic…`, `ResultsCelebrationSoundTest.withOsAnimationsOffThePops…`, `TutorialHapticsTest.aCompletedTutorialJumpStillFires…`, `TutorialSoundTest.aCompletedJumpStillLayers…`. Options: **(A, recommended)** drive reduced motion in those five files through `SystemMotionScale` in the rule's `effectContext` (assertions unchanged); **(B)** fall back to reading `Settings` when no scale element is present (keeps files unedited, re-adds our own read, contradicts §3's "absent → enabled").
2. No git hooks installed in this repo (only `.sample`, no `core.hooksPath`); trailers were reworded by hand.
3. A live toggle while the Undecided notice is up swaps `AnimatedVisibility` ↔ plain branch, recreating the node, so the polite region may be spoken again (QA, ux-designer).
4. Stale text outside the ruled files: "No audio engine yet" in `SettingsUiState.kt:29-31`, `data/SettingsRepository.kt:18`; `GameScreenSoundTest`'s KDoc cites `> 0` guards.
5. No 15e sitting: Home's first composition now makes the window's first read of the scale (Compose's own cached flow; no SDK call of ours). QA's device run confirms.

## Handoff
tech-lead: rule open issue 1 (A or B). code-auditor: `6aa7624..47e7477`. qa-engineer: §3's device check (English-33; Remove animations on → mark within 450 ms; off → shake; WT-15, P9), Results stars and tutorial redirect after a live toggle, open issue 3.
