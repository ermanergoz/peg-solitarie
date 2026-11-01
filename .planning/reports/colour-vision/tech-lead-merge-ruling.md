# tech-lead report — colour-vision (ruling on the develop merge-in: the hint-ring residue, QA's smoke, the conflict hunks)
Date: 2026-11-01
Branch: feature/colour-vision (report only)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, testing, kotlin-android, compose, stacks, skills-policy, logging, memory, dispatch. puzzle-design and play-store are identical to the snapshot already loaded. Also the worktree's `.claude/rules/lessons.md`, android-architecture and android-gradle-logic
Ran in parallel with: code-auditor merge review (colour-vision, `904d735..6a6e82d`)
Ran on: claude-opus-5-5 at effort high (policy). The pin is fable at xhigh. Q-019 puts every Fable-pinned role on Opus until 3.0.0 ships, and the founder's 2026-10-31 directive (1) runs each dispatch one step below its pin. The harness names no effort
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **(1) The residue: fix it now, in one bounded commit, red first.** Brief §3.2 is amended for frames the composition has not sized. The fix gets one developer round and one auditor delta review. If either falls through, the code goes back to `6a6e82d`'s and the residue becomes a recorded 3.0.1 item, with no second loop (Ruling 1).
- **(2) QA owes one smoke pass on the merged head, and nothing more.** It goes in the same QA dispatch as the owed 15e sitting. The smoke runs first, on `ergoz_studio`, not on the bench, and the sitting follows on `Pixel_9a_bench`. The one-frame fixes are not device-checked (Ruling 2).
- **(3) The `--cc` hunks of `904d735` are sound.** Nothing in them needs a change. Six points are recorded for the sign-off (Ruling 3).
- The order to merge is: auditor PASS, the fix commit, the auditor delta, the Check, QA (smoke, then 15e), the tech-lead sign-off, then the merge (Handoff).

## Ruling 1 — the dashed hint rings on a frame-size geometry

**What happens.** Read at `f535aea` (whose code equals `6a6e82d`).
- `rememberBoardLook` (`BoardLook.kt:58-65`) builds both hint strokes in composition. It does this through `hintRingStroke(geometry.hintSourceRingRadius | hintTargetRingRadius, ringWidthPx, recipe.hintRingDashes)` for the **composed** geometry.
- On a canvas's first frame, the composed geometry is `BoardGeometry.forCanvas(spec, Size.Zero, recipe)`. That gives `cellSize = min(w, h) / n = 0` and both radii 0. `dashedRingStroke` then computes `interval = 2πr / 16 = 0`.
- The draw (`BoardCanvas.kt:229`) uses develop's frame-size geometry, so the rings sit at the right centre and radius. Their stroke, though, carries a dash pattern of zero intervals.
- A zero-interval dash effect is no dash effect. For that one frame each colour-blind hint ring is a **solid** amber ring where eight dashes belong. That is the 547 differing pixels in the developer's probe.
- On each resize frame the pattern was computed for the old radius, so the eight dashes do not close at the top seam.
- The standard palette's hint stroke is `Stroke(width)`, which does not depend on the radius, so its probe stays green. Only the colour-blind palette is affected.

**Why fix it now rather than ship it as 3.0.1.** Severity is low: one 16 ms frame, at the right colour and radius, in a narrow case. When it occurs:
- the first frame of a colour-blind gameplay board with a hint up: after a configuration change such as a dark-mode switch, or after a re-entry, since `GameUiState` keeps the hint across both;
- multi-window resizes.

The cost decides it, and the cost favours now:
- **The sitting is not lost.** The 15e sitting has not been sat. The fix lives in `ui/components/`, which is not on the start-path list, and it adds no work to Home's draw (constraint 3 below). So it moves no arm and costs no sitting.
- **The red already exists.** It is the developer's residual probe, one argument away from `BoardCanvasFirstFrameTest`'s third case, which already asserts this property for the ring without a hint.
- **The code is this branch's.** It is the stroke half of the same composed-versus-frame split that `61e96db` fixed for the radii, in two files this branch owns (`BoardLook.kt` is this branch's file; the frame line has been this branch's since `61e96db`).
- **Deferring costs more.** Shipping it in 3.0.1 means a separate `bugfix/*` branch with its own auditor, QA and sign-off chain. Fixing it now adds one commit to a chain that is already open: about one developer dispatch with its ~11 min Check, plus one auditor delta. That is roughly 1.5–2 h on a week whose goal is 3.0.0.
- **The dashes carry the hint's non-hue cue.** The source ring (0.44c) and the selection ring (0.42c) differ in radius by only 0.02c, so the dashes are what CV-AC08 rests on. A known frame without them is the one place the palette's purpose fails, even if briefly.

