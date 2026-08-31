---
phase: 09-consent-rewarded-ads
plan: 09
subsystem: ui
tags: [compose, material3, snackbar, dialog, rewarded-ads, robolectric, state-hoisting]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads (plan 09-03)
    provides: "AdOfferDialog + AD_OFFER_DIALOG_TEST_TAG, ClaySnackbarHost, the ad_unavailable_* strings"
  - phase: 09-consent-rewarded-ads (plan 09-06)
    provides: "GameUiState.pendingAdOffer / adOfferLoading / adMessage / adMessageTick and the tick carry-forward at the four board-install sites"
  - phase: 09-consent-rewarded-ads (plan 09-08)
    provides: "GameViewModel.onAcceptAdOffer / onDismissAdOffer and the D-11 emitAdMessage tick"
provides:
  - "GameScreen hosts the app's first snackbarHost, wired to ClaySnackbarHost"
  - "AdMessageSnackbarEffect — the D-11 message driven by the adMessage/adMessageTick pair, guarded on tick > 0"
  - "GameScreen raises AdOfferDialog from pendingAdOffer, gated exactly like confirmRestart"
  - "GameScreen.onAcceptAdOffer / onDismissAdOffer, wired in GameRoute as pure method references"
  - "GameScreenAdOfferTest — 8 screen-level specs including the phantom-cue and no-second-offer regressions"
affects: [09-10, gameplay-ui, assist-funnel]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "One-shot UI messages ride the file's Int-tick idiom, resolved with stringResource in composition and consumed in a LaunchedEffect keyed on the ordinal alone"
    - "A negative UI contract is asserted as a test whose KDoc names the contract, so adding the forbidden surface fails a test rather than a review"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt

key-decisions:
  - "The ad message is resolved with stringResource in composition rather than the plan's LocalContext.current.getString(...) — the latter is an error-level lint failure in the pinned Compose BOM (LocalContextGetResourceValueCall) because LocalContext reads survive a Configuration change stale"
  - "The two handlers are required parameters rather than defaulted no-ops, so a future host cannot silently render a dialog whose taps go nowhere; the three existing GameScreen test call sites were updated instead"
  - "The private effect is named AdMessageSnackbarEffect, not AdMessageSnackbar, so the plan's zero-Snackbar( grep has exactly one residual match — the showSnackbar call the plan's own action text mandates"
  - "The LongMethod justification stayed a trailing EOL comment on the annotation (ktlint forbids a line comment between a KDoc and its annotation); the 'amend the list rather than grow under it' rationale moved into the KDoc"

patterns-established:
  - "Screen-level Compose specs construct GameUiState by hand and never build the ViewModel — the funnel's economy is the ViewModel spec's subject, the composition's gating is the screen spec's"
  - "A regression test whose KDoc names both halves of the fix it pins, so removing either half fails a test that explains itself"

requirements-completed: [ADS-01, ADS-03, ADS-04]

# Metrics
duration: 55 min
completed: 2026-08-31
---

# Phase 9 Plan 09: The Opt-In Dialog and Snackbar on GameScreen Summary

**The rewarded-ad opt-in dialog and the D-11 "ad unavailable — refilled for free" snackbar are now reachable from real gameplay state, driven purely from hoisted state, with the rescue banner's undo still routing into the same funnel and carrying no ad affordance at all.**

## Performance

- **Duration:** ~55 min (two full 11-minute `check` runs inside it)
- **Completed:** 2026-08-31
- **Tasks:** 2 (3 commits)
- **Files:** 6 (1 created, 5 modified)

## Accomplishments

