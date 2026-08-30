---
phase: 9
slug: consent-rewarded-ads
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-30
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Robolectric/Roborazzi (JVM), Turbine, MockK, Truth |
| **Config file** | `app/build.gradle.kts` (existing test setup) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "<changed test class>"` |
| **Full suite command** | `./gradlew check` (offline-capable invariant must hold) |
| **Estimated runtime** | ~120 seconds full suite |

---

## Sampling Rate

- **After every task commit:** Run the quick command scoped to the touched test classes
- **After every plan wave:** Run `./gradlew check`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| *(filled by planner — see RESEARCH.md requirement→test map and its 7 Wave-0 test files)* | | | | | | | | | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Wave-0 test files per RESEARCH.md `## Validation Architecture` (7 identified: allowance policy, snapshot codec compat, ViewModel dialog gating with fake gateway, ADS-06 search-first ordering, clock-freeze, settings privacy rows, unservable fallback)
- [ ] JVM suite must remain offline-capable — no UMP/GMA classes reachable from Robolectric-run `App.onCreate()`

*Existing infrastructure (JUnit4/Robolectric/Turbine/MockK) covers the frameworks; no new installs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| *(filled by planner from RESEARCH.md device-verification checklist: real consent form in EEA debug-geography, test rewarded ad end-to-end, privacy options form, reward-earned callback timing)* | CONS-01/02/03, ADS-05 | Requires real UMP consent message + test ad units on device | See RESEARCH.md manual checklist |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
