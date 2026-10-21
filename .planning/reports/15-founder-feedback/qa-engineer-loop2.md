# qa-engineer report — S11 founder-feedback slice, fix round 1 (loop 2), hands-on
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (cd25522..7cc46bf)
Skills loaded: ergoz-soft:standards (role qa-engineer; rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing — plugin copies read because the 0.5.1 snapshot is older than plugin 0.6.2), android-emulator-skill, android-testing
Ran in parallel with: the code-auditor's re-audit of the same fix round (worktree `PegSolitarie-audit-l2`). It committed `2cfad41` (docs only) to this branch during my session. I read only that commit's message, whose N-1 is a test-only 18e finding; its report I did not read. On the device, the timer is not clipped at 200% (18e below).
Ran on: Opus (claude-opus-5-5) at the agent file's effort xhigh. **Staffing deviation:** the qa-engineer is pinned to fable, and Fable is exhausted. This ran on Opus under the founder's Q-003 answer (`.planning/questions/FOUNDER.md`), which allows an Opus review chain for phase 15. The author also runs on Opus, so independence rests on this being a fresh agent. It saw only `docs/ux-spec.md`, the diff, the build, my own loop-1 report and the code-auditor's loop-1 findings. It did not read `android-game-dev*.md` or `.planning/log/`.
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot carries 0 active lessons)

VERDICT: FAIL

## Summary
The fix round does what it claims. On the device, the timer pill has one width at 00:00, 08:08, 11:11, 59:59, 22:22 and 99:59: a spread of 0.00dp at font 1.0 and 2.0, on the Pixel_9 and in a 360×640dp window. It never clips, and the title stays whole at 360dp. The accessibility tree exposes the clock as one node. The regression spot-check is clean, and the Check is green.
**This emulator still renders the Roboto fallback**, and I found out why. **The FAIL is on F-A, a pre-existing defect: Fredoka and Nunito never load on any device.** `Type.kt` builds `AppTypography` before the two font families exist, so every style carries a null font family. The provider certificate is also corrupt (F-B), which will block the fonts even once F-A is fixed. So no device can yet show 18d "with the shipped font". The Fredoka-width proof rests on the JVM tests, which pass. F-C is a small test gap.
**15e: VOID, sitting not run.** Over 30 minutes of polling, no Gradle or Kotlin process ran, but the 1-minute load never fell below 2.0 (minimum 2.24). On this setup, preconditions 1 (the AVD is Pixel_9, not Pixel_9a) and 7 (both arms kept installed) cannot hold in any case.

## Device and setup
- **Device:** `emulator-5560`, AVD `Pixel_9` (`sdk_gphone64_arm64`, Google Play image, `user` build), API 36, 1080×2424 at 420dpi (411.4×923.4dp). Status bar 54.1dp, gestural navigation bar 24.0dp.
- **Compact window:** `wm size 1080x1920` plus `wm density 480` gives 360×640dp.
- **Build under test:** the installed APK's sha256 is `a2796279…2e99`. That equals `app/build/outputs/apk/debug/app-debug.apk`, built at 05:16 after HEAD's last production commit (05:04) on a clean tree, and its dex contains `TimerDigitsKt`.
- **Founder's data:**
  - Backed up before any change: `run-as tar` of `databases files shared_prefs no_backup`, `backup/app-data-before.tar`, sha256 `8e519172…`.
  - Restored at the end (see Checks).
  - The session edited the Classic resume snapshot's `elapsedMs` and the Challenge ladder, and toggled Show-moves. The restore undoes all three.

