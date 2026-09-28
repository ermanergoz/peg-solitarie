# Founder questions (newest first)

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
