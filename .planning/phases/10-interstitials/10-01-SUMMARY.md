---
phase: 10-interstitials
plan: 01
subsystem: ads
tags: [interstitial, cadence, admob, pure-kotlin, tdd, domain, junit4, truth]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads
    provides: "AssistAllowance — the constants-object + immutable-value + validate-on-read pattern this policy clones, and the rewarded-ad presentation whose stamp D-15 makes shared"
  - phase: 06-platform-foundation-build-hardening
    provides: "D-04 (policy objects live in pure :domain) and the DomainPurityTest ArchUnit gate that mechanically enforces it"
provides:
  - "InterstitialCadenceParams — D-01..D-04 tunables as scoped KDoc'd constants, with SESSION_GAP_MS derived from minutes (D-05)"
  - "InterstitialCadenceState.mayShow(nowMs) — the single four-gate conjunction that decides whether an interstitial may show; nothing else in the phase re-decides it"
  - "The transitions recordWin / recordInterstitialShown / recordRewardedShown / startNewSession, all saturating and total"
  - "InterstitialCadenceState.isNewSession(lastForegroundAtMs, nowMs) — the D-04/D-16 session boundary as pure arithmetic"
  - "InterstitialCadenceState.fromStored(...) — validate-on-read for the D-13 persisted scalars, failing toward FEWER ads"
  - "The NEVER_SHOWN / NEVER_FOREGROUNDED sentinels the store must write and read"
affects: [10-02, 10-03, 10-04, 10-05, 10-06, interstitial-store, results-trigger, admob-gateway]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Cadence as a pure conjunction of individually named private predicates over an immutable value"
    - "Saturating counters (coerceAtMost at each counter's own threshold) as the overflow control for long-lived counters"
    - "Inverted validate-on-read: fail toward FEWER ads, the mirror of AssistAllowance.fromStoredOrFull's fail-open-to-full"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceStateTest.kt
  modified: []

key-decisions:
  - "lastFullscreenAdAtMs is ONE stamp shared by interstitials and rewarded ads (D-15), so the 90s cooldown closes the rewarded-then-interstitial adjacency without changing D-02's value"
  - "A negative clock delta OPENS the wall-clock gate but CLOSES the session boundary — one rule (no gate sticks, the cap keeps binding) seen from two sides"
  - "Both lifetime counters saturate at their own threshold rather than growing, because each is consumed only by a >= against that threshold, which makes Int overflow unreachable by construction"
  - "fromStored CLAMPS out-of-range counters down rather than rejecting them upward, the deliberate inverse of AssistAllowance's fail-open-to-full"
  - "interstitialsThisSession is deliberately not a fromStored parameter (D-13) — it is in-memory and derives from a boundary any relaunch has already crossed"

patterns-established:
  - "Four-gate conjunction: mayShow = newInstallSuppressionOver() && underSessionCap() && enoughWins() && enoughElapsed(nowMs), ordered widest-suppression-first, each clause independently proven necessary by a test"
  - "Sentinel-before-arithmetic: NEVER_SHOWN / NEVER_FOREGROUNDED are checked before any subtraction, so no delta is computed against a non-stamp"
  - "Test-asserts-the-constant: every threshold expectation is written as an InterstitialCadenceParams member; bare literals appear only as arbitrary stored fixtures"

requirements-completed: [INT-02]

# Metrics
duration: 9min
completed: 2026-09-02
---

# Phase 10 Plan 01: Interstitial Cadence Policy Summary

**A pure-Kotlin four-gate interstitial cadence policy (`mayShow` = new-install suppression AND session cap AND wins AND wall-clock gap) over an immutable state value, TDD'd against a 31-case truth table, with one fullscreen-ad stamp shared with rewarded ads (D-15) and validate-on-read that fails toward fewer ads.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-09-02T20:37:54Z
- **Completed:** 2026-09-02T20:46:43Z
- **Tasks:** 2 (RED, GREEN)
- **Files modified:** 3 created, 0 modified

## Accomplishments

