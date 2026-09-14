---
phase: 12-leaderboards-achievements
type: operator-checklist
status: executed (2026-09-15; UAT test 9 install half outstanding)
blocks: [PGSL-01, PGSL-02, PGSL-03, PGSL-06, PGSA-01, PGSA-04, PGSA-05]
generated_by: 12-08-PLAN.md
---

# Phase 12 Operator Checklist — Play Console leaderboards, achievements and IDs

## Why this exists

No Play Games Services project exists for this game yet (Phase 11 D-04). Plans 12-01 through
12-07 leave the app fully able to submit scores, unlock achievements and open the stock Play Games
UIs — against 27 identifiers that nobody has created. Every one of those 27 is resolved at build
time from your own untracked `local.properties` (or a same-named CI environment variable) into
`BuildConfig` (D-17), so executing this checklist touches no source file whatsoever; it is Play
Console work plus one gitignored text file.

Until it is executed, a debug build submits the harmless placeholder `UNCONFIGURED-<KEY>` for every
identifier — well-formed, inert, rejected server-side — and a release invocation refuses to
configure at all, naming every missing key. Four of ROADMAP Phase 12's five success criteria
therefore cannot be observed by anyone: a score landing on a real leaderboard, the stock
leaderboard and achievement UIs opening, and the unlock popup appearing. Those observations are
`.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests **4 through 9**, and this document is
their precondition.

Work top to bottom. Step 0 is ordered for a reason; the rest is data entry.

## Step 0 — Ordering (do these in this order or non-tester accounts break)

The Play Console UI enforces none of this ordering, and every one of these mistakes surfaces at
runtime as `ERROR_NOT_AUTHORIZED` or a silent `isAuthenticated() == false` — indistinguishable from
a code bug. Transcribed from PITFALLS.md Pitfall 7 and Pitfall 8, carried unchanged as 12-RESEARCH
Pitfall 11.

1. [x] **Create a Google Cloud project and enable the Google Play Games Services API on it.**
       Everything below hangs off this project; the Console links to it rather than creating it.
2. [x] **Create the Play Games Services project in Play Console and link it to this app**
       (package `com.erman.pegsolitarie` — the published store id with the "-arie" spelling; `com.app.pegsolitaire` is only the code namespace, see `app/build.gradle.kts`). Verify the numeric project ID is what lands in
       `PGS_PROJECT_ID` — the full `…apps.googleusercontent.com` client ID is a documented and
       common error, and a value containing dots is always wrong.
3. [x] **Configure and PUBLISH the OAuth consent screen in Google Cloud**, with scopes `games`,
       `games_lite` and `drive.appdata`. Publishing is a separate action from configuring. Without
       it, **no non-tester account can use any Play Games Services feature at all** — sign-in,
       leaderboards and achievements all fail for the public audience while working perfectly for
       you.
4. [x] **Create the Android credentials THROUGH PLAY CONSOLE, never directly in Google Cloud.** A
       client created in the Cloud console is not associated with this game's leaderboards and
       achievements, so sign-in appears to work and every submission silently goes nowhere. Create
       **two** credentials against the same package name:
       - the **debug keystore SHA-1** (`keytool -list -keystore ~/.android/debug.keystore -v`,
         alias `androiddebugkey`), so debug builds can sign in and submit — this is exactly why
         plan 12-04 deliberately lets a debug build honour a configured ID rather than hard-wiring
         a test value;
       - the **Play App Signing SHA-1**, taken from Play Console → **App integrity**. This is *not*
         the upload certificate. Registering the upload certificate is the classic failure: the
         local build signs in, the internal-testing download does not (PITFALLS.md Pitfall 8).
5. [x] **Fill in the game properties** — display name at minimum, plus the store icon and graphics
       the Console marks required before it will let you publish.
6. [x] **Add yourself, and every tester, to the tester list.** Stated on its own line because it is
       the single most expensive omission here:
       **the account that created the game does not automatically become a tester.**
       Propagation takes up to a couple of hours, so do this before you need it, not when a test
       fails.
7. [x] **Create the 7 leaderboards (Step 1) and the 20 achievements (Step 2), and upload all 20
       icons (Step 3).**
8. [x] **Publish the Play Games Services settings BEFORE publishing the app.** Google's own
       wording: publish the Play Games Services settings first, or players authenticating with
       non-tester accounts encounter errors.

Steps **3, 4, 6 and 8** are the four that produce failures indistinguishable from a code bug. If
sign-in or submission misbehaves later, re-read those four before opening a single Kotlin file.

## Step 1 — Create 7 leaderboards

All seven share the same configuration, and it is not negotiable: **Numeric** score format with no
custom unit, **0 decimal places**, and **larger is better** ordering. The app submits a plain
integer score to every one of them (`submitScore` takes a `long`), so a fractional format or a
smaller-is-better ordering would silently misrepresent every entry.

**A leaderboard's sort order, score format and decimal places are IMMUTABLE once the leaderboard is
published.** There is no migration and no edit. The only remedy for a wrong ladder ordering is to
delete the leaderboard and create it again, which destroys every score already on it and issues a
new ID you then have to re-record (12-RESEARCH A6). Check all three fields against the table below
before you press publish on each row.

| # | Key | Console display name | Description | Format | Sort | Decimals | Value the app submits |
|---|-----|----------------------|-------------|--------|------|----------|-----------------------|
| L1 | `PGS_LB_CHALLENGE_LADDER` | Challenge Ladder | The highest Challenge level you have solved. | Numeric | Larger is better | 0 | `MAX(CAST(levelId AS INTEGER))` over solved Challenge results (PGSL-01) |
| L2 | `PGS_LB_CHALLENGE_SCORE` | Challenge Total Score | The sum of your best score on every Challenge level you have solved. | Numeric | Larger is better | 0 | `SUM(bestScore)` over Challenge results (PGSL-02, D-07) |
| L3 | `PGS_LB_CLASSIC_ENGLISH33` | English-33 Best Score | Your best score on the English-33 board. | Numeric | Larger is better | 0 | Stored best score for English-33 (PGSL-03) |
| L4 | `PGS_LB_CLASSIC_EUROPEAN37` | European-37 Best Score | Your best score on the European-37 board. | Numeric | Larger is better | 0 | Stored best score for European-37 (PGSL-03) |
| L5 | `PGS_LB_CLASSIC_ASYMMETRIC39` | Asymmetric-39 Best Score | Your best score on the Asymmetric-39 board. | Numeric | Larger is better | 0 | Stored best score for Asymmetric-39 (PGSL-03) |
| L6 | `PGS_LB_CLASSIC_DIAMOND41` | Diamond-41 Best Score | Your best score on the Diamond-41 board. | Numeric | Larger is better | 0 | Stored best score for Diamond-41 (PGSL-03) |
| L7 | `PGS_LB_CLASSIC_WIEGLEB45` | Wiegleb-45 Best Score | Your best score on the Wiegleb-45 board. | Numeric | Larger is better | 0 | Stored best score for Wiegleb-45 (PGSL-03) |

Seven, and only seven. Per-level Challenge leaderboards are deliberately absent — Play Games caps a
game at 70 leaderboards and the Challenge ladder is unbounded, which is why the ROADMAP lists them
under Out of Scope. If the Console offers an optional **upper score bound** while creating a board,
setting a generous one is a cheap way to discard absurd submissions from a tampered client; a
local-first single-player game has no stronger anti-cheat available (12-RESEARCH, Security Domain).

## Step 2 — Create 20 achievements

Play Games' published constraints, all verified (12-RESEARCH A7): a game may define at most **400
achievements over its lifetime**; the **total point value of all achievements in a game may not
exceed 2,000**; **no single achievement may exceed 200 points**; and **every point value must be a
multiple of 5**. Google advises reserving headroom for achievements added in later updates, because
the 2,000 cap is a lifetime budget, not a per-release one.

**Total points: 1,130 of the 2,000 maximum — 870 points of headroom deliberately reserved for
achievements added after v1.1.**

All 20 are **visible**; none is hidden. PGSA-01 asks for visible achievements, and a hidden
achievement cannot serve as a first-hour early win because the player cannot see what to aim at.

**An achievement's type (standard vs incremental) and, for an incremental achievement, its total
number of steps are IMMUTABLE after publication.** The six incremental step totals in the table
below are the same numbers `AchievementThresholds` compiles into the app (`LADDER_TIER_1..3` =
10/50/100, `STARS_TIER_1..3` = 25/100/250). If the Console value and the app value disagree, the
Console value wins at runtime: the app will keep reporting absolute steps against a total it does
not know about, and the tier will either complete early or never complete at all.

| # | AchievementId | Key | Console name | Description | Type | Steps | Points |
|---|---------------|-----|--------------|-------------|------|-------|--------|
| A01 | `FIRST_SOLVE` | `PGS_ACH_FIRST_SOLVE` | First Clear | Solve your first puzzle, in either mode. | standard | — | 5 |
| A02 | `FIRST_THREE_STAR` | `PGS_ACH_FIRST_THREE_STAR` | Three Stars | Earn three stars on any level, in either mode. | standard | — | 10 |
| A03 | `CHALLENGE_LEVEL_5` | `PGS_ACH_CHALLENGE_LEVEL_5` | Getting Started | Solve Challenge level 5. | standard | — | 10 |
| A04 | `SPEED_SOLVE` | `PGS_ACH_SPEED_SOLVE` | Quick Hands | Solve any level in under 60 seconds. | standard | — | 25 |
| A05 | `HINT_FREE` | `PGS_ACH_HINT_FREE` | No Hints Needed | Solve a level without using a single hint. | standard | — | 25 |
| A06 | `UNDO_FREE` | `PGS_ACH_UNDO_FREE` | No Take-Backs | Solve a level without undoing a single move. | standard | — | 25 |
| A07 | `CENTER_FINISH` | `PGS_ACH_CENTER_FINISH` | Dead Center | Solve the English-33 board with the last peg in the center hole. | standard | — | 50 |
| A08 | `CLEARED_ENGLISH33` | `PGS_ACH_CLEARED_ENGLISH33` | English Cleared | Solve the English-33 board. | standard | — | 15 |
| A09 | `CLEARED_EUROPEAN37` | `PGS_ACH_CLEARED_EUROPEAN37` | European Cleared | Solve the European-37 board. | standard | — | 20 |
| A10 | `CLEARED_ASYMMETRIC39` | `PGS_ACH_CLEARED_ASYMMETRIC39` | Asymmetric Cleared | Solve the Asymmetric-39 board. | standard | — | 25 |
| A11 | `CLEARED_DIAMOND41` | `PGS_ACH_CLEARED_DIAMOND41` | Diamond Cleared | Solve the Diamond-41 board. | standard | — | 30 |
| A12 | `CLEARED_WIEGLEB45` | `PGS_ACH_CLEARED_WIEGLEB45` | Wiegleb Cleared | Solve the Wiegleb-45 board. | standard | — | 40 |
| A13 | `LADDER_10` | `PGS_ACH_LADDER_10` | Climbing | Reach Challenge level 10. | incremental | 10 | 25 |
| A14 | `LADDER_50` | `PGS_ACH_LADDER_50` | Ascending | Reach Challenge level 50. | incremental | 50 | 75 |
| A15 | `LADDER_100` | `PGS_ACH_LADDER_100` | Century Climb | Reach Challenge level 100. | incremental | 100 | 150 |
| A16 | `STARS_25` | `PGS_ACH_STARS_25` | Star Collector | Collect 25 stars across both modes. | incremental | 25 | 25 |
| A17 | `STARS_100` | `PGS_ACH_STARS_100` | Star Hoarder | Collect 100 stars across both modes. | incremental | 100 | 75 |
| A18 | `STARS_250` | `PGS_ACH_STARS_250` | Constellation | Collect 250 stars across both modes. | incremental | 250 | 150 |
| A19 | `GRAND_TOUR` | `PGS_ACH_GRAND_TOUR` | Grand Tour | Solve all five Classic boards. | standard | — | 150 |
| A20 | `PERFECTIONIST` | `PGS_ACH_PERFECTIONIST` | Perfectionist | Reach the minimum peg count — three stars — on all five Classic boards. | standard | — | 200 |

### First-hour achievements (D-02)

ROADMAP Phase 12 success criterion 3 requires **at least four achievements reachable in the first
hour**. These are those four, and they carry the four cheapest point values in the roster on
purpose, so the budget itself records which ones they are:

- **A01 First Clear** (5) — the first solved puzzle, either mode.
- **A02 Three Stars** (10) — the first three-star result, either mode.
- **A03 Getting Started** (10) — Challenge level 5, deliberately below the first ladder tier.
- **A08 English Cleared** (15) — the first per-board Classic unlock. A separate "first Classic
  board" achievement was considered and dropped as fully implied by the per-board unlocks (D-02).

If any of these four is renamed, made harder, or has its description changed to describe something
later in the game, criterion 3 stops holding.

### Notes on individual rows

- **A07 Dead Center is English-33 only**, and means the last remaining peg sits on the center hole
  `(3,3)` (D-01). Do not generalise the description to "finish on the starting hole" — European-37
  starts off-center, Diamond-41 provably cannot reach a single peg, and Challenge boards have no
  defined center, so the phrasing would promise something unreachable on four of the five boards.
- **A05 No Hints Needed and A06 No Take-Backs unlock on a solve in either mode** (D-01), Classic or
  Challenge. Every hint and every undo counts, free-allowance or rewarded (D-09).
- **A20 Perfectionist** means each board's stored best sits at that board's own minimum peg count,
  which is exactly the three-star band — so Diamond-41's proven two-peg floor counts as perfect
  (D-04). The description says "minimum peg count" rather than "one peg" for that reason.
- **A13–A18 are driven by absolute step values** (`setSteps`, D-03 / D-12), never by increments.
  Resubmitting the same absolute aggregate is a no-op rather than a double-count, which is what
  makes the offline backfill safe to run repeatedly.
- **Descriptions must stay inside Play Console's limits**: achievement name at most 100 characters,
  description at most 500. Every row above is well inside both; if you reword one, keep it so.

## Step 3 — Upload 20 icons

One **512×512** PNG per achievement — **twenty unique images**, one for every row A01 through A20
above. No placeholders, no reuse.

**Without an uploaded icon, the stock unlock popup silently fails to render.** It does not error and
it does not log; it simply never appears (12-RESEARCH A2). That means **PGSA-04 cannot be verified
at all until all twenty icons are in place**, and it is the whole reason the ROADMAP lists the icons
as a Day-1 External Prerequisite with art turnaround as their lead time.

The roster size is asserted in code — `AchievementCatalogTest` requires exactly 20 `AchievementId`
entries — so the icon count, the Console entity count and the enum can never legitimately drift
apart from 20. If you find yourself uploading a nineteenth or a twenty-first, something upstream is
wrong.

## Step 4 — Record the 27 generated IDs

**Write the generated IDs into your own gitignored `local.properties` (or the same-named CI
environment variable) and nowhere else. Never paste an ID value into this file, into any file under
`.planning/`, or into any file under `src/main`.**

These are not cryptographic secrets — they ship inside the APK and anyone can read them out of a
published build — but a committed ID lets a forked repo write to this game's leaderboards and
achievements, and plan 12-04's `gamesIdLiteralCheck` build gate fails `check` on an ID-shaped
literal found anywhere under `src/main`.

Mechanics, all from plan 12-04 and the existing `local.properties.template` preamble:

- An **environment variable of the same name wins** over a `local.properties` entry for the same key.
- A value that is **absent, blank, or still prefixed `REPLACE_ME`** all count identically as "not set".
- **Debug tolerates all 27 being missing** — each resolves to `UNCONFIGURED-<KEY>`, the app builds,
  the whole JVM suite runs, and only submission is inert. Debug **honours** a value that is set, so
  filling these in means your debug builds write to the real leaderboards.
- **Only a release invocation fails**, and it fails at configuration time naming every missing key.
- **CI needs no new secret today**: the workflow never runs a release invocation. The names in the
  middle column are what a future release workflow will use, and they are identical to the
  `local.properties` key names — there is nothing to translate.

| local.properties key | CI secret / env var name | Identifies | ID recorded? |
|----------------------|--------------------------|------------|--------------|
| PGS_LB_CHALLENGE_LADDER | PGS_LB_CHALLENGE_LADDER | L1 — Challenge Ladder | [x] |
| PGS_LB_CHALLENGE_SCORE | PGS_LB_CHALLENGE_SCORE | L2 — Challenge Total Score | [x] |
| PGS_LB_CLASSIC_ENGLISH33 | PGS_LB_CLASSIC_ENGLISH33 | L3 — English-33 Best Score | [x] |
| PGS_LB_CLASSIC_EUROPEAN37 | PGS_LB_CLASSIC_EUROPEAN37 | L4 — European-37 Best Score | [x] |
| PGS_LB_CLASSIC_ASYMMETRIC39 | PGS_LB_CLASSIC_ASYMMETRIC39 | L5 — Asymmetric-39 Best Score | [x] |
| PGS_LB_CLASSIC_DIAMOND41 | PGS_LB_CLASSIC_DIAMOND41 | L6 — Diamond-41 Best Score | [x] |
| PGS_LB_CLASSIC_WIEGLEB45 | PGS_LB_CLASSIC_WIEGLEB45 | L7 — Wiegleb-45 Best Score | [x] |
| PGS_ACH_FIRST_SOLVE | PGS_ACH_FIRST_SOLVE | A01 — First Clear | [x] |
| PGS_ACH_FIRST_THREE_STAR | PGS_ACH_FIRST_THREE_STAR | A02 — Three Stars | [x] |
| PGS_ACH_CHALLENGE_LEVEL_5 | PGS_ACH_CHALLENGE_LEVEL_5 | A03 — Getting Started | [x] |
| PGS_ACH_SPEED_SOLVE | PGS_ACH_SPEED_SOLVE | A04 — Quick Hands | [x] |
| PGS_ACH_HINT_FREE | PGS_ACH_HINT_FREE | A05 — No Hints Needed | [x] |
| PGS_ACH_UNDO_FREE | PGS_ACH_UNDO_FREE | A06 — No Take-Backs | [x] |
| PGS_ACH_CENTER_FINISH | PGS_ACH_CENTER_FINISH | A07 — Dead Center | [x] |
| PGS_ACH_CLEARED_ENGLISH33 | PGS_ACH_CLEARED_ENGLISH33 | A08 — English Cleared | [x] |
| PGS_ACH_CLEARED_EUROPEAN37 | PGS_ACH_CLEARED_EUROPEAN37 | A09 — European Cleared | [x] |
| PGS_ACH_CLEARED_ASYMMETRIC39 | PGS_ACH_CLEARED_ASYMMETRIC39 | A10 — Asymmetric Cleared | [x] |
| PGS_ACH_CLEARED_DIAMOND41 | PGS_ACH_CLEARED_DIAMOND41 | A11 — Diamond Cleared | [x] |
| PGS_ACH_CLEARED_WIEGLEB45 | PGS_ACH_CLEARED_WIEGLEB45 | A12 — Wiegleb Cleared | [x] |
| PGS_ACH_LADDER_10 | PGS_ACH_LADDER_10 | A13 — Climbing | [x] |
| PGS_ACH_LADDER_50 | PGS_ACH_LADDER_50 | A14 — Ascending | [x] |
| PGS_ACH_LADDER_100 | PGS_ACH_LADDER_100 | A15 — Century Climb | [x] |
| PGS_ACH_STARS_25 | PGS_ACH_STARS_25 | A16 — Star Collector | [x] |
| PGS_ACH_STARS_100 | PGS_ACH_STARS_100 | A17 — Star Hoarder | [x] |
| PGS_ACH_STARS_250 | PGS_ACH_STARS_250 | A18 — Constellation | [x] |
| PGS_ACH_GRAND_TOUR | PGS_ACH_GRAND_TOUR | A19 — Grand Tour | [x] |
| PGS_ACH_PERFECTIONIST | PGS_ACH_PERFECTIONIST | A20 — Perfectionist | [x] |

Nothing goes in the **ID recorded?** column except a tick. The IDs themselves live only in your
untracked `local.properties`.

## Step 5 — Before calling Phase 12 done

- [x] All 7 leaderboards and 20 achievements created, 20 icons uploaded, PGS settings **published**.
- [x] All 27 IDs recorded in the untracked `local.properties`; `git status` shows nothing new.
- [x] `./gradlew :app:assembleRelease` configures without a missing-ID failure (`verifyReleaseGamesIds`).
- [ ] (tests 4–8 pass, test 9 install half pending) `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests **4 through 9** executed on a
      device with Play Games and a tester account, results recorded.