## Criteria — verdicts
Widths and heights are device dp, taken from `uiautomator` bounds and cross-checked on screenshot pixels (the pill-colour run on the timer's centre row). The helper scripts are `tools/timer_sweep.py`, which writes `elapsedMs` into the resume snapshot, resumes the game, captures and measures, and `tools/set_elapsed.sh`.

### D-05d, §4.3.1 — the timer after the fixed-width-digit change

**The rendered face on this device is the Roboto fallback, not Fredoka.** F-A explains why. No device can render Fredoka with this build.

| Criterion | Verdict | Device evidence |
|---|---|---|
| **18d** no jitter | **PASS** in the face this build renders, backed by the JVM test in Fredoka. The device reading in Fredoka is not possible anywhere (F-A, F-B). | See the per-window results after this table. The only width change is the legitimate one when minutes reach three digits (a sixth cell). **In Fredoka (JVM):** `GameStatusStripTest.theTimerPillKeepsOneWidthAsItTicksInTheShippedFace` and `TimerDigitsTest` pass on a forced rerun. Mutation M1 (no cells, as the old plain `Text`) turns the 18d test red. F-C is the one mutation that survives. |
| 18e 200% | **PASS** | The digits are whole in a 48.00dp pill at every value, on both windows. Only the title ellipsises: "Peg Sol…" at 360dp, and "Peg Solit…" at 100:00 on the Pixel_9. Its node text stays "Peg Solitaire". Loop-1 O-3 (digits ≈2dp left of centre at 360dp/200%) is **gone**: ink margins are now 14.0 / 14.0dp. |
| 18a title whole at 360×640dp | **PASS** | At font 1.0, "Peg Solitaire" is fully drawn at every value, including 100:00, where the pill is 98.67dp (`strip-sheet-c360-fs1.0.png`). The pill is 40.00dp tall. The Challenge title "Level 2" is whole, and its description reads "Challenge level 2". |
| 18b each bar once | **PASS** (spot-check) | The back chip's top edge is at 58.3dp, 4.2dp below the 54.1dp status bar. The dock slab ends at 883.4dp, 16.0dp above the navigation bar at 899.4dp. |
| 18c board size | **PASS** (spot-check) | The Pixel_9 board region is 379.4dp wide (411.4 − 32). |
| Timer read as one value | **PASS on structure; the live utterance was not captured** | At every value and scale, `uiautomator` (which reads the accessibility tree) shows **one** node for the clock: `TextView` with text "05:12" / "00:00" / "100:00", content-desc "Elapsed time", and **0 child nodes**. The one-character cells are hidden. TalkBack was on, with its "Display speech output" developer option, but its hardware-keyboard navigation (Alt+arrows) on this emulator moves only between actionable nodes. It skipped the title, the timer and the board, and it also skips the message text of a system permission dialog, so this is the tool and not the app. Swipe gestures can't be injected over adb. See O-A for what TalkBack most likely says. |

18d results per window (pill width from the tree; the pixel run agrees within 0.4dp):

| Window, font scale | 00:00, 08:08, 11:11, 59:59, 22:22 | 100:00 |
|---|---|---|
| Pixel_9, 1.0 | **85.71dp at every value**, and 99:59 as well (pixel run 226px = 86.10dp) | 99.43dp, and 999:59 as well |
| Pixel_9, 2.0 | 114.67dp at every value | 134.86dp |
| 360×640dp, 1.0 | 85.00dp at every value | 98.67dp |
| 360×640dp, 2.0 | 114.67dp at every value | 135.00dp |

### Regression pass (F-1–F-3 were test-only)
All spot-checks **PASS**:
- **Home (Pixel_9, founder state):** the title starts at 16.0dp, the gear ends at 395.8dp, and the stage, ribbon, chip and tiles are as in loop 1. Tree order: title → Settings → Stars/Boards/Level → Continue Classic → Classic → Challenge.
- **Dock states:**
  - On a resumed Classic game (`s11l2-game-timer.png`): Undo Available, Hint Available, Show-moves Off (ring and slash), Restart Available.
  - On a fresh Challenge Level 2: Undo Unavailable (faded glyph, no disc).
- **Gameplay flow:**
  - Selecting a peg shows the ring. With Show-moves Off there is no dot; toggling it On shows the check mark and one dot, and the node reads `checked`.
  - The jump lands, and Undo restores the board.
  - Hint shows "Out of free hints" (the saved allowance is spent), and "Watch ad" / "No thanks" are offered.
  - Restart asks "Restart this board?", and I chose "Keep Playing".
  - Back returns to Home with Continue Classic kept.
  - The timer ticked 05:12 → 05:14 after the first move, with no width change.
- **Insets:** as in 18b above. Nothing sits under a system bar.
- **Logs:** the crash buffer is empty for the whole session, which includes the debugger runs. The app log has 0 `AndroidRuntime` / `FATAL` / `Exception` / `ANR` lines.

## FAIL items
**F-A — Fredoka and Nunito never load, on any device (high; pre-existing since 2026-08-26, not introduced by S11).**
- **Where:** `app/src/main/java/com/app/pegsolitaire/ui/theme/Type.kt:23`. `internal val AppTypography = clayTypography()` is declared **above** `FredokaFamily` (`:26`) and `NunitoFamily` (`:29`). Kotlin initialises top-level properties in file order, so `clayTypography()` runs in `TypeKt.<clinit>` while both families are still `null`. `heading()` and `body()` then copy `fontFamily = null` into every role, and every style falls back to `FontFamily.Default` (Roboto).
- **Steps:**
  1. Cold-launch the app on any device.
  2. Look at any title. It renders in Roboto: flat terminals, not Fredoka's rounded ones.
- **Expected:** §6.3, §4.3.1 (D-05d) and the design system call for the titles and the timer in Fredoka Medium (`titleLarge` 22sp, `headlineSmall` 24sp) and the body in Nunito.
- **Actual:** Roboto everywhere, on every device. This includes the founder's own `home-founder.png` and every phase-15 device measurement.
- **Evidence** (`fredoka/root-cause.txt` has the full set):
  - **Bytecode:** `javap -c TypeKt` shows `<clinit>` doing `invokestatic clayTypography` / `putstatic AppTypography` at bytecode index 0–3, and the two `putstatic`s of `FredokaFamily` and `NunitoFamily` only at 35 and 85. `heading()` reads `getstatic FredokaFamily`.
  - **On the device, under jdb** (`fredoka/jdb-step.txt`): Compose's first `FontListFontFamilyTypefaceAdapter.resolve` on Home receives `TypefaceRequest(fontFamily=FontFamily.Default, fontWeight=FontWeight(weight=500), …)` and steps straight into `PlatformTypefacesApi28.createDefault`.
  - Breakpoints on `AsyncFontListLoader.load`, `GoogleFontTypefaceLoader.awaitLoad` and `DefaultFontsContractCompatLoader.requestFont` were never hit in 45–50s of Home (`jdb-probe-3/4/6.txt`).
  - The app process has no Fredoka or Nunito file open.
  - The `FontLog` "query resolved" lines I cited in loop 1 as proof that the provider works are **other clients' requests**: EmojiCompat's Noto emoji, and Google Sans (the app holds `Google_Sans-500/-700` fds).
  - `git show 59c8496:…/Type.kt` has the same order. `develop` 3f2af90 has it. `main` still has plain `Typography()`, so no release has shipped this.
- **Correction to loop 1:** O-1 said "Fredoka never resolves on this emulator". That is wrong. It resolves nowhere, so a physical-phone check of 18d would also have shown Roboto.
- **Why it blocks here:**
  - 18d is defined "on a device with the shipped font", and no device can be one until F-A is fixed.
  - D-05d answers the founder's complaint about type, and the founder has been judging it in the wrong face.
  - Every S11 device reading (15k fold, 15o/15p gaps, 18a/18e fits) was taken in Roboto. Fredoka's metrics differ, so those need a re-walk once the fonts load.
- **Owner:** the developer, through the tech-lead. It is not in this slice's diff, and routing is the tech-lead's call. No test covers it. A JVM assertion that `AppTypography.titleLarge.fontFamily` is `FredokaFamily` is red today.

**F-B — the provider's production certificate is corrupt, so F-A's fix alone will not make the fonts load on a Google Play device (medium, latent, pre-existing).**
- **Where:** `app/src/main/res/values/font_certs.xml`, `com_google_android_gms_fonts_certs_prod`.
- **Expected:** the string is the GMS signing certificate, as published in the Android downloadable-fonts documentation.
- **Actual:**
  - It is 1,464 base64 characters (1,098 bytes), where the real certificate is 1,460 (1,095). `openssl x509` cannot parse it, and its sha256 is `1C8C1E02…`.
  - Diffed against the certificate carried in this device's GMS APK (lineage signer #1, sha256 `f0fd6c5b410f25cb…db83`), it is identical except for **four characters, `wYAs`, inserted at character 795**, inside the RSA modulus.
- **Why it matters:**
  - androidx.core 1.18.0 `FontProvider.getProvider` (`FontProvider.java:186-197`) reads `PackageInfo.signatures` via `GET_SIGNATURES` and compares bytes exactly.
  - GMS signs with key rotation. For a rotated package, that API returns the lineage's first (original) certificate, `f0fd6c5b…`: `dumpsys` shows past signatures `e3ca78d8, 98deb0ca, d1b21c60`, and the real certificate's `Signature.hashCode()` is `e3ca78d8`.
  - The app's corrupted certificate hashes to `fae88b43`, and its other entry is the 2008 "Android" debug certificate. So once F-A is fixed, each request would fail with `FAIL_REASON_WRONG_CERTIFICATES`, and the app would silently stay in Roboto.
- **Evidence:**
  - `fredoka/root-cause.txt` §5.
  - `fredoka/gms-lineage-root-cert.der`, the real certificate extracted from the GMS APK.
  - `apksigner lineage --print-certs` output, quoted in the Checks section.
- **Owner:** the developer, together with F-A.

**F-C — 18d's JVM tests cannot catch cells measured in the wrong face (low, test gap).**
- **Mutation M2:** in a scratch copy of HEAD (not the repo), I measured the cells in `style.copy(fontFamily = FontFamily.Default)` instead of `style` (`TimerDigits.kt:88`). `GameStatusStripTest` stayed 7/7 and `TimerDigitsTest` 1/1, both green (`check/mutation-M2.log`).
- **Why the tests miss it:**
  - `TimerDigitsTest` compares two clocks that share the same measuring code. Under M2 both get the same fallback cells and agree.
  - Robolectric's fallback digits are evidently at least as wide as Fredoka's, because `GameStatusStripTest` stays green too.
- **Why it matters:** on a device, the fallback is Roboto, whose digits are 0.5615em. Fredoka's "0", "2" and "3" are 0.580–0.587em, so they would outgrow their cells and the pill would swing about 1.8dp between 11:11 and 00:00. That is 18d failing on exactly the devices where the fonts load, with the suite green.
- **Mutation M1** (no cells) is caught.
- **Owner:** the developer. Suggestion: assert the cell width against the shipped face's own widest digit, rather than against a second clock built the same way. The code-auditor re-audits.

## Fredoka on this emulator — could it be made to resolve?
**No, not without changing product code.** Nothing on the device side is broken:
- The GMS font provider (`com.google.android.gms.fonts`, GMS 26.08.34) answers queries and serves files to other apps' requests.
- The network is up.
- The provider's lineage includes the certificate the platform reports.

The two blockers are both in the app: F-A (the app never asks for the fonts) and F-B (if it did, it would reject the provider). Correcting either means editing `Type.kt` or `font_certs.xml`, which QA may not do. I did not build a patched APK.

What I changed on the device to diagnose this, and how each change was reverted:
- **`am set-debug-app -w com.erman.pegsolitarie`**, to attach `jdb` at launch over `adb forward tcp:8712 jdwp:<pid>`. There were 10 attach attempts.
  - Reverted with `am clear-debug-app`. The app has since launched normally more than 30 times.
  - `adb forward --remove tcp:8712` after each run. Once, cleaning up a stuck session, I ran `adb forward --remove-all`. No other forward existed then or afterwards (`adb forward --list` is empty).
  - Three attempts hung on attach, leaving the app on its splash. I force-stopped it each time, with no crash (the crash buffer is empty).
- **I pulled the GMS `base.apk`** to a scratch directory, to read its signing lineage. Nothing was written to the device.
- **Founder's app data:** untouched by the diagnosis. The jdb runs only cold-launched the app.

The Fredoka-width proof therefore rests on the JVM tests (F-C notes their limit). **A device check in real Fredoka is owed** once F-A and F-B are fixed, on any Google Play device, the emulator included: 18d at the five readings, and the late-face pickup that `TimerDigitsTest` stands in for. Until then, a physical phone would show Roboto too.

## Test review (step 4)
- **F-1 (auditor):** `DockOnMarkAndSlashTest` now has 7 tests: fill tone, ring and shadow read off pixels for every reachable look, besides the mark and the slash. They are named for the property, and they pass on a forced rerun. The auditor mutation-checks them.
- **F-2:** `GameStatusStripTest.eachSystemBarIsAppliedToTheGameplayScreenOnce` now also reads the back chip's top edge (within 8dp of the status bar, which is 18b's device clause) and where the slab's paint ends (576dp). It passes.
- **F-3:** `HomeHeroRowTest` has 5 tests, now including "nothing but the gear is composed beside or above the title" on two windows. It passes.
- **18d:** `GameStatusStripTest.theTimerPillKeepsOneWidthAsItTicksInTheShippedFace` renders the strip with the Fredoka binary (a test-only OFL fixture) and first proves that the face's set widths really do spread by more than 0.5dp, so it cannot pass on a tabular fallback. M1 turns it red. `TimerDigitsTest` guards the "width remembered in composition" failure mode but not F-C's.
- **18a and 18e** now read every cell's text layout and check that the cells spell the node's text. They pass.
- **Fixture:**
  - `fixtures/fonts/Fredoka-Variable.ttf` is on the unit-test classpath only, with its OFL notice beside it. The repo has no `THIRD_PARTY.md`, so there is no row for it or for the downloadable Fredoka/Nunito. That is for the research-analyst and code-auditor to judge, not a QA finding.
  - The fixture README and `TimerDigitsTest`'s KDoc say the face "never resolves" on the emulator. The premise is F-A, not the emulator (O-D).

