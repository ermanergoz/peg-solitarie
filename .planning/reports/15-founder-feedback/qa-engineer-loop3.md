# qa-engineer report — S11 founder-feedback slice, fix round 2 (loop 3), device re-walk in the bundled faces
Date: 2026-10-17
Branch: feature/phase-15-ux-refresh (2cfad41..e1875cb; built from the detached worktree `PegSolitarie-p15-qa` at e1875cb, the exact code of the branch; later commits add `.planning` records only) (committed by the producer)
Skills loaded: ergoz-soft:standards (role qa-engineer; the plugin 1.0.0 copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing, read in full because the product's snapshot is 0.5.1; the product's lessons.md), android-emulator-skill, android-testing
Ran in parallel with: candy-backdrop ux spec (documents only)
Ran on: claude-opus-5-5 (Opus 5.5) at effort high — **staffing deviation:** the qa-engineer is pinned to fable; this ran on Opus under the founder's Q-003 (phase-15 review chain) and Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), recorded in `.planning/questions/FOUNDER.md`. The developer also writes on Opus, so every reading below was produced by this session on the device or the host; no recorded result was taken on trust.
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

Inputs: `docs/ux-spec.md` (D-03l, D-03m, D-04e, D-05d, §4.2.4, §4.3.1, §6.3, §6.5, criteria 15–15r and 18–18e), `tech-lead-fonts.md`, `qa-engineer.md`, `qa-engineer-loop2.md`, `code-auditor-loop2.md`, `code-auditor-loop3.md`, `ux-designer-15p.md`, `ux-designer-6-5-and-d03m-check.md`, `FOUNDER.md` (Q-003, Q-013, Q-019). Not read: `android-game-dev*.md`, `.planning/log/`. An earlier dispatch of this task left files under `.planning/qa/15-founder-feedback/` (a `loop3/` folder and a `founder/` folder, written 15:15–21:16 today); none of it is used here. I moved both aside with `mv` to `prior-cutoff-loop3/` and `prior-cutoff-founder/` so that `loop3/` and `founder/` hold only this session's files.

VERDICT: PASS

## Summary
- **The bundled faces render on the device.** The gameplay title "Level 111" has an ink width of **81.52dp**: Fredoka Medium gives 82.10dp and Roboto 88.86dp, and the pass band is 82 ± 2. The Home Level value "111" is 17.90dp (Fredoka 17.77, Roboto 23.15). Nunito's glyphs match a HarfBuzz render of the bundled file letter for letter. Every reading below was taken in these faces.
- **Every walked criterion passes:** 18a, 18d (pill constant to the pixel at five readings, at 1.0 and 2.0, on both windows, and across a live font-scale change), 18e, 18b/18c, 15g, 15i, 15k, 15m, 15o, 15p (24 window × chip-state readings), 15f on its letter, the tutorial captions, and a light/dark, 1.0/2.0 walk of every screen. The Check is green with every task executed. The crash buffer is empty.
- **15e: VOID, not run.** The Pixel_9a's `/data` is 95% full (≈306MB free against Android's 304MB low-storage floor), so `adb install` refuses both arms. I made two attempts, and freeing space would mean wiping the AVD or deleting other packages' data, which I may not do. Escalated as **E-1**. Per 15e, the slice does not close until a decided sitting exists.
- **Two medium findings, neither in this round's diff.** **F-1:** on every cold start, Home reflows about 0.6s after first paint, when the Continue chips arrive. **F-2:** the status-bar icons are white on the light theme (≈1.1:1 contrast). The airplane-mode half of AC-F7 was not run: a radio toggle is prohibited on the shared device. It is replaced by a check that can fail, and escalated as **E-2**.

## Device and build
- **Device:** `emulator-5554`, AVD **Pixel_9a** (`adb emu avd name`), `sdk_gphone64_arm64`, API 36, `PAGE_SIZE` 16384, 1080×2424 at 420dpi (411.4×923.4dp). Status bar 54.1dp, gesture bar 24.0dp. It was the only emulator running (one `qemu-system` process, started by the earlier dispatch with `-avd Pixel_9a -no-snapshot-save`). I did not reboot it.
- **Found state** (restored at the end, see Checks):
  - The app was installed with base.apk sha256 `3137f53e…a3dc`, which equals my arm-B build of e1875cb.
  - App data was in place, the app was in front, and dexopt was `verify (cmdline)`.
  - Night mode was on, animations were 0/0/0 and the font scale was 1.0.
  - Backed up before any change: `backup/found-app-data.tar`, plus `found-screen.png`.