- **The app's first `SnackbarHost` is live.** `GameScreen`'s `Scaffold` gained a `snackbarHost` slot wired to plan 09-03's `ClaySnackbarHost`. No snackbar surface existed anywhere in the app before this, so the slot is genuinely new — and the styling stays entirely inside `ClaySnackbarHost`: `GameScreen` constructs no `Snackbar`.
- **The D-11 message rides the file's own tick idiom.** `AdMessageSnackbarEffect` is the fifth instance of the `Int`-tick pattern in this file and sits beside `SelectionSound`/`UndoSound` so it is read against them. It guards on `adMessageTick > 0`, which is the second half of the phantom-cue fix whose first half is plan 09-06's carry-forward — and that guard now has a test that fails if either half is removed.
- **The dialog is gated exactly like the restart confirm.** `state.pendingAdOffer?.let { … }` sits beside `if (state.confirmRestart) { … }` and follows its shape. Because `AdOfferDialog` is hosted in a real `Dialog` window, the board behind it is inert — which is what makes "declining leaves the game exactly as it was" a fact rather than an intention (T-09-39).
- **`GameRoute` wires both callbacks as pure method references**, matching every other handler in that file, so the route added no logic.
- **The negative contract is now machine-checked.** `theRescueBannerReachesTheUndoFunnelAndCarriesNoAdAffordance` asserts both halves of UI-SPEC §4: the banner's Undo still reaches `onUndo`, and none of the five ad strings appears anywhere inside the banner subtree. A future change that adds a second offer surface fails a test whose KDoc explains why it must not exist.
- **The stub is still bound.** The whole opt-in experience is reachable and tested with `NoOpAdGateway` still bound in `PlatformModule` — every ADS-01/03/04 behaviour is proven before any real ad SDK exists, which is exactly the ROADMAP's locked ordering.

## Task Commits

