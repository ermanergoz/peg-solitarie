---
phase: 14-release-readiness
type: release-plan
status: pending
owner: release-manager
blocks: [REL-02, REL-03]
requires: [14-OPERATOR-CHECKLIST.md]
---

# Phase 14 Release Plan — the studio side of shipping v1.1

Companion to `14-OPERATOR-CHECKLIST.md` (what only the founder can do) and `14-COLD-START.md`
(how PLAT-08 gets measured). This document is the studio's own work: what the version becomes,
how the release branch is cut, how the signed artifact is produced, how a *downloaded* build gets
verified feature by feature, and the `THIRD_PARTY.md` the studio's rules require and this repo has
never had.

Nothing in this document publishes anything. Upload and production deploy wait on
`.planning/approvals/publish-3.0.0.approved`, which does not exist, must not be created by the
studio, and is enforced by the plugin's publish hook rather than by anyone's good intentions.

---

## 1. Version — recommendation and reasoning

### Recommendation

| Field | Today | On the release branch | Why |
|-------|-------|----------------------|-----|
| root `VERSION` | `3.0.0` | **`3.0.0` — unchanged** | see below |
| `versionName` | `3.0.0` | `3.0.0` (must equal `VERSION`) | studio rule |
| `versionCode` | `3` | **`30000`** | `X*10000 + Y*100 + Z` per rules/play-store.md |
| git tag | — | **`v3.0.0`** | studio rule `v<X.Y.Z>` |
| release branch | — | **`release/3.0.0`** | studio rule `release/<X.Y.Z>` |
| approval file | — | `.planning/approvals/publish-3.0.0.approved` | derived from root `VERSION` |

**Ship milestone v1.1 as product version 3.0.0. Do not rename the release to 1.1.0.**

### Why `3.0.0` and not `1.1.0`

The question is real, because two numbering systems are running side by side in this project and
they do not agree:

- `.planning/` counts **milestones**: `v1.0` (Slice 1, shipped 2026-08-25) and `v1.1`
  (Slice 2 — Play Games, AdMob & Polish, this one). `MILESTONES.md`, `ROADMAP.md` and `STATE.md`
  all speak this language, and the existing git tag is `v1.0`.
- The **Play listing** counts published builds, and it is at `versionCode 2` / `versionName "2.0"`
  — a *different application*. That is the founder's earlier peg-solitaire app. Milestone v1.0 of
  this rewrite was tagged on `main` but **never uploaded**; the store has never seen any build
  produced by this repo.

So the store-facing number has to move forward from `"2.0"`, and four things follow:

1. **`1.1.0` would read to a player as a downgrade.** Play only enforces monotonicity on
   `versionCode`, so `versionName "1.1.0"` after a published `"2.0"` is *permitted* — and it
   would appear on the store page, in the app's own about surface, and in every review, as the
   app going backwards. There is no upside to buy with that.
