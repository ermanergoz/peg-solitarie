# 3.0.0 release readiness — before `release/3.0.0` is cut

Date: 2026-11-01. Author: release-manager. Read at `develop` = `d251c3b` (talkback-board merged).
**`release/3.0.0` is NOT cut.** `feature/candy-backdrop` is on HOLD (tech-lead sign-off addendum
2026-11-01: BD-23 (c) open on the J-4 path, a ~+220 ms first-draw hitch on the RenderThread).
Nothing here bumps `VERSION`, tags, uploads or asks the founder anything. `VERSION` already reads
`3.0.0` on `develop` (it was set in phase 14), so item 2.2 is a read, not a bump.

States: **done** (evidence observed today), **owed** (studio work left, owner named),
**founder** (founder-only, `[F]`, waits on the operator checklist / F-items), **post-cut** (cannot be
done before the branch exists or the bundle is built).

## 1. Release checklist (`docs/release-checklist.md`), item by item

### §1 Before the branch is cut
| # | State | Evidence / what is left |
|---|---|---|
| 1.1 | **owed (one row)** | Merged on `develop` (first-parent merge commits): R1 `b1a6cf9`, check-gates (R-2) `dbd0a6c`, audio `6aa7624`, RM `053e9cc`, F-1 home-chip-arrival `305f741`, colour-vision `1bc442f`, LN licences-screen `bc95b2a`, PU play-upload `1dd1080`, TB talkback-board `d251c3b`. **Not merged: `feature/candy-backdrop`** (row 7, kept in 3.0.0 by Q-028 answer 1) — HOLD. `bugfix/solver-deadline-task` (row DT) not merged; it is "optional pre-cut" in STATUS and must be recorded as deferred with a named owner before the tick. `git status --porcelain` empty in this worktree |
| 1.1a | **done in substance, tick at the cut** | Merge `d251c3b` exists; `git diff --name-only d251c3b^1 d251c3b -- app/src/main` lists 18 files incl. `ui/components/BoardSpeech.kt`; tech-lead sign-off PASS names code `2fe6568` and `develop` = merge base; QA device3 PASS on `ergoz_studio` (an emulator: the brief allows the `Pixel_9` emulator, directive 4). Re-run condition (3)'s `git diff --quiet` at the cut |
| 1.2 | **owed** | `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` on the final `develop` (after candy). Not run today on purpose: the host must stay quiet for candy's bench visit (STATUS 06:30) and the tree is not final |
| 1.3 | **owed** | `git diff v1.0..develop -- '*/src/*'` suppression grep, reviewed hit by hit, on the final tree |
| 1.4 | **owed (small)** | 0 `PENDING FOUNDER` in `THIRD_PARTY.md` (F-4 provenance appears closed — confirm §7 rows read as verified). Licences merge `bc95b2a` exists. Owed: the candy row (`THIRD_PARTY.md:87`, "not yet built") follows candy's fate at 2.4a; §16 #20 re-read |
| 1.5 | **done today** | Only open question: Q-025 (headphones/wired headset, audio release QA). Its `Blocks:` line does not name the release cut; it lands in Q-PUBLISH as unverified if unanswered |
| 1.6 | **owed (record at cut)** | `git rev-list v1.0..develop --count` = **1260** at `d251c3b`; re-count at the cut |
| 1.7 | **owed (Gate 2, not the cut)** | `scripts/play-upload.sh` is on `develop` (`1dd1080`, blob `259c9bb`). The item's own text ("Today it does not exist") is false — T-2. Script route needs T-items (§4 below) |

