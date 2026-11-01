# research-analyst report — licences-screen (the MPL-2.0 text, LS-11)
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role research-analyst). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and ip-policy. I also read the product's `.claude/rules/lessons.md`. I loaded research-open-source, research-competitive and market-research because the role lists them. I did not use their AskUserQuestion intake or their `artifacts/research/` output: only the producer talks to the founder, and this dispatch allows one output file.
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Verdict: CLEAN for the MPL-2.0 text.** It may ship verbatim inside a commercial app. LS-11 stands, and `licences-screen.md` §9 item 6 is closed.
- **The Public Suffix List stays ATTRIBUTION REQUIRED (THIRD_PARTY.md row J).** The NOTICE, the source address and the bundled text meet MPL-2.0 §3.2(a), on one condition. The entry must also name the exact version the app ships. The address it shows serves today's list, but OkHttp compiled the list as it stood on 2023-02-15.
- **Bundle** https://www.mozilla.org/media/MPL/2.0/index.txt: 16,726 B, SHA-256 `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04`.
- No escalation and no founder question.

## Verdicts

| # | Question | Answer | Matrix row (`rules/ip-policy.md`) | Verdict |
|---|---|---|---|---|
| 1 | May the MPL-2.0 text itself be redistributed verbatim inside a commercial app? | Yes. The licence reserves only modified and new versions to its steward. It is written to travel with the files it covers, and Mozilla's FAQs say to ship it as-is | No row covers a licence document. "No license / unknown" does not apply, because the steward publishes the text for this use. The CC-BY-SA row was considered and does not apply (§1 below) | **CLEAN** |
| 2 | Does bundling it, with the NOTICE and the source location, satisfy §3.2(a) for an unmodified Executable Form? | Yes, once the entry also names the exact version shipped. The licence text itself is not required by §3.2, though Mozilla calls shipping it "the best and easiest way" | MPL-2.0: "yes (file-level copyleft) … modified files stay MPL". The file ships unmodified, so nothing else in the app becomes MPL | **ATTRIBUTION REQUIRED** (row J, unchanged). It is met by the spec plus one line |
| 3 | Which text and URL should be bundled, and what is its SHA-256? | Mozilla's plain text at https://www.mozilla.org/media/MPL/2.0/index.txt: 16,726 B, SHA-256 `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04` | — | The spec's §7 prefix `3f3d9e0024b1921b` is confirmed |

### 1. The MPL-2.0 text may be copied verbatim

**What the text says about itself.** It carries no copyright line and no copying clause. It has nothing like the GPL's "Everyone is permitted to copy and distribute verbatim copies" (a grep of the file finds none). Three of its own provisions decide the question:
- **What it reserves.** §10.1: "Mozilla Foundation is the license steward. Except as provided in Section 10.3, no one other than the license steward has the right to modify or publish new versions of this License." §10.3 lets others make a modified licence only under a new name. Verbatim copying is not reserved.
- **What it expects of copies.** §3.1 obliges source distributors to tell recipients "how they can obtain a copy of this License". Exhibit A reads: "If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/." The licence is designed to travel with files.
- **Commercial use.** Nothing in the text separates commercial distribution from any other kind.

**What Mozilla says.** The steward's own FAQs say this directly:
- MPL 2.0 Revision FAQ, item 10: "In most cases, distributing the license with the code - as was required in MPL 1.1 - is still the best and easiest way to meet this requirement."
- MPL 2.0 FAQ (last updated January 30th, 2024):
  - Q21: "If you like the MPL 2.0, just use it as-is".
  - Q25 treats a project that "just ships a copy of the full MPL 2 with their code" as ordinary.
  - Q8: "You may distribute any executables you create under a license of your choosing".
- The licence's landing page, https://www.mozilla.org/en-US/MPL/, offers the text for download: "The current version of the license is MPL 2.0 (html | plain text)."

