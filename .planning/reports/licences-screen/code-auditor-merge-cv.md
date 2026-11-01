# code-auditor report — licences-screen (develop merge-in: colour vision)
Date: 2026-11-01
Branch: feature/licences-screen (7f03051..7f03051: the merge commit alone, parents `326291c` and develop `1bc442f`; read at `696a83b`. HEAD moved to `50b71aa` during this review with the tech-lead's addendum. That commit adds one report and nothing outside `.planning/`: `git diff --quiet 7f03051 50b71aa -- . ':(exclude).planning'` exits 0. I did not read the addendum.)
Skills loaded: ergoz-soft:standards (role code-auditor). The snapshot is 0.6.2 and the plugin 1.0.0, so these plugin copies were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing. logging, puzzle-design and play-store are byte-identical to the snapshot in context (`diff -q`). Also `.claude/rules/lessons.md`. Plus code-reviewer, security-reviewer, compose-skill and compose-performance-audit.
Ran in parallel with: none named by the dispatch (another session's `ergoz_studio` emulator and a `PegSolitarie-candy` build ran on the host; I touched neither)
Ran on: claude-opus-5-5 at effort xhigh (policy; the harness names no effort). **Deviation:** my agent file pins fable. Q-019 puts every Fable-pinned role on Opus until 3.0.0 is published, so Opus reviewed Opus code. Mitigation: I re-derived the merge myself, ran the Check, forced a re-execution of the `:app` tests, and ran two mutations of my own.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **The merge `7f03051` is faithful.**
  - `git merge-tree --write-tree 326291c 1bc442f` conflicts in exactly `SettingsScreen.kt` and `SettingsScreenTest.kt`. Its tree (`612ba45`) differs from `7f03051^{tree}` in those two files only, and only inside the conflict regions.
  - The five other files both sides changed carry each side's changed lines exactly, in both directions.
  - Every other file equals one parent's version.
- **The six hand hunks match the tech-lead's merge-in conditions.** Those conditions are the colour-vision sign-off (Merge readiness) and `docs/ux/colour-vision.md` §3.1's table: a contiguous 0–13 run, palette row 4, Play Games 5–8, About 9, Licenses 10 and Privacy 11–13, with one KDoc sentence naming both insertions.
  - The constants follow composition order (General, Play Games, About, Privacy).
  - All 12 tests from the base and both sides are present, and no assertion line was lost.
- **The Check is green:** exit 0. A forced re-execution of `:app:testDebugUnitTest` also passed: 165 suites, 1,454 tests, 0 failed, 0 skipped.
- **The developer's open issue 1 is confirmed and is pre-existing.** It is wider than reported: `SettingsScreenTest` cannot see entrance orders at all. Two mutations of my own also survive, one of which moves the palette row out of General (L-1, L-2). My non-binding view is that it should not block this merge; it belongs in a 3.0.1 low item.
- There are no Critical, Major or Minor findings in the merge. There are two Low test-gap findings and three Info notes, none of them blocking.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor-merge-cv.md`: this report, committed alone.
- Throwaway worktree `…/scratchpad/ca-mcv/wt`, detached at `7f03051`, used for the mutations. I removed it with `git worktree remove --force`, and `git worktree list` no longer shows it. The branch worktree was never edited, and `git status --short` is empty.
- My logs are under `…/scratchpad/ca-mcv/` and are not committed. Another agent shares this session's scratchpad and overwrote my first `scratchpad/check.log` mid-run, so I quote the Check's own exit line from the task output, and the test tallies from the worktree's JUnit XML.

## Checks
**Host**
- `uptime` before every Gradle run. Every run started with a 1-minute load below 12: 6.18 before the Check, then 3.18, 7.39, 7.98 and 8.53.
- No `Pixel_9a_bench` process; `.gradle/ergoz-build.lock` absent. `./gradlew --stop` was never run, and no emulator was started — pass.

**The merge**
- `git show --no-patch --format='%P' 7f03051` gives `326291c 1bc442f`, and `git merge-base 326291c 1bc442f` gives `305f741` — read.
- `git merge-tree --write-tree --name-only 326291c 1bc442f` exits 1. The conflicts are only `SettingsScreen.kt` and `SettingsScreenTest.kt`. The auto-merged files are `SettingsRoute.kt`, `strings.xml`, `ButtonSoundTest.kt`, `SettingsPlayGamesTest.kt` and `SettingsPrivacyTest.kt` — pass (matches the developer's claim).
- `git diff --stat 612ba45 7f03051` lists only the two conflicted files, and `git diff 612ba45 7f03051` changes nothing outside the `<<<<<<<`/`>>>>>>>` regions — pass. Every non-conflicted file is git's own merge result.
- The files changed by both sides: for each, the sorted set of changed lines in `git diff -U0 326291c 7f03051` was compared with `305f741 1bc442f`, and in `1bc442f 7f03051` with `305f741 326291c`.
  - **The five auto-merged files are equal in both directions** — pass.
  - **`SettingsScreen.kt`:** the only differences are the 0-13 count, the merged history sentence and the renumbered constants — pass.
  - **`SettingsScreenTest.kt`:** the only difference is the `setContent` signature, where theirs' one-line form became ours' multi-line form plus one parameter — pass.
- `git show --cc 7f03051`, read in full against `docs/ux/colour-vision.md` §3.1 (line 250) — pass:
  - `COLORBLIND_ROW_ORDER` 4, `PLAY_GAMES_*` 5–8, `ABOUT_HEADER_ORDER` 9, `LICENSES_ROW_ORDER` 10, `PRIVACY_*` 11–13;
  - `grep -n '_ORDER'` shows each used once in composition order (`SettingsScreen.kt:241–516`), with `PLAY_GAMES_ROW_ORDER` used in its two exclusive branches as before;
  - `SettingsScreen(` and `SettingsRoute` keep both `onToggleColorblindPalette` and `onOpenLicenses`.
- Test names in `SettingsScreenTest` at `305f741`, `326291c`, `1bc442f` and `7f03051` — pass:
  - base 3, licences 3+6, colour-vision 3+3, merge 12;
  - the merge holds the union, and no name is lost or duplicated.
- `git grep -nE '^(<<<<<<<|>>>>>>>|=======$|\|\|\|\|\|\|\|)' 7f03051 -- app domain docs build-logic` exits 1 — pass.
- `strings.xml` at `7f03051`: `uniq -d` over the names is empty — pass.
- Suppressions: `git diff 1bc442f 7f03051 -- app domain build-logic` adds no `@Suppress`, `@Ignore`, `!!` or baseline line — pass.
  - Against `326291c` the one added `@Suppress("LongMethod")` is develop's `BoardCanvas` line, byte-identical to `1bc442f`, approved at colour-vision loop 0.
  - New suppressions approved here: none.
- Dependencies: `git diff --name-only 326291c 7f03051 -- '*.gradle.kts' gradle build-logic THIRD_PARTY.md app/src/main/AndroidManifest.xml` is empty — pass. No new dependency, so no THIRD_PARTY.md row is owed.
- `git diff --name-only 1bc442f 7f03051` over `MainActivity.kt`, `ui/theme`, `ui/components`, `ui/game` and `docs/ux/colour-vision.md` is empty, so colour-vision's files equal develop's — pass. The branch's own main diff against develop is the licences set: `data/licenses`, `ui/licenses`, `di/`, `navigation/`, `ui/settings/`, `strings.xml` and the license assets.
- Security: the merge adds no network, secret, permission or input path. The two hand-resolved files are UI ordering and tests — pass.

**The Check and tests**
- `./scripts/check.sh --wait --max-workers=4 --console=plain` at `7f03051`, 06:08:09 to 06:19:27 — **pass**:
  - `CHECK-EXIT=0`, `BUILD SUCCESSFUL in 11m 18s`, `107 actionable tasks: 11 executed, 1 from cache, 95 up-to-date`;
  - `:app:testDebugUnitTest` came FROM-CACHE (the developer's run of this same tree), and `:domain:depthSweep`, `:domain:koverVerify` and `:app:lint` executed.
- Because the cached `:app` result came from the author's run, I forced it: `./gradlew --console=plain --max-workers=4 :app:testDebugUnitTest --rerun` — **pass**:
  - `RERUN-EXIT=0`, the task executed (1 executed, 50 up-to-date);
  - JUnit XML written 06:21:39: 165 suites, 1,454 tests, 0 failures, 0 errors, 0 skipped.
- Named classes in that XML (tests/failures) — pass: `SettingsScreenTest` 12/0, `SettingsPrivacyTest` 8/0, `SettingsPlayGamesTest` 22/0, `ButtonSoundTest` 20/0, `SettingsViewModelTest` 24/0, `ColorblindPaletteLiveSwitchTest` 1/0, `LicensesNavFlowTest` 5/0.
- Other XML after the Check — pass: `:domain` test 33 suites, 294 tests; fuzz 1/0; depthSweep 1/0 (written 06:19:27, this run); `:build-logic` 10 suites, 100 tests; all 0 failed, 0 skipped.

**Mutations** (throwaway worktree, `./gradlew --console=plain --max-workers=4 :app:testDebugUnitTest --tests …`)
- **M1, a half-renumbering from the colour-vision side.** `COLORBLIND_ROW_ORDER = 9`, so the palette row enters together with the About header. `SettingsScreenTest`: `compileDebugKotlin` executed, 12/0, exit 0 — **survives**.
  - The developer's probe was the licences-side mirror of this (`ABOUT_HEADER_ORDER` 8, `LICENSES_ROW_ORDER` 9), and it also survived.
- **M2, the palette row moved out of General.** The row moves to the About section, below Licenses, with its order unchanged. `SettingsScreenTest`: 12/0, exit 0 — **survives**.
  - Widened to `ui.settings.*`, `*Licen*`, `*ButtonSound*`, `*NavFlow*`, `*TalkBack*` and `*Colorblind*`: 29 suites, 197 tests, 0 failed — **survives**.
- `.planning/log/` was not read.

## Findings

### Critical / Major / Minor
None. The resolution is what the merge-in conditions ask for, and nothing from either side was lost.

### Low (not blocking; recommended as one 3.0.1 test item)
- **L-1** — `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsScreenTest.kt:52-77` (`setContent` ends in `composeRule.waitForIdle()`), together with `app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt:187-205`. The test cannot see entrance orders at all. This confirms the developer's open issue 1, and the problem is wider than a half-renumbering.
  - **Why the test is blind.** `clayEntrance` moves an element only through `graphicsLayer { translationY = (END - progress) * distance }`, and that settles to 0. The test reads `positionInRoot` after idle, with the clock auto-advancing, so every top it reads is the layout position. No value of any `*_ORDER` constant can change one.
  - **Evidence.** M1 and the developer's probe survive. So would any permutation of the 14 constants.
  - **Four statements claim the opposite.** They are false:
    - `SettingsScreen.kt:581-583`, the order KDoc ("`SettingsScreenTest` reads the rendered top coordinates instead, so it fails on a half-renumbering");
    - `SettingsScreenTest.kt:29-33`, the class KDoc;
    - the KDoc of `theColorblindRowSitsDirectlyAfterHapticsAndAboveThePrivacyHeader`;
    - `docs/ux/colour-vision.md:250` ("`SettingsScreenTest`'s rendered-top check covers both").
  - **It is pre-existing.** `git log -S'fails on a half-renumbering'` traces the claim to `c642676` (phase 15), so it is already on `develop` and on both parents. This merge neither adds nor widens it.
  - **Remedy, for the follow-up.** Pause the clock (`mainClock.autoAdvance = false`), advance to a frame inside the cascade, and assert that the elements' `positionInRoot.y` offsets from their settled tops strictly decrease down the column. `positionInRoot` includes the layer's translation, and two equal orders give equal offsets. Then correct the four statements.
- **L-2** — `SettingsScreenTest.kt`: `theColorblindRowSitsDirectlyAfterHapticsAndAboveThePrivacyHeader` (its node list stops at `privacyHeader`) and `theAboutSectionFollowsGeneralWhenPlayGamesIsAbsent` (it compares About with `hapticsLabel`). Neither pins the palette row above the About section.
  - **Evidence.** M2 puts the row under Licenses, and 197 Settings, licences and palette tests stay green.
  - **What the test misses.** CV-AC01 says "directly after Haptics, still inside General". The test asserts only "after Haptics, before Privacy", and since this merge About lies in between.
  - **Origin.** The gap existed on develop for a signed-in Play Games section. Keep-both makes it wider, because each side's position test was written without the other's row.
  - **Remedy, for the follow-up.** Add the About header to the colour-vision test's node list, after `colorblindLabel`. Or change the About test's anchor from Haptics to the palette row, the last row of General. Neither change belonged in a keep-both resolution.

### Info
- **I-1 — `develop` has moved.** It is now `f1c399a`, past the `1bc442f` this branch merged. The two new commits touch only `.planning/reports/STATUS.md`, and `git merge-tree --write-tree --name-only HEAD develop` is conflict-free (`273113c`). No code merge-in is owed, but the merge to develop will 3-way merge STATUS.md.
- **I-2 — `docs/ux/licences-screen.md:558` still lists this branch's pre-merge orders** (About 8 … Privacy 12). The note just below it (lines 560–562) declares those numbers "this branch's own, not a contract", and `docs/ux/colour-vision.md:250` gives the merged run, which the code matches. This is a doc sync for the ux-designer at the close, not a defect.
- **I-3 — Commit trailer.** `7f03051` carries the single `Claude Fable 5.1` trailer of plugin 1.0.0's `rules/gitflow.md`, written by an Opus author. The developer records this as their open issue 4, and the real model is in each report's `Ran on:` line.

## Block now or 3.0.1? (non-binding view, asked by the dispatch)
**Recommendation: 3.0.1, as one low test item with L-2, and not a block on this merge.**
- **The merge is correct.** I verified the run by reading and by re-deriving the merge. It is the run colour-vision §3.1 specifies, in composition order.
- **Blocking would not stop anything shipping.** The gap is in the test and in four comments, not in the product. `develop` already carries the same blind test and the same false claim for colour-vision's own renumbering, which merged with it.
- **The failure the guard exists for is small and visible.** It is two Settings rows fading in together, and QA's device pass would see it.
- **Blocking has a cost.** It would hold licences behind a test rewrite in a file the merge did not cause to be blind, while licences must still land before candy's sitting.
- **If the tech-lead prefers to close it now,** the fix is test-only, plus the four comment and doc corrections. It touches no start-path file, so no sitting follows from it.

## Open issues / risks
1. L-1 and L-2 are unguarded orderings. Until the follow-up lands, review is the only guard on any future Settings row's entrance order and section placement.
2. Opus reviewed Opus code (Q-019). A Fable review stays owed whenever Fable reviews resume; the producer decides.

## Handoff
Next: tech-lead — re-read the `--cc` for the merge (colour-vision sign-off, Handoff), rule on L-1 and L-2 (block or 3.0.1; my view is above), and confirm that I-1's STATUS-only move of develop needs no further merge-in.
Next: producer — route the L-1 and L-2 follow-up and I-2's doc sync if the tech-lead accepts them.

VERDICT: PASS
