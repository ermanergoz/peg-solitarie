# Changelog

All notable changes to Peg Solitaire are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Why this convention.** Keep a Changelog is written for the person who uses the software, not the
person who wrote it — one section per release, grouped by *Added / Changed / Fixed / Removed*, one
bullet per delivered capability rather than one per commit. That is the right shape here: this
release carries 1366 commits since `v1.0` (counted at the cut, `release/3.0.0` off `develop`
da886ed, 2026-11-01, checklist item 1.6), and a dump of even its 214 `feat` subjects would be a log, not a
changelog. Each release section therefore has two layers:

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

## [3.0.0] — 2026-11-01

*Dated 2026-11-01, when `release/3.0.0` was merged to `main` and tagged `v3.0.0`. Sources: `.planning/ROADMAP.md` phases 6–15 (success criteria, written in outcome
language) and the phase summaries under `.planning/phases/*/`.*

*Every branch this section describes merged into `develop` before `release/3.0.0` was cut
(checklist item 2.4a, 2026-11-01), so no entry carries a pending marker.*

*Re-cut 2026-11-01. The first `v3.0.0` (main a3f2221) was tagged locally and never uploaded or
pushed. The founder then put four more changes into 3.0.0 (Q-030, Q-031, Q-039, Q-040, D-05g,
D-03p/q): the Turkish interface, full-screen Gameplay and Results, the Home stage withdrawal with a
live Classic Continue chip, and the Challenge star bands with a 30-level difficulty shift. They are
marked "(re-cut)" below. `release/3.0.0` was cut again from `develop` 26b56bf and the tag moved to
the new merge. A second re-cut the same day added the Challenge star multiplier on the score (Q-042, from `develop` 25f23e0); the full Check and the release-tier fuzz were **skipped for that pass by the founder's decision**. A third re-cut (develop 0022a7d, 2026-11-02) moved Home's title to the top (Q-046), hid Home's system bars and added Play again on a solved Challenge, again with no tests, Check or QA by the founder's decision; `HomeHeadroomTest` and `HomeChipArrivalTest` still assert the superseded D-03q layout and fail under `./gradlew check`. **The founder took over QA and sign-off for these four changes (2026-11-01) and signed
off the release themselves;** no qa-engineer, code-auditor or tech-lead step ran on them.*

A complete rewrite of Peg Solitaire, and the first build of this codebase to reach the store. The
game now has a voice, remembers you across devices, competes, and answers hints on the big boards
from a built-in opening book.

### Added

- **A Turkish interface** (re-cut). On a phone set to Turkish, or with Turkish picked as the app's
  own language in the system settings, every screen, dialog, TalkBack label and the Licenses screen
  is in Turkish, in the familiar "sen" voice. The heading typeface, Fredoka, gains the letters
  Ğ ğ İ Ş ş so Turkish headings keep the same face as English ones.
- **A tutorial you play, not read**. A first-time player
  learns on a live board: every step needs a real jump, and it covers both jump directions before
  it ends. A wrong tap costs nothing, with no error and no red. Three taps without progress bring
  more help, never a block. Skip is on every step, system back does the same, and the tutorial does
  not come back on the next launch. Replay it from Settings › General.
- **A colorblind palette**. A switch in Settings › General, off
  by default. Turned on, it makes the selection, the legal targets, the hint marks and the stars
  readable by brightness and pattern as well as colour, for red-green and blue-yellow colour blindness
  and for full colour blindness alike.
- **A Licenses screen** in Settings › About lists the fonts,
  sounds and open-source software the game is made with, and opens their licence texts in the app,
  offline.
- **Play the board with TalkBack.** With TalkBack on, every cell of the board is a stop that says
  what is on it; select a peg and jump with a double-tap or from TalkBack's actions menu, and each
  jump is spoken with the pegs left. The tutorial speaks each step's board and instruction, and the
  Results screen reads its summary once.
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
  shows no move costs neither an allowance nor an ad.
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

- **Full-screen Gameplay and Results** (re-cut). The status and navigation bars hide while you play
  and on the Results screen, and every screen draws edge to edge on every Android version the game
  supports; a swipe from the edge brings the bars back for a moment.
- **A calmer Home, and Continue shows where you left off** (re-cut). The decorative stage behind
  the mode cards is gone and its space becomes headroom above the title. The Classic Continue chip
  shows a small picture of your saved board position.
- **Challenge star bands and a later start on the difficulty curve** (re-cut). Stars in Challenge
  are now awarded in bands, so three stars is within reach on every level, and the ladder starts
  thirty levels further along the generator's difficulty curve; level numbers are unchanged. The
  Results screen names the next star and shows a Cleared headline.
- **Home title at the top, Home full screen, and Play again** (re-cut, third pass). The title sits
  at the top of Home, with a little space above it, and the mode buttons kept low; Home hides the system bars like gameplay does;
  a solved Challenge level offers Play again to chase more stars.
