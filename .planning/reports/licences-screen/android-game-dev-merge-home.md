# android-game-dev report — licences-screen (develop merge-in: home chip arrival; R3 test findings)
Date: 2026-11-01
Branch: feature/licences-screen (398be26..65a9d4e)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing were read in full; `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow, android-testing, compose-skill
Ran in parallel with: none named by the dispatch (other emulator and Gradle work ran on the host)
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort, and the founder's 2026-10-31 directive would make it xhigh)
Fan-out: none
Loops: 1 (closes the code-auditor's R3 findings; R3 was a PASS)
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- `develop` at `305f741` (bugfix/home-chip-arrival) is merged in with `--no-ff` as `398be26`. There was no conflict and no hand-resolved hunk. Only `strings.xml` was auto-merged, with disjoint hunks.
- No test on the merged tree drives reduced motion through `Settings.Global`, except `AnimationsEnabledGateWithoutScaleTest`, which does it on purpose (C11 ruling §2(c) item 3). So nothing had to move to `SystemMotionScale`.
- `65a9d4e` closes R3's three test findings (Minor 1, Minor 2, I-1) and changes test files only.
  - Minor 1: test d now asserts that the exposed speech equals the card's label. It goes red when `hideFromAccessibility()` is removed.
  - I-1: both R2 notices tests now fail when no rows render. Before the edit they passed vacuously.
- The full Check is green at `65a9d4e`: exit 0, with `:app` running 1,401 tests, 0 failed and 0 skipped.

## Outputs
- `398be26` chore(licences): merge develop (home chip arrival). Parents are `a801ae3` and `305f741`. Its tree is `99e0435`, the same as `git merge-tree --write-tree a801ae3 305f741`.
- `65a9d4e` test(licences): close the R3 review's test findings. Three files under `app/src/test/java/com/app/pegsolitaire/ui/licenses/`:
  - `LicencesAccessibilityExposureTest.kt` (Minor 1). Test d's last assertion was `.startsWith(title)`. It is now `.isEqualTo(labelOf(entry))`: what the half-clipped Gson card's visible children say, joined by `CARD_LABEL_SEPARATOR`, must equal the label. The label is resolved from the resources (title, then each line) by private `labelOf` and `textOf` helpers. The class KDoc and a new test KDoc state "whole, once, title first". The position guards and the reading path are unchanged.
  - `LicencesTalkBackContractTest.kt` (Minor 2). The class KDoc changed, and nothing else in the file did. It now says the tests read Compose's merged semantics tree, and that this is not the tree TalkBack reads. Compose hands services only the on-screen nodes (F-4 ruling, Layer 1), so these tests are necessary, not sufficient. It points to `LicencesAccessibilityExposureTest` for the exposed tree.
  - `GoogleNoticesScreenTest.kt` (I-1). `everyRowSpeaksItsWholeLabelAsItsOwnDescription` and `everyTextInsideARowIsHiddenFromAccessibility` now assert `hasSize(ROWS.size)` on their row nodes before `zip(ROWS)`.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/android-game-dev-merge-home.md`: this report, committed alone after `65a9d4e`.

