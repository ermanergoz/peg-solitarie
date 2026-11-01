# tech-lead report — candy-backdrop: the jank-block ruling and the final technical sign-off
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; reviewed `git diff develop...HEAD`, develop `d251c3b`, HEAD `22a8802`, whose code equals `d029950`)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, testing, gitflow, stacks, dispatch, kotlin-android, compose, puzzle-design, play-store, skills-policy, logging and memory in full, plus `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh; the founder's Q-019 answer (option 3) puts Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Jank block (BD-23 (c)):** the FAIL stands as recorded, and I do not re-judge its numbers. It is neither a proven product defect nor a proven artefact. The instrument I wrote in R3 has three structural defects (J-1). I register a corrected instrument once, here, before any new data. QA runs (c) alone with it, once, at one bench visit (J-2). That result binds.
- **BD-09:** `BackdropStatusBandTest` compares the band against the theme's `background` (`LightColors` / `DarkColors`), with exact equality. It does not compare against `develop`.
- **15f is owed**, and it is folded into the same bench visit as a short visual check (item 3). It blocks nothing on its own.
- **Every other sign-off condition is met** (item 4). **VERDICT: HOLD.** The one next step is QA's bench visit under J-2 and J-3.
- **For the Q-PUBLISH-3.0.0 summary:** cold start on `Pixel_9a_bench` costs **+44.5 ms** (B 455.0 against A 410.5 ms; null floor 6.5 ms; threshold 100 ms). That is a PASS.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-signoff.md` — this report: the J-1 ruling, the J-2 instrument, the J-3 15f check, the sign-off table, the FAIL path and the 3.0.1 list.

## 1. The jank block: ruling

### J-1 — Why the FAIL cannot be read either way, and why it still stands
QA's literal result: median janky B **12.0**, A **10.5**, line `median_B ≤ median_A + 1` = 11.5, **FAIL by 0.5** (`qa-engineer-15e.md`, BD-23 (c)). R3 has no VOID clause. Its pre-registered line binds those numbers.

The defects are in R3's instrument. I wrote it, so they are mine:
1. **No null control.** I set a +1-frame tolerance without measuring the instrument's floor. The kept counts range 7–12 (A) and 5–18 (B). 15e's own null showed that two identical APKs differ by 6.5 ms. The noise of the jank count was never measured, so a 0.5-frame miss has no stated significance.
2. **It counts in a time window, not over a set of frames.** "Janky frames one second after `am start -W` returns" falls when an arm renders fewer frames. It can therefore reward the slower arm. Blocks 1–2 show this. Every frame there was janky (5/5, 6/6, 7/7 …), so the count equalled the frame count. In that regime B, with fewer frames, scored *lower* than A (5–6 against 7–9).
3. **No warm-up after the scale switch.** The slow regime began right after Remove animations went off. Both arms put 4 of their 12 kept samples in it.

**Judging on the steady blocks alone (3–6) is refused.** R3 allows no exclusion, and choosing the blocks after seeing them is a re-judgement. It would also not clear B on the merits. On blocks 3–6 the count reads B 13 against A 12, which is exactly at the line. Read as a share, it points the other way: B renders fewer frames in the window (median about 23 against 31), and a larger share of them miss (median about 58 % against 37 %). I record this as a warning about what the corrected run may show, not as a verdict.

**A recorded deviation is not the first answer.** A 20-minute bench run with a sound instrument costs less than sending the founder an unresolved technical question. The deviation remains an option only on the FAIL path below.

**Ruling:** this is an instrument question with a possible product signal behind it. It gets one fresh run of (c) alone, under J-2, registered now and before any new data (the rule of rules/testing.md for a corrected line, applied here). The old numbers are never re-judged. The corrected run can fail. If it does, the FAIL is a product finding.

### J-2 — The corrected (c), run once (replaces R3's (c) instrument; R3's other text stands)
**Instrument.** `Pixel_9a_bench`, API 36, debug variant, from a fresh cold boot (`-no-snapshot-load -no-snapshot-save -no-boot-anim`). Nothing else is run on this boot except 15f (J-3), which comes after the jank block.

