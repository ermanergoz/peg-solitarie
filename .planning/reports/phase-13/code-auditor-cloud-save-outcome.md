# code-auditor report — phase 13 cloud save outcome (stage + status code)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (0437a2b..c25dd7f under review; HEAD e5a5ec1)
Skills loaded: ergoz-soft:standards (role code-auditor) → rules studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md (plugin copies, v0.1.1, newer than the repo snapshot `.claude/rules/VERSION` 0.1.0). code-reviewer / security-reviewer / gsd-code-review applied as review checklists; compose-skill and compose-performance-audit not invoked — the diff contains no Compose UI (no composable, no `.dp`/`.sp`, no recomposition surface).
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
**Verdict: PASS.** The diff implements section B of the tech-lead report as specified: an SDK-free `CloudSaveOutcome(stage, statusCode)`, per-await classification in `PlayGamesCloudSaveStore`, and one DataStore edit that moves the pending marker and writes both diagnostic keys.
Privacy holds by construction, not by restraint: the outcome type has no `String` slot, only `ApiException.statusCode` is copied, and the T-11-04 grep gate is still 0 in `platform/games/` and `data/`.
CLOUD-05 / D-03 are untouched — no reader, no `Flow`, no UI for either new key; pending still means "not committed".
`c25dd7f` did **not** weaken the test it edited: the original filter could never pass (proof in F-1), so it removed a compiler-artifact false failure, not an assertion.
Four items are recorded for planning (F-2..F-4 are the ones asked about; all pre-existing, none a regression from this diff), plus three informational notes.

## Outputs
- `.planning/reports/phase-13/code-auditor-cloud-save-outcome.md` — this report (left uncommitted)

## Requirements coverage

| Req | Where | Verdict |
|-----|-------|---------|
| 1 — `save` returns SDK-free `CloudSaveOutcome`; 7-stage enum; nullable `Int` from `ApiException.statusCode` | `CloudSaveOutcome.kt:218-254`, `CloudSaveStore.kt:316-322`, `PlayGamesCloudSaveStore.kt:207-208` | met — enum names and order are exactly those specified, pinned by `CloudSaveOutcomeTest.kt:33-45` |
| 1 — `pushIfNeeded` writes stage + status in the SAME edit as the pending flag | `ProgressSubmitter.kt:508` → `CloudSyncStore.kt:118-128` | met — proven, not asserted by reading: `CloudSyncStoreTest.kt:116-124` counts exactly 1 `updateData` through `CountingDataStore` |
| 1 — pending semantics unchanged (pending = not committed) | `CloudSyncStore.kt:120`, `ProgressSubmitter.kt:489` (guard returns before any write) | met — `ProgressSubmitterCloudTest.kt` `aPushTheGuardDeclinedRecordsNothing` pins "declined ≠ attempted"; `setPushPending(true)` survives for its two non-attempt callers (`ProgressSubmitter.kt:359`, `:427`), so no dead code |
| 2 — no message / player id / description / payload recorded | `PlayGamesCloudSaveStore.kt:198-208`, `StageFailure` at `:389-391` (no message, no cause) | met — grep for `.message`/`localizedMessage`/`stackTrace`/`toString()`/`displayName`/`playerId` over added main-source lines returns only a KDoc mention |
| 2 — no logging in `platform/games`, `data/cloud`, `data/` | plan gate `13-11-PLAN.md:90` | met — 0 hits (command in Checks) |
| 2 — no `^import com.google.android.gms` outside `platform/` | `ApiException` imported at `PlayGamesCloudSaveStore.kt:369` only | met — 0 hits outside `platform/` in `src/main`; `PlatformBoundaryTest` + `PlatformBoundaryGateTest` green (3+3) |
| 2 — outcome type carries no GMS types | `CloudSaveOutcome.kt:218-221` | met — `javap -p` on the compiled class: fields are `CloudSaveStage` and `java.lang.Integer`, nothing else |
| 3 — CLOUD-05 / D-03: nothing user-visible, silent degradation | `grep -rn CLOUD_LAST_PUSH app/src` — writers and tests only, no `Flow`, no UI, no reader in `src/main` | met |
| 4 — no magic numbers/strings; scoped constants | key literals in `PlayGamesPreferences.kt:107,118`; test codes as named consts (`ProgressSubmitterCloudTest.kt:831`, `PlayGamesCloudSaveStoreTest.kt:1305`) | met — machine-checked: detekt runs with `MagicNumber` enabled and `ignoreFailures=false` (`build-logic/src/main/kotlin/standards.gradle.kts:9,27`) and reports 0 |
| 4 — no new `@Suppress` / baseline / `@Ignore` / disabled test | added lines grep | met — **APPROVED SUPPRESSION: none required; the diff adds no suppression, no baseline entry, no `@Ignore`, no skipped test (`skipped="0"` in all 10 result XMLs), and no `!!` in production code. No repo contains a detekt/ktlint baseline file.** |
| 4 — detekt + ktlint clean | fresh `--rerun-tasks` run | met |
| THIRD_PARTY.md row per new dependency | `git diff 659edf6..HEAD -- gradle/ '*.gradle.kts'` is empty | n/a — no dependency added (`ApiException` comes from the already-pinned `play-services-games-v2` 22.0.0); the repo has no `THIRD_PARTY.md`, which is correct while no third-party row is owed |

