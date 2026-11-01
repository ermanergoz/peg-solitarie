# 3.0.0 release readiness — `release/3.0.0` cut, device pass recorded (2026-11-01)

Date: 2026-11-01. Author: release-manager. **`release/3.0.0` is cut** from `develop` = `da886ed`
(`feature/candy-backdrop` merged, the last branch-cut row), in the worktree
`/Users/ermanergoz/Desktop/PegSolitarie-release`. Merged to `main` and tagged `v3.0.0` on 2026-11-01 (§1 Gitflow finish); nothing uploaded,
no founder question asked: Gate 2 comes later. The earlier pre-cut revision of this file (read at
`d251c3b`, candy on HOLD) is in git history.

States: **done** (evidence observed today, in `.planning/reports/release-3.0.0/release-manager-cut.md`),
**owed** (studio work left, owner named), **founder** (founder-only, `[F]`, waits on the operator
checklist / F-items), **device** (the release device pass, §3), **post-build** (needs the AAB of 4.3).

## 1. Release checklist (`docs/release-checklist.md`), item by item

### §1 Before the branch is cut
| # | State | Evidence / what is left |
|---|---|---|
| 1.1 | **done** | `git status --porcelain` empty at the cut. Every branch-cut row merged (first-parent merges): R1 `b1a6cf9`, R-2 `dbd0a6c`, audio `6aa7624`, RM `053e9cc`, F-1 `305f741`, colour-vision `1bc442f`, LN `bc95b2a`, TB `d251c3b`, candy `da886ed`. Row DT deferred to 3.0.1, owner tech-lead (producer's decision `4135e2d`, on `develop` via `47cb7e0`). PU blocks Gate 2 only (1.7) |
| 1.1a | **done** | Merge `d251c3b` exists and brought `ui/components/BoardSpeech.kt` among its `app/src/main` files; reviews and sign-off as read pre-cut (tech-lead sign-off PASS, QA device3 PASS on `ergoz_studio`) |
| 1.2 | **done** | `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks --console=plain` → `exit 0`, BUILD SUCCESSFUL in 11 m 38 s, 107 tasks, 107 executed. Fuzz: whole space, 10,000 levels examined (requested 100,000), determinism pass, 0 failures. 1,937 unit tests, 0 failed, 0 skipped. Quiet host: 1-min load 4.31 at start, 4.32 at end (≤ 5.0). Run on `release/3.0.0` = `develop` `da886ed` code (only docs edits on top), so it serves 1.2 and 3.1 alike |
| 1.3 | **done (ticked)** | code-auditor PASS, `.planning/reports/release-3.0.0/code-auditor-suppressions.md`: 31 `@Suppress`/`@file:Suppress` sites (13 main, 18 test), 8 prior approvals cited, 23 approved there, none rejected; 0 `@SuppressLint`/`tools:ignore`/`@Ignore`/baselines. The release-manager's 44-hit list was one short (`Clay.kt:3` `@file:Suppress`): the checklist grep is widened per A5. A1–A4, A6 → §5 (3.0.1) |
| 1.4 | **done (pre-cut half)** | 0 `PENDING FOUNDER`; §6 U-1..U-5 closed, U-6 closes at 3.5; licences merge `bc95b2a` exists; candy and licences rows updated (2.4a). §16 #20's remainder is 3.5 / 4.6a |
| 1.5 | **done** | Only open question Q-025, `Blocks: nothing` |
| 1.6 | **done** | `git rev-list v1.0..release/3.0.0 --count` = **1366**; 214 `feat`, 89 `fix`/`perf` subjects (no-merges) |
| 1.7 | **owed (Gate 2)** | script on `develop` since `1dd1080`; the producer records the route (script or Console) and the tech-lead's PU close before the tick |

### §2 Cutting the branch
| # | State |
|---|---|
| 2.1 | **done** — `release/3.0.0` from `develop` `da886ed` (worktree add) |
| 2.2 | **done** — `VERSION` = `3.0.0` |
| 2.3 | **done [S] / founder [F]** — `versionName "3.0.0"`, `versionCode = 30_000` = 3·10000+0·100+0 > published 2; the Console read of the published max is F-13 |
| 2.4 | **done (ticked)** — `## [3.0.0] — 2026-11-01`, dated on `release/3.0.0` right before the merge to `main` and the tag |
| 2.4a | **done** — all pending branches merged (`--is-ancestor` exit 0 for audio-review, colour-vision, licences-screen, candy-backdrop, phase-15-ux-refresh); markers removed in `CHANGELOG.md`, `THIRD_PARTY.md` (branch table, candy row, Apache row, LN bullet) and `docs/store/README.md` (both pending tables); `grep -n 'pending:' CHANGELOG.md` silent. Listing lengths unchanged (no store text edited) |
| 2.5 | **owed with the TalkBack store line** — the CHANGELOG now carries the TalkBack bullet; the store texts make no TalkBack claim yet. If the line is added (Gate 2 offer), it is counted, word-checked and read against the changelog then |
| 2.5a | **taken in the changelog only** — R1 merged (`b1a6cf9`); the CHANGELOG Changed bullet says "no winning move" appears only when proven. Store copy not raised (founder's choice at Gate 2) |
| 2.6 | **done** — `CLAUDE.md` and `AGENTS.md` Build command: `./gradlew :app:bundleRelease` |
| 2.7 | **done** — this cut's commit `chore(release): 3.0.0` |

### §3 Gates on the release branch
| # | State |
|---|---|
| 3.1 | **done** — as 1.2; log `build/release-check.log` (git-ignored); both fuzz lines present; no gate task `FROM-CACHE` (the 5 `UP-TO-DATE` lines are action-less lifecycle tasks: `preBuild`, `generateDebugAssets`, `testClasses`) |
| 3.2 | **done** — `test ! -e domain/build/fuzz-failures.txt` after an executed `:domain:fuzz` |
| 3.3 | **done** — `:domain:koverVerify` executed; `./gradlew :domain:koverXmlReport --offline` exit 0, `report.xml` 12:41:55 (after the run): LINE missed 9 covered 912 = **99.0 %** (912/921) at bound 90, not below the recorded 99.0 % |
| 3.4 | **done** — 8 of 8 literal-gate tasks executed (`:app` and `:domain` × ad-ID, Games-ID, audio-focus, dp/sp) |
| 3.5 | **done (ticked)** — research-analyst PASS (`research-analyst-licences.md`): 191 coordinates, the 192 of 2026-09-15 less `ui-text-google-fonts`, none new or changed; 191 POMs re-fetched, each maps to a row; okhttp 4.11.0; no `cronet-embedded`. U-6 and §5b closed in `THIRD_PARTY.md` `2cef970`, diff reviewed by the release-manager: PASS (`release-manager-gate2-prep.md`, 2026-11-01 addendum) |
| 3.5a | **pre-bundle half done; post-bundle half post-build** — 191 scanned, 2 NOTICE hits (Jakarta, PSL), both bundled byte-equal in `assets/licenses/`; checker-qual credited on the Licenses screen, so no `mapping.txt` grep. After 4.3: `unzip -l <aab> \| grep -i notice` shows both |
| 3.5b | **done, ticked** — versions printed are 22.0.0 (games-v2), 1.4.0 (ads-mobile-sdk), 4.0.0 (UMP), 2.0.1 (hsdp), unchanged; no re-read owed (`research-analyst-licences.md`) |
| 3.6 | **done (ticked)** — `git diff v1.0..HEAD` grep: 488 hits; the 36 outside `.planning/` (10 files) are signing-code comments, docs prose, test fixtures (`fixture-store-password`, `/fixture/keys/upload.jks`), the template placeholder, and the checklist row's own pattern — no key, value or `/Users/` path. **Producer's scope ruling (2026-11-01):** 3.6 guards shipped and published material — app/domain/build-logic sources, resources, `docs/store/`, `THIRD_PARTY.md`, `CHANGELOG.md`, scripts; the 405 `/Users/…` paths in `.planning/` reports are internal studio records, the repo has no remote, they carry no secret or credential, and they are accepted |
| 3.7 | **done** — `git status --porcelain` clean after the commit; `find` for `*.jks`/`*.keystore`/`keystore.properties` empty |

### §4 The artifact
| # | State |
|---|---|
| 4.1, 4.2 | **founder** (F-7, F-8) — no `PLAY_UPLOAD_*` variable is set on this host (names checked, no values read) |
| 4.3 | **founder-blocked; builds from the `v3.0.0` tag** (checkout of the tag, or the kept `release/3.0.0` worktree at the tagged commit) — `:app:bundleRelease` depends on `verifyReleaseSigning` and the real-ID gates, so it cannot run without 4.1/4.2. Not attempted |
| 4.1a, 4.4, 4.5, 4.6, 4.6a, 4.7 | **post-build** (T-8 for 4.5 still owed) |
| 4.8 | **post-build**; draft in `Q-PUBLISH-3.0.0-draft.md` (candy lines now measured) |

### §5 — **founder** + QA after upload. §6–§8 — post-release.

### Gitflow finish (2026-11-01)
`release/3.0.0` merged to `main` (`--no-ff`), tagged `v3.0.0` (annotated), `main` merged back into `develop` (`--no-ff`); details and commits in `.planning/reports/release-3.0.0/release-manager-merge.md`. **Branch deletion deferred:** `release/3.0.0` and its worktree `/Users/ermanergoz/Desktop/PegSolitarie-release` are kept until the upload is done, because 4.3+ (signed bundle) waits on the founder's keys (F-7/F-8) and must be built from the `v3.0.0` tag; delete both after the upload (rules/gitflow.md last step).

## 2. Docs syncs
Items 1–6 of the pre-cut list landed with `0ac2abd` (tech-design sync), `8a7cb89` (ux folds) and
`452f187` (release prep). Still owed, none blocking the cut:
- **candy-backdrop** [tech-lead + ux-designer]: the D1 docs sync (BD-23 (a), B-08 wording; tech-design D1 line) and §8.5's two-frame oracle, due right after the merge (candy sign-off §5 item 4).
- **tech-lead** next sync: Conditions 7–9 of `tech-lead-docs-signoff.md` (§10 first-frame threshold, §8.6 item 3 pointer, bundletool host-tool row).
- Conditions 4–6 of that sign-off (release-manager): **done on this branch** — T-11 lapse wording, the quiet-host read in 1.2/3.1, the bare `bash -n <` syntax check, and the release plan's stray spaces.

## 3. Release device-pass list (for the qa-engineer)
Founder directive 4: emulator only unless a phone is attached; each phone-only item then goes to Q-PUBLISH as unverified.

**D-1 preconditions (tech-lead docs sign-off, Condition 2; binding before the pass runs):**
- **(a) Signing.** `bundletool build-apks` with no `--ks` signs with `~/.android/debug.keystore`, so the installed APKs carry the **debug** signature, not the upload key's. **A Play Games sign-in failure on this install is expected and is not a 3.0.0 defect**; Play Games is verified from the track download (§5). **Any install of the app under another signature must be removed first** (`adb -s <serial> uninstall com.erman.pegsolitarie`). Never pass `--ks` with the upload key (it would put the keystore passwords on a command line).
- **(b) Device.** Both bundletool calls name the emulator explicitly: `--device-id=<serial of ergoz_studio>`, **never `Pixel_9a_bench`** (reserved for 15e sittings, §10), and never `--connected-device` alone.
- **(c) Real ad IDs.** The release build carries the real ad units (`docs/ad-testing-safety.md` §1), so it runs **only on an emulator or on a phone already registered as an AdMob test device** (§2–§3 there). Never tap an ad without the "Test Ad" label.
- Commands: `bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=.planning/qa/release-3.0.0/3.0.0.apks --device-id=<serial>` then `bundletool install-apks --apks=.planning/qa/release-3.0.0/3.0.0.apks --device-id=<serial>`. The `.apks` stays under `.planning/qa/`, never in `app/build/outputs/` (4.7). bundletool is not installed on this host yet (Homebrew `bundletool`). D-1 waits for the AAB (4.3, founder-blocked) and for 4.6's sha256.

| # | Item | Source | Phone needed? |
|---|---|---|---|
| D-1 | Palette + TalkBack 2-minute check on the release AAB (preconditions above): colour-blind palette on, TalkBack on, one game — read a cell, select a peg, reveal a hint, make one jump; selection ring, target marks, dashed hint ring draw; lines spoken. Failure = 3.0.0 defect | talkback sign-off §2 | no (emulator) |
| D-2 | The 6 tutorial step lines repeated (both routes) | talkback sign-off §3, D-TBe | phone if attached; else device3 stands |
| D-3 | L1-2: hardware keyboard with TalkBack on | talkback sign-off §3 | phone + keyboard; else Q-PUBLISH untested line |
| D-4 | CV-AC14: system colour correction with the game | colour-vision sign-off item 1 | no |
| D-5 | 23e device half (off-book Hint notice after a live Remove-animations switch) | RM sign-off | no |
| D-6 | One tap on the real launcher icon with a game resident | RM observation 3 | no |
| D-7 | English-33 in landscape, published 2.0 vs 3.0.0 | RM observation 2, §14.2 | no (needs the 2.0 build) |
| D-8 | Settings rows 0–13: colour-blind (4), Play Games (5–8), About (9), Licenses (10), Privacy (11–13) — present, ordered, TalkBack-reachable; Licenses opens offline, over the backdrop | colour-vision sign-off; candy `d0a65ae` | no |
| D-9 | Audio Q-010 branches D2–D4 (speaker mute, wired headset) | Q-025 | phone + hardware |
| D-10 | Cold start ≤ 2 s on a mid-range phone (D-03j, PLAT-08); emulator figures are candy's +44.5 ms TTID / +57.7 ms settle | candy final sign-off §4 | phone |
| D-11 | `compose-performance-audit` for the backdrop (rules/compose.md) — **closed by candy's J-4a/J-4b** (`android-game-dev-perf-audit.md`, ruled in `tech-lead-j4b-ruling.md`); not owed again | candy sign-off | — |
| D-12 | §5 store-copy items 5.0–5.10 (after upload) | checklist §5 | phone (founder's) |
| **D-13** | **§16 #30, the notice half:** with TalkBack on, raise an off-book Classic hint until the Undecided notice shows ("couldn't work it out"; R1's QA saw 17/17 off-book hints Undecided on API 36) — check 23j item 5's spoken order and 23d (the notice spoken). If it cannot be raised, the one Q-PUBLISH line "the notice's TalkBack traversal: tree-order and speech-output evidence only" applies | tech-design §16 #30; tech-lead docs sign-off Condition 3 | no (emulator `ergoz_studio`) |
| **D-14** | **§16 #30, the banner's TalkBack half:** at AC-19's English-33 dead position, Hint raises the "no winning move" banner and TalkBack speaks "No winning move — undo or restart" once (23k). The banner has no Dismiss, in spec or in code (23k: actions Undo, Restart, Home, unchanged); its traversal and its Undo and Restart were closed on row TB. The actions-menu Dismiss is the Undecided notice's and is checked under D-13 (23j item 5). *(Corrected 2026-11-01, tech-lead device-pass triage: until then this row asked for a banner actions-menu Dismiss, a wording error.)* | tech-design §16 #30; Condition 3 | no |
| **D-15** | Candy on the release build: backdrop behind Home, gameplay and the Licenses screens; fades in after Home's entrance; static with animations off (frame 3, no fade); no visible stall beyond the recorded one | candy final sign-off §4 | no |
| **D-16** | Store screenshots recaptured after candy (Q-016: plain captures; `THIRD_PARTY.md` U-5/§7) — ≥ 4 phone screenshots, en-US/tr-TR | THIRD_PARTY.md §6 U-5 | no (emulator capture), store use is the founder's |

### 3a. Device-pass results (2026-11-01)
Sources: `.planning/reports/release-3.0.0/qa-engineer-device-pass.md` (emulator `ergoz_studio`, no phone attached) and `tech-lead-device-pass-triage.md`.

| # | Result | Note |
|---|---|---|
| D-1 | **founder-blocked** | needs the AAB (4.3: F-7/F-8) and its sha256 (4.6) |
| D-2 | **unverified (no phone)** | device3 emulator evidence stands; Q-PUBLISH line |
| D-3 | **unverified (no phone)** | L1-2 untested; Q-PUBLISH line |
| D-4 | **PASS** | on form cues; not judged by eye |
| D-5 | **PASS** | |
| D-6 | **PASS** | |
| D-7 | **blocked** | no published 2.0 build on this host; 3.0.0 half captured (board clipped in landscape = B-10). Not compared; Q-PUBLISH line |
| D-8 | **PASS** | |
| D-9 | **unverified (no phone)** | Q-025 hardware branches |
| D-10 | **unverified (no phone)** | emulator figures only |
| D-11 | **closed earlier** | J-4a/J-4b |
| D-12 | **founder-blocked** | after upload |
| D-13 | **FAIL → 3.0.1, Minor (accessibility)** | focus leaves Hint for "Navigate back" (3 of 4); notice spoken once, order and Dismiss PASS. `bugfix/hint-focus` in 3.0.1; Q-PUBLISH line (triage §2.3). Folded into 3.0.0 only if a release-branch code loop opens anyway |
| D-14 | **PASS** (on the corrected wording above) | banner spoke once at 13:18:56.7; no re-run (triage §3) |
| D-15 | **PASS** (guarded re-measure, attempt 2; `qa-engineer-d15-remeasure.md`) | OFF median **465.5 ms** ≤ ON median **440.5 ms** + 100; slowest OFF **508 ms** < 2 s; no Davey in any of 60 launches, so no OFF frame ≥ 700 ms. Attempt 1 VOID (Guard 2 post-block gate, 8 of 12 ON launches); attempt 2 valid, 4 of 12 VOID per condition; nap guard never fired. The §1.4 probe stays VOID (App Nap). Merge to main unblocked (`tech-lead-d15-ruling.md`). Frame 1 with animations off ~50 ms longer by design (ruling §1.2). Other clauses PASS |
| D-16 | **drafts only** | 9 draft captures; final store screenshots are recaptured from the release candidate (after 4.3) |

## 4. T-items (play-upload) — status
Unchanged from the pre-cut revision: every T-item done except **T-8** (mapping check in 4.5, owed, post-build) and **T-1**'s act (the founder's).

## 5. 3.0.1 list (carried out of 3.0.0)
- **`bugfix/hint-focus` (D-13)** — android-game-dev, red first: one Hint `IconButton` call site, stable semantics node; Compose UI test on `SemanticsNode.id`; QA re-run of 23j item 5 bullet 1 (4 of 4). Owner tech-lead (triage §2.4); new tech-design §16 row.
- **A1 (Medium, tech-lead)** — the gate's plain `detekt` task runs no `RequiresAnalysisApi` rule (`standards.gradle.kts:34-39`, `:94`); `LongParameterList` is never run.
- **A2 (Low, developer on develop)** — remove the inert `@Suppress("LargeClass")` in `GameViewModelTelemetryTest.kt:94` and `GameViewModelAllowanceTest.kt:89`.
- **A3 (Low)** — `PlayGamesService.kt:117-118` KDoc says four private helpers; there are six.
- **A4 (Low, backlog, tech-lead)** — move the assist economy and ad-clock arithmetic out of `GameViewModel` into stateless delegates.
- **A5 (Medium)** — checklist 1.3 grep widened on `release/3.0.0` (this release); carried so `develop` gets it at the back-merge and the plugin's checklist template is updated (producer).
- **A6 (Info)** — one shared test helper for the nine `UNCHECKED_CAST` ViewModel factories (T7–T15).
- **B-10** — the board is clipped in landscape (D-7's 3.0.0 half shows it).

## 6. Studio/plugin follow-ups (not product 3.0.1; producer → 3.0.0 retro or a plugin branch)
- **Standard emulator-timing guards** (`tech-lead-d15-ruling.md` §4.1): every emulator timing session in the plugin's QA bench procedure, and every tech-lead probe pre-registration, carries Guard 1 (nap guard: no qemu thread at priority 4 from 2 s before `am start` to the log dump) and Guard 2 (absolute guest speed gate: 64 MiB sha256 ≤ 50 ms, three runs before and after each block). A speed control relative to the other condition is never the only guard.
- **App Nap on the studio Mac** (§4.2–4.3): either disable App Nap for the emulator only (`NSAppSleepDisabled` on its own bundle domain, verified by Guard 1), **with the founder's consent** (non-blocking Q, `Blocks: nothing` unless the re-measure is VOID twice), or a bench rule: timing only with the screen unlocked and the emulator window visible. `caffeinate` is not expected to help.
- **Speed-gate settle (QA, `qa-engineer-d15-remeasure.md` Open issue 1):** all 5 VOID blocks of the re-measure failed Guard 2's *post*-block gate (51–57 ms, first run right after the last force-stop, qemu at priority 31, i.e. launch residue, not a nap). Before Guard 2 is standardised, add a fixed settle (e.g. 10 s) before the post gate, or judge the median of the three runs.
- **Retro finding candidate** (§4.4): earlier "post-toggle slow regime" records (`candy-backdrop/qa-engineer-15e.md`, `qa-engineer-j5.md` attempt 1, the original D-15) may share the cause.
