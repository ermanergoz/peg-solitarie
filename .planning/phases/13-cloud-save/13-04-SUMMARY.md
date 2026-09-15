---
phase: 13-cloud-save
plan: 04
subsystem: data
tags: [kotlin, kotlinx-serialization, wire-format, trust-boundary, input-validation, junit4, truth, cloud-save]

# Dependency graph
requires:
  - phase: 13-cloud-save
    provides: "plan 13-01's ProgressMerge / CloudProgress / LevelKey / LevelBest / ProgressMode / ProgressMergeLimits — the pure model this codec mirrors and the merge mergeBytes runs"
  - phase: 05-persistence
    provides: "ResumeSnapshotDto + SnapshotMapper — the `@Serializable`-lives-in-data pattern, the never-throwing single-catch decode, and the on-disk-contract constants-object shape, all copied verbatim"
  - phase: 03-challenge-mode
    provides: "ChallengeLevelBounds.MIN/MAX/clamp — the fail-safe-not-fail-closed bound applied to every untrusted Challenge level id and to the ladder"
  - phase: 12-play-games-achievements
    provides: "AchievementThresholds.PERFECT_STARS — the star ceiling the decoder clamps to"
provides:
  - "CloudProgressCodec.encode / decode / encodeWithinCap / mergeBytes — the whole byte-level surface plans 13-05, 13-08 and 13-09 build against"
  - "CloudProgressDto / LevelBestDto — the pinned wire format, structurally incapable of carrying a board (CLOUD-04)"
  - "CloudPayloadKeys — SCHEMA_VERSION plus the two mode tokens, decoupled from the Room column values"
  - "CloudPayloadLimits — the 3 MB documented floor, the 256 KB margin, and the derived TRIM_TARGET_BYTES"
  - "A validating decode boundary: the phase's first and only untrusted-input gate"
affects: [13-05, 13-08, 13-09, cloud-save, conflict-resolution, saved-games]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Wire tokens as their own constants object, deliberately duplicating the DB column values rather than importing them"
    - "Drop-the-row-not-the-payload validation: one hostile row costs one level, never the whole save"
    - "A schemaVersion ceiling that refuses outright, paired with ignoreUnknownKeys that accepts silently — opposite policies for opposite directions"
    - "Structural CLOUD-04 enforcement by set equality over the serial descriptor's element names"
    - "Encode-measure-trim with an overshoot-proportional drop count and the termination argument stated in KDoc"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressDto.kt
    - app/src/main/java/com/app/pegsolitaire/data/cloud/CloudPayloadLimits.kt
    - app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt
    - app/src/test/java/com/app/pegsolitaire/data/cloud/CloudProgressCodecTest.kt
  modified: []

key-decisions:
  - "A newer schemaVersion is a failed load, not a partial one — reading a newer payload on its known fields would look like success, then push the lossy subset back over the richer original"
  - "stars is clamped but bestTimeMs/bestScore/levelId/mode drop the row — clamping is only correct where the repair is unambiguous and the rest of the row is still valuable"
  - "MIN_RECORDED_BEST_TIME_MS is named rather than written as `> 0`: the zero is the min-merge's absorbing element, so this is a policy, not an arithmetic bound"
  - "Duplicate keys inside one payload are merged with ProgressMerge rather than last-one-wins, so a crafted duplicate cannot overwrite a better result with a worse one"
  - "Rows are emitted in a total order (mode, then Challenge numerically) so the same progress always produces the same bytes"
  - "mergeBytes re-encodes through encodeWithinCap, not encode — a union is the one operation that can grow a payload"
  - "CloudProgressDto/LevelBestDto/CloudPayloadKeys are internal; only the codec and the limits are visible outside data/cloud, so no caller can hand-roll a payload"

patterns-established:
  - "Forward-compatibility fixtures are raw JSON strings with extra keys, never a serialized 'future DTO' — the point is the real wire text a newer writer emits"
  - "The CLOUD-04 gate is an `isEqualTo` over the whole descriptor element-name set, never a `contains`, so adding a field fails the test"

requirements-completed: [CLOUD-01, CLOUD-04, CLOUD-05]

# Metrics
duration: 22min
completed: 2026-09-15
---

# Phase 13 Plan 04: Cloud Payload Codec Summary

