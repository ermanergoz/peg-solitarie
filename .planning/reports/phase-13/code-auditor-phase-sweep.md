# code-auditor report — phase 13 sweep (H-3)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (`develop`...`750e6e0`; no branch switch, nothing edited, nothing committed)
Skills loaded: ergoz-soft:standards (role: code-auditor) → plugin copies v0.5.2 of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`, `kotlin-android.md`, `compose.md`, `testing.md` (the repo snapshot `.claude/rules/VERSION` is 0.1.0 and therefore stale); `.claude/rules/lessons.md`. `code-reviewer` / `security-reviewer` / `gsd-code-review` applied as review method. `compose-skill` / `compose-performance-audit` not invoked: the only Compose file in scope (`ProfileSwapDialog.kt`) is a stateless leaf and UI style is out of scope by the task.
Ran in parallel with: the producer's H-1 `./gradlew check` and H-2 `:app:assembleRelease` (no Gradle task run by me, by instruction)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)


Verdict: PASS — nothing in the never-reviewed range blocks the merge; five suppressions approved.

> Saved by the producer from the code-auditor's final message; the agent's harness did not let it write the file.

## Summary
- **Verdict: PASS.** Five suppressions approved (three in `src/main`, two in tests the tech-lead's `src/main`-scoped grep did not see). No suppression silences a correctness rule; no baseline file exists in the tree; 0 skipped tests in the on-disk results.
- The four scoped risks come back clean at the structural level: **no logging** in `platform/games`, `data/cloud` or `data/`; **no GMS type** outside `platform/`; **no Android import** in `domain/`; the D-14 wipe has exactly one reachable call chain and every dismissal is the safe answer; the Room import is a single transaction that rolls back whole.
- Five findings, all on the **decode/merge boundary**, none blocking. The one that matters is F-1: a payload the codec correctly *refuses* to read is then *overwritten* by the merge callback, so `decode`'s own "a failed load leaves the cloud untouched" is false on the write path. It cannot fire today and becomes live the day `SCHEMA_VERSION` moves.
- F-2/F-3 are the cardinality half of the trust boundary: every field's *value* is validated, the payload's *row count* and Classic `levelId` text are not.
- I distinguish throughout: F-1, F-3 and F-5 are **defects** (latent, bounded); F-2 and F-4 are **risks I could not disprove** within the stated threat model.

## Scope
`git diff develop...HEAD -- app domain` (66 files, +10,111/−108 over those two paths), **excluding** what `code-auditor-cloud-save-outcome.md` (`0437a2b..c25dd7f`), `code-auditor-getconflict-fix.md` (`f1eb206..12514cb`) and `code-auditor-convergence-build.md` (`c2c1006..f30de3d`) already recorded — I read all three first and have not re-raised any of their F/L/I items. Scoped by the tech-lead to PII/privacy, the `platform/` boundary, the destructive paths and the decode boundary. Style, naming, test-quality and `.planning/` are out of scope and were not reviewed.

## Approved suppressions

| # | Site | Rule | Decision |
|---|---|---|---|
| 1 | `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:322` | `TooManyFunctions` | **APPROVED** |
| 2 | `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:151` | `TooManyFunctions`, `LongParameterList` | **APPROVED** |
| 3 | `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt:44` | `TooManyFunctions` | **APPROVED** |
| 4 | `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt:852` | `LongParameterList` | **APPROVED** |
| 5 | `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:1896` | `LongParameterList` | **APPROVED** |

**APPROVED SUPPRESSION: app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:322 — `TooManyFunctions` on `DefaultProgressRepository`.** The count is eleven interface overrides the `ProgressRepository` contract fixes plus four private helpers (`recordResultInTransaction`, `importInTransaction`, `List<ClassicStanding>.boards`, two entity mappers) that each state one rule — the merge, the transaction boundary, the entity shape — exactly once. Necessary: the only way to get under the threshold is a second class writing the same tables, which destroys the single-source-of-truth property the class exists to hold. Smaller alternative considered and rejected: moving the mappers to file scope would satisfy detekt while making the class no simpler; the helpers are not the problem, the fixed interface is. One rule, class-scoped, reason written in the KDoc above it ("Size suppression"). Same shape as the pre-existing `PlayGamesService.kt:124`.

**APPROVED SUPPRESSION: app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:151 — `TooManyFunctions`, `LongParameterList` on `ProgressSubmitter`.** Eight constructor collaborators, one per store the pass sequences, and the pass's five steps as small private functions. The ordering *is* the specification (step 1 must precede step 2 or the new account's data merges into the old account's store irrecoverably — `ProgressSubmitter.kt:368-375`), and splitting the class would put half that ordering somewhere else. A smaller alternative does exist and I record it rather than require it: the four cloud collaborators (`cloudSyncStore`, `cloudProfileStore`, `cloudSaveStore`, `challengeProgress`) could become one `@Inject`-constructed parameter object, which would retire `LongParameterList` on its own. That is a refactor with its own Hilt cost and no behavioural gain; the suppression is honest and narrow, so I approve it as written. Reason written in the KDoc ("Size suppressions").

**APPROVED SUPPRESSION: app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt:44 — `TooManyFunctions` on the codec object.** This is the one I scrutinised hardest, because it sits on the trust boundary. The function count is driven by **one private validator per untrusted field** (`toProgressModeOrNull`, `isWellFormed`, `hasLegalChallengeLevelId`, `toLevelBest`, `toValidatedBests`, `toEntryOrNull`) — merging any two of them would fold two rejection rules into one predicate, which is exactly the shape that loses a rule in a later edit. The suppression therefore *protects* the property under review rather than hiding a violation, and the reason on the line says so. Precedent: the identical suppression and identical wording on `SnapshotMapper.kt:89`, the resume blob's decoder. Smaller alternative — a separate `LevelBestDtoValidation` object — would split the decode rules across two files for the sole benefit of a count; rejected.

**APPROVED SUPPRESSION: app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt:852 and ResultsViewModelTest.kt:1896 — `LongParameterList`.** Both sit on a test factory whose parameters are the defaulted collaborators of the class under test; the count is inherited from production and cannot be reduced without hiding which fake a test swapped. Test-scope, no production effect, reason on the line. (Recorded here because they are new on this branch and no range covered them; `ResultsViewModelTest.kt:90`'s `LargeClass` is pre-existing and not mine to rule on.)

Nothing else required approval: the branch adds no `@Ignore`, no disabled or skipped test (`skipped="0"` across all 79 `app` result XMLs and all `domain` ones), no detekt/lint baseline (none exists in the repo), and no `!!` in production code — the only two `!!` added are assertion-site unwraps in `CloudProgressCodecTest`.

## Findings

### F-1 — a payload refused on read is overwritten on write (Medium — defect, latent; does not block the merge)
`app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt:167` (`schemaVersion > SCHEMA_VERSION → null`), `:196` (`decodeOrEmpty`), `:191` (`mergeBytes`), consumed at `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt:209-218` and in `resolveOnce`.

`decode` refuses a newer-schema payload for a stated reason — "A failed load leaves the cloud untouched; a partial one corrupts it (D-07 / D-11)" (`:157-158`) — and that reasoning is right. But the refusal only governs the *read*. `mergeBytes` reaches the same payload through `decodeOrEmpty`, which maps **both** "corrupt bytes" and "decodable but newer" to `CloudProgress.EMPTY`; `commitOrDiscard` then computes `finalBytes = mergeRemote(existing, bytes)` and `writeBytes` a **full overwrite**. So the sequence "this build refuses to read the newer payload, then deletes it" is reachable on the push path and on the conflict-resolution path inside a *pull*. The one claim the codec makes about its own safety is false in exactly the direction that loses data.

Not reachable today: `SCHEMA_VERSION` is 1, `SCHEMA_VERSION_UNKNOWN` is 0, cloud save ships for the first time in this version, so no payload in the world declares 2. It goes live the first time a schema bump ships and two devices are on different versions — which is the normal state of a staged rollout. **Not a merge blocker; must be recorded against D-07 before any bump.** Fix: have `mergeBytes` distinguish the two null causes and, for "decodable but newer", return the remote bytes verbatim (or abort the push) rather than the identity — plus a test, because today both behaviours pass (`anUndecodableSideContributesTheIdentityRatherThanDiscardingTheOther` exercises only the garbage case, `CloudProgressCodecTest.kt:207`).

### F-2 — every field's value is validated; the payload's cardinality and Classic ids are not (Medium — risk, within the declared threat model)
`CloudProgressCodec.kt:252-260` (`toValidatedBests`, unbounded `forEach`), `:288-292` (`isWellFormed`: Classic `levelId` only has to be non-blank), against `LevelResultDao.kt:59-70` and `CloudProgressCodec.kt:126`.

The Challenge side is bounded — `hasLegalChallengeLevelId` (`:301-302`) pins ids to `ChallengeLevelBounds.MIN..MAX`. The Classic side has no closed set and no row-count limit, so a payload may carry arbitrarily many rows of arbitrary text up to the server's 3 MB. The codec's own KDoc sets the threat model at `:18-20` ("fully attacker-influenceable by anyone with access to the player's account", ASVS V5), so this is in scope rather than hypothetical. Three consequences, in increasing order of how much I care:

1. Rows persist: `importInTransaction` (`ProgressRepository.kt:471-506`) upserts them, nothing but the D-14 wipe ever deletes a row, and `exportProgress` (`:421-445`) re-publishes them on every push. Monotone by design, so there is no path back.
2. `LevelResultDao.totals` counts `SUM(stars)`, `COUNT(*)` and the three-star count over **all** rows with no catalog filter (`:64-69`, deliberately — the D-03 star tiers count everywhere). Fabricated rows therefore inflate the leaderboard values and achievement unlocks the pass submits to Play Games.
3. If such rows carry the export past `TRIM_TARGET_BYTES`, `encodeWithinCap` returns over-cap by explicit design — the "honest failure" at `:110-116` — and every subsequent push ends `OVER_CAP`. That is a permanently wedged cloud save for that player, produced by input.

I could not disprove reachability, and I could not demonstrate it either (it needs a writer on the player's account). Recommendation, in the order I would spend the effort: a **row-count cap at decode** (cheap, no forward-compat cost, kills (1) and (3)); optionally let the trim drop Classic rows whose id names no shipped board. Note the argument on the code's side, which is why I do not ask for id validation: rejecting unknown Classic ids would drop a future sixth board's rows from a newer writer, the exact forward-compat break D-07 forbids.

### F-3 — `dropCount`'s arithmetic overflows `Int` on an oversized payload (Low — defect)
`CloudProgressCodec.kt:141-145`: `maxOf(1, challengeRows * (encodedSize - maxBytes) / encodedSize + 1)`, all `Int`.

With the full 10,000-row ladder and an overshoot above ~210 KB, `challengeRows * overshoot` exceeds `Int.MAX_VALUE` and the product wraps. The `maxOf(1, …)` floor means the loop still terminates — this is **not** the spin the KDoc at `:133-139` guards against, and I checked that specifically — but the property the KDoc sells ("converges in a handful of encodes instead of thousands") silently degrades to one row per pass, i.e. up to 10,000 re-encodes of a multi-megabyte payload on the sync pass. Only reachable together with F-2. Fix is one character class: widen the multiplication to `Long`.

### F-4 — the pull's decode is the one untrusted parse outside every guard (Low — risk, defence in depth)
`app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:411`, against `:407` and the scope at `di/DataModule.kt:89`.

`attemptOrNull` wraps the *store load* (`:407`); the decode on the next line is bare. `submitAll` runs inside a `scope.launch` on `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, and a `SupervisorJob` isolates a failed child without swallowing its exception — the repo says so itself at `ChallengeProgress.kt:167-169`. So `decode`'s totality is the only thing between a hostile payload and a process crash.

