---
phase: 10-interstitials
plan: 06
subsystem: ads
tags: [interstitial, uat, verification, detekt, ktlint, processlifecycleowner, admob, gate]

# Dependency graph
requires:
  - phase: 10-interstitials
    plan: 01
    provides: "InterstitialCadenceParams and InterstitialCadenceState — the single home of the five cadence values invariant 1 checks, and the pure policy invariant 4 checks for Android imports"
  - phase: 10-interstitials
    plan: 02
    provides: "AppForegroundTracker and its [ASSUMED] A1 marker — the one claim in the phase no JVM test can reach, and the file this plan edits"
  - phase: 10-interstitials
    plan: 03
    provides: "the AdGateway interstitial slot and AdMobAdGateway whose real render the UAT exercises"
  - phase: 10-interstitials
    plan: 04
    provides: "isInterstitialEligible and the Diamond-41 trap KDoc that invariant 5 checks at package level"
  - phase: 10-interstitials
    plan: 05
    provides: "AdQuiescenceScrim and the rewired ResultsRoute — the dim and the swallowed tap UAT steps 4, 5 and 10 exercise"
  - phase: 06-ads-play-integration
    provides: "AdIds -> BuildConfig debug test ad units and verifyReleaseAdIds, which make the UAT serve Google test inventory (T-10-04)"
provides:
  - "The phase-10 gate evidence: ./gradlew check and :app:assembleDebug both green, recorded verbatim in 10-06-SWEEP-EVIDENCE.md"
  - "The five cross-cutting invariants checked and their exact grep output written down in a re-runnable form"
  - "An empirical result for RESEARCH assumption A1 — 4 interstitials in one session, no 5th — replacing the [ASSUMED] marker in AppForegroundTracker's KDoc"
  - "An itemised 12-step UAT record, and a named open debt: that record was produced on an emulator, not a physical device"
