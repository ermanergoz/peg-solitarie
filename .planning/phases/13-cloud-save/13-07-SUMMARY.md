---
phase: 13-cloud-save
plan: 07
subsystem: platform
tags: [play-games, saved-games, cloud-save, listing-metadata, strings, vector-raster, robolectric, junit4, truth, mockk]

# Dependency graph
requires:
  - phase: 13-cloud-save
    provides: "13-06's CloudSaveDescription (three ints) and CloudSaveConstants (COVER_PX, MAX_DESCRIPTION_CHARS) — this plan is the consumer both were shaped for"
  - phase: 11-play-games-session
    provides: "PlayGamesService's `@param:ApplicationContext` injection spelling and the no-logging-in-platform/games rule (T-11-04)"
  - phase: 12-leaderboards-achievements
    provides: "domain/achievement/ClassicBoard — `entries.size` is the description's denominator, so a sixth Classic board moves the line on its own"
  - quick-task: 260914-x4a
    provides: "ic_launcher_background.xml / ic_launcher_foreground.xml — the approved English-33 board art this plan rasterizes rather than re-authoring"
provides:
  - "CloudSaveDescriptionFormatter — CloudSaveDescription -> one localized, unconditionally length-capped line (D-11)"
  - "R.string.cloud_save_description — the four-positional-argument D-11 wording, in a delimited Phase-13 block in strings.xml"
  - "CoverImageSource — @Volatile-cached, off-main-thread 512px raster of the app's own board art, null on any failure"
  - "R.drawable.cloud_save_cover — the unmasked, full-bleed layer-list over the existing launcher vectors"
affects: [13-09, 13-10, 13-11, cloud-save, saved-games]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Listing copy stays in strings.xml and is resolved in platform/ from raw numbers, so data/ never touches a Context"
    - "Unconditional truncation instead of a length branch — a guard with no path around it"
    - "Runtime vector rasterization of existing art instead of a committed PNG: one source of truth, no binary in the diff, no rasterizer in the authoring pipeline"
    - "Optional decoration degrades to null rather than failing the operation it decorates"
    - "@GraphicsMode(NATIVE) is mandatory for any Robolectric test that asserts on rendered pixels or compressed bytes"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveDescriptionFormatter.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CoverImageSource.kt
    - app/src/main/res/drawable/cloud_save_cover.xml
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudSaveDescriptionFormatterTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/CoverImageSourceTest.kt
  modified:
    - app/src/main/res/values/strings.xml

key-decisions:
  - "The cover is rasterized at runtime from the existing vectors, not committed as a PNG — the repo's no-PNG rule holds and the cover cannot drift from the launcher icon"
  - "cloud_save_cover.xml exists as its own layer-list because R.mipmap.ic_launcher resolves to the <adaptive-icon> on API 26+, whose draw() applies a launcher-chosen mask — the cover would be circle-cropped and device-dependent"
  - "The background item points at ic_launcher_background, not its _legacy sibling: the legacy inset treatment is a square-tile concern with no meaning on a 512-square cover"
  - "The description's denominator comes from ClassicBoard.entries.size in both the production call and the test assertion — never a literal 5 in either"
  - "The length cap is applied unconditionally rather than behind an if — the guard has no branch a future input can reach around (T-13-24)"
  - "The cover-failure test intercepts getResources(), not getDrawable(): Context.getDrawable is final and forwards to getResources().getDrawable(id, theme), which is the lowest reachable point on the real inflation path"
  - "@GraphicsMode(NATIVE) on CoverImageSourceTest is load-bearing and KDoc'd as such — under Robolectric's default LEGACY mode both pixel assertions passed over an all-zero bitmap"

patterns-established:
  - "New user-facing copy is appended to strings.xml inside a delimited, phase-labelled block so parallel plans touching the same file merge cleanly"
  - "A measured number that justifies a constant (here 35,907 bytes against 800 KB) is recorded in the KDoc next to the constant, so a later failure reads as 'the input changed' rather than 'the number was always marginal'"

requirements-completed: []

