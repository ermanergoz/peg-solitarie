---
role: ux-designer
task: candy-backdrop
slug: scene-backdrop
status: draft
created: 2026-10-17
revised: 2026-10-18 (third edition: direction B "Deep gloss", the founder's answer to Q-021); 2026-10-31 (B-19a, BD-19 amended); 2026-11-01 (B-22a, the sheen at a settle; BD-01's grep scope); 2026-11-01 (B-09b, BD-09's status-strip rule)
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
ip-verdict: .planning/reports/candy-backdrop/research-analyst.md (inspiration-only)
direction-board: docs/ux/backdrop-directions.html, section #b
layout-contract: docs/ux-spec.md (phase 15, D-03l, §4.2.4, §4.3.1, §4.4.1, §6.5, §10, §11, §12)
architecture: .planning/reports/candy-backdrop/tech-lead-architecture.md (R1–R6, D1–D11; §15 here)
companions: docs/ux/licences-screen.md, docs/ux/colour-vision.md
---

# Peg Solitaire — Backdrop: Deep gloss (direction B)

> **The founder's words.** First request (2026-10-17): *"Also for the game and the menu background
> to be like candy crush game."* On the sweets landscape this document first specified (Q-020,
> 2026-10-17): *"I wanted you to get inspired by the aliveness of candy crush not to copy it. galssy
> look with something in the back maybe?"* On the three directions drawn for him (Q-021,
> 2026-10-18): **"B · Deep gloss"**, chosen knowing it is the biggest change (gel mode cards, a
> darker and pinker feel).
>
> **What direction B is.** A deep violet world behind Home and the game screen:
> - a vertical gradient that deepens from today's background into violet and pink;
> - two glossy waves of colour low on the screen, each with a bright lip and a soft sheen, bobbing slowly;
> - soft out-of-focus lights rising through it and fading;
> - a wet-gloss finish on the clay chips and pills;
> - the two mode tiles turned into **gel cards**: deep violet and pink gradients with white text and a shine across the top.
>
> The board, its slab, the phase-15 layout, every text size and every control stay as they are. The
> research-analyst's verdict still binds (`.planning/reports/candy-backdrop/research-analyst.md`,
> inspiration-only): an idea is free, King's names, candies, characters, map and art are not.
> Direction B has no confectionery motif at all. Its waves, lights, gloss and gel are generic finishes
> pending the research-analyst's one-line confirmation (§14 item 1). This document assumes that
> confirmation. It is a design document. It contains no code; the stack's developer builds it.
>
> **This is the third edition.** The sweets landscape of the first two editions (2026-10-17) is
> superseded. Its decisions stay in §1 as a dated record. Its replaced sections are listed in §16 and
> kept whole in this file's git history on `feature/candy-backdrop`.

## 0. Scope, inputs and the rules that bind this document

**What this specifies.**
- One decorative backdrop, drawn in Compose `Canvas` from `Color.kt` tokens, at three densities, on
  every screen of the app (§2).
- Its layers, elements and composition as measurable values (§3.1–§3.4).
- The finish the founder's pick gives the containers on Home and the game screen (§3.5): a rim and a
  lip on every hero container, a gloss on the chips and pills, and gel fills on the two mode tiles.
- The colour and dimension tokens in light and dark (§4) and the legibility rules (§5).
- The motion model, reduced motion, and the battery default proposed for the tech-lead (§6).
- The cold-start budget (§7) and how all of it sits inside the phase-15 layouts (§8).
- The acceptance criteria a code-auditor and a qa-engineer check on a device (§11).

The in-app licences surface is a separate document, `docs/ux/licences-screen.md`. The colour-blind
palette and the Home title gradient are `docs/ux/colour-vision.md`, which this edition amends where the
backdrop changed (its §11).

**Inputs read for this edition.**
- The board, section #b, every token and keyframe of it: `docs/ux/backdrop-directions.html`.
- `.planning/reports/candy-backdrop/ux-designer-directions.md`: its keep/replace tables are this
  edition's plan, and §16 maps them.
- The tech-lead's ruling, `.planning/reports/candy-backdrop/tech-lead-architecture.md` (R1–R6, D1–D11).
- The second edition of this file and `docs/ux/colour-vision.md`, both in full.
- On `feature/phase-15-ux-refresh` (worktree `PegSolitarie-p15`):
  - `ui/theme/Color.kt`, `res/values/dimens.xml`;
  - `ui/home/HomeScreen.kt` (the mode tiles and Continue chips: `ModeTile`, `ResumeTile`);
  - `ui/home/HomeProgressRibbon.kt` (the pills), `ui/game/GameStatusStrip.kt` (the back chip and the timer pill);
  - `ui/theme/Clay.kt` (`clayShadow`);
  - the phase-15 criteria 15g–15r and 21a–21g in `docs/ux-spec.md`;
  - the test sources: no phase-15 test asserts a mode tile's or Continue chip's colour, and the dock's `DockOnMarkAndSlashTest` asserts the discs' fills (21e).
- FOUNDER.md Q-004, Q-016, Q-018, Q-019, Q-020; the Q-021 answer as the producer's dispatch states it
  (§14 item 5).
- rules/compose.md, rules/ip-policy.md, rules/puzzle-design.md, rules/kotlin-android.md.

**Three rules bind every line below, and every reviewer applies them literally.**

1. **The two-question test** (verdict §2). For any screenshot of the app with the backdrop: *(1) Could
   any single element be traced to, or recognised as, a King design? (2) Would a player shown one
   screenshot think it is Candy Crush, a Candy Crush spin-off, or a reskin of it?* Fail either one and
   the art is out. "It is only gloss" is not the test, and "it is inspired by Candy Crush" is not a
   defence.
2. **The do / don't list** (verdict §3): words (§3a), shapes and compositions (§3b), palette (§3c), store
   assets (§3d). §10 restates it as a checklist against this design.
3. **The studio's own gates are unchanged by any of this** (rules/compose.md):
   - text contrast ≥ 4.5:1 in both themes, and never colour alone;
   - touch targets unchanged, and reduced motion honoured;
   - every dp a `dimens.xml` token, every colour a `Color.kt` token, every ratio and duration in a scoped constants object;
   - light and dark `@Preview`s, and a Compose UI test per screen's main flow.

**What this does not touch.**
- No rule, footprint, score, generator, string, control, touch target, readout, oracle (`docs/ux-spec.md`
  §4.2.1), sound or haptic changes.
- No layout changes: every position, size, gap and band phase 15 set keeps its value (§8).
- The board is drawn exactly as today, inside its opaque clay slab: pegs, holes, legal-target marks,
  the selection ring, the hint, the jump.
- The Home stage art's interior is unchanged.
- No logo, mascot, character, motif or title treatment is added anywhere.

**What the founder's pick does change, knowingly** (B-17):
- the Classic and Challenge tiles' fills: gel, white text;
- the finish of the clay chips, pills and slabs on Home and the game screen: a rim, a lip, a gloss on
  the chips and pills, and a coloured glow in place of the clay shadow's tint;
- a darker, pinker world behind both screens.

Sparse screens (tutorial, catalogue, Results, Settings, Licences) keep today's finish and get a static
wash (B-23).

## 1. Decisions

House style per `docs/ux-spec.md` §3: `- **B-nn:** <ruling> — <rationale>.` Never rewritten in
place; a later ruling is `B-nna` with a pointer. §1.1 holds the 2026-10-17 decisions word for word.
§1.2 holds this edition's rulings, which govern wherever the two differ.

**Status of every 2026-10-17 decision under direction B**

| Decision | Status (2026-10-18) | Where it now lives |
|---|---|---|
| B-01 One backdrop, three densities, every screen | **kept** | §2 |
| B-02 Canvas from tokens; no drawable, raster, AI or sourced art | **kept** | §3, §9 |
| B-03 Text only on zone T; saturation in zone S | **kept**; the calm band is its zone T at the top of Home and the game screen | B-20, §5.1 |
| B-04 Violet leads; pink seconds; no gold, blue, red, green | **kept**; the creams are gone (B has no cream token) | §4.1 |
| B-05 Only the orbs move; nothing else ever moves | **superseded** by B-05a; the orbs retire | B-05a, §6 |
| B-06 Hero objects untouched and opaque | **superseded in part** by B-06a: opacity and the slab's interior kept, the finish changed, the tile fills replaced | B-06a, §3.5 |
| B-07 No per-mode tint | **kept** | §2 |
| B-08 The backdrop announces nothing, takes no focus, adds no string | **kept**; its test keys name the new layers | §11 BD-15 |
| B-09 Transparent containers; insets as today | **kept**, amended by B-09a (the host `Surface` the app lacked, D1) and B-09b (2026-11-01: the status strip is `background` on every screen) | B-09a, B-09b |
| B-10 No "candy" or "sweet" in new identifiers | **kept**, extended by B-10a ("jelly") | B-10a |
| B-11 The scene is off Home's first frame | **kept**, clarified by B-11a (what frame 1 now holds) | B-11a, §7 |
| B-12 Standing sweets in the stage's wings | **void**, withdrawn by B-12a | B-12a |
| B-13 The sky is continuous across navigation | **kept**, amended by B-13a (one 700 ms constant, D2) | B-13a |
| B-14 The dark theme is a night scene in luminance bands | **superseded** by B-14a (the slab-rim rule) | B-14a, §5.2 |
| B-15 The scene mirrors in RTL | **kept** | §5.6 |
| B-16 How much candy the game screen carries (Q-020) | **answered**, superseded by B-16a (Q-021) | B-16a |

### 1.1 The 2026-10-17 record (B-01 to B-16, text unchanged)

Read every line here through the status table above. Section numbers inside these entries point at
the second edition, whose sections §16 lists.

- **B-01: One backdrop, three densities, every screen.** A single `SceneBackdrop` draws the same
  scene family on every screen at one of three densities — **Landscape** (Home), **Quiet**
  (gameplay, Classic and Challenge alike) and **Sparse** (tutorial, Classic catalogue, Results,
  Settings, the Licences screens). — The founder named the menu and the game. A backdrop that
  appears on Home, vanishes on the catalogue and returns on the board would flicker through the
  core flow (Home → catalogue → board → Results) and make the app read as two apps. One sky at
  three densities keeps one world while each screen's hero keeps the eye. §2 gives the per-screen
  reason.

- **B-02: Drawn in Compose `Canvas` from tokens; no vector drawable, no raster, no AI art, no
  sourced art.** Geometry is built from constants once per window size and remembered; colours
  come from `Color.kt` through a palette holder like `ClayColors`; sizes from `dimens.xml`;
  ratios, anchors and counts from a scoped `BackdropMetrics` object. — The verdict's
  recommendation (§4a) and the app's all-vector property (THIRD_PARTY.md §2: no raster anywhere
  in `res`). Between `Canvas` and vector XML, `Canvas` wins on Home specifically: D-03g's hard rule
  for Home's first frame is "no asset load", and a `VectorDrawable` is an XML inflate at first
  use; code-drawn geometry loads nothing. Kenney's CC0 pack is not used, even as a redraw source,
  so no asset row is owed (§9).

- **B-03: Text sits only on text-safe colours ("zone T"); saturation lives where nothing has to
  be read ("zone S").** Every backdrop colour that any uncontained text can overlap — at any
  scroll position, at any font scale up to 200%, in either theme — is a zone-T colour, which reads
  ≥ 4.5:1 under `onSurface`, `onSurfaceVariant` **and** `primary` text (§5.1). Zone-S colours (the
  horizon glow, hills, frosting, motifs, sprinkles) are drawn only where the screen's layout puts no
  uncontained text: below Home's hero, below the gameplay strip. Sparse is zone T from edge to
  edge. — Seven screens put text straight on the background (the gameplay title, the tutorial
  caption and `Skip`, the Settings section headers, the Results headline and metric labels, four
  top-bar titles, the Home title). Moving them onto pills would change five screens the founder has
  approved; a split by zone keeps every one of them as built and still lets the scene be bright
  where it is seen.

- **B-04: Violet leads; pink seconds; cream highlights; no gold, no blue, no red, no green.**
  Motif and hill fills are the two house hues plus cream and one lilac. Gold stays reserved for
  hint and reward semantics (DESIGN-SYSTEM.md: "KEEP reserved"). — The verdict bans King's
  six-candy colour-coded set (§3b) and any eyedropped colour (§3c); two house hues cannot form a
  six-colour set, a backdrop with no gold cannot dilute the star and hint vocabulary, and a scene
  with no blue cannot contain a "blue round lollipop head". Violet staying the lead is what keeps
  the app reading as itself (§3c).

- **B-05: The existing Home blobs become the sky orbs and stay the only moving elements.** The two
  `HomeBlobs` discs keep their colours (`primary`, `secondary`), their 0.10 alpha, their ±16dp /
  8 s linear reverse drift and their animator-scale gate; the secondary orb is re-seated from the
  bottom-start corner to the start edge at 0.32 H (§3.3), and with animations off both are drawn
  **static, at rest**, instead of being omitted. Nothing else in the scene ever moves. No
  parallax, no scroll-linked motion. — §10 of the phase-15 spec fixes "the blobs remain the only
  drifting elements", and the qa-engineer's O-B records that this drift alone keeps idle Home
  rendering at ~61 fps; a second animated layer would double a cost the tech-lead has not yet
  ruled on. Parallax would move only where Home scrolls (narrow phones, large fonts) and would add
  a motion channel reduced-motion users must be able to switch off; it does not pay for either.
  Drawing the orbs static under reduced motion makes the reduced-motion Home the same picture,
  still, rather than a different picture; the re-seat takes the pink orb out from behind the mode
  tiles, where it sits today.

- **B-06: The hero objects are untouched and stay opaque; nothing is drawn through or over them.**
  The board sits on the existing `surfaceContainerHigh` clay slab (large radius, violet-tinted
  soft shadow) — never on a dark translucent grid of tiles, never translucent itself. The
  gameplay density carries **no standing sweet**: sky, two clouds behind the status strip, frosted
  hills with drips and sprinkles under and behind the dock. — The verdict (§3b): "the peg board
  stays the hero object of the gameplay screen, and the Home tiles stay the hero of the menu", and
  "keep the existing clay slab". Pegs and holes live inside an opaque slab, so their
  distinguishability is unchanged by construction (criterion BD-17 proves it pixel for pixel); what
  the backdrop can affect is each container's **edge**, and §5.2 makes every hero edge at rest at
  least as separated as it is today, in both themes.

- **B-07: No per-mode tint.** Classic and Challenge play on the same Quiet backdrop. — Challenge has
  no screen of its own (`GameScreen` with `challengeLevel`); the strip title already says which mode
  is up; a pink-led Challenge backdrop would break B-04's "violet leads" on half the play sessions,
  and the Home tiles' violet/pink coding is a *selection* cue, not a *place* cue.

- **B-08: The backdrop announces nothing, is never tappable or focusable, and adds no string.**
  Its only semantics are a test tag and one test-only property naming the layers it drew
  (§12). — rules/compose.md and `docs/ux-spec.md` §11: decorative elements carry no description;
  a backdrop that took a focus stop would put a nameless node in every screen's TalkBack order. No
  string means the verdict's words rule (§3a) is met by construction on the backdrop side.

- **B-09: Transparent containers over the scene; insets applied exactly as today.** Every screen's
  `Scaffold` takes a transparent container colour, the four M3 `TopAppBar`s (catalogue, Results,
  Settings, Licences) take transparent container colours, and the tutorial's root `Surface` stops
  painting `background`. The backdrop is drawn edge to edge behind them, under both system bars,
  and the content keeps the one inset application it has: the `Scaffold`'s content padding on Home
  and gameplay (criterion 18b), `safeDrawingPadding()` on the tutorial column. The window-filling
  theme `Surface` (rules/compose.md, F-32) stays, so text that names no colour still takes
  `onBackground`. — A `Scaffold` paints `background` opaquely by default, so without this the
  scene is invisible; and a backdrop that respected the insets would leave a flat band of
  `background` under the bars of an edge-to-edge app, the one place a backdrop must not stop.

- **B-10: New identifiers contain neither "candy" nor "sweet".** `SceneBackdrop`, `Backdrop*`
  tokens, `BackdropMetrics`, `BackdropDensity`; motif names (`Lollipop`, `Cane`, `Gumdrop`,
  `Cupcake`, `Sprinkle`, `Frosting`) are fine. — The verdict allows existing internal names
  (`drawCandyPeg`, "Candy Clay" stay), but the words grep (BD-01) is only meaningful if this change
  adds zero new hits, and a test tag or a KDoc that leaks into a crash log is one grep away from a
  user-facing string. The cost is nil.

- **B-11: The scene is off Home's first frame; the first frame costs what a static gradient
  costs.** The sky (one vertical gradient over the window) and the two existing orbs are drawn on
  the first frame; the scene layer is composed from the **second** frame on, with no animation of
  its own. The §6.5 15e sitting still runs on the branch that lands the backdrop. — The dispatch's
  bar is "nothing on the startup critical path that a static gradient would not cost". A gradient
  is one brush and one rect; the scene is some sixty primitives and a handful of paths. Deferring
  it one frame (~16 ms, under the hero's own 240 ms entrance fade) puts it off the measured
  `TotalTime` without any visible difference, and makes the backdrop's first-frame cost exactly the
  bar's. §7 has the budget and the degradation path.

- **B-12: On Home, the standing sweets and the sky sprinkles live in the stage's wings.** The
  stage slot (D-03l) draws, behind its board art, a small frosted hill in each wing — the strip of
  the slot left and right of the art — with the lollipop and the gumdrop on the start one, the cane
  and the cupcake on the end one, and three sprinkles above each, whenever the stage is at least
  `backdrop_wing_min_height` (160dp) tall (§3.3). The window-level Landscape scene carries the sky,
  orbs, clouds, hills, lips, drips and hill sprinkles only. — Anchored to the window, the sweets fell
  behind the ribbon and the Continue chips on the founder's own one-chip phone (their bases at
  0.41–0.46 H, the ribbon from ≈ 0.31 H) and under a title wrapped at a large font scale; anchored
  to the stage, they appear exactly where the slack is and nowhere else — always below the hero row
  and above the lower group by layout, never under text, never behind a card — and they go away
  with the stage on a short screen, as the stage art already does. This also retires the first
  draft's need for a title-wraps flag.

- **B-13: The sky is continuous across navigation.** A destination change never fades or dips the
  sky: the new density's sky and scene cross-fade in over the old one, which stays opaque beneath
  until the new one is fully in, over the navigation transition's own duration; with animations
  off the swap is instant. How this is hosted (one backdrop behind the `NavHost` that reads the
  current destination, or another way) is the tech-lead's; the property is criterion BD-21. — Two
  per-screen backdrops cross-fading over the window background would composite the sky at 75%
  mid-transition, a visible flicker of the horizon on every navigation.

- **B-14: The dark theme is a night scene.** In dark, the sky, clouds and hills are all at or below
  the dark background's luminance (deep violet and plum, separated from each other by hue and by
  their frosting lips), and every accent — frosting, motifs, sprinkles — is clearly brighter than
  every container. — On a dark surface the clay shadow barely shows, so container edges are carried
  by luminance; a mid-tone dark sky would sit at the board slab's own luminance and erase its edge
  (§5.2). Light and dark were designed together (ui-ux-pro-max `dark-mode-pairing`), not inverted.

- **B-15: The scene mirrors in right-to-left layouts.** Every horizontal position is a fraction from
  the start edge; in RTL the whole scene mirrors, as the orbs' `TopEnd`/`BottomStart` alignment
  already does today, and the cane's hook still curves toward the start. — B-12's wing rule and the
  start-aligned title then hold in both directions with one composition.

- **B-16: Gameplay's calm frame is the default; how much candy the game screen carries is the one
  open taste choice.** Quiet (B-06) is specified in full and is what gets built; the report drafts
  Q-NNN offering the founder the full menu scene behind the board, or a plain sky. Each option is
  specified here (§3.3) so the answer is a constant change, not a redesign. — The verdict allows
  sweets behind the board at low contrast, legibility allows them because the slab is opaque, and
  whether a puzzle board wants a busy frame is taste; the default stays closest to the request that
  keeps the board the thing the eye lands on.

