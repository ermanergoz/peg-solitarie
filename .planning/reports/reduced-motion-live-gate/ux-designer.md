# ux-designer report — reduced-motion-live-gate (rulings: the notice on a live switch, 23e's wording, QA's F1)
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (committed by the producer). Read at head `46d4317`. The only file edited is `docs/ux-spec.md`, and it is uncommitted.
Skills loaded: ergoz-soft:standards (role ux-designer; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch; also the snapshot's lessons.md. Plus ui-ux-pro-max, android-accessibility and compose-skill
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (QA), licences-screen (auditor and ux ruling), colour-vision (auditor, tech-lead and ux rulings)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published. The code under judgement was also written on Opus.
Fan-out: none. One helper ran (Explore, haiku): a grep of `docs/ux-spec.md`, because this role has no search tool. It made no judgement.
Loops: 0
Read in full:
- `.planning/reports/reduced-motion-live-gate/tech-lead-ruling.md`, including the Finding 1 section and the F1 amendment;
- `.planning/reports/reduced-motion-live-gate/qa-engineer.md`;
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeBlobs.kt`.

Read in part:
- `.planning/reports/audio-review/tech-lead-signoff.md`, lines 1–256, which hold §1–§6 with §3 whole (the rest is the merge-in re-read);
- `ui/game/HintUndecidedNotice.kt:100-209`;
- `docs/ux-spec.md`: the head through `:488`, `:3776-3805` (§4.4.2 motion and TalkBack), `:4765-4824` (§10), `:6200-6339` (23–23j) and `:6490-6579` (TB-3 to TB-8, D-TBa's "each is spoken once");
- `ergoz-soft/agents/ux-designer.md`, the frontmatter.

Lessons applied: none. The snapshot has 0 active lessons for game / android-compose-game, and `.planning/recall/RECALL.md` does not exist.

## Summary
- **§6 (open issue 3), UX decision: a switch of the animation setting is not a new occasion to speak, so the re-speak is a defect, rated low.** Per the ruling, it becomes a 3.0.1 item and does not block the cut. The same switch back on also replays the notice's entrance. That is the same defect seen visually, and the same fix covers it. Both are recorded under 23d, with the four things the fix must hold.
- **§7, 23e:** reworded to "With the composition's animation scale at 0, as on a phone with Remove animations on". The first wording is kept in a dated note, and the behaviour is unchanged. §10's mechanism sentence named the same stale `Settings.Global` read, so it is corrected too.
- **F1, UX decision: about 10 s of the old setting after a switch is acceptable for 3.0.0.** Until the change arrives, the game runs whole on the old setting, and nothing is lost. For any later change to the mechanism, the gate and the clock agreeing comes before speed. QA's pass line for a live switch is: in force within 15 s, and no tap answered by neither a shake nor a mark.
- The device count for §6, which QA could not run, is not needed for this ruling. It moves to the 3.0.1 fix's QA, with the app kept in the foreground.

## 1. §6: the Undecided notice on a live switch (UX decision)
**The facts I rely on**, checked in the code and not taken from the reports alone:
- `UndecidedNoticeSlot` (`HintUndecidedNotice.kt:155-189`) picks a structure by the gate inside `key(raiseKey)`. With animations on, it uses an `AnimatedVisibility` around the notice, with `clayEntrance(0)` on the notice. With them off, it uses the plain notice.
- A flip while the notice is up therefore disposes one structure and composes the other. The node is new, so the polite live region speaks again. From off to on, the entrance replays.
- The display timer, `LaunchedEffect(raiseKey) { delay(…); ended = true }` at `:163-168`, sits **outside** the gate's branch. So a flip neither restarts nor shortens the reading time.
- The flip loses nothing:
  - from on to off while the notice is showing, the plain notice is composed;
  - from off to on, `AnimatedVisibility(visible = true)` is composed with `EnterTransition.None`;
  - a flip during the fade-out ends the fade at once, which is the reduced-motion result anyway.
- So the only effects are a repeated announcement and a replayed entrance.

**The ruling: not a new occasion, and so a defect.**
- 23d's "every time" is tied to the screen's raise count, and its own text keys the node "on the screen's own raise count". A setting change is not a raise.
- The spec already treats every non-event this way:
  - 23g: a recreation "neither shows nor announces it again";
  - TB-6: "no event is spoken twice or late";
  - TB-8: "a rotation on Results speaks no second summary";
  - D-TBa (the TB-6 amendment): the notice's line "is spoken once".

  A setting change is one more such case.
- F1 makes the repeat stranger than the ruling assumed. From Settings, the flip reaches the app about 10 s after the switch, so the second announcement is not spoken together with TalkBack's read of the returning window. It arrives seconds later and is tied to nothing the player did. It needs a notice that outlives that trip, which, with the default 5 s, means a long Time to take action. TalkBack users are the ones likely to set that.
- The replayed entrance (a 240 ms fade and rise) tells a sighted player that a new Undecided result arrived, which is not true.

**Severity: low, and not cut-blocking.** I agree with the tech-lead's reasons:
- Reaching it takes a live switch while the notice is up.
- The text is true and polite.
- It is a repeat, not a loss or an error.

So the stack's accessibility rules still hold for 3.0.0, and it ships as a known exception.

**What the 3.0.1 fix must hold** (user-facing; the mechanism is the tech-lead's). This is written into 23d:
1. A switch while the notice is up adds no announcement, at once or late. TalkBack speaks the text once per raise.
2. It adds no entrance. The notice stays where it is and as it is.
3. It leaves the reading time alone (true today).
4. The notice leaves by the setting in force when it leaves: the fade with animations on, gone in one frame with them off (23e).

On top of these, 23d's node per raise, 23e's timings and 23g all hold. The fix's device check counts the announcements across a switch in each direction, with the app kept in the foreground (split screen with Settings, QA's own route) so that F1's delay does not hide the result.

## 2. §7: 23e's wording
- **The replacement, as the ruling worded it:** "On Robolectric with `Settings.Global.ANIMATOR_DURATION_SCALE` set to 0," becomes "With the composition's animation scale at 0, as on a phone with Remove animations on,".
- **The dated note** keeps the first wording and says why it was replaced: under Compose's test rule, that write reaches neither the gate nor the clock (ruling §1–§2).
- **The timings are unchanged.**
- **One more stale sentence, fixed at the same time:** §10's "Reduced motion is a hard requirement" sentence said the gate "reads `Settings.Global.ANIMATOR_DURATION_SCALE`" (`Clay.kt:209-218`). On this branch, that is the falsehood the branch removes. It now says that the gate reads the scale the composition's own animations run at, so the gate and the clock are one value. A dated note keeps the first wording. The ruling's §7 did not list this sentence. It is this role's document and the same class of stale text, so it is fixed here.
- **The spec's other two mentions of the key** are the dated notes that quote the first wordings, and they are meant to stay. The QA steps at `:4366` and `:5666` ("Developer options → Animator duration scale: off") are device settings, not test setups, and they stay valid.

## 3. F1: about 10 s of the old setting after a switch (UX decision: acceptable for 3.0.0)
**What the player gets.**
- The player switches Remove animations on in Settings and comes back. QA's measured run returned after 2.65 s.
- The game keeps the old setting until about 10 s after the switch: 9.8–11.7 s, in both directions, on the board, the stars and the tutorial pulse.
- The game stays consistent throughout: the shake plays its full 233 ms, and no feedback is missing.
- A cold start and a restore after process death follow the setting from their first frame (QA §8).

**Why it is acceptable for 3.0.0.**
- **The exposure is small.** It comes once per switch and lasts at most about 10 s from the switch, less the time spent in Settings.
- **The unprompted motion is mild.** On the board, the motion is the player's own: a jump, a shake, a selected peg's pulse. Only two things move unprompted, and both are slow and small:
  - Home's two blobs, at low alpha, peeking from the corners, ±16dp over about 8 s (`HomeBlobs.kt`);
  - the tutorial's coach pulse.
- **Nothing is lost or wrong in the meantime.**
- **The alternative is worse.** The only faster route on the table was a second read of the setting, and the ruled design rejected it because it can split the gate from the clock (audio sign-off §3). That split is the frozen gate's own defect, a wrong tap answered by nothing (D-06a's "never for nothing"). A few seconds of mild motion are better than lost feedback.
- **The cause is very likely the platform's.** If QA's likely cause holds, every app that learns the setting through Compose's observer gets the same delay.

**Recorded in §10 ("A switch made while the game is open"):**
- the behaviour above;
- the acceptance;
- **the order for any later change:** the gate and the clock agree on every frame first, and speed comes second; a change that shortens the delay but lets them disagree for one frame is a regression;
- **a non-blocking backlog aim:** the new setting in force from the first frame after the player comes back, for the tech-lead to weigh under that order;
- **QA's pass line:** in force within 15 s of the switch (10 s, plus the 1.8 s tap cadence and a margin), no tap answered by neither a shake nor a mark, and a setting that never arrives without a restart fails.

The mechanism stays the tech-lead's.

**Deliberately left out.** I make no claim that a **rotation** inside the window follows the new setting. The tech-lead's ruling (§8) has Compose's flow cached per application context, so a rotation inside the window may still show the old value. QA did not measure that case. If it does show the old value, it is inside the same accepted bound, and it is not judged here.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rm/docs/ux-spec.md`, edited in these places (all uncommitted):
  - the revision index at the head gains "Twelfth revision (the reduced-motion live gate)";
  - 23d gains the dated paragraph "A change of the animation setting is not a raise", with 3.0.0's known exception and the 3.0.1 fix's terms;
  - 23e is reworded, with the first wording in a dated note;
  - §10's mechanism sentence is corrected, with a dated note;
  - §10's notice bullet gains a pointer to 23d;
  - §10 gains the F1 ruling, "A switch made while the game is open".
- `/Users/ermanergoz/Desktop/PegSolitarie-rm/.planning/reports/reduced-motion-live-gate/ux-designer.md`: this report.

## Checks
This role has no Bash: no Gradle was run, nothing was committed, and the producer commits.
- Read `.claude/rules/VERSION` (0.6.2) and the plugin's `plugin.json` (1.0.0). The snapshot is older, so the plugin rule files were read in full — pass.
- Read `HintUndecidedNotice.kt:100-209`:
  - the timer is outside the gate's branch (`:163-168` against `:171-188`);
  - only the node and the entrance change on a flip;
  - no flip direction drops a showing notice

  — confirmed.
- Read `HomeBlobs.kt`: the KDoc says ±16dp over about 8 s, at low alpha, and `:33` skips the blobs entirely when the gate is off — read.
- Helper grep (Explore, haiku) over `docs/ux-spec.md` (6,817 lines before the edits):
  - `ANIMATOR_DURATION_SCALE` had 2 hits, at `:4788` (§10) and `:6264` (23e). Both are corrected, and the key now appears only inside the dated notes that quote the first wordings;
  - "3.0.1" and "known issue" had no prior hit;
  - "Remove animations" had no prior hit, so the term enters the spec here, matching QA's and the ruling's term for the user's own path

  — read.
- Read `docs/ux-spec.md:6250-6283` and `:6497-6579` (line numbers from before the edits). 23d's "keyed on the screen's own raise count", 23g's "neither shows nor announces it again", TB-6's "no event is spoken twice or late", the D-TBa amendment's "each is spoken once" and TB-8's rotation clause are the texts the §6 ruling rests on — confirmed.

## Open issues / risks
1. **3.0.0 ships the notice's repeat**, a known low defect (23d). The producer records it in STATUS.md. The tech-lead owes the 3.0.1 brief (ruling §6: node identity kept across a gate flip, 23e's one-frame exit kept, red first) and the §16 item of ruling §9 item 4. I have now opened that item, so it is owed.
2. **F1's acceptance rests on QA's likely cause** (the platform's delayed delivery to an app in the background). If the tech-lead finds the delay is the game's own, the reason "not the game's" falls, and the acceptance comes back to this role.
3. **The device count for §6 is still BLOCKED.** It is not needed for this ruling, but the 3.0.1 fix's QA needs an AVD on which TalkBack has been set up once by hand. `Pixel_9b_clean` was left with TalkBack and a 2-minute timeout on (QA's open issue 1, the producer's to route).
4. **Merge risk on `docs/ux-spec.md`.** The parallel licences-screen and colour-vision ux rulings may also append to the revision index at the same insertion point. The new entry says to keep both entries, as loop 4 ruled.
5. **Owed by others, not done here:** the tech-design text at the RM close (ruling §9). §14.1 row RM and §16 #28 can cite §10's order (agreement before speed) and the 15 s pass line.

## Handoff
Next: producer —
- commit `docs/ux-spec.md` and this report on `bugfix/reduced-motion-live-gate`;
- in STATUS.md, record:
  - §6 is ruled a defect, rated low, as a 3.0.1 item that does not block the cut;
  - F1 is ruled acceptable for 3.0.0;
  - 23e is reworded;
- queue the 3.0.1 notice brief with the tech-lead.

No founder question is drafted: nothing here is an art-direction choice between equals.

Next: tech-lead (the fresh sign-off) —
- 23d's new paragraph and §10's F1 ruling are the UX decisions the sign-off can cite;
- ruling §9 item 4 (a §16 item for open issue 3) is now owed;
- at the RM close, §16 #28 can take §10's order and pass line;
- later, the 3.0.1 brief against 23d's four terms.

Next: qa-engineer — nothing on this branch from this role. On the 3.0.1 fix: 23d's device count, in each direction, with the app in the foreground.
Next: android-game-dev — nothing on this branch from this role.
