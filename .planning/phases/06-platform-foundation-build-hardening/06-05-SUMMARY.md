---
phase: 06-platform-foundation-build-hardening
plan: 05
subsystem: platform/activity
tags: [PLAT-01, PLAT-04, seam, archunit, hilt-scoping, weak-reference]
requires:
  - "06-01 — NoOpAdGateway returning RewardOutcome.Granted (named by the corrected PlatformModule KDoc)"
provides:
  - "ForegroundActivityProvider — the platform seam for foreground-Activity acquisition (dependency root of ads, consent and Play Games)"
  - "DefaultForegroundActivityProvider — @Singleton, WeakReference-held, scope-free implementation"
  - "App.onCreate registration through the interface, plus the third PlatformModule binding"
  - "PlatformBoundaryRules — the single definition of the three boundary rules, reused by 06-09's negative gate"
  - "PlatformBoundaryTest — the production containment gate wired into check"
  - "libs.errorprone.annotations — catalog entry unblocking Dagger members-injector codegen"
affects:
  - "06-09 — references PlatformBoundaryRules from PlatformBoundaryGateTest and confirms assumption A5"
  - "Phase 9 (real rewarded ads / UMP consent) — awaitActivity() is how the real gateway gets its Activity"
  - "Phase 11 (Play Games) — same seam, same timeout obligation"
tech-stack:
  added:
    - "com.google.errorprone:error_prone_annotations 2.47.0 (compileOnly)"
  patterns:
    - "Second @Singleton-on-the-class in the repo, after data/ChallengeProgress — annotated because it holds state"
    - "Suspend-until-available derived with mapNotNull{}.first() instead of stateIn, so no CoroutineScope is injected"
    - "Shared internal object holding ArchRule values, so a production gate and a negative gate cannot drift"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProvider.kt
    - app/src/main/java/com/app/pegsolitaire/platform/activity/DefaultForegroundActivityProvider.kt
    - app/src/test/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProviderTest.kt
    - app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt
    - app/src/test/java/com/app/pegsolitaire/PlatformBoundaryTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/App.kt
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
    - app/build.gradle.kts
    - gradle/libs.versions.toml
decisions:
  - "Impl class carries @Singleton and App injects the interface — avoids the second unscoped instance whose symptom is awaitActivity() hanging forever (Pitfall 6, T-06-15)"
  - "onActivityPaused nulls the reference deliberately; callers capture the Activity before show()"
  - "Both clears are identity-checked, so an A-to-B transition cannot evict the live Activity"
  - "No CoroutineScope injected and no stateIn — mapNotNull{}.first() needs neither"
  - "Activity-seam rule exempts App, App_MembersInjector and PlatformModule by exact name, not by package"
  - "Rule 1 (SDK containment) passes vacuously until 06-09 puts gms on the graph — T-06-18 transferred there"
  - "error_prone_annotations added compileOnly: Dagger 2.60 no longer exports it, and the generated members-injector for App's @Inject field needs it at javac time"
metrics:
  duration: "~55 min"
  completed: 2026-08-27
  tasks: 3
  commits: 5
  tests: "317 JVM tests green (309 baseline + 8 new), 0 failures"
---

# Phase 6 Plan 05: ForegroundActivityProvider Seam and Platform Boundary Gate Summary

Built the `ForegroundActivityProvider` bridge (PLAT-01) — the dependency root of the whole v1.1 milestone, needed by ads, consent and Play Games alike — as a `@Singleton` that holds the resumed Activity behind a `WeakReference`, and stood up the `PlatformBoundaryTest` gate (PLAT-04) that keeps it, and the future Play SDKs, confined to `com.app.pegsolitaire.platform..`.

## What Was Built

**`ForegroundActivityProvider`** (new, `platform/activity/`) — the seam, interface-only in its own file, mirroring the `AdGateway` / `NoOpAdGateway` split. Three members: `current`, `suspend fun awaitActivity()`, `fun attachTo(application)`. Its KDoc carries four obligations that would otherwise be rediscovered painfully in Phase 9:

- The type is reachable only from `..platform..`, and `PlatformBoundaryTest` fails the build otherwise.
- `AdGateway` and `GamesService` expose no Activity in any signature, so nothing outside platform can obtain one transitively.
- `awaitActivity()` callers **must** apply their own timeout — a backgrounded app may never resume, and a forever-waiting ad request leaves the assist control spinning.
- `current` is deliberately `null` while a fullscreen ad is on screen, because showing an ad pauses the host Activity. This is documented as intentional precisely so Phase 9 does not "fix" it by holding a strong reference across pause, which would reintroduce the configuration-change leak the seam exists to prevent.

