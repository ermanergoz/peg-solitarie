# 3.0.0 release readiness — `release/3.0.0` cut, pre-device-pass state

Date: 2026-11-01. Author: release-manager. **`release/3.0.0` is cut** from `develop` = `da886ed`
(`feature/candy-backdrop` merged, the last branch-cut row), in the worktree
`/Users/ermanergoz/Desktop/PegSolitarie-release`. Not merged to `main`, not tagged, nothing uploaded,
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
| 1.3 | **reviewed, not ticked — to the code-auditor** | `git diff v1.0..HEAD -- '*/src/*'` grep: **44 hits**, read one by one. 12 `@Suppress` in `app/src/main`; 31 in tests (`!!` in test assertions, `UNCHECKED_CAST`, `unused` arch fixtures, test `LargeClass`/`LongParameterList`); 1 false positive (`SemanticsProperties.Disabled`). Approval lines found for the main-source ones in `ProgressRepository` (×2), `ProgressSubmitter`, `CloudProgressCodec` (phase-13 sweep, phase-15), `GameScreen` (hint-undecided, audio-review), `BoardCanvas` (colour-vision loop 0). **No approval line found** for `SnapshotMapper` `TooManyFunctions`, `GamesIds` `CyclomaticComplexMethod`, `PlayGamesService` `TooManyFunctions`, `GameViewModel` `TooManyFunctions/LongParameterList/LargeClass`, `ModeGlyphs` `MagicNumber` ×2, nor for the test-side `UNCHECKED_CAST`/`unused` set. The release-manager does not approve suppressions: one code-auditor pass over the list closes it |
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
| 2.4 | **owed at the merge to main** — `## [3.0.0] — unreleased` stays undated until the tag (the section's own rule); dated on the release branch right before step 5 |
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
| 3.5 | **partly done — per-coordinate diff owed (research-analyst)** — `:app:dependencies --configuration releaseRuntimeClasspath` exit 0: **191** resolved coordinates. Every non-AndroidX group appears in `THIRD_PARTY.md`; AndroidX falls under the §5b family row. okhttp **4.11.0** (row J's address holds). No `cronet-embedded` (cronet-api/common/fallback/shared 143.7445.0, Java-only). The diff against the 192 coordinates of the 2026-09-15 report needs that report's list in comparable form: the research-analyst's, which also closes §5b and U-6 |
| 3.5a | **owed (studio, post-build for its AAB half)** — NOTICE scan of the 191 artifacts not run in this pass |
| 3.5b | **done** — versions printed are 22.0.0 (games-v2), 1.4.0 (ads-mobile-sdk), 4.0.0 (UMP), 2.0.1 (hsdp), unchanged |
| 3.6 | **reviewed — no secret; one question to the producer** — outside `.planning/`: no key, keystore or credential (only test fixtures such as `"fixture-store-password"`, `/fixture/keys/upload.jks`, and docs prose). Inside `.planning/` reports: absolute paths naming the macOS user (`/Users/ermanergoz/…`, studio worktrees, one Downloads path) appear in many reports. Not a credential; whether "no path or account identifier in a tracked file" covers report provenance paths is for the producer to rule |
| 3.7 | **done** — `git status --porcelain` clean after the commit; `find` for `*.jks`/`*.keystore`/`keystore.properties` empty |

### §4 The artifact
| # | State |
|---|---|
| 4.1, 4.2 | **founder** (F-7, F-8) — no `PLAY_UPLOAD_*` variable is set on this host (names checked, no values read) |
| 4.3 | **founder-blocked** — `:app:bundleRelease` depends on `verifyReleaseSigning` and the real-ID gates, so it cannot run without 4.1/4.2. Not attempted |
| 4.1a, 4.4, 4.5, 4.6, 4.6a, 4.7 | **post-build** (T-8 for 4.5 still owed) |
| 4.8 | **post-build**; draft in `Q-PUBLISH-3.0.0-draft.md` (candy lines now measured) |

### §5 — **founder** + QA after upload. §6–§8 — post-release.

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
| **D-14** | **§16 #30, the banner's actions-menu Dismiss:** with the "no winning move" / hint banner showing, open TalkBack's actions menu on it and choose Dismiss; the banner goes and focus lands sensibly | tech-design §16 #30; Condition 3 | no |
| **D-15** | Candy on the release build: backdrop behind Home, gameplay and the Licenses screens; fades in after Home's entrance; static with animations off (frame 3, no fade); no visible stall beyond the recorded one | candy final sign-off §4 | no |
| **D-16** | Store screenshots recaptured after candy (Q-016: plain captures; `THIRD_PARTY.md` U-5/§7) — ≥ 4 phone screenshots, en-US/tr-TR | THIRD_PARTY.md §6 U-5 | no (emulator capture), store use is the founder's |

## 4. T-items (play-upload) — status
Unchanged from the pre-cut revision: every T-item done except **T-8** (mapping check in 4.5, owed, post-build) and **T-1**'s act (the founder's).