**Brief §3.2, amended (tech-lead decision, 2026-11-01).** The sentence "The two dashed strokes are built once per `(geometry, recipe, ringWidthPx)` in a `remember`, exactly as audio builds `markStroke`, never in the draw lambda" is replaced by the following:

> The two dashed strokes are built once per `(geometry, recipe, ringWidthPx)` in a `remember`, and the draw lambda allocates none on a frame whose size the composition built `geometry` for (the steady state). On a frame it has not sized — the first frame and each resize frame, which develop's home-chip fix already draws with a frame-size `BoardGeometry` (ux-spec §4.2.5 part 4) — the draw builds the dashed hint strokes for that frame's geometry, through the same builder, only when a hint is drawn and the palette's hint rings are dashed.

This is the exception the home-chip fix already takes for the geometry itself. It is extended to the strokes that depend on that geometry, and to nothing else. rules/compose.md's "no allocation in hot composables" is about the steady state, and the steady state keeps zero allocation.

**The fix: constraints the auditor checks.** The mechanism is the developer's, within these:
1. **Files:** `ui/components/BoardCanvas.kt` and `ui/components/BoardLook.kt` only, plus the one test file below. No start-path file, no `BoardGeometry.kt` hunk, no `DashedRing.kt` change.
2. **One builder.** The frame path builds its strokes with the same function the `remember` uses (`hintRingStroke`, which may become `internal`), at the composed recipe's `hintRingDashes` (`look.recipe`, already in scope since `61e96db`) and `palette.ringWidthPx`. The two paths cannot drift.
3. **Only where it is drawn.** The frame strokes are built only when all four hold: the geometry is a frame geometry (`geometrySize != size`); the board is `interactive`; a hint is drawn (`hintSource` and `hintTarget` non-null); and `palette.hintRingsDashed`.
   - Home's read-only boards never enter the hint branch, so Home's first frame does no new work, and that is why no sitting is affected.
   - The standard palette's solid strokes are reused as they are.
4. **No steady-state allocation.** A matched frame allocates nothing new: no `Pair`, no `Stroke`, no lambda. The "is this a frame geometry" fact reaches the hint drawing as a `Boolean` (for example a `drawBoard` parameter), not as a rebuilt `BoardLook` or `BoardPalette`.
5. **The wrong-tap `markStroke` is not touched.** It has the same composed-radius property, but it is develop's code, signed off on home-chip-arrival, and there is no probe for it (Open issue 1).
6. **No suppression, baseline, `!!` or new constant outside a named private.** The `LongMethod` suppression keeps its one annotation (brief §3.10).

**Red first.** Add one case to `app/src/test/java/com/app/pegsolitaire/ui/components/BoardCanvasFirstFrameTest.kt`:
- **Name:** `theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes`.
- **Setup:** the same as `theFirstFrameOfAColourBlindBoardRingsTheHeldPegAsTheSettledFrameDoes`, with one change: `state = scene.state`, so `PaletteScene.midGame`'s hint stays on and the held peg is kept. Keep `AppTheme(darkTheme = false, boardMarkPalette = Colorblind)`, `motion.turnAnimationsOff()`, the paused clock and `INTERACTIVE_CANVAS_DP`.
- **Assertion:** `assertTheFirstFrameIsTheSettledFrame(BOARD_CANVAS_TEST_TAG)`.
- **Expected red on `6a6e82d`'s code:** "pixels inside board_canvas that differ between the first frame and the settled one, expected: 0, but was: 547" (or close to it). Quote the number in the report.
- **Expected green after the fix:** 4 of 4 in the class.
- A resize-frame case is welcome if the paused clock can stop on the resize frame, but it is not required, because the same `geometrySize != size` branch serves both.
- No existing assertion changes.

**Commit.** One commit: `fix(colour-vision): dash the hint rings on a frame-size geometry` (64 characters). Use the studio trailer only. It rides the developer's next round on this branch, together with any findings from the auditor's running review, in one round. It is not a dispatch of its own unless the auditor passes with no findings.

