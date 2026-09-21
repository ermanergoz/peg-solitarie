# release-manager report — app-ads.txt verification failure (REL-03 / ROADMAP success criterion 2)
Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (014eba9..HEAD)
Skills loaded: ergoz-soft:standards (studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, ip-policy.md, kotlin-android.md, compose.md — local `.claude/rules/` snapshot verified byte-identical to the plugin copies, so the 0.5.1/0.5.2 version gap carries no rule changes)
Ran in parallel with: phase-14 external/release track (other worktrees — not touched)
Loops: 0
Lessons applied: none

## Summary
The `app-ads.txt` file is not the problem, and never was. It has been live at
`https://ermanergoz.com/app-ads.txt` continuously since 2026-09-02, served with `200`, zero
redirects, `text/plain`, no BOM, and exactly one correct record for the publisher ID recorded in
`local.properties`. The failure is one step earlier: **the Play store listing for
`com.erman.pegsolitarie` carries no developer website URL at all.** AdMob derives the
`app-ads.txt` hostname from that field; with the field empty there is no hostname to derive, the
crawler never issues a request, and no amount of work on the file can change the status. This is
proven from the public listing and from Google's own documented confirmation method, not inferred.

## Ruling

### The single cause (proven)
**The Play listing's Contact-details → Website field is empty.**

Google's AdMob documentation (fetched live, quoted in Checks) states the mechanism and the test
for it in the same paragraph:

> "Your developer website must be linked in your store listings in Google Play or the App Store in
> order for AdMob and other ad platforms to establish your app's ownership."

> "To confirm you successfully added the URL, check that the Developer Website URL is linked in
> the 'app support' of your Android app page."

> "The AdMob app-ads.txt crawler checks for your app-ads.txt file based on the developer website
> in your app's store listing. […] The hostname is determined from your app's developer website."

The public listing page for `com.erman.pegsolitarie` was fetched in three locale combinations. In
every one, the **App support** block contains exactly two rows — *Support email*
(`ermanergoz@gmail.com`) and *Privacy Policy*
(`https://ermanergoz.github.io/policies/peg-solitarie-privacy-policy.html`) — and the literal word
`Website` appears **zero times on the entire page**. By the confirmation test Google itself
prescribes, the field is unset.

That single fact explains the founder's report exactly: the file is there, it has been there a
long time, it is correct, and AdMob still reports the app as unauthorised — because AdMob has
never fetched it.

### What the evidence rules out
Each of these was checked explicitly and is **disproven**, not merely unlikely:

| Candidate cause | Verdict | Evidence |
|---|---|---|
| File missing on the canonical host | Disproven | `200`, 0 redirects, 58 bytes |
| File authorises a *different* publisher (an old app / another network) | Disproven | Exactly one record; the publisher block is the one from `local.properties`; no second line, no RESELLER row |
| UTF-8 BOM | Disproven | First bytes are `67 6f 6f 67` (`goog`) — no `EF BB BF` |
| HTML served as `text/plain`, or a 404 page returning 200 | Disproven | `content-type: text/plain; charset=utf-8`; a deliberately missing path returns a real `404` |
| CRLF line endings | Disproven | Zero `CR` bytes in the body |
| File under a subdirectory rather than the host root | Disproven | Served at `/app-ads.txt` |
| Wrong case (`App-ads.txt`) | Disproven | Lowercase path serves 200; the capitalised variant 404s, i.e. only the correct name exists |
| `robots.txt` blocking the crawler | Disproven | `User-agent: * / Allow: /` |
| Recently published, crawl still pending | Disproven | The file has been in the **deployed** `gh-pages` tree since commit `baccd92`, 2026-09-02 02:10 +03, byte-identical to today's copy (`git diff baccd92 b5d1dbd -- app-ads.txt` is empty) |
| Cross-root-domain 301 from the Play-registered host defeats the crawler | **Disproven — see below** |

