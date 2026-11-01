---
phase: 13-cloud-save
plan: 10
subsystem: ui
tags: [kotlin, compose, material3, home, cloud-save, profile-swap, robolectric, turbine, truth]

# Dependency graph
requires:
  - phase: 13-cloud-save
    plan: "08"
    provides: "ProgressSubmitter.profileSwap: StateFlow<ProfileSwapRequest?> and onProfileSwapResolved(keepLocalProgress) — the pending question and its one answer channel"
  - phase: 13-cloud-save
    plan: "07"
    provides: "the delimited Phase-13 cloud-save block in strings.xml the five new strings sit inside"
  - phase: 11-play-games-session
    provides: "PlayGamesPromptDialog (the clay dialog shell, the claySquish/ctaGradientBrush/withClickSound idiom), HomeUiState's sequenced first-run flags, and the combine + distinctUntilChanged collector idiom this plan extends to three surfaces"
provides:
  - "ProfileSwapDialog + PROFILE_SWAP_DIALOG_TEST_TAG — the stateless clay dialog with the gradient CTA on the SAFE answer"
  - "HomeUiState.profileSwapVisible + profileSwapAccountName — two peers of the two first-run flags, never one nullable field"
  - "HomeViewModel.onKeepDeviceProgress / onUseCloudProgress — both answers, handed to the application scope without viewModelScope"
  - "FakeProgressSubmitter — the shared test double for the swap question, beside the class it fakes"
affects: [13-11, home, cloud-save, profile-swap]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Inverted CTA emphasis: the gradient goes on the non-destructive answer when the alternative is irreversible — the inverse of every other dialog in the app, commented as such in source"
    - "A screen-level guard that can only ever be NARROWER than the ViewModel's invariant, so the two expressions can disagree in exactly one (safe) direction"
    - "Two peer state fields rather than one nullable, when the nullable payload is itself legitimately null"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/home/ProfileSwapDialog.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/ProfileSwapDialogTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelCloudTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/FakeProgressSubmitter.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt

key-decisions:
  - "The gradient CTA goes on 'Keep this device's progress' and the destructive replace is the TextButton — the inverse of PlayGamesPromptDialog, because D-13 makes non-destruction the default and a gradient on a wipe makes the irreversible option the one a thumb finds first."
  - "profile_swap_use_cloud is account-NEUTRAL ('Use their cloud progress'), not '%1$s's cloud progress'. The plan's own objective and truths use the neutral wording, and naming the account in the label would need a sixth string for the null-name case against an acceptance criterion of exactly five. The account is named in the body, which has both variants."
  - "HomeScreen's hosting conditional is NARROWER than a plain `if (state.profileSwapVisible)`. The plan asked for a plain third sibling `if` AND for screen-level never-stacked assertions; a plain `if` renders both dialogs when both flags are true, so the two requirements were mutually exclusive. The narrower guard resolves it in the only safe direction — it can withhold the wipe question, never raise one — and onboardingVisible genuinely has a second writer (the How-to-play affordance) the ViewModel's sequencing does not cover."
  - "FakeProgressSubmitter lives in `data/` beside the class it fakes rather than file-private in the new test, because HomeViewModelTest needs its quiet form too — the FakePlayGamesOptInRepository precedent. Its eight collaborators are relaxed mocks: every member that could reach one is overridden, and the base constructor touches none of them."
  - "Both answer handlers leave profileSwapAccountName standing and clear only the visibility flag. The pair is meaningful only together and the screen reads the name only while visible; blanking it would change the copy under a dialog that is on its way out."

patterns-established:
  - "A dialog whose dismissal is expressed as `onDismissRequest = <the safe lambda>` with no separate onDismiss parameter, so the safe default has exactly one home and no place to diverge"
  - "A screen-level guard documented as deliberately redundant-but-narrower, with the direction of possible disagreement stated in the comment"

requirements-completed: []

# Metrics
duration: 70min
completed: 2026-09-16
---

