# Founder questions (newest first)

## Q-023 — Make the board playable with TalkBack in 3.0.0?   (open)
Date: 2026-10-21    Asked by: game-designer (via producer)
Context: With TalkBack on, the board can't be heard or played, in the game or the tutorial: a blind player can open 3.0.0
but never make a move. The studio rule is that every app meets its platform's accessibility rules before it ships; this
misses it. Making the board playable is one small branch that can run alongside the rest of the 3.0.0 work.
Options: 1) Make the board playable with TalkBack in 3.0.0; the release waits for it only if it is the last thing left
2) Ship 3.0.0 with the board named and its peg count spoken, but not playable; playable in 3.1
3) Ship 3.0.0 as it is; fix it later. (Options 2 and 3 ship below the studio's accessibility rule and need your explicit OK.)
Recommendation: game-designer picks 1 — the only option that meets the rule; likely costs no release time (runs in
parallel); under 2 and 3 a blind player can still spend hints and watch an ad for one they cannot use. If at Gate 2 the
board is the only item left, "wait, or ship option 2" goes into Q-PUBLISH-3.0.0. (.planning/reports/ux-spec-folds/game-designer-talkback-board.md)
Blocks: nothing now (the studio proceeds with 1 by default); release/3.0.0 reads the answer before its cut
Answer:

## Q-022 — The Turkish store listing describes the old app   (answered)
Date: 2026-10-18    Asked by: release-manager
Context: The live Play listing has its own Turkish text ("5 farklı oyun modu") written for the old app;
14 other languages show translated English (saved in the Console or automatic: only you can see which).
The new en-US copy is drafted in docs/store/ (feature/phase-14-release-docs) for your approval at Gate 2.
Options: 1) The studio drafts a tr-TR translation of the new copy for you to approve at Gate 2
2) Delete the Turkish translation so Turkish users see the English text (or Play's automatic translation)
3) You write the Turkish text yourself
Recommendation: release-manager picks 1 — a stale text describing features that are gone is a Play policy
exposure, and a drafted translation costs no extra round trip at Gate 2.
Blocks: nothing (the Gate 2 store-asset line)
Answer: 1 — the studio drafts tr-TR for approval at Gate 2 (2026-10-18).

## Q-021 — Which look should the menu and game backgrounds take?   (answered)
Date: 2026-10-18    Asked by: ux-designer
Context: You asked for Candy Crush's aliveness, not a copy: glassy, with something in the back. Three directions
are drawn and animated (Home and game, light and dark) in docs/ux/backdrop-directions.html on feature/candy-backdrop
(published privately for the founder). The board, layout and colours you kept stay as they are in all three.
Options: 1) A · Frosted glass  2) B · Deep gloss: a deep violet world with glossy waves, rising soft lights and gel
mode cards (the biggest change)  3) C · Airy daylight  4) None of these: tell us what to adjust
Recommendation: ux-designer picks 1 (closest to "glassy", keeps the cards, cheapest to build and run).
Blocks: candy-backdrop build
Answer: 2 — B · Deep gloss (2026-10-18). The research-analyst's addendum finds B clean on conditions A.3/A.4
(.planning/reports/candy-backdrop/research-analyst.md on feature/candy-backdrop).

## Q-020 — How much of the candy scene goes on the game screen?   (answered — direction reopened)
Date: 2026-10-17    Asked by: ux-designer
Context: The menu gets the full sweets landscape you asked for: frosted hills with drips and sprinkles, clouds,
and a lollipop, a cane, a gumdrop and a cupcake beside the board picture. The game screen gets a calmer version
so your eye stays on the pegs. All three options are readable and IP-safe; this is taste.
Options: 1) Calm game screen (default, being built): cotton-candy sky, clouds behind the title, frosted hills
with sprinkles under and behind the board; the standing sweets stay on the menu  2) Full scene on the game
screen too: the lollipop, cane, gumdrop and cupcake also stand on the hills around the board (on phones they
mostly peek out from below it; fully on tablets)  3) Plain game screen: sky and clouds only, no hills
Recommendation: ux-designer picks 1 — the game screen joins the same candy world while nothing stands next to
the board you are solving; 2 is the busiest frame, but on phones the sweets would mostly show as pieces
peeking out from behind the board and the buttons; 3 is calmest but loses the candy feel you asked for.
Blocks: nothing (option 1 is specified and buildable; 2 and 3 are constant changes, docs/ux/candy-backdrop.md §3.3)
Answer: none of the three (2026-10-17), verbatim: "I wanted you to get inspired by the aliveness of candy crush
not to copy it. galssy look with something in the back maybe? Idk but get my approval. Show me the design first".
So the sweets-landscape direction is reopened: the ux-designer shows visual directions (a glossy/glassy, lively
look with depth behind it) and the founder approves one before anything is built. The candy-backdrop build now
waits on that approval (next question, Q-021).

