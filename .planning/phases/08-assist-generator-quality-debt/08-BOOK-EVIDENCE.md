# Phase 8 — Opening Book Release-Retention Evidence

**Purpose:** prove that the five committed opening-book resources under
`domain/src/main/resources/golden/book/` survive — **byte-intact** — into a minified,
resource-shrunk release APK. This closes RESEARCH assumption **A5**, the last unverified
structural claim in the phase.

**Why this is load-bearing rather than precautionary.** Since plan 08-10 the book is on the
production hint path. `OpeningBook` **fails closed**: if the resources are absent it returns a
MISS and the pipeline silently degrades to solver-only — which is measurably the pre-book
behaviour, including Wiegleb-45's untruthful "no winning move" banner. That failure would
produce **no error at all**, so packaging has to be proven, not assumed.

**Environment:** AGP 9.1.0, Gradle 9.5.1 wrapper, Kotlin 2.3.20, JDK 17, `compileSdk` 36,
macOS (Darwin 24.6.0). Release variant, **unsigned** (Phase 6 D-16 — no signing config is
committed; real signing is Phase 14 / REL-02).

---

## Result summary

| Question | Answer |
|----------|--------|
| Does `:app:assembleRelease` succeed? | **Yes** — `BUILD SUCCESSFUL in 24s` |
| Are all five book entries present in the APK? | **Yes** — 5 of 5 |
| Is each packaged entry byte-identical to its committed source? | **Yes** — 5 of 5, `cmp` exit 0 |
| Was R8 minification actually active? | **Yes** — `minifyReleaseWithR8` ran; `mapping.txt` emitted |
| Was resource shrinking actually active? | **Yes** — `convertShrunkResourcesToBinaryRelease` ran |
| **Assumption A5** | **RESOLVED — retention CONFIRMED** |
| Escape hatch (Kotlin string constants) needed? | **No** |

---

## 1. The release build

### 1a. The D-09 ad-ID gate fired first — and how it was satisfied

The first invocation failed at configuration time in ~1 s, exactly as Phase 6's D-09 gate
intends:

```
$ ./gradlew :app:assembleRelease
* What went wrong:
Release build requires real ad/Play Games IDs.
Missing: ADMOB_APP_ID
Set it in local.properties (untracked) or as the ADMOB_APP_ID environment variable.
Missing: ADMOB_REWARDED_AD_UNIT_ID
Missing: ADMOB_INTERSTITIAL_AD_UNIT_ID
Missing: PGS_PROJECT_ID
Debug builds use Google test IDs and need no configuration.

BUILD FAILED in 1s
```

**This is recorded rather than glossed over, because "the release gate fired and was satisfied
with placeholders for a local measurement" is exactly the kind of step that must not be
invisible.** The wording is Phase 6's own, from `06-BUILD-EVIDENCE.md` § APK size ledger, and
the method here is identical to the one that produced ledger **row 3**: throwaway, all-zero,
**non-functional** IDs (`ca-app-pub-0000000000000000~0000000000` and friends) placed in the
untracked, **gitignored** `local.properties` — never committed, incapable of serving an ad or
reaching a PGS project.

- `git check-ignore -v local.properties` → `.gitignore:7:local.properties` (confirmed before use).
- Real ad/PGS IDs still do not exist; the external prerequisites remain open.
- These IDs reach the APK only as four short strings in `BuildConfig` and `resources.arsc`, so
  they cannot move the byte count meaningfully and do not weaken the size figure below.

**This is a packaging check, not a shippable artifact.** The APK is unsigned and carries
placeholder ad IDs. It is a verification artifact only — the same status Phase 7's shrink-proof
APK carries in STATE.md.

### 1b. The build

```
$ ./gradlew :app:assembleRelease --console=plain
> Task :app:minifyReleaseWithR8
> Task :app:convertShrunkResourcesToBinaryRelease UP-TO-DATE
> Task :app:optimizeReleaseResources UP-TO-DATE
> Task :app:packageRelease UP-TO-DATE
> Task :app:verifyReleaseAdIds
> Task :app:assembleRelease

BUILD SUCCESSFUL in 24s
70 actionable tasks: 3 executed, 1 from cache, 66 up-to-date
```

**Offline caveat (Phase 6 D-18) did not bite.** `assembleRelease` is not offline-safe —
`isMinifyEnabled` activates `produceReleaseComposeMapping`, which resolves the implicit,
uncatalogued `org.jetbrains.kotlin:compose-group-mapping:2.3.20`. This machine's Gradle cache
was already warm from Phase 6, so no online warm run was needed and **no `### Release build
BLOCKED` finding applies**. `produceReleaseComposeMapping` and `reportReleaseComposeMappingErrors`
both ran green.

