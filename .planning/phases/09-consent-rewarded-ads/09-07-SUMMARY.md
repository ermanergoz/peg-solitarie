---
phase: 09-consent-rewarded-ads
plan: 07
subsystem: ui
tags: [settings, privacy, ump, consent, rel-05, cons-03, compliance]
status: complete

# Dependency graph
requires:
  - phase: 09-02
    provides: "ConsentGateway.privacyOptionsRequired + showPrivacyOptions() seam"
  - phase: 09-03
    provides: "ClaySnackbarHost, settings_* string resources, settings_row_icon_size dimen"
provides:
  - "PrivacyLinks.POLICY_URL — the compiled-in live privacy-policy URL"
  - "SettingsUiState.privacyOptionsRequired — the CONS-03 visibility flag"
  - "SettingsViewModel.onManagePrivacyChoices() — the UMP privacy-options entry point"
  - "SettingsLinkRow / SettingsSectionHeader — the tappable-row and section-label leaves"
  - "FakeConsentGateway — the shared test double for the consent seam"
affects: [09-11, 14-release-readiness]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scoped compile-time constant for an ACTION_VIEW URI (intent-redirection defence)"
    - "Robolectric shadowOf(app).nextStartedActivity for launched-intent assertions"
    - "Robolectric shadowOf(app).checkActivities(true) to force ActivityNotFoundException"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/settings/PrivacyLinks.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/FakeConsentGateway.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt
    - .planning/phases/09-consent-rewarded-ads/09-CONTEXT.md

key-decisions:
  - "The T-09-31 gate was cleared by a policy revision published 2026-09-02, not by relaxing the gate"
  - "PrivacyLinks.POLICY_URL is a plain const val, not a buildConfigField — RESEARCH Assumption A6 confirmed"
  - "The Settings column became verticalScroll: six elements pushed the CONS-03 row below the fold"
  - "privacyOptionsRequired is fail-closed while the file's toggle defaults are fail-open, for the same 'player keeps playing' reason"

patterns-established:
  - "SettingsLinkRow is the exact peer of SettingsToggleRow, differing only in a trailing icon slot"
  - "Section headers join the clayEntrance cascade so the stagger stays strictly top-to-bottom"

requirements-completed: [CONS-03, REL-05]

# Metrics
duration: 42min
completed: 2026-09-02
---

# Phase 9 Plan 07: Settings Privacy Section Summary

**Settings now has two named sections: an always-present "Privacy policy" row that opens the live policy in the system browser (REL-05), and a "Manage privacy choices" row that appears only when UMP reports a required consent form (CONS-03).**

## Performance

- **Duration:** ~42 min
- **Tasks:** 3 of 3 complete
- **Files:** 3 created, 7 modified
- **Verification:** `./gradlew check --offline` green (11m 9s) — detekt, ktlint, `dpSpLiteralCheck`, `PlatformBoundaryTest` and the full unit suite

## Checkpoint History

Task 1 is a `checkpoint:human-action` with `gate="blocking"`. A prior executor ran it on 2026-08-31 and **halted**: the live policy was reachable but was the v1.0 text (last updated April 2023), which declared "I do not collect or log any personal data" and named no advertising SDK. That is threat **T-09-31**, and the gate is exactly where the plan says it must surface.

**The gate was resolved by fixing the policy, not by lowering the bar.** The developer published a revision on **2026-09-02**, and this run re-verified all four criteria against the served page before writing anything:

| # | Task 1 criterion | Result |
|---|------------------|--------|
| 1 | Full URL obtained | **Pass** — `https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` |
| 2 | HTTPS, stable, reachable without login | **Pass** — HTTP 200, no redirect hop, no auth |
| 3 | Names advertising-ID collection and every third-party SDK receiving player data | **Pass** — see below |
| 4 | Single static URL, build-type invariant | **Pass** — confirms RESEARCH Assumption A6 |

Criterion 3, re-checked directly against the rendered text rather than taken on report:

