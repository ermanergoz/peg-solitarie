---
phase: 3
slug: challenge-generation-difficulty-curve
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-01
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Derived from `03-RESEARCH.md` §Validation Architecture. Per-task rows are
> populated by the planner/executor once PLAN.md task IDs exist.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Google Truth 1.4.x (pure-JVM `:domain`); Robolectric / Compose UI test in `:app` for wiring |
| **Config file** | Gradle version catalog (`gradle/libs.versions.toml`) — no separate test-runner config |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` (established Phase-1/2 gate) |
| **Estimated runtime** | ~quick: seconds (JVM `:domain`); full gate: ~minutes (incl. `assembleDebug`) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (fast JVM; includes golden-master + solvability invariants)
- **After every plan wave:** Run `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Hard ordering constraint (roadmap risk note):** golden-master fixtures for levels 1/2/10/100/1000 committed and CI-green **BEFORE any difficulty-knob tuning task starts** — non-negotiable
- **Max feedback latency:** quick suite (`:domain:test`) is the per-commit gate

---

## Per-Task Verification Map

> Task IDs (`3-NN-NN`) are assigned during planning. Rows below are keyed by
> requirement + test type from research; the planner maps each to its owning task.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | 0/1 | CHAL-03 | — | Level N byte-identical to committed fixture | golden-master (JVM) | `./gradlew :domain:test --tests '*ChallengeGeneratorGoldenTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0/1 | CHAL-02 | — | Every sampled solution folds to one peg, each step legal | invariant (JVM) | `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0/1 | CHAL-03 | — | Regenerating level N twice ⇒ identical `Board.pegs` + solution | determinism (JVM) | `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | CHAL-04 | — | Difficulty knobs monotonic/continuous over N; freeform gated by intro level | tier table (JVM) | `./gradlew :domain:test --tests '*DifficultyTierTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | CHAL-04 | — | Quality gates reject trivial/degenerate/duplicate boards deterministically | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 1 | CHAL-01 | — | Freeform footprints connected + ≤64 cells | validity (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | 2 | CHAL-01 | T-3 V5 | `levelNumber` nav arg validated/clamped (≥1); advance-on-solve ⇒ N+1; stuck ⇒ Retry only (no Next) | ViewModel/nav (Robolectric) | `./gradlew :app:test` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ChallengeGeneratorGoldenTest.kt` + committed fixtures `challenge_level_{1,2,10,100,1000}.txt` — CHAL-03 (**must land first**)
- [ ] `ChallengeSolvabilityTest.kt` — solvability-by-construction + regeneration determinism, CHAL-02/CHAL-03
- [ ] `DifficultyTierTest.kt` — tier structure/monotonicity, CHAL-04
- [ ] `QualityGateTest.kt` — reject-and-reseed determinism + freeform validity (connected, ≤64), CHAL-04/CHAL-01
- [ ] `:app` Challenge wiring test — `levelNumber` validation, advance-on-solve, stuck→Retry, CHAL-01/D-01/D-10
- [ ] Reuse (no new infra): `BoardCodec`, `GoldenMasterAssert`, `UPDATE_GOLDEN` workflow already exist

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Difficulty *feels* like a gentle on-ramp then smooth continuous climb (D-05/D-06) | CHAL-04 | Knob VALUES are a playtest-iteration loop (D-07) — cannot be set by static analysis | Play the generated ladder across level ranges (1–10, ~50, ~100, ~1000); confirm "trickier before bigger"; tune tier table, then re-freeze `generatorVersion` |
| Freeform shapes read as intentional/aesthetic where cheap (D-09) | CHAL-01 | Subjective visual quality | Spot-check generated freeform boards at high levels for symmetry/appeal |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (golden-master fixture lands before any tuning)
- [ ] No watch-mode flags
- [ ] Feedback latency: `:domain:test` is the per-commit gate
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
