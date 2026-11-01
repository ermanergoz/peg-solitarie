# qa-engineer report — candy-backdrop: device rows (A10), the owed checks, TalkBack over the backdrop, one smoke pass
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; build commit 3d3879f, code = branch head 38789b6, whose only later change is a report file)
Skills loaded: ergoz-soft:standards (role qa-engineer; snapshot 0.5.1 is older than plugin 1.0.0, so I read the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing files in full, plus the product's lessons.md), android-emulator-skill, android-testing
Ran in parallel with: code-auditor loop 2
Ran on: claude-opus-5-5 (the harness's model; the agent file pins fable at high). The producer should weigh this against rules/studio.md, Model and effort.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Ran on the emulator `ergoz_studio`** (Pixel 9 profile, API 36, 1080×2424 at 420 dpi, gesture navigation). I booted it, shut it down after `sync`, and left it as I found it (diff below).
- **B was installed and confirmed on the device.** Its sha256 `0124d15c…94e4` matched in `/data/app`. A is `develop` at d251c3b, the merge-base: sha256 `e0d641b1…9257`, installed afterwards for the "≤ develop" rows.
- **The two owed checks pass.**
  - F1: Remove animations ON→OFF with the app resident and the backdrop running. It froze at scale 0 and moved again after OFF, at 307 frames in 5 s. It then settled to 0 frames.
  - B-22a: after Home's 60 s settle, neither tile's lip shows a glint. That held on two settles, one timed to begin inside a pass.
- **TalkBack passes on Home, gameplay and the tutorial.** The slab is the board stop. The backdrop has no focus stop. The spoken channel speaks over the transparent Scaffold. Results was not walked.
- **Two rows fail their literal pass line:**
  - BD-01's command 3 prints 2 lines (KDoc, not user-facing).
  - BD-09: on the four top-bar Sparse screens the status band is the backdrop's SkyTop, while `develop` paints the TopAppBar colour there. That contradicts BD-14 in the spec itself.
- **Every other device row I ran passes.** VERDICT: FAIL, on those two items only. Neither is high severity.

## Outputs
- `.planning/reports/candy-backdrop/qa-engineer-device.md`: this report.
- Artifacts, all under `.planning/qa/candy-backdrop/` (git-ignored, 97 MB, never committed). Sizes are in bytes. Light and dark are the theme; B is the branch, A is `develop`.

**BD-02 capture set (the auditor's BD-02 line reads these):**

| Path | Size | What it shows |
|---|---|---|
| `02-home-light-motion-1/-2/-3/-4.png` | 332042 / 336167 / 341004 / 338496 | Home, no chip, light, in motion, ≥ 3 s apart |
| `03-home-light-settled.png`, `03-home-light-settled-tiles-crop.png` | 330644, 98222 | Home light settled (at rest by settle); tile crop with no glint |
| `04-home-light-sheen.png`, `04-home-light-sheen-crop.png` | 342522, 103642 | Home light while the Classic tile's sheen passes |
| `11-home-light-scale0-a.png` | 343586 | Home light at rest (scale 0), one chip |
| `16-home-light-onechip.png` | 337644 | Home light, one chip (Continue Classic), in motion, glint on the Classic lip |
| `20-home-dark-motion-1/-2/-3.png` | 365055 / 364906 / 357631 | Home dark, one chip, in motion |
| `21-home-dark-sheen.png`, `-crop.png`, `21-home-dark-nosheen-ref.png` | 357973, 97602, 353447 | Home dark, sheen pass and a no-sheen reference |
| `38-home-light-360x640-fs2-motion-1/-2.png`, `39-home-dark-360x640-fs2-motion-1/-2.png` | 288036 / 294656 / 297645 / 296744 | Home at 360×640dp and font scale 2.0, light and dark |
| `06-game-english33-light-start.png`, `07-game-light-midgame-motion-1/-2/-3.png`, `07-game-light-selection.png`, `08-game-light-settled.png`, `09-game-light-scale0-a.png` | 381674, 392331 / 393270 / 391565, 387746, 395471, 395500 | Classic English-33: start, mid-game in motion, selection with a target dot, settled, at rest |
| `22-game-dark-selection-motion-1.png`, `22-game-dark-motion-2/-3.png` | 418035, 425348 / 423426 | English-33 dark, mid-game with selection, in motion |
| `14-challenge-L1-light.png`, `28-challenge-L2-dark.png` | 234990, 253939 | A Challenge level, light and dark |
| `01-tutorial1-light-B.png`, `27-tutorial1-dark-B.png` | 97123, 102604 | Tutorial step 1. The light one is the first launch's first frame: system bars black |
| `05-catalogue-light-B.png`, `10-catalogue-light-scale0.png`, `23-catalogue-dark-B.png` | 247313, 247606, 231496 | Catalogue (Sparse) |
| `15-results-light-B.png`, `29-results-dark-B.png` | 102284, 107789 | Results |
| `17-settings-light-B.png`, `24-settings-dark-B.png` | 199440, 184312 | Settings |
| `18-licenses-light-B.png`, `25-licenses-dark-B.png`, `19-licensetext-light-B.png`, `26-licensetext-dark-B.png` | 260432, 248427, 471454, 495460 | Licences and licence text (now transparent over Sparse) |

**Other rows' evidence:**

| Path | Size | What it shows |
|---|---|---|
| `12-F1-split-scale0.png`, `13-game-after-F1-fullscreen.png` | 255559, 394825 | F1 run in split screen with Settings; the game afterwards |
| `33-home-dark-batterysaver-a/-b.png` | 367912 each | Battery saver: rest pose, identical 5 s apart after a tap |
| `34-home-dark-3button.png` … `37-home-light-3button.png`, `sheet-3button.png` | 353102, 419988, 395952, 328605, 236503 | Three-button navigation scrim over the waves, both themes |
| `42-home-light-ar-RTL-motion.png`, `43-home-light-RTL-rest.png`, `44-home-light-LTR-rest.png`, `45-home-light-LTR-rest-after-toggle.png` | 343339, 346345, 347152, 346727 | RTL (`ar`) against LTR at rest, for the mirror check |
| `40-home-dark-arXB-motion.png`, `41-home-light-arXB-motion.png` | 365863, 344494 | `ar-XB` set: still LTR, because the build ships no pseudo-locales |
| `46-bd21-home-catalogue-game-home.mp4` | 10466646 | BD-21 recording, 16.2 s: Home → catalogue → game → catalogue → Home |
| `47-grey-*.png` (7 files) | 73844–154381 | BD-24 greyscale conversions of Home, gameplay and Settings, both themes |
| `A-01…A-14-*.png` | 33028–275647 | `develop` (A) captures for the BD-09, BD-13, BD-16 and TG-AC05 comparisons |
| `31-tb-game-dark-after-move.png`, `32-tb-tutorial-dark.png`, `30-tb-home-0.png`, `tb/*-order.txt`, `tb/*-logcat.txt` | 418044, 119420, 371994, — | TalkBack: focus orders and logs, including speech output |
| `burst/` (51 MB) | — | Capture bursts: the sheen search, the two B-22a settles (`burst/s1-*`), the F1 steps |
| `gfx-results.txt`, `device-state-before.txt`, `device-state-after.txt`, `uia-home-360x640-fs2-light.xml` | 1156, 577, 577, 11884 | Frame counts; device state before and after; the 360×640 at 2.0 semantics dump |
| `px.py`, `glint.py`, `settle-glint.sh`, `f1.sh`, `watch.sh`, `gfx.sh`, `edges-*.sh`, `tbnav.py`, `tbwalk.sh`, `tbtouch.sh`, `remove-anim.sh`, `sheet.py`, `a.sh`, `shot.sh` | — | Session helper scripts (never committed) |

## Checks
- `git worktree add --detach /Users/ermanergoz/Desktop/PegSolitarie-qacandy 3d3879f` + `./gradlew :app:assembleDebug` (1-min load 3.41, then 8.57 before arm A) — pass (B: sha256 `0124d15c781d5b21f476171624f88115739f77a2222606f89f7261ec51f494e4`; A at d251c3b: sha256 `e0d641b1a7713cb705aeb110a11719e9ddeaf04c5a949e9945782f958f3e9257`); worktree removed at the end (`git worktree remove --force`).
- `adb -s emulator-5554 shell sha256sum <pm path base.apk>` — pass (B on the device = `0124d15c…94e4`; later A = `e0d641b1…9257`).
- BD-01, the spec's three commands on 3d3879f: (1) `grep -rniwE --exclude=README.md 'candy|…|microsoft' app/src/main/res/values*/strings.xml app/src/main/assets docs/store` — pass (0 lines); (2) `grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store` — pass (0 lines); (3) `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` — **fail (2 lines)**, see Open issue 1.
- `adb shell dumpsys gfxinfo com.erman.pegsolitarie reset` + `sleep N` + `Total frames rendered` (`gfx.sh`) — the numbers in the table below.
- Pixel checks with `px.py` (numpy over screencaps).
  - WCAG contrast of glyph cores against the local fill. The text reading is the solid glyph colour.
  - Edges per §5.2: the stronger of fill or rim against the pixel just outside, as the minimum over 3 or 4 rows.
  - The masked status band [0, 142) px.
  - Mirrored diffs.
- **Not run:** BD-23 (b) and (c), 15e on `Pixel_9a_bench`. That is the separate later sitting, by founder directive.
- **Not run:** the full every-screen matrices (Pixel_9 profile only; no tablet), by founder directive.

### Per-row results

| Row | What I did | Result |
|---|---|---|
| **F1 (auditor, owed)** | Split screen, app (game screen, Quiet) beside Settings › Color and motion; backdrop running (motion diff 41,926 px over 1.5 s); Remove animations ON → 0 px change; OFF → 36,707 then 7,096 px changing, 307 frames/5 s; then settled to 0 frames/10 s after the 20 s game idle | **PASS** — the backdrop moves again after OFF; no frame-per-vsync leak after the settle |
| **B-22a (auditor, owed)** | Home light, idle ≥ 62 s; `settle-glint.sh` tapped at the instant a glint was on the Classic lip (7,780 px) so the next settle began at the same phase, sampled 16 frames across the settle, then rest +4 s and +14 s | **PASS** — glint present at +58–60 s (Running), then 0 differing lip px on both tiles against the settled reference through Settling and AtRest; rest frames identical 10 s apart. Second settle (`burst/settle-*`) the same |
| **BD-01 (QA greps)** | three commands above | **FAIL** (command 3: 2 lines; Open issue 1) |
| **BD-02** | capture set above, light and dark, rest/motion/sheen; my answers below | **PASS** (my half): every capture Q1 no, Q2 no |
| **BD-09** | status band [0,142) px, icon boxes masked, B vs A | B: flat `background` on every screen, light `#F4F1FA` / dark `#1B1626`, 0 off-colour px, in motion too. Against A: **identical** on gameplay; on Home B differs only where `develop`'s orb crosses the band (35,919 px in A); on catalogue, Settings, Licences and Results A paints the TopAppBar colour (light `#FBF9FF`, dark `#241E31`), so B differs by ≤ 9/255 per channel on 100% of the band | **FAIL as written** on the four top-bar Sparse screens — spec contradiction with BD-14 (Open issue 2). PASS on gameplay and tutorial |
| **BD-12** | title and body text on Home, light and dark, at rest/motion/sheen, at 1.0 (412×915) and 2.0 (360×640) | **PASS** — Home title light 8.05, dark 7.98–8.19 (worst core glyph 6.49–6.55); at 2.0 the title does not wrap at 360dp, so TG-AC04's wrapped case did not arise; game title light 11.69 / dark 14.78; timer 11.66 / 11.87 |
| **BD-13** | edges, light and dark, rest/motion/sheen; A measured at the same points | **PASS** — light (B vs A): stage 1.10–1.31 vs 1.04, gear 1.12 vs 1.05–1.10, ribbon 1.27–1.42 vs 1.05, Continue chip 1.70 vs 1.03, slab 1.20–1.22 vs 1.04–1.05, dock 2.03 vs 1.05, back chip 1.10 vs 1.05, timer pill 1.12 vs 1.05, gel tiles 2.10–3.26. Dark: Continue chip 1.42–1.46 (floor 1.20; fill alone 1.01–1.09, the rim carries it as §5.2 allows), gel tiles 1.37–2.26, stage 2.09, slab 1.89, dock 2.69, back chip 2.09, timer 3.89 |
| **BD-15 (TalkBack)** | TalkBack driven through the emulator console's touchscreen (`tbnav.py`); orders read from TalkBack's own speech log | **PASS** on Home, gameplay, tutorial (Results not walked). Home: Heading → Settings → Stars → Boards → Level → Continue Classic → Classic → Challenge (stage not a stop). Gameplay: Navigate back → title → Elapsed time → **Board, 31 pegs left** → cells → Undo → Hint → Show possible moves → Restart. Tutorial: Skip tutorial → Board → cells → instruction → Step 1 of 3. Explore-by-touch on 12 backdrop-only points (sky, waves, lights, between slab and dock): nothing focused. The channel (live region) spoke "Put down the selected peg first." and "Not a jump target." over the transparent game Scaffold |
| **BD-16 (a)** | 5 s idle at scale 0 (Accessibility › Remove animations), B vs A | **PASS** — Home 0 vs 0; gameplay 5 vs 6 (timer ticks); catalogue 0 vs 0; rest pose, no sheen (lip diff 0) |
| **BD-16 (b)** | in-motion captures ≥ 3 s apart; Sparse at scale 1 | **PASS** — Home and gameplay move (110k–236k px between frames); catalogue 0 frames/5 s; sheen period ≈ 9.8 s with the Challenge tile following the Classic one |
| **BD-16 (c)** | settled 10 s counts, B vs A; wake; battery saver | **PASS** — Home settled 0 (twice) vs A 606; gameplay (no selection, timer ticking) 10 vs 10; one tap restarts motion within the first capture after the tap (< 1 s); battery saver (Quick Settings tile) → rest pose, 0 frames/5 s even after a tap, identical frames; saver off + tap → 303 frames/5 s. Note: with a peg selected, gameplay counts 590/10 s — the legal-target dot's pulse (board, from `develop`), the backdrop region is static |
| **BD-19 (device half)** | gesture handle; three-button navigation (Settings › System › Navigation mode, restored) | **PASS** — handle vs backdrop light 4.13–4.20, dark 6.76–8.39; three-button scrim visible over the waves in both themes, icons 5.31–13.60; the dock sits above the bar |
| **BD-21** | screen recording, 30 fps frames, sky samples at y=160, 300, 700, 1150 | **PASS** — top samples constant ±1 (codec); lower samples ramp 690↔729 (sum RGB) between the Quiet/Landscape sky and the lighter Sparse wash in ≤ 12/frame steps, one 2-unit overshoot (within ±2/255 per channel) |
| **BD-22** | per-app locale `ar` (deviation: `ar-XB` is not packaged — the build has no `pseudoLocalesEnabled`, so the app stayed LTR), at rest | **PASS** at rest — RTL flipped vs LTR: 0 px differ > 2/255 below the tiles and in the stage margins (max 2); unmirrored control 72,311 and 51,219 px. Sheen travel not compared (no sheen at rest) |
| **BD-24** | greyscale (`L`) conversions, ratios recomputed | **PASS** — tile text 6.79–8.06, title 8.39–8.92, gel edges 1.49–3.68, dark chip 1.36, slab 1.18–1.94; tiles told apart by label and glyph |
| **BD-27** | gel tile text, pill text, timer, at rest/sheen/motion, 1.0 and 2.0, both themes; line box at 2.0 from the semantics dump | **PASS** — Classic 6.50/6.60, 5 boards 7.43/7.92, Challenge 5.16/5.12, Continue pill 8.87, chip title (dark) 7.79; at 2.0 on 360×640 both titles' line boxes start 48 px = **16dp** below the tile top (1177→1225, 1547→1595). Pressed state not sampled |
| **TG-AC03/04** | title glyph sampling 1.0 and 2.0 | **PASS** (as BD-12); the title does not wrap at 2.0 on 360dp, so the wrapped clause was not exercised |
| **TG-AC05** | Results "Next level" CTA interior, B vs A, dark | **PASS** — 0 px differ (control: light vs dark 89,440 px) |
| **Smoke (founder)** | light + dark Home, Game, Results, Settings, Licences (+ licence text, catalogue, tutorial) | **PASS** — no crash (no `FATAL`/`AndroidRuntime` in the session logcats), every screen rendered over its density |

### BD-02, my answers (Q1: could any element be traced to a King design? Q2: would a player think it is Candy Crush or a reskin?)
For every capture in the BD-02 set above (Home light/dark, rest/motion/sheen, no chip and one chip, 360×640 at 2.0; English-33 and Challenge L1/L2 mid-game; tutorial; catalogue; Results; Settings; Licences; licence text): **Q1 no, Q2 no.** Reason: the scene is two soft violet-to-pink bands, round blurred discs and two flat-rounded colour cards with a top lip; no candy shapes, no striped/wrapped tokens, no characters or lettering, no tile grid; the hero is the cross-shaped peg board on a flat slab, and the Sparse screens are a near-flat wash. Observation for the auditor's C1 tick: on dark Home the larger lights read as a disc with a slightly brighter rim than its centre (e.g. `20-home-dark-motion-1.png`, left edge at y≈650 px); I saw no stroke or dot.

### Logcat excerpt (TalkBack's speech log, `tb/game-dark-logcat.txt`, `tb/game-dark-move2-logcat.txt`; 8 lines)
```
talkback: GESTURE_SWIPE_RIGHT … content="Elapsed time, 2 minutes" … ACTION_ACCESSIBILITY_FOCUS
viewAccessibilityFocused: ttsOutput={Board, 31 pegs left}
viewAccessibilityFocused: ttsOutput={Row 1, column 3, peg, no jumps}
viewAccessibilityFocused: ttsOutput={Row 5, column 4, peg, 1 jump}
windowContentChanged: ttsOutput= {Put down the selected peg first.} … nodeLiveRegion=1
viewAccessibilityFocused: ttsOutput={Row 3, column 4, empty}
windowContentChanged: (176068) , ttsOutput= {Not a jump target.}, … nodeLiveRegion=1
tbtouch: game-dark touch(540,450) -> <nothing focused>   (backdrop sky above the slab)
```

## Open issues / risks
1. **BD-01 command 3 prints 2 lines (FAIL; low severity).** The two lines, from `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'`:
   - `+ * colour, so the backdrop's wash shows through it, as every top bar does (candy-backdrop BD-14, E7).`
     This is `ui/licenses/LicensesTopBar.kt`'s KDoc, from d0a65ae.
   - `+ *   \`BackdropTestHost\` at Results' density, Sparse (candy-backdrop §12), and 'beneath' is read from`
     This is `test/.../ResultsStarPaletteTest.kt`'s KDoc, from bd8abf5.
   - Both cite the spec's file slug. Neither is user-facing, and A8 C8's own clause names only "jelly" for identifiers and KDoc. The pass line is still "zero lines".
   - Route: either the developer rewords the two KDoc references (for example "the backdrop spec"), or the ux-designer narrows command 3's scope. The choice is the producer's.
2. **BD-09 contradicts BD-14 (FAIL as written; escalation: a spec contradiction).** BD-09 asks for the status band to be "pixel-identical to today" on every Sparse screen.
   - On `develop`, the TopAppBar of the catalogue, Settings, Licences (and licence text) and Results extends under the status bar. It paints the band in its own colour: light `#FBF9FF`, dark `#241E31`.
   - BD-14 makes those TopAppBars transparent, so on B the band shows SkyTop = `background`: light `#F4F1FA`, dark `#1B1626`. The band is flat, identical across screens, and steady in motion.
   - The difference is ≤ 9/255 per channel, but it covers the whole band, so no ±1 tolerance covers it.
   - Home differs from `develop` only where `develop`'s orb crossed the band, which the spec's "now that the orbs are gone" seems to intend.
   - Route: ux-designer, via the producer, to restate BD-09's comparison for the top-bar screens (for example, "equals `background`", which is the test half's form and holds everywhere).
3. **Deviations from the spec's QA matrix, by founder directive or because of the tooling:**
   - The device was the emulator `ergoz_studio` with the Pixel 9 profile, not a Pixel_9a.
   - There was no tablet.
   - BD-02 has no "both chips" Home capture: a saved Challenge chip never arose.
   - `ar` stood in for `ar-XB`, because the build has no pseudo-locales. If ar-XB is wanted, the build needs `pseudoLocalesEnabled` on debug.
   - Not sampled: BD-27's pressed state; gameplay at 2.0 and at 360×640; the wrapped title at 2.0 (it does not wrap at 360dp on this font); the Results TalkBack walk.
4. **A first-launch frame with black system bars.** `01-tutorial1-light-B.png` is the very first frame after install: status and navigation bars black, before the consent dialog. I did not capture A's first launch, so I cannot say whether this is new. It is informative only.
5. **The model.** The harness ran this QA session on claude-opus-5-5. The agent file pins fable, and rules/studio.md says a review that cannot get fable does not happen. The producer should decide whether this device session stands.

**System changes on the shared device, all restored:**
- dark mode on and off;
- Remove animations on and off, through Accessibility only;
- TalkBack on and off, through Settings;
- battery saver on and off, through the Quick Settings tile. Its first-run "Welcome to Battery Saver" choice, Standard, was made once and persists;
- the navigation mode, three-button and back to gesture;
- `wm size 1080x1920` + `wm density 480`, then reset;
- `settings put system font_scale 2.0`, then back to 1.0;
- the app's per-app locale, set and cleared;
- the app, installed and then uninstalled.

`device-state-before.txt` equals `device-state-after.txt`. The Settings app crashed once, opening the Battery Saver page by intent: a system app, not ours. The emulator got `sync` and then `emu kill`. `Pixel_9a_bench`, `Pixel_9a` and `Pixel_9` were not touched.

## Handoff
Next: producer —
1. **code-auditor:** its BD-02 line, on the captures listed under "BD-02 capture set". That means every Home, gameplay, Challenge, tutorial, catalogue, Results, Settings, Licences and licence-text PNG numbered 01–29 and 38–39, plus `04-…-sheen*` and `21-…-sheen*` for the sheen. The paths are relative to `.planning/qa/candy-backdrop/`. The C1 observation in "BD-02, my answers" is for its tick.
2. **android-game-dev or ux-designer:** Open issue 1 (BD-01 command 3).
3. **ux-designer:** Open issue 2 (BD-09 against BD-14, a spec contradiction). After that ruling, I re-run only BD-09's comparison, on the screens it changes.
4. **tech-lead:** this report for the sign-off. The 15e sitting on `Pixel_9a_bench` (BD-23 (b) and (c)) is still owed, as a separate dispatch.

VERDICT: FAIL
