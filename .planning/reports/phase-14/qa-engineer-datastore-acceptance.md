# qa-engineer report — R-3 / F-10: DataStore determinism acceptance

**Verdict: ACCEPT.** R-3's determinism evidence is accepted. **I reproduced the original failure** on the pre-fix code — 5 hangs in 23 runs (21.7 %), each a `kotlinx.coroutines.test.UncompletedCoroutinesError: After waiting for 1m, the test body did not run to completion` at 60.03–60.05 s, and one of them is F-10's *exact* test, `theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero` at **60.036 s** against F-10's 60.041 s. **Twelve consecutive green** full-suite runs on the fix at CI's worker count (1213 / 0 failures / 0 errors / 0 skipped / 110 classes, every run), plus 25 targeted and 15 migrated-set runs — **0 hangs in 52 post-fix executions** of the formerly-flaky class. `PreferenceDataStoreFactory.create` has **zero call sites**; the two surviving hits are KDoc prose. One finding beyond the brief: **F-14 is not cosmetic** — two `AppForegroundTrackerTest` awaits would hang for 60 s, not fail an assertion, if their wall-clock predicate were never satisfied, so the residual is in the same *symptom* class as F-10 even though its cause is different.

Date: 2026-09-21
Branch: feature/phase-14-datastore-determinism (b451808) — no branch switch, no commit, no source or test edit in this worktree; `git status --porcelain` empty at start and at finish
Worktree: `/Users/ermanergoz/Desktop/PegSolitarie-r3` (isolated; `/Users/ermanergoz/Desktop/PegSolitarie` and `-sign` untouched)
Skills loaded: `ergoz-soft:standards` (role: qa-engineer) → plugin copies (v0.5.2, newer than this worktree's `.claude/rules/VERSION` 0.5.1) of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `testing.md`; repo `.claude/rules/lessons.md`. **Deviation, declared:** `android-emulator-skill` and `android-testing` were **not** invoked. The task changed **no UI and no production source** (test sources only), the environment brief forbids booting an emulator or using adb, and there is therefore no hands-on device session to run — `rules/testing.md`'s "hands-on session for any task that changed UI" has no subject here. The testing judgements below are made against `rules/testing.md` directly and are itemised in Checks. Recording the non-invocation rather than implying coverage I did not obtain.
Ran in parallel with: nothing (the two Gradle daemons on this machine were idle at 0 % CPU throughout; see "CI's worker count" below)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **The failure is real and it reproduces on demand.** 5/23 pre-fix runs hang for 60 s with the exact F-10 error and duration. It is not a hypothesis any more.
- **It is the *pattern*, not the one test.** Two different tests hang — `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` (4×) and F-10's own `theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero` (1×). Both are "second holder over the same real, file-backed store" tests. The tech-lead's ruling that the *class* is a cross-dispatcher ordering dependency is now evidence, not inference.
- **The fix holds.** 12 consecutive green full-suite runs at CI's worker count; 0 failures in 52 post-fix executions of the class against 5 in 23 pre-fix (Fisher exact two-tailed **p = 0.020**).
- **The grep closes the class.** Zero `PreferenceDataStoreFactory.create` call sites in `app/src` + `domain/src`; zero `TemporaryFolder`; zero real dispatcher, sleep, latch, timeout, unseeded random, file or network reference in any of the ten migrated classes.
- **One extension to F-14.** The auditor's list is confirmed and corrected (`ResultsViewModelTest:1608` → **`:1624`** after the develop merge), and my judgement differs on severity in one direction that matters — see F-14 below.

## Outputs
- `.planning/reports/phase-14/qa-engineer-datastore-acceptance.md` — this report.
- `.planning/qa/phase-14-r3/` — 1.1 MB, git-ignored, 82 files, **for the producer to delete after STATUS.md's Done line** (`rules/testing.md`):
  - `prefix-repro/run*.log`, `prefix-repro/w1-*.log` — 30 files, 136 KB. The pre-fix reproduction loop's Gradle console output; `run7`, `run8`, `run12`, `run16` are the four post-baseline hangs.
  - `accept-run1..12.log` — 12 files, 876 KB. The twelve acceptance full-suite runs.
  - `cadence-fixed-1..25.log` — 25 files, 100 KB. Targeted post-fix repetition of the cadence class.
  - `migrated-set-1..15.log` — 15 files, 60 KB. Post-fix repetition of all ten migrated classes.
- No artifact is committed. No screenshot exists and none is owed: no UI changed and no device was touched.

## 1. Reproduction on the pre-fix code — SUCCEEDED

**Method, non-destructive.** `git clone --shared` of the worktree into the session scratchpad, `git checkout --detach f9d6ef9` (the commit this branch was cut from; I verified `InterstitialCadenceStoreTest.kt` is **byte-identical** between `f9d6ef9` and `c59b456`, so this is the code F-10 observed). `local.properties` in the clone was written from the `sdk.dir` line **only** — no PGS id, no AdMob id, no credential was copied. No file in the clone was edited: the pre-fix suite ran exactly as written. The product worktree was never modified.

**What I tried, in order.**
1. One cold `:app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest*'` at `--max-workers=4` to compile the clone. **It failed on the first attempt** — 1 m 23 s, `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` at **60.033 s**.
2. 12 repetitions of the same command with `--rerun` (test task only; ~1–2 s when green, ~62 s when it hangs). **3 more hangs** at runs 7, 8 and 12.
3. 8 repetitions at `--max-workers=1`, to test whether worker contention is the driver. **0 hangs.**
4. 10 more at `--max-workers=4`. **1 more hang**, run 16.

Total elapsed for the reproduction phase: **~16 minutes**, inside the ~20-minute budget. Background load throughout: an Android emulator (~101 % CPU) and Android Studio (~98 %) on a 10-core machine — hostile conditions, deliberately not quieted.

**Result — 5 hangs in 23 runs at `--max-workers=4` (21.7 %), 0 in 8 at `--max-workers=1`.**

| pre-fix run | failing test | duration |
|---|---|---|
| baseline (cold) | `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` | **60.033 s** |
| 7 | `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` | **60.041 s** ← F-10's figure to the millisecond |
| 8 | `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` | **60.050 s** |
| 12 | `theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero` | **60.036 s** ← **F-10's named test** |
| 16 | `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` | **60.052 s** |

Every other test in the class completed in 0.001–0.094 s in every run.

**The excerpt** (JUnit XML of the first hang; the stack is identical in all five):

```
InterstitialCadenceStoreTest  tests=13 failures=1 time=60.181
    60.033  recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack
kotlinx.coroutines.test.UncompletedCoroutinesError: After waiting for 1m, the test body did not run to completion
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTest$2$1$2.invokeSuspend$lambda$0(TestBuilders.kt:353)
	at kotlinx.coroutines.InvokeOnCancelling.invoke(JobSupport.kt:1571)
	at kotlinx.coroutines.JobSupport.notifyCancelling(JobSupport.kt:1603)
	at kotlinx.coroutines.JobSupport.cancelImpl$kotlinx_coroutines_core(JobSupport.kt:702)
	at kotlinx.coroutines.TimeoutCoroutine.run(Timeout.kt:243)
	at kotlinx.coroutines.EventLoopImplBase$DelayedRunnableTask.run(EventLoop.common.kt:498)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:256)
	at kotlinx.coroutines.DefaultExecutor.run(DefaultExecutor.kt:105)
	at java.base/java.lang.Thread.run(Thread.java:840)
```

`TimeoutCoroutine.run` → `cancelCoroutine` is `runTest`'s **real-time** 60 s timeout cancelling a body that never completed. Exactly F-10.

### What the reproduction licenses, and what it does not

**It licenses:**
- Treating F-10 as an established defect rather than a one-off. It is not a slow test, not worker contention turning 5 ms into 50 ms, and not an observation error: the body hangs and the wall-clock timeout kills it.
- Treating the *pattern* — a second `InterstitialCadenceStore` holder `stateIn`-collecting over one real `PreferenceDataStoreFactory.create` store whose actor runs on DataStore's own `Dispatchers.IO` scope, awaited from a `runTest` body on the test scheduler — as the enabling condition, because **two structurally identical tests** hang and **eleven structurally different ones in the same class never do**. This is the tech-lead's item-C "class", now measured.
- Treating the migration as necessary for all ten classes and not just the one F-10 named: the test F-10 named is the *less* frequent of the two failures I saw.
- Reading "it passes now" as meaning something. Against a 21.7 % per-run base rate, 52 consecutive clean executions is a real signal (below).

**It does not license:**
- Claiming the auditor's *specific* mechanism (two eager `stateIn` collectors losing a wakeup across the dispatcher boundary) is proven. I did not instrument the hang or take a thread dump — I stopped at the point where the decision was already determined, as the tech-lead directed. What is proven is the class and the enabling condition; the precise lost-wakeup path remains the best available explanation, not a measurement.
- Extrapolating the 21.7 % rate to `ubuntu-latest`. This is macOS/aarch64, JDK 17 toolchain on a JDK 21 daemon, 10 cores, under ~2 cores of foreign load. The rate is machine-specific. Its *existence* is not.
- Concluding that `--max-workers=1` is safe. 0/8 at one worker is suggestive, not significant (Fisher exact vs the 4-worker arm: p ≈ 0.14). Do not use it as a workaround; it was a probe, not a remedy.
- Reading the 4:1 split between the two failing tests as meaningful. n = 5.

## 2. Twelve consecutive green full-suite runs on the fix

### CI's worker count — determined, not assumed
`.github/workflows/ci.yml` exists and passes **no** `--max-workers`; `gradle.properties` sets `org.gradle.parallel=true` and no `org.gradle.workers.max`; no `--max-workers` appears in any build script. CI therefore runs at Gradle's default, `Runtime.availableProcessors()`, which on a GitHub-hosted `ubuntu-latest` standard runner is **4**. **I used `--max-workers=4`**, which also matches the developer's ten runs and is the count at which I reproduced the flake.

*The environment brief's `--max-workers=2` preference did not bind:* `pgrep -f GradleDaemon` found two daemons, but both were at **0.0 % CPU** with 3.5 h elapsed — idle, not building. I checked before starting and used the acceptance bar's count. Two further deviations from CI, both declared: CI adds `--no-daemon` (I used the daemon; the task fixed the command) and CI's task list is `ktlintCheck detekt :domain:test test assembleDebug` (the acceptance bar fixes `:domain:test :app:testDebugUnitTest`).

### The runs
`./gradlew --max-workers=4 :domain:test :app:testDebugUnitTest --rerun-tasks`, twelve times consecutively at `b451808`, each logged to `.planning/qa/phase-14-r3/accept-run<N>.log`. Counts are tallied from the JUnit XML of **both** tasks.

| run | result | wall | tests | failures | errors | skipped | classes | `InterstitialCadenceStoreTest` | `theSessionCountIsInMemoryOnly…AsZero` |
|---|---|---|---|---|---|---|---|---|---|
| 1 | BUILD SUCCESSFUL | 259 s | 1213 | 0 | 0 | 0 | 110 | 0.066 s | 0.003 s |
| 2 | BUILD SUCCESSFUL | 91 s | 1213 | 0 | 0 | 0 | 110 | 0.026 s | 0.002 s |
| 3 | BUILD SUCCESSFUL | 77 s | 1213 | 0 | 0 | 0 | 110 | 0.025 s | 0.002 s |
| 4 | BUILD SUCCESSFUL | 78 s | 1213 | 0 | 0 | 0 | 110 | 0.025 s | 0.002 s |
| 5 | BUILD SUCCESSFUL | 100 s | 1213 | 0 | 0 | 0 | 110 | 0.029 s | 0.002 s |
| 6 | BUILD SUCCESSFUL | 111 s | 1213 | 0 | 0 | 0 | 110 | 0.079 s | 0.006 s |
| 7 | BUILD SUCCESSFUL | 76 s | 1213 | 0 | 0 | 0 | 110 | 0.037 s | 0.003 s |
| 8 | BUILD SUCCESSFUL | 93 s | 1213 | 0 | 0 | 0 | 110 | 0.049 s | 0.004 s |
| 9 | BUILD SUCCESSFUL | 86 s | 1213 | 0 | 0 | 0 | 110 | 0.027 s | 0.002 s |
| 10 | BUILD SUCCESSFUL | 69 s | 1213 | 0 | 0 | 0 | 110 | 0.024 s | 0.002 s |
| 11 | BUILD SUCCESSFUL | 72 s | 1213 | 0 | 0 | 0 | 110 | 0.028 s | 0.003 s |
| 12 | BUILD SUCCESSFUL | 58 s | 1213 | 0 | 0 | 0 | 110 | 0.021 s | 0.002 s |

**12/12 green. No failure to report — and I would have reported one in full.**

Notes on the numbers, so nobody has to re-derive them:
- **1213, not 1204.** The developer and the auditor measured 1204 at the pre-merge base. `develop` (`c59b456`, R-1/R-2) was merged into this branch at `82ff777`, adding 9 tests. The delta is the merge, not this diff — which also means these twelve runs are the first green evidence on the **merged** tree, closing the auditor's F-12 ("merge and re-run", "compilation of the merged tree not verified"). It compiles and it is green, twelve times.
- **Run 1's 259 s** is the cold compile: `--rerun-tasks` re-executes every task, and only run 1 missed the build cache. Runs 2–12 average **83 s**, higher than the auditor's 45–52 s because I ran at 4 workers against its 2 with an emulator and Android Studio taking ~2 cores.
- The formerly-flaky test ran in **0.002–0.006 s** in all twelve — four orders of magnitude below the 60 s hang, and consistent with the 0.005 s F-10 quoted as its normal cost.

### Two supporting experiments, same conditions as the reproduction
- **25 targeted runs** of the migrated `InterstitialCadenceStoreTest`, `--max-workers=4 --rerun`, identical to the loop that produced 5 hangs pre-fix: **0 failures / 25**, class time a tight 0.071–0.098 s band.
- **15 runs of the whole migrated set** (`com.app.pegsolitaire.data.*`, `…platform.lifecycle.*`, `…ui.results.*` — all ten classes, 369 tests): **0 failures / 15**.

**Aggregate: 52 post-fix executions of the formerly-flaky class (12 + 25 + 15), 0 hangs, against 5 hangs in 23 pre-fix executions.** Fisher exact two-tailed on the matched 23-vs-25 targeted arms: **p = 0.020**.

**What the statistics do and do not establish.** 0/52 bounds the residual per-run hang rate at **< 5.8 %** with 95 % confidence (rule of three) — it rules out a *high* residual rate, not a low one. The statistics are corroboration; the load-bearing argument is structural and is the grep in §3: the enabling condition cannot occur, because no real store, no file, no foreign scope and no `Dispatchers.IO` survives inside any `runTest` body in the ten classes. That is a property of the source, not a sample.

## 3. The grep that closes the class of defect

**`grep -rn 'PreferenceDataStoreFactory.create' app/src domain/src` → 2 hits, both KDoc prose, zero call sites.**

```
app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt:14: * `PreferenceDataStoreFactory.create(produceFile = { tempFolder.newFile(…) })`.
app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt:18: * A real `PreferenceDataStoreFactory.create` call with no `scope` argument gives DataStore its own
```

Line 14 names the construction being replaced; line 18 explains the defect. Both sit inside the double's class KDoc, neither is code. Broadening to bare `PreferenceDataStoreFactory` adds three more, also all prose (`ChallengeNextLevelTest:256`, `ResultsViewModelTest:1963`, `ProgressSubmitterCloudTest:1334`). `grep -rn 'TemporaryFolder' app/src domain/src` returns the same three prose lines and **no `@Rule`, no import, no use**.

### Going further — residual wall-clock and real-dispatcher dependencies in the ten migrated classes

I swept all ten with a wider pattern set than the auditor's: `System.currentTimeMillis|nanoTime`, `SystemClock`, `Instant.now`, `LocalDate`, `java.util.Date`, `Calendar.getInstance`, `Clock.system`, `Thread.sleep`, `delay(`, `withTimeout`, `awaitility`, `CountDownLatch`, `TimeUnit`, `Dispatchers.(IO|Default|Main|Unconfined)`, `newSingleThreadContext`, `Executors.`, `runBlocking`, `newFixedThreadPool`, `GlobalScope`, `Thread(`, `Random`, `randomUUID`, `Math.random`, `File(`, `FileOutputStream`, `createTempFile`, `.toFile(`, `http`, `Socket`, `okhttp`.

**Everything is clean except the wall clock.** Zero hits for sleeps, latches, timeouts, `TimeUnit`, real or `Unconfined` dispatchers, raw threads, executors, `runBlocking`, `GlobalScope`, random, UUID, filesystem and network across all ten. The single `Dispatchers.IO` hit (`ResultsViewModelTest:1964`) is KDoc.

**I confirm the auditor's F-14 list and correct one line number:**

| file | sites | status |
|---|---|---|
| `platform/lifecycle/AppForegroundTrackerTest.kt` | `:35, :37, :54, :56, :70, :83, :85, :101, :103` (9) | confirmed, unchanged |
| `ui/results/ResultsViewModelTest.kt` | **`:1624`** (1 live) + `:1867` prose | **corrected** — the auditor's `:1608` is stale; the `develop` merge (`82ff777`) shifted it to 1624 |

No eleventh site exists. The auditor's list was complete.

**My own judgement on whether any of them could produce the same 60-second failure class — and here I differ from the auditor in one direction that matters.**

The auditor's reasoning is that these are monotonic-window assertions with seconds of margin against a minutes-scale gap, so they are latent *assertion* flakes, not hangs. That is right for most of them and wrong for two, and the distinction is exactly the one that made F-10 expensive: **a `first { predicate }` whose predicate is never satisfied does not fail an assertion — it hangs until `runTest`'s 60-second wall-clock timeout kills it**, producing an `UncompletedCoroutinesError` indistinguishable at the console from F-10's.

- **Not a hang, cannot be one — `:35/:37`, `:54/:56` (`onStopStamps…`, `onStartAsks…`).** These are not `runTest` at all: a MockK `slot` capture with `isAtLeast(before)` / `isAtMost(after)` around a synchronous call. `System.currentTimeMillis()` never suspends and never blocks, so it cannot park a coroutine. The only fault it can cause is a red assertion in microseconds, and only if the wall clock steps **backwards** between two adjacent statements (NTP/manual set — `currentTimeMillis` is not monotonic). Negligible, and cheap when it happens.
- **Not a hang — `:101/:103` (`onStartInsideTheGapContinuesTheSameSession`).** `leftAt` is the test's own captured value and is awaited by exact equality (`first { it == leftAt }`), which is deterministic. Breaking it needs a real stall of ≥ `SESSION_GAP_MS` = **30 minutes** between two statements, and the outcome is a clean assertion failure at `:113`, not a hang.
- **Can hang, and this is the finding — `:70`→`:73` (`onStopPersistsTheStampSoASecondHolderReadsItBack`) and `:85`→`:92` (`onStartAfterTheFullBackgroundGapOpensANewSession`).** Both await an *open* predicate over a value the production code samples from the wall clock independently of the test:
  - `:73` `store.lastForegroundAtMs.first { it >= before }` — if the tracker's own `currentTimeMillis()` came back **below** the `before` taken at `:70`, the predicate is never satisfiable and the body hangs for 60 s.
  - `:92` `store.state.first { it.firstSessionComplete }` — the session opens only if `now' - leftAt ≥ SESSION_GAP_MS`, i.e. `now' ≥ now - SETTLE_MARGIN_MS`. The `SETTLE_MARGIN_MS = 1_000` slack is absorbing a **backwards** clock step, not a stall (a forward stall makes the condition hold *more* strongly — worth stating precisely, because the auditor's "only a stall of over a second could flip it" has the direction the other way round for this site). A backwards step of > 1 s between `:85` and `:89` leaves `firstSessionComplete` false forever and the body hangs for 60 s.

  In both cases the trigger is the wall clock moving backwards — an NTP correction or a clock change on the runner — not load, so the probability is far below F-10's 21.7 % and I am **not** raising F-14 to a release blocker. But its consequence is not "a flaky assertion": it is the same 60-second `UncompletedCoroutinesError` that just cost this phase a release-gate scare, arriving on a run nobody can reproduce. **Two things follow.** (1) The auditor's severity framing understates it; the injected clock in `AppForegroundTracker` removes a residual hang path, not just a margin. (2) Operationally: **if a 60-second `UncompletedCoroutinesError` is ever seen in this suite again, `AppForegroundTrackerTest` is the first place to look**, and that sentence is the reason this paragraph is in the report.
- **`ResultsViewModelTest:1624` is a wall-clock *read*, not a wall-clock *dependency*, and I would not count it as a violation.** `eligibleCadenceStore` captures `now` once and uses the same constant for the seeding (`now - SESSION_GAP_MS - 1`), the transition (`startSessionIfGapElapsed(now)`) and the predicate (`first { it.mayShow(now) }`); `mayShow(nowMs: Long)` takes its instant as a parameter and reads no clock. The fixture is self-consistent by construction and cannot hang or flake on clock behaviour. It should still go when `AppForegroundTracker` gets an injected clock, for the rule's letter and for uniformity, but it carries no risk today.

**In the letter of `rules/testing.md` ("Deterministic tests only: no wall-clock…") the ten sites in `AppForegroundTrackerTest` remain violations.** They are correctly outside F-10's and R-3's remit — the honest fix is an injected clock in `AppForegroundTracker`, which is production source and would breach standing rule 4 for this task. F-14 stays with the producer to schedule; I am raising its *rationale*, not its priority.

## Checks
- `git status --porcelain` at start and at finish, `git rev-parse --short HEAD` — **pass** (empty both times; `b451808` throughout; branch `feature/phase-14-datastore-determinism`; no branch switch, no `git add`, no commit, no source or test edit).
- `git clone --shared … && git checkout --detach f9d6ef9` into the session scratchpad; `local.properties` written from the `sdk.dir` line only — **pass** (no credential copied; product worktree untouched).
- `git diff --stat f9d6ef9 c59b456 -- app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt` — **pass** (empty: the pre-fix test is byte-identical at the reproduction commit and at F-10's observation point).
- **Reproduction**, `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest*' [--rerun]` × 23 at the pre-fix commit — **REPRODUCED**: 5 hangs, `UncompletedCoroutinesError` at 60.033 / 60.041 / 60.050 / 60.036 / 60.052 s. Table in §1.
- Same command at `--max-workers=1` × 8 — **0 hangs** (probe, not significant: p ≈ 0.14).
- `.github/workflows/ci.yml` + `gradle.properties` + repo-wide `--max-workers` grep — **pass** (no worker count is configured; CI runs at Gradle's default = runner CPU count = **4** on `ubuntu-latest`).
- `pgrep -f GradleDaemon` + `ps -o pcpu` before starting — **pass** (2 daemons, both 0.0 % CPU, idle; no sibling build in flight, so `--max-workers=4` was used).
- **`./gradlew --max-workers=4 :domain:test :app:testDebugUnitTest --rerun-tasks` × 12 consecutive** at `b451808` — **all twelve BUILD SUCCESSFUL**; 1213 tests / 0 failures / 0 errors / 0 skipped / 110 classes in every run. Table in §2.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest*' --rerun` × 25 at `b451808` — **pass** (0 failures; 13 tests each; class 0.071–0.098 s).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --rerun --tests 'com.app.pegsolitaire.data.*' --tests 'com.app.pegsolitaire.platform.lifecycle.*' --tests 'com.app.pegsolitaire.ui.results.*'` × 15 — **pass** (0 failures; 369 tests each).
- `grep -rn 'PreferenceDataStoreFactory.create' app/src domain/src` — **pass** (**2 hits, both KDoc prose** at `InMemoryPreferencesDataStore.kt:14,18`; **zero call sites**).
- `grep -rn 'PreferenceDataStoreFactory' app/src domain/src` (broadened) — **pass** (5 hits, all prose).
- `grep -rn 'TemporaryFolder' app/src domain/src` — **pass** (3 hits, all prose; no `@Rule`, no import, no use).
- Residual-dependency sweep over the ten migrated classes, 28 patterns — **pass except the wall clock**: zero hits for sleeps, latches, timeouts, `TimeUnit`, real/`Unconfined` dispatchers, raw threads, executors, `runBlocking`, `GlobalScope`, random, UUID, filesystem, network. `System.currentTimeMillis()` at `AppForegroundTrackerTest:35,37,54,56,70,83,85,101,103` and `ResultsViewModelTest:1624` (auditor's `:1608` corrected). §3.
- The ten migrated classes identified from the diff rather than from the reports — **pass** (`CloudSyncStoreTest`, `ChallengeProgressTest`, `InterstitialCadenceStoreTest`, `SettingsRepositoryTest`, `PlayGamesOptInRepositoryTest`, `AppForegroundTrackerTest`, `SubmissionSyncStoreTest`, `ResultsViewModelTest`, `CloudProfileStoreTest`, `OnboardingRepositoryTest`; `ProgressSubmitterCloudTest`'s single hit is KDoc, matching the developer's stated non-migration).
- `SESSION_GAP_MS` and `mayShow(nowMs: Long)` read from `domain/src/main` rather than assumed — **pass** (`SESSION_GAP_MINUTES = 30L`; `mayShow` takes its instant as a parameter and reads no clock — the basis of the `:1624` judgement).
- **Not run, by the task's constraints:** any emulator, adb, device or Play Games account (the brief forbids them and nothing here needs them); the full `./gradlew check` (the producer owns it at the merge); `:app:lint`; any release assembly — so no ad unit id or Play Games id was ever required or read.

## Open issues / risks
- **F-14 is a residual 60-second-hang path, not only a margin.** `AppForegroundTrackerTest:73` and `:92` await open wall-clock predicates with no timeout; if the runner's clock steps backwards they hang for 60 s with F-10's exact error. Probability is far below F-10's and this is **not** a release blocker, but the injected clock the auditor recommends buys more than tidiness, and if this error is ever seen again this file is where to look first. For the producer to schedule; production source, so out of R-3's remit.
- **F-11 stands and I add nothing that weakens it.** `InMemoryPreferencesDataStoreTest` remains the only guard on both halves of the double's contract — my 52 green executions of the migrated classes are, if anything, further confirmation that the ten classes are insensitive to it. Any future edit to the double that is not accompanied by a failing test removes the mitigation silently.
- **F-12 is discharged by these runs, but only for this tree.** `develop` was merged into the branch at `82ff777`; the merged tree compiles and is green twelve times at 1213 tests. If `develop` moves again before the merge, the merge-and-re-run obligation returns.
- **My rates are machine-specific.** 21.7 % pre-fix and < 5.8 % post-fix (95 % upper bound) describe this macOS/aarch64 machine under ~2 cores of foreign load. The structural argument — zero `PreferenceDataStoreFactory.create` call sites — is what transfers to `ubuntu-latest`, not the numbers.
- **The tech-lead's fallback in item C is now moot and should be retired**, not merely unused: it allowed cutting the release branch with only the flaking class migrated, on evidence of 10 consecutive green runs. All ten migrated and twelve consecutive runs are green, so the full remedy landed and there is nothing for the release-manager to decide.
- **`.planning/qa/phase-14-r3/` (1.1 MB) is for the producer to delete** after the STATUS.md Done line (`rules/testing.md`). Nothing in it is committed; `git status` is clean.

## Handoff
Next: **producer** — R-3's acceptance evidence is complete and the verdict is ACCEPT. Merge `feature/phase-14-datastore-determinism` into `develop` with `git merge --no-ff` once the standing gitflow preconditions are met (full `./gradlew check` green at the merge — my twelve runs cover `:domain:test` and `:app:testDebugUnitTest` but **not** ktlint/detekt/ArchUnit/the dp-sp gate/`assembleDebug`, which the auditor ran separately and which `check` re-runs). Then: delete `.planning/qa/phase-14-r3/`; schedule **F-14** with the rationale in §3 attached (it is a hang path, not a margin) and **F-13** (the two untested private doubles and the comment the merge falsifies); and tell the **release-manager** that item C's partial-migration fallback is moot. Nothing here goes back to the tech-lead.