**Green means all of the following:**
- `./scripts/check.sh` exits 0;
- by name, with 0 failures: `BoardCanvasFirstFrameTest` (4), `BoardCanvasPaletteTest`, `BoardMarkMotionTest`, `ColorblindPaletteLiveSwitchTest`, `HomePaletteIdentityTest`, `HomeChipArrivalTest`, `BoardCanvasWrongTapReducedMotionTest`, `BoardCanvasWrongTapLiveMotionTest`, `BoardCanvasTest`;
- the test compilation's `w:` count is still 52, with 0 from main;
- the §11 tripwire's line 1 still prints the same seven files, and line 2 prints nothing.

**Fallback: the schedule guard.** The fix is abandoned if any of these happens:
- the red does not turn green within this one developer round;
- the change needs anything outside constraints 1–6;
- the auditor's delta review FAILs the fix commit.

In that case the fix commit is reverted, so the code returns to `6a6e82d`'s, and the residue is recorded as **3.0.1 item R-CV-1**, using this ruling's "What happens" paragraph, the amended §3.2 and the red case as its specification. No second loop runs, and the 15e sitting proceeds on the reverted head. Either outcome is a tech-lead decision already made here; neither comes back to this role.

## Ruling 2 — QA on the merged APK

**One smoke pass is owed, beyond the 15e sitting.** QA's device pass ran on `0f1f58f`. Since then, main source has gained:
- develop's home-chip, RM-gate and upload code, each QA'd on its own branch but never together with this palette;
- `61e96db`;
- the merge's `TutorialScreen` hunk, which joins RM's `CompletionStar` body to this branch's `StarGlyph`;
- the Ruling 1 fix.

Founder directive (2) provides for exactly this: "device re-runs check only the fixed defects plus one smoke pass". The fixed defects here (`61e96db`, the Ruling 1 fix) are one-frame properties that no device capture can judge. Their evidence is the JVM pixel tests, and QA does not attempt them.

