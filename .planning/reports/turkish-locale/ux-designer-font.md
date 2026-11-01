# ux-designer report — turkish-locale: the Fredoka glyph gap (developer escalation 1)
Date: 2026-11-01
Branch: feature/turkish-locale (committed by the producer)
Skills loaded: ergoz-soft:standards (role: ux-designer; the snapshot is 0.6.2 against plugin 1.0.0, so the plugin copies were read in full: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, dispatch.md; the product's .claude/rules/lessons.md), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none (one Explore helper on haiku located the ux-spec typography section and the Home title composable, rules/dispatch.md; it wrote nothing)
Loops: 0
Lessons applied: none (the snapshot has no active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Decision D-16: the heading face follows the language; the brand face does not.** On a Turkish device the translatable heading roles (`display*`, `headline*`, `title*`) draw in Nunito at the nearest heavier weight — Fredoka Medium → Nunito SemiBold, Fredoka SemiBold → Nunito Bold; sizes, line heights and tracking unchanged. Three surfaces keep Fredoka in every language because their glyph set is closed: the Home wordmark "Peg Solitaire" (`app_name`, untranslatable ASCII — the gap can never reach it, so the gradient title is not affected), the gameplay clock (`TimerDigits`) and the numeral values (ribbon pills, Results stats). English is untouched; no new file, licence row or THIRD_PARTY.md change.
- Rejected: rewording the five strings (copy constrained by a font; a player name hits the gap next), a per-glyph fallback chain (a Nunito ş inside a Fredoka word is the defect C-2 catches; API 29+ against minSdk 24), shipping the fallback letters. A different display face for every language (option 2) and a Modified Fredoka with the five letters drawn (OFL permits it) are 3.1 paths, not release-week ones.
- This is a taste question for the founder — the Turkish headings are plainer than the Fredoka ones approved for English — drafted below as **Q-038**, non-blocking; the build proceeds on D-16.
- Spec written: D-13 amended, D-16 added, §7.3 rewritten (finding, decision table with the exact styles, options weighed, build requirements), §6 face-width note, §7.6 tests, §8 C-2 rewritten with C-2a–C-2d and a C-7 clause, §9 items; mirrored in `docs/ux-spec.md` §6.3 and §9, and `docs/ui/DESIGN-SYSTEM.md`.
- The font ruling of 2026-09-29 said "both faces carry … Turkish" from an `hb-shape` read; the developer's cmap read shows Fredoka lacks Ğ ğ İ Ş ş in every official build. Android itemises by cmap, so the developer's finding governs; recorded in §7.3.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux/localisation-tr.md` — §1 D-13 amended and **D-16** added; §6 face-width paragraph and the `challenge_tile_title` slot; **§7.3 rewritten** (what the proof found; the decision table: Brand / Numerals / Headings / Text with English and Turkish faces and weights; the five options weighed; the visual cost judged; "What the build must honour" 1–5); §7.6 the glyph-coverage test's Fredoka closed set and a typography test, the goldens bullet annotated; §8 **C-2** rewritten, **C-2a** heading face is Nunito (shape check plus a numeric discriminator: "Seviye 111" title digits ≈ 39.6dp in Nunito against ≈ 25.6dp in Fredoka), **C-2b** wordmark and numbers kept Fredoka (≈ 205.7dp title, ≈ 17.5dp pill "111"), **C-2c** English unchanged, **C-2d** a player name with ş/ğ/İ; C-7 taller-ink clause; §9.1 Q-038 drafted; §9.2 release-manager (e), research-analyst, tech-lead items; §11 change-log row.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux-spec.md` — §6.3 Typography: the D-16 rule of record (one paragraph); §9 font row: no new file for Turkish.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ui/DESIGN-SYSTEM.md` — §Typography: one amendment paragraph pointing at the rule.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/.planning/reports/turkish-locale/ux-designer-font.md` — this report. No code, no resource, no string was touched (standing rule 4).

### Q-038 — draft for `FOUNDER.md` (the producer queues it; this role may not write that file)

    ## Q-038 — The Turkish app's headings are Nunito, not Fredoka   (open)
    Date: 2026-11-01    Asked by: ux-designer (via the producer)
    Context: Fredoka, the heading font you approved, has no Ğ ğ İ Ş ş in any official build, so Turkish words like "Başarımlar" and "Titreşim" would show those letters in the phone's own font. On a Turkish phone the headings (tiles, dialog titles, Settings rows, Results headline, "Seviye N") now draw in Nunito, the body font, one weight heavier; the gradient "Peg Solitaire" title, the clock and the big numbers keep Fredoka. English is untouched. (docs/ux/localisation-tr.md D-16, §7.3)
    Options: 1) Keep it so: Nunito headings in Turkish, Fredoka wordmark and numbers everywhere (being built) 2) Replace Fredoka in every language with a rounded font that covers Turkish (Baloo 2, Quicksand, Comfortaa, Varela Round or Mitr, after a licence and coverage check) — one look in both languages, but a change to the look you approved; 3.1 at the earliest 3) Keep Fredoka headings in Turkish and accept the mismatched letters in those five words and in player names
    Recommendation: ux-designer — 1. The Turkish player sees one consistent app and keeps the brand moments; nothing you approved changes in English; no new font, licence or file in a release week. A Fredoka with the five letters drawn in-house is possible 3.1 work under 1 or 2.
    Blocks: nothing
    Answer:

## Checks
This role has no Bash; nothing was built or run. What was verified by reading:
- `app/src/main/java/com/app/pegsolitaire/ui/theme/Type.kt` — the role → family map is as §7.3 states: nine heading roles on `AppFonts.Fredoka` (Medium for `headlineSmall` and `title*`, SemiBold for `display*`, `headlineLarge`, `headlineMedium`), six text roles on `AppFonts.Nunito` (Regular, SemiBold, Bold); no other weight is bundled, so D-16's 600/700 mapping needs no file — pass.
- `ui/home/HomeHero.kt:96–102` (helper read) — the wordmark is `stringResource(R.string.app_name)` in `displaySmall` with `titleGradientBrush()`; `app_name` is `translatable="false"` ASCII — the gradient title is unaffected by the gap and is listed as a fixed Fredoka surface — pass. (Side note for the spec, not this task: `docs/ux-spec.md` names the brush `ctaGradientBrush()` while the code's is `titleGradientBrush()`, `ui/theme/Clay.kt:115`; a naming drift to tidy in a later spec pass.)
- `.planning/reports/turkish-locale/android-game-dev.md`, open issue 1 — the cmap table (Nunito 13/13; Fredoka missing Ğ ğ İ Ş ş in the shipped files, Google Fonts v17 static and variable, `google/fonts`, upstream) and the five affected keys; §7.3 and C-2 quote them — pass.
- `.planning/reports/15-founder-feedback/tech-lead-fonts.md` T2 oracle — the advances used for §6 and C-2a/C-2b: Nunito "1" 600, Fredoka Medium "1" 388 per 1000 em; "Peg Solitaire" 5686/5715 (Fredoka) and 5797/5937 (Nunito) — the ≈ 2–4 % width note and the ≈ 39.6dp / 25.6dp / 213.7dp figures are computed from them, not measured; the device governs — recorded as such in the spec.
- `docs/ux-spec.md` §6.3 (lines 4517–4536) and §9 rows 5137–5138, `docs/ui/DESIGN-SYSTEM.md` §Typography — the only places that state the role → face mapping; each now carries the D-16 note — pass.

## Open issues / risks
- **Q-038 is open.** An answer of 2 or 3 changes D-16, §7.3 and C-2 in a later edit; nothing built under 1 is wasted (the Nunito-in-Turkish path is the fallback any new face would also need until its coverage is proven).
- **The mechanism is the tech-lead's.** §7.3 names the outcomes only: the face follows the app's resolved resource configuration (per-app language, C-5, and the mid-game switch, C-4, both covered), the three fixed surfaces use a style the theme names, English resolves as today, and §7.6's two tests pin it. A `values-tr` resource is the natural shape; a configuration-keyed typography is the other.
- **Numeric discriminators are computed, not measured.** C-2a/C-2b give advances from the T2 oracle; ink widths are a dp or two narrower. The tech-lead can refine them with `hb-shape --show-extents` as the 2026-09-29 ruling did; the shape checks in C-2a stand on their own.
- **Nunito is taller than Fredoka** (1.364 em against 1.210 em). A heading inside a clipping bound could lose a descender in Turkish; C-7 now checks it. The §6 figures grow ≈ 2–4 %; "Seviye 1234" has the room.
- **The tech-lead font ruling's glyph line is wrong for Fredoka** (`hb-shape` composes from a base and a combining mark where a device's itemiser does not). Recorded in §7.3; a retro candidate for the research-analyst: glyph coverage is proven by cmap, never by family metadata or a shaper.
- **Upstream:** Fredoka's `google/fonts` METADATA.pb declares `latin-ext` and the file lacks it. A fixed upstream file would be D-13's original remedy; §9.2 routes a report to the research-analyst through the producer.
- **Not this role's, through the producer:** the Roborazzi goldens against `docs/tech-design.md` §8.5 (the developer's escalation 2; §7.6 annotated, tech-lead decides); any tr-TR store screenshot shows Nunito headings (release-manager, §9.2 e); `settings_play_games_signed_in_as` on an *English* device still falls back glyph by glyph for a Turkish name — pre-existing, out of scope, noted in C-2d.