## Q-019 — Fable limit reached again; how do the Fable-pinned roles finish v1.1?   (answered)
Date: 2026-10-17    Asked by: producer
Context: Both Fable dispatches in flight died on HTTP 429 "You've reached your Fable limit": qa-engineer
phase-15 loop 3 (`req_011Cg8FSKsGNXgPkGc5fvo6t`, cut at its start) and ux-designer candy-backdrop spec
(`req_011Cg8FSCnNYKh3QkBpoYdpJ`, sections 0–2 of docs/ux/candy-backdrop.md written). code-auditor, qa-engineer,
tech-lead, ux-designer and game-designer are pinned to Fable; plugin 1.0.0 has no fallback. Q-003/Q-014
already allow Opus for the phase-15 review chain and rulings only.
Options: 1) Top up Fable (/usage-credits) and continue as staffed, no deviation 2) Wait for the Fable
allowance to reset 3) Opus for every Fable-pinned role until 3.0.0 is published, each report recording
the deviation (reviewers then share the developer's model)
Recommendation: producer — 1 if the spend is acceptable, since the code-auditor and qa-engineer are the
gates that make review independent of the Opus-written code; else 3, with fresh reviewers that run the
Check and their own mutation probes rather than trusting recorded results.
Blocks: candy-backdrop spec and build reviews, phase-14 R-1 QA and sign-off, check-gates reviews, the release QA
Answer: 3 — Opus for every Fable-pinned role until 3.0.0 is published (2026-10-17). Recorded as a deviation
from studio staffing in every report; mitigations: fresh reviewers, Check and mutation probes run by the reviewer.

## Q-018 — minSdk 24 → 26: when, if ever?   (answered)
Date: 2026-10-17    Asked by: tech-lead
Context: The studio rule names minSdk 26 for new products; this published app ships 3.0.0 at 24, and
raising it drops API 24–25 installs from updates. Technically nothing forces either (docs/tech-design.md §1.3).
Options: 1) raise to 26 in 3.0.0  2) raise in the first minor after 3.0.0 whose Play Console device
statistics show API 24–25 under 1 % of active installs  3) hold 24 indefinitely
Recommendation: tech-lead picks 2 — no technical cost is paid by waiting and no player is dropped on a guess.
The design records 24 as the ruling for 3.0.0 either way.
Blocks: nothing
Answer: 3 — keep minSdk 24 indefinitely, unless a technical need appears (2026-10-17). docs/tech-design.md §1.3 to record it.

## Q-017 — Store-art provenance: achievement icons, listing icon, feature graphic   (answered)
Date: 2026-10-17    Asked by: release-manager (Q-F3 / F-4)
Context: THIRD_PARTY.md §7 holds four PENDING FOUNDER rows for art that lives only in the Play Console;
rules/ip-policy.md blocks the release while a row lacks a licence.
Options: 1) I drew them 2) generated with an AI tool 3) downloaded / bought 4) not sure (studio redraws)
Recommendation: release-manager — an information request, not a decision.
Blocks: THIRD_PARTY.md completeness → cutting release/3.0.0
Answer: "you did it" (2026-10-17) — the studio produced them in its own sessions. The files are on the
founder's Desktop, dated 2026-09-14/15: `PegSolitaire Achievement Icons/` (20 PNGs, a01..a20),
`PegSolitaire Leaderboard Icons/` (7 PNGs), `peg-solitaire-icon-512.png`, `peg-solitaire-feature-1024x500.png`.
Rows to be written as own work by the release-manager, with the files' sha256 and how they were made.

