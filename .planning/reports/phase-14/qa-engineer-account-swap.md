# qa-engineer report — account-swap / profile-swap wipe UAT (D-12 … D-15, F-5)
Date: 2026-09-21
Branch: develop (8dc9f09, read-only — no branch switched, nothing staged or committed)
Skills loaded: ergoz-soft:standards (role: qa-engineer) → rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md (plugin copies at v0.5.2, snapshot `.claude/rules/` is v0.5.1)
Ran in parallel with: phase-14 r1 and r5 build tasks (sibling worktrees, not touched; no Gradle run)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` holds 0 active lessons; no `.planning/recall/RECALL.md` exists)

## Result per numbered item

| # | What was to be established | Verdict |
|---|---|---|
| 1 | The dialog appears at all, and only when it should | **PASS** |
| 2 | Dismissal means keep (D-13) — explicit, Back, tap-outside | **PASS** |
| 3 | The replace path wipes completely (D-14) | **PASS** |
| 4 | F-5's case: the same choice with the network off | **PARTIAL** |
| 5 | D-03 holds throughout (no app-owned toast/snackbar/Settings row) | **PASS** |
| 6 | Judge the copy (I-2) | **Opinion given — it is a real, minor problem; one clause fixes it** |

**VERDICT: PASS** — with item 4 recorded as partial. The one path in the milestone that had never been exercised on a device now has been, in both directions, and every D-12…D-15 assertion held. Item 4 is partial because the *failure* F-5 predicts could not be produced on this device, not because anything misbehaved; see §4.

## Environment and build

- Device: `emulator-5556`, AVD **Pixel_9b_clean** (`sdk_gphone64_arm64`, Android 16). One emulator, already running; none booted by me.
- Two distinct Google accounts present. Referred to throughout as **A** (the existing tester, already authorised for this game) and **B** (never authorised this game; it held **"No profile"** in the Play Games account list at the start of the session). Neither gamer tag, email nor player id is written anywhere in this report or in any artifact; the two player ids are distinguished only by the first 12 hex of their SHA-256: **A = `7b789a18bc55`**, **B = `d3257fac1cd8`**.
- Build under test: the debug APK already installed. I did **not** run Gradle. The installed `base.apk` is **byte-identical** to `app/build/outputs/apk/debug/app-debug.apk` (md5 `6a9c14651b4108c9f904e03f5e619a9e`, 20,637,687 bytes, built 2026-09-21 00:52). That artifact predates the two develop merges of 01:29+, so the build SHA is the tree at **`10ee7d7`** ("docs: name the ceiling exception in the next-level invariant (F-3)"), not the develop head `8dc9f09`. This is immaterial to the path under test: `git diff --stat 10ee7d7 8dc9f09 -- app/src/main domain/src/main` is **empty**.
- `applicationId` = `com.erman.pegsolitarie`, versionName 3.0.0 / versionCode 3.

## How the dialog was provoked (this is the part that was previously impossible)

`11-HUMAN-UAT.md` §Gaps records that the dialog "cannot be produced here". It can, and the lever is not the app:

**Google Play services → Play Games settings → Sign In Management → "Change the account by game" → Peg Solitaire → "Ask each time".**

```
adb shell am start -n com.google.android.gms/.games.ui.settingsv2.GamesSettingsActivity
```

The Play Games *app* (`com.google.android.play.games`) is **not installed** on this AVD, but GMS ships the whole surface itself (`com.google.android.gms/.games.ui.settingsv2.GamesSettingsActivity`, `.games.ui.account.SelectAccountActivity`). Three things were learned there, and they are why every earlier attempt at this test failed:

1. Changing the **profile being viewed** (the top account selector) or the **default account for games** does **not** change what this game signs in as. I tried it first: the app relaunched, GMS logged `Found existing game run`, and `play_games_merged_player_id` was unchanged (`7b789a18bc55`). The binding that matters is the **per-game** one.
2. The per-game sheet offers `<account A>` / `Ask each time` / `Sign out`. Setting **"Ask each time"** makes GMS raise its own profile chooser on **every** app launch, which is a per-launch account switch under the tester's control — reversible, and the only non-destructive lever I found. (It also, incidentally, unblocks Phase 11 test 2, which has been blocked since 2026-09-15 for exactly the same reason. Not my task; flagged to the tech-lead.)
3. Account B had **no Play Games profile**, so it could not be selected until one was created. I completed GMS's "Create a Play Games profile" flow for B from inside that chooser. **This is an environment change I made deliberately and I am naming it:** account B now has a Play Games gamer profile. Nothing else about B was altered, and no app state was hand-edited at any point — in particular I never wrote `play_games_merged_player_id` myself, which is what "forcing it" would have meant.

With that in place the dialog is reproducible on demand, in **both** directions (A→B and B→A), in about 25 seconds per cycle.

## 1. The dialog appears at all, and only when it should — PASS

Starting state (`prefs.py`, masked): `play_games_opted_in=True`, `play_games_merged_player_id` = A (`7b789a18bc55`), `challenge_current_level=26`, `play_games_cloud_last_push_stage=COMMITTED`.
Starting local progress (Room, pulled with `run-as … cat databases/peg_solitaire.db` and read with python3 `sqlite3`): **29 `level_result` rows** (challenge 25 rows / 75 stars / max id 25; classic 4 boards at 3 stars each = 12 stars), **15 `play_session` rows**, **2 `resume_snapshot` rows** after I deliberately left both a Challenge game (level 26) and a Classic game (Diamond-41) in progress.

- **Negative control, three times.** Cold relaunch signed in as A (stored id = A) → Home, **no dialog**, `Continue — Level 26` intact. Observed at 03:01 (artifact 01), again at 03:09 after the chooser was set and A re-picked, and a third time at 03:25 after the per-game binding was restored to A. In all three the GMS silent sign-in fired (`GamesConnectSignInOp: signIn(%s): Returning SUCCESS`, `SignInPerformer: Found existing game run`) and the app showed nothing of its own.
- **Positive.** Signed in as B (stored id = A) → the dialog **"Moving to a different account?"** appeared on **Home**, over nothing else, within ~10 s of launch. Raised 5 times across the session, in both directions (A→B ×3, B→A ×2), with no misfire and no stacking with the onboarding overlay or the opt-in prompt (D-15).
- **An unanswered dialog writes nothing.** With the dialog up I ran `am force-stop`; `play_games_merged_player_id` was still A afterwards. That matches `submitAll` step 1 returning before the pull and the push, and it is what let me stage the rest of the run without contaminating either cloud.

Verbatim dialog text as rendered on the device (gamer tag replaced by `<B>` here; the string is `profile_swap_body`):

> **Moving to a different account?**
> You're signed in as `<B>`. Keeping this device's progress merges it into their cloud save — nothing is lost on either side. Using their cloud progress instead replaces everything on this device: your level results, your play history, your Challenge level and both in-progress games.
> `[ Keep this device's progress ]`   `Use their cloud progress`

## 2. Dismissal means keep (D-13) — PASS

All three dismissal routes the platform allows were exercised, each on a freshly raised dialog, with local progress at 29 `level_result` rows / ladder 26 and the *other* account's cloud holding something different at the time.

| Route | Dialog closed | `level_result` | ladder | `play_games_merged_player_id` | push |
|---|---|---|---|---|---|
| **System Back** (`input keyevent KEYCODE_BACK`) | yes | 29 (unchanged) | 26 (unchanged) | advanced to the signed-in id | `COMMITTED` |
| **Tap outside** (`input tap 540 300`, in the scrim above the dialog) | yes | 29 (unchanged) | 26 (unchanged) | advanced to the signed-in id | `COMMITTED` |
| **"Keep this device's progress"** | yes | 29 (unchanged) | 26 (unchanged) | advanced to the signed-in id | `COMMITTED` |

Nothing was wiped on any route, and the app did **not** silently adopt the arriving account's progress: the Back route was run against account B whose cloud at that moment held only the **empty** payload committed by the earlier replace (§3), and local stayed at 29 rows rather than collapsing to 0. Home kept `Continue — Level 26` throughout (artifacts 06, 07). Each dismissal also counts as *answered* — the stored player id advances, so the dialog does not re-raise on the next pass (D-15) — which is why re-provoking it needs another account switch rather than another launch.

## 3. The replace path wipes (D-14), and wipes completely — PASS

Run twice, deliberately, in the two configurations that answer two different questions.

**3a — replace into an account whose cloud is empty (B).** Immediately before: 29 `level_result`, 15 `play_session`, **2** `resume_snapshot`, ladder 26, both Continue tiles on Home (artifact 04). Tapped **"Use their cloud progress"**. Within ~8 s, with no intermediate screen:

| | before | after |
|---|---|---|
| `level_result` | 29 | **0** |
| `play_session` | 15 | **0** |
| `resume_snapshot` | 2 (Classic + Challenge) | **0** |
| `challenge_current_level` | 26 | **1** |
| `play_games_merged_player_id` | A `7b789a18bc55` | **B `d3257fac1cd8`** |
| Home | two Continue tiles, `Continue — Level 26` | no Continue tile, **`Continue — Level 1`** |

This is the decisive proof that the path **replaces rather than merges**: a merge of A's 29 rows with B's empty cloud would have left all 29 rows and ladder 26 standing. It also confirms D-14's last sentence literally — B had no cloud save, and replace meant a clean slate (`play_games_cloud_last_pull_last_modified_at = -1`, i.e. the snapshot was created, not loaded). Nothing of A's progress survived into B's session (artifact 05).

**3b — replace into an account whose cloud is not empty (A).** From that clean slate I switched back to A, the dialog raised again, and I chose replace a second time. Result: `level_result` back to **29** (challenge 25 / 75 stars, the same four Classic boards at 3 stars), `challenge_current_level` back to **26** — and, correctly, `play_session` **stayed 0** and `resume_snapshot` **stayed 0**. That is exactly right and worth stating as a positive: `play_session` is local-only by D-06 and the resume snapshots are local-only by CLOUD-04, so the import must not bring them back, and it did not. The wipe of the in-progress games is therefore permanent across a swap — which is what the dialog copy promises.

## 4. F-5's case: the same choice with the network off — PARTIAL

F-5 says the wipe commits before the replacement is known to be obtainable, and asks for "tap *Use their cloud progress* with the network off" as a named case. I ran it twice, escalating.

**Attempt 1.** Dialog raised online (so the session could resolve at all — offline it cannot, and that is the only honest way to stage this), then `svc wifi disable` + `svc data disable`; `dumpsys connectivity` → `Active default network: none`. Tapped **Use their cloud progress**.
**Attempt 2.** Same, plus `cmd connectivity airplane-mode enable` and `am force-stop com.google.android.gms` immediately before the tap, to cold-start the SDK's process.

**Both times the device refilled and never looked blank.** `level_result` 29, ladder 26, Home `Continue — Level 26`, and `play_games_cloud_last_pull_opened=True` with a *fresh* `play_games_cloud_last_pull_last_modified_at` (1789949941220, then 1789949999829). The answer was genuinely processed — `play_games_merged_player_id` advanced on both, which is only reachable after `resetAllProgress()` in `applySwapAnswer` — so the wipe ran and the pull that follows it **succeeded with no network at all**. The plain reading is that the Saved Games client served the snapshot from its **on-device cache**, the account in question having been pulled minutes earlier in the same session.

What this establishes and what it does not:

- **The cloud is never at risk.** Exactly as the auditor predicted. Across two offline replaces and five swaps, no account's cloud save lost anything: after every cycle both accounts' progress was still reachable, and the final restore brought A back to 29 rows / ladder 26 from A's cloud alone.
- **A *cloud* that lost data was not observed.** No fail item.
- **F-5's blank-looking device is real but I could not produce it offline.** The only blank device I produced was §3a — an *empty target cloud*, which is D-14's documented outcome, not a defect. The failure branch F-5 describes needs a pull that actually fails, and on this device a cached snapshot answers instead. It would still bite where the cache is cold: a swap to an account this device has never pulled, taken offline. I could not construct that here without destroying the two-account setup, and I judged that not worth it.
- Verdict **partial**, not fail: the named case ran, twice, and produced a better outcome than the finding feared. **F-5 should stay open at Low** rather than being closed by this run.

## 5. D-03 must hold throughout — PASS

Checked on every screen the run touched, across 12 app launches, 5 dialog raises, 2 wipes, 2 imports and 3 dismissals:

- **No app-owned toast, snackbar or banner** at any point — not after a wipe, not after an import that changed 0 rows into 29 and the ladder from 1 to 26, not after a dismissal, not on the offline attempts. The only overlay the app ever drew was the profile-swap dialog itself.
- **No Settings sync row.** `Settings → Play Games` (checked immediately after the §3a wipe) reads exactly: `Signed in as <tag>` / "Leaderboards, achievements and cloud save", then the `Leaderboards` and `Achievements` rows, then `Privacy`. No "last synced", no "Progress restored", no cloud status of any kind. (Not screenshotted: that screen renders the gamer tag.)
- **No crash.** `logcat | grep -c "FATAL EXCEPTION"` = **0** over the whole session. The only app-tagged errors are three benign `hiddenapi: Accessing hidden method AccessibilityNodeInfo;->get/setSelection` lines, emitted by the uiautomator dump, not by app code.

## 6. Judge the copy (I-2) — my judgement, for the game-designer

**Yes, it is a real problem, and a small one.** Reading the dialog on the device as a player, with no knowledge of the design: the copy is unusually good — it names all four deleted categories in plain nouns, it says "nothing is lost on either side" about the safe branch, and the emphasis inversion is right (the gradient pill is *Keep*, the wipe is a plain text button, artifact 03). But the sentence structure sets up a symmetry that is not true: "Keeping … merges it into their cloud save" versus "Using their cloud progress instead replaces everything on this device". Both clauses presuppose that *their cloud progress* exists. A player who has just signed in on a second account — which is the only situation this dialog appears in — has no way to know whether that account has ever played, and the most likely case in practice is that it has not. The outcome I measured in §3a is then: tap the second button, and the device is empty. Nothing warned them, and the wipe is not undoable from inside the app.

It is Info and not a defect because the destructive branch is the de-emphasised one, dismissal is safe, and the player's own account still has its cloud save. But the cost of fixing it is one clause. My suggestion, phrased so it does not need the app to know the answer (it cannot — a `null` load is indistinguishable from an empty cloud, `pullOnce`'s KDoc says so):