- The whole phase's "may an interstitial show right now?" question is now **one conjunction over one immutable value** — `InterstitialCadenceState.mayShow(nowMs)` — with each of the four gates a separately named private predicate, so a failing test names the gate it broke rather than "the cadence". Nothing downstream re-decides it.
- **D-07's shared counter is structural, not a convention:** there is a single state value and a single policy, so no per-mode counter exists for a Classic/Challenge alternation to dodge.
- **D-15's rewarded-adjacency is closed at the field level:** `lastFullscreenAdAtMs` means "the last fullscreen ad", so `recordRewardedShown(t)` makes `mayShow(t + MIN_GAP_MS - 1)` false and `mayShow(t + MIN_GAP_MS)` true — the AdMob back-to-back pairing is unreachable, at no cost to D-02's locked 90-second value.
- **Every threat in the plan's register is closed by a test, not by a claim:** tampered counters clamp down (T-10-01), counters saturate so `Int` overflow is unreachable (T-10-05), a moved clock opens the gap gate but closes the session boundary (T-10-07), and a forged `Long.MAX_VALUE` stamp reads back as `NEVER_SHOWN` so the D-02 delta can never overflow (T-10-08).
- **The purity the ROADMAP demanded is now a build gate:** the package imports nothing from `android.`/`androidx.`, and `DomainPurityTest` passes — `SystemClock` is mechanically unreachable, which is why `nowMs` is a parameter.

## Task Commits

Each task was committed atomically:

1. **Task 1: RED — cadence constants, the state skeleton, and the full truth-table test** — `68ceab5` (test)
2. **Task 2: GREEN — implement the gates, the transitions and validate-on-read** — `bee45fe` (feat)

No REFACTOR commit: the GREEN implementation landed in the shape the plan specified (four single-expression predicates, `copy(...)` transitions, two private read-validation helpers), and `detekt`/`ktlint` passed on it, so there was nothing to clean up without churn.

## Files Created/Modified

- `domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt` — the D-01..D-04 tunables as KDoc'd `const val`s, with `SESSION_GAP_MS = SESSION_GAP_MINUTES * MILLIS_PER_MINUTE` derived per `ScoreParams.TWO_STAR_MAX_PEGS`' discipline. Its class KDoc states the remit explicitly (cadence only) and records why the values may not live in `platform/ads/AdConstants`, whose own KDoc limits it to ad *timing*.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt` — the immutable state, the four gates, the four transitions, `isNewSession`, and `fromStored`.
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceStateTest.kt` — 31 plain-JUnit4 + Truth cases: the all-pass baseline, one single-field flip per gate, the four inclusive boundaries, the clock edges, every transition, the session boundary, and the seven `fromStored` tamper/edge cases.

## Decisions Made

- **The `enoughElapsed` / `isNewSession` asymmetry is documented as one rule, not two.** A negative clock delta opens the wall-clock gate (an NTP correction must not lock a player out permanently, and a forged *future* stamp must not become an ad-suppression exploit) and closes the session boundary (so the D-04 cap keeps binding). Both resolve toward "no gate is permanently stuck, and the cap still binds"; the KDoc on each names the other so a future reader does not "fix" the inconsistency.
- **`fromStored` clamps rather than rejects.** `AssistAllowance.fromStoredOrFull` deliberately *rejects* an out-of-range stored counter in favour of the default, because clamping would state that the forged number is meaningful. Here the direction inverts: clamping down is strictly the fail-safe move (a forged `9999` can only ever buy what an honest counter could), and rejecting to the default would be the same value anyway for the wins counter while being *more* permissive for a partially-progressed lifetime counter.
- **`Long.MAX_VALUE` is excluded from the trusted stamp range, and that exclusion is what makes the overflow claim true.** With the stamp bounded to `0 until Long.MAX_VALUE`, `nowMs - lastFullscreenAdAtMs` is representable for every non-negative `nowMs`. This is stated in the KDoc as the reason for the bound, not as a side effect.
- **`recordWin` increments both counters and nothing else** — the "every win increments; the ad fires on the next eligible tap where all gates pass" reading the CONTEXT flagged as recommended-simple. The eligibility *tap* is plan 10-04's concern; this plan deliberately does not encode it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created the gitignored `local.properties` in the worktree**

- **Found during:** Task 2 (GREEN), running the `DomainPurityTest` acceptance gate
- **Issue:** `./gradlew :app:testDebugUnitTest` failed with "SDK location not found". `local.properties` is gitignored (`.gitignore:7`), so it exists in the main checkout but not in a fresh git worktree — every `:app` Gradle task is unrunnable there until it is recreated.
- **Fix:** Wrote `local.properties` containing `sdk.dir=/Users/ermanergoz/Library/Android/sdk` (the SDK location confirmed present on disk) into the worktree root.
- **Files modified:** `local.properties` (gitignored — deliberately NOT committed, and it correctly does not appear in `git status`)
- **Verification:** `./gradlew :app:testDebugUnitTest --tests '*DomainPurityTest'` then exited 0.
- **Committed in:** Not committed — the file is gitignored by design, and committing it would leak a machine-specific absolute path.

**2. [Rule 3 - Blocking] Collapsed `underSessionCap()` onto one line for ktlint**

