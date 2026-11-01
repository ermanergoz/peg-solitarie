# code-auditor report — colour-vision (review of the develop merge-in after home-chip-arrival)
Date: 2026-11-01
Branch: feature/colour-vision (904d735..6a6e82d; my Check ran at `f535aea`, whose code tree equals `6a6e82d`: `git diff --quiet 6a6e82d f535aea -- . ':(exclude).planning'` exits 0)
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing, plus the snapshot's `lessons.md`. Also code-reviewer, security-reviewer, compose-skill and compose-performance-audit
Ran in parallel with: tech-lead hint-ring ruling, talkback reviews, candy auditor
Ran on: claude-opus-5-5 at effort high (policy). **Staffing deviation, recorded:** `agents/code-auditor.md` pins fable at xhigh. Q-019's answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published, so this review shares the developer's model. The founder's 2026-10-31 directive (1) puts the code-auditor one effort step below its pin, and the harness names no effort. **Mitigation, per Q-019:** I ran my own Check with `--rerun-tasks` (104 of 104 tasks executed), plus five mutation and residue probes of my own in a throwaway worktree, since removed. No count below is taken from the developer's report.
Fan-out: none
Loops: 0 (this review's first pass. The branch's earlier audits are `code-auditor-loop0.md` and `code-auditor.md`)
Read in full: `.planning/reports/colour-vision/android-game-dev-merge-home.md`; `.planning/reports/home-chip-arrival/tech-lead-c11-ruling.md`. Read in part: `reduced-motion-live-gate/tech-lead-signoff.md` §3; `colour-vision/tech-lead-brief.md` §10–§12 and line 220 (§3.2's stroke rule); `colour-vision/code-auditor.md` (header, the Q-027 lines); `.planning/questions/FOUNDER.md` Q-019, Q-026, Q-027
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **The merge `904d735` keeps both sides.**
  - The three RM KDoc conflicts are resolved as RM sign-off §3 ruled.
  - `BoardCanvas.kt` keeps develop's first-frame `geometrySize` capture over this branch's `BoardLook`. Develop's inline palette block, which was dropped, already existed at the merge-base and was never edited on develop.
  - `FOUNDER.md` keeps Q-026 and Q-027 byte for byte.
  - Each file only one side changed equals that side's blob, and the auto-merged files keep each side's patch.
- **`b242153` (the C11 retarget) changes setup only.** No assertion line moved. Under it the off case now really runs with animations off: without the call it goes red (426 px).
- **`61e96db` fixes a real merge interaction, red first.** Reverting the one argument reproduces the developer's red exactly (589 px), and the fix is green. `6a6e82d` changes KDoc only.
- **My Check is green.** `./scripts/check.sh --wait --max-workers=4 --rerun-tasks` exits 0, with 1,665 tests and 0 failures. Every named test of RM, home-chip-arrival and this branch passes.
- **No blocking finding.** There are three informational items, and the residue is reproduced (547 px), with a non-binding view below. **VERDICT: PASS.**

## Outputs
- `.planning/reports/colour-vision/code-auditor-merge-home.md` — this report, the only file written, committed alone. No source file was edited. The probes ran in a throwaway `git worktree add --detach` outside the repo, which is now removed.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-cv`. The probe worktree was `<scratchpad>/cv-audit/probe`. No emulator was started.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0) — older, so the plugin copies were read.
- `git log --format='%H %P %s' 904d735^..6a6e82d --ancestry-path` — pass. `904d735` has parents `18981ba` and `305f741`, and `b242153`, `61e96db` and `6a6e82d` follow it in a line. `git merge-base develop HEAD` gives `305f741`, and develop is at `305f741`.
- `git merge-tree --write-tree 18981ba 305f741` — exit 1, so the merge does conflict, as the developer reports.

**One-sided files survive the merge.**
- I computed each side's files against the merge-base `d21ad07`. For every file only develop changed, the blob in `904d735` equals `305f741`'s, and for every file only the branch changed, it equals `18981ba`'s.
- The script printed no mismatch — pass.

**Both-sided files**, which are the five conflicts plus six auto-merged files:
- `git patch-id --stable` compares each side's patch from the merge-base with the merge's patch from the other parent.
  - These are equal on both sides, so pass: `GameScreen.kt`, `Clay.kt`, `strings.xml`, `GameFlowTest.kt` and `GameScreenSoundTest.kt`.
  - `SettingsScreen.kt` differs only because adjacent hunks fuse into one diff hunk. Read by hand in `--cc`: develop's "Both switches are live …" paragraph and this branch's "three toggle rows … colour-blind palette" are both present — pass.
- **`git show --cc --format= 904d735`**, read hunk by hunk (195 lines):
  - `SettingsRepository.kt`, `SettingsUiState.kt` and `TutorialScreen.kt` follow RM §3's resolution: develop's text, plus this branch's fourth flag and `colorblindPaletteOn`, plus "in the active palette". No "no audio engine yet" survives. `CompletionStar` runs `LaunchedEffect(Unit)`, an unconditional `animateTo` and `StarGlyph` — pass.
  - `BoardCanvas.kt`: develop's `geometrySize` capture follows `rememberBoardLook(...)`. Develop's dropped palette block is unchanged between `d21ad07` and `305f741` (`git diff d21ad07 305f741 -- …/BoardCanvas.kt` has only the `geometrySize` and `frameGeometry` hunks), and `BoardLook.kt` builds every field it built — pass.
  - `FOUNDER.md`: the Q-026 block is identical to `18981ba`'s and the Q-027 block to `305f741`'s (awk extraction and comparison) — pass.
- **RM §3 conditions on `6a6e82d`:**
  - `git grep -n -i 'audio engine' -- app/src/main` matches only `LocalSoundPlayer.kt:19`, `SoundCue.kt:46` and `AudioModule.kt:16`, all unrelated audio KDoc. No settings KDoc matches — pass.
  - `git grep -n -E '(LaunchedEffect|DisposableEffect|produceState)\([^)]*[Aa]nimations?Enabled' -- app/src/main` finds only `HomeBandColumn.kt:273`, the deviation the C11 ruling §3 recorded — pass.
  - I also read every effect in the ten gate-calling files by hand. None other is keyed on a gate-derived value — pass.
  - `git grep -c 'rememberAnimationsEnabled()'` gives 16 lines in 10 files, the same as `305f741` — pass.
- `git grep -n -E 'Settings\.Global|ContentObserver|contentResolver' -- app/src/main` finds one KDoc line, `Clay.kt:217` — pass.
- **The C11 grep on `6a6e82d`**, `git grep -n -E 'ANIMATOR_DURATION_SCALE|Settings\.Global' -- app/src/test`: the only write is in `AnimationsEnabledGateWithoutScaleTest`, which is on purpose. The rest are KDoc lines in `SystemMotionScale.kt`, `GameScreenSoundTest.kt` and `TutorialSoundTest.kt` — pass.
- `git show --format= b242153`, read in full across seven files:
  - each file gains `private val motion = SystemMotionScale()` above the rule, and the rule becomes `createComposeRule(effectContext = motion)`;
  - the `Settings.Global` write becomes `motion.turnAnimationsOff()`;
  - `BoardMarkMotionTest`'s `show(animationsOn)` leaves the scale at `ON` for its on case, which is equivalent to the old `1f` write;
  - unused imports and constants are removed;
  - no `-`/`+` line touches an `assert…`, `isEqualTo` or `that(` call — pass.
- **The `61e96db` diff:**
  - `BoardLook` gains `recipe`, and the frame geometry takes it;
  - the default recipe of `BoardGeometry.forCanvas` is `BoardMarkPalette.Standard.recipe` (`BoardGeometry.kt:118`), so read-only boards and the standard palette draw exactly as on develop;
  - the new case's state has no hint, so it pins the ring radius alone — pass.
- **The commit-tree rework:** for each pair (`e75f8e0`→`904d735`, `cb457b9`→`b242153`, `207e9ce`→`61e96db`, `a0d8065`→`6a6e82d`), `git rev-parse <old>^{tree}` equals `<new>^{tree}`. The merge's parents are kept, and the author and committer dates are equal.
  - `git branch -a --contains e75f8e0` prints nothing, so no ref holds the old commits.
  - The branch has no upstream.
  - The subjects are 68, 69, 72 and 66 characters. Each commit has exactly one `Co-Authored-By: Claude Fable 5.1` line as its last line and no `Claude-Session` line.

  Pass.
- **Suppression greps:**
  - `git diff 904d735 6a6e82d -- app domain | grep -nE '^\+.*(@Suppress|@SuppressLint|@Ignore|!!|ktlint-disable|detekt:|@Disabled|tools:ignore|baseline)'` exits 1;
  - the same over `git show --cc --format= 904d735` exits 1.

  So there is no new suppression or baseline — pass.
- **Brief §11, re-run against the new base `305f741`:**
  - CV-06: `drawCandyPeg` and `drawDimple` have no hunk.
  - The `--stat` over `ui/home`, `ui/backdrop`, `domain`, `data/cloud`, `platform` and the twelve frozen files is empty.
  - `strings.xml`/`dimens.xml` have no removal.
  - Tripwire line 1 prints `MainActivity.kt`, `AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt` and `Theme.kt`; line 2 prints nothing (exit 1).
  - `Clay.kt`'s diff against `305f741` is the `gradientFill` extraction only.
  - There is no diff in `*.gradle.kts`, `gradle/`, `build-logic`, `THIRD_PARTY.md` or the manifest.

  Pass, and the 15e sitting is still owed.
- `git diff 305f741 -- app/src/main | grep … Icons.Filled.Star|goldGradientFill|StarBorder|tertiary`, over develop's own additions — no new star or tertiary path that the palette would need to own — pass.
- **`./scripts/check.sh --wait --max-workers=4 --rerun-tasks` at `f535aea`** — **pass**: `BUILD SUCCESSFUL in 13m 1s`, `104 actionable tasks: 104 executed`, `CHECK-EXIT=0`. The worktree was clean.
  - JUnit XML, every suite with 0 failures, 0 errors and 0 skipped:
    - `:app` 141 suites, 1,314 tests;
    - `:domain` test 294; fuzz 1; depthSweep 1;
    - `:build-logic` 55.
  - Lint (`lint-results-debug.xml`): 64 `severity="Warning"` and 0 errors.
  - Log: 53 `w:` lines. 52 are from `src/test`, matching the developer's 52, and 1 is the Gradle notice "Detected multiple Kotlin daemon sessions". 0 are from `src/main`, and there are 0 `e:` lines.
- **Named tests, read from that run's XML** (tests/failures):
  - **RM §3:**
    - `GameScreenCueRotationTest` 4/0, `GameScreenSoundTest` 13/0, `GameFlowTest` 8/0, `GameScreenHintNoticeTest` 20/0;
    - `BoardCanvasWrongTapLiveMotionTest` 2/0, `BoardCanvasWrongTapReducedMotionTest` 6/0;
    - `AnimationsEnabledGateTest` 2/0, `AnimationsEnabledGateWithoutScaleTest` 1/0;
    - `ResultsStarsLiveMotionTest` 1/0, `ResultsCelebrationSoundTest` 7/0;
    - `TutorialHapticsTest` 4/0, `TutorialSoundTest` 5/0, `TutorialCompletionStarLiveMotionTest` 1/0.
  - **home-chip-arrival:**
    - `BoardCanvasFirstFrameTest` 3/0, `HomeChipArrivalTest` 13/0, `HomeRibbonShapeTest` 12/0;
    - `ElapsedTimeSpeechTest` 1/0, `WindowThemeApi35FilesTest` 2/0, `WindowThemeTest` 4/0;
    - `GameStatusStripTest` 10/0, `HomeLayoutTest` 17/0, `HomeStageTest` 10/0.
  - **This branch:**
    - `BoardCanvasPaletteTest` 3/0, `ColorblindPaletteLiveSwitchTest` 1/0, `StarGlyphTest` 4/0;
    - `HomePaletteIdentityTest` 2/0, `ResultsStarPaletteTest` 2/0, `CatalogStarPipPaletteTest` 2/0;
    - `TutorialPipPaletteTest` 3/0, `BoardMarkMotionTest` 2/0, `AppThemeRootTest` 3/0, `SettingsScreenTest` 6/0.
  - Talkback, licences and candy are not on `develop` (`git log --merges 305f741` names none), so brief §10.3's pairs for them do not apply to this merge-in.

**My probes.** None was committed. All ran with `./gradlew --max-workers=2 :app:testDebugUnitTest --tests …` in the throwaway worktree.
- **Run 1, on `6a6e82d` with three mutants, each aimed at a disjoint class:** 6 tests, 3 failed, as predicted.
  - **M1** reverts `61e96db`'s argument: `BoardGeometry.forCanvas(spec, size)`, with `val recipe = look.recipe` removed. `BoardCanvasFirstFrameTest.theFirstFrameOfAColourBlindBoardRingsTheHeldPegAsTheSettledFrameDoes` goes red with "pixels inside board_canvas that differ between the first frame and the settled one, expected: 0, but was: 589". That reproduces the developer's red exactly; the other two cases stay green.
  - **M2** restores this branch's pre-merge `CompletionStar` body, `LaunchedEffect(animationsEnabled) { if (animationsEnabled) animateTo }`, which is the hunk the merge resolved away. `TutorialCompletionStarLiveMotionTest` goes red: "the star 2 frames after animations turn off, expected: 639, but was: 165". RM's guard still holds over the merged code.
  - **M3** deletes `b242153`'s `if (!animationsOn) motion.turnAnimationsOff()` in `BoardMarkMotionTest.show`. The off case goes red: "pixels that changed in one frame with animations off, expected: 0, but was: 426". The retarget's one line is what makes that case green.
- **Run 2, on `6a6e82d` plus two temporary cases** (the new case's scene with the hint left on): 8 tests, 1 failed.
  - Colour-blind: red, 547 px — the residue, reproduced.
  - Standard: green.
  - The three real classes: green.
- **Run 3, on `904d735` alone, the seven retargeted classes with their old `Settings.Global` setup:** 15 tests, 1 failed. `BoardMarkMotionTest.withAnimationsOffAColourBlindBoardIsStillFromOneFrameToTheNext` is red at 426 px. The other six are green with or without the retarget. See I-1.
- `git worktree remove --force <probe>`; `git worktree prune` — the worktree is removed, and `git status --short` in the review worktree is empty.

## Findings
No blocking finding, and no suppression to approve or reject.

**Informational (no action required for the merge):**
- **I-1** — `.planning/reports/colour-vision/android-game-dev-merge-home.md` (Checks, probe P2). The merge commit `904d735` is not green on its own: `BoardMarkMotionTest`'s off case fails at 426 px under its old `Settings.Global` setup (run 3), and `b242153` fixes it.
  - This is the C11 ruling's foreseen "loud case at the merge-in". It is fixed in the next commit, and the developer's Checks ran only after it.
  - The report describes P2 as a stepped-frames probe, but the unmodified case was already red at the merge. Bisecting through `904d735` will meet this red.
  - Recorded so that no one reads the merge commit as a green point.
- **I-2** — `app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasPaletteTest.kt:65`, and the same `@Before` in `CatalogStarPipPaletteTest`, `ColorblindPaletteLiveSwitchTest`, `HomePaletteIdentityTest`, `TutorialPipPaletteTest` and `ResultsStarPaletteTest`. In these six suites the `turnAnimationsOff()` setup is not what makes them pass: they are green under the old setup (run 3), under the developer's P1 gate probe, and under the retarget.
  - The retarget is still right: the C11 rule is to state the off condition through the mechanism that reaches it.
  - But none of the six would catch a palette defect that shows only with animations off. The off-path coverage on this branch is `BoardMarkMotionTest` (M3) and the first-frame case.
  - No change is required. The tech-lead may want this in its §8.1 census note.
- **I-3** — `.planning/reports/colour-vision/android-game-dev-merge-home.md`, Open issue 2. The stated reason for not fixing the residue, that brief §3.2 rules out building strokes in the draw lambda, is stronger than the code supports.
  - Develop's own first-frame fallback, `BoardCanvas.kt:229` (`… else BoardGeometry.forCanvas(spec, size, recipe)`), already allocates a whole `BoardGeometry` inside the draw lambda on exactly the mismatched frames.
  - Its comment's guarantee is "in steady state … nothing is allocated", not "never".
  - See the view below. The ruling is the tech-lead's.

**Reviewed and passing** (the code-reviewer skill's positives):
- The merge drops develop's inline palette block in favour of `BoardLook` only after the fields were confirmed equal (`markStroke`, `mark`, `ringWidthPx`).
- `61e96db` fixes the merge's semantic interaction at its root: the recipe travels with the geometry it built. It is not patched at the call site, and the standard path stays byte-identical.
- The commit-tree rework keeps trees, parents and dates, and the developer proved tree equality for every pair.
- Security: the range touches UI drawing, tests, KDoc and a planning file only. There is no network, secret, permission, input path or dependency.
- Performance (compose-performance-audit): `BoardLook` gains one reference field. No new allocation in composition or in a steady-state draw. The frame-geometry allocation is develop's, confined to mismatched frames.

## View on the open hint-ring residue (non-binding; the tech-lead rules)
- **What it is** (run 2):
  - On the first frame, or a resize frame, of an interactive colour-blind board with a hint showing, 547 px differ from the settled frame.
  - The two hint strokes (`BoardLook.kt:58-65`) are remembered for the composed geometry's radii, and on a first frame that geometry is zero-sized.
  - `dashedRingStroke`'s interval is then `2π·0 / (n·2) = 0` (`DashedRing.kt:19`), a zero-length dash pattern. On a resize frame the dash length belongs to the old radius instead.
  - The rings are positioned by the frame geometry (right place, right radius), but their dash pattern is wrong for one frame.
  - The standard palette's solid rings carry no pattern, so they are clean.
  - Develop's wrong-tap `markStroke` has the same property.
- **Severity: low, and not a merge condition in my view.**
  - It lasts one frame, about 16 ms, and only with a hint up as the board first draws or resizes.
  - No state is wrong.
  - The non-colour cue it degrades, the dash, is back on the next frame.
  - Nothing a TalkBack or reduced-motion user depends on changes.
  - I did not establish what a zero-interval pattern draws on a device's hardware renderer. Whatever it draws, it lasts one frame.
- **If fixed:** the fix belongs inside the existing mismatch branch. When `frameGeometry !== geometry`, the draw would build the two hint strokes, and `markStroke`, for the frame geometry, and otherwise take the remembered ones.
  - That allocates only on the frames that already allocate a `BoardGeometry`, which keeps §3.2's purpose: no steady-state allocation in the draw lambda.
  - It clears develop's `markStroke` twin in the same move.
  - Run 2's colour-blind case is the red test for it.
  - It touches `ui/components/BoardCanvas.kt`, which is not on §10's start-path list, so no new sitting would follow from it alone. It does need a review loop.
- **My lean:**
  - Record it as a low 3.0.1 item, beside the `markStroke` twin, with this probe as its named test, unless the branch gets another developer round for other reasons. In that case, fold it in.
  - It is not worth a dedicated loop before the 15e sitting under the 3.0.0 week (founder directive 3).

## Open issues / risks
1. **Q-019.** This review ran on Opus, the developer's model. My own Check (`--rerun-tasks`) and runs 1–3 are the mitigation: none of the counts above comes from the developer's report.
2. **The residue** (view above) waits on the tech-lead's ruling. Whatever it rules, the developer's Open issue 2 and this report's I-3 are the inputs.
3. **The 15e sitting is still owed.** The tripwire still prints the seven start-path files, and nothing in this range touches a start-path file by hand. The arms are A = `305f741` and B = the head, if no further code lands.
4. **QA's device pass ran on `0f1f58f`.** Whether a smoke pass is owed under founder directive 2 for develop's code plus `61e96db` is for the producer and the tech-lead.
5. **Scratchpad sharing** (the developer's Open issue 6). I wrote only under a dedicated `cv-audit/` subdirectory, and my Check log was not overwritten.

## Handoff
Next: tech-lead — re-read the merge's conflict hunks (brief §10.3; RM sign-off §3). They match RM §3, and both sides survive (Checks). Rule on the residue (view above; I-3). Sign off after QA's smoke pass, if the producer schedules one, and the 15e sitting.
Next: qa-engineer — the 15e sitting on the bench per brief §9, with arm A `305f741` and arm B the head after this report's commit (code tree = `6a6e82d`), last before the sign-off.
Next: producer — route the residue ruling, Open issue 4 and I-1. No developer action is required by this review.

VERDICT: PASS
