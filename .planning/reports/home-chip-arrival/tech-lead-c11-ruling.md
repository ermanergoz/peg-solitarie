# tech-lead report — home-chip-arrival (C11 test ruling: the merged tree's two red Home tests)
Date: 2026-10-24
Branch: bugfix/home-chip-arrival (report only; read at `2343a84`; the merge-in is `5708d95`, parents `26c5618` and `f7dd1fe`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose, stacks, testing, dispatch. puzzle-design and play-store are identical in the snapshot and were read there. Also the worktree's lessons.md. Plus android-architecture and android-gradle-logic
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** `agents/tech-lead.md` pins fable at xhigh. The Fable limit is reached, and Q-019's answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The developer and the reviewers so far also ran on Opus. Mitigation: every fact below was re-read in the tree, and none is taken from the developer's report alone (Checks). Under §2 the auditor runs the probes itself.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/home-chip-arrival/android-game-dev-merge-c11.md`; `PegSolitarie-dev/.planning/reports/reduced-motion-live-gate/tech-lead-ruling.md` (all 329 lines: the ruling, the Finding 1 routing and the F1 amendment). Read in part: `reduced-motion-live-gate/tech-lead-signoff.md` §1–§4; `home-chip-arrival/tech-lead-defect-ruling.md` (Summary, §4, Open issues, Handoff); `docs/tech-design.md` on `5708d95` (How to read it; §8.1; §14.1 rows F-1 and RM; the second-to-merge paragraph; §16 #28; §17's merge-in row); the two red test files; `testing/SystemMotionScale.kt`; `GameFlowTest.kt:77-80, 250`; `ui/theme/Clay.kt:176-221`; `ui/home/HomeBandColumn.kt:183-284`; `ui/home/HomeScreen.kt:392-401`
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **The remedy is confirmed, with four amendments (§1).** It is §8.1's own rule applied to the two files RM's §3 table could not list. Both stay on the v1 rule, with `effectContext`. No assertion changes. The developer's probe is the right design, but its stated reason is backwards, and §1 gives its expected counts.
- **The code-auditor's loop 3 (§2)** covers the fix and the merge. Because the merge has no conflict hunk, a semantic re-read of the gate replaces the hunk re-read. The auditor runs three probes of its own.
- **That re-read has a finding, ruled here (§3).** `rememberChipRoom` keys its effect on the gate, which breaks §16 #28's rule as written. It cannot strand an animation, so it is a recorded deviation and a low 3.0.1 item, not a merge condition. It becomes gating only if probe P3 goes red.
- **Arm B stays `5708d95`'s APK (§4)**, sha256 `3f66c5b6…21332f7`, provided nothing outside the test sources changes. It is preserved before any Gradle run. The sitting waits for loop 3's PASS.
- **Owed text (§5):** two items in §8.1 and three mirror lines elsewhere. A fresh tech-lead writes them in this branch's close sync, after the merge and before release checklist 1.1.

## 1. The remedy: confirmed, amended
**The diagnosis, verified.**
- `rememberAnimationsEnabled()` (`Clay.kt:219-221`) reads only the composition's `MotionDurationScale`. With no element present, it reads "on".
- Both cases use a plain v1 `createComposeRule()`, with no `effectContext`. Their only reduced-motion mechanism is the `Settings.Global` write (`BoardCanvasFirstFrameTest.kt:85-89`, `HomeChipArrivalTest.kt:170-174`).
- Under Compose's test rule, that write reaches neither reader (§8.1, last paragraph; RM ruling §1). So C11's red is these two tests running with animations on. It is not a production defect.
- Both files were added on this branch after RM's cut at `6aa7624` (`ea094e2`, `98b66d7`), which is why RM's table could not list them.
- Unlike RM's seven, these two went red rather than vacuous. Each asserts an outcome that only the off path produces: a whole first frame in one, and one distinct frame in the other. That is how the merge-in surfaced them.

**The retarget (tech-lead decision).** It is one test-only commit, `test(home): drive reduced motion through the composition's scale`. It touches exactly two files, under `app/src/test/java/com/app/pegsolitaire/`:

| File | What changes | What must not change |
|---|---|---|
| `ui/components/BoardCanvasFirstFrameTest.kt` | Add `private val motion = SystemMotionScale()` **above** the rule, which becomes `createComposeRule(effectContext = motion)`, as `GameFlowTest.kt:77-80` has it (property initialisers run in order). In `theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab`, the `Settings.Global.putFloat(…)` statement (`:85-89`) becomes `motion.turnAnimationsOff()`, in place, as the first statement. Remove `import android.provider.Settings`, `import org.robolectric.RuntimeEnvironment`, and `ANIMATIONS_OFF` with its KDoc (`:130-131`). Add `import com.app.pegsolitaire.testing.SystemMotionScale` | Both test names; every other line of both bodies; `assertTheFirstFrameIsTheSettledFrame` and every assertion; the class KDoc and the case's KDoc |
| `ui/home/HomeChipArrivalTest.kt` | The same field and rule. In `withAnimationsOffTheRoomIsOpenInTheNextFrame`, the write at `:170-174` becomes `motion.turnAnimationsOff()`, in place, as the first statement, before `renderWithThePausedClock()`. Remove the same two imports, and `ANIMATIONS_OFF` with its KDoc (`:774-775`). Add the same import | All 13 test names and bodies apart from that one statement; every helper and assertion; the class KDoc and the case's KDoc |

**The amendments to the proposed remedy:**
- (a) The field comes before the rule.
- (b) The call replaces the write in place, before any composition. The BoardCanvas case is a claim about a cold start's first frame, so the gate must already read 0 when Home first composes. Turning the scale off after composing would test a live switch, which is a different claim.
- (c) Each removed symbol was checked: `Settings` and `RuntimeEnvironment` are used only by the write in each file (`grep`, Checks).
- (d) No file outside the table changes. In particular, `HomeBandColumn.kt` is not touched (§3). No test is added, and none is removed.

The two KDocs stay as they are because they name the condition ("animations off", "`rememberAnimationsEnabled()` false"), not the mechanism, so they are still true.

**Rule version: v1.** The rule is `androidx.compose.ui.test.junit4.createComposeRule(effectContext = motion)`, and the import line is unchanged.
- Not v2: under §8.1, providing a scale element is no reason for v2, and neither case needs a behaviour that only v2 gives.
- The `effectContext` overload carries the same single deprecation warning as the call it replaces (RM ruling §3), so the test compilation's `w:` count does not change.

**The sibling tests are unaffected.** `SystemMotionScale()` starts at `ON` (1f). Both readers treat that exactly as an absent element: the clock's `?: 1f` and the gate's `?: DEFAULT_ANIMATOR_SCALE`. So the other 13 tests run as before.

**Assertions: none may change.** If either case goes red under the retarget, the developer stops and reports to me through the producer, without touching it. With the gate and the clock both at 0, the test is in the phone's state, so such a red would be a Home defect on the device path.

**The probe, amended.** The developer's design is right, but its reason is worded backwards.
- *The probe:* a temporary `return true` in `rememberAnimationsEnabled()`, never committed, while each case still turns the scale to 0.
- *Why it works:* it holds the clock at 0 in both runs and flips only the gate. So a red belongs to the gate *although* the clock reads 0, not *because* it does.
- *Expected:* exactly 2 of the two classes' 15 tests red. The other 13 run at scale 1, where the gate reads "on" anyway, so they stay green.
  - **The BoardCanvas case:** red at "peg ink … in the first frame … but was: 0". With the gate on at composition, `clayEntrance` starts its `Animatable` at `ENTRANCE_START` (`Clay.kt:185`). Frame 1 is drawn before any animation frame, so a clock at 0 cannot rescue it.
  - **The chip room case:** red at "every frame after the emission … expected: 1". The room then reads `openness`, and at scale 0 its tween ends at its first animation frame. I expect 2 or 3 distinct frames, not C11's 15.
    - The count also tests the second reader. If the probe shows 15 frames, the clock is not reading the provided scale, which is a finding.

**What the developer reports** (`android-game-dev-c11-fix.md`):
- §4's APK copy, made first, with its sha256.
- `git show --stat` listing only the two files. `git show -U0 <sha> | grep -E '^[-+].*(assert|containsExactly|isEqualTo|hasSize|isEmpty|isGreaterThan|isNotEqualTo)'` must print nothing.
- The two classes under `--rerun`: 15/15 green.
- The probe's run: 2 of 15 red, with both messages and the distinct-frame count quoted.
- `./scripts/check.sh` green: 1,262 tests and 0 failures, the test compilation's `w:` count unchanged from `5708d95`, and 0 `w:` lines from `src/main`.
- §4's diff condition: exit 0.

## 2. The code-auditor: loop 3, scope and probes
- **Why loop 3 is no escalation.** It follows loop 2's PASS and reviews new commits; it is not a third FAIL. My escalation trigger of three review loops is about failed loops, so it does not apply.
- **Range, model and report.** `26c5618..<fix head>`, two commits: the merge `5708d95` and the fix. On Opus under Q-019; the report goes to `code-auditor-loop3.md`.

**(a) The fix, against §1's table, file by file.**
- The footprint and assertion greps.
- No `@Suppress`, `@Ignore` or baseline.
- `SystemMotionScale` declared before the rule.

**(b) The merge's integrity.**
- `git show --cc --format= 5708d95` prints nothing, so there is no hand-resolved hunk.
- The tree equals `git merge-tree --write-tree 26c5618 f7dd1fe` (`216b4f3`).
- The commit has one trailer.
- `GameFlowTest.kt`'s automatic merge keeps both sides intact:
  - the patch-id of `26c5618..5708d95` on that file equals RM's `6aa7624..f7dd1fe` (`0647347…`);
  - the patch-id of `f7dd1fe..5708d95` equals this branch's `6aa7624..26c5618` (`73d59c3…`).
- `develop`'s own code is not reviewed again. RM's code and tests were reviewed and signed off on RM (`reduced-motion-live-gate/code-auditor.md`, `code-auditor-finding1.md`, `tech-lead-signoff.md`).

**(c) The semantic re-read, in place of the conflict-hunk re-read.**
- *Why it replaces it.* The second-to-merge paragraph below §14.1's merge order asks the sign-off to re-read the merge commit's conflict hunks. This merge has none. The two sides' code is disjoint by file, so the surface where they interact is the gate.
- *The census on the fix head:*
  1. **Gate call sites:** 15 in 10 files. `develop` has 14 in 9; this branch adds `HomeScreen.kt:399`.
  2. **Effects keyed on the gate, or on a value derived from it,** in those 10 files and in any file a gate-derived value is passed to: exactly one, `HomeBandColumn.kt:273`, which §3 rules. Any other is a finding.
  3. **Tests that set the animator scale through `Settings.Global`:** after the fix, only `AnimationsEnabledGateWithoutScaleTest`, which does it on purpose. The only other match is `SystemMotionScale.kt`'s KDoc.
  4. **RM's 13 named classes** (the RM sign-off's §3) are green in the auditor's own Check, listed by name.