**One reading I considered and rejected.** mozilla.org's site policy says: "Unless otherwise indicated, the text of documents in this collection is available under the Creative Commons Attribution Share-Alike 3.0 Unported license". I do not think it reaches the licence text, for three reasons:
- The MPL 2.0 page and its plain text carry no Creative Commons notice. The FAQ page has a CC footer; the licence page has none.
- The licence document states its own terms (§10). The steward also tells everyone to ship it with their code, which counts as "otherwise indicated".
- Even if CC BY-SA 3.0 did apply, the matrix's reason to escalate CC-BY-SA is share-alike. Share-alike binds only Adaptations, and CC BY-SA 3.0 §1(a) says "a work that constitutes a Collection will not be considered an Adaptation for the purpose of this License". A verbatim copy inside the app is in a Collection, so share-alike cannot reach the app.

The row is not triggered and nothing escalates.

**Re-flow and name.**
- **Re-flow (LS-07).** It changes no word, and the asset ships byte-identical at its hash. Mozilla itself changed only the text's whitespace in 2022–2023 without issuing a new version (§3 below), so whitespace is not the licence's substance.
- **Name.** "Mozilla Public License 2.0", used as a heading, names the licence and so is a nominative use of the mark. No Mozilla logo is shown. No trademark question arises.

### 2. §3.2(a) for the unmodified Public Suffix List data

**The shipped file is Executable Form, so §3.2 binds the app's distributor.**
- §1.13 defines "Source Code Form" as "the form of the work preferred for making modifications". §1.6 defines "Executable Form" as "any form of the work other than Source Code Form".
- OkHttp's generator "Downloads the public suffix list … and transforms the file into an efficient format used by OkHttp". The result is a sorted, length-prefixed, gzipped list of 9,499 rules and 8 exceptions, with every comment and the Exhibit A header stripped.
- The FAQ's Q16 treats the comparable case, minified JavaScript, as Executable Form. It says the boilerplate need not survive, but §3.2(a) must still be met: "one way would be to include a comment with a link to the source code".

**What §3.2(a) asks:** "such Covered Software must also be made available in Source Code Form, as described in Section 3.1, and You must inform recipients of the Executable Form how they can obtain a copy of such Source Code Form by reasonable means in a timely manner, at a charge no more than the cost of distribution to the recipient".

**What the spec already provides:**
- **The source is available.** The publisher keeps it public and free: "The list is kept in source code control on GitHub" (publicsuffix.org/list/), and the list "is available for use in any software" (publicsuffix.org).
- **Recipients are told where.** OkHttp's NOTICE is shown verbatim: "Note that publicsuffixes.gz is compiled from The Public Suffix List: https://publicsuffix.org/list/public_suffix_list.dat It is subject to the terms of the Mozilla Public License, v. 2.0: https://mozilla.org/MPL/2.0/". The address is also shown as selectable text on the entry.
- **The means are reasonable.** FAQ Q8: "inform the recipients where they can get the source for the MPLed code in the executable program or library you are distributing". FAQ Q26: distribution "happens almost exclusively over the internet". A web address is enough.

