# code-auditor report — release-3.0.0: checklist item 1.3, the suppression review
Date: 2026-11-01
Branch: release/3.0.0 (report only). Reviewed tree: `a7c52ab`. HEAD is now `8b852a1`, and `a7c52ab..8b852a1` touches only `.planning/release/3.0.0/*`, `.planning/reports/release-3.0.0/release-manager-gate2-prep.md` and `docs/release-checklist.md`. Release range: `v1.0..a7c52ab`
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The snapshot is 0.6.2 and the plugin is 1.0.0, so I diffed the plugin copies against the snapshot and read the changed sections: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing. Also code-reviewer and security-reviewer. compose-skill and compose-performance-audit were not loaded because no Compose code changes
Ran in parallel with: none known
Ran on: claude-opus-5-5 at effort high (policy). The harness names the model but no effort; the founder directive of 2026-10-31 (1) in STATUS.md sets code-auditor one step below its xhigh pin. **Deviation:** `agents/code-auditor.md` pins fable. Q-019 (answer 3, 2026-10-17) puts the Fable-pinned roles on Opus until 3.0.0 is published. Its 2026-10-23 addendum makes Fable available again for supervising roles. This dispatch came on Opus. Mitigation: every "load-bearing" claim below rests on my own detekt probe, not on recorded results
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- I listed every suppression in `app/`, `domain/` and `build-logic/`, main and test. That is 31 `@Suppress` / `@file:Suppress` sites (13 main, 18 test), 0 `@SuppressLint`, 0 `tools:ignore`, 0 `@Ignore`/`@Disabled`, 0 baseline files, 0 ktlint/detekt disable comments, plus the detekt, ktlint, Kotlin-warning and kover config exclusions.
- 8 sites already had a code-auditor approval line. I cite each one below. I reviewed the other 23 today and **reject none**.
- The release-manager's list was one short. `Clay.kt:3` `@file:Suppress("MatchingDeclarationName")` entered the range at `283ffd6` (after `v1.0`). The checklist grep `@Suppress` does not match `@file:Suppress`. I approve it below and name the fix to the grep (A5).
- **Probe.** In a throwaway worktree of `a7c52ab` I removed all 19 detekt-rule suppressions and ran `:app:detekt :domain:detekt`. Result: 57 issues, and each one sits on a removed site. 13 of the 19 sites are load-bearing. The other 6 suppress nothing under today's gate. Four are `LongParameterList`: in detekt 2.0.0-alpha.5 that rule `implements dev.detekt.api.RequiresAnalysisApi`, and the plain parse-only `detekt` task never runs it. The other two are `LargeClass` on test classes under 600 lines. None of the six hides a finding, so none is a release blocker. Their cleanup goes to develop (A1, A2).
- **VERDICT: PASS.** Item 1.3 can be ticked on this report, provided the producer confirms that Q-019 covers an Opus run of this review (see Open issues 1).

## Inventory and decisions

The detekt column comes from the probe: what `:app:detekt` reports at the site once its suppression is removed.

### Shipped source (`app/src/main`; `domain/src/main` and `build-logic/src/main` have none)

| # | Site | Rule(s) | In `v1.0`? | detekt with the suppression removed | Decision |
|---|---|---|---|---|---|
| M1 | `app/src/main/java/com/app/pegsolitaire/ui/home/ModeGlyphs.kt:38` | MagicNumber | no (`2667b3a`) | 32 MagicNumber, lines 49–67 | APPROVED here |
| M2 | `app/src/main/java/com/app/pegsolitaire/ui/home/ModeGlyphs.kt:71` | MagicNumber | no | 11 MagicNumber, lines 82–87 | APPROVED here |
| M3 | `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:177` | LongMethod | yes (comment edited since) | LongMethod 153 > 60 | prior approval |
| M4 | `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:110` | TooManyFunctions, LongParameterList, LargeClass | TMF+LPL yes; LargeClass no (`d5900c7`) | TooManyFunctions 40 > 11; LargeClass; LPL not run | APPROVED here |
| M5 | `app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt:101` | LongMethod | yes (comment edited since) | LongMethod 129 > 60 | prior approval |
| M6 | `app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt:3` | MatchingDeclarationName (`@file:`) | no (`283ffd6`) | MatchingDeclarationName at `:49` (`ClayColors`) | APPROVED here |
| M7 | `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:124` | TooManyFunctions | no (`06e5ca4`) | 14 > 11 | APPROVED here |
| M8 | `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt:64` | CyclomaticComplexMethod | no (`6a54c9b`) | complexity 21 > 14 | APPROVED here |
| M9 | `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:151` | TooManyFunctions, LongParameterList | no | TooManyFunctions 17 > 11; LPL not run | prior approval |
| M10 | `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt:89` | TooManyFunctions | no (`d33335c`) | 14 > 11 | APPROVED here |
| M11 | `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:51` | TooManyFunctions (interface) | no | 12 > 11 | prior approval |
| M12 | `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:385` | TooManyFunctions (`DefaultProgressRepository`) | no | 17 > 11 | prior approval |
| M13 | `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt:44` | TooManyFunctions | no | 17 > 11 | prior approval |