affects: [11-release-prep, uat-audits, verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "A phase-closing invariant is only useful if its grep is written down in a form that can be re-run against a later change — and if the plan's grep cannot express the invariant, the corrected grep is what gets recorded"
    - "An assumption resolved by observation is stamped with the surface it was observed on; an emulator result is recorded as an emulator result, not promoted to a device result"

key-files:
  created:
    - .planning/phases/10-interstitials/10-06-SWEEP-EVIDENCE.md
    - .planning/phases/10-interstitials/10-06-SUMMARY.md
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt

key-decisions:
  - "Invariant 1's acceptance grep was corrected rather than forced green: the plan's pattern conflates constant VALUES with constant NAMES, so it can only pass if nothing references InterstitialCadenceParams at all. The value-only grep is recorded as the re-runnable form."
  - "The UAT was accepted on the Android emulator because the developer has no physical device. Recorded as an explicit deviation and as still-open debt in both the SUMMARY and the AppForegroundTracker KDoc, rather than being written up as if a device had been used."
  - "A1's KDoc resolution says OBSERVED, names the emulator and the date, and states that physical-device confirmation remains outstanding — an assumption downgraded to evidence, not upgraded to proof."

patterns-established:
  - "Itemised checkpoint records: step number -> observed outcome, answering the standing STATE.md caution that Phase 7's audit was globally approved rather than itemised"
  - "Verification-surface honesty: the environment a manual check ran on is part of the result, and a substituted environment is a deviation even when the check passes"

requirements-completed: [INT-01, INT-02, INT-03, INT-04]

# Metrics
duration: 48min
completed: 2026-09-03
---

# Phase 10 Plan 06: Phase Gate and Device UAT Summary

**The whole phase-10 gate is green, the five cross-cutting invariants hold with their grep output written down verbatim, and RESEARCH assumption A1 is now an observation rather than an assumption — 4 interstitials in one session and no 5th — but observed on the Android emulator, not the physical device the plan required.**

## Performance

- **Duration:** ~48 min across two agent sessions (Task 1 sweep, checkpoint pause for the developer's UAT, Task 2 resolution)
- **Started:** 2026-09-02 (Task 1 gate sweep)
- **Completed:** 2026-09-03T01:43:23+03:00
- **Tasks:** 2 (1 auto, 1 human-verify checkpoint)
- **Files modified:** 1 source file (`AppForegroundTracker.kt`), 2 planning artifacts created

## Accomplishments

- `./gradlew check` and `./gradlew :app:assembleDebug` both exit 0 — the gate includes `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck` and the full JVM unit suite across both modules, confirmed by reading the task graph rather than assuming it.
- All five cross-cutting invariants checked and **held**, with every command and its exact output recorded in [`10-06-SWEEP-EVIDENCE.md`](./10-06-SWEEP-EVIDENCE.md).
- The 12-step UAT was run by the developer and every step reported as holding, including step 8 — the session-cap step that no automated test in this phase can reach.
- `AppForegroundTracker`'s `[ASSUMED]` A1 marker is gone, replaced by the observed result stamped with the surface it was observed on.
- The one thing this plan could not deliver is named rather than buried: physical-device confirmation.

## Task Commits

1. **Task 1: Full gate sweep and the five cross-cutting invariants** — `721bd89` (docs; no source change was required — the gate was already green and every invariant already held)
2. **Task 2: Device UAT verdict processing** — `0c10a6f` (docs: resolve the `[ASSUMED]` A1 marker with the emulator UAT observation)

**Plan metadata:** this SUMMARY (docs: complete plan)

## Files Created/Modified

- `.planning/phases/10-interstitials/10-06-SWEEP-EVIDENCE.md` — the verbatim gate output, the `check --dry-run` task graph, the debug `BuildConfig` ad-unit confirmation, and all five invariant greps with their outputs
- `app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt` — `[ASSUMED]` A1 paragraph replaced with the `OBSERVED` result, the emulator model, the date, and the outstanding physical-device debt

## Task 1 — the gate and the five invariants, itemised

| Gate / invariant | Result | Evidence |
|------------------|--------|----------|
| `./gradlew check` | **PASS** — exit 0, 11m 16s | Full output in SWEEP-EVIDENCE § "The build gate". DepthSweep numbers byte-identical to the DEBT-01 residual STATE.md already carries (440 Diamond-41, 4 Wiegleb-45 positions outside their books) — this run neither widened nor narrowed that gap. |
| `./gradlew :app:assembleDebug` | **PASS** — exit 0 | APK at `app/build/outputs/apk/debug/app-debug.apk`, 20,301,522 bytes. |
| `check` composition | **CONFIRMED, not assumed** | `check --dry-run` shows all five literal checks plus `detekt`/`ktlintCheck` wired into `check` for **both** `:app` and `:domain`, alongside both test tasks. This is what makes invariant 2's claim ("`adIdLiteralCheck` ran") checkable. |
| 1 — cadence values live in exactly one file | **HELD** (grep corrected — see below) | Every match outside `InterstitialCadenceParams.kt` is a qualified `InterstitialCadenceParams.<NAME>` reference. Filtering those leaves zero. `AdConstants.kt`, the file the plan singles out, has zero matches. |
| 2 — no ad-unit literal under `src/main` | **HELD** | `:app:adIdLiteralCheck` and `:domain:adIdLiteralCheck` both in the green `check` graph. The only place test IDs exist is the generated `BuildConfig`, produced from `buildConfigField` in `app/build.gradle.kts:185-187` — build script, not `src/main`. |
| 3 — nothing in the ad or cadence path logs | **HELD** — 0 matches | The directory sweep is deliberately **wider** than the seven named files: all of `platform/ads/`, all of `platform/lifecycle/`, all of `ui/results/`, plus `InterstitialCadenceStore.kt`. No ad response info, consent status or cadence identifier can reach logcat (ASVS V7). |
| 4 — cadence policy imports no Android type | **HELD** — 0 matches | Textual grep clean; `DomainPurityTest` (ArchUnit, bytecode-based) green inside `check`. `SystemClock` is mechanically unreachable, which is why `mayShow` takes `nowMs`. |
| 5 — no `pegsRemaining == 1` test on the interstitial gate | **HELD** — 8 occurrences enumerated, none on the path | Two are `const val` declarations, three are KDoc prose naming the trap, one is `recordSolvedPlayOnce`'s D-08 solve guard, one is `tierFor`'s Challenge-stuck branch (guarded by `challengeLevel != null`, so never a Classic board and never Diamond-41), one is `ResultsUiState.showNextLevel`. The gate itself is `tier != ResultsHeadlineTier.STUCK` — no peg comparison, no star comparison. |

### The one correction Task 1 made

Invariant 1's acceptance criterion expects `grep -rln '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000'` to list exactly one path. It lists three. The pattern conflates the constants' **values** with their **names**: a file that correctly reads `InterstitialCadenceParams.MAX_PER_SESSION` necessarily contains the string `MAX_PER_SESSION`, so the criterion as written can only pass if nothing ever references the params object — which would defeat the invariant. The invariant's actual claim was checked directly and holds; the re-runnable form recorded for future changes is:

```bash
grep -rn '90_000\|WINS_BETWEEN\|MAX_PER_SESSION\|SUPPRESSED_FIRST\|1_800_000\|30 \* 60' app/src/main domain/src/main \
  | grep -v '^domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt:' \
  | grep -v 'InterstitialCadenceParams\.[A-Z_]'
```

Empty output is the invariant holding. This is a correction to the *check*, not to a *decision* — no design was changed to make a gate green.

## Task 2 — the UAT, itemised

**Developer's resume signal, verbatim:** `approved`

**Surface actually used:** Android **emulator**, AVD `sdk_gphone16k_arm64`, **Android 16**. When asked, the developer stated: *"emulator. I don't have access to a physical device."*

**Debug build, so Google test inventory only** — `INTERSTITIAL_AD_UNIT_ID = ca-app-pub-3940256099942544/1033173712` in the generated debug `BuildConfig`, verified in Task 1. T-10-04 (invalid traffic from the deliberate tapping in step 5) therefore cannot attach to real inventory.

| # | What the step checks | Decision | Reported outcome |
|---|----------------------|----------|------------------|
| 1 | Launch, accept consent | Phase 9 | Held — consent prompt appeared and was accepted. |
| 2 | 5 Challenge solves: **no ad, no dim, no delay** | D-03 | **Held — no interstitial and no dim observed** at any point across all 5 levels or any Next-level tap. |
| 3 | Home, clock +31 min, reopen — ends session 1 | D-04 | Held — session boundary taken. |
| 4 | 3 more solves, 3rd win's "Next level": ~0.5 s dim, then a fullscreen test ad | D-01, D-02, D-08 | Held — the dim appeared (no text, no spinner) and the test interstitial rendered over it. |
| 5 | Rapid taps + back press **during** the dim do nothing and do not land on the ad | INT-04 | Held — no button reacted, back did nothing, and no tap leaked through to the ad. |
| 6 | Dismiss: next level loads immediately, board fresh, timer starts at 00:00 **at that moment** | ADSF-01 | Held — timer did not run behind the ad. |
| 7 | One more solve + "Next level": **no ad** | D-01, D-02 | **Held — no interstitial and no dim observed** (win counter reset, 90 s gate still closed). |
| 8 | **The A1 step.** 4 interstitials in one session without a 30-min background, then keep playing — **no 5th** | D-04, RESEARCH A1, T-10-11 | **Held — 4 interstitials seen in the single session, and no 5th appeared.** The developer confirmed this step was actually run, not assumed. |
| 9 | Rewarded ad, then a win + "Next level" within 90 s: **no interstitial** | D-15 | **Held — no interstitial observed**; the rewarded ad opened the same 90-second cooldown. |
| 10 | Replay, Home, a 1-star Classic Catalog exit, and a stuck-Challenge Retry: **no ad, no dim, instant nav** on all four | D-06, INT-03 | **Held — no interstitial and no dim observed** on any of the four; navigation was immediate with no perceptible delay. |
| 11 | Solve, swipe from recents, relaunch within minutes: progress preserved, no immediate ad, no restarting the 3-win count | INT-02 | Held — cadence progress survived the force-quit. |
| 12 | **Judgement:** did any ad feel resented? Did the dim read as a broken button? | ROADMAP governing sentence | Approved with no complaint recorded. The developer's verdict was `approved` with no qualifier about feel — the dim was not reported as reading like a dead button, and no ad was reported as resented. |

**Step 8 did not fail.** A 5th ad would have invalidated A1 and blocked the phase; four-then-none is the result the design needs. `ProcessLifecycleOwner` did not forge a background event while the ad Activity was showing, so the D-04 per-session cap binds in practice and not merely by construction.

## Decisions Made

- **Correct the invariant-1 grep rather than the code.** The criterion could not express its own invariant; the corrected value-only grep is recorded in SWEEP-EVIDENCE as the form to re-run. No design decision was altered to make a gate green.
- **Accept the emulator UAT, and record it as an emulator UAT.** The developer has no physical device; refusing the result would have blocked the phase on hardware that does not exist, and silently writing it up as a device run would have manufactured evidence. The result is recorded as evidence with its surface named, and the physical-device run is carried forward as debt.
- **Word the A1 KDoc as `OBSERVED`, not `CONFIRMED`.** The emulator runs the same `androidx.lifecycle` dispatch code as a device, so the observation is real evidence — but it is not the physical-device confirmation the plan asked for, and the source that depends on A1 now says so in as many words.

## Deviations from Plan

### 1. [Accepted by user — verification surface] The UAT ran on an emulator, not a physical device

- **Found during:** Task 2 (the human-verify checkpoint)
- **Issue:** The plan requires the UAT on a **physical device** and explicitly excludes the emulator, citing STATE.md's standing caution that Phase 7's manual audit was emulator-only. Acceptance criterion 1 of Task 2 reads "A fresh debug APK is installed on a physical device (device model recorded in the summary)."
- **What happened:** The developer ran all twelve steps on the Android emulator `sdk_gphone16k_arm64` (Android 16) and replied `approved`. Asked directly about the surface, they stated they have no access to a physical device.
- **Resolution:** Accepted, and recorded as a deviation rather than absorbed. The behaviour under test — `ProcessLifecycleOwner`'s `ON_STOP` dispatch across a same-process fullscreen ad Activity — is `androidx.lifecycle` code that runs identically on the emulator, so the step-8 result is meaningful evidence for A1. What the emulator cannot stand in for is real hardware timing, real ad-SDK rendering behaviour under memory pressure, and the perceptual judgement of step 12 on a real screen.
- **Files affected:** `AppForegroundTracker.kt` — the KDoc names the emulator, the date, and the outstanding debt, so the source that depends on A1 does not overstate its evidence.
- **Committed in:** `0c10a6f`
- **Residual:** physical-device UAT of steps 4, 5, 8 and 12 (see Next Phase Readiness).

### 2. [Rule 3 - Blocking, check-level] Invariant 1's acceptance grep does not express invariant 1

- **Found during:** Task 1
- **Issue:** The literal acceptance grep matches constant *names* as well as *values*, so compliant references to `InterstitialCadenceParams` make it fail.
- **Fix:** The invariant's actual claim was checked with a value-only grep that filters qualified references; it returns empty. Both the original and corrected forms are recorded verbatim in SWEEP-EVIDENCE.
- **Files modified:** none — this was a check correction, not a code change.
- **Committed in:** `721bd89`

---

**Total deviations:** 2 — one user-accepted verification-surface substitution, one check-level correction.
**Impact on plan:** No design decision was changed and no source behaviour was altered. Deviation 1 leaves a named residual; deviation 2 leaves a better invariant than the plan specified.

## Issues Encountered

None during execution. The gate was green on first run and no invariant required a fix, which is the expected shape of a phase-closing sweep when the five preceding plans did their work.

## User Setup Required

None from this plan. The production interstitial ad unit remains a ROADMAP External Prerequisite and a human/AdMob-console task (T-10-21) — `verifyReleaseAdIds` fails a release build fast if it is absent, so no release can ship without it. This UAT ran entirely on Google's debug test unit and does not and cannot cover the production ID.

## Next Phase Readiness

**Ready.** Phase 10 closes with the gate green, the invariants written down in a re-runnable form, and A1 backed by observation instead of assumption.

**Open debt carried forward — physical-device UAT of phase 10.**

The twelve-step UAT above was executed on the Android emulator `sdk_gphone16k_arm64` (Android 16) because the developer has no physical device. This **extends** the standing STATE.md caution that Phase 7's audio audit was emulator-only; phase 10's ad audit now sits alongside it. Unlike Phase 7's, this one is itemised step-by-step rather than globally approved, which was the other half of that caution.

Before release, re-run on real hardware — the four steps whose evidence the emulator cannot fully supply:

- **Step 8 (highest value):** the session cap. A 5th interstitial in one session on a device would mean `ProcessLifecycleOwner` is the wrong session-boundary source (T-10-11) and the D-04 cap never binds. The emulator says it binds; hardware should say the same before players do.
- **Step 4:** a real interstitial rendering on real hardware, with real fill latency behind the 500 ms dim.
- **Step 5:** input swallowing under real touch hardware and real ad-Activity transition timing.
- **Step 12:** the "barely notices and never resents" judgement, which is a perceptual claim about a real screen in a real hand and is the sentence the ROADMAP actually governs the phase by.

Also still open and unchanged by this plan: the production interstitial ad unit (T-10-21) and the DEBT-01 DepthSweep residual (440 Diamond-41 and 4 Wiegleb-45 positions outside their books), which this run confirmed neither widened nor narrowed.

## Known Stubs

None. This plan added no code path; its only source change is documentation inside an existing KDoc.

## Threat Flags

None. This plan introduced no new network endpoint, auth path, file access pattern or schema change. It added no dependency (T-10-SC).

## Self-Check: PASSED

- `FOUND: .planning/phases/10-interstitials/10-06-SWEEP-EVIDENCE.md`
- `FOUND: .planning/phases/10-interstitials/10-06-SUMMARY.md`
- `FOUND: app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt`
- `FOUND: 721bd89` — docs(10-06): record the gate result and the five cross-cutting invariants
- `FOUND: 0c10a6f` — docs(10-06): resolve the [ASSUMED] A1 marker with the emulator UAT observation
- `grep -c ASSUMED app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt` → `0` (Task 2 acceptance criterion)
- `:app:detekt` and `:app:ktlintCheck` re-run green against the edited file (detekt report mtime 01:42:58 > source mtime 01:42:51, so the report is of the edited file)

---
*Phase: 10-interstitials*
*Completed: 2026-09-03*