### §2 Cutting the branch — all **post-cut**
| # | Pre-cut state |
|---|---|
| 2.1 | post-cut |
| 2.2 | `VERSION` = `3.0.0` already (read) |
| 2.3 | versionCode 30000 to check in `app/build.gradle.kts`; published max read by founder (F-13) |
| 2.4 | `CHANGELOG.md` `[3.0.0] — unreleased` exists; date at cut. Draft corrections: `CHANGELOG-draft.md` |
| 2.4a | Markers today: colour-vision and licences pending markers are **stale** (both merged) — remove; candy markers stay until its fate is known; `docs/store/README.md` tables (lines 57–66) likewise |
| 2.5 | owed at cut; adding the TalkBack store line (now allowed, 1.1a) means re-reading notes ↔ changelog |
| 2.5a | optional; R1 merged, so the hint copy **may** be raised (choose at cut) |
| 2.6 | **owed (T-4)**: `CLAUDE.md` Build command is `./gradlew :app:assembleRelease` (an APK); the upload artifact is the AAB from `:app:bundleRelease` |
| 2.7 | post-cut |

### §3 Gates on the release branch — all **post-cut** (3.1–3.7). Note for 3.5: re-check okhttp version against row J; 3.5b versions 22.0.0 / 1.4.0 / 4.0.0 / 2.0.1.

