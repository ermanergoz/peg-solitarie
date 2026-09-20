# research-analyst report — phase 13 snapshot upload

**Ranked causes and checks (section E, short form).** These are hypotheses, not findings: nothing here observes the server, and no source says why a committed snapshot would stay `hasChangePending()==true`.

| # | Cause | Basis | Conf. | Cheapest check | Who |
|---|---|---|---|---|---|
| 1 | Play services on each emulator holds a cached copy of the game's configuration from before Saved Games was active | Documented mechanism (24-hour activation; "clear Google Play services data" to test now). Applying it here is my inference. | ~35% | Clear Play services data on one emulator, sign in again, solve, re-read the probe | QA + founder (sign-in) |
| 2 | The Saved Games toggle was saved but is still in an unpublished draft of the PGS configuration | Documented that PGS changes are drafts until published | ~15% | Look for pending or draft changes on the PGS Configuration page | founder |
| 3 | The OAuth consent screen in the linked Cloud project lacks the `drive.appdata` scope | Documented requirement. No source for what happens when it is missing. | ~15% | Cloud Console → OAuth consent screen → scopes list | founder |
| 4 | Play services upload is slow or stuck | Community reports of 20-minute to 1-hour lags | ~20% | Re-read the probe after 1–24 hours with no writes; tap the picker's Refresh | QA |
| 5 | The account's Drive storage is full | Older Google wording says saves count against Drive quota | ~5% | one.google.com/storage | founder |
| 6 | The "deprecated v1 SDK" warning | No source found. The text is not in the 22.0.0 AAR. | — | none; I judge it unrelated to sync | — |

- **What is excluded:** the tester list and the Games API. The game is published, and leaderboards and achievements sync on both devices.
- **What weakens causes 1–3:** the single successful transfer at 03:12 shows the service worked end to end at least once.
- **Fastest server-side check with no code:** the founder opens drive.google.com → Settings → Manage apps. If uploads happened, the game is listed with a "hidden app data" size.

Date: 2026-09-20
Branch: feature/phase-13-cloud-save (fca4509..fca4509 — nothing committed, no file written or edited)
Skills loaded:
- ergoz-soft:standards (role research-analyst). I read the plugin copies of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md and ip-policy.md, plus the repo's `.claude/rules/lessons.md`.
- I did not read puzzle-design.md or play-store.md, because no design or release decision is made here.
- I did not invoke research-open-source, research-competitive or market-research. This is a technical prior-art question about a Google SDK, with no competitor, market or reusable-code element.
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; `.planning/recall/RECALL.md` does not exist)

My harness forbids writing report files, so this message is the full content. The intended paths are `.planning/research/phase-13-snapshot-upload.md` and `.planning/reports/phase-13/research-analyst-snapshot-upload.md`; the producer can save it as was done for the QA and tech-lead reports.

## Summary
- Google documents three requirements beyond the Play Console toggle:
  - an activation delay of up to 24 hours, with "clear Google Play services data" as the way to test immediately;
  - PGS configuration changes stay drafts until they are published;
  - the OAuth consent screen's scope list must include `drive.appdata`.
- Saved games are stored in the player's Google Drive application-data area under the `drive.appdata` scope. In PGS v2 that scope is a default, so the app has no scope to request in code.
- No source describes this exact failure pattern: commit succeeds locally, `hasChangePending()` stays true forever, and no error appears. The nearest matches are unresolved community reports.
- Saved Games is not in the PGS deprecation schedule. The exact warning text appears nowhere on the web, and it is not inside the games-v2 22.0.0 AAR.
- Server-side verification without app code is possible through Drive "Manage apps". It is also possible through the REST `snapshots.list` call, but that needs a token carrying the `drive.appdata` scope.

## A. Requirements beyond the Play Console toggle

**Documented facts**
1. **Activation delay and on-device cache.** "After performing the steps, it may take up to 24 hours for Google Play Games Services to activate the Saved Games service for your game." To test immediately, the page says to clear data in the Google Play services app (Settings → Apps → Google Play services → Manage Space → Clear All Data). Source: https://developer.android.com/games/pgs/console/enable-features
   - *My inference:* that remedy implies Play services caches whether Saved Games is enabled for the game, per device. A cache filled before activation would not pick up the toggle.
2. **Publishing.** PGS configuration changes are drafts until published. The setup page says "publish the Play Games Services settings first before you publish your game" and names the "Publish Game" option. Source: https://developer.android.com/games/pgs/console/setup
   - The page does not say whether a property edited on an already-published game needs a new publish. *No source found* on that point.
