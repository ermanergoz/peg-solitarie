# tech-lead brief — the check-gates branch (R-2 + the concurrency fix + auditor F-2)

Date: 2026-10-17
For: android-game-dev (implements), code-auditor (reviews), producer (dispatches after R-1 merges)
Governing design: `docs/tech-design.md` §7.3 (fuzz), §8.4 (kover), §9.3 (tripwire), §9.6 (F1/F2), §14.1 (order)
Ruled by: tech-lead. I wrote no code; everything below is a specification. The code-auditor reviews
the diff on a model superior to the author's; the tech-lead signs off against this brief and the design.

## 0. The ruling: one branch, serial commits, in this order

**One branch**, off `develop` **after `feature/phase-14-gate-scope` (R-1) has merged**, never in
parallel with it. Proposed name `feature/phase-14-check-gates` (the release-manager may rename;
`bugfix/check-gate-concurrency` from `tech-lead-depthsweep.md` is superseded by this). Worktree:
`git worktree add ../PegSolitarie-gates feature/phase-14-check-gates`.

Why one branch, not two: the same developer writes all of it, the same auditor reviews it, every
piece registers something in `check` (`standards.gradle.kts`'s `check` block or `domain/build.gradle.kts`'s
`check` dependency — the one place CI reads), and the depth-sweep fix must land before any long
Test task is run repeatedly on this shared host — which the fuzz measurement is. Two branches on the
same `check` wiring is a rebase for nothing. The textual collision my predecessors assumed is weaker
than stated (the fuzz task and kover live in `domain/build.gradle.kts`; only F1 edits
`standards.gradle.kts`); the ordering ruling stands on the shared registration and the shared review.

Commit order, and why this order:

| # | Commit (conventional, `(14)` scope, ≤ 72 chars) | Why here |
|---|---|---|
| C1 | `fix(14): self-heal a truncated test-result store before every Test task` | Every later step runs multi-minute Test tasks; the wedge must be unreachable first |
| C2 | `feat(14): take a per-worktree lock around the Check command` | Your own measurements in C5 need an uncontended host; the lock is how you prove it |
| C3 | `fix(14): the source-set tripwire admits only source sets that exist` | Auditor F-2 from R-1, same file family, smallest; F-1's KDoc rides with it |
| C4 | `refactor(14): expose the generator ladder as a sequence` | The harness needs O(N), not O(N²) (§1 below) |
| C5 | `test(14): fuzz the whole Challenge ladder, FUZZ_SEEDS-tiered` | The Core-Value gate; its tier is chosen by the measurement you record here |
| C6 | `feat(14): kover coverage gate on :domain` | Last, because its exclusion list must name the fuzz task |

This branch merges to `develop` **before phase 15**, which then merges `develop` in
(`tech-lead-depthsweep.md` ruling 2). Phase 15 edits `gradle/libs.versions.toml` too (it removes
`ui-text-google-fonts`); C6 adds kover in a different region. Do not touch any phase-15 file.

## 1. Facts you must not discover the hard way

- **`LevelGenerator.generate(n)` replays the chain from level 1 to `n`** to rebuild the exact dedup
  window (`LevelGenerator.kt:63-80`): it is O(n), and `generate(k)`'s pass is a prefix of
  `generate(k+1)`'s. A loop `for n in 1..N: generate(n)` is O(N²). At N = 10,000 that is fifty million
  level generations. That is why C4 exists.
- **The level space is finite: `ChallengeLevelBounds.MAX = GeneratorConstants.MAX_CHALLENGE_LEVEL = 10_000`.**
  Levels above it clamp. So `FUZZ_SEEDS=10000` is the whole space and `FUZZ_SEEDS=100000` cannot be
  more than that. The release tier for this product is **exhaustive**; §7.3 of the design records it.