- **Build under test:** `./gradlew --no-daemon -q :app:assembleDebug` in `PegSolitarie-p15-qa` (e1875cb), giving `app-debug.apk` with sha256 `3137f53eeabdc9df2ce75e6840f7e6f620fd8711719c79d2fe34f77c476ea3dc`, the APK installed for the whole walk.
  - It carries `res/font/{fredoka_medium,fredoka_semibold,nunito_regular,nunito_semibold,nunito_bold}.ttf` and `assets/licenses/{Fredoka,Nunito}-OFL.txt`.
  - Each font's sha256 in the APK equals the tech-lead's D1 table (`8143bd54…`, `021e30b2…`, `b0885056…`, `c7fd1944…`, `ad443e47…`).
- **Windows:**
  - native (≈ `w412dp-h915dp`);
  - `wm size 1080x1920` + `wm density 480`, giving 360×640dp with a 52dp status bar and a 24dp navigation bar (564dp usable);
  - for 15p, also 360×720 and 360×800 at density 480, and 412×780, 412×850 and 412×915 at density 420.
- **Player state:**
  - loop 2's 200-level ladder (`SUM(stars)` 401; Room identity hash `28f7b5a4…` equals `app/schemas/…/2.json`);
  - `challenge_current_level` rewritten in `app_prefs.preferences_pb` (111, 1234 or 1);
  - four chip-state copies of the database (none, Classic only, Challenge only, both).