- **Google Mobile Ads SDK / AdMob** named as a third-party recipient
- **Advertising ID (AAID)** collection disclosed
- The **UMP consent form** and the in-app "Settings → Privacy → Manage ad consent" path described — which is precisely the row this plan builds
- Google's privacy policy linked (`policies.google.com/privacy` and `.../technologies/partner-sites`)
- **Play Games Services** covered, so the revision holds through Phases 11–13 rather than needing redoing
- The old blanket "do not collect or log any personal data" claim is **gone**, narrowed to a truthful statement about the developer's own servers

This clears T-09-31 and removes the inherited blocker from Phase 14's Data-safety form (REL-01/REL-04), which must be written from this text.

## Task Commits

| Task | Name | Commit |
|------|------|--------|
| 1 | Record the live privacy-policy URL in D-19 | `b9f8ea1` |
| 2 | PrivacyLinks, the UiState flag and the ViewModel collector | `d5c427b` |
| 3 | The Privacy section on SettingsScreen, plus its test | `c022a3f` |

## What Was Built

**`PrivacyLinks.POLICY_URL`** — a scoped `const val`. Its KDoc records the security reason it must *stay* one: the URI handed to an implicit `ACTION_VIEW` is the entire attack surface of that hand-off (T-09-27, ASVS V12), so a value sourced from storage, a route argument or remote config would turn a trusted Settings row into an intent-redirection vector. A `buildConfigField` was rejected because criterion 4 confirmed the URL is build-type-invariant and public.

**`SettingsUiState.privacyOptionsRequired`** — defaults `false`. The file's existing KDoc justifies its `true` defaults as fail-open, so the opposite default needed its own reasoning or the file would contradict itself: both directions pick the one where a wrong guess still leaves the player playing. A hidden row costs a player who does not need one nothing; a shown row that opens nothing is a dead end on a compliance surface.

**`SettingsViewModel`** — a third `init` collector over `ConsentGateway.privacyOptionsRequired`, plus `onManagePrivacyChoices()`. The **interface** is injected; the ViewModel names no SDK type and never holds an `Activity` (both are bytecode build gates).

**`SettingsScreen`** — `SettingsSectionHeader` and `SettingsLinkRow` (the exact peer of `SettingsToggleRow`, differing only in a trailing icon slot), the "General"/"Privacy" headers, and the 0–5 entrance cascade. The policy row carries the `ExitToApp` glyph as functional signposting; the manage-choices row deliberately carries none, since the UMP form opens in-app — the glyph's absence is what makes the two rows distinguishable at a glance.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 — Missing critical functionality] The Settings column could not scroll**

- **Found during:** Task 3, caught by two failing tests rather than by inspection
- **Issue:** The column went from 2 elements to 6, but was never scrollable. The last element is the CONS-03 "Manage privacy choices" row, so on a short screen, in landscape, or at a large font scale the privacy entry point rendered **below the fold with no way to reach it**. The tests failed in exactly the way that matters: the node existed in the semantics tree, `assertIsDisplayed` failed, and a click on it did not invoke the handler — a row that is present and untappable.
- **Why not deferred:** a compliance affordance the player cannot scroll to is the same dead end as one that opens nothing, which is the sentence governing this phase. This is a correctness requirement, not polish.
- **Fix:** `.verticalScroll(rememberScrollState())` on the column, with the reasoning recorded inline.
- **Files modified:** `SettingsScreen.kt`
- **Commit:** `c022a3f`

**2. [Rule 3 — Blocking issue] `SettingsViewModel`'s new constructor parameter broke 13 existing test callsites**

- **Found during:** Task 2
- **Fix:** Added `FakeConsentGateway` as a **shared** test double (the three `Settings*Test` classes need an identical surface from it, unlike the `SettingsRepository` fakes which stay file-private because each class observes something different) and threaded it through `SettingsViewModelTest` and `SettingsViewModelSoundTest`.
- **Commit:** `d5c427b`

**3. [Rule 3 — Blocking issue] `detekt` LongMethod on `SettingsScreen`**

