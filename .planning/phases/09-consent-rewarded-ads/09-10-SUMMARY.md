---
phase: 09-consent-rewarded-ads
plan: 10
subsystem: platform
tags: [admob, gma-next-gen, rewarded-ads, consent, hilt, ads-economy, slice-2-swap]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening
    provides: ForegroundActivityProvider, AdIds/BuildConfig ad-ID plumbing, @ApplicationScope CoroutineScope, PlatformBoundaryRules, ManifestSurfaceTest, BuildConfigIdsTest, adIdLiteralCheck, ads-mobile-sdk on the dependency graph
  - phase: 09-consent-rewarded-ads (plan 09-02)
    provides: AdGateway (rewardedOfferable + preloadRewarded), RewardOutcome, AdReason, AdConstants, NoOpAdGateway
  - phase: 09-consent-rewarded-ads (plan 09-05)
    provides: ConsentGateway/AdsReadiness with a real signal, AdsModule, the "no SDK type in the Hilt graph" finding
  - phase: 09-consent-rewarded-ads (plan 09-08)
    provides: the assist funnel, the single requestRewarded call site, and the decision that the load timeout belongs to the gateway
provides:
  - RewardedAdSlot — single-slot rewarded ad load / hold / consume / invalidate cache
  - AdMobAdGateway — the real AdGateway (rewardedOfferable, preloadRewarded, requestRewarded)
  - "AdGateway is bound to AdMobAdGateway; the Slice-2 ad swap is delivered"
  - AdConstants.REWARDED_AD_MAX_AGE_MS — the defensive stale-ad bound
affects: [09-11 device verification, 10 interstitials, gameplay-viewmodel]

# Tech tracking
tech-stack:
  added: []  # ads-mobile-sdk 1.4.0 has been on the graph since Phase 6 plan 06-09
  patterns:
    - "SDK handle behind a seam, applied a second time: RewardedAdSlot owns its RewardedAd so the Hilt graph stays SDK-free"
    - "Load on the application scope, wait on the caller's: a timed-out accept leaves the warm ad behind instead of discarding the request"
    - "Main-thread confinement as the concurrency strategy, because the SDK mandates it anyway"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/di/AdsModule.kt

key-decisions:
  - "RewardedAdSlot.preload()/await() take no AdReason. detekt's UnusedParameter caught it: one rewarded unit serves both assists, so the reason cannot change what the slot does. Dropped the parameter rather than adding a @Suppress to satisfy the plan's literal signature — suppressing a gate to pass is the anti-pattern this phase has repeatedly refused."
  - "The load runs on the application scope via async, not on the caller's coroutine, so the D-10 accept timeout cancels only the wait. A player who times out still leaves a warm ad for the next offer."
  - "loadOnce wraps the SDK call in runCatching followed by ensureActive(). GMA Next-Gen throws on an ad call against an uninitialized SDK, and this load can be awaited from viewModelScope where an escaping exception is a crash mid-game; ensureActive restores the one thing runCatching must not swallow."
  - "PlatformModule's KDoc claim that AdsModule holds a UMP-typed ConsentInformation provider was stale and actively misleading — 09-05 removed that provider. Corrected, and the containment rule restated as 'no SDK-typed @Provides anywhere', which is what 09-05 actually proved."
  - "The post-show re-warm follows the plan and RESEARCH Pattern 7, and is routed through preloadRewarded so the consent gate applies. Its tension with D-09 is documented at the call site rather than silently resolved — see Concerns."

patterns-established:
  - "A bounded wait belongs to exactly one layer: the slot documents that the caller owns the timeout, the gateway owns it, and the ViewModel documents that it deliberately owns none."
  - "Grep-gated documentation, continued: the KDoc explaining that the reward flag is set in exactly one place avoids spelling the assignment out, so the criterion counting it stays at 1."

requirements-completed: [ADS-01, ADS-03, ADS-05, CONS-02, CONS-04]

# Metrics
duration: 24min
completed: 2026-09-01
---

# Phase 9 Plan 10: The Real Rewarded Path Summary

