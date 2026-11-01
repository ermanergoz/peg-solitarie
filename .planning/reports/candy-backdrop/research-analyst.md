# research-analyst report — candy-backdrop
Date: 2026-10-17
Branch: feature/candy-backdrop (report only)
Skills loaded: ergoz-soft:standards (plugin rules v1.0.0, read in full because the product snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; product `.claude/rules/lessons.md`), research-open-source, research-competitive, market-research
Ran in parallel with: phase-15 code-auditor and qa-engineer loop 3, phase-14 release docs and founder items, phase-14 tech design
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
VERDICT: inspiration-only. Not escalated: nothing copyleft or unlicensed is to be reused, and no live trademark will be used.
A bright candy/sweets backdrop is a theme, which is an idea, so it is free to use. Candy Crush Saga's names, logos, characters, six-candy set, special candies, callouts, level-map art and scene paintings are King's live trademarks and copyrighted art, and must be neither used nor imitated.
Recommended source: (a) draw the backdrops in-house as Compose Canvas or vector drawables, in the app's existing "Candy Clay" design system. Kenney's CC0 candy pack is a clean fallback or reference. AI-generated raster art is not recommended.
No founder question is needed. Section 3's do / don't list needs no further judgement to follow.
Addendum 2026-10-18 (end of file): the founder's direction B "Deep gloss" (Q-021) is clean under §2, on the conditions in A.3 and the word rules in A.4. A.5 replaces §6's "What was taken" cell.

## Verdict
**VERDICT: inspiration-only** (`rules/ip-policy.md`).

| Element | Status | Matrix row / rule that decides it |
|---|---|---|
| A candy-themed visual style: bright gradients, glossy sweets, lollipops, swirls, sprinkles, candy hills and clouds, a panel behind the board | Unprotected idea | "Ideas, mechanics, and features are not protected by copyright. Inspiration is always allowed. Record the source in THIRD_PARTY.md anyway." |
| King's artwork: candy pieces, special candies, characters, episode scenes, level map, UI, logo artwork, voice callouts | Protected (corporate copyright, first published 2012) | "No license / unknown — Code: no, Assets: no — no reuse". Also "modifying protected material does not make it yours". |
| King's names and marks: CANDY CRUSH SAGA, CANDY CRUSH, the CANDY CRUSH logo marks, EU CANDY, the -SAGA family | Live trademarks (§1) | "Never ship a title, logo, character, or distinctive visual identity from an existing product." |
| Hasbro's CANDY LAND (the request's own phrase "candy-land style") | Live trademark, US Reg. 0544328 (§1) | Same rule: never shipped |
| Source (a): drawn in-house | Own work, clean | No licence row needed; only the idea row in §6 |
| Source (b): Kenney *Platformer Art Candy* | **CC0 1.0**, clean | "CC0 — yes / yes — attribution optional" |
| Source (b): OpenGameArt items marked CC0 | Clean, item by item, once provenance is checked (§4) | Same CC0 row |
| Source (b): any CC-BY item | Clean, but credit is an obligation | "CC-BY — yes / yes — credit required". The app has no licences screen yet (THIRD_PARTY.md §7). |
| Source (c): AI-generated raster | Not covered by the matrix, so it escalates if anyone proposes it; **not recommended** (§4c) | The matrix lists licences, and AI output carries a contract, not a licence. Raster images would also end the app's all-vector property. |

Why this is not an escalation: the ESCALATION triggers are copyleft or unknown licences on something the team wants to reuse, and plausibly live trademarks in use. The plan reuses nothing of King's and uses none of its marks. If the team later wants a King element, or any asset under a licence outside the matrix, it comes back here and escalates.

## 1. What King protects in Candy Crush Saga
**Owner.** Candy Crush Saga is published on Google Play by "King" (listing `com.king.candycrushsaga`, read 2026-10-17) [S30]. Its marks are registered to King.com Limited, an Activision Blizzard company. Microsoft completed its acquisition of Activision Blizzard on 2023-10-13 [S20].

### 1a. Trademarks
| Mark | Register and number | Goods/classes | Status as read | Source |
|---|---|---|---|---|
| CANDY CRUSH SAGA (word) | USPTO Reg. **4,541,423** (serial 85566839); filed 2012-03-12, registered 2014-06-03 | game software for computers, phones and tablets | **Live**: "The registration has been renewed." | [S1] |
| CANDY CRUSH | USPTO Reg. **6,055,823** (serial 86401839); registered 2020-05-19 | merchandise across many classes | **Live** (§8 and §15 declaration accepted) | [S2] |
| CANDY CRUSH SAGA | EUTM **010718542**, registered 2012-08-09 | 9, 25, 41 | Cited as registered in a 2014 WIPO decision | [S3] |
| CANDY CRUSH SAGA (word) | WIPO IR **1143295**, 2012-08-28 | 9, 25, 41 | Same | [S3] |
| CANDY CRUSH SAGA and design (logo) | EUTM **011560448**, 2013-07-05 | 3, 9, 16, 25, 28, 29, 30, 32, 35, 41 | Same | [S3] |
| CANDY CRUSH and design (logo) | EUTM **011560604**, 2013-07-05 | Same classes | Same | [S3] |
| CANDY (word) | EUTM (number not established) | video games, game services, clothing | Registered by early 2014. ZeptoLab filed for cancellation on 2014-03-20; no outcome was found | [S4] [S5] |
| CANDY (word) | USPTO serial 85842584, filed 2013-02-06 | games | **Dead**: "Abandoned after publication because applicant filed an express abandonment" (Feb 2014, after public backlash) | [S6] [S5] |
| SAGA | King has no mark on SAGA alone, but holds at least 13 "…SAGA" game marks and has used them to oppose other SAGA titles | — | Policed (1c) | [S7] [S8] |
| CANDY LAND (Hasbro, not King) | USPTO Reg. **0544328**, registered 1951-06-26 | board game, class 28 | **Live**: "Registered and Renewed" (2021-02-01). A US court held it a *famous* mark in 1996 | [S9] [S10] |

**Register access.** EUIPO eSearch, TMview, USPTO TSDR and Justia refused scripted access from this machine (HTTP 403, 429 and connection reset; see Checks). US statuses were read from Docket Alarm's USPTO mirror. EU marks come from the 2014 WIPO decision, and their current status is **unverified**. The EU CANDY cancellation outcome was not found. This report therefore treats every mark above as live. That costs nothing, because section 3 avoids every one of them.

### 1b. Copyright and look
- Game ideas are free: "the name or title given to the game and the methods for playing it are not protected by copyright" (US Copyright Office) [S11]. The candy theme, match-3 rules and a level-map structure are ideas.
- The artwork is protected: candy pieces, special candies, characters, episode backgrounds, the map art, logo artwork, UI art and voice lines are corporate works first published in 2012. Under `rules/ip-policy.md` that protection runs for 95 years.
- A game's look can be protected even when its rules are not. In *Tetris Holding v. Xio Interactive* (D.N.J. 2012, 863 F. Supp. 2d 394), the court held (as summarised by the sources, not quoted from the opinion) that the style, shape, colour and movement of the pieces and the playfield's look were protectable expression, even though Tetris's rules were not. It also found trade-dress infringement [S12]. In *Spry Fox v. Lolapps* (W.D. Wash. 2012), a similar setting and object hierarchy (the order: "A snow field is not so different from a meadow") was enough to state a copyright claim, and the case settled [S13].
- No registered trade dress or colour mark for King was found. Unregistered trade dress (the overall combination of look elements) can still be asserted, as *Tetris* shows.

