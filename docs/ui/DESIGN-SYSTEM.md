# Peg Solitaire — "Candy Clay" Design System (v2 UI)

Source: ui-ux-pro-max design-system run, 2026-08-26. Style: **Claymorphism (mobile)** — the
recommended style for casual puzzle games. Replaces the stock-AOSP baseline M3 theme.

## Identity

Catchy, addictive, colorful casual-puzzle feel. Soft-3D "candy clay": chunky rounded surfaces,
layered soft shadows, gradient CTAs, squishy spring interactions, celebratory payoffs.
Anti-patterns (explicit): muted colors, low energy, pure-white/gray backgrounds, snap transitions.

## Color (Material 3 ColorScheme mapping)

All hex lives ONLY in `ui/theme/Color.kt`; components consume `MaterialTheme.colorScheme` roles.

### Light
| Role | Hex | Use |
|------|-----|-----|
| primary | #7C3AED (vivid violet) | CTAs, selected peg ring, active states |
| onPrimary | #FFFFFF | |
| primaryContainer | #EDE4FF | tile fills, chips |
| onPrimaryContainer | #3B1D77 | |
| secondary | #EC4899 (hot pink) | pegs, playful accents |
| onSecondary | #FFFFFF | |
| secondaryContainer | #FFE1EF | Challenge tile fill |
| onSecondaryContainer | #831843 | |
| tertiary | #F59E0B (reward gold) | hints, stars, "New best!" — KEEP reserved for hint/reward semantics |
| onTertiary | #241A00 | |
| tertiaryContainer | #FFEFC7 | star pips bg, New-best pill |
| onTertiaryContainer | #6B4E00 | |
| background | #F4F1FA (lavender-white — never pure white) | |
| surface | #FBF9FF | |
| surfaceVariant / surfaceContainerHigh | #F1EAFB | board panel, cards |
| onSurface | #332F3A | |
| onSurfaceVariant | #635F69 | |
| outline | #C9BEE0 | clay borders |
| error | #DC2626 | |

### Dark
Desaturated tonal variants (not inverted): background #1B1626, surface #241E31,
surfaceContainerHigh #2E2740, primary #A78BFA, secondary #F472B6, tertiary #FBBF24,
onSurface #EDE9F5, onSurfaceVariant #A79FB5, outline #4A4160. Contrast ≥4.5:1 body text both modes.

### Gradients (Brush constants in theme, not per-screen hex)
- `CtaGradient`: #A78BFA → #7C3AED (vertical) — primary buttons/tiles
- `PegGradient`: radial #F9A8D4 (top-left highlight) → #EC4899 → #BE185D (rim) — candy pegs
- `GoldGradient`: #FDE68A → #F59E0B — stars, celebration
- Dark variants defined alongside.

## Typography

Fredoka (headings/display, weight 500–600) + Nunito (body/labels, 400–700) via
`androidx.compose.ui:ui-text-google-fonts` (downloadable fonts, BOM-versioned) with
platform-default fallback chain so offline/first-launch never breaks. Map onto the M3 type
scale — displaySmall/headlineMedium/titleLarge = Fredoka; body/label = Nunito. Tabular figures
for timer and score (`FontFeatureSettings "tnum"`).

## Shape

M3 Shapes override: extraSmall 12dp, small 16dp, medium 20dp, large 28dp, extraLarge 32dp.
Tiles/cards use large; buttons medium; dialogs extraLarge. All radii via dimens.

## Elevation / clay depth

Soft double shadow feel: `shadow(elevation)` with `spotColor`/`ambientColor` tinted to the
surface hue (violet-tinted, alpha ~0.25) — never harsh black. Cards 6dp, pressed 2dp, dialogs 12dp.

## Motion (juice rules)

- **Squish press**: every tappable card/tile/button scales to 0.94 on press, releases with
  `spring(dampingRatio = MediumBouncy, stiffness = Medium)`. One shared `Modifier.claySquish()`.
- Micro-interactions 150–300ms; exits ~65% of enter duration; never block input.
- **Staggered entrance**: catalog cards + home tiles fade+slide-up in with 40ms/item stagger.
- **Board juice**: selected peg gets a gentle breathing pulse (scale 1.0→1.06, ~900ms,
  reverse repeat); legal-move dots pop in with spring overshoot; jump slide keeps current
  timing but adds a small landing squash; removed peg fade+shrink stays.
- **Results celebration**: stars pop in sequentially (spring overshoot, 120ms stagger, gold
  gradient + glow), score counts up ~600ms, "New best!" pill bounces in. Solved headline scales in.
- Respect OS animator duration scale (Settings.Global.ANIMATOR_DURATION_SCALE == 0 → skip
  decorative animations; functional transitions still occur instantly).

## Per-screen notes

- **Home**: gradient app title in Fredoka; two big clay mode tiles (Classic = violet container
  + grid glyph, Challenge = pink container + lightning/infinity glyph) with level chip; Continue
  tiles as smaller gold-accented clay chips; slow drifting soft blобs (2, ±16dp, ~8s) in the
  background at low alpha — skip when animations off.
- **Catalog**: cards keep uniform height; board preview sits on a soft violet-tinted panel;
  earned stars use GoldGradient pips; card squish + stagger.
- **Gameplay**: board panel = surfaceContainerHigh clay slab (large radius, soft shadow);
  pegs = PegGradient candy spheres with a top-left specular dot; holes = soft inset dimple
  (two-tone circle); hint overlay stays tertiary/gold; top bar icons get squish + 44dp targets.
- **Results**: celebration hierarchy — stars > headline > score card > actions. Primary action
  uses CtaGradient; secondary actions are quiet clay chips.
- **Settings**: clay list cards, playful switch (thumb squish), same palette.

## Hard constraints (unchanged project gates)

- All dp/sp in `dimens.xml` (dpSpLiteralCheck); all durations/ratios in scoped constants
  (GameAnim etc.); zero hex outside `Color.kt`; detekt + ktlint green; state hoisting intact —
  this is a render-layer change only, NO ViewModel/domain/logic changes; all 293 tests stay green.
- Touch targets ≥48dp; text contrast ≥4.5:1 both themes; content descriptions preserved.
- No emoji icons — vector `ImageVector`s only (local, core icon set; extended set stays banned).
