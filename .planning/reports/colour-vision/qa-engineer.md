# qa-engineer report — colour-vision (device pass, brief §7.3, run 2 on ergoz_studio)
Date: 2026-10-31 (session started 2026-10-24, paused by the usage limit, resumed after a cold re-boot)
Branch: feature/colour-vision (report only; head a197316, code 0f1f58f: `git diff --stat 0f1f58f HEAD -- . ':!.planning' ':!docs'` is empty, checked at both starts)
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing in full. logging, puzzle-design and play-store are identical to the snapshot already loaded. I also read `.claude/rules/lessons.md`. Then android-emulator-skill and android-testing.
Ran in parallel with: licences-screen QA re-run (Pixel_9_clean), home-chip-arrival code-auditor loop 2, talkback-board tech-lead and ux rulings, play-upload developer
Ran on: claude-opus-5-5 at effort high. This deviates from my pin, fable at high: the Fable limit is reached, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The code under test was also written on Opus, so every verdict below rests on a device capture or a number read from one.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
I installed and drove the branch APK on AVD `ergoz_studio`. **Deviation:** brief §7.3 names the Pixel_9a or the Pixel_9. The producer chose option (c) after blocker B-1 (`qa-engineer-blocked.md`). `ergoz_studio` has the Pixel_9a's screen (1080×2424 at 420 dpi) on `android-36` `google_apis_playstore`, not the 9a's 16 KB image.
- Every item I ran passes, and no product defect turned up. The off-palette screens match the base `d21ad07` pixel for pixel, except where the spec allows it. The palette-on board, stars and pips read §3.5's figures exactly.
- Three items have gaps:
  - CV-AC14 was not run.
  - CV-AC11 could not be judged by eye. Neither screen-capture path records the simulation, so the spec's fallback, CV-AC12's numbers, carries it.
  - CV-AC10's TalkBack evidence covers the palette-on side only.
