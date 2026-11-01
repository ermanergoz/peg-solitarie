# tech-lead report — reduced-motion-live-gate (sign-off before the merge to develop)
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (report only; read at `d195868`. The code range is `7382351..45f84ec`, off `develop` at `6aa7624`; `46d4317` and `d195868` carry only reports and `docs/ux-spec.md`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, dispatch, testing, stacks, puzzle-design, play-store, kotlin-android and compose; also the snapshot's lessons.md. Plus android-architecture and android-gradle-logic
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (QA), licences-screen (auditor and ux ruling), colour-vision (auditor, tech-lead and ux rulings)
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and Q-019's answer (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published. The author, both reviewers and the earlier rulings all ran on Opus, so the reviewing roles share the author's model. I am a fresh tech-lead and wrote none of the earlier rulings. Mitigation: the facts this sign-off rests on were re-read in the tree, the diff and the SDK sources, and none is taken from a report alone (Checks)
Fan-out: none
Loops: 0
Read in full: `.planning/reports/reduced-motion-live-gate/tech-lead-ruling.md` (the ruling, the Finding 1 routing and the F1 amendment); `android-game-dev.md`; `android-game-dev-finding1.md`; `code-auditor.md`; `code-auditor-finding1.md`; `qa-engineer.md`; `ux-designer.md`; `docs/tech-design.md` as it stands on `develop` at `29a078d` (1,838 lines; the branch's own copy is the older merge-base text, which `develop`'s r1-close sync supersedes); `git diff develop...HEAD` for `app/src/main`, `app/build.gradle.kts`, `docs/ux-spec.md`, the harness and the gate tests
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **SIGN-OFF: PASS.** The gate as built is the mechanism §14.1 RM and §16 #28 rule. Commit 1 matches the ruling's §3 table, and commit 3 matches F1.2 with the F1 amendment. Both code-auditor reads PASS. Every cut-blocking device check in row RM passed on the device.
- **QA's F1 (about 10 s to arrive) is not the game's.** It is closed for 3.0.0 by the ux-designer's ruling, which I confirm on the evidence in §2. The backlog aim it leaves goes into §14.2 at the RM close.
- **The two BLOCKED items.** §6's TalkBack count is a 3.0.1 item. The mid-pop star is closed by other evidence. Both observations are backlog items. The reinstall incident is closed by its timestamps and is a process item for the producer.
- **No Check re-run before the merge** while `develop` gains nothing outside `.planning/`, `docs/tech-design.md` and `docs/ux-spec.md`. The trial merge's code tree is byte-identical to `45f84ec`, where the last two green Checks ran.
- **Owed follow-ups**, named here and not written (§4): the 3.0.1 notice brief, a §16 item for §6, and the tech-design text at the RM close. All three are the tech-lead's.

## 1. The sign-off: PASS
**Against the tech design (§14.1 RM, §16 #28, §8.1, §9.8, §2).**
- **The mechanism is as ruled.**
  - `rememberAnimationsEnabled()` (`ui/theme/Clay.kt:219`) reads `rememberCoroutineScope().coroutineContext[MotionDurationScale]`. It is enabled when the factor is not 0 and enabled when the element is absent. Its name and signature are unchanged.
  - `app/src/main` makes no `Settings.Global`, `contentResolver` or `ContentObserver` call. The one hit is the KDoc at `Clay.kt:212`.
  - The gate has 14 call sites in 9 files (`git grep` lists 15 lines: the 14 calls plus the definition). No effect is keyed on the gate: `git grep 'LaunchedEffect(animationsEnabled|LaunchedEffect(rememberAnimationsEnabled'` finds nothing in `app/src/main`.
- **Commit 1 (`2b98c5c`) matches the ruling's §3.**
  - It touches the table's 11 files and no others, and the assertion-pattern grep over its hunks prints nothing.
  - The four new classes moved to the house v1 rule, as §8.1 allows. `GameScreenHintNoticeTest` stays on v2 and now has the §8.1 KDoc sentence (`recreateTheScreen`, `deliverTheTapsWrites`).
  - The test total stays at 1,575.
  - The 7c-race guards reach the snap path again, shown by the auditor's own probes rather than by the author's counts:
    - the probe fails 14 of 63 tests, against 7 before the ruling;
    - mutant A goes red on the head and stays green on the pre-ruling tests;
    - mutant B does the same.
- **Commit 3 (`45f84ec`) matches F1.2, FA.2 and FA.3.**
  - It touches two files. `CompletionStar` keys its effect on `Unit` and always calls `animateTo`. Its starting size still comes from the gate.
  - Its KDoc is FA.3's text verbatim.
  - `TutorialCompletionStarLiveMotionTest` runs on the v1 rule with `SystemMotionScale`. It asserts the second frame after the switch and 90 frames later, never the first.
  - The star reads 165 px on the old head and 639 px after the change. The auditor's mutant, with the gate-keyed effect and its guard restored, goes red.
- **The riders.**
  - Sound-designer F-1: `GameScreen.kt`'s four tick cues go through `NewOrdinalEffect`, pinned by `GameScreenCueRotationTest` (the auditor's `> 0` revert turned it 6/6 red).
  - One `inputs.files` block in `app/build.gradle.kts`.
  - The two KDoc commits.
- **The boundaries hold.**
  - `git diff develop...HEAD` touches nothing in `domain/`, `build-logic/`, `gradle/` or the settings and build scripts, and it adds no dependency, so `THIRD_PARTY.md` and §13 are unaffected.
  - It adds no `@Suppress`, `@Ignore` or baseline.
  - Main compilations have 0 `w:` lines (C7). The test compilation gains 5 v1 `createComposeRule` lines, from the four moved classes and the new star suite: 35 → 40. That is §9.8 rule 2's recorded exception, so it is no finding.
  - The test-input block belongs in `:app`, because both paths are `:app`'s own. No convention-plugin move is owed.
- **The reviews, by path.**
  - `.planning/reports/reduced-motion-live-gate/code-auditor.md`: PASS, with a green Check of its own and six probes.
  - `.planning/reports/reduced-motion-live-gate/code-auditor-finding1.md`: PASS, with a green Check at `bd116a0`, a forced `--rerun` and two mutants.
  - `.planning/reports/reduced-motion-live-gate/qa-engineer.md`: PASS WITH BLOCKED ITEMS. Row RM's device list passed: the mark with Remove animations on, the shake with it off, WT-15, P9, the Results stars and the tutorial after a live toggle, and the ruling's §8 start-path check (6 process starts, 2 rotations and 2 restores, 0 `MotionDurationScale` lines, 0 crashes).
  - QA's build came from `c6f52dc`, whose code is `45f84ec`'s: `git diff 45f84ec c6f52dc` outside `.planning/` is empty.
- **One step of row RM's QA list is not in QA's table:** the 23e step, where an off-book hint's notice raised after a live toggle keeps its time and leaves in one frame. It is **closed by other evidence**:
  - the slot composes per raise (`key(raiseKey)`), so a notice raised after the toggle reads the gate as it is at that raise;
  - the gate is proven live on the JVM (`AnimationsEnabledGateTest`) and on this device build at three other call sites (the board, Results and the tutorial);
  - `GameScreenHintNoticeTest.withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame` pins the notice at gate 0 under the live harness, and it is one of the probe's 14.

  For the record of 3.0.0, the device half rides the release/3.0.0 QA session (Handoff).
- **The ux-spec edits** (23d, 23e, §10, the revision entry) are the ux-designer's rulings, made in its own document. They contradict nothing in the tech design. The RM close cites them (§4).

**Why PASS and not HOLD.** Nothing open is cut-blocking or a regression of this branch:
- F1 is the platform's and ruled acceptable (§2);
- §6 is a pre-existing structure, ruled a low 3.0.1 defect by its owner;
- the mid-pop star is fixed and pinned;
- the observations predate the branch and touch none of its files.

The branch removes a medium accessibility defect that blocks the cut (§15's RM row) and adds none.

## 2. The BLOCKED items, F1 and the observations
| Item | Classification | Reason |
|---|---|---|
| **F1: the setting arrives about 10 s after the toggle** | **Closed for 3.0.0** (ux-designer ruling, `docs/ux-spec.md` §10, "A switch made while the game is open"), **plus a backlog aim** | **Not the game's, confirmed** (the ux-designer's open issue 2). (1) The game reads only Compose's own scale and runs no observer of its own. (2) QA's taps inside the window shook for the full 233 ms, so Compose's clock was still at the old value too: the late value is the upstream one, before any game code. (3) The platform does hold content-change dispatch to apps running in the background: the hidden `ContentResolver.NOTIFY_NO_DELAY` flag exists so that a provider can opt a change out of "any delays when dispatching to apps running in the background" (`ContentResolver.java:715-726`, android-35 sources). The delay's length lives in system_server, which the SDK does not ship, so I did not read the 10 s figure. QA's 9.8–11.7 s, measured from the toggle in both directions, fits a fixed delay that starts at the change. (4) The app cannot set that flag, because the provider is the platform's. The measured worst case is within the ux pass line of 15 s. **Backlog aim** (§14.2 at the RM close): the new setting in force from the first frame after the player returns, under the ux order, agreement before speed. The only route is an app-provided `MotionDurationScale` in the window recomposer's context, one that both observes and re-reads on start. That puts our own `Settings` read and a lifecycle hook back on the start path, so it is post-3.0.0 and carries a 15e sitting |
| **BLOCKED: §6, the TalkBack count on the notice across a live toggle** | **3.0.1 item** | The ux-designer ruled it a defect, rated low and not cut-blocking (23d, 2026-10-24). With that ruling made, the count is no longer evidence the merge needs. It moves to the 3.0.1 fix's QA: the app kept in the foreground (split screen with Settings) and an AVD on which TalkBack was set up once by hand. `feature/talkback-board` at `b8aa8c4` does not touch `ui/game/HintUndecidedNotice.kt`, so the fix does not ride that branch today |
| **BLOCKED: the completion star caught mid-pop** | **Closed by other evidence** | F1.3 made `TutorialCompletionStarLiveMotionTest` the evidence of record: 165 → 639 px at the second frame after the switch, held 90 frames, and the auditor's mutant red. F1 makes the case unreachable through Settings, since the change arrives about 10 s after a 0.7 s spring. The device did show the star composed with animations off settled (`t5`) |
| **Observation 2: landscape clips the board** | **Backlog item** (ux-designer, post-3.0.0) | It predates this branch, which changes no layout. The phase-15 sign-off ruled O-5: no orientation lock is added, and landscape layout quality is a post-3.0.0 item for the ux-designer (`15-founder-feedback/tech-lead-signoff.md:280-283`). That item is missing from §14.2, so the RM close records it with QA's `s8-off-3-rotated.png`. One fact can change the class: if the published 2.0 fits English-33 in landscape and 3.0.0 does not, it is a regression, and its severity is the ux-designer's to rule |
| **Observation 3: a launcher intent from the shell returns to Home** | **Backlog item** (an observation) | `MainActivity` has the default launch mode and no task affinity (`AndroidManifest.xml:32-40`); this branch does not touch it. Android brings a resident task to the front when the new intent matches the task's root intent. A shell `am start` whose fields differ can instead stack a second `MainActivity`, which starts at Home. Back and Recents reaching the game fit a second instance above the first. One tap on the real launcher icon, with a game resident, in the release/3.0.0 QA session settles it. If that tap lands on Home, it is a start-path defect for the tech-lead |
| **The cross-session reinstall** (15:57:53–54) | **Closed by other evidence** for this verdict; **a process item** for the producer | Every result in QA's table was recorded between 15:41 and 15:56, on the build whose md5 is `a962…`, before the swap. The reinstall restored the same md5. What it shows is a dispatch problem: two QA sessions shared one emulator. One reserved AVD per concurrent QA session, named in the dispatch, is the producer's discipline (a retro candidate). `Pixel_9b_clean` was also left with TalkBack on and the timeouts at 2 minutes, which is the producer's to route (QA open issue 1) |

**One low text item of mine.** `Clay.kt`'s KDoc ends: "a toggle with the app resident recomposes every reader and the next wrong tap, star or entrance takes the path the setting says now." On a device, "now" is "once Compose's observer delivers the change, about 10 s after a switch made from Settings (QA F1)". The mechanism the KDoc describes is right, so this does not gate the merge. It rides the 3.0.1 notice fix (§4a), the way the Settings KDocs were non-gating riders here.

## 3. The Check: no re-run before the merge
- **The last green Checks** ran on the code tree of `45f84ec`:
  - the developer's at `45f84ec`: 11 m 9 s, 1,576 tests, 0 failures;
  - the auditor's at `bd116a0`, which differs from `45f84ec` by one report: 11 m 7 s, the same totals, plus a forced `:app:testDebugUnitTest --rerun`.
- **Since then** the branch has changed `.planning/` and `docs/ux-spec.md` only. `develop` has moved from `6aa7624` to `29a078d`, also with docs and records only: the r1-close sync of `docs/tech-design.md`, its reports and STATUS.md.
- **The trial merge is clean.** `git merge-tree --write-tree develop HEAD` gives tree `3154589` with exit 0. That tree differs from `45f84ec` only in `.planning/`, `docs/tech-design.md` and `docs/ux-spec.md` (the `git diff --quiet` below exits 0).
- **No build input reads those files.** The only working-tree document the build or the tests read is `docs/audio/CREDITS.md` (`app/build.gradle.kts:466`, `AudioRepoFiles.kt:19`), and it is identical.
- So the merged tree's Check inputs are byte for byte the ones that were green. **Ruling (tech-lead decision): merge with `git merge --no-ff` without a re-run**, provided this still exits 0 at merge time:

      git diff --quiet 45f84ec "$(git merge-tree --write-tree develop bugfix/reduced-motion-live-gate)" -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' ':(exclude)docs/ux-spec.md'

  The `merge-tree` itself must also exit 0, which means no conflict.
- **If `develop` gains a code commit first** (talkback-board, home-chip-arrival, licences-screen or colour-vision), or the command above fails, this branch becomes the second to merge. It then merges `develop` in, re-runs `./scripts/check.sh` on the merged tree, and runs the other branch's named tests. A fresh tech-lead re-reads the merge commit's conflict hunks (§14.1, the second-to-merge paragraph).

**For whichever branch merges after this one** (measured with `git merge-tree` against `develop` at `29a078d` plus this branch; to be re-measured at the merge-in, by §14.1's record rule (c)):
- **No conflict:** `feature/talkback-board` (`b8aa8c4`; it shares `GameScreen.kt` and `TutorialScreen.kt`), `bugfix/home-chip-arrival` (`84695b2`; `GameFlowTest.kt`) and `feature/licences-screen` (`4b5747b`; `app/build.gradle.kts`, `SettingsScreen.kt`).
- **`feature/colour-vision` (`a3e1759`) conflicts in three KDocs only:** `data/SettingsRepository.kt`, `ui/settings/SettingsUiState.kt`, and `CompletionStar`'s KDoc in `ui/onboarding/TutorialScreen.kt`. `CompletionStar`'s body auto-merges with this branch's `LaunchedEffect(Unit)` and colour-vision's `StarGlyph`. **The resolution (tech-lead decision):**
  - keep this branch's text and add colour-vision's own clauses: the fourth flag, `colorblindPaletteOn`, and the palette in the star's first sentence;
  - colour-vision's "no audio engine yet" lines must not survive;
  - nothing in `TutorialScreen.kt` may bring back a gate-keyed effect.
- **This branch's named tests**, for that second-to-merge re-run:
  - `GameScreenCueRotationTest`, `GameScreenSoundTest`, `GameFlowTest` and `GameScreenHintNoticeTest`;
  - `BoardCanvasWrongTapLiveMotionTest` and `BoardCanvasWrongTapReducedMotionTest`;
  - `AnimationsEnabledGateTest` and `AnimationsEnabledGateWithoutScaleTest`;
  - `ResultsStarsLiveMotionTest` and `ResultsCelebrationSoundTest`;
  - `TutorialHapticsTest`, `TutorialSoundTest` and `TutorialCompletionStarLiveMotionTest`.

## 4. Owed follow-ups (named, not written)
**(a) The 3.0.1 notice brief.**
- *Owner:* tech-lead, a fresh dispatch through the producer.
- *When:* after this branch merges and before the 3.0.1 developer dispatch. If `feature/talkback-board`'s diff comes to touch `HintUndecidedNotice.kt` before it merges (it does not at `b8aa8c4`; the producer re-checks before that merge), the brief is due before that branch's next developer dispatch, and the fix rides that branch (ruling §6).
- *It must bind:*
  - ux 23d's four terms;
  - the notice node's identity kept across a gate flip while the notice is up;
  - 23d's new node per raise;
  - 23e's one-frame exit with animations off, and the reading time unchanged;
  - no gate-keyed effect (F1.4).
- *Red first:* a test on the live harness, a v1 rule with `SystemMotionScale`, asserting the same semantics node and no entrance across `turnAnimationsOff()` / `turnAnimationsOn()`, with the paused clock's one-frame delivery lag (`BoardCanvasWrongTapReducedMotionTest.kt:33-35`) allowed for.
- *Rider:* the `Clay.kt` KDoc latency clause (§2).
- *QA:* the TalkBack count in each direction, as in the §6 row of §2.

**(b) The §16 item for §6.**
- *Owner:* tech-lead, in the RM-close sync (c).
- *Content:* a new row, numbered after `develop`'s highest at that sync (#32 if #31 is still the last; otherwise by §17's merge-in rule (3)): "The Undecided notice is re-spoken, and its entrance replayed, on a live switch of Remove animations while it is up". **Open, owned, low, 3.0.1, not cut-blocking.** Chain: tech-lead brief → android-game-dev → code-auditor + qa-engineer → tech-lead. Cites ux 23d (2026-10-24), ruling §6 and `ux-designer.md` §1.

**(c) The tech-design text at the RM close.**
- *Owner:* tech-lead (the edits); code-auditor (reads it as text); a fresh tech-lead (signs it off). QA does not apply. This is the r1-close pattern.
- *When:* on a docs-only branch off `develop` cut right after this branch merges, and before release checklist 1.1 runs: 1.1 reads row RM's "Blocks the branch cut", so the row must say Done by then.
- *It carries:*
  1. **§14.1 row RM: Done.** The merge commit, this sign-off and the reports. Ruling §9 item 1's replacement for "stay green unedited". Finding 1's rider. F1's acceptance, pointing to ux §10 and its 15 s pass line. QA's §8 start-path PASS.
  2. **§16 #28: closed.** Its mechanism paragraph gains F1.4's sentence with FA.4's clause.
  3. **§8.1:**
     - ruling §9 item 2's sentence;
     - the v2 paragraph's "added by the first branch that next touches that file", marked done (`2b98c5c`);
     - the four RM classes, on v1.
  4. **§9.8:** the `createComposeRule` figure taken from the merged tree's own Check, by §17 rule (4) (+5 v1 suites here).
  5. **§15's RM row: closed.** Its residuals: F1 (accepted) and §6 (3.0.1).
  6. **§14.2: two lines.** F1's backlog aim, with the route and cost in §2. Landscape layout quality (O-5, observation 2).
  7. **§16:** item (b).
  8. **§17:** ruling §9 item 3's row.
  9. **The header:** its dated sync sentence.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rm/.planning/reports/reduced-motion-live-gate/tech-lead-signoff.md`: this sign-off. It is the only file written, committed alone.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-rm` unless named. No Gradle was run: a sign-off re-runs no gate (§3).
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0): older, so the plugin rule files were read — pass.
- `git log --oneline develop..HEAD` lists 17 commits, the code ending at `45f84ec`. `git merge-base develop HEAD` is `6aa7624`. `git log 6aa7624..develop` lists 11 commits, all docs, reports or STATUS — read.
- `git merge-tree --write-tree develop HEAD` gives `3154589`, exit 0. `git diff --quiet 45f84ec 3154589 -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' ':(exclude)docs/ux-spec.md'` exits 0 — pass.
- `git grep -n -E 'docs/|ux-spec|tech-design\.md|\.planning'` over the build scripts, build-logic, the tests, `check.sh` and CI finds only `docs/audio/CREDITS.md`, which is unchanged — pass.
- `git diff 45f84ec c6f52dc` outside `.planning/` is empty, so QA's build is the signed code. `git diff 45f84ec bd116a0` is one report — pass.
- `git diff develop...HEAD -- app/src/main app/build.gradle.kts`: read in full (`Clay.kt`, `TutorialScreen.kt`, `GameScreen.kt`, the three Settings KDocs, the build block) — read.
- `git show --stat 2b98c5c` lists 11 files, matching the ruling's §3 table. `git show -U0 2b98c5c | grep -E '^[-+].*(assert|containsExactly|isEqualTo|hasSize|isEmpty)'` prints nothing — pass.
- `git show --stat 45f84ec` lists two files. The KDoc is FA.3 verbatim. The test asserts at `SETTLING_FRAMES` and `HELD_FRAMES` (`:74-79`) — pass.
- `git grep -E 'Settings\.Global|ContentObserver|contentResolver' -- app/src/main` finds one KDoc line, `Clay.kt:212`. A grep for a gate-keyed `LaunchedEffect` finds nothing. The gate has 14 calls plus its definition, in 9 files — pass.
- `git diff develop...HEAD | grep '^+.*(@Suppress|@Ignore|baseline|-nowarn|suppressWarnings)'` prints nothing. `git diff --stat develop...HEAD -- domain build-logic gradle settings.gradle.kts build.gradle.kts` is empty — pass.
- The rule imports in the 13 touched test classes: 12 use v1 (directly, or through `createWrongTapComposeRule`); `GameScreenHintNoticeTest` uses v2 with its §8.1 KDoc sentence (`2b98c5c`, `:76-79`) — pass.
- `grep -n -B12 'int NOTIFY_NO_DELAY' ~/Library/Android/sdk/sources/android-35/android/content/ContentResolver.java` confirms the background dispatch delay. `BACKGROUND_OBSERVER_DELAY` is not in the SDK sources (server code) — read, length not verified.
- `sed -n 28,42p app/src/main/AndroidManifest.xml`: the default launch mode, no `screenOrientation`. `docs/ux-spec.md:5424`, landscape. `15-founder-feedback/tech-lead-signoff.md:280-283`, O-5 — read.
- `git diff develop...HEAD -- docs/ux-spec.md`: the revision entry, §10's corrected sentence, its F1 ruling and pointer, 23d's paragraph, 23e's rewording — read.
- `git merge-tree` of `develop` plus RM (a dangling trial commit `694f53a`, never referenced) against `feature/talkback-board`, `bugfix/home-chip-arrival` and `feature/licences-screen` exits 0. Against `feature/colour-vision` it exits 1, with three KDoc conflicts; `CompletionStar`'s merged body keeps `LaunchedEffect(Unit)` — read.
- `git diff --name-only develop...feature/talkback-board -- …/HintUndecidedNotice.kt` is empty at `b8aa8c4` — read.

