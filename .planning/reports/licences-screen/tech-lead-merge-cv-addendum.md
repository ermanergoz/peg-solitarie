# tech-lead report — licences-screen (addendum: the colour-vision merge-in re-read)
Date: 2026-11-01
Branch: feature/licences-screen (report only). It re-reads `326291c..696a83b`: the merge `7f03051` and the developer's report commit `696a83b`, which changes only `.planning/`. `develop` is at `f1c399a`, which is `1bc442f` plus one STATUS-only merge.
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies of studio, reporting, testing, compose, dispatch, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android and stacks in full, plus `.claude/rules/lessons.md`. `.planning/recall/RECALL.md` does not exist. Also android-architecture and android-gradle-logic.
Ran in parallel with: code-auditor review of `7f03051` (the same worktree; I ran no Gradle)
Ran on: claude-opus-5-5 at effort high (policy). My agent file pins fable at xhigh. Q-019 puts Fable-pinned roles on Opus until 3.0.0 is published, and the founder's 2026-10-31 directive (1) runs each dispatch one step below its pin. The harness names no effort.
Fan-out: none
Loops: 0
Read in full: `licences-screen/tech-lead-signoff.md` (mine, PASS at `326291c`), `develop:.planning/reports/colour-vision/tech-lead-signoff.md`, `licences-screen/android-game-dev-merge-cv.md`, `git show --cc 7f03051` (357 lines)
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **(1) My sign-off stands for the merged head, and no 15e sitting is owed.** The branch's start-path diff against `develop` is the same five `di/` and `navigation/` files, and their patch is byte-identical (equal `patch-id`) to the one I ruled on at `326291c`. Every colour-vision start-path file on the head equals `develop`, the tree whose sitting passed.
- **(2) The half-renumbering guard is a 3.0.1 item, not a merge condition.** The 0–13 run on the head is correct; I checked it below against every use site. No in-flight branch edits an order constant before 3.0.0. The four false "the test pins it" claims are corrected in the same 3.0.1 round, by the test that makes them true. The test is specified below, red on mutation probes.
- **(3) Before the merge:** `develop` has moved by STATUS only, so the merge is clean and needs no merge-in. Candy-backdrop then owes a merge-in that conflicts in `AppNavHost.kt` (start path) and `docs/ux/licences-screen.md`, before its sitting. No QA device step is owed for this merge-in. The 3.0.0 release pass walks Settings once with both rows.
- **VERDICT: PASS conditional on the code-auditor's PASS** on `7f03051`'s hand hunks, with its Check green on `696a83b`.

## (1) Does the sign-off stand? Is 15e still not owed?
**Yes on both.** The 15e reading in `tech-lead-signoff.md` was §10's first-frame test: a sitting is owed where code that runs before or on Home's first frame changed. Against the merged head:
- **The branch's own start-path diff is unchanged.** `git diff --name-only develop...HEAD` over the start-path list (§10 criterion 15e) prints the same five files: `di/DataModule.kt`, `di/DispatchersModule.kt`, `navigation/AppNavHost.kt`, `navigation/LicensesDestinations.kt` and `navigation/Routes.kt`. `git patch-id --stable` of `develop...HEAD` over `di/` and `navigation/` equals the patch-id of `305f741...326291c` over the same paths. The first-frame cost I ruled on (four `composable<>` registrations at graph build, unscoped bindings never called before a licences ViewModel asks) is exactly what is on the head.
- **The merge-in brought no start-path change of its own.** Colour-vision's start-path files (`MainActivity.kt`, `AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt`, `Theme.kt`) are absent from `git diff --name-only develop HEAD` over the start-path list. They equal `develop`, whose code is colour-vision's arm B, which passed its sitting (delta −4.0 ms). Colour-vision touches no `di/` or `navigation/` file (`git diff --name-only 305f741 1bc442f`), so the two sides share no start-path file.
- **The only hand-resolved files are off the start path.** The auto-merge tree of `326291c` and `1bc442f` differs from `7f03051` in `SettingsScreen.kt` and `SettingsScreenTest.kt` only.
- **The sittings around it.** Colour-vision's sitting is spent, because that branch has merged. Candy's sitting has not been held. This branch must still land before candy sits (open issue 2).

