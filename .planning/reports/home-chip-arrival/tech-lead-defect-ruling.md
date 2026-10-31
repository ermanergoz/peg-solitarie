# tech-lead report — home-chip-arrival (ruling: the device defects B-1, B-2, B-3)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (report only; head `84695b2`, arm B `7448713`, arm A `6aa7624`)
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; and the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: talkback-board QA, licences-screen auditor and ux ruling, reduced-motion ux ruling, colour-vision auditor, tech-lead and ux rulings
Ran on: claude-opus-5-5 at effort xhigh — **staffing deviation:** `agents/tech-lead.md` pins fable; the Fable limit is reached and Q-019 (answer 3) puts every Fable-pinned role on Opus until 3.0.0 is published. Mitigation: every diagnosis below rests on a line of platform, Compose or androidx source, or a QA artifact, listed under Checks, which any later reader can re-read.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/home-chip-arrival/qa-engineer-device.md`, `tech-lead-brief.md`, `tech-lead-device-ruling.md`, `android-game-dev.md`, `code-auditor.md`, `qa-engineer.md`; `PegSolitarie-tb/.planning/reports/talkback-board/tech-lead-brief.md` Amendment 2 (A2.1–A2.7). In part: `docs/ux-spec.md` §4.3.2, §6.2 (D-05f and its table), §8 (duration rows), §11 (traversal), criteria 15e, 15v, 18f, 18g, 26b, 29; `docs/tech-design.md` §3.5 (insets), §9.8, §10 (15e row), §14.1 F-1
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **B-1 — fixed here, theme only.** On API 35+ the platform forces a light navigation bar because the window background is light (`#fafafa`) and the theme names no bar colour. The v27 pair goes. A v35 pair names a transparent bar colour, which makes the platform honour `windowLightNavigationBar`. Reading this also found **B-1b**: below API 35, the v27 light file puts dark buttons on an opaque black bar.
- **B-2 — the §1.6 whole-minute fallback, confirmed.** This ruling fixes its words, its arithmetic and which strings go. The clock's accessibility node must also carry nothing that changes within a minute.
- **B-3 — fixed here, not waived and not moved to talkback-board.** The clock's node keeps hidden cells as semantics children, so Compose leaves it out of its reading order, and TalkBack places it first. The fix is one `clearAndSetSemantics` in `TimerPill`. It also makes B-2's node hold still.
- **Owed:** three red-first commits; the code-auditor's loop 2; a first QA session off the bench (29 (2), a new below-35 read, 18g, 26b), so a failed mechanism does not spend a sitting; then the **15e re-sit**, followed in the same bench session by 15v (all eight recordings), 15w's device half and one dark three-button read.

## 1. B-1 — the dark three-button bar on API 35+
**Diagnosis.** The platform's API 35 sources are on this machine. The API 36 device's flags match them.
1. **Edge to edge is enforced.** `targetSdk` is 36, so the window is enforced edge to edge on API 35 and up (`PhoneWindow.isEdgeToEdgeEnforced`, `:450-459`). The window's bar colour is ignored (`getNavigationBarColor`, `:3969-3975`). The decor draws a contrast scrim instead (`DecorView.calculateBarColor`, `:1404-1423`).
2. **The background decides when the theme names no bar colour.** `DecorView.setWindowBackground` (`:969-977`) then reads the window background's luminance:
   - above 0.5, it sets the hidden `APPEARANCE_FORCE_LIGHT_NAVIGATION_BARS` and moves the background colour into the scrim;
   - the flag's documentation (`WindowInsetsController.java:96-101`) says the system respects `APPEARANCE_LIGHT_NAVIGATION_BARS` only when this flag is cleared.