**Where and when: one QA dispatch, in this order.**
1. **The smoke first, on `ergoz_studio`** (the AVD of QA's colour-vision pass on `18981ba`), with arm B's APK: the final head, built in its detached worktree.
   - Not on `Pixel_9a_bench`. ux-spec §6.5 precondition 1 keeps the bench for sittings only, and candy-backdrop's sitting will use it next. A smoke on it would write app data (the palette key, played levels) onto the instrument.
   - Running the smoke first is deliberate. A smoke defect stops the dispatch before ~40 minutes of sitting are spent on a head that will change. Shut `ergoz_studio` down before the sitting, because the sitting allows exactly one emulator.
   - `Pixel_9a` and `Pixel_9` are never used (full `/data`).
   - **Fallback, only if `ergoz_studio` cannot boot:** run the smoke on the bench **after** the sitting's closing readings (the colour-blind key count, `Data-Free`, `uptime`, the `base.apk` sha256), with the same arm-B install. End with `adb shell pm clear com.erman.pegsolitarie`, and record it in the report as a bench use outside a sitting, for candy's sitting to see.
2. **Then the 15e sitting**, exactly as brief §9 and the deviation ruling rule it:
   - arm A is `git merge-base develop feature/colour-vision` at the sitting (`305f741` today, and `develop` has not moved);
   - arm B is the final head;
   - the `colorblind_palette_on` key is absent at both ends;
   - `app/schemas` shows no diff.

   If `develop` gains a start-path commit before the sitting, the branch merges it in first and the arms move (developer report, Handoff).

**The smoke script.** Light theme, font scale 1.0, animator scale 1 unless a step says otherwise. One screenshot per step goes under `.planning/qa/colour-vision/`, never committed:
1. Cold launch: Home draws its boards and the chip arrives (home-chip's surface on this APK).
2. Settings: the Colorblind palette row sits after Haptics. Switch it on.
3. Classic English-33: hold a peg. The selection ring has its inner light band (the halo), and the legal-target dots are opaque.
4. Show a hint by any route the build allows. Both hint rings are amber and dashed, eight dashes each.
5. With the hint still up, switch the system to dark mode. The activity recreates, the board returns with the hint's dashed rings, and nothing crashes. The one frame is not judged.
6. Set the animator scale to 0, then Settings › How to play, through to the tutorial's completion star. It appears settled, in the palette's earned look (the merged `CompletionStar` hunk). Set the scale back to 1.
7. Results with at least one earned star: the stars are in the palette's look. Then the catalogue's pips.
8. Settings: switch the palette off. Back on the board, the standard marks return without a restart.

PASS means every step shows what it names, with no crash or ANR in logcat. A FAIL on any step stops the dispatch before the sitting and goes to the producer as a defect. It is not a matrix: no TalkBack, no 200 %, no simulated vision, and CV-AC14 stays not-run as before (non-blocking).

**Report.** Both halves go in `.planning/reports/colour-vision/qa-engineer-15e.md`, with a "Smoke pass" section above the sitting's tables. If the smoke fails, the same path carries `Sitting: not run` and the reason.

**Brief §7.3 amended (tech-lead decision).** Its "on the Pixel_9a — or the Pixel_9" is superseded for this branch by `ergoz_studio` (the full `/data` of both, STATUS 2026-10-31 19:45; founder directive (4)). Its "never `Pixel_9a_bench`" stands, except for the fallback above.

## Ruling 3 — the conflict hunks of `904d735`, re-read for the sign-off

`git show --cc --format= 904d735` shows six hand-resolved or two-sided files. Each was read against both parents (`18981ba`, `305f741`):
1. **`ui/components/BoardCanvas.kt`: sound.**
   - The resolution keeps this branch's `rememberBoardLook(spec, canvasSize, interactive)` and adds develop's `val geometrySize = canvasSize` capture after it. Both read `canvasSize` in the same composition, so `geometry` and `geometrySize` always describe one size.
   - Dropping develop's inline palette block loses nothing. Develop's `BoardPalette` (`305f741:BoardCanvas.kt:389-399`) has nine fields, and `rememberBoardLook` builds each one from the same source: the four clay colours, `primary`, `tertiary` (as `marks?.hintRing ?: tertiary`, which is `tertiary` for a read-only board and for the standard palette), `peg_selected_ring_width`, `onSurfaceVariant`, and `markStroke` remembered on `(geometry.cellSize, markWidthPx)`.
   - The frame-size fallback then needed the recipe, which is `61e96db`. Its stroke half is Ruling 1.
   - For a read-only board the recipe is Standard, so Home's draw is develop's, operation for operation.
2. **`ui/onboarding/TutorialScreen.kt`: sound.** `CompletionStar` is develop's RM body: the `Animatable` seeded from the gate, `LaunchedEffect(Unit)`, and `animateTo` always, with no effect keyed on the gate. It draws through this branch's `StarGlyph(earned = true, size = results_star_size, Modifier.scale(scale.value))`, and its KDoc is reconciled. The sign-off cites `TutorialCompletionStarLiveMotionTest` and `TutorialPipPaletteTest` from the Check.
3. **`data/SettingsRepository.kt` and `ui/settings/SettingsUiState.kt`: sound.**
   - Develop's live-sound and haptics wording is kept, and the "no audio engine yet" text is gone.
   - "Four flags so far" counts `hintsEnabled`, `soundOn`, `hapticsOn` and `colorblindPaletteOn`, correctly.
   - The assist-versus-palette fail-open sentence is this branch's (brief §5.1).
4. **`ui/settings/SettingsScreen.kt`: the auto-merge left two stale counts.** `6a6e82d` corrects them (KDoc only). The sign-off reads `6a6e82d` with the merge.
5. **`.planning/questions/FOUNDER.md`: sound.** Q-027 sits above Q-026, newest first. Q-027's answer (option 2) is implemented on this branch at `2fd0baf`, before QA's pass, so nothing is owed for it.
6. **Start path: no `--cc` hunk touches a start-path file.**
   - The merge's start-path changes are all develop's, so they sit in arm A.
   - The branch's own start-path diff against `305f741` is still `MainActivity.kt` and six `ui/theme/` files, so the sitting stays owed (founder directive (2): start-path code changed).

**For the sign-off.** It cites:
- this ruling;
- the code-auditor's merge review and delta review (paths as the producer names them);
- `qa-engineer.md`;
- `qa-engineer-15e.md` (smoke and sitting);
- `tech-lead-deviation-ruling.md`.

It re-reads the Ruling 1 fix commit's diff against constraints 1–6.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/tech-lead-merge-ruling.md`: this ruling, which carries:
  - Ruling 1: the residue, fixed now and bounded, with brief §3.2 amended, the red named, and a one-round fallback to 3.0.1 item R-CV-1;
  - Ruling 2: one smoke pass on `ergoz_studio` before the 15e sitting in a single QA dispatch, with brief §7.3 amended;
  - Ruling 3: the `--cc` re-read.

  No other file was edited, and no product code was touched.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-cv`, read-only. No Gradle was run, because the code-auditor's concurrent Check holds the worktree.
- `cat .claude/rules/VERSION` gives 0.6.2, against the plugin's 1.0.0. `diff -q` of each plugin rule against the snapshot found 10 that differ, and those were read in full from the plugin — pass.
- `git show --cc --format='%H %P%n%s' 904d735 -- …/BoardCanvas.kt`, and `git show --cc --format= 904d735` over every other path: six files with two-sided hunks, each read — pass (Ruling 3).
- `git show 305f741:…/BoardCanvas.kt | grep -A14 'private class BoardPalette'`: nine fields, all built by `rememberBoardLook` — pass.
- `git show 305f741:…/BoardGeometry.kt | grep cellSize`: `cellSize = min(size.width, size.height) / gridDimension`, which is 0 at `Size.Zero` — pass (Ruling 1's mechanism).
- `cat -n …/ui/components/BoardLook.kt`, `…/DashedRing.kt`, and `sed -n` of `BoardCanvas.kt` lines 100–125, 210–245, 400–425 and 580–620 — read: the strokes are composed for the composed radii, the draw uses `frameGeometry`, and `dashedRingStroke`'s interval is `2πr / 16`.
- `git show --format= 61e96db -- app/src/main`: the recipe on `BoardLook` and the frame line — read.
- `sed -n 95,162p …/BoardCanvasFirstFrameTest.kt`: the third case strips the hint with `.copy(hintSource = null, hintTarget = null)`, so the red is that case with the hint kept — read.
- `git grep -n -E '^(<<<<<<<|>>>>>>>|=======$)' -- app docs` prints nothing — pass.
- `git diff --quiet 6a6e82d HEAD -- . ':(exclude).planning'` exits 0 — pass.
- `git merge-base develop HEAD` and `git rev-parse develop` both give `305f741` — pass (arm A as of today).
- The start-path filter over `git diff --name-only 305f741 HEAD` prints `MainActivity.kt` and six `ui/theme/` files — pass.
- `grep -n unearnedStar` over `BoardMarks.kt` and `BoardMarkRecipe.kt`, and `git log -S'Q-027' -- app`: Q-027 option 2 is in at `2fd0baf` — pass.
- `grep 2026-10-31 .planning/reports/STATUS.md`: founder directives (1)–(4), and the note that `Pixel_9a` and `Pixel_9` have full `/data` — read.

## Open issues / risks
1. **Develop's wrong-tap `markStroke` has the same composed-radius property.** It is a 12-dash ring whose interval is built for the composed `cellSize`. It may surface on a resize frame while a wrong-tap mark is up. Whether it can surface on a first frame depends on whether `rememberWrongTapFeedback` re-shows a mark in a new composition, which I did not probe. It is out of this branch's scope (constraint 5) and is recorded as a 3.0.1 candidate: probe first, and if red, apply the Ruling 1 remedy to the mark's branch.
2. **Ruling 1 adds one round to colour-vision's path, and colour-vision gates candy-backdrop's sitting** (brief §10.2). The fallback caps the cost at one round. If the producer's schedule cannot absorb even that, the fallback may be invoked at dispatch, with the same 3.0.1 record. That is the producer's call, and it needs no new ruling.
3. **Docs owed to me after the merge:** `docs/tech-design.md` §3 (the composed/frame split for strokes, the amended §3.2 rule) and §10 (the smoke on `ergoz_studio`, not the bench). These join the brief §12 folds already owed.
4. **The developer's Open issue 6** (two agents writing one scratchpad log) is the producer's to route. It does not affect any figure here.

## Handoff
Next: producer —
1. When the code-auditor's merge review is in:
   - dispatch the android-game-dev in `PegSolitarie-cv` with Ruling 1 (constraints 1–6, the red, the commit, the fallback) and any auditor findings, in one round;
   - its report goes to `.planning/reports/colour-vision/android-game-dev-cv-hint-frame.md`.
2. Dispatch the code-auditor on the fix commit's delta (constraints 1–6, especially 3 and 4, and the named tests). If it FAILs, or the developer stops, apply the fallback: revert, and record R-CV-1 in STATUS.md for 3.0.1.
3. Dispatch the qa-engineer once, under Ruling 2: the smoke on `ergoz_studio`, then the 15e sitting on `Pixel_9a_bench`, with the host quiet and the arms as above. The report goes to `.planning/reports/colour-vision/qa-engineer-15e.md`.
4. Dispatch me for the sign-off (`.planning/reports/colour-vision/tech-lead.md`) after those reports, then merge in slot 6b, before candy-backdrop's sitting.

Next: code-auditor — the review now running stands as it is. Read Ruling 3 as the tech-lead's re-read of the `--cc` hunks, and Ruling 1's constraints as the acceptance list for the delta.

Next: qa-engineer — Ruling 2 replaces brief §7.3's device choice for this dispatch.
