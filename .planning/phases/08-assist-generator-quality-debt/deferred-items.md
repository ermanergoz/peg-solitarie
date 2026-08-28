# Deferred Items — Phase 08

Out-of-scope discoveries logged during execution. Not fixed in the plan that found them.

## Pitfall 7 stale-floor surfaces #2 and #3 (found during 08-01, Task 2)

Two test-source KDocs still claim Diamond-41's fewest-pegs floor is "several pegs":

- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt:239`
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt:23`

Plan 08-01 corrected surface #1 (`domain/src/main/.../Boards.kt`) only — its
`files_modified` list scopes it to `:domain` main sources plus `BoardSpecTest`.

Both remaining surfaces sit in the recorded-solution fixture code that plan **08-06**
rewrites when it commits the `diamond41.txt` golden line. Correcting them there is
strictly cheaper than touching those files twice, and the corrected wording depends on
that fixture existing. Deferred to 08-06.

Correct wording (matching `Boards.kt` and `BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`):
the floor is exactly **F = 2** — GF(4) excludes one peg exhaustively (0 of 41 single-peg
positions pass), and two is reached by the committed golden line.
