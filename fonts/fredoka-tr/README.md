# fonts/fredoka-tr — Fredoka with the Turkish letters

The two Fredoka files the app ships, `app/src/main/res/font/fredoka_medium.ttf` and
`fredoka_semibold.ttf`, are Google Fonts' Fredoka v17 static instances with five glyphs added:
**Ğ ğ İ Ş ş**, each a composite of Fredoka's own letter and mark. Nothing else in either font changed.
This directory rebuilds them byte for byte. Nothing in it ships, and nothing in it is on a classpath.

## Why

No Fredoka build maps Ğ ğ İ Ş ş: not Google Fonts' v17 static or variable files, not the
google/fonts repository, not the designer's upstream at `35c584f`. Android picks a font for each
character from the font's character map, so a Turkish heading drew those five letters in the
platform's font. The founder chose to keep Fredoka in every language and add the letters from its
own parts (Q-039, 2026-11-01). The ruling is `docs/tech-design.md` §11.1, "Q-039 — Fredoka +
Turkish composites". The placement study is `.planning/reports/turkish-locale/research-analyst-fonts.md`.

## What is here

| File | What it is |
|---|---|
| `upstream/Fredoka-Medium.ttf`, `upstream/Fredoka-SemiBold.ttf`, `upstream/OFL.txt` | The inputs, as Google Fonts' download manifest serves them. Committed, so the build needs no network, and the origin is in the tree |
| `recipe.json` | The record: the inputs' sources and sha256, the five glyphs, each weight's (x, y) mark offsets, the `name` suffixes, the outputs' sha256 and sizes, and the tool versions that produced them |
| `build.py` | The builder. It reads everything from the recipe and refuses an input whose sha256 differs |
| `verify.py` | The verifier: the identity contract below. Any failed line exits 1 |
| `requirements.txt` | `fonttools` and `opentype-sanitizer`, pinned |
| `build.sh` | Creates `.venv` (git-ignored), installs the requirements, runs `build.py`, then `verify.py` |

## Re-running it

    fonts/fredoka-tr/build.sh            # rebuild in place, then verify
    fonts/fredoka-tr/build.sh --record   # rebuild and write the outputs' sha256 into recipe.json

You need `python3` 3.10 or newer, and a network the first time, to create the venv. On an
unchanged tree the rebuild changes no byte, and `verify.py` passes, its first check being that each
output's sha256 is the recipe's. `--record` is only for a reviewed change: an offset, a glyph, a
`name` string, a script or a tool version. The new hashes then appear in `recipe.json`'s diff,
beside the two font files.

The build is not part of `./gradlew check`, which stays offline and needs no Python. The Check holds
the shipped files to the recipe through two tests:
- `BundledFontRecipeTest` checks the hashes, the upstream `OFL.txt` against the shipped notice, the
  cmap delta, every upstream advance, and every English string's kerned advance.
- `BundledFontGlyphCoverageTest` checks every role's file against every shipped language's
  characters.

`app/build.gradle.kts` makes this directory an input of every test task, so an edit here re-runs them.

## Placement: how each offset was chosen

The recipe records the offsets, and `build.py` derives none. They were measured from the font
itself (the research-analyst's study, reproduced in the tech-lead's ruling):

- **Ğ ğ** = `G`/`g` + `uni0306` (combining breve), at Fredoka's own mark-to-base anchor offset. For
  symmetric marks, Fredoka's own composites sit at their anchor offsets to within 2 units (Ö exactly).
- **İ** = `I` + `uni0307` (dot above). The dot's ink bottom sits on the row of the dots of
  Fredoka's own `Idieresis`, centred on the stem. The anchor position, about 92 units higher, was
  rejected after rendering.
- **Ş ş** = `S`/`s` + `uni0327` (cedilla, which has no anchor). The cedilla's neck is centred under
  the letter's lowest point, with the same stroke overlap as Fredoka's own `Ccedilla` and `ccedilla`.

