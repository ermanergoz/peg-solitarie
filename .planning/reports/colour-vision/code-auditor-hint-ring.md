# code-auditor report — colour-vision (delta review of the hint-ring fix `fddc8e8`, merge ruling item 1)
Date: 2026-11-01
Branch: feature/colour-vision (fddc8e8..69cdc0d). The code under review is `fddc8e8` alone. `69cdc0d` adds only the developer's report: `git diff --quiet fddc8e8 69cdc0d -- . ':(exclude).planning'` exits 0
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing, plus the snapshot's `lessons.md`. Also code-reviewer, security-reviewer, compose-skill and compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5. The harness names no effort; the pin is xhigh, and the founder's 2026-10-31 directive (1) runs this dispatch one step below it, at high. **Staffing deviation, recorded:** `agents/code-auditor.md` pins fable. Q-019's answer (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published, so this review shares the developer's model. **Mitigation:** I ran my own Check with `--rerun-tasks` (104 of 104 tasks executed), reproduced the red myself, and ran three mutation probes in a throwaway worktree, since removed. No count below is taken from the developer's report.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/colour-vision/tech-lead-merge-ruling.md`; `.planning/reports/colour-vision/android-game-dev-hint-ring.md`. Read in part: my prior `code-auditor-merge-home.md` (header, Summary, Checks); `tech-lead-brief.md` §11 (the tripwire); `.planning/questions/FOUNDER.md` Q-019
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **`fddc8e8` meets Ruling 1's constraints 1–6.** It touches only `BoardCanvas.kt`, `BoardLook.kt` and the one test file.
  - The frame path builds its strokes through `hintRingStroke`, the same builder the `remember` uses. The width is the same `ringWidthPx`, and the dash count comes from the same `look.recipe`.
  - It builds new strokes only when all four conditions hold: the frame is unsized, the board is interactive, a hint is drawn, and the rings are dashed.
  - A sized frame allocates nothing new.
  - The wrong-tap `markStroke` is untouched, and no suppression, `!!` or constant was added.
- **Red reproduced: 547 px.** I put the two main files back at `6a6e82d` and kept the new test. The new case fails with "expected: 0 / but was : 547", the ruling's number exactly. With the fix restored, the class is 4 of 4.
- **My Check is green.** `./scripts/check.sh --wait --max-workers=4 --rerun-tasks` exits 0 with 1,666 tests and 0 failures. Every test the ruling names passes. The compile warnings are 52, all from `src/test`, 0 from `src/main`. The tripwire is unchanged.
- **No finding the code must fix.** There are four informational items. **I recommend keeping `fddc8e8`.** The fallback is not triggered, so no revert is needed and no R-CV-1 is recorded for 3.0.1.
- **VERDICT: PASS.**

