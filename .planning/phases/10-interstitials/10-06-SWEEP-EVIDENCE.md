# Phase 10 Plan 06 — Task 1 evidence: the full gate and the five cross-cutting invariants

Recorded verbatim so a later change can be re-checked against it rather than against a claim.
All commands were run from the repository root at commit `4c9557b`
(`docs(phase-10): update tracking after wave 4`), the tip of `feature/phase-09-consent-rewarded-ads`
after wave 4 merged, on 2026-09-03.

Nothing in this task required a source change: the gate was already green and every invariant
already held. `AppForegroundTracker.kt` is deliberately untouched here — task 2's device run is
what resolves its `[ASSUMED]` A1 marker.

---

## The build gate

### `./gradlew check`

```
> Task :domain:depthSweep

DepthSweepTest > everyPositionInEveryBoardsCoveredFrontierIsAnsweredTruthfullyWithinTheDeadline STANDARD_OUT
    english33 k=4 examined=373 (canonical=51) book-served=369 authoritative-dead=4 solver-fallback=0 (of which answered=0) maxPerCall=20ms
    european37 k=4 examined=609 (canonical=312) book-served=609 authoritative-dead=0 solver-fallback=0 (of which answered=0) maxPerCall=3ms
    asymmetric39 k=4 examined=461 (canonical=233) book-served=461 authoritative-dead=0 solver-fallback=0 (of which answered=0) maxPerCall=2ms
    diamond41 k=4 examined=785 (canonical=107) book-served=345 authoritative-dead=0 solver-fallback=440 (of which answered=0) maxPerCall=1501ms
    wiegleb45 k=3 examined=101 (canonical=16) book-served=97 authoritative-dead=0 solver-fallback=4 (of which answered=0) maxPerCall=1500ms

> Task :domain:detekt
> Task :domain:dpSpLiteralCheck
> Task :domain:test FROM-CACHE
> Task :domain:ktlintCheck
> Task :domain:check

BUILD SUCCESSFUL in 11m 16s
90 actionable tasks: 44 executed, 46 from cache
```

**Exit 0.** The DepthSweep numbers are byte-identical to the residual STATE.md already carries
from DEBT-01 (440 Diamond-41 and 4 Wiegleb-45 positions outside their books) — this run neither
widened nor narrowed that gap.

### Which tasks `check` actually pulls in

The `check` lifecycle's composition was read off the task graph rather than assumed, because
invariant 2 is the claim "`adIdLiteralCheck` ran as part of `check`":

```
$ ./gradlew check --dry-run | grep -iE "LiteralCheck|detekt|ktlintCheck|:check"
:check SKIPPED
:app:adIdLiteralCheck SKIPPED
:app:audioFocusLiteralCheck SKIPPED
:app:detekt SKIPPED
:app:dpSpLiteralCheck SKIPPED
:app:ktlintCheck SKIPPED
:app:check SKIPPED
:domain:adIdLiteralCheck SKIPPED
:domain:audioFocusLiteralCheck SKIPPED
:domain:detekt SKIPPED
:domain:dpSpLiteralCheck SKIPPED
:domain:ktlintCheck SKIPPED
:domain:check SKIPPED
```

