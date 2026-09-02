---
phase: 10-interstitials
plan: 03
subsystem: ads
tags: [admob, gma-next-gen, interstitial, kotlin, coroutines, stateflow, hilt]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads
    provides: "RewardedAdSlot (the class this one clones), ConsentGateway.readiness as the single serving gate, ForegroundActivityProvider + ACTIVITY_WAIT_MS, the AdMobAdGateway binding, and the SDK-containment discipline"
  - phase: 06-platform-foundation-build-hardening
    provides: "AdIds.interstitialAdUnitId behind BuildConfig and the adIdLiteralCheck build gate"
provides:
  - "InterstitialAdSlot — single-slot interstitial load/hold/age-out/destroy with an observable Boolean readiness signal"
  - "AdGateway's three interstitial members: interstitialReady, preloadInterstitial(), suspend showInterstitial(): Boolean"
  - "AdMobAdGateway's real interstitial implementation (consent-gated preload, peek-never-await show, recycle-then-rewarm)"
  - "AdConstants.INTERSTITIAL_AD_MAX_AGE_MS and INTERSTITIAL_QUIESCENCE_MS (the D-08 dim pause plan 10-04 consumes)"
  - "NoOpAdGateway's opposite-polarity interstitial parity, pinned by two self-explaining tests"
affects: [10-04-results-trigger-and-scrim, 10-cadence-wiring, future-ad-work]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Clone-and-delta ad slot: a second SDK-holding class written as a near-verbatim retype of the first, with each intentional divergence carrying a KDoc paragraph that names itself a delta"
    - "Derived readiness via combine(consent, slotHeld).stateIn(Eagerly, false) — never mirrored into a second mutable flow"
    - "Boolean-returning suspend show: presented/not-presented as the seam's whole vocabulary, keeping every GMA type out of the interface"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
    - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt

key-decisions:
  - "InterstitialAdSlot exposes heldFresh() rather than a suspending await() — D-10 forbids adding any latency to a continue tap, so the gateway only ever peeks"
  - "Readiness is observable (StateFlow<Boolean>) rather than a getter, and carries a Boolean rather than the ad, so no GMA type can reach Hilt or a ViewModel"
  - "interstitialReady derives from consent AND a held ad; preloadInterstitial gates on consent readiness alone, because gating it on interstitialReady would be self-defeating"
  - "showInterstitial()'s consent and slot guards folded into a private servableInterstitial() to stay inside detekt's ReturnCount limit of 2 — neither condition changed"
  - "NoOpAdGateway.interstitialReady is false, deliberately the opposite polarity to rewardedOfferable = true, because v1.0 had no interstitials and a ready-reporting stub would add a pause and scrim for zero revenue"

patterns-established:
  - "Symmetry-as-requirement: the second ad-holding class is a retype of the first, and every divergence is self-documenting so drift is visible in review"
  - "Asymmetric caller contract on a Boolean show: advance counters only on true, navigate unconditionally on either answer (D-09 + D-11)"
  - "Opposite-polarity stub parity pinned by a test whose KDoc argues why it must not be 'corrected' for symmetry"

requirements-completed: [INT-01, INT-02]

# Metrics
duration: 22min
completed: 2026-09-02
---

# Phase 10 Plan 03: Interstitial Ad Slot & Seam Summary

**A real GMA Next-Gen interstitial behind the `AdGateway` seam — `InterstitialAdSlot` as a documented retype of `RewardedAdSlot`, an evolved three-member seam that names no SDK type, and a stub whose deliberate opposite polarity is pinned by tests.**

## Performance

- **Duration:** ~22 min
- **Started:** 2026-09-02T20:28:00Z
- **Completed:** 2026-09-02T20:50:00Z
- **Tasks:** 3
- **Files modified:** 15 (1 created, 14 modified)

## Accomplishments

- `InterstitialAdSlot` holds at most one interstitial with the shipped rewarded discipline retyped intact: main-thread confinement, idempotent `preload()`, exactly one delayed retry at `PRELOAD_RETRY_DELAY_MS`, `destroy()` on invalidate, and a no-fill mapped to `null` rather than to an error path. Its two divergences each carry a KDoc paragraph declaring themselves deltas — there is no `await()` (D-10 forbids latency on a continue tap; `heldFresh()` peeks instead), and readiness is an observable `StateFlow<Boolean>`.
- The `AdGateway` seam now expresses readiness, warm-up and a presented/not-presented show in three members, and still names **zero** Google Mobile Ads types (`grep -c 'com.google.android' AdGateway.kt` → 0). `showInterstitial()`'s KDoc states the asymmetric caller contract explicitly: advance cadence counters only on `true`, navigate unconditionally on either answer.
- `AdMobAdGateway` implements the three members with Phase 9's ordering discipline intact — consent-gated preload (T-09-44), a peek that never awaits, the Activity captured **before** `show()` and bounded by `ACTIVITY_WAIT_MS` (Pitfall 13), `isActive`-guarded resumes, `invokeOnCancellation` detaching the callback, and `recycleInterstitial()` destroying-then-rewarming on **both** terminal callbacks (Pitfall 9).
- The SDK containment gate held after adding a second SDK-holding class: `PlatformBoundaryTest` and `PlatformBoundaryGateTest` are 6/6 green. No Hilt module change was needed, exactly as `AdsModule`'s KDoc predicted for `RewardedAdSlot`.
- The whole app unit suite is green at **432 tests, 0 failures**, with `detekt`, `ktlintCheck` and `adIdLiteralCheck` all passing.