### Test source (`app/src/test`; `domain/src/test` and `build-logic/src/test` have none)

| # | Site | Rule | Probe / compiler | Decision |
|---|---|---|---|---|
| T1 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt:79` | LargeClass | fires (in `v1.0`) | APPROVED here |
| T2 | `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:91` | LargeClass | fires | APPROVED here |
| T3 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt:94` | LargeClass | **does not fire** (class body ≈ 462 non-comment lines < 600) | APPROVED here, inert (A2) |
| T4 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt:89` | LargeClass | **does not fire** (≈ 435 < 600) | APPROVED here, inert (A2) |
| T5 | `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt:853` | LongParameterList | rule not run (A1) | prior approval |
| T6 | `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:1902` | LongParameterList | rule not run (A1) | prior approval |
| T7–T15 | `GameViewModelTelemetryTest.kt:635`, `GameViewModelAdClockTest.kt:415`, `GameViewModelChallengeAdvanceTest.kt:200`, `GameViewModelAssistOrderingTest.kt:433`, `GameViewModelTest.kt:1442`, `GameViewModelHintStalenessTest.kt:710`, `GameViewModelResumeTest.kt:543`, `GameViewModelAllowanceTest.kt:644`, `GameViewModelSoundTest.kt:610` (all under `app/src/test/java/com/app/pegsolitaire/ui/game/`) | UNCHECKED_CAST | each sits on a `ViewModelProvider.Factory.create` that ends `) as T` (`grep -A22` checked all nine) | APPROVED here |
| T16–T18 | `app/src/test/java/com/app/pegsolitaire/archfixture/GmsBoundaryViolationFixture.kt:24`, `.../archfixture/ForegroundActivityBoundaryViolationFixture.kt:24`, `.../archfixture/ui/ActivityBoundaryViolationFixture.kt:24` | unused | IDE-only; the classes exist only as ArchUnit bytecode targets for `PlatformBoundaryGateTest` | APPROVED here |

### Existing approval lines I relied on

- M3: `.planning/reports/audio-review/code-auditor-merge.md:51` (at `GameScreen.kt:159`) and `.planning/reports/hint-undecided/code-auditor.md:52` (at `:147`). `.planning/reports/talkback-board/code-auditor-loop3-record.md:37` keeps it at `:170`. It is the same annotation; the line moved with code added above it, and the comment is unchanged since audio-review.
- M5: `.planning/reports/colour-vision/code-auditor-loop0.md:47` (at `BoardCanvas.kt:96`). `.planning/reports/talkback-board/code-auditor-merge-cv.md:98` carries it at `:101`.
- M9: `.planning/reports/phase-13/code-auditor-phase-sweep.md:36`.
- M11: `.planning/reports/phase-15/code-auditor.md:61`, restated at `.planning/reports/phase-15/code-auditor-s9.md:382`.
- M12: `.planning/reports/phase-13/code-auditor-phase-sweep.md:34` (at `:322`; same declaration, `DefaultProgressRepository`).
- M13: `.planning/reports/phase-13/code-auditor-phase-sweep.md:38`.
- T5, T6: `.planning/reports/phase-13/code-auditor-phase-sweep.md:40` (at `:852` and `:1896`).

### Approval lines (one per item reviewed here)

- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/ui/home/ModeGlyphs.kt:38` `@Suppress("MagicNumber")`. It is scoped to one private builder whose body is only vector path coordinates in a declared 24-unit viewport (`GLYPH_VIEWPORT`). Naming 32 coordinates would make the glyph unreadable and hide no rule. The string-path style of `UndoIcon.kt` needs no suppression, but switching is a refactor, not a condition.
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/ui/home/ModeGlyphs.kt:71` `@Suppress("MagicNumber")`. Same reason, for the 7-point bolt path.
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:110` `@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")`. This is the screen's single state owner: 40 functions, 686 non-comment lines, 11 Hilt-injected collaborators, all constructor-injected as the rules require. Splitting the state would create the drift between the assist counters, the board and the ad clock that the KDoc names. Its claim that a second state owner is "the only way" is overstated: stateless delegates are possible. I accept the suppression and put the decomposition in the backlog (A4). `LongParameterList` is not run by today's gate (A1).
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt:3` `@file:Suppress("MatchingDeclarationName")`. The file holds one class, `ClayColors`, beside the clay toolkit's top-level functions. The design contract names the file `Clay.kt`, and renaming it `ClayColors.kt` would misname the other members. The rule is file-scoped by nature, so `@file:` is no wider than the finding (exactly one issue in the probe).
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:124` `@Suppress("TooManyFunctions")`. 8 overrides are fixed by `GamesService`; the rest are private helpers that each state one rule once. A split would make a second holder of `_session`. The KDoc's "four private helpers" is stale: there are six (A3). The reason still holds.
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt:64` `@Suppress("CyclomaticComplexMethod")`. It is function-scoped on a total `when` over the 20-constant `AchievementId` with no `else`. The score of 21 counts the roster, not the logic. The split the plan proposed would need an `else` and would lose the compile-time exhaustiveness that keeps a wrong achievement ID from being submitted. Setting `ignoreSimpleWhenEntries` globally would be wider than this one site.
- approved by code-auditor: `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt:89` `@Suppress("TooManyFunctions")`. 14 functions: 4 public (`toDto`, `fromDto`, `encode`, `decode`) and one private validator or converter per stored field, on the untrusted resume-blob boundary. Merging validators would fold rejection rules together. It is the precedent the approved `CloudProgressCodec.kt:44` copies. Security: the suppressed rule is a size metric and hides no input-validation finding.
- approved by code-auditor: `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt:79` `@Suppress("LargeClass")`. One file per behaviour contract; test-only.
- approved by code-auditor: `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:91` `@Suppress("LargeClass")`. Same reason, test-only.
- approved by code-auditor: `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt:94` `@Suppress("LargeClass")`. Inert today. It is approved because it is test-only, disables no test and hides no finding; its removal goes to develop (A2).
- approved by code-auditor: `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt:89` `@Suppress("LargeClass")`. Same as above.
- approved by code-auditor: the nine `@Suppress("UNCHECKED_CAST")` at T7–T15. Each covers the `as T` of a `ViewModelProvider.Factory.create`, the standard unchecked generic return. Without them each `:app` unit-test compilation adds a warning, and design §9.8 makes a new unit-test warning a review finding.
- approved by code-auditor: the three `@Suppress("unused")` at T16–T18. The fixtures are deliberately unreferenced bytecode targets that prove the ArchUnit platform-boundary rules can fail. The suppression is IDE-only and no gate depends on it.

### Other things item 1.3 and the dispatch name

- **`@Ignore`, `@Disabled`, `@SuppressLint`, `tools:ignore`, `ktlint-disable`, detekt disable comments, `@SdkSuppress`, assume calls:** none anywhere in `app/`, `domain/` or `build-logic/` (grep below). `SemanticsProperties.Disabled` is the release-manager's false positive.
- **Baselines:** no detekt baseline, no lint baseline, no `lint.xml` (`find` below). `app/build.gradle.kts:399`'s "baseline" is prose about `proguard-android-optimize.txt`.
- **Gated test bodies:** these are not disabled tests.
  - `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt:118` (`RUN_SWEEP`) and `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/FuzzHarnessTest.kt:38` (`RUN_FUZZ`) run in `check` through `:domain:depthSweep` and `:domain:fuzz` (`domain/build.gradle.kts`, `check.dependsOn`). Their gate and their anti-vacuity were mutation-tested in `.planning/reports/phase-14/code-auditor-check-gates.md:55`.
  - The `UPDATE_GOLDEN` regenerators (`OpeningBookAuthoringTest.kt:25`, `SolutionTest`, `GoldenMasterAssert`) are authoring tools, not assertions. The assertions over those fixtures run on every `:domain:test`.
  - Accepted; no approval is needed.
- **Config exclusions:**
  - `config/detekt/detekt.yml` is unchanged since `v1.0` (`58b70af`, phase 1). Against the detekt 2.0.0-alpha.5 defaults (`default-detekt-config.yml` from the cached `detekt-core` jar), its only loosening is MagicNumber `ignoreEnums: true` (default false). That lets a number stand as an enum entry's argument, where the entry's name already names it. Its `excludes` drops the default `**/*.kts`, which is a tightening. `FunctionNaming.ignoreAnnotated: Composable` is the standard Compose exemption. APPROVED.
  - `.editorconfig:15` `ktlint_function_naming_ignore_when_annotated_with = Composable` is the same exemption for ktlint. APPROVED.
  - `build-logic/src/main/kotlin/android-app.gradle.kts:71` exempts `:app`'s unit-test compilations from `allWarningsAsErrors`. This is the design's held deviation (§9.8, ends before the first release branch after 3.0.0). The code-auditor reviewed it in `.planning/reports/phase-14/code-auditor-check-gates.md:57` (C7, probed) under `VERDICT: PASS` (`:98`). Not re-ruled.
  - `domain/build.gradle.kts:27` (`disabledForTestTasks` = `depthSweep`, `fuzz`) only keeps kover's agent off two non-unit tasks. No production line is excluded from the 90% bound. Accepted.
  - Android lint has no `lint {}` block, so it runs with defaults and is not warnings-as-errors. That is the held lint deviation in `docs/tech-design.md` (around line 136). It is not a suppression and needs nothing from 1.3.
- **`!!`:** 0 in shipped source (main, debug, release). 28 in test code, which the rule does not cover (rules/kotlin-android.md: "`!!` in production code").

## Findings (advisory; none blocks 3.0.0)

- **A1 (Medium; tech-lead):** `build-logic/src/main/kotlin/standards.gradle.kts:34-39`, together with the task choice at `:94`, means the gate's plain `detekt` task runs no `RequiresAnalysisApi` rule.
  - Evidence: `javap` on `detekt-rules-complexity-2.0.0-alpha.5.jar` gives `class LongParameterList extends Rule implements RequiresAnalysisApi`. The probe reported no LongParameterList anywhere, although `GameViewModel` has 11 constructor parameters (limit 6) and `ProgressSubmitterCloudTest.kt:854` has 7 function parameters (limit 5).
  - Effect: the four LongParameterList suppressions (M4, M9, T5, T6) are inert. Every rule of that kind is silently off for the whole tree.
  - The convention plugin chose this on purpose ("parse-only gate") but never named the rules it loses. The tech-lead should list them and decide whether any belongs in the gate (3.0.1 backlog). The four suppressions stay approved, and they become load-bearing if the rule is turned on.
- **A2 (Low; developer via producer, on develop):** `GameViewModelTelemetryTest.kt:94` and `GameViewModelAllowanceTest.kt:89`. Remove both `@Suppress("LargeClass")`; the probe shows they suppress nothing. Do not do this on the release branch.
- **A3 (Low):** `PlayGamesService.kt:117-118` says "four private helpers"; there are six (`hostActivityAfterAwait`, `signedInSessionFor`, `resolveSignedInState`, `awaitForegroundActivity`, `submitting`, `launchStockUi`). Fix the KDoc on develop.
- **A4 (Low, backlog):** `GameViewModel.kt:101-108` overstates the case (see its approval line). Record a 3.0.1 backlog item to move the assist economy and the ad-clock arithmetic into stateless delegates. Owner: tech-lead.
- **A5 (Medium; release-manager / tech-lead docs):** `docs/release-checklist.md:34` uses the evidence grep `grep -cE '^\+.*(@Suppress|@Ignore|Disabled|!!)'`. It cannot see `@file:Suppress`, `@SuppressLint` or `tools:ignore`, and it missed M6 in this range. It should be `'^\+.*(@(file:)?Suppress|@SuppressLint|tools:ignore|@Ignore|@Disabled|!!)'`, run over `*/src/*` and the build/config files (`*.gradle.kts`, `config/`, `.editorconfig`).
- **A6 (Info):** the same ViewModel factory with its `UNCHECKED_CAST` appears nine times (T7–T15). One shared test helper would hold the single suppression.

## Outputs
- `.planning/reports/release-3.0.0/code-auditor-suppressions.md` — this report: the inventory, prior-approval citations, one approval line per item reviewed today, and advisories A1–A6.
- Local only, not committed: `<scratchpad>/supprobe-detekt.log` (76 lines, the probe's detekt output); the extracted `default-detekt-config.yml`. The throwaway worktree `<scratchpad>/supprobe` was removed (`git worktree list` no longer shows it).

## Checks
- `grep -rnE '@(file:)?Suppress|@SuppressLint|@SuppressWarnings|tools:ignore|@Ignore\b|@Disabled|ktlint-disable|detekt:disable|@Test\(enabled|assumeTrue|Assume\.' app domain build-logic` — 31 suppression sites, nothing else.
- `find . \( -name '*baseline*' -o -name 'lint.xml' -o -name 'detekt*.yml' -o -name '.editorconfig' \)` (outside `build/`) — `./.editorconfig` and `./config/detekt/detekt.yml` only, plus one `.planning` report name. No baseline.
- `git diff v1.0..a7c52ab -- '*/src/*' | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|!!)'` — 44, the release-manager's count reproduced. The same diff grepped for `@file:Suppress|@SuppressLint|tools:ignore` gives 1 more (`+@file:Suppress("MatchingDeclarationName")`, M6).
- `git grep -n '@Suppress\|@file:Suppress' v1.0 -- app/src domain/src build-logic/src` — 7 lines in `v1.0`. Every other site entered in the release range.
- Probe, in a throwaway `git worktree add --detach … a7c52ab` with the 19 detekt-rule suppressions replaced by a comment in place: `./gradlew :app:detekt :domain:detekt --continue --rerun-tasks --offline -q` — exit 1, "Analysis failed with 57 issues". 43 MagicNumber (M1, M2); TooManyFunctions at M4, M7, M9, M10, M11, M12, M13; LargeClass at M4, T1, T2; LongMethod at M3, M5; CyclomaticComplexMethod at M8; MatchingDeclarationName at M6. Nothing at T3, T4 or any LongParameterList site.
- `javap -cp <extracted detekt-rules-complexity-2.0.0-alpha.5> dev.detekt.rules.complexity.LongParameterList` — "implements dev.detekt.api.RequiresAnalysisApi". LargeClass does not.
- `grep -rnF '!!' app/src/{main,debug,release} domain/src/main build-logic/src/main` (comments excluded) — 0.
- The full Check was not re-run (the dispatch: Gradle not needed). The release-manager's recorded run is 3.1/1.2 in `.planning/release/3.0.0/READINESS.md`. No emulator. `.planning/log/` was not read.

## Open issues / risks
1. **Model deviation.** Q-019 answer 3 allows Opus for Fable-pinned roles until 3.0.0 is published. Its 2026-10-23 addendum says supervising roles may run on Fable again. The producer confirms whether this Opus run satisfies rule 1 for item 1.3. If it does not, a Fable re-run of this inventory is the fix; the probe is reproducible from the Checks above.
2. A1 and A5 are process gaps, not code defects. They are owed to the tech-lead and release-manager on develop and in the checklist, not on `release/3.0.0`.

## Handoff
Next: producer —
- the release-manager ticks 1.3, citing this report's approval lines, the prior approvals above, and A5's corrected grep for the record;
- tech-lead: A1 and A4 into the 3.0.1 backlog;
- android-game-dev, on develop after the release: A2, A3 and optionally A6.

VERDICT: PASS
