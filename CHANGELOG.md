# Changelog

All notable changes to Peg Solitaire are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Why this convention.** Keep a Changelog is written for the person who uses the software, not the
person who wrote it — one section per release, grouped by *Added / Changed / Fixed / Removed*, one
bullet per delivered capability rather than one per commit. That is the right shape here: this
release carries 647 commits since `v1.0`, and a dump of even the 132 `feat` subjects would be a
log, not a changelog. Each release section therefore has two layers:

1. **the player's layer** — *Added / Changed / Fixed*, written for someone who plays the game and
   has never seen the code;
2. **Under the hood** — a short engineering layer for the developer reading this file in two
   years, kept separate rather than interleaved so the player's layer is never diluted by build
   plumbing. Keep a Changelog does not forbid extra subsections, and the alternative (two files)
   guarantees drift.

A third, much shorter layer lives outside this file: the Play release notes, capped at 500
characters per language, in `docs/store/`. Every claim there must map to a bullet here.

**This changelog starts at 3.0.0.** Earlier history — v1.0 Slice 1, phases 1–5 — is recorded in
`.planning/MILESTONES.md` and `.planning/milestones/v1.0-ROADMAP.md`. Back-filling a retrospective
1.0.0 section from 66 pre-studio plans would be archaeology with a low truth ratio.

**On the version number.** `.planning/` counts milestones and calls this one **v1.1**; the Play
listing counts published builds and sits at versionCode 2 / versionName "2.0" from an earlier,
different application. This rewrite's milestone v1.0 was tagged but never uploaded, so this is the
store's first sight of this codebase, and the store-facing number has to move forward from "2.0".
Milestone **v1.1 ships as product version 3.0.0**, tag `v3.0.0`, versionCode `30000`
(`X*10000 + Y*100 + Z`, `rules/play-store.md`). The full reasoning is in
`.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` §1.

---

## [3.0.0] — unreleased

*Date is filled in when `release/3.0.0` is merged and tagged; an unreleased section with a date is
a false claim. Sources: `.planning/ROADMAP.md` phases 6–15 (success criteria, written in outcome
language) and the phase summaries under `.planning/phases/*/`.*

*Entries marked **(pending: `<branch>`)** describe work on a branch that has not merged into
`develop` yet. When the branch merges, the marker is removed. If it does not reach
`release/3.0.0`, the entry is deleted at the release cut (checklist step 1.4), together with its
line in `docs/store/` (see `docs/store/README.md`). This keeps the 3.0.0 section complete without
claiming work that has not landed.*

A complete rewrite of Peg Solitaire, and the first build of this codebase to reach the store. The
game now has a voice, remembers you across devices, competes, and — where it was quietly guessing
before — tells you the truth.

### Added

- **A tutorial you play, not read** *(pending: `feature/phase-15-ux-refresh`)*. A first-time player
  learns on a live board: every step needs a real jump, and it covers both jump directions before
  it ends. A wrong tap costs nothing, with no error and no red. Three taps without progress bring
  more help, never a block. Skip is on every step, system back does the same, and the tutorial does
  not come back on the next launch. Replay it from Settings › General.
- **A colorblind palette** *(pending: `feature/candy-backdrop`)*. A switch in Settings › General, off
  by default. Turned on, it makes the selection, the legal targets, the hint marks and the stars
  readable by brightness and pattern as well as colour, for red-green and blue-yellow colour blindness
  and for full colour blindness alike.
- **A Licenses screen** *(pending: `feature/candy-backdrop`)* in Settings › About lists the fonts,
  sounds and open-source software the game is made with, and opens their licence texts in the app,
  offline.
- **Sound.** Every meaningful action makes a sound: a pitch-varied jump that rises through a run,
  distinct peg removal, peg pick-up and put-down, undo, a quiet non-punishing tap on an illegal
  move, button presses, the win, and a rising star-pop that lands in step with the Results
  celebration. It plays at system media volume and **does not interrupt your music or your
  podcast** — nothing ducks, nothing pauses. The existing sound switch silences the game
  immediately, including cues already queued.