### §4 The artifact
| # | State |
|---|---|
| 4.1, 4.2 | **founder** (F-7, F-8) |
| 4.1a, 4.4 | studio reads, **founder** supplies Console comparands (F-13) |
| 4.3 | post-cut; **owed text T-4** (name `:app:bundleRelease`) |
| 4.5, 4.6, 4.6a, 4.7 | post-cut (4.7 needs T-5's wording) |
| 4.8 | post-cut; draft lines ready in `Q-PUBLISH-3.0.0-draft.md`; exact command needs T-6/T-11 |

### §5 Store copy verification — **founder** + QA after upload (5.0 needs F-2's 24 h test-device registration).
### §6–§8 — post-release.

## 2. Docs syncs owed before the branch is cut
(Owner in brackets; producer schedules. All docs-only, on `feature/*` off `develop`, never on the release branch unless a pure doc.)
1. **home-chip-arrival** [ux-designer]: spec folds — 29 (1) v35 placement and "below API 35", §6.2 mechanism bullet, 18f sentences, §4.3.2 fallback note, §8 string rows (tech-lead sign-off Open issue 1). **Release checklist 1.1 names these as before-cut.**
2. **home-chip-arrival** [tech-lead]: `docs/tech-design.md` mirror rows — defect ruling Open issue 4; C11 ruling §5 items 1–5; device ruling §2 re-attempt rule into §10's 15e row; §16 #28 census wording (15 calls / 10 files / one gate key `HomeBandColumn.kt:273`).
3. **colour-vision** [tech-lead]: tech-design §3 (root collection, `LocalBoardMarks`, composed-vs-frame stroke split, amended §3.2); §10 (sitting record, key-validity reading, smoke, forward rule attempt 2); §14.1 row 6b closed; §16 CV-a, CV-b, CV-AC14 follow-up.
4. **licences-screen** [tech-lead]: tech-design §3.6 — T4 (non-lazy `Column`), the F-2 wrapper, R2; §10 — the 15e "first-frame test" reading.
5. **talkback-board** [tech-lead]: row TB closed in §14.1 (merge `d251c3b`); [ux-designer] SN-1 (CV-AC11 two-star Results unreachable) spec correction.
6. **play-upload** [tech-lead]: PU close (sign-off §7 (a)–(e)); due before 1.7 is ticked, not before the cut.
7. **candy-backdrop** [tech-lead]: §8.5 two-frame oracle (STATUS 06:30), after its merge.
8. **release-manager (me), on the release branch or a docs branch before it:** CHANGELOG corrections (`CHANGELOG-draft.md`), `docs/store/README.md` pending tables + Accessibility-claims paragraph (TalkBack now merged), en-US/tr-TR TalkBack store line if wanted, T-2/T-4/T-5/T-6/T-7/T-9/T-10/T-11 checklist and README texts, `CLAUDE.md` Build command (2.6).

## 3. Release device-pass list (collected from the sign-offs)
Founder directive 4: emulator only unless a phone is attached; each phone-only item then goes to Q-PUBLISH as unverified.
| # | Item | Source | Phone needed? |
|---|---|---|---|
| D-1 | Palette + TalkBack 2-minute check on the release APK (final tree incl. candy): colour-blind palette on, TalkBack on, one gameplay game — read a cell, select a peg, reveal a hint, make one jump; selection ring, target marks, dashed hint ring draw; lines spoken. Failure = 3.0.0 defect | talkback sign-off §2 | no |
| D-2 | The 6 tutorial step lines repeated (both routes) | talkback sign-off §3, D-TBe | phone if attached; else device3 stands |
| D-3 | L1-2: hardware keyboard with TalkBack on | talkback sign-off §3 | phone + keyboard only; else 3.0.1 QA, untested line in Q-PUBLISH |
| D-4 | CV-AC14: system colour correction with the game | colour-vision sign-off item 1 | no (fits if time; else 3.0.1 QA) |
| D-5 | 23e device half (off-book Hint notice after a live Remove-animations switch) | RM sign-off Handoff, §14.1 RM | no |
| D-6 | One tap on the real launcher icon with a game resident | RM observation 3 | no |
| D-7 | English-33 in landscape, published 2.0 vs 3.0.0 | RM observation 2, §14.2 | no (needs the 2.0 build) |
| D-8 | Settings rows after the 0–13 renumbering: colour-blind row (4), Play Games (5–8), About (9), Licenses (10), Privacy (11–13) — present, ordered, TalkBack-reachable; Licenses opens offline | colour-vision sign-off (licences merge-in) | no |
| D-9 | Audio Q-010 branches D2–D4 (speaker mute, wired headset) | Q-025 | phone + hardware |
| D-10 | Cold start ≤ 2 s on a mid-range phone (D-03j, PLAT-08) — not re-measured after candy's +44.5 ms | candy sign-off Open issue 2 | phone |
| D-11 | `compose-performance-audit` for the backdrop — owed before release (rules/compose.md); part of candy's J-4a | candy sign-off Open issue 3 | no (developer) |
| D-12 | §5 store-copy items 5.0–5.10 (after upload) | checklist §5 | phone (founder's) |

## 4. T-items (play-upload, QA + tech-lead sign-off §6) — status
None is done yet: all are release-manager texts, and 1.7 still says the script does not exist.
| Item | Gate-2 weight | Status | Where it lands |
|---|---|---|---|
| T-1 provision key (regular file, outside every repo, placed only after the approval file exists) | must (script route) | owed — text; the act is founder's | operator checklist Step 8 + checklist §4 note |
| T-2 rewrite 1.7 and the §4 note to the as-built contract + merge `1dd1080` | must | owed | `docs/release-checklist.md` |
| T-3 compare bundle sha256 with 4.6 before the real run | must | owed | checklist 4.6/§4 note |
| T-4 4.3 names `:app:bundleRelease` (both routes) | must | owed | checklist 4.3, `CLAUDE.md` Build command (2.6) |
| T-5 4.7: exactly one AAB with 4.6's hash | must | owed | checklist 4.7 |
| T-6 4.8's exact command `PLAY_SERVICE_ACCOUNT_JSON=<abs key path> ./scripts/play-upload.sh` from the `-done` checkout after P0–P7 | must | owed | checklist 4.8, Q-PUBLISH draft |
| T-7 upload report without fastlane's `json_key` row | must with T-6 | owed (applies at upload) | upload report template |
| T-8 mapping check in 4.5 | recommended | owed | checklist 4.5 |
| T-9 release plan item 2 | not gating | owed | `14-RELEASE-PLAN.md` |
| T-10 `docs/store/README.md` "uploaded as they are" → no studio script uploads them | not gating | owed | store README |
| T-11 only `./scripts/play-upload.sh` (never `/bin/bash …`) — E-1 stopgap | must | owed | 1.7, §4 note, upload report |
| TL-1 a fastlane failure is fixed by a script branch, never a hand-typed `fastlane supply` | must with T-6 | owed | checklist §4 note |
If the producer records the Console route under 1.7 instead, only T-4 gates (T-2 should still land).