# Phase 13 Plan 10: The Account-Switch Dialog Summary

**A player signing in with a different Play Games account is asked once, on Home, with the safe answer carrying the gradient CTA, the destructive one naming all four things it deletes, and every dismissal routed to the safe answer by a single assignment.**

## Performance

- **Duration:** ~70 min
- **Tasks:** 3 of 3
- **Files modified:** 12 (4 created, 8 modified)
- **Tests:** 826 app unit tests green across 72 classes, 0 failures, 0 errors — 26 of them new here (8 dialog, 11 ViewModel, 7 screen), with all 28 pre-existing `HomeViewModelTest` and all 9 pre-existing `HomeScreenTest` assertions unchanged and still green

## Accomplishments

- **Dismissal is the safe answer, and it is one assignment (D-13, T-13-42).** `onDismissRequest = onKeepLocal` — no separate `onDismiss` parameter, so there is nowhere for a second, divergent default to live. Two tests drive it through the dialog's *own* `ComponentDialog.onBackPressedDispatcher` (the path a real back press takes, not the host Activity's), one on the named-account body and one on the unknown-name fallback, because the degraded branch is the one most likely to be rebuilt by hand later.
- **The gradient is on the safe answer — the deliberate inverse of every other dialog in the app (T-13-43).** `ctaGradientBrush()` + `SoundCue.ButtonForward` on "Keep this device's progress"; a plain `TextButton` + `SoundCue.ButtonDismiss` on "Use their cloud progress". Verified structurally: the single `ctaGradientBrush()` usage (line 200) sits inside the `Button(` opened at line 187, whose label is `profile_swap_keep` (line 204); the `TextButton` at 208 carries `profile_swap_use_cloud` and no gradient. The inversion and its reason are commented in source at both the file and the action-pair level.
- **The copy names everything the wipe destroys (D-14, T-13-44).** `profile_swap_body` and `profile_swap_body_unknown_account` both enumerate all four categories — level results, play history, Challenge level, both in-progress games — and a test asserts all four against **both** resources, so a future rewrite that quietly drops the in-progress games fails rather than shipping.
- **An unknown display name is a degraded name, never "null" on screen.** `SignedIn.displayName` is legitimately null (Phase 11 D-14), so the body branches to a neutral fallback and the test asserts both that the fallback is what renders and that the `%1$s`-formatted body with the literal `"null"` does not exist anywhere.
- **Visibility is a pure function of three sequenced inputs (D-15).** One collector over `profileSwap` + the two persisted first-run flags, yielding the request only when both are settled. A pending swap is silent under the onboarding overlay and under the opt-in prompt, and surfaces the moment the surface ahead of it is answered — asserted as a live transition, not just as two static states.
- **The WR-07 flicker is closed on the third surface too.** Both boolean inputs are deduped per-input, because they map over the same `app_prefs` DataStore and `combine` delivers the re-emissions in no fixed order. A test stages exactly that: an unchanged `seen = true` re-emitted after an answer must not put the wipe question back on screen.
- **Neither answer runs on the screen's scope (T-13-46).** `awk`-scoped over each handler body, `grep -c viewModelScope` returns **0** for both. Both clear the flag synchronously and then call the non-`suspend`, application-scoped `onProfileSwapResolved` — asserted with **no** `runCurrent()`, so "cleared synchronously and already handed off" is what the test actually pins.
- **Home is the dialog's only host anywhere in the app (D-15).** `grep -rln "ProfileSwapDialog" app/src/main/java/com/app/pegsolitaire/ui/` returns three files, all under `ui/home/` — nothing in gameplay, Results or Settings can reach it.
- **The never-stacked rule is asserted at the screen as well as the ViewModel.** Both impossible states are constructed and both assert the *other* surface is genuinely present (the onboarding overlay by its own "Got it" label, the opt-in prompt by its test tag) before asserting the profile-swap tag is absent — so neither case can pass vacuously.
- **No new dimension and no hardcoded copy.** `git diff --stat app/src/main/res/values/dimens.xml` is empty; `dpSpLiteralCheck` green; zero non-comment occurrences of the button copy under `app/src/main/java`.