**The elements that make Candy Crush recognisable**, and which section 3 therefore bans:
- **The six regular candies**: red jelly bean, orange lozenge, yellow lemon drop, green gum square, blue lollipop head, purple jujube cluster [S14].
- **The special candies**: striped [S15], wrapped and colour bomb (the latter two are general knowledge of the game, not separately sourced).
- **The characters**: Tiffi, Mr. Toffee, Mr. Yeti, Odus the owl [S16].
- **The voice callouts**: the "Sugar Crush!" end-of-level callout [S17]. The cascade praise words "Sweet / Tasty / Delicious / Divine" are widely associated with the game but were *not* verified from a primary source; they are banned anyway, at no cost.
- **The logo and map**: the CANDY CRUSH logo marks above, and the winding "saga map" path through painted candy episodes.

### 1c. Enforcement history (King polices names hard, and art through copyright)
- **2013-08-27**: King sued 6waves in US federal court in San Francisco, alleging copyright infringement of *Pet Rescue Saga* and *Farm Heroes Saga* by clones [S18].
- **2013-12-27**: King opposed Stoic's THE BANNER SAGA at the USPTO on its SAGA marks, while saying publicly it had no concern that Stoic was trading on its brand. Settled in spring 2014 [S7] [S8] [S19].
- **January 2014**: App Store developers received notices over "candy" in their titles. King's line was "We don't enforce against all uses of CANDY". In the CandySwipe dispute, King bought the older "Candy Crusher" mark to counterclaim against developer Albert Ransom. Both sides withdrew in April 2014 [S5] [S19].
- **2014-02-20**: WIPO UDRP D2013-1986 transferred `crush-the-candy.com` and two other domains to King [S3].
- **February to March 2014**: King abandoned the US CANDY application but kept the EU registration. ZeptoLab (*Cut the Rope*) filed to cancel it [S4] [S6].
- **About 2016 to 2023** (dated by proceeding numbers 91231476 to 91281561; dates are not shown): Docket Alarm's USPTO mirror lists **at least 25 TTAB oppositions** filed by King.com Limited / Midasplayer against the applicants it names [S1]. Among them are Netease, IGG, The Creative Assembly, Aristocrat, Huuuge, Npixel and Candy Digital (#91281561). The opposed marks are not shown there.

**Reading.** King acts mainly against **names** (CANDY-, CRUSH- and SAGA-formative titles), at volume and against large publishers. It acts against **art** through copyright suits on clones. A peg-solitaire game with no King word and its own art sits outside both patterns. Section 3 keeps it there.

## 2. Genre convention versus a distinctive look
**Free to use (convention and idea).** All of these are allowed:
- a confectionery theme as such;
- bright saturated gradients, glossy highlights and soft specular dots;
- lollipops, candy canes, swirl discs, sprinkles, gumdrops, twist-wrapped sweets in general, cupcakes, frosting drips, cake or cream hills, cotton-candy clouds and pastel skies;
- a translucent or raised panel behind the board;
- a bouncy, celebratory mood.

The motif set is common property. Kenney's 2013 *Platformer Art Candy* ("Candy Caverns") pack, released CC0, is built from exactly these pieces: lollipops, striped canes, gummy worms, cookie and cake hills [S21]. This app already uses the convention: pink "candy sphere" pegs on a clay slab, under the Candy Clay design system (`docs/ui/DESIGN-SYSTEM.md`).