## (2) The half-renumbering guard: 3.0.1, with the specification ready
**The finding is correct.** `clayEntrance` animates `graphicsLayer` alpha and `translationY` with `delayMillis = order × 40 ms`. `SettingsScreenTest` reads `positionInRoot` after `waitForIdle()`, and with auto-advance the cascade has settled by then. The tops therefore show layout order, which is composition order, and never the stagger. The developer's probe (About 8 and Licenses 9 on `7f03051`, 12 of 12 green) agrees with that reading.

**Why it does not hold 3.0.0.**
- **The head's run is right.** I read it against every use site in `SettingsScreen.kt`:
  - General: header 0, How to play 1, Sound 2, Haptics 3, palette 4 (lines 241–268);
  - Play Games: header 5, row 6, Leaderboards 7, Achievements 8 (340–365). Row 6 is used at 350 and 375 in the exclusive `SignedIn` and `SignedOut` branches;
  - About: header 9, Licenses 10 (398–403);
  - Privacy: header 11, policy 12, choices 13 (486–516).
  - The sections compose in that order (171–187). The run is contiguous, has no duplicate, and Privacy is last.
- **The defect class is cosmetic.** A half-renumbering makes two rows enter together, 40 ms apart from the intended order. It cannot lose a row, a callback, a semantics node or a heading. Reduced motion skips the cascade entirely.
- **Nothing before 3.0.0 can break it unseen.** Of the in-flight branches, only `feature/candy-backdrop` touches `SettingsScreen.kt`. It adds transparent `Scaffold` and `TopAppBar` colours, not an order constant, and `git merge-tree` auto-merges that file against this head.
- **Interim guard:** until the 3.0.1 test lands, a branch that changes any `*_ORDER` constant in `SettingsScreen.kt` gets its run read by the tech-lead at sign-off, as above.

