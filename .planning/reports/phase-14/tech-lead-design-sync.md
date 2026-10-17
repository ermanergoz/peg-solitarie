# tech-lead report — phase-14 design sync: the last two sign-offs, the candy rulings, and the Kotlin-warnings ruling
Date: 2026-10-18
Branch: feature/phase-14-release-docs (866dffb..866dffb)
Skills loaded: ergoz-soft:standards (role tech-lead). This worktree's snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. Also the product's `.claude/rules/lessons.md`. Then android-architecture, android-gradle-logic.
Ran in parallel with: phase-15 H-1 code audit (`PegSolitarie-p15`), candy-backdrop spec rewrite for direction B (ux-designer)
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort pin). **Staffing deviation:** the tech-lead is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), `PegSolitarie-p15/.planning/questions/FOUNDER.md`, committed on `feature/phase-15-ux-refresh`.
Fan-out: none
Loops: 0
Read in full: `PegSolitarie-p15/.planning/reports/15-founder-feedback/tech-lead-signoff.md`; `develop:.planning/reports/phase-14/tech-lead-signoff-gate-scope.md`; `PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-architecture.md` (two pages); `docs/tech-design.md` (this worktree, before the edit). Read in part: Q-019, Q-020 and Q-021 in `PegSolitarie-p15/.planning/questions/FOUNDER.md`; `tech-lead-r2-gate-brief.md` §0, §9 and §10; the loop-3 Check log and lint report named under Checks.
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- This ran on Opus, not the pinned Fable, under Q-019. `docs/tech-design.md` now carries the design text of the phase-15 sign-off and of the R-1 gate-scope sign-off. It also carries the candy-backdrop rulings that survive the founder's Q-021 choice of direction B, "Deep gloss": the host, the first-frame gate and the licences routes. The B-12 wing rulings are void.
- **Ruling on Kotlin warnings as errors.** The main code becomes warning-free and gated as check-gates commit C7. That covers `:app`'s main compilations and every `:domain` compilation: 10 warnings, all mechanical. `:app`'s unit-test compilations (228 warnings) and lint (57 warnings, 0 errors) are a held deviation. It ends before the first `release/*` branch after `release/3.0.0` is cut (design §9.8).
- Addendum A3 below is the developer's text for C7.
- No product code was written. No Gradle build was run.

## What changed in `docs/tech-design.md` (commit 866dffb)

