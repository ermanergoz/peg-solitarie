# code-auditor report — phase 14 gate scope

Date: 2026-09-21
Branch: feature/phase-14-gate-scope (f2b5de9..004e5fe)
Skills loaded: ergoz-soft:standards (role: code-auditor) — studio.md, reporting.md, gitflow.md, testing.md, kotlin-android.md, skills-policy.md, logging.md, memory.md (plugin copies, v0.5.2 > snapshot v0.5.1)
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` — 0 active lessons)

## Summary
R-1′ implements all four rulings and I could reproduce every load-bearing claim with my own probes. The predicate correctly frees analysis-only tasks and still fires for every real packaging entry point; the three ID/signing checks are ordered before packaging by a hard `dependsOn` edge that I confirmed both in the graph and in an **executed** build that failed in `verifyReleaseSigning` with no artifact on disk; the literal gates now scan all three shipped source sets and no test source set; the tripwire fires on both modules. Five findings, all Low/Medium, none blocking. PASS.

## Verdict
**Verdict: PASS**

## Rulings

### Ruling 1 — `isReleaseInvocation` as a packaging-verb prefix test — SATISFIED
Extracted to `build-logic/src/main/kotlin/ReleaseInvocation.kt:62`, consumed at `app/build.gradle.kts:90`, configuration-time throw kept at three sites (`app/build.gradle.kts:104, 143, 175`).

*False positives (the point of the ruling) — cleared.* `env -u PLAY_UPLOAD_* ./gradlew -m :app:lintRelease :app:detektRelease` → `BUILD SUCCESSFUL in 467ms`. Under the old `contains("elease")` both demanded two keystore passwords and four ID values. That is the operator-pressure hazard closed, verified directly.

*False negatives — none among real entry points.* I enumerated every task in the combined `:app:bundleRelease :app:assembleRelease :app:installRelease` graph (73 tasks containing "Release") and classified each against `PACKAGING_VERBS`. The only matches are `assembleRelease`, `bundleRelease`, `packageRelease`, `packageReleaseBundle`, `installRelease`, `packageReleaseResources`. Every task that produces or installs a shippable artifact is matched. `signReleaseBundle` is the one real task that signs an artifact and does **not** match — but it depends on `packageReleaseBundle`, which carries the three verify tasks, so it is covered at execution time (F-1).

*Adequacy of the unit-test proof for `publishReleaseBundle` — YES, and for a stronger reason than the developer gave.* There is no publishing plugin in this build (`triplet`/`play-publisher` absent from `gradle/libs.versions.toml` and every build script), no `publishing {}` / `singleVariant` block in `app/build.gradle.kts`, and no `publish*` task in the release graph. A Gradle-level invocation of `publishReleaseBundle` was **impossible**, not merely blocked — so the predicate-level test is the only available and the correct proof. See F-5 for the misattributed reason in the developer's report.

*The narrowing is safe because the backstop is real, not asserted.* `verifyReleaseAdIds` / `verifyReleaseGamesIds` / `verifyReleaseSigning` (`app/build.gradle.kts:192, 207, 226`) re-run the resolutions from `adIdEnv`/`adIdLocal`/`signingEnv`, not from the possibly-placeholder configured values. Confirmed by execution, below.

### Ruling 2 — both ID checks on `packageRelease`/`packageReleaseBundle`, lifecycle hook kept — SATISFIED
`app/build.gradle.kts:255` (package hook, all three checks), `:259` (lifecycle hook retained), `:269` (`validateSigningRelease` ordering).

Graph, my own runs:
- `-m :app:bundleRelease` → `verifyReleaseAdIds`(83), `verifyReleaseGamesIds`(84), `verifyReleaseSigning`(85), **then** `packageReleaseBundle`(86).
- `-m :app:assembleRelease` → `verifyReleaseSigning`(84), `validateSigningRelease`(85), `verifyReleaseAdIds`(86), `verifyReleaseGamesIds`(87), **then** `packageRelease`(90).

**Is a `--dry-run` graph sufficient for an ordering property?** For a `dependsOn` edge, yes, and I will say why rather than assert it: `dependsOn` is a hard happens-before in Gradle's execution contract, `-m` renders the *resolved* graph in execution order, and the edge either exists or it does not — there is no runtime-only failure mode for the ordering itself. What a dry run cannot show is whether the *task bodies* work and whether anything reconfigures late. So I ran it for real, which the developer declared as a gap:

```
PLAY_UPLOAD_KEYSTORE=/tmp/nope.jks PLAY_UPLOAD_KEY_ALIAS=upload \
PLAY_UPLOAD_KEY_PASSWORD=x PLAY_UPLOAD_STORE_PASSWORD=y \
./gradlew :app:packageRelease --max-workers=2
→ > Task :app:verifyReleaseSigning FAILED
  Execution failed for task ':app:verifyReleaseSigning'
  "Check the exported path; the keystore lives outside this repository."
  BUILD FAILED in 9s
