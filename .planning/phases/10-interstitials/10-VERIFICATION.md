---
phase: 10-interstitials
verified: 2026-09-03T00:00:00Z
status: human_needed
score: 6/6 must-haves verified
overrides_applied: 1
overrides:
  - must_have: "A real interstitial renders on a physical device at the moment the cadence says it should, and at no other moment (10-06 plan-level truth)."
    reason: "Developer has no physical Android device available. All twelve UAT steps (including the A1 session-cap step) were run itemised on the Android emulator sdk_gphone16k_arm64 (Android 16) and the developer replied 'approved' to every step. This is recorded as an explicit, itemised deviation in 10-06-SUMMARY.md and in AppForegroundTracker.kt's KDoc (worded OBSERVED, not CONFIRMED, and names the emulator and date). Physical-device re-confirmation is carried forward as open debt for the pre-release checklist, not silently absorbed."
    accepted_by: "developer (erman) — verbatim resume signal 'approved', surface disclosed on request: 'emulator. I don't have access to a physical device.'"
    accepted_at: "2026-09-03T01:43:23+03:00"
gaps: []
human_verification:
  - test: "Re-run the 10-06 device UAT (all 12 steps, especially step 8 — the session-cap / RESEARCH assumption A1 step) on a physical Android device before release."
    expected: "Same result as the emulator run: no ad during the first session/first 5 levels, dim+ad on the 3rd eligible win, taps/back swallowed during the dim, exactly 4 interstitials per session and no 5th, no interstitial within 90s of a rewarded ad, no ad on Replay/Home/Retry/1-star exits, cadence survives a force-quit, and the dim reads as 'something is coming' rather than a broken button."
    why_human: "RESEARCH assumption A1 (ProcessLifecycleOwner not dispatching ON_STOP for a same-process fullscreen ad Activity) and the perceptual 'barely notices and never resents' judgement are not decidable by a JVM test or by static analysis - they require real ad-SDK rendering behavior and real touch/back hardware timing, which the emulator only approximates. This is explicitly named as open debt in 10-06-SUMMARY.md and AppForegroundTracker.kt's KDoc, not a silently skipped check."
---

# Phase 10: Interstitials — Verification Report

**Phase Goal:** Between-level interstitial ads with a fair cadence policy (INT-01), persistence across process death (INT-02), never on losses/exits (INT-03), and a quiescence window so taps/back presses never leak through to or from the ad (INT-04).
**Verified:** 2026-09-03
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player sees an interstitial only after tapping the Results continue action — never during the win celebration, never at level start (INT-01) | VERIFIED | `ResultsViewModel.kt`: the only two production entry points into the ad path are `onNextLevelTapped`/`onCatalogTapped` → `continueWith` → `presentThenNavigate`. Nothing runs at construction (`claimWinOnce()` only preloads, never shows). Verified in code at lines 210-267, 371-445. Test `constructingTheViewModelNeverShowsAnInterstitial` and 37 other `ResultsViewModelTest` cases pass (independently re-run: `tests="38" failures="0" errors="0"`). |
| 2 | Player who loses, abandons, or restarts a level never sees an interstitial (INT-03) | VERIFIED | `ResultsRoute.kt` (read in full): `onReplay`, `onHome`, `onRetry`, `onNavigateBack` are direct pass-throughs that never touch the ViewModel — confirmed at lines 96-101 (`onReplay = onReplay`, `onHome = onHome`, `onRetry = onRetry`, `onNavigateBack = onNavigateBack`). `isInterstitialEligible(tier) = tier != STUCK` (`ResultsViewModel.kt:443`) excludes every stuck Challenge board and every 1-star Classic finish by construction, verified directly in source, not merely claimed. |
| 3 | Interstitials are frequency-capped and fully suppressed through the first session and the first levels of a new install, and the cap survives the app being killed (INT-02) | VERIFIED | `InterstitialCadenceParams.kt`: `WINS_BETWEEN_INTERSTITIALS=3`, `MIN_GAP_MS=90_000L`, `SUPPRESSED_FIRST_LEVELS=5`, `MAX_PER_SESSION=4`, `SESSION_GAP_MINUTES=30` — the only file where these values are declared (confirmed by independent grep, matches 10-06-SWEEP-EVIDENCE.md exactly). `InterstitialCadenceStore` persists 4 of 5 scalars to `app_prefs` DataStore with `interstitialsThisSession` deliberately in-memory (D-13); a second-holder-over-same-store test proves survival. Independently re-run: `InterstitialCadenceStoreTest tests="13" failures="0"`. |
| 4 | A brief quiescence pause precedes every interstitial so a tap aimed at the board or a Results button cannot land on the ad (INT-04) | VERIFIED | `AdQuiescenceScrim.kt`: `pointerInput` consumes every event at `PointerEventPass.Initial` plus `BackHandler(enabled = true)` swallows back. Rendered as a `Scaffold` sibling (covers the `TopAppBar`), gated purely on `state.adScrimVisible`. SUMMARY documents a RED check (consumption temporarily removed, swallow test failed, restored, re-green) proving the mechanism is load-bearing, not decorative. Independently re-run: `ResultsScreenTest tests="9" failures="0"`, including the paired swallow/control tests `aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction` / `theSamePrimaryActionTapLandsWhenNoAdIsComing` (both present in source, both passing). |
| 5 | The game clock stays paused for the whole interstitial presentation (ADSF-01, carried acceptance criterion) | VERIFIED | `ResultsViewModelTest.kt:1005-1023`: `elapsedSeconds` asserted identical before the tap, during the ad (`adScrimVisible=true`), and at `pendingNavigation`. Test present and passing in the independently re-run suite. |