## Q-016 — Store listing copy: rewrite for the new app, or keep?   (answered)
Date: 2026-10-17    Asked by: release-manager
Context: The live listing was written for the old app (versionCode 2), which had no Challenge ladder,
leaderboards, cloud save or sound. Screenshots are already settled: the studio recaptures them after
the candy backdrop lands. The copy and the ≤ 500-char release notes are taste.
Options: 1) The studio drafts new short + full descriptions and the release notes into docs/store/;
         you approve them at Gate 2  2) The current copy still fits; the studio drafts only the release
         notes  3) You write the copy yourself; the studio checks the limits and checks it against the changelog
Recommendation: release-manager picks 1. The rewrite is complete, so old copy almost certainly
describes features that are gone and misses the ones that are new. Approval at Gate 2 adds no extra
round trip.
Blocks: nothing mechanical; the store-asset line of the Gate 2 summary
Answer: 1 — the studio drafts new short + full descriptions and the release notes into docs/store/;
founder approves them at Gate 2 (2026-10-17).

## Q-015 — Play App Signing: keep your existing key or let Google make a new one?   (answered)
Date: 2026-10-17    Asked by: release-manager
Context: The first upload of 3.0.0 enrols the app in Play App Signing (D-21: not enrolled yet).
Enrolment is one-way. The key you choose signs every copy players get from now on.
Options: 1) Upload your existing release key as the app signing key; keep using it as the upload key
         2) Let Google generate a new app signing key; your existing key becomes the upload key only
Recommendation: release-manager picks 1. It keeps every copy of the old app installed today
upgradable to 3.0.0, and it keeps the Play Games credential already registered for that key's SHA-1
valid. Option 2 needs a new PGS credential before T-1 can pass, and gains nothing this release needs.
Blocks: the first upload (F-3, F-11); nothing before it
Answer: 1 — keep the existing release key as the app signing key and as the upload key (2026-10-17).

## Q-014 — Fable-pinned tech-lead and ux-designer rulings for the rest of phase 15   (answered)
Date: 2026-09-29    Asked by: producer
Context: The font fix (Q-013) needs a tech-lead ruling (bundle the OFL fonts vs repair the Google
Fonts provider) and the ux-designer has two open spec rulings (15p gap reading; the 15e "both arms
installed" precondition, E-1). Both roles are pinned to Fable, which is exhausted; 0.6.2 has no
fallback. Q-003 covered only the phase-15 review chain.
Options: 1) Opus for phase 15 2) Wait for Fable 3) Founder tops up Fable
Recommendation: producer — 1, so phase 15 can finish.
Blocks: phase-15 font fix and merge
Answer: 1 — Opus for the tech-lead and ux-designer rulings for the rest of phase 15 (2026-09-29),
each recorded as a deviation. Scoped to phase 15.

## Q-013 — The app's fonts have never loaded on any device; when do we fix it?   (answered)
Date: 2026-09-29    Asked by: producer (from qa-engineer F-A/F-B, qa-engineer-loop2.md)
Context: `Type.kt` builds `AppTypography` before the Fredoka/Nunito families exist, so every style
has a null family and Roboto renders everywhere, since 2026-08-26 (unreleased; `main` predates it).
The provider certificate in `font_certs.xml` is also corrupt. Every phase-15 fit measurement was
taken in Roboto.
Options: 1) Fix inside phase 15 and re-walk the fit criteria 2) Separate phase after merge
3) Drop the custom fonts
Recommendation: producer — 1.
Blocks: nothing
Answer: 1 — fix inside phase 15 (2026-09-29); founder sees screenshots in the real faces before merge.

