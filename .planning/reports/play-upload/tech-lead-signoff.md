# tech-lead report — play-upload sign-off
Date: 2026-10-31
Branch: feature/play-upload (report only). Signed off: `git diff develop...b70cac9`, code `9005fe3..6a0b032` (`scripts/play-upload.sh`, 131 lines, mode 100755, blob `259c9bb`, sha256 `31873d87…20747`); the commits after `6a0b032` add reports only
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.6.2 in this worktree and 0.5.1 in the main checkout, older than plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch, read in full; `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh. **Deviation:** `agents/tech-lead.md` pins fable at xhigh. The Fable limit is reached, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every fable-pinned role on opus until 3.0.0 is published. The author, the code-auditor and the qa-engineer also ran on opus, so no review of this script ran on a model above its author's; Q-019 is what covers that
Fan-out: none
Loops: 0
Read in full: `.planning/reports/play-upload/android-game-dev-loop0.md`, `android-game-dev.md`, `code-auditor-loop0.md`, `code-auditor.md`, `qa-engineer-loop0.md`, `qa-engineer.md`; `docs/tech-design.md` §9.1, §9.5–§9.7, §13, §14.1 (row PU, merge order 6c), §14.2
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

SIGN-OFF: PASS

## Summary
- **PASS.** The script meets row PU (§14.1) as written, and nothing in it contradicts the tech design. The code-auditor's loop-1 PASS (`.planning/reports/play-upload/code-auditor.md`) closes F-1 to F-4 of its loop-0 FAIL (`code-auditor-loop0.md`). QA's loop-1 review-mode PASS (`qa-engineer.md`; loop 0 `qa-engineer-loop0.md`) is the QA PASS rules/gitflow.md requires. The developer's Check on `6a0b032` is green, and no Gradle or CI input reads `scripts/`.
- **The merge needs no merge-in and no Check re-run.** Since the cut (`f7dd1fe`), `develop` has changed only `.planning/reports/STATUS.md`. A trial merge is clean, and it adds only the six reports and the script.
- **E-1 is real, and wider than QA found** (probed offline below). Any wrapper the gates know, named by its path (`/bin/bash`, `/usr/bin/env`, `/usr/bin/nohup`, `/usr/bin/time`), hides the command after it from the publish gate *and* from the branch guard. That includes `/usr/bin/env fastlane supply …`, with no script involved. It is a plugin defect that exists with or without this branch. So it does not hold this merge, but it does constrain how the script is used (below).
- Rulings: N-1 and N-2 are left for the script's next edit, with the exact text. Lint: option (c) for 3.0.0, and (a)'s shape recorded with its trigger. `check.sh:38` is fixed with that file's next edit, already scheduled. Before Gate 2 by the script route: T-1, T-2, T-3, T-4, T-5, T-6, T-7 and T-11, plus TL-1 and the key-ordering rule. The tech-design text this merge owes is listed, with who writes it and when.

## The sign-off

### Row PU, re-read against the committed blob
| Row PU says | Verdict | Where (`6a0b032`) |
|---|---|---|
| Runs `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON` | met | `:69`, `:120-131`, the key passed as a path, as supply's `json_key` takes it |
| Uploads the signed AAB of `:app:bundleRelease` | met | `:41` is AGP's default path. `PACKAGE_NAME` `:38` equals `applicationId` (`app/build.gradle.kts:290`). The signing is the build's job (§9.2.5), not the script's. Provenance is the procedure's (T-3) |
| To the `internal` track | met | `:39`, `:122`. Load-bearing, because supply's default track is production |
| Checks no approval itself | met | It reads no `VERSION` and no `.planning/approvals/`. `:63` moves to the repository that holds the script, after `unset CDPATH` (`:60`) |
| Fails with a message when the variable or the AAB is missing; echoes no secret | met | `:70-73`, `:94-97`, and seven more refusals, all through `fail`, exit 78, before anything is sent. No message interpolates `$key` |
| `shellcheck scripts/play-upload.sh` clean, and the code-auditor's read | met | Checks; `code-auditor.md` VERDICT PASS |
| No session runs it before Gate 2; nothing routes around the hook | met by this branch, **not enforced by the plugin** (E-1) | `.planning/approvals/` holds `concept.approved` only, and no release bundle exists in this worktree |

The branch adds choices the row leaves open: `completed`, the five skip flags, the `SUPPLY_*` unset, `--ack_bundle_installation_warning false`, the refusal of four config files, the absolute key path outside the checkout, the bash-only guard, the printed sha256, fastlane's analytics and update check switched off, exit 78, and no arguments. The auditor approved them, and I accept them on technical grounds. Each one narrows what can decide the upload to the one command line in the file, and each one fails closed. `--ack_bundle_installation_warning false` fails an AAB over Play's size warning instead of acknowledging the warning. That is the right direction for an unattended upload.

### Boundaries and dependency rules
- The branch touches no module, no Gradle file, no CI file and no shipped source set. `scripts/` is outside every Gradle input (the developer's, the auditor's and QA's `git grep`). So §9.1's single registration and the C7 warnings gate are untouched, and the green Check on `6a0b032` stands for this tree. `git diff --quiet 6a0b032 b70cac9 -- . ':!.planning'` exits 0.
- It adds no Gradle dependency. It invokes two host tools: fastlane 2.233.0 (MIT) and, at review, shellcheck 0.11.0 (GPL-3.0). Neither is linked into or shipped with the product, and nothing in the Gradle graph reaches either one. A GPL-3.0 tool run on the build host takes no product code under its licence, so this is **not** a copyleft escalation (rules/ip-policy.md's GPL row is about code taken into the product). §13 owes a row for both (item 7(d) below).

### Merge mechanics
- `git diff --stat f7dd1fe develop`: one file, `.planning/reports/STATUS.md`. `git merge-tree --write-tree develop b70cac9` is clean (tree `7c60e57`), and the trial merge differs from `develop` by the six reports and the script only.
- So the producer merges `--no-ff` with no merge-in. If `develop` gains a commit that touches `scripts/`, a Gradle or CI input, or this branch's report folder before the merge, the merge-in rule of §14.1 applies, and this sign-off re-reads its hunks.

## Decisions

### 1. PASS
SIGN-OFF: PASS, with no merge condition beyond what is above. The script's *use* at Gate 2 has its conditions in 5 and 6. They bind the release procedure, not this merge.

**Rule 1, recorded.** No review of this script ran on fable. It is the one file in the product that holds the Play credential's path and a store upload. When fable is available again, I recommend that the producer schedule a fable code-auditor read of `scripts/play-upload.sh` at `6a0b032` before its first script-route upload. That is not a merge condition: Q-019 governs, and checklist 1.7's Console route is always open.

### 2. N-1 and N-2: leave both now; fold them into the script's next edit
Another developer, auditor and QA loop on opus is a whole loop spent on wording and an edge that needs a deliberate operator act. Row PU blocks Gate 2, not the cut, and the procedure can close N-2's edge in the meantime (T-1 below). The script's next edit is likely soon: T-8's `--mapping`, if the bundle turns out to lack R8's mapping. Both changes ride that edit, or the first edit after 3.0.0, whichever comes first:
- **N-1, `:109`:** replace `# supply reads each option it is not given from its SUPPLY_* variable: a SUPPLY_ROOT_URL left in` with `# supply reads an option it is not given from its SUPPLY_* variable, when it has one: a SUPPLY_ROOT_URL left in`, re-flowed to 100 columns.
- **N-2, directly after the readable-file check `:81-83`:**

      if [ -L "$key" ]; then
          fail "PLAY_SERVICE_ACCOUNT_JSON names a symbolic link. Name the key file itself."
      fi

  With this, the header's "a key inside it is refused" (`:21`) holds for a symlinked key too. Until it lands, T-1's step says that the key is a regular file, not a link, outside every repository.

