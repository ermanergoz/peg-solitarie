# code-auditor report — phase 14 DataStore determinism

**Verdict: PASS.** The double's contract holds under my own mutation: removing `writeLock.withLock` reproduces `expected: 50 but was: 1` on the atomicity test exactly as reported, and a conflating buffer (`replay=1, extra=0, DROP_OLDEST` ≡ `StateFlow`) fails exactly the two named emission tests (`[null, 7, 9]` vs `[null, 9]`; size 65 vs 2). Neither test is vacuous. I also probed six properties the shipped contract test does *not* pin — exception propagation, state-after-failed-update, lock release after a throw and after a cancel, two concurrent collectors, duplicate-on-equal-write — all six behave correctly or diverge only in the documented, conservative direction. The migration is behaviour-neutral: all eleven `@Test` counts identical (19/6/21/13/4/7/39/11/6/6/54), exactly one added non-KDoc line in the ten classes, nothing outside `app/src/test/` and `.planning/`, no suppression, no `@Ignore`, no dependency. Two full suites green at 1204/109/0 skipped. **No blocking finding.** One item the producer must act on before merging: `develop` advanced *during this audit* (`c59b456`, the R-1/R-2 merge) and now touches `ResultsViewModelTest.kt`, so the developer's "touches no file they touch, merge in either order" no longer holds — F-12. The trial merge is clean, but it must be merged-then-re-run, not merged on that claim.