## Task Commits

1. **Task 1: InterstitialAdSlot + AdConstants tokens** — `390d258` (feat)
2. **Task 2: Evolve the AdGateway seam and implement the real interstitial** — `16ff8c7` (feat)
3. **Task 3: Bring every AdGateway test double onto the new seam** — `3fda38b` (test)

## Files Created/Modified

- `platform/ads/InterstitialAdSlot.kt` **(new)** — single-slot interstitial lifecycle: `preload()`, `heldFresh()`, `invalidate()`, `ready: StateFlow<Boolean>`, one bounded retry, no logging, `AdIds.interstitialAdUnitId` as the only ad-unit route.
- `platform/ads/AdConstants.kt` — added `INTERSTITIAL_AD_MAX_AGE_MS` (2,700,000 ms, `[ASSUMED]`-labelled under the undocumented one-hour expiry) and `INTERSTITIAL_QUIESCENCE_MS = 500L` (D-08, chosen to exceed the 300 ms double-tap timeout). Class KDoc sharpened so it now explicitly forbids D-01..D-04 cadence values.
- `platform/ads/AdGateway.kt` — the stale `fun showInterstitial()` and its Slice-1 KDoc replaced by `interstitialReady` / `preloadInterstitial()` / `suspend showInterstitial(): Boolean`; class KDoc corrected and extended.
- `platform/ads/AdMobAdGateway.kt` — `interstitialSlot` constructor dependency, derived `interstitialReady`, consent-gated `preloadInterstitial()`, peek-never-await `showInterstitial()`, private `servableInterstitial()`, `showToDismissal()` and `recycleInterstitial()`. Stale no-op KDoc block deleted.
- `platform/ads/NoOpAdGateway.kt` — `interstitialReady = false`, `preloadInterstitial()` no-op, `showInterstitial() = false`, with the asymmetry argued out in the class KDoc.
- `test/platform/ads/NoOpAdGatewayTest.kt` — `showInterstitialDoesNotThrow` replaced by `interstitialReadyIsFalseSoTheStubNeverPausesThePlayer` and `showInterstitialReportsNotPresented`.
- Eight `test/ui/game/GameViewModel*Test.kt` files — AdGateway doubles moved onto the new seam (only `override` lines changed; +5/−1 per file).

## Decisions Made

- **`INTERSTITIAL_AD_MAX_AGE_MS` is a duplicated literal, not a reference to `REWARDED_AD_MAX_AGE_MS`.** The two share an assumption today but are separate inventory with separate lifetimes; aliasing them would make one impossible to move without dragging the other. The KDoc says so.
- **`recycleInterstitial()` re-warms immediately on both terminal callbacks.** The instant an interstitial finishes is the moment another is certainly not imminent, so it is the cheapest possible time to pay for the next load — and it still passes the consent gate.
- **`ready` sets `false` through `invalidate()` only.** The age-out path in `freshOrDiscard()` routes through `invalidate()`, so there is exactly one place readiness drops, rather than two that can disagree.
- **The `AdGateway` interface KDoc's stale Slice-1 claim was corrected.** It asserted the seam "is fulfilled by an auto-grant NoOpAdGateway stub" and that the binding swap was still ahead — untrue since Phase 9 bound `AdMobAdGateway`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Two more `AdGateway` test doubles than the plan listed**
- **Found during:** Task 3
- **Issue:** The plan enumerated six `ui/game` fakes. `GameViewModelResumeTest.kt` (`GrantingAds`) and `GameViewModelAllowanceTest.kt` (`AllowanceAdGateway`) also implement `AdGateway` and were still on the old seam, so the debug unit-test source set would not compile.
- **Fix:** Applied the identical mechanical change to both — the same three overrides, nothing else touched.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt`
- **Verification:** `:app:testDebugUnitTest` compiles and the full 432-test suite passes; `grep -rc 'override fun showInterstitial() = Unit'` over `ui/game/` is 0.
- **Committed in:** `3fda38b` (Task 3 commit)

**2. [Rule 3 - Blocking] `showInterstitial()` tripped detekt's `ReturnCount` limit**
- **Found during:** Task 2
- **Issue:** The plan's literal shape (three guard clauses plus a final return) is four exits; detekt caps a function at two. `:app:detekt` failed with `Function showInterstitial has 4 return statements which exceeds the limit of 2`.
- **Fix:** Folded the consent check and the slot peek — one question, "is there an ad I am allowed to show right now?" — into a private `servableInterstitial(): InterstitialAd?`, and expressed the Activity guard as an `if/else`. Neither condition changed; the required `interstitialSlot.heldFresh()`, `AdConstants.ACTIVITY_WAIT_MS` and capture-before-show ordering are all intact. This follows the in-repo precedent recorded at `GameViewModel.resumeTickerIfNeeded`, and the reason is written into the KDoc so the shape is not "simplified" back.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt`
- **Verification:** `:app:detekt`, `:app:ktlintCheck`, `:app:compileDebugKotlin` all green.
- **Committed in:** `16ff8c7` (Task 2 commit)