### 1.2 This edition's rulings (2026-10-18)

- **B-17: Direction B, "Deep gloss", is the backdrop.** It has three parts:
  - Behind Home and the game screen: a deep violet-to-pink gradient, two glossy waves low on the
    screen, and soft lights rising through it (§3.2).
  - On those two screens' containers: a gloss finish (B-19), and gel fills on the two mode tiles (B-18).
  - Behind every other screen: a static wash (B-23).

  — It is the founder's answer to Q-021 (2026-10-18), given knowing it is the biggest change. The
  board's section #b is the look. This document turns it into values a developer can transcribe, and
  it tunes the board's drawing only where a studio gate required it. Each such change is named where
  it is made, with its reason, and §13 lists them all.

- **B-05a: Slow, periodic motion behind opaque heroes, on one clock** (supersedes B-05). What moves:
  - **Home:** the two waves, nine lights and the sheen on the two gel tiles.
  - **The game screen:** the two waves and five lights.
  - **Sparse screens:** nothing.

  Rules:
  1. Every loop is ≥ 9 s.
  2. Only translation and alpha change: no colour, size, shape or path animates.
  3. One clock per screen drives all of it (B-24).
  4. At animator scale 0 every element is drawn at its rest pose: the same picture, still.
  5. The game screen is slower and sparser than Home: its waves move 1.5 times more slowly, and it has five lights to Home's nine and no sheen.
  6. No parallax, no scroll-linked motion, no tilt or sensor response.
  7. Nothing moves on, in or in front of the board slab: the slab is opaque, and the scene is behind it.
  8. A drifting element moves at ≤ 8dp/s.
  9. The one faster movement is the gel sheen: a glint, once per 10 s loop, ≤ 2.5 s long, inside the tile's lip only.

  **The two Home orbs retire.** `HomeBlobs` is deleted rather than re-seated. The lights take its
  place, and a second slow layer would only double the frame cost.

  — The founder asked for aliveness (Q-020). These rules give it without competing with the board's
  own motion vocabulary:
  - The vocabulary: the selection pulse, the jump slide, the removal fade and the hint glow.
  - Slow periodic motion behind opaque containers reads as atmosphere, not as information.
  - One clock means one frame callback, one place to stop it (B-22), and poses a test can set exactly.

- **B-06a: The hero objects stay opaque, and the board is untouched. Their finish changes, and the
  tiles' fill changes** (supersedes B-06 in part).
  - **Kept from B-06:** the board's slab stays `surfaceContainerHigh`, opaque, never translucent and
    never on a grid of tiles. Its interior inset 2dp is pixel-identical to today (BD-17).
  - **Kept for every container:** nothing in the scene is ever drawn inside, over or through a
    container.
  - **Changed:** the hero containers on Home and the game screen gain the finish of B-19, and the
    two mode tiles become gel (B-18).

  — The verdict's "keep the existing clay slab" holds. The board stays the hero of the game screen,
  and the tiles stay the hero of Home: the tiles become the boldest objects on it.

- **B-09a: The host adds the window-filling `Surface` the app lacks** (amends B-09; the tech-lead's
  D1).
  - There is no theme `Surface` today. `AppNavHost`'s root becomes `Surface(color = background,
    contentColor = onBackground)`, filling the window.
  - Every transparent `Scaffold` also passes `containerColor = Transparent` and `contentColor =
    onBackground` explicitly.
  - The four `TopAppBar`s are transparent in both their resting and scrolled colours.
  - Frame 1 gains one solid fill (§7).

  — Without it, a transparent `Scaffold`'s text that names no colour falls back to black in dark
  mode, which is rules/compose.md's F-32.

- **B-09b (2026-11-01): The status strip is `background` on every screen; BD-09 no longer compares
  it with `develop`** (amends BD-09, §3.3, §5.5 and §8.3; a UX decision on the qa-engineer's Open
  issue 2 in `.planning/reports/candy-backdrop/qa-engineer-device.md`).
  - **The conflict.** BD-09 asked for the status-bar band to be pixel-identical to `develop` on
    every screen. On `develop` the M3 `TopAppBar`s of the catalogue, Results, Settings, Licences and
    Licence text reach under the status bar and paint the band in their own container colour
    (measured on the device: light `#FBF9FF`, dark `#241E31`). B-09, B-09a and BD-14 make those
    bars transparent, resting and scrolled, so on this branch the band shows the wash's flat top,
    `SkyTop` = `background` (light `#F4F1FA`, dark `#1B1626`): at most 9/255 per channel apart, but
    over the whole band. No build can meet both criteria. BD-09's premise ("`SkyTop` equals
    `background`, so the band is unchanged") held only where `develop` already showed `background`
    there: Home outside the old orbs, gameplay and the tutorial.
  - **The ruling.** BD-14 stands, and BD-09 is restated as the property it was meant to protect:
    the strip is one known, flat, text-safe colour on every screen, and the status icons keep their
    appearance and their contrast. The rule, which BD-09 now checks:
    1. **The strip.** On Home, gameplay, the tutorial, the catalogue, Results, Settings, Licences and
       Licence text; in light and dark; at rest and in motion; on a settled screen at the top of its
       scroll: every pixel of the band from the window's top edge to `statusInset` (the top
       `WindowInsets.statusBars` inset), outside the system's own boxes (the clock, notification and
       status icons, and the display cutout), equals the theme's `background`, which is
       `BackdropSkyTop`: light `#F4F1FA`, dark `#1B1626`, within ±1/255 per channel.
    2. **Where it holds.** On every window whose `statusInset` is at most 0.12 H, where the sky is
       flat `SkyTop` on all three densities and no light is visible (B-20). Every QA window meets it:
       the Pixel_9's 54dp of 923dp is 0.059 H. A shorter window, such as a split-screen half, is
       outside the criterion; there the band below 0.12 H is the sky's own blend, still zone T.
    3. **The icons.** The status-bar icon appearance stays the app's current one: dark icons in the
       light theme, light icons in the dark theme. Nothing on this branch sets the system-bar style,
       and `MainActivity` is untouched (the tech-lead's R1). Against the strip, sampled at glyph
       cores: the clock digits ≥ 4.5:1 and every status icon ≥ 3:1 (WCAG 1.4.3 and 1.4.11), in both
       themes.
  - **Expected values** (computed; the device reading governs). Assuming AOSP's single-tone icon
    tints (dark icons 60% black, light icons white):
    - light: 5.57:1 on the strip, against 5.65:1 on `develop`'s top-bar strip; with opaque black
      icons, 18.8:1 against 20.1:1;
    - dark: 17.6:1 on the strip, against 16.1:1 on `develop`'s.
    - Both clear 4.5:1 with margin. Light falls by under 0.1, dark rises.
  - **What this does not change.** No code, no token, no test oracle: the strip already measures
    flat `background` on every screen, in motion too (the qa-engineer's BD-09 row), and by the
    qa-engineer's report the automated half of BD-09 already takes the form "equals `background`".
    If a test instead compares the strip against `develop`, its oracle follows this ruling; that
    is the code-auditor's line to confirm. The status icons' appearance and
    the navigation bar (§5.5) are untouched.

  — Reasons:
  - **B-09's own reason.** A backdrop must not stop under the bars. Painting the strip in the old
    bar colour would put a seam between the strip (`#FBF9FF`) and the transparent bar right under
    it (`#F4F1FA`), a seam `develop` does not have. On `develop` the strip and the bar are one
    surface, and on this branch they are still one surface: the wash's flat top band.
  - **One strip colour across the core flow.** On `develop` the strip changes colour between Home
    and the catalogue. On this branch it never does, which is what B-01 and BD-21 want.
  - **The alternative undoes BD-14.** Opaque top bars again would cut the Sparse wash under every
    title and restore the two-surface look B-23 removed.
  - **Accessibility is unchanged.** The strip stays zone T, the icon appearance stays correct, and
    the icons' contrast moves by less than 0.1 in light and rises in dark.
  - **The difference is in the same tone.** It is at most 9/255 per channel, from one pale lavender to
    another, and from one deep violet to another.
  - **No code change.** The build that is measured is the build that ships, and the final cold-start
    sitting (BD-23 (b), (c)) is not voided.

- **B-10a: New identifiers, KDoc, test tags and strings also avoid "jelly"** (extends B-10). "Gel",
  "Gloss", "Wave", "Light" and "Sheen" are fine. The lights are `BackdropLight` in code, and
  user-facing text names none of these things. — King publishes *Candy Crush Jelly Saga*. The word is
  not on the verdict's list, but avoiding it costs nothing, and anyone who wants it goes to the
  research-analyst.

- **B-11a: What Home's first frame holds** (clarifies B-11; the tech-lead's R3 and D7).
  - **Frame 1:** the window background, the host `Surface`, the L1 sky gradient, and Home's content
    with its containers' own finishes. Those finishes are the gel fills, rims, lips, glosses and glow
    tints: each container's own fill, drawn as part of the container.
  - **From frame 2 on:** the waves, the lights and the clock start. They are composed on the second
    frame, or on the third at the latest, behind the content's existing 240 ms entrance fade.

  — A container finish is one gradient brush in place of one solid colour, and the bar allows a
  gradient. A gel tile that changed from pale to deep on frame 2 would be a visible pop. The waves
  and the lights are the scene, and they keep B-11's deferral.

- **B-12a: Withdrawn: nothing stands anywhere.** No motif is drawn on Home's stage or anywhere else.
  The stage is exactly what D-03l built, plus the rim, lip and glow of B-19 on its art. —
  Direction B has no confectionery motif. The tech-lead's R2 and D3 (the wings' fit and their
  semantics) are void (§15).

- **B-13a: One duration for the sky's cross-fade** (amends B-13; the tech-lead's D2).
  - The cross-fade uses `ClayAnim.NAV_TRANSITION_MS` = 700 ms. That is today's `NavHost` default,
    made explicit on its four transitions.
  - No `BACKDROP_CROSSFADE_MS` exists.
  - During a cross-fade, both densities' scenes read the one clock (B-24).

  — Two constants can drift apart, and one cannot.

