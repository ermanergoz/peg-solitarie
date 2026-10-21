# release-manager report — phase 14 external and release track (planning)
Date: 2026-09-20
Branch: feature/phase-14-release-plan (f9d6ef9..HEAD; cut from `develop`, nothing merged, nothing pushed, `main` untouched)
Skills loaded: ergoz-soft:standards (role: release-manager) → plugin copies **v0.5.2** of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`, `kotlin-android.md`, `compose.md`, `ip-policy.md`, `testing.md` (the repo snapshot `.claude/rules/VERSION` is **0.5.1** and therefore stale); `.claude/rules/lessons.md`. `gitflow` skill mechanics applied per rules/gitflow.md overrides; `gsd-ship` not invoked — no PR, no merge and no push is in scope for this task.
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- Phase 14 is **not** blocked on the item the ROADMAP names as the milestone's biggest schedule
  risk: the developer website already exists and already serves a live privacy policy. It is
  blocked on a different, cheaper set — and on one item nobody had listed.
- The single hard stop is **no signed artifact can exist on this machine**: two fail-fast Gradle
  gates need the founder's real AdMob IDs, and no `signingConfig` exists in the build at all
  (D-16 deferred it here).
- The live privacy policy is dated **2026-09-02** — before phases 12 and 13 merged — so it cannot
  name the Play Games and Saved Games data flows. REL-04 is not "already done from phase 9"; it
  needs a revision, and REL-01 must be written from the revised text.
- **VERSION stays 3.0.0**; versionCode moves 3 → 30000 per the studio formula. Tag `v3.0.0`,
  branch `release/3.0.0`.
- Two studio-side release blockers found that are not founder-dependent: `THIRD_PARTY.md` does not
  exist (release gate reads it), and `check` has no fuzz harness or coverage gate, so "full check
  at release fuzz count" cannot be honoured at all.

## Outputs
- `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` — founder-facing checklist,
  11 steps ordered by lead time, each with what it blocks, how long it takes and how to tell it is
  done; includes the Play App Signing / PGS credential story read against D-21.
- `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` — version decision with reasoning,
  release-branch procedure and its two preconditions, two-layer changelog approach, signed-build
  procedure (what is missing from the build and gets added), the T-0..T-13 internal-track
  verification script, the `THIRD_PARTY.md` inventory, the blocked/parallel split, and five
  founder questions in FOUNDER.md format.
- `.planning/phases/14-release-readiness/14-COLD-START.md` — the phase 6 baseline located and
  quoted, four properties of it that constrain the comparison, the four-condition measurement
  procedure, and a three-threshold budget with an attribution rule for a miss.
- `.planning/reports/phase-14/release-manager-plan.md` — this report.

## Checks
- `git checkout -b feature/phase-14-release-plan develop` — pass (branched from `develop` at
  `f9d6ef9`; `git status --short` clean before the writes; `main` never checked out)
- `cat VERSION` — **3.0.0**; `app/build.gradle.kts` → `versionCode = 3`, `versionName = "3.0.0"`,
  `applicationId = "com.erman.pegsolitarie"` — pass (the two agree on the name, disagree with
  rules/play-store.md's versionCode formula, which is what Q-F1 resolves)
- `git tag -l` — **`v1.0`** only. The published Play build is versionCode 2 / versionName "2.0",
  i.e. a different application — so milestone v1.0 of this repo was tagged and never uploaded
- `grep -c signingConfig app/build.gradle.kts build-logic/src/main/kotlin/android-app.gradle.kts`
  — **0 and 0** (D-16 confirmed: no release signing exists anywhere in the build)
- `ls scripts docs/store THIRD_PARTY.md CHANGELOG.md docs/release-checklist.md` — **all five
  missing**. `scripts/` does not exist at all, so the gated `scripts/play-upload.sh` the studio's
  Play rules assume is absent
- `grep -rn kover build-logic/ app/build.gradle.kts gradle/libs.versions.toml | wc -l` — **0**;
  `grep -rln FUZZ_SEEDS build-logic/ app/ domain/ | wc -l` — **0** (no coverage gate, no fuzz
  harness; `FUZZ_SEEDS=100000 ./gradlew check` would run an ordinary `check`)
- `git rev-list v1.0..develop --count` — **647**; by conventional type: 279 `docs`, 132 `feat`,
  105 `test`, 72 `chore`, 35 `fix`, 5 `refactor`, 3 `style`, 1 `perf`, 1 `build`, 11 merges
  (this is why the changelog is two layers and hand-edited, not generated)
- `06-BUILD-EVIDENCE.md` § *Cold-start baseline* — **present and complete**: `Pixel_9a`, API 36,
  **debug**, `am start -W` ×10, first discarded, 9 recorded, min 603 / median **633** / max 649,
  raw samples listed, explicit "same AVD, same host" caveat. Cross-confirmed in `06-08-SUMMARY.md`
  and `06-VERIFICATION.md` row 8
- `git log -S "libs.ads.mobile.sdk" -- app/build.gradle.kts` — SDKs joined the graph at **`3921135`**
  (06-09), parent **`33c2d0d`** — pass (D-14's ordering held: the baseline is genuinely pre-SDK,
  and `33c2d0d` is the recovery comparand if `Pixel_9a` is ever lost)
- `09-DEVICE-EVIDENCE.md` item 7 / `09-VERIFICATION.md` row 1a — **REL-05 PASS**, the policy page
  opened on a device headed *"Privacy Policy — Last updated: September 2, 2026"*; `PrivacyLinks.kt`
  holds the canonical HTTPS URL as a `const val`. Pass, and it is the evidence that the ROADMAP's
  "developer website does not exist" line is stale
- `find app/src/main/res -type f \( -name '*.png' -o -name '*.webp' -o -name '*.jpg' \) | wc -l`
  — **0**; launcher icon and cloud-save cover are vector XML (so the only art with no recorded
  provenance is Console-side: the 20 achievement icons, the listing icon, the feature graphic)
- `docs/audio/CREDITS.md` — read: 12 files, all **CC0 1.0**, 9 Kenney + 3 freesound borrows, with a
  per-file provenance table. Pass — `THIRD_PARTY.md` references it rather than duplicating it
- **Not run, deliberately:** `./gradlew check`, any `assemble*`/`bundle*` task, anything touching
  adb or an emulator, any git merge/push/tag. This task is planning; the release branch is not cut
  and `develop` is unchanged. No file outside `.planning/phases/14-release-readiness/` and
  `.planning/reports/phase-14/` was written

## Open issues / risks
- **`Pixel_9a` is both the PLAT-08 comparand and the wedged Saved Games control.** Clearing GMS
  data on it, wiping it or deleting it destroys the only valid pre/post cold-start comparison in
  existence. Nobody had connected those two facts. Mitigation written into `14-COLD-START.md` §1.4
  as a sequencing rule, plus a recovery path via a `33c2d0d` worktree if it is lost anyway.
- **REL-04 looks done and is not.** The policy is live, the URL resolves, the Data-safety form will
  accept it — and the text predates two phases of new data flows. This fails silently in every
  system that can be checked from a console.
- **`check` is weaker than the release rule it will be measured against.** A game whose stated core
  value is "always-solvable" has never had that claim tested at 100,000-seed scale; solvability
  rests on five frozen golden-master levels plus a depth sweep. Q-F2 routes the decision; the
  studio-side work is scoped in `14-RELEASE-PLAN.md` §2a and is wiring, not new algorithms.
- **`THIRD_PARTY.md` blocks the release by rule** and three of its rows cannot be filled by the
  studio (Console-only art). Q-F3.
- **Store listing content is unverified in every particular.** The studio has never seen it; it was
  written for a different application. Recorded as unverified with the exact Console path, never
  guessed at.
- **AAB vs APK:** whether this pre-2021 listing would still accept an APK is unverified and was
  deliberately not resolved — the AAB is the right artifact for a Play-App-Signing enrolment either
  way, and `bundleRelease` already carries both ID gates. `CLAUDE.md`'s Build command names
  `assembleRelease` and is updated on the release branch.
- No Play Console value, account id or email appears in any file written by this task; the AdMob
  publisher ID is given as a placeholder with the Console path to find it, and the developer domain
  is referenced through `PrivacyLinks.kt` rather than restated where it could be avoided.

## Handoff
**Next: producer.** Four dispatches, three of which are parallel and none of which waits on the
founder:

1. **producer → FOUNDER.md**: paste Q-F1..Q-F5 from `14-RELEASE-PLAN.md` § *Founder questions*, and
   hand the founder `14-OPERATOR-CHECKLIST.md`. Steps 0–4 have latency and should start today.
   Q-F1 blocks cutting `release/3.0.0`; the rest block nothing the studio can do meanwhile.
2. **tech-lead → android-game-dev**: scope and build the `FUZZ_SEEDS` harness + `kover`/
   `koverVerify` into `check` (`14-RELEASE-PLAN.md` §2a), and the §2b carry-ins the phase-13
   sign-off made v1.1 conditions — FU-3 "Next level", the L-3 debug source-set split, FU-2's six
   doc corrections, and `docs/tech-design.md`.
3. **qa-engineer**: run the cold-start measurement now, per `14-COLD-START.md` §3, conditions C1,
   C2 and C3. It needs no ad IDs and no keystore, and §1.4's sequencing rule means running it early
   is worth more than running it tidily. Also write `14-HUMAN-UAT.md` from the T-0..T-13 script with
   empty `result:` lines.
4. **release-manager (me, next dispatch)**: author `THIRD_PARTY.md` (every row but the three
   Console-art ones), `CHANGELOG.md`'s 3.0.0 section, `docs/release-checklist.md`, `docs/store/`
   texts, the `PLAY_UPLOAD_*` signing config and `scripts/play-upload.sh` — all of which can be
   written and reviewed before any founder item lands, and none of which can execute until
   checklist Steps 6 and 7 do.

Nothing in this task publishes, uploads or deploys anything, and
`.planning/approvals/publish-3.0.0.approved` does not exist and was not created.