**The one gap: "such Covered Software" means the version the app ships.**
- The address serves a copy "updated daily from GitHub". On 2026-10-24 it returned a 338,012 B file headed `VERSION: 2026-10-22_08-36-41_UTC`. That file differs from the shipped copy by 10,388 diff lines.
- The copy OkHttp 4.11.0 compiled is the list as of PSL commit `a312abe0e11421aff8fe909a5e02f36b7c5d28de` (2023-02-15, "util: gTLD data autopull updates for 2023-02-15T15:49:52 UTC (#1705)"). Each step of the chain was checked:
  1. **One run writes both files.** OkHttp's generator downloads the list into `okhttp/src/test/resources/okhttp3/internal/publicsuffix/public_suffix_list.dat` and compiles that same file into `publicsuffixes.gz`. At tag `parent-4.11.0`, both files were last changed in one commit: `40cb04338da4`, 2023-04-22, "[4.x] Public suffix import (#7701)".
  2. **The tag's `.dat` compiles to the tag's `.gz` exactly.** I decoded the `.gz` and parsed the `.dat` with the generator's own rules (`pslcmp.py`). Both give 9,499 rules and 8 exceptions, and both lists are equal in order.
  3. **The shipped `.gz` is that file.** It is byte-identical, SHA-256 `9af43e9995ec749e7a30eec4e5a87770d7e2027847084a07cca82060eff0c791`, in three places: at the tag, in the Gradle cache's `okhttp-4.11.0.jar`, and in the app's 2026-09-21 release Java resources (`merged_java_res/release/minifyReleaseWithR8/base.jar`).
  4. **One PSL revision matches.** The tag's `.dat` is 246,016 B, SHA-256 `e8b273972eb5a70e888bd3e7d7c5b9b04e600a59d69def9136a74d70ae6fcdd3`, git blob `fcdecb55…`. That blob occurs in exactly one of the 1,902 revisions of `public_suffix_list.dat` in `publicsuffix/list`: `a312abe0…`. The raw file at that commit has the same SHA-256.
- **The condition.** The entry must name the exact version. The address to show is `github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat`. It is the publisher's own repository at a commit address, which cannot move, unlike a tag. An equal copy sits at `github.com/square/okhttp/blob/parent-4.11.0/okhttp/src/test/resources/okhttp3/internal/publicsuffix/public_suffix_list.dat`. Which string it is and where it sits on the entry is the ux-designer's call; the NOTICE already names the live list.

**Not required by §3.2, and still right to keep:**
- **The MPL text.** §3.2 places no duty to give a copy of the licence on whoever distributes the Executable Form. §3.1's duty falls on source distributors, and the NOTICE already gives the licence's address. Bundling the text is what Mozilla's Revision FAQ calls "the best and easiest way". It also keeps LS-05's rule that the screens work in airplane mode.
- **Standing conditions, not tasks:**
  - §3.2(b) and §3.3: the app may stay under its own terms only while those terms do not "limit or alter the recipients' rights in the Source Code Form". Nothing I read in this task's inputs does. Any future terms of use must carve out third-party open-source components.
  - §3.4: the studio removes no notice. The compiled file never carried the header, and the NOTICE carries it.

### 3. The canonical text

- **URL:** https://www.mozilla.org/media/MPL/2.0/index.txt, the steward's plain text.
  - The "plain text" link on https://www.mozilla.org/en-US/MPL/ currently points at the content-hashed name `index.f75d2927d3c1.txt`, which serves the same bytes. Record the stable, unhashed name: it is served `cache-control: max-age=315360000, public, immutable`, with ETag `f75d2927d3c1ed2414ef72048f5ad640`, which equals the file's MD5.
- **Bytes:**
  - 16,726 B in 373 lines; ASCII, LF only, no BOM, ending in one newline.
  - It opens with "Mozilla Public License Version 2.0".
  - Its Exhibit A address is `https://mozilla.org/MPL/2.0/`, which resolves to https://www.mozilla.org/en-US/MPL/2.0/.
- **SHA-256:** `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04`. This equals the prefix in spec §7 and in row J.
- **Stability:**
  - The Wayback Machine's digest for these exact bytes (SHA-1 base32 `27R62WWBJGWB4LJOB5G274EBYHNP54OA`) appears in captures from 2023-03-20, 2025-03-02, 2025-06-25, 2025-09-06 and 2026-03-23.
  - A gzip capture from 2026-07-20 decompresses to the same SHA-256.
- **Copies not to bundle.** Each is the same licence, differing only in whitespace or a URL scheme:
  - Mozilla's 2022 copy, which also equals SPDX's `MPL-2.0.txt` and the PSL repository's own `LICENSE`: 16,727 B, SHA-256 `66a3107d5ad6a058aab753eaac2047ccb2ed0e39465dd0fe5844da3e300d5172`. It has one trailing space at line 38.
  - Mozilla's 2015 copy: 16,726 B, SHA-256 prefix `fab3dd6bdab226f1`. It has that trailing space and `http://` in Exhibit A.
  - After LS-07's normalisation all three show the same words, apart from the 2015 URL scheme. The test pins one file, so bundle the steward's current copy and pin its full hash in LS-AC12.
