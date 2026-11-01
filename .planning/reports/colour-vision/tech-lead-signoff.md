# tech-lead report — colour-vision (final technical sign-off)
Date: 2026-11-01
Branch: feature/colour-vision (report only). It signs off `305f741..a09a0fd`. The code head is `fddc8e8`: `git diff --quiet fddc8e8 a09a0fd -- . ':(exclude).planning'` exits 0. `develop` is `305f741`, which is the merge-base.
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, stacks, testing and dispatch. logging, puzzle-design and play-store are identical to the snapshot already loaded. Also the snapshot's `.claude/rules/lessons.md`.
Ran in parallel with: talkback developer fixes
Ran on: claude-opus-5-5 at effort high (policy). The pin is fable at xhigh. Q-019 puts every Fable-pinned role on Opus until 3.0.0 ships, and the founder's 2026-10-31 directive (1) runs each dispatch one step below its pin. The harness names no effort.
Fan-out: none
Loops: 0
Read in full: `colour-vision/tech-lead-merge-ruling.md`, `code-auditor.md` (loop 1), `code-auditor-merge-home.md`, `code-auditor-hint-ring.md`, `qa-engineer.md`, `qa-engineer-15e.md`. Read in part: `tech-lead-brief.md` §9–§12, the Summary, Open issues and Handoff of `android-game-dev-merge-home.md` and `android-game-dev-hint-ring.md`, `reduced-motion-live-gate/tech-lead-signoff.md` §3, `home-chip-arrival/tech-lead-device-ruling.md` §2, and `feature/licences-screen:.planning/reports/licences-screen/tech-lead-signoff.md` (its 15e and merge-order sections).
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Every merge condition holds.** The Check is green on the head. The code-auditor passed loop 1, the merge-in and the hint-ring delta. QA passed the device pass and the smoke. The 15e sitting is a PASS, with a delta of −4.0 ms against a 100 ms threshold. The `--cc` hunks were re-read in Ruling 3, and `fddc8e8` meets Ruling 1's constraints 1–6.
- **Sitting attempt 2 stands** under the home-chip forward rule. Attempt 1 was voided by an off-device cause (another worktree's `depthSweep`), and attempt 2 met every clause of the rule.
- **No further merge-in is owed.** `develop` is the merge-base, so the merge is conflict-free and its tree equals the head's. `feature/licences-screen` must merge `develop` in after this branch lands. It conflicts in `SettingsScreen.kt` and `SettingsScreenTest.kt` (the renumbering brief §10.3 foresaw), so its sign-off's "no merge-in owed" premise lapses (Handoff).
- **Open items placed:** CV-AC14 is a non-blocking QA follow-up. The two develop stroke items become 3.0.1 low items. SN-1 goes to the ux-designer. CV-AC10's partial device half is accepted.
- **VERDICT: PASS.**

## The conditions, one by one

| # | Condition (brief §12, merge ruling, RM sign-off §3) | Evidence | Met |
|---|---|---|---|
| 1 | The Check is green on the final code tree | Mine at `a09a0fd`: exit 0 (Checks) | yes |
| 2 | Code-auditor PASS on the branch's diff | `.planning/reports/colour-vision/code-auditor.md` (loop 1; loop 0 is `code-auditor-loop0.md`) | yes |
| 3 | Code-auditor PASS on the develop merge-in (`904d735..6a6e82d`) | `.planning/reports/colour-vision/code-auditor-merge-home.md` | yes |
| 4 | Code-auditor PASS on the Ruling 1 fix (`fddc8e8`) | `.planning/reports/colour-vision/code-auditor-hint-ring.md`. Red reproduced at 547 px, and the M1 and M3 probes are killed | yes |
| 5 | QA's device pass | `.planning/reports/colour-vision/qa-engineer.md`: PASS, with no product defect | yes |
| 6 | QA's smoke on the merged head (Ruling 2) | `.planning/reports/colour-vision/qa-engineer-15e.md`, Smoke pass: 8 of 8 steps, no FATAL or ANR, on `ergoz_studio` | yes |
| 7 | The 15e sitting (brief §9) | `qa-engineer-15e.md`: attempt 2 PASS, delta −4.0 ms, floor 4.0 ms, threshold 100 ms; 9 of 9 preconditions held; key count 0 at both ends; `app/schemas` shows no diff | yes |
| 8 | Arm B is the code being merged | QA's arm B is `155045e`, whose code equals `fddc8e8`, and so does `a09a0fd` (the only file added since is QA's report). Arm A `305f741` is still `git merge-base develop HEAD` | yes |
| 9 | The `--cc` hunks of `904d735` were re-read (brief §10.3) | `tech-lead-merge-ruling.md` Ruling 3, with `6a6e82d` for the two stale counts. The auditor's patch-id and one-sided-blob checks agree | yes |
| 10 | RM sign-off §3: the KDoc resolution, no "no audio engine yet", no gate-keyed effect | Re-checked on the head (Checks): `CompletionStar` runs `LaunchedEffect(Unit)` and an unconditional `animateTo` through `StarGlyph`. The only gate-keyed effect is `HomeBandColumn.kt:273`, the C11 deviation already recorded | yes |
| 11 | RM sign-off §3: the named tests for the second branch to merge | All 13 classes are green in my Check's XML (Checks) | yes |
| 12 | Ruling 1, constraints 1–6, re-read on `git show fddc8e8` | Below | yes |
| 13 | Brief §11: what must not change | Re-run on the head (Checks) | yes |

