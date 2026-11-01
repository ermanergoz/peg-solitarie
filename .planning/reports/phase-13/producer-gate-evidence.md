# producer report — phase 13 gate evidence (tech-lead HOLD items H-1, H-2)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save, run at HEAD `e15db2f`
Skills loaded: ergoz-soft:standards (role producer)
Ran in parallel with: code-auditor H-3 sweep (static only, no Gradle)
Loops: 0
Lessons applied: none (0 active lessons)

## Why this file exists
The android-game-dev reconcile correctly reported that **no report recorded the gate run**, only a
STATUS.md line. The tech-lead then found the deeper problem: the green `./gradlew check` I had been
citing was run at `9211416`, which **predates the entire convergence build** (`044c33a`..`f30de3d`,
24 files, +1447/−113). `git merge-base --is-ancestor 9211416 71a66e0` confirms it. Both
code-auditor PASS verdicts and the tech-lead's sign-off were conditional on a gate that had never
covered the code under review. This file records the re-run that closes that gap.

## H-1 — full Check command at HEAD
    $ git rev-parse --short HEAD
    e15db2f
    $ ./gradlew check
    BUILD SUCCESSFUL
    exit 0
Covers, per `build-logic/src/main/kotlin/standards.gradle.kts`: detekt, ktlintCheck, the four
literal gates (dpSpLiteralCheck, adIdLiteralCheck, gamesIdLiteralCheck, audioFocusLiteralCheck),
`:domain:depthSweep`, `:app:lint`, and the JVM suites. The on-disk test results the code-auditor
read during its sweep belong to this run: app 926 tests, domain 268, **0 skipped, 0 failures,
0 errors** across 79 + N result files.

Run on an idle host with all three emulators shut down first, because earlier in this session host
load near 80 caused the emulator hang detector to kill two running emulators mid-test.

## H-2 — minified release and R8 keep rules
    $ ADMOB_APP_ID=<Google public test id> ADMOB_REWARDED_AD_UNIT_ID=<test> \
      ADMOB_INTERSTITIAL_AD_UNIT_ID=<test> ./gradlew :app:assembleRelease
    BUILD SUCCESSFUL
    exit 0
    $ ls app/build/outputs/mapping/release/missing_rules.txt
    absent
    $ grep -c 'games.*[Ss]napshot\|SnapshotsClient' app/build/outputs/mapping/release/mapping.txt
    409

R8 wrote **no `missing_rules.txt` at all**, so it needs no new keep rule for the snapshot surface —
including `PlayGamesSnapshotPicker`, the new GMS surface Hilt keeps reachable in release, which was
the tech-lead's specific concern. `app/proguard-rules.pro` is unchanged on this branch
(`git log develop..HEAD -- app/proguard-rules.pro` is empty). **RESEARCH assumption A5 is confirmed
by measurement**, not inferred from Phase 12.

Note on the ad ids: this machine holds no real AdMob ids, so the release build was configured with
Google's public test ad unit ids purely to satisfy the fail-fast `verifyReleaseAdIds` gate at
configuration time. That exercises R8 and the packaging, which is what H-2 asks. It is **not** a
release-candidate artifact and must not be uploaded; the real signed build is phase 14's job with
the founder's own ids.

## Checks
- `./gradlew check` at `e15db2f` — pass (exit 0)
- `./gradlew :app:assembleRelease` at `e15db2f` — pass (exit 0)
- `missing_rules.txt` absent; 0 lines naming `com.google.android.gms.games` — pass
- `git log develop..HEAD -- app/proguard-rules.pro` — pass (empty: no keep rule added)
- `git merge-base --is-ancestor 9211416 71a66e0` — pass (confirms the earlier gate predated the work)
- `git diff 750e6e0..e15db2f --stat -- app domain build-logic gradle` — pass (empty: the commits
  after the gate started are documentation only, so this result stands for the merged tree)

## Open issues / risks
- The gate ran at `e15db2f`; commits after it are documentation only, verified by the diff above.
- No kover coverage gate and no fuzz harness exist in this repo's `check`, contrary to
  rules/kotlin-android.md and rules/testing.md — inherited from the pre-studio phases and raised by
  the tech-lead for phase 14. This gate is therefore green against a Check command that is itself
  weaker than the studio rules require.

## Handoff
Next: producer — merge to develop. H-1, H-2 and H-3 are all green, which is the condition the
tech-lead set for the merge to proceed with no further tech-lead round.
