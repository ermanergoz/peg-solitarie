# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — Slice 1: Playable Core

**Shipped:** 2026-08-26
**Phases:** 5 | **Plans:** 31 | **Timeline:** 60 days (2026-06-28 → 2026-08-26), 235 commits

### What Was Built
- Pure-JVM `:domain` (bitmask Board, RulesEngine, generator, Solver, ScoreCalculator) + Compose `:app` — ~17.6k LOC Kotlin, 293 JVM tests
- The core value: deterministic reverse-generation Challenge generator, always-solvable by construction, frozen behind golden-master fixtures
- Full game loop across 5 Classic boards + infinite Challenge, with hints/undo/no-win rescue, scoring, and migration-safe persistence (bests + resume-by-replay + settings)

### What Worked
- **Quality gates from commit 0** (detekt/ktlint/dp-sp/ArchUnit-purity in `check`) — never retrofitted, caught violations at commit time all milestone
- **Repository seams planned ahead of consumers** — LevelRepository (P2), ChallengeProgress (P3 in-memory → P5 DataStore), AdGateway (P4) absorbed every later phase without rework; the "in-memory holder shaped as the persistence seam" trick made P5's swap invisible to callers
- **Golden-master fixtures as contracts** — locked the board encoding (P1) and the generator output (P3); difficulty tuning after the freeze couldn't silently change shipped levels
- **Wave-parallel executors in git worktrees** with per-wave merge + full-check gate — phase 5's 7 plans landed in 4 waves with zero merge conflicts (one wave serialized proactively on a shared strings.xml)
- **Post-execution audit stack paid off**: code review found 2 real crash/correctness bugs (Continue crash-loop, ladder double-advance) and the security audit found a genuinely missing mitigation (unguarded Room reads) — all fixed same-day with regression tests

### What Was Inefficient
- Phase 5 execution was interrupted mid-plan-01 and sat for ~6 weeks; resume required forensic reconstruction of uncommitted Gradle changes (they matched Task 1 exactly, but only verifiable by re-deriving the plan's acceptance criteria)
- Research recommendations occasionally wrong at the mechanism level (Robolectric can't see unit-test asset srcDirs; the `MigrationTestHelper` file-path overload isn't an escape hatch) — cost an executor a deep detour, though the write-up became a reusable rule
- `develop` went stale after phase 3 — phase branches chained off each other instead of merging back, requiring a milestone-close Gitflow repair
- Two quick-task artifacts used a nonstandard SUMMARY name, tripping the milestone-close artifact audit with false positives

### Patterns Established
- Untrusted-state rule: anything deserialized (snapshots, route args) is re-validated through RulesEngine or clamped at the VM boundary — never trusted
- Fail-open reads / best-effort writes for all persistence (`.catch` + defaults on reads, `storageFailSafe` on writes, CancellationException always rethrown)
- Robolectric asset rule: assets a JVM test must read live on `main`/`debug` source sets, never test srcDirs
- Checkpoint plans (`autonomous: false`) for device-only behavior (haptics, process-death resume) — JVM tests cover logic, humans verify hardware
- One executor per plan in an isolated worktree; orchestrator owns STATE/ROADMAP writes and the post-merge test gate

### Key Lessons
1. Verified-fix loops beat trust: every subagent claim (tests green, threats closed) was re-verified by the orchestrator running the gate itself — twice this caught nothing, once it mattered (stale REQUIREMENTS.md).
2. The 1.5s solver deadline is a product decision, not a bug — but it should be revisited with better pruning before rewarded-ad hints make "no winning move" feel like a paid dead end (Slice 2).
3. Commit partial work before pausing a phase — uncommitted-but-correct work is recoverable only by luck.

### Cost Observations
- Model mix: executor/fixer/security-auditor on opus, verifier/integration-checker on sonnet (quality profile)
- Phase 5 close (execute + review + fix + secure + audit): 12 subagents across one session
- Notable: wave parallelism halved wall-clock on waves 1–2; the serialized wave 3 cost little because plans were small

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Phases | Plans | Days | Notable process change |
|-----------|--------|-------|------|------------------------|
| v1.0 | 5 | 31 | 60 | Established: gates-from-commit-0, worktree wave execution, post-phase review/secure/audit stack |
