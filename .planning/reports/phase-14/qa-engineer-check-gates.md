# qa-engineer report — phase 14 R-2 check-gates (review mode, no device session)
Date: 2026-10-24
Branch: feature/phase-14-check-gates (report only; committed by the producer). Reviewed: `develop` `c5f2c54`...`6011842` — code `348279d..f15e58a` (C1–C7), the release-manager's `ec49df3` and `77dcf88`, the sign-off commit `6011842` at the head
Skills loaded: ergoz-soft:standards (role: qa-engineer) — plugin copies (1.0.0, newer than the repo snapshot 0.5.1) of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md; repo `.claude/rules/lessons.md`; android-testing; android-emulator-skill (loaded as required; no device was driven: the tech-lead's ruling 1 in `tech-lead-signoff-check-gates.md`)
Ran in parallel with: the wait for `bugfix/hint-undecided` (producer's queue); no other build ran in this worktree
Ran on: claude-fable-5-1 at effort high
Fan-out: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
- The Check is green on the head: `./scripts/check.sh --offline --rerun-tasks --max-workers=4` exited 0 in 11 m 37 s, 104 of 104 tasks executed (none `FROM-CACHE`/`UP-TO-DATE`), 280 + 1 + 1 `:domain` tests and 1,067 `:app` tests, 0 failed, 0 skipped; the four wall-clock solver deadline tests passed at 1-minute load 3.9 → 3.1 (0.001–0.396 s each).
- Invariant 8 is real and exhaustive: `:domain:fuzz` walks all 10,000 Challenge levels (the whole finite level space, `MAX_CHALLENGE_LEVEL = 10_000`), replays every certificate to one peg and regenerates the ladder identically, in 5.9–6.1 s; `FUZZ_SEEDS=100000` prints the "exceeds the level space … exhaustive" line and examines 10,000; `10k`, `''` and `0` fail naming the value.
- The harness bites: a planted certificate mutation (drop the last move of every 101st level) produced `99 failures`, a 99-line `domain/build/fuzz-failures.txt`, a red `:domain:fuzz` and therefore a red `check`, while `:domain:test` stayed green; a planted empty ladder failed on the anti-vacuity assertion (`expected: 10000, but was: 0`). Reverted, the copy was byte-identical and green again.
- The literal gates and the tripwire behave as C3/C3a claim: planted literals in `src/main/assets/`, `src/debug/res/` and `src/main/resources/` each fail their gate naming file and line; a `#` comment line and a `src/test` literal pass; `src/staging` fails configuration naming both lists. kover: `koverVerify` in `check`, `:domain` at 896/905 lines = 99.0 % against `minBound(90)`.
- Three doc-level findings for the release checklist, none a code defect, listed under Open issues: 3.3's evidence file is not produced by the command 3.1 runs; a cached green `:domain:fuzz` leaves a stale failures file behind (fail-closed; `--rerun-tasks` covers the release); 3.4's console proof needs a plain console.

## What was reviewed, against which rule

**`FuzzHarnessTest` (invariant 8: generate → validate → solve → replay → verify win).**
- *Generate:* `LevelGenerator.generateLadder(upTo)` walks levels `1..min(N, MAX)` in one O(N) pass — the chain `generate(n)` has always walked (C4 lifts the loop body unchanged; `git diff develop...HEAD -- domain/src/main` is that one file).
- *Validate:* per level, `levelNumber == index + 1`, `generatorVersion == GENERATOR_VERSION`, every move's three positions inside the footprint, every move in `RulesEngine.legalMoves(board)` on the board it is applied to. What it does **not** re-check is the five generator quality gates and the dedup window (invariant 7, WR-01); see Open issues 3 — my census over all 10,000 levels found 0 offenders, so the gap is theoretical today.
- *Solve:* certificate verification, as `docs/tech-design.md` §7.3 item 3 pre-registers. Sound: the recorded solution replayed through the unchanged `RulesEngine` to exactly one peg **is** a proof of solvability (invariant 1); an independent solver run would test the solver, not the level, at the depth sweep's cost ten-thousand-fold.
- *Replay → verify win:* the replay stops at the first illegal or out-of-footprint move and asserts `pegCount == 1` at the end; the failure line names the level, the step and the move.
- *Over the run:* a second, independently built ladder compared element-wise on content (`pegs`, `inPlay`, `solution`, version, number) and on length — invariant 3. The `LevelRecord` comparison is right to compare `inPlay` rather than the `BoardSpec` instance, since a freeform footprint is a fresh instance per generation.
- *"Exhaustive at 10,000 = `MAX_CHALLENGE_LEVEL`":* sound. The seed **is** the level number (there is no other input to `generate`), `ChallengeLevelBounds.clamp` folds every number above 10,000 onto 10,000, and the player reaches level n through `generate(n) = generateLadder(n).last()`, so the 10,000 levels the harness walks are the only 10,000 boards the product can ever deal. 100,000 therefore cannot exceed 10,000 and the harness says so on the console. This is strictly stronger than 100,000 samples from an infinite space, which is what rules/puzzle-design.md's count is written for.
- *Anti-vacuity:* real and not circular. `examined` is a counter incremented per yielded level and is asserted equal to `upTo` **and** greater than zero, so neither an empty ladder nor a wrong bound can read as a pass; the planted empty ladder failed exactly there (`mutant-vacuity-fuzz.log`). The `FUZZ_SEEDS` path cannot produce `upTo = 0` either (`FuzzSeeds.parse` rejects 0).
- *Failures file:* `File("build", "fuzz-failures.txt")`, relative to the Test task's working directory, which Gradle defaults to the project directory, so it lands at `domain/build/fuzz-failures.txt` as the checklist reads it (proven: the mutant wrote `<copy>/domain/build/fuzz-failures.txt`); deleted at the start of every **executed** run; written before the assertion fails.
- *Gating:* `TestFlags.RUN_FUZZ`, set only by `:domain:fuzz`; under `./gradlew :domain:test --tests '*FuzzHarnessTest*'` the body returns at once (0.009 s, no `fuzz:` line). Belt and braces as the sweep.
- *Deterministic tests:* no wall clock, no random, no network, no sleep in any new test; the file I/O is deterministic. *Test names read as sentences:* yes, all five new tests.

**`FuzzSeeds` / `FuzzSeedsTest` (parsing contract, rules/stacks.md contract 6).** `requireNotNull(raw?.toIntOrNull()?.takeIf { it > 0 })` with a message naming the raw value. Tested: `1000`, `100000` accepted; `null`, `""`, `abc`, `1e3`, `0`, `-7` rejected with the exact message. Verified end to end through Gradle: `FUZZ_SEEDS=10k`, `FUZZ_SEEDS=` (empty — the provider's `orElse` does not apply to a present-but-empty variable, so it reaches the parser and fails, which is the right outcome) and `FUZZ_SEEDS=0` each fail `:domain:fuzz` with `FUZZ_SEEDS must be a positive integer; was '…'` on the console (`TestExceptionFormat.FULL`). Lenient only in that Kotlin's `toIntOrNull` accepts a leading sign and leading zeros (`+5`, `010`), which cannot change a count silently; not a gap.

**`LevelGeneratorLadderTest` (C4).** Pins, for bounds 1, 2, 3, 10, 100, 1000: the ladder has `upTo` elements, numbered in order, and at every sampled k the k-th element equals `generate(k)` on content; and that 0 and −5 clamp to one level numbered `MIN`. The bytes stay pinned by `ChallengeGeneratorGoldenTest` and `SolutionFixtures`, unchanged and green in the Check. The upper clamp (`upTo > MAX` yields exactly 10,000) is not in this test but is proven by the harness's release-tier run (`examined 10000 (requested 100000)`). Adequate.

**`SourceSetPolicyTest` (C3/C3a).** 10 tests, run by hand (`./gradlew -p build-logic test --offline --rerun-tasks`: 55 tests, 0 failed, 0 skipped). They pin the policy — the three shipped roots, their derived description, the test-set exclusion, the detekt widening, the known-name set and the tripwire's message. `everyFileOfAShippedSourceSetIsUnderAScannedRoot` is a string-prefix check over the test's own list, so it proves the list and not the gate's traversal; the traversal lives in `standards.gradle.kts` (a script plugin, unit-testable only through TestKit) and is proven here by probes instead: `probe-adid-gate.log`, `probe-gamesid-gate.log`, `probe-gates-clean.log`, `probe-tripwire-staging.log`. Every gate's failure text says `Scanned: every file under src/main, src/release, src/debug`, which is what it did.

**The kover bound and exclusions (C6).** `domain/build.gradle.kts`: kover 0.9.11 on `:domain` only; `instrumentation.disabledForTestTasks("depthSweep", "fuzz")` and **no class or package exclusion**, so nothing is hidden from the 90 % line bound; `minBound(90)` with kover's defaults (LINE, covered percentage, whole application). `koverVerify` attaches itself to `check` (`> Task :domain:koverCachedVerify`, `> Task :domain:koverVerify` after `:domain:test` in `check.log`); measured on this tree: LINE 896 covered / 9 missed = 99.0 %, BRANCH 494/41 = 92.3 %, METHOD 223/2, CLASS 65/2 (`kover-root-counters.txt`, from a fresh `:domain:koverXmlReport`). Rule met (≥ 90 %).

**The release checklist's 1.2 and 3.1–3.4, run as written.**
- 1.2 / 3.1 `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks`: runnable — `check.sh` forwards every non-`--wait` argument to `./gradlew check` verbatim, `--rerun-tasks` executed all 104 tasks here, and the two evidence lines print exactly as the checklist quotes them (`fuzz-release-100000.log` lines 36–37, with `:domain:fuzz` on its own; inside a Check they print the same way, `check.log` line 200). Wall clock 11 m 37 s on this host.
- 3.2 `test ! -e domain/build/fuzz-failures.txt`: the right path; absent after every green executed run here. Caveat in Open issues 2 (a **cached** green run does not delete a stale file; the checklist's `--rerun-tasks` is what makes 3.2 sound, so the two items must stay coupled).
- 3.3: **not runnable as written** — see Open issues 1. `check` runs `koverVerify`, which writes `domain/build/reports/kover/verify.err` and **not** `report.xml`: after my `--rerun-tasks` Check the XML still carried its 00:27 mtime from an earlier hand run, and on a fresh clone of the release branch it would not exist. The figure needs one more command after 3.1: `./gradlew :domain:koverXmlReport --offline` (1 s, `:domain:test` stays up to date), which is how I produced the 99.0 % above.
- 3.4: the four literal gates declare no outputs, so they can never be `FROM-CACHE`/`UP-TO-DATE`; all eight (`:app` and `:domain`) executed here (`check.log` lines 23–47). The proof "confirm in the console" needs a plain console: on a TTY Gradle's rich console does not keep a line per executed task. Run 3.1 with output to a file (as §9.6's operator rule already requires) or with `--console=plain`.

**CI (C3b/C5).** `ci.yml` read in full: push and PR on `develop`, `release/**`, `main`; `FUZZ_SEEDS: "10000"` on the Check step; the build-logic suite runs first by name. CI has still never run (no remote); read from the YAML only.

**Warnings as errors (C7).** `check.log` has 31 `w:` lines: 30 from `:app:compileDebugUnitTestKotlin` (the held unit-test deviation, §9.8 rule 2) and one Kotlin-daemon notice (`Detected multiple Kotlin daemon sessions`), none from a main or `:domain` compilation. No device session: ruling 1 accepted — the diff changes imports, annotation targets and compiler flags, and nothing on a screen.

**Standing checks.** `git diff develop...HEAD -- . ':(exclude).planning' | grep -cE '@Suppress|@Ignore|Disabled|baseline|ignoreFailures|!!'` → 0. `git status --short` clean before and after every run; no file of the repo was edited; nothing committed.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/.planning/reports/phase-14/qa-engineer-check-gates.md` — this report
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/.planning/qa/phase-14-check-gates/` — session artifacts (176 KB, git-ignored, never committed), each listed below

## Artifacts (`.planning/qa/phase-14-check-gates/`)
| File | Size | What it shows |
|---|---|---|
| `check.log` | 26.3 KB | The full Check, plain console: `BUILD SUCCESSFUL in 11m 37s`, `104 actionable tasks: 104 executed`; lines 23–47 the eight literal-gate tasks executed; line 200 the fuzz summary; lines 204–206 kover in `check` |
| `check-uptime.txt` | 207 B | `uptime` before and after the Check: load 3.91 → 3.14; exit 0 |
| `fuzz-ci-10000.log`, `fuzz-ci-10000-junit.xml`, `fuzz-ci-uptime.txt` | 1.9 KB, 614 B, 94 B | CI tier alone: `examined 10000 levels (requested 10000), determinism pass, 0 failures`; method time 5.936 s; load 2.97 |
| `fuzz-release-100000.log` | 2.0 KB | Release tier: the two checklist evidence lines (quoted below); exit 0; no failures file |
| `fuzz-bad-10k.log`, `fuzz-empty.log`, `fuzz-zero.log` | ≈ 2.5 KB each | `FUZZ_SEEDS=10k`, `=` (empty), `=0` each fail `:domain:fuzz` naming the value; exit 1 |
| `plain-test-fuzz-gated.log` | 1.7 KB | `:domain:test --tests '*FuzzHarnessTest*'`: green in 0.009 s, no `fuzz:` line — the body is gated off |
| `mutant-certificate-fuzz.log`, `mutant-certificate-fuzz-failures.txt` | 7.1 KB, 4.1 KB | Planted mutation (last move dropped on every 101st level) in a throwaway copy: `99 failures`, `:domain:fuzz FAILED`, exit 1; the 99-line failures file (`level 101: final pegCount 2, expected 1` … `level 9999: …`) |
| `mutant-certificate-domain-test.log` | 1.9 KB | The same mutant under `:domain:test`: green — only the harness catches it |
| `mutant-reverted-fuzz.log`, `mutant-reverted-fuzz-rerun.log` | 1.9 KB each | Reverted copy: `:domain:fuzz FROM-CACHE` (stale failures file survived, Open issues 2); then with `--rerun-tasks`: executed, green, file gone |
| `mutant-vacuity-fuzz.log` | 2.9 KB | Planted empty ladder: `examined 0 levels (requested 10000)`, assertion `levels examined (anti-vacuity, T-08-39) expected: 10000 but was: 0`, exit 1 |
| `probe-adid-gate.log`, `probe-gamesid-gate.log`, `probe-gates-clean.log`, `probe-tripwire-staging.log` | 2.4, 2.3, 1.5, 2.1 KB | Literal-gate and tripwire probes in the copy (quoted below) |
| `kover-xml-report.log`, `kover-root-counters.txt` | 2.0 KB, 197 B | `:domain:koverXmlReport` with `:domain:test UP-TO-DATE`; the root counters: LINE missed 9 covered 896 |
| `build-logic-test.log` | 864 B | `-p build-logic test --rerun-tasks`: 55 tests, 0 failed/skipped; `SourceSetPolicyTest` 10 |
| `census-probe.txt`, `census-probe-full.log`, `QaCensusProbeTest.kt.helper` | 210 B, 45 KB, 2.4 KB | QA helper test run in a throwaway copy, then deleted with the copy: over all 10,000 levels, 0 below `minSolutionLength`, 0 outside `pegRange`, 0 below `minOpeningMoves`, 0 below `minSpread`, 0 canonical-key collisions inside the 8-level dedup window |

Log excerpts that matter (≤ 20 lines):

    check.log:215  BUILD SUCCESSFUL in 11m 37s
    check.log:216  104 actionable tasks: 104 executed
    check.log:200      fuzz: examined 10000 levels (requested 10000), determinism pass, 0 failures
    fuzz-release-100000.log:36  fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)
    fuzz-release-100000.log:37  fuzz: examined 10000 levels (requested 100000), determinism pass, 0 failures
    fuzz-bad-10k.log:35   java.lang.IllegalArgumentException: FUZZ_SEEDS must be a positive integer; was '10k'
    mutant-certificate-fuzz.log:35  fuzz: examined 10000 levels (requested 10000), determinism pass, 99 failures
    mutant-certificate-fuzz.log:38  failing levels, also written to …/gates-mutant/domain/build/fuzz-failures.txt
    mutant-vacuity-fuzz.log:38-40   levels examined (anti-vacuity, T-08-39) / expected: 10000 / but was : 0
    mutant-reverted-fuzz.log:31  > Task :domain:fuzz FROM-CACHE
    probe-adid-gate.log:26  > Ad-unit-ID literals are forbidden in shipped source (PLAT-06 / D-05). … Scanned: every file under src/main, src/release, src/debug. Test source sets are excluded on purpose (see SourceSetPolicy).
    probe-adid-gate.log:27      src/main/assets/qa-probe.txt:1: qa probe ca-app-pub-1234567890123456~1234567890
    probe-adid-gate.log:28      src/debug/res/values/qa_probe.xml:2: <string name="qa_probe">ca-app-pub-1234567890123456/1234567890</string>
    probe-gamesid-gate.log:27   src/main/resources/qa-games.txt:1: qa probe CgkIabcdefghijkEAIQAQ
    probe-tripwire-staging.log:22   > Unclassified source set(s) in :app: src/staging
    census-probe.txt  census: examined 10000 / belowMinSolutionLength 0 / outsidePegRange 0 / belowMinOpeningMoves 0 / belowMinSpread 0 / dedupCollisionInWindow 0

## Checks
- `./scripts/check.sh --offline --rerun-tasks --max-workers=4 --console=plain` (the Check command, in the worktree, alone on the host) — **pass** (exit 0; 11 m 37 s; 104/104 executed; `:domain:test` 280, `:domain:fuzz` 1, `:domain:depthSweep` 1 (666.3 s), `:app:testDebugUnitTest` 1,067; 0 failures, 0 skipped; deadline tests 0.001–0.396 s at load 3.9 → 3.1)
- `FUZZ_SEEDS=10000 ./gradlew :domain:fuzz --offline --rerun-tasks --max-workers=4` — **pass** (examined 10000, determinism pass, 0 failures; 5.936 s; no `domain/build/fuzz-failures.txt`)
- `FUZZ_SEEDS=100000 ./gradlew :domain:fuzz --offline --rerun-tasks --max-workers=4` — **pass** (the "exceeds the level space … exhaustive" line, examined 10000, 0 failures)
- `FUZZ_SEEDS=10k …`, `FUZZ_SEEDS= …`, `FUZZ_SEEDS=0 …` `:domain:fuzz --rerun-tasks` — **fail as required** (exit 1 each, `FUZZ_SEEDS must be a positive integer; was '10k'` / `''` / `'0'`)
- `./gradlew :domain:test --offline --rerun-tasks --tests '*FuzzHarnessTest*'` — **pass** (gated body: 0.009 s, no `fuzz:` line)
- Throwaway copy, certificate mutant (`level % 101 == 0` → last move dropped): `FUZZ_SEEDS=10000 ./gradlew :domain:fuzz --offline` — **fails as required** (99 failures, 99-line failures file, exit 1); `./gradlew :domain:test --offline` — green (the harness alone catches it); `./gradlew check --offline --dry-run` lists `:domain:fuzz`, `:domain:koverVerify`, the eight literal gates
- Throwaway copy, reverted (`diff -q` identical): `:domain:fuzz --offline` — `FROM-CACHE`, green, stale failures file left; `--rerun-tasks` — executed, green, file deleted
- Throwaway copy, vacuity mutant (empty ladder): `:domain:fuzz` — **fails as required** on the anti-vacuity assertion
- Throwaway copy, literal-gate probes: `:app:adIdLiteralCheck` — **fails as required** naming the `assets/` and `debug/res/` probes and not the `#` comment nor the `src/test` file; `:app:gamesIdLiteralCheck` — **fails as required** naming `src/main/resources/qa-games.txt`; probes removed, the three gates — pass; `mkdir app/src/staging` then `./gradlew help --offline` — **fails as required** at configuration naming `src/staging` and both lists; copy byte-identical to the worktree afterwards (`diff -rq` empty), then deleted
- `./gradlew :domain:koverXmlReport --offline` — pass (LINE 896/905 = 99.0 %, BRANCH 92.3 %; `report.xml` mtime moved from 00:27 to 03:45 only by this command, not by `check`)
- `./gradlew -p build-logic test --offline --rerun-tasks` — pass (55 tests, 0 failed, 0 skipped; `SourceSetPolicyTest` 10)
- Throwaway copy, QA census helper over `generateLadder(10000)` — pass (0 levels below any of the five quality gates, 0 dedup collisions in any 8-level window)
- `git diff develop...HEAD -- . ':(exclude).planning' | grep -cE '@Suppress|@Ignore|Disabled|baseline|ignoreFailures|!!'` — pass (0)
- `git status --short` after every run — pass (clean; nothing edited, nothing committed)
- `uptime` recorded before the fuzz (2.97) and around the Check (3.91 → 3.14); no deadline test failed, so no re-run was needed
- **Not run:** any device or emulator (tech-lead ruling 1: C7 has no acceptance path on a screen; the branch has no plan)

## Open issues / risks
1. **Checklist 3.3 cannot be ticked from 3.1's run as written** (severity: medium, doc only; owner: release-manager through the producer; a docs commit may land on the release branch). `check` runs `koverVerify`, which writes `domain/build/reports/kover/verify.err` and does not write `report.xml`; my `--rerun-tasks` Check left `report.xml` at its earlier hand-run mtime (00:27 vs the Check at 03:32–03:44), and a fresh clone of the release branch would have no file at all, so the release-manager would read a stale figure or none. Fix: 3.3's evidence becomes "after 3.1, `./gradlew :domain:koverXmlReport --offline` (≈ 1 s, `:domain:test` stays up to date), then the root LINE counter of `domain/build/reports/kover/report.xml`". Reproduction: `ls -l domain/build/reports/kover/` before and after `./scripts/check.sh --rerun-tasks`.
2. **A cached green `:domain:fuzz` leaves a stale `fuzz-failures.txt` in place** (severity: low; fail-closed; owner: android-game-dev, backlog). The file is written by the test body and declared as no task output, so a `FROM-CACHE` hit after a red run restores a green result without deleting the file (`mutant-reverted-fuzz.log`). Checklist 3.2 is sound only because 3.1 forces `--rerun-tasks`; the two items must stay coupled as they are. The clean fix is `outputs.file("build/fuzz-failures.txt")` on the task (Gradle removes stale declared outputs before a cache restore), or a `doFirst { delete(...) }` as C1 does for the result store; not a 3.0.0 blocker.
3. **The harness does not re-check the generator's own quality gates** (severity: low today; owner: qa-engineer plan mode, then android-game-dev). `generateForLevel` has a deterministic fallback that accepts the best candidate after `MAX_ATTEMPTS` even when it fails a tier gate or, last of all, collides on the canonical key — so invariant 7 and WR-01 are not true by construction, and `QualityGateTest` proves them on 9 sampled levels only. My census over all 10,000 levels found 0 offenders on all five gates and 0 window collisions, so nothing ships wrong; but a future generator version (`GENERATOR_VERSION` bump) would not be caught. The cheap addition (≈ 2 s at 10,000) belongs in the harness's invariant list in `docs/qa-plan.md` — which does not exist in this repo; the harness was built to `docs/tech-design.md` §7.3 instead. That is QA's own plan-mode debt, to be scheduled by the producer, not a defect of this branch.
4. **The local Check does not run the build-logic suite** (pre-existing; CI runs `-p build-logic test` first). `SourceSetPolicyTest`, `ReleaseInvocation` and the signing resolutions are gated locally only by hand. Noted for the checklist's owner: 3.1 does not cover them; a one-line `./gradlew -p build-logic test` beside 3.1 would.
5. **Checklist 3.4's console proof needs a plain console**: on a TTY Gradle's rich console keeps no line per executed task, so "confirm each one was executed" is readable only with `--console=plain` or output to a file, which §9.6's operator rule (background run, output to a file) already gives. Text only.
6. `SourceSetPolicyTest.everyFileOfAShippedSourceSetIsUnderAScannedRoot` proves the test's own list, not the gate's traversal; the traversal is covered by probes (this report and the auditor's), not by a unit test. Observation, no action owed for 3.0.0.
7. CI has never run (no remote): triggers and `FUZZ_SEEDS` are read from the YAML only.

## Handoff
Next: **tech-lead** — this PASS, with the Check's result above, feeds the sign-off already recorded in `tech-lead-signoff-check-gates.md`; nothing on the branch needs a code change before the merge.
Via the producer: open issue 1 to the release-manager (one evidence line in `docs/release-checklist.md` 3.3, before item 1.2 is run on `develop`); open issues 2 and 3 to the backlog (android-game-dev; qa-engineer plan mode owes `docs/qa-plan.md` with the harness's invariant list); 4 and 5 to the release-manager as checklist text with its next edit.

VERDICT: PASS