| Section | Change | Source |
|---|---|---|
| Header | The 2026-10-18 sync and its three sources | this report |
| §1.2 | A pointer from the "known divergence" paragraph to the warnings ruling | §9.8 |
| §3.1 | The Licenses list's no-ViewModel exception | candy R4 |
| §3.2 | `Licenses` and `LicenseText(entryId)` joined the destination list | candy R4 |
| §3.5 (new) | One backdrop in `AppNavHost` behind the `NavHost`. The variant comes from the committed back-stack destination, with an `else` branch for later routes. The adapter is composed after the `NavHost` and drawn behind it, so a restore shows the right variant on frame 1. One constant, `NAV_TRANSITION_MS`, drives both fades over two slots. All animation is read in the draw phase, and the geometry is a pure model. The F-32 theme `Surface` is added. Insets stay as today, a recorded divergence from F-33. The one-frame scene-ready gate. 15e on that branch, with arm A at its merge-base. Status: Q-021 picked B, the spec is being rewritten, R2's wings are void, and B's own surfaces get a fit ruling when the spec lands | candy R1, R3; Q-020, Q-021 |
| §3.6 (new) | The licences screens. The routes carry ids, never asset paths. The catalogue is code, and `BundledLicenseText` is the one place a path is written. The text screen has a ViewModel, a `Column` body and an `@IoDispatcher` source. A pure re-flow. The Apache-2.0 text, byte-identical | candy R4 |
| §8.1 | build-logic test counts: 54 at C3 (A0), 55 after C3a | gate-scope addendum A0 |
| §8.5 | Pixel comparisons inside one run are allowed; stored goldens stay out | candy R5 |
| §9.1 | C7's compile gate | §9.8 |
| §9.3 | The gates read every file of a shipped source set. Amendment note with its reason; R-1 PASS and merged as `c3d3437` | gate-scope ruling 2, text block |
| §9.7 | CI on `develop`, `release/**` and `main`. The repository has no remote, so the 25-minute trigger waits for a remote run | gate-scope ruling 1, text block |
| §9.8 (new) | The Kotlin and lint warnings ruling, with the count | this report |
| §10 | 15e: `Pixel_9a_bench` with `/data` free ≥ 4 GB, arm A at the merge-base, the start-path list, runs on phase 15 before its merge. Home's first frame gains the backdrop gate and F-1's cause. Idle Home: the draw-phase move, with the audit after candy | phase-15 Decision 1, Decision 3; O-B |
| §11 | AC-F7 now reads as Decision 2: no airplane run; the tracking-free discriminator role; AC-F5's release half | phase-15 Decision 2 |
| §13 | The Apache-2.0 licence-text row. The licences gap is scheduled. No dependency is added by candy. C7's direct declaration of `hilt-lifecycle-viewmodel-compose`, already on the runtime classpath | candy R4; §9.8 |
| §14.1 | R-1 done. R-2 lists C1–C7 and depends on R-1 **and** phase 15. Phase 15 depends on H-1 to H-4. New F-1 and candy-backdrop rows. Audio row: parallel work and the owed haptics ruling. R-5's font and Apache rows. **The merge order into `develop`** | phase-15 Decision 4; gate-scope; candy R6 |
| §14.2 | The held half of the warnings ruling, with its due point. F-33 host insets, post-v1.1. The licences surface is no longer backlog | §9.8; candy R1 |
| §15 | The `libs.versions.toml` mitigation reversed. Q-019 added to the Fable row. New risks: a bench VOID or FAIL, F-1 on `develop`, a post-C7 warning, B's gel cards | phase-15; candy |
| §16 | #3 closed. #4 scheduled. #11 gains the 2026-10-18 spec folds. New #13 F-1, #14 warnings (decided), #15 the timer's TalkBack form, #16 B's fit ruling, #17 the haptics ruling the tech-lead owes | phase-15 routed table; candy R6 |
| §17 | Eleven rows: the instrument; arm A and the run point; AC-F7; the merge order; the source roots; the CI triggers; the C3 count; the brief §10 exclusion lifted for C7; §3.1's exception; §16 #4; candy R2 and the sweets variants | all three |

**One amendment of my own ruling, recorded.** The 15e start-path list now also names `res/values-night/themes.xml`. H-2 created that file, and it is the night launch-window theme, so a later merge-in that changes it re-opens a sitting just as `res/values/themes.xml` does.

## Ruling — Kotlin warnings as errors (design §9.8)

**Evidence.** The newest Check log on disk is `PegSolitarie-p15/.planning/qa/15-founder-feedback/loop3/check/gradlew-check.log`. It is `--no-daemon --no-build-cache --rerun-tasks check` on `e1875cb`: green, 98 of 98 tasks executed, so every compilation ran.
- **239 `w:` lines** in all.
- **10 in `:app` main.** Six are the deprecated `androidx.hilt.navigation.compose.hiltViewModel`, in the six `…Route.kt` files (`HomeRoute.kt:59` among them). Four are qualifier annotations on constructor `private val`s, "currently applied to the value parameter only", in `ChallengeProgress.kt:54`, `ClassicLevelSource.kt:40`, `InterstitialCadenceStore.kt:98` and `TutorialViewModel.kt:76`.
- **0 in `:domain`** (main and test) and 0 in `build-logic`.
- **228 in `:app`'s unit tests:**
  - 198 are `ExperimentalCoroutinesApi` opt-in warnings;
  - 29 are the deprecated `createComposeRule`, to be replaced by `junit4.v2`, which queues work on `StandardTestDispatcher`;
  - 1 is the `kotlin-reflect` call at `GameScreenSoundTest.kt:300`.
- **1 is environmental**, "multiple Kotlin daemon sessions".

Lint in the same run gives **0 errors and 57 warnings**:
- 35 version checks;
- 13 `UnusedResources`;
- 5 `PluralsCandidate`;
- 2 `UseOfNonLambdaOffsetOverload` in `HomeBlobs`;
- 1 `TypographyDashes`;
- 1 `FieldSiteTargetOnQualifierAnnotation`.

