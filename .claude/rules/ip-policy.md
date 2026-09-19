# IP policy

Separate four things when studying any existing product: mechanics or
features (ideas), assets (art, audio, fonts, copy, level data), code, and
name/branding (title, logo, characters, distinctive look).

- Ideas, mechanics, and features are not protected by copyright. Inspiration
  is always allowed. Record the source in THIRD_PARTY.md anyway.
- Faithful remake or copying assets/levels/look/code is allowed only when the
  research-analyst's verdict shows the original is unprotected: public
  domain, or released as freeware/open source by the rights holder under a
  license that permits it, and the name is not a live trademark. Protected →
  inspiration only.
- Corporate-authored software is copyrighted for 95 years from publication
  in the US; a 1990 DOS game is protected until the 2080s. "Abandonware",
  "it's on an old website", or "no license file" mean NO reuse.
- Never ship a title, logo, character, or distinctive visual identity from an
  existing product.
- Assets and code reuse follow the matrix; attribution in THIRD_PARTY.md and
  in the product's licenses screen or page.
- Google Play and App Store IP policies apply; modifying protected material
  does not make it yours.

| License | Code | Assets | Notes |
|---|---|---|---|
| MIT, Apache-2.0, BSD-2/3, ISC, Zlib | yes | yes | keep notice + attribution |
| MPL-2.0 | yes (file-level copyleft) | yes | modified files stay MPL |
| LGPL | dynamic link only | — | escalate |
| GPL-2.0/3.0, AGPL | escalate to founder | — | whole product becomes GPL |
| CC0 | yes | yes | attribution optional |
| CC-BY | yes | yes | credit required |
| CC-BY-SA | escalate | escalate | share-alike |
| CC-BY-NC, CC-BY-ND | no | no | unusable for a store product |
| No license / unknown | no | no | no reuse |

THIRD_PARTY.md is mandatory: one row per borrowed idea, asset, or snippet
with source URL, license, what was taken, how credited. The release skill
blocks a release if a row lacks a license.