## AC-F7 — the bundled faces render (the precondition for every reading)
Ink widths are first-ink to last-ink columns on the screenshot (an `|ΔRGB| > 90` threshold against the box's dominant colour), divided by the density. The expected values are my own `hb-shape --show-extents` runs on the bundled files and on the device's own `/system/fonts/Roboto-Regular.ttf` (wght 400/500/600). They match the tech-lead's table.

| Probe | Style | Device | Fredoka / Nunito | Roboto | Verdict |
|---|---|---|---|---|---|
| Gameplay title "Level 111" (cold start) | titleLarge 22sp, tracking 0 | **81.52dp** | 82.10 | 88.86 | **PASS** (82 ± 2) |
| Home Level pill value "111" | titleMedium 16sp | **17.90dp** | 17.77 | 23.15 | **PASS** |
| Home Challenge chip "Continue — Level 111" | bodyMedium 14sp | **139.05dp** | 137.89 (no tracking) to 142.64 (0.25sp) | 129.32 to 134.07 | **Nunito**: outside the ruling's literal 142.6 ± 2.5 band, but 5dp beyond Roboto's widest; see O-1 |
| Same chip, glyph shapes | — | the device crop beside a HarfBuzz render of each face (`fonts/nunito-vs-roboto.png`) | matches Nunito: flagged "1", 1.0em dash, "t" | Roboto's "1" and its short dash differ visibly | **Nunito** |
| Home title "Peg Solitaire" | displaySmall 36sp | 203.05dp | 203.26 (SemiBold) | 202.14 | not a discriminator; it shows the title is whole |

- **Cold start:** both Home probes come from a `LaunchState: COLD` launch (`fonts/01-home-level111-light.png`), and so does the gameplay probe (`fonts/02-game-level111-light.png`).
- **Airplane mode: not run** (E-2). `cmd connectivity airplane-mode enable` is a radio toggle, which my role may not use on the shared device (agent file, PROHIBITED; F-51, F-28).
- **The substitute, which can fail:** I scanned all 30 dex files and `resources.arsc` of each arm.

  | | arm B (e1875cb) | arm A (develop 3f2af90), positive control |
  |---|---|---|
  | `com.google.android.gms.fonts` (the provider authority) | **0** | 5 |
  | `androidx/compose/ui/text/googlefonts` classes | **0** | 47 |
  | `com_google_android_gms_fonts_certs` resources | **0** | 3 |
  | `pegsolitaire/ui/theme/AppFonts` | 2 | 0 |

  The build has no code path that can fetch a face, so the network cannot change what renders. On the old downloadable path the same scan finds the provider. AC-F4's `git grep` returns 0 code hits; the one line is Nunito's OFL notice URL.

## Criteria — verdicts
| Criterion | Verdict | Device evidence (this session) |
|---|---|---|
| **18a** title/timer sizes | **PASS** | The pill is 40.00dp tall at 1.0. At 360dp and font 1.0, "Peg Solitaire" is whole at every timer value up to 100:00 (`timer/strip-sheet-c360.png`). "Level 1234" ink is **103.00dp** against 102.74dp for the whole string in Fredoka, so there is no ellipsis (`walk/c360-level1234-strips.png`). The styles themselves are covered by the JVM tests. |
| **18b** each bar once | **PASS** (spot-check) | At 360×640: the back chip's top is at 56.0dp against the 52.0dp status bar (4.0dp). The dock slab's painted bottom is at 1800px = **600.0dp** = 616 − 16. |
| **18c** board keeps its size | **PASS** (spot-check) | At 360×640, the slab colour `#F1EAFB` runs px 48..1031, which is **328dp**, at font 1.0 and at 2.0. The shadow-inclusive extent is identical at both scales (334dp). |
| **18d** no jitter (shipped font confirmed) | **PASS** | Pill width from the tree, with the pixel run of the pill fill in brackets: native 1.0 **86.10dp** [86.48] at 00:00, 08:08, 11:11, 22:22 and 59:59; native 2.0 **116.57dp** [116.95] at all five; 360dp 1.0 **85.67dp** [86.00] at all five; 360dp 2.0 **116.33dp** [116.67] at 00:00, 11:11 and 59:59. The digits' own ink varies (11:11 is 54.86dp, 22:22 is 59.81dp), which proves the proportional Fredoka digits sit in fixed cells. **Live scale change:** with the timer running, 00:02, 00:05 and 00:08 at 1.0 read 86.67dp, and after `font_scale 2.0` in place, 00:14 through 00:28 read 117.33dp (pixel runs; the pill keeps one width at each scale with no relaunch). 100:00 adds one cell (99.67 / 137.33dp), as loop 2 recorded (I-1). |
| **18e** 200% | **PASS** | At 360dp and 2.0, "00:00", "59:59" and "100:00" are whole in a 48.00dp pill grown to 116.33 / 137.33dp. Only the title ellipsises ("Peg Sol…", "Peg S…", "Level 1…"). The node text stays "Peg Solitaire", and the Challenge description reads "Challenge level 1234". |
| **15o** title row | **PASS** | **Native:** title 16.0..347.4dp, gear 347.8..395.8dp (content end 395.4, Δ0.4); vertical ranges 72.0–116.2 and 70.5–118.5. **360×640:** title starts at 8.0dp and the gear ends at 352.0dp, both on the content edges. In both, nothing sits above the title and its ink is whole (203.05dp). At 2.0 the title stays on one line, 53dp tall, and the gear stays in the row. |
| **15p** no empty middle gap | **PASS** | **Part 1** (native, real insets): stage art **308.6dp** with no chip and **235.4dp** with one chip, both ≥ 120. Largest gap between visible elements: **16.0dp**. Hero→art reads 15.6 because the pixel edge includes the art's shadow. **Part 2:** all 24 window × chip-state readings. The largest gap is **55.0dp** (360×640, both chips, no art drawn), against the compact bound of 64; on full-set windows it is 16.0dp against 80. **Part 3:** one-chip art is ≥ 120 on every full-set window (235.4 native, ≈230 at 412×915). Table: `grid/*.xml/.png`. |
| **15g** tall phone | **PASS** | Native, all four chip states: tile 1's top is at 557.3 / 581.3 / 605.3dp, below the 476.8dp content midpoint. No band is pinned to the bottom, and the largest gap is 16.0dp against the tiles' 208dp. Footer against slack, with no chip: the stage slot is 305.9dp and the footer spacer ≈102dp (134.1dp below tile 2 less 32dp of gap and padding), so the footer is ¼ of the slack, as §4.2.4's table designs. |
| **15k** 360×640, both chips, real insets | **PASS** | Hero 60–108, ribbon 163–203, chips 211–291 and 299–379, tiles 387–483 and **491–587**; viewport 52–616. **Overflow 0dp**: no scroll container in the tree, as in loop 1 (O-4). Part 1 complete. Part 2 holds trivially (nothing overflows). Part 3: 0 ≤ 120. Part 4: nothing clipped, compact pills, no art. |
| **15m** narrow ribbon | **PASS** | At 360×640 and 1.0: pills 40dp tall (band ≤ 56), label and value on one line with no glyph ("Stars 401", "Boards 0/5", "Level 111"). Each pill is one non-clickable node described "Stars, 401" etc. A fresh profile reads "Stars, 0", "Level, 1". |
| **15i** 200%, both baselines | **PASS** | On 360×640 and native, with both chips at 2.0: Home scrolls (`ScrollView scrollable=true`), and every band is reached by one swipe. Nothing is clipped, no band is dropped, and the title is whole on one line. Carried open: O-2 (labels broken mid-word, below). |
| **15f** first paint | **PASS on its letter; F-1** | I screen-recorded a cold start (`15f/r2/coldstart9s.mp4`, frame times from `ffprobe`). First Home frame at 2.528s: ribbon "—" / "—" / "111", no spinner, title already in Fredoka, so there is no face swap. Stars and Boards fill at 2.894s **with no pill moving**. But see F-1: the Continue chips arrive at 3.140s and the column re-lays out. |
| Tutorial captions at 360dp | **PASS** | The board slab sits at y 424–1273px and x 116–963px in **every** step (T1, T2, T3, after T3's first jump, completion). The T2 caption takes 2 lines (40dp) and T1/T3 take 1 line (20dp) inside the reserve. T1 fits on one line here: ink 326.67dp of 328dp, whole (`tutorial/c360-t1-caption.png`), not the two lines the ruling projected. The whole tutorial was walked by real jumps (T1 horizontal, T2 vertical, T3 four jumps), then "Got it" led to the fresh Home. At 360×640, "Got it" sits 12dp under the gesture bar until the column is scrolled, which reveals it whole (pre-existing tutorial layout, noted only). |
| Whole-app walkthrough | **PASS** | Home (fresh / Classic chip / both chips), Classic resume, Challenge 111 and 1234 with a real jump, Classic catalogue, Settings, Results ("Perfect!", three stars) after solving Challenge level 1, and the tutorial. Light and dark at 1.0 native, and light and dark at 2.0 on 360dp (`fs2/screens/sheet-*.png`). Nothing clipped or truncated beyond O-2 and the allowed title ellipsis. 0 `E` lines in the app's log. |
| **15e** cold start | **VOID — not run (E-1)** | See the table below. |

## Cold start — criterion 15e (§6.5 second edition, Ruling 2)
**VOID: precondition 7 cannot be met on this AVD.**
- Arm A: `develop` 3f2af90. I built it in a new detached worktree, `PegSolitarie-armA-qa3`, which is removed afterwards. sha256 `8d8e3a5ac1c2f25e15f5112589da81d64e60d4c16aa7a953cc3ea96fa7aa3761`.
- Arm B: e1875cb, sha256 `3137f53e…a3dc`.
- Both APKs are signed by certificate digest `8a4e64a3…a4a6`, and both have the same `applicationId`.
- Schema identity holds: `git diff --stat develop e1875cb -- app/schemas` is empty, and the diff adds or removes no `*PreferencesKey`.
- I stopped the Gradle daemons, and `pgrep -fl 'GradleDaemon|KotlinCompileDaemon|gradlew'` found nothing.
- Then the first block's install failed:

```
adb: failed to install …/armB-e1875cb.apk:
android.os.ParcelableException: java.io.IOException: Requested internal only, but not enough space
```
- A second attempt, with arm A, failed the same way.
- `dumpsys diskstats` reads `Data-Free: 313004K / 6082144K total = 5% free`. Android refuses an install below max(5% of /data, …) plus the package size, and 5% here is ≈304MB.
- The space is held by system and Google packages and two other studio products (`com.erman.percentagecalculator`, `com.p2pchat.android_p2p_chat`). Making room means wiping the AVD, trimming other packages' caches or uninstalling other packages. All three are forbidden to me on this device, and the first is forbidden by the dispatch.
- The ART reset I tried on our own package (`cmd package compile --reset`) freed nothing: free space went to 263MB. I put it back to `verify (cmdline)`, and free space is back at 309.8MB.

| # | Precondition | Recorded value | Held? |
|---|---|---|---|
| 1 | AVD `Pixel_9a`, API 36, debug variant, animations off | Pixel_9a, API 36, `PAGE_SIZE` 16384, debug APKs, animation scales 0/0/0 (as found) | Yes |
| 2 | Exactly one emulator | `adb devices` 1, `qemu-system` processes 1 | Yes |
| 3 | No Gradle build or other measuring session | no daemon after the arm builds (21:17); the producer declared none | Yes |
| 4 | 1-minute load ≤ 5.0 (`hw.ncpu` 10 ÷ 2) at both ends | 21:13 `1.27 1.64 2.42`; 21:17 (after the builds) `4.19 2.29 2.45`; no sitting, so no "after" reading | Before: yes |
| 5 | ART normalised (`compile -m speed -f`) after every swap | not reached | — |
| 6 | Component resolved from the device | `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity` | Yes |
| 7 | Each arm swapped in at a block boundary by `adb install -r -d --no-incremental` | **refused both times: "not enough space"** | **No** |
| 8 | ~200-level ladder | seeded; Home read "Stars, 401" (`coldstart/seeded-home.png`) | Yes |

No null table, no arm tables, no floor and no delta exist, and no number is reported. **E-1 (escalation, to the producer):** a decided 15e needs storage on the instrument of record. The options are the producer's and founder's, not mine:
- free space on `Pixel_9a` by uninstalling the other products' test apps or trimming GMS caches there;
- grow its userdata partition (needs a wipe, which the dispatch forbids);
- have the tech-lead name another instrument.

Pixel_9a's Play Games and consent state are untouched by this session.

## Findings
**F-1 — Home re-lays out about 0.6s after first paint on every cold start for a player with a save (medium; not in this round's diff; owner: ux-designer for the ruling, tech-lead for the cause).**
- **Steps:**
  1. Have a player with a Classic and a Challenge save.
  2. `am force-stop`, then start with `adb shell screenrecord`.
  3. Read the frames.
- **Expected:**
  - §4.2.2 (`docs/ux-spec.md:1551-1552`): "a layout that reshuffles when a Continue chip appears is worse than one that is stable per device".
  - 15f: first paint fills "without the layout shifting".
  - 15r: the stage renders identically across the first emissions.
- **Actual** (`15f/r2/coldstart9s.mp4`, frames `f_020`–`f_035`; contact sheet `15f/r2/sheet.png`):
  - **2.528–2.744s:** Home with no chips. The stage is a large empty slab with no pegs, and the Classic tile's art is missing.
  - **2.791s:** the pegs and the tile art appear.
  - **2.894s:** Stars and Boards fill, with no shift.
  - **3.140s:** both Continue chips appear. The stage shrinks from the no-chip size (≈306dp) to the two-chip size (≈163dp), and the ribbon and tiles jump. Frame 32 catches the stage mid-redraw.
  - The first 4s recording (`15f/coldstart.mp4`) shows the same sequence.
- **Why it is not counted as a FAIL:** on their words, 15f concerns the ribbon values and 15r the first *progress* emission, and both hold. The reshuffle comes from the resume flags arriving after first paint, which the stage now magnifies (the stage is sized by slack, D-03l). The founder's own state, one chip, sees it on every cold start.
- **What would settle it:** a ruling on whether chip arrival may move Home, and if not, the developer reading the resume flags before first paint, or reserving the space. Nothing in the tests covers the timing.

**F-2 — status-bar icons are white on the light theme, on every screen (medium; pre-existing; owner: developer through the tech-lead).**
- **Steps:** light system theme, open any screen.
- **Expected:** the system bar icons readable against the app's light background (rules/compose.md contrast; Android's light-status-bar appearance).
- **Actual:** the clock and icons draw `#FFFFFF` on `#F4F1FA` (Home, gameplay) and on `#FBF9FF` (Results, catalogue, Settings), about 1.1:1 (`walk/statusbar-light-crops.png`). `themes.xml:8` uses `android:Theme.Material.Light.NoActionBar` with no `windowLightStatusBar`, and no code sets the bar appearance (`git grep enableEdgeToEdge|SystemBarStyle|isAppearanceLightStatusBars` in `app/src/main/java` returns 0 hits). Dark theme is correct.
- **Scope:** loop 2's light Home capture (`loop2/regress/01-home.png`) shows the same, and `themes.xml` has not changed since `develop`. It is more visible since D-03/D-05 removed the top bars. It shows in the founder's light screenshots.

## Prior findings — closed or open
| Item | Status on this build |
|---|---|
| loop 1 F-1 (18d jitter in Fredoka) | **Closed on the device**: one pill width at every reading, in Fredoka. |
| loop 2 F-A (faces never load) | **Closed**: the discriminators pass. |
| loop 2 F-B (corrupt provider certificate) | **Closed by deletion**: no provider, certificate or GMS font authority in the APK. |
| loop 2 F-C (18d tests blind to the cell face) | Closed by the code-auditor's M2 run (loop 3). Not re-mutated here; the device reading is the stronger check. |
| loop 1 E-1 (15p hero reading) | **Closed**: D-03m is in; hero→art reads 16dp (15.6 shadow-inclusive) on every full-set window. |
| loop 2 E-1 (15e precondition 7 wording) | **Ruled** (Ruling 2), but **blocked again here** by device storage (new E-1). |
| code-auditor loop 2 N-1, N-2 | Closed (code-auditor loop 3). On the device, the clock reads as one node with text and description "Elapsed time" and no children. |
| loop 1 O-2 / ux-designer R-3 (200% compact pill labels break mid-word) | **Open, worse in Nunito**: "Sta/rs", "Bo/ard/s" on 3 lines, "Leve/l" at 360dp and 2.0 (`fs2/sheet-c360.png`). Nothing is clipped, so 15i holds. Still unruled, for the ux-designer. |
| loop 2 O-A / auditor I-2 (TalkBack may say "Elapsed time" without the value) | **Open**, unchanged: same node shape, utterance not captured (adb cannot inject TalkBack gestures). |
| ux-designer R-1 (5-digit level chip tight at 360dp) | Not reproduced at level 1234 (chip one line, 144dp node). Not seeded at 5 digits. |
| ux-designer O-1 (tablet blank) | Out of scope for this walk (phones only). |
| code-auditor loop 3 MINOR (THIRD_PARTY rows on phase-14 branch) | Not a device item; still open per that report. |

## Observations (no verdict impact)
- **O-1 (tech-lead): the Nunito discriminator band assumes 0.25sp tracking.**
  - Material3 1.4.0 (the version resolved here) sets `BodyMediumTracking` to **0.2sp**, read from `TypeScaleTokens` bytecode (`ldc2_w 0.2d`).
  - The device's effective tracking also varies by string: the chip implies ≈0.06–0.08sp per glyph, the T1 caption ≈0.17.
  - So "Continue — Level 111" reads 139.05dp, below the literal 142.6 ± 2.5 band, while being 5dp beyond any Roboto reading. The glyph-shape comparison settles the face.
  - A future discriminator should use a tracking-free role, such as titleLarge's "Level N".
- **O-2:** after the first jump on a fresh board the timer runs, and after a resume it restarts at the first move (existing behaviour, loop 1).
- **O-3:** `.planning/qa/` now holds about 310MB (this task's folder about 250MB, mine 99MB, of which 40MB is the two arm APKs). That is over the 200MB cap in rules/testing.md, for the producer to prune.

## Test review (step 4)
- I read the fix round's tests against the criteria.
- **`AppTypographyRendersBundledFacesTest`:** its 15-role table (`TypeScaleFixture.kt:47-63`) is a literal list. Its oracle is typed from HarfBuzz, not measured by the code under test. I re-derived three of its cells on the device's own text stack (Fredoka Medium "Level 111" agrees within 0.6dp).
- **D-03m's `HomeStageTest`:** the 16dp hero→art reading matches the device, which shows 16dp on every full-set window.
- **18d:** `TimerDigitsTest` holds each cell to Fredoka's "2" (587/1000). That is the same property the device shows.
- **Mutations:** the code-auditor's ten mutation runs at loop 3 cover the teeth. I did not repeat them. Every device reading above is this session's own.
- **Gaps:** no test can see F-1's timing, and none could see F-2.

## Checks
- `cd PegSolitarie-p15-qa && ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon --no-build-cache --rerun-tasks check` — **pass**: `BUILD SUCCESSFUL in 11m 37s`, `98 actionable tasks: 98 executed`, exit 0 (21:55–22:06). There is no gsd gate record for this review loop, so I ran it myself.
  - JUnit XML: app **1,061** tests, domain **275**, each with 0 failures, 0 errors and 0 skipped.
  - detekt and ktlintCheck ran on both modules. Lint: `0 errors, 57 warnings`. `:domain:depthSweep` ran.
  - Full output: `check/gradlew-check.log`. Daemons stopped afterwards, and the arm-A worktree was removed (`git worktree remove --force`, then `prune`).
- `./gradlew --no-daemon -q :app:assembleDebug` in `PegSolitarie-p15-qa` (e1875cb) and in a fresh detached worktree at `develop` 3f2af90 — pass. The APK sha256s, the signer digests and the font hashes are quoted above. `./gradlew --stop` ran in both, and `pgrep` was empty afterwards.
- `unzip -l` / `unzip -p … | shasum -a 256` on arm B — 5 fonts and 2 licences, with the fonts equal to D1.
- `git grep -n -E "googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle` — 1 line, Nunito's OFL notice URL (no code).
- `hb-shape --font-size=1000 --output-format=json --show-extents` (HarfBuzz 14.4.0) on the bundled files and the device's `Roboto-Regular.ttf` — the expected ink widths above.
- Dex and `resources.arsc` scan of both arms — the table in AC-F7.
- `adb install -r -d --no-incremental` (arm B 21:18, arm A 21:19) — **fail** both times ("Requested internal only, but not enough space").
- `adb logcat -d -b crash` — empty (0 bytes). `adb logcat -d --pid=$(pidof …) | tail -n 500` — 0 `E` lines from the app.
- **Restore:**
  - `run-as … tar -xf found-app-data.tar` after removing the session's database and prefs. Re-archived: `peg_solitaire.db`, `-wal`, `app_prefs.preferences_pb` and `com.erman.pegsolitarie_preferences.xml` all have the same sha256 as the backup.
  - base.apk `3137f53e…` (unchanged). Dexopt `verify (cmdline)` as found.
  - Font scale 1.0, `wm size`/`density` physical (1080x2424 / 420), night mode **yes** (as found), animations 0/0/0 (as found, never changed).
  - Free storage 309.8MB.
  - App relaunched on Home (`final-home.png`).
- **System changes made and reverted:**
  - `settings put system font_scale` (1.0 ↔ 2.0), `wm size` / `wm density`, `cmd uimode night` (no ↔ yes).
  - The app's own data (seeded, varied, restored).
  - `cmd package compile --reset` / `-m verify -f` on our package.
  - Files under `/data/local/tmp` and `/sdcard` created and removed.
- **Not touched:** no `settings put global|secure`, no radio toggle, no other package force-stopped or cleared.
- Excerpt, `logcat.txt` (the app's pid, last launch; every warning is platform noise):
```
10-17 21:51:57.427 12739 12739 W com.erman.pegsolitarie: type=1400 audit(0.0:238): avc:  granted  { execute } …
10-17 21:51:57.695 12739 12745 W an.pegsolitarie: userfaultfd: MOVE ioctl seems unsupported: Connection timed out
10-17 21:52:17.661 12739 12745 W an.pegsolitarie: Cleared Reference was only reachable from finalizer (only reported once)
```

## Founder screenshots (Q-013) — `.planning/qa/15-founder-feedback/founder/`
All are from the install that passed the discriminators (base.apk `3137f53e…`), on the Pixel_9a at its native 1080×2424 and font 1.0. "Returning player" is the seeded profile: 401 stars, ladder level 111, one Classic save. It is not the founder's own data.

| Path | Size | What it shows |
|---|---|---|
| `founder/01-home-fresh-light.png` | 198 KB | Home after the tutorial on a fresh profile: Stars 0, Boards 0/5, Level 1, no chip, the full stage |
| `founder/02-home-fresh-dark.png` | 193 KB | The same, dark |
| `founder/03-home-returning-light.png` | 197 KB | Returning player: Stars 401, Level 111, Continue Classic chip, stage at one-chip size |
| `founder/04-home-returning-dark.png` | 191 KB | The same, dark |
| `founder/05-game-midgame-light.png` | 263 KB | Classic English-33 resumed mid-game, timer 05:12, dock with Undo/Hint available and Show-moves off |
| `founder/06-game-midgame-dark.png` | 265 KB | The same, dark |
| `founder/07-classic-catalogue-light.png` | 193 KB | Classic catalogue, five boards with star pips |
| `founder/08-classic-catalogue-dark.png` | 191 KB | The same, dark |
| `founder/09-challenge-level111-light.png` | 102 KB | Challenge "Level 111", fresh board, timer 00:00 |
| `founder/10-challenge-level111-dark.png` | 103 KB | The same, dark |
| `founder/11-results-light.png` | 68 KB | Results after solving Challenge level 1: "Perfect!", three stars, score, time, pegs left |
| `founder/12-results-dark.png` | 72 KB | The same, dark |
| `founder/13-settings-light.png` | 161 KB | Settings: How to play, Sound, Haptics, Play Games, privacy rows |
| `founder/14-settings-dark.png` | 156 KB | The same, dark |
| `founder/15-tutorial-step1-light.png` | 86 KB | Tutorial step 1 of 3 with the guided first jump and its caption |
| `founder/16-tutorial-step1-dark.png` | 89 KB | The same, dark |

The founder should know two things when looking at these:
- The light screenshots show F-2: the status-bar icons are invisible.
- The type is the real Fredoka/Nunito for the first time in any QA capture.

## Outputs
- `.planning/reports/15-founder-feedback/qa-engineer-loop3.md` — this report (not committed; the producer commits it).
- **Artifacts (git-ignored).** Everything is under `.planning/qa/15-founder-feedback/loop3/` unless the path starts with `founder/`.

  **Device state and builds:**
  - `backup/found-app-data.tar` (5.3 MB): the Pixel_9a's app data as found.
  - `backup/after-restore.tar` (5.3 MB): the restore check.
  - `backup/found-screen.png` (192 KB): the screen as found.
  - `coldstart/armA-3f2af90.apk` (20.6 MB) and `coldstart/armB-e1875cb.apk` (21.0 MB): the two arms. Arm B is byte-identical to the APK found installed; I deleted my duplicate copy of it.
  - `coldstart/seeded-home.{png,xml}`: "Stars, 401".

  **Seeding:**
  - `seed/peg_solitaire.db`: the 200-level ladder.
  - `seed/variants/{none,classic,challenge,both}.db`: the chip states.
  - `seed/{test111,level1,level1234,fresh}.pb` and `app_prefs_level111.preferences_pb`: the prefs variants.
  - `seed/found/…`: the found prefs, extracted.

  **AC-F7:**
  - `fonts/01-home-level111-light.{png,xml}` and `fonts/02-game-level111-light.{png,xml}`: the cold-start discriminator captures.
  - `fonts/ref-{nunito,roboto}.png` and `fonts/nunito-vs-roboto.png`: the glyph comparison.

  **18d / 18e:**
  - `timer/{fs1.0,fs2.0,c360-fs1.0,c360-fs2.0}-<ms>.{png,xml}` (≈260 KB / 12 KB each): one pair per reading.
  - `timer/strip-sheet-fs1.0.png` and `timer/strip-sheet-c360.png`: the strips side by side.
  - `timer/live/fs{1,2}-*.{png,xml}`: the live font-scale change.
  - `timer/elapsed/`: the snapshot-edit working copy.

  **Home geometry (15g/15k/15p/15o):**
  - `grid/<window>-<state>.{png,xml}` (56 files): the 28 window × state captures.
  - `walk/c360-*.{png,xml}`: the 360dp Home, Challenge 111/1234 and strip checks.
  - `walk/statusbar-light-crops.png`: F-2's evidence.

  **200% (15i):**
  - `fs2/home-{c360,native}-both-{top,bottom}.{png,xml}` and `fs2/sheet-{c360,native}.png`.
  - `fs2/screens/*.{png,xml}` and `fs2/screens/sheet-{light,dark}.png`: every screen at 2.0 on 360dp, light and dark.

  **15f / F-1:**
  - `15f/coldstart.mp4` (321 KB) and `15f/r2/coldstart9s.mp4` (432 KB): the recordings.
  - `15f/f_*.png`, `15f/r2/f_*.png` and `sheet.png`: the extracted frames.

  **Tutorial:**
  - `tutorial/c360-t{1,2,3}*.{png,xml}` and `tutorial/c360-t1-caption.png`: the board-stability and caption check.
  - `tutorial/t1-{light,dark}.png`.

  **Walkthrough:**
  - `walk/native/*.{png,xml}` and `walk/native/sheet-*.png`: the native light/dark walk, including Results.
  - `walk/native/level1-*.png`: the level-1 solve.

  **Logs and Check:**
  - `logcat.txt` (500 lines) and `logcat-crash-buffer.txt` (0 B).
  - `check/gradlew-check.log`: the Check output.
  - `final-home.png`: the app left on Home.

  **Helper scripts (never committed):**
  - `tools/ink.py` (HarfBuzz ink widths), `tools/inkbox.py` (screenshot ink extents), `tools/nodes.py` (tree with dp bounds).
  - `tools/homegrid.py` (15p/15g/15k grid), `tools/timer_sweep.py` and `tools/set_elapsed.sh` (adapted from loop 2), `tools/prefs.py` (DataStore int rewrite).
  - `tools/setstate.sh`, `tools/shot.sh`, `tools/tap.py`, `tools/sheet.py`.
  - `tools/fonts/Roboto-Regular.ttf`: pulled from the device for the contrast numbers.
- `founder/01…16-*.png` (3.1 MB): the Q-013 set, indexed above.

## Open issues / risks
- **E-1, 15e is undecided:** the slice cannot close under 15e's own words until a sitting on a Pixel_9a with storage exists. The producer chooses how to free storage or whom to ask.
- **E-2, airplane-mode cold start (AC-F7 second half):** not run, because a radio toggle is prohibited for my role on the shared device. The dex scan with its positive control shows that no network path to a face exists. If the tech-lead still wants the literal airplane run, the producer can authorise it for this AVD or route it to a role allowed to toggle radios.
- **F-1** (cold-start reshuffle when the chips arrive) and **F-2** (light-theme status-bar icons) are medium. F-2 is visible in the founder's screenshots.
- O-2 (200% pill labels breaking mid-word) and O-A (the timer's spoken value) stay with the ux-designer.

## Handoff
Next: tech-lead — the merge decision on S11, with **VERDICT: PASS** on the device re-walk in the real faces: AC-F7 (on-screen), the re-walk list and the founder's screenshots (AC-F10) are done and green, and the Check is green. Two conditions sit outside this verdict and are for the tech-lead and producer: **15e is VOID (E-1)**, and the slice does not close without a decided sitting; and **AC-F7's airplane half is substituted (E-2)**. Through the producer: the ux-designer rules on F-1 (may chip arrival move Home?) and keeps O-2/O-A; the developer, through the tech-lead, fixes F-2 before the v1.1 release.

The emulator is left running: **Pixel_9a (`emulator-5554`)** with the **e1875cb debug build** (base.apk sha256 `3137f53e…a3dc`) and the app data it was found with, on Home, in dark mode.