> "… replaces everything on this device: your level results, your play history, your Challenge level and both in-progress games. **If that account has never played, this device starts over.**"

That is 10 words, it states the exact observed outcome, and it makes no claim the code cannot back. Routing: **game-designer** (dialog copy is theirs by D-15's discretion list), with the ux-designer on the wording. Marked clearly: this is my judgement as QA, not a test result.

## Outputs

- `.planning/reports/phase-14/qa-engineer-account-swap.md` — this report (uncommitted, for the producer).
- `.planning/qa/phase-14-swap/` (git-ignored, `QA_OUT`) — 9 screenshots, each also at full resolution:
  - `01-relaunch-as-A-no-dialog-s.png` (42 KB) — negative control: Home as A, no dialog, `Continue — Level 26`.
  - `02-challenge-L26-in-progress-s.png` (44 KB) — the Challenge board left mid-game, so the resume wipe is observable.
  - `03-profile-swap-dialog-redacted.png` (66 KB) — **the dialog**, with the two body lines that carry the gamer tag blacked out by me before the file was written.
  - `04-home-before-replace-s.png` (78 KB) — both Continue tiles + `Continue — Level 26`, immediately before the wipe.
  - `05-after-replace-empty-cloud-s.png` (40 KB) — after the D-14 wipe into an empty cloud: no Continue tile, `Continue — Level 1`.
  - `06-after-back-dismiss-s.png` (47 KB) — after the system-Back dismissal: progress intact.
  - `07-after-keep-progress-intact-s.png` (41 KB) — after the explicit Keep: progress intact.
  - `08-offline-replace-blank-device-s.png` (41 KB) — the F-5 case; the device is **not** blank, it refilled from the GMS cache.
  - `09-restored-account-A-s.png` (40 KB) — environment restored: account A, ladder 26, 29 rows.

Logcat excerpt (the silent sign-in that precedes every raise; GMS redacts its own identifiers with `%s`):

```
SignInPerformer-16: (REDACTED) Found existing game run [%s]
SignInPerformer-16: (REDACTED) Successfully resolved account: [%s]
SignInPerformer-16: (REDACTED) Successfully authorized %s
SignInPerformer-16: (REDACTED) Successfully found player (id [%s], originalId [%s], displayName [%s]) for account [%s]
SignInPerformer-16: (REDACTED) Connection from [%s], returning true for shouldShowWelcomePopup.
GamesConnectSignInOp: (REDACTED) signIn(%s): Returning SUCCESS (%s) for request %s
```

## Checks

- `adb -s emulator-5556 devices -l` — pass (one device, `emulator-5556`, AVD Pixel_9b_clean; no emulator booted by me)
- `md5 app/build/outputs/apk/debug/app-debug.apk` vs the pulled `base.apk` — pass (identical, `6a9c14651b4108c9f904e03f5e619a9e`)
- `git diff --stat 10ee7d7 8dc9f09 -- app/src/main domain/src/main` — pass (empty: the installed build's swap path is the develop head's)
- `git status` — pass (clean apart from this report; no branch switch, no `git add`, no commit)
- `python3 .planning/qa/tools/prefs.py emulator-5556` — pass (read 11 keys per sample, 9 samples; every player-id value masked to a SHA-256 prefix before it reached any output)
- `adb shell run-as com.erman.pegsolitarie cat databases/peg_solitaire.db*` + python3 `sqlite3` — pass (8 snapshots: before / pre-swap / post-replace / post-import / post-back / post-outside / post-keep / f5 / restore)
- `adb shell dumpsys connectivity | grep "Active default network"` — pass (`none` on both offline attempts)
- `adb logcat -d | grep -c "FATAL EXCEPTION"` — pass (**0** across the whole session)
- Hands-on session with screenshots exists for a task that exercised UI — pass (9 screenshots, §Outputs)
- **Not run: Gradle.** `./gradlew check` was deliberately not executed — two build tasks are live in sibling worktrees and the brief forbids it. The check gate for this work belongs to whoever owns those branches; this report is device evidence only.

## Open issues / risks

1. **F-5 stays open (Low).** Its failure branch was not reproducible on a warm GMS cache. Recommend the finding be kept, with this run appended as "exercised twice offline; refilled from the on-device Saved Games cache both times; cloud never at risk". A cold-cache repeat (first-ever swap into an account on a device that has never pulled it, taken offline) is the outstanding case.
2. **I-2 needs a design decision** — §6, routed to the game-designer.
3. **Environment changes I made, for the producer's record.** (a) Account B now has a Play Games gamer profile, created through GMS's own flow — it did not have one. (b) The per-game sign-in binding was set to "Ask each time" for the duration and **restored to account A** at the end; a clean relaunch now goes straight to Home as A with no chooser. (c) Collateral of running the D-14 wipe for real: this device's **`play_session` (15 rows) and both resume snapshots are permanently gone** — `play_session` is local-only by D-06 and the resume snapshots by CLOUD-04, so no cloud restores them. `level_result` (29 rows) and the ladder (26) were restored from A's cloud and are intact. Phase 12 achievements are unaffected (they live server-side).
4. **`11-HUMAN-UAT.md` §Gaps is now stale** and I did not edit it (instructed not to). It says the dialog "cannot be produced here". It can — the recipe is §"How the dialog was provoked". Someone with authority over that file should replace the gap with a test 14 carrying this result.
5. **Phase 11 test 2 is unblocked by the same lever.** "Ask each time" makes GMS show its account chooser on every launch, which is the surface test 2 has needed since 2026-09-15 to settle `CANCELED` (16) vs `SIGN_IN_CANCELLED` (12501). Out of scope for me; flagged.
6. **Not covered by this run:** the "Ask each time" chooser sits between the launcher and the app on every launch while it is set, so launch-timing observations made during this session are not comparable to a normal cold start. Nothing in D-12…D-15 depends on that.

## Handoff

Next: **tech-lead** — the account-swap UAT the v1.1 release branch was waiting on is done and PASSES; item 4 is partial and F-5 should stay open at Low rather than be closed by this run. Then **game-designer** for §6 (I-2 copy), and **producer** for open issues 3–5 (environment record, the stale `11-HUMAN-UAT.md` gap, and the now-unblocked Phase 11 test 2). `.planning/qa/phase-14-swap/` is git-ignored and can be deleted once STATUS.md carries this task's Done line.