1. **Task 1: the dialog and snackbar on `GameScreen` + `GameRoute` wiring** — `6b07152` (feat)
2. **Task 2: `GameScreenAdOfferTest`** — `6308caf` (test)
3. **Lint fix on Task 1's mandated idiom** — `f030920` (fix)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — `snackbarHostState` + the `ClaySnackbarHost` slot, the `AdMessageSnackbarEffect` tick consumer, the `pendingAdOffer` dialog gate, two new hoisted handlers with `@param` KDoc, and an amended `LongMethod` justification.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt` — `onAcceptAdOffer = viewModel::onAcceptAdOffer` / `onDismissAdOffer = viewModel::onDismissAdOffer`.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt` — 8 Robolectric specs, one per plan `<behavior>` bullet, on the `GameScreenSoundTest`/`NoWinBannerTest` harness.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt`, `GameFlowTest.kt` — the two new handlers wired to the real ViewModel's methods, so both screen specs keep route parity.
- `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — the two new handlers as no-ops (this spec drives the screen from a bare `GameUiState`).

## Decisions Made

- **`stringResource` in composition, not `LocalContext.current.getString(...)` in the effect.** The plan's `<action>` and RESEARCH Pattern 9 both sketch the `LocalContext` form; the pinned Compose BOM raises `LocalContextGetResourceValueCall` as an **error** on it, and `check` failed. `LocalContext` reads are not invalidated by a `Configuration` change, so that form can hand a stale-locale string to the snackbar. Resolving in composition fixes it without weakening anything: the effect still keys on the ordinal alone, so a locale change re-resolves the text without re-showing a message the player has already seen. Recorded as a deviation below.
- **The two handlers are required parameters.** Defaulting them to `{}` would have avoided touching three test call sites, but it would also let a future host render a dialog whose taps go nowhere — the exact failure a hoisted handler exists to make impossible. The three call sites were updated instead; the two that own a real ViewModel wire the real methods.
- **`AdMessageSnackbarEffect`, not `AdMessageSnackbar`.** The plan asks for zero matches of `Snackbar(` in this file while its own action text mandates `snackbarHostState.showSnackbar(…)`. Naming the private effect with an `Effect` suffix leaves exactly one residual match — the mandated call — instead of three.
- **The `LongMethod` justification stayed a trailing comment.** Moving it to a comment block above the annotation is a ktlint violation ("an EOL comment may not be preceded by a KDoc"). The list now names the snackbar host and the ad-offer dialog on the annotation line, and the "amend this list rather than grow under it, and never add a second suppression or a baseline entry" rationale moved into the function KDoc where there is room for it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The plan's mandated `LocalContext.current.getString(...)` fails the project's own lint gate**

- **Found during:** Task 1's `./gradlew check --offline` gate (after the two named test suites had already passed)
- **Issue:** `<action>` specifies "resolving `state.adMessage` (a `@StringRes` id) through `LocalContext.current.getString(...)`", copied from RESEARCH Pattern 9. The pinned Compose BOM's lint raises `LocalContextGetResourceValueCall` as an **error** on exactly that call: `LocalContext` reads are not invalidated when the `Configuration` changes, so a locale switch can leave the effect holding a stale string. `check` failed on it; the plan's own acceptance criterion requires `check` green.
- **Fix:** the id is resolved with `stringResource` in composition (`val message = adMessage?.let { stringResource(it) }`) and the resolved `String` is captured by the effect. Every other property the plan mandates is unchanged — the `> 0` guard, the `SnackbarDuration.Short`, the absent action label, and the `LaunchedEffect(adMessageTick)` key. An inline KDoc paragraph names the lint so the next reader does not "restore" the `LocalContext` form.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt`
- **Verification:** `./gradlew :app:lintDebug --offline` green; `GameScreenAdOfferTest` (including both snackbar specs) still 8/8.
- **Committed in:** `f030920`

**2. [Rule 3 - Blocking] Three existing `GameScreen` call sites did not compile against the new parameters**

- **Found during:** Task 1
- **Issue:** `onAcceptAdOffer`/`onDismissAdOffer` are required parameters (see Decisions), so `GameScreenSoundTest`, `GameFlowTest` and `ButtonSoundTest` stopped compiling. The plan's acceptance criteria assume the first two still pass, which they cannot do unless their call sites are updated.
- **Fix:** the two specs that own a real ViewModel wire `viewModel::onAcceptAdOffer` / `viewModel::onDismissAdOffer`, keeping them in parity with `GameRoute`; `ButtonSoundTest`, which drives the screen from a bare `GameUiState`, passes no-ops. No assertion in any of the three was changed.
- **Files modified:** `GameScreenSoundTest.kt`, `GameFlowTest.kt`, `ButtonSoundTest.kt`
- **Verification:** `GameScreenSoundTest` 10/10, `GameFlowTest` 7/7, `ButtonSoundTest` green inside the full `check`.
- **Committed in:** `6b07152`

**3. [Rule 3 - Blocking] ktlint forbids the comment placement the amended justification wanted**

- **Found during:** Task 1's ktlint gate
- **Issue:** the amended `LongMethod` justification was first written as a three-line `//` block between the KDoc and the annotation, to fit both the surface list and its rationale. ktlint: "an EOL comment may not be preceded by a KDoc" (not auto-correctable).
- **Fix:** the surface list went back to a trailing comment on the `@Suppress("LongMethod")` line (naming the snackbar host and the ad-offer dialog, within the 120-column limit) and the rationale moved into the function KDoc.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt`
- **Verification:** `./gradlew :app:ktlintCheck --offline` green; `grep -c '@Suppress'` is 1, unchanged from before this plan.
- **Committed in:** `6b07152`

**4. [Rule 3 - Environment] Recreated the gitignored `local.properties`**

- **Found during:** the first Gradle invocation
- **Issue:** `local.properties` is gitignored, so this fresh worktree had no `sdk.dir` and every `:app` task would fail with "SDK location not found".
- **Fix:** copied from the main checkout. Environment-only; the file stays untracked and is in no commit.
- **Files modified:** none tracked
- **Commit:** n/a

---

**Total deviations:** 4 auto-fixed (4 blocking). No Rule 4 architectural questions arose.
**Impact on plan:** no scope creep. Deviation 1 is the only behavioural change from the plan's text, and it moves *toward* the plan's own acceptance criterion (`check` green) rather than away from it. Deviations 2 and 3 are mechanical consequences of the plan's own instructions.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew check --offline` (all suites, detekt, ktlint, dp/sp + ad-id literal gates, `PlatformBoundaryTest`, `DomainPurityTest`, `:domain` depth sweep) | **PASS** in 11m 7s |
| `:app:testDebugUnitTest` — **420 tests, 0 failures** (412 before this plan, +8) | PASS |
| `GameScreenAdOfferTest` 8 / `GameScreenSoundTest` 10 / `GameFlowTest` 7 / `NoWinBannerTest` 1 | PASS |
| `GameScreen.kt` contains `snackbarHost = { ClaySnackbarHost(` and `AdOfferDialog(` | 1 each |
| The snackbar effect guards on `adMessageTick > 0` | present (line 465) |
| `grep -cE "(^\|[^A-Za-z])Snackbar\(" GameScreen.kt` — no M3 `Snackbar` is constructed here | **0** (see Deviation note on the literal `Snackbar(` grep) |
| `GameRoute.kt` contains `viewModel::onAcceptAdOffer` / `viewModel::onDismissAdOffer` | both present |
| `grep -c '@Suppress' GameScreen.kt` | **1** — unchanged from before this plan |
| The `LongMethod` justification names the snackbar host and the ad-offer dialog | yes |
| `NoWinBanner.kt` / `HintControl.kt` / `UndoControl.kt` / `HintToggleControl.kt` changed | **none of the four** — not in this plan's diff |
| New `[0-9]+\.(dp\|sp)` literal in `GameScreen.kt` | none |
| detekt baseline changed | no baseline file exists in the project; `git diff` on `config/detekt/` is empty |
| `grep -c "GameViewModel" GameScreenAdOfferTest.kt` | **0** |
| `GameScreenAdOfferTest` `@Test` count | **8** — one per `<behavior>` bullet |
| Phase gate: the stub is still bound | `bindAdGateway(impl: NoOpAdGateway): AdGateway` unchanged; `PlatformModule.kt` is not in this plan's diff |

**Note on two literal criteria.** `grep -c "Snackbar(" GameScreen.kt` cannot be 0 while the plan's own `<action>` mandates `snackbarHostState.showSnackbar(message)` — `showSnackbar(` contains the substring. The criterion's intent (the styling lives only in `ClaySnackbarHost`; no M3 `Snackbar` is constructed here) is verified above with a word-boundary grep, and `GameScreen.kt` imports no `Snackbar` type. Likewise the phase gate's `grep -c "NoOpAdGateway" PlatformModule.kt` returns **3**, not 1 — the import, a KDoc mention and the binding — a pre-existing file shape this plan did not touch; the gate's intent (the stub is still the bound implementation, and 09-10 is the first plan permitted to change it) holds exactly.

## Threat Model Coverage

| Threat ID | Disposition | How it landed |
|-----------|-------------|---------------|
| T-09-39 | mitigated | `AdOfferDialog` is hosted in a real `Dialog` window (plan 09-03), so the board is inert behind the scrim while an offer is pending. The screen adds nothing that could bypass it — the dialog is raised outside the `Scaffold` content, from the same position as `RestartConfirmDialog`. |
| T-09-40 | mitigated | The `> 0` guard, pinned by `anAdMessageAtTickZeroIsNeverShown`, whose KDoc names both halves of the fix (this guard and 09-06's carry-forward) so removing either fails a test that explains itself. |
| T-09-41 | mitigated | `theRescueBannerReachesTheUndoFunnelAndCarriesNoAdAffordance` asserts the absence of all five ad strings *within the banner subtree* (`hasAnyAncestor(hasTestTag(NOWIN_BANNER_TEST_TAG))`), and that no dialog is on screen, while positively asserting the banner still reaches `onUndo`. |
| T-09-42 | mitigated | `GameScreen` stays stateless over hoisted state and names no `Activity` and no SDK type; `PlatformBoundaryTest` ran green as part of `check`. |
| T-09-SC | accepted | No package installed, no dependency added. |

## Known Stubs

None. Both surfaces are fully wired to real hoisted state and real ViewModel callbacks. `NoOpAdGateway` is still the bound `AdGateway`, but that is the ROADMAP's locked ordering rather than a stub introduced here — plan 09-10 swaps it.

## Threat Flags

None. This plan adds no network endpoint, auth path, file access or schema change; it renders two existing composables from two existing state fields.

## Issues Encountered

None beyond the four deviations above. The two 11-minute `check` runs dominate the wall-clock duration.

## User Setup Required

None — no external service configuration required by this plan.

## Next Phase Readiness

- **09-10 (the real `AdMobAdGateway`)** is unblocked and is the first plan permitted to change the `NoOpAdGateway` binding in `PlatformModule`. Plan 09-08's hand-off note still stands and is worth restating: `withTimeoutOrNull(AdConstants.ACCEPT_WAIT_MS)` must wrap the ad **load** inside `requestRewarded`, because that is the only bound on this screen's `adOfferLoading` lock — the dialog itself has no timeout and the ViewModel deliberately applies none.
- Every ADS-01/02/03/04/05 behaviour is now reachable, visible and tested against the stub, which is exactly the state the real SDK is meant to be swapped into.
- No blockers.

## Self-Check: PASSED

- `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt` — FOUND
- Commits `6b07152`, `6308caf`, `f030920` — all FOUND in the branch history
- Working tree clean apart from the untracked, gitignored `local.properties`

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-31*