3. **OAuth consent screen scopes.** "The final list of scopes needs to include `games`, `games_lite`, and `drive.appdata`; none of these scopes will require app verification." The consent screen must also be available to everyone the game is available to. Source: https://developer.android.com/games/pgs/console/setup
4. **Storage and scope.** "Play Games Services has a unique scope for games (`games-lite`) and relies on another scope (`drive.appdata`) if your game uses the saved games feature. The saved games feature gives access to the user's Google Drive account, which is where the game data is stored." Source: https://developer.android.com/games/pgs/platform-authentication
5. **Default scopes in v2.** The server-access page lists `DRIVE_APPFOLDER` and `GAMES_LITE` as the default scopes; only EMAIL, PROFILE and OPEN_ID are extras. Source: https://developer.android.com/games/pgs/android/server-access
   - I checked the 22.0.0 AAR locally: `gamessignin/AuthScope.class` contains both `drive.appdata` and `games_lite`.
   - The v2 Saved Games guide obtains the client with `PlayGames.getSnapshotsClient(activity)` and mentions no scope. Source: https://developer.android.com/games/pgs/android/saved-games
   - So the app's sign-in (detail 4 of the brief: no `GoogleSignInOptions`, no Drive scope) is the documented v2 pattern and is not a defect.
6. **Cloud APIs.** The only API the setup page tells you to enable is `games.googleapis.com`. Source: https://developer.android.com/games/pgs/console/setup
   - *No source found* saying the Drive API must be enabled in the Cloud project for Saved Games.
7. **Testers.** They are required only while the game is unpublished; without them, accounts get OAuth and 404 errors. Source: https://developer.android.com/games/pgs/console/setup
   - This does not apply here: the game is published and achievements sync.
8. **v2 Drive dependency.** games-v2 21.0.0 (2025-06-23) "Removed the deprecated Google Drive dependency" and "Removed the deprecated Google Sign-In dependency". Source: https://developers.google.com/android/guides/releases

**Behaviour when a requirement is missing: no source found.** No official page says that commit succeeds locally and the upload silently never happens. What is documented:
- Offline, `commitAndClose` "stores the saved game data locally", and Play Games Services syncs it after the device reconnects. Source: https://developer.android.com/games/pgs/android/saved-games
- The server update is asynchronous. Source: https://developer.android.com/games/pgs/savedgames
- Both statements are consistent with a silent failure, but neither proves one.
- The one community datum is Unity issue #2744 (Saved Games enabled after publishing). It shows a hard plugin error, not a silent failure, and records no resolution. Source: https://github.com/playgameservices/play-games-plugin-for-unity/issues/2744

## B. Known causes of `hasChangePending()` staying true

| Candidate | Status | Source |
|---|---|---|
| Activation delay of up to 24 hours, and a Play services cache that needs clearing | documented | https://developer.android.com/games/pgs/console/enable-features |
| Stale grant from before the toggle, fixed by revoking access and re-consenting | no source found | The documented remedy is clearing Play services data. A "delete individual game data" page exists at play.google.com/games/profile, but it deletes data and is not documented as resetting consent: https://support.google.com/googleplay/answer/9130646 |
| Slow or stuck upload | community reports | android-basic-samples #168 shows `pending=true`, `commitAndClose` returning OK, and one-directional sync. The reporter says it worked about 1 hour later. No Google answer; the repository was archived on 2026-04-19: https://github.com/playgameservices/android-basic-samples/issues/168 |
| Stale reads on the second device that are "eventually correct after a few saves" | community report (plugin v11, still open) | https://github.com/playgameservices/play-games-plugin-for-unity/issues/3167 |
| A roughly 20-minute lag before new data can be loaded | community report, seen only as a search-result summary; I did not open the thread | https://discussions.unity.com/t/google-play-games-cloud-save-is-not-saving/763888 |
| Emulator-specific sync limits | no source found | — |
| Play services background-sync conditions (charging, idle, metered) | no source found | Docs only say the update is "asynchronous" |
| Drive storage full | weak | A search-result summary of Google's older concept page said saves are "counted against the player's Google Drive quota". The current page says only that the developer is not charged: https://developer.android.com/games/pgs/savedgames |
| Workspace or child account restrictions | no source found | — |
| A Play Games profile setting that disables saves | no source found | The help page lists only the auto-sign-in setting and clearing data: https://support.google.com/googleplay/answer/9275812 |

## C. The "deprecated v1 SDK" warning
- **Deprecation schedule (documented):**
  - v1 SDK deprecated in September 2025;
  - v1 APIs removed from the SDK on 2026-06-15 (`play-services-games:25.0.0` is an empty library);
  - Google Sign-In removed in September 2026;
  - v1 shutdown in May 2027 (the same page also says June 2027).
  - Sources: https://developer.android.com/games/pgs/deprecation and https://developers.google.com/android/guides/releases
  - The schedule does not mention Saved Games, Snapshots or `getSelectSnapshotIntent`.
