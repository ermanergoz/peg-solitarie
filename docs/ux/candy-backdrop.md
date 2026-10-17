---
role: ux-designer
task: candy-backdrop
slug: scene-backdrop
status: draft
created: 2026-10-17
revised: 2026-10-17 (second dispatch; finishes and corrects the cut-off first draft)
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
ip-verdict: .planning/reports/candy-backdrop/research-analyst.md (inspiration-only)
layout-contract: docs/ux-spec.md (phase 15, D-03l, §4.2.4, §4.3.1, §4.4.1, §6.5, §10, §11, §12)
companion: docs/ux/licences-screen.md
---

# Peg Solitaire — Backdrop scene (the "sweets landscape")

> The founder's request, verbatim: *"Also for the game and the menu background to be like candy
> crush game."* The research-analyst's verdict on that request is **inspiration-only**
> (`.planning/reports/candy-backdrop/research-analyst.md`, binding): a confectionery theme is an
> idea and is free; King's names, candies, characters, map and art are not, and must be neither
> used nor imitated. This document specifies what the verdict allows, as close to the founder's
> words as it permits: a **bright, playful sweets landscape drawn in the game's own Candy Clay
> style** — a cotton-candy sky, frosted hills with drips and sprinkles, clouds, and a lollipop, a
> plain cane, a gumdrop and a cupcake standing on the hills — behind the menu, a calmer version of
> it behind the game, and a quiet wash behind every other screen, with the peg board and the Home
> tiles kept as the hero objects. It is a design document. It contains no code; the stack's
> developer builds it.

## 0. Scope, inputs and the rules that bind this document

**What this specifies.** One decorative backdrop, drawn in Compose `Canvas` from `Color.kt`
tokens, at three densities, on every screen of the app (§2 says which density where and why); its
layers, motifs and composition as measurable values; its colour and dimension tokens in light and
dark; its legibility rules; its motion and reduced-motion behaviour; its cold-start budget; how it
sits inside the phase-15 layouts that are about to merge; and the acceptance criteria a
code-auditor and a qa-engineer check on a device. The in-app licences surface is a separate
document, `docs/ux/licences-screen.md`.

**Inputs read.** `docs/ui/DESIGN-SYSTEM.md`; `docs/ux-spec.md` on `feature/phase-15-ux-refresh`
(worktree `PegSolitarie-p15`): D-03l and §4.2.4 the Home stage, §4.3.1 the status strip, §4.4.1 the
dock, §6.2 colour, §6.5 the cold-start budget and the 15e protocol (second edition), §9–§12, §15
(Q-902, Q-005); the Compose sources `ui/theme/Color.kt`, `Clay.kt`, `ClayAnim.kt`, `Theme.kt`,
`ui/home/HomeScreen.kt`, `HomeBlobs.kt`, `HomeStage.kt`, `ui/game/GameScreen.kt`,
`GameActionDock.kt`, `ui/onboarding/TutorialScreen.kt`, `ui/settings/SettingsScreen.kt`, and the
Results and catalogue screens' text roles; `res/values/dimens.xml`; `strings.xml`;
`.planning/reports/15-founder-feedback/qa-engineer-loop2.md` (the screens on a device: the strip
and dock geometry, the 54dp status bar and 24dp gesture bar of the Pixel_9, the Home band order);
`THIRD_PARTY.md` on `feature/phase-14-release-docs` (worktree `PegSolitarie-r5`); FOUNDER.md
Q-004, Q-005, Q-016, Q-019; rules/compose.md, rules/ip-policy.md, rules/puzzle-design.md.

**What this edition corrects in the cut-off first draft** (the draft's §0–§3 are kept where right
and rewritten where not; nothing of it is authoritative except what this file now says):

- The tutorial's density. The draft's §2 said Quiet (identical to gameplay) and its §3.3 said
  Sparse. **Sparse is ruled** (§2): the tutorial's caption sits on the backdrop at the height
  where the Quiet hills begin, and on a 360×640dp phone it would read at about 3.5:1.
- The draft's planned founder question (text on pills so the sky could be saturated) is withdrawn.
  The one open taste choice is how much of the scene the **game** screen carries (Q-NNN in the
  report, default specified in full here).
- On Home the standing sweets and the sky sprinkles move from window anchors into **the stage's
  wings** (B-12): anchored to the window they fell behind the ribbon and the Continue chips on the
  founder's own one-chip phone. Every sweet now **stands on a hill** (its base is computed from the
  hill), so nothing floats; the cupcake's cup is violet so it reads against the pink hill under it.
- The Quiet set gains frosting lips, drips and sprinkles on the hills, so the game screen is a
  sweets landscape too, without any standing sweet beside the board.
- The dark theme is specified as a night scene whose every colour is darker than the dark
  background or clearly brighter than every container. The draft's dark bound (sky L ≤ 0.014)
  allowed values at the board slab's own luminance, which would have dimmed the slab's edge below
  what it is today (§5.2).

**Three rules bind every line below, and every reviewer applies them literally.**

1. **The two-question test** (verdict §2). For any screenshot of the backdrop: *(1) Could any
   single element be traced to, or recognised as, a King design? (2) Would a player shown one
   screenshot think it is Candy Crush, a Candy Crush spin-off, or a reskin of it?* Fail either one
   and the art is out. "It is a sweet" is not the test, and "it is inspired by Candy Crush" is not
   a defence.
2. **The do / don't list** (verdict §3): words (§3a), shapes and compositions (§3b), palette (§3c),
   store assets (§3d). §10 restates it as a checklist against this design. The backdrop uses only
   the "do draw" set: soft rounded hills and frosting layers (with drips), cloud puffs of
   overlapping circles, scattered sprinkles (short rounded capsules), swirl discs, plain candy
   canes, gumdrop domes, a cupcake silhouette, and the existing slow-drifting soft blobs.
3. **The studio's own gates are unchanged by any of this** (rules/compose.md): text contrast
   ≥ 4.5:1 in both themes, never colour alone, touch targets, reduced motion honoured, every dp a
   `dimens.xml` token, every colour a `Color.kt` token, every ratio and duration in a scoped
   constants object, light and dark `@Preview`s, a Compose UI test per screen's main flow.

**What this does not touch.** No rule, footprint, score, generator, string of gameplay copy,
control, touch target, readout, oracle (`docs/ux-spec.md` §4.2.1) or sound changes. The hero
objects keep their colours, shapes and shadows: the gameplay slab, the dock, the status strip's
back chip and timer pill, the Home stage art, ribbon, Continue chips and mode tiles, every card,
row and dialog. No logo, mascot, character or title treatment is added anywhere. Pegs and holes are
drawn exactly as today.

## 1. Decisions

House style per `docs/ux-spec.md` §3: `- **B-nn:** <ruling> — <rationale>.` Never rewritten in
place; a later ruling is `B-nna` with a pointer.

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

## 2. Which screens carry the backdrop, and at what density

| Screen | Density | Why this density | Hero object the backdrop defers to | Uncontained text on the backdrop (§5.1 governs) |
|---|---|---|---|---|
| **Home** (the menu) | **Landscape** | The founder's first-named screen and the start destination. Its lower half is covered by the ribbon, the Continue chips and the tiles, so the scene is seen through the hero band, in the stage's two wings beside the board art — where the sweets stand (B-12) — in the 8–16dp band gaps and side margins, and in the footer above the navigation bar: the places a full landscape can be bright without touching a readout. | The two mode tiles; then the stage art, the Continue chips and the ribbon | the gradient `displaySmall` title only |
| **Gameplay** (Classic and Challenge — one screen, B-07) | **Quiet** | The founder's second-named screen. Sky and clouds behind the transparent status strip, a cotton-candy sky around the slab, frosted hills with drips and sprinkles from the slab's foot to the bottom edge, behind the dock. **No standing sweet** (B-06, B-16). On a 360dp phone the slab is width-limited (328dp of 360) and the dock is full-width, so only the margins and the band between slab and dock show; on a 6.7-inch phone that band is ≈139dp of frosted hill. | The board slab; then the dock and the strip's back chip and timer pill | the `titleLarge` strip title (`onSurface`) |
| **Tutorial** | **Sparse** | Its caption (`bodyMedium`, `onSurfaceVariant`) and `Skip` (`TextButton`, `primary`) sit on the background, and the column scrolls; at 360×640dp the caption lies at 0.65–0.75 H, exactly where Quiet's hills begin, so a Quiet tutorial would put body text on a pink hill at ≈3.5:1. D-01a's fidelity is to the **board and the mechanic** — the tutorial slab is the same `surfaceContainerHigh` clay slab either way. A teaching screen also wants the calmest frame, and it is the first screen a new player meets after Home. | The tutorial slab; `Skip`; the completion CTA | the caption, `Skip`, the step-pip row |
| **Classic catalogue** | **Sparse** | Sits between Home and the board in the core flow; a backdrop that dropped out here would flicker. The cards are the hero and are opaque `surfaceContainerHigh`, so a sky wash and a cloud is all the screen needs. | The board cards | the `titleLarge` top-bar title |
| **Results** | **Sparse** | A celebration whose own gold stars, headline and CTA are the payoff (D-14); a landscape would compete with them, and B-04 keeps gold out of the backdrop so the stars stay the only gold on screen. The column scrolls. | The star row, the headline, the primary CTA | the top-bar title, the `displaySmall` headline, the `labelLarge` metric labels, the outlined and text buttons' labels |
| **Settings** | **Sparse** | A utility screen of clay rows; the backdrop keeps the world continuous without competing. Its section headers (`labelLarge`, `onSurfaceVariant`) sit on the background and scroll. | The rows and switches | the top-bar title, the section headers |
| **Licences** and **Licence text** (new, `docs/ux/licences-screen.md`) | **Sparse** | Settings children; identical treatment to their parent. | The entry cards; the licence text | the top-bar titles, the section headers |
| **Dialogs** (restart confirm, ad offer, Play Games prompt, profile swap), snackbars, the Results ad scrim | — (inherit) | They scrim or sit on the screen beneath them. Nothing changes. | — | — |
| Ad and Play Games full-screen UIs, the system splash | — | Not drawn by this app. | — | — |

**Challenge** is not a screen. It is the gameplay screen entered with a `challengeLevel`
(`AppNavHost.kt`, `GameRoute`), so it carries Quiet by construction (B-07).

**What a density is.** A `BackdropDensity` value each destination maps to. It selects which layers
draw (§3.1), which sky stops and composition table apply (§3.3), and nothing else. It never changes
a colour token's meaning, a motif's shape or any rule in §5.

## 3. The scene

### 3.1 Layers

The backdrop is three window layers behind the screen's content, back to front, plus, on Home only,
the stage wings drawn inside the stage slot (B-12). Each window layer fills the **whole window**
`W × H`, including the areas under the status bar, the navigation bar and any display cutout (B-09). Positions are fractions of `W` and `H` (unitless, `BackdropMetrics`, §4.3) or
`dimens.xml` tokens (§4.2); nothing is a literal. `statusInset` below is the status-bar inset's
height on the current window.