**3. [Rule 1 - Bug] Stale factual claim in the `AdGateway` interface KDoc**
- **Found during:** Task 2
- **Issue:** The class KDoc stated the seam "Today ... is fulfilled by an auto-grant `NoOpAdGateway` stub" and that swapping the Hilt binding for a real implementation was still future work. Phase 9 already made that swap, so the file's own documentation contradicted the binding in `PlatformModule` — in the exact file being edited to remove a *different* piece of the same staleness.
- **Fix:** Corrected to name both implementations and extended with the two-ad-kinds / no-SDK-type-in-this-file rule.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt`
- **Verification:** Documentation only; no behaviour change. Gates green.
- **Committed in:** `16ff8c7` (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 documentation-correctness)
**Impact on plan:** All three were necessary to complete the planned work as specified — two were compile/gate blockers and the third removed a false statement inside a file the plan already required editing. No scope creep; no behaviour beyond the plan's three tasks.

## Issues Encountered

- **The worktree had no `local.properties`,** so every Gradle invocation failed with "SDK location not found". The file is gitignored and therefore absent from a fresh worktree. Resolved by passing `ANDROID_HOME=/Users/ermanergoz/Library/Android/sdk` on each Gradle invocation rather than writing an untracked file into the worktree — nothing was added to the repo, and the plan's verification commands are otherwise unchanged. Any future worktree-isolated executor on this project will hit the same wall.
- **The GMA interstitial surface was verified against bytecode, not recall.** `javap` over the resolved `ads-mobile-sdk-1.4.0.aar` confirmed `InterstitialAd.Companion.load(AdRequest): AdLoadResult<InterstitialAd>` (suspend), `show(Activity)` with no reward listener, and `InterstitialAdEventCallback` inheriting the six default `AdEventCallback` methods — matching the plan's `<interfaces>` block exactly.

## Threat Flags

None — no new security-relevant surface beyond the plan's `<threat_model>`. Every mitigation it assigned is present: consent gating on both interstitial entry points (T-10-02), zero logging across all four ad files (T-10-09, verified by grep), Boolean-only signatures with `PlatformBoundaryTest`/`PlatformBoundaryGateTest` green (T-10-13), exactly one bounded retry (T-10-14), destroy-on-both-terminal-callbacks plus `invokeOnCancellation` (T-10-15), and the Activity captured before `show()` under `ACTIVITY_WAIT_MS` and never re-awaited from a callback (T-10-16). No dependency was added (T-10-SC).

## Known Stubs

None. `NoOpAdGateway`'s interstitial members are `false`/no-op **by design**, not as placeholders — that polarity is the deliverable, argued in the class KDoc and pinned by two tests. It is not work deferred to a later plan.

## User Setup Required

None — no external service configuration required by this plan. (The phase-level prerequisite that a real AdMob interstitial ad unit must exist for release builds is tracked in the ROADMAP's External Prerequisites table and is unchanged by this plan; debug builds use Google's test unit via `AdIds.interstitialAdUnitId`.)

## Next Phase Readiness

- **Ready for plan 10-04 (the Results trigger + D-08 scrim):** `adGateway.interstitialReady` is the flow the tap gate observes, `preloadInterstitial()` is the D-12 warm-up hook, `showInterstitial(): Boolean` is the sequencing primitive, and `AdConstants.INTERSTITIAL_QUIESCENCE_MS` is the pause the scrim holds for.
- **The caller contract 10-04 must honour, restated:** advance cadence counters **only** on `true`; navigate **unconditionally** on either answer; show no scrim and add no delay when `interstitialReady` is `false` (D-10).
- **No device verification happened here** — this plan is unit-tested only. The ADSF-01 clock-freeze re-assertion and any on-hardware interstitial check belong to the phase's verification pass.
- **No blockers.**

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt` — FOUND
- `.planning/phases/10-interstitials/10-03-SUMMARY.md` — FOUND
- Commits `390d258`, `16ff8c7`, `3fda38b`, `684402c` — all FOUND on `worktree-agent-a0b1de7b655d86c14`
- Working tree clean; no shared orchestrator artifacts (STATE.md, ROADMAP.md) touched

---
*Phase: 10-interstitials*
*Completed: 2026-09-02*