For malformed JSON the argument is sound and I verified it rather than took it: `kotlinx.serialization.SerializationException` extends `IllegalArgumentException`, so the single catch at `:206-211` genuinely covers truncated, empty, non-UTF-8 and structurally wrong input, and `malformedTruncatedEmptyAndNonUtf8BytesAllDecodeToNull` pins it. What the catch does not cover is a non-`IllegalArgumentException` throwable — a library defect, or `StackOverflowError`/`OutOfMemoryError` on a deliberately pathological document. I could not disprove that without executing one, so I record it as a risk, not a defect. Structural fix costs one line: move the decode inside the existing `attemptOrNull`, or catch `Throwable` there rethrowing `CancellationException`, which turns "total by library contract" into "total by construction".

### F-5 — the D-14 wipe commits before the replacement is known to be obtainable (Low — defect, recoverable)
`ProgressSubmitter.kt:330-333` (`resetAllProgress()` + `resetAwaiting()` inside `applySwapAnswer`), `ChallengeProgress.kt:145`, against `pullOnce` at `:405-415`.

The wipe runs first and unconditionally; the pull that is supposed to refill the device runs afterwards and is gated by `pulled`, a once-per-process flag. If that single pull fails — no connectivity at that moment, a refused `open`, the conflict loop exhausted, all of them observed in this phase — the device stays empty for the **rest of the process**, with no second attempt until the next launch.