### 3. Lint the scripts: option (c) for 3.0.0; option (a) later, on a recorded trigger
- **(b) is rejected.** A CI-only step is a second registration of a gate, which §9.1 and §9.7 exist to prevent: CI runs `check` verbatim.
- **(a) is the right shape, but not now.** It would make shellcheck a required host tool for every Check, in every worktree and on CI, a week before the cut, after the Check's composition was signed off and measured (R-2). There are two scripts. Both are clean, neither is on a shipped path, and `play-upload.sh` is reviewed and run once per release under checklist 1.7's `shellcheck scripts/play-upload.sh`.
- **(c) holds for 3.0.0:** the code-auditor's read, plus checklist 1.7's shellcheck run.
- **The trigger for (a)** is the first edit of a `scripts/*.sh` after `release/3.0.0` is cut, or a third script, whichever comes first. The shape: one Gradle task that runs `shellcheck` over `scripts/*.sh`, with those files as inputs, registered where §9.1 says a gate is registered (the developer names the exact owner in that branch's brief). When shellcheck is missing, the task **fails** with an install hint; it never skips. A CI step that *installs* shellcheck, if the runner lacks it, is provisioning, like the JDK setup, not a second gate.

### 4. `scripts/check.sh:38`'s CDPATH-sensitive `cd`: fix it elsewhere, with that file's next edit
It fails closed. Under an exported `CDPATH`, `scripts/check.sh` exits 1 at `cd "$root"` (`:40`) before it takes the lock or starts Gradle, so it produces no build and no artifact, and no gate depends on its `cd`. Editing it here would put a Check-script change on a scripts-only branch and owe a Check re-run and a different review. The fix rides the edit §9.6 and §14.2 already schedule for `check.sh` after 3.0.0 (the Gradle child's PID in the lock, plus the INT comment): `unset CDPATH`, then `root="$(cd -- "$(dirname -- "$0")/.." && pwd)"`, the same idiom the auditor probed here.

### 5. E-1: how it bears on this merge, and the plugin fix

**What I probed.** Each form is a PreToolUse payload fed to the plugin 1.0.0 hook on stdin with `ERGOZ_LOG=0`. Nothing ran, and `cwd` was this worktree, with no approval present. Exit 2 means blocked.

| # | Form | Publish gate |
|---|---|---|
| 1 | `./scripts/play-upload.sh` | 2 |
| 2 | `bash ./scripts/play-upload.sh` | 2 |
| 3 | `/bin/bash ./scripts/play-upload.sh` | **0** |
| 4 | `/usr/bin/env ./scripts/play-upload.sh` | **0** |
| 5 | `/usr/bin/env bash ./scripts/play-upload.sh` | **0** |
| 6 | `/usr/bin/nohup ./scripts/play-upload.sh` | **0** |
| 7 | `/usr/bin/time ./scripts/play-upload.sh` | **0** |
| 8 | `exec /bin/bash ./scripts/play-upload.sh` | **0** |
| 9 | `bash -c "fastlane supply --track internal"` | 2 |
| 10 | `/bin/bash -c "fastlane supply --track internal"` | **0** |
| 11 | `/usr/bin/env fastlane supply --track internal` | **0** |

Branch guard (`hooks/branch-guard.sh`), with `cwd` a scratch repository on `develop`:

| Form | Guard |
|---|---|
| `git commit -m x` | 2 |
| `/usr/bin/git commit -m x` | 2 |
| `env git commit -m x` | 2 |
| `/usr/bin/env git commit -m x` | **0** |
| `/bin/bash -c "git commit -m x"` | **0** |

**Cause.** In `hooks/lib-cmdpos.sh:62`, `ERGOZ_CMD_POS` lists every wrapper word bare (`sudo`, `env`, `nohup`, `time`, `timeout`, `nice`, `caffeinate`, `xargs`, `sh|bash|zsh|dash|ksh`, `npx`, `yarn`, `bundle exec`, `pnpm`). `ERGOZ_CMD_PATH` (`:65`) applies only to the gated word that follows. So a path-named wrapper is not a command position, and the gated word after it sits at no position. `publish-gate.sh:44-49` says "through an optional path", so the code does not do what its own header promises. Both gates share the fragment, so both have the hole.

**Bearing on this merge: none that holds it.**
- The hole does not come from this branch. Row 11 shows `fastlane supply` going past the gate today, by hand, wherever fastlane and a key exist. The script makes the upload one line long. It adds no capability the hole does not already give.
- Before Gate 2, no service-account key exists on this Mac (T-1 is not done), and no release bundle exists in any checkout. So the script exits 78 at its key or bundle check, whichever shell runs it.
- Once `publish-<VERSION>.approved` exists, the gate allows the upload anyway, so for that version the hole no longer matters.

**What it binds instead: the script's use, until the plugin fix is released and installed here.**
- **T-11 stands.** The only invocation is `./scripts/play-upload.sh`, with optional `VAR=value` or bare `env …` prefixes.
- **Key ordering (tech-lead ruling).** The founder places the service-account key on this Mac only after `.planning/approvals/publish-<VERSION>.approved` exists in the checkout the upload runs from. Then no window exists in which a key, a release bundle and no approval sit on one host. This goes into T-1's step.
- When the plugin release that fixes E-1 is installed (the hooks run from the plugin cache, so this means the plugin version, not `sync-rules.sh`), the ordering rule lapses. T-11 stays as good practice.

**The plugin fix, for the producer to route to a plugin feature branch** (for example `feature/gate-path-wrappers`; the code is the plugin developer's, reviewed by the code-auditor and signed off by the plugin's tech-lead):
1. In `hooks/lib-cmdpos.sh`, give every wrapper word that exists as an executable the same optional path prefix as the gated word: `ERGOZ_CMD_PATH` in front of the wrapper alternation in `ERGOZ_CMD_POS`. That covers `sudo`, `ssh`, `command`, `time`, `timeout`, `nohup`, `nice`, `caffeinate`, `xargs`, `env`, `sh|bash|zsh|dash|ksh`, `npx`, `yarn`, `bundle`, `pnpm`, and zsh's `=bash`. Shell keywords and builtins with no path form (`if`, `exec`, `builtin`, `eval`, `coproc`) stay bare. Keep one definition, used by both gates.
2. The target resolver (`ergoz_targets` and its tracer) must recognise the same path-named wrappers, so that `/usr/bin/env -C <dir> ./scripts/play-upload.sh` is judged in `<dir>`, never in the caller's directory.
3. Tests, red first, for both hooks: every form in the two tables above, plus flagged forms (`/usr/bin/env -u X ./scripts/play-upload.sh`, `/usr/bin/sudo -u x …`, `/opt/homebrew/bin/bash ./scripts/play-upload.sh`, `=bash ./scripts/play-upload.sh`, `/bin/zsh -c "git commit -m x"`). Each must block without approval, or on `develop`. Plain reads such as `/bin/cat README.md` must still pass.
4. In the same branch, the two open items already reported against these files:
   - the developer's wording: "start with `cd "$(dirname "$0")/.."`" (`lib-cmdpos.sh:118`, `publish-gate.sh:58`) becomes "move to the repository that holds them before anything that depends on the working directory";
   - the auditor's loop-0 open issue 2: a bare-name `play-upload.sh` found through `PATH` is judged in the caller's directory. Block it as `U`, or resolve it through `command -v`.
5. After the release, the producer installs it here and records it in STATUS.md.

**One product-side gap the probe exposed.** rules/gitflow.md says that a git pre-commit hook backs the branch guard. This repository has none: the common `.git/hooks` holds only `*.sample` files, and `core.hooksPath` is unset. So `/usr/bin/env git commit` on `develop` or `main` passes both guards here. Installing the plugin's `branch-guard.sh --git-hook` as `pre-commit` closes the commit half of E-1 in this product today, independent of the plugin release. That is the producer's to route. I write no hook.

### 6. T-1 to T-11 and the Gate-2 script route
If 3.0.0 is uploaded with the script, these items must be on `develop` before Gate 2's upload step. They are the release-manager's texts, routed by the producer:

| Item | Before Gate 2? | Why |
|---|---|---|
| T-1: provision the key | **must** | Without it, every run ends at P2. Add three things: the key is a regular file, not a link (N-2); it is outside every repository; and it is placed only after the approval file exists (E-1's ordering) |
| T-2: 1.7 and the §4 note | **must** | 1.7 is the Gate-2 item, and its "Today it does not exist" is false once this merges. Use the as-built contract QA lists, and the merge commit |
| T-3: pin the bundle to 4.6's hash | **must** | The script uploads whatever sits at `:41`. The comparison before the real run (P7 or `shasum`) is the only provenance check |
| T-4: 4.3 names `:app:bundleRelease` | **must, on both routes** | The Console route needs the AAB too. As written, 4.3 can build an APK |
| T-5: 4.7, exactly one AAB with 4.6's hash | **must** | It is T-3's other half |
| T-6: 4.8's exact command, after P0–P7 | **must** | It is the command the founder approves |
| T-7: the upload report without the `json_key` row | **must, with T-6** | It binds the committed upload report, which is written at Gate 2 |
| T-11: `./scripts/play-upload.sh` only | **must** | E-1's stopgap |
| T-8: the mapping check in 4.5 | recommended, not gating | A missing mapping degrades crash reports and can be added in the Console. If it is missing, `--mapping` is a script change (and the edit that carries N-1 and N-2) |
| T-9: release plan item 2 | not gating | It is a planning record. Correct it in the same pass as T-2 |
| T-10: `docs/store/README.md` | not gating | Following the README as written makes the script refuse (fails closed). Correct it with T-2 |

**TL-1 (tech-lead ruling, new; it goes with T-6).** If fastlane fails at Gate 2 in a way that would need an option the script does not pass (for example Play refusing to commit the edit until `changes_not_sent_for_review` is set), the fix is a script change on a feature branch, through the developer and the code-auditor. It is never a hand-typed `fastlane supply` command. A hand-typed command would bypass every isolation the script adds (the `SUPPLY_*` unset, the config-file refusal, the pinned options), even with the approval in place.

**On the Console route.** The producer records the Console route under 1.7, and T-4 still applies. T-2 should land anyway, because 1.7's text is false after this merge, but it does not gate. The release report says that the script stays unverified until its first upload (QA's loop-0 note).

### 7. The tech-design text this merge owes
**Who and when.** The tech-lead writes it after the producer's `--no-ff` merge, because the text needs the merge commit. It goes on a docs-only branch cut from `develop` (`feature/tech-design-pu-close`), or it rides the next tech-lead docs-only close the producer schedules, whichever comes first. The code-auditor reads it as text and a fresh tech-lead signs it off, as for `feature/tech-design-rm-close`. It is due before checklist 1.7 is ticked at Gate 2, not before the cut: row PU carries no cut marker. It does not ride this branch, which carries no docs.
- **(a) Row PU, last cell:** **Done.** Signed off PASS (this report), on the code-auditor's PASS (`play-upload/code-auditor.md`; FAIL at loop 0, `code-auditor-loop0.md`, F-1 to F-4) and QA's review-mode PASS (`play-upload/qa-engineer.md`; loop 0 `qa-engineer-loop0.md`). Merged as `<sha>`, code `9005fe3..6a0b032`. The script half of 1.7 is met. The script route at Gate 2 waits on §6's must-rows and on TL-1. *Superseded:* "Nothing; scripts only" (§17).
- **(b) Row PU, item cell, as built:** no arguments; bash only; the key an absolute path outside the checkout; the `SUPPLY_*` unset and `ack_` pinned; four config names refused; the sha256 printed; analytics and the update check off; exit 78 before anything is sent. All reviews ran on opus under Q-019, and a fable read is recommended before the first script-route upload.
- **(c) Merge order 6c:** "*(Done …: merged as `<sha>`, with no merge-in.)*"
- **(d) §13, a new row, "Release-host tools":** fastlane 2.233.0 (MIT), run by `scripts/play-upload.sh`; shellcheck 0.11.0 (GPL-3.0), a review-time lint run on the host, never linked or shipped, **not** an escalation. Neither is in the Gradle graph, and nothing ships from either. Whether `THIRD_PARTY.md`, which records what ships, carries a tooling row is the release-manager's call (the developer's open issue 9).
- **(e) §14.2:**
  - N-1 and N-2 with the script's next edit (the exact text in 2);
  - `check.sh`'s `unset CDPATH` with its scheduled next edit (4);
  - the shellcheck gate's shape and trigger (3).
- **(f) §15, a risk row for E-1:** path-named wrappers pass both plugin gates (likelihood low, impact severe). Mitigation: T-11, the key ordering, and the pre-commit hook. Closed when the plugin release is installed here. Owner: the producer, through the plugin.
- **(g) §16, open items with owners:**
  - the plugin's E-1 fix (producer);
  - this product's missing pre-commit hook (producer);
  - the fable read of the script (producer, under Q-019).
- **(h) §17, a supersession:** `.planning/phases/15-ux-refresh/15-PLAN.md:24` and `:137` ("the publish hook and `scripts/play-upload.sh` resolve the approval file from [VERSION] at run time") are superseded by row PU: the script reads no `VERSION`, and only the hook does. The phase-15 record itself is not edited.
- **(i) The header:** one sync sentence naming this sign-off.

## Outputs
- `.planning/reports/play-upload/tech-lead-signoff.md` — this sign-off.

## Checks
- `git rev-parse HEAD` in the worktree — `b70cac967ba29dea13937bf35dde35cb667380f5`. `git status --short` — clean.
- `git show HEAD:scripts/play-upload.sh | shasum -a 256` — `31873d87faf0fd7be213141884ae6239e06a3ada7001431434763bdb2cc20747`. `git ls-tree HEAD scripts/` — `100755 … 259c9bb5d22b37a6224663f059c39fbc35bc34ca scripts/play-upload.sh`. `git hash-object scripts/play-upload.sh` — `259c9bb5…`, the same blob.
- `shellcheck scripts/play-upload.sh` (0.11.0) — pass: no output, exit 0.
- `/bin/bash -n < scripts/play-upload.sh` (3.2.57) — pass: exit 0.
- `git diff --quiet 6a0b032 HEAD -- . ':!.planning'` — exit 0: the head's code is the fix commit's.
- `git diff --stat f7dd1fe develop` — `.planning/reports/STATUS.md` only (develop at `3ab1cae`).
- `git merge-tree --write-tree develop HEAD` — pass: clean, tree `7c60e57`. `git diff --name-status develop 7c60e57` — the six reports and `scripts/play-upload.sh`, all `A`.
- `grep -n applicationId app/build.gradle.kts` — `:290` `com.erman.pegsolitarie`, equal to `PACKAGE_NAME`.
- Publish gate, offline, with `ERGOZ_LOG=0 bash <plugin 1.0.0>/hooks/publish-gate.sh` fed one payload per form (payloads written with the Write tool to the session scratchpad) — 11 forms: 3 blocked, 8 passed (table in 5). Hook sha256: `publish-gate.sh` `895a0ef7…77ef5c7`, `lib-cmdpos.sh` `8a0a8f37…bc02821c`, the same as the three reviewers'.
- Branch guard, offline, on a scratch `git init -b develop` repository — 5 forms: 3 blocked, 2 passed (table in 5).
- `ls -la "$(git rev-parse --git-common-dir)/hooks"` and `git config --get core.hooksPath` — no installed hooks, no hooks path.
- `./scripts/check.sh` — not re-run. The developer's green Check on `6a0b032` (1,576 tests, 0 failed or skipped; fuzz 10,000 / 0; 0 `w:`) stands for the head and for the merged tree: the code is identical outside `.planning/`, and no Gradle or CI input reads `scripts/`. A tech-lead sign-off re-runs nothing.
- Not run: the script, and fastlane, by any route.

## Open issues / risks
1. **E-1 (plugin, high):** path-named wrappers pass both gates (5). The producer routes the plugin branch. Until it is installed here, T-11 and the key-ordering rule bind the Gate-2 procedure.
2. **No git pre-commit hook in this product** (5). The producer routes its installation.
3. **Rule 1.** The script has had no fable review (1, recommendation). Q-019 covers the merge.
4. **One `gate.block` entry in the studio log is mine, from a read-only command.** It was a `git diff … -- . ':(exclude).planning' ':(exclude)scripts/play-upload.sh'`, whose `)` before the script's name the gate reads as a subshell running it: the documented over-block of quoted text. Nothing ran. I re-ran the check with `':!.planning'` and no pathspec naming the script. Recorded so the retro does not read it as an upload attempt.
5. The release-manager's T-items and TL-1 (6), and the tech-design text (7), are owed after the merge. Neither one blocks it.

## Handoff
Next:
- **producer:**
  - merge `feature/play-upload` into `develop` with `--no-ff`; no merge-in is needed (Merge mechanics);
  - route E-1's plugin fix (5) and this product's pre-commit hook (5);
  - route the must-rows of 6 and TL-1 to the release-manager;
  - schedule the tech-design close of 7;
  - delete `.planning/qa/play-upload/` after the Done line;
  - when fable returns, consider the fable read of the script (1).
- **release-manager (through the producer):** the §6 table and TL-1.
- **tech-lead (fresh, after the merge):** the §7 text, on a docs-only branch.

VERDICT: PASS