3. **Our theme names no bar colour, and its background is light in both modes.**
   - Both modes inherit `android:Theme.Material.Light.NoActionBar`.
   - Its `navigationBarColor` is `@color/navigation_bar_default` (`#00808080`), which `PhoneWindow` counts as unspecified (`:2597-2612`).
   - Its `windowBackground` is `?attr/colorBackground`, which is `material_grey_50` `#fafafa`, luminance 0.955.
   - So in dark mode the platform forces a light bar: `#fafafa` at the scrim's alpha over the dark app, which is QA's `(205,204,208)`.
   - `values-night-v27`'s `windowLightNavigationBar=false` is present (QA's `aapt2` read) and is overridden (QA's `dumpsys` read). Both readings are right.
4. **Why `WindowThemeTest` stayed green.** At `sdk = 34` the forcing code does not exist. The test read the theme item, and the item was right.

**B-1b — found by reading, not on a device; the same two files.**
- Below API 35 the window is not edge to edge: `MainActivity` calls no `enableEdgeToEdge()` (tech-design §3.5).
- The decor therefore draws the bar opaque:
  - Material.Light's `navigationBarColor` is `@color/black` on API 27–33 (`platforms/android-30` and `-33`, `themes_material.xml`);
  - on API 34 it is `navigation_bar_compatible`, also black, unless the device makes the default transparent (`PhoneWindow:2604-2610`).
- In light mode, `values-v27/themes.xml`'s `windowLightNavigationBar=true` therefore asks for dark buttons on a black bar on every Android 8.1–14 phone. That breaks §6.2's "light mode unchanged".

**Ruling — the mechanism, in the window theme.** `MainActivity`, `Theme.kt`, the manifest and Gradle are untouched.
- **Delete `res/values-v27/themes.xml` and `res/values-night-v27/themes.xml`.** Below API 35, `res/values/` and `res/values-night/` (no diff) govern again, and the bar is what it was before this branch, in both modes.
- **Add `res/values-v35/themes.xml` and `res/values-night-v35/themes.xml`.** Each restates the whole `Theme.PegSolitaire` on the same parent, with exactly three items:
  - `android:windowLightStatusBar`: true / false, as the base files;
  - `android:windowLightNavigationBar`: true / false;
  - `android:navigationBarColor`: `@android:color/transparent`.
- **Why the transparent colour works.** On API 35+ the colour changes nothing a player sees: the platform keeps the bar transparent. Naming it makes `PhoneWindow` treat the colour as specified (`:2602`, `:2612`), and it does so before `setWindowBackground` runs (`:2820`). So the forced light bar is never set, and `windowLightNavigationBar` decides:
  - dark mode: the dark scrim with light buttons;
  - light mode: the light scrim with dark buttons, as today (`calculateBarColor`'s `ensuresContrast` branch).
- **Comment in each file.** Each file carries a comment saying the above, so that nobody "cleans up" a transparent, deprecated-looking colour. `@android:color/transparent` is the platform's own value, not a hex, so §6.2's "no colour is added" stands.

**Rejected:**
- **(a) A dark framework parent, or a dark `windowBackground`, in night.** It clears the flag through the same heuristic. But it also turns the dark-mode starting window from `#fafafa` to `#303030`, and it changes every framework default the window inherits. That is a visible start-path change the spec has not asked for.
- **(b) `enableEdgeToEdge()` in `MainActivity`.** On API 35+ it clears the flag through the same specified-colour path (androidx 1.13.0 `EdgeToEdgeApi35` sets `window.navigationBarColor`; `PhoneWindow:3978-3988`). But:
  - it is runtime code, not the window theme that §6.2 names;
  - it edits a start-path file that §3.5 keeps untouched;
  - below API 35 it makes every screen edge to edge, which is out of scope for a defect loop.
- **(c) Clearing the hidden flag by its numeric value.** That is a hidden API.

**Tests.**
- **The test that would have been red.** No test at `sdk = 34` can be red for B-1. The one that would have been red is a Robolectric window test at sdk 35/36: night mode, `MainActivity` launched, and the decor's appearance carrying no light-navigation bit.
- **Why it is not on this branch.** This machine holds android-all jars for 14, 9 and 4.4 only (`~/.m2/repository/org/robolectric/android-all-instrumented/`). Robolectric would download 35/36 at test time, which rules/testing.md forbids. Provisioning the jar through Gradle is a build-infrastructure change, so it is a follow-up (Open issue 2).
- **On this branch, red first at `sdk = 34`, in `WindowThemeTest`.** The two D-05f navigation cases are retargeted, not deleted, into:
  - `belowApi35TheLightWindowThemeLeavesTheNavigationButtonsToThePlatform`;
  - `belowApi35TheDarkWindowThemeLeavesTheNavigationButtonsToThePlatform`.

  Each asserts that `obtainStyledAttributes(intArrayOf(android.R.attr.windowLightNavigationBar)).hasValue(0)` is false. Both are red today, because the v27 files define the attribute. This is B-1b's test. The two status-bar cases stand unchanged.
- **The mechanism pin: new `WindowThemeApi35FilesTest`.** A plain JVM test parses the two v35 source files and checks the style name, the parent, the three items and their values. It is red today, because the files are absent. It pins the mechanism, not the behaviour.
- **The behaviour is the device's:** 29 (2) on API 36, and the new below-35 read (§4.4).

## 2. B-2 — the focused timer re-spoken every 2.5–3 s
**Confirmed:** brief §1.6's pre-ruled fallback is the fix (§4.3.2's last bullet; 18g's last sentence). What §1.6 left open is ruled here:
1. **Words.** The description is `elapsed_time_spoken` with the duration said as follows:
   - from 60 s: `duration_minutes` with m = ⌊s / 60⌋;
   - below 60 s: `duration_under_a_minute` = "under a minute" (§8's row);
   - the minutes are floored, never rounded up: 0–59 s "under a minute", 60–119 s "1 minute", 6000 s "100 minutes";
   - minutes never roll into hours.
2. **Arithmetic.** `ElapsedTimeSpeech.parts` returns `SpokenDuration.Minutes(m)` or `SpokenDuration.UnderAMinute` (a `data object`). `Seconds` and `MinutesAndSeconds` are deleted.
3. **Strings.**
   - Add `duration_under_a_minute`.
   - Delete `duration_seconds` and `duration_minutes_seconds`: nothing would read them (brief §1.6, "no dormant … string").
   - Rewrite the D-05e comment in `strings.xml` and the KDocs of `TimerPill` and `ElapsedTimeSpeech`.
4. **The node holds still, not only its description.**
   - Compose sends `TYPE_WINDOW_CONTENT_CHANGED` for every changed property of a node: `CONTENT_DESCRIPTION` for the description, and `UNDEFINED` for any other property, the clock's per-second `text` included (`sendSemanticsPropertyChangeEvents`, ui-android 1.11.3 `:2564-2915`).
   - Nothing on record shows whether TalkBack re-speaks a focused node on `UNDEFINED`. And a second miss on the device would cost a second sitting.
   - So the clock's accessibility node carries nothing that changes within a minute: its description only. B-3's mechanism (§3) gives exactly that.
5. **18f under the fallback.** 18g's last sentence and §4.3.2 ("decided here so that a device finding needs a code change and not a new ruling") govern. 18f's nine times read, word for word:

   | Seconds | The sentence |
   |---|---|
   | 0, 1, 45 | "Elapsed time, under a minute" |
   | 60, 61 | "Elapsed time, 1 minute" |
   | 312 | "Elapsed time, 5 minutes" |
   | 720 | "Elapsed time, 12 minutes" |
   | 3599 | "Elapsed time, 59 minutes" |
   | 6000 | "Elapsed time, 100 minutes" |

   The code-auditor reads 18f's test against this table. The spec note is the ux-designer's (Open issue 3).

**Tests, red first:**
- `ElapsedTimeSpeechTest`'s table, `GameStatusStripTest`'s `SPOKEN_TIMES` with its KDoc, and `GameScreenChromeTest`'s `TIMER_AT_THE_START` are retargeted to the table above.
- New `GameStatusStripTest` · `theTimersAccessibilityNodeHoldsStillWithinAMinute`: the clock's `AccessibilityNodeInfo`, read from the node provider as `TimerDigitsTest`'s N-2 case reads it, has the same `text` and `contentDescription` at 0, 1, 30 and 59 s, and a different description at 60 s. It is red today.
- `GameFlowTest.kt:129`'s substring match stays unedited.

**18g, read again on the device:**
- (a) Moving focus to the timer speaks the minute sentence for the time shown.
- (b) Ten seconds of rest within one minute bring no further speech.
- (c) A rest across a minute boundary (focus at about 00:50, then 20 s of rest) brings at most one more utterance: the new minute.

## 3. B-3 — TalkBack visits the timer first
**Diagnosis.** The cause is in Compose's code, not TalkBack's.
- **How Compose orders the stops.** It writes traversal-before hints, but only between nodes it counts as screen-reader-focusable (`setTraversalValues`, `AndroidComposeViewAccessibilityDelegateCompat.android.kt:3427-3454`; `SemanticsSort.kt`, `geometryDepthFirstSearch`).
- **Who counts.** A node that does not merge its descendants counts only if it is an unmerged leaf with something to say (`isScreenReaderFocusable`, `:3456-3468`). A leaf means `replacedChildren.isEmpty()` (`SemanticsNode.isUnmergedLeafNode`, `:100-106`).
- **Why the clock does not count.** Its node (`TimerDigits`' `Layout`, carrying `text` and the strip's description) keeps its five cells as semantics children. They are hidden, but they are children. So the clock is not a leaf and is left out of the order:
  - no hint points to it, and it points to none;
  - the title's hint goes straight past it, to Undo;
  - its `info.isScreenReaderFocusable` is false (`:1145`).
- **Why TalkBack visits it first.** TalkBack still focuses it by its own rule, a visible node with a description. It places that node, which has no hints, on its own: first, on this build.
- **This matches every QA reading:** the tree order is right and the traversal is wrong; arm A is the same, because the cells have been hidden since N-2, before this branch; Home passes, because its pills use `clearAndSetSemantics`.

**Ruling: fix it here.**
- **The node is this branch's.** D-05e changed it on this branch, and B-2 changes it again.
- **26b is this branch's criterion.**
- **The fix is one modifier** in `GameStatusStrip.kt`, a file talkback-board does not touch (`git diff --name-only 6aa7624...feature/talkback-board -- app/src/main` lists neither `GameStatusStrip.kt` nor `TimerDigits.kt`).
- **It costs no extra sitting:** B-1 re-owes the sitting anyway.
- **Why not waive it.** A TalkBack player reaches the timer only by wrapping past the last stop.
- **Why not ride talkback-board.** It would need a production change there and that branch's re-review, for a node that branch does not own. talkback-board's A2.1 pin (the board region as a traversal group, the slab's index, both in `GameScreen.kt`) is independent of this fix.

**Mechanism.** In `TimerPill`, the modifier passed to `TimerDigits` becomes `Modifier.clearAndSetSemantics { contentDescription = description }`. Three things follow:
1. **The clock joins the reading order.** For accessibility services the cells are replaced, so the clock is an unmerged leaf with something to say. It enters Compose's order inside the pill's group, placed after the title by position: back → title → timer → Undo.
2. **The per-second text goes.** A node's semantics are collapsed innermost first, and a clearing modifier resets everything collected inside it (`LayoutNode.calculateSemanticsConfiguration`, `:470-480`). So `TimerDigits`' own per-second `text` is dropped too, and the node carries the description only (B-2 point 4).
3. **The tests still see the cells.** The cells stay in the unmerged test tree (`getChildren(includeReplacedSemantics = !mergingEnabled)`, `:354-372`). So the cell-layout reads (18a, 18d) and `theTimerIsStillOneNodeWithNoLiveRegion` keep working unedited.

`TimerDigits.kt` and `TimerDigitsTest` have no diff.

**Fallback.** If point 2 does not hold (the holds-still test stays red), `TimerDigits`' own node switches to `clearAndSetSemantics` without `text`, and N-2's text assertion in `TimerDigitsTest` is retargeted. The report says which form ran.

**Rejected:**
- `semantics(mergeDescendants = true)`: it merges the cells' texts into the node, against 18f's "no spoken children".
- A `traversalIndex`: an index reorders only nodes already in the order, and the clock is not in it.
- `isTraversalGroup` on the pill: a group whose only child is excluded adds nothing to the order.

**Test, red first: `GameScreenChromeTest` · `theTimerIsAStopInComposesReadingOrderBetweenTheTitleAndUndo`.**
- **(a)** The clock's `AccessibilityNodeInfo.isScreenReaderFocusable` is true. It is false today.
- **(b)** The same order, read from Compose's own hints:
  - accessibility is turned on for the composition: `ShadowAccessibilityManager` enabled with one enabled service, so that Compose computes its order (`isEnabled`, `:294-299`);
  - `addExtraDataToAccessibilityNodeInfo(…, "android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL", null)` on the title gives the clock's id;
  - the same call on the clock gives Undo's id;
  - today the title gives Undo's.
- If Robolectric does not compute the hints, (a) carries the JVM half, the report says so, and the device half is 26b.
- `GameStatusStripTest`'s `timerText()` helper is retargeted to join the cells' texts, because the node no longer carries `text`.

## 4. What is owed after the fixes
**4.1 The developer.** One android-game-dev dispatch, on opus, in `PegSolitarie-home`. Three code commits after C10, each red first and each green on `./scripts/check.sh`:

| # | Commit | Files | Red first |
|---|---|---|---|
| F1 | `fix(game): put the timer in Compose's reading order` | `ui/game/GameStatusStrip.kt` | §3's test; `timerText()` retargeted |
| F2 | `fix(game): the timer speaks whole minutes` | `GameStatusStrip.kt`, `ui/game/ElapsedTimeSpeech.kt`, `res/values/strings.xml` | §2's tests |
| F3 | `fix(theme): honour the dark navigation bar on API 35 and up` | v27 pair deleted, v35 pair added | §1's tests |

F1 comes before F2: the holds-still test stays red after F1 and turns green with F2. F3 is independent.

The report appends a "Loop 2" section with:
- every red quoted;
- the form §3 ran (main or fallback), and whether §3 (b) ran;
- `git diff --name-only 7448713..HEAD -- app/src/main | sort`, which must list only the files in the table;
- the new arm B hash: the F3 commit, or a later merge-in.

**4.2 The code-auditor, loop 2.** On Opus under Q-019; its report at its own path, `code-auditor-loop2.md`.
- **Scope:** `7448713..<F3>`, with this ruling as the requirement.
- **No diff in the frozen files:** `TimerDigits.kt`, `GameScreen.kt`, `MainActivity.kt`, `Theme.kt`, `Clay.kt`, `ui/home/`, the base theme files, the manifest, `dimens.xml` and every Gradle file.
- **Guards that must still hold:** one node, cells hidden (N-2), no live region, no role.
- **The rest:** no dormant string or type; 18f against §2's table; each v35 file's comment.
- **Probes:**
  - remove the clearing modifier → §3 (a) is red;
  - restore the seconds → holds-still is red;
  - drop `navigationBarColor` from a v35 file → the pin is red;
  - restore the light v27 file → B-1b's case is red.
- **The Check:** its own `--rerun-tasks` run.

**4.3 Does 15e re-sit (C-9)? Yes.**
- **Why.** The APK changes (C-9). And under tech-design §10 the window theme files are start-path: the v35 pair replaces the v27 pair in that list.
- **Protocol:** as before (brief §5, §6.5, D-1, and §2 of the device ruling for any re-attempt).
- **Arms.** A is the merge-base with `develop` at the sitting. B is the new code head.
- **Merge-in.** If `develop` has gained a start-path change since `6aa7624`, the merge-in comes before the sitting.
- **Quiet host (P-4).** Exactly one emulator on the host. The producer shuts down every other emulator, other sessions' included (QA's device report, open issue 2), and holds every Gradle build.

**4.4 The device checks, in an order that protects the sitting.**

**Session D first** — a fresh qa-engineer, before the sitting, on `Pixel_9_clean` (API 36), with the new arm B APK checked by sha256. Report: `qa-engineer-device2.md`. TTS health is checked first.
1. **29 (2), the full matrix.**
   - Three-button and gesture navigation, light and dark, on Home, gameplay and the tutorial.
   - In each dark three-button case, record `dumpsys window`: it must show neither `LIGHT_NAVIGATION_BARS` nor `FORCE_LIGHT_NAVIGATION_BARS`.
   - Dark: scrim luminance ≤ 0.18, buttons ≥ 3:1. Light: as today. The gesture handle ≥ 3:1. The status-bar icons as H-2 left them.
2. **29 below API 35 (new; B-1b).**
   - Device: an AVD on an installed API 27–34 image (`android-27`, `-28` or `-31`). Never the bench and never `Pixel_9`; the producer names it.
   - Three-button navigation, light and dark, on Home and gameplay.
   - The bar must match arm A `6aa7624` in both modes: opaque and dark, with light buttons.
   - Reading arm B `7448713` in light mode as well, to record B-1b, is recommended.
3. **18g:** parts (a), (b) and (c) of §2.
4. **26b:** Home, gameplay (Classic and Challenge) and the tutorial, light and dark, in the same TalkBack sitting as 18g.
   - Gameplay must read: "Navigate back" → title → the timer with its §2 sentence → Undo → Hint → Show moves → Restart.
   - Home and the tutorial read as before; the board stop is still absent and still not a failure.

Any FAIL in session D comes back to me, with the flags and captures, **before** the sitting.

**Session S next** — on the bench. Report: `qa-engineer-sitting2.md`.
- The 15e sitting.
- Then, in the same session:
  - 15v, all eight recordings (one save and two; animations off and on; native and 360×640 dp), parts 1–4, part 2 including the closing chip's shadow, under P-4;
  - 15w's device half, because the APK changed;
  - one 29 (2) read: dark mode, three-button navigation, on Home, on the instrument where B-1 was found (36.1);
  - restore, then shut the bench down (C-8, R-3a).
- If the post-sitting checks run in a fresh agent, the device ruling's C-1 to C-10 apply, re-keyed to the new arm B hash.

**4.5 Then my sign-off**, at `tech-lead.md`. It reads:
- `code-auditor.md` and `code-auditor-loop2.md`;
- `qa-engineer.md` (the first sitting, superseded as the sitting of record);
- `qa-engineer-device.md` (FAIL, superseded);
- `qa-engineer-device2.md` and `qa-engineer-sitting2.md`;
- this ruling and the developer's report.

## Outputs
- `.planning/reports/home-chip-arrival/tech-lead-defect-ruling.md` — this ruling, the only file written. No code, no Gradle, no device.

## Checks
All read-only. Run in `/Users/ermanergoz/Desktop/PegSolitarie-home` unless noted.
- `git status --short; git log --oneline -8` — pass: clean; head `84695b2`; no code since arm B `7448713`.
- `cat res/values{,-night,-v27,-night-v27}/themes.xml`, `grep` on `MainActivity.kt` and the manifest — pass:
  - parent `android:Theme.Material.Light.NoActionBar` in all four files;
  - no `navigationBarColor` and no `windowBackground` item;
  - no `enableEdgeToEdge`; `targetSdk` 36 in `build-logic/.../android-app.gradle.kts`.
- `grep FORCE_LIGHT` in `~/Library/Android/sdk/sources/android-35` — `WindowInsetsController.java:96-101`, `PhoneWindow.java:2597-2612, 2820, 3969-3988`, `DecorView.java:969-977, 1404-1423` read: the heuristic of §1.
- `grep` of `themes_material.xml` and `colors.xml` in `platforms/android-{30,33,34,35,36}/data/res/values` — Material.Light:
  - `navigationBarColor` `@color/black` on 30 and 33, `navigation_bar_default` `#00808080` from 34;
  - `windowBackground` `?attr/colorBackground`, `#fafafa`;
  - dark Material `#303030`.
- `unzip` of `androidx.activity:activity:1.13.0` sources, `EdgeToEdge.kt` — `EdgeToEdgeApi35` sets `window.navigationBarColor` (rejected option b).
- `unzip` of `androidx.compose.ui:ui-android:1.11.3` sources (the BOM 2026.06.00 line) — read for §2 and §3:
  - `SemanticsSort.kt`;
  - `AndroidComposeViewAccessibilityDelegateCompat.android.kt:294-299, 1145-1180, 2564-2915, 3427-3468`;
  - `SemanticsNode.kt:100-106, 339-372`;
  - `LayoutNode.kt:470-480`.
- `cat` of `GameStatusStrip.kt`, `ElapsedTimeSpeech.kt` and `TimerDigits.kt`, and `grep` of the timer tests — pass: the cells are hidden children of the clock's node, which carries `text`; the retarget sites are as §2 and §3 name them.
- `cd PegSolitarie-tb && git diff --name-only 6aa7624...HEAD -- app/src/main` — pass: neither `GameStatusStrip.kt` nor `TimerDigits.kt`.
- `ls ~/.m2/repository/org/robolectric/android-all-instrumented/` — only 14, 9 and 4.4 are present.
- `ls ~/Library/Android/sdk/system-images` plus each AVD's `config.ini` — images 27, 28, 31, 36, 36.1 and 37.0; the `Pixel` AVD is on 27.
- Viewed the QA artifacts `device/18g-dark/sheet.png` (the description re-spoken at 22, 25, 28, 30 and 33 s) and `device/26b/armA-order/sheet.png` (arm A: the timer first).
- Not run: Gradle, the Check, `adb` or an emulator. This is a ruling, and the tech-lead re-runs no gate. `.planning/log/` was not read.

## Open issues / risks
1. **API 36 against API 35.**
   - The B-1 mechanism is read from API 35 sources.
   - API 36 is supported by evidence: the flag QA saw is the one this heuristic sets, and API 36 still ships `navigation_bar_default` and `navigation_bar_compatible`.
   - Session D proves it before any sitting is spent. If the flag persists on 36, I rule again with the flags in hand.
2. **Window tests on the enforcing platform (follow-up, 3.0.1).**
   - Robolectric at sdk 35/36, with android-all resolved through Gradle and `robolectric.offline`, would turn §1's red test into a JVM test.
   - It is test infrastructure (Gradle files), so it stays off this branch and is not a merge condition.
3. **Spec folds (ux-designer), before `release/3.0.0` is cut; not merge conditions:**
   - 29 (1): the v35 placement, `navigationBarColor`, and "below API 35" instead of "API 24–26";
   - §6.2's mechanism bullet, to match;
   - 18f, plus a note under §4.3.2's table: the fallback is live, with §2's sentences;
   - §8: `duration_seconds` and `duration_minutes_seconds` removed, `duration_under_a_minute` added.
4. **Tech-design mirror rows (mine), with the brief's Open issue 4:**
   - §10's start-path list: the v35 pair replaces the v27 pair;
   - §14.1 F-1 and §16 #18: D-05f's mechanism as ruled here;
   - §3.5's "MainActivity is untouched" stays true.
5. **talkback-board's reads are confounded by B-3 until this fix is on its tree.**
   - Its 26b timer position, and TB-1's "swiping forward from the timer", read on its own build, will show B-3.
   - That is this branch's defect, not one for talkback-board's developer.
   - Both are read again after talkback-board takes `develop` in with this branch merged, which §6.2's order already gives.
   - If the producer merges talkback-board first, its sign-off carries B-3 as a known pre-existing defect, closed when this branch merges.
6. **The fallback's cost.** A screen-reader player no longer hears seconds: whole minutes and "under a minute" only. The spec accepted this trade (§4.3.2), and the pill still shows seconds.
7. **B-1b is a reading, not a device finding.** Session D's below-35 check confirms or refutes it. Either way the v27 pair goes, because on API 35+ it is overridden and below 35 it can only do harm.
8. **Staffing.** This ruling, the developer and every reviewer so far ran on Opus (Q-019).

## Handoff
Next: producer —
1. Commit this ruling alone on `bugfix/home-chip-arrival`: `docs(15): record the tech-lead home defect ruling (B-1..B-3)`.
2. Dispatch the android-game-dev, with this ruling and its §4.1, in `PegSolitarie-home`.
3. Then the code-auditor (§4.2).
4. Then QA session D on `Pixel_9_clean` and an API 27–34 AVD (§4.4). Any FAIL comes back to the tech-lead.
5. Then clear the host: every other emulator and every Gradle build (P-4).
6. Then QA session S on the bench: 15e, 15v, 15w and the 29 read (§4.4).
7. Then the tech-lead sign-off (§4.5).
8. Queue the ux-designer's folds (Open issue 3) and my mirror rows (Open issue 4) before the release cut.
9. Tell talkback-board's QA and sign-off about Open issue 5.
