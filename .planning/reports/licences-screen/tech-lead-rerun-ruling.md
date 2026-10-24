# tech-lead report — licences-screen (ruling on the device re-run's findings)
Date: 2026-10-24
Branch: feature/licences-screen (report only) — rules on the re-run at `1b79094` (code `bd3ce00`)
Skills loaded: ergoz-soft:standards (role tech-lead; the worktree snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; plus the product's `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-architecture, android-gradle-logic
Ran in parallel with: the ux-designer's O-2/F-4 ruling on this branch, and other branches' work
Ran on: claude-opus-5-5 at effort xhigh. **Deviation:** my agent file pins fable at xhigh. The Fable limit is reached, and the founder's Q-019 answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. This ruling therefore runs on the same model the code was written on; every cause below is checked against the code and the pinned Compose source, not taken from QA's report.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **F-2 is a product defect, and its cause is confirmed.** In the pinned Compose (ui-text 1.11.3), an unset text direction resolves to the *layout* direction, not to the content. Nothing in the app sets one. **Fix:** every text on the four screens takes `TextDirection.ContentOrLtr`, through one licences-scoped typography wrapper. Making this app-wide is routed elsewhere.
- **F-3 is confirmed.** `fillMaxWidth()` before `widthIn(max)` makes the cap a no-op. **Fix:** swap the order, as `ResultsScreen` already does. **Settings is fixed on this branch**, in its own commit. The three dialogs with the same defect are routed to a separate bugfix.
- **F-4 is confirmed.** The lazy list composes only what is on screen, and the tests hid this because they run on a 4,000 dp tall window. **Fix (my ruling, departure T4):** the 23-item Licenses body becomes a non-lazy `Column`, like Settings. The ux-designer rules only the contract text in parallel, and the build does not wait on it.
- **The airplane substitute suffices for the merge.** No network-less run is owed now; the literal radio-off run goes with the release build's device pass. **O-3** was already on `develop` and is outside this spec, so it is routed. The **jank reading** is a record, not a finding.
- **What is owed after the fixes:** one developer loop (a `develop` merge-in, then five commits), the code-auditor with eight probes, and QA's re-check list. No 15e sitting is owed.

## The rulings

### F-2 (High, accepted) — text direction in an RTL locale

**The cause, verified.**
- In the pinned Compose (`androidx.compose.ui:ui-text-android:1.11.3`, the version the build resolves), `resolveTextDirection` in `TextStyle.kt:1596-1611` maps `TextDirection.Unspecified` to the layout direction: `Ltr → TextDirection.Ltr`, `Rtl → TextDirection.Rtl`. Both are forced directions.
- Only `TextDirection.Content` maps to the content-based `ContentOrLtr` / `ContentOrRtl`.
- The Android paragraph then turns `Rtl` into `LayoutCompat.TEXT_DIRECTION_RTL` (`AndroidParagraphIntrinsics.android.kt:152-171`). That forces an RTL paragraph, so neutral characters at either end of a line ("." `"` "©" "7") resolve to RTL and move.
- `grep -rn TextDirection app/src/main/java` finds no use. So every `Text` in the app inherits the layout direction.
- QA's raw strings carry no bidi controls. The licence texts come from assets, which pseudo-locales never touch. Both facts agree with this cause.
- **What this means for the spec.** §4 "Direction" says "the default content-based text direction keeps them left to right". That is false for this Compose: the default follows the layout direction. Content-based direction must be **set**.
- **Why the tests missed it.** No Compose test lays these screens out right to left.

**The fix.**
1. **The value is `TextDirection.ContentOrLtr`, not `Content`.**
   - Each paragraph takes the direction of its first strong character. A paragraph with no strong character at all (a section number such as "1.1.", or a rule of dashes) falls back to LTR.
   - With `Content` in an RTL layout, such a paragraph would fall back to RTL and its punctuation would move. That is LS-07's misrepresentation again, in a smaller place.
   - A Google notice paragraph written in an RTL script would still lay out right to left, which is correct for it.
   - It is one named constant in `ui/licenses/LicensesUi.kt` (for example `LicensesUi.TEXT_DIRECTION`). No literal is written at a call site.
2. **The mechanism is one wrapper, not a style at each call site.**
   - Add a new file `ui/licenses/LicencesTextDirection.kt` with `@Composable internal fun ProvideLicencesTextDirection(content: @Composable () -> Unit)`.
   - It re-provides `MaterialTheme(typography = derived) { content() }`. `derived` is `remember(typography) { … }` over the enclosing `MaterialTheme.typography`, with every one of its styles merged with `TextStyle(textDirection = LicensesUi.TEXT_DIRECTION)`.
   - `colorScheme` and `shapes`, and `motionScheme` if the pinned M3 overload takes one, keep their defaults. Those defaults read the enclosing theme, so only typography changes.
   - Each of the four stateless screens (`LicensesScreen`, `LicenseTextScreen`, `GoogleNoticesScreen`, `GoogleNoticeTextScreen`) wraps its `Scaffold` in it. Previews and tests then get it too.
   - **Why a wrapper.** Every text on these screens passes `style = MaterialTheme.typography.<role>` explicitly. So a `LocalTextStyle` provider would reach none of them, and a per-call-site style would miss the next one written. The wrapper reaches all of them in one place:
     - the top-bar title in `LicensesTopBar`;
     - the reused `SettingsSectionHeader`, which keeps Settings' own file untouched;
     - the card lines, the paragraphs, "End of text", and the error and loading texts.
3. **What it must not be.** Do not use `CompositionLocalProvider(LocalLayoutDirection provides Ltr)`, on a screen or on its body. It would also make the texts LTR, but it un-mirrors the chrome, and §5.1 requires the chrome to mirror.
4. **Alignment follows from the platform, not from new code.** `TextAlign.Start` is relative to the paragraph direction, so English paragraphs sit left-aligned inside the mirrored column. That is what §4's "left to right inside an RTL layout" describes. No `TextAlign` is set. The ux-designer confirms the look in the spec fold (non-blocking, Handoff).
5. **Scope is the four screens only.** Everywhere else in the app (Home, Settings, the game, Results), English text in an RTL-only locale is laid out the same wrong way. That gap was on `develop` before this branch, and only this spec makes it a criterion.
   - The app-wide fix is one change: `textDirection = ContentOrLtr` on every style of `AppTypography` in `ui/theme/Type.kt`. But `ui/theme/` is frozen for this branch (brief §5.2) and is on the 15e void list (brief §5.3).
   - So it goes to the producer: the ux-designer rules the app-wide contract (`docs/ux-spec.md`'s RTL line), then a separate branch makes the change with its own 15e sitting. That branch deletes this wrapper, which the change makes redundant.

**The red-first test.**
- **File.** A new `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesRtlTextDirectionTest.kt`.
- **Setup.** Robolectric Compose, `@GraphicsMode(GraphicsMode.Mode.NATIVE)` (the `HomeLayoutTest` idiom, so the real `StaticLayout` resolves bidi), and the tall window, so that every text is composed. Each screen is set inside `AppTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { … } }`, with the fixture states:
  - the catalogue;
  - Fredoka `Loaded`, and Jakarta `Loaded` with its two parts;
  - a notices fixture that includes an ordinal row;
  - a notice text `Loaded`;
  - every `Failed` state.
- **The oracle.**
  - Collect `onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)`.
  - On each node, invoke the action and assert `getParagraphDirection(0) == ResolvedTextDirection.Ltr`. The failure message names the node's text.
  - **Red** today: the forced `Rtl` gives `Rtl`.
- **The tests:**
  - `everyTextOnTheLicensesScreenIsLaidOutLeftToRightInARightToLeftLayout`
  - `everyTextOnALicenseTextIsLaidOutLeftToRightInARightToLeftLayout` (Fredoka and Jakarta)
  - `everyTextOnTheGoogleNoticesListIsLaidOutLeftToRightInARightToLeftLayout`
  - `everyTextOnAGoogleNoticeTextIsLaidOutLeftToRightInARightToLeftLayout`
  - `theErrorTextsAreLaidOutLeftToRightInARightToLeftLayout`
  - `theChromeStillMirrorsInARightToLeftLayout`, on each screen: the back button's `boundsInRoot.left` lies right of the title's `right`, and a tappable card's chevron lies left of its title. This test is green before the fix and stays green. It is what fails the wrong fix of point 3.
- **Fallback.** If the red run shows `Ltr` before the fix (Robolectric not resolving bidi), the oracle cannot go red. The developer then switches to the structural half, `layoutInput.style.textDirection == LicensesUi.TEXT_DIRECTION` on every text node, and says so in the report. QA's ar-XB pass is then the behavioural half.

### F-3 (Medium, accepted) — the width cap

**The cause, verified.**
- **The code.** The chain is `.fillMaxWidth().widthIn(max = content_max_width)` at:
  - `LicensesScreen.kt:72-73`
  - `GoogleNoticesScreen.kt:73-74`
  - `GoogleNoticeTextScreen.kt:70-71`
  - `LicenseTextScreen.kt:83-84` (`TextColumn`)
  - `SettingsScreen.kt:157-158`
- **The Compose source** (`foundation-layout-android:1.11.3`, `Size.kt`; `ui-unit-android:1.11.3`):
  - `FillNode` measures its child with `minWidth = maxWidth = width`, a tight constraint (lines 697-721).
  - `widthIn` builds a `SizeElement` with `enforceIncoming = true` (line 187). So `SizeNode` measures with `constraints.constrain(targetConstraints)` (lines 830-831).
  - `Constraints.constrain` coerces the target's `maxWidth` into the incoming `[minWidth, maxWidth]` (`Constraints.kt:508-519`).
  - Against the tight width the earlier `fillMaxWidth()` set, `max = 480dp` therefore becomes the window width, and the cap does nothing.
- **The pattern was on `develop` before this branch.** Settings had it at `develop`'s `SettingsScreen.kt:164-165`, and the licences screens copied it.
- **The correct order is already in the app:** `ResultsScreen.kt:129-130` writes `.widthIn(max = …).fillMaxWidth()`.
- **Why the tests missed it.** Every licences Compose test runs at `w411dp-h4000dp`, and 411 dp is narrower than the 480 dp cap. The three `*_TALL_WINDOW` constants are at `LicensesScreenTest.kt:110`, `GoogleNoticesScreenTest.kt:117` and `LicencesTalkBackContractTest.kt:212`.

**The fix.**
- Write `.widthIn(max = dimensionResource(R.dimen.content_max_width)).fillMaxWidth()` at all five sites. Anything after it keeps its place (`.verticalScroll(…)`, `.padding(…)`, `.testTag(…)`, the notices list's `.semantics { collectionInfo … }`).
- **Why it centres.** Each site's parent is a `Box(fillMaxSize, contentAlignment = Alignment.TopCenter)` that does not propagate its minimum. I read all five: `SettingsScreen.kt:141-146`, the `Box` at `LicenseTextScreen.kt:57-62`, and the other three screens' own `Box`.
  - So the incoming width is `0..W`, the cap makes it `0..min(W, 480dp)`, and `fillMaxWidth()` fills that.
  - On a window narrower than the cap, the column still fills it.
- No new token, no literal, and no change to `dimens.xml`.

**Settings: fixed on this branch, in its own commit.** The reasons:
1. It is the same one-line mechanism, under the same test.
2. Settings is this feature's entry point, and this branch already edits it (About, `SettingsRows.kt`, A1.1, A1.4).
3. Without the fix, a tablet goes from a 768 dp Settings column straight to a 480 dp Licenses column, a jump between sibling screens.
4. Settings' own KDoc (`SettingsScreen.kt:56`) promises the cap.
5. QA already has the tablet window set up for the re-check.

The edit is a disjoint hunk from `develop`'s new `SettingsScreen.kt` change (around line 65, from reduced-motion) and from colour-vision's rows and orders.

**Routed elsewhere, not this branch.**
- `ProfileSwapDialog.kt:109-112`, `PlayGamesPromptDialog.kt:103-106` and `AdOfferDialog.kt:120-123` write `.fillMaxWidth().padding(horizontal = …).widthIn(max = …)` with `usePlatformDefaultWidth = false`, so they are uncapped on a tablet for the same reason.
- They are in `ui/home/` and `ui/game/`, which are frozen for this branch, and `ui/home/` is on its 15e void list.
- The producer schedules a `bugfix/dialog-width-cap` for the android-game-dev, with the same test idiom and QA on a tablet window. `TutorialScreen.kt:134-137` records an earlier no-op cap of the same family.
- `HomeScreen.kt:389` and `ClassicCatalogScreen.kt:83` have no `fillMaxWidth()` before the cap and are correct.

**The red-first tests.**
- **File.** A new `ContentColumnWidthCapTest.kt` in `ui/licenses/`. Robolectric Compose, `@Config(qualifiers = "w800dp-h1280dp")`, the tablet window QA used.
- **The oracle.**
  - The content column's node (by its test tag) has width equal to `content_max_width`, read from `R.dimen.content_max_width` through the resources and never written as a number.
  - Its left edge equals `(rootWidth − cap) / 2`, within 1 px.
  - **Red** today: the column measures about 800 dp.
- **Tags.** `LicenseTextScreen`'s column and Settings' column have no tag, so each gains one (`LICENSE_TEXT_BODY_TAG`, `SETTINGS_CONTENT_TAG`), declared in its screen file in the house idiom.
- **The tests:**
  - `theLicensesColumnIsCappedAtContentMaxWidthAndCentredOnAWideWindow`
  - `theLicenseTextColumnIsCapped…`
  - `theGoogleNoticesListIsCapped…`
  - `theGoogleNoticeTextListIsCapped…`
  - and in `SettingsScreenTest` (its own commit), `theSettingsColumnIsCappedAtContentMaxWidthAndCentredOnAWideWindow`.
- **The narrow-window guard.** One method-level `@Config(qualifiers = "w360dp-h640dp")` test per file, `theColumnFillsAWindowNarrowerThanTheCap` (width equals the window's), so the swap cannot regress the phone.

### F-4 (Medium, accepted) — heading navigation in the lazy Licenses list

**The cause, verified.**
- `LicensesScreen` is a `LazyColumn` (`LicensesScreen.kt:69-79`) over 23 items: the intro, four headers and eighteen entries. A lazy list composes only the items in the viewport, plus prefetch, which is not in the semantics tree. So headers that are off screen do not exist for TalkBack's heading search.
- The stray stop and the `O-1` sliver are the same limit seen from another side. TalkBack runs out of composed candidates, scrolls, and lands on whatever node becomes visible.
- **Why the tests missed it.** `LicencesTalkBackContractTest.theLicensesHeadingsAreTheTitleAndTheFourSectionHeaders` (line 132) runs on `w411dp-h4000dp`, where all 23 items compose. It could not see the limit.
- **Why the other screens are unaffected.**
  - Settings, a `Column`, passed the same device check.
  - The two other lazy screens have no heading besides the title: the notices list is flat (§4A), and the notice text's "Found in" line is plain since F-13. Licenses is the only lazy screen with headings to miss.

**The options.**

| # | Option | Ruling |
|---|---|---|
| A | The Licenses body becomes a non-lazy `Column` in `verticalScroll(rememberScrollState())`, so all 23 items are in the semantics tree | **chosen** |
| B | Keep the `LazyColumn`, and name a platform limit in §5.1 pass line 4 ("heading navigation reaches the headers on screen") | rejected: it weakens LS-13 on a static 23-item list that gains nothing from laziness |
| C | Keep it lazy, and add custom accessibility actions ("jump to Sounds") or a large prefetch window | rejected: custom actions are ear-only, which reverses LS-13's rule; prefetched items are not in the semantics tree, so a cache window would not fix it |

**The fix (departure T4 from my brief's §3.4, which said `LazyColumn`).**
- **The shape.** The body is `LicenseTextScreen`'s `TextColumn` shape, with F-3's order:
  ```kotlin
  Column(
      Modifier
          .widthIn(max = …)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(spacing_md)
          .testTag(LICENSES_LIST_TAG),
      verticalArrangement = Arrangement.spacedBy(spacing_md),
  )
  ```
  Inside it, the same intro, headers and `LicenseEntryItem`s, in the same order and with the same `licencesEntrance(order)`.
- `licensesItems` becomes a `@Composable` `ColumnScope` function; the item keys go, since a `Column` needs none.
- **The scroll position** survives the round trip, because `rememberScrollState()` is saveable and the back-stack entry restores it. That keeps LS-AC02.
- **The stagger cap** still animates the first eight items, and the rest appear plainly. Those rest are now composed at open but are off screen, so nothing visible changes. The KDoc sentence "later items, composed as they scroll in" is rewritten.
- **Cost.** About 70 `Text`s composed once at open, the same order as Settings. Nothing is read, decoded or loaded per card, and the catalogue is static.
- **The existing tests are unaffected.** `performScrollToNode` on `LICENSES_LIST_TAG` (`LicensesNavFlowTest.kt:83, 99, 112`; `ButtonSoundTest.kt:324`) supports a non-lazy container through `ScrollBy`. I read this in `ui-test-android` 1.11.3's KDoc of `performScrollToNode`.
  - So no existing test needs editing.
  - If one does need it, because it encodes a lazy-only oracle, that edit is part of this ruling, not an edit to pass. The developer's report lists it and the criterion it keeps.
- **What stays lazy.** The two Google screens stay lazy (LN-4: 108 rows, and 2,497 paragraphs).

**The red-first tests**, in `LicencesTalkBackContractTest`, each with a method-level `@Config(qualifiers = "w411dp-h891dp")`, a phone window:
- `everyLicensesHeadingIsInTheTreeOnAPhoneWindowBeforeAnyScroll`: the unmerged tree's `isHeading()` nodes, read in order, are exactly the title, Fonts, Sounds, Open-source software and Google libraries.
- `everyLicensesEntryIsInTheTreeOnAPhoneWindowBeforeAnyScroll`: all eighteen titles are present.
- **Red** on the `LazyColumn` (an 891 dp window composes about eight items), green on the `Column`. The tall-window tests stay as they are.

**The ux-designer dependency.** The ux-designer is ruling the contract half in parallel:
- how pass line 4 reads the stops TalkBack makes after the last heading, on Jakarta (`tb-jakarta-headings-down.txt`) and in the same way after "Google libraries";
- folding §3.1's "a `LazyColumn` … `rememberLazyListState()`" into the `Column`;
- O-2's "N of N".

That ruling governs QA's reading of pass line 4. **It does not gate this build:** option A is mine and fixes the screen's half whatever the wording. Only one outcome would change the build: the ux-designer rejecting option A on UX grounds. I foresee none. If it happens, the producer returns it to me before the developer is dispatched. Dispatch the developer once both rulings are in, so that the loop is one.

### The airplane substitute (§8.2 item 4, LS-AC07)

**It suffices for the merge, and no network-less AVD run is owed now.** The reasons:
- **"No network access, no outbound intent" is observed directly.**
  - QA opened 121 screens: 13 curated texts and 108 notices, each followed by back. Across those opens, no new remote endpoint appeared in the app uid's `/proc/net` tables.
  - No `ActivityTaskManager: START` appeared.
- **"Works with no network" holds by construction, and two checks prove it.**
  - Every read on these screens goes through the two asset sources (LS-AC14's `assets.open` grep).
  - LS-AC07's review grep finds no `Intent`, `Uri`, `http`, `URL` or `Socket` in `ui/licenses/` and `data/licenses/`. The auditor re-runs that grep on the fixed head (probe 6).
  - A path that opens no connection cannot fail when there is none.
- **What a radio-off run would add:** the app-level behaviour around these screens (ads, Play Games offline). That is not this branch's criterion.
- **The literal wording of §8.2 item 4 is closed at the release.** The release build's device pass on 3.0.0 runs it in airplane mode. That pass is also where R8 is first exercised on these screens (QA's open issue 6). Run it on a disposable AVD clone or the attached phone, never by toggling a shared AVD's radios.
- I make no claim about any particular emulator flag. QA picks the means on the disposable image.

### O-3 — the Settings title is not a heading, and the Licenses row has no "Button"

**Classification: on `develop` before this branch, outside this spec, and routed. It is not a finding against this branch, and this loop writes no code for it.**
- **The evidence:**
  - `develop`'s `SettingsScreen.kt:646` is `claySquish().clickable(onClick = onClick)`, with no `role`. Moved unchanged, it is now `SettingsRows.kt:173`.
  - No `heading()` is set on Settings' title, on `develop` or here.
  - So every tappable Settings row ("How to play", the two privacy rows, Licenses) is read without "Button".
- **Why it is not this branch's.**
  - LS-13's role-and-label rule covers the four licence screens.
  - §2 names the entry row only as "a `SettingsLinkRow`".
  - §8.2 item 11 accepts "the title where it is one".
- **It is still a real gap.** rules/compose.md asks for "contentDescription or semantics role on every interactive element", and `docs/ux-spec.md` §11 asks every title to be a heading.
- **The route.**
  1. The producer routes it to the ux-designer, for the Settings contract: the title as a heading; and on every `SettingsLinkRow`, `Role.Button` plus a click label.
  2. Then a small `bugfix/settings-talkback`, or the talkback-board branch if the producer's plan puts Settings there, with a TalkBack re-check of Settings by QA.
- **Why not this loop.** It changes how every Settings row is announced. That needs strings and a QA pass this loop does not have.

### The Cronet jank reading (LS-AC15)

**Classification: a record, not a finding, and no action on this branch.**
- **The reading.** 6,767 frames, 16.39 % janky, p50 17 ms, p90 34, p95 44, p99 69.
- **What LS-AC15 gates is no ANR, and that holds.** There were 0 app ANRs. The one `am_anr` is Chrome's, during the host stall.
- **The reading cannot show an app defect.**
  - It was taken on an emulator whose host was at load average 64, with a 30 s stall.
  - The scroll was 450 TalkBack page-scroll gestures. Each one composes a page of new paragraph items, which LN-4 accepted as the cost of the lazy body.
  - The p50 of 17 ms sits at the 16.7 ms frame budget.
- **Where the real number comes from:** the release's `compose-performance-audit` (rules/compose.md), by finger fling on the bench device.
- **If that run shows jank,** the first knob is the per-paragraph `SelectionContainer`, one per item (LS-08). The second is the brief's open issue 5, a `Reader`-based re-flow. Neither is built in advance.

### Also read, for the record
- **O-1** is the F-4 limit seen in a different way (see F-4). QA re-observes it on the `Column`. If it recurs there, it is a finding then, with its evidence.
- **LS-AC16's slow-open and error halves** are not reachable on a device whose assets exist. That is accepted. They rest on `LoadingAfterDelayTest` and the screen error tests, as the brief planned.
- **§8.2 item 12** (the date by ear) and **O-2** are not mine. They are in QA's handoff to the producer and to the ux-designer.

## What the developer builds — one loop, in this order
1. `chore(licences): merge develop again (reduced motion, RM close)`.
   - This branch's merge-base is `6aa7624`. `develop` is at `3ab1cae` and has moved with code: `bugfix/reduced-motion-live-gate` changes `ui/theme/Clay.kt`, which the licences entrance and squish use, and `SettingsScreen.kt`, `TutorialScreen.kt`, `GameScreen.kt` and `app/build.gradle.kts`.
   - Merge first, so that the red runs and the review happen on the tree that will land. No rebase.
   - Run the full Check (`./scripts/check.sh`) after the merge. The report lists reduced-motion's named tests and the conflict hunks, if any.
2. `fix(licences): lay the licence texts out by content in an RTL layout` (F-2).
3. `fix(licences): cap the licences columns at content_max_width` (F-3, the four screens).
4. `fix(settings): cap the Settings column at content_max_width` (F-3, Settings).
5. `fix(licences): compose the whole Licenses list so TalkBack finds every heading` (F-4, T4).
6. `docs(licences): record the android-game-dev F-2 to F-4 loop`, in its own commit.

In each commit the named tests are watched red before the code that turns them green. The report records the red runs, and the Check is green on every commit.

**Frozen, unchanged from the brief §5.2:**
- `ui/theme/`, `ui/home/`, `ui/game/`;
- `App.kt`, `MainActivity.kt`, the manifest, `res/font/`;
- `dimens.xml` (no token), `libs.versions.toml` and `build-logic/build.gradle.kts` (no coordinate).

No `@Suppress`, `@Ignore`, baseline or `!!` is added.

## What is owed after the fixes

### The code-auditor
**Scope.**
- The loop's diff `1b79094..<loop head>`, read from the code at `bd3ce00`.
- The merge-in commit's conflict hunks.

**Q-019's mitigation stands.** Opus is reviewing Opus, so the auditor runs the Check itself with the source tasks forced, as in `code-auditor-qa-f1.md`.

**Probes:**
1. **F-3 grep.** `grep -rn -A1 '\.fillMaxWidth()$' app/src/main/java/com/app/pegsolitaire/ui/{licenses,settings}` shows no following `.widthIn(max`. Run a mutation: swap one screen's order back, and its width test goes red.
2. **F-2 mutation.** Remove `ProvideLicencesTextDirection` from one screen, and that screen's RTL test goes red. The wrapper passes `colorScheme`, `shapes` (and `motionScheme`, if present) through by their defaults. The derived typography is `remember`ed, and the direction is the one named constant.
3. **F-2 wrong-fix guard.** Wrap one screen in `LocalLayoutDirection provides Ltr`, and `theChromeStillMirrorsInARightToLeftLayout` goes red.
4. **F-4 mutation.** Revert the body to a `LazyColumn`, and the two phone-window tests go red.
   - LS-AC02's round-trip tests pass unedited, or each edit is justified in the developer's report.
   - The stagger still stops at `STAGGERED_ROWS`.
5. **The 15e void list.** `git diff --name-only develop...HEAD` touches none of `App.kt`, `MainActivity.kt`, `ui/home/`, `ui/theme/`, `res/font/`, a `themes.xml` or the manifest, and adds no `@Inject` into `App`. This is checked after the merge-in, so develop's own `ui/theme/Clay.kt` drops out of the diff.
6. **LS-AC07 and LS-AC14 greps, re-run on the head.** `Intent|Uri|http|URL|Socket` over `ui/licenses` and `data/licenses`. `assets.open` returns exactly the two sources.
7. **LS-AC10.** No coordinate in `libs.versions.toml` or `build-logic/build.gradle.kts`, no new dimens token, no literal dp or sp, and light and dark previews still on every public composable.
8. **Test windows.** The new tests run on the windows named here: RTL with native graphics, `w800dp`, `w360dp` and `w411dp-h891dp`. None runs on the 4,000 dp window. This answers the auditor's carried advisory F2, on how much the oracle covers: the oracle must run on the window the device has.

### QA's re-check (a debug APK is acceptable; the parity task holds)
1. **F-2.**
   - The language list is `[ar-XB]` alone.
   - On all four screens, the six strings QA quoted read in order, with their punctuation in place. Every paragraph is left to right.
   - The chrome mirrors (back arrow, chevrons, title side), and nothing is clipped.
   - Settings stays RTL-paragraphed. That is known and routed (F-2, point 5), and it is not this branch's finding.
2. **F-3.**
   - At 800 × 1280 dp, the column on Licenses, Fredoka, Google notices, Cronet and **Settings** is 480 dp wide and centred (cards at x 320–1280 px at 320 dpi).
   - At 360 × 640 dp, the column is full width, as before.
3. **F-4.**
   - On Licenses, heading navigation from the top, from the bottom and from the middle stops on the title and the four headers, both ways.
   - Stops after the last heading are read by the ux-designer's amended pass line 4.
   - O-1 is re-observed.
4. **Regressions caused by the `Column` and the merge-in:**
   - §5.1 pass lines 1–3 on Licenses (the 25 stops, inert cards one stop, no "Button" on them);
   - the Checker and Jakarta round trips on gesture navigation, the position kept;
   - Remove animations on Licenses and the notices list (§8.2 item 5);
   - a light and dark look at Licenses.
5. **Not owed in this session:** airplane mode (it goes with the release pass, above); §8.2 item 12 (needs audible speech).

### No 15e sitting is owed
- The brief's §5.3 decision stands. This branch changes nothing on Home's first frame.
- The loop touches only `ui/licenses/`, `ui/settings/SettingsScreen.kt` and tests. None of them is on the void list.
- The earlier `navigation/` and `di/` edits are graph inserts and unscoped bindings, already ruled.
- The decision would be voided if the app-wide text-direction change (`ui/theme/Type.kt`) or the dialog fix (`ui/home/`) were pulled into this branch. That is why both are routed elsewhere.

### Then the tech-lead
I give the sign-off after the auditor's PASS and QA's PASS. It re-reads the merge-in's hunks and the departures from my brief: T4, and the F-2 wrapper.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/tech-lead-rerun-ruling.md` — this report, committed alone. Nothing else was written: no code, no spec, no tech design.

## Checks
- `Read` in full:
  - `qa-engineer-rerun.md`, `code-auditor-qa-f1.md` and `ux-designer-address-ruling.md`;
  - `tech-lead-brief.md`, in two pages;
  - `docs/ux/licences-screen.md` LS-07 to LS-14 (lines 200–349), §2–§3.2 (350–483), §3.3–§5.1 (562–898) and §8–§8.2 (1016–1100) — pass.
- `grep -rn 'content_max_width' app/src/main/java`, and `sed` reads of the modifier chain at each of the ten sites, and of `SettingsScreen.kt:136-150`, `LicenseTextScreen.kt` and `Theme.kt:25-37` — pass. Five sites have the wrong order, `ResultsScreen` has the right one, and the three dialogs are routed. Every licences parent is a non-propagating `Box(TopCenter)`.
- `grep -rhoa 'androidx.compose.ui:ui-text-android:[0-9.]*' app/build/intermediates` — pass: 1.11.3 resolved; `foundation-layout-android` 1.11.3.
- `unzip` of the 1.11.3 sources jars (ui-text, foundation-layout, ui-unit, ui-test) to the scratchpad, then reads of:
  - `TextStyle.kt:1596-1611` (`Unspecified → Ltr|Rtl`);
  - `AndroidParagraphIntrinsics.android.kt:152-171`;
  - `Size.kt` `FillNode` (697-721), `widthIn` (`enforceIncoming = true`, 187) and `SizeNode` (830-831);
  - `Constraints.kt:508-519`;
  - `performScrollToNode`'s KDoc (non-lazy needs `ScrollBy`).

  Pass: each cause is as stated.
- `grep -rn 'TextDirection\|LayoutDirection' app/src/main/java` — pass: no `TextDirection` anywhere.
- `grep -rn 'qualifiers\|_WINDOW =' app/src/test/.../ui/licenses` — pass: all three licences test files run on `w411dp-h4000dp`. `GraphicsMode` is used in `HomeLayoutTest` and `HomeStageTest`, and `TimerDigitsTest:221` has the RTL idiom.
- `git show develop:…/SettingsScreen.kt | grep` — pass: `fillMaxWidth().widthIn` at 164-165, and `clickable(onClick = onClick)` with no role at 646, both before this branch. No `heading()` on the Settings title.
- `git log --oneline 6aa7624..develop` and `git diff --name-only 6aa7624 develop -- app build-logic gradle` — pass: develop has moved, with reduced-motion's code (Clay.kt, SettingsScreen.kt around line 65), hence the merge-in in step 1.
- `./gradlew check` — not run. This ruling changes no code. The developer runs it at every commit, and the auditor runs it forced.

## Open issues / risks
1. **The F-2 oracle depends on Robolectric's native graphics resolving bidi.** If it does not, the structural fallback is named above, and QA's ar-XB pass is the behavioural half.
2. **The nested `MaterialTheme` is a new pattern in the app.** Its only intended difference is typography, and probe 2 checks that.
   - It is temporary: the app-wide `AppTypography` change, routed to its own branch with a 15e sitting, deletes it.
3. **App-wide RTL paragraphs** (Home, Settings, the game, Results) are on `develop` before this branch, and unruled. They are routed to the producer for the ux-designer and a separate branch.
4. **The three dialogs' uncapped width** is unverified on a device. It is routed as `bugfix/dialog-width-cap`.
5. **O-3** is routed to the ux-designer for the Settings contract.
6. **The tech design is not updated by this report** (the dispatch: commit only this report). §3.6 should record T4 and the F-2 wrapper in the next docs sync. That is the producer's to schedule.
7. **Staffing (Q-019):** this ruling ran on Opus, the author's model.

## Handoff
Next: producer —
1. **Read and dispatch.** Read this report. Dispatch the **android-game-dev** for the one loop above once the ux-designer's F-4 ruling is in, with:
   - this report;
   - `qa-engineer-rerun.md`;
   - the ux-designer's ruling;
   - the brief.

   Its required skills: `compose-skill` first, then `compose-ui`, `android-testing` and `superpowers:test-driven-development`.
2. **ux-designer** (parallel or next, non-blocking for the build):
   - the F-4 contract half and O-2 (already dispatched);
   - fold §4's "Direction" premise (content direction is **set**, `ContentOrLtr`, and English paragraphs left-aligned inside the mirrored chrome);
   - fold §3.1's `LazyColumn` into the `Column` (T4);
   - rule O-3, the Settings contract;
   - rule whether the RTL text rule is app-wide.
3. **code-auditor**, then **qa-engineer**: the scope, probes and re-check list above.
4. **Schedule, outside this branch:**
   - `bugfix/dialog-width-cap`;
   - the app-wide text-direction branch (with a 15e sitting), after the ux-designer's ruling;
   - the Settings TalkBack fix after O-3's ruling;
   - the release device pass with airplane mode and the release APK;
   - §8.2 item 12 on audible speech;
   - a tech-design sync of T4 and the wrapper.
5. **Tech-lead:** the sign-off once the auditor and QA pass.

## Amendment 1 — LS-15, LS-16 and LS-AC19 (the ux-designer's parallel ruling, `1895850`)
Date: 2026-10-24. Read in full first: `ux-designer-rerun.md`, and LS-15 and LS-16 in `docs/ux/licences-screen.md` (lines 374–463). Where this amendment and the ruling above differ, this amendment governs.

### A1.1 — LS-16's two edges can be met, and the F-2 mechanism is amended to meet them
**The conflict.**
- F-2 item 4 above let `TextAlign.Start` follow the paragraph direction. Under `ContentOrLtr`, that hangs English from the **left** on every screen.
- LS-16 asks for the left edge only on the two text screens. On the two lists, and in the top bars, which are chrome, every line must hang from the mirrored start edge: the **right** in an RTL layout.
- So item 4 is superseded. Nothing else in F-2 changes: the value is still `ContentOrLtr`, the scope is still the four screens, and forcing `LayoutDirection.Ltr` is still forbidden.

**Why an absolute alignment is needed on the lists.** This is checked in the 1.11.3 source.
- `AndroidParagraph.android.kt:643-647` maps:
  - `TextAlign.Start` to `ALIGN_NORMAL` and `End` to `ALIGN_OPPOSITE`, both relative to the **paragraph's** direction;
  - `TextAlign.Left` and `Right` to `ALIGN_LEFT` and `ALIGN_RIGHT`, which are absolute.
- An unset `textAlign` resolves to `Start` (`ParagraphStyle.kt:463`).
- So an LTR paragraph cannot hang from the RTL layout's start through `Start` or `End`. That edge has to be resolved from `LocalLayoutDirection`.
- The column's own `Alignment.Start` is not enough. It places a short, wrap-width text on the right. But a wrapped line fills its box, and with `Start` its lines would sit at the left, which breaks LS-16's "every line of one card shares that edge, wrapped lines included".

**The mechanism, amended.**
- **The wrapper takes the edge.** `ProvideLicencesText(edge: LicencesTextEdge, content)` replaces `ProvideLicencesTextDirection`, in the same new file. `enum class LicencesTextEdge { LayoutStart, TextStart }`.
- **The derived typography** is `remember(typography, edge, layoutDirection)`. Each style merges `TextStyle(textDirection = LicensesUi.TEXT_DIRECTION, textAlign = …)`, where the alignment is:
  - `LayoutStart` → `TextAlign.Right` when `LocalLayoutDirection.current == LayoutDirection.Rtl`, otherwise `TextAlign.Left`. In an LTR layout this equals today's `Start` for English, so nothing moves in an LTR locale.
  - `TextStart` → `TextAlign.Start`. Under `ContentOrLtr`, an English line hangs from the left in every locale, because the edge comes from the text and not from the locale.
  - **Why not `TextAlign.Left` on the text screens.** It would differ from `Start` only for a paragraph whose first strong character is RTL, and none ships. If the ux-designer wants the literal left even then, it is a one-token change, and no test changes.
- **Where each edge applies:**
  - `LicensesTopBar` wraps its title in `LayoutStart` on all four screens, so the chrome mirrors.
  - The bodies of `LicensesScreen` and `GoogleNoticesScreen` use `LayoutStart`: the intro, the section headers (the reused `SettingsSectionHeader`), every card line, and the notices error text.
  - The bodies of `LicenseTextScreen` and `GoogleNoticeTextScreen` use `TextStart`: the entry title and credit, the part headings, "Found in", the ordinal, the paragraphs, "End of text", and the error body.
- **Every text on the four screens is reached.** Each passes `style = MaterialTheme.typography.<role>` (`LicensesTopBar.kt:45`, `LicenseEntryCard.kt:55, 123-133`, `SettingsRows.kt:119`, and the screens). No call site passes `textAlign` or `textDirection`, and the developer adds none, because an explicit one would override the wrapper. The auditor greps for it (probe 2a below).

**The tests, amended. LS-AC19, F-2's direction:**
- The five tests of F-2 above stand as LS-AC19's Compose half:
  - `everyTextOnTheLicensesScreenIsLaidOutLeftToRightInARightToLeftLayout`
  - `everyTextOnALicenseTextIsLaidOutLeftToRightInARightToLeftLayout`
  - `everyTextOnTheGoogleNoticesListIsLaidOutLeftToRightInARightToLeftLayout`
  - `everyTextOnAGoogleNoticeTextIsLaidOutLeftToRightInARightToLeftLayout`
  - `theErrorTextsAreLaidOutLeftToRightInARightToLeftLayout`
- They live in `LicencesRtlTextDirectionTest`, with the oracle `getParagraphDirection(0) == ResolvedTextDirection.Ltr` read through `GetTextLayoutResult`.
- **Each test also proves LS-AC19's kinds were checked**, so that a fixture missing one cannot pass vacuously. It asserts that every kind LS-AC19 names was among the nodes it checked, found by its text: the intro, a section header, a card title and a card line, "Found in", an ordinal line, a curated paragraph, a notice paragraph, and "End of text".
- **Red** on `2d433b8`: the forced `Rtl` gives `Rtl`.

**The tests, amended. LS-16's edges, in the same file, with the same RTL setup:**
- **The fixture** must contain wrapped lines, so the check bites. At 411 dp, the Public Suffix List address and the AndroidX parts line wrap; so do a long "Found in" line and the Fredoka paragraphs.
- **The oracle** works in root coordinates, per line `i` of each text node's `TextLayoutResult`. A line's left edge is `node.boundsInRoot.left + getLineLeft(i)`, and its right edge is `left + getLineRight(i)`.
- `everyLineOnTheLicensesListHangsFromTheMirroredStartEdgeInARightToLeftLayout`:
  - every line of every text in one card ends at that card's one right edge, within 1 px;
  - every line of the intro and the headers ends at the column's content right edge.
- `everyLineOnTheGoogleNoticesListHangsFromTheMirroredStartEdgeInARightToLeftLayout`: the same, for the rows and the intro.
- `everyLineOfALicenseTextHangsFromTheLeftEdgeInARightToLeftLayout`: every line of every body text starts at the column's content left edge, within 1 px. Run it on Fredoka, and on Jakarta's two parts.
- `everyLineOfAGoogleNoticeTextHangsFromTheLeftEdgeInARightToLeftLayout`: the same, on the notice fixture.
- `theTopBarTitleHangsFromTheMirroredStartOnEveryScreenInARightToLeftLayout`: this folds into the chrome-mirroring test above.
- `everyLineHangsFromTheLeftEdgeInALeftToRightLayout`: the LTR regression guard, on all four screens.

**Which tests go red, and when.**
- The two **text-screen** edge tests are red on `2d433b8`, where a forced RTL paragraph hangs right.
- The two **list** edge tests are green on `2d433b8`, because a forced RTL paragraph also hangs right. They are the guard against the direction-only fix. The developer watches them go red in an intermediate step, with `ContentOrLtr` in and the `LayoutStart` alignment not yet in, and records that run, before adding the alignment.

### A1.2 — LS-15 is met by option A
- **The Compose half is met by construction.**
  - In a non-lazy `Column`, the title and the four section headers are in the semantics tree at every scroll position.
  - TalkBack's heading granularity then finds the next and previous heading in the node tree, and the `verticalScroll` container's scroll semantics bring it into view.
  - It never needs the scroll fallback. That fallback produced the skipped headers, the stray stop and O-1's bare "Button".
- **The device half is QA's:** §8.2 item 2's three runs. The evidence that it holds is Settings: the same header component in a plain `Column`, and it passed the same device check.
- **What I do not promise.** I do not promise TalkBack's behaviour past the last heading or before the first. LS-15 names that as the platform's.
- **If QA's runs still hear a stray stop or a bare "Button" between two headings on the `Column`,** it is a finding against this mechanism. It returns to me, and LS-15 is not downgraded.

### A1.3 — LS-AC06's F-4 half: the tests, renamed to the ux-designer's intent
These replace the two F-4 tests named above.
- **File and window.** They go in `LicencesTalkBackContractTest`, each with a method-level `@Config(qualifiers = "w411dp-h891dp")`, a phone window. Never the 4,000 dp class window.
- **The tests:**
  - `eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListAtItsTop`
  - `eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListScrolledToItsMiddle` (after `performScrollToNode(hasText(<Protocol Buffers' title>))`)
  - `eachLicensesHeadingIsInTheTreeAsAHeadingWithTheListScrolledToItsEnd` (after `performScrollToNode(hasText(<the notices entry's title>))`)
- **The oracle, per header and never as a count (F-19).** Five separate assertions, each with its own message:
  - `onNode(hasText(<heading>) and isHeading(), useUnmergedTree = true).assertExists()`;
  - the headings are the title, Fonts, Sounds, Open-source software and Google libraries.
- **Red** on `2d433b8`, at all three positions:
  - at the top, the last headers are not composed;
  - in the middle, Fonts, Sounds and Google libraries are not;
  - at the end, the first headers are not.
  `performScrollToNode` drives both the lazy and the non-lazy body.
- **Kept beside them,** for O-1's precondition: `everyLicensesEntryIsInTheTreeWithItsTitleOnAPhoneWindow`, with the same window and all eighteen entry titles present unscrolled.

### A1.4 — What changes in the owed work
- **The developer:** commit 2 above becomes `fix(licences): lay the licence texts out by content, with LS-16's two edges`. It carries the amended wrapper, the LS-AC19 tests and the edge tests, and records the red runs as A1.1 requires. The order and the other commits stand.
- **The code-auditor:** probe 2 becomes:
  - **2.** Remove the wrapper from one screen, and that screen's LS-AC19 test goes red.
  - **2a.** `grep -rn 'textAlign\|textDirection' app/src/main/java/com/app/pegsolitaire/ui/licenses` shows only the wrapper and `LicensesUi`.
  - **2b.** Swap `LayoutStart` and `TextStart` on one list and on one text screen, and that screen's edge test goes red.
  - **2c.** Remove the `LayoutStart` alignment and keep the direction, and both list edge tests go red.

  The other probes stand.
- **QA:** re-check item 1 (F-2) now reads, with `[ar-XB]` alone:
  - on all four screens, the characters stay in written order and the chrome mirrors;
  - on Licenses and Google notices, every line, wrapped lines included, hangs from the right, and each card keeps its mirrored shape (lines right, chevron left);
  - on Fredoka, Jakarta and Cronet, the body hangs from the left, ragged right, under a mirrored top bar.

  Item 3 (F-4) is read against LS-15 and the restated pass line 4.
- **The 15e answer is unchanged:** the amendment touches only `ui/licenses/`.

### A1.5 — Nothing returns to the ux-designer
LS-16's words stand as written, and both edges are met. One choice is recorded for the ux-designer's information: the text screens use `TextAlign.Start`, not `Left` (A1.1), and that changes nothing for any text that ships.

### Amendment 1 — Checks
- `Read` in full: `.planning/reports/licences-screen/ux-designer-rerun.md` (119 lines); `docs/ux/licences-screen.md` lines 374–463 (LS-15, LS-16) — pass. LS-AC06's F-4 note (line 1204) and LS-AC19 (line 1215) were read from `grep -n` output.
- `grep -rn 'TextAlign.(Start|Left|Right|End) ->'` over the ui-text 1.11.3 sources, and `ParagraphStyle.kt:463` — pass: `Start` and `End` are paragraph-relative, `Left` and `Right` absolute, unset is `Start`.
- `grep -n 'textAlign\|style ='` over `LicensesTopBar.kt`, `LicenseEntryCard.kt` and `SettingsRows.kt` — pass: every text reads `MaterialTheme.typography`, and none sets `textAlign`.
- `./gradlew check` — not run. This is a docs-only amendment.

Lessons applied (Amendment 1): none.
