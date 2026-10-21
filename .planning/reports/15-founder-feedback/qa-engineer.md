# qa-engineer report — S11 founder-feedback slice (D-03l Home stage, D-05d status strip, D-04e dock), hands-on
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (bffe558..d017711)
Skills loaded: ergoz-soft:standards (role qa-engineer; rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing — plugin copies read where the 0.5.1 snapshot is older than plugin 0.6.2), android-emulator-skill, android-testing
Ran in parallel with: code-auditor review of the same slice (JVM-only, same worktree; its report not read)
Ran on: Opus (claude-opus-5-5), agent-file effort xhigh — **staffing deviation**: the qa-engineer is pinned to fable; Fable was exhausted (HTTP 429, req_011CfWkXgc4wbcMaK2GwpDSB), so this ran on Opus under the founder's Q-003 answer (`.planning/questions/FOUNDER.md`). The author also ran on Opus, so independence rests on this being a fresh agent that saw only `docs/ux-spec.md`, the diff and the build.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot carries 0 active lessons)

VERDICT: FAIL

## Summary
Hands-on on emulator-5560 (AVD Pixel_9, API 36) with this branch's HEAD d017711 installed, plus a 360×640dp window via `wm size`/`wm density`. Home stage, title row, dock state language, TalkBack switch semantics, contrast, greyscale reading, insets on every screen and the full gameplay, tutorial and win flow all pass on the device.
**FAIL on 18d.** The shipped timer typeface, Fredoka Medium from Google Fonts, has no `tnum` feature and proportional digits. Wherever it resolves, the timer pill's width swings by up to 18.4dp (the limit is ±0.5dp). On this emulator Fredoka never resolves, so the device reading (identical widths) measured the Roboto fallback.
**15e: VOID.** Preconditions 1, 3, 4 and 7 failed, so the sitting cannot decide the gate; the numbers below are not a verdict.
15p part 1 has a spec ambiguity that I escalate (E-1). It is not counted as a FAIL.