**Protected (King's expression).** These are King's:
- the *specific designs*: the six candies with their colour pairings, the special candies, the characters, the logo lettering, the map art and King's painted episode scenes;
- the *particular combination* that makes a screen read as Candy Crush: those candies on King's tile grid, under King's callouts, in front of King-style episode scenes.

Copying one specific design is copyright infringement. Assembling several generic conventions *to evoke Candy Crush* is a look-and-feel and trade-dress risk (*Tetris*, *Spry Fox*, §1b).

**The test a reviewer applies** has two questions. Fail either one and the art is out:
1. Could any single element be traced to, or recognised as, a King design?
2. Would a player shown one screenshot think it is Candy Crush, a Candy Crush spin-off, or a reskin of it?

"It is a sweet" is not the test, and "it is inspired by Candy Crush" is not a defence.

**Why the exposure is small here.** The mechanic is peg solitaire on a cross-shaped board, not match-3 on a rectangular grid. The game screen's hero object, the peg board, is already ours. Risk lives only in the backdrop art and in words. Section 3 closes both.

## 3. Do / don't list for the ux-designer and the developer
Apply every line literally. Anything not covered comes back to the research-analyst through the producer.
Amended 2026-10-18 for direction B: the addendum's A.1 adds to §2's free list, A.3 adds to 3b, and A.4 adds to 3a.

### 3a. Words: app UI, `strings.xml`, achievements, leaderboards, Play Games names, notifications, store listing, screenshot overlays, feature graphic
**Never write any of these, in any language or casing, inside or outside another word:**
- `Candy Crush`, `Crush` (when anything candy-themed is on screen or in the same listing), `Saga`, `Sugar Crush`, `Candy Land`, `Candyland`
- `Tiffi`, `Toffee` (as a name), `Yeti`, `Odus`
- `King` (as a reference to the company), `Activision`, `Microsoft` (as references to Candy Crush's owners)

**Never use the word `candy`, alone or in a compound, in user-facing text or the store listing.** That covers the app title, short and full description, developer name, mode names, world or level names, button labels, achievement and leaderboard names, notifications and release notes. Reason: King's EU CANDY registration for video games (§1a) is unverified but must be presumed live. The current title, `Peg Solitaire` (`app_name`), stays.

**Never use these as celebration or combo callouts**, on screen or in audio: `Sweet!`, `Tasty!`, `Delicious!`, `Divine!`, `Sugar Crush!`. Keep the app's own `New best!` / `Solved` vocabulary.

**Never refer to Candy Crush, King, or "games like" it**, in any form. That includes comparisons ("like Candy Crush", "for fans of", "inspired by", "Candy Crush style"), keywords and hashtags in the listing, release notes, in-app copy, and ad keywords.

**Internal names may stay.** "Candy Clay" (the design system), `drawCandyPeg`, `PegGradient` and code comments never reach a player (the release build is minified). The rule: no string resource, About text, licences screen or listing may ever contain them.

### 3b. Shapes and compositions
**Don't draw:**
- any of Candy Crush's six regular candies as a recognisable shape: a red jelly bean, an orange lozenge, a yellow lemon-drop teardrop, a green gum square, a blue round "lollipop head", a purple jujube cluster. Never all six, never as a colour-coded set of tokens;
- a candy with **directional stripes** (striped candy), a **twist-wrapped square with a glow** (wrapped candy), or a **chocolate ball studded with sprinkles** (colour bomb);
- a lollipop used as a hammer or tool;
- **any character or mascot**: no owl, no yeti, no girl, no anthropomorphic sweet with a face;
- a **winding path of level nodes through themed candy "episodes"** (the saga map). The Challenge ladder keeps its own layout;
- the board on a **dark translucent grid of rounded square tiles**. Keep the existing clay slab (`surfaceContainerHigh`, large radius, soft violet-tinted shadow);
- **chunky candy-textured or candy-striped lettering** for the title or any heading. Fredoka and Nunito stay;
- any composition traced, eyedropped, or laid out over a King image. **No King screenshot, logo or marketing art enters the repository** (`.planning/` included) or a design file as an underlay or reference layer.

**Do draw** these, from scratch, in the app's own clay style: soft rounded hills and frosting layers, cloud puffs built from overlapping circles, scattered sprinkles (short rounded capsules), swirl discs, plain candy canes, gumdrop domes, cupcake and cookie silhouettes, and the existing slow-drifting soft blobs. Keep motifs low-contrast behind the board, and drift them only when the animator duration scale is above 0 (DESIGN-SYSTEM.md, Motion). The **peg board stays the hero object** of the gameplay screen, and the Home tiles stay the hero of the menu.

### 3c. Palette: how far it may go
- Colours themselves are free. No King colour mark was found, and a palette is not a protectable design on its own. Any bright, saturated hue is allowed, including pinks, blues, oranges, greens and yellows.
- Every colour comes from `ui/theme/Color.kt` tokens. New backdrop tokens are added there, both light and dark, derived from the existing violet `#7C3AED` / pink `#EC4899` / gold `#F59E0B` / lavender `#F4F1FA` system. **Violet stays the lead colour.** That is what makes the app read as itself.
- **Never eyedrop a colour from King's art**, and never pair six candy colours with six candy shapes (3b).
- Accessibility rules are not changed by any of this (`rules/compose.md`: text contrast at least 4.5:1, never colour alone).

### 3d. Store listing and Play assets
- Title `Peg Solitaire`. The short description, full description, developer name and feature graphic follow 3a.
- Screenshots are **plain captures of this app**, with no added candy art or King-like framing. Play's Metadata policy: "Provide screenshots and videos that reflect your app's actual experience" [S24].
- The launcher and listing icon stay the English-33 board on the violet gradient. Never use a candy piece, a striped sweet or a lollipop as the icon.
- None of this blocks the open THIRD_PARTY.md §7 store-artwork rows (Q-F3). Those are a separate provenance question.

## 4. Asset sourcing options, each with a matrix verdict
### (a) Drawn in-house: Compose Canvas or vector drawables. **Verdict: clean (own work). RECOMMENDED.**
- Matches the product as it ships: "There is **no raster image anywhere in `app/src/main/res`**" (THIRD_PARTY.md §2, on `PegSolitarie-r5`). Every drawable, the launcher icon and the board are authored vectors or Canvas.
- Light and dark variants come straight from `Color.kt` tokens. The art scales to every screen and footprint, adds no APK weight, and can drift or parallax while honouring reduced motion.
- No licence obligation and no credit. Provenance is the git history.
- The only record needed is the idea row in §6.
- Cost: design and implementation time (ux-designer spec, then android-game-dev).

### (b) CC0 / CC-BY candy art. **Verdict: clean under the CC0 row; CC-BY clean with mandatory credit.** Fallback or reference only.
| Item | Source | Licence, as checked | Fit |
|---|---|---|---|
| Kenney, *Platformer Art Candy* ("Candy Caverns") 1.0, 2013, 100 files: cake/chocolate hills, lollipops, canes, gummy worms, cookies, waffles | https://kenney.nl/assets/platformer-art-candy (zip `kenney_platformer-art-candy.zip`, sha256 `28f1dcdd38a9fbfba917531ed6d86887537248b150858f7d24f92c167bd45639`) | **CC0 1.0.** Asset page: "License: Creative Commons CC0". `license.txt` inside the zip: "License (CC0) http://creativecommons.org/publicdomain/zero/1.0/ — You may use these graphics in personal and commercial projects. Credit (Kenney or www.kenney.nl) would be nice but is not mandatory." Kenney support: "all game assets on the asset pages are public domain licensed (CC0) … even in commercial projects … Attribution is not required … Do not use our logo" [S21] [S22] | Generic candy motifs with nothing King-specific (preview inspected). But they are flat-style 70 px side-view platformer tiles in PNG: a style mismatch with claymorphism, and they would be the app's first raster images. Best used as a motif reference, or redrawn as vectors (CC0 allows derivatives). |
| Kenney *Background Elements* / *Background Elements Remastered* | https://kenney.nl/assets/background-elements, https://kenney.nl/assets/background-elements-remastered | CC0 (covered by the support-page statement; packs not opened) | Generic skies, clouds and hills; not candy-specific. |
| OpenGameArt, *Super Candy Set* (Master484, 2014, 300+ pixel-art sweets); *More Candy* (Master484) | https://opengameart.org/content/super-candy-set-m484-games, https://opengameart.org/content/more-candy-m484-games | CC0 (licence field on the page: creativecommons.org/publicdomain/zero/1.0/) | Pixel art: style mismatch. |
| OpenGameArt, *Candy Pack* (Clint Bellanger, 2014, 3D models) | https://opengameart.org/content/candy-pack | CC0 | 3D models; would need rendering. |
| OpenGameArt, *Colored Handful Candy* (way-21, 2015) | https://opengameart.org/content/colored-handful-candy | CC0 | A single image. |
| OpenGameArt, *Candy Button Pack* (pzUH, 2018) | https://opengameart.org/content/candy-button-pack | CC0 | **Avoid.** Tagged "match three … saga": made for Candy Crush-style games, so it risks importing King-like designs. |

Conditions on any (b) asset:
1. **OpenGameArt licences are asserted by the uploader.** Before use, confirm the uploader is the author and the work is not itself derived from King or another protected game. Record the page URL and the date read.
2. **Every asset still passes the §2 test and the §3b don't list.** A CC0 file that draws a striped candy is still out.
3. **CC-BY** is allowed only with credit, and credit needs a place to live. The app has no licences screen yet (THIRD_PARTY.md §7), so prefer CC0.
4. **Licences outside the matrix escalate to this role**: CC-BY-SA (share-alike), CC-BY-NC/ND (unusable), and custom licences such as Freepik, Craftpix, the Pixabay Content License or itch.io per-pack terms.

### (c) AI-generated raster art. **Verdict: outside the matrix, so it escalates if proposed. NOT RECOMMENDED.**
**What the tools' terms say:**
- **OpenAI**: "you … own the Output. We hereby assign to you all our right, title, and interest, if any, in and to Output". Also: "output may not be unique and other users may receive similar output from our Services" [S25].
- **Midjourney**: you own your assets, subject to "the rights of any third-parties". A company with more than USD 1,000,000 a year in revenue "must be subscribed to a 'Pro' or 'Mega' plan to own Your Assets". Free-trial users do not own their output. These terms were read through a third-party mirror, because Midjourney's own page refused access [S26].
- **Adobe Firefly**: trained on licensed Adobe Stock and public-domain material, and marketed as commercially safe. The IP indemnity covers enterprise customers only [S27].

**Copyright.**
- The US Copyright Office's Part 2 report (2025-01-29) allows protection where a human determines the expressive elements, "but not the mere provision of prompts" [S28].
- *Thaler v. Perlmutter* (D.C. Cir., 2025-03-18) held that human authorship is required. The Supreme Court declined review on 2026-03-02 [S29].
- So the studio would hold a contract position (the "if any" in OpenAI's own assignment), but probably **no copyright** in raw generated backdrops. Anyone could copy them.

**Infringement risk.** A prompt naming or describing Candy Crush can reproduce King's expression. The tools' terms leave that risk with the user. In a design meant to "look like candy crush", that is the most likely failure.

**Google Play.** The AI-Generated Content policy covers apps that *generate* content from user prompts [S23]. It sets no disclosure rule for AI-made assets shipped inside an app (none found). The IP and Impersonation policies still apply to every asset, whatever made it.

**Studio fit.** The matrix has no row for AI output. Raster art would end the app's all-vector property and add per-density PNGs.

### Recommendation
**(a)**: draw the backdrops in-house in Compose Canvas or vector drawables, from the Candy Clay tokens. Use Kenney *Platformer Art Candy* (CC0) only as a motif reference, or as a redraw-to-vector source if the ux-designer wants a head start. In that case, add the asset row given in §6. Do not use (c).

## 5. Google Play policy exposure
**What the policies say** (read 2026-10-17):

*Impersonation* [S23a]:
- "Don't imply that your app is related to or authorized by someone that it isn't."
- "Be careful not to use app icons, descriptions, titles, or in-app elements that could mislead users".
- Listed violations include "App titles and icons that are so similar to those of existing products or services that users may be misled". One example is an icon "copying the character and title of a famous TV show".

*Intellectual Property* [S23b]:
- "We don't allow apps that infringe copyright. Modifying copyrighted content may still lead to a violation."
- Listed violation: "Marketing images from movies, television, or video games".
- On trademarks: "If your app uses another party's trademarks in a way that is likely to cause confusion, your app may be suspended."

*Metadata* [S24]:
- Bans misleading or irrelevant metadata in the "description, developer name, title, icon, screenshots, and promotional images".
- Lists "Data comparison of apps or brands" as a violation.
- Advises "Avoid using repetitive or unrelated keywords or references" and "Don't use a celebrity's name or a brand's logo without permission".

**Exposure for this listing: low, provided §3 holds.** Different genre, a plain descriptive title, a board icon, own art. The risks, ranked:
1. **A King word or comparison in the listing** ("candy crush", "saga", "like Candy Crush"). This trips Impersonation, Metadata and Trademark at once, and it is the most likely way a well-meant listing goes wrong.
2. **An icon or feature graphic built from a candy piece** that resembles a King candy.
3. **Screenshots whose backdrop reads as King's art.** These are the first thing a reviewer or a King brand-protection team sees.
4. **A rights-holder notice from King.** King's record (§1c) is volume enforcement on names. The IP policy's remedy is suspension.

**What keeps us clear:**
- §3a's words rule across the app and the listing;
- §3b's shapes rule and the no-King-image-in-the-repo rule;
- §3d's plain-capture screenshots and board icon;
- the release-manager checking the listing text against §3a at every listing change.

## 6. THIRD_PARTY.md row text (idea row)
Superseded 2026-10-18: the idea row's "What was taken" cell below is replaced by the addendum's A.5. Copy the row from A.5, not from here.

THIRD_PARTY.md is not on `develop` or on this branch. It lives on `feature/phase-14-release-docs` (worktree `PegSolitarie-r5`, commit e5253f4), which this report does not touch. The row below goes into that file's **§1 "Ideas and mechanics"** table, whose columns are `| Item | Source | Licence | What was taken | Credit |`. Add it on whichever branch carries THIRD_PARTY.md when the backdrop merges, in the same review as the backdrop.

```
| Candy-themed backdrop for the Home (menu) and gameplay screens | The candy-themed casual-game genre; the founder's named reference was *Candy Crush Saga* by King.com Limited — https://www.candycrush.com/ , https://play.google.com/store/apps/details?id=com.king.candycrushsaga | Not protected — a visual theme is an idea, not an expression (`rules/ip-policy.md`). King's names, logos, characters, candy designs and artwork are protected, and **none was taken** — verdict *inspiration only*, `.planning/reports/candy-backdrop/research-analyst.md` | The idea of a bright sweets-landscape backdrop only; every shape, colour stop and composition is authored in this repo (`ui/theme/Color.kt` tokens, Compose Canvas / vector drawables) | — deliberately not credited in the app: naming King in the app or the listing would imply a relationship Play's Impersonation policy forbids; this row stays out of any in-app licences screen |
```

**Only if a Kenney file is used** (redrawn or as-is), add this row to **§2 "Assets shipped in the artifact"** too. Its columns are `| Item | Source | Licence | How verified | Credit |`.

```
| <res/drawable file names> — from Kenney *Platformer Art Candy* 1.0 | https://kenney.nl/assets/platformer-art-candy | **CC0 1.0** | `license.txt` inside the pack (zip sha256 28f1dcdd38a9fbfba917531ed6d86887537248b150858f7d24f92c167bd45639) and https://kenney.nl/support, read 2026-10-17 | Courtesy credit "Kenney" (optional under CC0; never Kenney's logo) |
```

## Sources
All read 2026-10-17 unless dated. "Refused" means the page blocked scripted access from this machine; the text was then confirmed through the mirror or search result named.

- [S1] Docket Alarm (USPTO mirror), CANDY CRUSH SAGA, serial 85566839, Reg. 4,541,423, with its TTAB opposition list: http://www.docketalarm.com/trademarks/85566839/CANDY_CRUSH_SAGA/
- [S2] Docket Alarm, CANDY CRUSH, serial 86401839, Reg. 6,055,823: http://www.docketalarm.com/trademarks/86401839/CANDY_CRUSH/
- [S3] WIPO Arbitration and Mediation Center, *King.com Limited v. Raymond Tan*, D2013-1986 (2014-02-20), listing King's EU and WIPO registrations: https://wipo.int/amc/en/domains/decisions/text/2013/d2013-1986.html
- [S4] Engadget, "Cut the Rope studio challenges King's 'candy' trademark in EU" (2014-03-26): https://www.engadget.com/2014-03-26-cut-the-rope-studio-challenges-kings-candy-trademark-in-eu.html ; The Next Web (2014-03-25): https://thenextweb.com/news/cut-rope-maker-zeptolab-wants-cancel-king-coms-eu
- [S5] Engadget, "Candy Crush dev: we won't enforce against all uses of 'candy' trademark" (2014-01-21): https://www.engadget.com/2014-01-21-candy-crush-developer-will-not-enforce-against-all-uses-of-cand.html
- [S6] Docket Alarm, CANDY, serial 85842584 (abandoned): http://www.docketalarm.com/trademarks/85842584/CANDY/ ; Law360, "Facebook Game Maker Ends 'Candy' Trademark Efforts In US": https://www.law360.com/articles/513333
- [S7] PC Gamer, "The Banner Saga's trademark filing is being opposed by the makers of Candy Crush Saga" (2014): https://www.pcgamer.com/the-banner-saga-3
- [S8] NBC News, "First 'Candy,' Now 'Saga'" (2014): https://www.nbcnews.com/tech/video-games/first-candy-now-saga-candy-crush-saga-goes-after-indies-n14741
- [S9] Justia Trademarks, CANDY LAND, Reg. 0544328 (serial 71589107), "Registered And Renewed", 2021-02-01: https://trademark.justia.com/715/89/candy-land-71589107.html (refused; status from the search-result extract)
- [S10] *Hasbro, Inc. v. Internet Entertainment Group, Ltd.*, No. C96-130WD (W.D. Wash. 1996-02-09): https://cyber.harvard.edu/metaschool/fisher/domain/dncases/hasbro.htm
- [S11] US Copyright Office, registration of games: https://www.copyright.gov/register/tx-games.html
- [S12] *Tetris Holding, LLC v. Xio Interactive, Inc.*, 863 F. Supp. 2d 394 (D.N.J. 2012): https://en.wikipedia.org/wiki/Tetris_Holding,_LLC_v._Xio_Interactive,_Inc. ; https://www.masslawblog.com/copyright/video-game-company-misreads-copyright-law-infringes-tetris/
- [S13] *Spry Fox, LLC v. Lolapps, Inc.* (W.D. Wash. 2012): https://en.wikipedia.org/wiki/Spry_Fox,_LLC_v._Lolapps,_Inc. ; settlement: https://www.pocketgamer.com/articles/045709/r/
- [S14] Candy Crush Saga Wiki (fan wiki), Candy: https://candycrush.fandom.com/wiki/Candy (refused; table from the search-result extract)
- [S15] TV Tropes, Candy Crush: https://tvtropes.org/pmwiki/pmwiki.php/VideoGame/CandyCrush
- [S16] Candy Crush Saga Wiki, Characters: https://candycrush.fandom.com/wiki/Characters
- [S17] Candy Crush Saga Wiki, Sugar Crush: https://candycrush.fandom.com/wiki/Sugar_Crush
- [S18] UNC Journal of Law & Technology blog, "King … Sues 6waves for Copyright Infringement" (2013-09-05): https://journals.law.unc.edu/ncjolt/blogs/a-new-hope-the-saga-continues-king-maker-of-candy-crush-saga-sues-6waves-for-copyright-infringement/
- [S19] PocketGamer.biz, "CandySwipe and Banner Saga trademark disputes resolved with King" (2014): https://www.pocketgamer.biz/candyswipe-and-banner-saga-trademark-disputes-resolved-with-king ; Snopes: https://www.snopes.com/fact-check/candy-crush-saga
- [S20] Acquisition of Activision Blizzard by Microsoft (closed 2023-10-13): https://en.wikipedia.org/wiki/Acquisition_of_Activision_Blizzard_by_Microsoft ; https://toucharcade.com/2023/10/13/microsoft-activision-blizzard-king-acquisition-complete-october-2023-deal/
- [S21] Kenney, Platformer Art Candy (asset page, and the zip's `license.txt`): https://kenney.nl/assets/platformer-art-candy
- [S22] Kenney, Support (licence and attribution FAQ): https://kenney.nl/support
- [S23] Google Play, AI-Generated Content: https://support.google.com/googleplay/android-developer/answer/13985936
- [S23a] Google Play, Impersonation: https://support.google.com/googleplay/android-developer/answer/9888374
- [S23b] Google Play, Intellectual Property: https://support.google.com/googleplay/android-developer/answer/9888072
- [S24] Google Play, Metadata: https://support.google.com/googleplay/android-developer/answer/9898842
- [S25] OpenAI Terms of Use, "Ownership of content" and "Similarity of content": https://openai.com/policies/row-terms-of-use/ (refused; clause text from the 2024-12-11 revision via search, https://openai.com/policies/row-terms-of-use/revisions/2024-12-11/ , and the mirror https://open.windriver.com/info/uni-license-list/licenses/openai-tou-20241211.html)
- [S26] Midjourney Terms of Service: https://docs.midjourney.com/hc/en-us/articles/32083055291277-Terms-of-Service (refused; clause text from https://conductatlas.com/platform/midjourney/midjourney-terms-of-service/provision/CA-P-023523/pro-or-mega-plan-required-for-company-ownership/ and https://terms.law/ai-output-rights/midjourney/)
- [S27] Adobe, "Growing responsibly in the age of AI: Adobe Firefly and Stock" (2024-04-16): https://blog.adobe.com/en/publish/2024/04/16/growing-responsibly-age-of-ai-adobe-firefly-stock ; Computerworld on the Firefly indemnity: https://www.computerworld.com/article/1628682/adobe-offers-copyright-indemnification-for-firefly-ai-based-image-app-users.html
- [S28] Library of Congress Copyright blog, "Inside the Copyright Office's Report … Part 2: Copyrightability" (2025-02-06): https://blogs.loc.gov/copyright/2025/02/inside-the-copyright-offices-report-copyright-and-artificial-intelligence-part-2-copyrightability/
- [S29] *Thaler v. Perlmutter*, No. 23-5233 (D.C. Cir. 2025-03-18): https://ipwatchdog.com/2025/03/24/d-c-circuit-upholds-human-authorship-requirement-thaler-v-perlmutter/ ; certiorari denied 2026-03-02: https://law.emory.edu/lawyer/issues/2026/summer/features/ai_copyright/index.html
- [S30] Candy Crush Saga, official site (king.com/game/candycrush redirects here) and Play listing (developer "King"): https://www.candycrush.com/ ; https://play.google.com/store/apps/details?id=com.king.candycrushsaga
- OpenGameArt item pages: the URLs in the §4(b) table, each with its CC0 licence field read.

## Outputs
- `.planning/reports/candy-backdrop/research-analyst.md`: this report. It holds the verdict, the do / don't list (§3), the asset verdicts (§4), the Play exposure (§5) and the THIRD_PARTY.md row text (§6). Nothing else was written to the repository. App code, THIRD_PARTY.md and every other branch are untouched, as dispatched. The Kenney zip and preview images were inspected in the session scratchpad only.
- `.planning/reports/candy-backdrop/research-analyst.md` (addendum 2026-10-18): the direction B section at the end of this file. It holds the verdict, A.1's element table and free-list additions, A.3's conditions, A.4's word rules with the Candy Crush Solitaire fact, and A.5's row text. One pointer line each was added to the Summary, §3 and §6. Nothing else was written to the repository. The King store screenshots were viewed in the session scratchpad only, and deleted afterwards.

## Checks
- `curl -sS -m 25 -A 'Mozilla/5.0' https://www.tmdn.org/tmview/api/trademark/detail/EM500000010718542`: fail (HTTP 403 `{"error":"Forbidden"}`; EU status not readable)
- `curl -sS -m 25 -A 'Mozilla/5.0' https://euipo.europa.eu/copla/trademark/data/010718542`: fail (connection reset by peer)
- `curl -sS -m 25 -A 'Mozilla/5.0' https://tsdr.uspto.gov/ts/cd/casestatus/sn85842584/info.xml`: fail (HTTP 429 Too Many Requests; US status read from the Docket Alarm mirror instead, [S1] [S2] [S6])
- `curl -sS -m 25 -A 'Mozilla/5.0' https://trademarks.justia.com/858/42/candy-85842584.html`: fail (HTTP 403, Cloudflare challenge)
- `curl -sS -m 25 -A 'Mozilla/5.0' "https://kenney.nl/assets?search=<q>"` for q = candy, sweet, food, background, dessert: pass (candy → platformer-art-candy; food → food-kit, pixel-platformer-food-expansion; background → background-elements, background-elements-remastered, shooting-gallery; sweet and dessert → none)
- `curl -sS -m 25 -A 'Mozilla/5.0' https://kenney.nl/assets/platformer-art-candy`: pass (200; "License Creative Commons CC0", "Released in 2013", "100×" files)
- `curl -sS -m 60 -A 'Mozilla/5.0' -o dl/pac.zip https://kenney.nl/media/pages/assets/platformer-art-candy/85fe3ec6ed-1677696339/kenney_platformer-art-candy.zip && unzip -p dl/pac.zip license.txt`: pass (200, 590,798 bytes, 103 files; CC0 licence text quoted in §4b)
- `shasum -a 256 dl/pac.zip`: pass (`28f1dcdd38a9fbfba917531ed6d86887537248b150858f7d24f92c167bd45639`)
- `curl -sS -m 25 -A 'Mozilla/5.0' https://kenney.nl/support`: pass (200; "all game assets on the asset pages are public domain licensed (CC0)", "Attribution is not required")
- `curl -sS -m 25 -A 'Mozilla/5.0' https://opengameart.org/content/<item>`, for super-candy-set-m484-games, candy-pack, colored-handful-candy, candy-button-pack and more-candy-m484-games: pass (200 each; licence field `creativecommons.org/publicdomain/zero/1.0/` on all five)
- `curl -sSL -m 25 -A 'Mozilla/5.0' https://www.king.com/game/candycrush`: pass (200, redirects to https://www.candycrush.com/)
- `curl -sSL -m 25 -A 'Mozilla/5.0' "https://play.google.com/store/apps/details?id=com.king.candycrushsaga&hl=en"`: pass (title "Candy Crush Saga - Apps on Google Play", developer "King")
- `grep -rniI -E 'candy|crush|saga|sweet|lollipop|jelly' --include='*.xml' --include='*.kt' --include='*.md' --include='*.txt' --include='*.json' .` and `grep -n 'app_name' app/src/main/res/values/strings.xml` (in the worktree): pass (no user-facing string contains these words; "candy" appears only in code comments, docs and `.planning/`; `app_name` is `Peg Solitaire`)
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-candy status --short` (before the commit): pass (only `?? .planning/reports/candy-backdrop/`, at develop tip 3f2af90)
- `./gradlew check`: not run. This is a report-only change, with no code or resource touched.
- (addendum 2026-10-18) `sed -n '36,56p;238,280p' docs/ux/backdrop-directions.html` and `sed -n '483,565p' docs/ux/backdrop-directions.html`: pass (B's tokens, styles and section #b read. Light alphas, centre against rim: 0.80/0.95 and 0.30/0.55. Dark: 0.14/0.24 and 0.16/0.28)
- (addendum) `curl -sSL -m 30 -A 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128 Safari/537.36' "https://play.google.com/store/apps/details?id=<pkg>&hl=en&gl=us"` for com.king.candycrushsodasaga, com.king.candycrushjellysaga and com.king.candycrushsaga: pass (200 each; titles "Candy Crush Soda Saga", "Candy Crush Jelly Saga" and "Candy Crush Saga - Apps on Google Play")
- (addendum) the same command for com.king.candycrushsolitaire: fail (404; the id was a guess). For com.king.apps.candysolitaire, the id linked from the King listings: pass (200, "Candy Crush Solitaire - Apps on Google Play", developer "King", developer id 6577204690045492686, the same as Candy Crush Saga's)
- (addendum) `curl -sSL -m 30 -A 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128 Safari/537.36' "https://play.google.com/store/apps/dev?id=6577204690045492686&hl=en"`: pass (200, "Android Apps by King on Google Play"; 10 titles, none containing "gel")
- (addendum) `curl -sSL -m 30 -o "img/<game>-<n>.png" "<screenshot url>=w540-h960-rw"`, 5 per listing, then `ffmpeg -i <5 files> -filter_complex "…hstack=inputs=5" sheet-<game>.jpg`: pass (four contact sheets, viewed in the session scratchpad; findings in A.1 and A.2; the files were deleted afterwards)
- (addendum) `curl -sSL -m 25 -A 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128 Safari/537.36' http://www.docketalarm.com/trademarks/86732458/`: pass (200; CANDY CRUSH JELLY SAGA, filed 2015-08-21, King.com Limited; status "An Office action suspending further action on the application has been sent (issued) to the applicant"; the mirror was last updated about ten years ago)
- (addendum) `curl -sSL -m 25 -A 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128 Safari/537.36' https://trademarks.justia.com/owners/king-com-limited-3012933`: fail (HTTP 403, Cloudflare challenge)
- (addendum) `curl -sSL -m 25 -A 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128 Safari/537.36' https://tsdr.uspto.gov/ts/cd/casestatus/sn86732458/info.xml`: fail (HTTP 429 Too Many Requests)
- (addendum) `curl -sSL -m 20 -A 'Mozilla/5.0' https://raw.githubusercontent.com/facebookarchive/shimmer-android/main/README.md`: pass (200; "add a shimmer effect to any view", "originally developed for Facebook Home", BSD License)
- (addendum) WebFetch of https://en.wikipedia.org/wiki/Aqua_(user_interface), https://en.wikipedia.org/wiki/Frutiger_Aero, https://en.wikipedia.org/wiki/Peg_solitaire and https://www.apple.com/newsroom/2000/01/05Apple-Unveils-Mac-OS-X/: pass (the quotes used in A.1 and A.4)
- (addendum) `git -C /Users/ermanergoz/Desktop/PegSolitarie-candy show feature/phase-14-release-docs:THIRD_PARTY.md | sed -n '79p'` and `… show develop:THIRD_PARTY.md`: pass (the row is at line 79 and carries the ship clause; develop c3d3437 has no THIRD_PARTY.md)
- (addendum) `git -C /Users/ermanergoz/Desktop/PegSolitarie-candy status --short` and `… diff --cached --name-only` (before the commit): pass (only this report modified, nothing staged, at f36d375)
- (addendum) `./gradlew check`: not run. Report-only change.

## Open issues / risks
1. **EU register status unverified.** EUIPO and TMview refused scripted access. The EU CANDY word mark (and the outcome of ZeptoLab's 2014 cancellation) and the 2012–2013 CANDY CRUSH EUTMs are treated as live. No action is needed, because §3 avoids them all. Any future wish to put "candy" in user-facing text escalates here, and a human EUIPO eSearch lookup would settle it.
2. **Callout list partly unverified.** "Sugar Crush" is sourced [S17]. "Sweet / Tasty / Delicious / Divine" is not verified from a primary source. All are banned anyway (§3a).
3. **Legibility is not an IP matter but is the next risk.** Pink candy-sphere pegs on a sweets backdrop can lose contrast against busy motifs. Board and peg legibility, and the 4.5:1 text contrast, are the ux-designer's call. Flagged, not judged here.
4. **THIRD_PARTY.md is not on develop.** It is on `feature/phase-14-release-docs`. The §6 idea row must land on whichever branch carries the file before the backdrop ships. The release gate reads that file.
5. **Founder expectation.** The founder asked for a backdrop "like candy crush". What this verdict allows is a candy-themed scene in the game's own style, not Candy Crush's look. A line for the producer to relay in its next titled update (information, not a question): "The menu and game backgrounds will get a candy-themed scene drawn in the game's own style. They can't copy Candy Crush's look: King's names, candies, characters and art are protected, and copying them risks a Play suspension."
6. **Skill deviations.** The research skills' AskUserQuestion intake was skipped: the dispatch answered every intake question, and only the producer addresses the founder. Their `artifacts/research/` file was also skipped, because this report is the single output the dispatch names.
7. **Commit trailer.** The commit carries the project rule's single trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` (`.claude/rules/gitflow.md`, imported by CLAUDE.md, which the harness reminder defers to). No prepare-commit-msg hook is installed in this repository to add it, so it was written by hand.

## Handoff
Next: producer:
1. Dispatch the **ux-designer** to specify the Home (menu) and gameplay backdrops under §3 and `docs/ui/DESIGN-SYSTEM.md`: light and dark, honouring the animator duration scale, with the peg board and Home tiles as hero objects.
2. Dispatch the **android-game-dev** to build them as Compose Canvas or vector drawables, with no raster, colours only from `Color.kt`, and dimensions in `dimens.xml`.
3. Give the **code-auditor** and **qa-engineer** §2's two-question test and §3b/§3c as acceptance items.
4. Have the §6 idea row added to THIRD_PARTY.md on the branch that carries the file, before the backdrop ships.
5. Have the **release-manager** check listing text against §3a at every listing change. That includes the Play Games achievement and leaderboard names held only in the Play Console, which the in-repo grep cannot see.
6. Relay the open-issue 5 line to the founder.

Anything that wants a King element, the word "candy" in user-facing text, or an asset outside CC0/CC-BY comes back to the research-analyst as an escalation.

## Addendum 2026-10-18 — direction B
Date: 2026-10-18
Branch: feature/candy-backdrop (report only)
Skills loaded: ergoz-soft:standards (plugin rules v1.0.0 read in full, because the product snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; product `.claude/rules/lessons.md`), research-open-source, research-competitive, market-research
Ran in parallel with: candy-backdrop spec rewrite for direction B (ux-designer), phase-15 H-2 developer fix
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

Inputs: the founder picked direction B "Deep gloss" (Q-021, 2026-10-18). Read: `docs/ux/backdrop-directions.html` (the B tokens at lines 40–52, the B styles at lines 238–278, section #b at lines 483–561), `.planning/reports/candy-backdrop/ux-designer-directions.md` (open issues 1, 3 and 4), and this report's §2 and §3.

**VERDICT: clean.** Direction B "Deep gloss" (Q-021) is built from generic convention and passes §2's two-question test, on the conditions in A.3 and the word rules in A.4. The inspiration-only verdict stands. Not escalated: nothing is reused, and no King mark is used.

### A.1 Element by element
| B element, as drawn on the board | Already free under | Generic precedent | Nearest King look | Status |
|---|---|---|---|---|
| Deep violet gradient world (dark `#1C0E38` to `#3A0F48`; light lavender to pink-lilac) | §2 "bright saturated gradients"; §3c: colours are free, violet leads | The app's own violet `#7C3AED` system | Candy Crush Soda Saga's purple background, which is its "purple soda" [S33] [S44] | Clean, on A.3 (2) and (3) |
| Two glossy colour waves low on the screen, bobbing on 9 s and 12 s | §2 gradients and glossy highlights; 3b "soft rounded hills", abstracted | Aurora and wave backgrounds: "aurorae" and "glossy textures" are named motifs of the Frutiger Aero interface style, 2004–2013 [S40] | Soda Saga's purple soda, whose wavy, lighter surface rises in the board and in a bottle meter [S33] [S45]; Candy Crush Jelly Saga's glossy pink jelly band along the foot of its store screenshots [S34] | Clean, on A.3 (2) and (3) |
| Soft "bokeh" lights (9 on Home, 5 in game) rising and fading on 22–40 s loops | Closest: §2 "soft specular dots", 3b "slow-drifting soft blobs". Not word for word | Bokeh is the photographic term for out-of-focus discs of light [S41]; "bokeh effects" is a named Frutiger Aero motif [S40] | Soda Saga's background bubbles: hollow rings with a highlight, rising [S33] | Clean, on A.3 (1). Added to the free list below |
| Wet gloss highlight and lip on panels | §2 "glossy highlights" and "a translucent or raised panel", word for word | Mac OS X Aqua (2000): "luminous and semi-transparent elements" [S39], "reflective effects and translucency" [S38]; glossy buttons were a Web 2.0 cliché by 2008 [S46] | King's glossy header bars (pink in Candy Crush, green in Jelly Saga): generic UI, not King's expression | Clean |
| Gel mode cards: solid violet and pink gradients, white text, a shine lip, a sheen crossing every 10 s | §2 gradients and glossy highlights | Aqua's controls had "a 'glass' or 'gel' effect" until OS X Yosemite [S38]; a sheen band crossing a view is the common shimmer effect, e.g. Facebook's BSD-licensed Shimmer for Android, made for Facebook Home in 2013 [S42] | Candy Crush's glossy candy rendering, which is gloss on a set of small coloured tokens [S30]; Jelly Saga's translucent jelly cells and jelly blobs [S34] | Clean, on A.3 (4) to (6) |

**Added to §2's free list** (convention and idea):
- soft out-of-focus "bokeh" light discs, drifting or rising and fading;
- broad glossy colour waves or bands;
- a gloss or "wet" highlight and lip on panels;
- gel-finish (Aqua-style) buttons and cards;
- a single sheen or shimmer band crossing a control.

### A.2 The two questions, applied to B's frames
1. *Could any single element be traced to, or recognised as, a King design?* **No.** Each element is a convention that is older than Candy Crush (2012) or common across interface design: Aqua's gloss and gel (2000), Frutiger Aero's gloss, aurorae and bokeh (from 2004), the shimmer effect, and photographic bokeh. B draws no candy, character, tile grid, King lettering or King scene. The board, the pegs and the slab are unchanged and opaque.
2. *Would a player shown one screenshot think it is Candy Crush, a spin-off, or a reskin?* **No, as drawn.** The nearest King look found is Soda Saga's purple-soda world: a violet background, a wavy liquid surface and rising ring bubbles [S33]. B differs on the three points that A.3 (1) to (3) lock down:
   - its lights are filled discs, not rings;
   - its waves are light behind the UI, not liquid in or around the board;
   - nothing rises with game state.

   Candy Crush Solitaire (A.4) is green felt, playing cards, candy pieces and coins [S32]. B draws none of these.

How this was checked: I viewed five Play screenshots each of Candy Crush Saga, Soda Saga, Jelly Saga and Candy Crush Solitaire, in the session scratchpad only, and deleted them afterwards. §3b's rule is that no King image enters the repository, and none did.

### A.3 What B must avoid (added to 3b; reviewers check each line)
1. **Lights stay bokeh, not bubbles.**
   - Each light is a filled disc whose edge fades out. Its centre alpha is at least half its rim alpha; the board draws 0.30 against 0.55 at the widest gap.
   - No hollow ring, outline stroke, highlight dot or crescent.
   - No clusters of tiny dots ("fizz"), and no popping or bursting.
   - A light never appears to emerge from a wave crest.

   Soda Saga's ring bubble is what makes violet plus rising read as soda.
2. **Waves stay light, not liquid.**
   - Broad smooth crests, with at most one lip and one soft highlight per wave, as drawn.
   - No drips, droplets, splash or foam, and no bubbles on the crest.
   - The only motion is the bob (a translate, as drawn). No wobble.
   - A wave never rises or falls with game state: pegs left, moves, progress or level number.
   - A wave is never drawn inside, over or through the board slab.
   - Nothing anywhere in the app is shown as liquid filling a bottle, tube or meter. That is Soda Saga's soda level and bottle meter [S33] [S45].
3. **No pink jelly band.**
   - In the light theme, a wave's body stays lilac to violet below its pink crest (`#DD8BE6` to `#B98AF2`, as drawn).
   - A wave is never a saturated, opaque hot-pink band with streak highlights along the foot of the screen, and never carries text or a caption. That is Jelly Saga's store art [S34].
   - Violet stays the lead colour in both themes (§3c), and the violet Classic card stays above the pink Challenge card.
   - Store screenshots stay plain captures (§3d).
4. **Gel stays on UI containers**: the two mode cards, the panels and the dock discs.
   - Never on pegs, holes, move dots, stars or any other small token. Gloss on a set of small coloured tokens is how Candy Crush renders its candies.
   - Never more than the two gel colours, violet and pink, both from `Color.kt`.
   - The board and the pegs stay exactly as they are today.
5. **Gel cards stay cards**: opaque rounded rectangles with an icon and a label, as drawn.
   - No twisted ends (a wrapped sweet), stripes, sprinkles, drips or wavy bottom edge.
   - No see-through body.
   - No squash-and-wobble ("jelly") motion, at idle or on press.
   - The sheen stays one soft band, at most once per 10 s per card.
6. **No translucent coloured layer on board cells.** That layer is exactly Candy Crush's "jelly" squares and Jelly Saga's spreading jelly. The slab's gloss stays one lip along its top edge.
7. **No multi-hue colour field behind the board.** Candy Crush's in-level screens use large swooshes of yellow, red, orange, purple and green [S30]. B's hue arc of 250°–335° already keeps this out (the ux-designer's report, "Hue arc").
8. **Everything in §3 still applies**: no tile grid, no candy lettering, no characters, and no King image in the repository or in a design file.

### A.4 Words: "gel", "jelly", "Solitaire"
**"gel"**
- **Safe internally**: identifiers such as `GelCard` and `gelLip`, tokens, comments, docs and `.planning/`.
- **User-facing, allowed only as a plain descriptive word in running text.** Never use it as the name of a mode, theme, setting, feature or achievement: a name would need a register lookup, and this machine cannot make one (USPTO and EUIPO refuse scripted access; see Checks).
- Never pair it with a sweet word ("candy gel", "gel drops", "jelly gel").
- No King title or filing found uses GEL. King's ten titles on its Play developer page [S35] and its jelly filings [S36] use JELLY.
- In practice, no copy in the app needs the word.

**"jelly"**
- **Never user-facing**, anywhere 3a covers.
- King publishes *Candy Crush Jelly Saga* [S34] and filed CANDY CRUSH JELLY SAGA (US serial 86732458) and CANDY CRUSH JELLY on 2015-08-21 [S36] [S37]. "Jelly" is also an objective inside Candy Crush Saga and Jelly Saga.
- The US status shown by the mirror is an Office action suspending the application, last updated about ten years ago. The mark is therefore presumed live, as §1a does for the others.
- Internal identifiers would be legally harmless, since they never reach a player. The spec's B-10 keeps the word out of them as well. Keep that; it costs nothing.

**Added to 3a's never-write list**, in any use and not only as callouts: `jelly`, `soda`, `sugar`, `sweet`. King's titles use "jelly" and "soda", and its listings use "sweet" ("a delightful sweet twist" [S32]; "this sweet puzzle game" [S33]). "Sugar" is in King's "Sugar Crush" callout (§1b). B draws no sweets, so none of these words has an honest use.

**New fact that §1 missed: Candy Crush Solitaire.**
- King launched it worldwide on 2025-02-06 [S31].
- It is on Play as `com.king.apps.candysolitaire`, by developer "King". Its short description reads "Enjoy TriPeaks solitaire - the classic card game with a delightful sweet twist!" [S32].
- Our title `Peg Solitaire` is the game's generic name, documented since 1697 [S43]. It stays.
- B's verdict does not change, but the stakes on words rise. In the title, the short and full descriptions, the release notes and any screenshot overlay:
  - never put "Solitaire" next to a sweet word or a "twist" phrase;
  - never decorate with playing cards or a green felt table.

### A.5 THIRD_PARTY.md idea row (replaces §6's)
**Where the row lives.** THIRD_PARTY.md is still not on `develop` (c3d3437). It is on `feature/phase-14-release-docs` (d11112d), with the row at line 79.

**What changes.** The row's Item cell there carries the release-manager's ship clause: "**Ships from `feature/candy-backdrop` (not yet built)**; if it does not reach `release/3.0.0`, delete this row at checklist 1.4". Keep that clause. Only the "What was taken" cell changes. The Item, Source, Licence and Credit cells stay as they are.

The new "What was taken" cell:

```
The idea of a lively, glossy backdrop behind the menu and the board, and nothing else. The look is direction B "Deep gloss" (Q-021): a deep violet gradient, two glossy colour waves low on the screen, soft out-of-focus lights that rise and fade, a gloss highlight on panels, and gel-finish Home mode cards with a passing sheen. These are generic glossy-UI conventions (the gel controls of Mac OS X Aqua, 2000; the gloss and bokeh of 2004–2013 interface design), and no candy, sweet, character or other King element is drawn. Every shape, colour stop, motion and composition is authored in this repo (`ui/theme/Color.kt` tokens, Compose Canvas)
```

The whole row, as §6 gave it, with the new cell in place (add the ship clause to the Item cell as the file has it):

```
| Candy-themed backdrop for the Home (menu) and gameplay screens | The candy-themed casual-game genre; the founder's named reference was *Candy Crush Saga* by King.com Limited — https://www.candycrush.com/ , https://play.google.com/store/apps/details?id=com.king.candycrushsaga | Not protected — a visual theme is an idea, not an expression (`rules/ip-policy.md`). King's names, logos, characters, candy designs and artwork are protected, and **none was taken** — verdict *inspiration only*, `.planning/reports/candy-backdrop/research-analyst.md` | The idea of a lively, glossy backdrop behind the menu and the board, and nothing else. The look is direction B "Deep gloss" (Q-021): a deep violet gradient, two glossy colour waves low on the screen, soft out-of-focus lights that rise and fade, a gloss highlight on panels, and gel-finish Home mode cards with a passing sheen. These are generic glossy-UI conventions (the gel controls of Mac OS X Aqua, 2000; the gloss and bokeh of 2004–2013 interface design), and no candy, sweet, character or other King element is drawn. Every shape, colour stop, motion and composition is authored in this repo (`ui/theme/Color.kt` tokens, Compose Canvas) | — deliberately not credited in the app: naming King in the app or the listing would imply a relationship Play's Impersonation policy forbids; this row stays out of any in-app licences screen |
```

### A.6 Open issues / risks
1. **Register access refused again.** Justia returned 403 and USPTO TSDR returned 429 for the jelly filings. Both filings are treated as live, which costs nothing.
2. **§1 missed Candy Crush Solitaire (2025).** A.4 corrects this. §1a's table is not re-cut; this line is the record.
3. **Direction C is moot.** Had it been picked, its soap bubbles (rings with a highlight) would have needed the Soda Saga distinction in A.3 (1).
4. **Play Console-only names.** The achievement and leaderboard names live only in the Play Console, which the in-repo grep cannot see. They need checking against A.4's words too.
5. **Skill deviations, as in open issue 6.** The research skills' AskUserQuestion intake was skipped: the dispatch answered it, and only the producer addresses the founder. No `artifacts/research/` file was written, because this addendum is the single output.
6. **Commit trailer, as in open issue 7.** `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` was written by hand, because no prepare-commit-msg hook is installed.

### A.7 Handoff
Next: producer.
1. **ux-designer** (rewriting `docs/ux/candy-backdrop.md` now): carry A.3 into BD-03 and the §10 verdict checklist, and A.4's words into B-10.
2. **code-auditor and qa-engineer**: at the build's review, use §2's test, 3b, 3c and A.3 as acceptance items.
3. **release-manager**: before the backdrop ships, swap A.5's cell into THIRD_PARTY.md line 79 on the branch that carries the file. Check the listing and the Play Console names against 3a and A.4.
4. **Founder**: nothing to queue.

### A.8 Sources (continuing the list above)
All read 2026-10-18. "Search extract" means the claim comes from the search result's summary of that page, not from opening the page.

- [S31] Business Wire, "A Beloved Classic Just Got Sweeter Than Ever; Candy Crush Solitaire™ Launches Worldwide" (2025-02-06): https://www.businesswire.com/news/home/20250206977362/en (search extract)
- [S32] Google Play, Candy Crush Solitaire (developer "King"): https://play.google.com/store/apps/details?id=com.king.apps.candysolitaire
- [S33] Google Play, Candy Crush Soda Saga: https://play.google.com/store/apps/details?id=com.king.candycrushsodasaga. Its screenshots show a purple gradient background, ring bubbles, purple soda with a wavy surface in the board, and a soda bottle meter. Short description: "Pop soda, crush cookies and keep matching candy in this sweet puzzle game!"
- [S34] Google Play, Candy Crush Jelly Saga: https://play.google.com/store/apps/details?id=com.king.candycrushjellysaga. Its screenshots show the caption "Spread the jelly!", a glossy pink jelly band, jelly blobs and pink jelly cells. Short description: "A fun jellylicous match 3 game, solve the puzzles & defeat the Jelly Queen!"
- [S35] Google Play, "Android Apps by King": https://play.google.com/store/apps/dev?id=6577204690045492686 (Blossom Blast Saga, Bubble Witch 2 Saga, Bubble Witch 3 Saga, Candy Crush Friends Saga, Candy Crush Jelly Saga, Candy Crush Saga, Candy Crush Soda Saga, Farm Heroes Saga, Farm Heroes Super Saga, Pet Rescue Saga)
- [S36] Docket Alarm (USPTO mirror), CANDY CRUSH JELLY SAGA, serial 86732458: http://www.docketalarm.com/trademarks/86732458/
- [S37] Justia Trademarks, King.com Limited owner listing: https://trademarks.justia.com/owners/king-com-limited-3012933 (refused; the CANDY CRUSH JELLY SAGA filing and the same-day CANDY CRUSH JELLY filing come from the search extract)
- [S38] Wikipedia, Aqua (user interface): https://en.wikipedia.org/wiki/Aqua_(user_interface)
- [S39] Apple Newsroom, "Apple Unveils Mac OS X" (2000-01-05): https://www.apple.com/newsroom/2000/01/05Apple-Unveils-Mac-OS-X/
- [S40] Wikipedia, Frutiger Aero: https://en.wikipedia.org/wiki/Frutiger_Aero
- [S41] Wikipedia, Bokeh: https://en.wikipedia.org/wiki/Bokeh (search extract)
- [S42] Meta, Shimmer for Android, README and BSD licence: https://github.com/facebookarchive/shimmer-android
- [S43] Wikipedia, Peg solitaire: https://en.wikipedia.org/wiki/Peg_solitaire
- [S44] Pocket Gamer, King soft-launches Candy Crush Soda Saga on Android: https://www.pocketgamer.com/candy-crush-soda-saga/king-soft-launches-candy-crush-saga-spin-off-candy-crush-soda-saga-on-android/ (search extract: the purple soda seen in the background makes the candies float)
- [S45] Giant Bomb, Candy Crush Soda Saga: https://www.giantbomb.com/games/3030-48504/ (search extract: matching bottle candies raises the soda level)
- [S46] 24 ways, "Shiny happy buttons" (2008): https://24ways.org/2008/shiny-happy-buttons/ (search extract: Mac OS X popularised glossy buttons, a Web 2.0 cliché by 2008)