**Minification and shrinking were genuinely active**, not skipped:

```
$ ls app/build/outputs/mapping/release/
configuration.txt  mapping.prt  mapping.txt  resources.txt  seeds.txt  usage.txt
```

---

## 2. The packaged entries

These are plain **JAR resources** (`:domain` is a plain JVM module, so its
`src/main/resources` are merged into the APK by `mergeReleaseJavaResource`), **not** Android
`res/` entries. They are therefore invisible to the Android resource table — which is both why
`aapt2` cannot see them and why `isShrinkResources` has no remit over them:

```
$ aapt2 dump resources app-release-unsigned.apk | grep -c "golden/book"
0
```

That `0` is the mechanism behind the retention, stated as a measurement rather than an
expectation. Inspecting the APK as a zip instead:

```
$ unzip -v app/build/outputs/apk/release/app-release-unsigned.apk | grep -E "Length|golden/book"
 Length   Method    Size  Cmpr    Date    Time   CRC-32   Name
   11344  Defl:N     2720  76% 01-01-1981 01:01 15c17167  golden/book/asymmetric39-k4.txt
    9267  Defl:N     2198  76% 01-01-1981 01:01 8369e6c4  golden/book/diamond41-k4.txt
    8754  Defl:N     2175  75% 01-01-1981 01:01 071a7f9a  golden/book/english33-k4.txt
   14664  Defl:N     3407  77% 01-01-1981 01:01 e89bb8ed  golden/book/european37-k4.txt
    3590  Defl:N     1128  69% 01-01-1981 01:01 745b7527  golden/book/wiegleb45-k3.txt
```

| # | Packaged entry | Uncompressed (B) | Compressed (B) | Ratio |
|---|----------------|-----------------:|---------------:|------:|
| 1 | `golden/book/english33-k4.txt` | 8 754 | 2 175 | 75 % |
| 2 | `golden/book/european37-k4.txt` | 14 664 | 3 407 | 77 % |
| 3 | `golden/book/asymmetric39-k4.txt` | 11 344 | 2 720 | 76 % |
| 4 | `golden/book/diamond41-k4.txt` | 9 267 | 2 198 | 76 % |
| 5 | `golden/book/wiegleb45-k3.txt` | 3 590 | 1 128 | 69 % |
| | **Total** | **47 619** | **11 628** | **76 %** |

The 47 619 B uncompressed total matches `08-08-SUMMARY.md`'s recorded book size exactly.

**On the file names.** Four files carry a `-k4` suffix; the fifth is **`wiegleb45-k3.txt`**,
because Wiegleb-45 ships at depth **K=3** by the developer's decision at plan 08-08's Task 3
checkpoint, and the emitter derives the suffix from `policy.depth` so the name cannot
misrepresent the coverage. Any check written against a uniform `-k4\.txt` pattern will find
four files, not five, and that is the artifacts being honest rather than a defect. See the
deviation recorded in `08-11-SUMMARY.md`.

---

## 3. Byte comparison — the payload, not just the id

Phase 7 learned that **a surviving resource id is a weaker claim than a surviving payload**
(STATE.md carries this as a recorded lesson: `isShrinkResources` can keep an id while replacing
what it holds). So each packaged entry was extracted and compared byte-for-byte against its
committed source:

```
$ unzip -p <apk> golden/book/english33-k4.txt   | cmp - domain/src/main/resources/golden/book/english33-k4.txt   ; echo $?
0
$ unzip -p <apk> golden/book/european37-k4.txt  | cmp - domain/src/main/resources/golden/book/european37-k4.txt  ; echo $?
0
$ unzip -p <apk> golden/book/asymmetric39-k4.txt| cmp - domain/src/main/resources/golden/book/asymmetric39-k4.txt; echo $?
0
$ unzip -p <apk> golden/book/diamond41-k4.txt   | cmp - domain/src/main/resources/golden/book/diamond41-k4.txt   ; echo $?
0
$ unzip -p <apk> golden/book/wiegleb45-k3.txt   | cmp - domain/src/main/resources/golden/book/wiegleb45-k3.txt   ; echo $?
0
```

| Packaged entry | `cmp` vs committed source | Verdict |
|----------------|---------------------------|---------|
| `english33-k4.txt` | exit 0, no output | **byte-identical** |
| `european37-k4.txt` | exit 0, no output | **byte-identical** |
| `asymmetric39-k4.txt` | exit 0, no output | **byte-identical** |
| `diamond41-k4.txt` | exit 0, no output | **byte-identical** |
| `wiegleb45-k3.txt` | exit 0, no output | **byte-identical** |