| # | Layer | Draws | Densities | Composed | Redraws when |
|---|---|---|---|---|---|
| L1 | **Sky** — one `Canvas`, one rect | A vertical gradient of the density's sky stops (§3.3), over the whole window | all three | on the first frame | window size, theme or density change only |
| L2 | **Orbs** — the existing `HomeBlobs`, re-seated | Two `blob_size` (240dp) discs at `ClayAnim.BLOB_ALPHA` 0.10, `primary` and `secondary`, drifting ±`blob_drift_range` over `ClayAnim.BLOB_DRIFT_MS`, linear, reverse-repeat — unchanged; drawn **at rest** when the animator scale is 0 (B-05) | Landscape only | on the first frame (as today) | every frame while drifting (unchanged, O-B) |
| L3 | **Scene** — one static `Canvas` | In this order: clouds (each cloud's shade puffs, then its body puffs); then each hill mound back to front — **F2, F1, N1, N2** — each immediately followed by its frosting lip, its drips (near hills), any motif standing on it (Q-NNN option 2 only), and its sprinkles | Landscape and Quiet: clouds, hills, lips, drips, hill sprinkles. Sparse: one cloud. | **from the second frame on** (B-11) | window size, theme or density change only |
| W | **Stage wings** — Home only, drawn by the stage slot behind its art (B-12) | Per wing: its wing hill and lip, then its two standing sweets, then its sprinkles | Landscape, when the stage is ≥ `backdrop_wing_min_height` | from the second frame on, with L3 | stage size or theme change only |

**Why three layers and not one.** The orbs are the only thing that moves. A drifting element inside
the same `Canvas` as sixty static primitives would redraw all sixty every frame; split, the
per-frame cost of Home stays exactly what it is today (two translating discs), and the tech-lead's
pending ruling on O-B can stop the drift without touching the scene. The scene is a separate layer
so it can leave the first frame (B-11).

**Building the scene.** Every path and every computed position (a hill's surface height at a motif's
anchor, a drip's position) is built once per window size, theme and density and kept; the draw
pass only replays it — no allocation in the draw lambda (rules/compose.md, Recomposition). Nothing
reads `HomeUiState`, the clock, a random source, a resource file, a font or the network.

**Where the hero objects sit.** Every container the player reads is above L3 and opaque: the Home
stage art, ribbon pills, Continue chips and mode tiles, the gear chip, the gameplay slab, the dock,
the strip's back chip and timer pill, the tutorial slab, every card, row, banner, snackbar and
dialog. Transparent by design (B-09): the Home hero row, the gameplay status strip, the four
`TopAppBar`s, the tutorial header — their text sits on L1–L3 and §5.1 governs it. Nothing in L1–L3
is ever drawn above any container. The stage wings are drawn behind the stage art, inside the stage
slot; the one part of them that leaves the slot, a wing hill's skirt, passes behind the ribbon, which
the column draws after the stage.

**Orb frame of reference.** The orbs keep today's positions relative to the **content box** (the
window less the system-bar insets): the primary orb's centre peeks past the content box's top-end
corner by `BLOB_PEEK_FRACTION` (0.30) × `blob_size` on both axes, exactly as `HomeBlobs.kt` does
today; the secondary orb's centre sits `BLOB_PEEK_FRACTION` × `blob_size` past the content box's
start edge, at `ORB_SECONDARY_Y` (0.32) × `H`. Like today they may extend under the bars; they are
decorative and announce nothing.

### 3.2 Motif vocabulary

Every motif is from the verdict's "do draw" list and is built from circles, ellipses, capsules,
arcs, one trapezoid and one spiral — the primitive family of the pegs and dimples
(`BoardCanvas.kt`), so the scene and the board share a hand. Fills are the §4.1 `Backdrop*` tokens
and nothing else (B-04).

**The one-highlight rule (standing and floating motifs).** A motif may carry **one** specular
highlight — a `BackdropMotifCream` dot placed as `drawCandyPeg` places the peg's specular, toward
the top-start — or, on the cane, one short cream arc. **Never a second highlight, and never two or
more parallel strokes on one motif**: two parallel strokes are a stripe, and a striped sweet is on
the banned list (verdict §3b). The lollipop's spiral is one continuous stroke — the "swirl disc"
the verdict lists — and carries no extra dot. Frosting drips belong to the hills, not to a sweet,
and are not stripes (§10).

**"Stands on hill H at anchor x"** means: the motif's base point — the lollipop stick's lower end,
the cane stem's lower end, the gumdrop's flat base, the cupcake cup's bottom edge — lies on H's top
edge at that x, computed from H's ellipse, within 2dp (BD-11 tests it). H is a stage wing hill on
Home and a far hill on the game screen (Q-NNN option 2 only). Nothing floats.

| Motif | Geometry (ratios in `BackdropMetrics`, sizes in `dimens.xml`) | Fill / stroke | Highlight | Why it is safe (verdict §3b) |
|---|---|---|---|---|
| **Hill mound** | The union of an ellipse — centre (`cx`·W, `cy`·H), radii (`rx`·W, `ry`·H) — and the rectangle beneath its centre line, from `cx−rx` to `cx+rx` across and from `cy`·H down to the window's bottom edge, so the ground is solid below the crest. Crest height = `cy − ry`. A stage wing hill is the same construction in the stage slot's coordinates (§3.3). | `BackdropHillFar` (F1, F2, wing hills) / `BackdropHillNear` (N1, N2) | none | a soft rounded hill is genre convention (§2) |
| **Frosting lip** | A band inside the mound, following its top edge (the upper half of the ellipse), `backdrop_far_frosting_width` (8dp) thick on far mounds and `backdrop_near_frosting_width` (12dp) on near ones; clipped to the mound, so it never shows outside it | `BackdropFrosting` | — | "frosting layers" are do-draw; the dimple's two-tone idea turned the other way up |
| **Frosting drip** (near mounds only) | A rounded capsule `backdrop_frosting_drip_width` (12dp) wide hanging straight down from the lip's lower edge, joined to it. Drip *k* is centred at x = (`cx − rx`)·W + (k + ½) × `backdrop_frosting_drip_spacing` (48dp), for k = 0, 1, 2 … while x lies on the mound's upper arc and inside the window; its length cycles with k through `backdrop_frosting_drip_long` (20dp), `_short` (12dp), `_mid` (16dp) | `BackdropFrosting` | — | "frosting drips" are on the verdict's free list (§2) |
| **Cloud** | Four puffs, each a circle, relative to the cloud centre `c` and base radius `r` = `backdrop_cloud_radius` (20dp) × the cloud's scale: P1 `(0, 0)` radius `r`; P2 `(−1.05 r, +0.30 r)` `0.70 r`; P3 `(+1.10 r, +0.25 r)` `0.80 r`; P4 `(+0.35 r, −0.30 r)` `0.75 r`. Drawn twice: first all four translated down by `CLOUD_SHADE_OFFSET` (0.22) × `r` in `BackdropCloudShade`, then all four in `BackdropCloudBody` on top. Extent: `−1.75 r … +1.90 r` across, `−1.05 r … +1.27 r` down. | `BackdropCloudBody` over `BackdropCloudShade` | none (the shade is the volume) | "cloud puffs built from overlapping circles" |
| **Swirl lollipop** | Stands on its hill at its anchor. **Stick:** from the base point straight up `backdrop_lollipop_stick_length` (72dp), `backdrop_lollipop_stick_width` (8dp), round caps. **Disc:** radius `backdrop_lollipop_radius` (28dp), its bottom meeting the stick's top (centre 100dp above the base point). **Spiral:** one Archimedean stroke, `backdrop_lollipop_spiral_width` (4dp), round caps, from the disc centre out to `LOLLIPOP_SPIRAL_END` (0.78) × radius over `LOLLIPOP_SPIRAL_TURNS` (1.75) turns, starting at the top and turning clockwise (mirrored in RTL, B-15) | stick `BackdropMotifPink`; disc `BackdropMotifViolet`; spiral `BackdropMotifCream` | none — the spiral is its pattern | a **swirl disc**, never a plain disc, never blue (B-04): King's blue candy is a plain round "lollipop head", a violet disc with a drawn spiral on a stick is neither. It stands; it is never held, swung or used as a tool |
| **Plain candy cane** | Stands on its hill at its anchor. One stroke of `backdrop_cane_stroke_width` (12dp), round caps: a vertical **stem** from the base point up `backdrop_cane_height` (96dp); a **hook** — a half circle of radius `backdrop_cane_hook_radius` (20dp) whose centre lies 20dp to the start of the stem's top, running from the stem's top over the top to the far side; a **tail** of `backdrop_cane_tail_length` (12dp) straight down from the hook's end. **One colour. No stripes.** | `BackdropMotifPink` | one `BackdropMotifCream` arc, `backdrop_cane_highlight_width` (4dp) wide, centred on the hook's topmost point, sweeping `CANE_HIGHLIGHT_SWEEP_DEG` (30°), its centre line at hook radius + 4dp so it lies inside the stroke near its outer edge | "plain candy canes" is the do-draw wording; the stripe is the banned part and there is none |
| **Gumdrop dome** | Stands on its hill at its anchor. A circle of radius `backdrop_gumdrop_radius` (20dp) whose centre lies `GUMDROP_BASE_OFFSET` (0.30) × radius above the base line, cut flat at the base line | `BackdropMotifViolet` | one cream dot, radius `MOTIF_HIGHLIGHT_RADIUS` (0.18) × radius, offset toward the top-start by `MOTIF_HIGHLIGHT_OFFSET` (0.42) × radius from the circle's centre | a dome on a flat base; never a square (the green gum square is banned), never a bean, never a teardrop, never a cluster |
| **Cupcake** | Stands on its hill at its anchor. Width `w` = `backdrop_cupcake_width` (56dp). **Cup:** a trapezoid, top width `w`, bottom width `CUP_BOTTOM_WIDTH` (0.70) × `w`, height `CUP_HEIGHT` (0.55) × `w`, corners rounded by `backdrop_motif_corner_radius` (4dp), **no flutes or grooves** (grooves read as stripes). **Frosting:** three circles of radius `CUPCAKE_FROSTING_RADIUS` (0.30) × `w` centred at `(−0.22 w, −0.08 w)`, `(+0.22 w, −0.08 w)` and `(0, −0.28 w)` from the cup's top-centre. **Top dot:** radius `CUPCAKE_TOP_DOT_RADIUS` (0.08) × `w` at `(0, −0.58 w)`. Total height ≈ 1.21 `w` (68dp). | cup `BackdropMotifViolet`; frosting `BackdropFrosting`; top dot `BackdropMotifPink` | one cream dot on the cup, radius `CUPCAKE_HIGHLIGHT_RADIUS` (0.06) × `w`, at `(−0.24 w, +0.16 w)` from the cup's top-centre | "cupcake silhouettes" is do-draw; no face, no cherry (no red exists), no sprinkle-studded ball |
| **Sprinkle** | A capsule `backdrop_sprinkle_length` × `backdrop_sprinkle_width` (16 × 4dp), rotated by one of `SPRINKLE_ROTATIONS_DEG` (−40, −15, 10, 35, 60, 80) | by context (§3.4): in the sky `BackdropMotifViolet`, `BackdropMotifPink`, `BackdropSprinkleLilac`; on a hill, cream plus the *other* hill's hue | none | "scattered sprinkles (short rounded capsules)" — scattered in the sky or across a broad hill cut by the window, **never on a ball or a sphere** (a sprinkle-studded ball is the banned colour bomb) |
| **Orb** | The existing `HomeBlobs` disc (L2), re-seated only (§3.1) | `primary` / `secondary` at 0.10 alpha | none | "the existing slow-drifting soft blobs" |

**Not in the vocabulary, on purpose:** any sphere or disc with dots on it; any lozenge, bean,
teardrop, square, cluster or twist-wrapped shape; any striped or grooved surface on a sweet; stars
(gold is reserved for rewards); cookies (a disc with chips is one step from a studded ball);
anything brown (chocolate is where the colour bomb lives); any face, eye, mouth, limb or
character; any path, trail, signpost, node chain or numbered marker (the saga map); any lettering;
a castle, a gate, a rainbow or a sun. §10 is the checklist and §13 gives the reasons.

### 3.3 Composition per density

Origin top-start of the window; `x` a fraction of `W`, `y` a fraction of `H`, unless a dp token is
named. Every number is a named constant (§4.3) or a token (§4.2); the developer transcribes them and
never invents a position. BD-10 asserts these tables against `BackdropMetrics`.

**Sky stops** (L1), top to bottom, the colour held from each stop to the next as a linear blend:

| Density | stop 1 | stop 2 | stop 3 | stop 4 | stop 5 |
|---|---|---|---|---|---|
| Landscape | `0.00` SkyTop | `0.16` SkyTop | `0.32` SkyMid | `0.42` SkyHorizon | `1.00` SkyHorizon |
| Quiet | `0.00` SkyTop | `0.08` SkyTop | `0.16` SkyMid | `0.72` SkyHorizon | `1.00` SkyHorizon |
| Sparse | `0.00` SkyTop | `0.12` SkyTop | `1.00` SkyMid | — | — |

`SkyTop` equals the theme's `background` by design (§4.1), so the area under the status bar and the
top of every screen are **pixel-identical to today**, and the status-bar icon appearance the app
uses today stays right. The sky deepens below.

#### Landscape (Home)

Seen through the hero band, in the stage (around and beside the board art), in every band gap, in
both side margins, and in the footer. The lower group (ribbon, chips, tiles) stands in front of the
window's hills, so the cards read as set on the candy hills, while the stage's own wing hills rise
just above the ribbon with the sweets standing on them.

**The window scene (L1–L3):**

| Element | Position and size | Notes |
|---|---|---|
| Orb, primary | as today (§3.1) | drifts |
| Orb, secondary | content box start edge − 0.30 × `blob_size`, `0.32 H` | drifts; was bottom-start |
| Cloud C1 | centre `(0.22, 0.20)`, scale 1.0 | behind the stage's start wing |
| Cloud C2 | centre `(0.62, 0.15)`, scale 0.8 | just below the hero row (its top puff clears a one-line title on every window, so the title at rest sits on `SkyTop` alone, pixel for pixel as today); zone T |
| Cloud C3 | centre `(0.86, 0.27)`, scale 1.2 | behind the stage's end wing |
| Far hill F2 | ellipse centre `(0.85, 0.66)`, radii `(0.45, 0.20)` → crest `0.46 H`; lip 8dp | |
| Far hill F1 | ellipse centre `(0.30, 0.62)`, radii `(0.55, 0.22)` → crest `0.40 H`; lip 8dp | |
| Near hill N1 | ellipse centre `(0.70, 0.70)`, radii `(0.55, 0.20)` → crest `0.50 H`; lip 12dp; drips | |
| Near hill N2 | ellipse centre `(0.10, 0.74)`, radii `(0.40, 0.18)` → crest `0.56 H`; lip 12dp; drips | |
| Hill sprinkles ×6 | on N1 and N2, rules in §3.4 | seen in the footer, the gaps and the side margins |

Nothing of zone S lies above the far hills' crests (0.40 H) at window level: above them there are
only the sky, the clouds and the orbs.

**The stage wings (W), drawn by the stage slot behind its art.** Coordinates are relative to the
stage slot: width `Sw` (the column's content width), height `Sh`, origin at its top-start. `e` is the
art's edge (D-03l: `min(Sh, Sw, home_stage_art_max_size)`, centred), and each wing is
`ww = (Sw − e) / 2` wide.

- **When.** The wings are drawn only when the stage art is drawn and `Sh ≥ backdrop_wing_min_height`
  (160dp). Otherwise the stage is exactly what D-03l built.
- **Wing hill.** One mound per wing: an ellipse centred on the slot's bottom outer corner — `(0, Sh)`
  for the start wing, `(Sw, Sh)` for the end wing — with radii `rx = ww + WING_HILL_REACH (0.15) × e`
  and `ry = WING_HILL_HEIGHT (0.22) × Sh`, plus a skirt continuing it straight down
  `backdrop_wing_hill_skirt` (32dp) below the slot, so the hill runs on behind the ribbon through the
  16dp gap with no seam. `BackdropHillFar`, with the far lip (8dp). Its inner part passes behind the
  art, so the board reads as standing among the hills.
- **Sweets.** Start wing: the **lollipop** stands at `x = max(WING_OUTER_SWEET_X (0.30) × ww,
  backdrop_wing_sweet_inset (28dp))` from the slot's start edge; the **gumdrop** at
  `x = max(WING_INNER_SWEET_X (0.80) × ww, 56dp)`, where 56dp is the nearest position at which its dome
  clears the lollipop's stick by `backdrop_wing_sweet_gap` (4dp) — 28 + 4 (half the stick) + 4 + 20
  (the dome's radius). End wing, measured from the slot's end edge: the **cane** at the outer position,
  the **cupcake** at `max(0.80 × ww, 68dp)`, 68dp being the nearest position at which the cup and its
  frosting (≈ 29dp half-width) clear the cane's stem by 4dp (28 + 6 + 4 + 29, rounded up to the grid);
  the cane's tail then passes ≥ 8dp above the cupcake's top. Each stands on its wing hill. The art is
  drawn over them, so an inner sweet may be partly hidden behind the board, which reads as depth.
- **Fit rule.** A sweet is drawn only if its topmost point lies ≥ `backdrop_motif_top_clearance`
  (8dp) below the slot's top edge; otherwise that one sweet is omitted (the lollipop, at 128dp the
  tallest, is the first to go; the gumdrop and the cupcake, at 26dp and 68dp, fit whenever the wings
  are drawn). Nothing of the wings ever reaches the 16dp gap above the stage, so nothing can touch
  the hero row.
- **Sprinkles.** Three per wing, `WING_SPRINKLES` as fractions `(of ww, of Sh)` from the wing's outer
  edge: `(0.75, 0.18)`, `(0.62, 0.42)`, `(0.92, 0.58)`, mirrored for the end wing; colours
  `[MotifViolet, MotifPink, SprinkleLilac]`, rotations as §3.4. A wing sprinkle is drawn only if its
  capsule clears every drawn sweet by ≥ 4dp and its centre lies outside the art.

What that gives on the 6.7-inch baseline (412×915dp, content width 380dp; stage heights from
`docs/ux-spec.md` §4.2.4 as corrected by D-03m), for orientation — the computed values govern:

| Home state | `Sh` = `e` | `ww` | Wings | Lollipop | Cane | Gumdrop, cupcake |
|---|---|---|---|---|---|---|
| No Continue chip (a fresh install) | 257–302dp | 39–62dp | drawn | stands at x 28dp, its top 76dp or more inside the slot; up to a third of its disc behind the art at the narrow end | drawn | mostly behind the art: they peek out at its sides |
| One chip (the founder's state) | 182–245dp | 68–99dp | drawn | stands at x 28–30dp; top 15–65dp inside the slot | drawn | in view beside the art's lower corners: the gumdrop 0–8dp and the cupcake 9–28dp of its width behind the art |
| Both chips | 71–152dp | — | not drawn (`Sh` < 160dp) | — | — | — |

On a 360dp phone Home is always compact (§4.2.3 of the phase-15 spec): the stage is 0 at 360×640dp
and ≈ 84dp at 360×800dp, so no wings are drawn — the scene shows where there is room, exactly as the
stage does. On an 800×1280dp tablet the column is 480dp and the stage tall, so `e` = 320dp, `ww` =
80dp, and the four sweets stand at the bottom of each wing beside the art's lower half, while the
160dp window margins show the window scene's sky, clouds and hills.

#### Quiet (gameplay)

Sky and two clouds behind the strip, a cotton-candy sky around the slab, frosted hills from the
slab's foot down, behind the dock. No orbs, no standing sweets, no sky sprinkles (B-06).

| Element | Position and size | Notes |
|---|---|---|
| Cloud C1 | centre `x = 0.36 W`, `y = statusInset + backdrop_cloud_strip_offset` (28dp), scale 1.0 | behind the strip's title; zone T |
| Cloud C2 | centre `x = 0.62 W`, same `y`, scale 0.8 | may pass behind the timer pill at 200% on a 360dp phone; the pill is opaque and its edge is checked in §5.2 |
| Far hill F2 | ellipse centre `(0.90, 0.96)`, radii `(0.45, 0.18)` → crest `0.78 H`; lip 8dp | |
| Far hill F1 | ellipse centre `(0.35, 0.92)`, radii `(0.60, 0.22)` → crest `0.70 H`; lip 8dp | the hill the board stands on |
| Near hill N1 | ellipse centre `(0.72, 0.98)`, radii `(0.60, 0.17)` → crest `0.81 H`; lip 12dp; drips | mostly behind the dock |
| Near hill N2 | ellipse centre `(0.08, 1.02)`, radii `(0.42, 0.15)` → crest `0.87 H`; lip 12dp; drips | |
| Hill sprinkles ×8 | four on F1/F2, four on N1/N2, rules in §3.4 | |

What a player sees, per window (24dp status bar and 48dp three-button bar unless named):

| Window | Strip | Slab | Between slab and dock | Dock slab | Below the dock |
|---|---|---|---|---|---|
| 360×640dp | 24–80dp: sky top, C1 behind the title | 120–448dp, 328dp wide; 16dp margins of lilac sky | 448–504dp: F1's lip right at the slab's foot, frosted hill | 504–576dp, over hills | 576–592dp margin and the navigation bar: hills |
| 412×915dp | 24–80dp | 231–611dp, 380dp wide | 611–640dp pink-lilac sky, then 640–779dp of frosted hill with drips and sprinkles | 779–851dp | 851–915dp: hills |
| Pixel_9, 411×923dp (54dp status, 24dp gesture bar) | 54–110dp | 263–642dp | F1's crest at 646dp: the slab stands on the hill; 646–811dp of hill | 811–883dp | 883–923dp: hills under the gesture bar |
| 800×1280dp | 24–80dp | 364–844dp, 480dp wide; 160dp side margins of sky and hill | 844–896dp sky, 896–1144dp hill | 1144–1216dp | hills |

**The founder's alternatives, specified so an answer is a constant change (B-16, Q-NNN in the
report).** *Option 2 — the full scene on the game screen:* Quiet gains the four standing sweets on
its own far hills — lollipop on F1 at x `0.09`, gumdrop on F1 at `0.20`, cupcake on F2 at `0.80`,
cane on F2 at `0.93` — at full colour, drawn after their host hill exactly as on Home; nothing else
changes. On phones they show mostly peeking between the slab and the dock (at 360×640dp the
cupcake's top is behind the slab and only its lower part shows); fully on tall phones and tablets. *Option 3 — a plain game screen:*
Quiet draws L1 and its two clouds only (no hills, lips, drips or sprinkles).

#### Sparse (tutorial, catalogue, Results, Settings, Licences)

The sky wash (`SkyTop` held to 0.12 H, then a blend to `SkyMid` at the bottom edge) and **one** cloud:
centre `(0.72 W, statusInset + backdrop_cloud_strip_offset)`, scale 0.9. Its full extent is
`statusInset + 9dp … statusInset + 51dp` vertically, so it lies inside the 64dp top-bar band on the
four `TopAppBar` screens, where no container can reach it (scrolled content is clipped below the
bar), and behind the tutorial's header row, where it sits under `Skip`'s label (zone T, §5.1). No
hills, no orbs, no motifs, no sprinkles: every colour on these screens is zone T, so nothing can
fall below 4.5:1 anywhere, in any scroll state, at any font scale.

### 3.4 Sprinkle placement

Sprinkles are **constant lists** in `BackdropMetrics`, chosen once by the developer within the rules
below — the same discipline as the stage's constant peg set (`HomeStage.kt`). A seeded or
`Random`-driven scatter is forbidden: it would make the screenshots the two-question test is run on
non-reproducible. Every sprinkle is static: it never moves, fades or pops.

**Wing sprinkles** (Home's stage wings) are fixed by §3.3: three per wing, their positions given, and
the draw-or-omit rule there decides each one deterministically from the stage's size.

**Hill sprinkles** — `LANDSCAPE_HILL_SPRINKLES` (6, on N1/N2) and `QUIET_HILL_SPRINKLES` (8, four on
F1/F2 and four on N1/N2), each entry a centre `(x/W, y/H)`, a rotation index and its host mound. A
unit test proves every rule at four windows: `w360dp-h640dp`, `w412dp-h915dp`, `w800dp-h1280dp` and
`w1280dp-h800dp`.

1. **Inside the host.** The capsule lies wholly inside its host mound, its nearest point ≥
   `backdrop_hill_sprinkle_inset` (16dp) below the lower edge of the host's lip and clear of every
   drip.
2. **Not hidden.** No part of the capsule lies inside a mound drawn after its host.
3. **Edges and spacing.** Every centre is ≥ `backdrop_sprinkle_edge_inset` (24dp) from the start and
   end edges, and every pair on the same density is ≥ `backdrop_sprinkle_spacing` (32dp) apart at
   `w360dp` (wider windows only increase it).
4. **Look.** Never the host's own hue: on a near (violet) mound the colours cycle
   `[MotifCream, MotifPink]`, on a far (pink) mound `[MotifCream, MotifViolet]`; rotation
   `SPRINKLE_ROTATIONS_DEG[i mod 6]`.
5. **Static.**
6. **Clear of the board (Quiet only).** Every Quiet hill sprinkle's centre has `y/H ≥ 0.76`
   (`QUIET_HILL_SPRINKLE_TOP`): 38dp below the slab's foot on a 360×640dp phone, 84dp on the
   6.7-inch baseline, 59dp on the Pixel_9, so no sprinkle ever sits against the board's edge.

## 4. Tokens

### 4.1 `Color.kt` — new colour tokens, light and dark

Every hex below lives **only** in `ui/theme/Color.kt`, named `Light<Token>` and `Dark<Token>` like the
existing `LightCtaGradientColors` / `DarkPegHighlight`, and reaches the backdrop through one
immutable per-theme palette holder provided by `AppTheme` the way `ClayColors` is
(`LocalClayColors`). No call site names a hex. `L` is WCAG relative luminance computed from the hex;
**zone** is B-03's: **T** = may sit under uncontained text, **S** = only where the layout puts none.
The two orbs keep `primary` and `secondary` at `ClayAnim.BLOB_ALPHA` and add no token.

| Token | Light | L | Dark | L | Zone | Derived from |
|---|---|---|---|---|---|---|
| `BackdropSkyTop` | `#F4F1FA` | 0.890 | `#1B1626` | 0.0095 | T | **identical to `background`** in each scheme (BD-09 asserts equality) |
| `BackdropSkyMid` | `#F0E2FC` | 0.800 | `#140D20` | 0.0054 | T | lavender tinted toward the violet lead (light); the background deepened toward violet-black (dark) |
| `BackdropSkyHorizon` | `#F8D3EA` | 0.725 | `#1E0D33` | 0.0080 | S | the `secondaryContainer` pink family, a cotton-candy glow (light); deep plum-violet (dark) |
| `BackdropCloudBody` | `#FFFBFE` | 0.974 | `#1A1230` | 0.0086 | T | `surface` lightened — never pure white (DESIGN-SYSTEM anti-pattern); a violet wisp in dark |
| `BackdropCloudShade` | `#EEE2FA` | 0.795 | `#120B22` | 0.0048 | T | lilac shade of the body |
| `BackdropHillFar` | `#F5AED6` | 0.546 | `#290A2A` | 0.0085 | S | the `PegGradient` highlight `#F9A8D4` family — pink seconds (light); deep plum (dark) |
| `BackdropHillNear` | `#A47CF0` | 0.286 | `#1A0E37` | 0.0081 | S | between the `CtaGradient` stops `#A78BFA`→`#7C3AED` — violet leads (light); deep violet (dark) |
| `BackdropFrosting` | `#FFF5FA` | 0.935 | `#A98BC2` | 0.308 | S | pink-cream (light; also text-safe); soft lilac (dark) |
| `BackdropMotifViolet` | `#8B5CF6` | 0.198 | `#9F7AEA` | 0.272 | S | the dark `CtaGradient` stop (light); between dark `primary` and the dark CTA stop (dark) |
| `BackdropMotifPink` | `#EC4899` | 0.248 | `#E06AA8` | 0.290 | S | `secondary` (light); dark `secondary` desaturated (dark) |
| `BackdropMotifCream` | `#FFF5FA` | 0.935 | `#F3E4F8` | 0.813 | S | the frosting cream (light); a lilac-white highlight (dark). Same light value as `BackdropFrosting`, separate owner (the `dimens.xml` convention, applied to colour) |
| `BackdropSprinkleLilac` | `#C4B5FD` | 0.519 | `#8E7CC3` | 0.241 | S | the `CtaGradient` light-stop family |

**The constraints these values satisfy, which is what a reviewer checks, not the hexes.** If a value
is retuned on a device, it must still meet all of these (BD-08 computes them from `Color.kt` in a
JVM test):

- **Zone T, light:** L ≥ 0.779 — so `primary` (L 0.134), `onSurfaceVariant` (0.119) and
  `onSurface` (0.030) all read ≥ 4.5:1 on it.
- **Zone T, dark:** L ≤ 0.0357 — so dark `primary` (0.336), `onSurfaceVariant` (0.364) and
  `onSurface` (0.829) all read ≥ 4.5:1 on it.
- **Dark, everything:** each colour is either **≤ the dark background's L (0.0095)** or **≥ 0.165**.
  The first band keeps every dark container's edge at least as separated as it is on today's
  background; the second clears the brightest container's need (the dark mode tiles, L ≈ 0.063,
  sit 1.89:1 off today's background, so an accent must be ≥ 0.165 to beat that). Nothing sits in
  between. The orbs' existing 0.10-alpha blend (≈ 0.019) is the one pre-existing exception and is
  unchanged in value (§5.2).
- **Light, around the hero objects at rest:** see §5.2's table; every ratio ≥ today's.
- **Sky order, light:** SkyTop > SkyMid > SkyHorizon in L; dark: SkyHorizon and SkyMid ≤ SkyTop.
- **No gold, blue, red, green or brown anywhere** (B-04): every token's hue lies in the violet–pink
  arc, 250°–335° (the values above span 252°–331°), the creams included, which are pink- or
  lilac-tinted rather than yellow. BD-08 asserts the hue arc.

### 4.2 `dimens.xml` — new dimension tokens

All are multiples of 4dp, so no sub-grid exception is needed. One token per owner even where values
coincide (the file's own convention). The orbs keep `blob_size` (240dp) and `blob_drift_range`
(16dp).

| Token | Value | Owner |
|---|---|---|
| `backdrop_cloud_radius` | 20dp | a cloud's base puff radius (× the cloud's scale) |
| `backdrop_cloud_strip_offset` | 28dp | Quiet/Sparse cloud centres below `statusInset` |
| `backdrop_far_frosting_width` | 8dp | far mounds' lip |
| `backdrop_near_frosting_width` | 12dp | near mounds' lip |
| `backdrop_frosting_drip_width` | 12dp | drip width |
| `backdrop_frosting_drip_spacing` | 48dp | drip pitch along a near lip |
| `backdrop_frosting_drip_short` | 12dp | drip length, short |
| `backdrop_frosting_drip_mid` | 16dp | drip length, mid |
| `backdrop_frosting_drip_long` | 20dp | drip length, long |
| `backdrop_lollipop_radius` | 28dp | lollipop disc |
| `backdrop_lollipop_stick_length` | 72dp | lollipop stick |
| `backdrop_lollipop_stick_width` | 8dp | lollipop stick stroke |
| `backdrop_lollipop_spiral_width` | 4dp | lollipop spiral stroke |
| `backdrop_cane_height` | 96dp | cane stem |
| `backdrop_cane_stroke_width` | 12dp | cane stroke |
| `backdrop_cane_hook_radius` | 20dp | cane hook |
| `backdrop_cane_tail_length` | 12dp | cane tail |
| `backdrop_cane_highlight_width` | 4dp | the cane's one highlight arc |
| `backdrop_gumdrop_radius` | 20dp | gumdrop dome |
| `backdrop_cupcake_width` | 56dp | cupcake cup's top width |
| `backdrop_motif_corner_radius` | 4dp | cupcake cup corners |
| `backdrop_sprinkle_length` | 16dp | sprinkle capsule |
| `backdrop_sprinkle_width` | 4dp | sprinkle capsule |
| `backdrop_sprinkle_edge_inset` | 24dp | sprinkles' distance from the window edges |
| `backdrop_sprinkle_spacing` | 32dp | sprinkles' minimum pairwise distance |
| `backdrop_hill_sprinkle_inset` | 16dp | hill sprinkles' distance below the lip |
| `backdrop_wing_min_height` | 160dp | the stage height from which Home's stage wings are drawn |
| `backdrop_wing_hill_skirt` | 32dp | a wing hill's continuation below the stage slot, behind the ribbon |
| `backdrop_wing_sweet_inset` | 28dp | the outer sweet's minimum distance from the stage slot's outer edge |
| `backdrop_motif_top_clearance` | 8dp | a wing sweet's minimum distance below the stage slot's top edge |
| `backdrop_wing_sprinkle_clearance` | 4dp | a wing sprinkle's minimum distance from any drawn sweet |
| `backdrop_wing_sweet_gap` | 4dp | the minimum horizontal gap between a wing's two sweets |

Motif sizes are fixed dp, not window fractions, so a sweet is the same physical size on every
phone; only positions scale with the window. On a tablet the sweets are therefore small against
the wide wings, which keeps them decoration.

### 4.3 `BackdropMetrics` — unitless ratios, anchors and counts

A scoped constants object (unitless, so not `dimens.xml`; §6.4 of the phase-15 spec), beside
`BoardMetrics` and `HomeStageMetrics`. It holds exactly the values named in §3, grouped:

| Group | Constants |
|---|---|
| Sky stops | `LANDSCAPE_SKY_STOPS` `[0.00, 0.16, 0.32, 0.42, 1.00]`, `QUIET_SKY_STOPS` `[0.00, 0.08, 0.16, 0.72, 1.00]`, `SPARSE_SKY_STOPS` `[0.00, 0.12, 1.00]`, each with its colour roles from §3.3 |
| Mounds | per density, each mound as `(cx, cy, rx, ry)`: Landscape `F2 (0.85, 0.66, 0.45, 0.20)`, `F1 (0.30, 0.62, 0.55, 0.22)`, `N1 (0.70, 0.70, 0.55, 0.20)`, `N2 (0.10, 0.74, 0.40, 0.18)`; Quiet `F2 (0.90, 0.96, 0.45, 0.18)`, `F1 (0.35, 0.92, 0.60, 0.22)`, `N1 (0.72, 0.98, 0.60, 0.17)`, `N2 (0.08, 1.02, 0.42, 0.15)` |
| Clouds | per density, centre and scale: Landscape `(0.22, 0.20) 1.0`, `(0.62, 0.15) 0.8`, `(0.86, 0.27) 1.2`; Quiet `x 0.36 scale 1.0`, `x 0.62 scale 0.8` (y from the strip offset); Sparse `x 0.72 scale 0.9`. Puffs `P1 (0, 0) 1.00`, `P2 (−1.05, +0.30) 0.70`, `P3 (+1.10, +0.25) 0.80`, `P4 (+0.35, −0.30) 0.75`; `CLOUD_SHADE_OFFSET` 0.22 |
| Stage wings | `WING_HILL_REACH` 0.15 (× `e`), `WING_HILL_HEIGHT` 0.22 (× `Sh`), `WING_OUTER_SWEET_X` 0.30 and `WING_INNER_SWEET_X` 0.80 (× `ww`), `WING_SPRINKLES` `[(0.75, 0.18), (0.62, 0.42), (0.92, 0.58)]` (× `ww`, × `Sh`, from the wing's outer edge) |
| Standing sweets on the game screen | Q-NNN option 2 only (Quiet): lollipop F1 `0.09`, gumdrop F1 `0.20`, cupcake F2 `0.80`, cane F2 `0.93` |
| Motif ratios | `LOLLIPOP_SPIRAL_TURNS` 1.75, `LOLLIPOP_SPIRAL_END` 0.78, `CANE_HIGHLIGHT_SWEEP_DEG` 30, `GUMDROP_BASE_OFFSET` 0.30, `MOTIF_HIGHLIGHT_RADIUS` 0.18, `MOTIF_HIGHLIGHT_OFFSET` 0.42, `CUP_BOTTOM_WIDTH` 0.70, `CUP_HEIGHT` 0.55, `CUPCAKE_FROSTING_RADIUS` 0.30, the three frosting centres, `CUPCAKE_TOP_DOT_RADIUS` 0.08 at `(0, −0.58)`, `CUPCAKE_HIGHLIGHT_RADIUS` 0.06 at `(−0.24, +0.16)` |
| Sprinkles | `SPRINKLE_ROTATIONS_DEG` `[−40, −15, 10, 35, 60, 80]`, `LANDSCAPE_HILL_SPRINKLES` (6), `QUIET_HILL_SPRINKLES` (8), `QUIET_HILL_SPRINKLE_TOP` 0.76 |
| Orbs | `BLOB_PEEK_FRACTION` 0.30 (moves here from `HomeBlobs.kt`'s private constant, value unchanged), `ORB_SECONDARY_Y` 0.32 |

**Durations.** The backdrop owns none. A density change cross-fades over the navigation transition's
own duration (B-13); if the implementation needs that number as a constant it lives in `ClayAnim`
as `BACKDROP_CROSSFADE_MS`, equal to the transition the `NavHost` uses, so the two cannot drift.

## 5. Legibility and accessibility

Ratios here are computed from the `Color.kt` hexes with the WCAG formula; the device reading
governs (BD-12, BD-13), exactly as in `docs/ux-spec.md` §4.4.1. Luminances used: light `onSurface`
0.030, `onSurfaceVariant` 0.119, `primary` 0.134; dark `onSurface` 0.829, `onSurfaceVariant` 0.364,
`primary` 0.336.

### 5.1 Text on the backdrop

**The rule (B-03).** Every uncontained text, at any scroll position and any font scale up to 200%,
in either theme, sits only on zone-T colours. The worst zone-T colour is the darkest one in light
(`CloudShade`, L 0.795) and the lightest one in dark (`SkyTop`, L 0.0095; on Home the existing orbs'
blend, ≈ 0.019). Against those:

| Text role | Light, worst | Dark, worst | Needs |
|---|---|---|---|
| `onSurface` (titles, the gameplay title, Results values and button labels) | 10.5:1 | 14.8:1 (12.7:1 over an orb) | 4.5:1 |
| `onSurfaceVariant` (tutorial caption, Settings headers, Results metric labels) | 5.0:1 | 6.9:1 | 4.5:1 |
| `primary` (`Skip`, any `TextButton` default) | 4.6:1 | 6.5:1 | 4.5:1 |
| Home title, `displaySmall` with `ctaGradientBrush()` (large text) — its dark stop | 4.6:1 | 4.2:1 | 3:1 |

**Where each screen's uncontained text can reach, and why it stays on zone T:**

| Screen | Text | Reach | How zone T is guaranteed |
|---|---|---|---|
| Home | the title | at rest, one line: the hero row, ≤ 118dp from the top on every window, which is `SkyTop` held to 0.16 H; wrapped (large font, narrow window): down to ≈ 0.46 H at 200% on a 360dp phone | at rest it is on `SkyTop` alone — pixel-identical to today (C2 clears it, §3.3). Every sweet and wing sprinkle lives in the stage, which the column always lays out **below** the hero row (B-12), so a wrapped title can never reach one. Wrapped, the title can reach only the window scene, whose zone-S colours begin at the far hills' crests (≥ 0.40 H): the gradient's light half stays in the top line, over zone T, and the lower lines are the dark stop, which reads ≥ 3.2:1 even over the far hill's pink (L 0.546) and ≥ 4.2:1 over the horizon glow (dark: ≥ 4.2:1 everywhere). The column only ever scrolls the title **up**, toward `SkyTop`. |
| Gameplay | the strip title (`titleLarge`, `onSurface`) | the strip: `statusInset … statusInset + 56dp`, ≈ 64dp at 200% — within the top 0.16 H on every supported window | Quiet's top 0.16 H is `SkyTop`→`SkyMid` plus the two clouds, all zone T. The title is `onSurface`, which would read ≥ 9.6:1 even on the horizon glow. |
| Tutorial | caption, `Skip`, step-pip row | anywhere: the column scrolls | Sparse: every colour is zone T |
| Catalogue, Results, Settings, Licences | top-bar titles; headline, metric labels, outlined- and text-button labels; section headers | anywhere: each scrolls | Sparse: every colour is zone T |
| Dialogs, snackbars, the rescue banner, ribbon pills, chips, tiles, cards, rows | — | — | contained: they sit on their own opaque surface |

**Non-text graphics on the backdrop** (WCAG 1.4.11, 3:1): the four `TopAppBar` back arrows
(`onSurface`) read ≥ 10:1 on zone T; the gameplay loading spinner (`primary`, centred in the board
region, over Quiet's mid sky) reads ≥ 4.2:1 light and ≥ 4.0:1 dark. The Results outlined button's
`outline` border is below 3:1 on today's background already (≈ 1.6:1) and drops to ≈ 1.4:1 on
`SkyMid`; its `onSurface` label is what identifies it, so this is recorded, not a failure, and is
pre-existing.

**One pre-existing finding, recorded and not fixed here.** The Home title gradient's **light** stop
(`#A78BFA`) reads ≈ 2.4:1 against today's light background, under the 3:1 large-text bar; only the
glyphs' lower half (the dark stop) passes. The backdrop leaves it exactly as it is — the title sits
on `SkyTop` at rest — and §14 routes it.

### 5.2 The hero objects: the board slab, the dock, the Home tiles and chips

**What sits under the board: the existing clay slab and nothing else.** `surfaceContainerHigh`,
`shapes.extraLarge`, `clayShadow(elevation_card)`, opaque — never a dark translucent grid of tiles,
never translucent, never blurred (verdict §3b). Pegs, holes, legal-target dots, the selection ring,
the hint glow and the jump slide are all drawn inside it, so **their distinguishability is
unchanged by construction**: BD-17 proves the slab's interior pixel-identical with and without the
backdrop, in both themes.

**What the backdrop can affect is each container's edge.** The rule: at rest, on the reference
windows, every hero container's luminance ratio against every backdrop colour adjacent to its edge
is **≥ its ratio against today's background**, in both themes.

| Container (role) | Today vs background, light | Worst adjacent at rest, light (colour) | Today, dark | Worst adjacent, dark |
|---|---|---|---|---|
| Gameplay slab, dock, back chip, timer pill, stage art, ribbon pills (`surfaceContainerHigh`) | 1.05:1 | 1.05:1 (`SkyMid`); 1.06 (`CloudShade`); 1.10 (frosting); ≥ 1.15 everywhere else | 1.26:1 | 1.26:1 (`SkyTop`); ≥ 1.34 on `SkyMid`; ≥ 2.9 on any accent |
| Classic tile (`primaryContainer`) | 1.10:1 | 1.11:1 (`SkyHorizon`); 1.15 (frosting); 1.44 (far hill) | 1.89:1 | 1.89:1 (`SkyTop`); ≥ 1.91 on any accent |
| Challenge tile (`secondaryContainer`) | 1.09:1 | 1.11:1 (`SkyHorizon`); 1.14 (frosting) | 1.85:1 | ≥ 1.85:1 |
| Continue chips (`tertiaryContainer`) | 1.02:1 | 1.07:1 (frosting); 1.19 (`SkyHorizon`) | 1.90:1 | ≥ 1.90:1 |
| Rescue banner (`errorContainer`) | 1.09:1 | ≥ 1.09:1 (it overlays the board region's sky and the slab) | 1.39:1 | ≥ 1.39:1 |

Why every row holds:

- **Light.** The tiles and Continue chips sit at or below 0.45 H at rest on every window the phase-15
  arithmetic covers (§4.2.2–§4.2.4 there: the first tile's top is at or below the midpoint on tall
  windows, and the compact set pushes it lower), which is past Landscape's horizon stop at 0.42 H —
  so they meet the horizon glow, the hills, the lips and the sprinkles, never `SkyMid` (whose
  luminance equals a tile's). The slab, the dock and the strip's chips meet `SkyTop`, `SkyMid`, the
  clouds, the horizon and the hills — all at or above today's 1.05:1. In light the clay shadow keeps
  doing the job it does today on the pale sky; where a container meets a saturated hill, luminance
  separates it 1.4–2.7:1, far more than today.
- **Dark.** Every backdrop colour is either at or below the background's luminance or ≥ 0.165 (§4.1),
  so no dark container can be less separated than it is on today's background, anywhere, at rest
  **or scrolled**. The orbs' blend (≈ 0.019) is the one exception; it is today's design, unchanged,
  and the re-seat (B-05) takes the pink orb out from behind the tiles, where it sits today.
- **Scrolled Home (narrow phones, large fonts), light only.** A tile scrolled up over `SkyMid` meets
  ≈ 1.01:1 luminance — the edge is then carried by the clay shadow alone, which is also what carries
  it on today's background (1.02–1.10:1 is not a luminance edge either). Recorded so nobody
  re-derives it.

**The hero stays the hero.** Beyond contrast:

- **Gameplay:** no standing sweet (B-06); no element of the scene lies within the slab's bounds or
  in front of it; hill sprinkles keep ≥ 38dp below the slab's foot (§3.4 rule 6); the brightest
  accents are behind the dock or below the slab, never beside the pegs.
- **Home:** no element of the scene is drawn in front of any container; the standing sweets stand
  in the stage's wings, between the hero row and the ribbon by layout (B-12), so they flank the stage
  art and never touch a tile, a chip, the ribbon or the title, and the art is drawn over the inner
  ones; the tiles remain the largest, most saturated, shadowed, labelled objects on the screen.
- **Size discipline:** every sweet is shorter than the board art beside it (the tallest, the
  lollipop at 128dp, is drawn only in a stage ≥ 160dp, whose art is then ≥ 160dp), and the largest
  sweet's bounding box is under a quarter of a mode tile's area (56 × 128dp against ≥ 328 × 96dp).

### 5.3 Colour vision

- **The backdrop encodes nothing.** No hue on it carries a meaning, so there is nothing for a colour
  vision deficiency to lose.
- **Every legibility rule above is a luminance ratio**, so it holds in greyscale and under
  deuteranopia, protanopia and tritanopia simulations, which preserve luminance far better than hue.
  (Protanopia darkens pinks; that only raises the pink hills' contrast against the pale containers.)
  BD-24 checks a greyscale capture.
- **Pegs against holes.** Inside the opaque slab and untouched: a peg is a sphere with a specular dot,
  a hole an inset two-tone dimple — shape and luminance (light peg body L ≈ 0.25 against a dimple
  fill of L ≈ 0.71), not hue alone.
- **The colour-blind palette option rules/compose.md requires does not exist in the app** (no
  setting, no alternative peg palette in `Color.kt`; Settings holds General, Play Games and
  Privacy). It is a pre-existing gap, not introduced here, and §14 routes it. Whatever form it
  takes, the backdrop needs no variant: none of its rules depends on hue, and it shares no token
  with the pegs, holes, hint glow or rings. If that palette ever moves the pegs into the backdrop's
  violet–pink arc, the slab still separates them, because the slab is opaque.

### 5.4 System bars and edge to edge

- **Drawn under every bar.** The backdrop fills the window under the status bar, the navigation bar
  and any display cutout (B-09). No content moves: each system bar is still applied to the content
  exactly once, by the same layer as today — criteria 18b and 18c of the phase-15 spec, and Home's
  15k/15p geometry, keep their numbers (BD-19).
- **Status bar.** `SkyTop` equals `background`, so the status bar area is pixel-identical to today
  (apart from the primary orb's peek, which already reaches it today), and the status-bar icon
  appearance the app uses today stays correct in both themes.
- **Navigation bar.** Zone S (hills) lies under it on Home and gameplay. With three-button
  navigation the system's contrast scrim must stay on — nothing may set
  `isNavigationBarContrastEnforced` to `false` — and with gesture navigation the handle adapts on its
  own. BD-19 checks both.
- **The tutorial** keeps `safeDrawingPadding()` on its column, outside `verticalScroll`, exactly as
  built; only its root `Surface` stops painting `background` (B-09).
- **Display cutouts** show sky; nothing is placed relative to them.

### 5.5 Screen readers, touch, text scaling, right to left

- **TalkBack.** The backdrop is not in the traversal: no description, not focusable, not clickable,
  no `pointerInput`, so it never takes a touch from the content above it and every screen's
  traversal order (`docs/ux-spec.md` §11) is unchanged (BD-15).
- **Touch targets.** Unchanged everywhere; the backdrop adds none and covers none.
- **Text scaling.** The backdrop has no text, and its window scene reads no font size: it renders
  identically at 1.0 and 2.0 font scale (BD-20). Home's stage wings follow the stage's size, which a
  larger font shrinks or removes exactly as it does the stage art.
- **Right to left.** The scene mirrors (B-15); every rule here holds mirrored.

## 6. Motion and reduced motion

**What moves: the two orbs on Home, and nothing else, ever (B-05).** Their drift is today's, unchanged:
±`blob_drift_range` (16dp) on both axes over `ClayAnim.BLOB_DRIFT_MS` (8000 ms), linear,
reverse-repeat, from the existing infinite transition, gated on `rememberAnimationsEnabled()`
(`Clay.kt`, which reads `Settings.Global.ANIMATOR_DURATION_SCALE`).

| Element | Animator scale > 0 | Animator scale = 0 (reduced motion) |
|---|---|---|
| Orbs (Home) | drift as today | **drawn static at their rest centres** (drift progress 0) — no longer omitted (B-05) |
| Sky, clouds, hills, lips, drips, sprinkles, stage wings and sweets | static | static |
| The scene's and the wings' first appearance (B-11) | appear on the second frame, no animation of their own, behind the hero's existing 240 ms `clayEntrance` fade | appear on the second frame, settled |
| Density change on navigation (B-13) | the new sky and scene cross-fade in over the old, over the navigation transition's own duration | instant swap |
| Light ↔ dark theme change | instant redraw, as the rest of the theme | instant |

Scale values other than 0 (0.5×, 2× …) stretch or shorten the drift and the cross-fade through
Compose's frame clock, as they already do for every animation in the app.

**What never moves.** No parallax and no scroll-linked motion — the backdrop does not scroll with the
content; no drifting clouds; no twinkling, popping or falling sprinkles; no swaying lollipop; no tilt
or gyroscope response; no time-of-day or per-level variation; no animation on a win, a jump or a
hint. A decorative scene that moved would compete with the board's own motion vocabulary (the
selection pulse, the jump slide, the removal fade, the hint glow), which is meaning, not decoration
(ui-ux-pro-max `motion-meaning`, `excessive-motion`).

**Battery.** Quiet and Sparse cost **zero frames when idle**. Landscape costs exactly what Home costs
today (the two drifting orbs, the qa-engineer's O-B, ≈ 61 fps idle); the tech-lead's pending ruling on
O-B applies unchanged, and whatever it rules, the scene layers need no change because they never
animate.

## 7. Cold start

**The bar.** The dispatch's: *nothing on the startup critical path that a static gradient would not
cost.* Under it, D-03g's hard rules for Home's first frame (`docs/ux-spec.md` §6.5): no blocking read,
no image decode, no asset load, no bitmap, no new SDK call — with the bundled type faces as the one
named exception, which this change does not touch.

**What Home's first frame gains:**

| Element | First-frame cost | Verdict |
|---|---|---|
| L1 sky | one vertical-gradient brush over one window rect — **the static gradient itself** | the bar, by definition |
| Transparent `Scaffold` | removes the opaque `background` fill the `Scaffold` paints today | negative: the number of full-window fills is unchanged (theme `Surface` + sky in place of theme `Surface` + `Scaffold` background); one fewer if the sky is drawn as the theme surface's own background |
| L2 orbs | today's two discs, re-seated | unchanged |
| L3 scene, W stage wings | **not on the first frame** (B-11) | none |

**Never on Home's start path, at any frame:** a vector drawable or any `painterResource`; a bitmap or a
decode; a resource or file read; a read of `HomeUiState`, DataStore or Room; an SDK call; a
`Random`; the clock. Everything the scene draws is built in memory from §4's constants.

**The deferral.** L3 and the wings are composed only after the host's first frame has been drawn
(one frame, ≈ 16 ms, later); the mechanism is the developer's, the property is criterion BD-23. They
build their paths once on that frame. Because a build on frame 2 can still jank the entrance, the
qa-engineer compares janky frames in the first second of a cold start against arm A (BD-23's device
clause), and `compose-performance-audit` covers the backdrop before release (rules/compose.md).

**Measurement.** Home's first frame changes, so **criterion 15e runs on the branch that lands the
backdrop** under §6.5's second-edition protocol, unchanged — arm A `develop` as it stands once phase 15
has merged, arm B this branch's head, the null control first, interleaved blocks, PASS / FAIL / VOID as
ruled there, the Pixel_9a and every precondition as recorded there (precondition 7's block-boundary
swap included). Criterion 15f (placeholders, no spinner, no reflow) is unaffected and stays as ruled.
The ≤ 2 s bar on a physical phone (D-03j) is unaffected and still owned by PLAT-08 / phase 14.

**Degradation path, if 15e fails on this branch.** Taken *before* §6.5's own steps, stopping at the
first sitting that passes; each step changes the first frame only and costs no oracle, no readout and
no legibility rule:

1. **Flat first frame.** The first frame draws L1 as a flat `SkyTop` fill (identical to today's
   background) and the gradient from the second frame on, with L3.
2. **No backdrop on the first frame.** The first frame is exactly today's (no backdrop, no orb
   change); the whole backdrop arrives on the second frame.
3. Then §6.5's steps 1–4, unchanged.

L3 and the wings can never be the cause — they are not on the first frame — so a failure that survives
step 2 is not the backdrop's, and the tech-lead rules on it with the numbers before any art is
removed (the same discipline §6.5 applies to the fonts).

## 8. Fit with the phase-15 layouts, and responsiveness

The backdrop changes **no layout**. Every position, size, gap, inset and band set the phase-15 spec
rules keeps its value; BD-19 re-runs those criteria on this branch.

### 8.1 Home's board stage (D-03l)

- **The stage stays what D-03l made it:** the one weighted child that may hold anything, decoration
  only, `0` when the column overflows. Its art is unchanged — the `surfaceContainerHigh` slab,
  `shapes.extraLarge`, `clayShadow(elevation_card)`, the constant English-33 position, the edge rule
  `min(Sh, Sw, home_stage_art_max_size)`, the `clayEntrance(HERO_ENTRANCE_ORDER)` entrance.
- **The wings join the stage under clause 4's own terms:** decorative, no description,
  non-interactive, non-focusable, and gone when the stage is short (they need ≥ 160dp, B-12). They are
  drawn behind the art, so nothing of the art changes.
- **15n–15r stand.** 15p's bound (no blank > 80dp / 64dp between visible elements) can only tighten:
  the wings add visible elements beside the art and remove none. 15q (constant, legal position) and
  15r (the stage is not clickable, not focusable, announces nothing — now the wings too) are re-run.
- **The hero row** stays transparent, with no surface of its own, and sits on `SkyTop` at rest.
- **`HomeBlobs`** leave the Home column and become the backdrop's L2 (B-05), positions unchanged.

### 8.2 The gameplay strip and dock (the Q-902 split, D-04e, D-05d)

- **The strip** stays transparent and status-only; its title is on zone T (§5.1) and its back chip
  and timer pill stay opaque clay chips. Quiet's clouds sit behind it. 18a, 18d and 18e (the title
  whole at 360dp, the pill's one width, the timer whole at 200%) are about text metrics and are
  untouched; 18b and 18c are re-run.
- **The dock** stays one opaque `surfaceContainerHigh` slab over the hills. Its five looks, their
  ring, fill, shadow, mark and slash are all measured against **the slab**, not the backdrop, so
  21a–21g are unaffected (21f's two state marks sit inside the slab).
- **The board region** between them is transparent, as today; the slab is centred in it; the rescue
  banner overlays the slab's lower edge on its own `errorContainer` surface; the Challenge loading
  spinner sits on Quiet's mid sky (§5.1).

### 8.3 Insets, edge to edge, safe drawing

§5.4: the backdrop is outside every inset, under both bars and any cutout; the content applies each
bar once, exactly as built (Home and gameplay through the `Scaffold`'s content padding, the tutorial
through `safeDrawingPadding()` on its column); `SkyTop` keeps the status-bar area pixel-identical; the
navigation bar's contrast scrim stays enforced.

### 8.4 Windows

| Window | Home (Landscape) | Gameplay (Quiet) | Sparse screens |
|---|---|---|---|
| 360×640dp, 3-button bar (the design baseline's small phone) | compact, stage 0: the scene shows in the hero band, the 8dp gaps and margins, and under the navigation bar; no wings | slab 120–448dp; the far hill's lip meets the slab's foot; 56dp of frosted hill above the dock | wash and the top-bar cloud |
| 360×800dp | compact; stage ≈ 84dp, art drawn, no wings (< 160dp) | slab width-limited, more hill below it | as above |
| 412×915dp, 3-button bar (the 6.7-inch baseline) | stage 182–302dp in the no-chip and one-chip states: wings with all four sweets (§3.3 table); both chips: no wings | slab 231–611dp; ≈ 139dp of frosted hill between slab and dock | as above |
| 411×923dp Pixel_9, 54dp status bar, 24dp gesture bar (the QA device) | as the baseline, everything ≈ 30dp lower | the slab stands on F1's crest (§3.3) | as above |
| 800×1280dp tablet | column capped at 480dp and centred; `e` = 320dp, `ww` = 80dp: the sweets stand beside the art's lower half; the 160dp window margins show sky, clouds and hills | slab capped at 480dp; 160dp margins of sky and hill; 248dp of hill above the dock | wide wash |
| Any other window, 320–1280dp wide, aspect ratio 0.4–2.2 (a foldable, a split screen, a landscape-shaped window although landscape is not a supported orientation, `docs/ux-spec.md` §12) | renders with no gap in the sky or the ground, no clipped sweet (the fit rule omits rather than clips), nothing drawn outside the window | the same | the same |

Positions scale with the window; motif sizes do not (§4.2), so a sweet is the same physical size on
every phone.

### 8.5 At 200% font scale

| Screen | What changes | Why it is still right |
|---|---|---|
| Home | the title wraps, the column scrolls, the stage shrinks or goes to 0 and the wings with it | sweets live in the stage (B-12); the title's lower lines are the dark stop (§5.1) |
| Gameplay | the strip grows to ≈ 64dp | still within the top 0.16 H, zone T |
| Tutorial, catalogue, Results, Settings, Licences | columns and grids scroll further | Sparse is zone T everywhere |
| The backdrop itself | nothing (BD-20) | it reads no font size |

### 8.6 Right to left, dark theme

The scene mirrors in RTL (B-15), including the wings (the lollipop and gumdrop stand in the start
wing, which is then on the right). Dark is the night scene of B-14 with every §5 rule computed for it.

## 9. Assets and licences

| Asset | Kind | Source | Licence |
|---|---|---|---|
| The scene — sky, clouds, hills, frosting lips and drips, the lollipop, cane, gumdrop and cupcake, sprinkles, the stage wings | drawn: Compose `Canvas` from §3's geometry and §4's tokens; no asset file | in-house, this repository | project-owned |
| The two orbs | drawn (existing `HomeBlobs`) | in-house | project-owned |

**No third-party asset, no file, no raster, no font, no sound.** Kenney's CC0 pack is not used, as an
asset or as a redraw source (B-02), so THIRD_PARTY.md needs **no §2 row**. The backdrop plays no cue
and changes nothing in `docs/audio-spec.md` (rules/audio.md: nothing plays that the spec does not
name).

**THIRD_PARTY.md §1.** The research-analyst's idea row ("Candy-themed backdrop for the Home (menu) and
gameplay screens") is already written on `feature/phase-14-release-docs` (THIRD_PARTY.md:79 in the
`PegSolitarie-r5` worktree). It must be present on `release/3.0.0` exactly when this branch is; if this
branch misses the release, the row is deleted at checklist step 1.4, as the row itself says. Its
Credit cell keeps it **out of the in-app licences screen**, and `docs/ux/licences-screen.md` obeys.

**Repository hygiene.** No King screenshot, logo or marketing art enters the repository, `.planning/`
or `.planning/qa/` included — not as a reference, an underlay or a comparison (verdict §3b). This
document was written without one, and reviewers check it without one (BD-02).

**Store.** The listing screenshots are recaptured after the backdrop lands, as plain captures of the
app with no added art or framing (Q-016; verdict §3d) — the release-manager's.

## 10. Words, shapes, palette and store — the verdict applied

Every line of the verdict's §3, against this design. A reviewer ticks each one on the diff and the
goldens.

| Verdict line | This design |
|---|---|
| **3a.** No `Candy Crush`, `Crush`, `Saga`, `Sugar Crush`, `Candy Land`, `Candyland`, `Tiffi`, `Toffee`, `Yeti`, `Odus`, `King`, `Activision`, `Microsoft` | the backdrop adds no string (B-08); the licences screen's strings are checked by the same grep (BD-01) |
| **3a.** No `candy` in user-facing text or the listing | none added; new identifiers avoid it too (B-10) |
| **3a.** No `Sweet!` / `Tasty!` / `Delicious!` / `Divine!` / `Sugar Crush!` callouts | the backdrop has no text and no sound |
| **3a.** No reference to Candy Crush, King or "games like" it, anywhere | none; the THIRD_PARTY.md idea row stays out of the app (§9) |
| **3a.** Internal names may stay | `drawCandyPeg` and "Candy Clay" untouched; nothing new uses the words (B-10) |
| **3b.** None of the six regular candies, never as a colour-coded set | no red, orange, yellow, green or blue exists in the scene (B-04); no bean, lozenge, teardrop, square, plain round head or cluster is drawn; two house hues plus cream and lilac cannot form a six-colour set, and no motif is a token or a game piece |
| **3b.** No directional stripes, no twist-wrapped square with a glow, no chocolate ball studded with sprinkles | the cane is one colour; the cupcake has no flutes; one highlight per sweet; nothing is wrapped; nothing is brown and nothing is a ball; sprinkles lie in the stage's sky or on broad hills cut by the window |
| **3b.** No lollipop used as a hammer or a tool | the lollipop stands on a hill in a wing; it is never held, swung, or placed on the board, the dock or any control |
| **3b.** No character or mascot, no face | none; no eye, mouth or limb on any shape (§3.2's exclusions) |
| **3b.** No winding path of level nodes through themed episodes | no path, trail, signpost, node chain or numbered marker; the Challenge ladder keeps its own layout |
| **3b.** Not the board on a dark translucent grid of rounded tiles; keep the clay slab | the existing opaque clay slab, unchanged (B-06, §5.2, BD-18) |
| **3b.** No chunky candy-textured or striped lettering | no lettering in the scene; Fredoka and Nunito unchanged |
| **3b.** Nothing traced, eyedropped or laid out over a King image; no King image in the repo | every shape is built from §3's stated geometry; every colour is derived in §4.1's last column from the house palette; §9's hygiene rule |
| **3b.** Do draw: hills and frosting, cloud puffs, sprinkles, swirl discs, plain canes, gumdrops, cupcakes, the drifting blobs | exactly that set, each motif citing its line (§3.2); cookies, though allowed, are left out (§3.2) |
| **3b.** Keep motifs low-contrast behind the board | the game screen carries no standing sweet at all by default (B-06); its sprinkles stay ≥ 38dp below the slab (§3.4) |
| **3b.** Drift only when the animator duration scale is above 0 | §6: the orbs alone, static at scale 0 |
| **3b.** The peg board stays the hero of the game screen, the Home tiles the hero of the menu | §5.2 |
| **3c.** Colours from `Color.kt` tokens, light and dark, derived from violet / pink / gold / lavender; violet leads | §4.1: violet near hills, violet sweets, the violet orb and the app's own violet tiles lead; pink seconds; gold is kept out for its reward meaning |
| **3c.** Never eyedrop from King's art; never six colours with six shapes | §4.1's derivation column; BD-08's hue arc |
| **3c.** Accessibility unchanged | §5, BD-12–BD-17 |
| **3d.** Title `Peg Solitaire`; plain-capture screenshots; the English-33 icon | unchanged; §9 |

**This role's own reading of the two-question test** (a pre-assessment only — standing rule 1: the
binding answers are the reviewers', BD-02). *(1)* No single element is traceable to a King design: each
is a generic convention the verdict lists as free (§2 there), drawn from primitives stated here, in
the house palette. *(2)* No: the screens are a peg-solitaire cross on a lavender clay slab under a
lilac-pink sky with violet and pink frosted hills — no match-3 grid, none of the six candies, no
special candy, no character, no map, no callout, no King lettering.

## 11. Acceptance criteria

**Who checks what.** *Auditor* = the code-auditor, on the diff, the Check and the Roborazzi goldens.
*QA* = the qa-engineer, hands-on on a device: the Pixel_9a (or the Pixel_9 with the deviation
recorded) plus a 360×640dp window (`wm size 1080x1920` + `wm density 480`), light and dark, font scale
1.0 and 2.0, animator scale 1 and 0, TalkBack, three-button and gesture navigation, and the RTL
pseudo-locale `ar-XB`. *Test* = a named automated test (§12), deterministic.

| # | Criterion | By | Pass line |
|---|---|---|---|
| **BD-01** | **Words grep** — the three commands under this table, run from the repository root | Auditor, QA | all three print **zero lines** (the `-w` word match keeps "making" or "checking" out of the first). Play Console names, which no grep reaches, are the release-manager's (§14) |
| **BD-02** | **The two-question test** on every capture: Home (412×915dp: no chip, one chip; 360×640dp; tablet), gameplay (Classic English-33 mid-game, a Challenge level), tutorial step 1, catalogue, Results, Settings, Licences — light and dark | Auditor (goldens), QA (device captures) | for every capture, each reviewer writes Q1 "no" and Q2 "no" with one line of reason. **Any "yes" is a FAIL**, routed through the producer to the research-analyst. Reviewers answer from the verdict's §1b list and their own knowledge; no King image is fetched, viewed side by side or stored anywhere in the repo or `.planning/qa/` |
| **BD-03** | **Shapes checklist** — §10's rows ticked on the code and the goldens: the cane one colour; no motif with two highlights or two parallel strokes; no flutes; no brown, red, orange, yellow, green, blue or gold pixel in the scene; nothing held; no lettering; no path | Auditor | every row ticked |
| **BD-04** | **No asset, no dependency.** No new file under `res/drawable*`, `res/mipmap*`, `res/raw` or `assets/`; no image file (`*.png *.jpg *.jpeg *.webp *.gif *.svg *.avif`) anywhere in the diff; no new entry in `gradle/libs.versions.toml` or a module build file | Auditor | `git diff --stat develop...HEAD` shows none |
| **BD-05** | **Tokens only.** No hex outside `Color.kt`; no dp/sp literal (`dpSpLiteralCheck` green); every ratio, anchor and count in `BackdropMetrics`; any duration in `ClayAnim`; detekt and ktlint green with no new `@Suppress` | Auditor, Check | Check green; grep of the new files for `0x`/`.dp`/`.sp` literals empty |
| **BD-06** | **Density per destination**: Home Landscape; gameplay Quiet, for Classic and for Challenge; tutorial, catalogue, Results, Settings, Licences, Licence text Sparse | Test, QA | as listed |
| **BD-07** | **Layers per density**, read from the backdrop's test property: Landscape {sky, orbs, scene}, plus the stage's wings iff the stage art is drawn and `Sh ≥ 160dp`; Quiet {sky, scene}; Sparse {sky, scene with one cloud} | Test | as listed, on the three reference windows and the three Home chip states |
| **BD-08** | **Colour constraints**, computed from `Color.kt` in a JVM test: `BackdropSkyTop` equals `background` in each scheme; zone-T colours L ≥ 0.779 (light) and ≤ 0.0357 (dark); every dark colour ≤ 0.0095 or ≥ 0.165; sky order (§4.1); every token's hue in 250°–335° | Test | all hold |
| **BD-09** | **Status-bar area pixel-identical to today** on Home, gameplay and every Sparse screen, light and dark (a capture of the top `statusInset` band against `develop`'s), apart from the primary orb's peek on Home, which is today's | QA, Test | identical |
| **BD-10** | **Composition constants** match §3.3 and §4.3: every sky stop, mound, cloud, puff, ratio, anchor and list length | Test | equal |
| **BD-11** | **Geometry rules** at `w360dp-h640dp`, `w412dp-h915dp`, `w800dp-h1280dp`, `w1280dp-h800dp` (and the stage sizes of §3.3's table): every standing sweet's base on its hill within 2dp; a wing sweet drawn iff its top is ≥ 8dp inside the stage; the lollipop's stick and the gumdrop's dome, and the cane's stem and the cupcake, ≥ 4dp apart horizontally, and the cane's tail ≥ 8dp above the cupcake's top, wherever both are drawn; wing sprinkles drawn iff ≥ 4dp clear of every drawn sweet and centred outside the art; hill sprinkles obey §3.4 rules 1–6; nothing drawn outside the window | Test | all hold |
| **BD-12** | **Text contrast on the device**: every uncontained text of §5.1's table sampled against the backdrop directly behind it — at rest and scrolled to the end, at 1.0 and 2.0, light and dark, 360×640dp and the Pixel_9 | QA | body and label text ≥ 4.5:1; the Home title's dark-stop glyph area ≥ 3:1; the title's light stop no lower than on `develop` |
| **BD-13** | **Hero edges at rest**, sampled just outside and just inside the edge of the gameplay slab, the dock, the back chip, the timer pill, the stage art, the ribbon pills, both mode tiles and the Continue chips, light and dark, against the same capture on `develop` | QA | every ratio ≥ its `develop` value (§5.2's table is the expectation) |
| **BD-14** | **Transparent containers**: every `Scaffold`, the four `TopAppBar`s and the tutorial's root `Surface` paint no colour; the theme's window `Surface` is still there and unlabelled text still takes `onBackground` in both themes (rules/compose.md F-32) | Test, Auditor | as stated |
| **BD-15** | **Semantics**: the backdrop and the stage wings have no description, no click, no focus and no `pointerInput`; Home's, gameplay's and the tutorial's TalkBack traversal orders are exactly `docs/ux-spec.md` §11's; a TalkBack pass on the device finds no new focus stop on any screen | Test, QA | as stated |
| **BD-16** | **Reduced motion**: with the animator scale at 0 the orbs are present, at rest, and nothing in the backdrop requests a frame; on Quiet and Sparse nothing ever requests a frame; with the scale at 1, Home's only animated elements are the orbs | Test, QA | as stated |
| **BD-17** | **The board untouched, pixel for pixel**: `GameScreen` rendered with and without the backdrop, light and dark, mid-game with a selection, legal-target dots and a hint showing — the slab's interior (inset 2dp from its edge) identical; likewise the interiors of the Home stage art, both mode tiles and the Continue chips | Test | zero differing pixels |
| **BD-18** | **What sits under the board** is the opaque `surfaceContainerHigh` clay slab at full alpha with its `clayShadow` — no grid, no translucency, no blur | Test, Auditor | as stated |
| **BD-19** | **Layouts unchanged**: the phase-15 criteria 15g, 15j, 15k, 15n–15r, 18a–18e and 21a–21g re-run green on this branch; on the device 18b's inset clause holds; with three-button navigation the system's navigation-bar scrim is visible over the hills; with gesture navigation the handle is legible | Test, QA | all green; scrim and handle present |
| **BD-20** | **Font independence**: the backdrop alone, rendered at font scale 1.0 and 2.0 on the same window, is pixel-identical | Test | identical |
| **BD-21** | **The sky is continuous across navigation**: on Home → catalogue → gameplay → Results → Home, a pixel in the top band never changes and no sky pixel lightens toward `background` at any frame of a transition (controlled clock in the test; a screen recording on the device) | Test, QA | as stated |
| **BD-22** | **RTL**: in `ar-XB` the scene is the mirror of the LTR golden, wings included | Test, QA | mirrored |
| **BD-23** | **Cold start**: (a) Home's first composition draws the sky and the orbs only — the scene and the wings are absent from the layers property on the first frame and present after one frame; (b) 15e on this branch, §6.5's protocol, is **PASS** (a VOID sitting is not a pass); (c) janky frames in the first second of a cold start (`dumpsys gfxinfo`) are at most arm A's + 1 | Test, QA | (a) as stated; (b) PASS; (c) as stated |
| **BD-24** | **Greyscale**: device captures of Home, gameplay and Settings converted to greyscale keep every BD-12 and BD-13 ratio, and pegs remain distinct from holes | QA | as stated |
| **BD-25** | **Previews and goldens**: every new public composable has light and dark `@Preview`s; Roborazzi goldens for each density × theme on the three reference windows, plus Home's three chip states on 412×915dp; the goldens are what BD-02's auditor half is run on | Auditor, Test | present and green |
| **BD-26** | **THIRD_PARTY.md**: on the release branch, the §1 idea row is present; no §2 row was added for the backdrop; the licences screen does not list the row | Auditor, release-manager | as stated |

BD-01's commands (`docs/store` may not exist yet on this branch; grep then reports it missing, which
is not a hit):

```sh
grep -rniwE 'candy|candies|crush|saga|candyland|tiffi|toffee|yeti|odus|king|activision|microsoft' \
  app/src/main/res/values*/strings.xml app/src/main/assets docs/store
grep -rniE '(sweet|tasty|delicious|divine)!' app/src/main/res docs/store
git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet)'
```

## 12. Tests owed

Deterministic only (rules/testing.md): no wall clock, no unseeded random, no sleeps; transitions on
the Compose test clock; the system-bar insets supplied per the phase-15 spec's §13 rule. Names read as
sentences; each watched fail once before it is trusted.

**The test hook.** One custom semantics property on the backdrop's node, carrying the set of layers it
drew — `Sky`, `Orbs`, `Scene` — and one on the stage, carrying which wing sweets it drew, in the
`PegCountKey` idiom (`GameScreen.kt`). Neither is read by TalkBack; both make BD-06, BD-07, BD-11 and
BD-23 assertable without pixels.

| Test (illustrative names) | Kind | Covers |
|---|---|---|
| `BackdropColorsTest` — "zone-T colours read 4.5:1 under every text role in both themes", "every dark backdrop colour is darker than the background or brighter than every container", "the sky's top colour is the background", "every backdrop hue lies in the violet–pink arc" | JVM | BD-08 |
| `BackdropMetricsTest` — "each density's sky stops, mounds and clouds are the spec's" | JVM | BD-10 |
| `BackdropGeometryTest` — "every standing sweet stands on its hill", "a wing sweet that would reach the stage's top edge is omitted", "hill sprinkles stay inside their hill and clear of the board", at the four windows | JVM | BD-11 |
| `BackdropDensityTest` — "Home draws the landscape, the board the quiet scene, every other screen the sparse one" | Compose UI | BD-06, BD-07 |
| `BackdropSemanticsTest` — "the backdrop is not focusable, clickable or announced", "TalkBack order is unchanged on Home, gameplay and the tutorial" | Compose UI | BD-15 |
| `BackdropReducedMotionTest` — "with animations off the orbs are drawn at rest and nothing animates" | Compose UI | BD-16 |
| `BackdropFirstFrameTest` — "Home's first frame draws only the sky and the orbs" | Compose UI | BD-23 (a) |
| `BackdropFontScaleTest` — "the backdrop is identical at 1.0 and 2.0 font scale" | Compose UI (captured image) | BD-20 |
| `BoardInteriorIdentityTest` — "the slab's interior is identical with and without the backdrop", and the same for the stage art, tiles and chips | Roborazzi | BD-17, BD-18 |
| `TransparentContainersTest` — "every scaffold, top bar and the tutorial root paint no colour, and unlabelled text is onBackground in dark mode" | Compose UI | BD-14 |
| `SkyContinuityTest` — "a destination change never lightens the sky" | Compose UI, test clock | BD-21 |
| Goldens — each density × light/dark × three windows, Home's three chip states, one `ar-XB` capture | Roborazzi | BD-22, BD-25 |
| The phase-15 suites for 15g, 15j, 15k, 15n–15r, 18a–18e, 21a–21g | existing | BD-19 |

Every screen keeps its one Compose UI test of its main flow (rules/compose.md); none needs a change
for the backdrop beyond running with it present.

## 13. Considered and rejected

So none of these is re-proposed as an improvement:

- **Vector drawables** for the motifs — an XML inflate on Home's first use, against D-03g (B-02).
- **Kenney's CC0 candy pack**, as-is or redrawn — flat 70px side-view PNG platformer tiles: a style
  mismatch with clay, and the app's first raster (verdict §4b). **AI-generated art** — outside the
  matrix, probably uncopyrightable, and the likeliest way to reproduce King's look (verdict §4c).
- **A saturated sky with every text on a pill** — it would restyle five approved screens to buy
  colour where the founder's eye is not.
- **Sweets anchored to the window on Home** (the first draft) — on the founder's one-chip phone they
  fell behind the ribbon and the chips, and a wrapped title could cover them; and **a title-wraps
  flag** to patch the second problem. The stage wings solve both (B-12).
- **Sweets in the window margins on tablets** as a second placement — one placement (the wings)
  serves every window and keeps the sweets beside the board on every device.
- **Parallax, drifting clouds, twinkling or falling sprinkles, a swaying lollipop** — motion without
  meaning, a reduced-motion channel to manage, and a frame cost on screens that are idle today (B-05,
  §6).
- **A per-mode tint** for Challenge (B-07). **A different scene per Classic board, per level or at
  random** — screenshots would stop being reproducible and the two-question test could not be run
  once for all; seeded variety would be its own spec later.
- **A Quiet tutorial** (the first draft's §2) — its caption would sit on a hill at ≈ 3.5:1 (§2).
- **A translucent slab, a backdrop seen through the board, a blurred or frosted panel behind it, a
  dark grid of rounded tiles under it** — the verdict's banned board treatment, or a cost to the
  hero for nothing; blur is also a dismissal cue (ui-ux-pro-max `blur-purpose`) and expensive on
  low-end phones.
- **Standing sweets around the board by default** — offered to the founder instead (Q-NNN option 2).
- **Stars, a sun, a rainbow, a castle or gate, a chocolate river, gingerbread figures, cookies,
  wrapped sweets, jelly beans, gummy worms** — gold is reserved; a rainbow is a many-colour set with
  blue, red and green in it; a castle or a figure is a character or a place in a map; brown is where
  the colour bomb lives; wrapped sweets and beans are King's candies by shape.
- **Mint or sky-blue accents "to read as more candy"** — they would end violet's lead and start a
  colour-coded set (B-04).
- **A mid-tone violet dark sky** — it would sit at the slab's own luminance and erase its edge (B-14).

## 14. Open items routed to the producer

1. **Q-NNN — how much of the scene the game screen carries.** Drafted in the report in FOUNDER.md
   format; non-blocking; the default (Quiet, B-06) is specified in full and is what gets built, and
   options 2 and 3 are specified in §3.3 so an answer is a constant change.
2. **The colour-blind palette option** rules/compose.md requires of games **does not exist** in the
   app (§5.3). Pre-existing, not introduced here, and not this task's to build; it needs its own small
   UX spec (this role's) and a developer task, or a code-auditor-approved record that the default
   palette's luminance-and-shape coding meets the rule. The producer decides which, and whether it is
   in 3.0.0.
3. **The Home title gradient's light stop** reads ≈ 2.4:1 against the light background, under the
   3:1 large-text bar (§5.1). Pre-existing and unchanged by the backdrop; a one-line theme ruling for
   this role on its own task (for example, starting the title's gradient at the CTA's dark stop).
4. **How the backdrop is hosted** so the sky stays continuous across navigation (B-13, BD-21) — one
   instance behind the `NavHost`, or another way — is the tech-lead's.
5. **The 15e sitting on this branch** (BD-23 (b)) needs the §6.5 conditions: the Pixel_9a alone, a
   quiet host, precondition 7's block-boundary swap. The producer schedules it.
6. **Merge order.** This branch builds on phase 15's layouts — the D-03l stage, the D-05d strip and
   dock, the bundled fonts — so it is built and reviewed after `feature/phase-15-ux-refresh` merges,
   and BD-19 re-runs that phase's criteria on top.
7. **For the release-manager:** the THIRD_PARTY.md §1 row travels with this branch to
   `release/3.0.0` (BD-26); the store screenshots are recaptured as plain captures after this lands
   (Q-016); the Play Console achievement and leaderboard names are checked against the verdict's §3a
   word list, which no grep in the repository can see.
8. **Unaffected and still open elsewhere:** the tech-lead's ruling on O-B (the orbs' idle frame cost);
   the qa-engineer's O-A (whether the timer's value is spoken), which is this role's on its own task.
