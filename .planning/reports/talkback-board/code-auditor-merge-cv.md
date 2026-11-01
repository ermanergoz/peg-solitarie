# code-auditor report — talkback-board, the merge-in of develop with colour-vision and licences
Date: 2026-11-01
Branch: feature/talkback-board (2fe6568..35c002a; the merge `2fe6568` has parents `01cb475` (this branch) and `bc95b2a` (`develop`: colour-vision and licences-screen); `35c002a` adds only the developer's report)
Skills loaded: ergoz-soft:standards (role code-auditor). The snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus the product's `.claude/rules/lessons.md`. Other skills: code-reviewer, security-reviewer, compose-skill, compose-performance-audit.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort; the harness names none). **Staffing deviation:** `agents/code-auditor.md` pins fable. Founder answer Q-019 puts every Fable-pinned role on Opus until 3.0.0 is published. The author also ran on Opus, so rule 1's "superior model" does not hold. Q-019's mitigations apply: I am a fresh reviewer, I ran my own Check with `--rerun-tasks`, and I ran my own mutation probes. Every ruling below rests on a command I ran.
Fan-out: none
Loops: 0 (first review of this merge-in)
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Every hand-resolved hunk keeps both sides.** I re-derived the merge with `git merge-tree`: it gives 4 conflicted files, and its tree differs from `2fe6568` only in those 4 files plus the two test fakes. For each of the 6 files both sides changed, I compared each side's changed lines against the merge minus the other parent. Four files are identical in both directions. In `BoardCanvas.kt` the only difference is the one `@Suppress` comment line. In `GameScreen.kt` the only difference is the indentation of `boardMarkPaletteValue = boardMarkPalette`.
- **The `@Suppress("LongMethod")` on `BoardCanvas` is the same annotation, with its two comments combined.** The count is 12 in `app/src/main` on the merge base, on both parents and on the merge. Across all `.kt`/`.kts` files the count is 31 on all four. No `@Ignore`, `ktlint-disable` or baseline file was added.
- **`BoardCanvas.kt` carries all three intents**, and probes show each is under test:
  - colour-vision's hint-ring rebuild (P1);
  - this branch's TalkBack overlay (P2);
  - home's per-frame geometry (P3);
  - colour-vision's `BoardLook` palette on the playable canvas (P4).
  - P5 covers the hand-placed `GameScreen` palette semantics line.
  - All five probes go red as expected.
- **Check: green** with `--rerun-tasks` (107/107 tasks executed): `:app` 1,558 tests, `:domain` 296, `:build-logic` 100, with 0 failures, 0 errors and 0 skipped in each. The tripwire is clean.
- **VERDICT: PASS.** No finding to fix. I approve the edits to the two test fakes. There are two Info items.

## Outputs
- `.planning/reports/talkback-board/code-auditor-merge-cv.md`: this report, committed alone on the branch.

## Review

### 1. The re-derived merge
- `git merge-tree --write-tree --name-only 01cb475 bc95b2a` gives tree `9e14613` with 4 content conflicts: `BoardCanvas.kt`, `GameScreen.kt`, `TutorialScreen.kt` and `ResultsScreen.kt`. `strings.xml` and `GameScreenWrongTapTest.kt` auto-merged.
- `git diff --stat 9e14613 2fe6568` lists exactly these 6 files: the 4 conflicted files (marker and duplicate-side lines removed) and the 2 test fakes (+8/−2). **No other file was hand-edited.**
- Six files were changed by both sides since the merge base `305f741`. For each, I compared sorted `-U0` `+`/`-` lines in both directions:

| File | develop's side (`305f741..bc95b2a` vs `01cb475..2fe6568`) | branch's side (`305f741..01cb475` vs `bc95b2a..2fe6568`) |
|---|---|---|
| `TutorialScreen.kt`, `ResultsScreen.kt`, `strings.xml`, `GameScreenWrongTapTest.kt` | identical | identical |
| `BoardCanvas.kt` | only the `@Suppress` comment line | only the `@Suppress` comment line |
| `GameScreen.kt` | only `boardMarkPaletteValue = boardMarkPalette`, now 4 spaces deeper | the same line, as an addition |

- `strings.xml` has no duplicate `name=`.
- `rememberWrongTapAnnouncement` and `wrongTapAnnouncement` have no definition or use left in `app/src`. The only hit is the forbidden-token list in `TutorialSourceGuardTest.kt:61`. Develop's side did not change that line (it was context there), so dropping it is this branch's existing removal (brief §13.3), not a lost develop change.
- The developer's report gives the same per-file figures. I re-derived them myself and they match.

### 2. The hand-resolved hunks (`git show --cc 2fe6568 -- app/src/main`)
- **`BoardCanvas.kt:101`**: `@Suppress("LongMethod") // One render unit: state, palette + marks, animations, and the playable board's stops (D-TB).`
  - It is the one annotation both sides already had on `fun BoardCanvas`. Each side had amended its surface list, and the merged comment names both amendments.
  - This is not a new suppression and needs no new approval. Brief §12 ("stay with their surface lists amended, never a second one") is met.
- **`GameScreen.kt`, 3 hunks:**
  - The imports are the union: develop's `BoardMarkPaletteKey` and `LocalBoardMarks` are used at `:84` and `:201`.
  - The state lines keep this branch's announcer and focus lines and develop's `boardMarkPalette` read.
  - The `Column` keeps this branch's root-`Box` structure. Develop's one new semantics line sits in the `GAME_STATE_TEST_TAG` `Box`'s block (`:248`), beside `legalTargetCountValue` and before `isTraversalGroup = true`. That is the same node develop put it on. P5 shows the line is what `ColorblindPaletteLiveSwitchTest` and `BoardCanvasPaletteTest` read.
- **`TutorialScreen.kt` (2) and `ResultsScreen.kt` (1)** are import unions: develop's `Brush` and `StarGlyph`, and this branch's focus, `BoardStop` and announcer imports. Develop's bodies auto-merged unaltered (§1 table).

### 3. The auto-merged `BoardCanvas.kt`: three intents
- **This branch's shared `draw`.** `val draw: DrawScope.() -> Unit` (`:210-228`) is the only draw. The playable `Canvas` (`:244-254`, inside the `Box`, then `BoardCellOverlay` at `:256-262`) uses it, and so does the read-only `Canvas` (`:267-274`).
  - The read-only `Canvas`'s modifier chain is develop's, less develop's `.then(Modifier)` no-op for a non-interactive board.
- **Colour-vision's `BoardLook` and unsized-frame hint-ring rebuild.**
  - `rememberBoardLook(spec, canvasSize, interactive)` and `look.palette` / `look.recipe` sit at `:115-118`, as on develop.
  - Develop's `unsizedFrame`, `forCanvas(spec, size, recipe)`, `recipe = recipe` and `unsizedFrame = unsizedFrame` all land inside the shared `draw`.
- **Home's per-frame geometry.** `geometrySize = canvasSize` is captured in composition (`:123`), and the frame geometry is built in `draw` (`:217-218`).
- **Below the composable, nothing moved.** From `jumperCenter`'s KDoc to the end of the file (418 lines), `develop` and `HEAD` hash the same (`34aad15f79a5`).
- `git diff develop HEAD -- BoardCanvas.kt` has 5 hunks, all at or above `:276`: imports, KDoc, the `@Suppress` comment, `Canvas` → `draw`, and the `if (interactive)` split.

**Probes.** These ran in a throwaway `git worktree add --detach 35c002a` under this session's scratchpad, outside the repo. I copied `local.properties` in for the SDK path only.
- The unmutated baseline was green: `BoardCanvasFirstFrameTest` 4/4, `BoardCanvasPaletteTest` 3/3, `BoardSemanticsTest` 20/20 and `GameScreenTalkBackTest` 25/25.
- Each mutant was written to compile under `allWarningsAsErrors`, with no unused value: the mutated condition is false at run time.
- After each run the worktree was reverted with `git checkout -- .`, and `git diff --quiet` confirmed it clean. The worktree was then removed (`git worktree remove --force`, `git worktree prune`).

| # | Mutation (production only) | Result | Shows |
|---|---|---|---|
| P1 | `drawHintHighlight`: `restroke = unsizedFrame && palette.hintRingsDashed` gains `&& hintRingDashes < 0` (no rebuild on an unsized frame) | `BoardCanvasFirstFrameTest.theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes` **red**: `pixels inside board_canvas that differ between the first frame and the settled one / expected: 0 / but was : 547`. The other 3 first-frame cases and the palette tests (3/3) stay green | Colour-vision's hint-ring rebuild survives the merge, and its own test pins it. |
| P2 | The interactive `Box`'s `BoardCellOverlay(…)` wrapped in `if (spec.inPlay.isEmpty())` (overlay never composed) | **red**: `BoardSemanticsTest` 14/20, `GameScreenTalkBackTest` 7/25, `TutorialTalkBackTest` 12/16. For example, `englishHasThirtyThreeCellNodesInRowMajorTraversalOrder`: `missing (33)` | The TalkBack overlay is live on the merged playable canvas, on gameplay and on the tutorial. |
| P3 | `draw`: `frameGeometry` built only if `unsizedFrame && size.width < 0f` (home's per-frame geometry off) | `BoardCanvasFirstFrameTest` **4/4 red**, each `peg ink inside … in the first frame … but was : 0`. That includes both interactive colour-blind cases and `home_stage_art`. `HomeChipArrivalTest` 13/13 green | Home's fix holds on both canvases. |
| P4 | `rememberBoardLook(spec, canvasSize, interactive && spec.inPlay.isEmpty())` (playable canvas off the palette) | **red**: `BoardCanvasPaletteTest` 3/3 (`the halo inside the selection ring / expected to be true`) and `ColorblindPaletteLiveSwitchTest` 1/1 (`board pixels the flip changed … but was : 0`) | Colour-vision's palette reaches the gameplay board under this branch's `Box` and overlay. The live-switch test composes `GameScreen`. |
| P5 | `GameScreen.kt:248`: `boardMarkPaletteValue` forced to `Standard` | **red**: `BoardCanvasPaletteTest` 3/3 and `ColorblindPaletteLiveSwitchTest` 1/1, `expected: Colorblind / but was : Standard` | The hand-placed semantics line sits on the node the tests read. |

- **This closes my merge-home I-1.** That review found that no JVM case composed the interactive canvas's first frame. Colour-vision's two colour-blind cases do: they compose `BoardCanvas` with the default `interactive = true`. P3 shows both go red if home's fix is lost from the playable canvas.

### 4. Ruling on the two test-fake edits
**APPROVED.** The files are `GameScreenTalkBackTest.kt` (`TalkBackSettings`) and `GameViewModelAnnouncementStateTest.kt` (`AnnouncementStateSettings`).
- Both are this branch's own test files: neither exists on `develop`. Brief §12's "zero diff to existing test files" protects tests that came from `develop`, so this edit is not of that kind. I rule on it because the developer asked (their Open issue 1).
- Colour-vision added two abstract members to `SettingsRepository`. Each fake gains exactly the two lines colour-vision gave every other fake: `colorblindPaletteOn = flowOf(false)` and a no-op setter. Each also gains a KDoc clause. No assertion or setup changed (`git diff 01cb475 2fe6568 --` on each: +4/−1).
- Both suites are green in my Check: `GameScreenTalkBackTest` 25/25 and `GameViewModelAnnouncementStateTest` 8/8.

approved by code-auditor: the `colorblindPaletteOn` / `setColorblindPaletteOn` overrides added to `TalkBackSettings` (`GameScreenTalkBackTest.kt`) and `AnnouncementStateSettings` (`GameViewModelAnnouncementStateTest.kt`) in merge `2fe6568`. They are required by colour-vision's `SettingsRepository` and weaken no assertion.

### 5. Tripwire, start path, boundaries, security
- **Brief §12 / ruling §11.4**, against `develop` = `bc95b2a`:
  - Line 1 prints nothing (rc 1). I widened it to `values*-vNN/themes.xml`, and it still prints nothing.
  - Line 2 prints only `+            BoardCellOverlay(`, which sits under `if (interactive)` (`:256`).
  - `grep -n 'pointerInput\|clickable\|toggleable' BoardCellOverlay.kt` prints nothing.
  - **No 15e sitting is triggered.**
- **Boundaries.** The merge adds no production line of its own beyond the hand hunks above. `:domain` has no diff from this branch.
- **Security.** There is no new dependency on this branch's side. Colour-vision's and licences' dependency and asset changes arrive through `develop` under their own sign-offs. The merge adds no secret, no network path and no input path.
- **Performance.** `draw` allocates a geometry only on a size mismatch, which is develop's behaviour. P1's restroke allocates a dashed `Stroke` only on such a frame. CV-a and CV-b (colour-vision's 3.0.1 items) are unchanged by the merge.
- **Commits.** `2fe6568` and `35c002a` each end with exactly one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, and carry no `Claude-Session` line.

APPROVED SUPPRESSION: none (none added; the existing `BoardCanvas.kt:101` `@Suppress("LongMethod")` carries a combined comment only).

### Findings
No Critical, Major or Minor finding.
- **I-1 (Info, tech-lead).** No device has seen colour-vision's palette and this branch's overlay together, as the developer's Open issue 5 says. P4 and P5 show on the JVM that the palette reaches the gameplay board under the overlay, and P2 shows that the overlay is intact. Whether QA owes a smoke pass is the tech-lead's call (ruling §11.5). Nothing is owed by the code.
- **I-2 (Info, process).** My P1 probe run started while my own Check was running, at a 1-minute load of 19.65, which is above the dispatch's bound of 12. My probe script printed `uptime` but did not gate on it. Both builds finished. The Check started at about 2.8, and every later probe started between 4.9 and 7.8. No emulator was running at any point. I record this as a breach of the host rule; it does not change any result.

## Checks
- `uptime` before the Check gave a load of 2.83. `ps` showed no emulator or `qemu` process, so no `Pixel_9a_bench`. No `.gradle/ergoz-build.lock` was held.
- `./scripts/check.sh --wait --rerun-tasks --max-workers=4 --console=plain` at `35c002a` (code = `2fe6568`) — **pass:**
  - `BUILD SUCCESSFUL in 11m 48s`, `107 actionable tasks: 107 executed`, `CHECK_EXIT=0`;
  - JUnit XML: `:app` 173 suites, 1,558 tests; `:domain` 35 suites, 296 tests (fuzz and depthSweep included); `:build-logic` 10 suites, 100 tests. 0 failures, 0 errors and 0 skipped in each;
  - lint: 64 warnings, 0 errors;
  - 57 `w:` lines: 56 from `src/test` and one Kotlin-daemon notice. None from `src/main`, and no `e:` line.
- `git merge-tree --write-tree --name-only 01cb475 bc95b2a` and `git diff --stat 9e14613 2fe6568` — pass, as in §1.
- The `-U0` changed-line comparisons for the 6 two-sided files, both directions — pass, as in the §1 table.
- `@Suppress` counts (12 in main, 31 overall), `@Ignore` (0), `ktlint-disable` (0), baseline files (none) on `305f741`, `01cb475`, `bc95b2a` and `2fe6568` — pass, unchanged.
- Tripwire lines 1 and 2, the widened line 1, and the overlay pointer grep — pass (§5).
- The `jumperCenter`-to-end-of-file hash on `develop` and `HEAD` — pass: `34aad15f79a5` for both.
- Probe baseline 52/52, and probes P1–P5 — each as in §3's table.
- The throwaway worktree was removed. `git status --short` in `PegSolitarie-tb` was empty before this report was written.
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- **I-1** (a device smoke pass of the palette and overlay together) is the tech-lead's to decide.
- **I-2** is recorded as a host-rule breach of my own.
- **Staffing:** this review, like the code, ran on Opus under Q-019 (see `Ran on:`).
- **Before merging,** the producer confirms `develop` is still `bc95b2a`. Otherwise `develop` is merged in again and that merge is reviewed.

## Handoff
Next: tech-lead.
- Sign off (ruling §11.5) from this report, `git show --cc 2fe6568`, and the developer's `android-game-dev-merge-cv.md`.
- Decide I-1.

Then: producer.
- Merge once `develop` is confirmed at `bc95b2a`.

VERDICT: PASS
