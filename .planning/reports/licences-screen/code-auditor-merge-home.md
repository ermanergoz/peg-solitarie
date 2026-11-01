# code-auditor report — licences-screen (develop merge-in: home chip arrival; R3 test findings)
Date: 2026-11-01
Branch: feature/licences-screen (398be26..65a9d4e; read at 76d3bd7, the developer's report commit. The worktree's HEAD moved to cb9ea7e during this review with two QA R4 commits. Both are docs-only and change nothing outside `.planning/` and `docs/`.)
Skills loaded: ergoz-soft:standards (role code-auditor). The snapshot is 0.6.2 and the plugin 1.0.0, so these plugin copies were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing. logging, puzzle-design and play-store are byte-identical to the snapshot in context (`diff -q`). Also `.claude/rules/lessons.md`. Plus code-reviewer, security-reviewer and compose-skill.
Ran in parallel with: licences QA R4
Ran on: claude-opus-5-5 at effort xhigh (policy; the harness names no effort). **Deviation:** my agent file pins fable. Q-019 (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published, so Opus reviewed Opus code. Mitigation: I re-derived every claim myself and ran the Check and every mutation myself.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **The merge `398be26` is clean.**
  - Its tree equals `git merge-tree --write-tree a801ae3 305f741` (`99e0435`).
  - `--cc` prints nothing, so no hunk was resolved by hand.
  - `strings.xml` is the only file both sides changed. It keeps both sides intact (crossed patch-ids) and has no duplicate resource name.
  - Every other file the merge brought is byte-identical to develop's `305f741`.
- **The test commit `65a9d4e` closes R3 Minor 1, Minor 2 and I-1, and changes test files only.**
  - I reproduced each red in a throwaway worktree.
  - One extra probe shows the new test d oracle catches an incomplete label, which the old oracle let pass.
- **The Check is green** with the `:app` source tasks forced: 1,401 `:app` tests, 0 failed, 0 skipped.
- **Start path:** this branch's own diff against develop touches `di/` and `navigation/`. Both are on tech design §10's start-path list. The diff touches none of the conditions that void brief §5.3's "no 15e sitting" decision. Details are in the start-path section below; the ruling is the tech-lead's.
- No Critical, Major or Minor findings. Three Info notes.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor-merge-home.md`: this report, committed alone.
- Throwaway worktree `…/scratchpad/mh/wt`, detached at `76d3bd7`, used for the mutations. I removed it with `git worktree remove --force`. The branch worktree was never edited, and `git status --short` is empty.

## Checks
**Host load**
- `uptime` before every Gradle run. Every run started with the 1-minute load below 12 (5.45, 2.15, 1.96, and under 2.4 for the mutation runs).

**The merge**
- `git merge-tree --write-tree a801ae3 305f741` against `git rev-parse 398be26^{tree}`: **pass**, both `99e043592e4f…`.
- `git show --cc --format= 398be26 | wc -l`: **pass**, 0.
- `git merge-base a801ae3 305f741`: `ae8bd2d`. `develop` is still at `305f741`.
- Overlap of `git diff --name-only ae8bd2d 305f741` and `ae8bd2d a801ae3`: **read**, only `app/src/main/res/values/strings.xml`.
- `strings.xml` keeps both sides: **pass**.
  - `git diff ae8bd2d 305f741` and `git diff a801ae3 398be26` on the file share patch-id `a71d9852…`. So develop's hunk is applied unchanged: `elapsed_time_spoken`, `duration_minutes` and `duration_under_a_minute`.
  - `git diff ae8bd2d a801ae3` and `git diff 305f741 398be26` share patch-id `936c606b…`. So the branch's licences block is unchanged.
  - Resource names: none duplicated (`uniq -d` empty); 200 names; none missing from either side; none extra.
  - `xmllint --noout`: well-formed.
- Every file develop changed since the merge base, other than `strings.xml`, against `305f741`: **pass**, all `same`.
  - In `app/src/main`, that is 10 files: `ui/home/` (5), `BoardCanvas.kt`, `ElapsedTimeSpeech.kt`, `GameStatusStrip.kt`, `values-v35/themes.xml` and `values-night-v35/themes.xml`.
  - It also covers develop's 11 test files and `scripts/play-upload.sh`.
- Every file the branch changed since the merge base, other than `strings.xml`, against `a801ae3`: **pass**, no difference.
- C11 ruling §5 item 1: `git grep -n 'ANIMATOR_DURATION_SCALE\|Settings\.Global\.putFloat' HEAD -- app/src/test`: **pass**. It matches only `AnimationsEnabledGateWithoutScaleTest.kt:27-29`, which writes the setting on purpose, and the KDoc at `SystemMotionScale.kt:16`. There is no vacuous reduced-motion case to retarget.
- Gate census: `git grep -c 'rememberAnimationsEnabled()'` gives the same 10 files and counts at `HEAD` as at `305f741`. `ui/licenses`, `ui/settings` and `navigation` call the gate nowhere. So the merge adds no gate reader and no gate-keyed effect.
- Trailers: `398be26`, `65a9d4e` and `76d3bd7` each carry exactly one `Co-Authored-By: Claude Fable 5.1` line (plugin 1.0.0 gitflow) and no `Claude-Session` line. Subjects are 50, 51 and 60 characters.

**The test commit**
- `git show --stat 65a9d4e`: **pass**, 3 files, all under `app/src/test/…/ui/licenses/`.
- `git diff --quiet 398be26 76d3bd7 -- . ':(exclude)app/src/test' ':(exclude).planning'`: **pass**, exit 0. The APK inputs are the same at the merge and at the head.
- `git show -U0 65a9d4e | grep -E '^-.*(assert|containsExactly|isEqualTo|hasSize|startsWith)'`: **read**. The only removed assertion is d's `.startsWith(title)`, together with its message line, and it is replaced by the stricter `.isEqualTo(labelOf(entry))`. The third match is the old KDoc line. No other oracle changed.
- `git diff 398be26 65a9d4e | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline|ktlint-disable|detekt:)'`: **pass**, no match. Over the branch's whole diff against develop, the count is 0.
- Nothing to approve or reject.