**APKs.** Use `armA-d251c3b.apk` (sha256 `aa4d6e2d…a053`) and `armB-599c88d.apk` (sha256 `b2ee555f…eeac`) from `.planning/qa/candy-backdrop/15e/`, after re-verifying each sha256.
- If either file is gone, rebuild it as in R3 (a detached worktree per arm, `:app:assembleDebug`, daemons stopped before the boot) and record the new sha.
- Arm B's code must equal `d029950`. Arm A must equal `git merge-base HEAD develop`.
- If `develop` has gained a start-path commit (R3's list), stop: 15e is owed again first.

**Preconditions.** 15e's preconditions 2–9 apply unchanged, each recorded with its value:
- one emulator;
- no Gradle or Kotlin process;
- `uptime` ≤ 5.0 at the start and at the end;
- `cmd package compile -m speed -f` after each swap, and identical ART status;
- the component resolved from the device;
- swaps only at block boundaries, with each installed `base.apk` sha matched;
- the app data byte-identical to `db-start` at the start and at the end;
- `Data-Free` ≥ 4 GB.

Precondition 1 is inverted for this block:
- Turn Remove animations off through Accessibility only, never `settings put`. Record the scales as `1.0 1.0 1.0`.
- Restore them at the end and record `0.0 0.0 0.0`.

**Sequence** (blocks of five launches each):
1. **Warm-up:** W-A, then W-B. Record all ten launches and keep none of them.
2. **Null control:** N1, N2, N1, N2, all with arm B's APK. Discard the first launch of each block, which leaves **8 kept per label**.
3. **Arms:** A, B, A, B, A, B. Discard the first launch of each block, which leaves **12 kept per arm**.

**Per launch:**
- `am force-stop`; `sleep 2`; `am start -W -n <component>`.
- Record `TotalTime` and `LaunchState`. The launch must be COLD, or the sample is invalid.
- `sleep 1`.
- `adb shell dumpsys gfxinfo com.erman.pegsolitarie framestats`. Save the whole output per sample.

**The value per sample: deadline misses among the first 16 frames.** QA's script computes it:
- Take the `---PROFILEDATA---` block under the `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity` window.
- Read the columns by their header names, never by position.
- Order the rows by `IntendedVsync`, and drop the rows whose `Flags` ≠ 0.
- Take the first **16** rows. The value is the number of those rows with `FrameCompleted > FrameDeadline`.
- If the header has no `FrameDeadline`, use `FrameCompleted − IntendedVsync > FrameInterval` instead, or 16,666,667 ns when there is no `FrameInterval` either. Use one rule for the whole run, and name it in the report.

**Invalid samples.** A sample is invalid when it has fewer than 16 rows with `Flags` = 0, has a non-COLD launch, or has an unreadable dump. Record each invalid sample with its reason, and exclude it.
- A block with more than one invalid sample among its kept positions is void. Re-run it once, appended at the end of the sequence, with the same label.
- If any label still has fewer than its kept target (8 per null label, 12 per arm), the run is **VOID**.

This rule also detects the slow regime: at about 6 frames per second, a sample cannot supply 16 frames.

**Informative only, never part of the verdict:**
- the summary's "Janky frames" and "Total frames rendered" lines;
- the dump's `Pipeline=` line (the bench's emulator log shows a lavapipe Vulkan ICD, so record whether HWUI rendered on Vulkan or OpenGL);
- the per-sample TotalTime;
- the 50th/90th percentile frame duration over the 16 frames.

**Verdict.**
- floor = |median_N1 − median_N2|; T = max(1, 2 × floor).
- **VOID** when floor > 2, or under the invalid rule above. QA makes one re-attempt on a fresh cold boot, the same day. A second VOID comes to me with the numbers.
- **PASS** when median_B − median_A ≤ T.
- **FAIL** otherwise. QA does not re-run a FAIL.

Quote at most 20 lines of output. Keep the artifacts under `.planning/qa/candy-backdrop/jank2/`. The report goes to `.planning/reports/candy-backdrop/qa-engineer-jank2.md`.

