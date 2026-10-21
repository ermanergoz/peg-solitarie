# code-auditor report — 15-founder-feedback, slice S11, fix round 1 re-audit (loop 2)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (771d81b..7cc46bf; audited diff `cd25522..7cc46bf`: 771d81b, af89ff0, 14b1812, f074c13, 86bfecf and the report commit 7cc46bf)
Skills loaded: ergoz-soft:standards (role code-auditor: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; the plugin 0.6.2 copies were diffed over the product's 0.5.1 snapshot and read where they differ), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: the qa-engineer's device session on this slice (emulator-5560; no Gradle run here touched a device)
Loops: 1
Ran on: claude-opus-5-5 (Opus 5.5) at effort xhigh (the agent file's effort) — **STAFFING DEVIATION:** the agent file pins fable. The producer's dispatch states Fable is exhausted, so this ran on Opus under the founder's Q-003 answer (`.planning/questions/FOUNDER.md`). The author also ran on Opus, so independence rests only on this being a fresh agent. It received the requirements, the diff, its own loop-1 report and the qa-engineer's loop-1 report. It did not read `android-game-dev*.md` or `.planning/log/`. One exception: a repo-wide `git grep -i fredoka -- '*.md'` printed three lines of `android-game-dev-fix1.md`, each truncated to 200 characters. Nothing else of that file was read, and no finding relies on them. Every result below was produced by this role.
Fan-out: none
Lessons applied: none (the snapshot and the plugin memory hold 0 active lessons)

## Summary
The fix round closes all three loop-1 findings: every loop-1 mutation (F, F2, F3, B, H, H2) now turns the suite red. The 18d fix holds. `TimerDigits` keeps the pill width exactly constant in the shipped Fredoka at font scales 1.0 and 2.0, including when the face arrives late. It also keeps TalkBack on one node. The Fredoka fixture is test-only and carries its OFL notice. `./gradlew check --no-build-cache` is green (98/98 executed; 1,049 app and 274 domain tests).
**One new blocking finding (N-1):** splitting the timer into one-character cells made 18e's "not clipped" assertion vacuous. A pill that clips "59:59" at 200% to "59:5" now passes all 1,049 tests; before this round, the same pill failed 18e. **VERDICT: FAIL.** The fix is test-only.

## Outputs
- `.planning/reports/15-founder-feedback/code-auditor-loop2.md` — this report

## Checks
- `git worktree add ../PegSolitarie-audit-l2 7cc46bf` (`local.properties` copied), then `./gradlew check --no-build-cache` — **pass**: `BUILD SUCCESSFUL in 11m 25s` / `98 actionable tasks: 98 executed`.
  - App unit tests: 1,049 run, 0 failures, 0 errors, 0 skipped. That is 1,044 + 5 new: three dock pixel tests, 18d, and `TimerDigitsTest`.
  - Domain: 274 / 0 / 0 / 0. `:domain:depthSweep` ran.
  - Detekt: `0 number of total findings`. ktlintCheck: pass.
  - Lint: pass, with no issue in `TimerDigits.kt`, `GameStatusStrip.kt` or any new test file.
  - The worktree was removed afterwards (`git worktree remove --force`, then `prune`). `git status --short` was empty in it before removal, and is empty in `PegSolitarie-p15`.
- `git diff cd25522..7cc46bf -- . ':!.planning' | grep '^+' | grep -E '@Suppress|@Ignore|@Disabled|!!|baseline|SuppressLint|ktlint-disable|detekt:disable|@file:'` — 0 code hits; the one hit is the prose word "baseline" in a KDoc.
- Removed `@Test` lines: 0; added: 5. No build, Gradle, detekt, lint or baseline config was touched, and no dependency was added.
- The 18a/18e timer assertions were rewritten per cell, not deleted. Their vertical half still has teeth (mutation C below); their horizontal half does not (N-1).
- **Fixture:**
  - `shasum -a 256 app/src/test/resources/fixtures/fonts/Fredoka-Variable.ttf` = `2ba02e68…61a73fc8`, identical to the README's value and to QA's saved `Fredoka[wdth,wght].ttf`.
  - After the check, `find app/build -name 'Fredoka*'` finds it only under `intermediates/java_res/debugUnitTest/`.
  - The build's only `sourceSets` line (`app/build.gradle.kts:441`) adds the Room schema directory to debug assets, and nothing points a variant at test resources. `git ls-files app/src/main | grep -i -E '\.ttf|fredoka'` → 0.
  - `Fredoka-OFL.txt` is the full SIL OFL 1.1 (93 lines) under "Copyright 2016 The Fredoka Project Authors". It declares no Reserved Font Name, so the file rename is immaterial.
  - No release AAB was built, to keep the host quiet for QA's cold-start sitting. The source-set evidence is conclusive: AGP never packages `src/test/resources`.
- **Audit probe** (`AuditTimerProbeTest`, a scratch test placed only in the throwaway worktree and deleted before removal; never committed). `./gradlew :app:testDebugUnitTest --offline --tests '*.AuditTimerProbeTest'` — 8 tests. The a11y test was run once more with a corrected expectation: a node's child count includes cells hidden from accessibility, so visibility is the right read. It passed.

  | Probe | Result |
  |---|---|
  | Strip pill width in Fredoka Medium, 14 readings (00:00, 08:08, 11:11, 59:59, 22:22, 33:33, 44:44, 55:55, 06:06, 17:17, 28:28, 39:39, 60:00, 99:59) | **identical** at every reading: mdpi 86.0dp (1.0), 116.0dp (2.0); xxhdpi 85.67dp (1.0), 116.33dp (2.0) |
  | 100:00 and 999:59 | one cell wider, then flat: 100.0 / 137.0dp (mdpi), 99.67 / 137.33dp (xxhdpi), same at both readings (I-1) |
  | Late face at font scale 2.0 (xxhdpi) | before arrival 91.0dp vs present 92.33dp; after arrival **equal** (92.33dp) at all 14 readings |
  | TalkBack, via the view's `AccessibilityNodeProvider` | clock node: text `11:11`, description `Elapsed time`, visible, important. Its 5 cells: `isVisibleToUser=false`, `importantForAccessibility=false`. One node speaks. |
  | `LayoutDirection.Rtl` | cells read `05:12` left to right |
  | 2.0, w360dp, "Level 1234", 59:59 / 100:00 / 999:59 | every cell inside the clock node (e.g. cells 723–993px, node 719–996px) |

- **Mutation runs.** Each ran in the throwaway worktree only, via a script that asserts each replacement took and restores with `git checkout -- app`. Every run recompiled (`> Task :app:compileDebugKotlin`), and `git status --short` was empty after each.

  | # | Mutation (production code only) | Criterion | Result |
  |---|---|---|---|
  | F | `DockFill.Pale -> scheme.primary`; ring drawn whatever `recipe.ring` says | 21e, 21b | **red**: fill ("Undo, Available: the fill: expected #FFEDE4FF, read #FF7C3AED") and ring |
  | F2 | ring never drawn | 21b, 21e | **red**: "Show possible moves, Off: the ring: expected #FF635F69, read #FFF1EAFB" |
  | F3 | shadow ignores `lift` | 21b | **red**: "Undo, Unavailable: the slab under a flat disc is darkened by … 18" |
  | B | both insets re-added **after** `.testTag` | 18b | **red**: 18b (back chip at 48dp) and both 18c |
  | B-strip | status-bar inset after the tag only | 18b | **red**: 18b (back chip 48 > 32) |
  | B-dock | navigation-bar inset after the tag only | 18b | **red**: "where the dock slab's paint ends … 528.0", 18b only |
  | H | decorative `Icon(contentDescription = null)` between title and gear | 15n | **red**: all 5 `HomeHeroRowTest` (title end 280 vs gear start 304) |
  | H2 | 120dp decorative icon above the row | 15n, 15o | **red**: all 5 (row starts 136dp below the column top, expected 8) |
  | H3 | 24dp decorative icon above the row (loop 1 predicted a small emblem would pass) | 15n, 15o | **red**: all 5 (40 vs 8) |
  | C | loop-1 C: pill `height(40dp)`; `withTabularFigures()` removed | 18a, 18e | **red**: 18a (`tnum` null) and 18e ("cut short vertically") |
  | J | loop-1 J: on mark on every disc; slashed glyph always | 21e | **red**: 3 mark/slash tests; the moved render helper still reads true |
  | T1 | timer back to one plain `Text` | 18d | **red**: "the pill at 08:08 … expected 86.0, was 84.0" (18a, 18e red too) |
  | T2 | digit cell `remember(style)`-ed in composition | 18d (late face) | **red**: `TimerDigitsTest` "late clock at 08:08 … 61.67 vs 61.0" |
  | T4 | cell = widest digit **present**, not the face's widest | 18d | **red**: "the pill at 11:11 … expected 86.0, was 66.0" |
  | T6 | the clock node loses its `text` | 18a, 18e | **red**: both (`Key not present: Text`) |
  | T3 | `hideFromAccessibility()` removed from the cells | §11, 18 | **all 1,049 app tests green** (N-2) |
  | T5 | cells placed with `placeRelative` (mirrored in RTL) | §4.3.1 KDoc claim | **all 1,049 app tests green** (N-2) |
  | T8 | pill fixed at 80dp wide (`.width(game_timer_pill_height * 2)` at `GameStatusStrip.kt:207`) | 18e | **all 1,049 app tests green** (N-1); the probe confirms the clip |

## Findings

**N-1 — MAJOR (blocking). 18e can no longer see a horizontally clipped timer. The fix round turned an assertion that could fail into one that cannot.**
- Tests: `GameStatusStripTest.kt:248` applies `assertTimerWhole` to each cell. Its horizontal check at `:379` compares a cell's line against that cell's own layout width. `:252-253` bound the clock inside the pill vertically only.
- Production cause: each cell is one character measured at unbounded width (`TimerDigits.kt:91`), so none can wrap, ellipsise or overflow its own layout. The clock then clamps its reported width to the incoming constraint without a trace (`TimerDigits.kt:98`).
- Before this round, the timer was one `Text` measured at the pill's width. A pill too narrow for the clock wrapped it, and 18e's `lineCount == 1` failed.
- Failure scenario (mutation T8): the pill is given a fixed 80dp width, the obvious alternative "fix" for 18d's jitter. At font scale 2.0 on w360dp, the clock's cells span 92dp in a 56dp content box. The clock node is 828–996px, and its cells run past the pill's clipped edge at 1032px. The last "9" is wholly clipped (its bounds collapse to zero) and the "5" before it is cut, so "59:59" reads "59:5". All 1,049 app tests stay green, including all 7 in `GameStatusStripTest`.
- The failure mode of the production code changed with it: a pill too narrow for the clock used to wrap the time, whole; it now clips it silently. With today's layout this needs a window narrower than about 212dp at 200% (233dp from 100:00), so no current device shows it (probe row 6).
- Criterion 18e: "the timer's text is fully visible — not clipped, not ellipsised — in a pill that has grown to hold it". Q-003's mitigation tells reviewers to hunt for assertions that cannot fail, and the dispatch asks that no test be weakened.
- Fix (test-only, the developer's): at 2.0, also assert the horizontal half. Every cell's `boundsInRoot` must lie inside the clock node, and the node inside the pill; or assert that the node's width equals the sum of its cells' widths (i.e. unclamped). Watch it go red against T8 as quoted. Making `TimerDigits` honour `constraints.maxWidth` is optional and not required by the spec.

**N-2 — MINOR. Two properties `TimerDigits` re-implements, and its KDoc promises (`TimerDigits.kt:49-60`), have no test.**
- One accessibility node: `:82` hides each cell. Left-to-right order in every direction: `:101` uses `place`.
- Both are correct as shipped (probe rows 4 and 5), but mutations T3 and T5 each leave all 1,049 tests green:
  - **T3:** the five cells become visible, important nodes that TalkBack can reach one by one ("1", "1", "colon", "1", "1"). This is the dispatch's "digit by digit" case.
  - **T5:** on an RTL device (`AndroidManifest.xml:29` has `supportsRtl="true"`), "05:12" draws as "21:50".
- Suggested in the same pass:
  - an unmerged-tree assertion that every child of the clock node carries `SemanticsProperties.HideFromAccessibility`, or that the view's node provider reports it `isVisibleToUser=false`;
  - one `LayoutDirection.Rtl` render asserting that the cells' left edges follow the string's order.
- Not blocking: no stated criterion fails under either mutation. Criterion 18's "announced as the elapsed time" still holds under T3.

**I-1 — INFO (ux-designer).** From 100:00 the pill steps once, by one digit cell: +14dp at 1.0, +21dp at 2.0. It is then flat again through 999:59. Elapsed time is uncapped (`GameTimerFormat.kt:19-23`), as it was before this diff, and 18d's range ends at 59:59. It only matters if hour-plus sessions are in scope.

**I-2 — INFO (qa-engineer, device).** The clock node carries the description "Elapsed time" and the text `11:11`, the same shape the old `Text` had, and §4.3.1 prescribes that unchanged description. TalkBack generally speaks a description in preference to text, so the value itself may not be spoken. This is unchanged by this diff and is not a finding against it. Worth one listen in the next TalkBack pass.

**I-3 — INFO (qa-engineer).** `TimerDigitsTest` makes its own host recompose when the late face lands (`TimerDigitsTest.kt:77`); its KDoc says Robolectric does not lay text out again by itself. The test proves the layout's own part: T2 is red. It does not prove the device's relayout trigger. 18d remains a device criterion, and QA's run on a physical phone where Fredoka resolves governs (QA loop-1 F-1 handoff).

**I-4 — INFO (release-manager / tech-lead).** `THIRD_PARTY.md` does not exist on this branch. The draft on `feature/phase-14-release-docs` has a Fredoka row (line 82) reading "not bundled in the artifact", which stays true. When the two branches meet, that row could note the repository's unmodified, test-only copy at `app/src/test/resources/fixtures/fonts/` and its OFL notice. OFL-1.1 permits the redistribution with the notice, and the notice sits beside the file. This is not a defect.

## What holds (verified, not taken on trust)
- **F-1 closed.** The fill, ring and shadow probes read every control in every reachable look, against §4.4.1's table typed into the test, not read from the recipe. F, F2 and F3 each turn them red. The flat-versus-raised split uses a hardware-rendered copy, and the shadow read is deterministic across all runs here.
- **F-2 closed.** 18b now reads the back chip and the slab's painted bottom. B, B-strip and B-dock are each red on 18b.
- **F-3 closed.** The hero row is checked through layout: the title and gear tile the row, and the row sits at the column's top. H, H2 and the smaller H3 each fail all five tests.
- **18d fixed.** The pill width is exactly constant across every same-length reading, at both font scales, both densities, and with a late face. The four named readings are held by a JVM test whose first assertion proves Fredoka's digits are proportional in that render (so it cannot pass on the fallback). T1, T2 and T4 are each red.
- **Code standards.**
  - `TimerDigits` is internal and stateless. Its parameters are stable (`String`, `TextStyle`, `Color`, `Modifier`), and it has private light and dark `@Preview`s.
  - Constants are named (`DECIMAL_DIGITS`, `PREVIEW_*_SECONDS`). There are no new dp literals or colours.
  - Modifier order is unchanged in `TimerPill`.
  - Per tick, five one-character cells recompose, and the ten digit measures are served from a 10-entry `TextMeasurer` cache. Negligible.
- **Security.** No network, secret, permission, SDK call or dependency. The only new binary is the test-only font described above.
- **Approved suppressions:** none requested, none added.

## Open issues / risks
- N-1 blocks the PASS. N-2 is recommended in the same test pass.
- I-2 and I-3 are device observations for the qa-engineer. 18d on a phone where Fredoka resolves is still owed and governs. The decided 15e cold-start sitting is still owed (QA loop 1).
- Q-003 deviation: the reviewer and the author share a model. This report's evidence is the mutation and probe runs above, not agreement with the author.

## Handoff
Next: android-game-dev — add 18e's horizontal half as N-1 describes (cells inside the clock node and the node inside the pill at 2.0, or an unclamped-width assertion), and watch it go red against T8 as quoted. Optionally close N-2 (cells hidden from accessibility; RTL order) in the same pass. No production change is required. Then code-auditor re-audits the test delta; the PASS route to tech-lead waits on that. Through the producer: qa-engineer takes I-2/I-3, ux-designer I-1, and release-manager/tech-lead I-4.

VERDICT: FAIL
