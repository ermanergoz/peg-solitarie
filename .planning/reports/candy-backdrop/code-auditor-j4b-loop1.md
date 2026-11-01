# code-auditor report — candy-backdrop: J-4b loop 1, re-check of F1 and the D5 line
Date: 2026-11-01
Branch: feature/candy-backdrop (eef1f46..fd45fd0; reviewed at HEAD `e3f8e5c`, whose `app/` tree equals `fd45fd0`'s. `14ce6d8` (ux-designer, B-11c) and `e3f8e5c` (the developer's report) are docs only.)
Skills loaded: ergoz-soft:standards (role code-auditor). The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus `.claude/rules/lessons.md`. Then code-reviewer, security-reviewer, compose-skill and compose-performance-audit.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation, not covered (Open issue 1).** `agents/code-auditor.md` pins fable, and the developer wrote these tests on opus. Q-019's answer 3 allowed opus reviewers. Its 2026-10-23 addendum moves supervising roles back to their pinned model, so it no longer covers this run.
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **F1 is closed.** `withTheGateAlreadyOpenTheSceneIsWholeFromItsFirstFrameAndNeverFades` (`BackdropSceneOpenTest.kt:137`) fails on ME and passes on the head. It also catches ME2, a re-fade at an open gate, through its every-frame loop.
- **The D5 line holds.** `BackdropReducedMotionTest.kt:147-149` fails on the late clean-up M8 (4 frames). It also fails on M8b, a delay of only one frame, so the bound is exact.
- **No production change.** `git diff 0c45666..HEAD -- app/src/main` is empty. Only two test files changed, and the commits only add lines.
- **The ux-designer's BD-23 (d) request is met.** The animations-off and fade-duration tests are unchanged since J-4b, and each still fails on its own mutant (MAO and MF).
- Technical result: PASS. Because of the staffing issue, the producer must decide whether this counts as the fable gate (Open issue 1).

## Findings
None. Notes on the diff:
- `eef1f46` asserts the scene's alpha on the first frame and on every frame through `framesFor(BACKDROP_SCENE_FADE_MS)`. This is exactly the fix F1 asked for. The test reads the same reader lambda that the layer calls, so it pins what is drawn.
- `fd45fd0` adds one frame and one assertion before the existing "a cross-fade later" checks.
  - Its pixel comparison still compares frame 2 against a fully settled catalogue. The settled capture now starts one frame later, which does not loosen the comparison.
  - No existing assertion was removed or relaxed.
  - This closes my J-4b INFO 2.

## Mutants (throwaway `git worktree add --detach e3f8e5c` under the scratchpad, removed afterwards)
Each run was `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests '…BackdropSceneOpenTest' --tests '…BackdropReducedMotionTest'`, which covers 12 tests. I applied each mutant to `SceneBackdrop.kt` with a script that checks each target string occurs exactly once. After each run, `git checkout -- app` restored the file, and the batch ended with "restored".

| Id | Mutation | Result |
|---|---|---|
| base | none, re-run with `--rerun` | pass, 8 of 8 and 4 of 4 (XMLs 09:12Z) |
| ME | `:163` `Animatable(SCENE_HIDDEN)` | **red**, 1: the new test, "the scene's alpha on the first frame / expected: 1.0 / but was : 0.0" |
| ME2 | `:165` the open-gate guard dropped and `fadeIn.snapTo(SCENE_HIDDEN)` added (a re-fade at an open gate) | **red**, 1: the new test, same message |
| M8 | `repeat(4) { withFrameNanos { } }` after the cross-fade's `animateTo` (late slot clean-up) | **red**, 1: `turningAnimationsOffMidCrossFadeSettlesItWithinTwoFrames`, "scenes still composed on frame 3 after the switch / expected to be empty / but was: [1.0]" |
| M8b | the same clean-up, delayed by one frame only | **red**, 1: same test, same message |
| MF | the scene fade's tween halved | **red**, 1: `theSceneOpensAtTheCascadesEndAndFadesInOverItsToken`, "alpha 64 ms after the open … expected 0.0914 but was 0.1371" |
| MAO | animations off ignored (the reader returns `fadeIn.value` and the fade always animates) | **red**, 1: `withAnimationsOffTheSceneOpensOnTheSecondFrameWithNoFade`, "the scene's alpha on frame 3 / expected: 1.0 / but was : 0.0" |

## Checks
- `git diff 0c45666..HEAD -- app/src/main | wc -c` — pass (0). `git diff --name-only 0c45666..HEAD -- app` lists only `BackdropReducedMotionTest.kt` and `BackdropSceneOpenTest.kt`.
- `git diff 0c45666..HEAD -- …/BackdropSceneOpenTest.kt`, filtered to removed lines — pass: none were removed. The animations-off test (`:103-134`) and the fade-duration test (`:60-101`) are as reviewed at J-4b.
- `git diff --name-only fd45fd0..HEAD` — `.planning/` reports and `docs/ux/candy-backdrop.md` only.
- `git diff 0c45666..HEAD -- app | grep -nE '^\+.*(@Suppress|@Ignore|baseline|!!|Thread\.sleep|System\.currentTimeMillis|Random\()'` — pass (no output). **No suppression to approve.** There are no new dependencies, and no build or `THIRD_PARTY.md` change.
- The mutation batch and the base re-run — as tabled above. The 1-minute load before each run was 2.39–5.81, below 12. No emulator was used, and `./gradlew --stop` was never run.
- I did not run the full Check. Production is unchanged. The developer's Check at `fd45fd0` (1,641 tests, 0 failures, `android-game-dev-f1.md`) covers the same `app/` tree as HEAD, and my targeted runs agree with it.
- `git worktree list` no longer shows the throwaway worktree. I did not read `.planning/log/`.

## Open issues / risks
1. **Model.** This re-check ran on opus, the author's model, which breaks rule 1 (code is reviewed on a superior model). Q-019's 2026-10-23 addendum returns review roles to fable. My J-4b review (`code-auditor-j4b.md`) cited Q-019 answer 3 without that addendum, so it has the same problem. Producer: either re-dispatch the J-4b review chain on fable, or queue the founder question. Do not count this report as the fable gate on your own authority.
2. INFO: the R-1 undo (MD) is still caught by no test. That is unchanged, and §16 #28 holds it by review.
3. INFO: `Bitmap.differingPixels` is still duplicated (J-4b INFO 5).
4. This report's commit carries only the studio trailer that the plugin's gitflow requires. I did not add the harness's attribution lines.

## Handoff
Next: producer — settle Open issue 1. If you accept this run, F1 and D5 are closed, and the tech-lead's sign-off addendum can proceed.
Next: tech-lead — the D5 line is in and exact (M8 and M8b are red).

VERDICT: PASS
