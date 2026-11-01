# android-game-dev report — phase 13 convergence build (plan 13-12)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (71a66e0..f30de3d, 10 commits, not pushed, not merged)
Skills loaded: ergoz-soft:standards (role android-game-dev; plugin 0.1.1 rules studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing), superpowers:test-driven-development, gitflow. The studio conventional-commit and trailer rules were used where the gitflow skill says otherwise. compose-skill was not loaded because no Compose file was touched.
Ran in parallel with: qa-engineer E1 on two emulators. I did not touch adb or the emulators, and every Gradle run used `--max-workers=2`.
Loops: 0
Lessons applied: none (0 active lessons)

> My harness forbids writing report `.md` files, so `.planning/reports/phase-13/android-game-dev-convergence-build.md` was not written or committed. This message is the full report, and the producer can save it as was done for the tech-lead report.

## Summary
1. **Cover size:**
   - `COVER_PX` is now 384, which is 589,824 raw bytes against the 819,200 cap.
   - `theRawArgbCoverFitsThePlayGamesCoverCeiling` pins the raw size against the cap.
   - The KDoc is corrected, and RESEARCH A1 is marked falsified with the measured GMS log line.
2. **Push guard:**
   - A pass that pulled now pushes only if `ProgressMerge.merge(remote, localExport) != remote`.
   - `land()` no longer arms the push marker. It used to, which would have made the "cloud ahead only" case push after all.
   - Solve pushes, swap pushes and the pre-existing retry marker are unchanged.
   - The four required cases plus a played-time case are tested, and the class KDoc is fixed.
3. **Pull probe:**
   - `load` returns `CloudPull(bytes, probe)`.
   - `CloudPullProbe` holds `changePending`, `conflictsResolved` and `lastModifiedAt`.
   - They are persisted in one edit as `play_games_cloud_last_pull_{opened,change_pending,conflicts_resolved,last_modified_at}`.
   - **`writtenByThisDevice` was dropped.**
     - The 22.0.0 javadoc says only "name of the device that wrote this snapshot, if known … or null".
     - Equality with `Build.MODEL` is not reliable.
     - It also could not tell apart two emulators that share a system image.
     - `lastModifiedAt` carries the usable part of that signal.
   - The store KDoc and RESEARCH Pitfall 4 are corrected.
   - `prefs.py` prints the new keys.
4. **Debug snapshot picker:**
   - `DebugSnapshotPickerEntry` is gated by `BuildConfig.DEBUG`.
   - It launches `PlayGamesSnapshotPicker`: `getSelectSnapshotIntent(title, false, false, 1)`, run inside `platform/`.
   - It waits up to 30 s for the session to report signed in.
   - The command is `adb -s <serial> shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true`.
   - `aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt` proves a non-debug configuration ignores the extra.
5. **Auditor F-1 and F-2:** both are pinned, and each new test was confirmed red against a deliberately broken store before the store was restored.

## Outputs
All paths are under `/Users/ermanergoz/Desktop/PegSolitarie/`.
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveConstants.kt` — `COVER_PX = 384` and the `DEBUG_PICKER_*` constants
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/CoverImageSource.kt` — corrected A1 accounting KDoc
- `app/src/main/res/drawable/cloud_save_cover.xml` — comment wording only
- `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt`
  - push guard (`holdsSomethingTheCloudLacks`)
  - `pullOnce` now returns `CloudProgress?` instead of `Boolean`
  - probe recording, including a `null` probe for a pull that never opened
  - KDoc fixes
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudPull.kt` — new; `CloudPull` and `CloudPullProbe` (Boolean, Int and Long only)
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveStore.kt`, `NoOpCloudSaveStore.kt`, `PlayGamesCloudSaveStore.kt`
  - new `load` signature
  - the probe read is the file-level `probeOf`, placed there to stay under detekt's function-count limit
  - corrected "no API can observe the upload" KDoc
