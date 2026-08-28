---
phase: 8
slug: assist-generator-quality-debt
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-08-29
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Populated from `08-RESEARCH.md` §Validation Architecture; plan/wave mapping from `08-01..11-PLAN.md`.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Google Truth 1.4.x (`:domain`, pure JVM); Robolectric in `:app` |
| **Config file** | Gradle version catalog + `domain/build.gradle.kts` (`UPDATE_GOLDEN` heap hook) — no separate runner config |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` |
| **Estimated runtime** | quick: seconds (sweep excluded by default); full: minutes; sweep (`-PrunSweep=true`): ~6 min pre-book at K=4, far less post-book |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (sweep excluded by default — Pitfall 5)
- **After every plan wave:** Run `./gradlew ktlintCheck detekt :domain:test :app:test`
- **Before `/gsd:verify-work`:** Full suite must be green **plus one explicit sweep run** (`-PrunSweep=true`)
- **Authoring (not a gate):** `UPDATE_GOLDEN=true ./gradlew :domain:test` with `maxHeapSize = "8g"` — minutes, run deliberately, never in CI
- **Max feedback latency:** ~30 seconds for the per-commit gate

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-02-* | 02 | 1 | DEBT-01 | — | N/A | property | `./gradlew :domain:test --tests '*CanonicalTest*'` | ❌ W0 | ⬜ pending |
| 08-03-* | 03 | 1 | DEBT-03 | — | N/A | doc gate | manual + `./gradlew check` | n/a | ⬜ pending |
| 08-05-* | 05 | 3 | DEBT-01 | T-04-SD | Node cap + deadline retained; per-attempt cap tightens DoS bound | unit | `./gradlew :domain:test --tests '*SolverTest*' --tests '*SolverBudgetTest*'` | ✅ | ⬜ pending |
| 08-06-* | 06 | 2 | DEBT-02 | — | N/A | golden replay | `./gradlew :domain:test --tests '*SolutionTest*'` | ✅ extend | ⬜ pending |
| 08-06-* | 06 | 2 | DEBT-02 | — | N/A | invariant (GF(4), F−1 unreachable) | `./gradlew :domain:test --tests '*PagodaTest*'` | ✅ extend | ⬜ pending |
| 08-07-* | 07 | 3 | DEBT-02 | — | N/A | unit (Diamond star bands 2→3★, 3–4→2★, ≥5→1★) | `./gradlew :domain:test --tests '*ScoreCalculatorTest*'` | ✅ extend | ⬜ pending |
| 08-07-* | 07 | 3 | DEBT-02 | Tampering (forged resume blob) | `minimumPegs in 1..spec.size` validated on decode; snapshot rejected otherwise | unit (Robolectric) | `./gradlew :app:test --tests '*SnapshotMapperTest*' --tests '*ResumeRepositoryTest*'` | ✅ extend | ⬜ pending |
| 08-09-* | 09 | 5 | DEBT-01 | Tampering (corrupt book resource) | Fail-closed parse: malformed line disables whole book, falls through to Solver | golden integrity | `./gradlew :domain:test --tests '*OpeningBookTest*'` | ❌ W0 | ⬜ pending |
| 08-10-* | 10 | 6 | DEBT-01 | T-04-CT | Every book move re-validated via `RulesEngine.legalMoves` before return | unit | `./gradlew :domain:test --tests '*GetHintUseCaseTest*'` | ✅ extend | ⬜ pending |
| 08-10-* | 10 | 6 | DEBT-01 | — | N/A | exhaustive sweep (D-08) | `./gradlew :domain:test --tests '*DepthSweepTest*' -PrunSweep=true` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*
*Wave 1 plans 08-01 (BoardSpec field) and Wave 2 plan 08-04 (SolverTables/Pagoda) verify through the same `SolverTest`/`PagodaTest`/`CanonicalTest` suites listed above.*

---

## Wave 0 Requirements

- [ ] `DepthSweepTest.kt` — DEBT-01's D-08 universal claim; must be property-gated out of the fast run
- [ ] `OpeningBookTest.kt` — book integrity (every entry replays to `minimumPegs`)
- [ ] `CanonicalTest.kt` — bit-count-preserving / idempotent canonicalisation across all five footprints (Pitfall 4)
- [ ] `BeamSearchAuthoring.kt` + `OpeningBookAuthoring.kt` (test source set) — fixture generators
- [ ] Raise `UPDATE_GOLDEN` `maxHeapSize` 4g → 8g in `domain/build.gradle.kts`
- [ ] Gradle property/task wiring so the sweep runs in `check` but not in `:domain:test`

*Framework install: none — JUnit4 + Truth already declared in `domain/build.gradle.kts`.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Phase 3/4 contract reconciliation sign-off | DEBT-03 | Doc-gate judgment call (plan 08-03) | Reconcile `03-VALIDATION.md` / `04-VALIDATION.md` Per-Task Verification Maps against shipped tests; flip their `nyquist_compliant` flags |
| On-device hint latency + book retention in release APK | DEBT-01 | Requires physical device / release build (plan 08-11, `checkpoint:human-verify`) | Install release APK; request hints from hard early positions on E-37/A-39/W-45; confirm real move within deadline, no rescue banner |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (plan-checker verified, 0 blockers)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (three ❌ W0 files listed above)
- [x] No watch-mode flags
- [x] Feedback latency < 30s for the per-commit gate (sweep excluded)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-08-29 (plan-checker pass; warning 3 noted — 08-10's sweep verify is deliberately slow, phase-gate only)