## Handoff (first edition — superseded by the addendum below)
Next: producer — queue Q-038 in `.planning/questions/FOUNDER.md` from the draft above (non-blocking); commit `docs/ux/localisation-tr.md`, `docs/ux-spec.md`, `docs/ui/DESIGN-SYSTEM.md` and this report on `feature/turkish-locale`; route the D-16 mechanism and the §7.6 tests to the tech-lead with the developer's escalation 2 and 4; the `google/fonts` note to the research-analyst.
Next: tech-lead — rule the D-16 mechanism against §7.3 "What the build must honour" (locale-qualified resource or configuration-keyed typography; the three fixed Fredoka surfaces as a named theme style; the English guard), then the developer builds it test-first (§7.6: the Fredoka closed-set coverage and the two-language typography test).
Next: android-game-dev — after the ruling: the Turkish typography, the three fixed surfaces, the two tests; `BundledFontGlyphCoverageTest` adds the Fredoka faces against "Peg Solitaire", 0–9, ":", "/", "—"; no string changes.
Next: qa-engineer — §8 C-2, C-2a–C-2d and the C-7 clause on a build of the branch, in Turkish and in English (C-2c), at 100 % and 200 %; seed the ladder to level 111 for the C-2a/C-2b discriminators; screenshots of Settings, the restart dialog, Results and the status strip in both languages for the founder's Q-038 look.

---

# Addendum (same day) — Q-038 rejected, Q-039 adopted: Fredoka + Turkish composites; D-16 → D-17
Date: 2026-11-01
Branch: feature/turkish-locale (committed by the producer)
Skills loaded: as above (no new skill; the same rule set)
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
- The founder rejected D-16 (Q-038: "change the font for all languages") and chose, on the research-analyst's specimen, **Fredoka Medium and SemiBold with Ğ ğ İ Ş ş composed from the family's own letters and marks, in every language** (Q-039). The tech-lead's contract is `docs/tech-design.md` §11.1 "Q-039". The spec now carries **D-17**; D-16 is marked superseded and its options are kept as the record.
- **Copy delivered** (the developer was waiting): `licenses_fredoka_modified`, en and tr, and the PSL date confirmed as the prose "15 February 2023". The line is a sentence, so it is **translatable** with a `values-tr` row — the tech-design's "`translatable="false"` like its siblings" fits the credit lines, not a sentence; lint's MissingTranslation would also demand the row.
- QA's criteria are rewritten for Fredoka in both languages: C-2a (heading face is Fredoka, the "Seviye 111" discriminator ≈ 25.6dp), C-2b (the five letters as Fredoka glyphs: cedilla as ç's, dot as ö/ü's, breve clear of the g, spacing as the base's; screenshots for this role's read against the specimen), C-2c (English pixel-identical to the pre-branch screenshots), C-2d (a Turkish name in Fredoka on both devices), new **C-2e** (the licences line and the PSL date). C-7's taller-ink clause and §6's face-width note are withdrawn.

## The copy (verbatim; XML escaping `\'`)
- `licenses_fredoka_modified`, **en** (`values/strings.xml`, translatable): `Modified for Peg Solitaire under the SIL Open Font License: Ğ ğ İ Ş ş added from Fredoka\'s own letters and marks; nothing else changed.`
- `licenses_fredoka_modified`, **tr** (`values-tr/strings.xml`): `Peg Solitaire için SIL Open Font License kapsamında değiştirildi: Ğ ğ İ Ş ş harfleri Fredoka\'nın kendi harf ve işaretlerinden eklendi; başka hiçbir şey değiştirilmedi.` — descriptive, no person (§2.1: a line that describes takes no register); the licence name stays as its owner writes it and takes no suffix ("kapsamında" is a postposition); "Fredoka'nın" carries the apostrophe §2.4 requires.
- `licenses_psl_parts`, **en**: `The public-suffix data compiled into OkHttp, as it stood on 15 February 2023` — **confirmed**, no reword: it matches the Turkish ("15 Şubat 2023 tarihindeki hâliyle") and the spec's §2.3 date rule, and a prose date is whole without any hyphen, so the string's base-file comment about U+2011 (`strings.xml:502–504`) goes with the change (developer).