Neither rule is configured anywhere at `ee16a2a`. The deprecated `hiltViewModel` is an inline forwarder to `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`, per `hilt-navigation-compose` 1.3.0's sources.

**Decision.** It is a split: a developer task for what ships, and a held deviation for the rest.
- **The task, C7.** The shipped code's warnings are ten mechanical edits whose bytecode meaning does not change. Gating them now means audio, F-1 and candy, the newest code before 3.0.0, cannot add any.
- **The deviation.** What is left in the tests is a migration that changes how 29 reviewed suites synchronise, in a release week. What is left in lint is mostly version checks the pins are held against for recorded reasons. Turning either into errors now would take a suppression or a baseline, and both hide warnings rather than fix them.
- **The deviation's guard is no growth.** A diff that adds a unit-test compilation warning or a lint warning in a file it touches is a review finding, fixed in that diff.
- **Its end:** before the first `release/*` branch after `release/3.0.0` is cut; a `hotfix/*` is exempt.
- **Why C7 rides check-gates and not its own branch:**
  - it is a gate;
  - check-gates is cut only after phase 15 merges, and two of the ten sites (`TutorialRoute.kt`, `TutorialViewModel.kt`) exist only on phase 15;
  - the branch already has the right reviewers;
  - a separate branch would need a full review chain of its own for ten lines.

## Addendum A3 to `tech-lead-r2-gate-brief.md` for the android-game-dev (check-gates): commit C7

Pass this section to the developer together with the brief and `tech-lead-signoff-gate-scope.md` addendum A0–A2. The commit order becomes C1, C2, C3, C3a, C3b, C4, C5, C6, **C7**. The governing text is design §9.8.

**Commit:** `feat(14): fail the build on Kotlin warnings in shipped code`.

**Before any edit.** Run the Check once on the cut and record the `w:` lines by source set and kind. The design's table is the 2026-10-17 baseline: 10 in main, 228 in unit tests, 0 in `:domain` and `build-logic`. You may find a main warning outside the ten sites, for example from a merge-in. If it is one of the two kinds below, fix it the same way. If it is any other kind, stop and report it to the tech-lead through the producer.

**Files.** Brief §10's exclusions ("any UI, ViewModel or `:app` production code", "any phase-15 file") are lifted for these files only:
- `build-logic/src/main/kotlin/android-app.gradle.kts`;
- `build-logic/src/main/kotlin/kotlin-jvm.gradle.kts`;
- `gradle/libs.versions.toml`;
- `app/build.gradle.kts`;
- `ui/classic/ClassicCatalogRoute.kt`, `ui/game/GameRoute.kt`, `ui/home/HomeRoute.kt`, `ui/onboarding/TutorialRoute.kt`, `ui/results/ResultsRoute.kt`, `ui/settings/SettingsRoute.kt`;
- `data/ChallengeProgress.kt`, `data/ClassicLevelSource.kt`, `data/InterstitialCadenceStore.kt`, `ui/onboarding/TutorialViewModel.kt`.

Brief §9's file list gains the same files.

**Change.**
1. **The flag.** Set `allWarningsAsErrors` on every Kotlin compilation, once, in the two convention plugins. Then exempt `:app`'s unit-test compilations by name, in the same block. Add a comment that cites design §9.8 and its end condition. `:domain`'s test compilation is **not** exempt.
2. **The opt-in.** In `android-app.gradle.kts`, opt in to `kotlinx.coroutines.ExperimentalCoroutinesApi` on the unit-test compilations only, never on a main compilation.
3. **The six imports.** `androidx.hilt.navigation.compose.hiltViewModel` becomes `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`. Make no other edit to those files.
4. **The four use-site targets.** `@ApplicationScope` and `@ApplicationContext` on the four constructor parameters become `@param:ApplicationScope` and `@param:ApplicationContext`. Make no other edit.
5. **The dependency.** Add a catalog entry `hilt-lifecycle-viewmodel-compose` (`androidx.hilt`) at `version.ref = "hiltNavigationCompose"`, with a comment: same release train; 1.4.x needs compileSdk 37. Add `implementation(libs.hilt.lifecycle.viewmodel.compose)` in `app/build.gradle.kts`. Keep `hilt-navigation-compose`.