## Device and setup
- Device: `emulator-5560`, AVD `Pixel_9`, `ro.build.version.sdk` 36, 1080×2424 at 420dpi (411.4×923.4dp). Real insets: status bar 142px = **54.1dp** (camera cutout), gestural navigation bar 63px = **24.0dp**.
- Compact window: `wm size 1080x1920` + `wm density 480` = 360×640dp. Real insets there: status bar 156px = **52dp**, navigation bar 72px = **24dp**, so **564dp** usable (the spec's design baseline assumes 24 + 48 = 568dp).
- Install: `ANDROID_SERIAL=emulator-5560 ./gradlew :app:installDebug` at d017711. The installed APK's sha256 was re-checked at the end and matches the branch build (`1c4df6be…`).
- The founder's app data was backed up before the session and restored at the end (restored database WAL hash matches the backup). Every override was reset: 1080x2424 / 420, font scale 1.0, night mode off, all three animation scales 1.0, no accessibility service. TalkBack's "Display speech output" was turned back off and its POST_NOTIFICATIONS permission restored to `granted=false, USER_SET`. The app is left on Home.

## Criteria — verdicts
Dimensions are device dp (px ÷ 2.625 on Pixel_9, ÷ 3.0 on the compact window), read from `uiautomator` bounds or from exact-colour pixel runs on screenshots.

### D-03l, §4.2.4 — Home stage
| Criterion | Verdict | Device evidence |
|---|---|---|
| 15n no logo | **PASS** | Nothing is drawn above or beside the title in any state or window (`s11-home.png`, `46-…nochip`, `34-…bothchips`, `30-…bothchips`). `grep -rn 'HomeHeroArt' app/src/main` returns 0 hits. |
| 15o title row | **PASS** | Pixel_9: title starts at 16.0 (content start 16.0); gear ends at 395.8 (content end 395.4, Δ 0.4); vertical ranges 84.2–128.4 and 82.7–130.7 overlap; full text shown. 360dp: title starts at 8.0 = content start, gear ends at 352.0 = content end. At 200% on 360dp the title stays on one line with the gear in the row. |
| 15p no empty middle gap | **PASS, with E-1** | Part 1 (Pixel_9 ≈ the 412×915 baseline, real insets): no chip → stage art **289dp** (slab y 158.1–446.1); one Classic chip → **216dp** (158.1–374.1); both chips → 144dp. Art→ribbon 16.4, ribbon→chip/tile 16.0, chip→tile 16.0, tile→tile 16.0. Hero row→art is **≈16dp measured from the row's 72dp box**, which is how `HomeStageTest` and §4.2.4's arithmetic count it, but **27.4dp from the gear's visible bottom edge** (130.7 → 158.1) — see E-1. Part 2: Pixel_9 both chips, largest gap 27.4 ≤ 80; 360×640 both chips, largest gap 49.0 (gear 120 → ribbon 169; the stage is 21dp, so no art) ≤ 64. Part 3: one-chip art is 216dp ≥ 120dp. |
| 15q real game, static | **PASS** | Device: the stage draws 14 pegs on English-33 with no selection, dots or glow, identical across states. `HomeStagePositionTest` (3 tests) passes on a fresh rerun. |
| 15r stage is decoration | **PASS** | The stage has no node in the accessibility tree (no desc, not clickable or focusable). Tree order on both windows is title → Settings gear → Stars/Boards/Level pills → Continue chip(s) → Classic tile → Challenge tile (`s11-home.xml`, `30-…xml`, `46-…xml`). "Identical before and after the first emission" is covered by the JVM test only; the placeholder frame is too short to capture. |
| 15k compact fold (360×640dp, both chips, real insets) | **PASS** | Hero row: title 74.0–118.0, gear 72.0–120.0. Ribbon (compact pills, label and value on one line) 169.0–209.0. Chips 217.0–297.0 and 305.0–385.0. Classic tile 393.0–489.0; Challenge tile **497.0–593.0**. Viewport 52.0–616.0 (564dp). **Everything fits: overflow 0dp**; the column reports not scrollable and a swipe moved no node (`30`/`31`). Part 1 complete; part 2 trivially satisfied (no overflow); part 3 overflow 0 ≤ 120; part 4 nothing clipped, compact pills, no art. The device bands are shorter than §4.2.3's Robolectric estimates (chips 80dp against ≈108, tiles 96dp against 108/116), so the predicted 72dp overflow does not occur on this device (O-4). |
| 15e cold start (§6.5, second edition) | **VOID** | See the cold-start table below. |

### D-05d, §4.3.1 — status strip
| Criterion | Verdict | Device evidence |
|---|---|---|
| 18a sizes | **PASS** | The timer pill is 40.00dp tall at 1.0 on both windows. "Peg Solitaire" and "Level 2" render whole at 360dp and font 1.0 (`26`, `28`). "Level 1234" and the resolved text styles are covered by `GameStatusStripTest` (fresh rerun passes). |
| 18b each bar once | **PASS** | Pixel_9: back chip top at 58.3dp against the status bar bottom at 54.1dp (**4.2dp**); dock slab 811.4–883.4dp with its bottom **exactly 16dp** above the navigation bar (899.4). 360dp: back chip top 56.0 against 52.0 (**4.0dp**); dock bottom 600.0 = 616 − 16. The empty band above the strip is gone (`02`, `26`). |
| 18c board keeps its size | **PASS** | 360×640dp: board slab **328.7dp** wide at font 1.0 **and** 2.0 (px 47–1032 at 3.0; 328 plus an antialiased edge pixel). Pixel_9: 380.2dp (411.4 − 32). |
| 18d no jitter | **FAIL (F-1)** | As rendered on this emulator, the pill is **226px = 86.10dp** wide at 00:00, 08:08, 11:11 and 59:59 at font 1.0, and **302px = 115.05dp** at all four at font 2.0 (identical to the pixel). **But the rendered face is the Roboto fallback, not Fredoka** (see F-1). |
| 18e 200% | **PASS** | 360dp at 2.0: "59:59" is whole in a pill grown to **48.00dp** (111.0dp wide); only the title ellipsises ("Peg Sol…"); its accessibility text is still the full "Peg Solitaire", and the Challenge title's description reads "Challenge level 2" (`25`). Cosmetic: the digits sit about 2dp left of centre in the grown pill (ink margins 10.3 / 14.3dp), O-3. |

### D-04e, §4.4.1 — dock state language
| Criterion | Verdict | Device evidence |
|---|---|---|
| 21a looks by state | **PASS** | Undo is Unavailable on a fresh board (node `enabled=false`, no disc, faded glyph) and Available after one jump. Hint shows Working (spinner, pale disc, no shadow) while searching (`10a`), and stays Available with its allowance spent and while a revealed hint is on the board (`48`). Show-moves goes On/Off; Restart is always Available. The table-driven test passes on a fresh rerun. |
| 21b recipes differ | **PASS** | `DockLookRecipeTest` (2 tests) passes on a fresh rerun. |
| 21c spent allowance unchanged | **PASS** | After spending all 4 free undos, the dock crop is **byte-identical** to before (ImageChops diff bbox `None`) and the nodes and flags are identical; tapping Undo raises "Out of free undos". Hint with 0 free hints (saved game) keeps the Available look and node; the tap goes Working, then "Out of free hints". |
| 21d switch | **PASS** | TalkBack (Tab traversal plus TalkBack's speech-output display): On is announced **"On", "Show possible moves", "Switch"**; after toggling, **"Off", "Show possible moves", "Switch"** (this TalkBack reads state, name, type). The tree shows `checkable`, `checked` flipping, and the constant name. `grep -rn 'show_moves_off' app/src` returns 0. `ButtonSoundTest` has no change in `bffe558..d017711`. |
| 21e only the toggle | **PASS** | Across every capture, only Show-moves ever shows the check mark, slashed glyph, strong fill or ring (`21g/21g-sheet-grey.png`, `16`, `17`); `DockOnMarkAndSlashTest` (4) passes. |
| 21f contrast | **PASS** | Light: On disc `#7c3aed` against slab `#f1eafb` **4.86:1**; Off ring `#635f69` (a solid 6px stroke) **5.31:1**. Dark: On `#a78bfa` against `#2e2740` **5.21:1**; Off ring `#a79fb5` **5.59:1**. All ≥ 3:1. |
| 21g greyscale | **PASS** | Five greyscale captures, each read without colour, all match §4.4.1. Fresh board/Off: Undo Unavailable, Hint Available, Show-moves Off (ring + slash), Restart Available. Fresh/On: the same but Show-moves On (strong fill + check). Jump/Off and Jump/On: Undo Available, the rest as above. Hint search: Hint Working (spinner arc). |

### Regressions (walked on the device)
- **Gameplay (Classic, English-33):** resume via Continue; select (ring); show-moves Off shows no dots and On shows the legal dot; jump; undo; Hint shows Working and then the offer when the allowance is spent; a free hint reveals the move (gold source and target rings); Restart asks to confirm, then gives a fresh board at 00:00 with Undo Unavailable; Back returns to Home with the save kept. The timer restarts on the first move after a resume (existing behaviour). **Win:** Challenge Level 2 solved → Results "Perfect!", 3 stars, score 5270, time and pegs left, Next level and Home; Home then shows Level 3 and the Continue Challenge chip is gone.
- **Tutorial:** auto-starts on a fresh install (the ads consent form appears over it first); all 3 steps walked to "That's the whole game. Go clear a board." → "Got it"; Skip works; Settings → How to play reopens it and returns to Settings.
- **Insets on every screen:** Home, Gameplay, Results (top bar under the 54dp status bar), Classic catalog (list bounded 118.1–899.4dp, ending at the navigation bar), Settings, Tutorial, and the restart, allowance and Play Games dialogs. No doubled padding and nothing under a system bar. Dark theme gameplay was checked as well.
- **Logs:** the crash buffer is empty for the session; the app's own log since the final launch has no `AndroidRuntime`, `FATAL`, exception or ANR lines.

## FAIL items
**F-1 — 18d: the timer's typeface has proportional digits, so the pill will change width as the time ticks (medium).**
- Steps: (1) The strip's timer is `headlineSmall.withTabularFigures()` (`GameStatusStrip.kt`, `TimerPill`), and `headlineSmall` is Fredoka Medium loaded as a downloadable Google Font (`Type.kt:25-26`, `:88`). (2) Take the Fredoka binary Google Fonts serves (`ofl/fredoka/Fredoka[wdth,wght].ttf`, OFL, saved as evidence) and instance it at wght 500. (3) Measure the digit advances and the timer strings at 24sp.
- Expected (18d, §4.3.1 "Tabular figures must actually be tabular"): pill width equal (±0.5dp) at 00:00, 08:08, 11:11 and 59:59.
- Actual: the font has **no `tnum` feature** (GSUB `aalt ccmp dlig frac liga ordn sups`; GPOS `kern mark mkmk`), so `withTabularFigures()` has nothing to switch on. Digits are proportional (units/1000: `0`=580, `1`=388, `2`=587, `5`=509, `9`=531). At 24sp the text measures **00:00 = 61.20dp, 08:08 = 59.90dp, 11:11 = 42.77dp, 59:59 = 55.44dp**, a **18.4dp** swing, and the pill (content + 2 × 12dp) swings with it on every tick that adds or removes a "1".
- Why the device reading passed anyway: on this emulator, Fredoka never resolves and the app falls back to Roboto everywhere. The strip, the timer and the Home title all render with Roboto's flat terminals (`25`, `26`, `27`), and the founder's own `home-founder.png` shows the same fallback. Roboto's digits are tabular by default, so the device measured 86.10dp at every value.
- Evidence: `.planning/qa/15-founder-feedback/18d/fredoka/digit-advances.txt`, `18d/fredoka/Fredoka[wdth,wght].ttf`, `18d/fs1.0-*.png`, `18d/fs2.0-*.png`.
- Owner: the developer (§4.3.1: "the fix — if one is needed — is the developer's"). Confirm the fix on a physical phone where the Google Fonts request resolves.

## Escalation
**E-1 — 15p part 1, spec ambiguity (to the ux-designer through the producer; not blocking this verdict).** 15p.1 bounds "the vertical distance between two consecutive visible elements — hero row, stage art, …" at ≤ 24dp. Measured from the row's 72dp box (the JVM test's reading, and §4.2.4's own "row 72" arithmetic), the hero→art gap is ≈16dp: PASS. Measured from what is actually visible in the row (the 48dp gear's bottom edge), it is **27.4dp in every chip state** on the Pixel_9. The 72dp row minimum around a 48dp gear adds 12dp by construction, so the two readings cannot both pass. On screen the band above the board is visibly deeper than the 16dp gaps below it (`s11-home.png`). The ux-designer should rule which reading governs; the implementation matches the spec as drawn.

## Cold start — criterion 15e (§6.5 second edition): VOID
Protocol run exactly as written, with the one mechanical adaptation forced by the build: both arms carry the applicationId `com.erman.pegsolitarie`, so they cannot both stay installed. Each block switch was `adb install -r -d` (data kept) followed by `cmd package compile -m speed -f`, applied identically in the null control. There were 10 blocks per phase in blocks of five (A/B/A/B), the first sample of each block discarded, and `am force-stop` plus 2s between samples. Component resolved from the device: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`. Arm A = `develop` 3f2af90, built from a clean `git archive` (`armA-3f2af90.apk`). Arm B = d017711 (`armB-d017711.apk`). Seeded ladder: 200 Challenge rows via the WAL-aware method (`SUM(stars)` 401; Home read "Stars, 401"). Sitting 03:41:19–03:46 on 2026-09-29.

| # | Precondition | Recorded value | Met? |
|---|---|---|---|
| 1 | AVD Pixel_9a, API 36, animations off | **Pixel_9** (`sdk_gphone64_arm64`), API 36; animation scales 0/0/0 | **No** — the dispatch pinned the founder-watched Pixel_9, and booting a Pixel_9a would break #2 |
| 2 | exactly one emulator | `adb devices` 1, qemu processes 1 | Yes |
| 3 | no concurrent Gradle build anywhere | `PegSolitarie-audio … :domain:depthSweep --offline --rerun` running before and after | **No** |
| 4 | 1-min load < 2.0 at both ends | before `4.73 5.28 5.55`, after `4.71 4.60 5.14` | **No** |
| 5 | ART normalised | `cmd package compile -m speed -f` after every install, both arms | Yes |
| 6 | component resolved from the device | `cmd package resolve-activity --brief` → as above | Yes |
| 7 | both arms installed for the whole sitting | impossible (same applicationId); swapped by `install -r -d` each block | **No** |
| 8 | ~200-level ladder, identical on both arms | 200 rows plus the founder's data, one shared data dir | Yes |

| Table | n (kept) | min | median | max |
|---|---|---|---|---|
| Null control, position A (branch build) | 20 | 469 | **574.5** | 807 |
| Null control, position B (branch build) | 20 | 477 | **544.0** | 790 |
| Arm A — develop | 20 | 354 | **446.5** | 591 |
| Arm B — this branch | 20 | 422 | **477.5** | 795 |

floor = |544.0 − 574.5| = **30.5ms**; threshold = max(2 × 30.5, 100) = **100ms**; median_B − median_A = **+31.0ms**. The arithmetic would read under the threshold, but four preconditions failed, so under §6.5 the sitting is **VOID: neither a pass nor a fail**, and these arm numbers are not evidence. Raw samples are in `coldstart/sitting.csv` and preconditions in `coldstart/preconditions.txt`. S11 changes Home's first frame (§6.5, "any later branch that changes Home's first frame"), so a decided sitting is still owed: Pixel_9a on a quiet host, with the founder's Pixel_9 shut down and no build in any worktree. That needs the producer to schedule it.

## Test review (step 4)
Each new criterion has a named test, and all pass on a fresh rerun: 15n/15o `HomeHeroRowTest` (5); 15p/15r `HomeStageTest` (10, insets supplied, including the six-window grid); 15q `HomeStagePositionTest` (3); 18a/18b/18c/18e `GameStatusStripTest` (6); 21a `DockStateLanguageTest`; 21b `DockLookRecipeTest` (2); 21c `DockSpentAllowanceTest` (2); 21d `GameScreenChromeTest.showMovesCarriesItsStateByFillRingMarkAndSwitchState…` (asserts `Role.Switch`, `ToggleableState` and the constant name); 21e `DockOnMarkAndSlashTest` (4). No test gaps against the JVM halves of the criteria.
Two limits worth stating:
- 18a asserts that the style *requests* `tnum`. That is exactly what the spec asks of 18a; the property belongs to 18d, and Robolectric cannot catch F-1.
- `HomeStageTest` measures the hero row by its 72dp box (E-1).

## Checks
- `ANDROID_SERIAL=emulator-5560 ./gradlew :app:installDebug` (worktree, d017711) — pass (Installed on 1 device; packageDebug up to date).
- `./gradlew check --offline` in a clean `git archive d017711` export plus `local.properties` (no gsd gate record exists for this slice, and the export keeps it clear of the auditor's work in the worktree) — **pass**: `BUILD SUCCESSFUL in 11m 10s`, 98 tasks (38 executed, 60 from cache). detekt, ktlint and lint pass; app **1,044** tests and domain **275**, 0 failures, 0 errors, 0 skipped. `:app:testDebugUnitTest` and `:domain:test` were build-cache hits; the slice touches no file under `domain/`.
- `./gradlew :app:testDebugUnitTest --offline --rerun --tests '*.DockLookRecipeTest' … '*.HomeStageTest'` (the slice's 10 test classes, forced to execute) — **pass**: 51 tests, 0 failures, 0 skipped, `BUILD SUCCESSFUL in 11s`.
- `.planning/qa/15-founder-feedback/tools/coldstart.sh` (§6.5 protocol) — **VOID** (see above).
- `grep -rn 'HomeHeroArt' app/src/main` — 0. `grep -rn 'show_moves_off' app/src` — 0. `git log bffe558..d017711 -- '**/ButtonSoundTest.kt'` — empty.
- `adb logcat -d -b crash` — empty. Log monitor on the app for 20s on Home — warnings and emulator codec noise only.

Log excerpt (`logcat-home-20s.txt`, app pid, final launch on Home; everything at W/E is platform noise, with no app exception):
```
09-29 03:48:23.902 10300 10316 E ashmem  : Pinning is deprecated since Android Q. Please use trim or other methods.
09-29 03:48:24.029 10300 10326 W HWUI    : Failed to choose config with EGL_SWAP_BEHAVIOR_PRESERVED, retrying without...
09-29 03:48:24.030 10300 10326 W HWUI    : Failed to initialize 101010-2 format, error = EGL_SUCCESS
09-29 03:48:24.086 10300 10300 W HWUI    : Unknown dataspace 0
09-29 03:48:24.103 10300 10324 E MediaCodec: Media Quality Service not found.
09-29 03:48:24.104 10300 10329 W Codec2Client: query -- param skipped: index = 1107298332.
09-29 03:48:24.104 10300 10329 E an.pegsolitarie: Failed to query component interface for required system resources: 6
09-29 03:48:24.104 10300 10325 E MediaCodec: Media Quality Service not found.
```
Font evidence (`18d/fredoka/digit-advances.txt`):
```
GSUB features: ['aalt', 'ccmp', 'dlig', 'frac', 'liga', 'ordn', 'sups']
tnum present: False
advances: {'0': 580, '1': 388, '2': 587, '3': 580, '4': 547, '5': 509, '6': 531, '7': 543, '8': 553, '9': 531, ':': 230}
00:00: 2550 units = 61.20 dp at 24sp (headlineSmall)
11:11: 1782 units = 42.77 dp at 24sp (headlineSmall)
```

## Outputs
- `.planning/reports/15-founder-feedback/qa-engineer.md` — this report (the only committed file).

Artifacts (git-ignored, all under `.planning/qa/15-founder-feedback/`; `home-founder.png` and `gameplay.png` there are the founder's and are not mine):
- **Founder-facing screenshots:**
  - `s11-home.png` (192 KB) — Home at Pixel_9 default in the founder's restored state (one Classic chip): title row with gear, 216dp stage board, ribbon, chip, tiles.
  - `s11-home-compact.png` (167 KB) — 360×640dp with both Continue chips; everything fits (seeded Stars 401).
  - `s11-game-showmoves-off.png` (255 KB) — gameplay, Show-moves Off (ring + slashed glyph); Undo, Hint and Restart as pale raised discs.
  - `s11-game-showmoves-on.png` (255 KB) — Show-moves On (strong fill + check mark on the rim).
  - `s11-game-fontscale-2.png` (272 KB) — Pixel_9 at font 2.0, 59:59 in the grown 48dp pill, title whole.
- **Home:**
  - `00-launch-existing-data.png`/`.xml` (192/13 KB) — one-chip Home before the session plus its tree.
  - `01-home-seeded-oneclassicchip.png` (191 KB) — the same state after seeding.
  - `46-home-pixel9-nochip.png`/`.xml` (189/12 KB) — fresh-install Home, 289dp stage.
  - `34-home-pixel9-bothchips.png`/`.xml` (190/14 KB) — both chips, 144dp stage.
  - `30-home-360x640-bothchips.png`/`.xml` (167/14 KB) — 15k.
  - `31-…-after-swipe.png` (166 KB) — nothing scrolled.
  - `27-home-360x640-after-relaunch.png` (168 KB) — Roboto fallback title.
  - `32-…fs2-top.png` / `33-…fs2-bottom.png` (177/159 KB) — compact Home at 200%, scrolls, mid-word label wrap (O-2).
  - `47-home-first-run-pgs-prompt.png` (175 KB) — first-run sign-in prompt.
  - `s11-home.xml` (13 KB) — tree of the final Home.
- **Gameplay:**
  - `02-game-classic-resumed.png`/`.xml` (255/13 KB) — resumed Classic, strip and dock.
  - `03-game-showmoves-on.xml` (13 KB) — tree with `checked`.
  - `04-select-showmoves-off.png`, `05-select-showmoves-on.png`, `06-after-jump.png`, `07-after-undo.png` (259/259/250/256 KB) — select, dots, jump, undo.
  - `08-undo-allowance-spent-dock.png` (251 KB) — the dock after 4 undos.
  - `09-undo-tap-with-allowance-spent.png` (119 KB) — "Out of free undos".
  - `10a/10b/10c-hint-tap-*.png` (252/119/119 KB) — Hint Working, then "Out of free hints".
  - `11-restart-confirm.png` (166 KB) — the restart dialog.
  - `12-fresh-board-showmoves-on.png`/`12-fresh-board.xml`, `13-fresh-board-showmoves-off.png`, `14-onejump-showmoves-off.png`, `15-onejump-showmoves-on.png` (263–266 KB each) — the 21g source captures.
  - `16-dark-game-showmoves-on.png`, `17-dark-game-showmoves-off.png` (265/263 KB) — dark theme, 21f.
  - `25-game-360x640-fs2.png`/`.xml` (266/13 KB) — 18e.
  - `26-game-360x640-fs1.png`/`.xml` (259/13 KB) — 18b/18c at 360dp.
  - `28-challenge-level-360.png`/`.xml`, `29-challenge-one-jump.png`, `35-challenge-resumed.png` (114–115 KB) — Challenge Level 2.
  - `36-win-moment.png`, `37-results.png`/`.xml` (69/67/14 KB) — the win and Results.
  - `48-hint-revealed-challenge.png` (122 KB) — a free hint revealed, Hint still Available.
- **Other screens:**
  - `38-classic-catalog.png`/`.xml` (182/16 KB), `39-settings.png`/`.xml` (152/16 KB), `40-settings-bottom.png` (152 KB) — insets.
  - `41-tutorial-from-settings.png`/`.xml`, `42-tutorial-step2.png`, `43-tutorial-step3.png`, `44-tutorial-complete.png` (70/88/117/83 KB) — the tutorial walk.
  - `45-fresh-launch.png` (253 KB) — the consent form over the auto-started tutorial.
- **TalkBack:**
  - `18-talkback-on.png`, `19-*`, `20-*`, `21-*`, `21b-*`, `22-*`, `23-*`, `24-*` (110–271 KB) — enabling TalkBack, the system permission prompt, and injected taps passing through.
  - `talkback/nav-1..12.png` and `talkback/key-1..3.png` (264–272 KB each) — Alt+arrow did not move focus.
  - `talkback/tab-1..8.png` (265–273 KB each) — Tab traversal with spoken fragments.
  - `talkback/switch-on-1..12.png`, `talkback/switch-activate-1..6.png`, `talkback/switch-off-1..12.png` (264–274 KB each) — "On / Show possible moves / Switch" and "Off / Show possible moves / Switch".
- **21g:** `21g/21g-{fresh-off,fresh-on,jump-off,jump-on,hint-working}-grey.png` (10–11 KB each) and `21g/21g-sheet-grey.png` (52 KB) — the greyscale dock readings.
- **18d:**
  - `18d/fs1.0-{0,488000,671000,3599000}.png` (261–264 KB) and `18d/fs2.0-*.png` (269–272 KB) — the pill at the four values, both scales.
  - `18d/home-fs2-*.txt` (2 KB each) — tree dumps used to find the chip.
  - `18d/fredoka/Fredoka[wdth,wght].ttf` (155 KB) and `18d/fredoka/digit-advances.txt` (1 KB) — F-1's font evidence.
- **Cold start:**
  - `coldstart/armA-3f2af90.apk` (19.7 MB) and `coldstart/armB-d017711.apk` (20.4 MB) — the exact builds measured.
  - `coldstart/sitting.csv` (4 KB) — every sample.
  - `coldstart/preconditions.txt` (1 KB) — the recorded preconditions and uptime.
  - `coldstart/run.log` (0 KB) — empty (no errors).
  - `coldstart/seed.db*` (48/32/0 KB) — the seeded database.
  - `coldstart/elapsed/e.db*` (48/32/0 KB) — the timer-edit working copy.
- **Backups:** `backup/app-data-before.tar` and `backup/app-data-after-restore.tar` (1006 KB each) — the founder's app data, and the restore check.
- **Logs:** `logcat-crash-buffer.txt` (0 KB), `logcat-home-20s.txt` (10 KB), `logcat-app-final-launch.txt` (104 KB).
- **Check:** `check/gradlew-check.log` (8 KB, Check output) and `check/slice-tests-rerun.log` (3 KB, forced rerun).
- **Helper scripts (never committed):** `tools/ui.py` (tree dump with dp bounds), `tools/px.py` and `tools/match.py` (colour runs), `tools/pill.py` (pill extent), `tools/contrast.py` (WCAG ratios), `tools/set_elapsed.sh` (resume-snapshot `elapsedMs` edit), `tools/coldstart.sh` (the §6.5 sitting).

## Open issues / risks
- **F-1 (18d)** is a FAIL for the developer. It is invisible on every emulator this studio uses, because of O-1.
- **15e is VOID**, and S11 still owes a decided sitting on a quiet host. It needs the Pixel_9 shut down, which only the producer or founder can arrange.
- **E-1**: the ux-designer should say which reading of "hero row" governs 15p.1.
- **O-1 (pre-existing, not this slice; owner: tech-lead):** Fredoka and Nunito never render on this emulator, even though the GMS font provider is present and online (it logs queries resolving). The whole app, including the founder's own `home-founder.png`, is drawn in Roboto. So the founder has been judging D-05d's and D-03l's type on the fallback font, and 18d can't be observed on an emulator. `Type.kt` has not changed since phase 1.
- **O-2 (pre-existing, not this slice; owner: ux-designer):** at 360dp and 200% the compact ribbon labels break mid-word ("Sta/rs", "Boa/rds"). Nothing is truncated, but it reads badly (`32`).
- **O-3 (cosmetic, 18e):** at 360dp and 200% the timer's digits sit about 2dp left of centre in the grown pill.
- **O-4 (info for the ux-designer):** on the device, the 360×640dp both-chips Home does not overflow. Chips are 80dp against §4.2.3's ≈108, and tiles 96 against 108/116, so 15k's fold-through-the-second-tile case never arises here.

## Handoff
Next: tech-lead — the merge decision on S11 with **VERDICT: FAIL** on 18d (F-1). Through the producer:
- the developer fixes F-1 and has it verified on a phone where Fredoka resolves;
- the ux-designer rules on E-1 (and may look at O-2 and O-4);
- the tech-lead takes O-1;
- the producer schedules a decided 15e sitting (Pixel_9a, quiet host, Pixel_9 down).