- **Found during:** Task 2 (GREEN), running `:domain:ktlintCheck`
- **Issue:** `ktlint_official` raised "First line of body expression fits on same line as function signature" at `InterstitialCadenceState.kt:76` — the predicate had been written wrapped, matching its three siblings, but its single expression fits inside the 120-column limit.
- **Fix:** Wrote `private fun underSessionCap(): Boolean = interstitialsThisSession < InterstitialCadenceParams.MAX_PER_SESSION` on one line. The other three predicates are genuinely too long to collapse and stay wrapped.
- **Files modified:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt`
- **Verification:** `./gradlew :domain:detekt :domain:ktlintCheck` exited 0; `:domain:test` re-run green.
- **Committed in:** `bee45fe` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Neither touched the policy's behaviour — one is a worktree environment file, the other is whitespace demanded by a project style gate. No scope creep; no test was edited to fit the implementation.

## Issues Encountered

None beyond the two blocking issues above. The RED gate behaved exactly as the plan predicted: all 31 tests compiled and failed on `NotImplementedError`, never on syntax, confirming the tests were linking against a real public surface before any implementation existed.

## TDD Gate Compliance

Gate sequence verified in `git log`:

1. **RED** — `68ceab5` `test(10-01): ...` — 31 tests, `31 tests completed, 31 failed`, every failure a `kotlin.NotImplementedError`.
2. **GREEN** — `bee45fe` `feat(10-01): ...` — all 31 pass, with no test file touched (`git diff --name-only HEAD -- domain/src/test` was empty for this commit).
3. **REFACTOR** — not required; see the Task Commits note above.

No test passed unexpectedly during RED.

## Verification

| Gate | Result |
|------|--------|
| `./gradlew :domain:test --tests '*InterstitialCadenceStateTest'` | PASS — 31/31 |
| `./gradlew :domain:test` (whole module, regression check) | PASS |
| `./gradlew :app:testDebugUnitTest --tests '*DomainPurityTest'` | PASS |
| `./gradlew :domain:detekt :domain:ktlintCheck` | PASS |
| `grep -rn 'import android\|import androidx' domain/.../domain/ads/` | no match |
| `grep -rn '90_000\|30 \* 60\|1_800_000' InterstitialCadenceState.kt` | no match (every threshold read from the params object) |
| `grep -c 'TODO(' InterstitialCadenceState.kt` | 0 |
| `grep -c 'coerceAtMost(' InterstitialCadenceState.kt` | 3 |
| `grep -c '@Test'` / `grep -c 'fun '` in the test | 31 / 32 (plan floor: 24 / 24) |

## Known Stubs

None. Every declared member is implemented; no placeholder value, empty collection or "coming soon" string was introduced.

## Threat Flags

None. This plan adds no network endpoint, no auth path, no file access and no schema — its only trust boundaries are the two the plan's `<threat_model>` already registers (persisted store → cadence state, device clock → cadence gates), and both are mitigated and tested. No dependency was added.

## User Setup Required

None — no external service configuration required. The AdMob interstitial ad unit named in the phase's External Prerequisites is not touched by this plan (no SDK is referenced here at all).

## Next Phase Readiness

Ready. The downstream plans consume a settled surface:

- **10-02 / the DataStore store** persists exactly four scalars — `winsSinceLastInterstitial`, `lastFullscreenAdAtMs`, `lifetimeCompletedLevels`, `firstSessionComplete` — and reconstitutes them through `fromStored(...)`. It must write `NEVER_SHOWN` (`-1L`) for "no fullscreen ad yet" and `NEVER_FOREGROUNDED` (`-1L`) for "never foregrounded"; any other negative is treated as tampering. `interstitialsThisSession` is deliberately **not** persisted (D-13) and must be supplied from a process-lifetime counter.
- **The rewarded path (Phase 9 code)** must call `recordRewardedShown(nowMs)` for D-15 to bind. This is the one cross-phase wiring obligation this plan creates and cannot self-enforce — if it is missed, the rewarded-then-interstitial adjacency reopens silently, with every test in this plan still green.
- **The session boundary owner (`ProcessLifecycleOwner`, D-16)** calls `isNewSession(lastForegroundAtMs, nowMs)` and, when true, `startNewSession()`. Note the coupling: `startNewSession()` is also what ends D-03's first-session suppression, so whatever drives the boundary is also what lifts the new-install window.
- **The trigger (10-04)** asks `mayShow(nowMs)` and, only if an ad actually presented, calls `recordInterstitialShown(nowMs)` — never on a no-fill (D-11).

No blockers.

## Self-Check: PASSED

All three source files and this SUMMARY exist on disk; commits `68ceab5`, `bee45fe` and `cee9cfa` are present in `git log`. No claimed artifact is missing.

---
*Phase: 10-interstitials*
*Completed: 2026-09-02*