**Rewarded ads are real: a single-slot cache loads one ad at a time behind the consent gate, the gateway shows it with every wait bounded, and the reward is granted only from the SDK's earned-reward callback — with the `AdGateway` binding swapped in one line because every opt-in behaviour above the seam had already been proven against the stub.**

## Performance

- **Duration:** ~24 min
- **Tasks:** 3 of 3 complete
- **Files created:** 2
- **Files modified:** 3

## Accomplishments

- **`RewardedAdSlot` holds exactly one ad and knows when to let go of it.** `preload()` is idempotent and does nothing when an ad is held or a load is in flight; `await()` returns the held ad or suspends on the in-flight one; `invalidate()` calls `destroy()` and empties the slot. A slot older than the new defensive age bound is treated as empty rather than shown. Retry is **one** delayed attempt, never a loop (T-09-46), and the class KDoc records why the SDK's own `RewardedAdPreloader` lost the trade-off: it keeps refilling its buffer for the rest of the session, which fights D-09 and forces `destroy(preloadId)` after every refill.
- **The load outlives the wait.** The request is started with `async` on the application scope, so the D-10 accept deadline cancels only the caller's await. A player who times out at five seconds and takes the free assist still leaves a warm ad behind for the next offer — the alternative, loading inline in the caller's coroutine, throws the request away at exactly the moment it was about to succeed.
- **`AdMobAdGateway` bounds every wait, and each bound answers a different question.** `ACCEPT_WAIT_MS` caps the load — the only bound on the dialog's loading state, kept exactly as 09-08 required — and `ACTIVITY_WAIT_MS` caps the Activity wait. The Activity is captured **before** `show()` and never re-awaited inside a callback, because the provider is deliberately null for the whole presentation and the failure mode of asking again is a hang (Pitfall 13, T-09-45).
- **The reward comes only from the SDK.** The earned flag is assigned in exactly one place — inside the `OnUserEarnedRewardListener` passed to `show()` — verified at 1 occurrence, on the same line as `ad.show(`. A dismissal without earning resumes `Declined`, which refills nothing and costs the player nothing (ADS-05, T-09-43).
- **Everything that can go wrong grants the assist anyway.** Consent not `Ready`, load failure, no fill, no foreground Activity, and a failed presentation all resolve to `Unavailable` — which the funnel treats as "proceed" (ADS-03). `AdsReadiness.Unknown` maps to *not offerable*, so the pre-consent window produces D-12's silent free grant rather than a dialog that resolves into an instant free grant (Pitfall 16, T-09-44).
- **The swap was one parameter type**, which is the whole point of the ROADMAP's locked ordering: binding a real gateway changed which ad plays, not whether the player is asked. `NoOpAdGateway` and `NoOpAdGatewayTest` both stay, deliberately unbound, as the seam's executable reference behaviour.
- **Every Phase-6 gate is still green with no expected value edited.** `PlatformBoundaryTest` passes with the real SDK-backed gateway bound — the containment pattern 09-05 established held, and the rewarded path needed no SDK-typed provider at all.

## Task Commits

1. **Task 1: `RewardedAdSlot` — single-slot load / hold / consume cache** — `adc4c79` (feat)
2. **Task 2: `AdMobAdGateway` — the real rewarded path** — `b99b5e2` (feat)
3. **Task 3: Bind the real gateway and re-prove every build gate** — `74c811d` (feat)

## Files Created/Modified

- `platform/ads/RewardedAdSlot.kt` — created. `@Singleton`, main-thread confined, holding one `RewardedAd` plus its monotonic load instant and the in-flight load.
- `platform/ads/AdMobAdGateway.kt` — created. `@Singleton`, implements all four `AdGateway` members; `rewardedOfferable` is derived from consent readiness via `map(...).stateIn(...)`, so no mutable flow is exposed.
- `platform/ads/AdConstants.kt` — modified. Adds `REWARDED_AD_MAX_AGE_MS`, whose KDoc marks the ~1h expiry figure as `[ASSUMED]` for Next-Gen and the constant as a defensive bound rather than an encoded guarantee.
- `di/PlatformModule.kt` — modified. `bindAdGateway` now takes `AdMobAdGateway`; the Slice-2 KDoc records the ad half as delivered and `NoOpAdGateway` as deliberately retained; the SDK-containment paragraph was corrected (see Deviations).
- `platform/ads/di/AdsModule.kt` — modified. Records that the predicted rewarded-loader collision never happened, because the slot owns its `RewardedAd` internally — the module still holds `@Binds` of interfaces and nothing else.

