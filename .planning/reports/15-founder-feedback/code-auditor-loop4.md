# code-auditor report — phase-15 sign-off condition H-1: S10, H-2's delta and the develop merge-in
Date: 2026-10-18
Branch: feature/phase-15-ux-refresh (a4d00ab..ee16a2a; audited: S10 `a4d00ab`, `90633e8`, `098b403`, `49db2ff` as they stand at HEAD; H-2 `04b6785..3b27012`; merge `ee16a2a`) — report only, committed by the producer
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus the product's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: candy-backdrop spec rewrite (ux-designer, docs only), tech-design edits (tech-lead, docs only)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable; this ran on Opus, the author's model, under the founder's Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), `.planning/questions/FOUNDER.md`.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

VERDICT: FAIL

## Summary
- **Deviation first:** this audit ran on Opus, not Fable, under Q-019. To compensate, I ran every check myself and trusted no recorded result. I proved each guard with a mutation in a throwaway worktree. I read none of the developer reports and nothing under `.planning/log/`.
- **The production code is sound.** S10's two-axis threshold and compact pill are sound, H-2's status-bar fix is sound, and the merge resolved no code conflict. MB-1 and A-10 are closed. The Check is green: 98/98 tasks executed; app 1,064, domain 274 and build-logic 54 tests, 0 failed, 0 skipped. Every guard the sign-off named goes red under its mutation.
- **FAIL on three test defects, all test-side; the code itself needs no change.** **F-1:** `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap` passes only on LEGACY's fake text metrics. Under NATIVE the 360×640 column does not overflow, which matches the device, and the test goes red. **F-2:** the test named `…ComposesNoGlyph` cannot see a glyph. **F-3:** 15l's third window (`w412dp-h915dp` takes the *full* set) has no assertion that tells the full set from the compact one.
- **LEGACY ruling:** the sign-off is right about the fold test and half wrong about part 3. The part-3 test passes on NATIVE too, and soundly. The developer's change is the sign-off's default plus three specifics (§ LEGACY ruling). No QA re-walk is owed.
- One mutation result the tech-lead should know: S10's half of the S9 shape on its own (M1a) leaves all three 15k tests green. Only the 15l and 15m tests catch it. S9's full geometry (M1b) turns two of the three red.

## Findings

Severity: MAJOR = must fix before merge; MINOR = must fix before merge, small; INFO = no action owed by this audit.

- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:275-292` — **F-1, MAJOR.** `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap` asserts, unconditionally, that a scroll moves the Challenge tile (`:291`), i.e. that the column overflows. It is green only because the suite runs on LEGACY (no `@GraphicsMode`, no `robolectric.properties`).
  - With `@GraphicsMode(NATIVE)` added, and nothing else changed, it is the **only** test in the class that goes red: `expected to be less than: 466.0.dp but was: 466.0.dp`.
  - The probe shows why. At `w360dp-h640dp` with 24/48 insets, LEGACY measures 44 dp of overflow, with the Challenge tile at 504..620 against a 592 fold. NATIVE measures **0 dp**, with the tile at 466..562 and 30 dp of clear space below it. The geometry is identical with and without `AppTheme`.
  - NATIVE agrees with the device (`qa-engineer-loop3.md`, 15k: "Overflow 0dp"). LEGACY does not.
  - So the test certifies an overflow the shipped layout does not have. That is MB-1's class of defect, a JVM reading the device contradicts. 15k part 2 is conditional ("If the column overflows at all…"); the test is not.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:350-379` — **F-2, MINOR.** `theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph` cannot detect a glyph.
  - Mutation M3b put the decorative `Icon` (20 dp, `contentDescription = null`) back inline in `CompactPillContent` (`HomeProgressRibbon.kt:246`). **Every Home suite stayed green**: HomeLayoutTest, HomeScreenTest, HomeStageTest and HomeHeroRowTest. The ribbon still measured 52 dp, with label and value on one line.
  - The KDoc's claim (`:354-359`) that the one-line and ≤ 56 dp checks are "the stronger claim anyway" is false.
  - §4.2.3 ("What this owes in tests") owes "no glyph node", and 15m says "with no decorative glyph". The shipped code is correct: `CompactPillContent` composes no `Icon`. But nothing in the suite guards it.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt:135-141` — **F-3, MINOR.** `theStageArtIsPresentOnAPhoneAboveTheCompactThreshold` is 15l's third window, which must take the **full** set. It cannot tell full from compact.
  - Mutation M5 forced the compact set at ≥ 400 dp (`HomeBandSet.kt:82`, `|| !narrow`). **Every Home suite stayed green.**
  - The test asserts stage art only, and the compact set draws stage art too: this file's own KDoc at `:334-336` says the compact `w360dp-h800dp` window "now draws a small stage board".
  - Since D-03l, the only difference between the two sets at ≥ 400 dp is band spacing: `spacing_md` against `spacing_sm` (`HomeScreen.kt:381-382`). The discriminator was lost when S11 retargeted S10's hero-art test (§13's founder-feedback table). 15l's sentence "nothing at or above 400dp … changes behaviour" is therefore unguarded.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt:272-277` — INFO. The KDoc still states the retired 15h claim: both tiles fully within the first viewport at 360×640, "and on that window the compact band set below is what keeps them there". The same KDoc contradicts it at `:350-358`. Also `:352-353` says "~72dp over", which is ≈ 48 dp since D-03m and 0 dp on NATIVE and on the device. And `HomeBandSet.kt:45` says "under a full hero"; there has been one hero shape since D-03l. These are documentation only.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeStageTest.kt` (class KDoc, "LEGACY measures every character one dp wide, so every text band would come out at its unwrapped floor") — INFO, outside S10. The measurement says the opposite: LEGACY over-wraps. Chips measure 108 dp on LEGACY against 80 on NATIVE; tiles 108/116 against 96/96; the Boards label 36 against 16.
- `app/src/test/java/com/app/pegsolitaire/ui/home/fixture/HomeTestFixture.kt:150` — INFO, which bears on the remedy. `renderHome` composes `HomeScreen` with **no `AppTheme`**. So every Home geometry suite measures in Material 3's baseline faces and weights, not `AppTypography`, including the two that already run on NATIVE. Sizes and line heights are the stock M3 scale (`Type.kt`). With the fixture wrapped in `AppTheme`, all four Home suites stay green on NATIVE except F-1's test (run M6b), so no current verdict changes. "In the real faces" still requires the wrap.
- `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModelTest.kt:302-324` — INFO. The G-1 test adds `ExperimentalCoroutinesApi` opt-in warnings in the same pattern as its 40-odd siblings. Warnings-as-errors is not configured (244 `w:` lines in the Check). The tech-lead's pre-release ruling already covers this.

## The four mutations the sign-off names
Every mutation ran in a throwaway `git worktree add --detach` of `ee16a2a` outside the repo. Each was restored from git before the next and the worktree was removed afterwards. The suite is on LEGACY unless a row says NATIVE.

| # | Mutation | Red (message) | Green |
|---|---|---|---|
| M1a | S10's production half undone: `compactPills = false` (S9's stacked glyph pill) and threshold read on height only; HEAD's 48 dp hero | 15l (`207 vs 203`), 15m (`144 vs 140`) | **all three 15k tests**: overflow 108 ≤ 120; Challenge tile 568..684 still cut by the 592 fold; first tile complete |
| M1b | M1a plus S9's 72 dp hero-row floor, i.e. S9's full geometry (overflow 132; tile 592..708, wholly below the fold — S10's commit numbers exactly) | **15k part 2** (`…contains 'Challenge' … is not displayed!`), **15k part 3** (`at most: 120.0.dp but was: 132.0.dp`), 15l, 15m | 15k part 1: correctly so, the first tile *was* complete at S9 (476..584 < 592) |
| M1c | Hero floor 172 dp (part 1's liveness) | **15k part 1** (`expected: 512.0.dp but was: 484.0.dp`) | — |
| M2 | 15l: threshold read on height only (`HomeBandSet.kt:74-81`) | **`aNarrowTallPhoneIsCompact…`** (`207.0 vs 203.0`: stacked) | every other Home test |
| M3 | 15m: the stacked, glyph-bearing `FullPillContent` drawn on the narrow class (`HomeProgressRibbon.kt:185`) | **`theNarrowClassPill…`** (`144.0 vs 140.0`), and the 15l test | 15k ×3 (overflow 108) |
| M4 | CTA guard: `.safeDrawingPadding()` removed (`TutorialScreen.kt:139`) | **`theCompletionButtonClearsTheNavigationBar`** (`at most: 536.0.dp but was: 560.0.dp`), and the Skip guard (`56 vs 36`) | — |
| M4b | `.statusBarsPadding()` in its place (top edge kept) | **only the CTA guard** (`536 vs 560`) — it now guards the bottom edge on its own: **A-10 closed** | Skip guard |
| M4-N | M4 with `TutorialScreenTest` on NATIVE | both guards (`536 vs 560`; `56 vs 34`) — the tutorial guards do not depend on fake metrics | — |

**MB-1 is closed.** The tautology (`bottom ≤ contentBottom()`) is gone. Insets are supplied through the shared `applySystemBarInsets`, and containment is asserted as "a scroll does not move this node". M1b and M1c show all three 15k assertions can fail. The 15k tests' trouble now is F-1, not a tautology.

**What M1a means for the tech-lead.** "The 15k tests, on the S9 shape" holds only for S9's full geometry. S10's own production change can be reverted with the 15k tests still green, because the S11 hero is 24 dp shorter. On that shape the guards that bite are the 15l and 15m tests. On NATIVE, even S9's full geometry (M1b-N) overflows only 12 dp, all of it trailing gap and bottom padding below a wholly visible tile (492..588 against 592). On real metrics, 15k at its own window does not tell S9 from S10. 15l and 15m do, and both are live in both modes.

## H-2's delta (`04b6785..3b27012`)
- **Mechanism, as Decision 3 specifies.**
  - `res/values/themes.xml:18-20` sets `android:windowLightStatusBar` to true. The new `res/values-night/themes.xml` keeps the same style and parent and sets it to false, with no other item.
  - `windowLightStatusBar` needs API 23; minSdk is 24 (`android-app.gradle.kts:29`). The manifest's application and activity both use `Theme.PegSolitaire`.
  - Only previews pass `darkTheme`, and nothing sets night mode at runtime, so the `-night` qualifier is exact.
  - `MainActivity`, `Theme.kt`, the composables and the navigation bar are untouched.
- **WindowThemeTest goes red as claimed.** Each read defaults to the wrong answer, so an unset attribute cannot pass.
  - M7 (light item removed): the light test is red alone (`expected to be true`).
  - M8 (`values-night/themes.xml` deleted): the dark test is red alone (`expected to be false`).
  - M8b (the night item flipped to true): the dark test is red.
- **The G-1 test goes red as claimed.** The count is typed tap by tap and never read from the constant.
  - Threshold 2: red, "the coach after the second tap … expected: 0 but was: 1".
  - Threshold 4: red, "after the third tap … expected: 1 but was: 0".
  - Threshold 1: red, "after the second tap … expected: 0 but was: 2".
  - The test is deterministic: `runCurrent()` only, no virtual-time advance, so the idle nudge cannot fire.

## The merge `ee16a2a`
- `git show --cc ee16a2a -- . ':!.planning' ':!.claude'` prints **0 lines**: the merge resolved no hunk in a code file.
- Recomputed independently: `git merge-tree --write-tree 8ae64ab c3d3437` conflicts only in `.planning/questions/FOUNDER.md` and `.planning/reports/STATUS.md`, and auto-merges `app/build.gradle.kts`. `git diff <merge-tree> ee16a2a -- . ':!.planning'` is **empty**. Every code file in the merge is byte-identical to git's own automatic merge.
- **The literal gates and the tripwire are live on this tree.** I ran each on a clean copy, then on a planted violation:

| Probe | Planted violation | Result |
|---|---|---|
| G0 | none: the four literal gates on the clean tree | pass |
| G1 | an ad-unit-ID literal in `app/src/debug` | `adIdLiteralCheck` fails: "Ad-unit-ID literals are forbidden in shipped source … Scanned: src/main, src/release, src/debug (java, kotlin, res)" |
| G2 | a `CgkI` Play Games ID literal in `app/src/release/res` | `gamesIdLiteralCheck` fails |
| G3 | a `requestAudioFocus` call in `app/src/main` | `audioFocusLiteralCheck` fails, naming `AuditProbeFocus.kt:2` |
| T1 | a new `app/src/staging` directory | configuration fails: "Unclassified source set(s) in :app: src/staging" |

- `:build-logic:test` ran inside this Check: 54 tests, 0 failures. They include SourceSetPolicyTest (9) and ReleaseInvocationTest (10).
- The merge adds no dependency line.

## LEGACY ruling (the sign-off's question)
**Evidence.** I ran a probe at `w360dp-h640dp` with 24/48 insets and both chips, in four configurations: LEGACY and NATIVE, each with and without `AppTheme`. Then I ran the suites themselves on NATIVE.

| Reading | Ribbon | Chips | Tiles | Challenge tile | Overflow |
|---|---|---|---|---|---|
| LEGACY (what the Check runs) | 52 | 108, 108 | 108, 116 | 504..620, cut by the 592 fold | **44 dp** |
| NATIVE, in either theme | 40 | 80, 80 | 96, 96 | 466..562, clear of the fold | **0 dp** |

**Ruling, test by test.**
1. `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap` **passes only on fake metrics.** It is the only HomeLayoutTest case that goes red on NATIVE (M6a), and on NATIVE in `AppTheme` (M6b). This is F-1.
2. `oneShortScrollCompletesHomeOnTheNarrowBaseline` (part 3) **does not depend on fake metrics.** It passes on NATIVE, and that reading is true: 0 ≤ 120, and the tile is displayed. At this window, though, its scroll moves nothing. The sign-off's "part 3's scroll test pass[es] only on fake metrics" is wrong on this point.
3. `theChipsAndTheFirstTileAreCompleteInTheFirstViewportOnTheNarrowBaseline` (part 1) is sound in both modes.
4. Every other HomeLayoutTest, HomeScreenTest, HomeStageTest and HomeHeroRowTest case passes on NATIVE in `AppTheme`.
5. The tutorial suite also passes on NATIVE, and both of its inset guards still go red there (M4-N).

**What the developer changes.** Every item below is test-only.
1. Put `@GraphicsMode(GraphicsMode.Mode.NATIVE)` on HomeLayoutTest.
2. Wrap `renderHome` (`HomeTestFixture.kt:150`) in `AppTheme(darkTheme = false)`, so that "in the real faces" is true for all four Home suites. Measured: nothing else changes verdict.
3. Assert parts 2 and 3 conditionally, as 15k states them:
   - if the column overflows, the fold cuts through a band and one short scroll of ≤ 120 dp completes the screen;
   - if it does not, the overflow is 0 and every band is whole.
4. Keep the conditional halves **live**. At the 15k window the real column does not overflow, so on NATIVE those halves can never fail there. Add one NATIVE window that does overflow and assert the overflowing branch there, watched red:
   - `w360dp-h560dp` with the same insets works. Measured at HEAD: overflow 24 dp, Challenge tile 424..520 cut by the 512 fold, all three tests green.
   - With M1c applied on that window, all three go red: "not displayed"; `148 > 120`; `444 vs 416`.
5. Choose the trailing-gap edge on purpose, and route it to the ux-designer if it is not obvious. The column's overflow includes the trailing `spacedBy` gap and the bottom padding, 16 dp in the compact set. A layout whose last band ends within 16 dp of the fold therefore "overflows" with nothing hidden: M1b-N measured 12 dp with the tile wholly visible. Read literally, "if the column overflows at all, the fold cuts through a band" fails that layout.

This leaves the sitting standing and needs no QA re-walk, as the sign-off says. F-3's fix is test-only too. F-2 can be test-only:
- **Geometric check:** the compact pill's label/value group is centred (`Arrangement.spacedBy(…, CenterHorizontally)`), so its two inner margins are equal. A glyph before the label breaks that by 24 dp.
- **Pixel check:** read the pill off pixels with `testing/RenderedWindow.kt`.

A production `testTag` on the glyph would also work. But it touches `ui/home/`, a start-path file under Decision 1, so it must land before the H-3 sitting.

## Standards review of S10 (production)
- **Architecture and UDF.**
  - `HomeBands` alone reads the space and decides the set. `HomeProgressRibbon` takes a `Boolean`, so no band decides its own shape.
  - `homeBandSetFor` reads `maxWidth`, a value the D-03i `BoxWithConstraints` has already measured; it adds no second measuring pass.
  - The font scale is never read. `HomeBandSet` is an `@Immutable` data class.
- **Semantics and accessibility.**
  - Each pill is one `clearAndSetSemantics` node. Its description is computed before the shape branch, from the same `PillLabel` and `PillValue` leaves. There is no new string and no click action.
  - The compact label is the weighted child, with `fill = false`, so the value is never squeezed and the label wraps instead of clipping.
  - The pill-description tests are byte-identical across both window classes (`theCompactPillSpeaks…` and `theFullPillSpeaks…`, one `announcement()` oracle).
- **Constants.** The three new `dimens` tokens (400, 880 and 40 dp) are multiples of 4 and carry derivation comments. There are no raw dp literals, and `dpSpLiteralCheck` is green.
- **Performance.** The compact pill drops three vector `Icon` draws from the start destination and adds no read, decode or allocation in composition.
- **Previews.** The compact `@Preview` now comes in light and dark (A-13 closed).
- **S10's test move.** It removed exactly one test, the MB-1 tautology `bothChipsAndBothTilesAreInsideTheViewportOnAShortPhoneWithNoHeroArt`, and added seven. Each moved test's body changed only in its receiver (`renderHome` became `composeRule.renderHome`; compared body by body).
- **Suppressions.** `@Suppress` in `app/src/main` and `domain/src/main` is 13 on `develop` and 13 at HEAD. No suppression line was added or removed in S10 or H-2, no `@Ignore` or assume call exists, there is no baseline file, and no `!!` or `GlobalScope` was added. **No suppression to approve or reject.**
- **Security and licences.**
  - S10 and H-2 add no dependency, network call, permission, file or preference access, or new input surface.
  - S10, H-2 and the merge touch no version catalog or settings file, and add no dependency line, so `THIRD_PARTY.md` correctly needs no row.
  - The two test-only platform pokes (`ViewCompat.dispatchApplyWindowInsets` and `Resources.newTheme`) have no production path.

## Outputs
- `.planning/reports/15-founder-feedback/code-auditor-loop4.md` — this report. Not committed: the producer commits it. I edited no source file. The mutation runs used a throwaway detached worktree under my session scratchpad, now removed (`git worktree list` shows no entry). `PegSolitarie-p15` is clean at `ee16a2a`.

## Checks
- `cd PegSolitarie-p15 && ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon --no-build-cache --rerun-tasks check` — **pass**.
  - `BUILD SUCCESSFUL in 11m 45s`, exit 0, `98 actionable tasks: 98 executed`.
  - JUnit XML: app 1,064 tests (loop 3's 1,061 plus H-2's three), domain 274, `depthSweep` 1, build-logic 54. 0 failures, 0 errors, 0 skipped.
  - The 11 S10/H-2 tests named in this report each appear executed once.
  - The executed tasks include `:app:adIdLiteralCheck`, `:app:gamesIdLiteralCheck`, `:app:audioFocusLiteralCheck`, `:app:dpSpLiteralCheck`, `:app:detekt`, `:app:ktlintCheck`, `:app:lint`, `:domain:depthSweep` and `:build-logic:test`.
- `git show --cc --format= ee16a2a -- . ':!.planning' ':!.claude' | wc -l` — pass (0).
- `git merge-tree --write-tree 8ae64ab c3d3437`, then `git diff <tree> ee16a2a -- . ':!.planning'` — pass (empty; the only conflicts are FOUNDER.md and STATUS.md).
- `git diff develop...`, plus a `git grep` count of `@Suppress` on `develop` and on HEAD — pass (13 / 13; no `@Ignore`, baseline, `!!` or `GlobalScope` added).
- Throwaway worktree, `./gradlew :app:testDebugUnitTest --offline --rerun --tests …`:
  - M0 baseline: pass. HomeLayoutTest 13, TutorialScreenTest 5, WindowThemeTest 2, TutorialViewModelTest 14, and the probe, all green.
  - M1a, M1b, M1c, M2, M3, M3b, M4, M4b, M4-N, M5, M6a, M6b, M6e, M7, M8, M8b and M9 (2, 4, 1): results as tabled above. Every named guard went red. F-1, F-2 and F-3 are the runs where a guard did not behave as named.
- Throwaway worktree, `./gradlew --offline :app:{adId,gamesId,audioFocus,dpSp}LiteralCheck --rerun-tasks` — the clean tree passes; G1, G2, G3 and T1 fail as tabled.
- `./gradlew --stop` (both trees), then the leftover `KotlinCompileDaemon` stopped — `pgrep` finds no Gradle or Kotlin daemon.

## Open issues / risks
- **Staffing deviation.** This review shares the author's model (Q-019). I compensated by executing every check and mutation myself. It still does not satisfy rule 1, and a Fable re-audit is owed when the policy allows.
- F-1's remedy can make 15k parts 2–3 vacuous on real metrics unless the overflowing window in item 4 is added. On the device the narrow class already fits (QA loop 3: overflow 0). So the 15k guard that matters is "nothing new is hidden", and it needs a window that overflows.
- The trailing-gap edge (LEGACY ruling, item 5) is a spec reading for the ux-designer if the developer's conditional must choose.
- All three findings are in test code. If F-2 is fixed with a production `testTag`, it must land before the H-3 sitting.

## Handoff
Next: android-game-dev, through the producer. Fix F-1, F-2 and F-3 on `feature/phase-15-ux-refresh`, test-only. Apply the LEGACY ruling's items 1–5, and watch each new assertion fail before trusting it:
- F-1's overflowing-window assertions: red against M1c on `w360dp-h560dp`.
- F-2: red against the inline-glyph mutation M3b.
- F-3: red against M5's "compact at ≥ 400 dp".

Then a code-auditor re-audit of that delta only, with a new report path. The production code of S10 and H-2 needs no change, no QA re-walk is owed, and the H-3 sitting may run before or after this test-only fix, never during it. Producer: this FAIL comes back to the tech-lead only if the developer reports that any of it needs more than a test change.

VERDICT: FAIL