- **Saved Games in v2 (documented):**
  - the v2 migration guide maps `SnapshotsClient` as a current class and lists Saved Games as something to test: https://developer.android.com/games/pgs/android/migrate-to-v2
  - the REST Snapshot reference carries no deprecation note: https://developer.android.com/games/services/web/api/rest/v1/snapshots
  - So Saved Games is not documented as deprecated.
- **Exact warning text:** no source found on the web.
- **Local fact:** a byte search of all classes in the 22.0.0 AAR finds no "deprecated Play Games" or "v1 SDK" string. The SDK builds only the log tag format (`PlayGamesServices` plus `%s[%s]`, in `internal/games_v2/zzfu`). The app's build has only `play-services-games-v2`, with no v1 artifact.
- **My inference (low confidence):** the message text comes from the Play services side, and the snapshot picker intent is still flagged as a v1 surface there. QA saw it only on the picker path and never on open or commit, so nothing ties it to the sync failure.

## D. Verifying an upload server-side

| Method | Code change? | Who | Status |
|---|---|---|---|
| Drive web → Settings → Manage apps. The game shows a "hidden app data" size if anything was uploaded. | none | founder | Documented that saves live in the Drive app-data area (https://developer.android.com/games/pgs/platform-authentication). The Manage-apps UI is community-described: https://www.overdrive.tools/blog/hidden-app-data-google-drive. Seeing no entry at all would itself be a finding. |
| Picker Refresh: QA's untried step, reloading Google's own UI from the server | none (the debug build already exists) | QA | My inference about what Refresh does; no source |
| REST `GET games/v1/players/me/snapshots`. It returns `uniqueName`, `lastModifiedMillis`, `description` and `driveId`. | none in the app | founder or developer | Documented. It needs an OAuth token with `drive.appdata` plus `games` or `games_lite`: https://developer.android.com/games/services/web/api/rest/v1/snapshots/list. *My inference:* the token must come from an OAuth client linked to the game, for example the game-server credential used in the OAuth Playground. The server-access page lists Players, Achievements, Leaderboards and Scores calls and does not list Snapshots: https://developer.android.com/games/pgs/android/server-access |
| Play Games app | — | — | No saves screen exists (tech-lead report, section E); no source otherwise |
| Play Console | — | — | No source found for any per-player snapshot view |
| On-device `hasChangePending()==false` | already built (E2 probe) | QA | Documented meaning: https://developers.google.com/android/reference/com/google/android/gms/games/snapshot/SnapshotMetadata |

## E. Ranked causes (full)
1. **Stale Play services game-config cache on each emulator (~35%).**
   - The mechanism is documented; applying it here is my inference.
   - For it: the project memory says the PGS project dates from 2026-09-15, and Pixel_9a's guest disk was rolled back to that same date. It is unknown whether the account first authorised the game before or after the toggle. Pixel_9 has never registered the game in its Play Games Library.
   - Against it: the 03:12 transfer.
   - Check: the founder states when the toggle was switched. QA then clears Play services data on Pixel_9 only, which forces a founder sign-in again, solves one board, and reads `change_pending`. Pixel_9a stays untouched as the control.
2. **Slow or stuck upload (~20%, community).**
   - Check: zero-cost waiting. QA leaves both emulators online and idle, re-reads the probe at +1 hour and the next morning, and taps Refresh in the picker.
   - If `change_pending` flips with no action, causes 1–3 are out.
3. **Toggle still in an unpublished draft (~15%, documented mechanism).** Check: the founder opens the Play Console PGS Configuration page and looks for unpublished changes.
4. **Consent screen lacks `drive.appdata` (~15%, documented requirement; the behaviour when it is missing is unknown).** Check: the founder opens Cloud Console → OAuth consent screen (Data access) for the linked project.
5. **Drive quota full (~5%).** Check: the founder opens one.google.com/storage for the tester account.
6. **Account type, profile setting, emulator limits:** no source found, so I leave them unranked.
   - A physical device would separate "emulator" from everything else, but none is attached.

The confidences are my judgement, not measurements, and they total under 100% because unknown causes remain.

## Outputs
- No file was written, because the harness forbids it. This message holds the full content for `/Users/ermanergoz/Desktop/PegSolitarie/.planning/research/phase-13-snapshot-upload.md` and `/Users/ermanergoz/Desktop/PegSolitarie/.planning/reports/phase-13/research-analyst-snapshot-upload.md`.
- There are no `THIRD_PARTY.md` or `SKILLS.md` rows: nothing was borrowed and no skill was adopted.
- Scratch copy of the SDK classes used for the string search, outside the repo: `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/4448ee25-b1e1-4d08-aeed-9032eea87236/scratchpad/ra-gv2/`.

## Checks
- Read `qa-engineer-e2-probe.md`, `qa-engineer-e1-convergence.md` and `tech-lead-non-convergence-triage.md` — pass (the evidence is used as given and not re-derived).
- Python byte search of `play-services-games-v2-22.0.0` `classes.jar` for `deprecated Play Games`, `v1 SDK`, `drive.appdata` and `games_lite` — pass (warning text: 0 hits; scope strings: `gamessignin/AuthScope.class` only).
- `grep -rn "play-services-games" gradle/libs.versions.toml app/build.gradle.kts` — pass (only games-v2 22.0.0).
- 13 WebFetch and 14 WebSearch calls — pass with gaps:
  - `developer.android.com/games/pgs/console/testpub` returned 404;
  - the Google support thread 400602852 came back truncated;
  - the `AuthScope` reference page rendered only as an index.
- `./gradlew check` — not run (no source changed). Emulators and Play Console were not touched.
- Quality gate "every claim carries a URL" — pass, except the items marked "no source found", "local fact" or "inference".
- License or IP verdicts — not applicable; nothing is to be reused.

## Open issues / risks
- The 03:12 transfer is unexplained by every configuration-type cause. It keeps causes 1, 3 and 4 below 50%.
- Clearing Play services data destroys the reproducible state and the pending local commits on that emulator. Do it on one device only, and only after the waiting check (cause 2) and the Drive "Manage apps" check.
- WebFetch answers come through a small summarising model. I did not read the quoted sentences on the source pages myself, so the founder or tech-lead should open the three key pages before acting: enable-features, console/setup and platform-authentication.
- The release-notes page dates 22.0.0 to 2026-07-29 and lists a 22.1.0 on 2026-09-15 (adding PlayDuration and `recordEventImmediate`). Neither entry mentions snapshots.
- I propose a founder question. It is non-blocking, and the producer would file it; I wrote nothing to FOUNDER.md:
  - **Q — Saved Games console checks.**
  - (1) When was Saved Games switched on, relative to creating the project on 2026-09-15?
  - (2) Are there unpublished PGS changes?
  - (3) Does the OAuth consent screen list `drive.appdata`?
  - (4) Does Drive → Manage apps list the game with hidden data?
  - (5) Is the tester account's storage full?
  - Blocks: nothing. Recommendation: do (4) first; it takes two minutes and tells you whether anything reached the server.

## Handoff
Next: producer — route to the tech-lead to pick the order of checks. I suggest: the Drive "Manage apps" check and QA's waiting and Refresh check first; then the founder console checks; and only then clearing Play services data on Pixel_9. No code change is indicated by anything I found, because the app's v2 sign-in and snapshot usage match the documented pattern.

Sources:
- [Enable PGS features (Saved Games, 24 h, clear data)](https://developer.android.com/games/pgs/console/enable-features)
- [Set up Play Games Services (scopes, publish, testers)](https://developer.android.com/games/pgs/console/setup)
- [Platform authentication (drive.appdata, Drive storage)](https://developer.android.com/games/pgs/platform-authentication)
- [Server-side access (default scopes)](https://developer.android.com/games/pgs/android/server-access)
- [Saved games for Android (v2)](https://developer.android.com/games/pgs/android/saved-games)
- [Saved games overview](https://developer.android.com/games/pgs/savedgames)
- [Migrate to PGS v2](https://developer.android.com/games/pgs/android/migrate-to-v2)
- [PGS deprecation schedule](https://developer.android.com/games/pgs/deprecation)
- [Google Play services release notes](https://developers.google.com/android/guides/releases)
- [REST snapshots.list](https://developer.android.com/games/services/web/api/rest/v1/snapshots/list)
- [REST Snapshot resource](https://developer.android.com/games/services/web/api/rest/v1/snapshots)
- [SnapshotMetadata reference](https://developers.google.com/android/reference/com/google/android/gms/games/snapshot/SnapshotMetadata)
- [android-basic-samples #168](https://github.com/playgameservices/android-basic-samples/issues/168)
- [Unity plugin #3167](https://github.com/playgameservices/play-games-plugin-for-unity/issues/3167)
- [Unity plugin #2744](https://github.com/playgameservices/play-games-plugin-for-unity/issues/2744)
- [Unity Discussions: cloud save not saving](https://discussions.unity.com/t/google-play-games-cloud-save-is-not-saving/763888)
- [Google Play Help: restore game data](https://support.google.com/googleplay/answer/9275812?hl=en)
- [Google Play Help: delete Play Games data](https://support.google.com/googleplay/answer/9130646?hl=en&co=GENIE.Platform%3DAndroid)
- [Overdrive: hidden app data in Drive](https://www.overdrive.tools/blog/hidden-app-data-google-drive)
