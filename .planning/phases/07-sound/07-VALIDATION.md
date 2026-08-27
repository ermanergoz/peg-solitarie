---
phase: 7
slug: sound
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-28
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + kotlinx-coroutines-test + Turbine + Truth + MockK (all already on `testImplementation`) |
| **Config file** | Gradle `test` tasks per module — no new install needed |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*Sound*"` |
| **Full suite command** | `./gradlew check` |
| **Estimated runtime** | ~90 seconds (full `check` incl. detekt/ktlint gates) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "*Sound*"` (plus touched-module tests)
- **After every plan wave:** Run `./gradlew check`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

*To be filled by the planner — one row per task, mapping SOUND-01…SOUND-06 to automated commands or Wave 0 stubs.*

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| — | — | — | SOUND-XX | — | N/A | unit | — | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Per RESEARCH.md: ~10 new test files plus one build-gate addition; no framework install required.

- [ ] Sound event/ordinal state tests (run-index derivation, undo mirroring, ordinal carry-across-rebuild)
- [ ] Sound player gating tests (`soundOn` off silences queued one-shots — tracked stream IDs + `autoPause()`)
- [ ] `audioFocusLiteralCheck` build gate in `standards.gradle.kts` (SOUND-06: no `requestAudioFocus` call may exist)
- [ ] Fake sound-gateway wiring in existing `GameFlowTest`-style Compose tests (mirrors `LocalHapticFeedback` fake idiom)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Sound aesthetic fits candy-clay (D-04 audit) | SOUND-01..04 | Aesthetic judgement, user decision D-04 | Play the build at verify/UAT; swap duds |
| Unducked coexistence with music/podcast | SOUND-06 | Requires real device audio session | Play music, launch game, confirm music keeps playing unducked at media volume |
| First tap of session never silent | SOUND-01 | Warm-up/preload timing on real device | Cold-start app, tap immediately, confirm audible cue |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
