# qa-engineer report — colour-vision (device pass, brief §7.3)
Date: 2026-10-24
Branch: feature/colour-vision (report only; head a5ab809, code 0f1f58f)
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing. logging, puzzle-design and play-store are identical to the snapshot already loaded. I also read `.claude/rules/lessons.md`. Then android-emulator-skill and android-testing.
Ran in parallel with: home-chip-arrival (code-auditor loop 2), licences-screen (QA re-run, Pixel_9_clean), talkback-board (QA, ergoz_studio)
Ran on: claude-opus-5-5 at effort high. This deviates from my pin, fable at high: the Fable limit is reached, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The code under test was also written on Opus.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
**The device pass did not run.** Its APK will not install on the only device the dispatch allows.
- The `Pixel_9a` AVD booted cold on `emulator-5614`.
- Its `/data` partition is 95–96 % full: 287–314 MB free of 6 GB.
- Android refused the install twice: "Requested internal only, but not enough space".
- That is my role file's escalation trigger: the artifact will not install after two attempts.

Not one CV-AC item has a device artifact. So nothing is passed, and no product defect was found either. Both debug APKs are built and hashed, ready for a re-run. I changed nothing on the AVD and shut it down.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/qa-engineer.md`: this report (not committed; the producer commits).

Artifacts, all under `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/qa/colour-vision/` (git-ignored, never committed):

| Path | Size | What it is |
|---|---|---|
| `cv-0f1f58f-debug.apk` | 21,334,265 B | The branch build, `:app:assembleDebug` at a5ab809. Its code equals 0f1f58f: `git diff --stat 0f1f58f HEAD -- . ':!.planning' ':!docs'` is empty. sha256 `5e694cbdfbe6266c92bc1b38e90e7c1a7145068c52111a89a94ead8d0b8881db` |
| `base-d21ad07-debug.apk` | 21,300,585 B | The base build, `:app:assembleDebug` at `d21ad07` = `git merge-base develop feature/colour-vision`, for the off-palette comparison. It was built in a detached scratchpad worktree, which I then removed. sha256 `35effc511fe52202e5fef9362591741d52157a30d4ad26171f3cecbaedb64a3d` |
| `build-cv.log` | 1,905 B | The Build command `./gradlew :app:assembleRelease`, run together with `:app:assembleDebug`. It fails with "Release build requires the Play upload signing credentials. Missing: PLAY_UPLOAD_KEYSTORE …" |
| `build-cv-debug.log`, `build-base.log` | 4,031 B, 3,668 B | The two debug builds. Each ends `BUILD SUCCESSFUL`, `EXIT=0` |
| `device-state-before.txt`, `device-state-after.txt` | 938 B, 482 B | The AVD's state at boot and before shutdown: fingerprint, `wm size`/`wm density`, night mode, font scale, animator scales, daltonizer, accessibility services, the installed app, `df /data`. The two are identical in every setting read |
| `prior/prior-base.apk`, `prior/prior-data.tar` | 20,977,221 B, 5,269,504 B | Read-only backup of the app already installed on the AVD: a debuggable 3.0.0 from 2026-10-17, sha256 `3137f53e…a3dc`, with its `databases/`, `files/`, `shared_prefs/` and `no_backup/` taken through `run-as … tar`. It stays here for whichever remedy below the producer picks. Nothing was restored, because nothing was changed |
| `tools/robot.py`, `ui.py`, `deadend.py`, `prefs.py` | 4,179 / 2,611 / 2,268 / 2,141 B | Session helper copies of the earlier QA tools, for the re-run. Unused this session |
| `emulator.log` | 9,063 B | The emulator's boot log |

The two install failures, quoted from the session (attempts at 22:14 and 22:16 +03):

    $ adb -s emulator-5614 install -r -d --no-incremental base-d21ad07-debug.apk
    adb: failed to install …/base-d21ad07-debug.apk:
    Exception occurred while executing 'install':
    android.os.ParcelableException: java.io.IOException: Requested internal only, but not enough space
    	at com.android.server.pm.PackageInstallerService.createSession(PackageInstallerService.java:752)
    $ adb -s emulator-5614 shell df -h /data
    /dev/block/dm-54 5.8G 5.3G  287M  96% /data/user/0
    $ adb -s emulator-5614 shell dumpsys diskstats
    Data-Free: 293500K / 6082144K total = 4% free
    $ adb -s emulator-5614 install -r -d base-d21ad07-debug.apk      # second attempt
    android.os.ParcelableException: java.io.IOException: Requested internal only, but not enough space
    $ adb -s emulator-5614 shell df /data
    /dev/block/dm-54   6082144 5629236    310696  95% /data/user/0

## Per-item table
| Item | Asked by | Result | Evidence |
|---|---|---|---|
| CV-AC01: the row, TalkBack, silent toggle, 200 % in light and dark | brief §7.3 | **not run** | blocked: no install |
| CV-AC02: persistence (fresh off, force-stop and relaunch, the key `colorblind_palette_on`, "Use their cloud progress") | §7.3 | **not run** | blocked |
| CV-AC03: live switch with a game saved; the dispatch's live switch with a game open | §7.3, dispatch | **not run** | blocked |
| CV-AC05: the off-palette comparison against the base `d21ad07`. The unearned stars are excepted for form (CV-13a) and colour (CV-13b) | dispatch | **not run** | both APKs built and hashed (above); blocked at install |
| CV-AC09: animator scale 0 and 1 | §7.3 | **not run** | blocked |
| CV-AC10: TalkBack traversal on gameplay, the tutorial, Results and the catalogue; 21a–21g in both palettes | §7.3 | **not run** | blocked |
| CV-AC11: simulated colour spaces × light/dark × the fixed scenario | §7.3, dispatch | **not run** | blocked |
| CV-AC11, graded item: standard palette, normal vision; earned filled and unearned hollow, on Results and a catalogue card, light and dark | dispatch, spec §12.5 | **not run** | blocked |
| CV-AC12: offline matrices on plain captures, with the 16 dp pip note | §7.3, dispatch, spec §12.5 | **not run** | blocked |
| CV-AC13: eight dashes at Wiegleb-45, 360×640, monochromacy | §7.3 | **not run** | blocked |
| CV-AC14: system colour correction, if the UI reaches it | §7.3 | **not run** | blocked |
| CV-13a / CV-13b on the device | spec §12, §13 | **not run** | blocked |
| 15e sitting | — | out of scope | the dispatch: a later bench session |

## Bugs
None filed. No product behaviour was observed.

**Blocker B-1 (environment, not product): the Pixel_9a AVD cannot install the artifact.**
- **Steps:**
  1. `emulator -avd Pixel_9a -port 5614 -no-snapshot-load -no-snapshot-save -no-boot-anim`, then wait for `sys.boot_completed=1`.
  2. `adb -s emulator-5614 install -r -d --no-incremental base-d21ad07-debug.apk`, repeated once.
- **Expected:** `Success`.
- **Actual:** "Requested internal only, but not enough space". `/data` has 287–314 MB free of 6,082,144 K. The APK is about 21 MB.
- **Cause, inferred:** Android's low-storage reserve is about 5 % of `/data`, roughly 304 MB here, so free space minus the APK falls under it. I read no threshold setting; both `sys_storage_threshold_*` keys read `null`, which means the defaults.
- **What fills the AVD:** the third-party packages are `com.erman.pegsolitarie` (the 2026-10-17 debug build), `com.erman.percentagecalculator`, `com.p2pchat.android_p2p_chat`, `com.google.android.play.games` and three Google packages. I could not see per-app sizes: `dumpsys diskstats` prints no app sizes, and there is no root.
- **Evidence:** the quoted lines above, `device-state-before.txt` and `device-state-after.txt`.

## Checks
- `./gradlew :app:assembleDebug :app:assembleRelease --max-workers=4` in `PegSolitarie-cv`: **fail** (exit 1, 20 s). The release half needs the four `PLAY_UPLOAD_*` secrets, which this shell does not have.
- `./gradlew :app:assembleDebug --max-workers=4` in `PegSolitarie-cv`: **pass** ("BUILD SUCCESSFUL in 37s", EXIT=0).
- `./gradlew :app:assembleDebug --max-workers=4` in a detached worktree at `d21ad07` (scratchpad, removed after the build with `git worktree remove --force` and `prune`): **pass** ("BUILD SUCCESSFUL in 22s", EXIT=0).
- `shasum -a 256` of both APKs: recorded above.
- The Check: **not run**. Device QA cites the Check, and the code-auditor's forced Check at 216c6c3 (code = 0f1f58f) is green: 1,265 + 296 tests, `CHECK-EXIT=0` (`code-auditor.md`).
- Boot: `emulator -avd Pixel_9a -port 5614 -no-snapshot-load -no-snapshot-save -no-boot-anim`: **pass**. `sys.boot_completed=1`, and `adb emu avd name` printed `Pixel_9a`.
- `adb -s emulator-5614 install -r -d [--no-incremental] base-d21ad07-debug.apk` ×2: **fail**, as quoted. Nothing else was installed, and no other serial was touched.
- AVD restored: **pass**. The settings read before and after are identical: night mode `yes`, font scale 1.0, animator scales 0.0, daltonizer `null`, no accessibility service, `wm size` 1080x2424, density 420, the 2026-10-17 build untouched at the same `codePath` and `lastUpdateTime`.
  - My only device actions: one `wm`/`settings get`/`dumpsys`/`df` read set; `pm path` and `adb pull` of the installed APK; one read-only `run-as … tar cf -`; and `cmd package resolve-activity --brief`, a read.
  - No setting was changed, so nothing needed resetting.
- Shutdown: `emulator_manage.py --shutdown emulator-5614`: **pass**. The emulator is gone from `adb devices` and the process list. `multiinstance.lock` was there before the boot as well.

## Open issues / risks
1. **Escalation, for the producer to route.** The dispatch allows only the Pixel_9a, and the Pixel_9a cannot take an install. Options I see, none taken:
   - **(a) Grow the Pixel_9a's data partition** (`disk.dataPartition.size` in its `config.ini`, so the emulator resizes `userdata` at the next boot).
     - This is a persistent AVD change, the founder's or producer's to approve.
     - `disk.dataPartition.size` is one of the keys 15e precondition 1 allows to differ from `Pixel_9a_bench`, so the bench comparison stays valid.
     - I have not verified that this emulator build resizes an existing image.
   - **(b) Free space on the Pixel_9a** by removing what another product or session left there.
     - Not mine to remove: PROHIBITED bars clearing or uninstalling a package I did not install.
     - The stale 2026-10-17 pegsolitarie build alone frees only about 25–50 MB, which is a thin margin. Swapping between the base and branch builds with `install -r` needs room for a second APK at every swap.
     - Its APK and data are backed up under `prior/` should the producer authorise that route.
   - **(c) Use another AVD,** such as the `Pixel_9` of brief §7.3, with the deviation recorded. The dispatch forbids it today.
2. **For the re-run, nothing needs rebuilding** unless the branch moves. Both APKs, their hashes and the helper tools are in place.
   - The comparison base I chose is `d21ad07`, the branch's merge base and the base CV-AC05 names, not develop's head.
   - Develop's head `f7dd1fe` has since merged `bugfix/reduced-motion-live-gate`: 22 app files, `GameScreen`, `TutorialScreen`, `SettingsScreen` and `Clay.kt` among them. Comparing against it would mix that branch's changes into the identity check.
3. **The Build command cannot run on this host without the upload secrets** (`build-cv.log`). The device APKs are debug builds, which `run-as` also needs (brief §9's key check).
4. **The review-model deviation stands.** The code and this session both ran on Opus. No verdict here rests on judgement alone: there is none.

## Handoff
Next: producer —
1. Route blocker B-1 and choose (a), (b) or (c). The founder decides if it is (a) or (b) on a shared AVD. Then re-dispatch the qa-engineer with the same scope; the APKs under `.planning/qa/colour-vision/` can be reused if the branch is still at 0f1f58f's code.
2. Do not count this report as QA's device pass. The tech-lead's sign-off and the 15e sitting still wait on a PASS.

VERDICT: FAIL (blocked: the device pass did not run; no CV-AC item was tested and no product defect was found)
