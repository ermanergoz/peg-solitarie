# Phase 6 — Deferred Items

Out-of-scope discoveries logged during execution. Not fixed by the plan that found them.

---

## DEF-06-01 — Configuring release IDs in `local.properties` silently puts them in the DEBUG build

**Found by:** plan 06-09, Task 1, while satisfying D-09's release-ID gate in order to measure
APK ledger row 3.
**Owner:** the D-05 / D-08 contract (plans 06-07 and 06-10); needs a user decision.
**Severity:** high — the failure mode is a non-appealable AdMob account suspension.

**The contradiction, concretely.** Two committed tests in this repo encode **opposite**
expectations for the same input:

- `build-logic/src/test/kotlin/AdIdResolutionTest.kt:44-55` asserts that a value supplied in
  `local` **overrides** `AdIdDefaults` on the **debug** path
  (`resolve(local = mapOf(REWARDED to "rewarded…"), isRelease = false).rewarded` equals the
  configured value, not the test unit).
- `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt:26-40` asserts that every ad ID
  in the debug build starts with Google's test-publisher prefix `ca-app-pub-3940256099942544`.

Both pass today only because nobody has configured any ID. The moment a developer follows
`local.properties.template` — which is the documented, intended way to supply the release IDs —
`AdIdResolution.resolve(isRelease = false)` picks those **real** IDs up for debug too,
`BuildConfigIdsTest` goes red, and a debug build on a development device is pointed at live ad
units. Observed directly: with the four keys set, `./gradlew check` failed with three
`BuildConfigIdsTest` failures.

**Why this matters more than a red test.** D-05 says debug uses Google's test units *because*
"real ad units on a development device is what gets AdMob accounts suspended for invalid
traffic". D-08's governing phrase is "a fresh clone builds with test IDs and never accidentally
serves real ads". The current resolver honours that only for a clone that has configured
*nothing*; it stops honouring it at exactly the moment the developer does the thing the
template tells them to do.

**Why plan 06-09 did not fix it.** The debug-override behaviour is deliberate and explicitly
tested by 06-07, so changing it is a decision about that plan's contract, not a bug fix inside
06-09's scope. Two defensible resolutions exist and they are not equivalent:

1. **Debug ignores configured ad-unit IDs entirely** — `resolve(isRelease = false)` always
   returns `AdIdDefaults`. Strictly honours D-05; costs the ability to point debug at a real
   unit for a one-off manual check. `AdIdResolutionTest:44-55` would be inverted.
2. **Separate the key namespaces** — e.g. `DEBUG_*` keys, so overriding debug is possible but
   never a side effect of configuring release. Keeps both capabilities; costs four more keys in
   the template.

**Interim state:** unchanged. `local.properties` in this worktree was restored to `sdk.dir`
only after row 3 was measured, so `check` is green — but green here means "nobody has
configured the IDs yet", not "the contract holds".