→ find app/build/outputs -name "*.apk" -o -name "*.aab"  →  (empty)
```
`packageRelease` never executed and no artifact reached disk — the exact scenario the ruling exists to prevent, demonstrated rather than inferred. The gap the developer declared is now closed; the report's declaration of it was honest and correct.

### Ruling 3 — `shippedSourceRoots` covers main+release+debug, tests excluded, scoped messages, tripwire — SATISFIED
My plant-and-delete probe across all five `:app` source sets (non-comment code lines; my first attempt used `//` comments and was correctly ignored by the pre-existing comment exemption at `standards.gradle.kts:192`):

| Planted in | `adIdLiteralCheck` | `gamesIdLiteralCheck` | `audioFocusLiteralCheck` |
|---|---|---|---|
| `src/main/java` + `src/main/res` | FAIL (both offenders listed) | FAIL | FAIL |
| `src/release/java` | FAIL | FAIL | FAIL |
| `src/debug/java` | FAIL | FAIL | FAIL |
| `src/test/java` | pass | pass | pass |
| `src/testDebug/java` | pass | pass | pass |

Every failure message carried its scope, derived not retyped: `Scanned: src/main, src/release, src/debug (java, kotlin, res). Test source sets are excluded on purpose (see SourceSetPolicy).`

Tripwire, exercised separately on both modules:
- `mkdir app/src/staging/java; ./gradlew :app:tasks` → configuration FAILED: `Unclassified source set(s) in :app: src/staging`, naming both lists, both decisions and `KNOWN_SOURCE_SET_NAMES`.
- `mkdir domain/src/flavourish; ./gradlew :domain:tasks` → `Unclassified source set(s) in :domain: src/flavourish`.

**Tree confirmed clean afterwards**: `git status --porcelain` empty; `app/src` back to `debug main release test testDebug`, `domain/src` back to `main test`.

### Ruling 4 — CI runs `check` verbatim plus a build-logic test step — SATISFIED, and the right call
`.github/workflows/ci.yml:53` (`./gradlew -p build-logic test`, first, fail-fast — correct, `build-logic` is an included build and its tests are not reachable from the root `check`) and `:70` (`./gradlew check assembleDebug`).

`./gradlew -m check` → 130 tasks, containing `:app:{ad,games,audioFocus,dpSp}LiteralCheck`, `:domain:{ad,games,audioFocus,dpSp}LiteralCheck`, `:app:lint`, `:domain:depthSweep`. The old task list (`ktlintCheck detekt :domain:test test assembleDebug`) reached **none** of the eight literal gates — the CI gate really was narrower than the local one, and naming `check` is the only change that cannot silently drift again. Keeping the sweep in the same job is right *for now*: correctness first, and a separate job is a pure wall-clock optimisation that changes no coverage. Recommend the split only if the measured figure lands above ~25 min.