- **Challenge scores follow the stars** (re-cut, second pass). Every clear still unlocks the next
  level, but the score is multiplied by the stars earned: three stars scores three times one star.
- **A fuller Home screen**: a title band, a three-pill
  progress ribbon, and board art on the Classic and Challenge cards, both cards in the lower half of
  the screen. The Challenge level, the Continue chips and the Classic stars are still on the first
  screen, with nothing to open. The info button is gone; its help is now the tutorial.
- **The gameplay screen has no top bar**. A slim status
  strip at the top keeps a labelled back control and the timer. Undo, Hint, Show moves and Restart
  move to a dock in the lower half of the screen, within thumb reach. Show moves is now shown by
  fill, not by colour alone, Hint has its own new icon, and no two controls share an icon.
- **The game's typefaces are built in**. Fredoka and
  Nunito ship inside the app, so the first screen is drawn in the right fonts on every device,
  offline and without Google Play services.
- **A new glossy look**. A deep violet backdrop with soft colour waves and slowly drifting light
  sits behind the menu, the board and the Licenses screens, and the Home mode cards have a glossy
  finish. The waves and lights fade in just after the Home screen appears. The board and the pegs
  are unchanged. The motion stops when the system's animations are turned off.
- **Faster hints on the big boards.** Asking for a hint from the opening positions of
  European-37, Asymmetric-39 or Wiegleb-45 now returns a real winning move inside the same
  one-and-a-half-second deadline. It is answered from an opening book that covers the first moves
  of every Classic board, and by a rebuilt solver beyond it.
- **Star ratings are measured against what each board can actually reach.** Every board's star
  bands are derived from that board's own fewest-peg floor rather than from one shared assumption.
- **A hint you ask for while the board is changing is cancelled**, rather than arriving late and
  pointing at a position you have left.
- **Turning off animations takes effect while you play.** Switching "Remove animations" on or off in
  the system settings now reaches the game without restarting it (within about ten seconds, the
  system's own delay).
- **When a hint cannot work out a position in time, it says so** instead of claiming there is no
  winning move; "no winning move" now appears only when it is proven.

### Fixed

- **The status bar was unreadable in the light theme**.
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
- **Home no longer jumps when your Continue chips appear** a moment after launch.
- **TalkBack now reads the game timer's value,** in whole minutes; before, it said only "Elapsed
  time".
- **Navigation buttons stay readable in dark mode** with three-button navigation.
- **Sign-in was fragile in a dozen small ways** — a prompt that could flash back open, an error
  message that replayed, a tap that did nothing while a round trip was in flight, a cancel treated
  as a failure instead of a decline. All of them are gone.

### Under the hood

- (re-cut) Challenge generator contract v2 (the 30-level shift; the fail-proof opening and mix deferred to 3.0.1) with the clear rule
  as one scoring function and `LEVEL_OFFSET = 30` (Q-040; `docs/gdd.md` GD-SB-21). Saved Challenge
  stamps from the earlier generator regenerate.
- (re-cut) Fredoka Medium/SemiBold are now an OFL-1.1 Modified Version built from
  `fonts/fredoka-tr/recipe.json`; `BundledFontRecipeTest` pins the outputs' sha256.
- (re-cut) A window system-bar seam for immersive mode, mirrored into dialog windows; below API 30
  the stable insets are read from the root view.
*Nothing in this section is visible while playing; it is here for the next person to read the
code.*

- The downloadable Google Fonts path is removed: the `GoogleFont` provider, `font_certs.xml` and
  `ui-text-google-fonts`. Five static OFL-1.1 font files load blocking from `res/font`, and their
  two licence texts ship in `assets/licenses/` (`THIRD_PARTY.md` §2).
- Release builds are minified and resource-shrunk by R8, with the sound files and the opening books
  proved to survive the shrink. The bundled fonts are reached through `R.font`, and the release
  branch checks that the bundle keeps all five and their licence texts.
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
- `check` carries a `:domain` line-coverage gate (kover, bound 90 %) and an exhaustive fuzz harness
  over the whole 10,000-level Challenge space (`FUZZ_SEEDS`), merged with
  `feature/phase-14-check-gates` (`dbd0a6c`).
- Board speech is built by a pure model with no Android dependency, pinned by an architecture
  test; the accessibility overlay is semantics-only above the unchanged tap path.
- The Licenses screen's Google SDK notices are generated at build time and bundled under
  `assets/licenses/`.
- The backdrop is drawn in-repo with Compose Canvas behind the navigation host, on one clock, from
  colour tokens; it adds no dependency and no asset file.

[3.0.0]: https://semver.org/ "tag v3.0.0 — link replaced with the repository compare URL when the repository has a remote"