- `app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt`, `CloudSyncStore.kt` — four keys and `recordPullProbe(probe?)` in a single edit
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPicker.kt`, `DebugSnapshotPickerEntry.kt` — new
- `app/src/main/java/com/app/pegsolitaire/MainActivity.kt` — calls the entry from `onCreate` and `onNewIntent`
- Tests, all under `app/src/test/java/com/app/pegsolitaire/`:
  - new: `platform/games/save/CloudPullProbeTest.kt`, `PlayGamesCloudSaveStorePullProbeTest.kt`, `DebugSnapshotPickerEntryTest.kt`, `PlayGamesSnapshotPickerTest.kt`
  - new class `ProgressSubmitterCloudGuardTest`, in the same file as `ProgressSubmitterCloudTest`
  - updated: `CoverImageSourceTest`, `CloudSyncStoreTest`, `FakeCloudSaveStore` and its test, `NoOpCloudSaveStoreTest`, `PlayGamesCloudSaveStoreTest` (adds the F-1 and F-2 tests)
- `.planning/phases/13-cloud-save/13-RESEARCH.md` — A1 falsified, Pitfall 4 corrected
- `.planning/reports/phase-13/tools/prefs.py` — docstring only; the reader needed no change

## Checks
- `./gradlew --max-workers=2 :domain:test :app:testDebugUnitTest` — pass (BUILD SUCCESSFUL, 1194 tests, 0 failures, 0 errors, 0 skipped)
- `./gradlew --max-workers=2 detekt ktlintCheck` — pass
  - The first run failed on TooManyFunctions, two LargeClass findings and two MaxLineLength findings.
  - They were fixed by restructuring the code and tests, with no suppression added.
- T-11-04: `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/ app/src/main/java/com/app/pegsolitaire/data/ | wc -l` — pass (0)
- `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/'` — pass (0)
- `grep -rn "RESOLUTION_POLICY_" app/src/main/java | grep -v RESOLUTION_POLICY_MANUAL` — pass (0)
- `grep -rn modifyBytes app/src/main/java` — pass (0)
- `git diff 9211416 -- gradle/libs.versions.toml app/schemas` — pass (empty)
- Added lines in `git diff 9211416 -- app domain` searched for `@Suppress`, `@Ignore`, `!!` and `baseline` — pass (0)
- `javap` on `classes.jar` from the 22.0.0 AAR — pass (`hasChangePending()`, `getLastModifiedTimestamp()`, `String getDeviceName()` and `getSelectSnapshotIntent(String, boolean, boolean, int)` all present)
- Javadoc text extracted from `SnapshotMetadata.html` — pass (wording quoted above)
- Red states, watched before each implementation:
  - cover size: 1 of 6 tests failed
  - push guard: 2 failed, cases (a) and (c), both "expected 0 but was 1"
  - probe and picker: compile errors, because the types did not exist yet
  - F-1 and F-2: 2 of 56 failed against the broken store
- `python3 .planning/reports/phase-13/tools/prefs.py <hand-encoded preferences_pb>` — pass. It prints all four `play_games_cloud_last_pull_*` keys. The file was synthetic, not read from a device.
- Not run, per the environment constraint or as too heavy: `./gradlew check`, lint, kover, `assembleRelease`, `installDebug`, anything connected, adb.

## Open issues / risks
- **Behaviour change beyond the literal spec:**
  - `land()` no longer sets the cloud push marker. Case (c) cannot hold without this.
  - If a "both ahead" push fails, `recordPushOutcome` still sets the marker, so the retry survives.
- **The push guard compares against the uncapped local export.**
  - For a player whose payload has been trimmed by `encodeWithinCap`, local always exceeds the capped remote.
  - Every launch would then push, which is the old behaviour for that player only.
  - No data is at risk.
- **The debug picker is not verified on a device, and no release build was made.**
  - The gate is proven by a JVM test, not by inspecting R8 output.
  - If the app is already running from the launcher, `am start -n` may only bring it to the front. `onNewIntent` is handled; if nothing appears, force-stop first.
  - The player must be signed in within 30 s of launch.
- **Release-APK detail:** the `DEBUG_PICKER_TITLE` string and `PlayGamesSnapshotPicker` are compiled into release. They are unreachable there, but they are not compiled out.
- **Failed conflict resolution yields no probe:** a pull whose conflict resolution is exhausted records `opened = false`, so its conflict count is lost. The push stage `CONFLICT_UNRESOLVED` still shows that case.
- **Still open:** auditor F-3 to F-6, and the tech-lead's note A(iii) on landing what the push saw.
- **Process deviation:** CLAUDE.md asks for a GSD entry point before edits. I worked directly under the producer's dispatch instead.

## Handoff
Next: code-auditor.
- Review the diff `9211416..f30de3d`.
- Focus on the `load` signature change and the `land()` marker change.

Next: qa-engineer.
- Read the `play_games_cloud_last_pull_*` keys with `prefs.py` after E1.
- Try the picker command on a debug build.

The tech-lead signs off after both.