**Unmeasured estimate — acceptable as written.** It is labelled "+12–20 minutes per PR, **not measured by me**", its provenance is named (the tech-lead's recorded ~11 min for `depthSweep`, which `outputs.upToDateWhen { false }` keeps permanently out of date), the method is stated, and the mitigation is pre-identified. An estimate presented as an estimate is a legitimate report artifact; a number presented as a measurement would have been a finding. Condition, non-blocking: the producer's `check` run on this branch produces the real number — record it in STATUS.md before the branch cut so the tech-lead's tier decision rests on a measurement.

### Ruling 5 (routed deviation) — TDD on `ReleaseInvocation` — ACCEPTED as proven-equivalent
The studio requires failing-test-first for core logic, and this is a build gate rather than game-core, but the rule's *purpose* is the thing to test: does the suite fail when the behaviour is wrong? I mutated the source myself three ways:

| Mutation | Result |
|---|---|
| `PACKAGING_VERBS` minus `"install"` | 1 failure — `installAndPublishShapedNamesAreReleaseInvocations` |
| `SHIPPED_SOURCE_SET_NAMES` minus `"debug"` | 2 failures — `everyShippedSourceSetIsScanned…`, `theGateFailureMessagesCanNameTheRoots…` |
| revert to `return name.contains(RELEASE_FRAGMENT)` | 3 failures — `oneReleasePackagingNameAmongOthers…`, `analysisTasksNamedAfterTheReleaseVariant…`, `aProjectQualifiedNameIsMatchedOnItsLastSegment` |

The suite is load-bearing in exactly the dimension the deviation put at risk. Rework would re-derive a 69-line predicate whose behaviour the ruling dictated verbatim, and would buy no evidence the mutation runs do not already provide. Accepted, one-off, declared; not a precedent for `:domain` logic. `SourceSetPolicy` was done test-first with a genuine red and needs no ruling.

## Findings

**F-1 [Low] `build-logic/src/main/kotlin/ReleaseInvocation.kt:19` — the KDoc overclaims in both directions.**
"a packaging task outside AGP's verbs is not a thing that exists" is false twice over in this very build:
- *Under-inclusive:* `signReleaseBundle` exists in the release graph and is the task that signs the AAB; it starts with no packaging verb and is not matched.
- *Over-inclusive:* `packageReleaseResources` matches and packages nothing shippable. Proven: `env -u PLAY_UPLOAD_KEYSTORE ./gradlew -m :app:packageReleaseResources` → configuration fails demanding all four `PLAY_UPLOAD_*` variables. `bundleReleaseClassesToCompileJar` is the same class of false positive.

Neither breaks the gate — the execution-time backstop covers the first and nobody invokes the second by name — but a KDoc that a future reader will rely on should not state a false universal. Fix: narrow the sentence to what is true ("every task that *produces or installs* a shippable release artifact starts with one of these verbs; some internal AGP tasks share the prefix and are harmlessly caught").

**F-2 [Medium] `build-logic/src/main/kotlin/SourceSetPolicy.kt:54-67` vs `:42` — the tripwire pre-authorises source sets that nothing scans.**
`KNOWN_SOURCE_SET_NAMES` admits `androidTest`, `androidTestDebug`, `androidTestRelease`, `screenshotTest`, `screenshotTestDebug`, `testRelease` — none of which exist in this repo. Excluding them from `shippedSourceRoots` is correct (they are test code). But `VARIANT_SOURCE_SET_NAMES` (`:42`) lists only `debug`, `release`, `testDebug`, and the object's own KDoc says the detekt widening covers build-type source sets "test source sets INCLUDED". So the day someone adds `src/androidTest`, its Kotlin is seen by **no detekt task** and the tripwire stays silent, because the name was pre-classified. ktlint still covers it (`ktlintAndroidTestSourceSetCheck` exists in `:app`, SKIPPED only for want of source), which is why this is Medium and not High. In a diff whose entire purpose is closing scope holes, this is the one scope hole it opens. Fix, either: add `androidTest`/`screenshotTest` (and the `*Debug`/`*Release` spellings) to `VARIANT_SOURCE_SET_NAMES`, or drop them from `KNOWN_SOURCE_SET_NAMES` so their first appearance trips the wire and forces the decision. Route to tech-lead; not a merge blocker.

**F-3 [Low] `app/build.gradle.kts:255, 259, 269` — task-name literals inline.**
`"packageRelease"`, `"packageReleaseBundle"`, `"assembleRelease"`, `"bundleRelease"`, `"validateSigningRelease"` are magic strings in the same file whose *predicate* was extracted to `build-logic` precisely so its names could be pinned by a test. The hook names are the other half of the same contract and are not pinned anywhere; a future AGP rename makes `matching {}` silently match nothing (which the comment at `:249-254` acknowledges as the reason the lifecycle hook is kept). Suggest hoisting them onto `ReleaseInvocation` as named constants so `ReleaseInvocationTest` pins both halves. Non-blocking: they are matched, not constructed, and a wrong name degrades to the lifecycle hook rather than to no check.

**F-4 [Low] `build-logic/build.gradle.kts:1-3` — `build-logic` applies only `kotlin-dsl`; its Kotlin is linted by nothing.**
No ktlint, no detekt. This diff adds 199 lines of production Kotlin (`ReleaseInvocation.kt`, `SourceSetPolicy.kt`) and 203 lines of test Kotlin into that unlinted area, and ruling 4's new CI step runs `test` there but not style. Pre-existing gap, enlarged here, and squarely in scope for a task about gates being narrower than they look. Route to tech-lead as a follow-up; not attributable to this developer and not a merge blocker.

**F-5 [Low] `.planning/reports/phase-14/android-game-dev-gate-scope.md:47` — the publish gap's stated reason is wrong.**
The report says `./gradlew -m :app:publishReleaseBundle` was "blocked by the studio's own publish gate hook". The task does not exist in this build at all (no publisher plugin, no `publishing {}` block, no `publish*` task in the release graph), so the invocation could never have produced evidence regardless of the hook. The developer's *conclusion* — that the predicate-level test is the proof — is correct and I accept it; only the reason is misattributed. Worth correcting so a future reader does not record "the publish hook blocks release-gate testing" as a studio fact.

**F-6 [Info] Pre-existing, not introduced:** Gradle's camelCase task abbreviation (`./gradlew aR` → `assembleRelease`) is not matched, because `startParameter.taskNames` carries the raw string. The old `contains("elease")` missed it too. Covered at execution time by the `packageRelease` dependency. No action.

## Suppressions
No new `@Suppress`, baseline entry, `@Ignore`, disabled test, `ignoreFailures`, or `!!` in the diff (`git diff develop..HEAD | grep -E '^\+.*(@Suppress|@Ignore|baseline|ignoreFailures|isEnabled = false)'` → only the developer's own report prose). **Nothing to approve or reject.**
Test inventory: 54 build-logic tests, `skipped="0"` in all six `TEST-*.xml` result files.

## Dependencies
No dependency change: `gradle/libs.versions.toml`, `settings.gradle.kts`, `gradle.properties` and every `build.gradle.kts` except `app/build.gradle.kts` are untouched, and the `app` change adds no artifact. **No `THIRD_PARTY.md` row is owed.** No secrets in the diff; the four `PLAY_UPLOAD_*` values remain environment-only and `local.properties` stays untracked.

## Checks
- `./gradlew -p build-logic test --rerun-tasks --max-workers=2` — **pass** (`BUILD SUCCESSFUL in 3s`; 54 tests, 0 skipped, 0 failures across 6 classes)
- `./gradlew ktlintCheck detekt --max-workers=2` — **pass** (`BUILD SUCCESSFUL in 676ms`; `:app:ktlintKotlinScriptCheck`, `:app:ktlintMainSourceSetCheck`, `:app:detekt` all up-to-date against HEAD)
- mutation: `PACKAGING_VERBS` − `"install"`, `SHIPPED_SOURCE_SET_NAMES` − `"debug"` — **pass (tests discriminate)** (`54 tests completed, 3 failed`, the 3 expected)
- mutation: `isReleasePackagingTask` reverted to `contains(RELEASE_FRAGMENT)` — **pass (tests discriminate)** (`54 tests completed, 3 failed`, reproducing the developer's claim exactly)
- `./gradlew :app:adIdLiteralCheck :app:gamesIdLiteralCheck :app:audioFocusLiteralCheck` with literals planted in `src/{main,release,debug}` — **pass (all three gates FAILED as required)**, offenders listed for all three source sets plus `src/main/res`
- same three gates with literals in `src/{test,testDebug}` only — **pass** (`BUILD SUCCESSFUL in 361ms`; test source sets correctly exempt)
- `./gradlew :app:tasks` with `app/src/staging/java` present — **pass (tripwire FAILED configuration as required)**: `Unclassified source set(s) in :app: src/staging`
- `./gradlew :domain:tasks` with `domain/src/flavourish` present — **pass (tripwire FAILED as required)**: `Unclassified source set(s) in :domain: src/flavourish`
- `./gradlew -m :app:bundleRelease` (dummy creds) — **pass**, order 83/84/85 verify → 86 `packageReleaseBundle`
- `./gradlew -m :app:assembleRelease` (dummy creds) — **pass**, order 84/85/86/87 verify+validate → 90 `packageRelease`
- `./gradlew :app:packageRelease` executed with bogus keystore — **pass (failed in the right place)**: `:app:verifyReleaseSigning FAILED`, `packageRelease` never ran, `app/build/outputs` contains no `.apk`/`.aab`
- `env -u PLAY_UPLOAD_* ./gradlew -m :app:lintRelease :app:detektRelease` — **pass** (`BUILD SUCCESSFUL in 467ms`; no credentials demanded — ruling 1's objective)
- `env -u PLAY_UPLOAD_KEYSTORE ./gradlew -m :app:packageReleaseResources` — **fail as designed/F-1**: config error `Missing: PLAY_UPLOAD_KEYSTORE …` (residual false positive)
- `./gradlew -m check` — **pass**, 130 tasks including all 8 literal gates, `:app:lint`, `:domain:depthSweep` (evidence the new CI line reaches what the old task list missed)
- `./gradlew check` (full) — **not run by me**, per task instruction; owned by the producer
- `git status --porcelain` after all probes — **pass** (empty; source sets restored, branch `feature/phase-14-gate-scope` unchanged, nothing committed)

## Open issues / risks
- F-2 is the only finding I would want addressed before the release branch is cut, and it is a two-line change in `SourceSetPolicy.kt`.
- The measured `check` wall clock from the producer's run should be recorded in STATUS.md; the +12–20 min estimate stands until then.
- `build-logic` Kotlin remains outside ktlint/detekt (F-4) — a standing gap for the tech-lead, not this branch.

## Handoff
Next: **tech-lead** — PASS, branch is mergeable. Three decisions are yours, none blocking: (1) F-2, whether `androidTest*`/`screenshotTest*` join `VARIANT_SOURCE_SET_NAMES` or leave `KNOWN_SOURCE_SET_NAMES`; (2) F-4, whether `build-logic` gets ktlint/detekt as its own task; (3) whether `:domain:depthSweep` moves to a parallel CI job once the producer's measured `check` time is in. F-1, F-3 and F-5 are doc/comment corrections the android-game-dev can fold into the next commit on this branch without a re-review.
