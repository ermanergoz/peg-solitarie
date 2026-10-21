# tech-lead report — 15-founder-feedback: how the fonts load (Q-013), and the 15e arm swap (E-1)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (base a8bfe8e; this report's own commit only)
Skills loaded: ergoz-soft:standards (role tech-lead; rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, and the plugin's dispatch.md. The plugin 0.6.2 copies were read wherever they differ from the product's 0.5.1 snapshot), android-architecture, android-gradle-logic
Ran in parallel with: the ux-designer's D-03m amendment in this worktree. It committed `a8bfe8e` during my session, and I read that commit's spec text. `docs/ux-spec.md` was not edited here.
Ran on: Opus 5.5 (claude-opus-5-5) at effort xhigh, the agent file's effort. **Staffing deviation:** the tech-lead is pinned to fable, and Fable is exhausted. This ran on Opus under the founder's Q-014 answer (`.planning/questions/FOUNDER.md`), which is scoped to phase 15. These are rulings, not a review. The work they specify is reviewed by the code-auditor and the qa-engineer, both also on Opus under Q-003. Inputs: the review reports `qa-engineer-loop2.md` and `code-auditor-loop2.md`, `docs/ux-spec.md`, `docs/ui/DESIGN-SYSTEM.md`, the code, and my role's earlier `phase-15/tech-lead-coldstart-gate.md`. Not read: `android-game-dev*.md` and `.planning/log/`.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot carries 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Fonts: bundle them.** Fredoka Medium and SemiBold, plus Nunito Regular, SemiBold and Bold, ship as five unmodified static OFL files in `res/font`. They load *blocking* on first use, so every device draws the first frame in the real faces, offline and without Play services.
- The Google Fonts provider path is deleted, along with its certificate file, its dependency and the test-only fixture. New tests go red if the type ever falls back to Roboto. APK cost is about +230 KB compressed. The expected cold-start cost is small and 15e measures it.
- **E-1: option (i).** Swapping the arm at block boundaries meets the gate, with a tightened procedure. The Pixel_9a stays required.
- The load threshold becomes ≤ half the host's logical cores: 5.0 on this 10-core M4.
- No spec dimension is at risk in the real faces. Eleven criteria are re-walked on the device anyway. No founder question.

---

## RULING 1 — the app's fonts are bundled

### Decision
**(b) Bundle the OFL files in `res/font`, as static instances. No hybrid.** Two changes go with it:
- The downloadable Google Fonts path is removed entirely: the `GoogleFont` provider, `font_certs.xml` and `ui-text-google-fonts`.
- F-A goes with it. Its in-file initialisation-order hazard cannot come back in the new shape (D3), and a test guards it (T1).

