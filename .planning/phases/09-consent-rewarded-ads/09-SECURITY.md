---
phase: 09
slug: consent-rewarded-ads
status: verified
threats_open: 0
asvs_level: 1
created: 2026-09-02
---

# Phase 09 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Register authored at plan time (all 11 PLAN files carry `<threat_model>` blocks, T-09-01..T-09-55).
> Verified 2026-09-02 by gsd-security-auditor in verify-mitigations mode — every CLOSED row below was
> confirmed by locating the actual construct (file:line) in implementation or tests, not by reading claims.
> Full per-threat evidence table: auditor run recorded in this file's audit trail; see also 09-REVIEW.md
> for the five advisory warnings that weaken (but do not absent) specific mitigations.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| App ↔ Google Mobile Ads SDK | Rewarded ad load/show behind `AdGateway` seam | Advertising ID, ad requests/responses (Google-processed) |
| App ↔ UMP / consent | Consent form + TCF state behind `ConsentGateway` seam | Consent choices (SDK-owned storage, never cached by app) |
| App ↔ resume snapshot | Untrusted stored blob incl. allowance counters | Player progress + free-assist counters (fail-open codec) |
| App ↔ external browser | Privacy-policy link from Settings | Compile-time-constant HTTPS URL only |
| :domain ↔ :app | Pure-Kotlin policy layer (ArchUnit-enforced) | AssistAllowance values, no Android types |

---

## Threat Register

55 threats, T-09-01..T-09-55, defined across the 11 plan `<threat_model>` blocks (see each
09-NN-PLAN.md for the full row: category, component, disposition, mitigation plan).

**Summary of dispositions after the 2026-09-02 audit:**

| Group | Threats | Disposition | Status |
|-------|---------|-------------|--------|
| Code-verified mitigations | T-09-01..30, T-09-32..37, T-09-39..50, T-09-53 (49 threats) | mitigate | closed |
| Policy transfer | T-09-31 (privacy policy contradicted app) | transfer | closed — policy revised & republished 2026-09-02, verified live (D-19); receiving party Phase 14 REL-01/REL-04 |
| Closed by decision | T-09-54 (SDK init / offers after consent decline) | accept (D-20) | closed — user decision 2026-09-02: `rewardedOfferable` gates on `canRequestAds()` only; ads non-personalized by construction after decline |
| Accepted risks | T-09-38, T-09-SC, T-09-51, T-09-52, T-09-55 | accept | closed — see Accepted Risks Log |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Notable code-verified anchors (full list in the 2026-09-02 auditor report):
- T-09-01/03: `AssistAllowance.kt` range-validated restore (`fromStoredOrFull`) + zero floor on `spend`
- T-09-13/14/16: clock-freeze bracket in `GameViewModel` (freeze at request, guard in `resumeTickerIfNeeded`, `startMark` null-safety)
- T-09-17/18/30: UMP consent — SDK-owned TCF state, no app-side caching, `MobileAds.initialize` reachable only post-consent-resolution
- T-09-21/49: no SDK type in the Hilt graph (`PlatformBoundaryTest` green with real gateway bound)
- T-09-32/43: exactly one `requestRewarded` call site (accept tap) and exactly one `earned = true` (SDK reward callback)
- T-09-48 + `adIdLiteralCheck` Gradle gate: no ad-unit literals outside `BuildConfig`

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-09-01 | T-09-38 | D-12 silence when ads are unservable is the designed UX (free grant, no snackbar); implemented at `GameViewModel.kt:440` and pinned by `GameViewModelAllowanceTest` asserting the *absence* of a snackbar. | user | 2026-09-02 |
| AR-09-02 | T-09-SC | Supply chain: only `ads-mobile-sdk` + `user-messaging-platform` added (first-party Google Maven, audited Approved in 09-RESEARCH.md §dependency audit); no `play-services-ads`, zero flagged packages. | user | 2026-09-02 |
| AR-09-03 | T-09-51 | AdMob dashboard "zero impressions" not read directly (developer console access). Structural substitute verified: debug resolves demo-publisher IDs only, every ad watermarked "Test Ad", SDK config request named `ca-app-pub-3940256099942544`. **Carried forward to Phase 14 (release verification).** | user | 2026-09-02 |
| AR-09-04 | T-09-52 | The developer's own published UMP consent message has never rendered — debug builds resolve Google's demo app ID, so only the test-publisher form is reachable. Consent *sequence* fully verified on device. **First real render is Phase 14's release build (REL-02).** | user | 2026-09-02 |
| AR-09-05 | T-09-55 | Early-dismiss-grants-nothing unprovable on test inventory (creative fires `onUserEarnedReward` in ~2s, no pre-reward close affordance; dismissals at t+2/8/33s all post-reward). Compensating pins verified: single `earned = true` site + `Declined`-refills-nothing spec. **Carried forward to Phase 14.** | user | 2026-09-02 |

*Accepted risks do not resurface in future audit runs.*

---

## Residual Warnings (advisory, tracked in 09-REVIEW.md)

Mitigations that exist but whose guarantee is narrower than documented — none absents a control:
- WR-01 weakens T-09-11/13: no re-entrancy guard on `onAcceptAdOffer` (same-frame double-tap double-folds the frozen clock, double undo)
- WR-02 weakens T-09-37/47: no exception containment around `ad.show()` (synchronous SDK throw → crash instead of `Unavailable`)
- WR-03 weakens T-09-47: `RewardedAdSlot.await()` take-semantics not enforced on cancellation paths
- IN-01: real accept-path bound is 15s (ACCEPT_WAIT_MS + ACTIVITY_WAIT_MS sequential), while four KDocs claim 5s
- D-20 is recorded in planning docs but not yet in `AdMobAdGateway.rewardedOfferable`'s KDoc — capture it there on next touch

Fix path: `/gsd:code-review 9 --fix`.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-02 | 55 | 55 | 0 | gsd-security-auditor (verify-mitigations mode) + user acceptance of AR-09-01..05 |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter
