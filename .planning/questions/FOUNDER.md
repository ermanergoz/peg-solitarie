# Founder questions (newest first)

## Q-902 — Do the in-game actions move to the bottom?   (open)
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
Answer:

## Q-901 — Home hero art direction   (open)
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
Answer:

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
