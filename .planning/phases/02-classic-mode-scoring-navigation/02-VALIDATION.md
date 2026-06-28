---
phase: 2
slug: classic-mode-scoring-navigation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-28
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.x + Turbine 1.2.1 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for JVM Compose/UI tests |
| **Config file** | Gradle (`:domain` JVM tests, `:app` unit tests) — existing |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew :domain:test :app:testDebugUnitTest ktlintCheck detekt` |
| **Estimated runtime** | ~60–120 seconds (full); ~10–20s (`:domain:test` quick) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (fast pure-domain feedback)
- **After every plan wave:** Run `./gradlew :domain:test :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite + `ktlintCheck detekt` + ArchUnit domain-purity green
- **Max feedback latency:** ~20 seconds (quick), ~120 seconds (full)

---

## Per-Task Verification Map

> Task IDs are assigned by the planner (step 8). Rows below are keyed by requirement; the planner/Nyquist auditor binds each to a concrete `{N}-PP-TT` task ID and confirms ≥1 automated verify per requirement.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | 0 | SCOR-01 | — | N/A | unit | `./gradlew :domain:test --tests *ScoreCalculatorTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | SCOR-02 | — | N/A | unit | `./gradlew :domain:test --tests *ScoreCalculatorTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | CLAS-02 | — | N/A (recorded solution → 1 peg; incl. off-center European/Diamond) | unit (golden) | `./gradlew :domain:test --tests *SolutionTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | CLAS-01 | — | catalog returns 5 descriptors in D-18 order | unit | `./gradlew :app:testDebugUnitTest --tests *LevelRepositoryTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | CLAS-02 | — | new-board geometry maps in bounds (dims 7/8/9) | unit | `./gradlew :app:testDebugUnitTest --tests *BoardGeometryTest*` | ⚠️ extend | ⬜ pending |
| TBD | TBD | 2 | NAV-01 | — | Home offers Classic + (disabled) Challenge entries | unit/UI | `./gradlew :app:testDebugUnitTest --tests *HomeViewModelTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 2 | NAV-02 | — | Home→Catalog→Gameplay→Results reachable | UI (Robolectric) | `./gradlew :app:testDebugUnitTest --tests *NavFlowTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 2 | SCOR-03 | — | Results shows stars/pegs/time/score | UI | `./gradlew :app:testDebugUnitTest --tests *ResultsScreenTest*` | ❌ W0 | ⬜ pending |
| TBD | TBD | 2 | ONB-01 | — | first launch shows overlay; markSeen hides it | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests *HomeViewModelTest*` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `domain/.../scoring/ScoreCalculatorTest.kt` — SCOR-01 star bands + SCOR-02 score formula + time-decay floor at 0
- [ ] `domain/.../golden/*SolutionTest.kt` — recorded winning line per non-trivial board → 1 peg (English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45); doubles as the D-19 solvability proof
- [ ] `domain/src/test/resources/golden/solutions/*` — committed move-list fixtures (emitted once by the authoring solver)
- [ ] One-off authoring solver (`tools/` or test source set) — generates the fixtures; **must NOT appear in `:domain` main** (ArchUnit domain-purity boundary)
- [ ] `app/.../data/LevelRepositoryTest.kt` — catalog order (D-18, 5 boards) + `loadClassic` round-trip; stubbed Challenge source contract
- [ ] `app/.../ui/components/BoardGeometryTest.kt` — extend for grid dims 8 (Asymmetric-39) and 9 (Diamond-41 / Wiegleb-45)
- [ ] `app/.../navigation/NavFlowTest.kt` — Robolectric end-to-end across the 5 destinations + selected-board nav arg
- [ ] `app/.../ui/home/HomeViewModelTest.kt` — `onboardingSeen` Flow + `markSeen` over a fake DataStore/repo

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Catalog mini board-shape previews render each footprint correctly (D-11) | CLAS-01 | Visual correctness of Canvas previews is a visual smoke test; geometry bounds are unit-tested but pixel layout is judged by eye | Launch app → Classic catalog; confirm all 5 board previews render their footprint un-clipped and centered |
| Onboarding overlay legibility / illustration clarity (D-13) | ONB-01 | Copy + illustration quality is subjective | First launch → confirm jump-rule overlay is clear and dismissable; re-open via permanent "How to play" affordance |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (ScoreCalculator, solution fixtures, LevelRepository, Nav flow, onboarding)
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