**Not in C7:**
- no `@Suppress`;
- no `-Xwarning-level`, `-nowarn`, `suppressWarnings`, or any `freeCompilerArgs` that silences a diagnostic;
- no `lint { }` setting;
- no test-file edit, and no migration to `junit4.v2`;
- no resource or string edit.

**Red first: negative controls, planted and then deleted.**
1. **Before step 1:** a planted unused local (`val c7Probe = 1` inside a function) in a new `app/src/main/…/C7Probe.kt` compiles with a `w:`.
2. **After step 1, each probe fails its compilation, naming the file:**
   - the main probe fails `:app:compileDebugKotlin`;
   - the same probe in `app/src/release/…` fails `:app:compileReleaseKotlin`;
   - the same probe in `domain/src/main/…` fails `:domain:compileKotlin`;
   - the same probe in `domain/src/test/…` fails `:domain:compileTestKotlin`.
3. **The exemption is exact:** the same probe in `app/src/test/…` still compiles, with a `w:`.
4. **The opt-in did not leak to main:** a planted `flatMapLatest` call (an `@ExperimentalCoroutinesApi` API in coroutines-core) with no `@OptIn`, in `app/src/main/…`, fails `:app:compileDebugKotlin`.
5. **Clean up:** delete every probe; `git status --porcelain` must be empty.

**Acceptance.**
1. **The Check is green with the flag on**, with the compile tasks executed rather than up to date. The log has 0 `w:` lines from any `/src/main/`, `/src/debug/` or `/src/release/` path or from `domain/`.
2. **The release compilation is clean.** `./gradlew :app:compileReleaseKotlin` executes with 0 warnings. No `PLAY_UPLOAD_*` is needed (design §9.2.6).
3. **The shipped classpath is unchanged.** Run `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` before and after. The sorted, de-duplicated list of resolved `group:artifact:version` must be identical; only the tree's shape may change.
4. **The residue is listed.** List every remaining unit-test compilation `w:` by kind and count. None may be an `ExperimentalCoroutinesApi` opt-in warning. The expected residue is the `createComposeRule` deprecations and the one reflection call, plus anything in tests added since the 2026-10-17 log, each named.
5. **No suppression.** Brief §9's suppression grep returns 0. So does `git diff develop...HEAD | grep -nE '^\+.*(-Xwarning-level|nowarn|suppressWarnings|@Suppress)'`, excluding your report's prose.
6. **Your report** carries:
   - the before and after counts;
   - every probe and its result;
   - the dependency comparison.

**Merge-in.** If `feature/audio-review` merges into `develop` before check-gates does, take `develop` in, then run C7's acceptance on the merged tree. The same two patterns, the import and the `@param:` target, apply to audio's files. Any other kind of warning: stop and report.

**Behaviour.** C7 changes no runtime behaviour. The six imports call the function the deprecated one already forwarded to, and `@param:` names the target the annotations already applied to. Whether that needs a device session is the qa-engineer's call through the producer.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r5/docs/tech-design.md` — the synced design; commit `866dffb` on `feature/phase-14-release-docs`.
- `/Users/ermanergoz/Desktop/PegSolitarie-r5/.planning/reports/phase-14/tech-lead-design-sync.md` — this report: the change list, the warnings ruling, and addendum A3 for the check-gates developer. It is committed in its own commit after `866dffb`.