## Task Commits

1. **Task 1: ProfileSwapDialog and its copy** — `9b1e164` (feat)
2. **Task 2: HomeUiState peers, the sequencing collector, both answer handlers** — `852e94c` (test, RED) → `1d3f179` (feat, GREEN)
3. **Task 3: Host the dialog on Home and prove it never stacks** — `81d3d9c` (test, RED) → `3e88c65` (feat, GREEN)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/ui/home/ProfileSwapDialog.kt` — **new**. `PROFILE_SWAP_DIALOG_TEST_TAG` plus the stateless `ProfileSwapDialog(accountName, onKeepLocal, onUseCloud, modifier)` over `PlayGamesPromptDialog`'s exact shell (`Dialog` + `usePlatformDefaultWidth = false`, `shapes.extraLarge` / `surfaceContainerHigh` / `clayShadow(elevation_dialog)`, `spacing_lg` inset, `content_max_width` cap). Private `ProfileSwapContent` (title, branching body) and `ProfileSwapActions` (the inverted pair). No `remember`, no state.
- `app/src/main/res/values/strings.xml` — five `profile_swap_*` strings inside plan 13-07's delimited Phase-13 block, under a comment recording that this is the one reserved-dialog case, why there are two bodies, and why the labels are account-neutral.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt` — `profileSwapVisible` and `profileSwapAccountName` as peers, with KDoc extending the existing sequencing sentence from two surfaces to three and stating why the name is a separate field.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` — `progressSubmitter` injected; one new `combine(...).distinctUntilChanged().collect { }` in `init`; `onKeepDeviceProgress()` / `onUseCloudProgress()`.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` — two new callbacks and the third sibling hosting conditional.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt` — both callbacks bound to the ViewModel.
- `app/src/test/java/com/app/pegsolitaire/data/FakeProgressSubmitter.kt` — **new**, the shared double.
- `app/src/test/java/com/app/pegsolitaire/ui/home/ProfileSwapDialogTest.kt` — **new**, 8 Robolectric cases.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelCloudTest.kt` — **new**, 11 Turbine/virtual-time cases in three groups (raised, never stacked, both answers).
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt` — 7 new cases; the `renderHome` helper and both inline call sites widened.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt` — `viewModelFor` widened (see deviations). **No existing assertion changed.**
- `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — its `HomeScreen` call site widened (see deviations).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `HomeViewModelTest` would not compile against the widened constructor**
- **Found during:** Task 2
- **Issue:** `HomeViewModel` gained a seventh constructor parameter, breaking `compileDebugUnitTestKotlin` for the whole module at `HomeViewModelTest.viewModelFor`.
- **Fix:** `viewModelFor` takes a `submitter: ProgressSubmitter` defaulting to a `FakeProgressSubmitter()` with **no** pending question — the same reasoning its `optIn` default already documents, so Phase 13's dialog cannot appear under assertions about onboarding, the ladder or the resume affordances.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt`
- **Commit:** `1d3f179`

**2. [Rule 3 - Blocking] `ButtonSoundTest` would not compile against the widened `HomeScreen`**
- **Found during:** Task 3
- **Issue:** `ButtonSoundTest` has a second `HomeScreen(...)` call site (`app/src/test/.../ui/ButtonSoundTest.kt:272`) that the plan's file list did not name; the two new parameters broke the module's test compilation.
- **Fix:** Both callbacks passed as no-ops, with a comment mirroring the one already there for the opt-in prompt — that helper renders Home's body, and the dialog's own cues are asserted in `ProfileSwapDialogTest`.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt`
- **Commit:** `3e88c65`

**3. [Rule 3 - Blocking] `FakeOnboardingRepository` redeclaration**
- **Found during:** Task 2
- **Issue:** The new test's onboarding fake collided with `HomeViewModelTest`'s file-private one — private top-level classes in the same package still produce the same JVM name.
- **Fix:** Renamed to `SwapTestOnboardingRepository`. Not merged with the existing one: this plan's fake needs a publicly settable `seen` to stage the WR-07 re-emission, and widening the other test's fake would have edited a file this plan had no reason to touch beyond the constructor widening.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelCloudTest.kt`
- **Commit:** `1d3f179`