# Metrics
duration: 22min
completed: 2026-09-15
---

# Phase 13 Plan 07: Cloud Save Listing Metadata Summary

**The two things that make a cloud save recognisable in the Play Games saves list — a localized, unconditionally capped one-liner built from the player's own figures through `strings.xml`, and a 512px cover rasterized at runtime from the app's existing launcher vectors — each of which degrades rather than failing the commit it decorates.**

## Performance

- **Duration:** ~22 min
- **Started:** 2026-09-15T20:34Z
- **Completed:** 2026-09-15T20:56Z
- **Tasks:** 2 of 2
- **Files modified:** 6 (5 created, 1 modified)

## Accomplishments

- **The copy exists in exactly one place, and it is not Kotlin.** `grep -rn '"Challenge level' app/src/main/java/` returns zero. The line is assembled in `platform/` because that is the nearest layer to the SDK call that can legally hold a `Context`; `data/`, which owns the three aggregates, never sees one. The consequence D-11 asked for falls out for free: the description follows the device locale **at commit time**, so a player who changes language sees the new wording on their next solve.
- **The denominator is derived, in production *and* in the test.** `ClassicBoard.entries.size` is the `%4$d` argument and is also what the test asserts against. A sixth Classic board changes the shipped line and the assertion together, with no third place to remember — which is the whole reason the acceptance criteria forbade a literal `5`.
- **The truncation cannot be reached around.** `.take(MAX_DESCRIPTION_CHARS)` is unconditional, not `if (length > cap)`. RESEARCH A2 is explicit that **no maximum is documented anywhere** for `setDescription`, so this is a guard against a server-side rejection that would cost the whole push, not a feature — and the `Int.MAX_VALUE` test proves it fires on inputs no player will ever produce.
- **The cover is the app's own art with no second copy of it.** `cloud_save_cover.xml` is a two-item layer-list pointing at the *existing* vectors. `find app/src/main/res -name "*.png"` still returns nothing, so the repo-wide no-PNG rule that `mipmap-anydpi/ic_launcher.xml` documents survives this phase, and the cover physically cannot drift from the launcher icon.
- **Rasterizing `R.mipmap.ic_launcher` would have been wrong and is now documented as such.** On API 26+ that id resolves to the `<adaptive-icon>`, and `AdaptiveIconDrawable.draw()` applies its own mask — the cover would arrive circle- or squircle-cropped depending on what the player's launcher last negotiated. The dedicated composite is deliberately unmasked and full-bleed, and the XML comment says why so nobody "simplifies" it later.
- **The 800 KB ceiling question is now answered with a number, not an assumption.** Measured: **35,907 bytes** PNG-lossless at `COVER_PX = 512` — 4.4% of the ceiling, four and a half doublings of headroom. RESEARCH A1 flagged this as `[ASSUMED]`; it is now measured on the JVM, and the number is recorded in both the production KDoc and the test constant's KDoc. UAT test 12's runtime `getMaxCoverImageSize()` cross-check remains worth doing, but it is now a confirmation rather than a discovery.
- **Both halves fail soft.** `coverOrNull()` is `runCatching`-wrapped and returns `null` on a stripped resource, a bad inflate or a failed allocation; the caller omits `setCoverImage` and commits anyway (T-13-26). The formatter cannot fail at all.

## Task Commits

1. **Task 1 (TDD RED): failing description-formatter test** — `f786e31` (test)
2. **Task 1 (TDD GREEN): `cloud_save_description` + `CloudSaveDescriptionFormatter`** — `af6704c` (feat)
3. **Task 2 (TDD RED): failing cover-image test** — `5377f68` (test)
4. **Task 2 (TDD GREEN): `cloud_save_cover.xml` + `CoverImageSource`** — `8020927` (feat)

No REFACTOR commits — neither implementation needed cleaning after GREEN.

**TDD gate compliance:** `test(13-07)` → `feat(13-07)` observed in order for both tasks. Both RED phases failed as compilation errors on the unresolved class under test, which is the correct RED for a package whose tests share one compilation unit.

## Files Created/Modified