**Score:** 5/5 ROADMAP success criteria verified. All 4 requirement IDs (INT-01..INT-04) have direct code evidence, not just SUMMARY claims.

### Plan-Level Must-Haves (supplementary, not reducing ROADMAP scope)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 6 | Physical-device confirmation that a real interstitial renders and the session cap binds (RESEARCH assumption A1) | PASSED (override) | Ran on the Android emulator instead of a physical device — see `overrides` in frontmatter. All 12 UAT steps itemised and approved; step 8 (4 ads, no 5th) held. `AppForegroundTracker.kt` KDoc honestly says `OBSERVED` on emulator, not `CONFIRMED` on device, and names the outstanding debt. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/.../ads/InterstitialCadenceParams.kt` | D-01..D-04 tunables, KDoc'd, no Android import | VERIFIED | 68 lines, exists, `import android/androidx` grep empty, `DomainPurityTest` passes (re-run, 1/1). |
| `domain/.../ads/InterstitialCadenceState.kt` | Pure four-gate conjunction + transitions + `fromStored` | VERIFIED | 249 lines; `mayShow`, `recordWin`, `recordInterstitialShown`, `recordRewardedShown`, `startNewSession`, `isNewSession`, `fromStored` all present and delegated-to by callers. |
| `app/.../data/InterstitialCadenceStore.kt` | app_prefs-backed persistence + in-memory session count | VERIFIED | 269 lines; `state: StateFlow<InterstitialCadenceState>`, five write methods, all `storageFailSafe`-guarded (re-verified in source). |
| `app/.../platform/lifecycle/AppForegroundTracker.kt` | ProcessLifecycleOwner-driven session boundary | VERIFIED | 84 lines; `onStart`/`onStop` call the correct store methods; A1 marker resolved with itemised emulator evidence (not silently left `[ASSUMED]`). |
| `app/.../platform/ads/InterstitialAdSlot.kt` | Single-slot hold/age-out/destroy with observable readiness | VERIFIED | 243 lines; `heldFresh()` (non-suspending peek, no `await`), `ready: StateFlow<Boolean>`, one bounded retry — all confirmed present in source. |
| `app/.../platform/ads/AdGateway.kt` | Three-member interstitial seam, no GMA type | VERIFIED | `interstitialReady`, `preloadInterstitial()`, `suspend fun showInterstitial(): Boolean` all present at the exact signatures the plan specified. |
| `app/.../platform/ads/AdMobAdGateway.kt` | Real implementation + D-15 rewarded stamp | VERIFIED | `override suspend fun showInterstitial(): Boolean`, `recordRewardedShown` wired inside `onAdShowedFullScreenContent` (exactly 1 occurrence, confirmed by grep). |
| `app/.../platform/ads/NoOpAdGateway.kt` | Opposite-polarity stub | VERIFIED | `interstitialReady = MutableStateFlow(false)`, `showInterstitial() = false` — confirmed in source. |
| `app/.../ui/results/ResultsUiState.kt` | `ResultsNavTarget`, `adScrimVisible`, `pendingNavigation` | VERIFIED | 139 lines; all three present. |
| `app/.../ui/results/ResultsViewModel.kt` | Eligibility, cadence decision, ad-then-navigate sequencing | VERIFIED | 445 lines; `onNextLevelTapped`, `onCatalogTapped`, `onNavigationHandled`, `shouldShowInterstitial`, `presentThenNavigate`, `isInterstitialEligible` all present and read in full — matches plan and SUMMARY claims exactly. |
| `app/.../ui/results/AdQuiescenceScrim.kt` | Full-screen input-swallowing dim | VERIFIED | 96 lines; `awaitPointerEvent(PointerEventPass.Initial)` + `it.consume()` + `BackHandler` all present. |
| `app/.../ui/results/ResultsRoute.kt` | D-06 split (2 ad-eligible lambdas via VM, 4 direct) | VERIFIED | 104 lines, read in full; confirmed the exact split described in every SUMMARY. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `InterstitialCadenceState.kt` | `InterstitialCadenceParams.kt` | every threshold read from constants | WIRED | Confirmed by grep — every value-bearing reference outside `InterstitialCadenceParams.kt` is a qualified `InterstitialCadenceParams.<NAME>` access (10-06-SWEEP-EVIDENCE.md, independently spot-checked). |
| `InterstitialCadenceStore.kt` | `InterstitialCadenceState.kt` | all arithmetic delegated | WIRED | Store contains no `+ 1` outside the in-memory `sessionCount.update` line (verified in SUMMARY, consistent with source read). |
| `AppForegroundTracker.kt` | `InterstitialCadenceStore.kt` | onStart/onStop → store methods | WIRED | Confirmed directly in source (lines 76-83 above). |
| `ResultsViewModel.kt` | `InterstitialCadenceState.mayShow` | single cadence question, asked once per tap | WIRED | `shouldShowInterstitial()` calls `cadenceForThisResult.mayShow(...)` exactly once (source, line 237). |
| `ResultsViewModel.kt` | `AdGateway.showInterstitial` | suspend show → record on true → navigate | WIRED | `presentThenNavigate` (source, lines 257-267) matches exactly. |
| `AdMobAdGateway.kt` | `InterstitialCadenceStore.recordRewardedShown` | D-15 stamp on rewarded presentation | WIRED | Confirmed: exactly 1 occurrence, inside `onAdShowedFullScreenContent` (grep-verified). |
| `ResultsRoute.kt` | `ResultsViewModel.onNextLevelTapped/onCatalogTapped` | the two D-06 ad-eligible taps | WIRED | Confirmed in source (lines 97, 99). |
| `ResultsRoute.kt` | `ResultsUiState.pendingNavigation` | `LaunchedEffect` keyed on target, consumed once | WIRED | Confirmed in source (lines 78-92). |
| `ResultsScreen.kt` | `AdQuiescenceScrim` | rendered as `Scaffold` sibling when `adScrimVisible` | WIRED | Confirmed in source (`fun ResultsScreen` at line 101, scrim render at lines 153-157, outside `Scaffold`). |

