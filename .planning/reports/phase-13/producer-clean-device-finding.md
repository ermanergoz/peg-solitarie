# producer note — a clean device restores from the cloud; the two test emulators are wedged
Date: 2026-09-20 16:38
Branch: feature/phase-13-cloud-save
Skills loaded: ergoz-soft:standards (role producer)
Lessons applied: none (0 active lessons)

## What happened
The founder's Google account on `Pixel_9` (emulator-5556) was damaged by the approved
`pm clear com.google.android.gms` (Q-002 e): the account survived but its token did not
(`UserRecoverableAuthException: BadAuthentication`), sign-in looped, and the founder removed the
account and could not re-add it. The founder authorised a new emulator.

I cloned `Pixel_9`'s hardware config into a **new AVD `Pixel_9_clean`** (same Android 36
`google_apis_playstore` image, own fresh `userdata-qemu.img` created 16:31 — verified: the config
has no shared disk path), booted it as `emulator-5556`, installed the same debug APK
(88c2fd7 build, `1c480a55cc614da0`), and the founder signed in to Google on it.
The old `Pixel_9` AVD is shut down, not deleted.

## The finding
On first launch, with no app data and no tutorial, the clean device **restored progress from the
cloud**: Challenge **level 9**, European-37 **3/3**, every other Classic board 0/3.
Its probe reads:

    play_games_cloud_last_pull_opened = True
    play_games_cloud_last_pull_change_pending = False        <-- nothing pending
    play_games_cloud_last_pull_conflicts_resolved = 0
    play_games_cloud_last_pull_last_modified_at = 03:14:22.272 (2026-09-20)

Notes: the Play Games companion app is NOT installed on this device and sign-in still worked
(silent/auto sign-in); GMS on this image is **25.07.34**, older than Pixel_9a's 26.15.33 and the
old Pixel_9's 26.08.34.

## What this establishes
1. **The server holds a snapshot and serves it.** Restore onto a fresh device works. The app's
   pull, decode and Room import are correct — "served but not merged" is now excluded.
2. **The server's copy is stale, from 03:14:22 today.** Its content (level 9 + European-37 only)
   matches the state the OLD Pixel_9 had at 03:12-03:14 in the UAT re-run, i.e. the last write that
   ever reached the server.
3. **Both test emulators have been wedged since ~03:14.** Each keeps `change_pending = true` for
   ever and its own local `last_modified_at` (Pixel_9a 04:53:00.9, old Pixel_9 04:55:19.6); neither
   value ever reached the server, across 9+ hours online and dozens of launches.
4. **The Play Console configuration is not the cause** (Q-002: consoles already correct), and
   neither is the account, the project, the network or the app's merge code.

## The new prime suspect
The wedge coincides with the only conflict activity of the session: at **03:12** Pixel_9a logged
`Found conflict for snapshot progress` twice in one launch, each followed ~1 s later by
`E/PlayGamesServices[ExtendedSnapshotsHelper]: Inconsistent DB:null/4` inside
`PlayGamesContentProvider.delete`. After that launch nothing from either device ever uploaded again.

Open code findings that touch exactly that path (code-auditor F-3/F-4, tech-lead A(iii)):
superseded conflict pairs are abandoned, and the give-up path closes only `unresolved.snapshot`
and never `conflictingSnapshot`. An abandoned/never-closed conflict snapshot leaving GMS with a
dangling conflict record is a coherent mechanism for "local writes never upload again".

This is a hypothesis, not a finding: nothing here observes GMS's internal state, and the
`Inconsistent DB` line was logged only by Pixel_9a, never by the old Pixel_9, which is equally wedged.

## Device state now
- `Pixel_9a` (emulator-5554): untouched control. Level 10; English-33, European-37, Asymmetric 3/3.
  Wedged since ~03:14.
- `Pixel_9_clean` (emulator-5556): fresh, signed in, restored level 9 + European-37. Has written
  nothing yet.
- Old `Pixel_9` AVD: shut down, account damaged, kept for forensics.