## Outputs
- `.planning/reports/colour-vision/code-auditor-hint-ring.md`: this report, the only file written, committed alone. No source file was edited. The probes ran in a throwaway `git worktree add --detach` at `fddc8e8`, under the session scratchpad, outside the repo. That worktree is removed and pruned (`git worktree list | grep -c hr-audit` gives 0).

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-cv` unless a probe is named. No emulator was started and no APK was built.

**Host and lock**
- Before my Check, `uptime` showed a 1-minute load of 13.09, so I waited. It fell to 9.89 and then the Check started. The probe runs started at loads of 3.34–8.34.
- `.gradle/ergoz-build.lock` was absent, and `git status --short` was empty before the run.

**My Check — pass**
- `./scripts/check.sh --wait --max-workers=4 --rerun-tasks`:
  - result: `BUILD SUCCESSFUL in 11m 56s`, `104 actionable tasks: 104 executed`, `CHECK-EXIT=0`;
  - `:app:detekt` reported `0 number of total findings` (`app/build/reports/detekt/detekt.md`).
- JUnit XML of that run, each with 0 failures, 0 errors and 0 skipped:

  | Module | Suites | Tests |
  |---|---|---|
  | `:app` | 141 | 1,315 |
  | `:domain` test | 33 | 294 |
  | `:domain` fuzz | 1 | 1 |
  | `:domain` depthSweep | 1 | 1 |
  | `:build-logic` | 6 | 55 |

**Named tests (Ruling 1, "Green means") — pass**
- Tests and failures from my run's XML:

  | Test class | Tests | Failures |
  |---|---|---|
  | `BoardCanvasFirstFrameTest` | 4 | 0 |
  | `BoardCanvasPaletteTest` | 3 | 0 |
  | `BoardMarkMotionTest` | 2 | 0 |
  | `ColorblindPaletteLiveSwitchTest` | 1 | 0 |
  | `HomePaletteIdentityTest` | 2 | 0 |
  | `HomeChipArrivalTest` | 13 | 0 |
  | `BoardCanvasWrongTapReducedMotionTest` | 6 | 0 |
  | `BoardCanvasWrongTapLiveMotionTest` | 2 | 0 |
  | `BoardCanvasTest` | 17 | 0 |

- `theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes` is among the four `BoardCanvasFirstFrameTest` cases, and it passes.

**Compile warnings — pass**
- `grep '^w: file' check.log | sort -u | wc -l` gives 52. All 52 match `/src/test/`, and 0 match `src/main`.
- The log has 0 `e:` lines.
- The one other `w:` line is Gradle's "Detected multiple Kotlin daemon sessions", which is not a compiler warning.

**The red, reproduced — fail as intended**
- In the probe worktree I ran `git checkout fddc8e8^ -- …/BoardCanvas.kt …/BoardLook.kt`, keeping the new test, then `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…BoardCanvasFirstFrameTest'`.
- Result: `4 tests completed, 1 failed`. The failing case is the new one, with "pixels inside board_canvas that differ between the first frame and the settled one / expected: 0 / but was : 547" (`BoardCanvasFirstFrameTest.kt:163`).
- `git checkout fddc8e8 -- app` and the same command — pass, `BUILD SUCCESSFUL` (the test task executed).

**Mutation probes**
Each probe is one edit of `drawHintHighlight` at `fddc8e8`, with the same command. The test task executed each time, not from cache.
- **M1: the two frame strokes' radii swapped.** Red: "but was : 575". The test pins each ring to its own radius.
- **M3: the source ring kept on the palette's stale stroke, the target restroked.** Red: "but was : 322". The test pins both rings, not just one.
- **M2: the `unsizedFrame &&` guard dropped,** so the rings are restroked on every frame. Green. This is expected: a pixel test cannot see an allocation, so constraint 4 rests on reading the diff (below, and item I-1).

**Constraint 1 (files) — pass**
- `git show --stat --format= fddc8e8` lists `BoardCanvas.kt`, `BoardLook.kt` and `BoardCanvasFirstFrameTest.kt` only.
- No `BoardGeometry.kt`, `DashedRing.kt` or start-path file is touched.

**Constraint 2 (one builder) — pass, by reading**
- The frame path calls `hintRingStroke(geometry.hint{Target,Source}RingRadius, palette.ringWidthPx, hintRingDashes)` (`BoardCanvas.kt` `drawHintHighlight`).
- `palette.ringWidthPx` is the same `ringWidthPx` that keys the `remember` (`BoardLook.kt:57,60-66,83`).
- `hintRingDashes` is `recipe.hintRingDashes`. `recipe` is `look.recipe` (`BoardCanvas.kt:114`), the same object `rememberBoardLook` built both strokes and `hintRingsDashed` from (`BoardLook.kt:52,61,65,92`).
- So the frame path and the `remember` path cannot drift. At a settled size the frame geometry equals the composed one (`forCanvas(spec, size, recipe)` with `size == canvasSize`), so its strokes equal the palette's, and the green test confirms this.
- `hintRingStroke` is now `internal`, with no other change to its body.

**Constraint 3 (only where it is drawn) — pass, by reading**
- `unsizedFrame = geometrySize != size` is develop's comparison, now named.
- `drawHintHighlight` is reached only under `interactive && hintSource != null && hintTarget != null` (`drawBoard`, unchanged).
- Inside it, `restroke = unsizedFrame && palette.hintRingsDashed`.
- Read-only boards, including Home's, never reach `hintRingStroke`. The standard palette keeps its solid `Stroke(width)` strokes.

