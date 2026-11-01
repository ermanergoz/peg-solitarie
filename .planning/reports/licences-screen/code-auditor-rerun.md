# code-auditor report — licences-screen (re-review of the fix for my two re-run findings)
Date: 2026-10-31
Branch: feature/licences-screen (report only) — reviews `9b6d232` (the fix, one test-only commit over `040ddcc`); head `9595c63`, whose code is identical to `9b6d232`
Skills loaded: ergoz-soft:standards (role code-auditor; the plugin rule files read in full in loop 0 of this step: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort high. **Deviation:** my agent file pins fable at xhigh. The founder's Q-019 answer puts every Fable-pinned role on Opus, and the founder's directive of 2026-10-31 sets every role one effort step lower. Q-019's mitigation applies: I ran the Check and both mutations myself, and trusted no recorded result.
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Both findings from loop 0 are closed.** The fix (`9b6d232`) changes only `LicencesRtlTextDirectionTest.kt`, and no production file.
- **M8 is now caught.** Removing the top bar's `LayoutStart` wrapper turns `theChromeStillMirrorsInARightToLeftLayout` red, by 53.7 px.
- **M11 is now caught.** Switching `ContentOrLtr` to `Content` turns 4 tests red. The two LS-AC19 direction tests are categorical reds (`Rtl` against `Ltr`). Those two are what close finding 2.
- **The 1.40 px edge margin under M11 is an artifact, not a guard.** It clears the 1 px tolerance by only 0.40 px. I did not count it. The direction tests already guard the value, so this needs no change.
- **The forced Check is green:** 1,359 `:app` tests, 0 failed.
- **VERDICT: PASS.**

## Finding 1 (the top bar title's edge) — closed
- **What changed.**
  - The notice fixture's name is now `WRAPPING_TITLE`, "AndroidX lifecycle viewmodel savedstate library". It is the shipped index's longest name, which I confirmed in loop 0.
  - `assertLinesHangFrom`'s `wrapped` set is now required, no longer defaulted. So no call site can drop a wrap record without anyone noticing.
  - The RTL chrome test asserts that `wrapped` contains `WRAPPING_TITLE`. The LTR guard's `containsAtLeast` now includes it too.
- **M8, re-run by me** (top bar `ProvideLicencesText(LayoutStart)` replaced with `run`): red in 1 of 36 tests.
  - `theChromeStillMirrorsInARightToLeftLayout`: `line 1 of "AndroidX lifecycle viewmodel savedstate library": its right edge, against 355.0 px … but was 53.695312`.
  - That margin is 53 times the tolerance. This matches the developer's "M8 red in 1 test (RTL)".
- **The LTR guard stays green under M8, and that is correct.** Without the wrapper, the text screens' title takes `TextStart`, which is `Start`. In an LTR layout, `Start` equals `Left`, so nothing visible changes. The guard's job is LTR regressions, and it now also proves the title wraps in LTR.

## Finding 2 (`ContentOrLtr` unpinned) — closed
- **What changed.**
  - `DASH_RULE` is `------------------`, an 18-dash rule. My loop-0 scan found it among the shipped no-strong-character paragraphs, for example in `c15ad8d2096909c1`.
  - It is appended to the Fredoka and notice-text fixtures, as `fredokaBody` and `noticeBody`. It is in the notice text's scroll targets.
  - Both text-screen LS-AC19 tests assert it among the kinds checked, as "a paragraph with no strong character". So the fixture cannot drop it and still pass.
- **M11, re-run by me** (`TextDirection.ContentOrLtr` changed to `TextDirection.Content`): red in 4 of 36 tests. This matches the developer's count.
  - `everyTextOnALicenseTextIsLaidOutLeftToRightInARightToLeftLayout` and `everyTextOnAGoogleNoticeTextIsLaidOutLeftToRightInARightToLeftLayout`: `"------------------": … expected: (Content, Ltr) but was : (Content, Rtl)`.
  - `everyLineOfALicenseTextHangsFromTheLeftEdge…` and `everyLineOfAGoogleNoticeTextHangsFromTheLeftEdge…`: `line 1 of "------------------": its left edge, against 16.0 px … but was 1.4000168`.

### Judging the 1.40 px edge margin
- **Why it is so small.** Under `Content`, the rule's paragraph resolves to RTL, so `TextAlign.Start` puts its line at the right of its own box. But an 18-dash rule is one short line. Its box is only about as wide as the line, and D3's `AbsoluteAlignment.Left` puts the box at the column's left edge.
  - So the line ends up only 1.40 px from where it should be.
  - That leftover is the box's slack around the line. It depends on font metrics and rounding, and says nothing about the misdirection itself.
- **It is fragile.** It clears the 1 px tolerance by 0.40 px. A change of font, density or rounding could bring it under 1 px, and the two edge tests would then pass under M11. So I do not count those two reds as a guard.
- **The guard is the direction oracle, and it is sound.**
  - `getParagraphDirection(0)` on the re-measured layout is categorical: `Rtl` against `Ltr`, with no tolerance.
  - It fires on two screens, curated text and notice text.
  - On the head, the dash rule resolves `Ltr`, and its line starts exactly at the left edge. So the green is not marginal either.
  - LS-AC19 is the criterion that pins direction, and these are its tests. That closes finding 2.
- **The edge oracle does bite on the shipped rule.** I ran the same two tests with `DASH_RULE` set to the shipped 67-dash rule, the commonest no-strong paragraph (252 of the 318).
  - It wraps on the 411 dp window. On the head all 11 tests are green.
  - Under M11 the same 4 tests are red, and both edge tests fail by a wide margin: `line 2 of "-------------------------------------------------------------------": its left edge, against 16.0 px … but was 341.8`.
  - A 78-character `=` rule behaves like the short one (1.80 px).
  - This also corrects my loop-0 wording. A short rule under `Content` moves by only about 1.4 px on the device. The visible defect is a wrapped rule's second line hanging from the right.
- **Advisory, not owed.** Setting `DASH_RULE` to the shipped 67-dash rule would make the edge tests a second robust guard. It would also put the defect's visible form (a wrapped line hanging right) under test. It is one constant, and no assertion changes. I do not make it a finding, because the direction tests already guard the ruled value categorically.

## The rest of the fix
- **Scope.** `git diff 9b6d232~1 9b6d232 --name-only` lists only the RTL test file. It changes fixtures, adds coverage and wrap assertions to existing tests, and makes one helper parameter required. No existing assertion is removed or loosened.
- **Suppressions.** None: no `@Suppress`, `@Ignore` or baseline.
- **Fixture side effects.**
  - The longer notice name also reaches `theErrorTextsAreLaidOutLeftToRightInARightToLeftLayout` (the failed notice text keeps its header). Its title is then a wrapping text, checked like any other, and the test passes.
  - Neither the width-cap tests nor the TalkBack tests use `NOTICE`. It is file-private.
- **The KDocs on `WRAPPING_TITLE` and `DASH_RULE` are accurate.** Both values are shipped data, as they say.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor-rerun.md` — this report, committed alone.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor-rerun-loop0.md` — the loop-0 report, renamed with `git mv` and committed alone (`9595c63`).
- No source file edited. The mutation and fixture runs were in a throwaway `git worktree add --detach` of `9b6d232` in the scratchpad, under a unique name, and removed afterwards. It held no file I did not write.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain :app:compileDebugKotlin --rerun :app:compileDebugUnitTestKotlin --rerun :app:testDebugUnitTest --rerun :app:detekt --rerun :app:runKtlintCheckOverMainSourceSet --rerun :app:runKtlintCheckOverTestSourceSet --rerun :app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun` at `9595c63` (clean tree; code identical to `9b6d232`), 20:03:30Z to 20:14:43Z — **pass**.
  - Output: `BUILD SUCCESSFUL in 11m 12s`, `107 actionable tasks: 19 executed, 1 from cache, 87 up-to-date`, exit 0.
  - Every forced task printed as executed, and `:app:check` ran. The one cache hit is the ktlint lifecycle task `ktlintTestSourceSetCheck`; its worker task `runKtlintCheckOverTestSourceSet` executed.
  - `:app` tests: 1,359, 0 failed, 0 skipped. Classes started 20:04:45Z to 20:08:51Z. `LicencesRtlTextDirectionTest`: 11, 0 failed.
  - Lint: `0 errors, 64 warnings`. detekt: 0 issues.
- `./gradlew :app:testDebugUnitTest --max-workers=4 --console=plain --offline` with the four suites (`LicencesRtlTextDirectionTest`, `ContentColumnWidthCapTest`, `LicencesTalkBackContractTest`, `SettingsScreenTest`) in the throwaway worktree:
  - baseline: 36 tests, 0 failed;
  - M8: 1 failed;
  - M11: 4 failed, as quoted above.
- The same command on `LicencesRtlTextDirectionTest` alone, with `DASH_RULE` set to the shipped 67-dash rule:
  - head: 11 tests, 0 failed;
  - M11: 4 failed, the edge tests at 341.8 px.

  The same with the 78-character `=` rule: head 0 failed, M11 4 failed (edge 1.80 px).
- A Python scan of the shipped notice and licence texts: 12 distinct no-strong-character paragraphs, 318 occurrences, all rules of 18 to 78 dashes or `=`, and none with digits.

## Open issues / risks
- Advisory only: the shipped 67-dash rule as `DASH_RULE` would make the LS-16 edge tests a robust second guard on `ContentOrLtr`. See the judgement above.
- The device half of LS-15 and LS-16 is still QA's re-check (the ruling's items 1–4, as amended).
- Staffing: an Opus review of Opus code (Q-019). The forced Check and my own mutation runs are the mitigation.

## Handoff
Next: producer — the code-auditor's PASS for the licences re-run fixes (`1b79094..9b6d232`, with `6e3d5e0`'s hunks). After QA's re-check passes, it goes to the **tech-lead** for sign-off: the merge-in hunks, departures D1–D9 (all accepted in loop 0), and this loop's test-only fix.

VERDICT: PASS
