# Pitfalls Research

**Domain:** Native Android peg-solitaire game — procedural (reverse-generated) levels, Jetpack Compose rendering, Clean Architecture/MVVM, AdMob + Play Games Services v2
**Researched:** 2026-06-28
**Confidence:** HIGH for generation/architecture/Compose reasoning (verified against published peg-solitaire math + official Android/Compose docs); HIGH for AdMob/PGS policy mechanics (official help docs); MEDIUM where the recommendation is a judgement call (difficulty tuning, Room-vs-DataStore).

This catalogue is deliberately specific to *this* project. Generic advice ("write tests", "handle errors") is omitted in favour of failure modes that are unique to reverse-generated peg solitaire, Compose `Canvas` game rendering, StateFlow-driven MVVM, and the Slice 2 SDK integrations.

---

## Critical Pitfalls

### Pitfall 1: Non-deterministic generation despite a "seeded" PRNG

**What goes wrong:**
Level N renders differently on different devices, different OS versions, or after a code refactor — even though generation is "seeded by level number." Retry/resume/leaderboard comparisons silently break because "level 5" is no longer one fixed board.

**Why it happens:**
The PRNG itself is deterministic, but the *consumption order* is not. Reverse-generation enumerates candidate reverse-moves and picks one with the PRNG. If candidates come from a `HashSet<Move>`/`HashMap` or `Set` whose iteration order is unspecified, the PRNG index maps to a different move per run. Other silent sources: `String.hashCode()` is stable across JVMs but a custom `hashCode()` or `Object.hashCode()` (identity) is not; relying on `Set.toList()` ordering; parallel/coroutine-dispatched generation; or seeding from `hash(N)` using a non-portable hash.

**How to avoid:**
- Enumerate candidate moves into a **deterministically sorted `List`** (e.g. sort by `(row, col, direction)`) before every PRNG draw. Never draw an index against an unordered collection.
- Use `kotlin.random.Random(seed)` or `java.util.Random(seed)` (both have a *specified* algorithm) — not `Random.Default`/`ThreadLocalRandom`.
- Derive the seed with a portable, pinned hash (e.g. a fixed splitmix/`Long`-mixing of N, or `Random(N.toLong())` directly). Avoid `Object.hashCode`, avoid `UUID`, avoid system time.
- Generate single-threaded and synchronously for a given level.
- **Golden-master test:** snapshot the board + solution for levels 1, 2, 10, 100, 1000 to a fixture; CI fails if regeneration ever differs. This is the single most valuable test in the project.

**Warning signs:**
A generation unit test that passes locally but flakes in CI; "the board looked different yesterday"; any use of `HashSet`/`HashMap`/`buildSet` in the generator hot path; `forEach` over a `Map` to pick a move.

**Phase to address:** Challenge generator phase (Slice 1). Lock the golden-master test before any difficulty tuning.

---

### Pitfall 2: Changing the generator after release silently rewrites every Challenge level

**What goes wrong:**
A post-launch tweak to difficulty knobs, reverse-walk logic, or seed derivation changes the output of level N for everyone. Players' in-progress resumes, recorded best times, stars, and (Slice 2) leaderboard entries now refer to a *different* board than the one that produced them. "Level 5 is always the same board" — the project's headline promise — is broken retroactively.

**Why it happens:**
Determinism-by-seed is treated as automatically stable, but it is only stable *for a fixed algorithm*. Any change to candidate ordering, knob curves, or footprint selection is a breaking change to the entire infinite level set.