**(d) Its own Check.** `./scripts/check.sh --rerun-tasks` at the fix head:
- 1,262 tests and 0 failures;
- fuzz, kover and the literal checks green, and lint with 0 errors;
- 0 `w:` lines from `src/main`, and the test compilation's count unchanged;
- §4's diff condition re-run.

**The probes.** These are the auditor's own runs, not the developer's counts (Q-019 mitigation). None is committed.
- **P1, the gate probe,** as in §1: 2 of 15 red, and the chip room's count is not 15.
- **P2, the setup mutant:** delete `motion.turnAnimationsOff()` from each case. Each must go red with C11's message: 0 px in the one case, 15 distinct frames in the other. This shows that each green rests on that one line.
- **P3, the chip room's live switch:** a temporary test in `HomeChipArrivalTest`, using the class's `motion`. Write the scale inside `runOnUiThread` with `Snapshot.sendApplyNotifications()`, as `framesAfter` writes state. It has two parts:
  1. **On to off, mid-opening:**
     - with animations on, emit the Classic chip and advance into the opening (`START_FRAMES + 2` frames);
     - then turn animations off;
     - from the second frame after the switch, every frame read equals the settled one-chip frame;
     - the hero row and both tiles stay at their no-chip bounds throughout.
  2. **Off to on:**
     - with animations off, emit the chip (the room is open in the next frame);
     - turn animations on;
     - the frame after the switch equals the frame before it;
     - then emit no chip: the closing passes `assertTheRoomMovesSmoothly`.

  Expected: green in both parts on the fix head. A red in either part is a defect. It comes to me before session S, and it makes §3 gating.

