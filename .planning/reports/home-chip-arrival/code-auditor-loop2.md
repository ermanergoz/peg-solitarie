# code-auditor report — home-chip-arrival (loop 2: B-1, B-1b, B-2, B-3)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (committed by the producer). Under review: `7448713..1e2bc3d` (F1 `925f36f`, F2 `88c52cb`, F3 `1e2bc3d`). Head `1a748c9` is the developer's report; there is no code after `1e2bc3d`.
Skills loaded: ergoz-soft:standards (code-auditor; the snapshot 0.6.2 is older than the plugin 1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus the product's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: colour-vision QA, licences-screen QA re-run, talkback-board QA
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable. The Fable limit is reached, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published, so the reviewer shares the developer's model. Mitigation: a fresh agent; my own `--rerun-tasks` Check; 15 mutation probes of my own; Compose 1.11.3 sources re-read; no recorded result trusted.
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot holds 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer.

## Summary
- F1 to F3 do what the defect ruling asks, in its main form. The clock is a cleared leaf that holds still within a minute, the description is in floored whole minutes, and the v27 theme pair is replaced by the v35 pair.
- Every §4.2 item holds: the frozen files have no diff; the guards hold; there is no dormant string or type; 18f matches §2's table row for row; both v35 files carry their comment.
- All four ruled probes are red, each on exactly its own test. My 11 other probes are red where they should be, or confirm what the developer claimed.
- Every existing-test edit and Deviations 1–7 are approved. No suppression was added.
- **VERDICT: PASS.**

## Findings
None to fix. INFO only:
- **I1. The holds-still test reads the first minute only.**
  - With seconds restored past one minute (P2b), it stays green.
  - 18f's table catches that case (01:01, 05:12, 59:59). Together the two tests pin the rule.
- **I2. `WindowThemeApi35FilesTest` pins the two files, not the bar.**
  - The device decides B-1: session D's 29 (2), whose `dumpsys` must show no `FORCE_LIGHT_NAVIGATION_BARS`, and the below-35 read.
  - Gradle does track the file read: P3 re-ran the test on a resource-only change, with no forced rerun.
- **I3. 18a's and 18e's `cells == timerText()` now compares the cells with themselves.**
  - The coverage it used to carry lives in `TimerDigitsTest`: P10 (a missed cell) turned 4 `TimerDigitsTest` cases red and left all 10 `GameStatusStripTest` cases green.
  - Comparing against `GameTimerFormat.format(...)` would restore the stronger form. That is optional, only if the file is touched again.
- **I4. Not verified, for session D.** On API 35+ the v35 items also reach the dark-mode starting window, whose background is still `#fafafa`. A dark-mode cold launch in three-button navigation may show a dark scrim with light buttons over that near-white window for its first frames. This is a glance, not a finding: the ruling accepted the `#fafafa` starting window when it rejected option (a).

## §4.2 checklist
- **No diff in the frozen files — holds.**
  - `TimerDigits.kt`, `GameScreen.kt`, `MainActivity.kt`, `Theme.kt`, `Clay.kt`, `ui/home/`, `res/values{,-night}/themes.xml`, the manifest and `dimens.xml`: empty diff, and every path exists.
  - No Gradle, `build-logic` or settings file is in the diff.
  - `TimerDigitsTest`, `GameFlowTest` (`:129` unedited) and `androidTest` are untouched.
- **One node, cells hidden (N-2), no live region, no role — holds.**
  - `theTimerIsStillOneNodeWithNoLiveRegion` is unedited and green.
  - P5 (polite live region) and P6 (`Role.Button`) each turn it red.
  - The provider hands out the clock with `text=null` and the description only.
- **No dormant string or type — holds.**
  - `git grep` finds no `duration_seconds`, `duration_minutes_seconds`, `MinutesAndSeconds`, `SpokenDuration.Seconds` or `secondsSaid` outside `.planning/` and `docs/`.
  - Lint's `UnusedResources` has nothing on `strings.xml` lines 83–100.
- **18f against the ruling's §2 table — holds.** `SPOKEN_TIMES` (`GameStatusStripTest`) and `ElapsedTimeSpeechTest`'s table match it word for word at 0, 1, 45, 60, 61, 312, 720, 3599 and 6000 s.
- **Each v35 file's comment — holds.**
  - `values-v35` says why the transparent colour is named, that a player sees no change from API 35, and "Do not remove the item as unused or deprecated".
  - `values-night-v35` says the same, pointing to the light file's reason.
  - Both restate the whole style on the base parent with exactly three items.
- **The mechanism is the one ruled.**
  - `clearAndSetSemantics` is the outer modifier, so the inner per-second `text` of `TimerDigits` is cleared (P1 shows `text=00:00` → `00:01` without it).
  - The chrome test reads Compose's own test hook:
    - `ExtraDataTestTraversalBeforeVal`, ui-android 1.11.3 `:391-393` and `:1920`;
    - `isScreenReaderFocusable`, `:3456-3468`;
    - `isUnmergedLeafNode`, `SemanticsNode.kt:100-106`.
- **Commits.**
  - The files in each commit are exactly §4.1's table.
  - Each subject is conventional and under 72 characters.
  - Each commit has one trailer, Fable 5.1, and no `Claude-Session` line.
- **Security and dependencies.**
  - No network, secret, permission or dependency is added.
  - `THIRD_PARTY.md` is untouched.
- **Performance.**
  - `TimerPill` recomposes once a second, as before.
  - The description now costs one string or one plural, where it cost up to three.
  - No new allocation in a hot path.

## Probes
All probes ran in the throwaway detached worktree `&lt;scratchpad&gt;/probe-wt` at `1e2bc3d`. Each was one mutation, or two independent ones, run with `./gradlew --offline --max-workers=4 :app:testDebugUnitTest --tests …` and restored after. The unmutated baseline was 35 run, 35 green.

| # | Mutation | Result |
|---|---|---|
| P1 (ruled) | `clearAndSetSemantics` → plain `semantics` | **RED** §3 (a): "whether the clock's node is screen-reader focusable / expected to be true". Holds-still also **RED**: `text=00:00` but was `00:01`. 29 run, 2 red |
| P2 (ruled) | Seconds restored under a minute (`duration_seconds` back) | **RED** holds-still: "description=Elapsed time, 0 seconds" but was "1 second". Also red: 18f at 00:00, and both chrome `TIMER_AT_THE_START` cases. 21 run, 4 red |
| P3 (ruled) + P4 (ruled) | `navigationBarColor` dropped from `values-night-v35`; `values-v27` restored from `7448713` | **RED** dark pin: "missing (1): (android:navigationBarColor, @android:color/transparent)". **RED** light below-35 case: "expected to be false". 6 run, 2 red, each on its own test |
| P3b + P4b | Light v35 colour → `#00000000`; `values-night-v27` restored | **RED** light pin ("unexpected (1): … #00000000"). **RED** dark below-35 case. 6 run, 2 red |
| P2b | Seconds restored past one minute only | **RED** 18f at 01:01: "expected Elapsed time, 1 minute, but was …, 1 second". Holds-still green (I1). 21 run, 1 red |
| P5 | Polite live region on the clock | **RED** "the clock's live region / expected: null / but was: Polite" |
| P6 | `Role.Button` on the clock | **RED** "the clock's role … but was: Button" |
| P7 | Minutes rounded, `(s + 30) / 60` | **RED** `ElapsedTimeSpeechTest` at 45 s, 18f at 00:45, holds-still at 00:30 |
| P11 | 60 s counted as "under a minute" | **RED** `ElapsedTimeSpeechTest` at 60 s, 18f at 01:00, holds-still "expected not to be: Elapsed time, under a minute" |
| P8 | The ruling's rejected `semantics(mergeDescendants = true)` | **RED** holds-still: `text=00:00` but was `00:01`. Only this test rejects it |
| P9 | Method-level NATIVE removed from the new chrome case | Harness failure `IllegalStateException: no node for the clock`. Confirms Deviation 1 |
| P10 | `TimerDigits` drops one cell | 4 **RED** in `TimerDigitsTest` (N-2 "expected 5 but was 4", RTL "05:1", …); `GameStatusStripTest` 10/10 green. Confirms Deviation 2 |
| P12 | Clearing modifier removed and assertion (a) deleted | **RED** (b): "the stop Compose reads after the title / expected: the timer / but was : Undo". §3 (b) is a real JVM read and reproduces B-3 on its own |

## Rulings
**Existing-test edits ordered by the ruling — all APPROVED.** Each was red first by the developer's quotes, and P2, P3 and P4 show each still catches the regression it is there for:
- `ElapsedTimeSpeechTest`'s table;
- `SPOKEN_TIMES` and its KDoc;
- `TIMER_AT_THE_START`;
- the two `WindowThemeTest` navigation cases, retargeted into `belowApi35…`;
- `timerText()`.

**The developer's deviations:**
1. **NATIVE graphics on the new chrome case — APPROVED.**
   - Under LEGACY the provider hands out no clock node (P9), so LEGACY gives a harness failure, not a test of behaviour.
   - The setting is per method, so the rest of the class stays LEGACY.
   - `GameStatusStripTest` and `TimerDigitsTest` already read the provider under class-level NATIVE.
2. **`timerText()` is weaker — APPROVED.** It is the ruled retarget, and P10 shows the lost coverage is held in `TimerDigitsTest` (I3).
3. **KDoc-only test edits and the private `windowTheme()` helper — APPROVED.**
   - The edited KDocs are: the 18a sentence, the 18f test, `ElapsedTimeSpeechTest`'s class, `TIMER_AT_THE_START` and `WindowThemeTest`'s class. Each describes what the code now does.
   - `windowFlag`'s body is equivalent to before, and the status-bar case bodies are unchanged.
4. **The compile-error red — APPROVED.** It is the only red a not-yet-existing type can give, and the four runtime reds were watched first.
5. **`minutesSaid` kept as a one-call helper — APPROVED.**
6. **The Fable 5.1 trailer — consistent** with rules/gitflow.md 1.0.0.
7. **The report returned as text — noted.**

**The developer's open issues:**
- 1 → the producer (Open issue 1 below).
- 2 and 3 → the device.
- 4 → accepted, as I3.
- 5 → accepted. With the clearing modifier the node's config holds the description alone, and 18d holds the bounds. 18g on the device decides.

**Suppressions:** none added (`@Suppress`, `@Ignore`, `!!`, ktlint or detekt disables, baselines: empty). Nothing to approve or reject.

## Outputs
- This report (returned as text; the producer writes it to `.planning/reports/home-chip-arrival/code-auditor-loop2.md` and commits it).

## Checks
- `./scripts/check.sh --max-workers=4 --rerun-tasks`, in `PegSolitarie-home` at `1a748c9` (code identical to `1e2bc3d`; `git diff --name-only 1e2bc3d..HEAD -- . ':!.planning'` is empty) — **pass**:
  - exit 0, BUILD SUCCESSFUL in 12m 18s (22:11:06–22:23:26); 104 actionable tasks, all executed;
  - `:app` 1251 tests in 122 classes; `:domain` 294 in 33; `depthSweep` 1; `:build-logic` 55 in 6; 0 failures, errors or skips;
  - `koverVerify`, `detekt`, `ktlintCheck` and `lint` all ran.
- Compiler warnings — **pass**: 39 `w:` lines, 0 from `src/main`. The touched test files have only the pre-existing `createComposeRule` deprecation (`GameScreenChromeTest.kt:53`, `GameStatusStripTest.kt:78`; tech-design §9.8). `ElapsedTimeSpeechTest`, `WindowThemeTest` and `WindowThemeApi35FilesTest` have none.
- `app/build/reports/lint-results-debug.xml` — **pass**: 64 warnings, 0 errors. None on the v35 files, `GameStatusStrip.kt`, `ElapsedTimeSpeech.kt` or `strings.xml` lines 83–100.
- `git diff --name-only 7448713..1e2bc3d -- app/src/main | sort` — **pass**: exactly §4.1's seven files (the two v27 entries are deletions).
- `git diff --stat 7448713..1e2bc3d --` the frozen list — **pass**: empty, every path exists. Gradle, `build-logic` and settings: none. `TimerDigitsTest`, `GameFlowTest`, `androidTest`: none.
- `git diff 7448713..1e2bc3d -- app | grep -E '^\+.*(@Suppress|@Ignore|!!|ktlint-disable|detekt|baseline)'` — **pass**: empty.
- `git grep -E 'duration_seconds|duration_minutes_seconds|MinutesAndSeconds|SpokenDuration\.Seconds|secondsSaid' 1e2bc3d -- . ':!.planning' ':!docs'` — **pass**: no match.
- `git show --stat` of each fix commit — **pass**: the files match §4.1's table; one trailer each.
- `unzip` of `ui-android-1.11.3-sources.jar` — read `:391-393`, `:1915-1930` and `:3456-3468` of the accessibility delegate, and `SemanticsNode.kt:98-108`.
- Probes: 13 runs plus the baseline, results as in the table above. `git worktree add --detach &lt;scratchpad&gt;/probe-wt 1e2bc3d`, then `git worktree remove --force` and `git worktree prune`. `PegSolitarie-home` is clean at `1a748c9`, and no build lock is held.
- `git diff --name-only $(git merge-base HEAD develop) develop` (develop `f7dd1fe`, merge-base `6aa7624`) against this loop's files — **no overlap**. Develop changes `GameScreen.kt` and `GameFlowTest.kt`; its `GameFlowTest` hunks touch no timer line.
- Not run: any device or emulator, and the merge-in. `.planning/log/` was not read.

## Open issues / risks
1. **The merge-in is owed before the 15e sitting** (`develop` `f7dd1fe`), and the producer schedules it.
   - No file of this loop overlaps develop's changes, so no textual conflict is expected.
   - The merged tree needs its own green Check, and any code conflict comes back to the code-auditor.
   - The merge commit becomes arm B.
2. **The device decides behaviour** (ruling §4.4): B-1 on API 36 (`dumpsys` without `FORCE_LIGHT_NAVIGATION_BARS`), B-1b below API 35, 18g (a)–(c), and 26b. The JVM pins prove only the mechanism. I4 is a glance for session D.
3. **Spec folds** (ruling Open issue 3; not merge conditions).
   - `docs/ux-spec.md` §8 still lists `duration_seconds` and `duration_minutes_seconds` (lines 4617–4618).
   - 18f still quotes "0 seconds".
   - 29 (1) still names the v27 placement.
4. **Staffing.** The author and the reviewer both ran on Opus (Q-019). The mitigations are as on the Ran on line.
5. **Shared host.**
   - **The daemon stop.** `./gradlew --stop` in the probe worktree stopped one idle 9.5.1 daemon (pid 45579) at 22:28:19. The colour-vision session's builds (`PegSolitarie-cv`, `scratchpad/cv-base`) had started that daemon and finished on it by 22:13. My last probe had just ended, so no build was cut off; the next build there cold-starts a daemon.
   - **The shared scratchpad.** This session's agents share the scratchpad. My files there all have new names (`probe.sh`, `probes.sh`, `probe-*.log`, `p12-mut.*`, `check-rerun.log`, `dev-files.txt`, `ui-src/`), and none overwrote another agent's file.

## Handoff
Next: producer —
1. commit this report alone;
2. do the develop merge-in, with its own green Check;
3. dispatch QA session D on the new arm B, checked by sha256, then session S on the bench (ruling §4.4);
4. then the tech-lead sign-off (§4.5), which reads this report.

VERDICT: PASS
