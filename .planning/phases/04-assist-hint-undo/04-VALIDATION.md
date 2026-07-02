---
phase: 4
slug: assist-hint-undo
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-02
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 (JVM unit) + kotlinx-coroutines-test + Turbine + MockK + Truth; Compose UI Test + Roborazzi (JVM/Robolectric) |
| **Config file** | Gradle (`:domain` JVM tests, `:app` unit + `testDebugUnitTest` for Robolectric) |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew test detekt ktlintCheck` |
| **Estimated runtime** | ~60–120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (Solver/use-case tasks) or the affected module's unit tests
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd:verify-work`:** `./gradlew test detekt ktlintCheck` must be green
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

> Filled by the planner from RESEARCH.md §Validation Architecture. Solver correctness + budget behavior are the highest-value gates (D-04).

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 4-XX-XX | XX | 0 | ASST-01/02/03 | — | N/A | unit | `./gradlew :domain:test` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `SolverTest` (`:domain`) — solve-correctness on catalog boards + budget behavior (returns null only when genuinely unsolvable within tuned budget; the D-04 completeness corpus)
- [ ] `GetHintUseCaseTest` (`:domain`) — recorded-line-relative → Solver fallback → null pipeline (ASST-01, ASST-03)
- [ ] `GameViewModelTest` (`:app`, Turbine/coroutines-test) — hint state, thinking flag, no-win flag, undo, stuck-rescue gating (ASST-02, ASST-03)
- [ ] Compose UI / semantics tests — source+dest highlight, "thinking" state, inline no-win banner, undo button

*Refined by the planner against the actual task list.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Hint highlight visual polish (glow + A→B arrow) | ASST-01 | Visual affordance quality is subjective beyond semantics/Roborazzi | Launch app, request hint on/off the recorded line, confirm source+dest legibility |

*Solver correctness/budget, undo, and stuck-rescue all have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