## Checks
- `uptime` before every Gradle run. Every run started with the 1-minute load below 12 (3.91, 4.45, 4.33, 9.04, 11.13). The Check was held back from 12.62 until the load fell to 11.13.
- `git merge-tree --write-tree --name-only HEAD develop` at `a801ae3` — pass: exit 0, tree `99e0435`, no conflicted file.
- Overlap of `git diff --name-only ae8bd2d develop` with `git diff --name-only ae8bd2d a801ae3`, where `ae8bd2d` is the merge base — read: only `app/src/main/res/values/strings.xml`.
- `git merge --no-ff --no-commit develop` — pass: "Auto-merging app/src/main/res/values/strings.xml / Automatic merge went well". `git write-tree` gave `99e0435`, the same as merge-tree. `strings.xml` keeps both sides: develop's `elapsed_time_spoken`, `duration_minutes` and `duration_under_a_minute`, and the branch's licences strings.
- `git show --cc --format= 398be26 | wc -l` — pass: 0, so no hand-resolved hunk.
- `git grep -n 'ANIMATOR_DURATION_SCALE\|Settings\.Global\.putFloat' HEAD -- app/src/test` on the merged tree — pass. It matches only `AnimationsEnabledGateWithoutScaleTest.kt:27-29` (on purpose) and the KDoc at `SystemMotionScale.kt:16`.
- A grep of every test file the branch added or changed since the merge base, for `animator`, `putFloat`, `Settings.(Global|System|Secure)`, `turnAnimationsOff` and `SystemMotionScale` (case-insensitive) — pass: no match (exit 1). Nothing to retarget.
- **Minor 1 red.** With the temporary mutation M2, `modifier = Modifier.semantics { hideFromAccessibility() }` became `modifier = Modifier` in `LicenceCardTexts` (never committed). The command was `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.LicencesTalkBackContractTest' --tests 'com.app.pegsolitaire.ui.licenses.GoogleNoticesScreenTest' --tests 'com.app.pegsolitaire.ui.licenses.LicencesAccessibilityExposureTest' --max-workers=4 --console=plain`.
  - Result: fail as intended, 21 tests and 3 failed. They are b, c's hidden half, and the new test d:
    > `aCardHalfAboveTheViewportIsExposedWithItsTitleFirst`: "what the card's visible children say, in order: its whole label, once — expected: …, J2ObjC annotations — but was : …, J2ObjC annotations. Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations"
  - Truth elides the shared prefix, which is the label. The speech is the label followed by the two visible lines a second time. This is the auditor's P2, now caught.
  - M2 was reverted with `git checkout --`, and `git diff --quiet -- app/src/main` exited 0.
- **I-1, the gap shown first.** With the temporary mutation R, `itemsIndexed(rows, …)` became `itemsIndexed(rows.take(0), …)` in `GoogleNoticesScreen.kt` (never committed). The old test ran with `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.GoogleNoticesScreenTest' --max-workers=4 --console=plain`.
  - Result: 7 tests and 4 failed. `everyRowSpeaksItsWholeLabelAsItsOwnDescription` and `everyTextInsideARowIsHiddenFromAccessibility` passed with no rows on screen. That is the vacuous pass.
- **I-1 red.** The same command and mutation, on the edited test — fail as intended: 7 tests and 6 failed. Both R2 tests fail with "the rows on screen — value of: iterable.size() — expected: 4 — but was: 0". R was reverted, and `git diff --quiet -- app/src/main` exited 0.
- **Green on the real code.** The three-class command above with `:app:ktlintCheck` added — pass. "BUILD SUCCESSFUL in 56s". `LicencesTalkBackContractTest` passed 13/13, `GoogleNoticesScreenTest` 7/7 and `LicencesAccessibilityExposureTest` 1/1. `ktlintTestSourceSetCheck` and `ktlintMainSourceSetCheck` executed.
- `./scripts/check.sh --wait --max-workers=4 --console=plain` at `65a9d4e`, with a clean tree — **pass**. Exit 0, "BUILD SUCCESSFUL in 11m 9s", 107 actionable tasks (20 executed, 87 up to date), from 04:27:51 to 04:39:01.
  - Executed: `:app:testDebugUnitTest`, `:app:detekt`, `:app:lint` and `:domain:depthSweep`.
  - `:app`: 1,401 tests, 0 failed, 0 errors, 0 skipped. Every XML was written at 04:30:50, during the run.
  - Develop's classes are green: `HomeChipArrivalTest` 13/13, `BoardCanvasFirstFrameTest` 2/2, `AnimationsEnabledGateWithoutScaleTest` 1/1, `NavFlowTest` 14/14. So is the branch's `LicensesNavFlowTest`, 5/5.
  - `:domain`: 294 tests, plus fuzz 1 and depthSweep 1, all with 0 failures. `:build-logic`: 100 tests, 0 failures. Lint: 0 errors, 64 warnings.
  - The log has 0 `w:` and 0 `e:` lines, because the compilations were already up to date from the targeted runs.