## Decisions Made

See `key-decisions` in the frontmatter. The one a reviewer should look at first is the dropped `AdReason` parameter: the plan specified `preload(reason)` / `await(reason)`, detekt proved the parameter unused, and the choice was between deleting it and suppressing the rule. Deleting it removes a documented distinction the code does not make; suppressing it would have been the third time this phase was tempted to widen a gate to pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking issue] `detekt` rejected the plan's `RewardedAdSlot` signatures**

- **Found during:** Task 1
- **Issue:** the plan specifies `fun preload(reason: AdReason)` and `suspend fun await(reason: AdReason)`. Both parameters are genuinely unused — this app serves `HINT` and `UNDO` from one rewarded unit, so the reason cannot influence the `AdRequest` — and `:app:detekt` failed with two `UnusedParameter` violations, blocking Task 1's own verification gate.
- **Fix:** dropped the parameter from `RewardedAdSlot.preload()` and `await()`, with KDoc on both explaining that the seam above them *is* expressed per reason and that the parameter returns here as a real one if a second ad unit ever appears. `AdGateway.preloadRewarded(reason)` and `requestRewarded(reason)` are untouched — the public contract 09-08 tests against is unchanged.
- **Rejected alternative:** `@Suppress("UnusedParameter")` to preserve the plan's literal signature. RESEARCH's own anti-pattern list forbids reaching for `@Suppress` to get past detekt, and a parameter whose KDoc has to say "this does nothing" is worse documentation than no parameter.
- **Files modified:** `platform/ads/RewardedAdSlot.kt`
- **Verification:** `:app:detekt` red before, green after.
- **Committed in:** `adc4c79`

**2. [Rule 1 - Bug] `PlatformModule`'s KDoc pointed the next reader straight into the 09-05 trap**

- **Found during:** Task 3
- **Issue:** the file claimed *"Phase 9's consent work delivered `AdsModule`, which lives inside `..platform..` and holds the UMP-typed `ConsentInformation` provider plus the `ConsentGateway` binding."* That provider does not exist — 09-05 deleted it precisely because it failed `PlatformBoundaryTest` from inside `..platform..`. The surrounding paragraph still taught the weaker rule ("put SDK-typed providers in `platform/ads/di/`") that 09-05 disproved, using a worked example that had been removed. A reader following this file's advice would have written exactly the binding that goes red.
- **Fix:** corrected the factual claim and restated the rule as **no SDK-typed `@Provides` anywhere, in any module**, with the generated-component reasoning and the note that exempting `DaggerApp_HiltComponents_SingletonC` is never the fix. `AdsModule`'s KDoc gained the matching update: the predicted rewarded-loader collision did not occur, because the slot owns its SDK object.
- **Files modified:** `di/PlatformModule.kt`, `platform/ads/di/AdsModule.kt`
- **Verification:** `PlatformBoundaryTest` green with the real gateway bound; `grep -rn` for SDK packages under `di/` returns nothing.
- **Committed in:** `74c811d`

**3. [Rule 2 - Missing critical functionality] The ad expiry bound had no constant to live in**

- **Found during:** Task 1
- **Issue:** the plan requires treating an over-age slot as empty and refers to "the ad-expiry cadence constant", but no such constant existed and `AdConstants.kt` is not in the plan's `files_modified`. Inlining the figure would violate CLAUDE.md's no-magic-numbers rule and detekt's `MagicNumber`.
- **Fix:** added `REWARDED_AD_MAX_AGE_MS` to `AdConstants` — the project's scoped ad-timing constants object, and the sibling the plan's own prose points at. Its KDoc marks the underlying ~1h figure as assumed for Next-Gen and states that correctness does not rest on it, because `onAdFailedToShowFullScreenContent` → `Unavailable` covers a stale ad regardless (T-09-47).
- **Files modified:** `platform/ads/AdConstants.kt`
- **Committed in:** `adc4c79`