- `platform/games/save/CloudSaveDescriptionFormatter.kt` — `@Singleton`, `@param:ApplicationContext`, one `format` method. KDoc records, in order: why formatting lives in `platform/` and not `data/`; that the commit-time locale is the decided behaviour and not a defect; RESEARCH A2 and why the cap is a guard; and that the returned string carries progress figures tied to a signed-in player and must never be logged (T-13-25 / T-11-04, ASVS V7).
- `res/values/strings.xml` — one string in a delimited `Phase-13 cloud-save copy` block at the end of the file, with an XML comment in the file's existing explanatory voice covering the locale behaviour, the four positional arguments in order, and why the fourth is supplied rather than written.
- `platform/games/save/CoverImageSource.kt` — `@Volatile private var cached`, `suspend fun coverOrNull()`, `Dispatchers.Default`, `runCatching { … }.getOrNull()?.also { cached = it }`. KDoc records: no Activity is needed; RESEARCH A3 and why a background inflate is the right trade given the failure mode; RESEARCH A1 with the measured byte count and the one constant to lower if it ever matters.
- `res/drawable/cloud_save_cover.xml` — two items, a long comment on why it is not `R.mipmap.ic_launcher`, why it is not a committed PNG, and why the non-legacy background is the right first item.
- `test/…/save/CloudSaveDescriptionFormatterTest.kt` — 5 tests, Robolectric, one per `<behavior>` bullet.
- `test/…/save/CoverImageSourceTest.kt` — 5 tests, Robolectric + `@GraphicsMode(NATIVE)`, one per `<behavior>` bullet.

## Decisions Made

Beyond the plan's pre-resolved discretion (runtime rasterization over a build-time PNG), three judgements were made during execution:

1. **`@GraphicsMode(GraphicsMode.Mode.NATIVE)` on the cover test.** See Deviation 1 — this is the difference between a test and a test-shaped no-op.
2. **The failure test intercepts `getResources()`, not `getDrawable()`.** `Context.getDrawable` is `final` (the compiler said so), and it forwards to `getResources().getDrawable(id, getTheme())`. Overriding `getResources()` on a `ContextWrapper` to return a MockK `Resources` that throws `NotFoundException` is therefore the lowest point still on the *real* inflation path — `ContextCompat.getDrawable` routes through it unchanged. The alternative, mocking the final `Context.getDrawable`, would have been a shortcut around the code under test.
3. **REQUIREMENTS.md left untouched, `requirements-completed` empty.** This plan's frontmatter names CLOUD-03 — "the cloud save carries a description, played time, and cover image so it is legible in the Play Games UI". It delivers the description and the cover as *values*; nothing attaches either to a snapshot until plan 13-09 builds the `SnapshotMetadataChange`, and played time is 13-08's `PlaySessionDao.totalElapsedMs()`. Marking CLOUD-03 complete here would report a player-visible outcome at the point where no player can see it. This also avoids a merge conflict with the parallel plan 13-08 worktree.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `@GraphicsMode(NATIVE)` added to `CoverImageSourceTest`**