### J-4 — If the corrected block FAILs: a product finding, and what changes
The FAIL then belongs to the product: B misses more frame deadlines in the entrance's first 16 frames than A does, beyond the measured noise.
1. **The developer** (android-game-dev, opus) runs the `compose-performance-audit` now. It is owed before release anyway (rules/compose.md; R1 deferred three decisions to it: the L3 offscreen layer, the scene node's frame-rate hint, and O-B, whether idle Home animates). The input is J-2's saved framestats: which frame indices miss, and by how much.
   - If the misses cluster on the scene-ready frame (indices 2–3), the cause is the PathOp build in `BackdropScene`'s `drawWithCache` (three `op(...)` calls per wave).
   - If they are spread across the window, the cause is the per-frame redraw of the full-window scene.
   - The developer proposes the smallest change, with numbers, and makes no edit before my ruling.
2. **I rule on the change.** The same J-2 block is then re-run on the changed build, with no new instrument. If `navigation/`, `ui/home/` or `ui/theme/` change, 15e is owed again.
3. If no change is acceptable within the week, the producer may put the trade to the founder as a `Q-<NNN>`: ship 3.0.0 with the measured cost recorded, or slip. That is a scope and date decision, not mine. The branch does not merge on a FAIL without that answer.

## 2. BD-09's automated test
`BackdropStatusBandTest.theStatusBarBandIsTheBackgroundOnEveryDensityInBothThemes` (`app/src/test/.../ui/backdrop/BackdropPixelTests.kt:231–265`):
- It counts band pixels `!= (if (theme) DarkColors else LightColors).background.toArgb()` over 24 dp, for every `BackdropDensity`, in light and dark.
- It asserts that count is 0. That is exact equality, stricter than B-09b's ±1/255, and it never compares against `develop`.

Its scope is the backdrop host. The eight screens are covered by QA's captures (`qa-engineer-15e.md`, BD-09: 0 off pixels, maximum deviation 0, on every screen in both themes).

## 3. 15f
**Owed.** R3 says 15f is "re-observed at the same visit", and that visit did not observe it. The criterion's mechanics are unchanged and tested on every Check:
- `HomeScreenTest` (the placeholder, never zero);
- `HomeRibbonShapeTest` (the progress emission changes no pill's shape or height at 1.0, 1.3 and 2.0);
- `HomeStageTest` (the stage is identical before and after the first emission).

This branch changes only the ribbon pills' finish (`HomeProgressRibbon.kt`, 13 lines: fill to `glossFinish`). The observation half is still R3's, so it rides along on the J-2 visit at no extra boot.

**J-3, the 15f check.** It runs after the jank block, with the scales restored to 0.0, on arm B only, with no timing:
- three cold starts, each recorded with `adb shell screenrecord`;
- read frame by frame, as in `docs/ux-spec.md`'s `15f/r2` method;
- on first paint the ribbon shows "—" placeholders: no spinner, no blank band, no blocked screen, and no pill moves when the values arrive.

The result is PASS or FAIL. A FAIL is a product finding.

## 4. Sign-off conditions

| Condition | Evidence | State |
|---|---|---|
| Check green on the code under review | Developer's Check at `d029950`: green, 1,632 tests (`code-auditor-loop3.md`). `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0 | met |
| code-auditor PASS | `code-auditor-loop3.md` VERDICT PASS (H1 fixed; BD-01's three commands print nothing; no suppressions) | met |
| Ruled test edits | `code-auditor-loop2.md` lines 85, 90, 95, 102: 15m oracle, M1, R-1 with G-1, R-2, each "amended, not weakened". Unchanged to HEAD | met |
| QA device rows | `qa-engineer-device.md` FAILed only BD-01 (fixed in `d029950`, re-checked in loop 3) and BD-09 (amended by B-09b, `599c88d`; re-read PASS in `qa-engineer-15e.md`). Every other row ran passed. Its device build `3d3879f` differs from `d029950` by two comment lines only | met |
| BD-23 (a) | `BackdropFirstFrameTest`, inside the green Check. R3's flag (`BackdropSceneReady.kt`) is provided once from `AppNavHost` | met |
| BD-23 (b), 15e | `qa-engineer-15e.md`: PASS, +44.5 ms, all nine preconditions held, no start-path change since | met |
| BD-23 (c), jank | FAIL on R3's instrument. Re-run under J-2 | **open** |
| 15f | J-3, at the same visit | **open** |
| BD-09 | Test against `background` (item 2), plus QA's captures | met |
| Boundaries | The diff touches only `app/` (`navigation/`, `ui/`, `res/values/dimens.xml`, tests). There is no domain, data, schema, DataStore key, string, Gradle, catalogue, manifest or `THIRD_PARTY.md` change. `ui/backdrop/` imports no `data`/`domain`, and contains no `Random`, wall clock, `painterResource` or DataStore. New `@Suppress`/`@Ignore`/`!!`: 0 | met |
| Tech-design adherence | R1 (one host behind the NavHost, F-32 `Surface`), R3 (the one flag, set late through `withFrameNanos`), D1–D8 as recorded in `tech-lead-architecture.md`. No new dependency, so no licence row is owed | met |
| Branch up to date | `git merge-base HEAD develop` = `develop` = `d251c3b` | met |

**Closing the HOLD.** Send `qa-engineer-jank2.md` to me once it reports **PASS on J-2 and PASS on J-3**, with no deviation from either. I then add a short confirmation addendum to this report, and the sign-off becomes PASS. That is a read of one report, not a new review. On VOID or FAIL, follow J-2 and J-4.

## Checks
- `git rev-parse develop HEAD; git merge-base HEAD develop` — pass (`d251c3b`, `22a8802`, `d251c3b`).
- `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` and the same against `599c88d` — pass (exit 0 both; HEAD's code = the audited code = arm B).
- `git diff --name-only develop...HEAD | grep -E 'gradle|toml|THIRD_PARTY|build-logic|AndroidManifest|schemas|domain/|game-core'` — pass (no output).
- `git diff develop...HEAD -- app/src/main | grep -cE '^\+.*(@Suppress|@Ignore|!!)'` — pass (0); the same over `app/src/test` for `@Suppress|@Ignore` — pass (0).
- `grep -rnE 'import com\.app\.pegsolitaire\.(data|domain)|Random|System\.currentTimeMillis|SystemClock|painterResource|DataStore' ui/backdrop/` — pass (no output).
- `git diff develop...HEAD -- app/src/main | grep -E 'PreferencesKey|@Entity|@Database'` — pass (no output); new `<string` lines in `res`: 0.
- Read `BackdropPixelTests.kt:231–265`, `BackdropSceneReady.kt`, `AppNavHost.kt:170–177`, `SceneBackdrop.kt:150–253` — pass (as items 2 and 4 state).
- Read `.planning/qa/candy-backdrop/15e/jank.sh` and `~/.android/avd/Pixel_9a_bench.avd/config.ini`, and grepped `15e/emulator.log` (`hw.gpu.mode=auto`; `vulkan_mode_selected:lavapipe gles_mode_selected:host`) — informative (J-2's `Pipeline=` record).
- Not run: the Check, Gradle and any emulator. Gsd's and the auditor's gates are not re-run (rules/studio.md). `.planning/log/` was not read. Host load was 3.80 at the start.

## Open issues / risks
1. **J-2 may FAIL.** The steady blocks' frame shares (J-1) point to a real per-frame cost of the animated scene on this bench. If it fails, J-4 applies. Whether the cost reaches a phone GPU cannot be shown under "emulator only". The `Pipeline=` record helps judge that.
2. **The +44.5 ms cold-start cost** goes into the Q-PUBLISH-3.0.0 summary as measured. It is an emulator delta inside the 100 ms threshold. The ≤ 2 s physical-phone bar (D-03j, PLAT-08) was not re-measured on a phone.
3. **`compose-performance-audit` is owed before the 3.0.0 release** (rules/compose.md), for the backdrop, whatever J-2 gives. It is a release-checklist item for the release-manager and producer, not this merge.
4. **Staffing and trailer.** This report ran on Opus under Q-019. The commit carries rules/gitflow.md's single Fable trailer, as the branch's other commits do. That is the same open question the developer's, auditor's and QA's reports raise.

**3.0.1 items from this branch** (the producer files them):
- **G-2:** fold the two `UserFacingTextHasNoProtectedNamesTest` classes into candy's (`tech-lead-r1-guard.md`). The developer deletes `ui/UserFacingTextHasNoProtectedNamesTest.kt`, the ux-designer updates LS-AC17's references, and the auditor reviews the deletion.
- **`pseudoLocalesEnabled` on debug,** so that BD-22 runs on `ar-XB` as written (`qa-engineer-device.md`, Open issue 3).
- **The QA rows skipped under reduced QA:**
  - BD-27's pressed state;
  - gameplay at font scale 2.0 and at 360×640;
  - the wrapped title at 2.0;
  - the Results TalkBack walk;
  - Home with both chips;
  - the tablet matrix.
- **The first-launch frame with black system bars** (`qa-engineer-device.md`, Open issue 4): capture `develop`'s first launch and decide whether it is new.
- **Replace the adb jank read with a Macrobenchmark `FrameTimingMetric`** (and `StartupTimingMetric` with `reportFullyDrawn()` for 15f, as the ux-spec already names). This is infrastructure for the producer to schedule.

## Handoff
Next: producer — dispatch the qa-engineer for one bench visit on `Pixel_9a_bench`:
- Run J-2 (the corrected (c)) and then J-3 (15f), as written above.
- Hold P-4 (no Gradle anywhere, Pixel_9 off) for the visit.
- The report path is `.planning/reports/candy-backdrop/qa-engineer-jank2.md`.

Then:
- on PASS/PASS, re-dispatch me for the confirmation addendum;
- on FAIL, follow J-4 (the developer's audit first);
- on a second VOID, re-dispatch me with the numbers.

File the 3.0.1 items above, and add the +44.5 ms line and Open issue 3 to the Q-PUBLISH-3.0.0 material.

VERDICT: HOLD

## Addendum 2026-11-01 — after J-2's second VOID (`tech-lead-j2-ruling.md`)
- **15f:** met. J-3 PASS with no deviation (`qa-engineer-jank2.md`).
- **BD-23 (c), jank:** still **open**, and now on J-4.
  - J-2 is VOID and is not re-judged.
  - The saved framestats show a cost that belongs to arm B's APK: a one-off hitch of about +220 ms in the first five frames after Home's first paint, mostly on the RenderThread in frame 1. There is no steady-state jank.
  - The path is J-4a (the developer's audit), my ruling on the change, then J-5 (the corrected instrument) on the changed build. The stop rule J-4c leads to a founder `Q-<NNN>`.
- Every other row of item 4 is unchanged.

VERDICT: HOLD

## Addendum 2026-11-01 — after J-5 and BD-23 (d) (`tech-lead-signoff-final.md`)
- **BD-23 (c):** met. J-5 PASS, C − A = +57.7 ms ≤ 100 ms (floor 30.2 ms), `qa-engineer-j5.md`.
- **Every other row of item 4 is met.** `code-auditor-j4b-loop1.md` PASS under Q-019's 2026-10-24 addendum. 15e is not owed. Develop `452f187` = the merge-base, so no merge-in is owed.
- **Outstanding:** BD-23 (d)'s battery-saver clause. Item (4) is 2.045 s against 2.0 s in 1 of 4 runs, and the ux-designer is ruling on it.
- The full table, the HOLD paths, the Q-PUBLISH-3.0.0 lines and the 3.0.1 list are in `tech-lead-signoff-final.md`.

VERDICT: PASS — conditional on `ux-designer-bd23d-bs.md` closing the battery-saver clause on the existing data; otherwise HOLD per `tech-lead-signoff-final.md` §3.