Date: 2026-09-21
Branch: feature/phase-14-datastore-determinism (944821f..86fbc93; reviewed as `git diff $(git merge-base develop HEAD)..HEAD`, base `f9d6ef9`)
Skills loaded: `ergoz-soft:standards` (role: code-auditor) → plugin copies (v0.5.2, newer than this worktree's `.claude/rules/VERSION` 0.5.1) of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `testing.md`, `kotlin-android.md`, `compose.md`, `puzzle-design.md`, `play-store.md`; repo `.claude/rules/lessons.md`. **Deviation, declared:** I did not invoke `code-reviewer`, `security-reviewer`, `gsd-code-review`, `compose-skill` or `compose-performance-audit` as Skill tools. The diff contains zero production source and zero Compose, so the Compose pair has no subject; the correctness and security passes were made directly against the rule files and are itemised in Checks (secrets/network/filesystem/reflection/process sweep over every added line, boundary and shared-state review of the new class). Recording the non-invocation rather than implying coverage I did not obtain.
Ran in parallel with: R-1/R-2 on `feature/phase-14-prerelease-fixes` — which **merged into `develop` mid-audit**; see F-12
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **1. Does the double honour the contract the tech-lead fixed?** Yes, and I verified it by mutation rather than accepting the report. Both halves are load-bearing and both are pinned by tests that discriminate. Six further properties I probed myself hold. Two divergences exist; neither is exercised by any of the ten classes, which I proved in situ.
- **2. Did the migration change what any test means?** No. Every `@Test` count is identical, exactly one added line across the ten classes is not the double's name or a KDoc line, no assertion was weakened or removed, and no class lost coverage of a real-IO path — none of the ten asserted anything about the file.
- **3. Is the flake addressed or hidden?** Addressed. The double contains no dispatcher, no `delay`, no timeout, no `System.*`, no shared mutable state (companion is `const val` only; every use site constructs a fresh instance). Zero `PreferenceDataStoreFactory.create` call sites survive.
- **4. Standards.** detekt + ktlint clean, and I proved by my own probe that detekt reaches the new package **in this worktree**, where the F-1 fix is not merged. No suppression, baseline, `@Ignore` or disabled test to rule on. No dependency, so no `THIRD_PARTY.md` row is owed. Test names read as sentences.
- **5. Evidence.** Counts reproduce exactly (1204 / 109 / 0 skipped, twice). Timings reproduce and are slightly better than reported on my machine (45 s, 52 s at `--max-workers=2`). I agree with the `ProgressSubmitterCloudTest.kt:1344` non-migration — with one new fact that strengthens the case for scheduling the fold-in (F-13).

## Findings

### Verified, not merely accepted — the double's contract

**Mutation A — serialisation.** `app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt:106`, `writeLock.withLock {` → `run {`:

```
InMemoryPreferencesDataStoreTest > concurrentReadModifyWriteUpdatesAllLandBecauseUpdateDataSerialises FAILED
  com.google.common.truth.ComparisonFailureWithFacts: expected: 50
  but was : 1
```

Exactly the developer's reported figure. 49 of 50 increments lost. The atomicity test is not vacuous.

**Mutation B — conflation.** `:87-91`, `extraBufferCapacity = UNBOUNDED_EXTRA_BUFFER` → `extraBufferCapacity = 0, onBufferOverflow = DROP_OLDEST` (semantically identical to the `MutableStateFlow` variant the developer described):

```
InMemoryPreferencesDataStoreTest > anAlreadyCollectingCollectorSeesEverySubsequentValueInOrder FAILED
  missing (1): 7 --- expected: [null, 7, 9] / but was: [null, 9]
InMemoryPreferencesDataStoreTest > noUpdateIsDroppedWhileTheCollectorIsSuspended FAILED
  expected: 65 / but was: 2 / iterable was: [null, 63]
```

The same two test names, the same count. Both emission properties are pinned independently.

**Properties the shipped contract test does not pin — my own probe, six tests, all green** (`ZzAuditProbeContractTest`, run then deleted; `git status --porcelain` empty afterwards):

| property | real `DataStore` | the double | judgement |
|---|---|---|---|
| a throwing `transform` propagates and leaves state untouched | yes | yes (`IOException` rethrown, `data` still `7`) | matches |
| the lock is released after a throwing transform | n/a | yes, next caller writes `3` | no deadlock |
| the lock is released after a **cancelled** writer | n/a | yes, next caller writes `5` | no stranded mutex |
| a caller cancelled inside its transform writes nothing | the write is enqueued on the store's own scope, so behaviour is implementation-dependent | writes nothing (`emptyPreferences()`) | divergence, conservative direction — a test expecting the write to land would go **red** here, i.e. be caught |
| two concurrent collectors each see every value | yes | yes, `[null, 1, 2]` on both, in order | matches |
| an unchanged write still emits | no — a real store skips the equal write | yes, `[null, 1, 1]` where a real store gives `[null, 1]` | divergence, documented; quantified here |

**The one direction in which the double is genuinely more forgiving, and why it is not live.** Non-conflation means production code that depends on receiving *every* intermediate emission would pass here and could still miss one against a real, conflating store. The developer's phrasing — "strictly weaker … so it cannot turn a passing test red" — is correct about the tests but does not close that gap. I closed it empirically: I ran the whole migrated set against the **conflating** mutation (`--tests 'com.app.pegsolitaire.data.*' --tests '…platform.lifecycle.*' --tests '…ui.results.*'`) and it was **BUILD SUCCESSFUL in 25s, 0 failures**. No class depends on non-conflation, so the permissive direction is not exercised today.

### F-11 — LOW (informational, not blocking): neither contract half is exercised in situ, so `InMemoryPreferencesDataStoreTest` is the *only* guard

`app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt:75` (`writeLock`) and `:87` (`updates`). Running the migrated set against **mutation A** (no serialisation) gave **BUILD SUCCESSFUL in 24s, 0 failures**; against **mutation B** (conflating), **BUILD SUCCESSFUL in 25s, 0 failures**. So the ten classes' assertions are insensitive to both halves. Two consequences, and they point in opposite directions:
- **In the diff's favour:** this is the strongest available evidence that the migration is behaviour-neutral. The ten classes cannot have changed meaning through a property they do not touch.
- **The risk to record:** the tech-lead's condition 1 ("an unverified fake under 10 classes and ~200 tests is a worse risk than the flake") is satisfied *entirely* by the contract test and by nothing else. If that class is ever weakened, loosened or its two discriminating tests reworded, the loss is silent — no downstream red. The developer says this in its own risk section; I am confirming it with the measurement, so the next person does not have to re-derive it. `AppForegroundTrackerTest.kt:108-110`'s stated rationale ("DataStore serialises writes, so observing this later one proves the onStart write already ran") does rest on the serialisation property, so the property must stay even though today's assertion survives without it.

### F-12 — MEDIUM (merge precondition, not a defect in this diff): the diff's base moved mid-audit and the "independent branches" claim no longer holds

`develop` was `f9d6ef9` when this branch was cut and is now `c59b456` ("Merge branch 'feature/phase-14-prerelease-fixes' into develop"), landed while I was auditing. The developer's report states R-3 "touches no file they touch, so this branch can merge to `develop` in either order". That is now false for one file:

```
$ comm -12 <(git diff f9d6ef9..HEAD --name-only|sort) <(git diff f9d6ef9..develop --name-only|sort)
app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
```

`develop` added to that file a `challengeProgress` constructor argument, a `freshLadder()` fixture and a private `inMemoryDataStore()`; this branch deleted `realDataStore()`, `tempFolder` and `storeFileCount` from it. I verified the trial merge is textually clean and reference-consistent — `git merge-tree --write-tree develop HEAD` → tree `801367e`, **0 conflict markers**, `@Test` count still **54**, **no surviving reference** to `realDataStore`/`tempFolder`/`storeFileCount`/`TemporaryFolder` outside one KDoc line, and every `import` in the merged file still used (so ktlint's unused-import rule will not fire). **What I did not verify: that the merged tree compiles and is green.** That is the producer's `./gradlew check` at the merge, which `rules/gitflow.md` already requires. Action: merge `develop` into the branch (or merge the branch and re-run), do not rely on the "independent" sentence.

### F-13 — LOW (schedule, not a release blocker): the fold-in the developer scheduled is now two sites, and one of them will ship a false comment

`app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt:1344` holds a private, untested, non-serialised, conflating `inMemoryDataStore()`. `develop` has just added a **byte-identical copy** of it to `ResultsViewModelTest.kt` (merged line 1967), whose KDoc at merged line 1963 reads:

> `Deliberately not `PreferenceDataStoreFactory` over a `TemporaryFolder` (the cadence fixtures above)`

That sentence becomes false the moment R-3 lands — the cadence fixtures above will use `InMemoryPreferencesDataStore()` (merged lines 1606, 1623). It is precisely the stale cross-reference class the developer correctly fixed in `ProgressSubmitterCloudTest.kt:1334-1339`. So after the merge the file will carry the tested shared double *and* an untested private duplicate, with a comment telling the reader the opposite. Not this diff's fault and not worth holding a release for; for the producer to schedule with the fold-in the developer already flagged.

### F-14 — LOW (pre-existing, correctly outside F-10's remit): a residual wall-clock dependency the migration did not touch

`app/src/test/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTrackerTest.kt:35,37,54,56,70,83,85,101,103` and `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:1608` still call `System.currentTimeMillis()`. These are the only residuals in the ten classes — `Thread.sleep`, real `Dispatchers`, `delay`, `withTimeout`, `nanoTime` and unseeded `Random` are absent from all ten. I agree they are not F-10's to fix and they are not of the same failure class: the assertions are monotonic windows (`isAtLeast(before)` / `isAtMost(after)`), and the one arithmetic case at `:85` carries `SESSION_GAP_MS` (`SESSION_GAP_MINUTES × 60_000`) plus a deliberate `SETTLE_MARGIN_MS = 1_000L` of slack, so only a stall of over a second between two adjacent statements could flip it. In the letter of `rules/testing.md` ("no wall-clock") they are still violations; the honest fix is an injected clock in `AppForegroundTracker`, which is a production change and out of this task's remit under standing rule 4. Recorded so it is not rediscovered, not blocking.

### Informational — the unbounded buffer trades a dropped value for unbounded retention

`InMemoryPreferencesDataStore.kt:131`, `UNBOUNDED_EXTRA_BUFFER = Int.MAX_VALUE - REPLAY_LATEST`. The KDoc's claim is accurate (the array grows lazily), and a suspended collector therefore retains every unread value rather than dropping one. Bounded in practice — the largest burst anywhere in the suite is 64, in the double's own test — and the alternative is the conflation the whole design rejects. No action.

## Suppressions
**None to rule on.** No `@Suppress`, baseline entry, `@Ignore`, `assumeTrue`, `@Disabled`, disabled or deleted test, and no widened detekt exclude appears anywhere in `f9d6ef9..HEAD`. `config/detekt/detekt.yml` is not in the diff; `find . -name '*baseline*.xml' -not -path '*/build/*'` returns nothing. The four `grep` hits for those strings in the diff are all prose inside the developer's report.

## Checks
- `git diff $(git merge-base develop HEAD)..HEAD --stat` — **pass** (14 files, 532 insertions / 220 deletions; base `f9d6ef9`).
- `git diff f9d6ef9..HEAD --name-only | grep -vE '^app/src/test/|^\.planning/'` — **pass** (empty: **no production source touched**, test sources and planning only).
- `git diff f9d6ef9..HEAD --stat -- gradle/ '*.gradle.kts' gradle.properties settings.gradle.kts` — **pass** (empty: zero dependency change, so **no `THIRD_PARTY.md` row is owed**; the file's absence is R-5, the release-manager's item).
- `@Test` count per touched file, base vs HEAD — **pass** (identical in all eleven: 19/6/21/13/4/7/39/11/6/6/54; the only delta is `InMemoryPreferencesDataStoreTest.kt` 0 → 10).
- Added lines in the ten classes that are neither the double's name nor a KDoc/blank line — **pass** (**exactly one**: the wrapped `private fun freshCadenceStore(): InterstitialCadenceStore =` signature). No `assert`/`verify`/`expect` line removed anywhere except one KDoc prose line.
- `./gradlew --max-workers=2 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.testing.*' --rerun-tasks` at HEAD — **pass** (`BUILD SUCCESSFUL in 12s`; 10 tests, 0 failures, 0.072 s).
- **Mutation A** (`writeLock.withLock` → `run`), same command — **fails as intended** (`10 tests completed, 1 failed`; `concurrentReadModifyWriteUpdatesAllLandBecauseUpdateDataSerialises`, `expected: 50 but was : 1`).
- **Mutation B** (conflating buffer), same command — **fails as intended** (`10 tests completed, 2 failed`; `anAlreadyCollectingCollectorSeesEverySubsequentValueInOrder` `[null, 7, 9]` vs `[null, 9]`; `noUpdateIsDroppedWhileTheCollectorIsSuspended` size 65 vs 2, `[null, 63]`).
- **In-situ mutation, A and B**, `./gradlew --max-workers=2 :app:testDebugUnitTest --rerun-tasks --tests 'com.app.pegsolitaire.data.*' --tests 'com.app.pegsolitaire.platform.lifecycle.*' --tests 'com.app.pegsolitaire.ui.results.*'` — **BUILD SUCCESSFUL in 24s / 25s**, 0 failures both times. Basis of F-11.
- **Auditor probe, 6 properties the contract test omits** (`ZzAuditProbeContractTest`) — **pass** (6 tests, 0 failures, 0.007 s). Table above.
- **Detekt reaches the new package in *this* worktree** (F-1 fix **not** merged here) — probe `ZzAuditProbeDetekt.kt` in `app/src/test/java/com/app/pegsolitaire/testing/`, `./gradlew --max-workers=2 :app:detekt --rerun-tasks` — **fails as intended**: `> Analysis failed with 2 issues.`, quoted `…/testing/ZzAuditProbeDetekt.kt:9:18 … [SwallowedException]` and `…:4:9 onlyReturningConstant is returning a constant … [FunctionOnlyReturningConstant]`. Checked rather than assumed. Probe deleted.
- `./gradlew --max-workers=2 detekt ktlintCheck --rerun-tasks` at HEAD — **pass** (`BUILD SUCCESSFUL in 7s`, `25 actionable tasks: 25 executed`).
- `./gradlew --max-workers=2 :domain:test :app:testDebugUnitTest --rerun-tasks` **× 2** — **both pass**, counts tallied from the JUnit XML of both tasks:

  | run | wall | result | tests | failures | errors | skipped | classes | `InterstitialCadenceStoreTest` | the formerly-flaky test |
  |---|---|---|---|---|---|---|---|---|---|
  | 1 | 45 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.018 s | 0.002 s |
  | 2 | 52 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.021 s | — |

  Counts reproduce the report exactly. Timings are *better* than its 59.5 s mean at a lower worker count (`--max-workers=2` vs its 4), so the "suite got faster" claim reproduces in direction and magnitude. Ten consecutive runs not repeated — the qa-engineer owns the N ≥ 10 acceptance evidence per the tech-lead's ruling.
- `grep -rn 'PreferenceDataStoreFactory' app/src domain/src` — **pass** (**3 hits, all KDoc prose**: `InMemoryPreferencesDataStore.kt:14,18` and `ProgressSubmitterCloudTest.kt:1334`. **Zero call sites** anywhere.)
- Residual non-determinism sweep over the ten classes (`System.currentTimeMillis|Thread.sleep|Dispatchers.(IO|Default|Main)|Random(|delay(|withTimeout|nanoTime|Instant.now`) — **8 of 10 clean**; `System.currentTimeMillis()` remains in `AppForegroundTrackerTest` and `ResultsViewModelTest:1608` only (F-14). No `Thread.sleep`, no real dispatcher, no `delay`, no `withTimeout`, no unseeded random anywhere.
- Shared-state sweep — **pass**. Every one of the ~70 use sites constructs `InMemoryPreferencesDataStore()` inline; no class-level field, no `object`, no static holder; the companion contains two `const val` and nothing else. No state can leak between tests or classes.
- The double's imports — **pass** (`androidx.datastore.core.DataStore`, `…preferences.core.{Preferences, emptyPreferences}`, `kotlinx.coroutines.flow.{Flow, MutableSharedFlow, asSharedFlow}`, `kotlinx.coroutines.sync.{Mutex, withLock}`). No `Dispatchers`, no `delay`, no `withTimeout`, no `runBlocking`, no `Thread`, no `System.*`, no file API. Nothing reintroduced.
- Security pass over every added line in `app/src` — **pass** (no `http`/`url`/`socket`/okhttp/retrofit, no `ca-app-pub-`/`CgkI`/`AIza`/PEM/password/secret/token/keystore, no `File(`/`FileOutputStream`, no `Runtime.`/`ProcessBuilder`/`System.getenv`, no reflection. Single hit is the KDoc line naming the API being replaced.)
- Test names read as sentences (`rules/testing.md`) — **pass** for all ten new names, listed in full.
- `@Ignore`/`assumeTrue`/`@Disabled` across `app/src/test` and `domain/src/test` — **pass** (zero hits repo-wide, not just in the diff).
- Trial merge, `git merge-tree --write-tree develop HEAD` — tree `801367e`, **0 conflict markers**; merged `ResultsViewModelTest.kt` has 54 `@Test`, no dangling `realDataStore`/`tempFolder`/`storeFileCount`/`TemporaryFolder` reference, and every import used. Basis of F-12/F-13. **Compilation of the merged tree not verified.**
- Final state — `git status --porcelain` **empty**, `git diff --stat` **empty**, HEAD still `86fbc93`, `app/src/test/.../testing/` contains exactly the two shipped files, no probe residue, no commit, no source edit persisted, nothing written into the repo by me.
- **Not run, by the task's constraints:** the full `./gradlew check` (the producer owns it), `:app:lint`, any release assembly, anything touching adb or an emulator. No release-named task invoked, so no ad ids were needed.

## Open issues / risks
- **F-12 is the only thing that must happen before the merge**, and it is a mechanic, not a fix: `develop` moved to `c59b456` during this audit and now shares `ResultsViewModelTest.kt` with this branch. Merge and re-run; the trial merge is clean but unverified by compilation.
- **F-11:** the contract test is the sole guard on both halves of the double's contract. Any future edit to `InMemoryPreferencesDataStore` that is not accompanied by a test failing without it removes the mitigation, and nothing downstream will go red.
- **F-13:** two identical untested private in-memory doubles will coexist with the tested shared one after the merge, and one carries a comment that the merge makes false. Schedule the fold-in.
- **F-14:** `AppForegroundTracker` takes its wall-clock stamp from `System.currentTimeMillis()`, so its test cannot be fully deterministic without an injected clock. Out of remit here; the margins are seconds against a minutes-scale gap.
- **I make no claim about the reproduction.** I did not attempt the 60.041 s failure and neither did the developer. Two green runs plus the developer's ten prove the *pattern* is gone from test sources, which is the falsifiable part; the qa-engineer owns the acceptance bar.
- `THIRD_PARTY.md` still absent — vacuous for this branch (zero dependency change) and still R-5 before Gate 2.

## Handoff
Next: **producer** — merge `develop` (`c59b456`) into `feature/phase-14-datastore-determinism` first, re-run `:domain:test :app:testDebugUnitTest` and the full `./gradlew check` on the merged result, then `git merge --no-ff` to `develop` after the **qa-engineer** PASS and the **tech-lead** sign-off. F-12 is the reason the order now matters; the developer's "either order" sentence predates `c59b456`.
Then: **qa-engineer** — owns R-3's acceptance evidence per the tech-lead's ruling (bounded reproduction attempt, N ≥ 10 consecutive green full-suite runs at CI's worker count, and the `PreferenceDataStoreFactory.create`-without-`scope` grep, which I already show at zero call sites).
Also for the **producer** to schedule, neither a release blocker: **F-13** (fold the two private doubles into the shared one and delete the comment the merge falsifies) and **F-14** (an injected clock in `AppForegroundTracker`).
