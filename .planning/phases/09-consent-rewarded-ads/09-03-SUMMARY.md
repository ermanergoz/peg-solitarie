---
phase: 09-consent-rewarded-ads
plan: 03
subsystem: ui
tags: [compose, material3, dialog, snackbar, admob, rewarded-ads, robolectric, strings, dimens]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads
    provides: "AssistAllowanceLimits.FREE_HINTS / FREE_UNDOS (plan 09-01) — the single source of the two allowance numbers the dialog copy discloses"
  - phase: 06-platform-seam
    provides: "AdReason, AdConstants.ACCEPT_WAIT_MS — the ad seam the dialog's reason and bounded wait are expressed in"
  - phase: 07-sound
    provides: "withClickSound / SoundCue.ButtonForward / SoundCue.ButtonDismiss — the per-callsite button cue idiom"
provides:
  - "AdOfferDialog — the stateless themed rewarded-ad opt-in dialog (ADS-01) with AD_OFFER_DIALOG_TEST_TAG"
  - "ClaySnackbarHost — the app's first SnackbarHost, clay-styled, shared by GameScreen and SettingsScreen"
  - "Every Phase-9 string (16 keys) in strings.xml — dialog copy, ad-unavailable snackbar copy, Settings Privacy copy"
  - "Three new dimen tokens: ad_offer_glyph_size, ad_offer_progress_size, settings_row_icon_size"
