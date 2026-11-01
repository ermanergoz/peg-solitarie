---
role: ux-designer
task: candy-backdrop (routed items 2 and 3 of docs/ux/candy-backdrop.md §14)
slug: colour-vision
status: draft
created: 2026-10-17
revised: 2026-10-18 (amendments for backdrop direction B, Q-021: §11; earlier text kept, with dated pointers); 2026-10-24 (deviation D-1, the hollow star: CV-10a, CV-13a, §12); 2026-10-24 (the founder's Q-027 answer, option 2, the standard empty star: CV-13b, §13); 2026-11-01 (SN-1: a two-star Results is not reachable in play; CV-AC11 and §13.5 corrected by dated notes)
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
layout-contract: docs/ux-spec.md (phase 15: §4.4.1, §4.5, §6.2, §11)
companions: docs/ux/candy-backdrop.md, docs/ux/licences-screen.md
---

# Peg Solitaire — Colour vision: the colour-blind palette and the Home title

> **The colour-blind palette** is a switch in Settings › General, "Colorblind palette", off by
> default and saved with the other settings. Turned on, it changes only what the board audit (§2)
> finds missing. Every change makes a mark readable by luminance or by pattern, so it serves
> protanopia, deuteranopia, tritanopia and monochromacy with one palette:
> - the selection ring gets a halo;
> - the legal-target marks turn opaque;
> - the hint rings turn deep amber and dashed;
> - stars and pips get a visible fill and outline.
>
> Pegs, holes, the dock, the backdrop and Home are untouched.
>
> **The Home title** is large-scale text, so the bar is 3:1. Today its light stop fails that at a
> large font scale and over the coming backdrop. It gets a violet gradient of its own, one step
> deeper in light and one step lighter in dark, which reads ≥ 5.1:1 and ≥ 6.5:1 at rest. The CTA
> gradient, the font, the size and the layout are unchanged.

## 0. Scope, inputs, method and the rules that bind this document

**What this specifies.** Two accessibility fixes the 3.0.0 release needs, both found while the
candy-backdrop spec was being written (`docs/ux/candy-backdrop.md` §5.1, §5.3, §14 items 2 and 3):

1. **A colour-blind palette option.** rules/compose.md requires one of games ("never colour alone
   (shape, pattern, label); colour-blind palette option in games"), and the app has none. §2 audits
   what the board already encodes without hue, so that the option adds only what is missing. §3
   specifies the option itself: where it lives in Settings, what it changes, the palette and the
   deficiencies it serves, how each was checked, how it persists, and its default.
2. **The Home title gradient's light stop.** It reads about 2.4:1 against the light background.
   §4 says which bar applies and why, and specifies the fix in light and dark, inside the Candy Clay
   system and the founder's Q-004 answer ("keep the palette and the style").

§5 holds the acceptance criteria for the code-auditor and the qa-engineer, including the simulated
deficiency checks. This is a design document. It contains no code; the stack's developer builds it.

**Inputs read.**
- `docs/ui/DESIGN-SYSTEM.md`.
- On `feature/phase-15-ux-refresh` (worktree `PegSolitarie-p15`): `docs/ux-spec.md` §4.4.1, §4.5,
  §6.2, §6.3, §11 and §12; and the Compose sources:
  - theme: `ui/theme/Color.kt`, `Clay.kt`, `ClayAnim.kt`, `Theme.kt`, `Type.kt`;
  - board: `ui/components/BoardCanvas.kt`, `BoardGeometry.kt`, `ui/game/BoardMetrics.kt`,
    `GameActionDock.kt`, `DockLook.kt`;
  - Settings: `ui/settings/SettingsScreen.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`,
    `data/SettingsRepository.kt`;
  - Home: `ui/home/HomeHero.kt`, `HomeScreen.kt`, `HomeProgressRibbon.kt`;
  - stars and pips: `ui/results/ResultsScreen.kt`, `ui/classic/BoardPreview.kt`,
    `ui/onboarding/TutorialScreen.kt`;
  - `MainActivity.kt`, `res/values/dimens.xml`, `res/values/strings.xml`.
- On `feature/candy-backdrop`: `docs/ux/candy-backdrop.md` (all of it, so the palettes and the
  backdrop's tokens agree) and `docs/ux/licences-screen.md` (its Settings renumbering, §2).
- FOUNDER.md: Q-004 and Q-019.

**Method: how every number here was computed.**
- **Contrast** is the WCAG 2.x relative-luminance ratio, computed from the `Color.kt` hexes.
  Translucent marks are blended in gamma-encoded sRGB over the colour they are drawn on, which is
  what Compose's default `drawCircle(alpha = …)` does.
- **Colour-vision deficiencies** are simulated with Machado, Oliveira and Fernandes (2009), severity
  1.0, applied to linear sRGB, for **protanopia**, **deuteranopia** and **tritanopia**.
  **Monochromacy** is luminance only. Severity 1.0 (dichromacy) is the worst case of each type: the
  anomalous trichromacies (protanomaly, deuteranomaly, tritanomaly) sit between it and normal vision
  on the same axis, so a pair that separates at 1.0 also separates for them.
- **Colour difference** is CIEDE2000 (ΔE00) in CIELAB, D65, between the two simulated colours.
- One script, run in three passes, did all of the above. Its numbers are quoted here and in the
  report. The device reading governs, as everywhere in this product (§5, CV-AC11 to CV-AC14).

**Rules that bind every line below.**
1. rules/compose.md: never colour alone; a colour-blind palette option in games; text contrast
   ≥ 4.5:1; touch targets; reduced motion honoured; every colour a `Color.kt` token; every dp a
   `dimens.xml` token; every ratio in a scoped constants object; light and dark `@Preview`s.
2. `docs/ux-spec.md` §11, Contrast: "≥ 4.5:1 for all body and label text and ≥ 3:1 for glyphs and
   boundaries, in both light and dark". This document reads a board mark (the selection ring, a
   legal-target mark, a hint ring, a star or pip outline) as a glyph or a boundary in that sense.
3. Q-004: the founder kept the palette and the overall style. Nothing here adds a new hue family to
   the standard look: the colour-blind palette is opt-in, and the title fix moves one step along the
   violet ramp the title already uses.
4. The candy-backdrop verdict and spec: no new string may contain a banned word (backdrop BD-01), and
   nothing here changes a backdrop token or a backdrop rule.

**What this does not touch.**
- No rule, footprint, score, generator, solver, control, touch target, readout, sound or haptic
  changes.
- The **standard palette**, the one every player sees by default, keeps every colour and every piece
  of board geometry it has today (CV-13). Its shortfalls are recorded in §7, not fixed.
  *Amended 2026-10-24 (CV-13a, §12): one glyph form changes in it. The unearned star is hollow in
  both palettes, so the stars stop differing by colour alone. No colour and no board geometry
  changes.*
  *Amended again 2026-10-24 (CV-13b, §13): one colour changes in it as well, by the founder's
  Q-027 answer. The unearned star takes `onSurfaceVariant`. The board is unchanged.*
- The **dock** (§3.7), the **backdrop** (§3.7), **text colours** and the **CTA gradient** (§4.5) are
  not changed.

## 1. Decisions

House style per `docs/ux-spec.md` §3: `- **ID:** <ruling> — <rationale>.` Never rewritten in place; a
later ruling is `CV-nna` with a pointer.

**The colour-blind palette**

- **CV-01: One colour-blind palette behind one switch, not one palette per deficiency.** — A single
  palette whose every distinction holds in luminance and pattern serves protanopia, deuteranopia,
  tritanopia, their milder anomalous forms, and monochromacy alike. The player does not need to
  know their type (many do not), and QA checks one palette instead of three (§8).
- **CV-02: Luminance and pattern carry every distinction; hue is chosen along the axes each
  deficiency keeps.** — Every vision keeps luminance. The palette's two accents are violet (the move
  marks) and amber (the hint and the reward). They lie far apart on the blue–yellow axis, which
  protans and deutans keep (the great majority of colour-blind players), and also on the
  red–green axis, which tritans keep (ΔE00 ≥ 29 under tritanopia, §3.5).
- **CV-03: It lives in Settings › General as a switch row directly after Haptics.** It is labelled
  "Colorblind palette", with the supporting line "Clearer moves, hints and stars". — General already
  holds what the player tunes about how the game feels. A section of its own would be a header over
  one row. The label uses the word players look for in game settings, and the supporting line says
  what changes (§3.1).
- **CV-04: Off by default, persisted in DataStore with the other settings, device-local.** A failed
  read means off. Android's system colour correction is never read to turn it on. — Off keeps the
  look the founder approved for everyone (Q-004). The store and the failure path are the existing
  settings' own (§3.2). Reading the system's daltonizer state needs non-SDK keys, and the player's
  own choice is the reliable signal (§8).
- **CV-05: The palette is part of the theme.** It reaches the board (gameplay and the tutorial),
  the stars (Results, catalogue, tutorial completion) and the tutorial's done pip.
  Home draws nothing it changes. — One source, no per-screen reads. Home's first frame needs no new
  read (D-03g), so cold start is untouched and nothing flashes there (§3.3).
- **CV-06: Pegs and holes are unchanged.** — Peg against hole already separates by size, form and
  luminance in every vision (§2), and the candy-pink peg is the identity Q-004 kept.
- **CV-07: The selection ring gains a halo.** The halo is a 1.5dp band of `surfaceContainerLowest`
  directly inside the ring, and the ring moves out to 0.42c. — The ring then reads ≥ 4.17:1 against
  both of its neighbours, the halo and the slab, in every vision, where today it merges into the
  peg's shaded half at 1.02–1.18:1. This is the two-colour indicator technique, and it holds in
  light and dark (§3.4).
- **CV-08: The legal-target mark becomes opaque.** It stays `primary`, alpha 1.0 instead of 0.38,
  same size. — 4.17–5.34:1 against the slab in every vision, up from 1.73–1.96:1.
- **CV-09: The hint rings become dashed and opaque.** Eight dashes; deep amber `#B45309` in light,
  `tertiary` gold in dark; the source ring at 0.44c, still larger than the selection ring. — The
  light rings move from 1.61–1.98:1 to 3.82–5.25:1. In greyscale the hint and the selection reach
  the same luminance in light (1.13:1), so the hint carries a pattern the selection does not: dashed
  against solid with a halo.
- **CV-10: Stars and the done pip read by luminance.** Earned stars are filled and outlined;
  unearned ones are outlined in `onSurfaceVariant`; in light the fill is amber (`#C05A08 → #B45309`).
  — Filled against hollow becomes visible in greyscale: the light fill reads ≥ 3.22:1 and every
  outline ≥ 5.0:1 against the surface beneath, in every vision.
- **CV-10a (2026-10-24, amends CV-10 and §3.4's star rows): "outlined" means the hollow star, and
  the spec names the shape, not an icon constant.** The outline of CV-10 and §3.4 is the hollow star:
  Material's `star_border` artwork, which is the filled star's own silhouette with the interior open,
  drawn in one colour at the artwork's own band weight. It serves as the unearned star and as the
  outline the colour-blind palette draws over an earned star. It is not `Icons.Outlined.Star`, which
  in `material-icons-core` is a filled star. The hollow star as built at `32b2600` is accepted. How it
  is constructed is the tech-lead's call (§12.3). — The spec named an icon constant for a shape, and
  that constant draws a different shape.
- **CV-11: The dock is unchanged.** — Every pair of its five looks already differs in two or more
  non-hue channels, and its On disc reads ≥ 4.17:1 in every vision (§2).
- **CV-12: The backdrop is unchanged and needs no colour-blind variant.** — It encodes nothing, it
  shares no token with the board's marks, and the slab the marks live in is opaque
  (`docs/ux/candy-backdrop.md` §5.3).
- **CV-12a (2026-10-18, amends CV-12 for direction B): neither the B backdrop nor the gloss finish
  nor the gel mode tiles needs a colour-blind variant.** — The waves, the lights, the gloss and the gel
  encode nothing, and they share no token with the board's marks. The two gel tiles are told apart by
  label, badge glyph, trailing art and the level chip, never by hue alone. The dock's discs, and the
  slab fill around them, are unchanged. §11.2 has the check.
- **CV-13: The standard palette is not changed by this document.** — Its marks never use colour
  alone, but four of them fall below `docs/ux-spec.md` §11's 3:1 for glyphs and boundaries (§7).
  Changing them would change the board the founder approved, so it is drafted as a founder question,
  with a default that keeps today's look.
- **CV-13a (2026-10-24, amends CV-13 and §2's star row): the standard palette's unearned star is
  hollow too. This is its only change.** Earned stars stay filled, the unearned star keeps
  `outlineVariant`, and no colour, size, motion, sound, semantics or board mark moves. — CV-13 rested
  on §2's claim that the standard palette never uses colour alone. That claim fails for the stars.
  Today's unearned star is a filled star (`Icons.Outlined.Star`), so earned and unearned stars differ
  by colour alone. rules/compose.md forbids that without exception, and an opt-in palette cannot
  cure it. Filled against hollow is the smallest fix, and Q-004's palette is kept (§12.2).
- **CV-13b (2026-10-24, amends CV-13 and CV-13a; the founder's Q-027, option 2, "stars only"): the
  standard palette's unearned star takes `onSurfaceVariant`, the colour-blind palette's colour for
  it.** That is `#635F69` in light and `#A79FB5` in dark, for the hollow star of CV-10a wherever an
  unearned star draws (Results' 48dp row, the catalogue's 16dp pips). With CV-13a's hollow form,
  these are the standard palette's only changes. The earned star keeps `GoldGradient` with no
  outline. The tutorial's upcoming step pip keeps its `outlineVariant` ring: it is not a star. No
  board mark, size, motion, sound or semantics moves, and no hex is added. — This is the founder's
  answer, not a UX decision. In `outlineVariant` the hollow band read 1.20–1.50:1 and nearly
  vanished at 16dp (F-CV4). In `onSurfaceVariant` it reads 5.31–5.58:1 in light and 5.59–6.95:1 in
  dark at normal vision, and ≥ 5.02 / ≥ 5.56 in every vision (§3.5 row 11, §13.3).

**The Home title**

- **TG-01: The title takes a gradient of its own, one step along the violet ramp.** Light
  `#7C3AED → #5B21B6`, dark `#C4B5FD → #A78BFA`, in the CTA brush's geometry. — It keeps the violet
  gradient title (Q-004), reads ≥ 5.10:1 (light) and ≥ 6.49:1 (dark) at rest, and leaves the CTA
  gradient and every button alone (§4.3).
- **TG-02: The bar is 3:1, because the title is large-scale text.** The design target is ≥ 4.5:1
  at rest. — 36sp SemiBold is large-scale under WCAG 2.2 SC 1.4.3, and `docs/ux-spec.md` §11's 4.5:1
  is written for body and label text. Meeting 4.5:1 anyway satisfies rules/compose.md read literally
  (§4.2).

## 2. What the board already encodes without hue

This section is read from `ui/components/BoardCanvas.kt` (`drawCell`, `drawCandyPeg`, `drawDimple`,
`drawSelectedPeg`, `drawHintHighlight`), `BoardGeometry.kt` and `ui/game/BoardMetrics.kt`, plus the
star and pip renderers. Radii are fractions of the cell edge `c`. Ratios are "worst of normal,
protanopia, deuteranopia, tritanopia and monochromacy" (§0's method). The standard palette is
the one built on `feature/phase-15-ux-refresh`.

| Element | How it is drawn today | Channels other than hue | Luminance today: light / dark (worst vision) | What is missing for a colour-blind player |
|---|---|---|---|---|
| **Peg against empty hole** | Peg: a sphere of radius 0.34c, a radial gradient (`#F9A8D4 → #EC4899 → #BE185D` light, `#FBC5E2 → #F472B6 → #BE185D` dark) plus a specular dot. Hole: a two-tone dimple of radius 0.16c, `dimpleShadow` ring and offset `dimpleFill` | **Size** (the peg is 2.1 times the hole's diameter), **form** (a shaded sphere with a highlight against a flat inset ring), luminance | Peg body against dimple fill: 2.29–3.43 / 5.32–7.03 | **Nothing.** Peg against hole is told apart in every vision by size and form, and by luminance on top of that |
| **Selected peg** | The peg at 1.08 scale with a breathing pulse (1.0 → 1.06; off with reduced motion), plus a `primary` ring of radius 0.40c and 3dp (`peg_selected_ring_width`) touching the peg | **Size**, the ring's **silhouette**, **motion** | Ring against slab: 4.17–4.86 / 5.17–5.34. Ring against the peg's own edge (the edge colour sampled every 15° around the drawn gradient): **1.02–1.18 at the shaded side**, in every vision and both themes | **The ring merges into the peg.** Where the ring meets the peg's shaded half, the two are the same luminance, so in greyscale (and nearly so under protanopia) the selection reads only as "a slightly bigger peg" |
| **Legal-target mark** (Show-moves on) | A `primary` disc at alpha 0.38, radius 0.18c, over the dimple (0.16c), so it covers the hole and a thin ring of slab | **Form** (a flat disc replaces the two-tone dimple), a 12% larger radius, a pop-in (off with reduced motion) | Against the dimple fill: 1.67–1.69. Against the slab: 1.73–1.75. Dark: 1.93–2.04 | **Luminance.** The disc reads under 2.1:1 against everything around it in every vision, below §11's 3:1. A normal-vision player separates it by chroma (ΔE00 19–21), a monochromat at ΔE00 13 |
| **Hint** | `tertiary` gold at alpha 0.9: a source ring of 0.42c around peg A, a target ring of 0.30c around hole B, both 3dp, and a connector line at alpha 0.5, 0.06c wide | **Pattern**: two rings plus a connector. The source ring is larger than the selection ring (`BoardCanvasTest`) | Rings against slab: **1.61–1.98 light**; 6.49–7.58 dark. Connector: 1.32–1.47 light. Hint against selection: 2.11–2.80 light / 1.22–1.47 dark; ΔE00 in greyscale 27 light / **7.9 dark** | **Light: luminance.** The rings sit under 2:1 on the slab in every vision. **Dark: a non-hue difference from the selection ring beyond the connector.** Both are bright rings of nearly the same size, and in greyscale they differ by ΔE00 8 |
| **Stars** (Results 48dp, catalogue pips 16dp, tutorial completion star) | Earned: `Icons.Filled.Star` recoloured with `GoldGradient`. Unearned: `Icons.Outlined.Star` in `outlineVariant` | **Shape** (filled against hollow). *Amended 2026-10-24 (§12.1): wrong. `Icons.Outlined.Star` is a filled star, so the two stars share one silhouette and one fill and differ by colour alone; CV-13a makes the unearned star hollow* | Light: earned fill against the background 1.09–2.22, unearned outline 1.29, catalogue pip outline 1.23. Dark: earned 9.58–15.9, unearned 1.48–1.50, pip outline 1.20 | **Luminance.** The unearned outline is near-invisible in both themes. In light the earned fill barely separates from the background, so "filled against hollow" carries almost nothing in greyscale. *2026-10-24 (CV-13b, §13): the standard unearned star now takes `onSurfaceVariant` and reads ≥ 5.31:1 at normal vision. The standard earned fill in light is unchanged (F-CV5)* |
| **Tutorial step pips** | Done: a solid `GoldGradient` disc. Current: a `surface` disc with a `primary` ring. Upcoming: a `surface` disc with an `outlineVariant` ring. The row speaks "Step N of 3" | **Shape** (filled, ringed, faint), and the spoken text | Current ring: 4.68–5.45 / 5.86–6.07. Done fill in light: 1.09–2.22 (the star's gold) | **The done pip's fill in light** (as the stars). The current and upcoming pips are fine: the faint ring is meant to be faint |
| **The dock's on/off language** | Five looks from `DockLookRecipe` (§4.4.1 of the phase-15 spec): fill, ring, shadow, glyph shape (plain, slashed or spinner), on mark | **Every pair of looks differs in at least two non-hue channels** (criterion 21b) | The On disc against the slab: 4.17–4.86 / 5.17–5.34 (criterion 21f wants ≥ 3:1) | **Nothing** |
| **Mode tiles, Continue chips, ribbon, rescue banner, dialogs** | Text labels, glyph shapes, containers | Labels, shapes | Text pairs from the design system's verified set | **Nothing.** *Amended 2026-10-18 (§11.2): under direction B the tiles are gel with white text (≥ 4.90:1 where text sits) and still miss nothing* |
| **The backdrop** | Decoration, outside the opaque slab (`docs/ux/candy-backdrop.md`) | — | Every rule there is a luminance ratio (§5.3 there) | **Nothing.** It encodes nothing. *Amended 2026-10-18 (§11.2): the same holds for direction B's waves, lights and gloss finish* |

**What the audit says.** The standard palette never uses colour alone: every state has a shape
channel, which is what rules/compose.md's first clause asks. *Amended 2026-10-24 (§12.1): this
holds for every board mark and fails for the stars, which differed by colour alone until CV-13a.*
What it lacks is **luminance** where hue, or rather chroma, does the work for a normal-vision
player. Four places:
1. the selection ring against the peg's shaded edge;
2. the legal-target mark against its hole;
3. the light-theme hint rings, and the hint against the selection ring in dark greyscale;
4. the star and pip outlines and fills.

A colour-blind player keeps luminance and loses part of the chroma, so these four places are
exactly what the option must add. Nothing else on the board needs to change. Pegs, holes and the
dock already work.

## 3. The colour-blind palette

### 3.1 The setting

| Element | Spec |
|---|---|
| Where | Settings › **General**, directly after **Haptics** and before the Play Games section. The group then reads: How to play, Sound, Haptics, Colorblind palette |
| Row | A `SettingsToggleRow`, the exact peer of Sound and Haptics: label `settings_colorblind_label` "**Colorblind palette**", supporting line `settings_colorblind_supporting` "**Clearer moves, hints and stars**", and an M3 `Switch` with the row's existing colours |
| Size | `settings_row_min_height` (72dp) like its peers; the whole row is not a target, the switch is, as for Sound and Haptics |
| Semantics | The switch carries the label as its content description (the rows' existing idiom), so TalkBack says "Colorblind palette, switch, off" or "…, on". No new focus stop beyond the switch |
| Sound and haptics | **Silent.** A toggle takes no press cue (D-09). The sound row's confirmation pop is Sound's own special case and is not copied |
| Effect | Immediate. No dialog and no restart. The next frame of any board, star row or pip row draws in the chosen palette. Settings itself shows no change except the switch |
| Entrance orders | A new `COLORBLIND_ROW_ORDER` of 4, and every later order moves up by one. Without the licences screen: Play Games 5–8, Privacy 9–11. With `docs/ux/licences-screen.md` landed: Play Games 5–8, About 9, Licenses 10, Privacy 11–13. Either way it is a whole renumbering, Privacy stays last, and whichever of the two changes lands second renumbers on top of the first. `SettingsScreenTest`'s rendered-top check covers both |
| Copy | US English, like the rest of `res/values/` ("Catalog", "Licenses"). Neither string contains a word from the candy-backdrop BD-01 list |
| 200% font | The supporting line wraps under the label; nothing truncates |

### 3.2 Persistence and default

- **Store.** DataStore Preferences in the shared `app_prefs` store, through `SettingsRepository`
  like `hintsEnabled`, `soundOn` and `hapticsOn`:
  - `colorblindPaletteOn: Flow<Boolean>` and `setColorblindPaletteOn(on: Boolean)`;
  - the key `booleanPreferencesKey("colorblind_palette_on")` in `SettingsPreferences`, with
    `DEFAULT_COLORBLIND_PALETTE = false`;
  - the read uses the same `catch { emit(emptyPreferences()) }`, and the write the same
    best-effort `storageFailSafe`.
- **Default: off.** A fresh install shows the standard palette.
- **A failed read means off, too.** This is the repository's only flag whose fallback is `false`,
  and its KDoc must say why, beside D-17's fail-open rule. D-17 protects assists: a damaged store must
  never strip a beginner's help. This flag is a look. A corrupt store resets every setting at once,
  and showing the alternative look to every player whose store failed would misstate the default
  the founder approved. A player who had chosen the palette sees the change at once, and the row is
  one tap away.
- **`SettingsUiState` gains `colorblindPaletteOn: Boolean = false`.** The pre-first-emission value
  equals the store's default, so the switch never flickers (the existing KDoc's reasoning).
- **Device-local.** It is not part of the cloud progress payload. The profile swap's replace path
  does not touch it (its copy lists results, history, the Challenge level and in-progress games,
  and settings are none of these). Restart, the tutorial and Skip do not touch it either.

### 3.3 How it reaches the screens

- **One source, read once.** The repository flow is collected once at the app root and passed to
  `AppTheme`, which provides the palette through a `CompositionLocal` beside `LocalClayColors`
  (illustratively `LocalBoardMarks`). No composable reads the repository itself. How the root
  collects it (an activity-scoped state holder, a Hilt-provided `StateFlow` with an initial value)
  is the tech-lead's call. Three properties are binding:
  1. before the first read lands, the standard palette is in use;
  2. a configuration change never flashes the other palette;
  3. a change in Settings reaches every consumer without recreating anything.
- **Consumers, the complete list:**
  - `BoardCanvas` in interactive mode (gameplay, Classic and Challenge, and the tutorial's board):
    the selection, the legal-target marks and the hint;
  - Results' `StarRow`;
  - the catalogue's `EarnedStarPips`;
  - the tutorial's `StepPips` (the done pip only) and `CompletionStar`.
- **Not consumers.** None of these draws anything the palette changes, so none reads it:
  - **Home**, all of it. The stage art draws pegs and holes only (`HomeStagePositionTest`), the
    ribbon's star glyph is decorative and the pill speaks its value as text, and the Continue
    chip's arrow is decorative;
  - the catalogue's preview boards (`interactive = false`: pegs and holes only);
  - the dock, every dialog and banner, all text, and the backdrop.

  Home's first frame therefore performs no new read and looks pixel-identical with the palette on
  or off (CV-AC06; D-03g's "no blocking read" is untouched).
- **Process restored straight into gameplay.** If the first read has not landed when the board
  draws, the board shows the standard palette for those frames and then switches. There is no
  dialog and no flash beyond that one change.
- **Test hook.** One custom semantics property on the board-state node, in the `PegCountKey` idiom,
  reports the palette in use (`Standard` or `Colorblind`). TalkBack does not read it.

### 3.4 The palette, mark by mark

Radii are fractions of the cell edge `c`. Every cell size the app draws, from about 32dp (a 9×9
footprint on the narrowest window) to about 83dp (the tutorial's 3×3), is covered by the numbers
below.

*Amended 2026-10-24: the Standard column is unchanged except for the unearned star, which takes
CV-13a's hollow form and CV-13b's `onSurfaceVariant` (§12, §13).*

| Mark | Standard (unchanged) | Colour-blind palette, light | Colour-blind palette, dark |
|---|---|---|---|
| **Selection ring** | `primary`, radius 0.40c, 3dp, solid, touching the peg | `primary` `#7C3AED`, radius **0.42c**, 3dp, solid; plus the **halo**: a 1.5dp band of `surfaceContainerLowest` `#FDFBFF` whose outer edge meets the ring's inner edge | the same, with `primary` `#A78BFA` and halo `surfaceContainerLowest` `#15101F` |
| Selected peg | 1.08 scale, breathing pulse | unchanged; the halo and the ring scale with the pulse | unchanged |
| **Legal-target mark** | `primary` at alpha 0.38, disc of 0.18c | `primary` at **alpha 1.0**, same disc | the same |
| **Hint source ring** | `tertiary` at alpha 0.9, 0.42c, 3dp, solid | **`#B45309`** (amber-700, new token) at alpha 1.0, **0.44c**, 3dp, **dashed** | `tertiary` `#FBBF24` at alpha 1.0, 0.44c, 3dp, dashed |
| **Hint target ring** | `tertiary` at alpha 0.9, 0.30c, 3dp, solid | `#B45309` at alpha 1.0, 0.30c, 3dp, dashed | `#FBBF24` at alpha 1.0, 0.30c, dashed |
| Hint connector | `tertiary` at alpha 0.5, 0.06c wide | `#B45309` at alpha 0.5 | `#FBBF24` at alpha 0.5 |
| **Earned star** (Results 48dp, catalogue pips 16dp, tutorial completion star) | `Icons.Filled.Star` with `GoldGradient` | filled with **`#C05A08 → #B45309`** (new tokens, `GoldGradient`'s linear geometry), plus an outline: `Icons.Outlined.Star` drawn over it in `onTertiaryContainer` `#6B4E00` *[2026-10-24, CV-10a: the outline is the hollow star of §12.3, not `Icons.Outlined.Star`]* | `GoldGradient` fill unchanged, plus the outline (the hollow star, CV-10a) in `onTertiaryContainer` `#FFEFC7` |
| **Unearned star** | `Icons.Outlined.Star` in `outlineVariant` *[2026-10-24, CV-13a: the hollow star, in `outlineVariant`]* *[2026-10-24, CV-13b (Q-027): the hollow star in `onSurfaceVariant`, `#635F69` light and `#A79FB5` dark, as in the colour-blind columns]* | the same glyph in **`onSurfaceVariant`** `#635F69` *[the hollow star, CV-10a]* | the hollow star in `onSurfaceVariant` `#A79FB5` |
| **Done step pip** | a `GoldGradient` disc | a `#C05A08 → #B45309` disc | `GoldGradient`, unchanged |
| Current and upcoming pips | `primary` ring, `outlineVariant` ring | unchanged | unchanged |
| Results star glow | `tertiary` at alpha 0.35 | unchanged (decorative) | unchanged |
| Pegs, holes, specular dot | as built | unchanged | unchanged |

**The dash pattern.** Eight dashes per ring, each 22.5° of arc, with 22.5° gaps and butt caps (round
caps would close the gaps). The pattern starts at the ring's topmost point and is fixed: it never
rotates or animates. Each dash is 5.5dp long on the source ring and 3.8dp on the target ring at
c = 32dp, and 14.3dp and 9.8dp at c = 83dp. Both rings stay visibly dashed at every size.

**Geometry this implies, so nobody re-derives it:**
- **The halo** is drawn after the selected peg and before the ring. Where the halo overlaps the
  sphere's outermost band, it covers it.
- **The selected peg stays about resting size.** Its visible part has radius 0.42c − 3dp: 4% below
  a resting peg at c = 32dp, 2.5% above at c = 42dp. The whole selected mark, to the ring's outer
  edge, is 37% larger than a resting peg at c = 32dp.
- **The hint source ring stays strictly larger than the selection ring:** 0.44 against 0.42, as
  `BoardCanvasTest` already requires of the standard 0.42 against 0.40. Its inner edge leaves
  0.10c − 1.5dp of slab (≥ 1.7dp) between it and an unselected peg.
- **A hint and a selection on the same peg** (the player has just selected the hinted peg) overlap.
  The hint is drawn over the selection, as today, and reads as "selected and hinted".
- **No mark collides with a neighbouring cell's marks.** At the pulse's peak the selection ring's
  outer edge is 0.445c + 1.5dp from its centre (15.7dp at c = 32dp). A neighbour's hint source ring
  comes no nearer than 0.56c − 1.5dp (16.4dp).

**Tokens.** Every new value lives in `ui/theme/Color.kt`, and there are only two:
- `LightColorblindHint` = `#B45309`;
- `LightColorblindStarFillColors` = `[#C05A08, #B45309]`.

Everything else is an existing role or token: `primary`, `surfaceContainerLowest`, `tertiary`,
`onTertiaryContainer`, `onSurfaceVariant`, and the dark `GoldGradient` values. The dark theme adds no
hex. The radii, the alphas and the dash count go in a scoped constants object beside `BoardMetrics`
(illustratively `ColorblindBoardMetrics`: `SELECTED_RING_RATIO` 0.42, `HINT_SOURCE_RING_RATIO`
0.44, `HINT_TARGET_RING_RATIO` 0.30, `DESTINATION_DOT_ALPHA` 1.0, `HINT_HIGHLIGHT_ALPHA` 1.0,
`HINT_ARROW_ALPHA` 0.5, `HINT_RING_DASHES` 8). The halo width goes in `dimens.xml` as
`peg_selection_halo_width`, 1.5dp. It carries the file's documented sub-4dp stroke exception, beside
`peg_selected_ring_width`.

**Hue check.** The new amber is 26.0° (`#B45309`) and 26.7° (`#C05A08`), the reward family of
`tertiary` (`#F59E0B`, 38°) taken deeper. Neither it nor anything else here is a blue, a green or a
red. The board's marks stay inside the house vocabulary: violet for the player's move, amber and gold
for the hint and the reward.

### 3.5 The deficiencies it serves, and how each was checked

**Served:**
- protanopia and protanomaly;
- deuteranopia and deuteranomaly;
- tritanopia and tritanomaly;
- monochromacy (achromatopsia).

Every distinction below holds in luminance or in pattern, which every one of these keeps. The
violet–amber pair is also far apart in hue for each dichromacy (row 6).

**Checked by computation (§0's method).** Columns are normal / protanopia / deuteranopia /
tritanopia / monochromacy. "Today" is the standard palette for comparison.

| # | Pair | Light, colour-blind palette | Dark, colour-blind palette | Today, light / dark (worst) | Line |
|---|---|---|---|---|---|
| 1 | Selection ring against the slab | 4.86 / 4.17 / 4.51 / 4.53 / 4.86 | 5.21 / 5.32 / 5.17 / 5.34 / 5.21 | the same | ≥ 3:1 |
| 2 | Selection ring against its inner neighbour (the halo; today, the peg's edge) | 5.54 / 4.76 / 5.14 / 5.16 / 5.54 | 6.85 / 7.01 / 6.79 / 7.05 / 6.85 | **1.02 / 1.02** | ≥ 3:1 |
| 3 | Halo against the peg's edge, sampled every 15° around the drawn gradient (information: what WCAG asks of the ring is rows 1–2) | the shaded half ≥ 3.36; the lit top-start ≥ 2.33, where the ring itself already differs from the peg by ≥ 1.56 | the shaded half ≥ 2.85 (protanopia), ≥ 3.79 otherwise; the lit half ≥ 4.67 | — | recorded |
| 4 | Legal-target mark against the slab | 4.86 / 4.17 / 4.51 / 4.53 / 4.86 | 5.21 / 5.32 / 5.17 / 5.34 / 5.21 | 1.73 / 1.93 | ≥ 3:1 |
| 5 | Hint rings against the slab | 4.28 / 5.25 / 3.82 / 4.33 / 4.28 | 8.50 / 7.68 / 9.01 / 7.83 / 8.50 | 1.61 / 6.49 | ≥ 3:1 |
| 6 | Hint against selection, ΔE00 | 51.2 / 61.0 / 63.2 / 35.9 / **3.3** | 62.7 / 60.8 / 61.0 / 29.4 / **12.0** | 27.4 / 7.9 in greyscale | carried by **pattern**: dashed against solid with a halo |
| 7 | Hint target ring against legal-target mark | the same colours as row 6; shapes differ (a dashed 0.30c ring against a solid 0.18c disc) | the same | — | shape |
| 8 | Hint connector against the slab (supplementary: the two rings identify the move) | 1.99 / 2.15 / 1.90 / 2.01 / 1.99 | ≈ 3.2 (normal vision, computed by hand) | 1.32 | recorded |
| 9 | Earned-star outline against the background and the Results sky (≈ 6.6 on the catalogue card, normal vision) | ≥ 6.02 in every vision | ≥ 15.0 | — | ≥ 3:1 |
| 10 | Earned-star fill against the background, the Results sky and the catalogue card (filled against hollow) | light stop ≥ **3.22**, deep stop ≥ 3.64, in every vision | ≥ 7.68 (unchanged gold) | 1.09 / 9.58 | ≥ 3:1 |
| 11 | Unearned-star outline against the background, the Results sky and the catalogue card | ≥ 5.02 in every vision | ≥ 5.56 | **1.23 / 1.20** *[2026-10-24, CV-13b: the standard palette now draws this row's colours, so it reads ≥ 5.02 / ≥ 5.56 too (§13.3)]* | ≥ 3:1 |
| 12 | Done pip against the tutorial background | as row 10: ≥ 3.56 | unchanged, ≥ 9.58 | 1.09 | ≥ 3:1 |
| 13 | For reference, unchanged: peg body against dimple fill; the dock's On disc; the current pip's ring | 2.29–3.43; 4.17–4.86; 4.68–5.45 | 5.32–7.03; 5.17–5.34; 5.86–6.07 | the same | already met |

**Checked on a device and from screenshots.** This is the qa-engineer's half. It is specified as
CV-AC11 to CV-AC14:
- **On the device:** Android's own Developer options › Simulate color space, which offers
  Monochromacy, Deuteranomaly, Protanomaly and Tritanomaly. These are the anomalous forms, so they
  are milder than the computation.
- **From screenshots:** plain device captures transformed offline with the same severity-1.0
  matrices and the luminance-only transform, and sampled at the pixels rows 1–12 name.
- **In tests:** the computation itself becomes a JVM test over `Color.kt` (CV-AC07). The numbers
  above are then re-proved on every Check, not trusted from this document.

### 3.6 Motion, reduced motion, sound

- **Unchanged.** The selection pulse, the legal-target pop and the jump all behave exactly as
  today, under the same `rememberAnimationsEnabled()` gate. The halo and the ring pulse together.
- **Nothing new ever moves.** The dashes do not rotate, and the outlines and fills are static.
- **Switching the palette is instant.** It is a change of state, not a transition, so it has no
  cross-fade.
- **No cue is added or changed** (rules/audio.md).

### 3.7 What it does not change, and why

- **Pegs and holes** (CV-06), **the dock** (CV-11), **the backdrop** (CV-12): §2's audit finds
  nothing missing in any of them. The backdrop's BD-17 (the slab's interior identical with and
  without the backdrop) runs once per palette.
- **Text, containers, dialogs, banners, the ribbon, the mode tiles and the Continue chips:**
  labelled, and drawn from verified pairs.
- **Home** (CV-05): nothing on it encodes a state in a colour the palette touches.
- **The Home title:** §4's fix applies to both palettes alike.
- **The standard palette** (CV-13): §7. *Amended 2026-10-24: apart from CV-13a's hollow unearned
  star (§12).* *Amended again 2026-10-24: and CV-13b's `onSurfaceVariant` for that star, by the
  founder's Q-027 answer (§13).*

## 4. The Home title gradient

### 4.1 What is drawn today, and what it measures

`HeroTitle` (`ui/home/HomeHero.kt`) draws `app_name` at `displaySmall`: Fredoka SemiBold, 36sp, line
height 44sp. It has no `maxLines` and is merged with `ctaGradientBrush()`, the CTA gradient. That
gradient is vertical. Stop 0 is the light colour and stop 0.5 the deep colour, held to the bottom
(`Clay.kt`: `CTA_LIGHT_STOP` 0, `CTA_DARK_STOP` 0.5). The brush spans the whole text box, so on a
wrapped title one sweep covers every line.

| Theme | Light stop | Deep stop |
|---|---|---|
| Light | `#A78BFA` | `#7C3AED` |
| Dark | `#B6A0FC` | `#8B5CF6` |

| Where (light theme) | Ratio against the background |
|---|---|
| The light stop itself (the top of the text box) | **2.44:1**, the "about 2.4:1" of the finding |
| A one-line title's ascender tops (about a fifth of the way down the box) | ≈ 3.2:1 |
| A two-line title's top line (about a tenth of the way down) | ≈ 2.8:1 |
| A three-line title (200% on a 360dp phone) | ≈ 2.6:1 |
| The lower half (the deep stop) | 5.10:1 |
| The light stop over the backdrop's `SkyMid` / `CloudShade` (zone T, once the backdrop lands) | 2.20 / 2.19:1 |

| Where (dark theme) | Ratio against the background |
|---|---|
| The light stop | 7.92:1 |
| The deep stop | **4.17:1**, under 4.5:1 across the whole lower half of every glyph |
| The deep stop over the Home orbs | 3.6:1 |

The glyph positions are estimates from the face's metrics; the device reading governs (TG-AC03).
Every blend between the stops lies between the two stops' ratios, because every channel moves the
same way from one stop to the other.

### 4.2 Which bar applies, and why

**3:1 applies, because the title is large-scale text.** WCAG 2.2 SC 1.4.3 gives large-scale text,
at least 18pt (24 CSS px) or 14pt bold, a minimum of 3:1. The title is 36sp SemiBold. That is 1.5 times
the 24px threshold by size alone, under any mapping of sp to CSS px that keeps 1sp near one
device-independent pixel at 100% font scale. It only grows from there.

The studio's own wording points the same way:
- rules/compose.md's "contrast ≥ 4.5:1" is the studio's text rule.
- `docs/ux-spec.md` §11 states that rule as "≥ 4.5:1 for all body and label text". A display
  wordmark is neither body text nor label text.
- The backdrop spec already reads this title at 3:1 (§5.1 there).

So 3:1 is the line a reviewer fails on, everywhere the title can be: at rest, wrapped, scrolled,
over the backdrop, in both themes.

**Today's title fails it.** In light it fails wherever the light half carries glyph pixels at a
large font scale (2.6–2.8:1), and over the backdrop's zone-T sky (2.2:1). At rest, at the default
scale, it clears 3:1 by about 0.2, which is too thin for a device reading with anti-aliasing. In
dark it passes 3:1 but fails the 4.5:1 that rules/compose.md states without exception.

**The design target is higher than the bar.** Every glyph pixel at rest reads **≥ 4.5:1** in both
themes, so the fix passes rules/compose.md read literally as well, with headroom for anti-aliasing.
The binding line stays 3:1 for every state the title can reach (TG-AC02 to TG-AC04).

### 4.3 The fix: a title gradient of its own, one step along the violet ramp

**TG-01.** The title gets its own gradient, `TitleGradient`. Its geometry is the CTA brush's
(vertical, a light lip at 0, the deep body from 0.5), and its colours sit one step deeper along the
same violet ramp in each theme:

| Theme | Light stop (0) | Deep stop (0.5 → 1) | Against the background | Worst over the backdrop's text-safe colours (zone T) | Worst anywhere a wrapped title can reach |
|---|---|---|---|---|---|
| Light | `#7C3AED` (= light `primary`, today's deep stop; hue 262°) | `#5B21B6` (violet-800; hue 263°) | **5.10–8.05:1** | 4.58:1 (`CloudShade`), 4.61:1 (`SkyMid`); 4.55:1 over the pink orb | 3.23:1: the light stop over the far hill's pink (`BackdropHillFar`), §4.4 |
| Dark | `#C4B5FD` (violet-300; hue 252°) | `#A78BFA` (= dark `primary`, hue 255°) | **6.49–9.56:1** | 6.49:1 everywhere in zone T; 5.58:1 over either orb | 6.49:1 over the night hills; the dark frosting lip fails, §4.4 |

**Why these values.**
- **The palette and the style stay (Q-004).** It is still a violet gradient title in Fredoka,
  light at the top and deep below. In light, the light stop is the violet the title already ends
  in. In dark, the deep stop is the dark theme's own `primary`. No new hue family is added (252°–263°,
  inside the backdrop's 250°–335° arc).
- **The sweep stays visible.** The lightness difference between the stops is ΔL* 12.3 in light and
  12.6 in dark, about three-fifths of today's 21.2 and 19.4. The two half-step alternatives that also
  pass, `#7C3AED → #6D28D9` and `#B6A0FC → #A78BFA`, flatten it to ΔL* ≈ 6, which reads as one
  colour (§8).
- **One test covers the whole sweep.** Every channel moves the same way between the stops, so every
  blended pixel lies between the two stops' ratios.

**What the developer builds** (names illustrative, values binding):
- In `Color.kt`: `LightTitleGradientColors = [#7C3AED, #5B21B6]` and
  `DarkTitleGradientColors = [#C4B5FD, #A78BFA]`.
- A `titleGradient` field in `ClayColors`, provided by `AppTheme` like the others.
- A `titleGradientBrush()` beside `ctaGradientBrush()`, with its own two stop constants (0 and 0.5:
  one constant per owner, the house rule).
- `HeroTitle` switches to `titleGradientBrush()`. Nothing else in the app uses it.

### 4.4 Over the backdrop

> **Amended 2026-10-18.** This subsection was computed against the sweets landscape, which direction B
> replaced. Its method stands, and its numbers are re-derived for B in §11.3. The dark-theme failure
> it reports, the far mounds' frosting lips under a wrapped title, cannot occur under B. The text
> below is kept as the record.

**At rest** the title sits on `SkyTop`, which equals `background` pixel for pixel
(`docs/ux/candy-backdrop.md` §3.3). §4.3's background column is therefore the at-rest reading on
the branch with the backdrop and on the branch without it.

**The orbs.**
- The violet orb peeks past the content box's top-end corner, under the gear chip. The title ends
  where the gear begins, so it does not reach that orb. Its light stop would read 4.43:1 there,
  the one at-rest value in this section under 4.5:1.
- The pink orb sits at the start edge at 0.32 H (backdrop B-05), where a wrapped title can pass
  over it: 4.55:1 in light, 5.60:1 in dark.

**Wrapped**, the title can reach down to about 0.46 H: at 200%, on a 360dp phone, it wraps to
three lines (backdrop §5.1).
- **Light: passes.** The backdrop colours there are zone T, the frosting lip and the far hill's
  pink. The worst glyph pixel is the light stop over the pink, at 3.23:1 (≥ 3:1). The lower lines
  are the deep stop, at 5.09:1 even over the pink. The bound behind this is that every light
  backdrop colour the title can overlap has L ≥ 0.503. It holds for the backdrop as specified: the
  near hills, at L 0.286, crest below the title's reach, and every motif lives in the stage, below
  the hero row.
- **Dark: fails, and the backdrop has to move, not the title.** The far mounds' frosting lips
  (`BackdropFrosting` dark `#A98BC2`, L 0.308) begin at 0.40 H and 0.46 H. That is inside a
  three-line title's reach, under the glyphs of its last line. The title reads 1.08–1.59:1 against
  that lip, today's title 1.32–1.44:1. No title colour can fix this: a title light enough to read
  on the night sky is as light as the lip. For ≥ 3:1 against the deep stop, every dark backdrop
  colour inside the title's reachable box needs **L ≤ 0.0786**. The backdrop spec's §5.1 claims
  "dark: ≥ 4.2:1 everywhere", and this case breaks that claim. I may not edit that document under
  this dispatch, so the finding is routed (§10 item 1), with a proposed rule for its owner. In dark
  Landscape, no colour brighter than L 0.0786 may lie above the wrapped title's lowest reach
  (≈ 0.47 H). For example, the far mounds' lips could take a dark-band value, or their crests could
  move below that line. TG-AC04 checks this on the device, whichever way it is fixed.

### 4.5 What the fix does not change

The CTA gradient (`ctaGradientBrush()`) and every button on it are unchanged: the tutorial CTA,
Results' Replay, the dialogs' confirm actions. Their `onPrimary` labels already sit on its deep half
by construction. The title's text, face, size, weight, start alignment, wrapping, `heading()`
semantics, `clayEntrance` and the hero row's layout are unchanged too. The colour-blind palette
does not touch the title. The title encodes nothing, and the fix already gives it ≥ 4.5:1 on its
background in both themes.

## 5. Acceptance criteria

**Who checks what.**
- *Auditor:* the code-auditor, on the diff, the Check and the Roborazzi goldens.
- *QA:* the qa-engineer, hands-on on the Pixel_9a (or the Pixel_9, with the deviation recorded),
  plus a 360×640dp window (`wm size 1080x1920` + `wm density 480`). The matrix is light and dark,
  font scale 1.0 and 2.0, and animator scale 1 and 0, with TalkBack on for the Settings and board
  checks.
- *Test:* a named automated test (§6), deterministic.

QA artifacts (captures, the simulation script and the samples) live under
`.planning/qa/<task-slug>/` and are never committed (rules/testing.md).

### 5.1 The colour-blind palette

| # | Criterion | By | Pass line |
|---|---|---|---|
| **CV-AC01** | **The row.** Settings › General shows "Colorblind palette" / "Clearer moves, hints and stars" with a switch, directly after Haptics. The entrance orders are renumbered whole (§3.1) and Privacy is still last. The row is ≥ 72dp, its switch is announced by TalkBack as "Colorblind palette, switch" with its state, flipping it plays no cue, and at 200% the supporting line wraps without truncation in light and dark | Test, QA | `SettingsScreenTest`'s rendered-top order check passes with the row; the TalkBack pass hears the name, role and state; no cue fires (`ButtonSoundTest` idiom) |
| **CV-AC02** | **Persistence.** Off on a fresh install. A choice survives process death and a cold start. A failed read gives off. The key is `colorblind_palette_on` in `app_prefs`. It is not in the cloud payload, and the profile swap's "Use their cloud progress" leaves it as it was | Test (in-memory and throwing stores, the `SettingsRepositoryTest` idiom), QA (kill and relaunch) | as stated |
| **CV-AC03** | **Live switch.** With a game saved, flip the switch, go back and resume: the board draws in the new palette with no restart. Flip it back: standard again | Test (the board's palette semantics property), QA | as stated |
| **CV-AC04** | **Tokens only.** The two new hexes (§3.4) are in `Color.kt` and nowhere else. The ratios, alphas and dash count are in a scoped constants object, and the halo width in `dimens.xml`. The palette is provided by `AppTheme` and read through its `CompositionLocal` only. No new `@Suppress`; detekt and ktlint are green | Auditor, Check | Check green; a grep of the diff for `0x` outside `Color.kt` and for `.dp`/`.sp` literals is empty |
| **CV-AC05** | **Exactly §3.4.** With the palette on, the board draws the halo and the 0.42c ring, opaque legal-target marks, the dashed opaque hint rings (0.44c and 0.30c, eight dashes) and unchanged pegs and holes. Stars and the done pip draw as §3.4. **With it off, every screen is pixel-identical to the branch's base**. *Amended 2026-10-24 (§12.5): one exception, the unearned stars on Results and in the catalogue, which are hollow in both palettes (CV-13a). Added: in both palettes and both themes, an unearned star's centre shows the surface beneath it, and an earned star's centre shows its fill.* *2026-10-24 (§13.5): the exception now covers the unearned stars' colour as well, `onSurfaceVariant` in both palettes (CV-13b)* | Test (Roborazzi goldens, both palettes, light and dark) | goldens match; the off-palette goldens equal the base's, except the unearned stars |
| **CV-AC06** | **Home is untouched.** Home renders pixel-identically with the palette on and off, in both themes and all three chip states, and its first frame performs no read of the setting | Test, Auditor | identical; no read on Home's first frame (code review against D-03g) |
| **CV-AC07** | **Contrast constraints, computed from `Color.kt`.** A JVM test computes rows 1, 2, 4, 5, 9, 10, 11 and 12 of §3.5 under normal vision, the three Machado-2009 severity-1.0 simulations and luminance only, and also samples the peg's drawn edge (row 3). It also asserts that the title stops meet §4.3 (TG-AC02) | Test | every "≥ 3:1" row holds in every vision |
| **CV-AC08** | **Not colour alone.** In the palette's recipe, every pair of marks that can share a board differs in at least one non-hue channel: selection (solid, halo) against hint (dashed, no halo); legal-target mark (solid disc) against hint target (dashed ring); earned (filled) against unearned (hollow). The hint source radius is strictly larger than the selection radius. *Amended 2026-10-24 (§12.5): earned against unearned holds in **both** palettes' recipes. The standard recipe's board pairs already differ in form, radius or dashes, so the criterion covers both recipes whole* | Test (a JVM test over the recipe, the `DockLookRecipeTest` idiom) | as stated |
| **CV-AC09** | **Motion.** With the animator scale at 0 there is no pulse and no pop, the halo and the ring are static, and the dashes never move. At scale 1, only the existing pulse and pop animate | Test, QA | as stated |
| **CV-AC10** | **Nothing else moved.** The TalkBack traversal on gameplay, the tutorial, Results and the catalogue is unchanged (`docs/ux-spec.md` §11). `PegCountKey`, `LegalTargetCountKey`, `EarnedStarCountKey` and every content description are unchanged. No touch target changed. The dock's 21a–21g pass in both palettes | Test, QA | as stated |
| **CV-AC11** | **Simulated deficiencies on the device.** The palette is on. Developer options › Hardware-accelerated rendering › **Simulate color space** is set in turn to Monochromacy, Deuteranomaly, Protanomaly and Tritanomaly, each in light and dark. QA plays a fixed scenario: Classic English-33 mid-game with a peg selected and Show-moves on (legal targets showing); then a revealed hint; then Wiegleb-45 on the 360×640dp window with a selection and a hint (the smallest cells); then Results with two of three stars; the catalogue's pips; tutorial step 2. For each capture, QA states whether it can tell apart the selected peg from the others, each legal target from a plain hole, the hint's source and target from the selection and the legal targets, earned from unearned stars, and the done pip from the others, and names the cue it used. The same scenario with the palette off is recorded as the baseline | QA | **every distinction is made, in every mode and theme, by a cue §3.4 names**; the off-palette baseline is reported, not graded. QA records whether its screen captures show the simulation (screencap may capture the unsimulated frame); if not, it judges on the device and relies on CV-AC12 for numbers. *Amended 2026-10-24 (§12.5): one baseline item is graded. In the standard palette at normal vision, earned stars are filled and unearned stars are hollow, on Results and on a catalogue card, in light and dark* *Corrected 2026-11-01 (SN-1, `.planning/reports/colour-vision/tech-lead-signoff.md` item 5; QA's `qa-engineer.md` finding): "Results with two of three stars" cannot be reached in play. Results follows only a win (`GameStatus.Won`), and a win (pegs ≤ the board's minimum) is exactly the three-star band, so on the device Results always shows three earned stars. On the device QA captures Results as play reaches it (three of three), and the unearned star is judged on the catalogue card. The unearned star on Results is carried by the JVM: `ResultsStarPaletteTest` sets a two-star state directly (QA: the only proof of it), and the goldens row below specifies the same constructed state. Whether a one- or two-star Results should be reachable in play is a gameplay question, routed to the game-designer, and not ruled here* |
| **CV-AC12** | **Simulated deficiencies from screenshots.** On plain captures of CV-AC11's scenario (simulation off), QA applies the Machado 2009 severity-1.0 protanopia, deuteranopia and tritanopia transforms (on linear sRGB) and the luminance-only transform with a helper script under `.planning/qa/`. It samples the stroke-centre pixels of the selection ring, the halo, a legal-target mark, both hint rings, the star outlines and the star fill, and the adjacent slab or background pixels | QA | each sampled pair is within 0.3 of §3.5's figure or above it (anti-aliasing allowance), and every "≥ 3:1" row is ≥ 3:1. *Amended 2026-10-24 (§12.5): a catalogue pip's hollow band is about 3px wide on the Pixel_9a. If its centre pixel misses row 11 by more than the allowance, Results' 48dp star sets the number and the pip is judged by eye under CV-AC11* |
| **CV-AC13** | **Dashes are dashes at the smallest size.** On Wiegleb-45, 360×640dp, palette on, monochromacy: a zoomed capture shows eight separate dashes on the source ring and on the target ring, and a solid ring with a light inner band on the selection | QA | as stated |
| **CV-AC14** | **With the system's own colour correction.** Accessibility › Color and motion › Color correction is on in each of its modes, with the palette on. CV-AC11's distinctions still hold | QA | every distinction still made; a failure is reported to the ux-designer, it does not block the merge |
| **CV-AC15** | **Strings and previews.** Two new strings (§3.1). The candy-backdrop BD-01 grep prints nothing. Every new or changed public composable has light and dark `@Preview`s, and the board previews show both palettes | Auditor | as stated |

### 5.2 The Home title

| # | Criterion | By | Pass line |
|---|---|---|---|
| **TG-AC01** | **Tokens.** `LightTitleGradientColors = [#7C3AED, #5B21B6]` and `DarkTitleGradientColors = [#C4B5FD, #A78BFA]` are in `Color.kt`. A `titleGradient` in `ClayColors`. A `titleGradientBrush()` with stops at 0 and 0.5. `HeroTitle` uses it. `ctaGradientBrush()`, its colours and every other caller are unchanged | Auditor | the diff shows exactly this |
| **TG-AC02** | **Contrast, computed.** Each title stop against its theme's `background`: light 5.10 and 8.05, dark 9.56 and 6.49, so ≥ 4.5:1 for both stops in both themes. Every channel moves the same way between the stops. Each stop is ≥ 4.5:1 against the backdrop's zone-T tokens in its theme and against the orbs' blends: light ≥ 4.58 against `CloudShade` and ≥ 4.55 against the pink orb, with one exception, the primary orb at 4.43, which sits under the gear and not the title (§4.4). In light, each stop is ≥ 3:1 against `BackdropHillFar`, `BackdropSkyHorizon` and `BackdropFrosting`. **Amended 2026-10-18 (§11.3):** under direction B, the backdrop clauses read: each stop ≥ 4.5:1 against B's zone-T sky tokens in its theme (light `SkyTop`, `SkyMid`; dark `SkyTop`, `SkyMid`, `SkyLow`, `SkyDeep`; there are no orbs); and in light, each stop ≥ 3:1 against every lane light's rim composite over the sky at 0.47 H (the worst is a Pink rim, L 0.522) | Test (JVM, from `Color.kt`) | as stated |
| **TG-AC03** | **Contrast on the device.** On Home at rest, font scale 1.0, light and dark, on the Pixel_9 and at 360×640dp, QA samples the topmost pixels of the ascenders and pixels in the lower half against the pixels beside them. At 2.0 on 360×640dp (three lines) and on the Pixel_9, it samples every line | QA | at rest **≥ 4.5:1** in both themes; at 2.0 **≥ 3:1** for every sampled glyph pixel, except TG-AC04's case |
| **TG-AC04** | **Dark, wrapped, over the backdrop.** ~~At 2.0 on 360×640dp in dark, with the candy backdrop landed, no backdrop pixel inside the wrapped title's box has L > 0.0786.~~ **Target amended 2026-10-18 (§11.1):** at 2.0 on 360×640dp in dark, with the backdrop landed, **no pixel of the direction's dark backdrop above 0.47 H has L > 0.0786**, at rest and at every pose of its motion | QA, Test (`docs/ux/candy-backdrop.md` BD-08 and BD-11: every lane light's rim composite ≤ 0.0786, no two lane lights overlapping, no wave pixel above 0.47 H) | as stated. ~~Fails against the backdrop as specified today (§4.4), so it passes only once the backdrop's owner has amended the far lips (§10 item 1).~~ Direction B meets it by construction: the worst point is a Pearl light's rim at L 0.047 (§11.1) |
| **TG-AC05** | **Nothing else moved.** Every CTA-gradient button is pixel-identical to the base: the tutorial CTA, Results' Replay, the dialogs. The title's face, size, weight, alignment, wrapping, heading semantics and entrance are unchanged, as are the phase-15 criteria 14, 15i and 15o. The title is identical with the colour-blind palette on and off | Test (goldens), Auditor | as stated |

## 6. Tests owed

Deterministic only (rules/testing.md): no wall clock, no unseeded random, no sleeps. Names read as
sentences, and each test is watched failing once before it is trusted. The names are illustrative.

| Test | Kind | Covers |
|---|---|---|
| `ColorblindPaletteContrastTest`: "every colour-blind mark reads 3:1 against its neighbours in every simulated vision"; "the halo separates the ring from the peg's shaded edge" | JVM. It holds the Machado 2009 severity-1.0 matrices, the WCAG luminance and a sampled radial-gradient edge, so §3.5 is re-proved on every Check | CV-AC07 |
| `TitleGradientContrastTest`: "both title stops read 4.5:1 on the background in both themes"; "the title gradient is monotone"; "the title clears 3:1 over every backdrop colour it can reach in light" | JVM | TG-AC02 |
| `ColorblindMarkRecipeTest`: "every pair of marks that can share a board differs in a channel that is not hue"; "a hint ring is larger than the selection ring in both palettes" | JVM | CV-AC08 |
| `SettingsRepositoryTest` additions: "the colorblind palette is off on a fresh install"; "a failed read leaves it off"; "the choice round-trips" | JVM | CV-AC02 |
| `SettingsViewModelTest` and `SettingsScreenTest` additions: "flipping the colorblind switch persists it"; "the colorblind row sits after Haptics, in order" | JVM, Compose UI | CV-AC01, CV-AC02 |
| `ColorblindPaletteLiveSwitchTest`: "the board redraws in the chosen palette without leaving the screen" | Compose UI (palette semantics property) | CV-AC03 |
| Goldens: the gameplay board (English-33 with a selection and legal targets; Wiegleb-45 at 360×640dp with a selection and a hint) × both palettes × light and dark; Results with two of three stars; a catalogue card; tutorial step 2; Home × both palettes (which must be identical); the Home hero at 1.0 and 2.0 | Roborazzi | CV-AC05, CV-AC06, TG-AC05 |
| The existing `BoardCanvasTest`, `DockLookRecipeTest`, `GameFlowTest`, `ButtonSoundTest`, and the phase-15 suites for 14, 15i, 15o and 21a–21g | existing, run once per palette where they draw the board | CV-AC10, TG-AC05 |

Every screen keeps its one Compose UI test of its main flow (rules/compose.md). Settings' test gains
the row.

## 7. Findings in the standard palette, recorded and not fixed here

§2's audit was taken on the standard palette, the one every player sees by default. Each item
below complies with "never colour alone": a shape channel exists. Each falls below
`docs/ux-spec.md` §11's "≥ 3:1 for glyphs and boundaries". A normal-vision player separates them by
chroma. These findings were not introduced by phase 15 or by the backdrop. They are recorded so that
nobody re-derives them, and CV-13 keeps them out of this change. *Amended 2026-10-24 (CV-13b,
§13): F-CV4 is now fixed in the standard palette too, by the founder's Q-027 answer. The other five
stand.*

| # | Finding | Worst ratio, light / dark | The colour-blind palette's answer |
|---|---|---|---|
| F-CV1 | The legal-target mark (`primary` at 0.38) against its hole and the slab | 1.67 / 1.93 | opaque (CV-08) |
| F-CV2 | The hint rings (`tertiary` at 0.9) against the light slab; the connector | 1.61; 1.32 / fine in dark | deep amber, opaque, dashed (CV-09) |
| F-CV3 | The selection ring merges into the peg's shaded edge | 1.02 / 1.02 | the halo (CV-07) |
| F-CV4 | The unearned-star outline (`outlineVariant`) against the background and the catalogue card. *2026-10-24 (§12.6): the ratio is unchanged, but the star is now hollow, so this thin band is the whole glyph. At 16dp it is close to invisible.* *Fixed 2026-10-24 (CV-13b, §13.3): the standard palette takes `onSurfaceVariant` too, so the star reads ≥ 5.02 / ≥ 5.56* | 1.23 / 1.20 | `onSurfaceVariant` (CV-10) |
| F-CV5 | The earned star's gold fill against the light background (the filled-against-hollow channel in greyscale) | 1.09 / fine in dark | amber fill and an outline (CV-10) |
| F-CV6 | In dark, hint against selection in greyscale (ΔE00 7.9; two bright rings of nearly one size) | — / 1.22 luminance | the dash pattern (CV-09) |

**Routed as a founder question** (drafted in the report, non-blocking): should the standard palette
take some or all of these answers? Doing so would change the board the founder approved: opaque
dots, deeper amber hints in light, outlined stars. **The default keeps today's look for 3.0.0.** The
colour-blind palette is the compliant path for anyone who needs it. The halo and the dashes stay
specific to the colour-blind palette under every option.

*Amended 2026-10-24 (§12.6): this question was never queued. It is re-drafted with the stars as an
option of their own. Until the founder answers, the default stays CV-13 (today's colours), with
CV-13a's hollow form.*

*Answered 2026-10-24 (Q-027, option 2, "stars only"; CV-13b, §13): the standard palette takes the
colour-blind palette's `onSurfaceVariant` for the unearned star, and nothing else. F-CV1 to F-CV3,
F-CV5 and F-CV6 stay as recorded.*

## 8. Considered and rejected

So that none of these comes back as an improvement:

- **Three per-type palettes (Protanopia / Deuteranopia / Tritanopia), the common game menu.** Three
  palettes triple the QA matrix. They also ask the player to know their type, which many do not. A
  hue remap tuned to one type can collide for another. And none of them helps monochromacy. One
  palette that carries every distinction in luminance and pattern serves all four (CV-01).
- **A hue-only remap, such as Okabe–Ito blue and orange for the pegs and marks.** It fails
  monochromacy, and the light-theme marks need the luminance fix anyway. It would also recolour the
  hero object, and a blue candy sphere comes close to the round blue candy the candy-backdrop
  verdict lists among the banned ones (§3b there).
- **Recolouring the pegs.** Peg against hole is already separated by size, form and luminance in
  every vision (§2), and the candy-pink peg is the identity Q-004 kept.
- **Turning the palette on automatically from Android's Color correction** (or reading the
  daltonizer keys in `Settings.Secure`). Those keys are not public SDK API. The system's correction
  already transforms the whole screen, so stacking a palette on it automatically is untested. An
  explicit choice is reliable and reversible. CV-AC14 checks the two together.
- **On by default.** It would change the approved look for every player (Q-004).
- **A live sample of the marks inside the Settings row.** It would put a board drawing in Settings
  for a one-time choice that the board shows one tap away. It is not needed to meet the rule.
- **Dashing the selection ring instead of the hint rings.** Selection is the most frequent mark and
  should be the calmest. Hints are rare and transient, so the pattern goes on them.
- **Keeping the hint gold, only opaque, in the light colour-blind palette.** On the light slab gold
  reaches 1.74:1 at best, and amber-600 `#D97706` reaches 2.45–3.26:1. Only amber-700 holds 3:1 in
  every vision.
- **A separate selection colour (near-black, or a second hue) instead of the halo.** A ring that has
  to contrast with the dark rim and with the pale slab at once must be near-black in light. That is
  a harsh line the clay style avoids. The halo is the established two-colour indicator, and it
  works in both themes.
- **An outline or stroke around the title's glyphs.** It reads as chunky candy-textured lettering,
  which the candy-backdrop verdict bans (§3b there), and the light stop would still sit inside it.
- **Putting the title on a pill or chip.** It would change the hero row the founder approved
  (Q-004, Q-005, D-03l).
- **Darkening the shared CTA gradient.** That would change every primary button for a problem that
  only the title has. The buttons' labels already sit on its deep half.
- **A flat, single-colour title.** It drops the "gradient app title" of DESIGN-SYSTEM.md's Home note.
- **Half-step title gradients** (`#7C3AED → #6D28D9`, `#B6A0FC → #A78BFA`). Both pass, but they
  flatten the sweep to ΔL* ≈ 6, which reads as a single colour (§4.3).

## 9. Assets and licences

| Asset | Kind | Source | Licence |
|---|---|---|---|
| Colour-blind palette tokens (§3.4) and the title gradient tokens (§4.3) | colour values in `ui/theme/Color.kt` | in-house, this repository | project-owned |
| The selection halo, the opaque legal-target mark, the hint rings | drawn in Compose `Canvas` from tokens | in-house | project-owned |
| `Icons.Filled.Star`, `Icons.Outlined.Star` (the star outline overlay) | vector, `material-icons-core`, **already a dependency and already used** for the earned and unearned stars | Google, AndroidX | Apache-2.0 (covered by THIRD_PARTY.md's AndroidX family row) |
| *Amended 2026-10-24 (CV-10a, §12.3).* Material Icons' star artwork: the filled star (`Icons.Filled.Star`) and the hollow star (`star_border`). As built, the hollow star is the opaque layer of `material-icons-core`'s `Icons.TwoTone.Star` | vector, `material-icons-core`, already a dependency. Under CV-13a, `Icons.Outlined.Star` is drawn nowhere | Google, AndroidX | Apache-2.0 (the AndroidX family row). A construction that copies the path into the repository adds a Material Icons row to THIRD_PARTY.md; one that adds `material-icons-extended` adds a dependency. The licence is Apache-2.0 either way |

**No new file, no new dependency, no raster, no font, no sound, no permission.** THIRD_PARTY.md
needs no new row, and the licences screen (`docs/ux/licences-screen.md` §3.2) lists nothing new.

## 10. Open items routed to the producer

1. **Closed 2026-10-18 (§11.1): moot under direction B**, which has no hills or lips. TG-AC04's target
   moved to "the direction's dark backdrop above 0.47 H", and B meets it. The original item follows,
   as the record. **The candy backdrop's dark far-hill frosting lips lie under a wrapped title** (§4.4,
   TG-AC04). `docs/ux/candy-backdrop.md` §5.1 claims "dark: ≥ 4.2:1 everywhere". At 200% on a
   360dp phone, the title's last line reads 1.08–1.59:1 over `BackdropFrosting` dark (L 0.308). No
   title colour can fix it. The proposed rule for that document's owner (this role, on its own
   dispatch, after the tech-lead's reading): in dark Landscape, no colour brighter than L 0.0786
   above about 0.47 H. For example, a dark-band lip on the far mounds, or far crests below that
   line. Until then TG-AC04 fails on the backdrop branch.
2. **Closed 2026-10-24 (§13): queued as Q-027 and answered with option 2, "stars only" (CV-13b).**
   The original item follows, as the record. **Founder question**, drafted in the report as Q-NNN:
   does the standard palette take the colour-blind palette's luminance answers (§7)? Non-blocking;
   the default is "no, for 3.0.0".
3. **Architecture, the tech-lead's:** how the setting reaches `AppTheme` once, at the root,
   meeting §3.3's three properties. This includes a process restored straight into gameplay.
4. **Settings renumbering.** It has to be co-ordinated with `docs/ux/licences-screen.md` §2.
   Whichever lands second renumbers on top of the first (§3.1).
5. **Build order.** The row and the title fix build on phase 15's Settings and Home: the "How to
   play" row and the D-03l hero row. So they are built after `feature/phase-15-ux-refresh` merges,
   like the backdrop. The palette and the title fix do not depend on the backdrop. TG-AC04 does.
6. **Documents to amend when this lands**, this role's, on the branch where they live after the
   phase-15 merge:
   - `docs/ux-spec.md` §4.5 gains the row.
   - §6.2's "Hero title" row changes to `titleGradientBrush()`.
   - §11's "State, not just colour" table gains the board marks and the palette.
   - `docs/ui/DESIGN-SYSTEM.md` gains a `TitleGradient` line under Gradients and a short
     "Colour-blind palette" section.
7. **For the release-manager.** The store listing copy being drafted under Q-016 may name the new
   "Colorblind palette" setting. That is optional, and it is copy, not a claim of compliance.
8. **Scheduling into 3.0.0.** The producer's call. Both fixes are rule gaps
   (rules/compose.md, `docs/ux-spec.md` §11), so they are asked of the release. The specs are
   complete and need no founder answer to be built.

## 11. Amendments for backdrop direction B (2026-10-18)

The founder picked direction B, "Deep gloss" (Q-021, 2026-10-18), and the third edition of
`docs/ux/candy-backdrop.md` specifies it. B brings four things:
- a deep violet-to-pink gradient behind Home and the game screen;
- two glossy waves, and soft lights rising and fading;
- a gloss finish on the clay chips and pills;
- gel fills, with white text, on the two mode tiles.

This section records what B changes here. Nothing else in this document changes: CV-01 to CV-13,
TG-01, TG-02 and every palette value stand. Superseded text above stays as the record, with a dated
pointer to this section.

### 11.1 TG-AC04's target, and §10 item 1

- **The target** is now "the direction's dark backdrop above 0.47 H". No pixel of it may have
  L > 0.0786, at rest or at any pose of its motion.
- **Direction B meets it by construction** (candy-backdrop B-21, BD-08, BD-11). Above 0.47 H in dark
  there is only:
  - the sky: L ≤ 0.011, its brightest point above the line being 0.47 H itself;
  - at most one light at a time, because the lights that can reach above the line run in disjoint
    lanes. A light's brightest point is its rim. Over that sky the rim composites to at most L 0.047
    (Pearl), 0.035 (Pink) or 0.03 (Violet).

  No wave pixel lies above the line at any pose: the back wave's crest, bob included, never rises
  higher than 0.484 H on a 640dp-tall window.
- **What the wrapped dark title reads there:** its deep stop ≥ **3.99:1** and its light stop
  ≥ 5.89:1, against the 3:1 bar.
- **§10 item 1 is closed.** B has no hills and no frosting lips.

### 11.2 §2's audit and the colour-blind palette, beside the gel tiles and the gloss dock

- **The gel mode tiles** keep §2's "nothing missing".
  - White title and supporting text: ≥ 4.90:1 on the pink gel and ≥ 5.72:1 on the violet gel, at
    every font scale. They sit only from 16dp below the tile's top, where the gel is fully deep
    (candy-backdrop §3.5.2, §5.3).
  - The white badge glyph: ≥ 3.73:1. The level chip's text: 8.9:1.
  - **Telling the tiles apart:** by label, badge glyph (grid or bolt), trailing art (footprint or bolt)
    and the level chip. In greyscale the two gels read close (mid L 0.134 and 0.164), and nothing
    relies on that.
  - Pressing darkens the gel, so no state lowers the text's contrast.
- **The Continue chips and the ribbon** keep their fills.
  - In light, the white gloss only raises their dark text's contrast.
  - In dark, the 6% gloss keeps `onSurfaceVariant` at ≥ 4.66:1, and the rest higher.
  - Nothing is missing.
- **The dock** ("the gloss dock"). B draws **no gloss on the dock**: not on its discs, its on mark or
  the slab fill around the discs. Its rim, lip and glow lie at the slab's outer edge, away from every
  disc. So:
  - CV-11 and §2's "dock: nothing missing" hold;
  - criterion 21f's "the On disc and the Off ring each ≥ 3:1 against the dock slab" holds in both
    palettes;
  - CV-AC10's "the dock's 21a–21g pass in both palettes" is unaffected.

  The board drew a gloss on the discs, and candy-backdrop §13 records why it was dropped: the On
  disc's top would read ≈ 2.4:1 against the slab.
- **The backdrop and the finish encode nothing.** Every rule on them is a luminance ratio (CV-12a).
- **The palette's consumers** (§3.3) are unchanged. None is on Home's tiles, on the gloss finish or on
  the backdrop. Home therefore stays pixel-identical with the palette on and off (CV-AC06) under B
  too: the gel tiles and the finish do not read the palette.
- **The palette's marks** live inside the opaque board slab. B draws nothing in it or over it
  (candy-backdrop BD-17), so every §3.5 ratio stands.

### 11.3 §4.3 and §4.4's backdrop numbers, re-derived for B

| Where the title can be | Light theme: light stop `#7C3AED` / deep stop `#5B21B6` | Dark theme: light stop `#C4B5FD` / deep stop `#A78BFA` |
|---|---|---|
| At rest, in the calm band (the sky alone, L ≥ 0.876 in light, ≤ 0.0095 in dark) | ≥ 5.04:1 / ≥ 7.92:1 | ≥ 9.56:1 / ≥ 6.48:1 |
| On B's zone-T sky tokens (light `SkyTop`, `SkyMid`; dark `SkyTop`, `SkyMid`, `SkyLow`, `SkyDeep`) | ≥ 4.69:1 (`SkyMid`, L 0.812) / ≥ 7.37:1 | ≥ 8.48:1 / ≥ 5.75:1 (`SkyDeep`, L 0.017) |
| Wrapped, above 0.47 H: the sky plus at most one light | ≥ 3.11:1 (a Pink light's rim, L 0.522) / ≥ 4.89:1 | ≥ 5.89:1 / ≥ 3.99:1 (a Pearl light's rim, L 0.047) |
| Over the orbs | the orbs retire under B | the orbs retire under B |

- **The light bound of §4.4** (every light backdrop colour the title can overlap has L ≥ 0.503) holds.
  The darkest such colour is a Pink light's rim, at 0.522.
- **TG-AC02's backdrop clauses** read as amended in its row. TG-AC03's "≥ 3:1 at 2.0" now holds with
  no exception.
- **`TitleGradientContrastTest`** ("the title clears 3:1 over every backdrop colour it can reach in
  light") reads the backdrop palette, so it tests B's tokens without a rename.

### 11.4 What this amendment does not change

- The palette (§3) and its persistence, reach, marks and deficiency checks.
- The title fix (§4.3), its tokens and TG-AC01, TG-AC03 and TG-AC05.
- Every CV-AC criterion, and §6's tests apart from the backdrop tokens they read.
- §7's findings.
- §8's rejections. A gel-coloured or glossy title joins them, for the outline's reason: it would read
  as candy-textured lettering.

## 12. Amendments for deviation D-1: the hollow star (2026-10-24)

The build at `32b2600` on `feature/colour-vision` found something this document got wrong (the
developer's D-1). This section rules on it. CV-01 to CV-13a, TG-01, TG-02 and every colour value
stand. Superseded text above stays as the record, with a dated pointer to this section.
*[2026-10-24: the founder's Q-027 answer later changed which colour the standard unearned star
takes, CV-13b (§13). No colour value changes.]*

### 12.1 What was found

- **`Icons.Outlined.Star` is a filled star.** In `material-icons-core`, the Outlined theme draws the
  star solid. Material's hollow star is a separate icon, `star_border`. `material-icons-core` 1.7.8
  (the BOM's version) ships `Star` in its five themes and has no `StarBorder` or `StarOutline`
  (its class list, read on 2026-10-24).
- **So today's stars differ by colour alone.** The standard unearned star is the filled star tinted
  `outlineVariant`. The earned star is the same filled star with a `GoldGradient` fill. They have the
  same silhouette, the same size and the same fill, in two colours. §2's star row named "Shape (filled
  against hollow)" as the non-hue channel, but there was none. The audit's line "the standard palette
  never uses colour alone" holds for every board mark and fails for the stars, and CV-13 rested on it.
- **Nothing else shows the count.** The Results star row and the catalogue pips tell TalkBack their
  count (`EarnedStarCountKey`, the catalogue's description), but no visible text repeats it. A player
  who cannot tell gold from lavender-grey sees three stars. In light, the top of the earned fill
  reads 1.09:1 against the background and the unearned star 1.29:1. In greyscale they are almost the
  same shape at almost the same grey.
- **Where it shows.** Only where earned and unearned stars sit together: Results' star row (48dp) and
  the catalogue's pips (16dp). The tutorial's completion star is always earned. Home's ribbon star is
  one decorative glyph beside a text value. Neither pairs the two.

### 12.2 Ruling 1: colour alone is not acceptable for 3.0.0, so the unearned star is hollow in both palettes (CV-13a)

**Not acceptable.**
- rules/compose.md says "never colour alone (shape, pattern, label)". It says "colour-blind palette
  option in games" as a separate clause, so the option does not discharge the first.
- The standard palette is what every player sees by default. With colour-only stars, a player would
  have to find a setting before the rating became readable. WCAG 2.2 SC 1.4.1 (Use of Color) reads
  the same way.
- The founder kept the palette in 3.0.0 (Q-026) so that the board marks stop depending on colour
  alone. Stars that still did would leave that answer half true.

**The visual requirement, in both palettes:**
- An **earned** star is the filled star, as today. Standard: `GoldGradient`, no outline.
  Colour-blind: §3.4's fill and outline.
- An **unearned** star is the **hollow star** of §12.3: the same silhouette with the interior open,
  so the surface beneath shows through.
- Earned and unearned stars share one silhouette, one size and one place in the row. The fill is
  their only difference of form, and every vision keeps it.

**Nothing else changes in the standard palette.**
- The unearned star keeps `outlineVariant` (CV-13). *[2026-10-24, superseded by CV-13b: it takes
  `onSurfaceVariant`, by the founder's Q-027 answer (§13).]*
- The earned star keeps `GoldGradient` and gets no outline.
- The glow, the pop-in and its stagger, the pop sounds, the sizes and the spacing are unchanged.
- `EarnedStarCountKey`, the catalogue's description and every traversal are unchanged.
- No board mark changes.

**Why this is a UX decision and not a founder question.**
- It fixes a rule; it is not a taste choice between equals. The colour-only star fails a mandatory
  rule, so the two options are not equal.
- It is the smallest change that passes: one glyph form on two screens, with no colour, hue, size or
  motion change. Q-004's "keep the palette and the style" holds.
- Filled against hollow is the rating idiom Android players already read.

**Its cost, stated plainly.**
- A hollow star carries less ink than a filled one of the same colour. In `outlineVariant` (1.23 to
  1.50:1 against what lies beneath, F-CV4), the standard unearned star gets fainter. It is faintest at
  the catalogue's 16dp, where its band is about 1.1dp.
- The earned count stays plain: the filled stars are the ones that are there. The empty slots become
  faint.
- Raising that outline to `onSurfaceVariant` would change a colour in the approved look, so it goes to
  the founder (§12.6). CV-13's default holds until the founder answers. *[2026-10-24: the founder
  raised it (Q-027, option 2). The band now reads 5.31–6.95:1 at normal vision (§13.3).]*

### 12.3 Ruling 2: the hollow star as built is the spec's outline (CV-10a)

The developer built `StarOutline` from the opaque layer of `material-icons-core`'s
`Icons.TwoTone.Star`. That layer is Material's `star_border` artwork: the star's outer outline with
the inner star cut out. **It is accepted** as the hollow star of CV-10, §3.4 and §12.2, in both of its
uses: the unearned star, and the outline the colour-blind palette draws over an earned star.

The visual requirement below holds for any construction. The construction itself is the tech-lead's
ruling.
- **Silhouette.** The filled star's own, on the same 24-unit grid. The outer edge of `star_border`
  agrees with `Icons.Filled.Star` within 0.01 units at every point. So the outline over an earned star
  sits on the fill's edge, and an unearned star fills exactly the place an earned one would.
- **Interior.** Open: no fill and no tone inside. The two-tone star's 30% inner layer is not drawn,
  because it would put a fill back.
- **Band.** The artwork's own weight: about 1/15 of the glyph's edge along its sides, so about 3.2dp
  at Results' 48dp and about 1.1dp at the catalogue's 16dp. It is uniform and scales with the glyph.
  There is no hand-drawn stroke and no per-size weight.
- **Colour.** One opaque colour from the palette. Unearned: `outlineVariant` (standard) or
  `onSurfaceVariant` (colour-blind). Over an earned star: `onTertiaryContainer` (colour-blind only).
  *[2026-10-24, CV-13b: unearned is `onSurfaceVariant` in both palettes (§13).]*
- **Asset.** Material Icons artwork, Apache-2.0, from a dependency the app already has (§9 as
  amended). If a construction copies the path into the repository instead, THIRD_PARTY.md gains a
  Material Icons row for it. If it adds `material-icons-extended`, that is a new dependency for the
  tech-lead to weigh. The licence is Apache-2.0 either way.

### 12.4 Ruling 3: the wording corrections

The spec named an icon constant where it meant a shape, and that constant draws a different shape.
From here on, the spec names the shape:
- **§3.4, earned star, colour-blind.** "An outline: `Icons.Outlined.Star` drawn over it" now reads
  "an outline: **the hollow star** (§12.3) drawn over it", in light and in dark.
- **§3.4, unearned star.** Standard: "`Icons.Outlined.Star` in `outlineVariant`" now reads "**the
  hollow star** in `outlineVariant`" (CV-13a). Colour-blind: "the same glyph" now reads "the hollow
  star". *[2026-10-24, CV-13b: the standard hollow star is drawn in `onSurfaceVariant` (§13).]*
- **§2, stars row.** The description of today ("Unearned: `Icons.Outlined.Star` in `outlineVariant`")
  stays as the record of what was built. Its channel cell is corrected: there was no shape channel
  (§12.1).
- **CV-AC05.** "Stars and the done pip draw as §3.4" now means §3.4 as amended.
- **§9.** The star asset row now reads "Material Icons' star artwork: the filled star
  (`Icons.Filled.Star`) and the hollow star (`star_border`)".
- **`StarBorder`.** The tech-lead's brief (Open issues 3, §4.1) proposed `Icons.Outlined.StarBorder`
  as the corrected name. That icon ships in `material-icons-extended`, not in `material-icons-core`
  1.7.8, so this document cannot bind to it. The spec names the artwork, `star_border`, and leaves
  the construction to the tech-lead.

### 12.5 What the rulings change in the acceptance criteria

- **CV-AC05.** "With it off, every screen is pixel-identical to the branch's base" now has one
  exception: the unearned stars on Results and in the catalogue, which are hollow in both palettes.
  Added: in both palettes and both themes, an unearned star's centre shows the surface beneath it (the
  background, the Results sky or the catalogue card), and an earned star's centre shows its fill.
- **CV-AC08 (the UI half of D-2).** The pair "earned (filled) against unearned (hollow)" must hold in
  **both** palettes' recipes, not only the colour-blind one. The standard recipe's board pairs already
  differ in form, radius or dashes (§2), so the criterion covers both recipes whole. How the test
  proves it is the tech-lead's ruling. The wrong-tap mark's row is the tech-lead brief's Open issues 2
  and is not ruled here.
- **CV-AC11.** The off-palette baseline stays reported and is not graded for luminance. One item is now
  graded: in the standard palette at normal vision, earned stars are filled and unearned stars are
  hollow, on Results and on a catalogue card, in light and dark.
- **CV-AC12.** At the catalogue's 16dp, the hollow band is about 3px wide on the Pixel_9a, so its
  stroke-centre pixel may never reach the full colour. QA samples it anyway. If it misses row 11 by
  more than the 0.3 allowance, Results' 48dp star sets the number and the pip is judged by eye under
  CV-AC11.
- **CV-AC10 is unchanged.** The star is decorative, and no description, key or traversal moves.

### 12.6 §7's F-CV4, and the founder question re-drafted

- **F-CV4 sharpens.** The standard unearned outline still reads 1.23 / 1.20:1. But the thin band is
  now the whole glyph, not the edge of a filled shape, so at 16dp it is close to invisible. The
  colour-blind palette's `onSurfaceVariant` reads ≥ 5.02:1 in every vision (§3.5 row 11).
  *[2026-10-24: fixed in the standard palette by CV-13b (§13.3).]*
- **The founder question was never queued.** It was drafted in
  `.planning/reports/candy-backdrop/ux-designer-colour-vision.md` (2026-10-17). FOUNDER.md on this
  branch has no entry for it, and Q-026 asks a different question: whether the palette ships at all.
  *[2026-10-24: it has since been queued as Q-027 and answered (§13.1).]*
- **It is re-drafted** in `.planning/reports/colour-vision/ux-designer-star-ruling.md`, with the stars
  as an option of their own.
- **Until the founder answers,** the default stays CV-13 (today's colours) with CV-13a's hollow form.
  The procedure's "proceed with the recommendation" covers art-direction choices between equals. This
  question changes a look the founder approved, so CV-13's default holds. Either answer is a change
  of one value. *[Answered 2026-10-24: option 2, CV-13b (§13).]*

### 12.7 What this does not change

- Every colour token, and every value of §3.4 except how the star glyph is named. *[2026-10-24:
  CV-13b later changed which token the standard unearned star reads (§13). No token's value
  changes.]*
- The board, the dock, Home, the backdrop and the title.
- The colour-blind palette's numbers (§3.5). Rows 9 to 11 measure each colour against what lies
  beneath it, and a star's form does not change those ratios.
- §8's rejections.

## 13. Amendment for the founder's Q-027 answer: the standard empty star (2026-10-24)

§12.6 drafted a founder question about the standard palette's marks and stars, and the producer
queued it as Q-027. This section records the answer. CV-01 to CV-13a, TG-01, TG-02, §11 and §12
stand, except where a dated pointer above names CV-13b. Superseded text stays as the record.

### 13.1 The answer

- **Q-027, "Should the standard colours get the stronger marks and stars too?"**, was answered on
  2026-10-24 with **option 2, "stars only"**. The standard palette's empty-star outline takes the
  colour-blind palette's deeper grey, and the board is unchanged (`.planning/questions/FOUNDER.md`
  on `develop`).
- Option 2 was this role's recommendation. Option 1 (no change) and option 3 (the standard palette
  also takes the solid move dots, the darker amber hints in light and the colour-blind stars) were
  not taken. Option 3 stays a candidate for after 3.0.0, as the recommendation said.
- It is recorded as CV-13b. It is the founder's decision, not a UX decision.

### 13.2 What changes

- **One colour, in one place.** The standard palette's unearned star, the hollow star of CV-10a, is
  drawn in `onSurfaceVariant` instead of `outlineVariant`:
  - light `#635F69`, was `#DCD3EC`;
  - dark `#A79FB5`, was `#3A3350`.
- **Where it shows:** Results' star row (48dp) and the catalogue's star pips (16dp). These are the
  only places an unearned star is drawn (§12.1).
- **The unearned star is now the same in both palettes,** in form and in colour. The two palettes'
  stars still differ in the earned star: the colour-blind palette's amber fill in light, and its
  `onTertiaryContainer` outline (§3.4).
- **No new token.** `onSurfaceVariant` is an existing scheme role. §3.4's two new hexes stay the only
  new ones.
- **As built** at `2fd0baf`, `boardMarksFor` reads one shared `onSurfaceVariant` value for both
  palettes' unearned star. That is this ruling.

### 13.3 Contrast, recomputed

The method is §0's: the WCAG 2.x relative-luminance ratio, from the `Color.kt` hexes. Monochromacy is
luminance only, so its ratio equals normal vision's. These figures were computed by hand. The same
arithmetic reproduces the `outlineVariant` figures this document already states (1.29, 1.23, 1.49,
1.20), which checks it.

| The surface beneath the star | Light, `outlineVariant` (was) | Light, `onSurfaceVariant` (now) | Dark, `outlineVariant` (was) | Dark, `onSurfaceVariant` (now) |
|---|---|---|---|---|
| `background` (`#F4F1FA` light, `#1B1626` dark), normal vision and monochromacy | 1.29 | **5.58** | 1.49 | **6.95** |
| `surfaceContainerHigh`, the catalogue card (`#F1EAFB`, `#2E2740`), normal vision and monochromacy | 1.23 | **5.31** | 1.20 | **5.59** |
| The worst of every vision over every surface, the Results sky included (§3.5 row 11) | 1.23 | **≥ 5.02** | 1.20 | **≥ 5.56** |

- **The bar.** `docs/ux-spec.md` §11 asks ≥ 3:1 for glyphs and boundaries. The standard unearned
  star now meets it in both themes, with at least 2.0 to spare. F-CV4 is closed.
- **Where no figure is computed here:** the three dichromat simulations, and the Results sky. This
  document never stated a Results-sky figure for the standard star; §2 measured it against the
  background and the card only. None is needed. The standard star is now the colour-blind palette's
  colour on the same surfaces, so row 11's floors are its figures. CV-AC07's test re-proves row 11
  from `Color.kt` on every Check.
- **At 16dp** the band is still about 1.1dp, about 3px on the Pixel_9a (§12.5). At these ratios it
  reads as a thin dark outline, where `outlineVariant` was close to invisible.

### 13.4 What stays, stated plainly

- **The standard earned star is unchanged:** `GoldGradient`, no outline. F-CV5 stands: in light its
  fill reads 1.09–2.22:1 against the background.
- **So in light, the empty star now has more luminance contrast than the earned one.** At normal
  vision the earned star still reads as the full one: it is a filled star in saturated gold with a
  glow behind it, beside a thin grey band. In greyscale the earned star is the fainter shape. The
  colour-blind palette is the path for that (CV-10), and the founder kept the gold (option 3 was not
  taken).
- **The tutorial's upcoming step pip keeps its `outlineVariant` ring.** It is a step pip, not a star,
  and §2 records that its faint ring is meant to be faint. The done pip and the completion star are
  earned marks, and they are unchanged.
- **Unchanged everywhere else:**
  - the board, every mark of it, so F-CV1 to F-CV3 and F-CV6 stand;
  - Home (its ribbon star is one decorative filled glyph, §3.3), the dock, the backdrop and the title;
  - motion, sound, semantics, sizes and touch targets;
  - every colour-blind value of §3.4 and §3.5.
- **The Settings row keeps its copy.** "Clearer moves, hints and stars" stays true, because the
  colour-blind palette's earned stars still differ from the standard ones.

### 13.5 The acceptance criteria

No criterion changes. Each one that touches the stars already covers the answer:
- **CV-AC05:** its off-palette exception, the unearned stars, now covers their colour as well as their
  form (the pointer in its cell). The clause that an unearned star's centre shows the surface is
  unchanged.
- **CV-AC07:** row 11 is computed for `onSurfaceVariant`, the colour both palettes now draw. The
  standard palette is tied to that colour by the build's own pin: `BoardMarksTest` holds the standard
  unearned colour at `#635F69` and `#A79FB5` (the developer's loop-1 report, RED-2 and GREEN-2). No new
  test is owed.
- **CV-AC08 and CV-AC10** concern form and semantics. A colour change moves neither.
- **CV-AC11:** the graded item (filled against hollow, standard palette, normal vision) is unchanged.
  The off-palette baseline QA already records shows the new grey: Results with two of three stars and
  the catalogue's pips, in light and dark. It stays reported, not graded.
  *(Note 2026-11-01 — SN-1.)* On the device the grey shows on the catalogue's pips only: Results in
  play always has three earned stars (CV-AC11's dated correction). A two-star Results exists only as
  a constructed state on the JVM.
- **CV-AC12** samples the colour-blind palette, whose values have not changed.
