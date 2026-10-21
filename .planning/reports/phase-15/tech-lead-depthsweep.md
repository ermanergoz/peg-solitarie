# tech-lead report — phase-15 B1: the `:domain:depthSweep` gate failure

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (e757b3b..this commit — reports only, no source, no build file)
Skills loaded: ergoz-soft:standards (role: tech-lead), android-gradle-logic, android-architecture
Ran in parallel with: phase-14 (worktrees `-r1`, `-r5`, primary); a qa-engineer device session in this worktree
Ran on: Opus 5 1M (`claude-opus-5[1m]`) at the harness default effort — `agents/tech-lead.md` pins
`model: fable`; Fable's allowance is exhausted, so this ran on Opus per the founder's decision
Q-003. This is a diagnosis of a build gate, not a review of code I wrote, so it carries no
independence concern (rules/studio.md, rule 1 is untouched).
Fan-out: none (one `general-purpose` helper for upstream Gradle research; no studio role dispatched)
Loops: 0
Lessons applied: none

---

## Summary

The sweep is right and Gradle cannot report it. `:domain:depthSweep` is red because **two
concurrent Gradle builds ran in this one worktree** and clobbered Gradle's binary test-result
store — proven from the daemon logs and reproduced deterministically in both of its two failure
modes. Not memory, not a timeout, not a killed worker, not the diff. **The Check command passes
on this exact source** (two independent greens), so B1 does not block the merge — but the gate can
be left permanently red by any interrupted build, which is a real defect, latent since phase 12,
and I am scheduling its fix before the phase-15 merge.

## RULING

**1. B1 does not block the phase-15 merge.** rules/gitflow.md's "Check command green" precondition
is satisfied on this source, twice, uncontended:

| Green | Evidence |
|---|---|
| `./gradlew check --offline` → **BUILD SUCCESSFUL in 11m 8s**, 98 actionable tasks | qa-engineer's run; `.planning/qa/phase-15/check-offline-run2.log`; daemon-55009 build `bdb6c07e` 16:54:57→17:06:05 in `-p15` |
| `:domain:depthSweep` → **exit 0 in 11m07.57s**, `tests="1" … failures="0"` | mine, uncontended, throwaway worktree at `d06a287` (E1 below) |

The tree those ran on is byte-identical to current HEAD: `git diff --stat 370e733 e757b3b` and
`git diff --stat 57b2ad5 e757b3b` over `app/src domain/src */build.gradle.kts build-logic config
gradle gradle.properties settings.gradle.kts build.gradle.kts` are both **empty**. Phase 15 touches
no build file and no `:domain` test. B1 is not a property of this diff and charging the merge for
it would be wrong.

**2. B1 is fixed as its own task, and that task merges before phase 15.** This is not "out of
scope". A gate that any interrupted build can leave permanently red, failing in one second with a
Kryo stack trace and an undocumented recovery, is a broken gate, and every future phase leans on
it. The fix costs **nothing on the critical path**: the phase-15 merge is already blocked by the
qa-engineer's device findings (F-1 Skip under the status bar, 7c the jump cue with animations off,
criterion 15), so the window exists now.

- Branch **`bugfix/check-gate-concurrency`** off `develop` — not off this feature branch. The
  defect is in `build-logic/` and `domain/build.gradle.kts`, which `develop` and every future
  phase share; fixing it on a UX branch would hide it from `develop` until this phase lands.
- Merged to `develop`, then `develop` merged into `feature/phase-15-ux-refresh`, so phase 15's
  closing Check command runs on a gate that cannot wedge.
- Implemented by **android-game-dev** (build configuration is the stack developer's work;
  rules/studio.md standing rule 1 — I ruled, I wrote no code). Reviewed by **code-auditor**.

**3. I am not weakening the gate.** The sweep stays in `check` and stays `upToDateWhen { false }`.
rules/puzzle-design.md invariant 8 and rules/testing.md make it release-blocking, and a slow gate
beats a gate that can be doubted. Both fixes below remove the fragility without touching the
assertion, the coverage, or the always-run semantics.

## Outputs