affects: [09-06, 09-07, 09-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "androidx.compose.ui.window.Dialog with usePlatformDefaultWidth = false for clay dialogs that own their width"
    - "Loading state as a dialog-level lock (disabled actions + non-dismissible properties), not merely a spinner"
    - "Resource-file ownership concentrated in one plan per phase so sibling UI plans can run in parallel"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt
    - app/src/main/java/com/app/pegsolitaire/ui/components/ClaySnackbarHost.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/AdOfferDialogTest.kt
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values/dimens.xml

key-decisions:
  - "The accept/decline peer hierarchy is documented in code as an AdMob rewarded-inventory policy constraint, not a styling preference — an auditor restyling the decline down is committing an account-level risk, so the reason is in the KDoc rather than in a plan document nobody reads at edit time"
  - "The D-10 loading lock is expressed as two whole DialogProperties branches rather than `dismissOnBackPress = !loading`, so the locked state is a named, greppable branch carrying its own T-09-11 rationale"
  - "The accept Button overrides disabledContainerColor/disabledContentColor to transparent/onPrimary — M3's default disabled slab would paint over the CTA gradient the modifier draws behind it, making the loading CTA stop reading as the CTA"
  - "Per-reason copy selection lives in three private AdReason extension properties (titleRes/bodyRes/freeAllowance) rather than inline whens, so the reason→copy→allowance triple is stated once and the disclosed number is read straight from AssistAllowanceLimits"

patterns-established:
  - "Dimen justification comment: every new token names the same-valued token it deliberately does not reuse, so a future reader cannot 'tidy up' a duplicate value that is actually two independent owners"
  - "Allowance counts reach copy only as %1$d format placeholders fed from AssistAllowanceLimits at the callsite — no literal count is ever typed into strings.xml"
  - "A shared stateless leaf whose absent test is a decision states that decision in its own KDoc, so the gap reads as intent rather than omission"

requirements-completed: [ADS-01, ADS-03]

# Metrics
duration: 45min
completed: 2026-08-31
---

# Phase 9 Plan 03: Phase-9 UI Leaves and Resource Tokens Summary

**The themed Candy Clay rewarded-ad opt-in dialog with a locked bounded loading state and AdMob-policy-compliant peer actions, the app's first clay SnackbarHost, and every Phase-9 string and dimen token.**

## Performance

- **Duration:** ~45 min (including an 11-minute full `check` run)
- **Tasks:** 3
- **Files modified:** 5 (3 created, 2 modified)

## Accomplishments

- `AdOfferDialog` — a stateless leaf hosted in `androidx.compose.ui.window.Dialog` (D-06 rules out the stock M3 alert component). It names the reward and its count before any ad can play, presents accept and decline as stacked full-width visual peers, and locks itself completely while a request is in flight.
- `ClaySnackbarHost` — the app's first `SnackbarHost`, extracted once in `ui/components` so `GameScreen` (09-08) and `SettingsScreen` (09-07) cannot drift apart on the styling.
- All sixteen Phase-9 strings and exactly three new dimen tokens landed in one plan, which is what lets 09-06, 09-07 and 09-08 run in parallel without any of them touching a resource file.
- Eight Robolectric tests, one per plan `<behavior>` bullet, all green — including the decline-by-its-own-text click that is the machine-checkable form of the T-09-09 policy mitigation.

## Task Commits

Each task was committed atomically:

1. **Task 1: All Phase-9 strings and dimension tokens** — `61a775c` (feat)
2. **Task 2: AdOfferDialog — the themed opt-in dialog** — `70bdc7d` (feat)
3. **Task 3: ClaySnackbarHost — the app's first snackbar surface** — `5a2b98d` (feat)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt` — the stateless opt-in dialog plus `AD_OFFER_DIALOG_TEST_TAG`; three private `AdReason` extension properties map the reason to its title, body and disclosed allowance.
- `app/src/main/java/com/app/pegsolitaire/ui/components/ClaySnackbarHost.kt` — clay-styled `SnackbarHost` (`shapes.medium`, `surfaceContainerHighest`, `onSurface`, `spacing_md` padding, `clayShadow` at `elevation_card`), no action label.
- `app/src/test/java/com/app/pegsolitaire/ui/game/AdOfferDialogTest.kt` — 8 Robolectric + `createComposeRule` tests on the `NoWinBannerTest` harness; expected body text is formatted from `AssistAllowanceLimits`, never hand-typed.
- `app/src/main/res/values/strings.xml` — 16 Phase-9 strings in three requirement-named comment groups (ADS-01 dialog, ADS-03 snackbar, CONS-03/REL-05 Settings Privacy).
- `app/src/main/res/values/dimens.xml` — `ad_offer_glyph_size` (48dp), `ad_offer_progress_size` (24dp), `settings_row_icon_size` (24dp), each with its non-reuse justification.

## Decisions Made

- **The policy rationale lives in the KDoc.** `AdOfferDialog`'s KDoc states that the peer accept/decline hierarchy is an AdMob rewarded-inventory policy constraint whose violation is an account-level risk. The threat register (T-09-09) is not visible to someone editing the file six months from now; the KDoc is.
- **The loading lock is two explicit `DialogProperties` branches.** `dismissOnBackPress = !loading` would have been DRYer, but the locked state carries a specific correctness rationale (a dismissal racing a returning `Granted`, T-09-11), and a named branch is where that rationale can be stated. This also satisfies the plan's literal `dismissOnBackPress = false` acceptance grep.
- **The disabled CTA stays transparent.** `ButtonDefaults.buttonColors` was given explicit `disabledContainerColor = Color.Transparent` / `disabledContentColor = onPrimary`. This is not in the plan text but is required for it to be true: the gradient is painted by `Modifier.background(...)` *outside* the button's internal `Surface`, so M3's default disabled container would drop an opaque slab over it and the loading CTA would stop reading as the CTA. Recorded as a deviation below.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Disabled accept button would have painted over its own CTA gradient**

- **Found during:** Task 2 (AdOfferDialog)
- **Issue:** The plan specifies `ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = onPrimary)` and, separately, `enabled = false` while loading. Those two interact: M3 resolves a *disabled* button to `disabledContainerColor`, which defaults to `onSurface.copy(alpha = 0.12f)` and is unaffected by the transparent `containerColor` override. Since the CTA gradient is drawn by `Modifier.background(...)` outside the button's internal `Surface`, the disabled slab would render on top of it — the loading CTA would visibly stop being the CTA at exactly the moment the player is waiting on it.
- **Fix:** Added `disabledContainerColor = Color.Transparent` and `disabledContentColor = MaterialTheme.colorScheme.onPrimary` to the `buttonColors` call, with an inline comment naming the interaction.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt`
- **Verification:** `./gradlew check --offline` green; the loading-state tests assert the accept node is present, disabled and announcing `ad_offer_loading`.
- **Committed in:** `70bdc7d` (Task 2 commit)

**2. [Rule 3 - Blocking] Task 3's action text and its own acceptance criterion contradicted each other on `inverseSurface`**

- **Found during:** Task 3 (ClaySnackbarHost)
- **Issue:** The plan's `<action>` requires the KDoc to state "the stock M3 snackbar defaults to `inverseSurface`", while its `<acceptance_criteria>` requires that the file "does not contain `inverseSurface`". Both cannot hold literally. The criterion's evident intent is "do not *use* the inverse-surface role as a color"; the action's intent is "explain why the restyle is mandatory".
- **Fix:** The KDoc names the default as "the M3 inverse-surface role" rather than the code token. The full rationale — dark slab, foreign to the candy palette, on the design system's anti-pattern list — is preserved verbatim, and the greppable token now appears nowhere in the file.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/components/ClaySnackbarHost.kt`
- **Verification:** `grep -c "inverseSurface"` returns 0; `grep -c "surfaceContainerHighest"` and `grep -c "clayShadow"` are both non-zero; KDoc names both `GameScreen` and `SettingsScreen`.
- **Committed in:** `5a2b98d` (Task 3 commit)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking plan-internal contradiction)
**Impact on plan:** Both are corrections *toward* the plan's stated intent, not scope changes. No new file, dependency or public API beyond the plan's `<interfaces>`.