- **Found during:** Task 3 `check` run — 88 lines against a 60-line limit
- **Fix:** Extracted `GeneralSection` and `PrivacySection`. Both emit their children straight into the caller's `Column`, so `Arrangement.spacedBy` and the `clayEntrance` cascade are unaffected — this changes how the file reads, not how the screen lays out. `ktlintFormat` then fixed import ordering.
- **Commit:** `c022a3f`

### Documented, not deviated

The KDoc in `SettingsViewModel` originally spelled out `UmpConsentGateway` while explaining why that type must *not* be named. That tripped the plan's own `grep -c "UmpConsentGateway\|android.app.Activity"` acceptance criterion, which is an automated gate and does not read intent. Reworded to "the concrete UMP implementation"; the criterion now returns 0.

## Verification Results

| Criterion | Result |
|-----------|--------|
| `check --offline` (detekt, ktlint, dpSpLiteralCheck, PlatformBoundaryTest, full suite) | **green** |
| `SettingsPrivacyTest` + `SettingsPrivacyViewModelTest` | **14 tests green** |
| `grep -c "LocalActivity\|as Activity\|android.app.Activity"` on `SettingsScreen.kt` | **0** |
| `grep -c "UmpConsentGateway\|android.app.Activity"` on `SettingsViewModel.kt` | **0** |
| `grep -c "ctaGradientBrush\|goldGradientFill"` on `SettingsScreen.kt` | **0** — neither accent appears |
| `grep -nE "[0-9]+\.(dp\|sp)"` on `SettingsScreen.kt` | **no matches** |
| `ClaySnackbarHost` used, no inline `Snackbar(` styling | **confirmed** |

The intent test asserts **both** the action (`ACTION_VIEW`) and the data URI (equal to `PrivacyLinks.POLICY_URL`), read back off Robolectric's shared instrumentation via `shadowOf(app).nextStartedActivity` — not merely "nothing threw". The missing-browser test forces a real `ActivityNotFoundException` with `checkActivities(true)` and asserts the snackbar message reaches the player.

## Known Stubs

None. Every row is wired to a live source: the policy URL is the real published policy, and the manage-choices row's visibility and action both go through the real `ConsentGateway` seam.

## Threat Flags

None. No new network endpoint, auth path, file access or schema change was introduced; the one trust boundary touched (the `ACTION_VIEW` hand-off) is the one the plan's threat model already covers, and its mitigation (a compile-time-constant URI) is implemented and documented in `PrivacyLinks`' KDoc.

## Issues Encountered

The snackbar assertion needed `waitUntil` on node presence rather than a bare `waitForIdle`: `showSnackbar` suspends for its `Short` duration, and the Compose test clock auto-advances, so a plain idle wait races the message. Rows below the fold likewise need `performScrollTo()` before assertion now that the column scrolls — worth knowing for anyone adding a seventh Settings element.

## Next Phase Readiness

- **CONS-03 and REL-05 are implemented and tested**, but CONS-03's *real* form has still never been rendered: `showPrivacyOptions()` is asserted through a fake here. On-device verification stays with **plan 09-11**, which remains gated on **D-18** (the UMP consent message is not yet configured in the AdMob console — UMP renders nothing without it, and its absence is invisible from a non-EEA test location). This plan does not clear that gate.
- **Phase 14** inherits a policy that now matches the app's behaviour. The Data-safety form (REL-01/REL-04) must be written from the revised text, which names AAID collection, AdMob, UMP and Play Games.
- **Upstream dependencies were all present and needed no rework:** the consent seam (09-02/09-05), `ClaySnackbarHost` and the seven `settings_*` strings (09-03), and the `settings_row_icon_size` dimen.

## Self-Check: PASSED

- All three created files verified present on disk
- All three task commits verified in `git log`
- `git diff --diff-filter=D` against the plan's base commit reports **no deletions**
- Working tree clean

---
*Phase: 09-consent-rewarded-ads*
*Plan: 07 — complete (Task 1 paused at the T-09-31 gate on 2026-08-31; resolved by a policy revision published 2026-09-02)*
*Date: 2026-09-02*
