---
phase: 1
slug: domain-foundation-playable-board
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-28
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Google Truth 1.4.x (JVM unit tests on `:domain`); ArchUnit boundary test; Roborazzi/Compose UI test deferred within phase |
| **Config file** | none — Wave 0 installs (Gradle version catalog + convention plugins) |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew check` (test + detekt + ktlint + ArchUnit boundary + custom dp/sp gate) |
| **Estimated runtime** | ~{N} seconds (planner to finalize once scaffold exists) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test`
- **After every plan wave:** Run `./gradlew check`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** {N} seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| {N}-01-01 | 01 | 1 | REQ-{XX} | T-{N}-01 / — | {expected secure behavior or "N/A"} | unit | `{command}` | ✅ / ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Planner populates this map: one row per task, every GAME-/QUAL- requirement traced to an automated command or Wave 0 dependency.*

---

## Wave 0 Requirements

- [ ] Gradle wrapper (9.1+) + version catalog (`gradle/libs.versions.toml`) + convention plugins
- [ ] JUnit4 + Truth wired on `:domain` JVM test source set
- [ ] ArchUnit (bytecode) domain-purity boundary test — zero `android.*` in domain (QUAL-01)
- [ ] `dev.detekt` 2.0.0-alpha + ktlint-gradle 14.2.0 wired as build-failing gates (QUAL-02)
- [ ] Custom no-dp/sp-literal gate (no built-in rule exists) — Konsist or Gradle verification task (QUAL-03)
- [ ] `BoardCodec` + `GoldenMasterAssert` determinism harness over `RulesEngine` (QUAL-04)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| {behavior} | REQ-{XX} | {reason} | {steps} |

*Planner: tap→highlight→jump→removal→game-over→restart UI feel may need a manual/instrumented row if not covered by Compose UI test within phase scope.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < {N}s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
