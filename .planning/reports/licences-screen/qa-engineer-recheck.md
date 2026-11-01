# qa-engineer report — licences-screen (device re-check of F-2, F-3, F-4, reduced scope)
Date: 2026-10-31
Branch: feature/licences-screen (report only) — head `2acbe8c`, code `9b6d232` (`git diff --name-only 9b6d232 2acbe8c` lists only two code-auditor reports)
Skills loaded: ergoz-soft:standards (role qa-engineer; the worktree snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin rule files were read in full: studio, reporting, testing, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose; `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-emulator-skill, android-testing
Ran in parallel with: none known to this role
Ran on: claude-opus-5-5 at effort medium. **Deviation:** the agent file pins fable at high. Founder Q-019 puts Fable-pinned roles on Opus, and the founder's directive of 2026-10-31 sets every role one effort step lower (high → medium). The code under test was also written on Opus, so every verdict below rests on a device artifact.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Scope.** The founder's 2026-10-31 directive cuts this to the fixed defects plus one smoke pass. There are no light/dark × every-screen matrices.
- **F-2 is fixed (PASS).** With `[ar-XB]` as the only language, all six strings read in written order with their punctuation in place. Lists hang from the right, wrapped lines included, and text bodies hang from the left. The chrome mirrors.
- **F-3 is fixed (PASS).** At 800 × 1280 dp, Licenses and Settings are capped at 480 dp and centred. At 360 × 640 dp, both are full width.
- **F-4 is NOT fixed on the device (FAIL).** Going up from the last entry, TalkBack moves from "Google libraries" straight to the title and skips three headings. With the list scrolled to its middle, a swipe down from the title goes nowhere. Going down, one stray stop falls between two headings: a card read without its title. All of this happened on the build that has the non-lazy `Column`. By A1.2 it returns to the tech-lead.
- **The smoke round trip passes:** Licenses → Fredoka → back, in light mode, with the scroll position kept.

## Build and device
- **Worktree.** `git worktree add --detach <scratchpad>/lic-recheck 2acbe8c`, with `local.properties` copied in. It was removed at the end with `git worktree remove --force`, and `git worktree list` no longer shows it.
- **Build.** `./gradlew :app:assembleDebug --console=plain -q` gave `app-debug.apk`, 21,980,496 B, **sha256 `9ed4ae350d5d41d0c06e3cbe104b2f50a41c856db98b9a5721d7ad961b84ea9c`**. On the device, `sha256sum` of the installed `base.apk` gave the same hash.
- **The built tree is the fix.** `LicensesScreen.kt:80-82` is `.widthIn(max = …content_max_width).fillMaxWidth().verticalScroll(rememberScrollState())`, a non-lazy `Column`. `ProvideLicencesText(...)` wraps all four screens and `LicensesTopBar`.
- **Device.** AVD `Pixel_9_clean` (1080 × 2424, 420 dpi). It was cold-booted with `-port 5602 -no-snapshot-load -no-snapshot-save -no-boot-anim`, serial `emulator-5602`, and no other emulator was touched. The host load average was 1.7–3.7.
- **Starting state** (`state-start.txt`):
  - `com.erman.pegsolitarie` was **not installed** (`pm path` was empty);
  - locales `en-US`, font scale 1.0, night mode off;
  - TalkBack off, developer options on (left on by the 2026-10-24 session);
  - gesture navigation, animator scale 1.0, no `wm` override.

## Per-item results

| # | Item | Result | Evidence (under `.planning/qa/licences-screen/recheck/`) |
|---|---|---|---|
| 1 | F-2: `[ar-XB]` alone, the six strings in order with their punctuation | **PASS** | `f2-01-arXB-licenses-top.png`, `f2-03-arXB-fredoka-top.png`, `f2-04-arXB-notices-top.png` |
| 1a | F-2: LS-16 edges. On the lists, lines (wrapped ones included) hang right and chevrons sit left. On the text screen, the body hangs left, ragged right. The chrome mirrors | **PASS** | `f2-01`, `f2-02-arXB-licenses-wrapped.png`, `f2-04` (lists); `f2-03` (Fredoka); `f2-arXB-*.xml` bounds |
| 2 | F-3: 800 × 1280 dp, Licenses and Settings capped at 480 dp and centred | **PASS** | `f3-tablet-licenses.png/.xml`, `f3-tablet-settings.png/.xml` |
| 2a | F-3: 360 × 640 dp, full width | **PASS** | `f3-phone360-licenses.png/.xml`, `f3-phone360-settings.png/.xml` |
| 3 | F-4: heading navigation down from the top | **FAIL**: all four headers are reached, but one stray stop falls between "Open-source software" and "Google libraries" | `tb-f4-probe-down.txt`, `tb-f4-down-from-title.txt`, `f4-01-after-down.png` |
| 3a | F-4: heading navigation up from the last entry | **FAIL**: "Google libraries" → title. Open-source software, Sounds and Fonts are skipped | `tb-f4-up-from-last.txt`, `f4-02-at-last-entry.png`, `f4-03-after-up.png` |
| 3b | F-4: from the title, with the list scrolled to its middle (left there by 3a), heading down | **FAIL**: it stays on the title | `tb-f4-mid-down.txt`, `f4-04-after-mid-down.png` |
| 4 | Smoke: Licenses → Fredoka → back, light mode | **PASS** | `smoke-01…04-*.png`; `smoke-licenses.xml` = `smoke-back.xml` |

## What was seen

**F-2 (the `[ar-XB]` language list was set through Settings › Languages: add Arabic (Pseudo-Bidi), then remove English (United States); `system_locales` read `ar-XB`).**
- **Licenses** (`f2-01`):
  - The intro reads "Peg Solitaire is made with these fonts, sounds and open-source software. Thank you to their authors." with the full stop at the end.
  - The other strings read "© 2016 The Fredoka Project Authors", "7 sound effects" and "“Pop 01” by LilMati".
  - Every line ends at the right edge: x 996 for card lines, 1038 for the intro, and the headers at 1017.
  - Chevrons are at the left, pointing left. The back arrow is at the right, pointing right, with the title beside it.
- **Wrapped lines** (`f2-02`): the two-line card lines hang from the same right edge as the card's other lines. Examples: "The Eclipse Jakarta Dependency Injection / project", "javax.inject, JSR-305 annotations, JSpecify, / JetBrains Java annotations", and "The Cronet networking classes inside Google / Mobile Ads".
- **Fredoka text** (`f2-03`, `f2-arXB-fredoka.xml`):
  - Every body node starts at x 42. The body is ragged right, and the layout is identical to the en-US smoke screenshot `smoke-03`.
  - These strings read in order: "…partnership with others.", "…fonts or their derivatives.", "\"Reserved Font Name\" refers to any names specified as such after the copyright statement(s).", and the OFL dash rules whole.
  - The top bar mirrors.
- **Google notices** (`f2-04`): "Google supplies these notices for the open-source software inside its libraries. Each one is shown as supplied." has its full stop at the end, and its lines hang right. The rows and their wrapped "Found in" lines hang right.
- **Clipping:** nothing is clipped on any of the four screenshots.

**F-3 (`wm size` / `wm density`, reset afterwards).**

| Window | Screen | Content column / cards (px) | Expected |
|---|---|---|---|
| 1600×2560 @320 (800×1280 dp) | Licenses | scroll column `[320,278][1280,2496]` = 960 px = 480 dp; cards `[352..1248]` | x 320–1280, centred: **yes** |
| same | Settings | cards `[352,382][1248,542]` etc. | same as Licenses: **yes** |
| 945×1680 @420 (360×640 dp) | Licenses | column `[0,305][945,1617]`; intro and cards `[42..903]` | full width: **yes** |
| same | Settings | column `[0..945]`; cards `[42..903]` | full width: **yes** |

On 2026-10-24 the same tablet window gave cards at `[32..1568]`.

**F-4 (TalkBack on and off through Settings › Accessibility › TalkBack; the reading control was already Headings).** Excerpt from `tb-f4-*.txt`, read in order:
```
# down from the back button (probe, then 6 swipes)
FOCUSED | Licenses. Heading
FOCUSED | Fonts. Heading
FOCUSED | Sounds. Heading
FOCUSED | Open-source software. Heading
HINT    | Double-tap to Read the license
FOCUSED | Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations. Button   <- stray stop, title "Gson, Tink and Guava" and credit "Google" not read
FOCUSED | Google libraries. Heading
FOCUSED | Licenses. Heading                                                    <- past the last heading (the platform's, LS-15)
# up from "Open-source notices in Google libraries…" (the last entry), 6 swipes
FOCUSED | Google libraries. Heading
FOCUSED | Licenses. Heading          <- Open-source software, Sounds, Fonts skipped
FOCUSED | Licenses. Heading
FOCUSED | Licenses. Heading
# list now mid-scrolled (f4-03), focus on the title; 3 swipes down
FOCUSED | Licenses. Heading
FOCUSED | Licenses. Heading          <- Fonts never reached
```
- **This is the 2026-10-24 pattern, unchanged.** The up run and the mid-list run match `rerun/tb-licenses-headings-up.txt` and `-down3.txt`. The down run's stray, title-less card is the same Gson/Tink card seen then.
- **A linear-swipe observation, outside this re-check's scope.** It came while I was moving to the last entry, and it bears on LS-15's "a card read without its title fails pass line 2". The list was left at its end by the down run. A forward swipe from the title then went to "BSD 3-Clause License. The Cronet networking classes inside Google Mobile Ads. Button": the Chromium (Cronet) card, with its title and credit, which were under the top bar, not read. After that, the forward swipes cycled through 8 stops only (back, title, that card, PSL, Checker, Google libraries, Play services, notices entry) and never reached the 15 entries above (`tb-f4-linear-to-last.txt`, 21 lines).
- **Likely cause (for the tech-lead, not verified in code or in TalkBack's source).**
  - The `Column` puts all 23 items in the semantics tree, which the Compose tests assert.
  - But TalkBack's next/previous search on this AVD seems to skip nodes that are not visible on screen. It scrolls the container only when it runs out of visible candidates going forward, and it does not scroll back to reach a header above.
  - So "in the tree" is necessary, not sufficient. Settings passed only because almost all of it fits on one screen.
  - A1.2 foresaw this case: "it is a finding against this mechanism. It returns to me, and LS-15 is not downgraded."

**Smoke (light mode, en-US, 1080 × 2424).**
- Settings › About › Licenses opened (`smoke-02`). Fredoka opened (`smoke-03`, the top bar reads "SIL Open Font License 1.1", the body is complete and readable).
- The in-app back returned to Licenses at the same scroll position: the uiautomator bounds of `smoke-licenses.xml` and `smoke-back.xml` are identical.
- No crash: the crash buffer since boot is empty (`logcat-crash.txt`, 0 lines). `logcat.txt` holds 66 lines for the app's pid after the language change, with no `E`/`FATAL`/`ANR` lines.

## Bugs

### F-4 (Medium, reopened) — heading navigation on Licenses still misses off-screen headers, on the `Column` build
- **Where:** LS-15; §5.1 pass line 4 as restated; §8.2 item 2; tech-lead ruling A1.2.
- **Steps:**
  1. Install `9ed4ae35…` on Pixel_9_clean. Turn TalkBack on, with the reading control on Headings.
  2. Open Settings › Licenses. Focus is on "Navigate back".
  3. Swipe down 7 times. Then focus the last entry and swipe up 6 times. Then, with the list left mid-scrolled and focus on the title, swipe down 3 times.
- **Expected:** title → Fonts → Sounds → Open-source software → Google libraries going down, the reverse going up, and nothing else between two headings, from any scroll position.
- **Actual:**
  - Going down: a stray stop on the Gson, Tink and Guava card, read without its title and credit.
  - Going up: Google libraries → title, with three headings skipped.
  - From the middle: the title only.
- **Evidence:** `tb-f4-down-from-title.txt`, `tb-f4-up-from-last.txt`, `tb-f4-mid-down.txt`, `f4-01…04-*.png`; the linear observation is in `tb-f4-linear-to-last.txt`.
- **Why Medium:** as on 2026-10-24. A1.2 routes it back to the tech-lead for a new mechanism. LS-15 is not downgraded.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/qa-engineer-recheck.md` — this report. The producer commits it.
- Artifacts under `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/qa/licences-screen/recheck/` (git-ignored, about 5 MB). Sizes are in bytes.

| File | Size | What it shows |
|---|---|---|
| `f2-01-arXB-licenses-top.png` | 223,434 | ar-XB Licenses top: strings in order, lines right, chrome mirrored |
| `f2-02-arXB-licenses-wrapped.png` | 225,357 | ar-XB wrapped card lines hanging right |
| `f2-03-arXB-fredoka-top.png` | 350,059 | ar-XB Fredoka: body hangs left, OFL punctuation in place |
| `f2-04-arXB-notices-top.png` | 217,243 | ar-XB Google notices: intro full stop at end, rows right |
| `f2-00-language-list.png` | 148,083 | language settings after the change |
| `f2-arXB-*.xml` | 12,009–17,641 | uiautomator bounds for the three ar-XB screens |
| `f3-tablet-licenses.png/.xml` | 247,760 / 22,259 | 800 × 1280 dp Licenses, column `[320..1280]` |
| `f3-tablet-settings.png/.xml` | 131,829 / 16,313 | 800 × 1280 dp Settings, cards `[352..1248]` |
| `f3-phone360-licenses.png/.xml` | 134,890 / 12,821 | 360 × 640 dp Licenses, full width |
| `f3-phone360-settings.png/.xml` | 113,440 / 13,715 | 360 × 640 dp Settings, full width |
| `tb-f4-probe-down.txt`, `tb-f4-down-from-title.txt`, `tb-f4-down-past-last.txt` | 55, 396, 55 | F-4 down run |
| `tb-f4-to-last-entry.txt`, `tb-f4-up-from-last.txt` | 217, 228 | F-4 up run |
| `tb-f4-mid-down.txt` | 110 | F-4 from mid-list |
| `tb-f4-rep-focus-google-libs.txt` | 0 | one swipe whose utterance was not logged in the wait window, superseded by `tb-f4-mid-down.txt` |
| `tb-f4-linear-to-last.txt` | 3,860 | linear swipes cycling over the visible 8 stops, a title-less Cronet card |
| `f4-00…04-*.png` | 220,727–270,066 | TalkBack focus and scroll position at each F-4 step (`f4-03` = `f4-04`: nothing moved) |
| `tb-off-dialog.png` | 127,366 | the "Turn off TalkBack?" dialog used to restore |
| `smoke-01…04-*.png`, `smoke-*.xml` | 161,633–350,093 | the smoke round trip |
| `logcat.txt`, `logcat-crash.txt` | 7,598, 0 | the app's pid log (no errors); the crash buffer (empty) |
| `state-start.txt`, `state-end.txt` | 354 each | the AVD state before and after, identical |
| other `*.xml` (`home*`, `set*`, `a11y`, `tb*`, `loc*`, `r*`, `ar0`) | 2,637–26,098 | navigation dumps (all taken with TalkBack off) |

Helper scripts, never committed: `qa.sh`, `state.sh`, `tb.sh`, `gest.py` (the last two adapted from the 2026-10-24 run's helpers).

## Checks
- `git diff --name-only 9b6d232 2acbe8c` — pass (two reports only).
- `git worktree add --detach <scratchpad>/lic-recheck 2acbe8c` — pass.
- `./gradlew :app:assembleDebug --console=plain -q` — pass (APK 21,980,496 B, sha256 `9ed4ae35…a9c`).
- `app_launcher.py --serial emulator-5602 --install app-debug.apk`, then on-device `sha256sum` — pass (same hash).
- F-2, F-3, F-4 and the smoke — as in the table: F-2 PASS, F-3 PASS, F-4 **FAIL**, smoke PASS.
- `app_launcher.py --uninstall com.erman.pegsolitarie` — pass.
- `diff state-start.txt state-end.txt` — pass (identical).
- `emulator_manage.py --shutdown emulator-5602` — pass (`adb devices` is empty, and no Pixel_9_clean qemu process remains).
- `git worktree remove --force <scratchpad>/lic-recheck` — pass.
- The Check command was not run. This is a re-check in a build-branch loop, and the auditor's re-run report records the Check for this code.

## Device left as found — system changes, every one
- **The app:** not installed at the start. Installed for the session, then uninstalled.
- **Languages:** changed through the Settings UI to `[ar-XB]`, then back to `en-US` alone. `system_locales` and `persist.sys.locale` both read `en-US`, as found.
- **TalkBack:** turned on and off through Settings › Accessibility. `enabled_accessibility_services` is empty and `accessibility_enabled` is 0, as found. TalkBack's own preferences were not changed; the reading control was already Headings.
- **`wm size` and `wm density`:** overridden for F-3, then reset ("Physical size: 1080x2424", "Physical density: 420", no override).
- **Not touched:** dark mode, font size, navigation mode, animations, developer options (on, as found), radios, airplane mode.
- **The AVD:** shut down. This session booted it.
- **No `settings put`** was used, and no `uiautomator dump` was taken while TalkBack was on.

## Open issues / risks
1. **F-4 is open.** The non-lazy `Column` does not deliver LS-15 on the device: TalkBack does not find headers that are off screen, in either direction. Per A1.2 it returns to the tech-lead for a new mechanism. LS-15 is not downgraded. The linear-swipe observation (a title-less Cronet card, and forward swipes that cycle over only the visible stops) is evidence for the same root cause.
2. **The Compose half of LS-AC06's F-4 tests is green but not sufficient.** It asserts that headers are in the semantics tree, but the device shows that is not what TalkBack's heading search uses. Whatever the next mechanism, the oracle should be re-chosen with it.
3. **Not re-checked, per the reduced scope:**
   - the other §8.2 items;
   - the dark theme and 200 % text;
   - the Google notice text in ar-XB (only Licenses, Fredoka and the notices list were looked at);
   - the regression list in the ruling's re-check item 4.
4. **Staffing deviation:** Opus at medium (Q-019 plus the 2026-10-31 directive), the same model as the author.

## Handoff
Next: producer — route F-4 to the **tech-lead**, per ruling A1.2, with:
- this report;
- `tb-f4-*.txt`;
- the likely cause above.

Then a developer loop, and a QA re-check of F-4 alone. That re-check runs the down, up and mid-list runs, and also a linear swipe from the title with the list at its end. F-2 and F-3 need no further device work on this branch.

VERDICT: FAIL