**Constraint 4 (no steady-state allocation) — pass, by reading**
- On a matched frame, `restroke` is `false` and both branches pick `palette.hint{Target,Source}Stroke`.
- The new arguments are `unsizedFrame` (a `Boolean`), `recipe` (an existing reference, already captured by the draw lambda for `forCanvas`) and `recipe.hintRingDashes` (an `Int` field read). None of these boxes or allocates.
- `geometrySize != size` compares two `Size` value classes through the unboxed `equals-impl0`, as the old `==` did.
- `BoardLook` and `BoardPalette` are not rebuilt, and no `Pair`, `Stroke` or lambda is added on that path.

**Constraint 5 (wrong-tap `markStroke` untouched) — pass**
- `git show --format= fddc8e8 | grep -n 'markStroke\|drawWrongTapMark'` prints nothing (exit 1).
- `drawBoard` still draws `palette.markStroke`.

**Constraint 6 (no suppression, `!!` or new constant) — pass**
- `git show --format= fddc8e8 -- app | grep -nE '^\+.*(@Suppress|@SuppressLint|@Ignore|!!|ktlint-disable|@Disabled|baseline)'` prints nothing (exit 1).
- `BoardCanvas`'s single `@Suppress("LongMethod")` is unchanged, and no number or constant is added.
- `git show --format= fddc8e8 -- app/src/test | grep -cE '^-[^-]'` gives 0, so no existing test line changed.

**Tripwire (brief §11), against `develop` = `305f741` = `git merge-base develop HEAD` — pass**
- Line 1 prints the same seven files: `MainActivity.kt`, `ui/theme/AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt` and `Theme.kt`.
- Line 2 prints nothing (exit 1).
- The fix adds no start-path file.

**Commit — pass**
- `git log -1 --format=%s fddc8e8 | awk '{print length}'` gives 64.
- The last line is the studio trailer alone (rules/gitflow.md, plugin 1.0.0).

**Security**
- The diff adds no I/O, network, input parsing, secret or dependency. There is nothing for the security-reviewer checklist, and `THIRD_PARTY.md` is untouched, as nothing new is used.

## Findings
None the code must fix.

Informational, none blocking:
- **I-1: constraint 4 is held by reading, not by a test.** Mutation M2, which restrokes on every frame, stays green. No pixel test can see an allocation, so this is expected. I read the matched-frame path above and found no new allocation. Nothing is owed.
- **I-2: no resize-frame case.** The ruling makes it optional. M1 and M3 show that the first-frame case pins each ring's own radius through the same `geometrySize != size` branch a resize frame takes. Only the "dashes close at the top seam after a resize" half is unpinned by a test, as the developer's Open issue 2 says.
- **I-3: confirms the developer's Open issue 3.** `drawSelectedPeg` builds `Stroke(width = palette.ringWidthPx)` on every frame a peg is held (`BoardCanvas.kt:583`).
  - It is develop's code, not this commit's. `git show 305f741:…/BoardCanvas.kt` has it at line 593, along with the two per-frame hint `Stroke`s at lines 619 and 625 that this branch had already removed.
  - It is a 3.0.1 candidate for the tech-lead, outside Ruling 1.
- **I-4: a style note.** `drawBoard` takes the whole `recipe` but reads only `hintRingDashes` from it. This is harmless: no allocation, and it matches the ruling's "the composed recipe's `hintRingDashes`". No change is asked.

## Open issues / risks
- **The staffing deviation (Q-019).** The reviewer and the author are both on Opus, which is below rule 1's "superior model". The mitigations are listed in the header. The fable re-review at phase end, if the founder wants one after 3.0.0, is the producer's call.
- **The ruling's Open issue 1** (develop's wrong-tap `markStroke` on a frame geometry) and **I-3** stay 3.0.1 candidates. Neither is this branch's.

## Handoff
Next: producer —
- `fddc8e8` passes the delta review. **Keep it.** Ruling 1's fallback is not triggered, so there is no revert and no R-CV-1.
- Arm B for QA is the branch head after this report's commit. Its code tree equals `fddc8e8`'s.
- Dispatch the qa-engineer under Ruling 2: the smoke on `ergoz_studio` first, then the 15e sitting on `Pixel_9a_bench`.
- Then dispatch the tech-lead for the sign-off, citing this report.

Next: tech-lead — at the sign-off, re-read `fddc8e8` against constraints 1–6 (all pass here), and decide I-3 and the developer's Open issue 3 for 3.0.1.

VERDICT: PASS
