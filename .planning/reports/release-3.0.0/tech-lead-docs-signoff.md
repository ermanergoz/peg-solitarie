# tech-lead report — release-3.0.0: sign-off of three docs-only branches before the cut
Date: 2026-11-01
Branch: feature/release-3.0.0-prep (report only). Signed off, each off `develop` `d251c3b` (`develop` now `47cb7e0`, STATUS.md only since): `feature/tech-design-3.0.0-sync` `3aa7825`; `feature/release-3.0.0-prep` `d855c51`; `feature/ux-3.0.0-folds` `2c8b31b`
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The snapshots are 0.5.1/0.6.2 and the plugin is 1.0.0, so the plugin copies were read; studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose, stacks and testing were diffed against the snapshot and the differences read; puzzle-design and play-store are byte-identical; the plugin's dispatch.md; `.claude/rules/lessons.md`
Ran in parallel with: the code-auditor's loop-1 re-check of the same three branches (its report is not yet committed)
Ran on: claude-opus-5-5 at effort high (policy) *(corrected in the addendum below: it read "xhigh (policy)"; the founder directive of 2026-10-31 (1) in STATUS.md sets tech-lead dispatches to high)*. **Staffing deviation:** `agents/tech-lead.md` pins fable; founder answer Q-019 puts Fable-pinned roles on Opus until 3.0.0 is published. I authored none of the three diffs; every fact below was re-read in the trees, the cited reports or the installed hook
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **All three branches are technically sound, and none weakens a gate.** Each verdict is conditional on the code-auditor's loop-1 PASS for that branch. That loop covers the fixes made since loop 0: `6b2f0c0` on the tech-design branch, `d54659a` on the prep branch, and `2c8b31b` on the ux branch.
- **Build command → `./gradlew :app:bundleRelease`: approved.** It is gated exactly like `assembleRelease` (`app/build.gradle.kts:258-264`: both lifecycle tasks, plus `packageRelease` and `packageReleaseBundle`, depend on the three verify tasks). It writes the AAB the script uploads. No hook and no gsd config reads the line. The Check is untouched.
- **T-11 now matches the ruling, and I proved it on the installed gate.** Plugin 1.0.0's `publish-gate.sh`, run with no approval file, blocks all five forms T-11 allows, the exact T-6 command among them. It passes `/usr/bin/env …`, which T-11 forbids.
- **The 15e rules, the §16 closures and the §14.3 backlog are sound.** Row DT's deferral is the producer's recorded decision (`4135e2d`, on `develop` since `47cb7e0`). It leaves no gate weaker: a red release Check fails closed.
- Non-blocking items are below: four go to the next tech-design sync, three to the prep branch's next pass, and three to the release device pass, which must have them before it runs.

## The judgement, item by item

### 1. `feature/tech-design-3.0.0-sync` — the tech-design changes
- **§10's 15e rulings are sound and bind later sittings correctly.**
  - **Sittings of record.** F-1 passed at 25.5 ms against a floor of 18.5 ms, and colour-vision at −4.0 ms against a floor of 4.0 ms. Both are on `Pixel_9a_bench`, with arm A as the merge-base, as §10 requires. The code-auditor re-derived both.
  - **Forward rule.** A precondition VOID may be re-attempted on the same boot only for a cause off the device, with its own null control, and the earlier attempts named. A device-side cause, or any doubt, needs a new cold boot. This keeps the instrument's independence.
  - **Key clause.** "Shared" is read as "no persisted key that either arm does not know". It is checked by the key's count at both ends of the sitting and an empty `app/schemas` diff, and a sitting where the key was written is VOID. That test is falsifiable, not a reading by assertion.
  - **Bench for sittings only.** The one smoke exception ends with `pm clear` and is recorded. Sound.
  - **The merge-in patch-id rule.** The branch's own start-path patch must keep its `git patch-id --stable`, and every start-path file the merge-in brings must equal `develop`'s, already sat. That is a strict identity test, so the sitting rightly stands.