## Cold start — criterion 15e (§6.5 second edition)
**Verdict: VOID — the sitting was not run.** The dispatch allows the sitting only on a quiet host, and the host never became quiet:
- **Polling:** 30 minutes, 06:04:42–06:34:46, 61 samples (`coldstart/quiet-poll.txt`).
- **Gradle/Kotlin:** no Gradle or Kotlin java process in any sample, after I stopped my own idle daemons (`gradlew --stop` at 06:00 and 06:03, with no build running anywhere).
- **1-minute load:** minimum **2.24**, median 2.90, maximum 3.77. It never went below 2.0.
- **What held the load up:** macOS `mediaanalysisd` (70% CPU) and Spotlight `mds` (60%), several other agent sessions, and WindowServer (21%, the founder's visible emulator window).
- **Consequence:** there is no null-control floor, no arm medians and no delta, and no numbers are reported. The ~200-level ladder was staged on the device and removed again. The two arm APKs are kept as artifacts.

| # | Precondition (§6.5) | Recorded value | Held? |
|---|---|---|---|
| 1 | AVD `Pixel_9a`, API 36, `:app:installDebug`, animations off | **`Pixel_9`**: same 1080×2424 at 420dpi, 4 cores and 2 GB as this host's `Pixel_9a`, but a different system image (`android-36` `google_apis_playstore` against the 9a's `android-36.1` `google_apis_playstore_ps16k`, a 16 KB-page image). The debug build is installed. Animations were set to 0/0/0 for the prep and restored to 1.0. | **No** |
| 2 | Exactly one emulator | `adb devices` 1, `qemu-system` processes 1 | Yes |
| 3 | No concurrent Gradle build anywhere | none in 61 of 61 samples (the dispatch's `pgrep -fl 'GradleDaemon\|KotlinCompileDaemon\|gradlew'`, java processes only) | Yes |
| 4 | 1-minute load < 2.0 at both ends | min 2.24 over 30 min; `uptime` at 06:35 gave `4.14 3.10 2.94` | **No** |
| 5 | ART normalised (`compile -m speed -f`) on both arms | not reached | — |
| 6 | Component resolved from the device | `cmd package resolve-activity --brief` → `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity` | Yes |
| 7 | Both arms installed for the whole sitting | **cannot hold**: both arms are `applicationId` `com.erman.pegsolitarie` (versionCode 30000 on both per `aapt2 dump badging`), so only one can be installed at a time | **No** |
| 8 | ~200-level Challenge ladder, identical on both arms | 200 rows seeded (`SUM(stars)` 401; Home read "Stars, 401"), in the one data directory both arms would share. The founder's data was restored afterwards. | Yes (staged) |

**How Pixel_9 against Pixel_9a affects validity:**
- 15e's comparison is relative, so a same-AVD A/B on the Pixel_9 would be internally consistent.
- But the AVD is a named precondition. The Pixel_9 runs a different system image with 4 KB rather than 16 KB pages, and renders into a window the founder watches, which costs host CPU.
- So a Pixel_9 sitting is VOID by the letter, and its floor is not comparable with the floors §6.5 cites (57 and 59ms). Loop 1's sitting failed precondition 1 in the same way.

**What is still owed for a decided 15e on this slice.** The producer schedules it:
- **(a)** The founder's Pixel_9 shut down, and `Pixel_9a` booted as the only emulator.
- **(b)** A host with no other agent session, no Gradle run, and macOS indexing finished (1-minute load < 2.0 at both ends).
- **(c)** A ruling on precondition 7 (**E-1** below).
- **(d)** The full protocol:
  - The null control first.
  - A/B/A/B in blocks of five, ≥ 20 kept samples per arm, the first of each block discarded.
  - `am force-stop` + 2s between samples.
  - PASS if `median_B − median_A ≤ max(2 × floor, 100ms)`; VOID if `floor > 150ms`.
- `tools/coldstart.sh` is ready for it: it records the preconditions, the dispatch's daemon check and `uptime` at both ends.

**E-1 — escalation, spec contradiction (to the ux-designer and the tech-lead, through the producer; not counted as a FAIL):**
- **The contradiction:** §6.5 precondition 7 ("both arms installed and kept installed for the whole sitting") cannot be met. The first edition fixed the launch identity to the one `applicationId`, and both arms carry it.
- **Loop 1's workaround:** swap arms per block with `adb install -r -d` followed by `cmd package compile -m speed -f`, applied identically in the null control.
- **Needed:** a ruling that either (i) accepts that adaptation as meeting precondition 7, or (ii) asks for a measurement variant with a distinct `applicationIdSuffix`. Option (ii) is a build change that is the tech-lead's to make.
- **Until then,** every sitting is VOID on precondition 7 alone.

## Checks
- `./gradlew check --offline` on a clean `git archive 7cc46bf` export plus `local.properties` — **pass**. No gsd gate record exists for this slice, so I ran it myself, as Q-003's mitigation asks.
  - `BUILD SUCCESSFUL in 11m 8s`, 98 tasks (35 executed, 63 from cache).
  - detekt, ktlint and lint pass.
  - Tests: app **1,049** and domain **275**, with 0 failures, errors or skips.
  - `:app:testDebugUnitTest` and `:domain:test` were **FROM-CACHE**, keyed to these exact inputs, hence the forced rerun below.
- `./gradlew :app:testDebugUnitTest --offline --rerun --tests *.DockOnMarkAndSlashTest … *.TutorialScreenTest` — **pass**. That covers the 7 classes the fix round touches or whose helpers it changed (`SystemBarInsets`, `RenderedWindow`, `ShippedFace`): 48 tests, 0 failures, executed fresh at 05:57.
- Mutation M1 (`val cell = 0`) in the scratch export — the 18d test is **red** as it should be (`theTimerPillKeepsOneWidthAsItTicksInTheShippedFace FAILED`). The export was restored to HEAD and diffed clean.
- Mutation M2 (cells measured in `FontFamily.Default`) — **green** (F-C). The export was restored afterwards.
- `ANDROID_SERIAL=emulator-5560` device sweeps with `tools/timer_sweep.py` — 26 readings, all listed above.
- `javap -c -p …/TypeKt.class` — `<clinit>` order as quoted in F-A.
- `apksigner lineage --in <GMS base.apk> --print-certs` — signer #1 `f0fd6c5b…db83` (2048-bit), #2 `7ce83c1b…`, #3 `5f239127…` (current).
- `openssl x509 -inform der` on the app's prod certificate — fails with `Could not find certificate`. The dev certificate parses ("Android", 2008, sha256 `1975B2F1…`).
- `adb logcat -d -b crash` — empty.
- **Restore:**
  - `run-as tar -xf` of `backup/app-data-before.tar` after removing the seeded database files.
  - Re-archived as `backup/app-data-after-restore.tar`. `peg_solitaire.db`, `peg_solitaire.db-wal`, `app_prefs.preferences_pb` and the app's shared prefs all have sha256 identical to the backup.
  - Installed APK `a2796279…` (HEAD).
  - Animation scales 1.0/1.0/1.0, font scale 1.0, `wm size`/`density` physical (1080x2424/420), night mode off.
  - `enabled_accessibility_services` null, `accessibility_enabled` 0. TalkBack's "Display speech output" is back off. Its `POST_NOTIFICATIONS` is still `granted=false, USER_SET`: every prompt was dismissed with Back, never answered.
  - No `adb forward`, debug-app cleared.
  - The app is left on Home, showing Stars 3, Continue Classic and "Continue — Level 2" (`final-home.png`).

Excerpt, `fredoka/jdb-step.txt` (Home's first font resolve, main thread):
```
Breakpoint hit: "thread=main", androidx.compose.ui.text.font.FontListFontFamilyTypefaceAdapter.resolve(), line=130 bci=0
 typefaceRequest = "TypefaceRequest(fontFamily=FontFamily.Default, fontWeight=FontWeight(weight=500), fontStyle=Normal, fontSynthesis=All, resourceLoaderCacheKey=null)"
androidx.compose.ui.text.font.PlatformFontFamilyTypefaceAdapter.resolve(), line=33 bci=0
androidx.compose.ui.text.font.PlatformTypefacesApi28.createDefault-FO1MlWM(), line=173 bci=0
```
Excerpt, `javap -c TypeKt` `<clinit>`:
```
 0: invokestatic  #197  // Method clayTypography:()Landroidx/compose/material3/Typography;
 3: putstatic     #11   // Field AppTypography
35: putstatic     #17   // Field FredokaFamily
85: putstatic     #21   // Field NunitoFamily
```
Excerpt, the app's own launch log (`fredoka/logcat-launch-2.txt`). The provider serves only other clients; no Fredoka or Nunito file ever reaches the app:
```
09-29 05:21:05.223   888   911 I ActivityTaskManager: Displayed com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity for user 0: +1s121ms
09-29 05:21:05.337 17854 17884 I FontLog : (REDACTED) Received query %s, URI %s
09-29 05:21:05.347 17854 17883 I FontLog : (REDACTED) Pulling font file for id = %d, cache size = %d
```

## Outputs
- `.planning/reports/15-founder-feedback/qa-engineer-loop2.md` — this report (the only committed file).

Artifacts (git-ignored, all under `.planning/qa/15-founder-feedback/loop2/`):
- **Founder-facing:** `s11l2-game-timer.png` (255 KB) — resumed Classic game on the Pixel_9. The new timer "05:12" sits in its 85.7dp pill, and the dock shows Undo/Hint Available, Show-moves Off and Restart Available. It renders in Roboto because of F-A.
- **Timer (18a/18d/18e):**
  - `timer/{fs1.0,fs2.0,c360-fs1.0,c360-fs2.0}-<ms>.png` plus `.xml` (248–264 KB / 12 KB each). One pair per reading, `<ms>` ∈ 0, 488000, 671000, 3599000, 1342000, 5999000, 6000000, 59999000. Each shows the strip and its tree.
  - `timer/fs1.0-00-00.png`/`.xml` (253/12 KB) — the first manual capture.
  - `timer/tb-312000.png`/`.xml` — 05:12, the TalkBack start state.
  - `timer/sweep-*.txt` (1 KB each) — every measured width.
  - `timer/strip-fs1.0-{0,671000,6000000}.png` (13–14 KB) and `timer/strip-sheet-{fs1.0,fs2.0,c360-fs1.0,c360-fs2.0}.png` (39–53 KB) — the strips at 00:00, 11:11 and 100:00 side by side.
  - `timer/elapsed/e.db*` (40/32/0 KB) — the snapshot-edit working copy.
- **Fredoka root cause (F-A/F-B):**
  - `fredoka/root-cause.txt` (2 KB) — the evidence summary.
  - `fredoka/jdb-step.txt` (9 KB) — the resolve path.
  - `fredoka/jdb-probe-{,2,3,4,5,6}.txt`, `jdb-fontprobe.txt`, `jdb-session.txt` (0.6–65 KB) — the breakpoint runs. Probes 2 and 5 and one `jdb-session` run hung on attach.
  - `fredoka/gms-lineage-root-cert.der` (1 KB) — the real GMS certificate.
  - `fredoka/logcat-launch-{1,2}.txt` (172/165 KB) — the launch logs with `FontLog`.
  - `fredoka/00-home-launch.png`, `01-after-jdb.png`, `02-after-probe.png` (191 KB each) — the Roboto Home.
- **TalkBack:**
  - `talkback/00-talkback-on.png`, `01-after-dismiss.png` (114/113 KB) — TalkBack on, and its notification prompt dismissed with Back, not answered.
  - `talkback/tab-{1..4}-{a,b}.png` plus `tab-sheet.png` — Tab reaches only the actionable nodes.
  - `talkback/alt-1-{a,b}.png`, `prev-{1..7}-{a,b,c}.png` plus `prev-sheet.png`, `next-{1..3}-{a,b,c}.png` plus `next-sheet-{top,bottom}.png` — Alt+arrow traversal goes back → Undo → Hint → Show-moves → Restart, skipping the title, timer and board.
  - `talkback/home-next-*.png` plus sheet, `dlg-prev-{1..4}.png` plus `dlg-sheet.png`, `rc-{1,2}.png` plus `rc-sheet.png` — the same skipping on Home and in a system dialog.
  - Sizes: 30–263 KB each.
- **Regression:**
  - `regress/01-home.png`/`.xml`, `02-game-resumed.png`/`.xml`, `03-select-showmoves-off.png`, `04-select-showmoves-on.png`, `05-after-jump.png`, `06-after-undo.png`, `07a-hint-tap.png`, `07b-hint-after.png`, `08-restart-dialog.png`, `09-home-after-back.png`, `10-challenge.png`/`.xml` (115–260 KB / 12 KB).
  - `sheet-flow-{1,2}.png` (44/111 KB).
  - `11-home-idle.png`, `12-home-idle-t0/t1.png` and `12-home-idle-diff.png` (190/2 KB) — O-B.
- **Check:** `check/gradlew-check.log` (8 KB), `check/fix-tests-rerun.log` (3 KB), `check/mutation-M1.log` (4 KB), `check/mutation-M2.log` (3 KB).
- **Cold start:**
  - `coldstart/quiet-poll.txt` (4 KB) — every poll sample.
  - `coldstart/preconditions.txt` (1 KB) — the eight recorded preconditions.
  - `coldstart/armA-3f2af90.apk` (19.7 MB) and `coldstart/armB-7cc46bf.apk` (20.4 MB) — the staged arms.
  - `coldstart/seed.db*` (48/32/0 KB) — the 200-level ladder.
  - `coldstart/founder/databases/*` (0–92 KB) — the founder DB copy the seed was built from.
- **Backup:** `backup/app-data-before.tar` (1.1 MB) — the founder's app data. `backup/app-data-after-restore.tar` (1.1 MB) — the restore check. `final-home.png` (192 KB) — the app left on Home.
- **Logs:** `logcat-crash-buffer.txt` (0 B), `logcat-app-session.txt` (117 KB).
- **Helper scripts (never committed):**
  - `tools/timer_sweep.py`, `tools/set_elapsed.sh`, `tools/ui.py`, `tools/taptext.sh` — driving and measuring the device.
  - `tools/jdb_probe.py`, `tools/jdb_step.py`, `tools/jdb_fontprobe.sh` — the debugger runs.
  - `tools/wait_quiet.sh`, `tools/coldstart.sh` — §6.5.
  - `tools/restore.sh` — the device reset.
  - Copied from loop 1 and unused: `tools/{px,pill,match,contrast}.py`.

## Open issues / risks
- **F-A and F-B:** the product's whole type system has never rendered, and this is unreleased: `main` predates it. Fixing it restyles every screen. Every phase-15 device measurement was taken in Roboto (15k, 15o, 15p, 18a, 18e), and several are fit-sensitive: the 360dp title row at 36sp and the 360×640 fold. They need a re-walk in the real faces, and so does 18d.
- **F-C:** a green suite would not catch one plausible regression of 18d.
- **O-A (for the ux-designer; likely, not captured live):**
  - The clock's node carries content-desc "Elapsed time" **and** text "05:12". TalkBack speaks a node's content description in place of its text, so it most probably says "Elapsed time" and **not the time**.
  - This is unchanged by this fix: the old `Text` had the same two properties.
  - Criterion 18 asks only that the timer "is still announced as the elapsed time". Whether the value should be spoken, for example as "Elapsed time, 5 minutes 12 seconds", is the ux-designer's call. A device with gesture input can confirm the utterance.
- **O-B (for the tech-lead; pre-existing, designed):**
  - Idle Home renders continuously: 305 frames in 5s (≈61 fps), from `HomeBlobs`' infinite drift (§10: "the two existing `HomeBlobs` remain the only drifting elements").
  - The emulator's CPU goes from about 17% with the launcher in front to about 220% on Home.
  - This is by design, but it is a battery cost for `compose-performance-audit` before release, and it loads the host during §6.5 sittings.
- **O-C:** loop-1 O-3 (off-centre digits at 200%) is resolved.
- **O-D (cosmetic):** the `TimerDigitsTest` KDoc and `fixtures/fonts/README.md` blame the emulator for the face "never resolving". The cause is F-A.
- **15e is still owed on this slice** (see what is owed above), and E-1 blocks any decided sitting until it is ruled on.

## Handoff
Next: tech-lead — the merge decision on S11, with **VERDICT: FAIL** on F-A (pre-existing, high). The fix round itself passes: 18d holds on the device in the face the build renders and in Fredoka on the JVM, and the regression pass and Check are green. Through the producer:
- The developer fixes **F-A** (initialisation order in `Type.kt`, plus a test that is red today) and **F-B** (the prod certificate in `font_certs.xml`), and optionally **F-C**. Whether that happens on this slice or on its own bugfix branch before the release is the tech-lead's call.
- Then QA re-walks S11 on the device **in Fredoka/Nunito**: 18d, 15k, 15o, 15p, 18a and 18e.
- The ux-designer takes O-A, and the tech-lead takes O-B.
- The producer schedules the decided 15e sitting: Pixel_9a, a quiet host, the Pixel_9 down, and E-1 ruled on first.
