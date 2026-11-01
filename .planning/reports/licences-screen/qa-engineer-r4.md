# qa-engineer report — licences-screen (device re-check R4: F-4 and the R2 regression)
Date: 2026-11-01
Branch: feature/licences-screen (report only) — device-checked code `65a9d4e`; branch head `76d3bd7` (`git diff --name-only 65a9d4e 76d3bd7` lists only `.planning/reports/licences-screen/android-game-dev-merge-home.md`)
Skills loaded: ergoz-soft:standards (role qa-engineer; the snapshot is 0.5.1 in this checkout and the plugin is 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing were read in full; `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-emulator-skill, android-testing
Ran in parallel with: code-auditor merge review
Ran on: claude-opus-5-5. **Deviation:** the agent file pins fable at high. Under Q-019 Fable-pinned roles run on Opus, the same model as the author, so every grade below rests on a device transcript, not on reading code.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Pass line 4 as ruled (LS-15, option F): PASS.** Run 1 reaches Licenses, Fonts, Sounds, Open-source software and Google libraries in order, none skipped or repeated. Its one non-heading stop, the Gson card, comes right after a TalkBack page scroll and is read whole, title first. Run 3b-down from Jakarta reaches Google libraries after one scroll, with no stop in between.
- **Pass line 2 / 3: PASS on all 38 card and row stops** across runs 1–5 and every positioning swipe. R2 works on the device. The Gson card and the Cronet card under the top bar now read title first; both were read without their titles on `9b6d232`.
- **Run 5 (the R2 regression): PASS.** Notices rows 1–14 are each read once, name first, then "Button" and "k of over a hundred", in order across one TalkBack scroll.
- Runs 2, 3a, 3b-up and 4 are recorded, not graded, as the table says. Two observations are outside the grade: O-R4-1 (a heading swipe from the title sometimes finds nothing) and O-R4-2 (a backward linear swipe skips four entries).

## Build and device
- **Worktree:** `git worktree add --detach <scratchpad>/lic-r4 65a9d4e`, with `local.properties` copied in. The built tree contains R2: `LicensesUi.CARD_LABEL_SEPARATOR = ". "`, `contentDescription = label` in `LicenseEntryCard.kt:113`, `hideFromAccessibility()` at `:160`, and the notices row label at `GoogleNoticesScreen.kt:138`. The worktree was removed at the end, and `git worktree list` no longer shows it.
- **Build:** `./gradlew :app:assembleDebug --console=plain -q` ran at a 1-minute load of 2.45. It produced `app-debug.apk`, 22,014,560 B, at 04:42, **sha256 `4e4049204b19ade369731c88dbe0df9fabda9d5a08f4a55a1ec16774f3dd453b`**. On the device, `sha256sum` of the installed `base.apk` gave the same hash.
  - The producer reported that another agent ran `./gradlew --stop` around 04:50. My build had finished at 04:42, so it was not affected.
- **Device:** AVD `Pixel_9_clean` (1080 × 2424, 420 dpi), cold-booted with `-port 5602 -no-snapshot-load -no-snapshot-save -no-boot-anim` as serial `emulator-5602`.
  - `ergoz_studio` (`emulator-5580`) was running and was not touched. No other AVD was started.
- **Setup:**
  - Light theme, en-US, font scale 1.0, gesture navigation.
  - TalkBack 16 was turned on and off through Settings › Accessibility. The reading control was Headings: the logs show `originalNavigationGranularity=HEADING`, `targetType=TARGET_HEADING` on each swipe down or up.
  - On the fresh install the app first showed the UMP consent form, answered "Do not consent", then the tutorial (skipped) and the Play Games prompt ("Not now"). Then Settings › About › Licenses.
- **Method:**
  - TalkBack gestures went through the emulator console's mouse events, using `gest.py` from the 2026-10-31 re-check.
  - Every run has a raw log (`tb-<run>-raw.txt`) and a timeline (`<run>-timeline.txt`, from `timeline.sh`). The timeline lists each swipe, each TalkBack page scroll (`PerformActionUtils: perform action=ACTION_SCROLL_FORWARD|BACKWARD`), each navigation that found nothing, and every utterance.
  - In TalkBack's log the hint line comes a few milliseconds before the card's speech. That is TalkBack's queueing order, as in the earlier sessions. Each stop's speech is one line.

## Per-run grades (the LS-15 grading table, `ux-designer-ls15.md`)

| Run | What the table grades | Result | Transcript (`.planning/qa/licences-screen/r4/`) |
|---|---|---|---|
| **1. Down from the back button, 7 swipes** | Pass line 4 (a), (b) | **PASS**: title → (one swipe, nothing found; O-R4-1) → Fonts → Sounds → [page scroll] Open-source software → [page scroll] Gson card, whole, title first → [page scroll] Google libraries. Nothing skipped or repeated. The one non-heading stop comes right after a scroll, and it is the only one after that scroll | `run1-timeline.txt`, `tb-run1-down-from-back*.txt`, `r4-03-run1-end.png` |
| **3b. From a mid entry (Jakarta Dependency Injection, entry 11 of 18), down, 3 swipes** | Pass line 4 (a), (b), for the headings below it | **PASS**: [page scroll] Google libraries, with no stop in between. Then "nothing found", then the title (past the last heading: the platform's, not graded) | `run3b-down-timeline.txt`, `r4-07`, `r4-08` |
| 2. Up from the last entry, 6 swipes | Recorded, not graded | Google libraries → [backward page scroll] Licenses (title) → nothing → title → nothing → title. Open-source software, Sounds and Fonts are not reached, the expected outcome. No card stops | `run2-timeline.txt`, `r4-05` |
| 3a. From the title with the list mid-scrolled, down, 3 swipes | Recorded, not graded | nothing → title → nothing. Fonts is not reached, the expected outcome | `run3a-timeline.txt`, `r4-06` |
| 3b. From the same mid entry (Jakarta), up, 4 swipes | Recorded, not graded | [backward page scroll] Open-source software → [backward page scroll] Sounds → Fonts → title. All headings above it were reached | `run3b-up-timeline.txt`, `r4-11` |
| 4. Linear forward from the title, list at its end, 10 swipes | Not graded for order; pass line 2 on each card | Chromium (Cronet) → Public Suffix List → Checker → Google libraries → Google Play services and SDKs (inert) → notices entry → nothing → Navigate back → title → Chromium (Cronet). The same 8-stop cycle as on 2026-10-31, the platform's. **Every card is whole, title first** | `run4-timeline.txt`, `r4-12`, `r4-13` |
| **5. R2 regression** | Pass line 2 on a whole Licenses card and a notices row; about 15 linear swipes on the notices list across one scroll | **PASS**: fully visible Licenses cards (Checker, PSL, Jakarta) and the notices entry are each read once, label first, with "Button" and the hint. On the notices list, 16 swipes: title, intro, then rows "1 of over a hundred" to "14 of over a hundred", each once, name first, then "Found in…", "Button" and the hint. One TalkBack scroll fell before row 8, and no row was skipped or repeated across it | `run5-notices-timeline.txt`, `r4-14`, `r4-15` |
| **Every card stop, runs 1–5 and positioning** | Pass line 2 (3 for an inert card) | **PASS: 38 of 38.** 24 Licenses card stops and 14 notices rows, each read once, whole, title or name first. Tappable cards carry "Button" and their hint. The inert "Google Play services and SDKs" card carries neither (pass line 3) | all `*-timeline.txt` |

**Excerpt** (`run1-timeline.txt` and `run4-timeline.txt`, ≤ 20 lines):
```
04:46:02.003 SWIPE                 SAID Licenses. Heading
04:46:04.003 SWIPE                 navigation found nothing          <- O-R4-1
04:46:05.998 SWIPE                 SAID Fonts. Heading
04:46:07.996 SWIPE                 SAID Sounds. Heading
04:46:09.987 SWIPE   TalkBack page scroll: ACTION_SCROLL_FORWARD
                                   SAID Open-source software. Heading
04:46:11.982 SWIPE   TalkBack page scroll: ACTION_SCROLL_FORWARD
                                   SAID Gson, Tink and Guava. Google. Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations. Button
04:46:13.980 SWIPE   TalkBack page scroll: ACTION_SCROLL_FORWARD
                                   SAID Google libraries. Heading
-- run 4, first stop, the card half under the top bar (on 9b6d232: "BSD 3-Clause License. The Cronet networking classes …", no title)
04:49:26.104                       SAID Chromium (Cronet). The Chromium Authors. BSD 3-Clause License. The Cronet networking classes inside Google Mobile Ads. Button
```

**The same pattern seen again during positioning.** To bring the list to its end for run 4, I swiped down from the title (`tb-pos-to-end.txt`). Those swipes are not a graded run, and none was repeated to change a result. They gave Fonts → Sounds → [scroll] Open-source software → [scroll] Gson card, whole → [scroll] Google libraries. That is run 1's sequence again, without the "nothing found" swipe.

## Observations (outside the grade)
- **O-R4-1 (Low; for the tech-lead's information). A heading swipe from the pinned title sometimes moves nowhere.** TalkBack logs `execute() success=false` for that swipe, nothing is spoken, and focus stays put.
  - It happened on run 1's second swipe, when Fonts was on screen; the next swipe reached Fonts. It also happened in runs 2, 3a and 3b-down, and in the positioning pass after Google libraries.
  - No heading is skipped or repeated, so the table's run 1 line still passes. One swipe is just lost.
  - It fits R1's inference that the title sits outside the scroll container. It is recorded here, not graded.
- **O-R4-2 (Medium candidate; to the producer for the ux-designer and tech-lead). A backward linear swipe skips entries.** I focused Public Suffix List by touch and swiped left (previous) 4 times (`linear-back-timeline.txt`).
  - The swipes gave Chromium (Cronet), then **two backward page scrolls** and OkHttp and Okio, then Dagger and Hilt, then Kotlin and kotlinx.
  - **Protocol Buffers, Annotation and injection APIs, Jakarta Dependency Injection, and Gson, Tink and Guava were skipped** (catalogue order, `LicenseCatalog.kt:155-346`).
  - Every card that was read was whole, title first.
  - **Why it does not affect this verdict.** Backward linear navigation is not one of R4's runs. Pass line 1 grades forward swipes only, and forward swipes from Kotlin reached Dagger → OkHttp → [scroll] Gson → Jakarta with none skipped (`linear-fwd-timeline.txt`).
  - **Likely cause.** It is the same mechanism as R1: TalkBack scrolls a full page backward and picks a newly exposed node.
  - **Who decides.** Whether it is the platform's or a finding is for the ux-designer to rule. It was not repeated.

## Bugs
None graded by the R4 table. O-R4-2 is routed above for a ruling.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/qa-engineer-r4.md` — this report.
- Artifacts under `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/qa/licences-screen/r4/` (git-ignored by `.gitignore:47`, 12 MB, never committed). Sizes are in bytes.

| File | Size | What it shows |
|---|---|---|
| `run1-timeline.txt`, `tb-run1-down-from-back.txt` / `-raw.txt` | 747 / 501 / 784,294 | Run 1: swipes, scrolls, utterances |
| `run2-timeline.txt`, `tb-run2-up-from-last*.txt` | 414 / 228 / 530,540 | Run 2 |
| `run3a-timeline.txt`, `tb-run3a-title-mid-down*.txt` | 175 / 55 / 195,408 | Run 3a |
| `run3b-down-timeline.txt`, `tb-run3b-mid-down*.txt` | 240 / 118 / 276,037 | Run 3b down |
| `run3b-up-timeline.txt`, `tb-run3b-mid-up*.txt` | 355 / 227 / 444,976 | Run 3b up |
| `run4-timeline.txt`, `tb-run4-linear-from-title-at-end*.txt` | 1,734 / 1,669 / 615,759 | Run 4 |
| `run5-notices-timeline.txt`, `tb-run5-notices-linear*.txt`, `tb-run5-open-notices*.txt` | 3,136 / 3,073 / 2,073,954; 104 / 219,909 | Run 5: notices rows 1–14 |
| `to-last-timeline.txt`, `focus-jakarta-timeline.txt`, `linear-back-timeline.txt`, `linear-fwd-timeline.txt`, `pos-to-end-timeline.txt`, and the matching `tb-*.txt` / `tb-*-raw.txt` (`to-last-entry`, `focus-back`, `focus-jakarta`, `focus-psl`, `linear-back-to-jakarta`, `linear-fwd-to-jakarta`, `pos-to-end`, `pos-wrap-title`, `pos-to-notices-entry`) | 55–1,078 (text); 50,957–686,976 (raw) | Positioning swipes. Their card stops are graded by pass line 2. O-R4-2 is in `linear-back-timeline.txt` |
| `r4-00-licenses-top.png` … `r4-15-notices-after-linear.png` (16 files) | 220,684–283,922 | Focus and scroll position at each step (run ends, the mid entry, the list at its end, the notices list) |
| `r4-tb-on.png`, `tb-off-01.png`, `tb-off-dialog.png` | 121,287 / 122,173 / 126,962 | TalkBack turned on, and the "Turn off TalkBack?" dialog used to restore |
| `logcat-app-pid.txt`, `logcat-crash.txt` | 960 / 0 | The app pid's lines from the run logs: six `W AccessibilityDelegate: Drawing order is not available…` warnings at 04:50:04, when the notices list opened, and no `E`/`F` lines. The crash buffer since boot is empty |
| `state-start.txt`, `state-end.txt` | 354 each | The AVD state before and after; identical |
| `emulator-boot.log` | 8,776 | Emulator stdout |
| `*.xml` (`home*`, `set`, `lic-top`, `a11y`, `tbset`, `tbdlg`) | 5,653–26,098 | uiautomator dumps for navigation, all taken with TalkBack off |
| `qa.sh`, `state.sh`, `tb.sh`, `gest.py`, `timeline.sh` | 620–2,199 | Session helper scripts, never committed |

## Checks
- `git worktree add --detach <scratchpad>/lic-r4 65a9d4e`, then `cp local.properties` — pass.
- `./gradlew :app:assembleDebug --console=plain -q` (1-minute load 2.45) — pass. APK 22,014,560 B, sha256 `4e404920…dd453b`.
- `app_launcher.py --serial emulator-5602 --install app-debug.apk`, then on-device `sha256sum <base.apk>` — pass, the same hash.
- Runs 1, 3b-down and 5 — **PASS**. Runs 2, 3a, 3b-up and 4 — recorded. Pass line 2/3 — **PASS**, 38 of 38 stops.
- `app_launcher.py --serial emulator-5602 --uninstall com.erman.pegsolitarie` — pass.
- `diff state-start.txt state-end.txt` — pass (identical).
- `adb -s emulator-5602 shell sync`, then `adb -s emulator-5602 emu kill` — pass. No `Pixel_9_clean` process remains, and `adb devices` lists only `emulator-5580`, which is not mine.
- `git worktree remove --force <scratchpad>/lic-r4` — pass.
- **The Check command was not run.** This is a device re-check in a review loop, and the code-auditor running concurrently owns the Check for this code (R3).

## Device left as found — every system change
- **The app:** not installed at the start; installed, then uninstalled.
- **TalkBack:** turned on and off through Settings › Accessibility. At the end `enabled_accessibility_services` is empty and `accessibility_enabled` is 0, as found. TalkBack's own preferences were not changed: the reading control was already Headings.
- **Not touched:** language, font size, dark mode (`ui_night_mode=1`, light), navigation mode, animations, developer options (on, as found), `wm`, radios.
- **No `settings put`** was used, and no `uiautomator dump` was taken while TalkBack was on.
- **The AVD:** cold-booted by this session and shut down by it.

## Open issues / risks
1. **O-R4-2** (backward linear swipes skip 4 entries) needs a ruling from the ux-designer, with the tech-lead on the mechanism: either the platform's, like the up runs, or a finding. It does not affect this R4 verdict.
2. **O-R4-1** (a lost heading swipe from the pinned title) is recorded for the tech-lead. No action is asked.
3. **Not re-checked**, per the founder's reduced scope:
   - 200 %, dark theme, and the other §8.2 items;
   - the ordinal rows ("Notice k of 2") on the device; R2's JVM tests cover their label.
4. **Staffing:** Opus (Q-019), the author's model. The grades rest on the transcripts above.

## Handoff
Next: tech-lead — R5 item 3 is met. QA passes pass line 2 and pass line 4 as ruled. Sign off once the code-auditor's merge review passes. Producer — route O-R4-2 to the ux-designer for a ruling; it does not block this merge under the R4 table.

VERDICT: PASS