### What was weighed
| Concern | (a) Repair the downloadable path (F-A order + the correct prod cert) | (b) Bundle the static instances |
|---|---|---|
| **First launch, offline** | GMS must fetch both families. Offline, the app stays in Roboto for that process: Compose keeps a failed resolve as its permanent fallback (`Type.kt`'s own KDoc). | The same faces on every launch. No network is needed. |
| **No Play services** (Fire, Huawei, de-Googled phones, GMS disabled or outdated) | Roboto forever, silently. | The same faces. No GMS is involved at all. |
| **Fragility** | Depends on the GMS signing lineage. F-B shows how one corrupted certificate turns into a silent Roboto with nothing failing. | No certificate, no provider and no IPC. |
| **Home on every cold start** (§4.2.4 fit, 15f) | The per-process typeface cache starts empty, so Home's first frames draw in the fallback. When the faces arrive, Home lays out again: every title, pill and chip re-measures. That is a visible face swap plus a relayout on the start destination, on every cold start. | The first frame is final. There is no swap and no relayout, so §4.2.4's fit and 15f's "no layout shift" hold from frame one. |
| **Timer** (18d, `TimerDigits`) | The late-face case is live on every cold start. The cells re-measure when Fredoka lands, which is the path `TimerDigitsTest` stands in for. | The late-face path is dormant on devices. It is kept as a guard for any post-layout face or font-scale change. |
| **Cold start** (§6.5, 15e) | Nothing on the main thread before the first frame. The price is paid after first paint: the provider IPC, then a relayout of Home. | Blocking resource loads during Home's first composition. Home draws 4 of the 5 faces (Fredoka SemiBold for the title; Fredoka Medium for pill values and chip/tile titles; Nunito SemiBold for pill labels; Nunito Regular for supporting lines): ≈364 KB raw, ≈175 KB inflated from the APK. The typefaces are cached per process, so this is paid once. **Estimate, not a measurement:** a few ms per face on a phone, 10–40 ms in all on this shared-host emulator. That is far inside 15e's `max(2 × floor, 100 ms)`, and 15e measures it. |
| **Static vs variable** | — | **Static.** minSdk is **24**, and variation settings on resource fonts need API 26: a Nunito VF would render its default ExtraLight on API 24–25. A VF is also read once per weight: 3 × 276 KB for Nunito, more bytes on the first frame, not fewer. The saving would have been ≈50 KB compressed. |
| **APK size** | +0 | **+496,332 B raw, ≈229 KiB compressed.** aapt2 deflates `.ttf`; the estimate is `gzip -9` per file. Removing `ui-text-google-fonts` gives back a little. Size is baselined, not gated (D-12, `06-CONTEXT.md:42`). The last recorded release APK was 2,038,611 B at phase 7, before the ads and PGS SDKs, so this is a smaller share of today's. |
| **Weights actually used** | Six requested (Nunito Medium declared) | **Five.** Fredoka 500 (headlineSmall, title*) and 600 (display*, headlineLarge/Medium). Nunito 400 (body*), 600 (labelMedium/Small) and 700 (labelLarge). No role maps to Nunito Medium, and `app/src/main` has **no** `fontWeight`/`fontFamily` override (`grep`: 0 hits), so Medium is dropped. |
| **OFL obligations** | Nothing is distributed. | We distribute the font software. OFL-1.1 §2 allows bundling with any software, provided each copy carries the copyright notice and the licence, as stand-alone text files: D2. There is **no Reserved Font Name** in either OFL, and the files are unmodified. |
| **Testability** | Robolectric has no provider, so the production families can never render in a test. Every face test goes through a fixture, which is how F-A stayed hidden for a month. | Tests render the **production** `AppTypography` from `R.font`. Robolectric 4.16 NATIVE shadows `Font.Builder(AssetManager, path, …)` (`javap` below). A fall back to Roboto turns red (T1–T3). |

**Glyph coverage** (HarfBuzz, `.notdef` check): both faces carry `—`, `…`, `’`, `·`, `×` and Turkish. Fredoka has no Cyrillic. The app is English-only, and a player name in another script falls back glyph by glyph to the platform, as it did before.

**Fredoka has no `tnum`.** Its digits are proportional with or without the feature: "1" is 388 units, "2" is 587 (per 1000). That is why `TimerDigits` exists. Nunito's digits are tabular by design, at 600 each. The spec note is below.

### Developer change list (android-game-dev, on `feature/phase-15-ux-refresh`; TDD, tests first)

**D1 — font files**, in `app/src/main/res/font/`. Each is unmodified: no subsetting, instancing or table edits, only the Android resource file name changes. Source: the official Google Fonts family download manifest, `https://fonts.google.com/download/list?family=Fredoka` (and `=Nunito`), entry `static/<file>`. Retrieved 2026-09-29.

| Resource | Source entry (version) | Bytes | sha256 |
|---|---|---|---|
| `fredoka_medium.ttf` | `static/Fredoka-Medium.ttf` (v17) — https://fonts.gstatic.com/s/fredoka/v17/X7nP4b87HvSqjb_WIi2yDCRwoQ_k7367_B-i2yQag0-mac3OwyLMFuOLlNldbw.ttf | 49,932 | `8143bd54f5b1e04f4554b326a5e5fb70414add9b0a44210837fdfba2304ae6fa` |
| `fredoka_semibold.ttf` | `static/Fredoka-SemiBold.ttf` (v17) — https://fonts.gstatic.com/s/fredoka/v17/X7nP4b87HvSqjb_WIi2yDCRwoQ_k7367_B-i2yQag0-mac3OLyXMFuOLlNldbw.ttf | 49,900 | `021e30b2bb317fa3618bad84289179f93c9607220cbf9a77ff3f83d7688e43a8` |
| `nunito_regular.ttf` | `static/Nunito-Regular.ttf` (v32) — https://fonts.gstatic.com/s/nunito/v32/XRXI3I6Li01BKofiOc5wtlZ2di8HDLshRTM9jo7eTWk.ttf | 132,200 | `b0885056b60511b29aa9f69198055fe3bad13e76ec8a9b9e37cd61da7b4ee1e5` |
| `nunito_semibold.ttf` | `static/Nunito-SemiBold.ttf` (v32) — https://fonts.gstatic.com/s/nunito/v32/XRXI3I6Li01BKofiOc5wtlZ2di8HDGUmRTM9jo7eTWk.ttf | 132,152 | `c7fd194467c320db02afd5ebf09262ff900ab6065c61fc2eaa300f0236a48a74` |
| `nunito_bold.ttf` | `static/Nunito-Bold.ttf` (v32) — https://fonts.gstatic.com/s/nunito/v32/XRXI3I6Li01BKofiOc5wtlZ2di8HDFwmRTM9jo7eTWk.ttf | 132,148 | `ad443e47990c5fc4b263693b3018b7807868263b8c42ccb1115e030158931960` |

If a download's sha256 differs, Google has revised the family. In that case:
- Use the new files and record their hashes and versions in your report.
- Re-derive the T2 oracle, e.g. `hb-shape --font-size=1000 --output-format=json --no-glyph-names app/src/main/res/font/fredoka_medium.ttf "1111"` and sum the `ax` values.

Fredoka Medium's advances are identical to the current test fixture's variable font at `wght=500` (checked on every digit, the colon and "Peg Solitaire"). So the auditor's JVM timer numbers carry over unchanged.

**D2 — the licence texts:** `app/src/main/assets/licenses/Fredoka-OFL.txt` and `app/src/main/assets/licenses/Nunito-OFL.txt`. Each is the `OFL.txt` from the same family download, verbatim: its copyright line plus the OFL 1.1 text. Nothing reads them at runtime, so they are off the start path.

**D3 — `app/src/main/java/com/app/pegsolitaire/ui/theme/AppFonts.kt`** (new): `internal object AppFonts` with two `FontFamily` properties. Every font is `Font(resId, weight, loadingStrategy = FontLoadingStrategy.Blocking)`; the default is Blocking, and it is stated so T1 can assert it.

| Property | Weight → resource |
|---|---|
| `AppFonts.Fredoka` | `FontWeight.Medium` → `R.font.fredoka_medium`; `FontWeight.SemiBold` → `R.font.fredoka_semibold` |
| `AppFonts.Nunito` | `FontWeight.Normal` → `R.font.nunito_regular`; `FontWeight.SemiBold` → `R.font.nunito_semibold`; `FontWeight.Bold` → `R.font.nunito_bold` |

**Why an `object` in its own file:**
- An object initialises on first access, and a cross-file reference has no in-file declaration order to get wrong.
- F-A is exactly that class of bug: `AppTypography` was declared above `FredokaFamily`.

**D4 — `ui/theme/Type.kt`:**
- Delete `FredokaFamily`, `NunitoFamily`, `googleFontFamily`, `FontProviderSpec`, `FREDOKA_FONT`, `NUNITO_FONT` and both `googlefonts` imports.
- `heading()` and `body()` read `AppFonts.Fredoka` and `AppFonts.Nunito`.
- The role → weight mapping stays exactly as it is.
- Rewrite the KDoc. The faces are bundled and load blocking, so the first frame is drawn in them. The only fallback left is the platform's per-glyph fallback for characters the faces lack.

**D5 —** delete `app/src/main/res/values/font_certs.xml`.

**D6 — the build:**
- Remove `androidx-compose-ui-text-google-fonts` from `gradle/libs.versions.toml`, and its `implementation(...)` from `app/build.gradle.kts`.
- Replace the `font_certs.xml` paragraph in the `release {}` comment (`app/build.gradle.kts:382-388`) with one line: `res/font` is referenced from code through `R.font`, so the resource shrinker keeps it.

**D7 — tests.** T1–T3 are written first and shown red on today's code.
- **T1 `AppTypographyTest`** (JVM; no rendering needed). Table-driven over all 15 roles:
  - `fontFamily` is `AppFonts.Fredoka` for display*/headline*/title* and `AppFonts.Nunito` for body*/label*, and never `null`.
  - `fontWeight` equals the mapping above.
  - Every weight a role requests is present **exactly** in its family's font list, so there is no nearest-weight match and no synthesis.
  - Every font in both families is a `ResourceFont` with the resource id from the D3 table and `loadingStrategy == FontLoadingStrategy.Blocking`.
  - Red today: the families are null (F-A) and the fonts are `GoogleFont`s.
- **T2 `AppTypographyRendersBundledFacesTest`** (Robolectric, `@GraphicsMode(NATIVE)`, SDK 34, `mdpi`). Inside `AppTheme`, for every role, take the style from `MaterialTheme.typography` with `fontSize = 100.sp` and `letterSpacing = 0.sp`, and measure three probes with a `TextMeasurer`:
  - Each width equals the oracle below, ±1px.
  - At least one probe differs from the same probe in `FontFamily.Default` at the same weight by **≥ 8px**. If none does, the test fails and names the role.
- **T3 (closes F-C), in `TimerDigitsTest`:**
  - Render `TimerDigits` with the production `MaterialTheme.typography.headlineSmall.withTabularFigures().copy(fontSize = 100.sp)` in `AppTheme`, at xxxhdpi.
  - Every digit slot must be exactly as wide as Fredoka Medium's widest digit, "2" at 587/1000 em (≈234.8px), within ±1px.
  - Precondition in the same test: `FontFamily.Default`'s widest digit at that size differs from it by ≥ 3px. The assertion then fails under the M2 mutation (cells measured in `FontFamily.Default`). It must not compare against a second clock built the same way.
- **T4:** `GameStatusStripTest`'s 18d test uses `AppTheme`'s own typography. Delete the `copy(headlineSmall = …fontFamily = shippedFace)` override and the `fredokaMedium()` helper. Keep its "the face's set widths spread by > 0.5dp" precondition.
- **T5:**
  - Delete `app/src/test/resources/fixtures/fonts/` (the fixture, its OFL and its README) and `testing/ShippedFace.kt`.
  - `TimerDigitsTest`'s late-face test keeps its property. Its late face is built from the production Fredoka Medium bytes.
  - **Fallback, only if Robolectric cannot load `res/font`:**
    - Say so in your report, with the failure.
    - T2/T3 then load the production files by path (`src/main/res/font/*.ttf`, relative to the module directory the unit tests run in) through the D3 weight table.
    - T1 still ties `AppTypography` to those resource ids.
    - There is never a second copy of the bytes under `src/test`.

**D8 — KDoc (O-D):**
- `TimerDigits`' "Measured while measuring" paragraph: the reason becomes "if the face or the font scale changes after the first layout", not "the downloadable face arrives after the first frame".
- `TimerDigitsTest`'s KDoc drops "the face never resolves on the emulator".

**D9 — not yours to edit:**
- `docs/ux-spec.md` and `docs/ui/DESIGN-SYSTEM.md` belong to the ux-designer (text below).
- `THIRD_PARTY.md` lives on `feature/phase-14-release-docs` (rows below).

**D10 — out of scope:** no role's size, weight, line height or letter spacing; no dimen, layout or string; not `TimerDigits`' algorithm. The only visible change is that the specified faces finally render.

**T2 oracle.** Advance widths in units per 1000 em. HarfBuzz 14.4.0 (`hb-shape`) on the D1 files, kerning included. At 100sp on mdpi, divide by 10 for px.

| Face (roles) | `1111` | `0123456789` | `Peg Solitaire` |
|---|---|---|---|
| Fredoka Medium (headlineSmall, title*) | 1552 | 5349 | 5686 |
| Fredoka SemiBold (display*, headlineLarge/Medium) | 1532 | 5286 | 5715 |
| Nunito Regular (body*) | 2400 | 6000 | 5682 |
| Nunito SemiBold (labelMedium/Small) | 2400 | 6000 | 5797 |
| Nunito Bold (labelLarge) | 2400 | 6000 | 5937 |
| *Contrast: Roboto Regular / Medium (Google Fonts static; the platform's may differ slightly, so T2 measures `FontFamily.Default` itself)* | *2248 / 2272* | *5620 / 5680* | *5589 / 5674* |

How the probes split:
- `1111` separates Fredoka from Roboto by about 70px.
- `0123456789` separates Fredoka Medium from SemiBold by 6.3px.
- `Peg Solitaire` separates the three Nunito weights by 11.5px and 14px.

### Acceptance criteria (the code-auditor and the qa-engineer check these)
- **AC-F1:** T1 is green, and the developer's report shows it red on the pre-fix `Type.kt` first. That red run is F-A reproduced by a test.
- **AC-F2:** T2 is green for every role. The code-auditor runs these mutations, and each must turn at least one test red:
  - **M-a:** the families declared after `AppTypography` in one file (F-A reintroduced).
  - **M-b:** one role's family set to `FontFamily.Default`.
  - **M-c:** `R.font.fredoka_medium` swapped with `R.font.fredoka_semibold`.
  - **M-d:** `AppFonts.Nunito` rebuilt from `GoogleFont`.
- **AC-F3:** mutation M2 (cells measured in `FontFamily.Default`, `TimerDigits.kt:88`) turns T3 red. This closes F-C.
- **AC-F4:** `git grep -n -E "googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle` returns 0 hits.
- **AC-F5:** the debug APK carries all 7 new files, byte-identical to D1/D2:
  - `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E "res/font/|assets/licenses/"` lists 5 fonts and 2 licences.
  - `unzip -p <apk> res/font/<name>.ttf | shasum -a 256` matches D1 for each font.
  - For the release, the release-manager checks at the release branch, by resource name, that the AAB keeps the five: `bundletool dump resources --bundle <aab> | grep 'font/'`. Checked, not assumed.
- **AC-F6:** the Check command `./gradlew check` is green: ktlint, detekt, lint, tests, kover.
- **AC-F7 (device, QA):** the real faces render, per the discriminators in the re-walk below. They also render on a cold start in airplane mode.
- **AC-F8:** 15e is run on this build by Ruling 2's procedure.
- **AC-F9:** the re-walk list passes.
- **AC-F10:** founder screenshots in the real faces before the merge (Q-013).

### Re-walk on the device, in the real faces (qa-engineer)
**Precondition for every reading below:** the install under test passes both discriminators on the same session. Otherwise a reading may be in Roboto without anyone knowing, which is what happened to every S11 reading so far. The two faces cannot be told apart by the width of the timer or of "Peg Solitaire": the timer pill measures 85.67dp in Fredoka against 85.71dp in Roboto. The digit "1" is what separates them.

- **Fredoka:** seed the Challenge ladder so the current level is **111** and open it. The gameplay title "Level 111" (titleLarge 22sp, tracking 0) has an ink width, from the stem of "L" to the right edge of the last "1", of **≈82.1dp** in Fredoka Medium against **≈88.9dp** in Roboto Medium. **PASS at 82 ± 2dp.** A second reading: the Home Level pill value "111" (titleMedium 16sp) is ≈17.5dp in Fredoka against ≈22.8dp.
- **Nunito:** Home's Challenge tile line "Continue — Level 111" (bodyMedium 14sp, +0.25sp tracking) has an ink width of **≈142.6dp** in Nunito Regular against ≈134.1dp in Roboto. **PASS at 142.6 ± 2.5dp.**
- For any other level N, compute the expected ink width with `hb-shape --show-extents` on `app/src/main/res/font/`.
- **Airplane-mode cold start:** `cmd connectivity airplane-mode enable`, then `am force-stop`, then a cold start. Both discriminators still pass. Then disable airplane mode.

| Criterion | Why re-walk | Risk in the real faces (computed; the device governs) |
|---|---|---|
| **18d** | Defined "with the shipped font"; no device has had it yet. Take the five readings (00:00, 08:08, 11:11, 22:22, 59:59), plus the late-face pickup that `TimerDigitsTest` stands in for. | Expected pill **≈85.7dp at 1.0 and ≈116.3dp at 2.0** (the auditor's Fredoka JVM probe at xxhdpi). Constant ±0.5dp, or a FAIL. |
| **18e** | Measured in Roboto. | Low. Fredoka's widest digit (0.587em) makes the 200% pill ≈1.7dp wider than QA's 114.67dp, so the title ellipsises a little earlier, as allowed. |
| **18a** | Measured in Roboto. | None. At 22sp, "Peg Solitaire" is **125.1dp** (Roboto Medium 124.8) and "Level 1234" is **105.0dp**, against ≈178dp of room at 360dp. |
| **15o** | Measured in Roboto. | None at 1.0. At 36sp, "Peg Solitaire" is **205.7dp** in Fredoka SemiBold (205.5 in Roboto, per D-03m) against 296dp of room at 360dp. At 2.0 it wraps in either face, which 15o allows. |
| **15k** (the 360×640 fold, overflow ≤ 120dp; 48dp over per D-03m) | Measured in Roboto. | Low. Heights come from each role's `lineHeight` (Compose's `includeFontPadding` is off), so a band grows only if a line wraps. Home's strings are 0.3–5% wider in Nunito and none is near its wrap width at 360dp: "Pick up your saved board" 157dp, "Continue — Level 1234" 148dp. |
| **15m** (narrow ribbon ≤ 56dp, label and value on one line) | Measured in Roboto. | Low. "Boards" at 12sp is 38.7dp in Nunito SemiBold against 37.9dp. |
| **15p** (gaps, stage art ≥ 120dp), with **15g** on the same windows | Band heights were measured in Roboto. | Low, because nothing wraps. The one-chip art is the narrowest margin in §4.2.4's range. |
| **15i / 14** (200%) | Where lines wrap differs between faces at 200%. | Low. Check that nothing is clipped. Nunito's natural height is 1.364em (Roboto 1.172), so at labelMedium/bodySmall (12/16sp) its ink exceeds the 16sp line by ≈0.4dp. A tightly clipped pill would show it. |
| **15f** | The first paint was never seen in the real faces. | Better than before: there is no face swap. Only the "—" → value fill remains, and pills are sized from their labels. |
| **18b / 18c** | Spot-check only. | None. Neither depends on text width, and the strip's height at 200% is bound by its line height. |
| **Tutorial captions at 360dp** | The T1 caption goes from one line in Roboto (≈323dp with tracking) to **two lines in Nunito (≈331dp)** against ≈328dp of room. | Designed for: the 48dp caption reserve holds two lines. Check that **the board does not move between steps**. |
| **Whole app** | Every screen restyles, including pre-phase-15 ones: catalogue, results, settings, the consent dialogs we render, the tutorial. | Visual pass in light and dark, at 1.0 and at 2.0 on 360dp: nothing clipped or truncated that was not before. |
| **Founder screenshots (Q-013)** | "Founder sees screenshots in the real faces before merge". | Home in the founder's state, Classic gameplay, Challenge gameplay, Settings and tutorial step 1, in light and dark, from the install that passed the discriminators. |

**Spec dimensions at risk: none.** The 360dp title row at 36sp needs 205.7dp of its 296dp. The 360×640 fold changes only if a line wraps, and no Home string is within 5% of its wrap width. The device readings govern.

### THIRD_PARTY.md — the text (the file lives on `feature/phase-14-release-docs`; the producer routes this edit there)
**§2 "Assets shipped in the artifact".**

Opening sentence becomes: "The only binary assets in the app are twelve sound files **and five font files** (`app/src/main/res/font/`)."

Replace the Fredoka/Nunito row with these two:

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| Fredoka Medium, Fredoka SemiBold — `app/src/main/res/font/fredoka_medium.ttf`, `fredoka_semibold.ttf`, **bundled in the artifact** | Google Fonts, Fredoka **v17** static instances, unmodified (renamed only): `https://fonts.google.com/download/list?family=Fredoka`, entries `static/Fredoka-Medium.ttf` (sha256 `8143bd54…2304ae6fa`) and `static/Fredoka-SemiBold.ttf` (sha256 `021e30b2…d7688e43a8`, full hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/hafontia/Fredoka-One. © 2016 The Fredoka Project Authors | **OFL-1.1**, no Reserved Font Name | The `OFL.txt` shipped in the same family download (copyright line + full OFL 1.1) | Notice and licence ship in the APK as `assets/licenses/Fredoka-OFL.txt` (OFL §2, stand-alone text file); listed here; no in-app licences screen — see §7 |
| Nunito Regular, SemiBold, Bold — `app/src/main/res/font/nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf`, **bundled in the artifact** | Google Fonts, Nunito **v32** static instances, unmodified (renamed only): `https://fonts.google.com/download/list?family=Nunito`, entries `static/Nunito-Regular.ttf`, `static/Nunito-SemiBold.ttf`, `static/Nunito-Bold.ttf` (hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/googlefonts/nunito. © 2014 The Nunito Project Authors | **OFL-1.1**, no Reserved Font Name | The `OFL.txt` shipped in the same family download | Notice and licence ship in the APK as `assets/licenses/Nunito-OFL.txt`; listed here; see §7 |

The rest of the file:
- **§3:** delete the `font_certs.xml` row. The file is deleted (D5).
- **§4:** delete the `androidx.compose.ui:ui-text-google-fonts` row. The dependency is removed (D6).
- **§7**, in the "No in-app licences screen" bullet, replace "so **nothing shipped carries an attribution obligation**" with: "The two bundled font families carry OFL-1.1's notice condition. It is met by the stand-alone licence files in the APK (§2), so nothing shipped carries an **unmet** attribution obligation."
- **§8:** nothing to add. The test-only Fredoka fixture is deleted (T5). If T5's fallback keeps it, it is test-only: add it to §8's list.

---

## RULING 2 — E-1, the 15e sitting

### Precondition 7: option (i) meets the gate, tightened. (ii) is rejected.
The contradiction is my role's error. The second edition (`phase-15/tech-lead-coldstart-gate.md` §3) wrote "both installed and kept installed" without checking the arms' `applicationId`, which is shared. The first edition's "re-install between arms" was the true constraint.

**What precondition 7 is for.** No install is to perturb the samples, and both arms are to be measured in the same ART state. Swapping only at block boundaries meets both:
- An install never happens between samples.
- Every swap is followed by `compile -m speed -f`, and the first sample of each block is already discarded.
- **The null control swaps its one build the same way at every block boundary**, so whatever a swap costs lands in the floor. Absorbing a symmetric procedural cost is exactly what the null control exists to do.

**Why not (ii), a distinct `applicationIdSuffix`:**
- A suffixed package is a different app to Play Games (the OAuth client is bound to package and certificate), to UMP/AdMob consent storage and to GMS. Its start path differs from the one players run: automatic PGS sign-in fails, and there is no consent state. It would measure a start that nobody ships.
- Arm A would have to be a patched `develop`, no longer `develop` as built.
- Each package gets its own data directory, so the ladder is seeded twice and can diverge.
- It buys nothing the null control does not already give.

**This ruling does not revive loop 1's sitting.** That sitting was VOID on precondition 1 regardless.

**The adaptation is valid only while both arms share the Room schema and the DataStore keys.** Today `git diff --stat develop...HEAD -- app/schemas` is empty. A downgrade across a schema change could wipe the ladder, so the procedure checks this first.

### Pixel_9 vs Pixel_9a: the Pixel_9a stays required. A Pixel_9 sitting is VOID.
The verdict is relative and derived within one sitting, so a Pixel_9 sitting would be internally consistent. But substituting buys nothing:
- **No extra cost.** Precondition 2 already requires shutting the other emulator down, so booting the 9a instead of the 9 costs nothing more.
- **The instrument of record.** The 9a (`android-36.1` `google_apis_playstore_ps16k`, 16 KB pages) is the instrument both null floors (57 and 59 ms) were measured on. It is also the page size new devices ship and Play now requires apps to support.
- **The founder's data.** The Pixel_9 holds the founder's data and Play Games session. A sitting would seed a 200-level ladder over it.

Relaxing a named precondition for convenience is the drift the second edition removed.

### Host-quiet threshold: 1-minute load ≤ half the host's logical cores (≤ 5.0 here), read at defined moments
- **Why < 2.0 fails.** It was unattainable on this host: minimum 2.24, median 2.90 over 30 minutes with no build anywhere. macOS's own indexers and the visible emulator window keep the load above 2 on a 10-core M4 (4 performance + 6 efficiency cores).
- **Why it was not needed.** Both null floors were measured on busier hosts: 57 ms with three agent sessions, and 59 ms at loads 6.43/8.49. Both are far below the 150 ms VOID line. The null control, not the load average, is the gate's noise guard. The load ceiling only keeps an obviously contended host from wasting a sitting.
- **The new ceiling:** `sysctl -n hw.ncpu` ÷ 2 = **5.0**. That is below the load at which the 59 ms floor was taken, and above every reading in QA's 30-minute poll.
- **When the readings are taken:**
  - *Before:* the emulator booted and idle on the launcher for ≥ 60 s.
  - *After:* 60 s after the last sample, with the app force-stopped and the launcher in front.
  - So the sitting's own launches do not count against it.
- Precondition 3 is unchanged: no Gradle or Kotlin daemon, and no other agent session building or measuring.

### The procedure the qa-engineer runs
**Preparation (not timed)**
1. **Arms.**
   - Arm A = `develop` HEAD. Arm B = this branch's HEAD **after both D-03m and the font fix**: both change Home's first frame, so 15e runs once, on the head that carries both.
   - Build each with `:app:assembleDebug` in its own clean worktree (copy `local.properties`), then `./gradlew --stop` in each.
   - Record both commit SHAs and both APKs' sha256.
   - Check that the signers match: the `apksigner verify --print-certs` digests are equal (the same host debug keystore).
   - The null build = arm B's APK file.
2. **Schema identity.** `git diff --stat <A>..<B> -- app/schemas` must be empty, and no DataStore key may be added or removed on B. Otherwise stop: VOID, and back to the tech-lead.
3. **Emulators.**
   - The producer has cleared the founder's Pixel_9 shutdown with the founder.
   - `adb emu kill` every running emulator, then boot **`Pixel_9a`** alone (`emulator -avd Pixel_9a -no-snapshot-save`).
   - Wait for `sys.boot_completed=1` and 3 minutes on the launcher.
   - Record: `adb devices` (one device), one `qemu-system` process, `getprop ro.build.version.sdk` = 36, `getconf PAGE_SIZE` = 16384.
4. **Device state.**
   - Animations 0/0/0, font scale 1.0, `wm size reset`, `wm density reset`, `svc power stayon true`, launcher in front.
   - Back up the 9a's app data if the app is installed there.
   - Install arm B, seed the ~200-level ladder (loop 2's `seed.db`, `SUM(stars)` = 401), and confirm Home reads "Stars, 401". Then `am force-stop`.
5. **Host.**
   - `pgrep -fl 'GradleDaemon|KotlinCompileDaemon|gradlew'` finds no java process.
   - The producer has declared no other agent session building or measuring.
   - Record `sysctl -n hw.ncpu` → threshold = half of it.
   - `uptime` 1-minute load ≤ threshold. Otherwise poll every 30 s for up to 30 min, then VOID with the readings.

**Block** (one block = one arm label, five samples):
1. `adb install -r -d --no-incremental <that label's APK>`
2. `adb shell cmd package compile -m speed -f com.erman.pegsolitarie`
3. Confirm the swap: `adb shell pm path com.erman.pegsolitarie`, then `adb shell sha256sum <base.apk path>` must equal that label's APK sha256. Record it. If they differ, VOID.
4. `adb shell am force-stop com.erman.pegsolitarie`, then a 2 s pause.
5. Take five samples, each by:
   - `adb shell am start -W -n "$(adb shell cmd package resolve-activity --brief com.erman.pegsolitarie | tail -1)"` and record `TotalTime`;
   - then `am force-stop` and a 2 s pause.
   - **Discard the first sample** of the block.

**Sitting (timed), in this order:**
1. **Null control.** Ten blocks alternating the labels N1/N2, both installing arm B's APK. That gives ≥ 20 kept samples per label. `floor = |median(N1) − median(N2)|`.
2. **Real arms.** Ten blocks A/B/A/B…, ≥ 20 kept samples per arm.
3. **After.**
   - `am force-stop`, launcher in front, wait 60 s, then `uptime` (≤ threshold, else VOID).
   - `SUM(stars)` must still be 401, or the sitting is VOID.

**Verdict:** §6.5 unchanged.
- PASS if `median_B − median_A ≤ max(2 × floor, 100 ms)`.
- FAIL if greater.
- VOID if `floor > 150 ms`.

If it FAILs on this build, the tech-lead rules on the fonts' share **before** the degradation path is walked. Those steps remove art, and removing art cannot pay for type.

**Record:** everything in §6.5's "Where the numbers go", plus every swap's installed sha256.

**Restore:** app data, animations, font scale. The founder's Pixel_9 may then be booted again.

---

## Spec text for the ux-designer (exact; apply after D-03m's edit)
**§6.5.** After "**Hard rules for Home's first frame.** No blocking read. No image decode. No asset load. No bitmap. No new SDK call.", insert:

> *(Amended 2026-09-29 — tech-lead font ruling, `.planning/reports/15-founder-feedback/tech-lead-fonts.md`.)* **One named exception: the app's type faces.** Fredoka and Nunito ship as five static font files in `res/font` and load blocking on first use, during Home's first composition, so the first frame is drawn in the real faces and nothing lays out again when they arrive. They are the only asset Home's first frame reads, their cost is inside 15e's measured delta, and they replace the downloadable path, which drew the first frames of every cold start in the platform face and then laid out Home again. No other asset, font or otherwise, joins them.

Add this row to the first-frame table:

> | Type faces (Fredoka ×2, Nunito ×3, `res/font`) | Blocking resource load of the faces the first frame draws (four of the five on Home; 50–132 KB each) | Low — measured inside 15e; **not** on the degradation path (a face is not decoration) |

Append to the degradation-path paragraph:

> *(2026-09-29.)* If 15e fails on the build that first bundles the faces, the tech-lead rules on their share before any step is taken: the steps remove art, and removing art cannot pay for type.

**§6.5 preconditions.**
- **Precondition 1:** append "A sitting on any other AVD is VOID; the `Pixel_9` is not a substitute (tech-lead-fonts.md, Ruling 2)."
- **Precondition 4:** replace it with:

> 4. `uptime` recorded **before** the sitting, with the emulator idle on the launcher for ≥ 60 s, and **after** it, 60 s after the last sample with the app force-stopped; the **1-minute load average ≤ half the host's logical cores** (`sysctl -n hw.ncpu` ÷ 2; **5.0** on the studio's 10-core host) at both ends. *(Amended 2026-09-29: the earlier "< 2.0" was unattainable on this host and stricter than decidability needs; the null control is the noise guard.)*

- **Precondition 7:** replace it with:

> 7. **Each arm swapped in only at a block boundary.** Both arms carry `applicationId` `com.erman.pegsolitarie` and cannot be installed side by side, so the arm under measurement is installed at the start of its block — `adb install -r -d --no-incremental <arm>.apk`, then `cmd package compile -m speed -f com.erman.pegsolitarie`, then `am force-stop` — and never between samples; the installed `base.apk`'s sha256 is recorded after every swap and must equal that arm's. The null control swaps its one build back in at each of its block boundaries, so the floor contains whatever a swap costs. Valid only while both arms share the Room schema (`git diff <A>..<B> -- app/schemas` empty) and the DataStore keys; otherwise the sitting is VOID. *(Amended 2026-09-29, tech-lead ruling on qa-engineer E-1 — "both arms installed and kept installed" was a tech-lead error: the arms share one `applicationId`.)*

**Criterion 15e**, first paragraph:
- Replace "both arms installed for the whole sitting" with "each arm swapped in only at a block boundary, the null control swapping its one build the same way (§6.5 precondition 7)".
- After "no concurrent Gradle build anywhere on the host", add "and a 1-minute load ≤ half the host's logical cores at both ends".

**§6.3.** Append:

> *(Note 2026-09-29 — tech-lead font ruling.)* Fredoka has **no `tnum` feature**: its digits are proportional ("1" is 388 units per 1000, "2" is 587) whatever `withTabularFigures()` requests, which is why the timer sets its digits in cells (§4.3.1, `TimerDigits`). Nunito's digits are tabular by design (600 each). Fredoka stat-pill values are proportional; they do not tick, so nothing jitters.

**§9.** Replace the row "| Fredoka, Nunito | font | downloadable Google Fonts, already wired | OFL (already shipped) |" with:

> | Fredoka Medium/SemiBold, Nunito Regular/SemiBold/Bold | font | **bundled** static instances in `app/src/main/res/font/` (Google Fonts, unmodified; tech-lead font ruling 2026-09-29) — replaces the downloadable-font path | OFL-1.1, no Reserved Font Name; notice and licence ship in the APK (`assets/licenses/`); **one `THIRD_PARTY.md` row per family** |

After the section's first sentence, add: "*(Amended 2026-09-29.)* The fonts are the exception: bundled, they are a third-party asset in the artifact and need their `THIRD_PARTY.md` rows (text in `tech-lead-fonts.md`)."

**Criterion 18d.** Optional, after "On a device with the shipped font", add: "— the shipped font confirmed on that install by the discriminators in `tech-lead-fonts.md`".

**`docs/ui/DESIGN-SYSTEM.md` §Typography.** Replace the first sentence:

> Fredoka (headings/display, weight 500–600) + Nunito (body/labels, 400–700), **bundled as static font files in `res/font`** (Fredoka Medium, SemiBold; Nunito Regular, SemiBold, Bold — OFL-1.1) and loaded on first use, so the first frame is drawn in them on every device, online or not.

In the tabular-figures clause, append: "— honoured by Nunito; Fredoka has no `tnum`, so the timer sets its digits in fixed cells (`TimerDigits`)."

---

## Outputs
- `.planning/reports/15-founder-feedback/tech-lead-fonts.md`: this report, with both rulings, the developer's change list, the test oracle, the acceptance criteria, the re-walk list, the 15e procedure, the THIRD_PARTY rows and the spec text. It is the only committed file.
- Scratch only, never committed: the official font files and OFL texts, downloaded to the session scratchpad to measure them.

## Checks
- `git grep` / `grep -rn` over `app gradle build-logic docs` for `GoogleFont|googlefonts|font_certs|FredokaFamily|NunitoFamily|fontFamily|fontWeight` — all the font wiring is in `Type.kt`, `font_certs.xml`, the catalogue and the build. **0** `fontWeight`/`fontFamily` overrides in `app/src/main`.
- `grep -rhoE "typography\.[a-zA-Z]+" app/src/main/java | sort | uniq -c` — 7 roles are called directly. The M3 components use the rest. Five weights are needed.
- `curl https://fonts.google.com/download/list?family={Fredoka,Nunito,Roboto}` — the official manifests; the static entries were fetched to the scratchpad. `stat` and `shasum -a 256` give the D1 values. `gzip -9 -c | wc -c` gives 234,503 B compressed in all.
- `hb-shape --font-size=1000 --output-format=json` (HarfBuzz 14.4.0) — every advance in this report. `--show-extents` for the ink discriminators. `--features=tnum` changes nothing in Fredoka. `--variations=wght=500,wdth=100` on the fixture VF equals the static Medium on every digit, the colon and "Peg Solitaire".
- `hb-shape` `.notdef` check — `— … ’ · × ğ ş İ` present in both faces; `й` is missing from Fredoka only.
- A Python `struct` read of `head`/`hhea`/`OS/2`: Fredoka 974/−236 (1.210em), Nunito 1011/−353 (1.364em), Roboto 1.172em. Both bundled faces have `USE_TYPO_METRICS` and no `fvar`.
- `grep "Reserved Font Name"` on both OFL texts — only the definition clause, so no RFN is declared.
- `javap` on `ui-text-android` 1.11.3 — `ResourceFont` is public with `getResId`, `getWeight` and `getLoadingStrategy`, and `Font(int, FontWeight, FontStyle, FontLoadingStrategy)` exists. On Robolectric `shadows-framework-4.16`, `ShadowNativeFontBuilder.__constructor__(AssetManager, String, boolean, int)` exists, so T2 should load `res/font` (not yet run).
- `build-logic/src/main/kotlin/android-app.gradle.kts:29` — minSdk **24**, which decides static over variable.
- `git diff --stat develop...HEAD -- app/schemas` — empty, so option (i)'s schema condition holds today.
- `sysctl -n hw.ncpu hw.perflevel0.physicalcpu hw.perflevel1.physicalcpu` — 10 / 4 / 6 (Apple M4). `uptime` at 14:11 gave `4.22 3.17 2.69`, with this session and others running.
- No Gradle build was run, per the dispatch.

## Open issues / risks
- **The cold-start figure is an estimate.** 15e on the font build is the measurement. If it FAILs, the fonts come back to me before any art is dropped.
- **Robolectric loading `res/font` is inferred from the shadows, not yet run.** T5 names the fallback, and the code-auditor judges it if it is taken.
- **API 24–25.** `TypefaceCompat` copies each resource font to the cache directory before loading it, so a first launch there does a small disk write. That is a tiny share of devices and was not measured.
- **Licensing:**
  - OFL-1.1 is not in `rules/ip-policy.md`'s matrix. My technical reading: its copyleft binds only the font files (like MPL-2.0's file-level row), they ship unmodified, and bundling is expressly permitted. So this is **not** a copyleft escalation.
  - The research-analyst records the verdict. A plugin retro item should add OFL-1.1 to the matrix.
  - Bundling makes THIRD_PARTY §7's "nothing shipped carries an attribution obligation" false; D2 meets the obligation.
  - The studio's licences-screen gap (a Settings row) stays a v1.1.1 backlog item unless the producer pulls it in. That work is ux-designer + developer.
- **`docs/tech-design.md` does not exist in this repo**, which predates the studio. This report is the decision record. Whoever creates the tech design folds it in.
- **O-B** (idle Home draws at ≈61 fps because of `HomeBlobs`) is not ruled here. It is a pre-release `compose-performance-audit` item (rules/compose.md). It does not touch 15e, because the app is force-stopped between samples.
- **Retro candidate (tech-lead):** a precondition that names installed artifacts must be checked against their `applicationId`. My second-edition ruling was not, and E-1 is the result.

## Handoff
Next: producer —
- **Developer:** D1–D10 with T1–T5 on `feature/phase-15-ux-refresh`, alongside D-03m.
- **ux-designer:** the spec and design-system text above, once D-03m's edit is committed.
- **research-analyst / release-manager:** the THIRD_PARTY edits on `feature/phase-14-release-docs`, the OFL verdict, and AC-F5's AAB check at the release branch.
- **code-auditor:** re-audit, including mutations M-a to M-d and M2.
- **qa-engineer:**
  - the discriminators first;
  - then the re-walk list;
  - the founder's screenshots (Q-013);
  - then the 15e sitting by Ruling 2's procedure, after the producer has the founder's Pixel_9 shut down and confirms no other session is building.
- **Tech-lead sign-off** then follows. No founder question is needed.