## 3. The chip room's gate-derived key: a recorded deviation, not a merge condition
**The fact.**
- `rememberChipRoom` (`HomeBandColumn.kt:267-284`) runs `LaunchedEffect(target, animationsEnabled)`. The parameter comes from the gate read at `HomeScreen.kt:399`.
- This branch added both at `fc9a7dd`, before RM's rule reached it. Loop 2 reviewed it before the merge-in.
- On the merged tree, §16 #28 says: "No effect is keyed on the gate." Its census paragraph names "a second key" and "a gate-derived local" as the forms that `git grep` misses and review must catch. So the merged tree breaks the rule as written.

**Why it cannot strand an animation.**
- *Turned off mid-opening:* the restarted effect takes its off branch and snaps to the target (`:274-275`). The room also reads its target directly while the gate is off (`:281`), in the same recomposition.
- *Turned on:* while animations were off, every target change snapped, so `openness == target`. The restarted effect then does nothing (`:276`).
- So the harm F1.4 names, a cancelled animation left part-way, cannot occur here. This is the `else snapTo` shape that F1.2 declined for the star as the less clean form. It is not a defect.

**Ruling (tech-lead decision): non-gating, recorded, fixed in 3.0.1.**
- *The fix:* move to the house idiom. Key the effect on `target` only, and run the tween whenever `openness` differs from the target. The fraction still reads the target while animations are off. The fix rides the next branch that touches `HomeBandColumn.kt`, if one comes first.
- *Why not now:*
  - there is no defect, and P3 checks that on the JVM;
  - a fix now edits `ui/home/`, a start-path directory;
  - it would move arm B off the APK the device sessions are about to use;
  - and it would add production scope to this branch's third review.
