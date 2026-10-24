# android-game-dev report — talkback-board
Date: 2026-10-24
Branch: feature/talkback-board (c690e55..92b0c31)
Skills loaded: ergoz-soft:standards (plugin 1.0.0 rule files), compose-skill, compose-ui, android-accessibility, superpowers:test-driven-development, gitflow, android-testing, android-viewmodel, android-coroutines
Ran in parallel with: none dispatched (builds in PegSolitarie-home, PegSolitarie-lic shared the host)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: returned as text; condensed by the producer with every check, deviation and QA sequence kept.

## Summary
- B0 merged develop `6aa7624` with no conflicts (Check green, 11m 32s). B1–B6 test-first, each `:app:check` green; B7 adds the four dated D-TBa pointers; `92b0c31` removes a `@Suppress` copied into a test.
- Final `./scripts/check.sh --max-workers=4 --console=plain` at `92b0c31` — pass, 11m 8s: 1,642 tests (`:app` 1,291 in 125 classes, `:domain` 294, build-logic 55, depthSweep 1, fuzz 1), 0 failures/skips; koverVerify passed; lint 0 errors, 64 warnings (as develop); 0 `w:` (C7), `:app:compileReleaseKotlin` 0 `w:`/`e:`.
- Only the three A1.2-item-5 tests edited among existing tests.