### Plan Interpretations

**4. The screen's hosting conditional is narrower than a bare `if (state.profileSwapVisible)`**
Task 3 asked for a plain third sibling `if` whose comment says the ViewModel's guarantee must not be expressed twice — **and** for screen-level tests asserting that the dialog is absent whenever either other surface is present. Those are mutually exclusive: a bare `if` renders both dialogs when both flags are true, so the required assertion could not pass. Resolved in favour of the behaviour contract, which the plan states three separate times (`must_haves`, task 3 `<behavior>`, task 3 acceptance): the conditional is `state.profileSwapVisible && !state.onboardingVisible && !state.playGamesPromptVisible`, still a sibling `if` and still not an `else` (the `} else` acceptance grep returns 0). The source comment records why this one differs — `onboardingVisible` has a **second writer** the ViewModel's sequencing does not cover (the permanent How-to-play affordance forces it true at any time, D-15), and the guard can only ever withhold the wipe question, never raise one, so the two expressions can disagree in exactly one safe direction.

**5. `profile_swap_use_cloud` is account-neutral**
Task 1's `<action>` describes the label as "the destructive label, naming the account when known", but the same plan's objective and `must_haves` both quote it as **"Use their cloud progress"**, and the acceptance criterion fixes the string count at exactly five. Naming the account in the label would need a sixth string for the null-display-name case (or would render `Use 's cloud progress`). The label is therefore `Use their cloud progress`, and the arriving account is named in the body — which *does* have both variants. The reasoning is recorded in the `strings.xml` comment beside the five strings.

**6. Two acceptance greps were already unsatisfiable at HEAD; verified in intent instead**
- `grep -rn '"Keep this device' app/src/main/java/` "returns 0 hits": it returned **2** before this plan started, both in `ProgressSubmitter.kt`'s KDoc from plan 13-08. The intent — no copy in Kotlin code — is verified: filtering comment lines leaves **0** hits.
- `grep -c "ctaGradientBrush" …ProfileSwapDialog.kt` "returns 1": the import line makes 2 unavoidable (`PlayGamesPromptDialog.kt` also returns 2). The intent — one *usage*, in the `profile_swap_keep` button — is verified by line numbers above.

**7. `REQUIREMENTS.md` deliberately not updated**
CLOUD-02 is shared with plans 13-08 and 13-09; 13-09 is running in parallel in a sibling worktree, and the requirement is not satisfied end-to-end until `NoOpCloudSaveStore` is replaced with the real `PlayGamesCloudSaveStore`. Marking it here would risk a merge conflict and be premature — the same call plan 13-08 made and recorded. Left for the orchestrator or a later plan.

### Deferred Issues

None.

## Verification Results