- **§16 closures (#4, #13, #18, #20, #24, #28, #30, #32): each closure names its merge and its sign-off, and each old wording is kept in a dated note.**
  - #18's mechanism change (the v35 pair replaces the v27 pair) agrees with the ux folds and with §10's start-path list.
  - #20 correctly stays open for the release-manager's half (checklist 3.5 and 4.6a).
  - #30 moves the notice half and the banner's actions-menu Dismiss to the release device pass. That is right, but READINESS §3 does not list them yet (Conditions, item 3).
  - #36 and #37 (E-1, no pre-commit hook) are true today: `.git/hooks` holds samples only and `core.hooksPath` is unset (re-read).
  - #38 (no Fable read of the script) is correctly not a merge or cut condition, because the Console route stays open.
- **§14.3, the 3.0.1 backlog.**
  - It is one list. Every item has a chain that ends at a tech-lead sign-off where code is involved.
  - Items 1 and 7 carry their own 15e question, because `ui/home/` and `ui/theme/` are start-path folders.
  - Item 3 correctly owes no sitting (`BoardCanvas.kt` is not start path), and its line numbers are now correct for `d251c3b` (`:592`/`:617`).
  - None blocks the cut.
- **Row DT and §14.3 item 13.**
  - The deferral is the producer's, cited by commit (`4135e2d`). That commit is on `develop` (merge `47cb7e0`), so the pointer resolves once this branch merges.
  - Deferring DT weakens nothing. The four wall-clock deadline tests stay in `:domain:test` and in the Check. A loaded host turns them red, never green (§8.6 item 3), and the operator rule forbids widening the deadline.
  - What DT's absence costs is a possible false red, paid with a re-run on a quiet host.
- **Decision (1) of the sync report** (the ten licences test classes on the v2 rule, accepted as built): agreed. They are test-only and green, they sit in one feature, and §14.2's migration absorbs them. The rule still binds every new class.
- **§13's release-host row.** fastlane (MIT) and shellcheck (GPL-3.0) are host tools that are never linked or shipped, so there is no copyleft escalation. That reading is correct under rules/ip-policy.md, whose matrix governs code and assets that ship.

### 2. The Build-command change and its consequences
- **Check against Build.**
  - The Check stays `./scripts/check.sh`. CI runs `check assembleDebug` (`ci.yml:78`), so nothing in the Check or CI reads the Build command.
  - The Build command needs the `PLAY_UPLOAD_*` secrets and the real IDs, exactly as `assembleRelease` did. QA's device passes have always built debug APKs instead (`audio-review/qa-engineer-integration.md:173`, `colour-vision/qa-engineer-blocked.md:109`), so no routine pass changes.
- **Hooks and gsd.**
  - `grep -rn 'Build command\|bundleRelease\|assembleRelease'` over plugin 1.0.0's `hooks/` finds nothing.
  - `.planning/config.json` has no `build_command`.
  - `scripts/check-product.sh:31` checks only that the line exists.
  - The publish gate judges `play-upload.sh` and `fastlane supply`, not Gradle. Building a bundle is not an upload.
- **Against the plugin's stack row.** The plugin's android-compose-game row names `assembleDebug`. This product has named a release task since phase 14, so this is a pre-existing deviation, not a new one. An AAB meets the contract's "installable or deployable artifact", and it matches release-manager step 7's "signed AAB". Information only.
- **The device-pass install route (READINESS D-1, bundletool).** It is the right choice: it tests the dex and resources Play receives and the splits Play serves, not a second build. Three facts must go into the pass before it runs (Conditions, item 2):
  - **(a) Signing.** `build-apks` with no `--ks` signs with `~/.android/debug.keystore`, so the installed APKs carry the debug signature, not the upload key's.
    - Play Games sign-in is tied to the registered certificate, so a sign-in failure there is expected and is not a 3.0.0 defect. Play Games is verified from the track download (checklist §5).
    - An existing install under another signature must be uninstalled first.
    - Passing `--ks` with the upload key is not an option: it would put the keystore passwords on a command line.
  - **(b) Device selection.** `--connected-device` fails or picks wrongly when two devices are up, and §10 reserves `Pixel_9a_bench` for sittings. Both bundletool calls must therefore name `--device-id=<serial>` of `ergoz_studio`, never the bench.
  - **(c) Real ad IDs.** The release build carries them (`docs/ad-testing-safety.md` §1). An emulator is an automatic test device (§2 there), so the pass is safe on an emulator. On a phone it is safe only after that phone is registered as a test device (§3 there). D-1 must say "emulator, or a registered test phone", and the never-tap-without-"Test Ad" rule applies.
- **No gate is weakened.**
  - The release-branch file scope is unchanged: checklist 2.6 becomes a read.
  - Checklist 4.3 now builds the artifact both routes upload.
  - Checklist 4.7, "exactly one AAB with 4.6's hash", is tighter than before.
  - D-1 writes its `.apks` file under `.planning/qa/`, never into `app/build/outputs/`, so 4.7 stays true.

### 3. The T-11 upload-run rule against `play-upload/tech-lead-signoff.md`
- **The ruling (`:106`):** "`./scripts/play-upload.sh`, with optional `VAR=value` or bare `env …` prefixes".
- **The checklist (1.7 and the §4 note)** now says the same. It adds "literally the word `env`, never `/usr/bin/env`" and forbids `/usr/bin/env`, `/usr/bin/nohup`, `/usr/bin/time`, `exec`, and any wrapper or shell named by its path. That is stricter than the ruling, never looser. The loop-0 must-fix is closed.
- **Probe (mine, offline, on the installed plugin 1.0.0 hook).** Each form below was fed as a PreToolUse payload with `ERGOZ_LOG=0` and `cwd` set to the prep worktree. `VERSION` is 3.0.0 there and no `publish-3.0.0.approved` exists. Nothing ran.

  | Form | Gate exit |
  |---|---|
  | `./scripts/play-upload.sh` | 2 |
  | `PLAY_SERVICE_ACCOUNT_JSON=/tmp/k.json ./scripts/play-upload.sh` (T-6's form) | 2 |
  | `env ./scripts/play-upload.sh` | 2 |
  | `env PLAY_SERVICE_ACCOUNT_JSON=/tmp/k.json ./scripts/play-upload.sh` | 2 |
  | `cd <checkout> && PLAY_SERVICE_ACCOUNT_JSON=… ./scripts/play-upload.sh` | 2 |
  | `/usr/bin/env ./scripts/play-upload.sh` (forbidden by T-11) | **0** (E-1, as ruled) |
  | `/bin/bash -n < scripts/play-upload.sh` (the checklist's syntax check) | 0 |
  | `bash -n < scripts/play-upload.sh` | 0 |

  So every form T-11 allows is caught by the gate, and the only passing form among the invocations is one T-11 forbids. The syntax check runs nothing, because `-n` only parses. The bare `bash -n <` form passes too, so the checklist could drop the path-named shell from its text (Conditions, item 6).
- **The other rules transcribe the sign-off's §6 faithfully:**
  - key ordering (T-1);
  - the hash pin (T-3 with T-5);
  - the exact command, run after probes P0–P7 (T-6);
  - the report without the `json_key` row (T-7);
  - TL-1.

  The operator checklist puts the key after the approval file. The store README no longer implies that the script uploads listings.

### 4. `feature/ux-3.0.0-folds`
- Its technical claims agree with tech-design §16 #18 and §10:
  - the v35 pair holds three items with a transparent `navigationBarColor`;
  - the v27 pair is deleted;
  - "below API 35";
  - the whole-minute fallback's strings, floored.
- The report now has a `Ran on:` line (loop-0 must-fix). Its form lacks "at effort …" (minor, below).

## Conditions and follow-ups
**Merge conditions:**

1. **The code-auditor's loop-1 PASS for each branch.** Each verdict below holds only with it.

**Before the release device pass runs** (the producer puts these in the QA dispatch, or the release-manager folds them into READINESS §3):

2. D-1's three facts from §2: (a) debug-signed APKs (an expected Play Games sign-in failure; uninstall a differently signed install first); (b) `--device-id` of `ergoz_studio`, never the bench; (c) emulator only, or a phone already registered as an AdMob test device.
3. Add §16 #30's notice half (an off-book Classic hint with TalkBack on: 23j item 5, 23d) and the banner's actions-menu Dismiss to READINESS §3. This is the producer's, as the sync report and the auditor asked.

**At the next pass of `feature/release-3.0.0-prep` or `release/3.0.0`** (release-manager, non-blocking):

4. **T-11's lapse.** The checklist says T-11 "holds until the plugin fix is installed here". The ruling says the opposite way round: the key-ordering rule lapses once the fix is installed, and T-11 stays as good practice (`tech-lead-signoff.md:108`). This loosens nothing before the fix, and the gate itself closes the gap after it. Still, the text should say what the ruling says.
5. **The quiet-host read.** While DT is deferred, write the quiet-host read into checklist 1.2's and 3.1's evidence: the 1-minute load ≤ 5.0 before and after, recorded, and a red above it re-run, never widened (§8.6 item 4).
6. **Cosmetic.** The syntax check may use the bare `bash -n < scripts/play-upload.sh` form, which keeps a path-named shell out of the text beside T-11's forbidden list. `14-RELEASE-PLAN.md:311` also has a run of spaces before "`CLAUDE.md`'s Build command".

**At the next tech-design sync** (the tech-lead; candy's close sync is next):

7. **§10's first-frame test: state its threshold.** The licences application exempts four destination registrations that do run at graph build, before Home's first frame. The source ruling (`licences-screen/tech-lead-signoff.md`, "The 15e reading") gave the reason: the cost is far below the instrument's resolution, with no read, decode, load, collector or SDK call. The tds text gives the facts but not that criterion, so the rule and its application read as contradictory.

    Write the threshold into the rule: a change whose first-frame work is constant-size registration only, with none of those operations, may be ruled below resolution at sign-off, with the reasoning recorded. That keeps it from reading as an unwritten exception.
8. **§8.6 item 3** still says "else in the first post-3.0.0 batch (§14.2)". Add a dated pointer to §14.3 item 13, as row DT now has.
9. **§13's release-host tools row:** add bundletool (Apache-2.0, Google). It is a host tool that checklist 4.6a and D-1 use, and it is never shipped.
10. **The ux report's and the release-manager reports' `Ran on:` lines** name no effort, or "(not named by the harness)". rules/reporting.md wants the agent file's pin marked `(policy)`. The lines exist, so the reports are valid. Their authors fix the form in their next pass.

**Merge order:** any order, serially, `--no-ff`. The three change disjoint files, and none touches `STATUS.md`. `git merge-tree` against `47cb7e0` exits 0 for each branch alone, and for all three merged in a row.

## Outputs
- `.planning/reports/release-3.0.0/tech-lead-docs-signoff.md` — this report, committed alone on `feature/release-3.0.0-prep`.

## Checks
- **Check command** (`./scripts/check.sh`) — not run, because the dispatch forbids Gradle. The three diffs are docs-only: `git diff --stat d251c3b..<head>` lists no file under `app/`, `domain/`, `build-logic/`, `scripts/` or `gradle/`.
- **Diffs read.**
  - `git diff d251c3b..3aa7825 -- docs/tech-design.md`: in full, plus a word diff for the long table rows.
  - `git diff d251c3b..d855c51`: CLAUDE.md, AGENTS.md, the release checklist, the store README, the release plan and the operator checklist.
  - READINESS, CHANGELOG-draft and the Q-PUBLISH draft: read in full.
  - `git diff d251c3b..2c8b31b`: the added lines.
- **Probe of plugin 1.0.0's `hooks/publish-gate.sh`** (the installed path in `~/.claude/plugins/installed_plugins.json`) — pass: 8 PreToolUse payloads, with results as in §3's table. The probe script lives in the scratchpad, run with `python3 -I`. No approval file was created or exists.
- `sed -n 255,264p app/build.gradle.kts` — pass: both `assembleRelease` and `bundleRelease` (and both package tasks) depend on `verifyReleaseAdIds`, `verifyReleaseGamesIds` and `verifyReleaseSigning`.
- `grep -rn 'bundleRelease\|assembleRelease\|Build command\|build_command'` over plugin `hooks/` — no hit. `grep build_command .planning/config.json` — no hit. `grep -n gradlew .github/workflows/ci.yml` — `check assembleDebug` only.
- `ls .git/hooks | grep -v sample` — empty. `git config --get core.hooksPath` — unset (exit 1). This confirms §16 #37.
- `git show 4135e2d` — the DT deferral line is present. `git log develop` shows it under merge `47cb7e0`.
- `git merge-tree --write-tree` — pass (exit 0) for each head against `47cb7e0`, and for the three heads merged serially.
- Cross-reads:
  - `play-upload/tech-lead-signoff.md` §5–§6 (T-11 at `:106`, the lapse at `:108`);
  - `licences-screen/tech-lead-signoff.md`, "The 15e reading";
  - `docs/ad-testing-safety.md` §0–§2;
  - §8.6 items 3–4.
- `.planning/log/` was not read.

## Open issues / risks
1. **E-1 stays open in the plugin** (§16 #36). Until its release is installed, T-11 and the key-after-approval order are the only cover against path-named wrappers. The probe above confirms both halves: the allowed forms are blocked, and `/usr/bin/env` passes.
2. **No pre-commit hook here yet** (§16 #37). Installing it is the producer's job and needs no plugin release.
3. **Rule 1 is met only in form.** Authors, auditor and signer all ran on Opus under Q-019. The mitigation is re-reading every fact and running my own probe of the gate.
4. **The release device pass list is incomplete** until Conditions 2 and 3 are folded in.

## Handoff
Next: producer —
- When the code-auditor's loop-1 report gives a branch PASS, merge that branch `--no-ff`. Each branch's verdict below is conditional on that report.
- Put Conditions 2 and 3 into the release device-pass dispatch, or route them to the release-manager for READINESS §3.
- Route Conditions 4–6 to the release-manager's next pass, and 7–9 to the tech-lead's next sync. Condition 10 goes to each report's author.

VERDICT (feature/tech-design-3.0.0-sync): PASS conditional on auditor loop 1
VERDICT (feature/release-3.0.0-prep): PASS conditional on auditor loop 1
VERDICT (feature/ux-3.0.0-folds): PASS conditional on auditor loop 1

## Addendum — the code-auditor's loop 1 (2026-11-01, after this report's first commit `84e7988`)
The loop-1 re-check landed on this branch as `b4e586a` (`.planning/reports/release-3.0.0/code-auditor-docs-loop1.md`) just before this report's commit. It gives **PASS** for all three branches: the prep branch's two must-fixes are fixed, the tech-design sync's three minors are fixed and insertion-only, and the ux report is valid. Its remaining minors are non-blocking: m1–m7, and i1–i3 for information. So Condition 1 is met, and each verdict above becomes an unconditional PASS.

The loop-1 report overlaps my follow-ups:
- Its m3 (D-1's wording: "to be uploaded", "the same code and resources", the uninstall, the real ad units) agrees with Condition 2 (a) and (c). Condition 2 (b), `--device-id` of `ergoz_studio` and never the bench, is still mine alone.
- Its m5 is the stray spaces of Condition 6.
- Its m4 is correct: a bare `exec` in front of the script is blocked by the gate. Only `exec` followed by a path-named shell passes. T-11 forbidding `exec` stays right, as stricter.
- Its i2 (tech-lead effort `high` under the founder directive) applies to this report's own `Ran on:` line, which is now corrected.

Conditions 2 and 3 remain due before the release device pass. Conditions 4–10 remain non-blocking.

VERDICT (feature/tech-design-3.0.0-sync): PASS
VERDICT (feature/release-3.0.0-prep): PASS
VERDICT (feature/ux-3.0.0-folds): PASS
