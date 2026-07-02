---
phase: 03-challenge-generation-difficulty-curve
reviewed: 2026-07-01T00:00:00Z
depth: standard
files_reviewed: 38
files_reviewed_list:
  - app/src/main/java/com/app/pegsolitaire/data/ChallengeLevelSource.kt
  - app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt
  - app/src/main/java/com/app/pegsolitaire/data/LevelRepository.kt
  - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
  - app/src/main/java/com/app/pegsolitaire/di/DomainModule.kt
  - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
  - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/LevelRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTier.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTiers.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/FreeformFootprints.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratedLevel.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratorConstants.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeGeneratorGoldenTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/ChallengeSolvabilityTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/DifficultyTierTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/FreeformValidityTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt
findings:
  critical: 1
  warning: 3
  info: 3
  total: 7
status: resolved
resolved: 2026-07-02
---

# Phase 3: Code Review Report

**Reviewed:** 2026-07-01
**Depth:** standard
**Files Reviewed:** 38
**Status:** resolved

## Resolution (2026-07-02)

All four actionable findings were fixed and the full gate
(`ktlintCheck detekt :domain:test :app:testDebugUnitTest assembleDebug`) is green;
the golden-master fixtures (levels 1/2/10/100/1000) remain byte-identical.

- **CR-01** — `LevelGenerator.generate()` now `coerceIn(MIN_LEVEL, MAX_CHALLENGE_LEVEL=10_000)` (cap >1000, fixtures untouched), and `GameViewModel` generates the Challenge board on an injected `CoroutineDispatcher` off the main thread (Classic stays synchronous), with a loading `GameUiState`/`CircularProgressIndicator`. Fixed in `5eedff8`, `26d61a4`.
- **WR-01** — exhaustion fallback now prefers the best candidate that still clears the symmetry-canonical dedup gate. Fixed in `5eedff8`.
- **WR-02** — dead forced-line gate + `FORCED_LINE_PREFIX` + tautological test removed (the `minOpeningMoves >= 2` floor already covers it). Fixed in `5eedff8`.
- **WR-03** — a stuck Challenge board (pegsRemaining != 1) now shows the STUCK headline instead of "Solved!", consistent with the Retry action; positive+negative Results tests added. Fixed in `487039e`.
- Info items IN-01/IN-02/IN-03 remain as low-priority notes (IN-03's forced-line tautology was resolved alongside WR-02).

## Summary

The determinism substrate is genuinely well-built: every PRNG draw is against a `List`
(never a `Set`/`Map`), `stableSeed` is a portable splitmix64 over `GeneratorConstants`,
generation is single-threaded/synchronous, and solvability-by-construction is enforced
by replaying the reverse walk forward through `RulesEngine`. The golden-master lock and
the property specs (solvability, clamp, dedup window) are appropriate. Domain purity is
essentially intact (no `android.*` in `:domain`), constants/dimens/strings discipline is
followed, Compose state hoisting is correct, and no `if (challenge)` leaks into board
logic — routing stays in `LevelRepository` and the single `challengeLevel` route arg.

However, the adversarial pass surfaced one ship-blocking robustness defect and several
correctness/quality gaps that the current tests do not exercise:

- **CR-01 (BLOCKER):** `generate(n)` replays the whole `1..n` chain synchronously, with
  no upper clamp on the untrusted level number, on the main thread during ViewModel init
  — a large `challengeLevel` nav arg (or simply deep progression) is a hard ANR/hang,
  directly contradicting the "never crash the load" (T-03-09) claim.
- **WR-01:** the deterministic fallback returns a board that has passed **no** quality
  gate — including the dedup gate — so the "never-repeating within the window" core value
  can silently break.
- **WR-02:** the forced-line quality gate is dead code (always a no-op given the
  branching floor).
- **WR-03:** a stuck Challenge board (2-3 pegs) renders the "Solved!" headline while
  offering "Retry".

## Critical Issues

### CR-01: Unbounded Challenge level triggers synchronous O(n) main-thread generation (ANR / hang)

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt:57-76`, `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:91-99`, `app/src/main/java/com/app/pegsolitaire/data/ChallengeLevelSource.kt:32-46`

**Issue:** `generate` deliberately replays from level 1 to `target`:

```kotlin
for (level in MIN_LEVEL..target) {
    val tier = DifficultyTiers.forLevel(level)
    produced = generateForLevel(level, tier, forbiddenKeys = recentKeys.toHashSet())
    ...
}
```

The level number is only ever clamped on the **lower** bound (`coerceAtLeast(MIN_LEVEL)`
in both the generator, `GameViewModel.clampedChallengeLevel`, and
`GeneratedChallengeLevelSource.load`). There is no upper clamp. `challengeLevel` is an
untrusted nav argument (`Routes.kt` `Gameplay.challengeLevel: Int?`), reachable via a
forged deep link with e.g. `challengeLevel = 2_000_000_000`. That value flows straight
into `generate(2_000_000_000)`, which loops two billion times, each iteration running a
bounded reject-and-reseed loop of up to `MAX_ATTEMPTS = 1000` reverse walks with repeated
`RulesEngine.legalMoves` calls.

Critically, `initialBoard` is a **constructor-init property** of `GameViewModel`, so this
runs **synchronously on the main thread** when `hiltViewModel()` builds the VM — the UI
freezes and Android raises an ANR. The KDoc claim "Generation always terminates, so it
can never hang on an untrusted level number" is false in practice: terminating "eventually"
is not the same as not hanging. This is worse than the crash T-03-09 set out to prevent.

Even without a forged arg, the replay-from-1 design makes every legitimate load O(n): the
golden test already regenerates 1000 levels to produce level 1000, and normal progression
past a few thousand levels degrades the app into unplayability — undermining the "infinite"
core value.

**Fix:** Bound the untrusted input and get generation off the main thread. Two changes:

```kotlin
// GeneratorConstants.kt — cap the replay chain to a sane, frozen ceiling.
const val MAX_CHALLENGE_LEVEL = 10_000

// LevelGenerator.generate — clamp BOTH ends.
fun generate(levelNumber: Int): GeneratedLevel {
    val target = levelNumber.coerceIn(MIN_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)
    ...
}
```

And make the ViewModel load asynchronously with a loading UiState instead of computing
`initialBoard` in the constructor:

```kotlin
// GameViewModel — do NOT block the main thread in init.
private val _uiState = MutableStateFlow(GameUiState.loading())
init {
    viewModelScope.launch {
        val board = withContext(Dispatchers.Default) {
            clampedChallengeLevel
                ?.let { levelRepository.loadChallenge(it).initialBoard }
                ?: levelRepository.loadClassic(levelId).initialBoard
        }
        _uiState.update { GameUiState.initial(board).copy(showHints = it.showHints) }
    }
}
```

Longer term, the replay-from-1 cost should be amortized (persist/cache the rolling
`DEDUP_WINDOW` keys and the current level rather than regenerating the entire prefix on
every load) — see the deferred `ChallengeProgress`/DataStore seam.

## Warnings

### WR-01: Deterministic fallback bypasses every quality gate, including dedup

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt:90-105`

**Issue:** When all `MAX_ATTEMPTS` reseeds fail, `generateForLevel` returns `best` chosen
purely by `candidate.solution.size` — it never re-checks `passesQualityGates`:

```kotlin
if (passesQualityGates(candidate, tier, forbiddenKeys)) return candidate
val score = candidate.solution.size
if (score > bestScore) { best = candidate; bestScore = score }
...
return requireNotNull(best) { ... }
```

The fallback board can therefore violate any gate: below `minSolutionLength`, outside
`pegRange`, below `minSpread`, a forced line, and — most importantly — it can collide on
the **symmetry-canonical dedup key** with one of the previous `DEDUP_WINDOW` levels. Since
`generate` is fully deterministic, such a fallback would reproduce every run, meaning two
levels within the window could be the *same puzzle*. That directly breaks the project's #1
core value ("never-repeating"). The gate-property tests in `QualityGateTest` never hit the
fallback for their sample levels, so this is unverified latent behavior.

**Fix:** Track the best board that also *clears the dedup gate* (the non-negotiable
invariant), and prefer it. At minimum, keep the dedup constraint in the fallback path:

```kotlin
// Prefer the highest-scoring candidate that at least passes the dedup gate.
if (canonicalKeyOf(candidate.startingBoard) !in forbiddenKeys && score > bestScore) {
    best = candidate
    bestScore = score
}
```

If no non-colliding candidate exists after `MAX_ATTEMPTS`, that is a signal the tier knobs
are unsatisfiable and should surface (e.g. widen the pool) rather than silently emit a
duplicate.

### WR-02: The forced-line quality gate is dead code (always a no-op)

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/LevelGenerator.kt:113-147`

**Issue:** In `passesQualityGates`, `!isForcedLine(...)` is only evaluated (via `&&`
short-circuit) *after* `RulesEngine.legalMoves(board).size >= tier.minOpeningMoves`
succeeds, and `minOpeningMoves` is the constant `2` for every tier
(`DifficultyTiers.Curve.MIN_OPENING_MOVES = 2`). `isForcedLine` immediately returns on the
starting board:

```kotlin
for (index in 0 until prefix) {
    if (RulesEngine.legalMoves(board).size > 1) return false  // starting board has >= 2 → always false
    ...
}
```

Because the starting board always has `>= 2` legal moves when this gate is reached,
`isForcedLine` always returns `false`, so `!isForcedLine` is always `true`. The gate never
rejects anything; `FORCED_LINE_PREFIX`, `isForcedLine`, and the corresponding
`QualityGateTest.theOpeningIsNotAScriptedForcedLine` assertion are all effectively tautological
(the test can never fail). Either the gate is genuinely intended (in which case it must
check *interior* prefix positions, not re-check the opening the branching floor already
guarantees) or it is dead weight.

**Fix:** Make the gate meaningful by scanning the prefix *after* the opening, or delete it
and its constant/test to avoid a false sense of coverage. If kept:

```kotlin
// Check positions 1..prefix-1 (the opening move is already guaranteed to branch).
var board = RulesEngine.applyMove(startingBoard, solution[0])
for (index in 1 until prefix) {
    if (RulesEngine.legalMoves(board).size > 1) return false
    board = RulesEngine.applyMove(board, solution[index])
}
return prefix > 1
```

### WR-03: Stuck Challenge board shows the "Solved!" headline over a "Retry" action

**File:** `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:57-62`, `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt:44-45`

**Issue:** `headlineTier` is derived from stars alone: 2 stars (`pegsRemaining` 2-3) maps
to `SOLVED` → "Solved!". But `showNextLevel` requires `pegsRemaining == 1`, so a Challenge
board that ends **stuck** with 2 or 3 pegs shows the headline "Solved!" while simultaneously
offering the **Retry** action (implying it was *not* solved). The screen contradicts itself.
`ResultsScreenTest`/`NavFlowTest` only cover the 1-star (`STUCK` headline) stuck case and the
3-star solved case, so the 2-star stuck Challenge path is untested and slips through.

**Fix:** For Challenge, base the headline on the actual terminal outcome, not just star
count — a board that did not fold to one peg should not read "Solved!". E.g. pass the
solved/stuck fact (or `pegsRemaining == 1`) into `tierFor` for Challenge results, so a
stuck 2-star Challenge board resolves to `STUCK` ("Out of moves") consistent with the Retry
action. Classic (star-only headline, D-08) can remain as-is.

## Info

### IN-01: `java.lang.Long.numberOfTrailingZeros` in the pure-Kotlin domain module

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt:71`

**Issue:** `:domain` is specified as pure Kotlin (QUAL-01). While `java.lang.Long` is not an
`android.*` dependency (so the purity gate passes) and is deterministic on both the JVM and
ART, it is a JVM-specific API where Kotlin's stdlib offers the idiomatic, portable
equivalent. This is the only non-`android` platform call in the reviewed domain code and
is inconsistent with the rest of the module.

**Fix:** Use `Long.countTrailingZeroBits()`:

```kotlin
val bit = remaining.countTrailingZeroBits()
```

### IN-02: `ChallengeProgress` is production-dead this phase (no caller advances or observes it)

**File:** `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt:26-43`

**Issue:** `ChallengeProgress` is a `@Singleton` with `advanceToNext()`/`reset()` and an
observable `currentLevel` StateFlow, fully unit-tested — but nothing in production injects
it. `AppNavHost` hard-codes `CHALLENGE_ENTRY_LEVEL = 1` for the Home tile and the Results
"Next level" action passes `challengeLevel + 1` directly through nav args, bypassing this
holder entirely. Its KDoc claims "the UI observes it," but no composable/ViewModel collects
it. This is a deliberate Phase-5 seam, so it is acceptable to defer — flagging so it is not
mistaken for wired behavior, and so that "Play Challenge" always restarting at level 1
mid-session (even after advancing to N via Next-level) is a known consequence, not a bug.

**Fix:** No action required this phase; wire `ChallengeProgress` into the Home entry point
and the advance flow when Phase 5 (CHAL-05) lands persistence, and drop the "the UI observes
it" wording until then.

### IN-03: `QualityGateTest.theOpeningIsNotAScriptedForcedLine` is a tautology

**File:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt:62-78`

**Issue:** Mirroring WR-02, the test sets `anyBranching = true` as soon as any prefix
position has `> 1` legal moves, and the starting board always does (branching floor `>= 2`),
so the assertion `assertThat(anyBranching).isTrue()` can never fail regardless of generator
behavior. It provides no real coverage of the forced-line property.

**Fix:** Tie this to the WR-02 resolution — assert branching on an *interior* prefix
position, or remove the test alongside the dead gate.

---

_Reviewed: 2026-07-01_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
