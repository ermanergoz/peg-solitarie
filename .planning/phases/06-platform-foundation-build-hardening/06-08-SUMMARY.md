---
phase: 06-platform-foundation-build-hardening
plan: 08
subsystem: build
tags: [cold-start, baseline, external-prerequisites, app-identity, PLAT-05]
requires: ["06-03"]
provides:
  - "pre-SDK cold-start baseline (Pixel_9a API 36, median 633 ms) in 06-BUILD-EVIDENCE.md"
  - "External prerequisites table with statuses, dates, and the answered developer-website question"
  - "published-app identity adopted: applicationId com.erman.pegsolitarie, versionCode 3 (D-20)"
affects:
  - "plan 06-09 (may now run — both pre-SDK baselines exist per D-14)"
  - "plan 06-10 (release smoke test installs com.erman.pegsolitarie)"
  - "Phase 14 (PLAT-08 comparand + revised SHA-1 story per D-21)"
tech-stack:
  added: []
  patterns:
    - "am start -W displayed-time sampling with warm-up discard as the honest coarse baseline"
key-files:
  created: []
  modified:
    - .planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md
    - app/build.gradle.kts
    - .planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md
decisions:
  - "D-20: update the published listing com.erman.pegsolitarie (versionCode 3, versionName 3.0.0); namespace stays com.app.pegsolitaire"
  - "D-21: original keystore, no Play App Signing — enroll at next upload; credential story is keystore SHA-1 + debug SHA-1 until then"
  - "D-22: developer account, AdMob account, and user-controlled website all EXIST — lead-time risk collapses"
  - "D-23: minSdk 24 vs published 16 accepted (API 16-23 users keep the old version); Realm scores do not migrate"
metrics:
  duration: ~25 min (+ user checkpoint)
  completed: 2026-08-27
  tasks: 3 (Task 2 skipped — device already attached)
  commits: 2
---

# Phase 6 Plan 08: Cold-Start Baseline & External Prerequisites Summary

Captured the pre-SDK cold-start baseline (median **633 ms** on AVD `Pixel_9a`, API 36, debug
variant, 9 samples, honestly caveated) before any SDK artifact reaches the graph, and recorded
all six External Prerequisites with statuses. The checkpoint surfaced a milestone-shaping
discovery: **an already-published listing exists** and this project now updates it.

## Task Outcomes

**Task 1 (auto)** — `emulator-5554` was already attached, so device automation stopped at step 1.
Installed `:app:installDebug`, sampled 10× `am force-stop` → `am start -W`, discarded the warm-up
(721 ms), recorded min 603 / median 633 / max 649 ms with the required precision caveat and the
Phase-14 same-device re-measure instruction. Committed as `90f4a72`.

**Task 2 (checkpoint:human-action)** — skipped; its trigger condition (no device) never fired.

**Task 3 (checkpoint:human-action)** — the user disclosed the published app
(`com.erman.pegsolitarie`, Java/Realm, no ads/PGS, versionCode 2) and answered the four identity
questions. Consequences recorded as D-20..D-23 in CONTEXT.md; the six-row prerequisites table
landed in 06-BUILD-EVIDENCE.md. The developer-website question — the milestone's most
schedule-relevant line — is answered: **a website exists and the user controls it.** All console
items remain "not started" but are unblocked and total ~1 hour of user work; achievement icons
are the only open-ended item.

## Deviation

**In-scope addition (user-approved at checkpoint):** `applicationId` → `com.erman.pegsolitarie`,
`versionCode` 3, `versionName` "3.0.0" in `app/build.gradle.kts` (commit `bca2719`). Applied here
rather than deferred because 06-09 consumes the merged manifest and 06-10 installs the release
APK — later adoption would have invalidated their evidence. `assembleDebug`,
`BuildConfigIdsTest`, and `ManifestSurfaceTest`-relevant checks re-run green; `namespace` and all
code packages unchanged.

## Verification

- `## Cold-start baseline` section: no `TBD`, device + API + variant + command + 9 samples + min/median/max + caveat ✓
- `## External prerequisites` section: six rows, each with status/date/notes; website question answered explicitly ✓
- D-14 ordering intact: both pre-SDK baselines (APK ledger rows 1–2, cold start) committed before 06-09 runs ✓