### The redirect theory is wrong, and so is the checklist's advice about it
`14-OPERATOR-CHECKLIST.md` Step 0 item 4 (and the repo's standing suspicion) treats the
`ermanergoz.github.io` → `ermanergoz.com` 301 as the prime risk and recommends serving the file at
the root of *both* hosts. Two independent findings kill that:

1. **Google documents cross-domain redirects as valid.** The "Web server redirects" section of the
   AdMob help page says: *"Your web server may redirect the crawler to an app-ads.txt file hosted
   elsewhere (including a 'www.' subdomain). […] Valid redirect destinations could include, but
   not limited to: […] Another domain, including multiple subdomains:
   `https://www.cdn.files.example.com/app-ads.txt`"*. A single 301 from `ermanergoz.github.io` to
   `ermanergoz.com` is squarely inside that. The redirect would have been followed.
2. **The "serve it on both hosts" fix is not available on this setup.** The developer website is a
   GitHub Pages site with a custom domain: `public/CNAME` and `dist/CNAME` both contain
   `ermanergoz.com`, and deployment is `gh-pages -d dist`, so the CNAME ships with every deploy.
   GitHub Pages 301-redirects the **entire** `*.github.io` host once a custom domain is configured
   — there is no per-path exception. `ermanergoz.github.io` therefore *cannot* serve
   `app-ads.txt` directly while `ermanergoz.com` is the configured domain. A founder following
   item 4 today would spend time attempting something the platform does not permit.

Step 0 item 4 must be corrected. A proposed diff is in "Proposed checklist edits" below; it is not
applied here because that file belongs to `feature/phase-14-release-plan`, a branch this worktree
is not on.

### Ranked alternatives, if filling the Website field does not flip the status
1. **The AdMob app entry is not linked to the published Play listing.** If the app was added in
   AdMob as "not listed on a store" / created manually, AdMob has no store listing to read the
   website field *from*, and the status can never flip regardless of the field or the file.
   Founder-only: AdMob → Apps → this app → App settings → "App store" / linking state.
2. **Crawl latency after the fix.** Google's note: *"If you added or updated your developer website
   within your app store listing in Google Play, allow up to 24 hours for AdMob to detect these
   changes."* Filling an empty field starts that clock from zero; changing it again restarts it.
3. **Publisher-ID mismatch at the account level.** The record on the site names the same publisher
   block that appears inside the `ADMOB_APP_ID` in `local.properties`, so this is effectively
   closed — but only the founder can read AdMob → Settings → Account information to confirm the
   account's own publisher ID is that one and not a second, older AdMob account.
4. **Trailing newline — a non-issue; do not chase it.** The body is 58 bytes for a 58-character
   record, i.e. no terminating newline. The ads.txt/app-ads.txt grammar is line-oriented and a
   final unterminated line is a valid record; IAB's and Google's parsers both accept it. Adding a
   trailing `\n` is free if the file is being edited anyway, but it is not a fix and must not be
   presented as one.

## Fix list for the founder — shortest path first

1. **Play Console → Grow → Store presence → Store listing → Contact details → Website.** Set it to
   exactly:

       https://ermanergoz.com

   Bare apex, `https`, no trailing path, no `www.`. *Fixes:* the actual cause — it gives AdMob the
   hostname it needs. *Why this host and not `ermanergoz.github.io`:* `ermanergoz.com` serves
   `/app-ads.txt` with a direct `200` and zero redirects, and it has no subdomain, so Google's
   "probe up to one subdomain level" rule cannot walk the crawler up to `github.io` (a shared
   public suffix belonging to someone else). *Confirmed by:* after the listing update goes live,
   the app's public Play page shows a **Website** row under **App support** pointing at
   `ermanergoz.com`. That row is Google's own documented confirmation that the field took effect —
   and it is exactly the check that is failing today.

2. **Wait up to 24 hours, then re-read AdMob → Apps → this app → App settings → `app-ads.txt`.**
   *Fixes:* nothing — it is the crawl window. *Confirmed by:* the status flips from
   "not found"/"unauthorised" to authorised/verified. Do not touch the Website field again during
   this window; every edit restarts the 24-hour clock.

3. **If and only if 48 hours pass with no flip:** check **AdMob → Apps → this app → App settings**
   and confirm the app entry is *linked to the Play listing* (store-listed), not a manually added
   entry. An unlinked entry has no listing to read the website from. *Fixes:* alternative cause 1.
   *Confirmed by:* the AdMob app page showing the Play store link / package
   `com.erman.pegsolitarie`.

4. **Same window, one-minute check:** AdMob → Settings → Account information. Confirm the
   publisher ID there is the one embedded in the `ADMOB_APP_ID` value in `local.properties`.
   *Fixes:* alternative cause 3 (a second, older AdMob account). *Confirmed by:* the two strings
   matching character for character.

5. **Do not touch the website.** No change to `app-ads.txt`, no attempt to serve it from
   `ermanergoz.github.io`, no re-deploy. The file is correct and live; a redeploy only adds noise
   and, if anything went wrong, would turn a working file into a broken one.

Optional, unrelated to this fault and worth one commit next time the site is edited: the live
`robots.txt` advertises `Sitemap: https://ermanergoz.github.io/sitemap.xml`, the redirecting host,
while the canonical host is `ermanergoz.com`. Cosmetic; it has no bearing on app-ads.txt.

## Outputs
- `.planning/reports/phase-15/release-manager-app-ads-diagnosis.md` — this report; the evidence
  base and ruling for REL-03 / ROADMAP success criterion 2.

## Proposed checklist edits
Proposed, not applied — `14-OPERATOR-CHECKLIST.md` lives on `feature/phase-14-release-plan`.
The maintainer of that branch should apply this to Step 0 item 4:

```diff
-4. [ ] **Check which host the store listing actually names.** Play Console → Grow → Store
-       presence → Store listing → *Contact details → Website*. AdMob discovers `app-ads.txt` by
-       reading the developer-website URL **off the store listing**, so the file has to be at the
-       root of *that* host. The repo already records that two hosts are in play: the
-       Play-registered address is `ermanergoz.github.io`, which 301-redirects to the canonical
-       domain compiled into the app (`PrivacyLinks.kt` KDoc). **Current state of the listing's
-       website field: unverified** — only you can read it.
-       - If the field names the GitHub Pages host, the simplest robust fix is to **serve
-         `app-ads.txt` at the root of both hosts** with a direct 200. Redirect handling across
-         root domains is the one part of the ads.txt spec that different crawlers read
-         differently, and serving both costs one extra file.
-       - Do **not** "fix" this by changing the listing's website field to the canonical domain
-         unless you also re-check Step 2's curl against the new host. Changing the field starts
-         the 24-hour crawl clock over.
+4. [x] **Which host the store listing names — settled 2026-09-21: it names none.** AdMob
+       discovers `app-ads.txt` by reading the developer-website URL **off the store listing**, so
+       the file has to be at the root of *that* host. The public Play page for
+       `com.erman.pegsolitarie` was fetched on 2026-09-21 in three locales: its **App support**
+       block carries only a support email and a privacy-policy link, and the word `Website` does
+       not appear on the page at all. Google's own confirmation test for this field is "check that
+       the Developer Website URL is linked in the 'app support' of your Android app page"
+       (AdMob Help 9363762), so **the Contact-details → Website field is empty**. That, not the
+       file, is why the status never flipped: with no hostname there is no crawl.
+       - **The fix is to fill the field with `https://ermanergoz.com`** (bare apex, https, no
+         path, no `www.`). That host serves `/app-ads.txt` with a direct 200 and zero redirects,
+         and having no subdomain it keeps Google's "probe up to one subdomain level" rule from
+         walking up to the shared `github.io` suffix.
+       - **Do not try to serve `app-ads.txt` on `ermanergoz.github.io`** — an earlier revision of
+         this checklist recommended serving it on both hosts. That is not possible here: the site
+         repo ships a `CNAME` of `ermanergoz.com` in `public/` and `dist/` and deploys with
+         `gh-pages -d dist`, and GitHub Pages 301-redirects the whole `*.github.io` host once a
+         custom domain is set. There is no per-path exception.
+       - The 301 itself was never the fault. AdMob Help 9363762, "Web server redirects", lists
+         "Another domain, including multiple subdomains" among valid redirect destinations, so a
+         single cross-domain 301 is followed.
+       - Filling an empty field starts the 24-hour crawl clock; every later edit restarts it.
```

Item 2's "no BOM, `Content-Type: text/plain`" guidance is already satisfied and can be ticked; item
3's curl returns `200 0` against `https://ermanergoz.com/app-ads.txt` and can be ticked. Items 1, 5
and 6 remain founder-only.

## Checks
Every command below was run from this machine on 2026-09-21 (UTC times are from the responses).

- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} %{content_type}\n' -L https://ermanergoz.com/app-ads.txt` — pass (`200 0 https://ermanergoz.com/app-ads.txt text/plain; charset=utf-8`)
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} %{content_type}\n' https://ermanergoz.com/app-ads.txt` — pass (identical without `-L`: direct 200, no redirect)
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} %{content_type}\n' -L https://ermanergoz.github.io/app-ads.txt` — pass (`200 1 https://ermanergoz.com/app-ads.txt text/plain; charset=utf-8` — one redirect, lands on the canonical host)
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} %{content_type}\n' https://ermanergoz.github.io/app-ads.txt` — pass, and this is the key host result (`301 0 https://ermanergoz.github.io/app-ads.txt text/html` — the GitHub Pages custom-domain redirect, not a file)
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} %{content_type}\n' -L https://www.ermanergoz.com/app-ads.txt` — pass (`200 1 https://ermanergoz.com/app-ads.txt text/plain` — `www` is a CNAME to the Pages host and redirects to the apex)
- `curl -sS https://ermanergoz.com/app-ads.txt` — pass (body is one record: `google.com, pub-…, DIRECT, f08c47fec0942fa0`, the publisher ID from `local.properties`; no second line)
- `head -c 32 canonical.txt | xxd` — pass (starts `67 6f 6f 67` = `goog`; **no `EF BB BF` BOM**)
- `wc -c canonical.txt && file canonical.txt` — pass (`58` bytes; `ASCII text, with no line terminators` — no trailing newline, which is valid)
- `grep -c $'\r' canonical.txt` — pass (`0` — no CRLF)
- `curl -sSI https://ermanergoz.com/app-ads.txt` — pass (`HTTP/2 200`, `content-type: text/plain; charset=utf-8`, `server: GitHub.com`, `content-length: 58`, `last-modified: Mon, 21 Sep 2026 00:09:46 GMT`)
- `curl -sSI https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` and `curl -sSI https://ermanergoz.com/` — pass, and they matter: **both carry the same `last-modified: Mon, 21 Sep 2026 00:09:46 GMT`**, proving that header is the GitHub Pages site-build timestamp and dates nothing about `app-ads.txt` itself
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{content_type}\n' https://ermanergoz.com/this-file-does-not-exist-zzz.txt` — pass (`404 0 text/html` — no "404 page served as 200" trap)
- `curl -sS -o /dev/null -w '%{http_code} …\n' -L https://ermanergoz.com/App-ads.txt` — pass (`404` — only the lowercase name exists, which is the correct one)
- `curl -sS -o /dev/null -w '%{http_code} …\n' -L https://ermanergoz.com/ads.txt` — pass (`404`; expected — this is an app, not a web property)
- `curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective} …\n' -L http://ermanergoz.com/app-ads.txt` and the `http://ermanergoz.github.io/…` form — pass (both reach `200 … https://ermanergoz.com/app-ads.txt` in one redirect; the plaintext entry point a crawler may start from works)
- `curl -sS https://ermanergoz.com/robots.txt` — pass (`User-agent: * / Allow: /` — the crawler is not blocked)
- `dig +short ermanergoz.com A` / `www.ermanergoz.com` / `ermanergoz.github.io` — pass (all four GitHub Pages anycast IPs `185.199.108-111.153`; `www` is a CNAME to `ermanergoz.github.io`)
- `git ls-tree -r --name-only <commit> | grep -qx 'app-ads.txt'` over every `origin/gh-pages` commit — pass, and this closes the timeline: `app-ads.txt` is **present in all six deployed commits**, the oldest `fb752f3` at 2026-09-02 01:58 +03, with `CNAME=ermanergoz.com` from `baccd92` (2026-09-02 02:10 +03) onward
- `git diff baccd92 b5d1dbd -- app-ads.txt` — pass (empty — today's redeploy did **not** change the file; the 2026-09-02 content is byte-identical to what is live)
- `for f in CNAME public/CNAME dist/CNAME; do cat $f; done` in the site repo — pass (`public/CNAME` and `dist/CNAME` = `ermanergoz.com`; with `"deploy": "gh-pages -d dist"` in `package.json`, the CNAME ships on every deploy, which is why the whole `*.github.io` host redirects)
- `curl -sS -A '<desktop UA>' -o play.html -w '%{http_code} …\n' -L 'https://play.google.com/store/apps/details?id=com.erman.pegsolitarie&hl=en&gl=US'` — pass (`200`, 1.08 MB) — **and the decisive result**: the literal word `Website` occurs **0 times**; the App support block contains only `Support email → ermanergoz@gmail.com` and `Privacy Policy → https://ermanergoz.github.io/policies/peg-solitarie-privacy-policy.html`
- The same fetch with `hl=tr&gl=TR` and `hl=en&gl=TR` — pass (0 hits for `Website` / `Web sitesi` in both; not a locale-rendering artefact)
- `curl -sS -A '<desktop UA>' -L 'https://support.google.com/admob/answer/9363762?hl=en'` — pass (AdMob's own app-ads.txt documentation; source of the three quotes in "The single cause" and of the "Web server redirects" section quoted under the redirect ruling)
- `grep applicationId app/build.gradle.kts` — pass (`com.erman.pegsolitarie` — confirms the listing fetched above is this app)
- `diff .claude/rules/<f>.md <plugin>/rules/<f>.md` for the nine release-relevant rule files — pass (all identical; the 0.5.1 snapshot is current in content)

Not run: `./gradlew check`. This task changed no product code — the only file written is a report
under `.planning/` — so the Check command has nothing to verify here. No `VERSION`, `CHANGELOG.md`,
tag or store asset was touched.

## Open issues / risks
- **The ruling rests on the public listing rendering.** Google prescribes that page as the
  confirmation test for this exact field, so the inference is the one Google intends — but the
  Console is still the authority. Item 1 of the fix list is self-verifying either way: if the field
  turns out to be already set, the founder will see it in the Console and we move straight to
  alternative cause 1.
- **Alternative causes 1 and 3 are not testable from here** (AdMob account internals). They are
  steps 3 and 4 of the fix list and cost about a minute each.
- **REL-03 stays open until the AdMob status flips**, which is at least 24 hours after step 1 — and
  the clock has not started. This is now the longest-latency item in the phase-14 external track,
  as Step 0 always claimed, but for a different reason than Step 0 assumed.
- **Proposed founder question, for the producer to file if it is worth a `Q-<NNN>`:**

      ## Q-<NNN> — Play listing has no developer website; app-ads.txt can never verify   (open)
      Date: 2026-09-21    Asked by: release-manager
      Context: The app-ads.txt file has been correct and live on the canonical domain since
      2026-09-02, but the public Play listing for com.erman.pegsolitarie shows no Website under
      App support in any locale, so AdMob has no hostname to crawl.
      Options: 1) Set Contact details → Website to https://ermanergoz.com and wait ≤ 24 h.
               2) Set it to https://ermanergoz.github.io (works via the 301, but risks Google's
                  subdomain probe walking up to the shared github.io suffix).
               3) Leave it and accept unauthorised inventory (suppressed programmatic demand on
                  the two ad surfaces phases 9 and 10 built).
      Recommendation: 1 — it is the only host that serves the file with a direct 200 and no
      redirect, it has no subdomain, and it is already the canonical domain compiled into the app.
      Blocks: REL-03 and ROADMAP success criterion 2 (non-blocking for every other phase-15 task).
      Answer:

## Handoff
Next: **producer** — this needs no developer and no code. Hand the founder the five-step fix list
above (step 1 is the whole fix; steps 2–5 are confirmation and fallbacks) and, separately, hand the
proposed Step 0 item 4 diff to whoever owns `feature/phase-14-release-plan`, because the checklist
currently sends the founder after a redirect that Google documents as fine and toward a
both-hosts fix that GitHub Pages cannot perform. REL-03 cannot be ticked until AdMob itself reports
the app as authorised; nothing in the repo can substitute for that observation.