What makes this Low rather than High, and I checked it specifically because it is the obvious way this design could destroy an account: the *cloud* is never at risk. `commitOrDiscard` reads the server copy and merges it into whatever it is told to write (`PlayGamesCloudSaveStore.kt:209-210`), so the post-wipe empty payload cannot erase the account's save; the next successful pull restores everything. The damage is confined to one session of a blank-looking device, on a path the player explicitly chose. Recommendation, no code change asked for: the second-account UAT the tech-lead already requires before v1.1 should include "tap *Use their cloud progress* with the network off" as a named case.

### I-1 — an untrusted level id reaches a throwable message (Info)
`domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMerge.kt:121`: `error("Key $key belongs to neither side of the merge")` interpolates a `LevelKey` whose `levelId` may be payload text. Unreachable by construction (the key is drawn from the union of both key sets, and the KDoc says so), and nothing in the phase logs, so no PII path exists. Recorded only because it is the single place payload text can enter a message in a phase whose rule is that it never does.

### I-2 — the dialog names what it deletes, not what may fail to arrive (Info — game-designer/ux, not mine)
`app/src/main/res/values/strings.xml`, `profile_swap_body` / `profile_swap_body_unknown_account`. The copy enumerates all four deleted categories, as D-14 requires, and the emphasis inversion in `ProfileSwapDialog.kt:174-221` (gradient on the safe answer, plain `TextButton` on the wipe) is the right call and worth recording as such. What the copy never says is that the arriving account may hold **no** cloud save at all, which is a legitimate and tested outcome (`usingTheOtherAccountsCloudProgressAgainstAnEmptyCloudLeavesACleanSlate`). Whether that warrants a sentence is a design call.

