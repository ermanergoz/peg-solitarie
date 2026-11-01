# qa-engineer report — licences-screen (device re-run after the F-1 fix)
Date: 2026-10-24
Branch: feature/licences-screen (report only) — code under test `2d433b8`, built from a detached worktree at `68ea217` (its tree differs from `2d433b8` only in `docs/` and `.planning/`, checked with `git diff --name-only 2d433b8 68ea217`)
Skills loaded: ergoz-soft:standards (role qa-engineer; the worktree snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-emulator-skill, android-testing
Ran in parallel with: talkback-board QA (ergoz_studio), home-chip-arrival developer loop 2, colour-vision developer loop 1, licences-screen developer F1 comment fix, tech-design RM-close audit
Ran on: claude-opus-5-5 at effort high. This is a deviation: the agent file pins fable at high. The Fable limit is reached, and the founder's Q-019 answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The code under test was also written on Opus, so every verdict below rests on a device artifact, not on reading the code.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **F-1 is fixed on the device.** §5.1 pass lines 1 and 3 pass on the Licenses screen. Each of the five addresses is read as the last line of its card's one stop, and nothing else is a stop. Light at 100 % and dark at 200 % give identical transcripts.
- **Ten items were BLOCKED in the first session. All were run.** These pass: the other three screens' TalkBack linear order with F3's listen (no paragraph read twice, no extra stop); Cronet's continuous read; three-button navigation; Remove animations; 360 × 640 dp; sound; the airplane substitute; and the 108 rows in index order.
- **Three new findings make this a FAIL.**
  - **F-2 (High):** in an RTL locale the licence texts and card lines are laid out right to left, and their punctuation moves (`.partnership with others`).
  - **F-3 (Medium):** on a tablet window the column is not capped at `content_max_width`.
  - **F-4 (Medium):** TalkBack heading navigation on the Licenses list misses section headers that are off screen.
- **Not run:** §8.2 item 12, the PSL date by ear. This session cannot hear speech.
- **Build:** the release build is refused without the `PLAY_UPLOAD_*` variables, so every check used the debug APK. Its generated notices tree is byte-identical to the release variant's.

## Build and device
- **Worktree.** `git -C /Users/ermanergoz/Desktop/PegSolitarie-lic worktree add --detach <scratchpad>/lic-qa 68ea217`, with `local.properties` copied in. Removed at the end with `git worktree remove --force` (only build outputs were in it). `git worktree list` no longer shows it.
- **Release build, refused.** `./gradlew :app:assembleRelease` → `BUILD FAILED in 1s`: "Release build requires the Play upload signing credentials. Missing: PLAY_UPLOAD_KEYSTORE … KEY_ALIAS … KEY_PASSWORD … STORE_PASSWORD" (`app/build.gradle.kts:180`). The dispatch expected a debug-key fallback; this tree has none. Saved as `rerun/build-release-refused.log`.
- **Debug build, used.** `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL in 5s`.
  - APK: `app-debug.apk`, 21,980,496 B, **sha256 `839e38530300132592d4c326581063299ca2b5376d0d5d2d2c0a53a4ec1d6811`**.
  - On the device, `sha256sum` of the installed `base.apk` gave the same hash.
  - Every check below used this APK.
- **Release notices.** `./gradlew :app:generateReleaseGoogleNotices` ran without credentials. `diff -r` of the debug and release `licenses/google/` trees printed nothing (43 files each). `index.tsv` has 108 lines, its last line is `Tink`, and its distinct sources are the ten AARs. Both indexes are saved (`index-debug.tsv`, `index-release.tsv`). So the debug build carries what the release would.
- **Device.** AVD `Pixel_9_clean` (API 36, google_apis_playstore, 1080 × 2424, 420 dpi), cold-booted with `-port 5602 -no-snapshot-load -no-snapshot-save`, serial `emulator-5602`. TalkBack 16.0.0.738667889.
  - No other serial was touched.
  - The AVD was rebooted once mid-session with `adb reboot` (see ar-XB, item 6).
- **Starting app.** The AVD already had `com.erman.pegsolitarie` 3.0.0, installed 2026-09-20 and updated 15:31 today, sha256 `303b1410…cbf49d84`. It was pulled before the install and reinstalled at the end.
- **TalkBack driving.** Gestures went through `adb emu`'s console (`event mouse x y 0 1|0`). Utterances were read from TalkBack's verbose log (`TalkBackFeedbackProvider … ttsOutput=`), the same text "Display speech output" shows as toasts (visible in the `t*` screenshots).
- **Host load.** The host's load average reached 64 at 21:58 and the emulator stopped answering for about 30 s. Every step after that was re-checked by a dump.

## Per-item table

| # | Item | Build | Result | Evidence (under `.planning/qa/licences-screen/rerun/`) |
|---|---|---|---|---|
| 1 | §5.1 pass line 1, Licenses: forward swipe order | debug | **PASS** | `tb-licenses-linear-light100.txt`, `tb-licenses-linear-dark200.txt` (identical; 25 stops from the back button, §5.1 table order) |
| 2 | §5.1 pass line 3, Licenses: inert entries are one stop, the address read last, no "Button" | debug | **PASS** | same transcripts; excerpt A |
| 3 | TalkBack linear order on License text (Fredoka, Jakarta), F3 listen | debug | **PASS** | `tb-fredoka-linear.txt`, `tb-jakarta-linear.txt`, `tb-jakarta-linear-dark200.txt` |
| 4 | TalkBack on Google notices: every row one stop, Button, label | debug | **PASS** | `tb-notices-linear-light100.txt` (108 rows, 0 mismatches with `index-debug.tsv` in name, sources, ordinal and position) |
| 5 | TalkBack on Google notice text (Cronet), F3 listen | debug | **PASS** | `tb-cronet-continuous.txt`, `tb-cronet-linear-dark200.txt` |
| 6 | Pass line 5: continuous read, Cronet ≥ 20 paragraphs, TalkBack's scroll to the end | debug | **PASS** | `tb-cronet-continuous.txt` (20 paragraphs read in sequence, none twice); `t11-cronet-end-by-talkback-scroll.png`; `tb-cronet-end-prev.txt` |
| 6a | Pass line 5 on a curated text (Fredoka): to "End of text" | debug | **PASS** | `tb-fredoka-continuous.txt` (ends `End of text`) |
| 7 | Pass line 4: heading navigation stops on exactly the marked headings | debug | **FAIL on Licenses (F-4)**; PASS on Jakarta's headings, with a stray stop after the last | `tb-licenses-headings-*.txt`, `tb-jakarta-headings-*.txt` |
| 8 | Pass line 2: double-tap opens the named text | debug | **PASS** | `tb-open-fredoka.txt`, `tb-open-jakarta.txt`, `tb-open-notices.txt`, `tb-open-cronet.txt`; `open-every-text.txt`, `open-every-notice.txt` |
| 9 | Pass line 7: back returns one step, nothing spoken twice | debug | **PASS** | `tb-open-*.txt`, `roundtrips-3button.txt` |
| 10 | '108 of 108' (LS-AC13, §8.2 item 10) | debug (index = release) | **PASS on position; wording differs** (O-2) | `tb-notices-linear-light100.txt`: last row "Tink. Found in ads-mobile-sdk. Button. 108 of over a hundred"; `index-*.tsv` 108 lines, last `Tink` |
| 11 | LS-AC16: loading announced once, only when shown; error announced | debug | **PASS** (fast-open half); **NOT REACHABLE** (slow-open and error halves) | `rec-open-cronet-talkback.mp4`, `s28-open-cronet-frames.png`, `tb-open-cronet.txt` |
| 12 | Three-button navigation round trips (LS-AC02, §8.2 item 3) | debug | **PASS** | `roundtrips-3button.txt`, `s07…s09-*.png` |
| 13 | Airplane mode (LS-AC07, §8.2 item 4) | debug | **SUBSTITUTE, no network seen; ruling owed to the tech-lead** | `open-every-text.txt`, `open-every-notice.txt`, `socks-*.txt` |
| 14 | Animations off (§8.2 item 5) | debug | **PASS** | `rec-anim-off-*.mp4` (4 frames each) against `rec-anim-on-open-licenses-control.mp4` (20); `s05-anim-off-all-frames.png` |
| 15 | ar-XB (§8.2 item 6) | debug | **FAIL (F-2)**: the chrome mirrors, the text does not stay LTR | `s23…s26-arXB-*.png` |
| 16 | 360 × 640 dp window (§8.2 item 7) | debug | **PASS** | `s10…s14-360x640-*.png` |
| 17 | Tablet window (§8.2 item 7) | debug | **FAIL (F-3)**: the column is not capped | `s15…s18-tablet-*.png` |
| 18 | Sound (LS-AC09, §8.2 item 9) | debug | **PASS** (one SoundPool start per forward press, none on back; which cue plays is not distinguishable on the device) | `audio-events.txt`, `audio-events-1.txt` |
| 19 | §8.2 item 12: the PSL date by ear | — | **NOT RUN** | `tts-check.txt` |
| 20 | Every text opens, no error state | debug | **PASS** | 13 curated entries (`open-every-text.txt`) and 108 of 108 notices (`open-every-notice.txt`), 0 errors, 0 title mismatches |
| 21 | Cronet scroll: ANR and janky frames (LS-AC15) | debug | **PASS** (no app ANR); jank recorded for the tech-lead | `gfxinfo-cronet-talkback-scroll.txt`, `logcat-crash-anr.txt` |

## How each item was run, and what was seen

**1–2. Pass lines 1 and 3 (Licenses).**
- Settings → About → Licenses, opened by TalkBack double-tap. Focus landed on "Navigate back. Button", then I swiped forward 25 times.
- The stops are back, title (heading), intro ("In list. 23 items"), Fonts (heading), 2 fonts, Sounds (heading), 3 sounds, Open-source software (heading), 11 entries, Google libraries (heading), the SDK entry, the notices entry.
- No address is a stop of its own. The first session's `tb-licenses-linear.txt` had them at lines 10, 12, 14, 36 and 41.
- Excerpt A (`tb-licenses-linear-light100.txt`, 6 of 37 lines):
  ```
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | Interface Sounds by Kenney. 7 sound effects. CC0 1.0 Universal (public domain). kenney.nl/assets/interface-sounds
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | “Pop 01” by LilMati. 2 sound effects. CC0 1.0 Universal (public domain). freesound.org/people/LilMati/sounds/506546
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | “Positive Blip Effect” by CogFireStudios. 1 sound effect. CC0 1.0 Universal (public domain). freesound.org/people/CogFireStudios/sounds/531512
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | Public Suffix List. Mozilla and the list's contributors. Mozilla Public License 2.0. The public-suffix data compiled into OkHttp, as it stood on 2023‑02‑15. github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat. Button
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | Google libraries. Heading
  TYPE_VIEW_ACCESSIBILITY_FOCUSED | Google Play services and SDKs. Play Games Services, Mobile Ads, User Messaging Platform, Play inline-install library, Play services libraries. Android Software Development Kit License Agreement. developer.android.com/studio/terms
  ```
- The four inert cards carry no "Button" and no "Double-tap" hint. Every tappable card is followed by `EVENT_SPEAK_HINT | Double-tap to Read the license`, or `… to Open the notices` for the notices entry.
- On the PSL card the hash is read before "Button", which LS-14 accepts.

**3–5. The other three screens, with F3's listen.**
- **Fredoka:** back, then "SIL Open Font License 1.1. Heading", then "Fredoka. © 2016 The Fredoka Project Authors. Heading", then one stop per paragraph.
- **Jakarta:** back, "Apache License 2.0. Heading", the entry heading, "Notice. Heading", then the NOTICE's paragraphs.
- **Google notices:** title (heading), intro, then 108 rows, each "<name>. Found in <sources>[. Notice n of m]. Button. k of over a hundred".
- **Cronet:** back, "Cronet. Heading", "Found in play-services-cronet", "Notice 1 of 2", then the paragraphs.
- **F3:** across all transcripts no paragraph is read twice (`sort | uniq -d` over `tb-cronet-continuous.txt` prints nothing), and no paragraph adds a stop of its own. The per-paragraph `SelectionContainer` on the notice text and the body-wide one on the curated text add no TalkBack stop.

**6. Continuous read (pass line 5).**
- Started from the TalkBack menu (swipe up then right) with "Read from focused item", from Cronet's back button. It read 26 stops in order, 20 of them paragraphs, with no extra stop (`tb-cronet-continuous.txt`). I then stopped it.
- I scrolled the list to its end with TalkBack's own scroll-forward gesture (swipe right then left), 450 times. `End of text` was then on screen (`t11-cronet-end-by-talkback-scroll.png`) and focusable by TalkBack: "End of text. In list. over a hundred" (`tb-cronet-end-prev.txt`).
- Fredoka's continuous read ran to `End of text` (`tb-fredoka-continuous.txt`).

**10. '108 of 108'.**
- The row positions are spoken 1 to 108, in index order, with the ordinals right on every repeated name: Cronet 2, J2ObjC 5, Kotlin 4.
- A script compared every spoken row with `index-debug.tsv` (name, joined sources, ordinal, position): 0 mismatches.
- TalkBack 16 speaks the total as "over a hundred", not "108". See O-2.

**11. LS-AC16.**
- Under TalkBack, opening Cronet (785 KB) put the text up in one step. The recording shows the list frames, then the loaded Cronet screen, with no spinner frame in between (`s28-open-cronet-frames.png`).
- Nothing was spoken but "Navigate back. Button. Out of list" (`tb-open-cronet.txt`). No "Loading license" and no "Loading notices" on any open in this session. That is correct for an open faster than 300 ms.
- A slow open did not occur on this AVD, even under host load. The error states cannot be reached from the UI of a build whose assets exist. Both halves are covered only by the Compose tests (`LoadingAfterDelayTest`).

**12. Three-button navigation.**
- Switched through Settings › System › Navigation mode › 3-button navigation, then back to Gesture.
- I pressed the nav-bar Back at (230, 2357) on three round trips, and Back again to leave each screen:
  - Licenses → Checker → back: the Checker card at y 1501 before and after.
  - Licenses → Jakarta (two texts) → back: y 1956 before and after.
  - Notices → Cronet row → back: the row at y 1492 before and after. Then back to Licenses, back to Settings, back to Home.
- Gesture back (`KEYCODE_BACK`) and the in-app back icon were also used throughout, with the same results. The notices round trip left the Licenses list at its end (`s02-licenses-after-notices-roundtrip.png`).

**13. Airplane substitute (radio toggles are prohibited).**
- **What was observed.** With TalkBack off and networking left on, I opened all 13 curated entries and all 108 notices, each followed by back. Before and after each pass I snapshotted the app uid's (10212) sockets from `/proc/net/{tcp,tcp6,udp,udp6}`.
  - No new remote endpoint appeared in either pass: `comm -13` of before and after printed nothing.
  - The only socket left was one in CLOSE_WAIT to 142.251.141.227:443, opened before the passes.
  - The logcat `ActivityTaskManager: START` count during the passes was 0, so no outbound intent.
- **What it shows, and what it does not.** It is evidence that these screens open no connection and send no intent. It is not evidence that they work with no network: that needs airplane mode or a network-less AVD, for example one booted with `-no-network` or a radio-less image. The ruling is the tech-lead's.

**14. Animations off.**
- Turned on through Accessibility › Color and motion › Remove animations: all three scales read 0.0.
- Opening Licenses recorded 4 frames: Settings, Settings with the row pressed (no squish), Settings, then the complete list. Opening the notices list also recorded 4 frames.
- The control run with animations on recorded 20 frames, for the entrance stagger.
- Everything opened. Restored, and all three scales read 1.0 again.

**15. ar-XB.** See F-2.
- **How it was set up.**
  - Developer options were turned on (Build number ×7) so the pseudo-locales appear. The AVD was then rebooted, because Settings had cached its language list.
  - Arabic (Pseudo-Bidi) was added and made the system language.
  - With `[ar-XB, en-US]` the app resolves to its `en` resources, and the platform then lays it out LTR (`s21`). So en-US was removed, leaving `[ar-XB]` (`s23`).
- **Mirroring.** The chrome mirrors: the back arrow sits right, the chevrons left, the title right-aligned.
- **Clipping.** Nothing is clipped.

**16–17. Window sizes** (`wm size`/`wm density`, reset afterwards).
- **360 × 640 dp:** `wm size 945x1680` at 420 dpi. Full width, nothing under the status bar. The PSL hash wraps as accepted. The "Found in" lines wrap.
- **Tablet:** `wm size 1600x2560` and `wm density 320`, which is 800 × 1280 dp. See F-3.
- **Reset:** "Physical size: 1080x2424", "Physical density: 420", no override.

**18. Sound.**
- I counted `piid:71 event:started` (the app's SoundPool) in `dumpsys audio` before and after each press:
  - Settings › Licenses row: +1
  - Fredoka entry: +1
  - the notices entry: +1
  - a notice row: +1
  - back icon: +0
  - system back: +0, twice
- Which cue plays (`ButtonForward`) cannot be told apart on the device. That half is `ButtonSoundTest`'s.

**19. §8.2 item 12.**
- Not run. This session has no way to listen to the emulator's audio output. `AudioService` also logs "AudioHardening background playback would be muted for com.google.android.tts (10152), level: full" on each utterance (`tts-check.txt`).
- What TalkBack hands to speech is unchanged: `…as it stood on 2023‑02‑15.` with the U+2011 characters intact. How the engine says it is unheard, as in the first session.
- The item stays open for a device with audible speech and a human ear.

**21. Cronet scroll.**
- `dumpsys gfxinfo com.erman.pegsolitarie`, reset before the 450 TalkBack scrolls: 6,767 frames, janky 1,109 (16.39 %), legacy janky 2,391 (35.33 %), p50 17 ms, p90 34 ms, p95 44 ms, p99 69 ms. Recorded, unbounded, for the tech-lead.
- App ANRs: 0. The one `am_anr` since boot is `com.android.chrome` at 21:59, during the host stall (`logcat-crash-anr.txt`).

## Bugs

### F-2 (High) — in an RTL locale, licence texts and card lines take RTL paragraph direction, and their punctuation moves
- **Where:** §4 "Direction", §4B "Direction", §5 "Dark theme, RTL", §8.2 item 6 ("every text left to right"); LS-07 ("no word is changed").
- **Steps:**
  1. Install `839e3853…` on Pixel_9_clean API 36. Turn on developer options.
  2. Settings › System › Languages: add Arabic (Pseudo-Bidi), make it the system language, then remove English (United States), so the list is `[ar-XB]`.
  3. Open Peg Solitaire › Settings › Licenses, then Fredoka, then Google notices.
- **Expected:** the chrome mirrors, and every text stays left to right with its characters in order. §4 says "the default content-based text direction keeps them left to right inside an RTL layout".
- **Actual:** the chrome mirrors, but every studio string and every licence or notice paragraph is laid out as an RTL paragraph. Right-aligned is only the visible part. The bidi algorithm also moves the neutral characters at each end:
  - intro: ".open-source software. Thank you to their authors" (the full stop moves to the front of the line);
  - credit: "The Fredoka Project Authors 2016 ©";
  - count: "sound effects 7";
  - title: "Pop 01” by LilMati“";
  - OFL text: ".partnership with others", ".fonts or their derivatives", 'Reserved Font Name" refers to any names specified as"';
  - notices intro: ".inside its libraries. Each one is shown as supplied".
- **Evidence:** `s23-arXB-only-licenses.png`, `s24-arXB-fredoka-text.png`, `s25-arXB-notices.png`, `s26-arXB-cronet.png`. With `[ar-XB, en-US]` (`s21-arXB-licenses-top.png`) the app resolves to en and is laid out LTR. The defect therefore shows for a user whose language list holds an RTL language and no language the app supports.
- **Raw strings:** the uiautomator dump has them unchanged and with no bidi controls: `'7 sound effects'`, `'© 2016 The Fredoka Project Authors'`. So the reordering happens at layout, not in the data.
- **Likely cause (for the tech-lead, not verified in code):**
  - Compose resolves `TextDirection.Unspecified` to the layout direction, not to the content's direction.
  - The texts need `TextDirection.Content` (or `ContentOrLtr`), as §4 assumes.
  - The same would apply app-wide. Only these screens' spec makes it a criterion.
- **Why High:** the visual reordering misrepresents legal texts the app exists to reproduce verbatim (LS-07), for every user with an RTL-only language list, and an explicit §8.2 check fails.

### F-3 (Medium) — the content column is not capped at `content_max_width` on a tablet window
- **Where:** §3.1, §4, §4A, §4B ("capped at `content_max_width`, centred"); §8.2 item 7.
- **Steps:** `adb shell wm size 1600x2560; adb shell wm density 320` (800 × 1280 dp). Open Licenses, Google notices and Cronet.
- **Expected:** the column is at most 480 dp (960 px at 320 dpi) wide and centred, so cards span x 320 to 1280.
- **Actual:** cards and paragraphs span x 32 to 1568, which is 768 dp. Bounds from the dumps: Licenses card `[32,264][1568,472]`; notice row `[32,284][1568,444]`; Cronet paragraph `[32,422][1568,1022]`.
- **Evidence:** `s15-tablet-cronet.png`, `s16-tablet-notices.png`, `s17-tablet-licenses.png`. Settings shows the same (`s18-tablet-settings.png`, card `[32,382][1568,542]`).
- **Likely cause (for the tech-lead, not verified):**
  - The modifier chain is `.fillMaxWidth().widthIn(max = …content_max_width)` (`LicensesScreen.kt:72-73`, with the same order in the three other screens and in `SettingsScreen.kt:157-158`).
  - `fillMaxWidth()` first makes the constraint tight, so the later `widthIn(max)` cannot narrow it.
  - The Licenses screens copy a pre-existing Settings pattern.
- **Why Medium:** nothing is clipped and everything stays readable, but the spec's layout criterion fails on every window wider than 480 dp.

### F-4 (Medium) — TalkBack heading navigation on the Licenses list misses the section headers that are off screen
- **Where:** §5.1 pass line 4 ("heading navigation stops on exactly the headings the table marks"), Licenses screen.
- **Steps:** TalkBack on, reading control set to Headings (swipe down then up until "read by headings"). Open Licenses.
  1. From the back button, swipe down six times.
  2. From the last entry, swipe up.
  3. With the list scrolled to its middle, swipe down from the title.
- **Expected:** title → Fonts → Sounds → Open-source software → Google libraries, both ways, and nothing else.
- **Actual:**
  - Down from the top, the four headings are reached, but one stray non-heading stop comes before Google libraries. It is a card scrolled half under the top bar and read without its title: "Google. Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations. Button" (`tb-licenses-headings-fromtop.txt`).
  - Up from the bottom: "Google libraries. Heading" → "Licenses. Heading. Out of list". Open-source software, Sounds and Fonts are skipped (`tb-licenses-headings-up.txt`, `-up2.txt`).
  - With the list scrolled to its middle, down from the title stays on "Licenses. Heading" (`tb-licenses-headings-down3.txt`).
- **Jakarta (not a lazy list):** headings in both directions are right (entry heading, Notice, Apache License 2.0), but past the last heading TalkBack also stops on two paragraphs (`tb-jakarta-headings-down.txt`). That part looks like TalkBack's own scroll-then-focus behaviour, not the app's.
- **Context:** the lazy list composes only the items on screen, so the off-screen headers are not in the tree TalkBack searches. Settings, a `Column`, passed the same check in the first session.
- **Ruling owed:** the tech-lead and the ux-designer decide whether this is a product defect (the Licenses list is 23 items, so a non-lazy body is an option) or a platform limit the contract should name.

### Observations (not findings)
- **O-1 (Low).** When a card is scrolled almost entirely under the top bar, TalkBack can focus it and say only "Button" (`[42,247][1038,318]`, `t02-licenses-sliver.png`, `tb-lh-next.txt`: "Button. In list. 23 items"). Its text lines are clipped, so TalkBack drops them. This was seen only after heading-navigation scrolling; the linear passes never hit it.
- **O-2 (wording).** TalkBack 16 says "k of over a hundred" for a list of 108, so "108 of 108" is never heard. The position 108 is spoken on the last row. The intent (rows counted alone, last row = N) holds. LS-AC13 and §8.2 item 10's literal wording needs the ux-designer to restate it for TalkBack's rounding.
- **O-3.** Settings' title "Settings" is read without "Heading", and the Licenses row is read without "Button" (`tb-s-walk.txt`). Both are outside this re-run's scope and were not ruled; recorded for whoever owns the Settings contract.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/qa-engineer-rerun.md` — this report.
- Artifacts under `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/qa/licences-screen/rerun/` (git-ignored, 19 MB). Sizes are in bytes.

**TalkBack transcripts (`tb-*.txt`; "FOCUSED" lines are stops, "HINT" lines are TalkBack's hints)**

| File | Size | What it shows |
|---|---|---|
| `tb-licenses-linear-light100.txt` | 4,226 | Licenses, all stops, light at 100 % |
| `tb-licenses-linear-dark200.txt` | 4,226 | the same at dark 200 %, identical |
| `tb-notices-linear-light100.txt` | 23,222 | all 108 notice rows |
| `tb-cronet-continuous.txt` | 8,595 | the continuous read |
| `tb-cronet-linear-dark200.txt` | 2,417 | Cronet, chrome and first paragraphs at dark 200 % |
| `tb-cronet-end-prev.txt` | 178 | "End of text" reached by TalkBack |
| `tb-fredoka-linear.txt` | 2,577 | Fredoka, linear |
| `tb-fredoka-continuous.txt` | 3,091 | Fredoka, continuous read |
| `tb-jakarta-linear.txt` | 771 | Jakarta, linear |
| `tb-jakarta-linear-dark200.txt` | 461 | Jakarta at dark 200 % |
| `tb-jakarta-headings-up.txt`, `-down.txt` | 243, 937 | Jakarta, heading navigation |
| `tb-licenses-headings-*.txt` | 165–520 | F-4 |
| `tb-open-*.txt` | 104–117 | what is spoken on each open |
| `tb-settings-linear*.txt`, `tb-s-walk*.txt` | 131–1,120 | Settings to the Licenses row |
| other `tb-*.txt` | 0–434 | navigation probes |

**Device-side records**

| File | Size | What it shows |
|---|---|---|
| `open-every-text.txt` | 1,245 | 13 curated entries opened, titles, 0 errors |
| `open-every-notice.txt` | 39,222 | 108 of 108 notices opened, 0 errors, 0 title mismatches |
| `roundtrips-3button.txt` | 3,382 | the three-button round trips (two early attempts carry `input tap` errors from a failed lookup; the result lines follow them) |
| `socks-before.txt`, `socks-after.txt`, `socks-before-notices.txt`, `socks-after-notices.txt` | 18 each | the app's remote socket endpoints |
| `audio-events.txt`, `audio-events-1.txt` | 470, 374 | SoundPool start events |
| `gfxinfo-cronet-talkback-scroll.txt` | 4,372 | jank after the Cronet scroll |
| `logcat-crash-anr.txt` | 223 | the crash buffer (empty) and `am_anr`/`am_crash` events |
| `tts-check.txt` | 795 | AudioHardening lines for the TTS engine |
| `index-debug.tsv`, `index-release.tsv` | 12,199 each | the generated indexes, identical |
| `build-release-refused.log` | 1,847 | the refused release build |

**Recordings**

| File | Size | What it shows |
|---|---|---|
| `rec-anim-off-open-licenses.mp4` | 177,326 | opening Licenses with animations off |
| `rec-anim-off-open-notices.mp4` | 236,178 | opening the notices list with animations off |
| `rec-anim-on-open-licenses-control.mp4` | 742,393 | the control run with animations on |
| `rec-open-cronet-talkback.mp4` | 2,553,418 | opening Cronet under TalkBack (LS-AC16) |

**Screenshots**

| Files | What they show |
|---|---|
| `s01`, `s02` | Licenses; its position after the notices round trip |
| `s03`–`s05` | animations off |
| `s06`–`s09` | three-button navigation |
| `s10`–`s14` | 360 × 640 dp |
| `s15`–`s18` | tablet (F-3) |
| `s19`–`s27` | ar-XB (F-2) |
| `s28` | the Cronet-open frames |
| `t00`–`t17` | the TalkBack session (menu, continuous read, dark 200 %) |
| `q1`–`q3` | the Quick Settings attempt |
| `cons/Screenshot_*.png` (8) | console screenshots taken while adb was offline |

**Helper scripts (never committed)**
- `gest.py`, `tb.sh`, `qa.sh`, `findtap.sh`, `focus.sh`, `open-all.sh`, `open-notices.py`, `rt3.sh`, `rt3b.sh`, `to-licenses.sh`, `title.py`, `socks.sh`, `tbscroll.py`, `cshot.py`.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-lic worktree add --detach <scratchpad>/lic-qa 68ea217` — pass.
- `git diff --name-only 2d433b8 68ea217` — pass (two reports and the spec only).
- `./gradlew :app:assembleRelease --console=plain` — **fail** ("Release build requires the Play upload signing credentials", 4 variables missing).
- `./gradlew :app:assembleDebug --console=plain` — pass (`BUILD SUCCESSFUL in 5s`; sha256 `839e3853…6811`).
- `./gradlew :app:generateReleaseGoogleNotices` and `diff -r` of the debug and release `licenses/google/` — pass (identical, 108 index lines).
- `adb -s emulator-5602 install -r app-debug.apk`, then on-device `sha256sum` — pass (same hash).
- The Check command was not run: this is the re-run of a build-branch session, and the Check at `2d433b8`'s code is recorded green in `code-auditor-qa-f1.md` (1,326 `:app` tests).
- `git worktree remove --force <scratchpad>/lic-qa` — pass.

## Device left as found — system changes, every one
Restored to the starting value:
- TalkBack: `enabled_accessibility_services` empty, `accessibility_enabled` 0.
- Font size: `font_scale` 1.0.
- Dark theme: off, `ui_night_mode` 1.
- Animations: all three scales 1.0.
- Navigation: gesture, `navigation_mode` 2.
- `wm size` and `wm density`: reset.
- Airplane mode: never touched, 0.
- `system_locales`: en-US.
- The app: the original APK reinstalled (`adb install -r -d`). On-device sha256 `303b14100d8a42d1deac1f4860aa3a01c2db0820bc98cc05b599dd54cbf49d84`, as found. Its data was kept by `-r`.
- Shutdown: the AVD was shut down with `emulator_manage.py --shutdown emulator-5602`; no `Pixel_9_clean` qemu process remains.

**Not restorable through the Settings UI. Each is recorded so the producer can decide (wipe or accept):**
1. **Developer options are ON** (`development_settings_enabled` 1; it was unset).
   - I turned them on for the ar-XB pseudo-locale.
   - Turning them off through the UI also turned off USB debugging (`adb_enabled` 0) and took the AVD offline for adb.
   - I re-enabled both through console taps, because an AVD without adb would be unusable for the next session.
   - USB debugging (`adb_enabled` 1) is as found. Developer options are not.
2. **`persist.sys.locale` is now `en-US`.** It was empty, with `system_locales` en-US. The effective locale is unchanged.
3. **Quick Settings gained the "Font size" tile.** The system adds it automatically on the first font-size change, along with `accessibility_font_scaling_has_been_changed`=1 and `font_scaling` in `qs_auto_tiles`.
   - My one attempt to remove the tile in the QS editor closed the editor and changed nothing (`sysui_qs_tiles` was the same before and after).
   - I did not try again, because the editor's drag gestures risked reordering the other tiles.
4. **TalkBack's own preferences:**
   - the reading control was left at "Headings";
   - the "Get detailed image descriptions?" prompt was shown once and declined ("No thanks");
   - the continuous-reading tip's "Always show this message" was left checked;
   - "Display speech output" and the verbose log were as the first session left them, on.
5. **One reboot of this AVD** (`adb reboot` at ~21:48), to refresh Settings' cached language list.

## Open issues / risks
1. **F-2, F-3 and F-4 are open.** F-2 and F-3 are code changes for the android-game-dev after the tech-lead's ruling. F-4 needs a ruling first: product defect, or a platform limit the §5.1 contract should name.
2. **The airplane check (§8.2 item 4) has only the substitute evidence above.** The tech-lead rules whether it suffices, or whether a network-less AVD run is wanted.
3. **§8.2 item 12 (the date by ear) is still unverified.** It needs a device with audible speech and a listener.
4. **LS-AC16's slow-open and error halves are not reachable on the device.** They rest on the Compose tests.
5. **O-2:** LS-AC13 and §8.2 item 10 ask for "108 of 108", which TalkBack 16 never says.
6. **The release APK was not exercised.** The generated notices are proven identical to the debug variant's, but R8 and minification were not on the device in this session.
7. **The AVD is not fully as found** (system changes 1–4 above). A wipe-data, or a fresh clone from the image, restores it exactly.
8. **Staffing deviation (Q-019):** QA ran on Opus, the same model as the author.

## Handoff
Next: tech-lead — with this report, for the licences-screen sign-off loop:
- rule on F-2 (text direction), F-3 (the width cap, which also affects Settings) and F-4 (heading navigation in the lazy Licenses list);
- rule on the airplane substitute (§8.2 item 4);
- the Cronet jank reading is recorded here.

Then:
- **ux-designer:** O-2 (LS-AC13's "N of N" against TalkBack's "over a hundred"), and F-4's contract question.
- **producer:**
  - dispatch the android-game-dev for the fixes the tech-lead rules;
  - schedule §8.2 item 12 on a device with audible speech;
  - decide whether `Pixel_9_clean` is wiped (system changes 1–4).
- **qa-engineer:** a re-check of F-2, F-3 and, if fixed, F-4 on the next build.

VERDICT: FAIL
