# release-manager report — phase-14 ad testing safety (AdMob ban risk for REL-02)
Date: 2026-09-21
Branch: feature/phase-14-release-docs (8dc9f09..HEAD)
Skills loaded: ergoz-soft:standards (role: release-manager) → rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md; plugin copies read because `.claude/rules/VERSION` is 0.5.1 and the plugin is 0.5.2
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` has 0 active lessons; no `.planning/recall/RECALL.md` in this checkout)

## Summary
The founder's real ad IDs do not endanger QA at all — debug builds in this repo are hard-wired to
Google's demo units (`DEF-06-01`) and emulators are documented automatic test devices on the
**Next-Gen** page itself, not merely the legacy one. The only live exposure in the plan is REL-02: a
release build on the founder's unregistered phone, where an automatic interstitial generates repeated
impressions and one stray tap is literally bullet 1 of Google's invalid-traffic list. Registering the
phone as a test device 24 h ahead removes that exposure completely while still exercising the real ad
unit IDs ("production ads in test mode"), so live ads are **not** needed for REL-02 and must not be
used. Guidance written, checklist gated on it.

## Outputs
- `docs/ad-testing-safety.md` — new, binding: per-build-type exposure table, the Next-Gen emulator
  answer with its caveats, AdMob-UI test-device registration steps, where a test-device ID may live
  without being committed, the invalid-traffic policy applied activity by activity, the
  accidental-click procedure, the REL-02 recommendation, and an 8-row source table with URLs.
- `docs/ad-testing-safety.md` §3a — the founder-facing steps A–E, written to be lifted verbatim into
  `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md`, which the phase-14 plan owns and
  which does not exist on this branch.
- `docs/release-checklist.md` — new row **5.0** (test-device gate, ≥ 24 h, "Test Ad" label observed,
  blocks 5.6–5.8a), new row **4.1a** (read-only ID comparison against the Console — the one ad check
  test mode cannot cover), new rows **5.8a** (ad inspector) and **5.8b** (no unlabelled tap / incident
  recorded), 5.7 and 5.8 reworded to "real unit, in test mode", and an intro paragraph making the new
  doc binding with its one rule quoted.
- `.planning/reports/phase-14/release-manager-ad-safety.md` — this report.

## Checks
- `curl -sL https://developers.google.com/admob/android/next-gen/test-ads` — pass (HTTP 200; contains
  the literal "Key Point: Android emulators are automatically configured as test devices" inside the
  Next-Gen page's own "Enable test devices" section — the Next-Gen-specific confirmation that was
  asked for, not an inference from legacy)
- `curl -sL https://developers.google.com/admob/android/test-ads` — pass (HTTP 200; identical
  sentence, so the behaviour is not legacy-only)
- `curl -sL https://support.google.com/admob/answer/9691433` — pass (HTTP 200, "Set up a test
  device": UI steps, advertising-ID location, "production ads in test mode … using the ad unit IDs
  you've created", "without violating AdMob's invalid traffic policy", 1 h/24 h propagation both ways)
- `curl -sL https://support.google.com/admob/answer/3342054` — pass (HTTP 200, "Invalid traffic": the
  four bullets, accidental clicks in scope, "may suspend or disable the account", report form)
- `curl -sL https://support.google.com/admob/answer/2753860` — pass (HTTP 200, "Behavioral policies":
  "Testing your own ads by clicking on them is not allowed"; closure is cross-product with AdSense;
  new accounts flagged as duplicates)
- `curl -sL https://support.google.com/admob/answer/3342099` — pass (HTTP 200, "How you can prevent
  invalid activity": "Don't click your own ads, even if you think it's okay to do so"; Google refunds
  advertisers; Google will not disclose what it detected)
- `curl -sL https://developers.google.com/admob/android/next-gen/ad-inspector` — pass (HTTP 200,
  "Ad inspector launches only on test devices")
- `grep -nE "ca-app-pub|pub-[0-9]{10,}|ermanergoz|@gmail" docs/ad-testing-safety.md
  docs/release-checklist.md` — pass (no match; no publisher ID, ad unit ID, account or email in any
  tracked file. The only ID-shaped string in the doc is Google's own documented example hash)
- Read of `build-logic/src/main/kotlin/AdIdResolution.kt` and `app/build.gradle.kts` — pass (debug
  returns `AdIdDefaults.TEST_*` unconditionally for the three ad IDs per `DEF-06-01`; release resolves
  env → `local.properties` and fails the build on a missing or `REPLACE_ME` value;
  `local.properties` is ignored at `.gitignore:7`)
- Gradle, adb and the emulator — **not run, deliberately.** The task forbade both, and nothing in
  this work needs them: every claim is documentary or a source read.

## Open issues / risks
- **`14-OPERATOR-CHECKLIST.md` does not exist on this branch** and is owned by the phase-14 plan
  (present in the other checkout). Checklist row 5.0 depends on founder action that currently lives
  only in `docs/ad-testing-safety.md` §3a. Whoever writes that file must lift steps A–E, or 5.0 is a
  gate with no operator instruction behind it.
- **Propagation latency is the schedule risk, not the policy.** Up to 24 h to start serving test
  mode, and up to 24 h again to stop after removal. A REL-02 session booked for the same day the
  device is registered will either be blocked by 5.0 or tempted past it. Register ahead.
- **Test mode can mask a valid-but-wrong ad unit ID.** Mitigated by 4.1a (read-only comparison) plus
  ad inspector, not by any amount of tapping. Residual: a Console-side misconfiguration of a correct
  unit (format, floors) that only shows under live serving — observable in the Console after rollout,
  and not worth trading against an account closure.
- **The day mediation is added, this document's §1 note becomes load-bearing:** mediated ads render
  no "Test Ad" label and each network must be put in test mode separately, so §0's single rule would
  no longer be sufficient on its own.
- Google states it will not disclose what invalid activity it observed, so there is no
  after-the-fact remediation path to plan around. Prevention is the whole strategy.

## Handoff
Next: producer — put `docs/ad-testing-safety.md` §3a steps A–E in front of the founder as the
operator steps for REL-02 (Step A must be done ≥ 24 h before the session), and carry the one rule
into the QA brief: never tap an ad without the "Test Ad" label. qa-engineer — release-checklist 5.0
is now a hard gate before 5.6–5.8a, and 5.8a (ad inspector) is only reachable on a registered test
device.
