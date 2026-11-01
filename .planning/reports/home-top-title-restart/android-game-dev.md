# android-game-dev report — home-top-title-restart
Date: 2026-11-02
Branch: feature/home-top-title-restart (from develop 58c05b8)
Skills loaded: compose-skill, compose-ui
Ran in parallel with: none
Loops: 0
Lessons applied: none

## Summary
Home: hero row pinned at the top; the slack sits between the hero row and the ribbon. The lower group stays where it is (D-03r, Q-046). Home bars hidden (D-05h). Challenge-solved Results gain "Play again" / "Yeniden oyna", which replays the same level and seed via onRetry.

## Outputs
- app/src/main/java/com/app/pegsolitaire/ui/home/HomeBandColumn.kt — hero placed at 0; `HomeBandPlacement.headroom` renamed `middle`; arithmetic unchanged
- app/src/main/java/com/app/pegsolitaire/navigation/SystemBarsDestinations.kt — Home -> Hidden
- app/src/main/java/com/app/pegsolitaire/ui/results/ResultsActions.kt, res/values{,-tr}/strings.xml — Play again
- app/src/test/.../HomeBandPlacementTest.kt — `.headroom` -> `.middle` (compile only)
- docs/ux-spec.md §3 — D-03r / D-05h / Results replay note

## Checks
- Check skipped by founder decision (no tests, no QA, no device runs)
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :domain:compileKotlin` — pass (exit 0)

## Open issues / risks
The Robolectric tests HomeHeadroomTest and HomeChipArrivalTest still assert headroom above the hero, a rising hero and the title bound. They compile, but these assertions will now fail. They were not retargeted, by founder instruction. SystemBars and Results tests were not updated for the new behaviour.

## Handoff
Next: producer — merge decision; code-auditor and qa-engineer if review is reinstated.
