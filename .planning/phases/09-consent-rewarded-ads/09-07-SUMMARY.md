---
phase: 09-consent-rewarded-ads
plan: 07
subsystem: ui
tags: [settings, privacy, ump, consent, rel-05, cons-03, compliance]
status: blocked-at-checkpoint

# Dependency graph
requires:
  - phase: 09-02
    provides: "ConsentGateway.privacyOptionsRequired + showPrivacyOptions() seam"
  - phase: 09-03
    provides: "ClaySnackbarHost, settings_* string resources, settings_row_icon_size dimen"
provides: []
affects: [09-11, 14-release-readiness]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified: []

key-decisions:
  - "Task 1 human gate NOT satisfied: the live policy URL was located, but its CONTENT fails the gate's third criterion"
  - "Canonical policy URL is https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html (the Play-registered github.io URL 301-redirects to it)"
  - "No source file under app/ was written — Tasks 2 and 3 both depend on a URL whose policy text must first be updated"

patterns-established: []

requirements-completed: []

# Metrics
duration: 8min
completed: 2026-08-31
---

# Phase 9 Plan 07: Settings Privacy Section Summary

**Halted at the Task 1 blocking human gate: the live privacy policy was found and verified reachable, but it declares "I do not collect or log any personal data" and names no advertising SDK — it cannot legally cover the ads this phase is adding.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-08-31T18:52:00Z
- **Completed (halted):** 2026-08-31T19:00:39Z
- **Tasks:** 0 of 3 complete (halted at Task 1, `checkpoint:human-action`, `gate="blocking"`)
- **Files modified:** 0 source files; 1 planning file (this SUMMARY)

## Status: BLOCKED AT CHECKPOINT

Task 1 is a `checkpoint:human-action` with `gate="blocking"`. It is the plan's **first** task, so no
implementation work precedes it. Tasks 2 and 3 both consume the value Task 1 collects
(`PrivacyLinks.POLICY_URL`), so neither could be started.

Per the plan's own instruction — *"a policy that omits the ads SDK will fail there, not here"* and
*"shipping a row that opens nothing is worse than shipping no row"* — execution stopped rather than
compiling in a URL that points at a policy contradicting the app's behaviour.

## What Was Established (research, not code)

The gate asks for a URL that "is not written down anywhere in `.planning/`". Rather than pause with an
empty prompt, the URL was recovered from the developer's own prior art (per the project's
"check existing work first" convention) and then checked against all four of the gate's criteria.

### The URL

The live Play listing for `com.erman.pegsolitarie` — the listing this rewrite **updates**, not a new
app — declares this privacy-policy URL:

```
https://ermanergoz.github.io/policies/peg-solitarie-privacy-policy.html
```

That URL returns **HTTP 301** to the developer website, which serves the policy directly:

```
https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html   → HTTP 200, no redirects
```

`https://ermanergoz.com/...` is therefore the canonical value and the one that should become
`PrivacyLinks.POLICY_URL` — it matches D-13's "live on the developer website" wording and avoids
spending a redirect hop on every tap.

### Gate criteria, checked

| # | Task 1 criterion | Result |
|---|------------------|--------|
| 1 | Full URL obtained | **Pass** — see above |
| 2 | HTTPS, stable, reachable without login | **Pass** — HTTP 200, 4.5 KB static HTML, no auth |
| 3 | Names advertising-ID collection and every third-party SDK receiving player data | **FAIL** — see below |
| 4 | Single static URL, invariant per build type | **Pass** — confirms RESEARCH Assumption A6; a plain `const val` is correct, no `buildConfigField` needed |

### Criterion 3 — the blocker

The served policy is the **v1.0 (no-ads) policy, last updated April 28, 2023**. Full-text scan of the
rendered page:

| Term | Occurrences |
|------|-------------|
| `advertis*` | **0** |
| `admob` | **0** |
| `google` | **0** |
| `SDK` | **0** |
| `analytics` / `tracking` / `cookie` | **0** |
| `GDPR` / `personali*` | **0** |

And it makes two affirmative claims that the Phase 9 app will contradict the moment the real gateway
is bound in:

> "Personal Data — I do not collect or log any personal data."
> "Usage Data — I do not collect or log any usage data."

