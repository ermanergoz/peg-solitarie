# android-game-dev report — home-chip-arrival
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (6f9ae70..c388993), merge-base 4ff4ec6
Skills loaded: ergoz-soft:standards (android-game-dev; snapshot 0.6.2 is older than plugin 1.0.0, so I read the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus the product's lessons.md), superpowers:test-driven-development (with writing-good-tests.md), gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: none of my own; the host was shared with other worktrees (load 4.5–6.4) and I never ran two builds at once
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (0 active lessons)

> Producer note: the agent returned this report as text because the harness refused a report file from a
> subagent; the producer wrote it here verbatim and committed it.

## Summary
- **D-03n:** the band column is now a measure policy (`HomeBandColumn`, brief shape (b): a plain multi-content `Layout`, plus a stage-art verdict state written only when it changes). The footer is a fifth of the no-chip slack S₀ and the stage gets the rest, less the chips' room. Rooms open and close over `ENTRANCE_MS`; a leaving chip is latched and clipped; a late chip takes no stagger; stage art below its floor fades instead of vanishing.
- **Part 4:** a board's first frame now draws its pegs.
- **D-03o:** the ribbon takes one shape (Full, Compact or Stacked), decided from the measured labels.
- **D-05e:** the timer speaks its value.
- **D-05f:** `values-v27` and `values-night-v27` themes give dark mode a dark navigation bar.
- **INFO rows 1, 3, 4, 5:** done.
- Every behaviour was watched red first. All 15s windows held; no shortfall goes back to the ux-designer.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeBandColumn.kt` (new): the measure policy, `ChipRoom`, `rememberChipRoom`, `ChipRoomSlot`, the stage-art verdict and `FOOTER_SHARE_OF_SLACK`.
- New: `ui/game/ElapsedTimeSpeech.kt`, `res/values-v27/themes.xml`, `res/values-night-v27/themes.xml`.
- Edited: `HomeScreen.kt`, `HomeStage.kt`, `HomeBandSet.kt`, `HomeProgressRibbon.kt`, `BoardCanvas.kt`, `GameStatusStrip.kt`, `strings.xml`.
- New tests: `HomeChipArrivalTest` (12), `HomeRibbonShapeTest` (12), `BoardCanvasFirstFrameTest` (2), `ElapsedTimeSpeechTest` (1).
- Edited tests: `HomeLayoutTest` (INFO 1 and 3), `HomeStageTest` (KDoc only), `WindowThemeTest` (+2), `GameStatusStripTest` (+2 and the `timer()` retarget), `GameFlowTest:129` and `GameScreenChromeTest:62/:75` (retargets).
- `git diff --name-only 4ff4ec6...HEAD -- app/src/main | sort` lists exactly the brief §4 files except `Clay.kt`/`Theme.kt` (C9 struck). Totals: 22 files, +2692/−256. No `@Suppress`, `@Ignore`, baseline or `!!` added.

## Commits: red, then green
| Commit | Red first (quoted) | Green | Maps to |
|---|---|---|---|
| C1 `6f9ae70` test-only | Throwaway worktree. **M3d**: "pill 0: the texts it lays out… expected: [Stars, 7] but was: [★, Stars, 7]". **M5c**: "pill 0: px painted besides its surface… expected at least 20, but was 0" | HomeLayoutTest 17/17; glyph ink measured 131 / 196 / 85 px | INFO 1, INFO 3 |
| C2 `ea094e2` | Read-only board: "peg ink… in the first frame: expected >0, but was 0". Home stage: "pixels… differ between the first frame and the settled one: expected 0, but was 16813". The brief's literal line kept both red, unchanged | 2/2, plus BoardCanvasTest 14 and GameFlowTest 7 | Part 4, 15v part 4 (JVM half) |
| C3 `98b66d7` | 15s on all six windows: "Classic chip: the Classic tile's top edge… expected 525.0, but was 549.0" (412×915; the tiles moved 22 dp at 360 wide, 24 at 412). 15t: "…moved down, R=170, S₀=282: expected 0.0, but was 43.0" and "expected 106.8, but was 110.0" | 8/8; all 124 `ui.home` tests green, HomeStageTest and HomeLayoutTest unedited | 15s, 15t, 15g's ruled reading, 15k unchanged |
| C4 `fc9a7dd` | Motion: "the frame the room settles in: expected 15..18, but was 1". Reduced motion passed on C3 (which snaps); red against an effect-only snap: "both chips leaving: every frame… expected 1 distinct, but was 2". Art fade (C4 step 1): "frame 9: the stage art, the room still opening: expected true". Late chip under the counted order: "frame 5, room 48dp: px drawn: expected >0, but was 0" | 12/12; the room first moves at frame 3 and settles at frame 16 arriving, 17 leaving | 15u ×2, brief §1.2 and §1.3, F-14, 15v part 3 |
| C5 `05d5bd7` | 4 of 12 red: "pill 0's shape: expected Stacked, but was OneLine"; "at 2.0: pill 0's 'Stars', in lines: expected 1, but was 2"; "12345 at 1.3: 'Level'… was 2"; "height after the progress emission at 2.0: expected 58.0, but was 80.0" | 12/12. At 360 dp, 'Boards' is 42 dp against a 46.5 dp half at 1.0 (spec estimated ≈42/47), 53 dp at 1.3 (stacked), 85 dp at 2.0 (stacked) | 15w; 15m unchanged |
| C6 `6450894` | "at 00:00: expected: Elapsed time, 0 seconds, but was: Elapsed time"; both chrome retargets red; ElapsedTimeSpeechTest red on "Unresolved reference 'SpokenDuration'". The one-node guard and the GameFlowTest retarget pass on both sides by design | Strip 9/9, Speech 1/1, Chrome 5/5, Flow 7/7 | 18f, the four 18f retargets |
| C7 `6e3f385` | "windowFlag(…) expected to be true" (light); "…expected to be false" (dark) | WindowThemeTest 4/4; base theme files untouched | 29 (1) |
| C8 `c388993` | Comments only | — | INFO 4, INFO 5 |

**15s readings** (stage height: none / one chip / both chips; one chip's room is chip + gap = R):
| Window | S₀ | Stage (none / one / both) | One chip's R |
|---|---|---|---|
| 360×640 | 232 | 186 / 98 / 10 | 88 |
| 360×720 | 312 | — / — / 74 | 88 |
| 360×800 | 392 | — / — / 138 | 88 |
| 412×780 | 328 | — / — / 86 | 88 |
| 412×850 | 342 | — / — / 82 | 96 |
| 412×915 | 407 | 326 / 230 / 134 | 96 |

All in dp; 412×915 matches the spec's 326.3 / 230.3 / 134.3.

## Checks
- `./gradlew --max-workers=4 check` on the untouched branch: pass, 11m11s, `:app` 1067 / `:domain` 274.
- The same command after each commit, all pass, 11m07s–11m23s each: `:app` 1068 (C1), 1070 (C2), 1078 (C3), 1082 (C4), 1094 (C5), 1097 (C6), 1099 (C7), **1099 (C8)**, `:domain` 274 throughout, 0 failures, errors or skips.
- C3 and C4 were re-committed only to shorten their subjects (see Deviations); `git rev-parse` shows each new commit's tree equals the tree its Check ran on.
- `./gradlew --max-workers=4 :app:compileDebugKotlin --rerun-tasks`: no warning in any touched main file. Ten main warnings remain, all pre-existing in untouched files.
- `./gradlew :app:ktlintCheck :app:detekt` before every commit: pass.

## Deviations
1. **`BoardCanvas.kt` has two hunks, not one.** The brief's line `canvasSize == size` reads the state at draw time, and `onSizeChanged` has already written that frame's size by then, so the stale geometry was still used; I ran it and both tests stayed red. The fix captures the composed size in composition (`val geometrySize = canvasSize`, one line beside the `geometry` remember). `pointerInput`, the `LaunchedEffect`s and `drawBoard` are untouched.
2. **Column structure.**
   - Five slots: hero, stage, ribbon, chips, tiles.
   - Both chips moved verbatim into one `ContinueChips` composable, to stay under detekt's LongMethod (60) and TooManyFunctions (11) limits in `HomeScreen.kt`.
   - Chips are measured at natural height and drawn clipped to their share of the room (a draw-phase clip), because the fade verdict needs natural heights.
3. **Stage entrance.** The hero-order `clayEntrance` moved from the stage art to the stage box. Otherwise, art re-composed after a chip leaves would replay the entrance, which §4.2.5 and §10 forbid.
4. **Reduced motion.** The room reads its target directly in composition, so it settles in the next frame.
5. **15u harness.** It sends the snapshot's apply notifications right after the state write. On the paused clock the write otherwise lands a frame late.
6. **Two tests beyond the brief:** the art fade and the late chip's zero stagger. The zero-stagger code landed in C4 step 1, before its test; that test's red was shown against the counted-order mutant.
7. **15w test names.** The brief's six names became twelve cases, split by font scale and shape. The equal-width check allows max(0.5 dp, 1 px): the ribbon splits 328 px as 110/109/109 at Robolectric's mdpi.
8. **Commit subjects.** The brief's C3/C4 subjects were 81 and 87 characters (the limit is 72), so I reworded them before any review (`fd3b0a9`→`98b66d7`, `af013bd`→`fc9a7dd`), with identical trees.
9. **Strings.** `strings.xml` gains exactly §8's two strings and two plurals; `duration_under_a_minute` is not added.
10. **First-frame Home case.** It runs with animations off, and insets can only be dispatched after the first composition.
11. **Commit trailer.** Every commit carries the studio's single Fable 5.1 trailer per `rules/gitflow.md`, though the code was written on Opus.

## Open issues / risks
1. A leaving chip stays tappable for up to 240 ms while its room closes, and a tap resumes the latched target. The code-auditor may want it made inert.
2. "Starts in the frame after the emission" measures as frame 3 after the state write: frame 1 composes, frame 2 is the animation's start-time frame. The test allows up to 3.
3. The art fade is read by exact slab-colour pixel counts, which relies on the background being a different colour.
4. At the merge-in (C10), `BoardCanvas.kt` overlaps with audio: keep both of my hunks. `strings.xml` and `GameFlowTest` need keep-both resolutions.

## Handoff
Next: **code-auditor** — review `4ff4ec6...c388993` against brief §1 and §4, with Deviations 1–8 above.
Then: **producer** — C10, the develop merge-in, once hint-undecided, check-gates and audio have merged.
Then: **qa-engineer** — the 15e sitting first, then the device checks: 15v (all parts), 15w's device half, 18g, 26b (no board stop on this build), 29 (2). The C9 scenarios are not on this branch. Arm B is the C10 merge commit; until then the last commit that changes the APK is `6e3f385` (`c388993` is comments only).