**4. [Rule 2 - Missing critical functionality] A thrown SDK error on the accept path was an app crash**

- **Found during:** Task 1
- **Issue:** GMA Next-Gen makes initialization mandatory and throws `UninitializedPropertyAccessException` on an ad call against an uninitialized SDK. The load is awaited from `requestRewarded`, which runs on `viewModelScope` — an escaping exception there is a crash during gameplay, not a lost ad. The readiness gate should make it unreachable, but "should be unreachable" is not the standard for a path that ends in a crash.
- **Fix:** `loadOnce()` wraps the SDK call in `runCatching` and immediately calls `currentCoroutineContext().ensureActive()`, so a thrown failure degrades to the same `null` a no-fill produces (→ `Unavailable` → free assist) while coroutine cancellation is still honoured rather than swallowed.
- **Files modified:** `platform/ads/RewardedAdSlot.kt`
- **Committed in:** `adc4c79`

**5. [Rule 3 - Blocking issue] Recreated the gitignored `local.properties`**

- **Found during:** the first Gradle invocation
- **Issue:** `local.properties` is gitignored, so this fresh worktree had no `sdk.dir` and every `:app` task failed with "SDK location not found" — the same environment gap 09-05 and 09-08 both recorded.
- **Fix:** copied from the main checkout. Environment-only; the file stays untracked and is in no commit.
- **Committed in:** n/a

### Acceptance criteria intentionally not met

One **phase-level** verification line is not literally satisfiable and was already false before this plan: *"`grep -rn "com.google.android.gms.ads\|play-services-ads" app/src/main gradle/libs.versions.toml` returns nothing."* It returns three matches, none of them the legacy SDK:

| Match | Why it is there |
|---|---|
| `AndroidManifest.xml:48` — `com.google.android.gms.ads.APPLICATION_ID` | The meta-data key UMP requires, added by plan 09-05 per Pitfall 7. The key name is fixed by the platform; it is not an import or a dependency. |
| `libs.versions.toml:77,83` | Two comments **warning against** `com.google.android.gms:play-services-ads`, added when the Next-Gen coordinate was pinned. |

The substantive claim holds and was verified directly: the only ads coordinate declared is `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`, and both new source files contain **zero** occurrences of `com.google.android.gms.ads` (T-09-SC).

---

**Total deviations:** 5 auto-fixed (2 blocking, 2 missing-functionality, 1 bug). No Rule 4 architectural questions arose.
**Impact on plan:** no scope creep. Deviations 1 and 3 are small shape corrections forced by build gates; 2 corrects documentation that would have cost the next phase real time; 4 closes a crash path on the accept flow.

## Verification Results

| Gate | Result |
|------|--------|
| `./gradlew check --offline` | **green end to end, 11m 8s** — the Phase 6 D-18 offline invariant holds with a real ad SDK bound |
| `:app:testDebugUnitTest` — 48 classes | **420 tests, 0 failures, 0 errors** |
| `:app:testDebugUnitTest --tests '*PlatformBoundaryTest*' '*ManifestSurfaceTest*' '*BuildConfigIdsTest*'` | green — SDK containment survived the real binding |
| `git diff` on `PlatformBoundaryTest.kt`, `ManifestSurfaceTest.kt`, `BuildConfigIdsTest.kt`, `PlatformBoundaryRules.kt` | **empty** — no expected value edited, no rule widened |
| `:app:assembleDebug --offline` | green |
| `:app:detekt`, `:app:ktlintMainSourceSetCheck`, `:app:adIdLiteralCheck` | green |
| `build-logic/` and `app/build.gradle.kts` changed | **no** — the Phase-6 release fail-fast on a missing/placeholder ad ID is structurally unchanged |