**Ruling 1's fix, re-read against `git show fddc8e8`.**
1. **Files.** `BoardCanvas.kt`, `BoardLook.kt` and `BoardCanvasFirstFrameTest.kt` only.
2. **One builder.** The frame path calls `hintRingStroke(radius, palette.ringWidthPx, hintRingDashes)`. `hintRingDashes` is `recipe.hintRingDashes`, and `recipe` is `look.recipe` (`BoardCanvas.kt:114`). These are the same builder, width and dash count as `BoardLook.kt:61,65`.
3. **Only where it is drawn.** `restroke = unsizedFrame && palette.hintRingsDashed` sits inside `drawHintHighlight`. That function is reached only under `interactive && hintSource != null && hintTarget != null`, so Home's read-only boards never reach it.
4. **No steady-state allocation.** On a matched frame, `restroke` is false and both rings take the palette's strokes. The new arguments are a `Boolean`, a reference already captured by the lambda, and an `Int` read. As the auditor's I-1 notes, no test can see this; I read it to the same result.
5. **Wrong-tap `markStroke` untouched.** The diff has no `markStroke` or `drawWrongTapMark` line.
6. **No suppression, `!!` or new constant.** `BoardCanvas`'s one `@Suppress("LongMethod")` is unchanged. Main source has 12 `@Suppress` on both the head and `develop`.

**The amended §3.2 holds in the code.** The KDocs of `rememberBoardLook` and `hintRingStroke` now state the amended rule exactly: no rebuild on a sized frame, and only the dashed hint strokes on an unsized one.

## Does sitting attempt 2 stand? Yes
Measured against `home-chip-arrival/tech-lead-device-ruling.md` §2's forward rule, clause by clause:
- **The cause was off the device.** A Gradle worker from `PegSolitarie-lic/domain` (`-DRUN_SWEEP=true`) ran at ≈100 % CPU from 05:11:16, a concurrent build. QA records no crash, ANR, dialog, foreign install or settings change, and `Data-Free` stayed ≥ 4 GB.
- **The bench state was re-verified before attempt 2.** `bench-state-pre-attempt2.txt` shows the settings unchanged and the installed sha256 recorded. `db-pre-attempt2/` is byte-identical to `db-start/`, and the key count was 0.
- **The launcher was idle ≥ 3 minutes:** about 8 min 46 s. The before-`uptime` was read again (`1.54 2.75 3.40`).
- **Attempt 2 ran its own null control:** floor 4.0 ms, against the 150 ms VOID line.
- **The evidence names the boot (05:06:11) and the attempt before it on that boot.**
- **No outcome was selected.** The void was declared by `run.sh`'s monitor (30 of 38 lines not quiet), not by the numbers. Attempt 1's numbers computed no FAIL either (delta −6.0 ms).

Attempt 2's own monitor read 0 of 37 lines not quiet, and exactly one emulator ran on every reading. So attempt 2 is the sitting of record, and the PASS stands.

## Merge readiness and the order with licences-screen
- **This branch needs no further merge-in.**
  - `git rev-parse develop` gives `305f741`, which equals `git merge-base develop HEAD`.
  - `git log 305f741..develop` is empty.
  - `git merge-tree --write-tree develop HEAD` exits 0, with tree `a825f48`, which is the head's own tree.
  - The `--no-ff` merge therefore adds nothing the Check has not run on. If `develop` moves before this merge, this sign-off no longer covers it (Handoff).
