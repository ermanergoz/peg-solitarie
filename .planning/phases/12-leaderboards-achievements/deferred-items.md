# Phase 12 — Deferred Items

Out-of-scope discoveries logged during execution. Not fixed by the discovering plan
(SCOPE BOUNDARY rule: only issues directly caused by the current task's changes are
auto-fixed).

---

## `:domain:depthSweep` fails with `java.io.EOFException`

- **Discovered during:** plan 12-03, plan-level verification (`./gradlew check --offline`)
- **Symptom:** `:domain:depthSweep` FAILED after ~550ms with `java.io.EOFException`. The
  Gradle test-worker JVM dies before the sweep runs; the failure is not an assertion.
- **Why it is out of scope for 12-03:** `depthSweep` is registered in `domain/build.gradle.kts`
  and runs `:domain`'s own test classes on `:domain`'s runtime classpath — it cannot see
  `:app` at all. Plan 12-03 changed **zero** `:domain` files (verified with
  `git diff --name-only <base> HEAD`), so nothing this plan did is reachable from the
  failing task. Reproduced in isolation via `./gradlew :domain:depthSweep --offline`.
- **Likely cause:** environmental — a dead/contended Gradle test-worker JVM, plausibly from
  several parallel worktree agents running Gradle against the same daemon set concurrently.
  Worth re-checking on a quiet machine before treating it as a real regression.
- **Everything 12-03 owns is green:** `:app:compileDebugKotlin`, the plan's five test
  classes, `:app:detekt`, `:app:ktlintCheck`, `detekt`, `ktlintCheck` and
  `PlatformBoundaryTest` / `PlatformBoundaryGateTest` all pass.