Grep criteria: `earned = true` count **1**, on the `ad.show(` line; `withTimeoutOrNull` count **3** (≥2 required); `AdsReadiness.Ready` count **2**; `com.google.android.gms.ads` count **0** in both new files; `withContext(Dispatchers.IO)` count **0**; `awaitActivity` at line 123 precedes `ad.show(` at line 186; `AdRequest.Builder(AdIds.rewardedAdUnitId)` count **1**; `ca-app-pub` count **0**; `destroy()` present in `invalidate()`; `@Singleton` on both new classes; no `while`/`repeat` loop around the load (the two `while` matches are the English word in KDoc prose); no SDK imports under `di/`; `NoOpAdGateway.kt` and `NoOpAdGatewayTest.kt` both still present.

## Known Stubs

- **`AdMobAdGateway.showInterstitial()` is a deliberate no-op**, as the plan mandates. Its KDoc names Phase 10 as the owner. This is not a gap in this plan's goal: no surface renders a placeholder as a result of it, and implementing an interstitial here would put an unrequested fullscreen ad into a phase whose contract is "the player is always asked first". `AdGateway.showInterstitial` has had no caller since Phase 6.

## Threat Flags

None. The plan's own threat register already covers this plan's entire new surface — the app ↔ AdMob ad-serving boundary, the reward economy, the Activity/fullscreen-ad interaction, and debug-vs-real ad inventory. No new network endpoint, auth path, file access pattern or schema change was introduced beyond it, and the merged permission set is unchanged (`ManifestSurfaceTest`'s set-equality assertion passes untouched, T-09-48).

## Concerns for the Phase Verifier

1. **Nothing in this plan is covered by an automated behavioural test.** `RewardedAdSlot` and `AdMobAdGateway` are device-only: every path runs through the GMA SDK, and the plan explicitly defers end-to-end verification of a real test rewarded ad to plan **09-11**. The gates proved here are structural (compilation against the real API, SDK containment, manifest surface, offline invariant, lint) plus the grep criteria pinning ADS-05. The behavioural proof that an early dismiss declines and an earned reward grants exists only at the seam, against fakes, from plan 09-08. **09-11 is where ADS-05 actually gets confirmed against a real ad.**
2. **The post-show re-warm is in mild tension with D-09, knowingly.** Both terminal callbacks call `slot.invalidate()` then `preloadRewarded(reason)`, exactly as the plan and RESEARCH Pattern 7 specify. After a *granted* reward the ViewModel refills the allowance, so that request is issued while free allowance remains — which is the letter of what D-09 forbids. It is one bounded request rather than a maintained buffer, which is precisely the distinction that made the manual slot preferable to `RewardedAdPreloader` (Pitfall 12), and it is documented at the call site. Flagging it rather than silently re-deciding it: a stricter reading would preload only on `Declined`, but that requires the gateway to encode the ViewModel's private economy rules, which is a layering violation. **This is a judgement worth a second opinion, not a defect.**
3. **`REWARDED_AD_MAX_AGE_MS` rests on an assumed figure** (RESEARCH A1). If device testing shows ads expiring sooner or later, this is the one number to tune; nothing else depends on it, because `onAdFailedToShowFullScreenContent` → `Unavailable` is the real safety net.

## Next Phase Readiness

- **09-11 (device verification)** can now exercise the full funnel against Google's **test** rewarded unit, which debug builds resolve via `BuildConfig` (`BuildConfigIdsTest` still pins the debug prefix). Note 09-05's standing caveat: until a UMP consent message is configured in the AdMob Privacy & messaging console, a real device sees no form and `privacyOptionsRequired` reports `false` — correct behaviour, not a defect.
- **Phase 10 (interstitials)** inherits a live `AdGateway` binding and an initialized SDK; `showInterstitial()` is the single seam to fill, and the `RewardedAdSlot` shape is the template for an interstitial slot.
- **The containment pattern is now proven twice** (UMP `ConsentInformation`, GMA `RewardedAd`). Any future SDK object belongs inside a platform class, never in a Hilt binding.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt` — FOUND (deliberately retained)
- Commits `adc4c79`, `b99b5e2`, `74c811d` — all FOUND in `git log`
- No modifications to `STATE.md` or `ROADMAP.md` — the orchestrator owns those.

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-09-01*
