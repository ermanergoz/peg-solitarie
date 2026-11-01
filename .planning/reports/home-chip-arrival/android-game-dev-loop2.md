# android-game-dev report — home-chip-arrival (loop 2: B-1, B-2, B-3)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (925f36f..1e2bc3d), from head 9f0c1be; arm B was 7448713
Skills loaded: ergoz-soft:standards (android-game-dev; the snapshot 0.6.2 is older than the plugin 1.0.0, so I read the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus the product's lessons.md), superpowers:test-driven-development (with writing-good-tests.md), gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: talkback-board QA, licences-screen code-auditor, colour-vision code-auditor, tech-design RM-close sync (all on the shared host; I never ran two builds at once)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 2
Read in full: tech-lead-defect-ruling.md, qa-engineer-device.md; tech-lead-brief.md §0–§4 (§1.6 included); docs/ux-spec.md §4.3.2, the §8 D-05e rows, criteria 18f, 18g, 26b and 29
Lessons applied: none (the snapshot holds 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer.

## Summary
- **F1 (B-3):** `TimerPill` now sets the clock's description with `clearAndSetSemantics`. The clock joins Compose's reading order after the title, and its own per-second `text` is gone from the node. This is the main form of §3: `TimerDigits.kt` and `TimerDigitsTest` have no diff.
- **F2 (B-2):** the description is said in whole minutes, floored, or as "under a minute". `SpokenDuration` is now `Minutes` or `UnderAMinute`. `duration_under_a_minute` is added, and `duration_seconds` and `duration_minutes_seconds` are deleted.
- **F3 (B-1, B-1b):** the v27 theme pair is deleted. A v35 pair restates `Theme.PegSolitaire` with three items, including `navigationBarColor` set to `@android:color/transparent`, and carries a comment saying why.
- Every named test was watched red first. §3 (b) ran under Robolectric.
- The new arm B is `1e2bc3d`.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt`:
  - F1: the clearing modifier, and a TimerPill KDoc section on B-3.
  - F2: the whole-minute spoken duration and the rewritten KDocs; `secondsSaid` is deleted.
- `app/src/main/java/com/app/pegsolitaire/ui/game/ElapsedTimeSpeech.kt` — F2: `parts` returns `Minutes(m)` or `UnderAMinute` (a `data object`), with a rewritten KDoc.
- `app/src/main/res/values/strings.xml` — F2: adds `duration_under_a_minute` ("under a minute"), deletes the two seconds strings, and rewrites the D-05e comment.
- `app/src/main/res/values-v27/themes.xml` and `values-night-v27/themes.xml` — deleted (F3).
- `app/src/main/res/values-v35/themes.xml` and `values-night-v35/themes.xml` — new (F3). Each has the three items and a "do not remove" comment.
- Tests:
  - `GameScreenChromeTest`: added `theTimerIsAStopInComposesReadingOrderBetweenTheTitleAndUndo` and its helpers (F1); retargeted `TIMER_AT_THE_START` (F2).
  - `GameStatusStripTest`: retargeted `timerText()` (F1); retargeted `SPOKEN_TIMES` and the 18f test's KDoc, and added `theTimersAccessibilityNodeHoldsStillWithinAMinute` (F2).
  - `ElapsedTimeSpeechTest`: retargeted the table (F2).
  - `WindowThemeTest`: the two D-05f navigation cases are retargeted, not deleted, into the two `belowApi35…` cases (F3).
  - `WindowThemeApi35FilesTest`: new, two cases (F3).

## Loop 2

### Commits: red, then green
| Commit | Red first (quoted) | Green |
|---|---|---|
| F1 `925f36f` `fix(game): put the timer in Compose's reading order` | (a): `whether the clock's node is screen-reader focusable` / `expected to be true` (GameScreenChromeTest.kt:126).&lt;br&gt;(b): `the stop Compose reads after the title` / `expected: the timer` / `but was : Undo` (:127). I showed (b) by neutralising (a) for one run, then restored the file (`cmp`: identical).&lt;br&gt;After the modifier, before the retarget, `theTitleIsTwentyTwoSp…` and `atDoubleFontScale…` both failed: `IllegalStateException: Key not present: AccessibilityKey: Text … at GameStatusStripTest.timerText(GameStatusStripTest.kt:395)`. That is the ruled `timerText()` retarget. | Chrome 6/6, Strip 9/9, TimerDigits 4/4, GameFlow 8/8 |
| F2 `88c52cb` `fix(game): the timer speaks whole minutes` | Run on the F1 tree:&lt;br&gt;18f: `the timer's description at 00:00` / `expected: Elapsed time, under a minute` / `but was : Elapsed time, 0 seconds` (:293).&lt;br&gt;Holds-still: `the clock's node at 00:01, against 00:00` / `expected: ClockNodeReading(text=null, description=Elapsed time, 0 seconds)` / `but was : ClockNodeReading(text=null, description=Elapsed time, 1 second)` (:357).&lt;br&gt;`theGameplayScreenHasNoMaterialTopAppBar`: `expected to be true` (:72).&lt;br&gt;`theBackControlTitleAndTimerAreAllInTheStatusStrip`: `Assert failed: The component with ContentDescription = 'Elapsed time, under a minute' (ignoreCase: false) is not displayed!` (:85).&lt;br&gt;`ElapsedTimeSpeechTest` failed to compile: `e: …ElapsedTimeSpeechTest.kt:22:37 Unresolved reference 'UnderAMinute'.` (also at :23 and :24). | `ui.game.*` + `ui.theme.*`: 278 tests, 0 failed. `GameFlowTest.kt:129` is unedited and green. |
| F3 `1e2bc3d` `fix(theme): honour the dark navigation bar on API 35 and up` | `whether the light window theme sets android:windowLightNavigationBar below API 35` / `value of: windowSets(...)` / `expected to be false`, and the same for dark.&lt;br&gt;`whether app/src/main/res/values-v35/themes.xml exists` / `expected to be true` (WindowThemeApi35FilesTest.kt:71), and the same for `values-night-v35`. | `ui.theme/game/home`: 421 tests, 0 failed (WindowTheme 4/4, Api35Files 2/2) |

F1's holds-still red shows `text=null` on the F1 tree. So the clearing modifier had already dropped the per-second text; F2 only had to make the description hold still. This is the ruling's predicted order.

### §3: which form ran, and (b)
- **Main form.** The holds-still test turned green with F2 alone, and `TimerDigits.kt` and `TimerDigitsTest` have no diff.
- **§3 (b) ran under Robolectric**, with two conditions. First, `ShadowAccessibilityManager` was enabled, with touch exploration and one `AccessibilityServiceInfo`. Second, the test method runs under NATIVE graphics (see Deviation 1).
- A throwaway probe (deleted, never committed) read every node's hint:
  - Before F1: back → title → Undo → Hint → Show moves → Restart. The clock was not focusable and had no hint. This is B-3 reproduced on the JVM.
  - After F1: back → title → timer ("Elapsed time, 0 seconds", `text` null) → Undo → Hint → Show moves → Restart.

### `git diff --name-only 7448713..HEAD -- app/src/main | sort`
```
app/src/main/java/com/app/pegsolitaire/ui/game/ElapsedTimeSpeech.kt
app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt
app/src/main/res/values-night-v27/themes.xml
app/src/main/res/values-night-v35/themes.xml
app/src/main/res/values-v27/themes.xml
app/src/main/res/values-v35/themes.xml
app/src/main/res/values/strings.xml
```
These are exactly §4.1's files; the two v27 entries are the deletions.

### New arm B
`1e2bc3d`, the F3 commit. A merge-in has not been done (see Open issue 1).

### Deviations
1. **NATIVE graphics on the new chrome case.** The rest of `GameScreenChromeTest` stays LEGACY. Under LEGACY, the provider handed out the root alone: the first run failed with `IllegalStateException: no node for the clock`, which is a harness failure, not a red. The cause is that Compose picks the nodes it hands a service by intersecting `android.graphics.Region`s, which LEGACY stubs. The test's KDoc says so.
2. **`timerText()` is retargeted as ruled, but it is weaker now.** Its two callers compare the cells' layouts with the cells' texts. So "no cell missed" now rests on `theTimerIsStillOneNodeWithNoLiveRegion` (children not empty) and `TimerDigitsTest` N-2 (`hasSize(5)`). I changed the 18a test's KDoc sentence to say what it actually reads.
3. **KDoc-only test edits beside the ruled retargets:** the 18f test, `ElapsedTimeSpeechTest`'s class, `TIMER_AT_THE_START`, and `WindowThemeTest`'s class. `WindowThemeTest` also gains a private `windowTheme()` shared by `windowFlag` and `windowSets`; the status-bar cases' bodies are unchanged.
4. **The ElapsedTimeSpeech red is a compile error**, because the type did not exist yet. I ran the four runtime reds first.
5. **`minutesSaid` is kept** as a one-call helper, for the smallest diff.
6. **The commit trailer is Claude Fable 5.1**, per rules/gitflow.md 1.0.0 and the dispatch, though the code was written on Opus.
7. **This report** is returned as text, so the producer commits it.

## Checks
- `./scripts/check.sh --max-workers=4`, F1 tree (= 925f36f): **pass, exit 0**, BUILD SUCCESSFUL in 11m 8s (21:14:14–21:25:23).
  - `:app` 1248 tests in 121 classes; 0 failures, errors or skips.
  - `:domain:test` was up to date (no `:domain` input changed): 294 tests in 33 classes, 0 failures. `depthSweep` and `koverVerify` ran.
  - No `w:` from `src/main`.
- Same command, F2 tree (= 88c52cb): **pass, exit 0**, 11m 8s (21:28:58–21:40:06). `:app` 1249 tests in 121 classes, `:domain` 294 in 33, 0 failures.
- Same command, F3 tree, run 1: **fail, exit 1**, BUILD FAILED in 11m 8s (21:42:12–21:53:20).
  - `:app:ktlintTestSourceSetCheck`: `WindowThemeApi35FilesTest.kt:72`, 4 × `standard:chain-method-continuation`.
  - Fixed by splitting the `DocumentBuilderFactory` chain over lines. Nothing else changed.
- `./gradlew --max-workers=4 :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.theme.*'`: pass, 19 s.
- Same command, F3 tree, run 2 (= 1e2bc3d): **pass, exit 0**, 11m 8s (21:54:14–22:05:22). `:app` 1251 tests in 122 classes, `:domain` 294 in 33, 0 failures, errors or skips.
- Targeted red and green runs: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '&lt;class&gt;'`, each 1–47 s, results as in the table above.
- Lint report: 64 warnings, all pre-existing. None is on the v35 files, `GameStatusStrip.kt`, `ElapsedTimeSpeech.kt` or `strings.xml` lines 85–100.
- Test warnings: each recompile of a touched test file showed only the pre-existing `createComposeRule` deprecation (the line numbers moved by the new imports). `ElapsedTimeSpeechTest`, `WindowThemeTest` and `WindowThemeApi35FilesTest` compiled with none.
- Frozen files:
  - `git diff --stat 7448713..HEAD -- TimerDigits.kt GameScreen.kt MainActivity.kt Theme.kt Clay.kt ui/home res/values/themes.xml res/values-night/themes.xml AndroidManifest.xml dimens.xml TimerDigitsTest.kt GameFlowTest.kt '*.gradle.kts' gradle build-logic gradle.properties` — empty.
  - `git diff 7448713..HEAD | grep -E '^\+.*(@Suppress|@Ignore|!!)'` — empty.
- Each commit has exactly one trailer line, `Co-Authored-By: Claude Fable 5.1 &lt;noreply@anthropic.com&gt;`, and no `Claude-Session` line. The worktree is clean and no build lock is held.

## Open issues / risks
1. **A merge-in is owed before the sitting.** `develop` is at `f7dd1fe`; the merge-base is still `6aa7624`. It has gained `bugfix/reduced-motion-live-gate`, which changes `ui/theme/Clay.kt` (start-path), `GameScreen.kt`, `TutorialScreen.kt` and `app/build.gradle.kts`. No file overlaps with this loop's. That merge commit would become arm B.
2. **B-1 on API 36 is not proven here.** It rests on the API 35 sources (ruling Open issue 1), and the JVM pin covers only the files. Session D's 29 (2), with `dumpsys` showing no `FORCE_LIGHT_NAVIGATION_BARS`, and the below-35 read decide it.
3. **18g's (a)–(c) and 26b's TalkBack walk are the device's.** Robolectric shows only Compose's hints.
4. **The `timerText()` weakening** (Deviation 2) is low risk. Using the literal "00:00" of `GameUiState.initial()` would restore the stronger form, if the auditor wants it.
5. **The holds-still test reads `text` and the description only.** The clock's bounds are held by its tabular cells (18d).

## Handoff
Next: **code-auditor** — loop 2 (§4.2): review `7448713..1e2bc3d` against the ruling; its report goes to `code-auditor-loop2.md`.
Then: **producer** — the develop merge-in (Open issue 1).
Then: **qa-engineer** — session D, with the new arm B APK checked by sha256.
Then: session S, then the tech-lead's sign-off.