- **Licences after this branch is the right order.**
  - Licences edits `navigation/` and `di/`. Had it landed first, this branch would have had to take it in, which by brief §9's merge-in rule voids the sitting it has just passed.
  - Landing second leaves this branch's sitting intact. It also keeps licences before candy's sitting, as its own sign-off requires.
- **Licences then owes a merge-in, not a straight merge.** `git merge-tree --write-tree HEAD feature/licences-screen` (`326291c`) exits 1:
  - `SettingsScreen.kt` has 3 hunks: the order KDoc's two sentences and the constant block. They need brief §10.3's whole renumbering on top. Contiguous 0–13 with both rows: `COLORBLIND_ROW_ORDER` 4, Play Games 5–8, About 9, Licenses 10, Privacy 11–13, with one KDoc sentence naming both insertions.
  - `SettingsScreenTest.kt` has 3 hunks, one of them about 150 lines, which need keep-both.
  - Licences' sign-off states "`develop` is at `305f741` … no further merge-in is owed". That premise lapses the moment this branch lands.

## Open items, placed (tech-lead decisions)
1. **CV-AC14 (system colour correction) is not run. It is not a merge condition.** The spec makes its failure non-blocking, and §5's form cues do not depend on hue. It is owed as a QA follow-up: in the 3.0.0 release device pass if it fits that session, otherwise as a 3.0.1 QA item. A failure goes to the ux-designer through the producer.
2. **CV-AC10's device half is partial (QA open issue 2). Accepted as sufficient.**
   - The branch's main-source diff adds no spoken semantics. Every star `Icon` it replaces had `contentDescription = null`, and `StarGlyph`'s three do too.
   - The one new key, `BoardMarkPaletteKey`, is a test key that TalkBack does not read.
   - `ColorblindPaletteLiveSwitchTest` holds the keys unchanged across a flip, and QA's palette-on walk read the same strings as the base.
   - No further TalkBack walk is owed for this branch.
3. **3.0.1 low item CV-a: develop's per-frame `Stroke` in `drawSelectedPeg`** (`BoardCanvas.kt:583`; on develop since `d21ad07`).
   - It allocates one `Stroke` per frame while a peg is held. The cost is GC churn, and it is not a visible defect.
   - The remedy is to remember a solid `Stroke(ringWidthPx)` once in `BoardLook`, beside `haloStroke`, and draw with it.
   - There is no pixel red: the auditor reads it, as for constraint 4. It is not cut-blocking.
4. **3.0.1 low item CV-b: the wrong-tap `markStroke` on an unsized frame** (the merge ruling's Open issue 1; the auditor's merge-home view).
   - Probe first, on a resize frame and a first frame with a mark up.
   - If it is red, apply Ruling 1's remedy inside the same `unsizedFrame` branch, through `wrongTapMarkStroke`.
   - CV-a and CV-b go in one brief, one developer round and one auditor delta. They touch `BoardCanvas.kt` and `BoardLook.kt` only, neither of which is on the start-path list, so no sitting follows.
5. **SN-1 (two-star Results unreachable) goes to the ux-designer through the producer. Non-blocking.** It is a spec correction to CV-AC11's "Results with two of three stars", because Results follows only `GameStatus.Won`, and Won (`pegCount ≤ minimumPegs`) is the 3-star band.
   - This branch changes neither fact.
   - Whether a one- or two-star Results should be reachable at all is a gameplay question for the game-designer, if it is not already ruled. It is not a technical item.