### What the sweep confirms positively
- **PII:** zero logging calls anywhere in `app/src/main` or `domain/src/main`. The player id exists in exactly two places — the app-private `app_prefs` DataStore under `play_games_merged_player_id` (`CloudProfileStore.kt:88-92`) and an in-memory `StateFlow` — and the display name only reaches the dialog that shows it to its owner. The diagnostic keys (`PlayGamesPreferences.kt:108-156`) are a stage name, an `Int` status, two booleans, an `Int` count and a timestamp: no free text, no identifier. The snapshot description (`CloudSaveDescriptionFormatter.kt:61-69`) is three of the player's own numbers from `strings.xml`, length-capped.
- **Boundary:** no `com.google.android.gms` import outside `platform/`, no such type in any binding (`PlatformModule.kt:123-146` binds the seam, not the impl), no Android/AndroidX/GMS import in `domain/src/main`.
- **Destructive paths:** `resetAllProgress` has exactly one call chain — `ProfileSwapDialog` `TextButton` → `HomeViewModel.onUseCloudProgress` (`:341-342`) → `onProfileSwapResolved(false)` → `applySwapAnswer` — and that function returns without wiping if the session is not `SignedIn` (`:329`). Every dismissal, including system back and a scrim tap, is wired to `onKeepLocal` at a single assignment (`ProfileSwapDialog.kt:101`). The import is one `withTransaction` with one bulk upsert (`ProgressRepository.kt:471-506`) inside `storageFailSafe`, so a failure rolls the whole merge back and reports `UNCHANGED`; the wipe is likewise one transaction over the three DAOs (`:511-517`).
- **Decode:** no unchecked cast anywhere in the new cloud code (the only two casts are `as?`), no index arithmetic over the payload, stars clamped, ladder clamped, `bestTimeMs`'s absorbing `0L` gated at the boundary with the merge identity `Long.MAX_VALUE` on the other side, and a bad row degrades to a dropped row rather than a refused payload, as CLOUD-05 requires.