- **Play Games sign-in, on your terms.** A single dismissible prompt the first time you open the
  game asks whether to sign in. Accept and you are signed in silently on every later launch, with
  no wall and no pause before you can start a level. Decline and **everything still works** —
  every mode, every board, every feature; the social surfaces are simply hidden. If you change
  your mind, a Settings row appears while you are signed out.
- **Leaderboards.** Seven of them: your highest Challenge level, your cumulative Challenge score,
  and a best-score ladder for each of the five Classic boards. Open them from inside the game. If
  you played offline first, your existing bests are submitted once when you sign in.
- **Achievements.** Twenty, including four reachable in your first hour, and skill awards for
  solving a board without a hint, without an undo, and finishing with the last peg in the centre.
  Ladder position and star total tick incremental achievements forward as you play, and the unlock
  popup appears the moment one is earned.
- **Cloud save.** Your per-level bests and your Challenge ladder position follow your Play Games
  account to a new device. Play on two devices and you end up with the **union** of both — highest
  ladder, best stars, best scores, fastest times — with no prompt and nothing lost. The save shows
  up legibly in the Play Games saves list with a description, played time and a cover image. If
  the cloud is unreachable, the game quietly keeps playing from the device: no error wall, no lost
  progress.
- **Switching accounts is asked once, and never destroys anything by accident.** Signing in with a
  different Play Games account on a device that already holds progress shows one dialog: keep this
  device's progress, or use the new account's cloud progress. Backing out, tapping outside, or
  walking away all mean *keep* — the safe answer — and the dialog says exactly what "replace"
  replaces.
- **Extra hints and undos, only if you ask for them.** Each level gives you a free allowance of
  hints and undos. Only once it is spent does a dialog offer a rewarded ad, naming what you get;
  declining leaves the game exactly as it was. **An ad never plays on its own.** You can accept the
  offer straight from the "no winning move" banner, and the reward lands only when the ad reports
  it earned.
- **The clock stops for every ad.** The elapsed game clock is frozen for the whole time an ad is on
  screen and resumes when it is dismissed, so watching one can never cost you score. A hint that
  honestly answers "there is no winning move from here" costs neither an allowance nor an ad.
- **If an ad cannot load, you still get the assist.** No-fill, a failed load or a failed consent
  flow all hand you the hint or the undo anyway, with a brief explanation. There is no dead end.
- **Ads consent, before anything is requested.** On a fresh install in a region that requires it,
  the consent form appears before the game's home screen and before any ad is ever requested, and
  the ads system only starts up once consent resolves. Your privacy choices can be reopened from
  Settings wherever the rules require it.
- **The privacy policy is always one tap away** in Settings, on every install, not only in consent
  regions.
- **Between-level ads that are easy to ignore.** An interstitial can appear only after you tap
  Continue on the Results screen — never during the win celebration, never at level start, and
  never when you lose, abandon or restart. They are frequency-capped, fully suppressed through
  your first session and your first levels, and the cap survives the app being killed. A brief
  pause precedes each one so a tap meant for a button cannot land on the ad.

### Changed

- **A fuller Home screen** *(pending: `feature/phase-15-ux-refresh`)*: a title band, a three-pill
  progress ribbon, and board art on the Classic and Challenge cards, both cards in the lower half of
  the screen. The Challenge level, the Continue chips and the Classic stars are still on the first
  screen, with nothing to open. The info button is gone; its help is now the tutorial.
- **The gameplay screen has no top bar** *(pending: `feature/phase-15-ux-refresh`)*. A slim status
  strip at the top keeps a labelled back control and the timer. Undo, Hint, Show moves and Restart
  move to a dock in the lower half of the screen, within thumb reach. Show moves is now shown by
  fill, not by colour alone, Hint has its own new icon, and no two controls share an icon.
- **The game's typefaces are built in** *(pending: `feature/phase-15-ux-refresh`)*. Fredoka and
  Nunito ship inside the app, so the first screen is drawn in the right fonts on every device,
  offline and without Google Play services.
- **A new glossy look** *(pending: `feature/candy-backdrop`)*. A deep violet backdrop with soft
  colour waves and slowly drifting light sits behind the menu and the board, and the Home mode
  cards have a glossy finish. The board and the pegs are unchanged. The motion stops when the
  system's animations are turned off.