**The false claims are left on this branch, and the 3.0.1 test makes them true.** They are `SettingsScreen.kt:581-583`, `SettingsScreenTest.kt:32-33` and `:222-223`, `docs/ux/colour-vision.md:250` (and CV-AC01's oracle at `:585`), and `docs/ux/licences-screen.md:558`. Editing them now would put a new commit under the code-auditor's running review for comment text alone. It would also take a developer round and a ux-designer round this week, to say what the 3.0.1 test will make true anyway.

**3.0.1 item LS-g: the entrance-order guard (one developer round, android-game-dev → code-auditor → tech-lead).**
1. **Where:** a new class, `app/src/test/.../ui/settings/SettingsEntranceOrderTest.kt`, on the same Robolectric `@Config` as `SettingsScreenTest`. It is test-only, with no product hook and no new main-source key.
2. **Content:** `AppTheme { SettingsScreen(...) }` with `SettingsUiState(gamesSignInState = SignedIn(playerId = "player", displayName = null), privacyOptionsRequired = true)`, so all 14 ordered elements render. Animations stay on: the composition provides no `MotionDurationScale`, so the gate is on (`rememberAnimationsEnabled`).
3. **The oracle:**
   - Set `composeRule.mainClock.autoAdvance = false` before `setContent`.
   - Read each element's top with the existing `topOf` (`positionInRoot.y`, `useUnmergedTree = true`), by its label or header string from resources. The layer's `translationY` is part of `positionInRoot`.
   - Step `mainClock.advanceTimeByFrame()` until every top stops changing (well past 13 × 40 + 240 ms). For each element, record the first frame index at which its top left its frame-0 value.
   - Assert that those 14 first-move frames, listed in column order, are **strictly increasing**. A 40 ms stagger at 16 ms frames gives distinct frames.
   - The test reads no constant and writes no number except the frame bound, which is derived from `ClayAnim` in the test.
4. **Red first, by mutation.** On the head the test is green, because the run is correct. The red is shown on two probes, each reverted and neither committed, and is quoted in the report:
   - **(a)** `ABOUT_HEADER_ORDER = 8` and `LICENSES_ROW_ORDER = 9`, the developer's half-renumbering;
   - **(b)** `COLORBLIND_ROW_ORDER = 3`, a duplicate with Haptics.
   - Both must fail on the strict-order assertion. If a probe stays green (for example, Robolectric does not apply the layer offset to `positionInRoot`), stop and report to the producer for my ruling. Do not add a semantics key or a test tag to main source to work around it.
5. **The claims made true.** In the same commit, point the KDocs at `SettingsScreen.kt:581-583`, `SettingsScreenTest.kt:32-33` and `:222-223` to `SettingsEntranceOrderTest`. The rendered-top tests keep their real claim: layout order. The two spec rows (`colour-vision.md:250` and `:585`, and `licences-screen.md:558`) go to the ux-designer through the producer, to name the new class as the oracle. That is text only.
6. **The Check is green, and the auditor's delta review is the merge gate.** No device step and no 15e sitting are owed, because `ui/settings/` is not on the start path.
7. **Optional bundling:** this round may ride CV-a and CV-b's developer round. The files are disjoint, so the producer may also brief it alone.

## (3) Anything else before the merge
1. **`develop` moved to `f1c399a`,** which is `1bc442f` plus `.planning/reports/STATUS.md` (+7 lines) only. `git merge-tree --write-tree HEAD develop` exits 0. The `--no-ff` merge adds no code that the Check has not run on, so no merge-in is owed. If `develop` gains anything outside `.planning/` before this merge, this addendum no longer covers it.
2. **Candy-backdrop's merge-in after this lands.** `git merge-tree --write-tree HEAD feature/candy-backdrop` (`7ce3314`) exits 1:
   - `navigation/AppNavHost.kt` conflicts (content). It is a start-path file.
   - `docs/ux/licences-screen.md` conflicts (add/add).
   - `SettingsScreen.kt` auto-merges, and candy's hunks there touch no order constant.

   So candy takes `develop` in, with a hand resolution, a code-auditor review of the hunks and a tech-lead re-read, **before** its 15e sitting. Arm A is then its merge-base at the sitting, and A..B stays candy's own diff. This is the merge order already ruled (licences before candy sits). The conflict is the new fact.
3. **No QA device step is owed for this merge-in.** The hand hunks are a constant renumbering, a KDoc and test unions. The Robolectric suite pins the layout order, both rows' presence, the four headings and the width caps with both rows on the tree.
   - **The 3.0.0 release device pass** should open Settings once, signed in and with privacy choices shown, and see the palette row, the About section and the Licenses row together. It is one screen and folds into that pass. It is not a merge condition.
4. **The hand hunks agree with the colour-vision sign-off.** That sign-off required a contiguous 0–13 run with both rows (palette 4, Play Games 5–8, About 9, Licenses 10, Privacy 11–13), one KDoc sentence naming both insertions, and keep-both in the test. The `--cc` shows exactly that. No assertion was changed and no test was dropped (12 tests: 9 + 6 − 3 shared).
5. **The developer's open issue 2** (the `:app` JUnit XML on disk is the probe's) costs the auditor a re-run of `:app:testDebugUnitTest`. That is already in its forced Check, and it is not a defect.
6. **Staffing (Q-019).** The merge, its review and this re-read are all Opus. The mitigation is the same as before: every point above is checked against the repo, not taken from a report.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/tech-lead-merge-cv-addendum.md` — this addendum, committed alone. No code, spec or tech-design text was written.

## Checks
All commands were run in `/Users/ermanergoz/Desktop/PegSolitarie-lic`. I ran no Gradle and started no emulator, because the code-auditor's Check is running in this worktree. `uptime`: 1-minute load 8.59.
- `git log --oneline -8`, and `git status --short` empty — pass. HEAD is `696a83b`, and `git diff --name-only 7f03051 696a83b` lists only the developer's report.
- `git show --cc 7f03051`, read in full — pass. Hand hunks: `SettingsScreen.kt` KDoc lines 1–2 and the constant block; `SettingsScreenTest.kt` imports, the `setContent` union and the test-body union. The auto-merged files are `SettingsRoute.kt`, `ButtonSoundTest.kt`, `SettingsPlayGamesTest.kt` (4 calls) and `SettingsPrivacyTest.kt`; each passes both callbacks.
- `git merge-tree --write-tree 326291c 1bc442f`, then `git diff --name-only <tree> 7f03051` — pass. It lists only `SettingsScreen.kt` and `SettingsScreenTest.kt`.
- `grep -n '_ORDER\|Section(' SettingsScreen.kt` — pass. 14 constants, 0–13, each at its use site in composition order, with row 6 in two exclusive branches.
- `sed -n 150,230p ui/theme/Clay.kt` (`clayEntrance`) and `grep ENTRANCE_ ClayAnim.kt` (240 ms, stagger 40 ms) — read. This confirms the mechanism behind open issue 1.
- `git grep -n -i 'half-renumber\|rendered.top\|rendered top' -- app docs` — five claim sites, listed in (2).
- `bash -c` start-path diffs over §10's list: `develop...HEAD` gives the five licences files, `develop HEAD` gives the same five, and `305f741 1bc442f` gives colour-vision's seven `MainActivity`/`ui/theme` files, with no `di/` or `navigation/` — pass.
- `git diff develop...HEAD -- di navigation | git patch-id --stable` equals `git diff 305f741...326291c -- di navigation | git patch-id --stable` — pass (the "identical" check printed).
- `git rev-parse develop` gives `f1c399a`; `git diff --stat 1bc442f develop` gives `.planning/reports/STATUS.md` only; `git merge-tree --write-tree HEAD develop` exits 0 — pass.
- For each in-flight `feature/*`, `git diff --name-only <merge-base> <branch> -- ui/settings ui/theme/Clay.kt` — only `candy-backdrop` matches. Its `SettingsScreen.kt` diff, read, has no `_ORDER` line — pass.
- `git merge-tree --write-tree --name-only HEAD feature/candy-backdrop` exits 1, with conflicts in `AppNavHost.kt` and `docs/ux/licences-screen.md` — recorded for (3).2.
- `.planning/log/` was not read.

## Open issues / risks
1. **LS-g (3.0.1): the entrance-order guard,** as specified in (2). Until it lands, the four code claims and the spec oracles overstate `SettingsScreenTest`, and a Settings order change is guarded by the tech-lead's reading at sign-off.
2. **Candy-backdrop owes a hand-resolved merge-in** (`AppNavHost.kt`, start path; `docs/ux/licences-screen.md`) after this branch lands and before its sitting.
3. **The tech-design docs sync** (my sign-off's open issue 2) gains one line: §10's 15e reading, applied here to a merge-in that keeps the branch's start-path patch-id.
4. **Q-019 staffing,** as in (3).6.

## Handoff
Next: producer —
1. **Merge `feature/licences-screen` into `develop` with `--no-ff`, once the code-auditor's report on `7f03051` reads PASS** and its Check is green on `696a83b`. If the auditor fails it, this addendum lapses with it.
2. **Confirm `develop` still has nothing outside `.planning/` beyond `1bc442f`** before the merge ((3).1). The merge must land before candy-backdrop's sitting.
3. **Then route candy-backdrop's merge-in** ((3).2): the developer resolves it, the code-auditor reviews the hunks and the tech-lead re-reads the result, before candy sits.
4. **STATUS.md:**
   - **LS-g** as a 3.0.1 item (chain: android-game-dev → code-auditor → tech-lead), optionally in CV-a and CV-b's round;
   - **the spec-oracle wording** to the ux-designer, non-blocking, when LS-g lands;
   - **the release-pass Settings walk** in (3).3.

VERDICT: PASS conditional on auditor
