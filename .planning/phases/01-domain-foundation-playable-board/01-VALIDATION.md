---
phase: 1
slug: domain-foundation-playable-board
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-28
updated: 2026-06-28
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Google Truth 1.4.x (JVM unit tests on `:domain`); Turbine 1.2.1 (ViewModel); Robolectric/Roborazzi + Compose UI Test (UI, JVM-side); ArchUnit boundary test |
| **Config file** | `gradle/libs.versions.toml` + `build-logic/` convention plugins + `config/detekt/detekt.yml` (installed in Plan 01 / Wave 0) |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew ktlintCheck detekt check assembleDebug` (test + detekt + ktlint + ArchUnit boundary + custom dp/sp gate + debug build) |
| **Estimated runtime** | Quick (`:domain:test`) seconds; full suite ~minutes (first build longer) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (and `:app:testDebugUnitTest` once the VM/UI exists)
- **After every plan wave:** Run `./gradlew ktlintCheck detekt check`
- **Before `/gsd:verify-work`:** `./gradlew ktlintCheck detekt check assembleDebug` must be green
- **Max feedback latency:** seconds for domain; minutes for the full Android suite

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | QUAL-01 | T-01-01 | `:domain` has no Android classpath (structural) | build | `./gradlew :domain:test assembleDebug` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | QUAL-01, QUAL-02, QUAL-03 | T-01-SC | Gates fail build on violation; named ArchUnit purity test | gate + arch | `./gradlew ktlintCheck detekt check` | ❌ W0 | ⬜ pending |
| 01-01-03 | 01 | 1 | QUAL-02 | T-01-SC | CI runs full gate on `feature/*→develop` | gate | `test -f .github/workflows/ci.yml && grep -q ktlintCheck .github/workflows/ci.yml` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 2 | GAME-01 | T-02-02 | Immutable board; no in-place mutation | unit | `./gradlew :domain:test --tests "*BoardTest"` | ❌ W0 | ⬜ pending |
| 01-02-02 | 02 | 2 | GAME-01, GAME-02 | T-02-01 | RulesEngine sole move authority; sorted enumeration | unit | `./gradlew :domain:test --tests "*RulesEngineTest"` | ❌ W0 | ⬜ pending |
| 01-02-03 | 02 | 2 | QUAL-04 | — | Deterministic golden-master over committed fixtures | unit (golden) | `./gradlew :domain:test --tests "*GoldenMasterTest"` | ❌ W0 | ⬜ pending |
| 01-03-01 | 03 | 3 | GAME-04 | — | Immutable `@Immutable` UiState (game-over as state) | compile | `./gradlew :app:compileDebugKotlin` | ❌ W0 | ⬜ pending |
| 01-03-02 | 03 | 3 | GAME-02, GAME-04, GAME-05 | T-03-01 | VM applies only `legalMoves` members; illegal tap no-op | unit + VM | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelTest"` | ❌ W0 | ⬜ pending |
| 01-04-01 | 04 | 4 | QUAL-03 | — | No raw dp/sp; ratios/durations in scoped constants | gate | `./gradlew :app:assembleDebug check` | ❌ W0 | ⬜ pending |
| 01-04-02 | 04 | 4 | GAME-03, GAME-04 | T-04-01, T-04-02 | Tap bounds-checked; no legality in Canvas | UI (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*BoardCanvas*"` | ❌ W0 | ⬜ pending |
| 01-05-01 | 05 | 5 | GAME-05 | T-05-02 | Route/Screen/Leaf; replayable game-over state | build | `./gradlew :app:assembleDebug check` | ❌ W0 | ⬜ pending |
| 01-05-02 | 05 | 5 | GAME-01, GAME-03, GAME-05 | T-05-01 | End-to-end tap→jump→game-over→restart | UI (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*GameFlowTest"` | ❌ W0 | ⬜ pending |
| 01-05-03 | 05 | 5 | GAME-03 (feel) | — | Human-perceptible jump/removal feel | manual | see Manual-Only Verifications | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Requirement coverage:** GAME-01 (01-02-02, 01-05-02) · GAME-02 (01-02-02, 01-03-02) · GAME-03 (01-04-02, 01-05-02, +manual feel) · GAME-04 (01-03-02, 01-04-02) · GAME-05 (01-03-02, 01-05-02) · QUAL-01 (01-01-01/02) · QUAL-02 (01-01-02/03) · QUAL-03 (01-01-02, 01-04-01) · QUAL-04 (01-02-03). All 9 traced to an automated command (plus a manual feel row for GAME-03 animation quality).

---

## Wave 0 Requirements

- [ ] Gradle wrapper (9.x) + version catalog (`gradle/libs.versions.toml`) + `build-logic/` convention plugins (Plan 01 Task 1)
- [ ] JUnit4 + Truth wired on `:domain` JVM test source set (Plan 01 Task 1; used Plan 02)
- [ ] ArchUnit (bytecode) domain-purity boundary test — zero `android.*`/`androidx.*` in domain, QUAL-01 (Plan 01 Task 2)
- [ ] `dev.detekt` 2.0.0-alpha.5 + ktlint-gradle 14.2.0 wired as build-failing gates, QUAL-02 (Plan 01 Task 2)
- [ ] Custom no-dp/sp-literal gate (no built-in rule) — Gradle verification task, QUAL-03 (Plan 01 Task 2)
- [ ] `BoardCodec` + `GoldenMasterAssert` determinism harness over `RulesEngine`, QUAL-04 (Plan 02 Task 3)
- [ ] Turbine + coroutines-test + MockK wired on `:app` test source set (Plan 01 Task 1; used Plan 03)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Jump/removal animation FEEL (slide smoothness, fade+shrink timing, highlight clarity) | GAME-03 | Animation quality/"satisfying feel" is perceptual; the automated `GameFlowTest` asserts state changes and dialog presence but cannot judge motion feel | Plan 05 Task 3 checkpoint: `./gradlew :app:installDebug` on a Pixel AVD, then run the 6-step playtest (tap→highlight→jump→removal→game-over→restart) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or a Wave 0 dependency (the one manual row is GAME-03 animation feel, covered automatically for state by `GameFlowTest`)
- [x] Sampling continuity: no 3 consecutive code tasks without an automated verify
- [x] Wave 0 covers all MISSING references (installed in Plans 01 & 02)
- [x] No watch-mode flags (all commands are one-shot; `--no-daemon` used in plan verifies)
- [x] Feedback latency acceptable (`:domain:test` seconds)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** planned — flips to executing as Wave 0 lands in Plan 01.