6. **The resize-frame half of Ruling 1 is unpinned by a test** (auditor I-2; developer Open issue 2). Accepted: the first-frame case drives the same `geometrySize != size` branch, and M1 and M3 pin each ring's radius.
7. **The merge commit `904d735` is not green on its own** (auditor I-1): `BoardMarkMotionTest` is red at 426 px until `b242153`. It is recorded so that no bisect treats it as a green point.
8. **Staffing (Q-019).** The author, the auditor, QA and this sign-off all ran on Opus. The mitigations are recorded in each report: forced Checks, reproduced reds, mutation probes, device captures, and here the repo-level re-checks below. A superior-model review stays owed whenever Fable reviews resume, and the producer decides that.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/tech-lead-signoff.md`: this sign-off. It is the only file written, and no product code was touched. The dispatch named this path; brief §12's `tech-lead.md` is superseded by it.
- My Check's log is in the session scratchpad. It is not committed.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-cv`. No emulator was started and no APK was built.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0), and `diff -q` of each plugin rule against the snapshot — 9 differ, and those were read from the plugin — pass.
- `uptime` at the start: 1-minute load 5.23, then 9.53. `.gradle/ergoz-build.lock` absent, and `git status --short` empty — pass.
- `./scripts/check.sh --wait --max-workers=4` at `a09a0fd` — **pass**:
  - `BUILD SUCCESSFUL in 11m 15s`, `104 actionable tasks: 11 executed, 93 up-to-date`, `CHECK-EXIT=0`;
  - lint, the literal checks, `koverVerify` and `depthSweep` executed;
  - the compile and unit-test tasks were UP-TO-DATE against the auditor's `--rerun-tasks` run at `fddc8e8`, whose code is identical. Gradle's input hashes certify that, and its XML is this tree's result;
  - 0 `w:` and 0 `e:` lines in this run, since nothing recompiled. The auditor's figure on the same tree is 52 `w:`, all in `src/test` and 0 in main.
- JUnit XML, every suite with 0 failures, 0 errors and 0 skipped — pass:
  - `:app` 141 suites, 1,315 tests;
  - `:domain` 33 suites, 294 tests; fuzz 1; depthSweep 1 (written 05:46:17, this run);
  - `:build-logic` 6 suites, 55 tests.
- Named tests (tests/failures) — pass:
  - **Ruling 1:**
    - `BoardCanvasFirstFrameTest` 4/0, `BoardCanvasPaletteTest` 3/0, `BoardMarkMotionTest` 2/0;
    - `ColorblindPaletteLiveSwitchTest` 1/0, `HomePaletteIdentityTest` 2/0, `HomeChipArrivalTest` 13/0;
    - `BoardCanvasWrongTapReducedMotionTest` 6/0, `BoardCanvasWrongTapLiveMotionTest` 2/0, `BoardCanvasTest` 17/0.
  - **This branch:**
    - `StarGlyphTest` 4/0, `ResultsStarPaletteTest` 2/0, `CatalogStarPipPaletteTest` 2/0;
    - `TutorialPipPaletteTest` 3/0, `SettingsScreenTest` 6/0, `AppThemeRootTest` 3/0, `ColorblindPaletteContrastTest` 3/0.
  - **RM §3:**
    - `GameScreenCueRotationTest` 4/0, `GameScreenSoundTest` 13/0, `GameFlowTest` 8/0, `GameScreenHintNoticeTest` 20/0;
    - `AnimationsEnabledGateTest` 2/0, `AnimationsEnabledGateWithoutScaleTest` 1/0;
    - `ResultsStarsLiveMotionTest` 1/0, `ResultsCelebrationSoundTest` 7/0;
    - `TutorialHapticsTest` 4/0, `TutorialSoundTest` 5/0, `TutorialCompletionStarLiveMotionTest` 1/0.
- `git show --stat` and `git show --format= fddc8e8 -- app/src/main`, read in full against constraints 1–6 — pass.
- `grep -n "val recipe\|val look\|geometrySize\|Stroke(" BoardCanvas.kt BoardLook.kt` — the frame path and the `remember` share `hintRingStroke` and `look.recipe`. The one remaining per-frame `Stroke(` is `BoardCanvas.kt:583` (item 3) — pass.
- `git diff --quiet fddc8e8 HEAD -- . ':(exclude).planning'` exits 0, and `git diff --stat 155045e HEAD` lists only `qa-engineer-15e.md` — pass (arm B is this code).
- Tripwire, brief §11: line 1 prints `MainActivity.kt`, `AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt` and `Theme.kt`; line 2 prints nothing (exit 1) — pass, and the sitting was owed and was sat.
- Brief §11's frozen surfaces against `develop...HEAD` — pass:
  - `--stat` over `ui/home`, `ui/backdrop`, `domain`, `data/cloud`, `platform`, `navigation`, `di`, `*.gradle.kts`, `gradle`, `build-logic`, `THIRD_PARTY.md`, the manifest and `app/schemas` is empty;
  - each of the twelve frozen files (`GameActionDock.kt` … `ProgressPersistenceKeys.kt`) has 0 hunks;
  - the CV-06 grep (`drawCandyPeg`, `drawDimple`) exits 1;
  - `strings.xml` and `dimens.xml` have no removed line (exit 1).