## Q-005 — Home hero: how should the top of Home fill the screen?   (answered)
Date: 2026-09-28    Asked by: ux-designer
Context: Per your Q-004 answer the peg-cross logo goes, the title is re-seated, and the empty band
between the stats and the cards must go. A default is specified and will be built (docs/ux-spec.md
§4.2.4, D-03l); this only asks whether you prefer another direction. Stats, cards and colours stay.
Options: 1) Board stage — the title sits left-aligned beside the gear on one line; the space below
it holds a large clay board showing a game in progress, which grows or shrinks with the screen (not
drawn on small phones) 2) No art, one block — no picture anywhere; the title sits right on the stats,
and title + stats + Continue + mode cards form one block in the lower part of the screen, with only
the gear and the soft background blobs above 3) Bigger cards — no picture; title line as in 1; on
taller phones the mode cards and Continue chips grow taller (bigger art, bigger tap areas) to take
the space — this changes the mode cards you said you like
Recommendation: ux-designer picks 1 — it removes the gap on every phone without moving the mode
cards out of thumb reach, shows the actual game instead of a logo, and costs nothing at cold start
(one drawing, replacing the logo's). 2 leaves the top third of a tall phone empty, which is the
original "looks empty" complaint; 3 spends the space on the part you asked to keep.
Blocks: nothing (default 1 is specified and buildable)
Answer: 1 — board stage (2026-09-29). Founder confirmed the ux-designer's default (D-03l, §4.2.4).

## Q-004 — What bothers you about the new Home screen?   (answered)
Date: 2026-09-28    Asked by: producer
Context: Founder reviewed the phase-15 build on the emulator and said "I don't really like this
screen" (Home), plus two gameplay notes: the dock icons do not show whether they are on or off, and
the gameplay title and elapsed time are too small.
Options: 1) Empty middle gap 2) Stats ribbon 3) Mode cards 4) Overall look (multi-select)
Recommendation: none — taste; asked to scope the ux-designer's rework.
Blocks: nothing
Answer: Empty middle gap, the peg-cross logo, and how the title sits (2026-09-28). Stats ribbon,
mode cards and the overall palette/style were not selected — keep them.

## Q-003 — Fable credits are exhausted and the whole phase-15 review chain is staffed on Fable   (answered)
Date: 2026-09-28    Asked by: producer
Context: Phase 15 is code-complete (S1–S8 committed, each on a green `check`). Both reviewers died
on HTTP 429 "You've reached your Fable limit": code-auditor `req_011CfVursaCjxSYYoSr3Cco1`,
qa-engineer `req_011CfVutjoBky6dChQ5jGKZa`. Per the plugin's agent definitions, code-auditor,
qa-engineer, tech-lead and ux-designer are all `model: fable`, while android-game-dev is
`model: opus` — that split is what makes the audit independent of the author. Nothing merges to
develop without code-auditor PASS, qa-engineer PASS and tech-lead sign-off (rules/gitflow.md), so
the phase cannot close until this is resolved. Only the founder can buy credits.
Options:
  1) Top up Fable (`/usage-credits`) and re-dispatch the chain as staffed — no deviation.
  2) Substitute **Sonnet** for the three review roles on this phase. Independence from the author is
     preserved (Sonnet ≠ the developer's Opus), recorded as a deviation in each report and here.
  3) Wait for the Fable allowance to reset before reviewing.
Recommendation: producer — 1 if the spend is acceptable, because the code-auditor is the phase's most
consequential gate and it is deliberately staffed on a stronger reviewer; otherwise 2, which keeps
the property that actually matters (a different model from the one that wrote the code) and lets the
device session happen now. Never substitute **Opus** for the reviewers: that is the developer's own
model and would make the audit self-review in all but name.
Blocks: phase-15 review, tech-lead sign-off and the merge to develop. Phase-15 implementation is done
and committed, so nothing is lost by waiting.
Answer: **Use Opus for the review** (2026-09-28). The founder chose Opus after the producer's
recommendation against it was stated in this question, and reaffirmed it in a second message.
Recorded as a **deviation from studio staffing**: the reviewers now run on the same model as the
author (android-game-dev is `model: opus`), so rules/studio.md standing rule 1 is upheld only by its
other mechanisms — a freshly spawned agent per reviewer, receiving the requirements and the diff and
explicitly barred from the developer's reports, its reasoning and the studio log. Every phase-15
review report must carry this deviation. Mitigations applied by the producer: reviewers instructed to
run the Check command and the §10 prohibition commands themselves rather than trusting recorded
results, and to hunt specifically for assertions that cannot fail.