- **The NOTICE beside it:** `PublicSuffixList-NOTICE.txt` is 218 B, full SHA-256 `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a`. That hash is the same at the OkHttp tag, in the jar and in the 2026-09-21 release build.

## Changes to transcribe

| Where (owner) | Change |
|---|---|
| `THIRD_PARTY.md` §5b row J, Source URL (release-manager) | Add the exact version: https://github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat (SHA-256 `e8b273972eb5a70e…`; the list compiled into okhttp 4.11.0's `publicsuffixes.gz`) |
| Row J, How credited (release-manager) | Replace "Optionally the MPL-2.0 text (16,726 B, sha256 `3f3d9e0024b1921b…`)" with: "and the MPL-2.0 text, `assets/licenses/MPL-2.0.txt`, verbatim from https://www.mozilla.org/media/MPL/2.0/index.txt (16,726 B, sha256 `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04`). Research-analyst, 2026-10-24: CLEAN, may ship verbatim. The entry also names the exact version shipped (Source URL)." Give the NOTICE its full hash, `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a` |
| `docs/ux/licences-screen.md` §3.2 Public Suffix List row and §6 (ux-designer) | The exact-version address as a selectable, non-linked line (a `translatable="false"` string). It can be added to, or replace, `licenses_psl_address`, because the NOTICE the entry opens already names the live list |
| `licences-screen.md` §7 MPL-2.0 row and §9 item 6 (ux-designer) | Closed: CLEAN, full SHA-256 as above. LS-11 stands |
| Release checklist 3.5, U-6 (release-manager) | If the release listing shows an okhttp other than 4.11.0, re-derive the address before the cut, the same way: hash that tag's test-resource `.dat` and find the PSL commit with the same git blob |

## Outputs
- `.planning/reports/licences-screen/research-analyst-mpl.md`: this report.
- I edited no other repo file and committed nothing, as dispatched.
- My working files sit in the session scratchpad, outside the repo: the fetched texts, a blob-less partial clone of `publicsuffix/list`, and two helper scripts (`h2t.py`, `pslcmp.py`).
- An earlier attempt at this task stopped on an API 529 before it wrote anything. This pass started again from the inputs.

