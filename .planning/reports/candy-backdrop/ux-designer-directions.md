# ux-designer report — candy-backdrop: backdrop directions for the founder (Q-021)
Date: 2026-10-18
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; plugin rules v1.0.0 read in full because the product snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; product `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill, artifact-design
Ran in parallel with: phase-15 tech-lead sign-off, phase-14 R-1 qa-engineer review
Ran on: claude-opus-5-5 at effort xhigh (deviation: the pinned model is fable; Opus by the founder's answer to Q-019, option 3, 2026-10-17)
Fan-out: none (one haiku Explore helper listed the 16 founder screenshot file names, which this role's tools cannot list; rules/dispatch.md)
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Staffing deviation: this step ran on Opus instead of the pinned Fable, per the founder's Q-019 answer (option 3).
The founder reopened the backdrop in Q-020 with "aliveness, not a copy; glassy, with something in the back". The design board shows three directions: **A · Frosted glass**, **B · Deep gloss** and **C · Airy daylight**. Each is drawn as Home and the game screen, in light and dark: 12 animated 360×780 frames, plus today's Home for comparison. Motion stops under `prefers-reduced-motion` and with a toggle on the page.
Recommendation: **A**. Q-021 is drafted below for the producer to queue (`Blocks: candy-backdrop build`).
`docs/ux/candy-backdrop.md` and `docs/ux/colour-vision.md` are not edited. Which parts of them each direction keeps or replaces is mapped below.

## Outputs
- `docs/ux/backdrop-directions.html`: the design board, about 70 KB, with no script. Its stylesheet comes only from Google Fonts (Fredoka, Nunito). Every picture is inline CSS or SVG (one shared sprite of the two boards and the icons), with no raster. Every colour is a custom property on `:root`; page colours have dark values under `@media (prefers-color-scheme: dark) :root:not([data-theme="light"])` and again under `:root[data-theme="dark"]`, and `body` has an explicit background. The `<title>` is "Peg Solitaire Backdrops". At phone width the frames scale to the 16px side gutter, with no horizontal scroll.
- `.planning/reports/candy-backdrop/ux-designer-directions.md`: this report.

## What the board shows
Every direction keeps these unchanged: the board (pegs, holes, selection ring and move dots on the **opaque** `surfaceContainerHigh` slab), the phase-15 layout, text sizes and controls, violet leading with gold reserved, and in-house drawing with no asset. The first frame is a plain gradient. A **calm band** at the top of every screen holds the backdrop at today's background, so the uncontained titles (the Home title and the game strip title) read exactly as today. The title uses colour-vision.md's TG-01 gradient in all three; today's frame keeps today's CTA gradient.

| | A · Frosted glass | B · Deep gloss | C · Airy daylight |
|---|---|---|---|
| Behind the screen | Gradient plus three large soft pools of violet, pink and lilac light (radial gradients) | Deep violet gradient, two glossy waves of colour low on the screen, nine soft "bokeh" lights rising | Pale sky gradient, two wide swaying ribbons of lilac and pink light, seven soap bubbles with shimmering rims |
| Panels | Frosted glass: white 80% (light) or the background at 72% (dark), 1dp light rim, clay shadow; the chip and tiles keep their tints as tinted glass | Opaque with a wet gloss highlight and lip; the **mode tiles become gel** (violet and pink gradients, white text, shine lip) | White glass at 86% with soft pink and violet shadows; the chip and tiles keep their fills |
| Motion | Pools drift on 21–29 s loops (2 pools, 33–39 s on the game screen); a glint crosses each panel every 9 s | Waves bob on 9 s and 12 s; lights rise on 22–40 s loops (5 on the game screen); a sheen crosses the tiles every 10 s | Ribbons sway on 17 s and 21 s; bubbles rise on 19–30 s loops (1 ribbon and 4 bubbles on the game screen) |
| Compose cost | One Canvas: gradient plus 3 radial fills, moved in the draw phase only (about today's two-blob cost plus one); no blur pass | One Canvas: gradient, 2 Paths built once per size, about 9 radial fills, one clock; gloss overlays drawn with gradients (no inset shadow in Compose) | One Canvas: gradient, 2 rotated ellipses, 7 bubble rings plus dots, one clock; translucent panels with two shadows |
| Change from today | Moderate (finish only) | Large (tile fills, a pinker light theme, a darker feel) | Small |

No direction uses a blur. Compose has no backdrop blur, `RenderEffect` is API 31 while the app supports minSdk 24 (Q-018), and over soft gradients a blur changes almost nothing. The mock therefore shows what would ship.

## Readability, computed
Method: WCAG 2.x relative luminance; translucent layers composited in gamma-encoded sRGB, as Compose's default alpha draw does. Each figure is the worst case over the brightest backdrop overlap. The device reading governs once a direction is built (BD-12, BD-13).

- **A, light.** The brightest pool overlap measures L 0.418. A white-80% pill over it composites to L 0.856, where `onSurfaceVariant` reads **5.36:1**, `onSurface` 11.3:1 and `primary` 4.92:1. On the 72%-tinted glass, Classic and Challenge tile text reads 9.0:1 and 6.8:1. On the 82% glass, the Continue chip reads 6.0:1.
- **A, dark.** The brightest overlap is magenta plus violet, (109,38,148), L 0.068. Under the 72% glass the panel composites to L 0.0187, where `onSurfaceVariant` reads **6.03:1**. Tiles read 7.6:1 and 7.8:1; the chip reads 8.4:1.
- **B.** Panels are opaque under the gloss, and text sits where the gloss has faded to 2% or less. White text on the gel tiles reads **5.03:1** on violet and **4.76:1** on pink at the title's top edge (24% of tile height). The supporting line reads 5.7:1 or more. The gel lip ends at 22dp, before the title.
- **C, light.** White-86% glass over the darkest ribbon overlap (L 0.625) composites to L 0.94: `onSurfaceVariant` reads **5.86:1**.
- **C, dark.** Glass composites to L 0.0256 over the brightest ribbon: **5.48:1**.
- **Dark wrapped title** (colour-vision.md §4.4's bound: no backdrop colour brighter than L 0.0786 above 0.47 H on dark Home):
  - A's pools reach at most 0.068.
  - B's wave crests start at 0.49 H, and its lights are tuned so their rims stay at L 0.044 or less.
  - C's ribbons reach 0.029, its bubble rims 0.057 or less, and the highlight dot 0.034.
- **Slab edge.** Every direction adds a 1dp inner rim to the board slab and the stage art (white 90% in light, white 16% in dark). It sits inside BD-17's 2dp inset.
  - Light: the fill or the rim keeps the edge at **1.07:1 or more** everywhere (today 1.05:1); A's pools raise it to 1.91:1.
  - Dark: the rim composites to L 0.0736, and the stronger of fill and rim is never under **1.28:1** (today 1.26:1). The worst point is a backdrop at L 0.046.
- **Dock (criterion 21f).** On A's glass dock the On disc reads 4.9:1 (light) and 5.6:1 (dark), above the 3:1 it needs.
- **Hue arc.** Every new backdrop colour lies in §4.1's arc of 250°–335°. No gold, blue, red or green is added.

Accessibility (android-accessibility, rules/compose.md): the backdrop layers stay decorative (no description, not focusable, no pointer input; B-08). Touch targets and the TalkBack order do not change. Animator scale 0 draws every layer at rest. No direction encodes meaning in a backdrop hue.

## docs/ux/candy-backdrop.md: keep or replace, per direction
| Part | A | B | C |
|---|---|---|---|
| B-01 one backdrop, three densities | keep (Home full; game fewer and slower; Sparse screens get the gradient wash alone, to be specified) | keep | keep |
| B-02 Canvas from tokens, no raster or sourced art | keep | keep | keep |
| B-03 zone T / zone S | keep as the calm band; **add the glass rule** (text on a translucent panel ≥ 4.5:1 against the panel composited over the brightest colour beneath it) | keep (panels opaque) | keep, plus the glass rule |
| B-04 violet leads; no gold, blue, red or green | keep | keep | keep |
| B-05 only the orbs move; no other motion | **replace** with a new motion rule: slow loops (≥ 9 s), transform and alpha only, one clock per screen, at rest at animator scale 0, game screen slower and sparser, still no parallax or scroll-linked motion, nothing on the board | replace (same rule) | replace (same rule) |
| B-06 hero objects untouched and opaque | slab: keep (adds the rim); other containers: **replace finish** (glass) | slab: keep (adds rim and lip); containers: replace finish (gloss); **tile fills replaced** (gel) | slab: keep (adds rim); pills, gear and dock: replace finish; tile and chip fills kept |
| B-07 no per-mode tint · B-08 no semantics · B-09 transparent containers and insets · B-13 continuous sky · B-15 RTL mirror | keep | keep | keep |
| B-10 no "candy" or "sweet" in identifiers | keep, and also avoid "jelly" (King publishes *Candy Crush Jelly Saga*) | keep, same | keep, same |
| B-11 scene off the first frame | keep (the first frame draws gradient plus pools at rest, in place of today's two orbs) | keep (gradient only) | keep (gradient only) |
| B-12 stage wings with standing sweets | **drop** | drop | drop |
| B-14 dark night scene with luminance bands (≤ 0.0095 or ≥ 0.165) | **replace** with the slab rim rule (fill or rim ≥ 1.28:1); keep the 0.0786 dark-title bound | replace, same | replace, same |
| B-16 and Q-020's game-screen options | superseded by Q-021 | same | same |
| §3 scene (layers, motifs, compositions, sprinkles) | **replace**: gradient, 3 pools, glint | replace: gradient, 2 waves, 9 lights, gloss, gel | replace: gradient, 2 ribbons, 7 bubbles |
| §4.1 colour tokens | replace the values (the board's `:root` A set); keep the constraints (zone T L ≥ 0.779 / ≤ 0.0357, SkyTop = background, hue arc); dark bands per B-14 above | replace values (B set) | replace values (C set) |
| §4.2 dimens · §4.3 BackdropMetrics | replace (motif tokens drop; same discipline: constant lists, no `Random`) | replace | replace |
| §5 legibility method (§5.1–§5.5) | keep, with the numbers re-derived as above | keep | keep |
| §6 motion | replace (see B-05) | replace | replace |
| §7 cold start, 15e on the branch, degradation path | keep | keep | keep |
| §8 fit with the phase-15 layouts | keep (no layout change); drop §8.1's wing bullets | keep, same | keep, same |
| §9 assets (in-house, no file) | keep; the THIRD_PARTY.md idea row's wording needs updating (open issue 4) | keep, same | keep, same |
| §10 verdict checklist | keep (most shape rows become trivially met) | keep; add gloss and sheen to BD-03 | keep |
| §11 criteria | keep BD-01, 02, 04–06, 08 (dark-band clause → rim rule), 09, 12–15, 17–26; rewrite BD-03, 07, 10, 11 and 16 for the new layers and motion | same | same |
| §13 considered and rejected | reverse "parallax, drifting clouds … motion without meaning" (founder's aliveness ask; still no parallax); keep "no frosted panel under the board" | same | same |
| §14 open items | item 1 superseded by Q-021; items 2 and 3 handled in colour-vision.md; 4–8 stand | same | same |

## docs/ux/colour-vision.md: keep or replace, per direction
All three directions keep the whole document: the colour-blind palette (CV-01 to CV-13; the marks live inside the opaque slab, so CV-12 still holds) and the title fix (TG-01, TG-02). One change in all three: **TG-AC04's target** moves from "the far mounds' frosting lips" to "the direction's dark backdrop above 0.47 H", which each direction already meets (figures above). §10 item 1 becomes moot under any of them. Under B, §2's "mode tiles: nothing missing" still holds (labelled, ≥ 4.7:1); the gloss on the dock discs applies to every look alike, so criteria 21a–21g still separate the looks.

## Drafted founder question (for the producer to queue in FOUNDER.md)

    ## Q-021 — Which look should the menu and game backgrounds take?   (open)
    Date: 2026-10-18    Asked by: ux-designer
    Context: You asked for Candy Crush's aliveness, not a copy: glassy, with something in the back. Three directions are drawn and
    animated (Home and game, light and dark) in docs/ux/backdrop-directions.html. The board, layout and colours you kept stay as they are in all three.
    Options: 1) A · Frosted glass: frosted panels over slow pools of violet and pink light, with a glint crossing the glass  2) B · Deep gloss:
    a deep violet world with glossy waves, rising soft lights and gel mode cards (the biggest change)  3) C · Airy daylight: pale sky, swaying
    light ribbons, floating bubbles (the smallest change)  4) None of these: tell us what to adjust
    Recommendation: ux-designer picks 1. It answers "glassy with something in the back" most directly, feels alive on both screens, keeps the
    cards you chose to keep, and is the cheapest to build and run. 2 is the boldest but restyles the mode cards and darkens the app; 3 is calm but barely glassy.
    Blocks: candy-backdrop build
    Answer:

## Checks
- `Edit` of the 11 placeholders in `docs/ux/backdrop-directions.html` (`@tokens-A/B/C`, `@css-A/B/C`, `@page-pick`, `@section-A/B/C/pick`): pass (each matched once and was replaced)
- `Read docs/ux/backdrop-directions.html` lines 1–80: pass (tokens on bare `:root`, page dark values in both guarded blocks, no literal-only theme colour)
- Width budget at 360dp, Challenge tile (icon 44 + level pill + bolt): fail at first (302dp against 294dp of room), fixed by a 44dp bolt and a 13sp nowrap pill (284dp)
- Contrast and luminance figures above, computed by hand with the WCAG formula and sRGB compositing: pass (every text figure ≥ 4.5:1, the slab edge ≥ today's in both themes, the dark-title bound met)
- Two-question test (verdict §2), this role's pre-check on every frame: pass for A, B and C (Q1 no: no element traces to a King design; Q2 no: a peg cross on a solid slab, no tile grid, no candy, no character). The binding answer belongs to the reviewers (BD-02).
- Browser render of the board: not run (this role has no Bash and no preview tool); see open issue 5
- `./gradlew check`: not run (design document only; no code or resource changed)

## Open issues / risks
1. **Bokeh lights (B) and soap bubbles (C) are not on the verdict's free lists word for word.** The lists name soft blobs, glossy highlights, specular dots and translucent panels, which cover all of A. Before B or C is built, the research-analyst should confirm them in one line through the producer. This does not block the founder's pick.
2. **Battery.** All three move continuously on Home (A costs about what today's two blobs cost) and on the game screen. That is new: today's game screen draws no idle frames. The tech-lead's pending O-B ruling applies. Options if the device test shows a cost: game-screen motion that pauses after some seconds without input, or motion on Home only. A QA device check is needed.
3. **The word "jelly"** is kept out of the board's names and copy, because King publishes *Candy Crush Jelly Saga*. It is not on the verdict's list; anyone who wants the word goes to the research-analyst.
4. **The THIRD_PARTY.md idea row** (verdict §6) describes a "candy-themed backdrop … sweets landscape". After the pick, its "What was taken" cell should describe the chosen look. Owners: the research-analyst for the text, the release-manager for the file.
5. **The board was not seen rendered by this role.** It uses container-query units (Chrome 105+, Safari 16+, Firefox 110+), an SVG `<use>` sprite themed through CSS custom properties, and `background-clip:text`. The producer, or the publish preview, should look at it once before the founder does.
6. **Sparse screens are not drawn** (tutorial, catalogue, Results, Settings, Licences). After the pick, the spec gives them the direction's gradient wash with no moving element (B-01 kept).
7. **Q-004 kept the ribbon, cards and style.** A and C keep the fills and change only the finish; B changes the tile fills. This is said on the board so the founder decides it knowingly.

## Handoff
Next: producer:
1. Open the board once (open issue 5), then commit `docs/ux/backdrop-directions.html` and this report on `feature/candy-backdrop`.
2. Queue Q-021 above in FOUNDER.md and show the founder the board (as the private page if it is published).
3. Route open issue 1 to the research-analyst and open issue 2 to the tech-lead, both non-blocking.
4. After the answer: dispatch the ux-designer to rewrite `docs/ux/candy-backdrop.md` for the chosen direction, per the keep/replace table, and to amend colour-vision.md TG-AC04's target. Then dispatch the android-game-dev to build it.