## Q-902 — Do the in-game actions move to the bottom?   (answered)
Date: 2026-09-21    Asked by: ux-designer
Context: You said "redesign the icons on the top" and "remove toolbar from the game" in one breath.
The code says those are one surface: `GameScreen.kt:502` is a single `TopAppBar` holding the back
arrow, the title and all five controls. So the spec reads it as one change and splits it.
Options: 1) Split — read-only status (back, title, timer) stays top, the four actions (Undo, Hint,
Show-moves, Restart) move to a thumb-zone dock at the bottom 2) Keep all four icons visually on
top, redrawn as a clay cluster
Recommendation: ux-designer picks 1 — rules/compose.md requires primary actions in the lower thumb
zone of a 6.7-inch phone, and Undo (the control a peg-solitaire player uses most) is currently in
the worst-reachable corner. Choosing 2 swaps §4.3 and §4.4 of docs/ux-spec.md and changes nothing else.
Blocks: nothing (default 1 is specified and buildable)
Answer: 1 — the split (2026-09-21). Founder confirmed: status strip on top (back, title, timer), four actions in a thumb-zone dock at the bottom.

## Q-901 — Home hero art direction   (answered)
Date: 2026-09-21    Asked by: ux-designer
Context: Home gains a titled hero band to fill the emptiness you named. Three treatments are
equally buildable; this is taste, and cold start is a measured gate (≤ 950 ms, PLAT-08 C1).
Options: 1) A static decorative peg cluster in Candy Clay 2) A slowly drifting mini board idling
behind the title 3) No art — a larger gradient title alone
Recommendation: ux-designer picks 1 — it costs no asset licence, cannot distract from the two mode
tiles, and adds nothing to the cold-start critical path. Option 2 competes with the two `HomeBlobs`
already drifting there and puts a running animation on that path. Option 3 is also step 3 of the
spec's degradation path, so if the cold-start measurement forces it, that answer is already built.
Blocks: nothing (default 1 is specified and buildable)
Answer: 1 — static decorative peg cluster (2026-09-21). Founder confirmed the default; no drifting mini-board, no title-only treatment.

## Q-007 — Refresh the rules snapshot from v0.5.1 to plugin 0.6.2 now?   (answered)
Date: 2026-09-29    Asked by: producer
Context: The installed plugin is 0.6.2 (latest release), but this product's `.claude/rules/` was the
v0.5.1 snapshot (CLAUDE.md still named v0.1.0). 0.6.2 changes studio.md (staffing, model policy,
hierarchy), gitflow, logging, reporting, memory, stacks and testing.
Options: 1) Refresh now on a sync branch, merge to develop, then into phase 15 2) After phase 15 merges
Recommendation: producer — 1, so every role from here on works to the current rules.
Blocks: nothing
Answer: 1 — refresh now (2026-09-29).

## Q-006 — Fable is exhausted; how does the Fable-pinned sound-designer run the audio review?   (answered)
Date: 2026-09-29    Asked by: producer
Context: The founder asked the sound-designer to review the game's sounds. The role is pinned to
Fable, the Fable allowance is exhausted (HTTP 429, req_011CfWPFThnPBGzB46L4c6zh), and plugin 0.6.2's
studio.md says a Fable step never falls back to another model. The producer also disclosed that on
2026-09-28 it ran the phase-15 ux-designer (Fable-pinned) on Opus by stretching Q-003 without asking.
Options: 1) Wait for Fable 2) Run it on Opus now 3) Founder tops up Fable
Recommendation: producer — 1, no deviation and nothing else blocked.
Blocks: audio-review
Answer: "run it on opus only for once" (2026-09-29). A **one-time** exception for this audio review
only: the sound-designer runs on Opus, recorded as a staffing deviation in its report. It is not a
standing policy change — the next Fable step that cannot get Fable fails and is queued here again.
The earlier ux-designer Opus run (phase 15, 2026-09-28) stands as a disclosed deviation.