- *If P3 goes red,* the item becomes gating and comes back to me.
- *Until the close sync writes this into §16 (§5),* this section is the requirement that loop 3 checks `:273` against. The file disagrees with it in the meantime. That disagreement is a defect of the file and mine to fix (How to read it), as RM ruling §9 handled its own.

## 4. Arm B: `5708d95`'s APK stays
**Yes.** Arm B is the debug APK built at `5708d95`: sha256 `3f66c5b6409cce8429c354891633a2f5f48e2b8133cd8e56ff755ad6a21332f7`, 22,023,475 bytes. I re-hashed the file on disk, and it matches.
- Test sources never enter the debug APK. The condition is that `git diff --quiet 5708d95 <fix head> -- . ':(exclude)app/src/test' ':(exclude).planning'` exits 0. The developer reports it, and the auditor re-runs it.
- If that command fails, arm B is the fix head's own build, hashed again.

**Preserve it before any Gradle run.**
- The fix dispatch's first step copies `app/build/outputs/apk/debug/app-debug.apk` to `.planning/qa/home-chip-arrival/armB-5708d95.apk`. That folder is git-ignored and already holds the earlier arms under the same naming. The developer then hashes the copy.
- QA installs that copy, checked by sha256.
- A rebuild at the fix head counts only if its sha256 equals the one above.