- **Found during:** Task 2, GREEN phase
- **Issue:** Written per the plan against plain `@RunWith(RobolectricTestRunner::class)`, two of the five tests failed — and *how* they failed is the point. `getPixel` returned `0` at both sample points (a fully transparent bitmap), and the "PNG" measured **1,049,700 bytes**, which is exactly 512 × 512 × 4 plus a header. Robolectric's default `LEGACY` graphics mode stubs the entire drawing pipeline: `Canvas` calls are no-ops and `Bitmap.compress` writes the raw ARGB buffer instead of encoding. Had the assertions been written any weaker — "not null", "not empty" — this file would have shipped green while proving nothing at all about the two failure modes it exists to catch.
- **Fix:** `@GraphicsMode(GraphicsMode.Mode.NATIVE)` on the test class, with a KDoc paragraph naming both observed wrong numbers so the annotation cannot be removed as noise by a future reader.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/platform/games/save/CoverImageSourceTest.kt`
- **Verification:** both tests pass under NATIVE; the cover measures 35,907 bytes and the centre and corner pixels differ.
- **Committed in:** `8020927`

**2. [Rule 3 - Blocking] Reworded the `cloud_save_cover.xml` comment so the plan's own grep gate reads clean**

- **Found during:** Task 2, acceptance-criteria check
- **Issue:** The acceptance criterion is `grep -c "ic_launcher_background" app/src/main/res/drawable/cloud_save_cover.xml` returns 1. The first draft's comment explained why the `_legacy` background was *not* used and named the non-legacy drawable while doing so, making the count 2. Harmless to aapt, but a grep-based gate that a later plan re-runs would report a false positive forever — the same failure mode plan 13-06 hit with the gms package name in a KDoc.
- **Fix:** Reworded to "the first item is the adaptive BACKGROUND vector and deliberately not its `..._legacy` sibling". The reasoning survives intact; the duplicate literal is gone.
- **Files modified:** `app/src/main/res/drawable/cloud_save_cover.xml`
- **Verification:** both greps now return exactly 1.
- **Committed in:** `8020927`

**3. [Rule 3 - Blocking] ktlint `function-signature` on the test's `ContextWrapper` subclass**

- **Found during:** Task 2, gate run
- **Issue:** `private class BrokenDrawableContext(base: Context) : ContextWrapper(base) {` exceeded what ktlint's `ktlint_official` style permits on one line given the supertype call, failing `:app:ktlintTestSourceSetCheck`.
- **Fix:** Multi-line constructor parameter list.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/platform/games/save/CoverImageSourceTest.kt`
- **Verification:** `:app:ktlintCheck` green.
- **Committed in:** `8020927`

---

**Total deviations:** 3 auto-fixed (1× Rule 1, 2× Rule 3)
**Impact on plan:** No scope change. All three corrections are to files this plan created. Nothing outside `files_modified` was touched — in particular `data/ProgressSubmitter.kt` and its tests (plan 13-08, parallel) were neither read nor written, and the single `strings.xml` addition sits in a delimited, phase-labelled block at the end of the file so plan 13-10's later additions merge without conflict.

## Issues Encountered

One, and it is worth naming because it generalises: **a Robolectric test that asserts on rendered pixels or compressed bytes is silently meaningless without `@GraphicsMode(NATIVE)`**. The project had no prior test that rendered anything — the existing Robolectric suites are Compose-semantics and resource-resolution tests, neither of which touches a real `Canvas` — so nothing had forced the question before. The failure was only visible because the assertions were specific enough to produce a number that could be recognised as wrong (1,049,700 ≈ 512 × 512 × 4). This is recorded in `patterns-established` for any future screenshot or raster work.

## Verification Results

All commands run from the worktree, results as observed:

| Command | Result |
|---|---|
| `:app:testDebugUnitTest --tests "*CloudSaveDescriptionFormatterTest*"` | BUILD SUCCESSFUL — 5 tests, 0 failures, 0 errors |
| `:app:testDebugUnitTest --tests "*CoverImageSourceTest*"` | BUILD SUCCESSFUL — 5 tests, 0 failures, 0 errors |
| `:app:testDebugUnitTest --tests "*CloudSave*" --tests "*CoverImageSource*" --tests "*PlatformBoundary*" --tests "*DomainPurityTest*"` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` (full app suite) | BUILD SUCCESSFUL — **774 tests, 0 failures, 0 errors** |
| `:app:detekt` `:app:ktlintCheck` | BUILD SUCCESSFUL |
| `:app:assembleDebug` | BUILD SUCCESSFUL — `R.drawable.cloud_save_cover` and `R.string.cloud_save_description` compile |
| `:app:dpSpLiteralCheck` `:app:adIdLiteralCheck` `:app:gamesIdLiteralCheck` `:app:audioFocusLiteralCheck` | BUILD SUCCESSFUL |
| `grep -c 'name="cloud_save_description"' res/values/strings.xml` | 1 |
| `grep -rn '"Challenge level' app/src/main/java/` | 0 hits — the copy exists only in `strings.xml` |
| `grep -c "MAX_DESCRIPTION_CHARS" CloudSaveDescriptionFormatter.kt` | 3 |
| `grep -c "entries.size\|ClassicBoard" CloudSaveDescriptionFormatter.kt` | 3 — the denominator is derived |
| `grep -c "ic_launcher_foreground" cloud_save_cover.xml` / `"ic_launcher_background"` | 1 / 1 |
| `find app/src/main/res -name "*.png"` | 0 results — the repo still contains no PNG by design |
| `grep -rn "R.mipmap.ic_launcher" platform/games/save/` | 0 hits — the masked adaptive icon is not what gets rasterized |
| `grep -c "@Volatile"` / `"CloudSaveConstants.COVER_PX"` in `CoverImageSource.kt` | 2 / 4 |
| `grep -rn "Log\.\|println\|Timber" platform/games/save/` | 0 hits (T-13-25) |
| Cloud-save package test counts | `CloudSaveConstantsTest` 4, `NoOpCloudSaveStoreTest` 4, `FakeCloudSaveStoreTest` 9, `CloudSaveDescriptionFormatterTest` 5, `CoverImageSourceTest` 5 — 27 tests, 0 failures |

Behavioural acceptance criteria, confirmed by the passing tests rather than by inspection:

- `format(CloudSaveDescription(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)).length <= MAX_DESCRIPTION_CHARS` — green
- `coverOrNull()` width and height both equal `CloudSaveConstants.COVER_PX` — green
- two consecutive `coverOrNull()` calls return the identical instance (`isSameInstanceAs`) — green
- an unresolvable drawable yields `null`, not an exception — green

## Requirements

`requirements-completed` is deliberately empty; `REQUIREMENTS.md` was not modified. See Decision 3 above: CLOUD-03 is a player-visible outcome ("legible in the Play Games UI") and nothing this plan produces is attached to a snapshot until plan 13-09.

## Known Stubs

None. Both classes are complete and fully exercised; neither has an unwired data source. They have no production **caller** yet — plan 13-09 injects both into `PlayGamesCloudSaveStore` — but that is the wave ordering the plan specifies, not a stub.

## Threat Flags

None. Neither file introduces a network endpoint, an auth path, a file-access pattern or a schema change. The four threats in this plan's register are all `mitigate` and all verified above: T-13-24 (unconditional truncation, proven by the `Int.MAX_VALUE` test), T-13-25 (zero logging in the package), T-13-26 (`runCatching` → `null`, proven by the unresolvable-drawable test), T-13-27 (35,907 bytes measured against an 800 KB ceiling).

## Next Phase Readiness

Ready. Plan 13-09 injects both as `@Singleton`s with no further setup:

```kotlin
@Singleton class CloudSaveDescriptionFormatter @Inject constructor(@param:ApplicationContext appContext: Context) {
    fun format(description: CloudSaveDescription): String
}
@Singleton class CoverImageSource @Inject constructor(@param:ApplicationContext appContext: Context) {
    suspend fun coverOrNull(): Bitmap?
}
```

Two things 13-09 must honour, both already KDoc'd: `coverOrNull()` returning `null` means **omit `setCoverImage` and commit anyway** — never abort the commit — and the formatted string must not be logged on its way to `SnapshotMetadataChange`.

One carry-forward for UAT: test 12 should call `getMaxCoverImageSize()` on-device and compare against the JVM-measured 35,907 bytes. The expectation is now a confirmation, not a discovery.

## Self-Check: PASSED

All 5 created files verified present on disk and `strings.xml` verified modified. All 4 task commits (`f786e31`, `af6704c`, `5377f68`, `8020927`) verified present in `git log`. `git diff --diff-filter=D` across every commit in this plan reports no deleted tracked files. `STATE.md` and `ROADMAP.md` were not modified — the orchestrator owns those after the wave merges. `local.properties` was seeded from the primary checkout and is untracked; it was not staged.

---
*Phase: 13-cloud-save*
*Completed: 2026-09-15*