## Q-002 — Four console checks: why Saved Games snapshots never upload   (answered)
Date: 2026-09-20    Asked by: producer
Context: On both emulators every save commits locally but never uploads (hasChangePending still true 9.5 h later, online);
leaderboards and achievements sync fine. The app matches the documented PGS v2 pattern; research found three documented
requirements beyond the toggle. Evidence: .planning/reports/phase-13/qa-engineer-e2-probe.md, research-analyst-snapshot-upload.md.
Options (answer each, in this order — (a) takes two minutes and says whether anything ever reached Google):
  a) drive.google.com (tester account) -> Settings -> Manage apps: is "Peg Solitaire" listed, and with a "hidden app data" size?
  b) Play Console -> Play Games Services -> Configuration: any unpublished / draft changes pending? When was Saved Games switched on (before or after the project was created on 2026-09-15)?
  c) Google Cloud Console (the project linked to the game) -> OAuth consent screen -> Data access / scopes: is `drive.appdata` in the list next to `games` and `games_lite`?
  d) one.google.com/storage for the tester account: is storage full?
  e) May QA clear Google Play services data on Pixel_9 only (Google's documented way to refresh a stale Saved Games activation)? It signs that emulator out of Google, so you would need to sign in on it again; Pixel_9a stays untouched as the control.
Recommendation: producer — answer a-d as found; yes to (e) only if a-d show nothing wrong.
Blocks: phase-13 close-out (UAT 10-13 re-run), phase-13 sign-off and merge
Answer: 2026-09-20 — "all set"; clarified: (b)-(d) all already correct, nothing changed (no draft PGS changes, drive.appdata on the consent screen, storage not full); (a) not done — the founder could not find Drive's Settings (direct link given: drive.google.com/drive/settings -> Manage apps), still open as a non-blocking check; (e) YES — QA may clear Google Play services data on Pixel_9 only. Producer re-read at 16:11 after the answer: still change_pending on both devices.

## Q-001 — Confirm Saved Games is enabled and the Play Games configuration is published   (answered)
Date: 2026-09-20    Asked by: producer
Context: QA's two-emulator pass found cloud save doing nothing: signed in and online, leaderboard/achievement
submissions drain but every Saved Games write is refused, so nothing restores (UAT 10-12 blocked, 13 partial).
That is how 13-OPERATOR-CHECKLIST.md Step 0 says an unset toggle presents; only the founder has Play Console access.
Options: 1) Step 0 was not done — enable Saved Games, publish the PGS configuration, answer "done" 2) Step 0 was already done and published — answer "already on"; the evidence then becomes an app defect in PlayGamesCloudSaveStore.save
Recommendation: producer — check the Console first (about a minute); either answer unblocks the next step.
Blocks: phase-13 close-out (13-11 tasks 2-3), phase-13 review and merge
Answer: 2 — already on (2026-09-20). Founder read the Play Console properties: Published; Saved games enabled; Firebase not linked. The toggle is not the cause; the refused save goes to the developer, starting with the tech-lead's CloudSaveOutcome reason code.

## Q-CONCEPT — Adopt the shipped Peg Solitaire concept as Gate 1   (answered)
Date: 2026-09-20    Asked by: producer
Context: This repo predates the studio: v1.0 shipped and v1.1 phases 6-12 were built under gsd alone, so
`.planning/approvals/concept.approved` was never written. /ergoz-soft:build requires it.
Options: 1) Approve the concept as already built (PROJECT.md / REQUIREMENTS.md are the concept of record) 2) Re-run /ergoz-soft:pitch and /ergoz-soft:design first
Recommendation: producer picks 1 — the game is published (versionCode 2) and 7 of 9 v1.1 phases are complete; a re-pitch would produce nothing the roadmap does not already hold.
Blocks: phase-13 close-out via /ergoz-soft:build, phase-14
Answer: 1 — approved as already built (2026-09-20, /ergoz-soft:approve concept 1)
