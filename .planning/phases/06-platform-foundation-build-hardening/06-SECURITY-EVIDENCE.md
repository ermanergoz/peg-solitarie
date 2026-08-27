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

**Status:** CLOSED (plan 06-06)

### Advisory

Original text: `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-SECURITY.md`
(same section as WARN-02), carried forward in `.planning/milestones/v1.0-MILESTONE-AUDIT.md`.

Abridged: *"`GameViewModel.kt:125` exposes `challengeLevel = stampedChallengeLevel ?:
routeChallengeLevel` — the raw route arg when no restore stamp exists — and
`GameRoute.kt:69,88` passes it into the `Results` route and the screen title. … an
out-of-range value would be recorded in the DB under the raw level id and drive 'Next
level' as `raw + 1`, while the board actually played was the clamped one. … Suggested fix:
have `GameRoute` read the clamped value."*

### Trust boundaries — there are two, not one

The advisory names one. Reading the nav graph found a second: `Results` is a typed route in
`AppNavHost` and is reachable **without** going through `GameRoute`, so its own
`challengeLevel` arg is untrusted in its own right rather than merely downstream of any
`GameViewModel` fix. Fixing only the getter would have left the DB key reachable.

| Boundary | Untrusted input | Where it lands |
|----------|-----------------|----------------|
| nav route arg → `GameViewModel` | `Gameplay(challengeLevel = …)`, any `Int` | the exposed getter → `GameRoute` → the `Results` route + the screen title |
| nav route arg → `ResultsViewModel` | `Results(challengeLevel = …)`, any `Int` | `ProgressRepository.recordResult(levelId = …)` and `uiState.challengeLevel`, which `ResultsActions.kt:54` turns into `onNextLevel(challengeLevel + 1)` |

A third, quieter path was covered too: a resume snapshot's **provenance stamp** beats the
route arg (WR-03), and stored state on a rooted device or in a restored backup is no more
trusted than a nav arg. It was already clamped on assignment; a regression case now pins
that so the getter cannot be un-clamped again through that door.

### The fix, and why it is subtraction rather than a new guard

`GameViewModel` already held the correct value. `clampedChallengeLevel` clamps the route
arg, the stamp is clamped on assignment, and `activeChallengeLevel` combines them — its own
KDoc reads *"Everything level-keyed after construction reads THIS."* The public getter was
the single thing that did not. So the fix restores an existing invariant instead of adding a
third clamp expression that could drift:

```kotlin
val challengeLevel: Int? get() = activeChallengeLevel
```

`ResultsViewModel` got the same shape rather than four inline `coerceIn`s: **one** clamped
private value, read by all four sites.

| What | Where |
|------|-------|
| Getter delegates to the clamped value | `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:139` |
| Route-arg clamp | `GameViewModel.kt:148` |
| Restore-stamp clamp | `GameViewModel.kt:406` |
| The one clamped Results value | `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:75` |
| …read at the headline tier | `ResultsViewModel.kt:88` |
| …read at `uiState.challengeLevel` (drives "Next level") | `ResultsViewModel.kt:91` |
| …read at the mode branch | `ResultsViewModel.kt:116` |
| …read at the **database level key** | `ResultsViewModel.kt:121` |

`GameRoute.kt` was **not** modified. The advisory's suggested fix ("have `GameRoute` read the
clamped value") would have put the guard in the consumer, leaving the next consumer to
repeat it; fixing the producer covers the title, the Results route and every future reader
at once.

### D-15 — the consolidation that removed the fourth clamp site

Closing WARN-03 the naive way would have written the bound out a **fourth** time. It was
already duplicated three times, which is both a QUAL-03 / CLAUDE.md no-magic-numbers
violation and — for a *security* bound — three copies free to drift apart (T-06-21).

New: `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/ChallengeLevelBounds.kt`
— `MIN` (`:31`), a `MAX` **delegating** to `GeneratorConstants.MAX_CHALLENGE_LEVEL` (`:38`),
and `clamp(Int)` (`:56`). Pure Kotlin, zero Android deps. `GeneratorConstants.kt` and
`DifficultyTiers.kt` were deliberately left untouched — the ceiling keeps its existing
load-bearing KDoc in the frozen-generator-contract file, and `MAX` delegates rather than
relocating it.

| Duplicate removed | Now calls |
|---|---|
| `LevelGenerator.MIN_LEVEL` (was `:287`) | `ChallengeLevelBounds.clamp` at `LevelGenerator.kt:64`, `ChallengeLevelBounds.MIN` at `:75` |
| `ChallengeLevelSource.MIN_CHALLENGE_LEVEL` (was `:55`) | `ChallengeLevelSource.kt:39` |
| `GameViewModel.MIN_CHALLENGE_LEVEL` (was the companion at `:839`) | `GameViewModel.kt:148` and `:406` |

**`DifficultyTiers.Curve.MIN_LEVEL` is deliberately NOT absorbed.** It lives inside the
explicitly playtest-tunable `Curve` knob block as the origin of the difficulty ramp
(`fromStart = level - MIN_LEVEL`), which is a different concept from "the lowest legal level
number", and the two must be free to be tuned independently. That exclusion is recorded in
`ChallengeLevelBounds`' own KDoc so a later reader does not read it as an oversight, and it
is the one line the consolidation grep is expected to still return:

```bash
grep -rn "const val MIN_CHALLENGE_LEVEL\|const val MIN_LEVEL" app/src/main domain/src/main --include="*.kt"
# → domain/.../generator/DifficultyTiers.kt:82 only
```

### Fail-safe, not fail-closed

`clamp` coerces; it never throws and never refuses the load (T-06-22). An out-of-range
value plays the **nearest legal level**, matching what
`GeneratorConstants.MAX_CHALLENGE_LEVEL` already documents and what the resume codec
already does with stored moves. Clamping rather than rejecting is also what makes the
headline claim true: because the played board and the recorded key now read the *same*
clamped value, they cannot name different levels.

### Regression cases

Every assertion is on an **observable outcome** — the value the ViewModel exposes, or the
`levelId` string handed to `ProgressRepository` — never on a private field.

| Test | File | Forged value | Expectation |
|------|------|--------------|-------------|
| `aForgedHugeChallengeLevelArgIsExposedAsTheFrozenCeiling` | `GameViewModelTest.kt` | route arg `Int.MAX_VALUE` | `challengeLevel == MAX_CHALLENGE_LEVEL`, **and** equals the level the repository was asked to load |
| `aNegativeChallengeLevelArgIsExposedAsTheLowestValidLevel` | `GameViewModelTest.kt` | route arg `-1` | `challengeLevel == ChallengeLevelBounds.MIN` |
| `challengeLevelBelowOneIsClampedWithoutCrashing` | `GameViewModelTest.kt` | route arg `0` | `challengeLevel == ChallengeLevelBounds.MIN` |
| `anInRangeChallengeLevelArgIsExposedUnchanged` | `GameViewModelTest.kt` | route arg `10` | unchanged — **the non-regression proof** |
| `aForgedSnapshotStampStillWinsTheRouteButIsItselfClamped` | `GameViewModelResumeTest.kt` | stored stamp rewritten to `Int.MAX_VALUE` | stamp beats the route arg *and* is clamped to the ceiling |
| `aForgedHugeChallengeLevelArgIsRecordedUnderTheClampedLevelKey` | `ResultsViewModelTest.kt` | `Results` arg `Int.MAX_VALUE` | recorded `levelId == "10000"`; `uiState.challengeLevel` clamped, so "Next level" cannot exceed `MAX + 1` |
| `aChallengeLevelArgBelowOneIsRecordedUnderTheLowestValidLevelKey` | `ResultsViewModelTest.kt` | `Results` arg `0` | recorded `levelId == "1"`; `uiState.challengeLevel == 1` |
| `aClassicResultStaysClassicWhenTheChallengeArgIsAbsent` | `ResultsViewModelTest.kt` | arg absent | mode stays Classic, `uiState.challengeLevel` stays `null` — the clamp does not manufacture a level out of `null` |
| 4 range/clamp cases | `ChallengeLevelBoundsTest.kt` (`:domain`) | `0`, `-5`, `Int.MIN_VALUE`, `Int.MAX_VALUE`, in-range | the policy object itself |

Five of these were written **first** and observed failing against the unclamped code
(commit `1e66039`), then made to pass by the fix (commit `7548b99`) — that RED observation
is what makes them regression tests rather than assertions of present behaviour. The other
four are deliberate non-regression locks and passed on both sides, as intended.

`GameViewModelTest`'s existing `challengeLevelBelowOneIsClampedWithoutCrashing` **was**
modified: it asserted `challengeLevel == 0`, i.e. it encoded the vulnerability while its own
name claimed the opposite. Its assertion now matches its name. No other pre-existing
assertion was touched.

### Commands

```bash
./gradlew :domain:test --tests "*ChallengeLevelBoundsTest"
./gradlew :app:testDebugUnitTest --tests "*GameViewModelTest" --tests "*GameViewModelResumeTest" --tests "*ResultsViewModelTest"
./gradlew check
```

`check` is green at **320 JVM tests**, up from the 309 baseline at this plan's base commit
(+4 `:domain`, +7 `:app`). Nothing is persisted differently, so there is no Room migration
and no schema version bump — the clamp only affects which *key* a result is written under,
and only for values that were never legal.

---

## Both DEBT-04 advisories are closed

| Advisory | Boundary bounded | Proof | Plan |
|----------|------------------|-------|------|
| WARN-02 | decode boundary (`ResumeSnapshotDto.toPositionOrNull`) | 6 cases in `SnapshotMapperTest`, 3 observed RED first | 06-02 |
| WARN-03 | route boundary ×2 (`GameViewModel.challengeLevel`, `ResultsViewModel`) | 9 cases across 4 files, 5 observed RED first | 06-06 |

Both were closed in the D-13 shape — **bound at the boundary, prove with a regression test
that a forged value cannot get through** — which satisfies phase success criterion 5. The
v1.0 audit's finding was that these two paths lacked proof; this file is that proof, and it
is committed alongside the code rather than living in a build log.