**The Check**
- `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun :app:detekt --rerun :app:runKtlintCheckOverMainSourceSet --rerun :app:runKtlintCheckOverTestSourceSet --rerun :app:lintDebug --rerun`, first run: **void, not the branch.**
  - It reported "Gradle build daemon has been stopped: stop command received".
  - The producer confirmed that another agent ran `./gradlew --stop` around 04:50.
- Same command, re-run: **pass**.
  - "BUILD SUCCESSFUL in 11m 12s", exit 0, 107 actionable tasks (15 executed, 92 up to date).
  - `:app:testDebugUnitTest`, `:app:detekt`, both ktlint runs, `:app:lintDebug`, `:domain:depthSweep` and the literal checks executed.
  - `:app` test XMLs: 152 files, all written during the run at 04:54:52. 1,401 tests, 0 failed, 0 errors, 0 skipped.
  - `LicencesTalkBackContractTest` 13/13, `GoogleNoticesScreenTest` 7/7 and `LicencesAccessibilityExposureTest` 1/1.
  - Develop's classes: `HomeChipArrivalTest` 13/13, `BoardCanvasFirstFrameTest` 2/2, `AnimationsEnabledGateWithoutScaleTest` 1/1, `WindowThemeTest` 4/4, `WindowThemeApi35FilesTest` 2/2, `ElapsedTimeSpeechTest` 1/1 and `GameStatusStripTest` 10/10.
  - Shared surfaces: `NavFlowTest` 14/14, `LicensesNavFlowTest` 5/5 and `SettingsScreenTest` 9/9.
  - `:domain`: 294 tests and `:build-logic`: 100 tests, all with 0 failures. Both were up to date, since this loop changes neither module.
  - Lint: 0 errors and fatals, 64 warnings. The log has 0 `w:` and 0 `e:` lines.
- The code this ran on equals `65a9d4e`'s: `git diff --quiet 76d3bd7 cb9ea7e -- . ':(exclude).planning' ':(exclude)docs'` exits 0.

**Mutations and probes**
All in the throwaway worktree with `./gradlew :app:testDebugUnitTest --tests '<class>' … --max-workers=4 --console=plain`, and each reverted with `git checkout --`. None was committed.
- **M2**: `modifier = Modifier.semantics { hideFromAccessibility() }` → `modifier = Modifier` (`LicenseEntryCard.kt:160`). Run on the three licences classes. **Fail as intended**: 21 tests, 3 failed. They are b, c's hidden half and test d.
  > what the card's visible children say, in order: its whole label, once — expected: …, J2ObjC annotations — but was : …, J2ObjC annotations. Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations

  This is R3's P2, now caught by d. Minor 1 is closed.
- **R**: `itemsIndexed(rows, …)` → `itemsIndexed(rows.take(0), …)` (`GoogleNoticesScreen.kt:100`). Run on `GoogleNoticesScreenTest`.
  - With the edited test: **fail as intended**, 7 tests and 6 failed. Both R2 tests fail with "the rows on screen — value of: iterable.size() — expected: 4 — but was: 0".
  - With the same mutation and the test file as it was at `398be26`: 7 tests and 4 failed. Both R2 tests **passed** with no rows on screen. That is the vacuous pass I-1 named, now closed.
- **M3, my own probe of the new oracle**: `label = texts.joinToString(…)` → `texts.dropLast(1).joinToString(…)`, with the texts still hidden. Run on `LicencesAccessibilityExposureTest`.
  - The new d: **fail**, at `LicencesAccessibilityExposureTest.kt:97`.
  - The old d, from `398be26`: **pass**, "BUILD SUCCESSFUL".
  - So d now guards "whole" as well as "once", not only "title first".