### Behavioral Spot-Checks (independently re-run, not trusted from SUMMARY)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Cadence truth table green | `:domain:test --tests '*InterstitialCadenceStateTest'` | BUILD SUCCESSFUL | PASS |
| Cadence persistence + process-death proof | `:app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest'` | `tests="13" failures="0" errors="0"` | PASS |
| Session boundary tracker | `:app:testDebugUnitTest --tests '*AppForegroundTrackerTest'` | `tests="6" failures="0" errors="0"` | PASS |
| Stub polarity | `:app:testDebugUnitTest --tests '*NoOpAdGatewayTest'` | `tests="5" failures="0" errors="0"` | PASS |
| Results eligibility/sequencing/ADSF-01 | `:app:testDebugUnitTest --tests '*ResultsViewModelTest'` | `tests="38" failures="0" errors="0"` | PASS |
| Quiescence scrim block + control | `:app:testDebugUnitTest --tests '*ResultsScreenTest'` | `tests="9" failures="0" errors="0"` | PASS |
| Domain purity (no Android import) | `:app:testDebugUnitTest --tests '*DomainPurityTest'` | `tests="1" failures="0"` | PASS |
| SDK containment (no GMA type reaches Hilt) | `:app:testDebugUnitTest --tests '*PlatformBoundaryTest' --tests '*PlatformBoundaryGateTest'` | `tests="3"+"3" failures="0"` | PASS |
| No logging in ad/cadence/results path | `grep -rn 'android.util.Log\|Timber\|println(' <5 dirs>` | no output (exit 1) | PASS |
| No Android import in cadence policy | `grep -rn 'import android\|import androidx' domain/.../ads/` | no output (exit 1) | PASS |
| Style gates on changed modules | `:app:detekt :app:ktlintCheck :domain:detekt :domain:ktlintCheck` | BUILD SUCCESSFUL | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|--------------|--------|----------|
| INT-01 | 10-01, 10-03, 10-04, 10-05 | Interstitial only after Results continue tap, never during celebration/level start | SATISFIED | See Success Criteria row 1. |
| INT-02 | 10-01, 10-02 | Frequency-capped, suppressed first session/first levels, survives process death | SATISFIED | See Success Criteria row 3. |
| INT-03 | 10-04, 10-05 | Never follows a lost/abandoned/restarted level | SATISFIED | See Success Criteria row 2. |
| INT-04 | 10-03, 10-04, 10-05 | Quiescence pause before every interstitial, taps/back cannot leak to the ad | SATISFIED | See Success Criteria row 4. |

