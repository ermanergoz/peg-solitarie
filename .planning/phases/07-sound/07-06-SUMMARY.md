---
phase: 07-sound
plan: 06
subsystem: audio-verification
tags: [audio, r8, resource-shrinking, aapt2, release-build, uat, provenance, d-15, tuning]
requires:
  - "R.raw.sfx_* + docs/audio/CREDITS.md — the eleven assets and the provenance table (07-01)"
  - "SoundCue — the compile-time R.raw.* constants whose static reachability this plan proves (07-02)"
  - "StarPopSounds + SoundParams.starPopRate — the celebration ladder retuned here (07-04)"
  - "audioFocusLiteralCheck + AudioCreditsTest — the two permanent build gates (07-01)"
provides:
  - "docs/audio/CREDITS.md ## Release shrink verification — repeatable T-07-15 evidence against a real shrunk APK"
  - "The discharged Phase 7 manual audit (SOUND-01/05/06, D-01/D-02/D-04/D-10, T-07-02/T-07-04/T-07-16)"
  - "The audit-driven retune: a slower star reveal and a triad transposed down a rung"
affects:
  - "Any future phase adding a res/raw asset — the aapt2 technique is now recorded for audio as well as font_certs.xml"
  - "Phase 9 (manifest wiring) — inherits the note that release builds need the four real ad/PGS IDs"
tech-stack:
  added: []
  patterns:
    - "Prove a resource survives shrinking by following its id to the SHORTENED packaged path and byte-comparing, not by trusting the resource-table entry alone"
    - "Derive a tuning constant from the constant it must stay coherent with (STAR_POP_BASE_RATE from STAR_POP_STEP_SEMITONES) rather than writing a bare float"
key-files:
  created: []
  modified:
    - docs/audio/CREDITS.md
    - app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
    - app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt
key-decisions:
  - "A surviving resource id is a weaker claim than a surviving sound, so every entry was followed to its shortened packaged path and byte-compared with source — the shrinker can keep an id while replacing its payload"
  - "The star pitch complaint was fixed by transposing the whole triad down a rung (STAR_POP_BASE_RATE), not by narrowing STAR_POP_STEP_SEMITONES — narrowing would have flattened the rising climb D-15 exists to protect"
  - "The visual star stagger and the audio pop spacing stay coupled through the single ClayAnim.STAR_STAGGER_MS token; the animations-off fallback was scaled by the same 5/3 so both paths keep their prior relationship"
  - "The release APK built for the shrink proof used placeholder ad/PGS IDs supplied as environment variables — it is a verification artifact, not a shippable build"
patterns-established:
  - "Release-artifact verification: build, dump the resource table, follow each id to its packaged file, byte-compare against source"
requirements-completed: [SOUND-01, SOUND-02, SOUND-03, SOUND-04, SOUND-05, SOUND-06]
duration: ~15 min executor time (~3h 13m wall clock, including the blocking human audit)
completed: 2026-08-28
---

# Phase 7 Plan 06: Release Shrink Proof & Phase Audit Summary

**All eleven sounds proven byte-identical inside the minified, resource-shrunk release APK, and the
deferred aesthetic sign-off discharged — returning two tuning defects that were fixed: a slower star
reveal and a star-pop triad transposed down a major third.**

## Performance

- **Duration:** ~15 min executor time; ~3h 13m wall clock (18:06 → 21:19 local), the bulk of it the
  blocking human audit
- **Tasks:** 2 (1 automated, 1 blocking human-verify checkpoint)
- **Files modified:** 4

## Accomplishments

- **RESEARCH assumption A3 is no longer an assumption.** The phase's highest-consequence unknown —
  "every sound works in debug, every sound is silent in release" — is closed against a real shrunk
  artifact, with the evidence recorded in-repo so the check is repeatable.
- **The D-04 aesthetic decision, deferred since planning, is discharged**, and it earned its keep:
  the audit returned two real defects that no JVM test could have caught.
- **The set was retuned in response** — the Results celebration now reads as three counted events
  rather than one burst, and the star pops sit a major third lower.
- **Every shipped audio file's CC0 provenance is human-accepted** (D-01, T-07-02).

## Task Commits

1. **Task 1: Prove the sounds survive R8 and resource shrinking** — `69e4595` (docs)
2. **Task 2: Phase audit** — checkpoint; the two defects it returned fixed in `3f2d682` (fix)

