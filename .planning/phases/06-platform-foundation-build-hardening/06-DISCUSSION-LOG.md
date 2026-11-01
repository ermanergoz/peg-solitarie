# Phase 6: Platform Foundation & Build Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-27
**Phase:** 6-Platform Foundation & Build Hardening
**Areas discussed:** AdGateway result shape, Debug vs release SDK binding, Ad/game ID secrets handling, R8 rigor and CI gating

---

## AdGateway result shape

### Q1 — How should the rewarded-ad seam express its outcomes?

| Option | Description | Selected |
|--------|-------------|----------|
| Sealed result type | `sealed interface RewardOutcome { Granted, Declined, Unavailable }` — compiler forces the ADS-03 fail-open path to be handled; matches the existing `GameStatus` idiom | ✓ |
| Boolean + separate availability query | Keep `Boolean`, add `isAdAvailable()` — smaller diff, but fail-open logic lives per call site and can drift | |
| Boolean + thrown exception | Return the grant, throw on unavailable — makes the expected 5–15% no-fill path an exception | |

**User's choice:** Sealed result type
**Notes:** Driven by ADS-03 — the seam must distinguish "user declined" from "no ad available", because the second case still grants the assist.

### Q2 — Who decides whether the free per-level allowance is spent?

| Option | Description | Selected |
|--------|-------------|----------|
| Pure `:domain` policy object | An `AssistAllowance` in `:domain`; unit-testable with zero Android deps, mirrors `ScoreCalculator`/`DifficultyTiers` | ✓ |
| Inside AdGateway | Gateway returns Granted for the first N requests — buries a game rule in the platform layer | |
| In GameViewModel | Counters alongside existing move state — adds game-rule logic to the UI layer | |

**User's choice:** Pure `:domain` policy object
**Notes:** Recorded as binding on **Phase 9**, not built in Phase 6 — the allowance is ADS-02 scope. Captured here so Phase 9 can't place the logic elsewhere.

### Q3 — What should NoOpAdGateway do with the richer result in Phase 6?

| Option | Description | Selected |
|--------|-------------|----------|
| Always Granted — byte-identical v1.0 | Stub keeps auto-granting; opt-in dialog and allowance arrive in Phase 9 built against this stub | ✓ |
| Configurable stub | Outcome settable per-test/per-build so Declined/Unavailable get exercised early — but changes debug behavior inside a no-behavior-change phase | |

**User's choice:** Always Granted — byte-identical v1.0

---

## Debug vs release SDK binding

### Q1 — What should a debug build do for ads once real ads land?

| Option | Description | Selected |
|--------|-------------|----------|
| Google test ad units | Test IDs in debug, real in release — real ad flow including no-fill, zero ban risk | ✓ |
| Keep No-Op stubs in debug | Debug never touches the ad SDK — fastest builds, but the ad path is only exercised in release | |
| Real ads in debug too | Identical everywhere — Google warns this gets accounts suspended for invalid traffic | |

**User's choice:** Google test ad units

### Q2 — How should the debug/release split be implemented?

| Option | Description | Selected |
|--------|-------------|----------|
| Build types | `debug`/`release` with different `buildConfigField`s — no variant explosion, no new source sets | ✓ |
| Product flavors | A dedicated ad-config dimension — more flexible, but doubles the variant matrix and complicates the Room schema asset wiring | |
| Source-set binding swap | Per-source-set Hilt modules — powerful, but a known source of confusing build errors | |

**User's choice:** Build types
**Notes:** The project already uses a debug-only source-set tweak for Room schema assets; keeping the variant matrix at two protects that wiring.

### Q3 — How should tests be kept SDK-free (PLAT-07)?

| Option | Description | Selected |
|--------|-------------|----------|
| Remove providers via manifest merge | `tools:node="remove"` on auto-init providers + explicit init after consent — also what CONS-02 requires anyway | ✓ |
| Robolectric config exclusion | Leave providers, disable per-test — every new test file must remember the annotation | |
| Verify empirically first | Spike whether Robolectric 4.16 instantiates them, then decide | |

**User's choice:** Remove providers via manifest merge
**Notes:** Chosen partly because it double-serves: test isolation *and* the CONS-02 requirement that no ad request occurs before consent resolves.

---

## Ad/game ID secrets handling