**Arm A** is `f7dd1fe`, the merge-base, as long as `develop` gains no code. Today `develop` is at `3ab1cae`, which adds only `STATUS.md`. The producer re-checks this before session S.

**Sequencing.** This is a technical condition; the scheduling is the producer's.
- *Session D* (the defect ruling's §4.4) may run on this APK before loop 3 or alongside it, because a test-only fix cannot change what the session tests.
- *Session S, the 15e sitting,* starts only after loop 3's PASS. A red P3 would change `ui/home/` and the APK. P-4's quiet host also rules out the fix's and the auditor's 11-minute Checks during the sitting.
- *If loop 3 changes `app/src/main`,* session D re-runs on the new APK.

## 5. Owed to `docs/tech-design.md`
§8.1's reduced-motion paragraph is right as written, and the fix applies it. It owes two additions, and this ruling creates three mirror lines elsewhere:
1. **§8.1, after "A `Settings.Global` write reaches nothing under Compose's test rule.":** "A test on a branch in flight that still drives reduced motion through `Settings.Global` is retargeted at that branch's merge-in the same way, with its assertions unchanged, and needs no new ruling. Each merge-in greps `app/src/test` for `ANIMATOR_DURATION_SCALE`, since a vacuous case passes the Check (`home-chip-arrival/tech-lead-c11-ruling.md` §1)."
2. **§8.1's v1 count and §9.8's `createComposeRule` figure.** §8.1 reads "on `develop` at `053e9cc`, 38 files import the v1 rule". Both figures are taken again from the merged tree's own compilation when this branch merges, by §17's merge-in rule (4). This branch adds v1 suites.
3. **§16 #28.** Its "As built … 14 call sites in 9 files" becomes 15 in 10 (`HomeScreen.kt:399`). Its census sentence gains `HomeBandColumn.kt:273` as §3's recorded deviation.
4. **§16, one new row,** numbered after `develop`'s highest at the sync: "`rememberChipRoom` keys its effect on the gate". It is open, owned, low, 3.0.1 and not cut-blocking. Chain: tech-lead brief → android-game-dev → code-auditor → tech-lead. The brief rules whether a 15e sitting applies.
5. **§14.1 F-1 at Done:** cite this ruling and the fix commit.

**Who and when.**
- *Who:* the tech-lead, in a fresh dispatch.
- *When:* in this branch's close sync, a docs-only branch off `develop` cut right after this branch merges. It carries the defect ruling's Open issue 4 mirror rows too. It runs as the RM close did: the tech-lead edits, the code-auditor reads it as text, and a fresh tech-lead signs it off.
- *Deadline:* before release checklist 1.1, which reads F-1's row.
- *Why not on this branch:* loop 3 depends on none of this text, since this ruling is its requirement. And `docs/tech-design.md` is the file every in-flight branch's close sync edits.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-home/.planning/reports/home-chip-arrival/tech-lead-c11-ruling.md` — this ruling, the only file written, committed alone. No code, no Gradle, no device.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-home`. This is a ruling, so no gate was re-run, and `.planning/log/` was not read.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0); a `diff -q` of each rule file against the plugin copy — older, so the plugin copies were read.
- `git status --short`; `git log --format='%h %p %s' -3` — pass: clean; head `2343a84`; `5708d95`'s parents are `26c5618` and `f7dd1fe`.
- `git show --cc --format= 5708d95 | wc -l` — pass: 0.
- `git merge-tree --write-tree 26c5618 f7dd1fe` against `git rev-parse 5708d95^{tree}` — pass: both `216b4f38…`.
- `git log -1 --format=%B 5708d95 | grep -c Co-Authored-By` — pass: 1.
- `git patch-id --stable` on `GameFlowTest.kt` — pass, both sides intact:
  - `6aa7624..f7dd1fe` = `26c5618..5708d95` = `0647347…`;
  - `6aa7624..26c5618` = `f7dd1fe..5708d95` = `73d59c3…`.
- `git grep -n ANIMATOR_DURATION_SCALE 5708d95 -- app/src/test` — read: the two red files, `AnimationsEnabledGateWithoutScaleTest` and `SystemMotionScale.kt`'s KDoc.
- `grep -n 'Settings\b|RuntimeEnvironment|ANIMATIONS_OFF'` over the two files — pass: each symbol is used only by the write and its constant.
- `git log --diff-filter=A` on the two files — read: `ea094e2` and `98b66d7`, both after `6aa7624`.
- `sed` of `Clay.kt:176-221` — read: the gate at `:219-221`, and `clayEntrance`'s start value from the gate at `:185`.
- `git grep 'rememberAnimationsEnabled()'` at `f7dd1fe` and at `5708d95` — read: 14 calls in 9 files, then 15 in 10.
- `git grep 'LaunchedEffect(.*nimat|DisposableEffect(.*nimat' 5708d95 -- app/src/main` — read: `HomeBandColumn.kt:273` (gate-derived) and `JumpHapticEffect.kt:42` (`animatingMoveId`, not the gate).
- `git log -S'LaunchedEffect(target, animationsEnabled)'` — read: `fc9a7dd`, this branch.
- `grep -n` on `GameFlowTest.kt` — read: the field at `:77`, the rule at `:80`, and the call at `:250`.
- `grep -c '@Test'` — read: 2 + 13 = 15.
- `shasum -a 256 app/build/outputs/apk/debug/app-debug.apk` — pass: `3f66c5b6…21332f7`.
- `git rev-parse --short develop`; `git diff --name-only f7dd1fe develop` — read: `3ab1cae`, which changes only `.planning/reports/STATUS.md`.
- `git check-ignore -v .planning/qa/home-chip-arrival/x.apk` — pass: ignored by `.gitignore:47`.

## Open issues / risks
1. **Q-019.** This ruling, the code and the reviews all run on Opus. The auditor's own P1–P3 are the mitigation; it does not take the developer's counts on trust.
2. **Other in-flight branches** (talkback-board, licences-screen, colour-vision) may carry the same `Settings.Global` pattern. A loud case goes red at their merge-in Check, as these two did, but a vacuous one passes silently. §5 item 1's grep is the guard. The producer can name it in each of their merge-in dispatches now, without waiting for the close sync.
3. **A red P3 makes §3 gating** and changes the APK. That is why session S waits for loop 3 (§4).
4. **The sign-off's reading list** (defect ruling §4.5) gains:
   - `android-game-dev-merge-c11.md`;
   - this ruling;
   - `android-game-dev-c11-fix.md`;
   - `code-auditor-loop3.md`.
5. **Arm A is checked again before session S.** A code commit on `develop` before then means another merge-in and a new arm B.

## Handoff
Next: producer —
1. This file is committed alone.
2. Dispatch a fresh android-game-dev in `PegSolitarie-home` for §1. §4's APK copy comes first. Its report is `android-game-dev-c11-fix.md`.
3. Then the code-auditor's loop 3 (§2), at `code-auditor-loop3.md`.
4. Session D may run on the preserved arm B APK meanwhile (§4). Session S runs only after loop 3's PASS, on a quiet host.
5. Then a fresh tech-lead's sign-off (defect ruling §4.5, plus open issue 4).
6. Queue §5 for the close sync, and pass open issue 2's grep to the in-flight branches.

Next: android-game-dev — §1 exactly, after §4's copy. Stop and report on any red under the retarget.
Next: code-auditor — §2 (a)–(d) and P1–P3, checking `HomeBandColumn.kt:273` against §3.
Next: tech-lead (a fresh one) — the sign-off; later, §5 at the close sync.

RULING: REMEDY CONFIRMED (AMENDED); RULE v1 WITH effectContext; NO ASSERTION CHANGES; ARM B = 5708d95 (3f66c5b6…)
