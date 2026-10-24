# android-game-dev report — check-gates (R-2, F1/F2, auditor F-1/F-2, addenda A0–A3: C1–C7)
Date: 2026-10-23
Branch: feature/phase-14-check-gates (348279d..f15e58a) | report committed by the producer: the harness refused a report-file write from this subagent
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md`), superpowers:test-driven-development (with writing-good-tests.md), gitflow (mechanics only; rules/gitflow.md overrides it), compose-skill, compose-ui, android-viewmodel. **compose-skill and compose-ui were loaded AFTER C7's six one-line import edits in `ui/*Route.kt`, not before as rules/compose.md requires.** I reviewed those edits against both afterwards, and they touch nothing either skill governs.
Ran in parallel with: audio-review (`PegSolitarie-audio`) and the `PegSolitarie-hint` worktree's task; both built on this host, and the producer paused them for §7 only
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0 (one pause at §7 for a quiet host, resumed by the coordinator)
Lessons applied: none (0 active lessons in the snapshot and in the plugin's memory/LESSONS.md)

> Producer note: the agent returned this report as text because the harness refused a report file from a
> subagent; the producer wrote it here verbatim (HTML entities unescaped) and committed it. Evidence logs are in the
> producer session's scratchpad (`…/scratchpad/logs/`: `s7-measure.txt`, `kover-ab.txt`, `final-check-2.log`,
> `final-check-after.log`), not in the repo.

## Summary
- Nine commits in the ruled order: C1 `348279d`, C2 `139b418`, C3 `f46b888`, C3a `18e7885`, C3b `8687fca`, C4 `2520fa7`, C5 `e5f4e9b` (amended after §7), C6 `42d4812`, C7 `f15e58a`. Each has its red-first evidence, its acceptance and its negative controls below.
- §7, on a quiet host: 10,000 levels took 5.955 s on the warm run, so rule 1 applies and the local `check` default is 10,000. A local Check is now exhaustive and equal to CI.
- kover 0.9.11 (Apache-2.0) measures `:domain` at 99.0 % line coverage (896/905), so the bound is `minBound(90)`. C7 makes `:app` main and all of `:domain` compile warning-free, with warnings as errors. The release runtime classpath is identical before and after.
- The final `./scripts/check.sh --offline --rerun-tasks` is green, with the sweep's wedge planted immediately before it. The first attempt went red on three wall-clock solver deadline tests at host load 20→69. The same failure reproduced with kover's instrumentation off, but kover does eat into those tests' margin (Open issues).

## Outputs
- `build-logic/src/main/kotlin/standards.gradle.kts` — C1: every `Test` task deletes its own binary result store in `doFirst`. C3a: the gate comments.
- `scripts/check.sh` (new, executable), `CLAUDE.md` line 6, `AGENTS.md` (new, a byte-identical copy) — C2. Note for the reviewers: line 6 is the Check line every session reads, and it now says `./scripts/check.sh`, as the brief rules.
- `build-logic/src/main/kotlin/SourceSetPolicy.kt`, `build-logic/src/test/kotlin/SourceSetPolicyTest.kt` — C3: `KNOWN_SOURCE_SET_NAMES` is the five source sets that exist. C3a: `shippedSourceRoots()` returns whole source-set directories, and `SHIPPED_LANGUAGE_DIRS` is gone.
- `build-logic/src/main/kotlin/ReleaseInvocation.kt` — C3: F-1's KDoc.
- `.github/workflows/ci.yml` — C3b: triggers `[develop, 'release/**', main]` and the header comment. C5: `env: FUZZ_SEEDS: "10000"`.
- `domain/.../generator/LevelGenerator.kt` and `LevelGeneratorLadderTest.kt` — C4.
- `domain/.../TestFlags.kt`, `generator/FuzzSeeds.kt`, `FuzzSeedsTest.kt`, `FuzzHarnessTest.kt` — C5.
- `domain/build.gradle.kts` — C5: `:domain:fuzz` with default `"10000"`. C6: kover with `depthSweep` and `fuzz` excluded, and `minBound(90)`.
- `gradle/libs.versions.toml` — C6: `kover = "0.9.11"` and its plugin alias. C7: `hilt-lifecycle-viewmodel-compose` at the `hiltNavigationCompose` ref.
- `build-logic/src/main/kotlin/android-app.gradle.kts`, `kotlin-jvm.gradle.kts`, `app/build.gradle.kts` (the dependency only), the six `ui/*Route.kt` imports, and the four `@param:` sites (`ChallengeProgress.kt`, `ClassicLevelSource.kt`, `InterstitialCadenceStore.kt`, `TutorialViewModel.kt`) — C7.
- `git diff develop...HEAD --stat` — 29 files, +916/−102: exactly brief §9's list plus A3's files.

## Checks

### Baseline on the cut, before any edit (A3)
- `./gradlew check :app:compileReleaseKotlin --offline --rerun-tasks --no-build-cache --max-workers=4` on `0582e3f`, 2026-10-21 — pass (BUILD SUCCESSFUL in 11m 32s, 107 executed; load 11.15). 253 `w:` lines:
  - 10 in `compileDebugKotlin` and 10 in `compileReleaseKotlin`: the design's ten sites exactly;
  - 0 in `:domain` and `build-logic`;
  - 232 in `compileDebugUnitTestKotlin`: 202 `ExperimentalCoroutinesApi` opt-in (4 more than the design's 198, from phase-15 tests), 29 `createComposeRule`, 1 kotlin-reflect;
  - 1 environmental line.

### C1 — `348279d`
- Red, before C1: `./gradlew :domain:test --offline --max-workers=4` (store green), then `: > domain/build/test-results/test/binary/results-generic.bin`, then `./gradlew :domain:test --offline --rerun-tasks --max-workers=4` — fail as expected (`java.io.EOFException`, BUILD FAILED in 9s; no test ran: XML timestamps unchanged, store still 0 B).
- Green, same wedge, with C1 — pass (BUILD SUCCESSFUL in 29s, 274 tests).
- App wedge: `: > app/build/test-results/testDebugUnitTest/binary/results-generic.bin`, then `./gradlew :app:testDebugUnitTest --offline --rerun-tasks --max-workers=4` — pass (2m 6s; 1,067 tests in 99 suites, 0 failures, 0 skipped).
- Sweep wedge, planted immediately before each final Check (0 B `results-generic.bin` beside a 780–782 B `output-events.bin`) — pass both times. DepthSweepTest ran green over it, 0 failures, 677.8 s and 666.5 s, and rewrote the store (366 B).

### C2 — `139b418`
- `git check-ignore -q .gradle/x && echo ignored` — pass.
- Dead PID 99999999 in the lock, then `./scripts/check.sh --offline --dry-run` — pass (reclaimed, exit 0, `test ! -d .gradle/ergoz-build.lock` true).
- Live holder (the test shell's PID), then the same command — pass. Exit 75 in 0 s, no Gradle output, the holder's lock untouched. One stderr line: `check.sh: another build (pid 25269) holds …/.gradle/ergoz-build.lock in …/PegSolitarie-gates; wait for it, pass --wait, or if pid 25269 is dead: rm -rf …/.gradle/ergoz-build.lock`.
- `./scripts/check.sh --wait --offline --dry-run &`, `sleep 3`, `rm -rf` the lock — pass (still waiting at 3 s; exit 0 at 6 s; `--wait` never reached Gradle).
- Clean release: `test ! -d .gradle/ergoz-build.lock` after every run, the red final Check included — pass.
- `./scripts/check.sh --offline -x :domain:depthSweep :domain:test &`, `sleep 5`, `kill -TERM $!` — pass. At t+10 s the script, the lock and Gradle were all alive: bash defers a trapped signal while its foreground child runs. Gradle finished green, the script exited 143 at t+51 s, and the lock was gone.
- The same with `kill -9 $!` — pass. Exit 137; the lock stayed with dead PID 27839; the orphaned Gradle client finished by itself at t+7 s. The next `./scripts/check.sh --offline --dry-run` reclaimed the lock and exited 0.
- `grep -n '^Check command' CLAUDE.md AGENTS.md` (both `./scripts/check.sh`) and `cmp CLAUDE.md AGENTS.md` (identical) — pass.
- `shellcheck` — not run, not installed. `/bin/bash -n scripts/check.sh` under GNU bash 3.2.57 — pass; every run above ran under 3.2.57.

### C3 — `f46b888`
- Red: `aTestDimensionNobodyHasClassifiedTripsTheWire` (all six names, as A0 permits), then `./gradlew -p build-logic test --rerun-tasks` — fail as expected (55 tests, 1 failed: "missing (6) … but was: []").
- Green: trimmed set, KDoc, the pre-authorisation test deleted (A0) — pass (54 tests, 0 failures).
- `./gradlew :app:tasks :domain:tasks --offline -q` — pass (exit 0).
- Negative control: `mkdir -p app/src/androidTest/java && ./gradlew :app:tasks --offline` — fail as expected ("Unclassified source set(s) in :app: src/androidTest"). Removed; `git status --porcelain` showed only C3's files.

### C3a — `18e7885`
- Red on C3's tree: `everyFileOfAShippedSourceSetIsUnderAScannedRoot` — fail as expected (55 tests, 1 failed: "src/main/AndroidManifest.xml must lie under one of [src/main/java, …]").
- Green — pass (55 tests, 0 failures).
- Probes: one line carrying `ca-app-pub-0000000000000000~0000000000 CgkIc3aProbeOnly requestAudioFocus`, planted in a new `app/src/debug/AndroidManifest.xml` `<meta-data>`, in `app/src/main/assets/probe.txt` and in `domain/src/main/resources/probe.txt`. Then the six gates (`:app:` and `:domain:` × `adIdLiteralCheck`, `gamesIdLiteralCheck`, `audioFocusLiteralCheck`) with `--offline --continue` — fail as expected. All six FAILED, each naming its files, with "Scanned: every file under src/main, src/release, src/debug".
- Negative control: the same line in `app/src/test/resources/` and `domain/src/test/resources/` — pass. Probes deleted (`git status --porcelain --untracked-files=all` clean); the real tree — pass.

### C3b — `8687fca`
- The brief's `python3 -c 'import yaml; …'` (PyYAML 6.0.3 from a scratch venv) — pass (`['develop', 'release/**', 'main'] ['develop', 'release/**', 'main']`).
- `actionlint` — not run, not installed.
- The repository has no git remote, so no workflow run exists or is claimed as evidence.

### C4 — `2520fa7`
- Red against a throwaway `emptySequence()` stub: `./gradlew :domain:test --offline --tests '*LevelGeneratorLadderTest*'` — fail as expected (2 of 2, empty ladder).
- `./gradlew :domain:test --offline --rerun-tasks --max-workers=4` — pass (276 tests). Every golden test unchanged and green: ChallengeGeneratorGoldenTest 1, SolutionTest 10, QualityGateTest 8, FreeformValidityTest 5, OpeningBookTest 19, GetHintUseCaseTest 11, BoardSpecTest 14.
- `git diff develop...HEAD -- domain/src/main` — pass (the lifted loop and KDoc only).

### C5 — `e5f4e9b`
- Red: `FuzzSeedsTest` against a `= 0` stub — fail as expected (3/3). Green — pass (3/3).
- `./gradlew :domain:test --offline` — pass (the harness is a no-op at 0.0 s; 280 tests).
- `FUZZ_SEEDS=abc` and `FUZZ_SEEDS=0`, `./gradlew :domain:fuzz --offline` — fail as expected ("FUZZ_SEEDS must be a positive integer; was 'abc'", and the same with '0').
- `FUZZ_SEEDS=100000` — pass (prints the exhaustive note, then "examined 10000 levels (requested 100000), determinism pass, 0 failures").
- Subject mutation (levels divisible by 97 drop their last move), `FUZZ_SEEDS=1000` — fail as expected. `domain/build/fuzz-failures.txt` held exactly `level 97 … level 970: final pegCount 2, expected 1`. After the revert, `develop...HEAD -- domain/src/main` showed C4 only, and the next green run left no file.
- Vacuity mutation (the first pass walks nothing) — fail as expected ("levels examined … expected: 1000, but was: 0"). Restored byte-identical.
- `FUZZ_SEEDS=1000` twice, then 2000 — pass (FROM-CACHE, then UP-TO-DATE; 2000 executed). `./gradlew check --offline -m | grep ':domain:fuzz'` — pass (present).

### §7 — the tier measurement
Run per row: `FUZZ_SEEDS=$n ./gradlew :domain:fuzz --offline --rerun-tasks --max-workers=4`. Time is the test method's own `time=` from the JUnit XML. Before each run, my script required no other worktree's Gradle client and no daemon burning more than 0.5 CPU-s in 2 s. Its first version read `ps %cpu`, which stays high for a daemon that has just finished, so it skipped three runs on my own daemon. That attempt's one measured run (1,000 in 0.516 s) is excluded, and the set below was taken fresh, consecutively.

| FUZZ_SEEDS | Run | Time | Levels/s (both passes) | Load before → after | Other builds | Pixel_9a_bench |
|---|---|---|---|---|---|---|
| 1,000 | 1 | 0.495 s | 2,020 | 2.70 → 3.39 | none, daemons idle | up, idle 4.6 % |
| 1,000 | 2 | 0.540 s | 1,852 | 3.36 → 3.39 | none | up, idle 6.3 % |
| 10,000 | 1 | 5.989 s | 1,670 | 3.36 → 3.37 | none | up, idle 6.0 % |
| 10,000 | 2 | 5.955 s | 1,679 | 3.26 → 3.14 | none | up, idle 4.7 % |

The rule, verbatim from brief §7:
> 1. If **10,000 ≤ 60 s** on the second (warm) run → `fuzzSeedsDefault = "10000"`. A local `check` is then exhaustive, equal to CI, and rules/stacks.md's "10,000 by default" is met.
> 2. Else if **1,000 ≤ 60 s** → `fuzzSeedsDefault = "1000"`; CI's explicit `FUZZ_SEEDS=10000` is what makes the PR gate exhaustive.
> 3. Else **stop and report the numbers**; do not pick a tier. The tech-lead decides.

Applied: rule 1 (5.955 s ≤ 60 s), so the default is `"10000"`. C5 was amended `9c0df64` → `e5f4e9b`. Its first commit, made on a contended host, carried a placeholder that failed a local Check naming itself, so no tier existed before this measurement. I touched `…/scratchpad/gates-s7-done` at 22:01:47, right after the fourth run, and everything after ran on a shared host.

The two `check` wall clocks, both with `--rerun-tasks`:
- before, at the cut: `./gradlew check --offline --rerun-tasks --max-workers=4` — pass (13m 26s, 807 s; 98 executed). Load 42.72 → 22.21, with the audio daemon at 150 % CPU.
- after, at `f15e58a`: `./scripts/check.sh --offline --rerun-tasks --max-workers=4` — pass on the second attempt (11m 48s, 710 s; 104 executed). Load 14.92 → 3.25. The first attempt was red: 16m 1s, 965 s, load 20.19 → 60.10, 5-minute average 69.13.
- The difference (−97 s on the green pair) is host noise, larger than the branch's real additions: the fuzz took 4.9 s inside the green run, plus kover's instrumentation, report and verify steps, and six more tasks. It is not a cost figure; re-time both on a quiet host if one is needed.

### C6 — `42d4812`
- `latest.sh` reports 0.9.11 on both Maven Central (`org.jetbrains.kotlinx:kover-gradle-plugin`) and the Plugin Portal marker, 2026-10-23. Apache-2.0, JetBrains, build-only.
- One online warm run (0.9.11 was uncached), PLAT-07/D-18: `./gradlew :domain:test :domain:koverXmlReport --max-workers=4` — pass (32 s). Every run after it was `--offline`. kover applied beside AGP 9.1.0 and Kotlin 2.3.20 without issue, so no JaCoCo fallback.
- Root LINE counter in `domain/build/reports/kover/report.xml`: missed 9, covered 896, 99.0055 %, so the bound is `minBound(90)`. INSTRUCTION 97.8 %, BRANCH 92.3 %.
- `./gradlew :domain:koverVerify --offline -m` — pass (contains `:domain:test`, not `depthSweep` or `fuzz`).
- `check -m` — pass (contains `:domain:koverVerify`, which kover attaches itself; 0 `:app:kover` tasks).
- Bound 100 (mutation) — fail as expected ("Rule violated: lines covered percentage is 99.005500, but expected minimum is 100"); reverted.
- `koverVerify` executed in the green final Check — pass.

### C7 — `f15e58a`
- Control 1, before the flag: A3's `val c7Probe = 1` printed NO `w:`, because Kotlin 2.3.20's K2 command-line compiler does not report unused locals (an extended checker, off by default). I substituted a call to an `@Deprecated` function in the same file. `./gradlew :app:compileDebugKotlin --offline` — pass (compiled, with `w: …/C7Probe.kt:6:28 'fun c7ProbeTarget(): Int' is deprecated.`).
- After the flag, each probe planted alone — fail as expected, each with "e: warnings found and -Werror specified", naming the probe:
  - `app/src/main` → `compileDebugKotlin`;
  - `app/src/release` → `compileReleaseKotlin`;
  - `domain/src/main` → `:domain:compileKotlin`;
  - `domain/src/test` → `:domain:compileTestKotlin`.
- The exemption is exact: the probe in `app/src/test` → `compileDebugUnitTestKotlin` — pass (BUILD SUCCESSFUL, `w:` printed).
- The opt-in did not leak: `flatMapLatest` with no `@OptIn` in `app/src/main` — fail as expected ("needs opt-in … ExperimentalCoroutinesApi").
- Every probe deleted (`git status` clean).
- `./gradlew :app:compileReleaseKotlin --offline --rerun-tasks` with no `PLAY_UPLOAD_*` set — pass (executed; 0 source warnings; one environmental daemon line).
- `./gradlew :app:dependencies --configuration releaseRuntimeClasspath --offline`, before vs after, reduced to sorted unique `group:artifact:version` — pass (191 = 191, `diff` empty). The tree grew by one line (1,127 → 1,128), the direct declaration. `hilt-lifecycle-viewmodel-compose:1.3.0` was already present.
- Green final Check log — pass: 31 `w:` lines, 0 from any `/src/main/`, `/src/debug/`, `/src/release/` or `domain/` path. The residue is 29 `createComposeRule`, 1 kotlin-reflect (`GameScreenSoundTest.kt:300`) and 1 environmental line. No `ExperimentalCoroutinesApi` lines remain (202 before).
- Before → after: `:app` main 10 + 10 → 0 + 0; `:domain` 0 → 0; `:app` unit tests 232 → 30.

### Final (brief §9)
- `: > domain/build/test-results/depthSweep/binary/results-generic.bin`, then immediately `./scripts/check.sh --offline --rerun-tasks --max-workers=4` — pass (BUILD SUCCESSFUL in 11m 48s, 104 executed). Results: `:domain:test` 280/0; `:app:testDebugUnitTest` 1,067/0; sweep 1/0; fuzz 10,000/0; `koverVerify` passed; the lock released.
- The first attempt at the same command — fail: 3 deadline tests in `:domain:test` returned no move inside 1.5 s at load 20→69. They were `SolverTest.resolvesTheEnglish33FullStartInsideTheProductionDeadline`, `resolvesTheEuropean37FullStartInsideTheProductionDeadline` and `SolverBudgetTest.everyTractableSolvablePositionReturnsAMoveWithinBudget`. In that run the sweep, the app suite and the fuzz were all green. In the green run the same three took 0.319 s, 0.171 s and 0.392 s.
- `./gradlew -p build-logic test --rerun-tasks` — pass (55 tests, 0 failures, 0 errors, 0 skipped).
- `git status --porcelain` — pass (empty).
- `git diff develop...HEAD | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|baseline|ignoreFailures|!!)'` — pass (0). `… | grep -cE '^\+.*(-Xwarning-level|nowarn|suppressWarnings|@Suppress)'` — pass (0).

## Deviations from the brief's letter (for the code-auditor)
1. **`check.sh` traps.** The brief's `trap 'rm -rf "$lock"' EXIT INT TERM HUP` deletes the lock but does not exit, so a signal before Gradle starts would let Gradle run unlocked. The script uses an EXIT trap that releases, plus `exit 129`, `exit 130` and `exit 143` on HUP, INT and TERM, set the moment the lock directory is made. Gradle is still a plain call, never `exec`.
2. **`check.sh` details beyond the brief:**
   - it changes to the worktree root first;
   - an empty PID file gets a 1 s grace before the lock is judged stale;
   - a `--wait` run re-checks for a stale lock at each poll, while a direct run reclaims once.
3. **C2's signal runs** appended `-x :domain:depthSweep`, so each took under a minute instead of 11.
4. **The fuzz task** adds `testLogging.exceptionFormat = FULL`, so the invalid value or the failures-file path reaches the console.
5. **C3b** also rewords the header's first sentence, which named `./gradlew check` as CLAUDE.md's Check command, false after C2. The step comment ("`check` VERBATIM, because it is the Check command in CLAUDE.md") is untouched per "change nothing else", and is now loose.
6. **C7's probe** is a deprecated-function call, because A3's unused local prints nothing under K2.
7. **C4's clamp test** also asserts `levelNumber == 1`.
8. **C5 was amended** after §7. It was unpushed, unreviewed and HEAD.

## Open issues / risks
- **`kill -9` of `check.sh` leaves the Gradle client running (tech-lead).** The lock holds only the script's PID. After `kill -9` the lock can be reclaimed at once, while the orphaned client goes on building; C2's run showed it finishing on its own 2 s later. A second `check.sh` in that window could race it, which is M1. A fix: also record the Gradle client's PID, and reclaim only when both are dead. That needs Gradle run as a background child plus `wait`, which changes the brief's "plain call" shape, so it is the tech-lead's ruling. TERM and Ctrl-C are not affected.
- **Kover cuts the margin of `:domain:test`'s wall-clock deadline tests (tech-lead).** Design §8.4 instruments all of `:domain:test`. Interleaved A/B, three pairs at load 20–25, SolverTest with kover's agent ON and OFF (`--info` confirmed the agent only in ON):
  - European-37 full start: 2.04 / 1.95 / 1.49 s ON against 0.72 / 0.84 / 0.74 s OFF. It runs first in the class, so this includes one-time class instrumentation inside its deadline window.
  - Mid-game test: 10.1 / 12.8 / 10.6 s ON against 7.7 / 7.4 / 6.9 s OFF.
  - OFF pair 3 failed its deadline anyway (English-33, 1.862 s), so host load alone breaks these tests.
  - On a quiet host the margins hold: 0.17–0.39 s against 1.5 s in the green final Check.
  - Options for the tech-lead: accept; give the deadline tests their own Test task outside kover, as the sweep has; or ask the producer for quiet-host discipline for the Check.
- **A pre-existing build-script deprecation surfaced:** `app/build.gradle.kts:446:42 'fun srcDir(srcDir: Any): Any' is deprecated` (`sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)`, there since `f391ac6`, 2026-08-25, and on `develop`). It is a Kotlin DSL script diagnostic that `allWarningsAsErrors` does not reach; it printed only because C7's dependency edit recompiled the script. Out of A3's file scope; tech-lead to decide whether §9.8 covers build scripts.
- **Held deviation (§9.8 rule 2):** 29 `createComposeRule` deprecations, 1 reflection call and lint warnings remain, until before the first `release/*` after `release/3.0.0`.
- **Host:** the §7 timings are quiet-host figures. Both wall clocks and the final Checks ran on a shared host after the release signal.
- **Tooling:** no CI evidence (no remote). shellcheck and actionlint are not installed.

## Handoff
Next: **release-manager**
- `THIRD_PARTY.md`'s build-tooling row gains the word "kover": `org.jetbrains.kotlinx.kover` 0.9.11, JetBrains, Apache-2.0, build-only.
- `docs/release-checklist.md`: 3.1 becomes `FUZZ_SEEDS=100000 ./scripts/check.sh`; 3.2's path is `domain/build/fuzz-failures.txt`; 3.3 records 99.0 % line coverage at bound 90.

Next: **code-auditor**
- Review `develop...f15e58a` against the brief, A0–A3 and design §7.3, §8.4, §9.3, §9.6–§9.8, including the eight deviations above. C7's ten `:app` production lines are the tech-lead's scope change, not drift.

Then **tech-lead**
- Sign-off.
- The `kill -9` and kover-margin open issues, and the build-script deprecation.
- Transcribe for §7.3/§8.4: 10,000 levels in 5.955 s (about 1,679 levels/s); tier 10,000; kover 99.0 %, bound 90; the wall clocks with their loads.
- Whether C7 needs a device session is the qa-engineer's call, through the producer.