- **B-14a: The slab-rim rule** (supersedes B-14). Every opaque hero container on Home and the game
  screen carries a 1dp inner rim:
  - `BackdropPanelRim` on the clay family (white 90% in light, white 16% in dark);
  - `GelRim` (white 30%) on the gel tiles.

  A container's edge is then the stronger of two contrasts: its fill against the colour beside it,
  and its rim against the same colour. That stronger contrast has a floor that holds against **any
  colour whatever**, by arithmetic (§5.2):
  - the clay family, in both themes: at least today's ratio (≥ 1.07:1 in light, ≥ 1.29:1 in dark,
    against today's 1.05:1 and 1.24:1);
  - every light container: at least today's ratio;
  - the dark Continue chips and the dark rescue banner: ≥ 1.20:1 (in fact ≥ 1.25:1 and ≥ 1.26:1,
    against today's 1.90:1 and 1.39:1 on a flat background);
  - the gel tiles: ≥ 1.20:1 (≥ 1.21:1).

  The dark Home title's bound (no colour brighter than L 0.0786 above 0.47 H) is kept; B-21 meets it.

  — B's dark sky rises above the background's luminance, and its dark waves pass through every
  luminance between deep plum and bright violet. A band rule on the backdrop (B-14's) cannot hold
  that look. A rim makes the edge independent of what is behind it. The dark chips' drop is the one
  measurable cost of the founder's pick: still above the 1.02:1 the light Continue chip has today,
  and carried also by hue (amber on violet) and the label.

- **B-16a: Q-021 answers Q-020's open choice.** The game screen carries direction B at Quiet density:
  - the two waves below the slab and behind the dock;
  - five slow lights, above and below the slab;
  - no element beside the pegs.

  Q-020's options 2 and 3 and their constants no longer exist. — The founder chose B as drawn, and
  the board's game frames are Quiet.

- **B-18: The mode tiles are gel** (§3.5.2).
  - **Fill:** a vertical gradient, light at the top and deep below: violet for Classic, pink for
    Challenge. It is the same in both themes.
  - **Content:** white text and glyphs. The badge is white at 22%.
  - **Art:** Classic's footprint is drawn in white. Challenge's bolt is white at 90%.
  - **Edge and light:** a 1dp `GelRim`, a gel lip across the top, a sheen that glides along that lip,
    and a coloured glow in place of the clay shadow.
  - **Pressed:** the pressed state layer darkens the gel instead of lightening it.

  White text sits only from 16dp below the tile's top edge, which is the tile's own content padding.
  The gel is fully deep there, so white reads ≥ 4.90:1 on pink and ≥ 5.72:1 on violet at any font
  scale.

  — The board drew two things this rule changes:
  - Its gradient stayed light down to about a third of the tile. At 200% font a label would sit
    there at about 3.8:1.
  - Its sheen crossed the whole card, under the text, at about 3.1:1.

  The look stays. The light band is shorter, and the glint stays above the words.

- **B-19: The gloss finish on Home and the game screen** (§3.5.1).
  - **The rim:** every clay container gets B-14a's rim.
  - **The lip:** a 2dp highlight inside its top edge.
  - **The glow:** a coloured tint replaces the clay shadow's: pink in light, violet in dark.
  - **The gloss:** a white gradient fading from the top edge, on the chips and pills only.
    - Which: the gear chip, the ribbon pills, the Continue chips, the back chip and the timer pill.
    - Strength: 70% in light, 6% in dark.
  - **No gloss** on the board slab, the stage art, the dock slab or the rescue banner.
  - **Unchanged:** the dock's discs, every dialog and snackbar, and every container on a Sparse screen.

  — Reasons:
  - A gloss over the slab or the stage art would lie over the board.
  - The dock slab and its discs are what criteria 21e and 21f measure: the On disc and the Off ring
    each ≥ 3:1 against the dock slab. Glossed, the On disc's top reads about 2.4:1 against it.
  - At the board's 10%, dark gloss puts the ribbon labels (`onSurfaceVariant`) at 4.10:1. At 6% they
    read ≥ 4.66:1.
  - In light, a white gloss under dark text only raises its contrast.

- **B-19a (2026-10-31): The ribbon pills keep their gloss, and criterion 15m's pixel read admits
  the finish** (amends B-19's reach into phase 15; a UX decision on the conflict in
  `.planning/reports/candy-backdrop/android-game-dev.md`, Open issue 1). Pointer in
  `docs/ux-spec.md`, criterion 15m.
  - **The conflict.** `HomeLayoutTest.theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph`
    (15m) reads every pixel inside each narrow pill's `spacing_sm` padding and outside its texts,
    and expects flat `surfaceContainerHigh`. B-19's gloss covers the top 46% of the pill, inside
    that area. The test reported "pill 0 paints 444 px besides its surface and its texts". No
    build can satisfy both.
  - **The ruling.** Option (b). B-19 stands on every ribbon pill, in both shapes. 15m's
    requirement stands as written: no decorative glyph. Its pixel oracle changes: each read pixel
    is compared against the pill's **finished surface at that pixel**, in the same theme and the
    same run: the fill, the gloss gradient over it, and the rim and lip wherever the read area
    meets them. It is no longer compared against flat `surfaceContainerHigh`. The read area, the
    three pills, the text ink margin and every other assertion of the test are unchanged.
  - **What the amended read must prove** (this role's properties; how it is computed is the
    tech-lead's):
    1. It passes on this branch's compact pills in light and in dark. Today's test runs one
       theme; the amended one runs both, because the gloss differs by theme (70% white in light,
       6% in dark).
    2. **Red, light:** the H-1 audit's M3b mutation (the 20dp glyph put back inline before the
       label, `onSurfaceVariant`) fails it on every pill.
    3. **Red, dark:** the same mutation fails it in dark. The dark gloss is faint and the glyph is
       lighter than the fill, so this is the case a loose tolerance would miss.
    4. **Red, inside the gloss:** a glyph drawn inside the gloss band, the pill's top 46%, fails
       it in both themes. A read that skipped the glossed rows would pass a glyph placed there.
    5. Any tolerance is a named constant. It is wider than today's one level a channel only by
       what the gloss gradient's own rendering needs, and reds 2–4 fail with it in place.
  - **BD-19's carve-out.** BD-19 re-runs phase 15's criteria "with no test edited". This one edit,
    to 15m's pixel oracle (`assertEachPillPaintsOnlyItsSurfaceAndItsTexts`, its tolerance
    constant, and the theme parameter), is the one exception. It needs the code-auditor's line
    that the oracle was amended, not weakened, with reds 2–4 shown failing. Every other phase-15
    test stays unedited.

  — Reasons:
  - **15m is about a glyph, and the gloss is not one.** The flat-fill read stood in for "no glyph"
    because the pill's surface was flat when phase 15 wrote it. B-19 changed the surface, not the
    property. The test's own KDoc gives the reason for the read: a glyph "cannot avoid being
    drawn". A read against the finished surface still catches exactly that.
  - **One finish on Home's middle band.** The gear chip above the ribbon and the Continue chips
    below it are glossed (B-19). A flat ribbon between them would look unfinished, or disabled,
    beside its neighbours. The finish is a recipe per container, so option (a) removes the gloss
    from the full pills too, on every window. Dropping it on the compact shape only would give one
    readout two looks across widths.
  - **Accessibility gives no reason for (a).** In light the gloss only raises the labels'
    contrast. In dark, at 6%, the ribbon labels read ≥ 4.66:1 even at the pill's top edge (§5.3),
    and no text reaches that edge: the compact pill's label starts below its 8dp padding, where
    the gloss is weaker. Neither option changes a node, a target or an announcement.
  - **It is the founder's pick.** The board's ribbon is glossed (Q-021, direction B), and B-19
    names the ribbon pills.

- **B-20: The calm band is made by the composition, not by a veil.** On Home and the game screen the
  band above the calm end (0.14 H) holds the sky alone: `SkyTop` to 0.12 H, then the start of the
  blend to `SkyMid`.
  - Every light's alpha falls to 0 over the last `backdrop_light_calm_fade` (48dp) before its top
    edge reaches 0.14 H.
  - The calm end clears both texts that live there: the Home hero row at rest reaches at most 0.128 H,
    on the Pixel_9, and the game strip at 200% at most 0.1375 H, at 360×640dp.
  - The waves never come near the band.

  The status bar, the Home title at rest and the game strip's title therefore always sit on zone T.

  — The board drew a veil, an extra full-width gradient over the scene on every frame. Building the
  band into the poses costs nothing, and a pose test can check it.

- **B-21: The lights keep to lanes where the title can reach** (§3.3).
  - **Home:**
    - The five lights whose path reaches above `TITLE_REACH` (0.47 H) run in disjoint vertical lanes,
      so no two of them ever overlap.
    - The other four never rise above 0.47 H.
    - Each light alone composites within the title's bounds: L ≥ 0.503 in light, L ≤ 0.0786 in dark.
    - No wave pixel lies above 0.47 H at any pose.
  - **The game screen:**
    - No two lights ever overlap.
    - No light enters the band 0.33–0.59 H, where the Challenge loading spinner sits.

  — Two overlapping lights composite past both bounds: L 0.094 in dark, L 0.44 in light. Lanes keep
  every point of the title's reach to one light at a time, and the board's own layout already put its
  lights at the sides.

- **B-22: The backdrop settles when nobody is using the screen.** This is a **proposal for the
  tech-lead's idle-frame ruling**, specified as the default:
  1. The clock runs only while the screen's lifecycle is RESUMED.
  2. After `BACKDROP_HOME_IDLE_MS` (60 s, Home) or `BACKDROP_GAME_IDLE_MS` (20 s, game screen)
     without input, the clock's rate eases to 0 over `BACKDROP_SETTLE_MS` (2 s). The backdrop then
     requests no frames.
  3. Any pointer-down or key-down on the screen eases the rate back to 1 over the same 2 s. The input
     is observed, never consumed.
  4. In battery saver the backdrop draws its rest pose and runs no clock.

  §6.3 has the detail and the device check.

  — This is the first time the game screen animates continuously. A puzzle player thinks in long
  still stretches, and a phone left on the table buys nothing with motion. Motion that settles when
  you stop, and wakes when you touch, keeps the aliveness for a player who is playing.

- **B-22a (2026-11-01): The gel sheen shows only while the clock runs at full rate** (amends B-22
  and §6.3 step 2; a UX decision on the code-auditor's finding 5 in
  `.planning/reports/candy-backdrop/code-auditor.md`).
  - **The conflict.** A settle stops the clock's time "where it is". About 30% of settles on Home
    begin inside a tile's 2.5 s pass, so the glint froze on the lip and stayed there, against
    B-05a rule 9 (≤ 2.5 s) and §6.1's still picture (no glint).
  - **The ruling.** On the host's live clock, a gel tile draws its sheen only while
    `backdropMotion` is **Running**. While it is **Settling** (the 2 s ease to 0, and the 2 s ease
    back to 1 on a wake) or **AtRest** (settled), no sheen is drawn: a pass under way when the
    settle begins ends at once. After a wake the sheen returns when the clock is Running again; if
    the clock time is then inside a pass, the rest of that pass is drawn. The waves and the lights
    still decelerate and stop where they are, as §6.3 says.
  - **Unchanged.** Animator scale 0 and battery saver already draw no sheen (`isStatic`). A clock
    fixed at a pose for a preview or a pixel test (`FixedBackdropClock`) is not a settled clock: it
    draws the sheen at its pose, as before. How the tile tells the two apart is the developer's.
  - **The test** (BD-16 (c)): with the host's live clock on injected frames, Home's Classic tile
    is brought to a clock time inside its pass while Running (the control: its lip differs from the
    same tile drawn with no sheen), then left idle until the settle begins inside that pass. From
    that frame on, through Settling and AtRest, the tile's lip region is pixel-identical, in the
    same run, to the same tile drawn with `StaticBackdropClock`. With the rule removed, the AtRest
    frame differs (the red).

  — The sheen is the one faster movement (B-05a rule 9). A glint that crawls through a 2 s ease and
  then hangs on the lip for as long as the phone lies there reads as a stuck frame, not as gloss.
  Ending the pass is one condition on a value the tile already reads. Deferring the settle to the
  gap between passes would couple the clock's state machine to one tile's loop for no visible gain.

- **B-23: Sparse screens get a static wash and keep today's containers.**
  - **The wash:** `SkyTop` to 0.12 H, then a blend to `BackdropSparseLow` at the bottom edge.
  - **Nothing else:** no wave, no light, no motion, zero idle frames.
  - **Containers:** today's clay finish, unchanged.
  - **The constraint:** `BackdropSparseLow` is lighter than the background in light (L 0.897 against
    0.890) and darker in dark (L 0.0085 against 0.0095).

  So every Sparse colour is zone T, and every card's edge on a Sparse screen is at least today's by
  construction.

  — These screens put text everywhere and scroll. A wash keeps the world continuous through the core
  flow (B-01) at no legibility cost and no frame cost. In light, a darkening wash would cross the
  cards' own luminance and erase their edges somewhere on the screen, so the wash brightens instead.

- **B-24: One clock, and every pose is a pure function.**
  - **The clock:** one clock per backdrop host, read in the draw phase only. Its time is scaled by the
    animator duration scale, as every Compose animation is.
  - **The poses:** each element's pose (its offset and alpha) is a pure function of the clock time
    and that element's constants, in `BackdropGeometry`'s pure model.
  - **The sheen:** the gel tiles' sheen reads the same clock.
  - **At scale 0:** there is no clock, and the pose is the rest pose.
  - **Settling:** B-22 changes only the clock's rate.

  — A test sets the time and asserts a pose, with no frame clock and no pixel. Stopping one clock
  stops everything. How the clock reaches the tiles (a `CompositionLocal` from the host, or another
  way) is the tech-lead's call.

## 2. Which screens carry the backdrop, and at what density

| Screen | Density | What it carries, and why | Hero object the backdrop defers to | Uncontained text on the backdrop (§5.1 governs) |
|---|---|---|---|---|
| **Home** (the menu) | **Landscape** | The founder's first-named screen, and the board's Home frames. It carries the deep sky, the two waves (crests at 0.49 H and 0.62 H, behind the lower group), nine lights (five in lanes beside the stage, four low among the waves), gel tiles and glossed chips and pills. The scene shows in the hero band, beside the stage art, in the 8–16dp band gaps and the side margins, and in the footer above the navigation bar. | The two gel mode tiles; then the stage art, the Continue chips and the ribbon | the title only (`displaySmall`, `TitleGradient`, colour-vision.md TG-01) |
| **Gameplay** (Classic and Challenge, one screen: B-07) | **Quiet** | The founder's second-named screen, and the board's game frames. It carries the same deep sky, two waves below the slab and behind the dock (crests at 0.72 H and 0.80 H), five slower lights (three between the strip and the slab top, two below the slab foot), and glossed strip chips. Nothing lies beside the pegs, because the slab is opaque and the scene is behind it. | The board slab; then the dock and the strip's back chip and timer pill | the strip title (`titleLarge`, `onSurface`) |
| **Tutorial** | **Sparse** | The caption, `Skip` and the step pips sit on the background, and the column scrolls. It is the first screen a new player meets after Home, so it gets the calmest frame. | The tutorial slab; `Skip`; the completion CTA | the caption, `Skip`, the step-pip row |
| **Classic catalogue** | **Sparse** | It sits between Home and the board in the core flow, so a wash keeps the world continuous. The cards keep today's finish. | The board cards | the top-bar title |
| **Results** | **Sparse** | A celebration whose gold stars, headline and CTA are the payoff (D-14). No light or wave competes with them, and no gold enters the backdrop (B-04). | The star row, the headline, the primary CTA | the top-bar title, the headline, the metric labels, the outlined and text buttons' labels |
| **Settings** | **Sparse** | A utility screen of clay rows, whose headers sit on the background and scroll. | The rows and switches | the top-bar title, the section headers |
| **Licences** and **Licence text** (`docs/ux/licences-screen.md`) | **Sparse** | Settings' children, treated exactly as their parent. | The entry cards; the licence text | the top-bar titles, the section headers |
| **Dialogs** (restart confirm, ad offer, Play Games prompt, profile swap), snackbars, the Results ad scrim | inherit | They scrim or sit on the screen beneath them. Nothing about them changes, their finish included. | — | — |
| Ad and Play Games full-screen UIs, the system splash | — | The app does not draw them. The activity is paused under them, so the clock stops (B-22). | — | — |

**Challenge** is not a screen. It is the gameplay screen entered with a `challengeLevel`
(`AppNavHost.kt`, `GameRoute`), so it carries Quiet by construction (B-07).

**What a density is.** It is a `BackdropDensity` value that each destination maps to. The mapping is
the tech-lead's R1: `Home` → Landscape, `Gameplay` → Quiet, everything else → Sparse, the last as the
`else` branch so a destination added later is safe. A density selects which layers draw (§3.1), which
sky stops, wave positions and light table apply (§3.3), and which idle delay settles the clock
(B-22). It never changes a token's meaning or any rule in §5. The container finish (§3.5) belongs
to the containers on Home and the game screen. It is not a density.

## 3. The scene and the finish

### 3.1 Layers

The backdrop is one host behind the `NavHost` (the tech-lead's R1), drawing these layers back to front.
Each fills the **whole window** `W × H`, under the status bar, the navigation bar and any display cutout
(B-09). A position is a fraction of `W` or `H` (unitless, `BackdropMetrics`, §4.3) or a `dimens.xml`
token (§4.2), and never a literal.

| # | Layer | Draws | Densities | Composed | Redraws when |
|---|---|---|---|---|---|
| L1 | **Sky**: one rect | a vertical gradient of the density's sky stops (§3.3) | all three | on the first frame | window size, theme or density change only |
| L2 | **Waves**: the scene node, first | the back wave, then the front wave; each is its fill, then its lip, then its sheen (§3.2) | Landscape, Quiet | from the second frame on (B-11a) | every frame while the clock runs; only their translation changes |
| L3 | **Lights**: the scene node, after the waves | the density's lights, in table order (§3.3) | Landscape, Quiet | with L2 | every frame while the clock runs; only their translation and alpha change |

Whether L2 and L3 share one node or draw in two is the tech-lead's decision. The back-to-front order
is binding: sky, back wave, front wave, lights. Sparse draws L1 alone.

**Building and drawing.**
- **Built once:** every path and every brush is built once per window size, theme, density and layout
  direction, and kept. That covers the two wave shapes and their lip bands, the sky and wave-fill
  gradients, each light's radial brush (centred at its own origin), and the wave sheen's brush.
- **Each frame:** the draw pass only translates, sets an alpha and replays. It allocates nothing
  (rules/compose.md; the tech-lead's R1 lists the usual misses).
- **Inputs:** nothing reads `UiState`, a random source, a resource file, a font or the network. The
  one time source is the clock (B-24).

**Where the hero objects sit.**
- **Above every layer, opaque:** every container the player reads.
  - Home: the stage art, the ribbon pills, the Continue chips, the gel tiles and the gear chip.
  - The game screen: the board slab, the dock, and the strip's back chip and timer pill.
  - Every screen: every card, row, banner, snackbar and dialog.
- **Transparent by design (B-09):** the Home hero row, the gameplay status strip, the four
  `TopAppBar`s and the tutorial header. Their text sits on L1 in the calm band (B-20), and §5.1
  governs it.
- **Never:** a layer is never drawn above a container, inside the board slab, or in front of it.

### 3.2 The elements

Three kinds of element, and nothing else. Their fills are the §4.1 tokens only (B-04).

**The sky (L1).** One vertical linear gradient over the window, through the density's stops (§3.3),
the colour blending linearly from each stop to the next.

**A wave (L2).**

| Part | Geometry | Fill |
|---|---|---|
| **Shape** | Two pieces, unioned: the upper half of an ellipse, and the rectangle beneath it. The ellipse has centre `(cx·W, crest·H + WAVE_RY·H)` and semi-axes `(WAVE_RX·W, WAVE_RY·H)`, with `WAVE_RX` 0.75 and `WAVE_RY` 0.112. The rectangle spans from `cx − WAVE_RX` to `cx + WAVE_RX` across, and from the ellipse's centre line down to the window's bottom edge plus the wave's own vertical bob amplitude, so no pose shows a gap at the bottom. The **crest**, its highest point, is at `(cx·W, crest·H)`. Both waves' shapes span past both side edges at every pose (§3.3). | A vertical linear gradient in the wave's own coordinates, so it moves with the wave. Let `y0` be the crest line. `BackdropWaveCrest` sits at `y0`, `BackdropWaveBody` at `y0 + WAVE_BODY_STOP (0.18) × WAVE_DEPTH (0.70) × H`, and `BackdropWaveDeep` at `y0 + WAVE_DEPTH × H`, held below that |
| **Lip** | The band inside the shape between its top edge and the same ellipse moved down by `backdrop_wave_lip_depth` (4dp): the shape minus its own 4dp-lower copy, built once with a path difference | `BackdropWaveLip`, flat |
| **Sheen** | An ellipse centred at `(cx·W, y0 + backdrop_wave_sheen_offset + backdrop_wave_sheen_height / 2)`, with radii `(WAVE_SHEEN_HALF_WIDTH · W, backdrop_wave_sheen_height / 2)`: 0.42 W, and 12dp. It is clipped to the shape | A radial gradient, from `BackdropWaveSheen` at its centre to transparent at its edge. It is drawn as a circular radial gradient under a vertical scale of height over width, the usual way to draw an elliptical gradient in Compose |

The **back wave** is drawn first, and each of its three draws carries alpha `WAVE_BACK_ALPHA` (0.60),
applied draw by draw, with no offscreen layer. The **front wave** draws at full alpha over it. Each
draw keeps its own token's alpha on top.

**A light (L3).** A disc of diameter `d` (a size token, §4.2), its centre at `(x·W, y·H + dy)`. It is
filled with a radial gradient in three stops:
1. its kind's **core** colour from the centre to `LIGHT_CORE_STOP` (0.64) of the radius;
2. its kind's **rim** colour at `LIGHT_RIM_STOP` (0.84);
3. transparent at the edge.

It is drawn at alpha `opacity × envelope × calm` (§3.4). The rim stop is the light's extreme point:
the brightest in dark, and the darkest against the light sky. Every bound in §5 is computed there.

| Kind | Core | Rim | Reads as |
|---|---|---|---|
| **Pearl** | `BackdropLightPearlCore` | `BackdropLightPearlRim` | a white glint in light, a lilac glow in dark |
| **Pink** | `BackdropLightPinkCore` | `BackdropLightPinkRim` | a soft pink light |
| **Violet** | `BackdropLightVioletCore` | `BackdropLightVioletRim` | a soft violet light |

A light has no highlight dot, no ring drawn round it, no rays, no points and no twinkle. It is a soft
out-of-focus disc of light, never a bubble, a star, a sparkle or a sweet.

**Not in the vocabulary, on purpose.**
- Every motif of the second edition: hills, frosting lips and drips, clouds, the lollipop, cane,
  gumdrop and cupcake, sprinkles.
- The two orbs (B-05a).
- Any star, sun, rainbow, sparkle or twinkle glyph.
- Any bubble with a rim and a highlight dot (direction C's finish, which the founder did not pick).
- Any blur, noise or texture; any particle burst.
- Any character, face, path, map, node chain or lettering; any grid or tile pattern.
- Any wobble, jiggle or squash on the gel tiles beyond today's `claySquish` press.
- §10 is the checklist, and §13 gives the reasons.

### 3.3 Composition per density

The origin is the window's top-start corner. `x` is a fraction of `W` measured from the start edge,
and `y` a fraction of `H`, unless a dp token is named. Every number is a named constant (§4.3, §4.4)
or a token (§4.2). The developer transcribes them and never invents a position. BD-10 asserts these
tables against the constants.

**Sky stops (L1).** Top to bottom; the colour blends linearly from each stop to the next.

| Density | stop 1 | stop 2 | stop 3 | stop 4 | stop 5 |
|---|---|---|---|---|---|
| Landscape | `0.00` SkyTop | `0.12` SkyTop | `0.34` SkyMid | `0.66` SkyLow | `1.00` SkyDeep |
| Quiet | `0.00` SkyTop | `0.12` SkyTop | `0.34` SkyMid | `0.66` SkyLow | `1.00` SkyDeep |
| Sparse | `0.00` SkyTop | `0.12` SkyTop | `1.00` SparseLow | — | — |

`SkyTop` equals the theme's `background` (BD-09). The status-bar area of every screen is therefore
flat `background`: as today on Home, gameplay and the tutorial, and in place of the old top-bar colour
on the four top-bar screens (B-09b). The status-bar icon appearance the app uses today stays correct. Landscape and Quiet share their stop values but keep one constant each (one owner per
constant).

**Waves (L2).**

| Density | Wave | Centre `cx` | Crest | Bob, along x | Bob, along y | Half-cycle |
|---|---|---|---|---|---|---|
| Landscape | back (alpha 0.60) | 0.50 | 0.49 | `backdrop_wave_back_bob_x` 24dp | `backdrop_wave_back_bob_y` 4dp, rising as it moves toward the end edge | 12 s |
| Landscape | front | 0.35 | 0.62 | `backdrop_wave_front_bob_x` 20dp, opposite to the back wave | `backdrop_wave_front_bob_y` 4dp, sinking as it moves toward the start edge | 9 s |
| Quiet | back (alpha 0.60) | 0.50 | 0.72 | 24dp | 4dp, as above | 18 s |
| Quiet | front | 0.35 | 0.80 | 20dp, opposite | 4dp, as above | 14 s |

**Coverage.** The front wave's shape spans `−0.40 W … 1.10 W`, and the back wave's `−0.25 W … 1.25 W`.
With the bob, the front wave's end-side edge stays past the window by at least `0.10 W − 20dp`. That is
16dp at 360dp, and it only grows on wider windows, so the side edges never show a gap.

**Lights (L3), Landscape.** Each light has a rest centre `(x, y)`, a diameter token, a kind, a period,
a phase at clock time 0 (`φ0`) and an opacity. **Lanes** are the five lights that may reach above
`TITLE_REACH` (0.47 H). At 360dp their horizontal extents are disjoint, with ≥ 13dp between them, and
the gaps grow on every wider window. **Low** lights never rise above 0.47 H.

| Light | Set | `x` | `y` | Diameter | Kind | Period | `φ0` | Opacity | Extent at 360dp |
|---|---|---|---|---|---|---|---|---|---|
| H1 | lane | 0.08 | 0.27 | `backdrop_light_md` 44dp | Pearl | 30 s | 0.13 | 0.90 | 7–51dp |
| H2 | lane | 0.22 | 0.40 | `backdrop_light_xs` 28dp | Pink | 24 s | 0.67 | 0.85 | 65–93dp |
| H3 | lane | 0.50 | 0.33 | `backdrop_light_xl` 64dp | Violet | 36 s | 0.30 | 0.70 | 148–212dp |
| H4 | lane | 0.76 | 0.22 | `backdrop_light_sm` 36dp | Pink | 26 s | 0.42 | 0.85 | 256–292dp |
| H5 | lane | 0.92 | 0.37 | `backdrop_light_lg` 52dp | Violet | 34 s | 0.06 | 0.75 | 305–357dp |
| H6 | low | 0.39 | 0.91 | `backdrop_light_xxl` 76dp | Violet | 40 s | 0.20 | 0.55 | — |
| H7 | low | 0.92 | 0.90 | `backdrop_light_sm` 36dp | Pink | 28 s | 0.50 | 0.80 | — |
| H8 | low | 0.16 | 0.67 | `backdrop_light_md` 44dp | Pearl | 32 s | 0.19 | 0.60 | — |
| H9 | low | 0.50 | 0.94 | `backdrop_light_xs` 28dp | Pearl | 22 s | 0.41 | 0.80 | — |

A low light's highest top edge is `y·H − backdrop_light_rise_above − d/2`. At 360×640dp the lowest of
them, H8, reaches 327dp against the 301dp line, and every taller window adds margin. Low lights may
overlap each other and the waves: everything below 0.47 H on Home is either behind the lower group or
holds no uncontained text. The board's own lights sat in the same places: two clusters in the wings
beside the stage, and a few low among the waves. This table puts them in lanes and fixes their
values.

**Lights (L3), Quiet.** No two discs ever overlap. The three **upper** lights stay above 0.33 H, and
the two **lower** lights stay below 0.59 H, so none ever crosses the band where the Challenge loading
spinner sits (B-21).

| Light | Set | `x` | `y` | Diameter | Kind | Period | `φ0` | Opacity | Extent at 360dp |
|---|---|---|---|---|---|---|---|---|---|
| G1 | upper | 0.13 | 0.19 | `backdrop_light_sm` 36dp | Pearl | 36 s | 0.14 | 0.80 | 29–65dp |
| G2 | upper | 0.81 | 0.23 | `backdrop_light_lg` 52dp | Violet | 42 s | 0.43 | 0.70 | 266–318dp |
| G3 | upper | 0.46 | 0.25 | `backdrop_light_xs` 28dp | Pink | 30 s | 0.33 | 0.80 | 152–180dp |
| G4 | lower | 0.24 | 0.77 | `backdrop_light_lg` 52dp | Violet | 44 s | 0.27 | 0.60 | 60–112dp |
| G5 | lower | 0.82 | 0.79 | `backdrop_light_md` 44dp | Pink | 38 s | 0.63 | 0.80 | 273–317dp |

At 360×640dp the upper lights' lowest bottom edge is 206dp (0.32 H), and the lower lights' highest top
edge is 387dp (0.60 H). The upper lights live between the strip and the slab's top. On a 640dp-tall
window they are mostly behind the slab and the calm fade (B-20). On the 412×915dp baseline they show
between the calm end (128dp) and the slab's top (231dp), fading out as they rise toward the band.

**What a player sees** (24dp status bar and 48dp three-button bar unless named). The slab and dock
positions are phase 15's:

| Window | Home: back crest, front crest | Game: slab | Game: back crest, front crest | Game: dock |
|---|---|---|---|---|
| 360×640dp | 314dp, 397dp: behind the compact lower group; seen in the gaps, margins and footer | 120–448dp | 461dp (13dp below the slab's foot), 512dp (behind the dock's top) | 504–576dp |
| 412×915dp | 448dp, 567dp: behind the ribbon and the chips | 231–611dp | 659dp, 732dp: two glossy crests in the 168dp band between the slab and the dock | 779–851dp |
| Pixel_9, 411×923dp (54dp status, 24dp gesture bar) | 452dp, 572dp | 263–642dp | 665dp, 738dp | 811–883dp |
| 800×1280dp | 627dp, 794dp; the 160dp side margins show the waves' full width | 364–844dp | 922dp, 1024dp | 1144–1216dp |

**Sparse.** The wash (`SkyTop` held to 0.12 H, then a blend to `SparseLow` at the bottom edge), and
nothing else. Every colour on these screens is zone T (B-23), so no text falls below 4.5:1 anywhere, in
any scroll state, at any font scale.

### 3.4 Poses: what the clock does to each element

All of these are pure functions in `BackdropGeometry`'s model (B-24), and BD-11 tests them directly.
`t` is the clock's time; at animator scale 0 there is no clock and the **rest pose** is drawn.

**A wave.** `p(t) = −cos(π · t / T)`, where `T` is the wave's half-cycle, so `p` swings from −1 to +1
and back, eased at both ends.
- The **back wave** is offset by `(+24dp · p, −4dp · p)`.
- The **front wave** is offset by `(−20dp · p, +4dp · p)`.
- The two move against each other: as the back wave drifts toward the end edge and up, the front
  wave drifts toward the start edge and down.
- **Rest pose:** `p = 0`, an offset of (0, 0).
- Both waves start at `p = −1` when the clock starts.

**A light.** Its phase is `φ(t) = frac(φ0 + t / period)`.
- **Rise:** `dy(φ) = backdrop_light_rise_below − (backdrop_light_rise_below + backdrop_light_rise_above) × φ`.
  That is 32dp below its rest centre at `φ = 0`, rising linearly to 80dp above at `φ → 1`.
- **Envelope:** `envelope(φ)` ramps linearly 0 → 1 over `φ ∈ [0, LIGHT_FADE_IN)` (0.15), holds 1
  until `LIGHT_FADE_OUT` (0.85), and ramps 1 → 0 to `φ = 1`. A light therefore wraps from top to
  bottom only while invisible.
- **Calm factor:** `calm = clamp((topEdge − calmEnd · H) / backdrop_light_calm_fade, 0, 1)`, where
  `topEdge = y·H + dy − d/2` and `calmEnd` is the density's calm end (0.14).
- **Alpha:** `opacity × envelope × calm`.
- **Speed:** 112dp per period, so ≤ 5.1dp/s (H9, 22 s).
- **Rest pose:** `φ = LIGHT_REST_PHASE` = 32 / 112 (≈ 0.286), so `dy = 0` and the envelope is 1. The
  alpha is `opacity × calm`, with `calm` taken at the rest position.

**The gel sheen** (§3.5.2). `σ(t) = frac(t / GEL_SHEEN_PERIOD_MS)`, the Challenge tile's lagging by
`GEL_SHEEN_CHALLENGE_DELAY_MS`.
- It is drawn only while `σ ≥ GEL_SHEEN_START` (0.75). Its travel across the tile is `FastOutSlowIn`
  over the remaining quarter of the loop.
- **Rest pose:** not drawn.
- **On the live clock, only while Running** (B-22a): never while the clock is Settling or settled
  AtRest.

**Mirroring.** In RTL every x position is measured from the start edge, which is then on the right.
Every x offset and the sheen's direction mirror with it (B-15).

**The animator scale.** The clock's time advances at `1 / scale` of real time, so the periods stretch
at 2× and shrink at 0.5×, as every Compose animation does (B-24). At scale 0 nothing runs.

### 3.5 The container finish on Home and the game screen

The finish belongs to the containers. It is drawn by each container as part of its own fill, on
every frame the container draws, frame 1 included (B-11a). It is the same with or without the
backdrop behind it, so BD-17's comparisons hold by construction. No finish part changes a size, a
padding, a shape, a touch target, a semantics node or a click sound.

#### 3.5.1 The gloss finish (B-19)

| Part | Geometry | Light | Dark |
|---|---|---|---|
| **Rim** | A `finish_rim_width` (1dp) stroke whose outer edge is the container's edge, following its shape | `BackdropPanelRim`, white 90% | white 16% |
| **Lip** | The band inside the shape between its outline and the same outline moved down by `finish_lip_height` (2dp). It is widest at the top centre and thins to nothing down the sides | `BackdropPanelLip`, white 95% | white 18% |
| **Gloss** | A vertical gradient over the container's own height, clipped to its shape: `BackdropPanelGloss` at the top edge, transparent at `FINISH_GLOSS_FADE` (0.46) of the height | white 70% | white 6% |
| **Glow** | The container's existing `clayShadow` geometry and elevation, unchanged, with `BackdropPanelGlow` as the ambient and spot colour in place of `ClayColors.shadowTint` | `#DB2777` at 30% | `#8B5CF6` at 45% |

**Draw order inside a container:** fill, gloss, rim, lip, then the content. No content reaches within
2dp of a container's edge, so the rim and the lip never touch text or a glyph.

**Which container takes which part:**

| Container (screen) | Fill | Rim | Lip | Gloss | Glow | Why |
|---|---|---|---|---|---|---|
| Board slab (game) | `surfaceContainerHigh`, unchanged | ✓ | ✓ | — | ✓ (`elevation_card`) | A gloss would lie over the board. Rim and lip sit inside BD-17's 2dp inset |
| Stage art (Home) | unchanged | ✓ | ✓ | — | ✓ | Its interior is the board art (15q, BD-17) |
| Dock slab (game) | unchanged | ✓ | ✓ | — | ✓ | 21e and 21f measure the discs against the slab: its fill around the discs stays flat |
| The dock's discs, all five looks, and the on mark | unchanged | — | — | — | unchanged | The dock's own look language (§4.4.1 of the phase-15 spec); `DockOnMarkAndSlashTest` asserts their fills |
| Back chip, timer pill (game) | unchanged | ✓ | ✓ | ✓ | ✓ (`elevation_pressed`, as today) | The board's strip |
| Gear chip (Home) | unchanged | ✓ | ✓ | ✓ | ✓ | The board's hero row |
| Ribbon pills (Home) | unchanged | ✓ | ✓ | ✓ | ✓ | The board's ribbon |
| Continue chips (Home) | `tertiaryContainer`, unchanged | ✓ | ✓ | ✓ | ✓ | The board's chip |
| Rescue banner (game) | `errorContainer`, unchanged | ✓ | ✓ | — | its shadow unchanged | It carries three actions on its own surface. The rim gives its edge the B-14a floor |
| Classic and Challenge tiles (Home) | **gel** (§3.5.2) | `GelRim` | the gel lip | — | gel glow | B-18 |
| Snackbars, dialogs, the ad scrim | unchanged | — | — | — | unchanged | They sit over the screen, not on the backdrop |
| Every container on a Sparse screen | unchanged | — | — | — | unchanged | B-23 |

The glow is how the board's "pink glow in light, violet glow in dark" under panels is built in
Compose. `clayShadow` already takes its colour from `ClayColors`. How the containers on these two
screens take the glow tint, whether a parameter, a variant or a provided palette, is the tech-lead's
call. The geometry does not change.

#### 3.5.2 The gel mode tiles (B-18)

Everything not named here is `ModeTile` as built: shape `shapes.large`, `mode_tile_min_height`, the
16dp content padding, the badge, title, supporting slot and art layout, the entrance, `claySquish`,
`ButtonForward`, the semantics and the announcement.

| Part | Spec | Both themes |
|---|---|---|
| **Fill** | A vertical gradient over the tile's height: `GelTop` at its top edge, `GelMid` at `gel_top_band` (16dp), `GelBottom` at its bottom edge | Classic, violet: `GelVioletTop` `#9566F2`, `GelVioletMid` `#7C3AED`, `GelVioletBottom` `#5B21B6`. Challenge, pink: `GelPinkTop` `#E44593`, `GelPinkMid` `#D61F6F`, `GelPinkBottom` `#BE185D` |
| **Title, Classic's supporting line** | Content colour `OnGel` (white) | `#FFFFFF` |
| **Badge** | The glyph badge, `shapes.medium`, filled `GelBadge`; its glyph is `OnGel` | white 22% |
| **Challenge level chip** | The chip, `shapes.small`, filled `GelChip`; its text is `GelChipText` (`bodyMedium`, as today) | white 95%; `#831843` |
| **Classic trailing art** | The same footprint at `home_tile_art_size`, drawn flat: pegs `GelArtPeg`, holes `GelArtHole`, with no sphere shading or specular | white 90%; white 22% |
| **Challenge trailing art** | The bolt, tinted `GelArtPeg` | white 90% |
| **Rim** | `gel_rim_width` (1dp), its outer edge on the tile's edge, `GelRim` | white 30% |
| **Gel lip** | A rounded band inset `gel_lip_inset` (12dp) from each side, from `gel_lip_top` (4dp) to `gel_lip_bottom` (16dp) below the top edge, corners `gel_lip_corner` (8dp). It is filled with a vertical gradient from `GelLip` at its top to transparent at its bottom | white 38% |
| **Sheen** | A band `GEL_SHEEN_WIDTH` (0.30) × the tile's width wide, leaning `GEL_SHEEN_SKEW_DEG` (14°) with its top toward the end edge. It is filled with a horizontal gradient: transparent, then `GelSheen` at its centre line, then transparent. It is drawn **only inside the gel lip** (clipped to it), and it travels from wholly before the tile's start edge to wholly past its end edge in the last quarter of each 10 s loop (§3.4). Classic leads, and Challenge follows 0.6 s later | white 30% |
| **Glow** | The tile's existing `clayShadow(elevation_card)` geometry, tinted `GelGlowViolet` / `GelGlowPink` | `#5B21B6` at 55%; `#BE185D` at 50% |
| **Pressed and focused** | The card's state layer uses the tile's own `GelBottom` at M3's pressed and focus alphas. Pressing darkens the gel; it never lightens it | — |

**The text rule this layout already guarantees.** `ModeTile`'s row is padded by `spacing_md` (16dp)
on every side, so the title's line box starts at 16dp or lower at every font scale. Every white glyph
therefore sits at or below `gel_top_band`, on gel that only darkens downward:
- every channel falls from `GelMid` to `GelBottom`;
- the lip has faded to nothing by 16dp;
- the sheen never leaves the lip.

That padding is a contract: a change to it, or to `gel_top_band`, must keep `gel_top_band` ≤ the
tile's top content padding (BD-27 measures it). §5.3 has the ratios.

**What the gel does not do.** It does not wobble, jiggle, pulse or squash beyond today's press. It
has no texture, no bubbles and no inner highlight dots. It has no "drip" or liquid edge: its outline
is the tile's rounded rectangle. It is one colour family per tile, violet or pink, the house hues
(B-04), never a set of six.

#### 3.5.3 What keeps today's finish

The dock's discs and on mark; the tutorial slab and every Sparse screen's cards, rows, buttons and
top bars; the catalogue's preview boards; Results' stars and their glow; every dialog, snackbar and
the ad scrim; and the pegs, holes and marks inside every board.

## 4. Tokens

### 4.1 `Color.kt`: new colour tokens

Every hex below lives **only** in `ui/theme/Color.kt`, named `Light<Token>` and `Dark<Token>` like the
existing `LightCtaGradientColors` / `DarkPegHighlight`. The gel tokens are theme-independent and get
one value each. The tokens reach the backdrop and the containers through immutable palette holders
provided by `AppTheme`, the way `ClayColors` is (`LocalClayColors`; the tech-lead's R1 names
`BackdropColors`). No call site names a hex. Alpha tokens are written `#AARRGGBB`, as `Color(0xAARRGGBB)`
takes them. `L` is WCAG relative luminance computed from the hex. **Zone** is B-03's: **T** may sit
under uncontained text, **S** only where the layout puts none. "The board" is
`docs/ux/backdrop-directions.html`'s `:root`.

**The backdrop**

| Token | Light | L | Dark | L | Zone | Derived from |
|---|---|---|---|---|---|---|
| `BackdropSkyTop` | `#F4F1FA` | 0.890 | `#1B1626` | 0.0095 | T | **identical to `background`** in each scheme (BD-09) |
| `BackdropSkyMid` | `#F6E3F6` | 0.812 | `#1C0E38` | 0.0085 | T | the board's `--bLMid` / `--bDMid` |
| `BackdropSkyLow` | `#F0C9E7` | 0.661 | `#2A1150` | 0.0147 | S light, T dark | `--bLLow` / `--bDLow` |
| `BackdropSkyDeep` | `#E3C3F6` | 0.620 | `#3A0F48` | 0.0171 | S light, T dark | `--bLDeep` / `--bDDeep` |
| `BackdropSparseLow` | `#FBF0FA` | 0.897 | `#1C0E38` | 0.0085 | T | new for B-23: the background lifted toward pink (light, above the background's L); the dark sky's mid (dark, below it) |
| `BackdropWaveCrest` | `#F6B3DD` | 0.571 | `#9B6BF7` | 0.242 | S | `--bLW1` / `--bDW1` |
| `BackdropWaveBody` | `#DD8BE6` | 0.396 | `#7C2DB8` | 0.096 | S | `--bLW2` / `--bDW2` |
| `BackdropWaveDeep` | `#B98AF2` | 0.349 | `#4A1158` | 0.026 | S | `--bLW3` / `--bDW3` |
| `BackdropWaveLip` | `#B3FFFFFF` (white 70%) | — | `#59FFFFFF` (white 35%) | — | S | `--bLWLip` / `--bDWLip` |
| `BackdropWaveSheen` | `#A6FFFFFF` (white 65%) | — | `#4DFFFFFF` (white 30%) | — | S | `--bLWHi` / `--bDWHi` |
| `BackdropLightPearlCore` | `#CCFFFFFF` (white 80%) | — | `#24C4B5FD` (`#C4B5FD` 14%) | — | S | `--bLK1` / `--bDK1` |
| `BackdropLightPearlRim` | `#F2FFFFFF` (white 95%) | — | `#3DC4B5FD` (24%) | — | S | `--bLK1r` / `--bDK1r` |
| `BackdropLightPinkCore` | `#4DF472B6` (`#F472B6` 30%) | — | `#24F472B6` (14%) | — | S | `--bLK2` / `--bDK2` |
| `BackdropLightPinkRim` | `#8CF472B6` (55%) | — | `#3DF472B6` (24%) | — | S | `--bLK2r` / `--bDK2r` |
| `BackdropLightVioletCore` | `#4DA78BFA` (`#A78BFA` 30%) | — | `#298B5CF6` (`#8B5CF6` 16%) | — | S | `--bLK3` / `--bDK3` |
| `BackdropLightVioletRim` | `#8CA78BFA` (55%) | — | `#478B5CF6` (28%) | — | S | `--bLK3r` / `--bDK3r` |

**The gloss finish (§3.5.1)**

| Token | Light | Dark | Derived from |
|---|---|---|---|
| `BackdropPanelRim` | `#E6FFFFFF` (white 90%) | `#29FFFFFF` (white 16%) | the board's `--slabRim` (`--w90` / `--w16`) |
| `BackdropPanelLip` | `#F2FFFFFF` (white 95%) | `#2EFFFFFF` (white 18%) | `--bLLip` / `--bDLip` |
| `BackdropPanelGloss` | `#B3FFFFFF` (white 70%) | `#0FFFFFFF` (white 6%) | `--bLGloss`; **dark lowered from the board's 10%** (B-19) |
| `BackdropPanelGlow` | `#4DDB2777` (`#DB2777` 30%) | `#738B5CF6` (`#8B5CF6` 45%) | `--bLGlow` / `--bDGlow` |

**The gel tiles (§3.5.2), one value in both themes**

| Token | Value | L | Derived from |
|---|---|---|---|
| `GelVioletTop` / `GelVioletMid` / `GelVioletBottom` | `#9566F2` / `#7C3AED` / `#5B21B6` | 0.223 / 0.134 / 0.067 | `--gV1..3`; `#7C3AED` is light `primary`, `#5B21B6` the title gradient's deep stop |
| `GelPinkTop` / `GelPinkMid` / `GelPinkBottom` | `#E44593` / `#D61F6F` / `#BE185D` | 0.229 / 0.164 / 0.124 | `--gP1..3`; `#BE185D` is the peg gradient's rim |
| `OnGel` | `#FFFFFF` | 1.000 | `--white` |
| `GelBadge` | `#38FFFFFF` (white 22%) | — | `--w22` |
| `GelChip` | `#F2FFFFFF` (white 95%) | — | `--w95` |
| `GelChipText` | `#831843` | 0.059 | `--lOnSc`, light `onSecondaryContainer` |
| `GelArtPeg` / `GelArtHole` | `#E6FFFFFF` (white 90%) / `#38FFFFFF` (white 22%) | — | `--w90`; `--w22` |
| `GelRim` | `#4DFFFFFF` (white 30%) | — | new, B-14a (the board had a 38% top line only) |
| `GelLip` / `GelSheen` | `#61FFFFFF` (white 38%) / `#4DFFFFFF` (white 30%) | — | `--gelLip` / `--gelSheen` |
| `GelGlowViolet` / `GelGlowPink` | `#8C5B21B6` (55%) / `#80BE185D` (50%) | — | `--gelGlowV` / `--gelGlowP` |

**The constraints these values satisfy.** A reviewer checks these, not the hexes. If a value is
retuned on a device, it must still meet all of them. BD-08 computes each from `Color.kt` in a JVM test.

- **Zone T, light:** L ≥ 0.779. `primary` (L 0.134), `onSurfaceVariant` (0.119) and `onSurface` (0.030)
  then all read ≥ 4.5:1 on it.
- **Zone T, dark:** L ≤ 0.0357. Dark `primary` (0.336), `onSurfaceVariant` (0.364) and `onSurface`
  (0.829) then all read ≥ 4.5:1 on it.
- **Sky:**
  - `SkyTop` equals `background` in each scheme.
  - Light: SkyTop > SkyMid > SkyLow > SkyDeep in L.
  - Dark: SkyMid ≤ SkyTop, and every dark sky stop is zone T.
- **Sparse** (B-23): light `SparseLow` has L ≥ the background's (0.890); dark `SparseLow` has L ≤ the
  background's (0.0095). Every blend between `SkyTop` and `SparseLow` stays on that side. BD-08 samples
  it at 11 points.
- **The Home title's reach** (B-21, colour-vision.md §4.4 and §11). Every lane light's rim colour, at that
  light's opacity, composited over the sky at 0.47 H, reads L ≥ 0.503 in light and L ≤ 0.0786 in
  dark. The sky at 0.47 H is the darkest sky above that line in light (L 0.747) and the brightest
  in dark (L 0.011), so it is the worst case. The worst cases are a Pink rim (L 0.522) in light
  and a Pearl rim (L 0.047) in dark.
- **The rim rule's floors** (B-14a, §5.2), from each container's fill token and its rim token.
- **Gel text** (§5.3):
  - White reads ≥ 4.5:1 on `GelVioletMid` and on `GelPinkMid`.
  - Every channel falls from `GelMid` to `GelBottom`.
  - White on `GelBadge` over `GelMid` reads ≥ 3:1.
  - `GelChipText` on `GelChip` over `GelMid` reads ≥ 4.5:1.
- **Gloss** (§5.3): dark `onSurfaceVariant` on `surfaceContainerHigh` under the full dark
  `BackdropPanelGloss` reads ≥ 4.5:1.
- **Hue.**
  - **The arc:** every opaque new token, and the RGB of every tinted one, lies in the violet–pink arc
    250°–336°. The values span 252°–336°. The arc is widened by 1° from the second edition's 335° to
    admit `#BE185D` (335.1°, the peg gradient's own rim, which `GelPinkBottom` reuses) and `#831843`
    (335.9°, an existing role). Red is still ≥ 24° away.
  - **White:** pure white at any alpha is achromatic and outside the arc test.
  - **Banned:** no token is gold, blue, red, green or brown (B-04).

### 4.2 `dimens.xml`: new dimension tokens

One block, comment-headed `<!-- Backdrop -->` (the tech-lead's R6). One token per owner, even where
values coincide (the file's own convention).
- The three sub-4dp values join the file's documented rendering-stroke exception, beside
  `peg_selected_ring_width`: `finish_rim_width`, `finish_lip_height` and `gel_rim_width`.
- Every other value is a multiple of 4dp.
- The second edition's 32 motif tokens are not created (§16).

| Token | Value | Owner |
|---|---|---|
| `backdrop_light_xs` | 28dp | a light's diameter, extra small |
| `backdrop_light_sm` | 36dp | a light's diameter, small |
| `backdrop_light_md` | 44dp | a light's diameter, medium |
| `backdrop_light_lg` | 52dp | a light's diameter, large |
| `backdrop_light_xl` | 64dp | a light's diameter, extra large |
| `backdrop_light_xxl` | 76dp | a light's diameter, the largest |
| `backdrop_light_rise_below` | 32dp | how far below its rest centre a light begins its rise |
| `backdrop_light_rise_above` | 80dp | how far above its rest centre it ends |
| `backdrop_light_calm_fade` | 48dp | the distance over which a light fades out before the calm end |
| `backdrop_wave_lip_depth` | 4dp | a wave lip's depth |
| `backdrop_wave_sheen_offset` | 8dp | a wave sheen's top, below the crest |
| `backdrop_wave_sheen_height` | 24dp | a wave sheen's height |
| `backdrop_wave_back_bob_x` | 24dp | the back wave's sideways bob |
| `backdrop_wave_back_bob_y` | 4dp | the back wave's vertical bob |
| `backdrop_wave_front_bob_x` | 20dp | the front wave's sideways bob |
| `backdrop_wave_front_bob_y` | 4dp | the front wave's vertical bob |
| `finish_rim_width` | 1dp | a glossed container's rim (sub-4dp exception) |
| `finish_lip_height` | 2dp | a glossed container's lip (sub-4dp exception) |
| `gel_top_band` | 16dp | where a gel tile's fill reaches `GelMid`; no white text above it |
| `gel_lip_top` | 4dp | the gel lip's top, below the tile's edge |
| `gel_lip_bottom` | 16dp | the gel lip's bottom |
| `gel_lip_inset` | 12dp | the gel lip's inset from each side |
| `gel_lip_corner` | 8dp | the gel lip's corners |
| `gel_rim_width` | 1dp | a gel tile's rim (sub-4dp exception) |

Light sizes are fixed dp, not window fractions, so a light is the same physical size on every phone.
Only positions scale with the window.

### 4.3 `BackdropMetrics`: unitless ratios, anchors and lists

A scoped constants object, beside `BoardMetrics` and `HomeStageMetrics`, holding exactly the values
§3 names:

| Group | Constants |
|---|---|
| Sky | `LANDSCAPE_SKY_STOPS` `[0.00, 0.12, 0.34, 0.66, 1.00]`, `QUIET_SKY_STOPS` (the same values), `SPARSE_SKY_STOPS` `[0.00, 0.12, 1.00]`, each with its colour roles from §3.3 |
| Calm and reach | `LANDSCAPE_CALM_END` 0.14, `QUIET_CALM_END` 0.14, `TITLE_REACH` 0.47, `QUIET_SPINNER_BAND` `[0.33, 0.59]` |
| Wave shape | `WAVE_RX` 0.75, `WAVE_RY` 0.112, `WAVE_DEPTH` 0.70, `WAVE_BODY_STOP` 0.18, `WAVE_BACK_ALPHA` 0.60, `WAVE_SHEEN_HALF_WIDTH` 0.42 |
| Wave placement | per density, back then front, as `(cx, crest)`: Landscape `(0.50, 0.49)`, `(0.35, 0.62)`; Quiet `(0.50, 0.72)`, `(0.35, 0.80)` |
| Light shape and envelope | `LIGHT_CORE_STOP` 0.64, `LIGHT_RIM_STOP` 0.84, `LIGHT_FADE_IN` 0.15, `LIGHT_FADE_OUT` 0.85. `LIGHT_REST_PHASE` is computed from the two rise tokens, never written as a literal |
| Light tables | `LANDSCAPE_LIGHTS` (9 entries) and `QUIET_LIGHTS` (5 entries). Each entry holds `x`, `y`, a size reference to one of the six size tokens, a kind (`Pearl`, `Pink`, `Violet`), `φ0`, the opacity, and its set (lane / low, upper / lower), in §3.3's order |
| Finish | `FINISH_GLOSS_FADE` 0.46 |
| Gel | `GEL_SHEEN_WIDTH` 0.30, `GEL_SHEEN_SKEW_DEG` 14, `GEL_SHEEN_START` 0.75 |
| Host | `BACKDROP_Z_INDEX` −1 (the tech-lead's R1) |

`BLOB_PEEK_FRACTION` and `ORB_SECONDARY_Y` retire with the orbs, and `HomeBlobs.kt`'s constants go
with that file.

### 4.4 Durations

- **The app-wide one stays in `ClayAnim`.** `NAV_TRANSITION_MS` = 700 (B-13a, D2).
- **The backdrop's own durations live in a scoped `BackdropMotion` object** beside `BackdropMetrics`.
  This follows the project's rule that constants are split by scope, and BD-05 is worded for it.

| Constant | Value |
|---|---|
| `LANDSCAPE_WAVE_BACK_HALF_MS`, `LANDSCAPE_WAVE_FRONT_HALF_MS` | 12 000, 9 000 |
| `QUIET_WAVE_BACK_HALF_MS`, `QUIET_WAVE_FRONT_HALF_MS` | 18 000, 14 000 |
| `LANDSCAPE_LIGHT_PERIODS_MS` | `[30 000, 24 000, 36 000, 26 000, 34 000, 40 000, 28 000, 32 000, 22 000]` (H1–H9) |
| `QUIET_LIGHT_PERIODS_MS` | `[36 000, 42 000, 30 000, 44 000, 38 000]` (G1–G5) |
| `GEL_SHEEN_PERIOD_MS`, `GEL_SHEEN_CHALLENGE_DELAY_MS` | 10 000, 600 |
| `BACKDROP_HOME_IDLE_MS`, `BACKDROP_GAME_IDLE_MS`, `BACKDROP_SETTLE_MS` | 60 000, 20 000, 2 000 (B-22: a proposal; the tech-lead's ruling may change the values, and the constants stay) |
| `MIN_LOOP_MS` | 9 000: the floor B-05a sets. A test asserts that every loop and half-cycle above is at least this |

## 5. Legibility and accessibility

Ratios here are computed from the `Color.kt` hexes with the WCAG 2.x formula. Translucent layers are
composited in gamma-encoded sRGB, as Compose's default alpha draw does. The device reading governs
(BD-12, BD-13, BD-27), exactly as in `docs/ux-spec.md` §4.4.1. Luminances used:
- **light:** `onSurface` 0.030, `onSurfaceVariant` 0.119, `primary` 0.134;
- **dark:** `onSurface` 0.829, `onSurfaceVariant` 0.364, `primary` 0.336;
- **the Home title** (colour-vision.md TG-01, which lands with or before this): light `#7C3AED` →
  `#5B21B6` (L 0.134 / 0.067), dark `#C4B5FD` → `#A78BFA` (L 0.519 / 0.336).

### 5.1 Text on the backdrop

**The rule (B-03, B-20, B-21).**
- **The calm band:** uncontained text on Home and the game screen sits in the band above the calm end.
  That band holds the sky alone, all zone T, at every pose.
- **The wrapped title:** the Home title, wrapped at a large font scale, reaches down to about
  0.46 H. Below the calm band it meets only the sky and one light at a time, within the title bounds.
- **Sparse:** zone T from edge to edge.

| Text role | Light, worst | Dark, worst | Needs |
|---|---|---|---|
| `onSurface`: the strip title, the top-bar titles, Results values and button labels | 11.6:1 (the calm end's L 0.876); 11.8:1 on Sparse | 14.8:1 | 4.5:1 |
| `onSurfaceVariant`: the tutorial caption, Settings headers, Results metric labels | 5.56:1 (Sparse never darker than the background) | 6.95:1 | 4.5:1 |
| `primary`: `Skip`, any `TextButton` default | 5.11:1 | 6.48:1 | 4.5:1 |
| The Home title at rest, in the calm band | 5.04:1 (its light stop) | 6.48:1 (its deep stop) | 3:1 (large text); the design target is 4.5:1 (colour-vision.md TG-02) |
| The Home title wrapped, down to 0.47 H | 3.11:1: the light stop over a Pink light's rim (L 0.522). The deep stop reads 4.89:1 | 3.99:1: the deep stop over a Pearl light's rim (L 0.047). The light stop reads 5.89:1 | 3:1 |

**Where each screen's uncontained text can reach, and why it stays readable:**

| Screen | Text | Reach | How it is guaranteed |
|---|---|---|---|
| Home | the title | At rest, one line: the hero row, ≤ 0.128 H on every reference window (the Pixel_9's 118dp is the deepest). Wrapped, at a large font on a narrow window: down to ≈ 0.46 H, 3 lines at 200% on a 360dp phone | At rest it sits in the calm band (B-20): the sky only, zone T. Wrapped, B-21 keeps every point above 0.47 H to the sky plus at most one light, inside the bounds above, and no wave pixel rises above 0.47 H at any pose. The column only ever scrolls the title **up**, toward the calm band |
| Gameplay | the strip title (`titleLarge`, `onSurface`) | `statusInset … statusInset + 56dp`, ≈ 64dp at 200%: ≤ 0.1375 H on every reference window | Calm band: B-20 |
| Tutorial | the caption, `Skip`, the step-pip row | anywhere: the column scrolls | Sparse, every colour zone T |
| Catalogue, Results, Settings, Licences | the top-bar titles; the headline, metric labels, outlined- and text-button labels; the section headers | anywhere: each scrolls | Sparse, every colour zone T |
| Dialogs, snackbars, the rescue banner, ribbon pills, chips, tiles, cards, rows | — | — | Contained: they sit on their own opaque surface (§5.3) |

**Non-text graphics on the backdrop** (WCAG 1.4.11, 3:1):
- **The top-bar back arrows** (`onSurface`) read ≥ 10:1 on the Sparse wash.
- **The Challenge loading spinner** (`primary`, in the board region before the slab is drawn) sits on
  the sky only, because no Quiet light ever enters 0.33–0.59 H (B-21). It reads ≥ 4.0:1 in light,
  over the darkest sky in that band at 0.59 H (L 0.691), and ≥ 6.0:1 in dark.
- **The Results outlined button's `outline` border** is about 1.6:1 on today's background, a
  pre-existing gap. Sparse in light is never darker than the background (B-23), so it does not get
  worse. It is recorded, not fixed here: the button's `onSurface` label is what identifies it.

### 5.2 The hero edges: the slab-rim rule (B-14a)

**What sits under the board: the existing clay slab and nothing else.**
- `surfaceContainerHigh`, `shapes.extraLarge`, opaque: never a translucent grid of tiles, never
  translucent, never blurred (verdict §3b).
- Pegs, holes, legal-target marks, the selection ring, the hint and the jump are drawn inside it, so
  their distinguishability is unchanged by construction.
- The slab's rim (1dp) and lip (2dp) lie inside BD-17's 2dp inset, so BD-17 proves the interior
  pixel-identical in both themes, with and without the backdrop.

**The rule.** At every pose, on every window, every hero container's edge, taken as the stronger of
fill-vs-adjacent and rim-vs-adjacent, is at least its floor.

**Why it holds against any colour, by arithmetic.** Take a container with fill luminance `F` and a
brighter rim `R`, against an adjacent colour `X`. The stronger of the two ratios is lowest when
`X + 0.05 = √((F + 0.05)(R + 0.05))`, and there it equals `√((R + 0.05) / (F + 0.05))`. That
minimum is the floor in the table, whatever the backdrop, the glow or a light draws beside the edge.

| Container | Fill L, light / dark | Rim L, light / dark | Floor against any colour, light / dark | Today against the background, light / dark | Required |
|---|---|---|---|---|---|
| **Clay family**: the board slab, the stage art, the dock slab, the back chip, the timer pill, the gear chip, the ribbon pills (`surfaceContainerHigh`) | 0.845 / 0.024 | 0.986 / 0.073 | **1.07 / 1.29** | 1.05 / 1.24 | ≥ today in both themes: met |
| **Continue chips** (`tertiaryContainer`) | 0.871 / 0.063 | 0.983 / 0.126 | **1.06 / 1.25** | 1.02 / 1.90 | light ≥ today: met. Dark ≥ 1.20: met |
| **Rescue banner** (`errorContainer`) | 0.810 / 0.032 | 0.979 / 0.081 | **1.09 / 1.26** | 1.09 / 1.39 | light ≥ today: met. Dark ≥ 1.20: met |
| **Gel tiles** (§3.5.2) | 0.067–0.229 (both themes) | `GelRim` over the gel: 0.19–0.38 | **1.21 / 1.21**, at the pink tile's top corners | (a new fill) | ≥ 1.20: met |

The earlier edition gave today's dark slab edge as 1.26:1. Recomputed from `Color.kt` it is 1.24:1
(`#2E2740`, L 0.0240, against `#1B1626`, L 0.0095). This edition uses 1.24.

**What the real backdrop gives** (informative; the floors govern):
- **Light, the clay family:** against every wave colour the fill carries the edge at ≥ 1.3:1 (the back
  wave's crest at 0.60 alpha is the worst). Against the pale sky (`SkyMid`), the waves' white lips
  and the Pearl lights, the rim carries it, never below the 1.07:1 floor (1.20:1 against `SkyMid`).
- **Dark, the clay family:** the rim carries the edge at ≥ 1.84:1 against every dark sky stop. The
  waves' crests and lips are far brighter than any container, so the fill carries it there.
- **Dark Continue chips:** they sit over the back wave's body on Home (L ≈ 0.07–0.10), where they read
  ≈ 1.25–1.4:1. Today they read 1.90:1 against a flat background.
  - This is the one edge the founder's pick makes measurably weaker.
  - It is still above the 1.02:1 the light Continue chip has today.
  - The chip is also amber on violet, labelled, with a lip and a glow.
  - It is accepted as an accessibility judgement by this role and recorded (B-14a). It is not a WCAG
    requirement: a card's outline is not needed to identify the card.

**The hero stays the hero.**
- **Gameplay:**
  - No element of the scene lies within the slab or in front of it.
  - The waves' crests lie below the slab's foot on every reference window (§3.3), so the brightest gloss is behind the dock or under the slab, never beside the pegs.
  - The upper lights pass between the strip and the slab's top.
- **Home:**
  - The gel tiles are the largest, most saturated, glowing and labelled objects on the screen.
  - Nothing in the scene is drawn in front of any container.

### 5.3 Text on the gel tiles and the glossed panels

**Gel tiles** (white text, both themes). Every white glyph sits at or below `gel_top_band` (§3.5.2),
where the gel only darkens downward:

| Pair | Worst | Needs |
|---|---|---|
| White title and supporting line on violet gel (`GelVioletMid` → `GelVioletBottom`) | **5.72:1** (at 16dp), rising to 8.98:1 | 4.5:1 |
| White title on pink gel (`GelPinkMid` → `GelPinkBottom`) | **4.90:1** (at 16dp), rising to 6.04:1 | 4.5:1 |
| White badge glyph on `GelBadge` over the gel (non-text) | 3.91:1 violet, 3.73:1 pink, at the badge's top (22dp) | 3:1 |
| `GelChipText` on `GelChip` over the pink gel (the level chip) | 8.9:1 | 4.5:1 |
| Any of the above while pressed | higher: the state layer darkens (§3.5.2) | — |
| Any of the above during the sheen | unchanged: the sheen never leaves the lip, which ends at 16dp | — |

**Why the board's drawing was changed here**, so nobody restores it:
- The board held its light top stop down to 36% of the tile. A label there, at 200% font, reads about
  3.8:1 on violet (`#9566F2`) and 3.8:1 on pink (`#E44593`).
- Its sheen crossed the whole card. At its centre over the pink title it reads about 3.1:1.
- White at the M3 pressed alpha over pink would read 4.46:1. Hence the darkening state layer.

**Glossed panels.**
- **Light:** a white gloss over a light container only raises the contrast of the dark text on it.
  The ribbon labels go from 5.30:1 to as much as 5.9:1, and the timer digits and the Continue chip's
  text rise likewise.
- **Dark:** the 6% gloss lowers it slightly, and only near each panel's top edge. At the very top
  edge the worst values are:
  - ribbon labels (`onSurfaceVariant` on `surfaceContainerHigh`): ≥ **4.66:1**, against 5.59:1 flat
    (the board's 10% would read 4.10:1);
  - the timer digits and ribbon values (`onSurface`): ≥ 9.9:1;
  - the Continue chip's text: ≥ 6.9:1;
  - the gear and back glyphs (non-text): ≥ 4.66:1.

  In the full-size pill the label starts below the gloss's fade (32dp, against 29dp), so there it
  reads the flat 5.59:1.
- **The rim and the lip** occupy the outer 1dp and 2dp, where no content ever sits.

### 5.4 Colour vision

- **The backdrop and the finish encode nothing.** No hue on them carries a meaning, so a colour
  vision deficiency loses nothing.
- **Every legibility rule above is a luminance ratio.** It holds in greyscale and under deuteranopia,
  protanopia and tritanopia simulations, which keep luminance far better than hue. BD-24 checks
  greyscale captures.
- **The gel tiles are never told apart by colour alone.** Classic and Challenge differ by label, by
  badge glyph (grid or bolt), by trailing art (footprint or bolt) and by the level chip. Their hues,
  violet and pink, read close in greyscale (mid L 0.134 and 0.164), and nothing depends on them.
- **The colour-blind palette** (`docs/ux/colour-vision.md`) changes the board's marks, the stars and
  the done pip only:
  - None of those is on the backdrop, the finish or a gel tile, so the palette needs no variant here.
  - The dock's discs and the slab fill around them are unchanged (§3.5.1), so its CV-11 holds.
  - colour-vision.md §11 records this check.
- **Pegs against holes:** inside the opaque slab, untouched.

### 5.5 System bars and edge to edge

- **Drawn under every bar.** The backdrop fills the window under the status bar, the navigation bar
  and any display cutout (B-09). No content moves: each bar is still applied to the content exactly
  once, by the same layer as today (18b, 18c and Home's 15k/15p geometry keep their numbers; BD-19).
- **Status bar.** `SkyTop` equals `background`, and no light enters the calm band, so the status-bar
  area is flat `background` on every screen, in light and dark, at rest and in motion (B-09b). That is
  today's strip on Home, gameplay and the tutorial. On the catalogue, Results, Settings and the
  Licences screens it replaces the strip their opaque `TopAppBar`s used to paint, by at most 9/255
  per channel. The status-bar icon appearance stays correct in both themes, and the icons keep their
  contrast against the strip (B-09b, item 3).
- **Navigation bar.** The waves and the deep sky lie under it on Home and the game screen. With
  three-button navigation the system's contrast scrim stays on: nothing may set
  `isNavigationBarContrastEnforced` to `false`, and `MainActivity` is not touched (the tech-lead's R1).
  With gesture navigation the handle adapts by itself. BD-19 checks both.
- **The tutorial** keeps `safeDrawingPadding()` on its column, outside `verticalScroll`, as built. Only
  its root `Surface` stops painting `background` (B-09).
- **Display cutouts** show the sky. Nothing is placed relative to them.

### 5.6 Screen readers, touch, text scaling, right to left

- **TalkBack.** The backdrop is not in the traversal: it has no description, takes no focus, is not
  clickable and has no `pointerInput`. The finish adds no node and no property. Every screen's
  traversal order (`docs/ux-spec.md` §11) is unchanged (BD-15).
- **Touch targets.** Unchanged everywhere. The backdrop adds none and covers none. The B-22 input
  observer sees pointer events and never consumes them.
- **Text scaling.** The backdrop has no text and reads no font size: it renders identically at 1.0
  and 2.0 font scale at a fixed clock time (BD-20). A gel tile's light band is fixed in dp, so a
  larger font only moves the text further into the deep gel.
- **Right to left.** The scene, the gel sheen's travel and every x offset mirror (B-15). Every rule
  here holds mirrored.

## 6. Motion, reduced motion and battery

### 6.1 What moves, and how (B-05a, B-24)

| Element | Animator scale > 0 | Animator scale = 0 (reduced motion), and battery saver |
|---|---|---|
| The two waves, on Home and the game screen | bob per §3.4: half-cycles of 12 s and 9 s on Home, 18 s and 14 s on the game screen; ≤ 7dp/s | drawn at the rest pose, offset (0, 0) |
| The lights: nine on Home, five on the game screen | rise and fade per §3.4, on 22–44 s loops; ≤ 5.1dp/s | drawn at the rest pose: each at its rest centre, at `opacity × calm` |
| The gel sheen, on Home | a glint inside each tile's lip, in the last 2.5 s of every 10 s loop; Challenge 0.6 s after Classic | not drawn |
| The sky, the gel fills, and the rims, lips, glosses and glows | static | static |
| The waves' and lights' first appearance (B-11a) | on the second frame, with no fade of their own, behind the content's 240 ms `clayEntrance` | the same, at rest |
| A density change on navigation (B-13a) | the new density's sky and scene cross-fade in over the old over `NAV_TRANSITION_MS` (700 ms); the old one stays opaque beneath | instant swap |
| A light ↔ dark theme change | instant redraw, as the rest of the theme | instant |
| Sparse screens | nothing moves | nothing moves |

Scale values other than 0 stretch or shorten every loop through the clock (§3.4). The reduced-motion
picture is the same picture, still: the waves at their centres, the lights at their rest places, no
glint. A settled picture (§6.3) has no glint either (B-22a).

### 6.2 What never moves

- No parallax and no scroll-linked motion. The backdrop does not scroll with the content.
- No tilt or gyroscope response, and no time-of-day or per-level variation.
- No reaction to a move, a jump, a win or a hint.
- No animated colour, size or shape, and no path rebuilt per frame.
- No wobble, jiggle or pulse on the gel tiles.
- Nothing on the board.

The board's own motion stays the only motion that means something: the selection pulse, the jump
slide, the removal fade and the hint glow (ui-ux-pro-max `motion-meaning`, `excessive-motion`). The
backdrop's motion is atmosphere, slow and periodic, and always behind opaque containers.

### 6.3 Battery: the settle default (B-22, a proposal for the tech-lead's idle-frame ruling)

**The facts it starts from.**
- On the phase-15 build this branch lands on, the game screen draws no idle frames except two:
  - the timer's once-a-second tick;
  - the selection pulse, while a peg is selected.
- Home draws the two orbs' drift on every frame (O-B, ≈ 61 fps idle).
- The app never keeps the screen on (no `FLAG_KEEP_SCREEN_ON` or `keepScreenOn` anywhere), so an idle
  phone's screen times out on the system's setting.
- Direction B makes the game screen animate continuously for the first time.

**The proposed default.**
1. **Lifecycle.** The clock runs only while the host's lifecycle is RESUMED and its window is visible.
   Paused, the clock holds its time. Resumed, it continues from that time, with no jump. Under an ad
   or a Play Games UI the activity is paused, so the clock stops.
2. **Settle.** The screen is idle after no pointer-down and no key-down anywhere in the activity
   window for:
   - `BACKDROP_HOME_IDLE_MS` (60 s) on Home;
   - `BACKDROP_GAME_IDLE_MS` (20 s) on the game screen.

   The clock's rate then eases from 1 to 0 over `BACKDROP_SETTLE_MS` (2 s, `FastOutSlowIn`): every
   wave and light decelerates to a stop where it is. The gel sheen does not decelerate: it is not
   drawn from the frame the settle begins, and returns only once a wake has brought the clock back
   to Running (B-22a). After that, neither the backdrop nor the sheen requests a frame.
3. **Wake.** The next pointer-down or key-down eases the rate from 0 to 1 over the same 2 s. The
   observer reads events at the `Initial` pass and **never consumes** them, so no tap, drag or
   long-press on the board, a tile or a control is delayed or changed.
4. **Navigation** to another density restarts the idle timer, and that density's delay applies.
5. **Battery saver.** While `PowerManager.isPowerSaveMode` is true (followed through
   `ACTION_POWER_SAVE_MODE_CHANGED`), the backdrop draws its rest pose and runs no clock, exactly as at
   animator scale 0.
6. **Frame rate.** A slow background needs no more than 60 fps. Where the platform or Compose offers
   a frame-rate hint, the tech-lead may lower the backdrop's. This is measured at the
   `compose-performance-audit`, not assumed.

**What it costs while it moves.**
- **Per frame:** Landscape draws 2 wave fills, 2 lips, 2 sheens and up to 9 light discs, plus the two
  tiles' sheen while it passes. Quiet draws 2, 2, 2 and up to 5.
- **Nothing more:** the sky is in its own layer and is not re-recorded, and nothing recomposes,
  because every read is in the draw phase.

**Why these values.**
- **Home's 60 s:** a player reading the ribbon or choosing a mode spends seconds there, and a phone
  left on Home is still within a minute.
- **The game's 20 s:** a player who has thought about a move for 20 s gets a still frame, which is
  also the calmer frame for thinking. The next tap brings the motion back gently.
- **Dialogs:** a dialog's own window does not reach the observer, so the backdrop may settle behind
  an open dialog's scrim. That is harmless.
- **TalkBack:** TalkBack's activation is an accessibility action, not a pointer-down, so for a
  TalkBack user the backdrop settles and stays still, which is the calmer state.

**What the tech-lead rules.** Whether these defaults stand, the delay values, whether battery saver
applies, the frame-rate cap, and how the observer and the clock are hosted. The O-B question (the
orbs' idle frames) is superseded, because the orbs retire. BD-16 (c) is the device check, and its
pass line is written against whatever the ruling decides.

### 6.4 Reduced motion

With the animator duration scale at 0 (`Settings.Global.ANIMATOR_DURATION_SCALE`, read by
`rememberAnimationsEnabled()` in `Clay.kt`):
- every element is drawn at its rest pose, the sheen is not drawn, and no clock exists;
- nothing in the backdrop or the finish requests a frame;
- density changes swap instantly.

Android's "Remove animations" switch sets that scale, and battery saver takes the same path (§6.3).
An in-app motion switch is considered in §13 and not added.

## 7. Cold start

**The bar.** The dispatch's: *nothing on the startup critical path that a static gradient would not
cost.* Under it sit D-03g's hard rules for Home's first frame (`docs/ux-spec.md` §6.5): no blocking
read, no image decode, no asset load, no bitmap, no new SDK call. The bundled type faces are the one
named exception, and this change does not touch them.

**What Home's first frame holds (B-11a; the tech-lead's R3).**

| Element | First-frame cost | Verdict |
|---|---|---|
| L1 sky | one vertical-gradient brush over one window rect: **the static gradient itself** | the bar, by definition |
| The host `Surface` (B-09a, D1) | one solid full-window fill | accepted (R1). Full-window fills go from two today (the window background and the `Scaffold`'s background) to three: the window background, the host `Surface` and the sky |
| Transparent `Scaffold`s | remove the `Scaffold`'s opaque `background` fill | counted in the line above |
| The container finishes (§3.5) | one gradient brush per glossed container and per gel tile, in place of a solid colour; one 1dp stroke and one small path each for rim and lip; a tint swap for the glow | the class of cost the bar allows: a gradient (B-11a) |
| L2 waves, L3 lights, the clock, the gel sheen | **not on the first frame** | none |
| The orbs | retired | today's first frame draws two discs when animations are on. 15e runs with animations off, where arm A draws none either |

**Never on Home's start path, at any frame:**
- a vector drawable or any `painterResource`; a bitmap or a decode;
- a resource or file read; a read of `HomeUiState`, DataStore or Room;
- an SDK call, a `Random` or the wall clock.

The battery-saver read (B-22) happens where the clock would start, from frame 2 on, never on frame 1.
Everything the scene draws is built in memory from §4's constants.

**The deferral.** The tech-lead's R3: one flag, set after the host's first frame through
`withFrameNanos`, provided as a `State` read only by the scene node. The waves and lights are absent
from the first frame and present within two frames (D7; BD-23 (a)). They build their paths and
brushes once, on that frame. A build on frame 2 can still jank the entrance, so BD-23 (c) compares
janky frames against arm A in a separate block with animations on (D8). The `compose-performance-audit`
covers the backdrop before release (rules/compose.md).

**Measurement.** Home's first frame changes, so **criterion 15e runs on this branch**. It uses §6.5's
second-edition protocol, unchanged, under the tech-lead's R3:
- arm A is `git merge-base develop feature/candy-backdrop`, which must contain the phase-15 merge;
- arm B is the branch head;
- each arm is built in its own clean worktree;
- the null control comes first, and blocks are interleaved;
- the host is quiet: the Pixel_9 off and no build anywhere;
- the verdict is PASS, FAIL or VOID, as ruled there.

Criterion 15f (placeholders, no spinner, no reflow) is unaffected. The ≤ 2 s bar on a physical phone
(D-03j) is unaffected too, and still owned by PLAT-08 / phase 14.

**Degradation path, if 15e fails on this branch.** These steps come *before* §6.5's own, and stop at
the first sitting that passes. Each changes only the first frame, and none costs an oracle, a readout
or a legibility rule. Each is a one-line change on R3's flag; none is built in advance.
1. **Flat first frame.** Frame 1 draws L1 as a flat `SkyTop` fill, identical to today's background.
   The gradient arrives from frame 2.
2. **No backdrop on the first frame.** The host draws nothing until the flag flips.
3. **Finishes from the second frame.** The containers draw their plain fills on frame 1 and their
   finishes from the flag on. The gel tiles then change on frame 2, behind an entrance fade that is
   still near zero alpha.
4. Then §6.5's steps 1–4, unchanged.

The waves, the lights and the clock can never be the cause, because they are not on the first frame.
A failure that survives step 3 is not the backdrop's: the tech-lead rules on it with the numbers
before any art is removed.

## 8. Fit with the phase-15 layouts, and responsiveness

The backdrop and the finish change **no layout**. Every position, size, gap, inset and band set the
phase-15 spec rules keeps its value, and BD-19 re-runs those criteria on this branch with **no test
edited**. The phase-15 test sources assert no mode tile's or Continue chip's colour. The one fill test
near this change is `DockOnMarkAndSlashTest` (21e), on the dock's discs, which B leaves unchanged.

### 8.1 Home's board stage (D-03l)

- **The stage stays what D-03l made it:** the one weighted child that may hold anything, decoration
  only, and `0` when the column overflows.
- **Its art is unchanged inside:** the `surfaceContainerHigh` slab, `shapes.extraLarge`, the constant
  English-33 position, the edge rule `min(Sh, Sw, home_stage_art_max_size)`, and the
  `clayEntrance(HERO_ENTRANCE_ORDER)` entrance.
- **Its edge gains the rim, the lip and the glow (B-19).** These are drawing only: no node, no
  semantics. 15r's guard (every node in the stage subtree carries exactly `TestTag`, plus `Shape` on
  the art) is untouched.
- **No wings** (B-12a). 15n–15r stand as they are.
- **The hero row** stays transparent, with no surface of its own, and sits in the calm band (B-20).
- **`HomeBlobs`** is removed from Home's column and deleted (B-05a).

### 8.2 The gameplay strip and dock (the Q-902 split, D-04e, D-05d)

- **The strip** stays transparent and status-only.
  - Its title sits in the calm band (§5.1).
  - Its back chip and timer pill keep their sizes and take the gloss finish.
  - 18a, 18d and 18e (the title whole at 360dp, the pill's one width, the timer whole at 200%) are
    about text metrics and are untouched; 18b and 18c are re-run.
- **The dock** stays one opaque `surfaceContainerHigh` slab, with a rim, a lip and a glow, and no
  gloss.
  - Its five looks, their ring, fill, shadow, mark and slash are all measured against **the slab**.
    The slab's fill around the discs is flat and unchanged, so 21a–21g, 21f's "≥ 3:1 against the dock
    slab" included, hold by construction.
- **The board region** between them is transparent, as today.
  - The slab is centred in it.
  - The rescue banner overlays the slab's lower edge on its own `errorContainer` surface, now with a
    rim and a lip.
  - The Challenge loading spinner sits on the sky, which no light crosses there (§5.1).

### 8.3 Insets, edge to edge, safe drawing

As §5.5:
- The backdrop is outside every inset, under both bars and any cutout.
- The content applies each bar once, exactly as built: Home, gameplay and the four top-bar screens
  through the `Scaffold`'s content padding, and the tutorial through `safeDrawingPadding()` on its
  column.
- `SkyTop` makes the status-bar area flat `background` on every screen (B-09b), and the navigation
  bar's contrast scrim stays enforced.
- No scroll container draws under a transparent top bar (the tech-lead's R1).

### 8.4 Windows

| Window | Home (Landscape) | Gameplay (Quiet) | Sparse screens |
|---|---|---|---|
| 360×640dp, three-button bar (the design baseline's small phone) | Compact, and the stage is 0. The crests (314dp, 397dp) lie behind the compact lower group. The lane lights show in the hero band and the side margins, and the waves in the gaps and the footer | The slab spans 120–448dp. The back crest sits 13dp below the slab's foot, and the front crest behind the dock's top | the wash |
| 360×800dp | compact; the stage is ≈ 84dp, the art drawn, the lights beside it | the slab is width-limited, with more of the waves below it | the wash |
| 412×915dp, three-button bar (the 6.7-inch baseline) | The stage is 182–302dp, and the lane lights H1 and H5 stand clear in the wings beside the art. The crests (448dp, 567dp) lie behind the ribbon and the chips | The slab spans 231–611dp, with both glossy crests in the 168dp band above the dock | the wash |
| 411×923dp Pixel_9, 54dp status bar, 24dp gesture bar (the QA device) | as the baseline, everything ≈ 30dp lower | the crests at 665dp and 738dp | the wash |
| 800×1280dp tablet | The column is capped at 480dp and centred. The 160dp margins show the waves' full sweep and the lane lights at 0.08 W, 0.22 W, 0.76 W and 0.92 W | the slab capped at 480dp; the crests at 922dp and 1024dp | the wide wash |
| Any other window, 320–1280dp wide, aspect ratio 0.4–2.2 (a foldable, a split screen, a landscape-shaped window, although landscape is not a supported orientation: `docs/ux-spec.md` §12) | Sky and waves show no gap at any pose. A light may leave the window at a side edge. B-21's title-reach guarantees are stated for windows ≥ 640dp tall, which includes all four BD-11 windows; a shorter window keeps the title at rest in the calm band | the same; no two lights overlap at any height | the same |

Positions scale with the window, while light sizes, wave bobs, lips and sheens are fixed dp. A light
is the same physical size on every phone.

### 8.5 At 200% font scale

| Screen | What changes | Why it is still right |
|---|---|---|
| Home | the title wraps, the column scrolls, and the stage shrinks or goes to 0 | B-21 bounds the wrapped title's reach. The gel tiles' text still starts at 16dp, on deep gel (§3.5.2) |
| Gameplay | the strip grows to ≈ 64dp | still above the calm end (0.14 H) |
| Tutorial, catalogue, Results, Settings, Licences | the columns and grids scroll further | Sparse is zone T everywhere |
| Glossed pills and chips | they grow, and their gloss's fade grows with their height | the dark gloss is safe at full strength (§5.3); light only raises contrast |
| The backdrop itself | nothing (BD-20) | it reads no font size |

### 8.6 Right to left, dark theme

The scene, the waves' bob and the gel sheen mirror in RTL (B-15): the lane lights at 0.08 W and
0.22 W then stand beside the right-hand edge. Dark is B's deep palette, with every §5 rule computed
for it.

## 9. Assets and licences

| Asset | Kind | Source | Licence |
|---|---|---|---|
| The sky, the two waves (fills, lips, sheens), the lights | drawn: Compose `Canvas` from §3's geometry and §4's tokens; no asset file | in-house, this repository | project-owned |
| The gloss finish and the gel tiles | drawn: brushes and strokes on the containers (§3.5) | in-house | project-owned |
| The Classic tile's footprint, the Challenge bolt | the existing in-house footprint drawing and the existing local `ImageVector`, recoloured | in-house | project-owned |
| The look itself | the board's direction B (`docs/ux/backdrop-directions.html` §b), drawn by this studio | in-house | project-owned |

**No third-party asset, no file, no raster, no font, no sound.** THIRD_PARTY.md needs **no §2 row**
for the backdrop. The backdrop plays no cue and changes nothing in `docs/audio-spec.md`
(rules/audio.md: nothing plays that the spec does not name). The one new asset file on this branch is
`assets/licenses/Apache-2.0.txt`, and it belongs to the licences screen (D5, BD-04).

**THIRD_PARTY.md §1.** The research-analyst's idea row is already written on
`feature/phase-14-release-docs` (THIRD_PARTY.md:79 in the `PegSolitarie-r5` worktree). It describes a
"candy-themed backdrop … sweets landscape", and its "What was taken" cell should now describe B.
Proposed wording, for the research-analyst (the text) and the release-manager (the file) (§14 item 4):

> *The idea of an animated, glossy, "alive" backdrop behind the menu and the game, after the aliveness
> of Candy Crush Saga: a deep violet gradient with glossy waves, soft rising lights, a gloss finish on
> panels and gel-style mode cards, all drawn in-house. No name, asset, colour, shape, layout or art
> was taken.*

The row must be on `release/3.0.0` exactly when this branch is. If this branch misses the release,
the row is deleted at checklist step 1.4, as the row itself says. Its Credit cell keeps it **out of
the in-app licences screen**, and `docs/ux/licences-screen.md` obeys.

**Pending IP confirmation** (§14 item 1). The verdict's free lists name soft blobs, glossy
highlights, specular dots and translucent panels. Waves of colour, out-of-focus lights and gel-style
cards are not named word for word. The research-analyst's one-line confirmation is running in
parallel. This document assumes it, and the build does not merge without it.

**Repository hygiene.** No King screenshot, logo or marketing art enters the repository: not in
`.planning/`, not in `.planning/qa/`, not as a reference, an underlay or a comparison (verdict §3b).
This document was written without one, and reviewers check it without one (BD-02).

**Store.** The listing screenshots are recaptured after the backdrop lands, as plain captures of the
app with no added art or framing (Q-016; verdict §3d). That is the release-manager's work.

## 10. Words, shapes, palette and store — the verdict applied

Every line of the verdict's §3, against this design. A reviewer ticks each one on the diff and on the
qa-engineer's device captures (BD-02, BD-03).

| Verdict line | This design |
|---|---|
| **3a.** No `Candy Crush`, `Crush`, `Saga`, `Sugar Crush`, `Candy Land`, `Candyland`, `Tiffi`, `Toffee`, `Yeti`, `Odus`, `King`, `Activision`, `Microsoft` | The backdrop and the finish add no string (B-08). The licences screen's strings are checked by the same grep, and by its JVM test (BD-01) |
| **3a.** No `candy` in user-facing text or the listing | None added. New identifiers avoid "candy", "sweet" and "jelly" (B-10, B-10a) |
| **3a.** No `Sweet!` / `Tasty!` / `Delicious!` / `Divine!` / `Sugar Crush!` callouts | The backdrop has no text and no sound |
| **3a.** No reference to Candy Crush, King or "games like" it, anywhere user-facing | None. The THIRD_PARTY.md idea row stays out of the app (§9) |
| **3a.** Internal names may stay | `drawCandyPeg` and "Candy Clay" are untouched. Nothing new uses the words |
| **3b.** None of the six regular candies, and never a colour-coded set | No confectionery shape exists in B. The scene is two waves and round soft lights in the two house hues. The gel tiles are two labelled rounded rectangles, one violet and one pink: never six, never a token, never a game piece |
| **3b.** No directional stripes, no twist-wrapped square with a glow, no chocolate ball studded with sprinkles | Each lip is one band, and the sheen is one band per pass. Nothing is wrapped. No light carries a dot, so nothing is studded. Nothing is brown, and nothing is a ball with a pattern on it |
| **3b.** No lollipop used as a hammer or a tool | No lollipop exists |
| **3b.** No character or mascot, no face | None (§3.2's exclusions) |
| **3b.** No winding path of level nodes through themed episodes | No path, trail, signpost, node chain or numbered marker. The lights rise straight up, never along a trail |
| **3b.** Not the board on a dark translucent grid of rounded tiles; keep the clay slab | The opaque clay slab, unchanged inside (B-06a, §5.2, BD-18). The deep violet world is behind the slab, never under the board as a grid |
| **3b.** No chunky candy-textured or striped lettering | No lettering in the scene. The title takes colour-vision.md's `TitleGradient`, with no outline and no texture |
| **3b.** Nothing traced, eyedropped or laid out over a King image; no King image in the repo | Every value comes from the studio's own board and the house palette (§4.1's derivation columns), under §9's hygiene rule |
| **3b.** Do draw: soft blobs, glossy highlights, specular dots, translucent panels | B draws glossy highlights (the lips, the sheens, the gloss) and soft round lights. Its waves and gel cards are covered by the pending one-line confirmation (§9) |
| **3b.** Keep motifs low-contrast behind the board | The scene is behind and below the opaque slab. Nothing lies beside the pegs (§5.2) |
| **3b.** Drift only when the animator duration scale is above 0 | §6: the rest pose at scale 0 |
| **3b.** The peg board stays the hero of the game screen, the Home tiles the hero of the menu | §5.2: the slab is untouched and the gel tiles are Home's boldest objects |
| **3c.** Colours from `Color.kt` tokens, light and dark, derived from violet / pink / gold / lavender; violet leads | §4.1. Violet leads: the deep sky, the waves' bodies and the Classic gel. Pink seconds. Gold stays out for its reward meaning |
| **3c.** Never eyedrop from King's art; never six colours with six shapes | §4.1's derivation columns; BD-08's hue arc |
| **3c.** Accessibility unchanged | §5; BD-12, BD-13, BD-17 and BD-27 |
| **3d.** Title `Peg Solitaire`; plain-capture screenshots; the English-33 icon | unchanged; §9 |

**This role's own reading of the two-question test.** This is a pre-assessment only: under standing
rule 1 the binding answers are the reviewers' (BD-02).
1. **Could any single element be traced to a King design?** No. Each element is a generic finish of
   casual games and UI kits, drawn from primitives stated here in the house palette: a vertical
   gradient, two elliptical waves with a highlight band, out-of-focus discs of light, a gloss gradient
   on a panel, and two gradient cards with a shine.
2. **Would a player think it is Candy Crush?** No. The screens are a peg-solitaire cross on a
   lavender clay slab in a deep violet world. There is no match-3 grid, none of the six candies, no
   special candy, no character, no map, no callout and no King lettering.

## 11. Acceptance criteria

**Who checks what.**
- **Auditor:** the code-auditor, on the diff, the Check, and the qa-engineer's device captures. There
  are no stored goldens on this branch (D4).
- **QA:** the qa-engineer, hands-on on a device. The device is the Pixel_9a, or the Pixel_9 with the
  deviation recorded, plus a 360×640dp window (`wm size 1080x1920` + `wm density 480`). The matrix:
  - light and dark; font scale 1.0 and 2.0;
  - animator scale 1 and 0; battery saver off and on;
  - TalkBack; three-button and gesture navigation;
  - the RTL pseudo-locale `ar-XB`.
- **Test:** a named automated test (§12), deterministic. The kinds (JVM, Robo, Compose, Pixel) are
  the tech-lead's R5.

"At rest" means animator scale 0. "In motion" means scale 1, sampled at three moments at least 3 s
apart. **Rewritten** and **amended** mark the criteria this edition changed. The others are the second
edition's, with only their layer and element names updated.

| # | Criterion | By | Pass line |
|---|---|---|---|
| **BD-01** | **Words grep** (amended: D9's JVM test; "jelly" added to the diff grep, B-10a; 2026-11-01: `docs/store/README.md` excluded from commands 1 and 2, see below the table). The three commands under this table, run from the repository root, plus `UserFacingTextHasNoProtectedNamesTest` over `src/main/res/values*/strings.xml` and `src/main/assets/**` on every Check | Auditor, QA, Test | all three print **zero lines**, and the test is green. Play Console names, which no grep reaches, are the release-manager's (§14) |
| **BD-02** | **The two-question test** (amended: D4). It is run on every capture: Home (412×915dp with no chip, one chip and both chips; 360×640dp; the tablet), gameplay (Classic English-33 mid-game; a Challenge level), tutorial step 1, the catalogue, Results, Settings and Licences, in light and dark. Each is captured at rest, and Home and gameplay also in motion, with one Home capture taken while the sheen passes | Auditor (on QA's captures, listed by path in QA's report), QA | For every capture, each reviewer writes Q1 "no" and Q2 "no", with one line of reason. **Any "yes" is a FAIL**, routed through the producer to the research-analyst. Reviewers answer from the verdict's §1b list and their own knowledge: no King image is fetched, viewed side by side or stored, in the repo or in `.planning/qa/`. The auditor's line is sequenced after QA's capture session |
| **BD-03** | **Shapes and finish checklist** (rewritten). §10's rows, ticked on the code and on the captures. The scene holds exactly the sky, two waves (each a fill, a lip and a sheen) and the density's lights (9 / 5 / 0), and no other primitive. Every light is a plain radial disc (core, rim, fade), with no dot and no stroked ring. The gloss finish is exactly §3.5.1's table: no gloss on the slab, the stage art, the dock slab or the banner, and the dock's discs untouched. The gel tiles are exactly §3.5.2: one colour family each, no texture, no wobble, the sheen inside the lip only | Auditor | every row ticked. There is no confectionery motif, sprinkle, star, sparkle, bubble with a dot, character, lettering, path or grid anywhere in the scene or the finish. There is no red, orange, yellow, green, blue, brown or gold pixel in them. There is no "jelly" in any new identifier, KDoc, test tag or string |
| **BD-04** | **No asset, no dependency** (amended: D5). There is no new file under `res/drawable*`, `res/mipmap*`, `res/raw` or `assets/`, except `assets/licenses/Apache-2.0.txt` (sha256 `cfc7749b…3d30`, LS §7) and an `assets/licenses/<name>-MIT.txt` only if the release listing proves an MIT artifact ships. There is no image file (`*.png *.jpg *.jpeg *.webp *.gif *.svg *.avif`) anywhere in the diff, and no new entry in `gradle/libs.versions.toml` or a module build file | Auditor | `git diff --stat develop...HEAD` shows none beyond the carve-out |
| **BD-05** | **Tokens only** (amended: `BackdropMotion`). There is no hex outside `Color.kt` and no dp/sp literal (`dpSpLiteralCheck` green). Every ratio, anchor, count and list is in `BackdropMetrics`. Every duration is in `ClayAnim` (app-wide) or `BackdropMotion` (the backdrop's). detekt and ktlint are green, with no new `@Suppress` | Auditor, Check | the Check is green, and a grep of the new files for `0x`, `.dp` and `.sp` literals is empty |
| **BD-06** | **Density per destination**: Home is Landscape; gameplay is Quiet, for Classic and for Challenge; the tutorial, catalogue, Results, Settings, Licences and Licence text are Sparse. A restore case (R5): a restored back stack whose top is `Gameplay` is Quiet on the first frame | Test, QA | as listed |
| **BD-07** | **Layers per density** (rewritten). Read from the backdrop's `backdropLayers` property: Landscape {Sky, Waves, Lights}; Quiet {Sky, Waves, Lights}; Sparse {Sky}. The model's light count is 9, 5 and 0 | Test | as listed, on the three reference windows and in Home's three chip states. No stage-wing node or tag exists anywhere in the code |
| **BD-08** | **Colour constraints**, computed from `Color.kt` in a JVM test (the dark clause rewritten: B-14a replaces "every dark colour ≤ 0.0095 or ≥ 0.165"). Every constraint of §4.1: `SkyTop` equals `background`; zone T's L bounds; the sky order; Sparse's side of the background, sampled at 11 points; every lane light's rim composite at 0.47 H (≥ 0.503 light, ≤ 0.0786 dark); the rim rule's floor for every container in §5.2's table, by the √ formula; the gel text pairs and their channel monotonicity; the dark gloss under `onSurfaceVariant` ≥ 4.5:1; and every opaque or tinted new token's hue within 250°–336° | Test | all hold |
| **BD-09** | **The status strip is `background`** (amended 2026-11-01: B-09b; the earlier "pixel-identical to `develop`" contradicted BD-14 on the four top-bar screens). On Home, gameplay, the tutorial, the catalogue, Results, Settings, Licences and Licence text, in light and dark, at rest and in motion, on a settled screen at the top of its scroll: every pixel of the band from the window's top edge to `statusInset`, outside the system's clock, icon and cutout boxes, is compared against the theme's `background` (`BackdropSkyTop`), not against `develop`. The status-bar icons are sampled at their glyph cores against the strip | QA, Test | Every compared pixel within ±1/255 per channel of light `#F4F1FA` / dark `#1B1626`. The icon appearance is dark in light and light in dark, as on `develop`. The clock digits read ≥ 4.5:1 and every status icon ≥ 3:1 against the strip, in both themes. It applies on windows whose `statusInset` ≤ 0.12 H, which every QA window meets |
| **BD-10** | **Composition constants** (rewritten) match §3.3, §4.3 and §4.4: every sky stop; each wave's `cx`, crest, `WAVE_RX`, `WAVE_RY`, depth, body stop, alpha, bob tokens and half-cycle; each light's `x`, `y`, size, kind, `φ0`, opacity, set and period; the calm ends, `TITLE_REACH` and the spinner band; the sheen constants; the idle and settle constants; the list lengths 9 and 5; and every loop ≥ `MIN_LOOP_MS` | Test | equal |
| **BD-11** | **Geometry and pose rules** (rewritten), on the pure model with `BackdropDimens` resolved from real `Resources` at `w360dp-h640dp`, `w412dp-h915dp`, `w800dp-h1280dp` and `w1280dp-h800dp`: (a) each Landscape lane light's, each Quiet upper light's and each Quiet lower light's x-extent is pairwise disjoint within its set; (b) every Landscape low light's highest top edge is ≥ 0.47 H; (c) every Quiet upper light's lowest bottom edge is ≤ 0.33 H, and every lower light's highest top edge ≥ 0.59 H; (d) no wave pixel lies above 0.47 H on Landscape at any pose, so the crest minus its bob is ≥ 0.47 H; (e) at every pose both waves span the full window width and reach below its bottom edge; (f) for every light and phase, the alpha is 0 whenever the top edge is at or above the calm end, and never above `opacity × (topEdge − calmEnd) / calm fade`; (g) the rest poses are as §3.4, the envelope is 0 at the wrap, and every drifting speed is ≤ 8dp/s; (h) `gel_top_band` ≤ `ModeTile`'s top content padding; (i) in RTL the model is the exact mirror | Test | all hold. (a)–(c) are phase-independent, because lights move only vertically, so they are proved exactly, not sampled |
| **BD-12** | **Text contrast on the device**: every uncontained text in §5.1's table, sampled against the backdrop directly behind it at rest and in motion, at the top of the scroll and scrolled to the end, at 1.0 and 2.0, in light and dark, at 360×640dp and on the Pixel_9 | QA | Body and label text ≥ 4.5:1. The Home title ≥ 4.5:1 at rest at 1.0, and ≥ 3:1 for every sampled glyph pixel when wrapped |
| **BD-13** | **Hero edges** (amended pass line: B-14a). Sampled just outside and just inside the edges of the board slab, the dock, the back chip, the timer pill, the stage art, the gear chip, the ribbon pills, both gel tiles, the Continue chips and the rescue banner, in light and dark, at rest and in motion | QA | Every reading is ≥ its §5.2 floor: the clay family and every light-theme container ≥ its `develop` value; the dark Continue chips, the dark rescue banner and both gel tiles ≥ 1.20:1. (The second edition's line was "every ratio ≥ its `develop` value".) |
| **BD-14** | **Transparent containers** (amended: D1). Every `Scaffold`, the four `TopAppBar`s (resting and scrolled colours) and the tutorial's root `Surface` paint no colour. The host's window-filling `Surface` exists, and unlabelled text takes `onBackground` in both themes (F-32) | Test, Auditor | as stated |
| **BD-15** | **Semantics.** The backdrop node carries its tag, `backdropDensity`, `backdropLayers`, and `backdropMotion` if the tech-lead adopts it (§12). It has no description, no click, no focus and no `pointerInput`. The finish and the gel add no node and no property. Home's, gameplay's and the tutorial's TalkBack traversal orders are exactly `docs/ux-spec.md` §11's | Test, QA | as stated. A TalkBack pass on the device finds no new focus stop on any screen |
| **BD-16** | **Motion** (rewritten: B-05a, B-22). (a) **At rest:** with the scale at 0, every element is at its rest pose and the sheen is not drawn. Over 5 s idle on Home, gameplay and a Sparse screen, the device's `dumpsys gfxinfo` frame count is no higher than `develop`'s for the same screen in the same state. (b) **In motion:** Home moves exactly the 2 waves, the 9 lights and the 2 sheens; gameplay exactly the 2 waves and the 5 lights; Sparse nothing. Only translation and alpha vary. One frame source drives it. Scrolling Home at a fixed clock time leaves the backdrop's pixels unchanged. (c) **Settle**, per the tech-lead's idle-frame ruling (B-22's defaults until it lands): after the idle delay plus 2 s with no input, the frame count over the next 10 s is no higher than `develop`'s for the same screen in the same state (the timer's ticks only, on gameplay). One tap restarts the motion within 2 s, and that tap reaches its target unchanged. In battery saver the backdrop stays at rest. **Sheen at a settle** (added 2026-11-01, B-22a): a settle that begins inside a gel pass ends that pass at once; no sheen is drawn while Settling or AtRest, by B-22a's same-run comparison and its red | Test (pose model, test clock, the board's tap test with the observer present, B-22a's sheen comparison), QA (frame counts, battery saver; on Home, after a settle, no glint on either tile's lip) | as stated |
| **BD-17** | **The board untouched, pixel for pixel** (D4: an in-run comparison, no file). `GameScreen` rendered with and without the backdrop, in light and dark, mid-game with a selection, legal-target dots and a hint showing: the slab's interior, inset 2dp from its edge, is identical. Likewise the interiors of the Home stage art, the gel tiles (clock fixed, no sheen passing) and the Continue chips | Test | zero differing pixels |
| **BD-18** | **What sits under the board** is the opaque `surfaceContainerHigh` clay slab at full alpha, with its rim, lip and glow, and nothing else: no grid, no translucency, no blur, no gloss | Test, Auditor | as stated |
| **BD-19** | **Layouts unchanged** (amended 2026-10-31: B-19a). The phase-15 criteria 15g, 15j, 15k, 15m, 15n–15r, 18a–18e and 21a–21g re-run green on this branch, **with no test edited**, except 15m's pixel oracle, which B-19a amends and the code-auditor approves on its reds. On the device, 18b's inset clause holds. With three-button navigation the system's scrim is visible over the waves, and with gesture navigation the handle is legible | Test, QA | all green; the scrim and the handle present |
| **BD-20** | **Font independence**: the backdrop alone, rendered at font scale 1.0 and 2.0 on the same window at the same clock time, is pixel-identical | Test | identical |
| **BD-21** | **The sky is continuous across navigation** (amended: D6). On Home → catalogue → gameplay → Results → Home, with the test clock paused at a fixed backdrop time, every frame of every transition is rendered. Pixels are sampled where no stand-in draws and away from every wave and light: the calm band, and the sky between the band and the back crest | Test, QA (a screen recording) | at every frame, each sampled colour lies on the segment between that pixel's outgoing and incoming colours, within ±2/255 per channel: only a convex blend is allowed. The sky never lightens toward `background` |
| **BD-22** | **RTL** (amended: D4). In `ar-XB` the scene, the waves' offsets and the sheen's travel are the mirror of LTR. An in-run comparison at a fixed clock time, under a stated tolerance constant, must fail on a deliberately unmirrored scene | Test, QA | mirrored |
| **BD-23** | **Cold start** (amended: D7, D8). (a) After Home's first frame, `backdropLayers` = {Sky}, no wave or light is drawn and the clock has not started; Waves and Lights are present within two `advanceTimeByFrame()`. (b) 15e on this branch, by §6.5's protocol under R3, is **PASS**; a VOID sitting is not a pass. (c) A separate block after 15e, animations **on**: ≥ 10 kept samples per arm, interleaved A/B in blocks of five with the first of each block discarded, reading "Janky frames" one second after `am start -W` returns | Test, QA | (a) as stated; (b) PASS; (c) `median_B ≤ median_A + 1` |
| **BD-24** | **Greyscale.** Device captures of Home, gameplay and Settings, converted to greyscale, keep every BD-12, BD-13 and BD-27 ratio. Pegs remain distinct from holes, and the two gel tiles are told apart by their labels and glyphs | QA | as stated |
| **BD-25** | **Previews** (amended: D4; the goldens are struck). Every new public composable has light and dark `@Preview`s: each density, the gel tiles, the glossed pills and chips. Screen previews go through `BackdropPreviewHost` with the clock fixed | Auditor | present |
| **BD-26** | **THIRD_PARTY.md.** On the release branch the §1 idea row is present and its "What was taken" cell describes direction B (§9). No §2 row was added for the backdrop, and the licences screen does not list the row | Auditor, release-manager | as stated |
| **BD-27** | **Text on the gel tiles and the glossed panels, on the device** (new). Sampled at rest, during a sheen pass, and while pressed, at 1.0 and 2.0, in light and dark, at 360×640dp and on the Pixel_9: the white text and badge glyphs on both gel tiles, the level chip, the ribbon labels and values, the Continue chips' text, and the timer digits. The tiles' title line box is also checked | QA | Text ≥ 4.5:1 and glyphs ≥ 3:1 (§5.3). At 2.0 the title's line box starts ≥ 16dp below the tile's top edge (semantics bounds or the layout inspector) |
| **BD-28** | **IP confirmation** (new). The research-analyst's one-line confirmation of the waves, the lights and the gel cards exists in `.planning/reports/candy-backdrop/` | Auditor | present and "confirmed". A "not confirmed" sends that element back to this role before the merge |

BD-01's commands. `docs/store` may not exist yet on this branch; grep then reports it missing, which
is not a hit.

```sh
grep -rniwE --exclude=README.md \
  'candy|candies|crush|saga|candyland|tiffi|toffee|yeti|odus|king|activision|microsoft' \
  app/src/main/res/values*/strings.xml app/src/main/assets docs/store
grep -rniE --exclude=README.md '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store
git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'
```

**The README exclusion (2026-11-01, a UX decision on the code-auditor's finding 7).**
`docs/store/README.md` is the store folder's working notes: it names this branch and quotes the
verdict's word rules so that whoever writes the listing can follow them. It ships nowhere, and while
it quotes the rules, command 1 can never print zero lines. Every other file under `docs/store/` is
listing copy and stays in scope. `--exclude=README.md` matches by file name, so it would also skip a
`README.md` under `app/src/main/res` or `app/src/main/assets`. A README is never text the app shows,
and `UserFacingTextHasNoProtectedNamesTest` still scans every file under `assets/`. The listing text that does ship stays covered by both commands,
and the Play Console names stay the release-manager's (§14 item 8). Command 3 is unchanged: it prints
zero lines over this diff because `UserFacingTextHasNoProtectedNamesTest` stores its word list
reversed, which the code-auditor accepted (finding 6).

## 12. Tests owed

Deterministic only (rules/testing.md): no wall clock, no unseeded random, no sleeps.
- Transitions run on the Compose test clock, and poses are set by setting the backdrop clock's time.
- The system-bar insets are supplied per the phase-15 spec's §13 rule.
- Names read as sentences, and each test is watched failing once before it is trusted.
- There are no stored goldens. Pixel tests compare two renders inside one run (D4).

**The test hook** (the tech-lead's R1). One semantics node on the backdrop's root, in the
`PegCountKey` idiom (`GameScreen.kt`), carries:
- its test tag;
- `backdropDensity`;
- `backdropLayers`, the set drawn, from {Sky, Waves, Lights};
- proposed for the tech-lead: `backdropMotion`, from {Running, Settling, AtRest}, so that BD-16 is
  assertable without counting frames.

TalkBack reads none of them. Nothing is added to the stage or to the tiles (D3's reasoning holds for
any node in the stage subtree).

| Test (illustrative names) | Kind | Covers |
|---|---|---|
| `BackdropColorsTest`: "zone-T colours read 4.5:1 under every text role in both themes"; "the sky's top colour is the background"; "every Sparse colour stays on the background's side"; "a single light never breaks the title's bounds above 0.47 H"; "every hero container's edge floor holds against any colour"; "white text reads 4.5:1 on the gel from its top band down"; "every new hue lies in the violet–pink arc" | JVM | BD-08 |
| `BackdropMetricsTest`: "each density's sky stops, waves and lights are the spec's"; "every loop is at least nine seconds" | JVM | BD-10 |
| `BackdropGeometryTest`, at the four windows: "lane lights never overlap"; "low lights never rise into the title's reach"; "no game light crosses the spinner's band"; "no wave rises above the title's reach at any pose"; "a light is invisible before it reaches the calm band"; "the waves always cover the window"; "the gel's text band fits inside the tile's padding" | Robo | BD-11 |
| `BackdropPoseTest`: "a wave swings between its bob limits and is still at rest"; "a light wraps only while invisible"; "the sheen passes only in the last quarter of its loop"; "the rest pose is the reduced-motion picture" | JVM | BD-11 (g), BD-16 (a), (b) |
| `BackdropDensityTest`: "Home draws the landscape, the board the quiet scene, every other screen the wash", with R5's restore case | Compose | BD-06, BD-07 |
| `BackdropSemanticsTest`: "the backdrop is not focusable, clickable or announced"; "the gel tiles announce exactly what the clay tiles did"; "TalkBack order is unchanged on Home, gameplay and the tutorial" | Compose | BD-15 |
| `BackdropReducedMotionTest`: "with animations off every element is at rest and no clock runs" | Compose | BD-16 (a) |
| `BackdropSettleTest`: "the backdrop settles after the idle delay"; "a tap wakes it and still reaches the board"; "a settle that begins mid-pass ends the gel sheen" (B-22a) | Compose, test clock | BD-16 (c) |
| `BackdropFirstFrameTest`: "Home's first frame draws only the sky" | Compose, on the real host | BD-23 (a) |
| `BackdropFontScaleTest`: "the backdrop is identical at 1.0 and 2.0 font scale" | Pixel | BD-20 |
| `BoardInteriorIdentityTest`: "the slab's interior is identical with and without the backdrop", and the same for the stage art, the gel tiles and the chips | Pixel | BD-17, BD-18 |
| `TransparentContainersTest`: "every scaffold, top bar and the tutorial root paint no colour, and unlabelled text is onBackground in dark mode" | Pixel, Compose | BD-14 |
| `SkyContinuityTest`: "a destination change only ever blends the sky" | Pixel, test clock | BD-21 |
| `BackdropMirrorTest`: "the scene in RTL is the mirror of LTR" | Pixel | BD-22 |
| `GelTileTest`: "the tile's text starts at or below the gel's top band at every font scale"; "pressing a gel tile darkens it" | Compose | BD-27 (the layout half), BD-15 |
| `FinishRecipeTest`: "the gloss lies on the chips and pills and never on the slab, the stage art or the dock", a recipe test in the `DockLookRecipeTest` idiom over §3.5.1's table | JVM | BD-03 (an aid to the auditor) |
| `UserFacingTextHasNoProtectedNamesTest` | JVM | BD-01 |
| The phase-15 suites for 15g, 15j, 15k, 15n–15r, 18a–18e, 21a–21g | existing, unedited | BD-19 |

Every screen keeps its one Compose UI test of its main flow (rules/compose.md). None needs a change
for the backdrop beyond running with it present. A suite that renders a whole screen and reads pixels
next to a container renders that screen in `BackdropTestHost`, at the screen's density, with the
clock fixed (R5). If a ratio assertion then fails, that is a BD-13 finding for this role through the
producer, never a re-baselined number.

## 13. Considered and rejected

So that none of these is re-proposed as an improvement.

**Kept from the second edition, still rejected:**
- **Vector drawables** for any element: an XML inflate on Home's first use, against D-03g (B-02).
- **Kenney's CC0 pack, or AI-generated art**: a style mismatch and the app's first raster in one case;
  outside the licence matrix, probably uncopyrightable, and the likeliest way to reproduce King's look
  in the other (verdict §4b, §4c).
- **A saturated backdrop with every text on a pill**: it would restyle five approved screens to buy
  colour where the founder's eye is not (B-03).
- **A per-mode tint** for Challenge (B-07). **A different scene per board, per level or at random**:
  screenshots would stop being reproducible, and the two-question test could not be run once for all.
- **A translucent slab, a backdrop seen through the board, a blurred or frosted panel behind it, a
  dark grid of rounded tiles under it**: the verdict's banned board treatment, or a cost to the hero
  for nothing.

**Withdrawn with the sweets landscape** (§16): every standing sweet and its placements, the stage
wings, the hills, frosting, drips, clouds and sprinkles. B has no motif.

**Reversed:** the second edition rejected "parallax, drifting clouds, twinkling or falling sprinkles,
a swaying lollipop" as motion without meaning. The founder asked for aliveness, so slow periodic motion
behind opaque containers is now the design (B-05a). Parallax, scroll-linked motion and tilt stay
rejected.

**New with direction B:**

| Considered | Why not |
|---|---|
| The board's **veil**: a full-width gradient drawn over the scene | An extra full-window fill on every frame. The calm fade builds the band into the poses, and a test can check it (B-20) |
| The board's **sheen across the whole gel card** | White text at its centre reads ≈ 3.1:1 during each pass (B-18) |
| The board's **light top third of the gel** (`GelMid` at 36% of the height) | A 200% label would sit on it at ≈ 3.8:1 (B-18) |
| A **white pressed state** on the gel | 4.46:1 under white text while pressed. The state layer darkens instead (§3.5.2) |
| **Gloss on the dock's discs** (the board's) | The On disc's top reads ≈ 2.4:1 against the slab, failing 21f, and the discs are the dock's state language (§4.4.1 of the phase-15 spec) |
| **Gloss on the dock slab** | It is 21a–21g's measuring surface around the discs |
| **Gloss on the board slab or the stage art** | It would lie over the board (BD-17, 15q) |
| The board's **10% dark gloss** | The ribbon labels would read 4.10:1. At 6% they read ≥ 4.66:1 (B-19) |
| The board's **inner bottom shade** on panels | On the timer pill, which has no vertical padding, it would lie under the digits. As a volume cue it adds little over the gloss and the lip |
| **Overlapping lights** where the wrapped title can reach | Two overlapped rims composite to L 0.094 in dark and L 0.44 in light, past both title bounds (B-21) |
| **Lights in the game screen's middle band** | The Challenge loading spinner sits there and needs 3:1 (B-21) |
| **Waves or lights on Sparse screens** | Text sits everywhere there and scrolls. Zone T is the only safe colour, and the frames would buy nothing (B-23) |
| **A darkening Sparse wash in light** | It would cross the cards' own luminance and erase their edges somewhere on the screen (B-23) |
| **The gloss and gel on Sparse screens' cards** | It would restyle five screens the founder did not see on the board, all of them utility screens (B-19) |
| **Keeping the orbs beside the lights** | Two slow layers would mean twice the frame cost and two motion vocabularies (B-05a) |
| **An infinite transition per element** | Many frame callbacks, which no single settle can stop, and poses no test can set (B-24) |
| **A blur or `RenderEffect` pass** | API 31, against minSdk 24 (Q-018). B has no glass that needs one |
| **A band rule on the dark backdrop**, like the second edition's B-14 | B's dark waves pass through every luminance. The rim makes each edge independent of what is behind it (B-14a) |
| **A rim strong enough** to keep the dark Continue chip at 1.90:1 | It needs about 55% white: a bright outline around an amber chip, a bordered look B does not have (B-14a) |
| **An in-app "background motion" switch** | Android's "Remove animations" switch (animator scale 0) is honoured everywhere, battery saver takes the same path, and idle motion settles (B-22). A second switch would duplicate the platform's. It can be revisited if players ask |
| **Waves or lights that react to taps, moves or wins** | Motion with meaning belongs to the board |
| **A wobble or jiggle on the gel cards** | Beyond `claySquish` it reads as a toy, and it is the gesture the word "jelly" brings to mind (B-10a) |
| **Bubbles (C) or glass panels (A)** | The founder picked B (Q-021) |

## 14. Open items routed to the producer

1. **IP confirmation: the research-analyst, running in parallel.** One line confirming that the
   waves of colour, the out-of-focus lights and the gel-style mode cards are free to use. The verdict's
   free lists name soft blobs, glossy highlights, specular dots and translucent panels, not these
   three word for word. This document assumes the confirmation. It does not block this spec. It
   blocks the merge (BD-28). A "not confirmed" sends that element back to this role, and the rest
   stands.
2. **The tech-lead's idle-frame ruling** on B-22 (§6.3): whether its defaults stand, the delay values,
   battery saver, a frame-rate cap, and how the input observer and the clock are hosted. BD-16 (c) is
   written to follow the ruling. The O-B question is superseded, because the orbs retire.
3. **The tech-lead's addendum to R1–R6 for direction B**, before the build, since the developer
   builds to that report where it differs (§15 lists what is void). It needs to rule on:
   - **The pure model:** waves, lights and poses, in place of the mounds, motifs and wings.
   - **The shared clock and its route to the gel tiles.**
   - **The glow's mechanism.** `clayShadow` reads `ClayColors.shadowTint`, at 21 call sites.
     `feature/audio-review`'s wrong-tap work edits `ui/theme/Clay.kt`, so a glow added inside
     `Clay.kt` would make it a fifth shared file under R6. A separate file avoids that.
   - **The gel tile's container:** a transparent `Card` with the gel drawn, and its state layer.
   - **The flat footprint for the Classic tile.**
4. **The THIRD_PARTY.md §1 idea row's wording** (§9): the research-analyst writes the text, and the
   release-manager owns the file, on `feature/phase-14-release-docs` and then `release/3.0.0`.
5. **Q-021's answer in FOUNDER.md.** The dispatch states it ("B · Deep gloss", 2026-10-18). This role
   found it in neither `PegSolitarie-p15/.planning/questions/FOUNDER.md` (its newest entry is Q-020)
   nor this worktree's copy (its newest is Q-007). The producer records it.
6. **The 15e sitting on this branch** (BD-23 (b)), under §6.5's conditions and the tech-lead's R3: the
   Pixel_9a alone, a quiet host, and the audio work idle. The producer schedules it.
7. **Merge order.** This branch builds on phase 15's layouts (the D-03l stage, the D-05d strip and
   dock, the bundled fonts), so it builds after `feature/phase-15-ux-refresh` merges, with `develop`
   merged in first (R6). BD-19 re-runs phase 15's criteria on top.
8. **For the release-manager:**
   - the THIRD_PARTY.md §1 row travels with this branch to `release/3.0.0` (BD-26);
   - the store screenshots are recaptured as plain captures after this lands (Q-016);
   - the Play Console achievement and leaderboard names are checked against the verdict's §3a list,
     which no grep in the repository can see.
9. **Documents this role amends, each on its own dispatch:**
   - after the merge, `docs/ux-spec.md`: §4.2 band 4, the tiles' fills; §6.2's colour roles; §10's "the
     blobs remain the only drifting elements", superseded by B-05a; §11's contrast table, for the gel;
   - after the merge, `docs/ui/DESIGN-SYSTEM.md`: a gel and gloss finish section and the Home note;
   - `docs/ux/licences-screen.md`: the tech-lead's D10 and D11. That file is outside this dispatch.
10. **Done in this dispatch:** `docs/ux/colour-vision.md` §11, the direction-B amendments: TG-AC04's
    target, the gel tiles in §2's audit, and the colour-blind palette beside the gel and the dock.
11. **For the founder's information, no question.** The build differs from the board where a gate
    required it (§13's "new with direction B" table). The dark Continue chip's edge is the one
    measurable cost of the pick (B-14a). The producer may mention both in the next phase summary.

## 15. The tech-lead's architecture ruling, folded in (D1–D11)

`.planning/reports/candy-backdrop/tech-lead-architecture.md` (2026-10-17) ruled on the second
edition. Where it departed from that spec, it governs. This section folds each departure in, and marks
what direction B makes void.

**The departures, D1–D11**

| # | The tech-lead's ruling | Status under direction B | Folded into |
|---|---|---|---|
| D1 | There is no window-filling theme `Surface` today. `AppNavHost` adds one, which is one more solid fill on frame 1 | **folded** | B-09a, §7, BD-14 |
| D2 | One constant, `ClayAnim.NAV_TRANSITION_MS` = 700, on the `NavHost`'s four transitions and the backdrop's cross-fade | **folded** | B-13a, §4.4 |
| D3 | No custom semantics key on the stage: a wings test tag plus the pure builder's model, because 15r's guard admits only `TestTag` (and `Shape` on the art) | **void**: there are no wings. Its reasoning still governs, so the finish adds nothing to the stage subtree | B-12a, §8.1, §12 |
| D4 | No stored goldens. Same-run pixel comparisons for BD-17, BD-20 and BD-22. BD-02's auditor half is answered from QA's device captures | **folded** | BD-02, BD-17, BD-20, BD-22, BD-25, §12 |
| D5 | BD-04's "no new file under `assets/`" exempts `assets/licenses/Apache-2.0.txt`, and a conditional MIT text | **folded** | BD-04, §9 |
| D6 | BD-21 becomes the convex-blend property, sampled away from the orbs and the clouds | **folded**, sampled away from the waves and lights instead | BD-21 |
| D7 | BD-23 (a): absent on the first frame, present within two | **folded** | B-11a, §7, BD-23 |
| D8 | BD-23 (c): a separate block after 15e, animations on | **folded** | §7, BD-23 |
| D9 | BD-01 also as a JVM test over the strings and assets | **folded** | BD-01, §12 |
| D10 | The licence text body is a `Column` in `verticalScroll`, not a `LazyColumn` | **not this file's**: `docs/ux/licences-screen.md`, outside this dispatch | §14 item 9 |
| D11 | No ViewModel for the licences list; one for the text | **not this file's**, as D10 | §14 item 9 |

**The rulings, R1–R6, against direction B**

| Ruling | Status |
|---|---|
| R1: the host, density mapping, two-slot cross-fade, palette holder, host `Surface`, insets, test hook and file shape | **Stands.** Void within it: `BackdropOrbs.kt` and the move of `HomeBlobs` (the orbs retire), `BLOB_PEEK_FRACTION` and `ORB_SECONDARY_Y`, `buildStageWings` and `StageWingsModel`, and the `HomeStage.kt` edit. Changed within it: `backdropLayers`' members (Sky, Waves, Lights), `BackdropGeometry`'s model (waves, lights and poses, §3.4), the token count (§4.1), and the new clock (B-24) |
| R2: the stage wings fit D-03l clause 4 | **Void** (B-12a) |
| R3: the one-frame gate and the 15e sitting on this branch | **Stands.** The gate now holds back the waves, the lights and the clock. The degradation path gains its step 3 (§7) |
| R4: the licences screen | **Stands**, unaffected |
| R5: the test strategy | **Stands.** Void within it: BD-07's wings clause and BD-11's wing and sprinkle rules. BD-11 and BD-16 are rewritten (§11) |
| R6: sequencing, and parallel work with `feature/audio-review` | **Stands.** This edition adds no string, so `strings.xml` is shared only through the licences block. The glow may touch `ui/theme/Clay.kt`, which the wrong-tap work edits (§14 item 3) |

## 16. Superseded content: a dated record

The second edition (2026-10-17, "the sweets landscape") is kept whole in this file's git history on
`feature/candy-backdrop`, in the commit before this edition. Its decisions are in §1.1, word for
word. Its other sections were replaced as follows, on 2026-10-18, by the founder's answer to Q-021.

| Second-edition section | What it specified | Now |
|---|---|---|
| Intro, §0 | the sweets-landscape scope, its inputs, and the list of corrections to the cut-off first draft | replaced by this edition's intro and §0 |
| §2 | the densities, with the hills, the clouds and the sweets in the stage's wings | replaced by §2; the densities and their screens are unchanged |
| §3.1 | layers L1 sky, L2 orbs, L3 scene, W stage wings | replaced by §3.1 (sky, waves, lights) |
| §3.2 | the motif vocabulary (hill mound, frosting lip and drip, cloud, swirl lollipop, plain cane, gumdrop, cupcake, sprinkle, orb) and the one-highlight rule | withdrawn; §3.2 holds B's three elements |
| §3.3 | the sky stops, clouds and hills per density, the stage-wing construction and its table, Quiet's window table, and Q-020's options 2 and 3 | replaced by §3.3; Q-020's options are void (B-16a) |
| §3.4 | sprinkle placement rules 1–6 | withdrawn: there are no sprinkles. §3.4 now holds the poses |
| §4.1 | 12 colour tokens per theme (SkyTop, SkyMid, SkyHorizon, CloudBody, CloudShade, HillFar, HillNear, Frosting, MotifViolet, MotifPink, MotifCream, SprinkleLilac) and the dark-band constraint | replaced by §4.1; the zone-T bounds, SkyTop = background and the hue arc are kept (the arc widened by 1°) |
| §4.2 | 32 dimension tokens for clouds, frosting, motifs, sprinkles and wings | replaced by §4.2 |
| §4.3 | `BackdropMetrics`' mounds, clouds, wings, motif ratios, sprinkles and orbs, and "the backdrop owns no duration" | replaced by §4.3 and §4.4 |
| §5 | legibility: the method, zone T and S, the hero-edge table, colour vision, system bars, TalkBack | the method is kept, and the numbers are re-derived for B; §5.2 is rebuilt on the rim rule, and §5.3 is new |
| §6 | motion: the orbs alone | replaced by §6 (B-05a, B-22) |
| §7 | cold start | kept in substance; frame 1's contents and the degradation path are updated |
| §8 | fit with phase 15 | kept; §8.1's wing bullets are withdrawn |
| §9, §10 | assets, and the verdict checklist, for the sweets | replaced by §9 and §10 |
| §11 | BD-01 to BD-26 | BD-03, 07, 10, 11 and 16 rewritten; BD-08's dark clause replaced; BD-13's pass line amended; the D-folds applied; BD-27 and BD-28 added |
| §12 | the test hook "and one on the stage", and the Roborazzi goldens | replaced by §12 (D3, D4) |
| §13 | considered and rejected, sweets items included | rebuilt in §13 |
| §14 | open items 1–8 | item 1 (the game-screen density question) answered by Q-020 and Q-021; items 2 and 3 specified in `docs/ux/colour-vision.md`; item 4 (hosting) ruled in R1; items 5–8 carried into §14 |