**`DefaultForegroundActivityProvider`** — `@Singleton class … @Inject constructor()` implementing both the seam and `Application.ActivityLifecycleCallbacks`. State is a `MutableStateFlow<WeakReference<Activity>?>`; `awaitActivity()` is `resumed.mapNotNull { it?.get() }.first()`. Per the plan's deliberate divergence from the milestone sketch, **no `@ApplicationScope CoroutineScope` is injected and no `stateIn` is used** — the operator chain needs neither a scope nor eager sharing, so only `ChallengeProgress`'s `@Singleton` *shell* was copied, not its body.

Both clears are identity comparisons (`current === activity`). This is the non-obvious part: on an A→B transition B resumes *before* A pauses, so an unchecked clear would evict the live Activity and leave the platform layer blind. There is a dedicated test for exactly that.

The class KDoc records the Hilt scoping contrast explicitly — this class carries `@Singleton` while `NoOpAdGateway` / `NoOpGamesService` deliberately do not — so a future reader does not "normalise" it back to match the stubs.

**`App`** — gains `@Inject lateinit var activityProvider: ForegroundActivityProvider` (the **interface**; the concrete type is never named in this file) and an `onCreate` that calls `super.onCreate()` then `attachTo(this)`. The KDoc explains why that ordering is not optional (Hilt performs `Application` field injection as part of `super.onCreate()`), why the interface rather than the impl (`@Binds @Singleton` scopes the binding of the *interface*, so naming the concrete type yields a second unscoped instance that never sees a lifecycle event — T-06-15), and that `App` is the class exempted by the boundary rule, so the exemption is discoverable from both ends.

**`PlatformModule`** — third `@Binds @Singleton`. Both stale KDoc claims were corrected: "these two bindings are the *only* Slice-2 swap points" became an explicit two-kinds split (the two stubs are swap points; the Activity provider is **permanent** — it is what makes the swap possible, not something the swap replaces), and "the ad stub grants (`true`)" now names `RewardOutcome.Granted`. A new paragraph records the Pitfall 8 constraint for Phase 9: an SDK-typed `@Provides` must not go in `di/`, because that package is outside `..platform..` and the generated component references every binding's types, so the violation would surface naming `DaggerApp_HiltComponents_SingletonC` rather than the file that caused it. SDK-typed modules belong at `platform/*/di/`.

**`PlatformBoundaryRules`** — an `internal object` in `app/src/test` holding the three `ArchRule` values, following `ResumeSnapshotFormat`'s shared-contract-object precedent including its contract warning. Its KDoc states that both the production gate and 06-09's negative gate reference these same objects, and why that matters: two copies of a rule is how a gate quietly stops matching what it claims to check. All three rules match by **name**, never by assignability — `android.app.Activity` and the Play SDK types are outside the `@AnalyzeClasses` scope, so ArchUnit resolves them to stubs and an assignability predicate would silently match nothing.

**`PlatformBoundaryTest`** — root package, `@AnalyzeClasses(packages = ["com.app.pegsolitaire"], importOptions = [ImportOption.DoNotIncludeTests::class])`, three `@ArchTest` fields delegating to the shared object. No rule body is duplicated in the test. `DomainPurityTest` was left byte-identical, as required: its `com.app.pegsolitaire.domain` scope would make a "nothing outside platform" rule vacuous.

## The Gate Bit on Its First Run

Worth recording, because it is the most useful thing this plan produced. `PlatformBoundaryTest` **failed on first execution** with five violations of the Activity-containment rule — a genuine RED, not a vacuous pass:

- 4× `com.app.pegsolitaire.App_MembersInjector` (constructor, field, `create`, `injectActivityProvider`) — Dagger's generated field-injector for `App`'s `@Inject` field.
- 1× `com.app.pegsolitaire.di.PlatformModule.bindForegroundActivityProvider` — the `@Binds` method must name its own return type, and `di/` is outside `..platform..`.

The planner's single `haveNameNotMatching(".*\\.App")` exemption was too narrow: it covers `App` but not Dagger's generated companion for it, nor the binding declaration that *creates* the seam. All five are the same sanctioned registration path seen from three angles, and none is a real leak. The rule now carries three exact-name exemptions (`.*\.App`, `.*\.App_MembersInjector`, `.*\.PlatformModule`), each documented individually in the rule's KDoc.

Two properties were preserved deliberately when widening: the exemptions are matched by **exact name, never by package** (exempting `..di..` wholesale would have opened a much larger hole), and 06-09's `archfixture` violator matches none of them, so the negative gate will still catch it.

