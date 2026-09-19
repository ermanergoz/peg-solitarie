# Puzzle design rules and invariants (product type: game)

Every puzzle game must satisfy these invariants and prove each with a test:
1. Solvable: every generated puzzle has at least one solution (Solver proves it).
2. Unique when required: if the GDD says solutions are unique, the Solver proves it.
3. Seed-reproducible: `generate(seed)` always returns the same puzzle.
4. No unrecoverable states: from any reachable state a solution exists, or
   undo/reset restores solvability.
5. Hints progress: a hint keeps the puzzle solvable and reduces the Solver's
   remaining-steps metric.
6. Difficulty is measurable: tiers map to a numeric property (solver depth,
   backtracks, branching factor, number of solutions) recorded per puzzle.
7. No trivial states: the generator rejects puzzles below the tier's minimum.
8. Fuzz harness: generate → validate → solve → replay → verify win over
   ≥ 10,000 seeds in CI and ≥ 100,000 before release; failing seeds written
   to `build/fuzz-failures.txt` block the release.

Design guidance:
- Invent mechanics, not themes; start from spatial, logical, or
  combinatorial primitives; reject concepts whose difficulty comes from
  arbitrary rules to memorise.
- A concept brief states what the player observes, what operations exist,
  what progress means, why mastery is possible, how difficulty scales.
- Onboarding: the first 3 puzzles teach without text, the way a level layout
  teaches; first failure impossible before puzzle 5.
- Flow: challenge rises with skill; measure it (solver metrics), don't guess.
- Fairness: all needed information visible; no hidden state; no forced guess.