## Checks
- `git diff develop...HEAD -- app domain | awk '/^diff --git/{f=$3} /^\+.*@Suppress/{print f": "$0}'` — **pass** (5 hits, all listed above; the three in `src/main` the tech-lead named, plus two in `src/test`)
- `git diff develop...HEAD -- app domain | grep -E '^\+' | grep -E '@Ignore|assumeTrue|@Disabled|ignoreFailures|baseline|!!'` — **pass** (2 hits, both `assertThat(decoded!!.ladder)` in `CloudProgressCodecTest`; no production `!!`, no `@Ignore`, no baseline)
- `find . -name '*baseline*' -not -path './*/build/*' -not -path './.git/*'` — **pass** (no detekt/lint baseline file; every hit is a `.planning/` QA text file)
- `grep -rnE 'android\.util\.Log|Timber|println\(|System\.(out|err)|Log\.[dviwe]\(' app/src/main/java domain/src/main` — **pass** (0 hits; the phase's no-logging gate over the whole of main, not just the two packages)
- `grep -rn '^import com\.google\.android\.gms' app/src/main/java | grep -v '/platform/'` — **pass** (0 hits); `grep -rn 'com\.google\.android\.gms' app/src/main/java | grep -v '/platform/'` — **pass** (4 hits, all prose or the Downloadable-Fonts provider authority in `ui/theme/Type.kt:53-54`, no SDK type)
- `grep -rnE '^import (android|androidx|com\.google)' domain/src/main` — **pass** (0 hits)
- `grep -rn --include='*.kt' -E 'playerId|displayName|gamerTag|accountName|email' app/src/main/java | grep -v '/platform/games/'` — **pass** (reviewed line by line; every hit is the two stores/flows named above or an unrelated Classic board `displayName`)
- `grep -rn --include='*.kt' -E 'resetAllProgress|resetAwaiting|clearAll\(\)|deleteAll\(\)' app/src/main/java` — **pass** (one non-test caller each, all inside `applySwapAnswer` / `resetAllProgress`)
- `grep -rn 'onProfileSwapResolved' app/src/main/java` — **pass** (two call sites, both in `HomeViewModel`, one per dialog answer)
- `grep -rnE '\bas [A-Z]|\bas\?|unchecked' app/src/main/java/com/app/pegsolitaire/data/cloud .../platform/games/save .../data/CloudSyncStore.kt .../data/CloudProfileStore.kt` — **pass** (2 hits, both safe `as?`)
- `awk` over `app/build/test-results/testDebugUnitTest/*.xml` (79 files) — **pass** (tests=926, skipped=0, failures=0, errors=0); same over `domain/build/test-results/test/*.xml` — **pass** (tests=268, skipped=0, failures=0, errors=0). Total 1,194 / 0 skipped, which corroborates the phase's figure. **These are on-disk artefacts I read, written by the producer's run in progress (newest file 22:20), not a run I performed.**
- `grep -c '<error' app/build/reports/detekt/detekt.xml domain/build/reports/detekt/detekt.xml` — **pass** (0 and 0; artefacts dated 04:26, i.e. the convergence-build run — superseded by H-1)
- **Not run, by the task's environment constraint:** `./gradlew check`, `:app:assembleRelease`, `lint`, any test or detekt task, anything touching adb or an emulator. This verdict rests on static reading plus the greps above; the Check-command evidence is H-1's and H-2's, and my PASS is conditional on them being green in the same way the three earlier audits were.

## Open issues / risks
- **F-1 is the item I would least like to see forgotten.** It is invisible until a schema bump and then loses data silently and permanently. It belongs written into the D-07 decision, not only into a follow-up list.
- **F-2's third consequence (a permanently wedged push) has no detection.** `OVER_CAP` is recorded in a diagnostic key nothing reads (by CLOUD-05's design), so a player in that state would present as "cloud save stopped working" with no signal — the same class of problem the phase just spent three days on. A row-count cap is cheaper than the next investigation.
- **The review-coverage residue the tech-lead accepted knowingly is smaller after this sweep but not zero.** I read the four scoped risks across the whole range; I did not line-audit `CoverImageSource`, `CloudSyncStore`'s or `PlayGamesPreferences`' bookkeeping beyond their PII surface, or any Compose detail, because the task excluded them.
- **The destructive path still has no device evidence** (the tech-lead's FU-3). Nothing I found changes that; F-5 adds one case to that UAT.
- I did not re-verify the three earlier audits' findings, by instruction, and have re-raised none of them.

## Handoff
**Next: tech-lead — PASS on H-3.** The five suppressions are approved and the approval lines above are the artefact the merge commit needs (`rules/studio.md`, definition of done). Nothing returns to a developer: no finding here is a merge blocker and none requires a code change before the merge.

For phase 14 scoping, in priority order: **F-1** into the D-07 decision and into `docs/tech-design.md` when it is written (it is a correctness contract, not a follow-up chore); **F-2** and **F-3** together as one decode-hardening task (row cap + `Long` arithmetic, one file, one test class); **F-4** as a one-line hardening on `ProgressSubmitter.kt:411`, naturally folded into FU-1; **F-5** as one named case in the second-account UAT that FU-3 already requires. **I-2** goes to the game-designer as a copy question.