2. **`3.0.0` is already a recorded decision, taken on 2026-08-27** and written into
   `app/build.gradle.kts` with its reasoning inline (*"the versionCode must exceed the published
   versionCode 2 (versionName '2.0')"*), plus root `VERSION`. Re-opening it now costs a decision
   record and buys nothing.
3. **A complete rewrite is what a major bump is for.** The whole application is new — new
   language, new UI toolkit, new architecture, a new mode. `2.0 → 3.0.0` describes that honestly.
4. **The two numbering systems are already decoupled and that is fine.** Milestone v1.0 shipped
   under tag `v1.0` while the store sat at `2.0`; nothing broke. What was missing is the mapping
   written down, so it is written down here: **milestone v1.1 ships as product version 3.0.0, tag
   `v3.0.0`.** Add that line to `MILESTONES.md` when the milestone closes.

### Why `versionCode` changes from 3 to 30000

`rules/play-store.md` fixes the formula: `versionCode = X*10000 + Y*100 + Z`. For `3.0.0` that is
**30000**. The current `3` predates the studio and does not follow it.

- Monotonic against the published `2`: yes.
- Monotonic against the repo's current `3`: yes — and `3` has never been uploaded, so raising it
  costs nothing and breaks no installed copy.
- It buys room: with the formula, `3.0.1` is `30001` and `3.1.0` is `30100`, and no future release
  ever has to think about the number again.

State the one-way door plainly: `versionCode` can never decrease for a published app. Adopting the
formula now is cheap; adopting it after shipping `3` would mean `30000` anyway. Adopt it now.

### Tag note

The existing tag is `v1.0`, two components, which does not match the studio's `v<X.Y.Z>`. Leave it
alone — retagging published history to satisfy a naming rule is not worth a rewritten ref. New tags
from here use three components.

---

## 2. The release branch

Standard gitflow per `rules/gitflow.md`, with one precondition this repo does not currently meet.

### 2a. Precondition: `check` must be green **at release settings** — and cannot be today

`rules/gitflow.md` requires "full check at release fuzz count where applicable", and for a game
that is `FUZZ_SEEDS=100000` (rules/testing.md, rules/puzzle-design.md invariant 8). **Neither the
fuzz harness nor the coverage gate exists in this repo.** The phase 13 tech-lead raised it as a
product-level escalation and the producer's gate evidence repeated it:

> No kover coverage gate and no fuzz harness exist in this repo's `check`, contrary to
> rules/kotlin-android.md and rules/testing.md — inherited from the pre-studio phases.
> This gate is therefore green against a Check command that is itself weaker than the studio
> rules require.

`check` here is detekt + ktlint + `lint` + the four literal gates + `:domain:depthSweep` + the JVM
suites. Verified again on this branch: `grep -rn kover build-logic/ app/build.gradle.kts
gradle/libs.versions.toml` → 0 hits; no task honours `FUZZ_SEEDS`.

**This is not a paperwork gap for a puzzle game whose stated core value is "always-solvable".**
The Challenge generator's solvability is currently proven by golden-master fixtures at levels
1/2/10/100/1000 and by a depth sweep — five frozen levels and a targeted sweep, not a population.
The claim "every generated level is solvable" is asserted at 100,000-seed scale and has never been
tested at it.

Two ways forward, and the recommendation is the first:

- **Recommended — build it before cutting the branch.** Scope: a `:domain` fuzz task reading
  `FUZZ_SEEDS` (default 10,000 for CI, 100,000 for release) that runs
  generate → validate → solve → replay → verify-win per seed and writes failing seeds to
  `build/fuzz-failures.txt`; plus `kover` with `koverVerify` at ≥ 90% line coverage on `:domain`,
  both wired into `check`. Every piece it needs already exists in `:domain` (`PuzzleGenerator`,
  `Solver`, `RulesEngine`, the recorded-solution replay harness) — this is wiring and a task, not
  new algorithms. **Owner: tech-lead to scope, android-game-dev to implement, on a
  `feature/*` branch off develop, before `release/3.0.0` is cut.**
- **Fallback — a recorded waiver.** If the founder decides v1.1 ships without it, that decision is
  written into `.planning/` as an explicit waiver naming what is unproven and for how long. It is
  not a silent omission. Route as a founder question (Q-F2 below); it does not block anything the
  studio can do meanwhile.

### 2b. Also carried into the release branch from phase 13's sign-off

These are pre-branch developer/tech-lead work the sign-off made v1.1 conditions, not phase-13
conditions. The release-manager does not own them and does not implement them; they are listed so
the release branch is not cut over the top of them:

| Item | Owner | What |
|---|---|---|
| **FU-3 "Next level" defect** | android-game-dev | Serve `max(challenge_current_level, N + 1)` from `ResultsViewModel.onNextLevelTapped` using the awaitable `ChallengeProgress.settledLevel()`; `ResultsActions` owns no navigation arithmetic. Explicitly *"before the v1.1 release branch is cut"*. |
| **L-3 debug picker source-set split** | android-game-dev | Move `PlayGamesSnapshotPicker` into `app/src/debug/`; `main` provides a no-op `show`. *"Required before v1.1 ships."* |
| **FU-2 doc corrections** | android-game-dev / tech-lead | Six documented claims the artifacts do not support. *"Phase 14 re-reads these docs, and a release-readiness phase reasoning from folklore is how the last three days were spent."* |
| **`docs/tech-design.md`** | tech-lead | Does not exist; the tech-lead's own required output, owned and dated "before v1.1". |
| **Account-swap / D-14 UAT** | qa-engineer | Needs the founder's second account (checklist Step 4). Keep-local, replace-with-cloud, walk-away. |
| **`prefs.py` under `.planning/reports/phase-13/tools/`** | producer | `rules/testing.md` forbids a committed QA helper in a product repo. Move it out of `reports/` and record the exception, or delete it. |

### 2c. The branch, step by step

```sh
# from a clean develop with every item in 2a/2b landed
git checkout develop && git pull --ff-only
FUZZ_SEEDS=100000 ./scripts/check.sh           # release-settings gate (2a); the Check command
                                                # once feature/phase-14-check-gates lands
                                                # (docs/tech-design.md §16 item 9)
git checkout -b release/3.0.0 develop           # alone: the branch guard judges a one-line
                                                # `checkout -b … && git commit` before the switch

# VERSION already reads 3.0.0 — confirm rather than edit
cat VERSION                                     # -> 3.0.0
# app/build.gradle.kts: versionCode is already 30_000 on develop
# (feature/phase-14-version-code) — confirm rather than edit
# CHANGELOG.md: add the 3.0.0 section (section 3 below)
# docs/release-checklist.md: tick every studio-verifiable item
git commit -m "chore(release): 3.0.0"
```

Merge, tag and back-merge are unchanged from `rules/gitflow.md`. They are part of the release
step (the plugin's skills/release), so they happen **before** Gate 2: the founder approves a
tagged build, not a branch.

```sh
git checkout main && git merge --no-ff release/3.0.0
git tag v3.0.0
git checkout develop && git merge --no-ff main
git branch -d release/3.0.0
git tag --merged develop | grep -x v3.0.0       # must print v3.0.0
```

The tag is what the release is; `main` is only where it lives. Note for later: the approval file
`.planning/approvals/publish-3.0.0.approved` is merged to **develop** from
`feature/publish-3.0.0`. It is not on `main` and not on the tag. After that merge the producer
cuts `feature/publish-3.0.0-done` from develop (the plugin's skills/publish step 3). The
release-manager runs the gated upload script **from that branch's checkout** and writes the
upload report there, at `.planning/reports/publish-3.0.0/release-manager-upload.md`. The
producer commits it with the post-launch records and merges the branch (skills/publish step 5).
Nothing is run or written on develop itself. The publish hook blocks the upload on `main`, on the
tag and on any checkout without the approval file.

---

## 3. Changelog

`CHANGELOG.md` does not exist. It is created on the release branch with **two layers**, because
one document cannot serve both readers.

### The problem, sized

`git rev-list v1.0..develop --count` → **647 commits**, of which by conventional-commit type:
279 `docs`, 132 `feat`, 105 `test`, 72 `chore`, 35 `fix`, 5 `refactor`, 3 `style`, 1 `perf`,
1 `build`, plus 11 merges. A mechanical dump of 647 subjects is not a changelog; it is a log. A
mechanical dump of the 132 `feat` subjects is not one either — they are plan-scoped ("feat(09-04):
…"), they describe intermediate states, and several are superseded by later commits on the same
branch.

### Layer 1 — `CHANGELOG.md`, Keep-a-Changelog format, grouped by phase

One `## [3.0.0] — YYYY-MM-DD` section. Inside it, `### Added` / `### Changed` / `### Fixed`, and
within each, one bullet per **delivered capability**, not per commit — which for this milestone
means one to four bullets per phase, since each phase is one feature branch and one requirement
group. The raw material is generated, then edited down by hand:

```sh
git log v1.0..HEAD --no-merges --pretty='%h %s' \
  --grep='^feat' --grep='^fix' --grep='^perf' -E \
  > /tmp/changelog-raw.txt
```

Then: drop anything whose effect a later commit in the same range replaced; collapse the
`feat(NN-MM):` plan prefixes into the phase's capability; keep every `fix:` whose subject describes
behaviour a player could have observed and drop the ones that fix intermediate build state. The
phase `*-SUMMARY.md` files and `ROADMAP.md`'s success criteria are the authority for what each
phase actually delivered — they are written in outcome language already, which is what a changelog
wants.

Skeleton for the 3.0.0 section, derived from ROADMAP phases 6–14 (bullets to be filled from the
generated list, not invented):

- **Added** — sound for every meaningful action (phase 7); Play Games sign-in, 7 leaderboards and
  20 achievements (phases 11–12); cloud save across devices (phase 13); rewarded hints/undo and
  between-level interstitials with a consent flow (phases 9–10); an always-present privacy-policy
  entry in Settings (phase 9, REL-05).
- **Changed** — hints now answer from a committed opening book before falling back to the solver
  (phase 8); per-board star bands derived from each board's own minimum peg count (phase 8);
  release builds minified and resource-shrunk (phase 6).
- **Fixed** — Diamond-41's star bands now reflect its proven two-peg floor rather than an
  unreachable one-peg target (phase 8, D-06); the landed-peg settle animation no longer strands a
  peg at squash scale (phase 6, `d2555c1`).

The earlier `v1.0` range is **not** back-filled. The changelog starts at 3.0.0 and says so in a
one-line preamble: earlier history is in `.planning/MILESTONES.md`. Inventing a retrospective
1.0.0 section from 66 plans of pre-studio history would be archaeology with a low truth ratio.

### Layer 2 — Play release notes, ≤ 500 characters

A separate artifact in `docs/store/release-notes-en-GB.txt`, written for a player, not a developer.
Play enforces 500 characters per language and truncates silently. Four lines is the right shape:
sound, cloud save + leaderboards, better hints, and a "what's new in this rewrite" line. The
studio drafts it; the founder approves the wording as taste (Q-F4).

**Both layers are checked against each other:** every claim in the 500-character notes must map to
a bullet in `CHANGELOG.md`. A store note describing a feature the changelog does not is how a
listing ends up promising something that is not there.

---

## 4. Signed build procedure

### 4a. What is missing from the build, and where each piece is added

1. **A `signingConfig`.** *Done 2026-10-17: merged to `develop` as
   `feature/phase-14-signing-config` (9ca3829), with the `verifyReleaseSigning` backstop. The
   text below is kept as the specification it was built from.* None existed. D-16 deferred it here deliberately, so that no
   debug-keystore shortcut could survive to a store upload. It is added to
   `app/build.gradle.kts` reading the four environment variables the studio's Play rules name,
   and **failing the release build outright when any is absent**, in the same fail-fast register
   as `verifyReleaseAdIds`:

   - `PLAY_UPLOAD_KEYSTORE`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`,
     `PLAY_UPLOAD_STORE_PASSWORD` — read via `providers.environmentVariable(...)`, never from
     `local.properties` and never from a committed file. A missing variable must fail; it must
     **not** silently fall back to the debug keystore or to an unsigned artifact, because
     `assembleRelease` currently succeeds unsigned and a "successful" unsigned build is precisely
     what gets mistaken for a release candidate.
   - `local.properties.template` gains a comment block naming the four variables and stating that
     they are environment-only — unlike the ad and Play Games keys, these *are* cryptographic
     secrets and must never appear in `local.properties`.

2. **`scripts/play-upload.sh`.** On `develop` since merge `1dd1080` (`feature/play-upload`,
   `docs/tech-design.md` §14.1 PU), reviewed by the code-auditor and QA and signed off by the
   tech-lead (`.planning/reports/play-upload/`). *Updated 2026-11-01 (T-9): this item earlier said
   the repo had no `scripts/` directory and that the script was still owed.* It was never created on
   the release branch: it is code, and the release branch takes no code (`docs/release-checklist.md`
   §2). The as-built contract and the script-route rules (T-1, T-3, T-6, T-7, T-11, TL-1) are in the
   note under §4 of `docs/release-checklist.md`; the producer records which route this release uses
   (script or the founder's Console upload, `14-OPERATOR-CHECKLIST.md` Step 8 item 1) under item 1.7.
   Its specification: `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON`, uploading the signed
   AAB of `:app:bundleRelease` to the `internal` track, failing with a message when the variable or
   the AAB is missing, and echoing no secret. **It checks no approval itself.** The gate is the
   plugin hook `hooks/publish-gate.sh`, which blocks any command that runs `play-upload.sh` or
   `fastlane supply` unless `.planning/approvals/publish-<VERSION>.approved` exists (plugin
   `rules/stacks.md` 1.0.0, scaffold contract 9: "it checks no approval itself
   (`hooks/publish-gate.sh` does)"; `rules/play-store.md`: "the publish hook blocks it").
   *Corrected 2026-10-21 (pass 3): this item earlier asked for "a hard refusal to run unless the
   approval file exists — belt as well as the plugin hook's braces". That contradicts the plugin
   rule, and §14.1 PU and §17 of `docs/tech-design.md` record the supersession.*

3. **`docs/release-checklist.md`.** Also missing. Created from the studio's Play rules with every
   item marked studio-verifiable or founder-only, so that "unticked founder-only item at publish
   time" is a detectable state rather than a discovery.

4. **`docs/store/`.** Also missing. Title (≤ 30), short description (≤ 80), full description,
   the content-rating and Data-safety answers as *drafted from the policy* for the founder to
   enter, the policy URL, and the release notes from section 3.

5. **The artifact format is an AAB, not an APK.** `CLAUDE.md`'s Build command is
   `./gradlew :app:assembleRelease`, which produces an APK; Play's upload path for an app enrolling
   in Play App Signing is the **App Bundle**. `bundleRelease` already carries the same
   `verifyReleaseAdIds` / `verifyReleaseGamesIds` dependency (`app/build.gradle.kts` wires both
   task names), so nothing else changes. Whether this listing — published before the 2021 bundle
   requirement — would still accept an APK is **unverified and irrelevant**: the AAB is the right
   artifact either way. `CLAUDE.md`'s Build command line is updated on the release branch to name
   `:app:bundleRelease` for the release artifact. *(Note 2026-11-01: for 3.0.0 the change landed
   before the cut, on `develop` with `feature/release-3.0.0-prep`, together with `AGENTS.md`; the
   release branch only reads it, checklist 2.6.)*

### 4b. The procedure

Preconditions, all founder-side, all from `14-OPERATOR-CHECKLIST.md`: Step 6 (real ad + PGS IDs in
`local.properties`) and Step 7 (keystore + four environment variables). Without both, this section
cannot be executed at all — not partially, not with substitutes. Test ad-unit IDs are **not** a
substitute: phase 13's gate evidence already recorded a test-ID release build and recorded, in the
same breath, that it *"is not a release-candidate artifact and must not be uploaded"*.

```sh
# 0. one warm online run has to have populated the Gradle cache with
#    org.jetbrains.kotlin:compose-group-mapping (D-18) — release builds are not
#    offline-safe on a cold cache; `check` is.
git checkout release/3.0.0

# 1. release-settings gate, at the release fuzz count
FUZZ_SEEDS=100000 ./gradlew check

# 2. the signed bundle
./gradlew :app:bundleRelease --max-workers=2

# 3. prove it is signed with the upload certificate, not the debug key
#    (bundletool, or jarsigner on the AAB's signature block)
jarsigner -verify -verbose:summary -certs app/build/outputs/bundle/release/app-release.aab \
  | grep -i 'CN=\|jar verified'
#    the SHA-1 must equal Play Console -> App integrity -> Upload key certificate

# 4. R8 evidence, the phase-13 exit gate repeated at release settings
ls app/build/outputs/mapping/release/missing_rules.txt   # expect: absent
grep -c 'com.google.android.gms.games' \
  app/build/outputs/mapping/release/mapping.txt          # expect: non-zero (kept, not stripped)

# 5. record the artifact identity in the release report
shasum -a 256 app/build/outputs/bundle/release/app-release.aab
unzip -p app/build/outputs/bundle/release/app-release.aab base/manifest/AndroidManifest.xml \
  | strings | grep -m1 '3\.0\.0'
```

Then a **one-screen founder summary**: version, changelog, checklist state, store-asset state, and
the exact upload command (T-6: `docs/release-checklist.md` item 4.8 and the note under its §4) — and stop. The upload command is run by the founder's approval, never by
the studio on its own initiative.

### 4c. What the studio must not do

- Not build a release artifact with Google's test ad-unit IDs and call it a candidate.
- Not sign with the debug keystore "to test the pipeline". Phase 6 did exactly that for a smoke
  test and was careful to record *why* it was safe there (`apksigner` after the fact, no committed
  `signingConfig`, test IDs removed immediately); a release candidate has no such licence.
- Not run `scripts/play-upload.sh` without `.planning/approvals/publish-3.0.0.approved`, and not
  create that file.
- Not touch `main` before the founder summary is written and Gate 2 answered.

---

## 5. Internal-testing-track verification script (ROADMAP success criterion 1)

The criterion excludes the easy path on purpose: *every feature re-verified from a signed
internal-testing-track **download**, not an `adb install` debug build.* The reason is certificate
identity — a locally built artifact carries the upload/debug certificate, the downloaded one
carries the **app signing** certificate, and only the second is what a real player's Play Games
sign-in is checked against. This is the single failure mode the whole phase exists to catch.

**Executed by:** qa-engineer, on a device or emulator signed in as a tester.
**Prerequisite:** checklist Step 8 complete.
**Artifact:** results recorded in `.planning/phases/14-release-readiness/14-HUMAN-UAT.md` with an
observed `result:` line per test — never a pre-ticked box.

### T-0 — Prove the build under test is the store copy (gate for everything below)

```sh
adb shell pm list packages -i com.erman.pegsolitarie      # installer must be com.android.vending
adb shell dumpsys package com.erman.pegsolitarie | grep -E 'versionCode|versionName|installerPackageName'
```

**Pass:** `installerPackageName=com.android.vending`, `versionCode=30000`, `versionName=3.0.0`.
**If this fails, stop.** Every result below is void — you are testing a locally signed build and
the criterion is not merely unmet, it is untested. This is not a formality: it is the one check
that distinguishes this phase's verification from every device pass phases 9–13 already did.

### T-1 — Play Games sign-in on the app signing certificate

Cold launch. **Pass:** the player is signed in (the welcome/greeting surface appears, or the
in-app session state shows signed-in) with no `ERROR_NOT_AUTHORIZED` and no silent
`isAuthenticated() == false`. **This is the certificate test**: if T-1 fails while the same
account signs in fine on an `installDebug` build, the cause is checklist Step 3 item 2 — the app
signing SHA-1 has no PGS credential — and not a line of Kotlin.

### T-2 — Leaderboards, from the download

Solve one Classic board and one Challenge level. **Pass:** the stock leaderboard UI opens and both
scores appear on the right ladders (Challenge Ladder and the board's own board-score ladder).
Submissions that go nowhere present as success in-app, so the *server-side* view is the oracle,
not the absence of an error.

### T-3 — Achievements and the unlock popup

Solve a first level. **Pass:** the `FIRST_SOLVE` popup renders (it silently fails to render if the
Console icon is missing — phase 12 Step 3) and the achievement shows unlocked in the stock UI.
Check one incremental tier too (`LADDER_10`), because incremental step totals are immutable after
publication and a Console/app mismatch shows up only at runtime.

### T-4 — Cloud save across devices

With the second tester device, solve different levels on each, relaunch both. **Pass:** the union
appears on both. The phase-13 record is explicit about the oracle: `change_pending == false`
**together with** `last_modified_at` equal to *this device's own* last commit. `change_pending`
alone is not an upload oracle — a fresh local commit makes it true about a newer commit than the
one whose content already landed.

### T-5 — Cloud save visible in Play Games (CLOUD-03)

Use the debug snapshot picker if it is still reachable in this build — **it is not**, if the L-3
source-set split in §2b landed, which is the intended state. So on a release build the check is
checklist Step 10's Drive route: drive.google.com → Settings → Manage apps → hidden app data size
> 0. **Record which oracle was used**; a release-build CLOUD-03 verification that silently used a
debug-only surface would be evidence about the wrong artifact.

### T-6 — Consent flow, on a real ad configuration

Fresh install, consent region. **Pass:** the UMP form appears **before** Home and before any ad
request; no `GoogleMobileAds*` activity in logcat before consent resolves; Settings shows both the
always-present policy row and the region-gated "Manage privacy choices" row. On this build the ad
units are **real**, unlike every prior device pass — which is why T-6 belongs here and not in
phase 9's evidence.

### T-7 — Rewarded ad, real unit

Request a hint past the free allowance. **Pass:** a real rewarded ad loads and plays, the reward
is granted on completion, and dismissing it early grants nothing. **Record the ad-unit behaviour,
not just the grant**: a test unit always fills and a real one does not, so a no-fill is a valid
observation and the fail-open path (`RewardOutcome.Unavailable`) must be observed too — hint and
undo must still work when the ad does not.

### T-8 — Interstitial, real unit

Finish a Challenge level and tap continue. **Pass:** the interstitial appears **after** the
continue tap, never during the win celebration and never at level start; losing, abandoning or
restarting shows none; the game clock is paused while it is up (ADSF-01).

### T-9 — Sound, on the store build

**Pass:** jump, removal, selection, undo, invalid tap, win and the rising star-pop all play at
system media volume; music or a podcast keeps playing unducked; the `soundOn` switch silences
immediately. This is a **release-build** check specifically: the 12 `res/raw` files must survive
`shrinkResources`, which phase 7 proved on a debug-signed release APK and nobody has proved on the
store copy.

### T-10 — Hint and undo, release build

**Pass:** a hint from a hard early position on European-37, Asymmetric-39 and Wiegleb-45 returns a
real winning move inside the 1.5 s deadline rather than the rescue banner. This exercises the
committed opening-book resources through R8 and `shrinkResources` — the book is a classpath
resource lookup with a fail-closed parse, and a shrunk-away book presents as "the hints got worse",
not as an error.

### T-11 — Resume, and the bundled fonts

Background mid-game, kill, relaunch, Continue. **Pass:** the exact board returns with undo intact
and elapsed time carried over. While you are there: **Fredoka/Nunito render** rather than the
default font. 3.0.0 **bundles** them as five static files in `res/font`
(`feature/phase-15-ux-refresh`; the downloadable-font provider and `font_certs.xml` are gone), so
the check is that R8's resource shrinking kept all five. Release checklist 1.4 confirms the AAB
carries the five fonts and both `assets/licenses/*-OFL.txt` files; this is the on-device half.

### T-12 — Account swap / D-14 wipe (needs checklist Step 4)

Sign in as the second account on a device holding the first account's progress. **Pass:** the
profile-swap dialog appears; **keep-local** preserves local progress; **replace-with-cloud**
replaces it; **every** walk-away route — system back, scrim tap, recents — routes to keep-local
and destroys nothing. This is the one destructive path in the milestone and it has never run on a
device; run it last, and on a device whose state you are willing to lose.

### T-13 — Cold start on the store build

See `14-COLD-START.md`. Recorded here as a numbered test so the UAT sheet is complete, but the
comparand measurement is the debug one and is not this artifact.

**Coverage against criterion 1:** Play Games sign-in (T-1), leaderboards (T-2), achievements (T-3),
cloud save (T-4, T-5), both ad formats (T-7, T-8) — the five the criterion names, plus the six the
studio adds because a signed, minified store build is the first artifact on which any of them has
been observed.

---

## 6. `THIRD_PARTY.md`

**Update 2026-10-17:** the file now exists on `feature/phase-14-release-docs` and is the
authority. It covers the 12 WAV cues (10 CC0 + 2 in-house), the five bundled OFL font files, the
dependency families, and the store-art rows (§7, own work, closed by Q-017). The inventory below
is kept as the plan's record, corrected for the 3.0.0 asset set.

When this plan was written, **it did not exist anywhere in the tree** — confirmed independently by the phase-13 auditor
(`getconflict` F-7), by the tech-lead sign-off (`ls THIRD_PARTY.md` → missing) and again here. The
studio's IP policy calls it mandatory and the release rules block a release when any row lacks a
license, so this is a release blocker in the literal sense, not a documentation nicety.

Written on the release branch, one row per borrowed idea, asset or snippet, with source URL,
license, what was taken, and how it is credited. The inventory, from reading the repo:

### Ideas and mechanics (no license needed; recorded because ip-policy says record it anyway)

| Item | Source | License | Credit |
|---|---|---|---|
| Peg solitaire itself | traditional, documented from the 17th century | not protected — ideas and mechanics are not copyrightable | — |
| The five Classic board layouts (English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45) | historical published board geometries; Wiegleb's 45-hole cross dates from 1779 | board *geometry* is an idea, not an expression | named in-game as board names |
| Rules and board fidelity reference | the founder's own earlier peg-solitaire repository | own work | — |

### Assets shipped in the APK

| Item | Source | License | Credit |
|---|---|---|---|
| 7 of 12 sound effects (WAV) | Kenney, *Interface Sounds* 1.0 | **CC0 1.0** | `docs/audio/CREDITS.md`, per-file table |
| `sfx_star_pop.wav` | freesound sound 531512 by CogFireStudios | **CC0 1.0** | same |
| `sfx_select.wav` + derived `sfx_deselect.wav` | freesound sound 506546 by LilMati | **CC0 1.0** | same |
| `sfx_invalid.wav`, `sfx_hint.wav` | rendered in-house from `audio/recipes/*.json` | own work | — |
| Fredoka (2 files), Nunito (3 files) | Google Fonts static instances, **bundled** in `res/font` with their OFL texts in `assets/licenses/` | **OFL-1.1** | the stand-alone OFL files in the artifact (OFL §2) |
| Launcher icon, cloud-save cover | drawn in this repo as vector XML | own work | — |
| Opening books, recorded solution fixtures, Diamond-41 floor line | authored in-house in phase 8 by beam search | own work | — |

`docs/audio/CREDITS.md` already carries the twelve-row provenance table and the reasoning for why
CC0 makes attribution a courtesy rather than an obligation. `THIRD_PARTY.md` **references** it
rather than duplicating it; two copies of an asset table drift.

### Code dependencies

One row per coordinate in `gradle/libs.versions.toml`, with the license verified from the artifact
rather than assumed. Three classes need calling out rather than a blanket "Apache-2.0":

1. **Apache-2.0** — Kotlin, AndroidX (Compose, Room, DataStore, Navigation, Lifecycle, Core),
   Hilt/Dagger, KSP, kotlinx-coroutines, kotlinx-serialization, Truth, MockK, Turbine, ArchUnit,
   Robolectric. Notice retained; no copy obligation beyond attribution.
2. **Not open source** — `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`,
   `com.google.android.ump:user-messaging-platform`, `com.google.android.gms:play-services-games-v2`,
   `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`. These ship under the **Android
   Software Development Kit License Agreement / Google APIs Terms**, not an OSI license. They are
   permitted, and they are **not** Apache-2.0 — recording them as Apache-2.0 would be a false row
   in the exact document the release gate reads. Row text: proprietary, redistribution permitted
   by its own terms as part of an application.
3. **EPL-1.0, test scope only** — JUnit 4 (`junit:junit:4.13.2`). EPL is not in the ip-policy
   matrix. It does not need an escalation: the artifact is `testImplementation`, is not in the
   APK, and is not distributed, so no source or notice obligation attaches to anything that
   ships. Record the scope in the row so the next reader does not have to re-derive it.

A `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` listing is the authority
for what actually ships, and the row set is checked against it so a transitive coordinate cannot
be missing.

### One row the studio cannot fill

The **20 achievement icons**, the listing's **512×512 icon** and the **1024×500 feature graphic**
exist only in the Play Console. Nothing in this repo records where they came from or under what
license — there is no raster image anywhere in `app/src/main/res`. Those rows are opened with an
explicit `license: unknown — pending founder` marker and **the release is blocked until they are
filled**, per ip-policy's "the release skill blocks a release if a row lacks a license". That is
checklist Step 9 and founder question Q-F3.

**Closed 2026-10-17:** the founder answered Q-017 (Q-F3): *"you did it"*, studio-produced. The
rows are own work, each file identified by its sha256, in `THIRD_PARTY.md` §7 on
`feature/phase-14-release-docs`. The 7 leaderboard icons are recorded there too.

---

## 7. What is blocked, and what the studio does meanwhile

### Cannot be done at all until a founder item lands

| Blocked | Waiting on |
|---|---|
| Any signed release artifact (AAB or APK) | Checklist Step 6 (real ad IDs) **and** Step 7 (keystore). `verifyReleaseAdIds` throws at configuration time; no signing config can run without the four environment variables. |
| Upload to any track | Step 6, 7, 8 **and** `.planning/approvals/publish-3.0.0.approved`. |
| REL-02 / criterion 1 in full | Step 8 — a store-installed build. No local build can substitute; that is the criterion's entire content. |
| REL-03 verification | Step 0 — the crawl and the AdMob status. |
| REL-01, REL-04 | Steps 2 and 5. Play Console and the website are founder-only. |
| PLAT-08 on the **release** variant | Step 6/7 (needs a real release build). The **debug-to-debug** comparison, which is the valid one, needs nothing — see `14-COLD-START.md`. |
| D-14 account-swap UAT | Step 4 — a second Google account. |
| `THIRD_PARTY.md` completeness | Step 9 — store-art provenance. |

### Can start immediately, in parallel, with no founder input

1. **The fuzz harness and the coverage gate** (§2a) — the largest item and the one with the most
   value per hour, since it closes a studio-rule violation *and* tests the product's core claim.
2. **The FU-3 "Next level" fix, the L-3 source-set split, FU-2's doc corrections,
   `docs/tech-design.md`** (§2b) — all pre-branch, all owned, none founder-dependent.
3. **`THIRD_PARTY.md`** — every row except the three store-art ones.
4. **`CHANGELOG.md`** — the whole 3.0.0 section; only the 500-character store note needs approval.
5. **The signing config, `scripts/play-upload.sh`, `docs/release-checklist.md`, `docs/store/`
   texts** — written and reviewed now; they simply cannot *execute* until Step 6/7.
6. **The Data-safety and content-rating answers, drafted** into `docs/store/` from the policy the
   founder revises — the founder enters them, the studio drafts them, and the drafting can start
   the moment Step 2's revision is published.
7. **The cold-start measurement** (`14-COLD-START.md`) — the comparand is a *debug* build and needs
   no ad IDs and no keystore. Runnable today, and there is a sequencing reason to run it today.
8. **`14-HUMAN-UAT.md`** — the T-0..T-13 sheet above, written out with empty `result:` lines so the
   session is a recording exercise rather than a design exercise.

---

## Founder questions

To be pasted into `.planning/questions/FOUNDER.md` by the producer. None of them blocks the studio
work listed in §7; Q-F1 blocks cutting the release branch.

### Q-F1 — Ship milestone v1.1 as product version 3.0.0, with versionCode 30000
**Context:** Two numbering systems disagree: `.planning/` calls this milestone v1.1, while the Play
listing sits at versionCode 2 / versionName "2.0" from your earlier app. Milestone v1.0 of this
rewrite was tagged but never uploaded, so this is the store's first sight of this codebase.
**Options:** 1) `VERSION` stays **3.0.0**, tag `v3.0.0`, versionCode **30000** (studio formula
`X*10000+Y*100+Z`) 2) rename the release to 1.1.0 to match the milestone label 3) something else
you have in mind for the store-facing number.
**Recommendation:** release-manager picks 1. "1.1.0" after a published "2.0" reads to a player as a
downgrade; 3.0.0 was already decided on 2026-08-27 and is written into the build; a complete
rewrite is what a major bump is for. versionCode 30000 is a one-way door — it can never go back
down — but it is the studio rule and adopting it now costs nothing.
**Blocks:** cutting `release/3.0.0`.

### Q-F2 — v1.1 ships without a fuzz harness, or waits for one
**Context:** This repo's `check` has no coverage gate and no fuzz harness, contrary to
rules/kotlin-android.md and rules/testing.md — inherited from the pre-studio phases and raised by
the phase-13 tech-lead. For a game whose core value is "always-solvable", that means solvability is
proven today by five frozen golden-master levels and a depth sweep, and has never been tested at
the 100,000-seed scale the release rule requires.
**Options:** 1) build the harness and the kover gate before cutting the release branch (est. one
developer phase; every piece it needs already exists in `:domain`) 2) ship v1.1 with a written,
dated waiver naming exactly what is unproven, and build it in v1.2.
**Recommendation:** release-manager picks 1. It closes a studio-rule violation and tests the
product's central claim in the same task, and it is wiring rather than new algorithms.
**Blocks:** nothing the studio can do meanwhile; it does block `release/3.0.0`.

### Q-F3 — Provenance and license of the store artwork
**Context:** `THIRD_PARTY.md` must exist before release and the release is blocked if any row lacks
a license. Everything in the APK has recorded provenance; the 20 achievement icons, the 512×512
listing icon and the 1024×500 feature graphic exist only in the Play Console and nothing in this
repo says where they came from.
**Options:** for each of the three groups — 1) you drew them 2) generated with a named tool
3) bought/downloaded from a named source under a named license 4) taken from a URL.
**Recommendation:** release-manager needs the answers, not a decision. Reply per group; the studio
writes the rows. Do not paste URLs or account details into this file.
**Blocks:** `THIRD_PARTY.md` completeness, and therefore the release gate.

### Q-F4 — Store listing: does it describe the app that is shipping, and who writes the 500 characters
**Context:** The listing was written for a different application (versionCode 2). Play policy
requires screenshots to represent the app. The studio has never seen the current listing and cannot
know. Separately, the Play release notes are capped at 500 characters and are taste, not
engineering.
**Options:** 1) listing is current, studio drafts the release notes for your approval 2) listing
needs new copy and/or screenshots — studio drafts copy and captures screenshots from the build once
it exists, you approve 3) you will rewrite the copy yourself.
**Recommendation:** release-manager picks 2 for the screenshots specifically — a rewrite this
complete will not match old captures — and asks you to approve the 500-character note either way.
**Blocks:** nothing; it is a Play policy exposure rather than a mechanical block.

### Q-F5 — Target audience declaration
**Context:** Play's target-audience declaration is not a formality: declaring any audience under 13
puts the app under the Families policy, which constrains which ad SDKs and ad content are
permitted. This build's ad stack (GMA Next-Gen + UMP) was not designed or tested against those
constraints.
**Options:** 1) declare **13+** 2) declare an under-13 audience, accepting that the ad
configuration then needs re-engineering and re-testing before release.
**Recommendation:** release-manager picks 1. A peg-solitaire game is broadly appealing, but
option 2 is an engineering decision disguised as a form field and would reopen phases 9 and 10.
**Blocks:** checklist Step 5 (Data safety / App content).
