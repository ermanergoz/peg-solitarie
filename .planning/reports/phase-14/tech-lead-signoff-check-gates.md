# tech-lead report — phase-14 R-2 check-gates: sign-off before the merge into develop
Date: 2026-10-24
Branch: feature/phase-14-check-gates (committed by the producer). Reviewed: `develop` `c5f2c54`...`77dcf88` — code `348279d..f15e58a` (C1, C2, C3, C3a, C3b, C4, C5, C6, C7), the release-manager's `ec49df3` and `77dcf88`, the report commits `1c843b6`, `bcb010b`, `9b04cf4`. The head moved from the dispatch's `9b04cf4` to `77dcf88` while I read: the release-manager's parallel docs commit, which closes auditor findings 1 and 2.
Skills loaded: ergoz-soft:standards (role tech-lead; the worktree snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, dispatch, puzzle-design, play-store, kotlin-android, compose, stacks, testing; plus `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: the release-manager's check-gates docs follow-up (`77dcf88`, this worktree)
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none
Loops: 0
Read in full: `.planning/reports/phase-14/tech-lead-r2-gate-brief.md`, `tech-lead-signoff-gate-scope.md` (A0–A2), `tech-lead-design-sync.md` (A3), `android-game-dev-check-gates.md`, `code-auditor-check-gates.md`, `release-manager-check-gates-docs.md`; the diff `develop...77dcf88` outside `.planning/` (code, tests, build files, `scripts/check.sh`, `ci.yml`, the catalog, `THIRD_PARTY.md`, `docs/release-checklist.md`); `docs/tech-design.md` §7.3, §8, §9, §10, §13, §14–§17
Lessons applied: none (0 active lessons in the snapshot; `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
- PASS. Nine code commits do what the brief, A0–A3 and design §7.3, §8.4, §9.3, §9.6–§9.8 rule; the code-auditor passed them on fable with its own negative controls; nothing on the branch needs a code fix before the merge. It merges after `bugfix/hint-undecided` (§14.1 step 3a); the two share no file and the three-way trial merge is clean.
- The Core-Value gate is real: all 10,000 Challenge levels replay their certificate to one peg, deterministically, in 5.955 s, so the default tier is 10,000 and a local Check, CI and the release are all exhaustive. kover 0.9.11 measures `:domain` at 99.0 % line coverage at bound 90.
- Ruling 1: C7 needs no device session; the qa-engineer PASS that rules/gitflow.md asks for is a review-mode session, as for R-1.
- Auditor findings ruled: 3 is a known limit for 3.0.0 with the fix shape recorded; 4 is a placement fix on its own branch after R1 and R-2 merge, not a branch-cut blocker; 5 is backlog at the AGP ladder step; 6 and 7 are text, with each file's next edit. Findings 1 and 2 are closed by `77dcf88`.
- `docs/tech-design.md` now carries the figures (§7.3, §8.4), the as-built state (§9.1, §9.6–§9.8), a new §8.6, and the rows in §14, §15, §16 and §17.

## Sign-off — PASS

**Against the brief and the addenda, per commit.**
- C1 (`348279d`): the ruled block verbatim in `standards.gradle.kts`; red-first on a planted 0-byte store for `:domain:test` and `:app:testDebugUnitTest`; the sweep's store, planted immediately before the final Check, healed inside it — twice for the developer, once for the auditor. §9.6 F1.
- C2 (`139b418`): `scripts/check.sh` (110 lines, bash 3.2, atomic `mkdir` lock with the PID inside, stale reclaim, `--wait`, exit 75, Gradle a plain child, never `exec`); `CLAUDE.md` line 6 and a byte-identical `AGENTS.md`. The four departures from the brief's letter (traps that exit, `cd "$root"`, the one-second PID grace, the per-poll re-check under `--wait`) are improvements with a reason each; I accept them and §9.6 records them. The `kill -9` window is the brief's own reclaim rule: ruling 3.
- C3 (`f46b888`) and C3a (`18e7885`): the five-name known set, F-1's KDoc, the whole-source-set roots and the description derived from the list; 55 build-logic tests; the auditor's probes in a debug manifest, `assets/`, Java `resources/` and `src/release` each fail their gate, while test sets and comment lines pass. §9.3.
- C3b (`8687fca`): push and pull_request on `[develop, 'release/**', main]`; `FUZZ_SEEDS: "10000"` on the Check step (C5); no remote, no run claimed. §9.7.
- C4 (`2520fa7`): `generateLadder` lifts the loop unchanged into a `sequence`; `generate(n) = generateLadder(n).last()`; `git diff develop...HEAD -- domain/src/main` is that one file; every golden test unchanged and green. §7.3 item 2.
- C5 (`e5f4e9b`): `FuzzSeeds.parse` strict (`abc`, `0`, `' 10'`, `-7`, `1e3`, `null`, `''` fail naming the value); `FuzzHarnessTest` asserts §7.3 item 4 per level and over the run, writes `domain/build/fuzz-failures.txt` before failing; `:domain:fuzz` in the sweep's shape with the count as an input property. The `level % 97` certificate mutation was caught by the harness alone (`:domain:test` stayed green) and the vacuity mutation by the examined count. §7 measurement: rule 1, 5.955 s. §7.3 items 1, 3–8.
- C6 (`42d4812`): kover 0.9.11 on `:domain` only, `depthSweep` and `fuzz` out of the instrumentation, `minBound(90)` at a measured 99.0 %; a bound of 100 fails naming the rule; no `:app:kover*`; one online warm run, then `--offline`. §8.4.
- C7 (`f15e58a`): `allWarningsAsErrors` on every `:app` `KotlinCompile` but the `…UnitTestKotlin` tasks, and on all of `:domain`; the opt-in on the exempt tasks only; six imports, four `@param:` targets, the catalog entry on the `hiltNavigationCompose` ref; 0 main or `:domain` `w:` lines, 30 residue in the unit tests; the release classpath set identical (361 = 361). §9.8 rule 1, A3.

**Boundaries, dependencies, standards.**
- `:domain` stays pure: `grep -rl 'import android' domain/src/main` is empty; C4 uses the stdlib `sequence`; the harness's `java.io.File` is test source, as the sweep's report already is.
- Two coordinates changed, both Apache-2.0, non-copyleft, autonomous: kover (`THIRD_PARTY.md` §0 row, `ec49df3`, the licence read from four POMs and the repository's `LICENSE.TXT`) and `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.3.0 (§4 row, `77dcf88`, the POM and the AAR's bundled `LICENSE.txt`; already on the release runtime classpath transitively). Both catalog comments carry the reason and the date.
- Suppressions: my own grep over the diff returns 0, agreeing with the auditor's and the developer's.
- Commits: conventional subjects ≤ 72 characters, one `Co-Authored-By: Claude Fable 5.1` trailer each, no `Claude-Session:` line.
- Standing rule 1: written on opus (the developer at max), reviewed on fable (the code-auditor at xhigh), signed off on fable. No Q-019 deviation on this branch's review chain.
- Files: 34 in `git diff --stat develop...HEAD`; outside `.planning/` exactly the brief §9 list, A3's files, and the release-manager's two.

**What I accept without a repetition, and why.** I re-ran no Gradle task: the dispatch forbids it, and rules/studio.md's "intentionally required" list gives the tech-lead the sign-off, not a re-run. Every number I transcribe has two independent sources, the developer's run and the auditor's reproduction (the Check with the wedge, the 10,000-level timing re-timed, the kover counter re-measured, every negative control re-planted). Where only one source exists — the `check` wall clocks, on a loaded host — §7.3 says so and treats the delta as noise.

## Ruling 1 — the qa-engineer gate: C7 needs no device session

**Decision.** No. The qa-engineer PASS that rules/gitflow.md requires before a merge into develop is met by a review-mode session, as it was for R-1 (`qa-engineer-gate-scope.md`: its own Check run, its own probes, an install-and-launch of the debug build on the emulator). The gate itself is the rule's and not mine to waive; its device half is not needed by anything on this branch.

**Why.**
1. The six imports change the symbol, not the code. Both `hiltViewModel`s are `public inline fun <reified VM : ViewModel>` — I read the two 1.3.0 sources jars in the Gradle cache (`hilt-navigation-compose`'s `HiltViewModel.kt:50-56`, `hilt-lifecycle-viewmodel-compose`'s `:37`); the deprecated one's whole body is `composeHiltViewModel(viewModelStoreOwner, key)`, the new one. An inline call to an inline forwarder compiles to the inner body, so the six call sites compile to the same instructions under either import. The equivalence is a compiler fact, not a test result: no device can observe a difference because there is none.
2. The four `@param:` targets write down the target the compiler already used — the warning's own text is "currently applied to the value parameter only". Hilt's KSP-generated factories see the same qualifier on the same parameter; the Check's `:app:testDebugUnitTest` (1,067 tests over the compiled Hilt graph) is the proof the graph still resolves.
3. The shipped set is unchanged: the `releaseRuntimeClasspath` sorted-unique coordinates are identical before and after (361 = 361, the auditor and the developer), the artifact having been on it transitively at the same version.
4. The flag changes what the compiler accepts, not what it emits; C1–C6 are build, test and script files with no runtime code at all.
5. There is no device acceptance path for a build gate and C7 adds none. QA's hands-on session covers "every executed plan's acceptance path" (rules/studio.md); this branch has no plan and nothing to accept on a screen.

**So:** dispatch the qa-engineer in review mode, in parallel with the wait for `bugfix/hint-undecided`; it costs one Check (≈ 12 min) and its probes. If the producer merges on the auditor's PASS and this sign-off alone, that is a departure from rules/gitflow.md's merge line which the producer records, not one I rule.

## Ruling 2 — merge order and the merged tree

`feature/phase-14-check-gates` merges after `bugfix/hint-undecided` (§14.1 step 3a, then 4). Facts: `git merge-tree --write-tree develop HEAD` is clean (tree `57e6684`); hint and check-gates touch zero files in common; a three-way (develop + hint, then check-gates) is clean (tree `0aae1e0`). The duty of the second to merge (§14.1's closing paragraph): take `develop` in after hint merges and run the Check on the merged tree before the `--no-ff` merge. C7's rule 3 then meets R1's code for the first time: a main-compilation warning in hint's `:domain` or `:app` files fails that Check, and the fix is the same two patterns (the import, the `@param:` target), made on check-gates by the android-game-dev with the code-auditor reading the hunk; any other kind of warning stops and comes to me. kover's bound and the fuzz are re-proven on the same run. No new tech-lead round unless that Check is red for a reason outside those two patterns.

## Ruling 3 — auditor finding 3: `kill -9` leaves the Gradle client while the lock is reclaimed

**Decision.** A known limit for 3.0.0, recorded in §9.6 and §16 #24, with the fix shape ruled and scheduled post-3.0.0 (§14.2). Not fixed now.

**Why.** It is my design's own stale-reclaim rule (brief §3 step 3), not developer drift. The damage is bounded by C1: a false red and a retry, never a wedge, and no artifact can be wrong. The sender is a SIGKILL of the script (a harness timeout is the realistic one); TERM and Ctrl-C are unaffected, and under TERM the lock is held until Gradle returns, which is the right behaviour. Fixing it now means `"$root/gradlew" … &` plus `wait`, the TERM trap forwarding to the child, and a two-PID reclaim — a change to the script's shape and a review loop on a finished branch in release week, for a window whose cost is one twelve-minute re-run. The release Check (3.1) is the release-manager's, on a quiet host, with `--rerun-tasks`; a SIGKILL there would itself be the reason to re-run.

**Meanwhile, the operator rule (§9.6):** run a Check in the background or in a driver script with a timeout above its wall clock (the harness's foreground cap is 10 minutes, the Check ≈ 12); after a SIGKILLed Check, `pgrep -fl GradleWrapperMain` and wait for the orphan before the next Check in that worktree. Finding 6 (the INT comment) rides the same future edit.

## Ruling 4 — auditor finding 4: kover and the wall-clock deadline tests

**Decision.** The auditor's recommendation, with a schedule. The four deadline tests (`SolverTest`'s three `resolvesThe…FullStartInsideTheProductionDeadline` and `SolverBudgetTest.everyTractableSolvablePositionReturnsAMoveWithinBudget`) move to their own class `SolverDeadlineTest`, run by their own `Test` task `:domain:solverDeadline` in the sweep's shape — in `check`, `outputs.upToDateWhen { false }`, outside kover's instrumentation. `:domain:test` stays the per-commit gate; `koverVerify` is re-measured after the move and the figure reported (≥ 90 stands; 99.0 % leaves room). The design's new §8.6 records them as the exception to "no wall clock": a product budget measured on the host.

**Not on this branch**, because `bugfix/hint-undecided` rewrites both files to `Solver.decide` and merges first; a move here collides with a reviewed branch. Its own small branch, `bugfix/solver-deadline-task`, off `develop` after R1 and R-2 merge: android-game-dev → code-auditor → tech-lead; no device session (test placement, no product code).

**Before `release/3.0.0`?** It is **not a branch-cut blocker** (§14.1 row DT carries no marker). The failure is a false red on a loaded host, fail-closed; the release Check runs on a quiet host by the release-manager's rule and §17's threshold (1-minute load ≤ 5.0), where the margins are 4–9× even under the agent. It lands before the cut if it is green in one loop (R-8's pattern) — the producer's schedule — else in the first post-3.0.0 batch, with the quiet-host rule governing every Check until then. Rejected: loosening the deadline (measures nothing), deleting the tests (drops DEBT-01's guard), "accept as is" (a gate that goes red by host load alone trains re-runs on red).

## Ruling 5 — auditor finding 5: the deprecated `assets.srcDir` at `app/build.gradle.kts:446`

§9.8 reaches module compilations; a Gradle build-script diagnostic is the Kotlin DSL script compiler's and outside it, which §9.8 now says. The call is on `develop` since `f391ac6` (2026-08-25), outside this diff, and a break only on the AGP that removes the method, which no pin reaches before a ladder step (AGP 9.1.0 is held, §1.2). Backlog (§14.2, §16 #25): replaced by the form that ladder step's release notes name, with that step. Not before 3.0.0.

## Ruling 6 — findings 6 and 7: the two comments

Text only; neither leads a reader to an action that costs more than a re-read. Finding 6 (`check.sh:21-22` and `:67-71`, the INT path) is corrected with the next edit of the script, which is ruling 3's fix. Finding 7 (`ci.yml:58`) with the next edit of the workflow (the sweep's job split of §9.7, or whichever comes first). Both are in §14.2 so they are not lost. Not before the merge.

## Findings 1 and 2 — routed, and already closed

Routing to the release-manager confirmed; both landed on this branch in `77dcf88` while I read: the `THIRD_PARTY.md` §4 row for `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.3.0 (Apache-2.0; the POM fetched 2026-10-24 and the AAR's bundled `LICENSE.txt`), and `--rerun-tasks` in checklist 1.2 and 3.1 with the cache-hit reason. I transcribed the consequence into §7.3 (a cache hit is a pass of the same code and count; the evidence lines live in the restored XML's `system-out`; the release run passes `--rerun-tasks`) and §14.1 R-7. Nothing further is owed.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/docs/tech-design.md` — edited (+171/−30, uncommitted; the producer commits it with this report): the header; §7.3 measured figures; §8.1 rows; §8.4 floor and bound; **§8.6 new** (the deadline tests' placement); §9.1; §9.6 as built and the known limit; §9.7; §9.8 as built and its scope; §10 wall-clock row; §13 rows; §14.1 R-2, R-6, R-7, new row DT, merge-order step 4; §14.2; §15 five rows; §16 #2, #8, #9, new #23–#25; §17 seven rows.
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/.planning/reports/phase-14/tech-lead-signoff-check-gates.md` — this sign-off.
- No source, test, build, script, workflow, catalog or planning file was created or edited; no commit, no merge, no Gradle.

## Checks
- `git log --first-parent develop..HEAD`; `git merge-base develop HEAD` — pass (`c5f2c54`; nine code commits in the ruled order C1, C2, C3, C3a, C3b, C4, C5, C6, C7, then the report and docs commits; head `77dcf88`)
- `git diff --stat develop...HEAD` — pass (34 files, +1249/−108 at `9b04cf4`, plus `77dcf88`'s +36/−5; outside `.planning/` only the brief §9 list, A3's files, `THIRD_PARTY.md` and `docs/release-checklist.md`)
- The diff outside `.planning/` and `AGENTS.md` read in full (929 + 351 lines) — pass (matches the brief, A0–A3 and §7.3, §8.4, §9.3, §9.6–§9.8 as written; the eight developer deviations are the ones the auditor accepted)
- `git diff develop...HEAD -- . ':(exclude).planning' | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|baseline|ignoreFailures|!!|-Xwarning-level|nowarn|suppressWarnings)'` — pass (0)
- `git log --format='%s' develop..HEAD | awk 'length>72'` — pass (none); trailers: 15 `Co-Authored-By: Claude Fable 5.1`, 0 `Claude-Session:` — pass
- `grep -rl 'import android' domain/src/main` — pass (none; `:domain` stays pure)
- `cmp CLAUDE.md AGENTS.md`; `grep -n '^Check command' CLAUDE.md AGENTS.md` — pass (identical; both `./scripts/check.sh`)
- `git merge-tree --write-tree --name-only develop HEAD` — pass (clean, tree `57e6684`); `comm -12` of the two branches' changed-file lists — pass (empty: hint and check-gates share no file); the three-way, `develop` + hint then check-gates — pass (clean, tree `0aae1e0`)
- `unzip -p` of `hilt-navigation-compose-1.3.0-sources.jar` and `hilt-lifecycle-viewmodel-compose-1.3.0-sources.jar` from `~/.gradle/caches` — pass (both `hiltViewModel`s `inline reified`; the deprecated body is `composeHiltViewModel(viewModelStoreOwner, key)`); the basis of ruling 1
- `git diff develop...bugfix/hint-undecided -- …/SolverTest.kt …/SolverBudgetTest.kt` — pass (both files rewritten to `Solver.decide`; the basis of ruling 4's "not on this branch")
- `sed -n 436,452p app/build.gradle.kts` — pass (the `assets.srcDir(roomSchemaDir)` call, with its Robolectric reason, is unchanged by this branch)
- `grep -n caching gradle.properties` — `org.gradle.caching=true` (finding 2's premise holds)
- `git status --short` before my edits — pass (clean at `77dcf88`); after — only `docs/tech-design.md`
- A Python cell-count check over `docs/tech-design.md`, escaped pipes ignored — pass (20 tables, 0 rows whose cell count differs from the header; the raw count flags only §10's cold-start row, whose `\|` escapes predate this edit)
- `grep -n 'pending R-2' docs/tech-design.md` — pass (none left)
- **Not run:** any Gradle task, by the dispatch and by rules/studio.md (the sign-off re-runs no gate). Every figure above has the developer's run and the auditor's reproduction behind it.

## Open issues / risks
- **The merged-tree Check is the last gate before the merge** (ruling 2): C7's warnings flag meets R1's code for the first time there. Expected green; the two patterns are the known fix.
- **Row DT is the producer's call** (ruling 4): before the cut if one loop, else post-3.0.0. Until it lands, any Check on a host above load 5.0 can go red on the deadline tests for no reason in the diff; re-run, never widen.
- **The `kill -9` window stays open through 3.0.0** (ruling 3); §9.6's operator rule is the mitigation, and it binds the producer's dispatches as much as the developer's runs.
- **CI has still never run** (no remote). C3b's triggers and C5's env are read from the YAML, not from a run.
- **The host:** the §7 timings are quiet-host figures and the wall clocks are not; §7.3 says which is which.

## Handoff
Next: **producer**
1. Dispatch the qa-engineer in review mode (ruling 1): its own `./scripts/check.sh --offline --rerun-tasks` (in the background or a driver script, §9.6's operator rule), its own probes, an emulator install-and-launch at most; no device session. In parallel with the wait for `bugfix/hint-undecided`.
2. After `bugfix/hint-undecided` merges: `git merge develop` into check-gates (never a rebase), run the Check on the merged tree; green → `git merge --no-ff` into `develop` with this report and the design edit. A main-compilation warning in R1's code → the android-game-dev fixes it on check-gates with the two patterns and the code-auditor reads the hunk, no new tech-lead round; anything else → me.
3. Schedule `bugfix/solver-deadline-task` (row DT, §8.6) after both merges, cut from `develop`: android-game-dev → code-auditor → tech-lead. Your call whether it goes before the cut.
4. Carry §14.2's four new items into the post-3.0.0 batch; none blocks the release.
5. The release-manager: nothing more is owed on this branch (findings 1 and 2 are in `77dcf88`); R-7 runs with `--rerun-tasks` on a quiet host.

VERDICT: PASS