Each composite also takes, from its base:
- the component flags of the font's own composites: `0x604` on the base, `0x4` on the mark;
- the base's advance, with lsb set to the composite's xMin;
- the base's GDEF class;
- every pair-kerning entry and class of its base: 201 entries per weight.

## Determinism

Three rules keep every original byte in place, and two runs of the build identical:
- **No recalculation.** The written font is opened with `recalcTimestamp=False, recalcBBoxes=False`.
  Otherwise fontTools writes the build time into `head.modified`, and it re-encodes original glyphs
  with recomputed boxes.
- **Probe and writer.** Every measurement is read from a second, throwaway copy. The written font
  never expands an original glyph, so each original glyph's `glyf` bytes and `loca` offsets are the
  upstream's.
- **`maxp` by hand.** The four composite limits are raised to cover the new glyphs: up to 108 points
  and 4 contours, against the upstream's 100 and 3. Nothing else in `maxp` moves.

## What `verify.py` proves

For each weight it checks the following. The record of a run is quoted in
`.planning/reports/turkish-locale/android-game-dev.md`.

Bytes and order:
- the inputs' and outputs' sha256 match the recipe's;
- the upstream glyph order is kept, and the five glyphs follow it;
- every original glyph's raw `glyf` bytes, `loca` offset and `hmtx` entry are unchanged.

Tables:
- `GSUB`, `OS/2`, `STAT`, `gasp` and `prep` are byte-identical;
- `head` differs only in `checkSumAdjustment`, and `hhea` only in `numberOfHMetrics`;
- `maxp` differs only in `numGlyphs` and the four composite limits;
- `name` differs only in IDs 3 and 5, each the upstream string plus the recipe's suffix;
- `post` keeps its header, and `cmap` gains exactly the five code points.

The new glyphs and kerning:
- each composite is built exactly as recorded, and lies inside the font's recorded extremes;
- every original kerning pair keeps its value, and each composite kerns as its base on either side
  of every glyph;
- OTS passes with no message but success.

Shaping, when `hb-shape` is on `PATH`:
- every base `strings.xml` value the upstream can draw shapes alike in both files;
- each of the five letters shapes to its one glyph.

## A Modified Version under OFL-1.1

Adding glyphs makes these files a Modified Version (OFL §1). What that obliges, and how it is met:

- **No Reserved Font Name** is declared, neither in `OFL.txt` nor in the `name` table. So the family
  name "Fredoka" may stay (OFL-FAQ 5.7, 3.8), and so may the resource names.
- **§1, never sold alone:** the fonts ship only inside the app.
- **§2, notice and licence in every copy:** `app/src/main/assets/licenses/Fredoka-OFL.txt` ships
  beside the fonts. It is byte-identical to `upstream/OFL.txt`, which `BundledFontRecipeTest` checks.
  OFL asks for no change log in the licence file.
- **§4:** the authors' names are not used to promote the app.
- **§5:** the modified files stay under OFL-1.1.
- **Marked as modified** in four places:
  - the font's own `name` IDs 3 and 5 (`recipe.json`, `name_suffixes`);
  - the licences screen's Fredoka entry (`licenses_fredoka_modified`);
  - `THIRD_PARTY.md` §2's Fredoka row, and §8 for the build tools and the upstream copies;
  - this file.

## Changing anything

- **An offset, a glyph or a `name` string.** Edit `recipe.json` and run `build.sh --record`. Review
  the diff of the recipe and of both font files. `verify.py` must pass, the Check must be green, and
  a device check in Turkish judges the letters.
- **A tool version.** Edit `requirements.txt`, delete `.venv`, and run `build.sh --record`. fontTools
  does not promise byte-identical output across releases, so the hashes may move. The identity
  contract is what binds.
- **A new upstream release.** Replace the files in `upstream/` and update the inputs' sha256 in the
  recipe. Re-measure the offsets by the rules above, then run `build.sh --record`.