## Outputs (this addendum)
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux/localisation-tr.md` — §1: D-13 pointed at D-17, D-16 marked superseded, **D-17** added; §5.4: new row `licenses_fredoka_modified` (en + tr, translatable), `licenses_psl_parts` English to the prose date with the reason; §5.6 count (180 strings); §6: the `challenge_tile_title` slot back to "Fredoka title", the face-width note withdrawn; **§7.3**: the decision rewritten under D-17 (the five letters as Fredoka's own parts; the Modified Version; the recipe; the four things this role holds the glyphs to; the options kept as the record with their outcomes; the build pointer to tech-design §11.1 and the four spec-side requirements); §7.6: the derived coverage test with no allowance list and the recipe test; §8: C-2a–C-2d rewritten, **C-2e** added, C-7's clause withdrawn; §9.1: Q-038 and Q-039 answered; §9.2: release-manager, research-analyst, tech-lead, android-game-dev items; §11 row.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux-spec.md` — §6.3: the D-16 paragraph replaced by the D-17 paragraph; §9 font row: Modified Version, recipe, unchanged OFL file, the licences line.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ui/DESIGN-SYSTEM.md` — §Typography: the D-16 note replaced by the D-17 note.
- `/Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux/licences-screen.md` — §3.2 table: the Fredoka row's third line; the Public Suffix List row's prose date; a third-edition note under the table; §3.2.1 mapping row; §6 key table: `licenses_fredoka_modified` and the changed `licenses_psl_parts`.
- This addendum. No code, resource, string, font or `THIRD_PARTY.md` edit (standing rule 4): the two strings are the developer's to write from §5.4; the `THIRD_PARTY.md` row is the research-analyst's and release-manager's.

## Checks
No Bash; verified by reading:
- `docs/tech-design.md` §11.1 "Q-039" (1)–(4), "What falls away", "The developer's contract" (lines 1603–1759) and `turkish-locale/tech-lead-rulings.md` addendum — the licences line's key name, its place (a third `LicenseLine.Plain` after the licence name), the substance it must carry, and §16 #41's prose date; the copy above carries that substance in plain words ("letters and marks" for "composite glyphs") — pass.
- `app/src/main/res/values/strings.xml:536–539, 573–576` — the Fredoka entry's two existing lines are `translatable="false"` names; `licenses_psl_parts` (`:575`) is translatable prose with the U+2011 date and the comment at `:502–504` — so the new line follows the prose lines (translatable), not the names — pass.
- `docs/ux/licences-screen.md` §3.2 rows 651 and 665, §3.2.1 row 701, §6 key table — the only places that state the Fredoka entry's lines and the PSL date; each now carries the change — pass.
- The Turkish line against the spec's own rules: §2.1 register (descriptive, no person), §2.2 casing (sentence case; İ Ş Ğ as typed), §2.4 apostrophes and the licence name unsuffixed, §3 glossary ("yazı tipi" not needed; "harf", "işaret" plain words), §4 proper names ("Peg Solitaire", "Fredoka", "SIL Open Font License" as their owners write them) — pass. `UserFacingTextHasNoProtectedNamesTest` scans `values-tr`: "Peg Solitaire" and "Fredoka" are the app's own names, already present in the Turkish copy (`licenses_intro`, the entry title) — no new exposure.

## Open issues / risks
- **`translatable` on the new line.** This role rules it translatable (a sentence, shown to a Turkish player in Turkish); the tech-design's parenthetical says `translatable="false"`. The developer follows the spec's §5.4 row (the strings are the ux-designer's); the tech-lead's sign-off reads it there. If the tech-lead has a reason this role does not see (a test that pins the Fredoka entry's lines as untranslatable), it says so and the Turkish row moves to a note — the copy stands either way.
- **The five glyphs are judged on the device, by this role, from QA's C-2b screenshots** against the specimen the founder saw. The composition rules of §7.3 (cedilla as ç's, dot as ö/ü's, breve clear of the base, spacing as the base's) are the read; the research-analyst's placement table is the build. A mark that reads wrong is a recipe-offset change, reviewed like any change to the recipe.
- **Nothing of D-16 was built** (tech-lead, `git log`), so nothing is removed; the spec keeps D-16's options as the record only.
- **`docs/ux-spec.md`'s brush name drift** (`ctaGradientBrush()` vs the code's `titleGradientBrush()`) stays noted, untouched.

## Handoff
Next: producer — commit the four `docs/` files and this report on `feature/turkish-locale`; hand the developer the two §5.4 rows (`licenses_fredoka_modified` en + tr, translatable; `licenses_psl_parts` English prose date) with tech-design §11.1's contract; record Q-038 as answered and Q-039 in `FOUNDER.md`; the `THIRD_PARTY.md` row to the research-analyst and release-manager.
Next: android-game-dev — the two strings exactly as §5.4 writes them (`\'` escaped), the catalogue's third `Plain` line, `LicenseCatalogTest.EXPECTED`, and the base-file comment at `strings.xml:502–504` removed with the date change; no other string.
Next: qa-engineer — §8 C-2, C-2a–C-2e on a build with the rebuilt fonts: Turkish at 100 % and 200 %, light and dark; English C-2c against the pre-branch screenshots at the same window, scale and theme; the ladder seeded to level 111; one screenshot per D-17 word at each scale under `.planning/qa/turkish-locale/` for the ux-designer's read of the five letters against the specimen.
Next: ux-designer (review mode, a fresh dispatch) — read QA's C-2b screenshots against §7.3's four points; PASS/FAIL per letter and weight.
