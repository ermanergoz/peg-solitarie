# Phase 6 — Security Advisory Closure Evidence (DEBT-04 / D-13)

**Purpose:** the v1.0 milestone audit carried two advisories forward as debt. D-13 locks
their closure shape: **bound or validate at the boundary, and prove with a regression test
that a forged value cannot get through.** A fix without the proving tests does not close
the advisory — the audit's finding was precisely that these paths lacked proof.

This file is the committed record of that proof.

---

## WARN-02 — resume-snapshot footprint coordinates

**Status:** CLOSED (plan 06-02)

### Advisory

Original text: `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-SECURITY.md:87-102`
(carried forward in `.planning/milestones/v1.0-MILESTONE-AUDIT.md`).

Abridged: *"`SnapshotMapper.kt:146-161` validates tuple arity, footprint size (1..64),
duplicates, target membership and peg-mask width, but never the coordinate values. A
tampered blob containing `[-5, 3]` or `[1073741824, 0]` passes validation (`BoardSpec`
imposes no constraints), producing a degenerate off-canvas board. … Suggested fix: bound
row/col in `toPositionOrNull()` against a named constant."*

### Trust boundary

Stored Room TEXT is untrusted input: a rooted device or a restored backup can carry an
arbitrary payload. The blob crosses into domain types here:

```
Room blob (TEXT)
  └─ SnapshotMapper.decode(blob)            → Json.decodeFromString (already fail-safe)
       └─ SnapshotMapper.fromDto(dto, rulesEngine)
            └─ toBoardSpecOrNull()                        SnapshotMapper.kt:146-161
                 └─ inPlay.mapNotNull { it.toPositionOrNull() }   ← the gap, now closed
```

### Bound chosen, and why not a tighter one

`0 until ResumeSnapshotFormat.MAX_BOARD_POSITIONS` — that is `0..63`.

`MAX_BOARD_POSITIONS` is `Long.SIZE_BITS`, and it already existed
(`ResumeSnapshotDto.kt:96-100`) because a `Board`'s peg mask is a single `Long`. **No new
numeric constant was introduced** (CLAUDE.md forbids magic numbers).

This is the tightest *sound* bound. A 64-cell footprint could in principle be a 1x64 line,
so `63` is the largest coordinate any legal board could carry. A tighter bound — e.g. `9`,
the largest current `GRID_DIMENSION` across `Boards.kt` — would be tighter today but would
fail **closed** the moment a future board grows, silently destroying a player's
in-progress game. That trade is recorded as T-06-05 in the plan's threat register and is
pinned by the `[63, 63]` regression case below.

### Fail-safe, not fail-fast

The guard returns `null`; it never throws. `toBoardSpecOrNull`'s existing
`positions.size == inPlay.size` check (`SnapshotMapper.kt:152`) escalates one rejected
coordinate into rejection of the whole snapshot, and the caller surfaces that as "no
resume" — the Continue affordance simply disappears and the player gets a fresh board.
This extends the philosophy `SnapshotMapper.decode` (`:113-123`) already applies to
malformed JSON.

### Fix location

| What | Where |
|------|-------|
| Guard | `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt:109-134` (`toPositionOrNull`, declared at `:127`) |
| Constant reused | `ResumeSnapshotDto.kt:96-100` (`ResumeSnapshotFormat.MAX_BOARD_POSITIONS`) |
| Escalation to whole-snapshot rejection | `SnapshotMapper.kt:152` — **unchanged**, no edit needed |

`SnapshotMapper.kt` was deliberately **not** modified. `targetRow`/`targetCol` are read raw
at `SnapshotMapper.kt:148-150` and do not flow through `toPositionOrNull`; they are already
constrained by `target in positions` at `:155`. That guard is **verified** by a regression
case rather than re-guarded, so there is no second bound that could drift out of sync
(T-06-06, disposition: accept).

### Regression cases

All six assert at the **public decode boundary** (`encode` → `decode` → `fromDto`, via the
test file's `restore()` helper), never on the internal `toPositionOrNull`. The tests
therefore prove the *system* rejects the forgery, not that one helper does.

File: `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`

| Test | Forged / stored value | Expectation |
|------|----------------------|-------------|
| `aNegativeFootprintCoordinateYieldsNoResume` | `inPlay` contains `[-5, 3]` | rejected → `null` |
| `anAbsurdlyLargeFootprintCoordinateYieldsNoResume` | `inPlay` contains `[1073741824, 0]` (`2^30`) | rejected → `null` |
| `aFootprintCoordinateOnePastTheBoundYieldsNoResume` | `inPlay` contains `[64, 0]` | rejected → `null` |
| `aFootprintCoordinateAtTheInclusiveBoundStillDecodes` | `inPlay` contains `[63, 63]` | **accepted** — the bound is not over-tight |
| `anOutOfRangeTargetYieldsNoResume` | `targetRow = 64`, `targetCol = 0` | rejected → `null` (pre-existing guard, verified) |
| `aPreviouslyValidEnglish33SnapshotStillDecodesToTheSameBoard` | a real English-33 in-progress state | **decodes to the same board** — the non-regression proof |

The three rejection cases were written **first** and observed failing against the
unbounded decoder (commit `eb706d1`), then made to pass by the fix (commit `de5a1c3`).
That RED observation is what makes them regression tests rather than assertions of
present behaviour.

### Command

```bash
./gradlew :app:testDebugUnitTest --tests "*SnapshotMapperTest"
```

`SnapshotMapperTest` went from **11** to **17** tests; no pre-existing test was modified to
accommodate the change. The stored blob format is unchanged, so there is no Room migration
and no schema version bump.

---

## WARN-03 — the public `challengeLevel` getter is unclamped

**Status:** OPEN — placeholder for plan 06-06.

Advisory text: `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-SECURITY.md`
(same section). `GameViewModel.kt:125` exposes the raw route arg when no restore stamp
exists, and `GameRoute.kt:69,88` carries it into the `Results` route. Closure follows the
same D-13 shape — validate at the route boundary, prove with a regression test — and is
recorded here by plan 06-06.