(`SKIPPED` is `--dry-run`'s marker for "would run", not a skip.) All five gates named in
`standards.gradle.kts:176-178` are wired into `check` for **both** modules, and both modules'
test tasks (`:app:testDebugUnitTest`, `:domain:test`) are in the same graph.

### `./gradlew :app:assembleDebug`

```
> Task :app:packageDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 6s
55 actionable tasks: 13 executed, 4 from cache, 38 up-to-date
```

**Exit 0.** APK at `app/build/outputs/apk/debug/app-debug.apk`, 20,301,522 bytes.

The generated debug `BuildConfig` confirms the UAT will serve Google's test inventory and not real
inventory — the T-10-04 mitigation, verified rather than asserted:

```
$ grep -n "AD_UNIT_ID\|ADMOB_APP_ID\|APPLICATION_ID" app/build/generated/source/buildConfig/debug/com/app/pegsolitaire/BuildConfig.java
8:  public static final String APPLICATION_ID = "com.erman.pegsolitarie";
13:  public static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
15:  public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
17:  public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
```

`ca-app-pub-3940256099942544/1033173712` is exactly the interstitial test unit the plan's
`<interfaces>` block names.

---

## Invariant 1 — cadence values live in exactly one file

**Result: HELD, by intent; the acceptance criterion's literal grep does not express it.**

The criterion as written expects one path:

```
$ grep -rln '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000' app/src/main domain/src/main
app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt
domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt
domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt
```

Three paths, not one. The pattern conflates two different things: the constants' **values** and the
constants' **names**. A file that reads `InterstitialCadenceParams.MAX_PER_SESSION` — which is
precisely the compliant thing to do, and what the invariant wants — necessarily contains the string
`MAX_PER_SESSION`. Under this grep the invariant can only pass if nothing ever references the params
object, which would defeat its purpose.

The invariant's actual claim was therefore checked directly. Every match outside the params file:

```
$ grep -rn '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000\|30 \* 60' app/src/main domain/src/main \
    | grep -v '^domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt:'
app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt:161:            sessionCount.update { (it + 1).coerceAtMost(InterstitialCadenceParams.MAX_PER_SESSION) }
domain/.../InterstitialCadenceState.kt:35: *   [InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS] (D-01, D-07).
domain/.../InterstitialCadenceState.kt:39: *   [InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS] (D-03).
domain/.../InterstitialCadenceState.kt:73:            lifetimeCompletedLevels >= InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS
domain/.../InterstitialCadenceState.kt:76:    private fun underSessionCap(): Boolean = interstitialsThisSession < InterstitialCadenceParams.MAX_PER_SESSION
domain/.../InterstitialCadenceState.kt:80:        winsSinceLastInterstitial >= InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS
domain/.../InterstitialCadenceState.kt:120:                    .coerceAtMost(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS),
domain/.../InterstitialCadenceState.kt:123:                    .coerceAtMost(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS),
domain/.../InterstitialCadenceState.kt:144:                    .coerceAtMost(InterstitialCadenceParams.MAX_PER_SESSION),
domain/.../InterstitialCadenceState.kt:234:                        .orZeroClampedTo(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS),
domain/.../InterstitialCadenceState.kt:238:                        .orZeroClampedTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS),
```

Every one is a **qualified reference** `InterstitialCadenceParams.<NAME>`. Filtering those out
leaves nothing:

```
$ grep -rn '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000\|30 \* 60' app/src/main domain/src/main \
    | grep -v '^domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt:' \
    | grep -v 'InterstitialCadenceParams\.[A-Z_]'
(no output)
```

So **no cadence value is duplicated anywhere**; every consumer reads it through the params object.
The five literal declarations are all in `InterstitialCadenceParams.kt` and nowhere else:

```
InterstitialCadenceParams.kt:30:    const val WINS_BETWEEN_INTERSTITIALS = 3
InterstitialCadenceParams.kt:39:    const val MIN_GAP_MS = 90_000L
InterstitialCadenceParams.kt:47:    const val SUPPRESSED_FIRST_LEVELS = 5
InterstitialCadenceParams.kt:50:    const val MAX_PER_SESSION = 4
InterstitialCadenceParams.kt:63:     * standalone `1_800_000L`, per `ScoreParams.TWO_STAR_MAX_PEGS`' discipline: the stated 30
```

Line 63 is KDoc explaining why `SESSION_GAP_MS` is *derived* from `SESSION_GAP_MINUTES` rather than
written as a standalone `1_800_000L` — the constant that mentions the literal is the one arguing
against writing it.

The plan singles out `AdConstants.kt` as the specific file whose own KDoc forbids these values:

```
$ grep -n '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000\|30 \* 60' \
    app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
(no output, exit 1)
```

**Clean.** No fix was required.

### A re-runnable form of this invariant

For a later change, the grep worth re-running is the value-only one, not the criterion's:

```bash
grep -rn '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000\|30 \* 60' app/src/main domain/src/main \
  | grep -v '^domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt:' \
  | grep -v 'InterstitialCadenceParams\.[A-Z_]'
```

Empty output is the invariant holding.

---

## Invariant 2 — no ad-unit literal anywhere under `src/main`

**Result: HELD.** `:app:adIdLiteralCheck` and `:domain:adIdLiteralCheck` are both in the `check`
graph (see the `--dry-run` listing above) and both ran inside the green `./gradlew check`. The gate
is `standards.gradle.kts:100-132`: it walks `src/main/{java,kotlin,res}`, skips comment lines, and
throws on any `ca-app-pub-` substring. Its scope is `src/main` only, by design — `src/test`
legitimately asserts on the Google test-publisher prefix.

The only place the test IDs exist is the generated `BuildConfig` (quoted above), produced from
`buildConfigField` declarations in `app/build.gradle.kts:185-187` — build script, not `src/main`.

---

## Invariant 3 — nothing in the ad or cadence path logs

**Result: HELD.**

```
$ grep -rn 'android.util.Log\|Timber\|println(' \
    app/src/main/java/com/app/pegsolitaire/platform/ads/ \
    app/src/main/java/com/app/pegsolitaire/platform/lifecycle/ \
    app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt \
    app/src/main/java/com/app/pegsolitaire/ui/results/
(no output, exit 1)
```

Per-file, for the six new/changed ad-and-cadence files the plan names plus `ResultsViewModel`:

```
$ grep -c 'android.util.Log\|Timber\|println(' <each>
platform/ads/InterstitialAdSlot.kt        : 0
platform/ads/AdGateway.kt                 : 0
platform/ads/AdMobAdGateway.kt            : 0
platform/ads/NoOpAdGateway.kt             : 0
data/InterstitialCadenceStore.kt          : 0
platform/lifecycle/AppForegroundTracker.kt: 0
ui/results/ResultsViewModel.kt            : 0
```

Note the directory sweep is **wider** than the seven named files: it covers all of
`platform/ads/`, all of `platform/lifecycle/` and all of `ui/results/` — so `AdConstants`,
`AdIds`, `AdQuiescenceScrim`, `ResultsUiState`, `ResultsRoute` and `ResultsScreen` are included
and also clean. No ad response info, consent status or cadence identifier can reach logcat
(ASVS V7).

---

## Invariant 4 — the cadence policy imports no Android type

**Result: HELD.**

```
$ grep -rn 'import android\|import androidx' domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/
(no output, exit 1)
```

`DomainPurityTest` (the ArchUnit gate, bytecode-based rather than textual) ran inside
`:app:testDebugUnitTest` as part of the green `check`. The grep is the cheap textual confirmation
of the same claim; `SystemClock` is mechanically unreachable, which is why `mayShow` takes `nowMs`
as a parameter.

---

## Invariant 5 — no `pegsRemaining == 1` test entered the interstitial gate

**Result: HELD.** This is the Diamond-41 trap (RESEARCH Pitfall 1), checked at package level.

```
$ grep -rn '== 1\|!= 1\|<= 1\|PEGS_REMAINING_SOLVED' app/src/main/java/com/app/pegsolitaire/ui/results/
ResultsViewModel.kt:312:            if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return
ResultsViewModel.kt:373:            const val PEGS_REMAINING_SOLVED = 1
ResultsViewModel.kt:403:                    challengeLevel != null && pegsRemaining != PEGS_REMAINING_SOLVED ->
ResultsViewModel.kt:428:             *  - It must **never** be written as a `pegsRemaining == 1` /
ResultsViewModel.kt:429:             *    [PEGS_REMAINING_SOLVED] comparison. That constant is correct where it is
ResultsViewModel.kt:430:             *    already used (every Challenge footprint has `minimumPegs == 1`) and it sits
ResultsUiState.kt:86:        get() = challengeLevel != null && pegsRemaining == PEGS_REMAINING_SOLVED
ResultsUiState.kt:121:private const val PEGS_REMAINING_SOLVED = 1
```

Eight occurrences, each enumerated with the function it sits in:

| # | Location | Enclosing declaration | Kind | On the interstitial path? |
|---|----------|----------------------|------|---------------------------|
| 1 | `ResultsViewModel.kt:312` | `recordSolvedPlayOnce()` | the D-08 solve guard on the bests merge — a non-solve writes no best | **No.** Pre-existing (Phase 2); reached from `init`, never from a tap. |
| 2 | `ResultsViewModel.kt:373` | `private companion object` | the `const val PEGS_REMAINING_SOLVED = 1` declaration | **No.** A declaration, not a test. |
| 3 | `ResultsViewModel.kt:403` | `tierFor(...)` | the Challenge-stuck branch: a Challenge board not folded to one peg is `STUCK` regardless of stars | **No** — and this is the load-bearing point: it is guarded by `challengeLevel != null`, so it never applies to a Classic board, and Diamond-41 is Classic. |
| 4–6 | `ResultsViewModel.kt:428-430` | KDoc of `isInterstitialEligible` | prose naming the trap being avoided | **No.** Comment lines. Added by 10-04 on purpose: the warning has to name the wrong code to be legible. |
| 7 | `ResultsUiState.kt:86` | `ResultsUiState.showNextLevel` getter | whether the "Next level" action is offered at all (D-01) | **No.** Pre-existing (Phase 2); decides which button is drawn, not whether an ad may follow it. |
| 8 | `ResultsUiState.kt:121` | file-private | the second `const val PEGS_REMAINING_SOLVED = 1` declaration | **No.** A declaration. |

And the gate itself, `ResultsViewModel.kt:443`:

```kotlin
fun isInterstitialEligible(tier: ResultsHeadlineTier): Boolean = tier != ResultsHeadlineTier.STUCK
```

No peg comparison, no star comparison. The functions on the interstitial path —
`onNextLevelTapped`, `onCatalogTapped`, `continueWith`, `shouldShowInterstitial`,
`presentThenNavigate`, `claimWinOnce` — contribute **zero** matches to the grep above, which is the
package-level form of the claim 10-04's `aPerfectDiamond41FinishWithTwoPegsRemainingIsAdEligible`
proves at one call site.

A perfect Diamond-41 finish (three stars, **two** pegs remaining, Classic so `challengeLevel` is
`null`) therefore takes `tierFor`'s `stars >= THREE_STARS` branch to `PERFECT`, and
`PERFECT != STUCK` is ad-eligible. The player who played it flawlessly is not the one denied.

---

## Summary

| Gate / invariant | Result |
|------------------|--------|
| `./gradlew check` | PASS (exit 0, 11m 16s) |
| `./gradlew :app:assembleDebug` | PASS (exit 0) |
| 1 — cadence values in exactly one file | HELD (by intent; criterion's grep cannot express it — see above) |
| 2 — no ad-unit literal in `src/main` | HELD (`adIdLiteralCheck` confirmed in the `check` graph and green) |
| 3 — no logging in the ad/cadence path | HELD (0 matches across three whole packages) |
| 4 — no Android import in the cadence policy | HELD (0 matches; `DomainPurityTest` green) |
| 5 — no `pegsRemaining == 1` on the interstitial gate | HELD (8 occurrences enumerated, none on the path) |

No source file needed a change. `AppForegroundTracker.kt` still carries its `[ASSUMED]` A1 marker;
plan 10-06 task 2's device run is what resolves it.