- **The generator's output contract is frozen** (`GeneratorConstants` KDoc, Pitfall 2). C4 lifts the
  loop into a sequence and changes no body line; the golden fixtures (`ChallengeGeneratorGoldenTest`,
  `SolutionFixtures`) are what prove it. If any golden test goes red, you changed behaviour — stop.
- **Only the Android 14 `android-all` image is cached offline.** Do not add a Robolectric `@Config(sdk=…)`
  anywhere (not relevant to `:domain`, but you will be in `:app`'s test tree for C1's acceptance).
- **Worktree day one:** `local.properties` is git-ignored and exists only in the primary worktree —
  copy it or export `ANDROID_HOME=$HOME/Library/Android/sdk`, else `check` dies at `:app:lintReportDebug`
  before a single test. The studio's branch guard resolves the branch from the session's primary cwd:
  run a bare `cd /Users/ermanergoz/Desktop/PegSolitarie-gates` as its own Bash call before your first
  `git commit`, or the commit is refused as "on develop".
- **This host is shared.** Until C2 lands, `pgrep -fl GradleDaemon` before every long task and do not
  start a build while another is running in your worktree. After C2, run everything through
  `./scripts/check.sh`. Use `--max-workers=4` and `--offline` (the PLAT-07/D-18 invariant: `check --offline`
  is the gate; kover's artifacts need one warm online resolution first — C6).

## 2. C1 — F1, the result-store self-heal

**File:** `build-logic/src/main/kotlin/standards.gradle.kts` only. Add, with the comment from
`tech-lead-depthsweep.md` F1 (why `doFirst`, why not per-module):

```kotlin
tasks.withType<Test>().configureEach {
    val binaryResults = binaryResultsDirectory
    doFirst { binaryResults.get().asFile.deleteRecursively() }
}
```

Covers `:domain:test`, `:domain:depthSweep`, `:domain:fuzz` (C5), `:app:testDebugUnitTest`. Captures only
the task's own `DirectoryProperty`; no `Project` reference at execution time.

**Red first (the failing acceptance, on the tree before C1):**
```
./gradlew :domain:test --offline --max-workers=4                    # green store
: > domain/build/test-results/test/binary/results-generic.bin      # truncate; output-events.bin stays
./gradlew :domain:test --offline --rerun-tasks --max-workers=4     # EXPECT: BUILD FAILED in ~4 s, java.io.EOFException, no test ran
```
Record the failure text. Then apply C1 and:
```
./gradlew :domain:test --offline --rerun-tasks --max-workers=4     # EXPECT: BUILD SUCCESSFUL, tests ran (TEST-*.xml tests= > 0)
```
Repeat the plant-and-run for `app/build/test-results/testDebugUnitTest/binary/` with
`./gradlew :app:testDebugUnitTest --offline --rerun-tasks --max-workers=4`. For the sweep, plant the same
wedge in `domain/build/test-results/depthSweep/binary/` **immediately before your final full
`./scripts/check.sh --offline`** (§8): the sweep running to green inside that run is C1's third
acceptance, at no extra cost. `--rerun-tasks` matters: the wedge read is inside the task action, so an
up-to-date skip would mask it (E3b-1 in the depthsweep report proved `--rerun-tasks` alone does not help).

## 3. C2 — F2, the per-worktree lock

**Files:** `scripts/check.sh` (new, `chmod +x`, `#!/usr/bin/env bash`, `set -euo pipefail`, bash 3.2
compatible — macOS ships 3.2, CI is Linux); `CLAUDE.md` line 6 → `Check command: ./scripts/check.sh`;
**`AGENTS.md` created as a byte-identical copy of `CLAUDE.md`** (rules/stacks.md contract 1; design §16
item 8 decides it is written now, with this line change). `.github/workflows/ci.yml` is **not** changed:
one build per runner needs no lock.

**Behaviour:**
1. `root` = the worktree root (`cd "$(dirname "$0")/.." && pwd`); `lock="$root/.gradle/ergoz-build.lock"`;
   `mkdir -p "$root/.gradle"` (already git-ignored — confirm with `git check-ignore -q .gradle/x && echo ignored`).
2. Acquire with `mkdir "$lock"` (atomic on macOS and Linux; `flock` does not exist on macOS, `lockf`/`shlock`
   are BSD-only). On success write `$$` to `$lock/pid`.
3. On contention read `$lock/pid`. If that PID is **not alive** (`kill -0 "$pid"` fails, or the file is
   missing/empty), the lock is stale (a `kill -9` skips the trap): `rm -rf "$lock"` and retry once.
   Else, with `--wait` (consumed, never forwarded to Gradle): poll every 5 s until acquired. Else print
   **one line to stderr**: `check.sh: another build (pid <P>) holds <lock> in <root>; wait for it, pass
   --wait, or if pid <P> is dead: rm -rf <lock>` and `exit 75`.
4. `trap 'rm -rf "$lock"' EXIT INT TERM HUP`, then run `"$root/gradlew" check "$@"` as a plain call
   (**not `exec`**, which would drop the trap) and exit with Gradle's status.

**Acceptance (record each):**
- Dead-PID reclaim: `mkdir .gradle/ergoz-build.lock && echo 99999999 > .gradle/ergoz-build.lock/pid`;
  `./scripts/check.sh --offline --dry-run` → reclaims, runs, exits 0; `test ! -d .gradle/ergoz-build.lock`.
- Live contention: `mkdir .gradle/ergoz-build.lock && echo $$ > .gradle/ergoz-build.lock/pid` (your shell
  is alive); `./scripts/check.sh --offline --dry-run` → the one-line message, exit **75**, no Gradle run.
  Then `./scripts/check.sh --wait --offline --dry-run &`, `sleep 3`, `rm -rf .gradle/ergoz-build.lock` →
  the waiting run proceeds and exits 0.
- Clean release: after any run, `test ! -d .gradle/ergoz-build.lock`.
- Signal paths: start `./scripts/check.sh --offline :domain:test &`, `sleep 5`, `kill -TERM $!` → the lock
  directory is gone (trap ran); repeat with `kill -9 $!` → the directory remains with a dead PID, and the
  next invocation reclaims it (first bullet).
- `grep -n '^Check command' CLAUDE.md AGENTS.md` → both `./scripts/check.sh`; `cmp CLAUDE.md AGENTS.md` → identical.
- `shellcheck scripts/check.sh` if installed; say so if it is not.

Scope limit, recorded: the lock covers the Check command, not every hand-run `./gradlew <task>`. C1 is
what makes that acceptable (a collision costs one retry, never a wedge).

## 4. C3 — auditor F-2 and F-1 from R-1

**Files:** `build-logic/src/main/kotlin/SourceSetPolicy.kt`, `build-logic/src/test/kotlin/SourceSetPolicyTest.kt`,
`build-logic/src/main/kotlin/ReleaseInvocation.kt` (KDoc only).

- **Test first:** add `aTestDimensionNobodyHasClassifiedTripsTheWire` asserting
  `SourceSetPolicy.unknownSourceSetNames(listOf("main", "test", "androidTest"))` equals `listOf("androidTest")`.
  It is **red today** (the name is pre-authorised). Watch it fail.
- **Fix:** `KNOWN_SOURCE_SET_NAMES = setOf("main", "test", "debug", "release", "testDebug")` — exactly the
  five directories that exist. Rewrite the KDoc: a name is known only once it exists on disk **and** both
  lists have been decided for it; the pre-authorised `androidTest*`/`screenshotTest*`/`testRelease` were a
  hole because their Kotlin would be seen by no detekt task (only `VARIANT_SOURCE_SET_NAMES` feeds the
  widening). Design §9.3.
- **F-1 KDoc** (`ReleaseInvocation.kt:19`): replace the false universal with the auditor's wording — every
  task that *produces or installs* a shippable release artifact starts with one of these verbs; some
  internal AGP tasks (`packageReleaseResources`, `bundleReleaseClassesToCompileJar`) share the prefix and
  are harmlessly caught; `signReleaseBundle` does not match and is covered by its `packageReleaseBundle`
  dependency.
- **Acceptance:** `./gradlew -p build-logic test --rerun-tasks` → 55 tests, 0 failures (54 + 1).
  `./gradlew :app:tasks :domain:tasks --offline -q` → configures (the real tree has only known names).
  **Negative control:** `mkdir -p app/src/androidTest/java && ./gradlew :app:tasks --offline` →
  configuration **fails** with `Unclassified source set(s) in :app: src/androidTest`; `rm -rf app/src/androidTest`;
  `git status --porcelain` shows only your intended files.

## 5. C4 — `LevelGenerator.generateLadder`

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt`; tests in
`domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/` (a new `LevelGeneratorLadderTest`, or
extend `ChallengeSolvabilityTest`).

```kotlin
/** Every accepted level of the deterministic chain, 1..clamp(upTo), in order; O(upTo) in total. */
fun generateLadder(upTo: Int): Sequence<GeneratedLevel>
```
`generate(n)` becomes `generateLadder(n).last()`. The loop body — `DifficultyTiers.forLevel`,
`generateForLevel(level, tier, forbiddenKeys = recentKeys.toHashSet())`, the `recentKeys` push/pop at
`DEDUP_WINDOW` — moves inside `sequence { … yield(produced) … }` **unchanged**. KDoc: the frozen contract is
about output; this exposes the same chain so the harness can walk it once (design §7.3 item 2).

**Tests first, named as sentences:**
- `theLadderYieldsEachLevelInOrderAndEndsWhereGenerateDoes`: for `upTo` in `1, 2, 3, 10, 100, 1000`,
  `generateLadder(upTo).toList()` has size `upTo`, element `k-1` has `levelNumber == k`, and for each
  sampled `k ≤ upTo` element `k-1` equals `generate(k)` on `startingBoard.pegs`, `startingBoard.spec.inPlay`,
  `solution`, `generatorVersion` (the equality `ChallengeSolvabilityTest` already uses — freeform specs are
  fresh instances, so compare content, not identity).
- `theLadderClampsItsBoundLikeGenerate`: `generateLadder(0).count() == 1` and `generateLadder(-5).count() == 1`.
  (Do not test `MAX + 1` here — that is a 10,000-level run and belongs to C5.)

**Acceptance:** `./gradlew :domain:test --offline --rerun-tasks --max-workers=4` green, **every golden test
unchanged** (`ChallengeGeneratorGoldenTest`, `SolutionTest`, `QualityGateTest`, `FreeformValidityTest`,
`OpeningBookTest`, `GetHintUseCaseTest`, `BoardSpecTest`). `git diff develop...HEAD -- domain/src/main` must
show the lifted loop and nothing else in that file.

## 6. C5 — the fuzz harness and its tiers

**Files:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/TestFlags.kt` (add `RUN_FUZZ`, `FUZZ_SEEDS`,
and `fun value(flag: String): String?` reading env then `-D`, like `isEnabled`);
`domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/FuzzSeeds.kt` (test source; pure parse) and
`FuzzSeedsTest.kt`; `FuzzHarnessTest.kt`; `domain/build.gradle.kts` (the task); `.github/workflows/ci.yml`
(env only).

**`FuzzSeeds.parse(raw: String?): Int` — test first, runner-free:** `"1000"` → 1000; `"100000"` → 100000;
`null`, `""`, `"abc"`, `"0"`, `"-7"`, `"1e3"` → throws `IllegalArgumentException("FUZZ_SEEDS must be a positive
integer; was '<raw>'")`. rules/stacks.md contract 6: a non-integer value fails.

**`FuzzHarnessTest` — one method, gated like the sweep:**
```
if (!TestFlags.isEnabled(TestFlags.RUN_FUZZ)) return
val requested = FuzzSeeds.parse(TestFlags.value(TestFlags.FUZZ_SEEDS))
val upTo = minOf(requested, ChallengeLevelBounds.MAX)
if (requested > ChallengeLevelBounds.MAX) println("fuzz: requested $requested exceeds the level space ${ChallengeLevelBounds.MAX}; running the whole space (exhaustive)")
```
Then: delete `build/fuzz-failures.txt` (cwd is the module dir, so this is `domain/build/fuzz-failures.txt`);
first pass `LevelGenerator.generateLadder(upTo).forEachIndexed { i, level -> … }` collecting a failure line
per level for any of: `levelNumber != i + 1`; a move not in `RulesEngine.legalMoves(board)` on the board it
is applied to; final `pegCount != 1`; a `from`/`over`/`to` outside `startingBoard.spec`;
`generatorVersion != GeneratorConstants.GENERATOR_VERSION`; keep each level (pegs, inPlay, solution, version)
for the second pass. Second pass: `generateLadder(upTo)` again, element-wise equal to the first (determinism,
invariant 3). Count `examined`. If failures exist, write them (one line each, `level <n>: <reason>`) to the
file **before** asserting. Print one summary line: `fuzz: examined <examined> levels (requested <requested>),
determinism <pass|fail>, <k> failures`. Assert `examined == upTo` and `examined > 0` (anti-vacuity, the
sweep's T-08-39 standard), then `failures.isEmpty()` with the file path in the message.

**Task, in `domain/build.gradle.kts`, in `depthSweep`'s exact shape:**
```kotlin
// Default FUZZ_SEEDS for a local `check`; chosen by measurement — see the C5 report. CI pins 10,000;
// the release runs FUZZ_SEEDS=100000 (= the whole 10,000-level space, design §7.3).
val fuzzSeedsDefault = "<measured; see §7>"
val fuzzSeeds = providers.environmentVariable("FUZZ_SEEDS").orElse(fuzzSeedsDefault)
val fuzz = tasks.register<Test>("fuzz") {
    group = "verification"; description = "Generate → verify-certificate → replay → win over FUZZ_SEEDS Challenge levels (rules/puzzle-design.md 8)."
    testClassesDirs = sourceSets["test"].output.classesDirs; classpath = sourceSets["test"].runtimeClasspath
    systemProperty("RUN_FUZZ", "true"); systemProperty("FUZZ_SEEDS", fuzzSeeds.get())   // forwarded verbatim; the test validates
    inputs.property("fuzzSeeds", fuzzSeeds)
    filter { includeTestsMatching("*FuzzHarnessTest*") }
    testLogging { showStandardStreams = true }
}
tasks.named("check") { dependsOn(fuzz) }
```
No `outputs.upToDateWhen { false }`: unlike the sweep this is a pure function of code and `N`; the input
property makes a tier change re-run. **CI:** add `env: FUZZ_SEEDS: "10000"` to the `Check command + debug
build` step — explicit whatever the local default.

**Acceptance and negative controls (record each):**
- `./gradlew :domain:test --offline` → `FuzzHarnessTest` passes as a no-op in ms (flag unset).
- `FUZZ_SEEDS=abc ./gradlew :domain:fuzz --offline` → BUILD FAILED, message names `'abc'`; `FUZZ_SEEDS=0` likewise.
- `FUZZ_SEEDS=100000 ./gradlew :domain:fuzz --offline` → green, console shows the exhaustive note and
  `examined 10000`.
- **Subject mutation (the harness must catch a broken generator):** scratch-edit `LevelGenerator` so that for
  levels divisible by 97 the recorded solution drops its last move; `FUZZ_SEEDS=1000 ./gradlew :domain:fuzz
  --offline` → BUILD FAILED; `domain/build/fuzz-failures.txt` lists exactly ten levels (97 … 970) with
  `pegCount` 2; revert; `git diff develop...HEAD -- domain/src/main` shows C4 only; the file is absent after
  the next green run.
- **Vacuity mutation:** scratch-edit the harness to skip the first pass; it must fail on `examined == upTo`.
- `FUZZ_SEEDS=1000 ./gradlew :domain:fuzz --offline` twice → second run `UP-TO-DATE`; `FUZZ_SEEDS=2000` → re-runs.
- `./gradlew check --offline -m | grep ':domain:fuzz'` → present.

## 7. The measurement that chooses the `check` tier

Do this **after** C5 is green and **before** you write `fuzzSeedsDefault`. Through the lock
(`./scripts/check.sh` is for `check`; for the single task confirm `pgrep -fl 'GradleDaemon|KotlinCompileDaemon'`
shows only your daemon and record `uptime` before each run):

```
for n in 1000 10000; do for r in 1 2; do
  FUZZ_SEEDS=$n ./gradlew :domain:fuzz --offline --rerun-tasks --max-workers=4
  grep -o 'testcase name="[^"]*" classname="[^"]*" time="[0-9.]*"' domain/build/test-results/fuzz/TEST-*.xml
done; done
```
Record the four `time=` values (the test method's own seconds, not Gradle's wall clock), the `uptime` loads,
and levels/s. **Apply this rule, verbatim, and quote it in your report:**

1. If **10,000 ≤ 60 s** on the second (warm) run → `fuzzSeedsDefault = "10000"`. A local `check` is then
   exhaustive, equal to CI, and rules/stacks.md's "10,000 by default" is met.
2. Else if **1,000 ≤ 60 s** → `fuzzSeedsDefault = "1000"`; CI's explicit `FUZZ_SEEDS=10000` is what makes the
   PR gate exhaustive.
3. Else **stop and report the numbers**; do not pick a tier. The tech-lead decides.

Also record the wall-clock delta of `./scripts/check.sh --offline` before and after the branch (two numbers).
`check` costs ~11 minutes today; the budget for the default tier is 60 s because it does not get a second
multi-minute gate (`tech-lead-prerelease-signoff.md` §D).

## 8. C6 — kover on `:domain`

**Files:** `gradle/libs.versions.toml` (`[versions] kover`, `[plugins] kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }`,
with a rationale comment and the date, as every pin in that file has); `domain/build.gradle.kts`
(`alias(libs.plugins.kover)`, the config below); nothing in `:app`.

- **Version:** the newest stable on the Gradle Plugin Portal / Maven Central for
  `org.jetbrains.kotlinx.kover` (rules/stacks.md Versions: the plugin's `scripts/latest.sh` from
  `/Users/ermanergoz/Desktop/ergoz-soft`, or the registry page); no pre-release. Record it in the catalog
  comment. Licence Apache-2.0 (JetBrains), build-only, non-copyleft → autonomous; the `THIRD_PARTY.md`
  build-tooling row gains the word "kover" on the release-docs branch — **not your file; name it in your
  report for the release-manager.**
- **Scope and exclusions:** `:domain` only. kover must **not** instrument or depend on `depthSweep` or `fuzz`
  (kover 0.9 DSL: `kover { currentProject { instrumentation { disabledForTestTasks.addAll("depthSweep", "fuzz") } } }`
  — use the DSL of the version you pin; the acceptance below is what matters, not the spelling).
- **Measure first:** `./gradlew :domain:test :domain:koverXmlReport --offline` (one online warm first if
  the artifacts are not cached — record it, PLAT-07/D-18), read `<counter type="LINE" missed="…" covered="…"/>`
  at the report root of `domain/build/reports/kover/report.xml`, compute covered / (covered + missed).
- **Bound (design §8.4, verbatim):** ≥ 90 % → `minBound(90)` and stop. Below → the measured floor rounded
  **down** to a whole percent, and your report states the measured figure, the bound, the shortfall and the
  90 target. Nobody writes tests to chase a number this week. Wire `check.dependsOn("koverVerify")` if the
  plugin does not attach it itself (check with `-m` first).

**Acceptance and negative controls:**
- `./gradlew :domain:koverVerify --offline -m` lists `:domain:test` and **not** `:domain:depthSweep` or `:domain:fuzz`.
- `./gradlew check --offline -m | grep ':domain:koverVerify'` → present; `grep ':app:kover'` → nothing.
- **Mutation:** set the bound to `100` temporarily → `./gradlew :domain:koverVerify --offline` FAILS naming the
  rule; revert.
- `./gradlew check --offline` green after the warm run (the offline invariant holds with the new plugin).
- **Pre-ruled fallback, so you do not wait on me:** if the kover plugin cannot apply beside AGP 9.1.0 /
  Kotlin 2.3.20 in this build, report the exact failure and version, and use Gradle's built-in `jacoco` on
  `:domain` with `jacocoTestCoverageVerification` at the same bound rule and the same two tasks excluded.
  That is a recorded deviation from rules/testing.md ("kover on Kotlin") which the tech-lead transcribes
  into the design at sign-off; it is not yours to argue.

## 9. Done means

- `./scripts/check.sh --offline` green, with the depth-sweep wedge planted immediately before it (§2);
  `./gradlew -p build-logic test --rerun-tasks` green (55); `git status --porcelain` empty.
- No new `@Suppress`, `@Ignore`, baseline, disabled test, `ignoreFailures` or `!!`
  (`git diff develop...HEAD | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|baseline|ignoreFailures|!!)'` → 0,
  excluding your report's prose).
- `git diff develop...HEAD --stat` touches only: `build-logic/src/main/kotlin/standards.gradle.kts`,
  `build-logic/src/main/kotlin/SourceSetPolicy.kt`, `build-logic/src/test/kotlin/SourceSetPolicyTest.kt`,
  `build-logic/src/main/kotlin/ReleaseInvocation.kt`, `scripts/check.sh`, `CLAUDE.md`, `AGENTS.md`,
  `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt`, `domain/src/test/**`,
  `domain/build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/ci.yml`, and
  `.planning/reports/phase-14/android-game-dev-check-gates.md`.
- Your report (rules/reporting.md; `Ran on:` and `Lessons applied:` lines) carries, per commit, the red-first
  evidence, every acceptance command with its one-line result, the negative controls, the four fuzz timings
  with host load and the rule applied, the two `check` wall clocks, the kover figure and bound, the kover
  version and its licence, and the two items routed onward: the `THIRD_PARTY.md` word for the
  release-manager, and `docs/release-checklist.md` 3.1/3.2 (`FUZZ_SEEDS=100000 ./scripts/check.sh`;
  `domain/build/fuzz-failures.txt`) for the same role.

## 10. Out of scope — do not do these here

Any generator behaviour change or Diamond-41 book work; the `:domain:depthSweep` CI-job split (threshold
ruled in the design §9.7, decided on the producer's measured figure); `outputs.upToDateWhen { false }` on the
sweep (it stays); `:app` coverage; ktlint/detekt over `build-logic` (auditor F-4, post-v1.1); hoisting the
package-task names onto `ReleaseInvocation` (F-3, post-v1.1); the R-1 developer report's F-5 wording (the
auditor's report is the correction of record); routing CI through `scripts/check.sh`; `--no-daemon`; the
Macrobenchmark module; any UI, ViewModel or `:app` production code; `docs/tech-design.md` (tech-lead),
`docs/release-checklist.md` and `THIRD_PARTY.md` (release-manager); any phase-15, audio-review or
candy-backdrop file. If a step costs materially more than this brief implies or fights the toolchain,
stop and report the number rather than growing the task.