## Red/green
| Commit | Red | Green |
|---|---|---|
| `1b7cda2` B1 speech model + strings | 25/27 on stubs (2 vacuous, strengthened); purity guard watched red with a temporary solver dependency | 27/27; `:app:check` 1,241 |
| `ea2481a` B2 overlay + board stop | BoardSemanticsTest 15/19 (4 TB-11 guards hold on today's code; 2 strengthened) | 19/19; 1,260 |
| `be7fbf3` B3 ordinals | 7/8 (1 strengthened) | 8/8; 1,268 |
| `cbd541b` B4 gameplay announcements | 10/22 (6 new + audio's 4 retargets); `jumpsFrom` mutation red | 1,281 |
| `505a2ee` B5 tutorial | 8/45 (7 new + retarget) | 1,288 |
| `ca0fda9` B6 Results | 2/3 (third is the latch-false guard) | 1,291 |
| `8d9379c` B7 pointers | docs | — |
| `92b0c31` fix | behaviour unchanged | final Check |

## Outputs
- New `ui/components/`: `BoardSpeech.kt`, `BoardSpeechFormat.kt`, `BoardCellOverlay.kt`, `TouchExploration.kt`, `BoardAnnouncement.kt`; `ui/game/GameTraversal.kt`; `ui/onboarding/TutorialAnnouncements.kt`. Deleted `ui/game/WrongTapAnnouncement.kt`.
- Edited: `BoardCanvas.kt`, `GameScreen.kt`, `GameUiState.kt`, `GameViewModel.kt`, `TutorialScreen.kt`, `ResultsScreen.kt`, `strings.xml` (+29 strings, +2 plurals, −3 `wrong_tap_*`).
- New tests: `BoardSpeechTest`, `BoardSpeechPurityTest`, `BoardSpeechFormatTest`, `BoardSemanticsTest`, `GameViewModelAnnouncementStateTest`, `GameScreenTalkBackTest`, `TutorialTalkBackTest`, `ResultsScreenTalkBackTest`.
- Edited tests (D-TBa items 2–3): `GameScreenWrongTapTest` (three cases → `board_a11y_rejected_*`, identical-repeat kept; `aSelectionOrAPutDownAnnouncesNothing` → `aSelectionAndAPutDownEachAnnounceTheirOwnLine`); `TutorialWrongTapTest` (→ `aWrongTapInTheTutorialSpeaksItsLineOnceWithNoRejectHapticAndNoSound`, recording fakes); `TutorialSourceGuardTest` (KDoc only, every forbidden token kept).
- Docs: four pointers (`docs/ux-spec.md` §4.4.2 TalkBack bullet, 23j item 5, 23k device half; `docs/ux/invalid-tap-feedback.md` §3.6).
- Size: production +1,266/−125 (much KDoc; brief est. ~550); tests +2,201 (est. ~800).

## Checks
- §12 tripwire line 1 (start-path) → nothing; line 2 → one line `+            BoardCellOverlay(` under the interactive branch. `grep -n 'pointerInput\|clickable\|toggleable' BoardCellOverlay.kt` → nothing.
- Frozen files no diff: BoardGeometry, TutorialViewModel, TutorialUiState, NoWinBanner, ResultsViewModel, `domain/`, `dimens.xml`. No `@Suppress`/`@Ignore`/`!!`/baseline/`announceForAccessibility` added. `focusable(` only on cells and the board stop, gated by touch exploration. Single Fable trailer on every commit; worktree clean.

## Deviations
1. `announcement` parameter moved forward to B2 (B2's Box moved audio's live region off the canvas node; an interim `announces` flag kept the tutorial region-free until B5; final API is A1.3's `announcement: String?`).
2. detekt's 11-functions cap: gameplay sources live in `BoardAnnouncement.kt` behind `GameplayBoardAnnouncements`; tutorial effects in `TutorialAnnouncements.kt` (brief: private in `TutorialScreen.kt`); `BoardCanvas.kt`'s interactive branch inline under its existing `LongMethod` suppression.
3. `wrong_tap_*` removed in B5 (TutorialWrongTapTest referenced them until its retarget).
4. Repeat mark by equality with the last line (§5), so A1.3's `REPEAT_EVERY_OTHER = 2` has no literal; `ANNOUNCEMENT_HOLD_MS`, `REPEAT_MARK` moved as `internal const`.
5. The `restartTick` carry is not observable through the ViewModel; test covers what is.
6. TB-11's "no focus move while touch exploration is off" testable only as "nothing is Focused"; the guard on each of the four `requestFocus()` must be read in the diff.
7. Robolectric's `ShadowAccessibilityManager` does deliver the change listener.
8. New helper `BoardSpeechModel.jumpsFrom`.
9. The brief's "both tripwire lines print nothing" vs its own note — line 2 prints the one overlay call.
10. Per-commit gate `:app:check`; full Check at B0 and the tip.

## Open issues / risks
- Device questions (A1.2 item 9): TalkBack follows input focus per move; announcement-then-focus order; re-read of a changed focused cell (fallback A1.2 item 7); stranding jump's two lines; TTS health. Also: "Step 1 of 3" heard twice (A2.4 fallback); landscape / >512dp 26b read (A2.6); TB-7 keyboard check with touch exploration off.
- Restart from the banner on a no-winning-move board raises the confirm dialog while the focus request happens — device to judge.
- Two-line ordering relies on source composition order (holds in tests) — auditor to check.

## QA sequences (English-33)
1. Swipe from the timer: "Board, 32 pegs left", then "Row 1, column 3, peg, no jumps". 2. Double-tap row 2 col 4: "Selected row 2, column 4, 1 jump." 3. Action "Jump down to row 4, column 4": "Row 2, column 4 to row 4, column 4. Removed row 3, column 4. 31 pegs left." (focus on row 4 col 4). 4. Double-tap row 2 col 4: "No peg here." 5. Select row 5 col 4, double-tap row 1 col 3: "Put down the selected peg first." 6. Double-tap row 2 col 4: "Not a jump target." 7. Undo: "Undid row 2, column 4 to row 4, column 4. 32 pegs left." 8. Restart: "Board restarted. 32 pegs left." Tutorial: opening says "Step 1 of 3. Tap a peg, then tap the empty hole two cells away." (focus not moved); steps 2–3 move focus to the board stop; completion moves focus to "Got it".

## Handoff
code-auditor: diff against brief §2, §3, §12, §13.3, the deviations, the three test edits. qa-engineer: device pass per §10 and A2.6 (Pixel_9 or a phone, never Pixel_9a_bench). tech-lead: sign-off (B0 `c690e55` had no conflict hunks). producer: merge at step 5a.
