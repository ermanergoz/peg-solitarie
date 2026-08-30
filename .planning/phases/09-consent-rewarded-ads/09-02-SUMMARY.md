---
phase: 09-consent-rewarded-ads
plan: 02
subsystem: platform
tags: [admob, ump, consent, adgateway, stateflow, seam, interface-first]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening
    provides: AdGateway/RewardOutcome/AdReason seam, NoOpAdGateway stub, AdIds + adIdLiteralCheck build gate, ForegroundActivityProvider, PlatformBoundaryRules
provides:
  - "AdGateway.rewardedOfferable — a pre-dialog readiness signal (D-12) that does not disturb the sealed three-way RewardOutcome"
  - "AdGateway.preloadRewarded(reason) — a non-suspending fire-and-forget preload hook (D-09)"
  - "AdConstants — ACCEPT_WAIT_MS / ACTIVITY_WAIT_MS / PRELOAD_RETRY_DELAY_MS, timeouts and cadence only"
  - "NoOpAdGateway.rewardedOfferable == true, so the ADS-01 opt-in dialog is reachable against the stub"
  - "FakeAdGateway/GatedAdGateway with an offerable flag and a preloadedReasons recorder, ready for plan 09-06"
  - "ConsentGateway — SDK-free consent seam (readiness, privacyOptionsRequired, bootstrap, showPrivacyOptions)"
  - "AdsReadiness — Unknown / Ready / Unavailable, with Unknown documented as NOT offerable"