No orphaned requirements: REQUIREMENTS.md lists exactly INT-01..INT-04 mapped to "Phase 10," and every one is claimed in at least one plan's frontmatter `requirements:` field (10-01, 10-03, 10-04 for INT-02; 10-04, 10-05 for INT-01/03/04). ADSF-01 is explicitly a carried acceptance criterion owned by Phase 9 and re-asserted here — not an orphan, per ROADMAP's own note.

**Documentation-sync observation (not a phase-10 code gap):** `REQUIREMENTS.md`'s status table still marks INT-01..INT-04 (and every Phase 9 requirement) as "Pending" even though ROADMAP.md marks both Phase 9 and Phase 10 complete. This is a milestone-wide tracking-table staleness pattern (affects Phase 9's rows identically), not something Phase 10 introduced or something that reflects a code gap — the underlying code evidence above is what was actually checked. Flagged for correction at milestone housekeeping, not blocking this phase.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ui/results/ResultsViewModel.kt` | 312 | Pre-existing `pegsRemaining != PEGS_REMAINING_SOLVED` (`== 1` trap) guards the *bests-record* path, the same trap this phase's own KDoc (line ~428) documents and correctly avoids in the *interstitial-eligibility* gate three functions below | INFO (pre-existing, not introduced by Phase 10 — flagged by 10-REVIEW.md as CR-01) | Does not block any of the four INT-01..04 truths — it is a Phase 5 bug on the bests-recording path (a flawless Diamond-41 solve is never saved to the catalog), unrelated to ad cadence/gating logic. Confirmed via code review (10-REVIEW.md) and independently read in source. Recommend a follow-up fix, but it is out of this phase's goal scope. |
| `platform/ads/AdMobAdGateway.kt` | ~379 (interstitial), ~317 (rewarded) | Cancellation mid-presentation does not `invalidate()`/`destroy()` the shown ad instance (WR-01 in 10-REVIEW.md) | INFO/WARNING (quality) | Does not affect INT-01..04's correctness (cadence counters and navigation are unaffected); could leak an SDK resource and misreport `interstitialReady` after a rare cancellation. Not a phase-goal blocker. |
| `platform/ads/InterstitialAdSlot.kt` | 52-58, 74-87 | Threading-confinement KDoc claim sourced from the legacy GMA SDK rather than Next-Gen (WR-02) | INFO/WARNING (quality) | Documentation/verification gap on an assumption, not an observed failure; does not change any of the four INT truths. |
| `platform/ads/InterstitialAdSlot.kt` | 203-216 | 30s load retry does not re-check consent (WR-03) | INFO/WARNING (quality) | A CONS-02-adjacent concern (Phase 9 requirement), not one of INT-01..04. Does not block Phase 10 goal. |

No BLOCKER-level anti-patterns found for the INT-01..INT-04 truths themselves. No TBD/FIXME/XXX debt markers found in any phase-10 file.

### Human Verification Required

### 1. Physical-device re-confirmation of the phase 10 UAT

**Test:** Re-run all 12 steps of the 10-06 device UAT on a physical Android device, especially step 8 (the session-cap / RESEARCH assumption A1 check: 4 interstitials in one uninterrupted session, no 5th).
**Expected:** Identical results to the emulator run recorded in `10-06-SUMMARY.md` — every step "held," step 8 shows exactly 4 ads and no 5th, and step 12's "barely notices and never resents" judgement holds on real hardware.
**Why human:** RESEARCH assumption A1 (`ProcessLifecycleOwner` not dispatching `ON_STOP` for a same-process fullscreen ad Activity) and the perceptual judgement in step 12 are runtime/perceptual claims about real ad-SDK rendering and real touch/back hardware that no JVM test or static grep can decide. This is explicitly named as open, accepted debt in `10-06-SUMMARY.md` and `AppForegroundTracker.kt`'s KDoc — not a hidden gap.

### Gaps Summary

No gaps found against the phase goal. All five ROADMAP success criteria (INT-01 through INT-04, plus the carried ADSF-01 criterion) are independently verified in source code — not merely claimed by SUMMARY.md — with matching, independently re-run test results (38/38 Results tests, 13/13 store tests, 6/6 tracker tests, 9/9 screen tests, 5/5 NoOp stub tests, plus the domain truth table, all green; zero logging leaks; zero stray Android imports in the pure domain package; zero cadence-value duplication outside `InterstitialCadenceParams`). The wiring chain from tap → eligibility → cadence → quiescence scrim → ad presentation → record-on-true → navigate is traced end-to-end in source and matches the plans exactly, including the two-plan handoff where 10-04 built the ViewModel surface with (by design) no production caller yet, and 10-05 closed that gap by wiring `ResultsRoute`.

One must-have (physical-device confirmation of RESEARCH assumption A1) is accepted via override rather than failed: the developer has no physical device, ran the full itemised 12-step UAT on an emulator instead, approved every step, and this deviation is transparently recorded in both `10-06-SUMMARY.md` and the `AppForegroundTracker.kt` KDoc (worded `OBSERVED`, not `CONFIRMED`). This is carried forward as a pre-release human-verification item rather than silently absorbed, which is why phase status is `human_needed` rather than `passed`.

Four INFO/WARNING-level findings from the phase's own code review (10-REVIEW.md) are noted for completeness but judged not to block the phase goal: CR-01 (a pre-existing, non-Phase-10 bug on the bests-record path, not the ad-eligibility path — the two were explicitly checked to be different code paths), and three ad-platform-code quality warnings (WR-01 cancellation cleanup, WR-02 threading-contract sourcing, WR-03 consent re-check on retry) that concern robustness/compliance nuances adjacent to Phase 9's CONS-02, not any of the four INT-01..04 truths this phase's goal is scoped to.

---

_Verified: 2026-09-03_
_Verifier: Claude (gsd-verifier)_