## Checks
- `grep -c '^w: ' …/loop3/check/gradlew-check.log` — pass: 239.
- Classified by path prefix and message — pass: 10 `app/src/main`, 226 `app/src/test`, 2 `app/src/testDebug`, 1 daemon-environment line, 0 under `domain/` or `build-logic/`. The log ends "BUILD SUCCESSFUL in 11m 37s, 98 actionable tasks: 98 executed".
- `PegSolitarie-p15-qa/app/build/reports/lint-results-debug.xml`, parsed by severity and id, and the `.txt` tail — pass: "0 errors, 57 warnings". The worktree is at `e1875cb`, and the report was written at 21:56, inside the log's 21:55–22:06 run.
- `git show HEAD:<file>` for both convention plugins, `standards.gradle.kts`, `app/build.gradle.kts` and `domain/build.gradle.kts` in `PegSolitarie-p15`, at `ee16a2a` — pass: no `allWarningsAsErrors`, no `warningsAsErrors`, no `lint {` block, no lint baseline file.
- `unzip` of `hilt-navigation-compose-1.3.0-sources.jar` and `hilt-lifecycle-viewmodel-compose-1.3.0-sources.jar` from the Gradle cache — pass: the deprecated `hiltViewModel` imports `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as composeHiltViewModel`, and its body is `composeHiltViewModel(viewModelStoreOwner, key)`.
- `git cat-file -e develop:<path>` for `TutorialRoute.kt`, `TutorialViewModel.kt` and `HomeRoute.kt` — pass: the first two are absent from `develop` (phase-15 files), and `HomeRoute.kt` is present.
- `git merge-base --is-ancestor <branch> develop` for the in-flight branches — only `feature/phase-14-gate-scope` is merged (`c3d3437`). No `check-gates` or `home-chip-arrival` branch exists yet.
- `git log -1 -- <path>` for the three source reports, and `git show HEAD:.planning/questions/FOUNDER.md` on `feature/phase-15-ux-refresh` — pass: each is committed (`04b6785`, `77216a3`, `d119919`), and Q-019's and Q-021's answers are committed.
- A Python table check over `docs/tech-design.md` — pass: 18 tables, 0 rows whose cell count differs from the header.
- `grep -n` for stale phrases (`before phase 15`, `java,kotlin,res`, `` `develop` and `main` ``, `airplane`, `follow-up slice`, `v1.1.1`) — pass: each remaining hit is a superseded wording kept on purpose in §17 or in a *Superseded* note.
- `git -C …/PegSolitarie-r5 show --stat 866dffb` — pass: 1 file, `docs/tech-design.md`, +327/−29. `git status --short` was clean afterwards.
- **Not run:** the Check and every other Gradle task. A code-auditor is running the Check in `PegSolitarie-p15`, the host takes one build at a time, and the dispatch forbids starting one. This diff is documentation only (`docs/`, `.planning/`), outside every compilation and gate input.

## Open issues / risks
- **Staffing deviation (Q-019).** These rulings were written on Opus, the developer's model. Mitigation: C7 is reviewed by the code-auditor on the diff, with its own probes.
- **§3.5 carries R1 and R3 into an unseen spec.** The B rewrite is in progress. "What the gate covers in B" is a default I set. The B fit ruling (§16 #16), due before the candy developer dispatch, may amend it and §3.5's variant wording.
- **The warning counts come from `e1875cb`.** The phase-15 head `ee16a2a` adds H-2's tests (`TutorialViewModelTest`, `WindowThemeTest`), which may add unit-test warnings. C7's first step recounts on the cut.
- **C7 widens check-gates** into ten `:app` production lines. Brief §10 forbade that until now, and the code-auditor should read C7 as a scope change I made here, not as developer drift.
- **Owed by the tech-lead and still open:** the haptics ruling for the wrong-tap feedback (§16 #17), and the B fit ruling (§16 #16).
- **The commit trailer.** `866dffb` carries rules/gitflow.md's single trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` (plugin 1.0.0), as this branch's earlier commits do. No prepare-commit-msg hook is installed here. The `Ran on:` line above records the true model.

## Handoff
Next: producer —
1. When check-gates is cut (after R-1 and phase 15 merge), pass the android-game-dev the brief, `tech-lead-signoff-gate-scope.md` addendum A0–A2, and **this report's addendum A3**. The code-auditor reviews C7 with the rest; I sign off against the brief plus both addenda.
2. The ux-designer: the spec folds in §16 #11, non-blocking. When the B rewrite of `docs/ux/candy-backdrop.md` lands, dispatch me for the B fit ruling (§16 #16) before the candy developer dispatch.
3. Dispatch me for the haptics ruling (§16 #17) before the wrong-tap half of `feature/audio-review` is built.
4. Schedule the held half of §9.8 in the first post-3.0.0 batch: it must close before any `release/*` after `release/3.0.0` is cut.
5. `feature/phase-14-release-docs` still needs the release-manager's `THIRD_PARTY.md` font rows and the OFL-1.1 verdict line before it merges (§14.1 R-5; merge order step 3).
