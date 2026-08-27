---
phase: 06-platform-foundation-build-hardening
plan: 10
subsystem: build
tags: [offline-contract, release-smoke, claude-md, DEF-06-01, PLAT-05, PLAT-07]
requires: ["06-03", "06-07", "06-08", "06-09"]
provides:
  - "D-18 offline contract proven and documented (check --offline green; assembleRelease cache-warm requirement recorded)"
  - "operator-signed release smoke test on a debug-signed, R8-minified APK"
  - "CLAUDE.md stack pins corrected (GMA Next-Gen 1.4.0, PGS 22.0.0) with traceability notes"
  - "DEF-06-01 resolved: debug hard-wired to Google test ad units"
affects:
  - "Phase 9 (reads the corrected CLAUDE.md ads row; inherits the hardened debug-ID rule)"
  - "Phase 14 (re-measures cold start; enrolls Play App Signing per D-21)"
tech-stack:
  added: []
  patterns:
    - "temporary Google-test-ID injection for smoke builds, removed immediately after with a gate re-fire check"
key-files:
  created: []
  modified:
    - .planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md
    - CLAUDE.md
    - build-logic/src/main/kotlin/AdIdResolution.kt
    - build-logic/src/test/kotlin/AdIdResolutionTest.kt
    - app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
decisions:
  - "DEF-06-01 resolved by user: debug ignores configured IDs unconditionally — no opt-in override"
  - "CLAUDE.md corrected now (not deferred): three stale rows + two adjacent mentions, traceability parentheticals added"
  - "Squash fix applied in-phase as its own commit (d2555c1) — pre-existing Candy Clay bug, not phase scope, but blocking the operator's smoke sign-off"
metrics:
  duration: ~45 min (+ user checkpoints)
  completed: 2026-08-27
  tasks: 3
  commits: 5
---

# Phase 6 Plan 10: Offline Contract, Release Smoke & Doc Corrections Summary

The phase's exit gates all closed: `check --offline` is green with the SDK artifacts on the
graph, an operator played the R8-minified release build and signed it off, and the two
standing-instruction corrections (CLAUDE.md pins, DEF-06-01 debug-ID rule) landed with
explicit user approval.

## Task Outcomes

**Task 1 — offline contract (auto).** `compose-group-mapping/2.3.20` was already cached from
06-03's online run, so no warming was needed. `check --offline` → green (344 app+domain + 7
build-logic tests). The first `assembleRelease` of the session failed in 332 ms with the D-09
gate naming all four keys — the gate's first live firing, recorded as evidence. The no-network
fallback added at plan-check time was not needed.

**Task 2 — release smoke test (checkpoint:human-verify).** Google's canonical test IDs were
temporarily placed in the untracked `local.properties` (zero ban risk), `assembleRelease`
produced a byte-identical APK to ledger row 3 (1,983,331 B), signed with the debug keystore via
`apksigner` (no `signingConfig` committed, D-16), installed as `com.erman.pegsolitarie` v3.
Automated checks: launch ok, onboarding→Home renders (fonts, gradients, DataStore write), fresh
logcat clean. **The operator's first pass found a real defect** — see below — after the fix the
operator confirmed all seven checks: "all good". IDs were removed immediately after each build
and the gate's re-firing verified both times.

**Task 3 — CLAUDE.md corrections (checkpoint:decision).** User chose "correct now". AdMob row
→ GMA Next-Gen `ads-mobile-sdk` 1.4.0 with a do-not-reintroduce warning on the legacy line;
PGS row → 22.0.0; two adjacent mentions updated. The Hilt row was already correct from v1.0.
The user also resolved **DEF-06-01** in the same sitting: debug is now hard-wired to test
units — `AdIdResolution` ignores configured IDs on debug, and the formerly-contradictory test
now asserts the new rule.

## The Smoke Test Earned Its Keep

The operator reported "when I play a peg, it appears smaller". Root cause (code-read, then
ancestry-verified): the landing squash ran inside the keyed animation effect *after*
`onAnimationFinished()` cleared `animating` — the key change restarted the effect and cancelled
the squash between shrink and settle, stranding every landed peg at `LANDING_SQUASH_SCALE`.
A **pre-existing Candy Clay bug** (candy-board commit `a565077` is an ancestor of the phase
base) — not an R8 regression, so the zero-player-visible-change contract stands. Fixed in
`d2555c1` by launching the squash on the composition scope. `check` green; the three protected
UI test files remain untouched.

## Commits

- `b198d4f` docs(06-10): offline contract, live D-09 gate firing, release smoke evidence
- `d2555c1` fix(ui): run the landing squash on a scope that survives the animation effect restart
- `408788e` fix(06-10): hard-wire debug to test ad IDs (DEF-06-01) + CLAUDE.md stack pins
- `90f4a72` / `bca2719` (shared with 06-08's session: baseline + identity, listed there)
- (this summary)
