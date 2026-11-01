# tech-lead report — talkback-board (final technical sign-off)
Date: 2026-11-01
Branch: feature/talkback-board (report only; head `2811c84`, last code commit `2fe6568`; `develop` = `bc95b2a`)
Skills loaded: ergoz-soft:standards (role tech-lead. The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. I also read the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** `agents/tech-lead.md` pins fable at xhigh. Founder answer Q-019 (option 3, its 2026-10-24 addendum) keeps Fable-pinned roles on Opus until 3.0.0 is published. The 2026-10-31 directive sets this role one effort step below its pin. The code, every review and both of my rulings on this branch also ran on Opus. Mitigation: every condition below rests on a reviewer's command output, a device artifact the reports quote, or a read-only command I ran myself (Checks).
Fan-out: none
Loops: 0
Read in full: `code-auditor-loop5.md`, `code-auditor-merge-cv.md`, `code-auditor-merge-home.md`, `code-auditor-loop4.md`, `code-auditor-loop3-record.md`, `code-auditor.md` (loops 0–2), `qa-engineer-device3.md`, `qa-engineer-device2.md`, `ux-designer-b5.md`, `ux-designer-b6.md`, `ux-designer-b8.md`, `ux-designer-step-order.md`, `tech-lead-device2-ruling.md`, `android-game-dev-merge-cv.md`, `android-game-dev-device2-fixes.md`, `android-game-dev-loop3.md`. In part: `tech-lead-defect-ruling.md` (Summary, §10, §11, Open issues, Handoff), `tech-lead-brief.md` (header, Amendments 1 and 2, §10–§14), `qa-engineer.md` (Summary; FAIL, superseded), `android-game-dev.md`, `android-game-dev-loop4.md` and `android-game-dev-merge-home.md` (Summaries and Outputs), `docs/ux-spec.md` (the "Recorded, not hidden" lines of D-TBb, D-TBd and D-TBe)
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Every sign-off condition is met.** The conditions are defect ruling §11.5 and device-2 ruling Handoff 5. The latest review of each code range is PASS: loop 5, merge-home and merge-cv. QA's device-3 re-check is PASS: 3 of 3 openings, 6 of 6 step lines with none lost, and 3 of 3 completions. Device-2's FAIL items are closed: B-1's residual and A2.4 under D-TBe, and B-8 under D-TBd. The §6 fallback was not triggered.
- **The Check is green on the exact tree that will merge.** The auditor ran it at code `2fe6568` with `--rerun-tasks`: `:app` 1,558 tests, `:domain` 296, `:build-logic` 100, 0 failures. `develop` has not moved, and `git merge-tree develop HEAD` returns HEAD's own tree. I had no reason to re-run it.
- **I-1: no QA smoke pass is owed before the merge.** The overlay draws no pixels and takes no pointer input, and the palette adds nothing TalkBack reads. Probes P2, P4 and P5 show both reach the merged board. I place one two-minute check in the release device pass instead (§2).
- **Open items placed.** The step-3 gap, the slow-speech hint and the other code items are 3.0.1. The repeat of the step lines on a phone and the hardware-keyboard check belong to the release device pass, and neither holds the merge (§3). The Q-PUBLISH-3.0.0 lines are in §4.
- **VERDICT: PASS.** Merge `--no-ff` while `develop` is still `bc95b2a`.

## 1. The sign-off conditions, checked
| # | Condition (source) | Evidence | Met |
|---|---|---|---|
| 1 | Every code-auditor report on the branch, all loops (defect ruling §11.5; device-2 ruling Handoff 5) | `code-auditor.md`: loops 0 and 1 FAIL, loop 2 PASS. `code-auditor-loop3-record.md`: FAIL on two test gaps only. `code-auditor-loop4.md`: PASS, both gaps closed by P9/P10 reds. `code-auditor-merge-home.md`: PASS. `code-auditor-loop5.md`: PASS, probes P1–P4, P7, P8 and R-FA/R-FB1 all red, baseline 16/16. `code-auditor-merge-cv.md`: PASS, merge re-derived with `git merge-tree`, P1–P5 red | yes |
| 2 | QA: `qa-engineer.md` (FAIL, superseded), `qa-engineer-device2.md`, `qa-engineer-device3.md` | device2 passes B-2, B-3 (26b and A2.6), B-4, B-6, B-7, B-9 and the smoke rows; it fails B-1's residual, B-8 and A2.4. device3 closes B-1's residual and A2.4: openings 3/3, step lines 6/6 on both routes with zero lost (one sample at a host load of 21.87), completions 3/3, and the rejected-tap and coach smoke spoken. The APK sha256 `f7515278…19dae65` matches the installed `base.apk` | yes |
| 3 | The ux-designer's rulings: B-5 (D-TBb), B-6 (D-TBc), B-8 (D-TBd), B-1 and A2.4 (D-TBe) | D-TBb: the gameplay menu route is at the floor 3/3, and the reading control lands on the destination (device2). D-TBc: the summary comes first, 2/2 (device2). D-TBd's pass line accepts device2's "Navigate back" 3/3 and the decline result. D-TBe: device3 is judged against TB-9's device-3 pass line verbatim | yes |
| 4 | The device-2 ruling's fix list: F-A then F-B1, only `TutorialAnnouncements.kt` and `TutorialAnim.kt` | `git diff --name-only 6a98510..HEAD -- app/src/main` lists those two (developer and loop 5). The shared `JumpAnnouncements` is untouched | yes |
| 5 | The defect ruling's F0–F5, and its "not edited" list | Loop 3 traced F0–F5, and every ruled probe behaved as expected. The frozen list is clean in my own `git diff develop...HEAD`: no diff in `:domain`, `TutorialViewModel`, `TutorialUiState`, `ResultsViewModel`, `BoardGeometry`, `NoWinBanner`, `dimens.xml`, Gradle or `THIRD_PARTY.md`. `GameViewModel`'s diff is brief §5's lines only (`restartTick` ×4, `undoneMove`, and two comments) | yes |
| 6 | The developer's deviations need my acceptance (`android-game-dev-loop3.md`) | **Accepted.** Deviation 1, the channel drawn last at 1 px: the auditor read the cause (M3 `Surface` `isContainer`), and P12 guards the footprint. The device shows TalkBack speaks the 1 px polite region: Results' summary once (device2), 6/6 rejected taps, and 6/6 step lines (device3). Deviations 2 and 3, the native-graphics touch-mode harness: P5 is red and P6 green, as ruled. Deviation 6, `LatestFocusMove`: pinned by loop 4's test | yes |
| 7 | Every merge-in's conflict hunks re-read (brief §13.3; defect ruling §11.5) | F0 `b84853e`: docs only, both sides kept (loop 3). `a980033` home: `BoardCanvas.kt`, both sides byte-identical (merge-home). `2fe6568` colour vision and licences: I re-read `git show --cc 2fe6568 -- app/src/main`. There are 7 hand hunks in 4 files: one combined `@Suppress` comment, import unions, and develop's `boardMarkPaletteValue` line on the node the palette tests read (P5). Merge-cv confirmed both sides from both directions | yes |
| 8 | Existing-test edits named and approved | Each has an "approved by code-auditor" line: audio's three retargets (loop 0); home's `GameScreenChromeTest` (merge-home); loop 5's two unnamed retargets; the two settings fakes (merge-cv) | yes |
| 9 | No new suppression; one `requestFocus()`; overlay pointer-free; read-only path unchanged; tripwire (brief §12; ruling §11.4) | My checks: `@Suppress` count in main is 12 on `develop` and on HEAD, the `+` hit being the existing `BoardCanvas` annotation's combined comment. No `@Ignore`, `!!`, `GlobalScope` or `announceForAccessibility` call was added. The only `requestFocus()` is `TouchExploration.kt:76`. The overlay grep prints nothing. Tripwire line 1 prints nothing, also when widened to `values-vNN`. Line 2 prints only `+            BoardCellOverlay(`, which is under `if (interactive)`. **No 15e sitting** | yes |
| 10 | The Check is green on the tree that merges | merge-cv: `./scripts/check.sh --wait --rerun-tasks` at `35c002a` (code `2fe6568`), 107/107 tasks executed, `CHECK_EXIT=0`, lint 0 errors, no `w:` from `src/main`. `2fe6568..2811c84` adds two reports only. `develop` = `bc95b2a` = the merge base, so the `--no-ff` merge adds nothing the Check has not run | yes |

**The architecture holds as briefed.** The words come from a pure model (`BoardSpeechPurityTest`). The overlay is semantics-only above the unchanged tap path. There is one channel per screen. Every focus move goes through one gated, window-aware function. The read-only Home path is develop's.

## 2. I-1 — does QA owe a smoke pass of the palette under the overlay? No.
**Ruling: not a merge condition.** Under founder directive (2) and my reading:
1. **Nothing in the combination can show on a device that the JVM does not already pin.**
   - The overlay is semantics nodes with no draw and no pointer input; the grep prints nothing.
   - The palette changes no spoken semantics: its one new key, `BoardMarkPaletteKey`, is a test key, as colour-vision's sign-off item 2 rules.
   - The palette reaches the playable canvas inside this branch's `Box`: P4 and P5 are pixel and semantics reds on `GameScreen`.
   - The overlay is live on the merged board: P2 is red on gameplay and the tutorial.
2. **The merge adds no production behaviour.** Its hand hunks are import unions, one comment, and one re-indented semantics line. This branch's main-source footprint against `develop` is the same 17 Kotlin files and `strings.xml` as before the merge. That footprint is what QA passed on device2 and device3.
3. **Every other part already has its own device record.** Colour vision has its smoke and 15e sitting on its merged APK, and licences has its own. Neither touches a file of this branch beyond the four whose hunks are above. No start-path file differs from `develop`.

Colour-vision's Ruling 2 owed a smoke because that branch's own drawing code met code it had never run with. Here the new code (develop's) draws, the branch's code speaks, and neither reads the other's output. **Placed instead:** the 3.0.0 release device pass runs one item of about two minutes on the release APK, the final tree with candy-backdrop. With the colour-blind palette on and TalkBack on, in one gameplay game: a cell is read, a peg is selected, a hint is revealed, and one jump is made. The selection ring, the target marks and the dashed hint ring must draw, and the lines must be spoken. A failure goes to the developer as a 3.0.0 defect.

## 3. Open items, placed
| Item | Source | Placement | Holds the merge? |
|---|---|---|---|
| **The ~700 ms step-3 gap**: completion inside step 3's focus gap plus the 500 ms wait lets "Step 3 of 3" follow the done line | auditor loop 5 Info 1; developer open issue | **3.0.1.** `LaunchedEffect(state.stepIndex, state.completed)` in `TutorialAnnouncements.kt`, with a paused-clock T3 test, red first. It cannot be reached today: step 3 is four jumps, eight activations under TalkBack | No |
| The same window for a selection or rejected-tap line within ~700 ms of a swap | loop 5 Info 2 | **3.0.1**, in the same brief as the next row | No |
| **The slow-speech hint replacing the step line**: at a slow speech rate, the coach's hint 4 s after the swap can take the place of a step line that has not been spoken yet | device-2 ruling open issue 3; D-TBe "Recorded, not hidden" | **3.0.1**: "the first nudge after a step change never takes an unspoken step line's place" (D-TBe's candidate), one brief with the two rows above. The spec does not owe a replaced line, and the hint names the move, so no player is stuck | No |
| The step line waits on the unbounded window-focus wait | loop 5 Info 3; loop 3 Info | **Backlog.** Every focus loss we can name returns focus. Revisit only if a device shows a stall | No |
| A recreation at step 2 or 3 speaks the caption without "Step N of 3" | loop 5 Info 4 | **ux-designer backlog**, judged only if a mid-tutorial rotation is ever read. This predates the diff | No |
| Results' `spoken` flag saved before the arm's 200 ms has passed, so a rotation in that window loses the summary | loop 3 Info | **3.0.1 low**: set the flag when the line is shown, with a test that recreates during the arm | No |
| **The 6 step lines repeated on a phone** | QA device3 open issue 1; D-TBe | **Release device pass, on a phone when one is attached.** Directive (4) says no physical device, so if the release pass is emulator-only, device3's evidence stands. The Q-PUBLISH summary then says TalkBack was verified on one emulator image (§4) | No |
| **L1-2, the hardware keyboard with TalkBack on: BLOCKED** | QA device2; auditor loop 1 L1-2 (platform limit, accepted in loop 2) | **Release device pass**, only with a physical device and a keyboard. Otherwise a 3.0.1 QA item, recorded as untested in the Q-PUBLISH summary | No |
| merge-home I-1: the interactive canvas's first frame was not pinned | merge-home | **Closed** by colour vision's two colour-blind first-frame cases, which compose `interactive = true` (merge-cv P3) | — |
| B-10 landscape clipping; the banner re-announced on rotation; UMP consent over step 1; the count-up and `StarPopSounds` replay on rotation; O-1 "Perfect!" heard twice | defect ruling §10; QA device2 O-1, O-2 | As the defect ruling §10 routed them: **3.0.1 or backlog**, B-10 in the Q-PUBLISH summary. O-1 goes to the ux-designer for awareness | No |
| The completion row moved to focus-then-speak; the step line carried in the board stop's own text | device-2 ruling §6 | **3.0.1 candidates**, only if a phone shows the completion row losing its line. It passes 3/3 today | No |
| Two producer docs commits (`6a98510`, `abc9b4a`) carry the Opus trailer and a `Claude-Session` line | auditor loop 5 Process; my trailer check | **Producer, process.** This is the known missing `prepare-commit-msg` hook. No history rewrite; the commits are docs only | No |

## 4. Lines this branch contributes to the Q-PUBLISH-3.0.0 summary
These are for the producer to put in the TalkBack block, beside D-TBa item 10's cell size and B-10:
- **D-TBb floor:** a jump taken through TalkBack's actions menu speaks its line, which names the destination. When the menu closes, TalkBack leaves focus on the source cell (now empty), not on the moved peg; the player swipes to it. The Actions reading control and double-tap land on the destination. In the tutorial, the menu route reaches the new board stop.
- **D-TBd:** after a banner Restart is confirmed in its dialog, "Board restarted. N pegs left." is spoken. Focus then rests where TalkBack puts it as the dialog closes, which is "Navigate back", not the board stop.
- **D-TBe:** in the tutorial under TalkBack, the new step's board stop is spoken first ("Board, N pegs left"), then the step's line. The jump that clears steps 1 and 2 speaks no line of its own, so the menu route hears no confirmation of that jump. Opening speaks the caption alone, and "Step 1 of 3" comes from the pips. The 500 ms timing held 6/6 on the emulator. A very slow speech rate can let the coach's hint replace the step line (3.0.1).
- **Verification scope:** TalkBack 16 on one API-36 emulator image at the default speech rate. There was no physical phone, and the hardware-keyboard check is untested (BLOCKED).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/tech-lead-signoff.md` — this sign-off. It is the only file written, committed alone. No product code, no Gradle, no device, no `.planning/log/`.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-tb`.
- `cat .claude/rules/VERSION` gives 0.6.2, against the plugin's `plugin.json` 1.0.0. The snapshot is older, so the plugin copies were read in full. `.planning/recall/` is absent.
- `git log --oneline -25`; `git rev-parse develop` gives `bc95b2a`, which equals `git merge-base develop HEAD`; `git merge-base --is-ancestor develop HEAD` exits 0; `git status --short` is clean — pass.
- `git merge-tree --write-tree develop HEAD` gives `9889f05…`, which is `HEAD^{tree}` (rc 0). The merge adds nothing the Check has not run — pass.
- `git diff --stat 2fe6568..HEAD` lists two report files only. The Check's tree is the code that merges — pass.
- Tripwire line 1 (brief §12, widened to `values-vNN/themes.xml`) prints nothing (rc 1). Tripwire line 2 prints only `+            BoardCellOverlay(` — pass.
- `grep -n 'pointerInput\|clickable\|toggleable' BoardCellOverlay.kt` prints nothing — pass.
- `git diff --name-only develop...HEAD --` over the frozen list (`domain`, `TutorialViewModel.kt`, `TutorialUiState.kt`, `ResultsViewModel.kt`, `BoardGeometry.kt`, `NoWinBanner.kt`, `dimens.xml`, `gradle`, `build-logic`, `app/build.gradle.kts`, `THIRD_PARTY.md`) prints nothing — pass.
- `git diff develop...HEAD -- app domain | grep -nE '^\+.*(@Suppress|@SuppressLint|@Ignore|!!|GlobalScope|Thread\.sleep|ktlint-disable|tools:ignore|announceForAccessibility)'` gives two hits. One is a KDoc saying `announceForAccessibility` is not used. The other is the existing `BoardCanvas` `@Suppress("LongMethod")` with its combined comment. `git grep -c '@Suppress'` in `app/src/main` gives 12 on `develop` and 12 on HEAD — pass.
- `grep -rn 'requestFocus()' app/src/main` gives one hit, `TouchExploration.kt:76` — pass.
- `git diff develop...HEAD -- …/GameViewModel.kt` shows `restartTick` carried ×3 and advanced ×1, `undoneMove = undone`, and two comments, within brief §5 — pass.
- `git show --cc 2fe6568 -- app/src/main`, read: the 7 hand hunks agree with merge-cv §2 — pass.
- I checked the trailer on all 55 commits in `develop..HEAD`. 53 carry exactly the studio's Fable trailer. `6a98510` and `abc9b4a` (producer docs) carry the Opus trailer and a session line: recorded under §3, not blocking.
- `grep` of `docs/ux-spec.md` for D-TBd (11 hits) and D-TBe (20 hits), and a read of their "Recorded, not hidden" lines — pass.
- `grep` of `.planning/questions/FOUNDER.md` (Q-019 and its addenda) and `.planning/reports/STATUS.md` (the 2026-10-31 directives 1–4). Read the colour-vision `tech-lead-merge-ruling.md` Ruling 2 and `tech-lead-signoff.md` items 1–6, as the precedent for I-1.
- Not run: the Check (the auditor's forced run at this code is green, and the tree has not changed since), Gradle, `adb`, an emulator, `.planning/log/`. A sign-off re-runs no gate.

## Open issues / risks
1. **TalkBack's queue behaviour is inferred**, from its logs, not from its source. F-B1 rests on two facts that every device3 sample shows. The 500 ms margin is about three times the measured round trip, and it held once at a host load of 21.87.
2. **One instrument.** Every device verdict on this branch comes from `ergoz_studio` (API 36, TalkBack 16.0, Google TTS, default rate). Phones and other TalkBack versions are unverified until the release pass, which may also be emulator-only (directive 4).
3. **Staffing.** The code, every review and this sign-off ran on Opus under Q-019. Rule 1's "superior model" did not hold at any step. The mitigations were fresh reviewers, a forced Check, and mutation probes that each reviewer ran itself.
4. **This sign-off lapses if `develop` moves** before the merge. Then `develop` is merged in again, and that merge-in is reviewed as merge-cv was.

## Handoff
Next: producer —
1. Confirm `git rev-parse develop` is still `bc95b2a`. Then `git merge --no-ff feature/talkback-board` into `develop`. If `develop` has moved, a merge-in plus an auditor review of it come first.
2. Put §4's four lines in the Q-PUBLISH-3.0.0 summary's TalkBack block, beside D-TBa item 10 and B-10.
3. Add two items to the release device pass:
   - §2's two-minute palette-under-TalkBack check;
   - on a phone if one is attached, the 6 step lines and L1-2.
4. Route the 3.0.1 items in §3:
   - to the tech-lead, one brief: the step-3 gap, the swap-window lines and the first-nudge rule;
   - Results' `spoken` flag, low priority.

   Route the ux-designer backlog items as the defect ruling §10 did, plus loop 5 Info 4 and O-1.
5. Then delete `.planning/qa/talkback-board/` (session3 62 MB, session4 18 MB) after the STATUS.md Done line (rules/testing.md).

VERDICT: PASS