- `.planning/reports/phase-15/tech-lead-depthsweep.md` — this report
- `.planning/reports/phase-15/tech-lead-coldstart-gate.md` — the criterion 15e ruling (separate
  subject, separate owner; written in the same sitting at the coordinator's request)

---

## What is actually failing — established by execution, not inference

The distinction the task asked me to settle: **the sweep is right; Gradle cannot report that it
was right.** The evidence, not the reasoning:

- Every red run printed the **complete, correct five-board report** with the same numbers as the
  green runs, as `develop`'s control, and as `DepthSweepTest`'s own KDoc table.
- The green run's XML records `tests="1" skipped="0" failures="0" errors="0"`, `time="666.894"`.
- Both deaths occur inside `SerializableTestResultStore` — a class that only serializes results —
  and I reproduced both **without touching the test** (E2, E3d).
- No assertion ever failed, on any run, on either branch.

### Two failure modes, one cause

**M1 — the primary failure (11 minutes, then red).** A second concurrent build's
`fs.delete(binaryResultsDirectory)` unlinks the first build's `in-progress-results-generic.bin`
while the first build is still writing. The first build runs every test, prints its report, then
dies at finalization, because `SerializableTestResultStore.Writer.close()` publishes results by
`Files.move(in-progress-results-generic.bin → results-generic.bin)` and the source path is gone:

```
java.nio.file.NoSuchFileException: …/domain/build/test-results/depthSweep/binary/in-progress-results-generic.bin
BUILD FAILED in 11m 8s
```

**M2 — the wedge (one second, then red, forever).** M1 (or any killed build) leaves a truncated
`results-generic.bin`. `AbstractTestTask.executeTests()` reads the *previous* store **before**
deleting it — `Test.getPreviousFailedTestClasses()` → `SerializableTestResultStore.hasResults()`,
to order previously-failed tests first — and `hasResults()` checks *existence, never validity*:

```java
public boolean hasResults() {
    if (Files.exists(serializedResultsFile) && Files.exists(outputEventsFile)) {
        try (KryoBackedDecoder decoder = openAndInitializeDecoder()) { … }   // readSmallInt() on 0 bytes
```

so a 0-byte file passes `Files.exists` and then hits end-of-stream. Every later invocation fails
identically, in about a second, before a single test runs, until someone deletes a git-ignored
directory. Gradle 9.5.1 source; the qa-engineer's `--stacktrace` names the same two frames
(`hasResults(...:310)` ← `Test.getPreviousFailedTestClasses(Test.java:721)`), independently of me.

### Intermittent, or deterministic? Both, and the split is the point

- **M2 is fully deterministic.** Given a truncated store beside an existing `output-events.bin`,
  the task fails 100% of the time in ~1 s. I reproduced it on demand on two different tasks
  (E2: `depthSweep`, 1 s; E3b-1: `test`, 4 s).
- **M1 is a race, and its window is the task's write duration.** Concurrency alone is *not*
  sufficient — E3c ran two builds on two daemons with 10.7 s of genuine overlap and **both
  passed**. The second build's delete has to land inside the first build's write window. Which is
  why this manifests on `depthSweep` and nowhere else: it writes for **11 minutes**, and
  `outputs.upToDateWhen { false }` forces it to re-run on every `check`, so *any* second build
  started during those 11 minutes lands in the window. On a 31-second task the same race is rare.

### The concurrency was real, and it is in the record

From the Gradle daemon logs, both builds in `/Users/ermanergoz/Desktop/PegSolitarie-p15`:

| Daemon | Build id | Window | Outcome |
|---|---|---|---|
| 42146 | `90946528` | 16:33:08.653 → 16:44:17.074 (11m08s) | — |
| 33593 | `52c930b4` | 16:36:23.866 → **16:47:31.506** (11m08s) | `NoSuchFileException` (`depthsweep-retry2.log`) |

**7 minutes 53 seconds of overlap**, two daemons, one worktree, one
`domain/build/test-results/depthSweep/binary/`. The earlier 1-second failures (`check-offline.log`
16:33:08, `depthsweep-retry1.log` 16:35:06) both fall inside build `90946528`'s window. The
qa-engineer saw the other side of it from the device session: *"a `depthSweep` JVM belonging to
another session held ~100 % of a core throughout (`-DRUN_SWEEP=true`, worker tmpdir under this
session's scratchpad)"*. Two roles, one worktree, two builds.

### What it is not — each ruled out, not assumed

| Hypothesis | Verdict | Evidence |
|---|---|---|
| The sweep is wrong / an assertion failed | **No** | Complete correct report on every red run; `failures="0"` on green; identical numbers to `develop` |
| Memory ceiling / OOM | **No** | Zero `OutOfMemoryError` in all four recent daemon logs; host 77% memory free; no `hs_err_pid` dump |
| The test worker was killed | **No** | Zero `finished with non-zero exit value` and zero `Could not receive message from` in any daemon log. A killed worker is reported as such; this is a *file-missing* error after a clean test run. (Phase 12 recorded "the test-worker JVM dies" — that inference was wrong.) |
| Timeout | **No** | No timeout configured on the task; the failures are at 1 s and at 11m08s, not at a bound |
| A large binary result set | **No** | The store is **366 bytes** + **780 bytes**. Size is irrelevant to this bug |
| Gradle "stale output" handling | **No** | Gradle's own `fs.delete(binaryResultsDirectory)` handles a stale dir — but it runs *after* the read that throws |
| The phase-15 diff | **No** | Source diff from `57b2ad5` empty; no build file, no `:domain` test touched |

Note for the record: the daemon that was alive through the failures exited at 16:47:37 with *"The
daemon has exited normally or was terminated in response to a user interrupt"* — a `--stop`, not a
crash. And `-p15` is **not currently wedged**: its store is valid (366 B / 780 B, 17:06).

---

## Is this new? No — latent since phase 12, and nobody worked around it

- `depthSweep` was introduced in phase 8, `ed95368 test(08-10)`, with `upToDateWhen { false }`
  from the start. The enabling conditions have existed since then.
- **Phase 12, plan 12-03 hit M2 and deferred it.** `12-03-SUMMARY.md:285-294` and
  `.planning/phases/12-leaderboards-achievements/deferred-items.md:9-24` record
  `:domain:depthSweep` failing with `java.io.EOFException` after ~550 ms, correctly attributed to
  *"environmental — a dead/contended Gradle test-worker JVM, plausibly from several parallel
  worktree agents running Gradle against the same daemon set concurrently"*. The family was right;
  the mechanism ("the worker JVM dies") was wrong, which is why the fix was never obvious.
- **It was filed honestly and then never scheduled.** It sits in `deferred-items.md` and appears
  nowhere in `ROADMAP.md` or `STATE.md`. Phase 15 is the second occurrence and the first
  diagnosis.
- **No silent workaround in 8–14.** I searched every phase and report for `-x depthSweep`,
  `--exclude-task`, and habitual `clean`: there is no exclusion anywhere, and every `gradlew
  clean` reference belongs to phase 6's own build-hardening work or phase 1/5 scaffolding. The
  only `rm -rf …/test-results` in the repo's history is in the two phase-15 reports written today.
  Nobody hid this; it was reported and lost.

This raises its priority: the failure has survived a deferral, and it cost this phase an auditor
FAIL and two roles' investigation time.

---

## The fix — specified to implement, two parts, both build configuration

Answering the options the coordinator put to me:

- **A task output / caching declaration — no.** The task already declares its outputs and already
  opts out of up-to-date checks; caching is off for it by construction. No output declaration
  stops another *process* deleting the directory mid-run, and none makes `hasResults()` tolerate a
  truncated file.
- **`--rerun-tasks` — no, and I have direct evidence.** E3b-1 used
  `:domain:test --offline --rerun-tasks` against a wedged store and **still failed in 4 s**. The
  read happens inside the task action regardless of why the task is running.
- **A cleanup step in the check path — half right, wrong place.** It must be in the task, not in a
  wrapper: a wrapper protects only the one command an agent remembers to use, and leaves
  `:domain:test`, `:app:testDebugUnitTest` and every ad-hoc task invocation exposed.
- **Splitting the sweep — no.** It is one test method; splitting it shortens the write window
  without removing the race, and buys nothing the two fixes below do not.

### F1 — self-heal the result store (fixes M2 completely). Required, one line.

In **`build-logic/src/main/kotlin/standards.gradle.kts`** — not in `domain/build.gradle.kts`. The
`standards` plugin is applied by **both** `:app` and `:domain` (`app/build.gradle.kts:8`,
`domain/build.gradle.kts:4`), so one edit covers `:domain:test`, `:domain:depthSweep`,
`:app:testDebugUnitTest` and `:app:testReleaseUnitTest`. Per-module copies are the
copy-paste the convention plugin exists to prevent (`android-gradle-logic`).

```kotlin
tasks.withType<Test>().configureEach {
    // A store truncated by an interrupted or clobbered run makes Gradle's own
    // SerializableTestResultStore.hasResults() throw EOFException before any test runs, and it
    // does so on every later invocation until the directory is deleted by hand.
    // AbstractTestTask.executeTests() reads the previous store — for previously-failed-first
    // ordering — BEFORE it deletes the binary directory, so the task cannot recover from a file
    // it is about to discard. doFirst runs before the task action, which is early enough.
    val binaryResults = binaryResultsDirectory
    doFirst { binaryResults.get().asFile.deleteRecursively() }
}
```

- **Verified, not assumed**: E4. With this in place, the exact wedge state that failed in 4 s
  (E3b-1) gives **BUILD SUCCESSFUL in 11s**.
- Cost: these tasks forfeit previously-failed-test-first ordering. Immaterial — `depthSweep` has
  one test method, and `:domain:test` is a 31-second full suite.
- Configuration-cache safe: it captures only the task's own `DirectoryProperty` and uses
  `kotlin.io`, with no `Project` reference at execution time. `gradle.properties` has CC off
  today; this does not add a new reason to keep it off.
- **Acceptance test the developer must run** (this is the gate on the fix, and it is cheap): with
  a green store present, `: > domain/build/test-results/depthSweep/binary/results-generic.bin`,
  then `./gradlew :domain:depthSweep --offline` must run the sweep instead of failing in ~1 s.
  Repeat for `:domain:test` and `:app:testDebugUnitTest`.

### F2 — one build at a time per worktree (fixes M1). Required.

M1 cannot be fixed inside the task: the deletion comes from another process. Gradle offers no
guard and says so — its own issue #4720 carries a Gradle engineer's statement that concurrent
builds in one workspace are *supported* but that Gradle "doesn't have enough guardrails for the
user's build to prevent task output clobbering". So the guard is ours.

**This is not a new rule.** rules/gitflow.md already says *"One worktree per concurrently working
agent"*. It was violated — a code-auditor and a qa-engineer were both pointed at `-p15`, and I was
sent there too. The rule did not hold, so the backstop must be mechanical:

- Add **`scripts/check.sh`** (the repo has no `scripts/` dir yet) that takes an **exclusive
  per-worktree lock** before invoking `./gradlew check "$@"`, and on contention fails immediately
  with the holding PID, the worktree path, and the one-line recovery. Use an atomic `mkdir` lock
  on `.gradle/ergoz-build.lock` with a `trap` release and the PID written inside: `flock(1)` does
  **not** exist on macOS, and `lockf`/`shlock` are BSD-only so they would break Linux CI. `mkdir`
  is atomic on both. Support `--wait` for CI.
- Then set `CLAUDE.md`'s `Check command:` to `./scripts/check.sh`.
- The generalising argument, which is why I want the lock rather than a narrower fix: two
  concurrent builds in one worktree also race on `:app` merged resources, KSP output, lint models
  and the build cache. The test-result store is only where it surfaced first and loudest.

Considered and rejected: a per-invocation unique `binaryResultsDirectory` (the Gradle forum's own
workaround). It makes an `@OutputDirectory` non-deterministic, litters `build/`, breaks the stable
report path, and protects only this one output among several that race.

### F3 — escalations to the producer (mine to raise, not to do)

1. **Two build-running roles were dispatched into one worktree**, against rules/gitflow.md. Worth
   the producer's attention as a dispatch rule, independent of F2's backstop.
2. **`deferred-items.md` has no route into the roadmap.** Phase 12 filed this correctly and it was
   lost for three phases. That is a process gap, not an individual's mistake.
3. **The template should ship the lock.** `templates/android-compose-game`'s check command and the
   `Check command:` contract in rules/stacks.md are plugin-side; a template change is a plugin
   branch (`feature/template-android-compose-game`), which the producer schedules. I am specifying
   the product-repo half only.
4. **No upstream Gradle issue exists for M2** (searched; #33990 is adjacent and closed as not
   planned, #36751 and #36601 are different store bugs, both fixed). `hasResults()` validating
   existence but not validity is still unfixed on `master`. Filing it is cheap and helps every
   Gradle user; low priority, nobody's phase.

---

## Checks

Every command I ran, in sequence, with its outcome. The intermittency pattern is the evidence, so
this is the full record rather than a summary. All reproductions ran in a **throwaway detached
worktree** at `d06a287` under the session scratchpad (`ds-diag`), deliberately *not* in `-p15`, to
avoid becoming the second concurrent build myself while the qa-engineer was working there. Prefix
for every Gradle command: `ANDROID_HOME=$HOME/Library/Android/sdk`.

**Setup**

- `git worktree add --detach $SCRATCH/ds-diag d06a287` — pass (detached at `d06a287`)
- `./gradlew :domain:testClasses --offline` — pass (BUILD SUCCESSFUL in 1s, 6 executed / 8 from cache)

**E1 — planted `results-generic.bin` alone, then the real task. Doubles as the uncontended control.**

- `mkdir -p domain/build/test-results/depthSweep/binary && : > …/results-generic.bin` — pass
- `./gradlew :domain:depthSweep --offline` — **pass** (exit 0, **11m07.57s**; XML
  `tests="1" skipped="0" failures="0" errors="0"`; store rebuilt to 366 B + 780 B). **Negative
  result, and it is informative**: a 0-byte `results-generic.bin` *alone* does not wedge, because
  `hasResults()` requires `output-events.bin` to exist too. Host during this run: load averages
  2.60 / 5.17 / 7.26, 77% memory free, no concurrent build in this worktree.

**E2 — the wedge, on the real task**

- `: > domain/build/test-results/depthSweep/binary/results-generic.bin` (truncate; `output-events.bin`
  left intact at 780 B) — pass
- `./gradlew :domain:depthSweep --offline` — **fail** (`java.io.EOFException`, **BUILD FAILED in
  1s**, no test ran). M2 reproduced deterministically, identical to `check-offline.log` and
  `depthsweep-retry1.log`.
- `rm -rf domain/build/test-results/depthSweep && ./gradlew :domain:depthSweep --offline --dry-run`
  — pass (configures clean; the recovery is the directory deletion)

**E3 — the same code path on the fast `:domain:test` (31 s), so the race is testable in minutes**

- `./gradlew :domain:test --offline --rerun-tasks` — pass (BUILD SUCCESSFUL in 31s; store 41527 B + 0 B)
- `: > domain/build/test-results/test/binary/results-generic.bin` then
  `./gradlew :domain:test --offline --rerun-tasks` — **fail** (`java.io.EOFException`, BUILD FAILED
  in 4s). **`--rerun-tasks` does not help** — this is the evidence against that option.
- `rm -rf domain/build/test-results/test && ./gradlew :domain:test --offline --rerun-tasks` — pass
  (BUILD SUCCESSFUL in 17s). Recovery confirmed.
- **E3c**: two `./gradlew :domain:test --offline --rerun-tasks` started 6 s apart in the same
  directory — **both pass** (A 18s exit 0, B 28s exit 0). Daemon logs confirm two *different*
  daemons (62657 build `13826f14` 17:31:25→17:31:43; 2351 build `a5fcf6da` 17:31:32→17:31:59) with
  **10.7 s of genuine overlap**. Concurrency alone is not sufficient — M1 is a race.
- **E3d**: start `:domain:test`; poll until `in-progress-results-generic.bin` appears (4 s, 12283 B);
  `rm -rf domain/build/test-results/test` mid-run — exactly what a concurrent build's
  `fs.delete(binaryResultsDirectory)` does — **fail** (exit 1,
  `java.nio.file.NoSuchFileException: …/binary/in-progress-results-generic.bin`, after the tests
  ran). **M1 reproduced**, identical signature to `depthsweep-retry2.log`.

**E4 — the fix, verified before specifying it**

- Added `val binaryResults = binaryResultsDirectory; doFirst { binaryResults.get().asFile.deleteRecursively() }`
  to `tasks.withType<Test>().configureEach` in the throwaway worktree only; planted **both**
  `results-generic.bin` and `output-events.bin` at 0 bytes; `./gradlew :domain:test --offline
  --rerun-tasks` — **pass** (BUILD SUCCESSFUL in 11s, where E3b-1's identical state failed in 4s).
- `git checkout -- domain/build.gradle.kts && git status --porcelain` — pass (empty; experiment
  reverted, no build file changed anywhere)
- `git worktree remove --force $SCRATCH/ds-diag && git worktree prune` — pass (worktree list back
  to the four real ones)

**Forensics (read-only)**

- `grep -acE 'OutOfMemoryError|finished with non-zero exit value|Could not receive message from|tenured space' ~/.gradle/daemon/9.5.1/daemon-{42146,33593,55009,62657}.out.log`
  — **0 in all four**. Memory and worker-kill hypotheses dead.
- `grep -aoE '…currentDir=[^,]*…' ~/.gradle/daemon/9.5.1/daemon-*.out.log` — pass (established the
  7m53s overlap of builds `90946528` and `52c930b4`, both in `-p15`)
- `ls -la ~/Library/Logs/DiagnosticReports/` — pass (no JVM crash dump today)
- `uptime; vm_stat; memory_pressure` — pass (load 2.60/5.17/7.26 rising to 7.30 15-min; 77% memory free)
- `git diff --stat 370e733 e757b3b -- <source paths>` — pass (**empty**; the green `check` ran on today's source)
- `git diff --stat 57b2ad5 e757b3b -- <source paths>` — pass (**empty**)
- `git log -S'depthSweep' -- domain/build.gradle.kts` — pass (introduced by `ed95368 test(08-10)`)
- `grep -rnoiE '(clean check|-x *depthSweep|--exclude-task[= ]*depthSweep|rm -rf .*build/test-results)' .planning/phases .planning/milestones .planning/reports`
  — pass (no exclusion anywhere; no workaround in phases 8–14)

I wrote no product code, no test and no build file on any real branch. The only files this task
adds are the two reports.

## Open issues / risks

- **The 11-minute `check` is the thing that makes the race near-certain, and shrinking it is a
  cost/taste call that is genuinely the founder's.** The collision window *is* the sweep's write
  duration, so every minute removed shrinks it proportionally. Options: (a) leave it;
  (b) drop `upToDateWhen { false }` so an unchanged tree reuses a green; (c) close Diamond-41's
  opening-book gap, which `STATE.md:129` already tracks as the honest lever and which notes it
  "also removes `check`'s 11-minute cost". **Recommendation and decided default: (a) plus (c) —
  keep the gate always-run and in `check`, and let the Diamond-41 book close the cost.** (b) is
  rejected on technical grounds: the sweep asserts per-call latency against a 1.5 s deadline, and
  letting an unchanged tree inherit a green measured on a differently-loaded host would make the
  timing half of the gate meaningless. No founder answer is needed to proceed — F1 and F2 remove
  the fragility either way, and I have queued nothing.
- **Changing `CLAUDE.md`'s Check command to `./scripts/check.sh` (F2) touches every role's habit
  and the stack template.** Recommendation: do it, because convention already failed twice.
  Decided default if the producer or founder declines: **F1 alone**, accepting that M1 can still
  cost one build per collision — which is a large improvement on today, since with F1 a collision
  costs one retry instead of wedging the gate until a manual `rm -rf`.
- **The recovery is now written down in two places** (this report and the qa-engineer's), but
  until F1 lands the next role to hit M2 needs it: `rm -rf domain/build/test-results/depthSweep`.
  It belongs in `CLAUDE.md` or the phase's evidence file, not only in a report nobody re-reads.
- `A-5` (Board stability at the UI boundary) and `A-7` (no `koverVerify` exists although
  rules/kotlin-android.md and rules/testing.md name one) were routed to me by the auditor and are
  **not addressed here** — this report is confined to B1. Both are real and I will rule on them in
  the phase-15 sign-off, after the follow-up slice.
- This report is **not** my phase-15 sign-off.

## Handoff

Next: **producer** — schedule `bugfix/check-gate-concurrency` off `develop` now (F1 + F2,
android-game-dev, reviewed by code-auditor), merge it to `develop`, then merge `develop` into
`feature/phase-15-ux-refresh` before the phase-15 merge; B1 must not be recorded as a phase-15
blocker. Also F3's four escalations: the two-roles-one-worktree dispatch, the `deferred-items.md`
routing gap, the plugin template's check command, and the optional upstream Gradle issue.

- **android-game-dev** — F1 in `build-logic/src/main/kotlin/standards.gradle.kts` (exact code and
  acceptance test above), F2 as `scripts/check.sh` plus the `CLAUDE.md` line. Build configuration
  only; no product source. Run F1's acceptance test on all three Test tasks and record it.
- **code-auditor** — review the bugfix diff; the acceptance test above is the check that matters,
  and it is cheap to re-run independently.
- **qa-engineer** — no action on B1. Your `check-offline-run2.log` is the green this ruling rests
  on; thank you for keeping it.
