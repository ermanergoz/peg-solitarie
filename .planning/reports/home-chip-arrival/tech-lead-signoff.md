# tech-lead report — home-chip-arrival (final technical sign-off)
Date: 2026-11-01
Branch: bugfix/home-chip-arrival (report only; read and checked at `876258e`; last code commit `6cbf333`, last APK-changing commit `5708d95`; merge-base with `develop` `f7dd1fe`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, stacks, testing, dispatch; logging, puzzle-design and play-store are identical in the snapshot and were read there; plus the worktree's `.claude/rules/lessons.md`
Ran in parallel with: licences F-4 ruling, candy 15m oracle spec
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** `agents/tech-lead.md` pins fable at xhigh. Founder answer Q-019 (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published, and the founder's 2026-10-31 directive (1) runs every dispatch one effort step below its pin (xhigh → high); the harness names no effort. Mitigation: every fact below was re-read in the tree or re-run (Checks); no reviewer's figure is taken on trust where a command could confirm it.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/home-chip-arrival/tech-lead-defect-ruling.md`, `tech-lead-c11-ruling.md`, `tech-lead-device-ruling.md`, `tech-lead-brief.md`, `code-auditor.md` (FAIL, then its loop 1 PASS), `code-auditor-loop2.md`, `code-auditor-loop3.md`, `qa-engineer-device2.md`, `qa-engineer-sitting2.md`, `android-game-dev-loop2.md`, `android-game-dev-merge-c11.md`, `android-game-dev-c11-fix.md`. In part: `qa-engineer.md` (header, Summary, verdict: the first sitting, superseded as the sitting of record), `qa-engineer-device.md` (header, Summary, table: FAIL, superseded by session D), `android-game-dev.md` (header, Summary, commit table); `.planning/reports/STATUS.md` on `develop` (`09e542e`, the founder's 2026-10-31 directives)
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Every condition of the defect ruling's §4.5 (as extended by the C11 ruling's Open issue 4) is met.** The auditor passed loops 1, 2 and 3; session D passed 29 (2) on API 36, the below-35 read (B-1b), 18g (a)–(c) and 26b; session S passed the 15e re-sit (delta 25.5 ms, floor 18.5 ms, threshold 100 ms), 15v, 15w and the bench 29 read. The superseded FAILs (auditor round 0, device session 1) are each closed by a later PASS.
- **The code that merges is the code that was measured.** `git diff --quiet 5708d95 HEAD -- . ':(exclude)app/src/test' ':(exclude).planning'` exits 0, and `develop` has gained only `scripts/play-upload.sh` (not an APK input, not start-path) since arm A `f7dd1fe`. A trial merge into `develop` `f918793` is clean.
- **The diff keeps to the design:** `app/src/main` lists exactly the brief's files less C9 (struck) with the v35 pair in place of the v27 pair; every frozen file has no diff; no suppression, `@Ignore`, `!!` or dormant string; one trailer per commit.
- **My Check is green** at `876258e`: exit 0, 1,262 + 294 + 55 tests and the fuzz and depth-sweep runs with 0 failures, lint 0 errors.
- Four small rulings the reviewers asked for are made here (§3). **VERDICT: PASS** — the branch may merge to `develop`, `--no-ff`, at step 6 of §14.1.

## 1. The §4.5 conditions, one by one
| Condition (defect ruling §4.5; C11 ruling Open issue 4) | Met by | Holds |
|---|---|---|
| `code-auditor.md` — first review of the brief's C1–C8 | round 0 FAIL on finding 1 (a leaving chip still live), findings 2–4; its loop 1 (`c388993..1d63674`) closes all four with probes, own `--rerun-tasks` Check | yes, PASS |
| `code-auditor-loop2.md` — F1–F3 against defect ruling §4.2 | frozen files empty, guards hold (P5, P6), no dormant string or type, 18f matches §2's table, both v35 comments present; the four ruled probes red each on its own test; own `--rerun-tasks` Check 1,251/0 | yes, PASS |
| `qa-engineer.md` — the first sitting (superseded as the sitting of record) | 15e PASS at `7448713`; its device checks were BLOCKED and then run by session 1 | superseded by session S, as ruled |
| `qa-engineer-device.md` — FAIL (superseded) | B-1, B-2 (18g), B-3 (26b) found; ruled in the defect ruling | each closed by session D below |
| `android-game-dev-loop2.md` | F1 `925f36f`, F2 `88c52cb`, F3 `1e2bc3d`, each red first with the red quoted; main form of §3 ran; §3 (b) ran on Robolectric; the file list is exactly §4.1's | yes |
| `android-game-dev-merge-c11.md` and `tech-lead-c11-ruling.md` | merge `5708d95`, no hand-resolved hunk; the two red Home tests diagnosed as test-setup, not production | yes |
| `android-game-dev-c11-fix.md` | `6cbf333`: two test files only, assertions unchanged, 15/15 green, probe 2 of 15 red, APK preserved first | yes |
| `code-auditor-loop3.md` — §2 (a)–(d), P1–P3 | (a) the fix is §1's table; (b) merge tree = `merge-tree`, patch-ids intact; (c) census 15 calls in 10 files, one gate-keyed effect (`HomeBandColumn.kt:273`), P3 green both parts, so C11 §3 stays non-gating; (d) own `--rerun-tasks` Check 1,262/0, 43 `w:` lines, 0 from `src/main` | yes, PASS |
| `qa-engineer-device2.md` — session D (defect ruling §4.4 items 1–4) | 29 (2) API 36 dark three-button: scrim L 0.010, buttons 17.36:1, no `LIGHT_NAVIGATION_BARS` or `FORCE_LIGHT_NAVIGATION_BARS` on Home, gameplay, tutorial; arm A still shows B-1. Below 35 (API 27): arm B pixel-identical to arm A in both modes; old arm B shows B-1b at 1.10:1. 18g (a), (b) (up to 42 s of rest), (c) one utterance at the boundary, light and dark. 26b: back → title → timer → Undo → Hint → Show moves → Restart on Classic and Challenge, light and dark; Home and tutorial as before | yes, PASS |
| `qa-engineer-sitting2.md` — session S (§4.3, §4.4) | 15e re-sit PASS first attempt, all nine preconditions with values, P-1.2 clean, one emulator on 37/37 readings, arms checked by sha256 on 20/20 swaps; 15v eight recordings PASS (parts 1, 2 incl. the closing chip's shadow, 4); 15w PASS light and dark; 29 (2) dark three-button Home PASS on the 36.1 bench; bench restored and shut down | yes, PASS |
| Sequencing: session S only after loop 3's PASS (C11 §4) | loop 3 ended 2026-10-31 20:13; session S's sitting ran 21:41–21:48 on a quiet host | yes |
| Arm identity (C11 §4; device ruling C-1/C-2) | arm B `5708d95`, sha256 `3f66c5b6…21332f7`, installed and checked in sessions D and S; my diff condition re-run exits 0 at `876258e` | yes |
| Any FAIL in session D comes back before the sitting (§4.4) | none filed | yes |

**What I checked in the diff myself** (adherence to the brief, the defect ruling and the C11 ruling):
- `git diff --name-only develop...HEAD -- app/src/main | sort` lists `BoardCanvas.kt`, `ElapsedTimeSpeech.kt`, `GameStatusStrip.kt`, `HomeBandColumn.kt`, `HomeBandSet.kt`, `HomeProgressRibbon.kt`, `HomeScreen.kt`, `HomeStage.kt`, `values-v35/themes.xml`, `values-night-v35/themes.xml`, `values/strings.xml`. That is brief §4's list less `Clay.kt` and `Theme.kt` (C9 struck), with the defect ruling's v35 pair in place of the v27 pair. No v27 folder exists.
- The frozen list (`TimerDigits.kt`, `TimerDigitsTest.kt`, `GameScreen.kt`, `MainActivity.kt`, `ui/theme/`, the base theme files, the manifest, `dimens.xml`, `HomeViewModel.kt`, `HomeUiState.kt`, `HomeRoute.kt`, `HomeHero.kt`, `HomeBlobs.kt`, `HomeBoardArt.kt`, `BoardGeometry.kt`, every Gradle file, `build-logic`, `THIRD_PARTY.md`, `domain`, `app/schemas`) has an empty diff. No dependency is added.
- `BoardCanvas.kt` has two hunks: a one-line composition capture (`val geometrySize = canvasSize`) and the draw-lambda geometry. The auditor accepted the capture in round 0 (Deviation 1: the brief's literal draw-time read stays red). I agree: nothing reaches `pointerInput`, the `LaunchedEffect`s or `drawBoard`.
- The timer: `TimerPill` passes `Modifier.clearAndSetSemantics { contentDescription = description }` (`GameStatusStrip.kt:251`); `SpokenDuration` is `Minutes` or `UnderAMinute`; `strings.xml` adds only `elapsed_time_spoken`, `duration_minutes` and `duration_under_a_minute`; no `duration_seconds`, `duration_minutes_seconds`, `MinutesAndSeconds` or `secondsSaid` outside `.planning/` and `docs/`.
- The v35 pair: each restates `Theme.PegSolitaire` on `android:Theme.Material.Light.NoActionBar` with exactly the three ruled items and the "do not remove" comment.
- 34 commits on the branch, each with exactly one `Co-Authored-By` line and no `Claude-Session` line.

## 2. Boundaries and dependency rules
- Game logic stays in `:domain`: no diff there; the branch is UI, resources and tests only.
- State hoisting holds: Home's band column is a measure policy over hoisted `HomeUiState`; the ViewModel, its reads and its `StateFlow` have no diff (brief §4: no new blocking read, no Home-wide loading state).
- Home's first frame gained only what the brief allowed (the measure policy, three label measurements); C9's observer was struck. The 15e re-sit measured it: +25.5 ms against a 100 ms threshold.
- No reflection, `!!`, `@Suppress`, baseline or `@Ignore` in production or tests (grep over `develop...HEAD -- app domain` empty).
- The one known deviation from tech-design §16 #28 (`HomeBandColumn.kt:273`, an effect keyed on a gate-derived value) is the recorded, non-gating C11 §3 item; P3 shows it strands no animation.

## 3. Rulings owed to the reviewers (tech-lead decisions)
1. **The test compilation's `w:` figure is 43** (code-auditor loop 3 I5; developer C11-fix Open issue 1). It comes from a full `compileDebugUnitTestKotlin --rerun`: 42 v1 `createComposeRule` deprecations and one reflection warning, 0 from `src/main`. The merge-in report's 26 came from an incremental compile and is not a baseline. Later comparisons on this tree use a full compile.
2. **The 3.0.1 brief for the chip room's gate-keyed effect** (C11 §3; loop 3 I1, I2) commits P3's two parts and the auditor's step 1b (switch back on, every frame equals the settled one) as that item's tests, and adopts the shape probe H showed green: `LaunchedEffect(target)` with the tween guarded by `openness.value != target`, the fraction still reading `target` while animations are off.
3. **The close sync's census wording for §16 #28** (loop 3 I3, I4): the census counts effects only. `HomeBandColumn.kt:280`'s `remember(present, animationsEnabled)` is named as a `remember` and not counted; `WrongTapFeedback.kt:173`'s `LaunchedEffect(arrived)` is memoised on `state.rejectedTapTick` and is not a gate key. The count stays 15 calls in 10 files, one gate-derived effect key (`:273`).
4. **The observations are accepted, with no further evidence asked** (founder directive 2, reduced QA):
   - session D O-1: on API 36 in light three-button mode the platform's contrast scrim (`(254,254,255)`, buttons 5.70:1) replaces the `#fafafa`-derived one. Defect ruling §1 predicted it; 29 (2)'s light case passes.
   - session D I4: the dark-mode starting window is near-white for about 2 s with dark, legible buttons (5.78:1) over it. Accepted when option (a) was rejected.
   - session S O-1: frames dropped during the cascade on the 2 GB bench (swap 1.45 of 1.53 GB, host quiet, the whole app frozen). An instrument limit, not a product defect; no band ended out of place.
   - session S Open issue 3: 15v part 3 (chips landing during the cascade) was not produced on the device. It stays covered on the JVM by 15u and by the design (§1.2: the cascade is a `graphicsLayer` offset, never a layout move).
5. **`develop` at `f918793` needs no further merge-in before this branch merges.** Since arm A `f7dd1fe` it adds only `scripts/play-upload.sh` outside `.planning/` (`git diff --name-only f7dd1fe develop -- . ':!.planning'`), which no Gradle script or `app/src` file references (loop 3 Open issue 1). It is not start-path, so the 15e re-sit stands, and `git merge-tree --write-tree develop HEAD` is clean.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-home/.planning/reports/home-chip-arrival/tech-lead-signoff.md` — this sign-off, committed alone. No code, no device, no emulator.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-home` unless noted. `.planning/log/` was not read.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0), and `diff -q` of each rule file against the plugin copy — older, so the plugin copies were read.
- `git status --short`; `git rev-parse HEAD` — clean; `876258e`.
- `./gradlew check` at `876258e` — **pass**: exit 0, BUILD SUCCESSFUL in 11m 12s (02:43–02:54 +03, 2026-11-01), 104 actionable tasks, 11 executed and 93 up to date. `:domain:depthSweep`, `:domain:koverVerify` and `:app:lint` executed; `:app:testDebugUnitTest`, `:domain:test`, `:domain:fuzz`, detekt and ktlint were up to date against the auditor's `--rerun-tasks` outputs at code-identical `5d665c8`, so their results are those outputs re-validated by Gradle's input hashes. The result XML: `:app` 1,262 tests, `:domain` 294, fuzz 1, depthSweep 1, `:build-logic` 55 — 0 failures, errors or skips. Lint: 64 warnings, 0 errors. The log has 0 `w:` and 0 `e:` lines (compilations up to date).
- `git diff --quiet 5708d95 HEAD -- . ':(exclude)app/src/test' ':(exclude).planning'` — pass: exit 0; `git diff --name-only 5708d95 HEAD` lists the two C11 test files and six reports.
- `git rev-parse --short develop`; `git merge-base develop HEAD`; `git diff --name-only f7dd1fe develop -- . ':!.planning'` — `f918793`; `f7dd1fe`; only `scripts/play-upload.sh`.
- `git merge-tree --write-tree develop HEAD` — pass: exit 0, clean.
- `git diff --name-only develop...HEAD -- app/src/main | sort` — pass: the 11 files in §1.
- `git diff --stat develop...HEAD --` the frozen list in §1 — pass: empty, every path exists (`TimerDigits.kt` lives in `ui/game/`).
- `git diff develop...HEAD -- app domain | grep -nE '^\+.*(@Suppress|@Ignore|!!|ktlint-disable|detekt:|@Disabled|tools:ignore)'` — pass: no match.
- `git diff develop...HEAD -- …/BoardCanvas.kt` — read: two hunks, as §1 states.
- `cat` of both v35 theme files; `ls app/src/main/res | grep -E 'v27|v35'` — pass: the v35 pair only.
- `git diff develop...HEAD -- app/src/main/res/values/strings.xml`; `grep` of `GameStatusStrip.kt` and `ElapsedTimeSpeech.kt` — pass: as §1 states.
- `git grep -nE 'duration_seconds|duration_minutes_seconds|MinutesAndSeconds|SpokenDuration\.Seconds|secondsSaid' HEAD -- . ':!.planning' ':!docs'` — pass: no match.
- Trailer count over `git rev-list develop..HEAD` (34 commits) — pass: one `Co-Authored-By` each, 0 `Claude-Session`.
- `git show 09e542e` — read: the founder's 2026-10-31 directives (effort one step below the pin, reduced QA, candy stays in 3.0.0, emulator only).
- Not re-run: the auditor's probes, any device or emulator step, `--rerun-tasks`. The tech-lead re-runs no gate it does not own.

## Open issues / risks
Not merge conditions; each has an owner and a deadline.
1. **Owed text before `release/3.0.0` is cut (release checklist 1.1):**
   - the ux-designer's spec folds (defect ruling Open issue 3): 29 (1)'s v35 placement and "below API 35", §6.2's mechanism bullet, 18f's sentences and §4.3.2's fallback note, §8's string rows;
   - my tech-design mirror rows in this branch's close sync (defect ruling Open issue 4; C11 ruling §5 items 1–5; device ruling §2's re-attempt rule into §10's 15e row; the census wording of §3.3 above).
2. **3.0.1 items:** the chip room's gate-keyed effect with the tests of §3.2; Robolectric window tests at sdk 35/36 with the android-all jar provisioned through Gradle (defect ruling Open issue 2).
3. **For the 3.0.0 retro (producer):** the bench's lost Classic save (session S Open issue 1: `adb shell sync` before `emu kill`; C-3 reads `resume_snapshot`), next to R-3's foreign APK finding.
4. **`Pixel_9_clean`'s Google test account** may hold a synced Classic resume snapshot from session D (its Open issue 1). Test data only; no product defect.
5. **talkback-board** reads B-3 on its own tree until it takes `develop` in with this branch merged (defect ruling Open issue 5); its merge-in re-runs this branch's named tests and greps `app/src/test` for `ANIMATOR_DURATION_SCALE` (C11 ruling Open issue 2).
6. **Staffing (Q-019):** the author, both reviewers and this sign-off ran on Opus. The mitigations are the auditor's own probes and Checks, the device artifacts, and the commands above.

## Handoff
Next: producer —
1. Commit this sign-off alone on `bugfix/home-chip-arrival` (`docs(15): record the tech-lead home-chip-arrival sign-off`).
2. Merge `bugfix/home-chip-arrival` into `develop` with `--no-ff` (§14.1 step 6), before talkback-board, colour-vision and candy-backdrop.
3. Dispatch the close sync before the release cut: the ux-designer's folds and my mirror rows (Open issue 1).
4. Pass Open issue 5 to talkback-board's merge-in dispatch, and Open issues 3 and 4 to STATUS for the retro.

VERDICT: PASS