## Issues Encountered

None. All three tasks' verification commands passed first time apart from the two deviations above.

## Verification Results

- `./gradlew :app:assembleDebug --offline` — green (resource compilation, apostrophe escaping)
- `./gradlew :app:lintDebug --offline` — green, no new `StringFormatMatches` / `MissingTranslation` / `UnusedResources` error
- `./gradlew :app:testDebugUnitTest --offline --tests '*AdOfferDialogTest*'` — 8 tests, 0 failures, 0 skipped
- `./gradlew check --offline` — **BUILD SUCCESSFUL in 11m 10s** (covers `dpSpLiteralCheck`, `detekt`, `ktlintCheck`, `lintDebug`, `PlatformBoundaryTest`, `DomainPurityTest`)
- No `@Suppress` added and no detekt baseline entry created
- UI-SPEC negative contract holds: `git diff --name-only <base> HEAD` lists five files, none of them `HintControl.kt`, `UndoControl.kt`, `HintToggleControl.kt` or `NoWinBanner.kt`
- The two allowance numbers still appear exactly once in the codebase, in `AssistAllowanceLimits`

## Threat Model Coverage

| Threat ID | Disposition | How it landed |
|-----------|-------------|---------------|
| T-09-09 | mitigated | Accept and decline are stacked full-width peers; decline is a real `TextButton`. Asserted by `declineInvokesOnDismissOnceAndNeverOnAccept`, which locates the node by its string-resource text. The policy reason is in the KDoc, not just the plan. |
| T-09-10 | mitigated | Body names count, scope ("for this level") and reward, formatted from `AssistAllowanceLimits`. Asserted for both reasons against `app.getString(..., FREE_HINTS/FREE_UNDOS)`. |
| T-09-11 | mitigated | Loading disables both actions and sets `dismissOnBackPress`/`dismissOnClickOutside` false in a dedicated branch. Asserted by `loadingDisablesBothActionsAndDropsTheAcceptLabel`. |
| T-09-12 | mitigated | Neither new composable names `android.app.Activity` or any SDK type; `check` green includes `PlatformBoundaryTest`. |
| T-09-SC | accepted | No package installed, no dependency added. |

## Known Stubs

None. Both composables are complete stateless leaves; they are simply not yet *raised* by any screen, which is by design — plans 09-07 and 09-08 wire them.

## User Setup Required

None — no external service configuration required by this plan.

## Next Phase Readiness

Ready for the wave-3 consumers:

- **09-07 (SettingsScreen)** can render `ClaySnackbarHost` and read `settings_section_general`, `settings_section_privacy`, `settings_privacy_policy_*`, `settings_privacy_choices_*` and `settings_row_icon_size` without touching a resource file.
- **09-08 (GameScreen funnel)** can raise `AdOfferDialog(reason, loading, onAccept, onDismiss)`, locate it by `AD_OFFER_DIALOG_TEST_TAG`, and show `ad_unavailable_hints` / `ad_unavailable_undos` through `ClaySnackbarHost`.
- **09-06** likewise needs no resource-file edit.

No blockers. One note for the wave-3 planner: `AdOfferDialog` takes `loading` as a plain `Boolean` and has no timeout of its own — the `AdConstants.ACCEPT_WAIT_MS` bound that makes the lock safe must be enforced by the caller (`GameViewModel`), and the dialog's KDoc says so. If 09-08 hoists `loading` without that bound, the lock becomes the dead end T-09-11 exists to prevent.

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-31*