**5 of 5 byte-identical.** Neither R8 nor the resource pipeline altered a single byte of any
book file, and each entry's headers (`entries=`, `proven-dead=`, `DOCUMENTED GAP`) therefore
still describe the payload shipped beside them.

---

## 4. APK size cost

| Ledger row | Configuration | APK (bytes) | MiB |
|---|---------------|------------:|----:|
| 3 (Phase 6, 2026-08-27) | Post-R8, SDK artifacts on the graph, unreferenced by code | 1 983 331 | 1.89 |
| **4 (this plan, 2026-08-30)** | **Row 3 + all of Phase 8's `:domain` work, including the book** | **2 069 301** | **1.97** |
| | **Δ vs row 3** | **+85 970 B (+4.33 %)** | |

```
$ stat -f %z app/build/outputs/apk/release/app-release-unsigned.apk
2069301
```

**The delta is NOT all book, and it would be wrong to report it as such.** Decomposing it
honestly:

| Component | Bytes | Share of delta |
|-----------|------:|---------------:|
| The five book resources, as packaged (deflated) | **11 628** | 13.5 % |
| Everything else Phase 8 added below `:domain` — `SolverTables`, the bitboard `Solver`, `OpeningBook`, `BoardCanonical`, target-aware `Pagoda`, per-board star bands — plus zip overhead | 74 342 | 86.5 % |

So **the book's own cost in the shipped APK is 11 628 B — about 11 KB, or 0.56 % of the APK.**
That is roughly a quarter of what a single `okhttp3` public-suffix table already costs this
build (41 394 B, per `06-BUILD-EVIDENCE.md`), and it buys the truthfulness of every hint on the
five Classic boards. Compression is doing real work: 47 619 B of source text ships as 11 628 B.

Phase 14 should continue to compare against **row 3** for SDK-integration growth, and against
**row 4** for anything landing after Phase 8.

---

## 5. Assumption A5 — resolved

> **A5 (RESEARCH):** `:domain`'s `src/main/resources` survive R8 and `isShrinkResources` into
> the packaged release artifact.

**RESOLVED — CONFIRMED, and confirmed at the stronger payload level rather than the id level.**

All five book resources are present in a minified, resource-shrunk release APK and every one is
byte-identical to its committed source. The mechanism is now measured rather than reasoned:
`aapt2 dump resources` returns **0** matches for `golden/book`, confirming these are plain JAR
resources outside the Android resource table and therefore outside `isShrinkResources`' remit;
they arrive via `mergeReleaseJavaResource` instead. R8 minified the code in the same build
(`mapping.txt` emitted), and touched none of it.

**The documented escape hatch — moving the book to Kotlin string constants in `:domain` main,
ugly but bulletproof — is NOT needed and was NOT implemented.** It is recorded here only so a
future reader knows what the fallback would have been had this check failed.

### What this proves, and what it does not

- **Proven here:** the bytes are in the shipping artifact, unaltered.
- **Not proven here:** that the decoder reads them *on a device*. This evidence is a packaging
  check run on the JVM-side artifact. Runtime decode is covered from two other directions:
  `./gradlew :domain:depthSweep` exercises `OpeningBook` against the same resources on the
  classpath across all 2 329 swept positions (plan 08-10), and the on-device audit in this
  plan's Task 3 confirms a book-served hint on real hardware. The three together — bytes
  shipped, decoder correct, hint visible — are what close the loop.

---

## 6. Compliance with Phase 6 D-16 (no signing, no committed artifact)

| Check | Command | Result |
|-------|---------|--------|
| No signing config committed | `grep -c 'signingConfig' app/build.gradle.kts` | **0** |
| No APK or keystore staged | `git status --porcelain` | **empty** |

The APK is `app-release-unsigned.apk`, as expected. `app/build/` is gitignored, and the
throwaway ad IDs live only in the gitignored `local.properties`. Real signing and a signed
track download remain Phase 14 (REL-02).

**Cleanup verified**, as Phase 6 did: the throwaway IDs were removed from `local.properties`
immediately after the packaging check, and a follow-up `:app:assembleRelease` fails again in 1 s
naming all four keys —

```
Release build requires real ad/Play Games IDs.
Missing: ADMOB_APP_ID
Missing: ADMOB_REWARDED_AD_UNIT_ID
Missing: ADMOB_INTERSTITIAL_AD_UNIT_ID
Missing: PGS_PROJECT_ID
BUILD FAILED in 1s
```

so the D-09 gate is re-armed and cannot be satisfied by leftovers from this measurement. The
main checkout's `local.properties` was never modified (this work ran in a worktree; its copy
holds `sdk.dir` only).

---

*Phase: 08-assist-generator-quality-debt · Plan 08-11 Task 1 · 2026-08-30*