- Suppressions over `git diff develop...HEAD -- app domain`: the only added line is the pre-existing `@Suppress("LongMethod")` with its comment edited, approved at loop 0. The `@Suppress` count in main is 12 on both the head and `develop` — pass.
- New hexes in main are only in `ui/theme/Color.kt` (lines 105 and 111). No `N.dp` or `N.sp` literal was added — pass.
- RM §3 on the head — pass:
  - `git grep -i 'audio engine' -- app/src/main` matches only three unrelated audio KDocs;
  - the gate-keyed-effect grep finds only `HomeBandColumn.kt:273`;
  - `CompletionStar` (`TutorialScreen.kt:360-371`) has `LaunchedEffect(Unit)`.
- Semantics over `git diff develop...HEAD -- app/src/main/java`: removed and added `contentDescription` lines are all `null`, and the one new key is `BoardMarkPaletteKey` — pass (item 2).
- `git rev-parse develop` and `git merge-base develop HEAD` both give `305f741`. `git merge-base --is-ancestor develop HEAD` exits 0, and `git log 305f741..develop` is empty — pass.
- `git merge-tree --write-tree develop HEAD` exits 0, and its tree `a825f48` equals `HEAD^{tree}` — pass.
- `git grep -E '^(<<<<<<<|>>>>>>>|=======$)' -- app docs domain` exits 1 — pass.
- `git merge-tree --write-tree --name-only HEAD feature/licences-screen` exits 1, with conflicts in `SettingsScreen.kt` (3 hunks) and `SettingsScreenTest.kt` (3 hunks), read — recorded for the producer.
- `git diff --name-only develop...feature/licences-screen`: licences touches `navigation/` and `di/` (start path) — read; this is why licences goes second.
- `.planning/log/` was not read.

## Open issues / risks
1. **This sign-off covers the merge only while `develop` is `305f741`.** If any commit lands on `develop` first, the branch merges it in. The Check and the other branch's named tests then re-run, and a tech-lead re-reads the hunks. A start-path commit (licences' `navigation/` or `di/`) also voids the sitting (brief §9).
2. **Licences-screen's merge-in after this branch** (Merge readiness). Licences' own sign-off does not cover a hand-resolved merge.
3. **Docs owed to me after the merge,** at the close sync, together with the merge ruling's Open issue 3:
   - `docs/tech-design.md` §3: the root collection, `LocalBoardMarks`, and the composed-versus-frame split for strokes (amended §3.2);
   - §10: this sitting's record, brief §9's key-validity reading, the smoke on `ergoz_studio`, and the forward rule applied to attempt 2;
   - §14.1: row 6b closed;
   - §16: CV-a, CV-b and the CV-AC14 follow-up.
4. **Q-019 staffing,** as in item 8.

## Handoff
Next: producer —
1. **Before merging,** confirm `git rev-parse develop` still prints `305f741`. If it does not, do not merge on this sign-off (Open issue 1).
2. **Merge** with `git merge --no-ff feature/colour-vision` into `develop`, in slot 6b, before candy-backdrop's sitting.
3. **Then route licences-screen's merge-in** (`git merge develop` on `feature/licences-screen`, never a rebase):
   - **The conflicts:** the whole renumbering to 0–13 as above, and keep-both in `SettingsScreenTest.kt`.
   - **Its Check,** with these classes green:
     - this branch's `BoardCanvasPaletteTest`, `ColorblindPaletteLiveSwitchTest`, `StarGlyphTest`, `HomePaletteIdentityTest`, `ResultsStarPaletteTest`, `CatalogStarPipPaletteTest`, `TutorialPipPaletteTest` and `SettingsScreenTest`;
     - licences' rendered-top cases.
   - **Reviews:** a code-auditor review of the hand hunks, and a tech-lead re-read of the `--cc` before its merge.
   - **Timing:** it lands before candy's sitting.
4. **STATUS.md:** record CV-a, CV-b (3.0.1 low, one brief, chain tech-lead → android-game-dev → code-auditor → tech-lead), the CV-AC14 QA follow-up, and SN-1 to the ux-designer (non-blocking).
5. **Delete `.planning/qa/colour-vision/`** after the Done line (rules/testing.md).
6. **Schedule the tech-design close sync** (Open issue 3).

Next: ux-designer — SN-1 (`qa-engineer.md`, Bugs), through the producer.

VERDICT: PASS
