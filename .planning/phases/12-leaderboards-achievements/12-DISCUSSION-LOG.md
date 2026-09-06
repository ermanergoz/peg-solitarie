# Phase 12: Leaderboards & Achievements - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-06
**Phase:** 12-leaderboards-achievements
**Areas discussed:** Achievement roster, Telemetry & score definitions, Offline backfill on sign-in, Entry points & popup

---

## Todo cross-reference

| Option | Description | Selected |
|--------|-------------|----------|
| Review only, don't fold | Telemetry records solved plays only; the Stuck question stays independent | ✓ |
| Fold it in | Treat Stuck-finish telemetry as a decision this phase must settle | |

**User's choice:** Review only. "Confirm audio cues" and "Wiegleb opening book" were keyword-only matches and were not offered for folding.

---

## Achievement roster

### Q1 — Where do the three skill achievements apply?

| Option | Description | Selected |
|--------|-------------|----------|
| Hint/undo-free in both modes; center on English-33 only | Center finish = English-33 last peg on (3,3) | ✓ |
| All three Classic-only | Challenge solves count for ladder and score only | |
| Center = finish on the starting hole, any board | Generalised target; needs start vacancy carried to Results | |

### Q2 — Which four are the first-hour early wins?

| Option | Description | Selected |
|--------|-------------|----------|
| First solve, first Classic board, first 3-star, Challenge level 5 | One per early surface | ✓ (later amended: "first Classic board" dropped, replaced by any per-board unlock) |
| First solve, first 3-star, hint-free, undo-free | Lean on skill achievements as early wins | |
| You decide | Planner picks four | |

### Q3 — Shape of the incremental progression achievements

| Option | Description | Selected |
|--------|-------------|----------|
| Tiered ladder + tiered stars, each tier incremental | Ladder 10/50/100, stars 25/100/250, progress bars | ✓ |
| One incremental achievement per axis | Single ladder + single stars achievement | |
| Tiered but one-shot unlocks | Same thresholds, plain unlock, no progress bar | |

### Q4 — Round out the roster (multi-select)

| Option | Description | Selected |
|--------|-------------|----------|
| Solve all five Classic boards | Grand tour | ✓ |
| 3-star every Classic board | Perfectionist (Diamond min-2 counts) | ✓ |
| Per-board first-solve achievements | Five separate unlocks | ✓ |
| Speed solve | Under a fixed time | ✓ |

### Q5 — Keep "First Classic board"? Speed threshold?

| Option | Description | Selected |
|--------|-------------|----------|
| Drop it; speed = under 60s | 20 achievements | ✓ |
| Keep both; speed = under 60s | 21 achievements | |
| Drop it; speed = under 90s | 20 achievements, gentler target | |

**User's choice:** Roster of 20; 20 icons needed (ROADMAP prerequisite row must be updated from 10–12).

---

## Telemetry & score definitions

### Q1 — Storage for per-session telemetry

| Option | Description | Selected |
|--------|-------------|----------|
| New play_session table, one row per solved play | level_result untouched; CREATE TABLE migration | ✓ |
| Boolean 'ever' flags on level_result | ALTER TABLE; loses per-play history | |
| Both | Session table plus denormalised flags | |

### Q2 — Meaning of "cumulative Challenge score"

| Option | Description | Selected |
|--------|-------------|----------|
| Sum of per-level best scores | SUM(bestScore) over level_result; consistent with Phase 13 sync | ✓ |
| Running total of every solved play | SUM(score) over play_session; rewards grinding | |

### Q3 — How telemetry reaches the Results write

| Option | Description | Selected |
|--------|-------------|----------|
| New domain SolveTelemetry with finish event + nav args | One-transaction write in recordResult | ✓ |
| Extend ScoreResult with telemetry fields | Muddles scoring with telemetry | |
| GameViewModel writes play_session itself | Splits the exactly-once guard | |

### Q4 — Counting rules for hint-free / undo-free

| Option | Description | Selected |
|--------|-------------|----------|
| Count every assist; persist counters in snapshot; restart resets | Free and rewarded both count | ✓ |
| Count assists, reset on restart only, not persisted | Process-death loophole | |
| Only rewarded assists count | Weaker mastery signal | |

---

## Offline backfill on sign-in

### Q1 — When the backfill runs

| Option | Description | Selected |
|--------|-------------|----------|
| Dirty-marker sync whenever session resolves SignedIn | Pending flag set by offline recordResult or failed submit | ✓ |
| Once ever, persisted flag | No retry after a later failure | |
| Full resubmit on every SignedIn | ~27 calls per launch | |

### Q2 — Scope of the backfill

| Option | Description | Selected |
|--------|-------------|----------|
| Both leaderboards and achievements, re-derived | One AchievementRules pass shared with live solves | ✓ |
| Leaderboards only | Offline progress never earns badges | |

### Q3 — Idempotent incremental achievements

| Option | Description | Selected |
|--------|-------------|----------|
| Add setAchievementSteps(id, steps) to the seam | Absolute steps; incrementAchievement unused or removed | ✓ |
| Keep increment; track reported steps locally | Second source of truth | |

### Q4 — Where orchestration lives

| Option | Description | Selected |
|--------|-------------|----------|
| Data-layer ProgressSubmitter observing session + results | Owns dirty flag, aggregates, rules, GamesService calls | ✓ |
| ResultsViewModel submits live; separate bootstrap backfills | Duplicated logic | |

---

## Entry points & popup

### Q1 — Where the stock UIs open from

| Option | Description | Selected |
|--------|-------------|----------|
| Two Settings rows only | "Leaderboards" and "Achievements" under the signed-in row | ✓ |
| Settings rows + Results shortcut | Signed-in-only affordance on Results | |
| Settings rows + Home button | Trophy button on Home | |

### Q2 — Rows when signed out

| Option | Description | Selected |
|--------|-------------|----------|
| Hidden; only the sign-in row shows | PGS-02 parity | ✓ |
| Shown, tapping triggers sign-in | Doubles as a nag | |

### Q3 — Stock unlock popup behaviour

| Option | Description | Selected |
|--------|-------------|----------|
| Stock popup, default position, no suppression | Phase 11 D-05 parity | ✓ |
| Stock popup, bottom gravity | setGravityForPopups(BOTTOM) | |
| Suppress stock, app-owned clay toast | Custom UI out of scope | |

### Q4 — ID plumbing

| Option | Description | Selected |
|--------|-------------|----------|
| local.properties keys -> BuildConfig via GamesIds, like AdIds | ~27 keys, release fails fast | ✓ |
| Play Console games-ids.xml per build type | IDs in source, violates PLAT-06 | |
| You decide | Planner picks | |

---

## Claude's Discretion

- Achievement names, descriptions, point values; hidden vs visible (default visible).
- Ladder aggregate source (SQL MAX over level_result vs ChallengeProgress) and exact SQL for all aggregates/predicates; DAO placement.
- play_session retention; dirty-flag storage detail; SolveTelemetry transport shape; snapshot field names; counter increment point in the assist funnel.
- showLeaderboard/showAchievements Activity mechanism; GamesIds key naming and build-logic resolution; test doubles and MigrationTest fixture; whether PlayGamesService bodies land in one plan or with first callers.
- Operator checklist additions (Console leaderboards/achievements, 20 icons, IDs into secrets, 11-HUMAN-UAT extension).

## Deferred Ideas

- Results "View leaderboard" shortcut and Home trophy button — v2 social UI.
- Bottom-gravity or custom unlock popup — v2.
- play_session pruning cap — only if profiling shows a cost.
- Additional achievement tiers with new content — v1.1 future consideration.