**The cloud payload now has a pinned wire format, a decode boundary that validates every untrusted field without ever throwing or logging, a schema ceiling that refuses a newer writer outright, and a trim loop proven to fit 10,000 Challenge levels into 64 KB while never dropping a Classic row.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-09-15T20:08:00Z
- **Completed:** 2026-09-15T20:30:00Z
- **Tasks:** 2 of 2
- **Files modified:** 4 (4 created, 0 modified)

## Accomplishments

- **CLOUD-04 is now structural, not aspirational.** `CloudProgressDto` has no field that could carry a board, a move history or a resume blob, and `CloudProgressCodecTest` asserts *set equality* over `CloudProgressDto.serializer().descriptor.elementNames` and `LevelBestDto`'s. A future field added for any reason fails that test. A payload type that cannot represent an in-progress game cannot leak one.
- **The phase's first trust boundary is closed (CLOUD-05, ASVS V5).** `decode` never throws and never logs. Malformed, truncated, empty and non-UTF8 bytes all return `null` (the caller's "stay local"). Every row is validated: `stars` is clamped into `0..PERFECT_STARS`, and a row is dropped for a non-positive `bestTimeMs`, a negative `bestScore`, a blank `levelId`, an unrecognised mode token, or a Challenge `levelId` that will not parse inside `ChallengeLevelBounds`. The drop is per-row — the "one valid row plus one zero-time row" test proves exactly one entry survives, not zero.
- **Pitfall 10 is now closed on both sides.** 13-01 pinned `NO_TIME_RECORDED = Long.MAX_VALUE` as the merge identity; this plan adds the gate that stops the absorbing value (`0L` or below) ever reaching that merge from the network.
- **D-07 is expressed as two opposite policies, deliberately.** Unknown keys at either level are ignored (a newer writer's extra fields cost nothing); a declared `schemaVersion` above this build's is refused outright. The asymmetry is the whole point: reading a *newer schema* partially would look like success and then push the lossy subset back over the richer original.
- **Pitfall 11 is closed and gated twice.** The wire tokens live in `CloudPayloadKeys`, duplicating the Room mode literals on purpose; the acceptance grep proves `data/cloud/` never names the persistence-keys object, and the codec test pins both token strings literally.
- **D-05's trim is proven, not asserted.** The full 10,000-level ladder plus five Classic rows encodes to ~430 KB and is trimmed to fit a deliberately artificial 64 KB cap; the surviving Challenge levels are strictly the numerically highest ones (minimum survivor > maximum dropped), every Classic row survives, and `ladder`/`playedTimeMs` are unchanged. A Classic-only payload against a 16-byte cap is emitted **over the cap** rather than losing a row — the honest failure.
- **Both of plan 13-01's hand-off obligations are discharged here** (see Prior-Wave Obligations below).

## Task Commits

1. **Task 1 (RED): failing codec spec** — `163c0f8` (test)
2. **Task 1 (GREEN): DTO, wire tokens, limits, validating decode** — `dc64a9f` (feat)
3. **Task 2 (RED): failing trim spec** — `347cea2` (test)
4. **Task 2 (GREEN): encode-measure-trim loop** — `f021af1` (feat)

No REFACTOR commits: neither GREEN implementation needed a cleanup pass.

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressDto.kt` — `@Serializable internal data class CloudProgressDto` (schemaVersion / ladder / playedTimeMs / bests) and `LevelBestDto` (mode / levelId / stars / bestTimeMs / bestScore), plus `internal object CloudPayloadKeys` holding `SCHEMA_VERSION = 1`, `SCHEMA_VERSION_UNKNOWN = 0`, `MODE_CLASSIC = "classic"`, `MODE_CHALLENGE = "challenge"`. The KDoc carries `ResumeSnapshotDto`'s nullable-with-a-default rule for post-v1 fields and states the wire-contract-vs-column-value separation.
- `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudPayloadLimits.kt` — `MAX_DATA_SIZE_FLOOR_BYTES = 3 * 1024 * 1024` (KDoc'd as the value `SnapshotsClient.getMaxDataSize()` is documented to be *guaranteed at least*, i.e. a floor), `PAYLOAD_SAFETY_MARGIN_BYTES = 256 * 1024`, and the **derived** `TRIM_TARGET_BYTES`. The KDoc records that plan 13-09's store re-checks the true runtime value and ignores an answer below the floor.
- `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt` — `encode` (stable ordering), `decode` (the validating boundary), `encodeWithinCap` (the trim loop, with its termination argument in KDoc), `mergeBytes` (the conflict lambda), and one private validator per untrusted field.
- `app/src/test/java/com/app/pegsolitaire/data/cloud/CloudProgressCodecTest.kt` — 23 pure-JVM tests covering round trip, stable encoding, forward compatibility, the schema gate, four malformed-input shapes, seven row-validation rules, scalar clamping, the pinned wire tokens, the two descriptor gates, both `mergeBytes` cases, the ladder-default cross-check, and the five trim properties.

## Decisions Made

- **The DTOs and the wire-token object are `internal`; only `CloudProgressCodec` and `CloudPayloadLimits` are visible outside the package.** Nothing in `data/` or `platform/` has a legitimate reason to construct a `CloudProgressDto` by hand — doing so would bypass the validating boundary entirely, which is the one thing this package exists to make impossible.
- **Duplicate keys within a single payload are merged, not overwritten.** `last one wins` would let a crafted payload carrying `{level 7: 3 stars}` followed by `{level 7: 0 stars}` walk a value backwards. Feeding both through `ProgressMerge.merge` makes the decoder immune to row order for the same reason the merge itself is immune to device order.
- **The drop count is floored at 1 *and* carries a `+ 1`.** Integer division rounds toward zero, so a small overshoot would otherwise compute zero rows to drop and the loop would never make progress — the single way this could spin. Both guards are noted in the KDoc as load-bearing rather than defensive.
- **`ARTIFICIAL_CAP_BYTES = 64 KB` is derived in the test from nothing, on purpose.** It is explicitly *not* `TRIM_TARGET_BYTES`: at ~2.75 MB the real target would never fire against a ~430 KB payload, and a safety valve that is never exercised is a safety valve that is not known to work.
- **`CHALLENGE_ROW_COUNT` in the test is `ChallengeLevelBounds.MAX`, not a literal `10_000`.** The plan's fixture size and the generator's ceiling are the same number for a reason — every row must be a *legal* level id, or the decoder would (correctly) drop it and the test would be measuring the wrong thing.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The `ProgressPersistenceKeys` grep gate tripped on KDoc prose**

- **Found during:** Task 1 (acceptance-criteria check)
- **Issue:** `grep -rn "ProgressPersistenceKeys" app/src/main/java/com/app/pegsolitaire/data/cloud/` must return 0 hits. It returned 1 — in `CloudPayloadLimits`' KDoc, which *named* the object while listing the sibling constants objects it is shaped after. No code referenced it; the gate is (correctly) a blunt textual instrument. This is the same class of trip plan 13-01 hit in `CloudProgress.kt`.
- **Fix:** Described the neighbour ("the `data` package's Room-identifier object") instead of naming it, and added an explicit note that a build gate greps this package for that name so the vocabulary must stay out, prose included. The `CloudProgressDto` KDoc that makes the Pitfall-11 argument was written the same way.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudPayloadLimits.kt`
- **Verification:** grep now returns 0.
- **Committed in:** `dc64a9f`

**2. [Rule 3 - Blocking] ktlint `chain-method-continuation` on the descriptor assertions**

- **Found during:** Task 1 (`:app:ktlintTestSourceSetCheck`)
- **Issue:** `CloudProgressDto.serializer().descriptor.elementNames.toSet()` is a four-link chain, and ktlint_official demands a newline before each `.` past its threshold — six violations across two lines.
- **Fix:** Split each into a named `payloadDescriptor` / `rowDescriptor` local plus a two-link chain. It also reads better: the descriptor is the subject of the assertion, and naming it says so.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/cloud/CloudProgressCodecTest.kt`
- **Verification:** `./gradlew :app:ktlintCheck :app:detekt` exits 0.
- **Committed in:** `dc64a9f`

**3. [Rule 3 - Blocking] Worktree missing `local.properties`**

- **Found during:** Setup, before Task 1
- **Issue:** `local.properties` is gitignored, so the parallel-execution worktree had none and every `:app` Gradle task would fail with "SDK location not found". Identical to plan 13-01's third deviation.
- **Fix:** Seeded the worktree's copy from the primary checkout, per the orchestrator's own setup instruction. The file is gitignored and is **not** part of any commit.
- **Files modified:** none tracked.
- **Verification:** `git status --short` clean after each commit; no commit touches `local.properties`.
- **Committed in:** n/a (untracked, environment-only)

---

**Total deviations:** 3 auto-fixed (3 × Rule 3 — blocking issues).
**Impact on plan:** None on scope or behaviour. Two were verification-gate mechanics (one prose, one formatting) and one was worktree environment setup. No production logic changed as a result.

## Prior-Wave Obligations Discharged

Plan 13-01 left two obligations for "13-03" (the codec plan, renumbered to 13-04). Both fall inside this plan's scope and both are now closed:

1. **Pin the `ProgressMode` wire tokens in a codec test.** The two-way mapping lives in exactly one place — `CloudProgressCodec.toWireToken` / `String.toProgressModeOrNull`, both reading `CloudPayloadKeys` — and `theWireTokenForEachModeIsPinned` asserts the encoded JSON contains the literal `"classic"` and `"challenge"`. An unknown token drops its row (`aRowWithAnUnknownModeTokenIsDropped`), which also proves the mapping is exhaustive rather than defaulting.
2. **Assert `ProgressMergeLimits.DEFAULT_LADDER == ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL`.** `theDomainLadderIdentityMatchesTheAppSideChallengeDefault` does exactly that. `:domain` cannot reference the `app`-module constant, so 13-01 could only assert its half against `ChallengeLevelBounds.MIN`; this is the `app`-side half, and the two together pin all three values to the same number.

13-01 also flagged that "`encodeWithinCap`'s shrink loop is bounded precisely because `dropLowestChallenge` leaves Classic rows untouched — so the `challengeRows == 0` escape branch is load-bearing and needs its own test with an absurdly small `maxBytes`." That test is `aClassicOnlyPayloadAgainstAnAbsurdCapIsEmittedWholeRatherThanLosingARow`.

## Issues Encountered

None. Both tasks executed as written.

One thing worth carrying forward rather than flagging as a problem: `encodeWithinCap` and `mergeBytes` still have **no production call site** — 13-05's sync path and 13-09's Saved Games store are what will call them. As with 13-01's forward-facing API, they are fully implemented and fully tested rather than placeholders: no hardcoded return, no TODO, no empty collection standing in for real data. Proving the codec before the SDK exists is the ROADMAP's explicit sequencing mandate, so a call-site-free window is the intended state at the end of this plan.

## Known Stubs

None.

## Threat Flags

None. This plan introduces no new network endpoint, auth path or schema — it *bounds* the one untrusted input the phase already planned for. All five `mitigate` dispositions in the plan's threat register are discharged:

| Threat ID | Mitigation | Evidence |
|-----------|-----------|----------|
| T-13-10 | `decode` parses inside a single `catch (IllegalArgumentException)` and returns `null` | `malformedTruncatedEmptyAndNonUtf8BytesAllDecodeToNull` green |
| T-13-11 | Per-row validation before any value reaches `ProgressMerge` | `aRowWithNonPositiveBestTimeIsDroppedAndEveryOtherRowSurvives`, `starsOutsideTheLegalRangeAreClampedRatherThanDroppingTheRow`, `rowsWithANegativeScoreOrABlankLevelIdAreDropped`, `aChallengeRowWhoseLevelIdIsNotAnIntOrIsOutOfBoundsIsDropped`, `theLadderIsClampedAndANegativePlayedTimeFloorsToItsIdentity` all green |
| T-13-12 | The payload type has no field that can carry a board; asserted by descriptor set equality | `thePayloadCarriesNoBoardNoMovesAndNoResumeField` green |
| T-13-13 | `encodeWithinCap` trims to the documented floor minus a margin, termination argued in KDoc | `tenThousandChallengeRowsAreTrimmedToFitAnArtificialCap` + `theTrimKeepsEveryClassicRowAndBothScalars` green |
| T-13-14 | Zero logging in the new package | `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/data/cloud/` returns 0 |

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

**Ready.** Plans 13-05, 13-08 and 13-09 can build against the exact names this plan contracted:

```kotlin
object CloudProgressCodec {
    fun encode(progress: CloudProgress): ByteArray
    fun decode(bytes: ByteArray): CloudProgress?                       // null = unreadable, stay local
    fun encodeWithinCap(progress: CloudProgress, maxBytes: Int = CloudPayloadLimits.TRIM_TARGET_BYTES): ByteArray
    fun mergeBytes(left: ByteArray, right: ByteArray): ByteArray       // the conflict-loop lambda
}
object CloudPayloadLimits { MAX_DATA_SIZE_FLOOR_BYTES; PAYLOAD_SAFETY_MARGIN_BYTES; TRIM_TARGET_BYTES }
```

Three things the downstream plans inherit:

1. **`decode` returning `null` means "stay local", never "the player has no progress".** Treating a `null` as `CloudProgress.EMPTY` at a *call* site would merge an empty payload over real local data on the next push. `mergeBytes` is the one place that substitution is correct, because there the identity is being joined *with* the other side rather than replacing it.
2. **Plan 13-09 must still re-check the real `getMaxDataSize()` before committing**, and must ignore an answer below `MAX_DATA_SIZE_FLOOR_BYTES` — a response that low violates the documented contract, and honouring it would shrink a player's payload for no reason.
3. **`CloudProgressDto` is internal and must stay that way.** Anything that needs bytes goes through the codec; that is what makes the validation unavoidable rather than merely available.

No blockers. The ROADMAP's sequencing mandate still holds: `app/src/main/java/com/app/pegsolitaire/platform/games/save/` does not exist.

## Self-Check: PASSED

**Created files exist:**

- FOUND: `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressDto.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudPayloadLimits.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/cloud/CloudProgressCodec.kt`
- FOUND: `app/src/test/java/com/app/pegsolitaire/data/cloud/CloudProgressCodecTest.kt`

**Commits exist:** FOUND `163c0f8`, FOUND `dc64a9f`, FOUND `347cea2`, FOUND `f021af1`

**Verification commands actually run, with results:**

| Command | Result |
|---------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | BUILD SUCCESSFUL — 23 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*" --tests "*SnapshotMapperTest*" --tests "*DomainPurityTest*" --tests "*PlatformBoundaryTest*"` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest :domain:test` (full suites) | BUILD SUCCESSFUL |
| `./gradlew :app:ktlintMainSourceSetCheck :app:detekt` | BUILD SUCCESSFUL |
| `./gradlew :app:ktlintCheck :app:detekt` | BUILD SUCCESSFUL |

**Acceptance-criteria gates:**

| Gate | Expected | Actual |
|------|----------|--------|
| `CloudProgressCodecTest` executed count | ≥ 18 | 23 |
| `grep -c "@Serializable"` in `CloudProgressDto.kt` | ≥ 1 | 3 |
| `grep -c "const val SCHEMA_VERSION = 1"` | 1 | 1 |
| `grep -rn "ProgressPersistenceKeys" …/data/cloud/` | 0 | 0 |
| `grep -rn "@Serializable" domain/src/main/kotlin/` | 0 | 0 |
| `grep -rn "Log\.\|println\|Timber" …/data/cloud/` | 0 | 0 |
| `grep -c "fun encodeWithinCap"` in `CloudProgressCodec.kt` | ≥ 1 | 1 |
| `grep -c "CloudPayloadLimits.TRIM_TARGET_BYTES"` in `CloudProgressCodec.kt` | ≥ 1 | 2 |
| `grep -c "ProgressMerge.dropLowestChallenge"` in `CloudProgressCodec.kt` | ≥ 1 | 2 |
| `decode({"schemaVersion":99,…})` | `null` | `null` (`aNewerSchemaVersionIsAFailedLoadNotAPartialOne`) |
| one valid row + one `bestTimeMs = 0` row | 1 entry | 1 entry (`aRowWithNonPositiveBestTimeIsDroppedAndEveryOtherRowSurvives`) |
| `CloudProgressDto` descriptor element names | `{schemaVersion, ladder, playedTimeMs, bests}` | equal (`isEqualTo` over the set) |
| `encodeWithinCap(tenThousandRows, 64 KB).size` | ≤ 65_536 | ≤ 65_536 |
| `encodeWithinCap(classicOnly, 16).size` | > 16 | > 16, decodes to the full 5 Classic rows |
| `git diff --diff-filter=D` over the plan's commits | no deletions | none |
| `ls app/…/platform/games/save` | absent | absent |

---
*Phase: 13-cloud-save*
*Completed: 2026-09-15*
