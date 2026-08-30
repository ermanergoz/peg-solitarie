---
phase: 3
slug: challenge-generation-difficulty-curve
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-01
reconciled: 2026-08-29
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

> Populated 2026-08-29 by v1.1 Phase 8 plan 08-03 (DEBT-03). Every row names the plan
> that produced it, the file that exists on disk, and a scoped command that was
> executed and exited 0 before this contract was signed off.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-02 | 02 | 2 | CHAL-03 | T-03-06 | Levels 1/2/10/100/1000 byte-identical to the committed golden fixtures (v1 output frozen) | golden-master (JVM) | `./gradlew :domain:test --tests '*ChallengeGeneratorGoldenTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeGeneratorGoldenTest.kt` | ✅ green |
| 03-01 | 01 | 1 | CHAL-02 | — | Every sampled solution folds its starting board to one peg, each step legal via `RulesEngine` | invariant (JVM) | `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeSolvabilityTest.kt` | ✅ green |
| 03-01 | 01 | 1 | CHAL-03 | T-03-03 | Regenerating level N twice ⇒ identical `Board.pegs` + solution | determinism (JVM) | `./gradlew :domain:test --tests '*ChallengeSolvabilityTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeSolvabilityTest.kt` | ✅ green |
| 03-01 | 01 | 1 | CHAL-04 | — | Difficulty knobs monotonic/continuous over N; freeform gated by intro level | tier table (JVM) | `./gradlew :domain:test --tests '*DifficultyTierTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTierTest.kt` | ✅ green |
| 03-02 | 02 | 2 | CHAL-04 | T-03-04 | **Trivial** boards rejected: `solution.size >= tier.minSolutionLength` and opening branching factor `>= tier.minOpeningMoves` for every sampled level | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt` | ✅ green |
| 03-02 | 02 | 2 | CHAL-04 | T-03-04 | **Degenerate** boards rejected: peg count inside the tier density band and bounding-box spread `>= tier.minSpread` | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt` | ✅ green |
| 03-02 | 02 | 2 | CHAL-04 | T-03-04 | **Duplicate** boards rejected: no two starting boards inside a `DEDUP_WINDOW` of consecutive levels share a symmetry-canonical `pegs` key, across a ≥40-level consecutive span | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt` | ✅ green |
| 03-02 | 02 | 2 | CHAL-04 | T-03-04 | Reject-and-reseed is **deterministic**: the bounded rejection loop terminates for every level, and regenerating a consecutive span twice yields the identical ordered sequence of starting boards | gates (JVM) | `./gradlew :domain:test --tests '*QualityGateTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt` | ✅ green |
| 03-02 | 02 | 2 | CHAL-01 | T-03-05 | Freeform footprints orthogonally connected + ≤64 in-play cells (invalid shapes rejected, never rendered) | validity (JVM) | `./gradlew :domain:test --tests '*FreeformValidityTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/FreeformValidityTest.kt` | ✅ green |
| 03-04 | 04 | 4 | CHAL-01 | T-03-09 | Forged `challengeLevel = 0` nav arg clamps to level 1 at the `GameViewModel` boundary (no crash, plays the nearest legal level) | ViewModel (Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt` | ✅ green |
| 03-05 | 05 | 5 | CHAL-01 | T-03-10 | Solved Challenge ⇒ single primary "Next level" advancing to N+1; stuck ⇒ "Retry" only (no Next); Home tile enters Challenge; nav carries only the Int level | ViewModel/nav (Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*ResultsViewModelTest*' --tests '*NavFlowTest*'` | ✅ `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt` | ✅ green |
| 05-06 ➕ | 06 (v1.0 Phase 5) | 3 | CHAL-05, PERS-02 | — | Challenge solve advances the **persisted** `currentLevel`; Home re-enters at the stored level | ViewModel (Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelChallengeAdvanceTest*'` | ✅ `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt` | ✅ green |
| 06-06 ➕ | 06 (v1.1 Phase 6) | 2 | CHAL-01 | T-06-21, T-06-22 | `ChallengeLevelBounds` is the single source of the level range; every clamp site (generator, data source, both ViewModels) shares it and is fail-safe at both ends (`0`/`Int.MIN_VALUE` → MIN, `Int.MAX_VALUE` → `MAX_CHALLENGE_LEVEL`) | bounds (JVM) | `./gradlew :domain:test --tests '*ChallengeLevelBoundsTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeLevelBoundsTest.kt` | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · ➕ test that exists but the original draft contract never named*

**Cross-phase rows (➕).** The last two rows are not Phase-3 deliverables; they are later
work that covers Phase-3 contract behaviour and was never mapped back. `05-06` supplied
the durable half of advance-on-solve (Phase 3 shipped it in-memory only); `06-06`
consolidated the `challengeLevel` clamp that Phase 3 introduced ad hoc at the
`GameViewModel` boundary (v1.1 DEBT-04/WARN-03). They are listed so the reader can see
every command that actually covers this phase's behaviour, with the owning plan named.

**`FreeformValidityTest` re-pointing.** The draft's CHAL-01 freeform row pointed at
`*QualityGateTest*`; `FreeformValidityTest.kt` is the file 03-02 actually produced for
connectivity + the ≤64-cell bound, so the row now names it.

---

## Wave 0 Requirements

*All six items verified present on disk and green on 2026-08-29 (DEBT-03 audit, v1.1 Phase 8 plan 08-03).*

- [x] `ChallengeGeneratorGoldenTest.kt` + committed fixtures `challenge_level_{1,2,10,100,1000}.txt` — CHAL-03 (**must land first**) → `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeGeneratorGoldenTest.kt`, fixtures in `domain/src/test/resources/golden/`
- [x] `ChallengeSolvabilityTest.kt` — solvability-by-construction + regeneration determinism, CHAL-02/CHAL-03
- [x] `DifficultyTierTest.kt` — tier structure/monotonicity, CHAL-04
- [x] `QualityGateTest.kt` — reject-and-reseed determinism (trivial/degenerate/duplicate legs), CHAL-04
- [x] Freeform validity (connected, ≤64) — `FreeformValidityTest.kt`, CHAL-01 *(the draft folded this into `QualityGateTest`; 03-02 shipped it as its own file)*
- [x] `:app` Challenge wiring test — `levelNumber` validation (`GameViewModelTest`), advance-on-solve + stuck→Retry (`ResultsViewModelTest`, `NavFlowTest`), CHAL-01/D-01/D-10
- [x] Reuse (no new infra): `BoardCodec`, `GoldenMasterAssert`, `UPDATE_GOLDEN` workflow already exist

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Difficulty *feels* like a gentle on-ramp then smooth continuous climb (D-05/D-06) | CHAL-04 | Knob VALUES are a playtest-iteration loop (D-07) — cannot be set by static analysis | Play the generated ladder across level ranges (1–10, ~50, ~100, ~1000); confirm "trickier before bigger"; tune tier table, then re-freeze `generatorVersion` |
| Freeform shapes read as intentional/aesthetic where cheap (D-09) | CHAL-01 | Subjective visual quality | Spot-check generated freeform boards at high levels for symmetry/appeal |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies — every row above carries a scoped command
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (all five plans 03-01…03-05 are represented)
- [x] Wave 0 covers all MISSING references (golden-master fixtures landed in 03-02, before any knob tuning)
- [x] No watch-mode flags — every command is a one-shot `./gradlew` invocation
- [x] Feedback latency: `:domain:test` is the per-commit gate (full `:domain:test` measured at ~15 s cold on 2026-08-29)
- [x] `nyquist_compliant` flag set to `true` in frontmatter

**Approval:** approved 2026-08-29 by v1.1 Phase 8 / DEBT-03 (plan 08-03). Every
`Automated Command` in the Per-Task Verification Map above was executed in this worktree
and exited 0 before these boxes were ticked:
`./gradlew :domain:test --tests '*ChallengeGeneratorGoldenTest*' --tests '*ChallengeSolvabilityTest*' --tests '*DifficultyTierTest*' --tests '*QualityGateTest*' --tests '*ChallengeLevelBoundsTest*' --tests '*FreeformValidityTest*'`
and
`./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*' --tests '*GameViewModelChallengeAdvanceTest*' --tests '*NavFlowTest*' --tests '*ResultsViewModelTest*'`.
No box is ticked for anything not actually run.