## Checks
- `curl -sS -L https://www.mozilla.org/media/MPL/2.0/index.txt` then `shasum -a 256` — pass (200, 16,726 B, `3f3d9e00…b9d04`; ASCII, no CR, no BOM).
- `curl -sSI https://www.mozilla.org/media/MPL/2.0/index.txt` — pass (200; immutable caching; ETag `f75d2927…`, which equals the MD5).
- `curl -sS https://www.mozilla.org/media/MPL/2.0/index.f75d2927d3c1.txt` then `shasum -a 256` — pass (the same bytes).
- `grep -niE 'copyright|permitted|verbatim|steward|everyone'` over the text — pass (no copying clause; §10.1 and §10.3 as quoted).
- `curl` of SPDX's `MPL-2.0.txt`, then `diff` against Mozilla's text — pass (differs only by one trailing space at line 38).
- The Wayback CDX query for the text's URL, then fetches of the 2015, 2022, 2026-01 and 2026-07 captures, then `diff` — pass (2015: a trailing space and `http://`; 2022: a trailing space; 2026-07 gunzips to the live bytes; the 2026-01 capture is compressed and was not decoded). The live file's SHA-1 base32 equals the digest of the 2023-03-20 to 2026-03-23 captures.
- `curl` of the MPL FAQ, Revision FAQ, MPL landing page, MPL 2.0 page, licence policy, website-content policy and licensing page, then `python3 -I h2t.py` and `grep` — pass (quotes above). `grep -ci 'creative commons'` over the MPL 2.0 page — 0.
- `curl https://creativecommons.org/licenses/by-sa/3.0/legalcode` — pass (§1(a) Collection clause as quoted).
- `curl` of OkHttp's NOTICE at `parent-4.11.0`, then `shasum -a 256` — pass (218 B, `8a9c58fb…876a`).
- The GitHub tree API for `square/okhttp@parent-4.11.0` (301, then followed) — pass (the `.gz`, the NOTICE, the generator and the test-resource `.dat` are present).
- `curl` of `PublicSuffixListGenerator.java` at the tag — pass (it downloads into the test `.dat` and compiles it into the `.gz` in one run).
- The GitHub commits API for both files at the tag — pass (both last changed in `40cb04338da4`, 2023-04-22).
- `python3 -I pslcmp.py tag-publicsuffixes.gz tag-public_suffix_list.dat` — pass (9,499 rules and 8 exceptions on both sides; equal).
- `unzip -p` of the cached `okhttp-4.11.0.jar` and of the 2026-09-21 release `base.jar`, then `shasum -a 256` — pass (`.gz` `9af43e99…` and NOTICE `8a9c58fb…` in both, equal to the tag).
- `git clone --filter=blob:none --no-checkout https://github.com/publicsuffix/list.git`, then `git rev-parse <c>:public_suffix_list.dat` over `git log -- public_suffix_list.dat` — pass (1 match in 1,902 revisions: `a312abe0…`, 2023-02-15).
- `curl` of the raw `.dat` at `a312abe0…`, then `shasum -a 256` — pass (equals the tag's `e8b27397…`).
- `curl` of the PSL repository's `LICENSE` — pass (16,727 B, `66a3107d…`, the 2022 Mozilla copy).
- `curl https://publicsuffix.org/list/public_suffix_list.dat` — pass (338,012 B, `VERSION: 2026-10-22_08-36-41_UTC`, MPL-2.0 header; 10,388 diff lines against the shipped copy).
- `curl` of https://publicsuffix.org/ and https://publicsuffix.org/list/ — pass (quotes above).
- `curl -L https://mozilla.org/MPL/2.0/` — pass (200 after 2 redirects, at https://www.mozilla.org/en-US/MPL/2.0/).
- Not run: `./gradlew check`, because no code or build file was touched; and `git commit`, as dispatched.

## Open issues / risks
1. **The exact-version line is the one change.** Without it the entry still says where the list's source lives, as OkHttp's own NOTICE does, but not where the version the app ships lives. A strict reading of "such Covered Software" in §3.2(a) can fault exactly that, and one string fixes it.
2. **The address is tied to okhttp 4.11.0.** `ads-mobile-sdk` 1.4.0 pulls it in. Another okhttp on the release graph (U-6, checklist 3.5) means another PSL revision, and the address must be re-derived before the cut.
3. **mozilla.org's CC BY-SA site policy** was weighed and does not reach the licence text (§1). If the founder ever wants certainty beyond the steward's FAQs, `licensing@mozilla.org` is the steward's address for copyright questions. I do not recommend asking: nothing turns on it.
4. **No founder question.** Nothing here is copyleft beyond MPL's file-level scope, unknown, NC or ND. "Mozilla" appears only in words, as the licence's name and in the credit; publicsuffix.org itself calls the list "an initiative of Mozilla". No trademark question arises.

## Handoff
Next: producer —
- **ux-designer:** add the exact-version address to the Public Suffix List entry (§3.2, §6), and close §9 item 6 and the §7 MPL-2.0 row with this report's verdict and full hash.
- **release-manager:** row J's Source URL and How-credited cells as in "Changes to transcribe"; the NOTICE's full hash; the U-6 re-derivation note at checklist 3.5.
- **android-game-dev (when the screen is built):** fetch `MPL-2.0.txt` from https://www.mozilla.org/media/MPL/2.0/index.txt and pin `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04` in LS-AC12. Pin `PublicSuffixList-NOTICE.txt` at `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a`.
- **code-auditor (at review):** both assets' hashes, and that the exact-version line names `a312abe0e11421aff8fe909a5e02f36b7c5d28de` while okhttp is 4.11.0.
- **Founder:** nothing.

## Sources
Every URL fetched for this report, all on 2026-10-24. No request carried anything about the product.
- https://www.mozilla.org/media/MPL/2.0/index.txt (GET and HEAD)
- https://www.mozilla.org/media/MPL/2.0/index.f75d2927d3c1.txt
- https://www.mozilla.org/en-US/MPL/
- https://www.mozilla.org/en-US/MPL/2.0/
- https://mozilla.org/MPL/2.0/ (redirects to the page above)
- https://www.mozilla.org/en-US/MPL/2.0/FAQ/
- https://www.mozilla.org/en-US/MPL/2.0/Revision-FAQ/
- https://www.mozilla.org/en-US/MPL/license-policy/
- https://www.mozilla.org/en-US/foundation/licensing/
- https://www.mozilla.org/en-US/foundation/licensing/website-content/
- https://creativecommons.org/licenses/by-sa/3.0/legalcode
- https://raw.githubusercontent.com/spdx/license-list-data/main/text/MPL-2.0.txt
- https://web.archive.org/cdx/search/cdx?url=www.mozilla.org/media/MPL/2.0/index.txt&fl=timestamp,statuscode,digest,length&collapse=digest&filter=statuscode:200
- https://web.archive.org/web/20151208160155id_/https://www.mozilla.org/media/MPL/2.0/index.txt
- https://web.archive.org/web/20220110103803id_/https://www.mozilla.org/media/MPL/2.0/index.txt
- https://web.archive.org/web/20260126140119id_/https://www.mozilla.org/media/MPL/2.0/index.txt
- https://web.archive.org/web/20260720111845id_/https://www.mozilla.org/media/MPL/2.0/index.txt
- https://raw.githubusercontent.com/square/okhttp/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/NOTICE
- https://raw.githubusercontent.com/square/okhttp/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/publicsuffixes.gz
- https://raw.githubusercontent.com/square/okhttp/parent-4.11.0/okhttp/src/test/resources/okhttp3/internal/publicsuffix/public_suffix_list.dat
- https://raw.githubusercontent.com/square/okhttp/parent-4.11.0/okhttp/src/test/java/okhttp3/internal/publicsuffix/PublicSuffixListGenerator.java
- https://api.github.com/repos/square/okhttp/git/trees/parent-4.11.0?recursive=1 (301 to https://api.github.com/repositories/5152285/git/trees/parent-4.11.0?recursive=1)
- https://api.github.com/repos/square/okhttp/commits?path=okhttp/src/main/resources/okhttp3/internal/publicsuffix/publicsuffixes.gz&sha=parent-4.11.0&per_page=3
- https://api.github.com/repos/square/okhttp/commits?path=okhttp/src/test/resources/okhttp3/internal/publicsuffix/public_suffix_list.dat&sha=parent-4.11.0&per_page=3
- https://github.com/publicsuffix/list.git (`git clone --filter=blob:none --no-checkout`)
- https://api.github.com/repos/publicsuffix/list/commits?path=public_suffix_list.dat&since=2023-03-15T00:00:00Z&until=2023-04-23T00:00:00Z&per_page=100
- https://raw.githubusercontent.com/publicsuffix/list/88467c960d6cdad2ca1623e892e5e17506bc269f/public_suffix_list.dat, and the same path at `29aa52a7e44712088e42b3c9d62b751d363b555b` and `ba5ffc31ecd6d68ce08a1bd7aa1a04be453f02f1` (no match)
- https://raw.githubusercontent.com/publicsuffix/list/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat
- https://raw.githubusercontent.com/publicsuffix/list/main/LICENSE
- https://publicsuffix.org/list/public_suffix_list.dat
- https://publicsuffix.org/
- https://publicsuffix.org/list/
