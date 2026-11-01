# release-manager report — phase-15 sequencing and release impact ruling
Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (2397732..HEAD — cut from develop at 2397732; this report is the branch's only commit, `docs(15): rule on phase 15 sequencing and release impact`)
Skills loaded: ergoz-soft:standards (rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md — plugin copies read and byte-identical to the repo's v0.5.1 snapshot)
Ran in parallel with: phase-14 (release readiness, `feature/phase-14-release-plan` and friends, other worktrees)
Loops: 0
Lessons applied: none

## Summary
A ruling only — no code, no store assets, no version file touched. Recommendation:
Phase 15 lands **before** the `release/3.0.0` cut and ships inside 3.0.0, because the
release's critical path is founder-side and not started, and because deferring the
refresh pays the project's most expensive verification (a signed track-download pass)
and the store-screenshot capture-and-approve loop **twice** instead of once. One
checkable flip condition is given so nothing blocks. `docs/store/` does not exist on
any branch: no screenshot is invalidated because none has ever been captured.

## Outputs
- `.planning/reports/phase-15/release-manager-sequencing.md` — this ruling (the only file this task writes)

---

## 0. The situation as the repo records it

Established by reading, not assumed:

| Fact | Evidence |
|---|---|
| `VERSION` = `3.0.0` | `cat VERSION` in this worktree |
| `versionCode = 30_000`, `versionName = "3.0.0"` | `app/build.gradle.kts:280-281`, on `develop` |
| The 3.0.0 → 30000 encoding is already merged to `develop` | `08b1b27 chore: encode the version name in versionCode (3.0.0 -> 30000)`, reachable from `develop` tip `2397732` |
| Only one tag exists, `v1.0` | `git tag -l` |
| Highest **published** versionCode is `2` (`versionName "2.0"`), and it belongs to a *different* application — the founder's earlier peg-solitaire app | `14-OPERATOR-CHECKLIST.md` Step 1; `14-RELEASE-PLAN.md` §1; `app/build.gradle.kts:269` comment |
| `docs/store/` does not exist on `develop`, `main`, or any of the five in-flight `feature/phase-14-*` branches | `git ls-tree -r --name-only <branch> -- docs/store/` → empty on all six |
| No screenshot-test baselines exist (no Roborazzi, no `screenshotTest`, no Paparazzi) | `grep -rn 'roborazzi\|screenshotTest\|paparazzi' gradle/libs.versions.toml app/build.gradle.kts build-logic/` → 0 hits |
| `.planning/questions/FOUNDER.md` holds Q-001, Q-002, Q-CONCEPT — all answered. **Q-F1..Q-F5 from the phase-14 release plan have not been filed yet** | the file; `14-RELEASE-PLAN.md` "to be pasted into FOUNDER.md by the producer" |
| The v1.1 cut is already blocked by an ordered blocker list R-1..R-7, of which R-2 (no fuzz harness, no coverage gate) is the Core-Value gate | `.planning/reports/STATUS.md` line dated 2026-09-21; `14-RELEASE-PLAN.md` §2a |
| The longest-lead external prerequisite — **a developer website** — does not exist, and `app-ads.txt`, the privacy policy, REL-01 and REL-04 all hang off it | `.planning/ROADMAP.md` External Prerequisites table; `14-OPERATOR-CHECKLIST.md` Steps 0, 2, 5 |

The five surfaces Phase 15 touches, located in code so the blast radius is a fact and
not a guess:

- **Interactive tutorial** — replaces/extends `ui/onboarding/OnboardingOverlay.kt`, hosted
  over `HomeScreen` body and gated by `data/OnboardingRepository.kt` (`HomeScreen.kt:175`).
- **Info button removal** — `Icons.Rounded.Info` in the Home top bar (`HomeScreen.kt:350`),
  the "How-to-play" affordance, D-15.
- **Fuller home screen** — `ui/home/HomeScreen.kt` body: up to two gold Continue chips
  (PERS-03, D-01/D-03) plus the two `ModeTile`s, and the top bar that also carries the
  Settings gear. This file also hosts `PlayGamesPromptDialog` and `ProfileSwapDialog`.
- **Redesigned in-game top icons** — `GameTopBar` in `GameScreen.kt:490-524`: back nav,
  title, live `mm:ss` timer (D-04), `UndoControl`, `HintControl`, `HintToggleControl`,
  `RestartControl`.
- **In-game toolbar removal** — the same `GameTopBar` action row, and/or the
  Undo · Restart · Home rescue row in `NoWinBanner` (D-09). **Which of the two the
  founder means is a ux-designer scope question, not a release one** — both readings land
  on the same release impact below, because both are on the game screen.

---

## 1. Order — before or after the v1.1 release branch

### The bill for landing Phase 15 *before* the cut

1. **Calendar delay to the release, measured against the real critical path — small.**
   The cut cannot happen today for reasons Phase 15 does not touch: Q-F1 (the version
   decision) is not even filed yet; R-2 (fuzz harness + `koverVerify`) is unbuilt and
   `rules/gitflow.md` makes a release-settings Check a precondition of the branch; and the
   founder's Steps 0/2/5/6/7 — developer website, `app-ads.txt`, privacy policy, Console
   declarations, real ad IDs, keystore — have not started, with the website named in the
   ROADMAP as *"the longest-lead item; nothing else here can start until it does"*. Phase 15
   is UI work in `app/src/main/.../ui/`; R-2 is `:domain` work; the founder's items are not
   engineering at all. On this project's own velocity (Phase 7 = 6 plans, Phase 12 = 8 plans,
   each days not weeks) a 5-part UX refresh is plausibly 6–10 plans in a parallel worktree,
   and it is very unlikely to be the last of those three tracks to finish.
2. **Cost that is real regardless: the Check command goes red until the tests are updated.**
   `app/src/test/.../ui/home/HomeScreenTest.kt`, `PlayGamesPromptDialogTest.kt`,
   `ProfileSwapDialogTest.kt`, `ui/game/GameScreenSoundTest.kt`, `GameScreenAdOfferTest.kt`,
   `HintControlTest.kt`, `NoWinBannerTest.kt` all assert on the surfaces being redesigned.
   Phase 15 must land with them updated and with a Compose UI test for the new tutorial flow
   (rules/compose.md, rules/testing.md). That is a developer/QA bill inside Phase 15, not a
   release bill — but release-checklist item 1.2 (Check green at release settings on the
   merged tree) cannot be ticked while it is outstanding, so it is on the cut's path.
3. **The discipline cost, which is the one that actually worries me.** A release-readiness
   phase with 7 named blockers, 5 unfiled founder questions and an unstarted founder track
   now acquires a fifth scope item. Accepting new product scope into a release window is how
   a release slips without anyone deciding to slip it. The mitigation is the flip condition
   in §1.3, not optimism.
4. **A Gate-2 packet that has to be re-authored rather than reused** — changelog bullets,
   the 500-character store note and the one-screen founder summary all describe the refreshed
   UX. Hours, not days, and only once.

### The bill for landing Phase 15 *after* the cut (first work of 3.1.0)

1. **The track-download pass is paid twice, not avoided.** Release-checklist §5 (5.0–5.10)
   is per-artifact by construction: *"a locally built artifact carries the upload
   certificate; the one a tester downloads carries the app signing certificate"*. 3.1.0 needs
   its own §5 pass whatever 3.0.0 did. So deferring does not save a pass — it adds one. This
   is the single largest number in the whole comparison: §5 needs a founder-registered test
   device (≥ 24 h of AdMob propagation before the run, per `docs/ad-testing-safety.md` §3), a
   second Google account that does not yet exist, two devices, an internal-track upload and
   a founder approval gate. It is the most expensive verification in the project.
2. **The store listing is captured, approved and uploaded twice.** No screenshots exist
   (§0). Capturing ≥ 4 phone screenshots for 3.0.0 and replacing all of them at 3.1.0 costs
   two capture rounds, two founder approval loops, and two Console edits — and Play policy
   exposure in between, since screenshots must represent the app.
3. **The store's first sight of this codebase is a UX the founder has already decided to
   change.** First impressions, the first reviews and the first screenshots would be of the
   pre-refresh Home and in-game chrome. That is a taste and marketing cost, and by
   rules/studio.md question routing it is the founder's call, not mine.
4. **The genuine benefit, stated fairly:** it decouples *"does Play work"* from *"does the
   new UX work"*. The 3.0.0 §5 pass has never happened and has the most unknowns in the
   project — certificate identity, real ad units, a second account, an internal track. Running
   it against the current, device-proven UI is the lower-variance run: a failure teaches you
   about certificates and Console configuration rather than about a brand-new tutorial. If
   the founder's priority is *"get one build through Play end to end as soon as possible"*,
   that argument wins and this ruling should flip.

### 1.3 Ruling

**Phase 15 lands before `release/3.0.0` is cut, and ships inside 3.0.0** — but the sharper
form of the ruling is that "before or after the release" is the wrong axis, because Phase 14
is not one thing:

| Phase 14 track | Relation to Phase 15 |
|---|---|
| Founder-side Steps 0–7 (website, `app-ads.txt`, policy, declarations, keystore, ad IDs, track) | **Parallel.** Zero coupling. Start now; do not wait for Phase 15. |
| R-2 fuzz harness + coverage gate, and the rest of R-1..R-7 | **Parallel.** `:domain` and build-logic work; separate worktree. |
| Artifact preparation already merged or in flight — signing config, `CHANGELOG.md`, `THIRD_PARTY.md`, `docs/release-checklist.md`, `docs/tech-design.md`, `scripts/play-upload.sh` | **Parallel.** Text and build config; Phase 15 adds changelog bullets at the end, nothing more. |
| **UI-dependent verification** — the §5 track-download pass, PLAT-08 C1/C2/C3, `docs/store/` screenshots, the `14-HUMAN-UAT.md` T-0..T-13 sheet | **Gated by Phase 15.** These are the only items that must wait, and they are exactly the items that would otherwise be run twice. |

So Phase 15 gates roughly one third of Phase 14 and blocks none of the long-lead two thirds.
Two conditions on the ruling:

- **Phase 15 never enters the release branch.** `docs/release-checklist.md` §2: *"A release
  branch is not where a feature gets finished."* Phase 15 merges to `develop` through
  `feature/phase-15-ux-refresh` with the usual gates (Check green, code-auditor PASS,
  qa-engineer PASS, tech-lead sign-off), and only then is `release/3.0.0` cut. If a Phase 15
  defect surfaces after the cut, the branch is re-cut; it is not patched in place.
- **Flip condition, so nothing blocks.** If every founder prerequisite (Steps 0–7) and R-2
  are complete while Phase 15 is **not yet code-complete and merged**, the ruling flips:
  cut `release/3.0.0` without the refresh and make Phase 15 the first work of 3.1.0. A ready
  release is not held for UI work. The producer can evaluate this from two observable
  states — `.planning/reports/STATUS.md` (are R-1..R-7 closed?) and the operator checklist's
  ticked boxes — without asking me again.

---

## 2. Store listing — what exists, and when screenshots get captured

### What exists (established, not assumed)

- **`docs/store/` does not exist.** Not on `develop`, not on `main`, not on
  `feature/phase-14-release-plan`, `-release-docs`, `-gate-scope`, `-prerelease-fixes` or
  `-kickoff`. `feature/phase-14-release-docs` adds `CHANGELOG.md`, `THIRD_PARTY.md`,
  `docs/release-checklist.md`, `docs/ad-testing-safety.md` and `docs/tech-design.md` — and no
  `docs/store/`. The directory is *planned* (`14-RELEASE-PLAN.md` §4a item 4) and not written.
- **Therefore zero screenshots exist in this repo, and no screenshot-test golden exists
  either** (no Roborazzi, no `screenshotTest` source set). There is nothing in the tree for
  Phase 15 to invalidate.
- **The live listing's screenshots are already invalid, independently of Phase 15.** The repo
  records the listing as belonging to the versionCode-2 application — *"a different
  application: different UI, different mode structure, no Challenge ladder, no leaderboards,
  no sound"* (`14-OPERATOR-CHECKLIST.md` Step 1) — and Q-F4 records that the studio has never
  seen it. Play policy requires screenshots to represent the app. They were dead before this
  refresh was proposed.

### Ruling

**Phase 15 invalidates nothing that exists, and it does not change the verdict on the live
listing — which is already "replace".** What it changes is *when* the replacements are worth
capturing.

**Capture after Phase 15.** Under the §1.3 ruling the ≥ 4 phone screenshots are captured once,
from the build that ships 3.0.0, with the refreshed Home, the redesigned in-game chrome and
the new tutorial already in it. Under the flip condition they are captured for 3.0.0 and
recaptured for 3.1.0 — two rounds, and the recapture is not optional, because 3 of the 4
mandatory shots are of surfaces Phase 15 redesigns.

One correction to `14-OPERATOR-CHECKLIST.md` Step 1 item 3 while I am here: it says the studio
can capture screenshots *"once Step 6 unblocks a build"*. **Screenshots do not need Step 6 or
Step 7.** Real ad IDs and the upload keystore are irrelevant to a screenshot of Home, a board,
Results or the catalog; a debug build on an emulator produces policy-acceptable captures. The
binding precondition on screenshots is **UI finality**, not signing — which is precisely why
the sequencing question matters and why capturing them before a scheduled refresh is
guaranteed rework.

### What the ≥ 4 phone screenshots must show (rules/play-store.md)

Ordered as a store reel, each justified by what it has to prove to a browsing player, with
the Phase 15 verdict per shot:

| # | Screen | What it proves | Phase 15 |
|---|---|---|---|
| 1 | **Home** — the refreshed screen, with the Continue chip visible | the app opens into play, and progress is remembered | **redesigned — must be post-15** |
| 2 | **Classic board mid-game** — a real position, a peg selected, legal-move dots on, the in-game chrome visible | the actual gameplay, and the assist that makes it fair | **chrome redesigned — must be post-15** |
| 3 | **Challenge level mid-game or the Classic catalog with star pips** — whichever reads as "there is more here" | the Core Value: an infinite, always-solvable ladder / five boards with stars to earn | catalog is **untouched** unless the fuller Home absorbs it — ux-designer to confirm |
| 4 | **Results at 3 stars**, celebration state | the reward loop, and that scoring is legible | **untouched** |
| 5 (recommended, not required) | **The interactive tutorial**, one instructive frame | that a new player is taught, not dropped in — this is the single most listing-worthy thing Phase 15 adds | **exists only post-15** |

So: **3 of the 4 mandatory shots, and the one recommended fifth, are Phase 15 surfaces.**
That is the store-listing bill of deferring, in one line.

Two store assets that a UI refresh does **not** invalidate, so they do not belong in this
sequencing decision: the 512×512 listing icon and the 1024×500 feature graphic. Both are store
art, not captures of UI. They have their own, separate problem — unknown provenance and
licence, Q-F3 / checklist Step 9, a `THIRD_PARTY.md` row that blocks the release under
rules/ip-policy.md — and Phase 15 neither helps nor hurts it. The launcher icon is drawn
in-repo as vector XML and is also untouched.

---

## 3. Versioning

Under rules/play-store.md: `VERSION` = semver `X.Y.Z` = `versionName`;
`versionCode = X*10000 + Y*100 + Z`, monotonic and a one-way door.

### If Phase 15 lands before the cut (the recommendation)

**It ships as 3.0.0. Nothing changes.** `VERSION` stays `3.0.0`, `versionCode` stays `30000`
(already on `develop` at `08b1b27`), tag `v3.0.0`, branch `release/3.0.0`, approval file
`.planning/approvals/publish-3.0.0.approved`. A UX refresh that lands on `develop` before the
branch is cut is simply part of that release; semver has nothing to say about it. Phase 15's
`feature/*` branch **must not touch `VERSION`** — gitflow bumps it on the release branch, and
an early bump on `develop` creates the gate trap in §5.

This answer is conditional on Q-F1 (*"ship milestone v1.1 as product version 3.0.0, with
versionCode 30000"*) resolving as its own recommendation, which is also what the build already
encodes. Q-F1 is not yet filed in `FOUNDER.md`; Phase 15 gives the producer no reason to change
it, and I am not re-opening it.

### If Phase 15 lands after the cut (the flip condition)

**It ships as 3.1.0, and the next versionCode is 30100** (`3*10000 + 1*100 + 0`).

- **Minor, not patch.** An interactive tutorial is a new, user-visible capability; a patch
  bump would misdescribe it and `3.0.1` = `30001` would have to be spent on the next real
  patch anyway.
- **Minor, not major.** `4.0.0` (= `40000`) is not warranted. The `2.0 → 3.0.0` major was
  bought by a complete rewrite — new language, new UI toolkit, new architecture, a new mode.
  A refresh of two screens plus an onboarding flow is additive and changed behaviour, not a
  break. Spending a major on it also discards expressive room for nothing.

### Monotonicity, confirmed against what is published

| Value | Source | Monotonic? |
|---|---|---|
| `2` | highest **published** versionCode, `versionName "2.0"`, the earlier different app (repo record; the Console value is read by the founder — checklist item 2.3 `[F]`) | — (baseline) |
| `3` | this repo's pre-studio value, **never uploaded** | raising it breaks no installed copy |
| `30000` | 3.0.0, already on `develop` | `30000 > 2` ✓ |
| `30100` | 3.1.0, if Phase 15 defers | `30100 > 30000 > 2` ✓ |

Both orderings are monotonic and neither closes a door the other keeps open. The one-way-door
warning stands in both: `versionCode` can never decrease for a published app, and
`docs/release-checklist.md` §7b names a too-high versionCode as unrollbackable. Adopting the
formula now (already done) costs nothing; `30000` would be the number either way.

One thing that does **not** change in either branch: `.planning/` counts milestones (v1.1) and
the store counts builds (3.0.0). The mapping goes in `.planning/MILESTONES.md` at close-out
(checklist item 6.4). If Phase 15 becomes 3.1.0, it needs its own milestone label there too —
it is a new milestone (v1.2 by the roadmap's own numbering), not a trailing phase of v1.1.

---

## 4. Re-verification scope, if Phase 15 lands before the release

The classification rule I applied, stated so it can be checked: an item re-runs when **the
surface it observes** changed, even if the mechanism under it did not. A platform or domain
mechanism proven through an unchanged oracle does not re-run. "Per-artifact" means the item
re-runs for any new build regardless of Phase 15 — so under the recommended order it is run
**once**, after Phase 15, and Phase 15 adds nothing to its cost.

### 4a. Phase 14 success criteria (`.planning/ROADMAP.md` §"Phase 14")

| Criterion | Verdict | Reasoning |
|---|---|---|
| **1 — REL-02**, every feature verified from a signed internal-track download | **partly re-run; mostly per-artifact.** See 4b for the test-level split. | The criterion is per-artifact by construction; the refresh changes *which* of its tests are observation-equivalent to a pre-15 run. |
| **2 — REL-03**, `app-ads.txt` published, linked, verified in AdMob | **untouched.** | A website file and a Console status. No UI surface, no code. |
| **3 — REL-01**, Console declares ads, data safety, target audience truthfully | **untouched, with one guard.** | A tutorial, a fuller Home and redesigned icons collect no data, add no SDK and change no permission. **Guard:** if Phase 15 adds any analytics, funnel event or crash-reporting call, REL-01 and REL-04 both re-open. It must not, and the tech-lead should say so in the phase's plan. |
| **3a — REL-04**, privacy policy live, consistent with REL-01 | **untouched**, same guard. | No new data category. |
| **4 — PLAT-08**, cold start against the Phase 6 baseline | **RE-RUN — this is the one criterion Phase 15 genuinely invalidates.** | Detail below. |

**PLAT-08, condition by condition** (`14-COLD-START.md` §3.3, §4):

- **C1 — comparand, steady state, the actual pass/fail gate (median ≤ 950 ms vs the 633 ms
  baseline): RE-RUN.** A "fuller home screen" adds composition and measurement work to the
  first frame, and Home is the first frame. This is not a formality: the budget is baseline
  +50%, the observed noise band is ±3.6%, and the gate exists precisely to catch work landing
  on the critical path. If the tutorial's state or assets are resolved at Home composition
  rather than lazily on first launch, C1 is where it shows.
- **C2 — offline attribution: RE-RUN**, as C1's pair. `C1 − C2` is the network-dependent
  share and is meaningless if only one of the two is current.
- **C3 — first run, `pm clear`, consent form **and onboarding** on the path: RE-RUN, and
  expect movement.** C3 is measured against the 2000 ms hard ceiling only, and Phase 15
  replaces a dismiss-once overlay with an *interactive* tutorial sitting directly on that
  path. This is the measurement most likely to change and the one with no comparand to hide
  behind.
- **C4 — release variant, store build: per-artifact.** Runs once, after Phase 15, against the
  2 s ceiling. Founder-blocked on Steps 6/7 either way.

Net: **3 of 4 cold-start conditions re-run, and C1 is a pass/fail gate that could fail.** Run
C1/C2/C3 on the merged Phase 15 tree *before* the cut, not after, so a regression is a
`feature/*` fix and not a re-cut release branch.

### 4b. The accumulating device checklist — `.planning/phases/11-play-games-session/11-HUMAN-UAT.md`

Per the Phase 12 decision recorded in `.planning/STATE.md`, this file is the project's single
accumulating device checklist: 13 tests, currently 12 passed / 1 blocked / 1 partial.

**Must re-run (5 of 13):**

| Test | Why the observation is no longer valid |
|---|---|
| **1 — silent sign-in and the "Welcome back" popup on relaunch (PGS-01)** | The mechanism (`PlayGamesSdk.initialize` from `App.onCreate`, quick task 260915-51n) is untouched, but the test's entry path is the Home *"Sign in to Play Games?"* prompt, and `PlayGamesPromptDialog` is hosted by the redesigned `HomeScreen`. Re-run the accept-then-relaunch path; the SDK evidence in the existing `result:` line stands as the explanation, not as the observation. |
| **9 — minified release build needs no new R8 keep rules (Pitfall 12)** | Per-artifact *and* substantively at risk: new Compose UI, new strings and any new drawable for the tutorial pass through R8 and `shrinkResources`. This is the exact change class that produces a `missing_rules.txt` entry or a silently shrunk resource. Re-check `missing_rules.txt` **absent** and the Play Games entries still present in `mapping.txt`. |
| **10 — restore on a new device (CLOUD-01)** — *read-out step only* | The merge mechanism and its oracles (`change_pending`, `last_modified_at`) are untouched. But the test's player-visible assertion is *"the Classic catalog shows English-33's stars and the Challenge tile reads the level"*, and the Challenge tile is on the redesigned Home. **Do not re-stage the two-device restore.** Re-identify the read-out surface on the new Home and re-assert the D-03 negatives there (no toast, dialog, snackbar or sync row). |
| **11 — two-device union, and the D-04 sub-check** — *read-out step only* | Same reasoning; the sub-check asserts *"the Continue tile still resumes that level"*, and the Continue chip is on the redesigned Home. The gap-5 device evidence from 2026-09-21 (`.planning/reports/phase-14/qa-engineer-nextlevel-device.md`) proves the *arithmetic*; what needs re-observing is that the tile still exists, still reads the merged head, and still resumes the in-progress board. |
| **13 — network kill degrades silently (CLOUD-05)** — *negative assertions only* | The offline-solve and reconvergence halves do not need re-staging. What must be re-asserted is the D-03 negative on the new surfaces: *"nothing in the UI that hints a cloud exists"* on the fuller Home and the redesigned chrome. A code review plus one launch, not a re-run. |

**Hard precondition on tests 10, 11 and 13, and it is a Phase 15 design constraint, not a QA
task:** the refreshed Home must keep the Challenge level, the Classic star state and the
Continue affordance readable, because they are the *oracles* of the cloud-save tests. If the
refresh removes or hides any of the three, those tests have no oracle and cannot be re-run at
all until a new one is defined. The ux-designer must confirm this in the Phase 15 UI spec, and
the producer should treat it as an acceptance criterion of the phase.

**Do not re-run on UX grounds (8 of 13):**

| Test | Why |
|---|---|
| **2 — Settings sign-in row, cancel vs error** | Settings screen; untouched. Stays blocked for its own reason (one account, already authorised — needs the founder's second account, checklist Step 4). |
| **3 — chooser/popup do not trip interstitial foreground cadence** | Asserts on `AppForegroundTracker` and a GMS-owned window. No Home or game-screen assertion. Stays partial for its own reason. |
| **4 — stock unlock popup on first solve** | Renders over **Results**, which Phase 15 does not touch. |
| **5 — both Settings rows open the stock Play Games UIs** | Settings; untouched. **Unless** the refresh relocates Leaderboards/Achievements onto the fuller Home — then the entry-point half re-runs and nothing else does. |
| **6 — a real score lands on all three leaderboards** | Server-side oracle; the solve path's *outcome*, not its chrome. |
| **7 — offline-then-sign-in backfill submits exactly once** | Server-side oracle and a persistence flag. |
| **8 — both Play Games rows hidden while signed out** | Settings; untouched, same relocation caveat as test 5. |
| **12 — legible Saved Games listing entry (CLOUD-03)** | Cover art and description string produced by `CloudSaveDescriptionFormatter` / `CoverImageSource`; no app UI in the assertion. |

**The recorded gap is unchanged and is not a Phase 15 item:** the profile-swap dialog
(D-12..D-15) still needs a second Google account. Note though that `ProfileSwapDialog` is
hosted by `HomeScreen`, so when checklist Step 4 finally lands, **the run must be on the
post-15 Home** — which is another argument for doing the refresh first. That is release-
checklist item 5.10 / `14-RELEASE-PLAN.md` T-12, and it is the one destructive path in the
milestone.

### 4c. The other UAT sheets

| Sheet | Verdict |
|---|---|
| `08-HUMAN-UAT.md` (3 tests + resume + evidence, all passed on hardware) | **Tests 1 and 2 re-run as control-level re-checks**, not as proofs: the hint affordance is one of the in-game top icons being redesigned, so what needs re-observing is that the redesigned control still shows the thinking state and still delivers a move inside 1.5 s. The solver, the opening book and their evidence are untouched. **Test 3 (Diamond-41 at 2 pegs → 3 stars) untouched** (Results). **The resume check re-runs** — it goes Home Continue → game screen, both redesigned. |
| `10-HUMAN-UAT.md` (1 test, **pending**: the 12-step interstitial UAT on physical hardware) | **Untouched by Phase 15** — its steps are Results-side (the quiescence dim, gesture-back absorption, the 5th-interstitial session bound). It is still outstanding for the release either way; that is pre-existing debt, not a Phase 15 bill, and it should not be charged to this decision. |
| `12-HUMAN-UAT.md` (7 tests, 7 pending; tests 1–6 mirror the 11 file's 4–9, *"a pass recorded in either file counts for both"*) | **Tests 1–6: same verdicts as 4b.** **Test 7 (perfect Diamond-41 → `CLEARED_DIAMOND41`): untouched** — Results, the Classic catalog and two database rows. |
| `14-HUMAN-UAT.md` (T-0..T-13, **planned, does not exist**) | Write it **after** Phase 15's surfaces are settled. T-0..T-13 are per-artifact; under the recommended order the sheet is authored once and run once. T-1, T-6, T-7, T-11 and T-12 are the ones whose *expected:* text names a Phase 15 surface and would otherwise be written twice. |

### 4d. Release-checklist §5 (the track-download pass), item by item

Per-artifact throughout — so under the recommended order it is run once. Listing the split
anyway, because under the flip condition these are the items paid for twice:

- **Observation changes with Phase 15:** 5.2 (PGS sign-in — Home prompt path), 5.6 (consent
  form appears **before Home**), 5.7 (rewarded — raised from the hint/undo funnel, i.e. the
  redesigned icons, via `AdOfferDialog`), 5.9's sound half (SOUND-04 cues are wrapped at the
  leaf composables `ModeTile`, `PrimaryAction`, `NeutralAction`, `BoardCard` — Home and chrome
  both — and rules/audio.md requires every cue to fire on its event) and 5.9's resume half,
  5.10 (account swap — Home-hosted dialog).
- **Observation unchanged by Phase 15:** 5.3 (server-side ladders), 5.4 (achievement unlock and
  popup over Results), 5.5 (cloud union), 5.8 (interstitial after Continue, clock paused),
  5.8a (ad inspector), 5.0/5.1/5.8b (gates and hygiene, per-artifact and UI-independent).
- **§4 artifact gates (4.1–4.8):** per-artifact, UI-independent. Phase 15 changes nothing
  about signing, `missing_rules.txt`, the hash or the identity read-back — except that a
  larger UI surface is one more reason 4.5 is worth reading rather than assuming.

### 4e. Summary of the re-verification bill

| Pay it once (Phase 15 before the cut) | Pay it twice (Phase 15 after) |
|---|---|
| PLAT-08 C1/C2/C3 | PLAT-08 C1/C2/C3, and C4 twice |
| 5 re-runs in `11-HUMAN-UAT.md` (3 of them read-out-only) | those 5, plus a full second §5 track-download pass |
| 2 control re-checks + resume in `08-HUMAN-UAT.md` | those, plus a second internal-track upload and a second Gate 2 |
| the `14-HUMAN-UAT.md` sheet, authored once | the sheet authored once and revised once |
| ≥ 4 store screenshots, captured and approved once | ≥ 4 captured, approved and uploaded twice |
| one changelog section, one 500-char store note, one founder summary | two of each |
| — | the ≥ 24 h AdMob test-device propagation and the second-account setup, staged twice |

"Re-test everything" would have been wrong in both directions: REL-01, REL-03, REL-04, the
leaderboard/achievement/cloud-merge mechanisms and every Results-side assertion are genuinely
untouched, and 8 of the 13 accumulated device tests do not re-run at all.

---

## 5. Gate 2 — the approval file

**Under the recommendation (Phase 15 inside 3.0.0):** the gate is unchanged in name and in
mechanism. The founder's approval file is
**`.planning/approvals/publish-3.0.0.approved`**, derived from root `VERSION` = `3.0.0`, asked
as **`Q-PUBLISH-3.0.0`**, created **only** by the founder via `/ergoz-soft:approve publish
3.0.0`, living on **`develop`** — not on `main` and not on the tag — so
`scripts/play-upload.sh` is run from a `develop` checkout after the approval merge. The publish
hook blocks it anywhere else. What Phase 15 changes is the *content* of the Gate-2 packet, not
its name: the one-screen summary, the changelog section and the 500-character store note all
describe the refreshed UX, and the store-asset state line reports screenshots captured from
the refreshed build. One gate, one ask, one packet — re-authored, not renamed.

**Under the flip condition (Phase 15 becomes 3.1.0):** **two** gates, and the first does not
carry over to the second. `publish-3.0.0.approved` for the pre-refresh release
(`Q-PUBLISH-3.0.0`), then a separate `publish-3.1.0.approved` for the refresh
(`Q-PUBLISH-3.1.0`) — two founder asks, two push notifications, two one-screen summaries, two
`.planning/reports/release-<X.Y.Z>/` directories. `docs/release-checklist.md` §7b already
states the rule for the hotfix case (*"a new `publish-<X.Y.Z+1>.approved` — the old one does
not carry over"*); it holds for any new version, because the file name is derived from root
`VERSION`.

**One trap that exists only under the flip condition, and it will bite if nobody names it.**
Both the plugin's publish hook and the planned `scripts/play-upload.sh` resolve the approval
file from `$(cat VERSION)` **at the moment the script runs**. If Phase 15 or anything else
bumps `VERSION` to `3.1.0` on `develop` while the 3.0.0 bundle is still awaiting upload, the
gate looks for `publish-3.1.0.approved`, does not find it, and **refuses a 3.0.0 upload the
founder has already approved** — a blocked release with no visible cause. Gitflow already
prevents this if it is followed exactly: `VERSION` is bumped **on the release branch**, and
`develop` only sees `3.1.0` after `release/3.1.0` is back-merged. So the guardrail is a rule,
not a mechanism, and it belongs in the Phase 15 plan either way:

> **`feature/phase-15-ux-refresh` must not touch root `VERSION`.** `develop` stays at `3.0.0`
> until the 3.0.0 upload has run and `release/3.1.0` is cut.

---

## Checks

No build was run: this is a documentation ruling on a branch that carries no code change, and
rules/reporting.md requires the Checks section regardless. What was verified, and how:

- `cat /Users/ermanergoz/Desktop/PegSolitarie-p15/VERSION` — pass (`3.0.0`)
- `grep -n 'versionCode\|versionName' app/build.gradle.kts` — pass (`versionCode = 30_000`, `versionName = "3.0.0"`, lines 280–281; the `versionCode 2` rationale comment at line 269)
- `git log --oneline -1 develop` — pass (`2397732`; `08b1b27 chore: encode the version name in versionCode (3.0.0 -> 30000)` reachable)
- `git tag -l` — pass (`v1.0` only; no `v3.0.0` yet)
- `git ls-tree -r --name-only <b> -- docs/store/` for `develop`, `main`, `feature/phase-14-release-plan`, `-release-docs`, `-gate-scope`, `-prerelease-fixes`, `-kickoff` — pass, **empty on all seven** (the finding behind §2)
- `git diff --stat develop feature/phase-14-release-docs` — pass (adds `CHANGELOG.md`, `THIRD_PARTY.md`, `docs/release-checklist.md`, `docs/ad-testing-safety.md`, `docs/tech-design.md`; **no** `docs/store/`)
- `grep -rn 'roborazzi\|screenshotTest\|paparazzi' gradle/libs.versions.toml app/build.gradle.kts build-logic/` — pass (0 hits; no golden images to re-record)
- `ls .planning/approvals/` — pass (`concept.approved` only; no `publish-3.0.0.approved`, as required)
- `diff` of all 11 role-relevant rule files, repo snapshot v0.5.1 vs plugin `rules/` (plugin.json 0.5.2) — pass, byte-identical for studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy

Files read and reasoned from (absolute paths; the phase-14 documents were read out of
`feature/phase-14-release-plan` and `feature/phase-14-release-docs` with `git show`, without
touching those worktrees):

- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/ROADMAP.md` — Phase 14 goal, requirements, the four success criteria, the External Prerequisites table
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/STATE.md` — milestone v1.1, accumulated decisions, blockers, the Phase 12 single-UAT-file decision
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/reports/STATUS.md` — read only, not edited; the 2026-09-21 phase-14 lines and the R-1..R-7 blocker line
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/questions/FOUNDER.md` — Q-001, Q-002, Q-CONCEPT all answered; Q-F1..Q-F5 not yet filed
- `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` (637 lines, via `git show feature/phase-14-release-plan:`) — version reasoning, branch preconditions §2a/§2b, T-0..T-13, `THIRD_PARTY.md` inventory, Q-F1..Q-F5
- `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` (510 lines, same source) — Steps 0–10, especially Step 1 (listing assets) and Steps 6/7
- `.planning/phases/14-release-readiness/14-COLD-START.md` (259 lines, same source) — the 633 ms baseline, conditions C1–C4, the 950 ms / 1.25× / 2000 ms thresholds
- `docs/release-checklist.md` (193 lines, via `git show feature/phase-14-release-docs:`) — §§1–8, and §5's per-artifact track-download items
- `CHANGELOG.md` (via the same branch) — the 3.0.0 preamble and the versionName reasoning
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/phases/11-play-games-session/11-HUMAN-UAT.md` — the 13-test accumulating device checklist
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/phases/12-leaderboards-achievements/12-HUMAN-UAT.md`
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/phases/10-interstitials/10-HUMAN-UAT.md`
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/phases/08-assist-generator-quality-debt/08-HUMAN-UAT.md`
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` — the Info affordance (line 350), the Settings gear, the `OnboardingOverlay` host (line 175), the Continue chips, the mode tiles, the two hosted dialogs
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — `GameTopBar` (lines 490–524) and its four action controls
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.claude/rules/*.md` and `/Users/ermanergoz/Desktop/ergoz-soft/rules/*.md` — the eleven role-relevant rule files
- `/Users/ermanergoz/Desktop/PegSolitarie-p15/.claude/rules/lessons.md` — 0 active lessons, hence `Lessons applied: none`

## Open issues / risks

**Proposed founder question — the producer files it, I do not contact the founder.**

    ## Q-<NNN> — Does the UX refresh ship inside 3.0.0, or as 3.1.0 after it?   (open)
    Date: 2026-09-21    Asked by: release-manager
    Context: Phase 15 redesigns Home, the in-game chrome and onboarding. No store
    screenshots exist yet; the live listing's belong to the versionCode-2 app and are
    already dead. The v1.1 cut is blocked by the developer website, the Console
    declarations, the keystore and the fuzz harness — none of which Phase 15 touches.
    Options: 1) Phase 15 lands before the cut and ships inside 3.0.0 (versionCode 30000)
    2) cut 3.0.0 now without it; Phase 15 becomes 3.1.0 (versionCode 30100) with its own
    Gate 2 3) something else you have in mind about what the store sees first.
    Recommendation: release-manager picks 1. It pays the track-download verification pass
    and the screenshot capture-and-approve loop once instead of twice, and its schedule
    cost against the real critical path is small because that path is founder-side and has
    not started. Option 2 buys one real thing — it decouples "does Play work" from "does
    the new UX work", so a first-ever Play round-trip fails for certificate or Console
    reasons rather than for tutorial reasons. If your priority is getting one build
    through Play end to end as fast as possible, pick 2.
    Blocks: nothing. The default in option 1 is in force and the producer can flip it
    without you (§1.3 flip condition) if the founder prerequisites and R-2 close first.
    Answer:

**Default in force so nothing blocks:** option 1, with the §1.3 flip condition. Phase 15 is
planned and built against 3.0.0; `feature/phase-15-ux-refresh` does not touch `VERSION`; no
screenshot is captured until Phase 15's surfaces are merged.

**Risks**

1. **PLAT-08 C1 could fail on the refreshed Home.** The gate is median ≤ 950 ms against a
   633 ms baseline with a ±3.6% noise band, and "fuller home screen" means more work on the
   first frame. Measure C1/C2/C3 on the merged Phase 15 tree **before** the cut, so a
   regression is a `feature/*` fix rather than a re-cut release branch. Owner: qa-engineer for
   the measurement, tech-lead if it misses.
2. **Three cloud-save tests read their oracle off the Home screen.** Tests 10, 11 and 13 of
   `11-HUMAN-UAT.md` assert on the Challenge tile, the Classic star state and the Continue
   affordance. If the refresh removes or hides any of the three, those tests lose their oracle
   and cannot be re-run at all. This belongs in the Phase 15 UI spec as an acceptance
   criterion. Owner: ux-designer to confirm, producer to carry it into the phase.
3. **Phase 15 must add no analytics, funnel event or crash reporting.** Any data collection
   re-opens REL-01 (Data safety) and REL-04 (privacy policy), both of which hang off the
   founder's longest-lead item. Owner: tech-lead, in the phase plan.
4. **Scope discipline.** This is the fifth open item in a release window that already carries
   R-1..R-7 and five unfiled founder questions. The flip condition is the stop-loss; if it is
   never evaluated, it is not a stop-loss.
5. **Not caused by Phase 15, and not to be charged to it:** `10-HUMAN-UAT.md`'s hardware
   interstitial re-run is still pending; `11-HUMAN-UAT.md` test 2 is blocked and test 3
   partial for want of a second Google account; and the store artwork's provenance rows
   (Q-F3 / checklist Step 9) still block `THIRD_PARTY.md` and therefore the release under
   rules/ip-policy.md. All three are pre-existing and independent of this ruling.
6. **`14-OPERATOR-CHECKLIST.md` Step 1 item 3 needs one correction** (it ties screenshot
   capture to Step 6): screenshots need a build whose **UI is final**, not a signed one. The
   correction belongs on the release branch with the rest of `docs/`, and it is the release-
   manager's to make when that branch is cut — not now, on a feature branch.

## Handoff
Next: producer — file the Q-<NNN> above in `.planning/questions/FOUNDER.md` (non-blocking,
`Blocks: nothing`) and write the Phase 15 roadmap entry against option 1: Phase 15 merges to
`develop` before `release/3.0.0` is cut, in parallel with the founder's Steps 0–7 and with R-2,
gating only Phase 14's UI-dependent verification (the §5 track-download pass, PLAT-08
C1/C2/C3, `docs/store/` screenshots, the `14-HUMAN-UAT.md` sheet). Carry three items into the
phase: the refreshed Home must keep the Challenge level, Classic stars and Continue affordance
readable (cloud-save oracles, §4b); Phase 15 adds no analytics of any kind (§4a guard); and
`feature/phase-15-ux-refresh` does not touch root `VERSION` (§5). Then: ux-designer for the UI
spec, qa-engineer for the PLAT-08 re-measurement and the five device re-runs in §4b, and the
release-manager again at the cut — where the screenshots, the changelog section and the Gate-2
packet are produced for **`Q-PUBLISH-3.0.0`** and
`.planning/approvals/publish-3.0.0.approved`.