affects: [09-03, 09-04, 09-05, 09-06, 09-07, 09-08, 09-09, settings-privacy-options, admob-rewarded-implementation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Readiness as a separate observable signal rather than a fourth sealed-outcome member"
    - "Consent seam publishing an SDK-free enum StateFlow, mirroring GamesService/GamesSignInState"
    - "Scoped timeout-constants object (AdConstants) kept strictly disjoint from the ad-ID object (AdIds)"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/AdsReadiness.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/ConsentGateway.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt
    - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt

key-decisions:
  - "AdsReadiness is an enum class, not a sealed interface — all three members are payload-free, so GamesSignInState's 'sealed because one member carries a payload' rationale does not transfer; AdReason and GameStatus are the in-repo precedent"
  - "AdConstants.ACTIVITY_WAIT_MS pinned at 10_000L — twice the D-10 in-dialog accept wait, so a slow Activity resolution cannot be the thing that trips the visible 5s cap"
  - "AdConstants.PRELOAD_RETRY_DELAY_MS pinned at 30_000L — the player has not asked for anything at preload time, so retry is deliberately unhurried rather than a tight no-fill loop"
  - "NoOpAdGateway.rewardedOfferable is true, reversing the naive reading of 'the stub has no ads', so the stub-first dialog ordering actually exercises the dialog (Pitfall 6)"
  - "Phase 6 D-02's 'the grant is deliberately not configurable' is explicitly rescoped to Phase 6 in the NoOpAdGateway KDoc rather than deleted"

patterns-established:
  - "Pre-decision signals live beside the sealed outcome type, never inside it — a fourth RewardOutcome member would break exhaustive call sites and answers the question too late"
  - "Platform seams expose an observable StateFlow of a plain app-owned type; the Activity is resolved inside the implementation and never appears in a signature"
  - "Interface-first plans widen the seam and repair every test fake in the same wave, so downstream plans never re-open the fake files"

requirements-completed: [ADS-01, ADS-03, CONS-03, CONS-04]

# Metrics
duration: 42min
completed: 2026-08-31
---

# Phase 9 Plan 02: Platform Contracts for Consent & Rewarded Ads Summary

**The Phase-9 seam surface declared before any SDK exists: `AdGateway` gains a `rewardedOfferable` StateFlow and a `preloadRewarded` hook, the NoOp stub flips to reporting offers as available so the opt-in dialog is reachable, and `ConsentGateway`/`AdsReadiness` land as contracts naming neither a consent SDK nor an Activity.**

## Performance

- **Duration:** ~42 min (dominated by an 11-minute full `check` run)
- **Started:** 2026-08-31T01:59Z
- **Completed:** 2026-08-31T02:41Z
- **Tasks:** 3
- **Files modified:** 12 (3 created, 9 modified)

## Accomplishments

- `AdGateway` widened additively: `rewardedOfferable: StateFlow<Boolean>` answers D-12 *before* the dialog is shown, and `preloadRewarded(reason)` gives D-09 its fire-and-forget hook. `RewardOutcome.kt` is byte-identical — the sealed three-way contract every call site handles exhaustively is untouched.
- `AdConstants` created holding only millisecond timeouts and cadence, and `AdIds`' KDoc — which previously said an `AdConstants` object "must never be written" — rewritten so the two objects' boundary reads as a rule about *ad IDs*, not about the file. `adIdLiteralCheck` green.
- `NoOpAdGateway.rewardedOfferable` set to `true` with the Pitfall-6 rationale in both the KDoc and a test that names it, so the ADS-01 opt-in dialog can be built and exercised against the stub before the real SDK arrives.
- All six `AdGateway` implementations across five test files compile against the widened seam, and the two fakes plan 09-06 needs (`offerable = false` for D-12, `preloadedReasons` for D-09) are already in place — 09-06 will not have to re-open those files.
- `ConsentGateway` and `AdsReadiness` exist as SDK-free contracts; `AdsReadiness.Unknown` carries the Pitfall-16 instruction ("observers MUST treat this as *not offerable*") on the member itself, closing T-09-08 client-side.

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend the AdGateway seam and create AdConstants** — `ac24172` (feat)
2. **Task 2: Flip the NoOp stub and teach every AdGateway fake the new members** — `6fdace6` (feat)
3. **Task 3: Declare the SDK-free consent contracts** — `b96551d` (feat)

**Plan metadata:** committed with this SUMMARY (docs).

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt` — gains `rewardedOfferable` and `preloadRewarded`; KDoc states why readiness is a separate signal rather than a fourth `RewardOutcome` member
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt` — `ACCEPT_WAIT_MS = 5_000L`, `ACTIVITY_WAIT_MS = 10_000L`, `PRELOAD_RETRY_DELAY_MS = 30_000L`; explicitly holds no ad ID
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt` — KDoc paragraph 3 amended only; the three `BuildConfig` accessors are unchanged
- `app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt` — private `MutableStateFlow(true)` exposed via `asStateFlow()`, no-op `preloadRewarded`; D-02 rescoped to Phase 6, `NoOpGamesService` contrast kept
- `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/AdsReadiness.kt` — `Unknown` / `Ready` / `Unavailable` enum with the enum-vs-sealed justification and the Pitfall-16 rule on `Unknown`
- `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/ConsentGateway.kt` — `readiness`, `privacyOptionsRequired`, `bootstrap()`, `showPrivacyOptions()`; documents `bootstrap()` as idempotent and every-launch, and why the Activity is absent from `showPrivacyOptions()`
- `app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt` — one new case, `rewardedOfferableIsTrueSoTheOptInDialogIsReachable`, whose KDoc names Pitfall 6
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt` — `FakeAdGateway(offerable = true)` + `preloadedReasons`; `GatedAdGateway(offerable = true)` + `preloadedReasons`
- `GameViewModelChallengeAdvanceTest.kt`, `GameViewModelHintStalenessTest.kt`, `GameViewModelResumeTest.kt`, `GameViewModelSoundTest.kt` — each local `AdGateway` implementation gains the two overrides; no assertion touched

## Decisions Made

- **`AdsReadiness` is an `enum class`.** The plan required a deliberate choice with a justification that does not falsely claim a payload. All three members are payload-free and none will acquire data — readiness is a tri-state answer, not a result type. `GamesSignInState` is sealed specifically because `SignedIn` carries a `playerId`; `AdReason` and `GameStatus` are the in-repo precedent for the payload-free case being an enum.
- **`ACTIVITY_WAIT_MS = 10_000L`.** The plan named the constant but not the value. Set to twice `ACCEPT_WAIT_MS` so that waiting for a foreground Activity can never be what makes the player-visible 5s in-dialog cap expire — the two bounds do not race.
- **`PRELOAD_RETRY_DELAY_MS = 30_000L`.** Preload fires when a counter hits zero, before the player has asked for anything, so a slow retry costs nothing and a tight loop against a no-fill response would burn battery and request quota.
- **Kept the extra `preloadRewarded` no-throw test out of `NoOpAdGatewayTest`.** The plan's acceptance criterion is "the same test count as before plus the one new NoOp case"; a second case was written and then removed to match, and it would have proven nothing beyond `= Unit`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing `local.properties` in the worktree**
- **Found during:** Task 2 (running `:app:testDebugUnitTest`)
- **Issue:** Gradle failed with "SDK location not found" — `local.properties` is gitignored (it also carries the Phase-6 untracked release ad IDs), so a fresh git worktree has no Android SDK path and no test task can resolve its dependencies.
- **Fix:** Copied the main checkout's `local.properties` into the worktree root. It stays gitignored and is not part of any commit.
- **Files modified:** none tracked (`local.properties` is ignored)
- **Verification:** `:app:testDebugUnitTest` and the full `check` both run; `git status --short` never showed the file.
- **Committed in:** n/a — untracked by design

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Environment-only. No source change, no scope creep. Worth noting for future worktree-isolated executors in this repo: every Gradle task needs `local.properties` copied in first.

## Issues Encountered

- **Two acceptance criteria are worded as greps over prose**, which the first draft of the KDoc violated. `ConsentGateway`'s KDoc originally explained the missing parameter using the fully-qualified framework Activity name, which fails the `grep -rn "android.app.Activity" ConsentGateway.kt` criterion even though it appears only in a comment. Rewritten to "the framework Activity type"; both greps now return nothing. The same care applied to `AdIds` — the amended `AdConstants` sentence lives entirely inside the KDoc, so `grep -v '^\s*[*/]' … | grep -c AdConstants` is 0.
- **The `check` gate takes ~11 minutes**, dominated by `:domain:depthSweep`. Green, and the sweep's own numbers are unchanged from Phase 8 (diamond41 440 solver-fallbacks, wiegleb45 4 — the documented residual gap positions, not a regression from this plan).

## Threat Flags

None. Task 3's `PlatformBoundaryTest` run closes T-09-05 in-plan rather than deferring it to phase end; T-09-06's caps and T-09-07's ID/timeout separation are declared here and enforced at the implementation sites in plans 09-05/09-09. No new network endpoint, auth path, file access pattern, or schema change was introduced — this plan declares interfaces only.

## Known Stubs

None that block this plan's goal. `ConsentGateway` and `AdsReadiness` intentionally ship without an implementation and without a Hilt binding — that is the plan's stated output ("Declare no implementation in this task"), and plan 09-05 supplies the UMP-backed implementation. `NoOpAdGateway.preloadRewarded` is a no-op by contract, which `AdGateway`'s KDoc names as a valid implementation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 09-03 through 09-09 can be written and executed against these signatures without exploring the codebase; every name they were planned against now exists.
- Plan 09-06 has both fakes it needs already wired: `FakeAdGateway(offerable = false)` for the D-12 silent-free-grant path and `preloadedReasons` for D-09's "preload only at exhaustion".
- Plan 09-05 (the UMP implementation) inherits `AdConstants.ACTIVITY_WAIT_MS` as the documented cap for `awaitActivity()` and `AdsReadiness` as its published state.
- One open follow-up outside this plan, already recorded in RESEARCH.md Pitfall 7: `AndroidManifest.xml` still has no `com.google.android.gms.ads.APPLICATION_ID` meta-data tag, which UMP requires. Nothing here depends on it; the SDK-integration plan must close it.

## Self-Check

- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/AdsReadiness.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/ConsentGateway.kt` — FOUND
- Commit `ac24172` — FOUND
- Commit `6fdace6` — FOUND
- Commit `b96551d` — FOUND

## Self-Check: PASSED

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-31*
