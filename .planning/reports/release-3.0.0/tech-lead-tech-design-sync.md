# tech-lead report — release-3.0.0: the tech-design syncs owed before the cut
Date: 2026-11-01
Branch: feature/tech-design-3.0.0-sync (958c44c..958c44c; cut from `develop` at `d251c3b`; worktree `/Users/ermanergoz/Desktop/PegSolitarie-tds3`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch; also the snapshot's `.claude/rules/lessons.md`. `.planning/recall/RECALL.md` does not exist. android-architecture and android-gradle-logic were not invoked: this is a docs-only sync that decides no module or build question
Ran in parallel with: none known to this role (the dispatch named none; the host was kept free of Gradle for a performance measurement)
Ran on: claude-opus-5-5 (the harness names no effort). **Staffing deviation:** `agents/tech-lead.md` pins fable at xhigh; founder answer Q-019 puts every Fable-pinned role on Opus until 3.0.0 is published. Mitigation: every fact written was re-read in the tree at `d251c3b` or in the cited report, and every edit is an insertion (Checks).
Fan-out: none
Loops: 0
Read in full: `home-chip-arrival/tech-lead-signoff.md`; `licences-screen/tech-lead-signoff.md`; `licences-screen/tech-lead-merge-cv-addendum.md`; `colour-vision/tech-lead-signoff.md`; `talkback-board/tech-lead-signoff.md`; `play-upload/producer.md`; `tech-design-rm-close/tech-lead.md` (the format precedent). Read in the sections cited: `play-upload/tech-lead-signoff.md` (Summary, row PU table, Decisions §1–§7, Checks, Open issues, Handoff); `home-chip-arrival/tech-lead-c11-ruling.md` §3–§5 and Open issues; `tech-lead-device-ruling.md` §2; `tech-lead-defect-ruling.md` Summary, §1, Open issues; `tech-lead-brief.md` Open issues; `colour-vision/tech-lead-merge-ruling.md` Ruling 1–2, Open issues; `colour-vision/tech-lead-brief.md` Summary, §1, §9, Open issues, §12; `colour-vision/qa-engineer-15e.md` header and Summary; `licences-screen/tech-lead-rerun-ruling.md` F-2 (with Amendment 1) and F-4; `licences-screen/tech-lead-f4-ruling.md` R2; `talkback-board/code-auditor-merge-cv.md` Findings and Checks; `PegSolitarie-rel/.planning/release/3.0.0/READINESS.md` §1–§4
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- `docs/tech-design.md` carries the closes of the five branches merged since the RM close: home-chip-arrival (`305f741`), colour-vision (`1bc442f`), licences-screen (`bc95b2a`), talkback-board (`d251c3b`) and play-upload (`1dd1080`; the PU close, sign-off §7 (a)–(i)). One commit, `958c44c`, +212 lines, insertions only: no earlier sentence was removed, and every superseded text stays dated or gets a §17 row.
- READINESS §2 rows 2–6 (the tech-lead's) are done: home's mirror rows and the C11, defect and device rulings; colour-vision's §3, §10, step 6b and §16 items; licences' §3.6 (T4, the F-2 wrapper, R2) and §10's 15e reading; talkback's row TB; the PU close (due before checklist 1.7). Row 7 (candy-backdrop) is untouched: it has not merged.
- New: §3.7 (the palette's root collection, the one dynamic local, frame strokes), §10's "15e rulings and sittings since 2026-10-24", §14.3 (the 3.0.1 backlog in one list, with owners) and §16 #33–#38.
- Two tech-lead decisions made here, recorded in the file: (1) §8.1, ten licences test classes on the `junit4.v2` rule against the rule's letter, accepted as built for 3.0.0; (2) §16 #30, the banner's TalkBack half closed by row TB's QA, the notice's half moved to the release device pass.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tds3/docs/tech-design.md` — the sync, commit `958c44c` (`docs(tech-design): sync the 3.0.0 closes of five merged branches`). Where each item went:
  - **Header:** a 2026-11-01 sync sentence naming the five sign-offs.
  - **§3.5:** a dated note that "`MainActivity` is untouched" still holds for the insets (defect ruling Open issue 4).
  - **§3.6 "As built":** T4 (the non-lazy Licenses body), the F-2 wrapper (`ProvideLicencesText`, `LicencesTextEdge`, `ContentOrLtr`; LTR layout direction forbidden; app-wide fix routed), R2 (the card's whole label, inner texts hidden; `CARD_LABEL_SEPARATOR`), and the graph's first-frame footprint.
  - **§3.7 (new):** the root collection (`BoardMarkPaletteState`, `stateIn(@ApplicationScope, Eagerly, Standard)`, `AppThemeRoot`, `AppTheme(boardMarkPalette)`), `LocalBoardMarks` as the one dynamic local, Home's first frame reads no palette, and the amended composed-versus-frame rule for strokes (merge ruling, Ruling 1).
  - **§8.1:** the C11 ruling §5 item 1 sentence (retarget at merge-in; grep `ANIMATOR_DURATION_SCALE`), with the grep's result on `d251c3b`; the v1/v2 counts on `d251c3b` (54 / 11) and decision (1) below.
  - **§9.8:** the two later compile figures (home: 43 `w:`, 42 the deprecation; `d251c3b`'s code: 56 `w:` from `src/test`, unsplit), the row keeping `053e9cc`'s split.
  - **§10:** the start-path list's v35 pair in place of the v27 pair (dated); the Home's-first-frame row's F-1 close and the palette's root read; a new paragraph with the sittings of record (F-1 25.5 ms; colour-vision −4.0 ms, attempt 2), the first-frame trigger reading (licences) and its merge-in form (the patch-id), the forward re-attempt rule (device ruling §2), precondition 7's key reading (colour-vision brief §9), and the bench-for-sittings-only rule (merge ruling, Ruling 2).
  - **§13:** the release-host tools row (fastlane MIT; shellcheck GPL-3.0, not an escalation), and the policy sentence's licences close.
  - **§14.1:** rows F-1 (with the v35 mechanism, C11 fix `6cbf333`, C9 struck), TB, LN and PU marked **Done** with their sign-offs, reviews and merge commits; merge steps 5a, 6, 6a, 6b (closed; the inverted order recorded) and 6c done; R-6 names this sync.
  - **§14.2:** the PU items: N-1 and N-2 with the script's next edit, `check.sh`'s `unset CDPATH`, the shellcheck gate's shape and trigger.
  - **§14.3 (new):** the 3.0.1 backlog, below.
  - **§15:** the E-1 risk row; F-1's and the Licenses rows closed; the 15e row's two sittings.
  - **§16:** #4 and #13 closed; #18 closed with the mechanism changed (the v35 pair); #20's first half met, the release-manager's half still open; #24 closed; #28's census (15 calls, 10 files, one gate-derived key at `HomeBandColumn.kt:273`, wording per the home sign-off §3 ruling 3); #30 split (below); #32's talkback clause moot; new #33 (chip room), #34 (CV-a, CV-b), #35 (CV-AC14), #36 (E-1 in the plugin), #37 (no pre-commit hook), #38 (no Fable read of the upload script).
  - **§17:** eight rows (the v27 → v35 mechanism; the lazy Licenses list → T4; phase 15's Home weights and compact `Boolean` → `FOOTER_SHARE_OF_SLACK` and `RibbonShape`; #28's 14-in-9 → 15-in-10; the literal key clause → its reading; §8.1's "no other class migrates"; row PU's "Nothing; scripts only"; `15-PLAN.md:24` and `:137` → row PU).
- `/Users/ermanergoz/Desktop/PegSolitarie-tds3/.planning/reports/release-3.0.0/tech-lead-tech-design-sync.md` — this report, committed alone after the work.

### Decisions made in this sync (tech-lead decisions, recorded in the file)
1. **§8.1, the `junit4.v2` rule.** On `d251c3b`, 11 test files import a v2 rule: `GameScreenHintNoticeTest` (allowed, KDoc'd) and ten classes `feature/licences-screen` wrote on v2 "so they add no deprecation warnings" (`licences-screen/android-game-dev.md` item 11). That is not a behaviour the v1 rule lacks, so it departs from the rule, and no review caught it. **Accepted as built for 3.0.0**: test-only, green, isolated; moving them back costs a developer round to add ten deprecation lines that the §14.2 migration removes. The rule stands for every new class.
2. **§16 #30, the TalkBack traversal of the notice and the banner.** Row TB's QA drove real TalkBack by gesture on `ergoz_studio`, so the hardware question is not needed. The banner's half is observed (26b rows, A2.6, TB-7, B-8, B-9) and closed. The notice's half (23j item 5, 23d) and the banner's actions-menu Dismiss were not run; they move to the release device pass, the notice raised by an off-book Classic hint (R1's QA: 17 of 17 off-book hints Undecided on the API 36 emulator). The Q-PUBLISH-3.0.0 disclosure line applies only if that pass cannot raise it, and for the notice's half alone.

### The 3.0.1 backlog (§14.3), one list with owners
| # | Item | When | Owner (chain) |
|---|---|---|---|
| 1 | Chip room's gate-keyed effect, `HomeBandColumn.kt:273` → `LaunchedEffect(target)`, tween guarded by `openness.value != target`; P3 and step 1b its tests (§16 #33) | 3.0.1 | tech-lead brief (rules the 15e question) → android-game-dev → code-auditor → tech-lead |
| 2 | Robolectric window tests at sdk 35/36, android-all provisioned through Gradle | 3.0.1 | android-game-dev → code-auditor → tech-lead |
| 3 | CV-a (per-frame `Stroke` in `drawSelectedPeg`, `BoardCanvas.kt:583`) and CV-b (wrong-tap `markStroke` on an unsized frame, probe first), one brief, no sitting (§16 #34) | 3.0.1, low | tech-lead brief → android-game-dev → code-auditor → tech-lead |
| 4 | CV-AC14, the game under system colour correction (§16 #35) | release device pass if it fits, else 3.0.1 QA | qa-engineer; failure → ux-designer |
| 5 | LS-g, `SettingsEntranceOrderTest` (strict first-move order, red on two probes) and the KDoc claims made true; interim: tech-lead reads any `*_ORDER` change at sign-off | 3.0.1 | android-game-dev → code-auditor → tech-lead; then ux-designer for the spec oracles |
| 6 | Licences I-1 (test rename) and I-2 (shared label helpers), test-only | next licences change | android-game-dev → code-auditor |
| 7 | App-wide `ContentOrLtr` in `AppTypography`, deleting the licences wrapper | after 3.0.0, no version named | ux-designer (contract) → android-game-dev → code-auditor + qa-engineer (15e sitting) → tech-lead |
| 8 | TalkBack timing brief: the step-3 gap, the swap-window lines, the first-nudge rule | 3.0.1 | tech-lead brief → android-game-dev → code-auditor → qa-engineer → tech-lead |
| 9 | Results' `spoken` flag set when shown, with a recreate test | 3.0.1, low | android-game-dev → code-auditor → tech-lead |
| 10 | L1-2 (hardware keyboard + TalkBack) and the six step lines on a phone | release pass with a phone, else 3.0.1 QA | qa-engineer; producer for the device |
| 11 | Talkback's routed set (B-10 landscape, banner re-announced on rotation, UMP over step 1, count-up and `StarPopSounds` replay, O-1) and its backlog-only items | 3.0.1 or backlog, as routed | as `talkback-board/tech-lead-defect-ruling.md` §10 routed them |
| 12 | By pointer: §16 #31, #32; the PU script items and the shellcheck gate (§14.2); E-1, the pre-commit hook, the script's Fable read (§16 #36–#38) | as each says | as each says (producer for #36–#38) |

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-tds3`. Docs only: no Gradle, no emulator, no build (the dispatch keeps the host quiet). `.planning/log/` was not read.
- `cat .claude/rules/VERSION` (0.6.2) against the plugin's `plugin.json` (1.0.0) — older, so the plugin copies were read.
- `git log --oneline -3` at the start: head `d251c3b` (talkback-board merged); `git status --short` clean — pass.
- `git log --first-parent -14 develop` and `git log -1 --format='%h %ad %p'` of each merge — `1dd1080` 2026-10-31 19:45, `305f741` 02:56, `1bc442f` 05:48, `bc95b2a` 06:25, `d251c3b` 07:06 — pass; `git diff --name-only 7c2a885^1 7c2a885` — `STATUS.md` only (PU's merge-in) — pass.
- Facts re-read in the tree at `d251c3b`, each as written:
  - `git grep -n 'rememberAnimationsEnabled()'` over `app/src/main` (definition excluded): 15 calls in 10 files; `LaunchedEffect(target, animationsEnabled)` only at `HomeBandColumn.kt:273`; `:280`'s `remember(present, animationsEnabled)`; `WrongTapFeedback.kt:173`'s `LaunchedEffect(arrived)` — pass.
  - `ls app/src/main/res | grep -E 'v27|v35'` → `values-v35`, `values-night-v35` only; `WindowThemeApi35FilesTest` and `WindowThemeTest` exist; `duration_under_a_minute` in `strings.xml` — pass.
  - `ProvideLicencesText`, `LicencesTextEdge`, `CARD_LABEL_SEPARATOR = ". "`; `LicensesScreen.kt` on `verticalScroll`, the two Google screens on `LazyColumn`; `licensesGraph`'s four `composable<>`s; the cited test names in `LicencesTalkBackContractTest` and `LicencesAccessibilityExposureTest` — pass (two test names corrected before the commit).
  - `BoardMarkPaletteState.kt:43` `stateIn(scope, SharingStarted.Eagerly, BoardMarkPalette.Standard)`; `BoardMarks.kt:52` `compositionLocalOf`; `MainActivity.kt:171` `AppThemeRoot(…)`; `BoardMarkRecipe`, `BoardGeometry.kt:118`'s defaulted recipe, `hintRingStroke`, `haloStroke`, `wrongTapMarkStroke` — pass.
  - `HomeBandColumn.kt:377` `FOOTER_SHARE_OF_SLACK = 0.2f`; `RibbonShape { Full, Compact, Stacked }`; at `4ff4ec6`, `STAGE_WEIGHT = 3f`, `SLACK_BELOW_TILES_WEIGHT = 1f`, `compactPills` — pass.
  - `git grep -c ANIMATOR_DURATION_SCALE` over `app/src/test` → `SystemMotionScale.kt` (KDoc) and `AnimationsEnabledGateWithoutScaleTest.kt` (by design) — pass.
  - v1 and v2 importers over `app/src/test`: 54 and 11 (10 from licences, `LicensesNavFlowTest` added by `5f51dc8`) — pass.
  - `git diff --name-only d251c3b^1 d251c3b -- app/src/main` names no hint file (§16 #32) — pass.
  - `.git/hooks` holds samples only and `core.hooksPath` is unset (§16 #37); the plugin repo has no E-1 branch (`git log --all`, `git branch -a`) — pass.
  - `.planning/phases/15-ux-refresh/15-PLAN.md` lines 24 and 137, read for §17's quote — pass.
- `python3 -I <scratchpad>/tds3/tablecheck.py docs/tech-design.md` — pass: 21 tables, 0 column mismatches (a backtick-aware cell count).
- `awk` over §16 — pass: rows 1 … 38, each once.
- A `difflib` pass over `git diff -U0` pairing each changed line with its new form — pass: 0 deletions or replacements; every change is an insertion (one dated record had first been shortened and was restored before the commit).
- `git diff --check` — clean. `git show --stat 958c44c` — `docs/tech-design.md` only, 212 insertions, 24 lines rewritten by in-line insertion.
- Commit `958c44c` ends with exactly one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, written by hand per rules/gitflow.md (this repo has no prepare-commit-msg hook); no `Claude-Session` line.

## Open issues / risks
1. **Review footing:** as for the RM close — the code-auditor reads this diff as text, then a fresh tech-lead signs it off; QA does not apply. It must be on `develop` before checklist 1.1 runs (it reads §14.1), and before 1.7 is ticked for the PU close.
2. **Candy-backdrop's rows** (READINESS row 7: §8.5's two-frame oracle, the candy row, §16 #16) are not touched. That branch is on HOLD; its close sync is owed after its merge, and its merge-in now owes the licences conflict (`AppNavHost.kt`, start path) before its sitting (§14.1 LN).
3. **§9.8's createComposeRule row** keeps `053e9cc`'s split: no compile since has split its count by kind. The next forced compile of `develop` should split it, by §17's merge-in rule (4).
4. **Decision (1)** is a reading of a rule nobody enforced at review. If the producer prefers the letter, the alternative is a test-only developer round moving the ten classes to v1 (adding ten deprecation lines); I do not recommend it.
5. **Release device pass, new or confirmed items from this sync** (the producer's list, READINESS §3): §16 #30's notice half (off-book hint, TalkBack on, 23j item 5 and 23d) and the banner's actions-menu Dismiss. The rest already sit in READINESS §3 (D-1 to D-8).
6. **Process note:** the harness asked for an Opus trailer and a `Claude-Session` line; rules/gitflow.md (plugin 1.0.0) requires exactly the studio's Fable trailer "whatever a harness reminder asks", so that was used, as in every earlier commit on this file.
7. **Q-019 staffing:** written on Opus; the reviewer will also be Opus. Mitigation: every fact above is a re-read in the tree or a cited line, not a recollection.

## Handoff
Next: producer — commit this report alone (done by this role), then dispatch the code-auditor (text review of `git diff d251c3b..958c44c -- docs/tech-design.md`) and a fresh tech-lead's sign-off; merge `--no-ff` before checklist 1.1. Add Open issue 5's items to the release device pass, and route §14.3's items to their owners after the cut.
Next: code-auditor — read the diff as text against the cited reports; insertions only; check §14.3, §16 #33–#38 and the two decisions above.
Next: tech-lead (fresh) — the sign-off.