This is exactly threat **T-09-31** (Information disclosure — policy content vs. app behaviour,
disposition `transfer`), which the plan assigns to *"the Task 1 human gate"*. The gate has now been
run, and it did not pass.

The consequence is not confined to Phase 14. Shipping a UMP consent flow and a rewarded-ad SDK behind
a policy that states no data is collected is a live Play-policy and GDPR exposure at the moment of
release, and Phase 14's Data-safety form (REL-01/REL-04) must be written *from* this text — it will
fail there too.

## Task Commits

None — no task reached completion.

**Plan metadata:** this SUMMARY only.

## Files Created/Modified

- `.planning/phases/09-consent-rewarded-ads/09-07-SUMMARY.md` — this partial-progress record

`09-CONTEXT.md` D-19 was deliberately **left untouched**, with its
`<!-- URL pending -->` marker intact. Writing the URL in now would flip the gate's automated check
(`! grep -q 'URL pending'`) to green while criterion 3 is still unresolved — closing the gate on
paper without closing it in fact. The resuming agent should perform that edit as Task 1's action once
the policy text is fixed.

## Decisions Made

- **The canonical URL is the `ermanergoz.com` one, not the `github.io` one.** Both work through a
  browser (`ACTION_VIEW` follows redirects), but the direct-200 URL is the honest constant.
- **Stopped rather than proceeded with a content-invalid policy.** Tasks 2 and 3 are mechanically
  unblocked by the URL alone — the row would render and open a live page. The halt is on the
  compliance criterion the gate exists to enforce, not on a missing string.
- **No `buildConfigField`.** Criterion 4 confirmed the URL is build-type-invariant, so the plan's
  scoped `const val` shape in `PrivacyLinks` stands and Task 2 does not change shape.

## Deviations from Plan

None — the plan's blocking gate was honoured exactly as written. The URL research performed within the
gate is the gate's own step 1, not a deviation.

## Issues Encountered

**The live privacy policy predates the entire monetization milestone.** It was written for a v1.0 app
with no ads, no Play Games, and no network use. Phase 9 adds Google Mobile Ads (Next-Gen), UMP, and
advertising-ID collection; Phases 11–13 add Play Games Services. The policy must be revised to name:

1. Collection/use of the **advertising ID (AAID)**
2. **Google AdMob / Google Mobile Ads SDK** as a third party receiving data, with a link to Google's
   own privacy policy
3. The **UMP consent mechanism** and how a player changes their choices (this is what the CONS-03
   "Manage privacy choices" row exists to reach)
4. Anything Phases 11–13 add (Play Games Services sign-in / Saved Games), if the revision is to hold
   through the milestone rather than be redone

Revising the policy is outside this repository and outside this plan's scope — it is developer-website
work, which is why the gate is `human-action`.

## User Setup Required

**Blocking.** Two items, in order:

1. **Update the privacy policy** at `https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html`
   to cover the four points above, and republish it.
2. **Confirm the URL** to compile into `PrivacyLinks.POLICY_URL` — presumably the same
   `ermanergoz.com` URL, unless the revision moves it.

Then re-run this plan. Tasks 2 and 3 are fully specified and need no further human input; they should
execute autonomously once D-19 carries a confirmed URL.

## Next Phase Readiness

- **09-07 is not complete.** REL-05 and CONS-03 remain unimplemented; `SettingsScreen` still has no
  Privacy section, no section headers, and no `ClaySnackbarHost`.
- **Upstream dependencies are in place and were verified present** in this worktree:
  `ConsentGateway.privacyOptionsRequired` / `showPrivacyOptions()` (09-02, implemented 09-05),
  `ClaySnackbarHost` (09-03), all seven `settings_*` string resources (09-03), and the
  `settings_row_icon_size` dimen (09-03). Nothing upstream needs rework.
- **Plan 09-11** (on-device UMP verification) is separately gated on D-18, the unconfigured AdMob
  consent message. Both that gate and this one are developer-console/website work and can be cleared
  in parallel.
- **Phase 14** (REL-01/REL-04 Data-safety form) inherits the policy-content problem directly. Fixing
  the policy now removes a blocker there as well.

---
*Phase: 09-consent-rewarded-ads*
*Plan: 07 — halted at Task 1 checkpoint*
*Date: 2026-08-31*