- **Hints tell the truth on the hard boards.** Asking for a hint from a difficult early position on
  European-37, Asymmetric-39 or Wiegleb-45 now returns a real winning move inside the same
  one-and-a-half-second deadline, instead of folding to the "no winning move" banner. It is
  answered from a built-in opening book for the first four moves and by a rebuilt solver beyond
  it, so "no winning move" now means *proven*, not *gave up*.
- **Star ratings are measured against what each board can actually reach.** Every board's star
  bands are derived from that board's own fewest-peg floor rather than from one shared assumption.
- **A hint you ask for while the board is changing is cancelled**, rather than arriving late and
  pointing at a position you have left.

### Fixed

- **The status bar was unreadable in the light theme** *(pending: `feature/phase-15-ux-refresh`)*.
  Its clock and icons were drawn light on the light background; they are now drawn dark there.
- **Diamond-41's stars were unreachable.** Its bands assumed a one-peg finish that this board
  cannot reach from its centre start; the floor is two pegs, now proven — by a committed 38-move
  line replayed through the game's own rules, and by an exhaustive proof that one peg is
  impossible. Ratings on Diamond-41 now reflect a target a player can actually hit.
- **"Next level" could send you backwards.** After solving a level behind your furthest position,
  Continue now serves the head of your Challenge ladder rather than the level after the one you
  just played.
- **A peg could be left squashed** at the end of a jump when the landing animation was interrupted.
- **A peg flickered at the top-left of the board** on the first move of a level.
- **The finished board is cleared** when moving to the Results screen instead of lingering behind
  it.
- **The star celebration's pacing and pitch** were reworked so the stars land one at a time rather
  than on top of each other.
- **The launcher icon** is scaled to sit correctly beside its neighbours on the home screen.
- **Sign-in was fragile in a dozen small ways** — a prompt that could flash back open, an error
  message that replayed, a tap that did nothing while a round trip was in flight, a cancel treated
  as a failure instead of a decline. All of them are gone.

### Under the hood

*Nothing in this section is visible while playing; it is here for the next person to read the
code.*

- *(pending: `feature/phase-15-ux-refresh`)* The downloadable Google Fonts path is removed: the
  `GoogleFont` provider, `font_certs.xml` and `ui-text-google-fonts`. Five static OFL-1.1 font
  files load blocking from `res/font`, and their two licence texts ship in `assets/licenses/`
  (`THIRD_PARTY.md` §2).
- Release builds are minified and resource-shrunk by R8, with the sound files, the opening books
  and the downloadable-font certificates proved to survive the shrink.
- Ad-unit and Play Games identifiers resolve from build configuration — test identifiers in debug,
  real ones in release — with build gates that fail a release build outright when a real
  identifier is missing, and that refuse to let an identifier literal into shipped source.
- The Play Games, ads and consent SDKs sit behind the `AdGateway` / `GamesService` / `CloudSaveStore`
  seams inside a `platform/` layer, enforced by a bytecode boundary test; `:domain` stays pure
  Kotlin with no Android dependency.
- The cloud merge is a pure `:domain` function proved commutative, associative and idempotent
  before any SDK code was written, and conflicts are resolved manually rather than by
  last-writer-wins, which silently deletes progress.
- Local storage migrated to a second schema version, with a test proving every existing best, star
  total and ladder position survives the migration.
- The release variant is signed with the Play upload key read only from environment variables; the
  build has no third state between "signed with the upload key" and "unsigned".
- Test determinism: the unit suite's DataStore fixtures run against an in-memory double rather
  than real file I/O, removing a wall-clock hang reproduced 5 times in 23 runs and absent in 52
  runs afterwards.
- Two studio-rule gaps are **open** at the time of writing and are release blockers, not silent
  omissions: `check` carries no coverage gate and no `FUZZ_SEEDS` fuzz harness, so the generator's
  solvability is asserted on sampled levels rather than at the 100,000-seed scale the release rule
  requires. Tracked as R-2 and R-7 in
  `.planning/reports/phase-14/tech-lead-signoff-r3-signing.md` §G.

[3.0.0]: https://semver.org/ "tag v3.0.0 — link replaced with the repository compare URL when the repository has a remote"