All commands run from the worktree root.

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests '*Home*' --tests '*ProfileSwap*'` | **PASS** — `HomeScreenTest` 16/16, `HomeViewModelCloudTest` 11/11, `HomeViewModelTest` 28/28, `ProfileSwapDialogTest` 8/8; 0 failures, 0 errors |
| `./gradlew :app:testDebugUnitTest` (full suite) | **PASS** — 826 tests across 72 classes, 0 failures, 0 errors |
| `./gradlew :domain:test` | **PASS** |
| `./gradlew :app:detekt :app:ktlintCheck` | **PASS** |
| `./gradlew :app:dpSpLiteralCheck` | **PASS** |
| `./gradlew :app:assembleDebug` | **PASS** |
| `grep -c "onDismissRequest = onKeepLocal" ProfileSwapDialog.kt` | **1** |
| `ctaGradientBrush` usage vs `profile_swap_keep` | usage at line 200, inside the `Button(` at 187, label at 204; the `TextButton` at 208 has neither |
| `grep -c 'name="profile_swap_' strings.xml` | **5** |
| `grep -rn '"Keep this device' app/src/main/java/` minus comment lines | **0** |
| `git diff --stat app/src/main/res/values/dimens.xml` | **empty** — no new dimension needed |
| `grep -c "progressSubmitter.onProfileSwapResolved" HomeViewModel.kt` | **2** |
| `awk '/fun onKeepDeviceProgress/,/^        }/' … \| grep -c viewModelScope` | **0** (same for `onUseCloudProgress`) |
| `grep -c "ProfileSwapDialog(" HomeScreen.kt` | **1** |
| `grep -c "onKeepDeviceProgress\|onUseCloudProgress" HomeRoute.kt` | **2** |
| `awk '/state.onboardingVisible/,/^}/' HomeScreen.kt \| grep -c "} else"` | **0** |
| `grep -rln "ProfileSwapDialog" app/src/main/java/com/app/pegsolitaire/ui/` | 3 files, **all** under `ui/home/` |

**RED gates observed, not assumed.** Task 2's test commit was run first and failed with `Unresolved reference 'profileSwapVisible'` / `'profileSwapAccountName'`; task 3's failed with `No parameter with name 'onKeepDeviceProgress'`. Both went green only after the implementation commit that followed.

## Threat Model Coverage

| Threat ID | Disposition | Evidence |
|---|---|---|
| T-13-42 (accidental dismissal wiping progress) | mitigated | `onDismissRequest = onKeepLocal` as the only dismissal expression (grep = 1), plus `dismissingTheDialogKeepsThisDevicesProgressAndNeverWipesIt` and its unknown-name twin, both driven through the dialog's own back dispatcher |
| T-13-43 (destructive option carrying the visual emphasis) | mitigated | The gradient + `ButtonForward` cue on `profile_swap_keep`, `TextButton` + `ButtonDismiss` on `profile_swap_use_cloud`; line-scoped source assertion above, inversion commented at two levels |
| T-13-44 (a wipe the player did not understand) | mitigated | `theBodyEnumeratesEverythingTheReplacePathDestroys` asserts all four wiped categories against **both** body resources |
| T-13-45 (progress merged into the wrong account while unanswered) | mitigated | Unchanged from 13-08 — the gate is `ProgressSubmitter` step 1 and the only way to clear `profileSwap` is `onProfileSwapResolved`. This plan adds no bypass: `HomeViewModel` consumes exactly those two members, and the ViewModel test `anAnsweredSwapIsNeverReRaisedWhenTheRequestClears` pins that the UI neither re-raises nor resolves twice |
| T-13-46 (wipe cancelled halfway by back-navigation) | mitigated | Neither handler names `viewModelScope` (`awk`-scoped grep = 0), and both answers are asserted with no `runCurrent()` — the hand-off has already happened when the test looks |

## Known Stubs

None. Every surface this plan adds is wired end to end: the dialog renders from state, the state comes from `ProgressSubmitter.profileSwap`, and both answers reach `onProfileSwapResolved`. `CloudSaveStore` is still bound to `NoOpCloudSaveStore` in `PlatformModule` — 13-06's documented mid-phase scaffolding, replaced by plan 13-09, untouched here.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema change. The one trust boundary this plan touches — player intent to a destructive local wipe — is already in the register as T-13-42 through T-13-46.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/ui/home/ProfileSwapDialog.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/home/ProfileSwapDialogTest.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelCloudTest.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/data/FakeProgressSubmitter.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt` — FOUND (modified)
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` — FOUND (modified)
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` — FOUND (modified)
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt` — FOUND (modified)
- Commit `9b1e164` — FOUND
- Commit `852e94c` — FOUND
- Commit `1d3f179` — FOUND
- Commit `81d3d9c` — FOUND
- Commit `3e88c65` — FOUND
