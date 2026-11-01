# tech-lead report — colour-vision (deviation ruling: D-1, D-2, D-6 and the 15e question)
Date: 2026-10-24
Branch: feature/colour-vision (report only)
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; and the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (QA), licences-screen (code-auditor and ux ruling), reduced-motion-live-gate (ux ruling), colour-vision (code-auditor and ux ruling)
Ran on: claude-opus-5-5 at effort xhigh. This deviates from the pin (fable at xhigh): the Fable limit is reached, and founder answer Q-019 (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **D-1: accept, no change.** `material-icons-core` 1.7.8 has no hollow star. `StarOutline`, built at first use from `Icons.TwoTone.Star`'s opaque layer, is the right way to get one. My brief's `StarBorder` line (§4.1) was wrong and is withdrawn. Whether the standard stars may differ by colour alone is the ux-designer's call. If they may not, the change is one recipe constant (named below).
- **D-2: accept, no change.** The star pair is asserted where a recipe has one, the colour-blind palette. The standard pair is already pinned as today's look, so it cannot drift silently. The ten board-mark pairs run over both palettes.
- **D-6: accept the floor form; correct one KDoc and one name (test-only).** The test's stated reason, "the Results sky arrives with candy-backdrop", is wrong under direction B. No B surface reaches those floors.
- **15e: owed, as brief §9 ruled.** The tripwire prints `MainActivity.kt` and six `ui/theme/` files. None of the three rulings changes that.

## D-1: the hollow star

**What I verified**, in `material-icons-core-android-1.7.8-sources.jar` (the BOM's version, from the local Gradle cache) and at the branch's C0 merge (`69e1cdd`):
- **No hollow star ships.** The core set has `Star` in Filled, Outlined, Rounded, Sharp and TwoTone, and no `StarBorder` or `StarOutline`.
- **`Icons.Outlined.Star` is one closed path, a filled star.**
  - The base drew it under an import alias, `import androidx.compose.material.icons.outlined.Star as StarBorder` (`ResultsScreen.kt`, `BoardPreview.kt`).
  - So today's unearned stars are filled `outlineVariant` shapes, and the standard pair differs by colour alone.
  - The spec's §2 audit, its §3.4 and my brief's §4.1 all read the alias as a hollow glyph.
- **`Icons.TwoTone.Star` is a 0.3-alpha inner star plus one opaque path.**
  - The opaque path has two subpaths: the outer star (`Filled.Star`'s outline to within 0.01 of the 24-unit viewport) and the inner star, wound the other way.
  - Under the default non-zero fill rule the inner star is cut out. The opaque layer is therefore a hollow star at Material's own stroke weight.
  - `StarOutline` copies that path's data and fill type, and nothing else.
  - I did not compare it byte for byte with `material-icons-extended`'s `StarBorder`, which is not in the local cache.

**Ruling: accept, as built.** The alternatives, and why each is rejected:

| Option | Why not |
|---|---|
| `material-icons-extended`, for `Icons.Outlined.StarBorder` | A new dependency: brief §8's stop line, and spec §9's "no new dependency". `libs.versions.toml` already rules the extended set out as heavy |
| The path data copied into the repo, as an `ImageVector` literal or a vector drawable | A second copy of Apache-2.0 artwork in our source, with nothing keeping it in step with the library. It is also a new file or asset (spec §9) |
| A `Stroke` drawn along `Outlined.Star`'s path | Our stroke weight, not Material's. Brief §4.1 rejected a hand-drawn stroke |
| `Icons.TwoTone.Star` used whole | Its 30 % interior would make an unearned star read half-filled, and would tint the earned star's amber. That breaks CV-10's filled-against-hollow |

`StarOutline` is needed for the colour-blind earned star's outline too, not only for the unearned star. Taken literally, the spec's "`Icons.Outlined.Star` drawn over it" would paint the earned star solid `onTertiaryContainer`.

**Its one coupling, accepted.**
- `StarOutline` reads the library vector's structure: exactly one child path at `fillAlpha == 1f`.
- If a later BOM moved `material-icons-core` and changed that structure, `single` would throw on first access.
- That failure is loud, and the Check reaches it. `StarGlyphTest` draws `StarOutline` in both colour-blind cases and checks the hollow centre by pixel.
- So a library change fails the Check, not a device. No extra test is owed.

**Licence: no change.**
- `Icons.TwoTone.Star` ships in `material-icons-core`, which already has a `THIRD_PARTY.md` row (`androidx.compose.material:material-icons-core`, Apache-2.0, AndroidX terms).
- Nothing is copied into the repo, so no row is added.
- Spec §9's asset row names the wrong icon (`Icons.Outlined.Star`). That is wording, the ux-designer's to fix.

**My brief, superseded.**
- §4.1 said: "the outline glyph is `Icons.Outlined.StarBorder`, the hollow star the app already draws for unearned stars". Both halves were false: I read the import alias as the glyph. Its Open issue 3 is superseded with it.
- Read instead:
  - the outline glyph is `StarOutline`;
  - the standard unearned glyph is today's filled `Icons.Outlined.Star` in `outlineVariant`, frozen by CV-13.
- The developer was right to stop and report rather than substitute.

**The exact change:** none.

**What depends on the ux-designer.** The ux-designer is ruling in parallel on whether the standard palette's colour-only star pair is acceptable. A change to the approved standard look may also be the founder's, alongside spec §7's question. If the ruling is that the standard stars must differ in a channel other than hue, on this branch, the technical change is:
1. **The recipe.** In `ui/game/BoardMarkRecipe.kt`, `STANDARD_RECIPE.unearnedStarHollow = true`. `StarGlyph` already switches on this field, so no new code path is needed.
2. **The colour, if it changes too.** A hollow `outlineVariant` star reads 1.23:1 (F-CV4). That is one value, `boardMarksFor(Standard, …).unearnedStarOutline` in `ui/theme/BoardMarks.kt`, plus its pin in `BoardMarksTest`.
3. **The tests.**
   - In `BoardMarkRecipeTest.theStandardRecipeIsTodaysBoardMetrics`, the `unearnedStarHollow` line flips to `isTrue()`, and its comment is replaced.
   - The star-pair test loops over `BoardMarkPalette.entries`.
   - `StarGlyphTest.theStandardStarsAreTodaysPixels` keeps its earned half. Its unearned half checks the hollow centre, as the colour-blind case does.
4. **The spec.** CV-AC05's "with it off, every screen is pixel-identical to the base" stops holding for unearned stars on Results and the catalogue. The ux-designer amends CV-13 and CV-AC05, and QA's off-palette comparison excepts those stars.

If the ruling keeps the pair as it is for 3.0.0, it becomes a §7 finding and nothing here changes.

**Timing:** any such change lands before the 15e sitting. Item 1 is in `ui/game/`, which is not a start-path directory. Item 2 is in `ui/theme/`, which is, so after a sitting it would force a re-sitting.

## D-2: CV-AC08's star pair

**Ruling: accept, as built.**
- **CV-AC08 binds "the palette's recipe"**: the colour-blind palette's earned star (filled) against its unearned star (hollow). `earnedAndUnearnedStarsDifferInAChannelThatIsNotHueInTheColourBlindPalette` asserts exactly that.
- **My brief's "× both recipes" is withdrawn for stars.** §7.1 asked for the star pair over both recipes, on D-1's false premise. On the standard recipe the assertion is false today, and CV-13 forbids making it true.
- **The standard pair is still watched.**
  - `theStandardRecipeIsTodaysBoardMetrics` pins `unearnedStarHollow == false`, with the reason in a comment.
  - `StarGlyphTest.theStandardStarsAreTodaysPixels` pins the standard stars' pixels against today's glyphs, in the same frame.
  - Any change to the standard stars, deliberate or not, turns both tests red.
- **The board pairs are right as built.**
  - The test checks the ten pairs of the five marks that can share a board, over both palettes.
  - My brief's `BoardMark.entries.pairs()` would also have asserted 11 pairs of a star against a board mark. Those marks are never drawn on one surface.
  - The developer's `MARKS_ON_THE_BOARD` list is the better reading, and it supersedes the brief's.

**The exact change:** none.

**What depends on the ux-designer:** D-1's item 3, if the standard stars change.

## D-6: the light star floors

**What the test does.**
- `theComputedRatiosMatchTheSpecsTableWithinFiveHundredths` holds rows 9, 10 (each stop) and 11, in light, to `≥ figure − 0.05` rather than `within 0.05 of figure`.
- Its stated reason: the spec's floors are "set by the Results sky, a backdrop colour that arrives with `feature/candy-backdrop`".

**What is true.**
- Under direction B, Results and the tutorial are Sparse screens (`docs/ux/candy-backdrop.md` B-23, on `PegSolitarie-candy`).
- Their only backdrop is a static wash:
  - from `BackdropSkyTop`, which is identical to `background` (BD-09);
  - to `BackdropSparseLow`: `#FBF0FA` in light (L 0.897, lighter than the background), `#1C0E38` in dark (darker than it).
- The catalogue's pips stay on its card, whose container is unchanged.

I recomputed rows 9–12 with the test's own method (Machado 2009 severity 1.0 on linear sRGB, clamped; WCAG ratios). The script is in the session scratchpad and is not committed.

| Row (light) | Spec figure | Worst on this tree | With `BackdropSparseLow` added |
|---|---|---|---|
| 9: earned-star outline | ≥ 6.02 | 6.64 (background) | 6.64 |
| 10: fill, light stop | ≥ 3.22 | 3.39 (catalogue card) | 3.39 |
| 10: fill, deep stop | ≥ 3.64 | 3.82 (catalogue card) | 3.82 |
| 11: unearned-star outline | ≥ 5.02 | 5.31 (catalogue card) | 5.31 |

The other figures come out within 0.05 of the spec here: dark rows 9–12 at 15.04, 7.68, 5.56 and 9.58, and light row 12 at 3.56. Adding the wash moves none of them either.

So no surface on this tree reaches the three light floors, and none under direction B does either. They were computed against a darker "Results sky" that direction B no longer has. Spec §11.2 re-derived the board marks for B, not the star rows. Candy's merge will not complete this check: it leaves these numbers exactly as they are.

**Ruling: accept the floor form, and correct its reason (test-only).**
- An exact match cannot be asserted until the spec restates the three figures.
- The transcription check loses nothing that matters:
  - all three matrices are proven cell by cell on rows 1, 2, 4, 5 and 8, and on the dark floors;
  - the 3:1 case holds every row in every vision regardless.
- What is wrong is only the recorded reason. It tells candy's developer that a sky is coming that will set these floors, and under B there is no such sky.

**The exact change.** One file, `app/src/test/java/com/app/pegsolitaire/ui/theme/ColorblindPaletteContrastTest.kt`, with no assertion changed:
1. **Rename** `LIGHT_FLOORS_SET_BY_THE_RESULTS_SKY` to `LIGHT_STAR_FLOORS`. Its KDoc becomes: "Light rows 9 to 11: the spec's figures, which no surface on this tree or under backdrop direction B reaches (candy-backdrop B-23); this tree's worst case must clear them (tech-lead D-6)."
2. **Replace** the second paragraph of `theComputedRatiosMatchTheSpecsTableWithinFiveHundredths`'s KDoc with: "Rows 9, 10 and 11 in light are floors the spec computed against a Results sky that direction B replaced with the Sparse wash (candy-backdrop B-23), which is never darker than the background in light. Here and under B the worst neighbour is the background or the catalogue card, so those three are held at or above the spec's figure, not to it, until the ux-designer restates them (tech-lead D-6)."

The change rides the developer's next commit on this branch (a `test(colour-vision): …` subject), together with any auditor findings. It is not a dispatch of its own. A test-only commit leaves a 15e sitting standing.

**No duty for candy-backdrop.**
- Brief §10.3's candy paragraph gains nothing.
- `ColorblindPaletteContrastTest` runs unchanged on candy's merged tree, inside its Check.
- Candy's BD-08 already holds `BackdropSparseLow` on the right side of the background in each theme.

**What depends on the ux-designer** (non-blocking, not a merge condition):
- If the ux-designer restates §3.5's light rows 9–11 for direction B (the figures above, with the worst neighbour named), the developer moves the four entries into `SPEC_FLOORS` (`isWithin(TOLERANCE)`) and deletes `LIGHT_STAR_FLOORS`.
- If the figures stay as floors, the test stays as corrected.

## The 15e sitting: owed

**Yes, as brief §9 ruled.** I ran the tripwire on `d21ad07..32b2600`, where `d21ad07` is today's `git merge-base develop 32b2600`:
- **Line 1 prints seven files:**
  - `MainActivity.kt`;
  - `ui/theme/AppThemeRoot.kt`, `BoardMarkPaletteState.kt` and `BoardMarks.kt`;
  - `ui/theme/Clay.kt`, `Color.kt` and `Theme.kt`.
- **Line 2 prints nothing** (exit 1). `App.kt`, `di/`, `navigation/`, `ui/home/` (main source), the manifest, `res/font/` and every `themes.xml` are untouched.

**Why it is owed and not waived.**
- A `ui/theme/` edit alone does not owe a sitting. Tech design §10's RM row rules the reduced-motion fix sitting-free, because "Home's first frame loses one `Settings` read and gains no call".
- The same row says "a deviation that registers or reads anything on the first frame carries a sitting". This branch does both:
  - `MainActivity` injects a `@Singleton` whose `Eagerly`-shared DataStore collection starts in `onCreate`;
  - `AppThemeRoot` collects that flow at the root of the first frame.
- Home's composition is unchanged (CV-AC06), so the expected delta is small. Expectation is not a reading.

**None of the three rulings changes this.**
- D-1 and D-2 change no code.
- D-6's change is test-only.
- D-1's conditional change, if it comes, lands before the sitting (D-1, Timing).

**Preconditions as they stand.**
- **Arm A** is `d21ad07`. It contains phase 15 (`c9228a9`) and audio-review (`6aa7624`).
- **`develop`** has since moved to `29a078d`, by docs only; no start-path file changed.
- **§9's validity ruling stands.** The `colorblind_palette_on` key must be absent at both ends (checked with `run-as … strings`), and `app/schemas` must show no diff.

**For the producer's order.** Three in-flight branches edit start-path files:
- `feature/licences-screen`: `navigation/` and `di/`;
- `bugfix/reduced-motion-live-gate`: `ui/theme/Clay.kt`;
- `bugfix/home-chip-arrival`: `ui/home/` and both `values*-v27/themes.xml` files.

Any of them that reaches `develop` and is merged into this branch after its sitting forces a re-sitting. So the sitting stays last:
1. after the code-auditor's PASS;
2. after QA's device pass;
3. after the merge-in of that day's `develop`.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/tech-lead-deviation-ruling.md`: this ruling.
  - D-1 and D-2 are accepted with no change.
  - D-6 is accepted with one test-only KDoc and name correction.
  - The dependencies on the ux-designer are named, and the 15e sitting is confirmed as owed.
  - It supersedes three passages of the brief: the §4.1 glyph line, Open issue 3, and the star clause of §7.1's CV-AC08 row.
  - No other file was edited.

## Checks
- **Rules:** the plugin's `rules/*.md` for the role, read in full with one `Read` each (snapshot 0.6.2 < plugin 1.0.0); `.claude/rules/lessons.md` read (0 active); `.planning/recall/RECALL.md` absent: pass.
- **Inputs read in full:**
  - `android-game-dev.md` (9 lines, condensed by the producer);
  - `tech-lead-brief.md` (525 lines, four reads);
  - `docs/ux/colour-vision.md` (785 lines, four reads).

  Pass.
- **The code at `32b2600`:**
  - `git show 32b2600:` of six files, in full: `StarGlyph.kt`, `BoardMarks.kt`, `BoardMarkRecipe.kt`, `BoardMarkRecipeTest.kt`, `StarGlyphTest.kt` and `ColorblindPaletteContrastTest.kt`;
  - `git diff 69e1cdd..32b2600` on `ResultsScreen.kt`, `BoardPreview.kt` and `TutorialScreen.kt`;
  - `git diff --stat 69e1cdd..32b2600`.

  Pass; these are the basis of D-1 to D-6.
- **The icon library:** `unzip -l` and `unzip -p` on `material-icons-core-android-1.7.8-sources.jar`: pass.
  - It holds five `Star.kt` files (filled, outlined, rounded, sharp, twotone) and no hollow star.
  - `outlined/Star.kt` is one closed path.
  - `twotone/Star.kt` is a 0.3-alpha layer plus an opaque layer of two subpaths.
- **The extended set:** `find ~/.gradle/caches/modules-2 -path '*material-icons-extended*'` found no artifact, so `StarBorder` was not compared byte for byte.
- **Licence and build files:** pass.
  - `grep material-icons THIRD_PARTY.md`: the row is present (Apache-2.0).
  - `git diff --name-only d21ad07..32b2600 -- THIRD_PARTY.md gradle/libs.versions.toml app/build.gradle.kts`: empty.
- **D-6's numbers:** `python3 -I <session scratchpad>/cv-d6/rows.py`: pass. It produced D-6's table with the test's method, independently of the test's code.
- **Direction B's surfaces:** `sed -n` on `PegSolitarie-candy/docs/ux/candy-backdrop.md` (B-23, the token table, the Sparse bound, the colour-blind paragraph): pass.
- **The tripwire:** both lines on `d21ad07..32b2600`: pass, as brief §11 expected. Line 1 prints seven files; line 2 prints none (exit 1).
- **Arm A:** pass.
  - `git merge-base --is-ancestor` for `c9228a9` and `6aa7624` against `d21ad07`: both yes.
  - `git diff --name-only d21ad07..develop` filtered by the start-path list: none.
- **Tech design §10:** `grep -n` on `docs/tech-design.md` for the 15e row and the RM row: pass.
- **The in-flight branches:** start-path files per branch, from `git diff --name-only <merge-base>..HEAD` in `PegSolitarie-rm`, `-home`, `-lic` and `-tb`: pass; this is the basis of the order note.
- **`./gradlew check`:** not run.
  - This ruling changes no code, and the code-auditor runs the Check on the diff.
  - A Gradle build on this host now would cut across the parallel sessions.

## Open issues / risks
1. **The ux-designer's parallel ruling** on the standard star pair decides whether D-1's conditional change happens. If it does, and it touches `ui/theme/BoardMarks.kt`, it must land before the 15e sitting.
2. **Spec wording errors**, the ux-designer's to correct, non-blocking:
   - §2's star row says "filled against hollow";
   - §3.4 says "`Icons.Outlined.Star` drawn over it";
   - §9's asset row names `Icons.Outlined.Star`;
   - §3.5's light rows 9–11 carry floors that no surface reaches under direction B.
3. **`StarOutline` reads a library vector's structure** (D-1). If a later BOM changed `TwoTone.Star`, `StarGlyphTest` fails on the Check before any device sees it.
4. **My brief's premise error** (§4.1, the `StarBorder` alias) came from reading an import alias as a glyph name. A sign-off or brief that names an icon should read the icon's source, not its import line.

## Handoff
Next: producer —
1. Pass D-6's change to the android-game-dev with the code-auditor's findings, in one fix round. It is test-only and does not affect the sitting.
2. Give D-1's "What depends on the ux-designer" paragraph to the ux-designer's ruling.
   - If the standard star pair must change on this branch, D-1's items 1–4 are the change, made before the 15e sitting.
   - If the decision is the founder's, it joins spec §7's drafted question.
3. Route Open issue 2's spec wording to the ux-designer, non-blocking.
4. Order the 15e sitting last, after that day's `develop` merge-in, minding the three start-path branches.

Next: code-auditor — read D-1, D-2 and D-6 as tech-lead decisions. The brief's §4.1 glyph line, its Open issue 3 and the star clause of its §7.1 CV-AC08 row are superseded.

Next: tech-lead (sign-off) — cite this ruling beside the auditor's, QA's and the sitting's reports.