## Task 1 — the shrink proof

`./gradlew :app:assembleRelease --offline` → BUILD SUCCESSFUL (1m 51s) →
`app/build/outputs/apk/release/app-release-unsigned.apk`, 2,038,611 B (unsigned by design, D-16).

`aapt2 dump resources | grep -c 'raw/sfx_'` returns **11**, with each of the eleven names — `sfx_jump`,
`sfx_peg_remove`, `sfx_select`, `sfx_undo`, `sfx_invalid`, `sfx_hint`, `sfx_win`, `sfx_star_pop`,
`sfx_btn_forward`, `sfx_btn_dismiss`, `sfx_sound_enabled` — appearing individually. Nothing was
stripped, so no `resources.getIdentifier()` workaround was needed (and `grep -rq getIdentifier
app/src/main/` still returns nothing, which is the point: that workaround would have defeated the
shrinker rather than satisfied it).

**One check beyond the plan, and it mattered.** The plan's criterion is the resource-table entry, but
a surviving *id* is a weaker claim than a surviving *sound* — the shrinker can retain an id while
replacing its payload. Every entry was therefore followed to its packaged file. Resource **paths are
shortened** in the shrunk APK (`raw/sfx_jump` → `res/NK.ogg`), which is expected and harmless since
the resource table carries the mapping and `SoundCue` resolves by id, never by path. All eleven
packaged entries are byte-identical to their committed sources (4,087–7,011 B) and are **Stored**,
not deflated — confirming aapt2 9.1.0's built-in `.ogg` handling and that the absence of a
`noCompress` block is correct rather than an oversight.

## Task 2 — the audit, and exactly what was attested

**The user approved the phase.** Being precise about the shape of that approval, because the
acceptance criteria asked for six itemised outcomes and that is not what was returned:

- The verifier gave a **global pass** ("all good"), not six separate answers. Steps 1 (cold-start
  first tap), 2 (the podcast test), 3 (immediate silence), 5 (D-10 single click) and 6 (CC0
  provenance sign-off) are covered by that global pass and are **not separately attested**.
- **Hardware: emulator only** — one emulator (`emulator-5554`, `sdk_gphone16k_arm64`) was connected
  and no physical device was. The verifier did **not** state which hardware was used; "emulator only"
  is inferred from what was attached, **not attested**. This matters most for step 2: audio-focus and
  ducking policy is enforced by the platform audio policy service, and emulator audio routing is the
  least faithful part of the emulator. T-07-04's manual half should be considered confirmed on an
  emulator, not on real hardware. Carried forward as a verifier note below.
- **Step 4 (the aesthetic audit) returned two named defects**, both fixed below. The rest of the
  aesthetic verdict — candy-clay character, one-family cohesion, the quiet invalid tap, the removal
  layering under the jump, the jump pitch ladder — passed within the global approval.

No licence problems were reported: all eleven CREDITS rows are single-source Kenney *Interface
Sounds* CC0 1.0 and were accepted. **No asset was swapped** — the two defects were tuning, not
sourcing, so `docs/audio/CREDITS.md`'s provenance table is unchanged.

## The two defects, and how they were fixed

**1. Star reveal too fast** — *"at the end of the game, let the stars be slower."*

`ClayAnim.STAR_STAGGER_MS` 120 → **200**. That one token drives both the visual stagger
(`ResultsScreen.EarnedStar`, and the trailing element at `TOTAL_STARS * STAR_STAGGER_MS`) and, through
`StarPopSounds`, the audio pop spacing. It stays a single token deliberately: the pop is not *near*
its star, it is *on* it, and forking the two is how they drift apart. At 120 ms each star arrived
barely halfway through the previous pop's 260 ms `STAR_POP_MS` and the three read as one burst.

`SoundParams.STAR_POP_STAGGER_MS` (the animations-disabled fallback, D-13) 150 → **250** — the same
5/3 factor, so the fallback keeps the slightly wider gap it always had. That gap is not arbitrary:
with no star landing to pace them, the pops need a little more air to read as three events.

**2. Star pop too high pitched** — *"change the star sound it is too high pitched."*

Fixed with lever (a), `STAR_POP_BASE_RATE`, and **not** lever (b), `STAR_POP_STEP_SEMITONES` — the
complaint was about pitch *height*, and the step size is ladder *shape*. Narrowing the step would
have flattened the rising climb that D-15 exists to protect, trading a fixed complaint for a broken
decision. Transposition changes height and leaves shape untouched.