## Findings

### F-1 — `c25dd7f` did not weaken the shape test (Info — judgement requested)
`app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudSaveOutcomeTest.kt:46-50` — the filter went from `filterNot { it.isSynthetic }` to `filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }`.

Evidence that the original assertion could never have passed, so nothing green was loosened:

    $ javap -v -p .../classes/com/app/pegsolitaire/platform/games/save/CloudSaveOutcome.class
      public static final int $stable;
        descriptor: I
        flags: (0x0019) ACC_PUBLIC, ACC_STATIC, ACC_FINAL

`$stable` (added by `@StabilityInferred`, visible in the same constant pool) carries no `ACC_SYNTHETIC`, so the old filter collected `[CloudSaveStage, Integer, int]` against `containsExactly(CloudSaveStage::class.java, Int::class.javaObjectType)` — a guaranteed failure for a reason that has nothing to do with the property under test. The commit message states exactly this and the exclusion is justified in the source comment. The guarantee that matters is intact: add a `String` field to the data class and the test still fails.

Residual, worth a follow-up but not a blocker: statics are now entirely out of scope, so a future class-level leak holder (`companion object { var lastError: String? = null }`, which compiles to a non-synthetic static) would no longer be caught. The narrower exclusion `filterNot { it.isSynthetic || it.name == "\$stable" }` keeps statics in scope and costs nothing.

### F-2 — `discardQuietly` swallows a failed `discardAndClose`, and `load` latches "closed" regardless (Low — planning)
`app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt:375-380`, with `load`'s unconditional `closed = true` at `:113-114`.