- Results with two of three stars cannot be reached in play: Results only follows a win, and every win earns three stars.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/qa-engineer.md`: this report. Not committed; the producer commits it.
- The artifacts are all under `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/qa/colour-vision/run2/` (git-ignored, never committed; 110 MB with the two APKs one level up). Captures are full-resolution screencaps in `caps/`, with 700 px previews in `caps/s/`.

| Path | Size | What it shows |
|---|---|---|
| `caps/A-base-{light,dark}-*.png` | ~42 MB with all of `caps/` | Base `d21ad07`, palette absent: tutorial steps 1–3, each with a selection, and completion; Home; Settings; the catalogue before and after a win; English-33 with a selection and Show-moves; English-33 with a hint; Results; Wiegleb-45 at 360×640dp with a selection and with a hint |
| `caps/B-cv-{light,dark}-*.png` | (in `caps/`) | The same scenario on the branch with the palette **off** (fresh install) |
| `caps/A2-base-light-tut*.png` | (in `caps/`) | The base's light tutorial re-captured with the same script as the branch's |
| `caps/C-on-{light,dark}-*.png` | e.g. `C-on-light-e33-sel.png` 262,979 B; `C-on-light-w45-hint.png` 214,243 B | The same scenario on the branch with the palette **on** |
| `montage-on.png` | 159,230 B | Crops of the palette-on marks, light row and dark row: halo and ring, opaque target dot, dashed hint rings at 33 and 45 holes, Results stars, catalogue pips, the done pip |
| `diff/*.png` | 1.4 MB | `compare.py`'s red overlays of every base-vs-branch difference |
| `caps/D-live-1-on.png`, `D-live-2-off.png`, `D-relaunch-on.png` | 278,705 / 278,144 / 278,239 B | CV-AC03's live switch, and CV-AC02 after a cold start |
| `caps/E-scale{0,0b,1,1b,1c}-f{1..4}.png` | (in `caps/`) | CV-AC09: four frames 0.2 s apart at animator scale 0 and at scale 1 |
| `caps/F-light-none*.png`, `F-light-mono.png`, `F-light-mono-console.png` | 282,547 / 400,950 B (mono) | CV-AC11: captures with Simulate color space off and at Monochromacy, through both `screencap` and the emulator console |
| `caps/G-200-{light,dark}.png` | 186,390 / 179,397 B | The Settings row at font scale 2.0 |
| `cv12-out.txt`, `cv13-out.txt`, `cv13ab-out.txt`, `live-out.txt` | 8,376 / 1,160 / ~1.5 K / 672 B | The numeric results quoted below |
| `tb-catalog-on.txt`, `tb-game-on.txt` | 299 / 188 B | TalkBack speech, read from its own `Speaking fragment` log lines |
| `logcat-cv.txt` | 67,400 B | The branch app's last 500 log lines (`--pid`). No FATAL and no AndroidRuntime error: 17 `E` lines, all environment noise (MediaCodec, MESA, WebView seed, a hidden-API notice) |
| `state-before.txt`, `state-reboot.txt`, `state-after.txt` | 734 / 762 / 725 B | The AVD's settings at the first boot, at the re-boot and at the end |
| `*.py`, `*.sh`, `*.moves` | 0.2–5.6 KB each | Session helpers, never committed: `shot.sh`, `ui.py`, `st.py`, `etap.sh`, `eswipe.sh`, `tb.sh`, `tbwalk.sh`, `tut.sh`, `e33.sh`, `e33full.sh`, `w45.sh`, `play.py`, `stuck.py`, `compare.py`, `cv12.py`, `motion.sh`, `sim.sh`, `font.py`, `state.sh`, plus the earlier `robot.py`, `deadend.py` and `prefs.py` |

## Per-item table
| Item | Result | Evidence |
|---|---|---|
| **CV-AC01:** the row, TalkBack, a silent toggle, 200 % in light and dark | **pass** | Dump order: How to play, Sound, Haptics, **Colorblind palette** / "Clearer moves, hints and stars", Play Games, …, Privacy last. TalkBack on the switch said `On, Colorblind palette, Switch`, and after a double-tap `Off, Colorblind palette, Switch`. The Haptics peer reads `On, Haptics, Switch`, and its label is a separate stop exactly as ours is. Flipping the switch added no audio start event (`dumpsys audio`), while the Sound toggle used as a control did (23:07:21). At font scale 2.0 the supporting line wraps onto two lines without truncation in both themes (`G-200-*.png`; label bounds 1866–1977, supporting line 1988–2184) |
| **CV-AC02:** off when fresh, the key, surviving process death | **pass** | After a fresh install the store has no `colorblind_palette_on` key. Flipped on, it reads `…colorblind_palette_on\x12\x02\x08\x01` (true); flipped off, `\x08\x00`. After `am force-stop` and a cold start (PID 10004 → 18945), the key is still true and the board draws the opaque target dot `(124,58,237)` (`D-relaunch-on.png`). **Not run:** "Use their cloud progress", which needs two Play Games profiles; the AVD has no Google account |
| **CV-AC03:** live switch with a game saved, and with a game open | **pass** | English-33 saved with a selection; flipped off in Settings; Continue Classic. The target dot went from opaque `(124,58,237)` to translucent `(188,158,241)` with no restart (PID 10004 throughout; `live-out.txt`). Off→on live: the switch was flipped in Settings and the next board drew the colour-blind marks in the same process (`C-on-*`). Gameplay has no Settings entry, so "a game open" means the saved game resumed in the same process |
| **CV-AC05:** off-palette comparison against `d21ad07` | **pass**, with the spec's exceptions only | `compare.py`, below the status bar. **Identical:** both themes of e33 with a selection, e33 with a hint (light differs only in the timer digits at x965–1010), w45 with a selection, w45 with a hint, Home (`home2`), and dark tutorial steps 1–3, selections and completion; light tutorial at rest after re-capture with one script (`A2-base`). **Different, as allowed:** the catalogue, only inside the 48×46 px boxes of the **unearned** pips (12 boxes; no earned pip differs), which are CV-13a and CV-13b. The Results stars are identical; the stat text differs (score and time) and a "New best!" badge moves the layout 69 px, and the dark star row aligned by that offset has 0 differing pixels. Settings gains its row. The light tutorial's selection frames differ only at the selected peg, because the branch frames were taken at animator scale 0 and the base re-capture at scale 1 (the pulse); at equal scale (dark) they are identical. Palette on, the board draws §3.4 (`montage-on.png`). Earned stars: centre = fill; unearned: centre = card (`cv13ab-out.txt`) |
| **CV-AC09:** animator scale 0 and 1 | **pass** | Scale 0, with a selection and a hint showing (2,085 amber px, 1,471 purple px): four frames 0.2 s apart change **0 px**. Scale 1, after a cold start: the selection box changes 3,849–9,653 px between frames (the pulse) and **both hint-ring boxes change 0 px**. Note, not this branch's doing: a scale change reached the pulse only after a cold start. Arm A has the same gate, and develop's `bugfix/reduced-motion-live-gate` addresses it |
| **CV-AC10:** TalkBack traversal, keys and descriptions | **pass, device half partial** | Palette on, TalkBack walk: catalogue "Navigate back, Button / Classic / 3 of 3 stars, English-33 / 0 of 3 stars …"; gameplay "Navigate back / Peg Solitaire, Heading / Undo, Button, disabled / Hint / On, Show possible moves / Restart Board / Elapsed time" (`tb-*.txt`). The star descriptions are unchanged ("0 of 3 stars", "3 of 3 stars", the same strings as the base's dump). I did not walk the tutorial and Results under TalkBack, or the palette-off side, within the session's budget. The 21a–21g dock looks the same in both palettes in every gameplay capture |
| **CV-AC11:** simulated colour spaces | **pass by the spec's fallback; not judged by eye** | Simulate color space set to Monochromacy through Developer options (secure `daltonizer_enabled=1`, `daltonizer=0`). **Neither `screencap` nor the emulator console's screenshot shows the simulation:** mean board chroma is 42.7 in all four captures, with it off and at Monochromacy. I cannot see the panel directly, so per the criterion the numbers are CV-AC12's, whose severity-1.0 dichromat matrices are stronger than the device's anomalous modes. Every distinction on the plain captures is a form cue: halo + solid ring, opaque disc, dashed rings (8 dashes, gaps verified at −101.25°), filled against hollow. The off-palette baseline is recorded in `B-cv-*` |
| **CV-AC11, graded item:** standard palette, normal vision, earned filled and unearned hollow | **pass on the catalogue card, light and dark; Results not reachable** | Branch catalogue: earned pip centre `(249,194,74)` light / `(253,217,116)` dark (filled); unearned pip centre = card `(241,234,251)` / `(46,39,64)` (hollow), band `#635F69` / `#A79FB5`. The base drew the unearned pip filled (centre `(220,211,236)`, no band). **Results never shows an unearned star in play:** `GameViewModel.statusFor` makes Won ⇔ pegs ≤ `minimumPegs`, which is exactly `ScoreCalculator`'s 3-star band, and only Won navigates to Results. The device's Results is 3/3 earned in both themes; the unearned Results star is proven only by `ResultsStarPaletteTest` |
| **CV-AC12:** offline matrices on plain captures, with the 16 dp pip note | **pass** | `cv12.py` (normal / protan / deutan / tritan / mono). Light: row 1 4.86/4.17/4.51/4.53/4.86; row 2 5.54/4.76/5.14/5.16/5.54; row 4 = row 1; row 5 (both rings) 4.28/5.25/3.82/4.33/4.28 — all equal to §3.5. Dark: row 1 5.21/5.32/5.17/5.34/5.21; row 2 6.85/7.01/6.79/7.05/6.85; row 5 8.50/7.68/9.01/7.83/8.50 — equal to §3.5. Results 48dp, light: fill vs background 4.20 min 3.74 (≥ 3.22); outline vs background 6.93, min 6.64 (≥ 6.02). Against the decorative glow disc, fill ≥ 3.03 and outline ≥ 5.38, recorded. Dark: fill ≥ 12.37, outline ≥ 15.04. Catalogue pips, light: earned fill vs card ≥ 3.61; **the 16dp unearned band reaches full `#635F69` at its centre pixel, 5.31** (dark `#A79FB5`, ≥ 5.56), so the 16dp allowance was not needed. Done pip: light ≥ 3.81, dark ≥ 11.81. Every "≥ 3:1" row ≥ 3:1 |
| **CV-AC13:** dashes at Wiegleb-45, 360×640 | **pass, on plain captures in luminance** | Walking each ring's stroke circle in 1° steps: source ring 8 dashes (light, r≈42 px; dark 8), target ring 8 dashes (light; dark 8 at r=27–29 px); on-fraction 0.48–0.51. Selection, light, luminance outward: peg 0.31 → halo **0.971** (light inner band) → ring 0.134 → slab 0.845. In dark the halo is the dark `#15101F` (L 0.006) by §3.4 (`cv13-out.txt`). The device's own Monochromacy is not in captures (CV-AC11) |
| **CV-AC14:** system colour correction | **not run** | Out of the session's budget. Non-blocking by the spec; for the producer to route |
| **CV-13a / CV-13b on the device** | **pass** | Standard unearned star: hollow (centre = surface beneath), drawn in `onSurfaceVariant` `#635F69` / `#A79FB5`, at 5.31 / 5.56–5.60 against the card. The earned star is unchanged gold and pixel-identical to the base (the catalogue's earned pips and the Results row) |
| 15e sitting | out of scope | per the dispatch |

## Bugs
No product defect was found.

**Spec note SN-1 (for the ux-designer through the producer; not a bug).** Two scenarios are written but cannot be reached in play:
- CV-AC11's "Results with two of three stars";
- every unearned star on Results.

Results is reached only through `GameStatus.Won`, and Won (`pegCount ≤ minimumPegs`) is exactly the 3-star band. A stuck board shows the rescue banner, not Results.
- **Steps:** finish English-33 stuck with 2 pegs (`e33-2peg.moves`).
- **Expected (spec):** Results with 2 of 3 stars.
- **Actual:** the rescue banner "No moves left — undo or restart" (`caps/x3.png`).

The unearned Results star is proven only on the JVM.

**Session incident (environment, recovered, not product).** My Settings helper tapped "Animator duration scale" at y = 541, under Developer options' sticky "Use developer options" bar. That turned developer options off, which set `adb_enabled=0`, and `emulator-5620` went offline.
- I recovered through the emulator console: mouse events turned developer options and USB debugging back on.
- The helper now ignores anything above y = 620.
- A palette-on run also went astray once (the tutorial missed a move, and the scripted taps landed in Settings and opened the ad-consent form). I discarded it, answered the form "Do not consent", and re-ran with screen guards.

## Checks
- `git diff --stat 0f1f58f HEAD -- . ':!.planning' ':!docs'`: **pass**, empty on 2026-10-24 and 2026-10-31.
- `shasum -a 256` of the two APKs: **pass**. `cv-0f1f58f-debug.apk` is `5e694cbd…81db` and `base-d21ad07-debug.apk` is `35effc51…a3d`, as recorded. The installed `base.apk` read back from the device gave `5e694cbd…81db`, at install and again after the re-boot.
- The Check: **not run.** I cite the code-auditor's forced Check at 216c6c3 (code = 0f1f58f), which is green: 1,265 + 296 tests, `CHECK-EXIT=0` (`code-auditor.md`).
- `emulator -avd ergoz_studio -port 5620 -no-snapshot-load -no-snapshot-save -no-boot-anim`: **pass** on both boots (`adb emu avd name` printed `ergoz_studio`).
- The re-boot on 2026-10-31: **pass.** `diff state-before.txt state-reboot.txt` shows only my own changes: developer options on, and my branch app installed.
- `adb -s emulator-5620 install -r -d --no-incremental …`: **pass**, three installs (base, branch, base). No other serial was touched.
- `/usr/bin/python3 compare.py`, `cv12.py` and the inline scripts: results as quoted above.
- Restore: **pass, with three differences recorded below.**
  - Night mode `no`, font scale 1.0 and all three animation scales 1.0 were set back through the Settings UI; `wm size` and `wm density` were reset; TalkBack is off (`enabled_accessibility_services` empty, `accessibility_enabled=0`).
  - `com.erman.pegsolitarie` was uninstalled, so the third-party package list is back to `com.topjohnwu.magisk` only.
- Shutdown: `emulator_manage.py --shutdown emulator-5620`: **pass.** Afterwards no `ergoz_studio` qemu process is left. `emulator-5640`, another session's emulator, was left alone.

**System changes left on the AVD** (`diff state-before.txt state-after.txt`):
1. `global development_settings_enabled` is `1` (it was `null`). I left it on deliberately: turning developer options off through the UI sets `adb_enabled=0`, as this session saw, and the next session would then find the AVD unreachable by adb. If the founder wants it off, it is one toggle at the top of Developer options, and USB debugging goes off with it.
2. `secure accessibility_display_daltonizer_enabled` and `accessibility_display_daltonizer` are both `0` (they were `null`). Simulate color space is "Disabled" in the UI; the effect is the same as unset.
3. `/data` usage is 28 % (it was 26 %): system caches, with nothing of mine installed.

## Open issues / risks
1. **CV-AC14 was not run.** It does not block the merge (spec). For the producer: a short follow-up session or a waiver.
2. **CV-AC10's device half is partial.** I walked the catalogue and gameplay under TalkBack with the palette on, but not the tutorial or Results, and not the palette-off side. The JVM half (`ColorblindPaletteLiveSwitchTest`: keys unchanged across a flip) and the unchanged description strings support it. The tech-lead decides whether that suffices.
3. **CV-AC11 cannot be judged by eye by an agent** on this emulator: both capture paths record the unsimulated frame. The spec's fallback, CV-AC12, is met exactly. A human glance at the panel would close it.
4. **SN-1:** the unreachable two-star Results scenario, for the ux-designer.
5. **The AVD deviation** from brief §7.3: `ergoz_studio` instead of the Pixel_9a or the Pixel_9. It has the same 1080×2424 at 420 dpi screen, so the pixel geometry, including the 16dp pip's ~3 px band, matches the spec's Pixel_9a assumptions.
6. **The review-model deviation** (Q-019): code and QA both ran on Opus.

## Handoff
Next: tech-lead — sign-off input. All tested CV-AC items pass on device artifacts. CV-AC14 is not run, CV-AC10's device half is partial, and CV-AC11 rests on CV-AC12 by the spec's own fallback. Then the 15e sitting on `Pixel_9a_bench`, last, as brief §9 rules.
Next: producer — route SN-1 to the ux-designer (non-blocking), decide on CV-AC14, and note the developer-options state on `ergoz_studio`.

VERDICT: PASS (no product defect; CV-AC14 not run and non-blocking per spec; CV-AC10's device half partial; CV-AC11 judged through CV-AC12 as the criterion allows)