The base is now **derived** rather than written as a bare float:

```kotlin
val STAR_POP_BASE_RATE: Float = SEMITONE_STEP.pow(-STAR_POP_STEP_SEMITONES)
```

One rung below the sample's own pitch, which centres the triad on it. The ladder moves from
**1.000 / 1.260 / 1.587** to **0.794 / 1.000 / 1.260** — the new top note is the old *middle* note, so
the third star tops out a major third above the sample instead of a minor sixth. Deriving it from the
step size means the two constants cannot drift apart later.

Everything D-15 asks for survives: the ladder is still a strictly rising prefix, a 3-star finish still
resolves strictly higher than a 2-star one, `starPopRate` is still total and clamped, and
`ResultsCelebrationSoundTest`'s negative assertion — that a 2-star result never plays the top note —
is untouched and still passes.

The asset itself was not swapped. The rate change achieved the result, and it costs no new file and
no CREDITS.md churn. Side effect worth noting: `SoundPool` resampling lengthens the 135 ms
`sfx_star_pop.ogg` to ~170 ms at the new base rate, comfortably inside the widened 200 ms stagger.

## Files Created/Modified

- `docs/audio/CREDITS.md` — `## Release shrink verification` appended beneath the existing
  `## Verification` section: date, exact Gradle and aapt2 commands, APK path, the 11/11 count, the
  byte-comparison result, and the env-var caveat
- `app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt` — `STAR_STAGGER_MS` 120 → 200, with
  the coupling to the audio path documented at the token
- `app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt` — `STAR_POP_BASE_RATE` derived and
  transposed down a rung; `STAR_POP_STAGGER_MS` 150 → 250
- `app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt` — renamed the base-rate test
  (star 0 is no longer the sample's own pitch) and added two cases pinning the transposition

## Decisions Made

- **Follow the id to the file, don't trust the table row.** Recorded as a reusable technique, since
  the same trap applies to any future `res/raw` addition.
- **Height, not shape.** `STAR_POP_BASE_RATE` over `STAR_POP_STEP_SEMITONES`, for the reason above.
- **Derive the base from the step.** A bare `0.79371f` would have been a magic number that silently
  decouples from the step size on the next edit; `SEMITONE_STEP.pow(-STAR_POP_STEP_SEMITONES)` cannot.
- **One stagger token for sight and sound.** Both raised together; the fallback scaled by the same
  factor rather than re-guessed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `assembleRelease` cannot run without the four release ad/PGS IDs**

- **Found during:** Task 1
- **Issue:** The release variant fails fast by design (D-09) when `ADMOB_APP_ID`,
  `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID` or `PGS_PROJECT_ID` is unresolved.
  `local.properties` holds only `sdk.dir` and none were in the environment, so the build failed at
  configuration in 417 ms. The plan assumed `assembleRelease` would just run.
- **Fix:** Supplied the four as deliberately non-real values (`LOCAL-SHRINK-VERIFY-NOT-A-REAL-ID`,
  `PGS_PROJECT_ID=000000000000`) via the **documented CI environment-variable path**, for that single
  invocation. Nothing written to disk, nothing committed, `local.properties` untouched. The IDs gate
  whether the build starts and have no bearing on resource shrinking; no real ad unit was involved,
  so D-05's "never a real ad unit in a developer build" is not weakened.
- **Files modified:** none (env-only); the caveat is recorded in `docs/audio/CREDITS.md`
- **Verification:** `assembleRelease` then succeeded and produced the artifact the proof runs against
- **Committed in:** `69e4595`

**2. [Rule 2 - Missing Critical] The plan's acceptance criterion could pass on a stripped sound**

- **Found during:** Task 1
- **Issue:** `grep -c 'raw/sfx_'` counts resource-**table** entries. `isShrinkResources` can retain a
  resource id while replacing its payload, so the criterion as written could report 11/11 for an APK
  whose sounds are placeholders — which is precisely the failure the task exists to rule out.
- **Fix:** Added the payload check: each id followed to its shortened packaged path via
  `aapt2 dump resources`, then `unzip -l` byte-compared against source. All eleven identical.
- **Files modified:** `docs/audio/CREDITS.md` (the stronger evidence is what is recorded)
- **Verification:** 11/11 byte-identical and Stored
- **Committed in:** `69e4595`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 missing-critical)
**Impact on plan:** No scope creep. The first unblocked the task; the second strengthened its central
claim from "the id survives" to "the sound survives".