**Requirements**
- **Minor 2**: `LicencesTalkBackContractTest.kt:30-35`'s class KDoc now says these tests read Compose's merged tree, "not the tree TalkBack reads", and are "necessary, not sufficient". It points to the exposure test. This matches F-4 ruling Layer 1. Closed.
- **The oracle helper**: `labelOf`/`textOf` (`LicencesAccessibilityExposureTest.kt:115-125`) resolve each `LicenseLine` variant the way production's `licenseLineText` does: `Plain`/`Address` through `getString`, and `SoundCount` through `R.plurals.licenses_sound_effects`. `LicenseLine` is sealed, so the `when` is exhaustive at compile time. The separator is the production constant, with no literal.

**Security and dependencies**
- Security: **pass**. The test commit adds no network, secret or input path. The merge brings develop's `scripts/play-upload.sh`, which was reviewed on develop's own branch and is byte-identical here.
- THIRD_PARTY.md: not applicable. This loop adds no dependency (`libs.versions.toml` is unchanged against develop).

## Start-path code in this branch's own diff (for the tech-lead's sign-off)
- **`git diff --name-only 305f741 HEAD`** (develop is the merge base after the merge-in) touches two start-path directories from tech design §10's list:
  - `navigation/`: `AppNavHost.kt` (M), `Routes.kt` (M) and `LicensesDestinations.kt` (A);
  - `di/`: `DataModule.kt` (M) and `DispatchersModule.kt` (A).
- **So, read literally against the founder's directive** ("15e sittings only where start-path code changed"), **yes: start-path-list files changed.** They are the same files, with the same content kinds, that brief §5.3 listed when it decided "no 15e sitting of its own".
- **What they add, read in the diff:**
  - four graph destinations;
  - four defaulted content lambdas and a third Settings callback;
  - four `@Serializable` route types;
  - two unscoped `@Binds`;
  - one `@IoDispatcher` qualifier, whose `object` `@Provides` returns `Dispatchers.IO`.
- **None of brief §5.3's void conditions holds** (`git diff --name-only 305f741 HEAD`, filtered):
  - `App.kt`, `MainActivity.kt`, `ui/home/`, `ui/theme/`, `res/font/`, every `themes.xml` (`values`, `-night`, `-v27`, `-v35`, `-night-v35`) and `AndroidManifest.xml` are untouched;
  - nothing is `@Inject`ed into `App`;
  - `AssetLicenseTextSource` and `AssetGoogleNoticeSource` have plain `@Inject` constructors, with no `init`, `lazy` or `@Singleton`;
  - nothing is read from the root.
- **The merge-in's own start-path changes** (`ui/home/`, the two v35 themes) are byte-identical to `305f741`. That tree passed home-chip-arrival's session S 15e sitting (develop's commit `876258e`, "record the home session S sitting (15e PASS, 25.5 ms)").
- **The facts therefore support §5.3's decision standing.** Whether the founder's directive means "§10's file list" or "§10's first-frame test" is the tech-lead's to rule. The sign-off should say which reading it applies.
- **Either way,** this branch must land before candy-backdrop's sitting (A11; merge order step 6a).

## Findings
No Critical, Major or Minor findings.

**Info**
- **I-1.** `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesAccessibilityExposureTest.kt:63`: the name `aCardHalfAboveTheViewportIsExposedWithItsTitleFirst` now under-describes its oracle, which checks whole, once and title first. The KDoc at `:57-61` states the full claim. The F-4 ruling, R3 and QA cite the test by this name, so a rename is the tech-lead's call (developer's open issue 1). No action needed for the merge.
- **I-2.** `LicencesAccessibilityExposureTest.kt:115-125` duplicates `LicencesTalkBackContractTest.kt:309-319` (`labelOf`/`textOf`) line for line. If a new `LicenseLine` variant is added, both copies must change. Exhaustiveness makes each copy fail to compile, so neither can drift silently. A shared `internal` test helper would be a small follow-up refactor (developer's open issue 2).
- **I-3.** The commit trailers name Fable 5.1, while every role in this loop ran on Opus (Q-019). That is exactly what plugin 1.0.0's gitflow rule mandates, so it is not a defect. Each report's `Ran on:` line is the record of the real model. Earlier commits on this branch carry the session's two-line trailer. Which form applies to them stays the producer's call (R3 I-3).

## Open issues / risks
1. **Device behaviour.** TalkBack's spoken output is still the device's to decide. Test d reads Compose's exposed tree under Robolectric, and QA R4 grades the device (R3 open issue 1).
2. **The 15e reading.** The question in the start-path section above needs one line in the tech-lead's sign-off.
3. **The 54 type-resolution `detektDebug` issues** (R3 open issue 4) are untouched and outside this diff. `check` does not run that task.
4. **Concurrent commits.** The QA R4 commits (`a2bec9a`, `cb9ea7e`) landed during this review. They change `docs/ux/licences-screen.md` and `.planning/` only, so they are outside this review's scope and change no APK input.

## Handoff
Next: tech-lead. You need three things from this report:
- the merge and the test commit are PASS;
- the 15e question, with the start-path facts above;
- a decision on Info I-1 and I-2, or leave them as they are.

QA R4 builds `65a9d4e`, whose APK inputs equal the head's.

VERDICT: PASS
