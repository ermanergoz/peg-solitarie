# qa-engineer report — phase 14 R-1 gate scope (review mode)
Date: 2026-10-18
Branch: feature/phase-14-gate-scope (committed by the producer). Reviewed: `develop` 3f2af90 ... `c944496` (f2b5de9, 86fc4a4, fb838f8 + two report commits)
Skills loaded: ergoz-soft:standards (role: qa-engineer) — plugin copies (v1.0.0, newer than the repo snapshot 0.5.1) of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md; repo `.claude/rules/lessons.md`; android-emulator-skill; android-testing
Ran in parallel with: phase-15 tech-lead sign-off, candy-backdrop design directions (ux-designer)
Ran on: claude-opus-5-5 at effort high. Staffing deviation: the qa-engineer agent pins fable at high; it ran on Opus because of the founder's answer to Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published, `PegSolitarie-p15/.planning/questions/FOUNDER.md`)
Fan-out: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
Staffing deviation: I ran on Opus, not my pinned Fable, under Q-019. The developer also wrote on Opus, so I ran every check myself and paired each one with a negative control.
- The Check is green: `./gradlew --no-daemon check` exited 0 in 11 min 19 s with every `PLAY_UPLOAD_*` variable unset. The build-logic suite passes: 54 tests, 0 skipped.
- Each of the three literal gates goes red on a literal planted in `src/main`, `src/release` or `src/debug`, and stays green when the same literals sit in `src/test` or `src/testDebug`. Every failure message names its scope. The tripwire fires on a new source set in `:app` and in `:domain`.
- Only packaging needs credentials. `check`, `assembleDebug` and the release analysis tasks pass without them. `assembleRelease` and `bundleRelease` fail at configuration in about 5 s, and the abbreviated `:app:aR` fails in `verifyReleaseSigning` before packaging. No release artifact ever reached disk. CI runs `check` word for word on pull requests to develop and main. The debug build installs and launches on the ergoz_studio emulator.
- One gap, not a FAIL: the brief asks for pull requests to `release/*`, but tech-design §9.7 and the R-1′(c) ruling specify only develop and main, which is what the branch does. The tech-lead should rule on it.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r1/.planning/reports/phase-14/qa-engineer-gate-scope.md` — this report (uncommitted; the producer commits it)
- Artifacts, all under `/Users/ermanergoz/Desktop/PegSolitarie-r1/.planning/qa/phase-14-gate-scope/` (git-ignored, never committed):

| Path | Size | What it shows |
|---|---|---|
| `check.log` | 8.9 KB | full Check run, `BUILD SUCCESSFUL in 11m 19s`, `exit=0 wall=680s` |
| `build-logic-test.log` | 1.0 KB | `-p build-logic test --rerun-tasks`, 12 tasks executed, success |
| `probe-gates.sh` | 1.5 KB | my QA helper: plants a Kotlin file and a res XML holding `ca-app-pub-`, `CgkI…`, `requestAudioFocus` in one `app/src/<set>`, runs the three gates with `--continue`, deletes the files, `git checkout -- .`, records `git status --short` |
| `probe-main.log`, `probe-release.log`, `probe-debug.log` | 4.8–4.9 KB each | all three gates FAILED; offenders listed with the scope sentence |
| `probe-test.log`, `probe-testDebug.log` | 1.8 KB each | the same literals; all three gates executed and passed (`BUILD SUCCESSFUL`) |
| `tripwire-app.log`, `tripwire-domain.log`, `tripwire-control.log` | 1.3 / 1.3 / 0.4 KB | `src/staging` (app) and `src/qaflavour` (domain) fail configuration; control with neither passes |
| `assembleDebug.log` | 4.3 KB | `:app:assembleDebug` with credentials unset, success |
| `release-nocreds-appassembleRelease.log`, `release-nocreds-appbundleRelease.log` | 2.1 KB each | configuration-time failure naming all four missing variables |
| `release-nocreds-abbrev-aR.log` | 8.4 KB | `:app:aR` gets past the configuration predicate (auditor's F-6) and then fails in `verifyReleaseSigning` before any package task |
| `analysis-nocreds-dryrun.log` | 2.9 KB | `-m :app:lintRelease :app:detektRelease` with no credentials, success |
| `probe-manifest-info.log` | 1.7 KB | an observation outside the ruled scope (Open issues): a `ca-app-pub-` literal in `src/debug/AndroidManifest.xml` passes `adIdLiteralCheck` |
| `install-attempt-pixel9.log` | 1.2 KB | Pixel_9 install refused: `Requested internal only, but not enough space` |
| `01-launch.png` | 124 KB | first launch on ergoz_studio: the how-to-play tutorial |
| `02-home.png` | 113 KB | the Play Games sign-in prompt after "Got it" |
| `03-home.png` | 72 KB | home: Classic (5 boards), Challenge (Continue — Level 1) |
| `04-challenge-level1.png` | 94 KB | Challenge level 1 board, toolbar (Undo, Hint, Show possible moves, Restart) |
| `05-after-move.png` | 106 KB | one legal jump made; the board updated; the "No moves left — undo or restart" banner shows the game logic responding |
| `logcat.txt` | 49 KB | the app's PID log, last 500 lines, taken while it ran |

## Checks
- `cd PegSolitarie-r1 && env -u PLAY_UPLOAD_KEYSTORE -u PLAY_UPLOAD_KEY_ALIAS -u PLAY_UPLOAD_KEY_PASSWORD -u PLAY_UPLOAD_STORE_PASSWORD ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon check` — **pass** (`BUILD SUCCESSFUL in 11m 19s`, 98 actionable tasks: 10 executed, 88 up-to-date. All 8 literal gates (`:app` and `:domain` × ad-ID, games-ID, audio-focus, dp/sp), `:app:lintDebug`/`:app:lint` and `:domain:depthSweep` executed. Unit tests were up-to-date against the same inputs. No other Gradle build was running in the worktree.)
- `./gradlew --no-daemon -p build-logic test --rerun-tasks` — **pass** (19 s; 54 tests, 0 skipped, 0 failures, 0 errors across the result XMLs)
- `probe-gates.sh main | release | debug` — **pass (red as required)**: each run reports `:app:adIdLiteralCheck FAILED`, `:app:gamesIdLiteralCheck FAILED`, `:app:audioFocusLiteralCheck FAILED`, `gradle exit=1`. Offenders are listed in both `kotlin/` and `res/`. `status-after: []` each time.
- `probe-gates.sh test | testDebug` — **pass (negative control green)**: all three gate tasks executed, `BUILD SUCCESSFUL`, `gradle exit=0`, `status-after: []`
- Scope in the messages — **pass**: each of the three failure texts ends `Scanned: src/main, src/release, src/debug (java, kotlin, res). Test source sets are excluded on purpose (see SourceSetPolicy).` The old "forbidden in src/main" wording is gone. It now reads "forbidden in shipped source".
- `mkdir -p app/src/staging/java && ./gradlew --no-daemon -q :app:help` — **pass (tripwire fired)**: exit 1, `Unclassified source set(s) in :app: src/staging`, naming both lists and both decisions
- `mkdir -p domain/src/qaflavour/kotlin && ./gradlew --no-daemon -q :domain:help` — **pass (tripwire fired)**: exit 1, `Unclassified source set(s) in :domain: src/qaflavour`
- `./gradlew --no-daemon -q :app:help` (control, nothing planted) — **pass** (exit 0)
- `./gradlew --no-daemon :app:assembleDebug` (all `PLAY_UPLOAD_*` unset, `env | grep -c PLAY_UPLOAD` → 0) — **pass** (`BUILD SUCCESSFUL in 32s`; only `apk/debug/app-debug.apk` exists, no release directory)
- `./gradlew --no-daemon :app:assembleRelease` (unset) — **pass (fails fast)**: exit 1 in 5 s at configuration, `Release build requires the Play upload signing credentials. Missing: PLAY_UPLOAD_KEYSTORE … PLAY_UPLOAD_KEY_ALIAS … PLAY_UPLOAD_KEY_PASSWORD … PLAY_UPLOAD_STORE_PASSWORD`
- `./gradlew --no-daemon :app:bundleRelease` (unset) — **pass (fails fast)**: exit 1 in 5 s, same message
- `./gradlew --no-daemon :app:aR` (unset; the abbreviation the configuration predicate does not match) — **pass (backstop held)**: exit 1 in 20 s, `:app:verifyReleaseAdIds`, `:app:verifyReleaseGamesIds`, then `:app:verifyReleaseSigning FAILED`. No package task ran.
- `find app/build/outputs -path '*elease*' \( -name '*.apk' -o -name '*.aab' \)` after the three release attempts — **pass** (empty; nothing was ever signed with the debug key)
- `./gradlew --no-daemon -m :app:lintRelease :app:detektRelease` (unset) — **pass** (`BUILD SUCCESSFUL in 5s`; analysis tasks demand no credentials)
- `.github/workflows/ci.yml` read in full — **pass, with the scope gap below**: `on.pull_request.branches: [develop, main]` and `on.push.branches: [develop, main]`. The steps are `./gradlew -p build-logic test --no-daemon --stacktrace`, then `./gradlew check assembleDebug --no-daemon --stacktrace`. The Check command runs word for word. Pull requests to `release/*` are not triggered.
- New test names read as sentences; no `@Ignore`, `@Suppress` or skipped test in the diff — **pass** (19 test names reviewed in `ReleaseInvocationTest`/`SourceSetPolicyTest`)
- `adb -s emulator-5554 install -r app-debug.apk` on **Pixel_9** — **fail (environment, not the build)**: `Requested internal only, but not enough space` (/data 97 % full, 217 MB free). Nothing was changed on that device.
- `adb -s emulator-5554 install app-debug.apk` on **ergoz_studio** (Android 16, /data 21 %) — **pass** (`Success`)
- `app_launcher.py --serial emulator-5554 --launch com.erman.pegsolitarie` — **pass** (pid 12292; `topResumedActivity=… com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`). The walk went tutorial → Got it → sign-in prompt → Not now → home → Challenge → level 1 → one jump, and the move registered.
- `adb -s emulator-5554 logcat -d -b crash` — **pass** (empty; no crash, no ANR)
- Tree hygiene — **pass**: `git status --short` was empty after every probe and at the end. `app/src` is back to `debug main release test testDebug` and `domain/src` to `main test`. HEAD is still `c944496`. I committed nothing.

Logcat excerpt (`logcat.txt`, app PID 12292; no crash; the `E` lines are emulator codec noise and the hidden-API denials that `uiautomator dump` triggers):
```
10-18 00:18:03.016 12292 12322 E MediaCodec: Media Quality Service not found.
10-18 00:18:03.016 12292 12353 E an.pegsolitarie: Failed to query component interface for required system resources: 6
10-18 00:18:03.482 12292 12337 W UserMessagingPlatform: Metrics reporting URL is not set yet.
10-18 00:18:03.530 12292 12292 W UserMessagingPlatform: Error making request.
10-18 00:18:10.017 12292 12292 E an.pegsolitarie: hiddenapi: Accessing hidden method Landroid/view/accessibility/AccessibilityNodeInfo;->getSelection()… using linking: denied
10-18 00:18:39.185 12292 12292 W WindowOnBackDispatcher: sendCancelIfRunning: isInProgress=false callback=androidx.navigationevent.OnBackInvokedInput$createOnBackAnimationCallback$1@5aa7b4d
```
Error tags in the 500-line dump: 8 × `MediaCodec`, 11 × the process tag (codec resource queries + hidden-API denials). No `AndroidRuntime`, no `FATAL`.

## Device and system changes (left as found)
- emulator-5554 = **Pixel_9a**, /data 95 %. I found it running, shut it down cleanly with `emulator_manage.py --shutdown` (`adb emu kill`), and later rebooted it. No wipe, no data clear, no delete. It is **running again now**, /data still 95 % (315 MB free). I installed nothing on it.
- **Pixel_9**: I booted it, and one install attempt failed for lack of space, so the package already there (`com.erman.pegsolitarie` 3.0.0 debug, `lastUpdateTime=2026-09-29 15:17:57`) was not touched. Then I shut it down. Its night mode was off and stayed off.
- **ergoz_studio**: I booted it, installed `com.erman.pegsolitarie` (debug), ran the smoke test and uninstalled the app (`Success`; 0 peg packages left). Night mode stayed off. Then I shut it down.
- **Left running: Pixel_9a only (emulator-5554)**, which is the state I found.
- No settings changed, no `settings put`, no `cmd *_manager`, no radio toggles, and no force-stop or clear on any package I did not install. `./gradlew --stop` ran after my builds, so I left no Gradle daemon behind.

## Open issues / risks
1. **[Medium, needs a ruling, not a FAIL] CI and `release/*`.** The dispatch asks for "pull requests to develop, release/* and main". `ci.yml` triggers on pull requests (and pushes) to `develop` and `main` only. That matches tech-design §9.7 ("every push and PR to `develop` and `main`") and the R-1′(c) ruling ("ci.yml must run the gates"), so the branch meets its requirement as written. A PR that targets `release/<X.Y.Z>` (for example a bugfix into the release branch) would run no CI. The tech-lead should decide whether §9.7 should add `release/**` to `pull_request.branches` (one line, likely in the R-2 branch that already edits gates), or whether the brief's wording was loose. Also, `pull_request` triggers already existed on `develop`. Commit `fb838f8` changed the steps, not the triggers.
2. **[Info, outside R-1's ruled scope, pre-existing] Manifests and assets are not scanned.** The ruling fixes the roots as `{java,kotlin,res}`, and the branch does exactly that. A `ca-app-pub-` literal in a source-set `AndroidManifest.xml` passes `adIdLiteralCheck` (`probe-manifest-info.log`, exit 0). `src/main/AndroidManifest.xml` is where the AdMob `APPLICATION_ID` meta-data lives, and today it reads `@string/admob_app_id`, so nothing is wrong in the tree. This is a hole for the tech-lead to weigh, possibly alongside F-9's durable shape. `src/<set>/assets` is likewise unscanned.
3. Auditor F-2 (`KNOWN_SOURCE_SET_NAMES` pre-authorising `androidTest*`/`screenshotTest*`/`testRelease`) is still present on this branch, and `theAgpCreatedTestSourceSetsAreKnownSoTheyDoNotFalselyTripTheWire` pins it. As the dispatch directs, it is ruled into the R-2 branch (tech-design §9.3) and I did not fail R-1 on it. The R-2 change must update that test.
4. Pixel_9 and Pixel_9a both have /data at 95–97 % and refuse installs. ergoz_studio (21 %) is the usable QA device until someone frees space on the others. That is the producer's call, since both are shared, and Pixel_9a is the cold-start comparand.

## Handoff
Next: tech-lead — QA PASS for `feature/phase-14-gate-scope` at `c944496`, for your sign-off. Two items need your decision: Open issue 1 (whether CI should trigger on `release/**` pull requests, a §9.7 wording question) and Open issue 2 (whether manifests and assets join the literal-gate roots). Neither blocks this merge. Producer: commit this report. The artifacts directory can go after the Done line.