## Issues Encountered

**The audit's approval was global, not itemised, and the hardware was not stated.** Recorded above
rather than smoothed over, because the acceptance criteria asked for per-step outcomes and a named
device. Steps 1/2/3/5/6 rest on a global pass; the hardware is inferred from `adb devices`, not
attested. No per-step results were fabricated.

**A third point the verifier raised — back navigation from Results returning to the finished game —
was deliberately not acted on.** It is navigation behaviour, outside this phase's sound scope, and is
being handled separately. No navigation file was touched.

**`AudioCreditsTest` staleness (the 07-01 caveat) bit as predicted.** `docs/audio/CREDITS.md` is read
from the working tree at runtime and is not a declared task input, so editing it alone left
`:app:testDebugUnitTest` UP-TO-DATE. Forced with `--rerun`: 2 tests, 0 failures against the edited
file.

## Verification Performed

- `./gradlew :app:assembleRelease --offline` — BUILD SUCCESSFUL (1m 51s)
- `aapt2 dump resources <release apk> | grep -c 'raw/sfx_'` — **11**; all eleven names present
  individually
- All eleven packaged payloads byte-identical to source and **Stored** (uncompressed)
- `grep -rq 'getIdentifier' app/src/main/` — no match; `grep -q 'noCompress' app/build.gradle.kts` —
  no match
- `./gradlew :app:check --offline` — BUILD SUCCESSFUL after the retune: **352 JVM tests, 0 failures,
  0 errors** (up from 350; the two new `SoundParamsTest` cases), plus detekt, ktlint (main + test),
  `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck` and lint
- `SoundParamsTest` 15 tests, `ResultsCelebrationSoundTest` 7, `ResultsScreenTest` 5 — all green after
  the stagger and pitch changes; no test pinned either old literal
- `AudioCreditsTest` force-re-run with `--rerun` against the edited CREDITS.md — 2 tests, 0 failures

## Note for the Verifier

Two things to carry forward rather than assume:

1. **The release APK used for the shrink proof is a verification artifact, not a shippable build.** It
   was built with placeholder ad/PGS IDs supplied via environment variables and is unsigned (D-16). A
   real release build requires the genuine four IDs and Phase 14 signing.
2. **The manual audit was emulator-only and globally approved.** T-07-04 (audio focus / ducking under
   real platform policy) and T-07-16 (cold-start first-tap latency) had their manual halves confirmed
   on an emulator, and by a global pass rather than an itemised one. If a physical device becomes
   available before release, steps 1 and 2 are the two worth repeating on it.

## User Setup Required

None — no external service configuration required by this plan. (The four release ad/PGS IDs are a
pre-existing Phase 6 requirement, not new here.)

## Requirements

**SOUND-01 through SOUND-06 are complete.** This plan closes the manual half of each: the cues fire
audibly from a cold start (SOUND-01), the invalid tap is quiet and non-punishing (SOUND-02), the
celebration reads as a rewarded sequence at its new pacing and pitch (SOUND-03), the button cues are
distinct (SOUND-04), the mute cuts overlapping playback immediately (SOUND-05), and the game never
ducks the player's own audio (SOUND-06) — the last confirmed on an emulator, as noted above.

## Threat Flags

None. This plan adds no network endpoint, auth path, schema change or permission. Its four registered
threats are addressed as registered: **T-07-15** by the release-artifact proof, strengthened to a
byte comparison; **T-07-02** by the human CC0 sign-off, with no row rejected; **T-07-04** and
**T-07-16** by the manual audit, with the emulator-only caveat recorded rather than hidden.

## Known Stubs

None.

## Next Phase Readiness

Phase 7 is complete: all six plans executed, all six requirements met, `./gradlew :app:check
--offline` green, and the release-shrink risk closed. Phase 8 (Assist / Generator debt — DEBT-01
solver deadline envelope, DEBT-02 Diamond-41) is unblocked and independent of this work.

## Self-Check: PASSED

All four modified files verified present on disk. Both commit hashes (`69e4595`, `3f2d682`) verified
present in `git log`.

---
*Phase: 07-sound*
*Completed: 2026-08-28*