### Q1 — Where should real AdMob and Play Games IDs live?

| Option | Description | Selected |
|--------|-------------|----------|
| Untracked `local.properties` + CI secret | Repo stays clean and forkable; a fresh clone builds with test IDs | ✓ |
| Committed to the repo | Simplest — any clone builds a working release, but anyone with the repo can point ads at the account | |
| Committed but release-gated | Build fails on placeholders, but the IDs are still published | |

**User's choice:** Untracked `local.properties` + CI secret
**Notes:** Framed during discussion as: these aren't cryptographic secrets (they ship in the APK) — the risk is a forked repo serving ads against this AdMob account.

### Q2 — What happens when a release build has no real IDs?

| Option | Description | Selected |
|--------|-------------|----------|
| Fail the build | Fail fast at configuration time naming the missing property — matches the `requireNotNull` idiom | ✓ |
| Fall back to test IDs with a warning | Convenient, but a warning in a long Gradle log is what gets missed before a store upload | |

**User's choice:** Fail the build

### Q3 — Where should the `INTERNET` permission be added?

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 6, with the build baseline | Foundation phase leaves a build that can reach the network; inert until an SDK uses it | ✓ |
| Defer to Phase 9 with the first real SDK | Keeps Phase 6 strictly zero-change, but risks a confusing runtime failure | |

**User's choice:** Phase 6, with the build baseline
**Notes:** Surfaced by stack research — the app currently declares no permissions at all.

---

## R8 rigor and CI gating

### Q1 — How aggressive should R8 be?

| Option | Description | Selected |
|--------|-------------|----------|
| Full mode + resource shrinking | Best size reduction; Room/Hilt/serialization all publish consumer keep rules | ✓ |
| Minify only, no resource shrinking | Slightly safer, leaves size on the table | |
| Compatibility mode | Fewer optimizations, fewer surprises — rarely needed with modern libraries | |

**User's choice:** Full mode + resource shrinking
**Notes:** The project has no `buildTypes` block at all today, so release is currently unminified — and the ads SDK adds ~5.5 MB before transitives.

### Q2 — Should APK size become a CI gate?

| Option | Description | Selected |
|--------|-------------|----------|
| Record baseline, no hard gate yet | Measure pre-SDK size for Phase 14 to compare; avoids a noisy gate while SDKs land | ✓ |
| Hard CI gate with a ceiling | Strong bloat protection, but needs re-tuning at each SDK addition | |
| No size tracking | Rely on the Phase 14 cold-start budget instead | |

**User's choice:** Record baseline, no hard gate yet

### Q3 — How should the carried security advisories be closed?

| Option | Description | Selected |
|--------|-------------|----------|
| Bound at the boundary + regression tests | Clamp/validate at decode and route boundaries, each with a test proving forged values can't pass | ✓ |
| Bound at the boundary, no new tests | Faster, but the audit's finding was that these paths lacked proof | |
| Re-audit first | Re-derive the findings in case the Candy Clay redesign shifted the code | |

**User's choice:** Bound at the boundary + regression tests
**Notes:** WARN-02 = resume-snapshot footprint coordinates unbounded; WARN-03 = raw route `challengeLevel` reaching Results unclamped on the non-restore path.

---

## Claude's Discretion

- Naming and file placement of the sealed outcome type, Activity provider, and build-config plumbing.
- `ForegroundActivityProvider` mechanism — not discussed; research's `WeakReference` + `StateFlow` + `registerActivityLifecycleCallbacks` recommendation stands. The fixed constraint is that the Activity is reachable from platform code and nowhere else.
- `GamesService` extension shape (PLAT-03) — not discussed; constrained only by "achievements + stock UI intents, silent no-op degradation, Saved Games stays off the interface".
- How the extended purity gate expresses "gms/ump only inside `platform/`".

## Deferred Ideas

- `AssistAllowance` implementation — decided here, built in Phase 9.
- Rewarded opt-in dialog — Phase 9, built against the No-Op stub first.
- Interstitial cadence policy — Phase 10.
- Saved Games / cloud save — Phase 13, behind `ProgressRepository`.
- Cold-start budget enforcement — Phase 14; Phase 6 captures only the baseline.
- APK size gate — deliberately not adopted; revisit at Phase 14.