Real defect? Partly, and not the part the code chose. Swallowing is correct where `discardQuietly` runs from a `finally` (`:179`, `:119`): a throw there would replace the failure already propagating, and re-attempting a terminal call on a handle the SDK may consider closed fails its own Task (the class's own Pitfall 2). What is real is that a *failed* discard leaves the handle open, and this class's KDoc states that a leaked handle makes every later `open` fail for the rest of the process. **So yes — it can cause a subsequent `open` to be refused**, which is precisely candidate A2 in `tech-lead-save-path-observability.md`. The swallow does not create the leak; it makes it undiagnosable.

This diff improves the situation rather than worsening it: the next push now reports `OPEN_FAILED` plus a status code instead of a bare `false`. Follow-up to plan: let `discardQuietly` report whether the close succeeded and surface a failed close in the diagnostic (a distinct stage, or a third key), rather than making cleanup throw. Severity Low; not a blocker of this diff.

### F-3 — the same `runCatching` swallows `CancellationException` (Low — planning)
`app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt:379`.

Real defect, in one narrow window. From a `finally` the swallow is harmless — the original `CancellationException` keeps propagating after the block. On `load`'s happy path, however, `discardQuietly` is called outside any `finally` (`:113`): a cancellation arriving inside that await is absorbed, `closed = true` is set, and `load` **returns bytes normally** instead of propagating. The coroutine is already cancelled, so the caller throws at its next suspension point, but in between a cancelled pass keeps working, and `ProgressSubmitter`'s deliberate "rethrow cancellation" path never sees it. It is also now the only await in the class that does not follow the rule this diff made explicit at `:202-205` and `:181-182` (WR-04).

Cannot cause an `open` to be refused. Pre-existing, on lines this diff did not touch. The fix is not a rethrow from cleanup (that would mask the primary failure) but `coroutineContext.ensureActive()` after the `try` in `load`/`save`, leaving the cleanup quiet. Severity Low.

### F-4 — the give-up conflict path closes one of the two conflict snapshots (Low — planning)
`app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt:316-321` — `discardQuietly(client, unresolved.snapshot)` closes the server-side handle; `unresolved.conflictingSnapshot` (read at `:299`) is never given a terminal call. Each loop iteration also abandons the previous iteration's pair, relying on `resolveConflict` superseding them.

Real defect: **unproven but plausible**, and the consequence if it is real is exactly Pitfall 1 — a handle open for the life of the process, refusing every later `open`. The SDK does not document that `getConflictingSnapshot()` needs no close, and this code's own standard elsewhere is "every opened handle reaches a terminal call". It cannot explain the device evidence under investigation: the path needs an existing server snapshot, and no commit has ever succeeded on those devices. Severity Low. Follow-up: either discard both handles on the give-up path or record the SDK ownership claim in the KDoc, and pin whichever is chosen with a `PlayGamesCloudSaveStoreTest` case (the existing conflict-exhaustion test asserts only the outcome and `commitAndClose`, so today either choice passes).

### F-5 — `FAILED_OTHER` covers two different causes (Info)
`ProgressSubmitter.kt:173-176` maps a throwing seam to `FAILED_OTHER` with no status code; `PlayGamesCloudSaveStore.kt:183-184` maps a non-await SDK failure to `FAILED_OTHER` **with** a status code when one exists (`anApiExceptionOutsideTheThreeAwaitsStillSurfacesItsStatusCode` pins it). Distinguishable in practice by the presence of the code, and documented in both KDocs. No action; noted so nobody reads a bare `FAILED_OTHER` as "the store threw".

### F-6 — the persisted stage name is not shrinker-proof (Info)
`CloudSyncStore.kt:121` persists `outcome.stage.name`. Release uses `proguard-android-optimize.txt` with no enum keep (`app/build.gradle.kts:296-298`, `app/proguard-rules.pro` deliberately holds no speculative keeps), so on a minified build the stored string may be an obfuscated field name. Harmless today, because the value is only ever read through `run-as` on a debuggable build — as `PlayGamesPreferences.kt:96-99` says — and `CloudSaveOutcomeTest`'s name pinning runs unminified so it cannot catch this. Only if a release-device dump is ever needed does a `-keepclassmembers enum …CloudSaveStage { *; }` become necessary.

### F-7 — `pending = true` beside `stage = COMMITTED` is a legitimate state (Info)
`ProgressSubmitter.kt:359` and `:427` set the marker without attempting a push, so the stage key keeps describing the last *attempted* push. The KDoc at `PlayGamesPreferences.kt:82` is precise about this and `aPushTheGuardDeclinedRecordsNothing` pins it, but the QA brief for the next device pass should say so explicitly, or the pair will read as a contradiction on the device.

### Things the diff got right, recorded because they are what makes it a PASS
- Privacy argued from the **type**, not from implementation restraint (`CloudSaveOutcome.kt:198-204`), and asserted by reflection rather than by reading the source.
- `StageFailure` carries neither message nor cause (`:389-391`), with the reason written down — the one place where the classification refactor could have reintroduced the leak.
- Cancellation rethrown before any classification in both new code paths (`:203`, `:182`).
- The one-edit invariant is proven by a counting fake, not by inspection.
- The seam's `null`-on-read contract is unchanged and explicitly re-tested (`aRefusedOpenOnTheReadPathIsStillJustNull`).

## Checks
- `./gradlew detekt ktlintCheck --rerun-tasks` — pass (`BUILD SUCCESSFUL in 8s`, `25 actionable tasks: 25 executed`; `app/build/reports/detekt/detekt.xml` and `domain/build/reports/detekt/detekt.xml` each contain 0 `<error` entries; `find . -name "*baseline*.xml" -not -path "*/build/*"` → empty, so nothing was silenced)
- `./gradlew :app:testDebugUnitTest --tests "*CloudSave*" --tests "*ProgressSubmitter*"` — pass (`BUILD SUCCESSFUL`; 109 tests over 8 classes, all `skipped="0" failures="0" errors="0"`: ProgressSubmitterCloudTest 30, ProgressSubmitterTest 20, PlayGamesCloudSaveStoreTest 33, FakeCloudSaveStoreTest 9, CloudSaveDescriptionFormatterTest 5, CloudSaveConstantsTest 4, CloudSaveOutcomeTest 4, NoOpCloudSaveStoreTest 4)
- `./gradlew :app:testDebugUnitTest --tests "*CloudSyncStoreTest*"` — pass (14 tests, 0 skipped/failed) — added by me: neither given filter matches `CloudSyncStoreTest`, which is where the same-edit and key-name assertions live
- `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*"` — pass (PlatformBoundaryTest 3, PlatformBoundaryGateTest 3, 0 failures) — the machine check behind requirement 2's boundary claim
- `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/ app/src/main/java/com/app/pegsolitaire/data/ | wc -l` — pass (0; the exact gate from `13-11-PLAN.md:90`)
- `grep -rn '^import com.google.android.gms' app/src/main/java/com/app/pegsolitaire/ | grep -v '/platform/'` — pass (no output)
- `git diff 659edf6..HEAD -- app domain | grep -E '^\+.*(@Suppress|@Ignore|baseline|Disabled)'` — pass (no output); `… | grep -E '^\+.*!!'` over `app/src/main` — pass (no output)
- `javap -v -p …/CloudSaveOutcome.class` — evidence for F-1 (`$stable` is `ACC_PUBLIC, ACC_STATIC, ACC_FINAL`, not synthetic) and for requirement 2 (instance fields are `CloudSaveStage` + `java.lang.Integer` only)
- `git diff 659edf6..HEAD -- gradle/ '*.gradle.kts'` — pass (empty; no dependency change, no THIRD_PARTY row owed)
- `./gradlew check` — **not run**, per the coordinator's instruction (the producer runs it; two emulators in use on a loaded host). This verdict therefore rests on detekt, ktlint, the boundary tests and the 123 unit tests above, not on `lint`, `koverVerify` or the fuzz test.

## Open issues / risks
- Coverage of the new code is not verified here: `koverVerify` is part of `./gradlew check`, which I did not run. Every added branch does have a named test, but the 90% gate is the producer's evidence to quote, not mine.
- F-2, F-3 and F-4 are three separate ways a Play Games snapshot handle can outlive its pass. None is introduced by this diff and none is proven to fire on the devices under investigation, but together they are the strongest remaining app-side explanation (A2) if the founder confirms the Play Console toggle was already on. They want one planning task, not three.
- The diagnostic is only as good as its reader. `.planning/reports/phase-13/tools/prefs.py` (commit `759f040`, outside the reviewed range) is the only thing that decodes these keys; its new `signed()` helper is correct for a negative int32 in protobuf varint form, but the tool is untested QA scaffolding and a misread there would send the next device pass after the wrong stage.
- `docs/tech-design.md` does not exist in this repo, so section B of the tech-lead report is the only written specification this diff can be audited against. That is sufficient here — it is precise — but it means a future reader has no design doc to check the seam's contract against.

## Handoff
Next: tech-lead — PASS; sign-off and merge to `develop` after the qa-engineer verdict. Carry F-2/F-3/F-4 into the phase-13 plan set as one snapshot-handle-lifetime task (they share a root cause and a test file), and F-1's narrower filter plus F-6/F-7 as notes on that task. The QA brief for the next device pass needs F-7 stated, or `pending=true` beside `stage=COMMITTED` will read as a contradiction.
