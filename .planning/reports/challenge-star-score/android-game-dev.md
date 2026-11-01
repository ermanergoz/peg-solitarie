# android-game-dev report — challenge-star-score
Date: 2026-11-01
Branch: feature/challenge-star-score (7ba7846..9110309, plus this report's commit)
Skills loaded: none invoked (lean dispatch at reduced effort; TDD followed by hand: test red, then green)
Ran on: macOS host, JVM only (no device or emulator)
Ran in parallel with: none
Loops: 0
Lessons applied: none

## Summary
Q-042: the Challenge score is now `(peg value + time bonus) × level multiplier × stars` (3★ ×3, 2★ ×2,
1★ ×1) through the new `ScoreCalculator.challengeScore` / `evaluateChallenge` in `:domain`.
`GameViewModel.scoreFor`'s Challenge arm calls `evaluateChallenge`. Classic still calls `evaluate`
and is not multiplied. Q-043 needed no code change. No UI copy changed.

## Outputs
- domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt — `challengeScore`, `evaluateChallenge`
- domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt — one table-driven case: ×1/×2/×3, strictly increasing 1★<2★<3★, Classic unmultiplied
- app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt — Challenge arm routed through `evaluateChallenge`
- docs/gdd.md — §6.3 rewritten for both modes; decision row GD-SB-22 (Q-042/Q-043, founder 2026-11-01)
- docs/tech-design.md — scoring bullet for `challengeScore` / `evaluateChallenge`

## Checks
- Check skipped by founder decision (2026-11-01): `./scripts/check.sh --offline` and `./gradlew check` were not run.
- `./gradlew --offline -q :domain:test --tests '*ScoreCalculatorTest*'` — fail (red, before the implementation: `evaluateChallenge` unresolved), then pass (green, after it; run before the founder's clarification arrived)
- `./gradlew --offline -q :domain:compileKotlin :domain:compileTestKotlin :app:compileDebugKotlin` — pass (exit 0)

## Open issues / risks
- Knock-ons, checked by reading the code only: best-score upsert and the cloud merge take `max` of an Int; the cumulative leaderboard sums into a Long (`SUM(bestScore)`). All of them take the new numbers without changes. Results recorded before the change keep their old scores (GD-SB-13), so an old best stays until a new play beats it.
- Int headroom: the score is at most about 8,100 × levelMultiplier × 3, and the level multiplier grows with level. It stays far below Int.MAX at any realistic level, as it did before.
- Not verified because the Check was skipped: ktlint, detekt, lint, kover ≥ 90% and the app's ViewModel tests. As far as I can see, the existing VM score assertions run on Classic boards.
- Suggestion, not built: a "×3" star-multiplier breakdown line on Results.

## Handoff
Next: founder — QA and sign-off (producer to route); code-auditor if the producer wants a review.