- `git diff --quiet 398be26 65a9d4e -- . ':(exclude)app/src/test' ':(exclude).planning'` — pass: exit 0. The APK inputs are the same at the merge and at the test commit.
- `git show -U0 65a9d4e | grep -E '^-.*(assert|containsExactly|isEqualTo|hasSize|startsWith)'` — read. The only removed assertion line is d's `.startsWith(title)`, replaced by the stricter `.isEqualTo(labelOf(entry))`. The other matches are the message line it rewrote and the old KDoc line. No assertion was weakened, and no other oracle changed.
- `git diff 398be26 65a9d4e | grep -nE '^\+.*(@Suppress|@Ignore|!!|baseline|ktlint-disable|detekt:)'` — pass: no match. Over the merge, the same grep matches only develop's KDoc prose, "the design baseline's system bars".
- `git diff --quiet develop HEAD --` on `ui/home`, `BoardCanvas.kt`, `ui/game`, `values-v35`, `values-night-v35`, `MainActivity.kt`, the manifest and `ui/theme` — pass: exit 0. Every start-path file the merge brought is byte-identical to develop's.
- Trailers on `398be26` and `65a9d4e` — one `Co-Authored-By` line each (the plugin 1.0.0 gitflow trailer), and no `Claude-Session` line.

## Open issues / risks
1. **Test d's name is kept.** `aCardHalfAboveTheViewportIsExposedWithItsTitleFirst` now asserts the whole label, once, which is more than its name says. The tech-lead's F-4 ruling, the R3 report and QA's plan cite it by that name, so I kept it and put the full claim in its KDoc. A rename is the tech-lead's call.
2. **The label helper is duplicated.** `labelOf` and `textOf` in the exposure test repeat the private helpers in `LicencesTalkBackContractTest`. I left that class's oracles untouched, as R3 probe 3 reviewed them. A shared test helper would be a small follow-up refactor.
3. **Commit trailers.** My two commits carry the single trailer of plugin 1.0.0's gitflow rule, as the earlier merge-in `6e3d5e0` and develop's home branch do. Earlier commits on this branch carry the session's two lines. Which applies to them stays the producer's call (R3 I-3). This report's `Ran on:` line records the real model.
4. **The type-resolution `detektDebug` issues are not touched.** The auditor's R3 open issue 4 lists 54 of them, and `check` does not run that task.
5. **TalkBack's speech is still the device's to decide** (R3 open issue 1). Test d reads Compose's exposed tree through Robolectric, not TalkBack.

## Handoff
Next: code-auditor, then qa-engineer, then tech-lead.
- **code-auditor:** the merge `398be26` and the test commit `65a9d4e`.
  - The merge has no hand-resolved hunk, and its tree equals merge-tree's.
  - Re-run M2 against test d, and R against the two notices tests.
- **qa-engineer:** R4 builds **`65a9d4e`**. The report commit changes only `.planning/`, so the APK is the same.
  - **`app/src/main` start-path files did change relative to `a801ae3` (R3's `595e2a7` had the same code).** The merge brings develop's home-chip-arrival code: `ui/home/` (5 files), `ui/components/BoardCanvas.kt`, `ui/game/ElapsedTimeSpeech.kt`, `ui/game/GameStatusStrip.kt`, `values-v35/themes.xml`, `values-night-v35/themes.xml` and `values/strings.xml`.
  - Each is byte-identical to develop's `305f741`, except `strings.xml`, which is both sides' disjoint hunks.
  - That code passed home-chip-arrival's session S 15e sitting on develop's tree.
  - Whether this branch owes its own 15e under the founder's directive (2) is the producer's and tech-lead's call.
  - My test commit changes no `app/src/main` file.
- **tech-lead:** decide on open issues 1 and 2 if wanted, then sign off (F-4 ruling R5).
