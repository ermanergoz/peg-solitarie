# Jetpack Compose rules

Any task that creates or edits Compose UI MUST invoke `compose-skill` first.

- State hoisting: composables take state + callbacks; only screen-level
  composables collect a ViewModel (`collectAsStateWithLifecycle()`).
- Stability: UI models are immutable data classes or `@Immutable`;
  collections via kotlinx.collections.immutable.
- Recomposition: no allocation in hot composables; `remember`,
  `derivedStateOf`, keys in lazy lists; remembered lambdas.
- Modifier order: layout → size → padding → background → clickable → semantics.
- Every public composable has light and dark `@Preview`s.
- Accessibility: contentDescription or semantics role on every interactive
  element; touch targets ≥ 48dp with ≥ 8dp spacing; text scales with system
  font size; contrast ≥ 4.5:1; never colour alone (shape, pattern, label);
  colour-blind palette option in games.
- One-handed use: primary actions in the lower thumb zone of a 6.7-inch phone.
- Animation respects reduced motion; performance audited with
  `compose-performance-audit` before release.
- Material 3 theme from the core-ui/game-ui module; no hard-coded colours.
- Tests: a Compose UI test per screen's main flow.