**How to avoid:**
- Treat the generator as a **versioned, frozen contract**. Stamp every generated/saved level with a `generatorVersion`.
- Make difficulty parameters **data-driven** (a table/config) so early tuning happens *before* the version is frozen — the spec already calls for this; honour it.
- After first public release, never mutate v1 in place. Ship `generatorV2` as a parallel algorithm and decide a migration policy (e.g. v2 only applies to levels above the player's current max, or starts a new "season").
- Persist the *board itself* for an in-progress resume (Pitfall 11), not just the level number, so a generator change can't corrupt a save mid-flight.

**Warning signs:**
Roadmap tasks like "improve difficulty curve" scheduled *after* launch with no version gate; no `generatorVersion` field in the persistence model; resume logic that regenerates from N rather than restoring stored cells.

**Phase to address:** Generator phase (introduce `generatorVersion` from day one); Persistence phase; revisited at Slice 2 leaderboard design.

---

### Pitfall 3: Reverse-generation produces trivial, degenerate, or duplicate boards

**What goes wrong:**
Reverse-generation guarantees *a* solution exists, but not that the puzzle is *good*. Common failures: (a) the board is solvable in a glaringly obvious near-linear line (trivial); (b) pegs form a single dense clump with the target hole adjacent (degenerate / no challenge); (c) two different level numbers generate the *same* starting board (violates "unique"); (d) the starting position is actually already near-solved.

**Why it happens:**
A pure random reverse walk tends to "undo" moves locally, producing compact, low-branching boards. Uniqueness across seeds is assumed, never enforced — birthday-paradox collisions are real for small footprints with few pegs (early levels have a small board state space).

**How to avoid:**
- Add **quality gates** in the generator's validation step, rejecting-and-reseeding (deterministically: derive a sub-seed sequence from N) until the board passes:
  - minimum reverse-move count (solution length) scaled by level → guarantees non-trivial depth;
  - spatial spread metric (pegs not all in one corner/clump);
  - branching-factor / "first-move-count" floor so the opening isn't forced;
  - reject boards where the recorded solution's first K moves are the *only* legal moves (degenerate forced line).
- **Enforce uniqueness explicitly:** keep a canonical-form hash (normalize for board symmetry/rotation/reflection) of recently generated boards; on collision, perturb the sub-seed. For small early footprints, accept that the *pool* is finite and design the difficulty curve so tiny boards are only used for the first handful of levels.
- Distinguish two different meanings of "unique" with stakeholders: *non-repeating in a player's sequence* vs *globally distinct boards*. The former is cheaper and probably what's intended.

**Warning signs:**
Playtest feedback "these all feel the same"; generator has no rejection loop; no symmetry-normalized dedup; early levels solvable without thought.

**Phase to address:** Generator phase + dedicated difficulty-tuning sub-phase (Slice 1). Flag for playtesting iteration.

---

### Pitfall 4: Hint gives an illegal or losing move after the player deviates

**What goes wrong:**
The hint blindly returns "the next move from the recorded winning solution." Once the player makes any move *not* on the canonical line, the recorded next move is illegal (its pegs aren't where the script expects) or, worse, legal but leading to a dead end. The hint then crashes, no-ops, or actively misleads — the exact opposite of its purpose, and directly damages the project's core "always feels fair" value.

**Why it happens:**
The recorded solution is a fixed sequence indexed by move number, but the player's actual board can diverge after move 1. Developers test hints only on the happy path (player follows hints), so deviation is never exercised.

**How to avoid:**
- Hint logic must be **state-relative, not script-relative**:
  1. If the recorded solution's next move is *legal in the current board* → return it.
  2. Else, run the `Solver` from the current state to find any move that preserves a win.
  3. If the Solver proves no win exists → return an explicit "no winning move — undo or restart" result (a first-class state, not an exception).
- Crucially: **the player can reach genuinely unsolvable states.** "Challenge levels are always solvable" applies only to the *starting* position, not every reachable position. The spec acknowledges this; the implementation and tests must too.
- Property-test hints: from random *legal-but-off-script* play, assert every returned hint is a legal move and (when one exists) keeps a win reachable.

**Warning signs:**
Hint code that indexes `solution[moveCount]`; no Solver fallback path; no "unsolvable" UI state; hint tests that always follow previous hints.

**Phase to address:** Hint/undo phase (Slice 1), depends on Solver phase. The "unsolvable detection" UI state must be designed here, not bolted on.

---

### Pitfall 5: Solver blows up the main thread (peg solitaire is NP-complete)

**What goes wrong:**
The `Solver` (used for board validation and as the deviation hint fallback) does an unbounded search. On a small board it's instant; on a larger high-level footprint the search space explodes and the call either ANRs the UI thread or takes seconds, freezing the game when the player taps "hint."

**Why it happens:**
Generalized peg solitaire is NP-complete; naive DFS over move sequences is exponential. The Solver is written for correctness on test boards and never stress-tested on the largest generated footprints, and it's called synchronously from a use case invoked on the UI dispatcher.

**How to avoid:**
- **Prefer the recorded solution; treat the Solver as the rare fallback** (Pitfall 4). Most hints cost O(1) because the player is on-script.
- Prune the Solver with **pagoda functions / resource counts** (a real-valued board function that cannot increase under play; negative in corners) to cut unsolvable branches early — the standard, well-documented peg-solitaire technique.
- Run any Solver call on `Dispatchers.Default`, off the UI thread, with a **bounded budget** (node/time limit). On budget exhaustion, degrade gracefully to "couldn't find a hint — try undo," never freeze.
- Cap generated footprint size so the worst-case board stays inside the Solver's tractable range; let solution *length* (not raw board area) carry late-game difficulty.

**Warning signs:**
Solver called from a composable or non-suspending use case; no time/node cap; no pagoda/heuristic pruning; "hint sometimes hangs"; StrictMode/ANR reports on hint taps.

**Phase to address:** Solver phase (Slice 1). Macro/stress test against the largest footprint the difficulty curve can produce.

---

### Pitfall 6: StateFlow doesn't recompose because the Board is mutated in place

**What goes wrong:**
After applying a move, the UI doesn't update — or updates only sometimes. The ViewModel holds `MutableStateFlow<GameState>`, the move handler mutates the existing `Board` (e.g. flips a cell in a backing array) and re-assigns the same reference. `StateFlow` dedupes by `equals()`; the value is reference-equal/structurally-equal to the previous, so **no emission, no recomposition.**

**Why it happens:**
Mixing mutable model objects with StateFlow's conflation/equality semantics. The classic Android trap of "I called `.value = state` but nothing happened."

**How to avoid:**
- Make domain models **immutable** (`data class` with `val`, persistent/`ImmutableList`-style cells). Apply a move by producing a **new** `GameState` via `copy()` — never mutate.
- Drive `Canvas` from this immutable state; a genuinely new value guarantees the draw phase re-runs.
- Annotate UI state as `@Immutable`/`@Stable` so Compose can skip correctly rather than over-recompose.

**Warning signs:**
`var` fields inside `Board`/`Cell`; `IntArray`/`Array` mutated then re-emitted; "UI updates after I rotate the screen but not on the move"; equality overridden in a way that hides changes.

**Phase to address:** Domain model + RulesEngine phase, and the ViewModel/UDF wiring phase (Slice 1). Cheap to get right early, expensive to retrofit.

---

### Pitfall 7: The elapsed-time clock recomposes the whole board every tick

**What goes wrong:**
A 1 Hz (or worse, per-frame) timer for the score clock lives in the same `UiState`. Every tick emits a new state, recomposing the entire gameplay screen — including the `Canvas` and all peg animations — burning CPU and causing jank, especially on 90/120 Hz panels where the frame budget is ~8–11 ms.

**Why it happens:**
Elapsed time is bundled into the single immutable `UiState` for convenience. Compose recomposes every reader of that state on each tick, even though only a clock label changed.

**How to avoid:**
- Isolate the timer: expose elapsed time as a **separate `StateFlow`/state**, and read it inside a small leaf composable (the clock label) so only that node recomposes. Or pass it as a **lambda** (`time: () -> Long`) so the read is deferred to the draw/layout phase, not composition.
- Keep board cells and timer in different state holders; never let a clock tick invalidate the `Canvas`.
- Drive jump/removal animations with `Animatable`/`animate*AsState` (recomposition-optimized) rather than feeding raw animation progress through `UiState`.

**Warning signs:**
Layout Inspector shows the board composable recomposing once per second; battery drain on the gameplay screen; jank during idle (no interaction) play; `UiState` contains `elapsedMs` read by the board.

**Phase to address:** Gameplay screen / Compose rendering phase (Slice 1). Verify with the Compose recomposition counts in Layout Inspector.

---

### Pitfall 8: Canvas allocates and over-draws every frame

**What goes wrong:**
The board `Canvas` allocates objects (`Path`, `Paint`, `Brush`, lists, `Offset` arrays, boxed primitives) inside `DrawScope` on each draw, and/or redraws the full board every frame even when one peg moves. Allocation in the frame path triggers GC pauses → visible stutter during the jump animation; over-draw wastes GPU.

**Why it happens:**
Treating `Canvas` like a one-shot drawing surface and re-creating geometry per frame; not memoizing static board grid; animating by redrawing everything.

**How to avoid:**
- Hoist allocations out of the draw lambda: `remember` static geometry (grid lines, hole positions), reuse `Path`/`Brush` objects, precompute cell-center offsets once per board size.
- Read animating values via lambda-based reads so only the **draw phase** re-runs (when state is read inside `Canvas`, Compose skips composition and runs draw only — exploit this).
- Avoid `drawIntoCanvas`/native bitmap churn; avoid per-frame `String`/object creation for labels inside the canvas.
- Measure with macrobenchmark before/after, not by eye.

**Warning signs:**
`Path()`/`Paint()`/`mutableListOf()` inside `DrawScope`; GC log spam during animation; frame-time spikes in the Compose tracing; jank that worsens on larger (Diamond-41 / high-level) boards.

**Phase to address:** Compose rendering phase (Slice 1).

---

### Pitfall 9: Android dependencies leak into the "pure Kotlin" domain

**What goes wrong:**
The domain layer (RulesEngine, Generator, Solver, Scoring) imports `android.*`, `Context`, `Log`, `Parcelable`, or a coroutine `Dispatcher`/`SavedStateHandle`. Suddenly the high-value game logic can't be unit-tested on the JVM, requires Robolectric/instrumentation, and the architecture's central promise (testable, SDK-free core) is gone.

**Why it happens:**
Convenience: `android.util.Log` for debugging, `@Parcelize` on `Board` for the resume bundle, injecting `Context` to read a resource. Each is a small step that hardens into a dependency.

**How to avoid:**
- Put the domain in a **pure Kotlin/JVM module** (or at least a package with an enforced dependency rule) so an Android import won't even compile. A Gradle module boundary is the strongest guard.
- Keep serialization (`@Parcelize`, Room entities, DataStore) in the **data layer**; map to/from domain models. Domain `Board` stays Android-free; a `BoardEntity` lives in data.
- Use a logging interface or none; pass dispatchers in from the data/UI layer, don't reach for `Dispatchers` inside pure logic.
- The `AdGateway`/`GamesService` interfaces already model this discipline — apply the same to persistence and logging.

**Warning signs:**
`import android.` in `domain/`; domain tests needing Robolectric; `Context` parameters on use cases; Room annotations on domain models.

**Phase to address:** Project/module scaffolding phase (first phase). Enforce with a module boundary or an ArchUnit/Konsist test from the start.

---

### Pitfall 10: Business logic creeps into composables; MutableStateFlow leaks out of the ViewModel

**What goes wrong:**
Move-legality checks, scoring, or generation get written inline in a composable; or the ViewModel exposes `MutableStateFlow`/`var` publicly so the UI can write state directly. Result: untestable logic, broken unidirectional data flow, race conditions on shared mutable state, and recomposition-driven re-execution of logic that shouldn't run during a draw.

**Why it happens:**
Speed during prototyping; "it's just one `if`." Compose makes it tempting to compute in the composable because the state is right there.

**How to avoid:**
- Composables **render state and emit events only** (state down, events up). All decisions live in use cases/ViewModel.
- ViewModel exposes `val uiState: StateFlow<UiState>` (read-only), backed by a private `MutableStateFlow`. One-off effects (navigate, show dialog) via a `SharedFlow`/`Channel`, not booleans in `UiState`.
- Collect with `collectAsStateWithLifecycle()` (not bare `collectAsState`) so the flow isn't collected while the screen is in the background — avoids wasted work and stale-resume bugs.

**Warning signs:**
`RulesEngine`/`Solver` called inside `@Composable`; public `MutableStateFlow`; `LaunchedEffect` doing game logic; `collectAsState()` without lifecycle; event booleans that must be reset.

**Phase to address:** UDF/ViewModel wiring phase (Slice 1).

---

### Pitfall 11: Resume/serialization restores an inexact game state

**What goes wrong:**
The player backgrounds mid-game and returns to a subtly wrong state: undo history lost (so undo/hint break), elapsed time reset or ballooned, the board off-by-one, or the wrong level. For Challenge, the board is regenerated from N instead of restored — and if the generator ever changed (Pitfall 2) or the player had deviated from the recorded line, the restored board is wrong.

**Why it happens:**
Only the board cells are serialized; move history, elapsed time, current-solution index, and `generatorVersion` are dropped. Or resume regenerates rather than restores. Time is stored as wall-clock and recomputed incorrectly.

**How to avoid:**
- Serialize the **complete** `InProgressGame`: board snapshot **plus** full `moveHistory` (for undo), `elapsedMs`, mode, levelId, and `generatorVersion`. Restore the stored board, don't regenerate it.
- Treat elapsed time as accumulated duration; pause it on `onStop` and resume on `onStart` — don't derive from wall-clock deltas across process death.
- Round-trip property test: random game state → serialize → deserialize → assert identical board, history, score, and that undo/hint still behave.

**Warning signs:**
`InProgressGame` lacks `moveHistory` or `generatorVersion`; resume calls `generate(N)`; elapsed time computed from `System.currentTimeMillis()` at save vs load; undo unavailable after resume.

**Phase to address:** Persistence/resume phase (Slice 1).

---

### Pitfall 12: No persistence versioning/migration — schema change wipes or crashes progress

**What goes wrong:**
A later release adds a field to `LevelResult` or changes the serialized board format. Without a Room migration the app crashes on launch (or, if `fallbackToDestructiveMigration` was used, silently wipes all the player's stars and progress). A versionless board blob can't be re-read at all.

**Why it happens:**
Room migrations are tedious; `fallbackToDestructiveMigration()` is the path of least resistance and ships by accident. Hand-rolled blob serialization has no version byte.

**How to avoid:**
- Decide Room-vs-DataStore early (spec flags this). For relational per-level results, **Room with explicit `Migration` objects** and `exportSchema = true` + the schema checked into version control + Room's migration test harness.
- Never ship `fallbackToDestructiveMigration()` in release — it deletes user data.
- Every serialized blob (resume board) carries a leading **format version**; the reader switches on it.
- For DataStore (settings/lightweight), use Proto DataStore or include a version key with a migration path.

**Warning signs:**
`fallbackToDestructiveMigration()` anywhere; `exportSchema = false`; no migration test; a board blob with no version field; "my stars disappeared after the update."

**Phase to address:** Persistence phase (Slice 1); enforced at every schema-touching phase thereafter.

---

### Pitfall 13: (Slice 2) AdMob policy violations — interstitial frequency & non-opt-in rewarded ads

**What goes wrong:**
Interstitials shown on app launch, on exit, immediately after another interstitial, or after every level with no spacing → accidental clicks, bad UX, and **AdMob policy strikes / ad-serving limits / account suspension**. Rewarded ads that grant the reward without an explicit opt-in tap, or that can't be skipped/dismissed, are *disallowed implementations*.

**Why it happens:**
Maximizing ad impressions for revenue; treating "between levels" as "after every single level"; auto-playing a rewarded ad when the player taps "hint" instead of presenting a yes/no choice first.

**How to avoid:**
- **Interstitials:** only between content (e.g. on level-complete → next), never on app open or exit, never back-to-back. Apply a **frequency cap** (time- and/or level-count-based) so they don't fire every level. Route all of this through the existing `AdGateway` so cadence is centrally controlled and tunable.
- **Rewarded:** always behind an explicit opt-in dialog ("Watch an ad for a free hint? [Yes]/[No]"), reward granted only on the SDK's completion callback, always skippable/dismissible. This maps cleanly to `AdGateway.requestRewarded(reason): Boolean`.
- Use AdMob **test ad unit IDs** in development — clicking live ads yourself is an invalid-traffic violation.
- Keep ad-free core loop intact: the Slice 1 stub auto-grants, so design must not *depend* on ads to be playable.

**Warning signs:**
Interstitial in `onCreate`/`onResume`; reward granted before the completion callback; no frequency cap; live ad unit IDs in debug builds; an `AdGateway` that exposes "showAd" without a reason/opt-in concept.

**Phase to address:** Slice 2 AdMob phase. The `AdGateway` interface (Slice 1) should already encode opt-in + cadence so Slice 2 only fills in the implementation.

---

### Pitfall 14: (Slice 2) Play Games v2 sign-in treated like v1 / cloud-save conflicts mishandled

**What goes wrong:**
Two failure clusters. (a) Sign-in: code written against the deprecated v1 `GoogleSignInClient`/interactive flow, when **PGS v2 signs in automatically at startup** and you query `GamesSignInClient.isAuthenticated()` — leading to double prompts, a broken sign-in button, or assuming a signed-in user when there isn't one. (b) Cloud save: last-write-wins overwrites the more-advanced device; or conflicts (`DataOrConflict.isConflict()`) are ignored, so a player who progressed on two devices loses stars/level progress.

**Why it happens:**
Most online tutorials predate PGS v2; the conflict callback is easy to skip because it rarely fires in single-device testing. The reverse-generation design means progress is "highest level + per-level stars/times" — exactly the data players will be furious to lose.

**How to avoid:**
- Use the **PGS v2 SDK**: rely on automatic sign-in, check `isAuthenticated()`, and present sign-in as optional (the game is fully playable signed-out). Don't gate core play on Play Games.
- Implement Snapshots **conflict resolution explicitly**: on `isConflict()`, **merge** rather than blind-overwrite — for this game, merge = take the max `highestLevelReached` and, per level, the best (fewest pegs / best time) result. Never silently pick one side.
- Load latest on app start/resume; save at sensible checkpoints (level complete), not every move.
- Keep cloud behind `GamesService`/`ProgressRepository` so local play is the source of truth and cloud is a sync layer — local-first, so an offline or signed-out player never blocks.

**Warning signs:**
`GoogleSignInClient`/`GoogleSignIn` imports (v1); no `isConflict()` handling; `commitAndClose` with a single snapshot and no merge; progress lost across two test devices; sign-in required to reach gameplay.

**Phase to address:** Slice 2 Play Games phase. Conflict-merge strategy must be designed alongside the `ProgressRepository` data model (so "best per level / max level" merge is well-defined).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Mutable `Board` with in-place flips | Less object churn, feels efficient | Breaks StateFlow recomposition (Pitfall 6); untestable diffs | Never for UI-bound state |
| Skip `generatorVersion` field | One less column | Can never safely tune the generator post-launch (Pitfall 2) | Never |
| `fallbackToDestructiveMigration()` | Ship schema changes instantly | Wipes player progress on update (Pitfall 12) | Debug builds only, never release |
| Hint = `solution[moveIndex]` | Trivial to implement | Illegal/misleading hints after deviation (Pitfall 4) | Never — must be state-relative |
| Domain in the app module (no separate module) | Simpler Gradle setup | Android deps creep in (Pitfall 9); slower tests | Acceptable *only* with an enforced Konsist/ArchUnit boundary test |
| Single `UiState` including `elapsedMs` | One state object | Whole-board recomposition per tick (Pitfall 7) | Never on the gameplay screen |
| Regenerate board on resume instead of storing it | Smaller save file | Resume corruption if generator/deviation changes (Pitfall 11) | Never for in-progress games |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| AdMob interstitial | Show on launch/exit or after every level | Between-content only, frequency-capped, via `AdGateway` (Pitfall 13) |
| AdMob rewarded | Auto-grant / non-skippable | Explicit opt-in dialog; reward only on completion callback |
| AdMob (dev) | Live ad unit IDs in debug | Test ad unit IDs; live IDs only in release |
| Play Games sign-in | v1 `GoogleSignInClient` flow | PGS v2 auto sign-in + `isAuthenticated()`; optional, non-blocking |
| Play Games Saved Games | Ignore conflicts / last-write-wins | Handle `isConflict()`, merge max-level + best-per-level, `resolveConflict()` (Pitfall 14) |
| Room | `exportSchema=false`, no migrations | Export schema to VCS, explicit `Migration`s, migration tests |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Timer recomposes board | Board recomposes ~1×/sec; idle battery drain | Isolated timer state / lambda read (Pitfall 7) | Immediately on gameplay screen, worse at 120 Hz |
| Allocation in `DrawScope` | GC spikes, jank during jump animation | Hoist geometry with `remember`, reuse Paint/Path (Pitfall 8) | Larger boards (Diamond-41, high levels) |
| Unbounded Solver on UI thread | ANR / freeze on hint tap | Off-thread, pagoda-pruned, time-budgeted; prefer recorded line (Pitfall 5) | High-level large footprints |
| Generator reject-loop with weak seeding | Slow level load, or infinite loop | Bounded deterministic re-seed attempts; cap footprint (Pitfalls 1,3) | Small footprints with tight quality gates |
| Per-frame state through `UiState` for animation | Recomposition storm during animation | `Animatable`/`animate*AsState`, draw-phase reads (Pitfalls 7,8) | Any animated move |

## Security / Integrity Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Trusting client time/score for leaderboards | Trivially spoofed rankings (Slice 2) | Server-side via PGS leaderboards; keep score formula deterministic and recomputable |
| Self-clicking live AdMob ads in testing | Invalid traffic → account ban | Test ad unit IDs in all non-release builds |
| Storing nothing to detect tampered saves | Edited local DB inflates progress | Acceptable for a single-player casual game; if leaderboards matter, validate server-side rather than obfuscate locally |
| Requiring Play Games sign-in to play | Excludes offline/declining users; privacy friction | Local-first; sign-in strictly optional |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No "no winning move" state | Player stuck, thinks game is broken | First-class unsolvable state offering undo/restart (Pitfall 4) |
| Withholding stars for slow-but-perfect solves | Frustration on a fair win | Stars from pegs remaining; time feeds a separate ranking score (per spec §7) |
| Difficulty spikes / flat early curve | Early churn or boredom | Data-driven curve, playtested; tiny boards only for first few levels (Pitfall 3) |
| Forced/surprise interstitials | Accidental taps, 1-star reviews, policy strikes | Capped, between-level, opt-in rewarded (Pitfall 13) |
| Lost progress across devices | Rage, uninstall | Explicit cloud conflict merge (Pitfall 14) |
| Resume loses undo history | Undo "disappears" mid-game | Serialize full move history (Pitfall 11) |

## "Looks Done But Isn't" Checklist

- [ ] **Challenge generation:** Looks deterministic — verify golden-master test for levels 1/2/10/100/1000 passes in CI, not just locally (Pitfall 1).
- [ ] **Generator quality:** Produces solvable boards — verify they're also non-trivial, non-degenerate, and de-duplicated across seeds (Pitfall 3).
- [ ] **Hints:** Work when following them — verify they're legal and win-preserving after the player deviates, and that "no winning move" is handled (Pitfall 4).
- [ ] **Solver:** Solves test boards — verify it's bounded, off-thread, and pruned on the *largest* generated footprint (Pitfall 5).
- [ ] **Move → UI:** Board updates — verify it updates on *every* move via immutable state, not just on rotation (Pitfall 6).
- [ ] **Score timer:** Counts up — verify it doesn't recompose the board each tick (Layout Inspector) (Pitfall 7).
- [ ] **Resume:** Restores the board — verify move history, elapsed time, and undo all survive process death (Pitfall 11).
- [ ] **Persistence:** Saves results — verify a schema migration path exists and `fallbackToDestructiveMigration` is absent from release (Pitfall 12).
- [ ] **Domain purity:** Compiles — verify a boundary test forbids `android.*` imports in domain (Pitfall 9).
- [ ] **(Slice 2) Ads:** Show ads — verify frequency cap, opt-in rewarded, test IDs in debug, no launch/exit interstitials (Pitfall 13).
- [ ] **(Slice 2) Cloud save:** Syncs — verify two-device conflict merges max level + best-per-level instead of overwriting (Pitfall 14).

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Generator changed post-launch (Pitfall 2) | HIGH | Freeze old as v1; ship v2 only for new/higher levels; migrate saved boards by stored snapshot not regeneration |
| Non-deterministic generation (Pitfall 1) | MEDIUM | Replace unordered collections with sorted lists; add golden-master test; re-pin seed derivation before launch |
| StateFlow not recomposing (Pitfall 6) | MEDIUM | Convert models to immutable `data class`; replace in-place mutation with `copy()` |
| Solver ANR (Pitfall 5) | MEDIUM | Move off-thread, add node/time budget + pagoda pruning, cap footprint size |
| Destructive migration shipped (Pitfall 12) | HIGH | Cannot recover already-wiped data; add proper migrations going forward; consider cloud restore if Slice 2 live |
| AdMob policy strike (Pitfall 13) | HIGH | Remove offending placements, request review; repeated strikes risk account-level suspension |
| Cloud conflict data loss (Pitfall 14) | HIGH | Implement merge; for already-lost data, only recoverable if a local copy survives on one device |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 Non-deterministic generation | Generator | Golden-master fixtures green in CI across machines |
| 2 Generator-change breakage | Generator + Persistence | `generatorVersion` present; tuning happens pre-freeze |
| 3 Trivial/degenerate/duplicate boards | Generator + tuning sub-phase | Quality-gate + dedup tests; playtest sign-off |
| 4 Hint after deviation | Hint/Undo (needs Solver) | Property test: off-script play → legal, win-preserving hints |
| 5 Solver blowup | Solver | Stress test largest footprint within time/node budget, off-thread |
| 6 StateFlow no recompose | Domain model + ViewModel wiring | Move updates UI in instrumented test |
| 7 Timer recomposition | Gameplay/Compose rendering | Layout Inspector: board recomposition count = 0 on tick |
| 8 Canvas allocation/over-draw | Compose rendering | Macrobenchmark frame timing; no DrawScope allocations |
| 9 Android deps in domain | Project scaffolding (first) | Konsist/ArchUnit boundary test fails on `android.*` |
| 10 Logic in composables / leaked MutableStateFlow | ViewModel/UDF wiring | Code review + public API has only read-only `StateFlow` |
| 11 Resume inexact | Persistence/resume | Serialize→deserialize round-trip property test |
| 12 No migration | Persistence | Room migration test; no destructive fallback in release |
| 13 AdMob policy/UX | Slice 2 AdMob | Cadence config + opt-in flow review against AdMob policy |
| 14 PGS v2 sign-in / conflict | Slice 2 Play Games | Two-device conflict test; v2 SDK; optional sign-in |

## Sources

Peg solitaire solvability / pagoda functions / NP-completeness:
- George I. Bell, "A Fresh Look at Peg Solitaire," Mathematics Magazine 2007 — https://www.gibell.net/pegsolitaire/papers/Bell_AFreshLookatPegSolitaire_MathMag2007.pdf
- "Analysis of Peg Solitaire" (Jaap Scherphuis) — https://www.jaapsch.net/puzzles/pegsolit.htm
- Peg solitaire — Wikipedia (pagoda functions / resource counts, NP-completeness) — https://en.wikipedia.org/wiki/Peg_solitaire
- "Integer Programming Based Algorithms for Peg Solitaire Problems" — https://www.kurims.kyoto-u.ac.jp/~kyodo/kokyuroku/contents/pdf/1185-11.pdf
- jagatsastry/peg-solitaire-solver (pagoda + weight-matrix A* heuristics) — https://github.com/jagatsastry/peg-solitaire-solver

Jetpack Compose performance / Canvas / recomposition:
- Jetpack Compose Performance — https://developer.android.com/develop/ui/compose/performance
- Practical performance problem solving in Jetpack Compose (codelab) — https://developer.android.com/codelabs/jetpack-compose-performance
- "The Anatomy of a Frame: the 16.6ms Pipeline in Jetpack Compose" — https://proandroiddev.com/the-anatomy-of-a-frame-understanding-the-16-6ms-pipeline-in-jetpack-compose-9a13948aa98b

Play Games Services v2 / Saved Games conflict resolution:
- Cloud save (Saved Games) — https://developer.android.com/games/pgs/savedgames
- Saved games conflict resolution — https://developers.google.com/games/services/android/savedgames
- Platform authentication (PGS v2 sign-in) — https://developer.android.com/games/pgs/platform-authentication

AdMob policy:
- AdMob policies and restrictions — https://support.google.com/admob/answer/6128543
- Disallowed interstitial implementations — https://support.google.com/admob/answer/6201362
- Policies for ad units that offer rewards — https://support.google.com/admob/answer/7313578

Project context:
- `.planning/PROJECT.md`
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` (esp. §5 generation, §6 hints, §12 risks)

---
*Pitfalls research for: native Android procedural peg-solitaire game*
*Researched: 2026-06-28*