## Open issues / risks
1. **§6 ships in 3.0.0** as a known low defect (23d). The producer records it in STATUS.md. My recommendation, the producer's call: name it in the Q-PUBLISH-3.0.0 summary's known issues.
2. **F1's acceptance** rests on the platform's delivery delay. Its mechanism is confirmed from the SDK; its 10 s length is QA's measurement. If a later device shows a delay that grows with time spent in the background, rather than a fixed one, it comes back to the tech-lead.
3. **Q-019:** this sign-off, the code and both reviews share Opus. The mitigations are recorded (the auditor's mutants and Checks, and my own re-reads). §16 #19's post-3.0.0 question about a Fable re-audit applies here too.
4. **Merge-time risk:** the no-re-run ruling holds only while §3's command exits 0. The producer runs it immediately before `git merge --no-ff`.
5. **Retro candidates** (for the producer to note; not lesson proposals): two QA sessions shared one emulator and one reinstalled over the other; and a "next frame" claim was made from a source read that missed the order of the scale read and the frame wait (FA.1). It was caught only because the developer's test went red first.

## Handoff
Next: producer —
- commit this file alone: `docs(14): record the tech-lead reduced-motion sign-off`;
- run §3's command, then `git merge --no-ff bugfix/reduced-motion-live-gate` into `develop` if it exits 0 (otherwise the second-to-merge path in §3);
- record in STATUS.md:
  - RM merged, so its row no longer blocks the cut;
  - §6 is a 3.0.1 low defect;
  - F1 is accepted;
  - the follow-ups below;
- restore `Pixel_9b_clean`'s TalkBack and timeouts (QA open issue 1);
- record the reinstall incident;
- delete `.planning/qa/reduced-motion-live-gate/` after the Done line;
- schedule the RM-close sync (§4c), then the 3.0.1 brief (§4a);
- add three steps to the release/3.0.0 QA session:
  - 23e's device half: switch Remove animations on, wait 15 s, raise an off-book hint, and check that the notice keeps its 5 s and leaves in one frame;
  - one tap on the real launcher icon with a game resident (observation 3);
  - landscape on the published 2.0 against 3.0.0 for English-33 (observation 2), for the ux-designer.

Next: tech-lead (fresh) — the RM-close sync (§4c, including §4b), and later the 3.0.1 notice brief (§4a).

Next: the branch that merges after this one — §3's second-to-merge duties and this branch's named tests; colour-vision follows its KDoc resolution in §3.

Next: ux-designer — nothing on this branch. Observation 2's severity if the 2.0 comparison shows a regression; the 3.0.1 fix's acceptance against 23d.

SIGN-OFF: PASS
VERDICT: PASS