Rule 1 (SDK containment) passes vacuously today — expected and called out in the plan; 06-09 is what gives it teeth (T-06-18, transferred).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Dagger members-injector codegen failed to compile**
- **Found during:** Task 2, at `./gradlew check`
- **Issue:** `:app:hiltJavaCompileDebug` failed with `package com.google.errorprone.annotations does not exist` / `cannot find symbol: CanIgnoreReturnValue` in the generated `DaggerApp_HiltComponents_SingletonC`. Verified by `dependencyInsight` that `error_prone_annotations` is genuinely absent from `debugCompileClasspath`: Dagger 2.60 no longer exports it transitively, yet its generated members-injectors still emit `@CanIgnoreReturnValue`. `App` gaining the first `@Inject` field in the app module is what triggered the first such codegen — this was directly caused by this task, not pre-existing.
- **Fix:** Added `errorproneAnnotations = "2.47.0"` and an `errorprone-annotations` library to the version catalog (per the mandatory-catalog rule — no hand-pinned literal in the build file), plus `compileOnly(libs.errorprone.annotations)` in `app/build.gradle.kts`. `compileOnly` is sufficient because these are CLASS-retention annotations needed only at javac time; nothing is added to the APK. The artifact was already present in the local Gradle cache (pulled transitively by test deps), so no new network install occurred.
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Commit:** bfa498b
- **Note for the orchestrator:** plan 06-07 also owns `app/build.gradle.kts`, but edits its `android {}` / `buildTypes` block. This edit is inside `dependencies {}`, so a textual conflict is unlikely.

**2. [Rule 1 - Bug] Activity-containment rule exemption was too narrow**
- **Found during:** Task 3, first run of the new gate
- **Issue:** The plan's rule 2 exempted only `.*\.App`, so the gate failed on `App_MembersInjector` (Dagger codegen for the sanctioned injection) and on `PlatformModule.bindForegroundActivityProvider` (the binding declaration itself). As written the rule would have made the seam unbindable.
- **Fix:** Two further exact-name exemptions, each documented in the rule KDoc with why it is codegen/wiring for the one sanctioned path rather than a second decision. Package-level exemption was explicitly rejected as too broad.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt`
- **Commit:** d8afa54

**3. [Rule 3 - Blocking] ktlint and detekt formatting**
- **Found during:** Tasks 2 and 3, at `./gradlew check`
- **Issue:** `ktlint_official` requires each super type on its own line when a class declares more than one (`DefaultForegroundActivityProvider` implements two); detekt flagged one `MaxLineLength` in `PlatformBoundaryTest`.
- **Fix:** `ktlintMainSourceSetFormat` (verified via `git status` to have touched only this plan's own files), and a manual line wrap.
- **Commits:** 49a3466, d8afa54

### Adjustments to Satisfy Mechanical Acceptance Criteria

Four acceptance criteria are `grep` assertions that do not distinguish code from prose, and my first-draft KDoc tripped them by *naming* the thing it was warning against — e.g. `! grep -q "CoroutineScope\|stateIn"` failed on a KDoc sentence saying no `CoroutineScope` is injected. In each case the prose was reworded to convey the identical meaning without the literal token, rather than weakening the documentation or the criterion: `CoroutineScope`/`stateIn` → "app-lifetime coroutine scope … eager sharing"; `=== activity` in prose → "reference equality, never `equals`"; `Robolectric` → "no Android runtime"; `areAssignableTo` → "assignability"; `@Binds` in the module KDoc → "abstract interface-to-impl binding shape" (so the `@Binds` count of 3 measures actual bindings). All criteria now pass as literally written.

## TDD Gate Compliance

Task 1 followed a full RED → GREEN cycle: `test(06-05): add failing tests` (00782de) landed the five tests against a deliberately inert skeleton and 3 of 5 failed; `feat(06-05): implement …` (3d2021f) made all five pass. The two clear-behaviour tests passed against the skeleton because a permanently-`null` `current` is trivially "cleared" — an artifact of the skeleton, not a weak test; both fail against a naive non-identity-checked implementation, which is what they exist to catch.

Task 3's gate produced a genuine RED on first run (five real violations, above) and a GREEN after the exemption fix, but both are in one commit because the files are test-only and the violation was in the rule definition itself rather than in production code.

## Verification

- `./gradlew :app:testDebugUnitTest --tests "*ForegroundActivityProviderTest"` — 5 tests green
- `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest" --tests "*DomainPurityTest"` — 4 rules green
- `./gradlew check` — **green**, including detekt, ktlint, Android lint and every Robolectric UI test
- **317 JVM tests, 0 failures** (243 `:app` + 74 `:domain`) against a 309 baseline: +5 provider tests, +3 boundary rules
- `git diff --stat HEAD app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` — zero changed lines
- `STATE.md` and `ROADMAP.md` untouched, per worktree instructions

## Known Stubs

None. Rule 1 of the boundary gate passes vacuously by design, which is documented in both the rule KDoc and the test KDoc and is explicitly deferred to plan 06-09 (T-06-18, disposition `transfer`) — not a stub in the code.

## Threat Flags

None. No new network endpoint, auth path, file access or schema change was introduced. The two mitigations this plan owns (T-06-14 weak-hold, T-06-15 Hilt scoping) are implemented and test-covered; T-06-16 is enforced by rules 2 and 3, which are live and were demonstrated to fail on a real violation.

## Self-Check: PASSED

All five created files exist on disk. All five commits (00782de, 3d2021f, bfa498b, 49a3466, d8afa54) are present in `git log`.
