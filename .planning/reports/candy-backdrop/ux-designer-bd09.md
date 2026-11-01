# ux-designer report — candy-backdrop: BD-09 against BD-14, the status-strip ruling
Date: 2026-11-01
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the product snapshot is older than the plugin, so I read the plugin's reporting.md, studio.md, dispatch.md and compose.md in full; the snapshot's gitflow, testing, ip-policy, logging, memory, puzzle-design, play-store, kotlin-android, audio and lessons.md were in context), android-accessibility, ui-ux-pro-max, compose-skill
Ran in parallel with: none
Ran on: claude-opus-5-5 (the harness's model; the agent file pins fable at xhigh). This is a design decision, so the producer should weigh it against rules/studio.md, Model and effort.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Ruling:** amend BD-09 so it agrees with BD-14. No code change. New decision B-09b in `docs/ux/candy-backdrop.md` §1.2. It is a UX decision.
- **The new rule:** the strip runs from the window's top edge to `statusInset`, outside the system's clock, icon and cutout boxes. It equals the theme's `background` (light `#F4F1FA`, dark `#1B1626`, ±1/255 per channel) on all eight screens, in both themes, at rest and in motion. It holds where `statusInset` ≤ 0.12 H, which every QA window meets.
- **Accessibility half:** the icon appearance stays dark in light and light in dark. Against the strip, the clock digits read ≥ 4.5:1 and the status icons ≥ 3:1. Expected values: light 5.57:1 (`develop` 5.65:1), dark 17.6:1 (`develop` 16.1:1).
- **Why not code:** B-09's own reason is that a backdrop must not stop under the bars. An opaque strip in the old bar colour would add a seam that `develop` does not have. Reverting the bars undoes BD-14. Any code change would also void the final cold-start sitting.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`, amended:
  - the front-matter `revised` line;
  - §1's status table, B-09 row: points to B-09b;
  - §1.2: **B-09b** added after B-09a, with the conflict, the three-part rule, expected values, what it does not change, and the reasons;
  - §3.3: the sentence under the sky-stops table no longer says "pixel-identical to today";
  - §5.5, Status bar bullet: restated;
  - §8.3: its `SkyTop` bullet restated;
  - §11 **BD-09** row: rewritten as "The status strip is `background`", with the icon-contrast pass line.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-bd09.md`: this report.

## Checks
- **Read in full:**
  - `.planning/reports/candy-backdrop/qa-engineer-device.md`;
  - `docs/ux/candy-backdrop.md`, all 1,858 lines, in three consecutive reads;
  - `.planning/reports/candy-backdrop/ux-designer-direction-b.md`.

  Pass: the contradiction is as QA states.
  - BD-09 (§11) and §3.3, §5.5 and §8.3 assumed that `SkyTop` = `background` makes the band "identical to today".
  - B-09, B-09a and BD-14 make the four `TopAppBar`s transparent, both resting and scrolled.
  - On `develop`, those bars painted the band their own colour.
- **Visual check of QA's captures, by Read:** pass.
  - Files: `.planning/qa/candy-backdrop/05-catalogue-light-B.png` and `23-catalogue-dark-B.png`.
  - Light: dark status icons and clock on the pale lavender strip.
  - Dark: white icons and clock on the deep violet strip.
  - In both themes, the strip and the transparent top bar read as one continuous field, with no seam.
  - This was by eye, not measured. The device reading governs.
- **WCAG 2.x contrast, by hand from the hexes**, sRGB compositing: pass.
  - The luminances:
    - B light `#F4F1FA`: L 0.890;
    - `develop` light `#FBF9FF`: L 0.955;
    - B dark `#1B1626`: L 0.0095;
    - `develop` dark `#241E31`: L 0.0152.
  - Dark icons, 60% black (AOSP single-tone, assumed): 5.57:1 on B, against 5.65:1 on `develop`.
  - Opaque black icons: 18.8:1 against 20.1:1.
  - White icons in dark: 17.6:1 against 16.1:1.
  - Every value is ≥ 4.5:1.
- **Geometry of the strip**, from §3.3: pass. On every density the sky is flat `SkyTop` from 0 to 0.12 H. The Pixel_9 band is 142 px of 2424 px, 0.059 H. No light is visible above the calm end (B-20), and no wave comes near it.
- `./gradlew check`: not run. This change is a design document only, and this role has no Bash.

## Open issues / risks
1. **The automated half of BD-09.** I could not read the test (no search tool). QA says it already has the form "equals `background`". If a test instead compares the strip against `develop`, its oracle must follow B-09b. The code-auditor confirms it in one line on the diff. A test edit would be test-only and outside the shipped APK.
2. **The icon-contrast half is new.** QA has not yet sampled it. It needs no device: the existing captures already hold it.
3. **Not ruled here:** QA's Open issue 4, the first-launch frame with black system bars (`01-tutorial1-light-B.png`). It comes before the app draws, and there is no `develop` capture to compare. It is informative only, and the tutorial's light strip should be read from a later capture.
4. **The model.** This ran on Opus, while the agent file pins fable at xhigh. The producer decides whether the ruling stands or a fable agent checks it.

## Handoff
Next: producer — commit the two files above on `feature/candy-backdrop`.

Next: qa-engineer — re-read BD-09 on the **existing** captures in `.planning/qa/candy-backdrop/` (no device session, no build):
- Read the captures that QA's BD-09 row used. Per screen and theme: Home `03`/`20`; gameplay `06`–`09`/`22`; tutorial `27` (dark) and a light capture after the first frame; catalogue `05`/`23`; Results `15`/`29`; Settings `17`/`24`; Licences `18`/`25`; licence text `19`/`26`.
- **The strip half** (equals `background`) is already measured as passing in QA's row. Restate it against the amended pass line.
- **Add the icon half:** the clock digits ≥ 4.5:1, and the battery, signal and Wi-Fi glyph cores ≥ 3:1, against the strip, in both themes.

Next: code-auditor — a one-line check that BD-09's automated test asserts "equals `background`" and not equality with `develop` (Open issue 1).

**Ruling: BD-09 is amended to "the status strip equals `background` (±1/255) on every screen in both themes, with the clock ≥ 4.5:1 and the status icons ≥ 3:1 against it" (B-09b). No code change is owed. No device re-check is owed. Only QA's desk re-read of the existing captures for the icon-contrast line is owed.**
